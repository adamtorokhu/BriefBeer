package com.example.demonstator2_databases

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demonstator2_databases.data.FavoriteBreweryRepository
import com.example.demonstator2_databases.db.FavoriteBreweryEntity
import com.example.demonstator2_databases.db.BriefBeerDatabase
import com.example.demonstator2_databases.db.BreweryEntity
import com.example.demonstator2_databases.db.BreweryDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

private const val OPEN_BREWERY_DB_BASE_URL = "https://api.openbrewerydb.org/v1/"

data class BreweryListItem(
    val id: String,
    val name: String,
    val breweryType: String,
    val city: String,
    val state: String,
    val country: String
)

data class BreweryDetail(
    val id: String,
    val name: String,
    val breweryType: String,
    val street: String?,
    val address1: String?,
    val address2: String?,
    val address3: String?,
    val city: String?,
    val state: String?,
    val countyProvince: String?,
    val stateProvince: String?,
    val postalCode: String?,
    val country: String?,
    val longitude: String?,
    val latitude: String?,
    val phone: String?,
    val websiteUrl: String?,
    val updatedAt: String?,
    val createdAt: String?
)

data class BreweryDto(
    val id: String,
    val name: String?,
    val brewery_type: String?,
    val street: String?,
    val address_1: String?,
    val address_2: String?,
    val address_3: String?,
    val city: String?,
    val state: String?,
    val county_province: String?,
    val postal_code: String?,
    val country: String?,
    val longitude: String?,
    val latitude: String?,
    val phone: String?,
    val website_url: String?,
    val state_province: String?,
    val updated_at: String?,
    val created_at: String?
)

interface OpenBreweryApiService {
    @GET("breweries")
    suspend fun getBreweries(
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1
    ): List<BreweryDto>
    
    @GET("breweries/{breweryId}")
    suspend fun getBreweryDetail(@Path("breweryId") breweryId: String): BreweryDto
    
    @GET("breweries/search")
    suspend fun searchBreweries(@Query("query") query: String): List<BreweryDto>
}

data class BriefBeerUiState(
    val breweries: List<BreweryListItem> = emptyList(),
    val filteredBreweries: List<BreweryListItem> = emptyList(),
    val favorites: List<BreweryListItem> = emptyList(),
    val selectedBrewery: BreweryDetail? = null,
    val searchQuery: String = "",
    val selectedTypeFilter: String? = null,
    val isLoading: Boolean = false
)

class BriefBeerViewModel(application: Application) : AndroidViewModel(application) {

    private val api: OpenBreweryApiService = Retrofit.Builder()
        .baseUrl(OPEN_BREWERY_DB_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenBreweryApiService::class.java)

    private val favoritesRepository: FavoriteBreweryRepository
    private val breweryDao: BreweryDao

    private val _uiState = MutableStateFlow(BriefBeerUiState())
    val uiState: StateFlow<BriefBeerUiState> = _uiState.asStateFlow()

    init {
        val db = BriefBeerDatabase.getDatabase(application)
        favoritesRepository = FavoriteBreweryRepository(db.favoriteBreweryDao())
        breweryDao = db.breweryDao()

        viewModelScope.launch {
            favoritesRepository.favorites
                .map { entities ->
                    entities.map {
                        BreweryListItem(
                            id = it.breweryId,
                            name = it.name,
                            breweryType = it.breweryType,
                            city = it.city,
                            state = it.state,
                            country = it.country
                        )
                    }
                }
                .collect { favoritesList ->
                    _uiState.value = _uiState.value.copy(favorites = favoritesList)
                }
        }

        loadBreweries()
    }

    fun loadBreweries() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Try to load from database first (offline support)
                val cachedBreweries = breweryDao.getAll()
                if (cachedBreweries.isNotEmpty()) {
                    val breweryList = cachedBreweries.map { entity ->
                        BreweryListItem(
                            id = entity.id,
                            name = entity.name,
                            breweryType = entity.breweryType ?: "",
                            city = entity.city ?: "",
                            state = entity.state ?: "",
                            country = entity.country ?: ""
                        )
                    }.sortedBy { it.name }
                    _uiState.value = _uiState.value.copy(breweries = breweryList, isLoading = false)
                    applyFilters()
                }
                
