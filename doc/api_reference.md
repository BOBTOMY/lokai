# LokAI SDK API 参考文档

## 📋 文档状态

| 项 | 值 |
|---|---|
| 文档版本 | v0.1 Draft |
| 创建日期 | 2026-06-02 |
| 状态 | 框架搭建中 |

---

## 📖 说明

本文档将在SDK开发过程中逐步完善。

详细设计请参考：[SDK_Development_Guide.md](./SDK_Development_Guide.md)

---

## 📋 API概览（v1.0）

### 1. LokAIEngine（引擎入口）

```kotlin
object LokAIEngine {
    fun initialize(context: Context, config: EngineConfig): Result<Unit>
    fun isInitialized(): Boolean
    fun getVersion(): String
    fun shutdown()
    fun getStatus(): EngineStatus
}
```

### 2. ModelManager（模型管理）

```kotlin
class ModelManager {
    fun loadModel(path: String, config: ModelConfig): Result<Model>
    fun loadModelFromAssets(context: Context, assetPath: String, config: ModelConfig): Result<Model>
    fun unloadModel(model: Model)
    fun getLoadedModels(): List<ModelInfo>
    fun isModelLoaded(modelId: String): Boolean
}
```

### 3. Model & InferenceSession

```kotlin
class Model : Closeable {
    val info: ModelInfo
    fun createSession(config: SessionConfig): InferenceSession
    fun getTokenizer(): Tokenizer
    override fun close()
}

class InferenceSession : Closeable {
    fun complete(prompt: String, params: InferenceParams): Result<CompletionResult>
    fun completeStream(prompt: String, params: InferenceParams, onToken: (String) -> Unit): Result<CompletionResult>
    fun chat(messages: List<ChatMessage>, params: InferenceParams): Result<CompletionResult>
    fun chatStream(messages: List<ChatMessage>, params: InferenceParams, onToken: (String) -> Unit): Result<CompletionResult>
    fun stop()
    fun reset()
    fun getStats(): SessionStats
    override fun close()
}
```

---

## 📝 详细说明

（待SDK开发时补充完整）

---

**最后更新**：2026-06-02
