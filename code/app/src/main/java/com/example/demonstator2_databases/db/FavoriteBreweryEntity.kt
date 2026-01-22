package com.example.demonstator2_databases.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_breweries")
data class FavoriteBreweryEntity(
    @PrimaryKey
    val breweryId: String,
    val name: String,
    val breweryType: String,
    val city: String,
    val state: String,
    val country: String
)