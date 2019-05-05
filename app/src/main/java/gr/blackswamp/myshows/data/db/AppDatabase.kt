package gr.blackswamp.myshows.data.db

interface AppDatabase {
    fun loadWatchlist(): List<IShowDO>
    fun deleteWatchlistItem(id: Int, isMovie: Boolean): List<IShowDO>
    fun addWatchlistItem(show: IShowDO): List<IShowDO>
}
