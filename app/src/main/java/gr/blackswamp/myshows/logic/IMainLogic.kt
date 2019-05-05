package gr.blackswamp.myshows.logic

interface IMainLogic {
    fun searchShows(newFilter: String)
    fun clear()
    fun showSelected(id: Int, fromShowList: Boolean)
    fun refreshData()
    fun displayShowList()
    fun displayWatchList()
    fun loadNextShows()
    fun searchWatchlist(newFilter: String)
    fun deleteItem(showId: Int)
    fun exitDisplay()
}
