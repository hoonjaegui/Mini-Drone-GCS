package com.jaehoon.minigcs.model

// 앱 내부에서 쓰는 Telemetry 표현. DJI SDK 타입은 이 파일에 절대 등장하지 않는다.
// 값을 아직 모르면 null — 0으로 채우면 "진짜 0"과 구분할 수 없다.
data class DroneState(
    val connection: ConnectionState = ConnectionState.DISCONNECTED,
    val location: GeoLocation? = null,
    val altitudeM: Double? = null,
    val headingDeg: Double? = null,
    val batteryPercent: Int? = null,
    val flightState: FlightState = FlightState.UNKNOWN,
)

// 폰 ↔ RC 연결과 RC ↔ 기체 연결은 별개다(Day 1).
enum class ConnectionState {
    DISCONNECTED,
    RC_ONLY,
    CONNECTED,
}

enum class FlightState {
    UNKNOWN,
    ON_GROUND,
    TAKING_OFF,
    IN_AIR,
    LANDING,
}

data class GeoLocation(val latitude: Double, val longitude: Double)
