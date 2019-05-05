package gr.blackswamp.myshows.logic

import androidx.annotation.StringRes
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

class MainLogic(private val vm: IMainViewModel, private val service: MovieDBService, private val db: AppDatabase, private val schedulers: ISchedulers) : IMainLogic {
    companion object {
        @Suppress("unused")
        const val TAG = "MainLogic"
    }

    private val disposables = CompositeDisposable()
    internal val showList = mutableListOf<Show>()
    internal val watchList = mutableListOf<Show>()
    internal var page = 0
    internal var maxPages = 0
    internal var showFilter: String = ""
    internal var watchFilter: String = ""
    internal var inShows: Boolean = true
    internal var show: Show? = null

    init {
        loadInitialData()
    }

    override fun searchShows(newFilter: String, submit: Boolean) {
        //if the newFilter is less than 1 character we return an error because it is not allowed by the api
        if (inShows && submit) {
            if (newFilter.isEmpty()) {
                gotError(R.string.error_invalid_filter, null)
                return
            }

            vm.showLoading(true)
            doSearchShows("", newFilter, 1)
        } else if (!inShows) {
            watchFilter = newFilter
            updateViewState(filter = newFilter)
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
                    { result ->
                        watchList.clear()
                        watchList.addAll(result.map { Show(it) })
                        val inWatchlist = show?.let { s -> watchList.count { s.id == it.id } != 0 }
                        if (inShows) {
                            updateViewState(watchListed = inWatchlist)
                        } else if (watchList.isEmpty()) {
                            inShows = true
                            updateViewState(shows = showList, inShows = inShows, filter = showFilter, hasMore = page < maxPages, hasWatchlist = watchList.size > 0, watchListed = inWatchlist)
                        } else {
                            updateViewState(shows = watchList, hasMore = false, hasWatchlist = watchList.size > 0, watchListed = inWatchlist)
                        }
                        vm.showLoading(false)
                    }
                    , { throwable ->
                        val inWatchlist = show?.let { s -> watchList.count { s.id == it.id } != 0 }
                        if (inShows)
                            updateViewState(hasWatchlist = watchList.size > 0, watchListed = inWatchlist)
                        else
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
                    { result ->
                        watchList.clear()
                        watchList.addAll(result.map { Show(it) })
                        val inWatchlist = show?.let { s -> watchList.count { s.id == it.id } != 0 }
                        if (inShows) {
                            updateViewState(hasWatchlist = watchList.size > 0, watchListed = inWatchlist)
                        } else {
                            updateViewState(hasWatchlist = watchList.size > 0, shows = watchList, watchListed = inWatchlist)
                        }
                        vm.showLoading(false)
                    }, { throwable ->
                        val inWatchlist = show?.let { s -> watchList.count { s.id == it.id } != 0 }
                        if (inShows) {
                            updateViewState(hasWatchlist = watchList.size > 0, watchListed = inWatchlist)
                        } else {
                            updateViewState(hasWatchlist = watchList.size > 0, shows = watchList, watchListed = inWatchlist)
                        }
                        gotError(R.string.error_add_watchlist, throwable)
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
        if (results.isEmpty() && pages == 1) {
            vm.showError(R.string.error_no_results)
            vm.showLoading(false)
            return
        } else if (results.isEmpty() && loadedPage >= pages) {
            vm.showError(R.string.error_no_more_shows)
            vm.showLoading(false)
            return
        } else if (results.isEmpty()) {
            page = loadedPage
            maxPages = pages
            doSearchShows(currentFilter, newFilter, page + 1)
        } else if (newFilter != currentFilter) { //this means we made a new searchFilter
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
        } else { //this means we are continuing with our previous searchFilter
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