# shift+grab 엎드리기 진입 BUG 리서치

> **결론 (2026-04-30 종결)**: 본 문서의 1차 가설 (Fix A/B/C) 은 **모두 틀림**.
> 실제 root cause 는 `restoreFromFlying = true` stale 잔존으로 매 틱 standupIfPossible
> 호출 → 끝의 `player.calculateDimensions()` 가 mixin 가드 매치 못 한 시점 호출 →
> vanilla SWIMMING POSE dim (0.6×0.6) 적용 → box height 0.6 → mustCrawl 오발동.
> 최종 fix + 진단 흐름 = `docs/checklist_shift_grab_crawl_bug.md` 참조.

---

사용자 보고 (2026-04-30):

> shift+grab으로 엎드리기 진입할 때 정상 엎드리기 콜리전보다 더 콜리전 크기가 줄어들고,
> 속도도 거의 안움직이는거처럼 느려지고, shift를 릴리즈해도 엎드리기 해제가 안될때가 있는데
> 이게 왜 생기는 버그인지 완전 꼼꼼히 찾아줘.

증상 3 가지:
1. **콜리전 더 줄어듦** — 정상 엎드리기보다 박스가 더 작거나 작아 보임.
2. **속도 거의 안 움직임** — 엎드리기 진입 직후 거의 정지에 가까운 이동.
3. **shift 릴리즈해도 해제 안 됨** — 가끔 엎드리기 풀림.

---

## 1. 코드 흐름 — shift+grab 입력 처리

### 1-1. tickEssential (SmartMovingClientState.java)

shift+grab 키 둘 다 눌리면 다음 4 가지가 동시에 발동:

```
sneakPressedRaw = true             (shift)
grabPressed0    = true             (grab)
grabJustPressed = true (1틱만)     (rising-edge)
```

**(a) wantCrawl 갱신** — `SmartMovingClientState.java:1240`
```java
wouldWantCrawl =
    !player.getAbilities().flying &&
    (
        (isCrawling && (inputContinueCrawl || contextContinueCrawl))
        ||
        (grabJustPressed && (sneakToggled || sneakPressedRaw) && player.isOnGround()
                && !facedClimbable_)
    );
wantCrawl = crawlingEnabled_ && wouldWantCrawl_;
```

- `facedClimbable_` = `isFacedToLadder || isFacedToSolidVine` — **ladder/vine 정면만 검사** (fence/iron_bars/일반 벽 X).
- 따라서 평지 / 일반 벽 인접 / 덩굴 정면 아닌 일반 케이스 → `wantCrawl=true`.

**(b) wantClimb 갱신** — `SmartMovingClientState.java:1358`
```java
wouldWantClimb =
    (grabPressed0 || (isClimbHolding && sneakPressedRaw)
        || (autoLadder && faced) || (autoVine && faced))
    && (!isSliding || ...) && !isHeadJumping
    && !wantCrawlNotClimb && !_disabled3a;
wantClimb = cfg0.freeClimb && cfg0.enabled && wouldWantClimb;
```

- `grabPressed0=true` → `wouldWantClimb=true` → **`wantClimb=true`** (grab 누르는 한 항상).
- `wantClimbNotClimb` 은 첫 틱 false (수평 충돌 없음).

**핵심 결과**: shift+grab 누름 시 거의 모든 상황에서 동시 활성:
- `wantCrawl=true` (평지/일반 벽 인접)
- `wantClimb=true` (grab 키)

### 1-2. travel — MixinLivingEntityClient.java:226-251

```java
boolean wantFreeClimb = sm.wantClimb;
if (!onClimbable && !sm.isCeilingClimbing && !wantFreeClimb) {
    SmartMovingMover.handleLand(...);   // 정상 land 처리
    return;
}

if (onClimbable || wantFreeClimb) SmartMovingClimber.handleClimbing(...);
SmartMovingClimber.handleCeilingClimbing(...);

if (!sm.isClimbing && !sm.isCeilingClimbing && !onClimbable) {
    SmartMovingMover.handleLand(...);   // fallback land
    return;
}
// ↓ 위 두 분기 모두 미통과 시 climbing motion code 진입 (L280+)
```

