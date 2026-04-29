/*
 * vulkan_surface_shim.so
 * this took a really, really long time. most of the project is just this file and the x11 stub.
 */

#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <unistd.h>
#include <stdarg.h>
#include <time.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <errno.h>
#include <android/native_window.h>
#include <android/hardware_buffer.h>
#include <android/log.h>

typedef struct native_handle {
    int version;
    int numFds;
    int numInts;
    int data[0];
} native_handle_t;
typedef const native_handle_t* (*PFN_AHardwareBuffer_getNativeHandle)(const AHardwareBuffer*);
static PFN_AHardwareBuffer_getNativeHandle pfn_ahb_getNativeHandle = NULL;
#include <vulkan/vulkan.h>
#include <vulkan/vulkan_android.h>

#ifndef VK_IMAGE_TILING_DRM_FORMAT_MODIFIER_EXT
#define VK_IMAGE_TILING_DRM_FORMAT_MODIFIER_EXT ((VkImageTiling)1000158000)
#endif
#ifndef VK_STRUCTURE_TYPE_IMAGE_DRM_FORMAT_MODIFIER_EXPLICIT_CREATE_INFO_EXT
#define VK_STRUCTURE_TYPE_IMAGE_DRM_FORMAT_MODIFIER_EXPLICIT_CREATE_INFO_EXT ((VkStructureType)1000158004)
#endif

#ifndef AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM
#define AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM 1
#endif
#ifndef AHARDWAREBUFFER_FORMAT_R5G6B5_UNORM
#define AHARDWAREBUFFER_FORMAT_R5G6B5_UNORM 4
#endif
#ifndef AHARDWAREBUFFER_FORMAT_R16G16B16A16_FLOAT
#define AHARDWAREBUFFER_FORMAT_R16G16B16A16_FLOAT 0x16
#endif
#ifndef AHARDWAREBUFFER_FORMAT_R10G10B10A2_UNORM
#define AHARDWAREBUFFER_FORMAT_R10G10B10A2_UNORM 0x2b
#endif

#define TAG "PolyDroid2-Vulkan"

static void shim_log(const char* level, const char* fmt, ...) {
    char buf[512];
    int off = snprintf(buf, sizeof(buf), "[%s] [%s] ", TAG, level);
    va_list ap;
    va_start(ap, fmt);
    off += vsnprintf(buf + off, sizeof(buf) - off, fmt, ap);
    va_end(ap);
    if (off > 0 && buf[off-1] != '\n') { buf[off++] = '\n'; }
    write(STDERR_FILENO, buf, off);
}

#define LOGI(...) shim_log("I", __VA_ARGS__)
#define LOGE(...) shim_log("E", __VA_ARGS__)

static long g_max_fps_interval_ns = 0;

__attribute__((constructor))
void shim_startup_log(void) {
    const char* e = getenv("POLYDROID_MAX_FPS");
    int fps = (e && *e) ? atoi(e) : 0;
    g_max_fps_interval_ns = (fps > 0) ? (1000000000L / fps) : 0;
    __android_log_print(ANDROID_LOG_INFO, "PolyDroid2-Vulkan",
        "shim loaded, POLYDROID_MAX_FPS='%s' -> fps=%d interval_ns=%ld",
        e ? e : "(unset)", fps, g_max_fps_interval_ns);
    LOGI("shim loaded, POLYDROID_MAX_FPS='%s' -> fps=%d interval_ns=%ld",
         e ? e : "(unset)", fps, g_max_fps_interval_ns);
}

static char g_gpu_name[64] = "";
static uint32_t g_vk_api_version = 0;
static uint32_t g_vk_driver_version = 0;

#define SHIM_SURFACE_HANDLE ((VkSurfaceKHR)(uintptr_t)0xDEAD5F01)

typedef void (*PFN_ANativeWindow_acquire)(void*);
static PFN_ANativeWindow_acquire pfn_ANativeWindow_acquire = NULL;

static void load_nativewindow(void) {
    if (pfn_ANativeWindow_acquire) return;
    void* lib = dlopen("libnativewindow.so", RTLD_NOW);
    if (!lib) lib = dlopen("libandroid.so", RTLD_NOW);
    if (lib) {
        pfn_ANativeWindow_acquire = (PFN_ANativeWindow_acquire)dlsym(lib, "ANativeWindow_acquire");
    }
    if (!pfn_ANativeWindow_acquire) {
        LOGE("Failed to resolve ANativeWindow_acquire: %s", dlerror());
    }
}

static void* g_real_vulkan = NULL;
static int g_using_system_driver = 0;

static PFN_vkGetInstanceProcAddr real_vkGetInstanceProcAddr = NULL;
static PFN_vkGetDeviceProcAddr real_vkGetDeviceProcAddr = NULL;
static PFN_vkCreateInstance real_vkCreateInstance = NULL;
static PFN_vkEnumerateInstanceExtensionProperties real_vkEnumerateInstanceExtensionProperties = NULL;

static VkInstance g_instance = VK_NULL_HANDLE;

static ANativeWindow* g_native_window = NULL;
static int g_window_loaded = 0;

// detect GPU vendor with EGL to decide if Turnip or system driver
static int detect_adreno_gpu(void) {
    void* libEGL = dlopen("libEGL.so", RTLD_NOW);
    void* libGLES = dlopen("libGLESv2.so", RTLD_NOW);
    if (!libEGL || !libGLES) {
        if (libEGL) dlclose(libEGL);
        if (libGLES) dlclose(libGLES);
        return 0;
    }

    typedef void* EGLDisplay;
    typedef void* EGLConfig;
    typedef void* EGLSurface;
    typedef void* EGLContext;
    typedef unsigned int EGLBoolean;
    typedef int EGLint;

    #define MY_EGL_DEFAULT_DISPLAY ((void*)0)
    #define MY_EGL_NO_CONTEXT ((EGLContext)0)
    #define MY_EGL_NO_SURFACE ((EGLSurface)0)
    #define MY_EGL_NO_DISPLAY ((EGLDisplay)0)
    #define MY_EGL_RENDERABLE_TYPE 0x3040
    #define MY_EGL_OPENGL_ES2_BIT 0x0004
    #define MY_EGL_SURFACE_TYPE 0x3033
    #define MY_EGL_PBUFFER_BIT 0x0001
    #define MY_EGL_NONE 0x3038
    #define MY_EGL_WIDTH 0x3057
    #define MY_EGL_HEIGHT 0x3056
    #define MY_EGL_CONTEXT_CLIENT_VERSION 0x3098
    #define MY_GL_RENDERER 0x1F01

    typedef EGLDisplay (*PFN_eglGetDisplay)(void*);
    typedef EGLBoolean (*PFN_eglInitialize)(EGLDisplay, EGLint*, EGLint*);
    typedef EGLBoolean (*PFN_eglChooseConfig)(EGLDisplay, const EGLint*, EGLConfig*, EGLint, EGLint*);
    typedef EGLSurface (*PFN_eglCreatePbufferSurface)(EGLDisplay, EGLConfig, const EGLint*);
    typedef EGLContext (*PFN_eglCreateContext)(EGLDisplay, EGLConfig, EGLContext, const EGLint*);
    typedef EGLBoolean (*PFN_eglMakeCurrent)(EGLDisplay, EGLSurface, EGLSurface, EGLContext);
    typedef EGLBoolean (*PFN_eglDestroyContext)(EGLDisplay, EGLContext);
    typedef EGLBoolean (*PFN_eglDestroySurface)(EGLDisplay, EGLSurface);
    typedef EGLBoolean (*PFN_eglTerminate)(EGLDisplay);
    typedef const unsigned char* (*PFN_glGetString)(unsigned int);

    PFN_eglGetDisplay pfn_eglGetDisplay = (PFN_eglGetDisplay)dlsym(libEGL, "eglGetDisplay");
    PFN_eglInitialize pfn_eglInitialize = (PFN_eglInitialize)dlsym(libEGL, "eglInitialize");
    PFN_eglChooseConfig pfn_eglChooseConfig = (PFN_eglChooseConfig)dlsym(libEGL, "eglChooseConfig");
    PFN_eglCreatePbufferSurface pfn_eglCreatePbufferSurface = (PFN_eglCreatePbufferSurface)dlsym(libEGL, "eglCreatePbufferSurface");
    PFN_eglCreateContext pfn_eglCreateContext = (PFN_eglCreateContext)dlsym(libEGL, "eglCreateContext");
    PFN_eglMakeCurrent pfn_eglMakeCurrent = (PFN_eglMakeCurrent)dlsym(libEGL, "eglMakeCurrent");
    PFN_eglDestroyContext pfn_eglDestroyContext = (PFN_eglDestroyContext)dlsym(libEGL, "eglDestroyContext");
    PFN_eglDestroySurface pfn_eglDestroySurface = (PFN_eglDestroySurface)dlsym(libEGL, "eglDestroySurface");
    PFN_eglTerminate pfn_eglTerminate = (PFN_eglTerminate)dlsym(libEGL, "eglTerminate");
    PFN_glGetString pfn_glGetString = (PFN_glGetString)dlsym(libGLES, "glGetString");

    if (!pfn_eglGetDisplay || !pfn_eglInitialize || !pfn_eglChooseConfig ||
        !pfn_eglCreatePbufferSurface || !pfn_eglCreateContext || !pfn_eglMakeCurrent ||
        !pfn_glGetString) {
        LOGI("shim: EGL symbols not available, assuming non-Adreno");
        dlclose(libEGL); dlclose(libGLES);
        return 0;
    }

    int result = 0;
    EGLDisplay display = pfn_eglGetDisplay(MY_EGL_DEFAULT_DISPLAY);
    if (display == MY_EGL_NO_DISPLAY) { dlclose(libEGL); dlclose(libGLES); return 0; }

    EGLint major, minor;
    if (!pfn_eglInitialize(display, &major, &minor)) { dlclose(libEGL); dlclose(libGLES); return 0; }

    EGLint configAttribs[] = { MY_EGL_RENDERABLE_TYPE, MY_EGL_OPENGL_ES2_BIT,
                               MY_EGL_SURFACE_TYPE, MY_EGL_PBUFFER_BIT, MY_EGL_NONE };
    EGLConfig config;
    EGLint numConfigs;
    if (!pfn_eglChooseConfig(display, configAttribs, &config, 1, &numConfigs) || numConfigs == 0) {
        if (pfn_eglTerminate) pfn_eglTerminate(display);
        dlclose(libEGL); dlclose(libGLES);
        return 0;
    }

    EGLint pbufAttribs[] = { MY_EGL_WIDTH, 1, MY_EGL_HEIGHT, 1, MY_EGL_NONE };
    EGLSurface surface = pfn_eglCreatePbufferSurface(display, config, pbufAttribs);
    if (surface == MY_EGL_NO_SURFACE) {
        if (pfn_eglTerminate) pfn_eglTerminate(display);
        dlclose(libEGL); dlclose(libGLES);
        return 0;
    }

    EGLint ctxAttribs[] = { MY_EGL_CONTEXT_CLIENT_VERSION, 2, MY_EGL_NONE };
    EGLContext context = pfn_eglCreateContext(display, config, MY_EGL_NO_CONTEXT, ctxAttribs);
    if (context == MY_EGL_NO_CONTEXT) {
        if (pfn_eglDestroySurface) pfn_eglDestroySurface(display, surface);
        if (pfn_eglTerminate) pfn_eglTerminate(display);
        dlclose(libEGL); dlclose(libGLES);
        return 0;
    }

    pfn_eglMakeCurrent(display, surface, surface, context);
    const char* renderer = (const char*)pfn_glGetString(MY_GL_RENDERER);
    if (renderer) {
        LOGI("shim: GL_RENDERER = %s", renderer);
        const char* r = renderer;
        while (*r) {
            if ((r[0] == 'A' || r[0] == 'a') &&
                (r[1] == 'D' || r[1] == 'd') &&
                (r[2] == 'R' || r[2] == 'r') &&
                (r[3] == 'E' || r[3] == 'e') &&
                (r[4] == 'N' || r[4] == 'n') &&
                (r[5] == 'O' || r[5] == 'o')) {
                result = 1;
                break;
            }
            r++;
        }
    }

    pfn_eglMakeCurrent(display, MY_EGL_NO_SURFACE, MY_EGL_NO_SURFACE, MY_EGL_NO_CONTEXT);
    if (pfn_eglDestroyContext) pfn_eglDestroyContext(display, context);
    if (pfn_eglDestroySurface) pfn_eglDestroySurface(display, surface);
    if (pfn_eglTerminate) pfn_eglTerminate(display);
    dlclose(libEGL);
    dlclose(libGLES);

    LOGI("shim: GPU vendor: %s", result ? "Adreno (will use Turnip)" : "non-Adreno (will use system Vulkan)");
    return result;
}

