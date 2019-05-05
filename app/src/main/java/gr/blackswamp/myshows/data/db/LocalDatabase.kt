package gr.blackswamp.myshows.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ShowDO::class], version = 1)
abstract class LocalDatabase : RoomDatabase(), AppDatabase {
    protected abstract val showDao: ShowDao

    override fun loadWatchlist() = showDao.loadShowList()

    override fun deleteWatchlistItem(id: Int)= showDao.deleteAndGetResult(id)

    override fun addWatchlistItem(show: ShowDO) = showDao.addAndGetResult(show)
}