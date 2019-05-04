package gr.blackswamp.myshows.data.api

import com.google.gson.annotations.SerializedName
import gr.blackswamp.myshows.ui.model.ShowDetailVO
import gr.blackswamp.myshows.ui.model.ShowVO


data class GenreAO(@SerializedName("id") val id: Int, @SerializedName("listTitle") val name: String)

data class ShowAO(
    @SerializedName("id") override val id: Int
    , @SerializedName("poster_path") val poster: String?
    , @SerializedName("release_date") override val release: String
    , @SerializedName("media_type") val media_type: String
    , @SerializedName("listTitle") override val title: String
    , @SerializedName("vote_average") val votes: Double
) : ShowVO {
    override val isMovie: Boolean = (media_type == "movie")
    override val thumbnail = poster?.let { MovieDBClient.THUMBNAIL_URL + poster }
    override val rating = votes.toString()
}

data class ShowDetailAO(
    @SerializedName("id") override val id: Int
    , @SerializedName("poster_path") val poster: String?
    , @SerializedName("listTitle") override val title: String
    , @SerializedName("overview") override val summary: String
    , @SerializedName("genres") val genres: List<GenreAO>
    , @SerializedName("videos") val videos: VideosAO
) : ShowDetailVO {
    override val image = poster?.let { MovieDBClient.IMAGE_URL + poster }
    override val genre = genres.firstOrNull()?.name ?: "N/A"
    override var isMovie = false
    override val trailerName: String?
        get() = firstVideo?.name

    private val firstVideo: VideoResultsAO?
        get() = videos.results.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }

    override val trailer: String?
        get() = firstVideo?.let { MovieDBClient.YOUTUBE_URL + it.key }
}

data class ShowListAO(
    @SerializedName("id") val id: Int
    , @SerializedName("page") val page: Int
    , @SerializedName("results") val results: List<ShowAO>
    , @SerializedName("total_pages") val pages: Int
)

data class VideosAO(@SerializedName("results") val results: List<VideoResultsAO>)

data class VideoResultsAO(
    @SerializedName("id") val id: String,
    @SerializedName("key") val key: String,
    @SerializedName("name") val name: String,
    @SerializedName("site") val site: String,
    @SerializedName("size") val size: Int,
    @SerializedName("type") val type: String
)
