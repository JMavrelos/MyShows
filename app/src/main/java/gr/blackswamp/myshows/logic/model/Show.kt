package gr.blackswamp.myshows.logic.model

import gr.blackswamp.myshows.data.api.MovieDBClient
import gr.blackswamp.myshows.data.api.ShowAO
import gr.blackswamp.myshows.data.api.ShowDetailAO
import gr.blackswamp.myshows.data.db.ShowDO
import gr.blackswamp.myshows.ui.model.ShowDetailVO
import gr.blackswamp.myshows.ui.model.ShowVO

data class Show(override val id: Int
                , val poster: String?
                , override val title: String
                , override val rating: String
                , override val release: String
                , override val isMovie: Boolean

                , override val summary: String
                , override val genre: String
                , override val trailer: String?)
    : ShowVO, ShowDetailVO {

    override val thumbnail: String?
        get() = poster?.let { MovieDBClient.THUMBNAIL_URL + it }
    override val image: String?
        get() = poster?.let { MovieDBClient.IMAGE_URL + it }

    constructor(show: ShowDO) : this(show.id, show.poster, show.title, show.rating, show.release, show.isMovie, show.summary, show.summary, show.trailer)

    constructor(show: ShowAO) :
            this(show.id
                , show.poster
                , (if (show.isMovie) show.title else show.name) ?: "N/A"
                , show.votes?.toString() ?: "N/A"
                , (if (show.isMovie) show.release else show.air_date) ?: "N/A"
                , show.isMovie
                , "", "", null)

    constructor(show: Show, details: ShowDetailAO) :
            this(show.id
                , show.poster
                , show.title
                , show.rating
                , show.release
                , show.isMovie
                , details.summary ?: "N/A", details.genres?.firstOrNull()?.name ?: "N/A", details.videos?.results?.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }?.let { /*MovieDBClient.YOUTUBE_URL + */it.key })
}

val ShowAO.isMovie get() = (this.media_type == "movie")
