package org.wpfy.carlauncher.feature.navigation

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.wpfy.carlauncher.R
import org.wpfy.carlauncher.location.FrameworkLocationProvider
import org.wpfy.carlauncher.location.LocationRequest
import org.wpfy.carlauncher.location.LocationSample
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Process-wide navigation session: destination search, route calculation (OSRM), live progress
 * from GPS fixes, off-route detection with automatic re-routing, and arrival detection.
 * It survives screen changes inside the launcher; GPS is only tracked while a route is active.
 */
class NavigationController private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val locationProvider = FrameworkLocationProvider(appContext)

    private val _state = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = _state.asStateFlow()

    private var tracker: RouteTracker? = null
    private var summary: RouteSummary? = null
    private var routingJob: Job? = null
    private var trackingJob: Job? = null
    private var offRouteFixes = 0
    private var lastRerouteAtMillis = 0L
    private var rerouting = false

    fun lastKnownLocation(): LocationSample? = locationProvider.lastKnownLocation()

    suspend fun search(query: String): Result<List<NavigationDestination>> {
        val near = locationProvider.lastKnownLocation()
        return withContext(Dispatchers.IO) {
            runCatching { DestinationSearch.search(query, near) }
        }
    }

    fun start(destination: NavigationDestination) {
        val latitude = destination.latitude
        val longitude = destination.longitude
        stopInternal()
        if (latitude == null || longitude == null) {
            _state.value = NavigationState(destination = destination, error = NavigationError.RouteUnavailable)
            return
        }
        if (!locationProvider.status().permissionGranted) {
            _state.value = NavigationState(destination = destination, error = NavigationError.PermissionDenied)
            return
        }

        _state.value = NavigationState(destination = destination, calculating = true)
        routingJob = scope.launch {
            val origin = awaitOrigin()
            if (origin == null) {
                _state.value = NavigationState(destination = destination, gpsAvailable = false)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    RoutePlanner.route(
                        GeoPoint(origin.latitude, origin.longitude),
                        GeoPoint(latitude, longitude),
                    )
                }
            }
            result.onSuccess { route ->
                installRoute(route, destination)
                startTracking()
            }.onFailure { error ->
                Log.w(LogTag, "Route calculation failed", error)
                _state.value = NavigationState(
                    destination = destination,
                    error = NavigationError.RouteUnavailable,
                    errorDetail = error.message?.take(180),
                )
            }
        }
    }

    fun stop() {
        stopInternal()
        _state.value = NavigationState()
    }

    private fun stopInternal() {
        routingJob?.cancel()
        trackingJob?.cancel()
        routingJob = null
        trackingJob = null
        tracker = null
        summary = null
        offRouteFixes = 0
        rerouting = false
    }

    private suspend fun awaitOrigin(): LocationSample? {
        val known = locationProvider.lastKnownLocation()
        if (known != null && known.ageMillis() < FreshFixMillis) return known
        if (!locationProvider.status().anyProviderEnabled) return known
        return withTimeoutOrNull(FixTimeoutMillis) {
            locationProvider.updates(LocationRequest(minTimeMillis = 1_000L, minDistanceMeters = 0f)).first()
        } ?: known
    }

    private fun installRoute(route: ComputedRoute, destination: NavigationDestination) {
        val newTracker = RouteTracker(route)
        tracker = newTracker
        summary = RouteSummary(
            destination = destination,
            totalDistanceMeters = route.distanceMeters,
            estimatedDurationSeconds = route.durationSeconds.toLong(),
            geometry = route.points,
        )
        offRouteFixes = 0
        rerouting = false
        publish(newTracker, newTracker.snap(route.points.first().latitude, route.points.first().longitude), null)
    }

    private fun startTracking() {
        trackingJob?.cancel()
        trackingJob = scope.launch {
            locationProvider
                .updates(LocationRequest(minTimeMillis = 1_000L, minDistanceMeters = 3f))
                .collect(::onLocation)
        }
    }

    private fun onLocation(sample: LocationSample) {
        val currentTracker = tracker ?: return
        val destination = _state.value.destination ?: return
        val snap = currentTracker.snap(sample.latitude, sample.longitude)

        val remaining = (currentTracker.totalMeters - snap.alongMeters).coerceAtLeast(0.0)
        val straightToDestination = destination.latitude?.let { lat ->
            destination.longitude?.let { lon ->
                haversineMeters(sample.latitude, sample.longitude, lat, lon)
            }
        }
        if (remaining < ArrivalMeters || (straightToDestination != null && straightToDestination < ArrivalMeters)) {
            trackingJob?.cancel()
            tracker = null
            summary = null
            _state.value = NavigationState(
                active = false,
                destination = destination,
                progress = NavigationProgress(arrived = true),
            )
            return
        }

        if (snap.offRouteMeters > OffRouteMeters) {
            offRouteFixes += 1
            val now = System.currentTimeMillis()
            if (offRouteFixes >= OffRouteFixesBeforeReroute && !rerouting && now - lastRerouteAtMillis > RerouteCooldownMillis) {
                reroute(sample, destination)
            }
        } else {
            offRouteFixes = 0
        }

        publish(currentTracker, snap, sample)
    }

    private fun reroute(sample: LocationSample, destination: NavigationDestination) {
        val latitude = destination.latitude ?: return
        val longitude = destination.longitude ?: return
        rerouting = true
        lastRerouteAtMillis = System.currentTimeMillis()
        routingJob?.cancel()
        routingJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    RoutePlanner.route(
                        GeoPoint(sample.latitude, sample.longitude),
                        GeoPoint(latitude, longitude),
                    )
                }
            }
            result.onSuccess { route -> installRoute(route, destination) }
                .onFailure { error ->
                    Log.w(LogTag, "Re-route failed", error)
                    rerouting = false
                }
        }
    }

    private fun publish(currentTracker: RouteTracker, snap: RouteSnap, sample: LocationSample?) {
        val route = currentTracker.route
        val remaining = (currentTracker.totalMeters - snap.alongMeters).coerceAtLeast(0.0)
        val fraction = if (currentTracker.totalMeters > 0.0) remaining / currentTracker.totalMeters else 0.0
        val remainingSeconds = (route.durationSeconds * fraction).toLong()

        val steps = route.steps
        val stepScale = if (route.distanceMeters > 0.0) currentTracker.totalMeters / route.distanceMeters else 1.0
        val nextIndex = steps.indexOfFirst { it.startAlongMeters * stepScale > snap.alongMeters + ManeuverLookaheadMinMeters }
        val currentRoad = steps.lastOrNull { it.startAlongMeters * stepScale <= snap.alongMeters }?.roadName

        fun maneuverAt(index: Int): Maneuver? {
            val step = steps.getOrNull(index) ?: return null
            return Maneuver(
                type = step.type,
                instruction = instructionFor(step.type),
                roadName = step.roadName,
                distanceMeters = (step.startAlongMeters * stepScale - snap.alongMeters).coerceAtLeast(0.0),
            )
        }

        _state.value = NavigationState(
            active = true,
            destination = _state.value.destination ?: summary?.destination,
            routeSummary = summary,
            progress = NavigationProgress(
                currentRoad = currentRoad,
                nextManeuver = if (nextIndex >= 0) maneuverAt(nextIndex) else null,
                secondaryManeuver = if (nextIndex >= 0) maneuverAt(nextIndex + 1) else null,
                remainingDistanceMeters = remaining,
                remainingDurationSeconds = remainingSeconds,
                etaEpochMillis = System.currentTimeMillis() + remainingSeconds * 1_000L,
                speedMps = sample?.speedMps?.toDouble(),
                headingDegrees = sample?.bearingDegrees?.toDouble(),
                rerouting = rerouting,
            ),
        )
    }

    private fun instructionFor(type: ManeuverType): String {
        val id = when (type) {
            ManeuverType.Continue, ManeuverType.Unknown -> R.string.maneuver_continue
            ManeuverType.TurnLeft -> R.string.maneuver_turn_left
            ManeuverType.TurnRight -> R.string.maneuver_turn_right
            ManeuverType.SlightLeft -> R.string.maneuver_slight_left
            ManeuverType.SlightRight -> R.string.maneuver_slight_right
            ManeuverType.SharpLeft -> R.string.maneuver_sharp_left
            ManeuverType.SharpRight -> R.string.maneuver_sharp_right
            ManeuverType.UTurn -> R.string.maneuver_uturn
            ManeuverType.Merge -> R.string.maneuver_merge
            ManeuverType.ExitLeft -> R.string.maneuver_exit_left
            ManeuverType.ExitRight -> R.string.maneuver_exit_right
            ManeuverType.Roundabout -> R.string.maneuver_roundabout
            ManeuverType.Arrive -> R.string.maneuver_arrive
        }
        return appContext.getString(id)
    }

    companion object {
        @Volatile
        private var instance: NavigationController? = null

        fun get(context: Context): NavigationController {
            return instance ?: synchronized(this) {
                instance ?: NavigationController(context).also { instance = it }
            }
        }

        private const val LogTag = "CarLauncherNav"
        private const val FreshFixMillis = 5 * 60_000L
        private const val FixTimeoutMillis = 12_000L
        private const val ArrivalMeters = 35.0
        private const val OffRouteMeters = 60.0
        private const val OffRouteFixesBeforeReroute = 3
        private const val RerouteCooldownMillis = 12_000L
        private const val ManeuverLookaheadMinMeters = 8.0
    }
}

