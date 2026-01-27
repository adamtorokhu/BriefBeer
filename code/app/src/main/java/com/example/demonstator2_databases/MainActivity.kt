package com.example.demonstator2_databases

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    data object Profile : BriefBeerDestination("profile", "Profile")
    data object BreweryDetail : BriefBeerDestination("brewery_detail", "Brewery")
    data object BarcodeScanner : BriefBeerDestination("barcode_scanner", "Scan")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefBeerApp(viewModel: BriefBeerViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message, uiState.messageActionLabel) {
        val msg = uiState.message ?: return@LaunchedEffect
        val actionLabel = uiState.messageActionLabel
        val action = uiState.messageAction
        val result = snackbarHostState.showSnackbar(
            message = msg,
            actionLabel = actionLabel
        )
        if (result == SnackbarResult.ActionPerformed) {
            // If user tapped "Add" from the scanner flow, first navigate back to Breweries,
            // then open the Add dialog.
            if (action == UiMessageAction.ADD_BREWERY_FROM_SCAN) {
                // Prefer popping the scanner off the back stack so we truly return to the list.
                val popped = navController.popBackStack(
                    route = BriefBeerDestination.BreweryList.route,
                    inclusive = false
                )
                if (!popped) {
                    navController.navigate(BriefBeerDestination.BreweryList.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
            viewModel.performMessageAction()
        }
        viewModel.clearMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            BriefBeerBottomBar(navController, viewModel, uiState)
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
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
                    } else if (currentDestination == BriefBeerDestination.BarcodeScanner.route) {
                        IconButton(onClick = {
                            // Remove the scanner screen and return to the breweries list
                            val popped = navController.popBackStack(
                                route = BriefBeerDestination.BreweryList.route,
                                inclusive = false
                            )
                            if (!popped) {
                                navController.navigate(BriefBeerDestination.BreweryList.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
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
fun BriefBeerBottomBar(navController: NavHostController, viewModel: BriefBeerViewModel, uiState: BriefBeerUiState) {
    val items = listOf(
        BriefBeerDestination.BreweryList,
        BriefBeerDestination.Profile
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val currentRoute = currentDestination?.route
        
        // Determine which parent tab should be highlighted
        val activeRoute = if (currentRoute == BriefBeerDestination.BreweryDetail.route) {
            // If on detail page, find the parent route from backstack
            navController.previousBackStackEntry?.destination?.route
        } else {
            currentRoute
        }
        
        items.forEach { screen ->
            val selected = activeRoute == screen.route
            NavigationBarItem(
                icon = {
                    when (screen) {
                        BriefBeerDestination.BreweryList -> Icon(Icons.Default.List, contentDescription = screen.label)
                        BriefBeerDestination.Favorites -> Icon(Icons.Default.Favorite, contentDescription = screen.label)
                        BriefBeerDestination.Profile -> Icon(Icons.Default.Person, contentDescription = screen.label)
                        else -> Icon(Icons.Default.List, contentDescription = screen.label)
                    }
                },
                label = { Text(screen.label) },
                selected = selected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.White.copy(alpha = 0.7f)
                ),
                onClick = {
                    val currentRoute = currentDestination?.route
                    
                    when {
                        // If on detail view, navigate to the selected screen
                        currentRoute == BriefBeerDestination.BreweryDetail.route -> {
                            val previousRoute = navController.previousBackStackEntry?.destination?.route
                            // If clicking the same page that opened the detail, just pop back
                            if (screen.route == previousRoute) {
                                viewModel.clearSelectedBrewery(screen.route)
                                navController.popBackStack()
                            } else {
                                // Navigate to different page, keeping the state
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        // If on BarcodeScanner, remove it completely and navigate to selected screen
                        currentRoute == BriefBeerDestination.BarcodeScanner.route -> {
                            // Always pop the barcode scanner first, then navigate
                            navController.popBackStack()
                            if (screen.route != navController.graph.findStartDestination().route) {
                                // Navigate to other screens (like Profile)
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        // Default navigation between main screens
                        else -> {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
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
            // If there's a selected brewery from this page, navigate to detail view
            LaunchedEffect(uiState.breweryListSelectedBrewery) {
                if (uiState.breweryListSelectedBrewery != null) {
                    navController.navigate(BriefBeerDestination.BreweryDetail.route) {
                        launchSingleTop = true
                    }
                }
            }
            
            BreweryListScreen(
                uiState = uiState,
                onSearchChange = viewModel::onSearchQueryChange,
                onTypeChange = viewModel::onTypeFilterChange,
                onBreweryClick = {
                    viewModel.selectBrewery(it, BriefBeerDestination.BreweryList.route)
                    navController.navigate(BriefBeerDestination.BreweryDetail.route)
                },
                onToggleFavorite = viewModel::toggleFavorite,
                onAddBrewery = viewModel::addBrewery,
                onShowAddDialog = viewModel::showAddBreweryDialog,
                onHideAddDialog = viewModel::hideAddBreweryDialog,
                onScanBarcode = { 
                    navController.navigate(BriefBeerDestination.BarcodeScanner.route)
                }
            )
        }
        composable(BriefBeerDestination.Profile.route) {
            // If there's a selected brewery from this page, navigate to detail view
            LaunchedEffect(uiState.profileSelectedBrewery) {
                if (uiState.profileSelectedBrewery != null) {
                    navController.navigate(BriefBeerDestination.BreweryDetail.route) {
                        launchSingleTop = true
                    }
                }
            }
            
            ProfileScreen(
                uiState = uiState,
                onUserNameSave = viewModel::saveProfileUserName,
                onOpenAvatarPicker = viewModel::showAvatarPicker,
                onCloseAvatarPicker = viewModel::hideAvatarPicker,
                onAvatarSelected = viewModel::setProfileAvatar,
                onBreweryClick = {
                    viewModel.selectBrewery(it, BriefBeerDestination.Profile.route)
                    navController.navigate(BriefBeerDestination.BreweryDetail.route)
                },
                onToggleFavorite = viewModel::toggleFavorite
            )
        }
        composable(BriefBeerDestination.BreweryDetail.route) {
            // Determine which page we came from by checking backstack
            val previousRoute = navController.previousBackStackEntry?.destination?.route
            val parentRoute = when (previousRoute) {
                BriefBeerDestination.BreweryList.route -> BriefBeerDestination.BreweryList.route
                BriefBeerDestination.Profile.route -> BriefBeerDestination.Profile.route
                else -> BriefBeerDestination.BreweryList.route
            }
            
            val selectedBrewery = when (parentRoute) {
                BriefBeerDestination.Profile.route -> uiState.profileSelectedBrewery
                else -> uiState.breweryListSelectedBrewery
            }
            
            // Handle back button to clear selected brewery and navigate back
            BackHandler {
                viewModel.clearSelectedBrewery(parentRoute)
                navController.popBackStack()
            }
            
            val isFavorite = selectedBrewery?.let { brewery ->
                uiState.favorites.any { it.id == brewery.id }
            } ?: false
            
            // Check if the brewery is editable (custom brewery)
            val isEditable = selectedBrewery?.id?.startsWith("MilosCodesBetterThanAdam<3_") == true
            
            if (uiState.showEditBreweryDialog && uiState.breweryToEdit != null) {
                EditBreweryDialog(
                    brewery = uiState.breweryToEdit,
                    onDismiss = viewModel::hideEditBreweryDialog,
                    onUpdateBrewery = viewModel::updateBrewery
                )
            }
            
            if (uiState.showDeleteDialog && selectedBrewery != null) {
                DeleteBreweryDialog(
                    breweryName = selectedBrewery.name,
                    onDismiss = viewModel::hideDeleteDialog,
                    onConfirmDelete = {
                        viewModel.deleteBrewery(selectedBrewery.id) {
                            navController.popBackStack()
                        }
                    }
                )
            }
            
            BreweryDetailScreen(
                detail = selectedBrewery,
                isFavorite = isFavorite,
                isEditable = isEditable,
                onToggleFavorite = { item ->
                    viewModel.toggleFavorite(item)
                },
                onEditBrewery = viewModel::showEditBreweryDialog,
                onDeleteBrewery = viewModel::showDeleteDialog
            )
        }
        composable(BriefBeerDestination.BarcodeScanner.route) {
            BarcodeScannerScreen(
                onBarcodeScanned = { barcode ->
                    viewModel.searchByBarcode(barcode)
                    // Navigate back to breweries after scanning
                    navController.popBackStack()
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
    onAddBrewery: (String, String, String, String, String, String?, String?, String?, String?, Uri?) -> Unit,
    onShowAddDialog: () -> Unit,
    onHideAddDialog: () -> Unit,
    onScanBarcode: () -> Unit
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
            onAddBrewery = onAddBrewery,
            prefill = uiState.addBreweryPrefill
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
                breweries = uiState.filteredBreweries,
        searchQuery = uiState.searchQuery,
        onSearchChange = onSearchChange,
                types = types,
                selectedType = uiState.selectedTypeFilter,
                onTypeChange = onTypeChange,
                onBreweryClick = onBreweryClick,
        onToggleFavorite = onToggleFavorite,
        favoriteIds = favoriteIds,
        onScanBarcode = onScanBarcode
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
fun ProfileScreen(
    uiState: BriefBeerUiState,
    onUserNameSave: (String) -> Unit,
    onOpenAvatarPicker: () -> Unit,
    onCloseAvatarPicker: () -> Unit,
    onAvatarSelected: (String) -> Unit,
    onBreweryClick: (String) -> Unit,
    onToggleFavorite: (BreweryListItem) -> Unit
) {
    var showEditNameDialog by rememberSaveable { mutableStateOf(false) }
    var draftName by rememberSaveable { mutableStateOf(uiState.profileUserName) }

    if (uiState.showAvatarPicker) {
        AvatarPickerDialog(
            onDismiss = onCloseAvatarPicker,
            onAvatarSelected = onAvatarSelected
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit username") },
            text = {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it.take(40) },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUserNameSave(draftName)
                        showEditNameDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Profile picture + greeting
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = "file:///android_asset/${uiState.profileAvatarAssetPath}",
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onOpenAvatarPicker() },
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Hello",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = uiState.profileUserName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FilledTonalIconButton(
                        onClick = {
                            draftName = uiState.profileUserName
                            showEditNameDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit username"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Tap the picture to change it",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // User Added Breweries Section
        Text(
            text = "Breweries I've Added",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "${uiState.userAddedBreweries.size} ${if (uiState.userAddedBreweries.size == 1) "brewery" else "breweries"}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (uiState.userAddedBreweries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "No breweries added yet",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val favoriteIds = remember(uiState.favorites) {
                uiState.favorites.map { it.id }.toSet()
            }
            
            // Display breweries in a grid layout without nested scrolling
            uiState.userAddedBreweries.chunked(2).forEach { rowBreweries ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowBreweries.forEach { brewery ->
                        Box(modifier = Modifier.weight(1f)) {
                            BreweryCard(
                                brewery = brewery,
                                onClick = { onBreweryClick(brewery.id) },
                                onToggleFavorite = { onToggleFavorite(brewery) },
                                isFavorite = favoriteIds.contains(brewery.id)
                            )
                        }
                    }
                    // Add empty space if odd number of items
                    if (rowBreweries.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Favorites Section
        Text(
            text = "My Favorites",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "${uiState.favorites.size} ${if (uiState.favorites.size == 1) "favorite" else "favorites"}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (uiState.favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "No favorites yet",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Display favorites in a grid layout without nested scrolling
            uiState.favorites.filter { it.id.isNotEmpty() && it.name.isNotEmpty() }.chunked(2).forEach { rowBreweries ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowBreweries.forEach { brewery ->
                        Box(modifier = Modifier.weight(1f)) {
                            BreweryCard(
                                brewery = brewery,
                                onClick = { onBreweryClick(brewery.id) },
                                onToggleFavorite = { onToggleFavorite(brewery) },
                                isFavorite = true
                            )
                        }
                    }
                    // Add empty space if odd number of items
                    if (rowBreweries.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
        // Add bottom padding to ensure content doesn't get cut off
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AvatarPickerDialog(
    onDismiss: () -> Unit,
    onAvatarSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val avatars = remember {
        val files = try {
            context.assets.list("avatars")?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        files
            .filter { it.lowercase().endsWith(".png") || it.lowercase().endsWith(".webp") || it.lowercase().endsWith(".jpg") || it.lowercase().endsWith(".jpeg") }
            .sorted()
            .map { "avatars/$it" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Choose profile picture") },
        text = {
            if (avatars.isEmpty()) {
                Text(
                    text = "No images found in assets/avatars",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(avatars) { assetPath ->
                        AsyncImage(
                            model = "file:///android_asset/$assetPath",
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .clickable { onAvatarSelected(assetPath) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun BreweryDetailScreen(
    detail: BreweryDetail?,
    isFavorite: Boolean = false,
    isEditable: Boolean = false,
    onToggleFavorite: (BreweryListItem) -> Unit,
    onEditBrewery: () -> Unit = {},
    onDeleteBrewery: () -> Unit = {}
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
        isEditable = isEditable,
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
        },
        onEditBrewery = onEditBrewery,
        onDeleteBrewery = onDeleteBrewery
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
    favoriteIds: Set<String> = emptySet(),
    onScanBarcode: () -> Unit
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
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = { Text("Search breweries") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            FilledTonalIconButton(
                onClick = onScanBarcode,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan barcode"
                )
            }
        }
        
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
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            types.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeChange(type) },
                    label = { Text(type.replaceFirstChar { it.uppercase() }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    // Display brewery image if available
                    if (brewery.imagePath != null) {
                        AsyncImage(
                            model = java.io.File(brewery.imagePath),
                            contentDescription = "Brewery photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "🍻",
                            fontSize = 48.sp
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
                        text = brewery.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (brewery.city.isNotEmpty() && brewery.country.isNotEmpty()) {
                        Text(
                            text = "${brewery.city}, ${brewery.country}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    } else if (brewery.city.isNotEmpty()) {
                        Text(
                            text = brewery.city,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (brewery.country.isNotEmpty()) {
                        Text(
                            text = brewery.country,
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
    isEditable: Boolean = false,
    onToggleFavorite: () -> Unit,
    onEditBrewery: () -> Unit = {},
    onDeleteBrewery: () -> Unit = {}
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
            // Display brewery image if available
            if (detail.imagePath != null) {
                AsyncImage(
                    model = java.io.File(detail.imagePath),
                    contentDescription = "Brewery photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "🍻",
                    fontSize = 80.sp
                )
            }
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
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditable) {
                            FloatingActionButton(
                                onClick = onDeleteBrewery,
                                modifier = Modifier.size(56.dp),
                                containerColor = MaterialTheme.colorScheme.error
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Brewery"
                                )
                            }
                            
                            FloatingActionButton(
                                onClick = onEditBrewery,
                                modifier = Modifier.size(56.dp),
                                containerColor = MaterialTheme.colorScheme.secondary
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Brewery"
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
    onAddBrewery: (String, String, String, String, String, String?, String?, String?, String?, Uri?) -> Unit,
    prefill: AddBreweryPrefill? = null
) {
    var name by remember(prefill) { mutableStateOf(prefill?.name ?: "") }
    var breweryType by remember(prefill) { mutableStateOf(prefill?.breweryType ?: "") }
    var city by remember(prefill) { mutableStateOf(prefill?.city ?: "") }
    var country by remember(prefill) { mutableStateOf(prefill?.country ?: "") }
    var state by remember(prefill) { mutableStateOf(prefill?.state ?: "") }
    var street by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            selectedImageUri = tempCameraUri
        }
    }
    
    fun createImageUri(context: Context): Uri {
        val tempDir = File(context.cacheDir, "camera_temp")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        val imageFile = File(tempDir, "brewery_temp_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

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
                val scannedQr = prefill?.qr
                if (!scannedQr.isNullOrBlank()) {
                    Text(
                        text = "Scanned barcode: $scannedQr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                
                // Image picker section
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Brewery Photo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Select Photo"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val uri = createImageUri(context)
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Take Photo"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera")
                    }
                    
                    if (selectedImageUri != null) {
                        OutlinedButton(
                            onClick = { 
                                selectedImageUri = null
                                tempCameraUri = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Photo"
                            )
                        }
                    }
                }
                
                // Show selected image preview
                selectedImageUri?.let { uri ->
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected brewery photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        },
        confirmButton = {
            Button(
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
                            websiteUrl.takeIf { it.isNotEmpty() },
                            selectedImageUri
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
                        selectedImageUri = null
                        phone = ""
                        websiteUrl = ""
                    }
                },
                enabled = name.isNotEmpty() && breweryType.isNotEmpty() && city.isNotEmpty() && country.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text("Add Brewery")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBreweryDialog(
    brewery: BreweryDetail,
    onDismiss: () -> Unit,
    onUpdateBrewery: (String, String, String, String, String, String, String?, String?, String?, String?, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(brewery.name) }
    var breweryType by remember { mutableStateOf(brewery.breweryType) }
    var city by remember { mutableStateOf(brewery.city ?: "") }
    var country by remember { mutableStateOf(brewery.country ?: "") }
    var state by remember { mutableStateOf(brewery.state ?: "") }
    var street by remember { mutableStateOf(brewery.street ?: "") }
    var postalCode by remember { mutableStateOf(brewery.postalCode ?: "") }
    var phone by remember { mutableStateOf(brewery.phone ?: "") }
    var websiteUrl by remember { mutableStateOf(brewery.websiteUrl ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var keepExistingImage by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) keepExistingImage = false
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            selectedImageUri = tempCameraUri
            keepExistingImage = false
        }
    }
    
    fun createImageUri(context: Context): Uri {
        val tempDir = File(context.cacheDir, "camera_temp")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        val imageFile = File(tempDir, "brewery_temp_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

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
        "🇰🇭 Cambodia",
        "🇨🇲 Cameroon",
        "🇨🇦 Canada",
        "🇨🇻 Cape Verde",
        "🇨🇫 Central African Republic",
        "🇹🇩 Chad",
        "🇨🇱 Chile",
        "🇨🇳 China",
        "🇨🇴 Colombia",
        "🇰🇲 Comoros",
        "🇨🇬 Congo",
        "🇨🇷 Costa Rica",
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
        "🇰🇵 Korea North",
        "🇰🇷 Korea South",
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
        "🇲🇰 Macedonia",
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
        "🇳🇴 Norway",
        "🇴🇲 Oman",
        "🇵🇰 Pakistan",
        "🇵🇼 Palau",
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
        "🇻🇨 Saint Vincent",
        "🇼🇸 Samoa",
        "🇸🇲 San Marino",
        "🇸🇹 Sao Tome",
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
        "🇸🇸 South Sudan",
        "🇪🇸 Spain",
        "🇱🇰 Sri Lanka",
        "🇸🇩 Sudan",
        "🇸🇷 Suriname",
        "🇸🇿 Swaziland",
        "🇸🇪 Sweden",
        "🇨🇭 Switzerland",
        "🇸🇾 Syria",
        "🇹🇼 Taiwan",
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

    var expandedBreweryType by remember { mutableStateOf(false) }
    var expandedCountry by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Brewery",
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

                ExposedDropdownMenuBox(
                    expanded = expandedBreweryType,
                    onExpandedChange = { expandedBreweryType = !expandedBreweryType }
                ) {
                    OutlinedTextField(
                        value = breweryType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Brewery Type *") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = expandedBreweryType,
                        onDismissRequest = { expandedBreweryType = false }
                    ) {
                        breweryTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    breweryType = type
                                    expandedBreweryType = false
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

                ExposedDropdownMenuBox(
                    expanded = expandedCountry,
                    onExpandedChange = { expandedCountry = !expandedCountry }
                ) {
                    OutlinedTextField(
                        value = country,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Country *") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCountry,
                        onDismissRequest = { expandedCountry = false }
                    ) {
                        countries.forEach { countryOption ->
                            DropdownMenuItem(
                                text = { Text(countryOption) },
                                onClick = {
                                    country = countryOption
                                    expandedCountry = false
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
                
                // Image update section
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Brewery Photo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Select Photo"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val uri = createImageUri(context)
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Take Photo"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera")
                    }
                    
                    if (selectedImageUri != null || (brewery.imagePath != null && keepExistingImage)) {
                        OutlinedButton(
                            onClick = { 
                                selectedImageUri = null
                                tempCameraUri = null
                                keepExistingImage = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Photo"
                            )
                        }
                    }
                }
                
                // Show image preview
                val imageToShow = selectedImageUri ?: if (keepExistingImage && brewery.imagePath != null) {
                    brewery.imagePath
                } else null
                
                imageToShow?.let { image ->
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = if (image is String) java.io.File(image) else image,
                        contentDescription = "Brewery photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && breweryType.isNotEmpty() && city.isNotEmpty() && country.isNotEmpty()) {
                        onUpdateBrewery(
                            brewery.id,
                            name,
                            breweryType,
                            city,
                            country,
                            state,
                            street.takeIf { it.isNotEmpty() },
                            postalCode.takeIf { it.isNotEmpty() },
                            phone.takeIf { it.isNotEmpty() },
                            websiteUrl.takeIf { it.isNotEmpty() },
                            selectedImageUri
                        )
                    }
                },
                enabled = name.isNotEmpty() && breweryType.isNotEmpty() && city.isNotEmpty() && country.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text("Update Brewery")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteBreweryDialog(
    breweryName: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Brewery",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"$breweryName\"? This action cannot be undone.",
                fontSize = 16.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}