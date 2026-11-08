package org.wpfy.carlauncher.feature.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import kotlin.math.pow
import kotlin.math.ln
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTransformGestures
import android.os.SystemClock
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.wpfy.carlauncher.R
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarShapes
import org.wpfy.carlauncher.feature.navigation.GeoPoint
import org.wpfy.carlauncher.location.FrameworkLocationProvider
import org.wpfy.carlauncher.location.LocationProviderStatus
import org.wpfy.carlauncher.location.LocationRequest
import org.wpfy.carlauncher.location.LocationSample
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Heading-up street map for the home screen navigation card.
 *
 * It follows [FrameworkLocationProvider] fixes, renders cached/downloaded dark tiles with a
 * plain Canvas (no map SDK, no extra dependency), and optionally draws a route polyline coming
 * from a routing engine ([RouteSummary.geometry][org.wpfy.carlauncher.feature.navigation.RouteSummary]).
 * The location puck is drawn by the host card, centred exactly where this map centres the car.
 */
@Composable
fun MiniMap(
    modifier: Modifier = Modifier,
    route: List<GeoPoint> = emptyList(),
    destination: GeoPoint? = null,
) {
    if (LocalInspectionMode.current) return

    val context = LocalContext.current
    var mapPromptHidden by remember { mutableStateOf(MapDownloadPreferences.isPromptHidden(context)) }
    var showMapDownloadDialog by remember { mutableStateOf(false) }
    var showHideConfirmation by remember { mutableStateOf(false) }
    if (showMapDownloadDialog) {
        MapDownloadDialog(onDismiss = { showMapDownloadDialog = false })
    }
    if (showHideConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showHideConfirmation = false },
            title = { Text("اقتراح تحميل الخرائط") },
            text = { Text("إخفاء الاقتراح في المرات القادمة؟ تقدر ترجع له من الإعدادات في أي وقت.") },
            confirmButton = {
                TextButton(onClick = {
                    MapDownloadPreferences.setPromptHidden(context, true)
                    mapPromptHidden = true
                    showHideConfirmation = false
                }) { Text("عدم الإظهار مرة أخرى") }
            },
            dismissButton = {
                TextButton(onClick = { showHideConfirmation = false }) { Text("إلغاء") }
            },
        )
    }
    val density = LocalDensity.current.density
    val location = rememberMiniMapLocation()
    val sample = location.sample?.takeUnless { location.isStale }
    var selectedMode by remember { mutableStateOf<MapDisplayMode?>(null) }
    var waitedForTiles by remember { mutableStateOf(false) }
    LaunchedEffect(sample != null) {
        waitedForTiles = false
        if (sample != null) {
            delay(6_000L)
            waitedForTiles = true
        }
    }

    remember(context) { MapTileStore.init(context) }
    val tileSource by MapTileStore.source.collectAsState()
    val offlineCoverage = sample != null && OfflineMbtiles.installed(context) &&
        sample.longitude in 42.0757..43.4207 && sample.latitude in 16.4054..17.7435
    LaunchedEffect(offlineCoverage) {
        if (offlineCoverage && MapZoomState.zoom > 14.99f) MapZoomState.setZoom(14f)
    }
    val zoomLevel = if (offlineCoverage) MapZoomState.zoom.coerceAtMost(14.99f) else MapZoomState.zoom
    val tileZoom = floor(zoomLevel).toInt().coerceIn(MapStyle.MinZoom.toInt(), MapStyle.MaxZoom.toInt())
    val zoomFraction = (zoomLevel - tileZoom).coerceIn(0f, 0.999f)
    val routePath = remember(route, tileZoom) { buildRoutePath(route, tileZoom) }
    val gpsFix = sample?.provider.equals("gps", ignoreCase = true)
    val tileVersionForMode by MapTileStore.version.collectAsState()
    val centerTileAvailable = if (sample != null) {
        val cx = (lonToWorldX(sample.longitude) * (1 shl tileZoom)).toInt()
        val cy = (latToWorldY(sample.latitude) * (1 shl tileZoom)).toInt()
        tileVersionForMode // Observe tile arrival.
        MapTileStore.peek(tileZoom, cx, cy) != null
    } else false
    val automaticMode = when {
        sample == null -> MapDisplayMode.Decorative
        centerTileAvailable && gpsFix -> MapDisplayMode.Gps
        centerTileAvailable -> MapDisplayMode.Online
        waitedForTiles -> MapDisplayMode.Decorative
        gpsFix -> MapDisplayMode.Gps
        else -> MapDisplayMode.Online
    }
    val displayMode = when (selectedMode) {
        MapDisplayMode.Gps -> if (sample != null && gpsFix) MapDisplayMode.Gps else MapDisplayMode.Decorative
        MapDisplayMode.Online -> if (sample != null) MapDisplayMode.Online else MapDisplayMode.Decorative
        MapDisplayMode.Decorative -> MapDisplayMode.Decorative
        null -> automaticMode
    }
    val decorativeIdle = displayMode == MapDisplayMode.Decorative
    // Continue probing map tiles even after decorative fallback; switch back when they arrive.
    LaunchedEffect(sample?.latitude, sample?.longitude, tileZoom, decorativeIdle) {
        if (sample != null && decorativeIdle && selectedMode == null) {
            val cx = (lonToWorldX(sample.longitude) * (1 shl tileZoom)).toInt()
            val cy = (latToWorldY(sample.latitude) * (1 shl tileZoom)).toInt()
            MapTileStore.request(this, tileZoom, cx, cy)
        }
    }

    // Heading: only follow GPS bearing while actually moving, and always rotate the short way.
    var unwrappedBearing by remember { mutableFloatStateOf(0f) }
    val bearing = sample?.bearingDegrees
    val speed = sample?.speedMps ?: 0f
    LaunchedEffect(bearing, speed) {
        if (bearing != null && speed >= MinSpeedForHeadingMps) {
            val current = ((unwrappedBearing % 360f) + 360f) % 360f
            var delta = bearing - current
            while (delta > 180f) delta -= 360f
            while (delta < -180f) delta += 360f
            unwrappedBearing += delta
        }
    }
    val animatedBearing by animateFloatAsState(
        targetValue = unwrappedBearing,
        animationSpec = tween(durationMillis = 900, easing = LinearOutSlowInEasing),
        label = "mini-map-heading",
    )

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                // Two-finger pinch zoom (the +/- buttons cover touchscreens without multi-touch).
                detectTransformGestures { _, _, gestureZoom, _ ->
                    if (gestureZoom > 0f && gestureZoom != 1f) {
                        MapZoomState.zoomBy(ln(gestureZoom) / ln(2f))
                    }
                }
            },
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val tilePx = MapStyle.TileSizePx * density.coerceIn(1f, 2f) * MapStyle.TileScale * 2f.pow(zoomFraction)
        val radiusPx = hypot(widthPx, heightPx) / 2f
        val radiusTiles = radiusPx / tilePx
        val ring = ceil(radiusTiles).toInt() + 1
        val zoom = tileZoom
        val tilesPerSide = 1 shl zoom
        val worldSize = MapStyle.TileSizePx.toDouble() * tilesPerSide

        if (sample != null && !decorativeIdle) {
            val centerX = lonToWorldX(sample.longitude) * worldSize
            val centerY = latToWorldY(sample.latitude) * worldSize
            val centerTileX = floor(centerX / MapStyle.TileSizePx).toInt()
            val centerTileY = floor(centerY / MapStyle.TileSizePx).toInt()

            // Ask for the tiles around the car, nearest first. Only re-runs on a tile change.
            LaunchedEffect(centerTileX, centerTileY, ring, tileSource, decorativeIdle, zoom) {
                if (decorativeIdle) return@LaunchedEffect
                val maxDistance = radiusTiles + TileRequestMarginTiles
                val wanted = buildList {
                    for (ty in (centerTileY - ring)..(centerTileY + ring)) {
                        if (ty < 0 || ty >= tilesPerSide) continue
                        for (tx in (centerTileX - ring)..(centerTileX + ring)) {
                            val dx = tx - centerTileX.toDouble()
                            val dy = ty - centerTileY.toDouble()
                            val distance = hypot(dx, dy)
                            if (distance <= maxDistance) {
                                add(Triple(tx.mod(tilesPerSide), ty, distance))
                            }
                        }
                    }
                }.sortedBy { it.third }
                wanted.forEach { (x, y, _) -> MapTileStore.request(this, zoom, x, y) }
            }

            val tileVersion = MapTileStore.version.collectAsState()
            val metersPerPx = metersPerTilePixel(sample.latitude, zoom) / (tilePx / MapStyle.TileSizePx)
            val accuracyRadiusPx = ((sample.accuracyMeters ?: 0f) / metersPerPx)
                .toFloat()
                .coerceIn(36f * density, 120f * density)

            Canvas(modifier = Modifier.fillMaxSize()) {
                tileVersion.value // subscribe: redraw whenever a tile finishes loading

                val center = Offset(size.width / 2f, size.height / 2f)
                val pxPerWorld = tilePx / MapStyle.TileSizePx

                rotate(degrees = -animatedBearing, pivot = center) {
                    if (decorativeIdle) {
                        drawLuxuryMap(centerX, centerY, pxPerWorld, radiusPx, density)
                    }
                    val tileSize = IntSize(tilePx.roundToInt() + 1, tilePx.roundToInt() + 1)
                    val tileRows = if (decorativeIdle) 1..0 else (centerTileY - ring)..(centerTileY + ring)
                    for (ty in tileRows) {
                        if (ty < 0 || ty >= tilesPerSide) continue
                        for (tx in (centerTileX - ring)..(centerTileX + ring)) {
                            val left = center.x + ((tx * MapStyle.TileSizePx.toDouble() - centerX) * pxPerWorld).toFloat()
                            val top = center.y + ((ty * MapStyle.TileSizePx.toDouble() - centerY) * pxPerWorld).toFloat()
                            val distance = hypot(left + tilePx / 2f - center.x, top + tilePx / 2f - center.y)
                            if (distance > radiusPx + tilePx * 0.72f) continue

                            val tile = MapTileStore.peek(zoom, tx.mod(tilesPerSide), ty) ?: continue
                            drawImage(
                                image = tile,
                                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                                dstSize = tileSize,
                                colorFilter = when {
                                    MapTileStore.isOfflineCached(zoom, tx.mod(tilesPerSide), ty) -> null
                                    tileSource.needsDarkFilter -> DarkTileFilter
                                    MapStyle.GradeMapTilerTiles -> GradeTileFilter
                                    else -> null
                                },
                            )
                        }
                    }

                    if (routePath != null) {
                        val originLeft = center.x + ((routePath.originX - centerX) * pxPerWorld).toFloat()
                        val originTop = center.y + ((routePath.originY - centerY) * pxPerWorld).toFloat()
                        translate(left = originLeft, top = originTop) {
                            scale(scale = pxPerWorld, pivot = Offset.Zero) {
                                drawPath(
                                    path = routePath.path,
                                    color = CarColors.AccentStrong.copy(alpha = 0.30f),
                                    style = Stroke(
                                        width = 16f * density / pxPerWorld,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round,
                                    ),
                                )
                                drawPath(
                                    path = routePath.path,
                                    color = CarColors.Accent,
                                    style = Stroke(
                                        width = 8f * density / pxPerWorld,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round,
                                    ),
                                )
                            }
                        }
                    }

                    if (destination != null) {
                        val dx = center.x + ((lonToWorldX(destination.longitude) * worldSize - centerX) * pxPerWorld).toFloat()
                        val dy = center.y + ((latToWorldY(destination.latitude) * worldSize - centerY) * pxPerWorld).toFloat()
                        drawCircle(color = Color.White, radius = 13f * density, center = Offset(dx, dy))
                        drawCircle(color = CarColors.AccentStrong, radius = 8f * density, center = Offset(dx, dy))
                    }
                }

                // Soft position halo under the host card's puck (accuracy-sized, unrotated).
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CarColors.AccentStrong.copy(alpha = 0.32f),
                            CarColors.AccentStrong.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = accuracyRadiusPx,
                    ),
                    radius = accuracyRadiusPx,
                    center = center,
                )
            }
        } else if (decorativeIdle) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLuxuryMap(5_000_000.0, 5_000_000.0, tilePx / MapStyle.TileSizePx, radiusPx, density)
            }
        }

        // Edge fades so the map melts into the card instead of ending abruptly.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(CarColors.Surface.copy(alpha = 0.92f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, CarColors.Surface.copy(alpha = 0.92f)),
                    ),
                ),
        )

        Row(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MapDisplayMode.entries.forEach { mode ->
                val active = displayMode == mode
                Surface(
                    modifier = Modifier.size(if (active) 25.dp else 21.dp)
                        .clickable { selectedMode = mode },
                    shape = CircleShape,
                    color = if (active) Color(0xFF3989F7) else Color(0xC9232B36),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (active) Color.White.copy(alpha = 0.7f)
                        else Color.White.copy(alpha = 0.2f),
                    ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when (mode) {
                                MapDisplayMode.Gps -> "G"
                                MapDisplayMode.Online -> "N"
                                MapDisplayMode.Decorative -> "D"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        if (decorativeIdle) {
            Text(
                text = "خريطة توضيحية",
                modifier = Modifier.align(Alignment.BottomStart).padding(9.dp),
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall,
            )
        }

        if (!mapPromptHidden && selectedMode == null) {
            Row(
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = { showMapDownloadDialog = true },
                    color = Color(0xE51B2B3E),
                    shape = CarShapes.extraSmall,
                ) {
                    Text(
                        text = "↓ تحميل خرائط السعودية",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Surface(
                    onClick = { showHideConfirmation = true },
                    color = Color(0xC91B2B3E),
                    shape = CarShapes.extraSmall,
                ) {
                    Text(
                        text = "×",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ZoomButton(icon = Icons.Rounded.Add, label = "+") { MapZoomState.zoomBy(1f) }
            ZoomButton(icon = Icons.Rounded.Remove, label = "-") { MapZoomState.zoomBy(-1f) }
        }

        if (sample != null && !decorativeIdle) {
            Text(
                text = tileSource.attribution,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = CarColors.TextDisabled,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private enum class MapDisplayMode { Gps, Online, Decorative }

private data class MiniMapLocation(
    val sample: LocationSample?,
    val status: LocationProviderStatus,
    val nowElapsedNanos: Long,
) {
    val isStale: Boolean
        get() = sample == null || sample.ageMillis(nowElapsedNanos) > StaleFixMillis
}

/** Fix age on the monotonic clock (a head unit's wall clock is often wrong). */
private fun LocationSample.ageMillis(nowElapsedNanos: Long): Long {
    return if (elapsedRealtimeNanos > 0L) {
        (nowElapsedNanos - elapsedRealtimeNanos) / 1_000_000L
    } else {
        System.currentTimeMillis() - timestampMillis
    }
}

@Composable
private fun MiniMapLocation.statusMessage(): String? {
    return when {
        !status.permissionGranted -> "فعّل إذن الموقع عشان تشتغل الخريطة"
        !status.anyProviderEnabled -> "ما لقينا GPS، تأكد من اتصاله"
        isStale -> "ندور على إشارة GPS..."
        else -> null
    }
}

@Composable
private fun rememberMiniMapLocation(): MiniMapLocation {
    val context = LocalContext.current
    val provider = remember(context) { FrameworkLocationProvider(context) }
    var status by remember(provider) { mutableStateOf(provider.status()) }
    var sample by remember(provider) { mutableStateOf(provider.lastKnownLocation()) }
    var now by remember(provider) { mutableLongStateOf(SystemClock.elapsedRealtimeNanos()) }

    // Permission / provider state can change while the launcher is open (e.g. after onboarding).
    LaunchedEffect(provider) {
        while (isActive) {
            val current = provider.status()
            if (current != status) status = current
            now = SystemClock.elapsedRealtimeNanos()
            delay(StatusPollMillis)
        }
    }

    val canTrack = status.permissionGranted && status.anyProviderEnabled
    LaunchedEffect(provider, canTrack) {
        if (!canTrack) return@LaunchedEffect
        if (sample == null) sample = provider.lastKnownLocation()
        provider
            .updates(LocationRequest(minTimeMillis = 1_000L, minDistanceMeters = 0f))
            .collect { fix ->
                sample = fix
                now = SystemClock.elapsedRealtimeNanos()
            }
    }

    return MiniMapLocation(sample = sample, status = status, nowElapsedNanos = now)
}

private const val MinSpeedForHeadingMps = 1.5f
private const val StatusPollMillis = 5_000L
private const val StaleFixMillis = 120_000L
private const val TileRequestMarginTiles = 1.5

/** Light OSM tiles -> dark map (invert + hue-rotate 180deg + small lift so roads stay visible). */
private val DarkTileFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.574f, -1.430f, -0.144f, 0f, 283f,
            -0.426f, -0.430f, -0.144f, 0f, 283f,
            -0.426f, -1.430f, 0.856f, 0f, 283f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

private class RoutePath(
    val path: Path,
    val originX: Double,
    val originY: Double,
)

/** Builds the route once, relative to its first point, so drawing only needs a translate/scale. */
private fun buildRoutePath(route: List<GeoPoint>, zoom: Int): RoutePath? {
    if (route.size < 2) return null
    val worldSize = MapStyle.TileSizePx.toDouble() * (1 shl zoom)
    val originX = lonToWorldX(route[0].longitude) * worldSize
    val originY = latToWorldY(route[0].latitude) * worldSize
    val path = Path()
    var lastX = 0f
    var lastY = 0f
    route.forEachIndexed { index, point ->
        val x = (lonToWorldX(point.longitude) * worldSize - originX).toFloat()
        val y = (latToWorldY(point.latitude) * worldSize - originY).toFloat()
        if (index == 0) {
            path.moveTo(x, y)
            lastX = x
            lastY = y
        } else if (index == route.lastIndex || hypot(x - lastX, y - lastY) >= MinRouteSegmentPx) {
            path.lineTo(x, y)
            lastX = x
            lastY = y
        }
    }
    return RoutePath(path, originX, originY)
}

private const val MinRouteSegmentPx = 3f

private fun luxuryHash(a: Int, b: Int): Int {
    var h = a * 374761393 + b * 668265263 + 1013904223
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return h and 0x7fffffff
}

private fun luxuryJitter(index: Int, salt: Int, amplitude: Double): Double {
    return ((luxuryHash(index, salt) % 1000) / 1000.0 - 0.5) * 2.0 * amplitude
}

private fun luxuryTrafficColor(hash: Int): Color = when (hash % 10) {
    in 0..5 -> Color(0xFF2FBF71)
    6, 7 -> Color(0xFFFFA63D)
    8 -> Color(0xFFFF5A52)
    else -> Color(0xFF5B8DEF)
}

/**
 * Decorative dark city map drawn procedurally from world coordinates, so it scrolls and rotates
 * with the car exactly like a real map but needs no data. Streets are generated, not real.
 */
private fun DrawScope.drawLuxuryMap(
    centerWorldX: Double,
    centerWorldY: Double,
    pxPerWorld: Float,
    radiusPx: Float,
    density: Float,
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val k = pxPerWorld.toDouble()
    val reach = radiusPx / k + 60.0
    val top = center.y - radiusPx * 1.2f
    val bottom = center.y + radiusPx * 1.2f
    val left = center.x - radiusPx * 1.2f
    val right = center.x + radiusPx * 1.2f

    drawRect(color = Color(0xFF0D1117))

    // Parks / water patches.
    val cell = 400.0
    for (ci in floor((centerWorldX - reach) / cell).toInt()..floor((centerWorldX + reach) / cell).toInt()) {
        for (cj in floor((centerWorldY - reach) / cell).toInt()..floor((centerWorldY + reach) / cell).toInt()) {
            val h = luxuryHash(ci, cj)
            if (h % 5 != 0) continue
            val x = center.x + (((ci + 0.12 + (h % 40) / 100.0) * cell - centerWorldX) * k).toFloat()
            val y = center.y + (((cj + 0.15 + ((h / 40) % 40) / 100.0) * cell - centerWorldY) * k).toFloat()
            drawRoundRect(
                color = if (h % 10 == 0) Color(0xFF0E2631) else Color(0xFF10261F),
                topLeft = Offset(x, y),
                size = Size((cell * 0.5 * k).toFloat(), (cell * 0.38 * k).toFloat()),
                cornerRadius = CornerRadius(28f * density),
            )
        }
    }

    // Minor streets (grid with jitter) and traffic-coloured avenues every 4th line.
    val gap = 84.0
    val segment = 240.0
    val minor = Color(0xFF222A35)
    val firstX = floor((centerWorldX - reach) / gap).toInt()
    val lastX = ceil((centerWorldX + reach) / gap).toInt()
    for (i in firstX..lastX) {
        val worldX = i * gap + luxuryJitter(i, 1, 12.0)
        val x = center.x + ((worldX - centerWorldX) * k).toFloat()
        if (i % 4 != 0) {
            drawLine(minor, Offset(x, top), Offset(x, bottom), strokeWidth = 2.5f * density)
        } else {
            for (j in floor((centerWorldY - reach) / segment).toInt()..ceil((centerWorldY + reach) / segment).toInt()) {
                val y1 = center.y + ((j * segment - centerWorldY) * k).toFloat()
                val y2 = center.y + (((j + 1) * segment - centerWorldY) * k).toFloat()
                val color = luxuryTrafficColor(luxuryHash(i, j))
                drawLine(color.copy(alpha = 0.22f), Offset(x, y1), Offset(x, y2), strokeWidth = 13f * density, cap = StrokeCap.Round)
                drawLine(color, Offset(x, y1), Offset(x, y2), strokeWidth = 5f * density, cap = StrokeCap.Round)
            }
        }
    }
    val firstY = floor((centerWorldY - reach) / gap).toInt()
    val lastY = ceil((centerWorldY + reach) / gap).toInt()
    for (j in firstY..lastY) {
        val worldY = j * gap + luxuryJitter(j, 2, 12.0)
        val y = center.y + ((worldY - centerWorldY) * k).toFloat()
        if (j % 4 != 0) {
            drawLine(minor, Offset(left, y), Offset(right, y), strokeWidth = 2.5f * density)
        } else {
            for (i in floor((centerWorldX - reach) / segment).toInt()..ceil((centerWorldX + reach) / segment).toInt()) {
                val x1 = center.x + ((i * segment - centerWorldX) * k).toFloat()
                val x2 = center.x + (((i + 1) * segment - centerWorldX) * k).toFloat()
                val color = luxuryTrafficColor(luxuryHash(j + 4096, i))
                drawLine(color.copy(alpha = 0.22f), Offset(x1, y), Offset(x2, y), strokeWidth = 13f * density, cap = StrokeCap.Round)
                drawLine(color, Offset(x1, y), Offset(x2, y), strokeWidth = 5f * density, cap = StrokeCap.Round)
            }
        }
    }

    // A few diagonal expressways.
    val angle = 0.55
    val dirX = cos(angle)
    val dirY = sin(angle)
    val normalX = -dirY
    val normalY = dirX
    val diagonalGap = 340.0
    val centerOffset = normalX * centerWorldX + normalY * centerWorldY
    val firstM = floor((centerOffset - reach) / diagonalGap).toInt()
    val lastM = ceil((centerOffset + reach) / diagonalGap).toInt()
    for (m in firstM..lastM) {
        val h = luxuryHash(m, 77)
        if (h % 3 != 0) continue
        val distance = (m * diagonalGap + luxuryJitter(m, 5, 40.0) - centerOffset) * k
        val baseX = center.x + (normalX * distance).toFloat()
        val baseY = center.y + (normalY * distance).toFloat()
        val span = radiusPx * 1.7f
        val start = Offset(baseX - (dirX * span).toFloat(), baseY - (dirY * span).toFloat())
        val end = Offset(baseX + (dirX * span).toFloat(), baseY + (dirY * span).toFloat())
        val color = if (h % 2 == 0) Color(0xFF5B8DEF) else Color(0xFFFFA63D)
        drawLine(color.copy(alpha = 0.20f), start, end, strokeWidth = 15f * density, cap = StrokeCap.Round)
        drawLine(color, start, end, strokeWidth = 6f * density, cap = StrokeCap.Round)
    }

    // Points of interest.
    val pinColors = listOf(Color(0xFFE879F9), Color(0xFFFFB74D), Color(0xFF4DD0E1))
    for (ci in floor((centerWorldX - reach) / cell).toInt()..floor((centerWorldX + reach) / cell).toInt()) {
        for (cj in floor((centerWorldY - reach) / cell).toInt()..floor((centerWorldY + reach) / cell).toInt()) {
            val h = luxuryHash(ci + 911, cj + 533)
            if (h % 3 != 1) continue
            val x = center.x + (((ci + 0.2 + (h % 60) / 100.0) * cell - centerWorldX) * k).toFloat()
            val y = center.y + (((cj + 0.2 + ((h / 60) % 60) / 100.0) * cell - centerWorldY) * k).toFloat()
            val color = pinColors[(h / 7) % pinColors.size]
            drawCircle(color.copy(alpha = 0.25f), radius = 17f * density, center = Offset(x, y))
            drawCircle(color, radius = 9f * density, center = Offset(x, y))
            drawCircle(Color.White, radius = 3.5f * density, center = Offset(x, y))
        }
    }
}

@Composable
private fun ZoomButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        color = CarColors.Background.copy(alpha = 0.78f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = Color.White,
            )
        }
    }
}

/** Neutral graphite grade: removes the blue cast of the source while retaining readable roads. */
private val GradeTileFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.255f, 0.500f, 0.095f, 0f, 5f,
            0.260f, 0.510f, 0.100f, 0f, 8f,
            0.275f, 0.530f, 0.105f, 0f, 12f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)
