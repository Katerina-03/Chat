package com.kate.chat.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kate.chat.R
import com.kate.chat.data.Chat
import com.kate.chat.databinding.ItemChatBinding

private object ChatDiff : DiffUtil.ItemCallback<Chat>() {
    override fun areItemsTheSame(a: Chat, b: Chat) = a.name == b.name
    override fun areContentsTheSame(a: Chat, b: Chat) = a == b
}

class ChatsAdapter(private val onClick: (Chat) -> Unit) :
    ListAdapter<Chat, ChatsAdapter.Holder>(ChatDiff) {

    var selectedChatName: String? = null
        set(value) {
            val old = field
            field = value
            currentList.forEachIndexed { index, chat ->
                if (chat.name == old || chat.name == value) notifyItemChanged(index)
            }
        }

    class Holder(val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(
            ItemChatBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val chat = getItem(position)
        val last = chat.lastMessage
        holder.binding.chatName.text = chat.name
        holder.binding.chatLast.text = when {
            last == null -> ""
            last.imageLink != null -> "${last.from}: ${holder.itemView.context.getString(R.string.photo)}"
            else -> "${last.from}: ${last.text}"
        }
        holder.binding.root.setBackgroundResource(
            if (chat.name == selectedChatName) R.drawable.chat_selected
            else android.R.color.transparent
        )
        holder.binding.root.setOnClickListener { onClick(chat) }
    }
}
