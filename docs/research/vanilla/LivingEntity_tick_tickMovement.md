# LivingEntity.tick() / tickMovement() — vanilla 1.21.1 리서치

Yarn 버전: `net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2`  
소스: IntelliJ 디컴파일 캐시 `AppData/Local/Temp/net/minecraft/entity/LivingEntity.java`  
실행 위치: 클라이언트 + 서버 (조건에 따라 분기)

---

## Yarn 이름 확인 (mappings.tiny 직접 확인)

| Yarn 이름 | intermediary | 디스크립터 | 비고 |
|-----------|-------------|-----------|------|
| `tick` | `method_5773` | `()V` | LivingEntity.tick() |
| `tickMovement` | `method_6007` | `()V` | LivingEntity.tickMovement() |
| `tickActiveItemStack` | `method_6076` | `()V` | private |
| `updateLeaningPitch` | `method_6072` | `()V` | private |
| `turnHead` | `method_6031` | `(FF)F` | protected |
| `getMaxRelativeHeadRotation` | `method_53964` | `()F` | protected |
| `tickFallFlying` | `method_6053` | `()V` | private |
| `tickNewAi` | `method_6023` | `()V` | protected |
| `tickCramming` | `method_6070` | `()V` | protected |
| `tickRiptide` | `method_6035` | `(Lewx;Lewx;)V` | protected |
| `jump` | `method_6043` | `()V` | public |
| `swimUpward` | `method_6010` | `(Lawu;)V` | protected |
| `lerpPosAndRotation` | `method_52532` | `(IDDDDD)V` | |
| `lerpHeadYaw` | `method_52539` | `(ID)V` | |
| `isImmobile` | `method_6062` | `()Z` | |
| `updateAttributes` | `method_52543` | `()V` | |
| `getJumpVelocity` | (method_6043 참조) | `()F` | protected |

---

## `LivingEntity.tick()` 전체 코드

```java
public void tick() {
    super.tick();
    this.tickActiveItemStack();
    this.updateLeaningPitch();

    if (!this.getWorld().isClient) {
        // 화살 박힌 카운터 처리
        int i = this.getStuckArrowCount();
        if (i > 0) {
            if (this.stuckArrowTimer <= 0) {
                this.stuckArrowTimer = 20 * (30 - i);
            }
            this.stuckArrowTimer--;
            if (this.stuckArrowTimer <= 0) {
                this.setStuckArrowCount(i - 1);
            }
        }

        // 독침 카운터 처리
        int j = this.getStingerCount();
        if (j > 0) {
            if (this.stuckStingerTimer <= 0) {
                this.stuckStingerTimer = 20 * (30 - j);
            }
            this.stuckStingerTimer--;
            if (this.stuckStingerTimer <= 0) {
                this.setStingerCount(j - 1);
            }
        }

        this.sendEquipmentChanges();

        if (this.age % 20 == 0) {
            this.getDamageTracker().update();
        }

        if (this.isSleeping() && !this.isSleepingInBed()) {
            this.wakeUp();
        }
    }

    if (!this.isRemoved()) {
        this.tickMovement();
    }

    // 이동 거리 계산 (stepBobbing)
    double d = this.getX() - this.prevX;
    double e = this.getZ() - this.prevZ;
    float f = (float)(d * d + e * e);
    float g = this.bodyYaw;
    float h = 0.0F;
    this.prevStepBobbingAmount = this.stepBobbingAmount;
    float k = 0.0F;
    if (f > 0.0025000002F) {
        k = 1.0F;
        h = (float)Math.sqrt(f) * 3.0F;
        float l = (float)MathHelper.atan2(e, d) * (180.0F / (float)Math.PI) - 90.0F;
        float m = MathHelper.abs(MathHelper.wrapDegrees(this.getYaw()) - l);
        if (95.0F < m && m < 265.0F) {
            g = l - 180.0F;
        } else {
            g = l;
        }
    }

    if (this.handSwingProgress > 0.0F) {
        g = this.getYaw();
    }

    if (!this.isOnGround()) {
        k = 0.0F;
    }

    this.stepBobbingAmount = this.stepBobbingAmount + (k - this.stepBobbingAmount) * 0.3F;

    // 머리 회전
    this.getWorld().getProfiler().push("headTurn");
    h = this.turnHead(g, h);
    this.getWorld().getProfiler().pop();

    // yaw/pitch 각도 범위 정규화 (-180~180)
    this.getWorld().getProfiler().push("rangeChecks");
    while (this.getYaw() - this.prevYaw < -180.0F) this.prevYaw -= 360.0F;
    while (this.getYaw() - this.prevYaw >= 180.0F) this.prevYaw += 360.0F;
    while (this.bodyYaw - this.prevBodyYaw < -180.0F) this.prevBodyYaw -= 360.0F;
    while (this.bodyYaw - this.prevBodyYaw >= 180.0F) this.prevBodyYaw += 360.0F;
    while (this.getPitch() - this.prevPitch < -180.0F) this.prevPitch -= 360.0F;
    while (this.getPitch() - this.prevPitch >= 180.0F) this.prevPitch += 360.0F;
    while (this.headYaw - this.prevHeadYaw < -180.0F) this.prevHeadYaw -= 360.0F;
    while (this.headYaw - this.prevHeadYaw >= 180.0F) this.prevHeadYaw += 360.0F;
    this.getWorld().getProfiler().pop();

    this.lookDirection += h;

    // 엘리트라 틱 카운터
    if (this.isFallFlying()) {
        this.fallFlyingTicks++;
    } else {
        this.fallFlyingTicks = 0;
    }

    // 수면 중 pitch 고정
    if (this.isSleeping()) {
        this.setPitch(0.0F);
    }

    this.updateAttributes();

    // 스케일 변화 시 히트박스 재계산
    float l = this.getScale();
    if (l != this.prevScale) {
        this.prevScale = l;
        this.calculateDimensions();
    }
}
```

