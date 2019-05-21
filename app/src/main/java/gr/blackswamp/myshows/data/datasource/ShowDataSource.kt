package gr.blackswamp.myshows.data.datasource

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import gr.blackswamp.myshows.data.api.MovieDBService
import gr.blackswamp.myshows.data.api.ShowAO
import gr.blackswamp.myshows.util.ISchedulers
import io.reactivex.disposables.CompositeDisposable


class ShowDataSource(private val service: MovieDBService, private val disposable: CompositeDisposable, private val query: String, private val schedulers: ISchedulers) : PageKeyedDataSource<Int, ShowAO>() {
    companion object {
        const val TAG = "ShowDataSource"
    }

    val state = MutableLiveData<State>()

    override fun loadInitial(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Int, ShowAO>) {
        Log.d(TAG, "load initial ${params.placeholdersEnabled} ${params.requestedLoadSize}")
        disposable.add(
            service.getShows(query, 1)
                .doOnSubscribe {
                    state.postValue(State.LOADING)
                }.subscribeOn(schedulers.subscribeScheduler)
                .observeOn(schedulers.observeScheduler)
                .subscribe({
                    state.postValue(State.SUCCESS)
                    callback.onResult(it.results, 1, it.pages, null, 2)
                }, {
                    state.postValue(State.error(it.message))
                })
        )
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, ShowAO>) {
        Log.d(TAG, "load before ${params.key} ${params.requestedLoadSize}")
        loadPage(params.key, callback, params.key - 1)
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, ShowAO>) {
        Log.d(TAG, "load after ${params.key} ${params.requestedLoadSize}")
        loadPage(params.key, callback, params.key + 1)
    }

    private fun loadPage(page: Int, callback: LoadCallback<Int, ShowAO>, nextPage: Int) {
        disposable.add(
            service.getShows(query, page)
                .doOnSubscribe {
                    state.postValue(State.LOADING)
                }.subscribeOn(schedulers.subscribeScheduler)
                .observeOn(schedulers.observeScheduler)
                .subscribe({
                    state.postValue(State.SUCCESS)
                    callback.onResult(it.results, nextPage)
                }, {
                    state.postValue(State.error(it.message))
                })
        )
    }
}