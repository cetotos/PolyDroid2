
// glx shim to please Unity because it uses it even when in Vulkan mode

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>

#define TAG "GLXShim"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define GLX_USE_GL            1
#define GLX_BUFFER_SIZE       2
#define GLX_LEVEL             3
#define GLX_RGBA              4
#define GLX_DOUBLEBUFFER      5
#define GLX_STEREO            6
#define GLX_AUX_BUFFERS       7
#define GLX_RED_SIZE          8
#define GLX_GREEN_SIZE        9
#define GLX_BLUE_SIZE         10
#define GLX_ALPHA_SIZE        11
#define GLX_DEPTH_SIZE        12
#define GLX_STENCIL_SIZE      13
#define GLX_VENDOR_            1
#define GLX_VERSION_           2
#define GLX_EXTENSIONS_        3
#define GLX_RENDER_TYPE       0x8011
#define GLX_RGBA_BIT          0x00000001
#define GLX_DRAWABLE_TYPE     0x8010
#define GLX_WINDOW_BIT        0x00000001
#define GLX_X_RENDERABLE      0x8012
#define GLX_FBCONFIG_ID       0x8013
#define GLX_VISUAL_ID_        0x800B
#define GLX_X_VISUAL_TYPE     0x22
#define GLX_TRUE_COLOR        0x8002
#define GLX_CONFIG_CAVEAT     0x20
#define GLX_NONE              0x8000
#define GLX_TRANSPARENT_TYPE  0x23
#define GLX_SAMPLE_BUFFERS    0x186A0
#define GLX_SAMPLES           0x186A1
#define None 0L
#define True 1
#define False 0
#define TrueColor 4

typedef unsigned long XID;
typedef XID Window;
typedef XID Pixmap;
typedef XID Drawable;
typedef XID Colormap;
typedef struct _XDisplay Display;
typedef unsigned long VisualID;

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

struct XVisualInfo {
    struct Visual* visual;
    VisualID visualid;
    int screen;
    int depth;
    int c_class;
    unsigned long red_mask;
    unsigned long green_mask;
    unsigned long blue_mask;
    int colormap_size;
    int bits_per_rgb;
};

static struct Visual s_visual;

typedef struct GLXFBConfigRec {
    int fbconfig_id;
    int visual_id;
    int buffer_size;
    int red_size, green_size, blue_size, alpha_size;
    int depth_size, stencil_size;
    int doublebuffer;
    int render_type;
    int drawable_type;
    int x_renderable;
} *GLXFBConfig;

typedef XID GLXDrawable;
typedef XID GLXWindow;
typedef XID GLXPixmap;
typedef void (*__GLXextFuncPtr)(void);
struct GLXContextRec {
    int width;
    int height;
    Drawable drawable;
};
typedef struct GLXContextRec *GLXContext;
static int screen_width = 1280; // hardcoded but replaced later
static int screen_height = 720; // ^^
static __thread GLXContext current_context = NULL;
static __thread GLXDrawable current_drawable = 0;
static GLXContext last_bound_context = NULL;
static struct XVisualInfo s_visual_info;
static struct GLXFBConfigRec s_fbconfig;
static int s_initialized = 0;

