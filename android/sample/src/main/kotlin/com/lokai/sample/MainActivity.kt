package com.lokai.sample

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lokai.sdk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    private val TAG = "LokAI-Demo"

    private lateinit var model: Model
    private lateinit var session: InferenceSession
    private var isModelLoaded = false
    private var isInferencing = false
    private var inferenceJob: Job? = null

    // 顶部状态栏
    private lateinit var versionTextView: TextView

    // 模型管理
    private lateinit var modelPathEditText: EditText
    private lateinit var browseBtn: Button
    private lateinit var loadModelBtn: Button
    private lateinit var unloadModelBtn: Button
    private lateinit var modelInfoLayout: LinearLayout
    private lateinit var modelNameTextView: TextView
    private lateinit var modelSizeTextView: TextView
    private lateinit var modelContextTextView: TextView

    // 性能监控
    private lateinit var tokensPerSecondTextView: TextView
    private lateinit var memoryUsageTextView: TextView
    private lateinit var inferenceTimeTextView: TextView
    private lateinit var generatedTokensTextView: TextView
    private lateinit var statusIndicator: View
    private lateinit var statusTextView: TextView

    // 推理功能
    private lateinit var promptEditText: EditText
    private lateinit var maxTokensEditText: EditText
    private lateinit var temperatureEditText: EditText
    private lateinit var syncGenerateBtn: Button
    private lateinit var streamGenerateBtn: Button
    private lateinit var stopBtn: Button

    // 对话功能
    private lateinit var chatInputEditText: EditText
    private lateinit var chatBtn: Button
    private lateinit var chatStreamBtn: Button
    private lateinit var clearChatBtn: Button
    private lateinit var chatHistoryLayout: LinearLayout
    private val chatMessages = mutableListOf<ChatMessage>()

    // Tokenizer
    private lateinit var tokenizerInputEditText: EditText
    private lateinit var tokenizeBtn: Button
    private lateinit var vocabBtn: Button
    private lateinit var tokenizerResultTextView: TextView

    // 输出
    private lateinit var outputTextView: TextView

    private val REQUEST_STORAGE_PERMISSION = 100
    private val REQUEST_SELECT_MODEL_FILE = 101
    private val REQUEST_MANAGE_STORAGE_PERMISSION = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i(TAG, "MainActivity onCreate")
        initViews()
        initSDK()
        checkPermissions()
    }

    private fun initViews() {
        Log.i(TAG, "Initializing views...")

        // 顶部状态栏
        versionTextView = findViewById(R.id.versionTextView)

        // 模型管理
        modelPathEditText = findViewById(R.id.modelPathEditText)
        browseBtn = findViewById(R.id.browseBtn)
        loadModelBtn = findViewById(R.id.loadModelBtn)
        unloadModelBtn = findViewById(R.id.unloadModelBtn)
        modelInfoLayout = findViewById(R.id.modelInfoLayout)
        modelNameTextView = findViewById(R.id.modelNameTextView)
        modelSizeTextView = findViewById(R.id.modelSizeTextView)
        modelContextTextView = findViewById(R.id.modelContextTextView)

        // 性能监控
        tokensPerSecondTextView = findViewById(R.id.tokensPerSecondTextView)
        memoryUsageTextView = findViewById(R.id.memoryUsageTextView)
        inferenceTimeTextView = findViewById(R.id.inferenceTimeTextView)
        generatedTokensTextView = findViewById(R.id.generatedTokensTextView)
        statusIndicator = findViewById(R.id.statusIndicator)
        statusTextView = findViewById(R.id.statusTextView)

        // 推理功能
        promptEditText = findViewById(R.id.promptEditText)
        maxTokensEditText = findViewById(R.id.maxTokensEditText)
        temperatureEditText = findViewById(R.id.temperatureEditText)
        syncGenerateBtn = findViewById(R.id.syncGenerateBtn)
        streamGenerateBtn = findViewById(R.id.streamGenerateBtn)
        stopBtn = findViewById(R.id.stopBtn)

        // 对话功能
        chatInputEditText = findViewById(R.id.chatInputEditText)
        chatBtn = findViewById(R.id.chatBtn)
        chatStreamBtn = findViewById(R.id.chatStreamBtn)
        clearChatBtn = findViewById(R.id.clearChatBtn)
        chatHistoryLayout = findViewById(R.id.chatHistoryLayout)

        // Tokenizer
        tokenizerInputEditText = findViewById(R.id.tokenizerInputEditText)
        tokenizeBtn = findViewById(R.id.tokenizeBtn)
        vocabBtn = findViewById(R.id.vocabBtn)
        tokenizerResultTextView = findViewById(R.id.tokenizerResultTextView)

        // 输出
        outputTextView = findViewById(R.id.outputTextView)

        // 设置默认模型路径
        val defaultPath = "/storage/emulated/0/Download/model.gguf"
        modelPathEditText.setText(defaultPath)

        // 设置点击事件
        browseBtn.setOnClickListener { browseModelFile() }
        loadModelBtn.setOnClickListener { loadModel() }
        unloadModelBtn.setOnClickListener { unloadModel() }
        syncGenerateBtn.setOnClickListener { syncGenerate() }
        streamGenerateBtn.setOnClickListener { streamGenerate() }
        stopBtn.setOnClickListener { stopGeneration() }
        chatBtn.setOnClickListener { sendChat() }
        chatStreamBtn.setOnClickListener { sendChatStream() }
        clearChatBtn.setOnClickListener { clearChat() }
        tokenizeBtn.setOnClickListener { testTokenizer() }
        vocabBtn.setOnClickListener { showVocabInfo() }

        // 初始化状态
        updateStatus("Ready", Status.READY)
        updatePerformanceStats(0f, 0L, 0L, 0)

        Log.i(TAG, "Views initialized")
    }

    private fun initSDK() {
        Log.i(TAG, "Initializing SDK...")
        updateStatus("Initializing SDK...", Status.BUSY)

        CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "Calling LokAIEngine.initialize()")
            val result = LokAIEngine.initialize(this@MainActivity)
            runOnUiThread {
                if (result.isSuccess) {
                    Log.i(TAG, "SDK initialization successful")
                    versionTextView.text = "v${LokAIEngine.getVersion()}"
                    updateStatus("SDK Ready", Status.READY)
                    loadModelBtn.isEnabled = true
                } else {
                    Log.e(TAG, "SDK initialization failed: ${result.exceptionOrNull()?.message}")
                    updateStatus("SDK Init Failed: ${result.exceptionOrNull()?.message}", Status.ERROR)
                }
            }
        }
    }

    private fun checkPermissions() {
        Log.i(TAG, "Checking permissions...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Log.i(TAG, "Requesting MANAGE_EXTERNAL_STORAGE permission")
                Toast.makeText(this, "请授予访问所有文件的权限", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_MANAGE_STORAGE_PERMISSION)
                return
            }
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Requesting READ_EXTERNAL_STORAGE permission")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
        } else {
            Log.i(TAG, "Storage permissions already granted")
        }
    }

    private fun browseModelFile() {
        Log.d(TAG, "browseModelFile: Opening file picker")
        // 使用 Storage Access Framework 打开文件选择器
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"  // 先选择所有文件，再过滤
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/octet-stream",  // GGUF文件
                "application/x-gguf",
                "*/*"  // 允许所有文件类型以便选择模型文件
            ))
        }
        startActivityForResult(intent, REQUEST_SELECT_MODEL_FILE)
    }

    /**
     * 从 Content URI 获取实际文件路径
     */
    private fun getPathFromUri(uri: Uri): String? {
        Log.d(TAG, "getPathFromUri: uri = $uri")
        Log.d(TAG, "getPathFromUri: authority = ${uri.authority}")
        Log.d(TAG, "getPathFromUri: path = ${uri.path}")
        Log.d(TAG, "getPathFromUri: lastPathSegment = ${uri.lastPathSegment}")
        Log.d(TAG, "getPathFromUri: scheme = ${uri.scheme}")
        
        // 首先尝试简单的方法：使用应用私有目录复制文件
        // 因为SAF返回的URI可能无法直接用File访问
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "getPathFromUri: Failed to open input stream")
                return null
            }
            
            // 创建临时文件
            val tempFile = File(getExternalFilesDir(null), "temp_model_${System.currentTimeMillis()}.gguf")
            Log.d(TAG, "getPathFromUri: Copying to temporary file: ${tempFile.absolutePath}")
            
            val outputStream = tempFile.outputStream()
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            
            Log.d(TAG, "getPathFromUri: File copied successfully")
            tempFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "getPathFromUri: Error copying file", e)
            null
        }
    }

    private fun loadModel() {
        val modelPath = modelPathEditText.text.toString().trim()
        Log.i(TAG, "loadModel called with path: $modelPath")

        if (modelPath.isEmpty()) {
            Toast.makeText(this, "Please enter model path", Toast.LENGTH_SHORT).show()
            return
        }

        updateStatus("Loading model...", Status.BUSY)
        loadModelBtn.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val modelFile = java.io.File(modelPath)
            Log.i(TAG, "Checking if model file exists: $modelPath")
            Log.i(TAG, "File exists: ${modelFile.exists()}")

            if (!modelFile.exists()) {
                runOnUiThread {
                    updateStatus("Model file not found", Status.ERROR)
                    loadModelBtn.isEnabled = true
                }
                return@launch
            }

            Log.i(TAG, "Calling ModelManager.loadModel()")
            val result = LokAIEngine.getModelManager().loadModel(modelPath)
            runOnUiThread {
                result.onSuccess { loadedModel ->
                    Log.i(TAG, "Model loaded successfully")
                    model = loadedModel
                    session = model.createSession()
                    isModelLoaded = true

                    // 更新模型信息
                    modelNameTextView.text = "Model: ${model.info.name}"
                    modelSizeTextView.text = "Size: ${formatSize(model.info.size)}"
                    modelContextTextView.text = "Context Window: ${model.info.contextWindowSize}"
                    modelInfoLayout.visibility = View.VISIBLE

                    // 更新性能监控
                    updatePerformanceStats(0f, model.info.size, 0L, 0)

                    // 更新按钮状态
                    unloadModelBtn.isEnabled = true
                    syncGenerateBtn.isEnabled = true
                    streamGenerateBtn.isEnabled = true
                    chatBtn.isEnabled = true
                    chatStreamBtn.isEnabled = true
                    tokenizeBtn.isEnabled = true
                    vocabBtn.isEnabled = true

                    updateStatus("Model Loaded: ${model.info.name}", Status.READY)
                    loadModelBtn.isEnabled = true
                }.onFailure { e ->
                    Log.e(TAG, "Model load failed: ${e.message}", e)
                    updateStatus("Model Load Failed: ${e.message}", Status.ERROR)
                    loadModelBtn.isEnabled = true
                }
            }
        }
    }

    private fun unloadModel() {
        Log.i(TAG, "Unloading model...")
        updateStatus("Unloading model...", Status.BUSY)

        try {
            session.close()
            model.close()
            isModelLoaded = false

            modelInfoLayout.visibility = View.GONE
            unloadModelBtn.isEnabled = false
            syncGenerateBtn.isEnabled = false
            streamGenerateBtn.isEnabled = false
            chatBtn.isEnabled = false
            chatStreamBtn.isEnabled = false
            tokenizeBtn.isEnabled = false
            vocabBtn.isEnabled = false

            updatePerformanceStats(0f, 0L, 0L, 0)
            updateStatus("Model Unloaded", Status.READY)
            outputTextView.text = ""
            tokenizerResultTextView.text = ""

            Log.i(TAG, "Model unloaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unload model: ${e.message}", e)
            updateStatus("Unload Failed: ${e.message}", Status.ERROR)
        }
    }

    private fun syncGenerate() {
        val prompt = promptEditText.text.toString().trim()
        if (prompt.isEmpty()) {
            Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show()
            return
        }

        val maxTokens = maxTokensEditText.text.toString().toIntOrNull() ?: 256
        val temperature = temperatureEditText.text.toString().toFloatOrNull() ?: 0.8f

        updateStatus("Generating...", Status.BUSY)
        syncGenerateBtn.isEnabled = false
        streamGenerateBtn.isEnabled = false
        stopBtn.isEnabled = true
        isInferencing = true

        CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "Calling session.complete()")
            val result = session.complete(prompt, InferenceParams(maxTokens = maxTokens, temperature = temperature))
            runOnUiThread {
                result.onSuccess { completion ->
                    Log.i(TAG, "Generation completed successfully")
                    outputTextView.text = completion.text
                    updatePerformanceStats(
                        completion.stats.tokensPerSecond,
                        completion.stats.memoryUsage,
                        completion.stats.inferenceTime,
                        completion.stats.generatedTokens
                    )
                    updateStatus("Completed", Status.READY)
                }.onFailure { e ->
                    Log.e(TAG, "Generation failed: ${e.message}", e)
                    outputTextView.text = "Error: ${e.message}"
                    updateStatus("Generation Failed", Status.ERROR)
                }
                syncGenerateBtn.isEnabled = true
                streamGenerateBtn.isEnabled = true
                stopBtn.isEnabled = false
                isInferencing = false
            }
        }
    }

    private fun streamGenerate() {
        val prompt = promptEditText.text.toString().trim()
        if (prompt.isEmpty()) {
            Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show()
            return
        }

        val maxTokens = maxTokensEditText.text.toString().toIntOrNull() ?: 256
        val temperature = temperatureEditText.text.toString().toFloatOrNull() ?: 0.8f

        updateStatus("Streaming...", Status.BUSY)
        syncGenerateBtn.isEnabled = false
        streamGenerateBtn.isEnabled = false
        stopBtn.isEnabled = true
        isInferencing = true
        outputTextView.text = ""

        inferenceJob = CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "Calling session.completeStream()")
            try {
                session.completeStream(prompt, InferenceParams(maxTokens = maxTokens, temperature = temperature))
                    .collect { tokenResult ->
                        runOnUiThread {
                            outputTextView.text = tokenResult.accumulatedText
                            updatePerformanceStats(
                                tokenResult.stats.tokensPerSecond,
                                tokenResult.stats.memoryUsage,
                                tokenResult.stats.inferenceTime,
                                tokenResult.stats.generatedTokens
                            )

                            if (tokenResult.isLast) {
                                updateStatus("Completed", Status.READY)
                                syncGenerateBtn.isEnabled = true
                                streamGenerateBtn.isEnabled = true
                                stopBtn.isEnabled = false
                                isInferencing = false
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Stream generation failed: ${e.message}", e)
                runOnUiThread {
                    outputTextView.text = "Error: ${e.message}"
                    updateStatus("Stream Failed", Status.ERROR)
                    syncGenerateBtn.isEnabled = true
                    streamGenerateBtn.isEnabled = true
                    stopBtn.isEnabled = false
                    isInferencing = false
                }
            }
        }
    }

    private fun stopGeneration() {
        Log.i(TAG, "Stopping generation...")
        inferenceJob?.cancel()
        session.stop()
        updateStatus("Stopped", Status.READY)
        syncGenerateBtn.isEnabled = true
        streamGenerateBtn.isEnabled = true
        stopBtn.isEnabled = false
        isInferencing = false
    }

    private fun sendChat() {
        val userInput = chatInputEditText.text.toString().trim()
        if (userInput.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        val maxTokens = maxTokensEditText.text.toString().toIntOrNull() ?: 256
        val temperature = temperatureEditText.text.toString().toFloatOrNull() ?: 0.8f

        // 添加用户消息到历史
        val userMessage = ChatMessage(MessageRole.USER, userInput)
        chatMessages.add(userMessage)
        addChatMessageToUI(userMessage)

        chatInputEditText.text.clear()
        updateStatus("Chatting...", Status.BUSY)
        chatBtn.isEnabled = false
        chatStreamBtn.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "Calling session.chat()")
            val result = session.chat(chatMessages, InferenceParams(maxTokens = maxTokens, temperature = temperature))
            runOnUiThread {
                result.onSuccess { completion ->
                    Log.i(TAG, "Chat completed successfully")
                    val assistantMessage = ChatMessage(MessageRole.ASSISTANT, completion.text)
                    chatMessages.add(assistantMessage)
                    addChatMessageToUI(assistantMessage)

                    updatePerformanceStats(
                        completion.stats.tokensPerSecond,
                        completion.stats.memoryUsage,
                        completion.stats.inferenceTime,
                        completion.stats.generatedTokens
                    )
                    updateStatus("Chat Completed", Status.READY)
                }.onFailure { e ->
                    Log.e(TAG, "Chat failed: ${e.message}", e)
                    addErrorMessageToUI("Error: ${e.message}")
                    updateStatus("Chat Failed", Status.ERROR)
                }
                chatBtn.isEnabled = true
                chatStreamBtn.isEnabled = true
            }
        }
    }

    private fun sendChatStream() {
        val userInput = chatInputEditText.text.toString().trim()
        if (userInput.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        val maxTokens = maxTokensEditText.text.toString().toIntOrNull() ?: 256
        val temperature = temperatureEditText.text.toString().toFloatOrNull() ?: 0.8f

        // 添加用户消息到历史
        val userMessage = ChatMessage(MessageRole.USER, userInput)
        chatMessages.add(userMessage)
        addChatMessageToUI(userMessage)

        chatInputEditText.text.clear()
        updateStatus("Streaming Chat...", Status.BUSY)
        chatBtn.isEnabled = false
        chatStreamBtn.isEnabled = false
        stopBtn.isEnabled = true

        // 创建助手消息视图
        val assistantTextView = TextView(this).apply {
            textSize = 14f
            setTextColor(resources.getColor(R.color.teal_200))
            setPadding(12, 8, 12, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 48
                topMargin = 8
            }
        }
        chatHistoryLayout.addView(assistantTextView)

        inferenceJob = CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "Calling session.chatStream()")
            try {
                var accumulatedText = ""
                session.chatStream(chatMessages, InferenceParams(maxTokens = maxTokens, temperature = temperature))
                    .collect { tokenResult ->
                        accumulatedText = tokenResult.accumulatedText
                        runOnUiThread {
                            assistantTextView.text = accumulatedText
                            updatePerformanceStats(
                                tokenResult.stats.tokensPerSecond,
                                tokenResult.stats.memoryUsage,
                                tokenResult.stats.inferenceTime,
                                tokenResult.stats.generatedTokens
                            )

                            if (tokenResult.isLast) {
                                chatMessages.add(ChatMessage(MessageRole.ASSISTANT, accumulatedText))
                                updateStatus("Chat Completed", Status.READY)
                                chatBtn.isEnabled = true
                                chatStreamBtn.isEnabled = true
                                stopBtn.isEnabled = false
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Stream chat failed: ${e.message}", e)
                runOnUiThread {
                    assistantTextView.text = "Error: ${e.message}"
                    updateStatus("Chat Failed", Status.ERROR)
                    chatBtn.isEnabled = true
                    chatStreamBtn.isEnabled = true
                    stopBtn.isEnabled = false
                }
            }
        }
    }

    private fun clearChat() {
        chatMessages.clear()
        chatHistoryLayout.removeAllViews()
        updateStatus("Chat Cleared", Status.READY)
    }

    private fun addChatMessageToUI(message: ChatMessage) {
        val textView = TextView(this).apply {
            textSize = 14f
            setPadding(12, 8, 12, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }

            when (message.role) {
                MessageRole.USER -> {
                    setTextColor(resources.getColor(R.color.white))
                    layoutParams = (layoutParams as LinearLayout.LayoutParams).apply {
                        marginEnd = 48
                    }
                    text = "User: ${message.content}"
                }
                MessageRole.ASSISTANT -> {
                    setTextColor(resources.getColor(R.color.teal_200))
                    layoutParams = (layoutParams as LinearLayout.LayoutParams).apply {
                        marginStart = 48
                    }
                    text = "Assistant: ${message.content}"
                }
                MessageRole.SYSTEM -> {
                    setTextColor(resources.getColor(R.color.grey))
                    text = "System: ${message.content}"
                }
            }
        }
        chatHistoryLayout.addView(textView)
    }

    private fun addErrorMessageToUI(message: String) {
        val textView = TextView(this).apply {
            textSize = 14f
            setTextColor(resources.getColor(R.color.red))
            setPadding(12, 8, 12, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
            text = message
        }
        chatHistoryLayout.addView(textView)
    }

    private fun testTokenizer() {
        val text = tokenizerInputEditText.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
            return
        }

        val tokenizer = model.getTokenizer()
        val tokens = tokenizer.encode(text)
        val decoded = tokenizer.decode(tokens)

        tokenizerResultTextView.text = "Original: $text\n\n" +
                "Token count: ${tokens.size}\n" +
                "Tokens: ${tokens.joinToString(", ", limit = 50)}\n\n" +
                "Decoded: $decoded"

        updateStatus("Tokenizer Test Completed", Status.READY)
    }

    private fun showVocabInfo() {
        val tokenizer = model.getTokenizer()
        tokenizerResultTextView.text = "Vocabulary Info:\n\n" +
                "Vocab Size: ${tokenizer.getVocabSize()}\n" +
                "BOS Token: ${tokenizer.getBosToken()}\n" +
                "EOS Token: ${tokenizer.getEosToken()}"

        updateStatus("Vocab Info Retrieved", Status.READY)
    }

    private fun updatePerformanceStats(tokensPerSec: Float, memoryBytes: Long, timeMs: Long, tokens: Int) {
        tokensPerSecondTextView.text = String.format("%.2f", tokensPerSec)
        memoryUsageTextView.text = String.format("%.2f", memoryBytes / (1024.0 * 1024))
        inferenceTimeTextView.text = timeMs.toString()
        generatedTokensTextView.text = tokens.toString()
    }

    private fun updateStatus(message: String, status: Status) {
        statusTextView.text = "Status: $message"
        statusIndicator.background = when (status) {
            Status.READY -> resources.getDrawable(R.drawable.circle_green)
            Status.BUSY -> resources.getDrawable(R.drawable.circle_yellow)
            Status.ERROR -> resources.getDrawable(R.drawable.circle_red)
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${String.format("%.2f", bytes / 1024.0)} KB"
            bytes < 1024 * 1024 * 1024 -> "${String.format("%.2f", bytes / (1024.0 * 1024))} MB"
            else -> "${String.format("%.2f", bytes / (1024.0 * 1024 * 1024))} GB"
        }
    }

    enum class Status {
        READY, BUSY, ERROR
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "MainActivity onDestroy")
        inferenceJob?.cancel()
        if (isModelLoaded) {
            Log.i(TAG, "Closing session and model")
            session.close()
            model.close()
        }
        Log.i(TAG, "Shutting down LokAI Engine")
        LokAIEngine.shutdown()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        
        // 处理文件选择结果
        if (requestCode == REQUEST_SELECT_MODEL_FILE && resultCode == RESULT_OK) {
            Log.d(TAG, "onActivityResult: Received file selection result")
            data?.data?.let { uri ->
                Log.d(TAG, "onActivityResult: Selected URI = $uri")
                try {
                    // 获取文件的实际路径
                    val path = getPathFromUri(uri)
                    if (path != null) {
                        modelPathEditText.setText(path)
                        Log.i(TAG, "Selected model file: $path")
                        Toast.makeText(this, "Selected: ${java.io.File(path).name}", Toast.LENGTH_SHORT).show()
                        
                        // 验证文件是否存在
                        val testFile = java.io.File(path)
                        Log.d(TAG, "onActivityResult: Test file exists = ${testFile.exists()}, size = ${testFile.length()} bytes")
                    } else {
                        Log.e(TAG, "onActivityResult: Failed to get file path")
                        Toast.makeText(this, "Failed to get file path", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get file path: ${e.message}", e)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } ?: run {
                Log.e(TAG, "onActivityResult: URI is null")
            }
        }
        
        // 处理存储权限结果
        if (requestCode == REQUEST_MANAGE_STORAGE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Log.i(TAG, "MANAGE_EXTERNAL_STORAGE permission granted")
                    Toast.makeText(this, "文件访问权限已获得", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w(TAG, "MANAGE_EXTERNAL_STORAGE permission denied")
                    Toast.makeText(this, "需要文件访问权限才能加载模型", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Storage permission granted")
                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Log.w(TAG, "Storage permission denied")
                Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }
}