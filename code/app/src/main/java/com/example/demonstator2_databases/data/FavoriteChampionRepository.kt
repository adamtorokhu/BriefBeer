package com.example.demonstator2_databases.data

import com.example.demonstator2_databases.db.FavoriteChampionDao
import com.example.demonstator2_databases.db.FavoriteChampionEntity
import kotlinx.coroutines.flow.Flow

class FavoriteChampionRepository(private val dao: FavoriteChampionDao) {

    val favorites: Flow<List<FavoriteChampionEntity>> = dao.getAll()

    suspend fun addFavorite(entity: FavoriteChampionEntity) {
        dao.insert(entity)
    }

    suspend fun removeFavorite(entity: FavoriteChampionEntity) {
        dao.delete(entity)
    }

    suspend fun isFavorite(id: String): Boolean {
        return dao.getById(id) != null
    }
}