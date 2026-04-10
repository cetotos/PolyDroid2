#include <jni.h>
#include <dlfcn.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <pthread.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <poll.h>
#include <time.h>
#include <stdatomic.h>
#include <sched.h>
#include <sys/resource.h>
#include <sys/syscall.h>

#define TAG "PolyDroid2-window"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static atomic_int g_comp_fps = 0;
static atomic_int g_unity_fps = 0;
static atomic_int g_comp_total_frames = 0;

static char g_vulkan_info[256] = "";

JNIEXPORT jlong JNICALL
Java_com_cetotos_polydroid2_GameActivity_nativeGetWindow(
    JNIEnv* env, jobject thiz, jobject surface)
{
    ANativeWindow* win = ANativeWindow_fromSurface(env, surface);
    LOGI("nativeGetWindow: ANativeWindow=%p", win);
    return (jlong)(uintptr_t)win;
}

static int g_bridge_fd = -1;
static int g_bridge_fd2 = -1;
static int g_bridge_fs_fd = -1;
static char g_real_path[256];
static char g_rootfs_path[512];

static ssize_t bridge_forward_msg(int from_fd, int to_fd) {
    char buf[65536];
    char cmsg_buf[CMSG_SPACE(28 * sizeof(int))];

    struct iovec iov = { .iov_base = buf, .iov_len = sizeof(buf) };
    struct msghdr msg;
    memset(&msg, 0, sizeof(msg));
    msg.msg_iov = &iov;
    msg.msg_iovlen = 1;
    msg.msg_control = cmsg_buf;
    msg.msg_controllen = sizeof(cmsg_buf);

    ssize_t n = recvmsg(from_fd, &msg, 0);
    if (n <= 0) return n;

    iov.iov_len = (size_t)n;
    struct msghdr out;
    memset(&out, 0, sizeof(out));
    out.msg_iov = &iov;
    out.msg_iovlen = 1;
    if (msg.msg_controllen > 0) {
        out.msg_control = msg.msg_control;
        out.msg_controllen = msg.msg_controllen;
    }

    ssize_t sent = sendmsg(to_fd, &out, MSG_NOSIGNAL);
    if (sent != n) return -1;
    return n;
}

static void* bridge_conn_thread(void* arg) {
    int client_fd = (int)(intptr_t)arg;

    // ---------------- connect to server
    int server_fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (server_fd < 0) {
        LOGE("bridge: socket() for server: %s", strerror(errno));
        close(client_fd);
        return NULL;
    }

    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    addr.sun_path[0] = '\0';
    strncpy(addr.sun_path + 1, g_real_path, sizeof(addr.sun_path) - 2);
    socklen_t addrlen = offsetof(struct sockaddr_un, sun_path) + 1 + strlen(g_real_path);

    // retry (67 boi :joy: :joy😂)
    int connected = 0;
    for (int attempt = 0; attempt < 67; attempt++) {
        if (connect(server_fd, (struct sockaddr*)&addr, addrlen) == 0) {
            connected = 1;
            break;
        }
        if (errno != ECONNREFUSED && errno != ENOENT) {
            LOGE("bridge: connect to @%s: %s (fatal)", g_real_path, strerror(errno));
            break;
        }
        if (attempt == 0) {
            LOGI("bridge: waiting for Lorie socket @%s ...", g_real_path);
        }
        usleep(100000); // 100 ms
    }
    if (!connected) {
        LOGE("bridge: connect to @%s failed after retries: %s", g_real_path, strerror(errno));
        close(server_fd);
        close(client_fd);
        return NULL;
    }

    LOGI("bridge: connected client_fd=%d to server @%s (sendmsg/recvmsg mode)", client_fd, g_real_path);

    struct pollfd pfds[2];
    pfds[0].fd = client_fd;
    pfds[0].events = POLLIN;
    pfds[1].fd = server_fd;
    pfds[1].events = POLLIN;

    while (1) {
        int ret = poll(pfds, 2, -1);
        if (ret <= 0) break;

        if (pfds[0].revents & (POLLIN | POLLHUP)) {
            if (bridge_forward_msg(client_fd, server_fd) <= 0) break;
        }
        if (pfds[1].revents & (POLLIN | POLLHUP)) {
            if (bridge_forward_msg(server_fd, client_fd) <= 0) break;
        }
        if ((pfds[0].revents | pfds[1].revents) & POLLNVAL) break;
    }

    close(client_fd);
    close(server_fd);
    return NULL;
}

