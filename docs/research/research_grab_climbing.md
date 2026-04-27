# Grab 키 클라이밍 (Free Climb) 리서치

## 문제

사용자 보고: **Grab 키 (LEFT_CTRL) 를 눌러도 클라이밍 (자유 등반) 이 작동 안 함**.

## 원본 흐름 (1.7.10 SmartMoving) — 라인 단위

### 1. 키 입력 → state 변환

**`SmartMovingOptions.java:110`**
```java
keyBindGrab = new KeyBinding("key.climb", _defaultGrabKeyCode.value, "key.categories.gameplay");
```

**`SmartMovingSelf.java:2387`**
```java
grabButton.update(Options.keyBindGrab);
```

**`SmartMovingSelf.java:2467-2477` (wouldWantClimb)**
```java
boolean wouldWantClimb =
    (grabButton.Pressed
     || (isClimbHolding && sneakButton.Pressed)
     || (Config.isFreeClimbAutoLaddderEnabled() && isFacedToLadder(...))
     || (Config.isFreeClimbAutoVineEnabled() && isFacedToSolidVine))
    && (!isSliding || ...) && !isHeadJumping && !disabled;
```

**L2479-2481 (wantClimb)**
```java
boolean wantClimb = Config.isFreeClimbingEnabled() && wouldWantClimb;
```

**L2489-2493 (wantClimbUp)**
```java
wantClimbUp = wantClimb && esp.movementInput.moveForward > 0F || ...;
```

### 2. handleClimbing 호출 (L657)

```java
handleClimbing(isOnLadder, isOnVine, wasClimbing);
```
→ 매 tick **무조건 호출** (조건 없음).

### 3. handleClimbing 본문 (L814-1110)

**L820 Standard Base Climb**: `isCollidedHorizontally && isOnLadderOrVine` → motionY = 0.2 * factor.
**L825 Simple Base Climb**: 동일 + feet/hands isClimbable.
**L856 Smart Base Climb**: 동일 + handsSubstitute/feetSubstitute.
**L896 Free Climb**: `fallDistance <= max && (!isOnLadderOrVine || isFreeBaseClimb)` — **isOnLadderOrVine 무관 (= 일반 벽 OK)**.

### 4. Free Climb 8방향 탐색 (L928-961)

```java
HandsClimbing handsClimbing = HandsClimbing.None;
FeetClimbing feetClimbing = FeetClimbing.None;
inout_handsClimbing[0] = handsClimbing;
inout_feetClimbing[0] = feetClimbing;
out_handsClimbGap.reset();
out_feetClimbGap.reset();

// 4방향 (PZ/NZ/ZP/ZN)
Orientation.PZ.seekClimbGap(rotation, world, i, id, jh, k, kd, ...);
Orientation.NZ.seekClimbGap(...);
Orientation.ZP.seekClimbGap(...);
Orientation.ZN.seekClimbGap(...);

handsClimbing = inout_handsClimbing[0];
feetClimbing = inout_feetClimbing[0];

if(!isSmallClimbing) {
    // 대각 4방향 (PP/NP/NN/PN)
    Orientation.PP.seekClimbGap(...);
    Orientation.NP.seekClimbGap(...);
    Orientation.NN.seekClimbGap(...);
    Orientation.PN.seekClimbGap(...);
}
```

### 5. 발동 (L979-1095)

```java
if(feetClimbing.IsRelevant() || handsClimbing.IsRelevant()) {
    if(wantClimbUp) {
        // ... setShouldClimbSpeed(...)
    } else if(wantClimbDown) {
        // ...
    }
}
```

### 6. setOnlyShouldClimbSpeed (L1513-1551)

```java
private void setOnlyShouldClimbSpeed(double value) {
    isClimbing = true;
    if(this.climbIntoCount > 0) value = HoldMotion;
    if(value != HoldMotion) {
        float factor = getCombinedSpeedFactor();
        if(isFast) factor *= Config._sprintFactor.value;
        // ...
        if(value > HoldMotion)
            value = ((value - HoldMotion) * Config._freeClimbingUpSpeedFactor.value * factor + HoldMotion);
        else
            value = HoldMotion - (HoldMotion - value) * Config._freeClimbingDownSpeedFactor.value * factor;
    }
    boolean relevant = value < 0 || value > sp.motionY;
    if(relevant) sp.motionY = value;
    isClimbJumping = !relevant && !isClimbHolding;
}
```

## 우리 매핑 현황 (1.21.1 Fabric)

### 잘 되어 있는 부분 ✅

- `SmartMovingKeys.java:21-32`: grab 키 등록 (LEFT_CTRL).
- `SmartMovingClientState.tickEssential`: `wouldWantClimb`, `wantClimb`, `wantClimbUp/Down` 갱신 (Agent 보고 L1250-1262, L1289-1295).
- `Orientation.java:3026`: `seekClimbGap` 메서드 이식.
- `SmartMovingClimber.handleClimbing` (L289-651): 8방향 탐색 + 속도 결정 + setShouldClimbSpeed 모두 이식.
- `SmartMovingClimber.getOnLadderOrVine`: 사다리/덩굴 4방향 탐색.

