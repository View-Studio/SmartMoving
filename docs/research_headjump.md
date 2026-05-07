# 헤드점프 (Head Jump) 기능 — 원본 1.7.10 전수 리서치

> 본 문서는 원본 SmartMoving 1.7.10 의 **헤드점프(head jump)** 관련 코드를
> **한 줄도 누락 없이** 수집·정리한 1:1 번역 기준 자료다.
> 애니메이션(렌더) 부분은 별도 항목(§E)에 분리만 해두고, 기능 구현 시 1차 참조 대상은 §A~§D 다.
>
> 원본 경로: `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\`
> 1.21.1 매핑 경로: `C:\Users\user\IdeaProjects\SmartMoving\src\`

---

## 0. 헤드점프란?

원본 SmartMoving 의 점프 변형. 사용자 키 입력 시퀀스:

1. 달리기/스프린트 중 **grab 키** 누른 채로
2. **점프 키 짧게 탭** → `headJumpCharge` 가 1 tick 씩 누적되며 **점프키를 떼는 순간 발사**
3. 발사되면 캐릭터가 머리부터 내미는 자세(SLIDING POSE, 납작) 로 **수평 비행**
4. 차지가 길수록 수직 성분이 줄고 수평 성분이 늘어남(`getHeadJumpFactor` 의 비율 변환)
5. 착지 시 거리에 따라 **헤드 낙하 데미지** (`_headFallDamageStartDistance / _headFallDamageFactor`)
6. 자세는 1m 높이 (`heightOffset = -1F`, vanilla SLIDING POSE 1:1)

추가 변형:
- **WallHeadJump** : 벽에 부딪힌 상태에서 grab + 점프 → 머리부터 내미는 벽 점프
- **ClimbBackHeadJump** : 자유 등반(free climb) 중 등반 정지 상태에서 점프 → 뒤로 헤드 점프
- **SlideToHeadJumping** : 슬라이딩 중 작은 낙하(0.05F 초과) → 자동 헤드점프 전환 + `isAerodynamic = true`
- **HeadJumpControlFactor** : 공중 헤드점프 중 이동 속도 인자 ×0.2 (제어 어려움 표현)

---

## A. 핵심 상태 / 필드 — 원본

### A-1. `SmartMoving.java` (`SmartMoving` abstract)
| 라인 | 코드 | 설명 |
|------|------|------|
| L47 | `public boolean isHeadJumping;` | 헤드점프 중 플래그 (self/remote 공통) |
| L146 | `net.smart.render.SmartRenderRender.getPreviousRendererData(sp).rotateAngleY += isHeadJumping ? Half : Quarter;` | `onStartClimbBackJump()` 안 — climb back 점프 시작 시 prev render Y 회전 가산. **헤드점프 분기에서는 +180° (`Half`)**, 일반 climb back은 +90° (`Quarter`) |

### A-2. `SmartMovingSelf.java` (`SmartMovingSelf extends SmartMoving`)
| 라인 | 코드 | 설명 |
|------|------|------|
| L1452 | `public float headJumpCharge;` | 차지 카운터 (틱당 ++) |
| L1325-L1328 | `public float getHeadJumpCharge() { return headJumpCharge; }` | `ISmartMovingSelf` 구현 |
| L3075 | `public boolean wasHeadJumping;` | 직전 틱 `isHeadJumping` (해제 엣지 검출용) |

### A-3. `ISmartMovingSelf.java` / `ISmartMovingClient.java`
| 라인 | 코드 |
|------|------|
| `ISmartMovingSelf` L26 | `float getHeadJumpCharge();` |
| `ISmartMovingClient` L26 | `float getMaximumHeadJumpCharge();` |

### A-4. `SmartMovingClient.java` L47-L50
```java
public float getMaximumHeadJumpCharge() {
    return Config._headJumpChargeMaximum.value;
}
```

### A-5. `SmartMovingContext.java` L51
```java
public static final float SlideToHeadJumpingFallDistance = 0.05F;
```

---

## B. 입력 / 차지 로직 — 원본 `SmartMovingSelf.handleJumping()` L1880-L1899

```java
boolean isHeadJumpCharging = false;
if (Config.isHeadJumpingEnabled())                                              // L1881
{
    isHeadJumpCharging =
            grabButton.Pressed
         && (isGroundSprinting || isSprintJump || (isRunning() && sp.onGround))
         && !isCrawling;                                                        // L1883
    if (isHeadJumpCharging)
        if (esp.movementInput.jump)
            headJumpCharge++;                                                   // L1886
        else
        {
            if (headJumpCharge > 0 && sp.onGround)
                tryJump(Config.HeadUp, null, null, null);                       // L1890 — 발사
            headJumpCharge = 0;                                                 // L1891
        }
    else
    {
        if (headJumpCharge > 0)
            blockJumpTillButtonRelease = true;                                  // L1896
        headJumpCharge = 0;                                                     // L1897
    }
}
```

**규칙 분해 (1:1 번역 시 누락 금지):**
1. `Config.isHeadJumpingEnabled()` (= `_headJump.value && enabled`) 가드 외부 분기 전체 무시.
2. `isHeadJumpCharging` = **grab 누름** AND (지상스프린트 OR sprintJump 잔존 OR (`isRunning() && onGround`)) AND **NOT crawling**.
3. 차징 중 + 점프키 누름 → `headJumpCharge` ++ (틱당 1).
4. 차징 중 + 점프키 떨어진 순간 (= release edge):
   - `headJumpCharge > 0 && sp.onGround` → `tryJump(Config.HeadUp, null, null, null)` 발사
   - `headJumpCharge = 0` (발사 직후 초기화)
5. 차징 외 (grab 떼거나 sprint 끊김 등):
   - 차지 잔존 시 `blockJumpTillButtonRelease = true` (직후 일반 점프 차단)
   - `headJumpCharge = 0`

이어 **L1915 일반 점프 가드:**
```java
if (jump && !blockJumpTillButtonRelease && !isJumpCharging
         && !isHeadJumpCharging && !isVineAnyClimbing)
    tryJump(Config.Up, false, null, null);
