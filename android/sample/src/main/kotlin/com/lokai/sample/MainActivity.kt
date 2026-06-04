package com.lokai.sample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lokai.sdk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    private val TAG = "LokAI-Demo"

    private lateinit var model: Model
    private lateinit var session: InferenceSession
    private var isModelLoaded = false

    private lateinit var statusTextView: TextView
    private lateinit var modelPathEditText: EditText
    private lateinit var inputEditText: EditText
    private lateinit var outputTextView: TextView
    private lateinit var loadModelBtn: Button
    private lateinit var generateBtn: Button
    private lateinit var chatBtn: Button
    private lateinit var clearBtn: Button
    private lateinit var tokenizerBtn: Button

    private val REQUEST_STORAGE_PERMISSION = 101
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
        statusTextView = findViewById(R.id.statusTextView)
        modelPathEditText = findViewById(R.id.modelPathEditText)
        inputEditText = findViewById(R.id.inputEditText)
        outputTextView = findViewById(R.id.outputTextView)
        loadModelBtn = findViewById(R.id.loadModelBtn)
        generateBtn = findViewById(R.id.generateBtn)
        chatBtn = findViewById(R.id.chatBtn)
        clearBtn = findViewById(R.id.clearBtn)
        tokenizerBtn = findViewById(R.id.tokenizerBtn)

        val defaultPath = "/storage/emulated/0/Download/model.gguf"
        modelPathEditText.setText(defaultPath)
        Log.i(TAG, "Default model path: $defaultPath")

        loadModelBtn.setOnClickListener { loadModel() }
        generateBtn.setOnClickListener { generateText() }
        chatBtn.setOnClickListener { chatCompletion() }
        clearBtn.setOnClickListener { clearOutput() }
        tokenizerBtn.setOnClickListener { testTokenizer() }

        generateBtn.isEnabled = false
        chatBtn.isEnabled = false
        tokenizerBtn.isEnabled = false
        Log.i(TAG, "Views initialized")
    }

    private fun initSDK() {
        Log.i(TAG, "Initializing SDK...")
        statusTextView.text = "Initializing SDK..."
        CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "Calling LokAIEngine.initialize()")
            val result = LokAIEngine.initialize(this@MainActivity)
            runOnUiThread {
                if (result.isSuccess) {
                    Log.i(TAG, "SDK initialization successful")
                    statusTextView.text = "✅ SDK Initialized\nVersion: ${LokAIEngine.getVersion()}"
                    loadModelBtn.isEnabled = true
                } else {
                    Log.e(TAG, "SDK initialization failed: ${result.exceptionOrNull()?.message}")
                    statusTextView.text = "❌ SDK Initialization Failed\n${result.exceptionOrNull()?.message}"
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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

    private fun loadModel() {
        val modelPath = modelPathEditText.text.toString().trim()
        Log.i(TAG, "loadModel called with path: $modelPath")

        if (modelPath.isEmpty()) {
            Toast.makeText(this, "Please enter model path", Toast.LENGTH_SHORT).show()
            return
        }

        statusTextView.text = "Loading model..."
        loadModelBtn.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val modelFile = File(modelPath)
            Log.i(TAG, "Checking if model file exists: $modelPath")
            Log.i(TAG, "File exists: ${modelFile.exists()}")
            Log.i(TAG, "File readable: ${modelFile.canRead()}")
            if (modelFile.exists()) {
                Log.i(TAG, "File size: ${modelFile.length()} bytes")
            }

            if (!modelFile.exists()) {
                runOnUiThread {
                    statusTextView.text = "❌ Model file not found:\n$modelPath"
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

                    statusTextView.text = "✅ Model Loaded!\n" +
                            "Name: ${model.info.name}\n" +
                            "Size: ${formatSize(model.info.size)}\n" +
                            "Context: ${model.info.contextWindowSize}"

                    generateBtn.isEnabled = true
                    chatBtn.isEnabled = true
                    tokenizerBtn.isEnabled = true
                    loadModelBtn.isEnabled = true
                }.onFailure { e ->
                    Log.e(TAG, "Model load failed: ${e.message}", e)
                    statusTextView.text = "❌ Model Load Failed\n${e.message}"
                    loadModelBtn.isEnabled = true
                }
            }
        }
    }

    private fun generateText() {
        val prompt = inputEditText.text.toString()
        if (prompt.isEmpty()) {
            Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show()
            return
        }

        statusTextView.text = "Generating..."
        generateBtn.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val logPrompt = if (prompt.length > 100) prompt.substring(0, 100) else prompt
            Log.i(TAG, "Calling session.complete() with prompt: $logPrompt")
            val result = session.complete(prompt, InferenceParams(maxTokens = 256))
            runOnUiThread {
                result.onSuccess { completion ->
                    Log.i(TAG, "Generation completed successfully")
                    outputTextView.text = "${prompt}${completion.text}"
                    statusTextView.text = "✅ Completed\n" +
                            "Tokens: ${completion.tokenCount}\n" +
                            "Time: ${completion.inferenceTime}ms\n" +
                            "Speed: ${String.format("%.2f", completion.stats.tokensPerSecond)} tokens/s"
                }.onFailure { e ->
                    Log.e(TAG, "Generation failed: ${e.message}", e)
                    statusTextView.text = "❌ Generation Failed\n${e.message}"
                }
                generateBtn.isEnabled = true
            }
        }
    }

    private fun chatCompletion() {
        val userInput = inputEditText.text.toString()
        if (userInput.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        statusTextView.text = "Chatting..."
        chatBtn.isEnabled = false

        val messages = listOf(
            ChatMessage(MessageRole.USER, userInput)
        )

        CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "Calling session.chat()")
            val result = session.chat(messages, InferenceParams(maxTokens = 256))
            runOnUiThread {
                result.onSuccess { completion ->
                    Log.i(TAG, "Chat completed successfully")
                    outputTextView.text = "User: $userInput\n\nAssistant: ${completion.text}"
                    statusTextView.text = "✅ Chat Completed\n" +
                            "Tokens: ${completion.tokenCount}\n" +
                            "Time: ${completion.inferenceTime}ms"
                }.onFailure { e ->
                    Log.e(TAG, "Chat failed: ${e.message}", e)
                    statusTextView.text = "❌ Chat Failed\n${e.message}"
                }
                chatBtn.isEnabled = true
            }
        }
    }

    private fun testTokenizer() {
        val text = inputEditText.text.toString()
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
            return
        }

        val tokenizer = model.getTokenizer()
        val tokens = tokenizer.encode(text)
        val decoded = tokenizer.decode(tokens)

        outputTextView.text = "Original: $text\n\n" +
                "Token count: ${tokens.size}\n" +
                "Tokens: ${tokens.joinToString(", ", limit = 20)}\n\n" +
                "Decoded: $decoded"

        statusTextView.text = "✅ Tokenizer Test Completed\nVocab Size: ${tokenizer.getVocabSize()}"
    }

    private fun clearOutput() {
        outputTextView.text = ""
        inputEditText.text.clear()
        statusTextView.text = "Ready"
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${String.format("%.2f", bytes / 1024.0)} KB"
            bytes < 1024 * 1024 * 1024 -> "${String.format("%.2f", bytes / (1024.0 * 1024))} MB"
            else -> "${String.format("%.2f", bytes / (1024.0 * 1024 * 1024))} GB"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "MainActivity onDestroy")
        if (isModelLoaded) {
            Log.i(TAG, "Closing session and model")
            session.close()
            model.close()
        }
        Log.i(TAG, "Shutting down LokAI Engine")
        LokAIEngine.shutdown()
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
