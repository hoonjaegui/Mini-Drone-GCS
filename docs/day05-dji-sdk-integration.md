# Day 5 — DJI SDK 4.18 연동 (기체 없이 할 수 있는 데까지)

> 환경: 회사(평일) · 실기체 없음 · SDK 빌드/등록/DataSource 코드까지. 실제 값 수신은 주말.

## 1. 오늘 목표

- 실제 DJI Mobile SDK 4.18을 프로젝트에 넣고 빌드, 실행이 통과한다.
- App Key 없이도 `registerApp`이 크래시 없이 "키 없음" 에러를 돌려주는 것까지 확인한다.
- `DjiDroneDataSource`를 실제 DJI 타입으로 작성하고 컴파일한다.
- SDK 콜백을 Flow로 바꾸는 패턴(`callbackFlow` / `awaitClose`)을 가짜 SDK로 테스트한다.

## 2. 필요한 개념

### 기체 없이 어디까지 되나

| 작업 | 기체 없이 | 오늘 결과 |
| --- | --- | --- |
| SDK 의존성 추가, 빌드 | ✅ | 성공 (APK 약 10MB) |
| 앱 실행 시 SDK 로딩 (`Helper.install`, 네이티브 라이브러리) | ✅ | 성공 |
| `registerApp` 호출 | ✅ 인터넷 필요 | "The app key was not provided." (키 발급 전이라 정상) |
| `DjiDroneDataSource` 컴파일 | ✅ | 성공 |
| 연결 콜백, Telemetry 수신 | ❌ RC와 기체 필요 | 주말 |

### 오늘 실제로 부딪힌 문제 3개

기체 앞에서 처음 만났으면 비행 시간을 다 썼을 문제들이다.

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| 빌드: `attribute layout_constraintTop_toTopOf not found` | SDK AAR 안의 레이아웃이 ConstraintLayout과 AppCompat 속성을 쓴다 | `constraintlayout`, `appcompat` 의존성 추가 (우리 코드에서는 안 씀) |
| 실행 직후 크래시: `NoClassDefFoundError: org.apache.http.params.BasicHttpParams` | SDK가 Android 9에서 빠진 Apache HTTP를 쓴다 | `useLibrary("org.apache.http.legacy")` + Manifest `<uses-library>` |
| 단위 테스트: `VerifyError: Constructor must call super()` | `dji-sdk-provided`는 **컴파일 전용 스텁**이다. JVM에서 클래스를 로드하면 깨진다 | DJI 타입을 만지는 코드는 얇게 두고, 판단 로직은 앱 타입 함수(`TelemetryMapping.kt`)에서 테스트 |

### 그 밖의 SDK 요구사항 (공식 샘플 기준)

| 설정 | 이유 |
| --- | --- |
| `Helper.install(this)`를 `attachBaseContext`에서 호출 | SDK의 다른 어떤 코드보다 먼저 불려야 한다. 빠지면 크래시 |
| `targetSdk = 34` (36에서 내림) | DJI 4.18 공식 샘플이 34까지 검증됐다. 4.17은 34에서 `registerApp` 문제가 보고됐다 |
| `useLegacyPackaging = true`, `keepDebugSymbols` | SDK가 `.so`를 압축 해제되고 strip되지 않은 상태로 요구한다 |
| `abiFilters` arm64-v8a, armeabi-v7a | 실기기용만 넣는다 |
| `INTERNET` 권한 | 첫 등록 때 App Key를 서버에서 검증한다 |
| `USB_ACCESSORY_ATTACHED` intent-filter + `@xml/accessory_filter` | RC를 꽂으면 Android가 이 앱을 띄울 후보로 보여준다. 필터 XML은 SDK AAR 안에 있다 (`WM160` 등) |
| `com.dji.sdk.API_KEY` meta-data | App Key. `local.properties`에서 읽어오므로 **저장소에 올라가지 않는다** |

### DjiDroneDataSource: 왜 `KeyManager`인가

DJI 4.x에서 값을 받는 방법은 두 가지다.

1. `aircraft.flightController.setStateCallback { ... }`: 10Hz로 상태 묶음을 받는다. 기체 객체가 있어야 걸 수 있고, 기체가 끊겼다 다시 붙으면 **다시 걸어야** 한다.
2. `KeyManager.addListener(key, listener)`: 키 하나(배터리 %, yaw 등)씩 받는다. 기체가 없어도 걸어둘 수 있고, 다시 연결돼도 유지된다.

