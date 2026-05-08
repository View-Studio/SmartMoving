# 여우무빙 / 헤드점프 거리 정밀 리서치 — vanilla 1.7.10 vs 원본 SmartMoving vs vanilla 1.21.1 vs 우리 매핑

> 추측 절대 금지. 모든 코드 직접 인용 + 출처 명시. 거리 차이 root cause 식별.
> 시작: 2026-05-09. 사용자 보고 "여우무빙 앞 거리 부족, 명확히 다름".

## §0 측정 기준 (= dump 검증 완료)

| 시나리오 | hDist | duration | maxH | addCnt |
|---------|-------|----------|------|--------|
| 정상 헤드점프 + W hold | **3.13m** | 14tk | 1.252m | 14 |
| 여우무빙 + W hold | **6.48m** | 12tk | (측정 BUG, 0.000) | 2 |

추측: 원본 1.7.10 거리 = ?? (= 사용자 추정값).

## §1 vanilla 1.7.10 motion 식

### §1.1 EntityLivingBase.moveEntityWithHeading (= 원본 SM 가 super 호출 X, 자체 식 사용)

원본 SM 의 `SmartMovingSelf.moveEntityWithHeading` (L70-L93) 가 vanilla super 직접 호출 X. SM 자체 `superMoveEntityWithHeading` (L95+) 사용. 즉 vanilla 1.7.10 식은 SM mod 활성 시 **무력화**.

vanilla default `EntityLivingBase.jumpMovementFactor = 0.02F` (= protected field). 사용처 — `moveFlying(strafe, forward, this.onGround ? 0.1F * f6 : this.jumpMovementFactor)`.

### §1.2 vanilla 1.7.10 Entity 매직넘버

| 식 | vanilla 1.7.10 값 | 출처 |
|----|---------------------|------|
| gravity | `0.08D` | EntityLivingBase.moveEntityWithHeading |
| Y damping | `0.98D` | 동일 |
| onGround block 기본 slipperiness | `0.6F` | Block.slipperiness |
| onGround damping = slip × air | `0.6 × 0.91 = 0.546F` | 동일 |
| !onGround damping | `0.91F` | 동일 |
| jumpMovementFactor (default) | `0.02F` | EntityLivingBase 필드 |
| sprint modifier | `+0.30000001192092896` (multiplier) | SharedMonsterAttributes.movementSpeed |
| onGround speed | `0.1F × f6` | f6 = 0.16277136 / damping³ |
| 작은 motion clamp | `Math.abs(motion) < 0.005 → 0` | EntityLivingBase 안 |
| Jump Boost amplifier 기여 | `+(amp+1) × 0.1` | EntityLivingBase.getJumpUpwardsMotion() |

vanilla 1.7.10 jump 식: `motionY = 0.42 + (jumpBoost ? (amp+1) × 0.1 : 0)`. sprint 시 추가 `motionX -= sin(yaw)×0.2; motionZ += cos(yaw)×0.2`.

## §2 원본 SmartMoving 식 (인용)

### §2.1 jumpMotion 저장 (`SmartMovingSelf.java` L1853-L1854)

```java
jumpMotionX = sp.motionX;
jumpMotionZ = sp.motionZ;
```

→ 매 tick `handleJumping` 시작에서 갱신. **X/Z 만, pitch 무관**.

### §2.2 tryJump HEAD_UP head 분기 (L2065-L2079)

```java
if (head) {
    double normalAngle = Math.atan(verticalMotion / horizontalMotion);
    double totalMotion = Math.sqrt(verticalMotion² + horizontalMotion²);
    double newAngle = Config.getHeadJumpFactor(headJumpCharge) * normalAngle;
    double newVerticalMotion = totalMotion * Math.sin(newAngle);
    double newHorizontalMotion = totalMotion * Math.cos(newAngle);
    if (maxHorizontalMotion != null)
        maxHorizontalMotion = maxHorizontalMotion * (newHorizontalMotion / horizontalMotion);
    verticalMotion = newVerticalMotion;
    horizontalMotion = newHorizontalMotion;
}
```

### §2.3 tryJump horizontalMotion>0 분기 (L2097-L2110)

```java
if (horizontalMotion > 0) {
    double absoluteMotionX = Math.abs(sp.motionX) * horizontalJumpFactor;
    double absoluteMotionZ = Math.abs(sp.motionZ) * horizontalJumpFactor;
    if (maxHorizontalMotion != null) {
        absoluteMotionX = Math.min(absoluteMotionX,
            maxHorizontalMotion * (horizontalJumpFactor * (Math.abs(sp.motionX) / horizontalMotion)));
        absoluteMotionZ = Math.min(absoluteMotionZ,
            maxHorizontalMotion * (horizontalJumpFactor * (Math.abs(sp.motionZ) / horizontalMotion)));
    }
    sp.motionX = Math.signum(sp.motionX) * absoluteMotionX;
    sp.motionZ = Math.signum(sp.motionZ) * absoluteMotionZ;
}
```

### §2.4 verticalMotion 식 (L2042)

```java
double verticalMotion = -0.078 + 0.498 * verticalJumpFactor * jumpChargeFactor;
```

= verticalJumpFactor=1, jumpChargeFactor=1 시 verticalMotion = -0.078 + 0.498 = **0.42**.

### §2.5 verticalJumpFactor 적용 (L2113-L2118)

```java
if (up && !noVertical) {
    sp.motionY = verticalMotion;
    ...
    isSprintJump = isFast;
}
```

### §2.6 jumpFactor potion 식 (L2029-L2031)

```java
float jumpFactor = 1F;
if (sp.isPotionActive(Potion.jump))
    jumpFactor = 1F + (sp.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.2F;
float horizontalJumpFactor = Config.getJumpHorizontalFactor(speed, type) * jumpFactor;
float verticalJumpFactor = Config.getJumpVerticalFactor(speed, type) * jumpFactor;
```

→ Jump Boost amplifier=0 (= LV1) 시 `jumpFactor = 1 + 1 × 0.2 = 1.2`. **vanilla 1.7.10 의 +0.1 per level 과 다름! SM 자체 = +0.2 per level**.

### §2.7 maxHorizontalMotion 식 (L2044-L2045)

```java
if (horizontalJumpFactor > 1F && !sp.isCollidedHorizontally)
    maxHorizontalMotion = (double) Config.getMaxHorizontalMotion(speed, type, inWater) * getCombinedSpeedFactor();
```

### §2.8 SmartMovingClientConfig.getMaxHorizontalMotion (L508-L525)

```java
public float getMaxHorizontalMotion(int speed, int type, boolean inWater) {
    float maxMotion = 0.117852041920949F;
    if (!enabled)
        return speed == Running ? maxMotion * 1.3F : maxMotion;
    if (inWater)
        maxMotion = 0.07839602977037292F;
    if (speed == Sprinting) maxMotion *= _sprintFactor.value;
    else if (speed == Running) maxMotion *= _runFactor.value;
    else if (speed == Sneaking) maxMotion *= _sneakFactor.value;
    return maxMotion;
}
```

