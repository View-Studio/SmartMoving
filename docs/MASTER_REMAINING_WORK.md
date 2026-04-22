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

### BUG-01. 사다리 감지 방향 버그 🔴 [1줄 수정] ✅ 완료 (2026-04-22)

**선행 읽기 파일**:
- [x] `src/client/java/choco/ratel/smartmoving/client/SmartMovingClimber.java` — getOnLadderOrVine() 루프 구조 전체
- [x] `docs/research/mapping/climbing.md` — 사다리 방향 판정 매핑 확인
- [x] `docs/research/original/smartmoving/moving/SmartMovingBase.md` — 원본 getOnLadderOrVine() 판정 로직

**작업 단계 체크리스트**:
- [x] SmartMovingClimber.java:116 에서 `playerFacing` → `dir` 로 1줄 수정
- [x] Javadoc(69번 줄) 설명 `== playerFacing` → `== dir` 로 수정
- [x] 인라인 주석(114~115번 줄) 올바른 예시로 교체
- [x] PART 6 트래킹 [x] 체크

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

**선행 읽기 파일**:
- [ ] `src/client/java/choco/ratel/smartmoving/mixin/client/MixinLivingEntityClient.java` — sm_travel_client() 전체 구조, "[5-1] 클라이밍 처리" 주석 위치 파악
- [ ] `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` — 상태 필드 전체 목록(isClimbing, isCrawlClimbing 등) 및 위치
- [ ] `src/client/java/choco/ratel/smartmoving/client/SmartMovingClimber.java` — handleClimbing(), handleCeilingClimbing() 내 isClimbing 설정 위치
- [ ] `docs/research/original/smartmoving/playerapi/SmartMovingSelf.md` — updateEntityActionState() 상단 전체 리셋 코드 확인
- [ ] `docs/research/mapping/climbing.md` — 클라이밍 상태 매핑 전체

**작업 단계 체크리스트**:
- [ ] MixinLivingEntityClient.java에서 "[5-1] 클라이밍 처리" 주석 바로 앞 위치 확인
- [ ] 6개 클라이밍 상태 리셋 코드 삽입 (isClimbing, isCrawlClimbing, isCeilingClimbing, isClimbJumping, isClimbHolding, isClimbCrawling)
- [ ] handleWallJumping() 호출 직전에 `sm.isWallJumping = false` 리셋 삽입
- [ ] `./gradlew compileJava compileClientJava` 컴파일 통과 확인
- [ ] `grep -n "isClimbing\s*=\s*false" src/.../MixinLivingEntityClient.java` 로 존재 확인
- [ ] PART 6 트래킹 [x] 체크

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

**선행 읽기 파일**:
- [ ] `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` — 기존 필드 전체 목록, tickEssential() 전체 흐름, resetState() 위치
- [ ] `src/client/java/choco/ratel/smartmoving/client/input/SmartMovingKeys.java` — grab 키 등록 방식 확인
- [ ] `docs/research/original/smartmoving/playerapi/SmartMovingSelf.md` — toCrawling(), fromCrawling(), wantCrawl/canCrawl 조건 코드 (updateEntityActionState 내 크롤링 진입 분기)
- [ ] `docs/research/mapping/crawl_slide.md` — wantCrawl/canCrawl/mustCrawl 전체 매핑, sneakToggled 플래그 설명
- [ ] `docs/research/vanilla/EntityPose_system.md` — canChangeIntoPose(STANDING) API 동작 방식
- [ ] `docs/MASTER_REMAINING_WORK.md` PART 7 7-2-A 절 — 원본 진입/해제 조건 재확인

**작업 단계 체크리스트**:
- [ ] SmartMovingClientState.java에 `crawlToggled` 필드 추가
- [ ] SmartMovingClientState.java에 `ignoreNextStopSneakButtonPressed` 필드 추가
- [ ] tickEssential() 내 크롤링 진입 조건 구현 (grabButton.wasPressed + sneakHeld + onGround + canCrawl 체인)
- [ ] tickEssential() 내 크롤링 유지/해제 조건 구현 (mustCrawl 강제유지 / crawlToggled 토글해제 / 스니크해제)
- [ ] resetState()에 crawlToggled, ignoreNextStopSneakButtonPressed 초기화 추가
- [ ] `./gradlew compileJava compileClientJava` 컴파일 통과 확인
- [ ] `grep -rn "isCrawling\s*=\s*true" src/` 결과 1개 이상 확인
- [ ] T-03 인게임 테스트: 1블록 높이 + Shift + LCTRL → 크롤링 진입
- [ ] T-04 인게임 테스트: 크롤링 중 LCTRL 재입력 → 공간 있으면 일어섬
- [ ] PART 6 트래킹 [x] 체크

