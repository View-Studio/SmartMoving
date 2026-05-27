# 리서치 — Space 연타 시 뱅글뱅글 + 하늘 상승 BUG

**작성일**: 2026-05-27
**사용자 보고 (verbatim)**: "서바이벌이나 비행이 disable 된 상태에서 스페이스바를 연타하면 플레이어가 뱅글뱅글 돌면서 하늘로 쭉 올라간다."
**사용자 가설**: "원본에서는 앞에 블록이 있을 때 w를 누른 상태로 jump를 연타하면 벽점프 같은게 되는 거 같은데 이게 제대로 안 옮겨와져서 그런 거 같다."

검증 결과 — **사용자 가설 정확**. cause 2건 식별:
- **Cause 1**: NaN 가드 누락 → 벽 없는 공중에서도 wall jump 발동 → setYaw + verticalMotion 누적 → "뱅글뱅글 + 하늘 상승".
- **Cause 2**: `updateWallJumpState` 식 순서 역전 → jumpKey release 시 wantWallJumping 자가유지 cycle → "탈출 안 됨".

사용자 추가 verbatim (2026-05-27):
- "지금 우리거는 그냥 벽이 없어도 평지에서도 발동됨" → Cause 1 확인.
- "원본은 jump키를 떼면 벽점프가 풀리는데 우리거는 탈출이 안됨" → Cause 2 신규 식별.

### 모드 분석 결과 (= 사용자 환경 매치)

원본 `canWallJumping = isWallJumpEnabled && !isHeadJumping && !sp.onGround && !isClimbing && !isSwimming && !isDiving && !isLevitating && !isFlying`.

- Creative + cfg.fly=true → space=비행 토글 → isFlying=true → wall jump 미발동.
- **Survival 또는 Creative+cfg.fly=false** → isFlying=false 가능 → 발동.

= 사용자 verbatim "서바이벌이나 비행이 disable 된 상태" 정확 매치.

---

## 1. 원본 mechanism (1.7.10 / 1.12.2 동일 패턴)

### 1.1 horizontalCollisionAngle 갱신 (`SmartMovingSelf.beforeMoveEntity` L1584-1592)

```java
if (wantWallJumping)
{
    int collisions = calculateSeparateCollisions(d, d1, d2);
    horizontalCollisionAngle = getHorizontalCollisionangle(
        (collisions & CollidedPositiveZ) != 0,
        (collisions & CollidedNegativeZ) != 0,
        (collisions & CollidedPositiveX) != 0,
        (collisions & CollidedNegativeX) != 0);
}
```

- `wantWallJumping=true` 시에만 계산 (= 매 tick beforeMoveEntity 안).
- `calculateSeparateCollisions(d, d1, d2)` — **이동 delta 기반 collision 검사** (`SmartMovingBase.java` L545-L788).
  - 박스를 par1/par3/par5 만큼 이동 시도 후 `worldObj.getCollidingBoundingBoxes` 매치 → 결과 par1 이 입력 d2 와 달라지면 (= 막힘) `isCollidedPositiveX` 등 bit set.
  - 단순 박스 옆 검사가 아닌 **실제 이동 시도 collision 결과**.

### 1.2 NaN 가드 (`SmartMovingSelf.handleWallJumping` L1948)

```java
public void handleWallJumping()
{
    if (!wantWallJumping || Double.isNaN(horizontalCollisionAngle))
        return;
    ...
}
```

- **벽 충돌 없음 = NaN = 즉시 return**. wall jump 발동 자체 X.
- 1.12.2 (`SMSelf.java` L1830) 도 정확히 동일: `if (!wantWallJumping || Double.isNaN(horizontalCollisionAngle)) return;`

### 1.3 `getHorizontalCollisionangle` NaN 반환 패턴 (`SRUtilities.java` L31-L74)

```java
public static float getHorizontalCollisionAngle(boolean posX, boolean negX, boolean posZ, boolean negZ) {
    if (posX)
        if (negX)
            if (posZ)
                if (negZ) ; else return 90F;
            else if (negZ) return 270F; else ;
        ...
    else if (negX) ...
    else if (posZ) ...
    else if (negZ) return 270F;
    else ;

    return Float.NaN;  // 충돌 없음 OR 양방향 동시 (=관통)
}
```

- 단방향 충돌만 valid 각도. 충돌 없음 / 양방향 충돌 = **NaN**.

### 1.4 트리거 식 (`SmartMovingSelf.updateActionState` L2865-2896)

