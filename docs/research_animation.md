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
currentVerticalAngle         — 수직 바라보기 각도 (headPitch를 라디안으로)
currentHorizontalAngle       — 수평 이동 방향 각도
currentCameraAngle           — 카메라 방향 각도
smallOverGroundHeight        — 머리 위 공간 높이
```

---

### 1. 기어가기 (isCrawl)

```java
float distance = totalHorizontalDistance * 1.3F; // 느린 애니메이션 (★1.3배 스케일!)
float walkFactor  = Factor(speed, 0F, 0.12951545F);
float standFactor = Factor(speed, 0.12951545F, 0F);

// 몸통 — 수평으로 눕힘 + Z축 진동 (★body.roll 있음)
bipedTorso.rotateAngleX = Quarter - Thirtytwoth;   // ~79°
bipedTorso.rotationPointY = 3F;
bipedTorso.rotateAngleZ = cos(distance + Quarter) * Sixtyfourth * walkFactor;

// 팔 — 앞으로 뻗음, Y축 외전 (★rightArm.Y=-Quarter, leftArm.Y=+Quarter)
bipedRightArm.rotateAngleX = Half + Eighth;
bipedLeftArm.rotateAngleX  = Half + Eighth;
bipedRightArm.rotateAngleY = -Quarter;    // ★ right=-Quarter
bipedLeftArm.rotateAngleY  =  Quarter;   // ★ left=+Quarter
// ★ roll 공식: (cos*Sixtyfourth ± Thirtytwoth) * walkFactor + Sixteenth * standFactor
bipedRightArm.rotateAngleZ = (cos(distance + Half) * Sixtyfourth + Thirtytwoth) * walkFactor + Sixteenth * standFactor;
bipedLeftArm.rotateAngleZ  = (cos(distance + Half) * Sixtyfourth - Thirtytwoth) * walkFactor - Sixteenth * standFactor;

// 다리 (★ right=cos(d-Quarter), left=cos(d-Half-Quarter) — 위상 다름)
bipedRightLeg.rotateAngleX = (cos(distance - Quarter) * Sixtyfourth + Thirtytwoth) * walkFactor + Thirtytwoth * standFactor;
bipedLeftLeg.rotateAngleX  = (cos(distance - Half - Quarter) * Sixtyfourth + Thirtytwoth) * walkFactor + Thirtytwoth * standFactor;
// ★ roll: right=(cos+1)*0.25*walkFactor+standFactor항, left=(cos-1)*0.25*walkFactor-standFactor항
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

**속도 구간 3개:** 정지 / 느린 수영(sneakFactor) / 빠른 수영(walkFactor)

```java
float walkFactor  = Factor(speed, 0.15679921F, 0.52264464F);
float sneakFactor = min(Factor(speed, 0, 0.15679921F), Factor(speed, 0.52264464F, 0.15679921F));
float standFactor = Factor(speed, 0.15679921F, 0F);
float combined    = standFactor + sneakFactor;  // ★ 정지+느린수영 통합

// 전체 몸통 앞으로 기울임 (★ 양수 값 — 앞으로 숙임)
bipedOuter.rotateAngleX = Quarter - Sixteenth * combined;   // →  body.pitch = QUARTER - SIXTEENTH * combined

// 머리
bipedHead.rotateAngleX = -Eighth * combined;

// ★ 팔 — Z축(roll)으로 주 동작, X축(pitch)은 연속 순환 (모듈로)
bipedRightArm.rotateAngleZ = Quarter + Eighth + cos(totalTime * 0.1F) * combined * 0.8F;
bipedLeftArm.rotateAngleZ  = -(Quarter + Eighth) - cos(totalTime * 0.1F) * combined * 0.8F;
bipedRightArm.rotateAngleX = ((distance * 0.5F) % Whole - Half) * walkFactor + Sixteenth * combined;
bipedLeftArm.rotateAngleX  = ((distance * 0.5F + Half) % Whole - Half) * walkFactor + Sixteenth * combined;

// 다리 — X축 플러터킥 (★ 진폭 0.52264464F)
bipedRightLeg.rotateAngleX = cos(distance) * 0.52264464F * walkFactor;
bipedLeftLeg.rotateAngleX  = cos(distance + Half) * 0.52264464F * walkFactor;
```

