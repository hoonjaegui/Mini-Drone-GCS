package com.jaehoon.minigcs.data.dji

import com.jaehoon.minigcs.data.FlightModeKind
import dji.common.flightcontroller.FlightMode

// DJI 타입을 만지는 번역은 최대한 얇게 둔다. dji-sdk-provided는 컴파일 전용 스텁이라
// JVM 단위 테스트에서 클래스를 로드하면 VerifyError가 난다 → 판단 로직은 TelemetryMapping(앱 타입)에서 테스트한다.
internal fun FlightMode.toModeKind(): FlightModeKind = when (this) {
    FlightMode.AUTO_TAKEOFF,
    FlightMode.ASSISTED_TAKEOFF -> FlightModeKind.TAKEOFF

    FlightMode.AUTO_LANDING,
    FlightMode.ATTI_LANDING,
    FlightMode.CONFIRM_LANDING,
    FlightMode.MOTOR_STOP_LANDING -> FlightModeKind.LANDING

    else -> FlightModeKind.OTHER
}
