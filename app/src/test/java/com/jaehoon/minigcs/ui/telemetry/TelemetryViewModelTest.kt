package com.jaehoon.minigcs.ui.telemetry

import com.jaehoon.minigcs.data.DroneDataSource
import com.jaehoon.minigcs.data.DroneRepository
import com.jaehoon.minigcs.data.TelemetryUpdate
import com.jaehoon.minigcs.model.ConnectionState
import com.jaehoon.minigcs.model.DroneState
import com.jaehoon.minigcs.model.SdkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryViewModelTest {

    // 테스트가 원하는 순간에 값을 밀어넣을 수 있는 DataSource
    private class ManualDataSource : DroneDataSource {
        override val updates = MutableSharedFlow<TelemetryUpdate>()
    }

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `구독 전에도 초기 상태를 가진다`() {
        val viewModel = TelemetryViewModel(DroneRepository(ManualDataSource()), MutableStateFlow(SdkStatus.NotUsed))

        assertEquals(DroneState(), viewModel.state.value)
    }

    @Test
    fun `DataSource 업데이트가 ViewModel 상태까지 전달된다`() = runTest(dispatcher) {
        val dataSource = ManualDataSource()
        val viewModel = TelemetryViewModel(DroneRepository(dataSource), MutableStateFlow(SdkStatus.NotUsed))
        val collector = backgroundScope.launch { viewModel.state.collect {} }

        dataSource.updates.emit(TelemetryUpdate.Connection(ConnectionState.CONNECTED))
        dataSource.updates.emit(TelemetryUpdate.Battery(55))

        assertEquals(ConnectionState.CONNECTED, viewModel.state.value.connection)
        assertEquals(55, viewModel.state.value.batteryPercent)
        collector.cancel()
    }
}
