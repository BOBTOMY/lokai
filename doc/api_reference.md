# LokAI SDK API 参考文档

## 📋 文档状态

| 项 | 值 |
|---|---|
| 文档版本 | v1.0.1 |
| 创建日期 | 2026-06-02 |
| 最后更新 | 2026-06-04 |
| 状态 | **实现完成** |

---

## 📖 说明

本文档描述LokAI SDK v1.0的完整API接口。

详细设计请参考：[SDK_Development_Guide.md](./SDK_Development_Guide.md)

---

## 📋 API概览（v1.0）

### 1. LokAIEngine（引擎入口）

```kotlin
object LokAIEngine {
    /**
     * 初始化SDK引擎
     * @param context Android上下文
     * @param config 引擎配置（可选）
     * @return Result<Unit> 初始化结果
     */
    fun initialize(context: Context, config: EngineConfig = EngineConfig()): Result<Unit>
    
    /**
     * 检查引擎是否已初始化
     * @return Boolean 是否初始化
     */
    fun isInitialized(): Boolean
    
    /**
     * 获取SDK版本号
     * @return String 版本号
     */
    fun getVersion(): String
    
    /**
     * 关闭引擎，释放所有资源
     */
    fun shutdown()
    
    /**
     * 获取引擎状态
     * @return EngineStatus 引擎状态
     */
    fun getStatus(): EngineStatus
    
    /**
     * 获取模型管理器实例
     * @return ModelManager 模型管理器
     */
    fun getModelManager(): ModelManager
}
```

### 2. ModelManager（模型管理）

```kotlin
interface ModelManager {
    /**
     * 从文件路径加载模型
     * @param path 模型文件路径
     * @param config 模型配置（可选）
     * @return Result<Model> 加载的模型
     */
    suspend fun loadModel(path: String, config: ModelConfig = ModelConfig()): Result<Model>
    
    /**
     * 从Assets加载模型
     * @param context Android上下文
     * @param assetPath Assets中的模型路径
     * @param config 模型配置（可选）
     * @return Result<Model> 加载的模型
     */
    suspend fun loadModelFromAssets(context: Context, assetPath: String, config: ModelConfig = ModelConfig()): Result<Model>
    
    /**
     * 卸载模型
     * @param model 要卸载的模型
     */
    fun unloadModel(model: Model)
    
    /**
     * 获取所有已加载的模型信息
     * @return List<ModelInfo> 模型信息列表
     */
    fun getLoadedModels(): List<ModelInfo>
    
    /**
     * 检查模型是否已加载
     * @param modelId 模型ID
     * @return Boolean 是否已加载
     */
    fun isModelLoaded(modelId: String): Boolean
}
```

### 3. Model（模型）

```kotlin
interface Model : Closeable {
    /**
     * 模型信息
     */
    val info: ModelInfo
    
    /**
     * 创建推理会话
     * @param config 会话配置（可选）
     * @return InferenceSession 推理会话
     */
    fun createSession(config: SessionConfig = SessionConfig()): InferenceSession
    
    /**
     * 获取Tokenizer
     * @return Tokenizer 分词器
     */
    fun getTokenizer(): Tokenizer
    
    /**
     * 关闭模型，释放资源
     */
    override fun close()
}
```

### 4. InferenceSession（推理会话）

```kotlin
interface InferenceSession : Closeable {
    /**
     * 文本补全（同步）
     * @param prompt 提示词
     * @param params 推理参数（可选）
     * @return Result<CompletionResult> 完成结果
     */
    fun complete(prompt: String, params: InferenceParams = InferenceParams()): Result<CompletionResult>
    
    /**
     * 文本补全（流式）
     * @param prompt 提示词
     * @param params 推理参数（可选）
     * @return Flow<TokenResult> Token流
     */
    fun completeStream(prompt: String, params: InferenceParams = InferenceParams()): Flow<TokenResult>
    
    /**
     * 对话补全（同步）
     * @param messages 对话消息列表
     * @param params 推理参数（可选）
     * @return Result<CompletionResult> 完成结果
     */
    fun chat(messages: List<ChatMessage>, params: InferenceParams = InferenceParams()): Result<CompletionResult>
    
    /**
     * 对话补全（流式）
     * @param messages 对话消息列表
     * @param params 推理参数（可选）
     * @return Flow<TokenResult> Token流
     */
    fun chatStream(messages: List<ChatMessage>, params: InferenceParams = InferenceParams()): Flow<TokenResult>
    
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
     * @return SessionStats 统计信息
     */
    fun getStats(): SessionStats
    
    /**
     * 关闭会话，释放资源
     */
    override fun close()
}
```

