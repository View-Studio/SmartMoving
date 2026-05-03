# 엎드리기 (Crawl) 애니메이션 1:1 매핑 리서치

**작업**: 원본 SmartMovingModel.setRotationAngles 의 isCrawl 분기를 1.21.1 sm_animateCrawling 과 라인별 1:1 대조 + 차이 fix.

**원본 위치**: `SmartMovingModel.java` L393-L436.
**1.21.1 위치**: `MixinPlayerEntityModelClient.sm_animateCrawling` L805-L855.

---

## 1. 라인별 1:1 대조

### 1-1. 입력 변수 (L395-L397)

**원본**:
```java
L395: float distance = totalHorizontalDistance * 1.3F;
L396: float walkFactor = Factor(currentHorizontalSpeedFlattened, 0F, 0.12951545F);
L397: float standFactor = Factor(currentHorizontalSpeedFlattened, 0.12951545F, 0F);
```

**1.21.1**:
```java
L815: float distance    = limbSwing * 1.3f;
L816: float walkFactor  = smFactor(limbSwingAmount, 0f, 0.12951545f);
L817: float standFactor = smFactor(limbSwingAmount, 0.12951545f, 0f);
```

**❌ 차이 (HIGH)**:
- distance: `limbSwing` (vanilla limbAnimator) → 원본 `totalHorizontalDistance` (SM stat).
- walkFactor/standFactor: `limbSwingAmount` (vanilla) → 원본 `currentHorizontalSpeedFlattened` (SM stat 평탄화).

**그랩 클라이밍 작업과 동일 패턴** — limbSwing/limbSwingAmount 가 vanilla limbAnimator (ground walk 기준) 으로 SM 누적과 차이 가능. 정정 필요.

`currentHorizontalSpeedFlattened` 는 SmartStatistics L17 에 이미 존재 (`flattened * 0.5 + currentHorizontalSpeed * 0.5`).

### 1-2. 머리 (L399-L401)

**원본**:
```java
L399: bipedHead.rotateAngleZ = -viewHorizontalAngelOffset / RadiantToAngle;
L400: bipedHead.rotateAngleX = -Eighth;
L401: bipedHead.rotationPointZ = -2F;
```

**1.21.1**:
```java
L820: head.roll  = -headYaw * DEG_TO_RAD;
L821: head.pitch = -EIGHTH;
L822: head.pivotZ = -2f;
```

**✅ 1:1**. `viewHorizontalAngelOffset` = `headYaw` (vanilla netHeadYaw) 의미 동등.

### 1-3. 몸통 (L403-L407)

**원본**:
```java
L403: bipedTorso.rotationOrder = ModelRotationRenderer.YZX;
L404: bipedTorso.rotateAngleX = Quarter - Thirtytwoth;
L405: bipedTorso.rotationPointY = 3F;
L406: bipedTorso.rotateAngleZ = MathHelper.cos(distance + Quarter) * Sixtyfourth * walkFactor;
L407: bipedBody.rotateAngleY = MathHelper.cos(distance + Half) * Sixtyfourth * walkFactor;
```

**1.21.1**:
```java
L825: body.pitch = QUARTER - THIRTYTWOTH;
L826: body.roll  = MathHelper.cos(distance + QUARTER) * SIXTYFOURTH * walkFactor;
L827: body.yaw   = MathHelper.cos(distance + HALF) * SIXTYFOURTH * walkFactor;
L828: body.pivotY = 3f;
```

**❌ 차이 (HIGH)**:
- 원본: bipedTorso (X + Z) + bipedBody (Y) 분리. bipedBody 가 bipedTorso 자식.
- 1.21.1: body 단일 노드. X+Y+Z 모두 적용.
- **회전 순서**: 원본 `bipedTorso.rotationOrder = YZX` 명시. 1.21.1 ModelPart 기본 ZYX.
- body.pitch (큰 값 0.78rad ≈ 78°) + body.yaw (작은 cos) + body.roll (작은 cos) 동시 → ZYX vs YZX 미세 차이 가능.

