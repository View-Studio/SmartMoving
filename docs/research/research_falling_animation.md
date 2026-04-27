# Falling 애니메이션 1:1 이식 리서치

> 작업 일자: 2026-04-27
> 사용자 보고: "공중 낙하 애니메이션의 디테일이 원본과 다르다.
>   팔/다리 휘저음의 속도 및 가속도, fade 보간으로 인한 중간중간 움직임의 부드러움."
> 원칙: 원본 코드 라인 하나하나 읽고 1:1 번역.

---

## 1. 원본 코드 (1.7.10 Forge)

### 1-1. 진입 조건 — `SmartMovingSelf.doFallingAnimation` (L3278-3282)

```java
@Override
public boolean doFallingAnimation()
{
    if(Config.isFallAnimationEnabled())
        return !sp.onGround && sp.fallDistance > Config._fallAnimationDistanceMinimum.value;
    return false;
}
```

- `Config._fallAnimationDistanceMinimum` 기본값 **3F** (`SmartMovingConfig.java:223`).
  - `_fallingDistanceMinimum` 과 별도 config (전자: 애니메이션 트리거, 후자: 크롤 차단).
- 다른 SM 상태 (수영/물 등) 와 충돌 검사는 **하지 않음** — 단순히 `onGround=false && fallDistance>3F`.
  하지만 `SmartMovingRender.renderRender L78-L81` 의 분기 우선순위 (isJump → isHeadJump → isFalling → standard) 가 if-else 라 다른 SM 상태가 활성이면 falling 까지 도달 X.

### 1-2. 회전 분기 — `SmartMovingModel.setRotationAngles` (L531-549)

```java
else if(isFalling)
{
    float distance = totalDistance * 0.1F;

    bipedRightArm.rotationOrder = ModelRotationRenderer.XZY;
    bipedLeftArm.rotationOrder  = ModelRotationRenderer.XZY;

    bipedRightArm.rotateAngleY = (MathHelper.cos(distance + Quarter) * Eighth);
    bipedLeftArm.rotateAngleY  = (MathHelper.cos(distance + Quarter) * Eighth);

    bipedRightArm.rotateAngleZ = (MathHelper.cos(distance) * Eighth + Quarter);
    bipedLeftArm.rotateAngleZ  = (MathHelper.cos(distance) * Eighth - Quarter);

    bipedRightLeg.rotateAngleX = (MathHelper.cos(distance + Half + Quarter) * Sixteenth + Thirtytwoth);
    bipedLeftLeg.rotateAngleX  = (MathHelper.cos(distance + Quarter)        * Sixteenth + Thirtytwoth);

    bipedRightLeg.rotateAngleZ = (MathHelper.cos(distance) * Sixteenth + Thirtytwoth);
    bipedLeftLeg.rotateAngleZ  = (MathHelper.cos(distance) * Sixteenth - Thirtytwoth);
}
```

| 파트 | 축 | 식 | 진폭 | 위상 | 오프셋 |
|------|----|----|------|------|--------|
| rightArm | rotationOrder | XZY | — | — | — |
| leftArm  | rotationOrder | XZY | — | — | — |
| rightArm | Y (yaw)  | cos(d+π/2)·π/4 | π/4 ≈ 45° | +π/2 | 0 |
| leftArm  | Y (yaw)  | cos(d+π/2)·π/4 | π/4 ≈ 45° | +π/2 | 0 |
| rightArm | Z (roll) | cos(d)·π/4 + π/2 | π/4 ≈ 45° | 0 | +π/2 (팔 옆으로 펼침) |
| leftArm  | Z (roll) | cos(d)·π/4 - π/2 | π/4 ≈ 45° | 0 | -π/2 (팔 옆으로 펼침) |
| rightLeg | X (pitch)| cos(d+3π/4)·π/8 + π/16 | π/8 ≈ 22.5° | +3π/4 | +π/16 (살짝 들어올림) |
| leftLeg  | X (pitch)| cos(d+π/2)·π/8 + π/16  | π/8 ≈ 22.5° | +π/2 | +π/16 |
| rightLeg | Z (roll) | cos(d)·π/8 + π/16 | π/8 ≈ 22.5° | 0 | +π/16 (다리 살짝 벌림) |
| leftLeg  | Z (roll) | cos(d)·π/8 - π/16 | π/8 ≈ 22.5° | 0 | -π/16 |

