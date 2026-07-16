#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <stdint.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/socket.h>
#include <sys/un.h>

#define AUDIO_SOCK      "polydroid_audio"
#define AUDIO_MAGIC     0x31414450u
#define PA_SAMPLE_S16LE 3

typedef unsigned long snd_pcm_uframes_t;
typedef long snd_pcm_sframes_t;
typedef struct snd_pcm snd_pcm_t;
typedef struct snd_pcm_hw_params snd_pcm_hw_params_t;
typedef struct snd_pcm_sw_params snd_pcm_sw_params_t;

struct snd_pcm {
    int fd;
    unsigned int rate;
    int channels;
    int started;
    uint8_t carry[16];
    int carry_len;
    int carry_off;
};

static int audio_enabled(void) {
    const char *f = getenv("POLYDROID_POLYTORIA2");
    return f && f[0] == '1';
}

static int connect_bridge(void) {
    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) return -1;
    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    addr.sun_path[0] = '\0';
    size_t n = strlen(AUDIO_SOCK);
    memcpy(addr.sun_path + 1, AUDIO_SOCK, n);
    socklen_t alen = (socklen_t)(offsetof(struct sockaddr_un, sun_path) + 1 + n);
    if (connect(fd, (struct sockaddr *)&addr, alen) != 0) { close(fd); return -1; }
    return fd;
}

static int write_all(int fd, const void *buf, size_t n) {
    const char *p = (const char *)buf;
    size_t off = 0;
    while (off < n) {
        ssize_t w = write(fd, p + off, n - off);
        if (w <= 0) return -1;
        off += (size_t)w;
    }
    return 0;
}

int snd_pcm_open(snd_pcm_t **pcm, const char *name, int stream, int mode) {
    (void)name; (void)stream; (void)mode;
    if (!pcm) return -1;
    snd_pcm_t *p = (snd_pcm_t *)calloc(1, sizeof(*p));
    if (!p) return -1;
    p->fd = -1;
    p->rate = 48000;
    p->channels = 2;
    *pcm = p;
    return 0;
}

int snd_pcm_close(snd_pcm_t *pcm) {
    if (pcm) { if (pcm->fd >= 0) close(pcm->fd); free(pcm); }
    return 0;
}


size_t snd_pcm_hw_params_sizeof(void) { return 1024; }
size_t snd_pcm_sw_params_sizeof(void) { return 1024; }
int snd_pcm_hw_params_any(snd_pcm_t *p, snd_pcm_hw_params_t *h) { (void)p; (void)h; return 0; }
int snd_pcm_hw_params_set_access(snd_pcm_t *p, snd_pcm_hw_params_t *h, int a) { (void)p; (void)h; (void)a; return 0; }
int snd_pcm_hw_params_set_format(snd_pcm_t *p, snd_pcm_hw_params_t *h, int f) { (void)p; (void)h; (void)f; return 0; }
int snd_pcm_hw_params_set_channels(snd_pcm_t *p, snd_pcm_hw_params_t *h, unsigned int ch) { (void)h; if (p) p->channels = (int)ch; return 0; }
int snd_pcm_hw_params_set_rate_near(snd_pcm_t *p, snd_pcm_hw_params_t *h, unsigned int *val, int *dir) { (void)h; (void)dir; if (p && val) p->rate = *val; return 0; }
int snd_pcm_hw_params_set_buffer_size_near(snd_pcm_t *p, snd_pcm_hw_params_t *h, snd_pcm_uframes_t *val) { (void)p; (void)h; (void)val; return 0; }
int snd_pcm_hw_params_set_period_size_near(snd_pcm_t *p, snd_pcm_hw_params_t *h, snd_pcm_uframes_t *val, int *dir) { (void)p; (void)h; (void)val; (void)dir; return 0; }
int snd_pcm_hw_params_set_periods_near(snd_pcm_t *p, snd_pcm_hw_params_t *h, unsigned int *val, int *dir) { (void)p; (void)h; (void)val; (void)dir; return 0; }
void snd_pcm_hw_params_free(snd_pcm_hw_params_t *h) { (void)h; }

