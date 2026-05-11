package com.example.fitnesstracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitnesstracker.data.DEFAULT_GOALS
import com.example.fitnesstracker.data.PreferencesManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context            = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val scope              = rememberCoroutineScope()

    val language            by preferencesManager.language.collectAsState(initial = "sr")
    val units               by preferencesManager.units.collectAsState(initial = "km")
    val notificationsEnabled by preferencesManager.notificationsEnabled.collectAsState(initial = true)
    val allGoals            by preferencesManager.allGoals.collectAsState(initial = emptyMap())

    val activityTypes = DEFAULT_GOALS.keys.toList()
    var selectedGoalType by remember { mutableStateOf(activityTypes.first()) }

    val currentGoal = allGoals[selectedGoalType]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Podešavanja", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        SettingsSection(title = "Jezik") {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = language == "sr",
                    onClick  = { scope.launch { preferencesManager.setLanguage("sr") } },
                    label    = { Text("Srpski") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = language == "en",
                    onClick  = { scope.launch { preferencesManager.setLanguage("en") } },
                    label    = { Text("English") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SettingsSection(title = "Jedinice mjere") {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = units == "km",
                    onClick  = { scope.launch { preferencesManager.setUnits("km") } },
                    label    = { Text("Kilometri (km)") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = units == "mi",
                    onClick  = { scope.launch { preferencesManager.setUnits("mi") } },
                    label    = { Text("Milje (mi)") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SettingsSection(title = "Notifikacije") {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Podsjetnici za aktivnost", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Obavijesti ako nisi aktivan duže vrijeme",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked         = notificationsEnabled,
                    onCheckedChange = { scope.launch { preferencesManager.setNotifications(it) } }
                )
            }
        }

        SettingsSection(title = "Dnevni ciljevi") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activityTypes.size) { index ->
                        val type       = activityTypes[index]
                        val isSelected = selectedGoalType == type
                        FilterChip(
                            selected     = isSelected,
                            onClick      = { selectedGoalType = type },
                            label        = { Text(type) },
                            leadingIcon  = {
                                Icon(
                                    imageVector        = activityIcon(type),
                                    contentDescription = null,
                                    modifier           = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                if (currentGoal != null) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Ciljna udaljenost", style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            "%.0f km (%.0f mi)".format(currentGoal.distanceKm, currentGoal.distanceKm * 0.621371f),
                            style      = MaterialTheme.typography.titleMedium,
                            color      = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value         = currentGoal.distanceKm,
                        onValueChange = { scope.launch { preferencesManager.setGoalDistance(selectedGoalType, it) } },
                        valueRange    = 1f..50f,
                        steps         = 48
                    )

                    HorizontalDivider()

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Ciljno trajanje", style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            "%.0f min".format(currentGoal.durationMin),
                            style      = MaterialTheme.typography.titleMedium,
                            color      = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value         = currentGoal.durationMin,
                        onValueChange = { scope.launch { preferencesManager.setGoalDuration(selectedGoalType, it) } },
                        valueRange    = 5f..180f,
                        steps         = 34
                    )
                }
            }
        }

        SettingsSection(title = "O aplikaciji") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Verzija", style = MaterialTheme.typography.bodyLarge)
                    Text("1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Autor", style = MaterialTheme.typography.bodyLarge)
                    Text("ETF Banja Luka", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleMedium,
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            content()
        }
    }
}