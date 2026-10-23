package com.jaehoon.minigcs.data

import com.jaehoon.minigcs.model.ConnectionState
import com.jaehoon.minigcs.model.FlightState
import com.jaehoon.minigcs.model.GeoLocation

// SDK 종류와 상관없는 번역 규칙. Sim/DJI DataSource가 같이 쓴다.

internal fun toConnectionState(remoteControllerConnected: Boolean, aircraftConnected: Boolean): ConnectionState =
    when {
        !remoteControllerConnected -> ConnectionState.DISCONNECTED
        !aircraftConnected -> ConnectionState.RC_ONLY
        else -> ConnectionState.CONNECTED
    }

// GPS 미수신 시 SDK는 NaN을 준다. 0,0도 실제 위치일 리 없으니 같이 걸러낸다.
internal fun toGeoLocation(latitude: Double, longitude: Double): GeoLocation? {
    if (latitude.isNaN() || longitude.isNaN()) return null
    if (latitude == 0.0 && longitude == 0.0) return null
    return GeoLocation(latitude = latitude, longitude = longitude)
}

// SDK마다 비행 모드 종류가 다르다(DJI는 40개 이상). 비행 상태 판단에 필요한 것만 남긴 분류.
internal enum class FlightModeKind { TAKEOFF, LANDING, OTHER }

// 값이 하나라도 아직 안 왔으면(null) 모른다고 한다.
internal fun toFlightState(motorsOn: Boolean?, isFlying: Boolean?, modeKind: FlightModeKind?): FlightState = when {
    motorsOn == null -> FlightState.UNKNOWN
    !motorsOn -> FlightState.ON_GROUND
    modeKind == FlightModeKind.TAKEOFF -> FlightState.TAKING_OFF
    modeKind == FlightModeKind.LANDING -> FlightState.LANDING
    isFlying == true -> FlightState.IN_AIR
    else -> FlightState.ON_GROUND // 모터만 돌고 아직 뜨지 않음
}
