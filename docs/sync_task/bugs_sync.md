# 멀티플레이어 SM 동기화 — 사용자 보고 BUG 목록

세션: 2026-05-05 (Phase 1-A 후속)
배경: 원본 1.7.10 SmartMoving 은 server 가 다른 player 의 SM state 를 relay 해서
멀티플레이어에서도 비행/엎드리기/슬라이딩 등 자세가 그대로 보임. 우리 1.21.1 매핑은
일부만 sync 되고 자세 매핑이 거의 작동 안 함.

---

## ✅ 해결된 BUG (= 이번 세션 처리)

### BUG-CONFIG-1 — 한 player 의 config 변경이 모든 player 에 적용
- **사용자 보고**: "스마트무빙 컨피그가 한명이 변경해도 전체 적용이 되는거 같던데"
- **사용자 의도**: "콘피그는 그냥 딱 자기거 끄는거야. 자기거 끄면 본인+멀티 상대에게 꺼진걸로 취급. 내가 껐다고 모든 사람 꺼지는게 아님"
- **root cause**: server `SmartMovingServer.initialize` + `broadcastConfig` 가 무조건 broadcast → 모든 client `Config = SERVER_CONFIG` 강제.
- **fix**: server-side broadcast stop (`initialize` + `broadcastConfig` no-op) + client `processConfigContentPacket` 가 `Config` 미변경.

### BUG-CONFIG-2 — a 가 config off 시 a 측에서 모두 vanilla
- **사용자 보고**: "a가 콘피그를 off 로 했을 때 a 클라이언트에서 모두가 꺼진거 처럼 보여. 다른 클라에서는 a만 꺼진거 처럼 잘보이는데"
- **root cause**: render-path 의 `cfg.enabled` 검사가 자기 client cfg → a disabled 시 자기 client 측 모든 player render 의 SM 분기 차단.
- **fix**: helper `SmartMovingClient.isSmRenderEnabled(entity)` — self → Config.enabled / remote → 항상 true. 6 site 적용.

---

## ❌ 미해결 BUG (= 내일 작업 대상)

### BUG-A — animation sync 거의 안 됨
- **사용자 보고**: "에니메이션은 동기화가 거의 아예 안되어있다고 생각하면됨. 완전 로컬 플레이어랑 똑같은 퀄리티로 에니메이션이 나와야됨"
- **분석 — log_temp.txt 03:40:21~30 시나리오**:
  - 03:40:21: rudals 비행 시작 → `[SYNC-DBG-C2S] sender=rudals fly=true` 송신
  - 03:40:21: server 측 `[SYNC-DBG-RELAY] sender=rudals relayedTo=0` ★ Tester2 미접속 ★
  - 03:40:23: Tester2 접속 + 비행 송신 → `relayedTo=1` (= rudals 받음)
  - 03:40:23: rudals 가 Tester2 비행 packet 수신 — `[SYNC-DBG-S2C] receiver=rudals entityName=Tester2 fly=true`
  - 03:40:24+: rudals 측에서 entity=Tester2 SM(fly=**true**) ★ 정상 ★
  - 03:40:30: Tester2 측에서 entity=rudals SM(fly=**false**) ★ vanilla 잔존 ★
- **root cause**: `SmartMovingClientState.sendStatePacket` (L3860) 의 `if (bits != lastSentBits)` dirty bit 검사 →
  - rudals 가 비행 시작 시 1 회만 송신 (= 그 시점 Tester2 미접속 → `relayedTo=0`)
  - 비행 유지 중 bits 변경 없음 → 다시 송신 안 함
  - Tester2 가 늦게 접속해도 영영 못 받음 → Tester2 측에서 rudals 가 vanilla 처럼 보임

### BUG-B — 먼저 접속한 player 가 늦게 접속한 사람 측에서 한동안 vanilla
- **사용자 보고**: "먼저 서버 접속한 사람의 스마트무빙 동기화가 이후 접속한사람하고 좀 오래동안 안됨. 정확히는 먼저 들어온 플레이어가 어느정도까지 바닐라 모드인거 처럼 보임"
- **root cause**: BUG-A 와 동일 (= dirty bit 검사). late join player 가 기존 player 의 latest state 를 못 받음.
- **참고**: 사용자는 "후순위" 라고 했지만, 실제로는 BUG-A 와 동일 root cause 라 같이 fix.

### BUG-C — Tester2 측에서 rudals 몸통 앞뒤 반대
- **사용자 보고**: "이상한게 tester2에서 rudals의 몸통 앞뒤가 반대로 보임"
- **root cause** (추정): `MixinPlayerEntityRenderer.sm_captureBodyYaw` HEAD L153 `if (!(player instanceof ClientPlayerEntity localPlayer)) return;` →
  - remote player 시 force/fade 분기 미진입 → `smBodyYawActive=false` + `smStandardFadeActive=false` 유지 (= reset 직후 값)
  - `sm_modifyBodyYaw` 가 mode=VANILLA → vanilla bodyYaw 그대로
  - 그러나 bodyYaw force 가 자기 stats 의 `currentHorizontalAngle` / `currentCameraAngle` 의존 → remote 의 force 값이 packet 으로 sync 안 됨
- **fix 방향** (Phase 2): packet 확장으로 force 값 sync + remote 분기 추가.

### BUG-D — 다른 player 의 팔다리 swing 안 흔들림 (보조 BUG)
- **로그 증거**: `[SYNC-DBG-STATS] remote=rudals d=(...) v=(0.000,0.000,0.000)` ★ 매 tick velocity 0 ★
- **root cause**: `MixinPlayerEntityClient.sm_tickStatsForRemote` 가 `entity.getVelocity()` 사용. vanilla 1.21.1 server 가 다른 player 의 velocity 를 sync 안 함 → 항상 (0,0,0).
- **결과**: stats 입력 0 → currentSpeed=0, totalDistance=0 → `MixinLivingEntityRenderer.sm_modifyLimbSwing/Amount` 의 SM stat 출력 0 → 다른 player 의 팔다리 swing 안 함.
- **fix 방향**: stats source 를 `prevX/getX delta` 기반으로 변경.

---

## 참고 — 디버그 로그 자료

- `docs/log_temp.txt` (= 사용자 영구 로그 파일):
  - L1-868: Tester2 client log
  - L869-1342: rudals client log
  - L1343+: Server log
- 핵심 line: 869 (rudals 비행 송신) / 900 (rudals receive Tester2) / 1349 (relayedTo=0 BUG 증거) / 187 (Tester2 측 rudals SM 모두 false) / 923+ (rudals 측 Tester2 fly=true 정상).

