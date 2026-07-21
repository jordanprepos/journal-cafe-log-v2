package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import com.example.data.CafeEntity
import com.example.ui.CafeViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    cafeViewModel: CafeViewModel,
    modifier: Modifier = Modifier
) {
    val cafes by cafeViewModel.allCafes.collectAsState()
    val context = LocalContext.current

    // Toggle between list and map views
    var isMapView by remember { mutableStateOf(false) }

    var selectedCafeOnMap by remember { mutableStateOf<CafeEntity?>(null) }

    val mapsApiKey = remember(context) {
        try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            appInfo.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    val isApiKeyPlaceholder = remember(mapsApiKey) {
        mapsApiKey.isEmpty() || 
        mapsApiKey == "YOUR_MAPS_API_KEY" || 
        mapsApiKey.contains("PLACEHOLDER") ||
        mapsApiKey.startsWith("$")
    }

    // Default to offline map if API Key is placeholder, otherwise default to google map
    var useGoogleMap by remember(isApiKeyPlaceholder) { mutableStateOf(!isApiKeyPlaceholder) }

    // Offline Map Pan and Zoom states
    var offlineScale by remember { mutableStateOf(1.2f) }
    var offlineOffsetX by remember { mutableStateOf(0f) }
    var offlineOffsetY by remember { mutableStateOf(0f) }

    // WebView reference for OpenStreetMap
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val mapCenter = remember(cafes) {
        if (cafes.isNotEmpty()) {
            val avgLat = cafes.map { it.latitude }.average()
            val avgLng = cafes.map { it.longitude }.average()
            Pair(avgLat, avgLng)
        } else {
            Pair(37.7749, -122.4194)
        }
    }

    val htmlContent = remember(cafes, mapCenter) {
        val cafeMarkersJs = cafes.joinToString(separator = "\n") { cafe ->
            val escapedName = cafe.name.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
            "addMarker(${cafe.id}, ${cafe.latitude}, ${cafe.longitude}, \"$escapedName\");"
        }
        
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body { padding: 0; margin: 0; background-color: #f4ede6; }
                html, body, #map { height: 100%; width: 100vw; }
                .leaflet-control-attribution { display: none !important; }
                .custom-div-icon {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                }).setView([${mapCenter.first}, ${mapCenter.second}], 11);
                
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19
                }).addTo(map);

                var markers = {};

                function addMarker(id, lat, lng, name) {
                    var iconHtml = '<div style="background-color: #795548; width: 32px; height: 32px; border-radius: 50%; border: 2.5px solid white; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 6px rgba(0,0,0,0.4);">' +
                        '<svg style="width:18px;height:18px;fill:white" viewBox="0 0 24 24"><path d="M2 21h18v-2H2v2M20 8h-2V5h2v3M4 19h12v-4H4v4m0-6h12V5H4v8m16-5c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-4v7h4z"/></svg>' +
                        '</div>';
                    
                    var customIcon = L.divIcon({
                        html: iconHtml,
                        className: 'custom-div-icon',
                        iconSize: [32, 32],
                        iconAnchor: [16, 16]
                    });

                    var marker = L.marker([lat, lng], {icon: customIcon}).addTo(map);
                    marker.on('click', function() {
                        if (window.AndroidBridge) {
                            window.AndroidBridge.onCafeClicked(id);
                        }
                    });
                    markers[id] = {
                        marker: marker,
                        lat: lat,
                        lng: lng
                    };
                }

                function selectCafe(id) {
                    for (var key in markers) {
                        var isSel = (key == id);
                        var bg = isSel ? '#6750A4' : '#795548';
                        var border = isSel ? '#EADDFF' : '#FFFFFF';
                        var scale = isSel ? 'scale(1.2)' : 'scale(1)';
                        markers[key].marker.setIcon(L.divIcon({
                            html: '<div style="background-color: ' + bg + '; width: 32px; height: 32px; border-radius: 50%; border: 2.5px solid ' + border + '; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 6px rgba(0,0,0,0.4); transform: ' + scale + '; transition: all 0.2s ease;">' +
                                '<svg style="width:18px;height:18px;fill:white" viewBox="0 0 24 24"><path d="M2 21h18v-2H2v2M20 8h-2V5h2v3M4 19h12v-4H4v4m0-6h12V5H4v8m16-5c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-4v7h4z"/></svg>' +
                                '</div>',
                            className: 'custom-div-icon',
                            iconSize: [32, 32],
                            iconAnchor: [16, 16]
                        }));
                        if (isSel) {
                            map.setView([markers[key].lat, markers[key].lng], map.getZoom());
                        }
                    }
                }

                function zoomIn() {
                    map.zoomIn();
                }

                function zoomOut() {
                    map.zoomOut();
                }

                function resetCenter() {
                    map.setView([${mapCenter.first}, ${mapCenter.second}], 11);
                }

                // Add all markers
                $cafeMarkersJs
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    LaunchedEffect(selectedCafeOnMap, useGoogleMap, webViewRef) {
        if (!useGoogleMap) {
            val webView = webViewRef
            if (webView != null) {
                if (selectedCafeOnMap != null) {
                    webView.evaluateJavascript("javascript:selectCafe(${selectedCafeOnMap?.id})", null)
                } else {
                    webView.evaluateJavascript("javascript:selectCafe(-1)", null)
                }
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(mapCenter.first, mapCenter.second), 11f)
    }

    LaunchedEffect(mapCenter) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(mapCenter.first, mapCenter.second), 11f)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 24.dp, bottom = 8.dp, start = 24.dp, end = 24.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WHERE YOU'VE BEEN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        text = "Places",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.testTag("places_screen_title")
                    )
                }

                IconButton(
                    onClick = { isMapView = !isMapView },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .testTag("map_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isMapView) Icons.Default.List else Icons.Default.Map,
                        contentDescription = "Toggle Map View",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { innerPadding ->
        if (!isMapView) {
            // Screen 5: Editorial log list
            if (cafes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No places logged yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
                ) {
                    items(cafes) { cafe ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("place_item_${cafe.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left rounded terracotta circle containing custom pin icon
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable {
                                            cafeViewModel.selectCafe(cafe)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Pin icon",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Middle/Right Text contents
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cafe.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Serif
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    Text(
                                        text = cafe.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )

                                    if (!cafe.mapShareLink.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cafe.mapShareLink))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        // Fallback
                                                    }
                                                }
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OpenInNew,
                                                contentDescription = "Open Maps",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Open in Google Maps",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Map Container
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val cx = maxWidth / 2
                val cy = maxHeight / 2
                val centerLat = mapCenter.first
                val centerLng = mapCenter.second
                val scaleFactor = 12000f // Scaling factor from degree distance to pixels

                if (useGoogleMap) {
                    // Real Google Map view
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = false
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = false
                        )
                    ) {
                        cafes.forEach { cafe ->
                            Marker(
                                state = MarkerState(position = LatLng(cafe.latitude, cafe.longitude)),
                                title = cafe.name,
                                snippet = cafe.address,
                                onClick = {
                                    selectedCafeOnMap = cafe
                                    true // Consumed event to show our custom pop-up card instead of standard info window
                                }
                            )
                        }
                    }
                } else {
                    // OpenStreetMap WebView (No API Key Required)
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                }
                                webViewClient = WebViewClient()
                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onCafeClicked(id: Long) {
                                        val found = cafes.firstOrNull { it.id.toLong() == id }
                                        if (found != null) {
                                            selectedCafeOnMap = found
                                        }
                                    }
                                }, "AndroidBridge")
                                
                                loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
                                webViewRef = this
                            }
                        },
                        update = { webView ->
                            webViewRef = webView
                        }
                    )
                }

                // HUD Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .padding(bottom = 70.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (useGoogleMap) {
                                val currentZoom = cameraPositionState.position.zoom
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                    cameraPositionState.position.target,
                                    (currentZoom + 1f).coerceAtMost(21f)
                                )
                            } else {
                                webViewRef?.evaluateJavascript("javascript:zoomIn()", null)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In")
                    }
                    FloatingActionButton(
                        onClick = {
                            if (useGoogleMap) {
                                val currentZoom = cameraPositionState.position.zoom
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                    cameraPositionState.position.target,
                                    (currentZoom - 1f).coerceAtLeast(1f)
                                )
                            } else {
                                webViewRef?.evaluateJavascript("javascript:zoomOut()", null)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
                    }
                    FloatingActionButton(
                        onClick = {
                            if (useGoogleMap) {
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                    LatLng(mapCenter.first, mapCenter.second),
                                    11f
                                )
                            } else {
                                webViewRef?.evaluateJavascript("javascript:resetCenter()", null)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Center Map")
                    }
                }

                // Map Selection Segment Chips and Warning Banner Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { useGoogleMap = false }
                                .background(if (!useGoogleMap) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = if (!useGoogleMap) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "OpenStreetMap (Free)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (!useGoogleMap) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { useGoogleMap = true }
                                .background(if (useGoogleMap) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = if (useGoogleMap) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Google Maps",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (useGoogleMap) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (useGoogleMap && isApiKeyPlaceholder) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "MAPS_API_KEY is not configured in Secrets. Google Map tiles cannot be loaded. Please use \"OpenStreetMap (Free)\" above for testing.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Pop-up details card
                selectedCafeOnMap?.let { cafe ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .padding(bottom = 120.dp) // Offset above standard floating zoom/pan controls
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .testTag("map_popup_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cafe.photoUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(File(cafe.photoUri))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Coffee,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cafe.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = cafe.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = { cafeViewModel.selectCafe(cafe) }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "View details",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(onClick = { selectedCafeOnMap = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close card",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
