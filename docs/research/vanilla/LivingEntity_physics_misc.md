# vanilla LivingEntity / Entity 물리 기타 — R-08 리서치

소스: Fabric Loom 디컴파일 (Vineflower 1.11.1)  
Yarn: `net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2`  
확인 파일:
- `common-unpicked.jar` → `LivingEntity.class`, `Entity.class`, `EntityAttributes.class`, `Block.class`, `AbstractBlock.class`

---

## B-05 — `updateLeaningPitch()` (확인됨)

```java
// LivingEntity.java
private void updateLeaningPitch() {
    this.lastLeaningPitch = this.leaningPitch;
    if (this.isInSwimmingPose()) {
        this.leaningPitch = Math.min(1.0F, this.leaningPitch + 0.09F);
    } else {
        this.leaningPitch = Math.max(0.0F, this.leaningPitch - 0.09F);
    }
}
```

- 호출: `LivingEntity.tick()` 매 틱
- 증가 조건: `isInSwimmingPose()` (SWIMMING 포즈 또는 FALL_FLYING 포즈)
- 증가량: +0.09F / 감소량: -0.09F
- 범위: 0.0F ~ 1.0F
- `lastLeaningPitch`: 이전 틱 값 보관 (렌더링 보간용)

**SM 관련성**: 크롤링(`isCrawling`) 시 SWIMMING 포즈를 사용하면 leaningPitch가 1.0F까지 올라간다. 렌더러에서 leaningPitch 기반 보간이 발생하므로 SM 렌더링 Mixin에서 주의 필요.

---

## B-10 — `updateMovementInFluid()` 물 가속 크기 (확인됨)

```java
// Entity.java
private static final double SPEED_IN_WATER = 0.014;

// checkWaterState() 내부:
this.updateMovementInFluid(FluidTags.WATER, 0.014)
```

**물 흐름 가속 크기: `0.014`** (Yarn: `SPEED_IN_WATER` 상수)

`updateMovementInFluid(TagKey<Fluid> fluidTag, double speed)` 내부 핵심:
```java
// speed = 0.014
vec3d = vec3d.normalize();  // 흐름 방향 정규화
// PlayerEntity는 normalize() 건너뜀:
// if (!(this instanceof PlayerEntity)) { vec3d = vec3d.normalize(); }
this.setVelocity(this.getVelocity().add(vec3d.multiply(speed)));
```

- 최소 임계값: 흐름 길이 < 0.003 → 0.0045로 대체 처리
- PlayerEntity: `normalize()` 적용 안 됨 (그대로 multiply)

**SM 관련성**: SM 원본 `handleMaterialAcceleration` 대응. 1.21.1에서는 `updateMovementInFluid(FluidTags.WATER, 0.014)`로 동일 역할.

---

## B-11 — `reverseHandleMaterialAcceleration` 1.21.1 대응 (확인됨)

1.21.1 `LivingEntity.java` 및 `Entity.java` 전체 검색 결과:  
**해당 코드(reverseHandleMaterialAcceleration) 존재하지 않음**.

SM 원본의 `reverseHandleMaterialAcceleration`은 1.7.10 전용 코드 패턴이며, 1.21.1에 대응 API 없음.

---

## B-12 — `GENERIC_GRAVITY` 속성 기본값 (확인됨)

```java
// EntityAttributes.java
public static final RegistryEntry<EntityAttribute> GENERIC_GRAVITY = register(
    "generic.gravity",
    new ClampedEntityAttribute("attribute.name.generic.gravity", 0.08, -1.0, 1.0)
        .setTracked(true)
        .setCategory(Category.NEUTRAL)
);
```

| 항목 | 값 |
|------|-----|
| 기본값 | **0.08** |
| 최솟값 | -1.0 |
| 최댓값 | 1.0 |
| Tracked | true (클라이언트 동기화됨) |

**호출 경로**:
```java
// Entity.getFinalGravity()
public final double getFinalGravity() {
    return this.hasNoGravity() ? 0.0 : this.getGravity();
}
// LivingEntity.getGravity()
protected double getGravity() {
    return this.getAttributeValue(EntityAttributes.GENERIC_GRAVITY);
}
```

**SM 관련성**: SM 원본 중력 조작(`gravityFactor`)은 이 속성 값을 수정하거나 `getFinalGravity()` Mixin에서 처리 가능.

---

## B-13 — `getSlipperiness()` Yarn 메서드명 (확인됨)

```java
// Block.java (AbstractBlock을 상속)
public float getSlipperiness() {
    return this.slipperiness;
}
```

