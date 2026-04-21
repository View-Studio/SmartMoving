# SmartMovingBase.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMovingBase.java  
패키지: `net.smart.moving`  
종류: `abstract class`  
상속: `SmartMovingBase extends SmartMovingContext`  
크기: 932줄

---

## 전체 소스

(932줄 전체 첨부 — 주요 메서드 전부 아래 섹션에서 코드 포함 분석)

---

## 역할

`SmartMovingContext`를 상속하고 `SmartMoving`(추상) 및 `SmartMovingSelf`에서 공통으로 사용하는 **유틸리티/물리 연산 메서드** 모음.

- 플레이어 주변 블록의 고체/액체 경계 계산
- 사다리/넝쿨 감지
- 3D 비행 이동 벡터 계산 (`moveFlying`)
- 충돌 계산 복제 (`calculateSeparateCollisions`)
- 물 흐름 역방향 처리 (`reverseHandleMaterialAcceleration`)

---

## 필드

```java
public final EntityPlayer sp;       // 플레이어 (서버/클라이언트 공통)
public final EntityPlayerSP esp;    // 클라이언트 전용. 서버 측이면 null
public final IEntityPlayerSP isp;   // PlayerAPI 인터페이스

public final static int CollidedPositiveX = 1;
public final static int CollidedNegativeX = 2;
public final static int CollidedPositiveY = 4;
public final static int CollidedNegativeY = 8;
public final static int CollidedPositiveZ = 16;
public final static int CollidedNegativeZ = 32;
```

---

## 생성자 (38-54줄)

```java
public SmartMovingBase(EntityPlayer sp, IEntityPlayerSP isp)
{
    this.sp = sp;
    this.isp = isp;

    if (sp instanceof EntityPlayerSP)
    {
        esp = (EntityPlayerSP) sp;
        if(Minecraft.getMinecraft().thePlayer == null)
        {
            Options.resetForNewGame();
            Config = Options;
        }
    }
    else
        esp = null;
}
```

- `sp instanceof EntityPlayerSP` → 클라이언트 플레이어. `thePlayer == null` 조건은 새 게임 시작 시 초기화.
- `esp = null` → 서버 측 플레이어 (ServerPlayerEntity 계열).
- `Config = Options` → 로컬 플레이어에서는 Config와 Options가 동일 인스턴스 (서버에서 별도 설정을 받기 전).

---

## `moveFlying` (56-93줄)

```java
protected void moveFlying(float moveUpward, float moveStrafing, float moveForward, float speedFactor, boolean treeDimensional)
{
    float diffMotionXStrafing = 0, diffMotionXForward = 0, diffMotionZStrafing = 0, diffMotionZForward = 0;
    {
        float total = MathHelper.sqrt_float(moveStrafing * moveStrafing + moveForward * moveForward);
        if(total >= 0.01F)
        {
            if(total < 1.0F)
                total = 1.0F;

            float moveStrafingFactor = moveStrafing / total;
            float moveForwardFactor = moveForward / total;
            float sin = MathHelper.sin((sp.rotationYaw * 3.141593F) / 180F);
            float cos = MathHelper.cos((sp.rotationYaw * 3.141593F) / 180F);
            diffMotionXStrafing = moveStrafingFactor * cos;
            diffMotionXForward = -moveForwardFactor * sin;
            diffMotionZStrafing = moveStrafingFactor * sin;
            diffMotionZForward = moveForwardFactor * cos;
        }
    }

    float rotation = treeDimensional ? sp.rotationPitch / RadiantToAngle : 0;
    float divingHorizontalFactor = MathHelper.cos(rotation);
    float divingVerticalFactor = -MathHelper.sin(rotation) * Math.signum(moveForward);

    float diffMotionX = diffMotionXForward * divingHorizontalFactor + diffMotionXStrafing;
    float diffMotionY = MathHelper.sqrt_float(diffMotionXForward * diffMotionXForward + diffMotionZForward * diffMotionZForward) * divingVerticalFactor + moveUpward;
    float diffMotionZ = diffMotionZForward * divingHorizontalFactor + diffMotionZStrafing;

    float total = MathHelper.sqrt_float(MathHelper.sqrt_float(diffMotionX * diffMotionX + diffMotionZ * diffMotionZ) + diffMotionY * diffMotionY);
    if(total > 0.01F)
    {
        float factor = speedFactor / total;
        sp.motionX += diffMotionX * factor;
        sp.motionY += diffMotionY * factor;
        sp.motionZ += diffMotionZ * factor;
    }
}
```

### 단계별 설명

**1단계: 수평 방향 벡터 (YAW 기반)**
- `total = sqrt(strafe² + forward²)`, 0.01 미만이면 처리 없음
- `total < 1.0F` → `total = 1.0F` (정규화 하한)
- `sin/cos` = `sp.rotationYaw * 3.141593F / 180F` (도→라디안, π = 3.141593F)
- `diffMotionXStrafing = (strafe/total) * cos`
- `diffMotionXForward = -(forward/total) * sin`
- `diffMotionZStrafing = (strafe/total) * sin`
- `diffMotionZForward = (forward/total) * cos`