static void* bridge_listen_thread(void* arg) {
    (void)arg;

    while (1) {
        fd_set rfds;
        FD_ZERO(&rfds);
        int maxfd = -1;
        if (g_bridge_fd >= 0) { FD_SET(g_bridge_fd, &rfds); if (g_bridge_fd > maxfd) maxfd = g_bridge_fd; }
        if (g_bridge_fd2 >= 0) { FD_SET(g_bridge_fd2, &rfds); if (g_bridge_fd2 > maxfd) maxfd = g_bridge_fd2; }
        if (g_bridge_fs_fd >= 0) { FD_SET(g_bridge_fs_fd, &rfds); if (g_bridge_fs_fd > maxfd) maxfd = g_bridge_fs_fd; }
        if (maxfd < 0) break;
        maxfd++;

        int ret = select(maxfd, &rfds, NULL, NULL, NULL);
        if (ret <= 0) break;

        int listen_fd = -1;
        const char* src = "unknown";
        if (g_bridge_fd >= 0 && FD_ISSET(g_bridge_fd, &rfds)) {
            listen_fd = g_bridge_fd;
            src = "abstract-std";
        } else if (g_bridge_fd2 >= 0 && FD_ISSET(g_bridge_fd2, &rfds)) {
            listen_fd = g_bridge_fd2;
            src = "abstract-termux";
        } else if (g_bridge_fs_fd >= 0 && FD_ISSET(g_bridge_fs_fd, &rfds)) {
            listen_fd = g_bridge_fs_fd;
            src = "filesystem";
        }
        if (listen_fd < 0) continue;

        int client_fd = accept(listen_fd, NULL, NULL);
        if (client_fd < 0) {
            if (errno == EINVAL || errno == EBADF) break;
            LOGE("bridge: accept(%s): %s", src, strerror(errno));
            continue;
        }
        LOGI("bridge: accepted connection fd=%d from %s socket", client_fd, src);

        pthread_t t;
        pthread_create(&t, NULL, bridge_conn_thread, (void*)(intptr_t)client_fd);
        pthread_detach(t);
    }
    return NULL;
}


// ---------------------- compositor (this is the bad part)
#include <android/rect.h>
typedef struct ASurfaceControl ASurfaceControl;
typedef struct ASurfaceTransaction ASurfaceTransaction;

ASurfaceControl* ASurfaceControl_createFromWindow(ANativeWindow* parent, const char* name);
ASurfaceTransaction* ASurfaceTransaction_create(void);
void ASurfaceTransaction_apply(ASurfaceTransaction* txn);
void ASurfaceTransaction_delete(ASurfaceTransaction* txn);
void ASurfaceTransaction_setVisibility(ASurfaceTransaction* txn, ASurfaceControl* sc, int8_t visibility);
void ASurfaceTransaction_setZOrder(ASurfaceTransaction* txn, ASurfaceControl* sc, int32_t z);
void ASurfaceTransaction_setBuffer(ASurfaceTransaction* txn, ASurfaceControl* sc, AHardwareBuffer* buf, int fence);
void ASurfaceTransaction_setGeometry(ASurfaceTransaction* txn, ASurfaceControl* sc, const ARect* src, const ARect* dst, int32_t transform);

#define ASURFACE_TRANSACTION_VISIBILITY_SHOW 0
#define ANATIVEWINDOW_TRANSFORM_IDENTITY 0
#include <android/hardware_buffer.h>


typedef struct native_handle {
    int version;
    int numFds;
    int data[0];
} native_handle_t;

typedef const native_handle_t* (*PFN_AHardwareBuffer_getNativeHandle)(const AHardwareBuffer* buffer);

static int g_compositor_fd = -1;
static ASurfaceControl* g_surface_ctl = NULL;
static ANativeWindow* g_compositor_win = NULL;


#define COMPOSITOR_MAX_BUFFERS 4
static AHardwareBuffer* g_comp_ahbs[COMPOSITOR_MAX_BUFFERS];
static uint32_t g_comp_ahb_count = 0;

