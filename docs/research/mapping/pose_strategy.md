# EntityPose 전략 교차 분석 — R-12

작성 기준: RESEARCH_RULES.md 규칙 3 (교차 분석). 코드 근거 없는 주장 금지.

의존 리서치:
- `docs/research/vanilla/EntityPose_system.md` (R-07)
- `docs/research/vanilla/LivingEntity_updatePose.md` (R-07)
- `docs/research/vanilla/PlayerEntity_pose_dimensions.md` (R-07)
- `docs/research/vanilla/Entity_calculateDimensions.md` (R-07)
- `docs/research/vanilla/LivingEntity_isInSwimmingPose.md` (R-07)
- `docs/research/original/smartmoving/moving/SmartMovingSelf.md` (R-01~R-06)
- `docs/research/original/smartmoving/playerapi/SmartMovingServerPlayerBase.md` (R-04)

---

## 1. SM 1.7.10 히트박스 시스템 원본 코드 근거

### 1-1. `setHeightOffset(float offset)` 전체 (SmartMovingSelf.md 확인됨)

```java
// SmartMovingSelf.java (1681-1703줄 확인)
private void setHeightOffset(float offset) {
    resetHeightOffset();
    heightOffset = offset;
    sp.boundingBox.minY -= heightOffset;   // -= (-1) → minY += 1
    sp.height += heightOffset;             // += (-1) → height = 1.8 - 1 = 0.8
}

private void resetHeightOffset() {
    sp.boundingBox.minY += heightOffset;   // 원위치
    sp.height -= heightOffset;             // 원위치
    heightOffset = 0F;
}
```

`offset = -1F` 적용 결과:
- `sp.boundingBox.minY` += 1F (바닥이 1블록 위로 올라감)
- `sp.height` = 1.8F - 1F = **0.8F**

### 1-2. `setHeightOffset(-1F)` 호출 조건 (SmartMovingSelf.md 확인됨)

```
// 크롤링/수영/다이빙/헤드점프 시 공통 적용 (SmartMovingSelf.md 2154줄 주석 확인)
이 상태들에서 setHeightOffset(-1F) 공통 적용
→ height = 0.8F, isSmall bit = (height < 1) = true
```

명시적 확인:
- `isSwimming || isDiving` → `setHeightOffset(-1F)` (SmartMovingSelf.md line 475 확인)
- `isHeadJumping = true; setHeightOffset(-1)` (SmartMovingSelf.md line 1567 확인)

`isCrawling`의 setHeightOffset 적용 여부: [미확인 — SmartMovingSelf.md에서 직접 대응 코드를 찾지 못함. 단, SmartMovingSelf.md 2154줄 주석에서 "크롤링" 포함이라고 명시되어 있음. 추가 확인 필요]

### 1-3. `isSmall` 비트 정의 (SmartMovingSelf.md line 1953 확인됨)

```
| 1 | isSmall (height < 1) |
```

`height = 0.8F` → `height < 1` → `isSmall = true` 상태 패킷 인코딩.

서버 처리 (SmartMovingServer.setSmall(), SmartMovingServerPlayerBase.md 확인됨):
```java
public void setSmall(boolean isSmall)
{
    mp.setHeight(isSmall ? 0.8F : 1.8F);
    this.isSmall = isSmall;
}
```

### 1-4. 히트박스 크기 요약 (1.7.10 원본)

| SM 상태 | width | height | 비고 |
|---------|-------|--------|------|
| `isHeadJumping` | 0.6F | 0.8F | `setHeightOffset(-1)` 확인됨 (line 1567) |
| `isSliding` | 0.6F | 0.8F | headJumping 착지 후 전환 → heightOffset 유지 추정 [미확인] |
| `isCrawling` | 0.6F | 0.8F | SmartMovingSelf.md 2154줄 주석 근거 |
| `isSwimming` | 0.6F | 0.8F | `setHeightOffset(-1F)` 확인됨 |
| `isDiving` | 0.6F | 0.8F | `setHeightOffset(-1F)` 확인됨 |
| `isCeilingClimbing` | — | — | [미확인 — setHeightOffset 호출 여부 미확인] |
| `isClimbing` | 0.6F | 1.8F | setHeightOffset 미적용 (확인됨) |

