package com.lokai.sdk

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object LokAIEngine {
    private const val TAG = "LokAI-Engine"
    private var isInitialized = false
    private val nativeContexts = mutableMapOf<Long, NativeContext>()
    private var nextContextId = 1L

    @Synchronized
    fun initialize(context: Context, config: EngineConfig = EngineConfig()): Result<Unit> {
        return try {
            Log.i(TAG, "Initializing LokAI Engine...")
            Log.i(TAG, "Loading native library: lokai-native")
            System.loadLibrary("lokai-native")
            isInitialized = true
            Log.i(TAG, "LokAI Engine initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            isInitialized = false
            Log.e(TAG, "Failed to initialize LokAI Engine: ${e.message}", e)
            Result.failure(LokAIException.EngineNotInitialized("Failed to load native library: ${e.message}"))
        }
    }

    fun isInitialized(): Boolean = isInitialized

    fun getVersion(): String = "1.0.0"

    @Synchronized
    fun shutdown() {
        nativeContexts.values.forEach { ctx ->
            ctx.release()
        }
        nativeContexts.clear()
        isInitialized = false
    }

    fun getStatus(): EngineStatus =
        if (isInitialized) EngineStatus.READY else EngineStatus.UNINITIALIZED

    fun getModelManager(): ModelManager = ModelManagerImpl

    @Synchronized
    internal fun createNativeContext(
        modelPath: String,
        nCtx: Int,
        nThreads: Int,
        temperature: Float,
        topK: Int,
        topP: Float,
        repeatPenalty: Float = 1.0f,
        frequencyPenalty: Float = 0.0f,
        presencePenalty: Float = 0.0f,
        penaltyLastN: Int = 64
    ): Long {
        Log.i(TAG, "createNativeContext called")
        Log.i(TAG, "Model path: $modelPath")
        Log.i(TAG, "Params: nCtx=$nCtx, nThreads=$nThreads, temperature=$temperature, topK=$topK, topP=$topP")
        Log.i(TAG, "Penalties: repeat=$repeatPenalty, freq=$frequencyPenalty, pres=$presencePenalty, lastN=$penaltyLastN")

        val contextPtr = NativeLib.createContext(
            modelPath,
            nCtx,
            nThreads,
            temperature,
            topK,
            topP,
            repeatPenalty,
            frequencyPenalty,
            presencePenalty,
            penaltyLastN
        )
        Log.i(TAG, "Native context pointer: $contextPtr")

        if (contextPtr != 0L) {
            val contextId = nextContextId++
            val nativeCtx = NativeContext(contextPtr)
            nativeContexts[contextId] = nativeCtx
            Log.i(TAG, "Native context created successfully, ID: $contextId")
            return contextId
        }
        Log.e(TAG, "Failed to create native context, pointer is 0")
        return 0L
    }

    @Synchronized
    internal fun releaseNativeContext(contextId: Long) {
        nativeContexts.remove(contextId)?.release()
    }

    internal fun getNativeContext(contextId: Long): NativeContext? = nativeContexts[contextId]
}

data class EngineConfig(
    val threadCount: Int = 0,
    val useMmap: Boolean = true,
    val enableGpu: Boolean = false,
    val logLevel: LogLevel = LogLevel.INFO,
    val resourceDir: String? = null
)

enum class EngineStatus {
    UNINITIALIZED, INITIALIZING, READY, INFERENCING, ERROR
}

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, NONE
}

class NativeContext(val contextPtr: Long) {
    fun release() {
        if (contextPtr != 0L) {
            NativeLib.freeContext(contextPtr)
        }
    }

    fun complete(prompt: String, maxTokens: Int): String {
        return NativeLib.complete(contextPtr, prompt, maxTokens)
    }

    fun completeStream(prompt: String, maxTokens: Int, callback: (String) -> Unit) {
        NativeLib.completeStream(contextPtr, prompt, maxTokens, object : StreamCallback {
            override fun onToken(token: String) {
                callback(token)
            }
        })
    }

    fun stopGeneration() {
        NativeLib.stopGeneration(contextPtr)
    }

    fun reset() {
        NativeLib.resetContext(contextPtr)
    }

    fun getContextSize(): Int = NativeLib.getContextSize(contextPtr)

    fun getMemoryUsage(): Long = NativeLib.getMemoryUsage(contextPtr)

    fun getVocabSize(): Int = NativeLib.getVocabSize(contextPtr)

    fun getModelName(): String = NativeLib.getModelName(contextPtr)

    fun getModelSize(): Long = NativeLib.getModelSize(contextPtr)

    fun getBosToken(): Int = NativeLib.getBosToken(contextPtr)

    fun getEosToken(): Int = NativeLib.getEosToken(contextPtr)

