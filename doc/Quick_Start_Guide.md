# LokAI SDK 快速开始指南

## 📋 文档信息

| 项 | 值 |
|---|---|
| 版本 | v1.0 |
| 更新日期 | 2026-06-04 |
| 适用平台 | Android |

---

## 🎯 本指南目标

帮助开发者快速集成LokAI SDK，实现端侧AI推理功能。

---

## 📦 集成步骤

### 步骤1：环境准备

#### 1.1 系统要求

| 要求 | 说明 |
|------|------|
| Android版本 | API 24+ (Android 7.0) |
| NDK版本 | r25c+ |
| Kotlin版本 | 1.9+ |
| Gradle版本 | 8.0+ |

#### 1.2 硬件建议

| 配置 | 最低要求 | 推荐配置 |
|------|---------|---------|
| RAM | 4GB | 8GB+ |
| CPU | 4核 | 8核+ |
| 存储 | 2GB可用 | 4GB+可用 |

---

### 步骤2：添加SDK依赖

#### 2.1 方式一：AAR包集成

1. 下载SDK AAR包
2. 将 `lokai-sdk.aar` 放入项目 `libs` 目录
3. 在 `build.gradle` 中添加：

```gradle
android {
    // ...
}

dependencies {
    implementation files('libs/lokai-sdk.aar')
}
```

#### 2.2 方式二：源码集成

```gradle
dependencies {
    implementation project(':lokai:sdk')
}
```

---

### 步骤3：配置项目

#### 3.1 添加NDK配置

在 `build.gradle` 中：

```gradle
android {
    defaultConfig {
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a'
        }
    }
    
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
        }
    }
}
```

#### 3.2 添加必要权限

在 `AndroidManifest.xml` 中：

```xml
<!-- 存储权限（用于读取模型文件） -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- Android 11+ 需要管理所有文件 -->
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" 
    tools:ignore="ScopedStorage" />
```

---

### 步骤4：初始化SDK

#### 4.1 创建Application类

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化LokAI SDK
        val result = LokAIEngine.initialize(this)
        if (result.isSuccess) {
            Log.i("LokAI", "SDK initialized successfully")
        } else {
            Log.e("LokAI", "SDK initialization failed: ${result.exceptionOrNull()?.message}")
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        LokAIEngine.shutdown()
    }
}
```

#### 4.2 在AndroidManifest中注册

```xml
<application
    android:name=".MyApplication"
    ... >
</application>
```

---

### 步骤5：准备模型文件

#### 5.1 模型格式要求

LokAI SDK支持 **GGUF** 格式的模型文件。

推荐模型：
- Qwen2.5-1.5B-Instruct (约1GB)
- Llama-3.2-1B-Instruct (约1GB)
- Phi-3-mini (约2GB)

#### 5.2 模型放置位置

**方式一：放入设备存储**

将模型文件放入设备存储目录，如：
- `/sdcard/Download/model.gguf`
- `/sdcard/lokai/models/model.gguf`

**方式二：放入Assets**

将模型放入 `assets/models/` 目录，SDK会自动复制到缓存目录。

---

### 步骤6：加载模型

#### 6.1 从文件路径加载

```kotlin
// 加载模型
val modelPath = "/sdcard/Download/qwen2.5-1.5b-instruct-q4_0.gguf"

val result = LokAIEngine.getModelManager().loadModel(modelPath)
if (result.isSuccess) {
    val model = result.getOrThrow()
    Log.i("LokAI", "Model loaded: ${model.info.name}")
} else {
    Log.e("LokAI", "Load failed: ${result.exceptionOrNull()?.message}")
}
```

#### 6.2 从Assets加载

```kotlin
val result = LokAIEngine.getModelManager()
    .loadModelFromAssets(context, "models/qwen.gguf")
```

---

### 步骤7：执行推理

#### 7.1 同步推理

```kotlin
// 创建会话
val session = model.createSession()

