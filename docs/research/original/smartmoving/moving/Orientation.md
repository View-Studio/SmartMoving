# Orientation.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/Orientation.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: `SmartMovingContext`  
파일 크기: 약 74KB

---

## 전체 소스 — 구조 개요

```java
package net.smart.moving;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.block.*;
import net.minecraft.block.material.*;
import net.minecraft.client.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.smart.moving.config.*;
import net.smart.utilities.*;
```

---

## 역할

특정 **수평 방향**을 나타내는 클래스이자, 그 방향에 대한 **클라이밍 가능 여부 판정** 엔진. `SmartMovingSelf`에서 4방향(또는 8방향)을 순회하며 `seekClimbGap()`을 호출해서 각 방향의 `HandsClimbing`/`FeetClimbing` 상태를 계산한다.

---

## 인스턴스 상수 (방향 9종)

```java
public static final Orientation ZZ = new Orientation(0, 0);   // 정지 (방향 없음)

public static final Orientation PZ = new Orientation(1, 0);   // +X (남쪽)
public static final Orientation ZP = new Orientation(0, 1);   // +Z (동쪽)
public static final Orientation NZ = new Orientation(-1, 0);  // -X (북쪽)
public static final Orientation ZN = new Orientation(0, -1);  // -Z (서쪽)

public static final Orientation PP = new Orientation(1, 1);   // +X+Z (남동)
public static final Orientation NN = new Orientation(-1, -1); // -X-Z (북서)
public static final Orientation PN = new Orientation(1, -1);  // +X-Z (남서)
public static final Orientation NP = new Orientation(-1, 1);  // -X+Z (북동)
```

`_i` = X축 성분(-1,0,1), `_k` = Z축 성분(-1,0,1).

```java
public static final HashSet<Orientation> Orthogonals = new HashSet<Orientation>();
static {
    Orthogonals.add(PZ);
    Orthogonals.add(ZP);
    Orthogonals.add(NZ);
    Orthogonals.add(ZN);
}
```

4방향(직교) 집합. `ZZ`, 대각선 4개는 포함하지 않음.

---

## 인스턴스 필드

```java
protected int _i, _k;
private boolean _isDiagonal;    // _i != 0 && _k != 0
private float _directionAngle;
private float _mimimumClimbingAngle;  // 오타: minimum
private float _maximumClimbingAngle;
```

---

## 생성자 (private)

```java
private Orientation(int i, int k)
{
    _i = i;
    _k = k;
    _isDiagonal = _i != 0 && _k != 0;
    setClimbingAngles();
}
```

`private` — 9개 상수만 존재. `setClimbingAngles()`로 방향별 클라이밍 각도 범위 초기화.

---

## int 상수

```java
public static final int DefaultMeta = -1;
public static final int VineFrontMeta = 0;
public static final int VineSideMeta = 1;

private static final int top = 2;
private static final int middle = 1;
private static final int base = 0;
private static final int sub = -1;
private static final int subSub = -2;

private static final int NoGrab = 0;
private static final int HalfGrab = 1;
private static final int AroundGrab = 2;
```

- `top/middle/base/sub/subSub`: 손/발 위치 오프셋 (반-블록 단위 수직 위치)
- `NoGrab/HalfGrab/AroundGrab`: 잡는 방식
- `DefaultMeta=-1`: 메타데이터 미설정 센티널
- `VineFrontMeta=0`, `VineSideMeta=1`: 덩굴 방향 구분

---

## 주요 메서드

### `rotate(int angle)`

```java
public Orientation rotate(int angle)
```

지원 각도: 0, ±45, ±90, ±135, ±180.  
`ZZ`에 rotate 시 `RuntimeException("unrotatable orientation")`.  
45도 회전: 인스턴스 동일성(`==`)으로 매핑 (PZ→PP→ZP→NP→NZ→NN→ZN→PN→PZ).  
±90: `rotate(±45).rotate(±45)`, ±135: `rotate(±180).rotate(∓45)`.

---

### `getOrientation(EntityPlayer p, float tolerance, boolean orthogonals, boolean diagonals)`

```java
public static Orientation getOrientation(EntityPlayer p, float tolerance, boolean orthogonals, boolean diagonals)
```

`p.rotationYaw % 360F` → `[0, 360)` 범위 정규화 → `[rotation-tolerance, rotation+tolerance]` 범위에 속하는 Orientation 반환. `null` 반환 가능 (범위 안에 없을 때).

---

### `getClimbingOrientations(EntityPlayer p, boolean orthogonals, boolean diagonals)`

```java
public static HashSet<Orientation> getClimbingOrientations(EntityPlayer p, boolean orthogonals, boolean diagonals)
```