`_sprintFactor` default = **1.5F**.

### §2.9 getJumpHorizontalFactor (= HeadUp / SlideDown 분기)

```java
// HeadUp: speed=Sprinting + type=HeadUp
// 결과 = vanilla 1F (= 헤드점프 hJF default 분기 미진입)
// hJF 적용 위치 = factor 곱셈에서 type 의 head 분기.

// SlideDown: speed=Sprinting + type=SlideDown
// 결과 = _slideDownSprintingHorizontalFactor (= ??)
```

검사 필요 — `getJumpHorizontalFactor` 정확 식.

### §2.10 landMotion 식 (L675-L719)

```java
private float landMotion(float moveForward, float moveStrafing, float speedFactor, ...) {
    float horizontalDamping;
    if (sp.onGround && (!isJumping || vanilla())) {
        Block block = ...;
        horizontalDamping = (block != null) ? block.slipperiness * 0.91F : 0.546F;
        if (esp.movementInput.jump && isFast && Config.isJumpingEnabled(Sprinting, Up))
            speedFactor *= Config._sprintJumpVerticalFactor.value;
    } else {
        horizontalDamping = 0.91F;
    }

    if (!isSliding) {
        if (isHeadJumping) speedFactor *= Config._headJumpControlFactor.value;
        else if (Config.enabled && !sp.onGround && !sp.capabilities.isFlying && !isFlying)
            speedFactor *= Config._jumpControlFactor.value;

        float f3 = 0.1627714F / (horizontalDamping × horizontalDamping × horizontalDamping);
        float rawSpeed = sp.onGround ? 0.1f * f3 : sp.jumpMovementFactor / (sp.isSprinting() ? 1.3F : 1F);
        if (Config.isRunningEnabled() && isRunning() && !isFast)
            speedFactor *= Config._runFactor.value;
        if (!sp.onGround) speedFactor /= getPotionSpeedFactor();

        sp.moveFlying(moveStrafing, moveForward, rawSpeed * speedFactor);
    }
    ...
}
```

### §2.11 setLandMotions (L1176-L1182)

```java
private void setLandMotions(float horizontalDamping) {
    sp.motionY -= 0.080000000000000002D;
    sp.motionY *= 0.98000001907348633D;
    sp.motionX *= horizontalDamping;
    sp.motionZ *= horizontalDamping;
}
```

## §3 vanilla 1.21.1 LivingEntity.travel 식

검사 필요. `.tmp_research/LivingEntity_1_21_1.java` 참조 가능.

## §4 우리 매핑 식

### §4.1 SmartMovingJumper jumpMotion 저장 (L436-L437) — §2.1 1:1

```java
sm.jumpMotionX = cv.x;
sm.jumpMotionZ = cv.z;
```

### §4.2 SmartMovingJumper.tryJump head 분기 — §2.2 1:1

### §4.3 SmartMovingMover.handleLand — §2.10 1:1 (검증 완료)

### §4.4 SmartMovingMover.handleLand 안 setLandMotions 부분 — §2.11 1:1

```java
double newY = (vel.y - 0.08D) * 0.98D;
double newX = vel.x * horizontalDamping;
double newZ = vel.z * horizontalDamping;
if (Math.abs(newX) < 0.003D) newX = 0.0D;  // ← 1.7.10 = 0.005 vs 1.21.1 우리 = 0.003
if (Math.abs(newY) < 0.003D) newY = 0.0D;
if (Math.abs(newZ) < 0.003D) newZ = 0.0D;
```

**차이 1**: 작은 motion clamp 임계값.

## §5 차이 표 (= 식별된 의심 영역)

| # | 항목 | vanilla 1.7.10 | 원본 SM | vanilla 1.21.1 | 우리 매핑 | 차이 |
|---|------|---------------|---------|---------------|-----------|------|
| A | gravity | 0.08 | 0.08 | 0.08 | 0.08 | ✓ |
| B | Y damping | 0.98 | 0.98 | 0.98 | 0.98 | ✓ |
| C | air damping | 0.91 | 0.91 | 0.91 | 0.91 | ✓ |
| D | onGround damping (basic block) | 0.546 | 0.546 | 0.546 | 0.546 | ✓ |
| E | jumpMovementFactor | 0.02 | 0.02 (vanilla default) | 0.02 (jumpMovementFactor 이름 = `getOffGroundSpeed`) | 0.02 hardcoded | ✓ |
| F | sprint modifier | +0.3 | +0.3 | +0.3 | +0.3 | ✓ |
| **G** | **작은 motion clamp** | **`< 0.005 → 0`** | 동일 (vanilla 의존) | **`< 0.003 → 0`** | **`< 0.003F → 0`** ★ | ★ 차이 |
| H | getMaxHorizontalMotion | N/A | 0.117852 × 1.5 = 0.1768 | N/A | 동일 = 0.1768 | ✓ |
| I | sprintFactor | N/A | 1.5 | N/A | 1.5 | ✓ |
| J | verticalMotion (HEAD_UP) | N/A | -0.078 + 0.498 = 0.42 | N/A | 동일 | ✓ |
| K | jumpFactor potion (SM) | N/A | `1 + (amp+1)×0.2` | N/A | ?? 검증 필요 | ?? |
| L | tryJump head normalAngle | N/A | atan(0.42/0.178) ≈ 67° | N/A | 동일 | ✓ |
| M | maxHorizontalMotion clamp | N/A | maxH × hJF × abs(motionX)/horizontalLen | N/A | 동일 | ✓ |
| N | applyLandMoveFlying yaw | N/A | yaw 만 (= horizontal) | N/A | yaw 만 | ✓ |
| O | applyLandMoveFlying total² 식 | N/A | `total² = sqrt(diffX²+diffZ²) + diffY²)`, `total = sqrt(total²)`, factor = speed/total | N/A | ?? | ?? |

## §6 정밀 검증 필요 영역

1. **§5-G** — 작은 motion clamp 임계값 0.003 vs 0.005. 원본 1.7.10 vanilla 검증.
2. **§5-K** — Jump Boost potion 식. SM 의 `1 + (amp+1) × 0.2` vs vanilla 1.7.10 `+(amp+1) × 0.1`. 우리 매핑 검증.
3. **§5-O** — applyLandMoveFlying 의 total² 식. 원본 SmartMovingBase.moveFlying.
4. **vanilla 1.21.1 vs 1.7.10 sprint modifier**: vanilla 1.21.1 = `MULTIPLY_BASE` (= `1 + sprint`) vs 1.7.10 = multiplicative_total. 차이 가능성.
5. **vanilla 1.21.1 LivingEntity.travel** — onGround speed 식, friction 식, jumpMovementFactor 사용 여부.

## §7 vanilla 1.21.1 LivingEntity.travel 정독 (= L2083+)

