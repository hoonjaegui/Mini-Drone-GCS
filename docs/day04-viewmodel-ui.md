# Day 4 — ViewModel과 Telemetry 화면 연결

> 환경: 회사(평일) · 실기체 없음 · Fake 데이터가 실제 화면에 보이는 것까지

## 1. 오늘 목표

- Day 3의 `Flow<DroneState>`를 `ViewModel → Compose` 화면까지 연결한다.
- 어떤 DataSource를 쓸지 고르는 **조립 지점**을 한 곳으로 만든다.
- 화면 회전, 백그라운드 전환 때 Flow 수집이 어떻게 되는지 설명할 수 있다.

## 2. 필요한 개념

### 전체 흐름 (Day 1에서 그린 그림의 완성)

```
MiniGcsApplication      ← 조립: FakeDroneDataSource()를 여기서 고른다
   └─ DroneRepository   (Day 3)
        └─ droneState : Flow<DroneState>        cold, 구독할 때 시작
             ▼ stateIn(viewModelScope, WhileSubscribed(5_000), DroneState())
TelemetryViewModel.state : StateFlow<DroneState> hot, 항상 현재 값이 있음
             ▼ collectAsStateWithLifecycle()
TelemetryRoute   (ViewModel과 연결하는 얇은 껍데기)
             ▼ state: DroneState
TelemetryScreen  (stateless: 값만 받아서 그림 → Preview 가능)
```

### Cold Flow vs StateFlow

| | `Flow<DroneState>` (Repository) | `StateFlow<DroneState>` (ViewModel) |
| --- | --- | --- |
| 언제 시작? | `collect`할 때마다 **처음부터 새로** | 공유됨. 여러 구독자가 같은 upstream 하나를 봄 |
| 현재 값 | 없음 | `.value`가 항상 있음 (초기값 `DroneState()`) |
| 비유 | 녹화 영상: 틀 때마다 처음부터 | 생방송: 켜면 지금 장면 |

UI는 "지금 상태"가 필요하므로 ViewModel에서 `stateIn`으로 StateFlow로 바꾼다.

### `WhileSubscribed(5_000)`의 의미

- 화면이 보이는 동안: upstream(Repository → DataSource)을 수집한다.
- 화면 회전: Activity는 다시 만들어지지만 ViewModel은 살아 있다. 구독이 1초도 안 되게 끊겼다가 다시 붙으므로 5초 안이라 upstream이 **유지된다**. Fake 시나리오가 처음부터 다시 시작되지 않는다.
- 홈 버튼으로 5초 넘게 나가 있으면: upstream 수집을 **멈춘다**. 주말에 DJI SDK를 붙이면 이때 SDK 리스너도 해제되어 배터리를 아낀다.
- 다시 돌아오면: Repository의 cold flow를 처음부터 다시 수집한다. Fake는 시나리오가 처음부터 다시 재생되고, 실기체는 그 시점의 실제 값부터 받는다.

### `collectAsStateWithLifecycle` vs `collectAsState`

`collectAsState`는 앱이 백그라운드여도 계속 수집한다. 그러면 위 `WhileSubscribed`의 5초 타이머가 시작되지 않는다.
`collectAsStateWithLifecycle`은 화면이 `STARTED` 미만이 되면 수집을 멈춰서 구독이 실제로 끊기게 한다.

### Route / Screen 분리

- `TelemetryRoute`: ViewModel을 얻고 state를 꺼내는 일만 한다.
- `TelemetryScreen(state: DroneState)`: ViewModel을 모른다. 그래서 Preview에서 `DroneState(...)`를 직접 넣어 "비행 중", "연결 전" 화면을 앱 실행 없이 볼 수 있다.

### null → 화면 문자열은 한 곳에서

`TelemetryFormat.kt`에서 `null`을 전부 `"—"`로 바꾼다(위치는 `"GPS 수신 대기"`). Composable 안에 `if (x == null)`이 흩어지지 않고, 문자열 규칙은 단위 테스트로 확인한다.

### 끊김 표시 정책 (Day 3 퀴즈 4번의 결정)

Repository는 끊겨도 마지막 값을 **그대로 둔다**. 대신 UI에서 `connection != CONNECTED`이면 값 영역을 흐리게(alpha 0.4) 그린다.
"마지막으로 받은 값"이라는 정보는 남기되, 현재 값으로 오해하지 않게 한다.

## 3. 구현 순서

1. `DAY4` 브랜치 생성 (`release`에서 분기)
2. 의존성: lifecycle 2.9.2 → 2.9.4 (로컬 캐시에 compose 모듈이 2.9.4부터 있음), `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` 추가
3. `MiniGcsApplication`: 조립 지점. Manifest의 `android:name`에 등록
4. `TelemetryViewModel` + `Factory` (`viewModelFactory { initializer { … } }`)
5. `TelemetryFormat.kt`: 값 → 문자열
6. `TelemetryScreen.kt`: Route / Screen / Preview 2개
7. `MainActivity`: `PlaceholderScreen` → `TelemetryRoute`
8. 테스트 작성 → `testDebugUnitTest`, `installDebug`로 폰에서 확인

## 4. 코드 작성

