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

enum class StatsPeriod { WEEK, MONTH, THREE_MONTHS}

@Composable
fun StatisticsScreen(viewModel: ActivityViewModel) {
    val activities by viewModel.activities.collectAsState()
    val units      by viewModel.units.collectAsState()
    val lang        = LocalAppLang.current
    val useKm       = units == "km"
    val unitLabel   = if (useKm) "km" else "mi"
    val divisor     = if (useKm) 1000f else 1609f

    var selectedPeriod by remember { mutableStateOf(StatsPeriod.WEEK) }

    val locale = remember(lang) {
        if (lang == "sr") Locale.forLanguageTag("sr-Latn") else Locale(lang)
    }

    val filteredActivities = remember(activities, selectedPeriod) {
        val now = System.currentTimeMillis()
        val from = when (selectedPeriod) {
            StatsPeriod.WEEK         -> now - 7L  * 24 * 60 * 60 * 1000
            StatsPeriod.MONTH        -> now - 30L * 24 * 60 * 60 * 1000
            StatsPeriod.THREE_MONTHS -> now - 90L * 24 * 60 * 60 * 1000
        }
        activities.filter { it.timestamp >= from }
    }

    val chartDays = remember(selectedPeriod, locale) {
        when (selectedPeriod) {
            StatsPeriod.WEEK         -> getLastNDays(7, locale)
            StatsPeriod.MONTH        -> getLastNDays(30, locale)
            StatsPeriod.THREE_MONTHS -> getLastNDays(90, locale)
        }
    }

    val statsData = remember(filteredActivities, units, chartDays) {
        val labels = chartDays.map { it.third }
        val entries = chartDays.mapIndexed { index, triple ->
            val dailySum = filteredActivities
                .filter { it.timestamp in triple.first..triple.second }
                .sumOf { it.distanceMeters.toDouble() } / divisor
            entryOf(index, dailySum.toFloat())
        }
        Pair(labels, entries)
    }

    val modelProducerBar = remember { ChartEntryModelProducer(statsData.second) }

    LaunchedEffect(statsData.second) {
        modelProducerBar.setEntries(statsData.second)
    }

    var selectedBarIndex by remember(selectedPeriod) { mutableStateOf<Int?>(null) }

    val selectedDayLabel = selectedBarIndex?.let { statsData.first.getOrNull(it) }
    val selectedDayValue = selectedBarIndex?.let { statsData.second.getOrNull(it)?.y }
    val selectedDayActivities = remember(selectedBarIndex, filteredActivities, chartDays) {
        selectedBarIndex?.let { idx ->
            val (start, end, _) = chartDays[idx]
            filteredActivities.filter { it.timestamp in start..end }
        }
    }

    val totalDistance        = filteredActivities.sumOf { it.distanceMeters.toDouble() } / divisor
    val totalDurationSeconds = filteredActivities.sumOf { it.durationSeconds }
    val totalCalories        = filteredActivities.sumOf { it.caloriesBurned }
    val primaryColor         = MaterialTheme.colorScheme.primary

    val barThickness = when (selectedPeriod) {
        StatsPeriod.WEEK         -> 12f
        StatsPeriod.MONTH        -> 3f
        StatsPeriod.THREE_MONTHS -> 1.5f
    }

    val barSpacing = when (selectedPeriod) {
        StatsPeriod.WEEK         -> 4.dp
        StatsPeriod.MONTH        -> 1.dp
        StatsPeriod.THREE_MONTHS -> 0.5.dp
    }

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

        // Period switcher
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsPeriod.entries.forEach { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick  = { selectedPeriod = period },
                    label    = {
                        Text(
                            periodLabel(period, lang),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Summary kartica
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(28.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    StatDetailItem(
                        label = stringResource(R.string.stats_activities),
                        value = filteredActivities.size.toString(),
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
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    StatDetailItem(
                        label = stringResource(R.string.stats_total_duration),
                        value = formatDurationShort(totalDurationSeconds),
                        icon  = Icons.Default.Timer
                    )
                }

                if (totalCalories > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        StatDetailItem(
                            label = stringResource(R.string.stats_total_calories),
                            value = "$totalCalories kcal",
                            icon  = Icons.Default.Whatshot
                        )
                    }
                }
            }
        }

        // Grafikon naslov
        StatSectionTitle(
            when (selectedPeriod) {
                StatsPeriod.WEEK         -> stringResource(R.string.stats_last7days)
                StatsPeriod.MONTH        -> stringResource(R.string.stats_last30days)
                StatsPeriod.THREE_MONTHS -> stringResource(R.string.stats_last90days)
            }
        )

        if (filteredActivities.isEmpty()) {
            EmptyChart()
        } else {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        Chart(
                            chart = columnChart(
                                columns = listOf(
                                    LineComponent(
                                        color       = primaryColor.hashCode(),
                                        thicknessDp = barThickness,
                                        shape       = Shapes.roundedCornerShape(allPercent = 40)
                                    )
                                ),
                                spacing = barSpacing
                            ),
                            chartModelProducer = modelProducerBar,
                            startAxis          = rememberStartAxis(),
                            bottomAxis         = rememberBottomAxis(
                                valueFormatter = { value, _ ->
                                    when (selectedPeriod) {
                                        StatsPeriod.WEEK -> statsData.first.getOrNull(value.toInt()) ?: ""
                                        else -> if (value.toInt() % 7 == 0)
                                            statsData.first.getOrNull(value.toInt()) ?: ""
                                        else ""
                                    }
                                }
                            ),
                            modifier = Modifier.fillMaxSize()
                        )

                        // Click overlay
                        Row(modifier = Modifier.fillMaxSize()) {
                            Spacer(modifier = Modifier.width(48.dp))
                            statsData.first.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            selectedBarIndex =
                                                if (selectedBarIndex == index) null else index
                                        }
                                )
                            }
                        }
                    }

                    // Detalji odabranog dana
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
                                                        getActivityColor(activity.type).copy(alpha = 0.1f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector        = activityIcon(activity.type),
                                                    contentDescription = null,
                                                    tint               = getActivityColor(activity.type),
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
                                                    color      = getActivityColor(activity.type)
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

        // Po tipu
        StatSectionTitle(stringResource(R.string.stats_by_type))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(24.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val activitiesByType = filteredActivities
                    .groupBy { it.type }
                    .entries
                    .sortedByDescending { it.value.size }

                if (activitiesByType.isEmpty()) {
                    Text(
                        text     = stringResource(R.string.stats_no_activities),
                        style    = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    activitiesByType.forEachIndexed { idx, (type, list) ->
                        TypeRow(
                            type         = type,
                            count        = list.size,
                            color        = getActivityColor(type),
                            distance     = list.sumOf { it.distanceMeters.toDouble() } / divisor,
                            unit         = unitLabel,
                            avgSpeed     = list.map { it.avgSpeedKmh }.filter { it > 0f }
                                .average().takeIf { it.isFinite() }?.toFloat() ?: 0f,
                            totalSeconds = list.sumOf { it.durationSeconds },
                            useKm        = useKm
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

// ── Composable helpers ─────────────────────────────────────────────────────────

@Composable
fun StatDetailItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(24.dp)
        )
        Text(
            value,
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
fun TypeRow(
    type: String,
    count: Int,
    color: Color,
    distance: Double,
    unit: String,
    avgSpeed: Float,
    totalSeconds: Long,
    useKm: Boolean
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier              = Modifier.weight(1f)
        ) {
            Surface(color = color.copy(alpha = 0.12f), shape = CircleShape) {
                Icon(
                    imageVector        = activityIcon(type),
                    contentDescription = null,
                    modifier           = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                    tint               = color
                )
            }
            Column {
                Text(
                    activityTypeDisplayName(type),
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.stats_activity_count, count),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (avgSpeed > 0f) {
                        Text(
                            "·",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatSpeed(avgSpeed, useKm),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "%.1f $unit".format(distance),
                fontWeight = FontWeight.Bold,
                style      = MaterialTheme.typography.bodyLarge,
                color      = color
            )
            Text(
                formatDuration(totalSeconds),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyChart() {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = stringResource(R.string.stats_no_data),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// helpers

fun getLastNDays(n: Int, locale: Locale = Locale.getDefault()): List<Triple<Long, Long, String>> {
    val result     = mutableListOf<Triple<Long, Long, String>>()
    val calendar   = Calendar.getInstance()
    val dayFormat  = SimpleDateFormat("EEE", locale)
    val dateFormat = SimpleDateFormat("dd.MM", locale)

    repeat(n) {
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

        val label = if (n <= 7)
            dayFormat.format(calendar.time)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        else
            dateFormat.format(calendar.time)

        result.add(Triple(start, end, label))
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }

    return result.asReversed()
}

fun formatDurationShort(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
fun periodLabel(period: StatsPeriod, lang: String): String = when (period) {
    StatsPeriod.WEEK         -> if (lang == "sr") "7 dana"  else "7 days"
    StatsPeriod.MONTH        -> if (lang == "sr") "30 dana" else "30 days"
    StatsPeriod.THREE_MONTHS -> if (lang == "sr") "90 dana" else "90 days"
}