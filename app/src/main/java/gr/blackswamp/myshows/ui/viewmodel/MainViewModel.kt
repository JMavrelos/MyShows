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
import gr.blackswamp.myshows.util.AppSchedulers
import gr.blackswamp.myshows.util.MediatorPairLiveData
import gr.blackswamp.myshows.util.SingleLiveEvent


class MainViewModel(application: Application) : AndroidViewModel(application), ListFragment.ListViewModel, DisplayFragment.ShowViewModel, IMainViewModel {
    override val loading = MutableLiveData<Boolean>() //if the loading overlay should be shown
    val error = SingleLiveEvent<String?>() // an error that should be displayed, when consumed it reverts to its initial state

    override val searchFilter = MutableLiveData<String>() // the current searchFilter that should be displayed
    override val show = MutableLiveData<ShowDetailVO>() //the currently selected show, if null then we are focusing on the list
    override val showInWatchlist = MutableLiveData<Boolean>() //if the currently selected show is in the watchlist, null if there is no selected show
    override val showList = MutableLiveData<List<ShowVO>>() //the list of shows that have been retrieved
    private val hasWatchList = MutableLiveData<Boolean>() //if there are shows saved in the watch later list
    override val displayingShowList = MutableLiveData<Boolean>() //holds a boolean that stores if the current view should be a show list or a watch list
    //holds the title that will be shown in the list fragment
    override val listTitle = MediatorPairLiveData<Boolean, String, String>(displayingShowList, searchFilter) { d, s ->
        if (!s.isNullOrBlank()) {
            s
        } else if (d == true) {
            application.getString(R.string.app_name)
        } else {
            application.getString(R.string.watchlist)
        }
    }
    override val canGoToWatchlist = MediatorPairLiveData<Boolean, Boolean, Boolean>(displayingShowList, hasWatchList) { d, w -> (d == true && w == true) } //declares if the goto watchlist menu item should be displayed
    override val canGoToShows: LiveData<Boolean> = Transformations.map(displayingShowList) { !it }  //declares if the goto shows menu item should be displayed
    override val canLoadMore = MutableLiveData<Boolean>() // indicates if there are more items in the list

    private val logic: IMainLogic = MainLogic(this, MovieDBClient.service, App.database, AppSchedulers) //object that handles the logic behind the list

    init {
        canLoadMore.postValue(false)
    }

    //region incoming from logic
    override fun showError(@StringRes messageId: Int, param: String?) =
        error.postValue(getApplication<Application>().getString(messageId, param))

    override fun showLoading(show: Boolean) = loading.postValue(show)

    override fun setShows(shows: List<ShowVO>, hasMore: Boolean, filter: String) {
        showList.postValue(shows)
        canLoadMore.postValue(hasMore)
        searchFilter.postValue(filter)
    }

    override fun showDetails(detail: ShowDetailVO?) = show.postValue(detail)

    override fun showList(isShows: Boolean, shows: List<ShowVO>, hasMore: Boolean, filter: String) {
        displayingShowList.postValue(isShows)
        setShows(shows, hasMore, filter)
    }

    override fun setHasWatchlist(has: Boolean) {
        hasWatchList.postValue(has)
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

    override fun searchItems(query: String) {
        logic.searchShows(query)
    }

    override fun delete(show: ShowVO) {
        logic.deleteItem(show.id)
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

    }

    override fun exitDisplay() {
        logic.exitDisplay()
    }
    //endregion

    override fun onCleared() {
        logic.clear()
    }
}