**2단계: 피치 기반 수직 보정 (`treeDimensional = true` 시)**
- `rotation = sp.rotationPitch / RadiantToAngle` (라디안)
- `divingHorizontalFactor = cos(rotation)` — 수평 성분 감쇠
- `divingVerticalFactor = -sin(rotation) * signum(moveForward)` — 위를 보면 앞으로 이동 시 올라감

**3단계: 최종 모션 합산**
- `diffMotionX = diffMotionXForward * divingHorizontalFactor + diffMotionXStrafing`
- `diffMotionY = sqrt(diffMotionXForward² + diffMotionZForward²) * divingVerticalFactor + moveUpward`
- `diffMotionZ = diffMotionZForward * divingHorizontalFactor + diffMotionZStrafing`

**4단계: 속도 팩터 적용**

```java
float total = MathHelper.sqrt_float(MathHelper.sqrt_float(diffMotionX*diffMotionX + diffMotionZ*diffMotionZ) + diffMotionY*diffMotionY);
```

**주의:** `total`을 `sqrt(sqrt(x² + z²) + y²)`로 계산 — 일반적인 `sqrt(x² + y² + z²)`가 아님. 수평과 수직을 비대칭으로 취급하는 고의적 공식.

`total > 0.01F`일 때: `factor = speedFactor / total`, 각 motion에 `diffMotion * factor` 가산.

**`treeDimensional = false` 시:** `rotation = 0` → `divingHorizontalFactor = 1`, `divingVerticalFactor = 0`. 순수 수평 이동.

---

## `supportsCeilingClimbing` (95-121줄)

```java
protected Block supportsCeilingClimbing(int i, int j, int k)
{
    Block block = sp.worldObj.getBlock(i, j, k);
    if(block == null)
        return null;

    Dictionary<Object,Set<Integer>> configuration = Config._ceilingClimbConfigurationObject.value;
    Set<Integer> metaDatas = configuration.get(block);
    if(metaDatas == null)
    {
        String blockName = block.getUnlocalizedName();
        if (blockName != null && !blockName.isEmpty())
        {
            metaDatas = configuration.get(blockName);
            if(metaDatas == null && blockName.startsWith("tile.") && blockName.length() > 5)
                metaDatas = configuration.get(blockName.substring(5));
        }
    }

    if(metaDatas == null)
        return null;
    if(metaDatas.isEmpty())
        return block;
    if(metaDatas.contains(sp.worldObj.getBlockMetadata(i, j, k)))
        return block;
    return null;
}
```

- `Config._ceilingClimbConfigurationObject.value`: `Dictionary<Object, Set<Integer>>`. 키가 Block 인스턴스 또는 블록 이름 문자열.
- 블록 이름 조회 시 `"tile."` 접두사 제거 시도 (`"tile.cobblestone"` → `"cobblestone"`).
- `metaDatas.isEmpty()`: 메타데이터 제한 없음 → 모든 메타 허용.
- `metaDatas.contains(blockMeta)`: 특정 메타데이터만 허용.
- 반환값: 천장 클라이밍 가능한 Block 인스턴스 (null이면 불가).

---

## `isLava` (123-129줄)

```java
@SuppressWarnings("static-method")
protected boolean isLava(Block block)
{
    if(block == Block.getBlockFromName("lava") || block == Block.getBlockFromName("flowing_lava"))
        return true;
    return block != null && block.getMaterial() == Material.lava;
}
```

`"lava"` / `"flowing_lava"` 이름 체크 또는 `Material.lava` 체크.

---

## 액체 경계 계산 (131-182줄)

### `getLiquidBorder(int i, int j, int k)` → float

```java
protected float getLiquidBorder(int i, int j, int k)
{
    float finiteLiquidBorder;
    Block block = sp.worldObj.getBlock(i, j, k);
    if(block == Block.getBlockFromName("water") || block == Block.getBlockFromName("flowing_water"))
        return getNormalWaterBorder(i, j, k);
    if(SmartMovingOptions.hasFiniteLiquid && (finiteLiquidBorder = getFiniteLiquidWaterBorder(i, j, k, block)) > 0)
        return finiteLiquidBorder;
    if(block == Block.getBlockFromName("lava") || block == Block.getBlockFromName("flowing_lava"))
        return Config._lavaLikeWater.value ? getNormalWaterBorder(i, j, k) : 0F;

    Material material = sp.worldObj.getBlock(i, j, k).getMaterial();
    if(material == null || material == Material.lava)
        return Config._lavaLikeWater.value ? 1F : 0F;
    if(material == Material.water)
        return getNormalWaterBorder(i, j, k);
    if(material.isLiquid())
        return 1F;

    return 0F;
}
```

**반환값:** 해당 블록의 액체 높이 (0.0~1.0F). 0이면 비액체.

**판정 순서:**
1. `water` / `flowing_water` → `getNormalWaterBorder()`
2. FiniteLiquid 모드 → `getFiniteLiquidWaterBorder()`
3. `lava` / `flowing_lava` → `_lavaLikeWater ? getNormalWaterBorder() : 0F`
4. `Material.lava` → `_lavaLikeWater ? 1F : 0F`
5. `Material.water` → `getNormalWaterBorder()`
6. `material.isLiquid()` → `1F`
7. 그 외 → `0F`

---

### `getNormalWaterBorder(int i, int j, int k)` → float (153-164줄)

