package com.jaehoon.minigcs.data.dji

import android.content.Context
import android.util.Log
import com.jaehoon.minigcs.model.SdkStatus
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// DJISDKManager 등록 절차를 감싼다. 등록 결과를 StateFlow로 내보낸다.
// 등록 성공 → startConnectionToProduct() → 이후 연결/Telemetry는 KeyManager로 받는다(DjiDroneDataSource).
class DjiSdk(private val context: Context) {

    private val _status = MutableStateFlow<SdkStatus>(SdkStatus.NotUsed)
    val status: StateFlow<SdkStatus> = _status.asStateFlow()

    fun register() {
        if (_status.value == SdkStatus.Registering || _status.value == SdkStatus.Registered) return
        _status.value = SdkStatus.Registering
        DJISDKManager.getInstance().registerApp(context.applicationContext, callback)
    }

    private val callback = object : DJISDKManager.SDKManagerCallback {
        override fun onRegister(error: DJIError?) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                Log.i(TAG, "registerApp 성공")
                _status.value = SdkStatus.Registered
                DJISDKManager.getInstance().startConnectionToProduct()
            } else {
                val message = error?.description ?: "알 수 없는 오류"
                Log.w(TAG, "registerApp 실패: $message (code=${error?.errorCode})")
                _status.value = SdkStatus.Failed(message)
            }
        }

        // 연결 상태는 KeyManager의 CONNECTION 키로 받으므로 여기서는 로그만 남긴다.
        override fun onProductDisconnect() {
            Log.i(TAG, "onProductDisconnect")
        }

        override fun onProductConnect(product: BaseProduct?) {
            Log.i(TAG, "onProductConnect: $product")
        }

        override fun onProductChanged(product: BaseProduct?) {
            Log.i(TAG, "onProductChanged: $product")
        }

        override fun onComponentChange(
            key: BaseProduct.ComponentKey?,
            oldComponent: BaseComponent?,
            newComponent: BaseComponent?,
        ) {
            Log.d(TAG, "onComponentChange: $key")
        }

        override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) = Unit
        override fun onDatabaseDownloadProgress(current: Long, total: Long) = Unit
    }

    private companion object {
        const val TAG = "DjiSdk"
    }
}