---

### 4. 잠수 (isDive)

**★ 팔/다리 모두 Z축(roll) 사용**

```java
float walkFactor  = Factor(speed, 0.15679921F, 0.52264464F);
float standFactor = Factor(speed, 0.15679921F, 0F);

// 몸통 — 수직 시선 방향 반영 (★ QUARTER - vAngle)
bipedOuter.rotateAngleX = Quarter - currentVerticalAngle;

// 다리 — Z축 돌핀킥
bipedRightLeg.rotateAngleZ = (cos(distance) + 1F) * 0.52264464F * walkFactor + Sixteenth * standFactor;
bipedLeftLeg.rotateAngleZ  = (cos(distance + Half) - 1F) * 0.52264464F * walkFactor - Sixteenth * standFactor;

// 팔 — Z축 스트로크 (★ 진폭 0.52264464F * 2.5F = 1.307)
bipedRightArm.rotateAngleZ = (cos(distance + Half) * 0.52264464F * 2.5F + Quarter) * walkFactor + (Quarter + Eighth) * standFactor;
bipedLeftArm.rotateAngleZ  = (cos(distance) * 0.52264464F * 2.5F - Quarter) * walkFactor - (Quarter + Eighth) * standFactor;
```

---

### 5. 슬라이딩 (isSlide)

```java
// 몸통 옆으로 눕힘
bipedOuter.rotateAngleX  = Quarter;
bipedOuter.rotationPointY = 5F;

// ★ 팔 — right=cos(d+Quarter), left=cos(d-Half) (위상 다름)
bipedRightArm.rotateAngleX = cos(distance + Quarter) * Sixtyfourth * walkFactor + Half - Sixtyfourth;
bipedLeftArm.rotateAngleX  = cos(distance - Half)    * Sixtyfourth * walkFactor + Half - Sixtyfourth;
// ★ arm.Y: right=-Quarter, left=+Quarter
bipedRightArm.rotateAngleY = -Quarter;
bipedLeftArm.rotateAngleY  =  Quarter;

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

// 몸통 + 팔 Y 회전 진동 (★ 팔이 몸통 회전을 상쇄)
float rotateY = cos(distance) * 0.44F * walkFactor;
bipedOuter.rotateAngleY = rotateY + horizontalAngle;
bipedRightArm.rotateAngleY = bipedLeftArm.rotateAngleY = -rotateY;  // ★ 팔 yaw = -rotateY
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

**★ 팔은 Z축(roll)이 주 축, X축(pitch)은 구부림**

```java
float vAngle = toRadians(currentVerticalAngle);

// 몸통 — 시선 방향 (★ QUARTER - vAngle)
bipedOuter.rotateAngleX = Quarter - currentVerticalAngle;
bipedHead.rotateAngleX  = -(Quarter - currentVerticalAngle) / 2F;  // ★ 머리 보상

// 벤딩 팩터 — 수직 각도 ±90° 기준
float bendFactor = min(Factor(vAngle, Quarter, 0), Factor(vAngle, -Quarter, 0));
// ★ 팔 X축(pitch) = -bendFactor * Eighth
bipedRightArm.rotateAngleX = -bendFactor * Eighth;
bipedLeftArm.rotateAngleX  = -bendFactor * Eighth;

// ★ 팔 Z축(roll) = 시선방향에 따른 팔 벌림 (right와 left 값 다름)
float armFactorZ = Factor(vAngle, Quarter, -Quarter);
bipedRightArm.rotateAngleZ =  Half - Sixteenth + armFactorZ * Eighth;
bipedLeftArm.rotateAngleZ  =  Sixteenth - Half - armFactorZ * Eighth;
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

## H. 우리 코드 vs 원본 비교 — 확인된 버그 목록

