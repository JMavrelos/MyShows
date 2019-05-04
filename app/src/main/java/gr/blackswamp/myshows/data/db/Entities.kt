package gr.blackswamp.myshows.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "shows")
data class ShowDO(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int
    , @ColumnInfo(name = "title") val title: String
    , @ColumnInfo(name = "poster") val poster: String?
    , @ColumnInfo(name = "summary") val summary: String
    , @ColumnInfo(name = "genre") val genre: String
    , @ColumnInfo(name = "isMovie") val isMovie: Boolean
    , @ColumnInfo(name = "rating") val rating: String
    , @ColumnInfo(name = "release") val release: String
    , @ColumnInfo(name = "trailer") val trailer: String?
    , @ColumnInfo(name = "trailerName") val trailerName: String?
) 