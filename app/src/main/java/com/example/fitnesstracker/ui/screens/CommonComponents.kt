package com.example.fitnesstracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitnesstracker.data.model.Activity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActivityCard(activity: Activity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = activityIcon(activity.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
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
                    "%.1f km".format(activity.distanceMeters / 1000f),
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
    "trčanje" -> Icons.Default.DirectionsRun
    "hodanje" -> Icons.Default.DirectionsWalk
    "biciklizam" -> Icons.Default.DirectionsBike
    else -> Icons.Default.FitnessCenter
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}

fun formatDistance(meters: Float, useKm: Boolean = true): String {
    return if (useKm) "%.1f km".format(meters / 1000f)
    else "%.1f mi".format(meters / 1609f)
}