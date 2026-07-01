package com.example.fitnesstracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fitnesstracker.LocalAppLang
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.model.Activity
import com.example.fitnesstracker.ui.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class StatsPeriod { WEEK, MONTH, THREE_MONTHS }

/** Jedan "bucket" na grafikonu — ili jedan dan (7/30 dana) ili jedna sedmica (90 dana). */
data class StatsBucket(
    val start: Long,
    val end: Long,
    val label: String,
    val activities: List<Activity>
) {
    val distanceMeters: Float get() = activities.sumOf { it.distanceMeters.toDouble() }.toFloat()
}

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

    val buckets = remember(activities, selectedPeriod, locale) {
        when (selectedPeriod) {
            StatsPeriod.WEEK         -> buildDailyBuckets(activities, 7, locale)
            StatsPeriod.MONTH        -> buildDailyBuckets(activities, 30, locale)
            StatsPeriod.THREE_MONTHS -> buildWeeklyBuckets(activities, 13, locale)
        }
    }

    val isAggregated = selectedPeriod == StatsPeriod.THREE_MONTHS

    val filteredActivities = remember(buckets) {
        buckets.flatMap { it.activities }
    }

    var selectedIndex by remember(selectedPeriod) { mutableStateOf<Int?>(null) }
    val selectedBucket = selectedIndex?.let { buckets.getOrNull(it) }

    val totalDistance        = filteredActivities.sumOf { it.distanceMeters.toDouble() } / divisor
    val totalDurationSeconds = filteredActivities.sumOf { it.durationSeconds }
    val totalCalories        = filteredActivities.sumOf { it.caloriesBurned }
    val primaryColor         = MaterialTheme.colorScheme.primary

    val barWidth: Dp = when (selectedPeriod) {
        StatsPeriod.WEEK         -> 32.dp
        StatsPeriod.MONTH        -> 14.dp
        StatsPeriod.THREE_MONTHS -> 26.dp
    }
    val barSpacing: Dp = when (selectedPeriod) {
        StatsPeriod.WEEK         -> 18.dp
        StatsPeriod.MONTH        -> 8.dp
        StatsPeriod.THREE_MONTHS -> 14.dp
    }
    val labelEvery = when (selectedPeriod) {
        StatsPeriod.WEEK         -> 1
        StatsPeriod.MONTH        -> 5
        StatsPeriod.THREE_MONTHS -> 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
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
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            StatSectionTitle(
                when (selectedPeriod) {
                    StatsPeriod.WEEK         -> stringResource(R.string.stats_last7days)
                    StatsPeriod.MONTH        -> stringResource(R.string.stats_last30days)
                    StatsPeriod.THREE_MONTHS -> stringResource(R.string.stats_last90days)
                }
            )
            if (isAggregated) {
                Text(
                    if (lang == "sr") "po sedmicama" else "by week",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
                    DistanceBarChart(
                        buckets       = buckets,
                        divisor       = divisor,
                        barWidth      = barWidth,
                        barSpacing    = barSpacing,
                        labelEvery    = labelEvery,
                        selectedIndex = selectedIndex,
                        onSelect      = { idx -> selectedIndex = if (selectedIndex == idx) null else idx },
                        barColor      = primaryColor
                    )

                    // Detalji (dan ili sedmica)
                    if (selectedBucket != null) {
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
                                        if (isAggregated)
                                            (if (lang == "sr") "Sedmica od ${selectedBucket.label}" else "Week of ${selectedBucket.label}")
                                        else selectedBucket.label,
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "%.2f $unitLabel".format(selectedBucket.distanceMeters / divisor),
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color      = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (selectedBucket.activities.isEmpty()) {
                                    Text(
                                        stringResource(R.string.stats_no_activities),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    selectedBucket.activities.forEach { activity ->
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
                                                    formatDate(activity.timestamp),
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
                                                if (activity.caloriesBurned > 0) {
                                                    Text(
                                                        "${activity.caloriesBurned} kcal",
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

// Bar chart
@Composable
fun DistanceBarChart(
    buckets: List<StatsBucket>,
    divisor: Float,
    barWidth: Dp,
    barSpacing: Dp,
    labelEvery: Int,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    barColor: Color
) {
    val chartHeight = 160.dp
    val maxValue = remember(buckets) {
        (buckets.maxOfOrNull { it.distanceMeters } ?: 0f).coerceAtLeast(1f)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val slot = barWidth + barSpacing
        val naturalWidth = slot * buckets.size
        val availableWidth = maxWidth
        val contentWidth = if (naturalWidth > availableWidth) naturalWidth else availableWidth
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .width(contentWidth)
                .height(chartHeight + 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (naturalWidth > availableWidth) Arrangement.Start else Arrangement.SpaceEvenly
            ) {
                buckets.forEachIndexed { index, bucket ->
                    val targetFraction = (bucket.distanceMeters / maxValue).coerceIn(0f, 1f)
                    val animatedFraction by animateFloatAsState(
                        targetValue   = targetFraction,
                        animationSpec = tween(500),
                        label         = "bar$index"
                    )
                    val isSelected = selectedIndex == index
                    val hasData = bucket.distanceMeters > 0f

                    Box(
                        modifier = Modifier
                            .width(slot)
                            .height(chartHeight)
                            .clickable(enabled = hasData) { onSelect(index) },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .fillMaxHeight(fraction = if (hasData) animatedFraction.coerceAtLeast(0.03f) else 0.015f)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    when {
                                        !hasData   -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                        isSelected -> barColor
                                        else       -> barColor.copy(alpha = 0.45f)
                                    }
                                )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // (X OSA)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = if (naturalWidth > availableWidth) Arrangement.Start else Arrangement.SpaceEvenly
            ) {
                buckets.forEachIndexed { index, bucket ->
                    val isSelected = selectedIndex == index

                    Box(
                        modifier = Modifier.width(slot),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (index % labelEvery == 0) {
                            Text(
                                text       = bucket.label,
                                style      = MaterialTheme.typography.labelSmall,
                                color      = if (isSelected) barColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines   = 1,
                                modifier   = Modifier.wrapContentWidth(unbounded = true)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Composable helpers
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
    val avgSpeedLabel = stringResource(R.string.stats_avg_speed)
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
                            "$avgSpeedLabel ${formatSpeed(avgSpeed, useKm)}",
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

fun buildDailyBuckets(activities: List<Activity>, days: Int, locale: Locale): List<StatsBucket> {
    val calendar   = Calendar.getInstance()
    val dayFormat  = SimpleDateFormat("EEE", locale)
    val dateFormat = if (days > 7) SimpleDateFormat("d", locale) else SimpleDateFormat("dd.MM", locale)
    val result     = mutableListOf<StatsBucket>()

    repeat(days) {
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

        val label = if (days <= 7) {
            dayFormat.format(calendar.time)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        } else {
            dateFormat.format(calendar.time)
        }

        result.add(StatsBucket(start, end, label, activities.filter { it.timestamp in start..end }))
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }

    return result.asReversed()
}

fun buildWeeklyBuckets(activities: List<Activity>, weeks: Int, locale: Locale): List<StatsBucket> {
    val calendar   = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd.MM", locale)
    val result     = mutableListOf<StatsBucket>()

    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)

    repeat(weeks) {
        val end = calendar.timeInMillis

        val startCal = calendar.clone() as Calendar
        startCal.add(Calendar.DAY_OF_YEAR, -6)
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)
        val start = startCal.timeInMillis

        val label = dateFormat.format(Date(start))
        result.add(StatsBucket(start, end, label, activities.filter { it.timestamp in start..end }))
        calendar.add(Calendar.DAY_OF_YEAR, -7)
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