
/*
 * patch UnityPlayer.so in order to stop Unity crashing from... honestly i dont know
 * stderr goes to Player.log, not logcat!!!
 */

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

typedef struct {
    uint8_t pattern[7];
    int src_reg;
} patch_pattern_t;

static const patch_pattern_t patterns[] = {
    { {0x49, 0x8B, 0x7E, 0x18, 0x48, 0x85, 0xFF}, 14 },
    { {0x49, 0x8B, 0x7F, 0x18, 0x48, 0x85, 0xFF}, 15 },
    { {0x49, 0x8B, 0x7E, 0x28, 0x48, 0x85, 0xFF}, 14 },
    { {0x49, 0x8B, 0x7F, 0x28, 0x48, 0x85, 0xFF}, 15 },
    { {0x49, 0x8B, 0x7E, 0x08, 0x48, 0x85, 0xFF}, 14 },
    { {0x49, 0x8B, 0x7F, 0x08, 0x48, 0x85, 0xFF}, 15 },
    { {0x48, 0x8B, 0x3C, 0xD1, 0x48, 0x89, 0x3E},  0 },
    { {0x4D, 0x8B, 0x6D, 0x00, 0x4D, 0x85, 0xED}, 13 },
    { {0x4C, 0x8B, 0x3B, 0x48, 0x8B, 0x7B, 0x28}, 215 },
    { {0x4C, 0x8B, 0x2B, 0x48, 0x8B, 0x7B, 0x28}, 213 },
    { {0x4C, 0x8B, 0x23, 0x48, 0x8B, 0x7B, 0x28}, 212 },
};
#define NUM_PATTERNS (sizeof(patterns) / sizeof(patterns[0]))

static volatile int g_patched = 0;

