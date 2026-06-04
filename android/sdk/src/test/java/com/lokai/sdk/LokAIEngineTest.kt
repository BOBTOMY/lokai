package com.lokai.sdk

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class LokAIEngineTest {

    @Before
    fun setUp() {
        LokAIEngine.shutdown()
    }

    @After
    fun tearDown() {
        LokAIEngine.shutdown()
    }

    @Test
    fun `test engine initializes correctly`() {
        assertFalse("Engine should not be initialized initially", LokAIEngine.isInitialized())
        assertEquals("Engine should be UNINITIALIZED", EngineStatus.UNINITIALIZED, LokAIEngine.getStatus())
    }

    @Test
    fun `test engine version`() {
        val version = LokAIEngine.getVersion()
        assertNotNull("Version should not be null", version)
        assertEquals("Version should be 1.0.0", "1.0.0", version)
    }

    @Test
    fun `test engine status enum values`() {
        val statuses = EngineStatus.values()
        assertEquals("Should have 5 status values", 5, statuses.size)
        assertTrue("Should contain UNINITIALIZED", statuses.contains(EngineStatus.UNINITIALIZED))
        assertTrue("Should contain INITIALIZING", statuses.contains(EngineStatus.INITIALIZING))
        assertTrue("Should contain READY", statuses.contains(EngineStatus.READY))
        assertTrue("Should contain INFERENCING", statuses.contains(EngineStatus.INFERENCING))
        assertTrue("Should contain ERROR", statuses.contains(EngineStatus.ERROR))
    }

    @Test
    fun `test log level enum values`() {
        val levels = LogLevel.values()
        assertEquals("Should have 5 log levels", 5, levels.size)
        assertTrue("Should contain DEBUG", levels.contains(LogLevel.DEBUG))
        assertTrue("Should contain INFO", levels.contains(LogLevel.INFO))
        assertTrue("Should contain WARN", levels.contains(LogLevel.WARN))
        assertTrue("Should contain ERROR", levels.contains(LogLevel.ERROR))
        assertTrue("Should contain NONE", levels.contains(LogLevel.NONE))
    }

    @Test
    fun `test engine config defaults`() {
        val config = EngineConfig()
        assertEquals("Default thread count should be 0", 0, config.threadCount)
        assertTrue("Default useMmap should be true", config.useMmap)
        assertFalse("Default enableGpu should be false", config.enableGpu)
        assertEquals("Default log level should be INFO", LogLevel.INFO, config.logLevel)
        assertNull("Default resourceDir should be null", config.resourceDir)
    }

    @Test
    fun `test engine config custom values`() {
        val config = EngineConfig(
            threadCount = 4,
            useMmap = false,
            enableGpu = true,
            logLevel = LogLevel.DEBUG,
            resourceDir = "/custom/path"
        )
        assertEquals("Thread count should be 4", 4, config.threadCount)
        assertFalse("useMmap should be false", config.useMmap)
        assertTrue("enableGpu should be true", config.enableGpu)
        assertEquals("Log level should be DEBUG", LogLevel.DEBUG, config.logLevel)
        assertEquals("Resource dir should be /custom/path", "/custom/path", config.resourceDir)
    }
}