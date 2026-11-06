package org.wpfy.carlauncher.feature.settings

/** A source must be verified before any reading can appear as live vehicle data. */
internal enum class VehicleSourceKind { CANBUS, MCU, OBD_BLUETOOTH, OBD_USB, OEM_SERVICE, UNKNOWN }
internal enum class VehicleConnectionState { NOT_DETECTED, CANDIDATE, VERIFIED, DISCONNECTED }
internal enum class VehicleSignal { ENGINE_RPM, VEHICLE_SPEED, COOLANT_TEMPERATURE, BATTERY_VOLTAGE, DOOR_STATE, FUEL_LEVEL, DIAGNOSTIC_CODES }

internal data class VehicleSignalReading(
    val signal: VehicleSignal,
    val value: String,
    val unit: String,
    val source: VehicleSourceKind,
    val receivedAtMillis: Long,
)

internal data class VehicleConnectionSnapshot(
    val source: VehicleSourceKind,
    val state: VehicleConnectionState,
    val readings: List<VehicleSignalReading> = emptyList(),
)

/** Prevents fabricated, stale, or unverified values from being presented as current. */
internal fun VehicleConnectionSnapshot.visibleReadings(nowMillis: Long): List<VehicleSignalReading> =
    if (state != VehicleConnectionState.VERIFIED) emptyList()
    else readings.filter { it.source == source && it.receivedAtMillis <= nowMillis && nowMillis - it.receivedAtMillis <= 10_000L }

internal val vehicleSourceSlots = VehicleSourceKind.entries.map {
    VehicleConnectionSnapshot(source = it, state = VehicleConnectionState.NOT_DETECTED)
}