```
→ 차징 중에는 일반 점프 발사 금지. 일반 점프 키는 차지 누적/release 발사로 흡수.

---

## C. 발사 로직 — 원본 `SmartMovingSelf.tryJump()` L1999-L2136

### C-1. 함수 시그니처 / type 분기
```java
public boolean tryJump(int type, Boolean inWaterOrNull, Boolean isRunningOrNull, Float angle)
```
- type 상수 (원본 `SmartMovingClientConfig` L178-L192):
  ```
  Up=0, ChargeUp=1, Angle=2, HeadUp=3, SlideDown=4,
  ClimbUp=5, ClimbUpHandsOnly=6, ClimbBackUp=7, ClimbBackUpHandsOnly=8,
  ClimbBackHead=9, ClimbBackHeadHandsOnly=10,
  WallUp=11, WallHead=12, WallUpSlide=13, WallHeadSlide=14
  ```
- L2012 — head 분기 인식:
  ```java
  boolean head = type == Config.HeadUp
              || type == Config.ClimbBackHead
              || type == Config.ClimbBackHeadHandsOnly
              || type == Config.WallHead;
  ```
- L2011 — `up = ... || type == Config.HeadUp || ...` (`up=true`).

### C-2. exhaustion (소비) — L2018-L2027
헤드점프 type별 exhaustion gain/stop 분기는 `SmartMovingClientConfig.isJumpExhaustionEnabled` /
`getJumpExhaustionGain` / `getJumpExhaustionStop` (§D-3) 안에서 처리. 원본에서 헤드점프 관련 키:
- `_climbJumpBackHeadExhaustion` / `_climbJumpBackHeadExhaustionGainFactor=20F` / `_climbJumpBackHeadExhaustionStopFactor=80F`
- `_wallHeadJumpExhaustion` / `_wallHeadJumpExhaustionGainFactor=20F` / `_wallHeadJumpExhaustionStopFactor=80F`

### C-3. 수직/수평 인자 계산 — L2029-L2038
```java
float jumpFactor = sp.isPotionActive(Potion.jump) ? 1 + (... * 0.2F) : 1;
float horizontalJumpFactor = Config.getJumpHorizontalFactor(speed, type) * jumpFactor;
float verticalJumpFactor   = Config.getJumpVerticalFactor  (speed, type) * jumpFactor;
float jumpChargeFactor     = charged ? Config.getJumpChargeFactor(jumpCharge) : 1F;

if (!up) {                       // head 의 경우 up=true 이므로 미진입
    horizontalJumpFactor = sqrt(h*h + v*v);
    verticalJumpFactor = 0;
}
```

### C-4. 초기 motion — L2040-L2045
```java
Double maxHorizontalMotion = null;
double horizontalMotion = sqrt(jumpMotionX² + jumpMotionZ²);
double verticalMotion   = -0.078 + 0.498 * verticalJumpFactor * jumpChargeFactor;
if (horizontalJumpFactor > 1F && !sp.isCollidedHorizontally)
    maxHorizontalMotion = (double) Config.getMaxHorizontalMotion(speed, type, inWater)
                          * getCombinedSpeedFactor();
