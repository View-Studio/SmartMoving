# Focus #2 — 스마트무빙 상태 이상

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #2 일 때 진입.

---

## 진행 상황

**상태**: ⚪ 대기 (재현 케이스 수집 필요)

**현재 단계**: 재현 케이스 수집 대기

---

## 증상 (일반)

`isCrawling`/`isDipping`/`isSwimming_sm`/`isDiving`/`isClimbing`/`isCrawlClimbing`/
`isCeilingClimbing`/`isSliding`/`isHeadJumping`/`isFast`/`isSlow`/`wasCrawling_st`/
`contextContinueCrawl` 등이 **잘못된 시점에 true/false** 로 설정됨.

## 재현 케이스 (진입 전 필수 수집)

아래 표를 플레이테스트로 채워야 작업 가능:

| # | 상황 | 예상 상태 | 실제 상태 | 영향 |
|---|------|----------|----------|------|
| 1 | (예: 얕은 물에서 sneak) | isCrawling=true | isCrawling=false | 크롤링 전환 안 됨 |

재현 케이스가 없는 상태에서는 **진입 금지** (PORTING_RULES 원칙 4).

---

## 의심 지점 (사전 예상)

- `canStandUp` 근사로 인한 크롤링 유지/해제 오판
- `wasCrawling_st` / `wasClimbCrawling` / `wasSneaking` 저장 타이밍
- `isSmall` 계산 위치가 다른 상태 변경보다 앞/뒤
- `updateSwimState` 의 offset 경계값(0.65/0.6/0.55/1.9) 판정
- `fromSwimmingOrDiving` 진입 조건 정확성

---

## 관련 파일

- `SmartMovingClientState.tickEssential()` — 주요 상태 전환 허브
- `SmartMovingSwimmer.updateSwimState()` — isDipping/isSwimming_sm/isDiving 3분류
- `SmartMovingJumper.handleJumping()` — isHeadJumping 설정
- `SmartMovingClimber` — isClimbing/isCrawlClimbing/isCeilingClimbing

---

## 원자 단위 작업 목록 (재현 케이스 확보 후 채움)

_(비어있음)_

---

## 완료 전 검증 체크리스트

- [ ] 재현 케이스 표의 모든 행이 "예상 == 실제" 로 검증됨
- [ ] 원본 해당 상태 계산 로직을 리서치 파일에서 재확인
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