플레이어 `rotationYaw`에 대해 `isRotationForClimbing(rotation)`이 `true`인 방향들을 반환. `_getClimbingOrientationsHashSet` static HashSet를 재사용(clear 후 사용).

---

### `setClimbingAngles()`

```java
@SuppressWarnings("incomplete-switch")
private boolean setClimbingAngles()
```

`_i`, `_k` 값으로 방향별 `_directionAngle` 결정:

| Orientation | `_directionAngle` |
|-------------|-------------------|
| ZZ | 0~360 (전방향) |
| ZP | 0° |
| NP | 45° |
| NZ | 90° |
| NN | 135° |
| ZN | 180° |
| PN | 225° |
| PZ | 270° |
| PP | 315° |

`halfAreaAngle = (_isDiagonal ? Config._freeClimbingDiagonalDirectionAngle.value : Config._freeClimbingOrthogonalDirectionAngle.value) / 2F`  
→ `[directionAngle - halfAreaAngle, directionAngle + halfAreaAngle]` 가 `_mimimumClimbingAngle`~`_maximumClimbingAngle`.

---

### `isRotationForClimbing(float rotation)` / `isWithinAngle(...)`

```java
private boolean isRotationForClimbing(float rotation) {
    return isWithinAngle(rotation, _mimimumClimbingAngle, _maximumClimbingAngle);
}

private static boolean isWithinAngle(float rotation, float minimumRotation, float maximumRotation) {
    if(minimumRotation > maximumRotation)
        return rotation >= minimumRotation || rotation <= maximumRotation;
    return rotation >= minimumRotation && rotation <= maximumRotation;
}
```

`minimumRotation > maximumRotation`이면 360°를 넘어서 감싸는 경우 (OR 조건).

---

### `seekClimbGap(...)`

```java
public void seekClimbGap(float rotation, World world, int i, double id, double jhd, int k, double kd,
    boolean isClimbCrawling, boolean isCrawlClimbing, boolean isCrawling,
    HandsClimbing[] inout_handsClimbing, FeetClimbing[] inout_feetClimbing,
    ClimbGap out_handsClimbGap, ClimbGap out_feetClimbGap)
```

1. `isRotationForClimbing(rotation)` → false이면 즉시 반환
2. `initialize(world, i, id, jhd, k, kd)` — static 작업 변수 설정
3. `handsClimbing(...)` 계산 → `inout_handsClimbing[0].max(...)` 갱신
4. `feetClimbing(...)` 계산 → `inout_feetClimbing[0].max(...)` 갱신

`inout_handsClimbing[0]`, `inout_feetClimbing[0]`: 배열 1원소로 in-out 전달 패턴.

---

### `handsClimbing(boolean isClimbCrawling, boolean isCrawlClimbing, boolean isCrawling, ClimbGap out_climbGap)` (private)

손 클라이밍 상태 계산. `initializeOffset(3D, ...)` — 손 위치 오프셋 3 half-blocks.

| 조건 | 반환 |
|------|------|
| `isLadderSubstitute(middle)>0` && `jh_offset > 1D-_handClimbingHoldGap` | `Up` |
| `isLadderSubstitute(middle)>0` && 나머지 | `None` |
| `isLadderSubstitute(base)>0` && `jh_offset < _handClimbingHoldGap` | `BottomHold` |
| `isLadderSubstitute(base)>0` && 나머지 | `Up` |
| `isLadderSubstitute(sub)>0` && 다양한 조건 | `FastUp`, `Up`, `TopHold`, `Sink` |
| `isLadderSubstitute(subSub)>0` && 다양한 조건 | `TopHold`, `FastUp`, `Sink` |

`_climbGapTemp.SkipGaps = isClimbCrawling || isCrawlClimbing` — sub/subSub에서 갭 합산 제어.

---

### `feetClimbing(boolean isClimbCrawling, boolean isCrawlClimbing, boolean isCrawling, ClimbGap out_climbGap)` (private)

발 클라이밍 상태 계산. `initializeOffset(0D, ...)` — 발 위치 오프셋 0.

| 조건 | 반환 |
|------|------|
| `isLadderSubstitute(top)>0` | `None` (발 너무 위) |
| `isLadderSubstitute(middle)>0` && `gap>3` && !isClimbCrawling && !isCrawlClimbing | `FastUp` |
| `isLadderSubstitute(middle)>0` && (isClimbCrawling\|\|isCrawlClimbing) && gap>1 | `BaseWithHands` 또는 `FastUp` |
| `isLadderSubstitute(middle)>0` && `gap>2` && !isClimbCrawling | `SlowUpWithHoldWithoutHands` |
| `isLadderSubstitute(middle)>0` && 나머지 | `TopWithHands` |
| `isLadderSubstitute(base)>0` && `gap>3` && !isCrawling && !isCrawlClimbing | `FastUp` |
| `isLadderSubstitute(base)>0` && `gap>2` && !isCrawling | `SlowUpWithHoldWithoutHands` 또는 `SlowUpWithSinkWithoutHands` |
| `isLadderSubstitute(base)>0` && 나머지 | `BaseWithHands` 또는 `BaseHold` |
| `isLadderSubstitute(sub)>0` | `None` (발 너무 아래) |
| `isCrawlClimbing \|\| isCrawling` | `BaseWithHands` (추가 OR) |