---

## `tick()` 호출 구조 요약

```
tick()
  super.tick()                          [Entity.tick()]
  tickActiveItemStack()                 [아이템 사용 틱 처리]
  updateLeaningPitch()                  [수영 자세 보간]
  [서버 전용]
    화살/독침 타이머 감소
    sendEquipmentChanges()
    age % 20 → getDamageTracker().update()
    isSleeping && !isSleepingInBed → wakeUp()
  [!isRemoved]
    tickMovement()                      ← 핵심 이동 처리
  [이동량 기반 stepBobbing 계산]
    f = dx²+dz²
    f > 0.0025000002F → k=1, h=sqrt(f)*3, bodyYaw 방향 계산
    handSwingProgress > 0 → g = yaw
    !isOnGround → k = 0
    stepBobbingAmount = lerp(k, 0.3F)
  turnHead(g, h)
  rangeChecks: yaw/pitch/bodyYaw/headYaw 360° 정규화
  lookDirection += h
  fallFlyingTicks ++/reset
  isSleeping → setPitch(0)
  updateAttributes()
  scale 변화 → calculateDimensions()
```

---

## `LivingEntity.tickMovement()` 전체 코드

```java
public void tickMovement() {
    if (this.jumpingCooldown > 0) {
        this.jumpingCooldown--;
    }

    // 이동 권한 측 처리
    if (this.isLogicalSideForUpdatingMovement()) {
        this.bodyTrackingIncrements = 0;
        this.updateTrackedPosition(this.getX(), this.getY(), this.getZ());
    }

    // 클라이언트: 서버에서 받은 위치로 보간
    if (this.bodyTrackingIncrements > 0) {
        this.lerpPosAndRotation(this.bodyTrackingIncrements, this.serverX, this.serverY, this.serverZ,
            this.serverYaw, this.serverPitch);
        this.bodyTrackingIncrements--;
    } else if (!this.canMoveVoluntarily()) {
        this.setVelocity(this.getVelocity().multiply(0.98));
    }

    if (this.headTrackingIncrements > 0) {
        this.lerpHeadYaw(this.headTrackingIncrements, this.serverHeadYaw);
        this.headTrackingIncrements--;
    }

    // 매우 작은 속도 0으로 정리 (스냅)
    Vec3d vec3d = this.getVelocity();
    double d = vec3d.x;
    double e = vec3d.y;
    double f = vec3d.z;
    if (Math.abs(vec3d.x) < 0.003) d = 0.0;
    if (Math.abs(vec3d.y) < 0.003) e = 0.0;
    if (Math.abs(vec3d.z) < 0.003) f = 0.0;
    this.setVelocity(d, e, f);

    // AI/이동 입력
    this.getWorld().getProfiler().push("ai");
    if (this.isImmobile()) {
        this.jumping = false;
        this.sidewaysSpeed = 0.0F;
        this.forwardSpeed = 0.0F;
    } else if (this.canMoveVoluntarily()) {
        this.getWorld().getProfiler().push("newAi");
        this.tickNewAi();
        this.getWorld().getProfiler().pop();
    }
    this.getWorld().getProfiler().pop();

    // 점프 처리
    this.getWorld().getProfiler().push("jump");
    if (this.jumping && this.shouldSwimInFluids()) {
        double g;
        if (this.isInLava()) {
            g = this.getFluidHeight(FluidTags.LAVA);
        } else {
            g = this.getFluidHeight(FluidTags.WATER);
        }
        boolean bl = this.isTouchingWater() && g > 0.0;
        double h = this.getSwimHeight();
        if (!bl || this.isOnGround() && !(g > h)) {
            if (!this.isInLava() || this.isOnGround() && !(g > h)) {
                if ((this.isOnGround() || bl && g <= h) && this.jumpingCooldown == 0) {
                    this.jump();
                    this.jumpingCooldown = 10;
                }
            } else {
                this.swimUpward(FluidTags.LAVA);
            }
        } else {
            this.swimUpward(FluidTags.WATER);
        }
    } else {
        this.jumpingCooldown = 0;
    }
    this.getWorld().getProfiler().pop();

    // 이동 (travel)
    this.getWorld().getProfiler().push("travel");
    this.sidewaysSpeed *= 0.98F;
    this.forwardSpeed *= 0.98F;
    this.tickFallFlying();
    Box box = this.getBoundingBox();
    Vec3d vec3d2 = new Vec3d(this.sidewaysSpeed, this.upwardSpeed, this.forwardSpeed);
    if (this.hasStatusEffect(StatusEffects.SLOW_FALLING) || this.hasStatusEffect(StatusEffects.LEVITATION)) {
        this.onLanding();
    }
    if (this.getControllingPassenger() instanceof PlayerEntity playerEntity && this.isAlive()) {
        this.travelControlled(playerEntity, vec3d2);
    } else {
        this.travel(vec3d2);
    }
    this.getWorld().getProfiler().pop();

    // 가루 눈 동결
    this.getWorld().getProfiler().push("freezing");
    if (!this.getWorld().isClient && !this.isDead()) {
        int i = this.getFrozenTicks();
        if (this.inPowderSnow && this.canFreeze()) {
            this.setFrozenTicks(Math.min(this.getMinFreezeDamageTicks(), i + 1));
        } else {
            this.setFrozenTicks(Math.max(0, i - 2));
        }
    }
    this.removePowderSnowSlow();
    this.addPowderSnowSlowIfNeeded();
    if (!this.getWorld().isClient && this.age % 40 == 0 && this.isFrozen() && this.canFreeze()) {
        this.damage(this.getDamageSources().freeze(), 1.0F);
    }
    this.getWorld().getProfiler().pop();

    // 리프타이드 (삼지창)
    this.getWorld().getProfiler().push("push");
    if (this.riptideTicks > 0) {
        this.riptideTicks--;
        this.tickRiptide(box, this.getBoundingBox());
    }
    this.tickCramming();
    this.getWorld().getProfiler().pop();

    // 물에 의한 피해
    if (!this.getWorld().isClient && this.hurtByWater() && this.isWet()) {
        this.damage(this.getDamageSources().drown(), 1.0F);
    }
}
```

