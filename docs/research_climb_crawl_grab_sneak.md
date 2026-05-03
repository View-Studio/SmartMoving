# 클라이밍/비행/엎드리기 3 기능 — 원본 SmartMoving 1.7.10 라인별 리서치

작업 범위: **원본 1.7.10 SmartMoving 소스 라인별 직접 확인**. 우리 1.21.1 매핑과의 비교/수정은 별도 작업 (이 문서에는 비교 노트만 표시, 수정 미수행).

원본 위치: `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\`
- `SmartMovingSelf.java` (3345 줄) — 핵심 로직.
- `SmartMovingBase.java` (932 줄) — 헬퍼 (isPlayerInSolidBetween 등).
- `Orientation.java` — 8 방향 등반 탐색 (seekClimbGap).
- `ClimbGap.java` — 갭 데이터 클래스.

사용자 보고 3 기능:
1. **클라이밍 + grab + 한 칸 공간 + sneak → 그 한 칸 공간으로 클라이밍하면서 엎드리기**.
2. **비행 → 1 칸 공간 → 비행 풀림 → 바로 엎드리기**.
3. **엎드린 채 블록 앞에서 grab 누름 → 엎드린 채 클라이밍**.

---

## 결론 요약

| 기능 | 원본 변수 | 원본 라인 | 존재 |
|------|-----------|-----------|------|
| 1 | `isClimbCrawling` + `hasClimbCrawlGap` + `climbIntoCount` | L2786-L2820 | ✅ |
| 2 | `restoreFromFlying` (로컬) + `standupIfPossible` + `toSlidingOrCrawling` | L2186-L2230, L2507-L2544 | ✅ |
| 3 | `isCrawlClimbing` | L2736-L2784 | ✅ |

**3 가지 모두 원본에 명확히 존재**. 라인별 인용 + 흐름 + 의존 변수 아래에 정밀 분석.

---

## 기능 1 — `isClimbCrawling` (한 칸 갭 자동 등반 진입)

### 사용자 의도
"grab 누르면서 클라이밍하다가 한 칸 공간이 있는데 그 때 shift 를 누르면 바로 그 한 칸 공간으로 클라이밍하면서 엎드리기되는 기능".

### 원본 핵심 식 — `SmartMovingSelf.java:2786-2795`

```java
boolean wasClimbCrawling = isClimbCrawling;
boolean needClimbCrawling = hasClimbCrawlGap || (hasClimbGap && isClimbHolding);
boolean canClimbCrawling = wantClimbHolding && wantClimbUp;

if (climbIntoCount > 1)
    climbIntoCount--;
else if (isClimbCrawling && !needClimbCrawling && climbIntoCount == 0)
    climbIntoCount = 6;

isClimbCrawling = canClimbCrawling
    && ((needClimbCrawling && climbIntoCount == 0) || climbIntoCount > 1);
```

**`needClimbCrawling`** — "좁은 갭이 필요하다"는 신호:
- `hasClimbCrawlGap` — 8 방향 탐색 결과 좁은 (crawl-only) 갭 발견.
- `hasClimbGap && isClimbHolding` — 일반 갭 + sneak hold 클라이밍 중.

**`canClimbCrawling`** — 사용자 입력이 진입 의도:
- `wantClimbHolding` — sneak / crawlToggle hold + grab + 등반 가능 표면.
- `wantClimbUp` — `wantClimb && forward > 0` (기본 분기) 또는 vine + jump.

**`climbIntoCount`** (필드 L3089, `private int climbIntoCount`) — 6 틱 카운트다운:
- 진입 후 6 → 5 → ... → 0 → 자동 해제.
- `needClimbCrawling=false` (갭 통과 후) + isClimbCrawling 활성 → 카운터 6 으로 재충전.
- 카운터 > 1 동안은 needClimbCrawling 무관하게 isClimbCrawling 유지.

### 진입 엣지 — L2796-2803

```java
if (isClimbCrawling && !wasClimbCrawling)
{
    setHeightOffset(-1F);

    boolean wasCollidedHorizontally = sp.isCollidedHorizontally;  // preserve
    move(0, 0.05, 0, true);  // SMP Illegal Stance 방지 — 머리 위 솔리드 시 밀어내기
    sp.isCollidedHorizontally = wasCollidedHorizontally;  // water 탈출 방지
}
```

- `setHeightOffset(-1F)` (L1694-L1704) → `boundingBox.minY -= -1 = +1` (박스 1 블록 위) + `height += -1` (1.8 → 0.8).
- `move(0, 0.05, 0, true)` — 작은 위 이동. 천장 솔리드 충돌 시 밀려남.
- `isCollidedHorizontally` 보존 — 수면 위 등반 종료 후 박스 변경 시 충돌 플래그 유지.

### 해제 엣지 — L2804-L2820

```java
else if (!isClimbCrawling && wasClimbCrawling)
{
    climbIntoCount = 0;
    if (mustCrawl || sneakButton.Pressed || crawlToggled)
    {
        double gapUnderneight = sp.boundingBox.minY
            - getMaxPlayerSolidBetween(sp.boundingBox.minY - 1D, sp.boundingBox.minY, 0);
        if (gapUnderneight >= 0D && gapUnderneight < 1D)
        {
            wasCrawling = toCrawling();          // ← 엎드리기 진입
            move(0, (-gapUnderneight), 0, true); // 바닥 스냅
        }
        else
            resetHeightOffset();
    }
    else
        resetHeightOffset();
}
```

**핵심**: 좁은 갭 통과 후 `mustCrawl || sneak || crawlToggled` 시 → **자동 엎드리기 전환**. 이게 사용자 의도 "한 칸 공간으로 클라이밍하면서 엎드리기" 의 정확한 매핑.

### 의존 변수

#### `wantClimbHolding` — L2721-2728

```java
boolean wantClimbHolding =
    (isClimbHolding && sneakButton.Pressed) ||
    (isClimbing && blocked) ||
    (wantClimb && !isSwimming && !isDiving && !isCrawling
        && (sneakButton.Pressed || crawlToggled));
```

- `isClimbHolding && sneak` — 이미 매달림 hold 중 sneak 유지.
- `isClimbing && blocked` — 등반 중 + UI screen 열림 (입력 차단).
- `wantClimb && !crawl && (sneak || crawlToggle)` — 일반 케이스 (사용자 첫 진입).

**중요**: forward 가드 없음. forward>0 일 때도 활성 가능.

#### `wantClimb` / `wantClimbUp` — L2467-2493

```java
wouldWantClimb =
    (grabButton.Pressed
        || (isClimbHolding && sneakButton.Pressed)
        || (Config.isFreeClimbAutoLaddderEnabled() && isFacedToLadder(isClimbCrawling))
        || (Config.isFreeClimbAutoVineEnabled() && isFacedToSolidVine(isClimbCrawling)))
    && (!isSliding || (grabButton.Pressed && esp.movementInput.moveForward > 0F))
    && !isHeadJumping
    && !wantCrawlNotClimb
    && !disabled;

