package gr.blackswamp.myshows.data.api

import com.google.gson.annotations.SerializedName


data class GenreAO(@SerializedName("id") val id: Int, @SerializedName("listTitle") val name: String)

data class ShowAO(
    @SerializedName("id") val id: Int
    , @SerializedName("poster_path") val poster: String?
    , @SerializedName("release_date") val release: String?
    , @SerializedName("first_air_date") val air_date: String?
    , @SerializedName("media_type") val media_type: String
    , @SerializedName("title") val title: String?
    , @SerializedName("name") val name: String?
    , @SerializedName("vote_average") val votes: Double?
)

data class ShowDetailAO(
    @SerializedName("id") val id: Int
    , @SerializedName("poster_path") val poster: String?
    , @SerializedName("title") val title: String?
    , @SerializedName("overview") val summary: String?
    , @SerializedName("genres") val genres: List<GenreAO>?
    , @SerializedName("videos") val videos: VideosAO?
)

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
