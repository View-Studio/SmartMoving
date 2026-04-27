# 원본 SmartMoving 1.7.10 모든 상태 속도 공식/값 정리

> 목적: 원본과 1:1 매핑 검증을 위해 모든 motion 상수, factor, 식, 흐름을 라인 단위로 추출.

---

## 1. Motion 상수 (`SmartMovingContext.java:31-51`)

```java
public static final float ClimbPullMotion          = 0.3F;          // L31
public static final double FastUpMotion            = 0.2D;          // L33
public static final double MediumUpMotion          = 0.14D;         // L34
public static final double SlowUpMotion            = 0.1D;          // L35
public static final double HoldMotion              = 0.08D;         // L36 ★ gravity 와 균형
public static final double SinkDownMotion          = 0.05D;         // L37
public static final double ClimbDownMotion         = 0.01D;         // L38
public static final double CatchCrawlGapMotion     = 0.17D;         // L39
public static final float SwimCrawlWaterMaxBorder  = 1F;            // L41
public static final float SwimCrawlWaterTopBorder  = 0.65F;         // L42
public static final float SwimCrawlWaterMediumBorder = 0.6F;        // L43
public static final float SwimCrawlWaterBottomBorder = 0.55F;       // L44
public static final float HorizontalGroundDamping    = 0.546F;      // L46 (slip 0.6 * 0.91)
public static final float HorizontalAirDamping       = 0.91F;       // L47
public static final float HorizontalAirodynamicDamping = 0.999F;    // L48 ★ headJump 시 적용
public static final float SwimSoundDistance         = 1F / 0.7F;    // L50
public static final float SlideToHeadJumpingFallDistance = 0.05F;   // L51
```

---

## 2. SmartMovingConfig default factor 값

```
speedFactor                    = 1F     (move.speed.factor)
sneakFactor                    = 0.3F   (move.sneak.factor)
crawlFactor                    = 0.15F  (move.crawl.factor)
runFactor                      = 1.3F   (move.run.factor)
sprintFactor                   = 1.5F   (move.sprint.factor)
sprintFactorLevitate           = 1.5F   (move.sprint.factor.levitate)
sprintFactorLevitateVertical   = 0.185F (move.sprint.factor.levitate.vertical)

flyingSpeedFactor              = 1.0F   (move.fly.factor)
freeClimbingUpSpeedFactor      = 1.0F   (move.climb.free.up.factor)
freeClimbingDownSpeedFactor    = 1.0F   (move.climb.free.down.factor)
ceilingClimbingSpeedFactor     = 0.2F   (move.ceiling.climb.factor)

jumpHorizontalFactor           = 1F     (move.jump.horizontal.factor)
jumpVerticalFactor             = 1F     (move.jump.vertical.factor)
sneakJumpHorizontalFactor      = 1F
walkJumpHorizontalFactor       = 1F
runJumpHorizontalFactor        = 2F     ★ 오버라이드
sprintJumpHorizontalFactor     = 2F     ★ 오버라이드
jumpChargeFactor               = 1.3F   (max charge 시)
```

---

## 3. SpeedFactor 계산 메서드 (`SmartMovingSelf.java:154-227`)

### 3-1. `getConfigSpeedFactor()` L154-157
```java
return Config.enabled ? Config._speedFactor.value * Config.getUserSpeedFactor() : 1F;
```
= 기본 1F. user speed factor (Creative 시 적용) 곱.

### 3-2. `getPotionSpeedFactor()` L159-162
```java
return Config.enabled ? getLandMovementFactor() * 10F / (sp.isSprinting() ? 1.3F : 1F) : 1F;
```
= **vanilla attribute (sprint 1.3x 포함) * 10 / sprint divisor**.
- vanilla attribute = 0.1 * (sprint ? 1.3 : 1).
- sprint 시: 0.13 * 10 / 1.3 = **1.0F**.
- 정지 시: 0.1 * 10 / 1 = **1.0F**.

= **결과 항상 1.0F** (no potion). potion 시 attribute 변경 → factor 변동.

### 3-3. `getNonSlowInputSpeedFactor()` L197-227
```java
float speedFactor = 1f;
if(isFast)
    speedFactor *= (!isLevitating() ? sprintFactor : sprintFactorLevitate);  // 1.5
if(isClimbing) ...

return speedFactor;
```
- **isFast 시: 1.5 (sprintFactor)**.
- 그 외: 1.0.