---

### `isLadderSubstitute(int local_Offset, ClimbGap out_climbGap)` (private)

핵심 판정 메서드. 해당 반-블록 오프셋 위치에서 잡을 수 있는지 확인하고 갭 크기(int)를 반환.

`local_half == 1`이면 `hasHalfHold()`, `local_half == 0`이면 `hasBottomHold()` 호출.

갭 반환값 의미:
- `0`: 잡을 수 없음
- `1`: 최소 갭 (사다리/덩굴 등)
- `2`: 기본 갭
- `3`: 크롤 필요 갭
- `4~5`: 서 있을 수 있는 큰 갭

```java
if(out_climbGap != null && gap > 0)
{
    out_climbGap.Block = grabBlock;
    out_climbGap.Meta = grabMeta;
    out_climbGap.CanStand = gap > 3;
    out_climbGap.MustCrawl = gap > 1 && gap < 4;
    out_climbGap.Direction = this;
}
```

`CanStand = gap > 3` (4, 5), `MustCrawl = gap > 1 && gap < 4` (2, 3).

---

### `hasHalfHold()` (private)

상체(half) 위치에서 잡을 수 있는지. 우선순위 순서:

1. `Config.isFreeBaseClimb()` && (`isOnLadder(0)&&isOnLadderFront(0)` → AroundGrab) 또는 (`remoteLadderClimbing(0)` → AroundGrab)
2. `SmartMovingOptions.hasBetterThanWolves || hasRopesPlus`: 로프(`getRopeId(0)`) → AroundGrab, 앵커(`getAnchorId(0)`) → HalfGrab
3. `iron_bars` (remote) && `headedToFrontWall` → HalfGrab
4. `iron_bars` (base wall) → HalfGrab, `isOnMiddleLadderFront(0)` → AroundGrab
5. `Config._freeFenceClimbing.value`: 펜스/cobblestone_wall 등 다수 조건 → HalfGrab
6. `isBottomHalfBlock` 또는 `isStairCompact && isBottomStairCompactNotBack` → HalfGrab
7. `isTrapDoor && isClosedTrapDoor` → HalfGrab
8. `isTrapDoor(base) && !isClosedTrapDoor` → HalfGrab (false remote)
9. `SmartMovingOptions.hasASGrapplingHook || hasRopesPlus`: AS 로프 조건 → HalfGrab
10. `Config.isFreeBaseClimb()`: 덩굴 클라이밍 → HalfGrab
11. 없으면 → NoGrab (false 반환)

대각선(`_isDiagonal`)이고 remote인 경우: CCW/CW 방향의 `isUpperHalfFrontEmpty` 둘 다 true여야 hasGrab=true.

---

### `hasBottomHold()` (private)

하체(bottom) 위치에서 잡을 수 있는지. 유사한 구조로 offset -1, 0 두 레벨을 확인.

1. `Config.isFreeBaseClimb()`: 사다리/덩굴 (-1, 0 두 레벨)
2. BetterThanWolves/RopesPlus: 로프, 앵커
3. RedPowerWire: 커버 사이드 비트 기반 판정
4. `iron_bars` → HalfGrab
5. `Config._freeFenceClimbing.value`: 펜스/cobblestone_wall
6. 벽 블록 (`belowWallBlockId`): iron_bars, 중간 사다리, grab wall
7. 반-블록/계단/트랩도어/문 등 다수 조건
8. AS 그래플링 훅/로프
9. 덩굴 (-1, 0 두 레벨)
10. 없으면 → NoGrab

---

### `setHalfGrabType` / `setBottomGrabType` / `setGrabType` (private)

```java
private static boolean setGrabType(int type, Block block, boolean remote, boolean hasGrab, int metaClimb)
{
    grabRemote = remote;
    grabType = hasGrab ? type : NoGrab;
    grabBlock = block;
    grabMeta = metaClimb;
    return hasGrab;
}
```

static 변수(`grabRemote`, `grabType`, `grabBlock`, `grabMeta`)에 결과 저장. `hasGrab`이 false이면 `grabType = NoGrab`.

