
/*
 * this overrides getaddrinfo because bionic's version cant reach Android's netd
 * checks /etc/hosts first, then does a direct UDp DNS query
 */

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
static const char *resolve_etc_path(const char *guest_path,
                                     char *buf, size_t bufsz)
{
    const char *root = getenv("POLYDROID_ROOTDIR");
    if (root) {
        snprintf(buf, bufsz, "%s%s", root, guest_path);
        return buf;
    }
    return guest_path;
}

static int lookup_hosts_file(const char *name, struct addrinfo **res,
                             const struct addrinfo *hints)
{
    char path_buf[512];
    const char *hosts_path = resolve_etc_path("/etc/hosts", path_buf, sizeof(path_buf));
    FILE *f = fopen(hosts_path, "r");
    if (!f) return EAI_NONAME;

    char line[512];
    while (fgets(line, sizeof(line), f)) {
        line[sizeof(line) - 1] = '\0';
        char *p = line;
        while (*p == ' ' || *p == '\t') p++;
        if (*p == '#' || *p == '\n' || *p == '\r' || *p == '\0') continue;

        char ip[64] = {0};
        char *ip_start = p;
        while (*p && *p != ' ' && *p != '\t' && *p != '\n' && *p != '\r') p++;
        size_t ip_len = (size_t)(p - ip_start);
        if (ip_len == 0 || ip_len >= sizeof(ip)) continue;
        memcpy(ip, ip_start, ip_len);
        ip[ip_len] = '\0';

        for (;;) {
            while (*p == ' ' || *p == '\t') p++;
            if (*p == '\0' || *p == '\n' || *p == '\r' || *p == '#') break;
            char *h_start = p;
            while (*p && *p != ' ' && *p != '\t' && *p != '\n' && *p != '\r') p++;
            char saved = *p;
            *p = '\0';
            int matched = (strcasecmp(h_start, name) == 0);
            *p = saved;
            if (matched) {
                fclose(f);

                struct sockaddr_in sa4;
                struct sockaddr_in6 sa6;
                int af = 0;
                void *addr_storage = NULL;
                socklen_t addrlen = 0;

                if (inet_pton(AF_INET, ip, &sa4.sin_addr) == 1) {
                    af = AF_INET;
                    memset(&sa4, 0, sizeof(sa4));
                    sa4.sin_family = AF_INET;
                    inet_pton(AF_INET, ip, &sa4.sin_addr);
                    addr_storage = &sa4;
                    addrlen = sizeof(sa4);
                } else if (inet_pton(AF_INET6, ip, &sa6.sin6_addr) == 1) {
                    af = AF_INET6;
                    memset(&sa6, 0, sizeof(sa6));
                    sa6.sin6_family = AF_INET6;
                    inet_pton(AF_INET6, ip, &sa6.sin6_addr);
                    addr_storage = &sa6;
                    addrlen = sizeof(sa6);
                } else {
                    return EAI_NONAME;
                }

                if (hints && hints->ai_family != AF_UNSPEC &&
                    hints->ai_family != af)
                    return EAI_NONAME;

                struct addrinfo *ai = calloc(1, sizeof(struct addrinfo));
                if (!ai) return EAI_MEMORY;

                ai->ai_family = af;
                ai->ai_socktype = (hints && hints->ai_socktype) ?
                                   hints->ai_socktype : SOCK_STREAM;
                ai->ai_protocol = (hints && hints->ai_protocol) ?
                                   hints->ai_protocol : 0;
                ai->ai_addrlen = addrlen;
                ai->ai_addr = malloc(addrlen);
                if (!ai->ai_addr) { free(ai); return EAI_MEMORY; }
                memcpy(ai->ai_addr, addr_storage, addrlen);
                ai->ai_canonname = strdup(name);
                ai->ai_next = NULL;

                *res = ai;
                return 0;
            }
        }
    }
    fclose(f);
    return EAI_NONAME;
}
static int dns_query(const char *name, const char *ns_ip, struct addrinfo **res,
                     const struct addrinfo *hints)
{
    int sock = socket(AF_INET, SOCK_DGRAM, 0);
    if (sock < 0) return EAI_SYSTEM;

    struct timeval tv = { .tv_sec = 3, .tv_usec = 0 };
    setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));

    struct sockaddr_in ns_addr;
    memset(&ns_addr, 0, sizeof(ns_addr));
    ns_addr.sin_family = AF_INET;
    ns_addr.sin_port = htons(53);
    if (inet_pton(AF_INET, ns_ip, &ns_addr.sin_addr) != 1) {
        close(sock);
        return EAI_NONAME;
    }

    unsigned char pkt[512];
    memset(pkt, 0, sizeof(pkt));

    uint16_t txid = (uint16_t)(getpid() ^ 0xBEEF);
    pkt[0] = txid >> 8; pkt[1] = txid & 0xFF;
    pkt[2] = 0x01; pkt[3] = 0x00; // RD=1
    pkt[4] = 0x00; pkt[5] = 0x01; // QDCOUNT=1

    // encode hostname
    int off = 12;
    const char *p = name;
    while (*p) {
        const char *dot = strchr(p, '.');
        int len = dot ? (int)(dot - p) : (int)strlen(p);
        if (len > 63 || off + 1 + len > 500) { close(sock); return EAI_NONAME; }
        pkt[off++] = (unsigned char)len;
        memcpy(pkt + off, p, len);
        off += len;
        p += len;
        if (*p == '.') p++;
    }
    pkt[off++] = 0;
    pkt[off++] = 0; pkt[off++] = 1; // QTYPE=A
    pkt[off++] = 0; pkt[off++] = 1; // QCLASS=IN

    ssize_t sent = sendto(sock, pkt, off, 0,
                          (struct sockaddr *)&ns_addr, sizeof(ns_addr));
    if (sent < 0) { close(sock); return EAI_SYSTEM; }

    unsigned char resp[512];
    ssize_t rlen = recvfrom(sock, resp, sizeof(resp), 0, NULL, NULL);
    close(sock);

    if (rlen < 12) return EAI_NONAME;

    int rcode = resp[3] & 0x0F;
    if (rcode != 0) return EAI_NONAME;

    int ancount = (resp[6] << 8) | resp[7];
    if (ancount == 0) return EAI_NONAME;

    // skip question section
    int pos = 12;
    while (pos < rlen && resp[pos] != 0) {
        if ((resp[pos] & 0xC0) == 0xC0) { pos += 2; break; }
        pos += 1 + resp[pos];
    }
    if (pos < rlen && resp[pos] == 0) pos++;
    pos += 4; // QTYPE + QCLASS

    // find first A record
    struct addrinfo *head = NULL;
    for (int i = 0; i < ancount && pos < rlen; i++) {
        if ((resp[pos] & 0xC0) == 0xC0) {
            pos += 2;
        } else {
            while (pos < rlen && resp[pos] != 0) pos += 1 + resp[pos];
            if (pos < rlen) pos++;
        }

        if (pos + 10 > rlen) break;
        uint16_t rtype = (resp[pos] << 8) | resp[pos + 1];
        uint16_t rdlen = (resp[pos + 8] << 8) | resp[pos + 9];
        pos += 10;

        if (rtype == 1 && rdlen == 4 && pos + 4 <= rlen) {
            if (hints && hints->ai_family != AF_UNSPEC &&
                hints->ai_family != AF_INET) {
                pos += rdlen;
                continue;
            }

            struct addrinfo *ai = calloc(1, sizeof(struct addrinfo));
            if (!ai) break;
            struct sockaddr_in *sa = calloc(1, sizeof(struct sockaddr_in));
            if (!sa) { free(ai); break; }

            sa->sin_family = AF_INET;
            memcpy(&sa->sin_addr, resp + pos, 4);

            ai->ai_family = AF_INET;
            ai->ai_socktype = (hints && hints->ai_socktype) ?
                               hints->ai_socktype : SOCK_STREAM;
            ai->ai_protocol = (hints && hints->ai_protocol) ?
                               hints->ai_protocol : 0;
            ai->ai_addrlen = sizeof(struct sockaddr_in);
            ai->ai_addr = (struct sockaddr *)sa;
            ai->ai_canonname = strdup(name);
            ai->ai_next = head;
            head = ai;

            pos += rdlen;
            break;
        } else {
            pos += rdlen;
        }
    }

    if (head) {
        *res = head;
        return 0;
    }
    return EAI_NONAME;
}

