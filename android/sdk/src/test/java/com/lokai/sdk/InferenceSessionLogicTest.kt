package com.lokai.sdk

import org.junit.Test
import org.junit.Assert.*

/**
 * InferenceSession 内部逻辑测试
 * 测试不依赖Native层的方法
 */
class InferenceSessionLogicTest {

    // ========== Chat Prompt 构建测试 ==========

    @Test
    fun `test build chat prompt with single user message`() {
        val messages = listOf(
            ChatMessage(role = MessageRole.USER, content = "Hello")
        )
        val prompt = buildTestChatPrompt(messages)
        
        assertEquals("Prompt should contain user tag", "<user>Hello</user>\n<assistant>", prompt)
    }

    @Test
    fun `test build chat prompt with system message`() {
        val messages = listOf(
            ChatMessage(role = MessageRole.SYSTEM, content = "You are an AI assistant")
        )
        val prompt = buildTestChatPrompt(messages)
        
        assertEquals("Prompt should contain system tag", "<system>You are an AI assistant</system>\n<assistant>", prompt)
    }

    @Test
    fun `test build chat prompt with assistant message`() {
        val messages = listOf(
            ChatMessage(role = MessageRole.ASSISTANT, content = "Hi there")
        )
        val prompt = buildTestChatPrompt(messages)
        
        assertEquals("Prompt should contain assistant tag", "<assistant>Hi there</assistant>\n<assistant>", prompt)
    }

    @Test
    fun `test build chat prompt with multiple messages`() {
        val messages = listOf(
            ChatMessage(role = MessageRole.SYSTEM, content = "Be helpful"),
            ChatMessage(role = MessageRole.USER, content = "Hello"),
            ChatMessage(role = MessageRole.ASSISTANT, content = "Hi"),
            ChatMessage(role = MessageRole.USER, content = "How are you?")
        )
        val prompt = buildTestChatPrompt(messages)
        
        val expected = "<system>Be helpful</system>\n<user>Hello</user>\n<assistant>Hi</assistant>\n<user>How are you?</user>\n<assistant>"
        assertEquals("Prompt should contain all messages in order", expected, prompt)
    }

    @Test
    fun `test build chat prompt with empty content`() {
        val messages = listOf(
            ChatMessage(role = MessageRole.USER, content = "")
        )
        val prompt = buildTestChatPrompt(messages)
        
        assertEquals("Empty content should still create tags", "<user></user>\n<assistant>", prompt)
    }

    @Test
    fun `test build chat prompt with special characters`() {
        val messages = listOf(
            ChatMessage(role = MessageRole.USER, content = "Hello\n\tWorld<test>")
        )
        val prompt = buildTestChatPrompt(messages)
        
        assertTrue("Prompt should preserve special characters", prompt.contains("Hello\n\tWorld<test>"))
    }

    @Test
    fun `test build chat prompt with unicode`() {
        val messages = listOf(
            ChatMessage(role = MessageRole.USER, content = "你好 🎉 こんにちは")
        )
        val prompt = buildTestChatPrompt(messages)
        
        assertTrue("Prompt should preserve unicode", prompt.contains("你好 🎉 こんにちは"))
    }

    @Test
    fun `test build chat prompt with very long content`() {
        val longContent = "Hello " + "world ".repeat(1000)
        val messages = listOf(
            ChatMessage(role = MessageRole.USER, content = longContent)
        )
        val prompt = buildTestChatPrompt(messages)
        
        assertTrue("Prompt should contain full long content", prompt.contains(longContent))
    }

    @Test
    fun `test build chat prompt with many messages`() {
        val messages = mutableListOf<ChatMessage>()
        for (i in 1..100) {
            messages.add(ChatMessage(role = MessageRole.USER, content = "Message $i"))
        }
        val prompt = buildTestChatPrompt(messages)
        
        assertTrue("Prompt should contain all 100 messages", prompt.contains("Message 1"))
        assertTrue("Prompt should contain last message", prompt.contains("Message 100"))
    }

    // ========== MessageRole 测试 ==========

    @Test
    fun `test message role values`() {
        val roles = MessageRole.values()
        assertEquals("Should have 3 roles", 3, roles.size)
        
        assertTrue("Should contain USER", roles.contains(MessageRole.USER))
        assertTrue("Should contain ASSISTANT", roles.contains(MessageRole.ASSISTANT))
        assertTrue("Should contain SYSTEM", roles.contains(MessageRole.SYSTEM))
    }

    @Test
    fun `test message role ordinal`() {
        assertEquals("USER ordinal", 0, MessageRole.USER.ordinal)
        assertEquals("ASSISTANT ordinal", 1, MessageRole.ASSISTANT.ordinal)
        assertEquals("SYSTEM ordinal", 2, MessageRole.SYSTEM.ordinal)
    }

    @Test
    fun `test message role name`() {
        assertEquals("USER name", "USER", MessageRole.USER.name)
        assertEquals("ASSISTANT name", "ASSISTANT", MessageRole.ASSISTANT.name)
        assertEquals("SYSTEM name", "SYSTEM", MessageRole.SYSTEM.name)
    }

