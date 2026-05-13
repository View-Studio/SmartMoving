# 슬라이딩 → 1칸 공간 → crawl 전환 시 remote 1m down 리서치 (2026-05-13)

## 사용자 보고

> "슬라이딩 중 1칸 공간에 들어가서 엎드리기로 전환될 때 아주 잠깐 1칸 아래로 잠겼다가 바로 다시 올라옴."

= 다른 player (= remote 시점) 가 슬라이딩 중 1칸 공간 진입 → crawl 자동 전환 시 1m down 시각.

= **BUG 2 (헤드점프 착지 1m down) 와 동일 카테고리** (= broadcast lag 메커니즘).

회귀 차단 필수:
- `project_slide_remote_sync_complete` (BUG-15) — 정상 slide 진입/종료 + 비행→slide 등.
- `project_transition_box_drop_complete` (fix #65~#71) — self side transition box drop.
- BUG 2 fix (= HJ remote 종료 1m down, 2026-05-13).
- BUG D/E fly landing / BUG-7 ICC EXIT / BUG B / BUG A.

## self side 식 (= 이미 정착, fix #70)

`SmartMovingClientState.java` L2318~2403 SS-SlideStop 분기:

```java
if (isSliding) {
    if (!sneakPressedRaw || speed<threshold || isFlying) {
        isSliding = false;
        // fox movement isAerodynamic set ...
        if (!isHeadJumping && !isFlying) {
            wasCrawling = toCrawling();   // ← isCrawling=true set
        }
        if (!isHeadJumping && this.heightOffset == -1F) {
            // fix #70 predictive drop check.
            double currentBoxMinY = player.getBoundingBox().minY;
            double predictedMinY = player.getY();
            if ((currentBoxMinY - predictedMinY) > 0.5) {
                player.setPosition(x, y + 1.0, z);   // ← +1m push
                player.lastRenderY += 1.0;
                player.prevY += 1.0;
            }
            heightOffset = 0;
            calc dim;
        }
    }
}
```

self side: **entity.y += 1.0** (= slide→crawl 시).

## remote side 현재 코드 (= 이미 있는 분기)

`SmartMovingClient.java` L223~239:

```java
if (wasSliding && !target.isSliding
        && entity instanceof AbstractClientPlayerEntity remoteSlideExit
        && !(entity instanceof ClientPlayerEntity)) {
    MixinLivingEntityAccessor accX = ...;
    double newY = remoteSlideExit.getY();
    boolean willDrop = (slideBoxMinYBefore - newY) > 0.5;
    if (willDrop) {
        newY += 1.0;
        remoteSlideExit.setPosition(x, newY, z);
        remoteSlideExit.lastRenderY = newY;
        remoteSlideExit.prevY = newY;
        accX.sm_setServerY(newY);
        accX.sm_setBodyTrackingIncrements(0);
    }
}
```

= **이론상 slide→crawl 도 cover** (= `wasSliding && !target.isSliding`).

## 가설 — fix 가 발동 안 되는 이유

### 가설 A — willDrop 검사 false 매치 (= slideBoxMinYBefore 측정 부정확)
- self side: 슬라이딩 활성 시 box.minY = entity.y+1 (mixin offset 활성, eye=1.62).
- remote side: SM state packet 도착 직전 box.minY = remote.y+1 (= 슬라이딩 활성 잔존).
- 그러나 `slideBoxMinYBefore` 저장 시점 (= L84~95) 이 정확한가?

코드 확인 (L88~95):
```java
if (entity instanceof AbstractClientPlayerEntity rsPre
        && !(entity instanceof ClientPlayerEntity)) {
    double bbMinY = rsPre.getBoundingBox().minY;
    if (wasSliding) slideBoxMinYBefore = bbMinY;  // ← 슬라이딩 활성 시 저장
    if (wasFlying) flyBoxMinYBefore = bbMinY;
}
```

= 정확. processStatePacket 호출 전 slideBoxMinYBefore = bb.minY (= mixin offset 활성).

### 가설 B — newY 계산 시 entity.y 가 다른 값
`newY = remoteSlideExit.getY()` = calc dim 후 entity.y. processStatePacket + calc dim 만으로 entity.y 변경 X.
willDrop = (slideBoxMinYBefore - entity.y). 매치 시 push 발동.

= 정확히 발동되어야.

### 가설 C — fix 발동 후도 vanilla lerp 가 끌어내림 (= lerp cancel 실패)
- `accX.sm_setServerY(newY)` + `sm_setBodyTrackingIncrements(0)` 적용.
- 다음 broadcast 도착 전까지 lerp 차단.
- 만약 lerp cancel 직후 즉시 새 broadcast 도착 + srvY=push 전 self.y (= ground-1m) 갱신 → lerp 가 끌어내림.

### 가설 D — 별도 mixin 또는 inject 가 entity.y 끌어내림
- `sm_handleRemoteFlyingExitYSync` 같은 inject 가 매 tick HEAD 작동.
- slide→crawl 시 wasFlying 가드는 false (= flying 아님) → skip.
- 다른 inject?

### 가설 E — POSE 매핑 차이 + 모델 origin
- remote 측 calc dim 후 POSE = SWIMMING (crawl) 가능.
- mixin offset 가드 `POSE == SLIDING` 매치 X → mixin offset 차단 → bb.minY = remote.y.
- self side 도 동일 → 동등 처리.

### 가설 F — slide 종료 분기 자체 작동 X (= 새 path)
- self side SS-SlideStop 가 isSliding=false set 만 하고 끝 + 다른 분기에서 isCrawling=true set 시.
- BUG-15 의 willDrop check 가 wasSliding=true && isSliding=false 만 검사 → 매치 ✓.
- 같은 tick 안 isCrawling=true set 도 packet 으로 도착 → target.isCrawling=true.

음 일단 발동 자체는 OK. 검증 필요.

## 검증 방법

기존 [HJ-MULTI-*] dump 가드 (= isHeadJumping window) 가 slide/crawl transition 미커버. 가드 확장 필수.

dump 가드 확장:
- self side: `isSliding || isCrawling || wasSliding || wasCrawling` 시도 window set.
- packet lambda: slide/crawl transition 시 window set + log.
- server side: isSliding/isCrawling bit transition 시 RECV log + window set.

dump prefix:
- 기존 [HJ-MULTI-*] (= BUG 2 dump, 이미 fix 완료, 제거 또는 유지 둘 다 가능).
- 새 [SC-MULTI-*] (= Slide-to-Crawl 전용).

선택: 기존 prefix 변경 `[HJ-MULTI-*] → [TX-MULTI-*]` (Transition 일반화) + 가드 확장. variable rename `hjDumpRemainingTicks → tranDumpRemainingTicks`. 통합 dump.

## 다음 단계

1. dump 가드 확장 + prefix 일반화.
2. 사용자 인게임 재현 + dump 수집.
3. self/server/remote 매트릭스 분석 → root cause 확정.
4. fix → 회귀 검증.
