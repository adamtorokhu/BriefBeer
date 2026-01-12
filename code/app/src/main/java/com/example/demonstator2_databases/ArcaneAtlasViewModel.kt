package com.example.demonstator2_databases

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demonstator2_databases.data.FavoriteChampionRepository
import com.example.demonstator2_databases.db.FavoriteChampionEntity
import com.example.demonstator2_databases.db.ArcaneAtlasDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

private const val DATA_DRAGON_BASE_URL =
    "https://ddragon.leagueoflegends.com/cdn/12.6.1/data/en_US/"
private const val DATA_DRAGON_IMAGE_BASE_URL =
    "https://ddragon.leagueoflegends.com/cdn/12.6.1/img/champion/"

data class ChampionListItem(
    val id: String,
    val name: String,
    val title: String,
    val roles: List<String>,
    val imageUrl: String
)

data class ChampionDetail(
    val id: String,
    val name: String,
    val title: String,
    val roles: List<String>,
    val lore: String,
    val imageUrl: String,
    val stats: Map<String, Double>
)

data class ChampionImageDto(
    val full: String
)

data class ChampionStatsDto(
    val hp: Double,
    val hpperlevel: Double,
    val mp: Double,
    val mpperlevel: Double,
    val movespeed: Double,
    val armor: Double,
    val armorperlevel: Double,
    val spellblock: Double,
    val spellblockperlevel: Double,
    val attackrange: Double,
    val hpregen: Double,
    val hpregenperlevel: Double,
    val mpregen: Double,
    val mpregenperlevel: Double,
    val crit: Double,
    val critperlevel: Double,
    val attackdamage: Double,
    val attackdamageperlevel: Double,
    val attackspeedperlevel: Double,
    val attackspeed: Double
)

data class ChampionDto(
    val id: String,
    val name: String,
    val title: String,
    val tags: List<String>,
    val image: ChampionImageDto,
    val stats: ChampionStatsDto
)

data class ChampionDetailDto(
    val id: String,
    val name: String,
    val title: String,
    val lore: String,
    val tags: List<String>,
    val image: ChampionImageDto,
    val stats: ChampionStatsDto
)

data class ChampionResponse(
    val data: Map<String, ChampionDto>
)

data class ChampionDetailResponse(
    val data: Map<String, ChampionDetailDto>
)

interface RiotApiService {
    @GET("champion.json")
    suspend fun getChampions(): ChampionResponse
    
    @GET("champion/{championId}.json")
    suspend fun getChampionDetail(@Path("championId") championId: String): ChampionDetailResponse
}

data class ArcaneAtlasUiState(
    val champions: List<ChampionListItem> = emptyList(),
    val filteredChampions: List<ChampionListItem> = emptyList(),
    val favorites: List<ChampionListItem> = emptyList(),
    val selectedChampion: ChampionDetail? = null,
    val searchQuery: String = "",
    val selectedRoleFilter: String? = null
)

class ArcaneAtlasViewModel(application: Application) : AndroidViewModel(application) {

    private val api: RiotApiService = Retrofit.Builder()
        .baseUrl(DATA_DRAGON_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RiotApiService::class.java)

    private val favoritesRepository: FavoriteChampionRepository

    private val _uiState = MutableStateFlow(ArcaneAtlasUiState())
    val uiState: StateFlow<ArcaneAtlasUiState> = _uiState.asStateFlow()

    init {
        val db = ArcaneAtlasDatabase.getDatabase(application)
        favoritesRepository = FavoriteChampionRepository(db.favoriteChampionDao())

        viewModelScope.launch {
            favoritesRepository.favorites
                .map { entities ->
                    entities.map {
                        ChampionListItem(
                            id = it.championId,
                            name = it.name,
                            title = "",
                            roles = listOf(it.primaryRole),
                            imageUrl = it.imageUrl
                        )
                    }
                }
                .collect { favoritesList ->
                    _uiState.value = _uiState.value.copy(favorites = favoritesList)
                }
        }

        loadChampions()
    }

