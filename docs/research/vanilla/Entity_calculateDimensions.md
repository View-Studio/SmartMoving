# Entity.calculateDimensions() vanilla 1.21.1 리서치

소스: IntelliJ decompile 캐시 `AppData/Local/Temp/net/minecraft/entity/Entity.java`, `LivingEntity.java`, `player/PlayerEntity.java`  
Yarn 매핑: `~/.gradle/caches/fabric-loom/1.21.1/net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/mappings.tiny`

---

## 1. 메서드 시그니처 (Yarn 이름)

| Yarn 이름 | intermediary | obf | 클래스 | 시그니처 |
|-----------|-------------|-----|--------|---------|
| `calculateDimensions` | `method_18382` | `i_` | Entity | `()V` |
| `recalculateDimensions` | `method_60490` | `a` | Entity | `(EntityDimensions)Z` |
| `getDimensions` | `method_18377` | `a` | LivingEntity | `(EntityPose)EntityDimensions` |
| `getBaseDimensions` | `method_55694` | `e` | LivingEntity / PlayerEntity | `(EntityPose)EntityDimensions` |
| `getPoses` | `method_24831` | `fE` | LivingEntity / PlayerEntity | `()ImmutableList<EntityPose>` |
| `refreshPosition` | `method_23311` | `av` | Entity | `()V` |
| `getScaleFactor` | `method_17825` | `ea` | LivingEntity | `()F` |
| `getScale` | `method_55693` | `eb` | LivingEntity | `()F` |

---

## 2. Entity.calculateDimensions() 전체 로직

- 소스: `Entity.java:4229`
- 실행 위치: **클라이언트 + 서버** (POSE TrackedData 변경 시 양쪽에서 트리거)

```java
public void calculateDimensions() {
    EntityDimensions oldDimensions = this.dimensions;     // 이전 크기
    EntityPose pose = this.getPose();
    EntityDimensions newDimensions = this.getDimensions(pose);  // 포즈에 맞는 새 크기

    this.dimensions = newDimensions;
    this.standingEyeHeight = newDimensions.eyeHeight();

    this.refreshPosition();
    // → setPosition(pos.x, pos.y, pos.z)
    // → setBoundingBox(calculateBoundingBox())
    // → 바운딩 박스가 새 dimensions 기준으로 재계산됨

    // ── 서버 전용: 크기가 커졌을 때 벽 충돌 보정 ──────────
    boolean sizeOk = newDimensions.width() <= 4.0 && newDimensions.height() <= 4.0;
    if (!this.world.isClient         // 서버
        && !this.firstUpdate          // 첫 틱이 아님
        && !this.noClip               // 노클립 아님
        && sizeOk                     // 크기 4×4 이하
        && (newDimensions.width() > oldDimensions.width()
            || newDimensions.height() > oldDimensions.height())
        && !(this instanceof PlayerEntity)) {  // ★ PlayerEntity 제외
        this.recalculateDimensions(oldDimensions);
    }
}
```

**핵심**: `!(this instanceof PlayerEntity)` 조건으로 **PlayerEntity는 recalculateDimensions가 절대 호출되지 않는다**.  
즉 플레이어가 CROUCHING → STANDING으로 복귀 시 벽 충돌 보정 없이 그냥 크기만 변경된다.

---

## 3. Entity.recalculateDimensions() 전체 로직

- 소스: `Entity.java:4247`
- 실행 위치: **서버 전용** (calculateDimensions에서 조건 충족 시, PlayerEntity 제외)

