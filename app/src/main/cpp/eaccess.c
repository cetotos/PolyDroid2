#define _GNU_SOURCE
#include <unistd.h>

int eaccess(const char *pathname, int mode) {
    return access(pathname, mode);
}

int euidaccess(const char *pathname, int mode) {
    return access(pathname, mode);
}
