package com.kate.chat.mvp

import com.kate.chat.data.Chat
import com.kate.chat.data.ChatRepository
import kotlinx.coroutines.CancellationException

class ChatsPresenter(repo: ChatRepository) :
    BasePresenter<ChatsContract.View>(repo), ChatsContract.Presenter {

    private var all: List<Chat> = emptyList()
    private var query: String = ""

    override fun onAttach() {
        val current = repo.currentChats()
        if (current != null) {
            all = current
            view?.showChats(filtered())
        } else {
            load(forceRefresh = false)
        }
    }

    override fun onRefresh() = load(forceRefresh = true)

    override fun onQueryChanged(query: String) {
        this.query = query
        view?.showChats(filtered())
    }

    override fun onChatClicked(chat: Chat) {
        view?.openChat(chat.name)
    }

    private fun filtered(): List<Chat> =
        if (query.isBlank()) all else all.filter {
            it.name.contains(query.trim(), ignoreCase = true)
        }

    private fun load(forceRefresh: Boolean) {
        view?.showLoading(true)
        launch {
            try {
                all = repo.chats(forceRefresh)
                view?.showChats(filtered())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleError(e)
            } finally {
                view?.showLoading(false)
            }
        }
    }
}
