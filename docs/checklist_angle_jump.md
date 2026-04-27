# Angle Jump 1:1 이식 체크리스트

> 리서치: `docs/research/research_angle_jump.md`
> 핵심 변경:
>   1. 애니메이션 — bipedPelvic 효과 1:1 (다리 yaw 에 cameraDelta 추가)
>   2. 트리거 가드 (canAngleJump/canLeftJump/canRightJump/canBackJump) 추가
>   3. angleJumpType 매 틱 reset (onGround || verticalCollision) 추가
>   4. 속도/거리 — 변경 없음 (1:1 일치 확인됨)

---

## 0. 사전 확인

- [x] 원본 SmartMovingSelf L1918-1944 (트리거 발동) + L2898-2964 (가드/카운터/reset) 정밀 read
- [x] 원본 SmartMovingModel.animateAngleJumping L559-584 정밀 read
- [x] 원본 tryJump D-11 (L2081-2095) 1:1 일치 확인
- [x] _angleJumpHorizontalFactor=0.4F, _angleJumpVerticalFactor=0.2F 일치 확인
- [x] getJumpHorizontalFactor / getJumpVerticalFactor 1:1 일치 확인

## 1. 애니메이션 — sm_animateAngleJumping bipedPelvic 효과 추가 (핵심)

- [ ] **1-1**. `MixinPlayerEntityModelClient.sm_animateAngleJumping` 수정:
  - 메서드 시그니처에 `LivingEntity entity` 추가 (bodyYaw 접근 위해).
  - bodyYaw 라디안 변환 + cameraDelta 계산.
  - leg.yaw 에 cameraDelta 합산.
- [ ] **1-2**. 호출처 (`sm_setAngles` L198) 업데이트 — entity 전달.
- [ ] **1-3**. JavaDoc 갱신 — bipedPelvic 1:1 매핑 명시 (이전 "생략" 주석 정정).

## 2. 트리거 가드 — canAngleJump/canSideJump/canBackJump 추가

- [ ] **2-1**. `SmartMovingClientState.tickEssential` (또는 카운터 갱신 위치) 에 `canAngleJump` 변수 추가:
  ```java
  boolean canAngleJump = !player.isSleeping() && player.isOnGround()
                       && !isCrawling && !isClimbing && !isCrawlClimbing
                       && !isSwimming_sm && !isDiving;
  ```
- [ ] **2-2**. 좌/우 카운터 갱신을 `canLeftJump = canAngleJump && cfg.angleJumpSide && !rightKey.isPressed()`,
  `canRightJump = canAngleJump && cfg.angleJumpSide && !leftKey.isPressed()` 가드 안으로 이동.
  else 분기에서 count = 0 reset.
- [ ] **2-3**. 뒤 카운터 갱신을 `canBackJump = canAngleJump && cfg.angleJumpBack &&
  !forwardKey.isPressed() && !isStandupSprintingOrRunning()` 가드 안으로 이동.
  - `isStandupSprintingOrRunning` 1.21.1 등가 함수 확인 (SmartMovingClientState 내 또는 sm.isFast 등).

## 3. angleJumpType 매 틱 reset 추가

- [ ] **3-1**. `SmartMovingClientState.tickEssential` 어딘가에 (원본 L2963-2964 동등):
  ```java
  if (player.isOnGround() || player.verticalCollision) {
      angleJumpType = 0;
  }
  ```
- [ ] **3-2**. 위치는 카운터 갱신 (a/s/d) 직후 또는 다른 SM 상태 갱신 블록과 인접하게.

## 4. 빌드 검증

- [ ] **4-1**. `./gradlew compileJava compileClientJava` 통과.

## 5. 코드 리뷰

- [ ] **5-1**. sm_animateAngleJumping 의 cameraDelta 부호 — 메모리 `feedback_render_scale_negation.md`
  (scale(-1,-1,1) Y 부호 영향) 참조. 인게임 검증 후 부호 정정 가능.
- [ ] **5-2**. `isStandupSprintingOrRunning` 의 1.21.1 등가 정확성 검증.
- [ ] **5-3**. canAngleJump 의 sleeping 가드: 1.21.1 `player.isSleeping()` 또는 `sm.isSleeping` 확인.
- [ ] **5-4**. verticalCollision 필드: 1.21.1 `Entity.verticalCollision` 1:1.

## 6. 통합 테스트 (deferred)

- [ ] **6-1**. (deferred) 인게임: a 두번 탭 → 좌 점프, 다리 좌측 회전 + 카메라 회전 시 즉시 따라감.
- [ ] **6-2**. (deferred) 우/뒤 + 대각선 (좌+뒤, 우+뒤) 점프 검증.
- [ ] **6-3**. (deferred) 점프 후 onGround 복귀 시 자세 복원 (angleJumpType=0).
- [ ] **6-4**. (deferred) 가드 검증: w 누른 상태 s 두번 탭 → back jump 차단 (forwardButton.Pressed 가드).

## 7. 메모리 갱신

- [ ] **7-1**. 본 작업 결과를 메모리에 등재 검토 — `project_angle_jump_complete.md` 또는
  기존 jump 관련 메모리에 통합.

## 8. 커밋

- [ ] **8-1**. `fix(angle_jump): 1:1 정정 — bipedPelvic 효과 + 트리거 가드 + reset` 패턴.
