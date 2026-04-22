# SmartMoving 1:1 번역 완결 마스터 가이드

> **목적**: 이 파일 + `PORTING_COMPLETION_GUIDE.md`(애니메이션 R-09~R-13) 두 파일을 전부  
> 체크하면 포팅이 완전해진다.  
> 모든 항목은 2026-04-22 세션에서 코드를 직접 A-Z 읽어 발견한 버그·누락이다.  
> 추정 없음. 각 항목에 원본 코드 근거 + 현재 코드 상태 + 정확한 수정 명세를 명시한다.

---

## ══════════════════════════════════════════
## 황금 규칙 (GOLDEN RULES) — 이것만 지키면 같은 버그가 안 난다
## ══════════════════════════════════════════

### G-01. 상태 필드는 매 틱 리셋 후 재평가한다

**위반 사례**: `isClimbing`이 사다리에서 벗어나도 계속 `true`로 남아 있음.

**원칙**:
```
원본: updateEntityActionState() 시작 시 → isClimbing = false, isWallJumping = false 등 전체 리셋
      → 해당 틱에서 조건이 맞을 때만 다시 true 세팅
```

**규칙**: SM이 특정 틱에 활성화 조건을 판단하는 모든 상태 필드는,  
그 판단 직전에 반드시 `false`(또는 기본값)로 초기화해야 한다.  
초기화 없이 설정만 하면 탈출 조건이 없는 한 영원히 유지된다.

---

### G-02. 상태 진입 로직과 해제 로직은 항상 쌍으로 구현한다

**위반 사례**: `isCrawling`은 선언만 있고 `true`로 설정하는 코드가 없음.

**규칙**: 상태 필드를 선언하면 반드시 세 가지를 모두 구현해야 한다:
```
1. 진입 조건 → true 설정
2. 유지 조건 → true 유지 (매 틱 재평가 or 명시적 유지)
3. 해제 조건 → false 설정
```

**검증 명령**:
```bash
grep -rn "isCrawling\s*=\s*true"   src/   # 진입 확인
grep -rn "isCrawling\s*=\s*false"  src/   # 해제 확인
grep -rn "sm\.isCrawling"          src/   # 소비 확인
```

---

### G-03. 방향 반복 루프에서 방향 판단은 루프 변수(dir)를 사용한다 — playerFacing 금지

**위반 사례**: `getOnLadderOrVine()` 사다리 판정에서 4방향 탐색 중  
`ladderFacing.getOpposite() == playerFacing` 사용 → 플레이어 정면 방향만 감지됨.

**원칙**:
```java
// 금지 패턴 — playerFacing는 루프와 무관
for (Direction dir : Direction.Type.HORIZONTAL) {
    if (ladderFacing.getOpposite() == playerFacing) { ... }  // ← 버그
}

// 올바른 패턴 — 탐색 방향 dir을 사용
for (Direction dir : Direction.Type.HORIZONTAL) {
    if (ladderFacing.getOpposite() == dir) { ... }  // ← 정확
}
```

---

### G-04. 키 바인딩 등록 ≠ 처리. wasPressed()/isPressed() 호출 위치를 grep으로 확인한다

**위반 사례**: `SmartMovingKeys.speedIncrease`, `speedDecrease` 등록됨.  
그러나 `tickEssential()` 어디에도 `wasPressed()` 없음 → 키를 눌러도 아무것도 안 됨.

**규칙**: 키 바인딩을 추가할 때 반드시 동시에 처리 코드를 작성한다.  
```bash
grep -rn "speedIncrease\|speedDecrease" src/   # 키 등록 위치와 처리 위치 모두 나와야 함
```

---

### G-05. 원본 메서드의 모든 분기(if-else, switch)를 구현한다

**위반 사례**: `tryJump()`에서 `LEFT(7)/RIGHT(8)/BACK(9)` 타입 분기가 없음.  
`angleJumpType`이 계산되지만 방향 속도 적용 코드가 없어서 실제 방향 점프가 안 됨.

**규칙**: 원본 코드에 N개 분기가 있으면 현재 코드도 N개 분기를 처리해야 한다.  
분기 수가 다르면 미구현이다.

---

### G-06. Javadoc에 "원본 설명 있음" ≠ 구현 완료

**위반 사례**: `handleCeilingClimbing()` Javadoc에 `motionY = HOLD_MOTION` 설명이 있지만  
입력 없을 때 `setVelocity` 자체가 호출되지 않아 motionY가 설정되지 않음.

