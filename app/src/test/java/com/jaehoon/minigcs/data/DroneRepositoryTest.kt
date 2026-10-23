package com.jaehoon.minigcs.data

import com.jaehoon.minigcs.model.ConnectionState
import com.jaehoon.minigcs.model.DroneState
import com.jaehoon.minigcs.model.FlightState
import com.jaehoon.minigcs.model.GeoLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DroneRepositoryTest {

    private class ScriptedDataSource(vararg updates: TelemetryUpdate) : DroneDataSource {
        override val updates: Flow<TelemetryUpdate> = flowOf(*updates)
    }

    @Test
    fun `첫 값은 아무것도 모르는 초기 상태`() = runTest {
        val states = DroneRepository(ScriptedDataSource()).droneState.toList()

        assertEquals(listOf(DroneState()), states)
    }

    @Test
    fun `업데이트는 해당 항목만 바꾸고 나머지는 유지한다`() = runTest {
        val repo = DroneRepository(
            ScriptedDataSource(
                TelemetryUpdate.Connection(ConnectionState.CONNECTED),
                TelemetryUpdate.Battery(80),
                TelemetryUpdate.Altitude(12.5),
            )
        )

        val last = repo.droneState.last()

        assertEquals(ConnectionState.CONNECTED, last.connection)
        assertEquals(80, last.batteryPercent)
        assertEquals(12.5, last.altitudeM!!, 0.0)
        assertNull(last.location)
    }

    @Test
    fun `heading은 0 이상 360 미만으로 정규화된다`() {
        assertEquals(0.0, normalizeHeading(0.0), 0.0)
        assertEquals(180.0, normalizeHeading(-180.0), 0.0)
        assertEquals(270.0, normalizeHeading(-90.0), 0.0)
        assertEquals(0.0, normalizeHeading(360.0), 0.0)
        assertEquals(10.0, normalizeHeading(370.0), 0.0)
    }

    @Test
    fun `배터리는 0~100 범위로 자른다`() {
        assertEquals(100, DroneState().reduce(TelemetryUpdate.Battery(120)).batteryPercent)
        assertEquals(0, DroneState().reduce(TelemetryUpdate.Battery(-5)).batteryPercent)
    }

    @Test
    fun `GPS를 잃으면 위치는 다시 null이 된다`() = runTest {
        val repo = DroneRepository(
            ScriptedDataSource(
                TelemetryUpdate.Location(GeoLocation(37.0, 127.0)),
                TelemetryUpdate.Location(null),
            )
        )

        assertNull(repo.droneState.last().location)
    }

    @Test
    fun `Fake 시나리오는 연결 후 이륙하고 착륙해서 끝난다`() = runTest {
        val states = DroneRepository(FakeDroneDataSource()).droneState.toList()

        // RC_ONLY를 거쳐서 CONNECTED가 된다
        val connections = states.map { it.connection }.distinct()
        assertEquals(
            listOf(ConnectionState.DISCONNECTED, ConnectionState.RC_ONLY, ConnectionState.CONNECTED),
            connections,
        )

        val flightStates = states.map { it.flightState }.distinct()
        assertEquals(
            listOf(
                FlightState.UNKNOWN,
                FlightState.ON_GROUND,
                FlightState.TAKING_OFF,
                FlightState.IN_AIR,
                FlightState.LANDING,
            ),
            flightStates,
        )

        val last = states.last()
        assertEquals(FlightState.ON_GROUND, last.flightState)
        assertEquals(0.0, last.altitudeM!!, 0.0)
        val maxAltitude = states.mapNotNull { it.altitudeM }.max()
        assertEquals(FakeDroneDataSource.TARGET_ALTITUDE_M.toDouble(), maxAltitude, 0.0)
        // 시나리오 중 어느 순간에도 heading이 범위를 벗어나지 않는다
        states.mapNotNull { it.headingDeg }.forEach { assert(it >= 0.0 && it < 360.0) }
    }
}
