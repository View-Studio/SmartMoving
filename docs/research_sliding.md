# Sliding 기능 리서치 (1.7.10 → 1.21.1 1:1 매핑)

> 범위: 슬라이딩 **기능**(state/조건/물리/네트워크). **애니메이션/모델 분기는 별도 단계**에서 수행.
>
> 원본 경로: `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\`
> 우리 경로: `C:\Users\user\IdeaProjects\SmartMoving\src\...`

---

## 1. 원본 라인 전수 (1.7.10) — 기능 영역만

### 1-1. 필드 선언 — `SmartMoving.java`

| 라인 | 코드 | 비고 |
|------|------|------|
| L41 | `public boolean isRopeSliding;` | ZipLine plugin 의존 (1.21.1 에선 ZipLine mod 부재 — 필드만 유지) |
| L49 | `public boolean isSliding;` | 슬라이딩 메인 state |
| L59 | `private float spawnSlindingParticle;` | 파티클 누적 타이머 (오타 그대로) |

### 1-2. 파티클 — `SmartMoving.java`

```java
// L78-L109
protected void spawnParticles(Minecraft mc, double pmx, double pmz)
{
    float horizontalSpeedSquare = 0;
    if(isSliding || isSwimming)                                              // L81
        horizontalSpeedSquare = (float)(pmx * pmx + pmz * pmz);

    if(isSliding)                                                            // L84
    {
        int i = MathHelper.floor_double(sp.posX);
        int j = MathHelper.floor_double(sp.boundingBox.minY - 0.1F);
        int k = MathHelper.floor_double(sp.posZ);
        Block block = sp.worldObj.getBlock(i, j, k);
        if(block != null)
        {
            double posY    = sp.boundingBox.minY + 0.1D;
            double motionX = -pmx * 4D;
            double motionY = 1.5D;
            double motionZ = -pmz * 4D;

            spawnSlindingParticle += horizontalSpeedSquare;                  // L97

            float maxSpawnSlindingParticle = Config._slideParticlePeriodFactor.value * 0.1F;  // L99
            while(spawnSlindingParticle > maxSpawnSlindingParticle)
            {
                double posX = sp.posX + getSpawnOffset();
                double posZ = sp.posZ + getSpawnOffset();
                int metaData = sp.worldObj.getBlockMetadata(i, j, k);
                sp.worldObj.spawnParticle("blockcrack_" + Block.getIdFromBlock(block) + "_" + metaData,
                                          posX, posY, posZ, motionX, motionY, motionZ);
                spawnSlindingParticle -= maxSpawnSlindingParticle;
            }
        }
    }
    // (isSwimming 분기 생략 — 별도 영역)
}
```

### 1-3. 자기 플레이어 메인 로직 — `SmartMovingSelf.java`

#### (a) 물 경계 — L416-L434

```java
dippingDepth = (float)playerSwimWaterBorder;
float playerCrawlWaterBorder = dippingDepth + wasHeightOffset;
if((isCrawling || isSliding) && playerCrawlWaterBorder < SwimCrawlWaterMaxBorder)  // L418
    if(playerCrawlWaterBorder < SwimCrawlWaterTopBorder) {
        // continue crawling in shallow water
        setHeightOffset(wasHeightOffset);
        handleSwimmingRejected = true;
    } else {
        // from crawling in shallow water to swimming/diving
        if(wantShallowSwim)
            move(0, 0.1, 0, true);
        isCrawling = false;
        isDiving   = false;
        isSwimming = true;
        isDipping  = false;
    }
```

#### (b) 지상 마찰 + strafe 강제 회전 — L701-L755 (핵심 물리)

```java
else if(!isSliding)                                                          // L701
{
    if(isHeadJumping)
        speedFactor *= Config._headJumpControlFactor.value;
    else if(Config.enabled && !sp.onGround && !sp.capabilities.isFlying && !isFlying)
        speedFactor *= Config._jumpControlFactor.value;

    float f3 = 0.1627714F / (horizontalDamping * horizontalDamping * horizontalDamping);
    float rawSpeed = sp.onGround ? 0.1f * f3
                                 : sp.jumpMovementFactor / (sp.isSprinting() && !sp.capabilities.isFlying ? 1.3F : 1F);
    if(!Config.enabled && sp.onGround)
        speedFactor *= (getLandMovementFactor() * 10f) / (sp.isSprinting() ? 1.3F : 1F);
    if(Config.isRunningEnabled() && isRunning() && !isFast)
        speedFactor *= !sp.capabilities.isFlying ? Config._runFactor.value : Config._runFactorLevitate.value;
    if(!sp.onGround) speedFactor /= getPotionSpeedFactor();
    sp.moveFlying(moveStrafing, moveForward, rawSpeed * speedFactor);
}