---

## 2. vanilla 1.21.1 EntityPose 히트박스 (코드 근거)

### 2-1. POSE_DIMENSIONS (PlayerEntity_pose_dimensions.md 확인됨)

```java
// PlayerEntity.java:135 직접 확인
private static final Map<EntityPose, EntityDimensions> POSE_DIMENSIONS = ImmutableMap.<EntityPose, EntityDimensions>builder()
    .put(EntityPose.STANDING,    EntityDimensions.changing(0.6F, 1.8F).withEyeHeight(1.62F))
    .put(EntityPose.CROUCHING,   EntityDimensions.changing(0.6F, 1.5F).withEyeHeight(1.27F))
    .put(EntityPose.SWIMMING,    EntityDimensions.changing(0.6F, 0.6F).withEyeHeight(0.4F))
    .put(EntityPose.FALL_FLYING, EntityDimensions.changing(0.6F, 0.6F).withEyeHeight(0.4F))
    // ...
    .build();
```

**SLIDING 포즈 (index 15)는 POSE_DIMENSIONS에 없음** → `getBaseDimensions(SLIDING)` → `STANDING_DIMENSIONS` 폴백 (Entity_calculateDimensions.md B-01 확인됨).

### 2-2. getBaseDimensions() Mixin 포인트 (코드 근거)

```java
// PlayerEntity.java:2061 확인됨
@Override
public EntityDimensions getBaseDimensions(EntityPose pose) {
    return (EntityDimensions)POSE_DIMENSIONS.getOrDefault(pose, STANDING_DIMENSIONS);
}

// LivingEntity.java:3395 확인됨
public final EntityDimensions getDimensions(EntityPose pose) {
    // final — 직접 오버라이드 불가
    return pose == EntityPose.SLEEPING ? SLEEPING_DIMENSIONS : this.getBaseDimensions(pose).scaled(this.getScale());
}
```

**SM 커스텀 치수 적용 경로: `getBaseDimensions()` Mixin만 가능** (`getDimensions()`는 final).

### 2-3. updatePose() 로직 (LivingEntity_updatePose.md 확인됨)

```java
// PlayerEntity.updatePose() (method_7318) 전체 확인됨
protected void updatePose() {
    if (this.canChangeIntoPose(EntityPose.SWIMMING)) {
        // ... 포즈 결정 로직 ...
        this.setPose(entityPose2);
    }
}
```

**canChangeIntoPose(SWIMMING) = false이면 entire body 실행 안 됨** → 현재 포즈 유지.

### 2-4. isInSwimmingPose() 영향 (LivingEntity_isInSwimmingPose.md 확인됨)

SWIMMING 포즈 설정 시 자동으로 영향받는 vanilla 코드:

| 호출 위치 | 영향 | 코드 |
|----------|------|------|
| `updateLeaningPitch()` | `leaningPitch` +0.09F/틱 → max 1.0F | `isInSwimmingPose() == true → leaningPitch++` |
| `setupTransforms()` | `leaningPitch > 0` → X축 -90° 회전 적용 | LivingEntity_isInSwimmingPose.md 확인 |
| `setupTransforms()` | `isInSwimmingPose() → translate(0,-1,0.3)` | 렌더 위치 이동 |
| `getLeashPos()` | 리쉬 위치 오프셋 변경 | 시각적 효과만 |

---

## 3. C-02: 헤드점프(isHeadJumping) EntityPose 전략 확정

### 3-1. 요구사항

SM 원본 (SmartMovingSelf.md line 1567 확인):
```java
if(head) { isHeadJumping = true; setHeightOffset(-1); }
```
- **히트박스**: 0.6W × 0.8H
- **렌더**: 플레이어가 서 있는 자세 유지 (수평 회전 없음)
- **용도**: 1블록 이하 천장 공간을 통과하는 점프

### 3-2. 선택지 분석

