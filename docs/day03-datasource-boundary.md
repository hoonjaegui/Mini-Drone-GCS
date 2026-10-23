# Day 3 — DataSource 경계와 DroneState 설계

> 환경: 회사(평일) · 실기체 없음 · 데이터 계층 + 단위 테스트까지 (UI 연결은 Day 4)

## 1. 오늘 목표

- Telemetry를 앱 내부 타입 `DroneState`로 표현한다.
- `DroneDataSource` 인터페이스로 SDK 경계를 만들고, `FakeDroneDataSource`로 실기체 없이 데이터를 흘린다.
- `DroneRepository`가 조각난 업데이트를 `Flow<DroneState>` 하나로 합친다.
- 이 구조를 단위 테스트로 검증한다.

## 2. 필요한 개념

### 전체 흐름

```
[주말] DJI SDK 콜백 ─► DjiDroneDataSource ┐
                                          ├─► DroneDataSource ─► Flow<TelemetryUpdate>
[평일] 가짜 시나리오 ─► FakeDroneDataSource ┘        (인터페이스)        │
                                                                       ▼
                                                    DroneRepository (scan으로 누적)
                                                                       │
                                                                       ▼
                                                              Flow<DroneState>
                                                                       │
                                                                       ▼
                                                     ViewModel → Compose  (Day 4)
```

### 왜 DataSource는 `DroneState`가 아니라 `TelemetryUpdate`를 내보내는가

DJI SDK 4.x는 배터리, 위치, 비행 상태 같은 값을 **항목마다 다른 콜백**으로 준다.
배터리 콜백은 배터리만 알고 위치는 모른다. 그래서 DataSource가 매번 완성된 `DroneState`를 만들려면 이전 값을 모두 기억해야 하고, SDK 코드와 상태 관리 코드가 섞인다.

그래서 역할을 나눴다.

| 계층 | 하는 일 | 하지 않는 일 |
| --- | --- | --- |
| `DroneDataSource` | SDK 값을 앱 타입(`TelemetryUpdate`)으로 **번역** | 이전 값 기억, 상태 합치기 |
| `DroneRepository` | 업데이트를 `DroneState`로 **누적**, 값 정규화 | SDK 타입 다루기 |

### `null`은 "모름"이다

`DroneState`의 위치/고도/방향/배터리는 nullable이다.
연결 직후 GPS를 아직 못 잡은 상태를 `0.0, 0.0`으로 채우면 아프리카 앞바다(위도 0, 경도 0)가 진짜 위치처럼 보인다.
값이 없으면 `null`로 두고, 어떻게 보여줄지는 UI가 정한다.

### `scan` — 누적하며 매 단계를 내보내기

```kotlin
updates.scan(DroneState()) { state, update -> state.reduce(update) }
```

`fold`와 같이 값을 누적하지만, 최종값 하나만이 아니라 **중간 상태를 전부** 흘려보낸다.
첫 값으로 초기 상태 `DroneState()`도 바로 나오므로 UI는 연결 전에도 그릴 게 있다.

### `reduce`는 순수 함수

`(이전 상태, 업데이트) → 다음 상태`. 코루틴도 SDK도 없어서 테스트가 쉽다.
Heading 정규화(`-180 → 180`)와 배터리 범위 자르기(0~100)도 여기서 한다.
SDK가 실제로 어떤 범위를 주는지는 아직 모르므로(Day 1 주말 확인 항목), 앱 안에서는 어떤 값이 들어와도 `0 ≤ heading < 360`이 되도록 방어한다.

## 3. 구현 순서

1. `DAY3` 브랜치 생성 (DAY2에서 분기)
2. `model/DroneState.kt`: 상태 모델과 enum (`ConnectionState`, `FlightState`)
3. `data/TelemetryUpdate.kt`: DataSource가 내보내는 업데이트 타입
4. `data/DroneDataSource.kt`: SDK 경계 인터페이스
5. `data/FakeDroneDataSource.kt`: 연결 → GPS → 이륙 → 선회 → 착륙 시나리오
6. `data/DroneRepository.kt`: `scan` + `reduce` + `normalizeHeading`
7. `kotlinx-coroutines-core` / `-test` 의존성 추가 (버전 카탈로그에 `coroutines = "1.10.2"`)
8. `DroneRepositoryTest` 작성 후 테스트 실행

## 4. 코드 작성

```
app/src/main/java/com/jaehoon/minigcs/
├── model/DroneState.kt
└── data/
    ├── TelemetryUpdate.kt
    ├── DroneDataSource.kt
    ├── FakeDroneDataSource.kt
    └── DroneRepository.kt
app/src/test/java/com/jaehoon/minigcs/data/DroneRepositoryTest.kt
```

핵심 인터페이스:

```kotlin
interface DroneDataSource {
    val updates: Flow<TelemetryUpdate>
}
```

