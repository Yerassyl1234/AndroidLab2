package com.example.chatlibrary

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatlibrary.adapter.ChatAdapter
import com.example.chatlibrary.databinding.ActivityChatBinding
import com.example.chatlibrary.model.ChatMessage
import com.example.chatlibrary.model.MessageType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

internal class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private var webSocket: WebSocket? = null
    private lateinit var okHttpClient: OkHttpClient

    private val webSocketUrl = "wss://echo.websocket.org/"
    private val specialMessageCode = 0xcb.toByte()
    private val specialInputTrigger = "203 = 0xcb"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupWebSocket()
        setupSendButton()

        supportActionBar?.title = "WebSocket Chat"
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        binding.recyclerViewChat.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupWebSocket() {
        okHttpClient = OkHttpClient()
        val request = Request.Builder().url(webSocketUrl).build()
        val listener = EchoWebSocketListener()
        webSocket = okHttpClient.newWebSocket(request, listener)

    }

    private fun setupSendButton() {
        binding.buttonSend.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                var messageSentSuccessfully = false

                if (messageText == specialInputTrigger) {
                    val byteToSend = ByteString.of(specialMessageCode)
                    messageSentSuccessfully = webSocket?.send(byteToSend) ?: false
                    if (!messageSentSuccessfully) {

                    }
                } else {
                    messageSentSuccessfully = webSocket?.send(messageText) ?: false
                    if (!messageSentSuccessfully) {

                    }
                }

                if (messageSentSuccessfully) {
                    val chatMessage = ChatMessage(messageText, MessageType.SENT)
                    addMessageToAdapter(chatMessage)
                    binding.editTextMessage.text.clear()
                } else {

                }
            }
        }
    }

    private fun sendMessage(message: String) {
        val sent = webSocket?.send(message)
        if (sent == true) {
            val chatMessage = ChatMessage(message, MessageType.SENT)
            runOnUiThread {
                addMessageToAdapter(chatMessage)
            }
        } else {
            Log.e("ChatActivity", "Failed to send message: WebSocket not available or queue full.")
        }
    }

    private fun addMessageToAdapter(message: ChatMessage) {
        chatAdapter.addMessage(message)
        binding.recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Activity Destroyed")
        Log.d("ChatActivity", "WebSocket connection closed.")
    }

    inner class EchoWebSocketListener : WebSocketListener() {
        private val TAG = "WebSocketListener"
        private val NORMAL_CLOSURE_STATUS = 1000
        private val PREDEFINED_MESSAGE = "Special server message received! (203 = 0xcb)"

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket Connection Opened: ${response.message}")
            runOnUiThread {
                addMessageToAdapter(ChatMessage("Connected to server.", MessageType.RECEIVED))
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Receiving Text: $text")
            val chatMessage = ChatMessage(text, MessageType.RECEIVED)
            runOnUiThread {
                addMessageToAdapter(chatMessage)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d(TAG, "Receiving Bytes: ${bytes.hex()}")
            val messageText = if (bytes.size == 1 && bytes[0] == specialMessageCode) {
                PREDEFINED_MESSAGE
            } else {
                "Received binary data: ${bytes.hex()}"
            }
            val chatMessage = ChatMessage(messageText, MessageType.RECEIVED)
            runOnUiThread {
                addMessageToAdapter(chatMessage)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "Closing: $code / $reason")
            runOnUiThread {
                addMessageToAdapter(
                    ChatMessage(
                        "Server closing connection: $reason",
                        MessageType.RECEIVED
                    )
                )
            }
            webSocket.close(NORMAL_CLOSURE_STATUS, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "Closed: $code / $reason")
            runOnUiThread {
                addMessageToAdapter(ChatMessage("Connection closed: $reason", MessageType.RECEIVED))
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "Error: " + t.message, t)
            runOnUiThread {
                addMessageToAdapter(
                    ChatMessage(
                        "Connection Error: ${t.message}",
                        MessageType.RECEIVED
                    )
                )
            }
        }
    }
}