### 3-4. `getSlowInputSpeedFactor()` L164-195
```java
float speedFactor = 1f;
if (sp.isUsingItem()) speedFactor *= itemFactor (0.2 ~ 1.0);
if (isCrawling || (isCrawlClimbing && !isClimbCrawling))
    speedFactor *= crawlFactor (0.15);
else if (isSlow)
    speedFactor *= sneakFactor (0.3);
if (isCeilingClimbing)
    speedFactor *= ceilingClimbingSpeedFactor (0.2);
return speedFactor;
```

### 3-5. `getCombinedSpeedFactor()` L149-152
```java
return getConfigSpeedFactor() * getPotionSpeedFactor();
```
= 1F * 1F = **1.0F** (default, no potion).

---

## 4. 진입 흐름 (`SmartMovingSelf.superMoveEntityWithHeading` L95-152)

```java
private void superMoveEntityWithHeading(float moveStrafing, float moveForward) {
    // L107: handleJumping();
    // L119: ★ 핵심 speedFactor 계산
    float speedFactor = getConfigSpeedFactor() * getPotionSpeedFactor() * getNonSlowInputSpeedFactor(moveForward, moveStrafing);
    float slowInputSpeedFactor = getSlowInputSpeedFactor(moveForward, moveStrafing);

    // L121-130: vanilla 모드 vs SM 모드 분기
    if (vanilla()) {
        moveForward *= slowInputSpeedFactor;
        moveStrafing *= slowInputSpeedFactor;
    } else {
        speedFactor *= slowInputSpeedFactor;
    }

    // L133-136: 분기별 처리
    handleSwimming(moveForward, moveStrafing, speedFactor, ...);
    handleLava(moveForward, moveStrafing, ...);
    handleAlternativeFlying(moveForward, moveStrafing, speedFactor, ...);
    handleLand(moveForward, moveStrafing, speedFactor, ...);
}
```

= **최종 speedFactor = configFactor(1) * potionFactor(1) * nonSlowFactor(1 or 1.5) * slowFactor**.
- 일반 sprint (run, !isFast): **1F** (nonSlow=1).
- isFast sprint: **1.5F** (nonSlow=1.5).
- crawl: nonSlow * 0.15.
- sneak: nonSlow * 0.3.

---

## 5. Land 이동 (`landMotion` L675-810)

### 5-1. horizontalDamping 결정 L677-690
```java
if (sp.onGround && (!isJumping || vanilla())) {
    Block block = world.getBlock(floor(posX), floor(bb.minY) - 1, floor(posZ));
    horizontalDamping = block != null ? block.slipperiness * HorizontalAirDamping : HorizontalGroundDamping;
} else {
    horizontalDamping = HorizontalAirDamping;  // 0.91F
}
```

### 5-2. climbingUpIsBlocked 분기 L692-700
```java
if (isClimbing && climbingUpIsBlockedByLadder())     moveFlying(0, -1, 0.07F);
else if (isClimbing && climbingUpIsBlockedByTrapDoor()) moveFlying(0, -1, 0.09F);
else if (isClimbing && climbingUpIsBlockedByCobbleStoneWall()) moveFlying(0, -1, 0.07F);
```

### 5-3. ★ 일반 land moveFlying 호출 L702-719
```java
else if (!isSliding) {
    if (isHeadJumping)
        speedFactor *= Config._headJumpControlFactor.value;
    else if (Config.enabled && !sp.onGround && !sp.capabilities.isFlying && !isFlying)
        speedFactor *= Config._jumpControlFactor.value;

    float f3 = 0.1627714F / (horizontalDamping^3);
    float rawSpeed = sp.onGround ? 0.1f * f3
                                  : sp.jumpMovementFactor / (sp.isSprinting() && !sp.capabilities.isFlying ? 1.3F : 1F);

    if (!Config.enabled && sp.onGround)
        speedFactor *= (getLandMovementFactor() * 10f) / (sp.isSprinting() ? 1.3F : 1F);

    if (Config.isRunningEnabled() && isRunning() && !isFast)
        speedFactor *= !capabilities.isFlying ? Config._runFactor.value : Config._runFactorLevitate.value;  // 1.3

    if (!sp.onGround)
        speedFactor /= getPotionSpeedFactor();

    sp.moveFlying(moveStrafing, moveForward, rawSpeed * speedFactor);
}
```