2번을 골랐다. 키 하나가 `TelemetryUpdate` 하나에 대응하므로 Day 3 설계와 그대로 맞는다.

```
DjiSdk.status ── Registered가 될 때까지 대기 (KeyManager.getInstance()는 등록 전엔 null)
   └─ merge(
        combine(RC CONNECTION, FC CONNECTION)        → Connection
        AIRCRAFT_LOCATION (LocationCoordinate3D)     → Location + Altitude
        ATTITUDE_YAW                                 → Heading
        BatteryKey.CHARGE_REMAINING_IN_PERCENT       → Battery
        combine(ARE_MOTOR_ON, IS_FLYING, FLIGHT_MODE) → Flight
      )
```

키 하나를 Flow로 만드는 부분:

```kotlin
private fun rawKeyValues(key: DJIKey): Flow<Any?> = callbackFlow {
    val keyManager = KeyManager.getInstance()
    val listener = KeyListener { _, newValue -> trySend(newValue) }
    keyManager.addListener(key, listener)   // 1. 등록부터
    trySend(keyManager.getValue(key))       // 2. 그다음 현재 값(리스너는 "바뀔 때만" 알려줌)
    awaitClose { keyManager.removeListener(listener) }  // 3. collect 취소 시 해제
}
```

- **등록 → 현재 값 순서:** 반대로 하면 "현재 값 읽기"와 "등록" 사이에 바뀐 값을 놓친다.
- **`awaitClose`:** Day 4의 `WhileSubscribed(5_000)`가 upstream을 취소하면 여기서 리스너가 해제된다. 앱이 백그라운드에 있는 동안 SDK 리스너도 쉰다.
- **`conflate()`:** 위치와 yaw는 자주 온다. 처리가 밀리면 지난 값은 버리고 최신 값만 쓴다.

### FlightMode 매핑

DJI `FlightMode`는 40개가 넘는다(`GPS_ATTI`, `AUTO_TAKEOFF`, `CONFIRM_LANDING`, `MOTORS_JUST_STARTED`…).
앱은 이것을 `FlightModeKind`(TAKEOFF / LANDING / OTHER) 셋으로만 줄이고, 비행 상태는 공통 함수로 판단한다.

```
모터 값 모름      → UNKNOWN
모터 꺼짐         → ON_GROUND
이륙 계열 모드    → TAKING_OFF   (AUTO_TAKEOFF, ASSISTED_TAKEOFF)
착륙 계열 모드    → LANDING      (AUTO_LANDING, ATTI_LANDING, CONFIRM_LANDING, MOTOR_STOP_LANDING)
isFlying          → IN_AIR
그 외(모터만 돎)  → ON_GROUND
```

### 가짜 SDK (`simsdk`)는 왜 남겨두나

기체 없이 평일에 화면을 돌려보는 용도다. DJI 4.x와 같은 모양(콜백 하나만 받는 `setStateCallback`, 여러 개 받는 리스너, GPS 없으면 NaN, yaw -180~180)으로 만들었다.
별도 스레드에서 10Hz로 콜백을 부르고 40초 시나리오를 무한 반복한다.
`callbackFlow`의 등록, 해제, 현재 값 먼저 받기는 이 가짜 SDK로 단위 테스트한다. 실제 DJI 타입은 JVM 테스트가 안 되기 때문이다.

### DataSource 고르기

```bash
./gradlew installDebug                           # 기본값 sim: 가짜 SDK
./gradlew installDebug -Pminigcs.dataSource=dji  # 실제 DJI SDK
```

어느 쪽이든 `registerApp`은 항상 시도한다. 화면 맨 아래에 등록 상태가 보인다.

## 3. 구현 순서

1. `DAY5` 브랜치 생성 (`release`에서 분기)
2. 가짜 SDK: `simsdk/SimDroneSdk.kt`, `SimDroneSdkDriver.kt`
3. `data/sim/SimSdkDroneDataSource.kt`: `callbackFlow` 3개를 `merge`
4. 공통 번역 규칙: `data/TelemetryMapping.kt` (연결 상태, GPS NaN, 비행 상태)
5. DJI SDK 4.18 의존성 + 빌드 에러 해결 (ConstraintLayout/AppCompat)
6. Manifest, Gradle 설정 (App Key placeholder, USB accessory, targetSdk 34, 패키징)
7. `data/dji/DjiSdk.kt`(등록), `DjiDroneDataSource.kt`(KeyManager), `DjiMapping.kt`(FlightMode)
8. `MiniGcsApplication`: `Helper.install`, 등록, DataSource 선택
9. `SdkStatus`를 화면 하단에 표시
10. 폰에서 실행 → Apache HTTP 크래시 해결 → 등록 실패 메시지 확인