```java
protected float getNormalWaterBorder(int i, int j, int k)
{
    int blockMetaData = sp.worldObj.getBlockMetadata(i, j, k);
    if(blockMetaData >= 8)
        return 1F;
    if(blockMetaData == 0)
        if(sp.worldObj.isAirBlock(i, j + 1, k))
            return 0.8875F;
        else
            return 1F;
    return (8 - blockMetaData) / 8F;
}
```

| 메타데이터 | 반환값 | 조건 |
|-----------|--------|------|
| `>= 8` | `1F` | 흐르는 물 (낮은 레벨) |
| `== 0` + 위가 공기 | `0.8875F` | 수면 (아래가 가득, 위가 공기) |
| `== 0` + 위가 비공기 | `1F` | 수면 아래 (위에 물 있음) |
| `1~7` | `(8 - meta) / 8F` | 흐르는 물 레벨 |

---

### `getFiniteLiquidWaterBorder(int i, int j, int k, Block block)` → float (166-182줄)

```java
protected float getFiniteLiquidWaterBorder(int i, int j, int k, Block block)
{
    int type;
    if((type = Orientation.getFiniteLiquidWater(block)) > 0)
    {
        if(type == 2)
            return 1F;
        if(type == 1)
        {
            Block aboveBlock = sp.worldObj.getBlock(i, j + 1, k);
            if(Orientation.getFiniteLiquidWater(aboveBlock) > 0)
                return 1F;
            return (sp.worldObj.getBlockMetadata(i, j, k) + 1) / 16F;
        }
    }
    return 0F;
}
```

- `Orientation.getFiniteLiquidWater(block)`: 0(비액체), 1(일반 물), 2(무한 물)
- `type == 2` → `1F`
- `type == 1`: 위 블록도 물이면 `1F`, 아니면 `(meta + 1) / 16F`

---

## 사다리/넝쿨 감지 (184-312줄)

### 공개 편의 메서드 (184-207줄)

```java
public boolean isFacedToLadder(boolean isSmall)   { return getOnLadder(1, true, isSmall) > 0; }
public boolean isFacedToSolidVine(boolean isSmall) { return getOnVine(1, true, isSmall) > 0; }
public boolean isOnLadderOrVine(boolean isSmall)   { return getOnLadderOrVine(1, false, isSmall) > 0; }
public boolean isOnVine(boolean isSmall)            { return getOnLadderOrVine(1, false, false, true, isSmall) > 0; }
public boolean isOnLadder(boolean isSmall)          { return getOnLadderOrVine(1, false, true, false, isSmall) > 0; }

protected int getOnLadder(int maxResult, boolean faceOnly, boolean isSmall)  { return getOnLadderOrVine(maxResult, faceOnly, true, false, isSmall); }
protected int getOnVine(int maxResult, boolean faceOnly, boolean isSmall)    { return getOnLadderOrVine(maxResult, faceOnly, false, true, isSmall); }
protected int getOnLadderOrVine(int maxResult, boolean faceOnly, boolean isSmall) { return getOnLadderOrVine(maxResult, faceOnly, true, true, isSmall); }
```

---

### `getOnLadderOrVine(int maxResult, boolean faceOnly, boolean ladder, boolean vine, boolean isSmall)` (224-312줄)

```java
protected int getOnLadderOrVine(int maxResult, boolean faceOnly, boolean ladder, boolean vine, boolean isSmall)
{
    int i = MathHelper.floor_double(sp.posX);
    int minj = MathHelper.floor_double(sp.boundingBox.minY);
    int k = MathHelper.floor_double(sp.posZ);

    if(Config.isStandardBaseClimb())
    {
        Block block = sp.worldObj.getBlock(i, minj, k);
        if(ladder)
            if(vine)
                return Orientation.isClimbable(sp.worldObj, i, minj, k) ? 1 : 0;
            else
                return block != Block.getBlockFromName("vine") && Orientation.isClimbable(sp.worldObj, i, minj, k) ? 1 : 0;
        else
            if(vine)
                return block == Block.getBlockFromName("vine") && Orientation.isClimbable(sp.worldObj, i, minj, k) ? 1 : 0;
            else
                return 0;
    }
    else
    {
        if(isSmall)
            minj--;

        HashSet<Orientation> facedOnlyTo = null;
        if(faceOnly)
            facedOnlyTo = Orientation.getClimbingOrientations(sp, true, false);

        int result = 0;
        int maxj = MathHelper.floor_double(sp.boundingBox.minY + Math.ceil(sp.boundingBox.maxY - sp.boundingBox.minY)) - 1;
        for(int j = minj; j <= maxj; j++)
        {
            Block block = sp.worldObj.getBlock(i, j, k);
            if(ladder)
            {
                boolean localLadder = Orientation.isKnownLadder(block);
                Orientation localLadderOrientation = null;
                if(localLadder)
                {
                    localLadderOrientation = Orientation.getKnownLadderOrientation(sp.worldObj, i, j, k);
                    if(facedOnlyTo == null || facedOnlyTo.contains(localLadderOrientation))
                        result++;
                }

                for(Orientation direction : facedOnlyTo != null ? facedOnlyTo : Orientation.Orthogonals)
                {
                    if(result >= maxResult) return result;
                    if(direction != localLadderOrientation)
                    {
                        Block remoteBlock = sp.worldObj.getBlock(i + direction._i, j, k + direction._k);
                        if(Orientation.isKnownLadder(remoteBlock))
                        {
                            Orientation remoteLadderOrientation = Orientation.getKnownLadderOrientation(sp.worldObj, i + direction._i, j, k + direction._k);
                            if(remoteLadderOrientation.rotate(180) == direction)
                                result++;
                        }
                    }
                }
            }

            if(result >= maxResult) return result;

            if(vine && Orientation.isVine(block))
                if(facedOnlyTo == null)
                    result++;
                else
                {
                    Iterator<Orientation> iterator = facedOnlyTo.iterator();
                    while(iterator.hasNext())
                    {
                        Orientation climbOrientation = iterator.next();
                        if(climbOrientation.hasVineOrientation(sp.worldObj, i, j, k) && climbOrientation.isRemoteSolid(sp.worldObj, i, j, k))
                        {
                            result++;
                            break;
                        }
                    }
                }

            if(result >= maxResult) return result;
        }
        return result;
    }
}
```

