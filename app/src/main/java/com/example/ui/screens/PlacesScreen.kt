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
import com.example.data.CafeEntity
import com.example.ui.CafeViewModel
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

    // Map Pan / Zoom states
    var scale by remember { mutableStateOf(1.2f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var selectedCafeOnMap by remember { mutableStateOf<CafeEntity?>(null) }

    val mapCenter = remember(cafes) {
        if (cafes.isNotEmpty()) {
            val avgLat = cafes.map { it.latitude }.average()
            val avgLng = cafes.map { it.longitude }.average()
            Pair(avgLat, avgLng)
        } else {
            Pair(37.7749, -122.4194)
        }
    }

    val centerLat = mapCenter.first
    val centerLng = mapCenter.second
    val scaleFactor = 12000f

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
            // Interactive vector map from original MapScreen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFEADDFF)) // Lavender ground
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 4.0f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
            ) {
                // Interactive Graphics Container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                ) {
                    // Map Canvas - stylized roads, parks, rivers
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2
                        val cy = h / 2

                        // Draw Parks
                        drawCircle(
                            color = Color(0xFFD4E2C6),
                            radius = 280f,
                            center = Offset(cx - 300f, cy - 200f)
                        )
                        drawRoundRect(
                            color = Color(0xFFD4E2C6),
                            topLeft = Offset(cx + 400f, cy + 100f),
                            size = Size(200f, 400f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(30f)
                        )

                        // Draw Water Body
                        val riverPath = Path().apply {
                            moveTo(0f, cy + 500f)
                            cubicTo(
                                cx - 400f, cy + 400f,
                                cx - 200f, cy + 800f,
                                w, cy + 600f
                            )
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(path = riverPath, color = Color(0xFFC0D5E3))

                        // Draw Streets
                        val roadColor = Color(0xFFF3EDF7)
                        val highwayColor = Color(0xFFD0BCFF)

                        for (i in -4..4) {
                            val yOffset = cy + i * 200f
                            drawLine(
                                color = roadColor,
                                start = Offset(0f, yOffset),
                                end = Offset(w, yOffset),
                                strokeWidth = 6f
                            )
                        }

                        for (i in -4..4) {
                            val xOffset = cx + i * 200f
                            drawLine(
                                color = roadColor,
                                start = Offset(xOffset, 0f),
                                end = Offset(xOffset, h),
                                strokeWidth = 6f
                            )
                        }

                        drawLine(
                            color = highwayColor,
                            start = Offset(0f, 0f),
                            end = Offset(w, h),
                            strokeWidth = 14f
                        )
                    }

                    // Overlaid Markers
                    cafes.forEach { cafe ->
                        val deltaLat = cafe.latitude - centerLat
                        val deltaLng = cafe.longitude - centerLng

                        val markerX = deltaLng * scaleFactor
                        val markerY = -deltaLat * scaleFactor

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (markerX).dp + 180.dp,
                                    y = (markerY).dp + 320.dp
                                )
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedCafeOnMap?.id == cafe.id) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondary
                                )
                                .border(2.dp, Color.White, CircleShape)
                                .clickable {
                                    selectedCafeOnMap = cafe
                                }
                                .testTag("map_marker_${cafe.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalCafe,
                                contentDescription = cafe.name,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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
                        onClick = { scale = (scale + 0.3f).coerceAtMost(4.0f) },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In")
                    }
                    FloatingActionButton(
                        onClick = { scale = (scale - 0.3f).coerceAtLeast(0.5f) },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
                    }
                    FloatingActionButton(
                        onClick = {
                            scale = 1.2f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Center Map")
                    }
                }

                // Pop-up details card
                selectedCafeOnMap?.let { cafe ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
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
