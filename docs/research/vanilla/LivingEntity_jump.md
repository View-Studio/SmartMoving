# LivingEntity.jump() vanilla 1.21.1 리서치

소스: IntelliJ decompile 캐시 `AppData/Local/Temp/net/minecraft/entity/`  
Yarn 매핑: `~/.gradle/caches/fabric-loom/1.21.1/net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/mappings.tiny`

---

## 1. LivingEntity.jump()

- Yarn: `jump` (intermediary: `method_6043`, obf: `ff`, desc: `()V`)
- 클래스: `net.minecraft.entity.LivingEntity`
- 소스 위치: `LivingEntity.java:2171`
- 실행 위치: **클라이언트 + 서버** (tickMovement → jump 분기에서 호출)

```java
@VisibleForTesting
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

**동작:**
- Y 속도를 `getJumpVelocity()`로 교체 (X/Z는 유지)
- 스프린트 중이면 yaw 방향으로 수평 가속 추가: `Vec3d(-sin(yaw)*0.2, 0, cos(yaw)*0.2)`
- `f <= 1.0E-5F`이면 점프 무시 (jump strength가 거의 0인 경우)
- `velocityDirty = true` → 서버에 속도 변경 알림

**관련 필드 Yarn:**
| 필드 | obf | intermediary | Yarn | 클래스 |
|------|-----|-------------|------|--------|
| `jumping` | `bn` | `field_6282` | `jumping` | LivingEntity |
| `jumpingCooldown` | `ch` | `field_6228` | `jumpingCooldown` | LivingEntity |
| `velocityDirty` | `av` | `field_6007` | `velocityDirty` | Entity |

---

## 2. getJumpVelocity() — 점프 속도 계산

### LivingEntity.getJumpVelocity() (파라미터 없음)

- Yarn: `getJumpVelocity` (intermediary: `method_6106`, obf: `fd`, desc: `()F`)
- 소스 위치: `LivingEntity.java:2157`

```java
protected float getJumpVelocity() {
    return this.getJumpVelocity(1.0F);
}
```

### LivingEntity.getJumpVelocity(float strength)

- Yarn: `getJumpVelocity` (intermediary: `method_56994`, obf: `y`, desc: `(F)F`)
- 소스 위치: `LivingEntity.java:2161`

```java
protected float getJumpVelocity(float strength) {
    return (float)this.getAttributeValue(EntityAttributes.GENERIC_JUMP_STRENGTH) * strength
        * this.getJumpVelocityMultiplier()
        + this.getJumpBoostVelocityModifier();
}
```

**수식:**
```
jumpVelocity = GENERIC_JUMP_STRENGTH * strength * blockMultiplier + jumpBoostModifier
```

- `GENERIC_JUMP_STRENGTH` (field_23728): 기본값 0.42 (플레이어 기본 점프 속도)
- `strength`: 기본 1.0F (꽉 찬 점프), 차지 점프면 0~1
- `getJumpVelocityMultiplier()`: 발 아래 블록에 따른 배율

### getJumpVelocityMultiplier() (Entity)

- Yarn: `getJumpVelocityMultiplier` (intermediary: `method_23313`, obf: `aN`, desc: `()F`)
- 소스 위치: `Entity.java:1149`

```java
protected float getJumpVelocityMultiplier() {
    float f = this.getWorld().getBlockState(this.getBlockPos()).getBlock().getJumpVelocityMultiplier();
    float g = this.getWorld().getBlockState(this.getVelocityAffectingPos()).getBlock().getJumpVelocityMultiplier();
    return f == 1.0 ? g : f;
}
```

- `getBlockPos()`: 엔티티 발 위치 블록
- `getVelocityAffectingPos()`: 속도 영향 위치 블록 (슬라임 블록 등)
- 두 위치 중 배율이 1.0이 아닌 쪽 우선 (발 위치 > 속도 영향 위치)
- 대부분 블록: 1.0 → 변화 없음

### getJumpBoostVelocityModifier() (LivingEntity)

- Yarn: `getJumpBoostVelocityModifier` (intermediary: `method_37416`, obf: `fe`, desc: `()F`)
- 소스 위치: `LivingEntity.java:2166`

```java
public float getJumpBoostVelocityModifier() {
    return this.hasStatusEffect(StatusEffects.JUMP_BOOST)
        ? 0.1F * (this.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1.0F)
        : 0.0F;
}
```

- `JUMP_BOOST` (field_5913)
- 점프 강화 I: +0.1F, II: +0.2F, ...

---

## 3. jump() 호출 조건 — tickMovement() 내부

- 소스 위치: `LivingEntity.java:2779`
- Yarn: `tickMovement` (intermediary: `method_6004`, 이전 문서 확인됨)

```java
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
```

**조건 분기 요약:**

| 상황 | 동작 |
|------|------|
| `jumping=false` 또는 `!shouldSwimInFluids()` | `jumpingCooldown = 0` (리셋만) |
| 수중(`bl=true`) & 수면 위 | `swimUpward(WATER)` (+0.04F/틱) |
| 수중이지만 `g <= swimHeight` | 지상 점프 판정으로 처리 |
| 용암 중(`isInLava()`) & 수면 위 | `swimUpward(LAVA)` (+0.04F/틱) |
| **지상 점프**: `isOnGround() && jumpingCooldown==0` | **`jump()` 호출 + cooldown=10** |

**점프 가능 조건 (단순화):**
```
jumping == true
&& shouldSwimInFluids() == true
&& (isOnGround() || 수중 낮은 위치)
&& jumpingCooldown == 0
```

**jumpingCooldown:**
- `jump()` 호출 직후 10으로 설정
- tickMovement 시작 시 매 틱 1씩 감소
- 쿨다운 중엔 jump() 재호출 불가 → 연속 점프 방지

---

## 4. 관련 메서드 Yarn 이름

| Yarn | intermediary | obf | desc | 클래스 |
|------|-------------|-----|------|--------|
| `jump` | `method_6043` | `ff` | `()V` | LivingEntity |
| `getJumpVelocity` | `method_6106` | `fd` | `()F` | LivingEntity |
| `getJumpVelocity(float)` | `method_56994` | `y` | `(F)F` | LivingEntity |
| `getJumpBoostVelocityModifier` | `method_37416` | `fe` | `()F` | LivingEntity |
| `getJumpVelocityMultiplier` | `method_23313` | `aN` | `()F` | Entity |
| `knockDownwards` | `method_6093` | `fg` | `()V` | LivingEntity |
| `swimUpward` | `method_6010` | `c` | `(Lawu;)V` | LivingEntity |
| `shouldSwimInFluids` | `method_29920` | `ec` | `()Z` | LivingEntity |
| `getSwimHeight` | `method_29241` | `di` | `()D` | Entity |
| `getFluidHeight` | `method_5861` | `b` | `(Lawu;)D` | Entity |
| `setJumping` | `method_6100` | `t` | `(Z)V` | LivingEntity |
| `isInLava` | `method_5771` | `bt` | `()Z` | Entity |
| `isTouchingWater` | `method_5799` | `bf` | `()Z` | Entity |
| `GENERIC_JUMP_STRENGTH` (필드) | `field_23728` | `o` | `Ljm;` | EntityAttributes |
| `JUMP_BOOST` (필드) | `field_5913` | `h` | `Ljm;` | StatusEffects |

---

## 5. PlayerEntity.jump() 오버라이드

- 소스 위치: `PlayerEntity.java:1513`

```java
@Override
public void jump() {
    super.jump();
    this.incrementStat(Stats.JUMP);
    if (this.isSprinting()) {
        this.addExhaustion(0.2F);
    } else {
        this.addExhaustion(0.05F);
    }
}
```

- `super.jump()` → LivingEntity.jump() 전체 실행
- 통계 기록: `Stats.JUMP`
- 피로도 추가: 스프린트 점프 +0.2F, 일반 점프 +0.05F

**addExhaustion:**
```java
public void addExhaustion(float exhaustion) {
    if (!this.abilities.invulnerable) {
        if (!this.getWorld().isClient) {
            this.hungerManager.addExhaustion(exhaustion);
        }
    }
}
```
- 서버에서만 실행 (`!isClient`)
- invulnerable(크리에이티브) 시 무시

---

## 6. knockDownwards()

- Yarn: `knockDownwards` (intermediary: `method_6093`, obf: `fg`, desc: `()V`)
- 소스 위치: `LivingEntity.java:2185`

```java
protected void knockDownwards() {
    this.setVelocity(this.getVelocity().add(0.0, -0.04F, 0.0));
}
```

- Y 속도에 -0.04F 추가 (천장 충돌 등)
- jump()와 반대 방향
- swimUpward와 동일 크기(-0.04F)

---

## 7. getSwimHeight()

- Yarn: `getSwimHeight` (intermediary: `method_29241`, obf: `di`, desc: `()D`)
- 소스 위치: `Entity.java:4934`

```java
public double getSwimHeight() {
    return this.getStandingEyeHeight() < 0.4 ? 0.0 : 0.4;
}
```

- 눈 높이 < 0.4 (아주 작은 엔티티) → swimHeight = 0.0
- 그 외 → swimHeight = 0.4
- 플레이어: 눈 높이 ≥ 0.4 → swimHeight = 0.4

---

## 8. SM 포팅 충돌 분석

### 충돌 1: jump()의 Y 속도 교체 방식

vanilla `jump()`는 `setVelocity(x, f, z)` — **Y 속도를 getJumpVelocity()로 완전 교체**한다.  
SM 원본에서 다양한 점프 타입(headJump, wallJump, sprintJump 등)은 motionY를 직접 조작했다.  
1.21.1에서 SM 점프 처리가 `jump()` Mixin으로 구현되어야 한다면, `super.jump()` 호출 전후에 velocity를 추가 조작해야 한다.

### 충돌 2: 스프린트 점프 수평 가속

vanilla: 스프린트 점프 시 `(-sin(yaw)*0.2, 0, cos(yaw)*0.2)` 수평 가속 자동 추가.  
SM: `isSprintJump` 플래그 + 별도 `speedFactor` 계산 보정.  
→ vanilla의 0.2F 가속과 SM의 별도 보정이 **중복 적용**될 수 있음.

### 충돌 3: jumpingCooldown = 10

vanilla는 jump() 직후 10틱 쿨다운. SM의 점프 타입 전환(예: handleJumping에서 tryJump 반복 시도)이 이 쿨다운에 의해 차단될 수 있음.

### 충돌 4: PlayerEntity.jump()의 addExhaustion

PlayerEntity.jump()는 super.jump() 후 항상 피로도를 추가한다.  
SM의 특수 점프(headJump, wallJump 등)가 PlayerEntity.jump()를 경유하면 피로도가 추가 누적된다.

### 수치 요약

| 수치 | 의미 |
|------|------|
| 기본 점프속도 | `GENERIC_JUMP_STRENGTH` 기본값 0.42 |
| 스프린트 점프 수평 가속 | `±sin/cos(yaw) * 0.2` |
| jumpingCooldown | 10틱 |
| swimUpward/knockDownwards | ±0.04F/틱 |
| 점프 피로도 (일반) | 0.05F |
| 점프 피로도 (스프린트) | 0.2F |
| swimHeight (플레이어) | 0.4 |
| jump() 무시 임계값 | f <= 1.0E-5F |