**setAnglesYZX 헬퍼 사용 권장** (그랩 클라이밍 팔 분기 패턴).

다만 — bipedBody.Y 가 bipedTorso 자식 효과로 누적되는데, 1.21.1 single body 노드는 자식 효과 없음 (= body 의 Y 값 자체). 결과 동등 추정 (X 큰 + Y/Z 작은 → 자식 효과 미세).

### 1-4. 다리 (L409-L418)

**원본**:
```java
L409: bipedRightLeg.rotateAngleX = (cos(distance - Quarter) * Sixtyfourth + Thirtytwoth) * walkFactor + Thirtytwoth * standFactor;
L410: bipedLeftLeg.rotateAngleX = (cos(distance - Half - Quarter) * Sixtyfourth + Thirtytwoth) * walkFactor + Thirtytwoth * standFactor;
L412: bipedRightLeg.rotateAngleZ = (cos(distance - Quarter) + 1F) * 0.25F * walkFactor + Thirtytwoth * standFactor;
L413: bipedLeftLeg.rotateAngleZ = (cos(distance - Quarter) - 1F) * 0.25F * walkFactor - Thirtytwoth * standFactor;
L415-418: setLegScales(...) — `if (scaleLegType != NoScaleStart)` 가드 안.
```

**1.21.1**:
```java
L831-834: rightLeg.pitch / leftLeg.pitch — 식 동일.
L835-836: rightLeg.roll  / leftLeg.roll  — 식 동일.
L849-851: setLegScales(...) — 가드 없음.
```

**✅ 1:1** (식 자체). `scaleLegType != NoScaleStart` 가드 누락 — 메인 모델 가정 시 통과 → 결과 동일. 명시성만 차이.

### 1-5. 팔 (L420-L435)

**원본**:
```java
L420: bipedRightArm.rotationOrder = ModelRotationRenderer.YZX;
L421: bipedLeftArm.rotationOrder = ModelRotationRenderer.YZX;
L423-424: bipedRightArm.rotateAngleX = Half + Eighth, leftArm 동일.
L426-427: rightArm.Z / leftArm.Z = cos 식.
L429-430: rightArm.Y = -Quarter, leftArm.Y = Quarter.
L432-435: setArmScales(...) — `if (scaleArmType != NoScaleStart)` 가드 안.
```

**1.21.1**:
```java
L843: setAnglesYZX(rightArm, HALF + EIGHTH, -QUARTER, rRoll);
L844: setAnglesYZX(leftArm,  HALF + EIGHTH,  QUARTER, lRoll);
L852-854: setArmScales(...) — 가드 없음.
```

**✅ 1:1** (rotationOrder=YZX 헬퍼 사용 + pitch/yaw/roll 식 동일). scale 가드 누락 — 동일 차이.

---

## 2. 진정한 1:1 차이 항목 (확정)

### 2-1. ❌ **distance 입력** (limbSwing → totalHorizontalDistance) [HIGH]

**Fix**:
```java
// 그랩 클라이밍 패턴 동일 — partial tick lerp 적용
float partialTicks = SmartMovingClientState.globalCachedTickDelta;
float distance = sm.stats.getTotalHorizontalDistance(partialTicks) * 1.3f;
```

### 2-2. ❌ **walkFactor/standFactor 입력** (limbSwingAmount → currentHorizontalSpeedFlattened) [HIGH]

**Fix**:
```java
float walkFactor  = smFactor(sm.stats.currentHorizontalSpeedFlattened, 0f, 0.12951545f);
float standFactor = smFactor(sm.stats.currentHorizontalSpeedFlattened, 0.12951545f, 0f);
```

원본은 partial tick lerp 안 적용 (currentHorizontalSpeedFlattened 직접 read). 1.21.1 도 동일.

