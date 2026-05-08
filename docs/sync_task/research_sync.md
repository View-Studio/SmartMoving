# 멀티플레이어 SM 동기화 — 분석 + Root Cause

세션: 2026-05-05 (Phase 1-A 후속)

## 1. 현재 상태 (= Phase 1-A 까지 적용된 것)

### 1-A) sync 인프라
- **C2S (sender → server)**: `MixinClientPlayerEntity.sm_sendStatePacket` (tickMovement TAIL) → `SmartMovingClientState.sendStatePacket` → `StatePayload(entityId, bits)` 송신.
- **S2C relay (server → other clients)**: `SmartMoving.SERVER` (= `ServerPlayNetworking.registerGlobalReceiver(StatePayload.ID)`) → 수신 시 sender 외 모든 player 에 broadcast.
- **수신 (client)**: `SmartMovingClient.registerClientReceivers` → `entityId` 로 entity 찾음 + `SmartMovingClientState.get(entity.getUuid()).processStatePacket(state)`.

### 1-B) Phase 1-A 작업 (이번 세션 적용)
- `MixinPlayerEntityModelClient.sm_setAngles` HEAD/TAIL: `instanceof ClientPlayerEntity` → `instanceof AbstractClientPlayerEntity` (= local + remote)
- `MixinPlayerEntityRenderer.sm_setupTransforms` TAIL: ClientPlayerEntity 가드 제거
- `MixinLivingEntityRenderer.sm_modifyLimbSwing/Amount`: `sm_currentRenderPlayer` 기반 (= 모든 player)
- `flyingCreative` 기준: `player.getAbilities().flying` (= datatracker 미동기) → `sm.isFlying` (= server-relay sync)
- `MixinPlayerEntityClient.sm_tickStatsForRemote`: PlayerEntity.tick TAIL → remote stats 매 tick 계산
- `MixinPlayerEntityRenderer.sm_captureBodyYaw`: ClientPlayerEntity 가드 그대로 유지 (= local-only force/fade)

### 1-C) BUG-CONFIG 추가 fix (이번 세션)
- server-side broadcast stop (`SmartMovingServer.initialize`/`broadcastConfig` no-op)
- client `processConfigContentPacket` 가 `Config` 미변경
- helper `SmartMovingClient.isSmRenderEnabled(entity)` — render-path 의 `cfg.enabled` 검사를 self-only 로 한정 (6 site)

---

## 2. log_temp.txt 분석 (= 03:40:21~30 시나리오)

### File 구조 (= 사용자가 세 source 합본)
- L1-868: Tester2 client log
- L869-1342: rudals client log
- L1343+: Server log

### 시간 순 이벤트 재구성

| 시각 | 출처 | 이벤트 | 의미 |
|---|---|---|---|
| 03:40:21 | rudals client (L869) | `[SYNC-DBG-C2S] sender=rudals fly=true` | rudals 비행 시작 — 자기 packet 송신 |
| 03:40:21 | server (L1349) | `[SYNC-DBG-RELAY] sender=rudals relayedTo=0` | ★ ★ ★ 아직 다른 player 미접속 ★ ★ ★ |
| 03:40:23 | Tester2 client (L2) | `[SYNC-DBG-C2S] sender=Tester2 fly=true` | Tester2 비행 송신 |
| 03:40:23 | server (L1353) | `[SYNC-DBG-RELAY] sender=Tester2 relayedTo=1` | rudals 한 명에 relay |
| 03:40:23 | rudals client (L900) | `[SYNC-DBG-S2C] receiver=rudals entityName=Tester2 fly=true` | rudals 가 Tester2 비행 packet 받음 |
| 03:40:24+ | rudals client (L923+) | `[SYNC-DBG-RENDER-ENTRY] viewer=rudals entity=Tester2 SM(fly=true)` | ★ rudals 측에서 Tester2 정상 ★ |
| 03:40:30 | Tester2 client (L187) | `[SYNC-DBG-RENDER-ENTRY] viewer=Tester2 entity=rudals SM(fly=false)` | ★ Tester2 측에서 rudals vanilla ★ |

### 가장 결정적 증거
**L1349 `relayedTo=0`** — rudals 의 비행 packet 이 server 에 도달했지만 ★ 그 시점 Tester2 미접속 ★ 이라 0 명에 relay. 그 후 **rudals 의 비행 state 가 변경되지 않으므로 dirty bit 검사로 다시 송신 안 함** → Tester2 가 영영 못 받음.

---

## 3. Root Cause 정리

### root cause 1 (= 가장 큰 BUG) — `sendStatePacket` 의 dirty bit 검사

`SmartMovingClientState.sendStatePacket` L3860:
```java
if (bits != lastSentBits) {
    ClientPlayNetworking.send(new SmartMovingNetwork.StatePayload(player.getId(), bits));
    lastSentBits = bits;
}
```

