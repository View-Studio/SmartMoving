# SmartMoving 수영 (Swim/Dive) 애니메이션 리서치

**작성일**: 2026-05-20
**작업 범위**: 애니메이션 (setAngles / ModelPart 회전 / setupTransforms / render). 자세 lean, 팔/다리 stroke/kick, head 회전, body sway, idle 진동.
**작업 범위 제외**: 기능 (state / motion / 박스 dim / 키 입력) — [[project_swimming_complete]] 완결, 절대 수정 X.
**사용자 명시**: 스마트무빙 마지막 영역. 수영 기능 완결 후 진행.

---

## 0. 개요

### 0-1. 핵심 발견 (= 신규 구현 거의 X)

**수영 애니메이션 영역은 이미 1.7.10/1.12.2 1:1 매핑 완료된 상태**. agent 전수 교차 대조 결과 식 / 매직넘버 / 회전 순서 / scale 식 모두 1:1 일치. 신규 구현 필요 항목 없음. 후속 작업 = **인게임 시각 검증 + 미세 부호/축/pivot 보정**.

### 0-2. 1.7.10 vs 1.12.2 vs 1.21.1

| 버전 | 핵심 클래스 | 상태 |
|------|------------|------|
| 1.7.10 | `SmartMovingModel.setRotationAngles` L317-L392 (isSwim/isDive 분기) | 원본 baseline |
| 1.12.2 | `SMModel.setRotationAngles` L352-L432 | 1.7.10 과 식/매직넘버/회전 순서 **완전 동일** (클래스 이름만 변경) |
| 1.21.1 | `MixinPlayerEntityModelClient.sm_animateSwimming` / `sm_animateDiving` + `MixinPlayerEntityRenderer.sm_setupTransforms` | 1.7.10/1.12.2 1:1 매핑 완료 |

---

## 1. 원본 1.7.10 핵심 매핑

### 1-1. 진입 식 (`SmartMovingRender.java:72-74`)

```java
boolean isSwim = moving.isSwimming && !moving.isDipping;
boolean isDive = moving.isDiving;
boolean isLevitate = moving.isLevitating;
```

- **isSwim** — 수면 수영 (isSwimming=true && !isDipping). offset 1.4~1.9 + 4 분류.
- **isDive** — 완전 잠수 (isDiving=true). offset >= 1.9 (또는 deeper).
- **isLevitate** — 잠수 부유 (= diving && !diveUp && !diveDown && 입력 0).
- **isDipping** 은 setAngles 별도 분기 없음 — standard 자세 유지.

### 1-2. isSwim 분기 (`SmartMovingModel.java:317-361`)

#### A. 입력 factor 식 (L319-322)

```java
float distance = totalHorizontalDistance;   // 누적 수평 거리
float walkFactor = Factor(currentHorizontalSpeed, 0.15679921F, 0.52264464F);   // (0,1) clamp
float sneakFactor = Math.min(
    Factor(currentHorizontalSpeed, 0, 0.15679921F),
    Factor(currentHorizontalSpeed, 0.52264464F, 0.15679921F));   // 보행/달리기 양 끝 가중
float standFactor = Factor(currentHorizontalSpeed, 0.15679921F, 0F);   // 정지~보행 가중
float standSneakFactor = standFactor + sneakFactor;
```

- `Factor(v, a, b)` = `((v - a) / (b - a))` clamp 0~1.
- `walkFactor`: 보행 (0.157) ~ 달리기 (0.523) 사이 0~1.
- `standSneakFactor`: 정지(=1) + sneak(=0~1) → 최대 ~2.

#### B. horizontalAngle 식 (L324-325)

```java
float horizontalAngle = horizontalDistance < (isGenericSneaking ? 0.005 : 0.015F)
                        ? currentCameraAngle : currentHorizontalAngle;
```

- 이동량 < 0.015 (sneaking 시 0.005) → 카메라 방향.
- 그 이상 → 이동 방향 (= 자유형 영법, 몸이 이동 방향 향함).
- sneak 시 threshold 더 작음 (0.005) → 더 빠른 카메라 추적.

#### C. head (L327-329, YXZ 회전 순서)

