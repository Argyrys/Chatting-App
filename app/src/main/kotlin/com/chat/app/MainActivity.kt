package com.chat.app

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ChatViewModel
    private lateinit var chatAdapter: ChatAdapter

    private var hasJoined = false
    private var token = ""
    private var displayName = ""

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
            ChatAdapter(emptyList(), displayName)

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
                ChatAdapter(messages, displayName)

            rvMessages.adapter = chatAdapter

            if (messages.isNotEmpty()) {

                rvMessages.scrollToPosition(
                    messages.size - 1
                )
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

        viewModel.connectWithToken(token)

        hasJoined = true
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