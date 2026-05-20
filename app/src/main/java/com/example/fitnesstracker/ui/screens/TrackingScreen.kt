package com.example.fitnesstracker.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.calculateCalories
import com.example.fitnesstracker.data.model.Activity
import com.example.fitnesstracker.service.TrackingService
import com.example.fitnesstracker.ui.ActivityViewModel

// Internal keys / database.
val ACTIVITY_TYPE_KEYS = listOf("Trčanje", "Hodanje", "Biciklizam", "Plivanje", "Planinarenje", "Ostalo")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(viewModel: ActivityViewModel, navController: NavController) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isTracking      by TrackingService.isTracking.observeAsState(false)
    val isPaused        by TrackingService.isPaused.observeAsState(false)
    val distanceMeters  by TrackingService.distanceMeters.observeAsState(0f)
    val elapsedSeconds  by TrackingService.elapsedSeconds.observeAsState(0L)
    val currentSpeedKmh by TrackingService.currentSpeedKmh.observeAsState(0f)
    val units       by viewModel.units.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val useKm        = units == "km"

    val avgSpeedKmh by TrackingService.avgSpeedKmh.observeAsState(0f)

    val distanceDisplay = if (useKm) distanceMeters / 1000f else distanceMeters / 1609f
    val distanceUnit    = if (useKm) "KM" else "MI"
    val speedUnit       = if (useKm) "km/h" else "mph"
    val currentSpeed    = if (useKm) currentSpeedKmh else currentSpeedKmh * 0.621371f
    val avgSpeed        = if (useKm) avgSpeedKmh else avgSpeedKmh * 0.621371f

    var selectedType   by rememberSaveable { mutableStateOf("Trčanje") }
    val caloriesBurned = calculateCalories(selectedType, elapsedSeconds, userProfile, avgSpeedKmh)
    var description                   by remember { mutableStateOf("") }
    var showSaveDialog                by remember { mutableStateOf(false) }
    var showPermissionDialog          by remember { mutableStateOf(false) }
    var showLocationExplanationDialog by remember { mutableStateOf(false) }

    val locationManager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    var isLocationEnabled by remember {
        mutableStateOf(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val startButtonColor by animateColorAsState(
        targetValue   = MaterialTheme.colorScheme.primary,
        animationSpec = tween(500),
        label         = "startBtnColor"
    )

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { startTracking(context) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) checkNotificationsAndStart(context, notificationLauncher)
        else showPermissionDialog = true
    }

    if (showLocationExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationExplanationDialog = false },
            icon    = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            title   = { Text(stringResource(R.string.gps_off_title)) },
            text    = { Text(stringResource(R.string.gps_off_text)) },
            confirmButton = {
                Button(onClick = {
                    showLocationExplanationDialog = false
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text(stringResource(R.string.gps_enable)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLocationExplanationDialog = false
                    checkNotificationsAndStart(context, notificationLauncher)
                }) { Text(stringResource(R.string.gps_continue_without)) }
            }
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.perm_location_title)) },
            text  = { Text(stringResource(R.string.perm_location_text)) },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }) { Text(stringResource(R.string.perm_settings)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    checkNotificationsAndStart(context, notificationLauncher)
                }) { Text(stringResource(R.string.perm_continue_without)) }
            }
        )
    }

    // Save dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.save_activity)) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.save_duration, formatDuration(elapsedSeconds)))
                    Text(stringResource(
                        R.string.save_distance,
                        "%.2f %s".format(distanceDisplay, if (useKm) "km" else "mi")
                    ))
                    OutlinedTextField(
                        value         = description,
                        onValueChange = { description = it },
                        label         = { Text(stringResource(R.string.save_description_hint)) },
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val gpsPoints = TrackingService.pathPoints.value
                        ?.joinToString(";") { "${it.latitude},${it.longitude}" } ?: ""
                    viewModel.saveActivity(
                        Activity(
                            type            = selectedType,
                            durationSeconds = elapsedSeconds,
                            distanceMeters  = distanceMeters,
                            timestamp       = System.currentTimeMillis(),
                            description     = description,
                            avgSpeedKmh = TrackingService.avgSpeedKmh.value ?: 0f,
                            gpsPoints       = gpsPoints,
                            caloriesBurned  = caloriesBurned
                        )
                    )
                    showSaveDialog = false
                    navController.navigate("history") {
                        popUpTo("tracking") { inclusive = true }
                    }
                }) { Text(stringResource(R.string.save_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.save_discard))
                }
            }
        )
    }

    // Main content
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = when {
                isPaused   -> stringResource(R.string.tracking_paused_title)
                isTracking -> stringResource(R.string.tracking_in_progress)
                else       -> stringResource(R.string.tracking_new)
            },
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        AnimatedVisibility(visible = !isLocationEnabled && !isTracking) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOff, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        stringResource(R.string.tracking_gps_off),
                        modifier = Modifier.weight(1f),
                        style    = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        AnimatedVisibility(visible = isPaused) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                        tint     = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        stringResource(R.string.tracking_paused_banner),
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color      = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Activity type selector
        if (!isTracking) {
            ActivityTypeSelector(
                selectedType = selectedType,
                onSelect = { selectedType = it }
            )
        }

        // Stats card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = MaterialTheme.shapes.extraLarge,
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
            )
        ) {
            Column(
                modifier            = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    formatDuration(elapsedSeconds),
                    fontSize   = 56.sp,
                    fontWeight = FontWeight.Black,
                    color      = if (isPaused) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem(label = distanceUnit, value = "%.2f".format(distanceDisplay))
                    StatDivider()
                    StatItem(label = stringResource(R.string.tracking_kcal), value = "$caloriesBurned")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem(
                        label     = stringResource(R.string.tracking_speed_current),
                        value     = "%.1f".format(currentSpeed),
                        unit      = speedUnit,
                        highlight = isTracking && !isPaused
                    )
                    StatDivider()
                    StatItem(
                        label = stringResource(R.string.tracking_speed_avg),
                        value = "%.1f".format(avgSpeed),
                        unit  = speedUnit
                    )
                }
            }
        }

        // Control buttons
        if (isTracking) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val action = if (isPaused) TrackingService.ACTION_RESUME
                        else TrackingService.ACTION_PAUSE
                        context.startService(
                            Intent(context, TrackingService::class.java).apply { this.action = action }
                        )
                    },
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isPaused) stringResource(R.string.tracking_resume)
                        else          stringResource(R.string.tracking_pause),
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick  = { stopTracking(context); showSaveDialog = true },
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape    = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.tracking_stop), fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Button(
                onClick = {
                    val gpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val locOk = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    when {
                        !gpsOn -> showLocationExplanationDialog = true
                        !locOk -> locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        else   -> checkNotificationsAndStart(context, notificationLauncher)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = startButtonColor),
                shape    = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector        = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier           = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.tracking_start),
                    style    = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp
                )
            }
        }
    }
}