- 정의 위치: `Block.java` (`net.minecraft.block.Block`)
- Yarn 이름: **`getSlipperiness`**
- 반환: `this.slipperiness` (AbstractBlock.Settings에서 설정된 필드)
- 기본값: Ice = 0.98F, Packed Ice = 0.98F, Blue Ice = 0.989F, 일반 블록 = 0.6F

**호출 위치 (LivingEntity.travel() 지상/공중 분기)**:
```java
// LivingEntity.travel() — ④ 지상/공중
float p = this.getWorld().getBlockState(blockPos).getBlock().getSlipperiness();
float fxx = this.isOnGround() ? p * 0.91F : 0.91F;
```

`getVelocityAffectingPos()` 반환 위치의 블록 미끄러움을 읽음.

---

## B-14 — `isInsideWall()` intermediary명 (확인됨)

```java
// Entity.java
public boolean isInsideWall() {
    if (this.noClip) { return false; }
    float f = this.dimensions.width() * 0.8F;
    Box box = Box.of(this.getEyePos(), f, 1.0E-6, f);
    return BlockPos.stream(box).anyMatch(pos -> {
        BlockState blockState = this.getWorld().getBlockState(pos);
        return !blockState.isAir()
            && blockState.shouldSuffocate(this.getWorld(), pos)
            && VoxelShapes.matchesAnywhere(...);
    });
}

// LivingEntity 오버라이드:
public boolean isInsideWall() {
    return !this.isSleeping() && super.isInsideWall();
}
```

| Yarn 이름 | intermediary | 디스크립터 |
|-----------|-------------|-----------|
| `isInsideWall` | `method_5757` | `()Z` |

출처: `mappings.tiny` 직접 확인.

**SM 관련성**: 벽 안에 박히는 suffocation 판정. SM 히트박스가 달라지는 포즈(슬라이딩, 크롤링)에서 false positive 가능 → 포즈별 hitbox를 올바르게 설정하면 자동 해결.

---

## B-15 — 수영 소리 자동 재생 코드 경로 (확인됨)

### 정의 (Entity.java)

```java
// Entity.java
protected SoundEvent getSwimSound() {
    return SoundEvents.ENTITY_GENERIC_SWIM;
}
protected SoundEvent getSplashSound() {
    return SoundEvents.ENTITY_GENERIC_SPLASH;
}
protected SoundEvent getHighSpeedSplashSound() {
    return SoundEvents.ENTITY_GENERIC_SPLASH;
}
```

### 오버라이드 (PlayerEntity.java)

```java
// PlayerEntity.java
protected SoundEvent getSwimSound() {
    return SoundEvents.ENTITY_PLAYER_SWIM;
}
protected SoundEvent getSplashSound() {
    return SoundEvents.ENTITY_PLAYER_SPLASH;
}
protected SoundEvent getHighSpeedSplashSound() {
    return SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED;
}
```

### 호출 경로

```
Entity.move() → 이동 처리 완료 후
  isTouchingWater() && moveEffect.playsSounds()
    → playSwimSound()           ← 수중 이동 중 매 스텝
         getSwimSound()로 소리 재생
         볼륨 = min(1.0, sqrt(vx²*0.2 + vy² + vz²*0.2) * 0.35F)

Entity.checkWaterState()
  updateMovementInFluid(FluidTags.WATER, 0.014) returns true
    && !touchingWater && !firstUpdate
      → onSwimmingStart()        ← 처음 물에 들어갈 때
           속도 크기 < 0.25F → getSplashSound()
           속도 크기 ≥ 0.25F → getHighSpeedSplashSound()
```

`playSwimSound()` 상세:
```java
// Entity.java
protected void playSwimSound() {
    Entity entity = Objects.requireNonNullElse(this.getControllingPassenger(), this);
    float f = entity == this ? 0.35F : 0.4F;
    Vec3d vec3d = entity.getVelocity();
    float g = Math.min(1.0F, (float)Math.sqrt(
        vec3d.x * vec3d.x * 0.2F + vec3d.y * vec3d.y + vec3d.z * vec3d.z * 0.2F
    ) * f);
    this.playSwimSound(g);  // → playSound(getSwimSound(), g, ...)
}
```

**SM 관련성**: SM 크롤링(SWIMMING 포즈)에서 수영 소리가 재생될 수 있음. 크롤링 중 소리를 억제하려면 `getSwimSound()` Mixin 오버라이드 또는 `playSwimSound()` Mixin 주입 필요.
