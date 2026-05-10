package com.example.fitnesstracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitnesstracker.ui.ActivityViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: ActivityViewModel, navController: NavController) {
    val activities by viewModel.activities.collectAsState()
    val todayDistance by viewModel.todayDistance.collectAsState()
    val todayCount by viewModel.todayCount.collectAsState()

    val dailyGoalMeters = 5000f
    val progressValue = (todayDistance / dailyGoalMeters).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressValue,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Fitness Tracker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Današnji napredak",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 14.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%.2f".format(todayDistance / 1000f),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "km",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Cilj: %.1f km".format(dailyGoalMeters / 1000f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Aktivnosti",
                value = todayCount.toString(),
                icon = Icons.Default.History,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Kalorije",
                value = "${(todayDistance * 0.06).toInt()} kcal",
                icon = Icons.Default.Whatshot,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            )
        }

        Button(
            onClick = {
                navController.navigate("tracking") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Zapocni aktivnost", style = MaterialTheme.typography.titleMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nedavne aktivnosti",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = {
                navController.navigate("history") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }) {
                Text("Vidi sve")
            }
        }

        if (activities.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Nema zabilježenih aktivnosti",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            activities.take(3).forEach { activity ->
                ActivityCard(
                    activity = activity,
                    onClick = { navController.navigate("detail/${activity.id}") }
                )
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}