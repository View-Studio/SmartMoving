# 머리/몸 회전 속도 차이 (fade lerp) — 낙하/기본 상태 1:1 매핑

> 작업 일자: 2026-04-27
> 사용자 요청: "마우스 회전 시 머리/몸 회전 속도 차이로 부드러운 움직임. 비행은 완성,
>   **낙하와 기본 상태**에 적용. 비행은 건드리지 말기."

---

## 1. 원본 (1.7.10 SmartRender) fade 시스템

### 1-1. fade 호출 위치 (SmartRenderModel.setRotationAngles)
- L208: `bipedOuter.rotateAngleY = actualRotation / RadiantToAngle` (target 설정).
- L209: `bipedOuter.fadeRotateAngleY = !(entity.ridingEntity instanceof EntityPig)` → **player 항상 true**.
- L247: `bipedOuter.fadeIntermediate(totalTime)` 호출.
- L248: `bipedOuter.fadeStore(totalTime)` (previous 갱신).

→ **모든 SM 활성/비활성 상태에서 매 프레임 fade 적용**.

### 1-2. fade 식 (ModelRotationRenderer.GetIntermediateAngle, L347-365)
```java
private static float GetIntermediateAngle(float prevAngle, float shouldAngle, boolean fade,
                                          float lastTotalTime, float totalTime) {
    if (!fade || shouldAngle == prevAngle) return shouldAngle;

    while (prevAngle   >= Whole) prevAngle   -= Whole;        // Whole = 2π
    while (prevAngle   <  0F)    prevAngle   += Whole;
    while (shouldAngle >= Whole) shouldAngle -= Whole;
    while (shouldAngle <  0F)    shouldAngle += Whole;

    if (shouldAngle > prevAngle && (shouldAngle - prevAngle) > Half) prevAngle   += Whole;
    if (shouldAngle < prevAngle && (prevAngle - shouldAngle) > Half) shouldAngle += Whole;

    return prevAngle + (shouldAngle - prevAngle) * (totalTime - lastTotalTime) * 0.2F;
}
```

- **factor 0.2 * deltaT lag lerp** (대략 5 frame e-fold = ~83ms at 60fps).
- ±π wrap (짧은 방향 보간).
- `fadeIntermediate` 가드 (L317): `totalTime - previous.totalTime <= 2F` (2초 이내만).

### 1-3. 효과
- target = `actualRotation` = renderYawOffset 라디안 lerped (vanilla 가 1 frame lerp).
- 원본 `bipedOuter.Y` = vanilla lerp + 추가 0.2 lag → **부드러운 lag**.
- vanilla setAngles 의 `head.yaw = netHeadYaw` 는 fade 안 받음 → 머리 직접 cameraYaw 따라감.
- → **머리 즉시, 몸 부드럽게 lag** = 사용자 보고 "원본은 부드러움".

---

## 2. 현재 1.21.1 매핑

### 2-1. 비행 — 이미 매핑됨 (`MixinPlayerEntityRenderer.sm_setupTransforms` TAIL)
```java
float thetaLerped = lerpFadeAngle(sm.smOuterTiltX_prev, thetaTarget,
                                  sm.smOuterFade_prevTime, animationProgress);
float yawLerped   = lerpFadeAngle(sm.smOuterExtraYaw_prev, yawTarget, ...);

matrices.multiply(POSITIVE_Y.rotation(-yawLerped));
matrices.multiply(POSITIVE_X.rotation(-thetaLerped));
sm.smOuterTiltX_prev = thetaLerped;
sm.smOuterExtraYaw_prev = yawLerped;
sm.smOuterFade_prevTime = animationProgress;
```

→ 비행 시 setupTransforms TAIL 에서 추가 yaw 를 fade lerp.

### 2-2. 낙하/기본 상태 — fade 부재
- `sm_captureBodyYaw` HEAD 에서 isFalling 분기 없음, 기본 상태 (anySmState=false) 시 즉시 return.
- ModifyArg `sm_modifyBodyYaw` 가 `smBodyYawActive=false` 시 vanilla bodyYaw 그대로 통과.
- → vanilla `lerpAngleDegrees(prevBodyYaw, bodyYaw, partialTicks)` 만 적용 (1 frame lerp). **추가 fade 없음**.
- 사용자 보고 "원본만큼 부드럽지 않음" 직접 원인.

