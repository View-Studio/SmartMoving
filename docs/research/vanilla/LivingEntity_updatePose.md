# LivingEntity.updatePose() / PlayerEntity.updatePose() — vanilla 1.21.1 리서치

Yarn 버전: `net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2`  
소스: IntelliJ 디컴파일 캐시  
- `AppData/Local/Temp/net/minecraft/entity/LivingEntity.java`  
- `AppData/Local/Temp/net/minecraft/entity/player/PlayerEntity.java`  
- `AppData/Local/Temp/net/minecraft/entity/Entity.java`  
실행 위치: 클라이언트 + 서버

---

## 비고: `trySetPose`는 존재하지 않음

mappings.tiny 및 디컴파일 소스 전체 검색 결과 `trySetPose`라는 메서드는 1.21.1 vanilla에 존재하지 않는다.  
PROGRESS.md의 `trySetPose` 표기는 과거 버전 이름 또는 잘못된 참조로 판단된다.  
실제로 구현되어 있는 것은 `updatePose()`(Yarn: `method_7318`)이며, `PlayerEntity`에 오버라이드가 있다.

---

## Yarn 이름 확인 (mappings.tiny 직접 확인)

| Yarn 이름 | intermediary | 디스크립터 | 클래스 |
|-----------|-------------|-----------|-------|
| `updatePose` | `method_7318` | `()V` | `LivingEntity` (PlayerEntity에서 오버라이드) |
| `canChangeIntoPose` | `method_52558` | `(Lbua;)Z` | `PlayerEntity` |
| `wouldNotSuffocateInPose` | `method_52542` | `(Lbua;)Z` | `LivingEntity` |
| `getPose` | `method_18376` | `()Lbua;` | `Entity` |
| `setPose` | `method_18380` | `(Lbua;)V` | `Entity` |
| `isInPose` | `method_41328` | `(Lbua;)Z` | `Entity` |
| `getPoses` | `method_24831` | `()Lcom/google/common/collect/ImmutableList;` | `LivingEntity`/`PlayerEntity` |
| `calculateDimensions` | `method_18382` | `()V` | `Entity` |
| `recalculateDimensions` | `method_60490` | `(Lbsu;)Z` | `Entity` |
| `isInSwimmingPose` | `method_20232` | `()Z` | `Entity` (LivingEntity에서 오버라이드) |
| `isSpaceEmpty` | `method_8587` | `(Lbsr;Lewx;)Z` | World |
| `isBlockSpaceEmpty` | `method_52569` | `(Lbsr;Lewx;)Z` | World |

`Lbua;` = `EntityPose`

---

## 호출 위치

`updatePose()`는 **`PlayerEntity.tick()`** 내부에서 호출된다.

```java
// PlayerEntity.tick() (line 315)
this.updatePose();
```

호출 순서 (PlayerEntity.tick()):
```
PlayerEntity.tick()
  this.updateWaterSubmersionState()
  super.tick()  → LivingEntity.tick() → tickMovement() → travel() 등
  ...
  this.updatePose()   ← super.tick() 이후
  ...
```

---

## `PlayerEntity.updatePose()` 전체 코드 (Yarn: `method_7318`, override)

```java
protected void updatePose() {
    if (this.canChangeIntoPose(EntityPose.SWIMMING)) {
        EntityPose entityPose;
        if (this.isFallFlying()) {
            entityPose = EntityPose.FALL_FLYING;
        } else if (this.isSleeping()) {
            entityPose = EntityPose.SLEEPING;
        } else if (this.isSwimming()) {
            entityPose = EntityPose.SWIMMING;
        } else if (this.isUsingRiptide()) {
            entityPose = EntityPose.SPIN_ATTACK;
        } else if (this.isSneaking() && !this.abilities.flying) {
            entityPose = EntityPose.CROUCHING;
        } else {
            entityPose = EntityPose.STANDING;
        }

        EntityPose entityPose2;
        if (this.isSpectator() || this.hasVehicle() || this.canChangeIntoPose(entityPose)) {
            entityPose2 = entityPose;
        } else if (this.canChangeIntoPose(EntityPose.CROUCHING)) {
            entityPose2 = EntityPose.CROUCHING;
        } else {
            entityPose2 = EntityPose.SWIMMING;
        }

        this.setPose(entityPose2);
    }
}
```

