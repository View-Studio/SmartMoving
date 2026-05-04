# Sliding 기능 작업 체크리스트 (완료)

> 기반: `docs/research_sliding.md`. 범위: **기능**만 (애니메이션 별도).
>
> 핵심 누락 2건 fix + 1건 검증 + 인게임 검증 시 추가 발견 3건 fix 정착.
> 사용자 2026-05-04 명시 완결 선언. 메모리: `project_sliding_complete.md`.

---

## Phase A — 종료 조건 재배치 + 1:1 정정 (원본 L2563-L2567)

### A-1. `SmartMovingSlider.handleSliding` 의 종료 분기 제거
- [ ] `SmartMovingSlider.java` L73-L77 의 `if (horizontalSpeed < cfg.slidingSpeedStopFactor) { sm.isSliding = false; }` 블록 삭제.
- [ ] 메서드 docstring 의 "종료 조건" 항목 제거 (이제 SmartMovingClientState 책임).

### A-2. `SmartMovingClientState` 메인 tick 에 종료 분기 추가
- [ ] 위치: `SmartMovingClientState.java` L1947 (직접 진입 6-AND 끝) 직후, L1949 (`isHeadJumping` 재평가) 직전. 원본 L2553 다음 줄 위치 1:1.
- [ ] 추가 코드 (원본 L2563-L2567 1:1):
  ```java
  // 원본 L2563-L2567: sneak 떼기 OR 속도² 임계 → 슬라이딩 종료 + 즉시 크롤 진입.
  //   horizontalSpeedSquare 는 원본 L2389 의 `motionX² + motionZ²` 1:1.
  //   stopFactor * 0.01 단위 (원본은 speed² < stopFactor*0.01).
  if (isSliding && (!sneakPressedRaw
                    || horizontalSpeedSquare < cfg0.slidingSpeedStopFactor * 0.01)) {
      isSliding   = false;
      wasCrawling = toCrawling();   // 원본 L2566
  }
  ```
- [ ] `horizontalSpeedSquare` 변수 위치 확인: 메인 tick 안 이미 계산됨 (원본 L2389 매핑 위치 grep 으로 확인).
- [ ] `sneakPressedRaw` 변수 위치 확인: 메인 tick 안 이미 계산됨 (위 영역에서 사용 중).
- [ ] `cfg0.slidingSpeedStopFactor` 접근 가능 확인 (`SmartMovingConfig.java` L780 ✅).
- [ ] `toCrawling()` 메서드 시그니처 확인 (이미 다른 분기에서 호출 중 ✅).

### A-3. 컴파일 + 빌드 확인
- [ ] `gradlew compileJava compileClientJava` 통과.

---

## Phase B — strafe 강제 회전 (원본 L730-L744)

### B-1. `SmartMovingSlider.handleSliding` 에 강제 회전 블록 추가
- [ ] 위치: damping 계산 (`L59`) 직후, `setVelocity` (`L67`) 직전.
- [ ] 추가 코드 (원본 L730-L744 1:1):
  ```java
  // 원본 L730-L744: strafe 강제 회전 (좌우 키로 슬라이딩 방향 컨트롤).
  //   _slideControlDegrees: tick 당 회전량 (deg). 기본 1F.
  //   moveStrafing 부호로 좌(+)/우(-) 회전 방향 결정.
  float moveStrafing = player.input.movementSideways;
  if (moveStrafing != 0F && cfg.slideControlDegrees > 0F) {
      double angle = -Math.atan(newVx / newVz);
      if (!Double.isNaN(angle)) {
          if (newVz < 0) angle += Math.PI;
          // 원본 RadiantToAngle = 180/π → Math.toRadians 로 1:1.
          angle -= Math.toRadians(cfg.slideControlDegrees) * Math.signum(moveStrafing);
          double hMotion = Math.sqrt(newVx * newVx + newVz * newVz);
          newVx = hMotion * -Math.sin(angle);
          newVz = hMotion *  Math.cos(angle);
      }
  }
  ```
- [ ] `cfg.slideControlDegrees` 필드 존재 확인 (`SmartMovingConfig.java` 에 추가 필요?).

### B-2. `slideControlDegrees` config 필드 신설 (원본 `_slideControlDegrees`)
- [ ] `SmartMovingConfig.java` 에 추가:
  ```java
  // 원본: SmartMovingConfig._slideControlDegrees (PositiveFactor, defaults 1F)
  public float slideControlDegrees = 1.0F;
  ```
- [ ] `loadFromProperties` 에 추가 (`getFloat(p, "move.slide.control.angle", slideControlDegrees)`).
- [ ] `saveToProperties` 에 추가 (`p.setProperty("move.slide.control.angle", String.valueOf(slideControlDegrees))`).
- [ ] 기본값 / property key 는 원본 `_slideControlDegrees = PositiveFactor("move.slide.control.angle").defaults(1F)` 1:1 (`research_sliding.md` 1-5 참조).

### B-3. 컴파일 + 빌드 확인
- [ ] `gradlew compileJava compileClientJava` 통과.

---

## Phase C — distanceWalkedModified 보호 (원본 L1578-L1581 + L1605-L1606) — 검증 후 결정

### C-1. 인게임 회귀 확인
- [ ] 슬라이딩 중 vanilla 걸음 사운드 발화 여부 확인.
- [ ] 크롤(엎드리기) 중 vanilla 걸음 사운드 발화 여부 확인.

