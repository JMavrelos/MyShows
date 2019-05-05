package gr.blackswamp.myshows.ui.viewmodel

import androidx.annotation.StringRes
import gr.blackswamp.myshows.ui.model.ViewState

interface IMainViewModel {
    fun showError(@StringRes messageId: Int, param: String? = null)
    fun showLoading(show: Boolean)
    fun updateState(state: ViewState)
}