**Standard Base Climb 경로:** `minj`에서 블록 하나만 확인.

**그 외 경로:**
- `isSmall`: `minj--` (크롤링/슬라이딩 등 낮은 자세에서 한 칸 아래까지 탐색)
- `faceOnly`: `facedOnlyTo = Orientation.getClimbingOrientations(sp, true, false)` (플레이어가 바라보는 방향만)
- `maxj = floor(minY + ceil(maxY - minY)) - 1` = 플레이어 높이 내 최상단 Y 블록
- 사다리 탐색: 플레이어 위치 블록 + 인접 4방향의 원격 사다리 (`remoteLadderOrientation.rotate(180) == direction` — 사다리 방향이 플레이어 쪽을 향해야 함)
- 넝쿨 탐색: `hasVineOrientation && isRemoteSolid` — 넝쿨이 해당 방향에 붙어있고 그 방향에 고체 블록이 있어야 함

---

## 클라이밍 블로킹 감지 (314-363줄)

세 메서드 모두 동일한 전제 조건:

```java
sp.isCollidedHorizontally && sp.isCollidedVertically && !sp.onGround && esp.movementInput.moveForward > 0F
```

`Orientation.getOrientation(sp, 20F, true, false)` — 20도 허용 오차로 플레이어가 바라보는 방향.  
확인 위치: `floor(sp.posX), floor(sp.boundingBox.maxY), floor(sp.posZ)` (머리 위 블록).

### `climbingUpIsBlockedByLadder()` (314-329줄)

```java
if(Orientation.isLadder(sp.worldObj.getBlock(i, j, k)))
    return Orientation.getKnownLadderOrientation(sp.worldObj, i, j, k) == orientation;
```

---

### `climbingUpIsBlockedByTrapDoor()` (331-346줄)

```java
if(Orientation.isTrapDoor(sp.worldObj.getBlock(i, j, k)))
    return Orientation.getOpenTrapDoorOrientation(sp.worldObj, i, j, k) == orientation;
```

---

### `climbingUpIsBlockedByCobbleStoneWall()` (348-363줄)

```java
if(sp.worldObj.getBlock(i, j, k) == Block.getBlockFromName("cobblestone_wall"))
    return !((BlockWall)Block.getBlockFromName("cobblestone_wall")).canConnectWallTo(sp.worldObj, i - orientation._i, j, k - orientation._k);
```

플레이어 방향 뒤쪽 (`i - orientation._i, k - orientation._k`)에 벽이 연결되지 않으면 막혀있다고 판정.

---

## 고체 경계 계산 (365-409줄)

### `getPlayerSolidBetween(double yMin, double yMax, double horizontalTolerance)` — private

```java
private List<?> getPlayerSolidBetween(double yMin, double yMax, double horizontalTolerance)
{
    double minY = sp.boundingBox.minY;
    double maxY = sp.boundingBox.maxY;
    sp.boundingBox.minY = yMin;
    sp.boundingBox.maxY = yMax;

    List<?> result = sp.worldObj.getCollidingBoundingBoxes(sp,
        horizontalTolerance == 0 ? sp.boundingBox : sp.boundingBox.contract(-horizontalTolerance, 0, -horizontalTolerance));

    sp.boundingBox.minY = minY;
    sp.boundingBox.maxY = maxY;

    return result;
}
```

임시로 boundingBox의 Y 범위를 변경하여 충돌 박스 목록을 쿼리한 뒤 복원.  
`horizontalTolerance != 0`: `contract(-tol, 0, -tol)` — 수평 축소 (엄격한 충돌만 포함).

---

### `isPlayerInSolidBetween(double yMin, double yMax)` (380-383줄)

```java
protected boolean isPlayerInSolidBetween(double yMin, double yMax)
{
    return getPlayerSolidBetween(yMin, yMax, 0).size() > 0;
}
```

---

### `getMaxPlayerSolidBetween(double yMin, double yMax, double horizontalTolerance)` (385-396줄)

```java
protected double getMaxPlayerSolidBetween(double yMin, double yMax, double horizontalTolerance)
{
    List<?> solids = getPlayerSolidBetween(yMin, yMax, horizontalTolerance);
    double result = yMin;
    for(int i = 0; i < solids.size(); i++)
    {
        AxisAlignedBB box = (AxisAlignedBB)solids.get(i);
        if(isCollided(box, yMin, yMax, horizontalTolerance))
            result = Math.max(result, box.maxY);
    }
    return Math.min(result, yMax);
}
```