```java
isWallJumping = false;
if (continueWallJumping && (sp.onGround || isClimbing || !jumpButton.Pressed))
    continueWallJumping = false;

boolean canWallJumping = Config.isWallJumpEnabled() && !isHeadJumping && !sp.onGround
    && !isClimbing && !isSwimming && !isDiving && !isLevitating && !isFlying;
boolean triggerWallJumping = false;

if (Options._wallJumpDoubleClick.value) {
    if (canWallJumping) {
        if (jumpButton.StartPressed) {
            if (wallJumpCount == 0)
                wallJumpCount = Options.wallJumpDoubleClickTicks();  // 3
            else {
                triggerWallJumping = true;
                wallJumpCount = 0;
            }
        }
        else if (wallJumpCount > 0)
            wallJumpCount--;
    } else
        wallJumpCount = 0;
} else
    triggerWallJumping = jumpButton.StartPressed;

wantWallJumping = canWallJumping &&
    (triggerWallJumping || continueWallJumping ||
    (wantWallJumping && jumpButton.Pressed && !sp.isCollidedHorizontally));
```

- **더블클릭 모드 기본 활성** (= `wallJumpDoubleClick=true`, default 3 tick window).
- 첫 jumpStart → wallJumpCount=3 set (= trigger=false).
- 3 tick 안 두번째 jumpStart → trigger=true.
- wantWallJumping 자기참조 = jumpKey hold + !horizontalCollision 이면 매 tick 유지.

### 1.5 jumpAngle 계산 (`SmartMovingSelf.handleWallJumping` L1965-L1996)

```java
float jumpAngle;
if (!wasCollidedHorizontally) {
    float movementAngle = getAngle(jumpMotionZ, -jumpMotionX);  // atan2 변형
    if (Double.isNaN(movementAngle)) return;
    jumpAngle = horizontalCollisionAngle * 2 - movementAngle + 180F;
} else
    jumpAngle = horizontalCollisionAngle;

while (jumpAngle > 360F) jumpAngle -= 360F;
... (orthogonalTolerance 정렬) ...

if (tryJump(jumpType, null, null, jumpAngle)) {
    continueWallJumping = !isHeadJumping;
    sp.isCollidedHorizontally = false;
    sp.rotationYaw = jumpAngle;
    onStartWallJump(jumpAngle);
}
```

- `tryJump(WallUp, ..., jumpAngle)` 의 D-11 분기 (`angle != null`) → setVelocity 새 방향 + verticalMotion = wallUpJumpVerticalFactor.
- `sp.rotationYaw = jumpAngle` — **즉시 회전**.

### 1.6 cfg 기본값 (우리 매핑, `SmartMovingConfig.java`)

| 필드 | 기본값 |
|------|--------|
| `wallUpJump` | **true** |
| `wallHeadJump` | **true** |
| `wallJumpDoubleClick` | **true** |
| `wallJumpDoubleClickTicks` | **3F** (= 150ms) |
| `wallUpJumpVerticalFactor` | 0.4F |
| `wallUpJumpFallMaximumDistance` | 2F |

= 사용자 환경에서 wall jump 기능 항상 활성. 더블클릭 모드 기본.

---

## 2. 우리 매핑 (`SmartMovingJumper.java`)

### 2.1 NaN 가드 누락 (L644)

```java
public static void handleWallJumping(ClientPlayerEntity player, SmartMovingClientState sm) {
    SmartMovingConfig cfg = SmartMovingConfig.Config;

    // 원본 L1948: 최우선 조건 — wantWallJumping=false 이면 즉시 return.
    if (!sm.wantWallJumping) return;        // ← NaN 가드 누락
    ...
    float horizontalCollisionAngle = calculateSeparateCollisionAngle(player, fallbackAngle);
    ...
}
```

- 원본 식: `if (!wantWallJumping || Double.isNaN(horizontalCollisionAngle)) return;`
- 우리 식: `if (!sm.wantWallJumping) return;` — **NaN 분기 부재**.

### 2.2 NaN fallback 으로 NaN 가드 무력화 (L717-L727)