### [BUG-A] 기어가기 — 팔 Y축 방향 스왑 ★★★
- **원본:** `bipedRightArm.Y = -Quarter`, `bipedLeftArm.Y = +Quarter`
- **현재 오류:** `m.leftArm.yaw = -QUARTER`, `m.rightArm.yaw = +QUARTER` (좌우 반전)
- **수정:** `m.rightArm.yaw = -QUARTER`, `m.leftArm.yaw = QUARTER`

### [BUG-B] 기어가기 — body.roll 누락 ★★
- **원본:** `bipedTorso.rotateAngleZ = cos(d + Quarter) * Sixtyfourth * walkFactor`
- **현재 오류:** body.roll 미설정
- **수정:** `m.body.roll = cos(d + QUARTER) * SIXTYFOURTH * walkFactor` 추가

### [BUG-C] 기어가기 — 팔 roll 공식 오류 ★★★
- **원본 rightArm:** `(cos(d+Half)*Sixtyfourth + Thirtytwoth) * walkFactor + Sixteenth * standFactor`
- **원본 leftArm:** `(cos(d+Half)*Sixtyfourth - Thirtytwoth) * walkFactor - Sixteenth * standFactor`
- **현재 오류:** 그룹핑 누락, standFactor 미사용
- **수정:** 원본 공식 그대로 적용

### [BUG-D] 기어가기 — 1.3x distance 스케일 누락 ★★
- **원본:** `float d = totalHorizontalDistance * 1.3F` (느린 애니메이션)
- **현재 오류:** `dist` 그대로 사용 (애니메이션이 너무 빠름)
- **수정:** `float d = dist * 1.3F` 사용

### [BUG-E] 기어가기 — 다리 pitch 위상 스왑 ★★★
- **원본:** `rightLeg = cos(d-Quarter)`, `leftLeg = cos(d-Half-Quarter)`
- **현재 오류:** `leftLeg = cos(d-Quarter)`, `rightLeg = cos(d+Half-Quarter)` (좌우 스왑)
- **수정:** rightLeg와 leftLeg 위상 올바르게 할당

### [BUG-F] 기어가기 — 다리 roll 공식 오류 ★★
- **원본 rightLeg.roll:** `(cos(d-Quarter)+1)*0.25*walkFactor + Thirtytwoth*standFactor` (양수)
- **원본 leftLeg.roll:** `(cos(d-Quarter)-1)*0.25*walkFactor - Thirtytwoth*standFactor` (음수)
- **현재 오류:** 부호 반전, standFactor 미사용
- **수정:** 부호 및 공식 수정

### [BUG-G] 슬라이딩 — 팔 Y축 방향 스왑 ★★★
- **원본:** `bipedRightArm.Y = -Quarter`, `bipedLeftArm.Y = +Quarter`
- **현재 오류:** 좌우 반전 (기어가기와 동일한 버그)
- **수정:** `m.rightArm.yaw = -QUARTER`, `m.leftArm.yaw = QUARTER`

### [BUG-H] 슬라이딩 — leftArm pitch 위상 오류 ★★
- **원본:** `leftArm = cos(d - Half)`
- **현재 오류:** `leftArm = cos(d + Quarter)` (rightArm과 동일 위상)
- **수정:** `m.leftArm.pitch = cos(dist - HALF) * ...`

### [BUG-I] 천장 클라이밍 — 팔 yaw 누락 ★★
- **원본:** `bipedRightArm.Y = bipedLeftArm.Y = -rotateY` (몸통 회전 상쇄)
- **현재 오류:** 팔 yaw 미설정
- **수정:** `m.rightArm.yaw = m.leftArm.yaw = -rotateY` 추가

### [BUG-J] 수영 — 팔 주 축 오류 (pitch vs roll) ★★★
- **원본:** 팔 주 동작이 Z축(roll), 보조가 X축(pitch) 연속 순환
- **현재 오류:** 팔 pitch에 작은 cos 진동만 있음
- **수정:** `m.rightArm.roll = QUARTER + EIGHTH + cos(time*0.1) * combined * 0.8F`