                // Fetch all breweries from API by paginating through pages
                val allBreweries = mutableListOf<BreweryDto>()
                var currentPage = 1
                val perPage = 50
                var hasMore = true
                
                while (hasMore) {
                    try {
                        val response = api.getBreweries(perPage = perPage, page = currentPage)
                        if (response.isEmpty()) {
                            hasMore = false
                        } else {
                            allBreweries.addAll(response)
                            // If we got less than perPage items, we've reached the end
                            if (response.size < perPage) {
                                hasMore = false
                            } else {
                                currentPage++
                            }
                        }
                    } catch (e: Exception) {
                        // If a page fails, stop pagination
                        hasMore = false
                    }
                }
                
                val breweries = allBreweries.map { dto ->
                    BreweryListItem(
                        id = dto.id,
                        name = dto.name ?: "Unknown",
                        breweryType = dto.brewery_type ?: "",
                        city = dto.city ?: "",
                        state = dto.state ?: "",
                        country = dto.country ?: ""
                    )
                }.sortedBy { it.name }

                // Store in database for offline access
                breweryDao.insertAll(allBreweries.map { dto ->
                    BreweryEntity(
                        id = dto.id,
                        name = dto.name ?: "Unknown",
                        breweryType = dto.brewery_type,
                        street = dto.street,
                        address1 = dto.address_1,
                        address2 = dto.address_2,
                        address3 = dto.address_3,
                        city = dto.city,
                        state = dto.state,
                        countyProvince = dto.county_province,
                        stateProvince = dto.state_province,
                        postalCode = dto.postal_code,
                        country = dto.country,
                        longitude = dto.longitude,
                        latitude = dto.latitude,
                        phone = dto.phone,
                        websiteUrl = dto.website_url,
                        updatedAt = dto.updated_at,
                        createdAt = dto.created_at
                    )
                })

