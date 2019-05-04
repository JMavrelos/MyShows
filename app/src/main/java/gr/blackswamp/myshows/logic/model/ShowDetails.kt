package gr.blackswamp.myshows.logic.model

import gr.blackswamp.myshows.data.api.MovieDBClient
import gr.blackswamp.myshows.data.api.ShowDetailAO
import gr.blackswamp.myshows.ui.model.ShowDetailVO

data class ShowDetails(
    override val id: Int
    , override val title: String
    , override val image: String?
    , override val summary: String
    , override val genre: String
    , override var isMovie: Boolean
    , override val trailer: String?
    , override val trailerName: String?) : ShowDetailVO {
    constructor(details: ShowDetailAO, isMovie: Boolean) : this(
        details.id
        , (if (isMovie) details.title else details.name) ?: "N/A"
        , details.poster?.let { MovieDBClient.IMAGE_URL + it }
        , details.summary ?: "N/A"
        , details.genres?.firstOrNull()?.name ?: "N/A"
        , isMovie
        , details.videos?.results?.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }?.let { MovieDBClient.YOUTUBE_URL + it.key }
        , details.videos?.results?.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }?.name)
}