| 포즈 | hitbox | 렌더 영향 | 문제점 |
|------|--------|----------|--------|
| STANDING | 0.6×1.8 | 없음 | 히트박스가 너무 큼 — 1.8H 공간 필요 |
| CROUCHING | 0.6×1.5 | 없음 | 히트박스 여전히 큼 — 1.5H 공간 필요 |
| SWIMMING | 0.6×0.6 | `leaningPitch` 증가 → setupTransforms X축 -90° 회전 | 렌더 충돌: 플레이어가 수평으로 누워버림 |
| SLIDING (index 15) | 0.6×1.8 폴백→Mixin 필요 | 없음 | SLIDING 표준 치수 없음, Mixin으로 0.8H 정의 가능 |

### 3-3. 결정: SWIMMING 포즈 + `isInSwimmingPose()` Mixin 억제

**근거**: 1.21.1에서 POSE_DIMENSIONS에 등록된 포즈 중 0.8H에 가장 근접한 것은 SWIMMING(0.6H). CROUCHING(1.5H)은 1블록 공간을 통과 불가.

**충돌 해결**: `isInSwimmingPose()` Mixin에서 `isHeadJumping` 시 `false` 강제 반환.

```java
// Mixin: PlayerEntityMixin
@Inject(method = "isInSwimmingPose", at = @At("RETURN"), cancellable = true)
private void smSuppressHeadJumpSwimmingPose(CallbackInfoReturnable<Boolean> cir) {
    if (SmartMovingContext.get((PlayerEntity)(Object)this).isHeadJumping) {
        cir.setReturnValue(false);
        // → updateLeaningPitch() 증가 억제
        // → setupTransforms() X축 회전 및 translate(0,-1,0.3) 억제
    }
}
```

**히트박스 차이**: SM 1.7.10 = 0.8H, 1.21.1 SWIMMING = 0.6H. **0.2H 차이 있음.**
- 1.7.10에서는 높이 0.8이상~1.0미만 공간에서 헤드점프 가능
- 1.21.1 SWIMMING(0.6H)에서는 높이 0.6이상~1.0미만 공간에서도 가능 → 실제보다 더 좁은 공간 통과 가능
- 이 차이를 허용하거나, SLIDING 포즈에 0.8H 치수를 부여하는 대안 검토 필요

**대안: SLIDING 포즈 + 0.8H 치수 (Mixin)**:
- `getBaseDimensions()` Mixin에서 SLIDING → `EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.72F)` 반환
- 렌더 영향 없음 (isInSwimmingPose() = false 유지)
- SM 1.7.10 원본 히트박스와 정확히 일치

**최종 결정**:

- **isHeadJumping → EntityPose.SLIDING**
  - 이유: SM 1.7.10 원본 0.8H 히트박스 유지 + 렌더 충돌 없음
  - `getBaseDimensions()` Mixin에서 SLIDING → `(0.6F, 0.8F, eyeHeight=0.72F)` 반환

충돌:
- vanilla `updatePose()`가 SLIDING을 자동으로 설정하지 않음 → SM이 직접 `setPose(SLIDING)` 필요
- SLIDING 포즈는 `updatePose()` 우선순위 체인에 없음 → vanilla가 매 틱 덮어쓸 수 있음
- **대응**: `updatePose()` Mixin에서 `isHeadJumping` 시 SLIDING 강제 설정 + vanilla 취소

---

## 4. C-03: 슬라이딩(isSliding) EntityPose 전략 확정

### 4-1. 요구사항

SM 원본 (SmartMovingSelf.md 확인):
```java
// isSliding 진입: isHeadJumping 착지 후
if(Config.isSlidingEnabled() && (grabButton.Pressed || wasHeadJumping))
    isSliding = true;
```

- **히트박스**: [미확인 — isSliding 상태에서 setHeightOffset 호출 여부 미확인]
  - 추론: headJumping에서 전환되므로 높이 0.8F 유지 가능성이 있으나, 코드에서 직접 확인 필요
  - `isSmall` 비트 = `height < 1`: isSliding 상태에서 이 비트가 true인지 서버 측 확인 필요
- **렌더**: 슬라이딩 자세 (수평이 아닌 엎드린 형태)
- **용도**: 빠르게 이동하며 낮은 자세 유지