**규칙**: Javadoc의 `원본:` 설명이 있는 메서드는 반드시 코드 레벨에서  
그 동작이 수행되는지 직접 검증한다.

---

### G-07. 원본 조건식 밖에 있던 코드는 조건식 밖에 있어야 한다

**위반 사례**: 원본에서 `motionY = value`는 항상 실행됨.  
현재 코드에서는 `if (distSq > 0.0001F) { setVelocity(x, value, z) }` 안에 있음 →  
입력 없으면 motionY가 설정되지 않음.

**규칙**: 원본 코드의 실행 조건을 보존한다.  
조건 밖에 있던 코드는 포팅 후에도 조건 밖에 있어야 한다.

---

### G-08. 새 기능 구현 시 의무 5단계 체크

```
[ ] 1. research 파일에서 원본 코드를 먼저 읽는다
[ ] 2. 원본 메서드의 호출 경로(어디서 호출되는가)를 확인한다
[ ] 3. 원본 코드의 모든 분기를 목록화한다
[ ] 4. 구현 후: 각 분기마다 대응 코드 라인을 확인한다
[ ] 5. 구현 후: G-01~G-07 규칙 위반이 없는지 검사한다
```

---

## ══════════════════════════════════════════
## PART 1 — 즉시 수정 버그 (코드는 있지만 잘못됨)
## ══════════════════════════════════════════

---

### BUG-01. 사다리 감지 방향 버그 🔴 [1줄 수정]

**파일**: `SmartMovingClimber.java:116`

**원본 로직**: 4방향(`dir`) 탐색 중 현재 블록이 사다리이면,  
"그 방향(dir)에서 플레이어 쪽을 향하는 사다리인지" = `ladderFacing.getOpposite() == dir`

**현재 코드 (버그)**:
```java
if (ladderFacing.getOpposite() == playerFacing) {
```

`playerFacing`은 플레이어가 현재 바라보는 고정된 방향. 탐색 루프의 `dir`과 무관.  
결과: 플레이어가 사다리를 정면으로 바라볼 때만 감지됨.  
측면에서 접근하거나 다른 방향을 보면 사다리 없음으로 판정.

**수정**:
```java
// SmartMovingClimber.java:116
if (ladderFacing.getOpposite() == dir) {
```

**검증**: 사다리 앞에서 A키나 D키를 누른 채 LCTRL+W → 클라이밍이 되면 성공.

---

### BUG-02. 클라이밍 상태 매 틱 미초기화 🔴

**파일**: `MixinLivingEntityClient.java` → `sm_travel_client()`

**원본 로직**: `SmartMovingSelf.updateEntityActionState()` 시작 시 매 틱:
```java
// 원본 (SmartMovingSelf.java, updateEntityActionState() 상단)
isClimbing = false;
isCrawlClimbing = false;
isCeilingClimbing = false;
isClimbJumping = false;
isClimbHolding = false;
isClimbCrawling = false;
isWallJumping = false;  // handleWallJumping()에서 재세팅
```

**현재 코드 (버그)**:  
`sm_travel_client()`에서 `setOnlyShouldClimbSpeed()`가 `sm.isClimbing = true`를 설정하지만,  
클라이밍 표면을 벗어나도 `sm.isClimbing = false`로 초기화하는 코드가 없음.  
`onClimbable = false`이면 `return`만 하고 리셋하지 않음.

**수정**:  
`sm_travel_client()` 내에서 클라이밍 탐색 결과와 상관없이,  
탐색 전에 반드시 아래 리셋을 실행한다:

```java
// MixinLivingEntityClient.sm_travel_client() 내
// "[5-1] 클라이밍 처리" 주석 직전에 삽입

// ── 매 틱 클라이밍 상태 리셋 ──────────────────────────────────
sm.isClimbing        = false;
sm.isCrawlClimbing   = false;
sm.isCeilingClimbing = false;
sm.isClimbJumping    = false;
sm.isClimbHolding    = false;
sm.isClimbCrawling   = false;
// isWallJumping: handleWallJumping()에서 horizontalCollision 조건부 세팅 — 아래 참고
```

**isWallJumping 처리**:  
원본에서 `isWallJumping`은 `continueWallJumping = false`가 될 때까지 유지된다.  
현재는 `handleWallJumping()`에서 `sm.isWallJumping = true`를 세팅하지만 해제 없음.  
`sm_travel_client()` 상단(handleWallJumping 호출 전)에서 `sm.isWallJumping = false`를 리셋한 뒤  
`handleWallJumping()`이 조건 충족 시 다시 세팅하도록 변경.

---