`ConnectionState`에 `RC_ONLY`가 있는 이유는 Day 1에서 정리했다. "RC는 붙었는데 기체는 꺼져 있는" 상태를 따로 표현하려는 것이다. Fake 시나리오도 일부러 `DISCONNECTED → RC_ONLY → CONNECTED` 순서로 흘린다.

## 5. 확인 방법

```bash
./gradlew testDebugUnitTest
# → app/build/reports/tests/testDebugUnitTest/index.html
```

테스트 6개:

| 테스트 | 확인하는 것 |
| --- | --- |
| 첫 값은 초기 상태 | `scan`이 업데이트 없이도 `DroneState()`를 먼저 내보냄 |
| 해당 항목만 바뀜 | 배터리 업데이트가 위치를 지우지 않음 |
| heading 정규화 | -180→180, -90→270, 360→0, 370→10 |
| 배터리 범위 | 120→100, -5→0 |
| GPS 상실 | 위치가 다시 `null`로 돌아감 |
| Fake 시나리오 전체 | 연결/비행 상태 순서, 최대 고도 10m, 착륙 후 고도 0 |

Fake 시나리오에는 `delay`가 들어 있지만 `runTest`가 가상 시간으로 건너뛰므로 테스트는 즉시 끝난다.

### 퀴즈 — 경계 설계

1. `DroneDataSource.updates`가 `Flow<DroneState>`가 아니라 `Flow<TelemetryUpdate>`인 이유는?
2. 고도를 `Double` 대신 `Double?`로 둔 이유는? `0.0`으로 두면 어떤 문제가 생기는가?
3. `reduce`를 `DroneRepository` 메서드가 아니라 순수 함수로 뺀 이점은?
4. 기체 연결이 끊겨(`DISCONNECTED`) 다른 값은 마지막 값 그대로 남는다. 이게 문제가 되는 상황과 해결 방법은?
5. 주말에 `DjiDroneDataSource`를 붙일 때 `DroneRepository`와 테스트 코드 중 바뀌어야 하는 것은?

<details>
<summary>모범 답안 요약</summary>

1. SDK가 항목별로 따로 콜백을 주기 때문이다. DataSource는 번역만 하고, 이전 값을 기억해서 합치는 일은 Repository 한 곳에 모은다. 그래야 Fake/DJI 두 구현이 상태 관리 로직을 중복하지 않는다.
2. "아직 모름"과 "진짜 0"을 구분하려고. 고도 0은 "지면에 있음"이라는 의미 있는 값이다. 위치를 0으로 채우면 지도에 위도 0, 경도 0 지점이 찍힌다.
3. 코루틴/Flow 없이 입력과 출력만으로 테스트할 수 있고, 정규화 같은 규칙이 한 곳에 모인다.
4. UI가 끊긴 뒤에도 마지막 고도/위치를 "현재 값"처럼 보여주면 사용자가 오해한다. UI에서 `connection != CONNECTED`이면 값을 흐리게 표시하거나 "마지막 수신값"이라고 표시한다. 또는 Repository에서 끊김 시 값을 비우는 정책을 정한다. 어느 쪽이든 의도적으로 정해야 한다.
5. 둘 다 바뀌지 않는다. `DjiDroneDataSource`라는 새 구현체를 추가하고, 어떤 DataSource를 쓸지 고르는 지점(Day 4의 조립 코드)만 바꾼다.
</details>

## 6. 완료 조건

- [x] `DAY3` 브랜치 생성
- [x] `./gradlew testDebugUnitTest` 6개 테스트 통과
- [x] `./gradlew assembleDebug` 여전히 성공
- [ ] 흐름 그림(SDK → DataSource → Repository → Flow<DroneState>)을 안 보고 그리고 각 계층의 책임을 한 문장으로 말할 수 있다.
- [ ] 퀴즈 5문항을 한두 문장으로 말할 수 있다.

## 7. 주말 실기체에서 확인해야 할 항목 (Weekend Task)

- [ ] SDK에서 각 Telemetry 항목이 **어느 콜백/키**로 오는지 목록화한다. 이 목록이 `TelemetryUpdate` 종류와 1:1로 맞는지 본다.
- [ ] 콜백이 어느 스레드에서 오는가? (메인 스레드인지 → `callbackFlow` + `flowOn` 필요 여부)
- [ ] 콜백 빈도: 위치/고도는 초당 몇 번 오는가? (UI가 너무 자주 다시 그려지면 `sample`/`conflate` 검토)
- [ ] SDK의 비행 상태 값 목록을 보고 `FlightState` 5개로 매핑 가능한지, 빠진 상태(예: 모터만 켜진 상태)가 있는지 확인
- [ ] 연결이 끊긴 순간 SDK가 마지막 값을 다시 보내는지, 아무것도 안 보내는지 확인 (퀴즈 4번 정책 결정에 필요)

## 메모 (궁금한 점)

-
