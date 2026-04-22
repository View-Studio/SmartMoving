# 교차 분석 — 애니메이션 시스템

원본: SmartRender + SmartMoving (1.7.10 Forge, net.smart.render / net.smart.moving.render)  
대상: vanilla 1.21.1 Fabric (Yarn 매핑)

---

## 원본 아키텍처 요약

```
SmartRenderRender.renderPlayer()
  → SmartStatistics에서 이동 데이터 수집
  → 모든 모델 레이어에 데이터 세팅
  → CurrentMainModel = modelBipedMain
  → irp.superRenderPlayer() (vanilla 렌더 파이프라인 진입)
       → SmartRenderModel.setRotationAngles()
            → reset()
            → imp.animateHeadRotation()  ← SmartMovingModel.animateHeadRotation()
                 → setRotationAngles(): 11가지 SM 이동 상태 분기 → isStandard 판정
            → imp.animateSleeping() / animateArmSwinging() / animateRiding() / ...
  → CurrentMainModel = null

SmartMovingRender.renderPlayer()
  → SmartMoving 상태 → SmartMovingModel 플래그에 반영
  → irp.superRenderRenderPlayer() (위 파이프라인)
```

---

## 1.21.1 vanilla 렌더 파이프라인 요약

```
PlayerEntityRenderer.render()
  → setModelPose(entity): model.sneaking / armPose 세팅
  → LivingEntityRenderer.render():
       → model.handSwingProgress / riding / child 세팅
       → bodyYaw / headYaw 보간
       → matrices.scale(entity.getScale())
       → setupTransforms() → PlayerEntityRenderer.setupTransforms():
            Branch 1 (isFallFlying): X회전
            Branch 2 (leaningPitch > 0): X회전(-90°) + translate(0,-1,0.3)
            Branch 3 (else): super만
       → matrices.scale(-1,-1,1) + scale(0.9375) + translate(0,-1.501,0)
       → limbSwing / limbSwingAmount: entity.limbAnimator.getPos/Speed(tickDelta)
       → model.animateModel(): model.leaningPitch = entity.getLeaningPitch(tickDelta)
       → model.setAngles() → PlayerEntityModel.setAngles() → BipedEntityModel.setAngles()
       → model.render()
       → features 렌더 (망토, 갑옷 등)
```

---

## ModelRotationRenderer (원본 핵심 파트 타입)

- **원본 동작**: vanilla `ModelRenderer` 상속. GL11 직접 사용(glPushMatrix/Pop, glTranslatef, glRotatef, glScalef). 6가지 회전 순서(XYZ/XZY/YXZ/YZX/ZXY/ZYX). scaleX/Y/Z 런타임 변경. offsetX/Y/Z. ignoreBase/ignoreRender/forceRender 플래그. ignoreSuperRotation(GL_MODELVIEW_MATRIX에서 translation만 추출). fade 보간 시스템(RendererData previous, timeDelta * 0.2F). preTransforms/postTransforms 재귀(루트→자신 변환 누적).
- **1.21.1 대응**: `ModelPart` (Yarn: `net/minecraft/client/model/ModelPart`, intermediary `net/minecraft/class_3542`) — 없음에 가까움 (직접 구현 필요)
- **동작 차이**:
  - **회전 순서**: vanilla `ModelPart`는 pitch(X) → yaw(Y) → roll(Z) 고정. 6가지 회전 순서 지원 없음.
  - **scale**: `ModelPart`에 scaleX/Y/Z 런타임 필드 없음. Dilations는 생성 시 설정, 런타임 변경 불가.
  - **offset**: `ModelPart`에 offsetX/Y/Z 없음. pivotX/Y/Z(rotationPoint)만 있음.
  - **ignoreRender/ignoreBase**: `ModelPart.visible` 하나만 있음.
  - **ignoreSuperRotation**: MatrixStack에서 구현 가능하나 방식 다름. `stack.peek().getPositionMatrix()`에서 translation 성분(matrix[m03, m13, m23]) 추출 후 loadIdentity 후 재적용.
  - **fade 보간**: 없음. 직접 구현 필요.
  - **preTransforms 재귀**: `ModelPart`는 render(MatrixStack, VertexConsumer, ...) 호출 시 자동으로 pivotX/Y/Z translate → pitch/yaw/roll rotate → 자식 렌더 처리. 외부에서 재귀 필요 없음.
  - **GL11**: 1.21.1은 MatrixStack API 사용. `glPushMatrix → stack.push()`, `glPopMatrix → stack.pop()`, `glTranslatef → stack.translate()`, `glRotatef → stack.multiply(RotationAxis.POSITIVE_*.rotationDegrees())`, `glScalef → stack.scale()`.
