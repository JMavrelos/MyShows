package gr.blackswamp.myshows.data.api

import gr.blackswamp.myshows.BuildConfig
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieDBService {
    @GET("search/multi/?api_key=" + BuildConfig.ApiKey)
    fun getShows(@Query("query") query: String, @Query("page") page: Int): Single<ShowListAO>

    @GET("movie/{id}?api_key=" + BuildConfig.ApiKey + "&append_to_response=videos")
    fun getMovieDetails(@Path("id") id: Int): Single<ShowDetailAO>

    @GET("tv/{id}?api_key=" + BuildConfig.ApiKey + "&append_to_response=videos")
    fun getTvDetails(@Path("id") id: Int): Single<ShowDetailAO>
}