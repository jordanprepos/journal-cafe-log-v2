package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import com.example.ui.CafeViewModel
import kotlin.random.Random
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring


data class PresetLocation(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val shareLink: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCafeScreen(
    cafeViewModel: CafeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val saveSuccess by cafeViewModel.saveSuccess.collectAsState()

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var mapShareLink by remember { mutableStateOf<String?>(null) }
    var tags by remember { mutableStateOf("") }
    var favoriteDrink by remember { mutableStateOf("") }
    var isSuggestionsExpanded by remember { mutableStateOf(false) }
    var isParsingUrl by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var isSearchingOnline by remember { mutableStateOf(false) }
    var onlineSuggestions by remember { mutableStateOf<List<PresetLocation>>(emptyList()) }

    // Trigger online location search with debounce
    LaunchedEffect(name) {
        val trimmed = name.trim()
        if (trimmed.isBlank() || trimmed.startsWith("http") || trimmed.contains("maps.google") || trimmed.contains("maps.app.goo") || trimmed.contains("goo.gl/maps")) {
            onlineSuggestions = emptyList()
            return@LaunchedEffect
        }
        
        // Wait 600ms to debounce keystrokes
        delay(600)
        
        isSearchingOnline = true
        try {
            val results = searchLocationsOnline(trimmed)
            onlineSuggestions = results
        } catch (e: Exception) {
            onlineSuggestions = emptyList()
        } finally {
            isSearchingOnline = false
        }
    }
    
    // Preset SF roasting hotspot locations with real coordinates and valid search query links
    val presetLocations = remember {
        listOf(
            PresetLocation("Blue Bottle Coffee - Mint Plaza", "66 Mint St, San Francisco, CA 94103", 37.7825, -122.4080, "https://www.google.com/maps/search/?api=1&query=${Uri.encode("Blue Bottle Coffee - Mint Plaza, 66 Mint St, San Francisco, CA 94103")}"),
            PresetLocation("Ritual Coffee Roasters - Valencia", "1026 Valencia St, San Francisco, CA 94110", 37.7564, -122.4214, "https://www.google.com/maps/search/?api=1&query=${Uri.encode("Ritual Coffee Roasters - Valencia, 1026 Valencia St, San Francisco, CA 94110")}"),
            PresetLocation("Sightglass Coffee - SOMA", "270 7th St, San Francisco, CA 94103", 37.7770, -122.4084, "https://www.google.com/maps/search/?api=1&query=${Uri.encode("Sightglass Coffee - SOMA, 270 7th St, San Francisco, CA 94103")}"),
            PresetLocation("Four Barrel Coffee - Mission", "375 Valencia St, San Francisco, CA 94103", 37.7670, -122.4219, "https://www.google.com/maps/search/?api=1&query=${Uri.encode("Four Barrel Coffee - Mission, 375 Valencia St, San Francisco, CA 94103")}"),
            PresetLocation("Philz Coffee - 24th Street", "3101 24th St, San Francisco, CA 94110", 37.7524, -122.4143, "https://www.google.com/maps/search/?api=1&query=${Uri.encode("Philz Coffee - 24th Street, 3101 24th St, San Francisco, CA 94110")}"),
            PresetLocation("Sextant Coffee Roasters - Folsom", "1415 Folsom St, San Francisco, CA 94103", 37.7725, -122.4116, "https://www.google.com/maps/search/?api=1&query=${Uri.encode("Sextant Coffee Roasters - Folsom, 1415 Folsom St, San Francisco, CA 94103")}"),
            PresetLocation("Andytown Coffee Roasters - Lawton", "3655 Lawton St, San Francisco, CA 94122", 37.7578, -122.5023, "https://www.google.com/maps/search/?api=1&query=${Uri.encode("Andytown Coffee Roasters - Lawton, 3655 Lawton St, San Francisco, CA 94122")}"),
            PresetLocation("Saint Frank Coffee - Russian Hill", "2340 Polk St, San Francisco, CA 94109", 37.7996, -122.4223, "https://www.google.com/maps/search/?api=1&query=${Uri.encode("Saint Frank Coffee - Russian Hill, 2340 Polk St, San Francisco, CA 94109")}"),
            PresetLocation("Flywheel Coffee Roasters - Golden Gate", "672 Stanyan St, San Francisco, CA 94117", 37.7691, -122.4526, "https://www.google.com/maps/search/?api=1&query=${Uri.encode("Flywheel Coffee Roasters - Golden Gate, 672 Stanyan St, San Francisco, CA 94117")}"),
            PresetLocation("Verve Coffee Roasters - Castro", "2101 Market St, San Francisco, CA 94114", 37.7668, -122.4294, "https://www.google.com/maps/search/?api=1&query=${Uri.encode("Verve Coffee Roasters - Castro, 2101 Market St, San Francisco, CA 94114")}")
        )
    }

    var latitude by remember { mutableStateOf("37.7749") }
    var longitude by remember { mutableStateOf("-122.4194") }
    var isManualModeEnabled by remember { mutableStateOf(false) }
    var rating by remember { mutableIntStateOf(5) }
    var coffeeQualityRating by remember { mutableIntStateOf(5) }
    var atmosphereRating by remember { mutableIntStateOf(5) }
    var notes by remember { mutableStateOf("") }
    
    val selectedPhotos = remember { mutableStateListOf<Uri>() }

    // Launcher for multiple photo selection
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedPhotos.addAll(uris)
        }
    }

    // React to successful save
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            cafeViewModel.resetSaveSuccess()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log New Cafe Visit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Interactive Mini-Map (Above the Input field)
            val mapLat = latitude.toDoubleOrNull() ?: 37.7749
            val mapLng = longitude.toDoubleOrNull() ?: -122.4194
            
            AddCafeMiniMap(
                latitude = mapLat,
                longitude = mapLng,
                isManualMode = isManualModeEnabled,
                onCoordinatesChanged = { newLat, newLng ->
                    latitude = String.format(java.util.Locale.US, "%.6f", newLat)
                    longitude = String.format(java.util.Locale.US, "%.6f", newLng)
                    if (address.isBlank() || address.startsWith("Manual Pin") || address.startsWith("Manually Dropped")) {
                        address = String.format(java.util.Locale.US, "Manual Pin (Lat: %.4f, Lng: %.4f)", newLat, newLng)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_cafe_mini_map")
            )

            if (isManualModeEnabled) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_mode_active_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Manual Mode Active",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Manual Pin Placement Active",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Long-press anywhere on the mini-map above to set the location's latitude and longitude.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(onClick = { isManualModeEnabled = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Cafe Name & Location Input with Autocomplete
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        isSuggestionsExpanded = true
                    },
                    label = { Text("Location / Cafe Name") },
                    placeholder = { Text("Search location or type name...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            IconButton(onClick = { 
                                name = "" 
                                address = ""
                                mapShareLink = null
                                isSuggestionsExpanded = false
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cafe_name_input")
                )

                // Match Criteria Dropdown List
                val filteredSuggestions = remember(name, onlineSuggestions) {
                    if (name.isBlank()) {
                        emptyList()
                    } else {
                        val presets = presetLocations.filter {
                            it.name.contains(name, ignoreCase = true) ||
                            it.address.contains(name, ignoreCase = true)
                        }
                        (onlineSuggestions + presets).distinctBy { it.name.lowercase() + "_" + it.address.lowercase() }
                    }
                }

                val isLink = remember(name) {
                    name.trim().startsWith("http") || 
                    name.contains("maps.google") || 
                    name.contains("maps.app.goo") || 
                    name.contains("goo.gl/maps")
                }

                // If empty name, show a Tip
                if (name.isBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Tip Info",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "💡 Tip: You can paste a Google Maps share link to auto-fill the location name, address, and GPS coordinates instantly!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // If there's a parsing error, display it
                if (parseError != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Parse Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = parseError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // If link or suggestions are present, show container
                if (isLink || (isSuggestionsExpanded && (filteredSuggestions.isNotEmpty() || isSearchingOnline))) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isLink) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .clickable(enabled = !isParsingUrl) {
                                            coroutineScope.launch {
                                                isParsingUrl = true
                                                parseError = null
                                                val parsed = parseGoogleMapsUrl(name)
                                                if (parsed != null) {
                                                    name = parsed.name
                                                    address = parsed.address
                                                    latitude = parsed.latitude.toString()
                                                    longitude = parsed.longitude.toString()
                                                    mapShareLink = parsed.shareLink
                                                    isSuggestionsExpanded = false
                                                } else {
                                                    parseError = "Failed to load location coordinates. Please verify your link or enter details manually."
                                                }
                                                isParsingUrl = false
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isParsingUrl) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = "Link Icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isParsingUrl) "Importing location..." else "Import Location from Google Maps Link",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (isParsingUrl) "Connecting and extracting GPS coords..." else "Tap to resolve coordinates automatically",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                    if (!isParsingUrl) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = "Import",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            if (isSuggestionsExpanded && (filteredSuggestions.isNotEmpty() || isSearchingOnline)) {
                                if (isLink) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }

                                if (isSearchingOnline) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Searching places worldwide...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                if (filteredSuggestions.isNotEmpty()) {
                                    Text(
                                        text = "Matching Locations",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    filteredSuggestions.forEach { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    name = suggestion.name
                                                    address = suggestion.address
                                                    latitude = suggestion.latitude.toString()
                                                    longitude = suggestion.longitude.toString()
                                                    mapShareLink = suggestion.shareLink
                                                    isSuggestionsExpanded = false
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = "Location Pin",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = suggestion.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = suggestion.address,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                                           }
                        }
                    }
                }

                // If search failed (name typed, but suggestions list is empty and not currently searching online)
                if (isSuggestionsExpanded && name.isNotBlank() && filteredSuggestions.isEmpty() && !isSearchingOnline && !isLink) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .testTag("manual_coordinates_override_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Search Failed",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "No Places Found",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = "We couldn't find any places matching \"" + name + "\". Would you like to drop a pin manually on the map instead?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Button(
                                onClick = {
                                    isManualModeEnabled = !isManualModeEnabled
                                    if (address.isBlank()) {
                                        address = name
                                    }
                                    isSuggestionsExpanded = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isManualModeEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("enable_manual_pin_drop_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Pin Drop"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isManualModeEnabled) "Deactivate Manual Pin Drop" else "Set Coordinates Manually",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }  }

            // Address Input
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                placeholder = { Text("e.g. 123 Sansome St, San Francisco") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cafe_address_input")
            )

            // Maps Share Link Banner (Visual feedback when link is set)
            if (!mapShareLink.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Link Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Google Maps Share Link Captured!",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = mapShareLink ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
            }

            // Advanced Coordinates (Optional toggle)
            var showAdvancedCoords by remember { mutableStateOf(false) }
            Column {
                TextButton(
                    onClick = { showAdvancedCoords = !showAdvancedCoords },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        imageVector = if (showAdvancedCoords) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Coordinates"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showAdvancedCoords) "Hide GPS Coordinates" else "Show GPS Coordinates",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (showAdvancedCoords) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = latitude,
                            onValueChange = { 
                                latitude = it
                                // Auto-assign share link if customized
                                if (mapShareLink == null) {
                                    val latVal = it.toDoubleOrNull() ?: 37.7749
                                    val lngVal = longitude.toDoubleOrNull() ?: -122.4194
                                    mapShareLink = "https://www.google.com/maps/search/?api=1&query=$latVal,$lngVal"
                                }
                            },
                            label = { Text("Latitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cafe_lat_input")
                        )
                        OutlinedTextField(
                            value = longitude,
                            onValueChange = { 
                                longitude = it
                                // Auto-assign share link if customized
                                if (mapShareLink == null) {
                                    val latVal = latitude.toDoubleOrNull() ?: 37.7749
                                    val lngVal = it.toDoubleOrNull() ?: -122.4194
                                    mapShareLink = "https://www.google.com/maps/search/?api=1&query=$latVal,$lngVal"
                                }
                            },
                            label = { Text("Longitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cafe_lng_input")
                        )
                    }
                }
            }

            // Ratings Area
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Experience Rating",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    InteractiveStarRating(
                        rating = rating,
                        onRatingChanged = { rating = it },
                        label = "Overall Experience",
                        testTagPrefix = "star_overall",
                        activeColor = Color(0xFFE07A5F)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    InteractiveStarRating(
                        rating = coffeeQualityRating,
                        onRatingChanged = { coffeeQualityRating = it },
                        label = "Coffee Quality",
                        testTagPrefix = "star_coffee",
                        activeColor = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    InteractiveStarRating(
                        rating = atmosphereRating,
                        onRatingChanged = { atmosphereRating = it },
                        label = "Atmosphere",
                        testTagPrefix = "star_atmosphere",
                        activeColor = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Photo Uploader Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cafe Photos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("pick_photos_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddAPhoto,
                                contentDescription = "Add Photos",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (selectedPhotos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Photos Placeholder",
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap to upload café & latte pictures",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(selectedPhotos) { uri ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Gray)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(uri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Selected Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Remove Photo badge button
                                    IconButton(
                                        onClick = { selectedPhotos.remove(uri) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .padding(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Favorite Drink Section
            OutlinedTextField(
                value = favoriteDrink,
                onValueChange = { favoriteDrink = it },
                label = { Text("Favorite Drink Name") },
                placeholder = { Text("e.g. Iced oat latte, Espresso, Pour over...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cafe_favorite_drink_input")
            )

            // Diary Tags Section with Preset Chips Selection
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Select Tags",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                val presetTags = listOf(
                    "Work-friendly", "Date Night", "Coffee Roastery", "Smokers Friendly",
                    "Non Smokers Only", "Free Wi-Fi", "No Wifi", "Instagram-able"
                )
                
                val currentTagList = remember(tags) {
                    tags.split(";").map { it.trim() }.filter { it.isNotEmpty() }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetTags.forEach { presetTag ->
                        val isSelected = currentTagList.any { it.equals(presetTag, ignoreCase = true) }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    val updatedList = if (isSelected) {
                                        currentTagList.filter { !it.equals(presetTag, ignoreCase = true) }
                                    } else {
                                        currentTagList + presetTag
                                    }
                                    tags = updatedList.joinToString("; ")
                                }
                                .testTag("tag_chip_$presetTag")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = presetTag,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Diary Tags (separated by semicolons)") },
                    placeholder = { Text("e.g. cosy;work-friendly;roastery") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cafe_tags_input")
                )
            }

            // Notes Section (Roasting / Quality / Atmosphere notes)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Tasting Notes & Atmosphere Review") },
                placeholder = { Text("Describe the espresso roast, acidity, milk textures, seating, music vibes...") },
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cafe_notes_input")
            )

            // Save Action
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val latVal = latitude.toDoubleOrNull() ?: 37.7749
                    val lngVal = longitude.toDoubleOrNull() ?: -122.4194

                    cafeViewModel.addCafe(
                        context = context,
                        name = name,
                        address = address.ifBlank { name },
                        latitude = latVal,
                        longitude = lngVal,
                        rating = rating,
                        coffeeQualityRating = coffeeQualityRating,
                        atmosphereRating = atmosphereRating,
                        notes = notes,
                        selectedPhotoUris = selectedPhotos.toList(),
                        mapShareLink = mapShareLink ?: "https://www.google.com/maps/search/?api=1&query=${Uri.encode(name + " " + address)}",
                        tags = tags.ifBlank { null },
                        favoriteDrink = favoriteDrink.ifBlank { null }
                    )
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 24.dp)
                    .testTag("save_cafe_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Cafe Log Entry", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun AddCafeMiniMap(
    latitude: Double,
    longitude: Double,
    isManualMode: Boolean,
    onCoordinatesChanged: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = if (isManualMode) 2.dp else 1.dp,
                color = if (isManualMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            )
            .background(if (isManualMode) Color(0xFFF3EDF7) else Color(0xFFEADDFF))
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }

        val pointerModifier = if (isManualMode) {
            Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        val dx = offset.x - (widthPx / 2f)
                        val dy = offset.y - (heightPx / 2f)
                        val newLat = latitude - (dy / heightPx) * 0.01
                        val newLng = longitude + (dx / widthPx) * 0.02
                        onCoordinatesChanged(newLat, newLng)
                    }
                )
            }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(pointerModifier)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2
                val cy = h / 2

                // Draw abstract parks
                drawCircle(
                    color = Color(0xFFD4E2C6),
                    radius = 120f,
                    center = Offset(cx - 200f, cy - 60f)
                )
                drawRoundRect(
                    color = Color(0xFFD4E2C6),
                    topLeft = Offset(cx + 150f, cy + 30f),
                    size = Size(100f, 150f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(15f)
                )

                // Draw river
                val riverPath = Path().apply {
                    moveTo(0f, cy + 100f)
                    cubicTo(cx - 150f, cy + 80f, cx - 50f, cy + 180f, w, cy + 120f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = riverPath, color = Color(0xFFC0D5E3))

                // Draw road networks
                val roadColor = if (isManualMode) Color(0xFFE1D5EC) else Color(0xFFF3EDF7)
                val highwayColor = if (isManualMode) Color(0xFFB0A2D3) else Color(0xFFD0BCFF)

                for (i in -2..2) {
                    drawLine(
                        color = roadColor,
                        start = Offset(0f, cy + i * 80f),
                        end = Offset(w, cy + i * 80f),
                        strokeWidth = 4f
                    )
                }

                for (i in -3..3) {
                    drawLine(
                        color = roadColor,
                        start = Offset(cx + i * 80f, 0f),
                        end = Offset(cx + i * 80f, h),
                        strokeWidth = 4f
                    )
                }

                drawLine(
                    color = highwayColor,
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                    strokeWidth = 8f
                )

                // Central pulse indicator
                drawCircle(
                    color = Color(0xFF6750A4).copy(alpha = 0.2f),
                    radius = 35f,
                    center = Offset(cx, cy)
                )
            }

            // Location marker pin
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Pin",
                    tint = Color(0xFF6750A4),
                    modifier = Modifier.size(32.dp)
                )
            }

            // Coordinate tag overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = String.format(java.util.Locale.US, "Lat: %.4f, Lng: %.4f", latitude, longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }

            // Manual Mode Badge
            if (isManualMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Manual Mode",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Long-Press Map to Drop Pin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

data class ParsedLocation(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val shareLink: String
)

suspend fun parseGoogleMapsUrl(inputUrl: String): ParsedLocation? {
    return withContext(Dispatchers.IO) {
        try {
            var currentUrl = inputUrl.trim()
            if (!currentUrl.startsWith("http://") && !currentUrl.startsWith("https://")) {
                currentUrl = "https://$currentUrl"
            }

            var conn: HttpURLConnection? = null
            var redirectCount = 0
            // Follow up to 5 redirects to get the final canonical URL
            while (redirectCount < 5) {
                val url = URL(currentUrl)
                conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                
                val status = conn.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || 
                    status == HttpURLConnection.HTTP_MOVED_PERM || 
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308) {
                    val newUrl = conn.getHeaderField("Location")
                    if (newUrl != null) {
                        currentUrl = if (newUrl.startsWith("http")) {
                            newUrl
                        } else {
                            val baseUri = URL(currentUrl)
                            "${baseUri.protocol}://${baseUri.host}$newUrl"
                        }
                        redirectCount++
                    } else {
                        break
                    }
                } else {
                    break
                }
            }

            // Now we have the final canonical URL
            // Extract coordinates using regex patterns
            // Pattern 1: look for @lat,lng
            val atPattern = "@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)".toRegex()
            var matchResult = atPattern.find(currentUrl)
            
            var lat: Double? = null
            var lng: Double? = null
            
            if (matchResult != null) {
                lat = matchResult.groupValues[1].toDoubleOrNull()
                lng = matchResult.groupValues[2].toDoubleOrNull()
            } else {
                // Pattern 2: look for query params like q=lat,lng or query=lat,lng or ll=lat,lng
                val qPattern = "[?&](?:q|query|ll)=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)".toRegex()
                matchResult = qPattern.find(currentUrl)
                if (matchResult != null) {
                    lat = matchResult.groupValues[1].toDoubleOrNull()
                    lng = matchResult.groupValues[2].toDoubleOrNull()
                }
            }

            // Extract place name from path or query
            // Pattern 1: /place/Name/
            val placePattern = "/place/([^/]+)".toRegex()
            var rawName: String? = null
            val placeMatch = placePattern.find(currentUrl)
            if (placeMatch != null) {
                rawName = placeMatch.groupValues[1]
            }
            
            // Decode the place name
            var decodedName = "Imported Location"
            if (!rawName.isNullOrBlank()) {
                try {
                    decodedName = URLDecoder.decode(rawName, "UTF-8").replace('+', ' ')
                } catch (e: Exception) {
                    // Fallback
                }
            } else {
                // Try extracting from query params if /place/ is missing
                val nameQueryPattern = "[?&]q=([^&]+)".toRegex()
                val queryMatch = nameQueryPattern.find(currentUrl)
                if (queryMatch != null) {
                    val qVal = queryMatch.groupValues[1]
                    if (!qVal.contains(",")) { // Make sure it's not raw coordinates
                        try {
                            decodedName = URLDecoder.decode(qVal, "UTF-8").replace('+', ' ')
                        } catch (e: Exception) {
                            // Fallback
                        }
                    }
                }
            }

            if (lat != null && lng != null) {
                ParsedLocation(
                    name = decodedName,
                    address = decodedName,
                    latitude = lat,
                    longitude = lng,
                    shareLink = inputUrl // Save the original share link entered by the user
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

fun parseNominatimResponse(jsonString: String): List<PresetLocation> {
    val list = mutableListOf<PresetLocation>()
    try {
        val array = org.json.JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val displayName = obj.optString("display_name", "")
            val latStr = obj.optString("lat", "37.7749")
            val lonStr = obj.optString("lon", "-122.4194")
            val lat = latStr.toDoubleOrNull() ?: 37.7749
            val lon = lonStr.toDoubleOrNull() ?: -122.4194
            
            // Extract name and address from display_name
            val parts = displayName.split(",")
            val name = parts.firstOrNull()?.trim() ?: "Unknown Location"
            val address = if (parts.size > 1) {
                parts.drop(1).joinToString(",").trim()
            } else {
                displayName
            }
            
            val queryText = "$name, $address"
            val shareLink = "https://www.google.com/maps/search/?api=1&query=${java.net.URLEncoder.encode(queryText, "UTF-8")}"
            list.add(PresetLocation(name, address, lat, lon, shareLink))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

suspend fun searchLocationsOnline(query: String): List<PresetLocation> {
    return withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=5&addressdetails=1&countrycodes=id"
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("User-Agent", "CafeDiaryApp/1.0 (christopherjordantp@gmail.com)")
            
            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
                parseNominatimResponse(jsonString)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            conn?.disconnect()
        }
    }
}

@Composable
fun InteractiveStarRating(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    label: String,
    testTagPrefix: String,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFE07A5F),
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
) {
    val ratingDescription = when (rating) {
        1 -> "Poor"
        2 -> "Fair"
        3 -> "Average"
        4 -> "Good"
        5 -> "Excellent"
        else -> ""
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (rating > 0) "$rating/5 ($ratingDescription)" else "Not Rated",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (rating > 0) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            repeat(5) { index ->
                val starIndex = index + 1
                val isSelected = starIndex <= rating
                
                var isPressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 1.25f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "star_scale"
                )

                LaunchedEffect(isPressed) {
                    if (isPressed) {
                        delay(100)
                        isPressed = false
                    }
                }

                IconButton(
                    onClick = {
                        isPressed = true
                        onRatingChanged(starIndex)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("${testTagPrefix}_$starIndex")
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Rate $starIndex stars out of 5",
                        tint = if (isSelected) activeColor else inactiveColor,
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale
                            )
                    )
                }
            }
        }
    }
}