- **포팅 주의사항**:
  - 회전 순서가 XYZ 이외인 파트(YZX, XZY 등)는 setAngles Mixin에서 MatrixStack을 직접 조작하거나, pivot 위치 이동 후 개별 축 회전을 수동 적용해야 함.
  - scaleY 변경(팔/다리 늘이기)은 해당 파트 렌더 전 `stack.scale(1, scaleY, 1)` 후 렌더 후 복원으로 구현 필요.
  - offsetY 보정(NoScaleEnd 타입)은 `pivotY += delta`로 근사.
  - fade 보간은 per-player RendererData 저장소를 직접 구현해야 함.

---

## SmartRenderModel 계층 구조 (bipedOuter/Torso/Breast/Neck/Pelvic/Shoulder)

- **원본 동작**: vanilla `ModelBiped.boxList.clear()` 후 15개 `ModelRotationRenderer` 노드로 재구성. 계층: `bipedOuter → bipedTorso → [bipedBody, bipedBreast → [bipedNeck → bipedHead → [bipedEars, bipedHeadwear], bipedCloak, bipedRightShoulder → bipedRightArm, bipedLeftShoulder → bipedLeftArm], bipedPelvic → [bipedRightLeg, bipedLeftLeg]]`. 중간 노드(Torso/Breast/Neck/Pelvic/Shoulder)가 그룹 회전을 담당.
  
  초기 pivot 값 (SmartRenderModel.java 생성자 직접 확인):
  - bipedOuter: (0, 0, 0), root, fadeEnabled=true
  - bipedTorso: (0, 0, 0)
  - bipedBody, bipedBreast, bipedNeck, bipedHead, bipedEars, bipedHeadwear: 모두 (0, 0, 0)
  - bipedCloak: (0, 0, 2F) — Z=2 (등 쪽)
  - bipedRightShoulder: (-5F, 2F, 0) / bipedLeftShoulder: (5F, 2F, 0) mirror=true
  - bipedRightArm, bipedLeftArm: (0, 0, 0) — Shoulder 기준 상대 좌표
  - bipedPelvic: (0, 12F, 0) — Y=12 (허리 아래)
  - bipedRightLeg: (-2F, 0, 0) / bipedLeftLeg: (2F, 0, 0) — Pelvic 기준
  
  초기 rotateAngle: 모두 0.0F (create() = ModelRotationRenderer 기본값; copy()는 rotateAngle 복사 안 함)
- **1.21.1 대응**: 없음 — 직접 구현 필요. `PlayerEntityModel`의 파트: head/hat/body/rightArm/leftArm/rightLeg/leftLeg + cloak/ear/sleeve/pants/jacket. 중간 노드 없음.
- **동작 차이**:
  - `bipedOuter` (전체 방향 루트, fadeRotateAngleY): 없음. setupTransforms의 `POSITIVE_Y.rotationDegrees(180 - bodyYaw)`가 유사한 역할.
  - `bipedTorso` (상체 그룹 X기울기): 없음. body + 모든 상체 파트 개별 조작으로 대체.
  - `bipedBreast` (가슴 그룹): 없음.
  - `bipedNeck` (머리 연결부): 없음.
  - `bipedPelvic` (골반 그룹): 없음.
  - `bipedRightShoulder`, `bipedLeftShoulder` (어깨): 없음.
- **포팅 주의사항**:
  - `bipedTorso.rotateAngleX += 0.5` (웅크리기 등) → `body.pitch += 0.5` 만으로는 부족. 상체 파트(head, rightArm, leftArm)도 개별 조작 필요.
  - `bipedOuter.rotateAngleY = forwardRotation` → setupTransforms @ModifyArg로 bodyYaw를 교체하는 방식.
  - `bipedOuter.rotateAngleX = (Quarter - verticalAngle)` → setupTransforms의 Branch 2가 일부 담당하지만, SM 커스텀 각도가 필요하면 추가 X rotate.
  - 어깨 파트(ignoreSuperRotation + workingAngle)는 Mixin에서 rightArm/leftArm을 대상으로 직접 구현.

---

## SmartRenderModel.setRotationAngles() 애니메이션 호출 순서

- **원본 동작**: `reset()` → 11개 animate 메서드를 조건부 순서로 호출:
  1. `animateHeadRotation` — 항상 (내부에서 SM 11가지 상태 분기 실행)
  2. `animateSleeping` — isSleeping
  3. `animateArmSwinging` — 항상
  4. `animateRiding` — mp.isRiding
  5. `animateLeftArmItemHolding` — mp.heldItemLeft != 0
  6. `animateRightArmItemHolding` — mp.heldItemRight != 0
  7. `animateWorkingBody` — mp.onGround > -9990F
  8. `animateWorkingArms` — mp.onGround > -9990F
  9. `animateSneaking` — mp.isSneak
  10. `animateArms` — 항상
  11. `animateBowAiming` — mp.aimedBow