**현재 상태**: `isCrawling` 필드 선언됨. hitbox(`getBaseDimensions`), 포즈(`updatePose`),  
렌더(`getPositionOffset`), 물리(`isInSwimmingPose` 차단) 등 수신측 코드 모두 완성.  
그러나 **`isCrawling = true`를 설정하는 코드가 전혀 없음.**

**원본 진입 조건** (`SmartMovingSelf.java:3047-3054` + `crawl_slide.md` wantCrawl/canCrawl 체인):
```
⚠️ 주의: wantCrawl의 트리거는 sneakKey(Shift)가 아닌 grabButton(LCTRL)이다.
         정확한 원본: grabButton.StartPressed && (sneakToggled || sneakButton.Pressed) && sp.onGround

wantCrawl = grabButton(LCTRL)이 방금 눌렸음(StartPressed)
          && (sneakToggled || sneakKey(Shift)가 현재 눌려있음)
          && sp.onGround
          && 현재 크롤링 중이 아님(not isCrawling)
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
            // ↑ 원본은 grabButton.StartPressed (LCTRL 방금 눌림).
            //   Shift(sneakKey)는 grab과 동시에 눌려있어야 하는 조건이지, 트리거가 아님.
            //   grab.wasPressed() = 1.21.1의 StartPressed 등가.
            ;
    // [크롤링 중이 아닐 때] 진입 체크
    if (!isCrawling) {
        boolean sneakHeld = player.isSneaking() || sneakToggled; // sneakButton.Pressed || sneakToggled
        boolean wantCrawl = sneakJustPressed   // grabButton.StartPressed (LCTRL 방금 누름)
                && sneakHeld               // Shift도 눌려 있어야 함
                && player.isOnGround()
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

**grab(LCTRL) wasPressed() 사용** (1.21.1 표준 KeyBinding API):
```java
// SmartMovingClientState.tickEssential() 내:
// SmartMovingKeys.grab은 KeyBinding — wasPressed()가 StartPressed 등가 (1틱 1회 소비)
boolean grabJustPressed = SmartMovingKeys.grab.wasPressed();
boolean sneakHeld       = player.isSneaking() || sneakToggled;
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

**선행 읽기 파일**:
- [ ] `src/client/java/choco/ratel/smartmoving/client/SmartMovingJumper.java` — resetHeightOffset() 전체, isHeadJumping = false 설정 위치
- [ ] `src/client/java/choco/ratel/smartmoving/client/SmartMovingSlider.java` — handleSliding() 전체 (해제 조건, isSliding 관련 필드)
- [ ] `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` — tickEssential() 내 크롤링 처리 이후 위치 (IMPL-01 완료 후 작업)
- [ ] `docs/research/original/smartmoving/playerapi/SmartMovingSelf.md` — toSlidingOrCrawling() lines 1607-1631, 스프린트+스니크 직접 슬라이딩 진입 분기
- [ ] `docs/research/mapping/crawl_slide.md` — 슬라이딩 진입 조건 전체, isFast 플래그 설명

**⚠️ 의존성**: IMPL-01(크롤링) 완료 후 작업 권장 — 헤드점프 착지 시 공간 부족이면 isCrawling으로 전환되므로 crawlToggled 필드가 먼저 존재해야 함.

