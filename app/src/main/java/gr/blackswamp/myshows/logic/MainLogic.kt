package gr.blackswamp.myshows.logic

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import gr.blackswamp.myshows.R
import gr.blackswamp.myshows.data.api.MovieDBService
import gr.blackswamp.myshows.data.db.AppDatabase
import gr.blackswamp.myshows.data.db.MovieDO
import gr.blackswamp.myshows.data.db.TvShowDO
import gr.blackswamp.myshows.logic.model.Show
import gr.blackswamp.myshows.ui.model.ShowVO
import gr.blackswamp.myshows.ui.model.ViewState
import gr.blackswamp.myshows.ui.viewmodel.IMainViewModel
import gr.blackswamp.myshows.util.ISchedulers
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

/**
 * This is the logic object that coordinates all actions between the model and the viewmodel
 */
class MainLogic(private val vm: IMainViewModel, private val service: MovieDBService, private val db: AppDatabase, private val schedulers: ISchedulers) : IMainLogic {
    companion object {
        @Suppress("unused")
        const val TAG = "MainLogic"
    }

    private val disposables = CompositeDisposable() //holds all observables that should be disposed on clear
    internal val showList = mutableListOf<Show>() //the currently loaded list of shows from the service
    internal val watchList = mutableListOf<Show>() //the list of shows that are added to the watchlist
    internal var page = 0 //holds up to what page we have currently loaded
    internal var maxPages = 0 //holds the number of pages in the service api
    internal var showFilter: String = "" //holds the last saved filter that was used to search in the service
    internal var watchFilter: String = "" //holds the last saved filter that was used to filter the items of the watchlist
    internal var inShows: Boolean = true //if true then the user is currently viewing the service search results, otherwise their watch list
    internal var show: Show? = null //if not null then the user is viewing the list otherwise the details of this show


    private var filter = MutableLiveData<String>()

    init {
        loadInitialData()
    }

    override fun searchShows(newFilter: String, submit: Boolean) {
        if (inShows && submit) { //if we are searching in shows then we start the search on submit

            //if the newFilter is less than 1 character we return an error because it is not allowed by the api
            if (newFilter.isEmpty()) {
                gotError(R.string.error_invalid_filter, null)
                return
            }
            vm.showLoading(true)
            doSearchShows("", newFilter, 1)
        } else if (!inShows) { //if we are searching in the watchlist we start the search on text change
            watchFilter = newFilter
            updateViewState(filter = newFilter) //sending back the new filter will mean that the view will update its adapter and the adapter will perform the filtering
        }
    }

    override fun loadNextShows() {
        vm.showLoading(true)
        doSearchShows(showFilter, showFilter, page + 1)
    }

    override fun refreshData() = searchShows(showFilter, true)

    override fun displayShowList() {
        inShows = true
        updateViewState(
            shows = showList.map { it.copy() }
            , inShows = inShows
            , hasMore = page < maxPages
            , filter = showFilter)
    }

    override fun displayWatchList() {
        if (watchList.size == 0) {
            vm.showError(R.string.error_no_watchlist)
        } else {
            inShows = false
            updateViewState(
                shows = watchList.map { it.copy() }
                , inShows = inShows
                , hasMore = false
                , filter = watchFilter)
        }
    }

    override fun showSelected(id: Int, fromShowList: Boolean) {
        vm.showLoading(true)
        val show: Show? = if (fromShowList) showList.firstOrNull { it.id == id } else watchList.firstOrNull { it.id == id }
        @Suppress("CascadeIf")
        if (show == null) {
            gotError(R.string.error_show_not_found, null)
        } else if (fromShowList) {
            doLoadShowDetails(show)
        } else {
            gotDetails(show)
        }
    }

    override fun toggleItem() {
        vm.showLoading(true)
        val selected = show
        if (selected != null) {
            if (watchList.count { it.id == selected.id } == 1) {
                updateViewState(watchListed = false)
                doDeleteItem(selected.id, selected.isMovie)
            } else {
                updateViewState(watchListed = true)
                doAddItem(selected)
            }
        } else {
            gotError(R.string.error_show_not_select, null)
        }
    }

    override fun deleteItem(show: ShowVO) {
        vm.showLoading(true)
        doDeleteItem(show.id, show.isMovie)
    }

