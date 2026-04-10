#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <poll.h>
#include <errno.h>
#include <stdint.h>
#include <stddef.h>

static void stub_log(const char* fmt, ...) {
    char buf[256];
    va_list ap;
    va_start(ap, fmt);
    int n = vsnprintf(buf, sizeof(buf), fmt, ap);
    va_end(ap);
    if (n > 0) write(STDERR_FILENO, buf, (size_t)n);
}
typedef unsigned long XID;
typedef XID Window;
typedef XID Drawable;
typedef XID Colormap;
typedef XID Cursor;
typedef XID Pixmap;
typedef XID Atom;
typedef XID Font;
typedef XID KeySym;
typedef unsigned char KeyCode;
typedef unsigned long Time;
typedef unsigned long VisualID;
typedef int Bool;
typedef int Status;
#define True 1
#define False 0
#define None 0L
#define Success 0
#define TrueColor 4
#define InputOutput 1
#define AllocNone 0

typedef void* GC;
typedef void* XIM;
typedef void* XIC;
typedef void* XFontSet;
typedef void* XPointer;

typedef struct _XDisplay Display;
typedef int (*XErrorHandler)(Display*, void*);
typedef int (*XIOErrorHandler)(Display*);

struct Visual {
    void* ext_data;
    VisualID visualid;
    int c_class;
    unsigned long red_mask;
    unsigned long green_mask;
    unsigned long blue_mask;
    int bits_per_rgb;
    int map_entries;
};

typedef struct {
    struct Visual *visual;
    VisualID visualid;
    int screen;
    int depth;
    int c_class;
    unsigned long red_mask;
    unsigned long green_mask;
    unsigned long blue_mask;
    int colormap_size;
    int bits_per_rgb;
} XVisualInfo;

typedef struct {
    unsigned char *value;
    Atom encoding;
    int format;
    unsigned long nitems;
} XTextProperty;

typedef struct {
    long flags;
    int x, y;
    int width, height;
    int min_width, min_height;
    int max_width, max_height;
    int width_inc, height_inc;
    struct { int x; int y; } min_aspect, max_aspect;
    int base_width, base_height;
    int win_gravity;
} XSizeHints;

typedef struct {
    long flags;
    int input;
    int initial_state;
    Pixmap icon_pixmap;
    Window icon_window;
    int icon_x, icon_y;
    Pixmap icon_mask;
    XID window_group;
} XWMHints;

typedef struct {
    char *res_name;
    char *res_class;
} XClassHint;

typedef struct {
    short lbearing, rbearing;
    short width;
    short ascent, descent;
    unsigned short attributes;
} XCharStruct;

typedef struct {
    void* ext_data;
    Font fid;
    unsigned direction;
    unsigned min_char_or_byte2, max_char_or_byte2;
    unsigned min_byte1, max_byte1;
    Bool all_chars_exist;
    unsigned default_char;
    int n_properties;
    void* properties;
    XCharStruct min_bounds, max_bounds;
    XCharStruct* per_char;
    int ascent, descent;
} XFontStruct;

typedef struct {
    int type;
    unsigned long serial;
    Bool send_event;
    Display *display;
    Window window;
    char pad[256];
} XEvent;

typedef XEvent XKeyEvent;
typedef XEvent XKeyPressedEvent;
typedef XEvent XMappingEvent;

typedef struct { int max_keypermod; KeyCode *modifiermap; } XModifierKeymap;

typedef struct {
    int x, y;
    int width, height;
    int border_width;
    int depth;
    struct Visual *visual;
    Window root;
    int c_class;
    int bit_gravity, win_gravity;
    int backing_store;
    unsigned long backing_planes, backing_pixel;
    Bool save_under;
    Colormap colormap;
    Bool map_installed;
    int map_state;
    long all_event_masks, your_event_mask, do_not_propagate_mask;
    Bool override_redirect;
    void *screen;
} XWindowAttributes;

typedef void* XComposeStatus;
typedef void* XSetWindowAttributes;
typedef void* XGCValues;
typedef void* XColor;

typedef struct { int count; int success; } XPixmapFormatValues;

typedef struct {
    void* ext_data;
    struct _XDisplay *display;
    Window root;
    int width, height;
    int mwidth, mheight;
    int ndepths;
    void* depths;
    int root_depth;
    struct Visual *root_visual;
    GC default_gc;
    Colormap cmap;
    unsigned long white_pixel, black_pixel;
    int max_maps, min_maps;
    int backing_store;
    Bool save_unders;
    long root_input_mask;
} Screen;

typedef struct _XImage {
    int width, height;
    int xoffset;
    int format;
    char *data;
    int byte_order;
    int bitmap_unit, bitmap_bit_order, bitmap_pad;
    int depth;
    int bytes_per_line;
    int bits_per_pixel;
    unsigned long red_mask, green_mask, blue_mask;
    XPointer obdata;
    struct funcs {
        void* create_image;
        int (*destroy_image)(struct _XImage*);
        unsigned long (*get_pixel)(struct _XImage*, int, int);
        int (*put_pixel)(struct _XImage*, int, int, unsigned long);
        void* sub_image;
        int (*add_pixel)(struct _XImage*, long);
    } f;
} XImage;

typedef void* xReply;
typedef void* xGenericReply;
typedef void* XGenericEventCookie;
typedef void* XkbStatePtr;
typedef void* XkbDescPtr;
typedef unsigned long BarrierEventID;
typedef unsigned long PointerBarrier;
typedef void* XrmHashBucketRec;
typedef int XICCEncodingStyle;
typedef struct { short x, y, width, height; } XRectangle;

static struct Visual s_visual = {
    .visualid = 0x21,
    .c_class = TrueColor,
    .red_mask = 0xFF0000,
    .green_mask = 0x00FF00,
    .blue_mask = 0x0000FF,
    .bits_per_rgb = 8,
    .map_entries = 256,
};

static Screen s_screen = {
    .width = 1280,
    .height = 1024,
    .mwidth = 338,
    .mheight = 270,
    .root_depth = 24,
    .root_visual = &s_visual,
    .root = 0x1,
    .cmap = 0x20,
    .white_pixel = 0xFFFFFF,
    .black_pixel = 0x000000,
};

// fake enough XDisplay values for SDL to get through
struct _XDisplay {
    void* ext_data;
    void* private1;            // 8
    int fd;                    // 16
    int private2;              // 20
    int proto_major_version;   // 24
    int proto_minor_version;   // 28
    char *vendor;              // 32
    XID private3;              // 40
    XID private4;              // 48
    XID private5;              // 56
    int private6;              // 64
    XID (*resource_alloc)(struct _XDisplay*); // 68
    int byte_order;            // 76
    int bitmap_unit;           // 80
    int bitmap_pad;            // 84
    int bitmap_bit_order;      // 88
    int nformats;              // 92
    void* pixmap_format;       // 96
    int private8;              // 104
    int release;               // 108
    void *private9, *private10;// 112, 120
    int qlen;                  // 128
    unsigned long last_request_read; // 132
    unsigned long request;     // 140
    XPointer private11;        // 148
    XPointer private12;        // 156
    XPointer private13;        // 164
    XPointer private14;        // 172
    unsigned max_request_size; // 180
    void *db;                  // 184
    int (*private15)(struct _XDisplay*); // 192
    char *display_name;        // 200
    int default_screen;        // 208
    int nscreens;              // 212
    Screen *screens;           // 216
};