// Composable helpers
@Composable
fun ActivityTypeSelector(
    selectedType: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.tracking_select_activity),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val rows = ACTIVITY_TYPE_KEYS.chunked(3)
        rows.forEach { rowTypes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTypes.forEach { typeKey ->
                    ActivityTypeChip(
                        modifier = Modifier.weight(1f),
                        typeKey = typeKey,
                        isSelected = selectedType == typeKey,
                        onSelect = onSelect
                    )
                }
                repeat(3 - rowTypes.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ActivityTypeChip(
    modifier: Modifier,
    typeKey: String,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    val contentColor = if (isSelected)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.clickable { onSelect(typeKey) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = activityIcon(typeKey),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
            Text(
                activityTypeDisplayName(typeKey),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    unit: String? = null,
    highlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                value,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = if (highlight) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            if (unit != null) {
                Text(
                    unit,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StatDivider() {
    Box(
        modifier         = Modifier.height(40.dp).width(1.dp).padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxHeight().width(1.dp),
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    }
}

// Private helpers

private fun checkNotificationsAndStart(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<String>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startTracking(context)
        }
    } else {
        startTracking(context)
    }
}

private fun startTracking(context: Context) {
    context.startForegroundService(
        Intent(context, TrackingService::class.java).apply { action = TrackingService.ACTION_START }
    )
}

private fun stopTracking(context: Context) {
    context.startService(
        Intent(context, TrackingService::class.java).apply { action = TrackingService.ACTION_STOP }
    )
}