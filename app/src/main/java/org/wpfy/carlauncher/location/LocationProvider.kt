package org.wpfy.carlauncher.location

import kotlinx.coroutines.flow.Flow

data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double? = null,
    val speedMps: Float? = null,
    val bearingDegrees: Float? = null,
    val timestampMillis: Long,
    val provider: String? = null,
    /** Monotonic time of the fix (SystemClock.elapsedRealtimeNanos); immune to a wrong system clock. */
    val elapsedRealtimeNanos: Long = 0L,
)

data class LocationRequest(
    val minTimeMillis: Long = 1_000L,
    val minDistanceMeters: Float = 0f,
    val allowNetworkFallback: Boolean = true,
)

data class LocationProviderStatus(
    val permissionGranted: Boolean,
    val gpsEnabled: Boolean,
    val networkEnabled: Boolean,
) {
    val anyProviderEnabled: Boolean
        get() = gpsEnabled || networkEnabled
}

interface LocationProvider {
    fun status(): LocationProviderStatus

    fun lastKnownLocation(): LocationSample?

    /**
     * Emits location fixes while at least one requested framework provider is available.
     * Call [status] first when the caller needs to distinguish permission/provider failures.
     */
    fun updates(request: LocationRequest = LocationRequest()): Flow<LocationSample>
}
