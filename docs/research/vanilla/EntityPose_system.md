# vanilla EntityPose 시스템 — R-07 리서치

소스: Fabric Loom 디컴파일 (Vineflower 1.11.1)  
Yarn: `net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2`  
확인 파일: `common-unpicked.jar` →  
- `net/minecraft/entity/EntityPose.class`  
- `net/minecraft/entity/player/PlayerEntity.class`  
- `net/minecraft/entity/LivingEntity.class`  
- `net/minecraft/entity/Entity.class`  
- `net/minecraft/entity/vehicle/BoatEntity.class`  
실행 위치: 서버 + 클라이언트

---

## B-01 — EntityPose 전체 값 목록 (확인됨)

```java
public enum EntityPose {
    STANDING(0),
    FALL_FLYING(1),
    SLEEPING(2),
    SWIMMING(3),
    SPIN_ATTACK(4),
    CROUCHING(5),
    LONG_JUMPING(6),
    DYING(7),
    CROAKING(8),
    USING_TONGUE(9),
    SITTING(10),
    ROARING(11),
    SNIFFING(12),
    EMERGING(13),
    DIGGING(14),
    SLIDING(15),      // ← SM isSliding에 재사용 가능
    SHOOTING(16),
    INHALING(17);
    ...
}
```

**총 18개** (index 0~17).

PlayerEntity.POSE_DIMENSIONS에 등록된 포즈 (히트박스 있음): STANDING, SLEEPING, FALL_FLYING, SWIMMING, SPIN_ATTACK, CROUCHING, DYING — 7개.  
나머지 11개 (LONG_JUMPING, CROAKING, USING_TONGUE, SITTING, ROARING, SNIFFING, EMERGING, DIGGING, **SLIDING**, SHOOTING, INHALING)는 POSE_DIMENSIONS에 없음 → `getBaseDimensions(pose)` → `getOrDefault(pose, STANDING_DIMENSIONS)` → **STANDING 치수 반환**.

### 기존 파일 (PlayerEntity_pose_dimensions.md)과의 차이

기존 리서치에서는 POSE_DIMENSIONS에 있는 7개만 기록됨. 실제 enum은 18개.  
**`SLIDING` 포즈(index 15)가 이미 존재** — SM의 `isSliding` 상태에 재사용 가능.

---

## B-01 — 커스텀 EntityPose 추가 가능 여부

```java
public enum EntityPose { ... }  // Java enum — 컴파일 타임 고정, 런타임 확장 불가
```

**결론: 불가능**.

Java의 `enum`은 `final class`로 컴파일됨. 런타임에 새 값을 추가하는 방법:
- ASM/ByteBuddy 바이트코드 조작: 기술적으로 가능하지만 불안정하고 복잡.
- Mixin으로 enum 값 추가: Mixin은 enum 값 추가를 지원하지 않음.

**SM 이식 대안**:
1. 이미 존재하는 `SLIDING` 포즈를 SM isSliding에 재사용 (POSE_DIMENSIONS에 없으므로 히트박스는 STANDING 기준 → `getBaseDimensions` Mixin에서 별도 치수 정의 가능)
2. 헤드점프(`isHeadJumping`)는 `STANDING` 또는 별도 포즈 없이 구현 (원본도 EntityPose 없음)
3. 크롤링(`isCrawling`) → vanilla의 `SWIMMING` 포즈 재사용 (0.6×0.6 hitbox)

---

## B-02 — PlayerEntity.updatePose() 전체 (확인됨)

→ `docs/research/vanilla/LivingEntity_updatePose.md` 및 `PlayerEntity_pose_dimensions.md`에 이미 완전히 기록됨.

**요약**:
- Yarn: `updatePose` (intermediary: `method_7318`)
- 호출: `PlayerEntity.tick()` 매 틱
- 우선순위: `FALL_FLYING > SLEEPING > SWIMMING > SPIN_ATTACK > CROUCHING > STANDING`
- 공간 부족 시 강제: `CROUCHING` → 실패 시 `SWIMMING`
- 조건식은 `PlayerEntity_pose_dimensions.md` 섹션 참조

---

## B-03 — getPoses() 호출 경로 (확인됨)

### 정의

```java
// LivingEntity (기본)
public ImmutableList<EntityPose> getPoses() {
    return ImmutableList.of(EntityPose.STANDING);
}

// PlayerEntity (오버라이드)
@Override
public ImmutableList<EntityPose> getPoses() {
    return ImmutableList.of(EntityPose.STANDING, EntityPose.CROUCHING, EntityPose.SWIMMING);
}
```

### 호출 위치: vehicle entity dismount 전용

**호출하는 클래스 (직접 확인)**: BoatEntity, AbstractMinecartEntity, AbstractHorseEntity, PigEntity, StriderEntity

이 클래스들의 `updatePassengerForDismount(Entity passenger)` 내에서 호출됨:

```java
// BoatEntity.updatePassengerForDismount() (확인됨)
UnmodifiableIterator var14 = passenger.getPoses().iterator();

while (var14.hasNext()) {
    EntityPose entityPose = (EntityPose)var14.next();

    for (Vec3d vec3d2 : list) {
        if (Dismounting.canPlaceEntityAt(this.getWorld(), vec3d2, passenger, entityPose)) {
            passenger.setPose(entityPose);  // 맞는 포즈로 설정
            return vec3d2;
        }
    }
}
```

