package com.kate.chat.mvp

import com.kate.chat.data.ChatRepository
import com.kate.chat.data.Message
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class ChatPresenter(private val chat: String, repo: ChatRepository) :
    BasePresenter<ChatContract.View>(repo), ChatContract.Presenter {

    private var messages: List<Message> = emptyList()
    private var loadingMore = false
    private var hasMore = true

    override fun onAttach() {
        val firstLoad = messages.isEmpty()
        if (!firstLoad) view?.showMessages(messages, ScrollMode.KEEP)
        else refresh()
        startPolling()
    }

    override fun onLoadMore() {
        val oldest = messages.firstOrNull() ?: return
        if (loadingMore || !hasMore) return
        loadingMore = true
        view?.showLoadingMore(true)
        launch {
            try {
                val page = repo.messages(chat, MESSAGE_COUNT, oldest.id)
                hasMore = page.size >= MESSAGE_COUNT && page.any { it.id < oldest.id }
                merge(page, ScrollMode.KEEP)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleError(e)
            } finally {
                loadingMore = false
                view?.showLoadingMore(false)
            }
        }
    }

    private fun refresh() {
        launch {
            try {
                val latest = repo.messages(chat, MESSAGE_COUNT, null)
                if (messages.isEmpty()) hasMore = latest.size >= MESSAGE_COUNT
                merge(latest, ScrollMode.BOTTOM)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun merge(incoming: List<Message>, mode: ScrollMode) {
        val merged = (messages + incoming).associateBy { it.id }.values.sortedBy { it.id }
        if (merged != messages) {
            messages = merged
            view?.showMessages(merged, mode)
        }
    }

    private fun startPolling() {
        launch {
            while (true) {
                delay(POLL_INTERVAL_MS.milliseconds)
                try {
                    merge(
                        repo.messages(chat, MESSAGE_COUNT, null),
                        ScrollMode.FOLLOW
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
        }
    }

    private companion object {
        const val MESSAGE_COUNT = 20
        const val POLL_INTERVAL_MS = 5_000L
    }
}