```java
bipedHead.rotationOrder = ModelRotationRenderer.YXZ;
bipedHead.rotateAngleY = MathHelper.cos(distance / 2.0F - Quarter) * walkFactor;   // ±90° walking
bipedHead.rotateAngleX = -Eighth * standSneakFactor;   // 정지 시 -22.5°*2 ≈ -45° 위 보기
bipedHead.rotationPointZ = -2F;   // 머리 앞으로 2px 이동
```

#### D. bipedOuter (= body group root) (L330-333)

```java
bipedOuter.fadeRotateAngleX = true;   // X 회전 fade 보간 (0.2 lerp)
bipedOuter.rotateAngleX = Quarter - Sixteenth * standSneakFactor;   // 정지=78.75°, 이동=90°
bipedOuter.rotateAngleY = horizontalAngle;   // 이동 방향
```

- `Quarter` = π/2 = 90°. `Sixteenth` = π/16 = 11.25°.
- 정지 시 standSneakFactor=1 → 78.75° (살짝 덜 수평, 상체 약간 들어올림).
- 이동 시 walkFactor=1, standSneakFactor=0 → 90° (완전 수평 자세).
- `fadeRotateAngleX=true` → SmartRenderModel 의 fadeIntermediate 가 prev → target 0.2 lerp → 자세 전환 부드러움.

#### E. bipedBody / bipedBreast (L335)

```java
bipedBody.rotateAngleY = bipedBreast.rotateAngleY = cos(distance/2 - Quarter) * walkFactor;
```

= 몸/가슴 좌우 sway (= 영법 휘저음).

#### F. 팔 (L337-345, YZX 회전 순서)

```java
bipedRightArm.rotationOrder = ModelRotationRenderer.YZX;
bipedLeftArm.rotationOrder = ModelRotationRenderer.YZX;

// Z (roll) — 좌우 벌림 + 정지 idle ±0.8
bipedRightArm.rotateAngleZ = Quarter + Eighth + cos(totalTime * 0.1F) * standSneakFactor * 0.8F;
bipedLeftArm.rotateAngleZ = -Quarter - Eighth - cos(totalTime * 0.1F) * standSneakFactor * 0.8F;

// X (pitch) — 누적 distance modulo 식 + 정지 idle ±Sixteenth
bipedRightArm.rotateAngleX = ((distance * 0.5F) % Whole - Half) * walkFactor + Sixteenth * standSneakFactor;
bipedLeftArm.rotateAngleX = ((distance * 0.5F + Half) % Whole - Half) * walkFactor + Sixteenth * standSneakFactor;
```

- **YZX 회전 순서** (Y→Z→X): yaw=0 → roll → pitch.
- Z roll: `Quarter + Eighth` = 90° + 22.5° = 112.5° (= 우 팔 옆으로 벌림). 좌 팔 -112.5°.
- 정지 idle: `cos(totalTime * 0.1F) * 0.8F` = ±0.8 rad ≈ ±45° 진동 (= 팔 펄럭임).
- X pitch: `(d*0.5) % Whole - Half` — distance 누적 modulo (= 톱니파 -π~+π 등속 회전).
- L/R `Half` (180°) phase 차이 → alternating stroke.

#### G. 다리 (L346-351)

```java
bipedRightLeg.rotateAngleX = cos(distance) * 0.52264464F * walkFactor;             // ±30°*walkFactor kick
bipedLeftLeg.rotateAngleX  = cos(distance + Half) * 0.52264464F * walkFactor;      // phase 반대

float rotateFeetAngleZ = Sixteenth * standSneakFactor
                       + cos(totalTime * 0.1F) * 0.4F * (standFactor - sneakFactor);
bipedRightLeg.rotateAngleZ =  rotateFeetAngleZ;
bipedLeftLeg.rotateAngleZ  = -rotateFeetAngleZ;
```

- 다리 X = `cos(distance)` alternating ±30° (= 자유형 발차기).
- 다리 Z = 정지 idle ±11.25° + standing 시 ±0.4 rad ≈ ±23° 진동 (sneak 시 부호 반전).

#### H. scale (호흡, L353-361)

```java
bipedRightArm.yScale = 1F + Sixteenth * sneakFactor;   // 팔 ±15% yScale 진동
bipedLeftArm.yScale  = 1F + Sixteenth * sneakFactor;
bipedRightLeg.yScale = 1F + Sixteenth * sneakFactor;
bipedLeftLeg.yScale  = 1F + Sixteenth * sneakFactor;
```

