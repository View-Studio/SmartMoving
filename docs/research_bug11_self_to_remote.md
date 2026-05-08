# BUG-11 self → remote/server 1:1 복제 리서치

## 목적
`22779f3` (BUG-11 v3 fix) 의 "살짝 플리킹" 잔존 한계를 BUG-7 v26.18 와 동일한 "self 코드 1:1 복제" 패턴으로 해결.

## BUG-11 증상 (3 분리)
- **self (local client)**: 비행 종료 처리 정상 (= `standupIfPossible` 호출).
- **server**: bit 17 (isFlying) 디코딩 안 함. broadcast 좌표는 server vanilla 결과.
- **remote (other client)**: SM relay packet 의 isFlying=false sync 시점 → bb +1m offset 가드 미통과 → bb 즉시 1m 아래 → 모델 땅에 반쯤 묻혔다 나옴.

## self-side 비행 종료 처리 코드 (사실 인용)

### `SmartMovingClientState.tickMovement` 흐름
- L1748-L1749: `wasFlying = isFlying; isFlying = cfg0.isFlyingEnabled() && abilities.flying && !isSwimming_sm && !isDiving;`
- L1774-L1776: `if (!isFlying && wasFlying) restoreFromFlying = true;` (해제 엣지)
- L1810-L1820: 해제 엣지에서 `heightOffset = -1F` 강제 재설정 (standupIfPossible 가드 통과용)
- L1822-L1850: tryLanding 계산 + `if (restoreFromFlying || tryLanding) standupIfPossible(player, tryLanding, restoreFromFlying);`

### `standupIfPossible(player, tryLanding, restoreFromFlying)` (L3492-L3608)
```java
if (heightOffset >= 0) return;
double gap        = getGapUnderneight(player);              // L3450
boolean groundClose = gap < 1D;
double overGap    = groundClose ? getGapOverneight(player) : -1D;
boolean standUpPossible = gap + overGap >= 1D;

if (tryLanding && groundClose && standUpPossible) {
    isFlying = false;
    player.getAbilities().flying = false;
    networkHandler.sendPacket(new UpdatePlayerAbilitiesC2SPacket(...));
    restoreFromFlying = true;
}
if (!restoreFromFlying) return;

boolean sneakPressed = options.sneakKey.isPressed();        // self only
boolean grabPressed  = SmartMovingKeys.grab.isPressed();    // self only

if (!groundClose && !sneakPressed) {
    resetHeightOffset();                                    // 좌표 미변경 (자유 낙하)
} else if (standUpPossible && !(sneak && grab)) {
    standUp(player, gap);                                   // setPos(y + (1 - gap))
} else {
    boolean wasSmallBox = (heightOffset == -1F);
    toSlidingOrCrawling(player, gap);                       // setPos(y - gap)
    if (wasSmallBox && (isCrawling || isSliding)) {
        player.calculateDimensions();
        player.setPosition(player.getX(), player.getY() + 1.0, player.getZ());
        player.lastRenderY += 1.0;
        player.prevY += 1.0;
        // Camera 보정 (1인칭 self only)
        mustCrawl = true;
    }
}

player.calculateDimensions();

// 안전망: 박스 발 아래 솔리드 검사 → push up
Box bbAfter = player.getBoundingBox();
double solidUnder = getMaxPlayerSolidBetween(player, bbAfter.minY - 1.0, bbAfter.minY + 0.5, 0);
if (bbAfter.minY < solidUnder - 1.0E-5) {
    double pushY = solidUnder - bbAfter.minY;
    player.setPosition(player.getX(), player.getY() + pushY, player.getZ());
}
```

### `standUp(player, gap)` (L3382-L3399)
```java
player.setPosition(player.getX(), player.getY() + (1D - gapUnderneight), player.getZ());
isCrawling    = false;
isHeadJumping = false;
resetHeightOffset();   // heightOffset = 0F
```

### `toSlidingOrCrawling(player, gap)` (L3412-L3430)
```java
player.move(MovementType.SELF, new Vec3d(0, -gapUnderneight, 0));
if (cfg.slide && cfg.enabled && (grabPressed || wasHeadJumping)) {
    isCrawling = false;
    isSliding = true;
} else {
    wasCrawling = toCrawling();   // isCrawling = true
}
```

## 현재 BUG-11 v3 fix (`MixinPlayerEntityClient.sm_handleRemoteFlyingExitYSync`)
```java
@Inject(method = "tick", at = @At("HEAD"))
private void sm_handleRemoteFlyingExitYSync(...) {
    if (sm.smPrevWasFlyingForLerpFix && !sm.isFlying) {
        double newY = remote.getY() + 1.0;            // 무조건 +1.0
        remote.setPosition(remote.getX(), newY, remote.getZ());
        remote.lastRenderY = newY;
        remote.prevY = newY;
    }
    sm.smPrevWasFlyingForLerpFix = sm.isFlying;
}
```

## self vs remote (v3) 차이점 — 사실 비교

| 항목 | self standupIfPossible | remote v3 fix |
|---|---|---|
| 검출 시점 | tickMovement 안 wasFlying 엣지 | tick HEAD enter-edge (1 tick lag) |
| gap 측정 | `getGapUnderneight` AABB 정밀 | 측정 안 함 |
| 분기 조건 | groundClose / standUpPossible / sneak·grab | 분기 없음 |
| 좌표 보정 | `(1 - gap)` (standUp), `-gap + 1` (crawl), `0` (자유 낙하) | 무조건 `+1.0` |
| dim 갱신 | calculateDimensions (마지막) | 없음 (별도 packet handler 에서) |
| 안전망 | 솔리드 push up | 없음 |
| isLevitating 처리 | 동일 분기 (L1810: `!isLevitating && wasLevitating`) | 미처리 (= isFlying 만 검사) |

