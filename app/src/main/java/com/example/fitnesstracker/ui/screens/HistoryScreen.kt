package com.example.fitnesstracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.model.Activity
import com.example.fitnesstracker.data.model.ActivityType
import com.example.fitnesstracker.ui.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: ActivityViewModel, navController: NavController) {
    val activities by viewModel.activities.collectAsState()
    val units by viewModel.units.collectAsState()
    val useKm = units == "km"
    val unitLabel = if (useKm) "km" else "mi"

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf<DatePickerTarget?>(null) }

    var filterType by remember { mutableStateOf("") }
    var minDistance by remember { mutableStateOf("") }
    var dateFrom by remember { mutableStateOf<Long?>(null) }
    var dateTo by remember { mutableStateOf<Long?>(null) }
    var minDuration by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val error by viewModel.error.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    error?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    val hasActiveFilters = filterType.isNotBlank() || minDistance.isNotBlank() ||
            minDuration.isNotBlank() || dateFrom != null || dateTo != null
    val isSelectionMode = selectedIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode) { selectedIds = emptySet() }
    BackHandler(enabled = isSearchActive && !isSelectionMode) {
        isSearchActive = false
        searchQuery = ""
        keyboardController?.hide()
    }
    BackHandler(enabled = showFilterSheet) {
        showFilterSheet = false
    }

    val shortDateFmt = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    val localizedTypeNames = ActivityType.allKeys.associateWith { activityTypeDisplayName(it) }

    val displayList = remember(activities, searchQuery, filterType, minDistance, minDuration, dateFrom, dateTo, useKm) {
        val base = if (searchQuery.isBlank()) activities
        else activities.filter { matchesSearch(it, searchQuery, localizedTypeNames) }
        base.filter { activity ->
            val dist = if (useKm) activity.distanceMeters / 1000f else activity.distanceMeters / 1609f
            val endOfDay = dateTo?.let { it + 86_399_999L }
            val matchesType = filterType.isBlank() || activity.type == filterType
            val matchesDistance = minDistance.toFloatOrNull()?.let { dist >= it } ?: true
            val matchesDuration = minDuration.toIntOrNull()?.let { activity.durationSeconds / 60 >= it } ?: true
            val matchesDate = when {
                dateFrom != null && endOfDay != null -> activity.timestamp in dateFrom!!..endOfDay
                dateFrom != null -> activity.timestamp >= dateFrom!!
                endOfDay != null -> activity.timestamp <= endOfDay
                else -> true
            }
            matchesType && matchesDistance && matchesDuration && matchesDate
        }
    }

    val listState = rememberLazyListState()
    val allSelected = displayList.isNotEmpty() && selectedIds.containsAll(displayList.map { it.id })

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_text, selectedIds.size)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteActivities(selectedIds)
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

    showDatePicker?.let { target ->
        val initialMs = if (target == DatePickerTarget.FROM) dateFrom else dateTo
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMs ?: System.currentTimeMillis(),
            selectableDates = when (target) {
                DatePickerTarget.FROM -> object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) =
                        dateTo == null || utcTimeMillis <= dateTo!! + 86_399_999L
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
                    pickerState.selectedDateMillis?.let { ms ->
                        if (target == DatePickerTarget.FROM) dateFrom = ms else dateTo = ms
                    }
                    showDatePicker = null
                }) { Text(stringResource(R.string.history_filter_apply)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = null }) {
                    Text(stringResource(R.string.history_cancel))
                }
            }
        ) { DatePicker(state = pickerState) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AnimatedContent(targetState = isSelectionMode, label = "toolbar") { selection ->
                if (selection) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
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
                                selectedIds = if (allSelected) emptySet()
                                else displayList.map { it.id }.toSet()
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
                    AnimatedContent(targetState = isSearchActive, label = "search") { searching ->
                        if (searching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(R.string.history_search_hint)) },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        isSearchActive = false
                                        searchQuery = ""
                                        keyboardController?.hide()
                                    }) { Icon(Icons.Default.Close, null) }
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                            )
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
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
                                            filterType = ""; minDistance = ""; minDuration = ""
                                            dateFrom = null; dateTo = null
                                        }) {
                                            Icon(Icons.Default.FilterListOff, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    IconButton(onClick = { isSearchActive = true }) {
                                        Icon(Icons.Default.Search, null)
                                    }
                                    IconButton(
                                        onClick = { showFilterSheet = true },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = if (hasActiveFilters)
                                                MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        )
                                    ) { Icon(Icons.Default.FilterList, null) }
                                }
                            }
                        }
                    }
                }
            }

            if (!isSelectionMode && !isSearchActive && hasActiveFilters) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (filterType.isNotBlank()) item {
                        ActiveFilterChip(activityTypeDisplayName(filterType)) { filterType = "" }
                    }
                    if (minDistance.isNotBlank()) item {
                        ActiveFilterChip("≥ $minDistance $unitLabel") { minDistance = "" }
                    }
                    if (minDuration.isNotBlank()) item {
                        ActiveFilterChip("≥ $minDuration min") { minDuration = "" }
                    }
                    dateFrom?.let {
                        item {
                            ActiveFilterChip("${stringResource(R.string.history_filter_date_from)}: ${shortDateFmt.format(Date(it))}") {
                                dateFrom = null
                            }
                        }
                    }
                    dateTo?.let {
                        item {
                            ActiveFilterChip("${stringResource(R.string.history_filter_date_to)}: ${shortDateFmt.format(Date(it))}") {
                                dateTo = null
                            }
                        }
                    }
                }
            }

            if (displayList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.history_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(displayList, key = { it.id }) { activity ->
                        val isSelected = activity.id in selectedIds
                        ActivityCard(
                            activity = activity,
                            useKm = useKm,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onLongClick = { if (!isSelectionMode) selectedIds = setOf(activity.id) },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedIds = if (isSelected) selectedIds - activity.id
                                    else selectedIds + activity.id
                                } else {
                                    navController.navigate("detail/${activity.id}")
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showFilterSheet) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .pointerInput(Unit) { detectTapGestures { showFilterSheet = false } }
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(32.dp).height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            )
                        }

                        val todayStartMs = remember {
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                                .padding(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
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
                                        filterType = ""; minDistance = ""; minDuration = ""
                                        dateFrom = null; dateTo = null
                                    }) {
                                        Icon(Icons.Default.FilterListOff, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource(R.string.history_clear_filters))
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            FilterSection(stringResource(R.string.history_filter_type), filterType.isNotBlank()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    val types = listOf("") + ActivityType.allKeys
                                    items(types.size) { index ->
                                        val type = types[index]
                                        FilterChip(
                                            selected = filterType == type,
                                            onClick = { filterType = type },
                                            label = {
                                                Text(
                                                    if (type.isEmpty()) stringResource(R.string.history_filter_all)
                                                    else activityTypeDisplayName(type)
                                                )
                                            },
                                            leadingIcon = if (type.isNotEmpty()) ({
                                                Icon(activityIcon(type), null, modifier = Modifier.size(16.dp))
                                            }) else null
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            FilterSection(stringResource(R.string.history_filter_min_distance, unitLabel), minDistance.isNotBlank()) {
                                OutlinedTextField(
                                    value = minDistance,
                                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) minDistance = it },
                                    placeholder = { Text(stringResource(R.string.history_filter_distance_hint)) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    trailingIcon = if (minDistance.isNotBlank()) {
                                        { IconButton(onClick = { minDistance = "" }) { Icon(Icons.Default.Clear, null) } }
                                    } else null,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {}),
                                    singleLine = true
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            FilterSection(stringResource(R.string.history_filter_min_duration), minDuration.isNotBlank()) {
                                OutlinedTextField(
                                    value = minDuration,
                                    onValueChange = { if (it.all { c -> c.isDigit() }) minDuration = it },
                                    placeholder = { Text(stringResource(R.string.history_filter_duration_hint)) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    trailingIcon = if (minDuration.isNotBlank()) {
                                        { IconButton(onClick = { minDuration = "" }) { Icon(Icons.Default.Clear, null) } }
                                    } else null,
                                    suffix = { Text("min") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {}),
                                    singleLine = true
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            FilterSection(stringResource(R.string.history_filter_date_range), dateFrom != null || dateTo != null) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                    if (isActive) dateFrom = null
                                                    else { dateFrom = presetFrom; dateTo = null }
                                                },
                                                label = { Text(stringResource(labelRes)) }
                                            )
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            }

                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { showFilterSheet = false }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.history_filter_apply))
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

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
    OutlinedCard(modifier = modifier, onClick = onClick, shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    date ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (date != null) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (date != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (date != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
        trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) }
    )
}

private fun matchesSearch(
    activity: Activity,
    query: String,
    localizedTypeNames: Map<String, String>
): Boolean {
    val q = query.trim().lowercase()
    if (q.isBlank()) return true
    val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(activity.timestamp))
    val localizedName = localizedTypeNames[activity.type]?.lowercase() ?: ""
    return activity.type.lowercase().contains(q) ||
            localizedName.contains(q) ||
            activity.description.lowercase().contains(q) ||
            dateStr.contains(q)
}