---

## `tickMovement()` 호출 구조 요약

```
tickMovement()
  jumpingCooldown-- (> 0)
  isLogicalSideForUpdatingMovement() → bodyTrackingIncrements=0, updateTrackedPosition()
  bodyTrackingIncrements > 0 → lerpPosAndRotation(), bodyTrackingIncrements--
  else !canMoveVoluntarily() → velocity *= 0.98 (클라이언트 보간 중 아님)
  headTrackingIncrements > 0 → lerpHeadYaw(), headTrackingIncrements--

  [속도 스냅] |vx/vy/vz| < 0.003 → 0.0

  [ai]
    isImmobile() → jumping=false, sideways=0, forward=0
    canMoveVoluntarily() → tickNewAi()

  [jump]
    jumping && shouldSwimInFluids():
      isTouchingWater && fluidHeight > 0 → swimUpward(WATER)
      isInLava && fluidHeight > swimHeight → swimUpward(LAVA)
      (isOnGround || isTouchingWater && fluidHeight<=swimHeight) && jumpingCooldown==0:
        jump(), jumpingCooldown=10
    !jumping → jumpingCooldown=0

  [travel]
    sidewaysSpeed *= 0.98F
    forwardSpeed  *= 0.98F
    tickFallFlying()
    movementInput = Vec3d(sidewaysSpeed, upwardSpeed, forwardSpeed)
    SLOW_FALLING || LEVITATION → onLanding()
    controllingPassenger is PlayerEntity && isAlive() → travelControlled()
    else → travel(movementInput)

  [freezing] 가루눈 동결 처리 (서버만)
  removePowderSnowSlow(), addPowderSnowSlowIfNeeded()
  age % 40 && isFrozen && canFreeze → damage(freeze, 1.0F) (서버만)

  [push]
    riptideTicks > 0 → tickRiptide()
    tickCramming()

  !isClient && hurtByWater() && isWet() → damage(drown, 1.0F)
```

