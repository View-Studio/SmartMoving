# 종합 리서치 — 키 커맨드 조합 시스템 (#4)

> **포커스 #4 1차 근거 문서**. 4 Agent 병렬 read 결과 (2026-04-25, 세션 1).
> 원본 4 파일 + vanilla 1.21.1 KeyBinding + 1.21.1 5 코드 모두 라인별 전수 read.
> **모든 라인-by-라인 작업의 1차 근거**.

| 영역 | 파일 | 줄 수 | Agent |
|---|---|---|---|
| SmartMovingSelf 키 사용 위치 | SmartMovingSelf.java | 3345 (키 위치만 발췌) | A |
| Button + Client + Mod + Context | Button + Client + Mod + Context | ~600 | B |
| SmartMovingOptions + vanilla 키 | SmartMovingOptions.java | 397 | C |
| 1.21.1 매핑 + Mixin | ClientState + Jumper + Mixin + Keys | (참고) | D |

---

## §1. SmartMovingSelf 키 사용 위치 (Agent A)

### 1.1 4 메인 키 + 7 추가 Button (총 11 Button 인스턴스)

**메인 키** (이동·물리·토글):
- `jumpButton` 12 회 (StartPressed 4 / StopPressed 2 / Pressed 6)
- `sneakButton` 22 회 (StartPressed 5 / StopPressed 4 / Pressed 13)
- `grabButton` 18 회 (StartPressed 3 / Pressed 15)
- `sprintButton` 4 회 (Pressed 3 / StopPressed 1)

**추가 Button**:
- `forwardButton` / `leftButton` / `rightButton` / `backButton` (4 방향 키 — 더블클릭 카운터)
- `toggleButton` / `speedIncreaseButton` / `speedDecreaseButton` (3 SM 커스텀 키)

### 1.2 `Button.update()` 호출 위치 (★ 핵심)

`SmartMovingSelf.updateEntityActionState` **L2380-L2387** (매 ClientTick 1 회):
```java
forwardButton.update(gameSettings.keyBindForward);
leftButton.update(gameSettings.keyBindLeft);
rightButton.update(gameSettings.keyBindRight);
backButton.update(gameSettings.keyBindBack);
jumpButton.update(esp.movementInput.jump);          // boolean 직접 전달
sprintButton.update(gameSettings.keyBindSprint);    // KeyBinding 전달
sneakButton.update(esp.movementInput.sneak);        // boolean 직접 전달
grabButton.update(Options.keyBindGrab);             // KeyBinding 전달
```

### 1.3 차지/헤드/벽/방향 점프 분기

| 분기 | 라인 | 진입 조건 | 핵심 |
|---|---|---|---|
| 차지 점프 | L1857-L1878 | sneak hold + esp.jump | jumpCharge++ → release 시 tryJump(CHARGE_UP) |
| 헤드 점프 | L1881-L1899 | grab.Pressed && (sprinting/sprintJump/running) + esp.jump | headJumpCharge++ → release 시 tryJump(HEAD_UP) |
| 벽 점프 | L2863-L2897 | jumpButton 더블탭 (wallJumpCount 타이머) 또는 단일탭 | triggerWallJumping = true |
| 방향 점프 | L1918-L1943 + L2904-L2964 | left/right/back 더블탭 (count == -1) | tryJump(ANGLE, ..., relAngle) |

### 1.4 R-09 토글 본체 (★ L2968-L3045)

5-갈래 의사결정 트리:
- **willStopCrawl** / **willStopCrawlStartSneak** (jumpButton.StopPressed / sneakButton.StopPressed 트리거)
- **willStopSneak** (4 조건 OR — sneakKey.StartPressed 더블탭 / wantSneak+wantSprint+sneakKey.StartPressed / wasSneaking+sneakKey.StartPressed / jumpKey.StopPressed)
- **willStartSneak** (3 조건 OR — willStopCrawlStartSneak+sneakKey.StopPressed / isFast+sneakKey.StopPressed / isSlow+!wasSneaking)
- **willStartCrawl** (isCrawling+!wasCrawling / isClimbCrawling+!wasClimbCrawling)
- 적용: `sneakToggled = willStartSneak / false (willStopSneak)`, `crawlToggled = willStartCrawl / false (willStopCrawl)`

### 1.5 같은 프레임 내 다중 이벤트 우선순위

```
1. Button.update() 모두 갱신 (frame 시작, L2380-L2387)
2. 상태 계산 (wouldWantCrawl → wouldWantClimb → wantSliding → wantSneak/wantSprint → wantWallJumping)
3. R-09 토글 처리 (L2968-L3045 — 마지막)
```

→ **AND 조건으로 우선순위 표현** + **StartPressed 는 한 frame 1 회만 true** (이전 WasPressed=false 조건).

---

## §2. Button.java 메커니즘 (Agent B)

### 2.1 Button.java 본체 (72 줄)

