package org.wpfy.carlauncher.feature.navigation

import org.wpfy.carlauncher.location.LocationSample

interface RoutingEngine {
    suspend fun calculateRoute(
        origin: LocationSample,
        destination: NavigationDestination,
    ): Result<RouteSummary>
}
