package com.lokai.sdk

import org.junit.Test
import org.junit.Assert.*

/**
 * 参数边界和验证测试
 */
class ParameterValidationTest {

    // ========== EngineConfig 边界测试 ==========

    @Test
    fun `test engine config with negative thread count`() {
        val config = EngineConfig(threadCount = -1)
        assertEquals("Negative thread count should be preserved", -1, config.threadCount)
        // 实际使用时，负值会被Native层处理为自动检测
    }

    @Test
    fun `test engine config with zero thread count`() {
        val config = EngineConfig(threadCount = 0)
        assertEquals("Zero thread count means auto-detect", 0, config.threadCount)
    }

    @Test
    fun `test engine config with large thread count`() {
        val config = EngineConfig(threadCount = 100)
        assertEquals("Large thread count should be preserved", 100, config.threadCount)
    }

    @Test
    fun `test engine config with empty resource dir`() {
        val config = EngineConfig(resourceDir = "")
        assertEquals("Empty resource dir should be preserved", "", config.resourceDir)
    }

    @Test
    fun `test engine config with null resource dir`() {
        val config = EngineConfig(resourceDir = null)
        assertNull("Null resource dir should be preserved", config.resourceDir)
    }

    // ========== ModelConfig 边界测试 ==========

    @Test
    fun `test model config with zero context window`() {
        val config = ModelConfig(contextWindowSize = 0)
        assertEquals("Zero context window should be preserved", 0, config.contextWindowSize)
        // 实际使用时可能导致错误
    }

    @Test
    fun `test model config with very large context window`() {
        val config = ModelConfig(contextWindowSize = 100000)
        assertEquals("Very large context window should be preserved", 100000, config.contextWindowSize)
    }

    @Test
    fun `test model config with zero batch size`() {
        val config = ModelConfig(batchSize = 0)
        assertEquals("Zero batch size should be preserved", 0, config.batchSize)
    }

    @Test
    fun `test model config with empty model id`() {
        val config = ModelConfig(modelId = "")
        assertEquals("Empty model id should be preserved", "", config.modelId)
    }

    @Test
    fun `test model config with very long model id`() {
        val longId = "model_" + "x".repeat(1000)
        val config = ModelConfig(modelId = longId)
        assertEquals("Very long model id should be preserved", longId, config.modelId)
    }

    @Test
    fun `test model config with empty parameters`() {
        val config = ModelConfig(parameters = emptyMap())
        assertTrue("Empty parameters map should be preserved", config.parameters.isEmpty())
    }

    @Test
    fun `test model config with many parameters`() {
        val params = mutableMapOf<String, String>()
        for (i in 1..100) {
            params["key_$i"] = "value_$i"
        }
        val config = ModelConfig(parameters = params)
        assertEquals("Many parameters should be preserved", 100, config.parameters.size)
    }

    // ========== SessionConfig 边界测试 ==========

    @Test
    fun `test session config with zero max tokens`() {
        val config = SessionConfig(maxTokens = 0)
        assertEquals("Zero max tokens should be preserved", 0, config.maxTokens)
    }

    @Test
    fun `test session config with negative max tokens`() {
        val config = SessionConfig(maxTokens = -100)
        assertEquals("Negative max tokens should be preserved", -100, config.maxTokens)
    }

    @Test
    fun `test session config with very large max tokens`() {
        val config = SessionConfig(maxTokens = 1000000)
        assertEquals("Very large max tokens should be preserved", 1000000, config.maxTokens)
    }

    @Test
    fun `test session config with zero max history`() {
        val config = SessionConfig(maxHistory = 0)
        assertEquals("Zero max history should be preserved", 0, config.maxHistory)
    }

    @Test
    fun `test session config with disabled history`() {
        val config = SessionConfig(enableHistory = false, maxHistory = 10)
        assertFalse("History should be disabled", config.enableHistory)
        assertEquals("Max history value should still be preserved", 10, config.maxHistory)
    }

    // ========== InferenceParams 边界测试 ==========

    @Test
    fun `test inference params with zero temperature`() {
        val params = InferenceParams(temperature = 0.0f)
        assertEquals("Zero temperature should be preserved", 0.0f, params.temperature, 0.001f)
    }

    @Test
    fun `test inference params with very high temperature`() {
        val params = InferenceParams(temperature = 10.0f)
        assertEquals("Very high temperature should be preserved", 10.0f, params.temperature, 0.001f)
    }

    @Test
    fun `test inference params with negative temperature`() {
        val params = InferenceParams(temperature = -1.0f)
        assertEquals("Negative temperature should be preserved", -1.0f, params.temperature, 0.001f)
    }

    @Test
    fun `test inference params with zero topK`() {
        val params = InferenceParams(topK = 0)
        assertEquals("Zero topK should be preserved", 0, params.topK)
    }