- **1.21.1 대응**: `BipedEntityModel.setAngles()` (Yarn: `method_17087`) — 하나의 메서드 안에서 순서대로 처리.
- **동작 차이**:
  - 원본: 11개 분리 메서드, isStandard로 SM/vanilla 택일.
  - vanilla: 단일 메서드에서 순차 처리.
  - `mp.isRiding` → `model.riding` (= `entity.hasVehicle()`, `LivingEntityRenderer.render()` step 1에서 세팅).
  - `mp.heldItemLeft/Right != 0` → `entity.isUsingItem()` + Hand + ArmPose enum.
  - `mp.onGround > -9990F` → `entity.isUsingItem()` 조건 (positionRightArm/positionLeftArm).
  - `mp.isSneak` → `model.sneaking` (= `entity.isInSneakingPose()`, setModelPose에서 세팅).
  - `mp.aimedBow` → ArmPose.BOW_AND_ARROW 또는 ArmPose.CROSSBOW_HOLD.
- **포팅 주의사항**: ModelBiped 필드 직접 접근이 전부 제거됨. entity 상태에서 동등한 조건을 추출해야 함. SM Mixin은 setAngles @Inject(at=TAIL) 또는 @Inject(at=HEAD, cancellable=true)로 원본 처리 후/전 SM 로직 삽입.

---

## SM 11가지 이동 상태 → setAngles 매핑 (SmartMovingModel.setRotationAngles)

우선순위 순서 (if-else 체인):

| SM 상태 | 원본 조작 대상 | 1.21.1 대응 파트 | 비고 |
|---------|----------------|-----------------|------|
| `isRopeSliding` | bipedOuter Y, bipedTorso X, arm X/Z, leg X/Z | body.yaw 제거→setupTransforms, body pitch, arm/leg pitch/roll | |
| `isClimb` / `isCrawlClimb` | bipedOuter Y, arm X/Y, leg X/Z, 스케일 | setupTransforms Y + arm/leg pitch/yaw/roll | 회전 순서 YZX 필요 |
| `isClimbJump` | arm X/Z | rightArm.pitch, leftArm.pitch, arm.roll | |
| `isCeilingClimb` | bipedOuter Y, arm X/Y, leg X/Y | setupTransforms Y + arm/leg pitch/yaw | |
| `isSwim` | bipedOuter X/Y, head Y/X, body Y, arm X/Z, leg X/Z, 스케일 | setupTransforms X(Branch 2로 일부) + arm/leg 직접 | head 회전 순서 YXZ |
| `isDive` | bipedOuter X/Y, head X, leg Z, arm Z, 스케일 | setupTransforms X + arm/leg | |
| `isCrawl` | bipedTorso X/Z/Y, head Z/X, arm X/Z/Y, leg X/Z, 스케일 | body.pitch 등 + arm/leg | 회전 순서 YZX |
| `isSlide` | bipedOuter X/Y, body X/Y, arm X/Z/Y, leg X/Z | setupTransforms + body/arm/leg | offsetY 사용 |
| `isFlying` | bipedOuter X/Y, head X, arm Y/Z, leg X/Z | setupTransforms X + arm/leg | 회전 순서 XZY |
| `isHeadJump` | bipedOuter X/Y, arm X/Z, leg X/Z | setupTransforms + arm/leg | overGroundBlock 조건 |
| `isFalling` | arm Y/Z, leg X/Z | arm/leg pitch/yaw/roll | 회전 순서 XZY |
| else (isStandard) | SM 조작 없음, vanilla super 사용 | BipedEntityModel.setAngles 기본 결과 그대로 | |

---

## animateArmSwinging (기본 보행 팔/다리 애니메이션)

- **원본 동작**:
  ```java
  bipedRightArm.rotateAngleX = cos(totalHorizontalDistance * 0.6662F + Half) * 2.0F * currentHorizontalSpeed * 0.5F;
  bipedLeftArm.rotateAngleX  = cos(totalHorizontalDistance * 0.6662F)        * 2.0F * currentHorizontalSpeed * 0.5F;
  bipedRightLeg.rotateAngleX = cos(totalHorizontalDistance * 0.6662F)        * 1.4F * currentHorizontalSpeed;
  bipedLeftLeg.rotateAngleX  = cos(totalHorizontalDistance * 0.6662F + Half) * 1.4F * currentHorizontalSpeed;
  ```
- **1.21.1 대응**: BipedEntityModel.setAngles() Step 6-7 (Yarn: `method_17087`)
  ```java
  rightArm.pitch = MathHelper.cos(limbSwing * 0.6662f + PI) * 2.0f * limbSwingAmount * 0.5f / speedFactor;
  leftArm.pitch  = MathHelper.cos(limbSwing * 0.6662f)      * 2.0f * limbSwingAmount * 0.5f / speedFactor;
  rightLeg.pitch = MathHelper.cos(limbSwing * 0.6662f)      * 1.4f * limbSwingAmount / speedFactor;
  leftLeg.pitch  = MathHelper.cos(limbSwing * 0.6662f + PI) * 1.4f * limbSwingAmount / speedFactor;
  ```
