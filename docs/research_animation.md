# 렌더링/애니메이션 시스템 리서치

> 원본 소스: `net.smart.moving.render.SmartMovingModel`, `SmartMovingRender`, `SmartRenderContext`

---

## 관련 클래스/파일

| 클래스 | 역할 |
|--------|------|
| `SmartMovingModel` | 핵심 애니메이션. `setRotationAngles()` 에서 11개 상태별 관절 각도 계산 |
| `SmartMovingRender` | 렌더 파이프라인 훅. 3개 모델 변형 동기화 |
| `SmartRenderContext` | 모델 파트 레퍼런스 + 상태 플래그 보유 |
| `ModelPlayer` | 바닐라 ModelBiped 확장 |
| `RenderPlayer` | 바닐라 RenderPlayer 확장 |

---

## 동작 원리

```
렌더 파이프라인 호출
  ↓
SmartMovingRender.renderPlayer()
  - SmartMoving 인스턴스에서 상태 플래그 18개 추출
  - 3개 모델(메인/흉갑/갑옷)에 동일 상태 동기화
  ↓
vanilla superRenderPlayer() 호출
  ↓
SmartMovingModel.setRotationAngles()
  - 상태 플래그 분기
  - Factor()로 부드러운 전환
  - MathHelper.cos()로 사지 진동
  - 관절 각도 직접 설정
```

---

## A. 핵심 유틸: Factor() 보간 함수

모든 애니메이션 전환에 사용되는 선형 보간 함수.

```java
private static float Factor(float x, float x0, float x1) {
    if (x0 > x1) {           // 내림 방향
        if (x <= x1) return 1F;
        if (x >= x0) return 0F;
        return (x0 - x) / (x0 - x1);
    } else {                  // 오름 방향
        if (x >= x1) return 1F;
        if (x <= x0) return 0F;
        return (x - x0) / (x1 - x0);
    }
}
```

**사용 예시:**
```java
// 속도 0.157~0.523 구간에서 0→1로 전환
float walkFactor  = Factor(currentHorizontalSpeed, 0.15679921F, 0.52264464F);
// 속도 0.157 미만이면 1, 이상이면 0
float standFactor = Factor(currentHorizontalSpeed, 0.15679921F, 0F);
```

---

## B. 각도 상수 (라디안 프리컴파일)

```java
static final float RadiantToAngle = (float)(180.0 / Math.PI);
static final float Whole    = (float)(2 * Math.PI);   // 360°
static final float Half     = (float)(Math.PI);       // 180°
static final float Quarter  = (float)(Math.PI / 2);   // 90°
static final float Eighth   = (float)(Math.PI / 4);   // 45°
static final float Sixteenth    = (float)(Math.PI / 8);   // 22.5°
static final float Thirtytwoth  = (float)(Math.PI / 16);  // 11.25°
static final float Sixtyfourth  = (float)(Math.PI / 32);  //  5.6°
```

---

## C. 모델 파트 구성 (12개)

바닐라(6개)에서 6개 추가.

| 파트 | 바닐라 여부 | 설명 |
|------|-----------|------|
| `bipedHead` | ✅ | 머리 |
| `bipedBody` | ✅ | 하체 |
| `bipedRightArm` | ✅ | 오른팔 |
| `bipedLeftArm` | ✅ | 왼팔 |
| `bipedRightLeg` | ✅ | 오른다리 |
| `bipedLeftLeg` | ✅ | 왼다리 |
| `bipedOuter` | ❌ | 루트 본 (전체 Y 회전 + 페이드) |
| `bipedTorso` | ❌ | 상체/흉부 |
| `bipedBreast` | ❌ | 가슴 레이어 |
| `bipedRightShoulder` | ❌ | 오른쪽 어깨 (팔과 독립) |
| `bipedLeftShoulder` | ❌ | 왼쪽 어깨 (팔과 독립) |
| `bipedPelvic` | ❌ | 골반 (몸통/다리 분리) |