wantClimb = Config.isFreeClimbingEnabled() && wouldWantClimb;

wantClimbUp =
    (wantClimb && esp.movementInput.moveForward > 0F) ||
    ((isVineAnyClimbing && jumpButton.Pressed
        && !(sneakButton.Pressed && isFacedToSolidVine))
        && (!isCrawling || sp.isCollidedHorizontally)
        && (!isSliding  || sp.isCollidedHorizontally));

wantClimbDown = wantClimb && esp.movementInput.moveForward <= 0F && !wantCrawl;
```

#### `hasClimbGap` / `hasClimbCrawlGap` — L949-961

8 방향 탐색 (4 직각 + 4 대각, isSmallClimbing 시 대각 skip) `Orientation.seekClimbGap` 호출 후:

```java
hasClimbGap = out_handsClimbGap.CanStand || out_feetClimbGap.CanStand;
hasClimbCrawlGap = out_handsClimbGap.MustCrawl || out_feetClimbGap.MustCrawl;
```

- `ClimbGap.CanStand` — 그 갭이 standing 박스 (1.8 height) 도 들어갈 만큼 큰지.
- `ClimbGap.MustCrawl` — crawl 박스 (0.8 height) 만 들어갈 수 있는 좁은 갭.
- 따라서 `hasClimbCrawlGap=true` = **8 방향 어딘가에 한 칸 좁은 공간 발견**.

### handleClimbing 의 좁은 갭 등반 분기 — L996-999

```java
else if ((hasClimbGap || hasClimbCrawlGap)
    && handsClimbing == HandsClimbing.FastUp
    && (feetClimbing == FeetClimbing.None || feetClimbing == FeetClimbing.BaseWithHands))
{
    // climb into crawl gap
    setShouldClimbSpeed(
        feetClimbing == FeetClimbing.None ? SlowUpMotion : FastUpMotion,
        HandsClimbing.MiddleGrab,
        FeetClimbing.DownStep);
}
```

- 한 손 빠르게 위 (FastUp) + 발 미접지 또는 박스 아래 손과 함께 (BaseWithHands) → 좁은 갭 진입 신호.
- 애니메이션: 손 `MiddleGrab` (한 손이 가운데 잡음) + 발 `DownStep`.
- 속도: 발 미접지 시 SlowUp (0.10), 발 BaseWithHands 시 FastUp (0.18).

### `setShouldClimbSpeed` 의 climbIntoCount 처리 — L1517-1518

```java
if (this.climbIntoCount > 0)
{
    value = HoldMotion;  // 진입 후 6 틱 동안 정지 (grip 확보)
    ...
}
```

진입 직후 climbIntoCount=6 → 6 틱 동안 motionY=HoldMotion (정지). 그 후 자연 등반 재개.

### 호출 흐름 도식

```
매 틱 tickEssential:
  ├─ 8 방향 seekClimbGap → hasClimbGap / hasClimbCrawlGap / isNeighborClimbing 갱신.
  ├─ wantClimbHolding 식 → isClimbHolding (= wantClimbHolding && isClimbing).
  ├─ wantClimbUp/Down 식 (forward, jump, vine).
  ├─ isCrawlClimbing 식 (기능 3, 별도).
  ├─ ★ wasClimbCrawling = isClimbCrawling.
  ├─ ★ needClimbCrawling / canClimbCrawling 계산.
  ├─ ★ climbIntoCount 카운터 갱신.
  ├─ ★ isClimbCrawling 새 값.
  ├─ ★ 진입/해제 엣지 처리 (setHeightOffset / move / toCrawling / resetHeightOffset).
  └─ handleClimbing → setShouldClimbSpeed → motionY 적용.
```

---

## 기능 2 — 비행 종료 + 1 칸 공간 → 즉시 엎드리기

### 사용자 의도
"비행하다가 1 칸 공간에서 비행이 풀리면 바로 엎드리기 되는 기능".

### 원본 핵심 — `SmartMovingSelf.java:2507-2544`

```java
boolean restoreFromFlying = false;       // ← 매 틱 reset 되는 로컬 변수

boolean wasFlying = isFlying;
isFlying = Config.isFlyingEnabled() && sp.capabilities.isFlying && !isSwimming && !isDiving;
if (isFlying && !wasFlying)
    setHeightOffset(-1);                  // 비행 진입 엣지 → 박스 작게
else if (!isFlying && wasFlying)
    restoreFromFlying = true;             // ★ 비행 종료 엣지

if (!Config.isFlyingEnabled() && Config.isLevitateSmallEnabled())
{
    if (isLevitating && !wasLevitating)
        setHeightOffset(-1);
    else if (!isLevitating && wasLevitating)
        restoreFromFlying = true;         // ★ Levitate 종료 엣지
}

wasHeadJumping = isHeadJumping;
isHeadJumping = isHeadJumping
    && !sp.onGround
    && !(isSwimming || isDiving)
    && !(isFlying || sp.capabilities.isFlying)
    && !(sp.handleWaterMovement() && sp.motionY < 0)
    && !sp.handleLavaMovement();

if (!isHeadJumping)
    isAerodynamic = false;

if (wasHeadJumping && !isHeadJumping)
    if (sp.onGround)
    {
        handleCrash(_headFallDamageStartDistance, _headFallDamageFactor);
        restoreFromFlying = true;         // ★ 헤드점프 종료 엣지
    }

boolean tryLanding = isFlying && !Options._flyCloseToGround.value
    && horizontalSpeedSquare < 0.003D && sp.motionY > -0.03D;

if (restoreFromFlying || tryLanding)
    standupIfPossible(tryLanding, restoreFromFlying);