    fun loadChampions() {
        viewModelScope.launch {
            try {
                val response = api.getChampions()
                val champions = response.data.values.map { dto ->
                    ChampionListItem(
                        id = dto.id,
                        name = dto.name,
                        title = dto.title,
                        roles = dto.tags,
                        imageUrl = DATA_DRAGON_IMAGE_BASE_URL + dto.image.full
                    )
                }.sortedBy { it.name }

                _uiState.value = _uiState.value.copy(champions = champions)
                applyFilters()
            } catch (e: Exception) {
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onRoleFilterChange(role: String?) {
        _uiState.value = _uiState.value.copy(selectedRoleFilter = role)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()
        val role = state.selectedRoleFilter

        val filtered = state.champions.filter { champ ->
            val matchesQuery = query.isEmpty() ||
                champ.name.lowercase().contains(query) ||
                champ.title.lowercase().contains(query)
            val matchesRole = role == null || champ.roles.contains(role)
            matchesQuery && matchesRole
        }

        _uiState.value = _uiState.value.copy(filteredChampions = filtered)
    }

    fun selectChampion(championId: String) {
        if (championId.isEmpty()) return
        
        val champion = _uiState.value.champions.find { it.id == championId } 
            ?: _uiState.value.favorites.find { it.id == championId }
            ?: return
        
        viewModelScope.launch {
            try {
                val response = api.getChampionDetail(champion.id)
                val dto = response.data.values.firstOrNull() ?: return@launch
                val statsMap = mapOf(
                    "HP" to dto.stats.hp,
                    "HP per Level" to dto.stats.hpperlevel,
                    "MP / Resource" to dto.stats.mp,
                    "MP per Level" to dto.stats.mpperlevel,
                    "Move Speed" to dto.stats.movespeed,
                    "Armor" to dto.stats.armor,
                    "Armor per Level" to dto.stats.armorperlevel,
                    "Magic Resist" to dto.stats.spellblock,
                    "Magic Resist per Level" to dto.stats.spellblockperlevel,
                    "Attack Range" to dto.stats.attackrange,
                    "HP Regen" to dto.stats.hpregen,
                    "HP Regen per Level" to dto.stats.hpregenperlevel,
                    "MP Regen" to dto.stats.mpregen,
                    "MP Regen per Level" to dto.stats.mpregenperlevel,
                    "Critical Strike" to dto.stats.crit,
                    "Critical Strike per Level" to dto.stats.critperlevel,
                    "Attack Damage" to dto.stats.attackdamage,
                    "Attack Damage per Level" to dto.stats.attackdamageperlevel,
                    "Attack Speed" to dto.stats.attackspeed,
                    "Attack Speed per Level" to dto.stats.attackspeedperlevel
                )
                val detail = ChampionDetail(
                    id = dto.id,
                    name = dto.name,
                    title = dto.title,
                    roles = dto.tags,
                    lore = dto.lore,
                    imageUrl = DATA_DRAGON_IMAGE_BASE_URL + dto.image.full,
                    stats = statsMap
                )
                _uiState.value = _uiState.value.copy(selectedChampion = detail)
            } catch (e: Exception) {
            }
        }
    }

    fun clearSelectedChampion() {
        _uiState.value = _uiState.value.copy(selectedChampion = null)
    }

    fun toggleFavorite(item: ChampionListItem) {
        viewModelScope.launch {
            val isFav = favoritesRepository.isFavorite(item.id)
            if (isFav) {
                favoritesRepository.removeFavorite(
                    FavoriteChampionEntity(
                        championId = item.id,
                        name = item.name,
                        imageUrl = item.imageUrl,
                        primaryRole = item.roles.firstOrNull().orEmpty()
                    )
                )
            } else {
                favoritesRepository.addFavorite(
                    FavoriteChampionEntity(
                        championId = item.id,
                        name = item.name,
                        imageUrl = item.imageUrl,
                        primaryRole = item.roles.firstOrNull().orEmpty()
                    )
                )
            }
        }
    }
}