**작업 단계 체크리스트**:
- [ ] SmartMovingJumper.resetHeightOffset() 내 `sm.isHeadJumping = false` 직전 위치 확인
- [ ] 헤드점프 착지 → 슬라이딩 or 크롤링 전환 코드 삽입 (`isSprinting || isFast` → isSliding, 공간부족 → isCrawling)
- [ ] tickEssential()에 스프린트+스니크 직접 슬라이딩 진입 조건 추가 (크롤링 체크 이후)
- [ ] resetState()에 `isSliding = false` 존재 확인 (없으면 추가)
- [ ] `./gradlew compileJava compileClientJava` 컴파일 통과 확인
- [ ] `grep -rn "isSliding\s*=\s*true" src/` 결과 1개 이상 확인
- [ ] T-05 인게임 테스트: 스프린트 중 헤드점프 착지 → 슬라이딩 전환
- [ ] PART 6 트래킹 [x] 체크

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

**선행 읽기 파일**:
- [ ] `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` — 기존 필드 목록, tickEssential() 전체, angleJumpType 필드 위치, resetState()
- [ ] `src/client/java/choco/ratel/smartmoving/client/SmartMovingJumper.java` — handleJumping() 전체, tryJump() 전체, getJumpMoving() 선언 및 공식
- [ ] `docs/research/mapping/jump.md` — angleJumpType 테이블(1-3), getJumpMoving 공식(1-4), jumpMotionX/Z 설명 전체 정독
- [ ] `docs/research/original/smartmoving/playerapi/SmartMovingSelf.md` — sideJump 더블클릭 카운터 코드 lines 2898-2961, handleJumping 최상단 velocity 저장 코드
- [ ] `docs/research/original/smartmoving/moving/Button.md` — StartPressed(방금 눌림) 원본 구현 — 1.21.1 prev 추적 방식과 비교

**작업 단계 체크리스트**:
- [ ] SmartMovingClientState.java에 `leftJumpCount`, `rightJumpCount`, `backJumpCount` 필드 추가
- [ ] SmartMovingClientState.java에 `jumpMotionX`, `jumpMotionZ` 필드 추가
- [ ] SmartMovingClientState.java에 `prevPressLeft`, `prevPressRight`, `prevPressBack` private 필드 추가
- [ ] tickEssential()에 매 틱 카운터 감소 코드 추가 (leftJumpCount--, rightJumpCount--, backJumpCount--)
- [ ] tickEssential()에 이동 키 rising-edge 추적 코드 추가 (startLeft/Right/Back)
- [ ] tickEssential()에 더블클릭 감지 및 angleJumpType 설정 코드 추가 (cfg.angleJumpSide/Back 조건 포함)
- [ ] SmartMovingJumper.handleJumping() 최상단에 `jumpMotionX/Z = currentVel.x/z` 저장 추가
- [ ] SmartMovingJumper.tryJump()에 `sm.isAngleJumping()` 분기 및 방향 수평 속도 적용 코드 추가
- [ ] resetState()에 6개 신규 필드 초기화 추가
- [ ] `./gradlew compileJava compileClientJava` 컴파일 통과 확인
- [ ] `grep -rn "leftJumpCount\|rightJumpCount\|backJumpCount" src/` 결과 확인
- [ ] `grep -rn "getJumpMoving(" src/.../SmartMovingJumper.java` 결과 1개 이상 확인
- [ ] T-06 인게임 테스트: 지상에서 A키 빠르게 두 번 → 왼쪽으로 살짝 점프 이동
- [ ] PART 6 트래킹 [x] 체크

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

### IMPL-04. F9 토글 채팅 피드백 🟡 ✅ 완료 (2026-04-22)

**선행 읽기 파일**:
- [x] `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` — tickEssential() 내 configToggle 처리 위치
- [x] `src/main/resources/assets/smartmoving/lang/en_us.json` — 현재 등록된 메시지 키 목록

**작업 단계 체크리스트**:
- [x] tickEssential()의 configToggle.wasPressed() 블록 내에 채팅 피드백 코드 추가
- [x] en_us.json에 `smartmoving.message.config.client.enabled/disabled` 키 추가
- [x] 컴파일 통과 확인
- [x] PART 6 트래킹 [x] 체크

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

### IMPL-05. 속도 키(O/I) 클라이언트→서버 요청 🟡 ✅ 완료 (2026-04-22)

**선행 읽기 파일**:
- [x] `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` — tickEssential() 내 키 처리 위치
- [x] `src/main/java/choco/ratel/smartmoving/network/SmartMovingNetwork.java` — SpeedChangePayload C2S 방향 생성자 확인

