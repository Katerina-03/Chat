package com.kate.chat.mvp

import com.kate.chat.data.ApiException
import com.kate.chat.data.Chat
import com.kate.chat.data.Message
import java.io.IOException

enum class UiError { NO_CONNECTION, SERVER, UNKNOWN }

enum class ScrollMode { BOTTOM, KEEP, FOLLOW }

fun Throwable.toUiError(): UiError = when (this) {
    is ApiException -> UiError.SERVER
    is IOException -> UiError.NO_CONNECTION
    else -> UiError.UNKNOWN
}

interface BaseView {
    fun showError(error: UiError)
}

interface ChatsContract {
    interface View : BaseView {
        fun showLoading(show: Boolean)
        fun showChats(chats: List<Chat>)
        fun openChat(name: String)
    }

    interface Presenter {
        fun onRefresh()
        fun onQueryChanged(query: String)
        fun onChatClicked(chat: Chat)
    }
}

interface ChatContract {
    interface View : BaseView {
        fun showMessages(messages: List<Message>, scroll: ScrollMode)
        fun showLoadingMore(show: Boolean)
    }

    interface Presenter {
        fun onLoadMore()
    }
}