static XID dummy_resource_alloc(struct _XDisplay *dpy) {
    static XID counter = 0x100;
    return ++counter;
}

static struct _XDisplay s_display = {
    .fd = 3,
    .proto_major_version = 11,
    .proto_minor_version = 0,
    .vendor = "PolyDroid",
    .byte_order = 0,
    .bitmap_unit = 32,
    .bitmap_pad = 32,
    .bitmap_bit_order = 0,
    .release = 12101000,
    .max_request_size = 65535,
    .display_name = ":0",
    .default_screen = 0,
    .nscreens = 1,
    .screens = &s_screen,
    .resource_alloc = dummy_resource_alloc,
};
static int s_display_initialized = 0;

static void ensure_display_init(void) {
    if (s_display_initialized) return;
    s_display_initialized = 1;
    s_screen.display = &s_display;
    stub_log("stubx11: display initialized (%dx%d)\n",
            s_screen.width, s_screen.height);
}

__attribute__((constructor))
static void init_from_env(void) {
    const char* w = getenv("POLYDROID_SCREEN_WIDTH");
    const char* h = getenv("POLYDROID_SCREEN_HEIGHT");
    if (w) s_screen.width = atoi(w);
    if (h) s_screen.height = atoi(h);
    // stub_log("stubx11: oaded (screen=%dx%d)\n",
    //        s_screen.width, s_screen.height);
}

#define DUMMY_WINDOW ((Window)0xDEAD01)
#define DUMMY_COLORMAP ((Colormap)0x20)


// -------------- do a REAL X11 connection to Lorie for input ---------
static int s_x11_fd = -1;
static int s_events_ready = 0;
static uint32_t s_resource_base = 0;
static uint32_t s_resource_mask = 0;
static uint32_t s_next_resource = 0;
static uint32_t s_real_window = 0;
static uint32_t s_x11_root = 0;

#define XEVENT_REAL_SIZE 192

static uint32_t alloc_xid(void) {
    s_next_resource++;
    return s_resource_base | (s_next_resource & s_resource_mask);
}
#define EQ_SIZE 256
static XEvent s_eq[EQ_SIZE];
static int s_eq_head = 0;
static int s_eq_count = 0;

static int s_eq_push_log = 0;
static void eq_push(const XEvent *ev) {
    if (s_eq_count >= EQ_SIZE) {
        s_eq_head = (s_eq_head + 1) % EQ_SIZE;
        s_eq_count--;
    }
    int idx = (s_eq_head + s_eq_count) % EQ_SIZE;
    s_eq[idx] = *ev;
    s_eq_count++;
    if (s_eq_push_log < 10) {
        s_eq_push_log++;
    }
}

static int read_exact(int fd, void *buf, int n) {
    int off = 0;
    while (off < n) {
        int r = read(fd, (char*)buf + off, n - off);
        if (r <= 0) return -1;
        off += r;
    }
    return 0;
}

static int write_exact(int fd, const void *buf, int n) {
    int off = 0;
    while (off < n) {
        int w = write(fd, (const char*)buf + off, n - off);
        if (w <= 0) return -1;
        off += w;
    }
    return 0;
}

// ---------- x11 handshake ------

static int x11_connect(void) {
    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) {
        stub_log("stubx11: socket(): %s\n", strerror(errno));
        return -1;
    }

    struct sockaddr_un sa;
    memset(&sa, 0, sizeof(sa));
    sa.sun_family = AF_UNIX;
    // path is mapped here
    memcpy(sa.sun_path + 1, "/tmp/.X11-unix/X0", 17);
    socklen_t salen = offsetof(struct sockaddr_un, sun_path) + 1 + 17;

    int ok = 0;
    for (int i = 0; i < 30; i++) {
        if (connect(fd, (struct sockaddr*)&sa, salen) == 0) { ok = 1; break; }
        usleep(200000);
    }
    if (!ok) {
        stub_log("stubx11: X11 connect failed! %s\n", strerror(errno));
        close(fd);
        return -1;
    }
    stub_log("stubx11: connected to X11 server fd=%d\n", fd);
    unsigned char creq[12] = { 'l',0, 11,0, 0,0, 0,0, 0,0, 0,0 };
    if (write_exact(fd, creq, 12) < 0) { close(fd); return -1; }
    unsigned char hdr[8];
    if (read_exact(fd, hdr, 8) < 0) { close(fd); return -1; }
    if (hdr[0] != 1) {
        stub_log("stubx11: X11 setup rejected! status=%d\n", hdr[0]);
        close(fd);
        return -1;
    }

    uint16_t add_words = *(uint16_t*)(hdr + 6);
    int add_bytes = add_words * 4;
    unsigned char *sd = (unsigned char*)malloc(add_bytes);
    if (!sd || read_exact(fd, sd, add_bytes) < 0) {
        stub_log("stubx11: failed reading setup data\n");
        free(sd); close(fd); return -1;
    }
    s_resource_base = *(uint32_t*)(sd + 4);
    s_resource_mask = *(uint32_t*)(sd + 8);
    uint16_t vlen = *(uint16_t*)(sd + 16);
    uint8_t nscr = sd[20];
    uint8_t nfmt = sd[21];
    int vpad = (4 - (vlen % 4)) % 4;
    int scr_off = 32 + vlen + vpad + nfmt * 8;
    if (nscr > 0 && scr_off + 40 <= add_bytes) {
        s_x11_root = *(uint32_t*)(sd + scr_off);
        stub_log("stubx11: root=0x%x res_base=0x%x res_mask=0x%x\n",
                 s_x11_root, s_resource_base, s_resource_mask);
    }

    free(sd);
    s_x11_fd = fd;
    s_display.fd = fd;
    return 0;
}

static void x11_create_and_map(int w, int h) {
    if (s_x11_fd < 0) return;
    s_real_window = alloc_xid();
    unsigned char cw[36];
    memset(cw, 0, 36);
    cw[0] = 1; // CreateWindow opcode
    *(uint16_t*)(cw+2)  = 9; // request length
    *(uint32_t*)(cw+4)  = s_real_window;
    *(uint32_t*)(cw+8)  = s_x11_root;
    *(uint16_t*)(cw+16) = (uint16_t)w;
    *(uint16_t*)(cw+18) = (uint16_t)h;
    *(uint32_t*)(cw+28) = 0x800;
    *(uint32_t*)(cw+32) = 0x0022004F;
    write_exact(s_x11_fd, cw, 36);
    unsigned char mw[8] = {0};
    mw[0] = 8;
    *(uint16_t*)(mw+2) = 2;
    *(uint32_t*)(mw+4) = s_real_window;
    write_exact(s_x11_fd, mw, 8);
    unsigned char cwa[16];
    memset(cwa, 0, 16);
    cwa[0] = 2;                             // ChangeWindowAttributes opcode
    *(uint16_t*)(cwa+2) = 4;               // length = 4 dwords (16 bytes)
    *(uint32_t*)(cwa+4) = s_x11_root;      // root window */
    *(uint32_t*)(cwa+8) = 0x800;           // value-mask: EventMask
    *(uint32_t*)(cwa+12) = 0x0022004F;     // same event mask
    write_exact(s_x11_fd, cwa, 16);

/*    stub_log("stubx11: created and mapped window 0x%x (%dx%d)",
             s_real_window, w, h, s_x11_root);*/
}

