import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// App Key는 저장소에 올리지 않는다. local.properties(.gitignore 대상)에 `dji.apiKey=...`로 넣는다.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.jaehoon.minigcs"
    compileSdk = 36
    // DJI SDK 내부가 Android 9에서 빠진 Apache HTTP를 쓴다. 없으면 registerApp 중 NoClassDefFoundError.
    useLibrary("org.apache.http.legacy")

    defaultConfig {
        // DJI 개발자 사이트에서 App Key를 발급받을 때 이 값으로 등록한다(Weekend 1).
        applicationId = "com.jaehoon.minigcs"
        minSdk = 26
        // DJI SDK 4.18 공식 샘플이 34까지 검증됐다. 35 이상은 확인 전까지 올리지 않는다.
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        manifestPlaceholders["djiApiKey"] = localProperties.getProperty("dji.apiKey", "")
        // sim: 가짜 SDK(평일) / dji: 실제 DJI SDK(실기체). `./gradlew installDebug -Pminigcs.dataSource=dji`
        val dataSource = (project.findProperty("minigcs.dataSource") as String?) ?: "sim"
        buildConfigField("String", "DRONE_DATA_SOURCE", "\"$dataSource\"")

        ndk {
            // DJI 네이티브 라이브러리 중 실기기용 ABI만 넣는다.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            // DJI SDK는 .so를 압축 해제된 상태 + strip하지 않은 상태로 요구한다.
            useLegacyPackaging = true
            keepDebugSymbols += "**/*.so"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.dji.sdk) {
        // Mavic 2 전용 왜곡 보정 라이브러리. Mini 2에는 필요 없고 APK만 커진다.
        exclude(module = "library-anti-distortion")
    }
    compileOnly(libs.dji.sdk.provided)
    // DJI SDK 내부 레이아웃이 참조한다. 우리 코드에서는 쓰지 않는다.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