```

### C-5. **헤드점프 핵심: 각도 재계산** — L2065-L2079
```java
if (head)
{
    double normalAngle = Math.atan(verticalMotion / horizontalMotion);
    double totalMotion = Math.sqrt(verticalMotion * verticalMotion
                                 + horizontalMotion * horizontalMotion);

    double newAngle = Config.getHeadJumpFactor(headJumpCharge) * normalAngle;
    double newVerticalMotion   = totalMotion * Math.sin(newAngle);
    double newHorizontalMotion = totalMotion * Math.cos(newAngle);

    if (maxHorizontalMotion != null)
        maxHorizontalMotion = maxHorizontalMotion * (newHorizontalMotion / horizontalMotion);

    verticalMotion   = newVerticalMotion;
    horizontalMotion = newHorizontalMotion;
}
```
- 본래 점프 각도 `normalAngle` = atan(v/h)
- `getHeadJumpFactor(headJumpCharge)` 가 0~1 비율을 반환 → 새 각도가 **점점 작아짐** (수평에 가까워짐)
- 동일 totalMotion 보존, sin/cos 분해로 v/h 재배분
- maxHorizontalMotion 도 같은 비율로 조정

### C-6. angle != null 분기 — L2081-L2095
헤드점프(L1890)는 `angle == null` 로 호출되므로 이 분기는 미진입. 단,
**WallHead/ClimbBackHead 호출은 angle 인자 전달** → 분기 진입.

### C-7. up && !noVertical 분기 — L2113-L2118
```java
if (up && !noVertical) {
    sp.motionY = verticalMotion;
    sp.addStat(StatList.jumpStat, 1);
    isSprintJump = isFast;
}
```
- `noVertical` = `WallUpSlide / WallHeadSlide` 만 true. HeadUp/ClimbBackHead 는 false → motionY 적용.

### C-8. **head 후처리** — L2126-L2130
```java
if (head)
{
    isHeadJumping = true;
    setHeightOffset(-1);
}
sp.isAirBorne = true;
isJumping = true;
onLivingJump();
```
- `isHeadJumping = true` 진입
- `setHeightOffset(-1)` → 1m 납작 박스 (vanilla SLIDING POSE 매핑)
- `isAirBorne` / `isJumping` / `onLivingJump` 는 vanilla 표준

---

## D. Config / 상수 — 원본

### D-1. `SmartMovingConfig.java` 헤드점프 키 정의

| 라인 | 키 / 기본값 | 설명 |
|------|-------------|------|
| L260 | `_headJump`<br>`"move.jump.head.charge"` (구 `move.forward.jump.charge`) | 헤드점프 ON/OFF (default true) |
| L261 | `_headJumpControlFactor`<br>`"move.jump.head.control.factor"` def=**0.2F** | 공중 헤드점프 중 이동 인자 |
| L262 | `_headJumpChargeMaximum`<br>`"move.jump.head.charge.maximum"` def=**10F** | 차지 최대치 (틱) |
| L264 | `_headFallDamageStartDistance`<br>`"move.fall.head.damage.start.distance"` def=**2F** (1F~3F) | 헤드 낙하 데미지 시작 거리 |
| L265 | `_headFallDamageFactor`<br>`"move.fall.head.damage.factor"` def=**2F** | 데미지 배율 |
| L286 | `_climbBackHeadJump`<br>`"move.jump.climb.back.head"` | climb back head jump ON/OFF |
| L287 | `_climbBackHeadJumpVerticalFactor` def=**0.2F** (pre_3.1: 1F) | 등반 뒤 헤드점프 수직 인자 |
| L288 | `_climbBackHeadJumpHorizontalFactor` def=**0.3F** (pre_3.1: 1F) | 등반 뒤 헤드점프 수평 인자 |
| L289 | `_climbBackHeadJumpHandsOnlyVerticalFactor` def=**0.8F** | hands-only 가산 수직 |
| L290 | `_climbBackHeadJumpHandsOnlyHorizontalFactor` def=**1F** | hands-only 가산 수평 |
| L300 | `_wallHeadJump` `"move.jump.wall.head"` | wall head jump ON/OFF |
| L301 | `_wallHeadJumpVerticalFactor` def=**0.3F** | 벽 헤드점프 수직 |
| L302 | `_wallHeadJumpHorizontalFactor` def=**0.15F** | 벽 헤드점프 수평 |
| L303 | `_wallHeadJumpFallMaximumDistance` def=**3F** (≥ `_wallUpJumpFallMaximumDistance`) | 헤드점프 가능 최대 낙하거리 |
| L345-L347 | `_climbJumpBackHeadExhaustion` / Gain=20F / Stop=80F | 등반 뒤 헤드점프 exhaustion |
| L363-L365 | `_wallHeadJumpExhaustion` / Gain=20F / Stop=80F | 벽 헤드점프 exhaustion |

### D-2. `SmartMovingClientConfig.java` 핵심 함수

**L137-L140 — 활성 검사**
```java
public boolean isHeadJumpingEnabled() { return _headJump.value && enabled; }
```

**L194-L227 — `isJumpingEnabled(speed, type)`** (헤드점프 관련 행만)
```java
if (type == ClimbBackHead || type == ClimbBackHeadHandsOnly)
    return _climbBackHeadJump.value;
if (type == WallHead)
    return _wallHeadJump.value;
```

**L410-L416 — 차지 → 비율 변환 (헤드점프 핵심 식)**
```java
public float getHeadJumpFactor(float headJumpCharge)
{
    if (!enabled || !_headJump.value) return 1F;
    headJumpCharge = Math.min(headJumpCharge, _headJumpChargeMaximum.value);
    return (headJumpCharge - 1) / (_headJumpChargeMaximum.value - 1);
}
```
- charge=1 → 0 (수평으로 완전히 누움)
- charge=`_headJumpChargeMaximum` (=10) → 1 (변환 없음, 일반 점프 각도)
- charge=0 (이론상 미발화) → 음수
- 즉 `getHeadJumpFactor` 가 작을수록 newAngle 작 → newVertical 작 + newHorizontal 큼.

**L418-L463 — `getJumpVerticalFactor`** (헤드 관련 누적)
```java
if (type == ClimbBackHead || type == ClimbBackHeadHandsOnly)
    result *= _climbBackHeadJumpVerticalFactor.value;        // 0.2F
if (type == ClimbBackHeadHandsOnly)
    result *= _climbBackHeadJumpHandsOnlyVerticalFactor.value; // 0.8F (가산)

if (type == WallUp || type == WallHead)
    result *= _wallUpJumpVerticalFactor.value;               // 0.4F
if (type == WallHead)
    result *= _wallHeadJumpVerticalFactor.value;             // 0.3F (가산)
```
- 즉 `WallHead` 의 vertical = base × wallUp(0.4) × wallHead(0.3)
- `ClimbBackHeadHandsOnly` 의 vertical = base × backHead(0.2) × handsOnly(0.8) (누적)

**L465-L505 — `getJumpHorizontalFactor`** (헤드 관련 누적, 동일 패턴)
```java
if (type == ClimbBackHead || type == ClimbBackHeadHandsOnly)
    result *= _climbBackHeadJumpHorizontalFactor.value;
if (type == ClimbBackHeadHandsOnly)
    result *= _climbBackHeadJumpHandsOnlyHorizontalFactor.value;

if (type == WallUp)
    result *= _wallUpJumpHorizontalFactor.value;
if (type == WallHead)
    result *= _wallHeadJumpHorizontalFactor.value;           // ※ wallUpHorizontal 미곱 (Vertical 과 다른 점)