// ----------- wire event parsing ------
#define X11_KeyPress         2
#define X11_KeyRelease       3
#define X11_ButtonPress      4
#define X11_ButtonRelease    5
#define X11_MotionNotify     6
#define X11_FocusIn          9
#define X11_FocusOut        10
#define X11_MapNotify       19
#define X11_ConfigureNotify 22

static int s_event_log_count = 0;

static void parse_and_queue(const unsigned char *wire) {
    uint8_t raw = wire[0];
    uint8_t type = raw & 0x7F;
    if (type == 0 || type == 1) return;

    if (s_event_log_count < 10) {
        int16_t ex = *(int16_t*)(wire+24);
        int16_t ey = *(int16_t*)(wire+26);
        // stub_log("stubx11: wire event type=%d detail=%d win=0x%x x=%d y=%d\n",
        //          type, wire[1], *(uint32_t*)(wire+12), (int)ex, (int)ey);
        s_event_log_count++;
    }
    XEvent ev;
    memset(&ev, 0, sizeof(ev));
    unsigned char *p = (unsigned char*)&ev;

    *(int*)(p)             = type;
    *(unsigned long*)(p+8) = (unsigned long)*(uint16_t*)(wire+2);
    *(int*)(p+16)          = (raw & 0x80) ? True : False;
    *(Display**)(p+24)     = &s_display;

    switch (type) {
    case X11_KeyPress: case X11_KeyRelease:
    case X11_ButtonPress: case X11_ButtonRelease:
    case X11_MotionNotify: {
        uint32_t ww = *(uint32_t*)(wire+12);
        *(Window*)(p+32)       = (ww == s_real_window || ww == s_x11_root) ? DUMMY_WINDOW : (Window)ww;
        *(Window*)(p+40)       = (Window)*(uint32_t*)(wire+8);
        *(Window*)(p+48)       = (Window)*(uint32_t*)(wire+16);
        *(unsigned long*)(p+56)= (unsigned long)*(uint32_t*)(wire+4);
        *(int*)(p+64)          = (int)*(int16_t*)(wire+24);
        *(int*)(p+68)          = (int)*(int16_t*)(wire+26);
        *(int*)(p+72)          = (int)*(int16_t*)(wire+20);
        *(int*)(p+76)          = (int)*(int16_t*)(wire+22);
        *(unsigned int*)(p+80) = (unsigned int)*(uint16_t*)(wire+28);
        *(unsigned int*)(p+84) = (unsigned int)wire[1];
        *(int*)(p+88)          = (int)wire[30];
        break;
    }
    case X11_FocusIn: case X11_FocusOut: {
        uint32_t ww = *(uint32_t*)(wire+4);
        *(Window*)(p+32) = (ww == s_real_window) ? DUMMY_WINDOW : (Window)ww;
        *(int*)(p+40)    = (int)wire[8];
        *(int*)(p+44)    = (int)wire[1];
        break;
    }
    case X11_MapNotify: {
        uint32_t we = *(uint32_t*)(wire+4);
        uint32_t ww = *(uint32_t*)(wire+8);
        *(Window*)(p+32) = (we == s_real_window) ? DUMMY_WINDOW : (Window)we;
        *(Window*)(p+40) = (ww == s_real_window) ? DUMMY_WINDOW : (Window)ww;
        *(int*)(p+48)    = (int)wire[12];
        break;
    }
    case X11_ConfigureNotify: {
        uint32_t we = *(uint32_t*)(wire+4);
        uint32_t ww = *(uint32_t*)(wire+8);
        *(Window*)(p+32) = (we == s_real_window) ? DUMMY_WINDOW : (Window)we;
        *(Window*)(p+40) = (ww == s_real_window) ? DUMMY_WINDOW : (Window)ww;
        *(int*)(p+48)    = (int)*(int16_t*)(wire+16);
        *(int*)(p+52)    = (int)*(int16_t*)(wire+18);
        *(int*)(p+56)    = (int)*(uint16_t*)(wire+20);
        *(int*)(p+60)    = (int)*(uint16_t*)(wire+22);
        *(int*)(p+64)    = (int)*(uint16_t*)(wire+24);
        *(Window*)(p+72) = (Window)*(uint32_t*)(wire+12);
        *(int*)(p+80)    = (int)wire[26];
        break;
    }
    default:
        *(Window*)(p+32) = DUMMY_WINDOW;
        break;
    }

    eq_push(&ev);
}

static unsigned char s_rbuf[8192];
static int s_rbuf_len = 0;
static int s_input_sock = -1;
static void input_sock_init(void);
static void drain_input_socket(void);
static int s_pending_calls = 0;
static void x11_drain_events(void) {
    if (s_x11_fd < 0 || !s_events_ready) return;
    if (s_input_sock < 0) input_sock_init();
    drain_input_socket();

    static int s_drain_log = 0;
    s_pending_calls++;
/*    if (s_pending_calls <= 5 || (s_pending_calls % 1000 == 0 && s_pending_calls <= 10000)) {
        stub_log("stubx11: drain called #%d, fd=%d, eq=%d\n",
                 s_pending_calls, s_x11_fd, s_eq_count);
    }*/
    struct pollfd pfd = { .fd = s_x11_fd, .events = POLLIN };
    while (poll(&pfd, 1, 0) > 0 && (pfd.revents & POLLIN)) {
        int space = (int)sizeof(s_rbuf) - s_rbuf_len;
        if (space <= 0) break;
        int n = read(s_x11_fd, s_rbuf + s_rbuf_len, space);
        if (n <= 0) break;
        s_rbuf_len += n;
/*        if (s_drain_log < 20) {
            stub_log("stubx11: drain: read %d bytes, buf=%d\n", n, s_rbuf_len);
            s_drain_log++;
        }*/

        int consumed = 0;
        while (s_rbuf_len - consumed >= 32) {
            uint8_t t = s_rbuf[consumed] & 0x7F;
            if (t == 0) {
                stub_log("stubx11: X11 error code=%d\n", s_rbuf[consumed + 1]);
                consumed += 32;
                continue;
            }
            if (t == 1) {
                uint32_t add = *(uint32_t*)(s_rbuf + consumed + 4);
                int total = 32 + (int)(add * 4);
                if (s_rbuf_len - consumed < total) break;
                consumed += total;
                continue;
            }
            parse_and_queue(s_rbuf + consumed);
            consumed += 32;
        }
        if (consumed > 0) {
            s_rbuf_len -= consumed;
            if (s_rbuf_len > 0)
                memmove(s_rbuf, s_rbuf + consumed, s_rbuf_len);
        }
        pfd.revents = 0;
    }
}

struct polydroid_input_msg {
    uint8_t  type;
    uint8_t  button;
    uint16_t pad;
    int32_t  x;
    int32_t  y;
    uint32_t time_ms;
};

static int s_input_log = 0;

static void input_sock_init(void) {
    if (s_input_sock >= 0) return;
    s_input_sock = socket(AF_UNIX, SOCK_DGRAM, 0);
    if (s_input_sock < 0) {
        stub_log("stubx11: input socket create failed: %s\n", strerror(errno));
        return;
    }
    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    addr.sun_path[0] = '\0';
    memcpy(addr.sun_path + 1, "polydroid_input", 15);
    socklen_t len = offsetof(struct sockaddr_un, sun_path) + 1 + 15;
    if (bind(s_input_sock, (struct sockaddr*)&addr, len) < 0) {
        stub_log("stubx11: input socket bind failed: %s\n", strerror(errno));
        close(s_input_sock);
        s_input_sock = -1;
        return;
    }
    stub_log("stubx11: input socket listening (fd=%d)\n", s_input_sock);
}

