/*
 * libpulse-simple.so.0 stub used for FMOD API
 * FMOD (and Unity along with it) will crash without this
 * will be rewritten later (or replaced) for actual audio support
 */

#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#define STUB_LOG(fmt, ...) fprintf(stderr, "[pulse-stub] " fmt "\n", ##__VA_ARGS__)

typedef enum pa_sample_format {
    PA_SAMPLE_U8,
    PA_SAMPLE_ALAW,
    PA_SAMPLE_ULAW,
    PA_SAMPLE_S16LE,
    PA_SAMPLE_S16BE,
    PA_SAMPLE_FLOAT32LE,
    PA_SAMPLE_FLOAT32BE,
    PA_SAMPLE_S32LE,
    PA_SAMPLE_S32BE,
    PA_SAMPLE_S24LE,
    PA_SAMPLE_S24BE,
    PA_SAMPLE_S24_32LE,
    PA_SAMPLE_S24_32BE,
    PA_SAMPLE_MAX,
    PA_SAMPLE_INVALID = -1
} pa_sample_format_t;

typedef struct pa_sample_spec {
    pa_sample_format_t format;
    unsigned int rate;
    unsigned char channels;
} pa_sample_spec;

typedef struct pa_channel_map {
    unsigned char channels;
    int map[32];
} pa_channel_map;

typedef struct pa_buffer_attr {
    unsigned int maxlength;
    unsigned int tlength;
    unsigned int prebuf;
    unsigned int minreq;
    unsigned int fragsize;
} pa_buffer_attr;

typedef enum pa_stream_direction {
    PA_STREAM_NODIRECTION,
    PA_STREAM_PLAYBACK,
    PA_STREAM_RECORD,
    PA_STREAM_UPLOAD
} pa_stream_direction_t;

typedef struct pa_simple {
    pa_sample_spec spec;
    int dummy;
} pa_simple;

typedef unsigned long long pa_usec_t;

pa_simple *pa_simple_new(
    const char *server,
    const char *name,
    pa_stream_direction_t dir,
    const char *dev,
    const char *stream_name,
    const pa_sample_spec *ss,
    const pa_channel_map *map,
    const pa_buffer_attr *attr,
    int *error)
{
    STUB_LOG("pa_simple_new(name=%s, dir=%d, rate=%u, ch=%u, fmt=%d)",
        name ? name : "NULL", dir,
        ss ? ss->rate : 0, ss ? ss->channels : 0, ss ? ss->format : -1);

    pa_simple *s = calloc(1, sizeof(pa_simple));
    if (!s) {
        if (error) *error = -1;
        return NULL;
    }
    if (ss) s->spec = *ss;
    if (error) *error = 0;
    return s;
}

// write audio data
int pa_simple_write(pa_simple *s, const void *data, size_t bytes, int *error) {
    STUB_LOG("pa_simple_write(%p, %zu bytes)", (void*)s, bytes);
    (void)data;
    if (error) *error = 0;
    return 0;
}

// read it
int pa_simple_read(pa_simple *s, void *data, size_t bytes, int *error) {
    STUB_LOG("pa_simple_read(%p, %zu bytes)", (void*)s, bytes);
    if (data) memset(data, 0, bytes);
    if (error) *error = 0;
    return 0;
}

int pa_simple_drain(pa_simple *s, int *error) {
    STUB_LOG("pa_simple_drain");
    (void)s;
    if (error) *error = 0;
    return 0;
}

int pa_simple_flush(pa_simple *s, int *error) {
    STUB_LOG("pa_simple_flush");
    (void)s;
    if (error) *error = 0;
    return 0;
}

void pa_simple_free(pa_simple *s) {
    STUB_LOG("pa_simple_free(%p)", (void*)s);
    free(s);
}

pa_usec_t pa_simple_get_latency(pa_simple *s, int *error) {
    STUB_LOG("pa_simple_get_latency(%p)", (void*)s);
    if (error) *error = 0;
    return 20000; /* 20ms */
}


const char *pa_strerror(int error) {
    return "stub: no PulseAudio";
}


