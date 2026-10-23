package com.jaehoon.minigcs.ui.telemetry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaehoon.minigcs.model.ConnectionState
import com.jaehoon.minigcs.model.DroneState
import com.jaehoon.minigcs.model.FlightState
import com.jaehoon.minigcs.model.GeoLocation
import com.jaehoon.minigcs.model.SdkStatus
import com.jaehoon.minigcs.ui.theme.MiniGCSTheme

// ViewModel과 연결하는 얇은 껍데기. 실제 그리기는 아래 stateless 버전이 한다.
@Composable
fun TelemetryRoute(
    modifier: Modifier = Modifier,
    viewModel: TelemetryViewModel = viewModel(factory = TelemetryViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sdkStatus by viewModel.sdkStatus.collectAsStateWithLifecycle()
    TelemetryScreen(state = state, sdkStatus = sdkStatus, modifier = modifier)
}

@Composable
fun TelemetryScreen(
    state: DroneState,
    sdkStatus: SdkStatus,
    modifier: Modifier = Modifier,
) {
    // 기체와 끊기면 마지막 값이 "현재 값"처럼 보이지 않도록 흐리게 표시한다(Day 3 퀴즈 4).
    val isLive = state.connection == ConnectionState.CONNECTED

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ConnectionBanner(state.connection)
        Column(modifier = Modifier.alpha(if (isLive) 1f else 0.4f)) {
            TelemetryRow("비행 상태", formatFlightState(state.flightState))
            TelemetryRow("위치", formatLocation(state.location))
            TelemetryRow("고도", formatAltitude(state.altitudeM))
            TelemetryRow("방향", formatHeading(state.headingDeg))
            TelemetryRow("배터리", formatBattery(state.batteryPercent))
        }
        Text(
            text = formatSdkStatus(sdkStatus),
            style = MaterialTheme.typography.bodySmall,
            color = if (sdkStatus is SdkStatus.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConnectionBanner(connection: ConnectionState) {
    val color = when (connection) {
        ConnectionState.DISCONNECTED -> Color(0xFFC62828)
        ConnectionState.RC_ONLY -> Color(0xFFEF6C00)
        ConnectionState.CONNECTED -> Color(0xFF2E7D32)
    }
    Text(
        text = formatConnection(connection),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(color, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
    HorizontalDivider()
}

@Preview(showBackground = true, name = "비행 중")
@Composable
private fun TelemetryScreenInAirPreview() {
    MiniGCSTheme {
        TelemetryScreen(
            DroneState(
                connection = ConnectionState.CONNECTED,
                location = GeoLocation(37.5665, 126.9780),
                altitudeM = 10.0,
                headingDeg = 137.0,
                batteryPercent = 82,
                flightState = FlightState.IN_AIR,
            ),
            sdkStatus = SdkStatus.Registered,
        )
    }
}

@Preview(showBackground = true, name = "연결 전")
@Composable
private fun TelemetryScreenDisconnectedPreview() {
    MiniGCSTheme {
        TelemetryScreen(DroneState(), sdkStatus = SdkStatus.Failed("Invalid App Key"))
    }
}
