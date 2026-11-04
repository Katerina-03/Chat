package com.kate.chat.ui

import android.content.Context
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kate.chat.R
import com.kate.chat.mvp.UiError

fun View.applySystemInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars =
            insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
        v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        insets
    }
}

fun Context.showErrorDialog(error: UiError) {
    val message = when (error) {
        UiError.NO_CONNECTION -> R.string.err_no_connection
        UiError.SERVER -> R.string.err_server
        UiError.UNKNOWN -> R.string.err_unknown
    }
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.error_title)
        .setMessage(message)
        .setPositiveButton(R.string.ok, null)
        .show()
}