if(sp.onGround && (!isJumping || vanilla()))
{
    Block block = sp.worldObj.getBlock(MathHelper.floor_double(sp.posX),
                                       MathHelper.floor_double(sp.boundingBox.minY) - 1,
                                       MathHelper.floor_double(sp.posZ));
    if(block != null)
    {
        float slipperiness = block.slipperiness;
        if(isSliding)                                                        // L727
        {
            // (1) 감쇠 공식 — 0.98 곱
            horizontalDamping = 1F / (((1F / slipperiness) - 1F) / 25F * Config._slideSlipperinessFactor.value + 1F) * 0.98F;  // L729

            // (2) ★★ strafe 강제 회전 ★★ — 좌우 입력으로 슬라이딩 방향 컨트롤
            if(moveStrafing != 0 && Config._slideControlDegrees.value > 0)   // L730
            {
                double angle = -Math.atan(sp.motionX / sp.motionZ);
                if (!Double.isNaN(angle))
                {
                    if(sp.motionZ < 0)
                        angle += Math.PI;

                    angle -= Config._slideControlDegrees.value / RadiantToAngle * Math.signum(moveStrafing);  // L738

                    double hMotion = Math.sqrt(sp.motionX * sp.motionX + sp.motionZ * sp.motionZ);
                    sp.motionX = hMotion * -Math.sin(angle);
                    sp.motionZ = hMotion *  Math.cos(angle);
                }
            }
        }
        else
            horizontalDamping = slipperiness * HorizontalAirDamping;
    }
    else
        horizontalDamping = HorizontalGroundDamping;
}
else if(isAerodynamic)
    horizontalDamping = HorizontalAirodynamicDamping;
else
    horizontalDamping = HorizontalAirDamping;
```

#### (c) handleClimbing 안 small/회전 — L918, L983-L987

```java
boolean isSmallClimbing = isCrawling || isSliding;                           // L918
if(isClimbCrawling || isCrawlClimbing || isSmallClimbing)
    jd += -1D;
// ... seekClimbGap 4 + 4 분기 ...

if(feetClimbing.IsRelevant() || handsClimbing.IsRelevant())
{
    if(wantClimbUp)
    {
        if(isSliding && handsClimbing.IsRelevant())                          // L983
        {
            isSliding = false;                                                // L985
            isCrawling = true;                                                // L986
        }
        // ...
    }
}
```

#### (d) 걸음 사운드 보호 — L1578-L1606

```java
// beforeMoveEntity (L1578)
if(isSliding || isCrawling)
{
    beforeDistanceWalkedModified = sp.distanceWalkedModified;
    sp.distanceWalkedModified    = Float.MIN_VALUE;
}

// afterMoveEntity (L1605)
if(isSliding || isCrawling)
    sp.distanceWalkedModified = beforeDistanceWalkedModified;
```

목적: 슬라이딩/크롤 이동에서 vanilla 걸음 사운드 + 거리 통계 발화 차단.

#### (e) WallSlide 점프 타입 — L1946-L2006

```java
public void handleWallJumping()
{
    if(!wantWallJumping || Double.isNaN(horizontalCollisionAngle))
        return;

    int jumpType;
    if(grabButton.Pressed) {
        if(sp.fallDistance > Config._wallHeadJumpFallMaximumDistance.value) return;
        jumpType = wasCollidedHorizontally ? Config.WallHeadSlide : Config.WallHead;  // L1956
    } else {
        if(sp.fallDistance > Config._wallUpJumpFallMaximumDistance.value) return;
        jumpType = wasCollidedHorizontally ? Config.WallUpSlide   : Config.WallUp;    // L1962
    }
    // ... angle 계산 ...
    if(tryJump(jumpType, null, null, jumpAngle)) {
        continueWallJumping = !isHeadJumping;
        sp.isCollidedHorizontally = false;
        sp.rotationYaw = jumpAngle;
        onStartWallJump(jumpAngle);
    }
}

public boolean tryJump(int type, Boolean inWaterOrNull, Boolean isRunningOrNull, Float angle)
{
    boolean noVertical = false;
    if(type == Config.WallUpSlide || type == Config.WallHeadSlide)            // L2002
    {
        type = type == Config.WallUpSlide ? Config.WallUp : Config.WallHead;  // L2004
        noVertical = true;                                                    // L2005
    }
    // ...
}
```

WallUpSlide/WallHeadSlide = 수평 충돌 시 vertical 점프 차단(noVertical=true) + WallUp/WallHead 으로 강등.

#### (f) 비행/낙하 → 슬라이딩 전환 (toSlidingOrCrawling) — L2165-L2229

```java
private void standupIfPossible()                                              // L2165
{
    if(heightOffset >= 0) return;
    double gapUnderneight = getGapUnderneight();
    boolean groundClose = gapUnderneight < 1D;
    if(!groundClose)
        resetHeightOffset();
    else {
        double gapOverneight = groundClose ? getGapOverneight() : -1D;
        boolean standUpPossible = gapUnderneight + gapOverneight >= 1D;
        if(standUpPossible)  standUp(gapUnderneight);
        else                 toSlidingOrCrawling(gapUnderneight);              // L2182
    }
}

