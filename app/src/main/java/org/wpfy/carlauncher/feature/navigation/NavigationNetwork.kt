package org.wpfy.carlauncher.feature.navigation

import android.net.Uri
import org.json.JSONObject
import org.wpfy.carlauncher.feature.map.MapStyle
import org.wpfy.carlauncher.location.LocationSample
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Blocking HTTP GET; call from a background dispatcher. */
internal fun httpGetText(url: String, timeoutMillis: Int = 10_000): String {
    var connection: HttpURLConnection? = null
    try {
        connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = timeoutMillis
            setRequestProperty("User-Agent", MapStyle.UserAgent)
            setRequestProperty("Accept", "application/json")
        }
        val code = connection.responseCode
        if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection?.disconnect()
    }
}

/** Blocking JSON POST; call from a background dispatcher. */
internal fun httpPostJson(url: String, body: String, timeoutMillis: Int = 20_000): String {
    var connection: HttpURLConnection? = null
    try {
        connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8_000
            readTimeout = timeoutMillis
            setRequestProperty("User-Agent", MapStyle.UserAgent)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection?.disconnect()
    }
}

/** Destination search using OpenStreetMap Nominatim. Call from a background dispatcher. */
internal object DestinationSearch {
    fun search(query: String, near: LocationSample?): List<NavigationDestination> {
        val text = query.trim()
        if (text.length < 2) return emptyList()
        return searchNominatim(text, near)
    }

    private fun searchNominatim(query: String, near: LocationSample?): List<NavigationDestination> {
        val viewbox = near?.let {
            "&viewbox=${it.longitude - 1.0},${it.latitude + 1.0},${it.longitude + 1.0},${it.latitude - 1.0}"
        }.orEmpty()
        val url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=8" +
            "&accept-language=ar,en&q=${Uri.encode(query)}$viewbox"
        val items = org.json.JSONArray(httpGetText(url))

        val results = ArrayList<NavigationDestination>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val latitude = item.optString("lat").toDoubleOrNull() ?: continue
            val longitude = item.optString("lon").toDoubleOrNull() ?: continue
            val displayName = item.optString("display_name")
            val label = item.optString("name").ifBlank { displayName.substringBefore(",") }
            if (label.isBlank()) continue
            results += NavigationDestination(
                label = label,
                address = displayName.takeIf { it.isNotBlank() && it != label },
                latitude = latitude,
                longitude = longitude,
            )
        }
        return results
    }
}

internal data class RouteStep(
    val type: ManeuverType,
    val roadName: String?,
    /** Distance along the route (metres) at which this manoeuvre happens. */
    val startAlongMeters: Double,
)

internal data class ComputedRoute(
    val points: List<GeoPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val steps: List<RouteStep>,
)

/** Driving routes from OSRM servers (public demo server first, OSM Germany as a fallback). */
internal object OsrmRouter {
    private val servers = listOf(
        "https://router.project-osrm.org",
        "https://routing.openstreetmap.de/routed-car",
    )

    fun route(origin: GeoPoint, destination: GeoPoint): ComputedRoute {
        val coordinates = "${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}"
        val failures = ArrayList<String>()
        for (server in servers) {
            try {
                val url = "$server/route/v1/driving/$coordinates" +
                    "?overview=full&geometries=geojson&steps=true&alternatives=false"
                return parse(httpGetText(url, timeoutMillis = 25_000))
            } catch (error: Exception) {
                failures += "${server.substringAfter("://").substringBefore("/")}: ${shortReason(error)}"
            }
        }
        throw IOException(failures.joinToString(" | "))
    }

    private fun parse(body: String): ComputedRoute {
        val root = JSONObject(body)
        if (root.optString("code") != "Ok") throw IOException("Routing failed: ${root.optString("code")}")
        val route = root.getJSONArray("routes").getJSONObject(0)

        val coordinates = route.getJSONObject("geometry").getJSONArray("coordinates")
        val points = ArrayList<GeoPoint>(coordinates.length())
        for (index in 0 until coordinates.length()) {
            val pair = coordinates.getJSONArray(index)
            points += GeoPoint(latitude = pair.getDouble(1), longitude = pair.getDouble(0))
        }
        if (points.size < 2) throw IOException("Route has no geometry")

        val steps = ArrayList<RouteStep>()
        var along = 0.0
        val legs = route.optJSONArray("legs")
        if (legs != null) {
            for (legIndex in 0 until legs.length()) {
                val legSteps = legs.getJSONObject(legIndex).optJSONArray("steps") ?: continue
                for (stepIndex in 0 until legSteps.length()) {
                    val step = legSteps.getJSONObject(stepIndex)
                    val maneuver = step.optJSONObject("maneuver")
                    steps += RouteStep(
                        type = mapManeuver(maneuver?.optString("type").orEmpty(), maneuver?.optString("modifier").orEmpty()),
                        roadName = step.optString("name").takeIf { it.isNotBlank() },
                        startAlongMeters = along,
                    )
                    along += step.optDouble("distance", 0.0)
                }
            }
        }

        return ComputedRoute(
            points = points,
            distanceMeters = route.getDouble("distance"),
            durationSeconds = route.getDouble("duration"),
            steps = steps,
        )
    }

