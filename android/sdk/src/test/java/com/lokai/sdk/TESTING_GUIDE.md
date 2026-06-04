# LokAI SDK 测试指南

## 测试概述

LokAI SDK 包含两类测试：

| 类型 | 位置 | 说明 |
|------|------|------|
| **单元测试** | `sdk/src/test/` | 不依赖Native层，测试数据类、参数验证、内部逻辑 |
| **集成测试** | `sdk/src/androidTest/` | 需要在实际设备上运行，测试完整功能 |

## 单元测试

### 运行单元测试

```bash
cd lokai/android
./gradlew --no-daemon sdk:testDebugUnitTest
```

### 测试文件列表

| 文件 | 测试内容 | 测试数量 |
|------|---------|---------|
| `LokAIEngineTest.kt` | 引擎状态、版本、配置 | 7 |
| `ModelManagerTest.kt` | 配置类、数据类 | 6 |
| `InferenceSessionTest.kt` | 参数类、结果类 | 19 |
| `LokAIExceptionTest.kt` | 异常类定义 | 9 |
| `ParameterValidationTest.kt` | 参数边界验证 | 36 |
| `ErrorHandlingTest.kt` | 错误处理场景 | 21 |
| `InferenceSessionLogicTest.kt` | 内部逻辑测试 | 5 |
| `DataClassIntegrityTest.kt` | 数据类完整性 | 28 |

**总计：131个单元测试**

### 测试覆盖范围

- ✅ 配置类默认值和自定义值
- ✅ 数据类属性验证
- ✅ 参数边界条件
- ✅ 异常类型和消息
- ✅ Result类型操作
- ✅ Chat prompt构建逻辑
- ✅ 数据类copy、equals、hashCode

## 集成测试

### 准备工作

1. **准备模型文件**
   - 获取GGUF格式模型文件（如Qwen、Llama等）
   - 将模型文件放入Android设备：`/sdcard/test-model.gguf`

2. **连接Android设备**
   ```bash
   adb devices
   ```

### 运行集成测试

```bash
cd lokai/android
./gradlew --no-daemon sdk:connectedAndroidTest
```

### 集成测试内容

| 测试 | 说明 | 是否需要模型 |
|------|------|-------------|
| `testEngineInitialization` | 引擎初始化 | ❌ 不需要 |
| `testEngineMultipleInitialization` | 多次初始化 | ❌ 不需要 |
| `testGetModelManager` | 获取ModelManager | ❌ 不需要 |
| `testModelLoading` | 模型加载 | ✅ 需要 |
| `testInference` | 推理功能 | ✅ 需要 |
| `testStreamInference` | 流式推理 | ✅ 需要 |
| `testChat` | 对话功能 | ✅ 需要 |
| `testTokenizer` | 分词功能 | ✅ 需要 |
| `testPerformanceStats` | 性能统计 | ✅ 需要 |

### 测试模型推荐

| 模型 | 大小 | 推荐用途 |
|------|------|---------|
| Qwen-1.5-0.5B-Chat-GGUF | ~500MB | 快速测试 |
| Llama-3.2-1B-Instruct-GGUF | ~1GB | 基础测试 |
| Qwen-2.5-3B-Instruct-GGUF | ~3GB | 性能测试 |

## 测试报告

### 查看单元测试报告

```bash
# HTML报告
open sdk/build/reports/tests/testDebugUnitTest/index.html

# XML报告
ls sdk/build/test-results/testDebugUnitTest/
```

### 查看集成测试报告

```bash
# HTML报告
open sdk/build/reports/androidTests/connected/index.html
```

## 测试最佳实践

### 单元测试原则

1. **独立性**：每个测试独立运行，不依赖其他测试
2. **可重复**：多次运行结果一致
3. **快速**：单元测试应快速完成
4. **命名清晰**：使用描述性测试名称

### 集成测试原则

1. **真实环境**：使用真实设备和模型
2. **清理资源**：测试完成后释放模型资源
3. **超时设置**：长时间测试设置合理超时
4. **日志记录**：记录关键操作日志便于调试

## 扩展测试

### 添加新的单元测试

1. 在 `sdk/src/test/java/com/lokai/sdk/` 目录创建测试文件
2. 使用JUnit 4编写测试
3. 运行测试验证

### 添加新的集成测试

1. 在 `sdk/src/androidTest/java/com/lokai/sdk/` 目录创建测试文件
2. 使用AndroidJUnit4编写测试
3. 在设备上运行测试验证

## 性能基准测试

### 测试指标

| 指标 | 目标值 | 测试方法 |
|------|--------|---------|
| 推理速度 | >5 tokens/s | `testPerformanceStats` |
| 模型加载时间 | <10s (7B-Q5) | `testModelLoading` |
| 内存占用 | <模型大小×1.5 | `testPerformanceStats` |

### 测试设备建议

- **最低配置**：Android 7.0 (API 24)，2GB RAM
- **推荐配置**：Android 10+，4GB RAM
- **高性能配置**：Android 12+，6GB RAM

---

**最后更新**：2026-06-04