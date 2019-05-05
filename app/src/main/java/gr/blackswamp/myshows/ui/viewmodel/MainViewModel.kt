package gr.blackswamp.myshows.ui.viewmodel

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import gr.blackswamp.myshows.App
import gr.blackswamp.myshows.R
import gr.blackswamp.myshows.data.api.MovieDBClient
import gr.blackswamp.myshows.logic.IMainLogic
import gr.blackswamp.myshows.logic.MainLogic
import gr.blackswamp.myshows.ui.fragments.DisplayFragment
import gr.blackswamp.myshows.ui.fragments.ListFragment
import gr.blackswamp.myshows.ui.model.ShowDetailVO
import gr.blackswamp.myshows.ui.model.ShowVO
import gr.blackswamp.myshows.ui.model.ViewState
import gr.blackswamp.myshows.util.AppSchedulers
import gr.blackswamp.myshows.util.MediatorPairLiveData
import gr.blackswamp.myshows.util.MediatorTripleLiveData
import gr.blackswamp.myshows.util.SingleLiveEvent


class MainViewModel(application: Application) : AndroidViewModel(application), ListFragment.ListViewModel, DisplayFragment.ShowViewModel, IMainViewModel {
    override val loading = MutableLiveData<Boolean>() //if the loading overlay should be shown
    val error = SingleLiveEvent<String?>() // an error that should be displayed, when consumed it reverts to its initial state

    private val currentFilter = MutableLiveData<String>() // the current searchFilter that should be displayed
    override val show = MutableLiveData<ShowDetailVO>() //the currently selectionChanged show, if null then we are focusing on the list
    override val showWatchListed = MutableLiveData<Boolean>() //if the currently selectionChanged show is in the watchlist, null if there is no selectionChanged show
    val shows = MutableLiveData<List<ShowVO>>() //the list of shows that have been retrieved
    private val hasWatchList = MutableLiveData<Boolean>() //if there are shows saved in the watch later list
    override val displayingShowList = MutableLiveData<Boolean>() //holds a boolean that stores if the current view should be a show list or a watch list

    //region livedata that is derived in some form from the actual data received
    override val listTitle = MediatorPairLiveData<Boolean, String, String>(displayingShowList, currentFilter) //holds the title that will be shown in the list fragment
    { d, s -> application.getString(if (d == true) R.string.app_name else R.string.watchlist) + if (!s.isNullOrBlank()) ": $s" else "" }
    override val canGoToWatchlist = MediatorPairLiveData<Boolean, Boolean, Boolean>(displayingShowList, hasWatchList) { d, w -> (d == true && w == true) } //declares if the goto watchlist menu item should be displayed
    override val canGoToShows: LiveData<Boolean> = Transformations.map(displayingShowList) { !it }  //declares if the goto shows menu item should be displayed
    override val canLoadMore = MutableLiveData<Boolean>() // indicates if there are more items in the list
    override val showInitialMessage = MediatorTripleLiveData<Boolean, List<ShowVO>, String, Boolean>(displayingShowList, shows, currentFilter) { d, s, f -> (d == true && s?.size ?: 0 == 0 && f.isNullOrEmpty()) }
    private val logic: IMainLogic = MainLogic(this, MovieDBClient.service, App.database, AppSchedulers) //object that handles the logic behind the list
    override val searchFilter: String get() = currentFilter.value ?: ""
    override val adapterFilter = MediatorPairLiveData<Boolean, String, String>(displayingShowList, currentFilter) { d, c -> if (d == true) "" else c ?: "" }
    override val showList = Transformations.map(shows) { Pair<List<ShowVO>, String?>(it, adapterFilter.value) }!!
    //endregion

    //region incoming from logic
    override fun showError(@StringRes messageId: Int, param: String?) =
        error.postValue(getApplication<Application>().getString(messageId, param))

    override fun showLoading(show: Boolean) = loading.postValue(show)

    /** Used to update screen state from logic*/
    override fun updateState(state: ViewState) {
        state.shows?.let { shows.postValue(it) }
        if (state.selectionChanged) {
            show.postValue(state.show)
        }
        state.hasMore?.let { canLoadMore.postValue(it) }
        state.filter?.let { currentFilter.postValue(it) }
        state.inShows?.let { displayingShowList.postValue(it) }
        state.hasWatchlist?.let { hasWatchList.postValue(it) }
        state.watchListed?.let { showWatchListed.postValue(it) }
    }

    //endregion


    //region incoming from list
    override fun select(show: ShowVO) {
        logic.showSelected(show.id, displayingShowList.value ?: true)
    }

    override fun displayShowList() {
        logic.displayShowList()
    }

    override fun displayWatchList() {
        logic.displayWatchList()
    }

    override fun searchItems(query: String, submit: Boolean) {
        logic.searchShows(query, submit)
    }

    override fun delete(show: ShowVO) {
        logic.deleteItem(show)
    }

    override fun loadNext() {
        logic.loadNextShows()
    }

    override fun refresh() {
        logic.refreshData()
    }
    //endregion

    //region incoming from display
    override fun toggleFavourite() {
        logic.toggleItem()
    }

    override fun exitDisplay() {
        logic.exitDisplay()
    }
    //endregion

    override fun onCleared() {
        logic.clear()
    }
}