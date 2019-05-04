package gr.blackswamp.myshows

import android.app.Application
import androidx.room.Room
import gr.blackswamp.myshows.data.db.AppDatabase
import gr.blackswamp.myshows.data.db.LocalDatabase

class App : Application() {
    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(applicationContext, LocalDatabase::class.java, "myshows.db").build()
    }
}