**핵심 식**:
- `f3 = 0.1627714F / damping^3`.
  - onGround (slip=0.6): damping = 0.6 * 0.91 = 0.546. damping^3 = 0.163. f3 = 1.0.
  - 공중: damping = 0.91. damping^3 = 0.753. f3 = 0.216.
- `rawSpeed`:
  - onGround: `0.1 * f3 = 0.1` (slip 0.6).
  - 공중: `jumpMovementFactor / (sprint ? 1.3 : 1)` = 0.02/1.3 ≈ **0.01538** (sprint).
- `speedFactor` 추가 곱:
  - `isRunning && !isFast` 시 *= 1.3 (runFactor).
  - 공중 시 `/= potionFactor`.

**최종 결과**:
| 상태 | rawSpeed | speedFactor | final = rawSpeed * speedFactor |
|------|----------|-------------|------|
| onGround sprint (run, !isFast) | 0.1 | 1 * 1 * 1 * 1.3 = 1.3 | **0.13** (= vanilla sprint) |
| onGround sprint (isFast) | 0.1 | 1 * 1 * 1.5 * 1 = 1.5 | **0.15** (= vanilla * 1.154) |
| 공중 sprint (run, !isFast) | 0.01538 | 1 * 1 * 1 / 1 * 1.3 = 1.3 | **0.02** (= jumpMovementFactor) |
| 공중 sprint (isFast) | 0.01538 | 1 * 1 * 1.5 / 1 = 1.5 | **0.0231** (= 1.154x) |

★ **isFast 사용자 보고와 일치**: SM 모드 sprint 시 1.154x.
★ **일반 sprint 시 vanilla 와 동일** (run factor 1.3 = vanilla sprint mod 1.3).

### 5-4. ladder/vine 시 motion clamp + sneak L757-794
```java
if (isOnLadder || isOnVine) {
    motionX clamp ±0.15
    motionZ clamp ±0.15
    if (notTotalFreeClimbing) {
        fallDistance = 0;
        motionY = max(motionY, -0.15 * combinedFactor);  // L780
    }
    if (Config.isFreeBaseClimb()) {
        if (sneak && motionY < 0 && !onGround && notTotalFreeClimbing)
            motionY = 0;
    } else {
        if (localIsSneaking && motionY < 0)
            motionY = 0;
    }
}
```

---

## 6. 비행 (`handleAlternativeFlying` L602-631)

```java
boolean handleAlternativeFlying = !handledSwimming && !handledLava && capabilities.isFlying && Config.isFlyingEnabled();
if (handleAlternativeFlying) {
    resetSwimming();
    resetClimbing();

    float moveUpward = 0F;
    if (sneak) { motionY += 0.15D; moveUpward -= 0.98F; }
    if (jump)  { motionY -= 0.15D; moveUpward += 0.98F; }

    moveFlying(moveUpward, moveStrafing, moveForward,
               speedFactor * 0.05F * Config._flyingSpeedFactor.value,
               Options._flyControlVertical.value);

    sp.moveEntity(motionX, motionY, motionZ);

    motionX *= HorizontalAirDamping;  // 0.91
    motionY *= HorizontalAirDamping;
    motionZ *= HorizontalAirDamping;
}
```
- speedFactor (= configFactor*potionFactor*nonSlowFactor*slowFactor) 사용.
- final speed = speedFactor * 0.05F * flyingSpeedFactor.
- isFast sprint 시: 1.5 * 0.05 * 1.0 = **0.075**.
- 일반: 1.0 * 0.05 = **0.05**.

---

## 7. 클라이밍 (`handleClimbing` L814-1110)

### 7-1. 진입 + 8방향 탐색 L814-961 (이전 분석 완료)

