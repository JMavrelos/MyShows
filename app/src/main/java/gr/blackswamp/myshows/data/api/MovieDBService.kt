package gr.blackswamp.myshows.data.api

import gr.blackswamp.myshows.BuildConfig
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path

interface MovieDBService {
    @GET("searchFilter/multi/?api_key=" + BuildConfig.ApiKey + "&query={query}&page={page}")
    fun getShows(@Path("query") query: String, @Path("page") page: Int): Observable<ShowListAO>

    @GET("movie/{id}?api_key=" + BuildConfig.ApiKey + "&append_to_response=videos")
    fun getMovieDetails(@Path("id") id: Int): Observable<ShowDetailAO>

    @GET("tv/{id}?api_key=" + BuildConfig.ApiKey + "&append_to_response=videos")
    fun getTvDetails(@Path("id") id: Int): Observable<ShowDetailAO>
}