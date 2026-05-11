package com.example.fitnesstracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.fitnesstracker.ui.ActivityViewModel
import com.example.fitnesstracker.ui.TypeDayStat

@Composable
fun DashboardScreen(viewModel: ActivityViewModel, navController: NavController) {
    val activities       by viewModel.activities.collectAsState()
    val todayCount       by viewModel.todayCount.collectAsState()
    val todayDistance    by viewModel.todayDistance.collectAsState()
    val statsByType      by viewModel.todayStatsByType.collectAsState()

    var selectedType by remember { mutableStateOf("Trčanje") }

    val selectedStat = statsByType.find { it.type == selectedType }

    val distanceProgress = selectedStat?.let {
        if (it.goal.distanceKm > 0f) (it.distanceMeters / 1000f) / it.goal.distanceKm else 0f
    }?.coerceIn(0f, 1f) ?: 0f

    val durationProgress = selectedStat?.let {
        if (it.goal.durationMin > 0f) (it.durationSeconds / 60f) / it.goal.durationMin else 0f
    }?.coerceIn(0f, 1f) ?: 0f

    val animatedDistanceProgress by animateFloatAsState(
        targetValue    = distanceProgress,
        animationSpec  = tween(durationMillis = 700),
        label          = "distProgress"
    )
    val animatedDurationProgress by animateFloatAsState(
        targetValue    = durationProgress,
        animationSpec  = tween(durationMillis = 700),
        label          = "durProgress"
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
            text       = "Fitness Tracker",
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier              = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text  = "Današnji napredak",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                ActivityTypeSwitcher(
                    types        = statsByType.map { it.type },
                    selectedType = selectedType,
                    onSelect     = { selectedType = it }
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    GoalRing(
                        progress    = animatedDistanceProgress,
                        centerValue = "%.1f".format((selectedStat?.distanceMeters ?: 0f) / 1000f),
                        centerUnit  = "km",
                        label       = "od %.0f km".format(selectedStat?.goal?.distanceKm ?: 0f),
                        modifier    = Modifier.size(130.dp)
                    )

                    GoalRing(
                        progress    = animatedDurationProgress,
                        centerValue = "%d".format((selectedStat?.durationSeconds ?: 0L) / 60L),
                        centerUnit  = "min",
                        label       = "od %.0f min".format(selectedStat?.goal?.durationMin ?: 0f),
                        modifier    = Modifier.size(130.dp)
                    )
                }
            }
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier       = Modifier.weight(1f),
                title          = "Aktivnosti",
                value          = todayCount.toString(),
                icon           = Icons.Default.History,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            )
            StatCard(
                modifier       = Modifier.weight(1f),
                title          = "Kalorije",
                value          = "${(todayDistance * 0.06).toInt()} kcal",
                icon           = Icons.Default.Whatshot,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            )
        }

        Button(
            onClick = {
                navController.navigate("tracking") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState    = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = MaterialTheme.shapes.large
        ) {
            Icon(
                imageVector     = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier        = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text  = "Započni aktivnost",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp
            )
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = "Nedavne aktivnosti",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = {
                navController.navigate("history") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState    = true
                }
            }) { Text("Vidi sve") }
        }

        if (activities.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier        = Modifier.fillMaxWidth().padding(32.dp),
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
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant
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
                    onClick  = { navController.navigate("detail/${activity.id}") }
                )
            }
        }
    }
}

@Composable
private fun ActivityTypeSwitcher(
    types: List<String>,
    selectedType: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        types.forEach { type ->
            val isSelected = type == selectedType
            val bgColor    = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant

            val iconTint = if (isSelected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onSelect(type) }
            ) {
                Box(
                    modifier        = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = activityIcon(type),
                        contentDescription = type,
                        tint               = iconTint,
                        modifier           = Modifier.size(22.dp)
                    )
                }
                if (isSelected) {
                    Text(
                        text  = type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalRing(
    progress: Float,
    centerValue: String,
    centerUnit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier        = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress      = { progress },
                modifier      = Modifier.fillMaxSize(),
                strokeWidth   = 10.dp,
                trackColor    = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap     = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = centerValue,
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text  = centerUnit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        colors   = CardDefaults.cardColors(containerColor = containerColor),
        shape    = MaterialTheme.shapes.large
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(24.dp)
            )
            Text(
                text       = value,
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}