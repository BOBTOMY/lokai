package com.lokai.sdk

import org.junit.Test
import org.junit.Assert.*

/**
 * 数据类完整性测试
 * 测试data类的copy、equals、hashCode、toString等功能
 */
class DataClassIntegrityTest {

    // ========== EngineConfig 测试 ==========

    @Test
    fun `test engine config copy`() {
        val original = EngineConfig(
            threadCount = 4,
            useMmap = false,
            enableGpu = true,
            logLevel = LogLevel.DEBUG,
            resourceDir = "/path"
        )
        
        val copy = original.copy(threadCount = 8)
        
        assertEquals("threadCount should be changed", 8, copy.threadCount)
        assertEquals("useMmap should be preserved", false, copy.useMmap)
        assertEquals("enableGpu should be preserved", true, copy.enableGpu)
        assertEquals("logLevel should be preserved", LogLevel.DEBUG, copy.logLevel)
        assertEquals("resourceDir should be preserved", "/path", copy.resourceDir)
    }

    @Test
    fun `test engine config equality`() {
        val config1 = EngineConfig(threadCount = 4, useMmap = true)
        val config2 = EngineConfig(threadCount = 4, useMmap = true)
        val config3 = EngineConfig(threadCount = 8, useMmap = true)
        
        assertEquals("Same configs should be equal", config1, config2)
        assertNotEquals("Different configs should not be equal", config1, config3)
    }

    @Test
    fun `test engine config hashCode`() {
        val config1 = EngineConfig(threadCount = 4, useMmap = true)
        val config2 = EngineConfig(threadCount = 4, useMmap = true)
        
        assertEquals("Same configs should have same hashCode", config1.hashCode(), config2.hashCode())
    }

    @Test
    fun `test engine config toString`() {
        val config = EngineConfig(threadCount = 4, logLevel = LogLevel.DEBUG)
        val str = config.toString()
        
        assertTrue("toString should contain threadCount", str.contains("4"))
        assertTrue("toString should contain logLevel", str.contains("DEBUG"))
    }

    // ========== ModelConfig 测试 ==========

    @Test
    fun `test model config copy`() {
        val original = ModelConfig(
            modelId = "test",
            contextWindowSize = 4096,
            batchSize = 1024,
            loadInMemory = true,
            parameters = mapOf("key" to "value")
        )
        
        val copy = original.copy(modelId = "new")
        
        assertEquals("modelId should be changed", "new", copy.modelId)
        assertEquals("contextWindowSize should be preserved", 4096, copy.contextWindowSize)
        assertEquals("parameters should be preserved", 1, copy.parameters.size)
    }

    @Test
    fun `test model config equality`() {
        val config1 = ModelConfig(modelId = "test", contextWindowSize = 2048)
        val config2 = ModelConfig(modelId = "test", contextWindowSize = 2048)
        val config3 = ModelConfig(modelId = "test", contextWindowSize = 4096)
        
        assertEquals("Same configs should be equal", config1, config2)
        assertNotEquals("Different configs should not be equal", config1, config3)
    }

    @Test
    fun `test model config with different parameters`() {
        val config1 = ModelConfig(parameters = mapOf("a" to "1"))
        val config2 = ModelConfig(parameters = mapOf("b" to "2"))
        
        assertNotEquals("Different parameters should not be equal", config1, config2)
    }

    @Test
    fun `test model config hashCode consistency`() {
        val config = ModelConfig(modelId = "test", contextWindowSize = 2048)
        val hashCode1 = config.hashCode()
        val hashCode2 = config.hashCode()
        
        assertEquals("hashCode should be consistent", hashCode1, hashCode2)
    }

    // ========== ModelInfo 测试 ==========

    @Test
    fun `test model info copy`() {
        val original = ModelInfo(
            modelId = "model1",
            name = "test.gguf",
            path = "/path/test.gguf",
            size = 1024L,
            quantType = "Q4_0",
            contextWindowSize = 2048,
            loadTime = 1234567890L,
            isInUse = true
        )
        
        val copy = original.copy(isInUse = false)
        
        assertEquals("modelId should be preserved", "model1", copy.modelId)
        assertEquals("isInUse should be changed", false, copy.isInUse)
        assertEquals("size should be preserved", 1024L, copy.size)
    }

    @Test
    fun `test model info equality`() {
        val info1 = ModelInfo(
            modelId = "model1",
            name = "test.gguf",
            path = "/path",
            size = 1024L,
            quantType = "Q4_0",
            contextWindowSize = 2048,
            loadTime = 1000L,
            isInUse = true
        )
        
        val info2 = ModelInfo(
            modelId = "model1",
            name = "test.gguf",
            path = "/path",
            size = 1024L,
            quantType = "Q4_0",
            contextWindowSize = 2048,
            loadTime = 1000L,
            isInUse = true
        )
        
        val info3 = info1.copy(size = 2048L)
        
        assertEquals("Same info should be equal", info1, info2)
        assertNotEquals("Different size should not be equal", info1, info3)
    }

