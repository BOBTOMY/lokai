# llama.cpp 集成操作指南

## 📋 文档信息

| 项 | 值 |
|---|---|
| 文档版本 | v1.0 |
| 创建日期 | 2026-06-02 |
| 目标读者 | 开发者 |

---

## 🎯 集成目标

将llama.cpp作为Native层依赖集成到LokAI SDK中，实现端侧AI推理能力。

---

## 📦 方案选择

### 方案对比

| 方案 | 优势 | 劣势 | 适用场景 |
|------|------|------|---------|
| **Git Submodule** | 版本管理清晰、方便更新 | 仓库体积大（~50MB） | ✅ 推荐 |
| **预编译库** | 构建快、体积小 | 维护成本高、版本绑定 | 备选 |
| **源码直接拷贝** | 简单直接 | 难更新、版本混乱 | 不推荐 |

### 选择建议

**推荐使用Git Submodule**：
- 版本可追溯
- 方便升级
- 与llama.cpp社区同步

---

## 🚀 操作步骤（Git Submodule方式）

### 步骤1：前置准备

```bash
# 1.1 检查git版本（需要2.25+）
git --version

# 1.2 进入项目目录
cd /Users/litong/ai_project/lokai

# 1.3 初始化git仓库（如果尚未初始化）
git init
git add .
git commit -m "Initial commit: project skeleton"
```

### 步骤2：添加llama.cpp Submodule

```bash
# 2.1 添加submodule
git submodule add https://github.com/ggml-org/llama.cpp.git android/sdk/src/main/cpp/llama.cpp

# 2.2 初始化并更新submodule
git submodule update --init --recursive
```

### 步骤3：选择llama.cpp版本

```bash
# 3.1 进入llama.cpp目录
cd android/sdk/src/main/cpp/llama.cpp

# 3.2 查看可用版本
git tag | tail -20

# 3.3 推荐版本（选择稳定版本）
# 方式A：使用最新稳定版
git checkout master
git pull

# 方式B：使用指定版本
git checkout b3472

# 3.4 回到项目根目录
cd /Users/litong/ai_project/lokai
```

### 步骤4：验证集成

```bash
# 4.1 检查llama.cpp目录
ls android/sdk/src/main/cpp/llama.cpp/

# 应该看到：
# - llama.cpp (主源文件)
# - ggml.c, ggml.h (张量库)
# - common/ (公共工具)
# - include/ (头文件)

# 4.2 检查关键文件存在
test -f android/sdk/src/main/cpp/llama.cpp/llama.cpp && echo "✓ llama.cpp存在"
test -f android/sdk/src/main/cpp/llama.cpp/ggml.c && echo "✓ ggml.c存在"
test -d android/sdk/src/main/cpp/llama.cpp/common && echo "✓ common目录存在"
```

### 步骤5：CMakeLists.txt配置

CMakeLists.txt已配置好，可以根据llama.cpp版本微调。关键配置：

```cmake
# 设置llama.cpp路径
set(LLAMA_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

# 递归收集源文件（排除examples和tests）
file(GLOB_RECURSE LLAMA_CPP_ALL_SOURCES
    "${LLAMA_CPP_DIR}/*.cpp"
    "${LLAMA_CPP_DIR}/*.c"
)
list(FILTER LLAMA_CPP_ALL_SOURCES EXCLUDE REGEX ".*/examples/.*")
list(FILTER LLAMA_CPP_ALL_SOURCES EXCLUDE REGEX ".*/tests/.*")

# 编译选项
target_compile_options(lokai-native PRIVATE
    -O3
    -DNDEBUG
    -fPIC
    -DGGML_USE_CPU
)
```

### 步骤6：编译验证

```bash
# 6.1 同步项目
cd android
./gradlew :sdk:sync

# 6.2 编译Debug版本
./gradlew :sdk:assembleDebug

# 6.3 查看编译输出
ls -lh sdk/build/intermediates/cmake/debug/obj/arm64-v8a/liblokai-native.so
```

---

## ⚙️ 版本选择建议

### 推荐版本

| 版本 | 日期 | 特性 |
|------|------|------|
| `master` | 最新 | 包含最新功能 |
| `b3472` | 较新稳定 | 性能优化、bug修复 |
| `b3000` | 稳定 | 兼容性好 |

### 版本要求

- **最低要求**：b3000+
- **推荐**：b3472+
- **必须支持**：GGUF格式、量化模型

---

## 🔧 常见问题

### Q1: 编译报错"找不到llama.h"

**原因**：include路径配置错误

**解决**：
```cmake
target_include_directories(lokai-native PRIVATE
    ${LLAMA_CPP_DIR}
    ${LLAMA_CPP_DIR}/include  # 部分版本需要
)
```

### Q2: 链接错误"undefined reference"

**原因**：源文件未完整包含

**解决**：
```cmake
# 确保使用glob模式收集所有源文件
file(GLOB_RECURSE LLAMA_CPP_ALL_SOURCES
    "${LLAMA_CPP_DIR}/*.cpp"
    "${LLAMA_CPP_DIR}/*.c"
)
```

### Q3: 编译时间过长

**原因**：编译全部源码耗时

**解决**：
- 仅编译需要的源文件
- 启用ccache
- 使用预编译头

### Q4: APK体积过大

**原因**：包含多个ABI

**解决**：
```gradle
android {
    defaultConfig {
        ndk {
            abiFilters "arm64-v8a"  // 仅保留64位
        }
    }
}
```

---

## 📊 验证清单

集成完成后，请验证以下项目：

- [ ] llama.cpp目录存在且有源码
- [ ] CMakeLists.txt配置正确
- [ ] 编译成功生成liblokai-native.so
- [ ] SO文件大小合理（10-30MB）
- [ ] JNI接口可以调用
- [ ] 模型加载测试通过

---

## 📝 后续工作

集成完成后，还需要：

1. **实现JNI桥接**：完善`native-lib.cpp`的JNI函数实现
2. **编写单元测试**：验证Native层功能
3. **性能测试**：确保推理速度达标
4. **文档更新**：更新SDK开发文档中的编译说明

---

**最后更新**：2026-06-02