## ══════════════════════════════════════════
## PART 2 — 미구현 기능 (로직 자체가 없음)
## ══════════════════════════════════════════

---

### IMPL-01. 크롤링 진입/해제 로직 전체 🔴

**현재 상태**: `isCrawling` 필드 선언됨. hitbox(`getBaseDimensions`), 포즈(`updatePose`),  
렌더(`getPositionOffset`), 물리(`isInSwimmingPose` 차단) 등 수신측 코드 모두 완성.  
그러나 **`isCrawling = true`를 설정하는 코드가 전혀 없음.**

**원본 진입 조건** (`SmartMovingSelf.java:3047-3054` + `crawl_slide.md` wantCrawl/canCrawl 체인):
```
wantCrawl = sneakKey가 방금 눌렸음(StartPressed) && 현재 서 있는 상태(not isCrawling)
canCrawl  = cfg.crawl
         && !isFlying
         && !isSwimming_sm && !isDiving && !isDipping
         && !isClimbing && !isCrawlClimbing && !isCeilingClimbing
         && !isSliding
         && !isHeadJumping

mustCrawl = 공간 부족으로 일어설 수 없음
          → canChangeIntoPose(STANDING) = false

toCrawling():
    isCrawling = true
    crawlToggled = true
    ignoreNextStopSneakButtonPressed = true  // 스니크 키 홀드 상태에서 진입했을 때 즉시 재해제 방지
```

**원본 해제 조건**:
```
fromCrawling():
    crawlToggled → 다음 sneakKey StartPressed 시 해제
    mustCrawl이 false이고 wantStandUp → isCrawling = false
    단, isSmall 필요한 공간이 있는지 먼저 확인
```

**구현 위치**: `SmartMovingClientState.tickEssential()` 또는 별도 `SmartMovingCrawler.handleCrawling()` 메서드.

**필요한 신규 필드** (`SmartMovingClientState.java`에 추가):
```java
/** 크롤링이 토글로 진입되었는지 여부 (스니크 키 재입력으로 해제 대기). */
public boolean crawlToggled;
/** 다음 스니크 StopPressed 이벤트를 무시하는 플래그. toCrawling() 직후 한 번 무시. */
public boolean ignoreNextStopSneakButtonPressed;
```

**구현 명세**:

```java
// SmartMovingClientState.tickEssential() 내, cfg.enabled 블록 안에 추가

// ── 크롤링 진입/유지/해제 ────────────────────────────────────────
if (cfg.crawl) {
    boolean sneakJustPressed  = SmartMovingKeys.grab.wasPressed()
            // ↑ 원본은 sneakKey.StartPressed이지만 1.21.1에서 sneak의 StartPressed는
            //   별도 추적이 필요. 아래 "스니크 StartPressed 구현" 참고.
            ;
    // [크롤링 중이 아닐 때] 진입 체크
    if (!isCrawling) {
        boolean wantCrawl = sneakJustPressed
                && !isFlying && !isSwimming_sm && !isDiving && !isDipping
                && !isClimbing && !isCrawlClimbing && !isCeilingClimbing
                && !isSliding && !isHeadJumping;
        // mustCrawl: 현재 서 있는 자리에서 일어날 수 없는 공간
        boolean mustCrawl = !player.canChangeIntoPose(EntityPose.STANDING);
        if (mustCrawl || wantCrawl) {
            isCrawling = true;
            crawlToggled = true;
            ignoreNextStopSneakButtonPressed = true;
        }
    } else {
        // [크롤링 중] 유지/해제 체크
        boolean mustCrawl = !player.canChangeIntoPose(EntityPose.STANDING);
        if (mustCrawl) {
            // 공간 부족 — 강제 유지
        } else if (crawlToggled) {
            // 다음 sneakKey StartPressed 시 해제
            if (sneakJustPressed) {
                isCrawling = false;
                crawlToggled = false;
            }
        } else {
            // 스니크 키를 떼면 해제
            if (!player.isSneaking()) {
                isCrawling = false;
            }
        }
    }
}
```

**스니크 StartPressed 구현** (sneakKey는 KeyBinding이므로 직접 StartPressed 없음):
```java
// SmartMovingClientState에 추가
private boolean prevSneakPressed = false;

// tickEssential() 내:
boolean sneakPressed    = mc.options.sneakKey.isPressed();
boolean sneakJustPressed = sneakPressed && !prevSneakPressed;
prevSneakPressed = sneakPressed;
```

**resetState()에 추가**:
```java
isCrawling  = false;
crawlToggled = false;
ignoreNextStopSneakButtonPressed = false;
prevSneakPressed = false;
```

