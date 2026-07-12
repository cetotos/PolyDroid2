#define _GNU_SOURCE
#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>
#include <pthread.h>
#include <sched.h>
#include <sys/resource.h>
#include <sys/syscall.h>
#include <unistd.h>

static __thread int want_rt = 0;

int pthread_attr_setschedpolicy(pthread_attr_t *attr, int policy) {
    static int (*real)(pthread_attr_t*, int) = NULL;
    if (!real) real = dlsym(RTLD_NEXT, "pthread_attr_setschedpolicy");
    if (policy == SCHED_FIFO || policy == SCHED_RR) want_rt = 1;
    return real ? real(attr, SCHED_OTHER) : 0;
}

int pthread_attr_setschedparam(pthread_attr_t *attr, const struct sched_param *param) {
    static int (*real)(pthread_attr_t*, const struct sched_param*) = NULL;
    if (!real) real = dlsym(RTLD_NEXT, "pthread_attr_setschedparam");
    struct sched_param zero;
    memset(&zero, 0, sizeof(zero));
    (void)param;
    return real ? real(attr, &zero) : 0;
}

struct wrap {
    void *(*start)(void *);
    void *arg;
    int rt;
};

static void *wrap_start(void *p) {
    struct wrap *w = (struct wrap *)p;
    void *(*start)(void *) = w->start;
    void *arg = w->arg;
    int rt = w->rt;
    free(w);
    if (rt) {
        setpriority(PRIO_PROCESS, (id_t)syscall(SYS_gettid), -16);
    }
    return start(arg);
}

int pthread_create(pthread_t *thread, const pthread_attr_t *attr,
                   void *(*start)(void *), void *arg) {
    static int (*real)(pthread_t*, const pthread_attr_t*, void*(*)(void*), void*) = NULL;
    if (!real) real = dlsym(RTLD_NEXT, "pthread_create");

    int rt = want_rt;
    want_rt = 0;

    pthread_attr_t copy;
    int have_copy = 0;
    const pthread_attr_t *use_attr = attr;
    if (attr && pthread_attr_init(&copy) == 0) {
        size_t stksz = 0;
        int dstate = PTHREAD_CREATE_JOINABLE;
        pthread_attr_getstacksize(attr, &stksz);
        pthread_attr_getdetachstate(attr, &dstate);
        if (stksz > 0) pthread_attr_setstacksize(&copy, stksz);
        pthread_attr_setdetachstate(&copy, dstate);
        use_attr = &copy;
        have_copy = 1;
    }

    struct wrap *w = (struct wrap *)malloc(sizeof(*w));
    int r;
    if (w) {
        w->start = start;
        w->arg = arg;
        w->rt = rt;
        r = real(thread, use_attr, wrap_start, w);
        if (r != 0) free(w);
    } else {
        r = real(thread, use_attr, start, arg);
    }
    if (have_copy) pthread_attr_destroy(&copy);
    return r;
}

int sched_setscheduler(pid_t pid, int policy, const struct sched_param *param) {
    (void)pid; (void)policy; (void)param;
    return 0;
}
