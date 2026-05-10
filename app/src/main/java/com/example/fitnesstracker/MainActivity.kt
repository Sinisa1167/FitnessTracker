package com.example.fitnesstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitnesstracker.ui.ActivityViewModel
import com.example.fitnesstracker.ui.screens.DashboardScreen
import com.example.fitnesstracker.ui.screens.HistoryScreen
import com.example.fitnesstracker.ui.screens.StatisticsScreen
import com.example.fitnesstracker.ui.screens.SettingsScreen
import com.example.fitnesstracker.ui.screens.TrackingScreen
import com.example.fitnesstracker.ui.screens.ActivityDetailScreen
import com.example.fitnesstracker.ui.theme.FitnessTrackerTheme
import com.example.fitnesstracker.worker.ReminderWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderWorker.schedule(this)
        setContent {
            FitnessTrackerTheme {
                val navController = rememberNavController()
                val viewModel: ActivityViewModel = viewModel()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val bottomNavItems = listOf(
                    Triple("dashboard", Icons.Default.Home, "Početna"),
                    Triple("tracking", Icons.Default.Add, "Trening"),
                    Triple("history", Icons.Default.History, "Historija"),
                    Triple("statistics", Icons.Default.BarChart, "Statistika"),
                    Triple("settings", Icons.Default.Settings, "Podešavanja")
                )

                val showBottomBar = currentDestination?.route != "detail/{activityId}"

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                bottomNavItems.forEach { (route, icon, label) ->
                                    NavigationBarItem(
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label) },
                                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                                        onClick = {
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding)
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
                            val activityId = backStackEntry.arguments?.getString("activityId")?.toLongOrNull() ?: return@composable
                            ActivityDetailScreen(
                                activityId = activityId,
                                viewModel = viewModel,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}