    override fun exitDisplay() {
        show = null
        updateViewState(selected = true, watchListed = false)
    }

    private fun loadInitialData() {
        vm.showLoading(true)
        watchFilter = ""

        disposables.add(
            Observable.fromCallable {
                db.loadWatchlist()
            }.subscribeOn(schedulers.subscribeScheduler)
                .observeOn(schedulers.observeScheduler)
                .map { list -> list.map { Show(it) } }
                .subscribe(
                    {
                        watchList.addAll(it)
                        updateViewState(
                            inShows = inShows
                            , hasMore = false
                            , hasWatchlist = watchList.size > 0
                        )
                        vm.showLoading(false)
                    }
                    , {
                        updateViewState(
                            inShows = inShows
                            , hasMore = false
                            , hasWatchlist = false
                        )
                        gotError(R.string.error_loading_data, it)
                    }
                )
        )
    }

    private fun doSearchShows(currentFilter: String, newFilter: String, page: Int) {
        disposables.add(
            service
                .getShows(newFilter, page)
                .subscribeOn(schedulers.subscribeScheduler)
                .observeOn(schedulers.observeScheduler)
                .map { result -> Pair(result.pages, result.results.asSequence().filter { it.media_type != "person" }.map { Show(it) }.toList()) }
                .subscribe(
                    { gotShows(currentFilter, newFilter, page, it.first, it.second) }
                    , { gotError(R.string.error_retrieving_data, it) }
                )
        )
    }

    private fun doLoadShowDetails(show: Show) {
        if (show.isMovie)
            disposables.add(
                service
                    .getMovieDetails(show.id)
                    .subscribeOn(schedulers.subscribeScheduler)
                    .observeOn(schedulers.observeScheduler)
                    .map { Show(show, it) }
                    .subscribe(
                        { gotDetails(it) }
                        , { gotError(R.string.error_retrieving_data, it) }
                    )
            )
        else
            disposables.add(
                service
                    .getTvDetails(show.id)
                    .subscribeOn(schedulers.subscribeScheduler)
                    .observeOn(schedulers.observeScheduler)
                    .map { Show(show, it) }
                    .subscribe(
                        { gotDetails(it) }
                        , { gotError(R.string.error_retrieving_data, it) }
                    )
            )
    }

    private fun doDeleteItem(id: Int, isMovie: Boolean) {
        disposables.add(
            Observable.fromCallable {
                db.deleteWatchlistItem(id, isMovie)
            }.subscribeOn(schedulers.subscribeScheduler)
                .observeOn(schedulers.observeScheduler)
                .subscribe(
                    { result ->//if the operation succeeded
                        watchList.clear()  // we reload the watchlist
                        watchList.addAll(result.map { Show(it) })
                        val inWatchlist = show?.let { s -> watchList.count { s.id == it.id } != 0 } //check if the currently shown item (if any) is in the new watchlist
                        if (inShows) { //and if we are displaying shows (i.e. the previous screen we came from was the shows) we update the watchlist variables
                            updateViewState(hasWatchlist = watchList.size > 0, watchListed = inWatchlist) //we update the watchlist variables
                        } else if (watchList.isEmpty()) { //if the reply is empty, (and we are not in the shows) we move to the shows and load the appropriate data
                            inShows = true
                            updateViewState(shows = showList, inShows = inShows, filter = showFilter, hasMore = page < maxPages, hasWatchlist = watchList.size > 0, watchListed = inWatchlist)
                        } else { //otherwise we update the currently shown list (because we may have deleted by swiping on the list) and the watchlist item
                            updateViewState(shows = watchList, hasMore = false, hasWatchlist = watchList.size > 0, watchListed = inWatchlist)
                        }
                        vm.showLoading(false)
                    }
                    , { throwable -> //on error
                        val inWatchlist = show?.let { s -> watchList.count { s.id == it.id } != 0 } //check if the currently shown item (if any) is in the watchlist
                        if (inShows) //we update the watchlist variables
                            updateViewState(hasWatchlist = watchList.size > 0, watchListed = inWatchlist)
                        else //or if we were in the watchlist, the show list too
                            updateViewState(shows = watchList, hasMore = false, hasWatchlist = watchList.size > 0, watchListed = inWatchlist)
                        gotError(R.string.error_delete_watchlist, throwable)
                    }
                )
        )
    }