    fun tokenize(text: String): List<Int> {
        val tokenStr = NativeLib.tokenize(contextPtr, text)
        if (tokenStr.isEmpty()) return emptyList()
        return tokenStr.split(",").mapNotNull { it.toIntOrNull() }
    }

    fun detokenize(tokens: List<Int>): String {
        if (tokens.isEmpty()) return ""
        return NativeLib.detokenize(contextPtr, tokens.joinToString(","))
    }
}

object NativeLib {
    init {
        System.loadLibrary("lokai-native")
    }

    external fun createContext(
        modelPath: String,
        nCtx: Int,
        nThreads: Int,
        temperature: Float,
        topK: Int,
        topP: Float,
        repeatPenalty: Float = 1.0f,
        frequencyPenalty: Float = 0.0f,
        presencePenalty: Float = 0.0f,
        penaltyLastN: Int = 64
    ): Long

    external fun complete(contextPtr: Long, prompt: String, maxTokens: Int): String

    external fun completeStream(contextPtr: Long, prompt: String, maxTokens: Int, callback: StreamCallback): Unit

    external fun stopGeneration(contextPtr: Long): Unit

    external fun resetContext(contextPtr: Long): Unit

    external fun freeContext(contextPtr: Long)

    external fun getContextSize(contextPtr: Long): Int

    external fun getMemoryUsage(contextPtr: Long): Long

    external fun getVocabSize(contextPtr: Long): Int

    external fun getModelName(contextPtr: Long): String

    external fun getModelSize(contextPtr: Long): Long

    external fun getBosToken(contextPtr: Long): Int

    external fun getEosToken(contextPtr: Long): Int

    external fun tokenize(contextPtr: Long, text: String): String

    external fun detokenize(contextPtr: Long, tokens: String): String
}

interface StreamCallback {
    fun onToken(token: String)
}

object ModelManagerImpl : ModelManager {
    private val TAG = "LokAI-ModelManager"
    private val loadedModels = mutableListOf<ModelInfo>()

    override suspend fun loadModel(path: String, config: ModelConfig): Result<Model> {
        Log.i(TAG, "loadModel called with path: $path")
        Log.i(TAG, "ModelConfig: $config")

        return try {
            Log.i(TAG, "Creating native context...")
            val contextId = LokAIEngine.createNativeContext(
                modelPath = path,
                nCtx = config.contextWindowSize,
                nThreads = Runtime.getRuntime().availableProcessors(),
                temperature = 0.8f,
                topK = 40,
                topP = 0.95f,
                repeatPenalty = 1.1f,
                frequencyPenalty = 0.0f,
                presencePenalty = 0.0f,
                penaltyLastN = 64
            )
            Log.i(TAG, "Native context ID: $contextId")

            if (contextId == 0L) {
                Log.e(TAG, "Failed to create native context, contextId is 0")
                return Result.failure(LokAIException.ModelLoadFailed("Failed to load model from $path"))
            }

            Log.i(TAG, "Getting native context for ID: $contextId")
            val nativeCtx = LokAIEngine.getNativeContext(contextId)
                ?: run {
                    Log.e(TAG, "Failed to get native context for ID: $contextId")
                    return Result.failure(LokAIException.ModelLoadFailed("Failed to get native context"))
                }

            val modelId = config.modelId.ifEmpty { path.substringAfterLast("/").substringBefore(".") }
            val modelInfo = ModelInfo(
                modelId = modelId,
                name = path.substringAfterLast("/"),
                path = path,
                size = nativeCtx.getModelSize(),
                quantType = "gguf",
                contextWindowSize = nativeCtx.getContextSize(),
                loadTime = System.currentTimeMillis(),
                isInUse = true
            )

            synchronized(loadedModels) {
                loadedModels.add(modelInfo)
            }
            Log.i(TAG, "Model added to loaded models list, total: ${loadedModels.size}")

            Result.success(ModelImpl(contextId, modelInfo))
        } catch (e: Exception) {
            Result.failure(LokAIException.ModelLoadFailed(e.message ?: "Unknown error"))
        }
    }

    override suspend fun loadModelFromAssets(context: Context, assetPath: String, config: ModelConfig): Result<Model> {
        return try {
            val assetManager = context.assets
            val modelFile = File(context.cacheDir, "models/${assetPath.substringAfterLast("/")}")

            modelFile.parentFile?.mkdirs()

            assetManager.open(assetPath).use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }

            loadModel(modelFile.absolutePath, config)
        } catch (e: Exception) {
            Result.failure(LokAIException.ModelLoadFailed("Failed to load model from assets: ${e.message}"))
        }
    }

    override fun unloadModel(model: Model) {
        synchronized(loadedModels) {
            loadedModels.removeIf { it.modelId == model.info.modelId }
        }
        Log.i(TAG, "Model removed from loaded models list, total: ${loadedModels.size}")
        model.close()
    }

    override fun getLoadedModels(): List<ModelInfo> {
        synchronized(loadedModels) {
            return loadedModels.toList()
        }
    }

    override fun isModelLoaded(modelId: String): Boolean {
        synchronized(loadedModels) {
            return loadedModels.any { it.modelId == modelId }
        }
    }
}

