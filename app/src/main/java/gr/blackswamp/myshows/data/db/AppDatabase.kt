package gr.blackswamp.myshows.data.db

interface AppDatabase {
    fun loadWatchlist(): List<ShowDO>
    fun deleteWatchlistItem(id: Int): List<ShowDO>
    fun addWatchlistItem(show: ShowDO): List<ShowDO>
}