    // ========== ChatMessage 测试 ==========

    @Test
    fun `test chat message default timestamp`() {
        val before = System.currentTimeMillis()
        val message = ChatMessage(role = MessageRole.USER, content = "test")
        val after = System.currentTimeMillis()
        
        assertTrue("Timestamp should be between before and after", 
            message.timestamp >= before && message.timestamp <= after)
    }

    @Test
    fun `test chat message copy`() {
        val original = ChatMessage(role = MessageRole.USER, content = "Hello", timestamp = 1000L)
        val copy = original.copy(content = "World")
        
        assertEquals("Role should be preserved", MessageRole.USER, copy.role)
        assertEquals("Content should be changed", "World", copy.content)
        assertEquals("Timestamp should be preserved", 1000L, copy.timestamp)
    }

    @Test
    fun `test chat message equality`() {
        val msg1 = ChatMessage(role = MessageRole.USER, content = "Hello", timestamp = 1000L)
        val msg2 = ChatMessage(role = MessageRole.USER, content = "Hello", timestamp = 1000L)
        val msg3 = ChatMessage(role = MessageRole.USER, content = "World", timestamp = 1000L)
        
        assertEquals("Same messages should be equal", msg1, msg2)
        assertNotEquals("Different content should not be equal", msg1, msg3)
    }

    @Test
    fun `test chat message hashCode`() {
        val msg1 = ChatMessage(role = MessageRole.USER, content = "Hello", timestamp = 1000L)
        val msg2 = ChatMessage(role = MessageRole.USER, content = "Hello", timestamp = 1000L)
        
        assertEquals("Same messages should have same hashCode", msg1.hashCode(), msg2.hashCode())
    }

    @Test
    fun `test chat message toString`() {
        val message = ChatMessage(role = MessageRole.USER, content = "Hello", timestamp = 1000L)
        val str = message.toString()
        
        assertTrue("toString should contain role", str.contains("USER"))
        assertTrue("toString should contain content", str.contains("Hello"))
        assertTrue("toString should contain timestamp", str.contains("1000"))
    }

    // ========== CompletionResult 测试 ==========

    @Test
    fun `test completion result copy`() {
        val original = CompletionResult(
            text = "Hello",
            tokenCount = 10,
            inferenceTime = 100L,
            isComplete = true,
            isStopped = false,
            stats = SessionStats(tokensPerSecond = 5.0f, generatedTokens = 10, contextTokens = 5, memoryUsage = 1024L, inferenceTime = 100L)
        )
        
        val copy = original.copy(text = "World")
        
        assertEquals("Text should be changed", "World", copy.text)
        assertEquals("Other fields should be preserved", 10, copy.tokenCount)
        assertEquals("Stats should be preserved", 5.0f, copy.stats.tokensPerSecond, 0.001f)
    }

    @Test
    fun `test completion result with null stats`() {
        // Note: CompletionResult.stats is non-null in current design
        // This test verifies the design
        val result = CompletionResult(
            text = "test",
            tokenCount = 5,
            inferenceTime = 50L,
            isComplete = true,
            isStopped = false,
            stats = SessionStats(tokensPerSecond = 0.0f, generatedTokens = 0, contextTokens = 0, memoryUsage = 0L, inferenceTime = 0L)
        )
        
        assertNotNull("Stats should not be null", result.stats)
    }

    // ========== TokenResult 测试 ==========

    @Test
    fun `test token result isLast flag`() {
        val normalToken = TokenResult(
            token = "Hello",
            isLast = false,
            accumulatedText = "Hello",
            stats = SessionStats(tokensPerSecond = 5.0f, generatedTokens = 1, contextTokens = 10, memoryUsage = 1024L, inferenceTime = 200L)
        )
        
        val lastToken = TokenResult(
            token = "",
            isLast = true,
            accumulatedText = "Hello World",
            stats = SessionStats(tokensPerSecond = 5.0f, generatedTokens = 2, contextTokens = 10, memoryUsage = 1024L, inferenceTime = 400L)
        )
        
        assertFalse("Normal token should not be last", normalToken.isLast)
        assertTrue("Last token should be last", lastToken.isLast)
    }

    @Test
    fun `test token result accumulated text`() {
        val token1 = TokenResult(
            token = "Hello",
            isLast = false,
            accumulatedText = "Hello",
            stats = SessionStats(tokensPerSecond = 0.0f, generatedTokens = 0, contextTokens = 0, memoryUsage = 0L, inferenceTime = 0L)
        )
        
        val token2 = TokenResult(
            token = " World",
            isLast = false,
            accumulatedText = "Hello World",
            stats = SessionStats(tokensPerSecond = 0.0f, generatedTokens = 0, contextTokens = 0, memoryUsage = 0L, inferenceTime = 0L)
        )
        
        assertEquals("First token accumulated", "Hello", token1.accumulatedText)
        assertEquals("Second token accumulated", "Hello World", token2.accumulatedText)
    }

    // ========== 辅助方法 ==========

    /**
     * 复制InferenceSessionImpl的buildChatPrompt逻辑用于测试
     */
    private fun buildTestChatPrompt(messages: List<ChatMessage>): String {
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
}