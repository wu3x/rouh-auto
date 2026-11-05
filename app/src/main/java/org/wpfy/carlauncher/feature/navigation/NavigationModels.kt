package org.wpfy.carlauncher.feature.navigation

data class NavigationDestination(
    val label: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

data class RouteSummary(
    val destination: NavigationDestination,
    val totalDistanceMeters: Double? = null,
    val estimatedDurationSeconds: Long? = null,
    /** Optional route polyline; when a [RoutingEngine] fills it the mini map draws it. */
    val geometry: List<GeoPoint> = emptyList(),
)

data class Maneuver(
    val type: ManeuverType,
    val instruction: String,
    val roadName: String? = null,
    val distanceMeters: Double? = null,
)

enum class ManeuverType {
    Continue,
    TurnLeft,
    TurnRight,
    SlightLeft,
    SlightRight,
    SharpLeft,
    SharpRight,
    UTurn,
    Merge,
    ExitLeft,
    ExitRight,
    Roundabout,
    Arrive,
    Unknown,
}

data class NavigationProgress(
    val currentRoad: String? = null,
    val nextManeuver: Maneuver? = null,
    val secondaryManeuver: Maneuver? = null,
    val remainingDistanceMeters: Double? = null,
    val remainingDurationSeconds: Long? = null,
    val etaEpochMillis: Long? = null,
    val speedMps: Double? = null,
    val headingDegrees: Double? = null,
    val rerouting: Boolean = false,
    val arrived: Boolean = false,
)

enum class NavigationError {
    ProviderUnavailable,
    RouteUnavailable,
    PermissionDenied,
    Unknown,
}

data class NavigationState(
    val active: Boolean = false,
    val destination: NavigationDestination? = null,
    val routeSummary: RouteSummary? = null,
    val progress: NavigationProgress = NavigationProgress(),
    val error: NavigationError? = null,
    /** Short technical reason shown under the error (e.g. which routing server failed). */
    val errorDetail: String? = null,
    val gpsAvailable: Boolean = true,
    /** True while the route to [destination] is being calculated. */
    val calculating: Boolean = false,
)