```

### `standupIfPossible(boolean, boolean)` 본체 — L2186-L2211

```java
private void standupIfPossible(boolean tryLanding, boolean restoreFromFlying)
{
    if (heightOffset >= 0)
        return;                                      // 가드: 비행/크롤 등 small state 만 진행

    double gapUnderneight = getGapUnderneight();
    boolean groundClose = gapUnderneight < 1D;
    double gapOverneight = groundClose ? getGapOverneight() : -1D;
    boolean standUpPossible = gapUnderneight + gapOverneight >= 1D;

    if (tryLanding && groundClose && standUpPossible)
    {
        isFlying = false;
        sp.capabilities.isFlying = false;            // vanilla 비행 해제
        restoreFromFlying = true;                    // 로컬 인자만 변경
    }

    if (!restoreFromFlying)
        return;

    if (!groundClose && !sneakButton.Pressed)
        resetHeightOffset();                          // (a) 공중 + sneak X → 서기 시도
    else if (standUpPossible && !(sneakButton.Pressed && grabButton.Pressed))
        standUp(gapUnderneight);                      // (b) 한 칸 이상 + sneak+grab 동시 X → 일어섬
    else
        toSlidingOrCrawling(gapUnderneight);          // (c) 좁은 천장 OR sneak+grab → 엎드리기/슬라이드
}
```

**3 분기 결정 로직**:
| 조건 | 분기 |
|------|------|
| !groundClose && !sneak | (a) `resetHeightOffset()` — 박스만 standing 으로 복원 |
| standUpPossible && !(sneak && grab) | (b) `standUp(gap)` — 일어섬 + 바닥 스냅 |
| 그 외 | (c) `toSlidingOrCrawling(gap)` — 엎드리기 또는 슬라이드 |

### `standUp(gap)` — L2214-2220

```java
private void standUp(double gapUnderneight)
{
    move(0, (1D - gapUnderneight), 0, true);  // entity.y += 1-gap (가능한 만큼 위로)
    isCrawling = false;
    isHeadJumping = false;
    resetHeightOffset();                       // 박스 0.8 → 1.8 복원
}
```

### `toSlidingOrCrawling(gap)` — L2222-2230

```java
private void toSlidingOrCrawling(double gapUnderneight)
{
    move(0, (-gapUnderneight), 0, true);       // entity.y -= gap (바닥 스냅)

    if (Config.isSlidingEnabled() && (grabButton.Pressed || wasHeadJumping))
        isSliding = true;                       // grab + sliding cfg 시 슬라이드
    else
        wasCrawling = toCrawling();             // ★ 엎드리기 진입
}
```

`toCrawling()` (L3047 근처):
- `isCrawling = true`.
- `Options.isCrawlToggleEnabled() ? crawlToggled = true : ...`.

### **사용자 의도 정확 매칭 시나리오**

비행 중 (heightOffset=-1, box 0.8) 좁은 1 칸 공간으로 진입한 상태에서 비행 해제:
1. T=N: `wasFlying=true`, `isFlying=false (사용자가 vanilla 비행 off)` → **L2513 `restoreFromFlying = true`**.
2. T=N: `tryLanding=false` (이미 isFlying=false), `restoreFromFlying=true` → **L2544 `standupIfPossible(false, true)`** 호출.
3. standupIfPossible:
   - `heightOffset = -1 < 0` 가드 통과.
   - groundClose 측정 → 1 칸 공간이라 gapUnderneight ≈ 0 → groundClose=true.
   - gapOverneight 측정 → 천장 1 블록 위에 있어 gapOver < 1 → standUpPossible=false.
   - `tryLanding=false` 첫 if skip.
   - `!restoreFromFlying=false` 통과.
   - `!groundClose && !sneak` = false (groundClose=true).
   - `standUpPossible && !(sneak && grab)` = false (standUpPossible=false).
   - **else `toSlidingOrCrawling(gap)`** 매치.
4. toSlidingOrCrawling:
   - `move(0, -gap, 0)` 바닥 스냅.
   - `slidingEnabled && (grab || wasHeadJumping)` — grab 안 누르면 → else 분기 → `wasCrawling = toCrawling()` → **isCrawling=true**.
5. T=N+1: `restoreFromFlying` 로컬 변수 사라짐 (다음 틱 false 새로). 정상 엎드리기 상태.

### **stale 잔존 불가**

`restoreFromFlying` 은:
- L2507 매 틱 시작 시 **로컬 변수 `boolean restoreFromFlying = false`** 선언.
- L2186 `standupIfPossible(boolean, boolean)` **인자 pass-by-value** — 호출자에 영향 없음.
- 그 틱 종료 시 변수 사라짐 → 다음 틱 다시 false.

→ 우리 1.21.1 매핑이 필드로 승격하면서 stale BUG 도입한 것은 **우리 매핑 특유의 결함**.

### `getGapUnderneight` / `getGapOverneight` 정의 (참고)

`SmartMovingBase` 추정 (Self 에서 호출됨):
- `getGapUnderneight()` = `boundingBox.minY - getMaxPlayerSolidBetween(minY - 1.1, minY, 0)`.
- `getGapOverneight()` = `getMinPlayerSolidBetween(maxY, maxY + 1.1, 0) - maxY`.

박스 발 아래/머리 위 1.1 블록 범위의 가장 가까운 솔리드까지 거리.

---

## 기능 3 — `isCrawlClimbing` (엎드린 채 등반)

### 사용자 의도
"엎드린 상태로 블록 앞에서 grab 을 누르고 있으면 엎드린 상태로 클라이밍이 되는 기능".

### 원본 핵심 식 — `SmartMovingSelf.java:2736-2737`

```java
boolean wasCrawlClimbing = isCrawlClimbing;
isCrawlClimbing = (wasCrawling || isCrawlClimbing)
    && isClimbing
    && isNeighborClimbing
    && (sneakButton.Pressed || crawlToggled)
    && esp.movementInput.moveForward > 0F;
