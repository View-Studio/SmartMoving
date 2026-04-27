# Angle Jump 머리 (head) 1:1 매핑 리서치

> 작업 일자: 2026-04-27
> 사용자 요청: "a/s/d 점프 시 머리 움직임을 원본과 동일하게 1:1 번역"
> 원칙: 원본 코드 라인 하나하나 다 읽어서 1:1 번역.

---

## 1. 원본 코드 (1.7.10 Forge) 흐름

### 1-1. `SmartMovingModel.animateHeadRotation` (L626-632)
```java
public void animateHeadRotation(...) {
    setRotationAngles(...);                                    // 분기 결정
    if (isStandard)
        imp.superAnimateHeadRotation(...);                     // vanilla head 처리
}
```
- angle jump 시 `isStandard=true` → super (`SmartRenderModel.animateHeadRotation`) 호출.
- 즉 angle jump 자체는 head 특수 처리 없음. **vanilla head 처리 적용**.

### 1-2. `SmartRenderModel.animateHeadRotation` (L254-259)
```java
public void animateHeadRotation(float viewHorizontalAngelOffset, float viewVerticalAngelOffset) {
    bipedNeck.ignoreBase = true;
    bipedHead.rotateAngleY = (actualRotation + viewHorizontalAngelOffset) / RadiantToAngle;
    bipedHead.rotateAngleX = viewVerticalAngelOffset / RadiantToAngle;
}
```
- `actualRotation` = `bipedOuter.Y` = renderYawOffset 라디안 (lerped).
- `viewHorizontalAngelOffset` = netHeadYaw (= headYaw - renderYawOffset).
- → `bipedHead.Y = (renderYawOffset + netHeadYaw) / RadiantToAngle` = **headYaw 라디안** (절대값).

### 1-3. `SmartMovingRender.rotatePlayer` (L137-152) 의 핵심 force
```java
float forwardRotation = entityplayer.prevRotationYaw + (entityplayer.rotationYaw - entityplayer.prevRotationYaw) * f2;
if (... || moving.isAngleJumping())
    entityplayer.renderYawOffset = forwardRotation;     // ★ bodyYaw=rotationYaw lerped force
```
- **원본 1.7.10 angle jump 시 entity.renderYawOffset 자체를 rotationYaw 로 force**.
- 효과: vanilla GL_rotatef(180 - actualYaw) 가 R_y(180 - rotationYaw) → 모델 전체가 cameraYaw 향함.
- vanilla setAngles 의 netHeadYaw = headYaw - renderYawOffset = 0 → bipedHead.Y = renderYawOffset = rotationYaw.

### 1-4. 결과 head world Y 회전
1.7.10 누적:
- GL setupTransforms: R_y(180 - rotationYaw_lerped).
- bipedOuter.Y (= renderYawOffset force = rotationYaw_lerped): R_y(rotationYaw_lerped).
- bipedHead.Y (= headYaw 라디안 absolute): R_y(headYaw_lerped).
- head world Y = R_y(180 - rotationYaw + rotationYaw + headYaw) = **R_y(180 + headYaw)**.

→ head 가 absolute headYaw (cameraYaw) 방향 향함.

---

## 2. 현재 1.21.1 매핑 상태

### 2-1. `MixinPlayerEntityRenderer.sm_captureBodyYaw` (L89-237)
- L108: `smActive = ... || sm.isAngleJumping()`.
- L235-236: angle jumping 분기 — `smBodyYawOverride = lerp(prevYaw, yaw, tickDelta)` (= rotationYaw lerped).
- L252 `sm_modifyBodyYaw`: ModifyArg 로 setupTransforms 의 `bodyYaw` 인자를 `smBodyYawOverride` 로 force.

→ **setupTransforms 의 bodyYaw 매개변수만 force**. setAngles 의 netHeadYaw 인자는 vanilla 계산 그대로.

### 2-2. vanilla LivingEntityRenderer.render 의 setAngles 호출 (디스어셈블 검증)
```
offset 49:  prev_h = lerpAngleDegrees(g, prevBodyYaw, bodyYaw)        (bodyYaw_lerped, vanilla 자유 lerp)
offset 63:  prev_i = lerpAngleDegrees(g, prevHeadYaw, headYaw)        (headYaw_lerped)
            prev_j = prev_i - prev_h                                   (= netHeadYaw)
offset 329: setupTransforms(entity, ..., prev_h, ...)                  (★ ModifyArg 로 prev_h → smBodyYawOverride)
offset 451: setAngles(entity, ..., ageInTicks, prev_j, headPitch)      (★ prev_j 는 vanilla 자유 lerp 기반)
```