```
**※ 주의:** `WallHead` 의 horizontal 은 wallUp 비누적 (단독 적용). vertical 만 누적 (`WallUp || WallHead`).

### D-3. exhaustion 분기 헤드점프 부분 — L244-L289 / L291-L346 / L348-L399
- `isJumpExhaustionEnabled`: `WallHead` → `_wallHeadJumpExhaustion`, `ClimbBackHead*` → `_climbJumpBackHeadExhaustion`. 추가로 `WallUp/WallHead` 그룹은 `_wallJumpExhaustion`, `Climb*` 그룹은 `_climbJumpExhaustion` AND.
- `getJumpExhaustionGain` / `getJumpExhaustionStop`: 같은 분기 구조로 head 전용 factor 누적.

---

## E. 매 틱 상태 재평가 / 해제 / 전환 — 원본 `updateEntityActionState()`

### E-1. setIsJumping (vanilla 점프 invoke 차단) — L2367-L2370
```java
isp.setIsJumpingField(
    esp.movementInput.jump && !isCrawling && !isSliding
 && !(Config.isHeadJumpingEnabled() && grabButton.Pressed && sp.isSprinting())
 && !(Config.isJumpChargingEnabled() && wouldIsSneaking && sp.onGround && isStanding)
 && !blockJumpTillButtonRelease);
```
- 헤드점프 차징 조건 (`headJumpEnabled && grab && sprint`) 동안 vanilla 점프 invoke 차단.

### E-2. wantClimb 가드 — L2467-L2477
```java
boolean wouldWantClimb = (
        grabButton.Pressed
     || (isClimbHolding && sneakButton.Pressed)
     || (Config.isFreeClimbAutoLaddderEnabled() && isFacedToLadder(...))
     || (Config.isFreeClimbAutoVineEnabled() && isFacedToSolidVine)
    )
 && (!isSliding || grabButton.Pressed && esp.movementInput.moveForward > 0F)
 && !isHeadJumping        // ★
 && !wantCrawlNotClimb
 && !disabled;
```
- 헤드점프 중 자유 등반 시도 차단.

### E-3. **isHeadJumping 매 틱 재평가** — L2524-L2530
```java
wasHeadJumping = isHeadJumping;
isHeadJumping = isHeadJumping
 && !sp.onGround
 && !(isSwimming || isDiving)
 && !(isFlying || sp.capabilities.isFlying)
 && !(sp.handleWaterMovement() && sp.motionY < 0)
 && !sp.handleLavaMovement();
```
- 진입은 §C-8 / §G 의 set, **해제는 이 5-AND** 만으로 결정.

### E-4. isAerodynamic 리셋 — L2532-L2533
```java
if (!isHeadJumping)
    isAerodynamic = false;
```

### E-5. 해제 엣지 후처리 — L2535-L2540
```java
if (wasHeadJumping && !isHeadJumping)
    if (sp.onGround) {
        handleCrash(Config._headFallDamageStartDistance.value,
                    Config._headFallDamageFactor.value);
        restoreFromFlying = true;            // ★ standupIfPossible 호출 트리거
    }
```
- 즉 헤드점프 종료 + 지면 착지 시 헤드 낙하 데미지 + `restoreFromFlying=true`로 강제 직립 복원 트리거.

### E-6. tryLanding / restoreFromFlying — L2542-L2544
```java
boolean tryLanding = isFlying && !Options._flyCloseToGround.value
                  && horizontalSpeedSquare < 0.003D && sp.motionY > -0.03D;
if (restoreFromFlying || tryLanding)
    standupIfPossible(tryLanding, restoreFromFlying);
```

### E-7. **SlideToHeadJumping 전환** — L2546-L2551
```java
if (isSliding && sp.fallDistance > SlideToHeadJumpingFallDistance) {  // 0.05F
    isSliding   = false;
    isHeadJumping = true;
    isAerodynamic = true;
}
```

### E-8. SlideDown 진입 (참고, 헤드점프 해제) — L2553-L2561
```java
if (Config.isSlidingEnabled() && grabButton.Pressed
 && (isGroundSprinting || (wasRunning && !isRunning && sp.onGround))
 && !isCrawling && sneakButton.StartPressed && !isDipping)
{
    setHeightOffset(-1);
    move(0, (-1D), 0, true);
    tryJump(Config.SlideDown, false, wasRunning, null);
    isSliding = true;
    isHeadJumping = false;             // ★
    isAerodynamic = false;             // ★
}
```

### E-9. wouldWantSneak 가드 — L2577-L2586
```java
boolean wouldWantSneak =
        !isFlying
     && !isSliding
     && !isHeadJumping             // ★ 헤드점프 중 sneak 미적용
     && !(isDiving && Config._diveDownOnSneak.value)
     && !(isSwimming && Config._swimDownOnSneak.value && !isFakeShallowWaterSneaking)
     && sneakContinueInput
     && !wantCrawl
     && !mustCrawl
     && (!Config.isCrawlingEnabled() || !grabButton.Pressed);
```

### E-10. canWallJumping 가드 — L2868
```java
boolean canWallJumping = Config.isWallJumpEnabled()
 && !isHeadJumping       // ★
 && !sp.onGround && !isClimbing && !isSwimming && !isDiving
 && !isLevitating && !isFlying;
```
- 헤드점프 중 추가 wall jump 시도 차단.

---

## F. 공중 이동 제어 (`headJumpControlFactor`) — 원본 `move()` 안 L703-L706

```java
if (isHeadJumping)
    speedFactor *= Config._headJumpControlFactor.value;        // 0.2F
else if (Config.enabled && !sp.onGround
      && !sp.capabilities.isFlying && !isFlying)
    speedFactor *= Config._jumpControlFactor.value;
