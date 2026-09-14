package com.chat.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {

    private lateinit var etLoginId: EditText
    private lateinit var etPassword: EditText
    private lateinit var etDisplayName: EditText
    private lateinit var etServerAddress: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvDisplayNameLabel: TextView

    private var isRegisterMode = false
    private var serverUrl = "http://192.168.1.2:8080"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etLoginId = findViewById(R.id.etLoginId)
        etPassword = findViewById(R.id.etPassword)
        etDisplayName = findViewById(R.id.etDisplayName)
        etServerAddress = findViewById(R.id.etServerAddress)

        tvDisplayNameLabel = findViewById(R.id.tvDisplayNameLabel)

        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        tvStatus = findViewById(R.id.tvStatus)

        btnLogin.setOnClickListener {

            val address = etServerAddress.text.toString().trim()
            if (address.isNotEmpty()) {
                serverUrl = "http://$address:8080"
            }

            val loginId = etLoginId.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (loginId.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please fill all fields",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (isRegisterMode) {
                register(loginId, password)
            } else {
                login(loginId, password)
            }
        }

        btnRegister.setOnClickListener {

            isRegisterMode = !isRegisterMode

            if (isRegisterMode) {

                btnLogin.text = "Register"
                btnRegister.text = "Already have an account? Login"

                tvDisplayNameLabel.visibility = View.VISIBLE
                etDisplayName.visibility = View.VISIBLE

            } else {

                btnLogin.text = "Login"
                btnRegister.text = "Don't have an account? Register"

                tvDisplayNameLabel.visibility = View.GONE
                etDisplayName.visibility = View.GONE
            }

            tvStatus.text = ""
        }
    }

    private fun login(loginId: String, password: String) {

        tvStatus.text = "Logging in..."
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

        Thread {

            try {

                val url = URL("$serverUrl/login")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("loginId", loginId)
                    put("password", password)
                }

                OutputStreamWriter(conn.outputStream).use {
                    it.write(json.toString())
                    it.flush()
                }

                val responseCode = conn.responseCode

                val response = if (responseCode == 200) {
                    conn.inputStream
                        .bufferedReader()
                        .readText()
                } else {
                    conn.errorStream
                        ?.bufferedReader()
                        ?.readText()
                        ?: "Error"
                }

                conn.disconnect()

                val result = JSONObject(response)

                val success = result.getBoolean("success")
                val message = result.optString(
                    "message",
                    "Login failed"
                )

                val token = result.optString(
                    "token",
                    ""
                )

                val displayName = result.optString(
                    "displayName",
                    ""
                )

                runOnUiThread {

                    if (success) {

                        val intent =
                            Intent(this, MainActivity::class.java).apply {

                                putExtra("token", token)

                                putExtra(
                                    "displayName",
                                    displayName
                                )

                                putExtra(
                                    "loginId",
                                    loginId
                                )

                                putExtra(
                                    "sharedKey",
                                    "chat-app-shared-key-2024"
                                )

                                putExtra(
                                    "serverAddress",
                                    serverUrl
                                )
                            }

                        startActivity(intent)
                        finish()

                    } else {

                        tvStatus.text = message
                        tvStatus.setTextColor(
                            ContextCompat.getColor(this@LoginActivity, R.color.disconnected)
                        )
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    tvStatus.text =
                        "Connection error: ${e.message}"

                    tvStatus.setTextColor(
                        ContextCompat.getColor(this@LoginActivity, R.color.disconnected)
                    )
                }
            }

        }.start()
    }

    private fun register(
        loginId: String,
        password: String
    ) {

        val displayName =
            etDisplayName.text.toString().trim()

        if (displayName.isEmpty()) {

            Toast.makeText(
                this,
                "Please enter a display name",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        tvStatus.text = "Registering..."
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

        Thread {

            try {

                val url = URL("$serverUrl/register")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"

                conn.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.doOutput = true

                val json = JSONObject().apply {

                    put("loginId", loginId)
                    put("password", password)
                    put("displayName", displayName)
                }

                OutputStreamWriter(conn.outputStream).use {

                    it.write(json.toString())
                    it.flush()
                }

                val responseCode = conn.responseCode

                val response = if (responseCode == 200) {

                    conn.inputStream
                        .bufferedReader()
                        .readText()

                } else {

                    conn.errorStream
                        ?.bufferedReader()
                        ?.readText()
                        ?: "Error"
                }

                conn.disconnect()

                val result = JSONObject(response)

                val success =
                    result.getBoolean("success")

                val message =
                    result.optString(
                        "message",
                        "Registration failed"
                    )

                val token =
                    result.optString(
                        "token",
                        ""
                    )

                runOnUiThread {

                    if (success) {

                        Toast.makeText(
                            this,
                            "Registration successful! Logging in...",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent =
                            Intent(
                                this,
                                MainActivity::class.java
                            ).apply {

                                putExtra(
                                    "token",
                                    token
                                )

                                putExtra(
                                    "displayName",
                                    displayName
                                )

                                putExtra(
                                    "loginId",
                                    loginId
                                )

                                putExtra(
                                    "sharedKey",
                                    "chat-app-shared-key-2024"
                                )

                                putExtra(
                                    "serverAddress",
                                    serverUrl
                                )
                            }

                        startActivity(intent)
                        finish()

                    } else {

                        tvStatus.text = message

                        tvStatus.setTextColor(
                            0xFFF44336.toInt()
                        )
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    tvStatus.text =
                        "Connection error: ${e.message}"

                    tvStatus.setTextColor(
                        ContextCompat.getColor(this@LoginActivity, R.color.disconnected)
                    )
                }
            }

        }.start()
    }
}