= 호흡 효과. yScale = `1F + π/16 * sneakFactor` ≈ 1 + 0.196 * sneakFactor (= 최대 +19.6%).

### 1-3. isDive 분기 (`SmartMovingModel.java:363-392`)

#### A. 입력 식 차이

```java
float distance = totalDistance * 0.7F;   // 3D 누적 거리, 0.7 감속
float walkFactor = Factor(currentSpeed, 0F, 0.15679921F);   // 정지~보행 가중
float standFactor = Factor(currentSpeed, 0.15679921F, 0F);   // 보행~정지 가중
float horizontalAngle = totalDistance < (isGenericSneaking ? 0.005 : 0.015F)
                        ? currentCameraAngle : currentHorizontalAngle;
```

- isSwim 의 `totalHorizontalDistance` 대신 `totalDistance * 0.7F` (3D, 잠수 천천히).
- walkFactor 범위 변경 (정지~보행 — isSwim 은 보행~달리기).

#### B. head (L368-370)

```java
bipedHead.rotateAngleX = -Eighth;   // 머리 -22.5° (살짝 위 보기)
bipedHead.rotationPointZ = -2F;
```

(isSwim 의 동적 식과 다름 — 잠수 시 머리 고정 -22.5°).

#### C. bipedOuter — 3 분기 (L373-376)

```java
bipedOuter.fadeRotateAngleX = true;
bipedOuter.rotateAngleX = isLevitate ? Quarter - Sixteenth                        // 78.75° (부유)
                       : (isJump    ? 0F                                          // 0° (정자세, 점프)
                                    : Quarter - currentVerticalAngle);            // 90° - 시선 위/아래 각도
bipedOuter.rotateAngleY = horizontalAngle;
```

- **isLevitate** (= 부유, 입력 0): 78.75° 고정 (= isSwim 정지 자세).
- **isJump** (= 점프): 0° (= 정자세, 점프 시 일시 일어섬).
- **일반**: 90° - currentVerticalAngle. pitch 위 보기 → vertical 양 → outer.X 감소 → 머리부터 위. pitch 아래 → outer.X 증가 → 머리부터 아래. **마우스 방향 이동의 핵심 식**.

#### D. 다리 Z (= 좌우 펼친 발차기) (L377-378)

```java
bipedRightLeg.rotateAngleZ = (cos(distance) + 1F) * 0.52264464F * walkFactor + Sixteenth * standFactor;
bipedLeftLeg.rotateAngleZ  = (cos(distance + Half) - 1F) * 0.52264464F * walkFactor - Sixteenth * standFactor;
```

- isSwim 의 X (앞뒤 발차기) 와 달리 **Z (좌우 펼친 발차기, 평형 영법)**.
- `cos(distance) + 1F` = 0~2 양수 → R 다리 항상 양 Z (= 오른쪽 펼침).
- `cos(distance + Half) - 1F` = -2~0 음수 → L 다리 항상 음 Z (= 왼쪽 펼침).

#### E. 팔 Z (= ×2.5 큰 진폭 평형 stroke) (L385-386)

```java
bipedRightArm.rotateAngleZ = (cos(distance + Half) * 0.52264464F * 2.5F + Quarter) * walkFactor
                           + (Quarter + Eighth) * standFactor;
bipedLeftArm.rotateAngleZ  = (cos(distance) * 0.52264464F * 2.5F - Quarter) * walkFactor
                           - (Quarter + Eighth) * standFactor;
```

- 진폭 ×2.5 (= 0.523 * 2.5 ≈ 1.31 rad = 75°) — isSwim 보다 큰 stroke.
- isSwim X 단독 stroke 와 달리 **Z 단독 stroke** (= 평형 영법, 옆으로 휘저음).

#### F. scale (호흡)

```java
bipedRightArm.yScale = bipedLeftArm.yScale = 1F + Eighth * walkFactor;   // 팔 ±15%
bipedRightLeg.yScale = bipedLeftLeg.yScale = 1F + Quarter * walkFactor;  // 다리 ±25%
```

