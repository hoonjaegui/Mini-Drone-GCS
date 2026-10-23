package com.jaehoon.minigcs

import android.app.Application
import android.content.Context
import com.cySdkyc.clx.Helper
import com.jaehoon.minigcs.data.DroneDataSource
import com.jaehoon.minigcs.data.DroneRepository
import com.jaehoon.minigcs.data.dji.DjiDroneDataSource
import com.jaehoon.minigcs.data.dji.DjiSdk
import com.jaehoon.minigcs.data.sim.SimSdkDroneDataSource
import com.jaehoon.minigcs.model.SdkStatus
import com.jaehoon.minigcs.simsdk.SimDroneSdk
import com.jaehoon.minigcs.simsdk.SimDroneSdkDriver
import kotlinx.coroutines.flow.StateFlow

// 조립 지점. 앱 전체에서 어떤 DataSource를 쓸지는 여기만 보고 결정한다.
// 빌드 옵션 -Pminigcs.dataSource=dji 로 실제 DJI SDK, 기본값(sim)은 가짜 SDK.
class MiniGcsApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // DJI SDK 4.x 필수: SDK의 다른 어떤 코드보다 먼저 불려야 한다. 빠지면 실행 중 크래시.
        Helper.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        // DataSource와 상관없이 등록은 항상 시도한다. 기체 없이도 App Key/네트워크/SDK 로딩을 확인할 수 있다.
        djiSdk.register()
    }

    private val djiSdk: DjiSdk by lazy { DjiSdk(this) }

    // 실제 SDK처럼 앱 구독과 상관없이 "기체"는 계속 돌아간다.
    private val simSdk: SimDroneSdk by lazy { SimDroneSdk().also { SimDroneSdkDriver(it).start() } }

    private val droneDataSource: DroneDataSource by lazy {
        when (BuildConfig.DRONE_DATA_SOURCE) {
            "dji" -> DjiDroneDataSource(djiSdk)
            else -> SimSdkDroneDataSource(simSdk)
        }
    }

    val droneRepository: DroneRepository by lazy { DroneRepository(droneDataSource) }

    val sdkStatus: StateFlow<SdkStatus> get() = djiSdk.status
}
