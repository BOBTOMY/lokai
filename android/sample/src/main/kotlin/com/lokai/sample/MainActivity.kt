package com.lokai.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.lokai.sdk.LokAIEngine

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SDK
        LokAIEngine.initialize(this)

        val inputText = findViewById<EditText>(R.id.inputText)
        val outputText = findViewById<TextView>(R.id.outputText)
        val generateBtn = findViewById<Button>(R.id.generateBtn)

        generateBtn.setOnClickListener {
            val prompt = inputText.text.toString()
            if (prompt.isNotEmpty()) {
                val modelManager = LokAIEngine.getModelManager()
                // Note: This is a placeholder, actual model loading requires a GGUF model file
                outputText.text = "Initializing... (Model loading requires GGUF file)"
            }
        }
    }
}
