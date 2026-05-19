# SmartMoving 수영 (Swim/Dive) 기능 리서치

**작성일**: 2026-05-20
**작업 범위**: 기능 (state / motion / transition / 박스 dim / 키 입력 / 멀티 동기화).
**작업 범위 제외**: 애니메이션 (setAngles / ModelPart 회전) — 다음 단계.
**사용자 명시**: 스마트무빙 자체의 마지막 작업 영역.

---

## 0. 개요

수영은 원본 SmartMoving 1.7.10 의 가장 큰 단일 영역 중 하나로, `handleSwimming()` (`SmartMovingSelf.java:229-576`) 단일 함수 안에 4 분류 state, 21 step 미세 motionYDiff 테이블, 5 분기 phase 전환 (crawl/walk/dipping/swimming/diving) 이 묶여 있다.

우리 1.21.1 매핑은 **상당 부분 이식 완료** 상태:
- `SmartMovingSwimmer.java` (732 lines) — updateSwimState / handleSwimming / handleLava / moveFlying 4-5인자 / isTunnelAhead
- `SmartMovingClientState` — 10+ 수영 관련 필드 + 매 tick 가드 조합 다수
- `MixinLivingEntityClient.sm_beforeTravel` 진입점
- `SmartMovingState` packet bits 9/11 (server)
- `SmartMovingClientState` StatePayload bits 9/10/11