#define MAX_NS 4

static int read_nameservers(char ns[][64], int max)
{
    char path_buf[512];
    const char *resolv_path = resolve_etc_path("/etc/resolv.conf", path_buf, sizeof(path_buf));
    FILE *f = fopen(resolv_path, "r");
    if (!f) return 0;

    int count = 0;
    char line[256];
    while (fgets(line, sizeof(line), f) && count < max) {
        char *p = line;
        while (*p == ' ' || *p == '\t') p++;
        if (strncmp(p, "nameserver", 10) != 0) continue;
        p += 10;
        while (*p == ' ' || *p == '\t') p++;
        char *end = p;
        while (*end && *end != ' ' && *end != '\t' && *end != '\n') end++;
        *end = '\0';
        if (strlen(p) > 0 && strlen(p) < 64) {
            strncpy(ns[count], p, 63);
            count++;
        }
    }
    fclose(f);
    return count;
}

int getaddrinfo(const char *node, const char *service,
                const struct addrinfo *hints, struct addrinfo **res)
{
    if (!node || !res) return EAI_NONAME;

    *res = NULL;
    struct in_addr tmp4;
    struct in6_addr tmp6;
    if (inet_pton(AF_INET, node, &tmp4) == 1 ||
        inet_pton(AF_INET6, node, &tmp6) == 1) {
        struct addrinfo *ai = calloc(1, sizeof(struct addrinfo));
        if (!ai) return EAI_MEMORY;

        if (inet_pton(AF_INET, node, &tmp4) == 1) {
            struct sockaddr_in *sa = calloc(1, sizeof(struct sockaddr_in));
            if (!sa) { free(ai); return EAI_MEMORY; }
            sa->sin_family = AF_INET;
            sa->sin_addr = tmp4;
            if (service) sa->sin_port = htons(atoi(service));
            ai->ai_family = AF_INET;
            ai->ai_addrlen = sizeof(*sa);
            ai->ai_addr = (struct sockaddr *)sa;
        } else {
            struct sockaddr_in6 *sa = calloc(1, sizeof(struct sockaddr_in6));
            if (!sa) { free(ai); return EAI_MEMORY; }
            sa->sin6_family = AF_INET6;
            sa->sin6_addr = tmp6;
            if (service) sa->sin6_port = htons(atoi(service));
            ai->ai_family = AF_INET6;
            ai->ai_addrlen = sizeof(*sa);
            ai->ai_addr = (struct sockaddr *)sa;
        }
        ai->ai_socktype = (hints && hints->ai_socktype) ?
                           hints->ai_socktype : SOCK_STREAM;
        ai->ai_protocol = (hints && hints->ai_protocol) ?
                           hints->ai_protocol : 0;
        ai->ai_canonname = strdup(node);
        ai->ai_next = NULL;
        *res = ai;
        return 0;
    }

    // check /etc/hosts
    int rc = lookup_hosts_file(node, res, hints);
    if (rc == 0) {
        if (service && *res) {
            int port = atoi(service);
            if ((*res)->ai_family == AF_INET)
                ((struct sockaddr_in *)(*res)->ai_addr)->sin_port = htons(port);
            else if ((*res)->ai_family == AF_INET6)
                ((struct sockaddr_in6 *)(*res)->ai_addr)->sin6_port = htons(port);
        }
        return 0;
    }

    // direct UDP DNS query
    char nameservers[MAX_NS][64];
    int ns_count = read_nameservers(nameservers, MAX_NS);

    if (ns_count == 0) {
        strcpy(nameservers[0], "8.8.8.8");
        strcpy(nameservers[1], "8.8.4.4");
        ns_count = 2;
    }

    for (int i = 0; i < ns_count; i++) {
        rc = dns_query(node, nameservers[i], res, hints);
        if (rc == 0) {
            if (service && *res) {
                int port = atoi(service);
                if ((*res)->ai_family == AF_INET)
                    ((struct sockaddr_in *)(*res)->ai_addr)->sin_port = htons(port);
                else if ((*res)->ai_family == AF_INET6)
                    ((struct sockaddr_in6 *)(*res)->ai_addr)->sin6_port = htons(port);
            }
            return 0;
        }
    }

    return EAI_NONAME;
}