**검증**: 좁은 1블록 높이 터널에서 스니크 키 → 엎드려서 통과 확인.

---

### IMPL-02. 슬라이딩 진입 로직 🟠

**현재 상태**: `SmartMovingSlider.handleSliding()`은 `isSliding`이 true일 때 물리를 처리하고  
종료 조건도 있음. 그러나 **`isSliding = true`로 진입시키는 코드가 없음.**

**원본 진입 조건** (`crawl_slide.md` + `SmartMovingSelf.md`):

① **헤드점프 착지 후 슬라이딩**:
```
toSlidingOrCrawling() (SmartMovingSelf lines 1607-1631):
isHeadJumping && onGround && (isSprinting || isFast)
    → isSliding = true
   else
    → isCrawling = true (공간 부족 크롤 진입)
```

② **스니크+스프린트 직접 슬라이딩**:
```
wantSlide = isSneaking && isSprinting && onGround && !isClimbing && !isHeadJumping
canSlide  = cfg.slide
→ isSliding = true
```

**구현 위치**: 
- ① `SmartMovingJumper.resetHeightOffset()` 내 `sm.isHeadJumping = false` 직전에 슬라이딩 전환 체크.
- ② `SmartMovingClientState.tickEssential()` 내.

**구현 명세**:

```java
// SmartMovingJumper.resetHeightOffset() 내 공간 확보 확인 후:
sm.isHeadJumping = false;
sm.heightOffset  = 0F;
// 헤드점프 착지 → 슬라이딩 or 크롤링 전환
if ((player.isSprinting() || sm.isFast) && cfg.slide) {
    sm.isSliding = true;
} else if (!player.canChangeIntoPose(EntityPose.STANDING)) {
    sm.isCrawling = true;
    sm.crawlToggled = true;
}
player.setPose(EntityPose.STANDING);
player.calculateDimensions();
```

```java
// tickEssential() 내 (크롤링 체크 이후):
if (!sm.isSliding && cfg.slide) {
    boolean wantSlide = player.isSneaking() && player.isSprinting()
            && player.isOnGround() && !sm.isClimbing && !sm.isHeadJumping && !sm.isCrawling;
    if (wantSlide) {
        sm.isSliding = true;
    }
}
```

**resetState()에 추가**:
```java
isSliding = false;
```

---

### IMPL-03. A/S/D 더블클릭 방향 점프 🟠

**현재 상태**:  
① `SmartMovingClientState`에 `leftJumpCount`/`rightJumpCount`/`backJumpCount` 필드 없음.  
② `handleJumping()`에서 `angleJumpType`은 계산되지만 항상 `tryJump(UP, 0F)` 호출.  
③ `tryJump()`에 `LEFT(7)/RIGHT(8)/BACK(9)` 타입 처리 코드 없음.  
④ `getJumpMoving()` 헬퍼가 선언되어 있지만 아무 데서도 호출되지 않음.

**원본 동작** (`PENDING_RESEARCH.md` A-05 확인, `jump.md` R-19 확인):
```
더블클릭 카운터: leftJumpCount, rightJumpCount, backJumpCount
- 매 틱: count > 0이면 count--
- A키 StartPressed: leftJumpCount > 0이면 → 왼쪽 방향 점프 발동; else leftJumpCount = 3
- D키 StartPressed: rightJumpCount 동일
- S키 StartPressed: backJumpCount 동일
- 최솟값 2틱, 기본값 3틱 윈도우 (count=3으로 초기화 후 3틱 내 재입력)

방향 점프 발동:
- angleJumpType = LEFT(6) / RIGHT(2) / BACK(4)  ← angleJumpType 테이블 (jump.md 1-3)
- jumpMotionX/Z = 점프 직전 플레이어 velocity.x / velocity.z (handleJumping 최상단에서 저장)
- tryJump(UP, 0F) → 수직 속도 + getJumpMoving으로 수평 속도 추가
```

**angleJumpType → 실제 각도 역산** (jump.md 1-3 테이블):
```
angleJumpType = ((360 - movementAngle) / 45) % 8
→ movementAngle = (360 - angleJumpType * 45) % 360

타입 2 (우) → movementAngle = 270° → jumpAngle = 270°
타입 4 (후) → movementAngle = 180° → jumpAngle = 180°
타입 6 (좌) → movementAngle = 90°  → jumpAngle = 90°
```

