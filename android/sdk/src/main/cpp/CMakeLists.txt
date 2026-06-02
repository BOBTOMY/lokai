cmake_minimum_required(VERSION 3.22.1)

project("lokai")

# 设置llama.cpp路径
set(LLAMA_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

# 收集llama.cpp源文件
file(GLOB LLAMA_CPP_SOURCES
    "${LLAMA_CPP_DIR}/llama.cpp"
    "${LLAMA_CPP_DIR}/common/common.cpp"
    "${LLAMA_CPP_DIR}/ggml.c"
    "${LLAMA_CPP_DIR}/ggml-alloc.c"
    "${LLAMA_CPP_DIR}/ggml-backend.c"
    "${LLAMA_CPP_DIR}/ggml-quants.c"
    "${LLAMA_CPP_DIR}/ggml-cuda.cu"
    "${LLAMA_CPP_DIR}/ggml-metal.m"
    "${LLAMA_CPP_DIR}/sampling.cpp"
)

# 如果上面的源文件不存在，尝试使用glob模式
file(GLOB LLAMA_CPP_SOURCES_FALLBACK
    "${LLAMA_CPP_DIR}/*.cpp"
    "${LLAMA_CPP_DIR}/*.c"
)

# 使用glob匹配所有源文件
file(GLOB_RECURSE LLAMA_CPP_ALL_SOURCES
    "${LLAMA_CPP_DIR}/*.cpp"
    "${LLAMA_CPP_DIR}/*.c"
    "${LLAMA_CPP_DIR}/*.cc"
)

# 排除examples和tests目录
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
    ${LLAMA_CPP_DIR}
    ${LLAMA_CPP_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# 链接Android标准库
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

# 编译配置
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
    -DGGML_USE_CPU
)

# Android特定配置
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
