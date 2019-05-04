package gr.blackswamp.myshows.util

import io.reactivex.Scheduler

interface ISchedulers {
    val subscribeScheduler: Scheduler
    val observeScheduler: Scheduler
}
