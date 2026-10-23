package com.jaehoon.minigcs.simsdk

import java.util.concurrent.CopyOnWriteArraySet

// DJI Mobile SDK 4.x의 "모양"만 흉내 낸 가짜 SDK. 앱 입장에서는 고칠 수 없는 외부 라이브러리 역할이다.
// 실제 SDK 대응(주말에 이름 대조):
//   FlightController.setStateCallback / FlightControllerState  ↔ dji.sdk.flightcontroller
//   Battery.setStateCallback / BatteryState                     ↔ dji.sdk.battery
//   ConnectionManager                                           ↔ KeyManager + 연결 Key 리스너
class SimDroneSdk {
    val flightController = FlightController()
    val battery = Battery()
    val connectionManager = ConnectionManager()
}

// 위도/경도는 GPS가 없으면 NaN. 고도는 이륙 지점 기준 상대 고도(m).
class LocationCoordinate3D(val latitude: Double, val longitude: Double, val altitude: Float)

// yaw는 -180~180, 북쪽이 0.
class Attitude(val pitch: Double, val roll: Double, val yaw: Double)

enum class FlightMode { MANUAL, GPS_ATTI, AUTO_TAKEOFF, AUTO_LANDING, GO_HOME, UNKNOWN }

class FlightControllerState(
    val aircraftLocation: LocationCoordinate3D,
    val attitude: Attitude,
    val areMotorsOn: Boolean,
    val isFlying: Boolean,
    val flightMode: FlightMode,
) {
    fun interface Callback {
        fun onUpdate(state: FlightControllerState)
    }
}

class BatteryState(val chargeRemainingInPercent: Int) {
    fun interface Callback {
        fun onUpdate(state: BatteryState)
    }
}

// 콜백을 "하나만" 받는다. 두 번째로 등록하면 첫 번째가 덮어써진다.
class FlightController {
    @Volatile private var callback: FlightControllerState.Callback? = null

    fun setStateCallback(callback: FlightControllerState.Callback?) {
        this.callback = callback
    }

    internal val hasStateCallback: Boolean get() = callback != null

    internal fun deliver(state: FlightControllerState) {
        callback?.onUpdate(state)
    }
}

class Battery {
    @Volatile private var callback: BatteryState.Callback? = null

    fun setStateCallback(callback: BatteryState.Callback?) {
        this.callback = callback
    }

    internal val hasStateCallback: Boolean get() = callback != null

    internal fun deliver(state: BatteryState) {
        callback?.onUpdate(state)
    }
}

// 리스너는 여러 개 등록할 수 있고, 값이 "바뀔 때만" 알려준다. 현재 값은 getter로 읽는다.
class ConnectionManager {
    fun interface Listener {
        fun onConnectionChanged(remoteControllerConnected: Boolean, aircraftConnected: Boolean)
    }

    private val listeners = CopyOnWriteArraySet<Listener>()

    @Volatile var isRemoteControllerConnected: Boolean = false
        private set

    @Volatile var isAircraftConnected: Boolean = false
        private set

    fun addListener(listener: Listener) {
        listeners += listener
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    internal val listenerCount: Int get() = listeners.size

    internal fun deliver(remoteControllerConnected: Boolean, aircraftConnected: Boolean) {
        isRemoteControllerConnected = remoteControllerConnected
        isAircraftConnected = aircraftConnected
        listeners.forEach { it.onConnectionChanged(remoteControllerConnected, aircraftConnected) }
    }
}
