# 헤드점프 멀티 BUG fix 체크리스트 (2026-05-13)

리서치 = `docs/research_headjump_multi.md`.

---

## Phase 1 — BUG 2 (1칸 down) 진단 + fix (사용자 합의: 먼저)

### 1.1 진단 dump 추가

- [ ] `[HJ-MULTI-SELF]` SmartMovingClientState.handleEntities (또는 PlayerEntity.tick TAIL inject) — wasHJ/isHJ transition + entity.y BEFORE/AFTER standupIfPossible.
- [ ] `[HJ-MULTI-SERVER-RECV]` SmartMovingServer.processStatePacket — newHeadJumping bit transition + player.getY().
- [ ] `[HJ-MULTI-SERVER-BCAST]` PlayerEntity.baseTick TAIL (server) — player.getY() + isHeadJumping broadcast 직전.
- [ ] `[HJ-MULTI-REMOTE-PKT]` SmartMovingClient L100 직전/직후 — wasHJ → isHJ transition + remote.getY() + srvY + bti.
- [ ] `[HJ-MULTI-REMOTE-TICK]` PlayerEntity.tick TAIL (= OtherClientPlayerEntity 한정 검사) — remote.getY() / srvY / bti 매 tick.

### 1.2 사용자 인게임 dump 수집

- [ ] 사용자 인게임 재현: 다른 player 가 헤드점프 수행 → 착지 시 1m down 시각 발생 frame 확인.
- [ ] `docs/log_temp.txt` 에 dump 붙여넣기 받음.
- [ ] dump 분석 — self.y 변화량 (= +1m 인지 확인) + remote.y vs srvY 매트릭스 + bti 값.

### 1.3 root cause 확정

- [ ] self side 종료 시 entity.y 정확 변화량 확정 (= +1m 인지 다른 값인지).
- [ ] remote 측 시각 1m down frame 의 정확 timing (= SM state packet 도착 후 ~1 tick 동안 잔존).
- [ ] BUG-7/15/D 와 동일 메커니즘 검증.

### 1.4 fix 적용

- [ ] `SmartMovingClient.java` registerClientReceivers — `wasHeadJumping` 저장 (L77 이전 영역).
- [ ] 헤드점프 종료 분기 추가 — slide 종료 분기 (L204-220) 패턴 1:1:
  - [ ] `wasHJ && !target.isHeadJumping && AbstractClientPlayerEntity remoteHJ && !ClientPlayerEntity` 가드.
  - [ ] `newY = remote.getY() + 1.0` setPos.
  - [ ] `lastRenderY/prevY` 동기화.
  - [ ] `sm_setServerY(newY) + sm_setBodyTrackingIncrements(0)` lerp cancel.
- [ ] 분기 매트릭스 충돌 검증:
  - [ ] 헤드점프 → 슬라이딩 직접 전환 (= fox movement 종료 등) 시 어느 분기 매치? `wasFlying && !target.isFlying` (= 아님), `wasSliding && !target.isSliding` (= isSliding 잔존이면 아님), `wasHJ && !target.isHJ` (= 매치).
  - [ ] 헤드점프 → crawl 자동 전환 시 (= fix #41) 동일 검토.

### 1.5 회귀 검증 (사용자 인게임)

- [ ] BUG 2 시나리오 — 다른 player 헤드점프 착지 1m down 사라짐.
- [ ] **회귀 차단 시나리오** — 사용자 명시 인게임 테스트:
  - [ ] self 헤드점프 진입/유지/종료 single-player (= project_headjump_final).
  - [ ] self 슬라이딩 진입/종료 (= project_sliding_complete).
  - [ ] 다른 player 슬라이딩 (= BUG-15) — 1m up/down 회귀 없음.
  - [ ] 다른 player ICC EXIT (= BUG-7/B) — 1m down 회귀 없음.
  - [ ] 다른 player 비행 종료 standing/crawl/slide (= BUG D/E) — visual jump/잠수 회귀 없음.
  - [ ] 자체 슬라이딩 fire 후 헤드점프 (= 여우무빙) — 다른 player 시점 정상.

### 1.6 dump 제거 + 메모리 + 커밋

- [ ] 진단 dump 일괄 제거.
- [ ] `memory/project_headjump_multi_bug2_fix.md` 작성 — root cause + fix + 회귀 검토.
- [ ] `MEMORY.md` 인덱스 추가.
- [ ] 커밋: `fix(headjump-multi): remote 헤드점프 착지 1칸 down fix`.

---

## Phase 2 — BUG 1 (끊김) 진단 + fix

### 2.1 진단 dump 추가

- [ ] `[HJ-MULTI-REMOTE-FADE]` MixinPlayerEntityRenderer L741 분기 안 — sm.isHeadJumping/smHeadJumpTiltX_prev/thetaTarget/sm.stats.currentVerticalAngle 매 frame.
- [ ] `[HJ-MULTI-SELF-FADE]` 동일 위치, self 측 비교.
- [ ] currentVerticalAngle 갱신 위치 모두 grep — self 매 tick 측정 코드 + remote 측 측정/broadcast 여부.

### 2.2 사용자 인게임 dump 수집

- [ ] 사용자 인게임 재현: 다른 player 헤드점프 → 시각 끊김 frame 확인.
- [ ] dump 붙여넣기.

### 2.3 root cause 확정 (후보 3개 중)

- [ ] (a) currentVerticalAngle stale — remote.stats.currentVerticalAngle 매 tick 갱신 여부 검증.
- [ ] (b) isHeadJumping transition timing — self 와 remote 의 enter/exit frame timestamp 차이 측정.
- [ ] (c) PlayerEntityModel single instance leak — 다른 player 분기 진입/종료 frame 의 prev/curr 충돌 검증.

### 2.4 fix 적용 (root cause 별)

- [ ] (a) 시 — remote 측 stats 측정 path 추가 (또는 packet 으로 broadcast).
- [ ] (b) 시 — fade prev reset 가드 조정 (= packet lambda 안 isHeadJumping transition 시 prev 정합 식 추가).
- [ ] (c) 시 — animateHeadJumping 안 set/reset 식 다른 player 충돌 가드 추가.

### 2.5 회귀 검증

- [ ] BUG 1 시나리오 — 다른 player 헤드점프 부드러움 회복.
- [ ] **회귀 차단** — 사용자 명시:
  - [ ] self 헤드점프 시각 (= 모든 fix #79~#86 영역).
  - [ ] 자체 슬라이딩 fire → 헤드점프 (= 여우무빙) 시각.
  - [ ] 슬라이딩 시각 (= project_sliding_animation_complete).
  - [ ] 1인칭 손 (= project_bug_a_first_person_hand_fix) — BUG A 회귀 없음.

### 2.6 dump 제거 + 메모리 + 커밋

- [ ] 진단 dump 제거.
- [ ] `memory/project_headjump_multi_bug1_fix.md` 작성.
- [ ] `MEMORY.md` 인덱스 추가.
- [ ] 커밋: `fix(headjump-multi): remote 헤드점프 끊김 fix`.

---

## 회귀 차단 일괄 명령어 (build + 인게임 매뉴얼 테스트 전 확인용)

- [ ] `./gradlew build` 통과.
- [ ] 사용자 인게임 — 위 회귀 시나리오 모두 통과.
