package com.example.fitnesstracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.model.Activity
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.fitnesstracker.data.model.ActivityType


@Composable
fun activityTypeDisplayName(type: String): String = when (ActivityType.fromKey(type)) {
    ActivityType.RUNNING  -> stringResource(R.string.type_running)
    ActivityType.WALKING  -> stringResource(R.string.type_walking)
    ActivityType.CYCLING  -> stringResource(R.string.type_cycling)
    ActivityType.SWIMMING -> stringResource(R.string.type_swimming)
    ActivityType.HIKING   -> stringResource(R.string.type_hiking)
    ActivityType.OTHER    -> stringResource(R.string.type_other)
}

fun activityIcon(type: String) = when (ActivityType.fromKey(type)) {
    ActivityType.RUNNING  -> Icons.AutoMirrored.Filled.DirectionsRun
    ActivityType.WALKING  -> Icons.AutoMirrored.Filled.DirectionsWalk
    ActivityType.SWIMMING -> Icons.Default.Pool
    ActivityType.CYCLING  -> Icons.AutoMirrored.Filled.DirectionsBike
    ActivityType.HIKING   -> Icons.Default.Terrain
    ActivityType.OTHER    -> Icons.Default.FitnessCenter
}

fun getActivityColor(type: String) = when (ActivityType.fromKey(type)) {
    ActivityType.RUNNING  -> Color(0xFF2196F3)
    ActivityType.WALKING  -> Color(0xFF4CAF50)
    ActivityType.CYCLING  -> Color(0xFFFF9800)
    ActivityType.SWIMMING -> Color(0xFF00BCD4)
    ActivityType.HIKING   -> Color(0xFF795548)
    ActivityType.OTHER    -> Color(0xFF9E9E9E)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActivityCard(
    activity: Activity,
    useKm: Boolean = true,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val activityColor = getActivityColor(activity.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else activityColor.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelectionMode && isSelected) {
                    Icon(Icons.Default.Check, null, tint = Color.White)
                } else {
                    Icon(
                        imageVector        = activityIcon(activity.type),
                        contentDescription = null,
                        tint               = if (isSelected) Color.White else activityColor,
                        modifier           = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    activityTypeDisplayName(activity.type),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatDate(activity.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatDistance(activity.distanceMeters, useKm),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else activityColor
                    )
                    Text(
                        formatDuration(activity.durationSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isSelectionMode) {
                    Icon(
                        imageVector        = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    title: String,
    isActive: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                content()
            }
        }
    }
}

private val dateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
fun formatDate(timestamp: Long): String = dateFormatter.format(Date(timestamp))

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun formatDistance(meters: Float, useKm: Boolean = true): String =
    if (useKm) "%.1f km".format(meters / 1000f)
    else       "%.1f mi".format(meters / 1609f)

fun formatSpeed(kmh: Float, useKm: Boolean = true): String =
    if (useKm) "%.1f km/h".format(kmh)
    else       "%.1f mph".format(kmh * 0.621371f)

fun formatPace(avgSpeedKmh: Float, useKm: Boolean): String {
    if (avgSpeedKmh < 0.5f) return "--:--"
    val speedKmh = if (useKm) avgSpeedKmh else avgSpeedKmh * 0.621371f
    val paceSeconds = (60f / speedKmh * 60f).toInt()
    val minutes = paceSeconds / 60
    val seconds = paceSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatSwimPace(avgSpeedKmh: Float): String {
    if (avgSpeedKmh < 0.1f) return "--:--"
    val speedMs = avgSpeedKmh / 3.6f
    val secondsPer100m = (100f / speedMs).toInt()
    val minutes = secondsPer100m / 60
    val seconds = secondsPer100m % 60
    return "%d:%02d".format(minutes, seconds)
}