static void drain_input_socket(void) {
    if (s_input_sock < 0) return;
    for (;;) {
        struct polydroid_input_msg msg;
        ssize_t n = recv(s_input_sock, &msg, sizeof(msg), MSG_DONTWAIT);
        if (n < (ssize_t)sizeof(msg)) break;

        if (s_input_log < 10) {
            stub_log("stubx11: input_sock: type=%d btn=%d x=%d y=%d\n",
                     msg.type, msg.button, msg.x, msg.y);
            s_input_log++;
        }

        XEvent ev;
        memset(&ev, 0, sizeof(ev));
        unsigned char *p = (unsigned char*)&ev;
        *(Display**)(p+24) = &s_display;
        *(Window*)(p+32)   = DUMMY_WINDOW;
        *(Window*)(p+40)   = (Window)s_x11_root;
        *(unsigned long*)(p+56) = (unsigned long)msg.time_ms;
        *(int*)(p+64)      = msg.x;
        *(int*)(p+68)      = msg.y;
        *(int*)(p+72)      = msg.x;
        *(int*)(p+76)      = msg.y;
        *(int*)(p+88)      = 1;

        if (msg.type == 1) {
            *(int*)(p) = 6;
            eq_push(&ev);
        } else if (msg.type == 2) {
            *(int*)(p) = 6;
            eq_push(&ev);
            *(int*)(p) = 4;
            *(unsigned int*)(p+84) = (unsigned int)msg.button;
            eq_push(&ev);
        } else if (msg.type == 3) {
            *(int*)(p) = 6;
            eq_push(&ev);
            *(int*)(p) = 5;
            *(unsigned int*)(p+84) = (unsigned int)msg.button;
            eq_push(&ev);
        } else if (msg.type == 4 || msg.type == 5) {
            *(int*)(p) = (msg.type == 4) ? 2 : 3;
            *(unsigned int*)(p+84) = (unsigned int)msg.x;
            eq_push(&ev);
        }
    }
}


static KeySym s_keycode_to_keysym[256] = {
    [  9] = 0xff1b, /* Escape */
    [ 10] = 0x0031, /* 1 */  [ 11] = 0x0032, /* 2 */  [ 12] = 0x0033, /* 3 */
    [ 13] = 0x0034, /* 4 */  [ 14] = 0x0035, /* 5 */  [ 15] = 0x0036, /* 6 */
    [ 16] = 0x0037, /* 7 */  [ 17] = 0x0038, /* 8 */  [ 18] = 0x0039, /* 9 */
    [ 19] = 0x0030, /* 0 */
    [ 20] = 0x002d, /* minus */     [ 21] = 0x003d, /* equal */
    [ 22] = 0xff08, /* BackSpace */ [ 23] = 0xff09, /* Tab */
    [ 24] = 0x0071, /* q */  [ 25] = 0x0077, /* w */  [ 26] = 0x0065, /* e */
    [ 27] = 0x0072, /* r */  [ 28] = 0x0074, /* t */  [ 29] = 0x0079, /* y */
    [ 30] = 0x0075, /* u */  [ 31] = 0x0069, /* i */  [ 32] = 0x006f, /* o */
    [ 33] = 0x0070, /* p */
    [ 34] = 0x005b, /* bracketleft */  [ 35] = 0x005d, /* bracketright */
    [ 36] = 0xff0d, /* Return */
    [ 37] = 0xffe3, /* Control_L */
    [ 38] = 0x0061, /* a */  [ 39] = 0x0073, /* s */  [ 40] = 0x0064, /* d */
    [ 41] = 0x0066, /* f */  [ 42] = 0x0067, /* g */  [ 43] = 0x0068, /* h */
    [ 44] = 0x006a, /* j */  [ 45] = 0x006b, /* k */  [ 46] = 0x006c, /* l */
    [ 47] = 0x003b, /* semicolon */  [ 48] = 0x0027, /* apostrophe */
    [ 49] = 0x0060, /* grave */
    [ 50] = 0xffe1, /* Shift_L */
    [ 51] = 0x005c, /* backslash */
    [ 52] = 0x007a, /* z */  [ 53] = 0x0078, /* x */  [ 54] = 0x0063, /* c */
    [ 55] = 0x0076, /* v */  [ 56] = 0x0062, /* b */  [ 57] = 0x006e, /* n */
    [ 58] = 0x006d, /* m */
    [ 59] = 0x002c, /* comma */  [ 60] = 0x002e, /* period */
    [ 61] = 0x002f, /* slash */
    [ 62] = 0xffe2, /* Shift_R */
    [ 64] = 0xffe9, /* Alt_L */
    [ 65] = 0x0020, /* space */
    [ 66] = 0xffe5, /* Caps_Lock */
    [ 67] = 0xffbe, /* F1 */  [ 68] = 0xffbf, /* F2 */  [ 69] = 0xffc0, /* F3 */
    [ 70] = 0xffc1, /* F4 */  [ 71] = 0xffc2, /* F5 */  [ 72] = 0xffc3, /* F6 */
    [ 73] = 0xffc4, /* F7 */  [ 74] = 0xffc5, /* F8 */  [ 75] = 0xffc6, /* F9 */
    [ 76] = 0xffc7, /* F10 */
    [ 77] = 0xff7f, /* Num_Lock */
    [ 78] = 0xff14, /* Scroll_Lock */
    [ 95] = 0xffc8, /* F11 */  [ 96] = 0xffc9, /* F12 */
    [105] = 0xffe4, /* Control_R */
    [108] = 0xffea, /* Alt_R */
    [110] = 0xff50, /* Home */
    [111] = 0xff52, /* Up */
    [112] = 0xff55, /* Page_Up */
    [113] = 0xff51, /* Left */
    [114] = 0xff53, /* Right */
    [115] = 0xff57, /* End */
    [116] = 0xff54, /* Down */
    [117] = 0xff56, /* Page_Down */
    [118] = 0xff63, /* Insert */
    [119] = 0xffff, /* Delete */
    [133] = 0xffeb, /* Super_L */
    [134] = 0xffec, /* Super_R */
};

static KeySym keycode_to_keysym(unsigned int keycode) {
    if (keycode < 256) return s_keycode_to_keysym[keycode];
    return 0;
}

XSizeHints* XAllocSizeHints(void) {
    return (XSizeHints*)calloc(1, sizeof(XSizeHints));
}

XWMHints* XAllocWMHints(void) {
    return (XWMHints*)calloc(1, sizeof(XWMHints));
}

XClassHint* XAllocClassHint(void) {
    return (XClassHint*)calloc(1, sizeof(XClassHint));
}

int XChangePointerControl(Display* a, Bool b, Bool c, int d, int e, int f) { return 0; }

int XChangeProperty(Display* a, Window b, Atom c, Atom d, int e, int f,
                    const unsigned char* g, int h) { return 0; }

static int s_xcheck_log = 0;
Bool XCheckIfEvent(Display* a, XEvent *b, Bool (*pred)(Display*,XEvent*,XPointer), XPointer d) {
    if (!s_events_ready) return False;
    x11_drain_events();
    for (int i = 0; i < s_eq_count; i++) {
        int idx = (s_eq_head + i) % EQ_SIZE;
        if (pred(a, &s_eq[idx], d)) {
            int type = *(int*)&s_eq[idx];
            memcpy(b, &s_eq[idx], XEVENT_REAL_SIZE);
            /* Remove element from queue */
            for (int j = i; j < s_eq_count - 1; j++) {
                int dst = (s_eq_head + j) % EQ_SIZE;
                int src = (s_eq_head + j + 1) % EQ_SIZE;
                s_eq[dst] = s_eq[src];
            }
            s_eq_count--;
            if (s_xcheck_log < 67 /* im tired */) {
                // stub_log("stubx11: XCheckIfEvent matched type=%d remaining=%d\n", type, s_eq_count);
                s_xcheck_log++;
            }
            return True;
        }
    }
    return False;
}