typedef struct pa_mainloop {
    int dummy;
} pa_mainloop;

typedef struct pa_mainloop_api {
    void *userdata;
} pa_mainloop_api;

static pa_mainloop_api g_stub_api;

pa_mainloop *pa_mainloop_new(void) {
    STUB_LOG("pa_mainloop_new");
    pa_mainloop *m = calloc(1, sizeof(pa_mainloop));
    return m;
}

void pa_mainloop_free(pa_mainloop *m) {
    STUB_LOG("pa_mainloop_free");
    free(m);
}

pa_mainloop_api *pa_mainloop_get_api(pa_mainloop *m) {
    (void)m;
    return &g_stub_api;
}

int pa_mainloop_iterate(pa_mainloop *m, int block, int *retval) {
    STUB_LOG("pa_mainloop_iterate(block=%d)", block);
    (void)m; (void)block;
    if (retval) *retval = 0;
    return 1;
}

int pa_mainloop_run(pa_mainloop *m, int *retval) {
    (void)m;
    if (retval) *retval = 0;
    return 0;
}

void pa_mainloop_quit(pa_mainloop *m, int retval) {
    (void)m; (void)retval;
}

int pa_mainloop_prepare(pa_mainloop *m, int timeout) { (void)m; return 0; }
int pa_mainloop_poll(pa_mainloop *m) { (void)m; return 0; }
int pa_mainloop_dispatch(pa_mainloop *m) { (void)m; return 0; }

// --------------------- mainloop ------------
typedef struct pa_threaded_mainloop { int dummy; } pa_threaded_mainloop;

pa_threaded_mainloop *pa_threaded_mainloop_new(void) {
    STUB_LOG("pa_threaded_mainloop_new");
    return calloc(1, sizeof(pa_threaded_mainloop));
}
void pa_threaded_mainloop_free(pa_threaded_mainloop *m) { free(m); }
int pa_threaded_mainloop_start(pa_threaded_mainloop *m) { (void)m; return 0; }
void pa_threaded_mainloop_stop(pa_threaded_mainloop *m) { (void)m; }
void pa_threaded_mainloop_lock(pa_threaded_mainloop *m) { (void)m; }
void pa_threaded_mainloop_unlock(pa_threaded_mainloop *m) { (void)m; }
void pa_threaded_mainloop_wait(pa_threaded_mainloop *m) { (void)m; }
void pa_threaded_mainloop_signal(pa_threaded_mainloop *m, int wait_for_accept) { (void)m; (void)wait_for_accept; }
void pa_threaded_mainloop_accept(pa_threaded_mainloop *m) { (void)m; }
pa_mainloop_api *pa_threaded_mainloop_get_api(pa_threaded_mainloop *m) {
    (void)m;
    return &g_stub_api;
}
int pa_threaded_mainloop_in_thread(pa_threaded_mainloop *m) { (void)m; return 0; }

typedef enum pa_context_state {
    PA_CONTEXT_UNCONNECTED = 0,
    PA_CONTEXT_CONNECTING,
    PA_CONTEXT_AUTHORIZING,
    PA_CONTEXT_SETTING_NAME,
    PA_CONTEXT_READY,
    PA_CONTEXT_FAILED,
    PA_CONTEXT_TERMINATED
} pa_context_state_t;

typedef void (*pa_context_notify_cb_t)(void *c, void *userdata);
typedef void (*pa_context_success_cb_t)(void *c, int success, void *userdata);

typedef struct pa_context {
    pa_context_state_t state;
    pa_context_notify_cb_t state_cb;
    void *state_cb_userdata;
} pa_context;

pa_context *pa_context_new(pa_mainloop_api *api, const char *name) {
    STUB_LOG("pa_context_new(name=%s)", name ? name : "NULL");
    (void)api;
    pa_context *c = calloc(1, sizeof(pa_context));
    if (c) c->state = PA_CONTEXT_UNCONNECTED;
    return c;
}

pa_context *pa_context_new_with_proplist(pa_mainloop_api *api, const char *name, void *proplist) {
    (void)proplist;
    return pa_context_new(api, name);
}