## 4. 코드 작성

```
app/src/main/java/com/jaehoon/minigcs/
├── MiniGcsApplication.kt          ← Helper.install, registerApp, DataSource 선택
├── model/SdkStatus.kt             ← 새로
├── simsdk/                        ← 새로: 가짜 SDK (외부 라이브러리 역할)
│   ├── SimDroneSdk.kt
│   └── SimDroneSdkDriver.kt
└── data/
    ├── TelemetryMapping.kt        ← 새로: SDK 공통 번역 규칙
    ├── sim/
    │   ├── SimSdkDroneDataSource.kt
    │   └── SimSdkMapping.kt
    └── dji/
        ├── DjiSdk.kt
        ├── DjiDroneDataSource.kt
        └── DjiMapping.kt
```

## 5. 확인 방법

```bash
./gradlew testDebugUnitTest   # 22개
./gradlew installDebug
```

폰에서 확인한 결과:
- **sim:** 연결 → 이륙(고도 0→10m) → 선회 → 착륙이 40초마다 반복된다. 하단에 `DJI SDK: 등록 실패 — The app key was not provided.`
- **dji:** "연결 끊김"이고 모든 값이 "—"(정상: 등록 전이라 KeyManager를 못 씀). 하단에 같은 메시지.

App Key를 넣은 뒤(확인함):
- 두 모드 모두 하단 `DJI SDK: 등록 완료`.
- dji 모드는 등록 후 KeyManager 리스너가 크래시 없이 붙는다. 기체가 없으니 "연결 끊김"이 정상이다.
- logcat에 SDK 내부 `NullPointerException: Attempt to get length of null array`(`DJISDKManager.initParams`)가 키를 넣은 뒤에도 찍힌다. SDK가 스스로 잡는 경고라 등록 결과에는 영향이 없다.

### 퀴즈 — SDK 연동

1. `dji-sdk`는 `implementation`, `dji-sdk-provided`는 `compileOnly`다. 각각 무엇이고 왜 다르게 넣는가?
2. `Helper.install`을 `onCreate`가 아니라 `attachBaseContext`에서 부르는 이유는?
3. `rawKeyValues`에서 `addListener`와 `getValue`의 순서를 바꾸면 어떤 버그가 생길 수 있는가?
4. `DjiDroneDataSource`가 `sdk.status`의 `Registered`를 기다리는 이유는?
5. DJI 타입을 쓰는 함수를 JVM 단위 테스트로 검증할 수 없을 때 어떻게 설계했는가?
6. App Key를 `AndroidManifest.xml`에 직접 쓰지 않고 `local.properties`에서 읽는 이유는?

<details>
<summary>모범 답안 요약</summary>

1. `dji-sdk`는 실제 구현이라 APK에 들어가야 한다. `dji-sdk-provided`는 컴파일할 때 타입만 알려주는 스텁이다. 실제 클래스는 `Helper.install`이 런타임에 로드하므로 APK에 넣지 않는다.
2. SDK 클래스는 `Helper.install`이 로드한 뒤에야 쓸 수 있다. `attachBaseContext`는 `Application.onCreate`보다, 또 다른 어떤 컴포넌트보다 먼저 불린다.
3. 현재 값을 읽은 뒤 리스너를 등록하기 전에 값이 바뀌면, 그 변화를 영영 못 받는다. 특히 연결 상태처럼 자주 안 바뀌는 값은 화면이 계속 틀린 값을 보여준다.
4. `KeyManager.getInstance()`가 등록 성공 전에는 null이다. 키 리스너를 걸 수 없다.
5. 판단 로직은 앱 타입(`FlightModeKind`, Boolean)만 받는 순수 함수로 빼서 테스트하고, DJI 타입 → 앱 타입 번역(`toModeKind`)은 분기만 있는 얇은 함수로 남겼다.
6. 키는 개인 계정에 묶인 비밀 값이다. `local.properties`는 `.gitignore` 대상이라 저장소(공개될 수 있음)에 올라가지 않는다.
</details>

## 6. 완료 조건