int XClearWindow(Display* a, Window b) { return 0; }

int XCloseDisplay(Display* a) {
    stub_log("stubx11: XCloseDisplay\n");
    if (s_x11_fd >= 0) {
        close(s_x11_fd);
        s_x11_fd = -1;
        s_display.fd = 3;
        s_events_ready = 0;
        s_real_window = 0;
    }
    return 0;
}

int XConvertSelection(Display* a, Atom b, Atom c, Atom d, Window e, Time f) { return 0; }

Pixmap XCreateBitmapFromData(Display *dpy, Drawable d, const char *data,
                             unsigned int width, unsigned int height) {
    return 0x1;
}

Colormap XCreateColormap(Display* a, Window b, struct Visual* c, int d) {
    stub_log("stubx11: XCreateColormap -> 0x%lx\n", DUMMY_COLORMAP);
    return DUMMY_COLORMAP;
}

Cursor XCreatePixmapCursor(Display* a, Pixmap b, Pixmap c, XColor* d, XColor* e,
                           unsigned int f, unsigned int g) { return 0x1; }

Cursor XCreateFontCursor(Display* a, unsigned int b) { return 0x1; }

XFontSet XCreateFontSet(Display* a, const char* b, char*** c, int* d, char** e) {
    if (c) *c = NULL;
    if (d) *d = 0;
    if (e) *e = NULL;
    return NULL;
}

GC XCreateGC(Display* a, Drawable b, unsigned long c, XGCValues* d) {
    return (GC)0x1;
}

static int dummy_destroy_image(XImage* img) { free(img->data); free(img); return 0; }
static unsigned long dummy_get_pixel(XImage* img, int x, int y) { return 0; }
static int dummy_put_pixel(XImage* img, int x, int y, unsigned long p) { return 0; }
static int dummy_add_pixel(XImage* img, long v) { return 0; }

XImage* XCreateImage(Display* a, struct Visual* b, unsigned int depth, int format,
                     int offset, char* data, unsigned int width, unsigned int height,
                     int bitmap_pad, int bytes_per_line) {
    XImage* img = (XImage*)calloc(1, sizeof(XImage));
    img->width = width;
    img->height = height;
    img->depth = depth;
    img->data = data;
    img->bits_per_pixel = 32;
    img->bytes_per_line = bytes_per_line ? bytes_per_line : width * 4;
    img->red_mask = 0xFF0000;
    img->green_mask = 0x00FF00;
    img->blue_mask = 0x0000FF;
    img->f.destroy_image = dummy_destroy_image;
    img->f.get_pixel = dummy_get_pixel;
    img->f.put_pixel = dummy_put_pixel;
    img->f.add_pixel = dummy_add_pixel;
    return img;
}

Window XCreateWindow(Display* dpy, Window parent, int x, int y,
                     unsigned int width, unsigned int height,
                     unsigned int border_width, int depth,
                     unsigned int c_class, struct Visual* visual,
                     unsigned long valuemask, XSetWindowAttributes* attributes) {
    stub_log("stubx11: XCreateWindow(%ux%u) -> 0x%lx\n", width, height, DUMMY_WINDOW);
    return DUMMY_WINDOW;
}

int XDefineCursor(Display* a, Window b, Cursor c) { return 0; }
int XDeleteProperty(Display* a, Window b, Atom c) { return 0; }
int XDestroyWindow(Display* a, Window b) { return 0; }

int XDisplayKeycodes(Display* a, int* b, int* c) {
    if (b) *b = 8;
    if (c) *c = 255;
    return 0;
}

int XDrawRectangle(Display* a, Drawable b, GC c, int d, int e, unsigned int f, unsigned int g) { return 0; }

char* XDisplayName(const char* a) {
    static char name[] = ":0";
    return name;
}

int XDrawString(Display* a, Drawable b, GC c, int d, int e, const char* f, int g) { return 0; }

static int s_xeq_log = 0;
int XEventsQueued(Display* a, int b) {
    if (!s_events_ready) return 0;
    x11_drain_events();
    if (s_xeq_log < 5) {
        stub_log("stubx11: XEventsQueued -> %d\n", s_eq_count);
        s_xeq_log++;
    }
    return s_eq_count;
}
int XFillRectangle(Display* a, Drawable b, GC c, int d, int e, unsigned int f, unsigned int g) { return 0; }

Bool XFilterEvent(XEvent *event, Window w) { return False; }

int XFlush(Display* a) { return 0; }

int XFree(void* a) {
    return 0;
}

int XFreeCursor(Display* a, Cursor b) { return 0; }
void XFreeFontSet(Display* a, XFontSet b) {}
int XFreeGC(Display* a, GC b) { return 0; }
int XFreeFont(Display* a, XFontStruct* b) { if (b) free(b); return 0; }
int XFreeModifiermap(XModifierKeymap* a) { if (a) { free(a->modifiermap); free(a); } return 0; }
int XFreePixmap(Display* a, Pixmap b) { return 0; }
void XFreeStringList(char** a) { /* no-op, we don't allocate these */ }

char* XGetAtomName(Display *a, Atom b) {
    /* Return a strdup'd name so XFree works */
    char buf[32];
    snprintf(buf, sizeof(buf), "ATOM_%lu", b);
    return strdup(buf);
}

int XGetInputFocus(Display *a, Window *b, int *c) {
    if (b) *b = DUMMY_WINDOW;
    if (c) *c = 0;
    return 0;
}

int XGetErrorDatabaseText(Display* a, const char* b, const char* c,
                          const char* d, char* e, int f) {
    if (e && f > 0) { strncpy(e, d ? d : "", f); e[f-1] = 0; }
    return 0;
}

XModifierKeymap* XGetModifierMapping(Display* a) {
    XModifierKeymap* m = (XModifierKeymap*)calloc(1, sizeof(XModifierKeymap));
    m->max_keypermod = 4;
    m->modifiermap = (KeyCode*)calloc(8 * 4, sizeof(KeyCode));
    return m;
}

int XGetPointerControl(Display* a, int* b, int* c, int* d) {
    if (b) *b = 2; if (c) *c = 1; if (d) *d = 4;
    return 0;
}

Window XGetSelectionOwner(Display* a, Atom b) { return None; }

XVisualInfo* XGetVisualInfo(Display* a, long b, XVisualInfo* c, int* d) {
    XVisualInfo* vi = (XVisualInfo*)calloc(1, sizeof(XVisualInfo));
    vi->visual = &s_visual;
    vi->visualid = s_visual.visualid;
    vi->depth = 24;
    vi->c_class = TrueColor;
    vi->red_mask = 0xFF0000;
    vi->green_mask = 0x00FF00;
    vi->blue_mask = 0x0000FF;
    vi->bits_per_rgb = 8;
    if (d) *d = 1;
    return vi;
}