static void load_real_vulkan(void) {
    if (g_real_vulkan) return;

    const char* force_system = getenv("POLYDROID_FORCE_SYSTEM_DRIVER");
    const char* force_turnip = getenv("POLYDROID_FORCE_TURNIP");

    int use_turnip;
    if (force_system && force_system[0] == '1') {
        LOGI("shim: forced to system driver by setting");
        use_turnip = 0;
    } else if (force_turnip && force_turnip[0] == '1') {
        LOGI("shim: forced to Turnip by setting");
        use_turnip = 1;
    } else {
        use_turnip = detect_adreno_gpu();
    }

    if (use_turnip) {
        const char* rootdir = getenv("POLYDROID_ROOTDIR");
        if (!rootdir) {
            LOGE("POLYDROID_ROOTDIR not set");
            return;
        }

        char path[512];
        snprintf(path, sizeof(path), "%s/usr/lib/arm64-native/libc++_shared.so", rootdir);
        dlopen(path, RTLD_NOW | RTLD_GLOBAL);
        snprintf(path, sizeof(path), "%s/usr/lib/arm64-native/libdrm.so.2", rootdir);
        dlopen(path, RTLD_NOW | RTLD_GLOBAL);

        snprintf(path, sizeof(path), "%s/usr/lib/arm64-native/libvulkan_freedreno.so", rootdir);
        LOGI("shim: loading Turnip from %s", path);
        g_real_vulkan = dlopen(path, RTLD_NOW | RTLD_LOCAL);
        if (g_real_vulkan) {
            LOGI("shim: Turnip loaded");
        } else {
            LOGI("shim: Turnip load failed: %s, falling back to system Vulkan", dlerror());
        }
    }

    if (!g_real_vulkan) {
        g_real_vulkan = dlopen("libvulkan.so", RTLD_NOW | RTLD_LOCAL);
        if (g_real_vulkan) {
            g_using_system_driver = 1;
            LOGI("shim: system Vulkan driver loaded");
        } else {
            LOGE("shim: system Vulkan driver load failed: %s", dlerror());
            return;
        }
    }

    real_vkGetInstanceProcAddr = (PFN_vkGetInstanceProcAddr)
        dlsym(g_real_vulkan, "vkGetInstanceProcAddr");
    if (!real_vkGetInstanceProcAddr)
        real_vkGetInstanceProcAddr = (PFN_vkGetInstanceProcAddr)
            dlsym(g_real_vulkan, "vk_icdGetInstanceProcAddr");

    if (!real_vkGetInstanceProcAddr) {
        LOGI("shim: trying HMI...");
        typedef struct hw_module_t {
            uint32_t tag;
            uint16_t module_api_version;
            uint16_t hal_api_version;
            const char *id;
            const char *name;
            const char *author;
            struct hw_module_methods_t *methods;
            void *dso;
            uint32_t reserved[32 - 7];
        } hw_module_t;
        typedef struct hw_module_methods_t {
            int (*open)(const struct hw_module_t* module, const char* id,
                        struct hw_device_t** device);
        } hw_module_methods_t;
        typedef struct hw_device_t {
            uint32_t tag;
            uint32_t version;
            struct hw_module_t* module;
            uint64_t reserved[12];
            int (*close)(struct hw_device_t* device);
        } hw_device_t;
        typedef struct {
            hw_device_t common;
            PFN_vkEnumerateInstanceExtensionProperties EnumerateInstanceExtensionProperties;
            PFN_vkCreateInstance CreateInstance;
            PFN_vkGetInstanceProcAddr GetInstanceProcAddr;
        } hwvulkan_device_t;

        hw_module_t* hmi = (hw_module_t*)dlsym(g_real_vulkan, "HMI");
        if (hmi && hmi->methods && hmi->methods->open) {
            LOGI("HMI: tag=0x%x api=%u.%u methods=%p open=%p",
                 hmi->tag, hmi->module_api_version, hmi->hal_api_version,
                 (void*)hmi->methods, (void*)hmi->methods->open);
            hw_device_t* dev = NULL;
            int rc = hmi->methods->open(hmi, "vk0", &dev);
            LOGI("HAL open returned rc=%d dev=%p", rc, (void*)dev);
            if (rc == 0 && dev) {
                hwvulkan_device_t* vkdev = (hwvulkan_device_t*)dev;
                LOGI("HAL dev: tag=0x%x ver=0x%x close=%p",
                     dev->tag, dev->version, (void*)dev->close);
                LOGI("HAL vkdev: EnumExt=%p CreateInst=%p GetProcAddr=%p",
                     (void*)vkdev->EnumerateInstanceExtensionProperties,
                     (void*)vkdev->CreateInstance,
                     (void*)vkdev->GetInstanceProcAddr);
                real_vkGetInstanceProcAddr = vkdev->GetInstanceProcAddr;
            } else {
                LOGE("HAL open failed: rc=%d dev=%p", rc, (void*)dev);
            }
        } else {
            LOGE("HMI=%p methods=%p", (void*)hmi,
                 hmi ? (void*)hmi->methods : NULL);
        }
    }

    if (!real_vkGetInstanceProcAddr) {
        LOGE("No vkGetInstanceProcAddr from any method (handle=%p)", g_real_vulkan);
        return;
    }
    LOGI("Got vkGetInstanceProcAddr: %p", (void*)real_vkGetInstanceProcAddr);

    real_vkCreateInstance = (PFN_vkCreateInstance)
        real_vkGetInstanceProcAddr(NULL, "vkCreateInstance");
    real_vkEnumerateInstanceExtensionProperties =
        (PFN_vkEnumerateInstanceExtensionProperties)
        real_vkGetInstanceProcAddr(NULL, "vkEnumerateInstanceExtensionProperties");

    LOGI("Real Vulkan driver loaded successfully (vkCreateInstance=%p, vkEnumExt=%p)",
         (void*)real_vkCreateInstance, (void*)real_vkEnumerateInstanceExtensionProperties);
}

static ANativeWindow* get_native_window(void) {
    if (g_native_window) return g_native_window;
    if (g_window_loaded) return NULL;
    g_window_loaded = 1;

    const char* ptrStr = getenv("POLYDROID_VULKAN_SURFACE_PTR");
    if (ptrStr && ptrStr[0]) {
        unsigned long long ptr = strtoull(ptrStr, NULL, 16);
        g_native_window = (ANativeWindow*)(uintptr_t)ptr;
        LOGI("ANativeWindow pointer stored (cross-process): %p", g_native_window);
        return g_native_window;
    }

    LOGE("POLYDROID_VULKAN_SURFACE_PTR not set");
    return NULL;
}

// intercept vkCreateInstance and replace VK_KHR_xlib_surface with VK_KHR_android_surface
static VkResult shim_vkCreateInstance(
    const VkInstanceCreateInfo* pCreateInfo,
    const VkAllocationCallbacks* pAllocator,
    VkInstance* pInstance)
{
    load_real_vulkan();
    if (!real_vkCreateInstance) return VK_ERROR_INITIALIZATION_FAILED;

    uint32_t count = pCreateInfo->enabledExtensionCount;
    const char** newExts = (const char**)malloc(sizeof(char*) * (count + 1));
    uint32_t newCount = 0;

    for (uint32_t i = 0; i < count; i++) {
        const char* ext = pCreateInfo->ppEnabledExtensionNames[i];
        if (strcmp(ext, "VK_KHR_surface") == 0 ||
            strcmp(ext, "VK_KHR_xlib_surface") == 0 ||
            strcmp(ext, "VK_KHR_xcb_surface") == 0 ||
            strcmp(ext, "VK_KHR_wayland_surface") == 0 ||
            strcmp(ext, "VK_KHR_android_surface") == 0 ||
            strcmp(ext, "VK_EXT_swapchain_colorspace") == 0 ||
            strcmp(ext, "VK_KHR_get_surface_capabilities2") == 0) {
            LOGI("Stripping %s (handled by shim, not real driver)", ext);
            continue;
        }
        LOGI("  ext[%u]: %s", newCount, ext);
        newExts[newCount++] = ext;
    }

    VkInstanceCreateInfo modifiedInfo = *pCreateInfo;
    modifiedInfo.ppEnabledExtensionNames = newExts;
    modifiedInfo.enabledExtensionCount = newCount;

    LOGI("Creating Vulkan instance with %u extensions", newCount);
    VkResult result = real_vkCreateInstance(&modifiedInfo, pAllocator, pInstance);
    free(newExts);

    if (result == VK_SUCCESS && *pInstance) {
        g_instance = *pInstance;
        LOGI("Vulkan instance created OK");
    } else {
        LOGE("vkCreateInstance failed: %d", result);
    }

    return result;
}

// vkCreateXlibSurfaceKHR --> shim surface
static VkResult shim_vkCreateXlibSurfaceKHR(
    VkInstance instance,
    const void* pCreateInfo,
    const VkAllocationCallbacks* pAllocator,
    VkSurfaceKHR* pSurface)
{
    LOGI("vkCreateXlibSurfaceKHR intercepted");

    ANativeWindow* nw = get_native_window();
    if (nw) {
        LOGI("ANativeWindow available: %p", nw);
    } else {
        LOGE("WARNING: No ANativeWindow, presentation will fail");
    }

    *pSurface = SHIM_SURFACE_HANDLE;
    LOGI("Created shim surface: %p", (void*)(uintptr_t)*pSurface);
    return VK_SUCCESS;
}

static VkResult shim_vkCreateXcbSurfaceKHR(
    VkInstance instance,
    const void* pCreateInfo,
    const VkAllocationCallbacks* pAllocator,
    VkSurfaceKHR* pSurface)
{
    LOGI("vkCreateXcbSurfaceKHR intercepted");
    return shim_vkCreateXlibSurfaceKHR(instance, pCreateInfo, pAllocator, pSurface);
}

static VkResult shim_vkCreateWaylandSurfaceKHR(
    VkInstance instance,
    const void* pCreateInfo,
    const VkAllocationCallbacks* pAllocator,
    VkSurfaceKHR* pSurface)
{
    LOGI("vkCreateWaylandSurfaceKHR intercepted");
    return shim_vkCreateXlibSurfaceKHR(instance, pCreateInfo, pAllocator, pSurface);
}

static void shim_vkDestroySurfaceKHR(
    VkInstance instance,
    VkSurfaceKHR surface,
    const VkAllocationCallbacks* pAllocator)
{
    LOGI("vkDestroySurfaceKHR (shim, no-op)");
}

static VkResult shim_vkGetPhysicalDeviceSurfaceSupportKHR(
    VkPhysicalDevice physicalDevice,
    uint32_t queueFamilyIndex,
    VkSurfaceKHR surface,
    VkBool32* pSupported)
{
    *pSupported = VK_TRUE;
    return VK_SUCCESS;
}

static VkResult shim_vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
    VkPhysicalDevice physicalDevice,
    VkSurfaceKHR surface,
    VkSurfaceCapabilitiesKHR* pCapabilities)
{
    uint32_t w = 3120, h = 1440;
    const char* sw = getenv("POLYDROID_SCREEN_WIDTH");
    const char* sh = getenv("POLYDROID_SCREEN_HEIGHT");
    if (sw) w = (uint32_t)atoi(sw);
    if (sh) h = (uint32_t)atoi(sh);

    *pCapabilities = (VkSurfaceCapabilitiesKHR){
        .minImageCount = 3,
        .maxImageCount = 3,
        .currentExtent = { w, h },
        .minImageExtent = { w, h },
        .maxImageExtent = { w, h },
        .maxImageArrayLayers = 1,
        .supportedTransforms = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR,
        .currentTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR,
        .supportedCompositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR,
        .supportedUsageFlags = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT |
                               VK_IMAGE_USAGE_TRANSFER_DST_BIT |
                               VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
    };
    LOGI("SurfaceCapabilities: %ux%u, %u-%u images", w, h,
         pCapabilities->minImageCount, pCapabilities->maxImageCount);
    return VK_SUCCESS;
}

static VkResult shim_vkGetPhysicalDeviceSurfaceFormatsKHR(
    VkPhysicalDevice physicalDevice,
    VkSurfaceKHR surface,
    uint32_t* pSurfaceFormatCount,
    VkSurfaceFormatKHR* pSurfaceFormats)
{
    static const VkSurfaceFormatKHR formats[] = {
        { VK_FORMAT_R8G8B8A8_UNORM, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR },
        { VK_FORMAT_R8G8B8A8_SRGB,  VK_COLOR_SPACE_SRGB_NONLINEAR_KHR },
        { VK_FORMAT_B8G8R8A8_UNORM, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR },
        { VK_FORMAT_B8G8R8A8_SRGB,  VK_COLOR_SPACE_SRGB_NONLINEAR_KHR },
    };
    uint32_t count = sizeof(formats) / sizeof(formats[0]);

    if (!pSurfaceFormats) {
        *pSurfaceFormatCount = count;
        return VK_SUCCESS;
    }
    uint32_t copy = count < *pSurfaceFormatCount ? count : *pSurfaceFormatCount;
    memcpy(pSurfaceFormats, formats, copy * sizeof(VkSurfaceFormatKHR));
    *pSurfaceFormatCount = copy;
    return (copy < count) ? VK_INCOMPLETE : VK_SUCCESS;
}

