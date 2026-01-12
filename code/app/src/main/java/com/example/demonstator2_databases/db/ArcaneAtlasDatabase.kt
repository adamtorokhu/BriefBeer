package com.example.demonstator2_databases.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FavoriteChampionEntity::class], version = 2)
abstract class ArcaneAtlasDatabase : RoomDatabase() {
    abstract fun favoriteChampionDao(): FavoriteChampionDao

    companion object {
        @Volatile
        private var INSTANCE: ArcaneAtlasDatabase? = null

        fun getDatabase(context: Context): ArcaneAtlasDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ArcaneAtlasDatabase::class.java,
                    "arcane_atlas_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}