---

## 호출되는 주요 메서드 상세

### `tickActiveItemStack()` (private, Yarn: `method_6076`)

```java
private void tickActiveItemStack() {
    if (this.isUsingItem()) {
        if (ItemStack.areItemsEqual(this.getStackInHand(this.getActiveHand()), this.activeItemStack)) {
            this.activeItemStack = this.getStackInHand(this.getActiveHand());
            this.tickItemStackUsage(this.activeItemStack);
        } else {
            this.clearActiveItem();
        }
    }
}
```

아이템 사용 중이면 손의 아이템이 바뀌었는지 확인하고, 동일하면 `tickItemStackUsage()`, 달라졌으면 `clearActiveItem()`.

### `updateLeaningPitch()` (private, Yarn: `method_6072`)

```java
private void updateLeaningPitch() {
    this.lastLeaningPitch = this.leaningPitch;
    if (this.isInSwimmingPose()) {
        this.leaningPitch = Math.min(1.0F, this.leaningPitch + 0.09F);
    } else {
        this.leaningPitch = Math.max(0.0F, this.leaningPitch - 0.09F);
    }
}
```

`leaningPitch`: 0.0(직립) ~ 1.0(수평 수영 자세). `isInSwimmingPose()` 시 +0.09F/틱, 아니면 -0.09F/틱. 완전 전환에 약 11틱(0.55초).

### `turnHead(float bodyRotation, float headRotation)` (protected, Yarn: `method_6031`)

```java
protected float turnHead(float bodyRotation, float headRotation) {
    float f = MathHelper.wrapDegrees(bodyRotation - this.bodyYaw);
    this.bodyYaw += f * 0.3F;
    float g = MathHelper.wrapDegrees(this.getYaw() - this.bodyYaw);
    float h = this.getMaxRelativeHeadRotation();   // 기본 50.0F
    if (Math.abs(g) > h) {
        this.bodyYaw = this.bodyYaw + (g - MathHelper.sign(g) * h);
    }
    boolean bl = g < -90.0F || g >= 90.0F;
    if (bl) {
        headRotation *= -1.0F;
    }
    return headRotation;
}
```

- `bodyYaw`를 목표 방향으로 0.3F 비율로 보간
- yaw와 bodyYaw의 차이가 `maxRelativeHeadRotation`(50°) 초과 시 bodyYaw 강제 이동
- 머리가 뒤를 향하면(|g|≥90°) headRotation 부호 반전

### `tickFallFlying()` (private, Yarn: `method_6053`)

```java
private void tickFallFlying() {
    boolean bl = this.getFlag(Entity.FALL_FLYING_FLAG_INDEX);
    if (bl && !this.isOnGround() && !this.hasVehicle() && !this.hasStatusEffect(StatusEffects.LEVITATION)) {
        ItemStack itemStack = this.getEquippedStack(EquipmentSlot.CHEST);
        if (itemStack.isOf(Items.ELYTRA) && ElytraItem.isUsable(itemStack)) {
            bl = true;
            int i = this.fallFlyingTicks + 1;
            if (!this.getWorld().isClient && i % 10 == 0) {
                int j = i / 10;
                if (j % 2 == 0) {
                    itemStack.damage(1, this, EquipmentSlot.CHEST);
                }
                this.emitGameEvent(GameEvent.ELYTRA_GLIDE);
            }
        } else {
            bl = false;
        }
    } else {
        bl = false;
    }
    if (!this.getWorld().isClient) {
        this.setFlag(Entity.FALL_FLYING_FLAG_INDEX, bl);
    }
}
```