`yMin`~`yMax` 구간에서 충돌하는 박스 중 최대 `maxY` 반환 (단, `yMax` 초과 없음). 즉, "발 아래 바닥 최상단 Y".

---

### `getMinPlayerSolidBetween(double yMin, double yMax, double horizontalTolerance)` (398-409줄)

```java
protected double getMinPlayerSolidBetween(double yMin, double yMax, double horizontalTolerance)
{
    List<?> solids = getPlayerSolidBetween(yMin, yMax, horizontalTolerance);
    double result = yMax;
    for(int i = 0; i < solids.size(); i++)
    {
        AxisAlignedBB box = (AxisAlignedBB)solids.get(i);
        if(isCollided(box, yMin, yMax, horizontalTolerance))
            result = Math.min(result, box.minY);
    }
    return Math.max(result, yMin);
}
```

`yMin`~`yMax` 구간에서 충돌하는 박스 중 최소 `minY` 반환 (단, `yMin` 미만 없음). 즉, "머리 위 천장 최하단 Y".

---

## 액체 경계 계산 (411-451줄)

### `isInLiquid()` (411-416줄)

```java
protected boolean isInLiquid()
{
    return
        getMaxPlayerLiquidBetween(sp.boundingBox.minY, sp.boundingBox.maxY) != sp.boundingBox.minY ||
        getMinPlayerLiquidBetween(sp.boundingBox.minY, sp.boundingBox.maxY) != sp.boundingBox.maxY;
}
```

`getMaxPlayerLiquidBetween != minY` (= 바운딩박스 범위 내에 액체 상한이 minY보다 높음) 또는  
`getMinPlayerLiquidBetween != maxY` (= 액체가 존재하는 최하단이 maxY보다 낮음).

---

### `getMaxPlayerLiquidBetween(double yMin, double yMax)` (418-432줄)

```java
protected double getMaxPlayerLiquidBetween(double yMin, double yMax)
{
    int i = MathHelper.floor_double(sp.posX);
    int jMin = MathHelper.floor_double(yMin);
    int jMax = MathHelper.floor_double(yMax);
    int k = MathHelper.floor_double(sp.posZ);

    for(int j = jMax; j >= jMin; j--)    // 위에서 아래로 탐색
    {
        float swimWaterBorder = getLiquidBorder(i, j, k);
        if(swimWaterBorder > 0)
            return j + swimWaterBorder;
    }
    return yMin;
}
```

플레이어 X/Z 위치에서 `yMax`→`yMin` 방향(위→아래)으로 탐색. 첫 번째 액체 블록 발견 시 `j + swimWaterBorder` 반환. 없으면 `yMin` 반환.

---

### `getMinPlayerLiquidBetween(double yMin, double yMax)` (434-451줄)

```java
protected double getMinPlayerLiquidBetween(double yMin, double yMax)
{
    int i = MathHelper.floor_double(sp.posX);
    int jMin = MathHelper.floor_double(yMin);
    int jMax = MathHelper.floor_double(yMax);
    int k = MathHelper.floor_double(sp.posZ);

    for(int j = jMin; j <= jMax; j++)    // 아래에서 위로 탐색
    {
        float swimWaterBorder = getLiquidBorder(i, j, k);
        if(swimWaterBorder > 0)
            if(j > yMin)
                return j;
            else if(j + swimWaterBorder > yMin)
                return yMin;
    }
    return yMax;
}
```

`yMin`→`yMax` 방향(아래→위)으로 탐색. 첫 번째 액체 발견 시:
- `j > yMin`: 블록이 `yMin`보다 위에 있음 → `j` 반환 (블록 하단)
- `j + swimWaterBorder > yMin`: 이 블록의 액체 상단이 `yMin`보다 높음 → `yMin` 반환
- 없으면 `yMax` 반환

---

## `isCollided` (453-462줄)

```java
public boolean isCollided(AxisAlignedBB box, double yMin, double yMax, double horizontalTolerance)
{
    return
        box.maxX >= sp.boundingBox.minX - horizontalTolerance &&
        box.minX <= sp.boundingBox.maxX + horizontalTolerance &&
        box.maxY >= yMin &&
        box.minY <= yMax &&
        box.maxZ >= sp.boundingBox.minZ - horizontalTolerance &&
        box.minZ <= sp.boundingBox.maxZ + horizontalTolerance;
}
```

플레이어 boundingBox와 지정된 AABB가 충돌하는지 수동 AABB 교차 판정. `horizontalTolerance`로 수평 허용 범위 조절.

---

## `isBlockTranslucent` (464-467줄)

```java
private boolean isBlockTranslucent(int i, int j, int k)
{
    return sp.worldObj.isBlockNormalCubeDefault(i, j, k, false);
}
```

`isBlockNormalCubeDefault(false)`: 일반 큐브 블록인지 확인. 이 메서드에서 `false`를 넘기면 "기본적으로 false"를 반환. 즉, 블록이 없거나 불투명 큐브가 아닌 경우 `false`.

---

## `pushOutOfBlocks` (469-522줄)

