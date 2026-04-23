# Focus #1 — 애니메이션 망가짐 / 이상함

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #1 일 때 진입.

---

## 진행 상황

**상태**: ⚪ 대기 (재현 케이스 수집 필요)

**현재 단계**: 재현 케이스 수집 대기

---

## 증상 (일반)

특정 상태의 애니메이션이 원본과 **시각적으로 다름**. 팔/다리/몸통 각도 이상,
회전축 방향 이상, 또는 애니메이션이 정지/떨림.

## 재현 케이스 (진입 전 필수 수집)

| # | 상태 | 원본 모습 | 실제 모습 | 차이 |
|---|------|----------|----------|------|
| 1 | isCrawling | (원본 스샷/동영상) | (현재 스샷) | 팔 각도 이상 |

상태 예시: 수영/잠수/사다리 클라이밍/넝쿨 클라이밍/크롤링/크롤링 중 이동/
슬라이딩/헤드점프/비행/천장 클라이밍/벽 점프/로프 슬라이딩.

---

## 의심 지점 (사전 예상)

- `bipedOuter` 계층 부재로 body/limb 분리 근사 오역 (구조적 차이)
- 팔/다리 각도 공식 이식 시 sin/cos 분해 근사 사용 (메모리 기록된 실패 패턴)
- `feetDistSideFactor` / `handsDistSideFactor` 등 상수 값 차이
- 애니메이션 pitch/yaw/roll 회전 순서(YZX vs XYZ) 오류
- `setAnglesYZX` 헬퍼가 원본 ModelRotationRenderer YZX 회전 순서와 정확히 일치하는지

---

## 관련 파일

- `MixinPlayerEntityModelClient.java`:
  - `sm_animateClimbing` (사다리/넝쿨)
  - `sm_animateCrawling`
  - `sm_animateSwimming`
  - `sm_animateDiving`
  - `sm_animateCeilingClimbing`
  - `sm_animateSliding`
  - `sm_animateHeadJumping`
  - `sm_animateFlying`
  - `sm_animateAngleJumping` (방향 점프)

---

## 원자 단위 작업 목록 (재현 케이스 확보 후 채움)

_(비어있음)_

---

## 완료 전 검증 체크리스트

- [ ] 재현 케이스의 각 상태 애니메이션이 원본과 시각적으로 일치 (근사 한도 내)
- [ ] 원본 `SmartMovingModel.setRotationAngles()` 해당 상태 블록을 리서치 파일에서 재확인
- [ ] 각도 공식이 sin/cos 분해 근사가 아닌 원본 공식 그대로
- [ ] 회전 순서(YZX 등) 적용 확인
- [ ] 컴파일 성공

---

## 작업 기록

_(비어있음)_

---

## 신규 발견

_(비어있음)_

---

## 잔여 / 후속

_(비어있음)_
