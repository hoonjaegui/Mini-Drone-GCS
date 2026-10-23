package com.jaehoon.minigcs

import android.app.Application
import com.jaehoon.minigcs.data.DroneDataSource
import com.jaehoon.minigcs.data.DroneRepository
import com.jaehoon.minigcs.data.FakeDroneDataSource

// 조립 지점. 앱 전체에서 어떤 DataSource를 쓸지는 여기 한 줄만 보고 결정한다.
// 주말엔 FakeDroneDataSource()를 DjiDroneDataSource(...)로 바꾼다.
class MiniGcsApplication : Application() {

    private val droneDataSource: DroneDataSource by lazy { FakeDroneDataSource() }

    val droneRepository: DroneRepository by lazy { DroneRepository(droneDataSource) }
}