Status XGetWindowAttributes(Display* a, Window b, XWindowAttributes* c) {
    if (c) {
        memset(c, 0, sizeof(*c));
        c->x = 0; c->y = 0;
        c->width = s_screen.width;
        c->height = s_screen.height;
        c->depth = 24;
        c->visual = &s_visual;
        c->root = 0x1;
        c->c_class = InputOutput;
        c->colormap = DUMMY_COLORMAP;
        c->screen = &s_screen;
    }
    return 1;
}

int XGetWindowProperty(Display* a, Window b, Atom c, long d, long e, Bool f,
                       Atom g, Atom* h, int* i, unsigned long* j,
                       unsigned long *k, unsigned char **l) {
    if (h) *h = None;
    if (i) *i = 0;
    if (j) *j = 0;
    if (k) *k = 0;
    if (l) *l = NULL;
    return 1;
}

XWMHints* XGetWMHints(Display* a, Window b) { return NULL; }

Status XGetWMNormalHints(Display *a, Window b, XSizeHints *c, long *d) {
    if (c) memset(c, 0, sizeof(*c));
    if (d) *d = 0;
    return 0;
}

int XIfEvent(Display* a, XEvent *b, Bool (*c)(Display*,XEvent*,XPointer), XPointer d) {
    /* Block forever — shouldn't be called in our stub scenario */
    return 0;
}

int XGrabKeyboard(Display* a, Window b, Bool c, int d, int e, Time f) { return 0; }
int XGrabPointer(Display* a, Window b, Bool c, unsigned int d, int e, int f,
                 Window g, Cursor h, Time i) { return 0; }
int XGrabServer(Display* a) { return 0; }

Status XIconifyWindow(Display* a, Window b, int c) { return 1; }

KeyCode XKeysymToKeycode(Display* a, KeySym b) {
    for (int i = 0; i < 256; i++) {
        if (s_keycode_to_keysym[i] == b) return (KeyCode)i;
    }
    return 0;
}

char* XKeysymToString(KeySym a) {
    return "unknown";
}

int XInstallColormap(Display* a, Colormap b) { return 0; }

#define MAX_ATOMS 256
static struct { const char* name; Atom id; } s_atoms[MAX_ATOMS];
static int s_atom_count = 0;

Atom XInternAtom(Display* a, const char* b, Bool c) {
    if (!b) return None;
    /* Search existing */
    for (int i = 0; i < s_atom_count; i++) {
        if (strcmp(s_atoms[i].name, b) == 0) return s_atoms[i].id;
    }
    if (c) return None;
    /* Create new */
    if (s_atom_count < MAX_ATOMS) {
        Atom id = 100 + s_atom_count;
        s_atoms[s_atom_count].name = strdup(b);
        s_atoms[s_atom_count].id = id;
        s_atom_count++;
        return id;
    }
    return None;
}

XPixmapFormatValues* XListPixmapFormats(Display* a, int* b) {
    XPixmapFormatValues* pf = (XPixmapFormatValues*)calloc(1, sizeof(XPixmapFormatValues));
    if (b) *b = 0;
    return pf;
}

XFontStruct* XLoadQueryFont(Display* a, const char* b) { return NULL; }

KeySym XLookupKeysym(XKeyEvent* a, int b) {
    if (!a) return 0;
    unsigned int kc = *(unsigned int*)((unsigned char*)a + 84);
    return keycode_to_keysym(kc);
}

int XLookupString(XKeyEvent* a, char* b, int c, KeySym* d, XComposeStatus* e) {
    KeySym ks = 0;
    if (a) {
        unsigned int kc = *(unsigned int*)((unsigned char*)a + 84);
        ks = keycode_to_keysym(kc);
    }
    if (d) *d = ks;
    if (ks >= 0x20 && ks <= 0x7e && b && c > 0) {
        b[0] = (char)ks;
        return 1;
    }
    return 0;
}

int XMapRaised(Display* a, Window b) {
/*    stub_log("stubx11: XMapRaised(0x%lx)\n", b);
    if (s_x11_fd >= 0 && s_real_window == 0) {
        x11_create_and_map(s_screen.width, s_screen.height);
        s_events_ready = 1;
        stub_log("stubx11: events enabled\n");
    }*/
    return 0;
}

Status XMatchVisualInfo(Display* a, int screen, int depth, int c_class, XVisualInfo* vinfo) {
    if (vinfo) {
        memset(vinfo, 0, sizeof(*vinfo));
        vinfo->visual = &s_visual;
        vinfo->visualid = s_visual.visualid;
        vinfo->screen = screen;
        vinfo->depth = depth;
        vinfo->c_class = c_class;
        vinfo->red_mask = 0xFF0000;
        vinfo->green_mask = 0x00FF00;
        vinfo->blue_mask = 0x0000FF;
        vinfo->bits_per_rgb = 8;
    }
    // stub_log("stubx11: XMatchVisualInfo(screen=%d, depth=%d, class=%d) Ok\n",
    //         screen, depth, c_class);
    return 1;
}

int XMissingExtension(Display* a, const char* b) { return 0; }

int XMoveWindow(Display* a, Window b, int c, int d) { return 0; }

Display* XOpenDisplay(const char* display_name) {
    ensure_display_init();
    stub_log("stubx11: XOpenDisplay('%s')\n", display_name ? display_name : ":0");
    if (s_x11_fd < 0) {
        x11_connect();
    }
    return &s_display;
}

/*Status XInitThreads(void) {
    const char msg[] = "stubx11: XInitThreads called\n";
    write(2, msg, sizeof(msg) - 1);
    return 1;
}*/

int XPeekEvent(Display* a, XEvent* b) {
    /* Block — shouldn't be called */
    if (b) memset(b, 0, sizeof(*b));
    return 0;
}

static int s_xpend_log = 0;
int XPending(Display* a) {
    if (!s_events_ready) return 0;
    x11_drain_events();
/*    if (s_xpend_log < 5) {
        stub_log("stubx11: XPending -> %d\n", s_eq_count);
        s_xpend_log++;
    }*/
    return s_eq_count;
}

int XPutImage(Display* a, Drawable b, GC c, XImage* d, int e, int f,
              int g, int h, unsigned int i, unsigned int j) { return 0; }

int XQueryKeymap(Display* a, char b[32]) {
    memset(b, 0, 32);
    return 0;
}

Bool XQueryPointer(Display* a, Window b, Window* c, Window* d,
                   int* e, int* f, int* g, int* h, unsigned int* i) {
    if (c) *c = 0x1;
    if (d) *d = DUMMY_WINDOW;
    if (e) *e = 0; if (f) *f = 0;
    if (g) *g = 0; if (h) *h = 0;
    if (i) *i = 0;
    return True;
}

int XRaiseWindow(Display* a, Window b) { return 0; }
int XReparentWindow(Display* a, Window b, Window c, int d, int e) { return 0; }
int XResetScreenSaver(Display* a) { return 0; }
int XResizeWindow(Display* a, Window b, unsigned int c, unsigned int d) { return 0; }

int XScreenNumberOfScreen(Screen* a) { return 0; }

int XSelectInput(Display* a, Window b, long c) { return 0; }

Status XSendEvent(Display* a, Window b, Bool c, long d, XEvent* e) { return 0; }

static XErrorHandler s_error_handler = NULL;
XErrorHandler XSetErrorHandler(XErrorHandler a) {
    XErrorHandler old = s_error_handler;
    s_error_handler = a;
    return old;
}

int XSetForeground(Display* a, GC b, unsigned long c) { return 0; }