### 5. Tokenizer（分词器）

```kotlin
interface Tokenizer {
    /**
     * 将文本分词
     * @param text 输入文本
     * @return List<Int> Token ID列表
     */
    fun encode(text: String): List<Int>
    
    /**
     * 将Token ID解码为文本
     * @param tokens Token ID列表
     * @return String 解码后的文本
     */
    fun decode(tokens: List<Int>): String
    
    /**
     * 获取词汇表大小
     * @return Int 词汇表大小
     */
    fun getVocabSize(): Int
    
    /**
     * 获取BOS Token
     * @return Int BOS Token ID
     */
    fun getBosToken(): Int
    
    /**
     * 获取EOS Token
     * @return Int EOS Token ID
     */
    fun getEosToken(): Int
}
```

---

## 📊 数据类型定义

### 1. EngineConfig（引擎配置）

```kotlin
data class EngineConfig(
    /** 线程数，默认自动检测 */
    val threadCount: Int = 0,
    /** 是否启用内存映射 */
    val useMmap: Boolean = true,
    /** 是否启用GPU加速（实验性） */
    val enableGpu: Boolean = false,
    /** 日志级别 */
    val logLevel: LogLevel = LogLevel.INFO,
    /** 资源目录 */
    val resourceDir: String? = null
)
```

### 2. ModelConfig（模型配置）

```kotlin
data class ModelConfig(
    /** 模型ID */
    val modelId: String = "",
    /** 上下文窗口大小 */
    val contextWindowSize: Int = 2048,
    /** 批处理大小 */
    val batchSize: Int = 512,
    /** 是否加载到内存 */
    val loadInMemory: Boolean = false,
    /** 模型参数（如量化类型） */
    val parameters: Map<String, String> = emptyMap()
)
```

### 3. SessionConfig（会话配置）

```kotlin
data class SessionConfig(
    /** 最大生成Token数 */
    val maxTokens: Int = 512,
    /** 是否启用历史消息 */
    val enableHistory: Boolean = true,
    /** 历史消息最大条数 */
    val maxHistory: Int = 10
)
```

### 4. InferenceParams（推理参数）

```kotlin
data class InferenceParams(
    /** 温度参数 */
    val temperature: Float = 0.8f,
    /** Top-K */
    val topK: Int = 40,
    /** Top-P */
    val topP: Float = 0.95f,
    /** 重复惩罚 */
    val repeatPenalty: Float = 1.1f,
    /** 频率惩罚 */
    val frequencyPenalty: Float = 0.0f,
    /** 存在惩罚 */
    val presencePenalty: Float = 0.0f,
    /** 停止词列表 */
    val stopWords: List<String> = emptyList(),
    /** 是否流式输出 */
    val stream: Boolean = false
)
```

### 5. ModelInfo（模型信息）

```kotlin
data class ModelInfo(
    /** 模型ID */
    val modelId: String,
    /** 模型名称 */
    val name: String,
    /** 模型路径 */
    val path: String,
    /** 模型大小（字节） */
    val size: Long,
    /** 量化类型 */
    val quantType: String,
    /** 上下文窗口大小 */
    val contextWindowSize: Int,
    /** 加载时间 */
    val loadTime: Long,
    /** 是否正在使用 */
    val isInUse: Boolean
)
```

### 6. CompletionResult（完成结果）

```kotlin
data class CompletionResult(
    /** 生成的文本 */
    val text: String,
    /** 生成的Token数 */
    val tokenCount: Int,
    /** 推理耗时（毫秒） */
    val inferenceTime: Long,
    /** 是否完成 */
    val isComplete: Boolean,
    /** 是否被停止 */
    val isStopped: Boolean,
    /** 统计信息 */
    val stats: SessionStats
)
```

### 7. TokenResult（Token结果）

```kotlin
data class TokenResult(
    /** Token文本 */
    val token: String,
    /** 是否是最后一个Token */
    val isLast: Boolean,
    /** 当前生成的总文本 */
    val accumulatedText: String,
    /** 当前统计信息 */
    val stats: SessionStats
)
```

### 8. ChatMessage（对话消息）

```kotlin
data class ChatMessage(
    /** 角色 */
    val role: MessageRole,
    /** 消息内容 */
    val content: String,
    /** 时间戳 */
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole {
    /** 用户 */
    USER,
    /** 助手 */
    ASSISTANT,
    /** 系统 */
    SYSTEM
}
```

### 9. SessionStats（会话统计）

