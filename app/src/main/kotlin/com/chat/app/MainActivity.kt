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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val btnJoin = findViewById<Button>(R.id.btnJoin)
        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)

        chatAdapter = ChatAdapter(emptyList(), "")
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = chatAdapter

        btnJoin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.setUsername(username)
            viewModel.connect()
            hasJoined = true
            etUsername.isEnabled = false
            btnJoin.isEnabled = false
            etMessage.requestFocus()
        }

        btnSend.setOnClickListener {
            val message = etMessage.text.toString().trim()
            if (!hasJoined) {
                Toast.makeText(this, "Please join the chat first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.sendMessage(message)
            etMessage.text.clear()
        }

        viewModel.messages.observe(this) { messages ->
            chatAdapter = ChatAdapter(messages, viewModel.username.value ?: "")
            rvMessages.adapter = chatAdapter
            if (messages.isNotEmpty()) {
                rvMessages.scrollToPosition(messages.size - 1)
            }
        }

        viewModel.isConnected.observe(this) { connected ->
            tvStatus.text = if (connected) "Connected" else "Disconnected"
            btnSend.isEnabled = connected && hasJoined
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (hasJoined) {
            viewModel.sendLeave()
        }
    }
}
