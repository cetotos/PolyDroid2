#define _GNU_SOURCE
#include <unistd.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static const char *machine_id_path(const char *p, char *buf, size_t sz) {
    static const char rel[] = "/etc/machine-id";
    if (!p || (strcmp(p, "/etc/machine-id") != 0 && strcmp(p, "/var/lib/dbus/machine-id") != 0))
        return p;
    const char *root = getenv("POLYDROID_ROOTDIR");
    if (!root) return p;
    size_t rl = strlen(root);
    if (rl + sizeof(rel) > sz) return p;
    memcpy(buf, root, rl);
    memcpy(buf + rl, rel, sizeof(rel));
    return buf;
}

static mode_t open_mode(int flags, va_list ap) {
    if (flags & (O_CREAT | __O_TMPFILE)) return (mode_t)va_arg(ap, int);
    return 0;
}

int open(const char *p, int flags, ...) {
    static int (*real)(const char *, int, ...);
    if (!real) real = dlsym(RTLD_NEXT, "open");
    va_list ap; va_start(ap, flags); mode_t m = open_mode(flags, ap); va_end(ap);
    char buf[512];
    return real(machine_id_path(p, buf, sizeof(buf)), flags, m);
}

int open64(const char *p, int flags, ...) {
    static int (*real)(const char *, int, ...);
    if (!real) real = dlsym(RTLD_NEXT, "open64");
    va_list ap; va_start(ap, flags); mode_t m = open_mode(flags, ap); va_end(ap);
    char buf[512];
    return real(machine_id_path(p, buf, sizeof(buf)), flags, m);
}

int openat(int dirfd, const char *p, int flags, ...) {
    static int (*real)(int, const char *, int, ...);
    if (!real) real = dlsym(RTLD_NEXT, "openat");
    va_list ap; va_start(ap, flags); mode_t m = open_mode(flags, ap); va_end(ap);
    char buf[512];
    return real(dirfd, machine_id_path(p, buf, sizeof(buf)), flags, m);
}

FILE *fopen(const char *p, const char *mode) {
    static FILE *(*real)(const char *, const char *);
    if (!real) real = dlsym(RTLD_NEXT, "fopen");
    char buf[512];
    return real(machine_id_path(p, buf, sizeof(buf)), mode);
}

int __xstat64(int ver, const char *p, void *st) {
    static int (*real)(int, const char *, void *);
    if (!real) real = dlsym(RTLD_NEXT, "__xstat64");
    char buf[512];
    return real(ver, machine_id_path(p, buf, sizeof(buf)), st);
}

int __xstat(int ver, const char *p, void *st) {
    static int (*real)(int, const char *, void *);
    if (!real) real = dlsym(RTLD_NEXT, "__xstat");
    char buf[512];
    return real(ver, machine_id_path(p, buf, sizeof(buf)), st);
}

int stat64(const char *p, void *st) {
    static int (*real)(const char *, void *);
    if (!real) real = dlsym(RTLD_NEXT, "stat64");
    char buf[512];
    return real(machine_id_path(p, buf, sizeof(buf)), st);
}

int lstat64(const char *p, void *st) {
    static int (*real)(const char *, void *);
    if (!real) real = dlsym(RTLD_NEXT, "lstat64");
    char buf[512];
    return real(machine_id_path(p, buf, sizeof(buf)), st);
}

int access(const char *p, int mode) {
    static int (*real)(const char *, int);
    if (!real) real = dlsym(RTLD_NEXT, "access");
    char buf[512];
    return real(machine_id_path(p, buf, sizeof(buf)), mode);
}

char *realpath(const char *p, char *resolved) {
    static char *(*real)(const char *, char *);
    if (!real) real = dlsym(RTLD_NEXT, "realpath");
    char buf[512];
    return real(machine_id_path(p, buf, sizeof(buf)), resolved);
}

FILE *fopen64(const char *p, const char *mode) {
    static FILE *(*real)(const char *, const char *);
    if (!real) real = dlsym(RTLD_NEXT, "fopen64");
    char buf[512];
    return real(machine_id_path(p, buf, sizeof(buf)), mode);
}
