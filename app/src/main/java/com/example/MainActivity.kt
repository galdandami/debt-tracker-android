package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.ArchiveScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtDetailScreen
import com.example.ui.theme.DebtTrackerTheme
import com.example.ui.viewmodel.DebtViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DebtViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DebtTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DebtTrackerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun DebtTrackerApp(viewModel: DebtViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToDetail = { debtId ->
                    navController.navigate("detail/$debtId")
                },
                onNavigateToArchive = {
                    navController.navigate("archive")
                }
            )
        }

        composable(
            route = "detail/{debtId}",
            arguments = listOf(navArgument("debtId") { type = NavType.LongType })
        ) { backStackEntry ->
            val debtId = backStackEntry.arguments?.getLong("debtId") ?: 0L
            DebtDetailScreen(
                debtId = debtId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("archive") {
            ArchiveScreen(
                viewModel = viewModel,
                onNavigateToDetail = { debtId ->
                    navController.navigate("detail/$debtId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