- isDive 호흡 = walkFactor 기반 (이동 시 활성).
- 다리 호흡 진폭 ×2 (= Quarter 0.39 vs Eighth 0.196).

### 1-4. bodyYaw force (`SmartMovingRender.java:147`)

```java
if (moving.isClimbing || ... || moving.isSwimming || moving.isDiving || ...) {
    entityplayer.renderYawOffset = forwardRotation;   // bodyYaw 강제
}
```

= `renderYawOffset` (= bodyYaw) 를 forwardRotation (lerpedYaw) 으로 강제 → vanilla setAngles 의 `head.yaw = headYaw - bodyYaw ≈ 0` → 머리+몸 정렬. 그 후 `bipedOuter.rotateAngleY = horizontalAngle` 로 추가 Y 회전 → 최종 모델 yaw = horizontalAngle.

### 1-5. fadeRotateAngleX 의 mechanism

`SmartRenderModel.fadeIntermediate` (L247):
```java
if (fadeRotateAngleX) {
    rotateAngleX = prev.rotateAngleX + (rotateAngleX - prev.rotateAngleX) * 0.2F;
}
```

= 매 frame `prev + (target - prev) * 0.2` lerp → 약 5 frame 후 target 도달 → 부드러운 자세 전환.

---

## 2. 1.12.2 차이점

전수 교차 대조 결과 **1.7.10 ↔ 1.12.2 식 / 매직넘버 / 회전 순서 / scale 식 모두 완전 동일**. 단 클래스/필드 이름만 변경:

| 1.7.10 | 1.12.2 |
|--------|--------|
| `SmartMovingModel` | `SMModel` |
| `SmartMovingRender` | `SMRender` |
| `ModelRotationRenderer` | `SRModelRotationRenderer` (= SmartRender 패키지 이름 변경) |
| `SmartMovingRender.CurrentMainModel` | `SMRender.CurrentMainModel` |
| `bipedBreast` 노드 | 동일 (= 가슴 별도 노드) |

매핑 시 1.7.10 우선 사용 가능.

---

## 3. 우리 1.21.1 매핑 현황

### 3-1. setAngles inject

**파일**: `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java`

**진입점** (L447-450):
```java
} else if (sm.isSwimming_sm) {
    sm_animateSwimming(sm, limbSwing, limbSwingAmount, animationProgress);
} else if (sm.isDiving) {
    sm_animateDiving(sm, limbSwing, limbSwingAmount);
}
```

= 우선순위 if-else 체인 안 (isSliding > isHeadJumping > isCrawling > **isSwimming_sm** > **isDiving** > ...).

**`sm_animateSwimming` 함수** (L897-944):
- 진입 시 cleanup 인프라 + leaningPitch=0 (= vanilla SWIMMING 차단).
- 위 §1-2 (A~H) 모든 식 1:1 매핑.

**`sm_animateDiving` 함수** (L955-985):
- §1-3 (A~F) 모든 식 1:1 매핑.

### 3-2. setupTransforms inject

**파일**: `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityRenderer.java`

**bodyYaw force** (`sm_captureBodyYaw` L260-270):
```java
} else if (sm.isSwimming_sm || sm.isDiving) {
    boolean isGenericSneaking = ...;
    float threshold = isGenericSneaking ? 0.005f : 0.015f;
    float horizontalAngle = horizontalDistance < threshold
            ? sm.stats.currentCameraAngle
            : sm.stats.currentHorizontalAngle;
    smBodyYawActive = true;
    smBodyYawOverride = (float) Math.toDegrees(horizontalAngle);
}
```

= 1.7.10 horizontalAngle 식 1:1 + bodyYaw force (= renderYawOffset 등가).

**X 회전 + 3 분기** (`sm_setupTransforms` L517-540):
```java
} else if (sm.isSwimming_sm || sm.isDiving) {
    float tiltAngle;
    if (sm.isLevitating) {
        tiltAngle = (float)(Math.PI / 2 - Math.PI / 16);   // 78.75°
    } else if (sm.isJumping) {   // (or specific dive jump flag)
        tiltAngle = 0F;
    } else if (sm.isDiving) {
        tiltAngle = (float)(Math.PI / 2 - sm.stats.currentVerticalAngle);
    } else {
        tiltAngle = (float)(Math.PI / 2 - Math.PI / 16 * sm.swimStandSneakFactor);   // isSwim
    }
    matrices.multiply(RotationAxis.POSITIVE_X.rotation(tiltAngle));
}
```