**케이스 분기**:

| 시나리오 | onClimbable | wantFreeClimb | handleLand? | climb pipeline? |
|----------|------------|----------------|-------------|------------------|
| 평지 (어떤 climbable 도 없음) | false | true (grab) | ✅ (L247 fallback) | ❌ |
| **사다리/덩굴 인접 + 정면 아님** | **true** | **true** | **❌** | **✅** |
| 사다리/덩굴 인접 + 정면 | true | true | ❌ | ✅ (climbing 정상) |

→ **사다리/덩굴 인접 (정면 아닌 케이스) + shift+grab → climb pipeline 진입** (handleLand 차단).

---

## 2. BUG 원인 분석

### 2-1. **속도 거의 안 움직임** [HIGH] — climb pipeline + sneak motionY=0

**조건**: `사다리/덩굴 인접 + 정면 아님 (옆/뒤) + shift+grab (W 없음)`.

**경로**:
- `wantCrawl=true` → `isCrawling=true`.
- `wantClimb=true` → `wantFreeClimb=true`.
- `onClimbable=true` (인접 ladder/vine 8방향 검출).
- `handleClimbing` 진입:
  - `wantClimbUp = wantClimb && forward>0` = false (W 없음).
  - `wantClimbDown = wantClimb && forward<=0 && !wantCrawl` = false (`wantCrawl=true` → `!wantCrawl=false`).
  - L760 `if (!wantClimbUp && !wantClimbDown) return;` → `isClimbing` set 안 됨.
- **MixinLivingEntityClient L247**: `!isClimbing && !isCeilingClimbing && !onClimbable` → `onClimbable=true` 라 **false** → handleLand SKIP.
- climb pipeline (L280+) 진입:
  - `climbSpeedFactor *= climbSlowFactor (= cfg.crawlFactor = 0.15)`
  - `updateVelocity(climbRawSpeed * climbSpeedFactor, movementInput)` — 작은 가속 (0.0216 × 0.15 ≈ 0.00324)
  - L331 `if (onClimbable) { x/z ±0.15 clamp }`
  - L362-388 sneak 가드:
    ```java
    if (sneakKeyPressed && my < 0) my = 0;
    ```
    **shift 누름 + motionY<0 (gravity) → motionY=0 강제**.
  - L350-352 damping 0.91.

**증상**:
- 수평: 0.15× 크롤 + climbing path 의 추가 0.91 damping → 매우 느림.
- 수직: gravity 방지 (`motionY=0`) → 떨어지지도 않음.
- 사용자 인지: "거의 안 움직임".

**원인 매핑 결함**: `wantFreeClimb=true` 가 `wantCrawl` 보다 우선되어 climb pipeline 진입. 원본 SmartMovingSelf 흐름은 isCrawling 시 land 분기 (handleLand) 우선이어야 함.

---

### 2-2. **콜리전 더 줄어듦 / 시각 작아 보임** [HIGH] — `(isCrawling && isClimbing)` 분기

**MixinPlayerEntityClient.java:88-91**:
```java
if (sm.isClimbCrawling || (sm.isCrawling && sm.isClimbing)) {
    cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(1.62F));
    return;
}
```

**MixinPlayerEntityClient.java:170**:
```java
if (sm.isCrawling && !sm.isClimbing) {
    player.setPose(EntityPose.SWIMMING);
    ci.cancel();
}
```

`isCrawling && isClimbing` 동시 시:
- dimensions = 0.6 × 0.8 + eyeHeight **1.62F** (STANDING-style).
- POSE = vanilla 통과 (STANDING).