**회전 순서 (RotationOrder):** 상태별로 `YZX`, `ZYX`, `XZY`, `YXZ` 중 선택. 적용 순서에 따라 최종 방향이 달라짐.

---

## D. 상태별 setRotationAngles() 완전 분석

### 공통 입력 값
```
currentHorizontalSpeed       — 수평 이동 속도
currentSpeed                 — 전체 속도
totalHorizontalDistance      — 누적 수평 이동 거리 (진동 위상)
totalDistance                — 누적 전체 이동 거리
totalTime                    — 누적 틱 수 (시간 기반 진동)
currentVerticalAngle         — 수직 바라보기 각도
currentHorizontalAngle       — 수평 이동 방향 각도
currentCameraAngle           — 카메라 방향 각도
smallOverGroundHeight        — 머리 위 공간 높이
```

---

### 1. 기어가기 (isCrawl)

```java
float distance = totalHorizontalDistance * 1.3F; // 느린 애니메이션
float walkFactor  = Factor(speed, 0F, 0.12951545F);
float standFactor = Factor(speed, 0.12951545F, 0F);

// 몸통 — 수평으로 눕힘
bipedTorso.rotateAngleX = Quarter - Thirtytwoth;   // ~79°
bipedTorso.rotationPointY = 3F;
bipedTorso.rotateAngleZ = cos(distance + Quarter) * Sixtyfourth * walkFactor;

// 팔 — 앞으로 뻗음, Y축 외전
bipedRightArm.rotateAngleX = Half + Eighth;  // ~270°
bipedLeftArm.rotateAngleX  = Half + Eighth;
bipedRightArm.rotateAngleY = -Quarter;
bipedLeftArm.rotateAngleY  = Quarter;
bipedRightArm.rotateAngleZ = (cos(distance + Half) * Sixtyfourth + Thirtytwoth) * walkFactor + Sixteenth * standFactor;
bipedLeftArm.rotateAngleZ  = (cos(distance + Half) * Sixtyfourth - Thirtytwoth) * walkFactor - Sixteenth * standFactor;

// 다리
bipedRightLeg.rotateAngleX = (cos(distance - Quarter) * Sixtyfourth + Thirtytwoth) * walkFactor + Thirtytwoth * standFactor;
bipedLeftLeg.rotateAngleX  = (cos(distance - Half - Quarter) * Sixtyfourth + Thirtytwoth) * walkFactor + Thirtytwoth * standFactor;
bipedRightLeg.rotateAngleZ = (cos(distance - Quarter) + 1F) * 0.25F * walkFactor + Thirtytwoth * standFactor;
bipedLeftLeg.rotateAngleZ  = (cos(distance - Quarter) - 1F) * 0.25F * walkFactor - Thirtytwoth * standFactor;
```

---

### 2. 클라이밍 (isClimb / isCrawlClimb)

**핵심:** 손/발 클라임 타입에 따라 파라미터가 달라지는 공식 기반 애니메이션.

```java
// 팔 — 거리 기반 진동 (FrequencyFactor = 0.6662F)
bipedRightArm.rotateAngleX = cos(vertDist * handsFreqUp + Half) * vSpeed * handsDistUp + handsDistUpOffset;
bipedLeftArm.rotateAngleX  = cos(vertDist * handsFreqUp)        * vSpeed * handsDistUp + handsDistUpOffset;
bipedRightArm.rotateAngleY = cos(horizDist * handsFreqSide + Quarter) * hSpeed * handsDistSide + handsDistSideOffset;
bipedLeftArm.rotateAngleY  = cos(horizDist * handsFreqSide)            * hSpeed * handsDistSide + handsDistSideOffset;

// 다리
bipedRightLeg.rotateAngleX = cos(vertDist * feetFreqUp)         * feetDistUp * vSpeed + feetDistUpOffset;
bipedLeftLeg.rotateAngleX  = cos(vertDist * feetFreqUp + Half)  * feetDistUp * vSpeed + feetDistUpOffset;
```