**단계 1: 신규 필드 추가** (`SmartMovingClientState.java`):
```java
// ── 10-1: 더블클릭 방향 점프 카운터 ──────────────────────────────────
/** A키 더블클릭 카운터. > 0이면 유효 윈도우 내. 매 틱 감소. */
public int leftJumpCount;
/** D키 더블클릭 카운터. */
public int rightJumpCount;
/** S키 더블클릭 카운터. */
public int backJumpCount;
/** 점프 직전 저장 velocity.x (각도 점프 수평 속도 계산용). */
public double jumpMotionX;
/** 점프 직전 저장 velocity.z. */
public double jumpMotionZ;
```

**단계 2: 카운터 갱신** (`SmartMovingClientState.tickEssential()` 내, 또는 `handleJumping()` 상단):
```java
// 매 틱 카운터 감소
if (leftJumpCount  > 0) leftJumpCount--;
if (rightJumpCount > 0) rightJumpCount--;
if (backJumpCount  > 0) backJumpCount--;
```

**단계 3: 이동 키 StartPressed 추적** (`SmartMovingClientState`에 prev 필드 추가):
```java
private boolean prevPressLeft  = false;
private boolean prevPressRight = false;
private boolean prevPressBack  = false;

// tickEssential() 내:
MinecraftClient mc = MinecraftClient.getInstance();
boolean pressLeft  = mc.options.leftKey.isPressed();
boolean pressRight = mc.options.rightKey.isPressed();
boolean pressBack  = mc.options.backKey.isPressed();

boolean startLeft  = pressLeft  && !prevPressLeft;
boolean startRight = pressRight && !prevPressRight;
boolean startBack  = pressBack  && !prevPressBack;

prevPressLeft  = pressLeft;
prevPressRight = pressRight;
prevPressBack  = pressBack;

// 더블클릭 처리
if (cfg.angleJumpSide) {
    if (startLeft)  { if (leftJumpCount  > 0) { angleJumpType = 6; } else leftJumpCount  = 3; }
    if (startRight) { if (rightJumpCount > 0) { angleJumpType = 2; } else rightJumpCount = 3; }
}
if (cfg.angleJumpBack) {
    if (startBack)  { if (backJumpCount  > 0) { angleJumpType = 4; } else backJumpCount  = 3; }
}
```

**단계 4: jumpMotionX/Z 저장** (`handleJumping()` 최상단에 추가):
```java
// SmartMovingJumper.handleJumping() 첫 줄:
Vec3d currentVel = player.getVelocity();
sm.jumpMotionX = currentVel.x;
sm.jumpMotionZ = currentVel.z;
```

**단계 5: tryJump()에 각도 점프 수평 속도 추가** (`SmartMovingJumper.java`):
```java
// tryJump() 내, player.setVelocity() 호출 직전에 삽입:

// ── 각도 점프 수평 속도 적용 ─────────────────────────────────────────
// 원본: jump.md 1-4, angleJumpType > 1 && < 7 → isAngleJumping()
// angleJumpType → 방향 각도 = (360 - type * 45) % 360
// jump.md 1-4 원본:
//   jumpX = -sin(jumpAngle / RadiantToAngle)  (RadiantToAngle = 180/PI)
//   jumpZ =  cos(jumpAngle / RadiantToAngle)
//   motionX = getJumpMoving(jumpMotionX, moveX, reset=true,
//               horizontal=cfg.angleJumpHorizontalFactor,
//               horizontalJumpFactor=cfg.angleJumpVerticalFactor)
if (sm.isAngleJumping()) {
    double jumpAngleDeg = (360.0 - sm.angleJumpType * 45.0) % 360.0;
    double jumpAngleRad = Math.toRadians(jumpAngleDeg);
    double jumpX = -Math.sin(jumpAngleRad);
    double jumpZ =  Math.cos(jumpAngleRad);

    // 이동 속도 팩터 (원본: moveX = jumpX * speed, jump.md 확인 필요)
    double speed = cfg.angleJumpHorizontalFactor;
    double moveX = jumpX * speed;
    double moveZ = jumpZ * speed;

    motionX = getJumpMoving(sm.jumpMotionX, moveX, true,
                cfg.angleJumpHorizontalFactor, cfg.angleJumpVerticalFactor);
    motionZ = getJumpMoving(sm.jumpMotionZ, moveZ, true,
                cfg.angleJumpHorizontalFactor, cfg.angleJumpVerticalFactor);
}
```