**작업 단계 체크리스트**:
- [x] tickEssential()에 speedIncrease.wasPressed() / speedDecrease.wasPressed() 처리 코드 추가
- [x] SmartMovingClient.java SpeedChange S2C 수신 시 채팅 피드백 추가 (getSpeedPercent() 사용)
- [x] SmartMovingConfig.getSpeedPercent() 헬퍼 메서드 추가
- [x] 컴파일 통과 확인
- [x] PART 6 트래킹 [x] 체크

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

**선행 읽기 파일**:
- [ ] `src/client/java/choco/ratel/smartmoving/mixin/client/MixinLivingEntityClient.java` — sm_travel_client() 전체 흐름, 클라이밍 처리 앞뒤 위치 파악
- [ ] `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityClient.java` — 비행 억제(getOffGroundSpeed) 코드 위치 확인
- [ ] `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` — isFlying 필드, stats 필드 확인
- [ ] `docs/research/original/smartmoving/playerapi/SmartMovingSelf.md` — travel() 또는 beforeMoveEntity() 내 비행 분기 **전체 코드** 정독 (grep: isFlying, flySpeed, motionY)
- [ ] `docs/research/mapping/speed_physics.md` — 비행 물리 속도 팩터 매핑
- [ ] `docs/research/vanilla/LivingEntity_travel.md` — vanilla travel() 비행 처리 구조, ci.cancel() 타이밍

**⚠️ 필수 선행 작업**: 위 파일 읽기 전에 아래 grep으로 원본 코드 범위를 먼저 찾는다:
```bash
grep -n "isFlying\|flySpeed\|horizontalFactor\|verticalFactor" \
  docs/research/original/smartmoving/playerapi/SmartMovingSelf.md | head -30
```

**작업 단계 체크리스트**:
- [ ] SmartMovingSelf.md에서 travel() 비행 분기 원본 코드 찾아 읽기 (정확한 공식 확인)
- [ ] 원본 공식과 이 파일 IMPL-06 명세의 근사치 비교 — 차이 있으면 명세 수정
- [ ] MixinLivingEntityClient.sm_travel_client()에 `isFlying && cfg.fly` 분기 추가 (클라이밍 처리 전)
- [ ] pitch 기반 horizontalFactor = cos(pitch), verticalFactor = -sin(pitch) 구현
- [ ] motionX/Y/Z 3D 이동 벡터 계산 및 setVelocity 적용
- [ ] 감쇠 0.91F 적용 코드 추가
- [ ] SM이 처리한 경우 `ci.cancel()` 또는 적절한 vanilla 처리 차단
- [ ] `./gradlew compileJava compileClientJava` 컴파일 통과 확인
- [ ] T-09 인게임 테스트: 크리에이티브 비행 중 아래 조준+W → 아래 방향으로 전진
- [ ] PART 6 트래킹 [x] 체크

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

**선행 읽기 파일**:
- [ ] `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java` — sm_animateFlying() 전체, 현재 head.pitch 설정 코드 확인
- [ ] `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityRenderer.java` — sm_setupTransforms()에서 theta 계산 방식 확인 (theta 공식이 sm_animateFlying과 일치해야 함)
- [ ] `docs/research/mapping/animation_system.md` — 비행 애니메이션 섹션 (bipedOuter.rotateAngleX, bipedHead.rotateAngleX 보정 공식)
- [ ] `docs/research/original/smartrender/playerapi/SmartMovingRenderPlayerBase.md` — bipedHead.rotateAngleX = -bipedOuter.rotateAngleX / 2 원본 코드 확인

**작업 단계 체크리스트**:
- [ ] MixinPlayerEntityRenderer.sm_setupTransforms()에서 theta 계산 공식 읽기
- [ ] MixinPlayerEntityModelClient.sm_animateFlying() 내 arm/leg 코드 이후에 head.pitch 보정 추가
- [ ] theta 계산이 sm_setupTransforms()와 동일한지 검증 (currentVerticalAngle, currentSpeed 사용)
- [ ] `./gradlew compileJava compileClientJava` 컴파일 통과 확인
- [ ] 인게임 비행 중 고개 기울기가 동체 각도의 절반으로 보정되는지 확인
- [ ] PART 6 트래킹 [x] 체크

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