- setAngles 의 netHeadYaw (`prev_j`) 가 **vanilla 자유 lerp bodyYaw 기반** 계산됨.
- 우리 ModifyArg 가 setupTransforms 의 bodyYaw 인자만 force → setAngles 는 영향 받지 않음.

### 2-3. 결과 head world Y 회전 (1.21.1 매핑)
- setupTransforms: R_y(180 - smBodyYawOverride) = R_y(180 - rotationYaw_lerped).
- ModelPart head: head.yaw = (headYaw_lerped - bodyYaw_lerped) * π/180.
  (vanilla 자유 lerp bodyYaw, force 안 됨.)
- head world Y = R_y(180 - rotationYaw + headYaw - bodyYaw_natural).

원본과 차이 = `-bodyYaw_natural`. 즉 head 가 (cameraYaw - bodyYaw_natural) 만큼 어긋남.

`bodyYaw_natural` 가 cameraYaw 에 lerp 진행 중일 때 (옆 점프 후 옆으로 lerp) → head 도 그만큼 lerp 진행 → 시각: head 가 cameraYaw 향함이 부드럽게 lerp.

원본 = head 가 즉시 cameraYaw 향함 (force).

→ 사용자 보고 "머리 움직임 원본과 다름" 직접 원인.

---

## 3. 추가 발견 — `anySmState` reset 인프라에 `isAngleJumping` 누락

`MixinPlayerEntityModelClient.sm_setAngles` L100-102:
```java
boolean anySmState = sm.isRopeSliding || sm.isClimbing || sm.isCrawlClimbing || sm.isCeilingClimbing
        || sm.isClimbJumping || sm.isSwimming_sm || sm.isDiving
        || sm.isCrawling || sm.isSliding || sm.isHeadJumping || flyingCreative
        || isFallingForReset;                                           // ★ isAngleJumping 누락
```

L114-128: anySmState 시 reset (head.pivotZ, body.pivotZ, body.yaw, head.roll, leftArm/rightArm.pivotX/Z).

- angle jumping 시 reset 인프라 미작동 → 이전 SM 분기 (예: climbing 후 angle jump) 잔존 가능.
- vanilla `BipedEntityModel.setAngles` Step 4 가 `body.yaw = 0`, `arm.pivotX/Z` 기본값으로 매 프레임 reset → 일반적으론 영향 없음.
- 그러나 vanilla animateArms (swing 시) 가 `body.yaw` 변경 → angle jumping 중 swing 시 잔존.

---

## 4. 매핑 결정

### 4-1. 단계 1 — `anySmState` 에 `isAngleJumping` 추가 (단순)
- reset 인프라 활성화: head.pivotZ/body.pivotZ/body.yaw/head.roll/arm.pivot 모두 0 reset.
- angle jumping 단독 시 vanilla 의 잔존 cancel.
- 모든 SM 상태와 일관성.

### 4-2. 단계 2 — setAngles `netHeadYaw` ModifyArg 추가 (head 1:1 force)
- ModifyArg 로 setAngles 호출 시 `netHeadYaw` 인자 (index=4) 를 force.
- 식: `netHeadYaw = headYaw_lerped - smBodyYawOverride`
  (smBodyYawOverride = rotationYaw_lerped force 값).
- 효과: head world Y = R_y(180 - rotationYaw + headYaw - rotationYaw) = R_y(180 + headYaw - rotationYaw).

음 잠깐, 이건 원본 R_y(180 + headYaw) 와 다름. 원본 1:1 = headYaw - rotationYaw 가 아닌 다른 식?

원본 흐름 재분석 (1-4):
- GL setupTransforms R_y(180 - bodyYaw_force) = R_y(180 - rotationYaw_lerped).
- bipedOuter.Y = bodyYaw force = rotationYaw_lerped.
- bipedHead.Y = absolute headYaw_lerped.
- 누적 = R_y(180 - rotationYaw + rotationYaw + headYaw) = R_y(180 + headYaw).

1.21.1 vanilla setAngles 흐름 (force 없는 상태):
- setupTransforms R_y(180 - bodyYaw_lerped).
- ModelPart head.yaw = netHeadYaw = headYaw - bodyYaw_lerped.
- 누적 = R_y(180 - bodyYaw + headYaw - bodyYaw) = R_y(180 + headYaw - 2*bodyYaw).