- **동작 차이**:
  - `totalHorizontalDistance` → `limbSwing` (LimbAnimator.getPos, 개념 동일).
  - `currentHorizontalSpeed` → `limbSwingAmount` (LimbAnimator.getSpeed, 0~1).
  - `Half = π` = `(float)Math.PI` — 동일.
  - vanilla는 fallFlying 시 `speedFactor` (속도²/0.2의 세제곱) 추가 나눗셈.
  - 수치(0.6662, 2.0, 0.5, 1.4) 동일.
- **포팅 주의사항**: SM이 isStandard=false 상태이면 animateArmSwinging에서 vanilla super를 호출하지 않으므로 이 공식이 적용되지 않음. 1.21.1에서는 setAngles @Inject(TAIL)에서 SM 상태에 맞는 arm/leg pitch를 덮어쓰면 vanilla 공식 결과를 재정의 가능.

---

## animateSneaking (웅크리기 애니메이션)

- **원본 동작**:
  ```java
  bipedTorso.rotateAngleX  += 0.5F;       // 상체 기울기
  bipedRightLeg.rotateAngleX += -0.5F;    // 다리 역기울기
  bipedLeftLeg.rotateAngleX  += -0.5F;
  bipedRightArm.rotateAngleX += -0.1F;    // 팔 약간 앞으로
  bipedLeftArm.rotateAngleX  += -0.1F;
  bipedPelvic.offsetY  = -0.137F;         // 골반 Y 보정
  bipedPelvic.offsetZ  = -0.051F;
  bipedBreast.offsetY  = -0.014F;         // 가슴 Y 보정
  bipedBreast.offsetZ  = -0.057F;
  bipedNeck.offsetY    =  0.0621F;        // 목 Y 보정
  ```
- **1.21.1 대응**: BipedEntityModel.setAngles() Step 11 (model.sneaking=true 분기)
  ```java
  body.pitch     = 0.5f;
  rightArm.pitch += 0.4f;
  leftArm.pitch  += 0.4f;
  rightLeg.pivotZ = 4.0f;
  leftLeg.pivotZ  = 4.0f;
  rightLeg.pivotY = 12.2f;
  leftLeg.pivotY  = 12.2f;
  head.pivotY     = 4.2f;
  body.pivotY     = 3.2f;
  leftArm.pivotY  = 5.2f;
  rightArm.pivotY = 5.2f;
  ```
- **동작 차이**:
  - 원본: `bipedTorso` 중간 노드에 +0.5 → 상체 전체(head/arm) 그룹 회전. vanilla: `body.pitch = 0.5`만, 상체 그룹화 없음.
  - 팔 각도: 원본 -0.1, vanilla +0.4 (방향 반대, 크기 다름).
  - 위치 이동: 원본 offsetY/Z(중간 노드), vanilla pivotY/Z 절댓값 이동.
  - vanilla model.sneaking은 `entity.isInSneakingPose()`에서 자동 설정됨.
- **포팅 주의사항**:
  - SM이 CROUCHING 포즈를 사용하면 vanilla sneaking 변환이 자동 적용됨. SM 커스텀 포즈에서 다른 애니메이션이 필요하면 `model.sneaking = false` Mixin 오버라이드 후 직접 구현.
  - SM의 `isGenericSneaking`(이동 속도 감소 상태)용 별도 애니메이션이 필요하면 setAngles Mixin 직접 처리.

---

## animateRiding (탑승 애니메이션)

- **원본 동작**:
  ```java
  bipedRightArm.rotateAngleX += -0.6283185F;  // += -π/5
  bipedLeftArm.rotateAngleX  += -0.6283185F;
  bipedRightLeg.rotateAngleX = -1.256637F;     // -2π/5
  bipedLeftLeg.rotateAngleX  = -1.256637F;
  bipedRightLeg.rotateAngleY =  0.3141593F;    // π/10
  bipedLeftLeg.rotateAngleY  = -0.3141593F;
  ```
- **1.21.1 대응**: BipedEntityModel.setAngles() Step 8 (this.riding=true 분기)
  ```java
  rightArm.pitch += -0.62831855f;
  leftArm.pitch  += -0.62831855f;
  rightLeg.pitch  = -1.4137167f;   // -0.45π (원본 -2π/5 = -1.2566... 과 다름)
  rightLeg.yaw    =  0.31415927f;
  rightLeg.roll   =  0.07853982f;  // π/40 (원본에 없음)
  leftLeg.pitch   = -1.4137167f;
  leftLeg.yaw     = -0.31415927f;
  leftLeg.roll    = -0.07853982f;
  ```
