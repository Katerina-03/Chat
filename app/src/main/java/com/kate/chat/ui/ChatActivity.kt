package com.kate.chat.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kate.chat.R
import com.kate.chat.data.Chat
import com.kate.chat.data.Message
import com.kate.chat.databinding.ActivityChatBinding
import com.kate.chat.mvp.ChatContract
import com.kate.chat.mvp.ChatPresenter
import com.kate.chat.mvp.ChatsContract
import com.kate.chat.mvp.ChatsPresenter
import com.kate.chat.mvp.ScrollMode
import com.kate.chat.mvp.UiError
import com.kate.chat.repository

class ChatActivity : AppCompatActivity(), ChatContract.View, ChatsContract.View {

    private lateinit var binding: ActivityChatBinding
    private lateinit var presenter: ChatPresenter
    private var chatsPresenter: ChatsPresenter? = null
    private var currentChatName: String = ""
    private val isLandscape: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    private lateinit var layoutManager: LinearLayoutManager

    private val chatsAdapter = ChatsAdapter { chatsPresenter?.onChatClicked(it) }

    private val adapter = MessagesAdapter { link ->
        startActivity(ImageActivity.intent(this, link))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemInsets()

        currentChatName = savedInstanceState?.getString(STATE_CURRENT_CHAT)
            ?: intent.getStringExtra(EXTRA_CHAT).orEmpty()
        val chatName = currentChatName
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = chatName

        presenter = ChatPresenter(chatName, repository)

        if (isLandscape) {
            chatsPresenter = ChatsPresenter(repository)
            findViewById<RecyclerView>(R.id.chatsList).apply {
                layoutManager = LinearLayoutManager(this@ChatActivity)
                adapter = chatsAdapter
            }
            findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
                .setOnRefreshListener { chatsPresenter?.onRefresh() }
            chatsAdapter.selectedChatName = currentChatName
        }

        layoutManager = LinearLayoutManager(this)
        binding.messagesList.layoutManager = layoutManager
        binding.messagesList.adapter = adapter
        binding.messagesList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy < 0 && layoutManager.findFirstVisibleItemPosition() <= 2)
                    presenter.onLoadMore()
            }
        })

    }

    override fun onStart() {
        super.onStart()
        presenter.attach(this)
        chatsPresenter?.attach(this)
    }

    override fun onStop() {
        presenter.detach()
        chatsPresenter?.detach()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_CURRENT_CHAT, currentChatName)
        super.onSaveInstanceState(outState)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun showLoading(show: Boolean) {
        if (isLandscape)
            findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
                .isRefreshing = show
    }

    override fun showChats(chats: List<Chat>) {
        if (isLandscape) {
            chatsAdapter.selectedChatName = currentChatName
            chatsAdapter.submitList(chats) {
                chatsAdapter.selectedChatName = currentChatName
                scrollToSelectedChat()
            }
            findViewById<android.widget.TextView>(R.id.emptyView).isVisible = chats.isEmpty()
        }
    }

    override fun openChat(name: String) {
        if (name == currentChatName) return
        presenter.detach()
        currentChatName = name
        title = name
        chatsAdapter.selectedChatName = name
        scrollToSelectedChat()
        presenter = ChatPresenter(name, repository)
        presenter.attach(this)
    }

    private fun scrollToSelectedChat() {
        if (!isLandscape) return

        val chatsList = findViewById<RecyclerView>(R.id.chatsList)
        chatsList.post {
            val position = chatsAdapter.currentList.indexOfFirst { it.name == currentChatName }
            if (position >= 0) {
                chatsList.scrollToPosition(position)
            }
        }
    }

    override fun showMessages(messages: List<Message>, scroll: ScrollMode) {
        val list = binding.messagesList
        val firstPos = layoutManager.findFirstVisibleItemPosition()
        val anchorId = adapter.currentList.getOrNull(firstPos)?.id
        val anchorOffset = layoutManager.findViewByPosition(firstPos)?.top ?: 0
        val wasAtBottom = !list.canScrollVertically(1)

        adapter.submitList(messages) {
            when (scroll) {
                ScrollMode.BOTTOM ->
                    if (messages.isNotEmpty()) list.scrollToPosition(messages.lastIndex)

                ScrollMode.FOLLOW ->
                    if (wasAtBottom && messages.isNotEmpty()) list.scrollToPosition(messages.lastIndex)

                ScrollMode.KEEP -> {
                    val index = messages.indexOfFirst { it.id == anchorId }
                    if (index >= 0) layoutManager.scrollToPositionWithOffset(index, anchorOffset)
                }
            }
        }
    }

    override fun showLoadingMore(show: Boolean) {
        binding.loadingBar.isVisible = show
    }

    override fun showError(error: UiError) = showErrorDialog(error)

    companion object {
        private const val EXTRA_CHAT = "chat"
        private const val STATE_CURRENT_CHAT = "current_chat"
        fun intent(context: Context, chat: String) =
            Intent(context, ChatActivity::class.java)
                .putExtra(EXTRA_CHAT, chat)
    }
}
