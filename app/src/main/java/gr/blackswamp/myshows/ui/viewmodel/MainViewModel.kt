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
import gr.blackswamp.myshows.logic.IListLogic
import gr.blackswamp.myshows.logic.IShowLogic
import gr.blackswamp.myshows.logic.ListLogic
import gr.blackswamp.myshows.logic.ShowLogic
import gr.blackswamp.myshows.ui.fragments.DisplayFragment
import gr.blackswamp.myshows.ui.fragments.ListFragment
import gr.blackswamp.myshows.ui.model.ShowDetailVO
import gr.blackswamp.myshows.ui.model.ShowVO
import gr.blackswamp.myshows.util.AppSchedulers
import gr.blackswamp.myshows.util.MediatorPairLiveData
import gr.blackswamp.myshows.util.SingleLiveEvent


class MainViewModel(application: Application) : AndroidViewModel(application), ListFragment.ListViewModel, DisplayFragment.ShowViewModel, IMainViewModel {
    val loading = MutableLiveData<Boolean>() //if the loading overlay should be shown
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

    private val listLogic: IListLogic = ListLogic(this, MovieDBClient.service, App.database, AppSchedulers) //object that handles the logic behind the list
    private val showLogic: IShowLogic = ShowLogic(this, App.database, AppSchedulers) //object that handles the logic behind the display

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
        listLogic.showSelected(show.id, show.isMovie)
    }

    override fun displayShowList() {
        listLogic.displayShowList()
    }

    override fun displayWatchList() {
        listLogic.displayWatchList()
    }

    override fun searchItems(query: String) {
        listLogic.searchShows(query)
    }

    override fun delete(show: ShowVO) {

    }

    override fun loadNext() {
        listLogic.loadNextShows()
    }

    override fun refresh() {
        listLogic.refreshData()
    }
    //endregion

    //region incoming from show
    override fun toggleFavourite() {

    }

    override fun exitShows() = showLogic.exit()
    //endregion

    override fun onCleared() {
        listLogic.clear()
    }
}