> **주의**: `tryJump()` 호출 전에 `angleJumpType`이 설정되어 있어야 한다.  
> `handleJumping()`에서 더블클릭으로 `angleJumpType`을 설정하고 `jumpPending = true`가 된 뒤,  
> 다음 틱 `tryJump(UP, 0F)` 호출 시 `sm.isAngleJumping() = true`이면 방향 속도가 추가된다.

**단계 6: resetState()에 추가**:
```java
leftJumpCount  = 0;
rightJumpCount = 0;
backJumpCount  = 0;
jumpMotionX    = 0D;
jumpMotionZ    = 0D;
prevPressLeft  = false;
prevPressRight = false;
prevPressBack  = false;
```

**검증**: 지상에서 A키를 빠르게 두 번 → 왼쪽으로 살짝 점프 이동되면 성공.

---

### IMPL-04. F9 토글 채팅 피드백 🟡

**현재 상태**: `tickEssential()`에서 F9 누름 시 `toggle()`을 호출하지만  
결과를 채팅으로 알리는 코드가 없음.

**원본 동작** (`SmartMovingComm.md` + `en_us.json`):
```
SM ON 시: "smartmoving.message.config.toggle.enable"
SM OFF 시: "smartmoving.message.config.toggle.disable"
```

**수정** (`SmartMovingClientState.tickEssential()`):
```java
if (SmartMovingKeys.configToggle.wasPressed()) {
    if (SmartMovingConfig.Config == SmartMovingConfig.INSTANCE) {
        SmartMovingConfig.INSTANCE.toggle();
        // 채팅 피드백 추가
        String key = SmartMovingConfig.INSTANCE.enabled
                ? "smartmoving.message.config.toggle.enable"
                : "smartmoving.message.config.toggle.disable";
        if (player != null) player.sendMessage(Text.translatable(key));
    } else {
        ClientPlayNetworking.send(new SmartMovingNetwork.ConfigChangePayload());
    }
}
```

**`en_us.json`에 추가**:
```json
"smartmoving.message.config.toggle.enable":  "Smart Moving enabled.",
"smartmoving.message.config.toggle.disable": "Smart Moving disabled."
```

> **참고**: 원본 메시지 문자열은 `SmartMovingComm.md`에서 확인.  
> 현재 `en_us.json`에 해당 키가 없으면 추가 필요.

---

### IMPL-05. 속도 키(O/I) 클라이언트→서버 요청 🟡

**현재 상태**: `SmartMovingKeys.speedIncrease`, `speedDecrease` 등록됨.  
`tickEssential()` 어디에도 `wasPressed()` 처리 없음.  
서버로 `SpeedChangePayload`를 전송하는 클라이언트 코드 없음.

**원본 동작** (`SmartMovingComm.md` C-18):
```
O키 누름 → 서버에 SpeedChangePayload(difference=+1) 전송
I키 누름 → 서버에 SpeedChangePayload(difference=-1) 전송
서버: speedUser=true이면 Config.changeSpeed(difference) 적용 후 S2C(difference) 반환
      speedUser=false이면 S2C(0) 반환 → 클라이언트가 "no rights" 메시지 표시
```

**수정** (`SmartMovingClientState.tickEssential()`):
```java
// F9 토글 처리 이후에 추가:
if (SmartMovingKeys.speedIncrease.wasPressed()) {
    ClientPlayNetworking.send(new SmartMovingNetwork.SpeedChangePayload(+1));
}
if (SmartMovingKeys.speedDecrease.wasPressed()) {
    ClientPlayNetworking.send(new SmartMovingNetwork.SpeedChangePayload(-1));
}
```

> **주의**: `SpeedChangePayload` 클라이언트→서버 방향 생성자가 있는지 확인.  
> `SmartMovingNetwork.java`에서 `C2S` 패킷 등록 여부를 grep으로 확인한다.

---

### IMPL-06. 비행 물리 — 바라보는 방향(pitch) 3D 이동 🟠

**현재 상태**:  
비행 체 tilt 애니메이션은 `MixinPlayerEntityRenderer.sm_setupTransforms()`에 구현됨.  
비행 억제(`getOffGroundSpeed = 0.05F`)도 `MixinPlayerEntityClient`에 구현됨.  
그러나 비행 중 **pitch 방향(위아래 조준각)으로 전진하는 SM 물리**가 없음.  
현재는 vanilla 비행 물리(수평 XZ 이동만)가 그대로 적용됨.