**선행 읽기 파일**:
- [ ] `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java` — sm_animateFlying() 전체, walkFactor 계산 방식, 현재 arm/leg 설정 코드 구조
- [ ] `docs/research/mapping/animation_system.md` — 비행 정지 자세 기본값, walkFactor=0 시 원본 처리 방식
- [ ] `docs/research/original/smartrender/playerapi/SmartMovingRenderPlayerBase.md` — 비행 정지 상태 팔/다리 각도 원본값 확인

**작업 단계 체크리스트**:
- [ ] sm_animateFlying() 내 walkFactor 계산 위치 확인
- [ ] `walkFactor < 0.01f` 분기 추가 및 정지 자세 팔/다리 각도 명시 설정
- [ ] 이동 비행 분기는 기존 공식 유지 (`else` 블록으로 감싸기)
- [ ] `./gradlew compileJava compileClientJava` 컴파일 통과 확인
- [ ] T-10 인게임 테스트: 비행 중 이동 멈춤 → 팔/다리 기본 자세 유지
- [ ] PART 6 트래킹 [x] 체크

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
| T-03 | 1블록 높이 공간에서 Shift 누른 채 LCTRL | 엎드리기(크롤링) 진입 |
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
[x] BUG-01  사다리 감지 방향 수정 (SmartMovingClimber.java:116)  ← 2026-04-22 완료
[ ] BUG-02  클라이밍 매 틱 리셋 (MixinLivingEntityClient)
[ ] IMPL-01 크롤링 진입/해제 전체
[ ] IMPL-02 슬라이딩 진입 (헤드점프 착지 + 스프린트+스니크)
[ ] IMPL-03 더블클릭 방향 점프 (카운터 + tryJump 방향 속도)
[x] IMPL-04 F9 토글 채팅 피드백  ← 2026-04-22 완료
[x] IMPL-05 속도 키 클라이언트 처리  ← 2026-04-22 완료
[ ] IMPL-06 비행 물리 (리서치 선행 후)
[ ] ANIM-01 isFlying head.pitch 보정
[ ] ANIM-02 isFlying 정지 자세 초기화
```

---

---

## ══════════════════════════════════════════
## PART 7 — 상태별 원본 키 조건 완전 매핑
## ══════════════════════════════════════════

> **목적**: 각 이동 상태의 원본 진입/해제 키 조건을 1:1 대응표로 정리.  
> 구현 전 반드시 이 표를 읽고 조건을 맞춰 구현한다.  
> 출처: `docs/research/original/smartmoving/moving/SmartMovingSelf.md`  
> 출처: `docs/research/mapping/crawl_slide.md`

---

### 7-1. 상태별 키 조건 요약표

| 상태 | 진입 키 | 원본 조건 요약 | 1.21.1 구현 방법 | 현황 |
|------|---------|--------------|-----------------|------|
| 크롤링 (Crawling) | **LCTRL 방금 누름** + Shift 유지 | `grabButton.StartPressed && (sneakToggled \|\| sneakButton.Pressed) && onGround` | `SmartMovingKeys.grab.wasPressed() && player.isSneaking()` | ❌ 미구현 |
| 슬라이딩 (Sliding) | ① 헤드점프 착지 + grabButton 유지 | `toSlidingOrCrawling() → isHeadJumping && onGround → grabButton.Pressed \|\| wasHeadJumping` | SmartMovingJumper 착지 시 전환 | ❌ 미구현 |
| 슬라이딩 (Sliding) | ② Shift + Sprint | `isSneaking && isSprinting && onGround && !isClimbing && !isHeadJumping` | tickEssential() 내 조건 체크 | ❌ 미구현 |
| 다이빙 (Diving) | 자동 (물 깊이) | 수면 오프셋 ≥ 1.9 → `isDiving = true` | SmartMovingSwimmer ✅ 이미 올바름 | ✅ 완료 |
| 수영 (Swimming) | 자동 (물 깊이) | 수면 오프셋 1.4~1.9 → `isSwimming_sm = true` | SmartMovingSwimmer ✅ 이미 올바름 | ✅ 완료 |
| 물살짝잠김 (Dipping) | 자동 (물 깊이) | 수면 오프셋 < 1.4 → `isDipping = true` | SmartMovingSwimmer ✅ 이미 올바름 | ✅ 완료 |
| 클라이밍 (Climbing) | **LCTRL 유지** + 클라이밍 가능 블록 | `grabButton.Pressed && onClimbable` | `SmartMovingKeys.grab.isPressed()` | ✅ 구현됨 (BUG-01 사다리 방향 버그 수정 필요) |
| 천장 클라이밍 | **LCTRL 유지** + 천장 근접 | `grabButton.Pressed && !wantCrawlNotClimb && !isSneaking()` | SmartMovingClimber.handleCeilingClimbing() | ⚠️ 진입 조건 재확인 필요 |
| 헤드점프 (HeadJump) | **LCTRL + Sprint** 중 Space | `grabButton.Pressed && sp.isSprinting()` → 점프 충전 → 발사 | SmartMovingJumper에 구현 여부 확인 필요 | ⚠️ 부분 구현 |
| 방향 점프 (AngleJump) | **A/D/S 더블클릭** | `leftJumpCount/rightJumpCount/backJumpCount` 카운터 시스템 | IMPL-03 참고 | ❌ 미구현 |
| 벽 점프 (WallJump) | **벽 충돌 + 더블클릭** | `horizontalCollision && continueWallJumping` | handleWallJumping() 확인 필요 | ⚠️ 부분 구현 |

---

### 7-2. 상태별 상세 원본 조건

#### 7-2-A. 크롤링 (Crawling)

**원본 소스**: `SmartMovingSelf.java updateEntityActionState()` 내 `toCrawling()` 경로

```
진입 트리거: grabButton.StartPressed  ← LCTRL이 방금 눌린 틱 1회
진입 조건:   (sneakToggled || sneakButton.Pressed)  ← Shift가 켜져 있거나 눌려있음
             && sp.onGround
             && !isCrawling
             && canCrawl (= cfg.crawl && !isFlying && !isSwimming_sm && !isDiving && !isDipping
                           && !isClimbing && !isCrawlClimbing && !isCeilingClimbing
                           && !isSliding && !isHeadJumping)

