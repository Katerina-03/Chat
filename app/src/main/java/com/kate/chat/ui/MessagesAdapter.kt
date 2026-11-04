package com.kate.chat.ui

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kate.chat.R
import com.kate.chat.data.Message
import com.kate.chat.databinding.ItemMessageBinding
import com.kate.chat.repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private object MessageDiff : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(a: Message, b: Message) = a.id == b.id
    override fun areContentsTheSame(a: Message, b: Message) = a == b
}

class MessagesAdapter(private val onImageClick: (String) -> Unit) :
    ListAdapter<Message, MessagesAdapter.Holder>(MessageDiff) {

    private val adapterScope = CoroutineScope(Dispatchers.Main + Job())

    class Holder(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        var imageLoadJob: Job? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(
            ItemMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val message = getItem(position)
        val b = holder.binding
        holder.imageLoadJob?.cancel()

        b.messageImage.setImageDrawable(null)
        b.bubble.setBackgroundResource(R.drawable.bubble_other)
        b.sender.isVisible = true
        b.sender.text = message.from

        val link = message.imageLink
        if (link != null) {
            b.messageText.isVisible = false
            b.messageImage.isVisible = true
            b.messageImage.setOnClickListener { onImageClick(link) }

            holder.imageLoadJob = adapterScope.launch {
                try {

                    val repository = holder.itemView.context.repository
                    val bitmap: Bitmap = repository.getThumbnail(link)
                    b.messageImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    if (e !is kotlinx.coroutines.CancellationException) {
                        b.messageImage.setImageDrawable(null)
                        b.messageImage.isVisible = false
                        b.messageText.isVisible = true
                        b.messageText.text = message.text
                    }
                }
            }
        } else {
            b.messageImage.isVisible = false
            b.messageText.isVisible = true
            b.messageText.text = message.text
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        adapterScope.launch { }.cancel()
    }
}

