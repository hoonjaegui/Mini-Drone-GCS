package com.jaehoon.minigcs.data.sim

import com.jaehoon.minigcs.data.FlightModeKind
import com.jaehoon.minigcs.data.TelemetryUpdate
import com.jaehoon.minigcs.data.toFlightState
import com.jaehoon.minigcs.data.toGeoLocation
import com.jaehoon.minigcs.model.GeoLocation
import com.jaehoon.minigcs.simsdk.FlightControllerState
import com.jaehoon.minigcs.simsdk.FlightMode
import com.jaehoon.minigcs.simsdk.LocationCoordinate3D

// SDK 타입 → 앱 타입 번역. SDK 타입을 아는 코드는 data/sim 패키지 밖으로 나가지 않는다.

internal fun toGeoLocation(location: LocationCoordinate3D): GeoLocation? =
    toGeoLocation(location.latitude, location.longitude)

internal fun FlightMode.toModeKind(): FlightModeKind = when (this) {
    FlightMode.AUTO_TAKEOFF -> FlightModeKind.TAKEOFF
    FlightMode.AUTO_LANDING -> FlightModeKind.LANDING
    else -> FlightModeKind.OTHER
}

// SDK 상태 하나에 여러 항목이 들어 있으므로 TelemetryUpdate 여러 개로 쪼갠다.
internal fun FlightControllerState.toTelemetryUpdates(): List<TelemetryUpdate> = listOf(
    TelemetryUpdate.Location(toGeoLocation(aircraftLocation)),
    TelemetryUpdate.Altitude(aircraftLocation.altitude.toDouble()),
    TelemetryUpdate.Heading(attitude.yaw), // 0~360 정규화는 Repository가 한다
    TelemetryUpdate.Flight(toFlightState(areMotorsOn, isFlying, flightMode.toModeKind())),
)