toCrawling() 실행 시:
    isCrawling = true
    crawlToggled = true
    ignoreNextStopSneakButtonPressed = true

해제 조건:
    mustCrawl = !canChangeIntoPose(STANDING)
    mustCrawl=true  → 강제 유지
    mustCrawl=false && crawlToggled → grabButton.StartPressed 시 해제 (토글 해제)
    mustCrawl=false && !crawlToggled → sneakButton.StopPressed 시 해제
```

**1.21.1 대응**:
```java
// 진입 트리거: SmartMovingKeys.grab.wasPressed() (= StartPressed)
// 진입 조건 Shift: player.isSneaking() || sneakToggled
// mustCrawl: !player.canChangeIntoPose(EntityPose.STANDING)
// 해제 트리거: SmartMovingKeys.grab.wasPressed() (토글 해제)
```

**현재 버그**: IMPL-01 구현 명세의 sneakJustPressed 변수가 `SmartMovingKeys.grab.wasPressed()`로  
이미 올바르게 수정됨(이 파일 내). 그러나 **`sneakHeld` 조건(Shift)이 반드시 함께 체크**되어야 함.

---

#### 7-2-B. 슬라이딩 (Sliding)

**원본 소스**: `SmartMovingSelf.java toSlidingOrCrawling()` (lines 1607-1631)

```
① 헤드점프 착지 경로:
   isHeadJumping && onGround && (isSprinting || sm.isFast)
       → cfg.slide → isSliding = true
       → !cfg.slide → toCrawling() or mustCrawl

② 직접 진입 경로 (스프린트+스니크):
   isSneaking && isSprinting && onGround
   && !isClimbing && !isHeadJumping
   && cfg.slide
       → isSliding = true

해제 조건:
   SliderHandler.handleSliding():
       속도가 최솟값 이하로 감소 → isSliding = false
       또는 스니크 해제 → isSliding = false