static VkResult shim_vkGetPhysicalDeviceSurfacePresentModesKHR(
    VkPhysicalDevice physicalDevice,
    VkSurfaceKHR surface,
    uint32_t* pPresentModeCount,
    VkPresentModeKHR* pPresentModes)
{
    static const VkPresentModeKHR modes[] = {
        VK_PRESENT_MODE_FIFO_KHR,
        VK_PRESENT_MODE_MAILBOX_KHR,
    };
    uint32_t count = sizeof(modes) / sizeof(modes[0]);

    if (!pPresentModes) {
        *pPresentModeCount = count;
        return VK_SUCCESS;
    }
    uint32_t copy = count < *pPresentModeCount ? count : *pPresentModeCount;
    memcpy(pPresentModes, modes, copy * sizeof(VkPresentModeKHR));
    *pPresentModeCount = copy;
    return (copy < count) ? VK_INCOMPLETE : VK_SUCCESS;
}

static VkResult shim_vkEnumerateInstanceExtensionProperties(
    const char* pLayerName,
    uint32_t* pPropertyCount,
    VkExtensionProperties* pProperties)
{
    load_real_vulkan();
    if (!real_vkEnumerateInstanceExtensionProperties)
        return VK_ERROR_INITIALIZATION_FAILED;

    uint32_t realCount = 0;
    VkResult result = real_vkEnumerateInstanceExtensionProperties(pLayerName, &realCount, NULL);
    if (result != VK_SUCCESS) return result;

    // VK_KHR_surface is required by xlib surface extensions
    static const char* fakeExts[] = {
        "VK_KHR_surface",
        "VK_KHR_xlib_surface",
        "VK_KHR_xcb_surface",
        "VK_KHR_get_surface_capabilities2",
    };
    static const uint32_t numFake = sizeof(fakeExts) / sizeof(fakeExts[0]);

    VkExtensionProperties* realProps = NULL;
    uint32_t extraCount = numFake;
    if (realCount > 0) {
        realProps = (VkExtensionProperties*)malloc(sizeof(VkExtensionProperties) * realCount);
        uint32_t rc = realCount;
        real_vkEnumerateInstanceExtensionProperties(pLayerName, &rc, realProps);
        for (uint32_t f = 0; f < numFake; f++) {
            for (uint32_t r = 0; r < rc; r++) {
                if (strcmp(fakeExts[f], realProps[r].extensionName) == 0) {
                    extraCount--;
                    break;
                }
            }
        }
    }

    uint32_t totalCount = realCount + extraCount;

    if (!pProperties) {
        free(realProps);
        *pPropertyCount = totalCount;
        return VK_SUCCESS;
    }

    uint32_t copyCount = realCount < *pPropertyCount ? realCount : *pPropertyCount;
    result = real_vkEnumerateInstanceExtensionProperties(pLayerName, &copyCount, pProperties);

    for (uint32_t f = 0; f < numFake && copyCount < *pPropertyCount; f++) {
        int already = 0;
        if (realProps) {
            for (uint32_t r = 0; r < realCount; r++) {
                if (strcmp(fakeExts[f], realProps[r].extensionName) == 0) {
                    already = 1;
                    break;
                }
            }
        }
        if (!already) {
            strncpy(pProperties[copyCount].extensionName, fakeExts[f],
                    VK_MAX_EXTENSION_NAME_SIZE);
            pProperties[copyCount].specVersion = 6;
            copyCount++;
        }
    }

    free(realProps);
    *pPropertyCount = copyCount;
    return (copyCount < totalCount) ? VK_INCOMPLETE : VK_SUCCESS;
}

// always true
static VkBool32 shim_vkGetPhysicalDevicePresentationSupport(
    VkPhysicalDevice physicalDevice,
    uint32_t queueFamilyIndex,
    void* dpy_or_connection,
    uint32_t visualID_or_ignored)
{
    return VK_TRUE;
}

// ------------------------------- swapchain ------------
static uint64_t g_swapchain_handle_counter = 0xDEAD5C00;
#define SHIM_NEXT_SWAPCHAIN_HANDLE() ((VkSwapchainKHR)(uintptr_t)(++g_swapchain_handle_counter))
#define SHIM_MAX_SWAPCHAIN_IMAGES 3

static VkDevice g_device = VK_NULL_HANDLE;
static VkImage g_swapchain_images[SHIM_MAX_SWAPCHAIN_IMAGES];
static VkDeviceMemory g_swapchain_memory[SHIM_MAX_SWAPCHAIN_IMAGES];
static int g_swapchain_dmabuf_fds[SHIM_MAX_SWAPCHAIN_IMAGES] = {-1, -1, -1};
static AHardwareBuffer* g_swapchain_ahbs[SHIM_MAX_SWAPCHAIN_IMAGES] = {NULL, NULL, NULL};
static uint32_t g_swapchain_strides[SHIM_MAX_SWAPCHAIN_IMAGES];
static int g_swapchain_has_dmabuf = 0;
static uint32_t g_swapchain_image_count = 0;

static VkImage g_pending_images[SHIM_MAX_SWAPCHAIN_IMAGES];
static VkDeviceMemory g_pending_memory[SHIM_MAX_SWAPCHAIN_IMAGES];
static int g_pending_dmabuf_fds[SHIM_MAX_SWAPCHAIN_IMAGES] = {-1, -1, -1};
static AHardwareBuffer* g_pending_ahbs[SHIM_MAX_SWAPCHAIN_IMAGES] = {NULL, NULL, NULL};
static uint32_t g_pending_image_count = 0;

static VkImage g_holdoff_images[SHIM_MAX_SWAPCHAIN_IMAGES];
static VkDeviceMemory g_holdoff_memory[SHIM_MAX_SWAPCHAIN_IMAGES];
static int g_holdoff_dmabuf_fds[SHIM_MAX_SWAPCHAIN_IMAGES] = {-1, -1, -1};
static AHardwareBuffer* g_holdoff_ahbs[SHIM_MAX_SWAPCHAIN_IMAGES] = {NULL, NULL, NULL};
static uint32_t g_holdoff_image_count = 0;

static VkImage g_render_images[SHIM_MAX_SWAPCHAIN_IMAGES];
static VkDeviceMemory g_render_memory[SHIM_MAX_SWAPCHAIN_IMAGES];
static int g_render_image_count = 0;

static VkImage g_pending_render_images[SHIM_MAX_SWAPCHAIN_IMAGES];
static VkDeviceMemory g_pending_render_memory[SHIM_MAX_SWAPCHAIN_IMAGES];
static int g_pending_render_count = 0;

static VkCommandPool g_blit_cmd_pool = VK_NULL_HANDLE;
static VkCommandBuffer g_blit_cmd_bufs[SHIM_MAX_SWAPCHAIN_IMAGES];
static VkFence g_blit_fences[SHIM_MAX_SWAPCHAIN_IMAGES];

static VkFence g_present_fences[SHIM_MAX_SWAPCHAIN_IMAGES];

static uint32_t g_swapchain_current = 0;
static VkFormat g_swapchain_format = VK_FORMAT_R8G8B8A8_UNORM;
static VkFormat g_ahb_vk_format = VK_FORMAT_R8G8B8A8_UNORM;
static uint32_t g_swapchain_width = 0;
static uint32_t g_swapchain_height = 0;
static int g_compositor_sock = -1;

typedef VkResult (*PFN_vkCreateImage)(VkDevice, const VkImageCreateInfo*, const VkAllocationCallbacks*, VkImage*);
typedef void (*PFN_vkDestroyImage)(VkDevice, VkImage, const VkAllocationCallbacks*);
typedef void (*PFN_vkGetImageMemoryRequirements)(VkDevice, VkImage, VkMemoryRequirements*);
typedef VkResult (*PFN_vkAllocateMemory)(VkDevice, const VkMemoryAllocateInfo*, const VkAllocationCallbacks*, VkDeviceMemory*);
typedef void (*PFN_vkFreeMemory)(VkDevice, VkDeviceMemory, const VkAllocationCallbacks*);
typedef VkResult (*PFN_vkBindImageMemory)(VkDevice, VkImage, VkDeviceMemory, VkDeviceSize);
typedef void (*PFN_vkGetPhysicalDeviceMemoryProperties)(VkPhysicalDevice, VkPhysicalDeviceMemoryProperties*);

typedef VkResult (*PFN_vkQueueSubmit_t)(VkQueue, uint32_t, const VkSubmitInfo*, VkFence);
typedef VkResult (*PFN_vkQueueWaitIdle_t)(VkQueue);
typedef VkResult (*PFN_vkDeviceWaitIdle_t)(VkDevice);
typedef void (*PFN_vkGetDeviceQueue_t)(VkDevice, uint32_t, uint32_t, VkQueue*);

typedef VkResult (*PFN_vkCreateCommandPool_t)(VkDevice, const VkCommandPoolCreateInfo*, const VkAllocationCallbacks*, VkCommandPool*);
typedef void (*PFN_vkDestroyCommandPool_t)(VkDevice, VkCommandPool, const VkAllocationCallbacks*);
typedef VkResult (*PFN_vkAllocateCommandBuffers_t)(VkDevice, const VkCommandBufferAllocateInfo*, VkCommandBuffer*);
typedef VkResult (*PFN_vkBeginCommandBuffer_t)(VkCommandBuffer, const VkCommandBufferBeginInfo*);
typedef VkResult (*PFN_vkEndCommandBuffer_t)(VkCommandBuffer);
typedef void (*PFN_vkCmdPipelineBarrier_t)(VkCommandBuffer, VkPipelineStageFlags, VkPipelineStageFlags, VkDependencyFlags, uint32_t, const VkMemoryBarrier*, uint32_t, const VkBufferMemoryBarrier*, uint32_t, const VkImageMemoryBarrier*);
typedef void (*PFN_vkCmdCopyImage_t)(VkCommandBuffer, VkImage, VkImageLayout, VkImage, VkImageLayout, uint32_t, const VkImageCopy*);
typedef void (*PFN_vkCmdBlitImage_t)(VkCommandBuffer, VkImage, VkImageLayout, VkImage, VkImageLayout, uint32_t, const VkImageBlit*, VkFilter);
typedef VkResult (*PFN_vkCreateFence_t)(VkDevice, const VkFenceCreateInfo*, const VkAllocationCallbacks*, VkFence*);
typedef void (*PFN_vkDestroyFence_t)(VkDevice, VkFence, const VkAllocationCallbacks*);
typedef VkResult (*PFN_vkWaitForFences_t)(VkDevice, uint32_t, const VkFence*, VkBool32, uint64_t);
typedef VkResult (*PFN_vkResetFences_t)(VkDevice, uint32_t, const VkFence*);
typedef VkResult (*PFN_vkGetFenceStatus_t)(VkDevice, VkFence);
typedef VkResult (*PFN_vkResetCommandBuffer_t)(VkCommandBuffer, VkCommandBufferResetFlags);