interface ModelManager {
    suspend fun loadModel(path: String, config: ModelConfig = ModelConfig()): Result<Model>
    suspend fun loadModelFromAssets(context: Context, assetPath: String, config: ModelConfig = ModelConfig()): Result<Model>
    fun unloadModel(model: Model)
    fun getLoadedModels(): List<ModelInfo>
    fun isModelLoaded(modelId: String): Boolean
}

data class ModelConfig(
    val modelId: String = "",
    val contextWindowSize: Int = 2048,
    val batchSize: Int = 512,
    val loadInMemory: Boolean = false,
    val parameters: Map<String, String> = emptyMap()
)

class ModelImpl(
    private val contextId: Long,
    override val info: ModelInfo
) : Model {

    override fun createSession(config: SessionConfig): InferenceSession {
        return InferenceSessionImpl(contextId, this, config)
    }

    override fun getTokenizer(): Tokenizer {
        val nativeCtx = LokAIEngine.getNativeContext(contextId)
            ?: throw LokAIException.EngineNotInitialized()
        return TokenizerImpl(contextId, nativeCtx)
    }

    override fun close() {
        LokAIEngine.releaseNativeContext(contextId)
    }
}

interface Model : AutoCloseable {
    val info: ModelInfo
    fun createSession(config: SessionConfig = SessionConfig()): InferenceSession
    fun getTokenizer(): Tokenizer
    override fun close()
}

data class ModelInfo(
    val modelId: String,
    val name: String,
    val path: String,
    val size: Long,
    val quantType: String,
    val contextWindowSize: Int,
    val loadTime: Long,
    val isInUse: Boolean
)

data class SessionConfig(
    val maxTokens: Int = 512,
    val enableHistory: Boolean = true,
    val maxHistory: Int = 10
)

class InferenceSessionImpl(
    private val contextId: Long,
    private val model: Model,
    private val config: SessionConfig
) : InferenceSession {

    private var nativeCtx: NativeContext? = null
    private val startTime: Long = System.currentTimeMillis()

    init {
        nativeCtx = LokAIEngine.getNativeContext(contextId)
    }

    override fun complete(prompt: String, params: InferenceParams): Result<CompletionResult> {
        return try {
            val ctx = nativeCtx ?: return Result.failure(LokAIException.EngineNotInitialized())

            val inferenceStart = System.currentTimeMillis()
            val result = ctx.complete(prompt, params.maxTokens.takeIf { it > 0 } ?: config.maxTokens)
            val inferenceTime = System.currentTimeMillis() - inferenceStart

            val tokens = result.length / 4
            val tokensPerSecond = if (inferenceTime > 0) {
                tokens.toFloat() / (inferenceTime.toFloat() / 1000f)
            } else 0f

            Result.success(CompletionResult(
                text = result,
                tokenCount = tokens,
                inferenceTime = inferenceTime,
                isComplete = true,
                isStopped = false,
                stats = SessionStats(
                    tokensPerSecond = tokensPerSecond,
                    generatedTokens = tokens,
                    contextTokens = prompt.length,
                    memoryUsage = ctx.getMemoryUsage(),
                    inferenceTime = inferenceTime
                )
            ))
        } catch (e: Exception) {
            Result.failure(LokAIException.InferenceFailed(e.message ?: "Unknown error"))
        }
    }

    override fun completeStream(prompt: String, params: InferenceParams): kotlinx.coroutines.flow.Flow<TokenResult> {
        return kotlinx.coroutines.flow.channelFlow {
            val ctx = nativeCtx ?: throw LokAIException.EngineNotInitialized()
            val inferenceStart = System.currentTimeMillis()
            var accumulatedText = ""
            var generatedTokens = 0

            ctx.completeStream(prompt, params.maxTokens.takeIf { it > 0 } ?: config.maxTokens) { token ->
                accumulatedText += token
                generatedTokens++
                val inferenceTime = System.currentTimeMillis() - inferenceStart
                val tokensPerSecond = if (inferenceTime > 0) {
                    generatedTokens.toFloat() / (inferenceTime.toFloat() / 1000f)
                } else 0f

                trySend(TokenResult(
                    token = token,
                    isLast = false,
                    accumulatedText = accumulatedText,
                    stats = SessionStats(
                        tokensPerSecond = tokensPerSecond,
                        generatedTokens = generatedTokens,
                        contextTokens = prompt.length,
                        memoryUsage = ctx.getMemoryUsage(),
                        inferenceTime = inferenceTime
                    )
                ))
            }

            val totalTime = System.currentTimeMillis() - inferenceStart
            val finalTokensPerSecond = if (totalTime > 0) {
                generatedTokens.toFloat() / (totalTime.toFloat() / 1000f)
            } else 0f

            send(TokenResult(
                token = "",
                isLast = true,
                accumulatedText = accumulatedText,
                stats = SessionStats(
                    tokensPerSecond = finalTokensPerSecond,
                    generatedTokens = generatedTokens,
                    contextTokens = prompt.length,
                    memoryUsage = ctx.getMemoryUsage(),
                    inferenceTime = totalTime
                )
            ))
        }
    }

    override fun chat(messages: List<ChatMessage>, params: InferenceParams): Result<CompletionResult> {
        val prompt = buildChatPrompt(messages)
        return complete(prompt, params)
    }

    override fun chatStream(messages: List<ChatMessage>, params: InferenceParams): kotlinx.coroutines.flow.Flow<TokenResult> {
        return completeStream(buildChatPrompt(messages), params)
    }

    private fun buildChatPrompt(messages: List<ChatMessage>): String {
        val builder = StringBuilder()
        messages.forEach { msg ->
            when (msg.role) {
                MessageRole.SYSTEM -> builder.append("<system>${msg.content}</system>\n")
                MessageRole.USER -> builder.append("<user>${msg.content}</user>\n")
                MessageRole.ASSISTANT -> builder.append("<assistant>${msg.content}</assistant>\n")
            }
        }
        builder.append("<assistant>")
        return builder.toString()
    }

    override fun stop() {
        nativeCtx?.stopGeneration()
    }

    override fun reset() {
        nativeCtx?.reset()
    }

    override fun getStats(): SessionStats {
        val ctx = nativeCtx
        return SessionStats(
            tokensPerSecond = 0f,
            generatedTokens = 0,
            contextTokens = 0,
            memoryUsage = ctx?.getMemoryUsage() ?: 0L,
            inferenceTime = System.currentTimeMillis() - startTime
        )
    }

    override fun close() {
        nativeCtx = null
    }
}

