package gr.blackswamp.myshows.util

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object AppSchedulers : ISchedulers {
    override val subscribeScheduler: Scheduler = Schedulers.io()
    override val observeScheduler: Scheduler = AndroidSchedulers.mainThread()
}