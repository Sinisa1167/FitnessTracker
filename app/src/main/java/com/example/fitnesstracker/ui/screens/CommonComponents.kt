package com.example.fitnesstracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.fitnesstracker.ui.theme.detailColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun activityTypeDisplayName(type: String): String = when (type) {
    "Trčanje"    -> stringResource(R.string.type_running)
    "Hodanje"    -> stringResource(R.string.type_walking)
    "Biciklizam" -> stringResource(R.string.type_cycling)
    "Plivanje"   -> stringResource(R.string.type_swimming)
    "Ostalo"     -> stringResource(R.string.type_other)
    else         -> type
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

@Composable
fun ActivityCard(
    activity: Activity,
    useKm: Boolean = true,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelectionMode && isSelected) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(
                        imageVector        = activityIcon(activity.type),
                        contentDescription = null,
                        tint               = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(24.dp)
                    )
                }
            }

            // Name + date
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

            // Distance + duration + chevron
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatDistance(activity.distanceMeters, useKm),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        formatDuration(activity.durationSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier           = Modifier.size(20.dp)
                )
            }
        }
    }
}

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