```

**조건 5 가지 모두 동시 매치 필요**:
1. `wasCrawling || isCrawlClimbing` — **엎드리기 상태에서 시작 OR 이미 엎드려 등반 중**.
2. `isClimbing` — `setShouldClimbSpeed` 매치 시 set (grab + 등반 가능 표면 + wantClimbUp/Down 매치).
3. `isNeighborClimbing` — 4 직각 인접에 등반 가능 표면 (handsClimbing.relevant || feetClimbing.relevant, L945).
4. `sneak hold || crawlToggled` — 사용자 입력 유지.
5. `forward > 0` — W 누름.

### 활성 시 처리 — L2738-2754

```java
if (isCrawlClimbing)
{
    boolean canStandUp = !isPlayerInSolidBetween(
        sp.boundingBox.minY - (isClimbCrawling ? 0.95D : 1D),
        sp.boundingBox.minY);

    if (canStandUp)
    {
        // 머리 위 1 블록 (또는 0.95 if 이미 ClimbCrawl) 비어있음 → 일어설 수 있음.
        wasCrawlClimbing = false;
        isCrawlClimbing = false;
        if (!isClimbCrawling)
            resetHeightOffset();                  // 박스 standing 복원
    }

    if (!wasCrawlClimbing)
    {
        // 진입 엣지 (이번 틱 첫 활성): SM crawl 모드 종료 (heightOffset 만 유지)
        wasCrawling = false;
        isCrawling = false;
    }
}
```

**의미**:
- 등반 중 위로 일어날 공간 발견 시 자동 해제.
- 진입 엣지에 `isCrawling=false` set — SM crawl 메인 식이 부작용 일으키지 않게 헤제.
- 박스는 setHeightOffset(-1) 그대로 유지 (heightOffset=-1 from previous crawl) → 작은 박스 등반.

### 해제 엣지 — L2755-2784

```java
else if (wasCrawlClimbing)
{
    boolean toCrawling = sneakButton.Pressed || crawlToggled;
    if (!isClimbing)
    {
        // (a) 등반 종료 (isClimbing=false) → 다시 엎드리기 모드 + 바닥 스냅
        wasCrawling = toCrawling();
        double minY = sp.boundingBox.minY;
        move(0, (-minY + Math.floor(minY)), 0, true);  // 정수 y 로 스냅
    }
    else if (esp.movementInput.moveForward <= 0F)
    {
        // (b) W 해제 (등반 중 정지) → sneak hold 시 엎드리기 유지, 아니면 서기
        wasCrawling = toCrawling;
        isCrawling = toCrawling;
        wantClimbUp = false;
        wantClimbDown = false;
        if (!toCrawling)
            resetHeightOffset();
        double minY = sp.boundingBox.minY;
        move(0, (-minY + Math.floor(minY) + (toCrawling ? 0F : 1F)), 0, true);
    }
    else if (!toCrawling)
    {
        // (c) sneak/toggle 해제 (계속 forward 등반) → 일어서서 등반 (= isClimbCrawling 으로 자연 전환)
        resetHeightOffset();
        double minY = sp.boundingBox.minY;
        move(0, (Math.ceil(minY) - minY), 0, true);
    }
}
```

### 헬퍼 — `isPlayerInSolidBetween` (`SmartMovingBase.java:380`)

```java
protected boolean isPlayerInSolidBetween(double yMin, double yMax)
{
    // boundingBox 의 X/Z 범위 + Y[yMin, yMax] 영역에 솔리드 블록 충돌 있는지.
}
```

`canStandUp = !isPlayerInSolidBetween(minY - 1, minY)` = "박스 발 아래 1 블록 영역 (= 일어선 박스의 발 아래 부분)에 솔리드 없음" = 일어설 수 있음.

`isClimbCrawling=true` 시 0.95 사용 — climbCrawl 박스가 약간 다른 위치라 미세 보정.

### 의존 변수

#### `wasCrawling` 갱신 시점
- L2435 / L2441 (원본 다양) — `wasCrawling = isCrawling` 매 틱 저장. isCrawling 새 값 set 직전.

#### `isClimbing` set 경로 — `setOnlyShouldClimbSpeed` (L1515)

```java
isClimbing = true;  // 무조건 set when called.
```

호출 경로: handleClimbing (L979) `if (feetClimbing.IsRelevant() || handsClimbing.IsRelevant())` 내부의 wantClimbUp/Down 분기 매치 시.

#### `isNeighborClimbing` (L945)

```java
isNeighborClimbing = handsClimbing != HandsClimbing.None || feetClimbing != FeetClimbing.None;
```

4 직각 (PZ/NZ/ZP/ZN) `seekClimbGap` 결과. 8 방향 결과 `handsClimbing`/`feetClimbing` 의 4 직각 부분만 본 결과.

### 호출 흐름 도식

```
매 틱 tickEssential:
  ├─ 8 방향 seekClimbGap → handsClimbing/feetClimbing/isNeighborClimbing 갱신.
  ├─ handleClimbing → wantClimbUp/Down 매치 시 setShouldClimbSpeed → isClimbing=true.
  ├─ wantClimbHolding / isClimbHolding 갱신.
  ├─ ★ wasCrawlClimbing = isCrawlClimbing.
  ├─ ★ isCrawlClimbing 식 (5 조건 AND).
  ├─ ★ 활성 처리 (canStandUp 검사 → 자동 해제 + isCrawling=false 진입 엣지).
  ├─ ★ 해제 엣지 처리 (등반 종료 / forward 해제 / sneak 해제).
  ├─ wasClimbCrawling = isClimbCrawling.
  ├─ isClimbCrawling 식 (기능 1, 별도).
  └─ isCrawling 식 (canCrawl && (wantCrawl || mustCrawl)).