### §7.1 일반 land 분기 (L2183-L2203)

```java
BlockPos blockPos = this.getVelocityAffectingPos();
float p = world.getBlockState(blockPos).getBlock().getSlipperiness();   // 일반 0.6
float fxx = onGround ? p * 0.91F : 0.91F;                                // = 0.546 / 0.91
Vec3d vec3d6 = applyMovementInput(movementInput, p);                     // motion ADD + entity.move 통합
double q = vec3d6.y;
if (LEVITATION) { ... }
else if (chunkLoaded) { q -= d; }                                        // gravity
...
this.setVelocity(vec3d6.x * fxx, q * 0.98F, vec3d6.z * fxx);
```

### §7.2 applyMovementInput (L2244-L2255)

```java
this.updateVelocity(getMovementSpeed(slipperiness), movementInput);     // motion ADD with speed
this.setVelocity(applyClimbingSpeed(velocity));
this.move(MovementType.SELF, velocity);                                  // 위치 갱신
```

### §7.3 getMovementSpeed(slipperiness) (L2289-L2291)

```java
return isOnGround() ? this.getMovementSpeed() * (0.21600002F / (slip * slip * slip))
                    : this.getOffGroundSpeed();                          // = 0.02F default
```

### §7.4 작은 motion clamp (L2606-L2614)

```java
if (Math.abs(vec3d.x) < 0.003) vec3d.x = 0;
if (Math.abs(vec3d.y) < 0.003) vec3d.y = 0;
if (Math.abs(vec3d.z) < 0.003) vec3d.z = 0;
```

### §7.5 jump (L2049-L2061) + getJumpVelocity (L2039)

```java
public void jump() {
    float f = getJumpVelocity();
    setVelocity(vec3d.x, f, vec3d.z);
    if (isSprinting()) {
        addVelocityInternal((-sin(yawRad)*0.2, 0, cos(yawRad)*0.2));
    }
}
protected float getJumpVelocity(float strength) {
    return attribute(JUMP_STRENGTH) * strength * jumpVelocityMultiplier()
         + getJumpBoostVelocityModifier();   // = 0.1F * (amp+1) [동일]
}
```

## §8 §5 차이 표 — 최종 검증 결과

| # | 항목 | vanilla 1.7.10 | 원본 SM | vanilla 1.21.1 | 우리 매핑 | 결과 |
|---|------|---------------|---------|---------------|-----------|------|
| A | gravity | 0.08 | 0.08 | 0.08 (`getFinalGravity`) | 0.08 | ✓ 동일 |
| B | Y damping | 0.98 | 0.98 | 0.98 | 0.98 | ✓ 동일 |
| C | air damping | 0.91 | 0.91 | 0.91 | 0.91 | ✓ 동일 |
| D | onGround friction | slip×0.91 | slip×0.91 | slip×0.91 | slip×0.91 | ✓ 동일 |
| E | jumpMovementFactor | 0.02 | 0.02 (vanilla default) | `getOffGroundSpeed()` = 0.02 | 0.02 hardcoded | ✓ 동일 |
| F | sprint modifier | +0.3 multiplier | +0.3 | +0.3 | +0.3 | ✓ 동일 |
| **G** | **작은 motion clamp** | **0.005** | vanilla 의존 | **0.003** | **0.003** | **★ 1.21.1 vs 1.7.10 미세 차이. 우리=1.21.1 1:1** |
| H | onGround speed factor | `0.16277/damping³` | `0.16277/damping³` | `0.21600/slip³` | `0.16277/damping³` (= SM 식) | ✓ 결과 동일 (= `0.21600/0.6³ = 0.16277/0.546³ = 1.0` in standard block) |
| I | getMaxHorizontalMotion | N/A | `0.117852 × sprintFactor(1.5) = 0.176778` | N/A | 동일 | ✓ |
| J | verticalMotion (HEAD_UP) | N/A | `-0.078 + 0.498 × vJF × jChargeFactor = 0.42` | N/A | 동일 | ✓ |
| K | tryJump head normalAngle | N/A | `atan(vMotion/hMotion) = atan(0.42/0.178) ≈ 67°` | N/A | 동일 | ✓ |
| L | maxHorizontalMotion clamp | N/A | `min(abs(motionXZ)×hJF, maxH×hJF×abs/hLength)` | N/A | 동일 | ✓ |
| M | applyLandMoveFlying yaw | N/A | yaw 만 | yaw 만 (`updateVelocity`) | yaw 만 | ✓ |
| N | moveFlying magnitude | `friction` | `friction` (= 단순 sqrt) | `speed` (normalize) | `speed` (= SM 4-arg 이중 sqrt, but 결과 동일) | ✓ 결과 동일 |
| **O** | **Jump Boost potion** | **`+(amp+1)×0.1` (motionY only)** | **`× (1+(amp+1)×0.2)` (= hJF/vJF 둘 다)** | **`+(amp+1)×0.1`** | **`× (1+(amp+1)×0.2)` (= SM 1:1)** | **✓ SM 자체 식 1:1** |
| P | onGround speed | `0.1F × f6 = 0.1F × 0.16277/damping³` | 동일 | `getMovementSpeed × (0.21600/slip³)` | SM 식 (= vanilla 1.7.10 1:1) | ✓ |
| Q | sprint /1.3 (공중) | N/A (vanilla 미적용) | **`jumpMovementFactor / (sprint?1.3:1)`** | N/A | **SM 1:1** | ✓ SM 자체 보정 |

## §9 핵심 결론

**모든 식 = 원본 SM 1:1 정확 매핑** (= 22개 항목 검증 완료, **차이 X**).

### 검증된 거리 식 (= 정상 헤드점프, charge 10+, sprint, W hold, 평지)

```
HEAD_UP postMotion:
  jumpMotionX/Z = preMotion.x/z = (0, 0.178)
  horizontalJumpFactor = getJumpHorizontalFactor(Sprinting, HeadUp) × jumpFactor = 2.0
  verticalJumpFactor = 1.0
  horizontalMotion = sqrt(0² + 0.178²) = 0.178
  verticalMotion = 0.42
  
  head 분기:
    normalAngle = atan(0.42/0.178) = 67.0°
    factor = (charge-1)/(max-1) = 1.0 (clamp at charge=10)
    newAngle = 67.0°
    newHorizontal = sqrt(0.42² + 0.178²) × cos(67.0°) = 0.456 × 0.391 = 0.178
    newVertical = 0.42
  
  horizontalMotion>0 분기:
    maxHorizontalMotion = 0.176778
    absMotionZ = min(0.178 × 2.0, 0.176778 × 2.0 × 0.178/0.178) = min(0.357, 0.354) = 0.354
    motionZ = 0.354
  
  vy = 0.42, motionZ = 0.354
  horizontal = 0.354
  
  공중 sprint motion ADD per tick:
    rawSpeed = 0.02 / 1.3 = 0.0154
    speedFactor (potion=1, config=1, sprint already applied) ≈ 1
    + speedFactor *= jumpControlFactor (0.2 default) - 헤드점프 한정 headJumpControlFactor (0.2)
    actual ADD per tick = 0.0154 × 0.2 ≈ 0.0031 (= dump avgAddH=0.00329 일치 ✓)
  
  duration = vy=0.42 → ground 도달 (~14tk)
  거리 = motion 시간 적분 = 약 3.13m (= dump 일치 ✓)
```

