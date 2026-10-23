package com.jaehoon.minigcs.simsdk

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// 가짜 SDK에 값을 밀어넣는 "가짜 기체". 실제 SDK처럼 메인이 아닌 별도 스레드에서 10Hz로 콜백을 부른다.
// 40초 시나리오(연결 → GPS → 이륙 → 선회 → 착륙)를 무한 반복한다.
class SimDroneSdkDriver(
    private val sdk: SimDroneSdk,
    private val tickMillis: Long = 100L,
) {
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "sim-sdk-callback").apply { isDaemon = true }
    }
    private var tick = 0
    private var lastConnection: Pair<Boolean, Boolean>? = null

    fun start() {
        executor.scheduleWithFixedDelay(::step, 0L, tickMillis, TimeUnit.MILLISECONDS)
    }

    private fun step() {
        val t = tick++ % LOOP_TICKS

        val connection = (t >= RC_ON_TICK) to (t >= AIRCRAFT_ON_TICK)
        if (connection != lastConnection) {
            lastConnection = connection
            sdk.connectionManager.deliver(connection.first, connection.second)
        }
        if (!connection.second) return

        sdk.flightController.deliver(flightControllerStateAt(t))
        if (t % 10 == 0) {
            sdk.battery.deliver(BatteryState(chargeRemainingInPercent = 90 - t / 30))
        }
    }

    private fun flightControllerStateAt(t: Int): FlightControllerState {
        val altitude = when {
            t < TAKEOFF_TICK -> 0f
            t < CRUISE_TICK -> (t - TAKEOFF_TICK) / 5f
            t < LANDING_TICK -> 10f
            t < MOTORS_OFF_TICK -> 10f - (t - LANDING_TICK) / 5f
            else -> 0f
        }
        val yaw = when {
            t < CRUISE_TICK -> 0.0
            t < LANDING_TICK -> -180.0 + (t - CRUISE_TICK) * 2.4
            else -> 180.0
        }
        val location = if (t < GPS_FIX_TICK) {
            LocationCoordinate3D(Double.NaN, Double.NaN, altitude)
        } else {
            val drift = (t.coerceIn(CRUISE_TICK, LANDING_TICK) - CRUISE_TICK) * 0.000005
            LocationCoordinate3D(HOME_LAT, HOME_LON + drift, altitude)
        }
        val mode = when {
            t in TAKEOFF_TICK until CRUISE_TICK -> FlightMode.AUTO_TAKEOFF
            t in LANDING_TICK until MOTORS_OFF_TICK -> FlightMode.AUTO_LANDING
            else -> FlightMode.GPS_ATTI
        }
        return FlightControllerState(
            aircraftLocation = location,
            attitude = Attitude(pitch = 0.0, roll = 0.0, yaw = yaw),
            areMotorsOn = t in MOTORS_ON_TICK until MOTORS_OFF_TICK,
            isFlying = t in TAKEOFF_TICK until MOTORS_OFF_TICK,
            flightMode = mode,
        )
    }

    private companion object {
        const val LOOP_TICKS = 400
        const val RC_ON_TICK = 20
        const val AIRCRAFT_ON_TICK = 40
        const val GPS_FIX_TICK = 60
        const val MOTORS_ON_TICK = 70
        const val TAKEOFF_TICK = 80
        const val CRUISE_TICK = 130
        const val LANDING_TICK = 280
        const val MOTORS_OFF_TICK = 330
        const val HOME_LAT = 37.5665
        const val HOME_LON = 126.9780
    }
}