이번 작업은 **신규 구현이 아닌 검증 + 패리티 보강 + 멀티 동기화 보강** 위주. 헤드점프 시리즈 (#79~#100) 와 유사한 fix 패턴으로 self/server/remote 3 측 1:1 매핑 마무리.

---

## 1. 원본 1.7.10 핵심 구조

### 1-1. State 4 분류

원본 `SmartMoving.java:43-46` (SmartMovingContext 공유 필드):
```java
public boolean isDipping;
public boolean isSwimming;
public boolean isDiving;
public boolean isLevitating;
```

**의미** (offset = `playerSwimWaterBorder + 0.1625D` 기준):
| State | offset 범위 | 의미 |
|-------|-------------|------|
| `isDipping` | `< 1.4` | 발만 물속 (얕은 물) |
| `isSwimming` | `1.4 ≤ offset < 1.9` | 수면 수영 (몸통까지 물속, 머리 위 공기) |
| `isDiving` | `≥ 1.9` | 완전 잠수 (머리 위 물) |
| `isLevitating` | (diving && !diveUp && !diveDown && 입력 0) | 잠수 정지 부유 |

**배타 관계**: 4 state 는 매 tick `resetSwimming()` 으로 리셋 후 재계산. 다른 SM phase (isClimbing/isFlying/isCrawling 등) 와는 **handleSwimming 진입 가드** (`!isFlying && !isLiquidClimbing && (sp.isInWater() || ...)`) 로 분리.

### 1-2. offset 계산 (`SmartMovingSelf.java:255-270`)

```java
int i = MathHelper.floor_double(sp.posX);
int j = MathHelper.floor_double(sp.boundingBox.minY);
int k = MathHelper.floor_double(sp.posZ);

double j_offset = sp.boundingBox.minY - j;

double totalSwimWaterBorder = getMaxPlayerLiquidBetween(sp.boundingBox.maxY - 1.8, sp.boundingBox.maxY + 1.2);
double minPlayerSwimWaterCeiling = getMinPlayerSolidBetween(sp.boundingBox.maxY - 1.8, sp.boundingBox.maxY + 1.2, 0);
double realTotalSwimWaterBorder = Math.min(totalSwimWaterBorder, minPlayerSwimWaterCeiling);
double minPlayerSwimWaterDepth = totalSwimWaterBorder - getMaxPlayerSolidBetween(totalSwimWaterBorder - 2, totalSwimWaterBorder, 0);
double realMinPlayerSwimWaterDepth = totalSwimWaterBorder - getMaxPlayerSolidBetween(realTotalSwimWaterBorder - 2, realTotalSwimWaterBorder, 0);
double playerSwimWaterBorder = totalSwimWaterBorder - j - j_offset;
```

- `totalSwimWaterBorder` — 액체 표면 절대 Y (블록 좌표).
- `minPlayerSwimWaterCeiling` — 액체 영역 천장 솔리드 (수중 동굴 천장).
- `realTotalSwimWaterBorder` — 표면과 천장 중 낮은 쪽 (천장이 있으면 실제 표면 효력 천장까지).
- `playerSwimWaterBorder` — 박스 발 기준 액체 깊이.
- `couldStandUp` — `playerSwimWaterBorder >= 0 && minPlayerSwimWaterDepth <= 1.5` — 발 솔리드까지 1.5m 이내.

### 1-3. 트리거 변수 (`SmartMovingSelf.java:278-289`)

```java
boolean diveUp = isp.getIsJumpingField();           // jump 키 누름
boolean diveDown = esp.movementInput.sneak && Config._diveDownOnSneak.value;
boolean swimDown = esp.movementInput.sneak && Config._swimDownOnSneak.value;

boolean wantShallowSwim = couldStandUp && (wasSwimming || wasDiving);
if(wantShallowSwim) {
    // 8 방향 tunnel 검사 — tunnel 있으면 wantShallowSwim=false
    HashSet<Orientation> orientations = Orientation.getClimbingOrientations(sp, true, true);
    Iterator<Orientation> iterator = orientations.iterator();
    while(iterator.hasNext())
        if(!(wantShallowSwim &= !iterator.next().isTunnelAhead(sp.worldObj, i, j, k))) break;
}
```

- `diveUp`: 점프 키 누름 → 잠수 시 수직 상승.
- `diveDown`: sneak + Config 활성화 → 잠수 더 깊이.
- `swimDown`: sneak + Config 활성화 → 표면 수영 → 수면 아래.
- `wantShallowSwim`: 얕은 물에서 standing 가능 + 이전 tick swim/dive 였음 + 주변 8 방향 tunnel 없음.

### 1-4. moveSwim 식 (`SmartMovingSelf.java:306`)

```java
boolean moveSwim = sp.rotationPitch < 0F && esp.movementInput.moveForward > 0F
                || sp.rotationPitch > 0F && esp.movementInput.moveForward < 0F;
```

- 위쪽 보면서 W → moveSwim=true.
- 아래쪽 보면서 S → moveSwim=true.
- = 시선 방향 + 입력 방향 일치 시 표면 위 향해 헤엄.

### 1-5. motionYDiff 21 step 테이블 (`SmartMovingSelf.java:309-437`)

분류 = `offset < 1.4` (dipping) / `1.4 ≤ offset < 1.9` (swimming) / `offset ≥ 1.9` (diving).

**dipping 분기** (`L309-316`):
```java
if(offset < 1.4) {
    dipping = true;
    if(offset < 1)      motionYDiff = -0.02D;
    else                motionYDiff = -0.01D;
}
```

**swimming 분기** (`L317-348`):
```java
else if(offset < 1.9) {
    if(diveDown)             { diveDown = false; diving = false; swimming = true; }
    else if(swimDown)        motionYDiff = -0.02D;
    else if(diveUp)          motionYDiff = +0.04D;
    // ... 11 step 미세 조정 (각 offset 경계 구간별 -0.02 ~ +0.02)
    swimming = true;
}
```

**diving 분기** (`L349-437`): A 경로 / B 경로 분리 (벽 위 다이빙 등 특수 케이스). 10 step 조정.

분기 결과 = `dipping` / `swimming` / `diving` 3 boolean 중 하나 true.

### 1-6. Config 게이트 (`SmartMovingSelf.java:438-441`)

```java
swimming = !useStandard && swimming && Config.isSwimmingEnabled();
diving   = !useStandard && diving   && Config.isDivingEnabled();
dipping  = !useStandard && dipping  && Config.isSwimmingEnabled();
useStandard = !swimming && !diving && !dipping;
```

= Config 비활성 시 vanilla 수영 사용. `useStandard=true` → `L544-572` vanilla swim 분기.

### 1-7. Motion damping (`SmartMovingSelf.java:443-471`)

```java
if(!useStandard) {
    if(diveUp)
        sp.motionY -= 0.039999999105930328D;     // = -0.04 정확

    if(swimming) {
        sp.motionX *= 0.85D;
        sp.motionY *= 0.85D;
        sp.motionZ *= 0.85D;
    } else if(diving) {
        sp.motionX *= 0.83D;
        sp.motionY *= 0.83D;
        sp.motionZ *= 0.83D;
    } else if(dipping) {
        sp.motionX *= 0.80D;
        sp.motionY *= 0.83D;
        sp.motionZ *= 0.80D;
    } else {                                       // useStandard=true 대비 (이 분기에선 사실상 매치 X)
        sp.motionX *= 0.9D;
        sp.motionY *= 0.85D;
        sp.motionZ *= 0.9D;
    }
}
```

| 상태 | damping XZ | damping Y |
|------|------------|-----------|
| swimming | 0.85 | 0.85 |
| diving | 0.83 | 0.83 |
| dipping | 0.80 | 0.83 |
| diveUp 추가 | — | motionY -= 0.04 |

### 1-8. levitating + waterMovementTicks (`SmartMovingSelf.java:473-484`)

```java
boolean moveFlying = true;
boolean levitating = diving && !diveUp && !diveDown && moveStrafing == 0F && moveForward == 0F;

if(diving)   speedFactor *= Config._diveSpeedFactor.value;
if(swimming) speedFactor *= Config._swimSpeedFactor.value;

if(swimming || diving) waterMovementTicks++;
else                   waterMovementTicks = 0;
```

- `levitating`: 잠수 + 입력 0 → 정지 부유.
- `waterMovementTicks`: 연속 수영/잠수 틱. jump-out-of-water 판정 사용.

### 1-9. jump-out-of-water (`SmartMovingSelf.java:486-500`)

```java
boolean wantJumpOutOfWater = (moveForward != 0 || moveStrafing != 0)
        && sp.isCollidedHorizontally
        && diveUp
        && !isSlow;
isJumpingOutOfWater = wantJumpOutOfWater
        && (waterMovementTicks > 10 || sp.onGround || wasJumpingOutOfWater);

// ... motion 분기 ...
else if(isJumpingOutOfWater)
    sp.motionY = 0.30000001192092896D;
```

= 수평 입력 + 벽 충돌 + jump 키 + !sneak + (10틱 수영 누적 OR 발이 ground OR 이미 점프중) → 수면 위로 0.3 m/tick 상승.

### 1-10. motion 분기 (`SmartMovingSelf.java:489-502`)

```java
if(diving) {
    if(diveUp || diveDown || levitating)
        sp.motionY = (sp.motionY + motionYDiff) * 0.6;
    else
        moveFlying((float)motionYDiff, moveStrafing, moveForward, 0.02F * speedFactor, Options._diveControlVertical.value);
    moveFlying = false;
} else if(swimming && swimDown)
    sp.motionY = (sp.motionY + motionYDiff) * 0.6;
else if(isJumpingOutOfWater)
    sp.motionY = 0.30000001192092896D;
else
    sp.motionY += motionYDiff;
```

- diving + (diveUp || diveDown || levitating) → motionY = (motionY + diff) * 0.6 (강한 수직 제어).
- diving + 일반 → moveFlying 5인자 (treeDimensional pitch 반영).
- swimming + swimDown → diving 강제 분기와 동일 식.
- jump-out-of-water → motionY = 0.3 (hardcoded).
- 기본 → motionY += motionYDiff (motionYDiff 누적).

### 1-11. state set + heightOffset (`SmartMovingSelf.java:504-511`)

```java
isDiving = diving;
isLevitating = levitating;
isSwimming = swimming;
isShallowDiveOrSwim = couldStandUp && (isDiving || isSwimming);
isDipping = dipping;

if(isDiving || isSwimming)
    setHeightOffset(-1F);
```

= isDiving/isSwimming 시 box 1m down (vanilla 1.7.10 의 박스 +1m up 효과). **isDipping 시 setHeightOffset 안 함 (= 발만 물속 → 박스 STANDING 유지)**.

### 1-12. 얕은 물 transition (`SmartMovingSelf.java:513-536`)

```java
if(isShallowDiveOrSwim && realMinPlayerSwimWaterDepth < SwimCrawlWaterBottomBorder) {  // BOTTOM=0.55
    if(isSlow) {
        // shallow swim/dive → crawling
        setHeightOffset(-1F);
        isCrawling = true;
        isDiving = false;
        isSwimming = false;
        isShallowDiveOrSwim = false;
        isDipping = true;
    } else {
        // shallow swim/dive → walking
        resetHeightOffset();
        sp.moveEntity(0, getMaxPlayerSolidBetween(sp.boundingBox.minY, sp.boundingBox.maxY, 0) - sp.boundingBox.minY, 0);
        isCrawling = false;
        isDiving = false;
        isSwimming = false;
        isShallowDiveOrSwim = false;
        isDipping = true;
    }
}
```

= 얕은 물 + 0.55m 이하 깊이 + sneak → crawl 전환, !sneak → walking 전환 + y-snap 자동 보정.

### 1-13. moveFlying + moveEntity (`SmartMovingSelf.java:538-540`)

```java
if(moveFlying)
    sp.moveFlying(moveStrafing, moveForward, 0.02F * speedFactor);
sp.moveEntity(sp.motionX, sp.motionY, sp.motionZ);
```

- `moveFlying`: diving 외 분기 후 (vanilla 4인자) 호출. moveFlying 후 motion 갱신.
- `moveEntity`: 실제 collision + 이동.

### 1-14. useStandard 분기 (`SmartMovingSelf.java:553-572`)

Config 비활성 시 vanilla 수영. 단 SM 특유 일부 처리는 유지:
```java
if(useStandard) {
    resetSwimming();
    if(isCrawling) setHeightOffset(wasHeightOffset);
    double dY = sp.posY;
    sp.moveFlying(moveStrafing, moveForward, 0.02F * speedFactor);
    sp.moveEntity(sp.motionX, sp.motionY, sp.motionZ);
    sp.motionX *= 0.80000001192092896D;
    sp.motionY *= 0.80000001192092896D;
    sp.motionZ *= 0.80000001192092896D;
    sp.motionY -= 0.02D;
    if(sp.isCollidedHorizontally && sp.isOffsetPositionInLiquid(sp.motionX, ((sp.motionY + 0.6) - sp.posY) + dY, sp.motionZ))
        sp.motionY = 0.30000001192092896D;
}
```

= vanilla 수영 식 (damping 0.8, gravity -0.02) + 수면 자동 점프 (jump-out-of-water 의 간소화).

### 1-15. handleLava (`SmartMovingSelf.java:578-600`)

```java
private boolean handleLava(float moveForward, float moveStrafing, boolean handledSwimming, boolean isLiquidClimbing) {
    boolean handleLava = !isFlying && !handledSwimming && !isLiquidClimbing && sp.handleLavaMovement();
    if(handleLava) {
        standupIfPossible();
        resetClimbing();
        resetSwimming();
        double d1 = sp.posY;
        sp.moveFlying(moveStrafing, moveForward, 0.02F);
        sp.moveEntity(sp.motionX, sp.motionY, sp.motionZ);
        sp.motionX *= 0.5D;
        sp.motionY *= 0.5D;
        sp.motionZ *= 0.5D;
        sp.motionY -= 0.02D;
        if(sp.isCollidedHorizontally && sp.isOffsetPositionInLiquid(...))
            sp.motionY = 0.30000001192092896D;
    }
    return handleLava;
}
```

= lava 진입 시 swim state 리셋 + 강한 damping (0.5) + jump-out-of-lava.

### 1-16. resetSwimming (`SmartMovingSelf.java:1488-1498`)

```java
private void resetSwimming() {
    dippingDepth = -1;
    isDipping = false;
    isSwimming = false;
    isDiving = false;
    isLevitating = false;
    isShallowDiveOrSwim = false;
    isFakeShallowWaterSneaking = false;
    isJumpingOutOfWater = false;
}
```

= 8 필드 일괄 reset. 6 곳 호출 (handleSwimming 시작 / handleLava / standUp / dimension change / restoreFromFlying / disable).

### 1-17. fallDistance reset

`SmartMovingSelf.java:603` (handleSwimming 반환 후):
```java
sp.fallDistance = 0F;
```

= 수영 진행 시 매 tick fall reset → fall damage 차단.

---

## 2. vanilla 1.21.1 비교

| 항목 | SM 1.7.10 | vanilla 1.21.1 |
|------|-----------|----------------|
| state 분류 | 4 (dipping/swimming/diving/levitating) | 1 (isSwimming) |
| 진입 트리거 | water + jump/sneak/forward+pitch | sprint+water+forward+!flying+!shift |
| 박스 dim | swim/dive 시 height -1 (heightOffset=-1F) | POSE=SWIMMING dim (0.6x0.6, 1.6 height) |
| 수평 damping | swim 0.85, dive 0.83, dip 0.80 | 0.9 (water 0.99 in newer) |
| 수직 motion | motionYDiff 21 step + diveUp/Down | gravity/16, vanilla water push |
| 산소 | 별도 없음 (vanilla 위임) | air supply system |
| 수직 jumpUp | motionY = 0.3 (hardcoded) | none (자동 surface) |
| sprint 모드 | _diveSpeedFactor/_swimSpeedFactor 곱셈 | water swim 자체가 sprint |
| 시각 | (별도 setAngles — 본 작업 영역 외) | POSE=SWIMMING vanilla 자세 |

**vanilla 1.21.1 신규 메커니즘 (메모리 참조)**:
- `@AUTOLAND` 가드 — `isOnGround() && abilities.flying → flying=false`. swim phase 에 직접 영향 X.
- `lerpPosAndRotation` lerp — multi BUG 영향 큼 ([[feedback_remote_setpos_lerp_cancel]]).
- server tick 순서 — `playerTick → world.tick → networkIo.tick` ([[reference_vanilla_server_tick_order]]).

---

## 3. 우리 1.21.1 매핑 현황

### 3-1. SmartMovingSwimmer.java (732 lines)

**상수 (L31-51)**:
```java
OFFSET_SWIMMING = 1.4D                       // isDipping/isSwimming 경계
OFFSET_DIVING   = 1.9D                       // isSwimming/isDiving 경계
SWIM_CRAWL_MAX/TOP/MEDIUM/BOTTOM = 1.0/0.65/0.6/0.55
DAMPING_DIPPING_XZ = 0.80D
DAMPING_DIPPING_Y  = 0.83D
DAMPING_SWIMMING   = 0.85D
DAMPING_DIVING     = 0.83D
BASE_SWIM_SPEED    = 0.02F
JUMP_OUT_OF_WATER_VELOCITY = 0.30000001192092896D
```

**함수**:
| 함수 | 위치 | 원본 매핑 |
|------|------|-----------|
| `resetSwimming(sm)` | L82-91 | SmartMovingSelf.L1488-1498 |
| `updateSwimState(player, sm)` | L93-237 | SmartMoving.java 의 isDipping/isSwimming/isDiving 갱신 + handleSwimming offset 계산 분리 |
| `handleSwimming(player, sm, input, jumping, ...)` | L251-605 | SmartMovingSelf.L229-576 (handleSwimming 본체) |
| `moveFlying(player, moveUpward, strafe, fwd, speed, treeDim)` | L625-659 | SmartMovingBase 5인자 moveFlying |
| `moveFlying(player, strafe, fwd, speed)` | L661-670 | SmartMovingBase 4인자 |
| `handleLava(player, sm, fwd, strafe, handledSwim)` | L732-784 | SmartMovingSelf.L578-600 |
| `isTunnelAhead(world, i, j, k, ox, oz)` | L802-? | Orientation.isTunnelAhead |

### 3-2. SmartMovingClientState 필드

```java
// L710-727
public boolean isDipping;                  // 원본 L43
public boolean isSwimming_sm;              // 원본 L44 (vanilla isSwimming 충돌 회피)
public boolean isDiving;                   // 원본 L45
public boolean isLevitating;               // 원본 L46
public float   dippingDepth;               // 원본 L1432 (resetSwimming -1)
public int     waterMovementTicks;         // 원본 L1448
public boolean isShallowDiveOrSwim;        // 원본 L1436
public boolean isFakeShallowWaterSneaking; // 원본 L1437
public boolean isJumpingOutOfWater;        // 원본 L1435
public boolean wasJumpingOutOfWater;       // pre-snapshot (B-10b)
```

### 3-3. StatePayload packet bits

```java
// SmartMovingClientState.java L1193-1195 (디코딩)
isDiving       = ((bits >>  9) & 1) != 0;
isDipping      = ((bits >> 10) & 1) != 0;
isSwimming_sm  = ((bits >> 11) & 1) != 0;
```

**인코딩** (`L4381-4383`): self → server 패킷 동일 bit 사용.

### 3-4. Server side (`SmartMovingState.java`)

```java
// L91-93
public boolean isDiving;
// (bit 10 isDipping 누락? 검증 필요)
public boolean isSwimming;

// L182-183 (디코딩)
isDiving       = ((bits >>  9) & 1) != 0;
isSwimming     = ((bits >> 11) & 1) != 0;
```

= **bit 10 isDipping 서버 측 디코딩 누락 가능성** — 검증 필요.

### 3-5. 진입점 (mixin)

`MixinLivingEntityClient.sm_beforeTravel`:
```java
SmartMovingSwimmer.updateSwimState(player, sm);
if (SmartMovingSwimmer.handleSwimming(player, sm, movementInput, this.jumping, ...)) {
    // handleSwimming 처리 완료
}
// handleSwimming false 시 handleLava 호출
SmartMovingSwimmer.handleLava(player, sm, fwd, strafe, handledSwim);
```

### 3-6. 다른 mixin 의 swim 영향

| 위치 | 분기 |
|------|------|
| `MixinClientPlayerEntity:268` | `isSmall = isSwimming_sm || isDiving || isDipping || isCrawling` |
| `MixinClientPlayerEntity:361` | vanilla inSneakingPose 가드 `!swimming` |
| `MixinLivingEntityClient:665-720` | orphan SWIMMING POSE 잔존 처리 (multi BUG 차단) |

### 3-7. 매 tick 가드 조합 위치

`SmartMovingClientState` 안 다수 위치에서 다른 SM phase 가드 OR 에 `isSwimming_sm || isDiving` 포함:
- L1596-1597 sneak 분기 — `!(isDiving && diveDownOnSneak) && !(isSwimming_sm && swimDownOnSneak && !isFakeShallowWaterSneaking)`
- L1751-1755 climbing 분기 — `!isDiving && !isSwimming_sm`
- L1804-1806 sprint 분기 — `_isSwimSprinting17 = canHorizontallySprint && isSwimming_sm`, `_isDiveSprinting17 = canAllSprint && isDiving`
- L1849 isFlying 가드 — `!isSwimming_sm && !isDiving`
- L2005-2029 crawl 진입 — `canCrawl = !isSwimming_sm && !isDiving && (!isDipping || ...)`
- L2554 SM small OR — `isSwimming_sm || isDiving` 포함

---

## 4. 누락 / 미검증 영역

### 4-1. Server side 패리티 누락

- `SmartMovingServer.java` 안 `handleSwimming` 자체 이식 X — state bits 만 디코딩.
- self side `SmartMovingSwimmer.handleSwimming` 의 motion 식 (damping/motionYDiff/jump-out-of-water) 이 server 에서 재계산 안 됨.
- vanilla server 가 swim state 인지 못하면 `processStatePacket` 후 `calculateDimensions()` 호출만 → 박스 dim 일치하나 motion mismatch 가능.
- **검증 필요**: server reconcile 시 swim 분기 정확한지 — multi BUG 잠재.

### 4-2. Packet 동기화 누락 필드

| 필드 | 클라 → server | server → remote |
|------|---------------|-----------------|
| `isDipping` | ✅ (bit 10) | ❓ server 디코딩 누락 가능 |
| `isSwimming_sm` | ✅ (bit 11) | ✅ |
| `isDiving` | ✅ (bit 9) | ✅ |
| `isLevitating` | ✅ (bit 19) | ✅ |
| `dippingDepth` | ❌ | ❌ |
| `waterMovementTicks` | ❌ | ❌ |
| `isShallowDiveOrSwim` | ❌ (계산식 — couldStandUp && (dive||swim)) | derive 가능 |
| `isFakeShallowWaterSneaking` | ❌ | ❌ |
| `isJumpingOutOfWater` | ❌ | ❌ |

**핵심 누락**:
- `dippingDepth` — 멀티 애니메이션 단계 영향 가능 (이번 작업 범위 외, 다음 단계).
- `waterMovementTicks` — jump-out-of-water 판정 source. 멀티 시점 다른 player 의 surface 점프 시각 영향. self 1:1 매핑 원칙 ([[feedback_self_to_server_remote_one_to_one]]).

### 4-3. 멀티 시나리오 검증 필요

다음 시나리오에서 remote 시점 시각 BUG 발생 가능 (헤드점프 #92/#93/#99 같은 패턴):

1. **standing → swim 진입** — self 측 setHeightOffset(-1F) → entity.y push 발동? 박스 dim 변화 시 remote 1m up/down 가능.
2. **swim → dive 전환** — POSE 그대로 (SWIMMING) → mixin offset 가드 일관. 박스 dim 변화 적음. BUG 가능성 낮음.
3. **swim → shallow walk** — `moveEntity(0, ground - bb.minY, 0)` y-snap. remote 측 broadcast lag 시 잠긴 위치 가능.
4. **swim → shallow crawl** — `setHeightOffset(-1F)` + `isCrawling=true`. POSE 변화 → mixin offset 차단/활성 변화.
5. **dive → surface (jump-out-of-water)** — motionY=0.3 1 tick → 잠수 vanilla lerp 추격.
6. **water 떠나기** — `resetSwimming` 호출 → 모든 state false. dim 변화 → mixin offset 차단 → 박스 drop 가능.
7. **lava 진입** — handleLava 분기 → swim state 리셋 + lava damping. remote 시점 일관 검증.

### 4-4. 헤드점프 시리즈 패턴 적용 영역

다음 메모리 패턴 적용 가능 영역:
- [[feedback_server_side_state_push_mirror]] — self side setHeightOffset push 시 server mirror.
- [[feedback_remote_setpos_lerp_cancel]] — remote phase 전환 후 lerp cancel.
- [[feedback_remote_phase_fix_ground_basis]] — ground top 기반 newY.
- [[feedback_slide_exit_willdrop_pose_kept]] — POSE 유지 transition 가드 (swim → dive 같은 케이스).
- [[feedback_self_to_server_remote_one_to_one]] — self 1:1 복제.
- [[feedback_processStatePacket_calculateDimensions]] — server processStatePacket 끝에 calculateDimensions 호출 의무.

---

## 5. 작업 영역 제한 / 보존 의무

### 5-1. 사용자 명시 보존 영역

- 헤드점프 시리즈 (fix #79~#100) — [[project_headjump_all_complete]] 함부로 수정 금지.
- 슬라이딩 시스템 — [[project_sliding_complete]] / [[project_sliding_animation_complete]] 함부로 수정 X.
- 엎드리기 시스템 — [[project_crawl_complete]] 함부로 수정 X.
- 그랩 클라이밍 / 비행 / 낙하 / 각도 점프 — 모두 완결 메모리 보존.

### 5-2. 이번 작업 영역 제외

- 애니메이션 (setAngles / ModelPart 회전 / setupTransforms) — 다음 단계.
- 수영 소리 / particle — 별도 영역.
- exhaustion / hunger — 별도 시스템.
- 산소 시스템 — vanilla 위임 (변경 X).

### 5-3. 작업 영역 (이번)

- **검증**: 이미 이식된 분기 (SmartMovingSwimmer) 동작 정확.
- **server 측 패리티 확인**: bit 10 디코딩 누락 보강 / handleSwimming server 미이식 영향 평가.
- **packet 동기화 보강**: waterMovementTicks / isJumpingOutOfWater 등 멀티 시각 영향 필드.
- **멀티 시나리오 시각 BUG fix**: standing↔swim, swim↔dive, shallow transition, lava, jump-out-of-water.

---

## 6. 핵심 검증 식 (vanilla 1.21.1)

### 6-1. mixin offset 가드 영향

`MixinEntity.sm_offsetBoundingBoxForFlying` (L101-129):
```java
if (dim.height() >= 1.0F) return;
if (dim.eyeHeight() <= 1.0F && pose != EntityPose.SLIDING) return;
// box +1m up
```

- swim/dive 시 POSE=SWIMMING (`sm_updatePose_client` 매핑 = isHJ||isSL||isFlying||isLevitating || swim||dive → SLIDING/SWIMMING).
- **검증 필요**: swim/dive 시 POSE=SWIMMING 인지 SLIDING 인지 — sm_updatePose 분기 정확 확인.
- dim eye 1.62 (= heightOffset=-1F 활성 시) 인지 0.62 인지 — `sm_getBaseDimensions` 분기 확인.

### 6-2. SmartMovingState packet bits

`SmartMovingState.java` 의 비트 매핑 변경 시 [[feedback_tickessential_self_only]] 패턴 따라:
1. self side `tickEssential` 에 비트 set.
2. server side `processStatePacket` 끝 `calculateDimensions()` 호출.
3. remote side packet lambda 안 즉시 setPos / lerp cancel (필요 시).

---

## 7. 다음 단계

`docs/checklist_swimming.md` 참조. Phase 1 검증부터 시작.
