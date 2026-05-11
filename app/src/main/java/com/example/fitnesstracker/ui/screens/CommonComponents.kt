package com.example.fitnesstracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fitnesstracker.data.model.Activity
import com.example.fitnesstracker.ui.theme.detailColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActivityCard(activity: Activity, useKm: Boolean = true, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = activityIcon(activity.type),
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(activity.type, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatDate(activity.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatDistance(activity.distanceMeters, useKm),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    formatDuration(activity.durationSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun activityIcon(type: String) = when (type.lowercase()) {
    "trčanje"    -> Icons.AutoMirrored.Filled.DirectionsRun
    "hodanje"    -> Icons.AutoMirrored.Filled.DirectionsWalk
    "plivanje"   -> Icons.Default.Pool
    "biciklizam" -> Icons.AutoMirrored.Filled.DirectionsBike
    else         -> Icons.Default.FitnessCenter
}

@Composable
fun getActivityColor(type: String): Color = detailColor

fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

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