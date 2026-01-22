package com.example.demonstator2_databases.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteBreweryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteBreweryEntity)

    @Query("SELECT * FROM favorite_breweries")
    fun getAll(): Flow<List<FavoriteBreweryEntity>>

    @Delete
    suspend fun delete(entity: FavoriteBreweryEntity)

    @Query("SELECT * FROM favorite_breweries WHERE breweryId = :breweryId LIMIT 1")
    suspend fun getById(breweryId: String): FavoriteBreweryEntity?
}