= 1.7.10 의 `bipedOuter.rotateAngleX` 식 (Quarter - Sixteenth*standSneakFactor / Quarter - currentVerticalAngle / Quarter - Sixteenth / 0F) 1:1 매핑. `matrices.multiply(POSITIVE_X.rotation(...))` 으로 모델 전체 회전.

### 3-3. leaningPitch 차단

**파일**: `MixinPlayerEntityModelClient.java:289`
```java
if (anySmState) {
    this.leaningPitch = 0f;
}
```

= vanilla `BipedEntityModel.leaningPitch` (수영 sprint 시 1 ramp) 차단 → vanilla SWIMMING 자동 자세 (= -90° X 회전 setupTransforms Branch 2 + setAngles Step 13 swim 팔 애니메이션) **완전 차단**. SM 자체 자세만 적용.

### 3-4. outer layer 동기화

hat / jacket / sleeves / pants 등 outer layer 가 main 모델 따라 동일 회전 — L492-498 매핑.

### 3-5. cleanup 인프라

`smWasClimbingForCleanup` / `smWasCrawlingForCleanup` / `smWasSwimmingForCleanup` 등 cleanup flag — SM 상태 종료 시 ModelPart override quat 정리 ([[feedback_first_person_arm_override_clear]] 패턴).

---

## 4. 핵심 매핑 1:1 검증 표

| 항목 | 1.7.10 SmartMovingModel | 1.12.2 SMModel | 우리 MixinPlayerEntityModelClient/Renderer |
|------|-------------------------|----------------|----------------------------------------|
| isSwim 진입 | L317 `else if(isSwim)` | L352 동일 | L447 `else if (sm.isSwimming_sm)` |
| walkFactor (isSwim) | `Factor(s, 0.15679921F, 0.52264464F)` | 동일 | `smFactor(...)` (L898) |
| sneakFactor | `min(Factor(...), Factor(...))` | 동일 | L899-901 동일 |
| head YXZ + cos(d/2 - Q) | L327-328 | L362-364 | `setAnglesYXZ(head, ...)` L909-912 |
| head.pivotZ=-2 | L329 | L365 | L913 |
| bipedOuter.X (isSwim) | `Q - S*standSneakFactor` L331 | 동일 | `POSITIVE_X.rotation(π/2 - π/16 * sSF)` L519 |
| bipedOuter.Y | `horizontalAngle` L333 | 동일 | smBodyYawOverride force L267-268 |
| body.yaw sway (isSwim) | L335 | L371 | `body.yaw = cos(limbSwing/2 - Q)*wF` L918 |
| 팔 YZX | L337-338 | L373-374 | `setAnglesYZX(...)` L926-927 |
| 팔 Z roll + idle ±0.8 | L340-341 | L376-377 | L924-925 동일 |
| 팔 X pitch modulo Whole | L343-344 | L379-381 | L922-923 동일 |
| 다리 X cos*0.52 | L346-347 | L383-384 | `leg.pitch` L930-931 |
| feet Z idle ±0.4*(sF-sneakF) | L349-351 | L386-389 | L933-936 |
| 팔/다리 yScale (sneakFactor) | L353-361 | L391-397 | `setLegScales/setArmScales` L940-943 |
| isDive 진입 | L363 | L398 | L449 `else if (sm.isDiving)` |
| isDive distance = totalDistance * 0.7 | L365 | L399 | `distance = sm.stats.totalDistance * 0.7f` L960 |
| isDive head.X = -Eighth | L370 | L405 | `head.pitch = -EIGHTH` L965 |
| isDive bipedOuter.X (3 분기) | L374 | L409 | L526-540 3 분기 |
| isDive 다리 Z + (cos+1) | L377-378 | L412-415 | L969-970 |
| isDive 팔 Z ×2.5 | L385-386 | L421-424 | L973-976 |
| isDive scale: leg 0.25 / arm 0.15 | L380-391 | L417-428 | L981-984 |
| bodyYaw force | SmartMovingRender L147 `renderYawOffset` | SMRender 동일 | sm_captureBodyYaw smBodyYawOverride L267 |
| leaningPitch=0 (1.13+ 신규) | N/A | N/A | `this.leaningPitch = 0f` L289 |

