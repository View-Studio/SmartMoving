# Crawl-Climbing (isCrawlClimb) — 원본 1:1 매핑 리서치

## 1. 원본 코드 (`SmartMovingModel.java` L239-L277)

```java
if(isCrawlClimb)
{
    float height = smallOverGroundHeight + 0.25F;
    float bodyLength = 0.7F;
    float legLength = 0.55F;

    float bodyAngleX, legAngleX, legAngleZ;
    if(height < bodyLength) {
        bodyAngleX = Math.max(0, (float)Math.acos(height / bodyLength));
        legAngleX = Quarter - bodyAngleX;
        legAngleZ = Thirtytwoth;
    } else if(height < bodyLength + legLength) {
        bodyAngleX = 0F;
        legAngleX = Math.max(0, (float)Math.acos((height - bodyLength) / legLength));
        legAngleZ = Thirtytwoth * (legAngleX / 1.537F);
    } else {
        bodyAngleX = 0F;
        legAngleX = 0F;
        legAngleZ = 0F;
    }

    bipedTorso.rotateAngleX = bodyAngleX;          // L265 — 부모
    bipedRightShoulder.rotateAngleX = -bodyAngleX; // L267 — arm 자식 cancel
    bipedLeftShoulder.rotateAngleX = -bodyAngleX;  // L268
    bipedHead.rotateAngleX = -bodyAngleX;          // L270 — head 자식 cancel
    bipedRightLeg.rotateAngleX = legAngleX;        // L272
    bipedLeftLeg.rotateAngleX = legAngleX;         // L273
    bipedRightLeg.rotateAngleZ = legAngleZ;        // L275
    bipedLeftLeg.rotateAngleZ = -legAngleZ;        // L276
}
```

## 2. 원본 모델 hierarchy (`SmartRenderModel.java`)

- **bipedTorso** (= 부모) → bipedBody, bipedHead, bipedRightShoulder/Arm, bipedPelvic/Leg, ... 모두 자식.
- bipedTorso.rotateAngleX = bodyAngleX → **모든 자식이 R_x(+bodyAngleX) 누적 회전**.

## 3. 원본 vertex 적용 결과

| 노드 | Local | Parent (bipedTorso) | World 결과 |
|------|-------|---------------------|------------|
| body | 0 | +bodyAngleX | **R_x(+bodyAngleX)** |
| head | -bodyAngleX | +bodyAngleX | **R_x(0) (= 무회전, mouse pitch 무시)** |
| arm  | rArmPitch (sm_animateClimbing 식) + shoulder(-bodyAngleX) | +bodyAngleX | **R_x(rArmPitch) (= 부모 + shoulder cancel)** |
| leg  | legAngleX | +bodyAngleX | **R_x(bodyAngleX + legAngleX)** |
| leg.roll | ±legAngleZ | 0 | ±legAngleZ |

## 4. 우리 매핑 (`MixinPlayerEntityModelClient.java` L642-L673)

```java
if (sm.isCrawlClimbing) {
    // ... height/bodyLength/legLength 식 1:1 OK ...
    body.pitch     =  bodyAngleX;
    head.pitch     = -bodyAngleX;          // ⚠ 원본 무회전 != 우리 -bodyAngleX
    rightLeg.pitch =  legAngleX;           // ⚠ 원본 +bodyAngleX 누락
    leftLeg.pitch  =  legAngleX;
    rightLeg.roll  =  legAngleZ;
    leftLeg.roll   = -legAngleZ;
    rightArm.pitch += -bodyAngleX;          // ⚠ 원본 부모 cancel 결과 != 우리 추가 -bodyAngleX
    leftArm.pitch  += -bodyAngleX;
}
```

vanilla 1.21.1 PlayerEntityModel = **단일 노드** (= bipedTorso 부재). 우리 매핑이 원본 부모 효과 누락.

## 5. 차이 정리

| 노드 | 원본 결과 | 우리 결과 | 차이 |
|------|----------|----------|------|
| body | R_x(+bodyAngleX) | R_x(+bodyAngleX) | OK |
| head | R_x(0) | R_x(-bodyAngleX) | **head 가 너무 아래로** |
| arm  | R_x(rArmPitch) | R_x(rArmPitch - bodyAngleX) | **arm 이 너무 아래로** |
| leg  | R_x(bodyAngleX + legAngleX) | R_x(legAngleX) | **leg 가 부족 회전 (음수면 뒤로 회전 → 모델 위로 들림)** |
| leg.roll | ±legAngleZ | ±legAngleZ | OK |

## 6. 사용자 보고 "모델 위로 올라오는 느낌"

- height < 0.7 (= 좁은 천장) 시 `legAngleX = π/4 - bodyAngleX < 0` (= 음수).
- 우리 매핑 leg.pitch = legAngleX (음수) → **leg 가 뒤로 회전** → 발이 위로 들림 → 모델 위로 올라옴 효과.
- 원본 = leg.pitch = bodyAngleX + legAngleX = π/4 (= 45° 양수) → leg 앞으로 정상 회전.

= **원인 직접 확인**.

## 7. fix 방향 (옵션 B — setAngles 만 매핑)

원본 부모 효과를 vanilla 단일 노드 매핑에 직접 합산:

```java
// 변경:
head.pitch     = 0f;                            // 부모 + cancel = 무회전
rightLeg.pitch = bodyAngleX + legAngleX;        // 부모 + leg local 합산
leftLeg.pitch  = bodyAngleX + legAngleX;
// 제거:
// rightArm.pitch += -bodyAngleX;               // 부모 cancel 결과 = vanilla swing
// leftArm.pitch  += -bodyAngleX;
// body.pitch / leg.roll 그대로 OK.
```

## 8. mouse pitch 처리

- 원본 isCrawlClimb 시 head.X = -bodyAngleX 로 덮어씀 = vanilla mouse pitch 무시.
- 우리 매핑 head.pitch = 0 = mouse pitch 무시 + 무회전. 동등.

## 9. 다른 노드 검토

원본 isCrawlClimb 분기는 isClimb 분기 (L127-L237) 안에 nested. isClimb 매핑 후 isCrawlClimb 추가 보정.

원본 isClimb 분기 안 다른 처리:
- L129 `bipedOuter.rotateAngleY = forwardRotation / RadToAngle` (= bodyYaw 매핑).
- L131-L132 head.Y/X.
- L134-L135 leg rotationOrder = YZX.
- L147-L221 hands/feet swing 식.
- L223-L237 leg roll + vine 분기.

이는 sm_animateClimbing (= 우리 매핑 L465+) 에서 매핑됨.

isCrawlClimb 추가 분기 (L239-L277) 만 fix 필요.