static void ensure_init(void) {
    if (s_initialized) return;
    s_initialized = 1;

    const char* w = getenv("POLYDROID_SCREEN_WIDTH");
    const char* h = getenv("POLYDROID_SCREEN_HEIGHT");
    if (w) screen_width = atoi(w);
    if (h) screen_height = atoi(h);
    LOGI("Screen: %dx%d", screen_width, screen_height);

    memset(&s_visual, 0, sizeof(s_visual));
    s_visual.visualid = 0x21;
    s_visual.c_class = TrueColor;
    s_visual.red_mask = 0xFF0000;
    s_visual.green_mask = 0x00FF00;
    s_visual.blue_mask = 0x0000FF;
    s_visual.bits_per_rgb = 8;
    s_visual.map_entries = 256;

    memset(&s_visual_info, 0, sizeof(s_visual_info));
    s_visual_info.visual = &s_visual;
    s_visual_info.visualid = 0x21;
    s_visual_info.depth = 24;
    s_visual_info.c_class = TrueColor;
    s_visual_info.red_mask = 0xFF0000;
    s_visual_info.green_mask = 0x00FF00;
    s_visual_info.blue_mask = 0x0000FF;
    s_visual_info.bits_per_rgb = 8;

    memset(&s_fbconfig, 0, sizeof(s_fbconfig));
    s_fbconfig.fbconfig_id = 1;
    s_fbconfig.visual_id = 0x21;
    s_fbconfig.buffer_size = 24;
    s_fbconfig.red_size = 8;
    s_fbconfig.green_size = 8;
    s_fbconfig.blue_size = 8;
    s_fbconfig.alpha_size = 0;
    s_fbconfig.depth_size = 24;
    s_fbconfig.stencil_size = 8;
    s_fbconfig.doublebuffer = 1;
    s_fbconfig.render_type = GLX_RGBA_BIT;
    s_fbconfig.drawable_type = GLX_WINDOW_BIT;
    s_fbconfig.x_renderable = 1;
}

struct XVisualInfo* glXChooseVisual(Display* dpy, int screen, int* attribList) {
    ensure_init();
    LOGI("glXChooseVisual");
    return &s_visual_info;
}

GLXContext glXCreateContext(Display* dpy, struct XVisualInfo* vis, GLXContext shareList, int direct) {
    ensure_init();
    LOGI("glXCreateContext(direct=%d)", direct);
    GLXContext ctx = (GLXContext)calloc(1, sizeof(struct GLXContextRec));
    ctx->width = screen_width;
    ctx->height = screen_height;
    LOGI("created dummy GLX context %p (%dx%d)", (void*)ctx, ctx->width, ctx->height);
    return ctx;
}

GLXContext glXCreateNewContext(Display* dpy, GLXFBConfig config, int render_type, GLXContext shareList, int direct) {
    return glXCreateContext(dpy, NULL, shareList, direct);
}

void glXDestroyContext(Display* dpy, GLXContext ctx) {
    if (!ctx) return;
    LOGI("glXDestroyContext %p", (void*)ctx);
    free(ctx);
}

int glXMakeCurrent(Display* dpy, GLXDrawable drawable, GLXContext ctx) {
    ensure_init();
    if (!ctx) {
        current_context = NULL;
        current_drawable = None;
        return True;
    }
    ctx->drawable = drawable;
    current_context = ctx;
    current_drawable = drawable;
    last_bound_context = ctx;
    LOGI("glXMakeCurrent Ok, drawable=0x%lx", drawable);
    return True;
}

int glXMakeContextCurrent(Display* dpy, GLXDrawable draw, GLXDrawable read, GLXContext ctx) {
    return glXMakeCurrent(dpy, draw, ctx);
}

void glXSwapBuffers(Display* dpy, GLXDrawable drawable) {
    // a
}

GLXContext glXGetCurrentContext(void) {
    return current_context ? current_context : last_bound_context;
}

GLXDrawable glXGetCurrentDrawable(void) {
    return current_drawable;
}

Display* glXGetCurrentDisplay(void) {
    return NULL;
}

int glXQueryExtension(Display* dpy, int* errorBase, int* eventBase) {
    if (errorBase) *errorBase = 0;
    if (eventBase) *eventBase = 0;
    return True;
}

int glXQueryVersion(Display* dpy, int* major, int* minor) {
    if (major) *major = 1;
    if (minor) *minor = 4;
    return True;
}

const char* glXQueryExtensionsString(Display* dpy, int screen) {
    return "GLX_ARB_create_context GLX_ARB_create_context_profile "
           "GLX_ARB_get_proc_address GLX_ARB_multisample "
           "GLX_EXT_visual_info GLX_EXT_visual_rating "
           "GLX_EXT_framebuffer_sRGB GLX_SGI_make_current_read";
}

const char* glXGetClientString(Display* dpy, int name) {
    switch (name) {
        case GLX_VENDOR_: return "PolyDroid2";
        case GLX_VERSION_: return "1.4";
        case GLX_EXTENSIONS_: return glXQueryExtensionsString(dpy, 0);
        default: return "";
    }
}

