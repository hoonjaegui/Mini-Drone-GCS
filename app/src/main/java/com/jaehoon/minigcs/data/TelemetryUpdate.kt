package com.jaehoon.minigcs.data

import com.jaehoon.minigcs.model.ConnectionState
import com.jaehoon.minigcs.model.FlightState
import com.jaehoon.minigcs.model.GeoLocation

// SDK 콜백 하나 = TelemetryUpdate 하나. SDK는 항목별로 따로따로 값을 알려주기 때문에
// DataSource도 "바뀐 항목 하나"만 내보내고, 합치는 일은 Repository가 한다.
sealed interface TelemetryUpdate {
    data class Connection(val state: ConnectionState) : TelemetryUpdate
    data class Location(val location: GeoLocation?) : TelemetryUpdate
    data class Altitude(val meters: Double) : TelemetryUpdate
    data class Heading(val degrees: Double) : TelemetryUpdate
    data class Battery(val percent: Int) : TelemetryUpdate
    data class Flight(val state: FlightState) : TelemetryUpdate
}