**문제**:
- 안정 상태 (= 같은 SM state 유지) 에서는 bits 변경 없음 → 송신 stop
- 새로 접속한 player 가 기존 player 의 latest state 못 받음
- 원본 1.7.10 SmartMovingPlayerBase.updateEntityActionState 끝 `writeEntityState()` 는 **매 tick 무조건 송신** (확인 필요).

**fix**:
- (A) dirty bit 검사 stop — 매 tick 송신. 단점: 네트워크 부하 (= 12 byte / tick / player). 그러나 client 수 작으면 미미.
- (B) server-side late join broadcast — player join 시 기존 모든 player 의 latest SM state 를 새 player 에게 송신. dirty bit 유지 가능. 더 복잡.
- ★ 결정 ★ : (A) 가 단순 + 원본 1:1 + late join 자동 해결.

### root cause 2 (= 보조 BUG) — `entity.getVelocity()` 가 remote 0

`MixinPlayerEntityClient.sm_tickStatsForRemote`:
```java
net.minecraft.util.math.Vec3d vel = remote.getVelocity();
sm.stats.calculate(0, 0, 0, vel.x, vel.y, vel.z, remote.getYaw());
```

**문제**:
- vanilla 1.21.1 server 가 다른 player 의 velocity field 를 sync 안 함 → `getVelocity()` 항상 (0, 0, 0)
- 로그 확인: 모든 `[SYNC-DBG-STATS] remote=rudals` 의 `v=(0.000, 0.000, 0.000)` (= velocity 0)
- 그러나 `d=(...)` (= prevX/getX delta) 는 정상 변동 (= 위치 보간 작동)

**fix**:
- stats 입력을 `prevX/getX delta` 기반으로 변경. delta = (entity.getX - entity.prevX) 등.
- 또는 entity.getX, prevX 둘 다 read 해서 stats 에 적절히 매핑.

### root cause 3 (= Phase 2 수준) — bodyYaw force/fade remote 미적용

`MixinPlayerEntityRenderer.sm_captureBodyYaw` L153:
```java
if (!(player instanceof ClientPlayerEntity localPlayer)) return;
```

**문제**:
- remote player render 시 force/fade 분기 미진입 → `smBodyYawActive=false` + `smStandardFadeActive=false` 잔존
- `sm_modifyBodyYaw` 가 mode=VANILLA → 회전 force/fade 미적용
- 사용자 보고 "Tester2 에서 rudals 몸통 앞뒤 반대" — 이 bodyYaw force 미적용이 직접 원인 후보

**fix 방향** (= Phase 2):
- packet 확장 — `currentHorizontalAngle`, `currentCameraAngle` 등 force 값을 packet 에 추가
- 또는 server-side 가 자체 계산 (= server 가 player.getYaw / velocity 등으로 force 값 계산 후 broadcast)
- `sm_captureBodyYaw` 의 remote 분기 추가 — sm.stats 의 sync 된 force 값 사용
- ★ static field 들 (`smBodyYawActive`, `smStandardFadeActive`, `smCrawlMode` 등) 이 PlayerEntityRenderer mixin 의 single-instance field 또는 SmartMovingClientState 의 static → ★ player 별 분리 안 됨 ★. SmartMovingClientState 의 인스턴스 field 로 이동 필요.

---

## 4. 작업 순서 (= 내일 진행)

1. **fix-1**: `sendStatePacket` 의 dirty bit 검사 제거 → 매 tick 송신 (= BUG-A + BUG-B 동시 해결)
2. **fix-2**: stats source 를 `getVelocity()` → delta 기반 변경 (= BUG-D 해결)
3. **검증**: 두 client + server 로 인게임 테스트 — Tester2 측에서 rudals 비행/엎드리기/슬라이딩 정상 자세 확인
4. (Phase 2) **fix-3**: bodyYaw force/fade remote 분기 추가 (= BUG-C). packet 확장 또는 static field player 별 분리.

상세 작업 단계 → `checklist_sync.md` 참조.

---

## 5. 메모리 — 다음 세션 진입 시 참고

- 현재 build 상태: render-path helper 적용 + 디버그 로그 추가 (= `[SYNC-DBG-RENDER-ENTRY/CAPTURE-YAW/MODIFY-YAW/MODIFY-HEAD]`). 빌드 OK.
- log_temp.txt 분석 완료 — root cause 명확. dirty bit 검사 + velocity=0 두 fix 만으로 큰 진전 예상.
- 사용자 명시 — "BUG-B 후순위" 였지만 BUG-A 와 동일 root cause 라 같이 fix.
- 사용자 의도 — "완전 로컬 플레이어랑 똑같은 퀄리티의 애니메이션" → 위 1+2+3 모두 필요.