**덩굴 클라이밍 스케일링:**
```java
// 관절 각도에 따라 사지 길이 동적 스케일 (cos 절댓값)
setArmScales(abs(cos(bipedRightArm.rotateAngleX)), abs(cos(bipedLeftArm.rotateAngleX)));
setLegScales(abs(cos(bipedRightLeg.rotateAngleX)), abs(cos(bipedLeftLeg.rotateAngleX)));
```

**크롤-클라임 특수 케이스 (역삼각함수 사용):**
```java
float height = smallOverGroundHeight + 0.25F;
float bodyLength = 0.7F, legLength = 0.55F;
if (height < bodyLength) {
    // 머리 위 공간에 따라 기하학적으로 각도 계산
    bodyAngleX = Math.max(0, (float)Math.acos(height / bodyLength));
    legAngleX  = Quarter - bodyAngleX;
    legAngleZ  = Thirtytwoth;
}
bipedTorso.rotateAngleX        = bodyAngleX;
bipedRightShoulder.rotateAngleX = -bodyAngleX;
bipedLeftShoulder.rotateAngleX  = -bodyAngleX;
bipedHead.rotateAngleX         = -bodyAngleX;
bipedRightLeg.rotateAngleX     = legAngleX;
bipedLeftLeg.rotateAngleX      = legAngleX;
```

---

### 3. 수영 (isSwim)

**속도 구간 3개:** 정지 / 느린 수영 / 빠른 수영

```java
float walkFactor  = Factor(speed, 0.15679921F, 0.52264464F);
float sneakFactor = min(Factor(speed, 0, 0.15679921F), Factor(speed, 0.52264464F, 0.15679921F));
float standFactor = Factor(speed, 0.15679921F, 0F);

// 전체 몸통 앞으로 기울임
bipedOuter.rotateAngleX = Quarter - Sixteenth * (standFactor + sneakFactor);
bipedOuter.rotateAngleY = horizontalAngle;

// 머리 — 회전 순서 YXZ로 변경
bipedHead.rotationOrder = ModelRotationRenderer.YXZ;
bipedHead.rotateAngleY = cos(distance / 2F - Quarter) * walkFactor;
bipedHead.rotateAngleX = -Eighth * (standFactor + sneakFactor);

// 팔 — 시간 기반 진동 + 거리 기반 전진
bipedRightArm.rotateAngleZ = Quarter + Eighth + cos(totalTime * 0.1F) * (standFactor+sneakFactor) * 0.8F;
bipedLeftArm.rotateAngleZ  = -(Quarter + Eighth) - cos(totalTime * 0.1F) * (standFactor+sneakFactor) * 0.8F;
bipedRightArm.rotateAngleX = ((distance * 0.5F) % Whole - Half) * walkFactor + Sixteenth * (standFactor+sneakFactor);
bipedLeftArm.rotateAngleX  = ((distance * 0.5F + Half) % Whole - Half) * walkFactor + Sixteenth * (standFactor+sneakFactor);

// 다리 — 반진동 (Half 위상차)
bipedRightLeg.rotateAngleX = cos(distance) * 0.52264464F * walkFactor;
bipedLeftLeg.rotateAngleX  = cos(distance + Half) * 0.52264464F * walkFactor;
```

---

### 4. 잠수 (isDive)

```java
// 몸통 기울기 — 수직 시선 방향 반영
bipedOuter.rotateAngleX = isLevitate ? Quarter - Sixteenth
                        : isJump     ? 0F
                        :              Quarter - currentVerticalAngle;

// 다리 — Z축 진동 (킥 동작)
bipedRightLeg.rotateAngleZ = (cos(distance) + 1F) * 0.52264464F * walkFactor + Sixteenth * standFactor;
bipedLeftLeg.rotateAngleZ  = (cos(distance + Half) - 1F) * 0.52264464F * walkFactor - Sixteenth * standFactor;

// 팔 — Z축 진동 (스트로크), 진폭 2.5배
bipedRightArm.rotateAngleZ = (cos(distance + Half) * 0.52264464F * 2.5F + Quarter) * walkFactor + (Quarter + Eighth) * standFactor;
bipedLeftArm.rotateAngleZ  = (cos(distance) * 0.52264464F * 2.5F - Quarter) * walkFactor - (Quarter + Eighth) * standFactor;
```