const char* glXQueryServerString(Display* dpy, int screen, int name) {
    return glXGetClientString(dpy, name);
}

int glXGetConfig(Display* dpy, struct XVisualInfo* vis, int attrib, int* value) {
    if (!value) return 1;
    switch (attrib) {
        case GLX_USE_GL: *value = True; break;
        case GLX_BUFFER_SIZE: *value = 32; break;
        case GLX_LEVEL: *value = 0; break;
        case GLX_RGBA: *value = True; break;
        case GLX_DOUBLEBUFFER: *value = True; break;
        case GLX_STEREO: *value = False; break;
        case GLX_AUX_BUFFERS: *value = 0; break;
        case GLX_RED_SIZE: *value = 8; break;
        case GLX_GREEN_SIZE: *value = 8; break;
        case GLX_BLUE_SIZE: *value = 8; break;
        case GLX_ALPHA_SIZE: *value = 8; break;
        case GLX_DEPTH_SIZE: *value = 24; break;
        case GLX_STENCIL_SIZE: *value = 8; break;
        default: *value = 0; break;
    }
    return 0;
}

struct XVisualInfo* glXGetVisualFromFBConfig(Display* dpy, GLXFBConfig config) {
    ensure_init();
    return &s_visual_info;
}

int glXGetFBConfigAttrib(Display* dpy, GLXFBConfig config, int attribute, int* value) {
    if (!value) return 1;
    switch (attribute) {
        case GLX_FBCONFIG_ID: *value = 1; break;
        case GLX_VISUAL_ID_: *value = 1; break;
        case GLX_RENDER_TYPE: *value = GLX_RGBA_BIT; break;
        case GLX_DRAWABLE_TYPE: *value = GLX_WINDOW_BIT; break;
        case GLX_X_RENDERABLE: *value = True; break;
        case GLX_X_VISUAL_TYPE: *value = GLX_TRUE_COLOR; break;
        case GLX_BUFFER_SIZE: *value = 32; break;
        case GLX_RED_SIZE: *value = 8; break;
        case GLX_GREEN_SIZE: *value = 8; break;
        case GLX_BLUE_SIZE: *value = 8; break;
        case GLX_ALPHA_SIZE: *value = 8; break;
        case GLX_DEPTH_SIZE: *value = 24; break;
        case GLX_STENCIL_SIZE: *value = 8; break;
        case GLX_DOUBLEBUFFER: *value = True; break;
        case GLX_STEREO: *value = False; break;
        case GLX_AUX_BUFFERS: *value = 0; break;
        case GLX_LEVEL: *value = 0; break;
        case GLX_CONFIG_CAVEAT: *value = GLX_NONE; break;
        case GLX_TRANSPARENT_TYPE: *value = GLX_NONE; break;
        case GLX_SAMPLE_BUFFERS: *value = 0; break;
        case GLX_SAMPLES: *value = 0; break;
        default: *value = 0; break;
    }
    return 0;
}

static GLXFBConfig s_fbconfig_list[1];

GLXFBConfig* glXChooseFBConfig(Display* dpy, int screen, const int* attrib_list, int* nelements) {
    ensure_init();
    s_fbconfig_list[0] = &s_fbconfig;
    if (nelements) *nelements = 1;
    return s_fbconfig_list;
}

GLXFBConfig* glXGetFBConfigs(Display* dpy, int screen, int* nelements) {
    return glXChooseFBConfig(dpy, screen, NULL, nelements);
}

int glXIsDirect(Display* dpy, GLXContext ctx) { return True; }
void glXWaitGL(void) {}
void glXWaitX(void) {}
void glXSwapIntervalEXT(Display* dpy, GLXDrawable drawable, int interval) {}
int glXSwapIntervalSGI(int interval) { return 0; }
int glXGetSwapIntervalMESA(void) { return 0; }
int glXSwapIntervalMESA(unsigned int interval) { return 0; }

int glXQueryCurrentRendererIntegerMESA(int attribute, unsigned int* value) {
    if (value) *value = 0;
    return 0;
}

int glXQueryDrawable(Display* dpy, GLXDrawable draw, int attribute, unsigned int* value) {
    if (value) *value = 0;
    return 0;
}

