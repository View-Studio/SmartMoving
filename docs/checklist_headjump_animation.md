# 헤드점프 애니메이션 — 원본 1:1 정정 체크리스트

> **🔴 사용자 명시 (2026-05-11)**: **헤드점프 *기능 자체* 는 완결. 절대 건드리지 말 것.**
>
> - 본 작업 = **순수 setAngles 영역 한정** (= `MixinPlayerEntityModelClient.sm_animateHeadJumping` 본체 + 동일 mixin 의 sm_setAngles inject).
> - 절대 건드리지 말 것:
>   - `MixinPlayerEntityRenderer.sm_setupTransforms` 의 헤드점프 분기 (L702-706) = 카메라/박스 인프라.
>   - `SmartMovingClientState` 의 새 필드 추가 = state 흐름 영역.
>   - `SmartMovingClient` / `SmartMovingClimber` / `SmartMovingJumper` 등 tick 흐름.
>   - `heightOffset` / `smOuterTiltX_prev` / `smOuterFade_prevTime` 등 카메라 안정화 / fade 인프라.
> - 관련 완결 메모리 (수정 금지):
>   - `project_headjump_complete.md` (2026-05-08) — Phase 0-K 18건 + 카메라 시점 안정화 30단계.
>   - `project_transition_box_drop_complete.md` (2026-05-10) — transition box drop 7 fix.
>   - `project_fox_movement_complete.md` (2026-05-09) — 헤드점프+자체슬라이딩 중첩 BUG 3 fix.

---

> **기반 리서치**: `docs/research_headjump_animation.md` (2026-05-11).
> **원본 본체**: `SmartMovingModel.java` L505-530 (1.7.10) / `SMModel.java` L544-568 (1.12.2) — 식 자체 1:1 동일.
> **우리 매핑 — *애니메이션 영역* (작업 대상)**:
> - `MixinPlayerEntityModelClient.sm_animateHeadJumping` L1317-1344 (회전식 본체).
> **우리 매핑 — *기능 영역* (수정 금지, 참조만)**:
> - `MixinPlayerEntityRenderer.sm_setupTransforms` L702-706 (bipedOuter X 기울기).
> - `MixinPlayerEntityRenderer.sm_modifyBodyYaw` L339-342 (bipedOuter Y 회전).
> - `MixinPlayerEntityModelClient.computeSmallOverGroundHeight` L1494-1504.
> - `SmartStatistics.currentVerticalAngle` L137.

---

## 1. 라인별 1:1 대조 표 (원본 → 우리 매핑)