### 로직 흐름 요약

```
updatePose()
  canChangeIntoPose(SWIMMING) == false → 아무것도 안 함 (return 없이 if 블록 안 들어감)
  canChangeIntoPose(SWIMMING) == true:
    1단계: 원하는 포즈(entityPose) 결정
      isFallFlying()              → FALL_FLYING
      isSleeping()                → SLEEPING
      isSwimming()                → SWIMMING
      isUsingRiptide()            → SPIN_ATTACK
      isSneaking() && !flying     → CROUCHING
      else                        → STANDING

    2단계: 실제 적용할 포즈(entityPose2) 결정
      isSpectator() || hasVehicle() || canChangeIntoPose(entityPose)
                                  → entityPose2 = entityPose
      canChangeIntoPose(CROUCHING)→ entityPose2 = CROUCHING
      else                        → entityPose2 = SWIMMING  (크롤링 포즈)

    setPose(entityPose2)
```

### 핵심 패턴: 공간 부족 시 강제 SWIMMING (크롤링)

`canChangeIntoPose(원하는포즈)`가 false인 경우:
- `canChangeIntoPose(CROUCHING)` 시도 → 성공하면 CROUCHING
- 둘 다 실패 → `SWIMMING` (1.21.1의 "크롤링"은 SWIMMING 포즈를 물 밖에서 쓰는 것)

이는 1블록 천장 터널에 들어갔을 때 자동 크롤링이 발동하는 메커니즘이다.

---

## `PlayerEntity.canChangeIntoPose(EntityPose)` (Yarn: `method_52558`)

```java
protected boolean canChangeIntoPose(EntityPose pose) {
    return this.getWorld().isSpaceEmpty(this, this.getDimensions(pose).getBoxAt(this.getPos()).contract(1.0E-7));
}
```

- `getDimensions(pose)`: 해당 포즈의 `EntityDimensions` 취득
- `.getBoxAt(this.getPos())`: 현재 위치에서 해당 포즈의 AABB 계산
- `.contract(1.0E-7)`: AABB를 아주 조금 줄임 (경계 블록 오버랩 방지)
- `isSpaceEmpty(entity, box)` (Yarn: `method_8587`): 해당 공간이 비어있는지 확인

**`wouldNotSuffocateInPose()`와의 차이**:
- `canChangeIntoPose`: `isSpaceEmpty` + `contract(1.0E-7)` — 엔티티 고려, 박스 축소
- `wouldNotSuffocateInPose`: `isBlockSpaceEmpty` — 블록만 고려, 박스 축소 없음

---

## `LivingEntity.wouldNotSuffocateInPose(EntityPose)` (Yarn: `method_52542`)

```java
protected boolean wouldNotSuffocateInPose(EntityPose pose) {
    Box box = this.getDimensions(pose).getBoxAt(this.getPos());
    return this.getWorld().isBlockSpaceEmpty(this, box);
}
```

PlayerEntity의 `updatePose()`에서는 직접 호출되지 않음. `canChangeIntoPose()`가 대신 사용됨.

---

## `Entity.setPose(EntityPose)` (Yarn: `method_18380`)

```java
public void setPose(EntityPose pose) {
    this.dataTracker.set(POSE, pose);
}
```

DataTracker에 포즈 값 저장. DataTracker를 통해 클라이언트에 동기화됨.

---

## `Entity.getPose()` (Yarn: `method_18376`)

```java
public EntityPose getPose() {
    return this.dataTracker.get(POSE);
}
```

---

## `Entity.isInPose(EntityPose)` (Yarn: `method_41328`)

```java
public boolean isInPose(EntityPose pose) {
    return this.getPose() == pose;
}
```

---

## `Entity.isInSwimmingPose()` (Yarn: `method_20232`)

