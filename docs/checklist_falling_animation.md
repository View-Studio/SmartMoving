# Falling 애니메이션 1:1 이식 체크리스트

> 리서치: `docs/research/research_falling_animation.md`
> 핵심 변경: `sm_animateFalling` 의 distance 입력을 `animationProgress * 0.1f` →
>   `sm.stats.getTotalDistance(partialTicks) * 0.1f` 로 교체.
>   부수: `_fallAnimationDistanceMinimum` config 추가 + 진입 조건 1.5f → 3F.

---

## 0. 사전 확인

- [x] 원본 `SmartMovingModel.java:531-549` falling 분기 라인별 분해 (research §1-2)
- [x] 원본 `getTotalDistance(partialTicks)` 식 + EMA 가속 동작 확인 (research §1-3)
- [x] 원본 fade 시스템이 arm/leg 미적용 (bipedOuter 한정) 확인 (research §1-4)
- [x] 현재 `sm_animateFalling` 본체 = 원본과 수식 1:1 일치, 입력만 다름 (research §2-2)
- [x] `sm.stats.getTotalDistance(partialTicks)` 인프라 이미 노출 (research §2-3)

## 1. SmartMovingConfig — `_fallAnimationDistanceMinimum` 추가

- [ ] **1-1**. `SmartMovingConfig.java` 의 `fallingDistanceMinimum` 근처에 `fallAnimationDistanceMinimum` 필드 추가.
  - 원본 `_fallAnimationDistanceMinimum` 기본 **3F**.
  - 이름 / 주석은 기존 `fallingDistanceMinimum` 패턴을 따른다.

## 2. MixinPlayerEntityModelClient — sm_animateFalling 입력 교체

- [ ] **2-1**. `sm_animateFalling` 시그니처 변경:
  `(float animationProgress)` → `(SmartMovingClientState sm)`.
- [ ] **2-2**. 메서드 본체 `distance` 계산을 1:1 교체:
  ```java
  float partialTicks = net.minecraft.client.MinecraftClient.getInstance()
          .getRenderTickCounter().getTickDelta(false);
  float totalDistance = sm.stats.getTotalDistance(partialTicks);
  float distance = totalDistance * 0.1f;
  ```
- [ ] **2-3**. 호출처 (`sm_setAngles` L168) 업데이트:
  `sm_animateFalling(animationProgress);` → `sm_animateFalling(sm);`.
- [ ] **2-4**. JavaDoc 주석 업데이트 — "totalTime(animationProgress)으로 totalDistance 근사" 문구를
  실제 매핑 (`sm.stats.getTotalDistance(partialTicks) * 0.1F` 1:1) 으로 교체.

## 3. MixinPlayerEntityModelClient — 진입 조건 정정

- [ ] **3-1**. `sm_setAngles` L164 의 `player.fallDistance > 1.5f` 를
  `player.fallDistance > SmartMovingConfig.Config.fallAnimationDistanceMinimum` 로 교체.
- [ ] **3-2**. SM disabled 시 fallback 동작 확인 — `cfgEnabled=false` 시 falling 분기 진입 X (이미 L127 가드).
  추가 변경 불필요.

## 4. 빌드 검증

- [ ] **4-1**. `./gradlew build` (또는 동등) 실행 — 컴파일 + Mixin AP 검증.
- [ ] **4-2**. 빌드 산출물에 sm_animateFalling 변경 반영 확인 (필요 시 `javap -p` 등).

## 5. 코드 리뷰 (전수 1:1 교차 대조)

- [ ] **5-1**. 수정된 `sm_animateFalling` 9개 회전식 vs 원본 L538-548 라인별 동일 확인 (변수명만 다름).
- [ ] **5-2**. 회전 순서 XZY 유지 — `setAnglesXZY` 헬퍼 호출 그대로 (BUG-30 정정 적용 상태).
- [ ] **5-3**. 다른 분기 (rope/swim/dive/flying 등) 의 `sm.stats.totalDistance` 직접 사용처는
  본 리팩의 영향 없음 — 변경 범위 falling 한정 (의도). 추후 별도 작업 (focus 외).

## 6. 통합 테스트 (deferred — 메모리 정책 준수)

- [ ] **6-1**. (deferred) 인게임 검증: 점프 → 낙하 직후 가속, terminal 이후 안정 진동, 착지 즉시 종료.
- [ ] **6-2**. (deferred) 비교 영상: 1.7.10 원본 vs 본 빌드 — 휘저음 속도/가속도/매끄러움.

## 7. 메모리 갱신

- [ ] **7-1**. `MEMORY.md` 에 본 작업 결과 항목 추가 검토.
  - 후보: "낙하 애니메이션 입력 = totalDistance * 0.1F 1:1 (animationProgress 사용 금지)" 가이드.
- [ ] **7-2**. 다른 분기들 (`sm_animateRopeSliding`, `sm_animateCeilingClimbing` 등) 의 `animationProgress`
  사용 흐름이 동일한 잠재 문제인지 따로 audit 항목 등재 검토 (현재 작업 범위 외).

## 8. 커밋

- [ ] **8-1**. `fix(falling): sm_animateFalling 입력 totalDistance 1:1 정정` 패턴으로 커밋.
- [ ] **8-2**. CLAUDE.md 규칙 준수 — 한국어, 단일 타입.