| # | 원본 식 (1.7.10 L#) | 우리 매핑 위치 | 영역 | 상태 |
|---|----------------------|----------------|------|------|
| 1 | `bipedOuter.fadeRotateAngleX = true` (L507) | `setupTransforms` L704 `matrices.multiply(R_x(theta))` (fade 없음) | **기능** | ⚠️ 누락 (수정 금지 — §2-B) |
| 2 | `bipedOuter.rotateAngleX = (Quarter - currentVerticalAngle)` (L508) | `setupTransforms` L703 `theta = π/2 - angle` | **기능** | ✅ |
| 3 | `bipedOuter.rotateAngleY = currentHorizontalAngle` (L509) | `modifyBodyYaw` L341 `smBodyYawOverride = toDegrees(angle)` | **기능** | ✅ |
| 4 | `bipedHead.rotateAngleX = -bipedOuter.rotateAngleX / 2F` (L511) | `sm_animateHeadJumping` L1330 `head.pitch = -(QUARTER - angle) / 2f` | **애니** | ✅ (등가) |
| 5 | `bendFactor = min(Factor(angle,Q,0), Factor(angle,-Q,0))` (L513) | `sm_animateHeadJumping` L1322 | **애니** | ✅ |
| 6 | `bipedRightArm.rotateAngleX = bendFactor * -Eighth` (L514) | L1323 | **애니** | ✅ |
| 7 | `bipedLeftArm.rotateAngleX = bendFactor * -Eighth` (L515) | L1324 | **애니** | ✅ |
| 8 | `bipedRightLeg.rotateAngleX = bendFactor * -Eighth` (L517) | L1325 | **애니** | ✅ |
| 9 | `bipedLeftLeg.rotateAngleX = bendFactor * -Eighth` (L518) | L1326 | **애니** | ✅ |
| 10 | `armFactorZ = Factor(angle, Quarter, -Quarter)` (L520) | L1333 | **애니** | ✅ |
| 11 | `if (overGroundBlock != null && getMaterial().isSolid())` (L521) | L1334 `if (sm.smallOverGroundHeight < 5f)` | **혼합** | ⚠️ 누락 (수정 보류 — §2-A) |
| 12 | `armFactorZ = Math.min(armFactorZ, smallOverGroundHeight / 5F)` (L522) | L1335 동일 | **애니** | ✅ |
| 13 | `bipedRightArm.rotateAngleZ = Half - Sixteenth + armFactorZ * Eighth` (L524) | L1337 | **애니** | ✅ |
| 14 | `bipedLeftArm.rotateAngleZ = Sixteenth - Half - armFactorZ * Eighth` (L525) | L1338 `-(HALF - SIXTEENTH) - armFactorZ * EIGHTH` | **애니** | ✅ (등가) |
| 15 | `legFactorZ = Factor(angle, -Quarter, Quarter)` (L527) | L1341 | **애니** | ✅ |
| 16 | `bipedRightLeg.rotateAngleZ = Sixtyfourth * legFactorZ` (L528) | L1342 | **애니** | ✅ |
| 17 | `bipedLeftLeg.rotateAngleZ = -Sixtyfourth * legFactorZ` (L529) | L1343 | **애니** | ✅ |
| 18 | `smallOverGroundHeight = ... ? getOverGroundHeight(5D) : 0F` (Render L89) | `MixinPlayerEntityModelClient` L399-406 (inject 안) | **기능** | ✅ |
| 19 | `overGroundBlock = ... ? getOverGroundBlockId(...) : null` (Render L90) | (계산 안 함, 필드도 부재) | **기능** | ⚠️ 누락 (수정 보류 — §2-A 와 연결) |

### 영역 구분
- **애니** (애니메이션 한정): 본 작업 가능 영역. `sm_animateHeadJumping` 메서드 본체 + setAngles inject.
- **기능** (기능 인프라): **수정 금지**. setupTransforms / modifyBodyYaw / state 필드 / tick 흐름.
- **혼합**: 검사 조건은 `sm_animateHeadJumping` 안 (애니) 이지만, 입력 데이터 (`overGroundBlock`) 가 기능 영역에서 계산되어야 함. 결과적으로 **수정 보류** (state 필드 추가 필요).

---

## 2. 잠재 누락 항목 — *수정 보류* (사용자 확인 / 인게임 보고 대기)

### ✅ §2-C — head.yaw 마우스 따라 회전 *(해소, 2026-05-11)*

#### 사용자 보고
헤드점프 시 머리가 마우스 움직임에 따라 움직임. 원본은 머리 고정 (이동 방향 기준).

#### 원인
- 우리 매핑 `sm_animateHeadJumping` 가 `head.pitch` 만 명시 set, **`head.yaw` cancel 누락**.
- `setupTransforms ModifyArg sm_modifyBodyYaw` 가 setAngles bodyYaw 인자만 force → `entity.bodyYaw` 필드 자유 값 → vanilla netHeadYaw = (headYaw lerp - 자유 bodyYaw lerp) 잔존.
- 결과: head.yaw = (마우스 yaw - 자유 bodyYaw) → 마우스 따라 큰 회전.
- 원본 1.7.10 은 `entity.renderYawOffset` 필드 자체 force → netHeadYaw = (마우스 - force 된 이동 방향) → 사용자 시각 "거의 고정".

