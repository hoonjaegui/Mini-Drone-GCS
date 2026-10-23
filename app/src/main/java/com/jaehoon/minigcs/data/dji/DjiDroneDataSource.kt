package com.jaehoon.minigcs.data.dji

import com.jaehoon.minigcs.data.DroneDataSource
import com.jaehoon.minigcs.data.TelemetryUpdate
import com.jaehoon.minigcs.data.toConnectionState
import com.jaehoon.minigcs.data.toFlightState
import com.jaehoon.minigcs.data.toGeoLocation
import com.jaehoon.minigcs.model.SdkStatus
import dji.common.flightcontroller.FlightMode
import dji.common.flightcontroller.LocationCoordinate3D
import dji.keysdk.BatteryKey
import dji.keysdk.DJIKey
import dji.keysdk.FlightControllerKey
import dji.keysdk.KeyManager
import dji.keysdk.RemoteControllerKey
import dji.keysdk.callback.KeyListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform

// 실제 DJI SDK 4.x → Flow<TelemetryUpdate>.
// FlightController/Battery 객체 대신 KeyManager를 쓴다: 기체가 아직 없어도 리스너를 걸 수 있고,
// 기체가 끊겼다 다시 붙어도 다시 등록할 필요가 없다. 키 하나 = TelemetryUpdate 하나라 Day 3 설계와도 맞다.
@OptIn(ExperimentalCoroutinesApi::class)
class DjiDroneDataSource(private val sdk: DjiSdk) : DroneDataSource {

    // KeyManager.getInstance()는 등록(registerApp) 성공 전엔 null이다. 등록될 때까지 기다린다.
    override val updates: Flow<TelemetryUpdate> = sdk.status
        .filter { it == SdkStatus.Registered }
        .take(1)
        .flatMapLatest { merge(connection(), location(), heading(), battery(), flightState()) }

    private fun connection(): Flow<TelemetryUpdate> = combine(
        keyValues<Boolean>(RemoteControllerKey.create(RemoteControllerKey.CONNECTION)),
        keyValues<Boolean>(FlightControllerKey.create(FlightControllerKey.CONNECTION)),
    ) { rc, aircraft -> TelemetryUpdate.Connection(toConnectionState(rc == true, aircraft == true)) }

    private fun location(): Flow<TelemetryUpdate> =
        keyValues<LocationCoordinate3D>(FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION))
            .conflate()
            .transform { location ->
                if (location == null) {
                    emit(TelemetryUpdate.Location(null))
                    return@transform
                }
                emit(TelemetryUpdate.Location(toGeoLocation(location.latitude, location.longitude)))
                emit(TelemetryUpdate.Altitude(location.altitude.toDouble()))
            }

    private fun heading(): Flow<TelemetryUpdate> =
        keyValues<Double>(FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW))
            .filterNotNull()
            .conflate()
            .map { TelemetryUpdate.Heading(it) }

    private fun battery(): Flow<TelemetryUpdate> =
        keyValues<Int>(BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT))
            .filterNotNull()
            .map { TelemetryUpdate.Battery(it) }

    private fun flightState(): Flow<TelemetryUpdate> = combine(
        keyValues<Boolean>(FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON)),
        keyValues<Boolean>(FlightControllerKey.create(FlightControllerKey.IS_FLYING)),
        keyValues<FlightMode>(FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE)),
    ) { motorsOn, isFlying, mode -> TelemetryUpdate.Flight(toFlightState(motorsOn, isFlying, mode?.toModeKind())) }

    // 키 하나를 Flow로. 리스너는 "바뀔 때만" 알려주므로 등록 직후 캐시된 현재 값(없으면 null)을 먼저 보낸다.
    private inline fun <reified T : Any> keyValues(key: DJIKey): Flow<T?> = rawKeyValues(key).map { it as? T }

    private fun rawKeyValues(key: DJIKey): Flow<Any?> = callbackFlow {
        val keyManager = KeyManager.getInstance()
        val listener = KeyListener { _, newValue -> trySend(newValue) }
        keyManager.addListener(key, listener)
        trySend(keyManager.getValue(key))
        awaitClose { keyManager.removeListener(listener) }
    }
}
