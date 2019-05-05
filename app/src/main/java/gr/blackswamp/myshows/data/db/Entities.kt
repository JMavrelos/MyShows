package gr.blackswamp.myshows.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import gr.blackswamp.myshows.logic.model.Show

interface IShowDO {
    val id: Int
    val title: String
    val poster: String?
    val summary: String
    val genre: String
    val rating: String
    val release: String
    val trailer: String?
    val isMovie: Boolean
}

@Entity(tableName = "tv")
data class TvShowDO(
    @PrimaryKey @ColumnInfo(name = "id") override val id: Int
    , @ColumnInfo(name = "title") override val title: String
    , @ColumnInfo(name = "poster") override val poster: String?
    , @ColumnInfo(name = "summary") override val summary: String
    , @ColumnInfo(name = "genre") override val genre: String
    , @ColumnInfo(name = "rating") override val rating: String
    , @ColumnInfo(name = "release") override val release: String
    , @ColumnInfo(name = "trailer") override val trailer: String?
) : IShowDO {
    @Ignore
    override val isMovie = false
    constructor(show: Show) : this(show.id, show.title, show.poster, show.summary, show.genre, show.rating, show.release, show.trailer)
}


@Entity(tableName = "movies")
data class MovieDO(
    @PrimaryKey @ColumnInfo(name = "id") override val id: Int
    , @ColumnInfo(name = "title") override val title: String
    , @ColumnInfo(name = "poster") override val poster: String?
    , @ColumnInfo(name = "summary") override val summary: String
    , @ColumnInfo(name = "genre") override val genre: String
    , @ColumnInfo(name = "rating") override val rating: String
    , @ColumnInfo(name = "release") override val release: String
    , @ColumnInfo(name = "trailer") override val trailer: String?
) : IShowDO {
    @Ignore
    override val isMovie = true

    constructor(show: Show) : this(show.id, show.title, show.poster, show.summary, show.genre, show.rating, show.release, show.trailer)
}