                _uiState.value = _uiState.value.copy(breweries = breweries, isLoading = false)
                applyFilters()
            } catch (e: Exception) {
                // If API fails, try to load from database
                val cachedBreweries = breweryDao.getAll()
                if (cachedBreweries.isNotEmpty()) {
                    val breweryList = cachedBreweries.map { entity ->
                        BreweryListItem(
                            id = entity.id,
                            name = entity.name,
                            breweryType = entity.breweryType ?: "",
                            city = entity.city ?: "",
                            state = entity.state ?: "",
                            country = entity.country ?: ""
                        )
                    }.sortedBy { it.name }
                    _uiState.value = _uiState.value.copy(breweries = breweryList, isLoading = false)
                    applyFilters()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onTypeFilterChange(type: String?) {
        _uiState.value = _uiState.value.copy(selectedTypeFilter = type)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()
        val type = state.selectedTypeFilter

        val filtered = state.breweries.filter { brewery ->
            val matchesQuery = query.isEmpty() ||
                brewery.name.lowercase().contains(query) ||
                brewery.country.lowercase().contains(query) ||
                brewery.city.lowercase().contains(query)
            val matchesType = type == null || brewery.breweryType.equals(type, ignoreCase = true)
            matchesQuery && matchesType
        }

        _uiState.value = _uiState.value.copy(filteredBreweries = filtered)
    }

    fun selectBrewery(breweryId: String) {
        if (breweryId.isEmpty()) return
        
        val brewery = _uiState.value.breweries.find { it.id == breweryId } 
            ?: _uiState.value.favorites.find { it.id == breweryId }
            ?: return
        
        viewModelScope.launch {
            try {
                // Try to load from database first
                val cachedBrewery = breweryDao.getById(breweryId)
                if (cachedBrewery != null) {
                    val detail = BreweryDetail(
                        id = cachedBrewery.id,
                        name = cachedBrewery.name,
                        breweryType = cachedBrewery.breweryType ?: "",
                        street = cachedBrewery.street,
                        address1 = cachedBrewery.address1,
                        address2 = cachedBrewery.address2,
                        address3 = cachedBrewery.address3,
                        city = cachedBrewery.city,
                        state = cachedBrewery.state,
                        countyProvince = cachedBrewery.countyProvince,
                        stateProvince = cachedBrewery.stateProvince,
                        postalCode = cachedBrewery.postalCode,
                        country = cachedBrewery.country,
                        longitude = cachedBrewery.longitude,
                        latitude = cachedBrewery.latitude,
                        phone = cachedBrewery.phone,
                        websiteUrl = cachedBrewery.websiteUrl,
                        updatedAt = cachedBrewery.updatedAt,
                        createdAt = cachedBrewery.createdAt
                    )
                    _uiState.value = _uiState.value.copy(selectedBrewery = detail)
                    return@launch
                }
                
                // If not in database, fetch from API
                val response = api.getBreweryDetail(brewery.id)
                val detail = BreweryDetail(
                    id = response.id,
                    name = response.name ?: "Unknown",
                    breweryType = response.brewery_type ?: "",
                    street = response.street,
                    address1 = response.address_1,
                    address2 = response.address_2,
                    address3 = response.address_3,
                    city = response.city,
                    state = response.state,
                    countyProvince = response.county_province,
                    stateProvince = response.state_province,
                    postalCode = response.postal_code,
                    country = response.country,
                    longitude = response.longitude,
                    latitude = response.latitude,
                    phone = response.phone,
                    websiteUrl = response.website_url,
                    updatedAt = response.updated_at,
                    createdAt = response.created_at
                )
                
                // Store in database for offline access
                breweryDao.insert(BreweryEntity(
                    id = response.id,
                    name = response.name ?: "Unknown",
                    breweryType = response.brewery_type,
                    street = response.street,
                    address1 = response.address_1,
                    address2 = response.address_2,
                    address3 = response.address_3,
                    city = response.city,
                    state = response.state,
                    countyProvince = response.county_province,
                    stateProvince = response.state_province,
                    postalCode = response.postal_code,
                    country = response.country,
                    longitude = response.longitude,
                    latitude = response.latitude,
                    phone = response.phone,
                    websiteUrl = response.website_url,
                    updatedAt = response.updated_at,
                    createdAt = response.created_at
                ))
                
                _uiState.value = _uiState.value.copy(selectedBrewery = detail)
            } catch (e: Exception) {
                // If API fails, try database again
                val cachedBrewery = breweryDao.getById(breweryId)
                if (cachedBrewery != null) {
                    val detail = BreweryDetail(
                        id = cachedBrewery.id,
                        name = cachedBrewery.name,
                        breweryType = cachedBrewery.breweryType ?: "",
                        street = cachedBrewery.street,
                        address1 = cachedBrewery.address1,
                        address2 = cachedBrewery.address2,
                        address3 = cachedBrewery.address3,
                        city = cachedBrewery.city,
                        state = cachedBrewery.state,
                        countyProvince = cachedBrewery.countyProvince,
                        stateProvince = cachedBrewery.stateProvince,
                        postalCode = cachedBrewery.postalCode,
                        country = cachedBrewery.country,
                        longitude = cachedBrewery.longitude,
                        latitude = cachedBrewery.latitude,
                        phone = cachedBrewery.phone,
                        websiteUrl = cachedBrewery.websiteUrl,
                        updatedAt = cachedBrewery.updatedAt,
                        createdAt = cachedBrewery.createdAt
                    )
                    _uiState.value = _uiState.value.copy(selectedBrewery = detail)
                }
            }
        }
    }

    fun clearSelectedBrewery() {
        _uiState.value = _uiState.value.copy(selectedBrewery = null)
    }

    fun toggleFavorite(item: BreweryListItem) {
        viewModelScope.launch {
            val isFav = favoritesRepository.isFavorite(item.id)
            if (isFav) {
                favoritesRepository.removeFavorite(
                    FavoriteBreweryEntity(
                        breweryId = item.id,
                        name = item.name,
                        breweryType = item.breweryType,
                        city = item.city,
                        state = item.state,
                        country = item.country
                    )
                )
            } else {
                favoritesRepository.addFavorite(
                    FavoriteBreweryEntity(
                        breweryId = item.id,
                        name = item.name,
                        breweryType = item.breweryType,
                        city = item.city,
                        state = item.state,
                        country = item.country
                    )
                )
            }
        }
    }
}