void pa_context_unref(pa_context *c) {
    STUB_LOG("pa_context_unref");
    free(c);
}

pa_context *pa_context_ref(pa_context *c) { return c; }

int pa_context_connect(pa_context *c, const char *server, int flags, void *api) {
    STUB_LOG("pa_context_connect(server=%s)", server ? server : "NULL");
    (void)flags; (void)api;
    if (c) {
        c->state = PA_CONTEXT_READY;
        if (c->state_cb) c->state_cb(c, c->state_cb_userdata);
    }
    return 0;
}

void pa_context_disconnect(pa_context *c) {
    STUB_LOG("pa_context_disconnect");
    if (c) c->state = PA_CONTEXT_TERMINATED;
}

pa_context_state_t pa_context_get_state(pa_context *c) {
    pa_context_state_t s = c ? c->state : PA_CONTEXT_FAILED;
    STUB_LOG("pa_context_get_state() = %d", s);
    return s;
}

void pa_context_set_state_callback(pa_context *c, pa_context_notify_cb_t cb, void *userdata) {
    if (c) {
        c->state_cb = cb;
        c->state_cb_userdata = userdata;
    }
}

int pa_context_errno(pa_context *c) { (void)c; return 0; }
int pa_context_is_pending(pa_context *c) { (void)c; return 0; }

typedef struct pa_operation { int dummy; } pa_operation;
typedef enum pa_operation_state { PA_OPERATION_RUNNING, PA_OPERATION_DONE, PA_OPERATION_CANCELLED } pa_operation_state_t;

static pa_operation g_done_op;


typedef unsigned int pa_volume_t;
typedef struct pa_cvolume { unsigned char channels; pa_volume_t values[32]; } pa_cvolume;
#define PA_VOLUME_NORM ((pa_volume_t)0x10000U)

void pa_operation_unref(pa_operation *o) { (void)o; }
pa_operation *pa_operation_ref(pa_operation *o) { return o; }
pa_operation_state_t pa_operation_get_state(pa_operation *o) { (void)o; return PA_OPERATION_DONE; }
void pa_operation_cancel(pa_operation *o) { (void)o; }


typedef struct pa_sink_info {
    const char *name;
    unsigned int index;
    const char *description;
    pa_sample_spec sample_spec;
    pa_channel_map channel_map;
    unsigned int owner_module;
    pa_cvolume volume;
    int mute;
    unsigned int monitor_source;
    const char *monitor_source_name;
    pa_usec_t latency;
    const char *driver;
    int flags;
    void *proplist;
    pa_usec_t configured_latency;
    pa_volume_t base_volume;
    int state;
    unsigned int n_volume_steps;
    unsigned int card;
    unsigned int n_ports;
    void **ports;
    void *active_port;
    unsigned char n_formats;
    void **formats;
} pa_sink_info;

typedef void (*pa_sink_info_cb_t)(void *c, const pa_sink_info *i, int eol, void *userdata);

pa_operation *pa_context_get_sink_info_list(pa_context *c, pa_sink_info_cb_t cb, void *userdata) {
    STUB_LOG("pa_context_get_sink_info_list");
    if (cb) {
        // report a fake sink
        pa_sink_info info;
        memset(&info, 0, sizeof(info));
        info.name = "polydroid_audio";
        info.index = 0;
        info.description = "Tottaly real linux Audio Output, trust me bro";
        info.sample_spec.format = PA_SAMPLE_S16LE;
        info.sample_spec.rate = 48000;
        info.sample_spec.channels = 2;
        info.channel_map.channels = 2;
        info.volume.channels = 2;
        info.volume.values[0] = PA_VOLUME_NORM;
        info.volume.values[1] = PA_VOLUME_NORM;
        info.driver = "polydroid-pulse";
        info.base_volume = PA_VOLUME_NORM;
        info.state = 0; // 0 = running
        info.n_volume_steps = 65537;
        cb(c, &info, 0, userdata);
        /* EOL marker */
        cb(c, NULL, 1, userdata);
    }
    return &g_done_op;
}

