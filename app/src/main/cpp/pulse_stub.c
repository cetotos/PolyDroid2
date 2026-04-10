/*
 * libpulse-simple.so.0 will link against this, which is why it's needed
 * everything happens in pulse_simple_stub.c, not here
 */

#include <stddef.h>
#include <stdlib.h>

const char *pa_strerror(int error) {
    return "stub: no PulseAudio";
}


void *pa_xmalloc(size_t l) { return malloc(l); }
void *pa_xmalloc0(size_t l) { return calloc(1, l); }
void pa_xfree(void *p) { free(p); }
void *pa_xrealloc(void *ptr, size_t size) { return realloc(ptr, size); }

typedef struct pa_channel_map { unsigned char channels; int map[32]; } pa_channel_map;
pa_channel_map *pa_channel_map_init_auto(pa_channel_map *m, unsigned channels, int def) {
    if (m) { m->channels = channels; }
    return m;
}
pa_channel_map *pa_channel_map_init_extend(pa_channel_map *m, unsigned channels, int def) {
    return pa_channel_map_init_auto(m, channels, def);
}
int pa_channel_map_valid(const pa_channel_map *m) { return m && m->channels > 0; }


typedef struct pa_sample_spec { int format; unsigned int rate; unsigned char channels; } pa_sample_spec;
size_t pa_frame_size(const pa_sample_spec *ss) {
    if (!ss) return 4;
    int bps = 2;
    switch (ss->format) {
        case 0: bps = 1; break;  // u8
        case 3: case 4: bps = 2; break;  // s16
        case 5: case 6: bps = 4; break;  // float32
        case 7: case 8: bps = 4; break;  // s32
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
unsigned long long pa_bytes_to_usec(unsigned long long bytes, const pa_sample_spec *ss) {
    size_t bps = pa_bytes_per_second(ss);
    return bps ? (bytes * 1000000ULL) / bps : 0;
}
unsigned long long pa_usec_to_bytes(unsigned long long usec, const pa_sample_spec *ss) {
    return (pa_bytes_per_second(ss) * usec) / 1000000ULL;
}

void *pa_mainloop_new(void) { return NULL; }
void pa_mainloop_free(void *m) {}
void *pa_mainloop_get_api(void *m) { return NULL; }
int pa_mainloop_run(void *m, int *retval) { return -1; }

void *pa_context_new(void *api, const char *name) { return NULL; }
void pa_context_unref(void *c) {}
int pa_context_connect(void *c, const char *server, int flags, void *api) { return -1; }
void pa_context_disconnect(void *c) {}
int pa_context_get_state(void *c) { return 0; /* PA_CONTEXT_UNCONNECTED */ }
