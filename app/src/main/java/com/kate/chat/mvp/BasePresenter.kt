package com.kate.chat.mvp

import com.kate.chat.data.ApiException
import com.kate.chat.data.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

abstract class BasePresenter<V : BaseView>(protected val repo: ChatRepository) {
    protected var view: V? = null
        private set
    private var scope: CoroutineScope? = null

    fun attach(view: V) {
        this.view = view
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        onAttach()
    }

    fun detach() {
        scope?.cancel()
        scope = null
        view = null
    }

    protected open fun onAttach() {}

    protected fun launch(block: suspend CoroutineScope.() -> Unit): Job? =
        scope?.launch(block = block)

    protected fun handleError(e: Throwable) {
        view?.showError(
            if (e is ApiException) UiError.SERVER else e.toUiError()
        )
    }
}
