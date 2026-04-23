# Focus #4 — 상태 전환 키 커맨드 조합 이상

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #4 일 때 진입.

---

## 진행 상황

**상태**: ⚪ 대기 (재현 케이스 수집 필요)

**현재 단계**: 재현 케이스 수집 대기

---

## 증상 (일반)

특정 키 조합(예: `ctrl(sneak) + grab + spacebar(jump)`)이 원본과 다른 동작을
발동시키거나, 아예 동작 안 함.

## 재현 케이스 (진입 전 필수 수집)

| # | 입력 조합 | 원본 기대 동작 | 실제 동작 | 영향 |
|---|----------|--------------|----------|------|
| 1 | sneak(hold) + jump(누름 유지) + jump 릴리즈 | 차지 점프 발동 | 바닐라 점프 | jumpCharge 미반영 |
| 2 | sprint + grab(hold) + jump | 헤드 점프 차지 | ? | ? |
| 3 | sneak + grab + spacebar | (확인 필요) | (확인 필요) | ? |

---

## 의심 지점 (사전 예상)

- `jumpKeyStartPressed` 엣지 감지 vs `jumpPending` (vanilla 가로채기) 이중 시스템 우선순위 충돌
- `sneakKeyStartPressed` / `sneakKeyStopPressed` 감지가 `jumpPending` 처리 후 클리어되는 문제
- `isJumpCharging` / `isHeadJumpCharging` 게이트 조건 (원본 `isGroundSprinting` / `isRunning` 와 비교)
- 차지 점프 해제(sneak 릴리즈 시 tryJump) 조건 원본 대조
- 벽 점프 더블클릭 타이머가 잘못 리셋되는 경우

---

## 관련 파일

- `SmartMovingClientState.tickEssential()` — sneakKey/jumpKey Start+Stop 엣지 감지
- `SmartMovingJumper.handleJumping()` — 차지/헤드/벽 점프 분기
- `SmartMovingJumper.updateWallJumpState()` — 벽점프 더블클릭 처리
- `SmartMovingClientState` IMPL-03 더블클릭 방향 점프 (leftJumpCount 등)
- `MixinLivingEntityClient.sm_jump` — vanilla jump() 가로채기

---

## 원자 단위 작업 목록 (재현 케이스 확보 후 채움)

_(비어있음)_

---

## 완료 전 검증 체크리스트

- [ ] 각 재현 케이스가 원본 기대 동작대로 발동됨
- [ ] 키 엣지 감지 타이밍이 원본 Button.update() 와 논리적으로 등가
- [ ] 이중 시스템(`jumpPending` vs `jumpKeyStartPressed`) 간 우선순위 정리
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
