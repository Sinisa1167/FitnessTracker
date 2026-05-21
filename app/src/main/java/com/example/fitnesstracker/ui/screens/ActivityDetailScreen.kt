package com.example.fitnesstracker.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.fitnesstracker.R
import com.example.fitnesstracker.ui.ActivityViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import android.view.MotionEvent

@Composable
fun ActivityDetailScreen(
    activityId: Long,
    viewModel: ActivityViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val units   by viewModel.units.collectAsState()
    val useKm   = units == "km"

    var activity       by remember { mutableStateOf<com.example.fitnesstracker.data.model.Activity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog   by remember { mutableStateOf(false) }

    LaunchedEffect(activityId) {
        activity = viewModel.getById(activityId)
    }

    activity?.let { act ->
        val gpsPoints     = parseGpsPoints(act.gpsPoints)
        val activityColor = getActivityColor(act.type)

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title            = { Text(stringResource(R.string.detail_delete_title)) },
                text             = { Text(stringResource(R.string.detail_delete_confirm_text)) },
                confirmButton    = {
                    TextButton(onClick = {
                        viewModel.deleteActivity(act)
                        navController.popBackStack()
                    }) {
                        Text(
                            stringResource(R.string.detail_delete_confirm),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.detail_delete_cancel))
                    }
                }
            )
        }

        if (showEditDialog) {
            var editedDescription by remember { mutableStateOf(act.description) }
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title            = { Text(stringResource(R.string.detail_edit_description)) },
                text             = {
                    OutlinedTextField(
                        value         = editedDescription,
                        onValueChange = { editedDescription = it },
                        modifier      = Modifier.fillMaxWidth(),
                        label         = { Text(stringResource(R.string.detail_description)) }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.updateDescription(act.id, editedDescription)
                        activity = act.copy(description = editedDescription)
                        showEditDialog = false
                    }) { Text(stringResource(R.string.save_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text(stringResource(R.string.detail_delete_cancel))
                    }
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.detail_back))
                }
                Text(
                    activityTypeDisplayName(act.type),
                    style      = MaterialTheme.typography.headlineSmall,
                    modifier   = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        Icons.Default.Edit,
                        stringResource(R.string.detail_edit_description),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        stringResource(R.string.detail_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (gpsPoints.isNotEmpty()) {
                Card(
                    modifier  = Modifier.fillMaxWidth().height(280.dp).padding(16.dp),
                    shape     = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    OsmMapView(context = context, gpsPoints = gpsPoints, routeColor = activityColor)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().height(150.dp).padding(16.dp),
                    shape    = RoundedCornerShape(24.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier            = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocationOff,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.detail_map_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape    = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier            = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        stringResource(R.string.detail_stats_title),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    Row(Modifier.fillMaxWidth()) {
                        DetailItem(
                            Modifier.weight(1f),
                            Icons.Default.Timer,
                            stringResource(R.string.detail_duration),
                            formatDuration(act.durationSeconds)
                        )
                        DetailItem(
                            Modifier.weight(1f),
                            Icons.Default.Route,
                            stringResource(R.string.detail_distance),
                            formatDistance(act.distanceMeters, useKm)
                        )
                    }

                    Row(Modifier.fillMaxWidth()) {
                        DetailItem(
                            Modifier.weight(1f),
                            Icons.Default.Speed,
                            stringResource(R.string.detail_speed),
                            formatSpeed(act.avgSpeedKmh, useKm)
                        )
                        DetailItem(
                            Modifier.weight(1f),
                            Icons.Default.Timer,
                            when (act.type) {
                                "Trčanje", "Hodanje", "Planinarenje" ->
                                    stringResource(R.string.detail_pace)
                                "Plivanje" ->
                                    stringResource(R.string.detail_pace) + " /100m"
                                else ->
                                    stringResource(R.string.detail_speed)
                            },
                            when (act.type) {
                                "Trčanje", "Hodanje", "Planinarenje" ->
                                    "${formatPace(act.avgSpeedKmh, useKm)}/${if (useKm) "km" else "mi"}"
                                "Plivanje" ->
                                    "${formatSwimPace(act.avgSpeedKmh)}/100m"
                                else ->
                                    formatSpeed(act.avgSpeedKmh, useKm)
                            }
                        )
                    }

                    Row(Modifier.fillMaxWidth()) {
                        DetailItem(
                            Modifier.weight(1f),
                            Icons.Default.Whatshot,
                            stringResource(R.string.detail_calories),
                            "${act.caloriesBurned} kcal"
                        )
                        DetailItem(
                            Modifier.weight(1f),
                            Icons.Default.CalendarToday,
                            stringResource(R.string.detail_date),
                            formatDate(act.timestamp).split(" ")[0]
                        )
                    }

                    if (act.description.isNotBlank()) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        DetailRow(
                            icon  = Icons.AutoMirrored.Filled.Notes,
                            label = stringResource(R.string.detail_description),
                            value = act.description
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun DetailItem(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier              = modifier.padding(vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun OsmMapView(context: Context, gpsPoints: List<GeoPoint>, routeColor: Color) {
    AndroidView(
        factory = {
            Configuration.getInstance().userAgentValue = context.packageName
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                zoomController.setVisibility(
                    org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                )
                val polyline = Polyline().apply {
                    setPoints(gpsPoints)
                    outlinePaint.color = android.graphics.Color.argb(
                        (routeColor.alpha * 255).toInt(),
                        (routeColor.red   * 255).toInt(),
                        (routeColor.green * 255).toInt(),
                        (routeColor.blue  * 255).toInt()
                    )
                    outlinePaint.strokeWidth = 12f
                }
                overlays.add(polyline)
                if (gpsPoints.isNotEmpty()) {
                    controller.setZoom(16.0)
                    controller.setCenter(gpsPoints.first())
                }
            }
        },
        update = { mapView ->
            mapView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.parent.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

fun parseGpsPoints(raw: String): List<GeoPoint> {
    if (raw.isBlank()) return emptyList()
    return raw.split(";").mapNotNull { point ->
        val parts = point.split(",")
        if (parts.size == 2) {
            val lat = parts[0].toDoubleOrNull()
            val lon = parts[1].toDoubleOrNull()
            if (lat != null && lon != null) GeoPoint(lat, lon) else null
        } else null
    }
}