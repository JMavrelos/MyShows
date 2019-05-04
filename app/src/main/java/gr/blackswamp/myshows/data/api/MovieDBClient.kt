package gr.blackswamp.myshows.data.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object MovieDBClient {
    private const val URL = "https://api.themoviedb.org/3/"
    internal const val THUMBNAIL_URL = "http://image.tmdb.org/t/p/w92"
    internal const val IMAGE_URL = "http://image.tmdb.org/t/p/original"
    internal const val YOUTUBE_URL = "https://www.youtube.com/watch?v="
    val service: MovieDBService

    init {
        //create a new interceptor
        val loggingInterceptor = HttpLoggingInterceptor { Log.d("Service", it) } //create a logger that prints to debug
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY //we are logging everything
        val okHttpClient = OkHttpClient //build http client
            .Builder()
            .addInterceptor(loggingInterceptor) //attach interceptor
            .readTimeout(5, TimeUnit.SECONDS) //set read timeout
            .connectTimeout(5, TimeUnit.SECONDS) //set connect timeout
            .build()

        val retrofit = Retrofit.Builder().baseUrl(URL) //build a retrofit client from a url
            .client(okHttpClient) //attach the client
            .addConverterFactory(GsonConverterFactory.create()) //add the converter factory to parse incoming data (gson)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) //add the call adapter factory
            .build()
        service = retrofit.create(MovieDBService::class.java)
    }

}