cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fuse-ld=bfd")
message(${CMAKE_CXX_FLAGS})
include_directories(./CL ./)
link_directories(.)
add_library(shader SHARED
            shader.cpp)
# Include libraries needed for hello-jni lib
target_link_libraries(shader
                      android
                      log)

add_library(Engine SHARED
            EnhanceInterface.cpp
            platformCL.cpp
            platformGL.cpp)

target_link_libraries(Engine
                        OpenCL
                        EGL
                        GLESv2
                        shader
                        android
                        log)