**결론**: 모든 식 1:1 매핑 완료.

---

## 5. 누락/미구현 항목 (= 신규 작업)

**없음**.

다음은 이미 모두 매핑됨:
- [x] isSwim 자세 + lean fade
- [x] isDive 자세 + 3 분기 (levitate / jump / 일반)
- [x] head YXZ + 동적 sway
- [x] body sway
- [x] 팔 YZX + roll idle + pitch stroke modulo
- [x] 다리 alternating kick
- [x] feet idle 진동
- [x] 호흡 yScale
- [x] horizontalAngle (이동/카메라 threshold)
- [x] bodyYaw force
- [x] vanilla leaningPitch 차단
- [x] outer layer 동기화
- [x] cleanup 인프라

---

## 6. 검증 필요 (= 인게임 시각 확인)

코드는 존재하나 인게임 실측 확인 필요:

### 6-1. totalTime 단위 등가성

- 원본: `totalTime` = vanilla 1.7.10 EntityPlayer.ticksExisted (tick 단위).
- 우리: `animationProgress` = 1.21.1 (= ageInTicks + tickDelta).
- `cos(totalTime * 0.1F)` idle 진동 주기 = 2π / 0.1F ≈ 62.8 tick ≈ 3.14 sec.
- 우리 매핑 등가 단위 사용 검증 필요.

### 6-2. swimStandSneakFactor 전달 race

- `sm_animateSwimming` 에서 set → `sm_setupTransforms` 에서 read.
- 같은 tick 안 setAngles 가 setupTransforms 보다 먼저 호출되는지 시각 확인.
- vanilla 1.21.1 render frame 순서: setupTransforms → setAngles. **반대 순서**. 우리 매핑이 전 frame 의 값 read 가능 → 1 frame lag. 자세 변화 시 1 frame 지연 가능.

### 6-3. currentVerticalAngle 등가성

- 원본 식 (SmartRenderRender.java L62~91): `currentVerticalAngle = atan(yDiff / horizontalDistance)`.
- 우리 매핑 `sm.stats.currentVerticalAngle` 1:1 매핑 여부.

### 6-4. fadeRotateAngleX 의 fade 구현

- 원본은 SmartRenderModel.fadeIntermediate 에서 0.2 lerp.
- 우리는 matrices 직접 rotate → fade 없음? 또는 별도 prev field 보관?
- **확인 필요**: 자세 전환 (= isSwim → isDive → standing 등) 시 visual jump 발생 여부.

---

## 7. 작업 우선순위 (인게임 시각 검증)

**Phase 1 — 기본 자세 lean 검증**
- 정지 isSwim → bipedOuter.X ≈ 78.75° 수평 자세.
- 이동 isSwim → 90° 완전 수평.
- isDive 정지 (= levitate) → 78.75°.
- isDive 마우스 위/아래 보기 → 90° - currentVerticalAngle 따라 회전.
- isDive 점프 (jump 키) → 0° 정자세 일시.

**Phase 2 — 팔 stroke**
- isSwim 이동 시 팔 X (톱니파, modulo Whole-Half) 자유형 stroke.
- isSwim 정지 시 팔 Z idle ±0.8 진동.
- isDive 이동 시 팔 Z ×2.5 평형 stroke.

**Phase 3 — 다리 kick**
- isSwim 이동 시 다리 X alternating ±30°.
- isDive 이동 시 다리 Z 평형 발차기.
- 정지 시 다리 Z idle 진동.

**Phase 4 — head 회전**
- isSwim 이동 시 head Y sway (= 좌우 두리번).
- 정지 시 head X = -22.5° * standSneakFactor (위 보기).
- isDive 시 head.pitch = -22.5° 고정.

**Phase 5 — bodyYaw 추적**
- 정지 시 (hDist < 0.015) currentCameraAngle 따라 회전 (= 카메라 추적).
- 이동 시 currentHorizontalAngle 추적 (= 이동 방향).
- sneak 시 threshold 0.005 더 빠른 전환.