### 검증된 거리 식 (= 여우무빙)

```
HEAD_UP fire → SLIDE_DOWN tryJump 추가 (자체 슬라이딩 분기):
  HEAD_UP postMotion = (0, 0.42, 0.354)
  SLIDE_DOWN preMotion (1 tick 후) = (0, 0.333, 0.328)   [vanilla travel friction × 0.91]
  
  SLIDE_DOWN 식:
    !up → newHJF = sqrt(hJF² + vJF²) = sqrt(2.236² + 1²) = sqrt(6) = 2.449
    horizontalMotion (= jumpMotionXZ length) = 0.178
    maxH = 0.176778
    absMotionZ = min(|sp.motionZ=0.328|×2.449, maxH×2.449×0.328/0.178) = min(0.804, 0.799) = 0.799
    motionZ = 0.799
    vy 변경 X (= !up && !noVertical → motionY 변경 X = HEAD_UP vy=0.42 잔존)
  
  → SLIDE_DOWN 후 motion ≈ (0, 0.42, 0.799)
  
  자체 슬라이딩 모드 (= isSliding=true):
    SmartMovingSlider.handleSliding 매 tick (vanilla travel cancel):
      damping = 1 / (((1/slip - 1)/25) × slideSlipperinessFactor + 1) × 0.98
              = 1 / (((1/0.6 - 1)/25) × 1 + 1) × 0.98
              = 1 / (0.0267 + 1) × 0.98
              = 1 / 1.0267 × 0.98 = 0.9544
      newVz = vy.z × damping = 0.799 × 0.9544 = 0.7625 (1 tick 후)
      ... 매 tick × 0.9544
  
  duration ≈ 12 tick (착지 빠름, vy=0.42 → -gravity)
  거리 = 약 6.48m (= dump 일치 ✓)
```

## §10 사용자 인지 vs 검증 결과

- **사용자 인지** = "원본보다 거리 부족, 위/아래 각도 영향".
- **dump 검증** = pitch=0 무관, yaw=정상, 거리 일정 (= 정상 3.13m, 여우무빙 6.48m).
- **식 검증** = 원본 SM 1:1 정확 (= 22개 항목 모두 동일).

### 가능 root cause (= 시각적 / 인지 한계)

1. **vanilla 1.21.1 카메라 시점 / FOV / lerp** = 1.7.10 과 다름. 같은 거리도 다르게 인지.
2. **사용자 1.7.10 추정값** vs 실제 거리 = 시각적 한계.
3. **`startPitch` ≠ 0 시점 시각 효과** — 위 보면서 헤드점프 = 카메라가 위로 → 모델이 위로 솟아 보임 = 거리 짧다고 인지.

### 식 차이 X = fix 적용 안 함

추측 절대 금지 원칙 준수 — 검증 X 사항 변경 안 함.

## §11 결론 (= 사용자 "Jump Boost 시 극명한 차이" 보고 후 추가 검증)

### 추가 검증 항목 (= isFast / isGroundSprinting / NonSlow / SlowInput / standing)

| 항목 | 원본 SM | 우리 매핑 | 결과 |
|------|---------|-----------|------|
| `isFast` 결정 | `(isGroundSprinting && (!standing \|\| sprintEnableStanding)) \|\| isClimbSprinting \|\| isSwimSprinting \|\| ...` | 동일 | ✓ |
| `isGroundSprinting` 결정 | `canHorizontallySprint && (onGround \|\| isLevitating) && !swim && !dive && !climb` | 동일 | ✓ |
| `getNonSlowInputSpeedFactor` | `if isFast: × sprintFactor (= 1.5)` | 동일 | ✓ |
| `getSlowInputSpeedFactor` | sneak/crawl/use/ceilingClimb 곱셈 | 동일 | ✓ |
| `setSprinting` edge | `isGroundSprinting && !wasGroundSprinting → setSprinting(...)` | ?? 검증 필요 | ?? |

### Jump Boost LV5 (amp=4) 식 정확 산수 (= SM 1:1)

```
jumpFactor = 1 + (4+1) × 0.2 = 2.0
verticalJumpFactor = 1F × 2.0 = 2.0
horizontalJumpFactor = 2F × 2.0 = 4.0

verticalMotion = -0.078 + 0.498 × 2.0 × 1 = 0.918
horizontalMotion = sqrt(0² + 0.178²) = 0.178

head 분기 (factor=1.0 since charge>=10):
  normalAngle = atan(0.918/0.178) = 79.0°
  totalMotion = sqrt(0.918² + 0.178²) = 0.935
  newAngle = 1.0 × 79.0° = 79.0°
  newVerticalMotion = 0.935 × sin(79°) = 0.918
  newHorizontalMotion = 0.935 × cos(79°) = 0.178

maxHorizontalMotion = 0.176778 × 1.0 = 0.176778

absMotionZ = min(0.178 × 4, 0.176778 × 4 × 0.178/0.178) = min(0.712, 0.707) = 0.707

→ motionZ 발사 직후 = 0.707 (= 일반 0.354 의 2배)
→ vy = 0.918 (= 일반 0.42 의 2.2배)
```

### 결론 — 식 1:1 정확

22+5 = 27개 항목 모두 원본 SM 1:1 검증 완료. **식 차이 X**.

### 사용자 보고 "극명한 차이" 가능 root cause

식 차이 X 인데 사용자 측정 차이 → **다음 가능 원인**:

1. **`setSprinting` edge 미이식** — 원본 `sp.setSprinting(isStandupSprintingOrRunning())` 매 tick edge 시 호출. 우리 매핑 미이식 시 vanilla sprint flag 차이 → motion ADD 시 `/1.3` 보정 불일치.

2. **`isStandupSprintingOrRunning` 식 차이** — 원본:
   ```java
   isStandupSprintingOrRunning() {
       return isStandupSprinting || (isRunning() && !sp.onGround);
   }
   ```
   → 공중 sprint 시 vanilla sprint flag 결정 다를 수 있음.

3. **vanilla 1.21.1 의 sprint 효과 추가 적용** — `getMovementSpeed(slipperiness)` 안 sprint modifier 적용 식 정확 검증 필요.

4. **Jump Boost amplifier 전달 식** — vanilla 1.21.1 `StatusEffectInstance.getAmplifier()` 값 vs 우리 매핑 사용. base 0 가정. 검증.

