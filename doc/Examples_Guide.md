# LokAI SDK 示例代码文档

## 📋 文档信息

| 项 | 值 |
|---|---|
| 版本 | v1.0 |
| 更新日期 | 2026-06-04 |
| 示例位置 | `lokai/android/sample` |

---

## 📂 示例应用结构

```
sample/
├── src/main/
│   ├── kotlin/com/lokai/sample/
│   │   └── MainActivity.kt      # 主示例Activity
│   ├── res/
│   │   ├── layout/
│   │   │   └── activity_main.xml # 主界面布局
│   │   └── values/
│   │       └── colors.xml       # 颜色资源
│   └── AndroidManifest.xml
└── build.gradle
```

---

## 🎯 示例功能概览

示例应用包含以下功能演示：

| 功能 | 说明 |
|------|------|
| 模型管理 | 加载/卸载GGUF模型 |
| 文件选择 | 通过系统文件选择器选择模型 |
| 同步推理 | 一次性返回完整结果 |
| 流式推理 | 实时Token回调输出 |
| 多轮对话 | 支持历史消息的对话模式 |
| Tokenizer测试 | 文本分词与Token统计 |
| 性能监控 | 实时显示推理速度、内存占用 |

---

## 📝 核心代码示例

### 1. SDK初始化

```kotlin
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化SDK引擎
        val result = LokAIEngine.initialize(this)
        if (result.isSuccess) {
            Log.i(TAG, "SDK initialized")
            updateStatus("Ready", Status.READY)
        } else {
            Log.e(TAG, "Init failed: ${result.exceptionOrNull()}")
            updateStatus("Init Failed", Status.ERROR)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 关闭引擎
        LokAIEngine.shutdown()
    }
}
```

---

### 2. 模型加载

#### 2.1 从文件路径加载

```kotlin
private fun loadModel() {
    val modelPath = modelPathEditText.text.toString().trim()
    
    if (modelPath.isEmpty()) {
        Toast.makeText(this, "Please enter model path", Toast.LENGTH_SHORT).show()
        return
    }
    
    updateStatus("Loading model...", Status.BUSY)
    
    CoroutineScope(Dispatchers.IO).launch {
        val modelFile = File(modelPath)
        
        if (!modelFile.exists()) {
            runOnUiThread {
                updateStatus("Model file not found", Status.ERROR)
            }
            return@launch
        }
        
        val result = LokAIEngine.getModelManager().loadModel(modelPath)
        
        runOnUiThread {
            if (result.isSuccess) {
                model = result.getOrThrow()
                isModelLoaded = true
                updateStatus("Model Loaded", Status.READY)
                updateModelInfo(model.info)
            } else {
                updateStatus("Load Failed: ${result.exceptionOrNull()?.message}", Status.ERROR)
            }
        }
    }
}
```

#### 2.2 从文件选择器加载

```kotlin
private fun browseModelFile() {
    // 使用 Storage Access Framework 打开文件选择器
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
    }
    startActivityForResult(intent, REQUEST_SELECT_MODEL_FILE)
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    
    if (requestCode == REQUEST_SELECT_MODEL_FILE && resultCode == RESULT_OK) {
        data?.data?.let { uri ->
            // 将文件复制到应用私有目录
            val path = getPathFromUri(uri)
            if (path != null) {
                modelPathEditText.setText(path)
            }
        }
    }
}

private fun getPathFromUri(uri: Uri): String? {
    return try {
        val inputStream = contentResolver.openInputStream(uri)
        val tempFile = File(getExternalFilesDir(null), "temp_model_${System.currentTimeMillis()}.gguf")
        
        inputStream?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        tempFile.absolutePath
    } catch (e: Exception) {
        Log.e(TAG, "Failed to copy file: ${e.message}")
        null
    }
}
```

---

### 3. 同步推理

