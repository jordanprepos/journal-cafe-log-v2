package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalSharedTransitionApi::class)
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

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedCafe,
            label = "cafe_details_transition",
            modifier = Modifier.fillMaxSize()
        ) { targetCafe ->
            val cafeDetailsTransitionScope = this
            if (targetCafe == null) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.background,
                            modifier = Modifier
                                .height(64.dp)
                                .testTag("bottom_nav_bar")
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
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                            },
                            label = "tab_fade_transition",
                            modifier = Modifier.fillMaxSize()
                        ) { targetTab ->
                            when (targetTab) {
                                TabScreen.Journal -> {
                                    DashboardScreen(
                                        cafeViewModel = cafeViewModel,
                                        authViewModel = authViewModel,
                                        onNavigateToAddCafe = onNavigateToAddCafe,
                                        onShowRecap = { showRecap = true },
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = cafeDetailsTransitionScope
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
                }
            } else {
                CafeDetailsView(
                    cafe = targetCafe,
                    onDismiss = { cafeViewModel.selectCafe(null) },
                    onEditCafe = {
                        cafeViewModel.setEditingCafe(targetCafe)
                        cafeViewModel.selectCafe(null)
                        onNavigateToAddCafe()
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent
                )
            }
        }
    }

    // Year-in-Café Recap overlay dialog
    if (showRecap) {
        YearInCafeRecapDialog(
            cafes = cafes,
            onDismiss = { showRecap = false }
        )
    }
}