---

### 5. 슬라이딩 (isSlide)

```java
// 몸통 옆으로 눕힘
bipedOuter.rotateAngleX  = Quarter;
bipedOuter.rotationPointY = 5F;
bipedBody.offsetY        = -0.4F;  // Y 오프셋 (웅크린 위치)
bipedBody.rotationPointY = +6.5F;

// 팔 — 앞으로 뻗음
bipedRightArm.rotateAngleX = cos(distance + Quarter) * Sixtyfourth * walkFactor + Half - Sixtyfourth;
bipedLeftArm.rotateAngleX  = cos(distance - Half)    * Sixtyfourth * walkFactor + Half - Sixtyfourth;
bipedRightArm.rotateAngleY = -Quarter;
bipedLeftArm.rotateAngleY  = Quarter;

// 다리
bipedRightLeg.rotateAngleZ = Thirtytwoth;
bipedLeftLeg.rotateAngleZ  = -Thirtytwoth;
```

---

### 6. 천장 클라이밍 (isCeilingClimb)

**속도 임계값:** 0.12951545F (정지/이동 전환)

```java
float walkFactor  = Factor(speed, 0F, 0.12951545F);
float standFactor = Factor(speed, 0.12951545F, 0F);

// 팔 — 위로 뻗어서 천장 잡기
bipedLeftArm.rotateAngleX  = (cos(distance) * 0.52F + Half) * walkFactor + Half * standFactor;
bipedRightArm.rotateAngleX = (cos(distance + Half) * 0.52F - Half) * walkFactor - Half * standFactor;

// 다리 — 뒤로 늘어짐
bipedLeftLeg.rotateAngleX  = -cos(distance) * 0.12F * walkFactor;
bipedRightLeg.rotateAngleX = -cos(distance + Half) * 0.32F * walkFactor;

// 몸통 Y 회전 진동
float rotateY = cos(distance) * 0.44F * walkFactor;
bipedOuter.rotateAngleY = rotateY + horizontalAngle;
bipedRightArm.rotateAngleY = bipedLeftArm.rotateAngleY = -rotateY;
```

---

### 7. 비행 (isFlying)

```java
float distance = totalDistance * 0.08F; // 매우 느린 진동
float time = totalTime * 0.15F;

// 몸통 — 이동 방향/속도로 기울기
bipedOuter.rotateAngleX = (Quarter - verticalAngle) * walkFactor;

// 팔 — 이동 시 활강 / 정지 시 시간 기반 흔들림
bipedRightArm.rotateAngleZ = (cos(distance + Half) * Sixtyfourth + (Half - Sixteenth)) * walkFactor + Quarter * standFactor;
bipedLeftArm.rotateAngleZ  = (cos(distance) * Sixtyfourth - (Half - Sixteenth)) * walkFactor - Quarter * standFactor;
bipedRightArm.rotateAngleY = cos(time) * Sixteenth * standFactor; // 정지 시 흔들림
bipedLeftArm.rotateAngleY  = cos(time) * Sixteenth * standFactor;
```

---

### 8. 헤드 점프 (isHeadJump)

```java
// 몸통 수직 방향으로 기울임
bipedOuter.rotateAngleX = Quarter - currentVerticalAngle;
bipedHead.rotateAngleX  = -bipedOuter.rotateAngleX / 2F;

// 벤딩 팩터 — 수직 각도 ±90° 기준
float bendFactor = min(Factor(vAngle, Quarter, 0), Factor(vAngle, -Quarter, 0));
bipedRightArm.rotateAngleX = bendFactor * -Eighth;
bipedLeftArm.rotateAngleX  = bendFactor * -Eighth;

// 팔 Z — 머리 위 고체 블록 있으면 제한
float armFactorZ = Factor(vAngle, Quarter, -Quarter);
if (overGroundBlock != null && overGroundBlock.getMaterial().isSolid())
    armFactorZ = min(armFactorZ, smallOverGroundHeight / 5F);
bipedRightArm.rotateAngleZ = Half - Sixteenth + armFactorZ * Eighth;
bipedLeftArm.rotateAngleZ  = Sixteenth - Half - armFactorZ * Eighth;
```

