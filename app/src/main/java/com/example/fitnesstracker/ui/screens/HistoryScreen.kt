package com.example.fitnesstracker.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitnesstracker.data.model.Activity
import com.example.fitnesstracker.ui.ActivityViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(viewModel: ActivityViewModel, navController: NavController) {
    val activities by viewModel.activities.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("") }
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isSelectionMode = selectedIds.isNotEmpty()
    val activityTypes = listOf("", "Trčanje", "Hodanje", "Biciklizam", "Plivanje", "Ostalo")
    val displayList = if (searchQuery.isNotBlank()) searchResults else activities
    val allSelected = displayList.isNotEmpty() && selectedIds.containsAll(displayList.map { it.id })

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Obriši aktivnosti") },
            text = { Text("Da li ste sigurni da želite obrisati ${selectedIds.size} aktivnost(i)?") },
            confirmButton = {
                Button(
                    onClick = {
                        displayList
                            .filter { it.id in selectedIds }
                            .forEach { viewModel.deleteActivity(it) }
                        selectedIds = emptySet()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Obriši") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Odustani") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Istorija", style = MaterialTheme.typography.headlineMedium)

        AnimatedContent(
            targetState = isSelectionMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "searchOrSelection"
        ) { selectionMode ->
            if (selectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Otkaži selekciju")
                        }
                        Text(
                            "${selectedIds.size} odabrano",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = {
                                selectedIds = if (allSelected) emptySet()
                                else displayList.map { it.id }.toSet()
                            }
                        ) { Text(if (allSelected) "Poništi sve" else "Sve") }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Obriši odabrane",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.setSearch(it)
                            },
                            placeholder = { Text("Pretraži...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        viewModel.setSearch("")
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = if (selectedFilter.isNotBlank())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                activityTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(if (type.isBlank()) "Sve aktivnosti" else type) },
                                        onClick = {
                                            selectedFilter = type
                                            viewModel.setFilter(type)
                                            showFilterMenu = false
                                        },
                                        leadingIcon = {
                                            if (selectedFilter == type) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (selectedFilter.isNotBlank()) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                selectedFilter = ""
                                viewModel.setFilter("")
                            },
                            label = { Text(selectedFilter) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        if (displayList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Nema aktivnosti",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(displayList, key = { it.id }) { activity ->
                    val isSelected = activity.id in selectedIds
                    SelectableActivityCard(
                        activity = activity,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onLongClick = { selectedIds = selectedIds + activity.id },
                        onClick = {
                            if (isSelectionMode) {
                                selectedIds = if (isSelected) {
                                    selectedIds - activity.id
                                } else {
                                    selectedIds + activity.id
                                }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableActivityCard(
    activity: Activity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedContent(
                targetState = isSelectionMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "cardLeading"
            ) { selectionMode ->
                if (selectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = null)
                } else {
                    Icon(
                        imageVector = activityIcon(activity.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

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