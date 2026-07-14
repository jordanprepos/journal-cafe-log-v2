package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.CafeEntity
import com.example.ui.CafeViewModel
import java.io.File

@Composable
fun MapScreen(
    cafeViewModel: CafeViewModel,
    modifier: Modifier = Modifier
) {
    val cafes by cafeViewModel.allCafes.collectAsState()
    
    // Zoom and Pan states
    var scale by remember { mutableStateOf(1.2f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    var selectedCafeOnMap by remember { mutableStateOf<CafeEntity?>(null) }

    // Dynamically center map based on the average lat/lng of logged cafes, or fallback to San Francisco
    val mapCenter = remember(cafes) {
        if (cafes.isNotEmpty()) {
            val avgLat = cafes.map { it.latitude }.average()
            val avgLng = cafes.map { it.longitude }.average()
            Pair(avgLat, avgLng)
        } else {
            Pair(37.7749, -122.4194) // Default: San Francisco SF
        }
    }

    val centerLat = mapCenter.first
    val centerLng = mapCenter.second
    val scaleFactor = 12000f // Scaling factor from degree distance to pixels

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .statusBarsPadding()
            ) {
                Text(
                    text = "Coffee Roastery Map",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("map_screen_title")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFEADDFF)) // Beautiful soft lavender map ground
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
                // 1. Map Canvas - stylized vector roads, rivers, parks
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2
                    val cy = h / 2

                    // Draw Parks (Soft green circles & blocks)
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

                    // Draw Water Bodies (Soft bay or river)
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

                    // Draw Roads/Streets (Elegant grids of sleek cream/lavender lanes)
                    val roadColor = Color(0xFFF3EDF7)
                    val highwayColor = Color(0xFFD0BCFF)

                    // Horizontal streets
                    for (i in -4..4) {
                        val yOffset = cy + i * 200f
                        drawLine(
                            color = roadColor,
                            start = Offset(0f, yOffset),
                            end = Offset(w, yOffset),
                            strokeWidth = 6f
                        )
                    }

                    // Vertical streets
                    for (i in -4..4) {
                        val xOffset = cx + i * 200f
                        drawLine(
                            color = roadColor,
                            start = Offset(xOffset, 0f),
                            end = Offset(xOffset, h),
                            strokeWidth = 6f
                        )
                    }

                    // Main Diagonal Boulevard
                    drawLine(
                        color = highwayColor,
                        start = Offset(0f, 0f),
                        end = Offset(w, h),
                        strokeWidth = 14f
                    )
                }

                // 2. Overlaid Markers
                val wDp = 1000.dp // Reference virtual width
                val hDp = 1000.dp // Reference virtual height
                
                cafes.forEach { cafe ->
                    // Calculate linear offset relative to the centered coordinate
                    val deltaLat = cafe.latitude - centerLat
                    val deltaLng = cafe.longitude - centerLng

                    val markerX = deltaLng * scaleFactor
                    val markerY = -deltaLat * scaleFactor // Inverted vertical coordinate

                    // Render custom interactive marker box
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (markerX).dp + 180.dp, // Center offset adjustments
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

            // HUD Controls (Zoom In, Zoom Out, Reset Pan)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 70.dp), // Clear bottom sheet overlays
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

            // Pop-up Cafe Details Card on Selection
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
                        // Thumbnail
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

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFE07A5F),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${cafe.rating}.0 • Coffee: ${cafe.coffeeQualityRating}/5 • Atmosphere: ${cafe.atmosphereRating}/5",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { cafeViewModel.selectCafe(cafe) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "View details",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { selectedCafeOnMap = null }
                        ) {
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
