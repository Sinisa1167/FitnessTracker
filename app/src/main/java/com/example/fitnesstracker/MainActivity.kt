package com.example.fitnesstracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.fitnesstracker.data.PreferencesManager
import com.example.fitnesstracker.service.TrackingService
import com.example.fitnesstracker.ui.ActivityViewModel
import com.example.fitnesstracker.ui.screens.*
import com.example.fitnesstracker.ui.theme.FitnessTrackerTheme
import com.example.fitnesstracker.util.navigateMain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

val LocalAppLang = compositionLocalOf { "sr" }

class MainActivity : ComponentActivity() {

    private val pendingNavRoute = MutableStateFlow<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(TrackingService.EXTRA_NAVIGATE_TO)?.let {
            pendingNavRoute.value = it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }

        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val initialLang = PreferencesManager(applicationContext).language.first()
            applyLocale(initialLang)

            intent.getStringExtra(TrackingService.EXTRA_NAVIGATE_TO)?.let {
                pendingNavRoute.value = it
            }

            isReady = true

            setContent {
                val prefs = remember { PreferencesManager(applicationContext) }
                val currentLang by prefs.language.collectAsState(initial = initialLang)

                LaunchedEffect(currentLang) { applyLocale(currentLang) }

                CompositionLocalProvider(LocalAppLang provides currentLang) {
                    FitnessTrackerTheme {
                        val navController = rememberNavController()
                        val viewModel: ActivityViewModel = viewModel()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        // Kolekcija pending rute — radi i za cold start i za onNewIntent
                        val routeToNavigate by pendingNavRoute.collectAsState()
                        LaunchedEffect(routeToNavigate) {
                            routeToNavigate?.let { route ->
                                pendingNavRoute.value = null
                                navController.navigateMain(route)
                            }
                        }

                        val navLabels = remember(currentLang) {
                            if (currentLang == "en")
                                listOf("Home", "Training", "History", "Statistics", "Settings")
                            else
                                listOf("Početna", "Trening", "Istorija", "Statistike", "Podešavanja")
                        }

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
                                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
                                composable("dashboard")  { DashboardScreen(viewModel, navController) }
                                composable("tracking")   { TrackingScreen(viewModel, navController) }
                                composable("history")    { HistoryScreen(viewModel, navController) }
                                composable("statistics") { StatisticsScreen(viewModel) }
                                composable("settings")   { SettingsScreen(viewModel) }
                                composable("detail/{activityId}") { backStackEntry ->
                                    val activityId = backStackEntry.arguments
                                        ?.getString("activityId")?.toLongOrNull() ?: return@composable
                                    ActivityDetailScreen(activityId, viewModel, navController)
                                }
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