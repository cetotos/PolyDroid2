cmake_minimum_required(VERSION 3.22)
project(polydroid LANGUAGES C)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -Wno-unused-function")
add_library(native_window_jni SHARED
    native_window_jni.c
)

target_link_libraries(native_window_jni
    log
    android
    nativewindow
)
add_library(GL SHARED
    glx.c
)

target_link_libraries(GL
    log
    dl
)
add_library(vulkan_surface_shim SHARED
    vulkan.c
)
set_target_properties(vulkan_surface_shim PROPERTIES
    SUFFIX ".so"
    OUTPUT_NAME "vulkan_surface_shim"
)
target_link_options(vulkan_surface_shim PRIVATE "-Wl,-soname,libvulkan.so.1")

target_link_libraries(vulkan_surface_shim
    dl
    nativewindow
    log
)

add_library(host_syscall_shim SHARED
    host_syscall.c
)
target_link_libraries(host_syscall_shim dl)

add_library(dbus-1 SHARED
    dbus_stub.c
)


