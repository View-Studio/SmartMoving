# 비행 swing 비-preferred arm pivot reset 체크리스트

> 리서치: `docs/research/research_flying_swing_left_arm.md`
> 핵심: `sm_animateFlying` swing 블록에 비-preferred arm pivotX/Z 의 vanilla 기본값 복원 추가.

---

## 0. 사전 확인

- [x] vanilla animateArms 가 swing 시 양 팔 pivotX/Z 변경 확인 (research §1)
- [x] SM 코드에서 `(rightArm|leftArm)\.pivot[XZ]` grep = No matches (다른 분기 충돌 없음)
- [x] 비행 swing 매핑 자체는 건드리지 않음 (preCancel*, fade lerp, preserve 패턴 그대로)

## 1. sm_animateFlying swing 블록에 pivot reset 추가

- [ ] **1-1**. `sm_animateFlying` 의 `if (swing > 0F)` 블록 안에 비-preferred arm 의
  `pivotX/pivotZ` 를 vanilla setAngles Step 4 기본값으로 복원하는 코드 추가.
  - rightArm: pivotX = -5, pivotZ = 0
  - leftArm: pivotX = +5, pivotZ = 0
  - preferred arm 은 미변경 (vanilla swing 효과 보존).

## 2. 빌드 검증

- [ ] **2-1**. `./gradlew compileJava compileClientJava` 통과.

## 3. 코드 리뷰

- [ ] **3-1**. 기존 비행 swing 매핑 (preCancel*, fade, preserve) 건드리지 않음 — 새 줄 추가만 확인.
- [ ] **3-2**. preferred arm 의 pivot 변경 없음 (vanilla swing 효과 보존).

## 4. 통합 테스트 (deferred)

- [ ] **4-1**. (deferred) 비행 + 휘두름 → 왼팔 정지, 오른팔 swing 그대로.
- [ ] **4-2**. (deferred) 왼손잡이 검증 (preferredArm=LEFT).

## 5. 메모리 갱신

- [ ] **5-1**. 비행 swing 메모리 갱신 검토 (preferred arm pivot 보존 + 비-preferred arm reset 패턴).

## 6. 커밋

- [ ] **6-1**. `fix(flying): swing 시 비-preferred arm pivot reset` 패턴 commit.