```

**1.21.1 대응**:
```java
// ① SmartMovingJumper.resetHeightOffset() 착지 감지 후
// ② tickEssential() 내: player.isSneaking() && player.isSprinting() && player.isOnGround()
```

---

#### 7-2-C. 다이빙/수영/물살짝잠김 (Diving/Swimming/Dipping)

**원본 소스**: `SmartMovingSelf.java handleSwimming()` (lines 229-576)

```
오프셋 계산: offset = getFluidHeightY() + 0.1625D  (눈 위치 기준)

offset < 1.4   → isDipping = true  (발만 잠김)
1.4 ≤ offset < 1.9 → isSwimming_sm = true  (수영)
offset ≥ 1.9   → isDiving = true  (완전 잠김)

→ 키 입력 없음. 완전 자동(물리 오프셋 기반).
```

**1.21.1 상태**: `SmartMovingSwimmer.updateSwimState()`에서  
`OFFSET_SWIMMING=1.4F`, `OFFSET_DIVING=1.9F` 상수로 정확히 구현됨. **수정 불필요.**

---

#### 7-2-D. 클라이밍 (Climbing)

**원본 소스**: `SmartMovingSelf.java handleClimbing()` 내 조건 체인

```
grabButton.Pressed  ← LCTRL 유지
&& onClimbable      ← 사다리, 넝쿨 등 클라이밍 가능 블록
&& !isCrawling      ← 크롤링 중이 아님
→ isClimbing = true
→ 이동 속도는 cfg.climbSpeed 기반

또는 (auto 모드):
cfg.autoLadder || cfg.autoVine → grabButton 없이 onClimbable이면 자동 클라이밍
```

**1.21.1 대응**:
```java
// SmartMovingKeys.grab.isPressed() (= Pressed, 유지 상태)
```

**현재 버그**: BUG-01 (사다리 방향 감지 playerFacing → dir 수정 필요)

---

#### 7-2-E. 천장 클라이밍 (Ceiling Climbing)

**원본 소스**: `SmartMovingSelf.java handleCeilingClimbing()`

```
grabButton.Pressed  ← LCTRL 유지
&& !wantCrawlNotClimb  ← (크롤링 의도 없음 = Shift 안 누름)
&& !isSneaking()
&& 머리 위 천장 블록 존재 (ceilingHeight 조건)
&& cfg.ceilingClimbing
→ isCeilingClimbing = true
```

**1.21.1 대응**:
```java
// SmartMovingKeys.grab.isPressed() && !player.isSneaking()
// 천장 블록 존재 여부: SmartMovingClimber.getOnCeiling()
```

---

#### 7-2-F. 헤드점프 (Head Jump)

**원본 소스**: `SmartMovingSelf.java updateEntityActionState()` 점프 충전 분기

```
점프 충전 조건 (jump charge 시작):
    grabButton.Pressed  ← LCTRL 유지
    && sp.isSprinting()  ← 스프린트 중
    && space 누름 (vanilla jump key)
    && onGround
    && cfg.headJump
    → jumpCharge 카운터 증가 시작

발동 조건:
    jumpCharge >= cfg.jumpChargeCountMinimum
    → 점프 발동: isHeadJumping = true
    → 수직 속도 cfg.headJumpFactor 적용
```

**1.21.1 대응**:
```java
// SmartMovingKeys.grab.isPressed() && player.isSprinting()
// 점프 키: MC 내부 jump 처리에서 감지 (vanilla jump event hooking)
```

---

#### 7-2-G. 방향 점프 (Angle Jump: 왼쪽/오른쪽/뒤)

**원본 소스**: `SmartMovingSelf.java` sideJump 더블클릭 카운터 (lines 2898-2961 참고)

```
카운터 방식:
    매 틱: leftJumpCount > 0 → leftJumpCount--  (윈도우 감소)
    A키 StartPressed:
        leftJumpCount > 0  → angleJumpType = 6 (LEFT) + 점프 발동
        leftJumpCount == 0 → leftJumpCount = 3  (3틱 윈도우 시작)
    D키 StartPressed:
        rightJumpCount > 0 → angleJumpType = 2 (RIGHT) + 점프 발동
        rightJumpCount == 0 → rightJumpCount = 3
    S키 StartPressed:
        backJumpCount > 0  → angleJumpType = 4 (BACK) + 점프 발동
        backJumpCount == 0 → backJumpCount = 3

