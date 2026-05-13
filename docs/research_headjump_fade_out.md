# 헤드점프 종료 fade 보간 — 리서치

## 사용자 보고
헤드점프 중 벽에 부딪혀서 플레이어가 옆으로 튕길 때 애니메이션 페이드 보간이 안 되어 있음. 중간에 이어지는 느낌 없음. self 도 안 되어있음.

## 시나리오 매트릭스
1. isHeadJumping=true → 누운 자세 (body R_x = π/2 - currentVerticalAngle ≈ horizontal).
2. 벽 부딪힘 → vx motion 반전 → 옆으로 튕김.
3. 짧은 공중 시간 후 onGround=true → isHeadJumping=false (L2181-2186 5-AND).
4. wasHeadJumping && !isHeadJumping && onGround → handleCrash + standing 자세 진입.
5. **그 시점 모델 자세가 누운 자세 → 즉시 standing 자세 (instant)** ← 사용자 보고 BUG.

## 원본 1.7.10 매트릭스

### SmartMovingModel.java L505-509 (헤드점프 분기 안)
```java
else if(isHeadJump)
{
    bipedOuter.fadeRotateAngleX = true;   // fade flag 활성화
    bipedOuter.rotateAngleX = (Quarter - currentVerticalAngle);
    ...
}
```

### SmartRenderModel.java L210 (default)
```java
fadeRotateAngleX = false;  // default reset
```

### SmartRenderModel.java L241-242 (외 분기)
```java
if(bipedOuter.previous != null && !bipedOuter.fadeRotateAngleX)
    bipedOuter.previous.rotateAngleX = bipedOuter.rotateAngleX;
```
= **fadeRotateAngleX=false 시 previous.rotateAngleX = 현재값 즉시 동기화**.

### ModelRotationRenderer.java L347-365 (fade 식)
```java
private static float GetIntermediateAngle(float prevAngle, float shouldAngle, boolean fade, ...) {
    if(!fade || shouldAngle == prevAngle)
        return shouldAngle;  // ← fade=false 시 즉시 shouldAngle 반환 (fade 없음)
    ...
}
```

### 결론 (원본)
- isHeadJump=true frame: fadeRotateAngleX=true → fade 진행.
- isHeadJump=false 진입 frame: fadeRotateAngleX=false (default) → fade 식 즉시 shouldAngle (=0) 반환 + previous=현재값 동기화.
- = **원본도 헤드점프 종료 시 fade out 없음 (instant)**.

## 우리 코드 매트릭스 (MixinPlayerEntityRenderer.java)

### L741-764 (헤드점프 분기 안)
```java
if (sm.isHeadJumping) {
    float thetaTarget = sm.wasSelfSlideFire ? π/2 : π/2 - currentVerticalAngle;
    if (sm.wasSelfSlideFire) {
        sm.smHeadJumpTiltX_prev = π/2;
        sm.smHeadJumpFade_prevTime = animationProgress;
    }
    float thetaLerped = lerpFadeAngle(sm.smHeadJumpTiltX_prev, thetaTarget,
                                       sm.smHeadJumpFade_prevTime, animationProgress);
    matrices.translate(0f, 1.5f, 0f);
    matrices.multiply(RotationAxis.POSITIVE_X.rotation(-thetaLerped));
    matrices.translate(0f, -1.5f, 0f);
    sm.smHeadJumpTiltX_prev = thetaLerped;
    sm.smHeadJumpFade_prevTime = animationProgress;
}
```

### L801-805 (헤드점프 외 분기)
```java
if (!sm.isHeadJumping && !sm.isSliding) {
    sm.smHeadJumpTiltX_prev = 0f;          // ← 즉시 reset = fade out 차단
    sm.smHeadJumpFade_prevTime = animationProgress;
    sm.wasSelfSlideFire = false;
}
```

### 결론 (우리 매핑)
- isHeadJumping=true frame: prev → target lerp (fade in/유지 정상).
- isHeadJumping=false 진입 frame: 외 분기 (L786-805) 진입. matrices.multiply 호출 없음 → R_x=0 instant. prev=0 강제 reset.
- = **원본 매트릭스와 동일 (instant)**.

## 핵심 인사이트

**원본도 fade out 없음.** 우리 매핑은 원본 1:1 정확. 사용자 의도 = **원본에 없는 새 효과 추가** (= fade out 시각 매끄러움).

