package com.example.chatlibrary.model

data class ChatMessage(
    val text: String,
    val type: MessageType
)

enum class MessageType {
    SENT, RECEIVED
}