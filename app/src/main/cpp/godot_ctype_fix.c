#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <sys/mman.h>
#include <unistd.h>
#include <pthread.h>
#include <errno.h>

#ifndef MAP_FIXED_NOREPLACE
#define MAP_FIXED_NOREPLACE 0x100000
#endif

static unsigned short g_b_table[384];
static const unsigned short *g_b_ptr;

static void build_ctype_table(void) {
    for (int i = 0; i < 256; i++) {
        unsigned short f = 0;
        if (i < 32 || i == 127) f |= 0x0002;
        if (i >= 'A' && i <= 'Z') f |= 0x0100 | 0x0400 | 0x0008;
        if (i >= 'a' && i <= 'z') f |= 0x0200 | 0x0400 | 0x0008;
        if (i >= '0' && i <= '9') f |= 0x0800 | 0x1000 | 0x0008;
        if ((i >= 'A' && i <= 'F') || (i >= 'a' && i <= 'f')) f |= 0x1000;
        if (i==' '||i=='\t'||i=='\n'||i=='\v'||i=='\f'||i=='\r') f |= 0x2000;
        if (i == ' ' || i == '\t') f |= 0x0001;
        if (i > 32 && i < 127)  f |= 0x8000;
        if (i >= 32 && i < 127) f |= 0x4000;
        if ((f & 0x4000) && !(f & (0x0008 | 0x2000))) f |= 0x0004;
        g_b_table[128 + i] = f;
    }
    g_b_ptr = g_b_table + 128;
}

typedef struct {
    uintptr_t addr;
    int is_hi;
    int is_jnz;
    uint8_t mask;
    int8_t disp8;
    int orig_len;
} site_t;

#define MAX_SITES 128
static site_t g_sites[MAX_SITES];
static int g_nsites = 0;
static volatile int g_patched = 0;

static int is_ctype_mask(uint8_t m, int is_hi) {
    if (is_hi) {
        switch (m) {
            case 0x01: case 0x02: case 0x04: case 0x08:
            case 0x10: case 0x20: case 0x40: case 0x80: return 1;
        }
    } else {
        switch (m) {
            case 0x01: case 0x02: case 0x04: case 0x08: return 1;
        }
    }
    return 0;
}

static void scan_region(uintptr_t s, uintptr_t e) {
    uintptr_t a = s;
    while (a + 7 <= e && g_nsites < MAX_SITES) {
        const uint8_t *p = (const uint8_t *)a;
        int test_len = 0; int is_hi = 0; uint8_t mask = 0;
        if (p[0]==0xF6 && p[1]==0x44 && p[2]==0x42 && p[3]==0x01) {
            test_len = 5; mask = p[4]; is_hi = 1;
        } else if (p[0]==0xF6 && p[1]==0x04 && p[2]==0x42) {
            test_len = 4; mask = p[3]; is_hi = 0;
        } else { a++; continue; }

        if (!is_ctype_mask(mask, is_hi)) { a++; continue; }
        uint8_t br = p[test_len];
        if (br != 0x74 && br != 0x75) { a++; continue; }

        site_t *st = &g_sites[g_nsites++];
        st->addr = a;
        st->is_hi = is_hi;
        st->is_jnz = (br == 0x75);
        st->mask = mask;
        st->disp8 = (int8_t)p[test_len + 1];
        st->orig_len = test_len + 2;
        a += st->orig_len;
    }
}

static void *find_nearby_gap(uintptr_t target) {
    uintptr_t lo = (target > 0x7FFFFFFFUL) ? (target - 0x7FFFFFFFUL) : 0;
    uintptr_t hi = target + 0x7FFFFFFFUL;
    lo = (lo + 0xFFF) & ~0xFFFUL;
    hi &= ~0xFFFUL;

    FILE *f = fopen("/proc/self/maps", "r");
    if (!f) return NULL;

    typedef struct { uintptr_t s, e; } region_t;
    static region_t regions[8192];
    int n = 0;
    char line[512];
    while (fgets(line, sizeof(line), f) && n < 8192) {
        uintptr_t s, e;
        if (sscanf(line, "%lx-%lx", &s, &e) != 2) continue;
        if (e <= lo || s >= hi) continue;
        regions[n].s = s;
        regions[n].e = e;
        n++;
    }
    fclose(f);

    for (int i = 0; i < n - 1; i++)
        for (int j = i + 1; j < n; j++)
            if (regions[j].s < regions[i].s) {
                region_t t = regions[i]; regions[i] = regions[j]; regions[j] = t;
            }

    uintptr_t best = 0;
    int64_t best_dist = INT64_MAX;
    for (int i = 0; i < n - 1; i++) {
        uintptr_t gap_s = (regions[i].e + 0xFFF) & ~0xFFFUL;
        uintptr_t gap_e = regions[i + 1].s;
        if (gap_s >= gap_e || gap_e - gap_s < 0x1000) continue;
        if (gap_s < lo) gap_s = lo;
        if (gap_e > hi) gap_e = hi;
        if (gap_e - gap_s < 0x1000) continue;
        int64_t d = (int64_t)gap_s - (int64_t)target;
        if (d < 0) d = -d;
        if (d < best_dist) { best = gap_s; best_dist = d; }
    }
    if (!best) return NULL;

    void *p = mmap((void*)best, 0x1000, PROT_READ | PROT_WRITE | PROT_EXEC,
                   MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED_NOREPLACE, -1, 0);
    if (p == MAP_FAILED)
        p = mmap((void*)best, 0x1000, PROT_READ | PROT_WRITE | PROT_EXEC,
                 MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED, -1, 0);
    return (p == MAP_FAILED) ? NULL : p;
}

