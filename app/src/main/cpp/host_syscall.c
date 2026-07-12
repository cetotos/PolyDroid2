#include <stdarg.h>
#include <stdint.h>
#include <errno.h>
#include <dlfcn.h>
#include <sys/syscall.h>

#define ARM64_SYS_mbind           235
#define ARM64_SYS_get_mempolicy   236
#define ARM64_SYS_set_mempolicy   237
#define ARM64_SYS_migrate_pages   238
#define ARM64_SYS_move_pages      239
#define ARM64_SYS_membarrier      283

typedef long (*real_syscall_t)(long number, ...);

static real_syscall_t real_syscall(void) {
    static real_syscall_t fn = NULL;
    if (!fn) fn = (real_syscall_t)dlsym(RTLD_NEXT, "syscall");
    return fn;
}

long syscall(long number, ...) {
    if (number == ARM64_SYS_get_mempolicy ||
        number == ARM64_SYS_set_mempolicy ||
        number == ARM64_SYS_mbind ||
        number == ARM64_SYS_migrate_pages ||
        number == ARM64_SYS_move_pages ||
        number == ARM64_SYS_membarrier) {
        errno = ENOSYS;
        return -1;
    }

    va_list ap;
    va_start(ap, number);
    long a0 = va_arg(ap, long);
    long a1 = va_arg(ap, long);
    long a2 = va_arg(ap, long);
    long a3 = va_arg(ap, long);
    long a4 = va_arg(ap, long);
    long a5 = va_arg(ap, long);
    va_end(ap);

    real_syscall_t fn = real_syscall();
    if (!fn) { errno = ENOSYS; return -1; }
    return fn(number, a0, a1, a2, a3, a4, a5);
}