private void standupIfPossible(boolean tryLanding, boolean restoreFromFlying) // L2186
{
    if(heightOffset >= 0) return;
    double gapUnderneight = getGapUnderneight();
    boolean groundClose = gapUnderneight < 1D;
    double gapOverneight = groundClose ? getGapOverneight() : -1D;
    boolean standUpPossible = gapUnderneight + gapOverneight >= 1D;

    if(tryLanding && groundClose && standUpPossible) {
        isFlying = false;
        sp.capabilities.isFlying = false;
        restoreFromFlying = true;
    }
    if(!restoreFromFlying) return;

    if(!groundClose && !sneakButton.Pressed)             resetHeightOffset();
    else if(standUpPossible && !(sneakButton.Pressed && grabButton.Pressed))  standUp(gapUnderneight);
    else                                                  toSlidingOrCrawling(gapUnderneight);  // L2211
}

private void toSlidingOrCrawling(double gapUnderneight)                       // L2222
{
    move(0, (-gapUnderneight), 0, true);                                      // L2224

    if(Config.isSlidingEnabled() && (grabButton.Pressed || wasHeadJumping))   // L2226
        isSliding = true;                                                      // L2227
    else
        wasCrawling = toCrawling();                                            // L2229
}
```

#### (g) reset (`resetState`) — L2272-L2305

```java
private void resetState()
{
    resetHeightOffset();
    this.isSlow = false;
    this.isFast = false;
    this.isClimbing = false;
    this.isHandsVineClimbing = false;
    this.isFeetVineClimbing  = false;
    this.isClimbJumping      = false;
    this.isClimbBackJumping  = false;
    this.isWallJumping       = false;
    this.isClimbCrawling     = false;
    this.isCrawlClimbing     = false;
    this.isCeilingClimbing   = false;
    this.isRopeSliding       = false;                                          // L2288
    this.isDipping           = false;
    this.isSwimming          = false;
    this.isDiving            = false;
    this.isLevitating        = false;
    this.isHeadJumping       = false;
    this.isCrawling          = false;
    this.isSliding           = false;                                          // L2296
    this.isFlying            = false;
    this.actualHandsClimbType = 0;
    this.actualFeetClimbType  = 0;
    this.angleJumpType        = 0;
    this.heightOffset         = 0;
}
```

#### (h) 점프 입력 차단 — L2358-L2371

```java
if(!startSleeping)
{
    isp.localUpdateEntityActionState();
    isp.setMoveStrafingField(Math.signum(esp.movementInput.moveStrafe));
    isp.setMoveForwardField (Math.signum(esp.movementInput.moveForward));
    isp.setIsJumpingField(
        esp.movementInput.jump && !isCrawling && !isSliding &&                 // L2367
        !(Config.isHeadJumpingEnabled() && grabButton.Pressed && sp.isSprinting()) &&
        !(Config.isJumpChargingEnabled() && wouldIsSneaking && sp.onGround && isStanding) &&
        !blockJumpTillButtonRelease);
}
```

#### (i) climb / sneak / sprint 영향 — L2474, L2493, L2579, L2597, L2686, L3236

```java
boolean wouldWantClimb =
    (grabButton.Pressed || (isClimbHolding && sneakButton.Pressed) || ...) &&
    (!isSliding || grabButton.Pressed && esp.movementInput.moveForward > 0F) &&  // L2474
    !isHeadJumping && !wantCrawlNotClimb && !disabled;

wantClimbUp =
    wantClimb &&
    esp.movementInput.moveForward > 0F || (isVineAnyClimbing && jumpButton.Pressed && !(sneakButton.Pressed && isFacedToSolidVine))&&
    (!isCrawling || sp.isCollidedHorizontally) &&
    (!isSliding  || sp.isCollidedHorizontally);                                 // L2493

boolean wouldWantSneak =
    !isFlying &&
    !isSliding &&                                                               // L2579
    !isHeadJumping && ...

wantSprint =
    Config.isSprintingEnabled() &&
    !isSliding &&                                                               // L2597
    sprintButton.Pressed && (...)
    && !disabled;

