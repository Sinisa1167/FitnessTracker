package com.example.fitnesstracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.model.Activity
import com.example.fitnesstracker.ui.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: ActivityViewModel, navController: NavController) {
    val activities by viewModel.activities.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val units by viewModel.units.collectAsState()
    val useKm = units == "km"
    val unitLabel = if (useKm) "km" else "mi"

    var searchQuery by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf<DatePickerTarget?>(null) }

    // Filter state
    var filterType by remember { mutableStateOf("") }
    var minDistance by remember { mutableStateOf("") }
    var dateFrom by remember { mutableStateOf<Long?>(null) }
    var dateTo by remember { mutableStateOf<Long?>(null) }

    val hasActiveFilters = filterType.isNotBlank() || minDistance.isNotBlank() || dateFrom != null || dateTo != null
    val isSelectionMode = selectedIds.isNotEmpty()

    val shortDateFmt = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    // Filter logic
    val displayList = remember(searchQuery, searchResults, activities, filterType, minDistance, dateFrom, dateTo, units) {
        val baseList = if (searchQuery.isNotBlank()) searchResults else activities
        baseList.filter { activity ->
            val matchesType = filterType.isBlank() || activity.type == filterType
            val dist = if (useKm) activity.distanceMeters / 1000f else activity.distanceMeters / 1609f
            val matchesDistance = minDistance.toFloatOrNull()?.let { dist >= it } ?: true
            val endOfDay = dateTo?.let { it + 86_399_999L }
            val matchesDate = when {
                dateFrom != null && endOfDay != null -> activity.timestamp in dateFrom!!..endOfDay
                dateFrom != null -> activity.timestamp >= dateFrom!!
                endOfDay != null -> activity.timestamp <= endOfDay
                else -> true
            }
            matchesType && matchesDistance && matchesDate
        }
    }

    val allSelected = displayList.isNotEmpty() && selectedIds.containsAll(displayList.map { it.id })

    // Date Picker Dialog
    showDatePicker?.let { target ->
        val initialMs = if (target == DatePickerTarget.FROM) dateFrom else dateTo
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialMs ?: System.currentTimeMillis(),
            selectableDates = when (target) {
                DatePickerTarget.FROM -> object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) =
                        dateTo == null || utcTimeMillis <= (dateTo!! + 86_399_999L)
                }
                DatePickerTarget.TO -> object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) =
                        dateFrom == null || utcTimeMillis >= dateFrom!!
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        if (target == DatePickerTarget.FROM) dateFrom = ms
                        else dateTo = ms
                    }
                    showDatePicker = null
                }) { Text(stringResource(R.string.history_filter_apply)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = null }) {
                    Text(stringResource(R.string.history_cancel))
                }
            }
        ) {
            DatePicker(state = state)
        }
    }

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_text, selectedIds.size)) },
            confirmButton = {
                Button(
                    onClick = {
                        displayList.filter { it.id in selectedIds }.forEach { viewModel.deleteActivity(it) }
                        selectedIds = emptySet()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.history_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.history_cancel))
                }
            }
        )
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // Scrollable filter content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(top = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.history_filters),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (hasActiveFilters) {
                            TextButton(onClick = {
                                filterType = ""; minDistance = ""; dateFrom = null; dateTo = null
                            }) {
                                Icon(Icons.Default.FilterListOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.history_clear_filters))
                            }
                        }
                    }

                    //Tip aktivnosti
                    Text(stringResource(R.string.history_filter_type), style = MaterialTheme.typography.labelMedium)
                    val types = listOf("", "Trčanje", "Hodanje", "Biciklizam", "Plivanje", "Ostalo")
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(types.size) { index ->
                            val type = types[index]
                            FilterChip(
                                selected = filterType == type,
                                onClick = { filterType = type },
                                label = {
                                    Text(
                                        if (type == "") stringResource(R.string.history_filter_all)
                                        else activityTypeDisplayName(type)
                                    )
                                },
                                leadingIcon = if (type.isNotEmpty()) ({
                                    Icon(activityIcon(type), null, modifier = Modifier.size(16.dp))
                                }) else null
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Minimalna distanca
                    Text(
                        stringResource(R.string.history_filter_min_distance, unitLabel),
                        style = MaterialTheme.typography.labelMedium
                    )
                    OutlinedTextField(
                        value = minDistance,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) minDistance = it },
                        placeholder = { Text(stringResource(R.string.history_filter_distance_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = if (minDistance.isNotBlank()) {
                            { IconButton(onClick = { minDistance = "" }) { Icon(Icons.Default.Clear, null) } }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { /* field stays visible above keyboard */ }
                        ),
                        singleLine = true
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Vremenski period
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.history_filter_date_range),
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (dateFrom != null || dateTo != null) {
                            IconButton(
                                onClick = { dateFrom = null; dateTo = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Quick presets
                    val todayStartMs = remember {
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            R.string.history_filter_date_today to 0,
                            R.string.history_filter_date_week to 6,
                            R.string.history_filter_date_month to 29,
                            R.string.history_filter_date_3months to 89,
                        )
                        items(presets.size) { i ->
                            val (labelRes, daysBack) = presets[i]
                            val presetFrom = todayStartMs - daysBack * 86_400_000L
                            val isActive = dateFrom == presetFrom && dateTo == null
                            FilterChip(
                                selected = isActive,
                                onClick = {
                                    if (isActive) { dateFrom = null }
                                    else { dateFrom = presetFrom; dateTo = null }
                                },
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }

                    // Od / Do picker buttons
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DateButton(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.history_filter_date_from),
                            date = dateFrom?.let { shortDateFmt.format(Date(it)) },
                            onClick = { showDatePicker = DatePickerTarget.FROM },
                            onClear = { dateFrom = null }
                        )
                        DateButton(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.history_filter_date_to),
                            date = dateTo?.let { shortDateFmt.format(Date(it)) },
                            onClick = { showDatePicker = DatePickerTarget.TO },
                            onClear = { dateTo = null }
                        )
                    }

                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(stringResource(R.string.history_filter_apply))
                }
            }
        }
    }

    // Main content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title / selection toolbar
        AnimatedContent(targetState = isSelectionMode, label = "") { selection ->
            if (selection) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, null)
                        }
                        Text(
                            stringResource(R.string.history_selected, selectedIds.size),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row {
                        TextButton(onClick = {
                            selectedIds = if (allSelected) emptySet() else displayList.map { it.id }.toSet()
                        }) {
                            Text(
                                if (allSelected) stringResource(R.string.history_deselect_all)
                                else stringResource(R.string.history_select_all)
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.history_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasActiveFilters) {
                            IconButton(onClick = {
                                filterType = ""; minDistance = ""; dateFrom = null; dateTo = null
                            }) {
                                Icon(Icons.Default.FilterListOff, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(
                            onClick = { showFilterSheet = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (hasActiveFilters)
                                    MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.FilterList, null)
                        }
                    }
                }
            }
        }

        // Active filter chips summary
        if (!isSelectionMode && hasActiveFilters) {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filterType.isNotBlank()) {
                    item {
                        ActiveFilterChip(
                            label = activityTypeDisplayName(filterType),
                            onRemove = { filterType = "" }
                        )
                    }
                }
                if (minDistance.isNotBlank()) {
                    item {
                        ActiveFilterChip(
                            label = "≥ $minDistance $unitLabel",
                            onRemove = { minDistance = "" }
                        )
                    }
                }
                if (dateFrom != null) {
                    item {
                        ActiveFilterChip(
                            label = "${stringResource(R.string.history_filter_date_from)}: ${shortDateFmt.format(Date(dateFrom!!))}",
                            onRemove = { dateFrom = null }
                        )
                    }
                }
                if (dateTo != null) {
                    item {
                        ActiveFilterChip(
                            label = "${stringResource(R.string.history_filter_date_to)}: ${shortDateFmt.format(Date(dateTo!!))}",
                            onRemove = { dateTo = null }
                        )
                    }
                }
            }
        }

        // Search bar
        if (!isSelectionMode) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; viewModel.setSearch(it) },
                placeholder = { Text(stringResource(R.string.history_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (searchQuery.isNotBlank()) {
                    {
                        IconButton(onClick = { searchQuery = ""; viewModel.setSearch("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                } else null,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
        }

        // List
        if (displayList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.history_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(displayList, key = { it.id }) { activity ->
                    val isSelected = activity.id in selectedIds
                    NewActivityCard(
                        activity = activity,
                        useKm = useKm,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onLongClick = { if (!isSelectionMode) selectedIds = setOf(activity.id) },
                        onClick = {
                            if (isSelectionMode) {
                                selectedIds = if (isSelected) selectedIds - activity.id else selectedIds + activity.id
                            } else {
                                navController.navigate("detail/${activity.id}")
                            }
                        }
                    )
                }
            }
        }
    }
}

// Helper

private enum class DatePickerTarget { FROM, TO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateButton(
    modifier: Modifier = Modifier,
    label: String,
    date: String?,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    OutlinedCard(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    date ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (date != null) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (date != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (date != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActiveFilterChip(label: String, onRemove: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        trailingIcon = {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewActivityCard(
    activity: Activity,
    useKm: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
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
                        activityIcon(activity.type),
                        null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    activityTypeDisplayName(activity.type),
                    style = MaterialTheme.typography.titleMedium,
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
                        color      = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        formatDuration(activity.durationSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isSelectionMode) {
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
}