### 7-2. wantClimbUp 분기 (L979-1027) — 7개 case
1. feetClimbing.FastUp + onGround/bed 예외 → **FastUpMotion (0.2)**.
2. (hasClimbGap||hasClimbCrawlGap) + handsClimbing.FastUp → **SlowUp (0.1) or FastUp (0.2)**.
3. feet+hands 둘 다 relevant + 3 예외 → **MediumUpMotion (0.14)**.
4. handsClimbing.IsUp() → **SlowUpMotion (0.1)**.
5. TopHold/BaseHold → **HoldMotion (0.08)**.
6. Sink → **SinkDownMotion (0.05)**.

### 7-3. wantClimbDown 분기 (L1028-1053)
- BottomHold + !feet.IsIndependentlyRelevant → **HoldMotion (0.08)**.
- handsClimbing.IsRelevant + feet 종류 → ClimbDownMotion (0.01) 또는 SinkDownMotion (0.05).

### 7-4. setShouldClimbSpeed → setOnlyShouldClimbSpeed (L1500-1551)
```java
private void setOnlyShouldClimbSpeed(double value) {
    isClimbing = true;
    if (climbIntoCount > 0) value = HoldMotion;
    if (value != HoldMotion) {
        float factor = getCombinedSpeedFactor();  // 1F default
        if (isFast) factor *= sprintFactor;       // 1.5
        if (isFreeBaseClimb && value == MediumUpMotion)
            switch (getOnLadder(...)) { case 1: factor *= freeOneLadderClimbUpSpeedFactor;
                                        case 2: factor *= freeBothLadderClimbUpSpeedFactor; }
        if (value > HoldMotion)
            value = (value - HoldMotion) * freeClimbingUpSpeedFactor * factor + HoldMotion;
        else
            value = HoldMotion - (HoldMotion - value) * freeClimbingDownSpeedFactor * factor;
        if (hasClimbCrawlGap && isClimbCrawling && value > HoldMotion)
            value = min(CatchCrawlGapMotion (0.17), value);
    } else {
        isClimbingStill = true;
    }
    boolean relevant = value < 0 || value > motionY;
    if (relevant) motionY = value;
    isClimbJumping = !relevant && !isClimbHolding;
}
```

---

## 8. 천장 클라이밍 (`handleCeilingClimbing` L1112-1174)
- jgap > 1.2 → motionY = **0.12**.
- jgap > 1.115 → **0.08**.
- 그 외 → **0.04**.
- supportsCeilingClimbing 검사 (iron bars + closed trapdoor).

---

## 9. setLandMotions (`L1176-1182`)
```java
private void setLandMotions(float horizontalDamping) {
    motionY -= 0.08000000000000002D;   // gravity
    motionY *= 0.98000001907348633D;   // vertical drag
    motionX *= horizontalDamping;       // 0.91 (공중) or block.slipperiness * 0.91 (땅)
    motionZ *= horizontalDamping;
}
```

---

## 10. 점프 motion (`tryJump` D-9 ~ D-12)

### D-9 vanilla 분기 L2047-2062 (sm.vanilla()=true)
```java
verticalMotion = 0.41999998688697815D;  // = vanilla jump
if (jumpBoost) verticalMotion += (level + 1) * 0.1F;
if (sprint) {
    motionX -= sin(yaw * π/180) * 0.2F;  // vanilla sprint horizontal boost
    motionZ += cos(yaw * π/180) * 0.2F;
}
```

### D-12 SM 분기 L2097-2110 (sm.vanilla()=false, horizontalMotion>0)
```java
absMotionX = abs(motionX) * horizontalJumpFactor;  // = 2 (sprintJumpHorizontalFactor)
absMotionZ = abs(motionZ) * horizontalJumpFactor;
if (maxHorizontalMotion != null) {
    absMotionX = min(absMotionX, maxHorizontalMotion * (horizontalJumpFactor * (abs(motionX)/horizontalMotion)));
    ...
}
motionX = sign(motionX) * absMotionX;
motionZ = sign(motionZ) * absMotionZ;
```
- `horizontalJumpFactor = jumpHorizontalFactor (1) * sprintJumpHorizontalFactor (2) = **2**`.
- `maxHorizontalMotion = 0.117852041920949F * sprintFactor (1.5) * combinedFactor (1) = **0.1768**`.
- `absMotionX = abs(motionX) * 2`, clamp `min(..., 0.1768 * 2 * (motionX/horizontalMotion)) = 0.353 * (motionX/horizontalMotion)`.
- 결과: motion 약 2배 boost.

