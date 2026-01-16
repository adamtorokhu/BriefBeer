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
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStreamReader

private const val OPEN_BREWERY_DB_BASE_URL = "https://api.openbrewerydb.org/v1/"
private const val OPEN_FOOD_FACTS_BASE_URL = "https://world.openfoodfacts.org/"

// Data classes for Austria beers JSON
data class AustriaBeerJson(
    val regions: Map<String, RegionData>,
    val lists: Map<String, List<String>>?
)

data class RegionData(
    val breweries: List<BreweryData>
)

data class BreweryData(
    val name: String,
    val location: String,
    val type: String?,
    val notes: String?,
    val beers: List<BeerData>?,
    val qr: String?
)

data class BeerData(
    val name: String,
    val style: String?,
    val list: String?
)

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

// Open Food Facts API data classes
data class OpenFoodFactsResponse(
    val status: Int?,
    val product: OpenFoodFactsProduct?
)

data class OpenFoodFactsProduct(
    val brands: String?,
    val product_name: String?,
    // Examples: ["en:beers", "en:alcoholic-beverages", ...]
    @SerializedName("categories_tags")
    val categoriesTags: List<String>?,
    // Freeform categories string (often comma-separated)
    val categories: String?
)

interface OpenFoodFactsApiService {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): OpenFoodFactsResponse
}

data class BriefBeerUiState(
    val breweries: List<BreweryListItem> = emptyList(),
    val filteredBreweries: List<BreweryListItem> = emptyList(),
    val favorites: List<BreweryListItem> = emptyList(),
    val selectedBrewery: BreweryDetail? = null,
    val searchQuery: String = "",
    val selectedTypeFilter: String? = null,
    val isLoading: Boolean = false,
    val showAddBreweryDialog: Boolean = false,
    val showEditBreweryDialog: Boolean = false,
    val breweryToEdit: BreweryDetail? = null,
    val showDeleteDialog: Boolean = false,
    // Transient UI message (snackbar)
    val message: String? = null,
    val messageActionLabel: String? = null,
    val messageAction: UiMessageAction? = null,
    // When we want to open Add Brewery from a scan, we store the suggested values here.
    val addBreweryPrefill: AddBreweryPrefill? = null
)

enum class UiMessageAction {
    ADD_BREWERY_FROM_SCAN
}

data class AddBreweryPrefill(
    val name: String? = null,
    val breweryType: String? = null,
    val city: String? = null,
    val country: String? = null,
    val state: String? = null,
    val qr: String? = null
)

class BriefBeerViewModel(application: Application) : AndroidViewModel(application) {

    private val api: OpenBreweryApiService = Retrofit.Builder()
        .baseUrl(OPEN_BREWERY_DB_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenBreweryApiService::class.java)

    private val foodFactsApi: OpenFoodFactsApiService = Retrofit.Builder()
        .baseUrl(OPEN_FOOD_FACTS_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenFoodFactsApiService::class.java)

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

