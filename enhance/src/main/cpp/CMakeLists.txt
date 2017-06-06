cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fuse-ld=bfd")
include_directories(./include)
link_directories(.)
add_library(shader SHARED
            shader.cpp)
# Include libraries needed for hello-jni lib
target_link_libraries(shader
                      android
                      log)