```java
public boolean Pressed;        // 현재 tick 상태
public boolean WasPressed;     // 이전 tick 상태
public boolean StartPressed;   // edge: !WasPressed && Pressed
public boolean StopPressed;    // edge: WasPressed && !Pressed

public void update(boolean pressed) {
    WasPressed = Pressed;
    Pressed = pressed;
    StartPressed = !WasPressed && Pressed;
    StopPressed = WasPressed && !Pressed;
}
```

★ **다중 참조 안전**: 매 tick 1 회 update() 호출 후 4 필드 저장 — 같은 tick 내 여러 핸들러가 `if (jumpButton.StartPressed)` 다중 참조 가능.

### 2.2 1.7.10 4 커스텀 KeyBinding

`SmartMovingOptions` 생성자:
- `keyBindGrab` = 29 (LCONTROL)
- `keyBindConfigToggle` = 67 (F9)
- `keyBindSpeedIncrease` = 24 (O)
- `keyBindSpeedDecrease` = 23 (I)

`SmartMovingContext` 등록 (L79-L82): `ClientRegistry.registerKeyBinding(...)` 4 회.

---

## §3. SmartMovingOptions 옵션 + vanilla 키 (Agent C)

### 3.1 33 옵션 분류

| 영역 | 개수 | 1.21.1 이식 |
|---|---|---|
| 키 KeyCode | 4 | ✅ SmartMovingKeys (GLFW 매핑) |
| 토글 (sneak/crawl) | 2 | ✅ cfg.sneakToggle / crawlToggle |
| 차징/점프 | 5 | ✅ jumpCharge / Maximum / Factor / CancelOnSneakRelease / headJump |
| 헤드 점프 보조 | 2 | ✅ headJumpControlFactor / headJumpChargeMaximum |
| 스프린트 릴리스 | 2 | ✅ runOnSprintRelease / walkOnSprintRelease |
| Display | 2 | ✅ displayExhaustionBar / displayJumpChargeBar |
| 비행/다이빙 | 4 | ✅ flyCloseToGround / flyWhileOnGround / flyControlVertical / diveControlVertical |
| **채팅** ★ | **8** | **❌ 미이식** |

### 3.2 ★ 미이식 8 채팅 옵션 발견

| 원본 옵션 | 설정 키 | 기본값 | 1.21.1 |
|---|---|---|---|
| `_configChat` | move.config.chat | true | ❌ |
| `_configChatInit` | move.config.chat.init | true | ❌ |
| `_configChatInitHelp` | move.config.chat.init.help | true | ❌ |
| `_configChatServer` | move.config.chat.server | true | ❌ |
| `_speedChat` | move.speed.chat | true | ❌ |
| `_speedChatInit` | move.speed.chat.init | true | ❌ |
| `_speedChatInitHelp` | move.speed.chat.init.help | true | ❌ |
| `_speedChatServer` | move.speed.chat.server | true | ❌ |

**영향**: 게임 시작 / 설정 변경 / 속도 변경 시 채팅 메시지 송출 옵션 — 우선순위 ↓ (UX, 핵심 키 동작 무관).

### 3.3 ★ KeyBinding.wasPressed() 메커니즘

```java
public boolean wasPressed() {
    if (this.timesPressed > 0) {
        this.timesPressed--;   // ★ 카운터 소비
        return true;
    }
    return false;
}
```

→ **같은 tick 2 회 호출 시 2 번째부터 false**. 원본 `Button.StartPressed` (필드, 다중 참조 가능) 와 의미 차이.

---

## §4. 1.21.1 매핑 — 키 엣지 + 토글 + 더블클릭 (Agent D)

### 4.1 키 엣지 필드 10 개 (ClientState)

| 필드 | 역할 | 갱신 위치 |
|---|---|---|
| `jumpKeyStartPressed` / `jumpKeyStopPressed` / `prevJumpKeyPressed` | jump 엣지 | tickEssential L826-830 |
| `sneakKeyStartPressed` / `sneakKeyStopPressed` / `prevSneakKeyPressed` | sneak 엣지 | tickEssential L832-835 |
| `sprintKeyStartPressed` / `sprintKeyStopPressed` / `prevSprintKeyPressed` | sprint 엣지 | tickEssential L837-842 |
| `grabJustPressed` | grab 1-shot 캐시 | tickEssential L847 |

★ **메커니즘**: `isPressed()` + 이전 tick 비교 (Button 패턴 그대로). 한 tick 1 회만 갱신, 다중 참조 안전. `grab` 만 `wasPressed()` → 캐시 필드 (counter-consuming 우회).

### 4.2 R-09 토글 1.21.1 매핑 (★ L1761-L1840)

원본 L2966-L3045 ↔ ClientState L1761-L1840 — **5 갈래 의사결정 트리 모두 1:1**:
- L1769-L1779 willStopCrawl / willStopCrawlStartSneak
- L1787-L1799 willStopSneak (4 조건 OR)
- L1802-L1810 willStartSneak (3 조건 OR)
- L1812-L1819 willStartCrawl
- L1821-L1831 sneakToggled / crawlToggled 적용

### 4.3 IMPL-03 더블클릭 1.21.1 매핑 (★ L1482-L1524)