static XIOErrorHandler s_io_error_handler = NULL;
XIOErrorHandler XSetIOErrorHandler(XIOErrorHandler a) {
    XIOErrorHandler old = s_io_error_handler;
    s_io_error_handler = a;
    return old;
}

int XSetInputFocus(Display *a, Window b, int c, Time d) { return 0; }
int XSetSelectionOwner(Display* a, Atom b, Window c, Time d) { return 0; }
int XSetTransientForHint(Display* a, Window b, Window c) { return 0; }
void XSetTextProperty(Display* a, Window b, XTextProperty* c, Atom d) {}
int XSetWindowBackground(Display* a, Window b, unsigned long c) { return 0; }
void XSetWMHints(Display* a, Window b, XWMHints* c) {}
void XSetWMNormalHints(Display* a, Window b, XSizeHints* c) {}
void XSetWMProperties(Display* a, Window b, XTextProperty* c, XTextProperty* d,
                      char** e, int f, XSizeHints* g, XWMHints* h, XClassHint* i) {}
Status XSetWMProtocols(Display* a, Window b, Atom* c, int d) { return 1; }

int XStoreColors(Display* a, Colormap b, XColor* c, int d) { return 0; }
int XStoreName(Display* a, Window b, const char* c) {
    // stub_log("stubx11: XStoreName('%s')\n", c ? c : "(null)");
    return 0;
}

Status XStringListToTextProperty(char** a, int b, XTextProperty* c) {
    if (c) {
        c->value = (unsigned char*)(a && b > 0 ? a[0] : "");
        c->encoding = 31;
        c->format = 8;
        c->nitems = a && b > 0 ? strlen((char*)c->value) : 0;
    }
    return 1;
}

int XSync(Display* a, Bool b) { return 0; }

int XTextExtents(XFontStruct* a, const char* b, int c, int* d, int* e, int* f, XCharStruct* g) {
    if (d) *d = 0; if (e) *e = 0; if (f) *f = 0;
    if (g) memset(g, 0, sizeof(*g));
    return 0;
}

Bool XTranslateCoordinates(Display *a, Window b, Window c, int d, int e,
                           int* f, int* g, Window* h) {
    if (f) *f = d; if (g) *g = e; if (h) *h = None;
    return True;
}

int XUndefineCursor(Display* a, Window b) { return 0; }
int XUngrabKeyboard(Display* a, Time b) { return 0; }
int XUngrabPointer(Display* a, Time b) { return 0; }
int XUngrabServer(Display* a) { return 0; }
int XUninstallColormap(Display* a, Colormap b) { return 0; }
int XUnloadFont(Display* a, Font b) { return 0; }
int XWarpPointer(Display* a, Window b, Window c, int d, int e,
                 unsigned int f, unsigned int g, int h, int i) { return 0; }

int XWindowEvent(Display* a, Window b, long c, XEvent* d) {
    if (d) memset(d, 0, sizeof(*d));
    return 0;
}

Status XWithdrawWindow(Display* a, Window b, int c) { return 1; }

VisualID XVisualIDFromVisual(struct Visual* a) {
    return a ? a->visualid : 0x21;
}

char* XGetDefault(Display* a, const char* b, const char* c) { return NULL; }

Bool XQueryExtension(Display* a, const char* name, int* c, int* d, int* e) {
    stub_log("stubx11: XQueryExtension('%s')\n", name ? name : "(null)");
    /* Fake GLX support */
    if (name && strcmp(name, "GLX") == 0) {
        if (c) *c = 152;
        if (d) *d = 0;
        if (e) *e = 0;
        return True;
    }
    return False;
}

char* XDisplayString(Display* a) { return ":0"; }

int XGetErrorText(Display* a, int b, char* c, int d) {
    if (c && d > 0) { snprintf(c, d, "Error %d", b); }
    return 0;
}


void _XEatData(Display* a, unsigned long b) {}
void _XFlush(Display* a) {}
void _XFlushGCCache(Display* a, GC b) {}
int _XRead(Display* a, char* b, long c) { return 0; }
void _XReadPad(Display* a, char* b, long c) {}
void _XSend(Display* a, const char* b, long c) {}
Status _XReply(Display* a, xReply* b, int c, Bool d) { return 0; }
unsigned long _XSetLastRequestRead(Display* a, xGenericReply* b) { return 0; }
typedef void (*_XLockMutexProc)(void*);
typedef void (*_XUnlockMutexProc)(void*);
_XLockMutexProc _XLockMutex_fn = NULL;
_XUnlockMutexProc _XUnlockMutex_fn = NULL;
void* _Xglobal_lock = NULL;
typedef void* (*XESetCloseDisplayProc)(Display*, int, void*);
typedef void* (*XESetCreateGCProc)(Display*, int, void*);
typedef void* (*XESetCopyGCProc)(Display*, int, void*);
typedef void* (*XESetFreeGCProc)(Display*, int, void*);
typedef void* (*XESetCreateFontProc)(Display*, int, void*);
typedef void* (*XESetFreeFontProc)(Display*, int, void*);
typedef void* (*XESetFlushGCProc)(Display*, int, void*);
typedef void* (*XESetErrorProc)(Display*, int, void*);
typedef void* (*XESetErrorStringProc)(Display*, int, void*);

void* XESetCloseDisplay(Display* a, int b, void* c) { return NULL; }
void* XESetCreateGC(Display* a, int b, void* c) { return NULL; }
void* XESetCopyGC(Display* a, int b, void* c) { return NULL; }
void* XESetFreeGC(Display* a, int b, void* c) { return NULL; }
void* XESetCreateFont(Display* a, int b, void* c) { return NULL; }
void* XESetFreeFont(Display* a, int b, void* c) { return NULL; }
void* XESetFlushGC(Display* a, int b, void* c) { return NULL; }
void* XESetError(Display* a, int b, void* c) { return NULL; }
void* XESetErrorString(Display* a, int b, void* c) { return NULL; }
void _XInitImageFuncPtrs(XImage* img) {}
void* _XAllocScratch(Display* a, unsigned long b) {
    static char scratch[4096];
    return scratch;
}
int _XGetBitsPerPixel(Display* a, int depth) {
    return (depth <= 8) ? 8 : (depth <= 16) ? 16 : 32;
}
void* _XGetRequest(Display* a, int type, unsigned long len) { return NULL; }
int _XGetScanlinePad(Display* a, int depth) { return 32; }
void _XData32(Display* a, const void* b, unsigned int c) {}
int _XRead32(Display* a, void* b, long c) { return 0; }
void* _XVIDtoVisual(Display* a, VisualID id) {
    if (id == s_visual.visualid) return &s_visual;
    return NULL;
}
void _XEatDataWords(Display* a, unsigned long b) {}
void* XAddExtension(Display* a) { return NULL; }
void* XInitExtension(Display* a, const char* name) { return NULL; }
void* XFindOnExtensionList(void** a, int b) { return NULL; }
void* XextCreateExtension(void) { return NULL; }
int XextAddDisplay(void* extinfo, Display* dpy, const char* name,
                   void* hooks, int nevents, void* data) { return 0; }
int XextRemoveDisplay(void* extinfo, Display* dpy) { return 0; }
void* XextFindDisplay(void* extinfo, Display* dpy) { return NULL; }
typedef int (*XSyncProc)(Display*);
static int dummy_sync_after(Display* d) { return 0; }
XSyncProc XSynchronize(Display* a, Bool b) { return dummy_sync_after; }
typedef Bool (*WireToEventProc)(Display*, void*, void*);
typedef Status (*EventToWireProc)(Display*, void*, void*);
WireToEventProc XESetWireToEvent(Display* a, int b, WireToEventProc c) { return NULL; }
EventToWireProc XESetEventToWire(Display* a, int b, EventToWireProc c) { return NULL; }