    private fun mapManeuver(type: String, modifier: String): ManeuverType {
        return when (type) {
            "arrive" -> ManeuverType.Arrive
            "depart" -> ManeuverType.Continue
            "roundabout", "rotary", "roundabout turn" -> ManeuverType.Roundabout
            "merge" -> ManeuverType.Merge
            "off ramp" -> when {
                modifier.contains("left") -> ManeuverType.ExitLeft
                modifier.contains("right") -> ManeuverType.ExitRight
                else -> ManeuverType.Continue
            }
            else -> when (modifier) {
                "uturn" -> ManeuverType.UTurn
                "sharp left" -> ManeuverType.SharpLeft
                "left" -> ManeuverType.TurnLeft
                "slight left" -> ManeuverType.SlightLeft
                "slight right" -> ManeuverType.SlightRight
                "right" -> ManeuverType.TurnRight
                "sharp right" -> ManeuverType.SharpRight
                else -> ManeuverType.Continue
            }
        }
    }
}

internal fun shortReason(error: Throwable): String {
    val text = error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName
    return text.take(60)
}

/** Valhalla (OpenStreetMap Germany) as a third routing source; used if both OSRM servers fail. */
internal object ValhallaRouter {
    private const val Server = "https://valhalla1.openstreetmap.de/route"

    fun route(origin: GeoPoint, destination: GeoPoint): ComputedRoute {
        val request = JSONObject()
            .put(
                "locations",
                org.json.JSONArray()
                    .put(JSONObject().put("lat", origin.latitude).put("lon", origin.longitude).put("type", "break"))
                    .put(JSONObject().put("lat", destination.latitude).put("lon", destination.longitude).put("type", "break")),
            )
            .put("costing", "auto")
            .put("directions_options", JSONObject().put("units", "kilometers").put("language", "en-US"))
        val trip = JSONObject(httpPostJson(Server, request.toString())).getJSONObject("trip")
        val summary = trip.getJSONObject("summary")
        val leg = trip.getJSONArray("legs").getJSONObject(0)

        val points = decodePolyline6(leg.getString("shape"))
        if (points.size < 2) throw IOException("Valhalla: empty shape")

        val steps = ArrayList<RouteStep>()
        var along = 0.0
        val maneuvers = leg.optJSONArray("maneuvers")
        if (maneuvers != null) {
            for (index in 0 until maneuvers.length()) {
                val maneuver = maneuvers.getJSONObject(index)
                val street = maneuver.optJSONArray("street_names")?.optString(0)?.takeIf { it.isNotBlank() }
                steps += RouteStep(
                    type = mapType(maneuver.optInt("type", 0)),
                    roadName = street,
                    startAlongMeters = along,
                )
                along += maneuver.optDouble("length", 0.0) * 1_000.0
            }
        }

        return ComputedRoute(
            points = points,
            distanceMeters = summary.getDouble("length") * 1_000.0,
            durationSeconds = summary.getDouble("time"),
            steps = steps,
        )
    }

    private fun mapType(type: Int): ManeuverType = when (type) {
        4, 5, 6 -> ManeuverType.Arrive
        9, 23 -> ManeuverType.SlightRight
        10 -> ManeuverType.TurnRight
        11 -> ManeuverType.SharpRight
        12, 13 -> ManeuverType.UTurn
        14 -> ManeuverType.SharpLeft
        15 -> ManeuverType.TurnLeft
        16, 24 -> ManeuverType.SlightLeft
        18, 20 -> ManeuverType.ExitRight
        19, 21 -> ManeuverType.ExitLeft
        25 -> ManeuverType.Merge
        26 -> ManeuverType.Roundabout
        else -> ManeuverType.Continue
    }

    private fun decodePolyline6(encoded: String): List<GeoPoint> {
        val points = ArrayList<GeoPoint>()
        var index = 0
        var lat = 0
        var lon = 0
        while (index < encoded.length) {
            var shift = 0
            var result = 0
            var chunk: Int
            do {
                chunk = encoded[index++].code - 63
                result = result or ((chunk and 0x1f) shl shift)
                shift += 5
            } while (chunk >= 0x20)
            lat += if (result and 1 != 0) (result ushr 1).inv() else result ushr 1

            shift = 0
            result = 0
            do {
                chunk = encoded[index++].code - 63
                result = result or ((chunk and 0x1f) shl shift)
                shift += 5
            } while (chunk >= 0x20)
            lon += if (result and 1 != 0) (result ushr 1).inv() else result ushr 1

            points += GeoPoint(lat / 1_000_000.0, lon / 1_000_000.0)
        }
        return points
    }
}

/** Tries OSRM first, then Valhalla; the thrown message lists why each source failed. */
internal object RoutePlanner {
    fun route(origin: GeoPoint, destination: GeoPoint): ComputedRoute {
        return try {
            OsrmRouter.route(origin, destination)
        } catch (osrmFailure: Exception) {
            try {
                ValhallaRouter.route(origin, destination)
            } catch (valhallaFailure: Exception) {
                throw IOException("${osrmFailure.message} | valhalla: ${shortReason(valhallaFailure)}")
            }
        }
    }
}

/** [RoutingEngine] backed by OSRM, so any code that only needs a [RouteSummary] can use it. */
class OsrmRoutingEngine : RoutingEngine {
    override suspend fun calculateRoute(
        origin: LocationSample,
        destination: NavigationDestination,
    ): Result<RouteSummary> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val latitude = destination.latitude ?: throw IOException("Destination has no coordinates")
            val longitude = destination.longitude ?: throw IOException("Destination has no coordinates")
            val route = RoutePlanner.route(
                GeoPoint(origin.latitude, origin.longitude),
                GeoPoint(latitude, longitude),
            )
            RouteSummary(
                destination = destination,
                totalDistanceMeters = route.distanceMeters,
                estimatedDurationSeconds = route.durationSeconds.toLong(),
                geometry = route.points,
            )
        }
    }
}