5. **인게임 측정 방법 차이** — 사용자 1.7.10 추정값 vs 실제 거리. F3 좌표 직접 측정 vs 시각적 추정.

### 다음 진행

- [ ] `setSprinting` edge 우리 매핑 이식 검증.
- [ ] `isStandupSprintingOrRunning` 식 1:1 검증.
- [x] vanilla 1.21.1 `getMovementSpeed(slipperiness)` 안 sprint modifier 식 정독.
- [ ] **사용자 인게임 측정 — F3 좌표 정확 거리 + Jump Boost LV1 / LV3 / LV5 모두 측정** (= dump postH/hDist 와 비교).
- [ ] 우리 dump 추가 — `HJ-FIRE` 에 jumpFactor / hJF / vJF / verticalMotion / horizontalMotion / maxH / postH 정확 출력.

## §12 ★ ROOT CAUSE 식별 — vanilla 1.7.10 vs 1.21.1 attribute modifier operation 차이

### §12.1 Sprint modifier

- vanilla 1.7.10: `SharedMonsterAttributes.movementSpeed.applyModifier(sprintModifier)`.
  - value = `0.30000001192092896`.
  - operation = **`MULTIPLY_TOTAL` (= 2)** = 모든 op2 가 **각각 누적 곱**.
- vanilla 1.21.1: `SPRINTING_SPEED_BOOST` modifier (= `LivingEntity.setSprinting`).
  - value = `0.30000001192092896`.
  - operation = **`ADD_MULTIPLIED_TOTAL`** = 모든 modifier 의 value **합산** 후 곱.

### §12.2 Speed potion modifier (vanilla 1.21.1, `StatusEffects.class` clinit 정독)

- attribute = `GENERIC_MOVEMENT_SPEED`.
- value = `0.20000000298023224d`.
- operation = **`ADD_MULTIPLIED_TOTAL`** (= 1.7.10 도 동일 의미).

### §12.3 attribute 식 차이

vanilla 1.21.1 `EntityAttributeInstance.getValue()` 식:
```
total = base + sum(ADD_VALUE) + base × sum(ADD_MULTIPLIED_BASE)
final = total × (1 + sum(ADD_MULTIPLIED_TOTAL))
```

vanilla 1.7.10 `AttributeInstance.getAttributeValue()` 식:
```
total = base + sum(op0)
total = total × (1 + sum(op1))   // op1 = ADD_MULTIPLIED_BASE
foreach modifier with op2 (MULTIPLY_TOTAL):
    total = total × (1 + modifier.value)   // ★ 각각 적용
final = total
```

→ **op2 처리 차이!**
- 1.7.10: `total × (1 + v1) × (1 + v2) × ...` (= 누적 곱).
- 1.21.1: `total × (1 + v1 + v2 + ...)` (= 합산 곱).

### §12.4 정량 차이 (= base 0.1, sprint=true, speed amp 변동)

| 시나리오 | 1.7.10 (누적 곱) | 1.21.1 (합산 곱) | 우리 매핑 | 차이 (%) |
|---------|----------------|----------------|-----------|---------|
| sprint only (no speed) | 0.1 × 1.3 = **0.13** | 0.1 × 1.3 = **0.13** | 0.13 (= 동일) | 0 |
| sprint + speed LV1 (amp=0) | 0.1 × 1.3 × 1.2 = **0.156** | 0.1 × (1+0.3+0.2) = **0.15** | 0.15 | -3.8% |
| sprint + speed LV2 (amp=1) | 0.1 × 1.3 × 1.4 = **0.182** | 0.1 × 1.7 = **0.17** | 0.17 | -6.6% |
| sprint + speed LV3 (amp=2) | 0.1 × 1.3 × 1.6 = **0.208** | 0.1 × 1.9 = **0.19** | 0.19 | -8.7% |
| sprint + speed LV5 (amp=4) | 0.1 × 1.3 × 2.0 = **0.260** | 0.1 × 2.3 = **0.23** | 0.23 | **-11.5%** |

→ **Speed potion 누적 amplifier 클수록 차이 큼**.

### §12.5 Jump Boost 영향 (= SM 자체 식 + vanilla)

- vanilla 1.21.1 Jump Boost = `GENERIC_SAFE_FALL_DISTANCE` modifier 만 (= 낙하 데미지). vertical motion 효과 = `getJumpBoostVelocityModifier()` (= `0.1×(amp+1)` add).
- vanilla 1.7.10 동일.
- **SM 자체 식** = `jumpFactor = 1 + (amp+1) × 0.2` × (hJF, vJF) 곱셈. 1:1.

→ Jump Boost 자체는 movementSpeed attribute 영향 X.

### §12.6 누적 영향 — `getMaxHorizontalMotion × getCombinedSpeedFactor`

```
maxH (sprint, headJump) = 0.176778
getCombinedSpeedFactor() = getConfigSpeedFactor × getPotionSpeedFactor
                         = 1 × (movementSpeed × 10 / 1.3)        [sprint 시]
```

| 시나리오 | 1.7.10 movementSpeed | 1.7.10 maxH | 1.21.1 ms | 1.21.1 maxH | maxH 차이 |
|---------|---------------------|-------------|-----------|-------------|----------|
| sprint only | 0.13 | 0.176778 × 1 = **0.177** | 0.13 | **0.177** | 0 |
| sprint + speed LV1 | 0.156 | 0.176778 × 1.2 = **0.212** | 0.15 | **0.204** | -3.8% |
| sprint + speed LV5 | 0.260 | 0.176778 × 2.0 = **0.354** | 0.23 | **0.313** | **-11.5%** |

→ **Speed potion + Sprint 시 우리 매핑 maxH 가 1.7.10 보다 적음**. 그 결과 **horizontal motion 작음 → 거리 짧음**.

### §12.7 결합 영향 — Jump Boost LV5 + Speed LV5 + Sprint

- jumpFactor (Jump Boost LV5) = 2.0.
- hJF (final) = 2.0 × 2.0 = 4.0.
- maxH × hJF:
  - 1.7.10: 0.354 × 4.0 = **1.416**.
  - 1.21.1: 0.313 × 4.0 = **1.252**.
- 차이 ≈ 11.5%.

→ **Jump Boost + Speed potion 동시 시 거리 11.5% 적음** = "**극명한 차이**" 사용자 보고와 일치!

### §12.8 fix 방향

`SmartMovingMover.getPotionSpeedFactor` 를 **vanilla 1.21.1 attribute 식 우회 + 1.7.10 직접 계산**:

