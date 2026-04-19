# 입력 처리 시스템 리서치

> 원본 소스: `net.smart.moving.Button`, `net.smart.moving.config.SmartMovingOptions`, `SmartMovingSelf`

---

## 관련 클래스/파일

| 클래스 | 역할 |
|--------|------|
| `Button.java` | 키 상태 추적 래퍼 (엣지 감지) |
| `SmartMovingOptions.java` | 키 바인딩 정의 및 등록 |
| `SmartMovingSelf.java` | 입력 폴링 + 토글 상태 관리 |
| `SmartMovingContext.java` | `ClientRegistry.registerKeyBinding()` 호출 |

---

## 동작 원리

```
매 틱 updateEntityActionState() 시작 시
  ↓
Button.update() x8 호출 (forward/left/right/back/jump/sprint/sneak/grab)
  ↓
Pressed / WasPressed / StartPressed / StopPressed 갱신
  ↓
SmartMovingSelf에서 플래그 조합으로 행동 결정
  ↓
토글 행동은 crawlToggled / sneakToggled로 별도 추적
```

---

## A. Button 클래스 구조

```java
public class Button extends SmartMovingContext {
    public boolean Pressed;       // 현재 프레임 눌림 상태
    public boolean WasPressed;    // 이전 프레임 눌림 상태
    public boolean StartPressed;  // 눌림 시작 (딱 1프레임만 true)
    public boolean StopPressed;   // 눌림 종료 (딱 1프레임만 true)

    public void update(boolean pressed) {
        WasPressed = Pressed;
        Pressed = pressed;
        StartPressed = !WasPressed && Pressed;   // false→true 전환
        StopPressed = WasPressed && !Pressed;    // true→false 전환
    }

    private static boolean isKeyDown(int keyCode) {
        if (keyCode >= 0)
            return Keyboard.isKeyDown(keyCode);        // 키보드
        return Mouse.isButtonDown(keyCode + 100);      // 마우스 (음수 코드)
    }
}
```

**핵심:** Button 클래스 자체는 토글 기능 없음. 토글은 `SmartMovingSelf`에서 `crawlToggled`, `sneakToggled` 필드로 별도 관리.

**포커스 체크:**
- `inGameHasFocus` — 게임 포커스 없으면 입력 무시
- `currentScreen.allowUserInput` — UI 열려있어도 허용된 경우 입력 받음

---

## B. 등록된 키 바인딩 4개

| 키 바인딩 | 필드명 | 기본값 | 키코드 |
|-----------|--------|--------|--------|
| 잡기/올라가기 | `keyBindGrab` | Left Control | 29 |
| 설정 토글 | `keyBindConfigToggle` | F9 | 67 |
| 속도 증가 | `keyBindSpeedIncrease` | O | 24 |
| 속도 감소 | `keyBindSpeedDecrease` | I | 23 |

이동 키(W/A/S/D), 점프, 스프린트, 스니크는 바닐라 키 바인딩 그대로 사용.

**등록 방식 (Forge):**
```java
ClientRegistry.registerKeyBinding(Options.keyBindGrab);
```

---

## C. 입력 폴링 위치 및 주기

**주기:** 매 클라이언트 틱 (updateEntityActionState 시작 시)

**폴링 코드:**
```java
forwardButton.update(gameSettings.keyBindForward);
leftButton.update(gameSettings.keyBindLeft);
rightButton.update(gameSettings.keyBindRight);
backButton.update(gameSettings.keyBindBack);
jumpButton.update(esp.movementInput.jump);
sprintButton.update(gameSettings.keyBindSprint);
sneakButton.update(esp.movementInput.sneak);
grabButton.update(Options.keyBindGrab);   // SmartMoving 전용
```

**Grab 버튼이 제어하는 행동:**
- 클라이밍 활성화
- 크롤-클라이밍
- 헤드 점프 (충전)
- 천장 클라이밍
- 슬라이딩 메커닉

---

## D. Hold vs Toggle 정리

**Hold (버튼 누르는 동안만 동작):**
- 이동 방향 (WASD)
- 점프
- 스프린트
- Grab/Climb (기본)
- 속도 증감

**Toggle (설정으로 변경 가능):**
- **스니크**: `Options.isSneakToggleEnabled()` → `sneakToggled` 필드로 유지
- **크롤**: `Options.isCrawlToggleEnabled()` → `crawlToggled` 필드로 유지

**토글 구현 예시 (스니크):**
```java
if (isSneakToggleEnabled) {
    if (willStartSneak) sneakToggled = true;
    if (willStopSneak)  sneakToggled = false;
}

boolean sneakContinueInput = Options.isSneakToggleEnabled()
    ? sneakToggled || sneakButton.StartPressed
    : sneakButton.Pressed;
```

---

## E. 크롤 토글 메커니즘 상세

```java
// 1. 크롤 상태 진입 감지
if (isCrawling && !wasCrawling)       willStartCrawl = true;
if (isClimbCrawling && !wasClimbCrawling) willStartCrawl = true;

// 2. 토글 플래그 설정
if (willStartCrawl) {
    crawlToggled = true;
    ignoreNextStopSneakButtonPressed = sneakButton.Pressed;
    // 첫 번째 스니크 해제는 무시 (실수 방지)
}
if (willStopCrawl) crawlToggled = false;

// 3. 크롤 유지 입력으로 사용
boolean inputContinueCrawl = Options.isCrawlToggleEnabled()
    ? crawlToggled
    : (sneakButton.Pressed || (!Config.isFreeClimbingEnabled() && grabButton.Pressed));
```

**특이사항:** `ignoreNextStopSneakButtonPressed` — 크롤 진입 순간 스니크가 눌려있으면, 그 다음 스니크 해제는 무시. 실수로 즉시 크롤이 해제되는 것 방지.

---

## 1.21.1 마이그레이션 포인트

| 원본 (Forge 1.7.10) | Fabric 1.21.1 대응 |
|---------------------|-------------------|
| `ClientRegistry.registerKeyBinding()` | `KeyBindingHelper.registerKeyBinding()` (Fabric API) |
| `Button` 클래스 | 동일 구조로 직접 구현 (변경 없음) |
| `Keyboard.isKeyDown()` (LWJGL 2) | `InputUtil.isKeyPressed()` (LWJGL 3) |
| `esp.movementInput.jump` | `Input.jumping` (ClientPlayerEntity) |
| `gameSettings.keyBindForward` | `MinecraftClient.getInstance().options.forwardKey` |
| 입력 폴링 위치 | `ClientTickEvents.END_CLIENT_TICK` 또는 `LivingEntity.tickMovement()` Mixin |

---

## 미확인 / 추가 조사 필요

- [ ] Fabric에서 `Input.jumping`이 정확히 어디서 노출되는지 확인
- [ ] `grabButton` 기본값을 Left Ctrl 이외로 바꿀 경우 UX 고려
- [ ] 토글 상태 (crawlToggled)를 `AttachmentType`으로 관리할지, 별도 필드로 관리할지 결정