interface InferenceSession : AutoCloseable {
    fun complete(prompt: String, params: InferenceParams = InferenceParams()): Result<CompletionResult>
    fun completeStream(prompt: String, params: InferenceParams = InferenceParams()): kotlinx.coroutines.flow.Flow<TokenResult>
    fun chat(messages: List<ChatMessage>, params: InferenceParams = InferenceParams()): Result<CompletionResult>
    fun chatStream(messages: List<ChatMessage>, params: InferenceParams = InferenceParams()): kotlinx.coroutines.flow.Flow<TokenResult>
    fun stop()
    fun reset()
    fun getStats(): SessionStats
    override fun close()
}

data class InferenceParams(
    val temperature: Float = 0.8f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val repeatPenalty: Float = 1.1f,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val stopWords: List<String> = emptyList(),
    val stream: Boolean = false,
    val maxTokens: Int = 512
)

data class CompletionResult(
    val text: String,
    val tokenCount: Int,
    val inferenceTime: Long,
    val isComplete: Boolean,
    val isStopped: Boolean,
    val stats: SessionStats
)

data class TokenResult(
    val token: String,
    val isLast: Boolean,
    val accumulatedText: String,
    val stats: SessionStats
)

data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

data class SessionStats(
    val tokensPerSecond: Float,
    val generatedTokens: Int,
    val contextTokens: Int,
    val memoryUsage: Long,
    val inferenceTime: Long
)

class TokenizerImpl(
    private val contextId: Long,
    private val nativeCtx: NativeContext
) : Tokenizer {
    override fun encode(text: String): List<Int> {
        return nativeCtx.tokenize(text)
    }

    override fun decode(tokens: List<Int>): String {
        return nativeCtx.detokenize(tokens)
    }

    override fun getVocabSize(): Int = nativeCtx.getVocabSize()

    override fun getBosToken(): Int = nativeCtx.getBosToken()

    override fun getEosToken(): Int = nativeCtx.getEosToken()
}

interface Tokenizer {
    fun encode(text: String): List<Int>
    fun decode(tokens: List<Int>): String
    fun getVocabSize(): Int
    fun getBosToken(): Int
    fun getEosToken(): Int
}

sealed class LokAIException(message: String) : Exception(message) {
    class EngineNotInitialized(message: String = "Engine not initialized") : LokAIException(message)
    class ModelLoadFailed(message: String) : LokAIException(message)
    class ModelNotFound(message: String) : LokAIException(message)
    class InferenceFailed(message: String) : LokAIException(message)
    class InvalidParameter(message: String) : LokAIException(message)
    class OutOfMemory(message: String) : LokAIException(message)
    class UnsupportedOperation(message: String) : LokAIException(message)
}
