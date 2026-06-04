package com.lokai.sdk

import org.junit.Test
import org.junit.Assert.*

/**
 * 错误处理场景测试
 */
class ErrorHandlingTest {

    // ========== 异常类型检查 ==========

    @Test
    fun `test exception hierarchy`() {
        val engineException = LokAIException.EngineNotInitialized()
        val modelException = LokAIException.ModelLoadFailed("test")
        val inferenceException = LokAIException.InferenceFailed("test")

        assertTrue("EngineNotInitialized should be LokAIException", engineException is LokAIException)
        assertTrue("ModelLoadFailed should be LokAIException", modelException is LokAIException)
        assertTrue("InferenceFailed should be LokAIException", inferenceException is LokAIException)

        assertTrue("All should be Exception", engineException is Exception)
        assertTrue("All should be Throwable", engineException is Throwable)
    }

    @Test
    fun `test exception message formats`() {
        val exceptions = listOf(
            LokAIException.EngineNotInitialized() to "Engine not initialized",
            LokAIException.EngineNotInitialized("Custom: engine failed") to "Custom: engine failed",
            LokAIException.ModelLoadFailed("model.gguf") to "model.gguf",
            LokAIException.ModelNotFound("id-123") to "id-123",
            LokAIException.InferenceFailed("timeout") to "timeout",
            LokAIException.InvalidParameter("temperature") to "temperature",
            LokAIException.OutOfMemory("OOM") to "OOM",
            LokAIException.UnsupportedOperation("GPU") to "GPU"
        )

        exceptions.forEach { (exception, expectedMessage) ->
            assertEquals("Message should match", expectedMessage, exception.message)
        }
    }

    @Test
    fun `test exception with null message handling`() {
        // Kotlin不允许null message，但测试边界情况
        val exception = LokAIException.EngineNotInitialized("")
        assertEquals("Empty message should be preserved", "", exception.message)
    }

    @Test
    fun `test exception with multiline message`() {
        val multilineMessage = "Error occurred:\nLine 1\nLine 2\nLine 3"
        val exception = LokAIException.ModelLoadFailed(multilineMessage)
        assertEquals("Multiline message should be preserved", multilineMessage, exception.message)
    }

    @Test
    fun `test exception with special characters in message`() {
        val specialMessage = "Error: <tag> & \"quote\" 'single' \n\t\r"
        val exception = LokAIException.InferenceFailed(specialMessage)
        assertEquals("Special characters should be preserved", specialMessage, exception.message)
    }

    // ========== Result 类型测试 ==========

    @Test
    fun `test result success`() {
        val result: Result<String> = Result.success("test")
        assertTrue("Result should be success", result.isSuccess)
        assertFalse("Result should not be failure", result.isFailure)
        assertEquals("Value should be test", "test", result.getOrNull())
    }

    @Test
    fun `test result failure`() {
        val exception = LokAIException.InferenceFailed("test error")
        val result: Result<String> = Result.failure(exception)
        assertFalse("Result should not be success", result.isSuccess)
        assertTrue("Result should be failure", result.isFailure)
        assertNull("Value should be null", result.getOrNull())
        assertTrue("Exception should be InferenceFailed", result.exceptionOrNull() is LokAIException.InferenceFailed)
    }

    @Test
    fun `test result getOrElse`() {
        val successResult: Result<String> = Result.success("value")
        val failureResult: Result<String> = Result.failure(LokAIException.EngineNotInitialized())

        assertEquals("Success should return value", "value", successResult.getOrElse { "default" })
        assertEquals("Failure should return default", "default", failureResult.getOrElse { "default" })
    }

    @Test
    fun `test result getOrThrow`() {
        val successResult: Result<String> = Result.success("value")
        val failureResult: Result<String> = Result.failure(LokAIException.EngineNotInitialized())

        assertEquals("Success should return value", "value", successResult.getOrThrow())
        
        try {
            failureResult.getOrThrow()
            fail("Should throw exception")
        } catch (e: LokAIException.EngineNotInitialized) {
            // Expected
        }
    }

    @Test
    fun `test result map`() {
        val result: Result<Int> = Result.success(10)
        val mapped = result.map { it * 2 }
        
        assertTrue("Mapped should be success", mapped.isSuccess)
        assertEquals("Mapped value should be 20", 20, mapped.getOrNull())
    }

    @Test
    fun `test result map on failure`() {
        val result: Result<Int> = Result.failure(LokAIException.EngineNotInitialized())
        val mapped = result.map { it * 2 }
        
        assertTrue("Mapped should still be failure", mapped.isFailure)
        assertTrue("Exception should be preserved", mapped.exceptionOrNull() is LokAIException.EngineNotInitialized)
    }

    @Test
    fun `test result onSuccess callback`() {
        var called = false
        var receivedValue = ""
        
        Result.success("test").onSuccess { value ->
            called = true
            receivedValue = value
        }
        
        assertTrue("onSuccess should be called", called)
        assertEquals("Value should be test", "test", receivedValue)
    }

    @Test
    fun `test result onSuccess not called on failure`() {
        var called = false
        
        Result.failure<Unit>(LokAIException.EngineNotInitialized()).onSuccess {
            called = true
        }
        
        assertFalse("onSuccess should not be called on failure", called)
    }

    @Test
    fun `test result onFailure callback`() {
        var called = false
        var receivedException: Throwable? = null
        
        Result.failure<Unit>(LokAIException.ModelLoadFailed("test")).onFailure { exception ->
            called = true
            receivedException = exception
        }
        
        assertTrue("onFailure should be called", called)
        assertTrue("Exception should be ModelLoadFailed", receivedException is LokAIException.ModelLoadFailed)
    }

    @Test
    fun `test result onFailure not called on success`() {
        var called = false
        
        Result.success("test").onFailure {
            called = true
        }
        
        assertFalse("onFailure should not be called on success", called)
    }

    // ========== 异常捕获场景测试 ==========

    @Test
    fun `test catching LokAIException`() {
        val exceptions = listOf(
            LokAIException.EngineNotInitialized(),
            LokAIException.ModelLoadFailed("test"),
            LokAIException.ModelNotFound("test"),
            LokAIException.InferenceFailed("test"),
            LokAIException.InvalidParameter("test"),
            LokAIException.OutOfMemory("test"),
            LokAIException.UnsupportedOperation("test")
        )

        exceptions.forEach { exception ->
            try {
                throw exception
            } catch (e: LokAIException) {
                // Should catch all LokAIException subclasses
                assertTrue("Should catch LokAIException", e is LokAIException)
            }
        }
    }

    @Test
    fun `test exception cause chain`() {
        val cause = RuntimeException("Original error")
        val exception = LokAIException.ModelLoadFailed("Failed to load")
        // Note: LokAIException doesn't support cause in current design
        
        assertNull("Cause should be null in current design", exception.cause)
    }

    // ========== 边界异常场景 ==========

    @Test
    fun `test very long exception message`() {
        val longMessage = "Error: " + "x".repeat(10000)
        val exception = LokAIException.InferenceFailed(longMessage)
        assertEquals("Very long message should be preserved", longMessage.length, exception.message?.length ?: 0)
    }

    @Test
    fun `test exception equality`() {
        val ex1 = LokAIException.EngineNotInitialized("test")
        val ex2 = LokAIException.EngineNotInitialized("test")
        
        // Exceptions are not equal by default
        assertNotSame("Exceptions should not be same instance", ex1, ex2)
    }
}