### [BUG-K] 수영 — body pitch 부호 오류 ★★★
- **원본:** `bipedOuter.X = Quarter - Sixteenth * combined` (양수, 앞으로 기울임)
- **현재 오류:** `m.body.pitch = -QUARTER * walkFactor` (음수, 뒤로 기울임!)
- **수정:** `m.body.pitch = QUARTER - SIXTEENTH * combined`

### [BUG-L] 수영 — 다리 진폭 오류 ★★
- **원본:** 진폭 `0.52264464F`
- **현재 오류:** 진폭 `0.3F`
- **수정:** `0.52264464F`로 수정

### [BUG-M] 수영 — sneakFactor 누락 ★★
- **원본:** 3단계 속도 (standFactor + sneakFactor + walkFactor)
- **현재 오류:** 2단계 (standFactor + walkFactor)
- **수정:** `sneakFactor = min(factor(speed, 0, 0.15679921F), factor(speed, 0.52264464F, 0.15679921F))` 추가

### [BUG-N] 잠수 — 팔/다리 주 축 오류 (pitch vs roll) ★★★
- **원본:** 팔/다리 모두 Z축(roll) 사용, 큰 진폭 (arms: 0.52264464*2.5, legs: 0.52264464)
- **현재 오류:** 팔 pitch=Half(고정), 다리 pitch (작은 진폭 0.2F)
- **수정:** 팔/다리 모두 roll로 변경, 진폭 수정

### [BUG-O] 잠수 — body pitch 공식 오류 ★★★
- **원본:** `bipedOuter.X = Quarter - vAngle` (시선 방향에 따라 동적)
- **현재 오류:** `m.body.pitch = -Half` (고정값)
- **수정:** `m.body.pitch = QUARTER - vAngle`

### [BUG-P] 헤드점프 — body/head pitch 공식 오류 ★★★
- **원본:** `body.X = Quarter - vAngle`, `head.X = -(Quarter - vAngle)/2`
- **현재 오류:** `m.body.pitch = vAngle`, `m.head.pitch = -vAngle * 0.5F`
- **수정:** `QUARTER - vAngle` 공식 적용

### [BUG-Q] 헤드점프 — 팔 축 완전 오류 ★★★
- **원본:** `rightArm.Z = Half - Sixteenth + armFactorZ * Eighth` (roll 사용)
         `leftArm.Z  = Sixteenth - Half - armFactorZ * Eighth`
         `rightArm.X = leftArm.X = -bendFactor * Eighth` (pitch는 구부림만)
- **현재 오류:** pitch에 roll 값 대입, roll에 엉뚱한 값
- **수정:** 축 올바르게 할당

### [BUG-R] body.pivotY 초기화 누락 ★★
- **원본:** 각 상태 전환 시 pivotY 복원
- **현재 오류:** crawling(3F)/sliding(5F) 해제 후 pivotY가 초기화되지 않음
- **수정:** inject 시작 시 `model.body.pivotY = 0F` 리셋

---

## I. 1.21.1 마이그레이션 포인트

| 원본 | Fabric 1.21.1 대응 |
|------|-------------------|
| `SmartMovingModel.setRotationAngles()` | `@Mixin(PlayerEntityModel.class)` → `setAngles()` `@Inject(at = @At("TAIL"))` |
| `bipedOuter`, `bipedTorso`, `bipedPelvic` 등 커스텀 파트 | 없음. body/head로 근사 처리 |
| `ModelRotationRenderer.YZX` 회전 순서 | `MatrixStack` 수동 회전 순서 적용 (미구현) |
| `bipedOuter.rotateAngleX` | `model.body.pitch` 로 매핑 |
| `rotateAngleX` | `ModelPart.pitch` |
| `rotateAngleY` | `ModelPart.yaw` |
| `rotateAngleZ` | `ModelPart.roll` |
| `rotationPointY` | `ModelPart.pivotY` |
| `totalTime` | `animationProgress` (setAngles 파라미터) |
| `currentVerticalAngle` | `Math.toRadians(headPitch)` |
| `Factor()` 함수 | `AnimationUtil.factor()` |
| `MathHelper.cos()` | `Math.cos()` |