    @Test
    fun `test model info hashCode`() {
        val info1 = ModelInfo(
            modelId = "model1",
            name = "test.gguf",
            path = "/path",
            size = 1024L,
            quantType = "Q4_0",
            contextWindowSize = 2048,
            loadTime = 1000L,
            isInUse = true
        )
        
        val info2 = info1.copy()
        
        assertEquals("Same info should have same hashCode", info1.hashCode(), info2.hashCode())
    }

    @Test
    fun `test model info toString`() {
        val info = ModelInfo(
            modelId = "model1",
            name = "test.gguf",
            path = "/path",
            size = 1024L,
            quantType = "Q4_0",
            contextWindowSize = 2048,
            loadTime = 1000L,
            isInUse = true
        )
        
        val str = info.toString()
        
        assertTrue("toString should contain modelId", str.contains("model1"))
        assertTrue("toString should contain name", str.contains("test.gguf"))
        assertTrue("toString should contain path", str.contains("/path"))
    }

    // ========== SessionConfig 测试 ==========

    @Test
    fun `test session config copy`() {
        val original = SessionConfig(
            maxTokens = 1024,
            enableHistory = false,
            maxHistory = 5
        )
        
        val copy = original.copy(maxTokens = 512)
        
        assertEquals("maxTokens should be changed", 512, copy.maxTokens)
        assertEquals("enableHistory should be preserved", false, copy.enableHistory)
        assertEquals("maxHistory should be preserved", 5, copy.maxHistory)
    }

    @Test
    fun `test session config equality`() {
        val config1 = SessionConfig(maxTokens = 512, enableHistory = true)
        val config2 = SessionConfig(maxTokens = 512, enableHistory = true)
        val config3 = SessionConfig(maxTokens = 1024, enableHistory = true)
        
        assertEquals("Same configs should be equal", config1, config2)
        assertNotEquals("Different configs should not be equal", config1, config3)
    }

    // ========== InferenceParams 测试 ==========

    @Test
    fun `test inference params copy`() {
        val original = InferenceParams(
            temperature = 0.7f,
            topK = 50,
            topP = 0.9f,
            repeatPenalty = 1.2f,
            frequencyPenalty = 0.1f,
            presencePenalty = 0.2f,
            stopWords = listOf("<end>"),
            stream = true,
            maxTokens = 1024
        )
        
        val copy = original.copy(temperature = 0.5f)
        
        assertEquals("temperature should be changed", 0.5f, copy.temperature, 0.001f)
        assertEquals("topK should be preserved", 50, copy.topK)
        assertEquals("stopWords should be preserved", 1, copy.stopWords.size)
        assertEquals("stream should be preserved", true, copy.stream)
    }

    @Test
    fun `test inference params equality`() {
        val params1 = InferenceParams(temperature = 0.8f, topK = 40)
        val params2 = InferenceParams(temperature = 0.8f, topK = 40)
        val params3 = InferenceParams(temperature = 0.9f, topK = 40)
        
        assertEquals("Same params should be equal", params1, params2)
        assertNotEquals("Different params should not be equal", params1, params3)
    }

    @Test
    fun `test inference params with different stop words`() {
        val params1 = InferenceParams(stopWords = listOf("a", "b"))
        val params2 = InferenceParams(stopWords = listOf("a", "c"))
        
        assertNotEquals("Different stop words should not be equal", params1, params2)
    }

    @Test
    fun `test inference params hashCode`() {
        val params = InferenceParams(temperature = 0.8f, topK = 40)
        val hashCode1 = params.hashCode()
        val hashCode2 = params.hashCode()
        
        assertEquals("hashCode should be consistent", hashCode1, hashCode2)
    }

    // ========== SessionStats 测试 ==========

    @Test
    fun `test session stats copy`() {
        val original = SessionStats(
            tokensPerSecond = 10.5f,
            generatedTokens = 200,
            contextTokens = 100,
            memoryUsage = 1024L * 1024L,
            inferenceTime = 20000L
        )
        
        val copy = original.copy(tokensPerSecond = 15.0f)
        
        assertEquals("tokensPerSecond should be changed", 15.0f, copy.tokensPerSecond, 0.001f)
        assertEquals("generatedTokens should be preserved", 200, copy.generatedTokens)
        assertEquals("memoryUsage should be preserved", 1024L * 1024L, copy.memoryUsage)
    }

    @Test
    fun `test session stats equality`() {
        val stats1 = SessionStats(
            tokensPerSecond = 10.0f,
            generatedTokens = 100,
            contextTokens = 50,
            memoryUsage = 1024L,
            inferenceTime = 1000L
        )
        
        val stats2 = SessionStats(
            tokensPerSecond = 10.0f,
            generatedTokens = 100,
            contextTokens = 50,
            memoryUsage = 1024L,
            inferenceTime = 1000L
        )
        
        val stats3 = stats1.copy(tokensPerSecond = 20.0f)
        
        assertEquals("Same stats should be equal", stats1, stats2)
        assertNotEquals("Different stats should not be equal", stats1, stats3)
    }

