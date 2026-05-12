# 슬라이딩 remote 동기화 BUG 리서치

## 사용자 보고 BUG (2026-05-12)

> "다른 플레이어가 슬라이딩 시작 시 처음에 공중에서 부터 (땅으로부터 1칸 위) 시작으로 하고, 그 다음 바로 땅 위에서 정상적으로 슬라이딩을 하다가 슬라이딩에서 엎드리기로 전환될 때 땅에 한번 들어갔다가 다시 땅 위로 올라와서 정상 엎드리기가 됨."

= remote (= 다른 client) 측에서 본 다른 player 의 슬라이딩 시각적 BUG.

## 가설 (사용자 힌트)

> "서버 또는 리모트에 self와 동일한 동기화 코드가 안 들어가있을 가능성 있음"

## 결정적 비슷한 사례 — BUG-7 ICC EXIT (2026-05-06 ~ 2026-05-07)

| 항목 | BUG-7 ICC EXIT | 이번 BUG (슬라이딩) |
|---|---|---|
| self 측 처리 | `setPos(y+1)` (ICC EXIT 시) | `move(0,-1,0)` (진입) + `setPos(y+1)` (종료, fix #62 v2) |
| remote 측 증상 | bb + model 1칸 *아래* (~50ms) | 진입 시 bb 1칸 *위* + 종료 시 bb 1칸 *아래* (~50ms) |
| timing | SM state packet 도착 → dim 변경 (mixin offset 해제) 즉시. 그러나 entity.y broadcast 도착은 1 tick lag → 그 사이 bb 1m 아래. | SM state packet 도착 → dim 변경 (진입 시 mixin offset 활성 / 종료 시 차단) 즉시. 그러나 entity.y broadcast 도착은 1 tick lag → 그 사이 bb 1m mismatch. |
| 정착된 fix | v25.3: **packet 처리 lambda 안 즉시 setPos +1m**. | 동일 패턴 (부호만 반전: 진입 -1m / 종료 +1m). |

## self 측 정확 코드 (현재)

### 슬라이딩 진입 (fix #60, 2026-05-10)
`SmartMovingClientState.java` L2106-L2111:
```java
heightOffset = -1F;
isSliding = true;
isHeadJumping = false;
isAerodynamic = false;
player.calculateDimensions();                            // dim 갱신 → mixin offset 활성.
player.move(MovementType.SELF, new Vec3d(0, -1D, 0));    // entity.y -1m + 박스 발 ground.
```

= self 측 동작: SM state set → dim 갱신 (mixin offset 활성, bb +1m up) → entity.y -1m push → bb 발 = entity.y + 1m = ground.

### 슬라이딩 종료 (fix #62 v2 + fix #70, 2026-05-10)
`SmartMovingClientState.java` L2337-L2360:
```java
if (!isHeadJumping && this.heightOffset == -1F) {
    double _currentBoxMinY70 = player.getBoundingBox().minY;     // calc dim 전 — mixin offset 활성 시점.
    double _predictedMinY70 = player.getY();
    boolean _willDrop70 = (_currentBoxMinY70 - _predictedMinY70) > 0.5;
    if (_willDrop70) {
        player.setPosition(player.getX(), player.getY() + 1.0, player.getZ());
        player.lastRenderY += 1.0;
        player.prevY += 1.0;
    }
    this.heightOffset = 0F;
    player.calculateDimensions();
    // cameraY 강제 (= self 1인칭 전용. remote 무관).
}
```

= self 측 동작: predictive drop 검사 → drop > 0.5m 시 entity.y +1m push → bb 발 = ground.

## 3측 분리 분석

### 슬라이딩 진입 시

| 시점 | self | server | remote |
|---|---|---|---|
| t=N: self 슬라이딩 발사 | dim eye=1.62 + bb +1m up + entity.y -1m push → bb 발 = self ground ✓ | — | — |
| t=N tick 끝: client → server | PlayerMoveC2SPacket (new y = self ground - 1m) + StatePayload (isSliding=true) 송신 | — | — |
| t=N+1 server tick 시작 | — | packet 처리 → isSliding=true (fix #63 calc dim) + entity.y -= 1m (vanilla move). bb +1m up + entity.y -1m → bb 발 = ground. fix #89 가 reconcile 차단 ✓ | — |
| t=N+1 server broadcast | — | EntityPositionS2CPacket + StatePayload broadcast | — |
| t=N+1 remote 도착 | — | — | **SM state packet 먼저 처리** → isSliding=true → calc dim → mixin offset 활성 → bb +1m up. 그러나 entity.y broadcast 도착 전 → entity.y = old (self ground). **bb 발 = self ground + 1m = 공중 1칸 위 BUG** |
| t=N+2 remote | — | — | EntityPosition broadcast 도착 → remote.y -= 1m → bb 발 = self ground ✓ |

### 슬라이딩 종료 시

| 시점 | self | server | remote |
|---|---|---|---|
| t=M: SS-SlideStop 매치 | predictive drop 검사 → setPos(y+1) → bb 발 = ground ✓ | — | — |
| t=M tick 끝: client → server | PlayerMoveC2SPacket (new y = self ground) + StatePayload (isSliding=false) 송신 | — | — |
| t=M+1 server tick 시작 | — | packet 처리 → isSliding=false (fix #63 calc dim) + entity.y += 1m (vanilla move) → bb 발 = ground ✓ | — |
| t=M+1 server broadcast | — | EntityPositionS2CPacket + StatePayload broadcast | — |
| t=M+1 remote 도착 | — | — | **SM state packet 먼저 처리** → isSliding=false → calc dim → mixin offset 차단 → bb 발 = entity.y. 그러나 entity.y broadcast 도착 전 → entity.y = old (self ground - 1m). **bb 발 = self ground - 1m = 땅속 1m BUG** |
| t=M+2 remote | — | — | EntityPosition broadcast 도착 → remote.y += 1m → bb 발 = self ground ✓ |

## remote 측 packet handler 검사 — 현재 코드

`SmartMovingClient.java` L58-L134 `registerClientReceivers`:

| 매핑 | 상태 |
|---|---|
| `processStatePacket` + `calculateDimensions` | ✓ 있음 (fix #63 client side 동등) |
| ICC ENTER (`!wasIcc && isClimbCrawling`) | ✓ 있음 (L94-L105) — self side `move(0,0.05,0)` + entity.y 복원 + horizontalCollision 복원 매핑 |
| ICC EXIT (`wasIcc && !isClimbCrawling`) | ✓ 있음 (L107-L132) — self side `setPosition(x, y+1, z) + lastRenderY/prevY +=1 + bottom snap` 매핑 |
| **isSliding 진입 (`!wasSliding && isSliding`)** | **✗ 누락** ← BUG root |
| **isSliding 종료 (`wasSliding && !isSliding`)** | **✗ 누락** ← BUG root |

server side (`SmartMovingServer.processStatePacket`) 는 isSliding bit 21 디코딩 + calculateDimensions 호출 (fix #63) 만 처리. server 자체 entity.y push 없음 — vanilla move packet 으로 entity.y 갱신.

## fix 설계

### 진입 매핑 (= self 의 fix #60 1:1)

```java
if (!wasSliding && target.isSliding
        && entity instanceof AbstractClientPlayerEntity remoteSlideEnter
        && !(entity instanceof ClientPlayerEntity)) {
    double newY = remoteSlideEnter.getY() - 1.0;
    remoteSlideEnter.setPosition(remoteSlideEnter.getX(), newY, remoteSlideEnter.getZ());
    remoteSlideEnter.lastRenderY = newY;
    remoteSlideEnter.prevY = newY;
}
```

- `processStatePacket` + `calculateDimensions` 이미 위 (L82-L84) 에서 호출됨 → dim eye=1.62 활성 → mixin offset 활성 → bb 발 = remote.y + 1m.
- setPos(y-1) 호출 → bb 발 = (remote.y - 1) + 1 = old remote.y = self ground ✓.
- lastRenderY/prevY 동기화 → vanilla lerp 점프 차단.

### 종료 매핑 (= self 의 fix #62 v2 + fix #70 1:1)

```java
// calc dim 전 — mixin offset 활성 시점의 bb 측정.
boolean wasSliding = target.isSliding;
double slideBoxMinYBefore = 0;
if (wasSliding && entity instanceof AbstractClientPlayerEntity rs) {
    slideBoxMinYBefore = rs.getBoundingBox().minY;
}

target.processStatePacket(payload.state());
if (entity instanceof LivingEntity living) living.calculateDimensions();

// ... 기존 ICC ENTER/EXIT ...

if (wasSliding && !target.isSliding
        && entity instanceof AbstractClientPlayerEntity remoteSlideExit
        && !(entity instanceof ClientPlayerEntity)) {
    double newY = remoteSlideExit.getY();
    boolean willDrop = (slideBoxMinYBefore - newY) > 0.5;
    if (willDrop) {
        newY += 1.0;
        remoteSlideExit.setPosition(remoteSlideExit.getX(), newY, remoteSlideExit.getZ());
        remoteSlideExit.lastRenderY = newY;
        remoteSlideExit.prevY = newY;
    }
}
```

- `slideBoxMinYBefore` = calc dim *전*. wasSliding=true 시 mixin offset 활성 → bb 발 = remote.y + 1m. = self 측 fix #70 의 `_currentBoxMinY70` 등가.
- `newY` = calc dim *후*. remote.y 변화 없음 (broadcast 도착 전). bb 발 = remote.y (mixin offset 차단).
- `willDrop = slideBoxMinYBefore - newY > 0.5` → 차이 1m → push.
- 비행 → 슬라이딩 종료 (fix #87 시나리오) 처럼 slide 직전 시점에 mixin offset 이 이미 차단된 케이스: slideBoxMinYBefore = remote.y (= mixin offset 차단된 bb 발) → willDrop=false → push 안 함 → 회귀 차단 ✓.

## 회귀 검토

| 케이스 | 영향 | 검증 |
|---|---|---|
| self 슬라이딩 (1인칭) | `!(entity instanceof ClientPlayerEntity)` 가드 → 분기 진입 X | ✓ |
| remote 일반 슬라이딩 | 진입 -1m / 종료 +1m → BUG fix | ✓ |
| remote 비행 → 슬라이딩 종료 (fix #87) | mixin offset 이미 차단 → willDrop=F → push X | ✓ (회귀 차단) |
| remote 헤드점프 (= 자체 슬라이딩 → fox movement) | wasSliding=T → isSliding=F + isHeadJumping=T. willDrop 검사 매치 시 push 발생. self 측은 fix #41/#43 헤드점프 진행 중 wasCrawling skip + heightOffset=-1F 잔존. remote 측은 setPos 영향이 entity.y 직접 → 헤드점프 진행 timing 영향 검토 필요. | ⚠ 모니터 |
| remote ICC ENTER/EXIT | 별 분기 → 영향 X | ✓ |

## 참조 메모리

- `project_bug7_remote_icc_exit_complete.md` — v25.3 packet lambda 즉시 fix 패턴.
- `feedback_packet_lambda_immediate_fix.md` — 패턴 일반화.
- `feedback_self_to_server_remote_one_to_one.md` — self 코드 그대로 1:1 복제 원칙.
- `feedback_network_explain_three_sides.md` — 3측 분리 의무.
- `project_slide_transition_camera_fix.md` — self 측 fix #60/#62 v2 정착.
- `project_transition_box_drop_complete.md` — self 측 fix #70 predictive drop.
- `feedback_processStatePacket_calculateDimensions.md` — server side 패턴 (fix #63).
- `feedback_server_reconcile_box_sync.md` — 클라/서버 박스 동기화.