### 4-2. 결정: EntityPose.SLIDING + 커스텀 치수 (Mixin)

**근거**: 
1. vanilla 1.21.1에 `SLIDING` EntityPose(index 15)가 이미 존재 (EntityPose_system.md B-01 확인됨)
2. SLIDING은 POSE_DIMENSIONS에 없음 → `getBaseDimensions()` Mixin으로 자유롭게 치수 정의 가능
3. `isInSwimmingPose()`에 영향 없음 → `leaningPitch` 무관계, 렌더 충돌 없음
4. SM의 슬라이딩 렌더는 자체 애니메이션으로 처리 (별도 R-13 청크)

```java
// Mixin: PlayerEntityMixin.getBaseDimensions()
@Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
private void smGetBaseDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
    if (pose == EntityPose.SLIDING) {
        // isHeadJumping과 동일한 0.8H 치수 (SM 1.7.10 기준)
        // eyeHeight: 0.72F = 0.8F × 0.9 (SWIMMING 비율 참고)
        cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.72F));
    }
}
```

**히트박스 치수**: [미확인 — isSliding 시 SM 1.7.10 실제 height 값 확인 필요. 현재 0.8F 추정이나 코드 직접 확인 필요]

충돌:
- vanilla `updatePose()`가 SLIDING을 설정하지 않음 → `updatePose()` Mixin에서 `isSliding` 시 SLIDING 강제 설정
- SLIDING 포즈의 눈 높이: 미확인 → 임시값 0.72F 사용, 추후 검증 필요

---

## 5. calculateDimensions() 연동 방법 확정

### 5-1. 자동 호출 체인 (코드 근거)

```
// Entity.java:onTrackedDataSet() 확인됨
public void onTrackedDataSet(TrackedData<?> data) {
    if (POSE.equals(data)) {
        this.calculateDimensions();   // ← setPose() 시 자동 호출
    }
}
```

**SM에서 `setPose(EntityPose.SLIDING)` 또는 `setPose(EntityPose.SWIMMING)` 호출만 하면 `calculateDimensions()` → `getBaseDimensions()` → 커스텀 치수 → `refreshPosition()` → 바운딩 박스 갱신이 모두 자동 처리됨.**

DataTracker로 동기화 → 클라이언트에서도 동일하게 `onTrackedDataSet()` → `calculateDimensions()` 자동 호출.

### 5-2. updatePose() Mixin 설계

```java
// Mixin: PlayerEntityMixin
@Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
private void smOverridePose(CallbackInfo ci) {
    SmartMovingContext ctx = SmartMovingContext.get((PlayerEntity)(Object)this);
    if (ctx == null) return;
    
    if (ctx.isCrawling) {
        this.setPose(EntityPose.SWIMMING);   // 0.6H — leaningPitch, 렌더 회전 모두 vanilla 적용
        ci.cancel();
    } else if (ctx.isHeadJumping) {
        this.setPose(EntityPose.SLIDING);    // 0.8H (Mixin 커스텀) — leaningPitch 무관계
        ci.cancel();
    } else if (ctx.isSliding) {
        this.setPose(EntityPose.SLIDING);    // 0.8H — leaningPitch 무관계
        ci.cancel();
    }
    // 그 외: vanilla updatePose() 정상 실행
}
```

**주의**: `@At("HEAD"), cancellable = true` 패턴 — vanilla 전체를 건너뛰므로 SM 상태가 아닐 때 반드시 정상 진행해야 함.

### 5-3. PlayerEntity의 recalculateDimensions 제외 (코드 근거)

```java
// Entity.calculateDimensions() 확인됨
if (!this.world.isClient
    && !this.firstUpdate
    && !this.noClip
    && bl
    && (크기 증가 조건)
    && !(this instanceof PlayerEntity)) {   // ← PlayerEntity 제외
    this.recalculateDimensions(entityDimensions);
}
```

**PlayerEntity는 포즈 복원 시 벽 충돌 보정이 없음.** SM이 SLIDING/SWIMMING → STANDING으로 복원할 때 공간 확인을 SM 자체적으로 처리해야 함.

---