```kotlin
private fun syncGenerate() {
    if (!isModelLoaded) {
        Toast.makeText(this, "Please load model first", Toast.LENGTH_SHORT).show()
        return
    }
    
    val prompt = inputEditText.text.toString().trim()
    if (prompt.isEmpty()) {
        Toast.makeText(this, "Please enter prompt", Toast.LENGTH_SHORT).show()
        return
    }
    
    updateStatus("Generating...", Status.BUSY)
    
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val session = model.createSession()
            val params = InferenceParams(
                maxTokens = maxTokens,
                temperature = temperature
            )
            
            val result = session.complete(prompt, params)
            
            runOnUiThread {
                if (result.isSuccess) {
                    val completion = result.getOrThrow()
                    outputTextView.text = completion.text
                    
                    // 更新性能统计
                    updatePerformanceStats(
                        completion.stats.tokensPerSecond,
                        completion.stats.memoryUsage,
                        completion.stats.inferenceTime,
                        completion.stats.generatedTokens
                    )
                    
                    updateStatus("Completed", Status.READY)
                } else {
                    outputTextView.text = "Error: ${result.exceptionOrNull()?.message}"
                    updateStatus("Failed", Status.ERROR)
                }
            }
            
            session.close()
        } catch (e: Exception) {
            runOnUiThread {
                outputTextView.text = "Error: ${e.message}"
                updateStatus("Error", Status.ERROR)
            }
        }
    }
}
```

---

### 4. 流式推理

```kotlin
private var inferenceJob: Job? = null

private fun streamGenerate() {
    if (!isModelLoaded) {
        Toast.makeText(this, "Please load model first", Toast.LENGTH_SHORT).show()
        return
    }
    
    val prompt = inputEditText.text.toString().trim()
    if (prompt.isEmpty()) {
        Toast.makeText(this, "Please enter prompt", Toast.LENGTH_SHORT).show()
        return
    }
    
    updateStatus("Generating...", Status.BUSY)
    outputTextView.text = ""
    stopBtn.isEnabled = true
    
    inferenceJob = CoroutineScope(Dispatchers.IO).launch {
        try {
            val session = model.createSession()
            val params = InferenceParams(
                maxTokens = maxTokens,
                temperature = temperature
            )
            
            session.completeStream(prompt, params)
                .collect { tokenResult ->
                    runOnUiThread {
                        // 实时更新输出
                        outputTextView.append(tokenResult.token)
                        
                        // 更新性能统计
                        updatePerformanceStats(
                            tokenResult.stats.tokensPerSecond,
                            tokenResult.stats.memoryUsage,
                            tokenResult.stats.inferenceTime,
                            tokenResult.stats.generatedTokens
                        )
                        
                        if (tokenResult.isLast) {
                            updateStatus("Completed", Status.READY)
                            stopBtn.isEnabled = false
                        }
                    }
                }
            
            session.close()
        } catch (e: Exception) {
            runOnUiThread {
                outputTextView.text = "Error: ${e.message}"
                updateStatus("Error", Status.ERROR)
                stopBtn.isEnabled = false
            }
        }
    }
}

private fun stopInference() {
    inferenceJob?.cancel()
    inferenceJob = null
    updateStatus("Stopped", Status.READY)
    stopBtn.isEnabled = false
}
```

---

### 5. 多轮对话

```kotlin
private val chatMessages = mutableListOf<ChatMessage>()

private fun sendChatStream() {
    if (!isModelLoaded) {
        Toast.makeText(this, "Please load model first", Toast.LENGTH_SHORT).show()
        return
    }
    
    val userMessage = chatInputEditText.text.toString().trim()
    if (userMessage.isEmpty()) {
        Toast.makeText(this, "Please enter message", Toast.LENGTH_SHORT).show()
        return
    }
    
    // 添加用户消息
    chatMessages.add(ChatMessage(MessageRole.USER, userMessage))
    addChatMessageToUI(ChatMessage(MessageRole.USER, userMessage))
    
    chatInputEditText.text.clear()
    updateStatus("Chatting...", Status.BUSY)
    
    // 创建助手消息视图
    val assistantTextView = TextView(this).apply {
        textSize = 14f
        setTextColor(resources.getColor(R.color.teal_200))
        setPadding(12, 8, 12, 8)
    }
    chatHistoryLayout.addView(assistantTextView)
    
    inferenceJob = CoroutineScope(Dispatchers.IO).launch {
        try {
            val session = model.createSession()
            val params = InferenceParams(maxTokens = maxTokens, temperature = temperature)
            
            session.chatStream(chatMessages, params)
                .collect { tokenResult ->
                    runOnUiThread {
                        assistantTextView.text = tokenResult.accumulatedText
                        
                        updatePerformanceStats(
                            tokenResult.stats.tokensPerSecond,
                            tokenResult.stats.memoryUsage,
                            tokenResult.stats.inferenceTime,
                            tokenResult.stats.generatedTokens
                        )
                        
                        if (tokenResult.isLast) {
                            // 保存助手回复
                            chatMessages.add(ChatMessage(MessageRole.ASSISTANT, tokenResult.accumulatedText))
                            updateStatus("Chat Completed", Status.READY)
                        }
                    }
                }
            
            session.close()
        } catch (e: Exception) {
            runOnUiThread {
                assistantTextView.text = "Error: ${e.message}"
                updateStatus("Chat Failed", Status.ERROR)
            }
        }
    }
}
```

