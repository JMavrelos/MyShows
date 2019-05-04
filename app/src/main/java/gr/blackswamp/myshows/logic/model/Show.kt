package gr.blackswamp.myshows.logic.model

import gr.blackswamp.myshows.data.api.MovieDBClient
import gr.blackswamp.myshows.data.api.ShowAO
import gr.blackswamp.myshows.data.db.ShowDO
import gr.blackswamp.myshows.ui.model.ShowVO

data class Show(override val id: Int
                , override val thumbnail: String?
                , override val title: String
                , override val rating: String
                , override val release: String
                , override val isMovie: Boolean)
    : ShowVO {


    constructor(show: ShowDO) : this(show.id, show.thumbnail, show.title, show.rating, show.release, show.isMovie)

    constructor(show: ShowAO) :
            this(show.id
                , show.poster?.let { MovieDBClient.THUMBNAIL_URL + it }
                , show.title ?: "N/A"
                , show.votes?.toString() ?: "N/A"
                , (if (show.media_type == "movie") show.release else show.air_date) ?: "N/A"
                , show.media_type == "movie")
}