대각선 체크:
- `setHalfGrabType`: `rotate(90).isUpperHalfFrontEmpty(base_i, 0, remote_k) && rotate(-90).isUpperHalfFrontEmpty(remote_i, 0, base_k)` — 두 방향 다 비어야 함
- `setBottomGrabType`: `rotate(90).isLowerHalfFrontFullEmpty(...)` 동일 패턴

---

### `getHorizontalBorderGap(Entity entity)` / `getHorizontalBorderGap(double i, double k)`

```java
public double getHorizontalBorderGap(Entity entity) {
    return getHorizontalBorderGap(entity.posX, entity.posZ);
}
private double getHorizontalBorderGap(double i, double k) {
    if(this == NZ) return i % 1;
    if(this == PZ) return 1 - (i % 1);
    if(this == ZN) return k % 1;
    if(this == ZP) return 1 - (k % 1);
    return 0D;
}
```

직교 방향에서 블록 경계로부터의 거리(0~1). 덩굴 사이드 클라이밍에서 `>= 0.65` 조건으로 사용.

---

### `isTunnelAhead(World world, int i, int j, int k)`

```java
public boolean isTunnelAhead(World world, int i, int j, int k)
{
    Block remoteId = world.getBlock(i + _i, j + 1, k + _k);
    if(isFullEmpty(remoteId)) {
        Material aboveMaterial = world.getBlock(i + _i, j + 2, k + _k).getMaterial();
        if(aboveMaterial != null && isSolid(aboveMaterial))
            return true;
    }
    return false;
}
```

앞 방향의 `j+1` 위치가 비어 있고 `j+2` 위치가 solid이면 터널 판정.

---

### `isFeetLadderSubstitute` / `isHandsLadderSubstitute`

```java
public boolean isFeetLadderSubstitute(World world, int bi, int j, int bk) {
    int i = bi + _i, k = bk + _k;
    return isLadderSubstitute(world, i, j, k, middle) > 0 || isLadderSubstitute(world, i, j, k, base) > 0;
}

public boolean isHandsLadderSubstitute(World world, int bi, int j, int bk) {
    int i = bi + _i, k = bk + _k;
    return isLadderSubstitute(world, i, j, k, middle) > 0 || isLadderSubstitute(world, i, j, k, base) > 0
        || isLadderSubstitute(world, i, j, k, sub) > 0;
}
```

단순화된 진입점. static 변수 없이 직접 월드를 받아서 판정.

---

### 블록 판별 메서드 (public static)

```java
public static boolean isLadder(Block block)  // block == "ladder"
public static boolean isVine(Block block)    // block == "vine"
public static boolean isLadderOrVine(Block block)  // isLadder || isVine || LadderKit
public static boolean isKnownLadder(Block block)   // isLadder || LadderKit (vine 제외)
public static boolean isClimbable(World world, int i, int j, int k)  // block.isLadder()
public static boolean isTrapDoor(Block block)
public static boolean isClosedTrapDoor(int metaData)  // (metaData & 4) == 0
public static boolean getFiniteLiquidWater(Block block)  // NoCean/FiniteWater 모드용
public static Orientation getKnownLadderOrientation(World world, int i, int j, int k)
public static Orientation getOpenTrapDoorOrientation(World world, int i, int j, int k)
```

---

### `hasVineOrientation(World world, int i, int j, int k)`

```java
public boolean hasVineOrientation(World world, int i, int j, int k)
{
    int metaData = world.getBlockMetadata(i, j, k);
    if(this == NZ) return (metaData & 2) != 0;
    if(this == PZ) return (metaData & 8) != 0;
    if(this == ZP) return (metaData & 1) != 0;
    if(this == ZN) return (metaData & 4) != 0;
    return false;
}
```

덩굴 메타데이터 비트로 덩굴 방향 확인.

---

### `hasLadderOrientation(int i, int j_offset, int k)` (private)

LadderKit 사다리: `metadata & 0x3` 비트, 일반 사다리: `metadata & 0x7` 비트로 방향 확인. Carpenter's Blocks 사다리는 `getCarpentersBlockData()`로 별도 처리.

---

### `getKnownLadderOrientation(World world, int i, int j, int k)` (public static)

LadderKit: `metadata & 0x3` (1→NZ, 3→PZ, 0→ZP, 2→ZN).  
일반: `metadata & 0x7` (5→NZ, 4→PZ, 2→ZP, 3→ZN).

---

### 각도 헬퍼

```java
private static boolean isTopHalf(double d) {
    return (int)Math.abs(Math.floor(d * 2D)) % 2 == 1;
}

private static int getTriple(double primary, double secondary) {
    primary = primary - Math.floor(primary) - 0.5;
    secondary = secondary - Math.floor(secondary) - 0.5;
    if(Math.abs(primary) * 2 < Math.abs(secondary)) return 0;
    else if(primary > 0) return 1;
    else if(primary < 0) return -1;
    else return 0;
}
```

