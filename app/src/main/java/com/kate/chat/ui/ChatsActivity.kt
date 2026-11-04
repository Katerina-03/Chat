package com.kate.chat.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kate.chat.R
import com.kate.chat.data.Chat
import com.kate.chat.data.Message
import com.kate.chat.databinding.ActivityChatsBinding
import com.kate.chat.mvp.ChatContract
import com.kate.chat.mvp.ChatPresenter
import com.kate.chat.mvp.ChatsContract
import com.kate.chat.mvp.ChatsPresenter
import com.kate.chat.mvp.ScrollMode
import com.kate.chat.mvp.UiError
import com.kate.chat.repository

class ChatsActivity : AppCompatActivity(), ChatsContract.View, ChatContract.View {

    private lateinit var binding: ActivityChatsBinding
    private lateinit var presenter: ChatsPresenter
    private var selectedChatName: String? = null
    private var chatPresenter: ChatPresenter? = null
    private var messagesAdapter: MessagesAdapter? = null
    private var messagesLayoutManager: LinearLayoutManager? = null

    private val isLandscape: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private val adapter = ChatsAdapter { presenter.onChatClicked(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityChatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemInsets()
        setSupportActionBar(binding.toolbar)
        title = getString(R.string.chats)

        selectedChatName = savedInstanceState?.getString(STATE_SELECTED_CHAT)
        presenter = ChatsPresenter(repository)
        binding.chatsList.layoutManager = LinearLayoutManager(this)
        binding.chatsList.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { presenter.onRefresh() }

        if (isLandscape) setupLandscapeMessages()
    }

    private fun setupLandscapeMessages() {
        val list = findViewById<RecyclerView>(R.id.messagesList)
        messagesLayoutManager = LinearLayoutManager(this)
        messagesAdapter = MessagesAdapter { link ->
            startActivity(ImageActivity.intent(this, link))
        }
        list.layoutManager = messagesLayoutManager
        list.adapter = messagesAdapter
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy < 0 && (messagesLayoutManager?.findFirstVisibleItemPosition() ?: Int.MAX_VALUE) <= 2) {
                    chatPresenter?.onLoadMore()
                }
            }
        })

        findViewById<TextView>(R.id.selectChatView).isVisible = selectedChatName == null
        findViewById<TextView>(R.id.selectedChatTitle).apply {
            text = selectedChatName.orEmpty()
            isVisible = selectedChatName != null
        }
        adapter.selectedChatName = selectedChatName
    }

    override fun onStart() {
        super.onStart()
        presenter.attach(this)
        if (isLandscape) selectedChatName?.let(::showSelectedChat)
    }

    override fun onStop() {
        chatPresenter?.detach()
        chatPresenter = null
        presenter.detach()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SELECTED_CHAT, selectedChatName)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chats, menu)
        val search = menu.findItem(R.id.action_search).actionView as SearchView
        search.queryHint = getString(R.string.search)
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                presenter.onQueryChanged(newText.orEmpty())
                return true
            }
        })
        return true
    }

    override fun showLoading(show: Boolean) {
        binding.swipeRefresh.isRefreshing = show
    }

    override fun showChats(chats: List<Chat>) {
        adapter.submitList(chats)
        binding.emptyView.isVisible = chats.isEmpty()
        adapter.selectedChatName = selectedChatName
    }

    override fun showError(error: UiError) = showErrorDialog(error)

    override fun openChat(name: String) {
        if (!isLandscape) {
            startActivity(ChatActivity.intent(this, name))
            return
        }
        if (selectedChatName == name) return
        selectedChatName = name
        adapter.selectedChatName = name
        showSelectedChat(name)
    }

    private fun showSelectedChat(name: String) {
        if (!isLandscape) return
        findViewById<TextView>(R.id.selectChatView).isVisible = false
        findViewById<TextView>(R.id.selectedChatTitle).apply {
            text = name
            isVisible = true
        }
        messagesAdapter?.submitList(emptyList())
        chatPresenter?.detach()
        chatPresenter = ChatPresenter(name, repository).also { it.attach(this) }
    }

    override fun showMessages(messages: List<Message>, scroll: ScrollMode) {
        val list = findViewById<RecyclerView>(R.id.messagesList)
        val layoutManager = messagesLayoutManager ?: return
        val adapter = messagesAdapter ?: return
        val firstPos = layoutManager.findFirstVisibleItemPosition()
        val anchorId = adapter.currentList.getOrNull(firstPos)?.id
        val anchorOffset = layoutManager.findViewByPosition(firstPos)?.top ?: 0
        val wasAtBottom = !list.canScrollVertically(1)

        adapter.submitList(messages) {
            when (scroll) {
                ScrollMode.BOTTOM -> if (messages.isNotEmpty()) list.scrollToPosition(messages.lastIndex)
                ScrollMode.FOLLOW -> if (wasAtBottom && messages.isNotEmpty()) list.scrollToPosition(messages.lastIndex)
                ScrollMode.KEEP -> {
                    val index = messages.indexOfFirst { it.id == anchorId }
                    if (index >= 0) layoutManager.scrollToPositionWithOffset(index, anchorOffset)
                }
            }
        }
    }

    override fun showLoadingMore(show: Boolean) {
        if (isLandscape)
            findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.loadingBar)
                .isVisible = show
    }

    private companion object {
        const val STATE_SELECTED_CHAT = "selected_chat"
    }
}
