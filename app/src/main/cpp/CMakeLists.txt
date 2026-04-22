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
    glx_shim.c
)

target_link_libraries(GL
    log
    dl
)
add_library(vulkan_surface_shim SHARED
    vulkan_surface_shim.c
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