#### fix
`sm_animateHeadJumping` 본체에 `head.yaw = 0f` 한 줄 추가:
```java
head.pitch = -(QUARTER - angle) / 2f;
head.yaw = 0f;  // ★ vanilla netHeadYaw cancel
```

→ head world yaw = setupTransforms 의 force bodyYaw (= 이동 방향) → 마우스 무관 고정.

#### 결과 — 사용자 검증 "잘된다" (2026-05-11)
다른 SM 분기 (sliding/flying/crawling/angleJump) 와 동일 패턴.
`feedback_smbody_yaw_force_head_yaw_zero.md` 의 미적용 분기 목록에서 isHeadJumping 해소.

---

### ✅ §2-A — `overGroundBlock.getMaterial().isSolid()` 필터 *(해소, 2026-05-11)*

**진짜 원인 발견 + fix 완결**. 디버그 dump 측정 결과 *에지 케이스* 아닌 *항상 발생하는 큰 차이*.

#### 원본 정확 동작 (재검토)
원본 `getOverGroundBlockId` (SmartMovingBase.java L862-882):
```java
int y = floor(boundingBox.minY);
for(; y >= minY; y--) {
    Block block = world.getBlock(x, y, z);  // 1.7.10 air 도 non-null
    if(block != null) return block;          // 항상 true → 첫 iteration 즉시 반환
}
```

→ 박스 발 위치 (`floor(bb.minY)`) 블록 1회 검사 후 반환. **헤드점프 박스 발 = 공중 (mixin offset 활성 → entity.y+1m) → air → isSolid=false → 원본 clamp 미적용**.

#### 디버그 dump 검증 (2026-05-11)
- `MixinPlayerEntityModelClient.sm_animateHeadJumping` 임시 dump 추가.
- 일반 헤드점프 + 여우무빙 시나리오 각 1회 측정.
- **모든 시점 `origIsSolid=false`, `blockId=air`** 확인.

#### fix 적용
`sm_animateHeadJumping` armFactorZ clamp 조건에 `isOverGroundBlockSolid(player)` 검사 추가:
```java
float armFactorZ = smFactor(angle, QUARTER, -QUARTER);
if (sm.smallOverGroundHeight < 5f && isOverGroundBlockSolid(player)) {
    armFactorZ = Math.min(armFactorZ, sm.smallOverGroundHeight / 5f);
}
```

`isOverGroundBlockSolid` 헬퍼 신규 추가 — 박스 발 위치 단일 검사, 1.21.1 collision shape 비어있지 않음 = isSolid 등가. `AbstractClientPlayerEntity` (멀티 친화).

#### 결과 — 사용자 검증 "잘된다" (2026-05-11)
하강 시 armFactorZ ≈ 0.9 도달 (= 팔 9π/8 ≈ 202° 가깝게 모임 = 슈퍼맨 두 손 합치는 자세). 기존 매핑 대비 rArmRoll 차이 최대 약 40°.

#### 관련 메모리
`feedback_headjump_overground_air_is_solid.md`.

---

### ⚠️ §2-B — `bipedOuter.fadeRotateAngleX = true` 5-tick lerp 누락 *(수정 보류)*

#### 원본 의도
원본 L507 `bipedOuter.fadeRotateAngleX = true` → 다음 프레임의 X 회전을 `prev + (target - prev) * deltaT * 0.2F` 로 보간 (≈ 5-tick 시정수).

#### 현재 매핑 동작
`MixinPlayerEntityRenderer.sm_setupTransforms` L702-706 → `matrices.multiply(R_x(theta))` 즉시 적용. fade 없음.
또한 L728-732 → 헤드점프 진입 시 `smOuterTiltX_prev = 0` 매 frame 리셋 → fade 인프라 무효.