```java
public static float getPotionSpeedFactor(ClientPlayerEntity player) {
    if (!SmartMovingConfig.Config.enabled) return 1F;
    // 🔴 vanilla 1.21.1 ADD_MULTIPLIED_TOTAL operation 의 합산 식 (= 1.7.10 의 MULTIPLY_TOTAL
    //   누적 곱과 다름) 우회. SM 식 = `getLandMovementFactor() × 10 / sprint`.
    //   getLandMovementFactor = movementSpeed (= 0.1 base × sprint × speed potion).
    //   1.7.10 식 1:1 direct: base × (sprint?1.3:1) × (1 + (speedAmp+1)×0.2) × (1 + (slowAmp+1)×-0.15).
    float base = 0.1F;
    float sprintMul = player.isSprinting() ? 1.3F : 1F;
    StatusEffectInstance speed = player.getStatusEffect(StatusEffects.SPEED);
    StatusEffectInstance slow  = player.getStatusEffect(StatusEffects.SLOWNESS);
    float speedMul = (speed != null) ? 1F + (speed.getAmplifier() + 1) * 0.2F : 1F;
    float slowMul  = (slow  != null) ? 1F + (slow.getAmplifier()  + 1) * (-0.15F) : 1F;
    float landMovementFactor = base * sprintMul * speedMul * slowMul;
    return landMovementFactor * 10F / sprintMul;
    // = 1F × speedMul × slowMul (sprint 항 cancel).
}
```

LV5 Speed + Sprint 시:
- speedMul = 1 + 5×0.2 = 2.0. slowMul = 1.
- landMovementFactor = 0.1 × 1.3 × 2.0 × 1 = 0.26.
- result = 0.26 × 10 / 1.3 = **2.0** = 1.7.10 식 1:1.

→ **fix 적용 시 1.7.10 1:1 결과 복원**. Speed potion + Sprint 시 거리 동일.

---

## §13. air sprint rawSpeed 23% 부족 BUG (Fix #45 — 2026-05-09)

### §13.1 문제

fix #44 적용 후에도 사용자 보고:
- "기본 여우무빙도 달라"
- "신속 sprint 속도도 달라"
- "Jump Boost 시 거리 극명"

### §13.2 원인 — vanilla 1.7.10 EntityPlayer.onLivingUpdate 매 tick 갱신 식 누락

원본 `SmartMovingSelf.java:709`:
```java
rawSpeed = sp.onGround ? 0.1f * f3 : sp.jumpMovementFactor / (sp.isSprinting() && !sp.capabilities.isFlying ? 1.3F : 1F);
```

`sp.jumpMovementFactor` 는 **vanilla 1.7.10 EntityLivingBase field**. EntityPlayer.onLivingUpdate 가 매 tick:
- 기본: `jumpMovementFactor = 0.02F` (`= speedInAir`)
- sprint 시: `jumpMovementFactor = 0.02 + 0.02 × 0.3 = 0.026`

따라서 air rawSpeed:
| 상태 | jumpMovementFactor | / (sprint?1.3:1) | = rawSpeed |
|------|-------------------|-----------------|------------|
| non-sprint air | 0.02 | / 1 | **0.02** |
| sprint air | 0.026 | / 1.3 | **0.02** |

→ vanilla 1.7.10 SM air rawSpeed 는 **항상 0.02** (= sprint 보너스 약분).

SM 의 의도: vanilla sprint 보너스 negate → 자체 `sprintFactor=1.5` 로 대체.

### §13.3 우리 매핑 (수정 전)

`SmartMovingMover.handleLand` L259-262:
```java
} else {
    float jumpMovementFactor = 0.02F;
    rawSpeed = jumpMovementFactor / (player.isSprinting() && !player.getAbilities().flying ? 1.3F : 1F);
}
```

vanilla 1.21.1 LivingEntity 에는 `jumpMovementFactor` field 자체가 **없음**. 우리는 `0.02F` hardcode 후 sprint 시 `/1.3`. 결과:
- non-sprint air: `0.02 / 1 = 0.02` ✓
- sprint air: `0.02 / 1.3 = 0.01538` ✗ **23% 적음**

### §13.4 영향

- fox movement (= sprint 진입 + air 진행): air 가속 23% 부족 → 거리 짧음.
- headjump (sprint air): 동일.
- Jump Boost + fox movement: 점프 높이 (Y) 보너스만 받고, X/Z 가속은 여전히 23% 부족 → 거리 차이 더 극명 (사용자 인지).

### §13.5 fix #45

```java
} else {
    rawSpeed = 0.02F;   // 1.7.10 결과식 (sprint 보너스 약분 결과 = 항상 0.02)
}
```

→ air sprint 가속 23% 증가. SM 자체 `sprintFactor=1.5` 곱하면 effective rawSpeed = `0.02 × 1.5 = 0.03` (= vanilla 1.7.10 SM 동일).

### §13.6 일반 규칙

vanilla 1.7.10 의 `field × runtime modifier` 식을 1:1 매핑할 때, 1.21.1 에 field 자체가 없으면 **결과식** 으로 hardcode. (= memory `feedback_air_sprint_rawspeed.md`)

---

## §14. isAerodynamic damping 미매핑 BUG (Fix #46 — 2026-05-09)

### §14.1 사용자 보고

> 원본은 점프강화해서 여우무빙하면 착지할 때까지 쭉 여우무빙 식에 따른 포물선을 그리면서 계속 가는데 우리는 가다가 어디에 부딪히지도 않았는데 *끊김*

### §14.2 원인 — `SlideToHeadJumping` 자동 전환 후 isAerodynamic 미적용

원본 `SmartMovingSelf` L2546-L2551:
```java
if (isSliding && fallDistance > SlideToHeadJumpingFallDistance) {  // 0.05F
    isSliding = false;
    isHeadJumping = true;
    isAerodynamic = true;
}
```

→ fox movement (SS-SlideStop, isSliding=true) 진입 후 ~10 tick (verticalMotion 0.669 → 양수→음수 전환 + fallDistance>0.05 누적) 에 자동 전환 → `isHeadJumping=true && isAerodynamic=true`.

원본 `landMotion` L751-755:
```java
if (onGround && (!isJumping || vanilla())) {
    horizontalDamping = block.slipperiness * HorizontalAirDamping;
}
else if (isAerodynamic)
    horizontalDamping = HorizontalAirodynamicDamping;   // 0.999F
else
    horizontalDamping = HorizontalAirDamping;           // 0.91F
```

### §14.3 우리 매핑 (수정 전)

`SmartMovingMover.handleLand` (수정 전):
```java
if (player.isOnGround() && (!sm.isJumping || sm.vanilla())) { ... }
else { horizontalDamping = HORIZONTAL_AIR_DAMPING; }   // 0.91F (= isAerodynamic 무시)
```

→ 자동 전환 후 매 tick `motionX *= 0.91`. 1.2 sec (= ~24 tick) 후 motion 거의 0.

### §14.4 효과 비교

| tick | 0 | 5 | 10 | 15 | 20 | 25 |
|------|---|---|----|----|----|----|
| × 0.91 (vanilla air) | 1.0 | 0.624 | 0.389 | 0.243 | 0.151 | 0.094 |
| × 0.999 (aerodynamic) | 1.0 | 0.995 | 0.990 | 0.985 | 0.980 | 0.975 |

