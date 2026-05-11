# jump 차징 + sneak BUG 체크리스트

## Phase 1 — 진단 dump 추가
- [ ] tickMovement HEAD/h2/TAIL dump (`MixinClientPlayerEntity`).
- [ ] 자체 슬라이딩 발사 분기 진입 dump (`SmartMovingClientState` L2055~).
- [ ] SS-SlideStop 매치 dump (L2275~).
- [ ] fix #62 v2 push 시점 dump (L2337~).
- [ ] guard: `isSliding || isCrawling || jumpCharge>0 || headJumpCharge>0 || justEndedHeadJump || wasSliding (= 직전 frame)`.

## Phase 2 — 사용자 재현
- [ ] 사용자에게 시나리오 재현 요청.
- [ ] log_temp.txt 분석.

## Phase 3 — 원인 확정
- [ ] Frame N: 자체 슬라이딩 발사 매치 확인.
- [ ] Frame N+1: SS-SlideStop hSpd² 측정.
- [ ] Frame N+1: fix #62 v2 push 발동 확인.
- [ ] Frame N+2: POSE 갱신 timing.

## Phase 4 — fix 적용
- [ ] 가설 검증 후 fix.
- [ ] 회귀 검토.

## Phase 5 — 정리
- [ ] 디버그 로그 제거.
- [ ] 메모리 기록.
- [ ] 커밋.