### 🔴 break point — `MixinLivingEntityClient.travel` L198-201

```java
SmartMovingClimber.getOnLadderOrVine(player, world, isSmall, false, hands, feet, ...);
boolean onClimbable = hands[0].isRelevant() || feet[0].isRelevant();
if (!onClimbable && !sm.isCeilingClimbing) return;  // ← 사다리/덩굴 없으면 즉시 return
if (onClimbable) SmartMovingClimber.handleClimbing(player, sm);
```

**문제**:
- `getOnLadderOrVine` 은 사다리/덩굴 만 감지.
- 일반 벽 (Free Climb 대상) 은 미감지 → `onClimbable=false` → 즉시 return.
- → `handleClimbing` 호출 안 됨 → 안의 `seekClimbGap` 8방향 검사 도달 안 됨.

**원본은 `handleClimbing` 무조건 호출**. 우리 가드가 잘못됨.

## 원인 한 줄 요약

`MixinLivingEntityClient.travel` 의 `onClimbable` 가드가 Free Climb (= 일반 벽 grab) 케이스를 차단. 원본은 무조건 `handleClimbing` 호출이라 일반 벽에서도 `seekClimbGap` 8방향 검사가 발동. 우리는 사다리/덩굴 인접 시만 진입.

## 해결 방향

`MixinLivingEntityClient.travel` 의 가드에 `sm.wantClimb` (= grab 키 + freeClimb 활성) 추가:
```java
if (!onClimbable && !sm.isCeilingClimbing && !sm.wantClimb) return;
if (onClimbable || sm.wantClimb) SmartMovingClimber.handleClimbing(player, sm);
```

`sm.wantClimb` 는 이미 `tickEssential` 에서 매 tick 갱신됨 (Agent 보고). `cfg.freeClimb && cfg.enabled && wouldWantClimb` 검증 후 set.

→ 일반 벽에서도 grab 키 누름 시 `handleClimbing` 호출 → 안의 8방향 `seekClimbGap` 검사 → `handsClimbing/feetClimbing` 갱신 → Free Climb 발동.

## 사이드 효과 검토

- Standard/Simple/Smart Base Climb 분기 (handleClimbing 안의 L306-585) 는 `horizontalCollision` 검사 그대로. `wantClimb` 만으로 통과해도 그 분기들은 자체 가드로 미발동.
- Free Climb 분기 (L587-) 는 `wantClimb`/`wantClimbUp`/`wantClimbDown` 자체 검사. 정상 처리.
- isCeilingClimbing 분기 (`handleCeilingClimbing` L202) 는 변경 없음.

## 추가 확인 필요

- `sm.wantClimb` 필드가 정말 `tickEssential` 에서 매 tick 갱신되는지 직접 확인. ✅ L1262.
- `SmartMovingConfig.freeClimb` 기본값 = true 인지 확인 (false 면 wantClimb 항상 false). ✅ L464.

## 추가 발견 — 천장 클라이밍 블록 종류 검사 누락

원본 `SmartMovingSelf.handleCeilingClimbing` L1139-1145:
```java
Block topBlock = supportsCeilingClimbing(i, j, k);
Block bottomBlock = supportsCeilingClimbing(i, j + 1, k);
if (topBlock != null || bottomBlock != null) {
    // ... 천장 climb 발동
}
```

원본 `SmartMovingBase.supportsCeilingClimbing` L95-121:
- `Config._ceilingClimbConfigurationObject` Dictionary 검색.
- 기본 dictionary (`SmartMovingConfig.java:142`):
  ```java
  new String[] { "tile.fenceIron", "tile.trapdoor/0/1/2/3", "tile.trapdoor_iron/0/1/2/3" }
  ```
- = **iron bars + closed trapdoor + closed iron trapdoor** 만 천장 climb.

우리 매핑:
- `CeilingClimbBlocks.supports(BlockState)` 헬퍼 **이미 이식**됨 (`climbing/CeilingClimbBlocks.java:32-45`).
  - `Blocks.IRON_BARS || (TrapdoorBlock && !OPEN)`.
- 그러나 `SmartMovingClimber.handleCeilingClimbing` 본문에서 **호출 안 됨**.
- → 모든 솔리드 천장에서 climb 발동 (원본보다 관대) 또는 사용자 보고처럼 부분 미발동.

수정 완료: `SmartMovingClimber.handleCeilingClimbing` 에 `CeilingClimbBlocks.supports(top/bottom)` 호출 추가.

## Free climb fence 등반

- `Orientation.isWallBlock` 이미 이식 (`PaneBlock + FenceBlock + WallBlock + closed FenceGate`).
- `seekClimbGap` 안에서 `isWallBlock`/`isFence` 호출 (L1167, L1513-1521).
- 이론상 fence/iron bars/wall climb 가능.
- 인게임 검증 후 미발동 시 추가 디버깅 필요.
