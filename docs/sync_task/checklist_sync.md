# 멀티플레이어 SM 동기화 — 작업 체크리스트

세션: 2026-05-05 작성 / 내일 작업 재개 시 사용
참조: `research_sync.md` (= 분석) + `bugs_sync.md` (= BUG 목록)

---

## fix-1 — dirty bit 검사 stop (= BUG-A + BUG-B 동시 해결)

### 1-1) 원본 송신 빈도 확인
- [ ] 원본 1.7.10 `SmartMovingPlayerBase.updateEntityActionState` 끝의 `writeEntityState()` 호출이 매 tick 무조건인지 dirty bit 인지 확인
  - 위치: `C:\Work\minecraft\porting\sm_original\` 안 source
  - grep: `writeEntityState\|lastSent\|isDirty`
  - 결과 메모: __________________

### 1-2) sendStatePacket dirty 검사 제거
- [ ] `SmartMovingClientState.java` L3860 `if (bits != lastSentBits) { ... lastSentBits = bits; }` →
  - 매 tick 무조건 송신 (= `if` 제거 + `lastSentBits` 갱신만 유지 또는 완전 제거)
  - 디버그 로그 (`[SYNC-DBG-C2S]`) 도 매 tick 출력되니 일단 임시로 dirty 일 때만 print 하도록 분기 (= 로그 폭증 방지)
- [ ] 빌드 검증 (`./gradlew compileClientJava`)

### 1-3) 매 tick 송신 시 부하 검토
- [ ] StatePayload 크기 = `int(4) + long(8) = 12 byte`. 1 player × 20 tick/s = 240 byte/s. 4 player 면 1 KB/s. 부하 무시 가능.
- [ ] 단, server relay 도 매 tick `(player_count - 1)` 명에 broadcast → 4 player 면 3 명 × 4 = 12 packet/tick. 미미.
- [ ] 결정: 그대로 매 tick 송신.

### 1-4) 인게임 검증
- [ ] rudals 가 먼저 server 접속 + 비행 시작 후 Tester2 접속 → Tester2 측에서 rudals 가 비행 자세로 표시되는지 확인
- [ ] 두 player 동시 비행/엎드리기/슬라이딩 등 시도 → 각각 상대 측에서 정상 자세
- [ ] log_temp.txt 검사:
  - [ ] `[SYNC-DBG-C2S]` 가 매 tick 출력 (= 매 tick 송신 확인) — 또는 dirty 시만 print
  - [ ] `[SYNC-DBG-RELAY] relayedTo` 가 정상 (= 다른 player 수)
  - [ ] `[SYNC-DBG-S2C]` 양쪽 client 매 tick 수신
  - [ ] `[SYNC-DBG-RENDER-ENTRY] viewer=X entity=Y SM(...)` — Y 의 SM state 가 Y 자기 client 와 일치

---

## fix-2 — stats source delta 기반 변경 (= BUG-D 해결)

### 2-1) 원본 stats 입력 확인
- [ ] 원본 `SmartStatistics.calculate` 가 받는 인자 확인
  - 위치: `C:\Work\minecraft\porting\sm_original\` 의 `SmartStatistics.java`
  - 우리 매핑 위치: `SmartMovingClientState.java` 의 stats field + `MixinPlayerEntityClient.sm_tickStatsForRemote`
- [ ] 원본이 motion (= velocity field) 사용인지 delta (= posX - lastTickPosX) 사용인지 확인
  - 결과 메모: __________________

### 2-2) sm_tickStatsForRemote 수정
- [ ] `MixinPlayerEntityClient.java` 의 sm_tickStatsForRemote inject 수정:
  ```java
  // 변경 전
  Vec3d vel = remote.getVelocity();
  sm.stats.calculate(0, 0, 0, vel.x, vel.y, vel.z, remote.getYaw());

  // 변경 후 (delta 기반)
  double dx = remote.getX() - remote.prevX;
  double dy = remote.getY() - remote.prevY;
  double dz = remote.getZ() - remote.prevZ;
  sm.stats.calculate(0, 0, 0, dx, dy, dz, remote.getYaw());
  ```
- [ ] `[SYNC-DBG-STATS]` 로그에서 `v=(...)` 가 0 이 아닌 값으로 변동하는지 확인 (= delta 기반 정상)
- [ ] 빌드 검증

### 2-3) 인게임 검증
- [ ] Tester2 측에서 rudals 가 걷기/달리기 시 팔다리 swing 정상
- [ ] rudals 측에서 Tester2 도 동일 확인

---

## fix-3 — bodyYaw force/fade remote 적용 (= BUG-C, Phase 2 수준)

### 3-1) static field player 별 분리 검토
- [ ] `smBodyYawActive`, `smBodyYawOverride` (= MixinPlayerEntityRenderer 의 mixin field — single instance) 가 player 별 분리 필요
- [ ] `smStandardFadeActive`, `smFallingFadeMode`, `smCrawlMode`, `smBodyYawActive_publicShared`, `smCachedBodyYawNaturalDeg`, `smCachedBodyYawLaggedDeg`, `smCachedAnimationProgress`, `smStandardBodyYawPrev`, `smStandardFadeTimePrev` (= SmartMovingClientState static field — JVM single)
- [ ] 모두 SmartMovingClientState 의 ★ 인스턴스 field ★ 로 이동
- [ ] capture / modifyArg 들이 sm 인스턴스 (= entity uuid 로 lookup) 의 field 사용
- [ ] ModifyArg 는 entity 인자 없음 → ThreadLocal 또는 `sm_currentRenderPlayer` (= MixinLivingEntityRenderer field) 활용

### 3-2) capture 의 remote 분기 추가
- [ ] `MixinPlayerEntityRenderer.sm_captureBodyYaw` L153 의 `if (!(player instanceof ClientPlayerEntity localPlayer)) return;` 제거
- [ ] sm 인스턴스를 player.getUuid() 로 lookup 후 분기 적용

### 3-3) force 값 sync (= packet 확장 또는 server 계산)
- [ ] StatePayload 또는 별도 payload 에 `currentHorizontalAngle`, `currentCameraAngle` 추가
- [ ] sender → server → other clients 로 force 값 sync
- [ ] 또는 receiver 측이 entity.getYaw / delta 로 자체 계산

### 3-4) 인게임 검증
- [ ] Tester2 측에서 rudals 의 비행/엎드리기 자세 회전이 자기 client 와 동일
- [ ] "몸통 앞뒤 반대" 보고 해소 확인

---

## 검증 후 — 디버그 로그 cleanup

- [ ] `[SYNC-DBG-C2S]`, `[SYNC-DBG-RELAY]`, `[SYNC-DBG-S2C]`, `[SYNC-DBG-STATS]`, `[SYNC-DBG-RENDER]`, `[SYNC-DBG-RENDER-ENTRY]`, `[SYNC-DBG-CAPTURE-YAW]`, `[SYNC-DBG-MODIFY-YAW]`, `[SYNC-DBG-MODIFY-HEAD]` 모두 제거
- [ ] `SmartMovingClient.syncDbgFrame`, `syncDbgShouldLog()`, `syncDbgEntityName()` helper 제거
- [ ] `MixinPlayerEntityModelClient.sm_syncDbgFrameCounter` 제거

---

## 최종 — Phase 1 완료 선언 + 커밋

- [ ] 사용자 "완전 로컬 플레이어랑 똑같은 퀄리티" 기준 충족 확인
- [ ] 사용자에게 Phase 1 완료 선언 부탁
- [ ] 한 번에 커밋: `feat(sync): multiplayer SM animation sync infrastructure (Phase 1)`
- [ ] 메모리 업데이트: `project_sync_phase1_complete.md` 생성

