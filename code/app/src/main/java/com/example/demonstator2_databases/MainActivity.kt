package com.example.demonstator2_databases

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

class MainActivity : ComponentActivity() {
    private val viewModel: ArcaneAtlasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Demonstator2databasesTheme {
                ArcaneAtlasApp(viewModel)
            }
        }
    }
}

sealed class ArcaneAtlasDestination(val route: String, val label: String) {
    data object ChampionList : ArcaneAtlasDestination("champion_list", "Champion List")
    data object Favorites : ArcaneAtlasDestination("favorites", "Profile")
    data object ChampionDetail : ArcaneAtlasDestination("champion_detail", "Champion")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcaneAtlasApp(viewModel: ArcaneAtlasViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            ArcaneAtlasBottomBar(navController)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ArcaneAtlas",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = backStackEntry?.destination?.route
                    if (currentDestination == ArcaneAtlasDestination.ChampionDetail.route) {
                        IconButton(onClick = {
                            viewModel.clearSelectedChampion()
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
            ArcaneAtlasNavHost(
                navController = navController,
                viewModel = viewModel,
                uiState = uiState
            )
        }
    }
}

@Composable
fun ArcaneAtlasBottomBar(navController: NavHostController) {
    val items = listOf(
        ArcaneAtlasDestination.ChampionList,
        ArcaneAtlasDestination.Favorites
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { screen ->
            val selected = currentDestination?.route == screen.route
            NavigationBarItem(
                icon = {
                    if (screen == ArcaneAtlasDestination.ChampionList) {
                        Icon(Icons.Default.List, contentDescription = screen.label)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = screen.label)
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
fun ArcaneAtlasNavHost(
    navController: NavHostController,
    viewModel: ArcaneAtlasViewModel,
    uiState: ArcaneAtlasUiState
) {
    NavHost(
        navController = navController,
        startDestination = ArcaneAtlasDestination.ChampionList.route
    ) {
        composable(ArcaneAtlasDestination.ChampionList.route) {
            ChampionListScreen(
                uiState = uiState,
                onSearchChange = viewModel::onSearchQueryChange,
                onRoleChange = viewModel::onRoleFilterChange,
                onChampionClick = {
                    viewModel.selectChampion(it)
                    navController.navigate(ArcaneAtlasDestination.ChampionDetail.route)
                },
                onToggleFavorite = viewModel::toggleFavorite
            )
        }
        composable(ArcaneAtlasDestination.Favorites.route) {
            FavoritesScreen(
                uiState = uiState,
                onChampionClick = {
                    viewModel.selectChampion(it)
                    navController.navigate(ArcaneAtlasDestination.ChampionDetail.route)
                },
                onToggleFavorite = viewModel::toggleFavorite
            )
        }
        composable(ArcaneAtlasDestination.ChampionDetail.route) {
            val isFavorite = uiState.selectedChampion?.let { champ ->
                uiState.favorites.any { it.id == champ.id }
            } ?: false
            ChampionDetailScreen(
                detail = uiState.selectedChampion,
                isFavorite = isFavorite,
                onToggleFavorite = { item ->
                    viewModel.toggleFavorite(item)
                }
            )
        }
    }
}

@Composable
fun ChampionListScreen(
    uiState: ArcaneAtlasUiState,
    onSearchChange: (String) -> Unit,
    onRoleChange: (String?) -> Unit,
    onChampionClick: (String) -> Unit,
    onToggleFavorite: (ChampionListItem) -> Unit
) {
    val roles = remember(uiState.champions) {
        uiState.champions.flatMap { it.roles }.distinct().sorted()
    }
    val favoriteIds = remember(uiState.favorites) {
        uiState.favorites.map { it.id }.toSet()
    }
    ChampionGridContent(
        champions = uiState.filteredChampions.ifEmpty { uiState.champions },
        searchQuery = uiState.searchQuery,
        onSearchChange = onSearchChange,
        roles = roles,
        selectedRole = uiState.selectedRoleFilter,
        onRoleChange = onRoleChange,
        onChampionClick = onChampionClick,
        onToggleFavorite = onToggleFavorite,
        favoriteIds = favoriteIds
    )
}

@Composable
fun FavoritesScreen(
    uiState: ArcaneAtlasUiState,
    onChampionClick: (String) -> Unit,
    onToggleFavorite: (ChampionListItem) -> Unit
) {
    ChampionFavoritesContent(
        favorites = uiState.favorites.filter { 
            it.id.isNotEmpty() && it.name.isNotEmpty() 
        },
        onChampionClick = onChampionClick,
        onToggleFavorite = onToggleFavorite
    )
}

@Composable
fun ChampionDetailScreen(
    detail: ChampionDetail?,
    isFavorite: Boolean = false,
    onToggleFavorite: (ChampionListItem) -> Unit
) {
    if (detail == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select a champion",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }

    ChampionDetailContent(
        detail = detail,
        isFavorite = isFavorite,
        onToggleFavorite = {
            onToggleFavorite(
                ChampionListItem(
                    id = detail.id,
                    name = detail.name,
                    title = detail.title,
                    roles = detail.roles,
                    imageUrl = detail.imageUrl
                )
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampionGridContent(
    champions: List<ChampionListItem>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    roles: List<String>,
    selectedRole: String?,
    onRoleChange: (String?) -> Unit,
    onChampionClick: (String) -> Unit,
    onToggleFavorite: (ChampionListItem) -> Unit,
    favoriteIds: Set<String> = emptySet()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Champions",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            label = { Text("Search champions") },
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
                selected = selectedRole == null,
                onClick = { onRoleChange(null) },
                label = { Text("All") }
            )
            roles.forEach { role ->
                FilterChip(
                    selected = selectedRole == role,
                    onClick = { onRoleChange(role) },
                    label = { Text(role) }
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(champions) { champ ->
                ChampionCard(
                    champion = champ,
                    onClick = { onChampionClick(champ.id) },
                    onToggleFavorite = { onToggleFavorite(champ) },
                    isFavorite = champ.id in favoriteIds
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampionCard(
    champion: ChampionListItem,
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
                    if (champion.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = champion.imageUrl,
                            contentDescription = champion.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = champion.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (champion.title.isNotEmpty()) {
                        Text(
                            text = champion.title,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
fun ChampionFavoritesContent(
    favorites: List<ChampionListItem>,
    onChampionClick: (String) -> Unit,
    onToggleFavorite: (ChampionListItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "List of stored champions",
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
                items(favorites) { champ ->
                    if (champ.id.isNotEmpty() && champ.name.isNotEmpty()) {
                        ChampionCard(
                            champion = champ,
                            onClick = { onChampionClick(champ.id) },
                            onToggleFavorite = { onToggleFavorite(champ) },
                            isFavorite = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChampionDetailContent(
    detail: ChampionDetail,
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
                .height(350.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = detail.imageUrl,
                contentDescription = detail.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
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
                        Text(
                            text = detail.title,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
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
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    detail.roles.forEach { role ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = role,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                Text(
                    text = "Stats",
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
                        detail.stats.forEach { (label, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = value.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (label != detail.stats.keys.last()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
                
                Text(
                    text = "Lore",
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
                    Text(
                        text = detail.lore,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}