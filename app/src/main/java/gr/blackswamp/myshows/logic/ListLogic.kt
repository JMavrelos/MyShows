package gr.blackswamp.myshows.logic

import androidx.annotation.StringRes
import gr.blackswamp.myshows.R
import gr.blackswamp.myshows.data.api.MovieDBService
import gr.blackswamp.myshows.data.api.ShowAO
import gr.blackswamp.myshows.data.api.ShowDetailAO
import gr.blackswamp.myshows.data.api.ShowListAO
import gr.blackswamp.myshows.data.db.AppDatabase
import gr.blackswamp.myshows.data.db.ShowDO
import gr.blackswamp.myshows.ui.viewmodel.IMainViewModel
import gr.blackswamp.myshows.util.ISchedulers
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

class ListLogic(private val vm: IMainViewModel, private val service: MovieDBService, private val db: AppDatabase, private val schedulers: ISchedulers) : IListLogic {
    companion object {
        const val TAG = "ListLogic"
    }

    private val disposables = CompositeDisposable()
    private val showList = mutableListOf<ShowAO>()
    private val watchList = mutableListOf<ShowDO>()

    var page = 0
        private set
    var maxPages = 0
        private set
    var showFilter: String = ""
        private set
    var watchFilter: String = ""
        private set

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        vm.showLoading(true)
        watchFilter = ""
        disposables.add(
            Observable.fromCallable {
                db.loadWatchlistMatching("")
            }.subscribeOn(schedulers.subscribeScheduler)
                .observeOn(schedulers.observeScheduler)
                .subscribe(
                    {
                        watchList.addAll(it)
                        vm.setHasWatchlist(!watchList.isEmpty())
                        vm.showLoading(false)
                    }
                    , {
                        vm.setHasWatchlist(false)
                        gotError(R.string.error_loading_data, it)
                    }
                )
        )
    }

    override fun searchShows(newFilter: String) {
        //if the newFilter is less than 1 character we return an error because it is not allowed by the api
        if (newFilter.isEmpty()) {
            vm.showError(R.string.error_invalid_filter)
            return
        }
        vm.showLoading(true)
        showFilter = ""
        doSearchShows(newFilter, 1)
    }

    override fun loadNextShows() {
        vm.showLoading(true)
        doSearchShows(showFilter, page + 1)
    }

    override fun refreshData() = searchShows(showFilter)

    override fun displayShowList() {
        vm.showList(true, showList.map { it.copy() }, page < maxPages, showFilter)
    }

    override fun searchWatchlist(newFilter: String) {
        vm.showLoading(true)
        watchFilter = ""
        doSearchWatchlist(newFilter)
    }

    override fun displayWatchList() {
        if (watchList.size == 0) {
            vm.showError(R.string.error_no_watchlist)
        } else {
            vm.showList(false, watchList.map { it.copy() }, false, showFilter)
        }
    }

    override fun showSelected(id: Int, isMovie: Boolean) {
        vm.showLoading(true)
        doLoadShowDetails(id, isMovie)
    }


    override fun deleteItem(showId: Int) {
        vm.showLoading(true)
        disposables.add(
            Observable.fromCallable {
                db.deleteWatchlistItem(showId)
            }.subscribeOn(schedulers.subscribeScheduler)
                .observeOn(schedulers.observeScheduler)
                .subscribe(
                    {
                        if (it != 1) {
                            vm.showError(R.string.error_show_not_found)
                        }
                        doSearchWatchlist(watchFilter)
                    }
                    , {
                        vm.showError(R.string.error_delete_watchlist, it?.message)
                        doSearchWatchlist(watchFilter)
                    }
                )
        )
    }


    private fun doSearchShows(newFilter: String, page: Int) {
        disposables.add(
            service
                .getShows(newFilter, page)
                .subscribeOn(schedulers.subscribeScheduler)
                .observeOn(schedulers.observeScheduler)
                .subscribe(
                    { gotShows(newFilter, page, it) }
                    , { gotError(R.string.error_retrieving_data, it) }
                )
        )
    }

    private fun doLoadShowDetails(id: Int, movie: Boolean) {
        if (movie)
            disposables.add(
                service
                    .getMovieDetails(id)
                    .subscribeOn(schedulers.subscribeScheduler)
                    .observeOn(schedulers.observeScheduler)
                    .subscribe(
                        { gotDetails(it, true) }
                        , { gotError(R.string.error_retrieving_data, it) }
                    )
            )
        else
            disposables.add(
                service
                    .getTvDetails(id)
                    .subscribeOn(schedulers.subscribeScheduler)
                    .observeOn(schedulers.observeScheduler)
                    .subscribe(
                        { gotDetails(it, false) }
                        , { gotError(R.string.error_retrieving_data, it) }
                    )
            )
    }

    private fun doSearchWatchlist(filter: String) {
        disposables.add(
            Observable.fromCallable {
                db.loadWatchlistMatching(filter)
            }.subscribeOn(schedulers.subscribeScheduler)
                .observeOn(schedulers.observeScheduler)
                .subscribe(
                    { gotWatchList(filter, it) }
                    , { gotError(R.string.error_retrieving_data, it) }
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

    private fun gotShows(newFilter: String, loadedPage: Int, list: ShowListAO) {
        val results = list.results.filter { it.media_type != "person" }
        maxPages = list.pages
        if (results.isEmpty() && maxPages == 1) {
            vm.showError(R.string.error_no_results)
            vm.showLoading(false)
            return
        } else if (results.isEmpty() && loadedPage >= maxPages) {
            vm.showError(R.string.error_no_more_shows)
            vm.showLoading(false)
            return
        } else if (results.isEmpty()) {
            page = loadedPage
            doSearchShows(newFilter, page + 1)
        } else if (newFilter != showFilter) { //this means we made a new searchFilter
            page = loadedPage
            showFilter = newFilter
            showList.clear()
            showList.addAll(results)
            vm.setShows(showList.map { it.copy() }, page < maxPages, showFilter)
            vm.showLoading(false)
        } else { //this means we are continuing with our previous searchFilter
            page = loadedPage
            showList.addAll(results)
            vm.setShows(showList.map { it.copy() }, page < maxPages, showFilter)
            vm.showLoading(false)
        }
    }

    private fun gotWatchList(newFilter: String, list: List<ShowDO>) {
        if (list.isEmpty() && !newFilter.isEmpty()) {
            vm.showError(R.string.error_no_results)
        } else if (list.isEmpty()) {
            vm.showList(true, showList.map { it.copy() }, page < maxPages, showFilter)
        } else {
            watchFilter = newFilter
            watchList.clear()
            watchList.addAll(list)
            vm.setShows(watchList.map { it.copy() }, false, watchFilter)
        }
        vm.showLoading(false)
    }

    private fun gotDetails(detail: ShowDetailAO, isMovie: Boolean) {
        detail.isMovie = isMovie
        vm.showDetails(detail)
        vm.showLoading(false)
    }


    override fun clear() {
        disposables.dispose()
    }

}