    @Test
    fun `test inference params with negative topK`() {
        val params = InferenceParams(topK = -1)
        assertEquals("Negative topK should be preserved", -1, params.topK)
    }

    @Test
    fun `test inference params with zero topP`() {
        val params = InferenceParams(topP = 0.0f)
        assertEquals("Zero topP should be preserved", 0.0f, params.topP, 0.001f)
    }

    @Test
    fun `test inference params with topP greater than 1`() {
        val params = InferenceParams(topP = 1.5f)
        assertEquals("TopP > 1 should be preserved", 1.5f, params.topP, 0.001f)
    }

    @Test
    fun `test inference params with negative penalties`() {
        val params = InferenceParams(
            repeatPenalty = -1.0f,
            frequencyPenalty = -0.5f,
            presencePenalty = -0.3f
        )
        assertEquals("Negative repeatPenalty should be preserved", -1.0f, params.repeatPenalty, 0.001f)
        assertEquals("Negative frequencyPenalty should be preserved", -0.5f, params.frequencyPenalty, 0.001f)
        assertEquals("Negative presencePenalty should be preserved", -0.3f, params.presencePenalty, 0.001f)
    }

    @Test
    fun `test inference params with many stop words`() {
        val stopWords = mutableListOf<String>()
        for (i in 1..50) {
            stopWords.add("stop_$i")
        }
        val params = InferenceParams(stopWords = stopWords)
        assertEquals("Many stop words should be preserved", 50, params.stopWords.size)
    }

    @Test
    fun `test inference params with empty stop words`() {
        val params = InferenceParams(stopWords = emptyList())
        assertTrue("Empty stop words should be preserved", params.stopWords.isEmpty())
    }

    @Test
    fun `test inference params with zero max tokens`() {
        val params = InferenceParams(maxTokens = 0)
        assertEquals("Zero max tokens should be preserved", 0, params.maxTokens)
    }

    // ========== ChatMessage 边界测试 ==========

    @Test
    fun `test chat message with empty content`() {
        val message = ChatMessage(role = MessageRole.USER, content = "")
        assertEquals("Empty content should be preserved", "", message.content)
    }

    @Test
    fun `test chat message with very long content`() {
        val longContent = "Hello " + "world ".repeat(1000)
        val message = ChatMessage(role = MessageRole.USER, content = longContent)
        assertEquals("Very long content should be preserved", longContent, message.content)
    }

    @Test
    fun `test chat message with special characters`() {
        val specialContent = "Hello\n\t\r\"'<>&@#$%^*()"
        val message = ChatMessage(role = MessageRole.USER, content = specialContent)
        assertEquals("Special characters should be preserved", specialContent, message.content)
    }

    @Test
    fun `test chat message with unicode content`() {
        val unicodeContent = "你好世界 🎉 こんにちは"
        val message = ChatMessage(role = MessageRole.USER, content = unicodeContent)
        assertEquals("Unicode content should be preserved", unicodeContent, message.content)
    }

    @Test
    fun `test chat message with negative timestamp`() {
        val message = ChatMessage(role = MessageRole.USER, content = "test", timestamp = -1L)
        assertEquals("Negative timestamp should be preserved", -1L, message.timestamp)
    }

    // ========== SessionStats 边界测试 ==========

    @Test
    fun `test session stats with zero values`() {
        val stats = SessionStats(
            tokensPerSecond = 0.0f,
            generatedTokens = 0,
            contextTokens = 0,
            memoryUsage = 0L,
            inferenceTime = 0L
        )
        assertEquals("Zero tokensPerSecond", 0.0f, stats.tokensPerSecond, 0.001f)
        assertEquals("Zero generatedTokens", 0, stats.generatedTokens)
        assertEquals("Zero contextTokens", 0, stats.contextTokens)
        assertEquals("Zero memoryUsage", 0L, stats.memoryUsage)
        assertEquals("Zero inferenceTime", 0L, stats.inferenceTime)
    }

    @Test
    fun `test session stats with very large values`() {
        val stats = SessionStats(
            tokensPerSecond = 1000000.0f,
            generatedTokens = 10000000,
            contextTokens = 10000000,
            memoryUsage = Long.MAX_VALUE,
            inferenceTime = Long.MAX_VALUE
        )
        assertEquals("Very large tokensPerSecond", 1000000.0f, stats.tokensPerSecond, 0.001f)
        assertEquals("Very large generatedTokens", 10000000, stats.generatedTokens)
        assertEquals("Very large memoryUsage", Long.MAX_VALUE, stats.memoryUsage)
    }

    @Test
    fun `test session stats with negative tokens per second`() {
        val stats = SessionStats(tokensPerSecond = -5.0f, generatedTokens = 0, contextTokens = 0, memoryUsage = 0L, inferenceTime = 0L)
        assertEquals("Negative tokensPerSecond should be preserved", -5.0f, stats.tokensPerSecond, 0.001f)
    }
}