static PFN_vkCreateImage pfn_createImage = NULL;
static PFN_vkDestroyImage pfn_destroyImage = NULL;
static PFN_vkGetImageMemoryRequirements pfn_getImageMemReq = NULL;
static PFN_vkAllocateMemory pfn_allocMemory = NULL;
static PFN_vkFreeMemory pfn_freeMemory = NULL;
static PFN_vkBindImageMemory pfn_bindImageMemory = NULL;
static PFN_vkGetPhysicalDeviceMemoryProperties pfn_getPhysDevMemProps = NULL;
static PFN_vkQueueSubmit_t pfn_queueSubmit = NULL;
static PFN_vkQueueWaitIdle_t pfn_queueWaitIdle = NULL;
static PFN_vkDeviceWaitIdle_t pfn_deviceWaitIdle = NULL;
static PFN_vkGetDeviceQueue_t pfn_getDeviceQueue = NULL;
static PFN_vkCreateCommandPool_t pfn_createCmdPool = NULL;
static PFN_vkDestroyCommandPool_t pfn_destroyCmdPool = NULL;
static PFN_vkAllocateCommandBuffers_t pfn_allocCmdBufs = NULL;
static PFN_vkBeginCommandBuffer_t pfn_beginCmdBuf = NULL;
static PFN_vkEndCommandBuffer_t pfn_endCmdBuf = NULL;
static PFN_vkCmdPipelineBarrier_t pfn_cmdPipelineBarrier = NULL;
static PFN_vkCmdCopyImage_t pfn_cmdCopyImage = NULL;
static PFN_vkCmdBlitImage_t pfn_cmdBlitImage = NULL;
static PFN_vkCreateFence_t pfn_createFence = NULL;
static PFN_vkDestroyFence_t pfn_destroyFence = NULL;
static PFN_vkWaitForFences_t pfn_waitForFences = NULL;
static PFN_vkResetFences_t pfn_resetFences = NULL;
static PFN_vkGetFenceStatus_t pfn_getFenceStatus = NULL;
static PFN_vkResetCommandBuffer_t pfn_resetCmdBuf = NULL;
typedef VkResult (VKAPI_PTR *PFN_vkCreateSemaphore_t)(VkDevice, const VkSemaphoreCreateInfo*, const VkAllocationCallbacks*, VkSemaphore*);
typedef void (VKAPI_PTR *PFN_vkDestroySemaphore_t)(VkDevice, VkSemaphore, const VkAllocationCallbacks*);
typedef VkResult (VKAPI_PTR *PFN_vkGetSemaphoreFdKHR_t)(VkDevice, const VkSemaphoreGetFdInfoKHR*, int*);
typedef VkResult (VKAPI_PTR *PFN_vkGetFenceFdKHR_t)(VkDevice, const VkFenceGetFdInfoKHR*, int*);
static PFN_vkCreateSemaphore_t pfn_createSemaphore = NULL;
static PFN_vkDestroySemaphore_t pfn_destroySemaphore = NULL;
static PFN_vkGetSemaphoreFdKHR_t pfn_getSemaphoreFdKHR = NULL;
static PFN_vkGetFenceFdKHR_t pfn_getFenceFdKHR = NULL;
static VkSemaphore g_present_semaphores[16] = {0};
static int g_present_fence_exportable = 0;
static int g_present_fence_pending_export[SHIM_MAX_SWAPCHAIN_IMAGES] = {0};
static VkPhysicalDevice g_physical_device = VK_NULL_HANDLE;
static VkQueue g_queue = VK_NULL_HANDLE;

static void resolve_device_funcs(VkDevice device) {
    if (pfn_createImage) return;
    if (!real_vkGetInstanceProcAddr || !g_instance) return;

    typedef PFN_vkVoidFunction (*PFN_vkGetDeviceProcAddr)(VkDevice, const char*);
    PFN_vkGetDeviceProcAddr gdpa = (PFN_vkGetDeviceProcAddr)
        real_vkGetInstanceProcAddr(g_instance, "vkGetDeviceProcAddr");
    if (!gdpa) return;

    pfn_createImage = (PFN_vkCreateImage)gdpa(device, "vkCreateImage");
    pfn_destroyImage = (PFN_vkDestroyImage)gdpa(device, "vkDestroyImage");
    pfn_getImageMemReq = (PFN_vkGetImageMemoryRequirements)gdpa(device, "vkGetImageMemoryRequirements");
    pfn_allocMemory = (PFN_vkAllocateMemory)gdpa(device, "vkAllocateMemory");
    pfn_freeMemory = (PFN_vkFreeMemory)gdpa(device, "vkFreeMemory");
    pfn_bindImageMemory = (PFN_vkBindImageMemory)gdpa(device, "vkBindImageMemory");
    pfn_getPhysDevMemProps = (PFN_vkGetPhysicalDeviceMemoryProperties)
        real_vkGetInstanceProcAddr(g_instance, "vkGetPhysicalDeviceMemoryProperties");
    pfn_queueSubmit = (PFN_vkQueueSubmit_t)gdpa(device, "vkQueueSubmit");
    pfn_queueWaitIdle = (PFN_vkQueueWaitIdle_t)gdpa(device, "vkQueueWaitIdle");
    pfn_deviceWaitIdle = (PFN_vkDeviceWaitIdle_t)gdpa(device, "vkDeviceWaitIdle");
    pfn_getDeviceQueue = (PFN_vkGetDeviceQueue_t)gdpa(device, "vkGetDeviceQueue");
    pfn_createCmdPool = (PFN_vkCreateCommandPool_t)gdpa(device, "vkCreateCommandPool");
    pfn_destroyCmdPool = (PFN_vkDestroyCommandPool_t)gdpa(device, "vkDestroyCommandPool");
    pfn_allocCmdBufs = (PFN_vkAllocateCommandBuffers_t)gdpa(device, "vkAllocateCommandBuffers");
    pfn_beginCmdBuf = (PFN_vkBeginCommandBuffer_t)gdpa(device, "vkBeginCommandBuffer");
    pfn_endCmdBuf = (PFN_vkEndCommandBuffer_t)gdpa(device, "vkEndCommandBuffer");
    pfn_cmdPipelineBarrier = (PFN_vkCmdPipelineBarrier_t)gdpa(device, "vkCmdPipelineBarrier");
    pfn_cmdCopyImage = (PFN_vkCmdCopyImage_t)gdpa(device, "vkCmdCopyImage");
    pfn_cmdBlitImage = (PFN_vkCmdBlitImage_t)gdpa(device, "vkCmdBlitImage");
    pfn_createFence = (PFN_vkCreateFence_t)gdpa(device, "vkCreateFence");
    pfn_destroyFence = (PFN_vkDestroyFence_t)gdpa(device, "vkDestroyFence");
    pfn_waitForFences = (PFN_vkWaitForFences_t)gdpa(device, "vkWaitForFences");
    pfn_resetFences = (PFN_vkResetFences_t)gdpa(device, "vkResetFences");
    pfn_getFenceStatus = (PFN_vkGetFenceStatus_t)gdpa(device, "vkGetFenceStatus");
    pfn_resetCmdBuf = (PFN_vkResetCommandBuffer_t)gdpa(device, "vkResetCommandBuffer");
    pfn_createSemaphore = (PFN_vkCreateSemaphore_t)gdpa(device, "vkCreateSemaphore");
    pfn_destroySemaphore = (PFN_vkDestroySemaphore_t)gdpa(device, "vkDestroySemaphore");
    pfn_getSemaphoreFdKHR = (PFN_vkGetSemaphoreFdKHR_t)gdpa(device, "vkGetSemaphoreFdKHR");
    pfn_getFenceFdKHR = (PFN_vkGetFenceFdKHR_t)gdpa(device, "vkGetFenceFdKHR");

    LOGI("Device funcs resolved: createImage=%p queueSubmit=%p cmdCopyImage=%p sema_fd=%p fence_fd=%p",
         (void*)pfn_createImage, (void*)pfn_queueSubmit, (void*)pfn_cmdCopyImage,
         (void*)pfn_getSemaphoreFdKHR, (void*)pfn_getFenceFdKHR);
}

static uint32_t find_memory_type(uint32_t typeBits, VkMemoryPropertyFlags props) {
    VkPhysicalDeviceMemoryProperties memProps;
    if (!pfn_getPhysDevMemProps || !g_physical_device) return UINT32_MAX;
    pfn_getPhysDevMemProps(g_physical_device, &memProps);
    for (uint32_t i = 0; i < memProps.memoryTypeCount; i++) {
        if ((typeBits & (1 << i)) &&
            (memProps.memoryTypes[i].propertyFlags & props) == props)
            return i;
    }
    return UINT32_MAX;
}

static void destroy_pending_images(void) {
    if (!g_device || !pfn_destroyImage) { g_pending_image_count = 0; return; }
    for (uint32_t i = 0; i < g_pending_image_count; i++) {
        if (g_pending_images[i]) {
            pfn_destroyImage(g_device, g_pending_images[i], NULL);
            g_pending_images[i] = VK_NULL_HANDLE;
        }
        if (g_pending_memory[i]) {
            pfn_freeMemory(g_device, g_pending_memory[i], NULL);
            g_pending_memory[i] = VK_NULL_HANDLE;
        }
        if (g_pending_dmabuf_fds[i] >= 0) {
            close(g_pending_dmabuf_fds[i]);
            g_pending_dmabuf_fds[i] = -1;
        }
        if (g_pending_ahbs[i]) {
            AHardwareBuffer_release(g_pending_ahbs[i]);
            g_pending_ahbs[i] = NULL;
        }
    }
    g_pending_image_count = 0;
}

static void destroy_holdoff_images(void) {
    if (!g_device || !pfn_destroyImage) { g_holdoff_image_count = 0; return; }
    for (uint32_t i = 0; i < g_holdoff_image_count; i++) {
        if (g_holdoff_images[i]) {
            pfn_destroyImage(g_device, g_holdoff_images[i], NULL);
            g_holdoff_images[i] = VK_NULL_HANDLE;
        }
        if (g_holdoff_memory[i]) {
            pfn_freeMemory(g_device, g_holdoff_memory[i], NULL);
            g_holdoff_memory[i] = VK_NULL_HANDLE;
        }
        if (g_holdoff_dmabuf_fds[i] >= 0) {
            close(g_holdoff_dmabuf_fds[i]);
            g_holdoff_dmabuf_fds[i] = -1;
        }
        if (g_holdoff_ahbs[i]) {
            AHardwareBuffer_release(g_holdoff_ahbs[i]);
            g_holdoff_ahbs[i] = NULL;
        }
    }
    g_holdoff_image_count = 0;
}

static void retire_swapchain_images(void) {
    destroy_holdoff_images();
    for (uint32_t i = 0; i < g_pending_image_count; i++) {
        g_holdoff_images[i] = g_pending_images[i];
        g_holdoff_memory[i] = g_pending_memory[i];
        g_holdoff_dmabuf_fds[i] = g_pending_dmabuf_fds[i];
        g_holdoff_ahbs[i] = g_pending_ahbs[i];
        g_pending_images[i] = VK_NULL_HANDLE;
        g_pending_memory[i] = VK_NULL_HANDLE;
        g_pending_dmabuf_fds[i] = -1;
        g_pending_ahbs[i] = NULL;
    }
    g_holdoff_image_count = g_pending_image_count;
    g_pending_image_count = 0;

    for (uint32_t i = 0; i < g_swapchain_image_count; i++) {
        g_pending_images[i] = g_swapchain_images[i];
        g_pending_memory[i] = g_swapchain_memory[i];
        g_pending_dmabuf_fds[i] = g_swapchain_dmabuf_fds[i];
        g_pending_ahbs[i] = g_swapchain_ahbs[i];
        g_swapchain_images[i] = VK_NULL_HANDLE;
        g_swapchain_memory[i] = VK_NULL_HANDLE;
        g_swapchain_dmabuf_fds[i] = -1;
        g_swapchain_ahbs[i] = NULL;
    }
    g_pending_image_count = g_swapchain_image_count;
    g_swapchain_image_count = 0;
    g_swapchain_has_dmabuf = 0;
}

static void destroy_swapchain_images(void) {
    if (!g_device || !pfn_destroyImage) {
        g_swapchain_image_count = 0;
        g_swapchain_has_dmabuf = 0;
        return;
    }
    for (uint32_t i = 0; i < g_swapchain_image_count; i++) {
        if (g_swapchain_images[i]) {
            pfn_destroyImage(g_device, g_swapchain_images[i], NULL);
            g_swapchain_images[i] = VK_NULL_HANDLE;
        }
        if (g_swapchain_memory[i]) {
            pfn_freeMemory(g_device, g_swapchain_memory[i], NULL);
            g_swapchain_memory[i] = VK_NULL_HANDLE;
        }
        if (g_swapchain_dmabuf_fds[i] >= 0) {
            close(g_swapchain_dmabuf_fds[i]);
            g_swapchain_dmabuf_fds[i] = -1;
        }
        if (g_swapchain_ahbs[i]) {
            AHardwareBuffer_release(g_swapchain_ahbs[i]);
            g_swapchain_ahbs[i] = NULL;
        }
    }
    g_swapchain_image_count = 0;
    g_swapchain_has_dmabuf = 0;
}

static void destroy_render_images(void) {
    if (!g_device || !pfn_destroyImage) { g_render_image_count = 0; return; }
    for (uint32_t i = 0; i < g_render_image_count; i++) {
        if (g_render_images[i]) {
            pfn_destroyImage(g_device, g_render_images[i], NULL);
            g_render_images[i] = VK_NULL_HANDLE;
        }
        if (g_render_memory[i]) {
            pfn_freeMemory(g_device, g_render_memory[i], NULL);
            g_render_memory[i] = VK_NULL_HANDLE;
        }
    }
    g_render_image_count = 0;
}

