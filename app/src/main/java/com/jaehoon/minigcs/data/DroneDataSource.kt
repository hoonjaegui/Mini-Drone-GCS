package com.jaehoon.minigcs.data

import kotlinx.coroutines.flow.Flow

// SDK 경계. 평일엔 FakeDroneDataSource, 주말엔 DjiDroneDataSource가 이 자리에 들어온다.
// 구현체는 SDK 값을 TelemetryUpdate(앱 타입)로 바꿔서 내보내는 것까지만 책임진다.
interface DroneDataSource {
    val updates: Flow<TelemetryUpdate>
}
