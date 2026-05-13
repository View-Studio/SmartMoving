# 헤드점프 멀티 환경 BUG 리서치 (2026-05-13)

## 사용자 보고 BUG

1. **BUG 1 (끊김)** — 다른 player 가 하는 헤드점프가 부드럽지 않고 끊기듯이 보임. self 에 적용되는 fade 보간이 remote 측에서 안 되는 느낌.
2. **BUG 2 (1칸 down)** — 다른 player 헤드점프 착지 시 1칸 아래로 잠깐 잠겼다 다시 올라옴. (BUG-7/15/B/D 와 동일 카테고리 패턴.)

회귀 차단 필수 (= 함부로 수정 금지 메모리):
- `project_headjump_final` (single-player 헤드점프 완결, fix #79~#89).
- `project_slide_remote_sync_complete` (BUG-15) + `project_bug_b_icc_exit_lerp_cancel_fix` (BUG B).
- `project_bug7_remote_icc_exit_complete` / `project_bug7_remote_icc_velocity_clamp_complete`.
- `project_bug_d_flying_landing_complete` / `project_bug_e_remote_fly_crawl_landing_fix` (분기 결정 매트릭스 검토).
- `project_bug_a_first_person_hand_fix` (1인칭 손 leak 패턴 — fade 식 수정 시 검토).

---

## 코드 직접 검증 결과 (2026-05-13)

### 1. SmartMovingClient.java packet lambda (registerClientReceivers, L58-310)

`grep -n "isHeadJumping\|HeadJump"` 결과 **L58-310 안 헤드점프 분기 0건** 확인.

현재 처리 분기 매트릭스:
- ICC 진입 (L112-123) / ICC 종료 (L125-161) ✓
- Slide 진입 (L178-192, `!wasFlying` 가드) / Slide 종료 (L204-220, predictive drop) ✓
- Flying 종료 (L253-308, 분기 2 standUp / 분기 3-A 자연 crawl / 분기 3-B sneak+grab slide) ✓
- **헤드점프 진입 / 종료 — 없음** ✗

### 2. self side 헤드점프 종료 식 (SmartMovingClientState.java L2196-2220)

```java
if (wasHeadJumping && !isHeadJumping && player.isOnGround()) {
    this.wasSelfSlideFire = false;
    handleCrash(player, ...);
    restoreFromFlying = true;
    this.justEndedHeadJump = true;     // fix #19 — 1-tick 플래그
    this.slideToHeadCooldown = 5;
    standupIfPossible(player, false, true);  // ★ entity.y 변경 path
    this.restoreFromFlying = false;
}
```

`standupIfPossible` 안 setPos(y+1) 호출 경로 후보:
- **L3891 안전망** — `wasFlying || wasLevitating` 가드. 정상 헤드점프는 wasFlying=false → 통과 X.
- **L3923-L3934** `justEndedHeadJump` 분기 — `wasSmallBox && (isCrawling||isSliding)` 가드. 정상 헤드점프는 isCrawling=false+isSliding=false → 통과 X.
- **별도 path** — L3823 `_wasSmallBox && justEndedHeadJump && !isHeadJumping && !isSliding && !isCrawling && !isCrawlClimbing && !isSwimming_sm && !isDiving && !isFlying && !isLevitating` 가드 (= 정상 헤드점프 종료 경로).

→ dump 으로 self.y 변화 정확값 측정 필요. self.y 변화 분기 결정 (= 시나리오별 isCrawling/isSliding state) + entity.y 변화량 확인.

### 3. server side 처리 (SmartMovingServer.java L168-218)

```java
boolean newHeadJumping = ((bits >> 20) & 1) != 0;
if (newHeadJumping != isHeadJumping) {
    isHeadJumping = newHeadJumping;
    if (newHeadJumping) player.calculateDimensions();  // 진입 시만
}
```

fix #42 의 의도 = 진입 시만 dim 갱신, 종료 시 skip (= fix #53 시나리오 = 헤드점프+자체슬라이딩 동시 cycle 차단).

### 4. remote side 처리

`SmartMovingClient.java L100-102`:
```java
target.processStatePacket(payload.state());
if (entity instanceof LivingEntity living) {
    living.calculateDimensions();  // 모든 SM state 비트 변경 시 dim 즉시 갱신
}
```

= SM state packet 도착 시 isHeadJumping=false 적용 + calculateDimensions 호출. mixin offset 차단 → bb 발 = remote.y.

### 5. fade 보간 (MixinPlayerEntityRenderer.java L741-764)

```java
if (sm.isHeadJumping) {
    float thetaTarget = ...;  // angle 식
    if (sm.wasSelfSlideFire) {
        sm.smHeadJumpTiltX_prev = (float) Math.PI / 2f;
        sm.smHeadJumpFade_prevTime = animationProgress;
    }
    float thetaLerped = lerpFadeAngle(sm.smHeadJumpTiltX_prev, thetaTarget,
                                       sm.smHeadJumpFade_prevTime, animationProgress);
    matrices.translate(0f, 1.5f, 0f);
    matrices.multiply(RotationAxis.POSITIVE_X.rotation(-thetaLerped));
    matrices.translate(0f, -1.5f, 0f);
    sm.smHeadJumpTiltX_prev = thetaLerped;
    sm.smHeadJumpFade_prevTime = animationProgress;
}

// L801~804 — 헤드점프/슬라이딩 외 분기 진입 시 prev reset.
if (!sm.isHeadJumping && !sm.isSliding) {
    sm.smHeadJumpTiltX_prev = 0f;
    sm.smHeadJumpFade_prevTime = animationProgress;
}
```

- prev/curr 인스턴스 — `SmartMovingClientState` 별 (UUID 별 get). self/remote 별도.
- `thetaTarget` 식은 `sm.stats.currentVerticalAngle` 의존.

---

## BUG 2 root cause (코드 검증 기반, dump 검증 예정)

3측 분리:

| 측 | 동작 | 시점 |
|---|---|---|
| self | wasHJ=true && !isHJ → standupIfPossible → setPos(y+1) **+1m push**. dim 갱신. | T |
| server | self c2s packet 수신 → entity.y 갱신 (+1m 적용). SM state bit 처리 → broadcast S2C: ① SM state packet, ② EntityPositionS2CPacket. | T+1~2 server tick |
| remote | ① SM state packet 도착 → isHeadJumping=false + L101 calculateDimensions → mixin offset 차단 → bb 발 = remote.y (= self ground - 1m) = **땅속 1m**. ② ~1 tick 후 EntityPositionS2CPacket 도착 → remote.y = self ground → 정상. | T+2~3 client tick (시각 1m down) |

= BUG-7 ICC EXIT / BUG-15 slide / BUG D fly 와 **정확히 동일 메커니즘**. server-side networkIo.tick vs world.tick 시점 차이로 SM state packet 이 entity.y broadcast 보다 ~1 tick 빨리 도착.

### fix 패턴 (= 검증된 packet lambda 분기 추가)

```java
boolean wasHJ = target.isHeadJumping;  // processStatePacket 전 저장
// ... processStatePacket + calculateDimensions ...
if (wasHJ && !target.isHeadJumping
        && entity instanceof AbstractClientPlayerEntity remoteHJ
        && !(entity instanceof ClientPlayerEntity)) {
    MixinLivingEntityAccessor accH = (MixinLivingEntityAccessor)(Object) remoteHJ;
    double newY = remoteHJ.getY() + 1.0;  // self side +1m 1:1
    remoteHJ.setPosition(remoteHJ.getX(), newY, remoteHJ.getZ());
    remoteHJ.lastRenderY = newY;
    remoteHJ.prevY = newY;
    accH.sm_setServerY(newY);
    accH.sm_setBodyTrackingIncrements(0);
}
```

= ICC EXIT (L134-160) 와 동일 패턴. lerp cancel 동반 필수 (= BUG B 회귀 패턴).

### 분기 매트릭스 검토 (= BUG D/E 충돌 검증)

- Flying 종료 분기 (`wasFlying && !target.isFlying`) 와 헤드점프 종료 분기는 *별개 path*. 헤드점프 = 비행 아님.
- Slide 종료 분기 (`wasSliding && !target.isSliding`) 도 별개. 헤드점프 = isSliding=false.
- ICC 종료 분기 (`wasIcc && !target.isClimbCrawling`) 도 별개.
- 단, **헤드점프 → 슬라이딩 직접 전환** 등 동시 변경 시나리오 검토 필요 — fix #41 의 fox movement (isHeadJumping=true && isSliding=true) 종료 시 어느 분기 매치?

---

## BUG 1 root cause 후보 (코드 검증 기반, dump 검증 필수)

### (a) currentVerticalAngle stale — 가능성 높음

- `thetaTarget = (sm.isAerodynamic ? cfg.headJumpAerodynamicMaxAngleRad : π/2 - sm.stats.currentVerticalAngle)`.
- `sm.stats.currentVerticalAngle` 갱신 위치 확인 필요 — self 측은 매 tick travel/handleX 안에서 측정 가능. remote 측은 packet 으로 broadcast 안 됨 가능성.
- stale 시 thetaTarget = 잘못된 값 → fade 가 그쪽으로 lerp → "끊김" 시각.

### (b) isHeadJumping 진입/종료 timing 차이 — 가능성 중

- SM state packet 도착 시점이 self 의 isHeadJumping 전환 시점과 ~1 tick 차이.
- fade prev=0 reset 가드 (L801~804) 가 self 와 다른 frame 에 발동 → 첫 frame 급격 회전.
- 정량 검증 = self.isHeadJumping enter/exit frame vs remote.isHeadJumping enter/exit frame timestamp dump.

### (c) PlayerEntityModel single instance leak — 가능성 낮음

- BUG A 와 동일 메커니즘. setupTransforms 의 matrices stack 은 frame 별 새로 시작 → fade prev 가 single instance 영향 X.
- 그러나 sm_animateHeadJumping (L1322~) 의 leg/arm 회전 set 식이 single instance 영향 가능.

---

## 진단 dump 설계 (BUG 2 + BUG 1 동시)

### self side (ClientPlayerEntity)
- `[HJ-MULTI-SELF]` PlayerEntity.tick TAIL inject:
  - wasHeadJumping/isHeadJumping enter/exit frame.
  - entity.y BEFORE / AFTER standupIfPossible 호출.
  - justEndedHeadJump set/reset 시점.
  - sm.stats.currentVerticalAngle 매 tick 값.

### server side (PlayerEntity)
- `[HJ-MULTI-SERVER-RECV]` SmartMovingServer.processStatePacket 안:
  - newHeadJumping bit transition 시 player.getY() 출력.
- `[HJ-MULTI-SERVER-BCAST]` PlayerEntity.baseTick TAIL (server 측):
  - player.getY() + isHeadJumping bit broadcast 직전 값.

### remote side (OtherClientPlayerEntity)
- `[HJ-MULTI-REMOTE-PKT]` packet lambda 안 (= SmartMovingClient L100 직전/직후):
  - wasHJ → isHJ transition 검출.
  - remote.getY() / serverY (= sm_getServerY) / bti (= sm_getBodyTrackingIncrements) 출력.
- `[HJ-MULTI-REMOTE-FADE]` MixinPlayerEntityRenderer L741 분기 안:
  - sm.isHeadJumping / smHeadJumpTiltX_prev / thetaTarget / sm.stats.currentVerticalAngle 매 frame.
- `[HJ-MULTI-REMOTE-TICK]` PlayerEntity.tick TAIL:
  - remote.getY() / serverY / bti 매 tick → 1m down 시각 transition 검증.

---

## fix 적용 순서 (사용자 합의)

1. BUG 2 dump 수집 → root cause 확정 → fix (packet lambda 분기 추가) → 사용자 회귀 테스트.
2. BUG 1 dump 수집 → root cause 확정 (3 후보 중) → fix → 회귀 테스트.

각 BUG 별로 사용자 인게임 테스트 통과 후 메모리 기록 + 커밋.

---

## 참조 코드 위치

- `SmartMovingClient.java` registerClientReceivers L58-310 (= packet lambda).
- `SmartMovingClientState.java` L2196-2220 (= self 종료 entity.y push path).
- `SmartMovingClientState.java` standupIfPossible (= L3700~3990, setPos 호출 분기).
- `SmartMovingServer.java` L168-218 (= server processStatePacket).
- `MixinPlayerEntityRenderer.java` L741-764 (= fade 보간) + L801-805 (= prev reset).
- `MixinPlayerEntityModelClient.java` sm_animateHeadJumping L1322+ (= 모델 set).
- `MixinLivingEntityAccessor` (= srvY/bti accessor).
