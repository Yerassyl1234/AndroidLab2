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
import java.util.regex.Pattern

internal class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private var webSocket: WebSocket? = null
    private lateinit var okHttpClient: OkHttpClient

    private val webSocketUrl = "wss://echo.websocket.org/"
    private val specialMessageCode = 0xcb.toByte()

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

    private val byteInputPattern = Pattern.compile("^(\\d+)\\s*=\\s*0x([0-9a-fA-F]+)$")
    private val sendButtonTag = "ChatSendLogic"

    private fun setupSendButton() {
        binding.buttonSend.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString().trim()
            Log.d(sendButtonTag, "Send button clicked. Raw input: '$messageText'")

            if (messageText.isNotEmpty()) {
                var messageSentSuccessfully = false
                var shouldSendAsByte = false
                var byteToSend: ByteString? = null

                val matcher = byteInputPattern.matcher(messageText)
                if (matcher.matches()) {
                    Log.d(sendButtonTag, "Input MATCHES pattern 'DEC = 0xHEX'.")

                    val decimalString = matcher.group(1)
                    val hexString = matcher.group(2)
                    Log.d(sendButtonTag, "Extracted parts: decimal='$decimalString', hex='$hexString'")

                    try {
                        val decimalValue = decimalString?.toIntOrNull()
                        val hexValue = hexString?.toIntOrNull(16)
                        Log.d(sendButtonTag, "Converted values: decimal=$decimalValue, hex=$hexValue")

                        if (decimalValue != null && hexValue != null && decimalValue == hexValue) {
                            Log.d(sendButtonTag, "Values ARE equal and not null.")

                            if (hexValue in 0..255) {
                                Log.d(sendButtonTag, "Hex value $hexValue is IN byte range (0-255).")

                                val byteValue = hexValue.toByte()
                                byteToSend = ByteString.of(byteValue)
                                shouldSendAsByte = true
                                Log.d(sendButtonTag, "--> Condition MET to send as byte: ${byteToSend?.hex()}")

                            } else {
                                Log.w(sendButtonTag, "Hex value $hexValue is OUT of byte range (0-255). Will send as text.")
                                shouldSendAsByte = false
                            }
                        } else {
                            Log.w(sendButtonTag, "Values are NOT equal or one is null. Will send as text.")
                            shouldSendAsByte = false
                        }
                    } catch (e: Exception) {
                        Log.e(sendButtonTag, "Error during conversion or comparison. Will send as text.", e)
                        shouldSendAsByte = false
                    }
                } else {
                    Log.d(sendButtonTag, "Input does NOT match pattern 'DEC = 0xHEX'. Will send as text.")
                    shouldSendAsByte = false
                }

                if (shouldSendAsByte && byteToSend != null) {
                    Log.i(sendButtonTag, ">>> SENDING BYTE: ${byteToSend.hex()}")
                    messageSentSuccessfully = webSocket?.send(byteToSend) ?: false
                    if (!messageSentSuccessfully) {
                        Log.e(sendButtonTag, "Failed immediate WebSocket send (byte).")
                    }
                } else {
                    Log.i(sendButtonTag, ">>> SENDING TEXT: '$messageText'")
                    messageSentSuccessfully = webSocket?.send(messageText) ?: false
                    if (!messageSentSuccessfully) {
                        Log.e(sendButtonTag, "Failed immediate WebSocket send (text).")
                    }
                }

                if (messageSentSuccessfully) {
                    Log.d(TAG_SEND, "Message added to adapter and input cleared.")
                    val chatMessage = ChatMessage(messageText, MessageType.SENT)
                    addMessageToAdapter(chatMessage)
                    binding.editTextMessage.text.clear()
                } else {
                    Log.w(TAG_SEND,"WebSocket send call returned false, UI not updated for send.")
                }
            } else {
                Log.d(TAG_SEND, "Input was empty after trimming.")
            }
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
        private val PREDEFINED_MESSAGE = "Special server message received!"

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
            val chatMessage = ChatMessage(PREDEFINED_MESSAGE, MessageType.RECEIVED)
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