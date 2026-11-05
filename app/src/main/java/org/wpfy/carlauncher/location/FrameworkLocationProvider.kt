package org.wpfy.carlauncher.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Location straight from the framework [LocationManager].
 *
 * Head units often report `isProviderEnabled(GPS)` as false (or never flip it) even though the
 * GPS chip delivers fixes to apps. So providers are chosen by what exists on the device
 * (`allProviders`), not by that flag, and they are always registered: GPS (works offline,
 * satellite based) plus the network provider as an extra source. Bearing and speed are derived
 * from consecutive fixes when the receiver does not report them.
 */
class FrameworkLocationProvider(context: Context) : LocationProvider {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun status(): LocationProviderStatus {
        return LocationProviderStatus(
            permissionGranted = hasLocationPermission(),
            gpsEnabled = hasProvider(LocationManager.GPS_PROVIDER),
            networkEnabled = hasProvider(LocationManager.NETWORK_PROVIDER),
        )
    }

    override fun lastKnownLocation(): LocationSample? {
        if (!hasLocationPermission()) return null

        return frameworkProviders(allowNetworkFallback = true)
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull(Location::getTime)
            ?.toSample()
    }

    override fun updates(request: LocationRequest): Flow<LocationSample> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        val providers = frameworkProviders(request.allowNetworkFallback)
        if (providers.isEmpty()) {
            close()
            return@callbackFlow
        }

        val motion = MotionDeriver()
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(motion.toSample(location))
            }

            @Suppress("DEPRECATION")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            // Must be overridden: the interface only got default methods in API 30, so older
            // head units would throw AbstractMethodError when a provider is toggled.
            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }

        var registered = 0
        providers.forEach { provider ->
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    request.minTimeMillis,
                    request.minDistanceMeters,
                    listener,
                    Looper.getMainLooper(),
                )
                registered += 1
            } catch (_: SecurityException) {
                // Permission missing for this provider; try the others.
            } catch (_: IllegalArgumentException) {
                // Provider vanished; try the others.
            }
        }
        if (registered == 0) {
            close()
            return@callbackFlow
        }

        // Deliver whatever the system already knows right away instead of waiting for a new fix.
        lastKnownLocation()?.let { trySend(it) }

        awaitClose {
            runCatching { locationManager.removeUpdates(listener) }
        }
    }.conflate()

    private fun frameworkProviders(allowNetworkFallback: Boolean): List<String> {
        return buildList {
            if (hasFineLocationPermission() && hasProvider(LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (allowNetworkFallback && hasProvider(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }
    }

    private fun hasProvider(provider: String): Boolean {
        return runCatching { locationManager.allProviders.contains(provider) }.getOrDefault(false)
    }

    private fun hasLocationPermission(): Boolean {
        return hasFineLocationPermission() || ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/** Fills in bearing/speed from consecutive fixes when the receiver does not provide them. */
private class MotionDeriver {
    private var anchor: Location? = null

    fun toSample(location: Location): LocationSample {
        var bearing = location.bearing.takeIf { location.hasBearing() && it != 0f }
        var speed = location.speed.takeIf { location.hasSpeed() }

        val previous = anchor
        if (previous == null) {
            anchor = location
        } else {
            val distance = previous.distanceTo(location)
            // Ignore GPS jitter: only trust movement of more than a few metres.
            if (distance > MinMoveMeters) {
                if (bearing == null) bearing = (previous.bearingTo(location) + 360f) % 360f
                if (speed == null) {
                    val seconds = (location.elapsedRealtimeNanos - previous.elapsedRealtimeNanos) / 1e9f
                    if (seconds > 0.2f) speed = distance / seconds
                }
                anchor = location
            }
        }

        return location.toSample().copy(bearingDegrees = bearing, speedMps = speed)
    }

    private companion object {
        const val MinMoveMeters = 3f
    }
}

private fun Location.toSample(): LocationSample {
    return LocationSample(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy.takeIf { hasAccuracy() },
        altitudeMeters = altitude.takeIf { hasAltitude() },
        speedMps = speed.takeIf { hasSpeed() },
        bearingDegrees = bearing.takeIf { hasBearing() },
        timestampMillis = time,
        provider = provider,
        elapsedRealtimeNanos = elapsedRealtimeNanos,
    )
}