static void destroy_blit_resources(void) {
    if (!g_device) return;
    for (uint32_t i = 0; i < SHIM_MAX_SWAPCHAIN_IMAGES; i++) {
        if (g_blit_fences[i] && pfn_destroyFence) {
            pfn_destroyFence(g_device, g_blit_fences[i], NULL);
            g_blit_fences[i] = VK_NULL_HANDLE;
        }
        g_blit_cmd_bufs[i] = VK_NULL_HANDLE;
    }
    if (g_blit_cmd_pool && pfn_destroyCmdPool) {
        pfn_destroyCmdPool(g_device, g_blit_cmd_pool, NULL);
        g_blit_cmd_pool = VK_NULL_HANDLE;
    }
}

static int create_blit_resources(uint32_t count) {
    if (g_blit_cmd_pool) destroy_blit_resources();

    VkCommandPoolCreateInfo poolInfo = {
        .sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO,
        .flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT,
        .queueFamilyIndex = 0,
    };
    VkResult r = pfn_createCmdPool(g_device, &poolInfo, NULL, &g_blit_cmd_pool);
    if (r != VK_SUCCESS) {
        LOGE("Failed to create blit command pool: %d", r);
        return -1;
    }

    VkCommandBufferAllocateInfo allocInfo = {
        .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO,
        .commandPool = g_blit_cmd_pool,
        .level = VK_COMMAND_BUFFER_LEVEL_PRIMARY,
        .commandBufferCount = count,
    };
    r = pfn_allocCmdBufs(g_device, &allocInfo, g_blit_cmd_bufs);
    if (r != VK_SUCCESS) {
        LOGE("Failed to allocate blit command buffers: %d", r);
        destroy_blit_resources();
        return -1;
    }

    VkFenceCreateInfo fenceInfo = {
        .sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO,
        .flags = VK_FENCE_CREATE_SIGNALED_BIT,
    };
    for (uint32_t i = 0; i < count; i++) {
        r = pfn_createFence(g_device, &fenceInfo, NULL, &g_blit_fences[i]);
        if (r != VK_SUCCESS) {
            LOGE("Failed to create blit fence[%u]: %d", i, r);
            destroy_blit_resources();
            return -1;
        }
    }

    LOGI("Blit resources created: pool=%p, %u cmd bufs, %u fences",
         (void*)g_blit_cmd_pool, count, count);
    return 0;
}

static uint32_t vkformat_to_ahb(VkFormat fmt) {
    switch (fmt) {
        case VK_FORMAT_R8G8B8A8_UNORM:
        case VK_FORMAT_R8G8B8A8_SRGB:
        case VK_FORMAT_B8G8R8A8_UNORM:
        case VK_FORMAT_B8G8R8A8_SRGB:
            return AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM;
        case VK_FORMAT_R5G6B5_UNORM_PACK16:
            return AHARDWAREBUFFER_FORMAT_R5G6B5_UNORM;
        case VK_FORMAT_R16G16B16A16_SFLOAT:
            return AHARDWAREBUFFER_FORMAT_R16G16B16A16_FLOAT;
        case VK_FORMAT_A2B10G10R10_UNORM_PACK32:
            return AHARDWAREBUFFER_FORMAT_R10G10B10A2_UNORM;
        default:
            return AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM;
    }
}

// ------------------------------ connection to compositor ----------------
static int connect_and_request_ahbs(uint32_t width, uint32_t height,
                                     uint32_t ahb_format, uint32_t count) {
    if (g_compositor_sock >= 0) {
        close(g_compositor_sock);
        g_compositor_sock = -1;
    }

    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) {
        LOGE("compositor connect: socket: %s", strerror(errno));
        return -1;
    }

    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    addr.sun_path[0] = '\0';
    const char* name = "polydroid_frame_bridge";
    strncpy(addr.sun_path + 1, name, sizeof(addr.sun_path) - 2);
    socklen_t addrlen = offsetof(struct sockaddr_un, sun_path) + 1 + strlen(name);

    // retry if compositor isnt ready yet
    int connected = 0;
    for (int i = 0; i < 50; i++) {
        if (connect(fd, (struct sockaddr*)&addr, addrlen) == 0) {
            connected = 1;
            break;
        }
        usleep(100000);
    }
    if (!connected) {
        LOGE("Failed to connect to compositor @%s: %s", name, strerror(errno));
        close(fd);
        return -1;
    }
    LOGI("Connected to frame compositor @%s", name);
    g_compositor_sock = fd;

    // send buffer request
    uint32_t req[4] = { width, height, ahb_format, count };
    if (send(fd, req, sizeof(req), 0) != sizeof(req)) {
        LOGE("Failed to send buffer request: %s", strerror(errno));
        close(fd);
        g_compositor_sock = -1;
        return -1;
    }
    LOGI("Sent buffer request: %ux%u fmt=%u count=%u", width, height, ahb_format, count);

    for (uint32_t i = 0; i < count; i++) {
        AHardwareBuffer* ahb = NULL;
        int ret = AHardwareBuffer_recvHandleFromUnixSocket(fd, &ahb);
        if (ret != 0 || !ahb) {
            LOGE("Failed to receive AHB[%u]: ret=%d", i, ret);
            close(fd);
            g_compositor_sock = -1;
            return -1;
        }
        g_swapchain_ahbs[i] = ahb;
        g_swapchain_dmabuf_fds[i] = -1;

        AHardwareBuffer_Desc desc;
        AHardwareBuffer_describe(ahb, &desc);
        g_swapchain_strides[i] = desc.stride;
        LOGI("Received AHB[%u] %ux%u stride=%u fmt=%u (UBWC)",
             i, desc.width, desc.height, desc.stride, desc.format);
    }

    // wait for ready
    uint8_t ready = 0;
    if (recv(fd, &ready, 1, MSG_WAITALL) != 1 || ready != 'R') {
        LOGE("Failed to receive ready signal");
        close(fd);
        g_compositor_sock = -1;
        return -1;
    }
    LOGI("Compositor ready, %u AHBs received", count);

    // send vulkan metadata to compositor for the stats overlay
    struct {
        char magic[4];
        char gpu_name[64];
        uint32_t api_ver;
        uint32_t drv_ver;
    } meta;
    memcpy(meta.magic, "VKIF", 4);
    memset(meta.gpu_name, 0, 64);
    strncpy(meta.gpu_name, g_gpu_name, 63);
    meta.api_ver = g_vk_api_version;
    meta.drv_ver = g_vk_driver_version;
    if (send(fd, &meta, sizeof(meta), 0) != sizeof(meta)) {
        LOGE("Failed to send Vulkan metadata: %s", strerror(errno));
    } else {
        LOGI("Sent Vulkan metadata: %s, API %u.%u.%u",
             meta.gpu_name,
             VK_VERSION_MAJOR(meta.api_ver),
             VK_VERSION_MINOR(meta.api_ver),
             VK_VERSION_PATCH(meta.api_ver));
    }

    return 0;
}

