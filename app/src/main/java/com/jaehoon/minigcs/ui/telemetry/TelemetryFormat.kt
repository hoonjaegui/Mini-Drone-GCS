package com.jaehoon.minigcs.ui.telemetry

import com.jaehoon.minigcs.model.ConnectionState
import com.jaehoon.minigcs.model.FlightState
import com.jaehoon.minigcs.model.GeoLocation
import com.jaehoon.minigcs.model.SdkStatus
import java.util.Locale

// DroneState 값 → 화면 문자열. null("모름")은 전부 여기서 "—"로 바뀐다.
internal const val UNKNOWN_TEXT = "—"

internal fun formatConnection(state: ConnectionState): String = when (state) {
    ConnectionState.DISCONNECTED -> "연결 끊김"
    ConnectionState.RC_ONLY -> "조종기만 연결됨"
    ConnectionState.CONNECTED -> "기체 연결됨"
}

internal fun formatSdkStatus(status: SdkStatus): String = when (status) {
    SdkStatus.NotUsed -> "DJI SDK: 사용 안 함"
    SdkStatus.Registering -> "DJI SDK: 등록 중…"
    SdkStatus.Registered -> "DJI SDK: 등록 완료"
    is SdkStatus.Failed -> "DJI SDK: 등록 실패 — ${status.message}"
}

internal fun formatFlightState(state: FlightState): String = when (state) {
    FlightState.UNKNOWN -> UNKNOWN_TEXT
    FlightState.ON_GROUND -> "지상"
    FlightState.TAKING_OFF -> "이륙 중"
    FlightState.IN_AIR -> "비행 중"
    FlightState.LANDING -> "착륙 중"
}

internal fun formatLocation(location: GeoLocation?): String =
    location?.let { String.format(Locale.US, "%.6f, %.6f", it.latitude, it.longitude) } ?: "GPS 수신 대기"

internal fun formatAltitude(meters: Double?): String =
    meters?.let { String.format(Locale.US, "%.1f m", it) } ?: UNKNOWN_TEXT

internal fun formatHeading(degrees: Double?): String =
    degrees?.let { String.format(Locale.US, "%.0f° %s", it, compassPoint(it)) } ?: UNKNOWN_TEXT

internal fun formatBattery(percent: Int?): String = percent?.let { "$it %" } ?: UNKNOWN_TEXT

// 0~360 heading을 8방위로. 경계(22.5° 단위)는 가까운 쪽으로 반올림.
internal fun compassPoint(degrees: Double): String {
    val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = Math.floorMod(Math.round(degrees / 45.0).toInt(), points.size)
    return points[index]
}