**상수**(SmartRenderUtilities, 라디안):
- Half=π, Quarter=π/2, Eighth=π/4, Sixteenth=π/8, Thirtytwoth=π/16

### 1-3. `distance` 입력 — `totalDistance * 0.1F` 의 정체

**호출 흐름** (1.7.10):
1. `SmartRenderRender.renderRender L56`: `float totalDistance = statistics.getTotalDistance(renderPartialTicks);`
2. `SmartStatisticsData.getTotalDistance(partialTicks)` (L33-36):
   ```java
   return total - legYaw * (1.0F - renderPartialTicks);
   ```
3. 매 틱 갱신 (`SmartStatisticsData.calcualte`, L49-50):
   ```java
   legYaw += (distance - legYaw) * 0.4F;   // EMA, factor 0.4
   total  += legYaw;                         // 누적
   ```
   - `distance` = raw 이번 틱 이동 거리 × 4 (clamped to 1).
   - `legYaw` = currentSpeed (EMA, 0~1 clamp).
   - `total` = 누적 이동 (EMA 결과 누적).

**핵심 동작**:
- 정지/저속 → legYaw 작음 → totalDistance 천천히 증가.
- 가속 중 (낙하 시작 직후) → legYaw 가 0→1 까지 EMA factor 0.4 로 점진 증가. 1.0 도달까지 ~5틱. 즉 **휘저음이 점점 빨라짐 = 사용자가 보고한 "가속도"**.
- 자유낙하 안정 (terminal) → legYaw≈1.0, totalDistance += 1/틱 → 매 초 20 단위 → distance(=tot×0.1) 매 초 2 라디안.
- 매 프레임 partialTicks 로 lerp → **60Hz 부드러움** (원본 `getTotalDistance` 식이 prev/cur lerp 등가).

### 1-4. fade 보간 — falling 분기에 **arm/leg fade 없음**

`SmartRenderModel.java`:
- L41 `bipedOuter.fadeEnabled = true` — **bipedOuter 만**.
- L246-L248 fadeIntermediate/fadeStore — **bipedOuter 만**.
- arm/leg 의 fadeRotateAngle* 는 매 프레임 `reset()` (`ModelRotationRenderer.reset()` L210-L212) 으로 false 로 초기화 되므로 falling 분기에서 활성화하지 않으면 fade 미적용.

따라서 사용자가 느낀 "fade 보간 부드러움" 의 실체:
- fade 보간이 아닌, **`getTotalDistance(partialTicks)` partial-tick lerp 로 인한 60Hz 매끄러운 cos 입력 진동**.
- 즉 **distance 자체가 매 프레임 lerp 되어 매끄럽게 증가** → cos 결과가 60Hz 매끄러운 진동.

> ※ 원본 `ModelRotationRenderer.fadeIntermediate` (L315-331) 식:
> `prevAngle + (shouldAngle - prevAngle) * (totalTime - lastTotalTime) * 0.2F`
> 매 프레임 0.2배 lag 보간 (~5프레임 e-fold). 이는 bipedOuter 회전 (actualRotation 추적) 에만 적용.

---

## 2. 현재 1.21.1 포팅 상태

### 2-1. 진입 조건 — `MixinPlayerEntityModelClient.sm_setAngles` (L162-169)

```java
boolean isFalling = !player.isOnGround()
        && player.fallDistance > 1.5f
        && !sm.isClimbing && !sm.isCrawlClimbing && !sm.isCeilingClimbing
        && !player.isTouchingWater();
if (isFalling) {
    sm_animateFalling(animationProgress);
}
```

**원본 차이**:
- `fallDistance > 1.5f` (하드코딩) ↔ 원본 `_fallAnimationDistanceMinimum.value` 기본 **3F**.
- `!isClimbing && !isCrawlClimbing && !isCeilingClimbing && !isTouchingWater()` 추가 — 원본은 if-else 우선순위로 처리하지만 `setAngles` 내 if-else 체인이 동일한 효과 (이미 isClimbing/isCeilingClimbing/isSwimming_sm 등이 if 분기 위에 있음). 단 isClimbing 등 가드는 if-else 가 이미 보장하므로 잉여, isTouchingWater 만 SM swim 조건과 별개로 의미 있음.