- **동작 차이**:
  - 팔 += -π/5: 동일.
  - 다리 pitch: 원본 -1.2566(-2π/5), vanilla -1.4137(-0.45π). 값이 다름 (약 13° 차이).
  - 다리 yaw: 동일 (±π/10).
  - 다리 roll: 원본 없음, vanilla ±π/40 추가.
- **포팅 주의사항**: 수치 차이 있음. SM이 탑승 애니메이션을 vanilla에 위임하면 vanilla 수치 적용됨. SM 원본 수치가 필요하면 setAngles Mixin에서 다리 pitch/roll를 덮어써야 함.

---

## animateArms (항상 실행 — 호흡/미세 흔들림)

- **원본 동작**:
  ```java
  bipedRightArm.rotateAngleZ += cos(totalTime * 0.09F) * 0.05F + 0.05F;
  bipedLeftArm.rotateAngleZ  -= cos(totalTime * 0.09F) * 0.05F + 0.05F;
  bipedRightArm.rotateAngleX += sin(totalTime * 0.067F) * 0.05F;
  bipedLeftArm.rotateAngleX  -= sin(totalTime * 0.067F) * 0.05F;
  ```
  `totalTime` = `animationProgress` = `entity.age + tickDelta`
- **1.21.1 대응**: BipedEntityModel.animateArms (Yarn: `method_29353`) + CrossbowPosing.swingArm (Step 12). 내부 공식 미확인 (바이트코드 분석 범위 외).
- **동작 차이**: 미확인 — animateArms 내부 구현이 이번 리서치에서 분석되지 않음.
- **포팅 주의사항**: 미확인 (추가 리서치 필요). SM 비표준 상태에서는 animateArms를 호출하지 않으므로(isStandard=false, animateArms는 항상 호출이지만 SmartMovingModel에서는 isStandard=false이면 아무것도 안 함) 1.21.1에서 대응 동작이 필요하면 setAngles Mixin 직접 구현.

---

## animateWorkingBody / animateWorkingArms (도구 사용 애니메이션)

- **원본 동작**:
  ```java
  // animateWorkingBody:
  float angle = MathHelper.sin(MathHelper.sqrt_float(mp.onGround) * Whole) * 0.2F;
  bipedBreast.rotateAngleY = bipedBody.rotateAngleY += angle;
  bipedBreast.rotationOrder = bipedBody.rotationOrder = ModelRotationRenderer.YXZ;
  bipedLeftArm.rotateAngleX += angle;
  
  // animateWorkingArms:
  float f6 = 1.0F - (1.0F - mp.onGround)^3;  // ease-in cubic
  float f7 = sin(f6 * Half);
  float f8 = sin(mp.onGround * Half) * -(bipedHead.rotateAngleX - 0.7F) * 0.75F;
  bipedRightArm.rotateAngleX -= f7 * 1.2F + f8;
  bipedRightArm.rotateAngleY += sin(sqrt(mp.onGround) * Whole) * 0.4F;
  bipedRightArm.rotateAngleZ -= sin(mp.onGround * Half) * 0.4F;
  
  // animateNonStandardWorking (SM 비표준 상태):
  bipedRightShoulder.ignoreSuperRotation = true;
  bipedRightShoulder.rotateAngleX = viewVerticalAngelOffset / RadiantToAngle;
  bipedRightShoulder.rotateAngleY = workingAngle / RadiantToAngle;
  bipedRightShoulder.rotateAngleZ = Half;  // π/2
  bipedRightShoulder.rotationOrder = ZYX;
  bipedRightArm.reset();
  ```
- **1.21.1 대응**: BipedEntityModel.setAngles() Step 9 — `positionRightArm(entity)` / `positionLeftArm(entity)` (Yarn: `method_30154`, `method_30155`). 내부 구현: ArmPose 기반 팔 포즈 설정.
- **동작 차이**:
  - 원본: mp.onGround float (도구 사용 진행도) 직접 접근.
  - vanilla: `entity.isUsingItem()`, `entity.getActiveHand()`, ArmPose enum.
  - 원본: bipedRightShoulder(ignoreSuperRotation)로 WorldSpace 고정 어깨 회전. vanilla: 없음.
  - SM 비표준 상태에서 오른팔만 어깨 기준 회전 → 활 조준과 유사한 별도 처리.
- **포팅 주의사항**:
  - mp.onGround 동등 값: 미확인 (ModelBiped.onGround가 1.21.1에 없음 — 추가 리서치 필요).
  - SM 비표준 이동 중 도구 사용: ignoreSuperRotation 대신 MatrixStack에서 WorldSpace 위치 추출 후 팔 회전 적용.

---

## animateBowAiming (활 조준 애니메이션)

