package com.example.demonstator2_databases.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BreweryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BreweryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<BreweryEntity>)

    @Query("SELECT * FROM breweries")
    suspend fun getAll(): List<BreweryEntity>

    @Query("SELECT * FROM breweries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BreweryEntity?
}