```java
public boolean pushOutOfBlocks(double d, double d1, double d2, boolean top)
{
    int i = MathHelper.floor_double(d);
    int j = MathHelper.floor_double(d1);
    int k = MathHelper.floor_double(d2);
    double d3 = d - i;  // X 소수 부분 (0~1)
    double d4 = d2 - k; // Z 소수 부분 (0~1)

    if(isBlockTranslucent(i, j, k) || (top && isBlockTranslucent(i, j + 1, k)))
    {
        boolean flag = !isBlockTranslucent(i - 1, j, k) && (!top || !isBlockTranslucent(i - 1, j + 1, k));  // X- 방향 고체
        boolean flag1 = !isBlockTranslucent(i + 1, j, k) && (!top || !isBlockTranslucent(i + 1, j + 1, k)); // X+ 방향 고체
        boolean flag2 = !isBlockTranslucent(i, j, k - 1) && (!top || !isBlockTranslucent(i, j + 1, k - 1)); // Z- 방향 고체
        boolean flag3 = !isBlockTranslucent(i, j, k + 1) && (!top || !isBlockTranslucent(i, j + 1, k + 1)); // Z+ 방향 고체

        byte byte0 = -1;
        double d5 = 9999D;
        if(flag && d3 < d5)     { d5 = d3;       byte0 = 0; }  // X- 방향, 거리 = d3
        if(flag1 && 1.0D - d3 < d5) { d5 = 1.0D - d3; byte0 = 1; }  // X+
        if(flag2 && d4 < d5)    { d5 = d4;       byte0 = 4; }  // Z-
        if(flag3 && 1.0D - d4 < d5) {             byte0 = 5; }  // Z+

        float f = 0.1F;
        if(byte0 == 0) sp.motionX = -f;
        if(byte0 == 1) sp.motionX = f;
        if(byte0 == 4) sp.motionZ = -f;
        if(byte0 == 5) sp.motionZ = f;
    }
    return false;  // 항상 false 반환
}
```

플레이어가 고체 블록 안에 있을 때 가장 가까운 빈 방향으로 밀어냄.  
`top = true`: 현재 블록과 한 칸 위 블록 모두 확인 (키 큰 상태).  
적용 속도 `f = 0.1F`.  
항상 `false` 반환 (반환값 미사용).

---

## `isInsideOfMaterial` (524-543줄)

```java
public boolean isInsideOfMaterial(Material material)
{
    if(SmartMovingOptions.hasFiniteLiquid && material == Material.water)
    {
        double d = sp.posY + sp.getEyeHeight();
        int i = MathHelper.floor_double(sp.posX);
        int j = MathHelper.floor_float(MathHelper.floor_double(d));
        int k = MathHelper.floor_double(sp.posZ);
        Block l = sp.worldObj.getBlock(i, j, k);
        float border;
        if(l != null && (border = getFiniteLiquidWaterBorder(i, j, k, l)) > 0)
        {
            float f = (1 - border) - 0.1111111F;
            float f1 = (j + 1) - f;
            return d < f1;
        }
        return false;
    }
    return isp.localIsInsideOfMaterial(material);
}
```

FiniteLiquid 모드 + water: 눈 높이(`posY + eyeHeight`)가 액체 상단 이하인지 확인.  
`f = (1 - border) - 0.1111111F` (1/9 ≈ 0.1111), `f1 = (j + 1) - f` → 액체 안 기준 임계값.  
그 외: `isp.localIsInsideOfMaterial(material)` 위임.

---

## `calculateSeparateCollisions` (545-788줄)

vanilla `Entity.moveEntity()`의 충돌 계산 로직을 복제하여 X/Y/Z 각 방향 충돌 여부를 **개별적으로** 감지.

```java
public int calculateSeparateCollisions(double par1, double par3, double par5)
```

**반환값:** `CollidedPositiveX | CollidedNegativeX | CollidedPositiveY | CollidedNegativeY | CollidedPositiveZ | CollidedNegativeZ` 비트 OR.

**상수:**
```java
public final static int CollidedPositiveX = 1;
public final static int CollidedNegativeX = 2;
public final static int CollidedPositiveY = 4;
public final static int CollidedNegativeY = 8;
public final static int CollidedPositiveZ = 16;
public final static int CollidedNegativeZ = 32;
```

**초기 상태 저장:** `ySize * 0.4F`, `boundingBox.copy()`, `onGround`, 웹 여부 등.

**웹 감속:** `par1 *= 0.25D`, `par3 *= 0.05`, `par5 *= 0.25D`.

**스닉 엣지 감지 (flag = onGround && isSneaking()):**
- X 방향: `getCollidingBoundingBoxes(boundingBox.getOffsetBoundingBox(par1, -1D, 0))` 비어있으면 par1을 0.05씩 줄임
- Z 방향: 동일
- 대각(XZ): 동일

**충돌 계산 순서:** Y → X → Z (vanilla와 동일).  
`field_9293_aM` (= `sp.field_70135_K`): noClip 플래그. true이면 충돌 후 par 클리어 없음.

**스텝 높이 처리:** `stepHeight > 0.0F && flag1 && (flag || ySize < 0.05F) && (d2 != par1 || d4 != par5)` 조건 시 stepHeight만큼 Y 상승 시도.  
이동 거리 비교: `d6² + d10² >= par1² + par5²` → 스텝 전이 더 멀면 스텝 없이 원래 값 복원.