- **원본 동작**:
  ```java
  bipedRightArm.rotateAngleZ = 0.0F;
  bipedLeftArm.rotateAngleZ  = 0.0F;
  bipedRightArm.rotateAngleY = -0.1F + bipedHead.rotateAngleY - bipedOuter.rotateAngleY;
  bipedLeftArm.rotateAngleY  =  0.1F + bipedHead.rotateAngleY + 0.4F - bipedOuter.rotateAngleY;
  bipedRightArm.rotateAngleX = -π/2 + bipedHead.rotateAngleX;
  bipedLeftArm.rotateAngleX  = -π/2 + bipedHead.rotateAngleX;
  // + animateArms 미세흔들림 추가
  ```
  SM 비표준 상태(`animateNonStandardBowAiming`): 양쪽 어깨 ignoreSuperRotation 설정, vanilla superAnimateBowAiming 전후로 head/outer Y 임시 0으로 설정.
- **1.21.1 대응**: ArmPose.BOW_AND_ARROW → `positionRightArm()`/`positionLeftArm()` 내부. 내부 구현 미확인.
- **동작 차이**: 미확인 — positionRightArm/positionLeftArm 내부 로직은 이번 리서치 범위 외.
- **포팅 주의사항**: SM 비표준 상태에서 활 조준 시 ignoreSuperRotation 대신 MatrixStack 직접 조작으로 구현 필요.

---

## SmartRenderRender.rotatePlayer() → bipedOuter 방향 제어

- **원본 동작**: `actualRotation = 0`을 vanilla에 전달하여 vanilla 회전 억제. 모든 모델 레이어에 `actualRotation` / `forwardRotation` / `workingAngle` 세팅. `bipedOuter.rotateAngleY = actualRotation / RadiantToAngle`.
- **1.21.1 대응**: LivingEntityRenderer.render()에서 bodyYaw 보간 → setupTransforms()의 `POSITIVE_Y.rotationDegrees(180 - bodyYaw)`.
- **동작 차이**:
  - 원본: SM이 actualRotation=0을 vanilla에 전달하여 vanilla 회전 차단, bipedOuter에 직접 설정.
  - vanilla: entity.bodyYaw/prevBodyYaw를 `lerpAngleDegrees`로 보간한 값이 setupTransforms에 전달됨.
- **포팅 주의사항**: SM이 bodyYaw를 제어하려면:
  1. setupTransforms @ModifyArg(index=3)로 bodyYaw 값을 교체하거나,
  2. entity.bodyYaw를 tick Mixin에서 직접 설정.
  방법 1이 렌더에만 영향을 줘서 더 안전.

---

## SmartMovingRender.rotatePlayer() → renderYawOffset 강제 설정

- **원본 동작**: 특수 이동 상태(클라이밍/수영/슬라이딩 등)에서 `entityplayer.renderYawOffset = forwardRotation`. body가 이동 방향을 향하도록 강제.
- **1.21.1 대응**: entity.bodyYaw (Yarn: `AbstractClientPlayerEntity.bodyYaw`). LivingEntityRenderer.render()에서 `lerpAngleDegrees(tickDelta, prevBodyYaw, bodyYaw)`로 보간 후 setupTransforms에 전달.
- **동작 차이**: 원본은 renderYawOffset 직접 설정. vanilla는 entity.bodyYaw 보간값이 setupTransforms에 전달.
- **포팅 주의사항**: setupTransforms @ModifyArg(method=render, index=3)으로 bodyYaw를 SM 이동 방향 yaw로 교체하는 방식 권장.

---

## SmartRenderRender.renderPlayer() 이동 데이터 (SmartStatistics)

- **원본 동작**: SmartStatistics에서 `totalVerticalDistance`, `currentVerticalSpeed`, `totalDistance`, `currentSpeed`(renderPartialTicks 기반 보간). `SmartMovingRender`는 추가로 `currentHorizontalSpeedFlattened`. posX-prevPosX, posY-prevPosY, posZ-prevPosZ 직접 계산 → `horizontalDistance`, `verticalDistance`, `distance`, `currentCameraAngle`, `currentVerticalAngle`, `currentHorizontalAngle`.
- **1.21.1 대응**:
  - `limbSwing` (= totalHorizontalDistance): `entity.limbAnimator.getPos(tickDelta)` (Yarn: `LimbAnimator`, 필드 미확인)
  - `limbSwingAmount` (= currentHorizontalSpeed, 0~1): `entity.limbAnimator.getSpeed(tickDelta)`
  - `currentVerticalSpeed`, `totalVerticalDistance`, `totalDistance`, `currentSpeed`: 없음 — 직접 구현 필요
  - `horizontalDistance`, 이동 벡터: entity.getPos() - entity.prevX/Y/Z 직접 계산 필요 (미확인 — entity.prevX 등 1.21.1 field name 확인 필요)
  - `currentCameraAngle`: `entity.getYaw(tickDelta)` / RadiantToAngle
  - `currentVerticalAngle`: `atan(yDiff / hDist)` 직접 계산
  - `currentHorizontalAngle`: `atan(xDiff / zDiff)` 직접 계산
