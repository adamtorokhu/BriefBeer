package com.example.demonstator2_databases.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteBreweryEntity::class, BreweryEntity::class],
    version = 2
)
abstract class BriefBeerDatabase : RoomDatabase() {
    abstract fun favoriteBreweryDao(): FavoriteBreweryDao
    abstract fun breweryDao(): BreweryDao

    companion object {
        @Volatile
        private var INSTANCE: BriefBeerDatabase? = null

        fun getDatabase(context: Context): BriefBeerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BriefBeerDatabase::class.java,
                    "brief_beer_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