`isTopHalf`: 소수 부분이 0.5 이상이면 상반부.  
`getTriple`: 두 좌표 중 primary가 secondary보다 2배 이상 크면 primary 방향(-1/0/1).

---

### 반-블록/계단 판별

```java
private static boolean isBottomHalfBlock(Block block, int metadata) {
    if(isHalfBlock(block) && isHalfBlockBottomMetaData(metadata)) return true;
    if(block == Block.getBlockFromName("bed")) return true;
    if(SmartMovingOptions.hasBetterThanWolves && isAnchorId(block) && metadata == 1) return true;
    return false;
}
private static boolean isHalfBlockBottomMetaData(int metadata) { return (metadata & 8) == 0; }
private static boolean isHalfBlockTopMetaData(int metadata)    { return (metadata & 8) != 0; }
private static boolean isHalfBlock(Block block) {
    return isBlock(block, BlockSlab.class, _knownHalfBlocks) && !((BlockSlab)block).isOpaqueCube();
}
private static boolean isStairCompact(Block block) {
    return isBlock(block, BlockStairs.class, _knownCompactStairBlocks);
}
private static boolean isTopStairCompact(int stairMetadata) { return (stairMetadata & 4) != 0; }
```

---

### `isBlock(Block block, Class<?> type, Block[] baseBlocks)` (private static)

우선순위:
1. `type != null && baseBlocks.length > 1 && isBlockType(block, type)` → true
2. `baseBlocks[i] == block` → true
3. `isBlockType(block, type)` → true (타입 체크 fallback)
4. `baseBlocks[i].getClass().isAssignableFrom(block.getClass())` → true (서브클래스 체크)

---

### 접근성 판별 메서드

```java
private static boolean isBaseAccessible(int j_offset)          // base 위치 접근 가능
private static boolean isBaseAccessible(int j_offset, boolean bottom, boolean full) // 세부 옵션
private boolean isRemoteAccessible(int j_offset)               // remote 위치 접근 가능
private boolean isAccessAccessible(int j_offset)               // 대각선 통로 접근
private boolean isFullAccessible(int j_offset, boolean grabRemote)  // full 접근
private boolean isFullExtentAccessible(int j_offset, boolean grabRemote)  // RedPower 체크 포함
private boolean isJustLowerHalfExtentAccessible(int j_offset)  // 반-블록/계단 상반부
```

`isEmpty(int i, int j_offset, int k)`:
```java
return isFullEmpty(getBlock(i, j_offset, k)) && !isFence(i, j_offset - 1, k);
```

---

### `isFullEmpty(Block block)`

```java
private static boolean isFullEmpty(Block block) {
    if(block == null) return true;
    boolean empty = !isSolid(block.getMaterial());
    if(!empty && block == Block.getBlockFromName("standing_sign")) empty = true;
    if(!empty && block == Block.getBlockFromName("wall_sign"))     empty = true;
    if(!empty && block instanceof BlockPressurePlate)              empty = true;
    if(!empty && (hasASGrapplingHook || hasRopesPlus) && isASGrapplingHook(block)) empty = true;
    if(empty && (hasASGrapplingHook || hasRopesPlus) && isASRope(block))           empty = false;
    return empty;
}
```

간판, 압력판 = 비어 있음. AS 로프 = 비어 있지 않음(로프는 solid 취급).

---

### 호환 모드 블록 판별

```java
private static boolean isRopeId(Block block) {
    return SmartMovingOptions.hasBetterThanWolves && hasBlockName(block, "tile.fcRopeBlock") ||
           SmartMovingOptions.hasRopesPlus && hasBlockName(block, "tile.blockRopeCentral");
}
private static boolean isAnchorId(Block block)    { return hasBlockName(block, "tile.fcAnchor"); }
private static boolean isASRope(Block block)       { return hasBlockName(block, "tile.blockRope"); }
private static boolean isASGrapplingHook(Block block){ return hasBlockName(block, "tile.blockGrHk"); }
private static boolean isRedPowerWire(Block block) { return hasBlockName(block, "tile.rpwire"); }
```

---

### `getFiniteLiquidWater(Block block)` (public static)

```java
public static int getFiniteLiquidWater(Block block) {
    String blockName = getBlockName(block);
    if(blockName == null) return 0;
    if(blockName.equals("tile.nocean")) return 2;      // NoCean 모드 물
    if(blockName.equals("tile.nwater_still")) return 1; // FiniteWater 모드 정적 물
    return 0;
}
```

---

### `isRedPowerWireXxx` 판별 메서드들

