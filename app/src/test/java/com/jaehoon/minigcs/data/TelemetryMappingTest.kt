package com.jaehoon.minigcs.data

import com.jaehoon.minigcs.model.FlightState
import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryMappingTest {

    @Test
    fun `모터 값이 아직 안 왔으면 모른다`() {
        assertEquals(FlightState.UNKNOWN, toFlightState(null, null, null))
    }

    @Test
    fun `모터가 꺼져 있으면 비행 모드와 상관없이 지상`() {
        assertEquals(FlightState.ON_GROUND, toFlightState(false, false, FlightModeKind.OTHER))
        assertEquals(FlightState.ON_GROUND, toFlightState(false, null, FlightModeKind.TAKEOFF))
    }

    @Test
    fun `모터만 켜지고 아직 안 뜨면 지상`() {
        assertEquals(FlightState.ON_GROUND, toFlightState(true, false, FlightModeKind.OTHER))
    }

    @Test
    fun `이륙과 착륙 모드`() {
        assertEquals(FlightState.TAKING_OFF, toFlightState(true, true, FlightModeKind.TAKEOFF))
        assertEquals(FlightState.LANDING, toFlightState(true, true, FlightModeKind.LANDING))
    }

    @Test
    fun `그 밖의 모드에서 떠 있으면 비행 중, 모드를 아직 모르면 isFlying만 본다`() {
        assertEquals(FlightState.IN_AIR, toFlightState(true, true, FlightModeKind.OTHER))
        assertEquals(FlightState.IN_AIR, toFlightState(true, true, null))
    }
}
