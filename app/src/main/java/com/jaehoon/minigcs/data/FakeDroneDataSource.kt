package com.jaehoon.minigcs.data

import com.jaehoon.minigcs.model.ConnectionState
import com.jaehoon.minigcs.model.FlightState
import com.jaehoon.minigcs.model.GeoLocation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// 실기체 없이 UI/구조를 만들기 위한 가짜 비행 시나리오.
// 연결 → GPS 수신 → 이륙 → 선회 → 착륙을 한 번 재생한다.
class FakeDroneDataSource(
    private val tickMillis: Long = 500L,
) : DroneDataSource {

    override val updates: Flow<TelemetryUpdate> = flow {
        emit(TelemetryUpdate.Connection(ConnectionState.DISCONNECTED))
        delay(tickMillis)

        // RC만 먼저 붙고, 기체는 나중에 켜지는 상황
        emit(TelemetryUpdate.Connection(ConnectionState.RC_ONLY))
        delay(tickMillis)

        emit(TelemetryUpdate.Connection(ConnectionState.CONNECTED))
        emit(TelemetryUpdate.Flight(FlightState.ON_GROUND))
        emit(TelemetryUpdate.Battery(BATTERY_START))
        emit(TelemetryUpdate.Altitude(0.0))
        emit(TelemetryUpdate.Heading(0.0))
        emit(TelemetryUpdate.Location(null)) // 아직 GPS 미수신
        delay(tickMillis)

        emit(TelemetryUpdate.Location(HOME))
        delay(tickMillis)

        emit(TelemetryUpdate.Flight(FlightState.TAKING_OFF))
        for (alt in 1..TARGET_ALTITUDE_M) {
            emit(TelemetryUpdate.Altitude(alt.toDouble()))
            delay(tickMillis)
        }

        emit(TelemetryUpdate.Flight(FlightState.IN_AIR))
        // SDK가 -180~180 범위로 줄 수도 있다는 가정을 일부러 섞는다.
        var battery = BATTERY_START
        for (step in 0..12) {
            val heading = -180.0 + step * 30.0
            emit(TelemetryUpdate.Heading(heading))
            emit(TelemetryUpdate.Location(HOME.copy(longitude = HOME.longitude + step * 0.00005)))
            battery -= 1
            emit(TelemetryUpdate.Battery(battery))
            delay(tickMillis)
        }

        emit(TelemetryUpdate.Flight(FlightState.LANDING))
        for (alt in TARGET_ALTITUDE_M - 1 downTo 0) {
            emit(TelemetryUpdate.Altitude(alt.toDouble()))
            delay(tickMillis)
        }
        emit(TelemetryUpdate.Flight(FlightState.ON_GROUND))
    }

    companion object {
        val HOME = GeoLocation(latitude = 37.5665, longitude = 126.9780)
        const val BATTERY_START = 87
        const val TARGET_ALTITUDE_M = 10
    }
}