    @Test
    fun `test session stats hashCode`() {
        val stats = SessionStats(
            tokensPerSecond = 10.0f,
            generatedTokens = 100,
            contextTokens = 50,
            memoryUsage = 1024L,
            inferenceTime = 1000L
        )
        
        val hashCode1 = stats.hashCode()
        val hashCode2 = stats.hashCode()
        
        assertEquals("hashCode should be consistent", hashCode1, hashCode2)
    }

    @Test
    fun `test session stats toString`() {
        val stats = SessionStats(
            tokensPerSecond = 10.0f,
            generatedTokens = 100,
            contextTokens = 50,
            memoryUsage = 1024L,
            inferenceTime = 1000L
        )
        
        val str = stats.toString()
        
        assertTrue("toString should contain tokensPerSecond", str.contains("10.0"))
        assertTrue("toString should contain generatedTokens", str.contains("100"))
    }

    // ========== CompletionResult 测试 ==========

    @Test
    fun `test completion result equality`() {
        val stats = SessionStats(
            tokensPerSecond = 5.0f,
            generatedTokens = 10,
            contextTokens = 5,
            memoryUsage = 1024L,
            inferenceTime = 2000L
        )
        
        val result1 = CompletionResult(
            text = "Hello",
            tokenCount = 10,
            inferenceTime = 2000L,
            isComplete = true,
            isStopped = false,
            stats = stats
        )
        
        val result2 = CompletionResult(
            text = "Hello",
            tokenCount = 10,
            inferenceTime = 2000L,
            isComplete = true,
            isStopped = false,
            stats = stats
        )
        
        val result3 = result1.copy(text = "World")
        
        assertEquals("Same results should be equal", result1, result2)
        assertNotEquals("Different results should not be equal", result1, result3)
    }

    @Test
    fun `test completion result hashCode`() {
        val stats = SessionStats(
            tokensPerSecond = 5.0f,
            generatedTokens = 10,
            contextTokens = 5,
            memoryUsage = 1024L,
            inferenceTime = 2000L
        )
        
        val result = CompletionResult(
            text = "Hello",
            tokenCount = 10,
            inferenceTime = 2000L,
            isComplete = true,
            isStopped = false,
            stats = stats
        )
        
        val hashCode1 = result.hashCode()
        val hashCode2 = result.hashCode()
        
        assertEquals("hashCode should be consistent", hashCode1, hashCode2)
    }

    // ========== TokenResult 测试 ==========

    @Test
    fun `test token result equality`() {
        val stats = SessionStats(
            tokensPerSecond = 5.0f,
            generatedTokens = 1,
            contextTokens = 10,
            memoryUsage = 1024L,
            inferenceTime = 200L
        )
        
        val result1 = TokenResult(
            token = "Hello",
            isLast = false,
            accumulatedText = "Hello",
            stats = stats
        )
        
        val result2 = TokenResult(
            token = "Hello",
            isLast = false,
            accumulatedText = "Hello",
            stats = stats
        )
        
        val result3 = result1.copy(token = "World")
        
        assertEquals("Same results should be equal", result1, result2)
        assertNotEquals("Different results should not be equal", result1, result3)
    }

    @Test
    fun `test token result hashCode`() {
        val stats = SessionStats(
            tokensPerSecond = 5.0f,
            generatedTokens = 1,
            contextTokens = 10,
            memoryUsage = 1024L,
            inferenceTime = 200L
        )
        
        val result = TokenResult(
            token = "Hello",
            isLast = false,
            accumulatedText = "Hello",
            stats = stats
        )
        
        val hashCode1 = result.hashCode()
        val hashCode2 = result.hashCode()
        
        assertEquals("hashCode should be consistent", hashCode1, hashCode2)
    }

    // ========== 边界情况测试 ==========

    @Test
    fun `test data class with null values`() {
        val config = EngineConfig(resourceDir = null)
        assertNull("resourceDir can be null", config.resourceDir)
        
        val copy = config.copy(resourceDir = "/path")
        assertEquals("copy can change null to value", "/path", copy.resourceDir)
    }

    @Test
    fun `test data class component functions`() {
        val config = EngineConfig(threadCount = 4, useMmap = true)
        
        val (threadCount, useMmap, enableGpu, logLevel, resourceDir) = config
        
        assertEquals("component1 should be threadCount", 4, threadCount)
        assertEquals("component2 should be useMmap", true, useMmap)
        assertEquals("component3 should be enableGpu", false, enableGpu)
        assertEquals("component4 should be logLevel", LogLevel.INFO, logLevel)
        assertNull("component5 should be resourceDir", resourceDir)
    }
}