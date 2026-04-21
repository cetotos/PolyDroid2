/*
 * dbus stub for FMOD init, without it, FMOD doesn't work
 */

#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

typedef int dbus_bool_t;
typedef unsigned int dbus_uint32_t;
typedef int dbus_int32_t;

typedef struct DBusError {
    const char *name;
    const char *message;
    unsigned int dummy1:1;
    unsigned int dummy2:1;
    unsigned int dummy3:1;
    unsigned int dummy4:1;
    unsigned int dummy5:1;
    void *padding1;
} DBusError;

typedef struct DBusConnection { int dummy; } DBusConnection;
typedef struct DBusMessage { int dummy; } DBusMessage;
typedef struct DBusMessageIter { char _p[16]; int _d[11]; } DBusMessageIter;
typedef struct DBusPendingCall { int dummy; } DBusPendingCall;
typedef struct DBusWatch { int dummy; } DBusWatch;
typedef struct DBusTimeout { int dummy; } DBusTimeout;
typedef struct DBusObjectPathVTable { void *fns[8]; } DBusObjectPathVTable;

typedef enum DBusBusType {
    DBUS_BUS_SESSION,
    DBUS_BUS_SYSTEM,
    DBUS_BUS_STARTER
} DBusBusType;

dbus_bool_t dbus_threads_init_default(void) { return 1; }
void dbus_shutdown(void) { }

void dbus_error_init(DBusError *err) {
    if (err) memset(err, 0, sizeof(*err));
}
void dbus_error_free(DBusError *err) {
    if (err) memset(err, 0, sizeof(*err));
}
dbus_bool_t dbus_error_is_set(const DBusError *err) {
    (void)err;
    return 0;
}

DBusConnection *dbus_bus_get(DBusBusType type, DBusError *err) {
    (void)type;
    if (err) memset(err, 0, sizeof(*err));
    return NULL;
}
DBusConnection *dbus_bus_get_private(DBusBusType type, DBusError *err) {
    (void)type;
    if (err) memset(err, 0, sizeof(*err));
    return NULL;
}
dbus_bool_t dbus_bus_register(DBusConnection *c, DBusError *err) {
    (void)c;
    if (err) memset(err, 0, sizeof(*err));
    return 0;
}
void dbus_bus_add_match(DBusConnection *c, const char *rule, DBusError *err) {
    (void)c; (void)rule;
    if (err) memset(err, 0, sizeof(*err));
}
dbus_uint32_t dbus_bus_request_name(DBusConnection *c, const char *name, unsigned int flags, DBusError *err) {
    (void)c; (void)name; (void)flags;
    if (err) memset(err, 0, sizeof(*err));
    return 0;
}

DBusConnection *dbus_connection_open_private(const char *address, DBusError *err) {
    (void)address;
    if (err) memset(err, 0, sizeof(*err));
    return NULL;
}
void dbus_connection_close(DBusConnection *c) { (void)c; }
void dbus_connection_unref(DBusConnection *c) { (void)c; }
DBusConnection *dbus_connection_ref(DBusConnection *c) { return c; }
dbus_bool_t dbus_connection_get_is_connected(DBusConnection *c) { (void)c; return 0; }
void dbus_connection_set_exit_on_disconnect(DBusConnection *c, dbus_bool_t b) { (void)c; (void)b; }
void dbus_connection_flush(DBusConnection *c) { (void)c; }
dbus_bool_t dbus_connection_read_write(DBusConnection *c, int timeout_ms) { (void)c; (void)timeout_ms; return 0; }
dbus_bool_t dbus_connection_read_write_dispatch(DBusConnection *c, int timeout_ms) { (void)c; (void)timeout_ms; return 0; }
int dbus_connection_dispatch(DBusConnection *c) { (void)c; return 0; /* DBUS_DISPATCH_COMPLETE */ }

typedef dbus_bool_t (*DBusHandleMessageFunction)(DBusConnection*, DBusMessage*, void*);
typedef void (*DBusFreeFunction)(void*);
dbus_bool_t dbus_connection_add_filter(DBusConnection *c, DBusHandleMessageFunction f, void *ud, DBusFreeFunction ff) {
    (void)c; (void)f; (void)ud; (void)ff;
    return 1;
}
void dbus_connection_remove_filter(DBusConnection *c, DBusHandleMessageFunction f, void *ud) { (void)c; (void)f; (void)ud; }