**시각**: 0.8 박스 안에 1.8 키 STANDING 모델 + 카메라 1.62 (박스 천장 0.8 의 2배 위) → 모델이 박스 위로 튀어나오고 카메라가 박스 보다 위. 사용자 인지 "박스가 작아 보임".

**isCrawling && isClimbing 동시 활성 트리거**:
- shift+grab+W + 일반 벽 인접 (handsClimbing/feetClimbing 8방향 detect):
  - tick N: tickEssential 에서 `canCrawl` 이 OLD `isClimbing=false` 사용 → `isCrawling=true`. travel 에서 `handleClimbing` 의 BASE_HOLD 등 sub-branch 매치 → `isClimbing=true`.
  - **Tick N 종료 시점**: 둘 다 true → mixin `(isCrawling && isClimbing)` 분기 활성.
- tick N+1: tickEssential 의 `canCrawl` 이 NEW `isClimbing=true` → `canCrawl=false` → `isCrawling=false`. → 1 틱 진동 (mixin 잠깐 STANDING POSE).

### 2-3. **shift 릴리즈해도 해제 안 됨** [MID] — 다중 가능 경로

#### 가능 경로 A: 정상 식 — 평지에서는 정상 동작 (BUG 아님)

- `inputContinueCrawl = sneak || (!freeClimb && grab)`.
- shift 릴리즈 → `sneak=false`. freeClimb=true (default) → `(!freeClimb && grab)=false`.
- `inputContinueCrawl=false`, `contextContinueCrawl=false` (어차피 평지) → `wouldWantCrawl=false`.
- `wantCrawl=false`, `mustCrawl=false` (평지).
- `isCrawling = canCrawl && (false || false) = false`. → 정상 해제.

#### 가능 경로 B: `freeClimb=false` 또는 grab 계속 누름 + 강제 유지

```java
inputContinueCrawl = isCrawlToggleEnabled
    ? crawlToggled
    : (sneakPressedRaw || (!freeClimbingEnabled0 && grabHeld0));
```

- 사용자 freeClimb 비활성화 (cfg.freeClimb=false) + grab 계속 누름 → `(!freeClimb && grab) = true` → `inputContinueCrawl=true` 유지 → `wantCrawl=true` → **shift 릴리즈해도 grab 만으로 크롤 유지**.
- default 는 freeClimb=true 라 평소는 발동 X.

#### 가능 경로 C: `mustCrawl=true` 우발적 활성

- ladder/vine/덩굴 천장 ≤1 블록 위 → `crawlStandUpCeiling - crawlStandUpBottom < playerHeight - heightOffset` 발동.
- `playerHeight = player.getDimensions(STANDING).height()` — **단, 우리 mixin 이 STANDING 쿼리에도 0.6×0.8 (height=0.8) 반환**.
  - 원본: `playerHeight=1.8`, `heightOffset=-1` → 임계 `1.8-(-1)=2.8`.
  - 1.21.1: `playerHeight=0.8`, `heightOffset=-1` → 임계 `0.8-(-1)=1.8`.
  - 박스 위 1.1 ceiling 검색 위치도 다름 (1.21.1 박스가 낮음 → 검색 범위 더 낮음).
  - 우연히 평지에서는 결과 동일 (mustCrawl=false), 하지만 **천장 가까이에서는 임계 0.8 차이 → 정상 일어설 수 있는 곳에서도 mustCrawl=true 발동 가능**.
- 이 경로로 sneak 릴리즈해도 `mustCrawl=true` → `isCrawling=true` 유지.

#### 가능 경로 D: `(isCrawling && isClimbing)` 진동 후 stale 잔존

- 2-2 의 진동 후 `isClimbing` 매 틱 reset → `isClimbing=false` (한 틱 뒤). canCrawl 정상화. wantCrawl 재계산.
- 진동 자체로 stuck 상태 진입은 안 함. **3rd 증상 직접 원인 아닌 것으로 추정**.

---

## 3. 종합 진단

### 가장 가능성 높은 시나리오

