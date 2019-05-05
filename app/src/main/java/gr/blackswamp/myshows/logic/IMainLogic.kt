package gr.blackswamp.myshows.logic

import gr.blackswamp.myshows.ui.model.ShowVO

interface IMainLogic {
    fun searchShows(newFilter: String, submit:Boolean)
    fun clear()
    fun showSelected(id: Int, fromShowList: Boolean)
    fun refreshData()
    fun displayShowList()
    fun displayWatchList()
    fun loadNextShows()
    fun deleteItem(show: ShowVO)
    fun toggleItem()
    fun exitDisplay()
}
