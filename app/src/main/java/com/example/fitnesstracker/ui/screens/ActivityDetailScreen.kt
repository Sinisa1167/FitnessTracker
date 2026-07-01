package com.example.fitnesstracker.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.example.fitnesstracker.data.model.ActivityType
import kotlin.math.hypot
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset

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

    val snackbarHostState = remember { SnackbarHostState() }
    val error by viewModel.error.collectAsState()

    error?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(activityId) {
        activity = viewModel.getById(activityId)
    }

    activity?.let { act ->
        val rawGpsPoints  = remember(act.gpsPoints) { parseGpsPoints(act.gpsPoints) }
        val displayPoints = remember(rawGpsPoints) {
            smoothPath(simplifyPath(rawGpsPoints))
        }
        val activityColor = getActivityColor(act.type)

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title            = { Text(stringResource(R.string.detail_delete_title)) },
                text             = { Text(stringResource(R.string.detail_delete_confirm_text)) },
                confirmButton    = {
                    TextButton(onClick = {
                        viewModel.deleteActivity(act) {
                            navController.popBackStack()
                        }
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
            val maxDescriptionLength = 200
            var editedDescription by remember { mutableStateOf(act.description) }
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title            = { Text(stringResource(R.string.detail_edit_description)) },
                text             = {
                    Column {
                        OutlinedTextField(
                            value         = editedDescription,
                            onValueChange = { if (it.length <= maxDescriptionLength) editedDescription = it },
                            modifier      = Modifier.fillMaxWidth(),
                            label         = { Text(stringResource(R.string.detail_description)) },
                            minLines      = 3,
                            maxLines      = 5
                        )
                        Text(
                            "${editedDescription.length}/$maxDescriptionLength",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                        )
                    }
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

        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenHeightDp = configuration.screenHeightDp.dp
        val screenHeightPx = with(density) { screenHeightDp.toPx() }
        val coroutineScope = rememberCoroutineScope()

        val hasDescription = act.description.isNotBlank()

        val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomBreathingRoom = 16.dp
        val bottomInset = navBarInset + bottomBreathingRoom

        val collapsedHeight = 72.dp + bottomInset
        val baseExpandedHeight = 230.dp + bottomInset
        val descriptionExtraHeight = 90.dp
        val maxExpandedHeight = screenHeightDp * 0.6f

        val expandedHeight = if (hasDescription) {
            (baseExpandedHeight + descriptionExtraHeight).coerceAtMost(maxExpandedHeight)
        } else {
            baseExpandedHeight
        }

        val collapsedHeightPx = with(density) { collapsedHeight.toPx() }
        val expandedHeightPx = with(density) { expandedHeight.toPx() }
        val expandedOffsetPx = screenHeightPx - expandedHeightPx
        val collapsedOffsetPx = screenHeightPx - collapsedHeightPx

        var isExpanded by remember { mutableStateOf(true) }
        val panelOffset = remember { Animatable(expandedOffsetPx) }

        LaunchedEffect(activityId, screenHeightPx) {
            isExpanded = true
            panelOffset.snapTo(expandedOffsetPx)
        }

        fun togglePanel() {
            val expand = !isExpanded
            isExpanded = expand
            coroutineScope.launch {
                panelOffset.animateTo(
                    if (expand) expandedOffsetPx else collapsedOffsetPx,
                    animationSpec = tween(220)
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (displayPoints.isNotEmpty()) {
                OsmMapView(
                    context = context,
                    gpsPoints = displayPoints,
                    routeColor = activityColor,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocationOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.detail_map_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // TRAKA ZA AKCIJE
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.detail_back))
                    }
                    Text(
                        activityTypeDisplayName(act.type),
                        style      = MaterialTheme.typography.titleMedium,
                        modifier   = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, stringResource(R.string.detail_edit_description), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.detail_delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // PANEL SA STATISTIKOM
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(expandedHeight)
                    .offset { IntOffset(0, panelOffset.value.roundToInt()) }
                    .shadow(16.dp, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    coroutineScope.launch {
                                        panelOffset.snapTo(
                                            (panelOffset.value + delta).coerceIn(expandedOffsetPx, collapsedOffsetPx)
                                        )
                                    }
                                },
                                onDragStopped = {
                                    val middle = (collapsedOffsetPx + expandedOffsetPx) / 2f
                                    val expand = panelOffset.value < middle
                                    isExpanded = expand
                                    coroutineScope.launch {
                                        panelOffset.animateTo(
                                            if (expand) expandedOffsetPx else collapsedOffsetPx,
                                            animationSpec = tween(220)
                                        )
                                    }
                                }
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { togglePanel() }
                            .padding(top = 10.dp, bottom = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), shape = CircleShape)
                        )
                    }

                    // summary
                    AnimatedVisibility(
                        visible = !isExpanded,
                        enter = fadeIn(tween(180)),
                        exit = fadeOut(tween(120))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { togglePanel() }
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
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
                    }

                    // Puna statistika
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn(tween(180)),
                        exit = fadeOut(tween(120))
                    ) {
                        val gridBorderColor = MaterialTheme.colorScheme.outlineVariant
                        val gridBorderWidth = 0.5.dp

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                stringResource(R.string.detail_stats_title),
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 6.dp))

                            Row(
                                Modifier.fillMaxWidth().drawBehind {
                                    drawLine(gridBorderColor, Offset(0f, size.height), Offset(size.width, size.height), gridBorderWidth.toPx())
                                }
                            ) {
                                DetailItem(
                                    Modifier.weight(1f).drawBehind {
                                        drawLine(gridBorderColor, Offset(size.width, 0f), Offset(size.width, size.height), gridBorderWidth.toPx())
                                    },
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

                            Row(
                                Modifier.fillMaxWidth().drawBehind {
                                    drawLine(gridBorderColor, Offset(0f, size.height), Offset(size.width, size.height), gridBorderWidth.toPx())
                                }
                            ) {
                                DetailItem(
                                    Modifier.weight(1f).drawBehind {
                                        drawLine(gridBorderColor, Offset(size.width, 0f), Offset(size.width, size.height), gridBorderWidth.toPx())
                                    },
                                    Icons.Default.Speed,
                                    stringResource(R.string.detail_speed),
                                    formatSpeed(act.avgSpeedKmh, useKm)
                                )
                                when (ActivityType.fromKey(act.type)) {
                                    ActivityType.RUNNING, ActivityType.WALKING,
                                    ActivityType.HIKING, ActivityType.CYCLING -> {
                                        DetailItem(
                                            Modifier.weight(1f),
                                            Icons.Default.Timer,
                                            stringResource(R.string.detail_pace),
                                            "${formatPace(act.avgSpeedKmh, useKm)}/${if (useKm) "km" else "mi"}"
                                        )
                                    }
                                    ActivityType.SWIMMING -> {
                                        DetailItem(
                                            Modifier.weight(1f),
                                            Icons.Default.Timer,
                                            stringResource(R.string.detail_pace) + " /100m",
                                            "${formatSwimPace(act.avgSpeedKmh)}/100m"
                                        )
                                    }
                                    ActivityType.OTHER -> {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }

                            Row(Modifier.fillMaxWidth()) {
                                DetailItem(
                                    Modifier.weight(1f).drawBehind {
                                        drawLine(gridBorderColor, Offset(size.width, 0f), Offset(size.width, size.height), gridBorderWidth.toPx())
                                    },
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

                            if (hasDescription) {
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 10.dp))
                                Text(
                                    stringResource(R.string.detail_description),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 100.dp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(bottom = 8.dp)
                                ) {
                                    Text(act.description, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            Spacer(modifier = Modifier.height(bottomInset))
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier  = Modifier.align(Alignment.BottomCenter)
            )
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
        modifier              = modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun OsmMapView(
    context: Context,
    gpsPoints: List<GeoPoint>,
    routeColor: Color,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    AndroidView(
        factory = {
            Configuration.getInstance().userAgentValue = context.packageName
            MapView(context).apply {
                setBackgroundColor(
                    android.graphics.Color.argb(
                        (surfaceColor.alpha * 255).toInt(),
                        (surfaceColor.red   * 255).toInt(),
                        (surfaceColor.green * 255).toInt(),
                        (surfaceColor.blue  * 255).toInt()
                    )
                )
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
                    outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                    outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    outlinePaint.isAntiAlias = true
                }
                overlays.add(polyline)

                if (gpsPoints.isNotEmpty()) {
                    val startIcon = ContextCompat.getDrawable(context, android.R.drawable.presence_online)
                    val endIcon = ContextCompat.getDrawable(context, android.R.drawable.presence_busy)

                    val startMarker = Marker(this).apply {
                        position = gpsPoints.first()
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = startIcon
                        title = "Start"
                    }
                    val endMarker = Marker(this).apply {
                        position = gpsPoints.last()
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = endIcon
                        title = "Cilj"
                    }
                    overlays.add(startMarker)
                    overlays.add(endMarker)

                    post {
                        val panelEstimatedHeightPx = (height * 0.30f).toInt()
                        setMapCenterOffset(0, -panelEstimatedHeightPx / 2)

                        if (gpsPoints.size == 1) {
                            controller.setZoom(15.0)
                            controller.setCenter(gpsPoints.first())
                        } else {
                            zoomToBoundingBox(BoundingBox.fromGeoPoints(gpsPoints), false, 380)
                        }
                    }
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
        modifier = modifier
    )
}

/*
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
} */

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

fun simplifyPath(points: List<GeoPoint>, epsilon: Double = 0.00003): List<GeoPoint> {
    if (points.size < 3) return points
    var maxDist = 0.0
    var index = 0
    val start = points.first()
    val end = points.last()
    for (i in 1 until points.size - 1) {
        val dist = perpendicularDistance(points[i], start, end)
        if (dist > maxDist) { maxDist = dist; index = i }
    }
    return if (maxDist > epsilon) {
        simplifyPath(points.subList(0, index + 1), epsilon).dropLast(1) +
                simplifyPath(points.subList(index, points.size), epsilon)
    } else listOf(start, end)
}

private fun perpendicularDistance(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
    val dx = b.longitude - a.longitude
    val dy = b.latitude - a.latitude
    if (dx == 0.0 && dy == 0.0) return hypot(p.longitude - a.longitude, p.latitude - a.latitude)
    val t = ((p.longitude - a.longitude) * dx + (p.latitude - a.latitude) * dy) / (dx * dx + dy * dy)
    val clampedT = t.coerceIn(0.0, 1.0)
    val projX = a.longitude + clampedT * dx
    val projY = a.latitude + clampedT * dy
    return hypot(p.longitude - projX, p.latitude - projY)
}

fun smoothPath(points: List<GeoPoint>, segmentsPerCurve: Int = 8): List<GeoPoint> {
    if (points.size < 3) return points
    val result = mutableListOf<GeoPoint>()
    val padded = listOf(points.first()) + points + listOf(points.last())
    for (i in 1 until padded.size - 2) {
        val p0 = padded[i - 1]; val p1 = padded[i]; val p2 = padded[i + 1]; val p3 = padded[i + 2]
        for (t in 0 until segmentsPerCurve) {
            val tt = t / segmentsPerCurve.toDouble()
            result.add(catmullRomPoint(p0, p1, p2, p3, tt))
        }
    }
    result.add(points.last())
    return result
}

private fun catmullRomPoint(p0: GeoPoint, p1: GeoPoint, p2: GeoPoint, p3: GeoPoint, t: Double): GeoPoint {
    val t2 = t * t; val t3 = t2 * t
    fun interp(v0: Double, v1: Double, v2: Double, v3: Double) = 0.5 * (
            2 * v1 + (-v0 + v2) * t +
                    (2 * v0 - 5 * v1 + 4 * v2 - v3) * t2 +
                    (-v0 + 3 * v1 - 3 * v2 + v3) * t3
            )
    return GeoPoint(
        interp(p0.latitude, p1.latitude, p2.latitude, p3.latitude),
        interp(p0.longitude, p1.longitude, p2.longitude, p3.longitude)
    )
}