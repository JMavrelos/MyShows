package gr.blackswamp.myshows.ui.viewmodel

import androidx.annotation.StringRes
import gr.blackswamp.myshows.ui.model.ShowDetailVO
import gr.blackswamp.myshows.ui.model.ShowVO
import gr.blackswamp.myshows.ui.model.ViewState

interface IMainViewModel {
    fun showError(@StringRes messageId: Int, param: String? = null)
//    fun setShows(shows: List<ShowVO>, hasMore: Boolean, filter: String)
    fun showLoading(show: Boolean)
//    fun showDetails(detail: ShowDetailVO?)
//    fun showList(isShows: Boolean, shows: List<ShowVO>, hasMore: Boolean, filter: String)
//    fun setHasWatchlist(has: Boolean)
    fun updateState(state: ViewState)
}
