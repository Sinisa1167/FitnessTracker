package com.example.fitnesstracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitnesstracker.data.PreferencesManager
import com.example.fitnesstracker.ui.ActivityViewModel
import com.example.fitnesstracker.ui.screens.ActivityDetailScreen
import com.example.fitnesstracker.ui.screens.DashboardScreen
import com.example.fitnesstracker.ui.screens.HistoryScreen
import com.example.fitnesstracker.ui.screens.SettingsScreen
import com.example.fitnesstracker.ui.screens.StatisticsScreen
import com.example.fitnesstracker.ui.screens.TrackingScreen
import com.example.fitnesstracker.ui.theme.FitnessTrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

val LocalAppLang = compositionLocalOf { "sr" }

private val NAV_LABELS = mapOf(
    "sr" to listOf("Početna", "Trening", "Istorija", "Statistike", "Postavke"),
    "en" to listOf("Home",    "Training", "History", "Statistics",  "Settings")
)

class MainActivity : ComponentActivity() {

    private var pendingNavRoute: String? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNavRoute = intent.getStringExtra(com.example.fitnesstracker.service.TrackingService.EXTRA_NAVIGATE_TO)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        val initialLang = runBlocking {
            PreferencesManager(applicationContext).language.first()
        }
        applyLocale(initialLang)
        pendingNavRoute = intent.getStringExtra(com.example.fitnesstracker.service.TrackingService.EXTRA_NAVIGATE_TO)

        super.onCreate(savedInstanceState)

        setContent {
            val prefs = remember { PreferencesManager(applicationContext) }
            val currentLang by prefs.language.collectAsState(initial = initialLang)

            LaunchedEffect(currentLang) {
                applyLocale(currentLang)
            }

            CompositionLocalProvider(LocalAppLang provides currentLang) {
                FitnessTrackerTheme {
                    val navController = rememberNavController()
                    val viewModel: ActivityViewModel = viewModel()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    LaunchedEffect(Unit) {
                        pendingNavRoute?.let { route ->
                            pendingNavRoute = null
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }

                    val lang = LocalAppLang.current
                    val navLabels = NAV_LABELS[lang] ?: NAV_LABELS["sr"]!!

                    val bottomNavRoutes = listOf(
                        Triple("dashboard",  Icons.Default.Home,     navLabels[0]),
                        Triple("tracking",   Icons.Default.Add,      navLabels[1]),
                        Triple("history",    Icons.Default.History,  navLabels[2]),
                        Triple("statistics", Icons.Default.BarChart, navLabels[3]),
                        Triple("settings",   Icons.Default.Settings, navLabels[4])
                    )

                    val showBottomBar = currentDestination?.route != "detail/{activityId}"

                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar {
                                    bottomNavRoutes.forEach { (route, icon, label) ->
                                        NavigationBarItem(
                                            icon     = { Icon(icon, contentDescription = label) },
                                            label    = {
                                                Text(
                                                    text     = label,
                                                    style    = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            },
                                            selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                                            onClick  = {
                                                navController.navigate(route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState    = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController    = navController,
                            startDestination = "dashboard",
                            modifier         = Modifier.padding(innerPadding)
                        ) {
                            composable("dashboard") {
                                DashboardScreen(viewModel = viewModel, navController = navController)
                            }
                            composable("tracking") {
                                TrackingScreen(viewModel = viewModel, navController = navController)
                            }
                            composable("history") {
                                HistoryScreen(viewModel = viewModel, navController = navController)
                            }
                            composable("statistics") {
                                StatisticsScreen(viewModel = viewModel)
                            }
                            composable("settings") {
                                SettingsScreen()
                            }
                            composable("detail/{activityId}") { backStackEntry ->
                                val activityId = backStackEntry.arguments
                                    ?.getString("activityId")?.toLongOrNull()
                                    ?: return@composable
                                ActivityDetailScreen(
                                    activityId    = activityId,
                                    viewModel     = viewModel,
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applyLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}