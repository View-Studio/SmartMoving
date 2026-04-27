# Falling 분기 swing arm 매핑 체크리스트

> 리서치: `docs/research/research_falling_swing.md`
> 핵심: ① anySmState 에 isFalling 추가 (body.yaw=0 reset 작동)
>       ② preferred arm 의 setAnglesXZY skip (vanilla swing 보존)

---

## 0. 사전 확인

- [x] vanilla animateArms 가 swing>0 시 body.yaw 흔드는 것 확인 (research §1)
- [x] falling 이 anySmState 미포함이라 reset 인프라 미작동 확인 (research §2)
- [x] 비행 swing 패턴 (preserveRight/Left) 1:1 차용 가능 확인 (research §3)
- [x] falling 은 setupTransforms X 회전 없음 → preCancel* 불필요 확인

## 1. anySmState 에 isFalling 포함

- [ ] **1-1**. `sm_setAngles` 의 reset 가드 (L91-94 근처) 에 falling 진입 조건 통합.
  - 진입 조건은 if-else 의 falling 가드 (L162-166) 와 동일해야 함.
  - 가독성을 위해 `boolean isFallingForReset` 변수 분리, anySmState 에 OR.

## 2. sm_animateFalling 에 player 파라미터 + preserve 패턴

- [ ] **2-1**. 시그니처 변경:
  `(SmartMovingClientState sm)` → `(SmartMovingClientState sm, ClientPlayerEntity player)`.
- [ ] **2-2**. 호출처 (`sm_setAngles` L168) 업데이트:
  `sm_animateFalling(sm)` → `sm_animateFalling(sm, player)`.
- [ ] **2-3**. 본체에 preserve 변수 추가:
  ```java
  float swing = player.handSwingProgress;
  Arm preferredArm = player.getMainArm();
  boolean preserveRight = swing > 0F && preferredArm == Arm.RIGHT;
  boolean preserveLeft  = swing > 0F && preferredArm == Arm.LEFT;
  ```
- [ ] **2-4**. setAnglesXZY 호출을 preserve 가드로 감쌈:
  ```java
  if (!preserveRight) setAnglesXZY(rightArm, 0f, rYaw, rRoll);
  if (!preserveLeft)  setAnglesXZY(leftArm,  0f, lYaw, lRoll);
  ```
- [ ] **2-5**. 다리 / 머리 / 몸통 처리는 **변경 없음** (현재 코드 유지).

## 3. 빌드 검증

- [ ] **3-1**. `./gradlew compileJava compileClientJava` 통과.

## 4. 코드 리뷰 (1:1 정확성)

- [ ] **4-1**. `sm_animateFlying` 의 preserve 패턴과 falling 매핑이 동일 구조인지 비교.
- [ ] **4-2**. preCancel* 미적용 — 의도적 (falling setupTransforms X 회전 없음). 주석으로 명시.
- [ ] **4-3**. 체크리스트 1-1 의 `isFallingForReset` 가 if-else 진입 조건 (L162-166) 과 일치 확인.

## 5. 통합 테스트 (deferred)

- [ ] **5-1**. (deferred) 인게임: 낙하 중 휘두름 → 몸통 정지, 오른팔만 휘두름.
- [ ] **5-2**. (deferred) 왼손잡이 (mainArm=LEFT) 검증: 왼팔 휘두름 + 오른팔 falling 자세.

## 6. 메모리 갱신

- [ ] **6-1**. 본 작업 결과를 비행 swing 메모리에 합치거나 별도 항목 등재 검토.
  - 후보: "낙하 swing 매핑 = 비행과 동일 패턴 (preserve preferred arm + body.yaw reset),
    단 부모 X 회전 cancel 불필요".

## 7. 커밋

- [ ] **7-1**. `fix(falling): swing 시 몸통 cancel + preferred arm 보존` 패턴 commit.