```
- 헤드점프 중 공중 이동 인자 ×0.2 (조작 어렵게 표현).
- 위 분기는 `if (isClimbing && ...) ... else if (!isSliding) {`  안 — 슬라이딩 중에는 미적용.

---

## G. WallHeadJump — 원본 `handleWallJumping()` L1946-L1996

```java
public void handleWallJumping()
{
    if (!wantWallJumping || Double.isNaN(horizontalCollisionAngle))
        return;

    int jumpType;
    if (grabButton.Pressed) {
        if (sp.fallDistance > Config._wallHeadJumpFallMaximumDistance.value)   // 3F
            return;
        jumpType = wasCollidedHorizontally ? Config.WallHeadSlide : Config.WallHead;
    } else {
        if (sp.fallDistance > Config._wallUpJumpFallMaximumDistance.value)
            return;
        jumpType = wasCollidedHorizontally ? Config.WallUpSlide : Config.WallUp;
    }

    float jumpAngle;
    if (!wasCollidedHorizontally) {
        float movementAngle = getAngle(jumpMotionZ, -jumpMotionX);
        if (Double.isNaN(movementAngle)) return;
        jumpAngle = horizontalCollisionAngle * 2 - movementAngle + 180F;
    } else
        jumpAngle = horizontalCollisionAngle;

    while (jumpAngle > 360F) jumpAngle -= 360F;

    if (Config._wallUpJumpOrthogonalTolerance.value != 0F) {
        float aligned = jumpAngle;
        while (aligned > 45F) aligned -= 90F;
        if (Math.abs(aligned) < Config._wallUpJumpOrthogonalTolerance.value)
            jumpAngle = Math.round(jumpAngle / 90F) * 90F;
    }

    if (tryJump(jumpType, null, null, jumpAngle))
    {
        continueWallJumping = !isHeadJumping;     // ★ WallHead 후 → false
        sp.isCollidedHorizontally = false;
        sp.rotationYaw = jumpAngle;
        onStartWallJump(jumpAngle);               // SmartMoving.java L150
    }
}
```

**`onStartWallJump(angle)` — SmartMoving.java L150-L156**
```java
protected void onStartWallJump(Float angle) {
    if (angle != null)
        net.smart.render.SmartRenderRender.getPreviousRendererData(sp).rotateAngleY = angle / RadiantToAngle;
    isWallJumping = true;
    sp.fallDistance = 0F;
}
```

---

## H. ClimbBackHeadJump — 원본 `handleClimbing()` L1055-L1077 (관련 부분)

```java
if (isClimbHolding)
{
    setOnlyShouldClimbSpeed(HoldMotion);

    if (jumpButton.StartPressed)
    {
        boolean handsOnly = feetClimbing != FeetClimbing.None;

        int type = (Options._climbJumpBackHeadOnGrab.value
                        ? grabButton.Pressed
                        : !grabButton.Pressed)
                ? (handsOnly ? Config.ClimbBackHead   : Config.ClimbBackHeadHandsOnly)
                : (handsOnly ? Config.ClimbBackUp     : Config.ClimbBackUpHandsOnly);

        float jumpAngle = sp.rotationYaw + 180F;
        if (tryJump(type, null, null, jumpAngle))
        {
            continueWallJumping = !isHeadJumping;     // ★ ClimbBackHead 후 → false
            isClimbing = false;
            sp.rotationYaw = jumpAngle;
            onStartClimbBackJump();                   // SmartMoving.java L144
        }
    }
}
```

**`onStartClimbBackJump()` — SmartMoving.java L144-L148**
```java
protected void onStartClimbBackJump() {
    net.smart.render.SmartRenderRender.getPreviousRendererData(sp).rotateAngleY +=
        isHeadJumping ? Half : Quarter;
    isClimbBackJumping = true;
}
```
- ClimbBackHead 의 경우 prev render Y 회전 += 180°, 일반 ClimbBackUp 은 += 90°.

**※ Options._climbJumpBackHeadOnGrab** (원본 `SmartMovingOptions`):
- false (기본) : grab 안 누른 상태에서 `jumpStart` → headJump 분기
- true : grab 누른 상태에서 `jumpStart` → headJump 분기

---

## I. 직립 / 박스 복원 — 원본 `standUp()` / `toSlidingOrCrawling()`

### I-1. `standUp(double gapUnderneight)` — L2214-L2220
```java
private void standUp(double gapUnderneight) {
    move(0, (1D - gapUnderneight), 0, true);
    isCrawling     = false;
    isHeadJumping  = false;          // ★
    resetHeightOffset();
}
```

### I-2. `toSlidingOrCrawling(double gapUnderneight)` — L2222-L2230
```java
private void toSlidingOrCrawling(double gapUnderneight) {
    move(0, (-gapUnderneight), 0, true);
    if (Config.isSlidingEnabled()
     && (grabButton.Pressed || wasHeadJumping))           // ★ wasHeadJumping → 슬라이딩 진입
        isSliding = true;
    else
        wasCrawling = toCrawling();
}
```
- 헤드점프 종료 직후 (지면 가까이 + 공간 부족 시) 슬라이딩 자동 진입.

### I-3. `resetState()` — L2294
```java
this.isHeadJumping = false;
```

---

## J. 네트워크 동기화 — 원본 `SmartMovingSelf.java` L3146 + `SmartMovingOther.java` L77

**state pack (Self → Server → Other broadcast)**, L3142-L3146 부분:
```java
state <<= 1; state |= isSliding ? 1 : 0;
state <<= 1; state |= isHeadJumping ? 1 : 0;     // ★
state <<= 1; state |= isLevitating ? 1 : 0;
state <<= 1; state |= isCeilingClimbing ? 1 : 0;
```

**state unpack (Other 측), L77:**
```java
isHeadJumping = (state & 1) != 0;
state >>>= 1;
```
- bit 19/20 부근. SLIDING POSE 자동 적용용 (small dim → height = 1).

---

## K. 채팅 코드 — 원본 `SmartMovingComm.java` L161

```java
processBlockCode(codes, "§9", Options._headJump);
```
- 서버가 `§9` 포함 메시지 송출 시 클라이언트 `_headJump = false` 강제 (옵션 비활성화).

---

## L. HUD — 원본 `SmartMovingRender.java` L222-L223

```java
float maxRunJumpCharge = Config._headJumpChargeMaximum.value;
float runJumpCharge    = Math.min(moving.headJumpCharge, maxRunJumpCharge);
```
- HUD 차지 게이지 (점프 차지와 동일 위치 표시).

---

## M. 렌더 / 모델 — 원본 (애니메이션 영역, 본 작업 deferred — 별도 사이클)

> **본 작업 범위에서 제외.** 사용자 지시: "기능 먼저 작업하자". 애니메이션은 다음 사이클.
> 단 의존성 파악만 정리.

### M-1. `SmartMovingRender.java`
| 라인 | 코드 / 설명 |
|------|------|
| L78 | `boolean isHeadJump = moving.isHeadJumping;` (모델 입력 변환) |
| L89 | `float smallOverGroundHeight = isCrawlClimb || isHeadJump ? (float)moving.getOverGroundHeight(5D) : 0F;` |
| L90 | `Block overGroundBlock = isHeadJump && smallOverGroundHeight < 5F ? moving.getOverGroundBlockId(smallOverGroundHeight) : null;` |
| L109 | `modelPlayer.isHeadJump = isHeadJump;` |
| L147 | bodyYaw 강제 분기 OR 절: `isHeadJumping` 포함 |

### M-2. `SmartMovingModel.java`
| 라인 | 코드 / 설명 |
|------|------|
| L52 | `isHeadJump = SmartMovingRender.CurrentMainModel.isHeadJump;` |
| L505+ | `else if (isHeadJump) { /* 모델 회전 분기 */ }` |
| L782 | `public boolean isHeadJump;` |

### M-3. `SmartMovingRenderPlayerBase.java` L100
```java
return render.modelBipedMain.isFlying || render.modelBipedMain.isSwim
    || render.modelBipedMain.isDive  || render.modelBipedMain.isHeadJump;
```
(`getRenderForCape` 등에서 망토 처리 가드).

---

# § 1.21.1 매핑 현황 (포팅 진행 정도)

> 본 섹션은 1.21.1 측 현재 매핑 상태 + 헤드점프 기능 관련 부족한/의심 가는 영역 식별용.

## 1.21.1 — 이미 매핑된 항목

| 원본 위치 | 1.21.1 위치 | 비고 |
|-----------|-------------|------|
| `SmartMoving.isHeadJumping` (L47) | `SmartMovingClientState.java` L55 `isHeadJumping` | OK |
| `SmartMovingSelf.headJumpCharge` (L1452) | `SmartMovingClientState.java` L46 `headJumpCharge` | OK |
| `SmartMovingSelf.wasHeadJumping` (L3075) | `SmartMovingClientState.java` L63 `wasHeadJumping` | OK |
| state pack/unpack bit | `SmartMovingState.java` bit 20 + `SmartMovingClientState.java` L1117 `((bits>>20)&1)` + `SmartMovingServer.java` bit 20 | OK |
| handleJumping 차징 (L1880-L1899) | `SmartMovingJumper.java` L454-L473 | OK (`!sm.isSliding` 가드 추가됨) |
| jump 가드 `!isHeadJumpCharging` (L1915) | `SmartMovingJumper.java` L498 | OK |
| tryJump head 분기 (L2065-L2079) | `SmartMovingJumper.java` L251-L263 | OK |
| tryJump head 후처리 (L2126-L2130) | `SmartMovingJumper.java` L306-L311 (`isHeadJumping=true; setPoseSmall; heightOffset=-1F`) | OK |
| handleWallJumping (L1946-L1996) | `SmartMovingJumper.java` L544-L678 부근 | OK |
| ClimbBack head jump (L1060-L1077) | `SmartMovingClimber.java` L1045-L1069 | OK |
| 매 틱 5-AND 재평가 (L2524-L2533) | `SmartMovingClientState.java` L2024-L2033 | OK |
| handleCrash 해제 엣지 (L2535-L2540) | `SmartMovingClientState.java` L2040-L2045 | OK (B-N-standup `standupIfPossible(player,false,true)` 추가) |
| SlideToHeadJumping (L2546-L2551) | `SmartMovingClientState.java` L2049-L2053 | OK |
| wouldWantSneak 가드 (L2577-L2586) | `SmartMovingClientState.java` L1497 부근 | OK |
| canWallJumping 가드 (L2868) | `SmartMovingJumper.java` L564 부근 | OK |
| wantClimb 가드 (L2475) | `SmartMovingClientState.java` L1583 부근 | OK |
| `isHeadJumping`/`isSliding` SLIDING POSE | `MixinPlayerEntity.java` L131 / `MixinPlayerEntityClient.java` L222 | OK |
| `setIsJumping` 가드 (L2367-L2370) | `MixinClientPlayerEntity.java` L122 | OK |
| `headJumpControlFactor` (L703-L706) | `SmartMovingMover.java` L230-L231 + `MixinLivingEntityClient.java` L326-L327 | OK |
| `standUp` `isHeadJumping=false` (L2218) | `SmartMovingJumper.java` L96-L114 (B-24) | OK |
| `toSlidingOrCrawling` `wasHeadJumping` 분기 (L2226) | `SmartMovingClientState.java` L3416-L3418 + `SmartMovingJumper.java` L118-L123 | OK |
| `resetState` `isHeadJumping=false` (L2294) | `SmartMovingClientState.java` L2759 / L3397 | OK |
| HUD `headJumpChargeMaximum / headJumpCharge` (L222-L223) | `SmartMovingHud.java` L51-L56 | OK |
| 채팅 코드 §9 (L161) | `SmartMovingClient.java` L272 | OK |
| Config 키 다수 | `SmartMovingConfig.java` L235-L380 / L670-L755 / L1647-L1848 | OK (getHeadJumpFactor / getJumpVerticalFactor / getJumpHorizontalFactor / 모든 헤드 키 + 직렬화) |
| `_headFallDamageStartDistance / Factor` (L264-L265) | `SmartMovingConfig.java` L1194 부근 (B-24) | OK |

## 1.21.1 — 점검 / 의심 / 누락 후보 (사용자가 "수정해야 한다"고 한 영역 후보)

다음 항목들은 grep 결과로는 매핑 흔적이 보이나 **원본 1:1 정합성 / 동작 검증 필요**:

1. **`SlideToHeadJumpingFallDistance = 0.05F` 상수의 일관성**
   - 1.21.1 `SmartMovingClientState.java` L2049 에 `0.05F` 리터럴 박혀있음. 원본은 `SmartMovingContext` 정적 상수.
   - 상수 분리 vs 인라인 — 동작 동일이면 OK.

2. **`isHeadJumping` 진입 시 카메라/박스 동기화**
   - `MixinEntityClient.java` L105: `if (sm.heightOffset != 0F && sm.isHeadJumping) { ... }` — 헤드점프 한정 박스 갱신.
   - 원본은 매 틱 `setHeightOffset(-1)` + vanilla 박스 매 틱 갱신. 1.21.1 매핑은 `isHeadJumping` 가드로 제한 — 진입/종료 타이밍 일치 검증 필요.

3. **`continueWallJumping = !isHeadJumping` 의 두 호출처**
   - `SmartMovingClimber.java` L1063-L1069 (ClimbBack)
   - `SmartMovingJumper.java` L678 (Wall)
   - 두 곳 모두 set 순서: `tryJump` 가 `isHeadJumping=true` 를 먼저 set 하고 → `continueWallJumping = !isHeadJumping` 평가 → false 결과.
   - **순서 의존**: tryJump 가 head 분기로 `isHeadJumping=true` set 한 후 호출 측이 `continueWallJumping = !sm.isHeadJumping` 호출해야 정확히 false.
   - 1.21.1 매핑은 OK 패턴이나 race 가능성 검증 필요.

4. **handleCrash 의 `restoreFromFlying = true` + 즉시 `standupIfPossible` 호출**
   - 원본 L2535-L2544: `restoreFromFlying=true` set → 같은 함수 안 L2543 에서 `if (restoreFromFlying || tryLanding) standupIfPossible(...)` 호출.
   - 1.21.1 `SmartMovingClientState.java` L2040-L2045 (B-N-standup) 에서 `restoreFromFlying=true` 직후 `standupIfPossible(player,false,true)` 직접 호출. 원본은 같은 tick 안 별도 분기로 처리.
   - **순서 1:1 검증 필요** (특히 fallDistance reset 타이밍, isCrawling 진입 여부).

5. **`getJumpVerticalFactor` WallHead 누적 vs `getJumpHorizontalFactor` WallHead 단독**
   - 원본 L443-L446: `(WallUp || WallHead) → wallUpVertical(0.4)`, `WallHead → wallHeadVertical(0.3)` 누적.
   - 원본 L485-L488: `WallUp → wallUpHorizontal`, `WallHead → wallHeadHorizontal` (누적 X, 단독).
   - 1.21.1 `SmartMovingConfig.java` 매핑은 위 차이를 명시적으로 반영하는지 확인 필요 (L292-L294 vertical / L377-L384 horizontal).

6. **헤드점프 차징 조건 "isRunning() && sp.onGround"**
   - 원본 L1883: `(isGroundSprinting || isSprintJump || (isRunning() && sp.onGround))`
   - 1.21.1 `SmartMovingJumper.java` L456-L460 의 매핑 정확성 검증 필요. `isRunning()` 정의는 원본 별도 함수 (run 키 / 기본 sprint 매핑 차이).

7. **`setIsJumping` 가드 — 원본 L2367-L2370 정확 식**
   - `(esp.movementInput.jump && !isCrawling && !isSliding && !(headJumpEnabled && grab && sprint) && !(jumpChargeEnabled && wouldIsSneaking && onGround && isStanding) && !blockJumpTillButtonRelease)`
   - 1.21.1 `MixinClientPlayerEntity.java` L105 부근 매핑 검증.

8. **`onStartClimbBackJump()` 의 prev render Y 회전 보정**
   - 원본 `SmartMoving.java` L146: `rotateAngleY += isHeadJumping ? Half : Quarter` (Half=180°, Quarter=90°).
   - 1.21.1 매핑은 `MixinPlayerEntityRenderer.java` 측 `prevBodyYaw` 매핑일 가능성. **애니메이션 영역**이지만 `isClimbBackJumping` 시작 시 한 번만 발생하는 **기능적 회전** 이라 헤드점프 발사 직후 시점 회전 일치 검증 필요.
   - ※ 본 작업이 "기능"이라 deferred 가능, 애니메이션 사이클에서 처리.

9. **`setHeightOffset(-1)` 가 vanilla SLIDING POSE (height=0.6) 와 매핑되는 정확성**
   - 원본은 height=`1.8F + heightOffset` = 0.8F.
   - 1.21.1 vanilla SLIDING POSE 는 box height=0.6F. 차이가 발 위치/카메라/충돌 영향. → `setPoseSmall` 매핑이 0.6F 사용하면 원본보다 박스 작음.
   - 박스 크기 차이로 인한 grip/충돌/카메라 점프 BUG 후보.

10. **`SmartMovingComm` `processBlockCode("§9", _headJump)` (서버 강제 비활성화)**
    - 1.21.1 `SmartMovingClient.java` L272: `if (codes.contains("§9")) cfg.headJump = false;`
    - 단순 매핑이나 서버 측 packet 차이 검증 필요.

---

# § 작업 우선순위 후보 (체크리스트 작성용)

본 작업이 "기능 먼저"이므로 다음 영역은 다음 사이클 (애니메이션) 로 deferred:
- M섹션 전체 (Render / Model)
- `MixinPlayerEntityRenderer.java` 의 currentVerticalAngle / θ = Quarter - V 등
- `MixinPlayerEntityModelClient.java` 의 `sm_animateHeadJumping` 본체

**기능 사이클 점검 후보** (사용자가 어느 부분을 "수정"하려 하는지에 따라 선별):
- [ ] §B 차징 조건 1:1 정합 (특히 grab + sprint 검출 패턴)
- [ ] §C-5 헤드점프 각도 재계산 (`getHeadJumpFactor`) 의 charge=1 → 0 경계
- [ ] §C-8 발사 후 상태 set 순서 (`isHeadJumping=true; setPoseSmall; heightOffset=-1F`) 와 호출 측 `continueWallJumping = !isHeadJumping` 의 race
- [ ] §E-3 매 틱 5-AND 해제 식 (특히 multi 환경 server reconcile 영향)
- [ ] §E-5 해제 엣지 handleCrash + restoreFromFlying + standupIfPossible 호출 순서
- [ ] §E-7 SlideToHeadJumping 전환 시 isAerodynamic 부수효과
- [ ] §F headJumpControlFactor ×0.2 의 정확 적용 위치 (move 함수 안)
- [ ] §G WallHead 의 jumpAngle / orthogonalTolerance / continueWallJumping 흐름
- [ ] §H ClimbBackHead 의 hands-only 분기 (`feetClimbing != FeetClimbing.None`)
- [ ] §I-2 `wasHeadJumping` 으로 슬라이딩 자동 진입
- [ ] §J state bit 20 직렬화 정합 (multi)
- [ ] §K 채팅 코드 §9 (서버 강제 비활성화)
- [ ] §L HUD 게이지

---

# § 부록: 본 리서치에서 사용한 원본 파일 / 라인 인덱스

```
SmartMovingSelf.java      L703-L706   speedFactor *= _headJumpControlFactor
                          L1071       continueWallJumping = !isHeadJumping (climb back)
                          L1325-L1328 getHeadJumpCharge
                          L1452       headJumpCharge 필드
                          L1880-L1899 isHeadJumpCharging / charge++/release
                          L1915       jump 가드 !isHeadJumpCharging
                          L1946-L1996 handleWallJumping (WallHead)
                          L1992       continueWallJumping = !isHeadJumping (wall)
                          L1999-L2136 tryJump 본체
                          L2011       up 분기 (HeadUp/ClimbBackHead/.../WallHead)
                          L2012       head 분기 (HeadUp/ClimbBackHead*/WallHead)
                          L2065-L2079 head 각도 재계산 (getHeadJumpFactor)
                          L2113-L2118 up && !noVertical → motionY 적용
                          L2126-L2130 head 후처리 (isHeadJumping=true; setHeightOffset(-1))
                          L2218       standUp: isHeadJumping=false
                          L2226       toSlidingOrCrawling: wasHeadJumping → isSliding=true
                          L2294       resetState: isHeadJumping=false
                          L2367-L2370 setIsJumping 가드
                          L2475       wantClimb 가드 !isHeadJumping
                          L2524-L2530 매 틱 5-AND 재평가
                          L2532-L2533 !isHeadJumping → isAerodynamic=false
                          L2535-L2540 해제 엣지 handleCrash + restoreFromFlying
                          L2546-L2551 SlideToHeadJumping 전환
                          L2553-L2561 SlideDown 진입 (isHeadJumping=false)
                          L2577-L2586 wouldWantSneak 가드 !isHeadJumping
                          L2868       canWallJumping 가드 !isHeadJumping
                          L3075       wasHeadJumping 필드
                          L3146       state pack bit isHeadJumping

SmartMoving.java          L47         isHeadJumping 필드
                          L144-L148   onStartClimbBackJump (prev rotateAngleY += Half/Quarter)
                          L150-L156   onStartWallJump

SmartMovingOther.java     L77-L78     state unpack isHeadJumping

SmartMovingContext.java   L51         SlideToHeadJumpingFallDistance = 0.05F

ISmartMovingSelf.java     L26         getHeadJumpCharge
ISmartMovingClient.java   L26         getMaximumHeadJumpCharge
SmartMovingClient.java    L47-L50     getMaximumHeadJumpCharge 구현
SmartMovingComm.java      L161        §9 → _headJump 비활성화

SmartMovingConfig.java    L260-L265   _headJump / _headJumpControlFactor / _headJumpChargeMaximum / _headFallDamage*
                          L286-L290   _climbBackHeadJump / 4종 factor
                          L300-L303   _wallHeadJump / 3종 factor + fallMaximumDistance
                          L345-L347   _climbJumpBackHeadExhaustion (gain 20F / stop 80F)
                          L363-L365   _wallHeadJumpExhaustion (gain 20F / stop 80F)

SmartMovingClientConfig   L137-L140   isHeadJumpingEnabled
                          L172-L192   speed/type 상수 정의 (HeadUp=3, ClimbBackHead=9, WallHead=12 등)
                          L194-L227   isJumpingEnabled (head 분기)
                          L244-L289   isJumpExhaustionEnabled (head 분기)
                          L291-L346   getJumpExhaustionGain (head 분기)
                          L348-L399   getJumpExhaustionStop (head 분기)
                          L410-L416   getHeadJumpFactor ★
                          L418-L463   getJumpVerticalFactor (head 누적)
                          L465-L505   getJumpHorizontalFactor (head 누적)

SmartMovingRender.java    L78,89,90   isHeadJump 모델 입력 + overGroundHeight/Block (※ 애니메이션)
                          L109,147    modelPlayer.isHeadJump set / bodyYaw 강제 OR
                          L222-L223   HUD 차지 게이지

SmartMovingModel.java     L52,505,782 (※ 애니메이션 영역)
SmartMovingRenderPlayerBase.java L100 (※ 망토 가드)
```
