package com.lokai.sdk

import org.junit.Test
import org.junit.Assert.*

class InferenceSessionTest {

    @Test
    fun `test inference params defaults`() {
        val params = InferenceParams()
        assertEquals("Default temperature should be 0.8", 0.8f, params.temperature, 0.001f)
        assertEquals("Default topK should be 40", 40, params.topK)
        assertEquals("Default topP should be 0.95", 0.95f, params.topP, 0.001f)
        assertEquals("Default repeatPenalty should be 1.1", 1.1f, params.repeatPenalty, 0.001f)
        assertEquals("Default frequencyPenalty should be 0.0", 0.0f, params.frequencyPenalty, 0.001f)
        assertEquals("Default presencePenalty should be 0.0", 0.0f, params.presencePenalty, 0.001f)
        assertTrue("Default stopWords should be empty", params.stopWords.isEmpty())
        assertFalse("Default stream should be false", params.stream)
        assertEquals("Default maxTokens should be 512", 512, params.maxTokens)
    }

    @Test
    fun `test inference params custom values`() {
        val stopWords = listOf("<end>", "[EOS]")
        val params = InferenceParams(
            temperature = 0.7f,
            topK = 50,
            topP = 0.9f,
            repeatPenalty = 1.2f,
            frequencyPenalty = 0.1f,
            presencePenalty = 0.2f,
            stopWords = stopWords,
            stream = true,
            maxTokens = 1024
        )
        assertEquals("temperature should be 0.7", 0.7f, params.temperature, 0.001f)
        assertEquals("topK should be 50", 50, params.topK)
        assertEquals("topP should be 0.9", 0.9f, params.topP, 0.001f)
        assertEquals("repeatPenalty should be 1.2", 1.2f, params.repeatPenalty, 0.001f)
        assertEquals("frequencyPenalty should be 0.1", 0.1f, params.frequencyPenalty, 0.001f)
        assertEquals("presencePenalty should be 0.2", 0.2f, params.presencePenalty, 0.001f)
        assertEquals("stopWords should contain 2 entries", 2, params.stopWords.size)
        assertTrue("stopWords should contain <end>", params.stopWords.contains("<end>"))
        assertTrue("stopWords should contain [EOS]", params.stopWords.contains("[EOS]"))
        assertTrue("stream should be true", params.stream)
        assertEquals("maxTokens should be 1024", 1024, params.maxTokens)
    }

    @Test
    fun `test completion result data class`() {
        val stats = SessionStats(
            tokensPerSecond = 5.5f,
            generatedTokens = 100,
            contextTokens = 50,
            memoryUsage = 1024L * 1024L * 512,
            inferenceTime = 18181818L
        )
        val result = CompletionResult(
            text = "Hello, this is a test completion.",
            tokenCount = 100,
            inferenceTime = 18181818L,
            isComplete = true,
            isStopped = false,
            stats = stats
        )
        assertEquals("text should match", "Hello, this is a test completion.", result.text)
        assertEquals("tokenCount should be 100", 100, result.tokenCount)
        assertEquals("inferenceTime should be 18181818", 18181818L, result.inferenceTime)
        assertTrue("isComplete should be true", result.isComplete)
        assertFalse("isStopped should be false", result.isStopped)
        assertNotNull("stats should not be null", result.stats)
        assertEquals("stats.tokensPerSecond should be 5.5", 5.5f, result.stats.tokensPerSecond, 0.001f)
    }

    @Test
    fun `test token result data class`() {
        val stats = SessionStats(
            tokensPerSecond = 5.0f,
            generatedTokens = 1,
            contextTokens = 10,
            memoryUsage = 512L * 1024L * 1024,
            inferenceTime = 200L
        )
        val result = TokenResult(
            token = "Hello",
            isLast = false,
            accumulatedText = "Hello",
            stats = stats
        )
        assertEquals("token should be Hello", "Hello", result.token)
        assertFalse("isLast should be false", result.isLast)
        assertEquals("accumulatedText should be Hello", "Hello", result.accumulatedText)
        assertNotNull("stats should not be null", result.stats)
    }

    @Test
    fun `test chat message data class`() {
        val message = ChatMessage(
            role = MessageRole.USER,
            content = "Hello AI",
            timestamp = 1234567890L
        )
        assertEquals("role should be USER", MessageRole.USER, message.role)
        assertEquals("content should be Hello AI", "Hello AI", message.content)
        assertEquals("timestamp should be 1234567890", 1234567890L, message.timestamp)
    }

    @Test
    fun `test message role enum values`() {
        val roles = MessageRole.values()
        assertEquals("Should have 3 roles", 3, roles.size)
        assertTrue("Should contain USER", roles.contains(MessageRole.USER))
        assertTrue("Should contain ASSISTANT", roles.contains(MessageRole.ASSISTANT))
        assertTrue("Should contain SYSTEM", roles.contains(MessageRole.SYSTEM))
    }

    @Test
    fun `test session stats data class`() {
        val stats = SessionStats(
            tokensPerSecond = 10.5f,
            generatedTokens = 200,
            contextTokens = 100,
            memoryUsage = 1024L * 1024L * 1024,
            inferenceTime = 20000000L
        )
        assertEquals("tokensPerSecond should be 10.5", 10.5f, stats.tokensPerSecond, 0.001f)
        assertEquals("generatedTokens should be 200", 200, stats.generatedTokens)
        assertEquals("contextTokens should be 100", 100, stats.contextTokens)
        assertEquals("memoryUsage should be 1GB", 1024L * 1024L * 1024, stats.memoryUsage)
        assertEquals("inferenceTime should be 20000000", 20000000L, stats.inferenceTime)
    }
}