```java
private static float calculateSeparateCollisionAngle(ClientPlayerEntity player, float movementAngle) {
    World world = player.getWorld();
    Box bb = player.getBoundingBox();
    double delta = 0.001D;
    boolean posX = !world.isSpaceEmpty(player, bb.offset( delta, 0, 0));
    boolean negX = !world.isSpaceEmpty(player, bb.offset(-delta, 0, 0));
    boolean posZ = !world.isSpaceEmpty(player, bb.offset(0, 0,  delta));
    boolean negZ = !world.isSpaceEmpty(player, bb.offset(0, 0, -delta));
    float angle = getHorizontalCollisionangle(posZ, negZ, posX, negX);
    return Float.isNaN(angle) ? (movementAngle + 180F) % 360F : angle;   // ← NaN 가드 무력화
}
```

- 원본 의도: `getHorizontalCollisionangle(false,false,false,false)` → NaN → handleWallJumping return.
- 우리 매핑: **NaN → fallback movementAngle 반환** → handleWallJumping 진행 → 공중 wall jump 발동.

### 2.3 추가 차이 — 검사 방식

| | 원본 (`calculateSeparateCollisions`) | 우리 (`calculateSeparateCollisionAngle`) |
|---|---|---|
| 입력 | 이동 delta (d, d1, d2) | (없음) |
| 방법 | 박스 이동 시도 후 막힌 축 검출 | 박스 0.001 미세 offset 후 `isSpaceEmpty` |
| 의미 | 실제 이동 시도 collision 결과 | 박스 옆 인접 solid 존재 여부 |

공중 (= 박스 옆 solid 없음) 시 우리 식도 모두 false → NaN → **단 fallback 식이 NaN 을 valid angle 으로 대체** → 결국 wall jump 진행.

검사 방식 차이는 secondary. **NaN fallback 이 primary cause**.

---

## 2.4 Cause 2 — `updateWallJumpState` 식 순서 역전 (L611-L625)

### 원본 (`SmartMovingSelf.updateEntityActionState` L2863-L2896) 순서

```java
isWallJumping = false;

if (continueWallJumping && (sp.onGround || isClimbing || !jumpButton.Pressed))
    continueWallJumping = false;        // ← 1. 먼저 false set

boolean canWallJumping = ...;
boolean triggerWallJumping = false;
... (double click 처리) ...

wantWallJumping = canWallJumping &&
    (triggerWallJumping || continueWallJumping ||  // ← 2. 그 다음 계산
    (wantWallJumping && jumpButton.Pressed && !sp.isCollidedHorizontally));
```

### 우리 매핑 — 순서 역전

```java
public static void updateWallJumpState(ClientPlayerEntity player, SmartMovingClientState sm) {
    ... (canWallJumping / double click 처리) ...

    boolean jumpPressed = MinecraftClient.getInstance().options.jumpKey.isPressed();
    sm.wantWallJumping = canWallJumping &&             // ← 1. 먼저 계산
        (sm.triggerWallJumping || sm.continueWallJumping ||
        (sm.wantWallJumping && jumpPressed && !player.horizontalCollision));

    if (sm.continueWallJumping && (player.isOnGround() || sm.isClimbing || !jumpPressed)) {
        sm.continueWallJumping = false;                // ← 2. 그 다음 false set
    }
}
```

### Cycle mechanism

| Tick | jumpKey | L618 (계산) | L623 (set) | handleWallJumping | L698 (set) |
|------|---------|-------------|------------|---------------------|-------------|
| N (hold) | true | continueWJ=true → wantWJ=**true** | jumpPressed=true → no change | tryJump 성공 | continueWJ=true |
| N+1 (release) | false | continueWJ=true 잔존 → wantWJ=**true** | jumpPressed=false → continueWJ=false | tryJump 성공 (1 tick 잔존) | continueWJ=**true 재set** |
| N+2 (release) | false | continueWJ=true 잔존 → wantWJ=**true** | continueWJ=false | tryJump 성공 | continueWJ=true 재set |
| ... | release | 영구 cycle | | | |

= jumpKey 떼도 wantWallJumping 매 tick true 잔존. handleWallJumping 매 tick 발동 → continueWallJumping=true 재set → **영구 자가유지**.

원본은 L2865 가 L2894 보다 먼저 → release 시점 다음 tick 의 wantWallJumping 식 우측 `continueWallJumping` 가 false 인 상태에서 계산 → 자기참조 `(wantWallJumping_prev && jumpPressed)` 도 jumpPressed=false 면 false → wantWallJumping=false → handleWallJumping return → continueWallJumping=false 유지.

---

## 3. BUG mechanism (사용자 시나리오 1:1 매치)

