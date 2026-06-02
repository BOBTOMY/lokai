# LokAI SDK 开发文档

## 目录

1. [SDK概述](#1-sdk概述)
2. [技术架构](#2-技术架构)
3. [功能模块设计](#3-功能模块设计)
4. [接口定义规范](#4-接口定义规范)
5. [集成指南](#5-集成指南)
6. [使用示例](#6-使用示例)
7. [性能优化策略](#7-性能优化策略)
8. [附录](#8-附录)

---

## 1. SDK概述

### 1.1 项目背景

LokAI SDK 是一个面向移动端的统一AI推理服务框架，旨在为Android和鸿蒙系统提供标准化的端侧AI推理能力。通过封装底层推理引擎的复杂性，SDK为上层应用提供简洁易用的API接口，使开发者无需深入了解模型推理细节即可快速集成AI能力。

### 1.2 设计目标

- **统一接口**：提供跨平台统一的API接口，降低多平台适配成本
- **高性能推理**：基于llama.cpp框架，实现高效的端侧模型推理
- **低集成门槛**：简化集成流程，提供完善的文档和示例代码
- **灵活配置**：支持多种模型格式和推理参数配置
- **安全可控**：数据本地处理，保护用户隐私

### 1.3 适用场景

- 智能对话助手
- 文本生成与补全
- 语义理解与分析
- 知识问答系统
- 本地化AI应用

### 1.4 版本规划

| 阶段 | 平台支持 | 核心功能 | 状态 |
|------|---------|---------|------|
| v1.0 | Android | 本地推理、基础API | 开发中 |
| v1.5 | Android | 性能优化、扩展API | 规划中 |
| v2.0 | Android + 鸿蒙 | 双平台统一接口 | 规划中 |

---

## 2. 技术架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    Application Layer                     │
│              (应用层：各类AI应用集成)                      │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                      LokAI SDK                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │              API Layer (接口层)                    │  │
│  │  - LokAIEngine (核心引擎接口)                     │  │
│  │  - ModelManager (模型管理接口)                    │  │
│  │  - InferenceSession (推理会话接口)                │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │           Core Layer (核心层)                     │  │
│  │  - EngineCore (引擎核心)                         │  │
│  │  - ModelLoader (模型加载器)                      │  │
│  │  - InferenceEngine (推理引擎)                    │  │
│  │  - MemoryManager (内存管理器)                    │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │         Platform Layer (平台适配层)               │  │
│  │  - AndroidPlatform (Android适配)                 │  │
│  │  - HarmonyPlatform (鸿蒙适配，v2.0)               │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                   Native Layer (原生层)                  │
│  ┌──────────────────────────────────────────────────┐  │
│  │              llama.cpp Framework                  │  │
│  │  - GGML/GGUF Model Support                       │  │
│  │  - CPU/GPU Inference                            │  │
│  │  - Quantization Support                          │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 2.2 技术栈

#### 2.2.1 核心依赖

| 组件 | 版本要求 | 用途 |
|------|---------|------|
| llama.cpp | >= b3472 (2024年12月) | 核心推理引擎 |
| NDK | r25c+ | Android原生开发 |
| CMake | 3.22.1+ | 构建系统 |
| Android Gradle Plugin | 8.0+ | 构建工具 |

#### 2.2.2 语言与框架

- **Native层**：C++17
- **Android SDK层**：Kotlin 1.9+
- **Android最低版本**：API 24 (Android 7.0)
- **目标Android版本**：API 34 (Android 14)

### 2.3 llama.cpp框架集成

#### 2.3.1 框架选择理由

llama.cpp是一个高性能的LLM推理框架，具有以下优势：

- **轻量级**：无外部依赖，易于集成
- **高性能**：优化的CPU/GPU推理性能
- **量化支持**：支持多种量化格式（Q4_0, Q4_1, Q5_0, Q5_1, Q8_0等）
- **跨平台**：支持多种平台和硬件架构
- **活跃维护**：社区活跃，更新频繁

#### 2.3.2 核心组件映射

| llama.cpp组件 | LokAI SDK封装 | 功能说明 |
|--------------|--------------|---------|
| llama_model | Model | 模型对象管理 |
| llama_context | InferenceContext | 推理上下文管理 |
| llama_tokenizer | Tokenizer | 分词器封装 |
| llama_sampler | Sampler | 采样器配置 |

### 2.4 数据流架构

```
用户输入 → Tokenizer → 推理引擎 → Detokenizer → 输出结果
    │           │           │            │           │
    └───────────┴───────────┴────────────┴───────────┘
                         SDK内部处理流程
```

---

## 3. 功能模块设计

### 3.1 模块总览

```
LokAI SDK
├── Core Module (核心模块)
│   ├── Engine Management (引擎管理)
│   ├── Model Management (模型管理)
│   └── Inference Control (推理控制)
├── Platform Module (平台模块)
│   ├── Android Platform (Android平台)
│   └── Harmony Platform (鸿蒙平台)
├── Utility Module (工具模块)
│   ├── Logger (日志系统)
│   ├── Config Manager (配置管理)
│   └── Performance Monitor (性能监控)
└── Extension Module (扩展模块)
    ├── Model Converter (模型转换)
    └── Benchmark Tool (性能测试)
```

### 3.2 核心模块详细设计

#### 3.2.1 引擎管理模块 (Engine Management)

**职责**：
- 初始化和销毁推理引擎
- 管理引擎生命周期
- 提供全局配置接口

**核心类设计**：

```kotlin
/**
 * LokAI引擎主入口
 */
object LokAIEngine {
    
    /**
     * 初始化引擎
     * @param context Android上下文
     * @param config 引擎配置
     */
    fun initialize(context: Context, config: EngineConfig): Result<Unit>
    
    /**
     * 检查引擎是否已初始化
     */
    fun isInitialized(): Boolean
    
    /**
     * 获取引擎版本
     */
    fun getVersion(): String
    
    /**
     * 销毁引擎，释放资源
     */
    fun shutdown()
    
    /**
     * 获取引擎状态
     */
    fun getStatus(): EngineStatus
}

/**
 * 引擎配置
 */
data class EngineConfig(
    val logLevel: LogLevel = LogLevel.INFO,
    val threadCount: Int = Runtime.getRuntime().availableProcessors(),
    val enablePerformanceMonitor: Boolean = false,
    val modelCachePath: String? = null,
    val maxMemoryMB: Int = 512
)

/**
 * 引擎状态
 */
sealed class EngineStatus {
    object Uninitialized : EngineStatus()
    object Initializing : EngineStatus()
    object Ready : EngineStatus()
    object Busy : EngineStatus()
    object Error : EngineStatus()
}
```

#### 3.2.2 模型管理模块 (Model Management)

**职责**：
- 模型加载与卸载
- 模型信息查询
- 模型缓存管理

**核心类设计**：

```kotlin
/**
 * 模型管理器
 */
class ModelManager private constructor() {
    
    companion object {
        fun getInstance(): ModelManager
    }
    
    /**
     * 加载模型
     * @param modelPath 模型文件路径
     * @param config 模型加载配置
     * @return 模型实例
     */
    fun loadModel(modelPath: String, config: ModelConfig = ModelConfig()): Result<Model>
    
    /**
     * 从Assets加载模型
     * @param context Android上下文
     * @param assetPath Assets中的模型路径
     * @param config 模型加载配置
     * @return 模型实例
     */
    fun loadModelFromAssets(
        context: Context, 
        assetPath: String, 
        config: ModelConfig = ModelConfig()
    ): Result<Model>
    
    /**
     * 卸载模型
     * @param model 要卸载的模型
     */
    fun unloadModel(model: Model)
    
    /**
     * 获取已加载的模型列表
     */
    fun getLoadedModels(): List<ModelInfo>
    
    /**
     * 检查模型是否已加载
     * @param modelId 模型ID
     */
    fun isModelLoaded(modelId: String): Boolean
}

/**
 * 模型配置
 */
data class ModelConfig(
    val contextSize: Int = 2048,
    val batchSize: Int = 512,
    val gpuLayers: Int = 0,
    val mainGpu: Int = 0,
    val tensorSplit: FloatArray? = null,
    val useMmap: Boolean = true,
    val useMlock: Boolean = false,
    val vocabOnly: Boolean = false
)

/**
 * 模型信息
 */
data class ModelInfo(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val parameters: Long,
    val quantization: String,
    val contextLength: Int,
    val vocabularySize: Int,
    val metadata: Map<String, String>
)

/**
 * 模型实例
 */
class Model internal constructor(
    val info: ModelInfo,
    internal val nativeHandle: Long
) : Closeable {
    
    /**
     * 创建推理会话
     * @param config 会话配置
     */
    fun createSession(config: SessionConfig = SessionConfig()): InferenceSession
    
    /**
     * 获取分词器
     */
    fun getTokenizer(): Tokenizer
    
    override fun close()
}
```

#### 3.2.3 推理控制模块 (Inference Control)

**职责**：
- 管理推理会话
- 执行推理任务
- 处理推理结果

**核心类设计**：

```kotlin
/**
 * 推理会话
 */
class InferenceSession internal constructor(
    private val model: Model,
    private val config: SessionConfig
) : Closeable {
    
    /**
     * 执行文本补全推理
     * @param prompt 输入提示词
     * @param params 推理参数
     * @return 推理结果
     */
    fun complete(prompt: String, params: InferenceParams = InferenceParams()): Result<CompletionResult>
    
    /**
     * 流式执行文本补全
     * @param prompt 输入提示词
     * @param params 推理参数
     * @param onToken 流式回调
     */
    fun completeStream(
        prompt: String,
        params: InferenceParams = InferenceParams(),
        onToken: (String) -> Unit
    ): Result<CompletionResult>
    
    /**
     * 聊天补全
     * @param messages 消息列表
     * @param params 推理参数
     */
    fun chat(
        messages: List<ChatMessage>,
        params: InferenceParams = InferenceParams()
    ): Result<CompletionResult>
    
    /**
     * 流式聊天补全
     * @param messages 消息列表
     * @param params 推理参数
     * @param onToken 流式回调
     */
    fun chatStream(
        messages: List<ChatMessage>,
        params: InferenceParams = InferenceParams(),
        onToken: (String) -> Unit
    ): Result<CompletionResult>
    
    /**
     * 停止当前推理
     */
    fun stop()
    
    /**
     * 重置会话状态
     */
    fun reset()
    
    /**
     * 获取会话统计信息
     */
    fun getStats(): SessionStats
    
    override fun close()
}

/**
 * 会话配置
 */
data class SessionConfig(
    val contextSize: Int = 2048,
    val batchSize: Int = 512,
    val threads: Int = Runtime.getRuntime().availableProcessors(),
    val seed: Long = -1
)

/**
 * 推理参数
 */
data class InferenceParams(
    val maxTokens: Int = 256,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    val presencePenalty: Float = 0.0f,
    val frequencyPenalty: Float = 0.0f,
    val stopSequences: List<String> = emptyList(),
    val stream: Boolean = false
)

/**
 * 补全结果
 */
data class CompletionResult(
    val text: String,
    val tokensGenerated: Int,
    val tokensPerSecond: Float,
    val totalTimeMs: Long,
    val finishReason: FinishReason
)

/**
 * 聊天消息
 */
data class ChatMessage(
    val role: Role,
    val content: String
) {
    enum class Role {
        SYSTEM, USER, ASSISTANT
    }
}

/**
 * 结束原因
 */
enum class FinishReason {
    STOP, LENGTH, ERROR
}

/**
 * 会话统计
 */
data class SessionStats(
    val totalTokens: Int,
    val totalInferences: Int,
    val averageTokensPerSecond: Float,
    val totalInferenceTimeMs: Long,
    val memoryUsageMB: Float
)
```

### 3.3 平台适配模块

#### 3.3.1 Android平台适配

**职责**：
- 提供Android特定功能实现
- 管理Android资源
- 处理Android生命周期

**核心实现**：

```kotlin
/**
 * Android平台适配器
 */
internal class AndroidPlatform private constructor(
    private val context: Context
) : Platform {
    
    companion object {
        fun initialize(context: Context): AndroidPlatform
    }
    
    override fun getAssetPath(assetName: String): String
    
    override fun getCacheDir(): String
    
    override fun getNativeLibraryDir(): String
    
    override fun getAvailableMemory(): Long
    
    override fun getCpuCount(): Int
    
    override fun isGpuAvailable(): Boolean
    
    override fun getGpuInfo(): GpuInfo?
    
    fun copyAssetToCache(assetName: String): String
}
```

#### 3.3.2 鸿蒙平台适配（v2.0规划）

```kotlin
/**
 * 鸿蒙平台适配器
 */
internal class HarmonyPlatform private constructor(
    private val context: AbilityContext
) : Platform {
    
    companion object {
        fun initialize(context: AbilityContext): HarmonyPlatform
    }
    
    override fun getAssetPath(assetName: String): String
    
    override fun getCacheDir(): String
    
    override fun getNativeLibraryDir(): String
    
    override fun getAvailableMemory(): Long
    
    override fun getCpuCount(): Int
    
    override fun isGpuAvailable(): Boolean
    
    override fun getGpuInfo(): GpuInfo?
}
```

### 3.4 工具模块

#### 3.4.1 日志系统

```kotlin
/**
 * SDK日志工具
 */
object LokAILogger {
    
    fun setLogLevel(level: LogLevel)
    
    fun setTag(tag: String)
    
    fun enableFileLogging(enable: Boolean, path: String? = null)
    
    fun d(message: String)
    
    fun i(message: String)
    
    fun w(message: String)
    
    fun e(message: String, throwable: Throwable? = null)
}

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, NONE
}
```

#### 3.4.2 配置管理

```kotlin
/**
 * 配置管理器
 */
object ConfigManager {
    
    fun loadConfig(context: Context, configName: String = "lokai_config.json"): LokAIConfig
    
    fun saveConfig(context: Context, config: LokAIConfig, configName: String = "lokai_config.json")
    
    fun getDefaultConfig(): LokAIConfig
}

data class LokAIConfig(
    val engine: EngineConfig = EngineConfig(),
    val model: ModelConfig = ModelConfig(),
    val session: SessionConfig = SessionConfig(),
    val inference: InferenceParams = InferenceParams()
)
```

#### 3.4.3 性能监控

```kotlin
/**
 * 性能监控器
 */
class PerformanceMonitor {
    
    fun startMonitoring()
    
    fun stopMonitoring()
    
    fun getMetrics(): PerformanceMetrics
    
    fun recordInference(durationMs: Long, tokens: Int)
    
    fun recordMemoryUsage()
}

data class PerformanceMetrics(
    val averageInferenceTime: Long,
    val averageTokensPerSecond: Float,
    val peakMemoryMB: Float,
    val currentMemoryMB: Float,
    val totalInferences: Int,
    val totalTokensGenerated: Int
)
```

---

## 4. 接口定义规范

### 4.1 API设计原则

1. **简洁性**：API接口简单直观，易于理解和使用
2. **一致性**：命名规范统一，参数风格一致
3. **安全性**：避免暴露内部实现细节
4. **扩展性**：预留扩展接口，支持未来功能增强
5. **容错性**：完善的错误处理机制

### 4.2 命名规范

#### 4.2.1 包命名

```
com.lokai.sdk              // SDK根包
com.lokai.sdk.core         // 核心功能
com.lokai.sdk.model        // 模型管理
com.lokai.sdk.inference    // 推理功能
com.lokai.sdk.platform     // 平台适配
com.lokai.sdk.util         // 工具类
com.lokai.sdk.exception    // 异常定义
com.lokai.sdk.callback     // 回调接口
```

#### 4.2.2 类命名

- 使用大驼峰命名法（PascalCase）
- 接口以`I`开头或使用描述性名称
- 实现类不加特殊后缀

示例：
```kotlin
class LokAIEngine
class ModelManager
interface InferenceCallback
class InferenceSession
```

#### 4.2.3 方法命名

- 使用小驼峰命名法（camelCase）
- 动词开头，描述性命名

示例：
```kotlin
fun loadModel()
fun createSession()
fun complete()
fun completeStream()
```

#### 4.2.4 常量命名

- 使用全大写，下划线分隔

示例：
```kotlin
const val MAX_CONTEXT_SIZE = 4096
const val DEFAULT_TEMPERATURE = 0.7f
```

### 4.3 数据类型规范

#### 4.3.1 基础数据类

```kotlin
/**
 * 统一返回结果封装
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: LokAIException) : Result<Nothing>()
    
    inline fun onSuccess(action: (T) -> Unit): Result<T>
    inline fun onError(action: (LokAIException) -> Unit): Result<T>
    fun getOrNull(): T?
    fun getOrThrow(): T
}

/**
 * 异常基类
 */
sealed class LokAIException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    class EngineNotInitializedException : LokAIException("Engine not initialized")
    class ModelLoadException(path: String, cause: Throwable?) : LokAIException("Failed to load model: $path", cause)
    class InferenceException(message: String) : LokAIException(message)
    class InvalidParameterException(param: String) : LokAIException("Invalid parameter: $param")
    class MemoryLimitException(required: Long, available: Long) : LokAIException("Insufficient memory. Required: $required, Available: $available")
    class UnsupportedModelException(format: String) : LokAIException("Unsupported model format: $format")
}
```

#### 4.3.2 回调接口

```kotlin
/**
 * 推理回调接口
 */
interface InferenceCallback {
    /**
     * 生成token回调
     * @param token 生成的token
     */
    fun onTokenGenerated(token: String)
    
    /**
     * 推理完成回调
     * @param result 推理结果
     */
    fun onComplete(result: CompletionResult)
    
    /**
     * 推理错误回调
     * @param exception 异常信息
     */
    fun onError(exception: LokAIException)
}

/**
 * 模型加载回调
 */
interface ModelLoadCallback {
    /**
     * 加载进度回调
     * @param progress 进度百分比 (0-100)
     */
    fun onProgress(progress: Int)
    
    /**
     * 加载完成回调
     * @param model 加载的模型
     */
    fun onComplete(model: Model)
    
    /**
     * 加载错误回调
     * @param exception 异常信息
     */
    fun onError(exception: LokAIException)
}
```

### 4.4 JNI接口规范

#### 4.4.1 Native方法命名

```cpp
// 命名规范: Java_<包名>_<类名>_<方法名>
extern "C" {

// 引擎管理
JNIEXPORT jlong JNICALL
Java_com_lokai_sdk_core_NativeEngine_nativeInit(JNIEnv *env, jobject thiz, jobject config);

JNIEXPORT void JNICALL
Java_com_lokai_sdk_core_NativeEngine_nativeDestroy(JNIEnv *env, jobject thiz, jlong handle);

// 模型管理
JNIEXPORT jlong JNICALL
Java_com_lokai_sdk_model_NativeModelLoader_nativeLoadModel(
    JNIEnv *env, 
    jobject thiz, 
    jstring model_path, 
    jobject config
);

JNIEXPORT void JNICALL
Java_com_lokai_sdk_model_NativeModelLoader_nativeUnloadModel(
    JNIEnv *env, 
    jobject thiz, 
    jlong model_handle
);

// 推理执行
JNIEXPORT jobject JNICALL
Java_com_lokai_sdk_inference_NativeInference_nativeComplete(
    JNIEnv *env,
    jobject thiz,
    jlong session_handle,
    jstring prompt,
    jobject params
);

JNIEXPORT void JNICALL
Java_com_lokai_sdk_inference_NativeInference_nativeCompleteStream(
    JNIEnv *env,
    jobject thiz,
    jlong session_handle,
    jstring prompt,
    jobject params,
    jobject callback
);

} // extern "C"
```

#### 4.4.2 数据结构映射

```cpp
// Kotlin -> C++ 数据映射

// EngineConfig
struct EngineConfigNative {
    int log_level;
    int thread_count;
    bool enable_performance_monitor;
    std::string model_cache_path;
    int max_memory_mb;
};

// ModelConfig
struct ModelConfigNative {
    int context_size;
    int batch_size;
    int gpu_layers;
    int main_gpu;
    std::vector<float> tensor_split;
    bool use_mmap;
    bool use_mlock;
    bool vocab_only;
};

// InferenceParams
struct InferenceParamsNative {
    int max_tokens;
    float temperature;
    float top_p;
    int top_k;
    float repeat_penalty;
    float presence_penalty;
    float frequency_penalty;
    std::vector<std::string> stop_sequences;
    bool stream;
};
```

### 4.5 错误码定义

```kotlin
/**
 * 错误码定义
 */
object ErrorCode {
    const val SUCCESS = 0
    const val UNKNOWN_ERROR = -1
    
    // 引擎错误 (1000-1999)
    const val ENGINE_NOT_INITIALIZED = 1001
    const val ENGINE_ALREADY_INITIALIZED = 1002
    const val ENGINE_INIT_FAILED = 1003
    
    // 模型错误 (2000-2999)
    const val MODEL_LOAD_FAILED = 2001
    const val MODEL_UNLOAD_FAILED = 2002
    const val MODEL_NOT_FOUND = 2003
    const val MODEL_FORMAT_UNSUPPORTED = 2004
    const val MODEL_CORRUPTED = 2005
    
    // 推理错误 (3000-3999)
    const val INFERENCE_FAILED = 3001
    const val INFERENCE_TIMEOUT = 3002
    const val INFERENCE_CANCELLED = 3003
    const val CONTEXT_OVERFLOW = 3004
    
    // 参数错误 (4000-4999)
    const val INVALID_PARAMETER = 4001
    const val PARAMETER_OUT_OF_RANGE = 4002
    
    // 资源错误 (5000-5999)
    const val MEMORY_INSUFFICIENT = 5001
    const val STORAGE_INSUFFICIENT = 5002
    const val THREAD_LIMIT_EXCEEDED = 5003
}
```

---

## 5. 集成指南

### 5.1 环境要求

#### 5.1.1 开发环境

| 工具 | 版本要求 |
|------|---------|
| Android Studio | Hedgehog (2023.1.1) 或更高 |
| JDK | 17+ |
| Android SDK | API 24+ |
| NDK | r25c+ |
| CMake | 3.22.1+ |

#### 5.1.2 设备要求

- **最低Android版本**：Android 7.0 (API 24)
- **推荐Android版本**：Android 10+ (API 29+)
- **CPU架构**：arm64-v8a, armeabi-v7a
- **推荐内存**：4GB+（取决于模型大小）
- **存储空间**：根据模型大小，建议预留模型大小2倍的空间

### 5.2 集成步骤

#### 5.2.1 添加依赖

**方式一：Maven依赖（推荐）**

在项目根目录的 `build.gradle` 中添加：

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://repo.lokai.com/maven' }
    }
}
```

在app模块的 `build.gradle` 中添加依赖：

```gradle
dependencies {
    implementation 'com.lokai:sdk:1.0.0'
    implementation 'com.lokai:sdk-native-arm64:1.0.0'  // arm64-v8a
    // implementation 'com.lokai:sdk-native-armv7:1.0.0'  // armeabi-v7a (可选)
}
```

**方式二：AAR文件集成**

1. 将 `lokai-sdk.aar` 文件放入项目的 `libs` 目录
2. 在 `build.gradle` 中添加：

```gradle
dependencies {
    implementation files('libs/lokai-sdk.aar')
}
```

#### 5.2.2 配置NDK

在app模块的 `build.gradle` 中配置：

```gradle
android {
    defaultConfig {
        ndk {
            abiFilters 'arm64-v8a'  // 推荐只包含arm64-v8a
        }
    }
    
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
            version "3.22.1"
        }
    }
}
```

#### 5.2.3 权限配置

在 `AndroidManifest.xml` 中添加必要权限：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- 读取模型文件 -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    
    <!-- 写入缓存文件 -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
    <!-- 可选：网络权限（用于下载模型） -->
    <uses-permission android:name="android.permission.INTERNET" />
    
</manifest>
```

#### 5.2.4 初始化SDK

在Application类中初始化SDK：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化LokAI SDK
        val config = EngineConfig(
            logLevel = LogLevel.INFO,
            threadCount = Runtime.getRuntime().availableProcessors(),
            enablePerformanceMonitor = BuildConfig.DEBUG
        )
        
        LokAIEngine.initialize(this, config)
            .onSuccess {
                Log.i("LokAI", "SDK initialized successfully")
            }
            .onError { exception ->
                Log.e("LokAI", "Failed to initialize SDK", exception)
            }
    }
    
    override fun onTerminate() {
        LokAIEngine.shutdown()
        super.onTerminate()
    }
}
```

### 5.3 模型准备

#### 5.3.1 支持的模型格式

LokAI SDK支持GGUF格式的模型文件，推荐使用以下量化格式：

| 量化类型 | 大小 | 质量 | 推荐场景 |
|---------|------|------|---------|
| Q4_0 | 最小 | 较低 | 内存受限设备 |
| Q4_1 | 小 | 中等 | 平衡性能与质量 |
| Q5_0 | 中 | 良好 | 推荐使用 |
| Q5_1 | 中 | 良好 | 推荐使用 |
| Q8_0 | 大 | 最佳 | 高质量要求 |

#### 5.3.2 模型获取

**方式一：下载预训练模型**

从Hugging Face或ModelScope下载GGUF格式模型：

```bash
# 示例：下载Qwen模型
wget https://huggingface.co/Qwen/Qwen-7B-Chat-GGUF/resolve/main/qwen-7b-chat-q5_0.gguf
```

**方式二：模型转换**

使用llama.cpp提供的工具将其他格式转换为GGUF：

```bash
# 转换PyTorch模型
python convert.py /path/to/model --outtype q5_0 --outfile model.gguf
```

#### 5.3.3 模型放置

**方式一：Assets目录（推荐用于小模型）**

将模型文件放入 `assets/models/` 目录：

```
app/
└── src/
    └── main/
        └── assets/
            └── models/
                └── qwen-7b-chat-q5_0.gguf
```

**方式二：外部存储（推荐用于大模型）**

将模型文件放入设备的存储目录：

```kotlin
val modelDir = File(context.filesDir, "models")
modelDir.mkdirs()
// 将模型文件复制到该目录
```

### 5.4 编译配置

#### 5.4.1 CMakeLists.txt配置

```cmake
cmake_minimum_required(VERSION 3.22.1)
project(lokai_sdk)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 设置编译选项
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -fPIC")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -fPIC")

# 添加llama.cpp源码
add_subdirectory(${CMAKE_SOURCE_DIR}/llama.cpp)

# 编译SDK native库
add_library(lokai_native SHARED
    src/main/cpp/lokai_engine.cpp
    src/main/cpp/lokai_model.cpp
    src/main/cpp/lokai_inference.cpp
    src/main/cpp/jni_interface.cpp
)

# 链接llama.cpp
target_link_libraries(lokai_native
    llama
    log
    android
)

# 包含头文件
target_include_directories(lokai_native PRIVATE
    ${CMAKE_SOURCE_DIR}/llama.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/include
)
```

#### 5.4.2 ABI配置

```gradle
android {
    defaultConfig {
        ndk {
            abiFilters 'arm64-v8a'  // 优先支持arm64
        }
    }
}
```

### 5.5 ProGuard配置

如果启用了代码混淆，需要在 `proguard-rules.pro` 中添加：

```proguard
# LokAI SDK
-keep class com.lokai.sdk.** { *; }
-keepclassmembers class com.lokai.sdk.** {
    native <methods>;
}

# 保留native方法
-keepclasseswithmembernames class * {
    native <methods>;
}
```

---

## 6. 使用示例

### 6.1 基础使用

#### 6.1.1 完整示例：文本补全

```kotlin
import com.lokai.sdk.LokAIEngine
import com.lokai.sdk.model.ModelManager
import com.lokai.sdk.model.ModelConfig
import com.lokai.sdk.inference.InferenceSession
import com.lokai.sdk.inference.InferenceParams
import com.lokai.sdk.inference.SessionConfig

class TextCompletionActivity : AppCompatActivity() {
    
    private var model: Model? = null
    private var session: InferenceSession? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_completion)
        
        // 加载模型
        loadModel()
    }
    
    private fun loadModel() {
        lifecycleScope.launch(Dispatchers.IO) {
            // 方式一：从Assets加载
            val result = ModelManager.getInstance()
                .loadModelFromAssets(
                    context = this@TextCompletionActivity,
                    assetPath = "models/qwen-7b-chat-q5_0.gguf",
                    config = ModelConfig(
                        contextSize = 2048,
                        gpuLayers = 0  // CPU推理
                    )
                )
            
            result.onSuccess { loadedModel ->
                model = loadedModel
                session = loadedModel.createSession(
                    SessionConfig(
                        contextSize = 2048,
                        threads = Runtime.getRuntime().availableProcessors()
                    )
                )
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TextCompletionActivity,
                        "模型加载成功",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.onError { exception ->
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TextCompletionActivity,
                        "模型加载失败: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    fun onGenerateClick(view: View) {
        val prompt = findViewById<EditText>(R.id.et_prompt).text.toString()
        
        lifecycleScope.launch(Dispatchers.IO) {
            session?.let { currentSession ->
                val result = currentSession.complete(
                    prompt = prompt,
                    params = InferenceParams(
                        maxTokens = 256,
                        temperature = 0.7f,
                        topP = 0.9f,
                        topK = 40
                    )
                )
                
                result.onSuccess { completionResult ->
                    withContext(Dispatchers.Main) {
                        findViewById<TextView>(R.id.tv_result).text = completionResult.text
                        
                        // 显示性能统计
                        findViewById<TextView>(R.id.tv_stats).text = 
                            "生成速度: ${completionResult.tokensPerSecond} tokens/s\n" +
                            "生成token数: ${completionResult.tokensGenerated}\n" +
                            "耗时: ${completionResult.totalTimeMs}ms"
                    }
                }.onError { exception ->
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@TextCompletionActivity,
                            "推理失败: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        session?.close()
        model?.close()
    }
}
```

#### 6.1.2 流式输出示例

```kotlin
class StreamInferenceActivity : AppCompatActivity() {
    
    private var model: Model? = null
    private var session: InferenceSession? = null
    
    fun onStreamGenerateClick(view: View) {
        val prompt = findViewById<EditText>(R.id.et_prompt).text.toString()
        val resultTextView = findViewById<TextView>(R.id.tv_result)
        
        resultTextView.text = ""
        
        lifecycleScope.launch(Dispatchers.IO) {
            session?.let { currentSession ->
                val result = currentSession.completeStream(
                    prompt = prompt,
                    params = InferenceParams(
                        maxTokens = 256,
                        temperature = 0.7f,
                        stream = true
                    ),
                    onToken = { token ->
                        withContext(Dispatchers.Main) {
                            resultTextView.append(token)
                        }
                    }
                )
                
                result.onSuccess { completionResult ->
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@StreamInferenceActivity,
                            "生成完成",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}
```

### 6.2 聊天应用示例

```kotlin
class ChatActivity : AppCompatActivity() {
    
    private var model: Model? = null
    private var session: InferenceSession? = null
    private val messageList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        setupRecyclerView()
        loadModel()
    }
    
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messageList)
        findViewById<RecyclerView>(R.id.rv_messages).apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ChatActivity)
        }
    }
    
    private fun loadModel() {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = ModelManager.getInstance()
                .loadModelFromAssets(
                    context = this@ChatActivity,
                    assetPath = "models/qwen-7b-chat-q5_0.gguf",
                    config = ModelConfig(contextSize = 4096)
                )
            
            result.onSuccess { loadedModel ->
                model = loadedModel
                session = loadedModel.createSession(
                    SessionConfig(contextSize = 4096)
                )
            }
        }
    }
    
    fun onSendClick(view: View) {
        val inputText = findViewById<EditText>(R.id.et_input).text.toString()
        if (inputText.isBlank()) return
        
        // 添加用户消息
        messageList.add(ChatMessage(ChatMessage.Role.USER, inputText))
        chatAdapter.notifyItemInserted(messageList.size - 1)
        
        findViewById<EditText>(R.id.et_input).text.clear()
        
        // 添加助手消息占位
        messageList.add(ChatMessage(ChatMessage.Role.ASSISTANT, ""))
        val assistantIndex = messageList.size - 1
        chatAdapter.notifyItemInserted(assistantIndex)
        
        lifecycleScope.launch(Dispatchers.IO) {
            session?.let { currentSession ->
                val result = currentSession.chatStream(
                    messages = messageList.dropLast(1),  // 不包含占位消息
                    params = InferenceParams(
                        maxTokens = 512,
                        temperature = 0.7f
                    ),
                    onToken = { token ->
                        withContext(Dispatchers.Main) {
                            messageList[assistantIndex] = ChatMessage(
                                ChatMessage.Role.ASSISTANT,
                                messageList[assistantIndex].content + token
                            )
                            chatAdapter.notifyItemChanged(assistantIndex)
                        }
                    }
                )
                
                result.onSuccess { completionResult ->
                    withContext(Dispatchers.Main) {
                        // 滚动到底部
                        findViewById<RecyclerView>(R.id.rv_messages)
                            .scrollToPosition(messageList.size - 1)
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        session?.close()
        model?.close()
    }
}

class ChatAdapter(
    private val messages: List<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }
    
    override fun getItemCount() = messages.size
    
    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(message: ChatMessage) {
            itemView.findViewById<TextView>(R.id.tv_message).text = message.content
            itemView.findViewById<TextView>(R.id.tv_role).text = 
                if (message.role == ChatMessage.Role.USER) "用户" else "助手"
        }
    }
}
```

### 6.3 高级用法

#### 6.3.1 自定义采样参数

```kotlin
fun advancedInference(prompt: String) {
    val params = InferenceParams(
        maxTokens = 512,
        temperature = 0.8f,
        topP = 0.95f,
        topK = 50,
        repeatPenalty = 1.2f,
        presencePenalty = 0.5f,
        frequencyPenalty = 0.5f,
        stopSequences = listOf("###", "\n\n"),
        stream = false
    )
    
    session?.complete(prompt, params)
}
```

#### 6.3.2 多会话管理

```kotlin
class MultiSessionManager(private val model: Model) {
    
    private val sessions = mutableMapOf<String, InferenceSession>()
    
    fun createSession(sessionId: String, config: SessionConfig = SessionConfig()): InferenceSession {
        val session = model.createSession(config)
        sessions[sessionId] = session
        return session
    }
    
    fun getSession(sessionId: String): InferenceSession? = sessions[sessionId]
    
    fun closeSession(sessionId: String) {
        sessions[sessionId]?.close()
        sessions.remove(sessionId)
    }
    
    fun closeAllSessions() {
        sessions.values.forEach { it.close() }
        sessions.clear()
    }
}
```

#### 6.3.3 性能监控

```kotlin
class PerformanceMonitorActivity : AppCompatActivity() {
    
    private lateinit var performanceMonitor: PerformanceMonitor
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        performanceMonitor = PerformanceMonitor()
        performanceMonitor.startMonitoring()
    }
    
    fun showMetrics() {
        val metrics = performanceMonitor.getMetrics()
        
        val info = """
            平均推理时间: ${metrics.averageInferenceTime}ms
            平均生成速度: ${metrics.averageTokensPerSecond} tokens/s
            峰值内存: ${metrics.peakMemoryMB}MB
            当前内存: ${metrics.currentMemoryMB}MB
            总推理次数: ${metrics.totalInferences}
            总生成token数: ${metrics.totalTokensGenerated}
        """.trimIndent()
        
        findViewById<TextView>(R.id.tv_metrics).text = info
    }
    
    override fun onDestroy() {
        super.onDestroy()
        performanceMonitor.stopMonitoring()
    }
}
```

#### 6.3.4 模型管理

```kotlin
class ModelManagementActivity : AppCompatActivity() {
    
    private lateinit var modelManager: ModelManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        modelManager = ModelManager.getInstance()
    }
    
    fun loadMultipleModels() {
        lifecycleScope.launch(Dispatchers.IO) {
            // 加载多个模型
            val model1Result = modelManager.loadModel(
                modelPath = "/sdcard/models/qwen-7b-chat-q5_0.gguf",
                config = ModelConfig(contextSize = 2048)
            )
            
            val model2Result = modelManager.loadModel(
                modelPath = "/sdcard/models/qwen-14b-chat-q4_0.gguf",
                config = ModelConfig(contextSize = 4096)
            )
            
            // 查看已加载模型
            val loadedModels = modelManager.getLoadedModels()
            withContext(Dispatchers.Main) {
                loadedModels.forEach { modelInfo ->
                    Log.i("Model", "Loaded: ${modelInfo.name}, Size: ${modelInfo.size}")
                }
            }
        }
    }
    
    fun unloadModel(modelId: String) {
        modelManager.getLoadedModels()
            .find { it.id == modelId }
            ?.let { modelManager.unloadModel(it) }
    }
}
```

---

## 7. 性能优化策略

### 7.1 模型选择优化

#### 7.1.1 量化策略选择

| 设备内存 | 推荐量化 | 模型大小 | 性能 |
|---------|---------|---------|------|
| 4GB | Q4_0 | ~2-3GB | 较快 |
| 6GB | Q4_1/Q5_0 | ~3-4GB | 平衡 |
| 8GB+ | Q5_1/Q8_0 | ~4-6GB | 最佳质量 |

#### 7.1.2 上下文长度优化

```kotlin
// 根据设备内存动态调整上下文长度
fun getOptimalContextSize(): Int {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryMB = activityManager.memoryClass
    
    return when {
        memoryMB >= 256 -> 4096
        memoryMB >= 128 -> 2048
        else -> 1024
    }
}

val config = ModelConfig(
    contextSize = getOptimalContextSize()
)
```

### 7.2 推理性能优化

#### 7.2.1 线程优化

```kotlin
// 获取最优线程数
fun getOptimalThreadCount(): Int {
    val processors = Runtime.getRuntime().availableProcessors()
    // 保留1-2个核心给系统
    return maxOf(1, processors - 2)
}

val sessionConfig = SessionConfig(
    threads = getOptimalThreadCount()
)
```

#### 7.2.2 批处理优化

```kotlin
// 对于短文本生成，使用较小的batch size
val config = ModelConfig(
    batchSize = 256  // 适合短文本
)

// 对于长文本生成，使用较大的batch size
val config = ModelConfig(
    batchSize = 512  // 适合长文本
)
```

#### 7.2.3 内存映射优化

```kotlin
// 对于大模型，启用mmap
val config = ModelConfig(
    useMmap = true,   // 启用内存映射
    useMlock = false  // 禁用内存锁定（需要root权限）
)
```

### 7.3 内存管理优化

#### 7.3.1 及时释放资源

```kotlin
class InferenceManager {
    private var session: InferenceSession? = null
    private var model: Model? = null
    
    fun cleanup() {
        session?.close()
        session = null
        
        model?.close()
        model = null
        
        System.gc()  // 建议垃圾回收
    }
}
```

#### 7.3.2 会话复用

```kotlin
// 复用会话，避免重复创建
class SessionPool(private val model: Model, private val poolSize: Int = 3) {
    
    private val availableSessions = ConcurrentLinkedQueue<InferenceSession>()
    
    init {
        repeat(poolSize) {
            availableSessions.add(model.createSession())
        }
    }
    
    fun <T> withSession(block: (InferenceSession) -> T): T {
        val session = availableSessions.poll() ?: model.createSession()
        try {
            return block(session)
        } finally {
            availableSessions.offer(session)
        }
    }
    
    fun closeAll() {
        availableSessions.forEach { it.close() }
        availableSessions.clear()
    }
}
```

### 7.4 GPU加速（实验性）

#### 7.4.1 GPU配置

```kotlin
// 启用GPU加速（需要设备支持）
val config = ModelConfig(
    gpuLayers = 32,  // 将32层卸载到GPU
    mainGpu = 0,     // 使用第一个GPU
    tensorSplit = floatArrayOf(1.0f)  // GPU负载分配
)
```

#### 7.4.2 GPU可用性检查

```kotlin
fun isGpuAvailable(): Boolean {
    return try {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.deviceConfigurationInfo.reqGlEsVersion >= 0x30000
    } catch (e: Exception) {
        false
    }
}
```

### 7.5 性能监控与调优

#### 7.5.1 实时性能监控

```kotlin
class PerformanceTracker {
    
    private val inferenceTimes = mutableListOf<Long>()
    private val tokensGenerated = mutableListOf<Int>()
    
    fun recordInference(durationMs: Long, tokens: Int) {
        inferenceTimes.add(durationMs)
        tokensGenerated.add(tokens)
    }
    
    fun getAverageTokensPerSecond(): Float {
        if (inferenceTimes.isEmpty()) return 0f
        
        val totalTokens = tokensGenerated.sum()
        val totalTimeSeconds = inferenceTimes.sum() / 1000f
        
        return if (totalTimeSeconds > 0) {
            totalTokens / totalTimeSeconds
        } else {
            0f
        }
    }
    
    fun getPercentileLatency(percentile: Double): Long {
        if (inferenceTimes.isEmpty()) return 0
        
        val sorted = inferenceTimes.sorted()
        val index = (sorted.size * percentile / 100).toInt()
        return sorted[index]
    }
}
```

#### 7.5.2 性能基准测试

```kotlin
class BenchmarkRunner {
    
    fun runBenchmark(model: Model, iterations: Int = 10): BenchmarkResult {
        val session = model.createSession()
        val tracker = PerformanceTracker()
        
        val testPrompts = listOf(
            "人工智能的未来发展趋势是什么？",
            "请解释量子计算的基本原理。",
            "如何提高编程能力？"
        )
        
        repeat(iterations) { iteration ->
            val prompt = testPrompts[iteration % testPrompts.size]
            
            val startTime = System.currentTimeMillis()
            val result = session.complete(
                prompt = prompt,
                params = InferenceParams(maxTokens = 100)
            )
            val endTime = System.currentTimeMillis()
            
            result.onSuccess { completion ->
                tracker.recordInference(
                    durationMs = endTime - startTime,
                    tokens = completion.tokensGenerated
                )
            }
            
            session.reset()
        }
        
        session.close()
        
        return BenchmarkResult(
            averageTokensPerSecond = tracker.getAverageTokensPerSecond(),
            p50Latency = tracker.getPercentileLatency(50.0),
            p95Latency = tracker.getPercentileLatency(95.0),
            p99Latency = tracker.getPercentileLatency(99.0)
        )
    }
}

data class BenchmarkResult(
    val averageTokensPerSecond: Float,
    val p50Latency: Long,
    val p95Latency: Long,
    val p99Latency: Long
)
```

---

## 8. 附录

### 8.1 常见问题（FAQ）

#### Q1: 模型加载失败怎么办？

**A:** 检查以下几点：
1. 模型文件路径是否正确
2. 模型文件是否完整（未损坏）
3. 模型格式是否为GGUF
4. 设备内存是否充足
5. 是否有文件读取权限

```kotlin
// 调试代码
try {
    val file = File(modelPath)
    Log.d("Model", "File exists: ${file.exists()}")
    Log.d("Model", "File size: ${file.length()}")
    Log.d("Model", "Can read: ${file.canRead()}")
} catch (e: Exception) {
    Log.e("Model", "File check failed", e)
}
```

#### Q2: 推理速度很慢怎么办？

**A:** 尝试以下优化：
1. 使用量化程度更高的模型（如Q4_0）
2. 减少上下文长度
3. 调整线程数
4. 启用mmap
5. 关闭不必要的后台应用

#### Q3: 内存不足怎么办？

**A:** 解决方案：
1. 使用更小的模型或更高量化
2. 减少上下文长度
3. 及时释放不用的模型和会话
4. 避免同时加载多个模型

#### Q4: 如何处理中文乱码？

**A:** 确保模型支持中文，并正确设置编码：

```kotlin
// 确保使用UTF-8编码
val prompt = "你好，世界！"
val bytes = prompt.toByteArray(Charsets.UTF_8)
val decodedPrompt = String(bytes, Charsets.UTF_8)
```

### 8.2 错误处理最佳实践

```kotlin
class SafeInferenceManager(private val context: Context) {
    
    private var model: Model? = null
    private var session: InferenceSession? = null
    
    fun initialize(modelPath: String): Result<Unit> {
        return try {
            // 检查引擎状态
            if (!LokAIEngine.isInitialized()) {
                return Result.Error(LokAIException.EngineNotInitializedException())
            }
            
            // 检查文件是否存在
            val file = File(modelPath)
            if (!file.exists()) {
                return Result.Error(LokAIException.ModelLoadException(modelPath, null))
            }
            
            // 检查内存
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            if (memoryInfo.availMem < file.length() * 2) {
                return Result.Error(
                    LokAIException.MemoryLimitException(
                        required = file.length() * 2,
                        available = memoryInfo.availMem
                    )
                )
            }
            
            // 加载模型
            val result = ModelManager.getInstance().loadModel(modelPath)
            result.onSuccess { loadedModel ->
                model = loadedModel
                session = loadedModel.createSession()
            }
            
            result.map { }
        } catch (e: Exception) {
            Result.Error(LokAIException.InferenceException("Initialization failed: ${e.message}"))
        }
    }
    
    fun inference(prompt: String): Result<String> {
        return try {
            val currentSession = session ?: return Result.Error(
                LokAIException.InferenceException("Session not initialized")
            )
            
            currentSession.complete(prompt).map { it.text }
        } catch (e: Exception) {
            Result.Error(LokAIException.InferenceException("Inference failed: ${e.message}"))
        }
    }
    
    fun cleanup() {
        try {
            session?.close()
            model?.close()
            session = null
            model = null
        } catch (e: Exception) {
            Log.e("SafeInferenceManager", "Cleanup failed", e)
        }
    }
}
```

### 8.3 版本兼容性

#### 8.3.1 Android版本兼容

| Android版本 | API Level | 支持状态 | 备注 |
|------------|-----------|---------|------|
| Android 7.0 | 24 | 支持 | 最低支持版本 |
| Android 8.0 | 26 | 支持 | 推荐最低版本 |
| Android 10 | 29 | 支持 | 推荐使用 |
| Android 11+ | 30+ | 完全支持 | 最佳性能 |

#### 8.3.2 CPU架构支持

| 架构 | 支持状态 | 性能 | 备注 |
|-----|---------|------|------|
| arm64-v8a | 完全支持 | 最佳 | 推荐使用 |
| armeabi-v7a | 支持 | 较好 | 兼容旧设备 |
| x86 | 支持 | 一般 | 主要用于模拟器 |
| x86_64 | 支持 | 较好 | 主要用于模拟器 |

### 8.4 性能参考数据

#### 8.4.1 不同设备性能参考

| 设备类型 | CPU | 内存 | 模型 | 速度 (tokens/s) |
|---------|-----|------|------|----------------|
| 高端手机 | Snapdragon 8 Gen2 | 12GB | Qwen-7B-Q5 | 8-12 |
| 中端手机 | Snapdragon 778G | 8GB | Qwen-7B-Q4 | 5-8 |
| 低端手机 | Snapdragon 665 | 6GB | Qwen-7B-Q4 | 2-4 |

#### 8.4.2 不同量化性能对比

| 量化类型 | 模型大小 | 内存占用 | 速度 | 质量 |
|---------|---------|---------|------|------|
| Q4_0 | 3.8GB | 4.2GB | 快 | 较低 |
| Q4_1 | 4.2GB | 4.6GB | 较快 | 中等 |
| Q5_0 | 4.6GB | 5.0GB | 中等 | 良好 |
| Q5_1 | 5.0GB | 5.4GB | 中等 | 良好 |
| Q8_0 | 7.0GB | 7.4GB | 较慢 | 最佳 |

### 8.5 更新日志

#### v1.0.0 (计划中)

**新功能**
- 初始版本发布
- 支持Android平台
- 基于llama.cpp的本地推理
- 支持GGUF模型格式
- 提供文本补全和聊天API
- 支持流式输出
- 支持多种量化格式

**已知限制**
- 仅支持Android平台
- GPU加速功能为实验性
- 大模型（>7B）性能受限

### 8.6 联系方式

- **项目地址**: https://github.com/lokai/lokai-sdk
- **问题反馈**: https://github.com/lokai/lokai-sdk/issues
- **文档地址**: https://docs.lokai.com
- **邮箱**: support@lokai.com

---

**文档版本**: v1.0  
**最后更新**: 2026-06-02  
**维护团队**: LokAI SDK Team