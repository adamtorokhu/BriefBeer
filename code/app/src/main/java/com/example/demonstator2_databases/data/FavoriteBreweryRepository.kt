package com.example.demonstator2_databases.data

import com.example.demonstator2_databases.db.FavoriteBreweryDao
import com.example.demonstator2_databases.db.FavoriteBreweryEntity
import kotlinx.coroutines.flow.Flow

class FavoriteBreweryRepository(private val dao: FavoriteBreweryDao) {

    val favorites: Flow<List<FavoriteBreweryEntity>> = dao.getAll()

    suspend fun addFavorite(entity: FavoriteBreweryEntity) {
        dao.insert(entity)
    }

    suspend fun removeFavorite(entity: FavoriteBreweryEntity) {
        dao.delete(entity)
    }

    suspend fun isFavorite(id: String): Boolean {
        return dao.getById(id) != null
    }
}