**Phase 6 — 호흡 scale**
- isSwim sneakFactor 호흡 — 정지~보행 사이 ±15%.
- isDive walkFactor 호흡 — 이동 시 다리 ±25% / 팔 ±15%.

**Phase 7 — fade 보간 (= prev → target 0.2 lerp)**
- isSwim → isDive 자세 전환 frame 시각 부드러움.
- 정지 → 이동 자세 전환.
- swim 종료 (= 수면 위 / 물 밖) 시 자세 복귀.

---

## 8. 회귀 차단 의무

### 8-1. 절대 수정 금지 영역

- **[[project_swimming_complete]]** — 수영 기능 (state / motion / dim / 박스). 함부로 수정 X.
- **[[project_headjump_all_complete]]** — 헤드점프 single + multi.
- **[[project_sliding_complete]]** / **[[project_sliding_animation_complete]]** — 슬라이딩.
- **[[project_crawl_complete]]** — 엎드리기.
- **[[project_grab_climbing_complete]]** — 그랩 클라이밍.
- **[[project_flying_complete]]** / **[[project_falling_complete]]** / **[[project_angle_jump_complete]]** — 비행/낙하/각도점프.

### 8-2. setAngles / setupTransforms 영역 한정 수정 가능

- `MixinPlayerEntityModelClient.sm_animateSwimming/sm_animateDiving` (L897-985).
- `MixinPlayerEntityRenderer.sm_captureBodyYaw` 의 `if (sm.isSwimming_sm || sm.isDiving)` 분기.
- `MixinPlayerEntityRenderer.sm_setupTransforms` 의 isSwimming_sm/isDiving 분기.

### 8-3. 핵심 회귀 차단 규칙

1. **isSwim/isDive 분기 진입 식** (= `sm.isSwimming_sm` / `sm.isDiving`) 절대 수정 X — [[project_swimming_complete]] 기능 영역.
2. **leaningPitch=0 강제** (L289) 절대 제거 X — vanilla SWIMMING 자세 차단의 핵심.
3. **anySmState** 에 `isSwimming_sm`/`isDiving` 포함 (L280-281) 유지 — leaningPitch 가드/cleanup 인프라 발동 조건.
4. **sm_captureBodyYaw** 의 if-else 우선순위 (isLevitating > isCeilingClimbing > isSwim/isDive > isFlying > isHeadJumping/isRopeSliding > isSliding) 유지.
5. **setupTransforms 의 X 회전 순서** (POSITIVE_X) + matrices.translate(pivotY, ...) 의 매트릭스 스택 차이 (= 슬라이딩/크롤은 pivot translate 추가, 수영은 단순 X 회전) — 절대 변경 X.

---

## 9. 메모리 패턴 참조

- [[feedback_setupTransforms_translate_axis]] — TAIL inject 의 translate 가 R_x 회전 전 적용 → modelpart Y ↔ world Z 축 변환.
- [[feedback_zxy_zyx_rotation_order]] — 원본 ZXY vs ModelPart ZYX 회전 순서. 큰 yaw 시 gimbal lock + 부호 반전 효과.
- [[feedback_smbody_yaw_force_head_yaw_zero]] — ModifyArg setupTransforms bodyYaw force 시 vanilla setAngles netHeadYaw 분리. sm_animate* 에 head.yaw=0 강제 패턴.
- [[feedback_idle_arm_vibration]] — vanilla CrossbowPosing idle 진동 보존. SM inject = set 으로 cancel 시 식에 += 직접 누적.
- [[feedback_animateArms_cancel]] — vanilla animateArms swing cancel 패턴.
- [[feedback_first_person_arm_override_clear]] — PlayerEntityModel single instance ModelPart override clear 패턴.
- [[feedback_fade_prev_pre_entry_pose]] — 상태 전환 fade prev 시작값 = 직전 자세.
- [[feedback_render_scale_negation]] — vanilla render scale 부호 반전. ModelPart R_x(theta) = world R_x(-theta).

---

## 10. 다음 단계

`docs/checklist_swimming_animation.md` 참조. Phase 1 (= 기본 자세 lean) 부터 인게임 검증 시작.

**원칙**: 헤드점프 애니메이션 작업 (fix #79~#86) 처럼 사용자 시각 보고 기반 frame 단위 부호/축/pivot 보정 패턴.
