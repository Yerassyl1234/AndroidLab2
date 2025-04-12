package com.example.chatlibrary

import android.content.Context
import android.content.Intent

object ChatLauncher {

    /**
     * Launches the chat activity.
     *
     * @param context The application or activity context.
     */
    @JvmStatic
    fun start(context: Context) {
        val intent = Intent(context, ChatActivity::class.java).apply {
            // Add NEW_TASK flag if launching from a non-activity context might occur
            // flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}