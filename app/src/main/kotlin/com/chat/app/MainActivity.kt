package com.chat.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chat.app.crypto.EncryptionUtils
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ChatViewModel
    private lateinit var chatAdapter: ChatAdapter

    private var hasJoined = false
    private var token = ""
    private var displayName = ""

    private val typingHandler = Handler(Looper.getMainLooper())
    private var isTyping = false

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->

        uri?.let {
            viewModel.sendMedia(it)

            Toast.makeText(
                this,
                "Uploading file...",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        token = intent.getStringExtra("token") ?: ""
        displayName = intent.getStringExtra("displayName") ?: ""

        val sharedKey = intent.getStringExtra("sharedKey") ?: "chat-app-shared-key-2024"
        EncryptionUtils.setSharedKey(sharedKey)

        requestNotificationPermission()
        saveAuthToken(token)

        if (token.isEmpty()) {

            Toast.makeText(
                this,
                "Not authenticated",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        viewModel =
            ViewModelProvider(this)[ChatViewModel::class.java]

        val tvStatus =
            findViewById<TextView>(R.id.tvStatus)

        val tvWelcome =
            findViewById<TextView>(R.id.tvWelcome)

        val tvAvatar =
            findViewById<TextView>(R.id.tvAvatar)

        val etMessage =
            findViewById<EditText>(R.id.etMessage)

        val btnSend =
            findViewById<MaterialButton>(R.id.btnSend)

        val btnAttach =
            findViewById<ImageButton>(R.id.btnAttach)

        val rvMessages =
            findViewById<RecyclerView>(R.id.rvMessages)


        // -----------------------------
        // Header
        // -----------------------------

        tvWelcome.text =
            displayName.ifEmpty { "Chat" }

        tvAvatar.text =
            displayName
                .trim()
                .firstOrNull()
                ?.uppercase()
                ?: "C"


        // -----------------------------
        // RecyclerView
        // -----------------------------

        chatAdapter =
            ChatAdapter(emptyList(), displayName) { action, message ->
                when (action) {
                    "DELETE" -> {
                        viewModel.deleteMessage(message.id)
                    }
                    "EDIT" -> {
                        viewModel.editMessage(message.id, message.content)
                    }
                }
            }

        rvMessages.layoutManager =
            LinearLayoutManager(this).apply {
                stackFromEnd = true
            }

        rvMessages.adapter = chatAdapter


        // -----------------------------
        // Send Message
        // -----------------------------

        btnSend.setOnClickListener {

            val message =
                etMessage.text.toString().trim()

            if (message.isEmpty()) {
                return@setOnClickListener
            }

            if (!viewModel.isConnected.value!!) {

                Toast.makeText(
                    this,
                    "Not connected to server",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            viewModel.sendMessage(message)

            etMessage.text.clear()

            viewModel.sendStopTyping()
            isTyping = false
        }


        // -----------------------------
        // Keyboard Send
        // -----------------------------

        etMessage.setOnEditorActionListener {
                _,
                actionId,
                _ ->

            if (actionId == EditorInfo.IME_ACTION_SEND) {

                btnSend.performClick()

                true

            } else {

                false
            }
        }


        // -----------------------------
        // Typing Indicator
        // -----------------------------

        etMessage.addTextChangedListener(object : TextWatcher {
            private var typingRunnable: Runnable? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim() ?: ""

                if (text.isNotEmpty() && !isTyping) {
                    isTyping = true
                    viewModel.sendTyping()
                }

                typingRunnable?.let { typingHandler.removeCallbacks(it) }

                typingRunnable = Runnable {
                    if (isTyping) {
                        isTyping = false
                        viewModel.sendStopTyping()
                    }
                }
                typingHandler.postDelayed(typingRunnable!!, 2000)
            }
        })


        // -----------------------------
        // Attach File
        // -----------------------------

        btnAttach.setOnClickListener {
            showFileChooser()
        }


        // -----------------------------
        // Messages Observer
        // -----------------------------

        viewModel.messages.observe(this) { messages ->

            chatAdapter =
                ChatAdapter(messages, displayName) { action, message ->
                    when (action) {
                        "DELETE" -> {
                            viewModel.deleteMessage(message.id)
                        }
                        "EDIT" -> {
                            viewModel.editMessage(message.id, message.content)
                        }
                    }
                }

            rvMessages.adapter = chatAdapter

            if (messages.isNotEmpty()) {

                rvMessages.scrollToPosition(
                    messages.size - 1
                )
            }
        }


        // -----------------------------
        // Typing Users Observer
        // -----------------------------

        val tvTypingIndicator = findViewById<TextView>(R.id.tvTypingIndicator)

        viewModel.typingUsers.observe(this) { typingUsers ->

            val typingNames = typingUsers.filter { it != displayName }

            when {
                typingNames.isEmpty() -> {
                    tvTypingIndicator.visibility = android.view.View.GONE
                }
                typingNames.size == 1 -> {
                    tvTypingIndicator.text = "${typingNames[0]} is typing..."
                    tvTypingIndicator.visibility = android.view.View.VISIBLE
                }
                typingNames.size == 2 -> {
                    tvTypingIndicator.text = "${typingNames[0]} and ${typingNames[1]} are typing..."
                    tvTypingIndicator.visibility = android.view.View.VISIBLE
                }
                else -> {
                    tvTypingIndicator.text = "${typingNames.size} people are typing..."
                    tvTypingIndicator.visibility = android.view.View.VISIBLE
                }
            }
        }


        // -----------------------------
        // Connection Status
        // -----------------------------

        viewModel.isConnected.observe(this) { connected ->

            tvStatus.text =
                if (connected) {
                    "Connected"
                } else {
                    "Disconnected"
                }

            tvStatus.setTextColor(
                if (connected) {
                    0xFF25D366.toInt()
                } else {
                    0xFFFFCDD2.toInt()
                }
            )

            btnSend.isEnabled = connected
        }


        // -----------------------------
        // Error Observer
        // -----------------------------

        viewModel.error.observe(this) { errorMsg ->

            errorMsg?.let {

                Toast.makeText(
                    this,
                    it,
                    Toast.LENGTH_LONG
                ).show()

                viewModel.clearError()
            }
        }


        // -----------------------------
        // Connect
        // -----------------------------

        viewModel.connectWithToken(token, displayName)

        hasJoined = true
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun saveAuthToken(token: String) {
        getSharedPreferences("chat_prefs", MODE_PRIVATE)
            .edit()
            .putString("auth_token", token)
            .apply()
    }


    private fun showFileChooser() {

        filePickerLauncher.launch("*/*")
    }


    override fun onDestroy() {

        if (hasJoined) {
            viewModel.disconnect()
        }

        super.onDestroy()
    }
}
