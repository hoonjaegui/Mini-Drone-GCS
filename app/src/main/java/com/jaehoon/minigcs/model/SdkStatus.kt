package com.jaehoon.minigcs.model

// 드론 SDK 자체의 준비 상태. 기체 연결(ConnectionState)과는 별개다.
// DJI SDK는 App Key 등록이 끝나야 KeyManager를 쓸 수 있다.
sealed interface SdkStatus {
    data object NotUsed : SdkStatus
    data object Registering : SdkStatus
    data object Registered : SdkStatus
    data class Failed(val message: String) : SdkStatus
}
