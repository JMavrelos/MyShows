package gr.blackswamp.myshows.data.db

import androidx.room.*
import gr.blackswamp.myshows.logic.model.Show

@Dao
abstract class  ShowDao {

    @Query("SELECT * FROM shows ")
    abstract fun loadShowList(): List<ShowDO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun newShow(show: ShowDO)

    @Query("DELETE FROM shows WHERE id = :id")
    abstract fun deleteShowById(id: Int): Int

    @Transaction
    open fun deleteAndGetResult(id:Int) : List<ShowDO> {
        deleteShowById(id)
        return loadShowList()
    }

    @Transaction
    open fun addAndGetResult(show:ShowDO) : List<ShowDO> {
        newShow(show)
        return loadShowList()
    }
}