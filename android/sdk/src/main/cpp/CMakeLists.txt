cmake_minimum_required(VERSION 3.22.1)

project("lokai")

set(LLAMA_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

set(GGML_VERSION_MAJOR 0)
set(GGML_VERSION_MINOR 13)
set(GGML_VERSION_PATCH 1)
set(GGML_VERSION "${GGML_VERSION_MAJOR}.${GGML_VERSION_MINOR}.${GGML_VERSION_PATCH}")
set(GGML_COMMIT "unknown")

set(LLAMA_BUILD_NUMBER 0)
set(LLAMA_COMMIT "unknown")
set(LLAMA_BUILD_COMPILER "${CMAKE_C_COMPILER_ID} ${CMAKE_C_COMPILER_VERSION}")
set(LLAMA_BUILD_TARGET "android")

file(GLOB_RECURSE LLAMA_CPP_ALL_SOURCES
    "${LLAMA_CPP_DIR}/src/*.cpp"
    "${LLAMA_CPP_DIR}/src/*.c"
)

list(FILTER LLAMA_CPP_ALL_SOURCES EXCLUDE REGEX ".*/examples/.*")
list(FILTER LLAMA_CPP_ALL_SOURCES EXCLUDE REGEX ".*/tests/.*")
list(FILTER LLAMA_CPP_ALL_SOURCES EXCLUDE REGEX ".*/ggml-cuda.cu")
list(FILTER LLAMA_CPP_ALL_SOURCES EXCLUDE REGEX ".*/ggml-metal.m")

file(GLOB GGML_SOURCES
    "${LLAMA_CPP_DIR}/ggml/src/*.cpp"
    "${LLAMA_CPP_DIR}/ggml/src/*.c"
)

file(GLOB LLAMA_CPP_COMMON_SOURCES
    "${LLAMA_CPP_DIR}/common/common.cpp"
    "${LLAMA_CPP_DIR}/common/console.cpp"
    "${LLAMA_CPP_DIR}/common/debug.cpp"
    "${LLAMA_CPP_DIR}/common/fit.cpp"
    "${LLAMA_CPP_DIR}/common/llguidance.cpp"
    "${LLAMA_CPP_DIR}/common/log.cpp"
    "${LLAMA_CPP_DIR}/common/ngram-cache.cpp"
    "${LLAMA_CPP_DIR}/common/ngram-map.cpp"
    "${LLAMA_CPP_DIR}/common/ngram-mod.cpp"
    "${LLAMA_CPP_DIR}/common/reasoning-budget.cpp"
    "${LLAMA_CPP_DIR}/common/regex-partial.cpp"
    "${LLAMA_CPP_DIR}/common/sampling.cpp"
    "${LLAMA_CPP_DIR}/common/speculative.cpp"
    "${LLAMA_CPP_DIR}/common/unicode.cpp"
)

set(BUILD_INFO_SRC "${CMAKE_CURRENT_BINARY_DIR}/build-info.cpp")
file(WRITE ${BUILD_INFO_SRC}
"// Auto-generated build info
int llama_build_number(void) {
    return ${LLAMA_BUILD_NUMBER};
}

const char * llama_commit(void) {
    return \"${LLAMA_COMMIT}\";
}

const char * llama_compiler(void) {
    return \"${LLAMA_BUILD_COMPILER}\";
}

const char * llama_build_target(void) {
    return \"${LLAMA_BUILD_TARGET}\";
}

const char * llama_build_info(void) {
    return \"build ${LLAMA_BUILD_NUMBER} (${LLAMA_COMMIT}) with ${LLAMA_BUILD_COMPILER} for ${LLAMA_BUILD_TARGET}\";
}

void llama_print_build_info(void) {
    // noop for now
}
")

set(ALL_SOURCES
    ${LLAMA_CPP_ALL_SOURCES}
    ${LLAMA_CPP_COMMON_SOURCES}
    ${GGML_SOURCES}
    ${BUILD_INFO_SRC}
)

add_library(
    lokai-native
    SHARED
    ${ALL_SOURCES}
    native-lib.cpp
)

target_include_directories(
    lokai-native
    PRIVATE
    ${LLAMA_CPP_DIR}/src
    ${LLAMA_CPP_DIR}/include
    ${LLAMA_CPP_DIR}/common
    ${LLAMA_CPP_DIR}/ggml/include
    ${LLAMA_CPP_DIR}/ggml/src
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_BINARY_DIR}
)

target_compile_definitions(lokai-native PRIVATE
    GGML_VERSION="${GGML_VERSION}"
    GGML_COMMIT="${GGML_COMMIT}"
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