---

### 9. 낙하 (isFalling)

```java
float distance = totalDistance * 0.1F;

bipedRightArm.rotateAngleZ = cos(distance) * Eighth + Quarter;
bipedLeftArm.rotateAngleZ  = cos(distance) * Eighth - Quarter;
bipedRightArm.rotateAngleY = cos(distance + Quarter) * Eighth;
bipedLeftArm.rotateAngleY  = cos(distance + Quarter) * Eighth;

bipedRightLeg.rotateAngleX = cos(distance + Half + Quarter) * Sixteenth + Thirtytwoth;
bipedLeftLeg.rotateAngleX  = cos(distance + Quarter) * Sixteenth + Thirtytwoth;
bipedRightLeg.rotateAngleZ = cos(distance) * Sixteenth + Thirtytwoth;
bipedLeftLeg.rotateAngleZ  = cos(distance) * Sixteenth - Thirtytwoth;
```

---

### 10. 로프 슬라이딩 (isRopeSlide)

```java
float time = totalTime * 0.15F;

bipedOuter.rotateAngleY = currentHorizontalAngle;
bipedTorso.rotateAngleX = Sixteenth + Sixtyfourth * cos(time); // 미세 흔들림

// 팔 — 로프 잡기 자세
bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = Half - bipedTorso.rotateAngleX;
bipedRightArm.rotateAngleZ = Sixteenth + Thirtytwoth;
bipedLeftArm.rotateAngleZ  = -(Sixteenth + Thirtytwoth);
bipedRightArm.rotationPointY = bipedLeftArm.rotationPointY = -2F;

// 다리 — 4분의 1 위상차 진동
bipedRightLeg.rotateAngleX = Sixtyfourth * cos(time + Quarter);
bipedLeftLeg.rotateAngleX  = Sixtyfourth * cos(time - Quarter);
bipedRightLeg.rotateAngleZ = Thirtytwoth;
bipedLeftLeg.rotateAngleZ  = -Thirtytwoth;
```

---

### 11. 클라임 점프 (isClimbJump)

```java
// 정적 포즈 (진동 없음)
bipedRightArm.rotateAngleX = Half + Sixteenth;
bipedLeftArm.rotateAngleX  = Half + Sixteenth;
bipedRightArm.rotateAngleZ = -Thirtytwoth;
bipedLeftArm.rotateAngleZ  = Thirtytwoth;
```

---

## E. 스케일링 시스템 (3가지 전략)

| 타입 | 상수 | 동작 |
|------|------|------|
| `Scale` (0) | 메인 모델 | `scaleY` 직접 변경 |
| `NoScaleStart` (1) | 흉갑 갑옷 | 시작점 오프셋 (상단 고정) |
| `NoScaleEnd` (2) | 표준 갑옷 | 끝점 오프셋 (하단 고정) |

```java
private void setArmScales(float rightScale, float leftScale) {
    if (scaleArmType == Scale) {
        md.bipedRightArm.scaleY = rightScale;
        md.bipedLeftArm.scaleY  = leftScale;
    } else if (scaleArmType == NoScaleEnd) {
        md.bipedRightArm.offsetY -= (1F - rightScale) * 0.5F;
        md.bipedLeftArm.offsetY  -= (1F - leftScale)  * 0.5F;
    }
}
```

**갑옷 모델에 Scale 대신 Offset을 쓰는 이유:** 갑옷 메시는 플레이어 메시보다 약간 크게 만들어져 있어서 scaleY를 직접 쓰면 갑옷이 살 속으로 파고드는 현상 발생 → offsetY로 대체.

