package com.example.demonstator2_databases.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_champions")
data class FavoriteChampionEntity(
    @PrimaryKey
    val championId: String,
    val name: String,
    val imageUrl: String,
    val primaryRole: String
)