```java
// L1485-L1488: 방향키 isPressed
// L1490-L1492: rising edge (startLeft / startRight / startBack)
// L1494-L1496: prev 저장
// L1500-L1514: 카운터 갱신 (count == 0 → angleTicks 또는 -1 발동)
// L1517-L1523: 대각선 우선순위 (-1 중복 → -2 강등 / -2 → -1 승격)
```

★ 카운터 상태 4 가지 (`0` 비활성 / `>0` 첫 클릭 대기 / `-1` 발동 예약 / `-2` 대각 대기) 모두 보존.

### 4.4 벽 점프 wallJumpCount

ClientState L531-L539 — 원본 L1895-L1898 + L2863-L2897 1:1.

### 4.5 jumpPending / jumpAvoided 흐름

```
vanilla jump() 호출
  ↓
MixinLivingEntityClient.sm_jump (HEAD inject L231-L238)
  jumpAvoided = true
  jumpPending = true
  ci.cancel()
  ↓
다음 tickEssential 진입
  L806: jumpAvoided = false (매 tick 초기화)
  ↓
SmartMovingJumper.handleJumping (sm_travel_client L73)
  L451: 수면 점프 (isDipping && jumpPending)
  L473: 일반 점프 (jumpPending && !isClimbing && !isCrawlClimbing && !isTouchingWater && !isInLava)
  ↓
tryJump 호출
  ↓
jumpPending = false (블록 종료)
```

### 4.6 SmartMovingKeys (1.21.1, 52 줄)

```java
KeyBindingHelper.registerKeyBinding(grab           = ... GLFW_KEY_LEFT_CONTROL ...);
KeyBindingHelper.registerKeyBinding(configToggle   = ... GLFW_KEY_F9 ...);
KeyBindingHelper.registerKeyBinding(speedIncrease  = ... GLFW_KEY_O ...);
KeyBindingHelper.registerKeyBinding(speedDecrease  = ... GLFW_KEY_I ...);
```

★ 4 SM 커스텀 키 모두 원본 1.7.10 LWJGL2 키코드 → GLFW 매핑 정확.

---

## §5. 매핑 정합 + 누락 영역

### 5.1 정합 표 (★ 핵심 키 메커니즘 100%)

| 영역 | 원본 라인 | 1.21.1 라인 | 정합 |
|---|---|---|---|
| jumpKey/sneakKey 엣지 | L2380-L2387 | ClientState L826-842 | ✅ 1:1 (Button 패턴 그대로) |
| grab 1-shot 캐시 | L2387 (Button.StartPressed) | ClientState L847 | ✅ 의미 등가 (wasPressed 우회) |
| 차지 점프 | L1857-L1878 | Jumper L411-L422 | ✅ 1:1 |
| 헤드 점프 | L1881-L1899 | Jumper L425-L444 | ✅ 1:1 |
| 벽 점프 더블클릭 | L2863-L2897 | ClientState L531-L539 | ✅ 1:1 |
| 방향 점프 더블클릭 | L1918-L1943 / L2904-L2964 | ClientState L1482-L1524 | ✅ 1:1 |
| R-09 토글 | L2966-L3045 | ClientState L1761-L1840 | ✅ 1:1 |
| jumpPending / jumpAvoided | (Button.StartPressed + jumpField) | sm_jump inject + tickEssential | ✅ 의미 등가 |
| sm_jump HEAD inject | (vanilla jump 처리) | MixinLivingEntityClient L231-L238 | ✅ 새 메커니즘 |

→ **핵심 키 메커니즘 = 100% 1:1 이미 완결**.

### 5.2 ★ 잔존 누락 — 8 채팅 옵션

원본 SmartMovingOptions L87-L95 — 1.21.1 SmartMovingConfig 미이식. 영향: UX 메시지 (게임 시작 / 설정 변경 / 속도 변경 시 채팅 피드백 옵션). 핵심 키 동작 무관 — 우선순위 ↓.

옵션 처리 결정 필요:
- **A. 8 옵션 이식**: SmartMovingConfig 에 8 boolean 필드 + load/save IO + 채팅 메시지 송출 측에서 cfg 체크.
- **B. §7 등록**: 1.21.1 채팅 메시지 송출 부분에서 항상 출력 (옵션 무시) — 영구 근사.
- **C. 일부만 이식**: configChat / speedChat 만 (기본 channel 토글), Init 등 세부 옵션 §7.

---

## §6. 1:1 번역 결론

원본 1.7.10 키 시스템 = **Button 클래스 (4 필드 메커니즘) + 11 Button 인스턴스 + 33 옵션**.

1.21.1 매핑 = **ClientState 키 엣지 필드 10 + grab 캐시 + R-09 / IMPL-03 / wallJumpCount / jumpPending 본체 모두 1:1 이식 + 25 옵션 이식**.

**핵심 키 메커니즘 100% 1:1 완결**. 잔존 = **8 채팅 옵션 (UX 우선순위 ↓)**.

→ #4 = **사실상 이미 완결**. 진단 작업으로 8 채팅 옵션 처리 결정 + 문서 갱신만 필요.
