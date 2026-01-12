package com.example.demonstator2_databases.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteChampionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteChampionEntity)

    @Query("SELECT * FROM favorite_champions")
    fun getAll(): Flow<List<FavoriteChampionEntity>>

    @Delete
    suspend fun delete(entity: FavoriteChampionEntity)

    @Query("SELECT * FROM favorite_champions WHERE championId = :championId LIMIT 1")
    suspend fun getById(championId: String): FavoriteChampionEntity?
}