```java
public boolean recalculateDimensions(EntityDimensions previous) {
    EntityDimensions current = this.getDimensions(this.getPose());

    // 이전 바운딩 박스 중심-상단에 아주 작은 탐색 박스 생성
    Vec3d center = this.getPos().add(0.0, previous.height() / 2.0, 0.0);
    double widthDelta  = Math.max(0.0F, current.width()  - previous.width())  + 1.0E-6;
    double heightDelta = Math.max(0.0F, current.height() - previous.height()) + 1.0E-6;

    VoxelShape searchBox = VoxelShapes.cuboid(Box.of(center, widthDelta, heightDelta, widthDelta));

    // 1차 탐색: 새 크기(width×height)로 빈 공간 탐색
    Optional<Vec3d> pos = this.world.findClosestCollision(
        this, searchBox, center,
        current.width(), current.height(), current.width()
    );
    if (pos.isPresent()) {
        this.setPosition(pos.get().add(0.0, -current.height() / 2.0, 0.0));
        return true;
    }

    // 2차 탐색: width AND height 둘 다 커진 경우 — 수평만 탐색
    if (current.width() > previous.width() && current.height() > previous.height()) {
        VoxelShape searchBox2 = VoxelShapes.cuboid(Box.of(center, widthDelta, 1.0E-6, widthDelta));
        Optional<Vec3d> pos2 = this.world.findClosestCollision(
            this, searchBox2, center,
            current.width(), previous.height(), current.width()
        );
        if (pos2.isPresent()) {
            this.setPosition(pos2.get().add(0.0, -previous.height() / 2.0 + 1.0E-6, 0.0));
            return true;
        }
    }

    return false;  // 빈 공간 없음 — 위치 변경 없음
}
```

---

## 4. calculateDimensions() 트리거 시점

### 트리거 1: POSE TrackedData 변경 시 (양쪽)

```java
// Entity.java
@Override
public void onTrackedDataSet(TrackedData<?> data) {
    if (POSE.equals(data)) {
        this.calculateDimensions();
    }
}
```

POSE는 `TrackedData`로 관리됨 → 서버에서 `setPose()` 호출 시 클라이언트에도 자동 동기화,  
클라이언트가 TrackedData 수신 시 `onTrackedDataSet()` 호출 → `calculateDimensions()` 호출.

### 트리거 2: scale 변경 시 (LivingEntity.tickMovement)

```java
// LivingEntity.java (tickMovement 끝)
float scale = this.getScale();
if (scale != this.prevScale) {
    this.prevScale = scale;
    this.calculateDimensions();
}
```

매 틱 실행. scale attribute가 변하면 calculateDimensions 재호출.

---

## 5. LivingEntity.getDimensions() — final

- 소스: `LivingEntity.java:3395`
- **`final`** — 하위 클래스 오버라이드 불가. Mixin 포인트는 `getBaseDimensions()`

```java
public final EntityDimensions getDimensions(EntityPose pose) {
    return pose == EntityPose.SLEEPING
        ? SLEEPING_DIMENSIONS   // 0.2×0.2, eyeHeight=0.2
        : this.getBaseDimensions(pose).scaled(this.getScale());
}
```

- **SLEEPING 포즈**: 항상 고정값 `SLEEPING_DIMENSIONS` (0.2×0.2, eyeHeight=0.2)
- **그 외**: `getBaseDimensions(pose).scaled(getScale())`

---

## 6. LivingEntity.getBaseDimensions() — 기본값

- 소스: `LivingEntity.java:3399`

```java
protected EntityDimensions getBaseDimensions(EntityPose pose) {
    return this.getType().getDimensions().scaled(this.getScaleFactor());
}
```

- `getType().getDimensions()`: EntityType에 등록된 기본 크기
- `getScaleFactor()`: `isBaby() ? 0.5F : 1.0F`

---

## 7. PlayerEntity.getBaseDimensions() — 오버라이드

- 소스: `PlayerEntity.java:2061`

```java
@Override
public EntityDimensions getBaseDimensions(EntityPose pose) {
    return (EntityDimensions) POSE_DIMENSIONS.getOrDefault(pose, STANDING_DIMENSIONS);
}
```

**PlayerEntity는 getScaleFactor/EntityType 치수를 무시하고 POSE_DIMENSIONS 맵에서 직접 반환.**

---

## 8. POSE_DIMENSIONS 전체 맵

- 필드 Yarn: `POSE_DIMENSIONS` (intermediary: `field_18134`)
- 소스: `PlayerEntity.java:135`

| EntityPose | width | height | eyeHeight | 비고 |
|------------|-------|--------|-----------|------|
| `STANDING` | 0.6 | 1.8 | 1.62 | changing, VEHICLE attachment 포함 |
| `SLEEPING` | 0.2 | 0.2 | 0.2 | LivingEntity.SLEEPING_DIMENSIONS (field_18072) |
| `FALL_FLYING` | 0.6 | 0.6 | 0.4 | changing |
| `SWIMMING` | 0.6 | 0.6 | 0.4 | changing |
| `SPIN_ATTACK` | 0.6 | 0.6 | 0.4 | changing |
| `CROUCHING` | 0.6 | 1.5 | 1.27 | changing, VEHICLE attachment 포함 |
| `DYING` | 0.2 | 0.2 | 1.62 | fixed |