static void* compositor_thread(void* arg) {
    (void)arg;
    pid_t tid = (pid_t)syscall(SYS_gettid);
    if (setpriority(PRIO_PROCESS, tid, -6) == 0) {
       // LOGI("compositor: thread priority set to nice -6 (tid=%d)", tid);
    } else {
       // LOGI("compositor: setpriority failed: %s (non-fatal)", strerror(errno));
    }


    while (1) {
        LOGI("compositor: waiting for Box64 connection on @polydroid_compositor");

        int client_fd = accept(g_compositor_fd, NULL, NULL);
        if (client_fd < 0) {
            LOGE("compositor: accept failed: %s", strerror(errno));
            if (errno == EINVAL || errno == EBADF) return NULL;
            continue;
        }
        LOGI("compositor: Box64 connected (fd=%d)", client_fd);

        uint32_t req[4];
        ssize_t n = recv(client_fd, req, sizeof(req), MSG_WAITALL);
        if (n != sizeof(req)) {
            LOGE("compositor: failed to read buffer request (%zd)", n);
            close(client_fd);
            continue;
        }
        uint32_t width = req[0], height = req[1], format = req[2], count = req[3];
        if (count > COMPOSITOR_MAX_BUFFERS) count = COMPOSITOR_MAX_BUFFERS;
        LOGI("compositor: allocating %u buffers %ux%u fmt=%u", count, width, height, format);

        int alloc_failed = 0;
        for (uint32_t i = 0; i < count; i++) {
            AHardwareBuffer_Desc desc = {
                .width = width,
                .height = height,
                .layers = 1,
                .format = format,
                .usage = AHARDWAREBUFFER_USAGE_GPU_COLOR_OUTPUT |
                         AHARDWAREBUFFER_USAGE_GPU_SAMPLED_IMAGE |
                         AHARDWAREBUFFER_USAGE_COMPOSER_OVERLAY |
                         AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN, // this might be slow, but it is required. without it the format is broken.
                .stride = 0,
                .rfu0 = 0,
                .rfu1 = 0,
            };
            int ret = AHardwareBuffer_allocate(&desc, &g_comp_ahbs[i]);
            if (ret != 0) {
                // LOGE("compositor: AHardwareBuffer_allocate[%u] failed: %d", i, ret);
                alloc_failed = 1;
                break;
            }
            ret = AHardwareBuffer_sendHandleToUnixSocket(g_comp_ahbs[i], client_fd);
            if (ret != 0) {
                // LOGE("compositor: AHardwareBuffer_sendHandleToUnixSocket[%u] failed: %d", i, ret);
                alloc_failed = 1;
                break;
            }

            AHardwareBuffer_Desc actual_desc;
            AHardwareBuffer_describe(g_comp_ahbs[i], &actual_desc);
            // LOGI("compositor: sent AHB[%u] stride=%u (linear for SurfaceFlinger)", i, actual_desc.stride);
        }
        g_comp_ahb_count = count;

        if (alloc_failed) {
            close(client_fd);
            for (uint32_t i = 0; i < g_comp_ahb_count; i++) {
                if (g_comp_ahbs[i]) {
                    AHardwareBuffer_release(g_comp_ahbs[i]);
                    g_comp_ahbs[i] = NULL;
                }
            }
            g_comp_ahb_count = 0;
            continue;
        }

        uint8_t ready = 'R';
        send(client_fd, &ready, 1, 0);
        LOGI("compositor: ready!");
        if (!g_surface_ctl) {
            g_surface_ctl = ASurfaceControl_createFromWindow(g_compositor_win, "polydroid_frame");
            if (!g_surface_ctl) {
                close(client_fd);
                for (uint32_t i = 0; i < g_comp_ahb_count; i++) {
                    if (g_comp_ahbs[i]) { AHardwareBuffer_release(g_comp_ahbs[i]); g_comp_ahbs[i] = NULL; }
                }
                g_comp_ahb_count = 0;
                continue;
            }
        }

        ASurfaceTransaction* txn = ASurfaceTransaction_create();
        ASurfaceTransaction_setVisibility(txn, g_surface_ctl, ASURFACE_TRANSACTION_VISIBILITY_SHOW);
        ASurfaceTransaction_setZOrder(txn, g_surface_ctl, 1);
        ASurfaceTransaction_apply(txn);
        ASurfaceTransaction_delete(txn);

        int32_t screen_w = ANativeWindow_getWidth(g_compositor_win);
        int32_t screen_h = ANativeWindow_getHeight(g_compositor_win);
        LOGI("compositor: presenting on %dx%d surface", screen_w, screen_h);


        {
            struct { char magic[4]; char gpu_name[64]; uint32_t api_ver; uint32_t drv_ver; } meta;
            struct pollfd pfd = { .fd = client_fd, .events = POLLIN };
            if (poll(&pfd, 1, 2000) > 0) {
                ssize_t mn = recv(client_fd, &meta, sizeof(meta), MSG_WAITALL);
                if (mn == sizeof(meta) && memcmp(meta.magic, "VKIF", 4) == 0) {
                    meta.gpu_name[63] = '\0';
                    uint32_t major = (meta.api_ver >> 22) & 0x3FF;
                    uint32_t minor = (meta.api_ver >> 12) & 0x3FF;
                    uint32_t patch = meta.api_ver & 0xFFF;
                    snprintf(g_vulkan_info, sizeof(g_vulkan_info), "%u.%u.%u | %s",
                             major, minor, patch, meta.gpu_name);
                    LOGI("compositor: Vulkan %u.%u.%u, GPU: %s, driver: 0x%x",
                         major, minor, patch, meta.gpu_name, meta.drv_ver);
                } else {
                    LOGI("compositor: no VKIF metadata (got %zd bytes)", mn);
                }
            }
        }

        // -------------------------- frame loop ------------
        int frame_count = 0;
        struct timespec fps_start;
        clock_gettime(CLOCK_MONOTONIC, &fps_start);
        int fps_frame_counter = 0;
        ARect src = {0, 0, (int32_t)width, (int32_t)height};
        ARect dst = {0, 0, screen_w, screen_h};

        while (1) {
            uint32_t msg[2]; // { image_index, unity_fps }
            n = recv(client_fd, msg, sizeof(msg), MSG_WAITALL);
            if (n != sizeof(msg)) break;
            uint32_t img_idx = msg[0];
            atomic_store(&g_unity_fps, (int)msg[1]);
            if (img_idx >= g_comp_ahb_count) continue;

            AHardwareBuffer* ahb = g_comp_ahbs[img_idx];
            txn = ASurfaceTransaction_create();
            ASurfaceTransaction_setBuffer(txn, g_surface_ctl, ahb, -1);
            ASurfaceTransaction_setGeometry(txn, g_surface_ctl, &src, &dst,
                                            ANATIVEWINDOW_TRANSFORM_IDENTITY);
            ASurfaceTransaction_apply(txn);
            ASurfaceTransaction_delete(txn);

            frame_count++;
            fps_frame_counter++;
            atomic_store(&g_comp_total_frames, frame_count);

            // calculate FPS
            struct timespec now;
            clock_gettime(CLOCK_MONOTONIC, &now);
            double elapsed = (now.tv_sec - fps_start.tv_sec) +
                             (now.tv_nsec - fps_start.tv_nsec) / 1e9;
            if (elapsed >= 1.0) {
                int fps = (int)(fps_frame_counter / elapsed + 0.5);
                atomic_store(&g_comp_fps, fps);
                fps_frame_counter = 0;
                fps_start = now;
            }

            if (frame_count == 1) {
                LOGI("compositor: first frame presented (idx=%u)", img_idx);
            }
        }

        LOGI("compositor: client disconnected after %d frames, waiting for reconnect", frame_count);
        close(client_fd);

        txn = ASurfaceTransaction_create();
        ASurfaceTransaction_setBuffer(txn, g_surface_ctl, NULL, -1);
        ASurfaceTransaction_apply(txn);
        ASurfaceTransaction_delete(txn);

        for (uint32_t i = 0; i < g_comp_ahb_count; i++) {
            if (g_comp_ahbs[i]) {
                AHardwareBuffer_release(g_comp_ahbs[i]);
                g_comp_ahbs[i] = NULL;
            }
        }
        g_comp_ahb_count = 0;
    }
    return NULL;
}

