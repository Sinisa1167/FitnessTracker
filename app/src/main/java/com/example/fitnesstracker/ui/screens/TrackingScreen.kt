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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.fitnesstracker.data.model.Activity
import com.example.fitnesstracker.service.TrackingService
import com.example.fitnesstracker.ui.ActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(viewModel: ActivityViewModel, navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isTracking by TrackingService.isTracking.observeAsState(false)
    val isPaused by TrackingService.isPaused.observeAsState(false)
    val distanceMeters by TrackingService.distanceMeters.observeAsState(0f)
    val elapsedSeconds by TrackingService.elapsedSeconds.observeAsState(0L)
    val currentSpeedKmh by TrackingService.currentSpeedKmh.observeAsState(0f)
    val avgSpeedKmh = if (elapsedSeconds > 0) (distanceMeters / 1000f) / (elapsedSeconds / 3600f) else 0f

    var selectedType by remember { mutableStateOf("Trcanje") }
    var description by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showLocationExplanationDialog by remember { mutableStateOf(false) }

    val activityTypes = listOf("Trcanje", "Hodanje", "Biciklizam", "Plivanje", "Ostalo")
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

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
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = tween(500),
        label = "startBtnColor"
    )

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { startTracking(context) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) {
            checkNotificationsAndStart(context, notificationLauncher)
        } else {
            showPermissionDialog = true
        }
    }

    if (showLocationExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationExplanationDialog = false },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            title = { Text("Lokacija isključena") },
            text = { Text("Bez GPS-a nećemo moći mjeriti distancu i rutu, već samo trajanje treninga. Želite li ipak uključiti GPS?") },
            confirmButton = {
                Button(onClick = {
                    showLocationExplanationDialog = false
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text("Uključi GPS") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLocationExplanationDialog = false
                    checkNotificationsAndStart(context, notificationLauncher)
                }) { Text("Nastavi bez GPS-a") }
            }
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Dozvola za lokaciju") },
            text = { Text("Aplikacija koristi lokaciju za precizno praćenje. Možete je omogućiti u podešavanjima ili nastaviti bez nje.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    })
                }) { Text("Podešavanja") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    checkNotificationsAndStart(context, notificationLauncher)
                }) { Text("Nastavi bez dozvole") }
            }
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Sačuvaj aktivnost") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Trajanje: ${formatDuration(elapsedSeconds)}")
                    Text("Udaljenost: ${"%.2f km".format(distanceMeters / 1000f)}")
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Opis") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val avgSpeed = if (elapsedSeconds > 0) (distanceMeters / 1000f) / (elapsedSeconds / 3600f) else 0f
                    val gpsPoints = TrackingService.pathPoints.value
                        ?.joinToString(";") { "${it.latitude},${it.longitude}" }
                        ?: ""
                    viewModel.saveActivity(
                        Activity(
                            type = selectedType,
                            durationSeconds = elapsedSeconds,
                            distanceMeters = distanceMeters,
                            timestamp = System.currentTimeMillis(),
                            description = description,
                            avgSpeedKmh = avgSpeed,
                            gpsPoints = gpsPoints
                        )
                    )
                    showSaveDialog = false
                    navController.navigate("history") {
                        popUpTo("tracking") { inclusive = true }
                    }
                }) { Text("Sačuvaj") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Odbaci") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = when {
                isPaused -> "Trening pauziran"
                isTracking -> "Trening u toku"
                else -> "Novi trening"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        AnimatedVisibility(visible = !isLocationEnabled && !isTracking) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOff, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        "GPS je isključen. Distanca neće biti mjerena.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        AnimatedVisibility(visible = isPaused) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Trening je pauziran",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        if (!isTracking) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Aktivnost") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    activityTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = { selectedType = type; expanded = false }
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    formatDuration(elapsedSeconds),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isPaused)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "KM", value = "%.2f".format(distanceMeters / 1000f))
                    StatDivider()
                    StatItem(label = "KCAL", value = "${(distanceMeters * 0.05).toInt()}")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "TRENUTNA",
                        value = "%.1f".format(currentSpeedKmh),
                        unit = "km/h",
                        highlight = isTracking && !isPaused
                    )
                    StatDivider()
                    StatItem(
                        label = "PROSJEČNA",
                        value = "%.1f".format(avgSpeedKmh),
                        unit = "km/h"
                    )
                }
            }
        }

        if (isTracking) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (isPaused) {
                            context.startService(
                                Intent(context, TrackingService::class.java).apply {
                                    action = TrackingService.ACTION_RESUME
                                }
                            )
                        } else {
                            context.startService(
                                Intent(context, TrackingService::class.java).apply {
                                    action = TrackingService.ACTION_PAUSE
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isPaused) "NASTAVI" else "PAUZA",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        stopTracking(context)
                        showSaveDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ZAUSTAVI", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Button(
                onClick = {
                    val gpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val locPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!gpsOn) {
                        showLocationExplanationDialog = true
                    } else if (!locPermission) {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        checkNotificationsAndStart(context, notificationLauncher)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = startButtonColor),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("ZAPOČNI", fontWeight = FontWeight.Bold)
            }
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
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (highlight)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            if (unit != null) {
                Text(
                    unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatDivider() {
    Box(
        modifier = Modifier
            .height(40.dp)
            .width(1.dp)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    }
}

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