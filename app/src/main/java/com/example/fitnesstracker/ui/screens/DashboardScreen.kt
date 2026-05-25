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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.calculateCalories
import com.example.fitnesstracker.data.model.ActivityType
import com.example.fitnesstracker.ui.ActivityViewModel
import com.example.fitnesstracker.util.navigateMain

@Composable
fun DashboardScreen(
    viewModel: ActivityViewModel,
    navController: NavController
) {
    val activities by viewModel.activities.collectAsState()
    val todayCount by viewModel.todayCount.collectAsState()
    val statsByType by viewModel.todayStatsByType.collectAsState()
    val units by viewModel.units.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val useKm = units == "km"

    var selectedType by remember(statsByType) {
        mutableStateOf(
            statsByType.maxByOrNull { it.distanceMeters + it.durationSeconds }
                ?.takeIf { it.distanceMeters > 0f || it.durationSeconds > 0L }
                ?.type
                ?: ActivityType.RUNNING.key
        )
    }

    val selectedStat = statsByType.find { it.type == selectedType }

    val goalDistanceInUnits = selectedStat?.goal?.distanceKm?.let {
        if (useKm) it else it * 0.621371f
    } ?: 0f

    val actualDistanceInUnits = selectedStat?.let {
        if (useKm) it.distanceMeters / 1000f else it.distanceMeters / 1609f
    } ?: 0f

    val distanceProgress = if (goalDistanceInUnits > 0f)
        (actualDistanceInUnits / goalDistanceInUnits).coerceIn(0f, 1f) else 0f

    val durationProgress = selectedStat?.let {
        if (it.goal.durationMin > 0f) (it.durationSeconds / 60f) / it.goal.durationMin else 0f
    }?.coerceIn(0f, 1f) ?: 0f

    val animatedDistanceProgress by animateFloatAsState(
        targetValue = distanceProgress,
        animationSpec = tween(700),
        label = "distProgress"
    )

    val animatedDurationProgress by animateFloatAsState(
        targetValue = durationProgress,
        animationSpec = tween(700),
        label = "durProgress"
    )

    val unitLabel = if (useKm) "km" else "mi"

    val todayCalories = remember(statsByType, userProfile) {
        statsByType.sumOf { stat ->
            calculateCalories(stat.type, stat.durationSeconds, userProfile, stat.avgSpeedKmh)
        }
    }

    var showProfileDialog by remember { mutableStateOf(false) }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text(stringResource(R.string.dashboard_calories_dialog_title)) },
            text = { Text(stringResource(R.string.dashboard_calories_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showProfileDialog = false
                    navController.navigateMain("settings")

                }) {
                    Text(stringResource(R.string.dashboard_calories_dialog_go))
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text(stringResource(R.string.dashboard_calories_dialog_dismiss))
                }
            }
        )
    }

    // Main content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.dashboard_title),
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
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_today_progress),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                ActivityTypeSwitcher(
                    types = statsByType.map { it.type },
                    selectedType = selectedType,
                    onSelect = { selectedType = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GoalRing(
                        progress = animatedDistanceProgress,
                        centerValue = "%.1f".format(actualDistanceInUnits),
                        centerUnit = unitLabel,
                        label = stringResource(R.string.dashboard_goal_of, goalDistanceInUnits, unitLabel),
                        modifier = Modifier.size(130.dp)
                    )

                    GoalRing(
                        progress = animatedDurationProgress,
                        centerValue = "%d".format((selectedStat?.durationSeconds ?: 0L) / 60L),
                        centerUnit = "min",
                        label = stringResource(
                            R.string.dashboard_goal_of,
                            selectedStat?.goal?.durationMin ?: 0f,
                            "min"
                        ),
                        modifier = Modifier.size(130.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.dashboard_activities),
                value = todayCount.toString(),
                icon = Icons.Default.History,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            )

            CaloriesStatCard(
                modifier = Modifier.weight(1f),
                calories = todayCalories,
                isEstimate = !userProfile.isConfigured,
                onWarningClick = { showProfileDialog = true }
            )
        }

        Button(
            onClick = {
                navController.navigateMain("tracking")
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.dashboard_start_activity),
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.dashboard_recent_activities),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = {
                navController.navigateMain("history")
            }) {
                Text(stringResource(R.string.dashboard_see_all))
            }
        }

        if (activities.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
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
                            stringResource(R.string.dashboard_no_activities),
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
                    useKm = useKm,
                    onClick = { navController.navigate("detail/${activity.id}") }
                )
            }
        }
    }
}

// Helper composables
@Composable
private fun ActivityTypeSwitcher(
    types: List<String>,
    selectedType: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        types.forEach { type ->
            val isSelected = type == selectedType
            val activityColor = getActivityColor(type)
            val bgColor = if (isSelected) activityColor
            else MaterialTheme.colorScheme.surfaceVariant
            val iconTint = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable { onSelect(type) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = activityIcon(type),
                        contentDescription = (type),
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (isSelected) {
                    Text(
                        text = activityTypeDisplayName(type),
                        style = MaterialTheme.typography.labelSmall,
                        color = activityColor,
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
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 10.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    centerValue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    centerUnit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CaloriesStatCard(
    modifier: Modifier = Modifier,
    calories: Int,
    isEstimate: Boolean,
    onWarningClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                if (isEstimate) {
                    IconButton(
                        onClick = onWarningClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                "$calories kcal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.dashboard_calories),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}