### 2-3. ❌ **body 회전 순서 YZX 명시** [HIGH]

**현재**:
```java
body.pitch = QUARTER - THIRTYTWOTH;
body.roll  = ...;
body.yaw   = ...;
```
→ ModelPart 기본 ZYX.

**Fix**:
```java
setAnglesYZX(body,
        QUARTER - THIRTYTWOTH,                       // pitch (X)
        cos(distance + HALF) * SIXTYFOURTH * walkFactor,   // yaw (Y)
        cos(distance + QUARTER) * SIXTYFOURTH * walkFactor);  // roll (Z)
```

원본 `bipedTorso.rotationOrder = YZX` 1:1.

### 2-4. ⚠️ **scale 가드 누락** [LOW]

**현재**: setLegScales / setArmScales 항상 호출.
**원본**: `if (scaleLegType != NoScaleStart)` / `scaleArmType != NoScaleStart` 가드 안.

메인 모델 = Scale 모드라 통과 → 결과 동일. 명시성만. **선택사항**.

### 2-5. ✅ **다른 항목 모두 1:1**

- 머리 (head.roll/pitch/pivotZ) — 1:1.
- 다리 식 (pitch/roll) — 1:1.
- 팔 식 + setAnglesYZX (yaw=±QUARTER, pitch=Half+Eighth, roll cos) — 1:1.

---

## 3. 작업 우선순위

| 우선 | 항목 | 영향 | 한 줄 수정 가능 |
|------|------|------|----------------|
| HIGH | (2-1) distance 입력 정정 + lerp | 진자운동 부드러움 / 보폭 정확 | YES |
| HIGH | (2-2) walkFactor/standFactor 입력 정정 | 정지 시 손/발 자세 정확 | YES |
| HIGH | (2-3) body rotationOrder YZX | body Y/Z 작은 회전 정확 | setAnglesYZX 호출 |
| LOW | (2-4) scale 가드 명시 | 명시성만 (결과 동일) | 선택사항 |

---

## 4. 사용자 의도 검증 안 한 항목

- **viewHorizontalAngelOffset 의미** — `headYaw` 매개변수 = vanilla netHeadYaw 동등 (확인됨). ✓
- **bipedBody (Y) vs body 단일 노드 차이** — 자식 효과 미세 (X 큰 + Y/Z 작은). 정밀 1:1 위해 별도 자식 노드 매핑 어려움 (ModelPart parallel 구조).

---

# 부록 — 2026-05-03 세션 재리서치 (Phase 2)

이전 (1-3 섹션) 리서치의 fix 들 (= distance 입력, walkFactor/standFactor 입력, setAnglesYZX) 은 모두 이미 적용됨 — 현재 `sm_animateCrawling:844-893` 가 그 결과.

## 부록.1 — 적용 후 잔존 issue 재검토

원본 라인별 1:1 비교 다시:

| 원본 라인 | 원본 코드 | 우리 매핑 위치 | 우리 코드 | 일치 |
|----------|----------|---------------|----------|------|
| L399 | `bipedHead.rotateAngleZ = -viewHorizontalAngelOffset/RadiantToAngle` | L854 | `head.roll = -headYaw * DEG_TO_RAD` | ✅ |
| L400 | `bipedHead.rotateAngleX = -Eighth` | L855 | `head.pitch = -EIGHTH` | ✅ |
| L401 | `bipedHead.rotationPointZ = -2F` | L856 | `head.pivotZ = -2f` | ✅ |
| L403 | `bipedTorso.rotationOrder = YZX` | L862 | `setAnglesYZX(body, ...)` | ✅ |
| L404 | `bipedTorso.rotateAngleX = Quarter - Thirtytwoth` | L863 | `QUARTER - THIRTYTWOTH` | ⚠️ **자식 효과 누락** |
| L405 | `bipedTorso.rotationPointY = 3F` | L866 | `body.pivotY = 3f` | ⚠️ **자식 효과 누락** |
| L406-L407 | bipedTorso/Body Y/Z | L864-L865 | 동일 | ✅ |
| L409-L413 | 다리 X/Z | L869-L874 | 동일 | ✅ |
| L417-L418 | legScales | L887-L889 | 동일 | ✅ |
| L420-L421 | 팔 rotationOrder = YZX | L881-L882 | `setAnglesYZX(arm, ...)` | ✅ |
| L423-L430 | 팔 X/Y/Z | L877-L882 | 동일 | ✅ |
| L434-L435 | armScales | L890-L892 | 동일 | ✅ |