pa_operation *pa_context_get_sink_info_by_name(pa_context *c, const char *name, pa_sink_info_cb_t cb, void *userdata) {
    STUB_LOG("pa_context_get_sink_info_by_name(%s)", name ? name : "NULL");
    return pa_context_get_sink_info_list(c, cb, userdata);
}

pa_operation *pa_context_get_sink_info_by_index(pa_context *c, unsigned int idx, pa_sink_info_cb_t cb, void *userdata) {
    STUB_LOG("pa_context_get_sink_info_by_index(%u)", idx);
    return pa_context_get_sink_info_list(c, cb, userdata);
}

typedef struct pa_source_info {
    unsigned int index;
    const char *name;
    const char *description;
    pa_sample_spec sample_spec;
    pa_channel_map channel_map;
} pa_source_info;

typedef void (*pa_source_info_cb_t)(void *c, const pa_source_info *i, int eol, void *userdata);

pa_operation *pa_context_get_source_info_list(pa_context *c, pa_source_info_cb_t cb, void *userdata) {
    STUB_LOG("pa_context_get_source_info_list");
    if (cb) cb(c, NULL, 1, userdata);
    return &g_done_op;
}


typedef struct pa_server_info {
    const char *user_name;
    const char *host_name;
    const char *server_version;
    const char *server_name;
    pa_sample_spec sample_spec;
    const char *default_sink_name;
    const char *default_source_name;
    unsigned int cookie;
    pa_channel_map channel_map;
} pa_server_info;

typedef void (*pa_server_info_cb_t)(void *c, const pa_server_info *i, void *userdata);

pa_operation *pa_context_get_server_info(pa_context *c, pa_server_info_cb_t cb, void *userdata) {
    STUB_LOG("pa_context_get_server_info");
    if (cb) {
        pa_server_info info;
        memset(&info, 0, sizeof(info));
        info.user_name = "user";
        info.host_name = "polydroid";
        info.server_version = "0.0.1";
        info.server_name = "polydroid-stub";
        info.sample_spec.format = PA_SAMPLE_S16LE;
        info.sample_spec.rate = 48000;
        info.sample_spec.channels = 2;
        info.default_sink_name = "polydroid_audio";
        info.default_source_name = "polydroid_audio.monitor";
        info.channel_map.channels = 2;
        cb(c, &info, userdata);
    }
    return &g_done_op;
}


typedef struct pa_stream { int dummy; } pa_stream;
typedef enum pa_stream_state {
    PA_STREAM_UNCONNECTED = 0,
    PA_STREAM_CREATING,
    PA_STREAM_READY,
    PA_STREAM_FAILED,
    PA_STREAM_TERMINATED
} pa_stream_state_t;

