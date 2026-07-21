package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CafeEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun YearInCafeRecapDialog(
    cafes: List<CafeEntity>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Compute dynamic stats from the logged cafes
    val totalVisited = cafes.size
    val avgRating = if (cafes.isNotEmpty()) cafes.map { it.rating }.average() else 0.0

    // Compute top drink
    val topDrink = remember(cafes) {
        val drinks = cafes.mapNotNull { it.favoriteDrink?.trim() }.filter { it.isNotBlank() }
        if (drinks.isEmpty()) "Oat latte"
        else drinks.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "Oat latte"
    }
    val topDrinkCount = remember(cafes, topDrink) {
        cafes.count { it.favoriteDrink?.trim()?.equals(topDrink, ignoreCase = true) == true }.coerceAtLeast(1)
    }

    // Compute busiest month
    val topMonthName = remember(cafes) {
        val months = cafes.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "October"
        }
        if (months.isEmpty()) "October"
        else months.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "October"
    }
    val topMonthCount = remember(cafes, topMonthName) {
        cafes.count {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) == topMonthName
        }.coerceAtLeast(1)
    }

    // Compute top rated cafe for the polaroid display
    val topCafe = remember(cafes) {
        cafes.maxByOrNull { it.rating }
    }
    val topCafeName = topCafe?.name ?: "Blue Bottle"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1D1613) // Dark Roasted Espresso Scrapbook Background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Top close action row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2025 WRAPPED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFE57F58) // Terracotta Gold
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_recap_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close recap",
                            tint = Color(0xFFECE0DB)
                        )
                    }
                }

                // Two column layout on wide screens, or vertical stack on standard mobile
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Column: Text Stats
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Your year\nin café",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 44.sp
                            ),
                            color = Color(0xFFECE0DB)
                        )

                        // Visited
                        Column {
                            Text(
                                text = "$totalVisited",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                ),
                                color = Color(0xFFE57F58)
                            )
                            Text(
                                text = "cafés visited",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFECE0DB).copy(alpha = 0.7f)
                            )
                        }

                        // Average Rating
                        Column {
                            Text(
                                text = String.format(Locale.US, "%.1f", avgRating),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                ),
                                color = Color(0xFFE57F58)
                            )
                            Text(
                                text = "average rating",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFECE0DB).copy(alpha = 0.7f)
                            )
                        }

                        // Favorite Drink
                        Column {
                            Text(
                                text = topDrink,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                ),
                                color = Color(0xFFE57F58)
                            )
                            Text(
                                text = "drink of the year — ordered $topDrinkCount times",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFECE0DB).copy(alpha = 0.7f)
                            )
                        }

                        // Busiest Month
                        Column {
                            Text(
                                text = topMonthName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                ),
                                color = Color(0xFFE57F58)
                            )
                            Text(
                                text = "busiest month — $topMonthCount visits",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFECE0DB).copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Right Column: Polaroid Image card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .width(200.dp)
                                .rotate(4f)
                                .border(1.dp, Color(0xFF3E332E), RoundedCornerShape(12.dp))
                                .testTag("recap_polaroid_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2ECE4))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Diagonal striped vintage sketch box placeholder representing the top café
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE8DFD4))
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val strokeWidth = 3f
                                        val spacing = 20f
                                        // Draw diagonal lines
                                        var x = -size.height
                                        while (x < size.width) {
                                            drawLine(
                                                color = Color(0xFFD4C3B1),
                                                start = Offset(x, 0f),
                                                end = Offset(x + size.height, size.height),
                                                strokeWidth = strokeWidth
                                            )
                                            x += spacing
                                        }
                                    }
                                }

                                Text(
                                    text = "Top café • $topCafeName",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF2E221C)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { /* Share simulated callback */ },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("recap_share_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE57F58),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("recap_next_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFECE0DB)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFECE0DB).copy(alpha = 0.4f))
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
