package com.jaehoon.minigcs.data.sim

import com.jaehoon.minigcs.data.TelemetryUpdate
import com.jaehoon.minigcs.model.ConnectionState
import com.jaehoon.minigcs.model.FlightState
import com.jaehoon.minigcs.simsdk.Attitude
import com.jaehoon.minigcs.simsdk.BatteryState
import com.jaehoon.minigcs.simsdk.FlightControllerState
import com.jaehoon.minigcs.simsdk.FlightMode
import com.jaehoon.minigcs.simsdk.LocationCoordinate3D
import com.jaehoon.minigcs.simsdk.SimDroneSdk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimSdkDroneDataSourceTest {

    private val sdk = SimDroneSdk()
    private val dataSource = SimSdkDroneDataSource(sdk)

    @Test
    fun `구독하면 콜백이 등록되고 취소하면 해제된다`() = runTest(UnconfinedTestDispatcher()) {
        val job = launch { dataSource.updates.collect {} }
        runCurrent()

        assertTrue(sdk.flightController.hasStateCallback)
        assertTrue(sdk.battery.hasStateCallback)
        assertEquals(1, sdk.connectionManager.listenerCount)

        job.cancel()
        runCurrent()

        assertFalse(sdk.flightController.hasStateCallback)
        assertFalse(sdk.battery.hasStateCallback)
        assertEquals(0, sdk.connectionManager.listenerCount)
    }

    @Test
    fun `구독 전에 이미 연결돼 있으면 현재 연결 상태를 먼저 받는다`() = runTest(UnconfinedTestDispatcher()) {
        sdk.connectionManager.deliver(remoteControllerConnected = true, aircraftConnected = false)

        val received = mutableListOf<TelemetryUpdate>()
        backgroundScope.launch { dataSource.updates.collect { received += it } }
        runCurrent()

        assertEquals(listOf(TelemetryUpdate.Connection(ConnectionState.RC_ONLY)), received)
    }

    @Test
    fun `SDK 콜백 값이 TelemetryUpdate로 번역된다`() = runTest(UnconfinedTestDispatcher()) {
        val received = mutableListOf<TelemetryUpdate>()
        backgroundScope.launch { dataSource.updates.collect { received += it } }
        runCurrent()
        received.clear()

        sdk.flightController.deliver(
            FlightControllerState(
                aircraftLocation = LocationCoordinate3D(Double.NaN, Double.NaN, 3.5f),
                attitude = Attitude(0.0, 0.0, -45.0),
                areMotorsOn = true,
                isFlying = true,
                flightMode = FlightMode.AUTO_TAKEOFF,
            )
        )
        sdk.battery.deliver(BatteryState(chargeRemainingInPercent = 71))
        runCurrent()

        assertEquals(
            listOf(
                TelemetryUpdate.Location(null),
                TelemetryUpdate.Altitude(3.5),
                TelemetryUpdate.Heading(-45.0),
                TelemetryUpdate.Flight(FlightState.TAKING_OFF),
                TelemetryUpdate.Battery(71),
            ),
            received,
        )
    }
}
