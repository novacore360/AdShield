package com.adshield.detector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.adshield.detector.ui.AdShieldViewModel
import com.adshield.detector.ui.screens.AppDetailScreen
import com.adshield.detector.ui.screens.DashboardScreen
import com.adshield.detector.ui.screens.SettingsScreen
import com.adshield.detector.ui.screens.StatisticsScreen
import com.adshield.detector.ui.screens.SuspectsScreen
import com.adshield.detector.ui.theme.AdShieldTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AdShieldViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdShieldTheme {
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    viewModel.refreshAccessibilityStatus()
                    viewModel.refreshPermissionScan()
                }

                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onOpenStatistics = { navController.navigate("statistics") },
                            onOpenSuspects = { navController.navigate("suspects") },
                            onOpenSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("statistics") {
                        StatisticsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onOpenApp = { pkg -> navController.navigate("app/$pkg") }
                        )
                    }
                    composable("suspects") {
                        SuspectsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onOpenApp = { pkg -> navController.navigate("app/$pkg") }
                        )
                    }
                    composable(
                        "app/{packageName}",
                        arguments = listOf(navArgument("packageName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val pkg = backStackEntry.arguments?.getString("packageName") ?: ""
                        AppDetailScreen(
                            packageName = pkg,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAccessibilityStatus()
        viewModel.refreshPermissionScan()
    }
}