// 同步推理
val result = session.complete("你好，请介绍一下自己")
if (result.isSuccess) {
    val completion = result.getOrThrow()
    println(completion.text)
    println("生成Token数: ${completion.tokenCount}")
    println("推理速度: ${completion.stats.tokensPerSecond} tokens/s")
}
```

#### 7.2 流式推理

```kotlin
// 流式推理（推荐）
session.completeStream("你好，请介绍一下自己")
    .collect { tokenResult ->
        // 实时打印每个Token
        print(tokenResult.token)
        
        if (tokenResult.isLast) {
            println("\n推理完成")
            println("速度: ${tokenResult.stats.tokensPerSecond} tokens/s")
        }
    }
```

---

### 步骤8：多轮对话

```kotlin
// 构建对话消息
val messages = listOf(
    ChatMessage(MessageRole.SYSTEM, "你是一个友好的AI助手"),
    ChatMessage(MessageRole.USER, "你好"),
    ChatMessage(MessageRole.ASSISTANT, "你好！有什么我可以帮助你的？"),
    ChatMessage(MessageRole.USER, "请解释一下量子计算")
)

// 流式对话
session.chatStream(messages, InferenceParams(maxTokens = 256))
    .collect { result ->
        print(result.token)
    }
```

---

## ⚙️ 推理参数配置

### 常用参数

```kotlin
val params = InferenceParams(
    maxTokens = 512,      // 最大生成Token数
    temperature = 0.7f,   // 温度参数（0.1-1.0）
    topP = 0.9f,          // Top-P采样
    topK = 40,            // Top-K采样
    repeatPenalty = 1.1f  // 重复惩罚
)
```

### 参数说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| maxTokens | 512 | 最大生成Token数量 |
| temperature | 0.8 | 控制输出随机性，越低越确定 |
| topP | 0.95 | nucleus采样阈值 |
| topK | 40 | Top-K采样数量 |
| repeatPenalty | 1.1 | 重复内容惩罚系数 |

---

## 🔧 性能监控

### 获取推理统计

```kotlin
// 每次推理后获取统计
val stats = session.getStats()
println("速度: ${stats.tokensPerSecond} tokens/s")
println("生成Token: ${stats.generatedTokens}")
println("内存占用: ${stats.memoryUsage / 1024 / 1024} MB")
println("推理耗时: ${stats.inferenceTime} ms")
```

### 流式输出中的统计

```kotlin
session.completeStream(prompt)
    .collect { result ->
        // 每个Token都带有实时统计
        updateUI(result.stats)
    }
```

---

## 🧹 资源管理

### 正确释放资源

```kotlin
// 使用完毕后释放
try {
    // 使用session和model
} finally {
    session.close()  // 关闭会话
    model.close()    // 关闭模型
}

// 或使用use扩展函数
model.use { m ->
    m.createSession().use { session ->
        session.complete(prompt)
    }
}
```

### 应用退出时

```kotlin
override fun onDestroy() {
    super.onDestroy()
    LokAIEngine.shutdown()  // 关闭引擎
}
```

---

## ⚠️ 常见问题

### Q1：模型加载失败

**原因**：
- 文件路径错误
- 存储权限未授予
- 模型格式不支持

**解决方案**：
```kotlin
// 检查文件是否存在
val file = File(modelPath)
if (!file.exists()) {
    Log.e("LokAI", "Model file not found: $modelPath")
}

// 检查权限
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    if (!Environment.isExternalStorageManager()) {
        // 申请权限
        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        startActivity(intent)
    }
}
```

### Q2：推理速度慢

**优化建议**：
- 使用量化模型（Q4_0、Q5_0）
- 减少上下文长度
- 降低maxTokens
- 使用更小的模型

### Q3：内存不足

**解决方案**：
- 使用更小的模型
- 启用内存映射（useMmap=true）
- 及时释放资源
- 监控内存使用

---

## 📱 完整示例

参考 `lokai/android/sample` 目录中的完整示例应用。

示例应用功能：
- 模型加载与卸载
- 同步/流式推理
- 多轮对话
- 性能监控面板
- Tokenizer测试

---

## 🔗 相关文档

- [API参考文档](./api_reference.md)
- [示例代码文档](./Examples_Guide.md)
- [SDK开发文档](./SDK_Development_Guide.md)

---

**最后更新**：2026-06-04