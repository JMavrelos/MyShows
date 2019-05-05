package gr.blackswamp.myshows.data.db

import androidx.room.*

@Dao
abstract class  ShowDao {

    @Query("SELECT * FROM movies ")
    abstract fun loadMovieList(): List<MovieDO>

    @Query("SELECT * FROM tv ")
    abstract fun loadTvList(): List<TvShowDO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun newMovie(show: MovieDO)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun newTv(show: TvShowDO)

    @Query("DELETE FROM movies WHERE id = :id")
    abstract fun deleteMovieById(id: Int): Int

    @Query("DELETE FROM tv WHERE id = :id")
    abstract fun deleteTvShowById(id: Int): Int

    @Transaction
    open fun deleteAndGetResult(id:Int,isMovie : Boolean) : List<IShowDO> {
        if (isMovie)
            deleteMovieById(id)
        else
            deleteTvShowById(id)
        return listOf(
            *loadTvList().toTypedArray(),*loadMovieList().toTypedArray()).sortedBy { it.title }
    }

    @Transaction
    open fun addAndGetResult(show:IShowDO) : List<IShowDO> {
        if (show.isMovie)
            newMovie(show as MovieDO)
        else
            newTv(show as TvShowDO)

        return listOf(
            *loadTvList().toTypedArray(),*loadMovieList().toTypedArray()).sortedBy { it.title }
    }
}