int snd_pcm_hw_params(snd_pcm_t *p, snd_pcm_hw_params_t *h) {
    (void)h;
    if (!p) return -1;
    if (!audio_enabled()) return 0;
    if (p->fd < 0) p->fd = connect_bridge();
    if (p->fd >= 0 && !p->started) {
        uint8_t hdr[12];
        uint32_t magic = AUDIO_MAGIC, rate = p->rate;
        memcpy(hdr + 0, &magic, 4);
        memcpy(hdr + 4, &rate, 4);
        hdr[8] = (uint8_t)(p->channels > 0 ? p->channels : 2);
        hdr[9] = PA_SAMPLE_S16LE; // Godot's alsa driver always uses S16_LE
        hdr[10] = 0; hdr[11] = 0;
        if (write_all(p->fd, hdr, sizeof(hdr)) == 0) {
            p->started = 1;
            int sndbuf = 8192;
            setsockopt(p->fd, SOL_SOCKET, SO_SNDBUF, &sndbuf, sizeof(sndbuf));
            int fl = fcntl(p->fd, F_GETFL, 0);
            if (fl >= 0) fcntl(p->fd, F_SETFL, fl | O_NONBLOCK);
        } else {
            fprintf(stderr, "Godot: header send failed, audio off\n");
            close(p->fd); p->fd = -1;
        }
    }
    return 0;
}

int snd_pcm_sw_params_current(snd_pcm_t *p, snd_pcm_sw_params_t *s) { (void)p; (void)s; return 0; }
int snd_pcm_sw_params_set_avail_min(snd_pcm_t *p, snd_pcm_sw_params_t *s, snd_pcm_uframes_t v) { (void)p; (void)s; (void)v; return 0; }
int snd_pcm_sw_params_set_start_threshold(snd_pcm_t *p, snd_pcm_sw_params_t *s, snd_pcm_uframes_t v) { (void)p; (void)s; (void)v; return 0; }
int snd_pcm_sw_params(snd_pcm_t *p, snd_pcm_sw_params_t *s) { (void)p; (void)s; return 0; }

snd_pcm_sframes_t snd_pcm_writei(snd_pcm_t *p, const void *buf, snd_pcm_uframes_t frames) {
    if (!p) return -1;
    if (p->fd < 0 || !buf || !frames)
        return (snd_pcm_sframes_t)frames;
    int fsz = (p->channels > 0 ? p->channels : 2) * 2; /* S16 */
    if (fsz > (int)sizeof(p->carry)) fsz = sizeof(p->carry);
    if (p->carry_len) {
        ssize_t w = send(p->fd, p->carry + p->carry_off, p->carry_len, MSG_NOSIGNAL);
        if (w < 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) return -EAGAIN;
            close(p->fd); p->fd = -1;
            return (snd_pcm_sframes_t)frames;
        }
        p->carry_off += (int)w;
        p->carry_len -= (int)w;
        if (p->carry_len) return -EAGAIN;
        p->carry_off = 0;
    }
    ssize_t s = send(p->fd, buf, (size_t)frames * (size_t)fsz, MSG_NOSIGNAL);
    if (s < 0) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) return -EAGAIN;
        close(p->fd); p->fd = -1;
        return (snd_pcm_sframes_t)frames;
    }
    size_t full = (size_t)s / (size_t)fsz;
    size_t rem = (size_t)s % (size_t)fsz;
    if (rem) {
        p->carry_len = fsz - (int)rem;
        p->carry_off = 0;
        memcpy(p->carry, (const char *)buf + full * fsz + rem, (size_t)p->carry_len);
        full += 1;
    }
    if (!full) return -EAGAIN;
    return (snd_pcm_sframes_t)full;
}

int snd_pcm_recover(snd_pcm_t *p, int err, int silent) { (void)p; (void)err; (void)silent; return 0; }
int snd_pcm_prepare(snd_pcm_t *p) { (void)p; return 0; }
int snd_pcm_drain(snd_pcm_t *p) { (void)p; return 0; }
int snd_pcm_drop(snd_pcm_t *p) { (void)p; return 0; }
int snd_pcm_start(snd_pcm_t *p) { (void)p; return 0; }
int snd_pcm_nonblock(snd_pcm_t *p, int n) { (void)p; (void)n; return 0; }

int snd_device_name_hint(int card, const char *iface, void ***hints) {
    (void)card; (void)iface;
    static void *empty[1] = { NULL };
    if (hints) *hints = empty;
    return 0;
}
char *snd_device_name_get_hint(const void *hint, const char *id) { (void)hint; (void)id; return NULL; }
int snd_device_name_free_hint(void **hints) { (void)hints; return 0; }

const char *snd_strerror(int e) { (void)e; return "polydroid alsa"; }
const char *snd_asoundlib_version(void) { return "1.2.0"; }