internal data class RouteSnap(
    val alongMeters: Double,
    val offRouteMeters: Double,
    val index: Int,
)

/** Matches GPS fixes onto the route polyline (nearest segment, searching forward first). */
internal class RouteTracker(val route: ComputedRoute) {
    private val size = route.points.size
    private val latitudes = DoubleArray(size) { route.points[it].latitude }
    private val longitudes = DoubleArray(size) { route.points[it].longitude }
    private val cumulative = DoubleArray(size)
    private var lastIndex = 0

    val totalMeters: Double

    init {
        for (i in 1 until size) {
            cumulative[i] = cumulative[i - 1] +
                haversineMeters(latitudes[i - 1], longitudes[i - 1], latitudes[i], longitudes[i])
        }
        totalMeters = cumulative[size - 1]
    }

    fun snap(latitude: Double, longitude: Double): RouteSnap {
        val last = size - 2
        var best = search(latitude, longitude, max(0, lastIndex - 3), min(last, lastIndex + 250))
        if (best.offRouteMeters > 60.0) {
            val global = search(latitude, longitude, 0, last)
            if (global.offRouteMeters < best.offRouteMeters) best = global
        }
        lastIndex = best.index
        return best
    }

    private fun search(latitude: Double, longitude: Double, from: Int, to: Int): RouteSnap {
        val metersPerLat = 110_540.0
        val metersPerLon = 111_320.0 * cos(Math.toRadians(latitude))
        var bestDistance = Double.MAX_VALUE
        var bestIndex = from
        var bestT = 0.0

        for (i in from..to) {
            val ax = (longitudes[i] - longitude) * metersPerLon
            val ay = (latitudes[i] - latitude) * metersPerLat
            val bx = (longitudes[i + 1] - longitude) * metersPerLon
            val by = (latitudes[i + 1] - latitude) * metersPerLat
            val dx = bx - ax
            val dy = by - ay
            val lengthSquared = dx * dx + dy * dy
            val t = if (lengthSquared <= 0.0) 0.0 else ((-ax * dx) + (-ay * dy)) / lengthSquared
            val clamped = t.coerceIn(0.0, 1.0)
            val px = ax + clamped * dx
            val py = ay + clamped * dy
            val distance = sqrt(px * px + py * py)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = i
                bestT = clamped
            }
        }

        val along = cumulative[bestIndex] + bestT * (cumulative[bestIndex + 1] - cumulative[bestIndex])
        return RouteSnap(alongMeters = along, offRouteMeters = bestDistance, index = bestIndex)
    }
}

internal fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    return 2 * earthRadius * kotlin.math.asin(sqrt(a))
}

/** Age of a fix using the monotonic clock, so a wrong head-unit clock cannot make it look stale. */
private fun LocationSample.ageMillis(): Long {
    return if (elapsedRealtimeNanos > 0L) {
        (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L
    } else {
        System.currentTimeMillis() - timestampMillis
    }
}