```
app/src/main/java/com/jaehoon/minigcs/
├── MiniGcsApplication.kt          ← 새로: 조립 지점
├── MainActivity.kt                ← TelemetryRoute로 교체
└── ui/telemetry/
    ├── TelemetryViewModel.kt
    ├── TelemetryFormat.kt
    └── TelemetryScreen.kt
app/src/test/java/com/jaehoon/minigcs/ui/telemetry/
├── TelemetryFormatTest.kt
└── TelemetryViewModelTest.kt
```

DJI로 갈아끼울 때 바뀌는 곳은 이 한 줄뿐이다.

```kotlin
// MiniGcsApplication.kt
private val droneDataSource: DroneDataSource by lazy { FakeDroneDataSource() }
```

## 5. 확인 방법

```bash
./gradlew testDebugUnitTest   # 11개 (Day 3: 6 + Day 4: 5)
./gradlew installDebug        # 폰/에뮬레이터에 설치
```

앱을 켜면 약 18초 동안 Fake 시나리오가 재생된다.

1. 빨강 "연결 끊김" → 주황 "조종기만 연결됨" → 초록 "기체 연결됨" (끊김 동안 값 영역이 흐림)
2. 위치 "GPS 수신 대기" → 좌표
3. 비행 상태 지상 → 이륙 중(고도 1→10m) → 비행 중(방향 회전, 배터리 감소) → 착륙 중 → 지상

직접 해볼 것:
- 비행 중에 **화면을 회전**한다. 시나리오가 처음부터 다시 시작되지 않아야 한다.
- 비행 중에 **홈 버튼 → 10초 대기 → 앱 복귀**를 한다. 시나리오가 처음부터 다시 시작된다. 왜 그런지 위 `WhileSubscribed` 설명으로 말해보기.
- Android Studio에서 `TelemetryScreen.kt`를 열고 Preview 2개를 본다.

### 퀴즈 — 화면까지의 흐름

1. Repository의 `Flow`를 Compose에서 바로 `collect`하지 않고 ViewModel에서 `stateIn`하는 이유는?
2. 화면 회전 때 시나리오가 처음부터 다시 시작되지 않는 이유는? 홈 버튼으로 10초 나갔다 오면 다시 시작되는 이유는?
3. `collectAsState`를 쓰면 2번의 동작이 어떻게 달라지는가?
4. `TelemetryScreen`이 ViewModel이 아니라 `DroneState`를 파라미터로 받으면 어떤 이점이 있는가?
5. 주말에 DJI로 바꿀 때 수정해야 하는 파일은 몇 개인가?

<details>
<summary>모범 답안 요약</summary>

1. cold flow라서 화면이 다시 그려지거나 Activity가 다시 만들어질 때마다 처음부터 다시 수집된다. ViewModel에서 StateFlow로 만들면 upstream 하나를 공유하고, 항상 현재 값이 있으며, 회전에도 살아남는다.
2. 회전은 구독이 5초 안에 다시 붙으므로 upstream이 유지된다. 10초 나가 있으면 5초가 지나 upstream이 취소되고, 복귀하면 cold flow를 새로 수집한다.
3. 백그라운드에서도 계속 수집하므로 구독이 끊기지 않는다. 시나리오가 처음부터 다시 시작되지는 않지만, 그 대신 안 보이는 동안에도 SDK 리스너가 계속 돈다.
4. ViewModel, Repository 없이 원하는 상태를 직접 넣어 Preview나 테스트를 할 수 있다. 화면은 "그리기"만 책임진다.
5. 새 파일 `DjiDroneDataSource.kt`를 추가하고 `MiniGcsApplication.kt`의 한 줄을 바꾸면 된다. 그 외 Repository, ViewModel, UI, 테스트는 바뀌지 않는다. (SDK 의존성, Manifest 권한, App Key 설정은 따로 필요하다.)
</details>

## 6. 완료 조건

- [x] `DAY4` 브랜치 생성 (`release`에서 분기)
- [x] `./gradlew testDebugUnitTest` 11개 통과
- [x] 폰에서 Fake 시나리오가 화면에 표시됨 (연결됨 / 비행 중 / 고도 10.0 m 확인)
- [ ] 화면 회전, 홈 버튼 실험을 직접 해보고 결과를 설명할 수 있다.
- [ ] 퀴즈 5문항을 한두 문장으로 말할 수 있다.

## 7. 주말 실기체에서 확인해야 할 항목 (Weekend Task)

- [ ] `MiniGcsApplication`의 DataSource를 `DjiDroneDataSource`로 바꿨을 때 다른 파일 수정 없이 빌드되는가?
- [ ] 실기체에서 앱을 백그라운드로 보냈다가 복귀했을 때 연결 상태가 어떻게 보이는가? (SDK 리스너 해제/재등록 시 첫 값이 바로 오는지)
- [ ] 실제 콜백 빈도에서 화면이 버벅이지 않는가? (Day 3 주말 항목의 `sample`/`conflate` 판단)
- [ ] 야외 햇빛 아래에서 흐린(alpha 0.4) 값과 정상 값이 구분되는가? 색 대비가 충분한가?

## 메모 (궁금한 점)

-
