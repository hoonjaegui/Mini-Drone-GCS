package com.jaehoon.minigcs.ui.telemetry

import com.jaehoon.minigcs.model.FlightState
import com.jaehoon.minigcs.model.GeoLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryFormatTest {

    @Test
    fun `모르는 값은 대시로 표시한다`() {
        assertEquals(UNKNOWN_TEXT, formatAltitude(null))
        assertEquals(UNKNOWN_TEXT, formatHeading(null))
        assertEquals(UNKNOWN_TEXT, formatBattery(null))
        assertEquals(UNKNOWN_TEXT, formatFlightState(FlightState.UNKNOWN))
        assertEquals("GPS 수신 대기", formatLocation(null))
    }

    @Test
    fun `값은 단위와 함께 표시한다`() {
        assertEquals("12.5 m", formatAltitude(12.5))
        assertEquals("0.0 m", formatAltitude(0.0))
        assertEquals("63 %", formatBattery(63))
        assertEquals("37.566500, 126.978000", formatLocation(GeoLocation(37.5665, 126.978)))
    }

    @Test
    fun `heading은 8방위와 함께 표시한다`() {
        assertEquals("0° N", formatHeading(0.0))
        assertEquals("137° SE", formatHeading(137.0))
        assertEquals("270° W", formatHeading(270.0))
        assertEquals("350° N", formatHeading(350.0))
    }
}
