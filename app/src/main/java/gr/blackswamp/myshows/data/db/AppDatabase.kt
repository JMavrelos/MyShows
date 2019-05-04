package gr.blackswamp.myshows.data.db

interface AppDatabase {
    fun loadWatchlistMatching(filter: String): List<ShowDO>
    fun deleteWatchlistItem(id:Int): Int
}