angleJumpType 테이블: ((360 - movementAngle) / 45) % 8
    타입 2 → 오른쪽(270°), 타입 4 → 뒤(180°), 타입 6 → 왼쪽(90°)

수평 속도 적용:
    jumpX = -sin(angle), jumpZ = cos(angle)
    getJumpMoving(currentVel, jumpX * cfg.angleJumpHorizontalFactor, ...) 사용
```

**1.21.1 대응**: IMPL-03 상세 명세 참고 (이 파일 내).

---

#### 7-2-H. 벽 점프 (Wall Jump)

**원본 소스**: `SmartMovingSelf.java handleWallJumping()`

```
진입 조건:
    horizontalCollision  ← 수평 방향 벽 충돌
    && continueWallJumping  ← 이전 틱 벽점프 유지 플래그 (또는 첫 감지)
    && grabButton.Pressed  ← LCTRL 유지
    && cfg.wallJumping
    → isWallJumping = true
    → 수직 속도 cfg.wallJumpingFactor 적용

해제 조건:
    continueWallJumping = false  (벽에서 멀어짐, 또는 onGround)
    → isWallJumping = false
```

**1.21.1 대응**:
```java
// player.horizontalCollision && SmartMovingKeys.grab.isPressed()
// continueWallJumping: SmartMovingClientState 내 별도 플래그
```

**현재 상태**: `handleWallJumping()` 내 `sm.isWallJumping = true` 설정은 있으나  
BUG-02(매 틱 미초기화)로 인해 `false`로 돌아오지 않는 버그 존재 → BUG-02 수정 시 함께 해결됨.

---

### 7-3. 원본과 현재 구현 불일치 목록

| 항목 | 원본 키 | 현재 구현 | 수정 필요 항목 |
|------|---------|---------|--------------|
| 크롤링 진입 트리거 | LCTRL (grabButton.StartPressed) + Shift 유지 | 미구현 | IMPL-01 구현 시 반드시 grab.wasPressed() 사용 |
| 슬라이딩 진입 | 없음 (isSliding = true 설정 코드 없음) | 미구현 | IMPL-02 구현 필요 |
| 방향 점프 | A/D/S 더블클릭 카운터 | 미구현 | IMPL-03 구현 필요 |
| 클라이밍 방향 감지 | 4방향 탐색 중 dir 기준 | playerFacing 오류 | BUG-01 수정 필요 |
| 클라이밍 상태 리셋 | 매 틱 false 리셋 후 재평가 | 리셋 없음 | BUG-02 수정 필요 |

---

### 7-4. 키 바인딩 원본-현재 대응표

| 원본 키 | 원본 변수 | 1.21.1 KeyBinding | wasPressed() | isPressed() |
|---------|----------|------------------|--------------|-------------|
| LCTRL (341) | `grabButton` | `SmartMovingKeys.grab` | grab.wasPressed() | grab.isPressed() |
| Shift | `sneakButton` | `mc.options.sneakKey` | 수동 prev 추적 필요* | sneakKey.isPressed() |
| F9 (298) | `configToggle` | `SmartMovingKeys.configToggle` | configToggle.wasPressed() | — |
| O (79) | `speedIncreaseButton` | `SmartMovingKeys.speedIncrease` | speedIncrease.wasPressed() | — |
| I (73) | `speedDecreaseButton` | `SmartMovingKeys.speedDecrease` | speedDecrease.wasPressed() | — |
| A | `leftKey` | `mc.options.leftKey` | 수동 prev 추적 필요* | leftKey.isPressed() |
| D | `rightKey` | `mc.options.rightKey` | 수동 prev 추적 필요* | rightKey.isPressed() |
| S | `backKey` | `mc.options.backKey` | 수동 prev 추적 필요* | backKey.isPressed() |

\* MC 이동 키(`sneakKey`, `leftKey` 등)는 `wasPressed()`가 없거나 소비되면 이동이 끊김.  
   `prevPressed` 필드로 수동 rising-edge 추적 구현 필요. (IMPL-01, IMPL-03 명세 참고)

---

*작성: 2026-04-22 세션 코드 전수 직접 독해 결과*  
*이 파일과 `PORTING_COMPLETION_GUIDE.md`(R-09~R-13)를 합산하면 1:1 포팅 완결.*
