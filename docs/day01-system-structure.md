# Day 1 — 드론 시스템 구조 이해

> 환경: 회사(평일) · 실기체 없음 · 코드 없음(개념 정리만)

## 1. 오늘 목표

- `Android App → DJI SDK → Remote Controller → Drone` 구조를 남에게 설명할 수 있다.
- Telemetry와 Command를 구분할 수 있다.
- 이 두 가지가 이후 Day 3의 `DroneDataSource → Repository → Flow<DroneState>` 구조와 어떻게 이어지는지 안다.

## 2. 필요한 개념

### GCS (Ground Control Station)
지상에서 드론의 상태를 보고(Telemetry 수신), 필요하면 명령을 보내는(Command 송신) 시스템.
이번에 만들 `MiniGCS` 앱이 바로 이 GCS다. 단, 이번 Phase에서는 **보는 쪽(Telemetry)** 에 집중한다.

### Flight Controller (FC)
드론 기체 안에 있는 비행 제어 컴퓨터. 센서(IMU, 기압계, 나침반, GPS)를 읽고 모터 4개의 회전수를 초당 수백 번 조절해서 기체를 띄운다.

- Android 앱은 모터를 직접 돌리지 않는다.
- 앱은 FC에게 "상태 알려줘" / "이륙해" 같은 **고수준 요청**만 한다.
- Mini 2의 FC는 DJI 독자 펌웨어다. 내부는 볼 수 없고, SDK가 열어준 범위만 접근 가능하다.

### Telemetry (드론 → GCS)
기체가 지상으로 보내는 상태 정보. 이번 앱이 다룰 항목:

| 항목 | 의미 | 메모 |
| --- | --- | --- |
| Connection | 앱이 기체를 인식하고 있는가 | 앱 쪽에서 판단하는 상태 |
| Latitude / Longitude | GPS 위치 | GPS 신호가 약하면 값이 없거나 0일 수 있음 |
| Altitude | 고도 | 보통 **이륙 지점 기준 상대 고도** (해발 고도 아님) |
| Heading | 기수 방향 | 북쪽 기준 각도. 0~360이 아닐 수도 있음 → 주말 확인 |
| Battery | 기체 배터리 % | 조종기 배터리와 별개 |
| Flight State | 지상/이륙 중/비행 중/착륙 중 | SDK 값을 앱 상태로 변환해야 함 |

### Command (GCS → 드론)
GCS가 기체에 내리는 명령. 예: Arm, Takeoff, Land, Goto, RTL(Return To Launch).
Command는 기체를 실제로 움직이므로 **위험**하고, 실패/거절 처리도 필요하다.
→ 이번 2주는 Command를 구현하지 않는다. 존재만 알고 넘어간다.

### Remote Controller (RC)
Mini 2 실험에서의 실제 데이터 경로:

```
DJI Mini 2 (FC)
   ⇅  무선 (OcuSync 2.0)
Remote Controller (RC-N1)
   ⇅  USB 케이블
Android 스마트폰
   ⇅  DJI Mobile SDK 4.x (앱에 포함된 라이브러리)
MiniGCS 앱 코드
```

핵심 포인트:
- 스마트폰은 드론과 **직접** 통신하지 않는다. RC가 중계기 역할을 한다.
- 그래서 "앱 ↔ RC 연결"과 "RC ↔ 기체 연결"은 **별개의 연결**이다.
  RC는 연결됐는데 기체는 꺼져 있는 상태가 가능하다 → Connection 상태가 생각보다 단순하지 않다.
- DJI SDK는 이 복잡한 통신을 감추고, 앱에는 콜백/리스너 형태로 값을 넘겨준다.

### 이 구조가 앱 코드 구조로 이어지는 방식

```
[기체 세계]                    [앱 세계]
Mini 2 → RC → DJI SDK   ──►   DroneDataSource   (SDK 콜백을 받는 유일한 곳)
                               ↓
                              DroneRepository   (앱이 쓰기 좋은 형태로)
                               ↓
                              Flow<DroneState>  (Telemetry의 앱 내부 표현)
                               ↓
                              ViewModel → Compose
```