**원본 동작** (`SmartMovingSelf.md`, travel() 비행 분기):
```
isFlying이고 SM fly 활성화 시:
    forward = movementForward (입력값, -1~1)
    strafe  = movementSideways
    pitch   = rotationPitch (시야각, 위=-90°, 수평=0°, 아래=+90°)

    // 3D 이동 벡터 분해
    horizontalFactor = cos(pitch * π/180)
    verticalFactor   = -sin(pitch * π/180)

    motionX -= sin(yaw) * forward * flySpeed * horizontalFactor
    motionX -= cos(yaw) * strafe  * flySpeed
    motionY += forward * flySpeed * verticalFactor
    motionZ += cos(yaw) * forward * flySpeed * horizontalFactor
    motionZ -= sin(yaw) * strafe  * flySpeed

    // 감쇠
    motionX *= 0.91F
    motionY *= 0.91F
    motionZ *= 0.91F
```

**주의**: 원본 정확한 공식은 `SmartMovingSelf.md` travel() 비행 분기를 직접 읽어 확인해야 함.  
위는 연구 파일 기반 근사치다. 구현 전 반드시 `docs/research/original/smartmoving/playerapi/SmartMovingSelf.md`에서  
`travel()` 또는 `beforeMoveEntity()` 비행 분기를 먼저 읽고 확인한다.

**구현 위치**: `MixinLivingEntityClient.sm_travel_client()` 내,  
클라이밍 처리 전에 비행 처리 분기 추가. SM이 처리하면 `ci.cancel()`.

**구현 시작 전 필수 리서치**:
```bash
# SmartMovingSelf.md에서 비행 관련 travel() 코드 확인
grep -n "isFlying\|flying\|motionY\|flySpeed" \
  docs/research/original/smartmoving/playerapi/SmartMovingSelf.md | head -40
```

---

## ══════════════════════════════════════════
## PART 3 — 애니메이션 버그 (기존 PORTING_COMPLETION_GUIDE.md 보완)
## ══════════════════════════════════════════

아래 항목들은 `PORTING_COMPLETION_GUIDE.md`의 R-09~R-13 항목이 해결하지 못한 추가 문제다.

---

### ANIM-01. isFlying 애니메이션 — head.pitch 보정 누락 🟡

**파일**: `MixinPlayerEntityModelClient.java`

**원본** (`jump.md` 2-1 + `animation_system.md` R-22):
```
bipedOuter.rotateAngleX = (Quarter - verticalAngle) * walkFactor
bipedHead.rotateAngleX  = -bipedOuter.rotateAngleX / 2   ← 보정
```

**현재 코드**: `sm_setupTransforms()`에서 `matrices.multiply(POSITIVE_X.rotation(theta))`를 적용하지만  
`head.pitch = -theta / 2` 보정이 없음.

**수정** (`MixinPlayerEntityModelClient.java`의 `sm_animateFlying()` 내):
```java
// 기존: arm/leg 애니메이션 코드 이후
// 추가:
if (sm.isFlying) {
    float theta = ((float)Math.PI/2f - sm.stats.currentVerticalAngle)
                * Math.min(1f, Math.max(0f, sm.stats.currentSpeed));
    head.pitch = -theta / 2f;
}
```

---

### ANIM-02. isFlying 중 가만히 있을 때 팔/다리 각도 초기화 누락 🟡

**원본**: 비행 중 이동이 없으면(`walkFactor ≈ 0`) 팔/다리는 기본 자세로 수렴.  
**현재**: `sm_animateFlying()`에서 `walkFactor=0`이어도 팔/다리 각도를 0으로 명시 설정하지 않음.

**수정**: `sm_animateFlying()` 내 분기:
```java
if (walkFactor < 0.01f) {
    // 정지 비행 — 팔/다리 기본 자세
    rightArm.pitch = -0.2f;
    leftArm.pitch  = -0.2f;
    rightArm.yaw   = 0f;
    leftArm.yaw    = 0f;
    rightLeg.pitch = 0f;
    leftLeg.pitch  = 0f;
} else {
    // 이동 비행 — 기존 공식
    ...
}
```

---

## ══════════════════════════════════════════
## PART 4 — 구현 순서 (의존성 고려)
## ══════════════════════════════════════════

```
즉시 수정 (의존성 없음):
  BUG-01  사다리 감지 1줄 수정 → SmartMovingClimber.java:116
  BUG-02  클라이밍 매 틱 리셋 → MixinLivingEntityClient.sm_travel_client()
  IMPL-04 F9 토글 채팅 피드백 → SmartMovingClientState.tickEssential()
  IMPL-05 속도 키 처리 → SmartMovingClientState.tickEssential()

기반 작업 (다른 기능의 전제):
  IMPL-01 크롤링 진입/해제 → SmartMovingClientState.tickEssential()
    ↓ 의존
  IMPL-02 슬라이딩 진입 → SmartMovingJumper + tickEssential()

독립 구현:
  IMPL-03 더블클릭 방향 점프 → SmartMovingClientState + SmartMovingJumper
  ANIM-01 isFlying head.pitch 보정
  ANIM-02 isFlying 정지 자세

연구 후 구현:
  IMPL-06 비행 물리 → SmartMovingSelf.md travel() 비행 분기 먼저 읽기
```