```kotlin
data class SessionStats(
    /** 推理速度（tokens/s） */
    val tokensPerSecond: Float,
    /** 已生成Token数 */
    val generatedTokens: Int,
    /** 上下文Token数 */
    val contextTokens: Int,
    /** 内存占用（字节） */
    val memoryUsage: Long,
    /** 推理耗时（毫秒） */
    val inferenceTime: Long
)
```

### 10. EngineStatus（引擎状态）

```kotlin
enum class EngineStatus {
    /** 未初始化 */
    UNINITIALIZED,
    /** 初始化中 */
    INITIALIZING,
    /** 已就绪 */
    READY,
    /** 推理中 */
    INFERENCING,
    /** 错误 */
    ERROR
}
```

### 11. LogLevel（日志级别）

```kotlin
enum class LogLevel {
    /** 详细 */
    DEBUG,
    /** 信息 */
    INFO,
    /** 警告 */
    WARN,
    /** 错误 */
    ERROR,
    /** 静默 */
    NONE
}
```

---

## ⚠️ 异常类型

### LokAIException（SDK异常）

```kotlin
sealed class LokAIException(message: String) : Exception(message) {
    /** 引擎未初始化 */
    class EngineNotInitialized(message: String = "Engine not initialized") : LokAIException(message)
    
    /** 模型加载失败 */
    class ModelLoadFailed(message: String) : LokAIException(message)
    
    /** 模型不存在 */
    class ModelNotFound(message: String) : LokAIException(message)
    
    /** 推理失败 */
    class InferenceFailed(message: String) : LokAIException(message)
    
    /** 参数无效 */
    class InvalidParameter(message: String) : LokAIException(message)
    
    /** 内存不足 */
    class OutOfMemory(message: String) : LokAIException(message)
    
    /** 不支持的操作 */
    class UnsupportedOperation(message: String) : LokAIException(message)
}
```

---

## 🔄 回调接口（可选）

### InferenceCallback（推理回调）

```kotlin
interface InferenceCallback {
    /**
     * Token生成回调
     * @param token Token文本
     */
    fun onToken(token: String)
    
    /**
     * 推理完成回调
     * @param result 完成结果
     */
    fun onComplete(result: CompletionResult)
    
    /**
     * 错误回调
     * @param exception 异常
     */
    fun onError(exception: LokAIException)
}
```

---

## 📝 使用示例

### 基本用法

```kotlin
// 初始化SDK
LokAIEngine.initialize(context)

// 加载模型
val model = LokAIEngine.getModelManager()
    .loadModel("/path/to/model.gguf")
    .getOrThrow()

// 创建会话
val session = model.createSession()

// 文本补全
val result = session.complete("Hello, ")
println(result.getOrThrow().text)

// 流式输出
session.completeStream("Hello, ")
    .collect { tokenResult ->
        print(tokenResult.token)
    }

// 关闭资源
session.close()
model.close()
```

### 聊天用法

```kotlin
val messages = listOf(
    ChatMessage(MessageRole.USER, "你好"),
    ChatMessage(MessageRole.ASSISTANT, "你好！有什么我可以帮助你的？"),
    ChatMessage(MessageRole.USER, "解释一下量子计算")
)

val result = session.chat(messages)
println(result.getOrThrow().text)
```

---

## 📝 更新日志

### v1.0.1 (2026-06-04)

**新增功能：**
- ✅ 实现流式输出功能 `completeStream()`，支持实时Token回调
- ✅ 实现会话控制功能 `stop()` 和 `reset()`
- ✅ 实现模型管理功能 `getLoadedModels()` 和 `isModelLoaded()`
- ✅ 添加 `getBosToken()` 和 `getEosToken()` 方法，从模型动态获取Token值

**修复问题：**
- ✅ 修复Tokenizer BOS/EOS Token硬编码问题
- ✅ 修复聊天模板格式不一致问题（统一使用 `<system>/<user>/<assistant>` 格式）
- ✅ 修复文本解码乱码问题（使用 `common_token_to_piece` 正确解码）
- ✅ 修复采样器链缺失导致的崩溃问题

**技术改进：**
- ✅ 使用 `channelFlow` 实现流式输出
- ✅ 添加 `StreamCallback` 接口支持流式回调
- ✅ 优化推理循环，添加停止标志检查
- ✅ 完善内存管理和资源释放

---

## 🔗 相关文档

- [SDK开发文档](./SDK_Development_Guide.md)
- [项目总览](../../doc/README.md)
- [系统架构](../../doc/architecture/system_architecture.md)

---

**最后更新**：2026-06-04