boolean standing = sp.onGround && !isSliding && !isCrawling;                    // L2686

public boolean isStandupSprintingOrRunning() {
    return (isFast || sp.isSprinting()) && sp.onGround && !isSliding && !isCrawling;  // L3236
}
```

#### (j) 슬라이딩 진입 / 해제 핵심 — L2546-L2574 (★★ 가장 중요)

```java
// (1) ★ SlideToHeadJumping (살짝 낙하) — L2546-L2551
if(isSliding && sp.fallDistance > SlideToHeadJumpingFallDistance)              // L2546 (= 0.05F)
{
    isSliding     = false;
    isHeadJumping = true;
    isAerodynamic = true;
}

// (2) ★ 직접 진입 6-AND — L2553-L2561
if(Config.isSlidingEnabled() && grabButton.Pressed
   && (isGroundSprinting || (wasRunning && !isRunning && sp.onGround))
   && !isCrawling && sneakButton.StartPressed && !isDipping)                    // L2553
{
    setHeightOffset(-1);                                                        // L2555
    move(0, (-1D), 0, true);                                                    // L2556
    tryJump(Config.SlideDown, false, wasRunning, null);                         // L2557
    isSliding     = true;                                                       // L2558
    isHeadJumping = false;                                                      // L2559
    isAerodynamic = false;                                                      // L2560
}

// (3) ★★★ sneak 떼기 / 정지 종료 — L2563-L2567 ★★★
if(isSliding && (!sneakButton.Pressed                                           // L2563
                || horizontalSpeedSquare < Config._slidingSpeedStopFactor.value * 0.01))
{
    isSliding   = false;                                                        // L2565
    wasCrawling = toCrawling();                                                 // L2566 — 즉시 크롤 진입
}

// (4) ★ 큰 낙하 → crawl 준비 — L2569-L2574
if(isSliding && sp.fallDistance > Config._fallingDistanceMinimum.value)         // L2569 (= 3F)
{
    isSliding   = false;
    wasCrawling = true;                                                         // L2572
    isCrawling  = false;                                                        // L2573
}
```

#### (k) isRopeSliding 매 틱 갱신 + 전송 — L2966, L3105-L3158

```java
isRopeSliding = isRopeSliding();                                                // L2966
// ...
public void addToSendQueue() {
    // ...
    state |= isp.localIsSneaking() ? 1 : 0;     state <<= 1;
    state |= isRopeSliding ? 1 : 0;              state <<= 1;                   // L3116
    state |= isWallJumping ? 1 : 0;              state <<= 1;
    state |= isFast ? 1 : 0;                     state <<= 1;
    state |= isSlow ? 1 : 0;                     state <<= 1;
    state |= isClimbBackJumping ? 1 : 0;         state <<= 1;
    state |= isClimbJumping ? 1 : 0;             state <<= 1;
    state |= isHandsVineClimbing ? 1 : 0;        state <<= 1;
    state |= isFeetVineClimbing  ? 1 : 0;        state <<= 3;
    state |= angleJumpType;                      state <<= 1;
    state |= isSliding ? 1 : 0;                  state <<= 1;                   // L3143
    state |= isHeadJumping ? 1 : 0;              state <<= 1;
    state |= isLevitating ? 1 : 0;               state <<= 1;
    state |= isCeilingClimbing ? 1 : 0;          state <<= 1;
    // ...
}

