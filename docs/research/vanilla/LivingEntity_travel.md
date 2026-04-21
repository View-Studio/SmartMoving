# LivingEntity.travel() — vanilla 1.21.1 리서치

Yarn 버전: `net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2`  
소스: IntelliJ 디컴파일 캐시 `AppData/Local/Temp/net/minecraft/entity/LivingEntity.java`  
실행 위치: 클라이언트 (로컬 플레이어) + 서버 (`isLogicalSideForUpdatingMovement()` 조건)

---

## Yarn 이름 확인 (mappings.tiny 직접 확인)

| Yarn 이름 | 중간 이름(intermediary) | 디스크립터 | 클래스 |
|-----------|------------------------|-----------|-------|
| `travel` | `method_6091` | `(Lexc;)V` | `LivingEntity` |
| `travelControlled` | `method_49483` | `(Lcmx;Lexc;)V` | `LivingEntity` |
| `isLogicalSideForUpdatingMovement` | `method_5787` | `()Z` | `Entity` |
| `canMoveVoluntarily` | `method_6034` | `()Z` | `Entity` |
| `getFinalGravity` | `method_56989` | `()D` | `Entity` |
| `applyMovementInput` | `method_26318` | `(Lexc;F)Lexc;` | `LivingEntity` |
| `applyFluidMovingSpeed` | `method_26317` | `(DZLexc;)Lexc;` | `LivingEntity` |
| `applyClimbingSpeed` | `method_18801` | `(Lexc;)Lexc;` | `LivingEntity` |
| `getMovementSpeed(float)` | `method_18802` | `(F)F` | `LivingEntity` |
| `getMovementSpeed()` | `method_6029` | `()F` | `LivingEntity` |
| `getBaseMovementSpeedMultiplier` | `method_6120` | `()F` | `LivingEntity` |
| `getOffGroundSpeed` | `method_49484` | `()F` | `LivingEntity` |
| `updateLimbs(boolean)` | `method_29242` | `(Z)V` | `LivingEntity` |
| `updateLimbs(float)` | `method_48565` | `(F)V` | `LivingEntity` |
| `updateVelocity` | (Entity) | `(FLexc;)V` | `Entity` |

`Lexc;` = `Vec3d`, `Lcmx;` = `PlayerEntity`

---

## `LivingEntity.travel(Vec3d movementInput)` 전체 코드