엘리트라 비행 유효성 체크. 조건: FALL_FLYING 플래그 && !지상 && !탑승 && !LEVITATION && 엘리트라 착용 && 내구 있음.  
서버: 20틱마다(i%10==0 && j%2==0) 내구 -1. 이벤트 발행.  
FALL_FLYING 플래그는 서버에서만 설정.

### `jump()` (public, Yarn: `method_6043`)

```java
public void jump() {
    float f = this.getJumpVelocity();
    if (!(f <= 1.0E-5F)) {
        Vec3d vec3d = this.getVelocity();
        this.setVelocity(vec3d.x, f, vec3d.z);
        if (this.isSprinting()) {
            float g = this.getYaw() * (float)(Math.PI / 180.0);
            this.addVelocityInternal(new Vec3d(-MathHelper.sin(g) * 0.2, 0.0, MathHelper.cos(g) * 0.2));
        }
        this.velocityDirty = true;
    }
}
```

`getJumpVelocity()` = `GENERIC_JUMP_STRENGTH` 속성 * 1.0F * `getJumpVelocityMultiplier()` + `getJumpBoostVelocityModifier()`  
- JUMP_BOOST 포션: +0.1F * (레벨+1)  
스프린트 중: yaw 방향으로 `(-sin(yaw)*0.2, 0, cos(yaw)*0.2)` 추가.

### 점프 조건 분기 (tickMovement 내)

```
jumping && shouldSwimInFluids():
  isTouchingWater && fluidHeight > 0:
    → swimUpward(WATER)  [+0.04 y velocity]
  isInLava && fluidHeight > swimHeight:
    → swimUpward(LAVA)   [+0.04 y velocity]
  (isOnGround || isTouchingWater && fluidHeight <= swimHeight) && jumpingCooldown == 0:
    → jump(), jumpingCooldown = 10
```

`swimHeight` = `standingEyeHeight < 0.4 ? 0.0 : 0.4`

---

## 관련 필드 (LivingEntity 선언부)

```java
public float sidewaysSpeed;          // line 221 — 좌우 이동 입력
public float upwardSpeed;            // line 222 — 위아래 이동 입력
public float forwardSpeed;           // line 223 — 전진 이동 입력
protected int bodyTrackingIncrements;// line 224 — 클라이언트 위치 보간 남은 틱
protected double serverX/Y/Z;        // line 225-227 — 서버에서 받은 목표 위치
protected double serverHeadYaw;      // line 230
protected int headTrackingIncrements;// line 231
protected float stepBobbingAmount;   // line 214 — 걸음 보빙 애니메이션 양
protected float lookDirection;       // line 215 — 머리 회전 누적 방향
protected int fallFlyingTicks;       // line 244 — 엘리트라 비행 틱 카운터
private float leaningPitch;          // line 254 — 수영 자세 보간값 (0~1)
private float lastLeaningPitch;      // line 255
private int jumpingCooldown;         // line 240 — 점프 쿨다운 (jump() 후 10틱)
```

---

## 중요 수치 목록

