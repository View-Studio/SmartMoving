# 헤드점프 착지 모서리 잠김 BUG 체크리스트

## Phase 1 — 진단 로그 추가
- [ ] `MixinClientPlayerEntity.tickMovement` HEAD/TAIL dump: SM state + 박스 + entity 위치 + heightOffset + vY.
- [ ] `SmartMovingClientState.standupIfPossible` 분기 매치 dump (= tryLanding / groundClose / standUpPossible / sneakPressed / grabPressed + 결과).
- [ ] `SmartMovingClientState.handleCrash` 분기 dump.
- [ ] guard: `isHeadJumping || wasHeadJumping || justEndedHeadJump || (직전 frame any)` — 짧은 catch.

## Phase 2 — 사용자 재현
- [ ] 사용자에게 시나리오 재현 요청.
- [ ] log_temp.txt 분석.

## Phase 3 — 원인 확정
- [ ] 모서리 frame 의 box / entity.y / heightOffset 확인.
- [ ] standupIfPossible 어느 분기 매치 확인.
- [ ] 정상 시나리오 vs BUG 시나리오 dump 비교.

## Phase 4 — fix 적용
- [ ] 가설 검증 후 fix.
- [ ] 회귀 검토.
- [ ] 인게임 검증.

## Phase 5 — 정리
- [ ] 디버그 로그 제거.
- [ ] 메모리 기록.
- [ ] 커밋.