```java
public void travel(Vec3d movementInput) {
    if (this.isLogicalSideForUpdatingMovement()) {
        double d = this.getFinalGravity();
        boolean bl = this.getVelocity().y <= 0.0;
        if (bl && this.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            d = Math.min(d, 0.01);
        }

        FluidState fluidState = this.getWorld().getFluidState(this.getBlockPos());

        // ① 물속
        if (this.isTouchingWater() && this.shouldSwimInFluids() && !this.canWalkOnFluid(fluidState)) {
            double e = this.getY();
            float f = this.isSprinting() ? 0.9F : this.getBaseMovementSpeedMultiplier();
            float g = 0.02F;
            float h = (float)this.getAttributeValue(EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY);
            if (!this.isOnGround()) {
                h *= 0.5F;
            }
            if (h > 0.0F) {
                f += (0.54600006F - f) * h;
                g += (this.getMovementSpeed() - g) * h;
            }
            if (this.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                f = 0.96F;
            }
            this.updateVelocity(g, movementInput);
            this.move(MovementType.SELF, this.getVelocity());
            Vec3d vec3d = this.getVelocity();
            if (this.horizontalCollision && this.isClimbing()) {
                vec3d = new Vec3d(vec3d.x, 0.2, vec3d.z);
            }
            this.setVelocity(vec3d.multiply(f, 0.8F, f));
            Vec3d vec3d2 = this.applyFluidMovingSpeed(d, bl, this.getVelocity());
            this.setVelocity(vec3d2);
            if (this.horizontalCollision && this.doesNotCollide(vec3d2.x, vec3d2.y + 0.6F - this.getY() + e, vec3d2.z)) {
                this.setVelocity(vec3d2.x, 0.3F, vec3d2.z);
            }

        // ② 용암속
        } else if (this.isInLava() && this.shouldSwimInFluids() && !this.canWalkOnFluid(fluidState)) {
            double ex = this.getY();
            this.updateVelocity(0.02F, movementInput);
            this.move(MovementType.SELF, this.getVelocity());
            if (this.getFluidHeight(FluidTags.LAVA) <= this.getSwimHeight()) {
                this.setVelocity(this.getVelocity().multiply(0.5, 0.8F, 0.5));
                Vec3d vec3d3 = this.applyFluidMovingSpeed(d, bl, this.getVelocity());
                this.setVelocity(vec3d3);
            } else {
                this.setVelocity(this.getVelocity().multiply(0.5));
            }
            if (d != 0.0) {
                this.setVelocity(this.getVelocity().add(0.0, -d / 4.0, 0.0));
            }
            Vec3d vec3d3 = this.getVelocity();
            if (this.horizontalCollision && this.doesNotCollide(vec3d3.x, vec3d3.y + 0.6F - this.getY() + ex, vec3d3.z)) {
                this.setVelocity(vec3d3.x, 0.3F, vec3d3.z);
            }

        // ③ 엘리트라(이코딩) 비행
        } else if (this.isFallFlying()) {
            this.limitFallDistance();
            Vec3d vec3d4 = this.getVelocity();
            Vec3d vec3d5 = this.getRotationVector();
            float fx = this.getPitch() * (float)(Math.PI / 180.0);
            double i = Math.sqrt(vec3d5.x * vec3d5.x + vec3d5.z * vec3d5.z);
            double j = vec3d4.horizontalLength();
            double k = vec3d5.length();
            double l = Math.cos(fx);
            l = l * l * Math.min(1.0, k / 0.4);
            vec3d4 = this.getVelocity().add(0.0, d * (-1.0 + l * 0.75), 0.0);
            if (vec3d4.y < 0.0 && i > 0.0) {
                double m = vec3d4.y * -0.1 * l;
                vec3d4 = vec3d4.add(vec3d5.x * m / i, m, vec3d5.z * m / i);
            }
            if (fx < 0.0F && i > 0.0) {
                double m = j * -MathHelper.sin(fx) * 0.04;
                vec3d4 = vec3d4.add(-vec3d5.x * m / i, m * 3.2, -vec3d5.z * m / i);
            }
            if (i > 0.0) {
                vec3d4 = vec3d4.add((vec3d5.x / i * j - vec3d4.x) * 0.1, 0.0, (vec3d5.z / i * j - vec3d4.z) * 0.1);
            }
            this.setVelocity(vec3d4.multiply(0.99F, 0.98F, 0.99F));
            this.move(MovementType.SELF, this.getVelocity());
            if (this.horizontalCollision && !this.getWorld().isClient) {
                double m = this.getVelocity().horizontalLength();
                double n = j - m;
                float o = (float)(n * 10.0 - 3.0);
                if (o > 0.0F) {
                    this.playSound(this.getFallSound((int)o), 1.0F, 1.0F);
                    this.damage(this.getDamageSources().flyIntoWall(), o);
                }
            }
            if (this.isOnGround() && !this.getWorld().isClient) {
                this.setFlag(Entity.FALL_FLYING_FLAG_INDEX, false);
            }

        // ④ 지상/공중 (기본)
        } else {
            BlockPos blockPos = this.getVelocityAffectingPos();
            float p = this.getWorld().getBlockState(blockPos).getBlock().getSlipperiness();
            float fxx = this.isOnGround() ? p * 0.91F : 0.91F;
            Vec3d vec3d6 = this.applyMovementInput(movementInput, p);
            double q = vec3d6.y;
            if (this.hasStatusEffect(StatusEffects.LEVITATION)) {
                q += (0.05 * (this.getStatusEffect(StatusEffects.LEVITATION).getAmplifier() + 1) - vec3d6.y) * 0.2;
            } else if (!this.getWorld().isClient || this.getWorld().isChunkLoaded(blockPos)) {
                q -= d;
            } else if (this.getY() > this.getWorld().getBottomY()) {
                q = -0.1;
            } else {
                q = 0.0;
            }
            if (this.hasNoDrag()) {
                this.setVelocity(vec3d6.x, q, vec3d6.z);
            } else {
                this.setVelocity(vec3d6.x * fxx, this instanceof Flutterer ? q * fxx : q * 0.98F, vec3d6.z * fxx);
            }
        }
    }

    this.updateLimbs(this instanceof Flutterer);
}
```