public static boolean isRopeSliding()                                           // L3312
{
    return onZipLine != null && Reflect.GetField(onZipLine, null) != null;
}
```

### 1-4. 다른 플레이어 패킷 수신 — `SmartMovingOther.java`

```java
public void processStatePacket(long state)
{
    actualFeetClimbType  = (int)(state & 15);    state >>>= 4;
    actualHandsClimbType = (int)(state & 15);    state >>>= 4;
    _isJumping       = (state & 1) != 0;         state >>>= 1;
    isDiving         = (state & 1) != 0;         state >>>= 1;
    isDipping        = (state & 1) != 0;         state >>>= 1;
    isSwimming       = (state & 1) != 0;         state >>>= 1;
    isCrawlClimbing  = (state & 1) != 0;         state >>>= 1;
    isCrawling       = (state & 1) != 0;         state >>>= 1;
    isClimbing       = (state & 1) != 0;         state >>>= 1;
    boolean isSmall  = (state & 1) != 0;
    heightOffset = isSmall ? -1 : 0;
    sp.height    = 1.8F + heightOffset;          state >>>= 1;
    _doFallingAnimation = (state & 1) != 0;      state >>>= 1;
    _doFlyingAnimation  = (state & 1) != 0;      state >>>= 1;
    isCeilingClimbing = (state & 1) != 0;        state >>>= 1;
    isLevitating      = (state & 1) != 0;        state >>>= 1;
    isHeadJumping     = (state & 1) != 0;        state >>>= 1;
    isSliding         = (state & 1) != 0;        state >>>= 1;                  // L80
    angleJumpType     = (int)(state & 7);        state >>>= 3;
    isFeetVineClimbing  = (state & 1) != 0;      state >>>= 1;
    isHandsVineClimbing = (state & 1) != 0;      state >>>= 1;
    isClimbJumping      = (state & 1) != 0;      state >>>= 1;
    boolean wasClimbBackJumping = isClimbBackJumping;
    isClimbBackJumping  = (state & 1) != 0;
    if(!wasClimbBackJumping && isClimbBackJumping) onStartClimbBackJump();
    state >>>= 1;
    isSlow            = (state & 1) != 0;        state >>>= 1;
    isFast            = (state & 1) != 0;        state >>>= 1;
    boolean wasWallJumping = isWallJumping;
    isWallJumping     = (state & 1) != 0;
    if(!wasWallJumping && isWallJumping) onStartWallJump(null);
    state >>>= 1;
    isRopeSliding     = (state & 1) != 0;                                       // L113
}
```

### 1-5. 상수 / 설정

| 위치 | 라인 | 값 | 의미 |
|------|------|------|------|
| `SmartMovingContext` | L51 | `SlideToHeadJumpingFallDistance = 0.05F` | (j)-(1) 임계값 |
| `SmartMovingClientConfig` | L182 | `SlideDown = 4` | tryJump type |
| `SmartMovingClientConfig` | L191-L192 | `WallUpSlide = 13`, `WallHeadSlide = 14` | tryJump type |
| `SmartMovingClientConfig` | L142-L144 | `isSlidingEnabled() = _slide.value && enabled` | 활성 헬퍼 |
| `SmartMovingConfig` | L203 | `_slide` Property (key `move.slide`) | 활성 토글 |
| `SmartMovingConfig` | L204 | `_slideControlDegrees` (default 1F, deg/tick) | strafe 회전량 |
| `SmartMovingConfig` | L205 | `_slideSlipperinessFactor` (default 1F) | 마찰 계수 |
| `SmartMovingConfig` | L206 | `_slidingSpeedStopFactor` (default 1F) | 정지 속도² 임계 |
| `SmartMovingConfig` | L207 | `_slideParticlePeriodFactor` (default 0.5F) | 파티클 주기 |
| `SmartMovingConfig` | L221 | `_fallingDistanceMinimum` (default 3F) | 큰 낙하 임계 |
| `SmartMovingConfig` | L392-L394 | `_jumpSlideExhaustion` 외 2개 | jump 시 피로 |

### 1-6. 키바인딩 토글 — `SmartMovingComm.java`

```java
processBlockCode(codes, "§6", Options._slide);                                  // L158
```

서버 → 클라 강제 비활성 패킷.

---

## 2. 1.21.1 현재 매핑 상태

### 2-1. 매핑 위치 요약

| 원본 영역 | 1.21.1 위치 | 상태 |
|----------|-------------|------|
| `isSliding` 필드 | `SmartMovingClientState.java` L223 | ✅ |
| `isRopeSliding` 필드 | `SmartMovingClientState.java` L393 | ✅ (state만 유지, 1.21.1 ZipLine 없음) |
| `spawnSlindingParticle` 누적 | `SmartMovingClientState.java` L675 | ✅ |
| 파티클 생성 (L78-L109) | `SmartMovingSlider.spawnSlidingParticle` | ✅ (BlockState 매핑) |
| 물 경계 (L418) | `SmartMovingSwimmer.java` L270 | ✅ |
| 마찰 공식 (L729) | `SmartMovingSlider.handleSliding` damping | ✅ |
| **strafe 강제 회전 (L730-L744)** | **— (없음)** | ❌ **누락** |
| handleClimbing isSmallClimbing (L918) | `SmartMovingClimber.java` L535 | ✅ |
| handleClimbing isSliding 해제 (L983-L987) | `SmartMovingClimber.java` L801 | ✅ |
| **distanceWalkedModified 보호 (L1578, L1605)** | **— (없음)** | ❓ **확인 필요 (1.21.1 vanilla 거리 변수명 차이)** |
| WallUpSlide/WallHeadSlide jumpType (L1956-L1962) | `SmartMovingJumper.java` L612-L624 | ✅ |
| WallUpSlide/WallHeadSlide noVertical (L2002-L2006) | `SmartMovingJumper.java` L162-L166 | ✅ |
| `toSlidingOrCrawling` (L2222-L2229) | `SmartMovingClientState.toSlidingOrCrawling` L3323-L3334 | ✅ |
| `standupIfPossible` 무인자 (L2165) | `SmartMovingClientState.standupIfPossible(player)` L3351 | ✅ |
| `standupIfPossible` 비행 (L2186) | `SmartMovingClientState.standupIfPossible(player, tryLanding, restore)` L3396 | ✅ |
| reset isRopeSliding (L2288) | `SmartMovingClientState.java` L2661 | ✅ |
| reset isSliding (L2296) | `SmartMovingClientState.java` L2688 | ✅ |
| 점프 입력 차단 (L2367) | `MixinClientPlayerEntity.java` L103-L104 | ✅ |
| wouldWantClimb `!isSliding`(L2474) | `SmartMovingClientState.java` L1528 | ✅ |
| wantClimbUp `!isSliding`(L2493) | `SmartMovingClientState.java` L1575 | ✅ |
| **SlideToHeadJumping (L2546-L2551)** | `SmartMovingClientState.java` L1980-L1986 | ✅ |
| **직접 진입 6-AND (L2553-L2561)** | `SmartMovingClientState.java` L1930-L1947 | ✅ |
| **★ sneak/속도 종료 (L2563-L2567) ★** | **부분 — `SmartMovingSlider.handleSliding` L75-L77** | ❌ **사실상 누락** (sneak 떼기/단위/`toCrawling()` 모두 빠짐) |
| 큰 낙하 → crawl (L2569-L2574) | `SmartMovingClientState.java` L1994-L1998 | ✅ |
| wouldWantSneak `!isSliding`(L2579) | `SmartMovingClientState.java` L1442 | ✅ |
| wantSprint `!isSliding`(L2597) | `SmartMovingClientState.java` L1477 | ✅ |
| standing (L2686) | `SmartMovingClientState.java` L1660 | ✅ |
| isRopeSliding 매 틱 갱신 (L2966) | — | ⚠️ (ZipLine 없음 — false 고정으로 OK) |
| 패킷 송신 isRopeSliding (L3116) | `SmartMovingState.java` bit 32 + `SmartMovingClientState.java` L3793 | ✅ |
| 패킷 송신 isSliding (L3143) | `SmartMovingState.java` bit 21 + `SmartMovingClientState.java` L3784 | ✅ |
| 패킷 수신 isSliding (Other.L80) | `SmartMovingState.java` L117 | ✅ |
| 패킷 수신 isRopeSliding (Other.L113) | `SmartMovingState.java` L126 | ✅ |
| `isStandupSprintingOrRunning()` (L3236) | `SmartMovingClientState.java` L3134-L3143 | ✅ |
| 키바인딩 토글 `§6` (Comm.L158) | `SmartMovingClient.java` L211 | ✅ |
| Config 4종 필드 | `SmartMovingConfig.java` L775-L780, L1681-L1683, L1848-L1850 | ✅ |

### 2-2. `SmartMovingSlider.java` (현재 구현 — 발견 누락)

```java
public static boolean handleSliding(ClientPlayerEntity player, SmartMovingClientState sm) {
    if (!sm.isSliding) return false;
    SmartMovingConfig cfg = SmartMovingConfig.Config;
    if (!cfg.slide) {
        sm.isSliding = false;
        return false;
    }

    Vec3d vel = player.getVelocity();

    BlockState below = player.getWorld().getBlockState(player.getSteppingPos());
    float slip = below.getBlock().getSlipperiness();

    // 원본 L729 — ✅ 1:1
    float damping = 1F / (((1F / slip - 1F) / 25F) * cfg.slideSlipperinessFactor + 1F) * 0.98F;

    double newVx = vel.x * damping;
    double newVz = vel.z * damping;
    double newVy = vel.y - player.getAttributeValue(EntityAttributes.GENERIC_GRAVITY);
    newVy *= 0.98D;

    player.setVelocity(newVx, newVy, newVz);
    player.move(MovementType.SELF, player.getVelocity());

    // ❌ 원본 L730-L744 strafe 강제 회전 — 누락
    // ❌ 원본 L727 horizontalDamping 적용 후 vanilla travel 의 0.21600002F/slip³ 가속 — 단순 vel*damping 로 대체

    spawnSlidingParticle(player, sm, new Vec3d(newVx, 0, newVz));

    // ❌ 원본 L2563 종료 조건 부분 매핑 (sneak 누락 + 단위 차이)
    double horizontalSpeed = Math.sqrt(newVx * newVx + newVz * newVz);   // ← speed (linear)
    if (horizontalSpeed < cfg.slidingSpeedStopFactor) {                    // ← 원본은 speedSquare * 0.01
        sm.isSliding = false;                                              // ← 원본은 wasCrawling = toCrawling() 호출
    }

    return true;
}
```

---

## 3. 차이/누락 분석 (수정 대상)

### 3-1. ★ 핵심 누락 #1 — 종료 조건 (원본 L2563-L2567)

**원본:**
```java
if (isSliding && (!sneakButton.Pressed
                  || horizontalSpeedSquare < Config._slidingSpeedStopFactor.value * 0.01))
{
    isSliding   = false;
    wasCrawling = toCrawling();   // ★ 슬라이딩 종료 즉시 크롤 진입 후 다음 tick state 결정
}
```

**현재 매핑 (`SmartMovingSlider`):**
```java
double horizontalSpeed = Math.sqrt(newVx*newVx + newVz*newVz);
if (horizontalSpeed < cfg.slidingSpeedStopFactor) {
    sm.isSliding = false;
}
```

**3가지 차이:**
1. **sneak 떼기 종료 분기 누락** — 사용자가 sneak 키 떼면 슬라이딩 즉시 종료해야 하는데, 현재는 속도가 작아질 때까지 계속.
2. **단위 차이** — 원본은 `speedSquare < stopFactor * 0.01` (속도 제곱), 우리는 `speed < stopFactor` (속도 절대값). `stopFactor=1F` 기본값에서:
   - 원본: speedSquare < 0.01 → speed < 0.1 m/tick
   - 우리: speed < 1 → speed < 1 m/tick (10× 일찍 종료)
3. **`wasCrawling = toCrawling()` 호출 누락** — 종료 시 즉시 크롤로 전환되어야 하는데, 현재는 단순 `isSliding = false` 만. 사용자 경험상 "슬라이딩 → 정지 → 자동 크롤" 흐름이 끊김.

또한 원본은 이 분기가 `SmartMovingSelf.updateEntityActionState` (= 우리 `SmartMovingClientState` 메인 tick) 안에 있음. SmartMovingSlider 의 land motion 처리 안이 아님. 위치도 옮길 필요 있음.

### 3-2. ★ 핵심 누락 #2 — strafe 강제 회전 (원본 L730-L744)

**원본:**
```java
if (moveStrafing != 0 && Config._slideControlDegrees.value > 0)
{
    double angle = -Math.atan(motionX / motionZ);
    if (!Double.isNaN(angle))
    {
        if (motionZ < 0) angle += Math.PI;
        angle -= Config._slideControlDegrees.value / RadiantToAngle * Math.signum(moveStrafing);
        double hMotion = sqrt(motionX² + motionZ²);
        motionX = hMotion * -sin(angle);
        motionZ = hMotion *  cos(angle);
    }
}
```

**현재 매핑:** 없음. 슬라이딩 중 좌우 입력 → 방향 컨트롤 동작 안 됨.

`RadiantToAngle = 180/π` (rad↔deg 변환). `_slideControlDegrees` 기본 1F = tick당 1도 회전.

### 3-3. 단위 / 메서드 차이

| 항목 | 원본 1.7.10 | 1.21.1 매핑 (현재) | 비고 |
|------|------------|-------------------|------|
| `block.slipperiness` | float field | `Block.getSlipperiness()` | 변환 OK |
| `sp.distanceWalkedModified` | EntityPlayer field | `Entity.distanceTraveled` (1.21.1) | 보호 필요 시 매핑 |
| `setIsJumpingField(...)` | reflection | `MixinClientPlayerEntity` inject | OK |
| `tryJump(SlideDown, false, wasRunning, null)` | self method | `SmartMovingJumper.tryJump(SLIDE_DOWN, ...)` | OK |
| `Block.getIdFromBlock + metadata` | 파티클 stringId | `BlockStateParticleEffect(ParticleTypes.BLOCK, BlockState)` | OK |
| `RadiantToAngle = 57.29578F` | 정의됨 | `Math.toDegrees / Math.toRadians` 또는 동일 상수 | 추가 필요 |

### 3-4. distanceWalkedModified 보호 (원본 L1578, L1605)

원본은 슬라이딩/크롤 중 vanilla 걸음 사운드 + 거리 통계 발화 차단을 위해 매 tick `Float.MIN_VALUE` 로 강제 후 `move` 끝에 복원.

1.21.1 vanilla 의 대응 변수: `Entity.distanceTraveled` / `LivingEntity.distanceMoved` 등. **현재 매핑 누락 가능성** — 슬라이딩/크롤 중 vanilla 걸음 사운드가 들리는 회귀 여부 인게임 확인 필요. (원본 의도 = 슬라이딩 중 발자국 사운드 X)

> 이 항목은 크롤(엎드리기) 완결과 무관하게 별도 검토. 크롤 시스템 완결되어 있으므로 우선순위 낮음 — 인게임 확인 후 결정.

### 3-5. 부수 항목

- **`isRopeSliding` (ZipLine plugin)** — 1.21.1 ZipLine mod 부재. `false` 고정 (현재 reset 외 갱신 없음)이면 OK. 매핑 신규 작업 불필요.
- **`spawnSwimmingParticle` 의 `isSliding || isSwimming` 조건 (원본 L81)** — 현재 SmartMovingSlider 분기는 isSliding 만 처리. swimming 분기는 SmartMovingSwimmer 별도. 분리 OK.
- **`tryJump(SlideDown, false, wasRunning, null)` 호출 (원본 L2557 = 진입 시)** — SmartMovingClientState L1942 `SmartMovingJumper.tryJump(player, this, SmartMovingJumper.SLIDE_DOWN, false, wasRunning, null)` 매핑 ✅. SLIDE_DOWN 점프 본문 (속도/exhaustion) 의 정확성은 `SmartMovingJumper` 별도 확인 (이번 작업 범위 외).

---

## 4. 1대1 매핑 작업 항목 (요약)

### A. 즉시 수정 (기능 정확성)

1. **종료 조건 재배치 + sneak 분기 + speedSquare 단위 + `wasCrawling = toCrawling()` 호출** — `SmartMovingSlider.handleSliding` 의 종료 분기를 제거하고 `SmartMovingClientState` 메인 tick (직접 진입 분기 다음 위치, 원본 L2563 자리) 로 옮김.
2. **strafe 강제 회전 (원본 L730-L744)** — `SmartMovingSlider.handleSliding` 안 damping 적용 직후 motionX/Z 강제 회전 블록 추가.

### B. 검증 (회귀 확인)

3. **distanceWalkedModified (원본 L1578, L1605)** — 인게임 슬라이딩/크롤 중 vanilla 걸음 사운드 발화 확인. 회귀 시 매핑 필요.

### C. 보존 항목 (이미 매핑됨, 변경 X)

- 진입 6-AND, SlideToHeadJumping, 큰 낙하 → crawl, sneak/sprint/climb 영향, WallSlide jumpType, toSlidingOrCrawling, 패킷 송수신, reset, 점프 차단, 키바인딩 토글, isStandupSprintingOrRunning, 마찰 공식, 파티클 — 전부 ✅ (수정 금지).

---

## 5. 매핑 전제

- 슬라이딩 진입 직후 `setHeightOffset(-1) + move(0, -1, 0) + tryJump(SLIDE_DOWN)` 적용은 SmartMovingClientState 매핑 ✅.
- `SmartMovingSlider.handleSliding` 은 `MixinLivingEntityClient` (sm_travel_client 분기) 에서 vanilla travel 대체로 호출됨 ✅.
- 종료 조건을 SmartMovingClientState 로 옮기면 `MixinLivingEntityClient` 호출 흐름 영향 X (Slider 는 land motion + 파티클 + strafe 회전만 담당).
- `wasCrawling = toCrawling()` 호출 시 `toCrawling()` 메서드는 SmartMovingClientState 에 이미 존재 (다른 분기에서 호출 ✅).

---

## 6. 인게임 검증 시나리오 (체크리스트 작업 후)

1. 평지 sprint + grab + sneak → 슬라이딩 시작 (✅ 현재 동작).
2. 슬라이딩 중 sneak 떼기 → 즉시 정지 + crawl 전환. **현재 미동작 — fix 후 재검증.**
3. 슬라이딩 중 좌/우 키 입력 → 방향 회전 (degrees-per-tick). **현재 미동작 — fix 후 재검증.**
4. 슬라이딩 중 충분히 감속 → 정지 + crawl 전환 (속도 임계 정확성).
5. 슬라이딩 중 살짝 낙하 (>0.05F) → 헤드점프 + 공기역학. (기존 ✅, 회귀 X 확인)
6. 슬라이딩 중 큰 낙하 (>3F) → 크롤 준비. (기존 ✅, 회귀 X 확인)
7. 슬라이딩 중 사다리/덩굴 grab → isSliding=false + isCrawling=true 전환. (기존 ✅, 회귀 X 확인)
8. 슬라이딩 중 점프 키 → 무시. (기존 ✅, 회귀 X 확인)
9. 슬라이딩 중 sprint 키 → 무시. (기존 ✅, 회귀 X 확인)
10. WallUpSlide/WallHeadSlide 점프 → noVertical 작동. (기존 ✅, 회귀 X 확인)