1. **사용자가 사다리/덩굴 (또는 일반 climbable) 인접에서 shift+grab → 정면 X**:
   - 1번 증상: climb pipeline 진입 + sneakKey 가드 → motionY=0 (수직 막힘) + 0.15 clamp + 크롤 factor 0.15 → "거의 안 움직임".
   - 2번 증상: 천장 가까운 곳이면 `mustCrawl` 식의 `playerHeight` 매핑 결함으로 정상 케이스에서도 발동 가능 → 시각 박스가 "더 작은" 인지 (실제로는 0.8 fixed, 사용자 perception).
   - 3번 증상: 천장 부근 + mustCrawl 잘못 활성 → shift 릴리즈해도 isCrawling 유지.

2. **벽 (사다리/덩굴 아님) 향해 shift+grab+W**:
   - `(isCrawling && isClimbing)` 1 틱 진동 → STANDING POSE + 0.8 박스 + eye 1.62 → "박스 작음" 시각.

---

## 4. 원본 SmartMovingSelf 의도된 동작

원본 (1.7.10) — Land vs Climb 우선순위:
- `superMoveEntityWithHeading` 에서 `handleSwimming/Lava` → `handleLand` → `handleClimbing` 호출 순서.
- `handleClimbing` 은 motionY 만 set (이전 motionX/Z 보존).
- `handleLand` 가 항상 호출 (handleClimbing 의 결과와 OR 처리).

1.21.1 의 결함:
- `if (!onClimbable && !wantFreeClimb) handleLand` 으로 land 호출 자체를 wantFreeClimb 기반으로 차단.
- 원본은 land + climb 둘 다 항상 적용 (어느 것이 우선이 아님).

---

## 5. 권장 수정 (제안)

### Fix A — climb pipeline 진입 가드에 `!isCrawling` 추가 [HIGH 우선]

**MixinLivingEntityClient.java:226-234**:
```java
// 변경 전
boolean wantFreeClimb = sm.wantClimb;
if (!onClimbable && !sm.isCeilingClimbing && !wantFreeClimb) {
    SmartMovingMover.handleLand(player, sm, movementInput);
    ci.cancel();
    return;
}

// 변경 후 — isCrawling 시 land 분기 강제 (climb 우선 차단)
boolean wantFreeClimb = sm.wantClimb && !sm.isCrawling;
if (!onClimbable && !sm.isCeilingClimbing && !wantFreeClimb) {
    SmartMovingMover.handleLand(player, sm, movementInput);
    ci.cancel();
    return;
}
```

**효과**:
- `isCrawling=true` 시 climb pipeline 진입 차단 → handleLand 호출 → 정상 크롤 속도.
- adjacent climbable + shift+grab + 정면 아님 → land 처리 (사용자 의도 = 그냥 엎드리기).

**부작용**:
- 본격 climbing 전 1 틱 isCrawling=true 잔존 시 climb pipeline 차단. 하지만 climbing 진입은 W 또는 jump 키 필요 → 그 때는 isCrawling 자체가 해제 (canCrawl `!isClimbing` 가드). 흐름상 안전.

---

### Fix B — `(isCrawling && isClimbing)` mixin 분기 단순화 [MID 우선]

**MixinPlayerEntityClient.java:88-91**:

`(isCrawling && isClimbing)` 분기 자체가 1 틱 진동 의 결과 → fix A 가 진동 자체를 차단하면 이 분기 도달 안 됨. 그래도 안전망:

```java
// 변경 전
if (sm.isClimbCrawling || (sm.isCrawling && sm.isClimbing)) {
    cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(1.62F));
    return;
}

// 변경 후 — isCrawling 우선 (SWIMMING POSE + eye 0.62 동등)
if (sm.isClimbCrawling) {
    cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(1.62F));
    return;
}
// isCrawling && isClimbing 케이스는 아래 smSmall 분기로 자연 처리 (eye 0.62 + SWIMMING POSE)
```