**수치 매핑 100% 1:1**. 그러나 **자식 효과 (= bipedTorso 회전/위치가 head/arm/leg 에 영향) 미매핑**.

## 부록.2 — 핵심 차이: 부모-자식 계층 vs 평탄 모델

### 원본 (1.7.10 SR) — 계층 모델
```
bipedOuter (root)
  └ bipedTorso (= 회전/위치 부모)
      ├ bipedHead
      ├ bipedBody (몸통 시각)
      ├ bipedLeftArm / bipedRightArm
      └ bipedLeftLeg / bipedRightLeg
```

- `bipedTorso.rotateAngleX = 78.75°` → **head/body/arm/leg 모두 78.75° 회전**.
- `bipedTorso.rotationPointY = 3F` → **head/body/arm/leg 모두 +3 이동**.

### 우리 (1.21.1) — 평탄 모델
```
PlayerEntityModel
  ├ head (root)
  ├ body (root)
  ├ leftArm / rightArm (root)
  └ leftLeg / rightLeg (root)
```

- `body.pitch = 78.75°` → **body 만 회전**. head/arm/leg 영향 없음.
- `body.pivotY = 3f` → **body 만 이동**. head/arm/leg 영향 없음.

### 시각 결과 차이
- 원본: 사용자 평지 누운 자세 (= 머리 정면 + 팔 위로 뻗음 + 다리 약간 굽힘 + 모든 부분 78.75° 누움).
- 우리: body 만 누워있고 head/arm/leg 가 standing 위치 그대로. **자세 어색함**.

## 부록.3 — 메모리 `feedback_animation_porting.md` 권장 방식

```
❌ setAngles에서 body.pitch만 변경 (파트 분리 문제 미해결)
Why: flat 모델에서 body.pitch=79°로 body만 기울면 arm/leg pivot 위치는 변하지 않음.
원본 계층 모델에서 bipedTorso 회전 시 자식(arm/leg)의 3D 위치가 함께 변하는 것을 재현 불가.

✅ 올바른 접근법:
1. 크롤링/슬라이딩: setupTransforms에서 엔티티 회전 + resetPivots() + LOCAL 각도
   - heightOffset = 0F (필수)
```

→ **isSliding 은 `sm_setupTransforms` 에서 entity 회전 매핑됨** (`MixinPlayerEntityRenderer.java:390-400`):
```java
if (sm.isSliding) {
    float tiltAngle = (float) Math.PI / 2f; // Quarter
    matrices.multiply(RotationAxis.POSITIVE_X.rotation(tiltAngle));
    sm.smOuterTiltX = tiltAngle;
    matrices.translate(0f, 5f / 16f, 0f);   // bipedOuter.rotationPointY = 5F
    matrices.translate(0f, -0.4f / 16f, 0f);  // bipedBody.offsetY = -0.4F
}
```

→ **isCrawling 은 `sm_setupTransforms` 미매핑** = 누락.

## 부록.4 — 권장 fix (옵션 A)