**충돌 방향 판정:**
```java
boolean isCollidedPositiveX = d2 > par1;   // 원본 X가 더 큼 → +X 방향 벽에 충돌
boolean isCollidedNegativeX = d2 < par1;   // 원본 X가 더 작음 → -X 방향 벽에 충돌
// Y, Z 동일 패턴
```

---

## `isSneaking` (797-800줄)

```java
public boolean isSneaking()
{
    return sp.isSneaking();
}
```

기본 구현. `SmartMovingSelf`에서 오버라이드됨.

---

## `correctOnUpdate` (802-843줄)

```java
public void correctOnUpdate(boolean isSmall, boolean reverseMaterialAcceleration)
{
    double d = sp.posX - sp.prevPosX;
    double d1 = sp.posZ - sp.prevPosZ;
    float f = MathHelper.sqrt_double(d * d + d1 * d1);
    if(f < 0.05F && f > 0.02 && isSmall)
    {
        float f1 = sp.renderYawOffset;
        f1 = ((float)Math.atan2(d1, d) * 180F) / 3.141593F - 90F;

        if(sp.swingProgress > 0.0F)
            f1 = sp.rotationYaw;

        float f4;
        for(f4 = f1 - sp.renderYawOffset; f4 < -180F; f4 += 360F) {}
        for(; f4 >= 180F; f4 -= 360F) {}
        float x = sp.renderYawOffset + f4 * 0.3F;
        float f5;
        for(f5 = sp.rotationYaw - x; f5 < -180F; f5 += 360F) {}
        for(; f5 >= 180F; f5 -= 360F) {}
        if(f5 < -75F) f5 = -75F;
        if(f5 >= 75F) f5 = 75F;
        sp.renderYawOffset = sp.rotationYaw - f5;
        if(f5 * f5 > 2500F)
            sp.renderYawOffset += f5 * 0.2F;
        for(; sp.renderYawOffset - sp.prevRenderYawOffset < -180F; sp.prevRenderYawOffset -= 360F) {}
        for(; sp.renderYawOffset - sp.prevRenderYawOffset >= 180F; sp.prevRenderYawOffset += 360F) {}
    }

    if(reverseMaterialAcceleration)
        reverseHandleMaterialAcceleration();
}
```

**목적:** 수영/크롤링 등 낮은 자세로 느리게 이동할 때 `renderYawOffset` 회전 보정.

**조건:** `0.02 < f < 0.05` (느린 이동) && `isSmall` (수영/다이빙/크롤링 등)  

**회전 계산:**
- `f1 = atan2(dz, dx) * 180/π - 90` = 이동 방향 각도
- 진동 중이면 `f1 = sp.rotationYaw`
- `renderYawOffset`를 `rotationYaw` 기준 ±75도 클램프
- `f5² > 2500` (75도 이상) → `renderYawOffset += f5 * 0.2F`

---

## 갭 계산 (845-882줄)

```java
protected double getGapUnderneight()
{
    return sp.boundingBox.minY - getMaxPlayerSolidBetween(sp.boundingBox.minY - 1.1D, sp.boundingBox.minY, 0);
}

protected double getGapOverneight()
{
    return getMinPlayerSolidBetween(sp.boundingBox.maxY, sp.boundingBox.maxY + 1.1D, 0) - sp.boundingBox.maxY;
}
```

- `getGapUnderneight()`: `boundingBox.minY`에서 1.1D 아래 범위의 고체 최상단까지의 거리.
- `getGapOverneight()`: `boundingBox.maxY`에서 1.1D 위 범위의 고체 최하단까지의 거리.

---

## `getOverGroundHeight` / `getOverGroundBlockId` (855-882줄)

```java
public double getOverGroundHeight(double maximum)
{
    if(esp != null)
        return (sp.boundingBox.minY - getMaxPlayerSolidBetween(sp.boundingBox.minY - maximum, sp.boundingBox.minY, 0));
    return (sp.boundingBox.minY + 1D - getMaxPlayerSolidBetween(sp.boundingBox.minY - maximum + 1D, sp.boundingBox.minY + 1D, 0.1));
}
```

`esp != null` (클라이언트): `minY - maxSolid(minY-maximum, minY)`. 서버: Y+1 오프셋, `horizontalTolerance = 0.1`.

```java
public Block getOverGroundBlockId(double distance)
{
    int x = MathHelper.floor_double(sp.posX);
    int y = MathHelper.floor_double(sp.boundingBox.minY);
    int z = MathHelper.floor_double(sp.posZ);
    int minY = y - (int)Math.ceil(distance);

    if(esp == null) { y++; minY++; }  // 서버: +1 오프셋

    for(; y >= minY; y--)
    {
        Block block = sp.worldObj.getBlock(x, y, z);
        if(block != null)
            return block;
    }
    return null;
}
```

발 아래 `distance` 블록 내에서 첫 번째 비-null 블록 반환.

---

## `reverseHandleMaterialAcceleration` (884-932줄)

