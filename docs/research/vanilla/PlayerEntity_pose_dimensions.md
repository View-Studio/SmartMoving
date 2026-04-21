# PlayerEntity.getEntityPose() 해당 Yarn 메서드 — 포즈·치수 시스템

소스: IntelliJ decompile 캐시 `AppData/Local/Temp/net/minecraft/entity/`  
Yarn 매핑: `~/.gradle/caches/fabric-loom/1.21.1/net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/mappings.tiny`

---

## 0. Mojmap `getEntityPose()` → Yarn 대응 결과

`getEntityPose` 라는 이름은 mappings.tiny에 존재하지 않는다.  
1.21.1 Yarn에서 "현재 포즈를 가져온다"는 기능은 **`getPose()` (method_18376)** 가 담당한다.  
"포즈별 치수를 반환한다"는 기능은 **`getBaseDimensions(EntityPose)` (method_55694)** 가 담당하며,  
PlayerEntity가 이를 오버라이드해 포즈별 실제 히트박스 크기를 결정한다.

이 두 메서드를 포함한 전체 포즈·치수 시스템을 아래에 정리한다.

---

## 1. Entity.getPose() / setPose()

### getPose()
- Yarn: `getPose` (intermediary: `method_18376`, obf: `at`, desc: `()Lbua;`)
- 클래스: `net.minecraft.entity.Entity`
- 소스 위치: `Entity.java:572`

```java
public EntityPose getPose() {
    return this.dataTracker.get(POSE);
}
```

### setPose()
- Yarn: `setPose` (intermediary: `method_18380`, obf: `b`, desc: `(Lbua;)V`)
- 소스 위치: `Entity.java:568`

```java
public void setPose(EntityPose pose) {
    this.dataTracker.set(POSE, pose);
}
```

**POSE TrackedData:**
```java
// Entity.java:353
protected static final TrackedData<EntityPose> POSE =
    DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.ENTITY_POSE);
// 초기값 (Entity.java:396):
builder.add(POSE, EntityPose.STANDING);
```

- `POSE`는 `TrackedData` — 서버→클라이언트 자동 동기화
- `setPose()` → `dataTracker.set()` → `onTrackedDataSet()` 트리거 → `calculateDimensions()` 자동 호출
- 클라이언트에서도 POSE 변경 시 즉시 치수 재계산됨

### isInPose()
- Yarn: `isInPose` (intermediary: `method_41328`, obf: `c`, desc: `(Lbua;)Z`)
- 소스 위치: `Entity.java:576`

```java
public boolean isInPose(EntityPose pose) {
    return this.getPose() == pose;
}
```

---

## 2. EntityPose 열거형 전체값

PlayerEntity.POSE_DIMENSIONS에 등록된 값 기준 (모두 실제 코드에서 확인):

| EntityPose | 너비 | 높이 | 눈 높이 | 용도 |
|-----------|------|------|---------|------|
| `STANDING` | 0.6F | 1.8F | 1.62F | 기본 서기 |
| `CROUCHING` | 0.6F | 1.5F | 1.27F | 웅크리기 |
| `SWIMMING` | 0.6F | 0.6F | 0.4F | 수영 / 크롤링 |
| `FALL_FLYING` | 0.6F | 0.6F | 0.4F | 엘리트라 |
| `SPIN_ATTACK` | 0.6F | 0.6F | 0.4F | 삼지창 회전 공격 |
| `SLEEPING` | 0.2F | 0.2F | 0.2F | 침대 수면 (`fixed` — 크기 변경 불가) |
| `DYING` | 0.2F | 0.2F | 1.62F | 사망 |

`STANDING_DIMENSIONS` 정의 (PlayerEntity.java:133):
```java
public static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.changing(0.6F, 1.8F)
    .withEyeHeight(1.62F)
    .withAttachments(EntityAttachments.builder().add(EntityAttachmentType.VEHICLE, VEHICLE_ATTACHMENT_POS));
```

`SLEEPING_DIMENSIONS` (LivingEntity.java:177):
```java
protected static final EntityDimensions SLEEPING_DIMENSIONS =
    EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(0.2F);
```

- `changing(w, h)`: 크기가 달라질 수 있는(변환 가능한) 치수
- `fixed(w, h)`: 크기 고정 치수

---

## 3. LivingEntity.getDimensions(EntityPose) — final

- Yarn: `getDimensions` (intermediary: `method_18377`, obf: `a`, desc: `(Lbua;)Lbsu;`)
- 클래스: `net.minecraft.entity.LivingEntity`
- 소스 위치: `LivingEntity.java:3395`
- **`final`** — 하위 클래스에서 오버라이드 불가

```java
@Override
public final EntityDimensions getDimensions(EntityPose pose) {
    return pose == EntityPose.SLEEPING
        ? SLEEPING_DIMENSIONS
        : this.getBaseDimensions(pose).scaled(this.getScale());
}
```

