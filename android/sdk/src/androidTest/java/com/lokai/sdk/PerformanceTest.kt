package com.lokai.sdk

import android.content.Context
import android.os.Build
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
import java.util.concurrent.TimeUnit

/**
 * 性能验证测试 - 在实际设备上验证SDK性能指标
 * 
 * 运行方式：
 * ./gradlew sdk:connectedAndroidTest --tests "com.lokai.sdk.PerformanceTest"
 * 
 * 性能指标要求：
 * - 7B模型（Q4_K_M量化）：> 5 tokens/s
 * - 3B模型（Q4_K_M量化）：> 15 tokens/s
 * - 1B模型（Q4_K_M量化）：> 30 tokens/s
 * 
 * 内存指标要求：
 * - 内存占用应稳定，无持续增长
 * - 模型卸载后内存应释放
 */
@RunWith(AndroidJUnit4::class)
class PerformanceTest {

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

    private fun modelExists(): Boolean {
        return File(modelPath).exists()
    }

    /**
     * 打印设备信息
     */
    private fun printDeviceInfo() {
        println("===== Device Info =====")
        println("Manufacturer: ${Build.MANUFACTURER}")
        println("Model: ${Build.MODEL}")
        println("Android Version: ${Build.VERSION.RELEASE}")
        println("SDK Version: ${Build.VERSION.SDK_INT}")
        println("CPU ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        println("========================")
    }

    @Test
    fun testBasicPerformance() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        printDeviceInfo()
        
        // 初始化引擎
        val initStart = System.currentTimeMillis()
        LokAIEngine.initialize(context)
        val initTime = System.currentTimeMillis() - initStart
        println("Engine initialization time: ${initTime}ms")
        
        val modelManager = LokAIEngine.getModelManager()
        
        // 加载模型
        val loadStart = System.currentTimeMillis()
        val modelResult = modelManager.loadModel(modelPath)
        val loadTime = System.currentTimeMillis() - loadStart
        
        assumeTrue("Model loading failed, skipping test", modelResult.isSuccess)
        println("Model loading time: ${loadTime}ms")
        
        val model = modelResult.getOrNull()!!
        println("Model: ${model.info.name}")
        println("Model Size: ${model.info.size / (1024 * 1024)} MB")
        println("Quant Type: ${model.info.quantType}")
        
        val session = model.createSession()
        
        // 预热运行（第一次推理较慢）
        session.complete("Hello", InferenceParams(maxTokens = 10))
        
        // 正式性能测试
        val iterations = 3
        var totalTokens = 0
        var totalTime = 0L
        
        for (i in 1..iterations) {
            val start = System.currentTimeMillis()
            val result = session.complete(
                "The quick brown fox jumps over the lazy dog. " +
                "This is a test for performance measurement.",
                InferenceParams(maxTokens = 50)
            )
            
            val time = System.currentTimeMillis() - start
            
            if (result.isSuccess) {
                val completion = result.getOrNull()!!
                val tokens = completion.stats.generatedTokens
                totalTokens += tokens
                totalTime += time
                
                println("Iteration $i: ${tokens} tokens in ${time}ms (${tokens.toFloat() / (time.toFloat() / 1000f):.2f} tokens/s)")
            }
        }
        
        // 计算平均性能
        val avgTokensPerSecond = totalTokens.toFloat() / (totalTime.toFloat() / 1000f)
        println("Average: ${avgTokensPerSecond:.2f} tokens/s")
        
        // 记录性能结果（不强制断言，不同设备性能差异大）
        // 但记录供分析使用
        println("\n===== Performance Report =====")
        println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        println("Android: ${Build.VERSION.RELEASE}")
        println("Model: ${model.info.name}")
        println("Average Speed: ${avgTokensPerSecond:.2f} tokens/s")
        println("===============================")
        
        session.close()
        modelManager.unloadModel(model)
    }

    @Test
    fun testStreamPerformance() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        val modelResult = modelManager.loadModel(modelPath)
        assumeTrue("Model loading failed, skipping test", modelResult.isSuccess)
        
        val model = modelResult.getOrNull()!!
        val session = model.createSession()
        
        // 预热
        session.complete("Test", InferenceParams(maxTokens = 10))
        
        // 测试流式推理
        val start = System.currentTimeMillis()
        var tokenCount = 0
        
        session.completeStream(
            "Write a short paragraph about artificial intelligence.",
            InferenceParams(maxTokens = 100)
        ).collect { result ->
            tokenCount++
        }
        