- `fixed` = 크기가 고정(scale 영향 없음)
- `changing` = scale 영향받음

**PlayerEntity.getDimensions() 최종 계산:**  
`getDimensions(pose) = getBaseDimensions(pose).scaled(getScale())`  
= `POSE_DIMENSIONS[pose].scaled(GENERIC_SCALE attribute value)`

scale 기본값 = 1.0이므로, 일반 플레이어에서는 POSE_DIMENSIONS 값 그대로.

---

## 9. STANDING_DIMENSIONS 필드

- Yarn: `STANDING_DIMENSIONS` (intermediary: `field_18135`)
- 소스: `PlayerEntity.java:132`

```java
public static final EntityDimensions STANDING_DIMENSIONS =
    EntityDimensions.changing(0.6F, 1.8F)
        .withEyeHeight(1.62F)
        .withAttachments(EntityAttachments.builder()
            .add(EntityAttachmentType.VEHICLE, VEHICLE_ATTACHMENT_POS));
```

---

## 10. PlayerEntity.getPoses()

- Yarn: `getPoses` (intermediary: `method_24831`)
- 소스: `PlayerEntity.java:2067`

```java
@Override
public ImmutableList<EntityPose> getPoses() {
    return ImmutableList.of(EntityPose.STANDING, EntityPose.CROUCHING, EntityPose.SWIMMING);
}
```

PlayerEntity가 시도하는 포즈: STANDING → CROUCHING → SWIMMING 순서.  
`updatePose()` 내에서 `canChangeIntoPose(pose)` 체크와 함께 사용됨.

---

## 11. LivingEntity.getScale() / getScaleFactor()

### getScale() (method_55693)
```java
public float getScale() {
    AttributeContainer attrs = this.getAttributes();
    return attrs == null ? 1.0F
           : this.clampScale((float) attrs.getValue(EntityAttributes.GENERIC_SCALE));
}

protected float clampScale(float scale) {
    return scale;  // LivingEntity 기본: 클램프 없음
}
```

- `GENERIC_SCALE` attribute 기본값: 1.0
- `getDimensions()` 내에서 `getBaseDimensions().scaled(getScale())` 형태로 사용

### getScaleFactor() (method_17825)
```java
public float getScaleFactor() {
    return this.isBaby() ? 0.5F : 1.0F;
}
```

- LivingEntity.getBaseDimensions()에서만 사용 (PlayerEntity는 getScaleFactor를 무시)

---

## 12. refreshPosition() (method_23311)

```java
protected void refreshPosition() {
    this.setPosition(this.pos.x, this.pos.y, this.pos.z);
}

public void setPosition(double x, double y, double z) {
    this.setPos(x, y, z);
    this.setBoundingBox(this.calculateBoundingBox());
}

protected Box calculateBoundingBox() {
    return this.dimensions.getBoxAt(this.pos);
    // 현재 this.dimensions (이미 calculateDimensions에서 교체됨)를 기준으로 박스 계산
}
```

`calculateDimensions()`에서 `this.dimensions`를 교체한 직후 `refreshPosition()` 호출 → 바운딩 박스가 새 포즈 크기로 즉시 교체됨.

---

## 13. EntityDimensions.scaled() (method_18383)

- Yarn: `scaled` (intermediary: `method_18383`)
- 시그니처: `scaled(float ratio) → EntityDimensions`

scale을 적용하여 새 EntityDimensions를 반환. `getScale() = 1.0`이면 값 그대로.

---

## 14. 전체 흐름 요약