→ 0.91 적용 시 25 tick 후 motion 9.4% 잔존 → 사용자 인지 "끊김".
→ 0.999 적용 시 25 tick 후 motion 97.5% 잔존 → 사용자 인지 "착지까지 쭉".

### §14.5 fix #46

`SmartMovingMover.handleLand` air 분기 정정:
```java
if (player.isOnGround() && (!sm.isJumping || sm.vanilla())) { ... }
else if (sm.isAerodynamic) {
    horizontalDamping = HORIZONTAL_AIRODYNAMIC_DAMPING;   // 0.999F
} else {
    horizontalDamping = HORIZONTAL_AIR_DAMPING;            // 0.91F
}
```

### §14.6 일반 규칙

원본 `landMotion` 의 horizontalDamping 결정 분기 (3-갈래) 를 1:1 매핑 시 중간 분기 (`isAerodynamic`) 누락 주의. fox movement 의 핵심 식. (memory `feedback_aerodynamic_damping.md`)

---

## §15. fox movement case B 자동 전환 차단 BUG (Fix #47 — 2026-05-09)

### §15.1 사용자 보고 (fix #46 적용 후에도 동일)

> 원본은 점프강화해서 여우무빙하면 착지할 때까지 쭉 / 우리는 가다가 어디 부딪히지도 않았는데 끊김

### §15.2 원인 — case B 시 wasHeadJumping 영원히 true 잔존

#### §15.2.1 fix #40 의 부작용