```java
// Entity.java
public boolean isInSwimmingPose() {
    return this.isInPose(EntityPose.SWIMMING);
}

// LivingEntity.java 오버라이드
@Override
public boolean isInSwimmingPose() {
    return super.isInSwimmingPose() || !this.isFallFlying() && this.isInPose(EntityPose.FALL_FLYING);
}
```

`LivingEntity`에서: `SWIMMING` 포즈 **또는** (`FALL_FLYING` 포즈이고 실제로 엘리트라 비행 중이 아닌 경우)도 수영 자세로 취급.  
즉, 크롤링 중(`SWIMMING` 포즈, 물 밖) 또는 전환 중인 경우도 포함.

---

## `Entity.calculateDimensions()` (Yarn: `method_18382`)

```java
public void calculateDimensions() {
    EntityDimensions entityDimensions = this.dimensions;   // 이전 크기
    EntityPose entityPose = this.getPose();
    EntityDimensions entityDimensions2 = this.getDimensions(entityPose);  // 새 크기
    this.dimensions = entityDimensions2;
    this.standingEyeHeight = entityDimensions2.eyeHeight();
    this.refreshPosition();
    boolean bl = entityDimensions2.width() <= 4.0 && entityDimensions2.height() <= 4.0;
    if (!this.world.isClient
        && !this.firstUpdate
        && !this.noClip
        && bl
        && (entityDimensions2.width() > entityDimensions.width()
            || entityDimensions2.height() > entityDimensions.height())
        && !(this instanceof PlayerEntity)) {
        this.recalculateDimensions(entityDimensions);
    }
}
```

포즈 변경 후 히트박스 크기 갱신. 서버에서 크기가 증가하면 `recalculateDimensions()` 호출 (충돌 재계산). **PlayerEntity는 제외**: `!(this instanceof PlayerEntity)` 조건.

**`calculateDimensions()` 호출 시점**: `LivingEntity.tick()` 내에서 `scale`이 변했을 때 호출됨 (앞 세션 리서치에서 확인).

---

## `PlayerEntity.POSE_DIMENSIONS` — 포즈별 히트박스 크기

```java
private static final Map<EntityPose, EntityDimensions> POSE_DIMENSIONS = ImmutableMap.<EntityPose, EntityDimensions>builder()
    .put(EntityPose.STANDING,    EntityDimensions.changing(0.6F, 1.8F).withEyeHeight(1.62F).withAttachments(...))
    .put(EntityPose.SLEEPING,    SLEEPING_DIMENSIONS)
    .put(EntityPose.FALL_FLYING, EntityDimensions.changing(0.6F, 0.6F).withEyeHeight(0.4F))
    .put(EntityPose.SWIMMING,    EntityDimensions.changing(0.6F, 0.6F).withEyeHeight(0.4F))
    .put(EntityPose.SPIN_ATTACK, EntityDimensions.changing(0.6F, 0.6F).withEyeHeight(0.4F))
    .put(EntityPose.CROUCHING,   EntityDimensions.changing(0.6F, 1.5F).withEyeHeight(1.27F).withAttachments(...))
    .put(EntityPose.DYING,       EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(1.62F))
    .build();
```

| 포즈 | 너비 | 높이 | 눈 높이 |
|------|------|------|---------|
| STANDING | 0.6F | 1.8F | 1.62F |
| CROUCHING | 0.6F | 1.5F | 1.27F |
| SWIMMING / FALL_FLYING / SPIN_ATTACK | 0.6F | 0.6F | 0.4F |
| DYING | 0.2F | 0.2F | 1.62F |

`PlayerEntity.getPoses()`:
```java
return ImmutableList.of(EntityPose.STANDING, EntityPose.CROUCHING, EntityPose.SWIMMING);
```

`LivingEntity.getPoses()` (기본):
```java
return ImmutableList.of(EntityPose.STANDING);
```

---

## `PlayerEntity.getBaseDimensions(EntityPose)` 오버라이드