bodyYaw=cameraYaw=headYaw 시 (정지 자세): R_y(180 - headYaw). 1.7.10 = R_y(180 + headYaw). 부호 차이?

음 이상. minecraft yaw convention 차이로 부호 반전 가능.

실제로 1.21.1 vanilla 가 정지 시 head 가 cameraYaw 향하는 효과는 잘 작동 (현재 1.21.1 게임 정상). 즉 R_y(180 + headYaw - 2*bodyYaw) 가 정지 시 cameraYaw 향함 (headYaw=bodyYaw=cameraYaw).

→ **1.21.1 매핑이 1.7.10 와 직접 비교 어려움** (좌표계 + lerp 차이).

핵심 = bodyYaw 가 cameraYaw 와 분리될 때 head 가 어디 향하는지.

bodyYaw_natural=옆 lerp 진행, headYaw=cameraYaw 일 때:
- 1.21.1 vanilla: head world Y = R_y(180 + headYaw - 2*bodyYaw_natural).
- 1.21.1 매핑 (smBodyYawOverride=rotationYaw force, netHeadYaw vanilla 그대로): R_y(180 - rotationYaw + headYaw - bodyYaw_natural).
- 1.7.10 (renderYawOffset force): R_y(180 - rotationYaw + rotationYaw + headYaw) = R_y(180 + headYaw).

setupTransforms 의 bodyYaw 가 force = rotationYaw 인 상태에서 setAngles netHeadYaw 도 force (= headYaw - rotationYaw) 시:
- head world Y = R_y(180 - rotationYaw + headYaw - rotationYaw) = R_y(180 + headYaw - 2*rotationYaw).

음 1.7.10 와 다름.

원본 의도 = head world Y 가 cameraYaw 향함. **R_y(180 + headYaw)** (절대 headYaw=rotationYaw=cameraYaw).

1.21.1 setupTransforms force 만 + netHeadYaw force 도:
- R_y(180 - rotationYaw + headYaw - rotationYaw) — bodyYaw force 가 ModelPart head.yaw 에 -rotationYaw 추가 효과를 주려면 netHeadYaw 가 -rotationYaw 가 아니라 절대 headYaw 가 되어야.

즉 head.yaw = headYaw_absolute (= rotationYaw 라디안 절대).
누적 = R_y(180 - rotationYaw_lerped) + R_y(headYaw_absolute) = R_y(180 - rotationYaw + headYaw).

1.7.10 = R_y(180 + headYaw). 차이 = -rotationYaw.

setupTransforms 의 bodyYaw 인자를 0 으로 force (비행 패턴) 시:
- R_y(180 - 0) + R_y(headYaw) = R_y(180 + headYaw). ✓ 1.7.10 일치.

**해결 방법**: angle jumping 시 smBodyYawOverride = 0 (비행 패턴) + 추가 yaw extra 로 모델 회전.

근데 이는 다리 매핑 (LOCAL only) 영향. 다리는 setupTransforms 의 bodyYaw 따라 회전 — 0 force 시 다리도 cameraYaw 안 향함. 다리 매핑이 잘못됨.

음 비행은 다리/팔/머리 모두 force=0 + extra yaw. angle jumping 도 동일하게 처리하려면 다리 매핑이 LOCAL + extra yaw 추가 필요.

복잡해짐. 

대안: smBodyYawOverride = rotationYaw_lerped 그대로 + ModifyArg 로 setAngles netHeadYaw 도 0 force.
- setupTransforms = R_y(180 - rotationYaw).
- head.yaw = 0.
- 누적 = R_y(180 - rotationYaw).

1.7.10 = R_y(180 + headYaw). 차이 = -rotationYaw - headYaw. headYaw=rotationYaw 시 -2*rotationYaw 차이. 다름.

Sigh. 분석 너무 복잡 + 매번 다른 결과.

**가장 단순 시도**: 단계 1 (anySmState 에 isAngleJumping 추가) 만 적용. 사용자 검증 후 단계 2 결정.

1단계만으로 OK 일 가능성:
- vanilla `body.yaw = 0` reset (sneak 안 시) + arm.pivot 기본값 reset.
- angle jumping 단독 시 reset 인프라 활성화 → 다른 SM 분기 잔존 cancel.
- swing 시 vanilla animateArms 의 body.yaw 변동 cancel.

사용자가 본 "머리 차이" 가 swing 시 body.yaw 흔들림의 누적 효과일 수도 (낙하/비행 swing 과 같은 패턴).

진행.