void XRefreshKeyboardMapping(XMappingEvent *a) {}

int XQueryTree(Display* a, Window b, Window* c, Window* d, Window** e, unsigned int* f) {
    if (c) *c = 0x1; /* root */
    if (d) *d = 0x1; /* parent */
    if (e) *e = NULL;
    if (f) *f = 0;
    return 1;
}

Bool XSupportsLocale(void) { return True; }

Status XmbTextListToTextProperty(Display* a, char** b, int c, XICCEncodingStyle d, XTextProperty* e) {
    if (e) {
        e->value = (unsigned char*)(b && c > 0 ? b[0] : "");
        e->encoding = 31;
        e->format = 8;
        e->nitems = b && c > 0 ? strlen((char*)e->value) : 0;
    }
    return 0;
}
Bool XkbQueryExtension(Display* a, int *b, int *c, int *d, int *e, int *f) { return False; }
KeySym XkbKeycodeToKeysym(Display* a, unsigned int b, int c, int d) { return keycode_to_keysym(b); }
Status XkbGetState(Display* a, unsigned int b, XkbStatePtr c) { return 0; }
Status XkbGetUpdatedMap(Display* a, unsigned int b, XkbDescPtr c) { return 0; }
XkbDescPtr XkbGetMap(Display* a, unsigned int b, unsigned int c) { return NULL; }
void XkbFreeClientMap(XkbDescPtr a, unsigned int b, Bool c) {}
void XkbFreeKeyboard(XkbDescPtr a, unsigned int b, Bool c) {}
Bool XkbSetDetectableAutoRepeat(Display* a, Bool b, Bool* c) { if (c) *c = b; return True; }
KeySym XKeycodeToKeysym(Display* a, unsigned int b, int c) { return keycode_to_keysym(b); }
PointerBarrier XFixesCreatePointerBarrier(Display* a, Window b, int c, int d, int e, int f,
                                          int g, int h, int *i) { return 0; }
void XFixesDestroyPointerBarrier(Display* a, PointerBarrier b) {}
int XIBarrierReleasePointer(Display* a, int b, PointerBarrier c, BarrierEventID d) { return 0; }
Status XFixesQueryVersion(Display* a, int* b, int* c) {
    if (b) *b = 0; if (c) *c = 0;
    return 0;
}
Status XFixesSelectSelectionInput(Display* a, Window b, Atom c, unsigned long d) { return 0; }
Bool XGetEventData(Display* a, XGenericEventCookie* b) { return False; }
void XFreeEventData(Display* a, XGenericEventCookie* b) {}
int Xutf8TextListToTextProperty(Display* a, char** b, int c, XICCEncodingStyle d, XTextProperty* e) {
    return XmbTextListToTextProperty(a, b, c, d, e);
}
int Xutf8LookupString(XIC a, XKeyPressedEvent* b, char* c, int d, KeySym* e, Status* f) {
    if (e) *e = 0;
    if (f) *f = 0;
    return 0;
}
void XDestroyIC(XIC a) {}
void XSetICFocus(XIC a) {}
void XUnsetICFocus(XIC a) {}
XIM XOpenIM(Display* a, struct XrmHashBucketRec* b, char* c, char* d) {
    stub_log("stubx11: XOpenIM is NULL\n");
    return NULL;
}
Status XCloseIM(XIM a) { return 0; }
void Xutf8DrawString(Display *a, Drawable b, XFontSet c, GC d, int e, int f, const char *g, int h) {}
int Xutf8TextExtents(XFontSet a, const char* b, int c, XRectangle* d, XRectangle* e) {
    if (d) memset(d, 0, sizeof(*d));
    if (e) memset(e, 0, sizeof(*e));
    return 0;
}
char* XSetLocaleModifiers(const char *a) {
    static char empty[] = "";
    return empty;
}
char* Xutf8ResetIC(XIC a) { return NULL; }
XIC XCreateIC(XIM im, ...) { return NULL; }
char* XGetICValues(XIC ic, ...) { return NULL; }
Bool XShmQueryExtension(Display* a) { return False; }
Bool XShmQueryVersion(Display* a, int* b, int* c, Bool* d) { return False; }
int XMapWindow(Display* a, Window b) {
    if (s_x11_fd >= 0 && s_real_window == 0) {
        x11_create_and_map(s_screen.width, s_screen.height);
        s_events_ready = 1;
        // stub_log("stubx11: events enabled (via XMapWindow)\n");
    }
    return 0;
}
int XUnmapWindow(Display* a, Window b) { return 0; }
int XMoveResizeWindow(Display* a, Window b, int c, int d, unsigned int e, unsigned int f) { return 0; }
char** XListExtensions(Display* a, int* b) {
    /* Return GLX in the list */
    char** list = (char**)malloc(sizeof(char*) * 2);
    list[0] = strdup("GLX");
    list[1] = NULL;
    if (b) *b = 1;
    return list;
}
int XFreeExtensionList(char** a) {
    if (a) { free(a[0]); free(a); }
    return 0;
}

Window XRootWindow(Display* dpy, int screen) { return 0x1; }
Window XDefaultRootWindow(Display* dpy) { return 0x1; }
int XDefaultScreen(Display* dpy) { return 0; }
int XDefaultDepth(Display* dpy, int screen) { return 24; }
struct Visual* XDefaultVisual(Display* dpy, int screen) { return &s_visual; }
Colormap XDefaultColormap(Display* dpy, int screen) { return DUMMY_COLORMAP; }
int XDisplayWidth(Display* dpy, int screen) { return s_screen.width; }
int XDisplayHeight(Display* dpy, int screen) { return s_screen.height; }
int XDisplayWidthMM(Display* dpy, int screen) { return s_screen.mwidth; }
int XDisplayHeightMM(Display* dpy, int screen) { return s_screen.mheight; }
int XConnectionNumber(Display* dpy) { return dpy ? dpy->fd : 3; }
Screen* XScreenOfDisplay(Display* dpy, int screen) { return &s_screen; }
Screen* XDefaultScreenOfDisplay(Display* dpy) { return &s_screen; }
int XScreenCount(Display* dpy) { return 1; }
unsigned long XBlackPixel(Display* dpy, int screen) { return 0; }
unsigned long XWhitePixel(Display* dpy, int screen) { return 0xFFFFFF; }
int XWidthOfScreen(Screen* s) { return s ? s->width : 1280; }
int XHeightOfScreen(Screen* s) { return s ? s->height : 1024; }

void* XdbeGetVisualInfo(Display* a, Drawable* b, int* c, int* d) { return NULL; }
void XdbeFreeVisualInfo(void* a) {}
int XAutoRepeatOn(Display* a) { return 0; }
int XAutoRepeatOff(Display* a) { return 0; }
typedef struct {
    int key_click_percent;
    int bell_percent;
    unsigned int bell_pitch;
    unsigned int bell_duration;
    unsigned long led_mask;
    int global_auto_repeat;
    char auto_repeats[32];
} XKeyboardState;

int XGetKeyboardControl(Display* dpy, XKeyboardState* state) {
    if (state) memset(state, 0, sizeof(*state));
    return 1;
}

// finally done