```java
public EntityDimensions getBaseDimensions(EntityPose pose) {
    return (EntityDimensions)POSE_DIMENSIONS.getOrDefault(pose, STANDING_DIMENSIONS);
}
```

POSE_DIMENSIONS 맵에서 조회. 없으면 STANDING_DIMENSIONS 폴백.

`LivingEntity.getDimensions(EntityPose)` (final):
```java
public final EntityDimensions getDimensions(EntityPose pose) {
    return pose == EntityPose.SLEEPING ? SLEEPING_DIMENSIONS : this.getBaseDimensions(pose).scaled(this.getScale());
}
```

SLEEPING이면 고정 크기, 아니면 `getBaseDimensions(pose)` × `getScale()`.

---

## `updatePose()` 조건 분기 전체

```
isFallFlying()          → FALL_FLYING   (엘리트라 비행 중)
isSleeping()            → SLEEPING
isSwimming()            → SWIMMING      (물/용암 속에서 스프린트+전진, isCrawling()과 다름)
isUsingRiptide()        → SPIN_ATTACK
isSneaking() && !flying → CROUCHING
else                    → STANDING

공간 체크 후 최종 결정:
isSpectator || hasVehicle || canChangeIntoPose(원하는포즈) → 원하는포즈
canChangeIntoPose(CROUCHING)                              → CROUCHING
else                                                      → SWIMMING (강제 크롤링)
```

---

## 중요 수치

| 수치 | 위치 | 의미 |
|------|------|------|
| `1.0E-7` | `canChangeIntoPose()` | AABB contract 크기 |
| `0.6F, 1.8F` | STANDING 히트박스 | 너비×높이 |
| `1.62F` | STANDING 눈 높이 | |
| `0.6F, 1.5F` | CROUCHING 히트박스 | |
| `1.27F` | CROUCHING 눈 높이 | |
| `0.6F, 0.6F` | SWIMMING/FALL_FLYING/SPIN_ATTACK 히트박스 | |
| `0.4F` | SWIMMING/FALL_FLYING/SPIN_ATTACK 눈 높이 | |
| `0.2F, 0.2F` | DYING 히트박스 | |

---

## SmartMoving와의 충돌 가능성

| 항목 | vanilla 동작 | SM 간섭 예상 위치 | 충돌 여부 |
|------|------------|-----------------|---------|
| `isSneaking() && !flying → CROUCHING` | 스니킹 시 CROUCHING 포즈, 히트박스 1.5H | SM 크롤링은 `SWIMMING` 포즈(0.6H) 사용 필요 | SM에서 `updatePose()` Mixin으로 크롤링 포즈 강제 설정 필요 |
| `canChangeIntoPose(SWIMMING)` 선행 체크 | SWIMMING 공간 없으면 전체 updatePose() 스킵 | SM 크롤링 진입 조건과 겹칠 수 있음 | 확인 필요 |
| `isSwimming() → SWIMMING` | 물속 스프린트 | SM의 수영은 별도 상태 | SM `isSwimming()` 오버라이드 여부 확인 필요 |
| 강제 `SWIMMING` 크롤링 | 공간 부족 시 자동 크롤링 발동 | SM도 크롤링을 `SWIMMING` 포즈로 구현해야 `updateLeaningPitch()` 등 vanilla 연동 가능 | SM의 크롤링이 이 포즈를 사용하는지 확인 필요 |
| `calculateDimensions()` PlayerEntity 제외 | `!(this instanceof PlayerEntity)` | PlayerEntity 히트박스는 `updatePose()` → `setPose()` → DataTracker 변경 감지로 처리 | 충돌 없음 (PlayerEntity는 별도 경로) |
| `isInSwimmingPose()` LivingEntity 오버라이드 | `SWIMMING` 포즈 OR (`FALL_FLYING` 포즈 && !isFallFlying()) | `updateLeaningPitch()`에서 이 메서드 사용 — SM 크롤링 시 수영 자세 보간 영향 | SM 크롤링이 SWIMMING 포즈이면 `leaningPitch`도 1.0F로 올라감 |