```java
private static boolean isRedPowerWireTop(int coverSides)    { return (coverSides >> 1) % 2 == 1; }
private static boolean isRedPowerWireBottom(int coverSides) { return (coverSides >> 0) % 2 == 1; }
```

방향별 `isRedPowerWireFullFront/AnyFront/FullBack/AnyBack`: 직교 방향은 단일 비트, 대각 방향은 두 직교 방향의 AND/OR.

---

### `isTrapDoorFront(int trapDoorMetadata)`

```java
if(this == NZ) return (trapDoorMetadata & 3) == 3;
if(this == PZ) return (trapDoorMetadata & 3) == 2;
if(this == ZP) return (trapDoorMetadata & 3) == 0;
if(this == ZN) return (trapDoorMetadata & 3) == 1;
if(this == PN) return (trapDoorMetadata & 3) == 2 || (trapDoorMetadata & 3) == 1;
if(this == PP) return (trapDoorMetadata & 3) == 2 || (trapDoorMetadata & 3) == 0;
if(this == NN) return (trapDoorMetadata & 3) == 3 || (trapDoorMetadata & 3) == 1;
if(this == NP) return (trapDoorMetadata & 3) == 3 || (trapDoorMetadata & 3) == 0;
```

트랩도어 메타데이터 하위 2비트(방향)와 Orientation 일치 여부.

---

### 계단 방향 판별

```java
private boolean isStairCompactFront(int stairMetadata) // metadata & 3 기반
private boolean isStairCompactBack(int stairMetadata)
private boolean isBottomStairCompactNotBack(int)
private boolean isBottomStairCompactFront(int)
private boolean isTopStairCompactFront(int)
private boolean isTopStairCompactBack(int)
```

계단 방향: `metadata & 3`: 0=PZ, 1=NZ, 2=ZP, 3=ZN (각 Orientation별 매핑).

---

### 문(Door) 판별

```java
private static boolean isDoor(Block block) {
    return block == Block.getBlockFromName("wooden_door") || block == Block.getBlockFromName("iron_door");
}
private static boolean isDoorTop(int metaData) { return metaData == 8; }
private boolean isDoorFrontBlocked(int i, int j_offset, int k) // metaData(0-7) 기반
```

문 상단(metaData=8): 하단으로 재귀. 개방 방향 메타데이터별 _i/_k 조건으로 막힘 판정.

---

### 담장(Fence/Wall/Gate) 판별

```java
private static boolean isFenceBase(Block block) // BlockFence || BlockWall
private static boolean isFence(Block block, int i, int j, int k) // isFenceBase || isClosedFenceGate
private static boolean isFenceGate(Block block)  // BlockFenceGate
private static boolean isClosedFenceGate(int metdata) { return (metdata & 4) == 0; }
private static boolean isOpenFenceGate(Block block, int metdata)
private boolean isFenceGateFront(int metaData)  // direction%4 기반 방향 체크
```

---

### `getWallFlag(Orientation direction, int i, int j_offset, int k, Block block)`

```java
if(block instanceof BlockPane)
    return ((BlockPane)block).canPaneConnectToBlock(getBlock(i + direction._i, j_offset, k + direction._k));
else if(isFenceBase(block))
    // BlockFence.canConnectFenceTo, BlockWall.canConnectWallTo, or Reflect
else if (isFenceGate(block))
    return isClosedFenceGate(metaData) && isFenceGateFront(metaData);
else
    // Carpenter's Blocks: carpentersBlockData 0→X방향, 1→Z방향
```

---

### `headedToFrontWall / headedToBaseWall / headedToBaseGrabWall / headedToRemoteFlatWall`

플레이어가 벽을 향해 이동 중인지 판별. `getWallFlag()`로 4방향 연결 여부를 구해서 `headedToWall()` 또는 복잡한 조건식으로 판정. `headedToBaseWall`은 플레이어 위치 소수 부분(`isTopHalf(base_id)`, `isTopHalf(base_kd)`)을 기반으로 8개 방향-위치 조합 처리.

---

### `isOnMiddleLadderFront(int j_offset)` (private)

```java
switch(getCarpentersBlockData(base_i, j_offset, base_k))
{
    case 0: // ZN→isTopHalf(base_kd), ZP→!isTopHalf(base_kd)
    case 1: // NZ→isTopHalf(base_id), PZ→!isTopHalf(base_id)
}
```

Carpenter's Blocks 사다리(data 0=Z방향, 1=X방향) 중간 위치 판정.

---

### `getCarpentersBlockData(int i, int j_offset, int k)` (private static)

```java
if (isExternalBlockType(getBlock(i, j_offset, k), _blockCarpentersLadder)) {
    TileEntity entity = getBlockTileEntity(i, j_offset, k);
    if (entity != null)
        return (Integer)Reflect.Invoke(_carpentersTEBaseBlockGetData, entity);
}
return -1;
```

