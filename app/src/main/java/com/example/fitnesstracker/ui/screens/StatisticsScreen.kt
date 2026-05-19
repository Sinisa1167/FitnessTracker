package com.example.fitnesstracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.LocalAppLang
import com.example.fitnesstracker.R
import com.example.fitnesstracker.ui.ActivityViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable

@Composable
fun StatisticsScreen(viewModel: ActivityViewModel) {
    val activities by viewModel.activities.collectAsState()
    val units      by viewModel.units.collectAsState()
    val lang        = LocalAppLang.current
    val useKm       = units == "km"
    val unitLabel   = if (useKm) "km" else "mi"
    val divisor     = if (useKm) 1000f else 1609f

    val locale = remember(lang) {
        if (lang == "sr") Locale.forLanguageTag("sr-Latn") else Locale(lang)
    }

    val last7Days = remember(locale) { getLast7Days(locale) }

    val statsData = remember(activities, units, locale) {
        val labels  = last7Days.map { it.third }
        val entries = last7Days.mapIndexed { index, triple ->
            val dailySum = activities
                .filter { it.timestamp in triple.first..triple.second }
                .sumOf { it.distanceMeters.toDouble() } / divisor
            entryOf(index, dailySum.toFloat())
        }
        Pair(labels, entries)
    }

    val modelProducerBar = remember(statsData.second) {
        ChartEntryModelProducer(statsData.second)
    }

    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    val selectedDayLabel = selectedBarIndex?.let { statsData.first.getOrNull(it) }
    val selectedDayValue = selectedBarIndex?.let { statsData.second.getOrNull(it)?.y }
    val selectedDayActivities = remember(selectedBarIndex, activities, last7Days) {
        selectedBarIndex?.let { idx ->
            val (start, end, _) = last7Days[idx]
            activities.filter { it.timestamp in start..end }
        }
    }

    val totalDistance = activities.sumOf { it.distanceMeters.toDouble() } / divisor
    val primaryColor  = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text       = stringResource(R.string.stats_title),
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color      = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(28.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier              = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                StatDetailItem(
                    label = stringResource(R.string.stats_activities),
                    value = activities.size.toString(),
                    icon  = Icons.Default.History
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                StatDetailItem(
                    label = stringResource(R.string.stats_total, unitLabel),
                    value = "%.1f".format(totalDistance),
                    icon  = Icons.Default.Route
                )
            }
        }

        StatSectionTitle(stringResource(R.string.stats_last7days))

        if (activities.isEmpty()) {
            EmptyChart()
        } else {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        Chart(
                            chart = columnChart(
                                columns = listOf(
                                    LineComponent(
                                        color       = primaryColor.hashCode(),
                                        thicknessDp = 12f,
                                        shape       = Shapes.roundedCornerShape(allPercent = 40)
                                    )
                                )
                            ),
                            chartModelProducer = modelProducerBar,
                            startAxis          = rememberStartAxis(),
                            bottomAxis         = rememberBottomAxis(
                                valueFormatter = { value, _ ->
                                    statsData.first.getOrNull(value.toInt()) ?: ""
                                }
                            ),
                            modifier = Modifier.fillMaxSize()
                        )

                        Row(modifier = Modifier.fillMaxSize()) {
                            Spacer(modifier = Modifier.width(48.dp))
                            statsData.first.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            selectedBarIndex = if (selectedBarIndex == index) null else index
                                        }
                                )
                            }
                        }
                    }

                    if (selectedBarIndex != null && selectedDayActivities != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(16.dp),
                            colors   = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier            = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(
                                        selectedDayLabel ?: "",
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "%.2f $unitLabel".format(selectedDayValue ?: 0f),
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color      = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (selectedDayActivities.isEmpty()) {
                                    Text(
                                        stringResource(R.string.stats_no_activities),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    selectedDayActivities.forEach { activity ->
                                        Row(
                                            modifier              = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment     = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier         = Modifier
                                                    .size(32.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector        = activityIcon(activity.type),
                                                    contentDescription = null,
                                                    tint               = MaterialTheme.colorScheme.primary,
                                                    modifier           = Modifier.size(18.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    activityTypeDisplayName(activity.type),
                                                    style      = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    formatDate(activity.timestamp).split(" ")[1],
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    formatDistance(activity.distanceMeters, useKm),
                                                    style      = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color      = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    formatDuration(activity.durationSeconds),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        StatSectionTitle(stringResource(R.string.stats_by_type))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(24.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val activitiesByType = activities.groupBy { it.type }
                if (activitiesByType.isEmpty()) {
                    Text(
                        text     = stringResource(R.string.stats_no_activities),
                        style    = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    activitiesByType.entries.forEachIndexed { idx, (type, list) ->
                        TypeRow(
                            type     = type,
                            count    = list.size,
                            color    = getActivityColor(type),
                            distance = list.sumOf { it.distanceMeters.toDouble() } / divisor,
                            unit     = unitLabel
                        )
                        if (idx < activitiesByType.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun StatDetailItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(24.dp)
        )
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StatSectionTitle(title: String) {
    Text(
        text       = title,
        style      = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier   = Modifier.padding(start = 4.dp)
    )
}

@Composable
fun TypeRow(type: String, count: Int, color: Color, distance: Double, unit: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = color.copy(alpha = 0.2f), shape = CircleShape) {
                Icon(
                    imageVector        = activityIcon(type),
                    contentDescription = null,
                    modifier           = Modifier.padding(8.dp).size(20.dp),
                    tint               = color
                )
            }
            Column {
                Text(
                    activityTypeDisplayName(type),
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(R.string.stats_activity_count, count),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            "%.1f $unit".format(distance),
            fontWeight = FontWeight.Bold,
            style      = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun EmptyChart() {
    Box(
        modifier         = Modifier.fillMaxWidth().height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = stringResource(R.string.stats_no_data),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun getLast7Days(locale: Locale = Locale.getDefault()): List<Triple<Long, Long, String>> {
    val result    = mutableListOf<Triple<Long, Long, String>>()
    val calendar  = Calendar.getInstance()
    val dayFormat = SimpleDateFormat("EEE", locale)

    repeat(7) {
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        val dayName = dayFormat.format(calendar.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

        result.add(Triple(start, end, dayName))
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }

    return result.reversed()
}