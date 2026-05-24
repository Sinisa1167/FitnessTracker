package com.example.fitnesstracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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

                        val routeToNavigate by pendingNavRoute.collectAsState()
                        LaunchedEffect(routeToNavigate) {
                            routeToNavigate?.let { route ->
                                pendingNavRoute.value = null
                                navController.navigateMain(route)
                            }
                        }

                        val navLabels = remember(currentLang) {
                            if (currentLang == "en")
                                listOf("Home", "Statistics", "History", "Settings")
                            else
                                listOf("Početna", "Statistika", "Istorija", "Podešavanja")
                        }

                        val showBottomBar = currentDestination?.route != "detail/{activityId}"
                        val isTrackingActive = currentDestination?.hierarchy?.any { it.route == "tracking" } == true

                        Scaffold(
                            bottomBar = {
                                if (showBottomBar) {
                                    Box {
                                        NavigationBar {
                                            NavigationBarItem(
                                                icon     = { Icon(Icons.Default.Home, null) },
                                                label    = { Text(navLabels[0], style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                                                selected = currentDestination?.hierarchy?.any { it.route == "dashboard" } == true,
                                                onClick  = {
                                                    navController.navigate("dashboard") {
                                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState    = true
                                                    }
                                                }
                                            )
                                            NavigationBarItem(
                                                icon     = { Icon(Icons.Default.BarChart, null) },
                                                label    = { Text(navLabels[1], style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                                                selected = currentDestination?.hierarchy?.any { it.route == "statistics" } == true,
                                                onClick  = {
                                                    navController.navigate("statistics") {
                                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState    = true
                                                    }
                                                }
                                            )
                                            NavigationBarItem(
                                                icon = {}, label = {}, selected = false, onClick = {}, enabled = false
                                            )
                                            NavigationBarItem(
                                                icon     = { Icon(Icons.Default.History, null) },
                                                label    = { Text(navLabels[2], style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                                                selected = currentDestination?.hierarchy?.any { it.route == "history" } == true,
                                                onClick  = {
                                                    navController.navigate("history") {
                                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState    = true
                                                    }
                                                }
                                            )
                                            NavigationBarItem(
                                                icon     = { Icon(Icons.Default.Settings, null) },
                                                label    = { Text(navLabels[3], style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                                                selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                                                onClick  = {
                                                    navController.navigate("settings") {
                                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState    = true
                                                    }
                                                }
                                            )
                                        }

                                        FloatingActionButton(
                                            onClick = {
                                                navController.navigate("tracking") {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState    = true
                                                }
                                            },
                                            modifier       = Modifier
                                                .align(Alignment.Center).size(60.dp),
                                            containerColor = if (isTrackingActive) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.primaryContainer,
                                            contentColor   = if (isTrackingActive) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onPrimaryContainer,
                                            shape          = CircleShape,
                                            elevation      = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector        = Icons.Default.PlayArrow,
                                                contentDescription = "Trening",
                                                modifier           = Modifier.size(34.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController    = navController,
                                startDestination = "dashboard",
                                modifier         = Modifier.padding(innerPadding),
                                enterTransition  = { EnterTransition.None },
                                exitTransition   = { ExitTransition.None }
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