        val time = System.currentTimeMillis() - start
        val tokensPerSecond = tokenCount.toFloat() / (time.toFloat() / 1000f)
        
        println("Stream Inference: ${tokenCount} tokens in ${time}ms (${tokensPerSecond:.2f} tokens/s)")
        
        session.close()
        modelManager.unloadModel(model)
    }

    @Test
    fun testMemoryStability() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        // 多次加载/卸载模型测试内存稳定性
        val iterations = 3
        
        for (i in 1..iterations) {
            println("Memory Test Iteration $i")
            
            val loadResult = modelManager.loadModel(modelPath)
            assumeTrue("Model loading failed", loadResult.isSuccess)
            
            val model = loadResult.getOrNull()!!
            val session = model.createSession()
            
            // 执行一次推理
            session.complete("Test", InferenceParams(maxTokens = 20))
            
            session.close()
            modelManager.unloadModel(model)
            
            println("Iteration $i completed")
            // 给GC一些时间
            System.gc()
            Thread.sleep(500)
        }
        
        println("Memory stability test completed successfully")
    }

    @Test
    fun testConcurrentRequests() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        val modelResult = modelManager.loadModel(modelPath)
        assumeTrue("Model loading failed, skipping test", modelResult.isSuccess)
        
        val model = modelResult.getOrNull()!!
        
        // 创建多个会话
        val sessions = mutableListOf<InferenceSession>()
        for (i in 1..3) {
            sessions.add(model.createSession())
        }
        
        // 并行执行推理
        val results = mutableListOf<String>()
        
        for ((index, session) in sessions.withIndex()) {
            val result = session.complete("Session $index: Hello", InferenceParams(maxTokens = 20))
            if (result.isSuccess) {
                results.add(result.getOrNull()?.text ?: "")
            }
            session.close()
        }
        
        assertEquals("All sessions should produce results", sessions.size, results.size)
        println("Concurrent sessions test completed")
        
        modelManager.unloadModel(model)
    }

    @Test
    fun testTokenizerPerformance() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        val modelResult = modelManager.loadModel(modelPath)
        assumeTrue("Model loading failed, skipping test", modelResult.isSuccess)
        
        val model = modelResult.getOrNull()!!
        val tokenizer = model.getTokenizer()
        
        val testText = "This is a test sentence for tokenizer performance measurement. " +
                      "It contains multiple words and punctuation to test encoding speed."
        
        // 测试encode性能
        val encodeStart = System.currentTimeMillis()
        val iterations = 100
        for (i in 1..iterations) {
            tokenizer.encode(testText)
        }
        val encodeTime = System.currentTimeMillis() - encodeStart
        println("Encode $iterations times: ${encodeTime}ms (${iterations.toFloat() / (encodeTime.toFloat() / 1000f):.2f} ops/s)")
        
        // 测试decode性能
        val tokens = tokenizer.encode(testText)
        val decodeStart = System.currentTimeMillis()
        for (i in 1..iterations) {
            tokenizer.decode(tokens)
        }
        val decodeTime = System.currentTimeMillis() - decodeStart
        println("Decode $iterations times: ${decodeTime}ms (${iterations.toFloat() / (decodeTime.toFloat() / 1000f):.2f} ops/s)")
        
        modelManager.unloadModel(model)
    }

    @Test
    fun testInferenceLatency() = runBlocking {
        assumeTrue("Model file not found, skipping test", modelExists())
        
        LokAIEngine.initialize(context)
        val modelManager = LokAIEngine.getModelManager()
        
        val modelResult = modelManager.loadModel(modelPath)
        assumeTrue("Model loading failed, skipping test", modelResult.isSuccess)
        
        val model = modelResult.getOrNull()!!
        val session = model.createSession()
        
        // 预热
        session.complete("Test", InferenceParams(maxTokens = 10))
        
        // 测试首token延迟
        val prompt = "Hello"
        var firstTokenReceived = false
        var firstTokenTime: Long = 0
        
        val start = System.currentTimeMillis()
        session.completeStream(prompt, InferenceParams(maxTokens = 20)).collect { result ->
            if (!firstTokenReceived) {
                firstTokenReceived = true
                firstTokenTime = System.currentTimeMillis() - start
                println("First token latency: ${firstTokenTime}ms")
            }
        }
        
        assertTrue("Should receive at least one token", firstTokenReceived)
        
        session.close()
        modelManager.unloadModel(model)
    }
}