---

## 분기 구조 요약

```
travel(movementInput)
  isLogicalSideForUpdatingMovement()
  ├─ true
  │   gravity = getFinalGravity()
  │       hasNoGravity() ? 0.0 : GENERIC_GRAVITY 속성값
  │   falling = velocity.y <= 0.0
  │   SLOW_FALLING 포션 + falling → gravity = min(gravity, 0.01)
  │
  │   ① isTouchingWater() && shouldSwimInFluids() && !canWalkOnFluid()
  │       f(저항) = isSprinting() ? 0.9F : getBaseMovementSpeedMultiplier() [0.8F]
  │       g(가속) = 0.02F
  │       WATER_MOVEMENT_EFFICIENCY 속성 h:
  │           !isOnGround() → h *= 0.5F
  │           h > 0 → f += (0.54600006F - f) * h, g += (movementSpeed - g) * h
  │       DOLPHINS_GRACE → f = 0.96F
  │       updateVelocity(g, input) → move() → 속도 * (f, 0.8F, f)
  │       applyFluidMovingSpeed(gravity, falling, velocity)
  │       수직 충돌 탈출: velocity.y = 0.3F
  │
  │   ② isInLava() && shouldSwimInFluids() && !canWalkOnFluid()
  │       updateVelocity(0.02F, input) → move()
  │       fluidHeight <= swimHeight → multiply(0.5, 0.8, 0.5) + applyFluidMovingSpeed
  │       fluidHeight >  swimHeight → multiply(0.5)
  │       gravity != 0 → velocity.y -= gravity / 4.0
  │       수직 충돌 탈출: velocity.y = 0.3F
  │
  │   ③ isFallFlying() [엘리트라]
  │       limitFallDistance()
  │       피치 기반 양력/항력 계산 → multiply(0.99, 0.98, 0.99) → move()
  │       수평 충돌 + 서버 → 벽 충돌 데미지
  │       onGround + 서버 → FALL_FLYING 플래그 해제
  │
  │   ④ else [지상/공중]
  │       slipperiness = 블록 미끄러움
  │       friction = isOnGround() ? slipperiness * 0.91F : 0.91F
  │       applyMovementInput(input, slipperiness) → vec3d6
  │       q = vec3d6.y
  │       LEVITATION → q 수정
  │       else → q -= gravity
  │       setVelocity(x*friction, Flutterer? q*friction : q*0.98F, z*friction)
  │
  └─ false → (아무것도 안 함)

  updateLimbs(this instanceof Flutterer)  [조건 무관, 항상 호출]
```

---

## 호출되는 메서드 상세

### `Entity.isLogicalSideForUpdatingMovement()` (Yarn: `method_5787`)

```java
public boolean isLogicalSideForUpdatingMovement() {
    return this.getControllingPassenger() instanceof PlayerEntity playerEntity
        ? playerEntity.isMainPlayer()
        : this.canMoveVoluntarily();
}

public boolean canMoveVoluntarily() {
    return !this.getWorld().isClient;
}
```

탑승자가 없으면: 서버 사이드에서만 `true`.  
탑승자가 플레이어면: 그 플레이어가 로컬 플레이어인 클라이언트에서만 `true`.

### `Entity.getFinalGravity()` (Yarn: `method_56989`)