### 2-2. 회전 분기 — `sm_animateFalling` (L749-770)

```java
private void sm_animateFalling(float animationProgress) {
    float distance = animationProgress * 0.1f;

    float rYaw  = MathHelper.cos(distance + QUARTER) * EIGHTH;
    float lYaw  = MathHelper.cos(distance + QUARTER) * EIGHTH;
    float rRoll = MathHelper.cos(distance) * EIGHTH + QUARTER;
    float lRoll = MathHelper.cos(distance) * EIGHTH - QUARTER;
    setAnglesXZY(rightArm, 0f, rYaw, rRoll);
    setAnglesXZY(leftArm,  0f, lYaw, lRoll);

    rightLeg.pitch = MathHelper.cos(distance + HALF + QUARTER) * SIXTEENTH + THIRTYTWOTH;
    leftLeg.pitch  = MathHelper.cos(distance + QUARTER)        * SIXTEENTH + THIRTYTWOTH;
    rightLeg.roll  = MathHelper.cos(distance) * SIXTEENTH + THIRTYTWOTH;
    leftLeg.roll   = MathHelper.cos(distance) * SIXTEENTH - THIRTYTWOTH;
}
```

**수식 자체는 1:1 정확** (회전 순서 XZY 도 setAnglesXZY 헬퍼로 정확히 처리, 다리 X/Z 부호/위상/오프셋 모두 일치).

**유일한 차이 = `distance` 입력값**:
- 현재: `animationProgress * 0.1f`
  - vanilla `animationProgress` = `entity.age + tickDelta` (LivingEntityRenderer 가 `f` 로 전달, partialTicks lerp 됨).
  - 매 프레임 단조 증가 — 시간 기반.
  - **이동 가속도 무관** — 정지든 낙하든 동일 속도로 증가.
- 원본: `totalDistance * 0.1F` (`statistics.getTotalDistance(partialTicks)`)
  - 이동 EMA 누적 — 가속도 반영.
  - 낙하 시작 시 0→1 까지 점진 증가 (legYaw EMA, factor 0.4).
  - 정지 시 거의 0 (떨어지지 않을 때).

**결과 차이**:
1. **속도** — vanilla animationProgress 매 프레임 +1/틱 ≈ 20/초 → distance += 2/초.
   원본 totalDistance 자유낙하 안정 ≈ +1/틱 ≈ 20/초 → distance += 2/초.
   → terminal velocity 에선 **거의 동일**.
2. **가속도** — 원본은 첫 5틱 동안 점진 가속 (사용자 보고 "팔/다리 휘저음 가속도").
   현재는 **즉시 max 속도** (animationProgress 가 일정 속도 증가).
   → **가시 차이**.
3. **부드러움** — 둘 다 partial-tick lerp 입력이라 60Hz 매끄러움.
   하지만 **원본은 cos 진동 자체가 가속도 (시작 부드럽게) + 안정 → 자연스러운 "휘적임"**.

### 2-3. `sm.stats` 에 이미 노출된 인프라

`SmartStatistics.java` (현재 코드):
- L13: `public float totalDistance` — 1.21.1 이식, 원본 1:1 누적 (세션 57 정정 적용됨).
- L78: `prevTotalDistance = totalDistance` (틱 시작에 저장).
- L135: `totalDistance += currentSpeed` (세션 57 — EMA 결과 누적, raw distance 아님 = 원본 1:1).
- L159: `getTotalDistance(partialTicks)` 함수 존재:
  ```java
  return totalDistance - currentSpeed * (1.0F - partialTicks);
  ```
  원본 `total - legYaw * (1.0F - partialTicks)` 와 1:1 동일.
- L160 코멘트: "`totalDistance - currentSpeed * (1.0F - partialTicks)`" — partial-tick lerp 적용.

→ **인프라 완비**. sm_animateFalling 입력만 교체하면 됨.

### 2-4. partialTicks 획득

