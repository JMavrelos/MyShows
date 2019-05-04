package gr.blackswamp.myshows.data.db

import androidx.room.*

@Dao
interface ShowDao {

    @Query("SELECT * FROM shows WHERE title LIKE '%' || :search || '%'")
    fun loadShowList(search: String): List<ShowDO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun newShow(show: ShowDO)

    @Query("DELETE FROM shows WHERE id = :id")
    fun deleteShowById(id: Int): Int
}