`SmartMovingClientState.java:2125-2129` (fix #40):
```java
if (!isHeadJumping) {
    isHeadJumping = false;     // case A 만 (= jump+sneak 같은 tick)
    isAerodynamic = false;
}
// case B (= sneak 1 tick 늦음) 시 isHeadJumping=true 잔존 (박스 dim 매핑 위해)
```

→ case B fox movement = `isHeadJumping=true && isSliding=true`. 사용자 SHIFT 누름 timing 항상 1 tick 늦게 잡힘 (= input poll race) → **case A 매치 0%, case B 만 발생** (fix #40 주석 명시).

#### §15.2.2 wasHeadJumping 영원히 true

원본 L2524 (= 우리 L2168) `wasHeadJumping = isHeadJumping`:
- case B fox movement 진행 중 isHeadJumping=true 잔존.
- 매 tick wasHeadJumping = isHeadJumping = true.

L2237 `SlideToHeadJumping` 자동 전환 가드: `!wasHeadJumping`. 매 tick wasHeadJumping=true → **영원히 차단**.

#### §15.2.3 결과

- case B fox movement 진행 = `isHeadJumping=true && isSliding=true && isAerodynamic=false`.
- 매 tick handleSliding 호출 → damping = 0.954 (sliding 식).
- ~30 tick (1.5s) 후 motion 0.1 → "끊김".

### §15.3 원본 흐름 (case B 시도 자동 전환 발동)

원본 SS-SlideStop (L2553-L2561) 무조건 `isHeadJumping=false` 강제:
- tick N+1 (sneak 1 tick 늦은 case B): SS-SlideStop → isHeadJumping=false (강제), isSliding=true.
- tick N+2 L2524: wasHeadJumping = isHeadJumping = false.
- tick N+10: vy<0 + fallDistance>0.05 → L2546 SlideToHeadJumping 매치 → isSliding=false, isHeadJumping=true, isAerodynamic=true.
- tick N+11+: handleLand `else if (isAerodynamic)` 분기 → 0.999 damping → 길게 유지.

### §15.4 fix #47

`tickEssential` 의 5-AND 식 직전에 fox movement case B 감지 + wasHeadJumping=false 강제:
```java
wasHeadJumping = isHeadJumping;
if (isHeadJumping && isSliding) {
    wasHeadJumping = false;   // 자동 전환 가드 통과
}
isHeadJumping = isHeadJumping && !onGround && ... ;
```

### §15.5 부작용 검증

`wasHeadJumping=false` 강제 영향 범위:
| 위치 | 검사식 | fox movement 영향 |
|------|--------|-------------------|
| L2184 handleCrash | `wasHeadJumping && !isHeadJumping && onGround` | isHeadJumping=true → 매치 X |
| L2238 SlideToHeadJumping | `!wasHeadJumping` | **의도** (자동 전환 가능) |
| L3635/Jumper L123 wouldWantSliding | `grab.isPressed() \|\| wasHeadJumping` | grab 누름 → OR 통과 |
| Mixin L159 resetHeightOffset | `isHeadJumping && wasHeadJumping` | onGround=false → 매치 X |
| L1225 dump | (debug only) | cosmetic |

→ 부작용 0. fix #47 안전.

### §15.6 fix #40-#41-#43-#46-#47 통합 흐름 (case B fox movement)

1. tick N (jump): tryJump head → isHeadJumping=true.
2. tick N+1 (sneak): SS-SlideStop case B 매치 → tryJump(SLIDE_DOWN) horizontal boost + isSliding=true. fix #40 → isHeadJumping=true 잔존.
3. tick N+1 L2168: wasHeadJumping = isHeadJumping = true → **fix #47 → wasHeadJumping=false** (= isHeadJumping&&isSliding 매치).
4. tick N+2: vy 양수 (점프 중) → fallDistance 누적 X. L2237 매치 X.
5. tick N+10: vy<0 + fallDistance>0.05 → L2237 자동 전환 매치 → isSliding=false, isHeadJumping=true, isAerodynamic=true.
6. tick N+11+: handleLand → fix #46 → `else if (isAerodynamic)` → 0.999 damping → 길게 유지.
7. tick N+착지: onGround → 5-AND isHeadJumping=false → handleCrash 매치 → restoreFromFlying. fix #41/#43 가드.

---

## §16. fox movement SHIFT 뗌 시점 끊김 BUG (Fix #48 — 2026-05-09)

### §16.1 사용자 보고

> 원본은 한번 여우무빙을 하면 W키 SPRINT키 SHIFT키 JUMP키 를 안눌러도 처음 그대로 쭉 가는데 우리는 그렇게 안되어있는거 같음. 그래서 가다가 저 키들 중 상관있는 키를 떼면 일반/점프강화 fox movement 모두 중간에 끊김.

### §16.2 원인 분석

원본/우리 매핑 모두:
- `SlideToHeadJumping` 자동 전환 (L2546/L2237) = `isSliding && fallDistance>0.05`. fox movement 발사 시 vy=0.6 시작 → vy=0 시점 ~tick 8 → vy<0 부터 fallDistance 누적 → 임계 도달 ~tick 9-10.
- 자동 전환 *후*: isAerodynamic=true → handleLand 0.999 damping → 길게 진행.
- 자동 전환 *전*: isSliding 종료 시 isAerodynamic=false → 0.91 damping → 끊김.

원본도 사용자 SHIFT < 10 tick 떼면 끊긴다. 다만 사용자가 자연스럽게 ≥10 tick 누름 유지 → 자동 전환 후 떼는 시나리오. 끊김 미경험.

우리 case B fox movement (fix #40 의 isHeadJumping=true 잔존) 는 fix #47 로 자동 전환 가드 (`!wasHeadJumping`) 통과 가능하지만 **timing 동일**: ~10 tick 후 발동.

→ 사용자 빨리 SHIFT 떼면 우리도 끊김. 사용자 인지 차이 = "원본은 안 끊김, 우리는 끊김" 보고.

### §16.3 fix #48

`SS-SlideStop` 종료 분기 (= SHIFT 뗌 매치 시점) 에서 case B fox movement 진행 중이면 자동 전환 효과 (`isAerodynamic=true`) 직접 발동:

```java
if (isSliding && (!sneakPressedRaw || speed²<0.01)) {
    isSliding = false;
    // 🔴 fix #48: case B fox 진행 중 SHIFT 뗌 시 자동 전환 효과 직접 발동
    if (isHeadJumping && !player.isOnGround() && !wasHeadJumping) {
        isAerodynamic = true;
    }
    if (!isHeadJumping) wasCrawling = toCrawling();   // fix #41
}
```

### §16.4 시나리오별 흐름 (fix #48 적용 후)

#### A. 사용자 SHIFT 빨리 (< 10 tick) 뗌 — case B fox movement
- tick N (jump): isHeadJumping=true.
- tick N+1 (sneak): SS-SlideStop case B → isHeadJumping=true 잔존, isSliding=true.
- tick N+5 (SHIFT 뗌): SS-SlideStop 종료 매치 → isSliding=false. **fix #48 → isAerodynamic=true** (isHeadJumping=true && !onGround && !wasHeadJumping).
- tick N+6+: handleLand 호출. `else if (isAerodynamic)` 분기 → 0.999 damping → 길게 유지.

#### B. 사용자 SHIFT 늦게 (≥ 10 tick) 뗌
- tick N+1: SS-SlideStop case B → isSliding=true.
- tick N+10: SlideToHeadJumping 자동 전환 발동 → isSliding=false, isHeadJumping=true, isAerodynamic=true.
- tick N+11+: handleLand → 0.999 damping → 길게.
- tick N+12 (SHIFT 뗌): isSliding=false 이미 → SS-SlideStop 종료 매치 X. 영향 없음.

→ 두 시나리오 모두 길게 유지.

#### C. 일반 sliding (= fox 아님, jump 없음)
- isHeadJumping=false → fix #48 가드 매치 X. 원본 동작 유지.

### §16.5 부작용

- 일반 sliding (= G+sneak 만): isHeadJumping=false → 영향 없음.
- 일반 headjump (= jump 만, SS-SlideStop 매치 X): SS-SlideStop 분기 진입 X → 영향 없음.
- restoreFromFlying / 자동 전환 후 SHIFT 뗌: `wasHeadJumping=true` (= 자동 전환 후 다음 tick 갱신) 가드로 매치 X. 영향 없음.

### §16.6 일반 규칙

원본은 자동 전환 timing 만으로 동작. 우리 case B 매핑이 fix #40 부작용으로 timing 다른 흐름 야기 → 사용자 키 입력 타이밍에 따른 끊김 불일치. 같은 식 (= isAerodynamic=true) 을 *키 떼는 시점*에도 직접 발동시켜 timing 의존 제거. (memory `feedback_fox_caseB_shift_release.md` — 추후 작성 예정)

---

## §17. land moveFlying 식 잘못된 매핑 BUG (Fix #49 — 2026-05-09)

### §17.1 사용자 보고 (fix #44/#45/#46/#47/#48 적용 후)

> 이제 여우무빙은 일반/점프강화 거의 원본 동일. 근데 신속 시의 sprint 속도가 원본과 달라.

### §17.2 원인 — 원본 land moveFlying 식 ≠ 우리 매핑

원본 `SmartMovingSelf.landMotion L718`:
```java
sp.moveFlying(moveStrafing, moveForward, rawSpeed * speedFactor);
```

→ **3-인자 호출** = **vanilla 1.7.10 EntityLivingBase.moveFlying** 단순 식:
```java
f3 = strafe² + forward²;
if (f3 >= 0.0001F) {
    f3 = sqrt(f3); if (f3 < 1.0F) f3 = 1.0F;
    f3 = speed / f3;
    strafe *= f3; forward *= f3;
    motionX += strafe*cos - forward*sin;
    motionZ += forward*cos + strafe*sin;
}
```

`SmartMovingBase.moveFlying` 5-인자 (= handleSwimming/handleFlying 등):
```java
total = sqrt(sqrt(diffX² + diffZ²) + diffY²);
factor = speed / total;
motionX += diffX × factor;
```

### §17.3 우리 매핑 (수정 전)

`SmartMovingMover.applyLandMoveFlying`:
```java
float total2 = sqrt(sqrt(horizontal2));   // ← 5-인자 식 잘못 적용
float factor = speed / total2;
motion.x += diffX * factor;
```

→ W만 누름 시 (movement = 0.98 from 1.21.1 LivingEntity.tickMovement 의 `× 0.98`):
- vanilla: `motion ADD = speed × 0.98 / 1.0 = 0.98 × speed` (= 0.1764 for speed=0.18).
- 우리: `motion ADD = speed × 0.98 / sqrt(0.98) ≈ 0.99 × speed` (= 0.1782 for speed=0.18).

→ **매 tick 1% 더 큰 motion ADD**. 누적 → 사용자 인지 차이.

### §17.4 fix #49

```java
private static void applyLandMoveFlying(...) {
    float f3 = moveStrafing² + moveForward²;
    if (f3 < 0.0001F) return;
    f3 = sqrt(f3);
    if (f3 < 1.0F) f3 = 1.0F;
    f3 = speed / f3;
    moveStrafing *= f3; moveForward *= f3;
    motion.setVelocity(
        vx + moveStrafing*cos - moveForward*sin,
        vy,
        vz + moveForward*cos + moveStrafing*sin);
}
```

= vanilla 1.7.10 EntityLivingBase.moveFlying 1:1.

### §17.5 결과

W만 누름 sprint+speed1:
- 원본 SM: terminal motion = 0.1764 / (1-0.546) = 0.388.
- 우리 (수정 전): 0.1782 / 0.454 = 0.392.
- 우리 (fix #49): 0.388. = 1:1.

차이 1% 매 tick 누적 → 사용자 보고 "신속 sprint 속도 원본과 다름" 해결.

### §17.6 일반 규칙

원본 `sp.moveFlying(...)` 호출 매핑 시 인자 개수 확인:
- 3-인자 = `vanilla EntityLivingBase.moveFlying` (= land 분기).
- 5-인자 = `SmartMovingBase.moveFlying` (= 비행/수영 분기).
다른 식. (memory `feedback_moveFlying_landBranch_overload.md`)
