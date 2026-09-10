package com.chat.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: ChatViewModel
    private lateinit var chatAdapter: ChatAdapter
    private var hasJoined = false
    private var token = ""
    private var displayName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        token = intent.getStringExtra("token") ?: ""
        displayName = intent.getStringExtra("displayName") ?: ""

        if (token.isEmpty()) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)
        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)

        tvWelcome.text = "Welcome, $displayName!"

        chatAdapter = ChatAdapter(emptyList(), displayName)
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = chatAdapter

        btnSend.setOnClickListener {
            val message = etMessage.text.toString().trim()
            if (message.isEmpty()) return@setOnClickListener
            viewModel.sendMessage(message)
            etMessage.text.clear()
        }

        viewModel.messages.observe(this) { messages ->
            chatAdapter = ChatAdapter(messages, displayName)
            rvMessages.adapter = chatAdapter
            if (messages.isNotEmpty()) {
                rvMessages.scrollToPosition(messages.size - 1)
            }
        }

        viewModel.isConnected.observe(this) { connected ->
            tvStatus.text = if (connected) "Connected" else "Disconnected"
            tvStatus.setTextColor(
                if (connected) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
            )
            btnSend.isEnabled = connected
        }

        viewModel.connectWithToken(token)
        hasJoined = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (hasJoined) {
            viewModel.disconnect()
        }
    }
}
