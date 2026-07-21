package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.auth.AuthViewModel
import com.example.ui.CafeViewModel

import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person

sealed class TabScreen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Journal : TabScreen("journal", "Journal", Icons.Default.ListAlt)
    object Stats : TabScreen("stats", "Stats", Icons.Default.BarChart)
    object Places : TabScreen("places", "Places", Icons.Default.Map)
    object Profile : TabScreen("profile", "Profile", Icons.Default.Person)
}

@Composable
fun MainScreen(
    cafeViewModel: CafeViewModel,
    authViewModel: AuthViewModel,
    onNavigateToAddCafe: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf<TabScreen>(TabScreen.Journal) }
    val selectedCafe by cafeViewModel.selectedCafe.collectAsState()
    val cafes by cafeViewModel.allCafes.collectAsState()
    var showRecap by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                val items = listOf(TabScreen.Journal, TabScreen.Stats, TabScreen.Places, TabScreen.Profile)
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentTab.route == screen.route,
                        onClick = { currentTab = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                TabScreen.Journal -> {
                    DashboardScreen(
                        cafeViewModel = cafeViewModel,
                        authViewModel = authViewModel,
                        onNavigateToAddCafe = onNavigateToAddCafe,
                        onShowRecap = { showRecap = true }
                    )
                }
                TabScreen.Stats -> {
                    StatsScreen(
                        cafeViewModel = cafeViewModel
                    )
                }
                TabScreen.Places -> {
                    PlacesScreen(
                        cafeViewModel = cafeViewModel
                    )
                }
                TabScreen.Profile -> {
                    ProfileScreen(
                        authViewModel = authViewModel,
                        cafeViewModel = cafeViewModel,
                        onShowRecap = { showRecap = true }
                    )
                }
            }
        }
    }

    // Universal details overlay overlaying any tab screen
    selectedCafe?.let { cafe ->
        CafeDetailsDialog(
            cafe = cafe,
            onDismiss = { cafeViewModel.selectCafe(null) }
        )
    }

    // Year-in-Café Recap overlay dialog
    if (showRecap) {
        YearInCafeRecapDialog(
            cafes = cafes,
            onDismiss = { showRecap = false }
        )
    }
}