- **동작 차이**: SmartStatistics 전체 미존재. per-player 데이터 저장소와 보간 로직을 직접 구현해야 함.
- **포팅 주의사항**: SM Mixin에서 매 렌더 틱 entity.getLerpedPos(tickDelta)와 이전 위치 차이로 이동 벡터를 계산하는 데이터 저장소 클래스 필요. entity 위치는 Entity.lerpX/Y/Z (Yarn 미확인 — 추가 리서치 필요) 또는 Entity.getPos() vs prevPos로 계산.

---

## SmartRenderModel.animateWorkingBody() — rotationOrder YXZ 변경

- **원본 동작**: 도구 사용 중 `bipedBody.rotationOrder = ModelRotationRenderer.YXZ`. GL 호출 순서를 Y→X→Z로 변경.
- **1.21.1 대응**: ModelPart는 pitch(X) → yaw(Y) → roll(Z) 고정. rotationOrder 변경 불가.
- **동작 차이**: 회전 순서 변경 불가.
- **포팅 주의사항**: YXZ 순서가 필요한 경우 해당 파트 렌더 시 MatrixStack에서 수동으로 Y → X → Z 순서로 회전 후 geometry 렌더. 그러나 ModelPart.render() 자체가 내부적으로 XYZ를 적용하므로 angles를 0으로 두고 MatrixStack에서 직접 처리해야 함 — 복잡한 구현 필요.

---

## SmartMovingRender.renderPlayerAt() — heightOffset

- **원본 동작**: `EntityOtherPlayerMP`(타인)의 `moving.heightOffset != 0`이면 `d1 += heightOffset`.
- **1.21.1 대응**: `PlayerEntityRenderer.getPositionOffset()` (Yarn: `method_4076`). isInSneakingPose()이면 `Vec3d(0, -0.125 * entity.getScale(), 0)`, 그 외 ZERO. EntityRenderDispatcher.render()에서 world-space translate로 적용.
- **동작 차이**: 원본은 SM heightOffset 직접 조작. vanilla는 getPositionOffset() 반환값으로 world-space Y translate.
- **포팅 주의사항**: getPositionOffset() @Inject(at=TAIL)로 SM heightOffset을 Vec3d Y에 추가하는 방식 권장. 또는 render() Mixin에서 matrices.translate() 추가.

---

## SmartMovingRender.renderName() — 이름표 위치

- **원본 동작**: `isCrawling && !isClimbing` → isSneaking 임시 변경(`!_crawlNameTag.value`). `originalSneaking && !temporarySneaking` → d1 -= 0.05F. `heightOffset == -1` → d1 -= 0.2F.
- **1.21.1 대응**: EntityRenderer.renderLabelIfPresent (Yarn: `method_3951`). 이름표 Y위치는 entity.getHeight() + 0.5 기반. isInSneakingPose()이면 getHeight() 대신 0.5 고정(vanilla 기본).
- **동작 차이**: 원본은 entity.setSneaking() 임시 변경으로 이름표 위치 제어. vanilla는 getHeight()/isInSneakingPose() 기반.
- **포팅 주의사항**: renderLabelIfPresent Mixin에서 Y오프셋을 SM 상태에 맞게 직접 조정. entity 상태 임시 변경 대신 렌더 파라미터 조작.

---

## fade 보간 시스템 (ModelRotationRenderer.fadeIntermediate)

- **원본 동작**: `bipedOuter.previous` (RendererData — per-player 저장, SmartRenderRender.getPreviousRendererData()). `rotateAngleY` 등에 `prev + (current - prev) * timeDelta * 0.2F` 보간. totalTime - previousTotalTime > 2F이면 스킵. 2π 범위 최단 경로 각도 보간.
- **1.21.1 대응**: entity.prevBodyYaw → `lerpAngleDegrees(tickDelta, prevBodyYaw, bodyYaw)`. setupTransforms에서 Y회전에 반영.
- **동작 차이**:
  - vanilla: entity.prevBodyYaw/bodyYaw가 서버 tick에서 업데이트, 렌더에서 보간.
  - 원본: 렌더러가 직접 이전 프레임 값 저장 + 보간. per-player Map<EntityPlayer, RendererData>.
  - SM 커스텀 방향 전환 부드러움이 필요하면 entity.bodyYaw와 prevBodyYaw로 자동 처리되지만, 원본의 세밀한 0.2F 보간 계수 차이 있음.