void freeaddrinfo(struct addrinfo *res)
{
    while (res) {
        struct addrinfo *next = res->ai_next;
        free(res->ai_addr);
        free(res->ai_canonname);
        free(res);
        res = next;
    }
}

int getnameinfo(const struct sockaddr *sa, socklen_t salen,
                char *host, socklen_t hostlen,
                char *serv, socklen_t servlen,
                int flags)
{
    if (host && hostlen > 0) {
        if (sa->sa_family == AF_INET) {
            const struct sockaddr_in *sin = (const struct sockaddr_in *)sa;
            inet_ntop(AF_INET, &sin->sin_addr, host, hostlen);
        } else if (sa->sa_family == AF_INET6) {
            const struct sockaddr_in6 *sin6 = (const struct sockaddr_in6 *)sa;
            inet_ntop(AF_INET6, &sin6->sin6_addr, host, hostlen);
        } else {
            return EAI_FAMILY;
        }
    }
    if (serv && servlen > 0) {
        if (sa->sa_family == AF_INET)
            snprintf(serv, servlen, "%d",
                     ntohs(((const struct sockaddr_in *)sa)->sin_port));
        else if (sa->sa_family == AF_INET6)
            snprintf(serv, servlen, "%d",
                     ntohs(((const struct sockaddr_in6 *)sa)->sin6_port));
    }
    return 0;
}