```java
public final double getFinalGravity() {
    return this.hasNoGravity() ? 0.0 : this.getGravity();
}

// LivingEntity.getGravity():
protected double getGravity() {
    return this.getAttributeValue(EntityAttributes.GENERIC_GRAVITY);
}
```

`GENERIC_GRAVITY` 속성값. `hasNoGravity()` 시 0.0.

### `LivingEntity.getBaseMovementSpeedMultiplier()` (Yarn: `method_6120`)

```java
protected float getBaseMovementSpeedMultiplier() {
    return 0.8F;
}
```

물속 저항 기본값. `0.8F` 고정 (subclass에서 오버라이드 가능).

### `LivingEntity.applyMovementInput(Vec3d, float)` (Yarn: `method_26318`)

```java
public Vec3d applyMovementInput(Vec3d movementInput, float slipperiness) {
    this.updateVelocity(this.getMovementSpeed(slipperiness), movementInput);
    this.setVelocity(this.applyClimbingSpeed(this.getVelocity()));
    this.move(MovementType.SELF, this.getVelocity());
    Vec3d vec3d = this.getVelocity();
    if ((this.horizontalCollision || this.jumping)
        && (this.isClimbing() || this.getBlockStateAtPos().isOf(Blocks.POWDER_SNOW)
            && PowderSnowBlock.canWalkOnPowderSnow(this))) {
        vec3d = new Vec3d(vec3d.x, 0.2, vec3d.z);
    }
    return vec3d;
}
```

- `getMovementSpeed(slipperiness)`: 지상→ `movementSpeed * (0.21600002F / slip³)`, 공중→ `getOffGroundSpeed()`
- `applyClimbingSpeed()`: `isClimbing()` 시 x/z를 ±0.15F 클램프, y를 max(y, -0.15F) — 아래로 내려가려 하면 0으로 제한(사다리 홀드 시)

### `LivingEntity.getMovementSpeed(float slipperiness)` (private, Yarn: `method_18802`)

```java
private float getMovementSpeed(float slipperiness) {
    return this.isOnGround()
        ? this.getMovementSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness))
        : this.getOffGroundSpeed();
}
```

지상: `movementSpeed * 0.21600002 / slip³`. 공중: `getOffGroundSpeed()`.

`0.21600002F` = `0.6³` (기본 미끄러움 0.6 기준 정규화).

### `LivingEntity.getOffGroundSpeed()` (Yarn: `method_49484`)

```java
protected float getOffGroundSpeed() {
    return this.getControllingPassenger() instanceof PlayerEntity ? this.getMovementSpeed() * 0.1F : 0.02F;
}
```

공중 가속: 플레이어 조종 탑승 시 `movementSpeed * 0.1`, 아니면 `0.02F`.

### `LivingEntity.applyFluidMovingSpeed(double, boolean, Vec3d)` (Yarn: `method_26317`)

```java
public Vec3d applyFluidMovingSpeed(double gravity, boolean falling, Vec3d motion) {
    if (gravity != 0.0 && !this.isSprinting()) {
        double d;
        if (falling && Math.abs(motion.y - 0.005) >= 0.003 && Math.abs(motion.y - gravity / 16.0) < 0.003) {
            d = -0.003;
        } else {
            d = motion.y - gravity / 16.0;
        }
        return new Vec3d(motion.x, d, motion.z);
    } else {
        return motion;
    }
}
```

스프린트 중이거나 gravity == 0이면 변경 없음.  
아니면 `y -= gravity / 16.0`. 단, `falling && |y-0.005| >= 0.003 && |y - gravity/16| < 0.003` → `y = -0.003` (수면 부유 보정).

### `LivingEntity.applyClimbingSpeed(Vec3d)` (private, Yarn: `method_18801`)