**목적**: 탈것에서 내릴 때 플레이어가 맞는 공간에 착지 가능한 포즈를 탐색.  
반환된 포즈 순서대로(STANDING → CROUCHING → SWIMMING) 공간 체크 후 첫 번째 성공 포즈 설정.

**SmartMoving 관련성**:
- SM 이식에서 탈것 dismount 시 SM 전용 포즈(크롤링 등) 고려가 필요하면 `getPoses()` (method_24831) Mixin 오버라이드 필요.
- SM 원본 1.7.10에서는 탈것 dismount와 SM 포즈의 상호작용이 없으므로, 일단 vanilla 그대로 유지 가능.

---

## B-04 — updatePose() → calculateDimensions() 호출 여부 (확인됨)

**호출 경로**:
```
updatePose()
  └─ setPose(entityPose2)
       └─ dataTracker.set(POSE, pose)
            └─ onTrackedDataSet() — POSE TrackedData 변경 감지
                 └─ calculateDimensions()   ← 호출됨
                      ├─ getDimensions(getPose())
                      ├─ dimensions 갱신
                      ├─ standingEyeHeight 갱신
                      ├─ refreshPosition()
                      └─ (PlayerEntity는 recalculateDimensions 스킵)
```

**결론**: `updatePose()` → `setPose()` → DataTracker → `onTrackedDataSet()` → `calculateDimensions()` — 자동 호출 확인.

```java
// Entity.onTrackedDataSet()
public void onTrackedDataSet(TrackedData<?> data) {
    if (POSE.equals(data)) {
        this.calculateDimensions();
    }
}
```

→ 상세 내용은 `PlayerEntity_pose_dimensions.md` 섹션 7 참조.

---

## 포즈별 치수 전체 요약

| EntityPose | index | 너비 | 높이 | 눈높이 | POSE_DIMENSIONS 등록 |
|-----------|-------|------|------|--------|---------------------|
| STANDING | 0 | 0.6F | 1.8F | 1.62F | ✓ |
| FALL_FLYING | 1 | 0.6F | 0.6F | 0.4F | ✓ |
| SLEEPING | 2 | 0.2F | 0.2F | 0.2F | ✓ (fixed) |
| SWIMMING | 3 | 0.6F | 0.6F | 0.4F | ✓ |
| SPIN_ATTACK | 4 | 0.6F | 0.6F | 0.4F | ✓ |
| CROUCHING | 5 | 0.6F | 1.5F | 1.27F | ✓ |
| LONG_JUMPING | 6 | — | — | — | ✗ → STANDING 폴백 |
| DYING | 7 | 0.2F | 0.2F | 1.62F | ✓ (fixed) |
| CROAKING | 8 | — | — | — | ✗ → STANDING 폴백 |
| USING_TONGUE | 9 | — | — | — | ✗ → STANDING 폴백 |
| SITTING | 10 | — | — | — | ✗ → STANDING 폴백 |
| ROARING | 11 | — | — | — | ✗ → STANDING 폴백 |
| SNIFFING | 12 | — | — | — | ✗ → STANDING 폴백 |
| EMERGING | 13 | — | — | — | ✗ → STANDING 폴백 |
| DIGGING | 14 | — | — | — | ✗ → STANDING 폴백 |
| SLIDING | 15 | — | — | — | ✗ → STANDING 폴백 |
| SHOOTING | 16 | — | — | — | ✗ → STANDING 폴백 |
| INHALING | 17 | — | — | — | ✗ → STANDING 폴백 |

POSE_DIMENSIONS 미등록 포즈는 `getBaseDimensions(pose)` → `getOrDefault(pose, STANDING_DIMENSIONS)` → STANDING(0.6×1.8) 반환.

---

## SM 이식 설계 메모

| SM 상태 | EntityPose 전략 | 히트박스 처리 |
|---------|----------------|--------------|
| `isCrawling` | `SWIMMING` (vanilla 크롤링과 동일) | 0.6×0.6 자동 적용 |
| `isSliding` | `SLIDING` (index 15) 재사용 | `getBaseDimensions` Mixin에서 슬라이딩 전용 치수 정의 필요 |
| `isHeadJumping` | 별도 포즈 없이 STANDING 유지 | `recalculateDimensions` 없음 주의 |
| `isCeilingClimbing` | SWIMMING 또는 STANDING | SM 원본 hitbox 참조 |
| `isClimbing` | STANDING | 별도 hitbox 변경 없음 |

**SM Mixin 전략**:
1. `updatePose()` — `@Inject(at=HEAD, cancellable=true)` 또는 `@Inject(at=TAIL)`:  
   SM 상태(isCrawling, isSliding 등) 확인 후 해당 EntityPose 강제 설정.
2. `getBaseDimensions(EntityPose)` — `@Inject(at=HEAD, cancellable=true)`:  
   SLIDING 포즈에 SM 전용 치수 반환 가능.
3. `getPoses()` — 오버라이드 필요성: 탈것 dismount에서만 사용됨. SM 이식 초기에는 vanilla 그대로 유지 가능.