    private fun doAddItem(selected: Show) {
        disposables.add(
            Observable.fromCallable {
                db.addWatchlistItem(if (selected.isMovie) MovieDO(selected) else TvShowDO(selected))
            }.subscribeOn(schedulers.subscribeScheduler)
                .observeOn(schedulers.observeScheduler)
                .subscribe(
                    { result -> //if the operation succeeded
                        watchList.clear() //,  we reload the watchlist
                        watchList.addAll(result.map { Show(it) })
                        val inWatchlist = show?.let { s -> watchList.count { s.id == it.id } != 0 } //check if the currently shown item is in the new watchlist
                        if (inShows) {  //and if we are displaying shows (i.e. the previous screen we came from was the shows) we update the watchlist variables
                            updateViewState(hasWatchlist = watchList.size > 0, watchListed = inWatchlist)
                        } else { //if we are not (i.e. we came here from the watchlist, removed and readded the item) we update the watchlist variables and the shown list
                            updateViewState(hasWatchlist = watchList.size > 0, shows = watchList, watchListed = inWatchlist)
                        }
                        vm.showLoading(false)
                    }, { throwable -> //if there was an error
                        val inWatchlist = show?.let { s -> watchList.count { s.id == it.id } != 0 }
                        if (inShows) { //we update the watchlist variables
                            updateViewState(hasWatchlist = watchList.size > 0, watchListed = inWatchlist)
                        } else { //or if we were in the watchlist, the show list too
                            updateViewState(hasWatchlist = watchList.size > 0, shows = watchList, watchListed = inWatchlist)
                        }
                        gotError(R.string.error_add_watchlist, throwable) //and display the error
                    }
                )
        )
    }

    private fun gotError(@StringRes msgId: Int, error: Throwable?) {
        if (error?.message != null) {
            vm.showError(msgId, error.message)
        } else {
            vm.showError(msgId)
        }
        vm.showLoading(false)
    }

    private fun gotShows(currentFilter: String, newFilter: String, loadedPage: Int, pages: Int, results: List<Show>) {
        if (results.isEmpty() && pages == 1) { //if there are no results and the max number of pages received is 1 that means we could not find anything useful
            vm.showError(R.string.error_no_results)
            vm.showLoading(false)
            return
        } else if (results.isEmpty() && loadedPage >= pages) { //if there are no results and the currently loaded page is at its maximum then we cannot load anything more
            vm.showError(R.string.error_no_more_shows)
            vm.showLoading(false)
            return
        } else if (results.isEmpty()) { //if there are no results (but because we are in the else clause) and we are not at the maximum, load the next page
            page = loadedPage
            maxPages = pages
            doSearchShows(currentFilter, newFilter, page + 1)
        } else if (newFilter != currentFilter) { //this means we made a new filter, so we empty the current list and add the new one's contents in it
            page = loadedPage
            maxPages = pages
            showFilter = newFilter
            showList.clear()
            showList.addAll(results)
            updateViewState(
                shows = showList.map { it.copy() }
                , hasMore = page < maxPages
                , filter = showFilter
            )
            vm.showLoading(false)
        } else { //this means we are continuing with our previous search so we append the results
            page = loadedPage
            maxPages = pages
            showList.addAll(results)
            updateViewState(
                shows = showList.map { it.copy() }
                , hasMore = page < maxPages
                , filter = showFilter
            )
            vm.showLoading(false)
        }
    }

    private fun gotDetails(details: Show) {
        show = details
        updateViewState(selected = true, show = show, watchListed = watchList.count { it.id == details.id } > 0)
        vm.showLoading(false)
    }

    private fun updateViewState(shows: List<Show>? = null, selected: Boolean = false, show: Show? = null, hasMore: Boolean? = null, filter: String? = null, inShows: Boolean? = null, hasWatchlist: Boolean? = null, watchListed: Boolean? = null) {
        vm.updateState(ViewState(shows?.map { it.copy() }, selected, show?.copy(), hasMore, filter, inShows, hasWatchlist, watchListed))
    }

    override fun clear() {
        disposables.dispose()
    }
}