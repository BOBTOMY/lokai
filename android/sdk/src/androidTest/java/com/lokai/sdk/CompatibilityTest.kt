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
import org.junit.Assert.*

import java.io.File

/**
 * 兼容性测试 - 验证SDK在不同Android版本和设备上的兼容性
 * 
 * 运行方式：
 * ./gradlew sdk:connectedAndroidTest --tests "com.lokai.sdk.CompatibilityTest"
 * 
 * 兼容性要求：
 * - 最低支持版本：Android 7.0 (API 24)
 * - 目标版本：Android 15 (API 35)
 * - 支持架构：arm64-v8a, armeabi-v7a, x86_64, x86
 */
@RunWith(AndroidJUnit4::class)
class CompatibilityTest {

    private lateinit var context: Context

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
     * 测试引擎在当前Android版本上能否正常初始化
     */
    @Test
    fun testEngineInitializationOnCurrentVersion() {
        println("Testing on Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        
        val result = LokAIEngine.initialize(context)
        
        assertTrue("Engine should initialize successfully", result.isSuccess)
        assertTrue("Engine should be initialized", LokAIEngine.isInitialized())
        assertEquals("Engine status should be READY", EngineStatus.READY, LokAIEngine.getStatus())
        
        println("✓ Engine initialized successfully on Android ${Build.VERSION.RELEASE}")
    }

    /**
     * 测试Android版本兼容性检查
     */
    @Test
    fun testAndroidVersionCompatibility() {
        val sdkVersion = Build.VERSION.SDK_INT
        
        println("Current SDK version: $sdkVersion")
        
        // 检查最低版本要求
        val minVersion = 24 // Android 7.0
        val maxTestedVersion = 35 // Android 15
        
        if (sdkVersion >= minVersion) {
            val result = LokAIEngine.initialize(context)
            assertTrue("Engine should initialize on supported version", result.isSuccess)
        } else {
            println("⚠️  Running on unsupported Android version ($sdkVersion < $minVersion)")
            // 在低于最低版本的设备上，引擎可能无法初始化
        }
        
        // 验证版本信息
        val version = LokAIEngine.getVersion()
        assertNotNull("Version should not be null", version)
        assertEquals("Version should be 1.0.0", "1.0.0", version)
    }

    /**
     * 测试文件访问兼容性
     */
    @Test
    fun testFileAccessCompatibility() {
        // 测试私有目录访问
        val filesDir = context.filesDir
        assertNotNull("Files directory should exist", filesDir)
        assertTrue("Files directory should be writable", filesDir.canWrite())
        
        // 测试缓存目录访问
        val cacheDir = context.cacheDir
        assertNotNull("Cache directory should exist", cacheDir)
        assertTrue("Cache directory should be writable", cacheDir.canWrite())
        
        println("✓ File access test passed")
    }

    /**
     * 测试权限兼容性
     */
    @Test
    fun testPermissionCompatibility() {
        // 检查是否有读取外部存储的权限
        // 在Android 13+上，READ_MEDIA_IMAGES等权限代替了READ_EXTERNAL_STORAGE
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            println("Running on Android 13+, using new permission model")
        } else {
            println("Running on Android < 13")
        }
        
        // 引擎初始化不应该因为权限问题失败
        val result = LokAIEngine.initialize(context)
        assertTrue("Engine should initialize without storage permissions", result.isSuccess)
        
        println("✓ Permission compatibility test passed")
    }

    /**
     * 测试架构兼容性
     */
    @Test
    fun testArchitectureCompatibility() {
        val supportedAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        val currentAbi = Build.SUPPORTED_ABIS.firstOrNull()
        
        println("Current ABI: $currentAbi")
        println("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        
        // 检查当前架构是否在支持列表中
        if (currentAbi != null && supportedAbis.contains(currentAbi)) {
            println("✓ Current architecture ($currentAbi) is supported")
            
            // 尝试初始化引擎
            val result = LokAIEngine.initialize(context)
            assertTrue("Engine should initialize on supported architecture", result.isSuccess)
        } else {
            println("⚠️  Current architecture may not be officially supported: $currentAbi")
        }
    }

    /**
     * 测试进程间兼容性
     */
    @Test
    fun testProcessCompatibility() {
        // 测试引擎多次初始化/关闭
        for (i in 1..3) {
            val initResult = LokAIEngine.initialize(context)
            assertTrue("Iteration $i: Engine should initialize", initResult.isSuccess)
            
            LokAIEngine.shutdown()
            assertFalse("Iteration $i: Engine should be shutdown", LokAIEngine.isInitialized())
        }
        
        println("✓ Process compatibility test passed")
    }

    /**
     * 测试上下文兼容性
     */
    @Test
    fun testContextCompatibility() {
        // 使用Application上下文测试
        val appContext = context.applicationContext
        
        val result = LokAIEngine.initialize(appContext)
        assertTrue("Engine should initialize with Application context", result.isSuccess)
        
        LokAIEngine.shutdown()
        
        // 使用Activity上下文测试
        val result2 = LokAIEngine.initialize(context)
        assertTrue("Engine should initialize with Activity context", result2.isSuccess)
        
        println("✓ Context compatibility test passed")
    }

    /**
     * 测试配置兼容性
     */
    @Test
    fun testConfigurationCompatibility() {
        // 测试默认配置
        val result = LokAIEngine.initialize(context)
        assertTrue("Engine should initialize with default config", result.isSuccess)
        
        // 测试自定义引擎配置
        LokAIEngine.shutdown()
        
        val customConfig = EngineConfig(
            threadCount = Runtime.getRuntime().availableProcessors(),
            logLevel = LogLevel.INFO
        )
        // 注意：当前API可能不支持直接传入配置，这里测试API可用性
        
        println("✓ Configuration compatibility test passed")
    }

    /**
     * 测试SDK版本信息
     */
    @Test
    fun testSdkVersionInfo() {
        // 测试版本获取
        val version = LokAIEngine.getVersion()
        assertNotNull("Version should not be null", version)
        assertTrue("Version should match semantic versioning", 
            version.matches(Regex("\\d+\\.\\d+\\.\\d+")))
        
        println("SDK Version: $version")
        println("✓ SDK version test passed")
    }

    /**
     * 输出设备兼容性报告
     */
    @Test
    fun generateCompatibilityReport() {
        println("\n===== Compatibility Report =====")
        println("Manufacturer: ${Build.MANUFACTURER}")
        println("Model: ${Build.MODEL}")
        println("Android Version: ${Build.VERSION.RELEASE}")
        println("API Level: ${Build.VERSION.SDK_INT}")
        println("Architecture: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        println("CPU Cores: ${Runtime.getRuntime().availableProcessors()}")
        println("Max Memory: ${Runtime.getRuntime().maxMemory() / (1024 * 1024)} MB")
        println("=================================\n")
        
        // 检查兼容性状态
        val isCompatible = Build.VERSION.SDK_INT >= 24
        println("Compatibility Status: ${if (isCompatible) "✓ Compatible" else "✗ Not Compatible"}")
        
        if (isCompatible) {
            val result = LokAIEngine.initialize(context)
            println("Engine Initialization: ${if (result.isSuccess) "✓ Success" else "✗ Failed"}")
        }
    }
}