static int build_trampoline(uint8_t *t, const site_t *s) {
    uint8_t *p = t;
    *p++ = 0x48; *p++ = 0xBA;
    uint64_t tbl = (uint64_t)g_b_ptr;
    memcpy(p, &tbl, 8); p += 8;
    if (s->is_hi) {
        *p++ = 0xF6; *p++ = 0x44; *p++ = 0x42; *p++ = 0x01; *p++ = s->mask;
    } else {
        *p++ = 0xF6; *p++ = 0x04; *p++ = 0x42; *p++ = s->mask;
    }
    *p++ = 0x74; *p++ = 0x05;

    uintptr_t after = s->addr + s->orig_len;
    uintptr_t target = after + s->disp8;
    uintptr_t is_true  = s->is_jnz ? target : after;
    uintptr_t is_false = s->is_jnz ? after  : target;

    *p++ = 0xE9;
    int32_t r1 = (int32_t)(is_true - ((uintptr_t)p + 4));
    memcpy(p, &r1, 4); p += 4;
    *p++ = 0xE9;
    int32_t r2 = (int32_t)(is_false - ((uintptr_t)p + 4));
    memcpy(p, &r2, 4); p += 4;
    return (int)(p - t);
}

static int patch_all(void) {
    if (g_nsites == 0) return 0;
    uint8_t *tramp_page = find_nearby_gap(g_sites[0].addr);
    if (!tramp_page) {
        fprintf(stderr, "godot_ctype_patch: could not allocate trampoline page\n");
        return 0;
    }
    uint8_t *tp = tramp_page;
    uint8_t *tp_end = tramp_page + 0x1000;
    int patched = 0;

    for (int i = 0; i < g_nsites; i++) {
        site_t *s = &g_sites[i];
        int64_t d = (int64_t)((uintptr_t)tp - s->addr);
        if (d < 0) d = -d;
        if (d > 0x7FFFFFFFL) continue;
        if (tp_end - tp < 32) break;
        int n = build_trampoline(tp, s);

        uintptr_t page = s->addr & ~0xFFFUL;
        if (mprotect((void*)page, 0x2000, PROT_READ|PROT_WRITE|PROT_EXEC) != 0) {
            fprintf(stderr, "godot_ctype_patch: mprotect rw failed @ %p: %s\n",
                    (void*)page, strerror(errno));
            continue;
        }
        uint8_t *dst = (uint8_t*)s->addr;
        dst[0] = 0xE9;
        int32_t rel = (int32_t)((uintptr_t)tp - (s->addr + 5));
        memcpy(dst + 1, &rel, 4);
        for (int k = 5; k < s->orig_len; k++) dst[k] = 0x90;
        mprotect((void*)page, 0x2000, PROT_READ|PROT_EXEC);
        tp += n;
        patched++;
    }
    fprintf(stderr, "godot_ctype_patch: %d/%d sites patched\n", patched, g_nsites);
    return patched > 0;
}

static int try_maps_scan(void) {
    FILE *f = fopen("/proc/self/maps", "r");
    if (!f) return 0;
    char line[1024];
    g_nsites = 0;
    while (fgets(line, sizeof(line), f)) {
        if (!strstr(line, "Polytoria Client.x86_64")) continue;
        uintptr_t s = 0, e = 0;
        if (sscanf(line, "%lx-%lx", &s, &e) != 2) continue;
        char perms[8] = {0};
        sscanf(line, "%*x-%*x %7s", perms);
        if (!strchr(perms, 'x')) continue;
        scan_region(s, e);
    }
    fclose(f);
    if (g_nsites == 0) return 0;
    return patch_all();
}

static void *poll_thread(void *arg) {
    (void)arg;
    for (int i = 0; i < 240 && !g_patched; i++) {
        usleep(250000);
        if (try_maps_scan()) { g_patched = 1; break; }
    }
    if (!g_patched)
        fprintf(stderr, "godot_ctype_patch: WARN: gave up waiting for binary\n");
    return NULL;
}

__attribute__((constructor))
static void godot_ctype_patch_init(void) {
    build_ctype_table();
    if (try_maps_scan()) { g_patched = 1; return; }
    pthread_t tid;
    if (pthread_create(&tid, NULL, poll_thread, NULL) == 0)
        pthread_detach(tid);
}
