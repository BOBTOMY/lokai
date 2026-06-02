cmake_minimum_required(VERSION 3.22.1)

project("lokai")

set(LLAMA_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

file(GLOB_RECURSE LLAMA_CPP_ALL_SOURCES
    "${LLAMA_CPP_DIR}/src/*.cpp"
    "${LLAMA_CPP_DIR}/src/*.c"
    "${LLAMA_CPP_DIR}/common/*.cpp"
    "${LLAMA_CPP_DIR}/common/*.c"
)

list(FILTER LLAMA_CPP_ALL_SOURCES EXCLUDE REGEX ".*/examples/.*")
list(FILTER LLAMA_CPP_ALL_SOURCES EXCLUDE REGEX ".*/tests/.*")
list(FILTER LLAMA_CPP_ALL_SOURCES EXCLUDE REGEX ".*/ggml-cuda.cu")
list(FILTER LLAMA_CPP_ALL_SOURCES EXCLUDE REGEX ".*/ggml-metal.m")

add_library(
    lokai-native
    SHARED
    ${LLAMA_CPP_ALL_SOURCES}
    native-lib.cpp
)

target_include_directories(
    lokai-native
    PRIVATE
    ${LLAMA_CPP_DIR}/src
    ${LLAMA_CPP_DIR}/include
    ${LLAMA_CPP_DIR}/common
    ${CMAKE_CURRENT_SOURCE_DIR}
)

find_library(
    log-lib
    log
)

find_library(
    android-lib
    android
)

target_link_libraries(
    lokai-native
    ${log-lib}
    ${android-lib}
)

set_target_properties(
    lokai-native
    PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED YES
    CXX_EXTENSIONS NO
)

target_compile_options(
    lokai-native
    PRIVATE
    -O3
    -DNDEBUG
    -fPIC
)

if(ANDROID)
    target_link_libraries(lokai-native log)
    target_compile_options(lokai-native PRIVATE
        -ffunction-sections
        -fdata-sections
    )
    target_link_options(lokai-native PRIVATE
        -Wl,--gc-sections
    )
endif()
