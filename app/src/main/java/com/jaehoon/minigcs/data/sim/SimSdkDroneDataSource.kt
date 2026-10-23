package com.jaehoon.minigcs.data.sim

import com.jaehoon.minigcs.data.DroneDataSource
import com.jaehoon.minigcs.data.TelemetryUpdate
import com.jaehoon.minigcs.data.toConnectionState
import com.jaehoon.minigcs.simsdk.BatteryState
import com.jaehoon.minigcs.simsdk.ConnectionManager
import com.jaehoon.minigcs.simsdk.FlightControllerState
import com.jaehoon.minigcs.simsdk.SimDroneSdk
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.transform

// SDK 콜백 → Flow<TelemetryUpdate>. 기체 없이 평일에 화면을 돌려보는 용도.
// 콜백 등록은 collect가 시작될 때, 해제는 collect가 취소될 때(awaitClose) 일어난다.
class SimSdkDroneDataSource(private val sdk: SimDroneSdk) : DroneDataSource {

    override val updates: Flow<TelemetryUpdate> = merge(
        connectionUpdates(),
        flightControllerUpdates(),
        batteryUpdates(),
    )

    private fun connectionUpdates(): Flow<TelemetryUpdate> = callbackFlow {
        val manager = sdk.connectionManager
        val listener = ConnectionManager.Listener { rc, aircraft -> trySend(toConnectionState(rc, aircraft)) }
        // 리스너는 "바뀔 때만" 알려준다. 등록부터 하고 현재 값을 읽어야 그 사이의 변화를 놓치지 않는다.
        manager.addListener(listener)
        trySend(toConnectionState(manager.isRemoteControllerConnected, manager.isAircraftConnected))
        awaitClose { manager.removeListener(listener) }
    }.map { TelemetryUpdate.Connection(it) }

    private fun flightControllerUpdates(): Flow<TelemetryUpdate> = callbackFlow {
        val flightController = sdk.flightController
        flightController.setStateCallback(FlightControllerState.Callback { trySend(it) })
        awaitClose { flightController.setStateCallback(null) }
    }
        .conflate() // 10Hz로 쏟아지는데 처리가 밀리면 지난 상태는 버리고 최신 상태만 쓴다
        .transform { state -> state.toTelemetryUpdates().forEach { emit(it) } }

    private fun batteryUpdates(): Flow<TelemetryUpdate> = callbackFlow {
        val battery = sdk.battery
        battery.setStateCallback(BatteryState.Callback { trySend(it) })
        awaitClose { battery.setStateCallback(null) }
    }.map { TelemetryUpdate.Battery(it.chargeRemainingInPercent) }
}