리플렉션으로 `TEBase.getData()` 호출. 없으면 -1.

---

### `getRpCoverSides(int i, int j_offset, int k)` (private static)

```java
TileEntity tileEntity = getBlockTileEntity(i, j_offset, k);
Class<?> tileEntityClass = tileEntity.getClass();
while(!tileEntityClass.getSimpleName().equals("TileCovered"))
    tileEntityClass = tileEntityClass.getSuperclass();
return (Integer)Reflect.GetField(tileEntityClass, tileEntity, new Name("CoverSides"));
```

RedPower wire의 TileEntity에서 "TileCovered" 슈퍼클래스까지 거슬러 올라가 `CoverSides` 필드를 리플렉션으로 읽음.

---

### `isASGrapplingHookFront(int metaData)`

```java
boolean kPos = metaData % 2 != 0;
boolean iNeg = (metaData / 2) % 2 != 0;
boolean kNeg = (metaData / 4) % 2 != 0;
boolean iPos = (metaData / 8) % 2 != 0;
// _i, _k 방향에 따라 iPos/iNeg/kPos/kNeg 조합으로 판정
```

AS 그래플링 훅 메타데이터 비트(0~3비트)로 훅이 이 방향을 향하는지 판정.

---

### `isHeadedToRope()`

`getTriple(base_id, base_kd)` / `getTriple(base_kd, base_id)`로 플레이어 위치의 X/Z 분면을 판별해서 해당 방향의 Orientation과 일치 여부 확인.

---

### `baseVineClimbing` / `remoteVineClimbing`

덩굴 클라이밍 가능 여부 판정:
- `baseVineClimbing(j_offset)`: base 위치에 덩굴 있음 && (`isOnVineFront` 또는 인접 4방향 중 덩굴 방향 && `getHorizontalBorderGap() >= 0.65`)
- `remoteVineClimbing(j_offset)`: remote 위치 기반 유사 로직

---

## static 작업 변수 (private static)

```java
private static ClimbGap _climbGapTemp = new ClimbGap();
private static ClimbGap _climbGapOuterTemp = new ClimbGap();
private static HashSet<Orientation> _getClimbingOrientationsHashSet = null;

private static World world;
private static double base_jhd, jh_offset;
private static int all_j, all_offset;
private static int base_i, base_k;
private static double base_id, base_kd;
private static int remote_i, remote_k;
private static boolean crawl;

private static int local_halfOffset;
private static int local_half;
private static int local_offset;

private static boolean grabRemote;
private static int grabType;
private static Block grabBlock;
private static int grabMeta;
```

모두 `private static` — 스레드 안전하지 않음. 단일 스레드(클라이언트) 전용.

---

## `_handClimbingHoldGap` 상수

```java
private final static float _handClimbingHoldGap =
    Math.min(0.25F, 0.06F * Math.max(Config._freeClimbingUpSpeedFactor.value, Config._freeClimbingDownSpeedFactor.value));
```

손 클라이밍 홀드 갭. 최대 0.25F.

---

## `initialize` / `initializeOffset` / `initializeLocal`

```java
private void initialize(World w, int i, double id, double jhd, int k, double kd) {
    world = w;
    base_i = i; base_id = id; base_jhd = jhd; base_k = k; base_kd = kd;
    remote_i = i + _i; remote_k = k + _k;
}

private static void initializeOffset(double offset_halfs, boolean isClimbCrawling, boolean isCrawlClimbing, boolean isCrawling) {
    crawl = isClimbCrawling || isCrawlClimbing || isCrawling;
    double offset_jhd = base_jhd + offset_halfs;
    int offset_jh = MathHelper.floor_double(offset_jhd);
    jh_offset = offset_jhd - offset_jh;
    all_j = offset_jh / 2;
    all_offset = offset_jh % 2;
}

private static void initializeLocal(int localOffset) {
    local_halfOffset = localOffset + all_offset;
    local_half = Math.abs(local_halfOffset) % 2;
    local_offset = all_j + (local_halfOffset - local_half) / 2;
}
```

반-블록 단위(`jhd`: 반-블록 높이)로 수직 위치를 계산해서 `local_offset`(블록 Y)과 `local_half`(상/하 반부)를 결정.

---

## static 초기화 블록