dbus_bool_t dbus_connection_try_register_object_path(DBusConnection *c, const char *path, const DBusObjectPathVTable *vt, void *ud, DBusError *err) {
    (void)c; (void)path; (void)vt; (void)ud;
    if (err) memset(err, 0, sizeof(*err));
    return 1;
}
dbus_bool_t dbus_connection_register_object_path(DBusConnection *c, const char *path, const DBusObjectPathVTable *vt, void *ud) {
    (void)c; (void)path; (void)vt; (void)ud;
    return 1;
}
dbus_bool_t dbus_connection_unregister_object_path(DBusConnection *c, const char *path) { (void)c; (void)path; return 1; }

dbus_bool_t dbus_connection_send(DBusConnection *c, DBusMessage *m, dbus_uint32_t *serial) {
    (void)c; (void)m;
    if (serial) *serial = 0;
    return 0;
}
DBusMessage *dbus_connection_send_with_reply_and_block(DBusConnection *c, DBusMessage *m, int timeout, DBusError *err) {
    (void)c; (void)m; (void)timeout;
    if (err) memset(err, 0, sizeof(*err));
    return NULL;
}
dbus_bool_t dbus_connection_send_with_reply(DBusConnection *c, DBusMessage *m, DBusPendingCall **pc, int timeout) {
    (void)c; (void)m; (void)timeout;
    if (pc) *pc = NULL;
    return 0;
}

typedef dbus_bool_t (*DBusAddWatchFunction)(DBusWatch*, void*);
typedef void (*DBusRemoveWatchFunction)(DBusWatch*, void*);
typedef void (*DBusWatchToggledFunction)(DBusWatch*, void*);
typedef dbus_bool_t (*DBusAddTimeoutFunction)(DBusTimeout*, void*);
typedef void (*DBusRemoveTimeoutFunction)(DBusTimeout*, void*);
typedef void (*DBusTimeoutToggledFunction)(DBusTimeout*, void*);

dbus_bool_t dbus_connection_set_watch_functions(DBusConnection *c, DBusAddWatchFunction a, DBusRemoveWatchFunction r, DBusWatchToggledFunction t, void *ud, DBusFreeFunction ff) {
    (void)c; (void)a; (void)r; (void)t; (void)ud; (void)ff;
    return 1;
}
dbus_bool_t dbus_connection_set_timeout_functions(DBusConnection *c, DBusAddTimeoutFunction a, DBusRemoveTimeoutFunction r, DBusTimeoutToggledFunction t, void *ud, DBusFreeFunction ff) {
    (void)c; (void)a; (void)r; (void)t; (void)ud; (void)ff;
    return 1;
}

DBusMessage *dbus_message_new_method_call(const char *dest, const char *path, const char *iface, const char *method) {
    (void)dest; (void)path; (void)iface; (void)method;
    return NULL;
}
DBusMessage *dbus_message_new_signal(const char *path, const char *iface, const char *name) {
    (void)path; (void)iface; (void)name;
    return NULL;
}
void dbus_message_unref(DBusMessage *m) { (void)m; }
DBusMessage *dbus_message_ref(DBusMessage *m) { return m; }
dbus_bool_t dbus_message_is_signal(DBusMessage *m, const char *iface, const char *name) {
    (void)m; (void)iface; (void)name;
    return 0;
}
dbus_bool_t dbus_message_is_method_call(DBusMessage *m, const char *iface, const char *name) {
    (void)m; (void)iface; (void)name;
    return 0;
}
int dbus_message_get_type(DBusMessage *m) { (void)m; return 0; }
const char *dbus_message_get_path(DBusMessage *m) { (void)m; return NULL; }
const char *dbus_message_get_interface(DBusMessage *m) { (void)m; return NULL; }
const char *dbus_message_get_member(DBusMessage *m) { (void)m; return NULL; }
const char *dbus_message_get_sender(DBusMessage *m) { (void)m; return NULL; }

dbus_bool_t dbus_message_append_args(DBusMessage *m, int first_arg_type, ...) {
    (void)m; (void)first_arg_type;
    return 1;
}
dbus_bool_t dbus_message_append_args_valist(DBusMessage *m, int first_arg_type, void *va) {
    (void)m; (void)first_arg_type; (void)va;
    return 1;
}
dbus_bool_t dbus_message_get_args(DBusMessage *m, DBusError *err, int first_arg_type, ...) {
    (void)m; (void)first_arg_type;
    if (err) memset(err, 0, sizeof(*err));
    return 0;
}
dbus_bool_t dbus_message_get_args_valist(DBusMessage *m, DBusError *err, int first_arg_type, void *va) {
    (void)m; (void)first_arg_type; (void)va;
    if (err) memset(err, 0, sizeof(*err));
    return 0;
}

