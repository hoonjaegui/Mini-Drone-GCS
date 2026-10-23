package com.jaehoon.minigcs.data.sim

import com.jaehoon.minigcs.data.toConnectionState
import com.jaehoon.minigcs.data.toFlightState
import com.jaehoon.minigcs.model.ConnectionState
import com.jaehoon.minigcs.model.FlightState
import com.jaehoon.minigcs.model.GeoLocation
import com.jaehoon.minigcs.simsdk.Attitude
import com.jaehoon.minigcs.simsdk.FlightControllerState
import com.jaehoon.minigcs.simsdk.FlightMode
import com.jaehoon.minigcs.simsdk.LocationCoordinate3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SimSdkMappingTest {

    private fun fcState(
        motorsOn: Boolean = true,
        flying: Boolean = true,
        mode: FlightMode = FlightMode.GPS_ATTI,
    ) = FlightControllerState(
        aircraftLocation = LocationCoordinate3D(37.0, 127.0, 5f),
        attitude = Attitude(0.0, 0.0, -90.0),
        areMotorsOn = motorsOn,
        isFlying = flying,
        flightMode = mode,
    )

    private fun toSimFlightState(state: FlightControllerState) =
        toFlightState(state.areMotorsOn, state.isFlying, state.flightMode.toModeKind())

    @Test
    fun `연결 상태는 RC가 먼저, 기체가 다음`() {
        assertEquals(ConnectionState.DISCONNECTED, toConnectionState(false, false))
        assertEquals(ConnectionState.DISCONNECTED, toConnectionState(false, true))
        assertEquals(ConnectionState.RC_ONLY, toConnectionState(true, false))
        assertEquals(ConnectionState.CONNECTED, toConnectionState(true, true))
    }

    @Test
    fun `GPS가 NaN이거나 0,0이면 위치를 모른다`() {
        assertNull(toGeoLocation(LocationCoordinate3D(Double.NaN, Double.NaN, 0f)))
        assertNull(toGeoLocation(LocationCoordinate3D(37.0, Double.NaN, 0f)))
        assertNull(toGeoLocation(LocationCoordinate3D(0.0, 0.0, 0f)))
        assertEquals(GeoLocation(37.0, 127.0), toGeoLocation(LocationCoordinate3D(37.0, 127.0, 0f)))
    }

    @Test
    fun `비행 상태 매핑`() {
        assertEquals(FlightState.ON_GROUND, toSimFlightState(fcState(motorsOn = false, flying = false)))
        assertEquals(FlightState.ON_GROUND, toSimFlightState(fcState(motorsOn = true, flying = false)))
        assertEquals(FlightState.TAKING_OFF, toSimFlightState(fcState(mode = FlightMode.AUTO_TAKEOFF)))
        assertEquals(FlightState.LANDING, toSimFlightState(fcState(mode = FlightMode.AUTO_LANDING)))
        assertEquals(FlightState.IN_AIR, toSimFlightState(fcState()))
        assertEquals(FlightState.IN_AIR, toSimFlightState(fcState(mode = FlightMode.GO_HOME)))
    }
}