- [x] `DAY5` 브랜치 생성 (`release`에서 분기)
- [x] DJI SDK 4.18 포함 `./gradlew assembleDebug` 성공
- [x] `./gradlew testDebugUnitTest` 22개 통과
- [x] 폰(Galaxy A32, Android 13)에서 크래시 없이 실행, 등록 시도 → "app key was not provided" 확인
- [x] `-Pminigcs.dataSource=dji` 빌드로 실행 확인 (연결 끊김 표시)
- [x] App Key 발급 → `local.properties`에 등록 → 하단 "등록 완료" 확인 (sim / dji 두 모드 모두)
- [ ] 퀴즈 6문항을 한두 문장으로 말할 수 있다.

## 7. App Key 발급 방법

1. **DJI 개발자 계정 만들기:** https://developer.dji.com 에서 가입하고 로그인한다. DJI 계정(DJI Fly에서 쓰는 것)으로 로그인할 수 있다. 처음이면 개발자 정보(이름, 회사 등)를 추가로 입력하라고 할 수 있다.
2. **앱 생성:** 우측 상단 계정 메뉴 → **User Center** → **Apps** → **Create App**
   | 항목 | 입력값 |
   | --- | --- |
   | App Type | **Mobile SDK** |
   | App Name | `MiniGCS` |
   | Software Platform | **Android** |
   | Package Name | **`com.jaehoon.minigcs`** (`applicationId`와 정확히 같아야 함) |
   | Category / Description | 자유 (예: Education / "Personal GCS study app for Mini 2 telemetry") |
3. **이메일 인증:** 가입 이메일로 활성화 메일이 온다. 링크를 눌러야 키가 생성된다. 안 오면 스팸함을 본다.
4. **키 복사:** User Center → Apps → `MiniGCS`에서 App Key(16진수 문자열)를 복사한다.
5. **프로젝트에 넣기:** 저장소 루트 `local.properties`에 한 줄을 추가한다.
   ```properties
   dji.apiKey=여기에_발급받은_키
   ```
6. **확인:** `./gradlew installDebug` → 앱 실행(인터넷 연결 필요) → 하단 `DJI SDK: 등록 완료`

자주 나는 실패:
- `Invalid App Key` / 패키지명 불일치: Package Name을 `com.jaehoon.minigcs`로 등록했는지 확인한다. 바꿨다면 키를 새로 받아야 한다.
- 첫 실행 때 오프라인이면 등록에 실패한다. 한 번 성공하면 이후에는 캐시된다(주말 야외 확인 항목).

## 8. 주말 실기체에서 확인해야 할 항목 (Weekend Task)

- [ ] `-Pminigcs.dataSource=dji`로 설치 → RC USB 연결 시 Android가 MiniGCS를 띄울지 물어보는가? (`accessory_filter`의 `WM160`이 Mini 2인지)
- [ ] RC만 켰을 때 `RC_ONLY`, 기체까지 켰을 때 `CONNECTED`로 바뀌는가? (`RemoteControllerKey.CONNECTION` / `FlightControllerKey.CONNECTION`)
- [ ] `KeyManager.getValue()`가 등록 직후 null인지, 캐시된 값을 주는지
- [ ] GPS 미수신 시 `AIRCRAFT_LOCATION`이 NaN인지, 0인지, null인지
- [ ] `ATTITUDE_YAW` 범위가 -180~180인지 (Day 1 항목)
- [ ] 지상에서 모터만 켰을 때 `FLIGHT_MODE` 값 (`MOTORS_JUST_STARTED`?)
- [ ] `KeyListener` 콜백 스레드 (메인인지) → 필요하면 `flowOn`
- [ ] 야외(인터넷 없음)에서 앱을 다시 켰을 때 등록이 캐시로 통과하는가

## 메모 (궁금한 점)

-

## 참고 자료

- DJI 공식 Android 샘플 (4.18 설정 출처): https://github.com/dji-sdk/Mobile-SDK-Android
- SDK 통합 가이드: https://developer.dji.com/mobile-sdk/documentation/application-development-workflow/workflow-integrate.html
- App Key / 샘플 실행: https://developer.dji.com/mobile-sdk/documentation/quick-start/index.html
- MSDK 4.17과 Android 14 이슈: https://sdk-forum.dji.net/hc/zh-cn/community/posts/36582965599129-Mobile-SDK-4-17-incompatible-with-Android-14-API-34-registerApp-method-problem