static void *find_nearby_gap(uintptr_t target) {
    uintptr_t lo = (target > 0x7FFFFFFFUL) ? (target - 0x7FFFFFFFUL) : 0;
    uintptr_t hi = target + 0x7FFFFFFFUL;
    lo = (lo + 0xFFF) & ~0xFFFUL;
    hi &= ~0xFFFUL;

    FILE *f = fopen("/proc/self/maps", "r");
    if (!f) return NULL;

    typedef struct { uintptr_t s, e; } region_t;
    region_t regions[8192];
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

static int build_trampoline(uint8_t *tramp, uintptr_t return_addr,
                            const uint8_t *orig_bytes, int src_reg) {
    uint8_t *p = tramp;

    if (src_reg == 13) {
        memcpy(p, orig_bytes, 4); p += 4;
        *p++ = 0x50;
        *p++ = 0x4C; *p++ = 0x89; *p++ = 0xE8;
        *p++ = 0x48; *p++ = 0xC1; *p++ = 0xE8; *p++ = 0x38;
        *p++ = 0x3C; *p++ = 0x80;
        *p++ = 0x58;
        *p++ = 0x73; *p++ = 0x03;
        *p++ = 0x45; *p++ = 0x31; *p++ = 0xED;
        memcpy(p, orig_bytes + 4, 3); p += 3;
        *p++ = 0xE9;
        int32_t r1 = (int32_t)(return_addr - ((uintptr_t)p + 4));
        memcpy(p, &r1, 4); p += 4;
        return (int)(p - tramp);
    }


    if (src_reg == 215 || src_reg == 213 || src_reg == 212) {
        uint8_t mov_rax_src, xor_modrm;
        if (src_reg == 215)      { mov_rax_src = 0xF8; xor_modrm = 0xFF; }
        else if (src_reg == 213) { mov_rax_src = 0xE8; xor_modrm = 0xED; }
        else                     { mov_rax_src = 0xE0; xor_modrm = 0xE4; }


        *p++ = 0x50;
        *p++ = 0x48; *p++ = 0x89; *p++ = 0xD8;
        *p++ = 0x48; *p++ = 0xC1; *p++ = 0xE8; *p++ = 0x38;
        *p++ = 0x3C; *p++ = 0xB4;
        *p++ = 0x58;
        *p++ = 0x74; *p++ = 0x0A;
        *p++ = 0x45; *p++ = 0x31; *p++ = xor_modrm;
        *p++ = 0x31; *p++ = 0xFF;
        *p++ = 0xE9;
        int32_t r_bad = (int32_t)(return_addr - ((uintptr_t)p + 4));
        memcpy(p, &r_bad, 4); p += 4;
        memcpy(p, orig_bytes, 3); p += 3;
        *p++ = 0x50;
        *p++ = 0x4C; *p++ = 0x89; *p++ = mov_rax_src;
        *p++ = 0x48; *p++ = 0xC1; *p++ = 0xE8; *p++ = 0x38;
        *p++ = 0x3C; *p++ = 0xB4;
        *p++ = 0x58;
        *p++ = 0x74; *p++ = 0x03;
        *p++ = 0x45; *p++ = 0x31; *p++ = xor_modrm;
        memcpy(p, orig_bytes + 3, 4); p += 4;
        *p++ = 0xE9;
        int32_t r1 = (int32_t)(return_addr - ((uintptr_t)p + 4));
        memcpy(p, &r1, 4); p += 4;
        return (int)(p - tramp);
    }

    if (src_reg == 0) {
        *p++ = 0x50;
        *p++ = 0x48; *p++ = 0x89; *p++ = 0xF0;
        *p++ = 0x48; *p++ = 0xC1; *p++ = 0xE8; *p++ = 0x38;
        *p++ = 0x3C; *p++ = 0x80;
        *p++ = 0x58;
        *p++ = 0x72;
        *p++ = 12;
        memcpy(p, orig_bytes, 4); p += 4;
        memcpy(p, orig_bytes + 4, 3); p += 3;
        *p++ = 0xE9;
        int32_t r1 = (int32_t)(return_addr - ((uintptr_t)p + 4));
        memcpy(p, &r1, 4); p += 4;
        *p++ = 0x31; *p++ = 0xFF;
        *p++ = 0xE9;
        int32_t r2 = (int32_t)(return_addr - ((uintptr_t)p + 4));
        memcpy(p, &r2, 4); p += 4;

        return (int)(p - tramp);
    }

    *p++ = 0x50;
    if (src_reg == 14) {
        *p++ = 0x4C; *p++ = 0x89; *p++ = 0xF0;
    } else {
        *p++ = 0x4C; *p++ = 0x89; *p++ = 0xF8;
    }
    *p++ = 0x48; *p++ = 0xC1; *p++ = 0xE8; *p++ = 0x38;
    *p++ = 0x3C; *p++ = 0x80;
    *p++ = 0x58;
    *p++ = 0x72;
    *p++ = 12;
    memcpy(p, orig_bytes, 4); p += 4;
    memcpy(p, orig_bytes + 4, 3); p += 3;
    *p++ = 0xE9;
    int32_t r1 = (int32_t)(return_addr - ((uintptr_t)p + 4));
    memcpy(p, &r1, 4); p += 4;
    *p++ = 0x31;
    *p++ = 0xFF;
    *p++ = 0xE9;
    int32_t r2 = (int32_t)(return_addr - ((uintptr_t)p + 4));
    memcpy(p, &r2, 4); p += 4;

    return (int)(p - tramp);
}

// scan function at +c194d0 for all matches and patch every single one
static int apply_patches(uintptr_t base) {
    fprintf(stderr, "unity_patch: UnityPlayer.so base=%p\n", (void*)base);
    typedef struct { uintptr_t s, e; } scan_range_t;
    scan_range_t ranges[] = {
        { base + 0xc18400, base + 0xc19830 },
        { base + 0xc25800, base + 0xc27000 },
    };
    int num_ranges = sizeof(ranges) / sizeof(ranges[0]);

    int match_count = 0;
    typedef struct { uintptr_t addr; int pat_idx; } match_t;
    match_t matches[64];

    for (int ri = 0; ri < num_ranges; ri++) {
        uintptr_t scan_start = ranges[ri].s, scan_end = ranges[ri].e;
        fprintf(stderr, "unity_patch: scanning %p-%p for matches...\n",
                (void*)scan_start, (void*)scan_end);
        for (uintptr_t addr = scan_start; addr + 7 <= scan_end; addr++) {
            for (int pi = 0; pi < (int)NUM_PATTERNS && match_count < 64; pi++) {
                if (memcmp((void*)addr, patterns[pi].pattern, 7) == 0) {
                    matches[match_count].addr = addr;
                    matches[match_count].pat_idx = pi;
                    match_count++;
                    fprintf(stderr, "unity_patch: match: +0x%lx pattern %d (r%d)\n",
                            (unsigned long)(addr - base), pi, patterns[pi].src_reg);
                    addr += 6;
                    break;
                }
            }
        }
    }

    if (match_count == 0) {
        fprintf(stderr, "unity_patch: no matches found!\n");
        return 0;
    }
    fprintf(stderr, "unity_patch: found %d matches, allocating trampoline page...\n",
            match_count);
    uint8_t *tramp_page = find_nearby_gap(ranges[0].s);
    if (!tramp_page) {
        fprintf(stderr, "unity_patch: error! could not allocate trampoline page\n");
        return 0;
    }
    int64_t page_dist = (int64_t)((uintptr_t)tramp_page - ranges[0].s);
    fprintf(stderr, "unity_patch: trampoline page at %p (dist=%ld)\n",
            tramp_page, (long)page_dist);
    uint8_t *tramp_ptr = tramp_page;
    int patched = 0;

    for (int i = 0; i < match_count; i++) {
        uintptr_t patch_addr = matches[i].addr;
        uintptr_t return_addr = patch_addr + 7;
        int pi = matches[i].pat_idx;
        int64_t dist = (int64_t)((uintptr_t)tramp_ptr - patch_addr);
        if (dist > 0x7FFFFFFFL || dist < -0x7FFFFFFFL) {
            fprintf(stderr, "unity_patch: skip +0x%lx! trampoline out of reach\n",
                    (unsigned long)(patch_addr - base));
            continue;
        }
        int tramp_size = build_trampoline(tramp_ptr, return_addr,
                                          patterns[pi].pattern, patterns[pi].src_reg);
        uintptr_t page = patch_addr & ~0xFFFUL;
        if (mprotect((void*)page, 0x2000, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
            fprintf(stderr, "unity_patch: error! mprotect %p failed: %s\n",
                    (void*)page, strerror(errno));
            continue;
        }
        uint8_t *dst = (uint8_t *)patch_addr;
        dst[0] = 0xE9;
        int32_t rel = (int32_t)((uintptr_t)tramp_ptr - (patch_addr + 5));
        memcpy(dst + 1, &rel, 4);
        dst[5] = 0x90;
        dst[6] = 0x90;

        mprotect((void*)page, 0x2000, PROT_READ | PROT_EXEC);

        fprintf(stderr, "unity_patch: patched +0x%lx → trampoline %p (r%d, +0x%02x)\n",
                (unsigned long)(patch_addr - base), tramp_ptr,
                patterns[pi].src_reg, patterns[pi].pattern[3]);

        tramp_ptr += tramp_size;
        patched++;
    }

    uintptr_t free_slot = base + 0x26cbb20;
    uintptr_t real_free = *(uintptr_t *)free_slot;
    if (real_free && tramp_ptr + 32 <= tramp_page + 0x1000) {
        uint8_t *t = tramp_ptr;
        *t++ = 0x50;
        *t++ = 0x48; *t++ = 0x89; *t++ = 0xF8;
        *t++ = 0x48; *t++ = 0xC1; *t++ = 0xE8; *t++ = 0x38;
        *t++ = 0x3C; *t++ = 0xB4;
        *t++ = 0x58;
        *t++ = 0x75; *t++ = 0x0E;
        *t++ = 0xFF; *t++ = 0x25; *t++ = 0x00; *t++ = 0x00;
        *t++ = 0x00; *t++ = 0x00;
        memcpy(t, &real_free, 8); t += 8;
        *t++ = 0xC3;

        uintptr_t slot_page = free_slot & ~0xFFFUL;
        if (mprotect((void *)slot_page, 0x1000, PROT_READ | PROT_WRITE) == 0) {
            *(uintptr_t *)free_slot = (uintptr_t)tramp_ptr;fprintf(stderr, "unity_patch: free GOT %p: %p -> %p (safe wrapper)\n",
                    (void *)free_slot, (void *)real_free, tramp_ptr);
            tramp_ptr += (t - tramp_ptr);
            patched++;
        } else {
            fprintf(stderr, "unity_patch: free GOT mprotect failed: %s\n", strerror(errno));
        }
    } else {
        fprintf(stderr, "unity_patch: free GOT skip (real_free=%p, space=%ld)\n",
                (void *)real_free, (long)(tramp_page + 0x1000 - tramp_ptr));
    }


    {
        uintptr_t qsort_call = base + 0xc193f1;
        uint8_t *q = (uint8_t *)qsort_call;
        if (q[0] == 0xE8) {
            uintptr_t qpage = qsort_call & ~0xFFFUL;
            if (mprotect((void *)qpage, 0x1000, PROT_READ | PROT_WRITE | PROT_EXEC) == 0) {
                // 5-byte NOP: 0F 1F 44 00 00
                q[0] = 0x0F; q[1] = 0x1F; q[2] = 0x44; q[3] = 0x00; q[4] = 0x00;
                mprotect((void *)qpage, 0x1000, PROT_READ | PROT_EXEC);
                fprintf(stderr, "unity_patch: NOP'd qsort call at +0xc193f1\n");
                patched++;
            }
        }
    }

    g_patched = patched;
    fprintf(stderr, "unity_patch: applied %d/%d patches\n", patched, match_count);
    return patched > 0;
}

// scan /proc/self/maps for UnityPlayer.so
static int try_maps_scan(void) {
    FILE *f = fopen("/proc/self/maps", "r");
    if (!f) return 0;

    char line[512];
    while (fgets(line, sizeof(line), f)) {
        if (!strstr(line, "UnityPlayer")) continue;

        uintptr_t start = 0;
        sscanf(line, "%lx-", &start);
        if (start == 0) continue;
        fclose(f);
        uint8_t *ehdr = (uint8_t*)start;
        if (ehdr[0] != 0x7f || ehdr[1] != 'E' || ehdr[2] != 'L' || ehdr[3] != 'F') {
            return apply_patches(start);
        }

        uint64_t e_phoff = *(uint64_t*)(ehdr + 32);
        uint16_t e_phentsize = *(uint16_t*)(ehdr + 54);
        uint16_t e_phnum = *(uint16_t*)(ehdr + 56);

        uintptr_t first_load_vaddr = 0;
        for (int i = 0; i < e_phnum; i++) {
            uint8_t *ph = ehdr + e_phoff + i * e_phentsize;
            uint32_t p_type = *(uint32_t*)ph;
            if (p_type == 1) {
                first_load_vaddr = *(uint64_t*)(ph + 16);
                break;
            }
        }

        uintptr_t base = start - first_load_vaddr;
        fprintf(stderr, "unity_patch: ELF base=%p (first_load=0x%lx)\n",
                (void*)base, (unsigned long)first_load_vaddr);
        return apply_patches(base);
    }
    fclose(f);
    return 0;
}

static void *poll_thread(void *arg) {
    (void)arg;
    for (int i = 0; i < 120 && !g_patched; i++) {
        usleep(500000);
        if (try_maps_scan()) break;
    }
    if (!g_patched)
        fprintf(stderr, "unity_patch: WARN: gave up waiting\n");
    return NULL;
}

__attribute__((constructor))
static void unity_crash_fix_init(void) {
    fprintf(stderr, "unity_patch: loaded (pid=%d)\n", getpid());

    if (try_maps_scan()) return;
    pthread_t tid;
    if (pthread_create(&tid, NULL, poll_thread, NULL) == 0)
        pthread_detach(tid);
}