- **포팅 주의사항**: entity.bodyYaw를 SM이 설정하면 vanilla lerpAngleDegrees 보간이 적용됨. 원본과 동일한 보간 계수(0.2F * timeDelta)가 필요하면 setupTransforms Mixin에서 별도 보간 로직 직접 구현.

---

## scaleY 시스템 (setArmScales / setLegScales)

- **원본 동작**: `SmartRenderModel.bipedRightArm.scaleY = value` — 팔/다리를 Y축으로 늘임. `NoScaleEnd` 타입: scaleY 대신 `offsetY -= (1 - scale) * 0.5F`. 주 바디(`Scale`), 갑옷흉갑(`NoScaleStart`/`NoScaleEnd`), 갑옷(`NoScaleStart`/`Scale`) 분리.
- **1.21.1 대응**: 없음 — ModelPart에 scaleY 런타임 필드 없음.
- **동작 차이**: ModelPart는 Dilations로 생성 시 크기 설정, 런타임 scaleY 변경 불가.
- **포팅 주의사항**: scaleY 효과가 필요한 경우:
  1. 해당 파트 렌더 전 `matrices.scale(1, scaleY, 1)` + 렌더 + 복원. FeatureRenderer나 모델 렌더 순서 고려 필요.
  2. 갑옷 레이어(ArmorFeatureRenderer)에서도 동일한 scale 보정 필요.
  3. 수영(isSwim), 다이브(isDive), 크롤(isCrawl), 클라이밍(isClimb) 상태에서만 사용됨.

---

## SmartMovingRender.renderGuiIngame() — HUD (소진 바, 점프 차지 바)

- **원본 동작**: GL11 + `drawTexturedModalRect()` → 화면 하단 좌/우에 아이콘 스프라이트 렌더.
- **1.21.1 대응**: `HudRenderCallback` (Fabric) + `DrawContext.drawTexture()`. 위치는 `client.getWindow().getScaledWidth/Height()`.
- **동작 차이**: GL11 → DrawContext API. ScaledResolution → Window. ingameGUI.drawTexturedModalRect → DrawContext.drawTexture(). GuiIngame 이벤트 훅 방식 다름.
- **포팅 주의사항**: Fabric의 `HudRenderCallback.EVENT.register()`로 HUD 렌더 등록. `DrawContext.drawTexture(Identifier, x, y, u, v, width, height, textureW, textureH)` 사용. 물속/갑옷 Y위치 보정 로직은 동일하게 구현 가능.

---

## 전체 포팅 난이도 요약

### 가장 어려운 부분

1. **ModelRotationRenderer 대체 (최고 난이도)**: 6가지 회전 순서, scaleY 런타임 변경, offsetX/Y/Z, ignoreBase/ignoreSuperRotation, fade 보간이 전부 vanilla에 없음. SM 애니메이션의 핵심인 비표준 회전 순서(수영=YXZ, 크롤=YZX, 비행=XZY 등)를 MatrixStack 수동 조작으로 구현해야 함.

2. **중간 노드 계층 없음 (높은 난이도)**: bipedOuter/Torso/Breast/Pelvic/Shoulder가 없어서 그룹 회전이 불가. 예: `bipedTorso.rotateAngleX += 0.5`는 체인된 모든 자식(head, neck, breast, shoulders, arms, pelvic, legs)에 영향을 줬지만, 1.21.1에서는 영향받을 파트를 일일이 열거해야 함.

3. **SmartStatistics 이동 데이터 (중간 난이도)**: per-player 렌더 틱 이동 벡터 추적, currentVerticalSpeed/totalDistance 등. 직접 구현하되, entity.getPos()와 prevX/Y/Z로 계산 가능.

4. **bodyYaw 제어 (중간 난이도)**: 원본의 renderYawOffset 직접 설정을 setupTransforms @ModifyArg로 대체. Mixin 포인트가 복잡할 수 있음.

### 권장 구현 순서

1. **SM 상태 플래그 시스템 구현**: SmartMovingRender의 이동 상태 → 렌더 플래그 변환 로직 (SmartMovingRender.renderPlayer() 해당 부분).

2. **SmartStatistics 등가 클래스 구현**: 이동 벡터, 속도, 각도 추적.

3. **setupTransforms Mixin**: bodyYaw 교체(SM 이동 방향), leaningPitch 제어(SM 이동 상태에 맞게 0 고정 또는 커스텀 각도).

4. **setAngles Mixin (@Inject TAIL)**: SM 11가지 이동 상태별 arm/leg pitch/yaw/roll 직접 설정. 이 단계에서 bipedOuter 역할 제외(setupTransforms에서 처리).

5. **scaleY 대체**: 해당 파트 렌더 전후 MatrixStack.scale() 삽입.

6. **HUD 구현**: Fabric HudRenderCallback.

7. **이름표/heightOffset**: Mixin으로 getPositionOffset() 오버라이드.
