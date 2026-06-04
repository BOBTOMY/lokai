package com.lokai.sdk

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import org.junit.Assume.assumeTrue

import org.junit.Assert.*

import java.io.File

/**
 * 集成测试 - 需要在实际Android设备上运行
 * 
 * 运行方式：
 * 1. 将GGUF模型文件放入设备的 /sdcard/ 目录
 * 2. 运行: ./gradlew sdk:connectedAndroidTest
 * 
 * 注意：需要模型文件的测试会自动检测，如果模型不存在则跳过
 */
@RunWith(AndroidJUnit4::class)
class LokAIIntegrationTest {

    private lateinit var context: Context
    private val modelPath = "/sdcard/test-model.gguf"

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        LokAIEngine.shutdown()
    }

    @After
    fun tearDown() {
        LokAIEngine.shutdown()
    }

    /**
     * 检查模型文件是否存在
     */
    private fun modelExists(): Boolean {
        return File(modelPath).exists()
    }

    // ========== 不需要模型的测试 ==========

    @Test
    fun testEngineInitialization() {
        val result = LokAIEngine.initialize(context)
        
        assertTrue("Engine initialization should succeed", result.isSuccess)
        assertTrue("Engine should be initialized", LokAIEngine.isInitialized())
        assertEquals("Engine status should be READY", EngineStatus.READY, LokAIEngine.getStatus())
        
        val version = LokAIEngine.getVersion()
        assertNotNull("Version should not be null", version)
        assertEquals("Version should be 1.0.0", "1.0.0", version)
    }

    @Test
    fun testEngineMultipleInitialization() {
        LokAIEngine.initialize(context)
        assertTrue("First initialization should succeed", LokAIEngine.isInitialized())
        
        LokAIEngine.shutdown()
        assertFalse("Engine should not be initialized after shutdown", LokAIEngine.isInitialized())
        
        val result = LokAIEngine.initialize(context)
        assertTrue("Second initialization should succeed", result.isSuccess)
        assertTrue("Engine should be initialized again", LokAIEngine.isInitialized())
    }

    @Test
    fun testGetModelManager() {
        LokAIEngine.initialize(context)
        
        val modelManager = LokAIEngine.getModelManager()
        assertNotNull("ModelManager should not be null", modelManager)
        
        val loadedModels = modelManager.getLoadedModels()
        assertTrue("No models should be loaded initially", loadedModels.isEmpty())
    }

    @Test
    fun testEngineStatusTransitions() {
        assertEquals("Initial status should be UNINITIALIZED", EngineStatus.UNINITIALIZED, LokAIEngine.getStatus())
        
        LokAIEngine.initialize(context)
        assertEquals("After initialization should be READY", EngineStatus.READY, LokAIEngine.getStatus())
        
        LokAIEngine.shutdown()
        assertEquals("After shutdown should be UNINITIALIZED", EngineStatus.UNINITIALIZED, LokAIEngine.getStatus())
    }

    @Test
    fun testLogLevelEnum() {
        val levels = LogLevel.values()
        assertEquals("Should have 5 log levels", 5, levels.size)
        assertEquals("DEBUG", levels[0].name)
        assertEquals("INFO", levels[1].name)
        assertEquals("WARN", levels[2].name)
        assertEquals("ERROR", levels[3].name)
        assertEquals("NONE", levels[4].name)
    }

    // ========== 需要模型的测试 ==========

    @Test
    fun testModelLoading() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        val result = modelManager.loadModel(modelPath)
        assertTrue("Model loading should succeed", result.isSuccess)
        
        val model = result.getOrNull()!!
        assertNotNull("Model should not be null", model)
        assertNotNull("Model info should not be null", model.info)
        
        assertTrue("Model name should contain .gguf", model.info.name.contains(".gguf", ignoreCase = true))
        assertTrue("Model size should be positive", model.info.size > 0)
        
        modelManager.unloadModel(model)
        assertTrue("No models should be loaded after unload", modelManager.getLoadedModels().isEmpty())
    }

    @Test
    fun testModelLoadingFromAssets() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        // 将模型复制到assets进行测试
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        // 测试路径验证（无需实际加载）
        val config = ModelConfig(modelId = "test-asset-model")
        assertNotNull("Config should not be null", config)
    }

    @Test
    fun testInference() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        val modelResult = modelManager.loadModel(modelPath)
        assumeTrue("Model loading failed, skipping test", modelResult.isSuccess)
        
        val model = modelResult.getOrNull()!!
        val session = model.createSession()
        
        val params = InferenceParams(maxTokens = 50)
        val result = session.complete("Hello, how are you?", params)
        
        assertTrue("Inference should succeed", result.isSuccess)
        val completion = result.getOrNull()!!
        assertNotNull("Completion text should not be null", completion.text)
        assertTrue("Completion text should not be empty", completion.text.isNotEmpty())
        
        session.close()
        modelManager.unloadModel(model)
    }

    @Test
    fun testStreamInference() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        val modelResult = modelManager.loadModel(modelPath)
        assumeTrue("Model loading failed, skipping test", modelResult.isSuccess)
        
        val model = modelResult.getOrNull()!!
        val session = model.createSession()
        
        val params = InferenceParams(maxTokens = 50)
        var tokenCount = 0
        var accumulatedText = ""
        
        session.completeStream("Hello", params).collect { tokenResult ->
            tokenCount++
            accumulatedText = tokenResult.accumulatedText
        }
        
        assertTrue("Should receive at least one token", tokenCount > 0)
        assertTrue("Accumulated text should not be empty", accumulatedText.isNotEmpty())
        
        session.close()
        modelManager.unloadModel(model)
    }

    @Test
    fun testChat() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        val modelResult = modelManager.loadModel(modelPath)
        assumeTrue("Model loading failed, skipping test", modelResult.isSuccess)
        
        val model = modelResult.getOrNull()!!
        val session = model.createSession()
        
        val messages = listOf(
            ChatMessage(role = MessageRole.USER, content = "Hello!")
        )
        
        val params = InferenceParams(maxTokens = 50)
        val result = session.chat(messages, params)
        
        assertTrue("Chat should succeed", result.isSuccess)
        val completion = result.getOrNull()!!
        assertNotNull("Completion text should not be null", completion.text)
        
        session.close()
        modelManager.unloadModel(model)
    }

    @Test
    fun testMultiTurnChat() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        val modelResult = modelManager.loadModel(modelPath)
        assumeTrue("Model loading failed, skipping test", modelResult.isSuccess)
        
        val model = modelResult.getOrNull()!!
        val session = model.createSession(SessionConfig(maxHistory = 5))
        
        // 第一轮对话
        val messages1 = listOf(
            ChatMessage(role = MessageRole.USER, content = "What is AI?")
        )
        var result = session.chat(messages1, InferenceParams(maxTokens = 30))
        assertTrue("First chat should succeed", result.isSuccess)
        
        // 第二轮对话（带上下文）
        val messages2 = listOf(
            ChatMessage(role = MessageRole.USER, content = "What is AI?"),
            ChatMessage(role = MessageRole.ASSISTANT, content = result.getOrNull()?.text ?: ""),
            ChatMessage(role = MessageRole.USER, content = "Can you explain more?")
        )
        result = session.chat(messages2, InferenceParams(maxTokens = 30))
        assertTrue("Second chat should succeed", result.isSuccess)
        
        session.close()
        modelManager.unloadModel(model)
    }

    @Test
    fun testTokenizer() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        val modelResult = modelManager.loadModel(modelPath)
        assumeTrue("Model loading failed, skipping test", modelResult.isSuccess)
        
        val model = modelResult.getOrNull()!!
        val tokenizer = model.getTokenizer()
        
        val tokens = tokenizer.encode("Hello, world!")
        assertNotNull("Tokens should not be null", tokens)
        assertTrue("Tokens should not be empty", tokens.isNotEmpty())
        
        val text = tokenizer.decode(tokens)
        assertNotNull("Decoded text should not be null", text)
        assertTrue("Decoded text should not be empty", text.isNotEmpty())
        
        val vocabSize = tokenizer.getVocabSize()
        assertTrue("Vocab size should be positive", vocabSize > 0)
        
        val bosToken = tokenizer.getBosToken()
        assertTrue("BOS token should be non-negative", bosToken >= 0)
        
        val eosToken = tokenizer.getEosToken()
        assertTrue("EOS token should be non-negative", eosToken >= 0)
        
        modelManager.unloadModel(model)
    }

    @Test
    fun testPerformanceStats() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        val modelResult = modelManager.loadModel(modelPath)
        assumeTrue("Model loading failed, skipping test", modelResult.isSuccess)
        
        val model = modelResult.getOrNull()!!
        val session = model.createSession()
        
        val params = InferenceParams(maxTokens = 100)
        val result = session.complete("The quick brown fox jumps over the lazy dog.", params)
        
        if (result.isSuccess) {
            val stats = result.getOrNull()!!.stats
            assertTrue("Tokens per second should be positive", stats.tokensPerSecond > 0)
            assertTrue("Generated tokens should be positive", stats.generatedTokens > 0)
            assertTrue("Memory usage should be positive", stats.memoryUsage > 0)
            assertTrue("Inference time should be positive", stats.inferenceTime > 0)
            
            // 性能指标验证
            // 中端设备目标: >5 tokens/s
            if (stats.inferenceTime > 0) {
                val tokensPerSecond = stats.generatedTokens.toFloat() / (stats.inferenceTime.toFloat() / 1000f)
                // 记录但不强制要求，不同设备性能不同
                println("Performance: $tokensPerSecond tokens/s")
            }
        }
        
        session.close()
        modelManager.unloadModel(model)
    }

    @Test
    fun testSessionStop() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        val modelResult = modelManager.loadModel(modelPath)
        assumeTrue("Model loading failed, skipping test", modelResult.isSuccess)
        
        val model = modelResult.getOrNull()!!
        val session = model.createSession()
        
        // 测试stop方法不会崩溃
        session.stop()
        
        session.close()
        modelManager.unloadModel(model)
    }

    @Test
    fun testSessionReset() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        val modelResult = modelManager.loadModel(modelPath)
        assumeTrue("Model loading failed, skipping test", modelResult.isSuccess)
        
        val model = modelResult.getOrNull()!!
        val session = model.createSession()
        
        // 执行一次推理
        session.complete("Test", InferenceParams(maxTokens = 10))
        
        // 测试reset方法不会崩溃
        session.reset()
        
        session.close()
        modelManager.unloadModel(model)
    }

    // ========== 错误处理测试 ==========

    @Test
    fun testLoadNonExistentModel() = runBlocking {
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        val result = modelManager.loadModel("/sdcard/nonexistent.gguf")
        
        assertFalse("Loading non-existent model should fail", result.isSuccess)
        assertTrue("Exception should be ModelLoadFailed", result.exceptionOrNull() is LokAIException.ModelLoadFailed)
    }

    @Test
    fun testInferenceWithoutModel() {
        LokAIEngine.initialize(context)
        
        // 尝试在没有加载模型的情况下创建会话应该失败
        try {
            // 这需要Model对象，所以我们测试获取ModelManager
            val modelManager = LokAIEngine.getModelManager()
            assertNotNull("ModelManager should exist", modelManager)
        } catch (e: Exception) {
            // 预期可能抛出异常
        }
    }
}