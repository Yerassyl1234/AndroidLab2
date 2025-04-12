package com.example.lab2

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.chatlibrary.ChatLauncher

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val launchChatButton: Button = findViewById(R.id.buttonLaunchChat)
        launchChatButton.setOnClickListener {
            ChatLauncher.start(this)
        }
    }
}