setAngles inject 는 partialTicks 를 직접 받지 않음. 다른 inject 들 처럼 `MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false)` 또는 `getLastFrameDuration()` 로 획득 가능.

1.21.1 API:
- `MinecraftClient.getRenderTickCounter()` → `RenderTickCounter`
- `RenderTickCounter.getTickDelta(boolean tick)` — `false` 면 game tick partialTicks (매 프레임 0~1).

이미 `SmartMovingHud.java` L37 에서 `RenderTickCounter` 사용 중.

---

## 3. 매핑 결정

### 3-1. distance 입력 정정 (핵심)

```java
// 변경 전
private void sm_animateFalling(float animationProgress) {
    float distance = animationProgress * 0.1f;
    ...
}

// 변경 후 (원본 1:1)
private void sm_animateFalling(SmartMovingClientState sm) {
    float partialTicks = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false);
    float totalDistance = sm.stats.getTotalDistance(partialTicks);
    float distance = totalDistance * 0.1f;
    ...
}
```

이렇게 하면:
- **가속도**: legYaw EMA factor 0.4 로 낙하 초기 자연 점진 가속 → 사용자 보고 "휘저음 가속도" 1:1 재현.
- **부드러움**: `getTotalDistance(partialTicks)` 매 프레임 prev/cur lerp → 60Hz cos 입력 매끄러움 → 사용자 보고 "fade 보간 부드러움" 1:1 재현 (실체는 partial-tick lerp).
- **정지 시**: totalDistance 누적 정지 → cos 입력 고정 → 휘저음 멈춤 (원본 1:1).

### 3-2. 진입 조건 정정 (1:1 정확성)

```java
// 변경 전
&& player.fallDistance > 1.5f

// 변경 후 (원본 _fallAnimationDistanceMinimum 기본 3F)
&& player.fallDistance > SmartMovingConfig.Config.fallAnimationDistanceMinimum
```

`SmartMovingConfig` 에 `_fallAnimationDistanceMinimum` 필드 추가 필요 (원본 기본값 3F).

### 3-3. 의도적 차이 (보존)

- `setAnglesXZY` 헬퍼의 quaternion 변환 — 이미 BUG-30 정정 (qY*qZ*qX) 으로 GL 1:1 매칭. **유지**.
- isClimbing/isCrawlClimbing/isCeilingClimbing/isTouchingWater 가드 — if-else 에서 이미 처리되지만 명시적이라 안전. **유지**.

---

## 4. 검증 계획

1. **빌드 검증**: gradle build → 컴파일 + Mixin AP 검증.
2. **수식 비교 자동화**: 빌드 후 변경된 sm_animateFalling 의 회전 식을 원본 L531-549 와 라인별 diff 로 재확인.
3. **인게임 통합테스트**: 메모리 `feedback_integration_test.md` 정책에 따라 별도 단계로 분리 (deferred).
   - 검증 시나리오: 점프 → 낙하 직후 휘저음 가속, 안정 후 매끄러운 진동, 착지 시 즉시 종료.

---

## 5. 참고 파일

### 원본
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\render\SmartMovingModel.java` L531-549 (falling 분기 본체)
- `.../SmartMovingSelf.java` L3278-3282 (doFallingAnimation)
- `.../config\SmartMovingConfig.java` L223 (_fallAnimationDistanceMinimum 기본 3F)
- `C:\Work\minecraft\porting\sm_original\SmartRender\src\main\java\net\smart\render\SmartRenderRender.java` L56 (totalDistance 호출)
- `.../statistics\SmartStatisticsData.java` L33-36, L49-50 (getTotalDistance, calcualte)
- `.../ModelRotationRenderer.java` L41/L210-L212/L298-L331 (fade 시스템 — bipedOuter 만)
- `.../SmartRenderModel.java` L246-248 (fade 호출 — bipedOuter 만)

### 1.21.1 포팅
- `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java` L162-169 (진입), L749-770 (sm_animateFalling 본체)
- `src/main/java/choco/ratel/smartmoving/stat/SmartStatistics.java` L135 (totalDistance 누적), L159-161 (getTotalDistance partialTicks lerp)
- `src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java` (fallAnimationDistanceMinimum 추가 필요)