GLXWindow glXCreateWindow(Display* dpy, GLXFBConfig config, Window win, const int* attrib_list) {
    return win;
}
void glXDestroyWindow(Display* dpy, GLXWindow win) {}

GLXPixmap glXCreatePixmap(Display* dpy, GLXFBConfig config, Pixmap pixmap, const int* attrib_list) {
    return pixmap;
}
void glXDestroyPixmap(Display* dpy, GLXPixmap pixmap) {}

const unsigned char* glGetString(unsigned int name) {
    LOGI("glGetString(0x%x)", name);
    switch (name) {
        case 0x1F00: return (const unsigned char*)"PolyDroid2";
        case 0x1F01: return (const unsigned char*)"PolyDroid2 very real GL, trust me unity";
        case 0x1F02: return (const unsigned char*)"4.5.0";
        case 0x1F03: return (const unsigned char*)"";
        case 0x8B8C: return (const unsigned char*)"4.50";
        default:     return (const unsigned char*)"";
    }
}

const unsigned char* glGetStringi(unsigned int name, unsigned int index) {
    return (const unsigned char*)"";
}

void glGetIntegerv(unsigned int pname, int* params) {
    if (!params) return;
    switch (pname) {
        case 0x821B: *params = 4; break;
        case 0x821C: *params = 5; break;
        case 0x821D: *params = 0; break;
        default:     *params = 0; break;
    }
}

unsigned int glGetError(void) {
    return 0;
}

void* glXGetProcAddressARB(const unsigned char* name);
GLXContext glXCreateContextAttribsARB(Display* dpy, GLXFBConfig config, GLXContext shareList, int direct, const int* attrib_list);

void* glXGetProcAddress(const unsigned char* name) {
    ensure_init();
    const char* sym = (const char*)name;

    #define CHECK(fn) if (strcmp(sym, #fn) == 0) return (void*)(fn)
    CHECK(glXChooseVisual);
    CHECK(glXCreateContext);
    CHECK(glXCreateNewContext);
    CHECK(glXDestroyContext);
    CHECK(glXMakeCurrent);
    CHECK(glXMakeContextCurrent);
    CHECK(glXSwapBuffers);
    CHECK(glXGetCurrentContext);
    CHECK(glXGetCurrentDrawable);
    CHECK(glXGetCurrentDisplay);
    CHECK(glXQueryExtension);
    CHECK(glXQueryVersion);
    CHECK(glXQueryExtensionsString);
    CHECK(glXGetClientString);
    CHECK(glXQueryServerString);
    CHECK(glXGetConfig);
    CHECK(glXGetVisualFromFBConfig);
    CHECK(glXGetFBConfigAttrib);
    CHECK(glXChooseFBConfig);
    CHECK(glXGetFBConfigs);
    CHECK(glXIsDirect);
    CHECK(glXWaitGL);
    CHECK(glXWaitX);
    CHECK(glXQueryDrawable);
    CHECK(glXGetProcAddress);
    CHECK(glXGetProcAddressARB);
    CHECK(glXCreateContextAttribsARB);
    CHECK(glXSwapIntervalEXT);
    CHECK(glXSwapIntervalSGI);
    CHECK(glXGetSwapIntervalMESA);
    CHECK(glXSwapIntervalMESA);
    CHECK(glXQueryCurrentRendererIntegerMESA);
    CHECK(glXCreateWindow);
    CHECK(glXDestroyWindow);
    CHECK(glXCreatePixmap);
    CHECK(glXDestroyPixmap);
    CHECK(glGetString);
    CHECK(glGetStringi);
    CHECK(glGetIntegerv);
    CHECK(glGetError);
    #undef CHECK

    LOGI("glXGetProcAddress(%s) is NULL", sym);
    return NULL;
}

void* glXGetProcAddressARB(const unsigned char* name) {
    return glXGetProcAddress(name);
}

GLXContext glXCreateContextAttribsARB(Display* dpy, GLXFBConfig config, GLXContext shareList, int direct, const int* attrib_list) {
    LOGI("glXCreateContextAttribsARB");
    return glXCreateContext(dpy, NULL, shareList, direct);
}
