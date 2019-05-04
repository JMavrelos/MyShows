package gr.blackswamp.myshows.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import gr.blackswamp.myshows.ui.model.ShowDetailVO
import gr.blackswamp.myshows.ui.model.ShowVO


@Entity(tableName = "shows")
data class ShowDO(
    @PrimaryKey @ColumnInfo(name = "id") override val id: Int
    , @ColumnInfo(name = "title") override val title: String
    , @ColumnInfo(name = "image") override val image: String?
    , @ColumnInfo(name = "summary") override val summary: String
    , @ColumnInfo(name = "genre") override val genre: String
    , @ColumnInfo(name = "isMovie") override val isMovie: Boolean
    , @ColumnInfo(name = "thumbnail") override val thumbnail: String?
    , @ColumnInfo(name = "rating") override val rating: String
    , @ColumnInfo(name = "release") override val release: String
    , @ColumnInfo(name = "trailer") override val trailer: String?
    , @ColumnInfo(name = "trailerName") override val trailerName: String?
) : ShowVO, ShowDetailVO