## 옮길 부분 (사실 기반 결정)

### ✅ 옮긴다 (= self 1:1 복제 가능)
1. **gap 측정** — `getGapUnderneight`/`getGapOverneight` 사용. 시그니처를 `AbstractClientPlayerEntity` 로 확장 (= getMaxPlayerSolidBetween 이 이미 호환).
2. **standUp 분기** — `setPos(y + (1 - gap))` 정밀 보정.
3. **toSlidingOrCrawling 분기** — `move(0, -gap, 0)` + crawl/slide=true + smSmall 보정 (= setPos +1.0 + lastRenderY/prevY +=1).
4. **!groundClose 분기** — 좌표 미변경 (= 자유 낙하 vanilla 진행).
5. **calculateDimensions** — 이미 packet handler L82-L84 에서 호출됨.
6. **안전망 솔리드 push up** — getMaxPlayerSolidBetween remote 호환.
7. **isLevitating 도 동일 처리** — self 의 L1810 `!isLevitating && wasLevitating` 분기 1:1.

### ❌ 옮기지 않는다 (= self only 의존)
1. **`networkHandler.sendPacket(UpdatePlayerAbilitiesC2SPacket)`** — self only. server 가 abilities 동기화.
2. **`abilities.flying = false`** — server 가 제어. remote 측 의미 없음.
3. **Camera 보정** — 1인칭 self only.
4. **`tryLanding` 식** — self 의 자동 착지 트리거. remote 는 enter-edge 자체가 신호.
5. **`restoreFromFlying` flag** — self lifecycle. remote 는 enter-edge 자체가 신호.

### ⚠️ 단순화 (= remote 정보 부족)
1. **`sneakPressed` / `grabPressed` 키 검사** — remote 키 정보 없음.
   - 대안 1: SM relay packet bit 33 (`isSneakButtonPressed`) — sneak 만 사용 가능. grab 비트 없음.
   - 대안 2: SM 결과 비트로 유추 (= `isCrawling`/`isSliding` 결과로 분기 결정).
   - **결정**: self 의 분기 결과를 SM packet 에서 받은 결과 비트로 reverse 추론.
     - self 가 `standUp` 분기 → isCrawling=false, isSliding=false, isFlying=false 결과.
     - self 가 `toSlidingOrCrawling`+`toCrawling` 분기 → isCrawling=true, heightOffset=-1F 결과.
     - self 가 `toSlidingOrCrawling`+`isSliding` 분기 → isSliding=true, heightOffset=-1F 결과.
     - self 가 `resetHeightOffset` 분기 (1m+ 공중) → 모든 SM 비트 false, heightOffset=0F.
   - SM relay packet 결과만 보고 분기 결정 → self 의 분기 1:1 매치.

## 적용 위치 결정

### remote-side
**`SmartMovingClient.registerClientReceivers`** 의 `StatePayload` lambda 안.

근거:
- BUG-7 v25.3 (메모리 *feedback_packet_lambda_immediate_fix*) 와 동일 패턴.
- `target.processStatePacket(payload.state())` 직후 → SM 비트 갱신과 dim 갱신과 setPos 보정이 같은 시점에 처리 → 1 tick lag 0.
- 현재 `MixinPlayerEntityClient.sm_handleRemoteFlyingExitYSync` 는 제거.

### server-side
**옵션 — 추가 작업 가능**:
- `SmartMovingServer.processStatePacket` 에 bit 17 (isFlying) / bit 19 (isLevitating) 디코딩 추가.
- ServerPlayNetworking.registerGlobalReceiver(StatePayload.ID) lambda 안에서 wasFlying detect + setPos 보정 → broadcast 좌표 정합.

근거 (BUG-7 v26.18 와 비교):
- BUG-7 ICC 진입은 매 tick vanilla travel 진행이 잘못 → server-side travel cancel 필수.
- BUG-11 비행 종료는 1회 enter-edge → server.player.y 1회 보정만 하면 됨.
- 우선 remote-side fix 만 진행. server-side 는 확인 후 결정 (= BUG-7 ICC EXIT v25.3 도 server-side 추가 작업 없이 client 측만으로 해결됨).

## 회귀 위험 영역
- 비행 종료 시 모델 위치 — 다양한 케이스 (지면/공중/천장 막힘).
- BUG-7 ICC EXIT — 같은 packet handler 안에 추가하므로 흐름 영향.
- self 측 비행 종료 — 변경 없음 (= MixinPlayerEntityClient 의 `sm_handleRemoteFlyingExitYSync` 제거. self 는 자체 standupIfPossible 사용).
- isLevitating 종료 — self 와 동일하게 처리 (= 새 분기 추가).

## 참조
- 메모리 *feedback_self_to_server_remote_one_to_one* — self 1:1 복제 원칙.
- 메모리 *feedback_packet_lambda_immediate_fix* — packet handler lambda 안 즉시 fix 패턴.
- 메모리 *project_bug7_remote_icc_exit_complete* — BUG-7 ICC EXIT v25.3 패턴.
- 메모리 *project_bug7_remote_velocity_clamp_complete* — BUG-7 v26.18 server-side travel cancel.
- git `092d6f4` — BUG-7 v26.18 server-side mixin.
- git `31bd14c` / `e202d9e` — BUG-7 ICC EXIT v25.3 packet handler lambda 안 fix.