### C-2. (회귀 발생 시) 매핑 추가
- [ ] 1.21.1 vanilla 의 `distanceTraveled` 변수명/접근 경로 grep + 확인 (`Entity.distanceMoved` / `LivingEntity.distanceTraveled` 등).
- [ ] `MixinPlayerEntity` 또는 `MixinEntity` 에 `beforeMoveEntity` / `afterMoveEntity` 후크 inject — 원본 1:1 :
  ```java
  // before move
  if (sm.isSliding || sm.isCrawling) {
      beforeDistanceWalkedModified = player.distanceMoved;
      player.distanceMoved = Float.MIN_VALUE;
  }
  // after move
  if (sm.isSliding || sm.isCrawling) {
      player.distanceMoved = beforeDistanceWalkedModified;
  }
  ```
- [ ] `beforeDistanceWalkedModified` 필드 신설 (`SmartMovingClientState.java`).

### C-3. (회귀 없으면) 매핑 생략
- [ ] 1.21.1 vanilla 가 자체적으로 슬라이딩 중 사운드 차단하면 매핑 불필요. 결과를 본 메모리에 기록.

---

## Phase D — 인게임 검증 (research_sliding.md §6 시나리오)

> 사용자 메모리 `feedback_integration_test.md` 참조 — 단독 포커스의 인게임 테스트는 통합 단계로 분리. **여기서는 크리티컬 시나리오 위주**로 검증.

### D-1. 크리티컬 시나리오
- [ ] **#1 진입**: 평지 sprint + grab + sneak → 슬라이딩 시작.
- [ ] **#2 종료 (sneak 떼기)**: 슬라이딩 중 sneak 떼면 즉시 정지 + crawl 전환. ★ Phase A fix 검증.
- [ ] **#3 strafe 회전**: 슬라이딩 중 좌/우 키 입력 → 방향 회전. ★ Phase B fix 검증.
- [ ] **#4 종료 (속도)**: 슬라이딩 중 자연 감속 → 정지 + crawl 전환. 임계값 정확 (= 원본 단위).

### D-2. 회귀 확인 시나리오
- [ ] **#5 SlideToHeadJumping**: 슬라이딩 중 살짝 낙하 (>0.05F) → 헤드점프 + 공기역학.
- [ ] **#6 큰 낙하 → crawl**: 슬라이딩 중 >3F 낙하 → crawl 준비.
- [ ] **#7 grab climbing 전환**: 슬라이딩 중 사다리/덩굴 → isSliding=false + isCrawling=true.
- [ ] **#8 점프 차단**: 슬라이딩 중 점프 키 → 무시.
- [ ] **#9 sprint 차단**: 슬라이딩 중 sprint 키 → 무시.
- [ ] **#10 WallSlide**: 벽 점프 + 수평 충돌 → WallUpSlide/WallHeadSlide noVertical.

---

## Phase E — 완료 처리 (완료)

### E-1. 메모리 등록
- [x] `project_sliding_complete.md` 작성 + `MEMORY.md` 인덱스 등록.

### E-2. 다음 단계 안내
- [x] 슬라이딩 **애니메이션** 작업 (별도 리서치/체크리스트) 대기.

---

## 추가 fix (인게임 검증 시 발견)

### F-1. 직접 진입 6-AND `!isSliding` 가드 제거
- [x] `SmartMovingClientState.java` L1930 — 원본 L2553 1:1 (가드 없음). 비행 → 착지 + grab+sneak hold → toSlidingOrCrawling 후 같은 tick 직접 진입 매치 → `tryJump(SLIDE_DOWN)` motion 부스트 → 종료 분기 미매치 → 진입 유지.

### F-2. 헤드점프 차징 차단 (`!sm.isSliding` 명시 가드)
- [x] `SmartMovingJumper.java` L443 — 슬라이딩 중 점프키 입력 → 헤드점프 차징 차단. 원본은 vanilla sprint 자동 종료 의존이지만 forward hold 시 차단 미작동 → 명시 가드로 보완. SlideToHeadJumping 자동 전환은 별경로라 영향 없음.

### F-3. ★ standupIfPossible(2) sneak raw key 사용 ★
- [x] `SmartMovingClientState.java` L3566 — `player.isSneaking()` → `MinecraftClient.options.sneakKey.isPressed()`. `MixinClientPlayerEntity.sm_isSneaking_ClientPlayer` inject 가 비행 직후 false 반환 → 분기 결정 잘못 → BUG. 메모리 `feedback_movementInput_vs_isSneaking.md` 패턴 정확 매치.

### F-4. debug log 추가 + 분석 + 제거
- [x] DEBUG_SLIDE flag + slogSlide helper 한시 추가 → 인게임 trace → log_temp.txt 분석 → F-3 근본 원인 5분 만에 확정.
- [x] fix 적용 + 인게임 검증 통과 후 debug log 코드 모두 제거.

---

## 작업 순서 요약

```
A (종료 조건 fix)  →  B (strafe 회전 fix)  →  C (걸음 사운드 검증)
                                                 ↓
                                          D (인게임 검증)
                                                 ↓
                                          E (완료 + 메모리 등록)
```

각 Phase 완료 후 직접 빌드 + 인게임 테스트 (사용자 직접 비교).
