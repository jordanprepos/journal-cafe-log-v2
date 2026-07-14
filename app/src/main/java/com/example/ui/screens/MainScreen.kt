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

sealed class TabScreen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Feed : TabScreen("feed", "Cafe Feed", Icons.Default.ListAlt)
    object Map : TabScreen("map", "Cafe Map", Icons.Default.Map)
}

@Composable
fun MainScreen(
    cafeViewModel: CafeViewModel,
    authViewModel: AuthViewModel,
    onNavigateToAddCafe: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf<TabScreen>(TabScreen.Feed) }
    val selectedCafe by cafeViewModel.selectedCafe.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                val items = listOf(TabScreen.Feed, TabScreen.Map)
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
                TabScreen.Feed -> {
                    DashboardScreen(
                        cafeViewModel = cafeViewModel,
                        authViewModel = authViewModel,
                        onNavigateToAddCafe = onNavigateToAddCafe
                    )
                }
                TabScreen.Map -> {
                    MapScreen(
                        cafeViewModel = cafeViewModel
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
}