SDK 경계 위쪽은 내가 바꿀 수 없는 세계, 아래쪽은 내가 설계하는 세계다.
이 경계가 있어야 평일엔 `FakeDroneDataSource`, 주말엔 `DjiDroneDataSource`로 갈아끼울 수 있다(Day 3).

## 3. 구현 순서 (오늘은 학습 순서)

1. 위 개념 5개(GCS / FC / Telemetry / Command / RC)를 읽는다.
2. 데이터 경로 그림을 **안 보고** 종이에 다시 그린다.
3. 아래 "확인 방법" 퀴즈를 푼다.
4. 주말 확인 항목을 읽고, 궁금한 점을 이 문서 맨 아래 메모에 적는다.

## 4. 코드 작성

오늘은 코드를 작성하지 않는다. Android 프로젝트 생성은 Day 2, `DroneState` 설계는 Day 3의 범위다.

## 5. 확인 방법

### 퀴즈 A — Telemetry인가 Command인가?

1. 배터리 잔량 63%
2. 이륙해라
3. 현재 기수 방향 137°
4. 홈 포인트로 돌아와라
5. 지금 비행 중이다
6. 고도 30m까지 올라가라
7. GPS 위성 12개 수신 중

<details>
<summary>정답</summary>

Telemetry: 1, 3, 5, 7 / Command: 2, 4, 6

6번 주의: "고도 30m"라는 숫자가 있어도 **방향이 GCS → 드론**이면 Command다.
</details>

### 퀴즈 B — 설명해보기 (소리 내어 1분)

1. 스마트폰이 Mini 2와 직접 연결되지 않는 이유는?
2. 앱이 모터를 직접 돌리지 않는다면, 앱은 FC에게 무엇을 하는가?
3. "RC 연결됨"과 "기체 연결됨"이 다른 상태일 수 있는 상황은?
4. UI가 DJI SDK 객체를 직접 쓰지 않고 `DroneState`를 거치게 하면 평일 개발에 어떤 이점이 있는가?

<details>
<summary>모범 답안 요약</summary>

1. 스마트폰엔 드론용 장거리 무선 장비가 없다. RC가 무선 링크를 담당하고 폰은 USB로 RC에 붙는다.
2. 상태를 읽고(Telemetry), 고수준 요청(Command)을 보낸다. 실제 제어 루프는 FC가 돌린다.
3. RC 전원 ON + 폰 연결 완료, 그런데 기체 전원 OFF 또는 신호가 끊긴 경우.
4. 실기체 없이 Fake 데이터로 UI/구조를 완성하고, 주말엔 DataSource만 교체하면 된다.
</details>

## 6. 완료 조건

- [ ] `Android App → DJI SDK → RC → Drone` 그림을 안 보고 그리고 각 화살표를 설명할 수 있다.
- [x] 퀴즈 A를 모두 맞혔다.
- [x] 퀴즈 B 4문항을 각각 한두 문장으로 말할 수 있다.

## 7. 주말 실기체에서 확인해야 할 항목 (Weekend Task)

평일에는 확인할 수 없으므로 Weekend 1로 넘긴다.

- [ ] 연결 순서: RC 켜기 → 폰 USB 연결 → 기체 켜기 순서에 따라 앱에서 연결 상태가 어떻게 바뀌는가?
- [ ] RC만 켜고 기체는 끈 상태에서 앱이 "연결됨"으로 보는가, "끊김"으로 보는가?
- [ ] Altitude: 지면에 둔 상태에서 0 근처인가? (이륙 지점 기준 상대 고도인지 확인)
- [ ] Heading: 기체를 손으로 돌렸을 때 값 범위가 0~360인가, -180~180인가? 북쪽이 0인가?
- [ ] GPS: 실내/실외에서 위도·경도 값이 각각 어떻게 들어오는가? (null, 0, 또는 NaN?)
- [ ] Battery: 앱 값과 DJI Fly 앱(또는 기체 LED) 값이 일치하는가?
- [ ] Flight State: 모터를 켜지 않은 지상 상태에서 SDK가 어떤 값을 주는가?

## 메모 (궁금한 점)

-
