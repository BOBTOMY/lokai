package com.lokai.sdk

import org.junit.Test
import org.junit.Assert.*

class ModelManagerTest {

    @Test
    fun `test model config defaults`() {
        val config = ModelConfig()
        assertEquals("Default modelId should be empty", "", config.modelId)
        assertEquals("Default contextWindowSize should be 2048", 2048, config.contextWindowSize)
        assertEquals("Default batchSize should be 512", 512, config.batchSize)
        assertFalse("Default loadInMemory should be false", config.loadInMemory)
        assertTrue("Default parameters should be empty", config.parameters.isEmpty())
    }

    @Test
    fun `test model config custom values`() {
        val params = mapOf("key1" to "value1", "key2" to "value2")
        val config = ModelConfig(
            modelId = "test-model",
            contextWindowSize = 4096,
            batchSize = 1024,
            loadInMemory = true,
            parameters = params
        )
        assertEquals("modelId should be test-model", "test-model", config.modelId)
        assertEquals("contextWindowSize should be 4096", 4096, config.contextWindowSize)
        assertEquals("batchSize should be 1024", 1024, config.batchSize)
        assertTrue("loadInMemory should be true", config.loadInMemory)
        assertEquals("parameters should contain 2 entries", 2, config.parameters.size)
        assertEquals("parameters should contain key1", "value1", config.parameters["key1"])
        assertEquals("parameters should contain key2", "value2", config.parameters["key2"])
    }

    @Test
    fun `test model info data class`() {
        val info = ModelInfo(
            modelId = "model1",
            name = "test-model.gguf",
            path = "/path/to/model.gguf",
            size = 1024L,
            quantType = "Q4_0",
            contextWindowSize = 2048,
            loadTime = 1234567890L,
            isInUse = true
        )
        assertEquals("modelId should be model1", "model1", info.modelId)
        assertEquals("name should be test-model.gguf", "test-model.gguf", info.name)
        assertEquals("path should be /path/to/model.gguf", "/path/to/model.gguf", info.path)
        assertEquals("size should be 1024", 1024L, info.size)
        assertEquals("quantType should be Q4_0", "Q4_0", info.quantType)
        assertEquals("contextWindowSize should be 2048", 2048, info.contextWindowSize)
        assertEquals("loadTime should be 1234567890", 1234567890L, info.loadTime)
        assertTrue("isInUse should be true", info.isInUse)
    }

    @Test
    fun `test session config defaults`() {
        val config = SessionConfig()
        assertEquals("Default maxTokens should be 512", 512, config.maxTokens)
        assertTrue("Default enableHistory should be true", config.enableHistory)
        assertEquals("Default maxHistory should be 10", 10, config.maxHistory)
    }

    @Test
    fun `test session config custom values`() {
        val config = SessionConfig(
            maxTokens = 1024,
            enableHistory = false,
            maxHistory = 5
        )
        assertEquals("maxTokens should be 1024", 1024, config.maxTokens)
        assertFalse("enableHistory should be false", config.enableHistory)
        assertEquals("maxHistory should be 5", 5, config.maxHistory)
    }
}