```

### 기능 1 vs 기능 3 명확한 차이

| 항목 | 기능 1 (`isClimbCrawling`) | 기능 3 (`isCrawlClimbing`) |
|------|----------------------------|----------------------------|
| **시나리오** | 일어선 채 등반 → 좁은 갭 발견 → 좁아진 자세로 갭 통과 | 엎드린 채 등반 표면 만남 → 엎드린 자세로 등반 |
| **주 trigger** | hasClimbCrawlGap (좁은 갭 발견) | wasCrawling (이전 틱 엎드리기) |
| **forward 의존** | `canClimbCrawling = wantClimbHolding && wantClimbUp` (forward>0 필요) | 식에 `forward > 0F` 직접 명시 |
| **sneak 의존** | wantClimbHolding 안의 `(sneak \|\| crawlToggled)` 항 | 식에 `(sneak \|\| crawlToggled)` 직접 명시 |
| **자동 해제 트리거** | climbIntoCount=0 + needClimbCrawling=false (갭 통과 후 카운터 만료) | canStandUp (위로 일어설 공간 발견) |
| **해제 후 처리** | mustCrawl/sneak/toggle 시 자동 toCrawling, 아니면 resetHeightOffset | 등반 종료 시 toCrawling 시도, W 해제 시 sneak 따라 토글, sneak 해제 시 일어섬 |

**둘은 독립적 + 동시 활성 가능**: 좁은 갭에 들어가면서 엎드림 = `isClimbCrawling` 만 활성. 엎드린 채 일반 사다리 등반 = `isCrawlClimbing` 만 활성. 좁은 갭에 엎드린 채 진입 = 둘 다 활성 가능 (각각 식의 조건 매치).

---

## 헬퍼 메서드 정의

### `setHeightOffset(float)` — L1694-L1704

```java
private void setHeightOffset(float offset)
{
    resetHeightOffset();                  // 이전 offset reverse
    if (offset == 0F) return;

    heightOffset = offset;
    sp.boundingBox.minY -= heightOffset;  // -1 면 minY +1 (박스 1 블록 위)
    sp.height += heightOffset;            // -1 면 1.8 → 0.8
}
```

### `resetHeightOffset()` — L1681-L1686

```java
private void resetHeightOffset()
{
    sp.boundingBox.minY += heightOffset;
    sp.height -= heightOffset;
    heightOffset = 0F;
}
```

**원본은 boundingBox/height 직접 조작** — 1.7.10 vanilla 가 EntityPose 시스템 없는 시대 설계. 1.21.1 매핑은 `EntityDimensions.changing(0.6F, 0.8F)` + `calculateDimensions()` 호출로 대체. 이 차이가 우리 매핑의 stale BUG (기능 2) 의 근본 원인.

### `move(double, double, double, boolean)` — vanilla `Entity.moveEntity` 호출

박스 평행이동 + 충돌 처리. 마지막 boolean 인자는 1.7.10 모드 옵션 (정확한 의미는 별도).

---

## 우리 1.21.1 매핑과의 차이 노트 (참고만, 수정 안 함)

> 본 문서의 목적은 원본 라인별 확인. 비교/수정은 별도 작업 단위.

발견된 차이 (검증 필요):

### 차이 1 — `restoreFromFlying` 필드 vs 로컬 변수 (기능 2)

- **원본 L2507**: 매 틱 로컬 변수 `boolean restoreFromFlying = false`.
- **1.21.1**: `SmartMovingClientState.restoreFromFlying` 필드 (L217 근처). stale 잔존 가능 → 2026-04-30 fix 로 standupIfPossible 호출 후 `false` 클리어 추가하여 수동 해소.

원본 1:1 매핑은 **로컬 변수로 환원 + 인자 전달**. 현재 fix 는 효과적 동등 (호출 직후 클리어).

### 차이 2 — `wantClimbHolding` 의 `!forwardPressed_holdGuard` 추가 가드 (기능 1 영향)

- **원본 L2721-2728**: forward 가드 없음.
- **1.21.1 SmartMovingClientState L1384**: `boolean forwardPressed_holdGuard = forward > 0F; wantClimbHolding = !forwardPressed_holdGuard && (...);` 추가.

→ forward>0 시 `wantClimbHolding=false` 강제 → `canClimbCrawling=false` → **`isClimbCrawling` 영구 비활성** → **사용자 보고 기능 1 작동 안 함 가능성**.

이 가드는 우리 측 의도된 추가 ("사다리 등반 + W + sneak → 등반 우선") 였으나 사용자 의도 (좁은 갭 자동 진입) 와 **상충**. 원본 1:1 복원 검토 필요.

### 차이 3 — handleClimbing matched skip + handleLand fallback (다른 기능 영향 가능)

- 우리 매핑은 `if (!matched) return;` 로 미매치 시 setShouldClimbSpeed skip. 원본은 미매치 시도 sub-branch 하나라도 매치되면 호출.
- L996-999 의 좁은 갭 분기 매치 안 되면 isClimbing 안 set → isCrawlClimbing 식의 `isClimbing` 항 false → 기능 3 도 영향 가능.

---

## 검증 필요 항목 (별도 작업)

1. **기능 1 작동 여부 인게임 검증** — 사다리 + grab + sneak + W → 위에 1 블록 갭 있는 자리. 우리 매핑에서 isClimbCrawling 활성되어 좁은 자세로 등반하는지.
2. **기능 2 — fix 후 재현 안 됨** (이미 사용자 검증 완료, 2026-04-30).
3. **기능 3 작동 여부** — 엎드린 채 사다리 정면 + grab + W + sneak → isCrawlClimbing 활성되어 엎드린 채 등반하는지.
4. **차이 2 (`!forwardPressed` 가드) 영향** — 가드 제거 시 기능 1 활성 + 사다리 등반 + W + sneak 동작 변화 (이전 사용자 보고 "쉬프트가 씹히고 올라가는게 우선이 되야됨" 의도와 상충 가능).
5. **차이 3 (matched skip) 영향** — 좁은 갭 분기 (L996) 매치 시 isClimbing set 정상 작동하는지.

---

## 원본 코드 원문 인용 (전체)

### 기능 1 — `SmartMovingSelf.java:2786-2820`
```java
boolean wasClimbCrawling = isClimbCrawling;
boolean needClimbCrawling = hasClimbCrawlGap || (hasClimbGap && isClimbHolding);
boolean canClimbCrawling = wantClimbHolding && wantClimbUp;

if(climbIntoCount > 1)
    climbIntoCount--;
else if(isClimbCrawling && !needClimbCrawling && climbIntoCount == 0)
    climbIntoCount = 6;

isClimbCrawling = canClimbCrawling && ((needClimbCrawling && climbIntoCount == 0) || climbIntoCount > 1);
if(isClimbCrawling && !wasClimbCrawling)
{
    setHeightOffset(-1F);

    boolean wasCollidedHorizontally = sp.isCollidedHorizontally;
    move(0, 0.05, 0, true);
    sp.isCollidedHorizontally = wasCollidedHorizontally;
}
else if(!isClimbCrawling && wasClimbCrawling)
{
    climbIntoCount = 0;
    if(mustCrawl || sneakButton.Pressed || crawlToggled)
    {
        double gapUnderneight = sp.boundingBox.minY - getMaxPlayerSolidBetween(sp.boundingBox.minY - 1D, sp.boundingBox.minY, 0);
        if(gapUnderneight >= 0D && gapUnderneight < 1D)
        {
            wasCrawling = toCrawling();
            move(0, (-gapUnderneight), 0, true);
        }
        else
            resetHeightOffset();
    }
    else
        resetHeightOffset();
}
```

### 기능 2 — `SmartMovingSelf.java:2507-2544`
```java
boolean restoreFromFlying = false;

boolean wasFlying = isFlying;
isFlying = Config.isFlyingEnabled() && sp.capabilities.isFlying && !isSwimming && !isDiving;
if(isFlying && !wasFlying)
    setHeightOffset(-1);
else if(!isFlying && wasFlying)
    restoreFromFlying = true;

if(!Config.isFlyingEnabled() && Config.isLevitateSmallEnabled())
{
    if(isLevitating && !wasLevitating)
        setHeightOffset(-1);
    else if(!isLevitating && wasLevitating)
        restoreFromFlying = true;
}