```java
private Vec3d applyClimbingSpeed(Vec3d motion) {
    if (this.isClimbing()) {
        this.onLanding();
        float f = 0.15F;
        double d = MathHelper.clamp(motion.x, -0.15F, 0.15F);
        double e = MathHelper.clamp(motion.z, -0.15F, 0.15F);
        double g = Math.max(motion.y, -0.15F);
        if (g < 0.0 && !this.getBlockStateAtPos().isOf(Blocks.SCAFFOLDING)
            && this.isHoldingOntoLadder() && this instanceof PlayerEntity) {
            g = 0.0;
        }
        motion = new Vec3d(d, g, e);
    }
    return motion;
}
```

`isClimbing()` 시: x/z ±0.15F 클램프, y max(-0.15F). 플레이어가 사다리(비 비계)에서 홀드 중이면 y=0 (하강 방지).

`isHoldingOntoLadder()`:
```java
public boolean isHoldingOntoLadder() {
    return this.isSneaking();
}
```

### `LivingEntity.updateLimbs(boolean)` (Yarn: `method_29242`)

```java
public void updateLimbs(boolean flutter) {
    float f = (float)MathHelper.magnitude(this.getX() - this.prevX,
        flutter ? this.getY() - this.prevY : 0.0,
        this.getZ() - this.prevZ);
    this.updateLimbs(f);
}

protected void updateLimbs(float posDelta) {
    float f = Math.min(posDelta * 4.0F, 1.0F);
    this.limbAnimator.updateLimbs(f, 0.4F);
}
```

이동 거리(델타)로 `limbAnimator` 업데이트. `flutter==false`이면 y 델타 제외(수평 이동만).  
`limbAnimator.updateLimbs(speed, blendFactor=0.4F)`.

### `Entity.updateVelocity(float, Vec3d)` (Yarn 확인 필요)

```java
public void updateVelocity(float speed, Vec3d movementInput) {
    Vec3d vec3d = movementInputToVelocity(movementInput, speed, this.getYaw());
    this.setVelocity(this.getVelocity().add(vec3d));
}

private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
    double d = movementInput.lengthSquared();
    if (d < 1.0E-7) {
        return Vec3d.ZERO;
    } else {
        Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);
        float f = MathHelper.sin(yaw * (float)(Math.PI / 180.0));
        float g = MathHelper.cos(yaw * (float)(Math.PI / 180.0));
        return new Vec3d(vec3d.x * g - vec3d.z * f, vec3d.y, vec3d.z * g + vec3d.x * f);
    }
}
```

입력 벡터를 yaw 방향으로 회전시켜 속도에 더함. 입력 크기 > 1.0이면 정규화 후 곱.

---

## 중요 수치 목록

