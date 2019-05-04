package gr.blackswamp.myshows.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ShowDO::class], version = 1)
abstract class LocalDatabase : RoomDatabase(), AppDatabase {
    protected abstract val showDao: ShowDao

    override fun loadWatchlistMatching(filter: String): List<ShowDO> = showDao.loadShowList(filter)

    override fun deleteWatchlistItem(id: Int): Int = showDao.deleteShowById(id)
}