---

### 6. Tokenizer测试

```kotlin
private fun testTokenizer() {
    if (!isModelLoaded) {
        Toast.makeText(this, "Please load model first", Toast.LENGTH_SHORT).show()
        return
    }
    
    val text = tokenizerInputEditText.text.toString().trim()
    if (text.isEmpty()) {
        Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show()
        return
    }
    
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val tokenizer = model.getTokenizer()
            
            // 分词
            val tokens = tokenizer.encode(text)
            
            // 解码验证
            val decoded = tokenizer.decode(tokens)
            
            runOnUiThread {
                tokenizerResultTextView.text = """
                    Token数量: ${tokens.size}
                    Token IDs: ${tokens.take(20)}...
                    解码结果: $decoded
                    
                    BOS Token: ${tokenizer.getBosToken()}
                    EOS Token: ${tokenizer.getEosToken()}
                    词汇表大小: ${tokenizer.getVocabSize()}
                """.trimIndent()
            }
        } catch (e: Exception) {
            runOnUiThread {
                tokenizerResultTextView.text = "Error: ${e.message}"
            }
        }
    }
}
```

---

### 7. 性能监控UI更新

```kotlin
private fun updatePerformanceStats(
    tokensPerSec: Float,
    memoryBytes: Long,
    timeMs: Long,
    tokens: Int
) {
    tokensPerSecondTextView.text = String.format("%.2f", tokensPerSec)
    memoryUsageTextView.text = String.format("%.2f", memoryBytes / (1024.0 * 1024))
    inferenceTimeTextView.text = timeMs.toString()
    generatedTokensTextView.text = tokens.toString()
}
```

---

### 8. 模型卸载

```kotlin
private fun unloadModel() {
    if (!isModelLoaded) {
        Toast.makeText(this, "No model loaded", Toast.LENGTH_SHORT).show()
        return
    }
    
    model.close()
    isModelLoaded = false
    
    updateStatus("Model Unloaded", Status.READY)
    modelInfoLayout.removeAllViews()
    
    Toast.makeText(this, "Model unloaded", Toast.LENGTH_SHORT).show()
}
```

---

## 🎨 界面布局示例

### 性能监控面板

```xml
<!-- 性能监控面板 -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@color/card_bg"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Performance Monitor"
        android:textColor="@color/white"
        android:textSize="16sp"
        android:textStyle="bold"/>

    <!-- 实时性能指标 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <!-- Tokens/sec -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Speed"/>

            <TextView
                android:id="@+id/tokensPerSecondTextView"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="0.00"
                android:textColor="@color/green"
                android:textSize="24sp"
                android:textStyle="bold"/>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="tokens/s"/>
        </LinearLayout>

        <!-- Memory Usage -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Memory"/>

            <TextView
                android:id="@+id/memoryUsageTextView"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="0.00"
                android:textColor="@color/yellow"
                android:textSize="24sp"
                android:textStyle="bold"/>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="MB"/>
        </LinearLayout>
    </LinearLayout>
</LinearLayout>
```

---

## 📊 最佳实践

### 1. 资源管理

```kotlin
// 使用 use 扩展自动释放资源
model.use { m ->
    m.createSession().use { session ->
        session.completeStream(prompt).collect { ... }
    }
}
```

### 2. 异步处理

```kotlin
// 使用协程处理推理
CoroutineScope(Dispatchers.IO).launch {
    val result = session.complete(prompt)
    withContext(Dispatchers.Main) {
        updateUI(result)
    }
}
```

### 3. 错误处理

```kotlin
val result = session.complete(prompt)
result.onSuccess { completion ->
    updateUI(completion)
}.onError { exception ->
    handleError(exception)
}
```

### 4. 流式输出停止

```kotlin
private var inferenceJob: Job? = null

private fun startInference() {
    inferenceJob = CoroutineScope(Dispatchers.IO).launch {
        session.completeStream(prompt).collect { ... }
    }
}

private fun stopInference() {
    inferenceJob?.cancel()
}
```

---

## 🔗 相关文档

- [快速开始指南](./Quick_Start_Guide.md)
- [API参考文档](./api_reference.md)
- [SDK开发文档](./SDK_Development_Guide.md)

---

**最后更新**：2026-06-04