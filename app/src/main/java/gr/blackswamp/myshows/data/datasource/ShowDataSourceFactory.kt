package gr.blackswamp.myshows.data.datasource

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import gr.blackswamp.myshows.data.api.MovieDBService
import gr.blackswamp.myshows.data.api.ShowAO
import gr.blackswamp.myshows.util.ISchedulers
import io.reactivex.disposables.CompositeDisposable

class ShowDataSourceFactory(private val service: MovieDBService, private val disposable: CompositeDisposable, private val query: String, private val schedulers: ISchedulers) : DataSource.Factory<Int,ShowAO>() {

    val source = MutableLiveData<ShowDataSource>()

    override fun create(): DataSource<Int, ShowAO> {
        val ds = ShowDataSource(service,disposable,query,schedulers)
        source.postValue(ds)
        return ds
    }

}