```java
static
{
    Class<?> modFenceBlock = Reflect.LoadClass(Block.class, SmartMovingInstall.ModBlockFence, false);
    _canConnectFenceTo = modFenceBlock != null
        ? Reflect.GetMethod(modFenceBlock, new Name("canConnectFenceTo"), false, IBlockAccess.class, int.class, int.class, int.class)
        : null;

    _blockCarpentersLadder = Reflect.LoadClass(Block.class, SmartMovingInstall.CarpentersBlockLadder, false);
    if (_blockCarpentersLadder != null) {
        Class<?> carpentersTEBaseBlock = Reflect.LoadClass(Block.class, SmartMovingInstall.CarpentersTEBaseBlock, false);
        _carpentersTEBaseBlockGetData = Reflect.GetMethod(carpentersTEBaseBlock, SmartMovingInstall.CarpentersTEBaseBlock_getData);
    } else
        _carpentersTEBaseBlockGetData = null;

    _knownFanceGateBlocks = new Block[] { Block.getBlockFromName("fence_gate") };
    _knownFenceBlocks = new Block[] { Block.getBlockFromName("fence"), Block.getBlockFromName("nether_brick_fence") };
    _knownWallBlocks = new Block[] { Block.getBlockFromName("cobblestone_wall") };
    _knownHalfBlocks = new Block[] { "stone_slab", "double_stone_slab", "wooden_slab", "double_wooden_slab" };
    _knownCompactStairBlocks = new Block[] { /* 12종 계단 */ };
    _knownTrapDoorBlocks = new Block[] { Block.getBlockFromName("trapdoor") };
    _knownThinWallBlocks = new Block[] { Block.getBlockFromName("iron_bars"), Block.getBlockFromName("glass_pane") };

    // LadderKit: BlockRopeLadder, BlockSturdyLadder
    _ladderKitLadderTypes = ...; // 둘 다/하나만/null
}
```

초기화 시 호환 모드 클래스/메서드 리플렉션 로드, 알려진 블록 목록 초기화.

---

## `toString()`

```java
@Override
public String toString() {
    if(this == ZZ) return "ZZ";
    if(this == NZ) return "NZ";
    // ... 9종 전부
    return "UNKNOWN(" + _i + "," + _k + ")";
}
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingContext` | 상속 — Config, Options 접근 |
| `ClimbGap` | 갭 정보 저장/전달 |
| `HandsClimbing` | 손 클라이밍 상태 반환 |
| `FeetClimbing` | 발 클라이밍 상태 반환 |
| `SmartMovingInstall` | 호환 모드 Name 상수 |
| `SmartMovingOptions` | 호환 모드 존재 여부 플래그 |
| `SmartMovingConfig` (`Config`) | `isFreeBaseClimb()`, `_freeFenceClimbing`, 클라이밍 각도/속도 설정 |
| `net.smart.utilities.Reflect` | 리플렉션 유틸리티 |
| `World`, `Block`, `BlockXxx`, `TileEntity` | MC 블록/월드 API |
| `MathHelper.floor_double` | 소수점 내림 |

---

## 주요 관찰 사항

1. **반-블록 단위 수직 위치 시스템**: `jhd`(반-블록 높이 double)로 수직 위치를 추적. 1블록 = 2 half-blocks. `all_j`(블록 Y), `all_offset`(0 또는 1), `local_half`(0 또는 1)로 분해해서 `local_offset`(실제 world Y) 계산. 이 시스템으로 사다리 0.5블록 단위 정밀 판정.

2. **static 작업 변수 스레드 비안전**: `world`, `base_i`, `remote_i`, `grabBlock` 등 모두 static. 단일 클라이언트 스레드에서만 사용하는 전제. 멀티스레드 환경에서 레이스 컨디션 발생.

3. **갭 값 의미**: `isLadderSubstitute` 반환 int:
   - 0: 잡을 수 없음
   - 1: 최소 (사다리 인접만)
   - 2: 크롤 필요 (`MustCrawl=true`)
   - 3: 크롤 필요 (`MustCrawl=true`)
   - 4~5: 서 있을 수 있음 (`CanStand=true`)

4. **모드 호환성 레이어**: Orientation 하나에 BetterThanWolves, RopesPlus, ASGrapplingHook, LadderKit, Carpenter's Blocks, RedPower, BetterMisc, NoCean(FiniteWater) 등 8개 이상 모드의 블록 처리가 직접 내장.

5. **1.21.1 이식**:
   - 1.7.10 Block 메타데이터 → 1.21.1 BlockState 속성(Property)으로 전면 교체 필요
   - `Block.getBlockFromName(String)` → `Registries.BLOCK.get(Identifier.of(String))`
   - `World.getBlock(i,j,k)` → `World.getBlockState(BlockPos).getBlock()`
   - `World.getBlockMetadata(i,j,k)` → BlockState에서 property 값 조회
   - 1.7.10 호환 모드들(BetterThanWolves 등)은 1.21.1 존재 안 함 → 해당 코드 제거
   - static 작업 변수 → 메서드 파라미터로 전환 권장 (스레드 안전)
   - Carpenter's Blocks / LadderKit → 1.21.1 버전 확인 필요
