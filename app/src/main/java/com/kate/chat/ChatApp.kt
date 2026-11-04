package com.kate.chat

import android.app.Application
import android.content.Context
import com.kate.chat.data.ChatRepository
import com.kate.chat.data.NetworkModule

class ChatApp : Application() {
    val repository: ChatRepository by lazy {
        ChatRepository(NetworkModule.createChatApi())
    }
}

val Context.repository: ChatRepository
    get() = (applicationContext as ChatApp).repository
