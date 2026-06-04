package com.lokai.sdk

import org.junit.Test
import org.junit.Assert.*

class LokAIExceptionTest {

    @Test
    fun `test engine not initialized exception`() {
        val exception = LokAIException.EngineNotInitialized()
        assertEquals("Default message should be 'Engine not initialized'", "Engine not initialized", exception.message)
    }

    @Test
    fun `test engine not initialized exception with custom message`() {
        val exception = LokAIException.EngineNotInitialized("Custom message")
        assertEquals("Message should be 'Custom message'", "Custom message", exception.message)
    }

    @Test
    fun `test model load failed exception`() {
        val exception = LokAIException.ModelLoadFailed("Failed to load model")
        assertEquals("Message should be 'Failed to load model'", "Failed to load model", exception.message)
    }

    @Test
    fun `test model not found exception`() {
        val exception = LokAIException.ModelNotFound("Model not found")
        assertEquals("Message should be 'Model not found'", "Model not found", exception.message)
    }

    @Test
    fun `test inference failed exception`() {
        val exception = LokAIException.InferenceFailed("Inference failed")
        assertEquals("Message should be 'Inference failed'", "Inference failed", exception.message)
    }

    @Test
    fun `test invalid parameter exception`() {
        val exception = LokAIException.InvalidParameter("Invalid parameter")
        assertEquals("Message should be 'Invalid parameter'", "Invalid parameter", exception.message)
    }

    @Test
    fun `test out of memory exception`() {
        val exception = LokAIException.OutOfMemory("Out of memory")
        assertEquals("Message should be 'Out of memory'", "Out of memory", exception.message)
    }

    @Test
    fun `test unsupported operation exception`() {
        val exception = LokAIException.UnsupportedOperation("Unsupported operation")
        assertEquals("Message should be 'Unsupported operation'", "Unsupported operation", exception.message)
    }

    @Test
    fun `test all exceptions extend LokAIException`() {
        val exceptions = listOf<Throwable>(
            LokAIException.EngineNotInitialized(),
            LokAIException.ModelLoadFailed("test"),
            LokAIException.ModelNotFound("test"),
            LokAIException.InferenceFailed("test"),
            LokAIException.InvalidParameter("test"),
            LokAIException.OutOfMemory("test"),
            LokAIException.UnsupportedOperation("test")
        )
        
        exceptions.forEach { exception ->
            assertTrue("All exceptions should extend LokAIException", exception is LokAIException)
        }
    }
}