wasHeadJumping = isHeadJumping;
isHeadJumping = isHeadJumping &&
    !sp.onGround &&
    !(isSwimming || isDiving) &&
    !(isFlying || sp.capabilities.isFlying) &&
    !(sp.handleWaterMovement() && sp.motionY < 0) &&
    !sp.handleLavaMovement();

if(!isHeadJumping)
    isAerodynamic = false;

if(wasHeadJumping && !isHeadJumping)
    if(sp.onGround)
    {
        handleCrash(Config._headFallDamageStartDistance.value, Config._headFallDamageFactor.value);
        restoreFromFlying = true;
    }

boolean tryLanding = isFlying && !Options._flyCloseToGround.value && horizontalSpeedSquare < 0.003D && sp.motionY > -0.03D;
if(restoreFromFlying || tryLanding)
    standupIfPossible(tryLanding, restoreFromFlying);
```

### 기능 2 보조 — `SmartMovingSelf.java:2186-2230`
```java
private void standupIfPossible(boolean tryLanding, boolean restoreFromFlying)
{
    if(heightOffset >= 0)
        return;

    double gapUnderneight = getGapUnderneight();
    boolean groundClose = gapUnderneight < 1D;
    double gapOverneight = groundClose ? getGapOverneight() : -1D;
    boolean standUpPossible = gapUnderneight + gapOverneight >= 1D;

    if(tryLanding && groundClose && standUpPossible)
    {
        isFlying = false;
        sp.capabilities.isFlying = false;
        restoreFromFlying = true;
    }

    if(!restoreFromFlying)
        return;

    if(!groundClose && !sneakButton.Pressed)
        resetHeightOffset();
    else if(standUpPossible && !(sneakButton.Pressed && grabButton.Pressed))
        standUp(gapUnderneight);
    else
        toSlidingOrCrawling(gapUnderneight);
}

private void standUp(double gapUnderneight)
{
    move(0, (1D - gapUnderneight), 0, true);
    isCrawling = false;
    isHeadJumping = false;
    resetHeightOffset();
}

private void toSlidingOrCrawling(double gapUnderneight)
{
    move(0, (-gapUnderneight), 0, true);

    if(Config.isSlidingEnabled() && (grabButton.Pressed || wasHeadJumping))
        isSliding = true;
    else
        wasCrawling = toCrawling();
}
```

### 기능 3 — `SmartMovingSelf.java:2736-2784`
```java
boolean wasCrawlClimbing = isCrawlClimbing;
isCrawlClimbing = (wasCrawling || isCrawlClimbing) && isClimbing && isNeighborClimbing && (sneakButton.Pressed || crawlToggled) && esp.movementInput.moveForward > 0F;
if(isCrawlClimbing)
{
    boolean canStandUp = !isPlayerInSolidBetween(sp.boundingBox.minY - (isClimbCrawling ? 0.95D : 1D), sp.boundingBox.minY);
    if(canStandUp)
    {
        wasCrawlClimbing = false;
        isCrawlClimbing = false;
        if(!isClimbCrawling)
            resetHeightOffset();
    }

    if(!wasCrawlClimbing)
    {
        wasCrawling = false;
        isCrawling = false;
    }
}
else if(wasCrawlClimbing)
{
    boolean toCrawling = sneakButton.Pressed || crawlToggled;
    if(!isClimbing)
    {
        wasCrawling = toCrawling();

        double minY = sp.boundingBox.minY;
        move(0, (-minY + Math.floor(minY)), 0, true);
    }
    else if(esp.movementInput.moveForward <= 0F)
    {
        wasCrawling = toCrawling;
        isCrawling = toCrawling;

        wantClimbUp = false;
        wantClimbDown = false;

        if(!toCrawling)
            resetHeightOffset();
        double minY = sp.boundingBox.minY;
        move(0, (-minY + Math.floor(minY) + (toCrawling ? 0F : 1F)), 0, true);
    }
    else if(!toCrawling)
    {
        resetHeightOffset();
        double minY = sp.boundingBox.minY;
        move(0, (Math.ceil(minY) - minY), 0, true);
    }
}
```

### `wantClimbHolding` 정의 — `SmartMovingSelf.java:2721-2732`
```java
boolean wantClimbHolding =
    (isClimbHolding && sneakButton.Pressed) ||
    (isClimbing && blocked) ||
    (wantClimb &&
    !isSwimming &&
    !isDiving &&
    !isCrawling &&
    (sneakButton.Pressed || crawlToggled));

isClimbHolding =
    wantClimbHolding &&
    isClimbing;
```

---

## 작업 종결 상태

- [x] 원본 위치 + 핵심 변수 grep 완료.
- [x] 기능 1 라인별 분석.
- [x] 기능 2 라인별 분석.
- [x] 기능 3 라인별 분석.
- [x] 의존 변수 (wantClimbHolding / hasClimbCrawlGap / isNeighborClimbing 등) 정의 확인.
- [x] 헬퍼 메서드 (setHeightOffset / resetHeightOffset / standUp / toSlidingOrCrawling / isPlayerInSolidBetween) 본체 확인.
- [x] 8 방향 seekClimbGap 흐름 + handleClimbing 의 좁은 갭 분기 (L996-999) 확인.
- [x] 사용자 의도 ↔ 원본 식 정확 매핑 검증.

**다음 작업** (사용자 결정 대기):
- 우리 1.21.1 매핑 vs 원본 라인별 cross-check (별도 체크리스트).
- 차이 2 (`!forwardPressed_holdGuard`) 의 기능 1 영향 인게임 검증.
- 차이 3 (`matched skip`) 의 좁은 갭 분기 매치 검증.

---

# 부록 — 2026-05-03 세션: 기능 3 (`isCrawlClimbing`) 우리 1.21.1 매핑 정밀 점검

사용자 보고: "엎드린 상태로 블록 앞에서 grab 을 누르고 있으면 엎드린 채로 클라이밍이 되는 기능이 지금 포팅 안되어있는거 같은데".

본 부록은 **원본 ↔ 1.21.1 매핑 라인 단위 cross-check 결과** 만 담는다. 수정 미수행.

---

## 부록.1 매핑 위치 일람 (라인 인용)

| 원본 위치 | 의미 | 1.21.1 매핑 위치 | 상태 |
|----------|------|------------------|------|
| `SmartMovingSelf.java:2737` | isCrawlClimbing 5-AND 메인 식 | `SmartMovingClientState.java:1985-1990` | ✅ 이식 (가드 1 추가) |
| `SmartMovingSelf.java:2738-2754` | canStandUp 자동 해제 분기 | `SmartMovingClientState.java:1992-2010` | ✅ 1:1 |
| `SmartMovingSelf.java:2755-2783` | 해제 엣지 3갈래 분기 | `SmartMovingClientState.java:2011-2045` | ✅ 1:1 |
| `SmartMovingSelf.java:2441` | wasCrawling = isCrawling | `SmartMovingClientState.java:1720` | ✅ 순서 OK |
| `SmartMovingSelf.java:2442-2444` | isCrawling 식 | `SmartMovingClientState.java:1721` | ✅ 가드 1 추가 |
| `SmartMovingSelf.java:2467-2477` | wouldWantClimb 4-OR | `SmartMovingClientState.java:1375-1385` | ✅ 1:1 |
| `SmartMovingSelf.java:2479-2481` | wantClimb | `SmartMovingClientState.java:1387` | ✅ 1:1 |
| `SmartMovingSelf.java:2489-2493` | wantClimbUp | `SmartMovingClientState.java:1424-1429` | ✅ 1:1 |
| `SmartMovingSelf.java:2452-2463` | wantCrawlNotClimb | `SmartMovingClientState.java:1761-1768` | ✅ 1:1 |
| `SmartMovingRender.java:65,76` | 렌더 분기 isClimb/isCrawlClimb | `MixinPlayerEntityModelClient.java` 분기 | ⚠️ 잔존 차이 3개 (메모리) |
| `SmartMovingModel.java:239-277` | isCrawlClimb 자세 (body/leg/arm) | `MixinPlayerEntityModelClient.java:620-650` | ⚠️ head.pitch/leg.pitch/arm.pitch 잔존 |
| `SmartMovingSelf.java:3170` | 상태 패킷 비트 | `SmartMovingClientState.sendStatePacket:3584` | ✅ 1:1 |

→ **메인 식과 진입/해제 분기는 모두 매핑되어 있음**.

---

## 부록.2 우리 매핑 추가 가드 — 영향 분석

### 가드 G1 — `iccExitJustToCrawl` (메인 식 첫 항)
**위치**: `SmartMovingClientState.java:1985`
```java
isCrawlClimbing = !iccExitJustToCrawl
        && (wasCrawling || isCrawlClimbing)
        && ...;
