/*
 * libXi.so.6 stub for Godot/2.0
 */

#include <stddef.h>
#include <stdio.h>

typedef int Bool;
typedef int Status;
typedef unsigned long XID;
typedef unsigned long Time;
typedef unsigned long Atom;
typedef unsigned long Window;
typedef unsigned long Cursor;
typedef unsigned long PointerBarrier;
typedef unsigned int  BarrierEventID;

#define False   0
#define True    1
#define Success 0

#define XITouchClass    8
#define XIDirectTouch   1
#define XISlavePointer  3
typedef struct { int type; int sourceid; } XIAnyClassInfo;
typedef struct { int type; int sourceid; int mode; int num_touches; } XITouchClassInfo;
typedef struct {
    int deviceid;
    char *name;
    int use;
    int attachment;
    Bool enabled;
    int num_classes;
    XIAnyClassInfo **classes;
} XIDeviceInfo;

int XIQueryVersion(void *dpy, int *major, int *minor) {
    if (major) *major = 2;
    if (minor) *minor = 2;
    return Success;
}
static XITouchClassInfo s_touch_class = { XITouchClass, 2, XIDirectTouch, 10 };
static XIAnyClassInfo *s_touch_classes[1] = { (XIAnyClassInfo *)&s_touch_class };
static char s_touch_name[] = "PolyDroid Touch";
static XIDeviceInfo s_touch_dev[1] = {{ 2, s_touch_name, XISlavePointer, 1, True, 1, s_touch_classes }};

void *XIQueryDevice(void *dpy, int deviceid, int *ndevices) {
    if (ndevices) *ndevices = 1;
    return s_touch_dev;
}
void XIFreeDeviceInfo(void *info) { }
int XIQueryPointer(void *dpy, int deviceid, Window win, Window *root, Window *child,
                   double *root_x, double *root_y, double *win_x, double *win_y,
                   void *buttons, void *modifiers, void *group) {
    if (root) *root = 0;
    if (child) *child = 0;
    if (root_x) *root_x = 0; if (root_y) *root_y = 0;
    if (win_x) *win_x = 0; if (win_y) *win_y = 0;
    return False;
}
int XIWarpPointer(void *dpy, int deviceid, Window src, Window dst,
                  double sx, double sy, unsigned int sw, unsigned int sh,
                  double dx, double dy) { return Success; }
int XIDefineCursor(void *dpy, int deviceid, Window win, Cursor cursor) { return Success; }
int XIUndefineCursor(void *dpy, int deviceid, Window win) { return Success; }

int XIChangeHierarchy(void *dpy, void *changes, int n) { return Success; }
int XISetClientPointer(void *dpy, Window win, int deviceid) { return Success; }
int XIGetClientPointer(void *dpy, Window win, int *deviceid) {
    if (deviceid) *deviceid = 0;
    return False;
}
int XISelectEvents(void *dpy, Window win, void *masks, int num_masks) {
    fprintf(stderr, "stubxi: TRACE XISelectEvents ENTER num_masks=%d\n", num_masks); fflush(stderr);
    fprintf(stderr, "stubxi: TRACE XISelectEvents EXIT\n"); fflush(stderr);
    return Success;
}
void *XIGetSelectedEvents(void *dpy, Window win, int *num_masks) {
    if (num_masks) *num_masks = 0;
    return NULL;
}

int XISetFocus(void *dpy, int deviceid, Window focus, Time time) { return Success; }
int XIGetFocus(void *dpy, int deviceid, Window *focus) {
    if (focus) *focus = 0;
    return Success;
}

int XIGrabDevice(void *dpy, int deviceid, Window grab_window, Time time, Cursor cursor,
                 int grab_mode, int paired_device_mode, int owner_events, void *mask) {
    return Success;
}
int XIUngrabDevice(void *dpy, int deviceid, Time time) { return Success; }
int XIAllowEvents(void *dpy, int deviceid, int event_mode, Time time) { return Success; }
int XIAllowTouchEvents(void *dpy, int deviceid, unsigned int touchid, Window grab_window, int event_mode) { return Success; }

int XIGrabButton(void *dpy, int deviceid, int button, Window grab_window, Cursor cursor,
                 int grab_mode, int paired_device_mode, int owner_events, void *mask,
                 int num_modifiers, void *modifiers) { return Success; }
int XIGrabKeycode(void *dpy, int deviceid, int keycode, Window grab_window,
                  int grab_mode, int paired_device_mode, int owner_events, void *mask,
                  int num_modifiers, void *modifiers) { return Success; }
int XIGrabEnter(void *dpy, int deviceid, Window grab_window, Cursor cursor,
                int grab_mode, int paired_device_mode, int owner_events, void *mask,
                int num_modifiers, void *modifiers) { return Success; }
int XIGrabFocusIn(void *dpy, int deviceid, Window grab_window,
                  int grab_mode, int paired_device_mode, int owner_events, void *mask,
                  int num_modifiers, void *modifiers) { return Success; }
int XIGrabTouchBegin(void *dpy, int deviceid, Window grab_window, int owner_events,
                     void *mask, int num_modifiers, void *modifiers) { return Success; }
int XIUngrabButton(void *dpy, int deviceid, int button, Window grab_window, int num_modifiers, void *modifiers) { return Success; }
int XIUngrabKeycode(void *dpy, int deviceid, int keycode, Window grab_window, int num_modifiers, void *modifiers) { return Success; }
int XIUngrabEnter(void *dpy, int deviceid, Window grab_window, int num_modifiers, void *modifiers) { return Success; }
int XIUngrabFocusIn(void *dpy, int deviceid, Window grab_window, int num_modifiers, void *modifiers) { return Success; }
int XIUngrabTouchBegin(void *dpy, int deviceid, Window grab_window, int num_modifiers, void *modifiers) { return Success; }
Atom *XIListProperties(void *dpy, int deviceid, int *num_props) {
    if (num_props) *num_props = 0;
    return NULL;
}
void XIChangeProperty(void *dpy, int deviceid, Atom prop, Atom type, int fmt, int mode,
                      unsigned char *data, int num_items) { }
void XIDeleteProperty(void *dpy, int deviceid, Atom prop) { }
int  XIGetProperty(void *dpy, int deviceid, Atom prop, long offset, long length,
                   int del, Atom type, Atom *type_ret, int *format_ret,
                   unsigned long *num_items_ret, unsigned long *bytes_after_ret,
                   unsigned char **data_ret) {
    if (type_ret) *type_ret = 0;
    if (format_ret) *format_ret = 0;
    if (num_items_ret) *num_items_ret = 0;
    if (bytes_after_ret) *bytes_after_ret = 0;
    if (data_ret) *data_ret = NULL;
    return Success;
}
void XIBarrierReleasePointers(void *dpy, void *barriers, int num_barriers) { }
void XIBarrierReleasePointer(void *dpy, int deviceid, PointerBarrier barrier, BarrierEventID eventid) { }