**동작:**
- `SLEEPING` 포즈 → 항상 SLEEPING_DIMENSIONS (고정, scale 무시)
- 그 외 → `getBaseDimensions(pose).scaled(getScale())`
- `getScale()`: `GENERIC_SCALE` attribute 기반 (기본 1.0F)

---

## 4. LivingEntity.getBaseDimensions(EntityPose) — 기본 구현

- Yarn: `getBaseDimensions` (intermediary: `method_55694`, obf: `e`, desc: `(Lbua;)Lbsu;`)
- 소스 위치: `LivingEntity.java:3399`

```java
protected EntityDimensions getBaseDimensions(EntityPose pose) {
    return this.getType().getDimensions().scaled(this.getScaleFactor());
}
```

- `getScaleFactor()` = 0.5F (baby), 1.0F (adult)
- 포즈를 무시하고 EntityType 기본 치수만 반환 — 대부분의 몹은 포즈별 치수 구분 없음

---

## 5. PlayerEntity.getBaseDimensions(EntityPose) — 핵심 오버라이드

- 소스 위치: `PlayerEntity.java:2061`

```java
@Override
public EntityDimensions getBaseDimensions(EntityPose pose) {
    return (EntityDimensions)POSE_DIMENSIONS.getOrDefault(pose, STANDING_DIMENSIONS);
}
```

**POSE_DIMENSIONS 맵 (PlayerEntity.java:135):**
```java
private static final Map<EntityPose, EntityDimensions> POSE_DIMENSIONS =
    ImmutableMap.<EntityPose, EntityDimensions>builder()
        .put(EntityPose.STANDING, STANDING_DIMENSIONS)                       // 0.6×1.8, eye 1.62
        .put(EntityPose.SLEEPING, SLEEPING_DIMENSIONS)                       // 0.2×0.2, eye 0.2
        .put(EntityPose.FALL_FLYING, EntityDimensions.changing(0.6F, 0.6F).withEyeHeight(0.4F))
        .put(EntityPose.SWIMMING,   EntityDimensions.changing(0.6F, 0.6F).withEyeHeight(0.4F))
        .put(EntityPose.SPIN_ATTACK,EntityDimensions.changing(0.6F, 0.6F).withEyeHeight(0.4F))
        .put(EntityPose.CROUCHING,
            EntityDimensions.changing(0.6F, 1.5F)
                .withEyeHeight(1.27F)
                .withAttachments(EntityAttachments.builder().add(EntityAttachmentType.VEHICLE, VEHICLE_ATTACHMENT_POS)))
        .put(EntityPose.DYING, EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(1.62F))
        .build();
```

- `getOrDefault(..., STANDING_DIMENSIONS)`: 맵에 없는 포즈는 STANDING 치수 반환
- 이 메서드가 플레이어의 **모든 포즈별 히트박스·눈높이를 결정하는 단일 지점**

---

## 6. PlayerEntity.getPoses()

- Yarn: `getPoses` (intermediary: `method_24831`, obf: `fE`, desc: `()Lcom/google/common/collect/ImmutableList;`)
- 소스 위치: `PlayerEntity.java:2067`

```java
@Override
public ImmutableList<EntityPose> getPoses() {
    return ImmutableList.of(EntityPose.STANDING, EntityPose.CROUCHING, EntityPose.SWIMMING);
}
```

- vanilla 플레이어가 진입 가능한 포즈: STANDING, CROUCHING, SWIMMING 3가지
- LivingEntity 기본 구현: `ImmutableList.of(EntityPose.STANDING)` 만 반환

---

## 7. Entity.calculateDimensions() — 포즈 변경 시 자동 호출

- Yarn: `calculateDimensions` (intermediary: `method_18382`, obf: `i_`, desc: `()V`)
- 소스 위치: `Entity.java:4229`
- 트리거: `onTrackedDataSet()` 에서 `POSE` TrackedData 변경 감지 시 호출

```java
public void calculateDimensions() {
    EntityDimensions entityDimensions = this.dimensions;       // 이전 치수
    EntityPose entityPose = this.getPose();
    EntityDimensions entityDimensions2 = this.getDimensions(entityPose);   // 새 치수
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
        && !(this instanceof PlayerEntity)) {    // ← PlayerEntity 제외
        this.recalculateDimensions(entityDimensions);
    }
}
```

**핵심 조건: `!(this instanceof PlayerEntity)`**
- PlayerEntity는 치수가 커질 때 위치 재조정(`recalculateDimensions`) 를 하지 않는다.
- 이유: 플레이어는 updatePose()에서 `canChangeIntoPose()` 공간 체크 후 포즈를 결정하므로,
  이미 충분한 공간이 보장된 상태에서만 치수가 커진다.

### recalculateDimensions()
- Yarn: `recalculateDimensions` (intermediary: `method_60490`, obf: `a`, desc: `(Lbsu;)Z`)
- 소스 위치: `Entity.java:4247`
- PlayerEntity에서는 **절대 호출되지 않음**

---

## 8. 관련 Yarn 이름 전체 목록

