package com.jaehoon.minigcs.data

import com.jaehoon.minigcs.model.DroneState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.scan

// DataSource가 흘려보내는 조각난 업데이트를 하나의 DroneState로 누적한다.
// 앱의 나머지 부분은 이 클래스의 droneState만 본다.
class DroneRepository(dataSource: DroneDataSource) {

    val droneState: Flow<DroneState> = dataSource.updates
        .scan(DroneState()) { state, update -> state.reduce(update) }
}

// 순수 함수라 코루틴 없이 바로 테스트할 수 있다.
internal fun DroneState.reduce(update: TelemetryUpdate): DroneState = when (update) {
    is TelemetryUpdate.Connection -> copy(connection = update.state)
    is TelemetryUpdate.Location -> copy(location = update.location)
    is TelemetryUpdate.Altitude -> copy(altitudeM = update.meters)
    is TelemetryUpdate.Heading -> copy(headingDeg = normalizeHeading(update.degrees))
    is TelemetryUpdate.Battery -> copy(batteryPercent = update.percent.coerceIn(0, 100))
    is TelemetryUpdate.Flight -> copy(flightState = update.state)
}

// 앱 안에서는 항상 0 이상 360 미만으로 다룬다. SDK 실제 범위는 주말에 확인한다.
internal fun normalizeHeading(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0