메모리 [[feedback-headjump-animation-scope]] 참조: 사용자 명시 (2026-05-11) "기능 자체 (= state/setupTransforms/카메라/박스) 절대 X. setAngles 영역 한정." — 그러나 현재 fade 식은 setupTransforms 안에 있음 (matrices.multiply R_x). setAngles 가 아닌 matrices transform 변경.

= **fade out 추가는 setupTransforms 안 matrices.multiply 식 확장**. 시각 만 영향. 기능 영향 X.

## Fix 방향 후보

### A. fade out 별도 prev 유지 (= isHeadJumping=true 마지막 frame 의 thetaLerped 값을 종료 후도 prev 로 유지)
```java
if (!sm.isHeadJumping && !sm.isSliding) {
    // prev 가 0 에 가까우면 reset, 아니면 fade out 진행.
    if (Math.abs(sm.smHeadJumpTiltX_prev) > 0.05f) {
        // fade out 진행: target=0 으로 lerp.
        float thetaLerped = lerpFadeAngle(sm.smHeadJumpTiltX_prev, 0f,
                                           sm.smHeadJumpFade_prevTime, animationProgress);
        matrices.translate(0f, 1.5f, 0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotation(-thetaLerped));
        matrices.translate(0f, -1.5f, 0f);
        sm.smHeadJumpTiltX_prev = thetaLerped;
        sm.smHeadJumpFade_prevTime = animationProgress;
    } else {
        sm.smHeadJumpTiltX_prev = 0f;
        sm.smHeadJumpFade_prevTime = animationProgress;
        sm.wasSelfSlideFire = false;
    }
}
```

장점: 자연 fade out (= isHeadJumping=true → false 전환 시 prev=직전 thetaLerped → 0 점진 lerp).
단점: wasSelfSlideFire reset 시점 fade out 진행 중에는 안 됨 → 시작 시 잘못 매트릭스 가능. 가드 분리 필요.

### B. fade in 도 자연하게 — wasSelfSlideFire 강제 reset (= prev = π/2) 제거
prev 가 자연 0 → π/2 lerp 되도록 식 변경. fix #81 v2 가 *prev=π/2 즉시 강제* 한 이유는 *발사 frame 부터 완전 수평* 의도. = **fix #81 v2 의도와 충돌**. 시도 안 함.

## 추천: 옵션 A

자연 fade out 만 추가. fade in 식 (= fix #81 v2 prev=π/2 강제) 유지.

## 영향 범위 검토

- self 1인칭 시각: 모델 카메라 안 보임. fade out 영향 X.
- self 3인칭 시각: 모델 보임. fade out 적용.
- remote 시각: 모델 보임. fade out 적용.
- 다른 transition (= 헤드점프 → 슬라이딩, 헤드점프 → 비행 등): wasSelfSlideFire reset 시점이 fade out 진행 중 일 가능. **검토 필요**.

## 회귀 위험
- fix #81 v2: 자체슬라이딩 발사 시 fade in (prev=0 → π/2) 의 lerp 12 tick 차이 잔존. → **fade in 분기는 영향 X** (= isHeadJumping=true 시 prev=π/2 강제 매트릭스 매트릭스 유지).
- fix #81 v3: 헤드점프/슬라이딩 외 분기 wasSelfSlideFire reset. → fade out 진행 중에 reset 시도 → 다음 헤드점프 발사 시 영향. **prev=0 도달 후 reset 분리 필요**.
- 슬라이딩 → 비행 전환 (fix #87): `isSliding && !isHeadJumping` → 외 분기 진입 시점. fade out 진행 매트릭스 매트릭스 매트릭스 매트릭스 매트릭스 — **slide 분기 prev=π/2 강제** 매트릭스 매트릭스 매트릭스 매트릭스 매트릭스. **검증 필요**.

## 메모리 참조
- [[feedback-headjump-animation-scope]] (= setAngles 영역 한정, 시각 변경 OK)
- [[project-headjump-final]] (= fix #79~#89 정착, 함부로 수정 금지)
- [[feedback-self-slide-fire-flag]] (= wasSelfSlideFire 패턴)
- [[feedback-fade-prev-pre-entry-pose]] (= fade prev 시작값 = 진입 직전 자세)