| Yarn 이름 | intermediary | obf | desc | 클래스 |
|-----------|-------------|-----|------|--------|
| `getPose` | `method_18376` | `at` | `()Lbua;` | Entity |
| `setPose` | `method_18380` | `b` | `(Lbua;)V` | Entity |
| `isInPose` | `method_41328` | `c` | `(Lbua;)Z` | Entity |
| `getDimensions` | `method_18377` | `a` | `(Lbua;)Lbsu;` | LivingEntity (final) |
| `getBaseDimensions` | `method_55694` | `e` | `(Lbua;)Lbsu;` | LivingEntity → PlayerEntity |
| `getPoses` | `method_24831` | `fE` | `()Lcom/google/common/collect/ImmutableList;` | LivingEntity → PlayerEntity |
| `calculateDimensions` | `method_18382` | `i_` | `()V` | Entity |
| `recalculateDimensions` | `method_60490` | `a` | `(Lbsu;)Z` | Entity |
| `getEyeHeight` | `method_18381` | `d` | `(Lbua;)F` | Entity (final) |
| `getStandingEyeHeight` | `method_5751` | `cL` | `()F` | Entity (final) |
| `getScale` | `method_55693` | `eb` | `()F` | LivingEntity |
| `getScaleFactor` | `method_17825` | `ea` | `()F` | LivingEntity |
| `canChangeIntoPose` | `method_52558` | `h` | `(Lbua;)Z` | PlayerEntity |
| `POSE` (TrackedData 필드) | — | — | `TrackedData<EntityPose>` | Entity |
| `POSE_DIMENSIONS` (필드) | `field_18134` | `d` | `Map<EntityPose,EntityDimensions>` | PlayerEntity |
| `STANDING_DIMENSIONS` (필드) | `field_18135` | `bW` | `EntityDimensions` | PlayerEntity |
| `SLEEPING_DIMENSIONS` (필드) | `field_18072` | `aF` | `EntityDimensions` | LivingEntity |
| `standingEyeHeight` (필드) | `field_18066` | `be` | `F` | Entity |

---

## 9. 포즈 변경 전체 흐름

```
[서버/클라이언트 양쪽]
PlayerEntity.tick()
  └─ updatePose()   (method_7318)
       ├─ canChangeIntoPose(SWIMMING) 사전 체크 (공간 있어야 진입 허용)
       ├─ 포즈 우선순위 결정: FALL_FLYING > SLEEPING > SWIMMING > SPIN_ATTACK > CROUCHING > STANDING
       ├─ canChangeIntoPose(목표 포즈) 공간 체크
       │    실패 시: CROUCHING → 실패 시 SWIMMING 강제
       └─ setPose(entityPose2)
              └─ dataTracker.set(POSE, ...)
                   └─ onTrackedDataSet() (POSE 변경 감지)
                        └─ calculateDimensions()
                             ├─ getDimensions(getPose())
                             │    └─ getBaseDimensions(pose)  ← PlayerEntity 오버라이드
                             │         └─ POSE_DIMENSIONS.getOrDefault(pose, STANDING)
                             ├─ dimensions = new dimensions
                             ├─ standingEyeHeight = new eyeHeight
                             ├─ refreshPosition()
                             └─ (PlayerEntity는 recalculateDimensions 건너뜀)
```

---

## 10. SM 포팅 충돌 분석

### 충돌 1: SM 크롤링 히트박스

SM 크롤링은 SWIMMING 포즈(0.6×0.6)를 사용.  
`getBaseDimensions(SWIMMING)` = `changing(0.6F, 0.6F).withEyeHeight(0.4F)`  
→ 히트박스 높이 0.6F, 눈 높이 0.4F.  
SM 원본에서 크롤링 시 사용한 히트박스 크기와 일치하는지 확인 필요.

### 충돌 2: getBaseDimensions Mixin 포인트

SM이 새 포즈(SM 전용 포즈 없으면 기존 SWIMMING 재사용)에 맞는 히트박스를 제공하려면  
`getBaseDimensions(EntityPose)` (method_55694)을 Mixin으로 오버라이드해야 한다.  
LivingEntity의 `getDimensions(EntityPose)` 는 **final** 이므로 직접 오버라이드 불가.

### 충돌 3: calculateDimensions의 PlayerEntity 예외

PlayerEntity는 치수가 커져도 `recalculateDimensions`가 호출되지 않는다.  
SM이 SWIMMING → STANDING 전환 시 위치 보정 없이 치수만 커지므로,  
블록에 끼인 경우 처리를 SM 자체적으로 해야 할 수 있다.  
(vanilla는 `canChangeIntoPose()` 공간 체크로 이를 방지하고 있음)

### 충돌 4: getPoses() 반환값

vanilla는 STANDING, CROUCHING, SWIMMING 3가지만 반환.  
SM 전용 포즈가 필요하다면 `getPoses()` (method_24831)도 오버라이드 필요.  
단 실제로 SM이 getPoses() 반환값을 직접 사용하는 코드 경로가 있는지 확인 필요.  
→ [미확인 — getPoses() 호출 위치 파악 안 됨]