| 수치 | 위치 | 의미 |
|------|------|------|
| `0.01` | SLOW_FALLING 최소 중력 | gravity = min(gravity, 0.01) |
| `0.9F` | 물속, 스프린트 저항 | f = 0.9F |
| `0.8F` | `getBaseMovementSpeedMultiplier()` | 물속 기본 저항 |
| `0.02F` | 물속/용암 기본 가속도 | g = 0.02F |
| `0.54600006F` | 물속 WATER_MOVEMENT_EFFICIENCY 보간 목표 | `f += (0.54600006F - f) * h` |
| `0.96F` | DOLPHINS_GRACE 저항 | f = 0.96F |
| `0.8F` | 물속 y 감속 | `multiply(f, 0.8F, f)` |
| `0.3F` | 수직 충돌 탈출 속도 | velocity.y = 0.3F |
| `0.6F` | 수직 충돌 탈출 판정 오프셋 | `vec3d.y + 0.6F - getY() + e` |
| `0.5` | 용암 내부 속도 감속 | `multiply(0.5)` |
| `0.5, 0.8, 0.5` | 용암 표면 속도 감속 | `multiply(0.5, 0.8F, 0.5)` |
| `4.0` | 용암 중력 감쇠 | `velocity.y -= gravity / 4.0` |
| `0.4` | 엘리트라 최소 k/length 기준 | `Math.min(1.0, k / 0.4)` |
| `0.75` | 엘리트라 양력 계수 | `l * 0.75` |
| `0.1` | 엘리트라 음의 피치 계수 | `* -0.1 * l` |
| `3.2` | 엘리트라 음의 피치 y 계수 | `m * 3.2` |
| `0.04` | 엘리트라 음의 피치 항력 계수 | `* 0.04` |
| `0.1` | 엘리트라 수평 방향 수렴 계수 | `(vec3d5.x/i*j - vec3d4.x) * 0.1` |
| `0.99F, 0.98F, 0.99F` | 엘리트라 공기 저항 | `multiply(0.99F, 0.98F, 0.99F)` |
| `10.0, 3.0` | 엘리트라 벽 충돌 데미지 계산 | `(n * 10.0 - 3.0)` |
| `0.91F` | 지상/공중 마찰 기준 | `friction = isOnGround() ? slip * 0.91F : 0.91F` |
| `0.05` | LEVITATION 상승 계수 | `0.05 * (amplifier + 1)` |
| `0.2` | LEVITATION 보간 속도 | `* 0.2` |
| `0.98F` | 공중 y 감속 | `q * 0.98F` |
| `0.21600002F` | 지상 속도 정규화 상수 | `movementSpeed * 0.21600002 / slip³` |
| `0.15F` | 클라이밍 x/z 클램프 | `MathHelper.clamp(motion.x, -0.15F, 0.15F)` |
| `4.0F` | limbAnimator 속도 스케일 | `posDelta * 4.0F` |
| `0.4F` | limbAnimator 블렌드 계수 | `limbAnimator.updateLimbs(f, 0.4F)` |
| `1.0E-7` | updateVelocity 입력 최소값 | `movementInput.lengthSquared() < 1.0E-7 → Vec3d.ZERO` |

---

## SmartMoving와의 충돌 가능성

| 항목 | vanilla 동작 | SM 간섭 예상 위치 | 충돌 여부 |
|------|------------|-----------------|---------|
| `travel()` 자체 오버라이드 | PlayerEntity가 오버라이드 | SM은 PlayerBase에서 `travel()` 오버라이드 | 충돌 없음 (PlayerAPI/Mixin 방식에 따라 다름) |
| `applyClimbingSpeed()` 클라이밍 판정 | `isClimbing()` = CLIMBABLE 블록 또는 trapdoor | SM의 손으로 기어오르기는 vanilla `isClimbing()` 아님 | SM에서 `isClimbing()` 재정의 또는 별도 처리 필요 |
| `applyMovementInput()` 내 climbing 분기 | `horizontalCollision || jumping` + `isClimbing()` → y=0.2 | SM 클라이밍 시 이 분기가 발동될 수 있음 | 확인 필요 |
| `updateLimbs()` 호출 | `travel()` 마지막에 무조건 호출 | SM 렌더 애니메이션과 독립 | 충돌 없음 (SM은 `setRotationAngles()`에서 별도 처리) |
| 지상 마찰 `0.91F` | 지상 속도 감쇠 | SM 슬라이딩/크롤링 시 마찰 처리 달라야 함 | 확인 필요 |
| `isLogicalSideForUpdatingMovement()` | 서버 또는 로컬 플레이어만 실행 | SM 이동 계산 위치와 동일 조건이어야 함 | SM 원본도 동일 조건 사용 확인 필요 |

---

## 우리와의 충돌 가능성 상세

**`applyClimbingSpeed()` 충돌**:  
vanilla `isClimbing()`은 CLIMBABLE 태그 블록(사다리, 덩굴 등) + trapdoor만 인식한다. SM의 손으로 기어오르기(`HandsClimbing`)나 천장 클라이밍은 이 조건을 통과하지 못한다. SM에서 `isClimbing()`을 오버라이드하거나, `applyClimbingSpeed()`에 Mixin을 주입해야 한다.

**`applyMovementInput()` 내 y=0.2 분기**:  
`horizontalCollision || jumping` 조건과 `isClimbing()` 조건이 동시에 만족되면 `y=0.2`가 강제된다. SM 클라이밍 중 점프 키 입력 시 이 분기가 발동할 수 있다.
