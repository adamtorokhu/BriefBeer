package com.example.demonstator2_databases

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.demonstator2_databases.ui.theme.Demonstator2databasesTheme

// Helper function to build Google Maps URL from coordinates
fun buildGoogleMapsUrl(latitude: String, longitude: String): String {
    return "https://www.google.com/maps?q=$latitude,$longitude"
}

class MainActivity : ComponentActivity() {
    private val viewModel: BriefBeerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Demonstator2databasesTheme {
                BriefBeerApp(viewModel)
            }
        }
    }
}

sealed class BriefBeerDestination(val route: String, val label: String) {
    data object BreweryList : BriefBeerDestination("brewery_list", "Breweries")
    data object Favorites : BriefBeerDestination("favorites", "Favorites")
    data object BreweryDetail : BriefBeerDestination("brewery_detail", "Brewery")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefBeerApp(viewModel: BriefBeerViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            BriefBeerBottomBar(navController)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "BriefBeer",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = backStackEntry?.destination?.route
                    if (currentDestination == BriefBeerDestination.BreweryDetail.route) {
                        IconButton(onClick = {
                            viewModel.clearSelectedBrewery()
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            BriefBeerNavHost(
                navController = navController,
                viewModel = viewModel,
                uiState = uiState
            )
        }
    }
}

@Composable
fun BriefBeerBottomBar(navController: NavHostController) {
    val items = listOf(
        BriefBeerDestination.BreweryList,
        BriefBeerDestination.Favorites
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { screen ->
            val selected = currentDestination?.route == screen.route
            NavigationBarItem(
                icon = {
                    if (screen == BriefBeerDestination.BreweryList) {
                        Icon(Icons.Default.List, contentDescription = screen.label)
                    } else {
                        Icon(Icons.Default.Favorite, contentDescription = screen.label)
                    }
                },
                label = { Text(screen.label) },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun BriefBeerNavHost(
    navController: NavHostController,
    viewModel: BriefBeerViewModel,
    uiState: BriefBeerUiState
) {
    NavHost(
        navController = navController,
        startDestination = BriefBeerDestination.BreweryList.route
    ) {
        composable(BriefBeerDestination.BreweryList.route) {
            BreweryListScreen(
                uiState = uiState,
                onSearchChange = viewModel::onSearchQueryChange,
                onTypeChange = viewModel::onTypeFilterChange,
                onBreweryClick = {
                    viewModel.selectBrewery(it)
                    navController.navigate(BriefBeerDestination.BreweryDetail.route)
                },
                onToggleFavorite = viewModel::toggleFavorite,
                onAddBrewery = viewModel::addBrewery,
                onShowAddDialog = viewModel::showAddBreweryDialog,
                onHideAddDialog = viewModel::hideAddBreweryDialog
            )
        }
        composable(BriefBeerDestination.Favorites.route) {
            FavoritesScreen(
                uiState = uiState,
                onBreweryClick = {
                    viewModel.selectBrewery(it)
                    navController.navigate(BriefBeerDestination.BreweryDetail.route)
                },
                onToggleFavorite = viewModel::toggleFavorite
            )
        }
        composable(BriefBeerDestination.BreweryDetail.route) {
            val isFavorite = uiState.selectedBrewery?.let { brewery ->
                uiState.favorites.any { it.id == brewery.id }
            } ?: false
            BreweryDetailScreen(
                detail = uiState.selectedBrewery,
                isFavorite = isFavorite,
                onToggleFavorite = { item ->
                    viewModel.toggleFavorite(item)
                }
            )
        }
    }
}

@Composable
fun BreweryListScreen(
    uiState: BriefBeerUiState,
    onSearchChange: (String) -> Unit,
    onTypeChange: (String?) -> Unit,
    onBreweryClick: (String) -> Unit,
    onToggleFavorite: (BreweryListItem) -> Unit,
    onAddBrewery: (String, String, String, String, String, String?, String?, String?, String?) -> Unit,
    onShowAddDialog: () -> Unit,
    onHideAddDialog: () -> Unit
) {
    val types = remember(uiState.breweries) {
        uiState.breweries.map { it.breweryType }.distinct().filter { it.isNotEmpty() }.sorted()
    }
    val favoriteIds = remember(uiState.favorites) {
        uiState.favorites.map { it.id }.toSet()
    }
    
    if (uiState.showAddBreweryDialog) {
        AddBreweryDialog(
            onDismiss = onHideAddDialog,
            onAddBrewery = onAddBrewery
        )
    }
    
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            BreweryGridContent(
                breweries = uiState.filteredBreweries.ifEmpty { uiState.breweries },
        searchQuery = uiState.searchQuery,
        onSearchChange = onSearchChange,
                types = types,
                selectedType = uiState.selectedTypeFilter,
                onTypeChange = onTypeChange,
                onBreweryClick = onBreweryClick,
        onToggleFavorite = onToggleFavorite,
        favoriteIds = favoriteIds
    )
            
            FloatingActionButton(
                onClick = onShowAddDialog,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Brewery"
                )
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    uiState: BriefBeerUiState,
    onBreweryClick: (String) -> Unit,
    onToggleFavorite: (BreweryListItem) -> Unit
) {
    BreweryFavoritesContent(
        favorites = uiState.favorites.filter { 
            it.id.isNotEmpty() && it.name.isNotEmpty() 
        },
        onBreweryClick = onBreweryClick,
        onToggleFavorite = onToggleFavorite
    )
}

@Composable
fun BreweryDetailScreen(
    detail: BreweryDetail?,
    isFavorite: Boolean = false,
    onToggleFavorite: (BreweryListItem) -> Unit
) {
    if (detail == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select a brewery",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }

    BreweryDetailContent(
        detail = detail,
        isFavorite = isFavorite,
        onToggleFavorite = {
            onToggleFavorite(
                BreweryListItem(
                    id = detail.id,
                    name = detail.name,
                    breweryType = detail.breweryType,
                    city = detail.city ?: "",
                    state = detail.state ?: "",
                    country = detail.country ?: ""
                )
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreweryGridContent(
    breweries: List<BreweryListItem>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    types: List<String>,
    selectedType: String?,
    onTypeChange: (String?) -> Unit,
    onBreweryClick: (String) -> Unit,
    onToggleFavorite: (BreweryListItem) -> Unit,
    favoriteIds: Set<String> = emptySet()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Breweries",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            label = { Text("Search breweries") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            singleLine = true
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == null,
                onClick = { onTypeChange(null) },
                label = { Text("All") }
            )
            types.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeChange(type) },
                    label = { Text(type.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(breweries) { brewery ->
                BreweryCard(
                    brewery = brewery,
                    onClick = { onBreweryClick(brewery.id) },
                    onToggleFavorite = { onToggleFavorite(brewery) },
                    isFavorite = brewery.id in favoriteIds
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreweryCard(
    brewery: BreweryListItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    isFavorite: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = brewery.name.take(2).uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = brewery.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (brewery.country.isNotEmpty() && brewery.city.isNotEmpty()) {
                        Text(
                            text = "${brewery.country}, ${brewery.city}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (brewery.breweryType.isNotEmpty()) {
                        Text(
                            text = brewery.breweryType.replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            IconButton(
                onClick = { onToggleFavorite() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreweryFavoritesContent(
    favorites: List<BreweryListItem>,
    onBreweryClick: (String) -> Unit,
    onToggleFavorite: (BreweryListItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Favorite Breweries",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No favorites yet",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(favorites) { brewery ->
                    if (brewery.id.isNotEmpty() && brewery.name.isNotEmpty()) {
                        BreweryCard(
                            brewery = brewery,
                            onClick = { onBreweryClick(brewery.id) },
                            onToggleFavorite = { onToggleFavorite(brewery) },
                            isFavorite = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BreweryDetailContent(
    detail: BreweryDetail,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = detail.name.take(2).uppercase(),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = detail.name,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (detail.breweryType.isNotEmpty()) {
                        Text(
                                text = detail.breweryType.replaceFirstChar { it.uppercase() },
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        }
                    }
                    
                    FloatingActionButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(56.dp),
                        containerColor = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite"
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                Text(
                    text = "Address",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (!detail.street.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Street",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = detail.street,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }
                        if (!detail.address1.isNullOrEmpty()) {
                            if (!detail.street.isNullOrEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                    text = "Address 1",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = detail.address1,
                                    fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }
                        if (!detail.address2.isNullOrEmpty()) {
                            if (!detail.street.isNullOrEmpty() || !detail.address1.isNullOrEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Address 2",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = detail.address2,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }
                        if (!detail.address3.isNullOrEmpty()) {
                            if (!detail.street.isNullOrEmpty() || !detail.address1.isNullOrEmpty() || !detail.address2.isNullOrEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Address 3",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = detail.address3,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }
                        if (!detail.city.isNullOrEmpty()) {
                            if (!detail.street.isNullOrEmpty() || !detail.address1.isNullOrEmpty() || !detail.address2.isNullOrEmpty() || !detail.address3.isNullOrEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "City",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = detail.city,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (!detail.state.isNullOrEmpty()) {
                            if (!detail.street.isNullOrEmpty() || !detail.address1.isNullOrEmpty() || !detail.address2.isNullOrEmpty() || !detail.address3.isNullOrEmpty() || !detail.city.isNullOrEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "State",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = detail.state,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (!detail.countyProvince.isNullOrEmpty()) {
                            if (!detail.street.isNullOrEmpty() || !detail.address1.isNullOrEmpty() || !detail.address2.isNullOrEmpty() || !detail.address3.isNullOrEmpty() || !detail.city.isNullOrEmpty() || !detail.state.isNullOrEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "County/Province",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = detail.countyProvince,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                            )
                        }
                    }
                        if (!detail.stateProvince.isNullOrEmpty()) {
                            if (!detail.street.isNullOrEmpty() || !detail.address1.isNullOrEmpty() || !detail.address2.isNullOrEmpty() || !detail.address3.isNullOrEmpty() || !detail.city.isNullOrEmpty() || !detail.state.isNullOrEmpty() || !detail.countyProvince.isNullOrEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "State/Province",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = detail.stateProvince,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (!detail.postalCode.isNullOrEmpty()) {
                            if (!detail.street.isNullOrEmpty() || !detail.address1.isNullOrEmpty() || !detail.address2.isNullOrEmpty() || !detail.address3.isNullOrEmpty() || !detail.city.isNullOrEmpty() || !detail.state.isNullOrEmpty() || !detail.countyProvince.isNullOrEmpty() || !detail.stateProvince.isNullOrEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Postal Code",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = detail.postalCode,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (!detail.country.isNullOrEmpty()) {
                            if (!detail.street.isNullOrEmpty() || !detail.address1.isNullOrEmpty() || !detail.address2.isNullOrEmpty() || !detail.address3.isNullOrEmpty() || !detail.city.isNullOrEmpty() || !detail.state.isNullOrEmpty() || !detail.countyProvince.isNullOrEmpty() || !detail.stateProvince.isNullOrEmpty() || !detail.postalCode.isNullOrEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Country",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = detail.country,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                if (!detail.longitude.isNullOrEmpty() || !detail.latitude.isNullOrEmpty()) {
                    Text(
                        text = "Coordinates",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            if (!detail.latitude.isNullOrEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                Text(
                                        text = "Latitude",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = detail.latitude,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (!detail.longitude.isNullOrEmpty()) {
                                if (!detail.latitude.isNullOrEmpty()) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Longitude",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = detail.longitude,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            // Add Google Maps link button if both coordinates are available
                            if (!detail.latitude.isNullOrEmpty() && !detail.longitude.isNullOrEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                val context = LocalContext.current
                                Button(
                                    onClick = {
                                        val lat = detail.latitude
                                        val lng = detail.longitude
                                        val url = buildGoogleMapsUrl(lat, lng)
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Location",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open in Google Maps")
                                }
                            }
                        }
                    }
                }
                
                if (!detail.phone.isNullOrEmpty() || !detail.websiteUrl.isNullOrEmpty()) {
                    Text(
                        text = "Contact",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                            if (!detail.phone.isNullOrEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                        text = "Phone",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                        text = detail.phone,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            }
                            if (!detail.websiteUrl.isNullOrEmpty()) {
                                if (!detail.phone.isNullOrEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Website",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = detail.websiteUrl,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (!detail.createdAt.isNullOrEmpty() || !detail.updatedAt.isNullOrEmpty()) {
                Text(
                        text = "Metadata",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            if (!detail.createdAt.isNullOrEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                                        text = "Created At",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = detail.createdAt,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (!detail.updatedAt.isNullOrEmpty()) {
                                if (!detail.createdAt.isNullOrEmpty()) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Updated At",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = detail.updatedAt,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBreweryDialog(
    onDismiss: () -> Unit,
    onAddBrewery: (String, String, String, String, String, String?, String?, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var breweryType by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }

    val breweryTypes = listOf("micro", "nano", "regional", "brewpub", "large", "planning", "bar", "contract", "proprietor", "closed")

    val countries = listOf(
        "🇦🇫 Afghanistan",
        "🇦🇱 Albania",
        "🇩🇿 Algeria",
        "🇦🇩 Andorra",
        "🇦🇴 Angola",
        "🇦🇬 Antigua and Barbuda",
        "🇦🇷 Argentina",
        "🇦🇲 Armenia",
        "🇦🇺 Australia",
        "🇦🇹 Austria",
        "🇦🇿 Azerbaijan",
        "🇧🇸 Bahamas",
        "🇧🇭 Bahrain",
        "🇧🇩 Bangladesh",
        "🇧🇧 Barbados",
        "🇧🇾 Belarus",
        "🇧🇪 Belgium",
        "🇧🇿 Belize",
        "🇧🇯 Benin",
        "🇧🇹 Bhutan",
        "🇧🇴 Bolivia",
        "🇧🇦 Bosnia and Herzegovina",
        "🇧🇼 Botswana",
        "🇧🇷 Brazil",
        "🇧🇳 Brunei",
        "🇧🇬 Bulgaria",
        "🇧🇫 Burkina Faso",
        "🇧🇮 Burundi",
        "🇨🇻 Cabo Verde",
        "🇰🇭 Cambodia",
        "🇨🇲 Cameroon",
        "🇨🇦 Canada",
        "🇨🇫 Central African Republic",
        "🇹🇩 Chad",
        "🇨🇱 Chile",
        "🇨🇳 China",
        "🇨🇴 Colombia",
        "🇰🇲 Comoros",
        "🇨🇬 Congo",
        "🇨🇩 Congo (Democratic Republic)",
        "🇨🇷 Costa Rica",
        "🇨🇮 Côte d’Ivoire",
        "🇭🇷 Croatia",
        "🇨🇺 Cuba",
        "🇨🇾 Cyprus",
        "🇨🇿 Czech Republic",
        "🇩🇰 Denmark",
        "🇩🇯 Djibouti",
        "🇩🇲 Dominica",
        "🇩🇴 Dominican Republic",
        "🇪🇨 Ecuador",
        "🇪🇬 Egypt",
        "🇸🇻 El Salvador",
        "🇬🇶 Equatorial Guinea",
        "🇪🇷 Eritrea",
        "🇪🇪 Estonia",
        "🇸🇿 Eswatini",
        "🇪🇹 Ethiopia",
        "🇫🇯 Fiji",
        "🇫🇮 Finland",
        "🇫🇷 France",
        "🇬🇦 Gabon",
        "🇬🇲 Gambia",
        "🇬🇪 Georgia",
        "🇩🇪 Germany",
        "🇬🇭 Ghana",
        "🇬🇷 Greece",
        "🇬🇩 Grenada",
        "🇬🇹 Guatemala",
        "🇬🇳 Guinea",
        "🇬🇼 Guinea-Bissau",
        "🇬🇾 Guyana",
        "🇭🇹 Haiti",
        "🇭🇳 Honduras",
        "🇭🇺 Hungary",
        "🇮🇸 Iceland",
        "🇮🇳 India",
        "🇮🇩 Indonesia",
        "🇮🇷 Iran",
        "🇮🇶 Iraq",
        "🇮🇪 Ireland",
        "🇮🇱 Israel",
        "🇮🇹 Italy",
        "🇯🇲 Jamaica",
        "🇯🇵 Japan",
        "🇯🇴 Jordan",
        "🇰🇿 Kazakhstan",
        "🇰🇪 Kenya",
        "🇰🇮 Kiribati",
        "🇰🇼 Kuwait",
        "🇰🇬 Kyrgyzstan",
        "🇱🇦 Laos",
        "🇱🇻 Latvia",
        "🇱🇧 Lebanon",
        "🇱🇸 Lesotho",
        "🇱🇷 Liberia",
        "🇱🇾 Libya",
        "🇱🇮 Liechtenstein",
        "🇱🇹 Lithuania",
        "🇱🇺 Luxembourg",
        "🇲🇬 Madagascar",
        "🇲🇼 Malawi",
        "🇲🇾 Malaysia",
        "🇲🇻 Maldives",
        "🇲🇱 Mali",
        "🇲🇹 Malta",
        "🇲🇭 Marshall Islands",
        "🇲🇷 Mauritania",
        "🇲🇺 Mauritius",
        "🇲🇽 Mexico",
        "🇫🇲 Micronesia",
        "🇲🇩 Moldova",
        "🇲🇨 Monaco",
        "🇲🇳 Mongolia",
        "🇲🇪 Montenegro",
        "🇲🇦 Morocco",
        "🇲🇿 Mozambique",
        "🇲🇲 Myanmar",
        "🇳🇦 Namibia",
        "🇳🇷 Nauru",
        "🇳🇵 Nepal",
        "🇳🇱 Netherlands",
        "🇳🇿 New Zealand",
        "🇳🇮 Nicaragua",
        "🇳🇪 Niger",
        "🇳🇬 Nigeria",
        "🇰🇵 North Korea",
        "🇲🇰 North Macedonia",
        "🇳🇴 Norway",
        "🇴🇲 Oman",
        "🇵🇰 Pakistan",
        "🇵🇼 Palau",
        "🇵🇸 Palestine",
        "🇵🇦 Panama",
        "🇵🇬 Papua New Guinea",
        "🇵🇾 Paraguay",
        "🇵🇪 Peru",
        "🇵🇭 Philippines",
        "🇵🇱 Poland",
        "🇵🇹 Portugal",
        "🇶🇦 Qatar",
        "🇷🇴 Romania",
        "🇷🇺 Russia",
        "🇷🇼 Rwanda",
        "🇰🇳 Saint Kitts and Nevis",
        "🇱🇨 Saint Lucia",
        "🇻🇨 Saint Vincent and the Grenadines",
        "🇼🇸 Samoa",
        "🇸🇲 San Marino",
        "🇸🇹 São Tomé and Príncipe",
        "🇸🇦 Saudi Arabia",
        "🇸🇳 Senegal",
        "🇷🇸 Serbia",
        "🇸🇨 Seychelles",
        "🇸🇱 Sierra Leone",
        "🇸🇬 Singapore",
        "🇸🇰 Slovakia",
        "🇸🇮 Slovenia",
        "🇸🇧 Solomon Islands",
        "🇸🇴 Somalia",
        "🇿🇦 South Africa",
        "🇰🇷 South Korea",
        "🇸🇸 South Sudan",
        "🇪🇸 Spain",
        "🇱🇰 Sri Lanka",
        "🇸🇩 Sudan",
        "🇸🇷 Suriname",
        "🇸🇪 Sweden",
        "🇨🇭 Switzerland",
        "🇸🇾 Syria",
        "🇹🇯 Tajikistan",
        "🇹🇿 Tanzania",
        "🇹🇭 Thailand",
        "🇹🇱 Timor-Leste",
        "🇹🇬 Togo",
        "🇹🇴 Tonga",
        "🇹🇹 Trinidad and Tobago",
        "🇹🇳 Tunisia",
        "🇹🇷 Turkey",
        "🇹🇲 Turkmenistan",
        "🇹🇻 Tuvalu",
        "🇺🇬 Uganda",
        "🇺🇦 Ukraine",
        "🇦🇪 United Arab Emirates",
        "🇬🇧 United Kingdom",
        "🇺🇸 United States",
        "🇺🇾 Uruguay",
        "🇺🇿 Uzbekistan",
        "🇻🇺 Vanuatu",
        "🇻🇦 Vatican City",
        "🇻🇪 Venezuela",
        "🇻🇳 Vietnam",
        "🇾🇪 Yemen",
        "🇿🇲 Zambia",
        "🇿🇼 Zimbabwe"
    )


    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Brewery",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                var breweryTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = breweryTypeExpanded,
                    onExpandedChange = { breweryTypeExpanded = !breweryTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = breweryType,
                        onValueChange = { },
                        label = { Text("Brewery Type *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = breweryTypeExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = breweryTypeExpanded,
                        onDismissRequest = { breweryTypeExpanded = false }
                    ) {
                        breweryTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    breweryType = type
                                    breweryTypeExpanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                var countryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = countryExpanded,
                    onExpandedChange = { countryExpanded = !countryExpanded }
                ) {
                    OutlinedTextField(
                        value = country,
                        onValueChange = { },
                        label = { Text("Country *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = countryExpanded,
                        onDismissRequest = { countryExpanded = false }
                    ) {
                        countries.forEach { countryName ->
                            DropdownMenuItem(
                                text = { Text(countryName) },
                                onClick = {
                                    country = countryName
                                    countryExpanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State/Province") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("Street Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = { postalCode = it },
                    label = { Text("Postal Code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = websiteUrl,
                    onValueChange = { websiteUrl = it },
                    label = { Text("Website URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotEmpty() && breweryType.isNotEmpty() && city.isNotEmpty() && country.isNotEmpty()) {
                        onAddBrewery(
                            name,
                            breweryType,
                            city,
                            country,
                            state,
                            street.takeIf { it.isNotEmpty() },
                            postalCode.takeIf { it.isNotEmpty() },
                            phone.takeIf { it.isNotEmpty() },
                            websiteUrl.takeIf { it.isNotEmpty() }
                        )
                        // Reset fields
                        name = ""
                        breweryType = ""
                        city = ""
                        country = ""
                        state = ""
                        street = ""
                        postalCode = ""
                        phone = ""
                        websiteUrl = ""
                    }
                },
                enabled = name.isNotEmpty() && breweryType.isNotEmpty() && city.isNotEmpty() && country.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}