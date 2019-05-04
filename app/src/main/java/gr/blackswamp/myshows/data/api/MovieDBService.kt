package gr.blackswamp.myshows.data.api

import gr.blackswamp.myshows.BuildConfig
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieDBService {
    @GET("search/multi/?api_key=" + BuildConfig.ApiKey)
    fun getShows(@Query("query") query: String, @Query("page") page: Int): Observable<ShowListAO>

    @GET("movie/{id}?api_key=" + BuildConfig.ApiKey + "&append_to_response=videos")
    fun getMovieDetails(@Path("id") id: Int): Observable<ShowDetailAO>

    @GET("tv/{id}?api_key=" + BuildConfig.ApiKey + "&append_to_response=videos")
    fun getTvDetails(@Path("id") id: Int): Observable<ShowDetailAO>
}