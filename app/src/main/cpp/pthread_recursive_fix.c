#define _GNU_SOURCE
#include <pthread.h>
#include <dlfcn.h>
#include <stdio.h>
#include <stddef.h>

#define GLIBC_MUTEX_KIND_OFFSET 16

static inline int is_recursive(pthread_mutex_t *m) {
    if (!m) return 0;
    int kind = *(int *)((char *)m + GLIBC_MUTEX_KIND_OFFSET);
    return (kind & 3) == 1;
}

#define MAX_HELD 64
static __thread struct {
    pthread_mutex_t *m;
    int count;
} g_held[MAX_HELD];
static __thread int g_held_count;

static int (*real_lock)(pthread_mutex_t *);
static int (*real_unlock)(pthread_mutex_t *);
static int (*real_trylock)(pthread_mutex_t *);

static void init_once(void) {
    if (!real_lock) {
        real_lock    = dlsym(RTLD_NEXT, "pthread_mutex_lock");
        real_unlock  = dlsym(RTLD_NEXT, "pthread_mutex_unlock");
        real_trylock = dlsym(RTLD_NEXT, "pthread_mutex_trylock");
    }
}

int pthread_mutex_lock(pthread_mutex_t *m) {
    init_once();
    if (!is_recursive(m)) return real_lock(m);
    for (int i = 0; i < g_held_count; i++) {
        if (g_held[i].m == m) { g_held[i].count++; return 0; }
    }
    int r = real_lock(m);
    if (r == 0 && g_held_count < MAX_HELD) {
        g_held[g_held_count].m = m;
        g_held[g_held_count].count = 1;
        g_held_count++;
    }
    return r;
}

int pthread_mutex_trylock(pthread_mutex_t *m) {
    init_once();
    if (!is_recursive(m)) return real_trylock(m);
    for (int i = 0; i < g_held_count; i++) {
        if (g_held[i].m == m) { g_held[i].count++; return 0; }
    }
    int r = real_trylock(m);
    if (r == 0 && g_held_count < MAX_HELD) {
        g_held[g_held_count].m = m;
        g_held[g_held_count].count = 1;
        g_held_count++;
    }
    return r;
}

int pthread_mutex_unlock(pthread_mutex_t *m) {
    init_once();
    if (!is_recursive(m)) return real_unlock(m);
    for (int i = 0; i < g_held_count; i++) {
        if (g_held[i].m == m) {
            if (--g_held[i].count > 0) return 0;
            g_held[i] = g_held[g_held_count - 1];
            g_held_count--;
            return real_unlock(m);
        }
    }
    return real_unlock(m);
}