pa_stream *pa_stream_new(pa_context *c, const char *name, const pa_sample_spec *ss, const pa_channel_map *map) {
    STUB_LOG("pa_stream_new(name=%s)", name ? name : "NULL");
    (void)c; (void)ss; (void)map;
    return calloc(1, sizeof(pa_stream));
}
pa_stream *pa_stream_new_with_proplist(pa_context *c, const char *name, const pa_sample_spec *ss, const pa_channel_map *map, void *p) {
    (void)p;
    return pa_stream_new(c, name, ss, map);
}
void pa_stream_unref(pa_stream *s) { free(s); }
pa_stream *pa_stream_ref(pa_stream *s) { return s; }
pa_stream_state_t pa_stream_get_state(pa_stream *s) { (void)s; return PA_STREAM_READY; }
int pa_stream_connect_playback(pa_stream *s, const char *dev, const pa_buffer_attr *attr, int flags, void *volume, void *sync) {
    STUB_LOG("pa_stream_connect_playback");
    (void)s; (void)dev; (void)attr; (void)flags; (void)volume; (void)sync;
    return 0;
}
int pa_stream_connect_record(pa_stream *s, const char *dev, const pa_buffer_attr *attr, int flags) {
    (void)s; (void)dev; (void)attr; (void)flags;
    return 0;
}
int pa_stream_disconnect(pa_stream *s) { (void)s; return 0; }
int pa_stream_write(pa_stream *s, const void *data, size_t nbytes, void (*free_cb)(void*), long long offset, int seek) {
    (void)s; (void)data; (void)nbytes; (void)free_cb; (void)offset; (void)seek;
    return 0;
}
size_t pa_stream_writable_size(pa_stream *s) { (void)s; return 4096; }
size_t pa_stream_readable_size(pa_stream *s) { (void)s; return 0; }
int pa_stream_peek(pa_stream *s, const void **data, size_t *nbytes) {
    (void)s;
    if (data) *data = NULL;
    if (nbytes) *nbytes = 0;
    return 0;
}
int pa_stream_drop(pa_stream *s) { (void)s; return 0; }
pa_operation *pa_stream_drain(pa_stream *s, pa_context_success_cb_t cb, void *userdata) {
    (void)s;
    if (cb) cb(s, 1, userdata);
    return &g_done_op;
}
pa_operation *pa_stream_flush(pa_stream *s, pa_context_success_cb_t cb, void *userdata) {
    (void)s;
    if (cb) cb(s, 1, userdata);
    return &g_done_op;
}
pa_operation *pa_stream_cork(pa_stream *s, int b, pa_context_success_cb_t cb, void *userdata) {
    (void)s; (void)b;
    if (cb) cb(s, 1, userdata);
    return &g_done_op;
}
void pa_stream_set_state_callback(pa_stream *s, void *cb, void *userdata) { (void)s; (void)cb; (void)userdata; }
void pa_stream_set_write_callback(pa_stream *s, void *cb, void *userdata) { (void)s; (void)cb; (void)userdata; }
void pa_stream_set_read_callback(pa_stream *s, void *cb, void *userdata) { (void)s; (void)cb; (void)userdata; }
void pa_stream_set_overflow_callback(pa_stream *s, void *cb, void *userdata) { (void)s; (void)cb; (void)userdata; }
void pa_stream_set_underflow_callback(pa_stream *s, void *cb, void *userdata) { (void)s; (void)cb; (void)userdata; }
const pa_sample_spec *pa_stream_get_sample_spec(pa_stream *s) { (void)s; return NULL; }
const pa_channel_map *pa_stream_get_channel_map(pa_stream *s) { (void)s; return NULL; }
const pa_buffer_attr *pa_stream_get_buffer_attr(pa_stream *s) { (void)s; return NULL; }
pa_operation *pa_stream_update_timing_info(pa_stream *s, void *cb, void *ud) { (void)s; (void)cb; (void)ud; return &g_done_op; }
int pa_stream_get_time(pa_stream *s, pa_usec_t *r_usec) { (void)s; if (r_usec) *r_usec = 0; return 0; }
int pa_stream_get_latency(pa_stream *s, pa_usec_t *r_usec, int *negative) {
    (void)s;
    if (r_usec) *r_usec = 20000;
    if (negative) *negative = 0;
    return 0;
}
int pa_stream_begin_write(pa_stream *s, void **data, size_t *nbytes) {
    (void)s;
    static char discard_buf[8192];
    if (data) *data = discard_buf;
    if (nbytes && *nbytes > sizeof(discard_buf)) *nbytes = sizeof(discard_buf);
    return 0;
}
int pa_stream_cancel_write(pa_stream *s) { (void)s; return 0; }
unsigned int pa_stream_get_index(pa_stream *s) { (void)s; return 0; }
int pa_stream_is_corked(pa_stream *s) { (void)s; return 0; }


void *pa_xmalloc(size_t l) { return malloc(l); }
void *pa_xmalloc0(size_t l) { return calloc(1, l); }
void pa_xfree(void *p) { free(p); }
void *pa_xrealloc(void *ptr, size_t size) { return realloc(ptr, size); }

pa_channel_map *pa_channel_map_init_auto(pa_channel_map *m, unsigned channels, int def) {
    if (m) { m->channels = channels; }
    return m;
}
pa_channel_map *pa_channel_map_init_extend(pa_channel_map *m, unsigned channels, int def) {
    return pa_channel_map_init_auto(m, channels, def);
}
int pa_channel_map_valid(const pa_channel_map *m) { return m && m->channels > 0; }