#### 왜 수정 보류
- `setupTransforms` 는 `project_headjump_complete.md` 의 *카메라 시점 안정화 30단계* + `project_transition_box_drop_complete.md` 의 *transition box drop 7 fix* 가 정착된 영역.
- fade lerp 추가 = `smOuterTiltX_prev` / `smOuterFade_prevTime` 인프라 변경 → 카메라 안정화 fade 인프라와 상호작용.
- 비행 분기와 같은 prev 갱신 패턴 도입 시 비행 ↔ 헤드점프 전환 frame 의 prev 공유 영향 가능 → 카메라 spike 회귀 위험.
- 사용자 명시 "헤드점프 기능 자체 = 완결, 절대 건드리지 말 것".

#### 재검토 조건
- 사용자가 인게임 테스트에서 *헤드점프 진입/이탈 시 X 기울기 갑자기 점프* 보고 시 재검토.
- 또는 사용자가 명시적으로 "fade lerp 추가해도 된다" 승인 시.

---

## 3. 현재 매핑 상태 결론

| 영역 | 식 개수 | 상태 |
|------|---------|------|
| **애니메이션 영역** (sm_animateHeadJumping 본체 + 등가 식) | 14 식 | **14/14 ✅ 완벽 1:1** |
| **기능 영역** (setupTransforms / modifyBodyYaw / smallOverGroundHeight) | 3 식 | 3/3 ✅ (라인 #2/#3/#18) |
| **혼합/누락** (isSolid 필터 + overGroundBlock 필드) | 2 식 | 2/2 ⚠️ 누락 (보류 — §2-A) |
| **누락** (fadeRotateAngleX lerp) | 1 식 | 1/1 ⚠️ 누락 (보류 — §2-B) |

→ **순수 애니메이션 영역 (sm_animateHeadJumping 본체) 은 14/14 모두 1:1 완벽 매핑**. 본 작업 대상 영역에는 정정할 항목 없음.

→ **누락 3 식 모두 기능 영역 영향** 으로 분류, **수정 보류**.

---

## 4. 작업 순서

### 진행 중 작업
- [x] **회전 중심점 fix (2026-05-11, 사용자 보고 "모델 전체가 아래로 회전")** — `MixinPlayerEntityRenderer.sm_setupTransforms` L702-706 헤드점프 X 기울기 분기를 비행 분기와 동일 패턴으로 정정.
  - (a) `translate(0, +1.5, 0)` / `translate(0, -1.5, 0)` head pivot 회전 중심 보정 추가 (`feedback_rotation_pivot_pattern.md`).
  - (b) 회전 부호 `+theta` → `-theta` (= `scale(-1,-1,1)` 좌표계 보정, `feedback_render_scale_negation.md`).
  - 원본 비행 (L484-485) 과 헤드점프 (L507-508) 모두 `bipedOuter.rotateAngleX = (Quarter - verticalAngle)` 동일 식 → 우리 매핑도 동일 패턴이어야 1:1.
- [x] **모델 높이 fix (2026-05-11, 사용자 보고 "모델 1칸 낮음")** — `MixinPlayerEntityRenderer.sm_getPositionOffset` 헤드점프 분기를 `Vec3d.ZERO` 반환으로 변경.
  - 기존: `new Vec3d(0, heightOffset, 0)` = `(0, -1, 0)` → 박스 발 (= `entity.y +1m`) 보다 모델 머리가 0.5m 아래 = 박스 밖 아래.
  - 변경 후: 모델 origin = `entity.y` → 모델 root = `entity.y +1.501m` = 박스 안 (= 슬라이딩과 동일 패턴).
  - `feedback_sliding_entity_y_push_offset_zero.md` 참조 — isSliding (= ZERO) 와 일관성. 박스 처리 (mixin offset / POSE=SLIDING) 는 그대로 둠 (기능 영역 보존).

### 즉시 작업 (현재 비어있음)
*없음* — 회전 중심점 fix 후 추가 디테일은 인게임 검증 결과 대기.

### 인게임 검증 (사용자 작업)
1. 헤드점프 인게임 시각 검증:
   - 진입 시 몸 기울기 자연스러운지.
   - 다이브 자세 (수평 시 팔/다리 굽힘) 원본과 비교.
   - 착지 직전 팔 모음 자세 정상 동작.
   - 상승/하강 시 다리 벌어짐 정도 확인.
2. 위 §2-A / §2-B 의 *재검토 조건* 에 해당하는 보고 사항 발견 시 메모리 갱신 후 재검토.

---

## 5. 검토 외 / 추가 확인 사항

### ⚠️ 5-1. animateArms / vanilla swing 후처리
원본 `setRotationAngles` 의 isHeadJump 분기는 *vanilla setAngles* 를 완전히 대체 (isStandard=false). 우리 매핑은 `sm_animateHeadJumping` 호출 후 vanilla `animateArms` 가 *handSwingProgress > 0 시 body.yaw / arm.pitch 등 덮어쓸 가능성*:
- 메모리 `feedback_animateArms_cancel.md` 참조 — 비행/낙하 분기에서 swing 보존 패턴 적용.
- 헤드점프는 swing 보존 패턴 미적용 → 헤드점프 중 좌클릭 시 *armFactorZ 매핑 자세* 가 swing 으로 덮어씌워질 가능성.
- **본 작업 범위**: `sm_animateHeadJumping` 본체에 swing 가드 추가 = 애니메이션 영역 → 가능.
- **단**: 인게임 보고 없는 *추측 분기*. 메모리 `feedback_no_premature_defer.md` 와 `feedback_simple_first_when_user_hints.md` 사이 — 사용자 보고 후 추가 fix 결정. **현재 보류**.

### ⚠️ 5-2. isJumping abs 가드 부재 (확인 완료)
원본 비행 분기 (L481): `verticalAngle = isJump ? abs(currentVerticalAngle) : currentVerticalAngle`.
헤드점프 분기 (L508): 단순 `Quarter - currentVerticalAngle` — abs 없음. 점프 상승 < 90°, 하강 > 90°. **원본 의도된 동작**.
우리 매핑 setupTransforms L703 도 abs 없음 → 일치. ✅

### ⚠️ 5-3. heightOffset / setHeightOffset(-1)
원본 `setHeightOffset(-1)` (SmartMovingSelf L2129) — 헤드점프 진입 시 1m 박스 down.
우리 매핑 L65-66 `if (sm.isHeadJumping && sm.heightOffset != 0f) cir.setReturnValue(new Vec3d(0, sm.heightOffset, 0))`.
**기능면 완결** (`project_headjump_complete.md`) → 본 작업 범위 외.

---

## 6. 위험 / 함부로 수정 금지 영역 (재강조)

본 작업 = **`sm_animateHeadJumping` 본체 (L1317-1344) 와 동일 mixin 의 sm_setAngles inject 한정**.

**절대 건드리지 말 것**:
- `MixinPlayerEntityRenderer.sm_setupTransforms` L702-706 (헤드점프 X 기울기 인프라).
- `MixinPlayerEntityRenderer` 의 fade prev 인프라 (smOuterTiltX_prev, smOuterFade_prevTime, smCachedBodyYawLaggedDeg).
- `SmartMovingClientState` 의 새 필드 추가 (= state 흐름 영역).
- `SmartMovingClient` / `SmartMovingClimber` / `SmartMovingJumper` 등 tick 흐름.
- `MixinPlayerEntityModelClient.computeSmallOverGroundHeight` (= 입력 데이터 계산 — 기능).
- `heightOffset` / `processStatePacket` 등 state/박스/카메라 인프라.

관련 완결 메모리:
- `project_headjump_complete.md` (2026-05-08).
- `project_transition_box_drop_complete.md` (2026-05-10).
- `project_fox_movement_complete.md` (2026-05-09).

---

**체크리스트 끝.**
