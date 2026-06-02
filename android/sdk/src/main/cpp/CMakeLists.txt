cmake_minimum_required(VERSION 3.22.1)

project("lokai")

add_library(
    lokai-native
    SHARED
    llama.cpp/llama.cpp
    llama.cpp/common.cpp
    llama.cpp/ggml.cpp
    llama.cpp/ggml-alloc.cpp
    llama.cpp/k_quants.cpp
    llama.cpp/k_quants_merged.cpp
    llama.cpp/k_matmul.cpp
    llama.cpp/rope.cpp
    llama.cpp/sampler.cpp
    llama.cpp/tokenizer.cpp
    llama.cpp/unicode.cpp
    native-lib.cpp
)

target_include_directories(
    lokai-native
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}
)

find_library(
    log-lib
    log
)

target_link_libraries(
    lokai-native
    ${log-lib}
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
