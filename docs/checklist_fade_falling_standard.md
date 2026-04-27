# 낙하/기본 상태 fade 매핑 체크리스트

> 리서치: `docs/research/research_fade_falling_standard.md`
> 핵심: ModifyArg `sm_modifyBodyYaw` 에 fade 분기 추가 (비행 force 분기 외).

---

## 0. 사전 확인

- [x] 원본 ModelRotationRenderer.GetIntermediateAngle 식 정밀 read (factor 0.2 * deltaT).
- [x] SmartRenderModel L208/209/247 fade 호출 확인 (player 항상 fade).
- [x] 비행 매핑 lerpFadeAngle 패턴 확인 (sm_setupTransforms TAIL).
- [x] 낙하/기본 상태 fade 부재 확인.

## 1. MixinPlayerEntityRenderer 변경

- [ ] **1-1**. 새 static fields 추가 (L37 근처):
  ```java
  @Unique private static float smCachedAnimationProgress;
  @Unique private static float smStandardBodyYawPrev = Float.NaN;
  @Unique private static float smStandardFadeTimePrev = Float.NaN;
  ```

- [ ] **1-2**. `sm_captureBodyYaw` HEAD 에서 animationProgress 캐시 (L97 근처):
  ```java
  smCachedAnimationProgress = animationProgress;
  ```

- [ ] **1-3**. `sm_modifyBodyYaw` 에 fade 분기 추가:
  ```java
  private float sm_modifyBodyYaw(float bodyYaw) {
      if (smBodyYawActive) return smBodyYawOverride;
      if (!SmartMovingConfig.Config.enabled) return bodyYaw;
      return applyFadeAngleDegrees(bodyYaw);
  }
  ```

- [ ] **1-4**. `applyFadeAngleDegrees` 헬퍼 추가 (private static):
  - 원본 GetIntermediateAngle L347-365 1:1.
  - degrees 단위 (Whole=360, Half=180).
  - deltaT > 2 또는 NaN 가드 → 즉시 (no fade).
  - prev/prevTime 갱신.

## 2. 빌드 검증

- [ ] **2-1**. `./gradlew compileClientJava` 통과.

## 3. 코드 리뷰

- [ ] **3-1**. 비행 분기 (smBodyYawActive=true) 시 fade 미적용 확인 (사용자 "비행 건드리지 말기").
- [ ] **3-2**. SM disabled 시 vanilla bodyYaw 그대로 통과 확인.
- [ ] **3-3**. 단위 정확성 — bodyYaw 매개변수 (degrees), animationProgress (ticks).

## 4. 통합 테스트 (deferred)

- [ ] **4-1**. (deferred) 마우스 빠르게 좌우 회전 → 머리 즉시 / 몸 lag (낙하 중).
- [ ] **4-2**. (deferred) 기본 idle/walk/run 시 동일 효과.
- [ ] **4-3**. (deferred) 비행 시 fade 영향 없음 (smBodyYawActive=true 우선).