        viewModelScope.launch {
            // Load Austrian breweries from JSON first
            loadAustrianBreweries()
            // Then load all breweries (including the Austrian ones we just added)
            loadBreweries()
        }
    }

    private suspend fun loadAustrianBreweries() {
        try {
            val inputStream = getApplication<Application>().assets.open("austria-beers.json")
            val reader = InputStreamReader(inputStream)
            val gson = Gson()
            val austriaData = gson.fromJson(reader, AustriaBeerJson::class.java)
            reader.close()
            
            val austrianBreweries = mutableListOf<BreweryEntity>()
            
            austriaData.regions.forEach { (region, regionData) ->
                regionData.breweries.forEach { brewery ->
                    val breweryId = "austria_${brewery.name.replace(" ", "_").lowercase()}"
                    
                    austrianBreweries.add(
                        BreweryEntity(
                            id = breweryId,
                            name = brewery.name,
                            breweryType = brewery.type ?: "micro",
                            street = null,
                            address1 = null,
                            address2 = null,
                            address3 = null,
                            city = brewery.location,
                            state = region,
                            countyProvince = null,
                            stateProvince = region,
                            postalCode = null,
                            country = "Austria",
                            longitude = null,
                            latitude = null,
                            phone = null,
                            websiteUrl = null,
                            updatedAt = null,
                            createdAt = null,
                            qr = brewery.qr
                        )
                    )
                }
            }
            
            // Insert Austrian breweries into database
            breweryDao.insertAll(austrianBreweries)
        } catch (e: Exception) {
            // Log error or handle it gracefully
            e.printStackTrace()
        }
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
                        createdAt = dto.created_at,
                        qr = null
                    )
                })

                // After inserting API breweries, reload from database to include Austrian breweries
                val allCachedBreweries = breweryDao.getAll()
                val allBreweriesList = allCachedBreweries.map { entity ->
                    BreweryListItem(
                        id = entity.id,
                        name = entity.name,
                        breweryType = entity.breweryType ?: "",
                        city = entity.city ?: "",
                        state = entity.state ?: "",
                        country = entity.country ?: ""
                    )
                }.sortedBy { it.name }
                
                _uiState.value = _uiState.value.copy(breweries = allBreweriesList, isLoading = false)
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

    fun searchByBarcode(barcode: String) {
        viewModelScope.launch {
            // First, try to find a brewery with this exact QR code
            val breweryWithQr = breweryDao.getAll().find { it.qr == barcode }
            
            if (breweryWithQr != null) {
                // Found a brewery with this QR code, search by its name
                _uiState.value = _uiState.value.copy(searchQuery = breweryWithQr.name)
            } else {
                // No QR match, try Open Food Facts API
                try {
                    val response = foodFactsApi.getProduct(barcode)
                    val product = response.product
                    if (response.status != 1 || product == null) {
                        _uiState.value = _uiState.value.copy(message = "Product not found")
                        applyFilters()
                        return@launch
                    }

                    // Only allow beers (based on OFF categories/tags/name).
                    if (!isBeerProduct(product)) {
                        _uiState.value = _uiState.value.copy(message = "Only beer products can be scanned")
                        applyFilters()
                        return@launch
                    }

                    val queryFromProduct =
                        product.brands?.takeIf { it.isNotBlank() }
                            ?: product.product_name?.takeIf { it.isNotBlank() }

                    if (queryFromProduct != null) {
                        _uiState.value = _uiState.value.copy(searchQuery = queryFromProduct)
                        applyFilters()

                        // If scanning a beer yields no matches, offer to add it.
                        if (_uiState.value.filteredBreweries.isEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                message = "Beer not found in your list. Add it?",
                                messageActionLabel = "Add",
                                messageAction = UiMessageAction.ADD_BREWERY_FROM_SCAN,
                                addBreweryPrefill = AddBreweryPrefill(
                                    name = queryFromProduct,
                                    breweryType = "micro",
                                    qr = barcode
                                )
                            )
                        }
                    } else {
                        // Beer is valid but no name/brand to search by -> offer add flow anyway.
                        _uiState.value = _uiState.value.copy(
                            message = "Beer found, but no name/brand. Add it manually?",
                            messageActionLabel = "Add",
                            messageAction = UiMessageAction.ADD_BREWERY_FROM_SCAN,
                            addBreweryPrefill = AddBreweryPrefill(
                                breweryType = "micro",
                                qr = barcode
                            )
                        )
                    }
                } catch (e: Exception) {
                    // API call failed, show "not found"
                    _uiState.value = _uiState.value.copy(message = "Scan failed (network or API error)")
                }
            }
            applyFilters()
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(
            message = null,
            messageActionLabel = null,
            messageAction = null
        )
    }

    fun performMessageAction() {
        when (_uiState.value.messageAction) {
            UiMessageAction.ADD_BREWERY_FROM_SCAN -> {
                _uiState.value = _uiState.value.copy(showAddBreweryDialog = true)
            }
            null -> Unit
        }
    }

    private fun isBeerProduct(product: OpenFoodFactsProduct): Boolean {
        val tags = product.categoriesTags.orEmpty().map { it.lowercase() }
        val categories = (product.categories ?: "").lowercase()
        val name = (product.product_name ?: "").lowercase()
        val brand = (product.brands ?: "").lowercase()

        // Strong signal: OFF category tags for beers
        if (tags.any { it.endsWith(":beers") || it.contains(":beer") }) return true

        // Fallback: keyword match in categories/name/brand (covers local languages a bit)
        val haystack = "$categories $name $brand"
        val beerKeywords = listOf(
            // English & Germanic
            "beer", "beers",
            "bier",
            "öl", "ol",
            "ale", "lager",

            // Romance
            "biere", "bière",
            "cerveza", "cervezas",
            "cerveja",
            "birra", "birre",
            "cervesa",
            "bere",

            // Slavic
            "pivo",
            "piwo",
            "pyvo",
            "bira",

            // Uralic
            "olut",
            "sör",
            "olu",

            // Celtic
            "beoir",
            "leann",
            "cwrw",

            // Greek & Balkan
            "mpyra", "býra", "μπύρα",
            "birrë",

            // Semitic
            "bira", "בירה", "بيرة",

            // Indo-Iranian
            "beer",        // common loanword usage
            "biyer",
            "aabjo", "abjo", "آبجو",

            // East Asian
            "pijiu", "啤酒",
            "biiru", "ビール",
            "maekju", "맥주",

            // Southeast Asian
            "bia",
            "bir",
            "serbesa",

            // African
            "ubhiya",
            "ọti oyinbo",

            // Constructed & classical
            "cerevisia",
            "biero"
        )

        return beerKeywords.any { haystack.contains(it) }
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()
        val type = state.selectedTypeFilter

        val filtered = if (query.isEmpty() && type == null) {
            // No filters applied, show all
            state.breweries
        } else {
            state.breweries.filter { brewery ->
                val matchesQuery = if (query.isEmpty()) {
                    true
                } else {
                    // Search in name, country, city, and state
                    brewery.name.lowercase().contains(query) ||
                    brewery.country.lowercase().contains(query) ||
                    brewery.city.lowercase().contains(query) ||
                    brewery.state.lowercase().contains(query)
                }
                val matchesType = type == null || brewery.breweryType.equals(type, ignoreCase = true)
                matchesQuery && matchesType
            }
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
                    createdAt = response.created_at,
                    qr = null
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

    fun showAddBreweryDialog() {
        _uiState.value = _uiState.value.copy(showAddBreweryDialog = true)
    }

    fun hideAddBreweryDialog() {
        _uiState.value = _uiState.value.copy(showAddBreweryDialog = false, addBreweryPrefill = null)
    }

    fun showEditBreweryDialog() {
        val brewery = _uiState.value.selectedBrewery
        _uiState.value = _uiState.value.copy(
            showEditBreweryDialog = true,
            breweryToEdit = brewery
        )
    }

    fun hideEditBreweryDialog() {
        _uiState.value = _uiState.value.copy(
            showEditBreweryDialog = false,
            breweryToEdit = null
        )
    }

    fun showDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = true)
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)
    }

    fun addBrewery(
        name: String,
        breweryType: String,
        city: String,
        country: String,
        state: String = "",
        street: String? = null,
        postalCode: String? = null,
        phone: String? = null,
        websiteUrl: String? = null
    ) {
        viewModelScope.launch {
            try {
                val newId = "MilosCodesBetterThanAdam<3_${System.currentTimeMillis()}"
                val qrFromScan = _uiState.value.addBreweryPrefill?.qr
                
                val breweryEntity = BreweryEntity(
                    id = newId,
                    name = name,
                    breweryType = breweryType,
                    street = street,
                    address1 = null,
                    address2 = null,
                    address3 = null,
                    city = city,
                    state = if (state.isNotEmpty()) state else null,
                    countyProvince = null,
                    stateProvince = null,
                    postalCode = postalCode,
                    country = country,
                    longitude = null,
                    latitude = null,
                    phone = phone,
                    websiteUrl = websiteUrl,
                    updatedAt = null,
                    createdAt = null,
                    qr = qrFromScan
                )
                
                breweryDao.insert(breweryEntity)
                
                val newBrewery = BreweryListItem(
                    id = newId,
                    name = name,
                    breweryType = breweryType,
                    city = city,
                    state = state,
                    country = country
                )
                
                val updatedBreweries = (_uiState.value.breweries + newBrewery).sortedBy { it.name }
                _uiState.value = _uiState.value.copy(
                    breweries = updatedBreweries,
                    showAddBreweryDialog = false,
                    addBreweryPrefill = null
                )
                applyFilters()
            } catch (e: Exception) {
                //Error Handling(TODO)
            }
        }
    }

    fun updateBrewery(
        id: String,
        name: String,
        breweryType: String,
        city: String,
        country: String,
        state: String = "",
        street: String? = null,
        postalCode: String? = null,
        phone: String? = null,
        websiteUrl: String? = null
    ) {
        viewModelScope.launch {
            try {
                val existingBrewery = breweryDao.getById(id)
                if (existingBrewery != null) {
                    val updatedEntity = BreweryEntity(
                        id = id,
                        name = name,
                        breweryType = breweryType,
                        street = street,
                        address1 = existingBrewery.address1,
                        address2 = existingBrewery.address2,
                        address3 = existingBrewery.address3,
                        city = city,
                        state = if (state.isNotEmpty()) state else null,
                        countyProvince = existingBrewery.countyProvince,
                        stateProvince = existingBrewery.stateProvince,
                        postalCode = postalCode,
                        country = country,
                        longitude = existingBrewery.longitude,
                        latitude = existingBrewery.latitude,
                        phone = phone,
                        websiteUrl = websiteUrl,
                        updatedAt = existingBrewery.updatedAt,
                        createdAt = existingBrewery.createdAt,
                        qr = existingBrewery.qr
                    )
                    
                    breweryDao.insert(updatedEntity)
                    
                    // Update in favorites if it's favorited
                    val isFavorite = favoritesRepository.isFavorite(id)
                    if (isFavorite) {
                        favoritesRepository.addFavorite(
                            FavoriteBreweryEntity(
                                breweryId = id,
                                name = name,
                                breweryType = breweryType,
                                city = city,
                                state = state,
                                country = country
                            )
                        )
                    }
                    
                    // Update the brewery in the list
                    val updatedBreweries = _uiState.value.breweries.map { brewery ->
                        if (brewery.id == id) {
                            BreweryListItem(
                                id = id,
                                name = name,
                                breweryType = breweryType,
                                city = city,
                                state = state,
                                country = country
                            )
                        } else {
                            brewery
                        }
                    }.sortedBy { it.name }
                    
                    // Update selected brewery if it's the one being edited
                    val updatedSelectedBrewery = if (_uiState.value.selectedBrewery?.id == id) {
                        BreweryDetail(
                            id = id,
                            name = name,
                            breweryType = breweryType,
                            street = street,
                            address1 = existingBrewery.address1,
                            address2 = existingBrewery.address2,
                            address3 = existingBrewery.address3,
                            city = city,
                            state = state,
                            countyProvince = existingBrewery.countyProvince,
                            stateProvince = existingBrewery.stateProvince,
                            postalCode = postalCode,
                            country = country,
                            longitude = existingBrewery.longitude,
                            latitude = existingBrewery.latitude,
                            phone = phone,
                            websiteUrl = websiteUrl,
                            updatedAt = existingBrewery.updatedAt,
                            createdAt = existingBrewery.createdAt
                        )
                    } else {
                        _uiState.value.selectedBrewery
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        breweries = updatedBreweries,
                        selectedBrewery = updatedSelectedBrewery,
                        showEditBreweryDialog = false,
                        breweryToEdit = null
                    )
                    applyFilters()
                }
            } catch (e: Exception) {
                //Error Handling(TODO)
            }
        }
    }

    fun deleteBrewery(id: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                // Check if brewery is in favorites and remove it
                val favorite = favoritesRepository.isFavorite(id)
                if (favorite) {
                    val favoriteEntity = _uiState.value.favorites.find { it.id == id }
                    if (favoriteEntity != null) {
                        favoritesRepository.removeFavorite(
                            FavoriteBreweryEntity(
                                breweryId = favoriteEntity.id,
                                name = favoriteEntity.name,
                                breweryType = favoriteEntity.breweryType,
                                city = favoriteEntity.city,
                                state = favoriteEntity.state,
                                country = favoriteEntity.country
                            )
                        )
                    }
                }
                
                // Delete from database
                breweryDao.deleteById(id)
                
                // Remove from breweries list
                val updatedBreweries = _uiState.value.breweries.filter { it.id != id }
                
                // Clear selected brewery if it's the one being deleted
                val updatedSelectedBrewery = if (_uiState.value.selectedBrewery?.id == id) {
                    null
                } else {
                    _uiState.value.selectedBrewery
                }
                
                _uiState.value = _uiState.value.copy(
                    breweries = updatedBreweries,
                    selectedBrewery = updatedSelectedBrewery,
                    showDeleteDialog = false
                )
                applyFilters()
                
                // Call the completion callback to navigate back
                onComplete()
            } catch (e: Exception) {
                //Error Handling(TODO)
            }
        }
    }

}