### A.1 `MixinPlayerEntityRenderer.sm_setupTransforms` 에 isCrawling 분기 추가
```java
if (sm.isCrawling && !sm.isClimbing) {
    // 원본 bipedTorso.rotateAngleX = Quarter - Thirtytwoth = 78.75°
    float tiltAngle = (float)(Math.PI / 2 - Math.PI / 16);
    // 부호 반전 (memory feedback_render_scale_negation.md — vanilla scale(-1,-1,1) 보정)
    matrices.multiply(RotationAxis.POSITIVE_X.rotation(-tiltAngle));
    sm.smOuterTiltX = tiltAngle;
    // 원본 bipedTorso.rotationPointY = 3F (단위: 픽셀, ModelPart /16 변환)
    matrices.translate(0f, 3f / 16f, 0f);
}
```

### A.2 `sm_animateCrawling` 에서 entity 회전 적용 항 제거
- `body.pitch` 의 `QUARTER - THIRTYTWOTH` 항 제거 (= entity 회전이 대신).
- `body.pivotY = 3f` 제거 (= entity translate 가 대신).
- 머리/팔/다리 LOCAL 각도 (= 원본 자식 노드 자체 회전) 는 그대로 유지.
- `head.pivotZ = -2f` 같은 LOCAL pivot 보정도 유지.

```java
private void sm_animateCrawling(SmartMovingClientState sm, float headYaw) {
    // 입력 변수 동일

    // 머리 (LOCAL — 원본 bipedHead 자체 회전)
    head.roll  = -headYaw * DEG_TO_RAD;
    head.pitch = -EIGHTH;
    head.pivotZ = -2f;

    // 몸통 (= entity 회전이 대신, LOCAL Y/Z 작은 진동만)
    setAnglesYZX(body,
            0f,                                             // ← QUARTER - THIRTYTWOTH 제거
            cos(distance + HALF) * SIXTYFOURTH * walkFactor,
            cos(distance + QUARTER) * SIXTYFOURTH * walkFactor);
    // body.pivotY = 3f 제거 (entity translate 가 대신)

    // 다리/팔 (LOCAL — 원본 자식 노드 자체 회전)
    // ... (기존 그대로)
}
```

### A.3 `heightOffset = 0F` 검증
- 메모리 명시: 엔티티 회전 방식은 heightOffset=0 필수.
- 우리 isCrawling 시 heightOffset=-1F (= dim height 0.8 매핑). 발 정렬 차이 가능.
- → setupTransforms entity 회전 적용 후 박스 위치 + 사용자 시점 검증 필수.

## 부록.5 — 회귀 검증 항목

옵션 A 적용 시 영향 가능 시스템:
1. **isCrawlClimbing** (메모리 `project_isCrawlClimbing_complete.md`) — sm_animateCrawling 변경 → 자세 차이.
2. **사용자 시점** — entity 회전 시 카메라 위치 변화. 1인칭 자세 검증.
3. **MixinCapeFeatureRenderer** — `smOuterTiltX` 사용 → 망토 회전 영향.
4. **`(isCrawling && isClimbing)` 분기** (메모리 `project_isCrawlClimbing_complete.md` Fix 7) — 자세 매핑 동시 적용 시 회귀 검토.

## 부록.6 — 작업 종결 상태 (리서치만)

- [x] 원본 SmartMovingModel.java L393-L435 라인별 추출.
- [x] 원본 상수 + 회전 순서 (YZX) 확인.
- [x] 원본 부모-자식 계층 (bipedTorso = 부모) 확인.
- [x] 우리 매핑 sm_animateCrawling 본체 라인별 분석.
- [x] 우리 매핑 sm_setupTransforms 의 isCrawling 분기 부재 확인.
- [x] 자식 효과 누락 (= 핵심 잔존 issue) 식별.
- [x] 메모리 권장 방식 (= 옵션 A) 확인.
- [x] fix 단계별 + 회귀 검증 항목 정리.

**다음 단계** (= 별도 작업, 사용자 결정 대기):
- [ ] 체크리스트 작성 (`docs/checklist_crawl_animation.md`)
- [ ] 옵션 A 단계별 적용
- [ ] 인게임 검증
