package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.auth.AuthViewModel
import com.example.data.CafeDatabase
import com.example.data.CafeRepository
import com.example.ui.CafeViewModel
import com.example.ui.screens.AddCafeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Room Data Layer
        val database = CafeDatabase.getDatabase(applicationContext)
        val repository = CafeRepository(database.cafeDao())

        // 2. Initialize ViewModels manually via factories (no heavy DI frameworks required)
        val authViewModel = AuthViewModel(applicationContext)
        val cafeViewModel = CafeViewModel(repository)
        cafeViewModel.loadThemePreference(applicationContext)

        setContent {
            val isDarkTheme by cafeViewModel.isDarkTheme.collectAsState()
            val useDarkTheme = isDarkTheme ?: androidx.compose.foundation.isSystemInDarkTheme()

            MyApplicationTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        authViewModel = authViewModel,
                        cafeViewModel = cafeViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    cafeViewModel: CafeViewModel
) {
    val navController = rememberNavController()
    val user by authViewModel.userState.collectAsState()

    // Dynamically react to authentication changes safely
    LaunchedEffect(user) {
        val currentRoute = navController.currentDestination?.route
        if (currentRoute != null) {
            if (user == null && currentRoute != "login") {
                navController.navigate("login") {
                    popUpTo(currentRoute) { inclusive = true }
                }
            } else if (user != null && currentRoute != "main") {
                navController.navigate("main") {
                    popUpTo(currentRoute) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (user == null) "login" else "main",
        enterTransition = { fadeIn(animationSpec = tween(400)) },
        exitTransition = { fadeOut(animationSpec = tween(400)) },
        popEnterTransition = { fadeIn(animationSpec = tween(400)) },
        popExitTransition = { fadeOut(animationSpec = tween(400)) }
    ) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainScreen(
                cafeViewModel = cafeViewModel,
                authViewModel = authViewModel,
                onNavigateToAddCafe = {
                    navController.navigate("add_cafe")
                }
            )
        }

        composable("add_cafe") {
            AddCafeScreen(
                cafeViewModel = cafeViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
