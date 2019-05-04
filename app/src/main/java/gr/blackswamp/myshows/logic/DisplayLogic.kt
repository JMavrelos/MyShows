package gr.blackswamp.myshows.logic

import gr.blackswamp.myshows.data.db.AppDatabase
import gr.blackswamp.myshows.ui.viewmodel.IMainViewModel
import gr.blackswamp.myshows.util.ISchedulers

class DisplayLogic(private val vm: IMainViewModel, private val database: AppDatabase, private val schedulers: ISchedulers) : IDisplayLogic {

    override fun exit() {
        vm.showDetails(null)
    }
}