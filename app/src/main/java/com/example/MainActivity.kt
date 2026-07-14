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

        setContent {
            MyApplicationTheme {
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

    // Dynamically react to authentication changes
    LaunchedEffect(user) {
        if (user == null) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate("main") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (user == null) "login" else "main"
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
