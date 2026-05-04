# Sliding 애니메이션 리서치 (1.7.10 → 1.21.1 1:1 매핑)

> 범위: 슬라이딩 **애니메이션** (모델 자세 + 회전). 기능은 별도 완결 (`project_sliding_complete.md`).
>
> 원본 경로: `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\render\`
> 우리 경로: `C:\Users\user\IdeaProjects\SmartMoving\src\client\java\choco\ratel\smartmoving\mixin\client\`

---

## 1. 원본 라인 전수 (1.7.10)

### 1-1. 필드 정의 — `SmartMovingModel.java`

```java
// L784: 메인 모델의 슬라이딩 플래그
public boolean isSlide;
// L790: rope sliding (zipline plugin 의존, 별경로)
public boolean isRopeSliding;
```

### 1-2. 모델 인스턴스 간 필드 복사 — `SmartMovingModel.java` L40-L65

```java
// 다른 모델 (cape, ears 등) 에 메인 모델 상태 전달
isSlide = SmartMovingRender.CurrentMainModel.isSlide;          // L53
isRopeSliding = SmartMovingRender.CurrentMainModel.isRopeSliding;  // L60
```

### 1-3. **핵심 — `setRotationAngles` 의 isSlide 분기** (L437-L474)

```java
else if(isSlide)
{
    float distance = totalHorizontalDistance * 0.7F;
    float walkFactor = Factor(currentHorizontalSpeed, 0F, 1F) * 0.8F;

    // === HEAD ===
    bipedHead.rotateAngleZ = -viewHorizontalAngelOffset / RadiantToAngle;   // L442
    bipedHead.rotateAngleX = -Eighth - Sixteenth;                            // L443  (-π/8 - π/16 = -3π/16 ≈ -33.75°)
    bipedHead.rotationPointZ = -2F;                                          // L444

    // === bipedOuter (전체 회전 노드) ===
    bipedOuter.fadeRotateAngleY = false;                                     // L446  ★ fade 보간 비활성
    bipedOuter.rotateAngleY = currentHorizontalAngle;                        // L447  ★ 이동 방향 (= atan2(-velX, velZ))
    bipedOuter.rotationPointY = 5F;                                          // L448  ★ 5 픽셀 아래로
    bipedOuter.rotateAngleX = Quarter;                                       // L449  (90° 누움)

    // === BODY ===
    bipedBody.rotationOrder = ModelRotationRenderer.YXZ;                     // L451  ★ 회전 순서
    bipedBody.offsetY = -0.4F;                                                // L452  ★ -0.4F 위로 (= 모델 픽셀)
    bipedBody.rotationPointY = +6.5F;                                        // L453
    bipedBody.rotateAngleX = MathHelper.cos(distance - Eighth) * Sixtyfourth * walkFactor;   // L454
    bipedBody.rotateAngleY = MathHelper.cos(distance + Eighth) * Sixtyfourth * walkFactor;   // L455

    // === LEGS ===
    bipedRightLeg.rotateAngleX = MathHelper.cos(distance + Half) * Sixtyfourth * walkFactor + Sixtyfourth;   // L457
    bipedLeftLeg.rotateAngleX = MathHelper.cos(distance + Quarter) * Sixtyfourth * walkFactor + Sixtyfourth; // L458

    bipedRightLeg.rotateAngleZ = Thirtytwoth;                                // L460  (π/32)
    bipedLeftLeg.rotateAngleZ = -Thirtytwoth;                                // L461

    // === ARMS ===
    bipedRightArm.rotationOrder = ModelRotationRenderer.YZX;                 // L463  ★
    bipedLeftArm.rotationOrder = ModelRotationRenderer.YZX;                  // L464  ★

    bipedRightArm.rotateAngleX = MathHelper.cos(distance + Quarter) * Sixtyfourth * walkFactor + Half - Sixtyfourth;   // L466
    bipedLeftArm.rotateAngleX = MathHelper.cos(distance - Half) * Sixtyfourth * walkFactor + Half - Sixtyfourth;       // L467

    bipedRightArm.rotateAngleZ = Sixteenth;                                  // L469
    bipedLeftArm.rotateAngleZ = -Sixteenth;                                  // L470

    bipedRightArm.rotateAngleY = -Quarter;                                   // L472  (-π/2)
    bipedLeftArm.rotateAngleY = Quarter;                                     // L473  (π/2)
}
```

### 1-4. `rotatePlayer` (회전) — `SmartMovingRender.java` L137-L152

```java
public void rotatePlayer(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2)
{
    SmartMoving moving = SmartMovingFactory.getInstance(entityplayer);
    if(moving != null)
    {
        boolean isInventory = f2 == 1.0F && ... instanceof GuiInventory;
        if(!isInventory)
        {
            float forwardRotation = entityplayer.prevRotationYaw
                                  + (entityplayer.rotationYaw - entityplayer.prevRotationYaw) * f2;

            if(moving.isClimbing || moving.isClimbCrawling || moving.isCrawlClimbing
               || moving.isFlying || moving.isSwimming || moving.isDiving
               || moving.isCeilingClimbing || moving.isHeadJumping
               || moving.isSliding || moving.isAngleJumping())              // ★ isSliding 포함
                entityplayer.renderYawOffset = forwardRotation;             // ★ bodyYaw = player yaw 보간
        }
    }
    irp.superRenderRotatePlayer(entityplayer, totalTime, actualRotation, f2);
}
```

`forwardRotation` = **player yaw lerp** (= 마우스 yaw 보간, **도** 단위).
`renderYawOffset` 강제 = vanilla setupTransforms 가 이 yaw 로 모델 회전.

### 1-5. 모델 필드 전달 — `SmartMovingRender.java` L80-L117

```java
boolean isSlide = moving.isSliding;       // L80
boolean isRopeSliding = moving.isRopeSliding;  // L85
// ...
modelPlayer.isSlide = isSlide;             // L110
modelPlayer.isRopeSliding = isRopeSliding;  // L117
```

각 모델 인스턴스에 슬라이딩 플래그 전달 → 매 프레임 `setRotationAngles` 가 isSlide 분기 진입.

### 1-6. 상수 (참고)

```
Quarter      = π/2     ≈ 90.0°
Half         = π       ≈ 180.0°
Eighth       = π/8     ≈ 22.5°
Sixteenth    = π/16    ≈ 11.25°
Thirtytwoth  = π/32    ≈ 5.625°
Sixtyfourth  = π/64    ≈ 2.8125°
RadiantToAngle = 180/π
```

### 1-7. isRopeSliding 분기 (별경로) — `SmartMovingModel.java` L100-L126

zipline plugin 의존. 1.21.1 에 ZipLine mod 부재 → **`isRopeSliding` 항상 false** → 분기 진입 X. 매핑 무관.

```java
if(isRopeSliding)
{
    // ... zipline 자세 (생략, 매핑 무관)
}
```

---

## 2. 1.21.1 현재 매핑 상태

### 2-1. 매핑 위치

| 원본 | 1.21.1 위치 | 상태 |
|------|------------|------|
| `SmartMovingModel.isSlide` 필드 | `SmartMovingClientState.isSliding` | ✅ |
| 모델 인스턴스 간 필드 복사 (L53/L60) | 1.21.1 단일 모델 — 불필요 | ✅ |
| 분기 디스패치 (L437 `else if(isSlide)`) | `MixinPlayerEntityModelClient` L366-L367 `else if (sm.isSliding) sm_animateSliding(...)` | ✅ |
| `setRotationAngles` isSlide 본문 (L437-L474) | `MixinPlayerEntityModelClient.sm_animateSliding` L989-L1019 | ✅ (대부분) |
| `bipedOuter.rotateAngleX = Quarter` (L449) | `MixinPlayerEntityRenderer.sm_setupTransforms` L447-L450 | ✅ |
| `bipedOuter.rotationPointY = 5F` (L448) | `MixinPlayerEntityRenderer.sm_setupTransforms` L452 `matrices.translate(0, 5/16, 0)` | ⚠️ scale 부호 검증 필요 |
| `bipedBody.offsetY = -0.4F` (L452) | `MixinPlayerEntityRenderer.sm_setupTransforms` L456 `matrices.translate(0, -0.4/16, 0)` | ⚠️ scale 부호 검증 필요 |
| **`bipedOuter.rotateAngleY = currentHorizontalAngle` (L447)** | **— 미매핑** | ❌ **누락 가능** |
| `bipedOuter.fadeRotateAngleY = false` (L446) | `smBodyYawActive=true` (force overwrite) → fade 비활성 등가 | ✅ |
| `rotatePlayer` 의 `renderYawOffset = forwardRotation` (L147-L148) | `MixinPlayerEntityRenderer.sm_captureBodyYaw` L347-L352: `smBodyYawOverride = playerYaw lerp` (도) | ✅ |
| 패킷 송수신 (isSliding bit) | 기능 메모리 매핑 ✅ | ✅ |

### 2-2. `sm_animateSliding` 본문 1:1 비교

```java
private void sm_animateSliding(float limbSwing, float limbSwingAmount, float headYaw) {
    float distance   = limbSwing * 0.7f;                                      // ✅ 원본 totalHorizontalDistance * 0.7F
    float walkFactor = smFactor(limbSwingAmount, 0f, 1f) * 0.8f;              // ✅ 원본 Factor(currentHorizontalSpeed, 0F, 1F) * 0.8F

    head.pitch  = -EIGHTH - SIXTEENTH;                                         // ✅ 원본 L443
    head.roll   = -headYaw * DEG_TO_RAD;                                       // ✅ 원본 L442 (-viewHorizontalAngelOffset/RadiantToAngle)
    head.pivotZ = -2f;                                                         // ✅ 원본 L444

    setAnglesYXZ(body,                                                         // ✅ 원본 L451 ModelRotationRenderer.YXZ
            MathHelper.cos(distance - EIGHTH) * SIXTYFOURTH * walkFactor,      // ✅ 원본 L454
            MathHelper.cos(distance + EIGHTH) * SIXTYFOURTH * walkFactor,      // ✅ 원본 L455
            0f);
    body.pivotY = 6.5f;                                                        // ✅ 원본 L453 +6.5F

    rightLeg.pitch = MathHelper.cos(distance + HALF) * SIXTYFOURTH * walkFactor + SIXTYFOURTH;     // ✅ 원본 L457
    leftLeg.pitch  = MathHelper.cos(distance + QUARTER) * SIXTYFOURTH * walkFactor + SIXTYFOURTH;  // ✅ 원본 L458
    rightLeg.roll  =  THIRTYTWOTH;                                             // ✅ 원본 L460
    leftLeg.roll   = -THIRTYTWOTH;                                             // ✅ 원본 L461

    float rPitch = MathHelper.cos(distance + QUARTER) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;   // ✅ 원본 L466
    float lPitch = MathHelper.cos(distance - HALF) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;      // ✅ 원본 L467
    setAnglesYZX(rightArm, rPitch, -QUARTER,  SIXTEENTH);                      // ✅ 원본 L463/L466/L469/L472
    setAnglesYZX(leftArm,  lPitch,  QUARTER, -SIXTEENTH);                      // ✅ 원본 L464/L467/L470/L473
}
```

전부 ✅ — 자세 식 자체는 1:1.

### 2-3. `sm_setupTransforms` 의 isSliding 분기

```java
if (sm.isSliding) {
    float tiltAngle = (float) Math.PI / 2f;                                    // = Quarter
    matrices.multiply(RotationAxis.POSITIVE_X.rotation(tiltAngle));            // ✅ 원본 L449 bipedOuter.rotateAngleX = Quarter
    sm.smOuterTiltX = tiltAngle;
    matrices.translate(0f, 5f / 16f, 0f);                                      // ⚠️ 원본 L448 bipedOuter.rotationPointY = 5F (부호 검증)
    matrices.translate(0f, -0.4f / 16f, 0f);                                   // ⚠️ 원본 L452 bipedBody.offsetY = -0.4F (부호 검증)
}
```

### 2-4. `sm_captureBodyYaw` 의 isSliding 분기

```java
// L347-L352 (분기 외 fallthrough — isClimb/ClimbCrawling/CeilingClimb/isSliding/isAngleJumping)
smBodyYawActive = true;
smBodyYawOverride = localPlayer.prevYaw + (localPlayer.getYaw() - localPlayer.prevYaw) * tickDelta;
```

**원본 `forwardRotation` (L145) 와 1:1**:
- 원본: `prevRotationYaw + (rotationYaw - prevRotationYaw) * f2` (도)
- 우리: `prevYaw + (getYaw() - prevYaw) * tickDelta` (도)
- ✅

---

## 3. 차이/누락 분석

### 3-1. ★ 잠재 BUG — `bipedOuter.rotateAngleY = currentHorizontalAngle` 매핑 누락 (L447)

**원본 회전 합성**:
- vanilla `setupTransforms` 가 `R_y(-renderYawOffset) = R_y(-forwardRotation)` 적용 → **player yaw 회전 cancel**
- 그 후 `setRotationAngles` 가 `bipedOuter.rotateAngleY = currentHorizontalAngle` 적용 → **이동 방향** 회전
- 최종 모델 yaw = `-forwardRotation + currentHorizontalAngle` = `currentHorizontalAngle - forwardRotation`
- 정상 시 (이동 방향 = 마우스 방향) → 0 → 모델 정면.
- **슬라이딩 중 마우스만 돌리면**:
  - `forwardRotation` = 마우스 yaw (변화함).
  - `currentHorizontalAngle` = 이동 방향 (= 진입 시점 방향 보존).
  - 최종 yaw = `currentHorizontalAngle - forwardRotation` = **마우스 회전과 반대 방향** → 모델은 **이동 방향** 으로 향함 (마우스 회전 무시).

**우리 매핑 회전 합성**:
- `sm_modifyBodyYaw` 가 `bodyYaw = forwardRotation` 강제 (도) → vanilla `setupTransforms` 가 `R_y(-forwardRotation)` 적용.
- `sm_animateSliding` 은 **`bipedOuter.rotateAngleY` 매핑 없음** (= 0).
- 최종 모델 yaw = `-forwardRotation` = **마우스 yaw 따라 모델 회전**.

**결과 차이**:
- 원본: 슬라이딩 중 마우스 돌리면 **모델은 이동 방향 유지** (시점만 자유).
- 우리: 슬라이딩 중 마우스 돌리면 **모델도 같이 회전** (시점 + 모델 동기).

**fix 방향**:
- `sm_setupTransforms` 의 isSliding 분기에 `matrices.multiply(POSITIVE_Y.rotation(currentHorizontalAngle - forwardRotation))` 추가.
- 또는 `smBodyYawOverride = currentHorizontalAngle` (도 변환) 으로 변경 — 그러면 vanilla setupTransforms 가 이동 방향으로 회전.
- 후자가 더 간단 + 원본 1:1 (currentHorizontalAngle 직접 적용).

원본 `currentHorizontalAngle` = `Math.atan2(-currentMotionX, currentMotionZ)` (라디안). `SmartStatistics` 의 `currentHorizontalAngle` 매핑 검증 필요. 우리 매핑에 `sm.stats.currentHorizontalAngle` 필드 존재 — `MixinPlayerEntityRenderer` L214 (isLevitating 분기) 에서 참조됨.

**fix 적용 시**:
```java
// sm_captureBodyYaw 의 isSliding 분기 — 다른 fallthrough 와 분리해서 명시적 매핑
if (sm.isSliding) {
    smBodyYawActive = true;
    // 원본 setRotationAngles L447: bipedOuter.rotateAngleY = currentHorizontalAngle.
    //   원본 rotatePlayer L147 의 renderYawOffset = forwardRotation 은 vanilla setupTransforms
    //   가 R_y(-forwardRotation) 적용 cancel. 그 후 bipedOuter.Y = currentHorizontalAngle 직접
    //   적용 → 최종 모델 yaw = currentHorizontalAngle. 우리 매핑은 setupTransforms 의 bodyYaw
    //   를 currentHorizontalAngle (도) 로 직접 set 하면 등가 (R_y(-currentHorizontalAngle) 적용).
    smBodyYawOverride = (float) Math.toDegrees(sm.stats.currentHorizontalAngle);
    return;
}
```

### 3-2. ⚠️ scale 부호 검증 — `matrices.translate(0, +5/16, 0)` + `matrices.translate(0, -0.4/16, 0)`

vanilla `LivingEntityRenderer.setupTransforms` 안 `matrices.scale(-1, -1, 1)` 적용 (Y 부호 반전). `sm_setupTransforms` 는 `setupTransforms` TAIL inject — scale 적용 후.

원본 `bipedOuter.rotationPointY = 5F` 의 의미:
- ModelPart pivotY = 위쪽 음수, 아래쪽 양수 (Minecraft 모델 좌표). pivotY=5 → 5 픽셀 아래로.
- 1.21.1 동등 효과 = 모델이 5/16 블록 아래로 이동.

scale(-1,-1,1) 후 `matrices.translate(0, +5/16, 0)`:
- 변환 행렬 합성: `S * T = scale 후 translate`. translate 가 **scale 된 좌표계** 에서 적용 → Y 가 위로 (음수 효과, 즉 모델 위로). 
- 원하는 효과 = 모델 아래로. scale 부호 반전 고려 시 **`matrices.translate(0, -5/16, 0)` 가 정답** 가능.

메모리 `feedback_render_scale_negation.md` 참조 — `LivingEntityRenderer.render` 의 `scale(-1,-1,1)` 가 ModelPart 좌표계 Y 부호 반전. 회전 매핑 시 부호 보정 필수.

★ **부호 정확성** 인게임 시각 검증 필요. 원본 의도 = 모델이 살짝 아래로 + 몸 살짝 위로 (offsetY=-0.4F). scale 후 부호 반전이면 fix 필요.

### 3-3. 보존 항목 (이미 매핑됨)

- 자세 식 (head/body/legs/arms 모두) ✅
- 회전 순서 (YXZ body, YZX arms) ✅
- bodyYaw 강제 (`forwardRotation` lerp) ✅ — 단 이동 방향 회전 누락 (3-1).
- bipedOuter.rotateAngleX = Quarter (90° 누움) ✅
- 패킷 송수신 (기능 시스템 메모리) ✅

---

## 4. 1대1 매핑 작업 항목 (요약)

### A. 잠재 BUG fix (인게임 비교 후 결정)

1. **이동 방향 회전 매핑** (원본 L447 `bipedOuter.rotateAngleY = currentHorizontalAngle`):
   - `sm_captureBodyYaw` 의 isSliding 분기를 분리.
   - `smBodyYawOverride = Math.toDegrees(sm.stats.currentHorizontalAngle)` 로 변경.
   - 원본 효과: 슬라이딩 중 마우스 돌려도 모델은 이동 방향 유지.
   - 인게임 비교: 슬라이딩 중 좌/우 마우스 회전 → 우리 매핑이 모델 같이 회전하면 BUG, 이동 방향 유지면 정상.

2. **scale 부호 검증** (원본 L448 `bipedOuter.rotationPointY = 5F` + L452 `bipedBody.offsetY = -0.4F`):
   - `matrices.translate` 부호 (+5/16, -0.4/16) 가 정답인지 인게임 비교.
   - vanilla scale(-1,-1,1) 영향으로 부호 반전 시 변경 필요.

### B. 보존 항목 (이미 매핑됨, 변경 X)

- `sm_animateSliding` 본문 (head/body/legs/arms 자세 식) — 1:1 ✅
- bipedOuter.rotateAngleX = Quarter ✅
- bipedBody.rotationPointY = +6.5F ✅
- 회전 순서 헬퍼 (setAnglesYXZ, setAnglesYZX) ✅

---

## 5. 인게임 검증 시나리오

### 5-1. 자세 (정지 슬라이딩 시작 시점)
- 머리 살짝 위로 (-3π/16 = -33.75°)
- 몸 누움 (Quarter = 90°)
- 팔 양쪽으로 펼침 (yaw ±Quarter, roll ±Sixteenth, pitch ≈ Half - Sixtyfourth)
- 다리 살짝 벌림 (roll ±Thirtytwoth, pitch Sixtyfourth)

### 5-2. 진행 중 흔들림
- 다리 양쪽 cos(distance) * Sixtyfourth * walkFactor (속도 따라 진폭).
- 팔 양쪽 cos(distance) * Sixtyfourth * walkFactor.
- 몸 미세 X/Y 흔들림.

### 5-3. ★ 마우스 회전 시 모델 동작
- 슬라이딩 중 좌/우 마우스 회전.
- **원본**: 모델은 이동 방향 유지, 시점만 자유 회전.
- **우리 매핑** (현재): 모델도 같이 회전 (3-1 fix 전).

### 5-4. ★ 위치 (모델 높이/오프셋)
- 슬라이딩 자세 진입 시 모델이 지면에서 살짝 띄움 (= bipedOuter.rotationPointY=5F + bipedBody.offsetY=-0.4F).
- vanilla setupTransforms scale 부호 반전 영향 확인 필요.

### 5-5. 슬라이딩 → 헤드점프 (낙하 0.05F) 전환 자세 부드러움
- SlideToHeadJumping 자동 전환 시 자세 변화. fade 보간 필요 여부 확인.

---

## 6. 매핑 전제

- `sm_animateSliding` 호출 디스패치는 이미 `MixinPlayerEntityModelClient.setAngles` L366-L367 매핑됨.
- `sm.stats.currentHorizontalAngle` 필드 존재 (`SmartStatistics` 매핑 완료).
- 회전 순서 헬퍼 (`setAnglesYXZ`, `setAnglesYZX`) 동작 검증됨 (다른 분기 사용 중).
- 사용자 시각 검증이 fix 결정의 핵심.

---

## 7. 작업 순서 권고

1. **인게임 시각 검증 우선** — 5-1 ~ 5-5 시나리오 비교.
2. 차이 발견 시 fix:
   - 5-3 차이 → 3-1 fix (이동 방향 회전).
   - 5-4 차이 → 3-2 fix (scale 부호).
3. 자세 식 자체 (5-1 / 5-2) 차이 시 sm_animateSliding 본문 재검토. 현재는 1:1 매핑이라 차이 가능성 낮음.