## 6. 전체 충돌 목록

| 번호 | 충돌 내용 | 관련 vanilla 코드 | 해결 방법 |
|------|---------|-----------------|---------|
| 1 | SWIMMING 포즈 → `leaningPitch` 자동 증가 | `updateLeaningPitch()` (method_6072) | `isInSwimmingPose()` Mixin: `isHeadJumping` 시 false 반환 (→ headJumping이 SLIDING 포즈를 쓰므로 이 충돌은 SLIDING 방식에서 해소됨) |
| 2 | SWIMMING 포즈 → `setupTransforms()` X축 -90° + translate(0,-1,0.3) | `setupTransforms()` (method_4212) | isCrawling에는 적용, isHeadJumping에는 불필요 → SLIDING 포즈 사용으로 충돌 해소 |
| 3 | vanilla `updatePose()` 매 틱 포즈 덮어씀 | `updatePose()` (method_7318) | `@At("HEAD") cancellable=true` Mixin으로 SM 상태 우선 적용 |
| 4 | SLIDING 포즈에 기본 치수 없음 (STANDING 폴백) | `PlayerEntity.getBaseDimensions()` (method_55694) | `getBaseDimensions()` Mixin에서 SLIDING → 0.6×0.8 반환 |
| 5 | 포즈 복원 시 벽 충돌 보정 없음 | `calculateDimensions()` PlayerEntity 예외 | SM 자체적으로 포즈 복원 전 공간 확인 필요 |
| 6 | `canChangeIntoPose(SWIMMING)` 선행 체크 | `updatePose()` 최상단 if 조건 | `@At("HEAD")` 취소로 해당 체크 자체를 우회 |

---

## 7. 미확인 항목

| ID | 미확인 내용 | 추가 필요 작업 |
|----|------------|------------|
| M-01 | `isCrawling` 상태에서 `setHeightOffset(-1F)` 명시적 호출 위치 (SmartMovingSelf.java 전체 코드에서 직접 확인 필요) | SmartMovingSelf.java 원본 GitHub에서 크롤링 진입/유지 코드 재확인 |
| M-02 | `isSliding` 상태에서 실제 height 값 (서버 측 `isSmall` bit 확인 필요) | state 패킷 인코딩에서 isSliding과 isSmall 동시 true인지 확인 |
| M-03 | `isCeilingClimbing` hitbox (setHeightOffset 호출 여부 미확인) | SmartMovingSelf.java에서 isCeilingClimbing 진입 시 heightOffset 처리 확인 |
| M-04 | SLIDING 포즈의 눈 높이 (0.72F는 임시값) | SM 원본 렌더/카메라 코드에서 crawling/sliding 중 eye height 참고값 확인 |
| M-05 | `updatePose()` `@HEAD` 취소 시 vanilla 크롤링 자동 발동(SWIMMING 강제) 억제 여부 확인 | `canChangeIntoPose(SWIMMING)` 체크와의 상호작용 테스트 필요 |

---

## 8. 구현 항목 요약 (C-02, C-03)

| 구현 항목 | Mixin 대상 | 방법 | 상태 |
|---------|-----------|------|------|
| headJumping 포즈 설정 | `PlayerEntity.updatePose()` | `@HEAD cancellable=true` → `setPose(SLIDING)` | 설계 확정 |
| sliding 포즈 설정 | `PlayerEntity.updatePose()` | `@HEAD cancellable=true` → `setPose(SLIDING)` | 설계 확정 |
| crawling 포즈 설정 | `PlayerEntity.updatePose()` | `@HEAD cancellable=true` → `setPose(SWIMMING)` | 설계 확정 |
| SLIDING 커스텀 치수 | `PlayerEntity.getBaseDimensions()` | `@HEAD cancellable=true` → `0.6×0.8` | 설계 확정, 수치 M-02 미확인 |
| leaningPitch 억제 | `LivingEntity.isInSwimmingPose()` | `@RETURN cancellable=true` → isHeadJumping/isSliding 시 false | SLIDING 포즈로 불필요해짐 |

**미확인(M-01~M-05) 해소 후 구현 시작 가능.**
