package com.kate.chat.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kate.chat.databinding.ActivityImageBinding
import com.kate.chat.mvp.UiError
import com.kate.chat.repository
import kotlinx.coroutines.launch

class ImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val link = intent.getStringExtra(EXTRA_LINK).orEmpty()

        binding.progress.isVisible = true

        lifecycleScope.launch {
            try {
                val bitmap = repository.getImage(link)
                binding.imageView.setImageBitmap(bitmap)
                binding.progress.isVisible = false
            } catch (_: Exception) {
                binding.progress.isVisible = false
                showErrorDialog(UiError.NO_CONNECTION)
            }
        }

        binding.imageView.setOnClickListener { finish() }
    }

    companion object {
        private const val EXTRA_LINK = "link"
        fun intent(context: Context, link: String) =
            Intent(context, ImageActivity::class.java)
                .putExtra(EXTRA_LINK, link)
    }
}
