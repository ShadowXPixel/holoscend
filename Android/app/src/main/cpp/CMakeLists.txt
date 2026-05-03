cmake_minimum_required(VERSION 3.22.1)

project("chat")

# llama.cpp options
set(BUILD_SHARED_LIBS OFF CACHE BOOL "build shared libraries" FORCE)
set(LLAMA_BUILD_COMMON ON CACHE BOOL "llama: build common" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "llama: build tests" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "llama: build examples" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "llama: build server" FORCE)

add_subdirectory(llama.cpp)

add_library(
    native-lib
    SHARED
    native-lib.cpp
)

find_library(
    log-lib
    log
)

target_include_directories(
    native-lib
    PRIVATE
    llama.cpp/include
    llama.cpp/common
)

target_link_libraries(
    native-lib
    llama
    llama-common
    ${log-lib}
)