dbus_bool_t dbus_message_iter_init(DBusMessage *m, DBusMessageIter *it) {
    (void)m;
    if (it) memset(it, 0, sizeof(*it));
    return 0;
}
void dbus_message_iter_init_append(DBusMessage *m, DBusMessageIter *it) {
    (void)m;
    if (it) memset(it, 0, sizeof(*it));
}
dbus_bool_t dbus_message_iter_append_basic(DBusMessageIter *it, int type, const void *v) {
    (void)it; (void)type; (void)v;
    return 1;
}
dbus_bool_t dbus_message_iter_open_container(DBusMessageIter *it, int type, const char *sig, DBusMessageIter *sub) {
    (void)it; (void)type; (void)sig;
    if (sub) memset(sub, 0, sizeof(*sub));
    return 1;
}
dbus_bool_t dbus_message_iter_close_container(DBusMessageIter *it, DBusMessageIter *sub) {
    (void)it; (void)sub;
    return 1;
}
int dbus_message_iter_get_arg_type(DBusMessageIter *it) { (void)it; return 0; /* DBUS_TYPE_INVALID */ }
int dbus_message_iter_get_element_type(DBusMessageIter *it) { (void)it; return 0; }
void dbus_message_iter_get_basic(DBusMessageIter *it, void *v) { (void)it; (void)v; }
void dbus_message_iter_recurse(DBusMessageIter *it, DBusMessageIter *sub) {
    (void)it;
    if (sub) memset(sub, 0, sizeof(*sub));
}
dbus_bool_t dbus_message_iter_next(DBusMessageIter *it) { (void)it; return 0; }
int dbus_message_iter_get_element_count(DBusMessageIter *it) { (void)it; return 0; }

void dbus_free(void *p) { free(p); }
void dbus_free_string_array(char **arr) {
    if (!arr) return;
    for (char **p = arr; *p; p++) free(*p);
    free(arr);
}
void *dbus_malloc(size_t n) { return malloc(n); }
void *dbus_malloc0(size_t n) { return calloc(1, n); }
void *dbus_realloc(void *p, size_t n) { return realloc(p, n); }

const char *dbus_get_local_machine_id(void) {
    static const char id[] = "polydroid00000000000000000000000";
    return id;
}
void dbus_connection_setup_with_g_main(DBusConnection *c, void *ctx) { (void)c; (void)ctx; }

void dbus_pending_call_unref(DBusPendingCall *pc) { (void)pc; }
DBusPendingCall *dbus_pending_call_ref(DBusPendingCall *pc) { return pc; }
dbus_bool_t dbus_pending_call_get_completed(DBusPendingCall *pc) { (void)pc; return 1; }
DBusMessage *dbus_pending_call_steal_reply(DBusPendingCall *pc) { (void)pc; return NULL; }
void dbus_pending_call_block(DBusPendingCall *pc) { (void)pc; }
void dbus_pending_call_cancel(DBusPendingCall *pc) { (void)pc; }

int dbus_watch_get_unix_fd(DBusWatch *w) { (void)w; return -1; }
int dbus_watch_get_socket(DBusWatch *w) { (void)w; return -1; }
unsigned int dbus_watch_get_flags(DBusWatch *w) { (void)w; return 0; }
void *dbus_watch_get_data(DBusWatch *w) { (void)w; return NULL; }
void dbus_watch_set_data(DBusWatch *w, void *d, DBusFreeFunction f) { (void)w; (void)d; (void)f; }
dbus_bool_t dbus_watch_handle(DBusWatch *w, unsigned int flags) { (void)w; (void)flags; return 1; }
dbus_bool_t dbus_watch_get_enabled(DBusWatch *w) { (void)w; return 0; }

int dbus_timeout_get_interval(DBusTimeout *t) { (void)t; return 0; }
void *dbus_timeout_get_data(DBusTimeout *t) { (void)t; return NULL; }
void dbus_timeout_set_data(DBusTimeout *t, void *d, DBusFreeFunction f) { (void)t; (void)d; (void)f; }
dbus_bool_t dbus_timeout_handle(DBusTimeout *t) { (void)t; return 1; }
dbus_bool_t dbus_timeout_get_enabled(DBusTimeout *t) { (void)t; return 0; }
