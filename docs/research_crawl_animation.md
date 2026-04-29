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
