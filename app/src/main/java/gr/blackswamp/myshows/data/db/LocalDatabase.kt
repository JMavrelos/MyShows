package gr.blackswamp.myshows.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TvShowDO::class, MovieDO::class], version = 1)
abstract class LocalDatabase : RoomDatabase(), AppDatabase {
    protected abstract val showDao: ShowDao

    override fun loadWatchlist(): List<IShowDO> {
        val tvList = showDao.loadTvList()
        val movieList = showDao.loadMovieList()
        return listOf(*tvList.toTypedArray(), *movieList.toTypedArray()).sortedBy { it.title }
    }

    override fun deleteWatchlistItem(id: Int,isMovie: Boolean) = showDao.deleteAndGetResult(id,isMovie)

    override fun addWatchlistItem(show: IShowDO) = showDao.addAndGetResult(show)
}