---

## ══════════════════════════════════════════
## PART 5 — 완료 검증 체크리스트
## ══════════════════════════════════════════

### 5-1. 코드 존재 검증 (grep)

```bash
# BUG-01: 사다리 방향 수정 확인
grep -n "ladderFacing.getOpposite() == dir" \
  src/client/java/choco/ratel/smartmoving/client/SmartMovingClimber.java

# BUG-02: 매 틱 리셋 확인
grep -n "isClimbing\s*=\s*false" \
  src/client/java/choco/ratel/smartmoving/mixin/client/MixinLivingEntityClient.java

# IMPL-01: 크롤링 진입 확인
grep -rn "isCrawling\s*=\s*true" src/
# 결과가 1개 이상 있어야 함

# IMPL-02: 슬라이딩 진입 확인
grep -rn "isSliding\s*=\s*true" src/
# 결과가 1개 이상 있어야 함

# IMPL-03: 더블클릭 카운터 확인
grep -rn "leftJumpCount\|rightJumpCount\|backJumpCount" src/

# IMPL-03: getJumpMoving 호출 확인
grep -rn "getJumpMoving(" \
  src/client/java/choco/ratel/smartmoving/client/SmartMovingJumper.java
# 결과가 0이면 미구현

# IMPL-04: F9 채팅 확인
grep -n "toggle.enable\|toggle.disable" \
  src/main/resources/assets/smartmoving/lang/en_us.json

# IMPL-05: 속도 키 처리 확인
grep -n "speedIncrease.wasPressed\|speedDecrease.wasPressed" \
  src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java
```

### 5-2. 인게임 동작 검증

| 번호 | 테스트 | 성공 조건 |
|------|-------|---------|
| T-01 | 사다리에 측면 접근 후 LCTRL+W | 클라이밍 됨 |
| T-02 | 사다리 클라이밍 후 공중으로 이동 | `isClimbing = false`로 전환됨 |
| T-03 | 1블록 높이 공간에서 Shift키 | 엎드리기(크롤링) 진입 |
| T-04 | 크롤링 중 Shift 재입력 | 공간 있으면 일어섬 |
| T-05 | 스프린트 중 헤드점프 착지 | 슬라이딩 전환 |
| T-06 | 지상에서 A키 빠르게 두 번 | 왼쪽으로 살짝 점프 이동 |
| T-07 | F9 키 | 채팅에 "Smart Moving enabled/disabled" |
| T-08 | O키 / I키 | 속도 변경 또는 "no rights" 채팅 |
| T-09 | 크리에이티브 비행 중 아래 조준+W | 아래 방향으로 전진 |
| T-10 | 비행 중 정지 | 팔/다리 기본 자세 유지 |

### 5-3. 컴파일 검증

```bash
./gradlew compileJava compileClientJava 2>&1 | grep -i "error\|warning"
```

---

## ══════════════════════════════════════════
## PART 6 — 항목별 완료 트래킹
## ══════════════════════════════════════════

> 완료 즉시 `[ ]` → `[x]`

```
[ ] BUG-01  사다리 감지 방향 수정 (SmartMovingClimber.java:116)
[ ] BUG-02  클라이밍 매 틱 리셋 (MixinLivingEntityClient)
[ ] IMPL-01 크롤링 진입/해제 전체
[ ] IMPL-02 슬라이딩 진입 (헤드점프 착지 + 스프린트+스니크)
[ ] IMPL-03 더블클릭 방향 점프 (카운터 + tryJump 방향 속도)
[ ] IMPL-04 F9 토글 채팅 피드백
[ ] IMPL-05 속도 키 클라이언트 처리
[ ] IMPL-06 비행 물리 (리서치 선행 후)
[ ] ANIM-01 isFlying head.pitch 보정
[ ] ANIM-02 isFlying 정지 자세 초기화
```

---

*작성: 2026-04-22 세션 코드 전수 직접 독해 결과*  
*이 파일과 `PORTING_COMPLETION_GUIDE.md`(R-09~R-13)를 합산하면 1:1 포팅 완결.*