JNIEXPORT jboolean JNICALL
Java_com_cetotos_polydroid2_GameActivity_nativeStartFrameCompositor(
    JNIEnv* env, jobject thiz, jobject surface)
{
    g_compositor_win = ANativeWindow_fromSurface(env, surface);
    if (!g_compositor_win) {
        LOGE("compositor: ANativeWindow_fromSurface failed");
        return JNI_FALSE;
    }

    if (g_compositor_fd >= 0) {
        close(g_compositor_fd);
        g_compositor_fd = -1;
    }

    g_compositor_fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (g_compositor_fd < 0) {
        LOGE("compositor: socket: %s", strerror(errno));
        return JNI_FALSE;
    }

    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    addr.sun_path[0] = '\0';
    const char* name = "polydroid_compositor";
    strncpy(addr.sun_path + 1, name, sizeof(addr.sun_path) - 2);
    socklen_t addrlen = offsetof(struct sockaddr_un, sun_path) + 1 + strlen(name);

    if (bind(g_compositor_fd, (struct sockaddr*)&addr, addrlen) < 0) {
        close(g_compositor_fd);
        g_compositor_fd = socket(AF_UNIX, SOCK_STREAM, 0);
        if (g_compositor_fd < 0 ||
            bind(g_compositor_fd, (struct sockaddr*)&addr, addrlen) < 0) {
            if (g_compositor_fd >= 0) close(g_compositor_fd);
            g_compositor_fd = -1;
            return JNI_FALSE;
        }
    }
    if (listen(g_compositor_fd, 1) < 0) {
        LOGE("compositor: listen: %s", strerror(errno));
        close(g_compositor_fd);
        g_compositor_fd = -1;
        return JNI_FALSE;
    }

    pthread_t t;
    pthread_create(&t, NULL, compositor_thread, NULL);
    pthread_detach(t);

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_cetotos_polydroid2_GameActivity_nativeStartX11Bridge(
    JNIEnv* env, jobject thiz, jstring jRealPath, jstring jRootfsPath)
{
    const char* realPath = (*env)->GetStringUTFChars(env, jRealPath, NULL);
    strncpy(g_real_path, realPath, sizeof(g_real_path) - 1);
    (*env)->ReleaseStringUTFChars(env, jRealPath, realPath);

    const char* rootfsPath = (*env)->GetStringUTFChars(env, jRootfsPath, NULL);
    strncpy(g_rootfs_path, rootfsPath, sizeof(g_rootfs_path) - 1);
    (*env)->ReleaseStringUTFChars(env, jRootfsPath, rootfsPath);

    if (g_bridge_fd >= 0) { close(g_bridge_fd); g_bridge_fd = -1; }
    if (g_bridge_fd2 >= 0) { close(g_bridge_fd2); g_bridge_fd2 = -1; }
    if (g_bridge_fs_fd >= 0) { close(g_bridge_fs_fd); g_bridge_fs_fd = -1; }

    g_bridge_fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (g_bridge_fd < 0) {
        LOGE("bridge: socket: %s", strerror(errno));
        return JNI_FALSE;
    }

    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    addr.sun_path[0] = '\0';
    const char* listen_name = "/tmp/.X11-unix/X0";
    strncpy(addr.sun_path + 1, listen_name, sizeof(addr.sun_path) - 2);
    socklen_t addrlen = offsetof(struct sockaddr_un, sun_path) + 1 + strlen(listen_name);

    if (bind(g_bridge_fd, (struct sockaddr*)&addr, addrlen) < 0) {
        LOGE("bridge: bind @%s: %s", listen_name, strerror(errno));
        close(g_bridge_fd);
        g_bridge_fd = -1;
        return JNI_FALSE;
    }

    if (listen(g_bridge_fd, 8) < 0) {
        LOGE("bridge: listen: %s", strerror(errno));
        close(g_bridge_fd);
        g_bridge_fd = -1;
        return JNI_FALSE;
    }

    LOGI("bridge: listening on abstract @%s, forwarding to @%s", listen_name, g_real_path);

    // mapper (path_remapper.c) will handle /tmp/
    char fs_dir[768];
    char fs_path[768];
    snprintf(fs_dir, sizeof(fs_dir), "%s/tmp/.X11-unix", g_rootfs_path);
    snprintf(fs_path, sizeof(fs_path), "%s/tmp/.X11-unix/X0", g_rootfs_path);
    char tmp_dir[768];
    snprintf(tmp_dir, sizeof(tmp_dir), "%s/tmp", g_rootfs_path);
    mkdir(tmp_dir, 0777);
    mkdir(fs_dir, 0777);
    chmod(fs_dir, 0777);
    unlink(fs_path);

    g_bridge_fs_fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (g_bridge_fs_fd >= 0) {
        struct sockaddr_un fs_addr;
        memset(&fs_addr, 0, sizeof(fs_addr));
        fs_addr.sun_family = AF_UNIX;
        strncpy(fs_addr.sun_path, fs_path, sizeof(fs_addr.sun_path) - 1);

        if (bind(g_bridge_fs_fd, (struct sockaddr*)&fs_addr, sizeof(fs_addr)) < 0) {
            close(g_bridge_fs_fd);
            g_bridge_fs_fd = -1;
        } else if (listen(g_bridge_fs_fd, 8) < 0) {
            close(g_bridge_fs_fd);
            g_bridge_fs_fd = -1;
        } else {
            chmod(fs_path, 0777);
        }
    }

    // termux's libxcb has a hardcoded path. im NOT rebuilding it just to fix it.
    g_bridge_fd2 = socket(AF_UNIX, SOCK_STREAM, 0);
    if (g_bridge_fd2 >= 0) {
        struct sockaddr_un termux_addr;
        memset(&termux_addr, 0, sizeof(termux_addr));
        termux_addr.sun_family = AF_UNIX;
        termux_addr.sun_path[0] = '\0';
        const char* termux_name = "/data/data/com.termux/files/usr/tmp/.X11-unix/X0";
        strncpy(termux_addr.sun_path + 1, termux_name, sizeof(termux_addr.sun_path) - 2);
        socklen_t termux_addrlen = offsetof(struct sockaddr_un, sun_path) + 1 + strlen(termux_name);

        if (bind(g_bridge_fd2, (struct sockaddr*)&termux_addr, termux_addrlen) < 0) {
            LOGE("bridge: bind @%s: %s (non-fatal)", termux_name, strerror(errno));
            close(g_bridge_fd2);
            g_bridge_fd2 = -1;
        } else if (listen(g_bridge_fd2, 8) < 0) {
            close(g_bridge_fd2);
            g_bridge_fd2 = -1;
        }
    }

    pthread_t t;
    pthread_create(&t, NULL, bridge_listen_thread, NULL);
    pthread_detach(t);

    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_com_cetotos_polydroid2_GameActivity_nativeGetCompositorFps(
    JNIEnv* env, jobject thiz)
{
    return (jint)atomic_load(&g_comp_fps);
}

JNIEXPORT jint JNICALL
Java_com_cetotos_polydroid2_GameActivity_nativeGetUnityFps(
    JNIEnv* env, jobject thiz)
{
    return (jint)atomic_load(&g_unity_fps);
}

JNIEXPORT jint JNICALL
Java_com_cetotos_polydroid2_GameActivity_nativeGetTotalFrames(
    JNIEnv* env, jobject thiz)
{
    return (jint)atomic_load(&g_comp_total_frames);
}

JNIEXPORT jstring JNICALL
Java_com_cetotos_polydroid2_GameActivity_nativeGetVulkanInfo(
    JNIEnv* env, jobject thiz)
{
    return (*env)->NewStringUTF(env, g_vulkan_info);
}

// -------------------- input injection -----------
static int g_input_sock = -1;
static struct sockaddr_un g_input_addr;
static socklen_t g_input_addr_len = 0;

struct polydroid_input_msg {
    uint8_t  type;
    uint8_t  button;
    uint16_t pad;
    int32_t  x;
    int32_t  y;
    uint32_t time_ms;
};

// ..im not gonna lie, i just made ai do this. im not looking up all the keycodes myself
static const int android_to_linux_keycode[304] = {
    [ 4  ] = 1,    /* BACK → KEY_ESC */
    [ 7  ] = 11,   /* 0 → KEY_0 */
    [ 8  ] = 2,    /* 1 → KEY_1 */
    [ 9  ] = 3,    /* 2 → KEY_2 */
    [ 10 ] = 4,    /* 3 → KEY_3 */
    [ 11 ] = 5,    /* 4 → KEY_4 */
    [ 12 ] = 6,    /* 5 → KEY_5 */
    [ 13 ] = 7,    /* 6 → KEY_6 */
    [ 14 ] = 8,    /* 7 → KEY_7 */
    [ 15 ] = 9,    /* 8 → KEY_8 */
    [ 16 ] = 10,   /* 9 → KEY_9 */
    [ 19 ] = 103,  /* DPAD_UP → KEY_UP */
    [ 20 ] = 108,  /* DPAD_DOWN → KEY_DOWN */
    [ 21 ] = 105,  /* DPAD_LEFT → KEY_LEFT */
    [ 22 ] = 106,  /* DPAD_RIGHT → KEY_RIGHT */
    [ 23 ] = 28,   /* DPAD_CENTER → KEY_ENTER */
    [ 29 ] = 30,   /* A */
    [ 30 ] = 48,   /* B */
    [ 31 ] = 46,   /* C */
    [ 32 ] = 32,   /* D */
    [ 33 ] = 18,   /* E */
    [ 34 ] = 33,   /* F */
    [ 35 ] = 34,   /* G */
    [ 36 ] = 35,   /* H */
    [ 37 ] = 23,   /* I */
    [ 38 ] = 36,   /* J */
    [ 39 ] = 37,   /* K */
    [ 40 ] = 38,   /* L */
    [ 41 ] = 50,   /* M */
    [ 42 ] = 49,   /* N */
    [ 43 ] = 24,   /* O */
    [ 44 ] = 25,   /* P */
    [ 45 ] = 16,   /* Q */
    [ 46 ] = 19,   /* R */
    [ 47 ] = 31,   /* S */
    [ 48 ] = 20,   /* T */
    [ 49 ] = 22,   /* U */
    [ 50 ] = 47,   /* V */
    [ 51 ] = 17,   /* W */
    [ 52 ] = 45,   /* X */
    [ 53 ] = 21,   /* Y */
    [ 54 ] = 44,   /* Z */
    [ 55 ] = 51,   /* COMMA */
    [ 56 ] = 52,   /* PERIOD */
    [ 57 ] = 56,   /* ALT_LEFT */
    [ 58 ] = 100,  /* ALT_RIGHT */
    [ 59 ] = 42,   /* SHIFT_LEFT */
    [ 60 ] = 54,   /* SHIFT_RIGHT */
    [ 61 ] = 15,   /* TAB */
    [ 62 ] = 57,   /* SPACE */
    [ 66 ] = 28,   /* ENTER */
    [ 67 ] = 14,   /* DEL → BACKSPACE */
    [ 68 ] = 41,   /* GRAVE */
    [ 69 ] = 12,   /* MINUS */
    [ 70 ] = 13,   /* EQUALS */
    [ 71 ] = 26,   /* LEFT_BRACKET */
    [ 72 ] = 27,   /* RIGHT_BRACKET */
    [ 73 ] = 43,   /* BACKSLASH */
    [ 74 ] = 39,   /* SEMICOLON */
    [ 75 ] = 40,   /* APOSTROPHE */
    [ 76 ] = 53,   /* SLASH */
    [ 92 ] = 104,  /* PAGE_UP */
    [ 93 ] = 109,  /* PAGE_DOWN */
    [ 111] = 1,    /* ESCAPE */
    [ 112] = 111,  /* FORWARD_DEL → DELETE */
    [ 113] = 29,   /* CTRL_LEFT */
    [ 114] = 97,   /* CTRL_RIGHT */
    [ 115] = 58,   /* CAPS_LOCK */
    [ 116] = 70,   /* SCROLL_LOCK */
    [ 117] = 125,  /* META_LEFT */
    [ 118] = 126,  /* META_RIGHT */
    [ 122] = 102,  /* MOVE_HOME → HOME */
    [ 123] = 107,  /* MOVE_END → END */
    [ 124] = 110,  /* INSERT */
    [ 131] = 59,   /* F1 */
    [ 132] = 60,   /* F2 */
    [ 133] = 61,   /* F3 */
    [ 134] = 62,   /* F4 */
    [ 135] = 63,   /* F5 */
    [ 136] = 64,   /* F6 */
    [ 137] = 65,   /* F7 */
    [ 138] = 66,   /* F8 */
    [ 139] = 67,   /* F9 */
    [ 140] = 68,   /* F10 */
    [ 141] = 87,   /* F11 */
    [ 142] = 88,   /* F12 */
    [ 143] = 69,   /* NUM_LOCK */
};

JNIEXPORT void JNICALL
Java_com_cetotos_polydroid2_GameActivity_nativeSendInputEvent(
    JNIEnv* env, jobject thiz, jint type, jint button, jint x, jint y)
{
    if (g_input_sock < 0) {
        g_input_sock = socket(AF_UNIX, SOCK_DGRAM, 0);
        if (g_input_sock < 0) {
            LOGE("input socket create failed: %s", strerror(errno));
            return;
        }
        memset(&g_input_addr, 0, sizeof(g_input_addr));
        g_input_addr.sun_family = AF_UNIX;
        g_input_addr.sun_path[0] = '\0';
        memcpy(g_input_addr.sun_path + 1, "polydroid_input", 15);
        g_input_addr_len = offsetof(struct sockaddr_un, sun_path) + 1 + 15;
        LOGI("input socket created (fd=%d)", g_input_sock);
    }

    struct polydroid_input_msg msg;
    msg.type = (uint8_t)type;
    msg.button = (uint8_t)button;
    msg.pad = 0;
    msg.x = x;
    msg.y = y;
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    msg.time_ms = (uint32_t)(ts.tv_sec * 1000 + ts.tv_nsec / 1000000);

    ssize_t n = sendto(g_input_sock, &msg, sizeof(msg), 0,
                       (struct sockaddr*)&g_input_addr, g_input_addr_len);
    if (n < 0) {
        LOGE("input sendto failed: %s", strerror(errno));
    }
}

JNIEXPORT void JNICALL
Java_com_cetotos_polydroid2_GameActivity_nativeSendKeyEvent(
    JNIEnv* env, jobject thiz, jint scanCode, jint keyCode, jboolean keyDown)
{
    if (g_input_sock < 0) {
        g_input_sock = socket(AF_UNIX, SOCK_DGRAM, 0);
        if (g_input_sock < 0) {
            LOGE("input socket create failed: %s", strerror(errno));
            return;
        }
        memset(&g_input_addr, 0, sizeof(g_input_addr));
        g_input_addr.sun_family = AF_UNIX;
        g_input_addr.sun_path[0] = '\0';
        memcpy(g_input_addr.sun_path + 1, "polydroid_input", 15);
        g_input_addr_len = offsetof(struct sockaddr_un, sun_path) + 1 + 15;
        LOGI("input socket created (fd=%d)", g_input_sock);
    }

    // Android keycode --> X11 keycode (its just X11 keycode + 8)
    int linux_code = (scanCode != 0) ? scanCode :
                     (keyCode >= 0 && keyCode < 304) ? android_to_linux_keycode[keyCode] : 0;
    if (linux_code == 0) {
        LOGE("unmapped android keyCode=%d scanCode=%d", keyCode, scanCode);
        return;
    }
    int x11_keycode = linux_code + 8;

    struct polydroid_input_msg msg;
    msg.type = keyDown ? 4 : 5;
    msg.button = 0;
    msg.pad = 0;
    msg.x = x11_keycode;
    msg.y = 0;
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    msg.time_ms = (uint32_t)(ts.tv_sec * 1000 + ts.tv_nsec / 1000000);

    static int key_log = 0;
    ssize_t n = sendto(g_input_sock, &msg, sizeof(msg), 0,
                       (struct sockaddr*)&g_input_addr, g_input_addr_len);
    if (n < 0) {
        LOGE("key sendto failed: %s", strerror(errno));
    } else if (key_log < 67 /* boii😂 */) {
        LOGI("key sent: scanCode=%d x11=%d down=%d n=%zd", linux_code, x11_keycode, keyDown, n);
        key_log++;
    }
}

//finally done