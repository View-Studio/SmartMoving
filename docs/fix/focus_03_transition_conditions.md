# Focus #3 — 상태 전환 조건 이상

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #3 일 때 진입.

---

## 진행 상황

**상태**: ⚪ 대기 (재현 케이스 수집 필요)

**현재 단계**: 재현 케이스 수집 대기

---

## 증상 (일반)

특정 조합에서 상태가 원본과 **다른 타이밍**에 전환됨. 또는 **전환이 일어나지 않음**.

## 재현 케이스 (진입 전 필수 수집)

| # | 상황 | 원본 기대 전환 | 실제 전환 | 영향 |
|---|------|--------------|----------|------|
| 1 | (예: sprint + sneak + onGround) | isSliding=true | 아무 일도 없음 | 슬라이딩 진입 안 됨 |

---

## 의심 지점 (사전 예상)

- `wantCrawl` pre-compute 블록이 IMPL-01 실제 전환 전에 실행되어 `isCrawling` 재계산 순서 문제
- `wantSlide` 조건 (`player.isSneaking()`) 이 `isSlow` 의존 → 순환
- `wantWallJumping` 자기참조 식의 이전 틱 값 사용
- `canCrawl` 게이트 조건이 원본과 정확히 일치하는지
- `isFast` (`grab && isSprinting`) 계산 타이밍 vs sprint 실제 상태

---

## 관련 파일

- `SmartMovingClientState.tickEssential()` (IMPL-01 크롤링 / IMPL-02 슬라이딩)
- `SmartMovingSwimmer` 수영↔크롤링 전환
- `SmartMovingJumper.updateWallJumpState()` + `handleWallJumping()`
- `SmartMovingClimber` 클라이밍 진입/해제

---

## 원자 단위 작업 목록 (재현 케이스 확보 후 채움)

_(비어있음)_

---

## 완료 전 검증 체크리스트

- [ ] 각 재현 케이스가 원본 기대대로 전환됨
- [ ] 전환 조건을 구성하는 모든 플래그(wantCrawl/mustCrawl/canCrawl/wantSlide 등)가 원본과 일치
- [ ] 순환 참조 지점(sm_isSneaking → isSlow → player.isSneaking())이 해결됨
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
