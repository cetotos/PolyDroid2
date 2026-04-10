/*
 * remaps paths from Unity etc. to rootfs paths instead of going to non-accesible Android ones
 * ..nothing else
 */

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>
#include <stdarg.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>

static char g_rootdir[512] = "";
static int  g_initialized = 0;

static void init_rootdir(void) {
    if (g_initialized) return;
    g_initialized = 1;
    const char *rd = getenv("POLYDROID_ROOTDIR");
    if (rd && strlen(rd) < sizeof(g_rootdir) - 1) {
        strncpy(g_rootdir, rd, sizeof(g_rootdir) - 1);
    }
}

static const char *remap_prefixes[] = {
    "/etc/",
    "/usr/",
    "/home/",
    "/tmp/",
    NULL
};

static const char *remap_exact[] = {
    "/etc",
    "/usr",
    "/home",
    "/tmp",
    NULL
};

static const char *remap_path(const char *path, char *buf, size_t bufsz) {
    if (!path) return path;
    init_rootdir();
    if (g_rootdir[0] == '\0') return path;
    if (strncmp(path, g_rootdir, strlen(g_rootdir)) == 0) return path;
    for (const char **p = remap_prefixes; *p; p++) {
        if (strncmp(path, *p, strlen(*p)) == 0) {
            snprintf(buf, bufsz, "%s%s", g_rootdir, path);
            return buf;
        }
    }
    for (const char **p = remap_exact; *p; p++) {
        if (strcmp(path, *p) == 0) {
            snprintf(buf, bufsz, "%s%s", g_rootdir, path);
            return buf;
        }
    }

    return path;
}

typedef int (*orig_open_t)(const char *, int, ...);

int open(const char *pathname, int flags, ...) {
    static orig_open_t real_open = NULL;
    if (!real_open) real_open = (orig_open_t)dlsym(RTLD_NEXT, "open");

    char buf[1024];
    const char *actual = remap_path(pathname, buf, sizeof(buf));

    if (flags & (O_CREAT | O_TMPFILE)) {
        va_list ap;
        va_start(ap, flags);
        mode_t mode = va_arg(ap, mode_t);
        va_end(ap);
        return real_open(actual, flags, mode);
    }
    return real_open(actual, flags);
}

int open64(const char *pathname, int flags, ...) {
    static orig_open_t real_open64 = NULL;
    if (!real_open64) real_open64 = (orig_open_t)dlsym(RTLD_NEXT, "open64");

    char buf[1024];
    const char *actual = remap_path(pathname, buf, sizeof(buf));

    if (flags & (O_CREAT | O_TMPFILE)) {
        va_list ap;
        va_start(ap, flags);
        mode_t mode = va_arg(ap, mode_t);
        va_end(ap);
        return real_open64(actual, flags, mode);
    }
    return real_open64(actual, flags);
}
typedef FILE *(*orig_fopen_t)(const char *, const char *);

FILE *fopen(const char *pathname, const char *mode) {
    static orig_fopen_t real_fopen = NULL;
    if (!real_fopen) real_fopen = (orig_fopen_t)dlsym(RTLD_NEXT, "fopen");

    char buf[1024];
    const char *actual = remap_path(pathname, buf, sizeof(buf));
    return real_fopen(actual, mode);
}

FILE *fopen64(const char *pathname, const char *mode) {
    static orig_fopen_t real_fopen64 = NULL;
    if (!real_fopen64) real_fopen64 = (orig_fopen_t)dlsym(RTLD_NEXT, "fopen64");

    char buf[1024];
    const char *actual = remap_path(pathname, buf, sizeof(buf));
    return real_fopen64(actual, mode);
}
typedef int (*orig_stat_t)(const char *, struct stat *);
typedef int (*orig_access_t)(const char *, int);

int stat(const char *pathname, struct stat *statbuf) {
    static orig_stat_t real_stat = NULL;
    if (!real_stat) real_stat = (orig_stat_t)dlsym(RTLD_NEXT, "stat");

    char buf[1024];
    const char *actual = remap_path(pathname, buf, sizeof(buf));
    return real_stat(actual, statbuf);
}

int lstat(const char *pathname, struct stat *statbuf) {
    static orig_stat_t real_lstat = NULL;
    if (!real_lstat) real_lstat = (orig_stat_t)dlsym(RTLD_NEXT, "lstat");

    char buf[1024];
    const char *actual = remap_path(pathname, buf, sizeof(buf));
    return real_lstat(actual, statbuf);
}

int access(const char *pathname, int mode) {
    static orig_access_t real_access = NULL;
    if (!real_access) real_access = (orig_access_t)dlsym(RTLD_NEXT, "access");

    char buf[1024];
    const char *actual = remap_path(pathname, buf, sizeof(buf));
    return real_access(actual, mode);
}

#include <dirent.h>
typedef DIR *(*orig_opendir_t)(const char *);

DIR *opendir(const char *name) {
    static orig_opendir_t real_opendir = NULL;
    if (!real_opendir) real_opendir = (orig_opendir_t)dlsym(RTLD_NEXT, "opendir");

    char buf[1024];
    const char *actual = remap_path(name, buf, sizeof(buf));
    return real_opendir(actual);
}

typedef ssize_t (*orig_readlink_t)(const char *, char *, size_t);

ssize_t readlink(const char *pathname, char *buf, size_t bufsiz) {
    static orig_readlink_t real_readlink = NULL;
    if (!real_readlink) real_readlink = (orig_readlink_t)dlsym(RTLD_NEXT, "readlink");

    char pbuf[1024];
    const char *actual = remap_path(pathname, pbuf, sizeof(pbuf));
    return real_readlink(actual, buf, bufsiz);
}
