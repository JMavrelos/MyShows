package gr.blackswamp.myshows

import gr.blackswamp.myshows.util.ISchedulers
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers


object TestSchedulers : ISchedulers {
    override val subscribeScheduler: Scheduler = Schedulers.trampoline()
    override val observeScheduler: Scheduler = Schedulers.trampoline()
}