```java
public void reverseHandleMaterialAcceleration()
{
    AxisAlignedBB axisalignedbb = sp.boundingBox.expand(0.0D, -0.40000000596046448D, 0.0D).contract(0.001D, 0.001D, 0.001D);
    Material material = Material.water;
    Entity entity = sp;

    int i = MathHelper.floor_double(axisalignedbb.minX);
    int j = MathHelper.floor_double(axisalignedbb.maxX + 1.0D);
    int k = MathHelper.floor_double(axisalignedbb.minY);
    int l = MathHelper.floor_double(axisalignedbb.maxY + 1.0D);
    int i1 = MathHelper.floor_double(axisalignedbb.minZ);
    int j1 = MathHelper.floor_double(axisalignedbb.maxZ + 1.0D);

    if(!entity.worldObj.checkChunksExist(i, k, i1, j, l, j1)) return;

    Vec3 vec3d = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
    for(int k1 = i; k1 < j; k1++)
        for(int l1 = k; l1 < l; l1++)
            for(int i2 = i1; i2 < j1; i2++)
            {
                Block block = entity.worldObj.getBlock(k1, l1, i2);
                if(block == null || block.getMaterial() != material) continue;
                double d1 = (l1 + 1) - BlockLiquid.getLiquidHeightPercent(entity.worldObj.getBlockMetadata(k1, l1, i2));
                if(l >= d1)
                    block.velocityToAddToEntity(entity.worldObj, k1, l1, i2, entity, vec3d);
            }

    if(vec3d.lengthVector() > 0.0D)
    {
        vec3d = vec3d.normalize();
        double d = -0.014D;  // 원래 vanilla는 +0.014D, 여기서는 반전
        entity.motionX += vec3d.xCoord * d;
        entity.motionY += vec3d.yCoord * d;
        entity.motionZ += vec3d.zCoord * d;
    }
}
```

**목적:** 수영 중 vanilla의 물 흐름 가속 (`+0.014D`)을 역방향으로 상쇄 (`-0.014D`).

**바운딩박스:** `expand(0, -0.4D, 0).contract(0.001D, 0.001D, 0.001D)` — Y 방향 0.4D 축소, 수평 0.001D 수축.  
`Material.water` 전용.  
조건: `l >= d1` (액체 상단이 바운딩박스 Y 범위 내에 있음).

---

## import

```java
import java.util.*;
import net.minecraft.block.*;
import net.minecraft.block.material.*;
import net.minecraft.client.*;
import net.minecraft.client.entity.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.smart.moving.config.*;
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingContext` | 상위 클래스 (상수 `RadiantToAngle`, `ClimbPullMotion` 등) |
| `EntityPlayer` (`sp`) | 플레이어 상태 직접 접근 |
| `EntityPlayerSP` (`esp`) | 클라이언트 플레이어. null이면 서버 |
| `IEntityPlayerSP` (`isp`) | PlayerAPI 인터페이스 |
| `SmartMovingConfig` (`Config`) | 설정값 |
| `SmartMovingOptions` (`Options`) | 옵션값 |
| `Orientation` | 방향/클라이밍 관련 정적 유틸 |
| `BlockLiquid` | `getLiquidHeightPercent()` |

---

## 주요 관찰 사항

1. **`moveFlying` total 계산 버그/의도:** `sqrt(sqrt(x² + z²) + y²)` — 유클리드 3D 거리가 아닌 비대칭 공식. 수평 거리의 루트 값에 Y를 더한 후 다시 루트. 의도적으로 수직 이동의 영향을 줄이는 설계로 보임.

2. **`esp == null` 분기:** `getOverGroundHeight`, `getOverGroundBlockId`, `getPlayerSolidBetween` 등에서 클라이언트(`esp != null`)와 서버(`esp == null`)가 다른 Y 오프셋을 사용. 서버는 Y+1 기준.

3. **`calculateSeparateCollisions`:** vanilla `Entity.moveEntity()` 코드를 거의 그대로 복제하여 각 축 충돌 여부를 비트 플래그로 반환. 실제 이동은 적용하지 않고 충돌 결과만 계산.

4. **`reverseHandleMaterialAcceleration`:** vanilla `handleMaterialAcceleration`의 `+0.014D`를 `-0.014D`로 역전. 수영 중 물 흐름의 가속 효과를 상쇄하기 위한 역보정.

5. **`getOnLadderOrVine` remote 사다리:** 인접 4방향에 있는 사다리가 플레이어 쪽을 향하는지 확인 (`rotate(180) == direction`). 사다리가 벽에 붙어 있을 때 플레이어와의 상대 방향 검사.

6. **1.21.1 이식 고려 사항:**
   - `moveFlying` → 동일 로직 유지 가능 (수학 계산이므로)
   - `getPlayerSolidBetween`: `Entity.getCollidingBoundingBoxes()` → 1.21.1에서 `world.getBlockCollisions()` 또는 `ShapeContext` 기반 API로 대체
   - `calculateSeparateCollisions`: `Entity.move()` 내부 복제 — 1.21.1 버전의 해당 로직 확인 필요
   - `getLiquidBorder`: 1.21.1에서 `FluidState.getHeight()` API 사용 가능
   - `reverseHandleMaterialAcceleration`: `FluidState.getVelocity()` 관련 API 확인 필요
   - `Block.getBlockFromName("vine")` → `Blocks.VINE`
   - `MathHelper.floor_double` → `MathHelper.floor` (Yarn)
