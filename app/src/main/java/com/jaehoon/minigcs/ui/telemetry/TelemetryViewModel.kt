package com.jaehoon.minigcs.ui.telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaehoon.minigcs.MiniGcsApplication
import com.jaehoon.minigcs.data.DroneRepository
import com.jaehoon.minigcs.model.DroneState
import com.jaehoon.minigcs.model.SdkStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TelemetryViewModel(
    repository: DroneRepository,
    // 이미 StateFlow라 stateIn이 필요 없다. 화면은 그대로 구독만 한다.
    val sdkStatus: StateFlow<SdkStatus>,
) : ViewModel() {

    // 화면 회전처럼 잠깐(5초 이내) 구독이 끊기는 경우엔 upstream을 유지한다.
    // 앱이 백그라운드로 오래 가면 수집을 멈춰서 SDK 콜백도 쉬게 한다.
    val state: StateFlow<DroneState> = repository.droneState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DroneState(),
    )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MiniGcsApplication
                TelemetryViewModel(app.droneRepository, app.sdkStatus)
            }
        }
    }
}