= **SM 모드 sprint+jump 시 motionX/Z 2배 boost** (clamp 0.176 × 2). vanilla = +0.2 add. **SM 더 큰 boost**.

---

## 11. moveFlying (`SmartMovingBase.java:55-93`)

```java
protected void moveFlying(float moveUpward, float moveStrafing, float moveForward, float speedFactor, boolean treeDimensional) {
    // 1. yaw 기반 horizontal 분해
    float total = sqrt(strafe^2 + forward^2);
    if (total >= 0.01F) {
        if (total < 1.0F) total = 1.0F;
        moveStrafingFactor = strafe / total;
        moveForwardFactor = forward / total;
        sin = sin(yaw * π/180);
        cos = cos(yaw * π/180);
        diffMotionXStrafing = moveStrafingFactor * cos;
        diffMotionXForward = -moveForwardFactor * sin;
        diffMotionZStrafing = moveStrafingFactor * sin;
        diffMotionZForward = moveForwardFactor * cos;
    }

    // 2. pitch 기반 vertical (treeDimensional=true)
    rotation = treeDimensional ? pitch / RadiantToAngle : 0;
    divingHorizontalFactor = cos(rotation);
    divingVerticalFactor = -sin(rotation) * sign(forward);

    // 3. 최종 합산
    diffMotionX = diffMotionXForward * divingHorizontalFactor + diffMotionXStrafing;
    diffMotionY = sqrt(diffMotionXForward^2 + diffMotionZForward^2) * divingVerticalFactor + moveUpward;
    diffMotionZ = diffMotionZForward * divingHorizontalFactor + diffMotionZStrafing;

    // 4. 비표준 정규화 + speedFactor 곱
    total = sqrt(sqrt(diffMotionX^2 + diffMotionZ^2) + diffMotionY^2);
    if (total > 0.01F) {
        factor = speedFactor / total;
        motionX += diffMotionX * factor;  // ★ ADD
        motionY += diffMotionY * factor;
        motionZ += diffMotionZ * factor;
    }
}
```

= **motion 에 ADD** (set 아님). speedFactor / total 곱. 비행 시 사용.

---

## 12. 정리 — 우리 매핑이 빠진 부분

| 항목 | 원본 | 1.21.1 매핑 |
|------|------|-------------|
| Land sprint speedFactor 매핑 | rawSpeed * speedFactor (= isFast 1.5 또는 run 1.3) | **`sm_getMovementSpeed` 가 vanilla * smFactor 적용**. 이 식이 원본과 동등하려면 정확히 검증 필요 |
| 공중 sprint speedFactor | jumpMovementFactor / 1.3 * speedFactor (= isFast 1.5/1.3=1.154 → 0.0231) | **`sm_getOffGroundSpeed` 비활성** 상태 (디버깅) |
| 점프 motion 2배 boost (D-12) | sprintJumpHorizontalFactor 2F | SmartMovingJumper 매핑 정확 |
| 비행 식 | speedFactor * 0.05 * flyingSpeedFactor | SmartMovingFlyer 매핑 정확 (사용자 OK) |

**사용자 보고 핵심**:
1. 땅 sprint vanilla 보다 빠름 (원본 동작).
2. 공중 (점프 후) 엄청 빨라짐 (가속 버그).

**원인 추정**:
- 땅: 우리 `sm_getMovementSpeed` 의 `smFactor /= 1.3F` 가 vanilla sprint 1.3x 정상화 → 사용자 인지에 "느림". 원본 식 검증 필요.
- 공중: `sm_getOffGroundSpeed` + 점프 D-12 boost 이중 적용 가능. 정확한 매핑 식 필요.

---

## 13. 우리 매핑 다음 단계

각 상태별 final 식을 원본과 정확히 매핑:
- **Land onGround**: vanilla `getMovementSpeed(slip)` 식이 원본 `rawSpeed * speedFactor` 와 동등하도록 매핑.
- **Land 공중**: vanilla `getOffGroundSpeed()` 식이 원본 `jumpMovementFactor / 1.3 * speedFactor` 와 동등하도록.
- **점프 시점**: D-9 (vanilla) vs D-12 (SM) 분기 정확.
- **비행**: 이미 정상 (사용자 OK).
- **수영/lava**: 별도 매핑.
