# Day 2 — Android 프로젝트 생성

> 환경: 회사(평일) · 실기체 없음 · 빈 앱이 빌드/실행되는 것까지

## 1. 오늘 목표

- `MiniGCS` Android 프로젝트(Kotlin + Jetpack Compose)를 만들고 빌드가 통과한다.
- 프로젝트 파일들이 각각 무슨 역할인지 설명할 수 있다.
- 아직 DJI SDK는 넣지 않는다. 이유를 설명할 수 있다.

## 2. 필요한 개념

### 프로젝트 구성

```
Mini-Drone-GCS/                 ← Gradle 루트 (docs/와 앱이 같은 저장소)
├── settings.gradle.kts         ← 어떤 모듈이 있는지(:app), 라이브러리를 어디서 받는지
├── build.gradle.kts            ← 루트: 플러그인 버전만 선언
├── gradle/libs.versions.toml   ← 버전 카탈로그: 모든 라이브러리 버전을 한 곳에서 관리
├── gradle.properties           ← Gradle/AndroidX 설정
└── app/
    ├── build.gradle.kts        ← 앱 모듈: SDK 버전, applicationId, 의존성
    └── src/main/
        ├── AndroidManifest.xml ← 앱이 OS에 자기소개하는 문서 (Activity, 권한)
        ├── java/com/jaehoon/minigcs/
        │   ├── MainActivity.kt ← 앱 진입점, Compose 화면을 띄움
        │   └── ui/theme/       ← Material3 테마
        └── res/                ← 문자열, 아이콘, XML 테마
```

### 주요 설정값

| 항목 | 값 | 이유 |
| --- | --- | --- |
| applicationId | `com.jaehoon.minigcs` | DJI App Key는 **패키지명에 묶여** 발급된다. 나중에 바꾸면 키를 다시 받아야 함 |
| minSdk | 26 (Android 8.0) | 적응형 아이콘 기본 지원, 현재 쓰이는 폰 대부분 커버 |
| compile/targetSdk | 36 | 설치된 최신 플랫폼 |
| AGP / Kotlin / Gradle | 8.12.3 / 2.2.10 / 8.13 | 로컬 캐시에 있는 안정 조합 |
| JDK | Android Studio 내장 JBR 21 (toolchain 17) | 시스템 JDK 18은 사용하지 않음 |

### 왜 DJI SDK를 아직 안 넣는가

- Mini 2는 **DJI Mobile SDK 4.x** 대상이다. SDK 4.x는 App Key 등록, 네이티브 라이브러리, 권한 설정 등 붙일 게 많고 기체 없이는 동작 확인이 안 된다.
- Day 1에서 정리한 대로 SDK는 `DroneDataSource` 하나 뒤에 숨긴다. 그 경계를 먼저 만들고(Day 3), SDK는 주말에 `DjiDroneDataSource`로 붙인다.
- 지금 넣으면 "빌드가 왜 깨졌는지"가 SDK 문제인지 내 코드 문제인지 구분하기 어려워진다.

## 3. 구현 순서

1. `DAY2` 브랜치 생성 (DAY1에서 분기)
2. Gradle 설정 파일 작성 (settings / 루트 build / 버전 카탈로그 / properties)
3. `app` 모듈 작성 (build.gradle.kts, Manifest, MainActivity, 테마, 리소스)
4. Gradle Wrapper 생성 (`gradlew`) — 누가 받아도 같은 Gradle 버전으로 빌드되게
5. `./gradlew assembleDebug` 로 빌드 확인
6. 에뮬레이터 또는 폰에서 실행

## 4. 코드 작성

현재 화면은 자리표시자(Placeholder) 하나뿐이다.

```kotlin
// Day 3 이후 DroneState를 그리는 화면으로 교체된다.
@Composable
fun PlaceholderScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "MiniGCS — 기체 미연결")
    }
}
```

## 5. 확인 방법

```bash
# 빌드
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk 생성

# 연결된 기기/에뮬레이터에 설치
./gradlew installDebug
```

또는 Android Studio에서 프로젝트 루트(`Mini-Drone-GCS/`)를 열고 ▶ Run.

### 퀴즈 — 이 파일은 왜 필요한가?

1. `libs.versions.toml`이 없으면 어떤 불편이 생기는가?
2. `applicationId`와 `namespace`는 각각 무엇에 쓰이는가?
3. `gradlew`를 저장소에 커밋하는 이유는?
4. `local.properties`는 왜 `.gitignore`에 들어가는가?

<details>
<summary>모범 답안 요약</summary>

1. 모듈이 늘어날 때 버전이 파일마다 흩어져 서로 어긋난다. 한 곳에서 올리면 끝나는 장점이 사라진다.
2. `applicationId`는 기기/스토어에서 앱을 구분하는 ID(= DJI App Key 등록 대상), `namespace`는 코드의 `R` 클래스·BuildConfig가 생성되는 패키지.
3. 저장소를 받은 사람(또는 CI)이 Gradle을 따로 설치하지 않아도 정확히 같은 버전으로 빌드하기 위해.
4. 내 컴퓨터의 Android SDK 경로 같은 **개인 환경 값**이라 다른 사람에겐 틀린 값이다.
</details>

## 6. 완료 조건

- [x] `DAY2` 브랜치 생성
- [x] `./gradlew assembleDebug` 성공
- [ ] 에뮬레이터 또는 폰에서 "MiniGCS — 기체 미연결" 화면 확인
- [ ] 퀴즈 4문항을 한두 문장으로 말할 수 있다.

## 7. 주말 실기체에서 확인해야 할 항목 (Weekend Task)

- [ ] DJI 개발자 계정에서 `com.jaehoon.minigcs`로 App Key 발급
- [ ] 실기체 연결에 쓸 폰의 Android 버전 확인 (minSdk 26 이상인지)
- [ ] 폰 ↔ RC-N1 USB 연결 시 Android가 어떤 앱을 띄우려 하는지 확인 (USB Accessory 인텐트)

## 메모 (궁금한 점)

-