### 2-3. vanilla LivingEntityRenderer.render 흐름 (디스어셈블 검증)
```
offset 49: prev_h = lerpAngleDegrees(g, prevBodyYaw, bodyYaw)        ← 1 frame lerp
offset 329: setupTransforms(entity, ..., prev_h, ...)                ← 우리 ModifyArg 가 prev_h 변경 가능
```

→ ModifyArg 로 setupTransforms 의 bodyYaw 인자에 추가 fade 적용 가능.

---

## 3. 매핑 결정

### 3-1. ModifyArg `sm_modifyBodyYaw` 에 fade 분기 추가

```java
@ModifyArg(method = "setupTransforms", index = 3)
private float sm_modifyBodyYaw(float bodyYaw) {
    if (smBodyYawActive) return smBodyYawOverride;             // 비행/SM force 분기
    if (!SmartMovingConfig.Config.enabled) return bodyYaw;     // SM disabled
    return applyFadeAngleDegrees(smStandardBodyYawPrev, bodyYaw,
                                  smStandardFadeTimePrev, smCachedAnimationProgress);
}
```

- 비행/SM force 분기는 `smBodyYawActive=true` → 그대로 통과 (영향 없음).
- 기본 상태 + 낙하 (smBodyYawActive=false 일 때) 만 fade 적용.

### 3-2. animationProgress 캐시
`sm_captureBodyYaw` HEAD 매개변수에 animationProgress 있음:
```java
private void sm_captureBodyYaw(... float animationProgress, float bodyYaw, ...) {
    SmartMovingClientState.globalCachedTickDelta = tickDelta;     // 기존
    smCachedAnimationProgress = animationProgress;                // 추가
    ...
}
```

### 3-3. fade 식 (원본 1:1, degrees 단위)
```java
private static float applyFadeAngleDegrees(float prev, float target,
                                            float prevTime, float curTime) {
    if (Float.isNaN(prev) || Float.isNaN(prevTime)) {
        smStandardBodyYawPrev = target;
        smStandardFadeTimePrev = curTime;
        return target;
    }
    float deltaT = curTime - prevTime;
    if (deltaT <= 0F || deltaT > 2F) {                       // 원본 L317 가드
        smStandardBodyYawPrev = target;
        smStandardFadeTimePrev = curTime;
        return target;
    }

    float p = prev, t = target;
    while (p >= 360F) p -= 360F;
    while (p < 0F)    p += 360F;
    while (t >= 360F) t -= 360F;
    while (t < 0F)    t += 360F;
    if (t > p && t - p > 180F) p += 360F;                    // 짧은 방향 wrap
    if (t < p && p - t > 180F) t += 360F;

    float faded = p + (t - p) * deltaT * 0.2F;               // 원본 L364 1:1
    smStandardBodyYawPrev = faded;
    smStandardFadeTimePrev = curTime;
    return faded;
}
```

원본 L347-365 1:1 (단위 degrees, Whole=360, Half=180).

### 3-4. 새 static fields
```java
@Unique private static float smCachedAnimationProgress;
@Unique private static float smStandardBodyYawPrev = Float.NaN;
@Unique private static float smStandardFadeTimePrev = Float.NaN;
```

---

## 4. 검증

1. **빌드**: gradle compile.
2. **시각 결과 예측**:
   - 마우스 빠르게 좌우로 돌리기:
     - 머리: cameraYaw 즉시 따라감 (vanilla 기본).
     - 몸: cameraYaw 향해 부드러운 lag (~83ms e-fold).
   - 비행: 영향 없음 (smBodyYawActive=true → force).
   - 낙하: fade 적용 → 낙하 중 마우스 회전 시 몸 lag.
   - 기본 idle/walk/run: fade 적용 → 자연스러운 부드러움.
3. **인게임 통합테스트**: deferred.

---

## 5. 참고

### 원본
- `SmartRender\src\main\java\net\smart\render\SmartRenderModel.java` L208/209/247 (fade 호출).
- `.../ModelRotationRenderer.java` L298-365 (fadeIntermediate, GetIntermediateAngle).

### 1.21.1
- `MixinPlayerEntityRenderer.java` L33-36 (static fields), L97 (globalCachedTickDelta), L251-253 (sm_modifyBodyYaw — 변경 대상).
- `MixinPlayerEntityRenderer.java` L351-372 (sm_setupTransforms 비행 fade — 그대로 보존).