size_t pa_frame_size(const pa_sample_spec *ss) {
    if (!ss) return 4;
    int bps = 2;
    switch (ss->format) {
        case PA_SAMPLE_U8: bps = 1; break;
        case PA_SAMPLE_S16LE: case PA_SAMPLE_S16BE: bps = 2; break;
        case PA_SAMPLE_FLOAT32LE: case PA_SAMPLE_FLOAT32BE: bps = 4; break;
        case PA_SAMPLE_S32LE: case PA_SAMPLE_S32BE: bps = 4; break;
        case PA_SAMPLE_S24LE: case PA_SAMPLE_S24BE: bps = 3; break;
        case PA_SAMPLE_S24_32LE: case PA_SAMPLE_S24_32BE: bps = 4; break;
        default: bps = 2; break;
    }
    return bps * ss->channels;
}
size_t pa_sample_size(const pa_sample_spec *ss) {
    return pa_frame_size(ss) / (ss ? ss->channels : 1);
}
int pa_sample_spec_valid(const pa_sample_spec *ss) {
    return ss && ss->rate > 0 && ss->channels > 0;
}
size_t pa_bytes_per_second(const pa_sample_spec *ss) {
    return ss ? pa_frame_size(ss) * ss->rate : 0;
}
pa_usec_t pa_bytes_to_usec(unsigned long long bytes, const pa_sample_spec *ss) {
    size_t bps = pa_bytes_per_second(ss);
    return bps ? (bytes * 1000000ULL) / bps : 0;
}
pa_usec_t pa_usec_to_bytes(unsigned long long usec, const pa_sample_spec *ss) {
    return (pa_bytes_per_second(ss) * usec) / 1000000ULL;
}

typedef struct pa_proplist { int dummy; } pa_proplist;
pa_proplist *pa_proplist_new(void) { return calloc(1, sizeof(pa_proplist)); }
void pa_proplist_free(pa_proplist *p) { free(p); }
int pa_proplist_sets(pa_proplist *p, const char *key, const char *value) { (void)p; (void)key; (void)value; return 0; }
const char *pa_proplist_gets(pa_proplist *p, const char *key) { (void)p; (void)key; return NULL; }
int pa_proplist_set(pa_proplist *p, const char *key, const void *data, size_t nbytes) { (void)p; (void)key; (void)data; (void)nbytes; return 0; }

pa_cvolume *pa_cvolume_init(pa_cvolume *a) {
    if (a) { memset(a, 0, sizeof(*a)); }
    return a;
}
pa_cvolume *pa_cvolume_set(pa_cvolume *a, unsigned channels, pa_volume_t v) {
    if (a) {
        a->channels = channels;
        for (unsigned i = 0; i < channels && i < 32; i++) a->values[i] = v;
    }
    return a;
}
int pa_cvolume_valid(const pa_cvolume *a) { return a && a->channels > 0; }
pa_volume_t pa_cvolume_avg(const pa_cvolume *a) { return a && a->channels ? a->values[0] : PA_VOLUME_NORM; }
pa_volume_t pa_cvolume_max(const pa_cvolume *a) { return pa_cvolume_avg(a); }
pa_volume_t pa_sw_volume_from_linear(double v) { return (pa_volume_t)(v * PA_VOLUME_NORM); }
double pa_sw_volume_to_linear(pa_volume_t v) { return (double)v / PA_VOLUME_NORM; }
pa_volume_t pa_sw_volume_from_dB(double v) { (void)v; return PA_VOLUME_NORM; }
double pa_sw_volume_to_dB(pa_volume_t v) { (void)v; return 0.0; }

typedef void (*pa_context_subscribe_cb_t)(void *c, int t, unsigned int idx, void *userdata);
pa_operation *pa_context_subscribe(pa_context *c, unsigned int mask, pa_context_success_cb_t cb, void *userdata) {
    (void)c; (void)mask;
    if (cb) cb(c, 1, userdata);
    return &g_done_op;
}
void pa_context_set_subscribe_callback(pa_context *c, pa_context_subscribe_cb_t cb, void *userdata) {
    (void)c; (void)cb; (void)userdata;
}