```
- **목적**: 사다리/덩굴 ICC EXIT 직후 crawl 진입 시 isCrawlClimbing 식이 잘못 매치되어 isCrawling=false reset 되는 BUG 차단 (메모리 `project_ladder_vine_iccexit_complete.md`).
- **set 조건** (L2229): ICC EXIT 분기 + sneak 누른 상태에서만 set.
- **reset 조건** (L1723): `isCrawling=false` 자연 시점.
- **사용자 시나리오 영향**: 평지 엎드리기 → 블록 앞 → grab. 이 경로에는 ICC EXIT 거치지 않음 → set 안 됨 → 가드 미작용. **영향 없음**.

### 가드 G2 — `canCrawl` 의 `(!isClimbing || isCrawling)`
**위치**: `SmartMovingClientState.java:1718`
```java
boolean canCrawl = ... && (!isClimbing || isCrawling) && ...;
```
- **목적**: 1.7.10 박스 (발=posY+1) ↔ 1.21.1 박스 (발=entity.y) 차이로 사다리 grip 영역 안 → handleClimbing 결과 isClimbing=true → canCrawl=false → isCrawling=false 강제 BUG 차단.
- **사용자 시나리오 영향**:
  - 이전 tick 엎드린 상태 (isCrawling=true) → 가드 매치 → canCrawl=true 유지 가능.
  - 다음 tick: isClimbing=true(grab+block) → 가드 매치 (`isCrawling=true 이전 값`) → canCrawl=true → isCrawling=true 재진입 가능.
  - **이 가드는 사용자 시나리오에 도움 됨**.

### 가드 G3 — `wantClimbHolding` 의 forwardPressed_holdGuard
**위치**: 부록 외 본문 §"차이 2" 참조.
- **이전 분석**: 이 가드가 **기능 1 (isClimbCrawling)** 영원 비활성 BUG 원인 가능성.
- **2026-05-02 세션**: 가드 제거됨 (`SmartMovingClientState.java:1402-1405` 코멘트 "가드 제거" 명시).
- **현재 상태**: 정상. **isCrawlClimbing 에는 영향 없음** (wantClimbHolding 은 기능 1 의존 변수).

→ 추가 가드 3종 모두 사용자 시나리오 (엎드림 → grab → 엎드린 채 climbing) 에는 부정적 영향 없음.

---

## 부록.3 사용자 시나리오 1 tick 흐름 추적

### 시나리오: 평지 엎드림 → 블록 정면 → grab + W + sneak hold
**Tick N-1 (평지 엎드림)**:
- isCrawling=true, wasCrawling=true, isClimbing=false, isCrawlClimbing=false

**Tick N (블록 정면 + grab + W 시작)**:
1. `wasCrawling = isCrawling` (= true) — `SmartMovingClientState:1720`
2. handleClimbing → grab + isFacedToBlock → isClimbing=true 가능 (조건: wantClimbUp=true)
3. wantClimbUp 식: `wantClimb && fwd > 0` → wantClimb=true (grab + freeClimb) + fwd>0 → wantClimbUp=true ✓
4. isClimbing=true 가정 (handleClimbing 결과)
5. isCrawlClimbing 식 평가:
   - `!iccExitJustToCrawl` = true (set 안 됨) ✓
   - `wasCrawling=true || ...` ✓
   - `isClimbing=true` ✓
   - `isNeighborClimbing` = ? (8방향 grip 결과)
   - `sneak=true || crawlToggled` = true ✓
   - `fwd > 0` = true ✓
6. **isNeighborClimbing 매치 시 → isCrawlClimbing=true 진입**
7. canStandUp 검사: 위 1m 빈공간 있으면 자동 해제 → 일반 climbing 으로 전환됨
8. canStandUp=false (위 막힘) → isCrawlClimbing=true 유지 + isCrawling=false set (진입 엣지)

**관전 포인트**:
- (P1) `isNeighborClimbing` — 엎드린 박스 (height=0.8) 기준 8방향 grip 검사가 정상 동작하는지
- (P2) `isClimbing` — handleClimbing 분기가 엎드린 상태에서도 wantClimbUp 매치 처리하는지
- (P3) canStandUp 검사 — 사용자 시나리오 (블록 앞, 위 빈공간) 에서 캐시 결과가 어떻게 나오는지
- (P4) 자세 — isCrawlClimbing=true 진입 후 D-4 분기 + isCrawlClimb 분기 동시 적용 결과 자세

---

## 부록.4 핵심 의심 지점 — 인게임 검증/디버그 로그 필요

### 의심 1 — canStandUp 즉시 해제
사용자가 블록 1개 짜리 앞에서 시도 시 → 위 1m 빈공간 → canStandUp=true → isCrawlClimbing=false 즉시 해제 → 일반 climbing 으로 전환 → 자동 standing 등반.
- **사용자 보고와 일치 가능성 高**: "엎드린 채 climbing 안 됨" = canStandUp 자동 해제로 standing 등반.
- **검증 방법**: 위에 천장 있는 블록 (= 2블록 이상 높이 + 위 막힘) 에서 시도. 이 경우 canStandUp=false → 유지.
- **확인 위치**: `SmartMovingClientState.java:1996-2003` `_canStandUp17` 계산.

### 의심 2 — isNeighborClimbing 누락
엎드린 박스 (height=0.8) 기준 8방향 grip 검사가 standing 박스 기준과 다른 결과 나올 가능성.
- **확인 위치**: `SmartMovingClimber.handleClimbing` 의 8방향 seekClimbGap 호출 (B-19a4 세션 108).
- **검증 방법**: 엎드린 상태에서 isNeighborClimbing 디버그 log dump.

### 의심 3 — isClimbing 미진입
handleClimbing 의 `if (!matched) return;` (메모리 `feedback_climb_branch_fallback_forbidden.md`) 가드 때문에 wantClimbUp/Down 미매치 시 setShouldClimbSpeed skip → isClimbing 안 set.
- 엎드린 상태 wantClimbUp 식: `wantClimb && fwd > 0F` (L1424) — 정상 매치돼야.
- 단 `(!isCrawling || hCollision)` 보조 조건 (vine+jump 분기 한정) 은 영향 없음 (wantClimb && fwd>0 분기는 별도).
- **검증 방법**: isClimbing 디버그 log dump.

### 의심 4 — 자세만 standing
isCrawlClimbing=true 정상이나 자세 매핑 잔존 차이 (메모리 `project_crawl_climbing_pending.md` head.pitch/leg.pitch/arm.pitch 3가지) 로 시각만 standing.
- **검증 방법**: 디버그 화면 (F3) 에서 박스 dim 확인 (height=0.8 이면 isCrawling 또는 isCrawlClimbing 활성).

---

## 부록.5 추가 매핑 누락 의심 (영향도 낮음)

### A1 — `getInputSpeedFactor` crawl factor 분기
- 원본 L186: `isCrawling || (isCrawlClimbing && !isClimbCrawling)` → `_crawlFactor` 적용
- 우리 매핑: `MixinLivingEntityClient` 또는 SmartMovingClientState 의 속도 factor 계산. **확인 안 됨**.
- 영향: isCrawlClimbing 진입은 OK 이나 등반 속도가 일반 climbing 속도. **사용자 시나리오 핵심 영향 X**.

### A2 — `isDipping` 의 isCrawlClimbing 분기
- 원본 L301: `isCrawling || isClimbCrawling || isCrawlClimbing` → isDipping=true
- 우리 매핑: 확인 안 됨.
- 영향: 물 표면 머리 잠김 처리. 일반 시나리오 무관.

### A3 — `Config.getFactor` hunger/exhaustion 인자
- 원본 L1202/L1278: 인자 리스트에 isCrawlClimbing 전달
- 우리 매핑: Config.getFactor 자체 매핑 여부 확인 안 됨.
- 영향: 배고픔/피로 계산. 사용자 시나리오 핵심 영향 X.

---

## 부록.6 검증 단계 권고

1. **사용자에게 시나리오 정확화 요청**
   - 시도한 블록 종류 (사다리 / 덩굴 / 일반 블록)
   - 위에 천장 있는지 (= canStandUp 검증용)
   - 자세는 standing 으로 보였는지 / 박스 크기는 어땠는지 (시각 vs 기능 분리)
   - sneak 누름 방식 (hold vs toggle)

2. **디버그 로그 1회 추가** (메모리 `feedback_debug_log_first.md`)
   - 위치: `SmartMovingClientState.java:1990` 직후
   - 변수: `iccExitJustToCrawl, wasCrawling, isCrawlClimbing(old), isClimbing, isNeighborClimbing, sneak||toggle, fwd, _canStandUp17, isCrawlClimbing(new)`
   - 1 tick dump 로 정확한 실패 지점 식별

3. **메모리 `project_crawl_climbing_pending.md` 잔존 차이 3가지 자세 검증**
   - 자세만 안 맞으면 그것이 원인 (= 의심 4)

4. **§부록.5 누락 항목 (A1/A2/A3) 매핑 확인** (별도 체크리스트)

---

## 부록.7 절대 건드리지 말 것

- `project_isclimbcrawling_complete.md` (ICC 시스템) — 메모리상 완결
- `project_grab_climbing_complete.md` (그랩 클라이밍) — 메모리상 완결
- `project_ladder_vine_iccexit_complete.md` (사다리/덩굴 ICC EXIT) — 메모리상 완결
- `project_restoreFromFlying_complete.md` (비행 → crawl) — 메모리상 완결
- `project_grab_sneak_hold_complete.md` (grab sneak hold) — 메모리상 완결
- `project_icc_camera_collision_fix.md` (ICC 카메라/콜리전 fix) — 메모리상 완결

이번 작업 = **isCrawlClimbing 진입 확인 + 자세 잔존 차이 fix** 만. 위 시스템 회귀 검증 필수.

---

## 부록.8 — 작업 완료 (2026-05-03)

사용자 "이제 너무 잘된다" 명시. **isCrawlClimbing 시스템 완결**.

5단계 fix 정착 (메모리 `project_isCrawlClimbing_complete.md`):
1. `MixinLivingEntityClient.java:167` — 매 tick `sm.isCrawlClimbing = false` reset 제거 (자가유지 보존)
2. `MixinPlayerEntityClient.java:97` — dim 분기에 `&& sm.wasClimbCrawling` 가드 (= ICC 해제 엣지 1 tick 한정)
3. `SmartMovingClimber.java:558` — `jd += -1D` (원본 L920 1:1, jh 자동 -2 보정)
4. `SmartMovingClientState.java:2171` — `canClimbCrawling` 식에 `&& !isCrawlClimbing` 가드 (ICC 차단)
5. `SmartMovingClientState.java:2230 부근` — ICC 진입 시 `cameraY/lastCameraY = ICC eye` 강제 set

**잔존 작업**: 자세 매핑 잔존 차이 3가지 (`project_crawl_climbing_pending.md`) — 본 fix 와 무관. 별도 작업.

