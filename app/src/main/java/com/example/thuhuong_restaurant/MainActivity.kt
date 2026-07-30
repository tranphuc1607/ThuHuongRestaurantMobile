package com.example.thuhuong_restaurant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.thuhuong_restaurant.core.navigation.AppNavGraph
import com.example.thuhuong_restaurant.core.navigation.BottomNavBar
import com.example.thuhuong_restaurant.core.ui.theme.ThuHuongTheme
import com.example.thuhuong_restaurant.feature.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThuHuongTheme {
                ThuHuongAppRoot()
            }
        }
    }
}

@Composable
private fun ThuHuongAppRoot() {
    // Scoped to the Activity so every destination shares the same session state.
    val authViewModel: AuthViewModel = hiltViewModel()
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController, authViewModel) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AppNavGraph(navController = navController, authViewModel = authViewModel)
        }
    }
}