1. **첫 space** (= jumpKey rising edge):
   - vanilla 점프 발동 → 공중 (onGround=false).
   - 동시 `updateWallJumpState`: canWallJumping=false (= onGround=true 시점) → wallJumpCount=0 → triggerWallJumping=false → wantWallJumping=false.
   - 다음 tick onGround=false 후 jumpStart 가 재발생 가능.
2. **두번째 space** (= 3 tick 안 두번째 jumpStart):
   - canWallJumping=true (= !onGround + 다른 조건 ok).
   - wallJumpCount>0 → **triggerWallJumping=true** → wantWallJumping=true.
3. **handleWallJumping** 진입:
   - 우리 매핑: NaN 가드 누락 + `calculateSeparateCollisionAngle` NaN fallback → 공중에서도 valid angle 반환.
   - `wasCollidedHorizontally=false` (= 벽 없음) → `jumpAngle = fallbackAngle * 2 - movementAngle + 180F`.
   - `tryJump(WALL_UP, null, null, jumpAngle)`:
     - D-11 분기 → setVelocity 새 방향. motionY = -0.078 + 0.498 * 0.4 ≈ **+0.121** (즉, 매 발동 시 수직 점프).
     - `player.setYaw(jumpAngle); player.bodyYaw = jumpAngle;` → **즉시 회전**.
   - `continueWallJumping = !isHeadJumping = true`.
4. **계속 space hold**:
   - `wantWallJumping = canWallJumping && (... || (wantWallJumping && jumpPressed && !horizontalCollision))` → **공중 + jumpKey hold + 벽 충돌 없음 → 매 tick 유지**.
   - 매 tick handleWallJumping 발동 → 매 tick jumpAngle 변동 (= movementAngle 이 jumpMotion 의존, jumpMotion 매 tick 새 vel) → **회전 누적 = 뱅글뱅글**.
   - 매 tick verticalMotion += 0.121 → **상승 누적 = 하늘로 쭉**.

= 사용자 보고 mechanism 정확 일치.

---

## 4. 검증 추가 항목 (실측 권장)

1. **사용자 시나리오 정확화**:
   - Survival vs Creative+cfg.fly=false: 어느 모드에서? (둘 다 onGround=false 가능 → 둘 다 발동 가능 예측).
   - 환경: 평지 / 벽 옆 / 사다리 — 평지에서 발동 시 가설 1:1 매치.
   - 회전 방향 일정한가? (=jumpAngle 식이 jumpMotion 의존, 평지 정지 시 jumpMotion≈0 → movementAngle=NaN → 두번째 NaN 가드 매치 → return).
2. **두번째 NaN 가드** (`handleWallJumping` L1969):
   - `if (Float.isNaN(movementAngle)) return;` — 우리 매핑 L677 도 동일.
   - jumpMotion 둘 다 0 시 atan2(0,0)=0 (NaN 아님). 우리 매핑 fallback `fallbackAngle = atan2(-vel.x, vel.z)` 도 동일. **NaN 회피**.
   - 즉, 정지 상태에서도 fallback angle valid → wall jump 진행 가능.
3. **첫 space 가 wall jump 진입 막는지**:
   - 첫 jumpStartPressed 시 wallJumpCount=3 set (= trigger=false).
   - 단 onGround=true 시 canWallJumping=false → wallJumpCount=0 reset (= L609 else 블록).
   - **첫 jumpStart 시점 = onGround=true → wallJumpCount=0 유지** → 두번째 jumpStart 도 마찬가지.
   - 단 첫 점프 발동 → 다음 tick onGround=false → 두번째 jumpStart 가 그 시점에 도달 → canWallJumping=true → wallJumpCount=3 set → 세번째 jumpStart 시 trigger=true.
   - **연타 = ≥3번째 jumpStart 시 wall jump 발동**.

---

## 5. Fix 방향 (예비 — 체크리스트로 분리)

### Fix-A. NaN 가드 복원 (Cause 1)

`SmartMovingJumper.calculateSeparateCollisionAngle` 의 NaN fallback 제거 + `handleWallJumping` 에 NaN 가드 추가.

```java
// L644
if (!sm.wantWallJumping) return;
float horizontalCollisionAngle = calculateSeparateCollisionAngle(player);  // fallback 제거
if (Float.isNaN(horizontalCollisionAngle)) return;
```

```java
private static float calculateSeparateCollisionAngle(ClientPlayerEntity player) {
    // 0.001 offset 검사 결과 그대로 — NaN fallback 제거.
    ...
    return getHorizontalCollisionangle(posZ, negZ, posX, negX);
}
```

