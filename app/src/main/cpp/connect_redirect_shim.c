/*
 * this file redirects connection to api.polytoria.com:443
 * coming from the client to the proxy, so that we can get
 * past the Polytoria API block
 */
#define _GNU_SOURCE
#include <stdlib.h>
#include <stdint.h>
#include <dlfcn.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <pthread.h>

static int   (*real_connect)(int, const struct sockaddr *, socklen_t) = NULL;
static uint32_t from_ip = 0;       // network byte order
static uint32_t to_ip   = 0;
static uint16_t to_port = 0;       // network byte order
static int ready = 0;
static pthread_once_t once = PTHREAD_ONCE_INIT;

static void init(void) {
    real_connect = dlsym(RTLD_NEXT, "connect");

    const char *f = getenv("POLYDROID_REDIRECT_FROM_IP");
    const char *t = getenv("POLYDROID_REDIRECT_TO_IP");
    const char *p = getenv("POLYDROID_REDIRECT_TO_PORT");
    if (!f || !t || !p) return;

    struct in_addr a;
    if (inet_pton(AF_INET, f, &a) != 1) return;
    from_ip = a.s_addr;
    if (inet_pton(AF_INET, t, &a) != 1) return;
    to_ip = a.s_addr;
    int port = atoi(p);
    if (port <= 0 || port > 65535) return;
    to_port = htons((uint16_t)port);

    ready = 1;
}

int connect(int fd, const struct sockaddr *addr, socklen_t len) {
    pthread_once(&once, init);
    if (!real_connect) return -1;

    if (ready && addr && addr->sa_family == AF_INET && len >= (socklen_t)sizeof(struct sockaddr_in)) {
        const struct sockaddr_in *in = (const struct sockaddr_in *)addr;
        if (in->sin_port == htons(443) && in->sin_addr.s_addr == from_ip) {
            struct sockaddr_in rw = *in;
            rw.sin_addr.s_addr = to_ip;
            rw.sin_port = to_port;
            return real_connect(fd, (struct sockaddr *)&rw, sizeof(rw));
        }
    }
    return real_connect(fd, addr, len);
}
