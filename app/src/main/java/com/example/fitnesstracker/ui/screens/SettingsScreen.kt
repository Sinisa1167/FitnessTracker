package com.example.fitnesstracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.example.fitnesstracker.util.responsiveMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fitnesstracker.LocalAppLang
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.DEFAULT_GOALS
import com.example.fitnesstracker.data.PreferencesManager
import com.example.fitnesstracker.data.UserProfile
import com.example.fitnesstracker.ui.ActivityViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ActivityViewModel) {
    val lang = LocalAppLang.current
    val context = LocalContext.current
    val prefs   = remember { PreferencesManager(context) }
    val scope   = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState  = remember { SnackbarHostState() }

    val language             by prefs.language.collectAsState(initial = "sr")
    val units                by prefs.units.collectAsState(initial = "km")
    val notificationsEnabled by prefs.notificationsEnabled.collectAsState(initial = true)
    val reminderHours        by prefs.reminderHours.collectAsState(initial = 48)
    val profile              by prefs.userProfile.collectAsState(initial = UserProfile())
    val allGoals by viewModel.allGoals.collectAsState()

    fun formatInitialValue(value: Float): String = if (value > 0f) value.toString().replace(".0", "") else ""
    fun formatInitialValue(value: Int): String = if (value > 0) value.toString() else ""

    var weightInput  by remember(profile.weightKg) { mutableStateOf(formatInitialValue(profile.weightKg)) }
    var heightInput  by remember(profile.heightCm) { mutableStateOf(formatInitialValue(profile.heightCm)) }
    var ageInput     by remember(profile.ageYears) { mutableStateOf(formatInitialValue(profile.ageYears)) }
    var selectedSex by remember(profile.isMale) { mutableStateOf(profile.isMale as Boolean?) }
    var showResetDialog by remember { mutableStateOf(false) }

    val cleanWeight = weightInput.replace(',', '.')
    val cleanHeight = heightInput.replace(',', '.')

    val weightFloat = cleanWeight.toFloatOrNull()
    val heightFloat = cleanHeight.toFloatOrNull()
    val ageInt      = ageInput.toIntOrNull()

    // Validacija
    val isWeightError = weightInput.isNotEmpty() && (weightFloat == null || weightFloat !in 30f..300f)
    val isHeightError = heightInput.isNotEmpty() && (heightFloat == null || heightFloat !in 100f..250f)
    val isAgeError    = ageInput.isNotEmpty() && (ageInt == null || ageInt !in 5..110)

    val isSaveEnabled = !isWeightError && !isHeightError && !isAgeError

    val hasChanges = weightInput != formatInitialValue(profile.weightKg) ||
            heightInput != formatInitialValue(profile.heightCm) ||
            ageInput != formatInitialValue(profile.ageYears) ||
            selectedSex != profile.isMale

    val activityTypeKeys = DEFAULT_GOALS.keys.toList()
    var selectedGoalType by remember { mutableStateOf(activityTypeKeys.first()) }
    val currentGoal      = allGoals[selectedGoalType]

    val labelWeight      = stringResource(R.string.settings_profile_weight)
    val labelHeight      = stringResource(R.string.settings_profile_height)
    val labelAge         = stringResource(R.string.settings_profile_age)
    val labelAgeUnit     = stringResource(R.string.settings_profile_age_unit)
    val labelSex         = stringResource(R.string.settings_profile_sex)
    val labelMale        = stringResource(R.string.settings_profile_male)
    val labelFemale      = stringResource(R.string.settings_profile_female)
    val labelSave        = stringResource(R.string.settings_profile_save)
    val labelSaved       = stringResource(R.string.settings_profile_save_success)
    val labelReset       = stringResource(R.string.settings_profile_reset)
    val dialogResetTitle = stringResource(R.string.settings_reset_dialog_title)
    val dialogResetDesc  = stringResource(R.string.settings_reset_dialog_desc)
    val dialogResetConf  = stringResource(R.string.settings_reset_dialog_confirm)
    val dialogResetCanc  = stringResource(R.string.settings_reset_dialog_cancel)
    val kilometresString  = stringResource(R.string.settings_units_km)
    val milesString  = stringResource(R.string.settings_units_mi)

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(dialogResetTitle) },
            text = { Text(dialogResetDesc) },
            confirmButton = {
                TextButton(
                    onClick = {
                        weightInput = formatInitialValue(profile.weightKg)
                        heightInput = formatInitialValue(profile.heightCm)
                        ageInput = formatInitialValue(profile.ageYears)
                        selectedSex = profile.isMale
                        showResetDialog = false
                    }
                ) {
                    Text(dialogResetConf, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(dialogResetCanc)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                .responsiveMaxWidth()
                .fillMaxWidth()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text       = stringResource(R.string.settings_title),
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onSurface
            )

            // Moje mjere
            SettingsSection(
                title = stringResource(R.string.settings_profile),
                icon  = Icons.Default.Person
            ) {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value           = weightInput,
                            onValueChange   = { weightInput = it },
                            label           = {
                                Text(
                                    text = labelWeight,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            },
                            suffix          = { Text("kg") },
                            trailingIcon    = {
                                if (weightInput.isNotEmpty()) {
                                    IconButton(onClick = { weightInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            modifier        = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine      = true,
                            isError         = isWeightError
                        )
                        OutlinedTextField(
                            value           = heightInput,
                            onValueChange   = { heightInput = it },
                            label           = {
                                Text(
                                    text = labelHeight,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            },
                            suffix          = { Text("cm") },
                            trailingIcon    = {
                                if (heightInput.isNotEmpty()) {
                                    IconButton(onClick = { heightInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            modifier        = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine      = true,
                            isError         = isHeightError
                        )
                    }

                    OutlinedTextField(
                        value           = ageInput,
                        onValueChange   = { ageInput = it },
                        label           = {
                            Text(
                                text = labelAge,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        },
                        suffix          = { Text(labelAgeUnit) },
                        trailingIcon    = {
                            if (ageInput.isNotEmpty()) {
                                IconButton(onClick = { ageInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        modifier        = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine      = true,
                        isError         = isAgeError
                    )

                    Text(labelSex, style = MaterialTheme.typography.labelLarge)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedSex == true,
                            onClick  = { selectedSex = if (selectedSex == true) null else true },
                            shape    = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text(labelMale) }
                        SegmentedButton(
                            selected = selectedSex == false,
                            onClick  = { selectedSex = if (selectedSex == false) null else false },
                            shape    = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text(labelFemale) }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            visible = hasChanges,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            TextButton(
                                onClick = { showResetDialog = true },
                                modifier = Modifier.wrapContentWidth(),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(labelReset)
                            }
                        }

                        Button(
                            onClick = {
                                keyboardController?.hide()
                                scope.launch {
                                    prefs.saveUserProfile(
                                        UserProfile(
                                            weightKg = weightFloat ?: 0f,
                                            heightCm = heightFloat ?: 0f,
                                            ageYears = ageInt ?: 0,
                                            isMale   = selectedSex
                                        )
                                    )
                                    snackbarHostState.showSnackbar(labelSaved)
                                }
                            },
                            enabled = isSaveEnabled,
                            modifier = Modifier.weight(1f),
                            shape    = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(labelSave)
                        }
                    }
                }
            }

            // Dnevni ciljevi
            SettingsSection(
                title = stringResource(R.string.settings_goals),
                icon  = Icons.Default.Flag
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activityTypeKeys.size) { index ->
                            val typeKey = activityTypeKeys[index]
                            FilterChip(
                                selected    = selectedGoalType == typeKey,
                                onClick     = { selectedGoalType = typeKey },
                                label       = { Text(activityTypeDisplayName(typeKey)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector        = activityIcon(typeKey),
                                        contentDescription = null,
                                        modifier           = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }

                    currentGoal?.let { goal ->
                        var distanceSlider by remember(goal.distanceKm) { mutableFloatStateOf(goal.distanceKm) }
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
                                Text(stringResource(R.string.settings_goal_distance), style = MaterialTheme.typography.bodyLarge)
                            }
                            Text(
                                text       = "%.0f km (%.1f mi)".format(distanceSlider, distanceSlider * 0.621371f),
                                style      = MaterialTheme.typography.titleMedium,
                                color      = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value               = distanceSlider,
                            onValueChange       = { distanceSlider = it },
                            onValueChangeFinished = {
                                val rounded = distanceSlider.toInt().toFloat().coerceIn(1f, 50f)
                                distanceSlider = rounded
                                viewModel.setGoalDistance(selectedGoalType, rounded)
                            },
                            valueRange = 1f..50f,
                            steps      = 0
                        )

                        HorizontalDivider()

                        var durationSlider by remember(goal.durationMin) { mutableFloatStateOf(goal.durationMin) }
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
                                Text(stringResource(R.string.settings_goal_duration), style = MaterialTheme.typography.bodyLarge)
                            }
                            Text(
                                text       = "%.0f min".format(durationSlider),
                                style      = MaterialTheme.typography.titleMedium,
                                color      = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value               = durationSlider,
                            onValueChange       = { durationSlider = it },
                            onValueChangeFinished = {
                                val rounded = (kotlin.math.round(durationSlider / 5f) * 5f).coerceIn(5f, 180f)
                                durationSlider = rounded
                                viewModel.setGoalDuration(selectedGoalType, rounded)
                            },
                            valueRange = 5f..180f,
                            steps      = 0
                        )
                    }
                }
            }

            // Sistemska podešavanja
            SettingsSection(
                title = stringResource(R.string.settings_system_title),
                icon  = Icons.Default.Settings
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_reminders),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                stringResource(R.string.settings_reminders_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked         = notificationsEnabled,
                            onCheckedChange = { scope.launch { prefs.setNotifications(it) } }
                        )
                    }

                    AnimatedVisibility(visible = notificationsEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Text(
                                stringResource(R.string.settings_reminder_interval),
                                style = MaterialTheme.typography.labelLarge
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val presets = listOf(12, 24, 48, 72)
                                items(presets.size) { i ->
                                    val hours = presets[i]
                                    FilterChip(
                                        selected = reminderHours == hours,
                                        onClick  = {
                                            scope.launch {
                                                prefs.setReminderHours(hours)
                                                com.example.fitnesstracker.worker.ReminderWorker.scheduleFromNow(context, hours)
                                            }
                                        },
                                        label = { Text("${hours}h") }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_units), style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = units == "km",
                                onClick  = { scope.launch { prefs.setUnits("km") } },
                                label    = { Text(kilometresString) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = units == "mi",
                                onClick  = { scope.launch { prefs.setUnits("mi") } },
                                label    = { Text(milesString) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = language == "sr",
                                onClick  = { scope.launch { prefs.setLanguage("sr") } },
                                label    = { Text(stringResource(R.string.settings_lang_sr)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = language == "en",
                                onClick  = { scope.launch { prefs.setLanguage("en") } },
                                label    = { Text(stringResource(R.string.settings_lang_en)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // O aplikaciji
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Fitness Tracker v${stringResource(R.string.settings_version_value)}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        stringResource(R.string.settings_author_value),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(20.dp)
                    )
                    Text(
                        text       = title,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector        = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier            = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider()
                    content()
                }
            }
        }
    }
}