= 벽 없는 공중에서는 NaN 반환 → handleWallJumping return → wall jump 미발동.

### Fix-C. `updateWallJumpState` 식 순서 정정 (Cause 2)

원본 L2865-L2896 순서 1:1 복원.

```java
public static void updateWallJumpState(ClientPlayerEntity player, SmartMovingClientState sm) {
    SmartMovingConfig cfg = SmartMovingConfig.Config;

    // 1. (원본 L2865-L2866) continueWallJumping false 전환 — 먼저
    boolean jumpPressed = MinecraftClient.getInstance().options.jumpKey.isPressed();
    if (sm.continueWallJumping && (player.isOnGround() || sm.isClimbing || !jumpPressed)) {
        sm.continueWallJumping = false;
    }

    // 2. (원본 L2868) canWallJumping
    boolean isWallJumpEnabled = cfg.wallUpJump || cfg.wallHeadJump;
    boolean canWallJumping = isWallJumpEnabled && !sm.isHeadJumping && !player.isOnGround()
            && !sm.isClimbing && !sm.isSwimming_sm && !sm.isDiving
            && !sm.isLevitating && !sm.isFlying;

    // 3. (원본 L2869-L2892) double click 처리
    if (cfg.wallJumpDoubleClick) { ... } else { sm.triggerWallJumping = sm.jumpKeyStartPressed; }

    // 4. (원본 L2894-L2896) wantWallJumping — 마지막
    sm.wantWallJumping = canWallJumping &&
            (sm.triggerWallJumping || sm.continueWallJumping ||
             (sm.wantWallJumping && jumpPressed && !player.horizontalCollision));
}
```

= jumpKey release 시 continueWallJumping=false **먼저** set → wantWallJumping 계산 시 false 반영 → wantWallJumping=false → handleWallJumping return → 영구 cycle 차단.

### Fix-B. 검사 방식 정정 (옵션, deferred)

원본 `calculateSeparateCollisions(d, d1, d2)` 의 이동 시도 collision 결과 매핑. 1.21.1 vanilla `Entity.move` 의 collision 결과를 활용하거나, 별도 trial-move 헬퍼 작성.

= Fix-A + Fix-C 만으로 BUG 차단 시 Fix-B 보류 (= 원본 의미적 정확도 향상이지만 BUG 차단 필수 아님).

---

## 6. 관련 메모리

- `[[project_grab_climbing_complete]]` — handleClimbing 분기 (= wall jump 와 별도 path).
- `[[feedback_keybinding_rising_edge]]` — jumpKeyStartPressed rising-edge 패턴 (= 더블클릭 모드 매치 조건).
- `[[project_angle_jump_complete]]` — angle jump (= 더블클릭 방향 점프, wall jump 와 별도 path).
- `[[feedback_air_sprint_rawspeed]]` — air sprint (= 다른 시스템, vy 누적과 무관).

---

## 7. 결론

**Root cause 2건**:

1. **Cause 1 — NaN 가드 누락 (공중 발동)**: `SmartMovingJumper.calculateSeparateCollisionAngle` (L726) 의 NaN fallback 식이 원본 `handleWallJumping` (L1948) 의 NaN 가드를 무력화. 벽 없는 공중에서도 wall jump 발동 → setYaw + verticalMotion 누적 → "뱅글뱅글 + 상승".

2. **Cause 2 — `updateWallJumpState` 식 순서 역전 (탈출 안 됨)**: 우리 매핑 L618 (`wantWallJumping` 계산) 가 L623 (`continueWallJumping=false` set) 보다 먼저. 원본은 반대 순서. jumpKey release 시 wantWallJumping 식 우측 continueWallJumping 가 set 전 true 잔존 → wantWallJumping=true 매 tick 유지 → handleWallJumping 매 tick 발동 → continueWallJumping=true 재set → **영구 자가유지 cycle**.

**사용자 가설**: 정확. "원본은 앞에 블록 있을 때만 벽점프. 매핑에서 누락" — Cause 1 매치. "jump키 떼면 풀리는데 우리거는 탈출이 안됨" — Cause 2 매치.

**Fix 방향**: Fix-A (Cause 1, NaN 가드 복원) + Fix-C (Cause 2, 식 순서 정정) 동시 적용. Fix-B (검사 방식 정정) deferred.