| 수치 | 위치 | 의미 |
|------|------|------|
| `0.0025000002F` | tick() stepBobbing 임계값 | `dx²+dz² > 0.0025000002F` → 걷는 중 |
| `3.0F` | tick() h 계산 | `h = sqrt(f) * 3.0F` |
| `95.0F`, `265.0F` | tick() bodyYaw 방향 판정 | 역방향 이동 감지 각도 |
| `0.3F` | tick() stepBobbingAmount lerp | `(k - stepBobbing) * 0.3F` |
| `0.3F` | turnHead() bodyYaw 보간 | `bodyYaw += f * 0.3F` |
| `50.0F` | getMaxRelativeHeadRotation() | head-body 최대 각도 차이 |
| `0.003` | tickMovement() 속도 스냅 | `|v| < 0.003 → 0` |
| `0.98` | tickMovement() 보간 중 속도 감쇠 | `velocity.multiply(0.98)` |
| `0.98F` | tickMovement() 이동 입력 감쇠 | `sidewaysSpeed *= 0.98F`, `forwardSpeed *= 0.98F` |
| `10` | tickMovement() jumpingCooldown | jump() 후 10틱 쿨다운 |
| `0.09F` | updateLeaningPitch() | 수영 자세 전환 속도 (약 11틱) |
| `1.0E-5F` | jump() 최소 점프속도 | `f <= 1.0E-5F` → 점프 안 함 |
| `0.2` | jump() 스프린트 점프 추가 속도 | `sin/cos * 0.2` |
| `0.04F` | swimUpward() y 속도 | `velocity.add(0, 0.04F, 0)` |
| `20` | tick() age % 20 | DamageTracker 업데이트 주기 |
| `40` | tickMovement() age % 40 | 동결 데미지 주기 |
| `1.0F` | 동결/익사 데미지 | `damage(freeze/drown, 1.0F)` |

---

## 한 틱의 실행 순서 (이동 관련)

```
Entity.tick() [super]
LivingEntity.tick()
  tickActiveItemStack()
  updateLeaningPitch()
  [서버] sendEquipmentChanges, DamageTracker, wakeUp
  tickMovement()
    jumpingCooldown--
    [이동권한 측] bodyTrackingIncrements=0, updateTrackedPosition
    [클라이언트] lerpPosAndRotation OR velocity*0.98
    속도 스냅 (|v| < 0.003 → 0)
    [AI/이동 입력] isImmobile: reset / canMoveVoluntarily: tickNewAi
    [점프] jump() / swimUpward() / jumpingCooldown=0
    sidewaysSpeed *= 0.98, forwardSpeed *= 0.98
    tickFallFlying()
    movementInput = Vec3d(sideways, upward, forward)
    SLOW_FALLING||LEVITATION → onLanding()
    travel(movementInput) 또는 travelControlled()
    [동결] 가루눈 처리
    riptide, tickCramming
    [익사 피해]
  stepBobbingAmount 계산
  turnHead()
  각도 정규화
  fallFlyingTicks 갱신
  isSleeping → setPitch(0)
  updateAttributes()
  scale 변화 → calculateDimensions()
```

---

## SmartMoving와의 충돌 가능성

| 항목 | vanilla 동작 | SM 간섭 예상 위치 | 충돌 여부 |
|------|------------|-----------------|---------|
| `tickMovement()` 이동 입력 구성 | `Vec3d(sidewaysSpeed, upwardSpeed, forwardSpeed)` → `travel()` | SM은 이 입력값을 수정하거나 `travel()` 자체를 오버라이드 | SM PlayerBase에서 `tickMovement()` 오버라이드 여부 확인 필요 |
| `jump()` 조건: `jumpingCooldown == 0` | 점프 후 10틱 쿨다운 | SM의 클라이밍 점프, 헤드점프 등 별도 점프 로직 | `jumpingCooldown`을 SM이 리셋하거나 무시할 수 있음 |
| `sidewaysSpeed *= 0.98F` | `travel()` 직전 감쇠 | SM이 이동 속도를 직접 설정하면 이 감쇠에 영향 받음 | SM 이동 입력 처리 방식 확인 필요 |
| `updateLeaningPitch()` | `isInSwimmingPose()` 기반 | SM 수영/다이빙 상태와 vanilla 수영 자세 판정 겹칠 수 있음 | `isInSwimmingPose()` 오버라이드 여부 확인 필요 |
| `tickFallFlying()` | FALL_FLYING 플래그 서버 설정 | SM의 비행(isFlying) 상태와 구분 필요 | vanilla elytra와 SM flying은 별개 플래그 — 충돌 없음 |
| 속도 스냅 `< 0.003` | 매우 작은 속도 제거 | SM 클라이밍/천장클라이밍의 작은 속도 제거될 수 있음 | 확인 필요 |
| `canMoveVoluntarily()` → `tickNewAi()` | 서버에서만 AI 실행 | SM 이동 로직 실행 위치와 동기 필요 | SM 원본도 서버 사이드 로직은 서버에서 실행 |