static VkResult shim_vkCreateSwapchainKHR(
    VkDevice device,
    const VkSwapchainCreateInfoKHR* pCreateInfo,
    const VkAllocationCallbacks* pAllocator,
    VkSwapchainKHR* pSwapchain)
{
    LOGI("vkCreateSwapchainKHR: %ux%u fmt=%u images=%u-%u oldSwapchain=%p",
         pCreateInfo->imageExtent.width, pCreateInfo->imageExtent.height,
         pCreateInfo->imageFormat, pCreateInfo->minImageCount,
         SHIM_MAX_SWAPCHAIN_IMAGES,
         (void*)(uintptr_t)pCreateInfo->oldSwapchain);

    g_device = device;
    resolve_device_funcs(device);

    if (!pfn_createImage || !pfn_allocMemory || !pfn_bindImageMemory) {
        LOGE("Cannot create swapchain: device funcs not resolved");
        return VK_ERROR_INITIALIZATION_FAILED;
    }

    retire_swapchain_images();

    g_swapchain_format = pCreateInfo->imageFormat;
    g_swapchain_width = pCreateInfo->imageExtent.width;
    g_swapchain_height = pCreateInfo->imageExtent.height;

    uint32_t count = pCreateInfo->minImageCount;
    if (count < 3) count = 3;
    if (count > SHIM_MAX_SWAPCHAIN_IMAGES) count = SHIM_MAX_SWAPCHAIN_IMAGES;

    uint32_t ahb_format = vkformat_to_ahb(pCreateInfo->imageFormat);

    if (connect_and_request_ahbs(pCreateInfo->imageExtent.width,
                                  pCreateInfo->imageExtent.height,
                                  ahb_format, count) != 0) {
        // fallbacks to plain images. this will keep it from crashing, but it WONT render.
        LOGE("Failed to get AHBs from compositor, falling back to plain images");
        for (uint32_t i = 0; i < count; i++) {
            VkImageCreateInfo imgInfo = {
                .sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO,
                .imageType = VK_IMAGE_TYPE_2D,
                .format = pCreateInfo->imageFormat,
                .extent = { pCreateInfo->imageExtent.width, pCreateInfo->imageExtent.height, 1 },
                .mipLevels = 1,
                .arrayLayers = pCreateInfo->imageArrayLayers,
                .samples = VK_SAMPLE_COUNT_1_BIT,
                .tiling = VK_IMAGE_TILING_OPTIMAL,
                .usage = pCreateInfo->imageUsage,
                .sharingMode = pCreateInfo->imageSharingMode,
                .queueFamilyIndexCount = pCreateInfo->queueFamilyIndexCount,
                .pQueueFamilyIndices = pCreateInfo->pQueueFamilyIndices,
                .initialLayout = VK_IMAGE_LAYOUT_UNDEFINED,
            };
            VkResult r = pfn_createImage(device, &imgInfo, NULL, &g_swapchain_images[i]);
            if (r != VK_SUCCESS) { destroy_swapchain_images(); return r; }

            VkMemoryRequirements memReq;
            pfn_getImageMemReq(device, g_swapchain_images[i], &memReq);
            VkMemoryAllocateInfo allocInfo = {
                .sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
                .allocationSize = memReq.size,
                .memoryTypeIndex = find_memory_type(memReq.memoryTypeBits,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
            };
            r = pfn_allocMemory(device, &allocInfo, NULL, &g_swapchain_memory[i]);
            if (r != VK_SUCCESS) { destroy_swapchain_images(); return r; }
            r = pfn_bindImageMemory(device, g_swapchain_images[i], g_swapchain_memory[i], 0);
            if (r != VK_SUCCESS) { destroy_swapchain_images(); return r; }
        }
        goto finish;
    }

    g_swapchain_has_dmabuf = 1;

    // resolve vkGetAndroidHardwareBufferPropertiesANDROID (needed by both paths)
    typedef VkResult (*PFN_vkGetAHBProps)(VkDevice, const struct AHardwareBuffer*, VkAndroidHardwareBufferPropertiesANDROID*);
    PFN_vkGetAHBProps pfn_getAHBProps = NULL;
    if (real_vkGetInstanceProcAddr && g_instance) {
        typedef PFN_vkVoidFunction (*PFN_gdpa)(VkDevice, const char*);
        PFN_gdpa gdpa = (PFN_gdpa)real_vkGetInstanceProcAddr(g_instance, "vkGetDeviceProcAddr");
        if (gdpa) pfn_getAHBProps = (PFN_vkGetAHBProps)gdpa(device, "vkGetAndroidHardwareBufferPropertiesANDROID");
    }
    if (!pfn_getAHBProps) {
        LOGE("vkGetAndroidHardwareBufferPropertiesANDROID not available");
        destroy_swapchain_images();
        return VK_ERROR_INITIALIZATION_FAILED;
    }

    for (uint32_t i = 0; i < count; i++) {
        if (!g_swapchain_ahbs[i]) {
            LOGE("AHB[%u] is NULL", i);
            destroy_swapchain_images();
            return VK_ERROR_INITIALIZATION_FAILED;
        }

        VkAndroidHardwareBufferFormatPropertiesANDROID ahbFmtProps = {
            .sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_FORMAT_PROPERTIES_ANDROID,
            .pNext = NULL,
        };
        VkAndroidHardwareBufferPropertiesANDROID ahbProps = {
            .sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_PROPERTIES_ANDROID,
            .pNext = &ahbFmtProps,
        };

        VkResult r = pfn_getAHBProps(device, g_swapchain_ahbs[i], &ahbProps);
        if (r != VK_SUCCESS) {
            LOGE("vkGetAndroidHardwareBufferPropertiesANDROID[%u] failed: %d", i, r);
            destroy_swapchain_images();
            return r;
        }
        if (i == 0 && ahbFmtProps.format)
            g_ahb_vk_format = ahbFmtProps.format;
        LOGI("AHB[%u] props: allocationSize=%llu memTypeBits=0x%x fmt=%u",
             i, (unsigned long long)ahbProps.allocationSize, ahbProps.memoryTypeBits,
             ahbFmtProps.format);

        if (g_using_system_driver) {
            VkExternalMemoryImageCreateInfo extImgInfo = {
                .sType = VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO,
                .pNext = NULL,
                .handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_ANDROID_HARDWARE_BUFFER_BIT_ANDROID,
            };

            VkImageCreateInfo imgInfo = {
                .sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO,
                .pNext = &extImgInfo,
                .imageType = VK_IMAGE_TYPE_2D,
                .format = ahbFmtProps.format ? ahbFmtProps.format : VK_FORMAT_R8G8B8A8_UNORM,
                .extent = { pCreateInfo->imageExtent.width, pCreateInfo->imageExtent.height, 1 },
                .mipLevels = 1,
                .arrayLayers = pCreateInfo->imageArrayLayers,
                .samples = VK_SAMPLE_COUNT_1_BIT,
                .tiling = VK_IMAGE_TILING_OPTIMAL,
                .usage = pCreateInfo->imageUsage,
                .sharingMode = pCreateInfo->imageSharingMode,
                .queueFamilyIndexCount = pCreateInfo->queueFamilyIndexCount,
                .pQueueFamilyIndices = pCreateInfo->pQueueFamilyIndices,
                .initialLayout = VK_IMAGE_LAYOUT_UNDEFINED,
            };

            r = pfn_createImage(device, &imgInfo, NULL, &g_swapchain_images[i]);
            if (r != VK_SUCCESS) {
                LOGE("vkCreateImage[%u] (AHB) failed: %d", i, r);
                destroy_swapchain_images();
                return r;
            }

            typedef struct {
                VkStructureType sType;
                const void* pNext;
                struct AHardwareBuffer* buffer;
            } VkImportAndroidHardwareBufferInfoANDROID;

            VkImportAndroidHardwareBufferInfoANDROID importAhbInfo = {
                .sType = VK_STRUCTURE_TYPE_IMPORT_ANDROID_HARDWARE_BUFFER_INFO_ANDROID,
                .pNext = NULL,
                .buffer = g_swapchain_ahbs[i],
            };

            typedef struct {
                VkStructureType sType;
                const void* pNext;
                VkImage image;
                VkBuffer buffer;
            } VkMemoryDedicatedAllocateInfo;

            VkMemoryDedicatedAllocateInfo dedicatedInfo = {
                .sType = VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO,
                .pNext = &importAhbInfo,
                .image = g_swapchain_images[i],
                .buffer = VK_NULL_HANDLE,
            };

            VkMemoryRequirements memReq;
            pfn_getImageMemReq(device, g_swapchain_images[i], &memReq);

            uint32_t typeBits = ahbProps.memoryTypeBits & memReq.memoryTypeBits;
            if (typeBits == 0) typeBits = ahbProps.memoryTypeBits;
            uint32_t typeIdx = find_memory_type(typeBits, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
            if (typeIdx == UINT32_MAX) typeIdx = find_memory_type(typeBits, 0);

            VkMemoryAllocateInfo allocInfo = {
                .sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
                .pNext = &dedicatedInfo,
                .allocationSize = ahbProps.allocationSize,
                .memoryTypeIndex = typeIdx,
            };

            r = pfn_allocMemory(device, &allocInfo, NULL, &g_swapchain_memory[i]);
            if (r != VK_SUCCESS) {
                LOGE("vkAllocateMemory[%u] (AHB import) failed: %d", i, r);
                destroy_swapchain_images();
                return r;
            }
            r = pfn_bindImageMemory(device, g_swapchain_images[i], g_swapchain_memory[i], 0);
            if (r != VK_SUCCESS) {
                LOGE("vkBindImageMemory[%u] failed: %d", i, r);
                destroy_swapchain_images();
                return r;
            }

            g_swapchain_dmabuf_fds[i] = -1;
            LOGI("Swapchain image[%u]: VkImage=%p (AHB import, system driver)", i,
                 (void*)(uintptr_t)g_swapchain_images[i]);

        } else {
            VkSubresourceLayout drmLayout = {
                .offset = 0,
                .size = 0,
                .rowPitch = g_swapchain_strides[i] * 4,
                .arrayPitch = 0,
                .depthPitch = 0,
            };

            typedef struct {
                VkStructureType sType;
                const void* pNext;
                uint64_t drmFormatModifier;
                uint32_t drmFormatModifierPlaneCount;
                const VkSubresourceLayout* pPlaneLayouts;
            } VkImageDrmFormatModifierExplicitCreateInfoEXT;

            VkImageDrmFormatModifierExplicitCreateInfoEXT drmModInfo = {
                .sType = VK_STRUCTURE_TYPE_IMAGE_DRM_FORMAT_MODIFIER_EXPLICIT_CREATE_INFO_EXT,
                .pNext = NULL,
                .drmFormatModifier = 0,
                .drmFormatModifierPlaneCount = 1,
                .pPlaneLayouts = &drmLayout,
            };

            if (!pfn_ahb_getNativeHandle) {
                pfn_ahb_getNativeHandle = (PFN_AHardwareBuffer_getNativeHandle)
                    dlsym(RTLD_DEFAULT, "AHardwareBuffer_getNativeHandle");
            }
            if (!pfn_ahb_getNativeHandle) {
                LOGE("AHardwareBuffer_getNativeHandle not available");
                destroy_swapchain_images();
                return VK_ERROR_INITIALIZATION_FAILED;
            }
            const native_handle_t* handle = pfn_ahb_getNativeHandle(g_swapchain_ahbs[i]);
            if (!handle || handle->numFds < 1) {
                LOGE("AHB[%u] has no native handle fds", i);
                destroy_swapchain_images();
                return VK_ERROR_INITIALIZATION_FAILED;
            }
            int dmabuf_fd = handle->data[0];
            g_swapchain_dmabuf_fds[i] = dmabuf_fd;
            LOGI("AHB[%u] dmabuf fd=%d", i, dmabuf_fd);

            VkExternalMemoryImageCreateInfo extImgInfo = {
                .sType = VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO,
                .pNext = &drmModInfo,
                .handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_DMA_BUF_BIT_EXT,
            };

            VkImageCreateInfo imgInfo = {
                .sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO,
                .pNext = &extImgInfo,
                .imageType = VK_IMAGE_TYPE_2D,
                .format = VK_FORMAT_R8G8B8A8_UNORM,
                .extent = { pCreateInfo->imageExtent.width, pCreateInfo->imageExtent.height, 1 },
                .mipLevels = 1,
                .arrayLayers = pCreateInfo->imageArrayLayers,
                .samples = VK_SAMPLE_COUNT_1_BIT,
                .tiling = VK_IMAGE_TILING_DRM_FORMAT_MODIFIER_EXT,
                .usage = pCreateInfo->imageUsage,
                .sharingMode = pCreateInfo->imageSharingMode,
                .queueFamilyIndexCount = pCreateInfo->queueFamilyIndexCount,
                .pQueueFamilyIndices = pCreateInfo->pQueueFamilyIndices,
                .initialLayout = VK_IMAGE_LAYOUT_UNDEFINED,
            };

            r = pfn_createImage(device, &imgInfo, NULL, &g_swapchain_images[i]);
            if (r != VK_SUCCESS) {
                LOGE("vkCreateImage[%u] (dmabuf) failed: %d", i, r);
                destroy_swapchain_images();
                return r;
            }

            typedef struct {
                VkStructureType sType;
                const void* pNext;
                VkExternalMemoryHandleTypeFlagBits handleType;
                int fd;
            } VkImportMemoryFdInfoKHR;

            int import_fd = dup(g_swapchain_dmabuf_fds[i]);
            if (import_fd < 0) {
                LOGE("dup dmabuf fd[%u] failed: %s", i, strerror(errno));
                destroy_swapchain_images();
                return VK_ERROR_OUT_OF_HOST_MEMORY;
            }

            VkImportMemoryFdInfoKHR importFdInfo = {
                .sType = VK_STRUCTURE_TYPE_IMPORT_MEMORY_FD_INFO_KHR,
                .pNext = NULL,
                .handleType = VK_EXTERNAL_MEMORY_HANDLE_TYPE_DMA_BUF_BIT_EXT,
                .fd = import_fd,
            };

            typedef struct {
                VkStructureType sType;
                const void* pNext;
                VkImage image;
                VkBuffer buffer;
            } VkMemoryDedicatedAllocateInfo;

            VkMemoryDedicatedAllocateInfo dedicatedInfo = {
                .sType = VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO,
                .pNext = &importFdInfo,
                .image = g_swapchain_images[i],
                .buffer = VK_NULL_HANDLE,
            };

            VkMemoryRequirements memReq;
            pfn_getImageMemReq(device, g_swapchain_images[i], &memReq);

            VkMemoryAllocateInfo allocInfo = {
                .sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
                .pNext = &dedicatedInfo,
                .allocationSize = memReq.size,
                .memoryTypeIndex = find_memory_type(memReq.memoryTypeBits,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
            };

            r = pfn_allocMemory(device, &allocInfo, NULL, &g_swapchain_memory[i]);
            if (r != VK_SUCCESS) {
                LOGE("vkAllocateMemory[%u] (dmabuf import) failed: %d", i, r);
                close(import_fd);
                destroy_swapchain_images();
                return r;
            }
            r = pfn_bindImageMemory(device, g_swapchain_images[i], g_swapchain_memory[i], 0);
            if (r != VK_SUCCESS) {
                LOGE("vkBindImageMemory[%u] failed: %d", i, r);
                destroy_swapchain_images();
                return r;
            }

            LOGI("Swapchain image[%u]: VkImage=%p dmabuf_fd=%d stride=%u", i,
                 (void*)(uintptr_t)g_swapchain_images[i],
                 g_swapchain_dmabuf_fds[i], g_swapchain_strides[i]);
        }
    }

finish:
    g_swapchain_image_count = count;
    g_swapchain_current = 0;
    *pSwapchain = SHIM_NEXT_SWAPCHAIN_HANDLE();

    destroy_holdoff_images();

    if (!g_queue && pfn_getDeviceQueue) {
        pfn_getDeviceQueue(device, 0, 0, &g_queue);
        LOGI("Captured queue: %p", (void*)g_queue);
    }

    for (uint32_t i = 0; i < SHIM_MAX_SWAPCHAIN_IMAGES; i++) {
        if (g_present_fences[i] && pfn_destroyFence) {
            pfn_destroyFence(device, g_present_fences[i], NULL);
            g_present_fences[i] = VK_NULL_HANDLE;
        }
        g_present_fence_pending_export[i] = 0;
    }
    g_present_fence_exportable = 0;
    if (pfn_createFence) {
        int try_export = (pfn_getFenceFdKHR != NULL);
        VkExportFenceCreateInfo exportInfo = {
            .sType = VK_STRUCTURE_TYPE_EXPORT_FENCE_CREATE_INFO,
            .pNext = NULL,
            .handleTypes = VK_EXTERNAL_FENCE_HANDLE_TYPE_SYNC_FD_BIT,
        };
        VkFenceCreateInfo fci = {
            .sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO,
            .pNext = try_export ? &exportInfo : NULL,
            .flags = VK_FENCE_CREATE_SIGNALED_BIT,
        };
        VkResult fr = pfn_createFence(device, &fci, NULL, &g_present_fences[0]);
        if (fr != VK_SUCCESS && try_export) {
            LOGI("present fence export rejected (%d)", fr);
            try_export = 0;
            fci.pNext = NULL;
            fr = pfn_createFence(device, &fci, NULL, &g_present_fences[0]);
        }
        if (fr == VK_SUCCESS) {
            for (uint32_t i = 1; i < count; i++) {
                pfn_createFence(device, &fci, NULL, &g_present_fences[i]);
            }
        }
        g_present_fence_exportable = try_export;
        LOGI("Created %u present fences (exportable=%d)", count, g_present_fence_exportable);
    }

    for (uint32_t i = 0; i < 16; i++) {
        if (g_present_semaphores[i] && pfn_destroySemaphore) {
            pfn_destroySemaphore(device, g_present_semaphores[i], NULL);
            g_present_semaphores[i] = VK_NULL_HANDLE;
        }
    }
    if (pfn_createSemaphore && pfn_getSemaphoreFdKHR) {
        VkExportSemaphoreCreateInfo exportInfo = {
            .sType = VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO,
            .handleTypes = VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_SYNC_FD_BIT,
        };
        VkSemaphoreCreateInfo sci = {
            .sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO,
            .pNext = &exportInfo,
        };
        for (uint32_t i = 0; i < count; i++) {
            if (pfn_createSemaphore(device, &sci, NULL, &g_present_semaphores[i]) != VK_SUCCESS) {
                g_present_semaphores[i] = VK_NULL_HANDLE;
            }
        }
        LOGI("Created %u exportable present semaphores", count);
    } else {
        LOGI("vkGetSemaphoreFdKHR not available - async fences disabled");
    }

    LOGI("Swapchain created: handle=%p %u images%s, %ux%u, fmt=%u",
         (void*)(uintptr_t)*pSwapchain, count,
         g_swapchain_has_dmabuf ? " (AHB-backed)" : " (plain)",
         g_swapchain_width, g_swapchain_height, g_swapchain_format);
    return VK_SUCCESS;
}

static void shim_vkDestroySwapchainKHR(
    VkDevice device,
    VkSwapchainKHR swapchain,
    const VkAllocationCallbacks* pAllocator)
{
    LOGI("vkDestroySwapchainKHR (pending=%u current=%u)",
         g_pending_image_count, g_swapchain_image_count);

    if (pfn_deviceWaitIdle) {
        pfn_deviceWaitIdle(device);
    }

    for (uint32_t i = 0; i < SHIM_MAX_SWAPCHAIN_IMAGES; i++) {
        if (g_present_fences[i] && pfn_destroyFence) {
            pfn_destroyFence(device, g_present_fences[i], NULL);
            g_present_fences[i] = VK_NULL_HANDLE;
        }
    }
    for (uint32_t i = 0; i < 16; i++) {
        if (g_present_semaphores[i] && pfn_destroySemaphore) {
            pfn_destroySemaphore(device, g_present_semaphores[i], NULL);
            g_present_semaphores[i] = VK_NULL_HANDLE;
        }
    }

    destroy_holdoff_images();
    if (g_pending_image_count > 0) {
        destroy_pending_images();
    } else if (g_swapchain_image_count > 0) {
        for (uint32_t i = 0; i < g_swapchain_image_count; i++) {
            g_pending_images[i] = g_swapchain_images[i];
            g_pending_memory[i] = g_swapchain_memory[i];
            g_pending_dmabuf_fds[i] = g_swapchain_dmabuf_fds[i];
            g_pending_ahbs[i] = g_swapchain_ahbs[i];
            g_swapchain_images[i] = VK_NULL_HANDLE;
            g_swapchain_memory[i] = VK_NULL_HANDLE;
            g_swapchain_dmabuf_fds[i] = -1;
            g_swapchain_ahbs[i] = NULL;
        }
        g_pending_image_count = g_swapchain_image_count;
        g_swapchain_image_count = 0;
        g_swapchain_has_dmabuf = 0;
        LOGI("Retired current images to pending (deferred destroy)");
    }
}

static VkResult shim_vkGetSwapchainImagesKHR(
    VkDevice device,
    VkSwapchainKHR swapchain,
    uint32_t* pSwapchainImageCount,
    VkImage* pSwapchainImages)
{
    if (!pSwapchainImages) {
        *pSwapchainImageCount = g_swapchain_image_count;
        LOGI("vkGetSwapchainImagesKHR: query count=%u", g_swapchain_image_count);
        return VK_SUCCESS;
    }
    uint32_t copy = g_swapchain_image_count < *pSwapchainImageCount
                  ? g_swapchain_image_count : *pSwapchainImageCount;
    for (uint32_t i = 0; i < copy; i++)
        pSwapchainImages[i] = g_swapchain_images[i];
    *pSwapchainImageCount = copy;
    LOGI("vkGetSwapchainImagesKHR: returned %u images", copy);
    return (copy < g_swapchain_image_count) ? VK_INCOMPLETE : VK_SUCCESS;
}

static VkResult shim_vkAcquireNextImageKHR(
    VkDevice device,
    VkSwapchainKHR swapchain,
    uint64_t timeout,
    VkSemaphore semaphore,
    VkFence fence,
    uint32_t* pImageIndex)
{
    *pImageIndex = g_swapchain_current;
    g_swapchain_current = (g_swapchain_current + 1) % g_swapchain_image_count;

    // queue submit so unity wont deadlock waiting (gd reference)
    if (g_queue && (semaphore || fence) && pfn_queueSubmit) {
        VkSubmitInfo submit = {
            .sType = VK_STRUCTURE_TYPE_SUBMIT_INFO,
            .signalSemaphoreCount = semaphore ? 1 : 0,
            .pSignalSemaphores = semaphore ? &semaphore : NULL,
        };
        pfn_queueSubmit(g_queue, 1, &submit, fence);
    }

    return VK_SUCCESS;
}

static VkResult shim_vkQueuePresentKHR(
    VkQueue queue,
    const VkPresentInfoKHR* pPresentInfo)
{
    static int frame_count = 0;
    static int fps_frame_counter = 0;
    static uint32_t unity_fps = 0;
    static struct timespec fps_start = {0, 0};

    static struct timespec last_present = {0, 0};
    long max_fps_interval_ns = g_max_fps_interval_ns;
    if (max_fps_interval_ns > 0) {
        struct timespec now;
        clock_gettime(CLOCK_MONOTONIC, &now);
        if (last_present.tv_sec != 0 || last_present.tv_nsec != 0) {
            long target_s = last_present.tv_sec + (last_present.tv_nsec + max_fps_interval_ns) / 1000000000L;
            long target_ns = (last_present.tv_nsec + max_fps_interval_ns) % 1000000000L;
            long wait_ns = (target_s - now.tv_sec) * 1000000000L + (target_ns - now.tv_nsec);
            if (wait_ns > 0 && wait_ns < 500000000L) {
                struct timespec req = { .tv_sec = 0, .tv_nsec = wait_ns };
                while (nanosleep(&req, &req) == -1 && errno == EINTR) {}
            }
            if (wait_ns > 0) {
                last_present.tv_sec = target_s;
                last_present.tv_nsec = target_ns;
            } else {
                last_present = now;
            }
        } else {
            last_present = now;
        }
    }

    if (!g_queue) {
        g_queue = queue;
        LOGI("Captured VkQueue: %p", (void*)queue);
    }

    for (uint32_t sc = 0; sc < pPresentInfo->swapchainCount; sc++) {
        uint32_t imgIdx = pPresentInfo->pImageIndices[sc];
        if (imgIdx >= g_swapchain_image_count) continue;

        VkFence presentFence = g_present_fences[imgIdx];

        int prev_exported = g_present_fence_pending_export[imgIdx];
        g_present_fence_pending_export[imgIdx] = 0;
        if (presentFence && pfn_waitForFences && pfn_resetFences && !prev_exported) {
            if (!pfn_getFenceStatus ||
                pfn_getFenceStatus(g_device, presentFence) != VK_SUCCESS) {
                pfn_waitForFences(g_device, 1, &presentFence, VK_TRUE, UINT64_MAX);
            }
            pfn_resetFences(g_device, 1, &presentFence);
        }

        VkSemaphore exportSem = g_present_semaphores[imgIdx];
        int doSignalExport = (exportSem != VK_NULL_HANDLE && pfn_getSemaphoreFdKHR != NULL);

        if (pPresentInfo->waitSemaphoreCount > 0 && pfn_queueSubmit) {
            static const VkPipelineStageFlags waitStages[8] = {
                VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
            };
            uint32_t waitCount = pPresentInfo->waitSemaphoreCount;
            if (waitCount > 8) waitCount = 8;
            VkSubmitInfo submit = {
                .sType = VK_STRUCTURE_TYPE_SUBMIT_INFO,
                .waitSemaphoreCount = waitCount,
                .pWaitSemaphores = pPresentInfo->pWaitSemaphores,
                .pWaitDstStageMask = waitStages,
                .signalSemaphoreCount = doSignalExport ? 1 : 0,
                .pSignalSemaphores = doSignalExport ? &exportSem : NULL,
            };
            pfn_queueSubmit(queue, 1, &submit, presentFence);
        } else if (presentFence || doSignalExport) {
            VkSubmitInfo empty = {
                .sType = VK_STRUCTURE_TYPE_SUBMIT_INFO,
                .signalSemaphoreCount = doSignalExport ? 1 : 0,
                .pSignalSemaphores = doSignalExport ? &exportSem : NULL,
            };
            if (pfn_queueSubmit) pfn_queueSubmit(queue, 1, &empty, presentFence);
        }

        int acquireFd = -1;
        if (doSignalExport) {
            VkSemaphoreGetFdInfoKHR gi = {
                .sType = VK_STRUCTURE_TYPE_SEMAPHORE_GET_FD_INFO_KHR,
                .semaphore = exportSem,
                .handleType = VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_SYNC_FD_BIT,
            };
            if (pfn_getSemaphoreFdKHR(g_device, &gi, &acquireFd) != VK_SUCCESS) {
                acquireFd = -1;
            }
        }

        if (acquireFd < 0 && g_present_fence_exportable && presentFence && pfn_getFenceFdKHR) {
            VkFenceGetFdInfoKHR fi = {
                .sType = VK_STRUCTURE_TYPE_FENCE_GET_FD_INFO_KHR,
                .pNext = NULL,
                .fence = presentFence,
                .handleType = VK_EXTERNAL_FENCE_HANDLE_TYPE_SYNC_FD_BIT,
            };
            int fd = -1;
            if (pfn_getFenceFdKHR(g_device, &fi, &fd) == VK_SUCCESS && fd >= 0) {
                acquireFd = fd;
                g_present_fence_pending_export[imgIdx] = 1;
            }
        }

        if (acquireFd < 0 && g_using_system_driver && presentFence && pfn_waitForFences) {
            pfn_waitForFences(g_device, 1, &presentFence, VK_TRUE, UINT64_MAX);
        }

        if (g_swapchain_has_dmabuf && g_compositor_sock >= 0) {
            uint32_t msg[2] = { imgIdx, unity_fps };
            struct iovec iov = { .iov_base = msg, .iov_len = sizeof(msg) };
            char cmsgBuf[CMSG_SPACE(sizeof(int))];
            struct msghdr mh;
            memset(&mh, 0, sizeof(mh));
            mh.msg_iov = &iov;
            mh.msg_iovlen = 1;
            if (acquireFd >= 0) {
                mh.msg_control = cmsgBuf;
                mh.msg_controllen = sizeof(cmsgBuf);
                struct cmsghdr *c = CMSG_FIRSTHDR(&mh);
                c->cmsg_level = SOL_SOCKET;
                c->cmsg_type = SCM_RIGHTS;
                c->cmsg_len = CMSG_LEN(sizeof(int));
                memcpy(CMSG_DATA(c), &acquireFd, sizeof(int));
            }
            ssize_t sent = sendmsg(g_compositor_sock, &mh, MSG_NOSIGNAL);
            if (acquireFd >= 0) close(acquireFd);
            if (sent != (ssize_t)sizeof(msg)) {
                LOGE("Failed to send frame to compositor: %s", strerror(errno));
                close(g_compositor_sock);
                g_compositor_sock = -1;
            }
        } else if (acquireFd >= 0) {
            close(acquireFd);
        }

        if (pPresentInfo->pResults)
            pPresentInfo->pResults[sc] = VK_SUCCESS;
    }

    // calculate Unity fps (no difference from compositor most of the time)
    frame_count++;
    fps_frame_counter++;
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    if (fps_start.tv_sec == 0 && fps_start.tv_nsec == 0) {
        fps_start = now;
    } else {
        long elapsed_ns = (now.tv_sec - fps_start.tv_sec) * 1000000000L +
                          (now.tv_nsec - fps_start.tv_nsec);
        if (elapsed_ns >= 1000000000L) {
            unity_fps = (uint32_t)((long long)fps_frame_counter * 1000000000LL / elapsed_ns);
            fps_frame_counter = 0;
            fps_start = now;
        }
    }

    if (frame_count == 1) {
        LOGI("First frame presented!");
    } else if (frame_count % 3000 == 0) {
        LOGI("Presented %d frames (Unity FPS: %u)", frame_count, unity_fps);
    }

    return VK_SUCCESS;
}

static VkResult shim_vkCreateDevice(
    VkPhysicalDevice physicalDevice,
    const VkDeviceCreateInfo* pCreateInfo,
    const VkAllocationCallbacks* pAllocator,
    VkDevice* pDevice)
{
    typedef VkResult (*PFN_vkCreateDevice)(VkPhysicalDevice, const VkDeviceCreateInfo*,
                                            const VkAllocationCallbacks*, VkDevice*);
    PFN_vkCreateDevice real_createDevice = (PFN_vkCreateDevice)
        real_vkGetInstanceProcAddr(g_instance, "vkCreateDevice");
    if (!real_createDevice) return VK_ERROR_INITIALIZATION_FAILED;

    uint32_t count = pCreateInfo->enabledExtensionCount;
    const char** newExts = (const char**)malloc(sizeof(char*) * (count + 10));
    uint32_t newCount = 0;
    int has_ahb_ext = 0;
    int has_ext_mem = 0;
    int has_drm_modifier = 0;
    int has_ext_mem_fd = 0;
    int has_ext_mem_dmabuf = 0;
    int has_image_format_list = 0;
    int has_dedicated_alloc = 0;
    int has_get_mem_req2 = 0;
    int has_ext_fence = 0;
    int has_ext_fence_fd = 0;

    for (uint32_t i = 0; i < count; i++) {
        const char* ext = pCreateInfo->ppEnabledExtensionNames[i];
        if (strcmp(ext, "VK_KHR_swapchain") == 0 ||
            strcmp(ext, "VK_KHR_swapchain_mutable_format") == 0 ||
            strcmp(ext, "VK_KHR_incremental_present") == 0 ||
            strcmp(ext, "VK_EXT_hdr_metadata") == 0) {
            LOGI("Stripping device ext: %s", ext);
            continue;
        }
        if (strcmp(ext, "VK_ANDROID_external_memory_android_hardware_buffer") == 0) has_ahb_ext = 1;
        if (strcmp(ext, "VK_KHR_external_memory") == 0) has_ext_mem = 1;
        if (strcmp(ext, "VK_EXT_image_drm_format_modifier") == 0) has_drm_modifier = 1;
        if (strcmp(ext, "VK_KHR_external_memory_fd") == 0) has_ext_mem_fd = 1;
        if (strcmp(ext, "VK_EXT_external_memory_dma_buf") == 0) has_ext_mem_dmabuf = 1;
        if (strcmp(ext, "VK_KHR_image_format_list") == 0) has_image_format_list = 1;
        if (strcmp(ext, "VK_KHR_dedicated_allocation") == 0) has_dedicated_alloc = 1;
        if (strcmp(ext, "VK_KHR_get_memory_requirements2") == 0) has_get_mem_req2 = 1;
        if (strcmp(ext, "VK_KHR_external_fence") == 0) has_ext_fence = 1;
        if (strcmp(ext, "VK_KHR_external_fence_fd") == 0) has_ext_fence_fd = 1;
        newExts[newCount++] = ext;
    }

    if (!has_ahb_ext) {
        newExts[newCount++] = "VK_ANDROID_external_memory_android_hardware_buffer";
        LOGI("Injecting device ext: VK_ANDROID_external_memory_android_hardware_buffer");
    }
    if (!has_ext_mem) {
        newExts[newCount++] = "VK_KHR_external_memory";
        LOGI("Injecting device ext: VK_KHR_external_memory");
    }
    if (!has_dedicated_alloc) {
        newExts[newCount++] = "VK_KHR_dedicated_allocation";
        LOGI("Injecting device ext: VK_KHR_dedicated_allocation");
    }
    if (!has_get_mem_req2) {
        newExts[newCount++] = "VK_KHR_get_memory_requirements2";
        LOGI("Injecting device ext: VK_KHR_get_memory_requirements2");
    }
    if (!g_using_system_driver) {
        if (!has_drm_modifier) {
            newExts[newCount++] = "VK_EXT_image_drm_format_modifier";
        }
        if (!has_ext_mem_fd) {
            newExts[newCount++] = "VK_KHR_external_memory_fd";
        }
        if (!has_ext_mem_dmabuf) {
            newExts[newCount++] = "VK_EXT_external_memory_dma_buf";
        }
        if (!has_image_format_list) {
            newExts[newCount++] = "VK_KHR_image_format_list";
        }
    }

    uint32_t baseCount = newCount;
    if (!has_ext_fence) {
        newExts[newCount++] = "VK_KHR_external_fence";
    }
    if (!has_ext_fence_fd) {
        newExts[newCount++] = "VK_KHR_external_fence_fd";
    }

    VkDeviceCreateInfo modInfo = *pCreateInfo;
    modInfo.ppEnabledExtensionNames = newExts;
    modInfo.enabledExtensionCount = newCount;

    LOGI("vkCreateDevice with %u extensions (stripped %u)", newCount, count - newCount);
    VkResult result = real_createDevice(physicalDevice, &modInfo, pAllocator, pDevice);
    if (result == VK_ERROR_EXTENSION_NOT_PRESENT && newCount > baseCount) {
        LOGI("vkCreateDevice rejected fence-fd ext");
        modInfo.enabledExtensionCount = baseCount;
        result = real_createDevice(physicalDevice, &modInfo, pAllocator, pDevice);
    }
    free(newExts);

    if (result == VK_SUCCESS) {
        g_physical_device = physicalDevice;
        g_device = *pDevice;
        LOGI("VkDevice created: %p", (void*)*pDevice);

        // get gpu info for stats overlay
        typedef void (*PFN_vkGetPhysicalDeviceProperties)(VkPhysicalDevice, VkPhysicalDeviceProperties*);
        PFN_vkGetPhysicalDeviceProperties getProps = (PFN_vkGetPhysicalDeviceProperties)
            real_vkGetInstanceProcAddr(g_instance, "vkGetPhysicalDeviceProperties");
        if (getProps) {
            VkPhysicalDeviceProperties props;
            getProps(physicalDevice, &props);
            strncpy(g_gpu_name, props.deviceName, sizeof(g_gpu_name) - 1);
            g_gpu_name[63] = '\0';
            g_vk_api_version = props.apiVersion;
            g_vk_driver_version = props.driverVersion;
            LOGI("GPU: %s, Vulkan %u.%u.%u, driver 0x%x",
                 g_gpu_name,
                 VK_VERSION_MAJOR(g_vk_api_version),
                 VK_VERSION_MINOR(g_vk_api_version),
                 VK_VERSION_PATCH(g_vk_api_version),
                 g_vk_driver_version);
        }
    } else {
        LOGE("vkCreateDevice failed: %d", result);
    }

    return result;
}

static VkResult shim_vkEnumerateDeviceExtensionProperties(
    VkPhysicalDevice physicalDevice,
    const char* pLayerName,
    uint32_t* pPropertyCount,
    VkExtensionProperties* pProperties)
{
    typedef VkResult (*PFN_t)(VkPhysicalDevice, const char*, uint32_t*, VkExtensionProperties*);
    PFN_t real_fn = (PFN_t)real_vkGetInstanceProcAddr(g_instance, "vkEnumerateDeviceExtensionProperties");
    if (!real_fn) return VK_ERROR_INITIALIZATION_FAILED;

    uint32_t realCount = 0;
    VkResult result = real_fn(physicalDevice, pLayerName, &realCount, NULL);
    if (result != VK_SUCCESS) return result;

    static const char* fakeDevExts[] = { "VK_KHR_swapchain" };
    uint32_t numFake = 1;
    uint32_t totalCount = realCount + numFake;

    if (!pProperties) {
        *pPropertyCount = totalCount;
        return VK_SUCCESS;
    }

    uint32_t copyCount = realCount < *pPropertyCount ? realCount : *pPropertyCount;
    result = real_fn(physicalDevice, pLayerName, &copyCount, pProperties);

    for (uint32_t f = 0; f < numFake && copyCount < *pPropertyCount; f++) {
        strncpy(pProperties[copyCount].extensionName, fakeDevExts[f],
                VK_MAX_EXTENSION_NAME_SIZE);
        pProperties[copyCount].specVersion = 70;
        copyCount++;
    }

    *pPropertyCount = copyCount;
    return (copyCount < totalCount) ? VK_INCOMPLETE : VK_SUCCESS;
}

static PFN_vkVoidFunction shim_vkGetDeviceProcAddr(VkDevice device, const char* pName) {
    if (strcmp(pName, "vkQueuePresentKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkQueuePresentKHR;
    if (strcmp(pName, "vkCreateSwapchainKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkCreateSwapchainKHR;
    if (strcmp(pName, "vkDestroySwapchainKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkDestroySwapchainKHR;
    if (strcmp(pName, "vkGetSwapchainImagesKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkGetSwapchainImagesKHR;
    if (strcmp(pName, "vkAcquireNextImageKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkAcquireNextImageKHR;

    if (!real_vkGetDeviceProcAddr && real_vkGetInstanceProcAddr && g_instance) {
        real_vkGetDeviceProcAddr = (PFN_vkGetDeviceProcAddr)
            real_vkGetInstanceProcAddr(g_instance, "vkGetDeviceProcAddr");
    }
    if (real_vkGetDeviceProcAddr)
        return real_vkGetDeviceProcAddr(device, pName);
    return NULL;
}

PFN_vkVoidFunction vkGetInstanceProcAddr(VkInstance instance, const char* pName) {
    load_real_vulkan();

    if (strcmp(pName, "vkGetDeviceProcAddr") == 0)
        return (PFN_vkVoidFunction)shim_vkGetDeviceProcAddr;
    if (strcmp(pName, "vkCreateInstance") == 0)
        return (PFN_vkVoidFunction)shim_vkCreateInstance;
    if (strcmp(pName, "vkEnumerateInstanceExtensionProperties") == 0)
        return (PFN_vkVoidFunction)shim_vkEnumerateInstanceExtensionProperties;
    if (strcmp(pName, "vkCreateXlibSurfaceKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkCreateXlibSurfaceKHR;
    if (strcmp(pName, "vkCreateXcbSurfaceKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkCreateXcbSurfaceKHR;
    if (strcmp(pName, "vkCreateWaylandSurfaceKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkCreateWaylandSurfaceKHR;
    if (strcmp(pName, "vkGetPhysicalDeviceXlibPresentationSupportKHR") == 0 ||
        strcmp(pName, "vkGetPhysicalDeviceXcbPresentationSupportKHR") == 0 ||
        strcmp(pName, "vkGetPhysicalDeviceWaylandPresentationSupportKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkGetPhysicalDevicePresentationSupport;

    if (strcmp(pName, "vkDestroySurfaceKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkDestroySurfaceKHR;
    if (strcmp(pName, "vkGetPhysicalDeviceSurfaceSupportKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkGetPhysicalDeviceSurfaceSupportKHR;
    if (strcmp(pName, "vkGetPhysicalDeviceSurfaceCapabilitiesKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
    if (strcmp(pName, "vkGetPhysicalDeviceSurfaceFormatsKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkGetPhysicalDeviceSurfaceFormatsKHR;
    if (strcmp(pName, "vkGetPhysicalDeviceSurfacePresentModesKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkGetPhysicalDeviceSurfacePresentModesKHR;

    if (strcmp(pName, "vkCreateDevice") == 0)
        return (PFN_vkVoidFunction)shim_vkCreateDevice;
    if (strcmp(pName, "vkEnumerateDeviceExtensionProperties") == 0)
        return (PFN_vkVoidFunction)shim_vkEnumerateDeviceExtensionProperties;
    if (strcmp(pName, "vkCreateSwapchainKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkCreateSwapchainKHR;
    if (strcmp(pName, "vkDestroySwapchainKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkDestroySwapchainKHR;
    if (strcmp(pName, "vkGetSwapchainImagesKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkGetSwapchainImagesKHR;
    if (strcmp(pName, "vkAcquireNextImageKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkAcquireNextImageKHR;
    if (strcmp(pName, "vkQueuePresentKHR") == 0)
        return (PFN_vkVoidFunction)shim_vkQueuePresentKHR;

    if (real_vkGetInstanceProcAddr)
        return real_vkGetInstanceProcAddr(instance, pName);

    return NULL;
}

VkResult vkCreateInstance(
    const VkInstanceCreateInfo* pCreateInfo,
    const VkAllocationCallbacks* pAllocator,
    VkInstance* pInstance)
{
    return shim_vkCreateInstance(pCreateInfo, pAllocator, pInstance);
}

VkResult vkEnumerateInstanceExtensionProperties(
    const char* pLayerName,
    uint32_t* pPropertyCount,
    VkExtensionProperties* pProperties)
{
    return shim_vkEnumerateInstanceExtensionProperties(pLayerName, pPropertyCount, pProperties);
}

VkResult vkEnumerateInstanceVersion(uint32_t* pApiVersion) {
    load_real_vulkan();
    PFN_vkEnumerateInstanceVersion fn = NULL;
    if (real_vkGetInstanceProcAddr)
        fn = (PFN_vkEnumerateInstanceVersion)real_vkGetInstanceProcAddr(NULL, "vkEnumerateInstanceVersion");
    if (!fn)
        fn = (PFN_vkEnumerateInstanceVersion)dlsym(g_real_vulkan, "vkEnumerateInstanceVersion");
    if (fn) return fn(pApiVersion);
    if (pApiVersion) *pApiVersion = VK_API_VERSION_1_0;
    return VK_SUCCESS;
}

VkResult vkEnumerateInstanceLayerProperties(
    uint32_t* pPropertyCount,
    VkLayerProperties* pProperties)
{
    load_real_vulkan();
    typedef VkResult (*PFN_t)(uint32_t*, VkLayerProperties*);
    PFN_t fn = (PFN_t)dlsym(g_real_vulkan, "vkEnumerateInstanceLayerProperties");
    if (fn) return fn(pPropertyCount, pProperties);
    if (pPropertyCount) *pPropertyCount = 0;
    return VK_SUCCESS;
}

// finally done