**근거**: 원본 1.7.10 `setHeightOffset(-1F)` 만 box 변경, eyeHeight 변경 없음 (POSE 시스템 없음). isClimbCrawling 의 STANDING 시각 유지는 의도된 동작 (사다리 매달림). 그러나 isCrawling 시는 항상 SWIMMING POSE + eye 0.62 가 원본 1:1.

`isClimbCrawling` 해제 엣지의 `toCrawling()` 1 틱 잔존은 별도 가드 필요 시 `(sm.isCrawling && sm.wasClimbing)` 같은 패턴 검토.

---

### Fix C — `mustCrawl` 식 의 `playerHeight` 1.21.1 보정 [LOW 우선]

**SmartMovingClientState.java:1179**:

`player.getDimensions(EntityPose.STANDING).height()` 가 mixin 으로 0.8 반환 (크롤 중) → 임계 1.8. 원본 임계는 2.8.

**옵션 1**: vanilla 1.21.1 STANDING height 직접 (1.8F):
```java
float playerHeight = 1.8F;  // vanilla EntityType.PLAYER STANDING height — mixin 영향 없음
```

**옵션 2**: ceiling 검색 범위에 `-heightOffset` 보정 (검색 범위를 원본 좌표계로):
```java
double crawlStandUpCeiling = SmartMovingClimber.getMinPlayerSolidBetween(player,
        maxYR - heightOffset, maxYR - heightOffset + 1.1D, 0);
double crawlStandUpBottom = getMaxPlayerSolidBetween(player,
        minYR - heightOffset - (initializeCrawling ? 0D : 1D),
        minYR - heightOffset, horizontalTolerance);
```

옵션 1 이 안전 (1 줄 수정). 천장 정상 일어설 자리에서 mustCrawl 잘못 발동 차단.

---

### Fix D — `inputContinueCrawl` 의 grab 의존 [LOW 우선]

`(!freeClimb && grab)` 항. default freeClimb=true 라 발동 안 함. 하지만 사용자가 freeClimb 비활성 시 shift 릴리즈해도 grab 만으로 크롤 유지 → 가능 경로 B 의 직접 원인.

원본 1:1 매핑이므로 유지 (사용자가 freeClimb 비활성 의도하면 그게 정상). 사용자 보고 시 freeClimb 설정 확인 권장.

---

## 6. 우선순위 / 영향 매트릭스

| 우선 | 항목 | 매핑 결함 정도 | 사용자 영향 | 매핑 줄 수 |
|------|------|---------------|------------|------------|
| **HIGH** | Fix A — `wantFreeClimb && !isCrawling` 가드 | 큼 (원본 동작 위반) | 1, 2번 증상 직접 해소 | 1 줄 |
| **MID** | Fix B — `(isCrawling && isClimbing)` mixin 단순화 | 중 (1 틱 진동 시각 결함) | 1번 증상 일부 해소 | 4 줄 |
| **LOW** | Fix C — `playerHeight` 보정 | 작음 (천장 부근 edge case) | 3번 증상 일부 (천장) | 1 줄 |
| **참고** | Fix D — `inputContinueCrawl` freeClimb 의존 | 0 (원본 1:1) | 사용자 설정 의존 | 변경 X |

---

## 7. 검증 필요 (인게임)

1. **Fix A 적용 후**: ladder/vine 정면 X + 인접 + shift+grab → 평지 크롤 속도 동등 검증. Climbing W → 정상 등반 검증 (canCrawl `!isClimbing` 가드로 자동 해제).
2. **Fix B 적용 후**: 일반 벽 + shift+grab+W → STANDING POSE 1 틱 진동 해소 검증.
3. **Fix C 적용 후**: 1 블록 천장 가까이 (크롤 진입 가능, 일어설 수 있는 자리) + sneak 릴리즈 → 정상 일어남 검증.
4. **회귀 점검**: 사다리/덩굴 정면 등반 정상, 천장 매달림 정상, 슬라이딩 정상, 비행 정상.