```
포즈 변경 트리거 (LivingEntity.updatePose() → setPose())
  │
  ├─ TrackedData POSE 변경 → DataTracker가 서버→클라이언트 동기화
  │
  └─ onTrackedDataSet(POSE) 호출 (클라이언트 + 서버 각각)
       │
       └─ Entity.calculateDimensions()
            │
            ├─ EntityDimensions newDims = getDimensions(getPose())
            │    └─ LivingEntity.getDimensions(pose):  [final]
            │         ├─ SLEEPING → SLEEPING_DIMENSIONS (0.2×0.2, eye=0.2)
            │         └─ else → getBaseDimensions(pose).scaled(getScale())
            │                       └─ PlayerEntity.getBaseDimensions(pose):
            │                            POSE_DIMENSIONS.getOrDefault(pose, STANDING)
            │
            ├─ this.dimensions = newDims         [즉시 교체]
            ├─ this.standingEyeHeight = newDims.eyeHeight()
            ├─ refreshPosition()                  [바운딩 박스 재계산]
            │
            └─ if (서버 && 크기증가 && NOT PlayerEntity):
                  recalculateDimensions(oldDims)  [벽 충돌 보정 — 플레이어 제외]
```

---

## 15. SM 포팅 충돌 분석

### 충돌 1: 포즈 변경 시 바운딩 박스 즉시 변경

`calculateDimensions()`는 **동기적으로** 바운딩 박스를 교체한다.  
SM이 CROUCHING 또는 SWIMMING 포즈를 설정하는 순간:
- CROUCHING: 1.8 → 1.5 (height 감소)
- SWIMMING: 1.8 → 0.6 (height 대폭 감소)

이 변화는 즉시 서버 충돌 판정에 영향을 준다. SM이 크롤링(SWIMMING 포즈)을 사용하면 높이 0.6으로 판정된다.

### 충돌 2: PlayerEntity는 recalculateDimensions 없음

PlayerEntity는 `calculateDimensions()` 안에서 `!(this instanceof PlayerEntity)` 조건으로 `recalculateDimensions()` 를 건너뜀.  
즉 CROUCHING → STANDING 전환 시 천장이 있어도 서버가 위치를 보정하지 않는다.  
대신 `trySetPose()` 단계에서 이미 `wouldNotSuffocateInPose()` 체크로 전환 가능 여부를 판단한다 (이전 연구에서 확인).

### 충돌 3: 눈 높이(standingEyeHeight) 즉시 변경

`calculateDimensions()` 호출 즉시 `this.standingEyeHeight = newDimensions.eyeHeight()` 교체.

| 포즈 | 눈 높이 |
|------|--------|
| STANDING | 1.62 |
| CROUCHING | 1.27 |
| SWIMMING / FALL_FLYING | 0.4 |

SM 포즈 변경 시 카메라 높이가 자동 변경된다. SM이 크롤링에 SWIMMING 포즈를 쓰면 카메라가 0.4로 내려간다.

### 충돌 4: getScale()이 getDimensions()에 곱해짐

`getDimensions() = getBaseDimensions().scaled(getScale())`.  
SM에서 GENERIC_SCALE attribute를 변경하면 모든 포즈의 바운딩 박스가 scale 배로 변함.  
scale = 1.0인 일반 플레이어는 영향 없음.

### 충돌 5: getDimensions()가 final

`LivingEntity.getDimensions(EntityPose)`는 **final** — 직접 오버라이드 불가.  
SM이 특정 포즈에서 다른 바운딩 박스를 쓰려면 `getBaseDimensions()` Mixin으로 해야 한다.

---

## 16. 수치 전체 요약

| 포즈 | width | height | eyeHeight |
|------|-------|--------|-----------|
| STANDING | 0.6 | 1.8 | 1.62 |
| CROUCHING | 0.6 | 1.5 | 1.27 |
| SWIMMING | 0.6 | 0.6 | 0.4 |
| FALL_FLYING | 0.6 | 0.6 | 0.4 |
| SPIN_ATTACK | 0.6 | 0.6 | 0.4 |
| SLEEPING | 0.2 | 0.2 | 0.2 |
| DYING | 0.2 | 0.2 | 1.62 |
| `recalculateDimensions` 탐색 여유값 | `1.0E-6` | — | 수치 오차 보정 |
| 최대 크기 (recalculate 조건) | ≤ 4.0 | ≤ 4.0 | — |
| scale 기본값 | `1.0` | — | GENERIC_SCALE attribute |
| 아기 scaleFactor | `0.5F` | — | `isBaby() ? 0.5 : 1.0` |
