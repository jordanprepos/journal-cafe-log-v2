package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.auth.AuthViewModel
import com.example.data.CafeEntity
import com.example.ui.CafeViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    cafeViewModel: CafeViewModel,
    authViewModel: AuthViewModel,
    onNavigateToAddCafe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cafes by cafeViewModel.allCafes.collectAsState()
    val user by authViewModel.userState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubTab by remember { mutableIntStateOf(0) }
    
    // Stats Calculations
    val totalVisited = cafes.size
    val averageRating = if (cafes.isNotEmpty()) {
        cafes.map { it.rating }.average()
    } else 0.0
    val averageCoffee = if (cafes.isNotEmpty()) {
        cafes.map { it.coffeeQualityRating }.average()
    } else 0.0

    // Filtered Cafes
    val filteredCafes = cafes.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.notes.contains(searchQuery, ignoreCase = true) ||
        it.address.contains(searchQuery, ignoreCase = true)
    }

    var cafeToDelete by remember { mutableStateOf<CafeEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // User Profile Avatar
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(user?.photoUrl ?: "https://lh3.googleusercontent.com/a/default-user")
                                .crossfade(true)
                                .build(),
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Hello, ${user?.displayName ?: "Coffee Lover"}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = user?.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { authViewModel.logout {} },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddCafe,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .testTag("add_cafe_fab")
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.LocalCafe, contentDescription = "Log Cafe")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Cafe", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Analytics Dashboard Card Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Visited",
                    value = "$totalVisited",
                    icon = Icons.Default.Storefront,
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Avg. Experience",
                    value = String.format(Locale.US, "%.1f ★", averageRating),
                    icon = Icons.Default.Star,
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                    modifier = Modifier.weight(1.1f)
                )
                StatCard(
                    title = "Coffee Quality",
                    value = String.format(Locale.US, "%.1f ☕", averageCoffee),
                    icon = Icons.Default.Coffee,
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                    modifier = Modifier.weight(1.1f)
                )
            }

            // Sub-Tab Switcher (Cafe Feed vs Trends & Charts)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Cafe Feed", "Trends & Charts").forEachIndexed { index, title ->
                    val selected = selectedSubTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedSubTab = index }
                            .padding(vertical = 10.dp)
                            .testTag("dashboard_subtab_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (selectedSubTab == 0) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search cafes, notes, or roast locations...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("search_bar")
                )

                // Feed / Logs list
                if (filteredCafes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Coffee,
                                contentDescription = "Empty State Coffee Cup",
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No match found." else "Your Cafe Diary is empty!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) {
                                    "Try searching for a different keyword or location."
                                } else {
                                    "Tap 'Log Cafe' to capture your first coffee brewing and cafe atmosphere experience."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredCafes) { cafe ->
                            CafeLogCard(
                                cafe = cafe,
                                onClick = { cafeViewModel.selectCafe(cafe) },
                                onDelete = { cafeToDelete = cafe }
                            )
                        }
                    }
                }
            } else {
                // Trends & Charts tab
                if (cafes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = "No Trends Yet",
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Trends Available Yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Log your first cafe visit to unlock cumulative stats, average ratings, and coffee quality breakdowns over time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        item {
                            CafesVisitedChart(cafes = cafes)
                        }
                        item {
                            RatingEvolutionChart(cafes = cafes)
                        }
                        item {
                            CoffeeQualityBreakdownChart(cafes = cafes)
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    cafeToDelete?.let { cafe ->
        AlertDialog(
            onDismissRequest = { cafeToDelete = null },
            title = { Text("Delete Cafe Log") },
            text = { Text("Are you sure you want to remove the log for '${cafe.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        cafeViewModel.deleteCafe(cafe)
                        cafeToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { cafeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CafeLogCard(
    cafe: CafeEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }
    val formattedDate = remember(cafe.timestamp) { formatter.format(Date(cafe.timestamp)) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("cafe_card_${cafe.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cafe Photo Thumbnail
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (cafe.photoUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(cafe.photoUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cafe Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LocalCafe,
                        contentDescription = "Cafe icon placeholder",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = cafe.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("delete_cafe_${cafe.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete cafe log",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = cafe.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Stars display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < cafe.rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "${cafe.rating} Stars",
                            tint = if (index < cafe.rating) Color(0xFFE07A5F) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Detail Quality ratings
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RatingBadge(
                        label = "Coffee: ${cafe.coffeeQualityRating}/5",
                        icon = Icons.Default.Coffee
                    )
                    RatingBadge(
                        label = "Atmosphere: ${cafe.atmosphereRating}/5",
                        icon = Icons.Default.WbSunny
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Visited: $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun RatingBadge(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun CafesVisitedChart(
    cafes: List<CafeEntity>,
    modifier: Modifier = Modifier
) {
    val sortedCafes = remember(cafes) { cafes.sortedBy { it.timestamp } }
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Visited Over Time",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Cumulative log of unique cafes visited",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (sortedCafes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available yet.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val dates = remember(sortedCafes) {
                    val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                    sortedCafes.map { sdf.format(Date(it.timestamp)) }
                }

                val surfaceColor = MaterialTheme.colorScheme.surface

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val paddingY = 20f

                        val pointsCount = sortedCafes.size
                        val xStep = if (pointsCount > 1) width / (pointsCount - 1) else width

                        // Draw Gridlines (horizontal)
                        val gridCount = 4
                        val yGridStep = height / gridCount
                        for (i in 0..gridCount) {
                            val y = i * yGridStep
                            drawLine(
                                color = primaryColor.copy(alpha = 0.05f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 2f
                            )
                        }

                        if (pointsCount > 0) {
                            val path = Path()
                            val fillPath = Path()

                            val maxVal = pointsCount.toFloat()
                            val minVal = 0f
                            val valueRange = maxVal - minVal

                            fun getY(value: Float): Float {
                                val normalized = if (valueRange > 0) (value - minVal) / valueRange else 0.5f
                                return height - paddingY - (normalized * (height - 2 * paddingY))
                            }

                            // Build path and fill path
                            sortedCafes.forEachIndexed { index, _ ->
                                val x = index * xStep
                                val y = getY((index + 1).toFloat())

                                if (index == 0) {
                                    path.moveTo(x, y)
                                    fillPath.moveTo(x, height)
                                    fillPath.lineTo(x, y)
                                } else {
                                    // Soft cubic curve
                                    val prevX = (index - 1) * xStep
                                    val prevY = getY(index.toFloat())
                                    val cpX1 = prevX + xStep / 2f
                                    val cpY1 = prevY
                                    val cpX2 = prevX + xStep / 2f
                                    val cpY2 = y
                                    path.cubicTo(cpX1, cpY1, cpX2, cpY2, x, y)
                                    fillPath.cubicTo(cpX1, cpY1, cpX2, cpY2, x, y)
                                }

                                if (index == pointsCount - 1) {
                                    fillPath.lineTo(x, height)
                                    fillPath.close()
                                }
                            }

                            // Draw area gradient
                            val areaBrush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                            drawPath(path = fillPath, brush = areaBrush)

                            // Draw trend line
                            drawPath(
                                path = path,
                                color = primaryColor,
                                style = Stroke(width = 6f, cap = StrokeCap.Round)
                            )

                            // Draw data dots
                            sortedCafes.forEachIndexed { index, _ ->
                                val x = index * xStep
                                val y = getY((index + 1).toFloat())
                                drawCircle(
                                    color = surfaceColor,
                                    radius = 8f,
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = primaryColor,
                                    radius = 5f,
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // X-axis date indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (dates.isNotEmpty()) {
                        Text(
                            text = dates.first(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (dates.size > 2) {
                            Text(
                                text = dates[dates.size / 2],
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (dates.size > 1) {
                            Text(
                                text = dates.last(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingEvolutionChart(
    cafes: List<CafeEntity>,
    modifier: Modifier = Modifier
) {
    val sortedCafes = remember(cafes) { cafes.sortedBy { it.timestamp } }
    val secondaryColor = Color(0xFFE07A5F) // Warm coral/terracotta accent for ratings
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Experience Rating Trend",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Running average rating over time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = secondaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (sortedCafes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available yet.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val runningAverages = remember(sortedCafes) {
                    var total = 0.0
                    sortedCafes.mapIndexed { index, cafe ->
                        total += cafe.rating
                        total / (index + 1)
                    }
                }

                val dates = remember(sortedCafes) {
                    val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                    sortedCafes.map { sdf.format(Date(it.timestamp)) }
                }

                val surfaceColor = MaterialTheme.colorScheme.surface

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Y-axis ratings
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(24.dp)
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            for (r in listOf("5★", "4★", "3★", "2★", "1★")) {
                                Text(
                                    text = r,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // The Chart Area
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp)
                        ) {
                            val width = size.width
                            val height = size.height
                            val paddingY = 10f

                            val pointsCount = sortedCafes.size
                            val xStep = if (pointsCount > 1) width / (pointsCount - 1) else width

                            // Draw horizontal grid lines for 1 to 5 stars
                            for (i in 0..4) {
                                val y = paddingY + i * (height - 2 * paddingY) / 4f
                                drawLine(
                                    color = primaryColor.copy(alpha = 0.05f),
                                    start = Offset(0f, y),
                                    end = Offset(width, y),
                                    strokeWidth = 2f
                                )
                            }

                            if (pointsCount > 0) {
                                val path = Path()

                                fun getY(rating: Float): Float {
                                    // scale from 5.0 (top) down to 1.0 (bottom)
                                    val normalized = (rating - 1f) / 4f
                                    return height - paddingY - (normalized * (height - 2 * paddingY))
                                }

                                // Plot individual ratings as elegant vertical bars
                                sortedCafes.forEachIndexed { index, cafe ->
                                    val x = index * xStep
                                    val yBar = getY(cafe.rating.toFloat())
                                    drawLine(
                                        color = secondaryColor.copy(alpha = 0.15f),
                                        start = Offset(x, height - paddingY),
                                        end = Offset(x, yBar),
                                        strokeWidth = 6f,
                                        cap = StrokeCap.Round
                                    )
                                    drawCircle(
                                        color = secondaryColor.copy(alpha = 0.4f),
                                        radius = 4f,
                                        center = Offset(x, yBar)
                                    )
                                }

                                // Draw running average trend line
                                runningAverages.forEachIndexed { index, avg ->
                                    val x = index * xStep
                                    val y = getY(avg.toFloat())

                                    if (index == 0) {
                                        path.moveTo(x, y)
                                    } else {
                                        val prevX = (index - 1) * xStep
                                        val prevY = getY(runningAverages[index - 1].toFloat())
                                        val cpX1 = prevX + xStep / 2f
                                        val cpY1 = prevY
                                        val cpX2 = prevX + xStep / 2f
                                        val cpY2 = y
                                        path.cubicTo(cpX1, cpY1, cpX2, cpY2, x, y)
                                    }
                                }

                                drawPath(
                                    path = path,
                                    color = secondaryColor,
                                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                                )

                                // Draw glowing endpoint dot for average
                                if (runningAverages.isNotEmpty()) {
                                    val xEnd = (pointsCount - 1) * xStep
                                    val yEnd = getY(runningAverages.last().toFloat())
                                    drawCircle(
                                        color = secondaryColor.copy(alpha = 0.25f),
                                        radius = 12f,
                                        center = Offset(xEnd, yEnd)
                                    )
                                    drawCircle(
                                        color = surfaceColor,
                                        radius = 7f,
                                        center = Offset(xEnd, yEnd)
                                    )
                                    drawCircle(
                                        color = secondaryColor,
                                        radius = 4f,
                                        center = Offset(xEnd, yEnd)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // X-axis date indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (dates.isNotEmpty()) {
                        Text(
                            text = dates.first(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (dates.size > 2) {
                            Text(
                                text = dates[dates.size / 2],
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (dates.size > 1) {
                            Text(
                                text = dates.last(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoffeeQualityBreakdownChart(
    cafes: List<CafeEntity>,
    modifier: Modifier = Modifier
) {
    val sortedCafes = remember(cafes) { cafes.sortedBy { it.timestamp } }
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Coffee Quality Insights",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Score breakdown and chronological trend",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.LocalCafe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (cafes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available yet.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                // Section 1: Score Distribution Breakdown
                Text(
                    text = "Quality Score Distribution",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val distribution = remember(cafes) {
                    val counts = IntArray(5)
                    cafes.forEach {
                        val score = it.coffeeQualityRating.coerceIn(1, 5)
                        counts[score - 1]++
                    }
                    counts
                }
                val totalCafes = cafes.size.toFloat()

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (score in 5 downTo 1) {
                        val count = distribution[score - 1]
                        val pct = if (totalCafes > 0) count / totalCafes else 0f

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$score ★",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.width(24.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Custom animated progress bar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = pct)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary
                                                )
                                            )
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "$count (${(pct * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(48.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 2: Chronological Trend
                Text(
                    text = "Chronological Trend (Average ☕)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val runningAverages = remember(sortedCafes) {
                    var total = 0.0
                    sortedCafes.mapIndexed { index, cafe ->
                        total += cafe.coffeeQualityRating
                        total / (index + 1)
                    }
                }

                val dates = remember(sortedCafes) {
                    val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                    sortedCafes.map { sdf.format(Date(it.timestamp)) }
                }

                val surfaceColor = MaterialTheme.colorScheme.surface

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Y-axis ratings
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(24.dp)
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            for (r in listOf("5★", "3★", "1★")) {
                                Text(
                                    text = r,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // The Chart Area
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp)
                        ) {
                            val width = size.width
                            val height = size.height
                            val paddingY = 10f

                            val pointsCount = sortedCafes.size
                            val xStep = if (pointsCount > 1) width / (pointsCount - 1) else width

                            // Draw horizontal grid lines for 1, 3, 5 stars
                            for (i in 0..2) {
                                val y = paddingY + i * (height - 2 * paddingY) / 2f
                                drawLine(
                                    color = primaryColor.copy(alpha = 0.05f),
                                    start = Offset(0f, y),
                                    end = Offset(width, y),
                                    strokeWidth = 2f
                                )
                            }

                            if (pointsCount > 0) {
                                val path = Path()

                                fun getY(rating: Float): Float {
                                    val normalized = (rating - 1f) / 4f
                                    return height - paddingY - (normalized * (height - 2 * paddingY))
                                }

                                // Build trend line
                                runningAverages.forEachIndexed { index, avg ->
                                    val x = index * xStep
                                    val y = getY(avg.toFloat())

                                    if (index == 0) {
                                        path.moveTo(x, y)
                                    } else {
                                        val prevX = (index - 1) * xStep
                                        val prevY = getY(runningAverages[index - 1].toFloat())
                                        val cpX1 = prevX + xStep / 2f
                                        val cpY1 = prevY
                                        val cpX2 = prevX + xStep / 2f
                                        val cpY2 = y
                                        path.cubicTo(cpX1, cpY1, cpX2, cpY2, x, y)
                                    }
                                }

                                drawPath(
                                    path = path,
                                    color = tertiaryColor,
                                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                                )

                                // Endpoint Dot
                                if (runningAverages.isNotEmpty()) {
                                    val xEnd = (pointsCount - 1) * xStep
                                    val yEnd = getY(runningAverages.last().toFloat())
                                    drawCircle(
                                        color = tertiaryColor.copy(alpha = 0.25f),
                                        radius = 12f,
                                        center = Offset(xEnd, yEnd)
                                    )
                                    drawCircle(
                                        color = surfaceColor,
                                        radius = 7f,
                                        center = Offset(xEnd, yEnd)
                                    )
                                    drawCircle(
                                        color = tertiaryColor,
                                        radius = 4f,
                                        center = Offset(xEnd, yEnd)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // X-axis date indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (dates.isNotEmpty()) {
                        Text(
                            text = dates.first(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (dates.size > 2) {
                            Text(
                                text = dates[dates.size / 2],
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (dates.size > 1) {
                            Text(
                                text = dates.last(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}