---

## F. 렌더 파이프라인 훅

### renderPlayer() — 상태 동기화
```java
// 상태 플래그 18개 추출 후 3개 모델에 동기화
for (IModelPlayer modelPlayer : irp.getPlayerModels()) {
    SmartMovingModel m = modelPlayer.getMovingModel();
    m.isClimb = isClimb;
    m.isSwim  = isSwim;
    // ...
    m.currentHorizontalSpeed      = currentHorizontalSpeed;
    m.smallOverGroundHeight       = smallOverGroundHeight;
}
```

### renderPlayerAt() — 높이 오프셋 보정
```java
// 크롤링 플레이어의 히트박스 Y 보정 (멀티플레이어)
if (moving != null && moving.heightOffset != 0)
    d1 += moving.heightOffset;
irp.superRenderRenderPlayerAt(entityplayer, d, d1, d2);
```

### rotatePlayer() — 몸통 회전 고정
```java
// 클라이밍/비행/수영 중 몸통을 이동 방향으로 고정
if (moving.isClimbing || moving.isFlying || moving.isSwimming ||
    moving.isCeilingClimbing || moving.isSliding)
    entityplayer.renderYawOffset = forwardRotation;
```

---

## G. HUD 렌더링

**텍스처:** `smartmoving:gui/icons.png` (9x9 픽셀 아이콘 그리드)

**Exhaustion 바:** 우측 하단 (방어구 바 아래)
- 아이콘 좌표: (0~2, 0) — 빈/반/꽉 하트 형태
- 수중일 때 10px 위로 이동 (물 레벨 인디케이터 회피)

**Jump Charge 바:** 좌측 하단 (방어구 바 아래)
- 차지 점프 / 헤드 점프 차지량 표시
- 최대 충전 시 별도 아이콘

---

## 1.21.1 마이그레이션 포인트

| 원본 | Fabric 1.21.1 대응 |
|------|-------------------|
| `SmartMovingModel.setRotationAngles()` | `@Mixin(PlayerEntityModel.class)` → `setAngles()` `@Inject(at = @At("TAIL"))` |
| `bipedOuter`, `bipedTorso`, `bipedPelvic` 등 커스텀 파트 | `ModelPart` 직접 추가 (PlayerEntityModel 확장 또는 Mixin으로 필드 주입) |
| `ModelRotationRenderer.YZX` 회전 순서 | `MatrixStack` 수동 회전 순서 적용 |
| 3개 모델 동기화 | `FeatureRenderer` 또는 `@Mixin(PlayerEntityRenderer)` |
| `renderGuiIngame()` (HUD) | `HudRenderCallback` (Fabric API) 또는 `@Mixin(InGameHud)` |
| `setArmScales()` scaleY | `ModelPart` 에 직접 scale 없음 → `MatrixStack.scale()` 사용 |
| `Factor()` 함수 | 그대로 유틸 클래스로 이식 (순수 Java 수학) |
| `MathHelper.cos()` | `Math.cos()` (LWJGL 제거됨) |

---

## 미확인 / 추가 조사 필요

- [ ] `ModelRotationRenderer` 클래스 구현 — 회전 순서(YZX 등) 어떻게 구현되어 있는지
- [ ] 1.21.1 `PlayerEntityModel`에서 커스텀 `ModelPart` 주입 방법 (Mixin accessor or duck interface)
- [ ] `bipedOuter.fadeRotateAngleX/Y` — 페이드 기능이 무엇인지 (`SmartRenderContext` 확인 필요)
- [ ] 갑옷 모델과 메인 모델 동기화를 Fabric에서 어떻게 할지 (FeatureRenderer vs Mixin)
- [ ] `MatrixStack.scale()` 이 `NoScaleStart` / `NoScaleEnd` 전략을 대체할 수 있는지 검증
- [ ] HUD 아이콘 텍스처 직접 그릴 경우 1.21.1의 `DrawContext` API 확인
