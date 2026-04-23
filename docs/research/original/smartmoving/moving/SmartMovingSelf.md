# SmartMovingSelf.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMovingSelf.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: `SmartMovingSelf extends SmartMoving implements ISmartMovingSelf`  
어노테이션: `@SuppressWarnings("static-access")`  
크기: 3345줄, 103KB  
실행 위치: **클라이언트 전용** (EntityPlayerSP / EntityClientPlayerMP 에 바인딩)

---

## 역할

SmartMoving의 **클라이언트 측 이동 처리 핵심 클래스**.  
`moveEntityWithHeading`, `updateEntityActionState`, `handleJumping`, `tryJump` 등 플레이어의 모든 이동 로직을 담당한다.  
`SmartMoving` 추상 클래스를 상속하고, `ISmartMovingSelf` 인터페이스를 구현한다.

---

## 클래스 선언 및 생성자 (1-68줄)

```java
@SuppressWarnings("static-access")
public class SmartMovingSelf extends SmartMoving implements ISmartMovingSelf
{
    private boolean initialized;
    private int multiPlayerInitialized;
    private boolean isActive = true;
    private int updateCounter;
    private float distanceSwom;

    public SmartMovingSelf(EntityPlayer sp, IEntityPlayerSP isp)
    {
        super(sp, isp);

        initialized = false;

        nextClimbDistance = 0;
        distanceClimbedModified = 0;

        exhaustion = 0;
        lastHorizontalCollisionX = 0;
        lastHorizontalCollisionZ = 0;
        lastHungerIncrease = -2;

        prevPacketState = -1;
    }
}
```

**생성자 초기화 값:**
- `initialized = false`
- `nextClimbDistance = 0`, `distanceClimbedModified = 0`
- `exhaustion = 0`
- `lastHorizontalCollisionX/Z = 0`
- `lastHungerIncrease = -2`
- `prevPacketState = -1`

---

## 공개 필드 (선언 위치: 1413-1468, 1706, 1818, 1836-1840, 3056-3104줄)

### 이동 상태 boolean (1413-1448줄)

```java
// 클라이밍 의도
public boolean wantClimbUp;
public boolean wantClimbDown;
public boolean wantSprint;
public boolean wantCrawlNotClimb;
public boolean wantClimbCeiling;

// 이동 상태
public boolean isStanding;
public boolean wouldIsSneaking;
public boolean isVineOnlyClimbing;
public boolean isVineAnyClimbing;

// 클라이밍 세부
public boolean isClimbingStill;
public boolean isClimbHolding;
public boolean isNeighborClimbing;
public boolean hasClimbGap;
public boolean hasClimbCrawlGap;
public boolean hasNeighborClimbGap;
public boolean hasNeighborClimbCrawlGap;

public float dippingDepth;

// 점프 상태
public boolean isJumping;
public boolean isJumpingOutOfWater;
public boolean isShallowDiveOrSwim;
public boolean isFakeShallowWaterSneaking;
public boolean isStillSwimmingJump;
public boolean isGroundSprinting;
public boolean isSprintJump;
public boolean isAerodynamic;

// 클라이밍 블록 참조
public Block handsEdgeBlock;
public int handsEdgeMeta;
public Block feetEdgeBlock;
public int feetEdgeMeta;

public int waterMovementTicks;
```

### 피로/점프 상태 (1450-1468줄)

```java
public float exhaustion;
public float jumpCharge;
public float headJumpCharge;
public boolean blockJumpTillButtonRelease;

public float maxExhaustionForAction;
public float maxExhaustionToStartAction;

public float prevMaxExhaustionForAction = Float.NaN;
public float prevMaxExhaustionToStartAction = Float.NaN;

public float foreignExhaustionFactor;
public float foreignMaxExhaustionForAction = Float.MAX_VALUE;
public float foreignMaxExhaustionToStartAction = Float.MAX_VALUE;

public double lastHorizontalCollisionX;
public double lastHorizontalCollisionZ;
public float lastHungerIncrease;
```

### 기타 공개 필드

```java
boolean wasOnGround;                    // 1706줄 (package-private)
boolean wasCapabilitiesIsFlying;        // 1818줄

private float fadingPerspectiveFactor = -1;  // 1836줄
private boolean wasInventory;                // 1837줄

private double jumpMotionX;             // 1839줄
private double jumpMotionZ;             // 1840줄

// 이전 모션 (3056-3058줄)
public double prevMotionX;
public double prevMotionY;
public double prevMotionZ;

// 버튼 상태 (3060-3070줄)
public final Button forwardButton = new Button();
public final Button leftButton = new Button();
public final Button rightButton = new Button();
public final Button backButton = new Button();
public final Button jumpButton = new Button();
public final Button sneakButton = new Button();
public final Button grabButton = new Button();
public final Button sprintButton = new Button();
public final Button toggleButton = new Button();
public final Button speedIncreaseButton = new Button();
public final Button speedDecreaseButton = new Button();

// 이전 틱 상태 (3072-3078줄)
public boolean wasRunning;
public boolean wasLevitating;
public boolean wasCrawling;
public boolean wasHeadJumping;

// 내부 private 상태 (3077-3104줄)
private boolean contextContinueCrawl;
private boolean ignoreNextStopSneakButtonPressed;
private int collidedHorizontallyTickCount;
private boolean wantWallJumping;
private boolean continueWallJumping;
private boolean wasCollidedHorizontally;
private boolean wasRunningWhenSprintStarted;
private boolean jumpAvoided;
private boolean jumpPending;
private int climbIntoCount;
private int leftJumpCount;
private int rightJumpCount;
private int backJumpCount;
private int wallJumpCount;
private int nextClimbDistance;
public float distanceClimbedModified;
private boolean sneakToggled = false;
private boolean crawlToggled = false;
private int lastWorldPlayerEntitiesSize = -1;
private int lastWorldPlayerLastEnttyId = -1;
private long prevPacketState;
private Boolean forceIsSneaking;

// static (1407-1411줄)
private static ClimbGap out_handsClimbGap = new ClimbGap();
private static ClimbGap out_feetClimbGap = new ClimbGap();
private static HandsClimbing[] inout_handsClimbing = new HandsClimbing[1];
private static FeetClimbing[] inout_feetClimbing = new FeetClimbing[1];

// Ropes+ 호환 (3343-3344줄)
private static Class<?> ropesPlusClient = Reflect.LoadClass(SmartMovingInstall.class, SmartMovingInstall.RopesPlusClient, false);
private static Field onZipLine = ropesPlusClient != null ? Reflect.GetField(ropesPlusClient, SmartMovingInstall.RopesPlusClient_onZipLine, false) : null;
```

---

## 이동 진입점: `moveEntityWithHeading` (70-93줄)

```java
public void moveEntityWithHeading(float moveStrafing, float moveForward)
{
    if(!vanilla())
    {
        // trick to avoid cutoff (vanilla cuts off motionX at 0.005, but not prevMotionX)
        if(sp.motionX == 0 && prevMotionX < 0.005)
            sp.motionX = prevMotionX;
        if(sp.motionZ == 0 && prevMotionZ < 0.005)
            sp.motionZ = prevMotionZ;
    }
    if(sp.capabilities.isFlying && !Config.isFlyingEnabled())
    {
        double d3 = sp.motionY;
        float f2 = sp.jumpMovementFactor;
        sp.jumpMovementFactor = 0.05F;
        superMoveEntityWithHeading(moveStrafing, moveForward);
        sp.motionY = d3 * 0.59999999999999998D;
        sp.jumpMovementFactor = f2;
    }
    else
    {
        superMoveEntityWithHeading(moveStrafing, moveForward);
    }
}
```

- `vanilla()` = false일 때: motionX/Z가 0이고 prevMotion이 0.005 미만이면 prevMotion으로 복원 (vanilla의 0.005 컷오프 우회)
- `isFlying && !Config.isFlyingEnabled()`: `jumpMovementFactor = 0.05F`로 임시 설정 후 처리, motionY에 `* 0.59999999999999998D` 적용

---

## `superMoveEntityWithHeading` (95-147줄)

```java
private void superMoveEntityWithHeading(float moveStrafing, float moveForward)
{
    if(isRunning() && !Config.isRunningEnabled())
        sp.setSprinting(false);

    boolean wasShortInWater = isSwimming || isDiving;
    boolean wasSwimming = isSwimming;
    boolean wasClimbing = isClimbing;
    boolean wasDiving = isDiving;
    boolean wasCeilingClimbing = isCeilingClimbing;
    boolean wasJumpingOutOfWater = isJumpingOutOfWater;

    handleJumping();

    // ... 위치 스냅샷 ...
    double d_S = sp.posX;
    double d1_S = sp.posY;
    double d2_S = sp.posZ;

    if(sp.isCollidedHorizontally)
    {
        lastHorizontalCollisionX = sp.posX;
        lastHorizontalCollisionZ = sp.posZ;
    }

    float speedFactor = getConfigSpeedFactor() * getPotionSpeedFactor() * getNonSlowInputSpeedFactor(moveForward, moveStrafing);
    float slowInputSpeedFactor = this.getSlowInputSpeedFactor(moveForward, moveStrafing);
    if(vanilla())
    {
        moveForward *= slowInputSpeedFactor;
        moveStrafing *= slowInputSpeedFactor;
    }
    else
    {
        speedFactor *= slowInputSpeedFactor;
    }

    boolean isLiquidClimbing = Config.isFreeClimbingEnabled() && sp.fallDistance <= 3.0 && wantClimbUp && sp.isCollidedHorizontally && !isDiving;
    boolean handledSwimming = handleSwimming(...);
    boolean handledLava = handleLava(...);
    boolean handledAlternativeFlying = handleAlternativeFlying(...);
    handleLand(...);

    handleWallJumping();

    double diffX = sp.posX - d_S;
    double diffY = sp.posY - d1_S;
    double diffZ = sp.posZ - d2_S;

    sp.addMovementStat(diffX, diffY, diffZ);
    handleExhaustion(diffX, diffY, diffZ);
}
```

**isLiquidClimbing 조건:** `Config.isFreeClimbingEnabled() && sp.fallDistance <= 3.0 && wantClimbUp && sp.isCollidedHorizontally && !isDiving`

**처리 순서:** `handleJumping()` → `handleSwimming()` → `handleLava()` → `handleAlternativeFlying()` → `handleLand()` → `handleWallJumping()` → `addMovementStat()` → `handleExhaustion()`

---

## 속도 팩터 메서드 (149-227줄)

### `getCombinedSpeedFactor()`

```java
private float getCombinedSpeedFactor()
{
    return getConfigSpeedFactor() * getPotionSpeedFactor();
}
```

### `getConfigSpeedFactor()`

```java
private float getConfigSpeedFactor()
{
    return Config.enabled ? Config._speedFactor.value * Config.getUserSpeedFactor() : 1F;
}
```

### `getPotionSpeedFactor()`

```java
private float getPotionSpeedFactor()
{
    return Config.enabled ? getLandMovementFactor() * 10F / (sp.isSprinting() ? 1.3F : 1F) : 1F;
}
```

- `getLandMovementFactor() * 10F`: 기본 이동 속도(0.1)를 10배 = 1F 기준으로 정규화
- 스프린트 중이면 `/ 1.3F`로 보정

### `getSlowInputSpeedFactor(float moveForward, float moveStrafing)` (164-195줄)

```java
private float getSlowInputSpeedFactor(float moveForward, float moveStrafing)
{
    float speedFactor = 1f;

    if (sp.isUsingItem())
    {
        float itemFactor = 0.2F;  // 기본값
        if(Config.enabled)
        {
            Item item = sp.getItemInUse().getItem();
            if(item instanceof ItemSword)
                itemFactor = Config._usageSwordSpeedFactor.value;
            else if(item instanceof ItemBow)
                itemFactor = Config._usageBowSpeedFactor.value;
            else if(item instanceof ItemFood)
                itemFactor = Config._usageFoodSpeedFactor.value;
            else
                itemFactor = Config._usageSpeedFactor.value;
        }
        speedFactor *= itemFactor;
    }

    if(isCrawling || (isCrawlClimbing && !isClimbCrawling))
        speedFactor *= Config._crawlFactor.value;
    else if(isSlow)
        speedFactor *= Config._sneakFactor.value;

    if(isCeilingClimbing)
        speedFactor *= Config._ceilingClimbingSpeedFactor.value;

    return speedFactor;
}
```

아이템 사용 중 기본 0.2F. 크롤링 또는 스닉 또는 천장클라이밍 시 추가 팩터 적용.

### `getNonSlowInputSpeedFactor(float moveForward, float moveStrafing)` (197-227줄)

```java
private float getNonSlowInputSpeedFactor(float moveForward, float moveStrafing)
{
    float speedFactor = 1f;

    if(isFast)
        speedFactor *= (!isLevitating() ? Config._sprintFactor.value : Config._sprintFactorLevitate.value);
    if(isClimbing)
        if(moveStrafing != 0F || moveForward != 0F)
            speedFactor *= Config._freeClimbingHorizontalSpeedFactor.value;
        else if(wantClimbDown && isNeighborClimbing && !(Math.abs(sp.posX - lastHorizontalCollisionX) < 0.05 && Math.abs(sp.posZ - lastHorizontalCollisionZ) < 0.05))
        {
            moveForward = ClimbPullMotion;
            if(isVineOnlyClimbing)
            {
                if(handsEdgeMeta != Orientation.VineFrontMeta && feetEdgeMeta != Orientation.VineFrontMeta)
                    moveForward = 0F;
                else
                {
                    Orientation orientation = Orientation.getOrientation(sp, 45F, true, false);
                    if(orientation != null)
                    {
                        float gap = (float)orientation.getHorizontalBorderGap(sp);
                        float minGap = sp.width / 2;
                        float factor = Math.max(0, gap * (1 + minGap) - minGap);
                        moveForward = factor * factor * 0.3F;
                    }
                }
            }
        }
    return speedFactor;
}
```

클라이밍 down 풀 모션: `moveForward = ClimbPullMotion` (상위클래스 상수). 바인 전용 클라이밍 시 방향/거리 계산하여 `factor * factor * 0.3F`.

---

## `handleSwimming` (229-576줄)

### 주요 상수 (offset 기반 수영/다이빙 상태 판정)

`offset = playerSwimWaterBorder + 0.1625D` (정밀 조정값)

**수영 상태 분기 (offset 기준):**

| offset 범위 | 상태 | motionYDiff |
|------------|------|-------------|
| `< 1.4` | dipping | `< 1` → `-0.02D`, 그 외 → `-0.01D` |
| `1.4 ~ 1.9` | swimming | (아래 세부 표 참조) |
| `>= 1.9` | diving | (아래 세부 표 참조) |

**swimming motionYDiff (diveUp/moveSwim/wantShallowSwim일 때):**

| offset | motionYDiff |
|--------|-------------|
| `< 1.5` | `-0.02D` |
| `< 1.6` | `-0.01D` |
| `< 1.62` | `-0.005D` |
| `< 1.64` | `-0.0025D` |
| `< 1.66` | `-0.00125D` |
| `< 1.664` | `-0.000625D` |
| `< 1.668` | `0D` |
| `< 1.672` | `0.000625D` |
| `< 1.676` | `0.00125D` |
| `< 1.68` | `0.0025D` |
| `< 1.7` | `0.005D` |
| `< 1.8` | `0.01D` |
| `>= 1.8` | `0.02D` |

**diving motionYDiff (diveUp/diveDown/moveSwim일 때):**
- diveUp: `0.05D * (isFast ? Config._sprintFactor.value : 1F)`
- diveDown: `0.01 - 0.1 * speedFactor`
- moveSwim: `0.04D`, 그 외: `0.02D`

**diving 깊이 > 2일 때 motionYDiff:**
- diveUp + isFast + depth<2.5 + airBlock 위: `0.11D / Config._sprintFactor.value`
- diveUp: `0.01 + 0.1 * speedFactor`
- diveDown: `0.01 - 0.1 * speedFactor`
- 그 외: `0.01D`

### 수중 velocity 감쇠 계수

| 상태 | motionX/Z | motionY |
|------|-----------|---------|
| swimming | `* 0.85D` | `* 0.85D` |
| diving | `* 0.83D` | `* 0.83D` |
| dipping | `* 0.80D` | `* 0.83D` |
| 그 외 | `* 0.9D` | `* 0.85D` |

### 특수 조건들

- `diveUp` = `isp.getIsJumpingField()`
- `diveDown` = `esp.movementInput.sneak && Config._diveDownOnSneak.value`
- `swimDown` = `esp.movementInput.sneak && Config._swimDownOnSneak.value`
- `wantJumpOutOfWater` = `(moveForward != 0 || moveStrafing != 0) && sp.isCollidedHorizontally && diveUp && !isSlow`
- `isJumpingOutOfWater` = `wantJumpOutOfWater && (waterMovementTicks > 10 || sp.onGround || wasJumpingOutOfWater)`
- diveUp 적용 시 `sp.motionY -= 0.039999999105930328D` 먼저 적용
- `isJumpingOutOfWater`이면 `sp.motionY = 0.30000001192092896D`
- `isDiving || isSwimming`이면 `setHeightOffset(-1F)`
- `moveFlying(moveStrafing, moveForward, 0.02F * speedFactor)` 또는 `moveFlying((float)motionYDiff, moveStrafing, moveForward, 0.02F * speedFactor, Options._diveControlVertical.value)`

### useStandard 경로 (수영 실패 또는 라이딩 중)

```java
sp.moveFlying(moveStrafing, moveForward, 0.02F * speedFactor);
sp.moveEntity(sp.motionX, sp.motionY, sp.motionZ);
sp.motionX *= 0.80000001192092896D;
sp.motionY *= 0.80000001192092896D;
sp.motionZ *= 0.80000001192092896D;
sp.motionY -= 0.02D;
if(sp.isCollidedHorizontally && sp.isOffsetPositionInLiquid(...))
    sp.motionY = 0.30000001192092896D;
```

### SwimCrawl 경계 상수 (SmartMoving 상위클래스에서 정의, 이 파일에서 사용)

- `SwimCrawlWaterTopBorder`: crawl→swim 전환 판단
- `SwimCrawlWaterMaxBorder`: 크롤 수영 최대 경계
- `SwimCrawlWaterBottomBorder`: 크롤 수영 하단 경계
- `SwimCrawlWaterMediumBorder`: 중간 경계

---

## `handleLava` (578-600줄)

```java
private boolean handleLava(float moveForward, float moveStrafing, boolean handledSwimming, boolean isLiquidClimbing)
{
    boolean handleLava = !isFlying && !handledSwimming && !isLiquidClimbing && sp.handleLavaMovement();
    if(handleLava)
    {
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
        if(sp.isCollidedHorizontally && sp.isOffsetPositionInLiquid(sp.motionX, ((sp.motionY + 0.60000002384185791D) - sp.posY) + d1, sp.motionZ))
            sp.motionY = 0.30000001192092896D;
    }
    return handleLava;
}
```

용암 처리: `moveFlying` 인자 `0.02F`, 감쇠 `* 0.5D`, `motionY -= 0.02D`, 벽 충돌 시 `motionY = 0.30000001192092896D`.

---

## `handleAlternativeFlying` (602-631줄)

```java
private boolean handleAlternativeFlying(float moveForward, float moveStrafing, float speedFactor, boolean handledSwimming, boolean handledLava)
{
    boolean handleAlternativeFlying = !handledSwimming && !handledLava && sp.capabilities.isFlying && Config.isFlyingEnabled();
    if(handleAlternativeFlying)
    {
        resetSwimming();
        resetClimbing();

        float moveUpward = 0F;
        if(esp.movementInput.sneak)
        {
            sp.motionY += 0.14999999999999999D;
            moveUpward -= 0.98F;
        }
        if(esp.movementInput.jump)
        {
            sp.motionY -= 0.14999999999999999D;
            moveUpward += 0.98F;
        }

        moveFlying(moveUpward, moveStrafing, moveForward, speedFactor * 0.05F * Config._flyingSpeedFactor.value, Options._flyControlVertical.value);

        sp.moveEntity(sp.motionX, sp.motionY, sp.motionZ);

        sp.motionX *= HorizontalAirDamping;
        sp.motionY *= HorizontalAirDamping;
        sp.motionZ *= HorizontalAirDamping;
    }
    return handleAlternativeFlying;
}
```

스닉: `motionY += 0.15D`, `moveUpward -= 0.98F`. 점프: `motionY -= 0.15D`, `moveUpward += 0.98F`.  
flyingSpeed = `speedFactor * 0.05F * Config._flyingSpeedFactor.value`.  
감쇠: `* HorizontalAirDamping` (상위클래스 상수).

---

## `handleLand` (633-663줄)

```java
private void handleLand(...)
{
    if(!handledAlternativeFlying)
    {
        if(esp.movementInput.jump && Config.isSprintingEnabled() && sprintButton.Pressed && sp.capabilities.isFlying)
            sp.motionY += Config._sprintFactorLevitate.value * Config._sprintFactorLevitateVertical.value;
    }

    if(!handledSwimming && !handledLava && !handledAlternativeFlying)
    {
        resetSwimming();

        if (!grabButton.Pressed)
            fromSwimmingOrDiving(wasShortInWater);

        boolean isOnLadder = isOnLadder(isClimbCrawling);
        boolean isOnVine = isOnVine(isClimbCrawling);

        float horizontalDamping = landMotion(moveForward, moveStrafing, speedFactor, isOnLadder, isOnVine);
        boolean crawlingThroughWeb = (isCrawling || isCrawlClimbing) && isp.getIsInWebField();
        move(sp.motionX, sp.motionY, sp.motionZ, crawlingThroughWeb);

        handleClimbing(isOnLadder, isOnVine, wasClimbing);
        handleCeilingClimbing(wasCeilingClimbing);
        setLandMotions(horizontalDamping);
    }

    landMotionPost(wasShortInWater);
}
```

---

## `move` (665-673줄)

```java
private void move(double motionX, double motionY, double motionZ, boolean relocate)
{
    boolean isInWeb = isp.getIsInWebField();
    if(relocate)
        isp.setIsInWebField(false);
    sp.moveEntity(motionX, motionY, motionZ);
    if(relocate)
        isp.setIsInWebField(isInWeb);
}
```

`relocate = true` 시 거미줄 상태를 임시로 false로 설정한 뒤 moveEntity, 이후 복원.

---

## `landMotion` (675-812줄)

### 수평 감쇠 계산 (복잡)

**지면 위 (onGround && (!isJumping || vanilla())):**
- 아래 블록 미끄러움: `block.slipperiness * HorizontalAirDamping`
- 블록 없음: `HorizontalGroundDamping`
- 점프+스프린트+설정: `speedFactor *= Config._sprintJumpVerticalFactor.value`

**공중:** `HorizontalAirDamping`

### 이동 처리 (isOnLadder, isOnVine, isClimbing)

**ladder/vine 위에서 motionX/Z 클램프:**
```java
float f4 = 0.15F;
// motionX와 motionZ 모두 [-0.15F, 0.15F] 클램프
```

**ladder notTotalFreeClimbing 시:**
```java
sp.fallDistance = 0.0F;
sp.motionY = Math.max(sp.motionY, -0.15 * getCombinedSpeedFactor());
```

**Config.isFreeBaseClimb() 시:** `sneak && motionY < 0 && !onGround → motionY = 0`  
**Config.isFreeBaseClimb() 아닐 때:** `isp.localIsSneaking() && motionY < 0 → motionY = 0`

### 클라이밍 속도 설정 (isClimbing 분기)

```java
if(isClimbing && climbingUpIsBlockedByLadder())
    sp.moveFlying(0F, -1F, 0.07F);
else if(isClimbing && climbingUpIsBlockedByTrapDoor())
    sp.moveFlying(0F, -1F, 0.09F);   // ladder나 아니나 동일
else if(isClimbing && climbingUpIsBlockedByCobbleStoneWall())
    sp.moveFlying(0F, -1F, 0.07F);
```

### 일반 이동 (isSliding 아닐 때):

```java
float f3 = 0.1627714F / (horizontalDamping * horizontalDamping * horizontalDamping);
float rawSpeed = sp.onGround
    ? 0.1f * f3
    : sp.jumpMovementFactor / (sp.isSprinting() && !sp.capabilities.isFlying ? 1.3F : 1F);
```

`headJump` 시: `speedFactor *= Config._headJumpControlFactor.value`  
`Config.enabled && !onGround && !flying`: `speedFactor *= Config._jumpControlFactor.value`  
`!Config.enabled && onGround`: `speedFactor *= (getLandMovementFactor() * 10f) / (sp.isSprinting() ? 1.3F : 1F)`  
`isRunning() && !isFast`: `speedFactor *= !isFlying ? Config._runFactor.value : Config._runFactorLevitate.value`  
`!sp.onGround`: `speedFactor /= getPotionSpeedFactor()`

### 슬라이딩 수평 감쇠:

```java
horizontalDamping = 1F / (((1F / slipperiness) - 1F) / 25F * Config._slideSlipperinessFactor.value + 1F) * 0.98F;
```

슬라이딩 방향 제어: `Math.atan(sp.motionX / sp.motionZ)` 계산 후 `Config._slideControlDegrees.value / RadiantToAngle * Math.signum(moveStrafing)` 만큼 각도 조정.

### autoLadder (797-810줄):

```java
if(Config.isFreeClimbAutoLaddderEnabled() && moveForward > 0)
{
    int j = MathHelper.floor_double(sp.boundingBox.minY);
    double jGap = sp.boundingBox.minY - j;
    if(jGap < 0.1)
    {
        int i = MathHelper.floor_double(sp.posX);
        int k = MathHelper.floor_double(sp.posZ);
        if(Orientation.isLadder(sp.worldObj.getBlock(i, j - 1, k)))
            sp.motionY = Math.max(sp.motionY, 0.0);
    }
}
```

---

## `handleClimbing` (814-1110줄)

### Standard Base Climb (820-823줄):

```java
if(Config.isStandardBaseClimb() && sp.isCollidedHorizontally && isOnLadderOrVine)
    sp.motionY = 0.2 * getCombinedSpeedFactor();
```

### Simple Base Climb (825-843줄):

- feet && hands: `sp.motionY = FastUpMotion`
- feet only: `sp.motionY = FastUpMotion`
- hands only: `sp.motionY = SlowUpMotion`
- 없음: `sp.motionY = 0.0D`
- 모두 `* getCombinedSpeedFactor()`

### Smart Base Climb (856-894줄):

`handsSubstitute` (PZ/NZ/ZP/ZN 방향의 `isHandsLadderSubstitute`) 및 `feetSubstitute` (ZZ/PZ/NZ/ZP/ZN 방향의 `isFeetLadderSubstitute`) 판정 후:
- feet && hands: `FastUpMotion`
- feet + (handsSubstitute): `FastUpMotion`, 아니면 `SlowUpMotion`
- hands + (feetSubstitute): `FastUpMotion`, 아니면 `SlowUpMotion`

### Free Climbing (896-1108줄)

**exhaustion 판정:**
```java
boolean exhaustionAllowsClimbing =
    !Config.isClimbExhaustionEnabled() ||
    (
        exhaustion <= Config._climbExhaustionStop.value &&
        (wasClimbing || exhaustion <= Config._climbExhaustionStart.value)
    );
```

**ClimbGap 탐색 순서:**
1. PZ, NZ, ZP, ZN (4방향) → `inout_handsClimbing`, `inout_feetClimbing`, `out_handsClimbGap`, `out_feetClimbGap` 업데이트
2. `isNeighborClimbing`, `hasNeighborClimbGap`, `hasNeighborClimbCrawlGap` 설정
3. `isSmallClimbing` 아닐 때: PP, NP, NN, PN (대각 4방향) 추가 탐색
4. `hasClimbGap`, `hasClimbCrawlGap` 설정

**클라이밍 속도 결정 (`wantClimbUp` 분기):**

| 조건 | 속도 | 손/발 타입 |
|------|------|----------|
| `feetClimbing == FeetClimbing.FastUp` + 예외조건 | `FastUpMotion` | `HandsClimbing.NoGrab, FeetClimbing.DownStep` |
| `(hasClimbGap 또는 hasClimbCrawlGap) && handsClimbing == HandsClimbing.FastUp` + 조건 | `feetNone ? SlowUp : FastUp` | `MiddleGrab, DownStep` |
| `feetClimbing.IsRelevant() && handsClimbing.IsRelevant()` + 예외조건 | `MediumUpMotion` | `MiddleGrab 또는 UpGrab, DownStep` |
| `handsClimbing.IsUp()` | `SlowUpMotion` | (기본: UpGrab, DownStep) |
| `TopHold 또는 BaseHold 또는 SlowUpWithHold` | `HoldMotion` | (tryJump 시도 후) |
| `Sink 또는 SlowUpWithSink` | `SinkDownMotion` | — |

**`wantClimbDown` 분기:**

| 조건 | 속도 |
|------|------|
| `BottomHold && !feetIndependent` | `HoldMotion` |
| `feetClimbing == FastUp` | `ClimbDownMotion, NoGrab, DownStep` |
| `SlowUpWithHoldWithoutHands` | `ClimbDownMotion` |
| `TopWithHands` | `ClimbDownMotion` |
| `BaseWithHands 또는 BaseHold` + 조건 | `ClimbDownMotion` 또는 `SinkDownMotion` |
| 그 외 | `SinkDownMotion, UpGrab 또는 MiddleGrab, NoStep` |

**isClimbHolding 시 점프:**
```java
if(jumpButton.StartPressed)
{
    int type = (Options._climbJumpBackHeadOnGrab.value ? grabButton.Pressed : !grabButton.Pressed)
        ? (handsOnly ? Config.ClimbBackHead : Config.ClimbBackHeadHandsOnly)
        : (handsOnly ? Config.ClimbBackUp : Config.ClimbBackUpHandsOnly);
    float jumpAngle = sp.rotationYaw + 180F;
    if(tryJump(type, null, null, jumpAngle))
    {
        continueWallJumping = !isHeadJumping;
        isClimbing = false;
        sp.rotationYaw = jumpAngle;
        onStartClimbBackJump();
    }
}
```

**클라이밍 낙하 데미지:**
```java
if(isClimbing)
    handleCrash(Config._freeClimbFallDamageStartDistance.value, Config._freeClimbFallDamageFactor.value);
```

**Vine 판정:**
```java
isHandsVineClimbing = isClimbing && handsEdgeBlock == Block.getBlockFromName("vine");
isFeetVineClimbing = isClimbing && feetEdgeBlock == Block.getBlockFromName("vine");
isVineAnyClimbing = isHandsVineClimbing || isFeetVineClimbing;
isVineOnlyClimbing = isVineAnyClimbing &&
    !(handsEdgeBlock != null && handsEdgeBlock != Block.getBlockFromName("vine") ||
      feetEdgeBlock != null && feetEdgeBlock != Block.getBlockFromName("vine"));
```

**RedPower Wire 호환 (1098줄):**
```java
if(SmartMovingOptions.hasRedPowerWire && !isClimbing && wantClimbUp && wasClimbing)
    sp.motionY = 0.15;
```

---

## `handleCeilingClimbing` (1112-1174줄)

**exhaustion 판정:** `exhaustion <= Config._ceilingClimbExhaustionStop.value && (wasCeilingClimbing || exhaustion <= Config._ceilingClimbExhaustionStart.value)`

**조건:** `wantClimbCeiling && !isClimbing && (!isCrawling || climbCeilingCrawlingStartConflict) && !isCrawlClimbing`

**jgap 기준 motionY:**
```java
if(jgap > 1.2)
    sp.motionY = 0.12;
else if(jgap > 1.115)
    sp.motionY = 0.08;
else
    sp.motionY = 0.04;
sp.fallDistance = 0.0F;
```

추가 조건: `jgap < 1.9 && actuallySolidHeight < jd + 0.5` (`actuallySolidHeight = getMinPlayerSolidBetween(jd, jd + 0.6, 0.2)`)

---

## `setLandMotions` (1176-1182줄)

```java
private void setLandMotions(float horizontalDamping)
{
    sp.motionY -= 0.080000000000000002D;
    sp.motionY *= 0.98000001907348633D;
    sp.motionX *= horizontalDamping;
    sp.motionZ *= horizontalDamping;
}
```

지면 이동 후 중력 적용: `motionY -= 0.08D`, `* 0.98D`.

---

## `handleExhaustion` (1184-1310줄)

### 수평/수직 이동 거리 계산

```java
float horizontalMovement = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
float movement = MathHelper.sqrt_double(horizontalMovement * horizontalMovement + diffY * diffY);
int relevantMovementFactor = Math.round(movement * 100F);
```

### 허기 증가

```java
float hungerGainFactor = Config.getFactor(true, sp.onGround, isStanding, isStill, isSlow, isRunning, isFast, isClimbing, isClimbCrawling, isCeilingClimbing, isDipping, isSwimming, isDiving, isCrawling, isCrawlClimbing);
hungerIncrease += Config._alwaysHungerGain.value + relevantMovementFactor * 0.0001F * hungerGainFactor;
```

### 클라이밍 피로 계산

- 기본: `Config._baseExhautionGainFactor.value`
- 수직 정지: `* Config._climbStrafeExhaustionGain.value`
- 이동 중:
  - standing + wantUp: `* Config._climbUpExhaustionGain.value`
  - standing + wantDown: `* Config._climbDownExhaustionGain.value`
  - !standing + wantUp: `* Config._climbStrafeUpExhaustionGain.value`
  - !standing + wantDown: `* Config._climbStrafeDownExhaustionGain.value`
  - 그 외: `* 0F`

### 천장클라이밍 피로

`Config._baseExhautionGainFactor.value * Config._ceilingClimbExhaustionGain.value`

### 스프린트 피로

`additionalExhaustion * Config._sprintExhaustionGainFactor.value`

### 피로 감소

```java
float exhaustionLossFactor = Config.getFactor(false, ...);
float exhaustionLoss = 1F * exhaustionLossFactor;
exhaustion -= exhaustionLoss;
```

허기 감소 연동: `hungerIncrease += Config._exhaustionLossHungerFactor.value * exhaustionLoss`

### exhaustion == 0 리셋

```java
if(exhaustion == 0)
    maxExhaustionForAction = maxExhaustionToStartAction = Float.NaN;
```

### 수직 정지 판정

```java
boolean isVerticalStill = Math.abs(diffY) < 0.007;
```

### 허기 변화 패킷 전송

```java
if(hungerIncrease != lastHungerIncrease)
{
    SmartMovingPacketStream.sendHungerChange(SmartMovingComm.instance, hungerIncrease);
    lastHungerIncrease = hungerIncrease;
}
```

---

## ISmartMovingSelf 구현 메서드 (1312-1349줄)

```java
@Override
public float getExhaustion() { return exhaustion; }

@Override
public float getUpJumpCharge() { return jumpCharge; }

@Override
public float getHeadJumpCharge() { return headJumpCharge; }

@Override
public void addExhaustion(float factor)
{
    if(!Float.isNaN(factor) && factor > 0)
        foreignExhaustionFactor += factor;
}

@Override
public void setMaxExhaustionForAction(float maxExhaustionForAction)
{
    if(!Float.isNaN(maxExhaustionForAction) && maxExhaustionForAction >= 0)
        foreignMaxExhaustionForAction = Math.min(foreignMaxExhaustionForAction, maxExhaustionForAction);
}

@Override
public void setMaxExhaustionToStartAction(float maxExhaustionToStartAction)
{
    if(!Float.isNaN(maxExhaustionToStartAction) && maxExhaustionToStartAction >= 0)
        foreignMaxExhaustionToStartAction = Math.min(foreignMaxExhaustionToStartAction, maxExhaustionToStartAction);
}
```

---

## `landMotionPost` / `fromSwimmingOrDiving` (1351-1405줄)

```java
private void landMotionPost(boolean wasShortInWater)
{
    if (grabButton.Pressed)
        fromSwimmingOrDiving(wasShortInWater);

    if(heightOffset != 0 && isp.getSleepingField())
        resetInternalHeightOffset();  // 수영→수면 전환
}
```

```java
private void fromSwimmingOrDiving(boolean wasShortInWater)
{
    boolean isShortInWater = isSwimming || isDiving;
    if(wasShortInWater && !isShortInWater && !isp.getSleepingField())
    {
        setHeightOffset(-1F);
        // crawlStandUpBottom/Ceiling 계산...
        resetHeightOffset();

        if(crawlStandUpCeiling - crawlStandUpBottom < sp.height)
            // → 크롤링 (좁은 구멍)
        else if(crawlStandUpLiquidCeiling - crawlStandUpBottom < sp.height)
            // → 크롤링 (물 아래)
        else if(crawlStandUpBottom > sp.boundingBox.minY)
            // → 걷기/크롤링
    }
}
```

`getMinPlayerLiquidBetween(sp.boundingBox.maxY, sp.boundingBox.maxY + 1.1D)` 및 `getMinPlayerSolidBetween(sp.boundingBox.maxY, sp.boundingBox.maxY + 1.1D, 0)` 사용.

---

## 리셋 메서드 (1474-1498줄)

```java
private void resetClimbing()
{
    isClimbing = false;
    isHandsVineClimbing = false;
    isFeetVineClimbing = false;
    isVineOnlyClimbing = false;
    isVineAnyClimbing = false;
    isClimbingStill = false;
    isNeighborClimbing = false;
    actualHandsClimbType = HandsClimbing.NoGrab;
    actualFeetClimbType = FeetClimbing.NoStep;
    isCeilingClimbing = false;
}

private void resetSwimming()
{
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

---

## `setShouldClimbSpeed` / `setOnlyShouldClimbSpeed` (1500-1551줄)

```java
private void setShouldClimbSpeed(double value) { setShouldClimbSpeed(value, HandsClimbing.UpGrab, FeetClimbing.DownStep); }

private void setShouldClimbSpeed(double value, int handsClimbType, int feetClimbType)
{
    setOnlyShouldClimbSpeed(value);
    actualHandsClimbType = handsClimbType;
    actualFeetClimbType = feetClimbType;
}

@SuppressWarnings("incomplete-switch")
private void setOnlyShouldClimbSpeed(double value)
{
    isClimbing = true;

    if(this.climbIntoCount > 0)
        value = HoldMotion;

    if(value != HoldMotion)
    {
        float factor = getCombinedSpeedFactor();
        if(isFast)
            factor *= Config._sprintFactor.value;
        if(Config.isFreeBaseClimb() && value == MediumUpMotion)
            switch(getOnLadder(Integer.MAX_VALUE, false, isClimbCrawling))
            {
                case 1: factor *= Config._freeOneLadderClimbUpSpeedFactor.value; break;
                case 2: factor *= Config._freeBothLadderClimbUpSpeedFactor.value; break;
            }

        if(value > HoldMotion)
            value = ((value - HoldMotion) * Config._freeClimbingUpSpeedFactor.value * factor + HoldMotion);
        else
            value = HoldMotion - (HoldMotion - value) * Config._freeClimbingDownSpeedFactor.value * factor;

        if(hasClimbCrawlGap && isClimbCrawling && value > HoldMotion)
            value = Math.min(CatchCrawlGapMotion, value);
    }
    else
        isClimbingStill = true;

    boolean relevant = value < 0 || value > sp.motionY;
    if(relevant)
        sp.motionY = value;
    isClimbJumping = !relevant && !isClimbHolding;
}
```

`climbIntoCount > 0`이면 강제 `HoldMotion`. `HoldMotion` 기준 상/하 보간: 상방 `* Config._freeClimbingUpSpeedFactor.value`, 하방 `* Config._freeClimbingDownSpeedFactor.value`. `relevant = value < 0 || value > sp.motionY` — 음수이거나 현재 motionY보다 클 때만 적용.

---

## `beforeMoveEntity` / `moveEntity` / `afterMoveEntity` (1558-1660줄)

### `beforeMoveEntity`

```java
public void beforeMoveEntity(double d, double d1, double d2)
{
    beforeMoveEntityPosX = sp.posX;
    beforeMoveEntityPosY = sp.posZ;   // ← 주의: posZ를 posY 변수에 저장 (원본 버그 또는 의도)
    beforeMoveEntityPosZ = sp.posY;   // ← 주의: posY를 posZ 변수에 저장

    if(esp.movementInput.sneak || sneakToggled)
        if(isSwimming || isDiving || isCrawling || isClimbing || (!Config.isSneakingEnabled() && !isSneaking()))
            sp.ySize = 0F;
        else if(isSlow)
            sp.ySize = 0.6F;
        else
            sp.ySize = 0F;

    if(isSliding || isCrawling)
    {
        beforeDistanceWalkedModified = sp.distanceWalkedModified;
        sp.distanceWalkedModified = Float.MIN_VALUE;
    }

    if(wantWallJumping)
    {
        int collisions = calculateSeparateCollisions(d, d1, d2);
        horizontalCollisionAngle = getHorizontalCollisionangle(
            (collisions & CollidedPositiveZ) != 0,
            (collisions & CollidedNegativeZ) != 0,
            (collisions & CollidedPositiveX) != 0,
            (collisions & CollidedNegativeX) != 0);
    }
}
```

**주의:** `beforeMoveEntityPosY = sp.posZ`, `beforeMoveEntityPosZ = sp.posY` — X/Y/Z 명칭과 실제 저장 값이 뒤바뀜. (원본 버그로 보임 — `afterMoveEntity`에서 `d10 = posX - beforeMoveEntityPosX`, `d12 = posZ - beforeMoveEntityPosY`, `d13 = posY - beforeMoveEntityPosZ`로 올바르게 계산됨)

### `afterMoveEntity`

```java
@SuppressWarnings("unused")
public void afterMoveEntity(double d, double d1, double d2)
{
    if(isSliding || isCrawling)
        sp.distanceWalkedModified = beforeDistanceWalkedModified;

    if(heightOffset != 0F)
        sp.posY = sp.posY + heightOffset;

    wasOnGround = sp.onGround;

    double d10 = sp.posX - beforeMoveEntityPosX;
    double d12 = sp.posZ - beforeMoveEntityPosY;
    double d13 = sp.posY - beforeMoveEntityPosZ;

    double distance = MathHelper.sqrt_double(d10 * d10 + d12 * d12 + d13 * d13);

    if(isClimbing || isCeilingClimbing)
    {
        distanceClimbedModified += distance * (isClimbing ? 1.2 : 0.9);
        if(distanceClimbedModified > nextClimbDistance)
        {
            Block stepBlock;
            // handsEdgeBlock/feetEdgeBlock 기준, 둘 다 null이면 "cobblestone"
            // nextClimbDistance % 2 != 0 ? feetEdgeBlock : handsEdgeBlock (교대)
            nextClimbDistance++;
            if(stepBlock != null)
            {
                SoundType stepsound = stepBlock.stepSound;
                if(stepsound != null)
                    playSound(stepsound.getStepResourcePath(), stepsound.getVolume() * 0.15F, stepsound.getPitch());
            }
        }
    }

    if(isSwimming)
    {
        distanceSwom += distance;
        if(distanceSwom > SwimSoundDistance)
        {
            Random rand = sp.getRNG();
            playSound("random.splash", 0.05F, 1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.4F);
            distanceSwom -= SwimSoundDistance;
        }
    }
}
```

클라이밍 발소리: 거리 계수 클라이밍 `1.2`, 천장클라이밍 `0.9`. 볼륨 `* 0.15F`.  
수영 소리: `"random.splash"`, 볼륨 `0.05F`, 피치 `1.0F ± rand*0.4F`. 거리 기준: `SwimSoundDistance` (상위 클래스 상수).

### `playSound`

```java
private void playSound(String id, float volume, float pitch)
{
    Minecraft.getMinecraft().theWorld.playSound(sp.posX, sp.posY - sp.yOffset, sp.posZ, id, volume, pitch, false);
    SmartMovingPacketStream.sendSound(SmartMovingComm.instance, id, volume, pitch);
}
```

---

## 수면/수면 전 처리 (1668-1679줄)

```java
@SuppressWarnings("unused")
public void beforeSleepInBedAt(int i, int j, int k)
{
    if(!isp.getSleepingField())
        updateEntityActionState(true);
}

public EnumStatus sleepInBedAt(int i, int j, int k)
{
    beforeSleepInBedAt(i, j, k);
    return isp.localSleepInBedAt(i, j, k);
}
```

---

## heightOffset 관리 (1681-1703줄)

```java
private void resetHeightOffset()
{
    sp.boundingBox.minY += heightOffset;
    sp.height -= heightOffset;
    heightOffset = 0F;
}

private void resetInternalHeightOffset()
{
    sp.height -= heightOffset;
    heightOffset = 0F;
    // boundingBox.minY는 변경하지 않음
}

private void setHeightOffset(float offset)
{
    resetHeightOffset();
    if(offset == 0F)
        return;
    heightOffset = offset;
    sp.boundingBox.minY -= heightOffset;
    sp.height += heightOffset;
}
```

`offset = -1F` (크롤링/수영 등): `minY += 1F` (위로 이동), `height += 1F` (더 작아짐).  
`resetHeightOffset`: `minY -= heightOffset` (원위치), `height -= heightOffset`.  
`resetInternalHeightOffset`: boundingBox 변경 없이 height/flag만 리셋.

---

## 밝기 오버라이드 (1708-1722줄)

```java
public float getBrightness(float f)
{
    sp.posY -= heightOffset;
    float result = isp.localGetBrightness(f);
    sp.posY += heightOffset;
    return result;
}

public int getBrightnessForRender(float f)
{
    sp.posY -= heightOffset;
    int result = isp.localGetBrightnessForRender(f);
    sp.posY += heightOffset;
    return result;
}
```

heightOffset 적용 시 실제 위치로 복원하여 밝기 계산.

---

## `pushOutOfBlocks` (1724-1734줄)

```java
public boolean pushOutOfBlocks(double d, double d1, double d2)
{
    if(multiPlayerInitialized > 0)
        return false;

    boolean top = false;
    if(heightOffset != 0F)
        top = sp.height > 1F;

    return pushOutOfBlocks(d, d1, d2, top);
}
```

`multiPlayerInitialized > 0`이면 false 반환 (서버 위치 동기화 중). `height > 1F` → top = true (키 작을 때 블록 밖으로 밀기 방향 상단).

---

## `beforeOnUpdate` / `afterOnUpdate` (1736-1816줄)

```java
public void beforeOnUpdate()
{
    prevMotionX = sp.motionX;
    prevMotionY = sp.motionY;
    prevMotionZ = sp.motionZ;

    wasCollidedHorizontally = sp.isCollidedHorizontally;

    isJumping = false;

    if(sp.worldObj.isRemote && updateCounter < 10)
    {
        List<?> chatMessageList = Reflect.GetField(GuiNewChat.class, isp.getMcField().ingameGUI.getChatGUI(), SmartMovingInstall.GuiNewChat_chatMessageList);
        for(int i=0; i<chatMessageList.size(); i++)
            if(SmartMovingComm.processBlockCode(((ChatLine)chatMessageList.get(i)).func_151461_a().getUnformattedText()))
                chatMessageList.remove(i--);
        updateCounter++;
    }
}
```

처음 10틱 동안 채팅에서 SmartMoving 블록 코드를 처리.

```java
public void afterOnUpdate()
{
    correctOnUpdate(isSwimming || isDiving || isDipping || isCrawling, isSwimming);
    spawnParticles(isp.getMcField(), sp.motionX, sp.motionZ);

    float landMovementFactor = getLandMovementFactor();

    if(Config.enabled)
    {
        float perspectiveFactor = landMovementFactor;
        if(sp.isSprinting()) perspectiveFactor /= 1.3F;
        perspectiveFactor = 0.1f + ((perspectiveFactor - 0.1f) * Options._perspectiveSpeedFactor.value);
        if(Options._perspectiveSpeedFactorMax.value > 0)
            perspectiveFactor = MathHelper.clamp_float(perspectiveFactor, 0.1f - Options._perspectiveSpeedFactorMax.value * 0.1f, 0.1f + Options._perspectiveSpeedFactorMax.value * 0.1f);
        if(sp.isSprinting()) perspectiveFactor *= 1.3F;

        if(isFast || isSprintJump || isRunning())
        {
            if(sp.isSprinting()) perspectiveFactor /= 1.3F;
            if(isFast || isSprintJump)
                perspectiveFactor *= Options._perspectiveSprintFactor.value;
            else if(isRunning())
                perspectiveFactor *= 1.3F * Options._perspectiveRunFactor.value;
        }

        if(fadingPerspectiveFactor != -1)
            fadingPerspectiveFactor += (perspectiveFactor - fadingPerspectiveFactor) * Options._perspectiveFadeFactor.value;
        else
            fadingPerspectiveFactor = landMovementFactor;
    }

    if(sp.capabilities.disableDamage) exhaustion = 0;
    if(sp.capabilities.isFlying) sp.fallDistance = 0F;

    if(sp.isCollidedHorizontally)
        collidedHorizontallyTickCount++;
    else
        collidedHorizontallyTickCount = 0;

    addToSendQueue();

    if(wasInventory)
        sp.prevRotationYawHead = sp.rotationYawHead;
    wasInventory = isp.getMcField().currentScreen instanceof GuiInventory;
}
```

**perspectiveFactor 계산:** `0.1f + (landFactor - 0.1f) * Options._perspectiveSpeedFactor`. 스프린트 시 `/ 1.3F` 후 `* 1.3F` 정규화. isFast/sprintJump: `* Options._perspectiveSprintFactor`, isRunning: `* 1.3F * Options._perspectiveRunFactor`.  
**fadingPerspectiveFactor EMA:** `+= (target - current) * Options._perspectiveFadeFactor.value`.

---

## `beforeOnLivingUpdate` / `afterOnLivingUpdate` (1820-1833줄)

```java
public void beforeOnLivingUpdate()
{
    wasCapabilitiesIsFlying = sp.capabilities.isFlying;
}

public void afterOnLivingUpdate()
{
    if(Options._flyWhileOnGround.value && !(sneakButton.Pressed && grabButton.Pressed) && wasCapabilitiesIsFlying && !sp.capabilities.isFlying && sp.onGround)
    {
        sp.cameraYaw = 0;
        sp.prevCameraYaw = 0;
        sp.capabilities.isFlying = true;
        ((EntityClientPlayerMP)sp).sendQueue.addToSendQueue(new C13PacketPlayerAbilities(sp.capabilities));
    }
}
```

`flyWhileOnGround` 옵션: 착지 시 비행 능력 복원 (sneak+grab 동시 누름 제외).

---

## `handleJumping` (1842-1943줄)

```java
public void handleJumping()
{
    jumpPending = false;

    if(blockJumpTillButtonRelease && !esp.movementInput.jump)
        blockJumpTillButtonRelease = false;

    if(isSwimming || isDiving) return;

    boolean jump = jumpAvoided && isp.getIsJumpingField() && !sp.isInWater() && !sp.handleLavaMovement();
    jumpMotionX = sp.motionX;
    jumpMotionZ = sp.motionZ;

    // 점프 차징 (Config.isJumpChargingEnabled())
    // isJumpChargingPossible = sp.onGround && isStanding
    // isJumpCharging = isJumpChargingPossible && wouldIsSneaking
    // jump 버튼 누름 → jumpCharge++
    // 버튼 뗌 → tryJump(Config.ChargeUp, ...)로 방출

    // 헤드 점프 차징 (Config.isHeadJumpingEnabled())
    // isHeadJumpCharging = grabButton.Pressed && (isGroundSprinting || isSprintJump || running) && !isCrawling
    // jump 버튼 → headJumpCharge++, 뗌 → tryJump(Config.HeadUp, ...)

    // 수면 점프 (isDipping 중 jump)
    if(esp.movementInput.jump && sp.isInWater() && isDipping)
        if(sp.posY - MathHelper.floor_double(sp.posY) > (isSlow ? 0.37 : 0.6))
        {
            sp.motionY -= 0.039999999105930328D;
            if(!isStillSwimmingJump && sp.onGround && jumpCharge == 0)
                if(tryJump(Config.Up, true, null, null))
                    playSound("random.splash", 0.05F, ...);
        }

    if(jump && !blockJumpTillButtonRelease && !isJumpCharging && !isHeadJumpCharging && !isVineAnyClimbing)
        tryJump(Config.Up, false, null, null);

    // 방향 점프 (side/back)
    // leftJumpCount/rightJumpCount/backJumpCount: 더블클릭 감지
    // angle 계산 후 tryJump(Config.Angle, null, null, rotationYaw + angle)
}
```

**수면 점프 Y 조건:** `posY - floor(posY) > (isSlow ? 0.37 : 0.6)`  
**수면 점프 motionY:** `-= 0.039999999105930328D`

**방향 점프 각도 매핑:**
| left/right | back | angle |
|-----------|------|-------|
| left > 0 | back == 0 | 270 |
| left > 0 | back > 0 | 225 |
| left < 0 | back == 0 | 90 |
| left < 0 | back > 0 | 135 |
| left == 0 | back > 0 | 180 |

`angleJumpType = ((360 - angle) / 45) % 8`

---

## `handleWallJumping` (1946-1997줄)

```java
public void handleWallJumping()
{
    if(!wantWallJumping || Double.isNaN(horizontalCollisionAngle)) return;

    int jumpType;
    if(grabButton.Pressed)
    {
        if(sp.fallDistance > Config._wallHeadJumpFallMaximumDistance.value) return;
        jumpType = wasCollidedHorizontally ? Config.WallHeadSlide : Config.WallHead;
    }
    else
    {
        if(sp.fallDistance > Config._wallUpJumpFallMaximumDistance.value) return;
        jumpType = wasCollidedHorizontally ? Config.WallUpSlide : Config.WallUp;
    }

    float jumpAngle;
    if(!wasCollidedHorizontally)
    {
        float movementAngle = getAngle(jumpMotionZ, -jumpMotionX);
        if(Double.isNaN(movementAngle)) return;
        jumpAngle = horizontalCollisionAngle * 2 - movementAngle + 180F;
    }
    else
        jumpAngle = horizontalCollisionAngle;

    while(jumpAngle > 360F) jumpAngle -= 360F;

    if(Config._wallUpJumpOrthogonalTolerance.value != 0F)
    {
        float aligned = jumpAngle;
        while(aligned > 45F) aligned -= 90F;
        if(Math.abs(aligned) < Config._wallUpJumpOrthogonalTolerance.value)
            jumpAngle = Math.round(jumpAngle / 90F) * 90F;
    }

    if(tryJump(jumpType, null, null, jumpAngle))
    {
        continueWallJumping = !isHeadJumping;
        sp.isCollidedHorizontally = false;
        sp.rotationYaw = jumpAngle;
        onStartWallJump(jumpAngle);
    }
}
```

벽 반사 각도: `horizontalCollisionAngle * 2 - movementAngle + 180F`.  
직각 허용 오차: `Math.round(jumpAngle / 90F) * 90F` (Config 값이 0이 아닐 때).

---

## `tryJump` (1999-2136줄)

```java
public boolean tryJump(int type, Boolean inWaterOrNull, Boolean isRunningOrNull, Float angle)
```

**WallUpSlide/WallHeadSlide 처리:** type 변환 + `noVertical = true`

**jump 타입 분류:**
```java
boolean up = type == Config.Up || type == Config.ChargeUp || type == Config.HeadUp ||
    type == Config.ClimbUp || type == Config.ClimbUpHandsOnly ||
    type == Config.ClimbBackUp || type == Config.ClimbBackUpHandsOnly ||
    type == Config.ClimbBackHead || type == Config.ClimbBackHeadHandsOnly ||
    type == Config.Angle || type == Config.WallUp || type == Config.WallHead;
boolean head = type == Config.HeadUp || type == Config.ClimbBackHead ||
    type == Config.ClimbBackHeadHandsOnly || type == Config.WallHead;
```

**수직 모션 기본값:** `verticalMotion = -0.078 + 0.498 * verticalJumpFactor * jumpChargeFactor`

**vanilla Up 점프:**
```java
verticalMotion = 0.41999998688697815D;
if(sp.isPotionActive(Potion.jump))
    verticalMotion += (float)(amplifier + 1) * 0.1F;
if(sp.isSprinting())
{
    float f = sp.rotationYaw * 0.017453292F;
    sp.motionX -= MathHelper.sin(f) * 0.2F;
    sp.motionZ += MathHelper.cos(f) * 0.2F;
}
```

**headJump 각도 계산:**
```java
double normalAngle = Math.atan(verticalMotion / horizontalMotion);
double totalMotion = Math.sqrt(v*v + h*h);
double newAngle = Config.getHeadJumpFactor(headJumpCharge) * normalAngle;
verticalMotion = totalMotion * Math.sin(newAngle);
horizontalMotion = totalMotion * Math.cos(newAngle);
```

**angle 점프 (방향 지정):**
```java
float jumpAngle = angle / RadiantToAngle;
double horizontal = Math.max(horizontalMotion, horizontalJumpFactor);
double moveX = -Math.sin(jumpAngle);
double moveZ = Math.cos(jumpAngle);
sp.motionX = getJumpMoving(jumpMotionX, moveX, reset, horizontal, horizontalJumpFactor);
sp.motionZ = getJumpMoving(jumpMotionZ, moveZ, reset, horizontal, horizontalJumpFactor);
```

**Potion.jump 보정:**
```java
float jumpFactor = sp.isPotionActive(Potion.jump) ? 1 + (amplifier + 1) * 0.2F : 1;
```

**결과 적용:**
```java
if(up && !noVertical)
{
    sp.motionY = verticalMotion;
    sp.addStat(StatList.jumpStat, 1);
    isSprintJump = isFast;
}
if(exhausionEnabled)
    exhaustion += Config.getJumpExhaustionGain(speed, type, jumpCharge);
if(head) { isHeadJumping = true; setHeightOffset(-1); }
sp.isAirBorne = true;
isJumping = true;
onLivingJump();
```

### `getJumpMoving` (2138-2145줄)

```java
private static double getJumpMoving(double actual, double move, boolean reset, double horizontal, float horizontalJumpFactor)
{
    if(!reset)
        return actual + move * horizontal;
    else if(Math.signum(actual) != Math.signum(move))
        return move * horizontalJumpFactor;
    else
        return Math.max(Math.abs(actual), Math.abs(move) * horizontal) * Math.signum(move);
}
```

### `getJumpSpeed` (2148-2163줄)

```java
private static int getJumpSpeed(boolean isStanding, boolean isSneaking, boolean isRunning, boolean isSprinting, Float angle)
{
    isSprinting &= angle == null;
    isRunning &= angle == null;

    if(isSprinting) return Config.Sprinting;
    else if(isRunning) return Config.Running;
    else if(isSneaking) return Config.Sneaking;
    else if(isStanding) return Config.Standing;
    else return Config.Walking;
}
```

angle != null이면 스프린트/런 점프 타입 비활성화.

---

## standUp 관련 (2165-2243줄)

```java
private void standupIfPossible()   // 1-arg: gapUnder < 1이면 standUp 또는 toSlidingOrCrawling
private void standupIfPossible(boolean tryLanding, boolean restoreFromFlying)  // 2-arg: 비행→지면 전환용

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

`standUp`: `move(0, 1-gap, 0, true)`. `toSlidingOrCrawling`: `move(0, -gap, 0, true)`.

---

## `handleCrash` (2232-2243줄)

```java
private void handleCrash(float fallDamageStartDistance, float fallDamageFactor)
{
    if(sp.fallDistance >= 2.0F)
        sp.addStat(StatList.distanceFallenStat, (int)Math.round(sp.fallDistance * 100D));

    if(sp.fallDistance >= fallDamageStartDistance)
    {
        sp.attackEntityFrom(DamageSource.fall, (int)Math.ceil((sp.fallDistance - fallDamageStartDistance) * fallDamageFactor));
        distanceClimbedModified = nextClimbDistance; // to force step sound
    }
    sp.fallDistance = 0F;
}
```

`fallDistance >= 2.0F`: 통계 기록. 데미지: `ceil((fallDistance - startDist) * factor)`.

---

## `beforeSetPositionAndRotation` (2245-2252줄)

```java
public void beforeSetPositionAndRotation()
{
    if(sp.worldObj.isRemote)
    {
        initialized = false;
        multiPlayerInitialized = 5;
    }
}
```

서버에서 위치 업데이트 수신 시 초기화 플래그 리셋. `multiPlayerInitialized = 5` → 5틱 동안 `pushOutOfBlocks` 억제.

---

## `tickEssential` / `resetState` (2254-2305줄)

```java
public void tickEssential() {
    isActive = !Compat.isBlockedByIncompatibility(sp);

    toggleButton.update(Options.keyBindConfigToggle);

    if(toggleButton.StartPressed)
    {
        if(Config == Options)
            Config.toggle();
        else
            SmartMovingPacketStream.sendConfigChange(SmartMovingComm.instance);
    }

    if(!isActive()) {
        resetState();
    }
}
```

```java
private void resetState() {
    resetHeightOffset();
    this.isSlow = false;
    this.isFast = false;
    this.isClimbing = false;
    this.isHandsVineClimbing = false;
    this.isFeetVineClimbing = false;
    this.isClimbJumping = false;
    this.isClimbBackJumping = false;
    this.isWallJumping = false;
    this.isClimbCrawling = false;
    this.isCrawlClimbing = false;
    this.isCeilingClimbing = false;
    this.isRopeSliding = false;
    this.isDipping = false;
    this.isSwimming = false;
    this.isDiving = false;
    this.isLevitating = false;
    this.isHeadJumping = false;
    this.isCrawling = false;
    this.isSliding = false;
    this.isFlying = false;
    this.actualHandsClimbType = 0;
    this.actualFeetClimbType = 0;
    this.angleJumpType = 0;
    this.heightOffset = 0;
}
```

---

## `updateEntityActionState` (2307-3044줄)

이 메서드가 모든 입력 처리와 상태 전환의 핵심. 매 틱 호출됨.

### processBlockCode 채팅 스캔 (2347-2357줄) — 접속 초기 10틱만 실행

```java
if(sp.worldObj.isRemote && updateCounter < 10)
{
    List<?> chatMessageList = (List<?>)Reflect.GetField(
        GuiNewChat.class,
        isp.getMcField().ingameGUI.getChatGUI(),
        SmartMovingInstall.GuiNewChat_chatMessageList);
    for(int i=0; i<chatMessageList.size(); i++)
        if(SmartMovingComm.processBlockCode(
            ((ChatLine)chatMessageList.get(i)).func_151461_a().getUnformattedText()))
            chatMessageList.remove(i--);
    updateCounter++;
}
```

- 조건: `sp.worldObj.isRemote && updateCounter < 10`
- `Reflect.GetField`로 `GuiNewChat.chatMessageList` private 필드에 직접 접근
- `ChatLine.func_151461_a().getUnformattedText()` — §코드 포함 raw 문자열
- `processBlockCode` 반환 `true` → 해당 채팅 라인 제거 (플레이어에게 안 보임)
- `updateCounter`: SmartMovingSelf 인스턴스 필드. 10틱 후 이 블록은 실행 안 됨.

---

### 피로 리셋

```java
maxExhaustionForAction = Float.MAX_VALUE;
maxExhaustionToStartAction = Float.MAX_VALUE;
prevMaxExhaustionForAction = maxExhaustionForAction;
prevMaxExhaustionToStartAction = maxExhaustionToStartAction;
```

### 초기화 로직

```java
if(!initialized && !(sp.worldObj.isRemote && multiPlayerInitialized != 0) && !sp.isRiding())
{
    if(getMaxPlayerSolidBetween(sp.boundingBox.minY, sp.boundingBox.maxY, 0) > sp.boundingBox.minY)
    {
        initializeCrawling = true;
        toCrawling();
    }
    initialized = true;
}
```

### 점프 직접 제어

```java
isp.setIsJumpingField(
    esp.movementInput.jump && !isCrawling && !isSliding &&
    !(Config.isHeadJumpingEnabled() && grabButton.Pressed && sp.isSprinting()) &&
    !(Config.isJumpChargingEnabled() && wouldIsSneaking && sp.onGround && isStanding) &&
    !blockJumpTillButtonRelease);
```

### crawl/sneak/sprint 판정 (핵심 조건들)

```java
// mustCrawl: 머리 위에 고체 블록이 있어 설 수 없을 때
double crawlStandUpCeiling = getMinPlayerSolidBetween(sp.boundingBox.maxY, sp.boundingBox.maxY + 1.1D, 0);
mustCrawl = crawlStandUpCeiling - crawlStandUpBottom < sp.height - heightOffset;

// wantCrawl 조건
boolean wouldWantCrawl =
    !esp.capabilities.isFlying &&
    (
        (isCrawling && (inputContinueCrawl || contextContinueCrawl)) ||
        (grabButton.StartPressed && (sneakToggled || sneakButton.Pressed) && sp.onGround)
    );

// canCrawl 조건
boolean canCrawl =
    !isSwimming && !isDiving &&
    (!isDipping || (dippingDepth + heightOffset) < SwimCrawlWaterTopBorder) &&
    !isClimbing &&
    sp.fallDistance < Config._fallingDistanceMinimum.value;

// isCrawling = canCrawl && (wantCrawl || mustCrawl)

// wouldWantClimb
boolean wouldWantClimb =
    (
        grabButton.Pressed ||
        (isClimbHolding && sneakButton.Pressed) ||
        (Config.isFreeClimbAutoLaddderEnabled() && isFacedToLadder(isClimbCrawling)) ||
        (Config.isFreeClimbAutoVineEnabled() && isFacedToSolidVine)
    ) &&
    (!isSliding || grabButton.Pressed && esp.movementInput.moveForward > 0F) &&
    !isHeadJumping && !wantCrawlNotClimb && !disabled;

// wantClimbUp
wantClimbUp =
    wantClimb &&
    esp.movementInput.moveForward > 0F || (isVineAnyClimbing && jumpButton.Pressed && !(sneakButton.Pressed && isFacedToSolidVine)) &&
    (!isCrawling || sp.isCollidedHorizontally) &&
    (!isSliding || sp.isCollidedHorizontally);

// wantClimbDown
wantClimbDown = wantClimb && esp.movementInput.moveForward <= 0F && !wantCrawl;

// wantClimbCeiling
wantClimbCeiling =
    Config.isCeilingClimbingEnabled() &&
    grabButton.Pressed && !wantCrawlNotClimb && !isSneaking() && !disabled;
```

### isFast 결정

```java
boolean standing = sp.onGround && !isSliding && !isCrawling;

isFast =
    (isGroundSprinting && (!standing || Config._sprintEnableStanding.value)) ||
    isClimbSprinting ||
    isSwimSprinting ||
    isDiveSprinting ||
    isCeilingSprinting ||
    isFlyingSprinting ||
    isClimbSprinting;  // 중복
```

### isStanding 판정

```java
isStanding = horizontalSpeedSquare < 0.0005;
```

(`horizontalSpeedSquare = sp.motionX * sp.motionX + sp.motionZ * sp.motionZ`)

### isSlow 결정

```java
wouldIsSneaking = wouldWantSneak && !wantSprint && !isClimbing;
isSlow = wantSneak && wouldIsSneaking;
```

### isClimbHolding

```java
boolean wantClimbHolding =
    (isClimbHolding && sneakButton.Pressed) ||
    (isClimbing && blocked) ||
    (wantClimb && !isSwimming && !isDiving && !isCrawling && (sneakButton.Pressed || crawlToggled));

isClimbHolding = wantClimbHolding && isClimbing;
```

### isCrawlClimbing / isClimbCrawling 상태 전환 (2736-2820줄)

복잡한 상호 전환 로직. `climbIntoCount`를 카운터로 사용해 천천히 crawl gap으로 진입:
- `climbIntoCount > 1` → `climbIntoCount--`
- `isClimbCrawling && !needClimbCrawling && climbIntoCount == 0` → `climbIntoCount = 6`
- `isClimbCrawling = canClimbCrawling && ((needClimbCrawling && climbIntoCount == 0) || climbIntoCount > 1)`

### 벽 점프 (2863-2897줄)

```java
boolean canWallJumping = Config.isWallJumpEnabled() && !isHeadJumping && !sp.onGround && !isClimbing && !isSwimming && !isDiving && !isLevitating && !isFlying;

// 더블클릭 감지 (Options._wallJumpDoubleClick.value)
// wallJumpCount: doubleClick 타이머

wantWallJumping = canWallJumping &&
    (triggerWallJumping || continueWallJumping ||
    (wantWallJumping && jumpButton.Pressed && !sp.isCollidedHorizontally));
```

### 사이드/백 점프 더블클릭 (2898-2961줄)

`leftJumpCount`, `rightJumpCount`, `backJumpCount`:
- 0 → 첫 클릭 → `Options.angleJumpDoubleClickTicks()`로 설정
- 타이머 > 0 → 두 번째 클릭 → `-1` (트리거)
- 복합 조건: -1이고 다른 방향도 진행 중이면 -2로 변환 (대각 처리)

### 스닉/크롤 토글 (2966-3045줄) — 원본 전체 덤프 (R-09)

**필드**:
```java
// L3099-3100
private boolean sneakToggled = false;
private boolean crawlToggled = false;
// L3078
private boolean ignoreNextStopSneakButtonPressed;  // default false
```

**L2576 sneakContinueInput 계산** (isSneaking 연결 시작점):
```java
boolean sneakContinueInput = Options.isSneakToggleEnabled()
    ? sneakToggled || sneakButton.StartPressed
    : sneakButton.Pressed;
```

**L2577-2586 wouldWantSneak / wouldIsSneaking / isSlow 체인**:
```java
boolean wouldWantSneak =
    !isFlying &&
    !isSliding &&
    !isHeadJumping &&
    !(isDiving && Config._diveDownOnSneak.value) &&
    !(isSwimming && Config._swimDownOnSneak.value && !isFakeShallowWaterSneaking) &&
    sneakContinueInput &&
    !wantCrawl &&
    !mustCrawl &&
    (!Config.isCrawlingEnabled() || !grabButton.Pressed);

// L2711-2719
wouldIsSneaking =
    wouldWantSneak &&
    !wantSprint &&
    !isClimbing;

boolean wasSneaking = isSlow;
isSlow =
    wantSneak &&
    wouldIsSneaking;
```

**L2966-3045 상태 전환 블록 전체**:
```java
isRopeSliding = isRopeSliding();

boolean isSneakToggleEnabled = Options.isSneakToggleEnabled();
boolean isCrawlToggleEnabled = Options.isCrawlToggleEnabled();

boolean willStopCrawl = false;
boolean willStopCrawlStartSneak = false;
if(isSneakToggleEnabled || isCrawlToggleEnabled)
{
    if(isCrawling && jumpButton.StopPressed)
        willStopCrawlStartSneak = true;
    if(isCrawling && sneakButton.StopPressed && !ignoreNextStopSneakButtonPressed)
        willStopCrawlStartSneak = true;
    if(!isCrawling && !isCrawlClimbing && !isClimbCrawling)
        willStopCrawl = true;

    willStopCrawl |= willStopCrawlStartSneak;
}

boolean willStopSneak = false;
if(isSneakToggleEnabled)
{
    if(isCrawling && !willStopCrawlStartSneak)
        willStopSneak = true;
    if(wantSneak && wantSprint && sneakButton.StartPressed && sneakToggled)
    {
        willStopSneak = true;
        ignoreNextStopSneakButtonPressed = true;
    }
    if(wasSneaking && sneakButton.StartPressed)
        willStopSneak = true;
    if(!isSwimming && !isDiving && jumpButton.StopPressed)
        willStopSneak = true;
}

boolean willStartSneak = false;
if(isSneakToggleEnabled)
{
    if(willStopCrawlStartSneak && sneakButton.StopPressed)
        willStartSneak = true;
    if(isFast && sneakButton.StopPressed && !ignoreNextStopSneakButtonPressed)
        willStartSneak = true;
    if(isSlow && !wasSneaking)
        willStartSneak = true;
}

boolean willStartCrawl = false;
if(isCrawlToggleEnabled)
{
    if(isCrawling && !wasCrawling)
        willStartCrawl = true;
    if(isClimbCrawling && !wasClimbCrawling)
        willStartCrawl = true;
}

if(isSneakToggleEnabled)
{
    if(willStartSneak)
        sneakToggled = true;
    if(willStopSneak)
        sneakToggled = false;
}

if(isCrawlToggleEnabled)
{
    if(willStartCrawl)
    {
        crawlToggled = true;
        ignoreNextStopSneakButtonPressed = sneakButton.Pressed;
    }
    if(willStopCrawl)
        crawlToggled = false;
}

if(sneakButton.StopPressed)
    ignoreNextStopSneakButtonPressed = false;

wasRunning = isRunning;
wasLevitating = isLevitating;
```

**L2419-2432 wouldWantCrawl (sneakToggled 참조)**:
```java
boolean wouldWantCrawl =
    !esp.capabilities.isFlying &&
    (
        (isCrawling && (inputContinueCrawl || contextContinueCrawl)) ||
        (
            grabButton.StartPressed &&
            (sneakToggled || sneakButton.Pressed) &&
            sp.onGround
        )
    );
```

**L1570 beforeMoveEntity (sneakToggled 참조)**: ySize 보정 조건에 `esp.movementInput.sneak || sneakToggled`.

**연결 흐름 요약**: `sneakToggled` → `sneakContinueInput`(L2576) → `wouldWantSneak`(L2577) → `wouldIsSneaking`(L2711) → `isSlow`(L2717) → `isSneaking()`(L3231).

---

## `toCrawling` (3047-3054줄)

```java
private boolean toCrawling()
{
    isCrawling = true;
    if(Options.isCrawlToggleEnabled())
        crawlToggled = true;
    ignoreNextStopSneakButtonPressed = true;
    return true;
}
```

---

## `addToSendQueue` (3105-3221줄)

클라이언트→서버로 이동 상태를 long 비트필드로 직렬화하여 전송.

**비트 순서 (MSB→LSB, 총 33비트):**

| 비트 수 | 내용 |
|---------|------|
| 1 | `isp.localIsSneaking()` |
| 1 | `isRopeSliding` |
| 1 | `isWallJumping` |
| 1 | `isFast` |
| 1 | `isSlow` |
| 1 | `isClimbBackJumping` |
| 1 | `isClimbJumping` |
| 1 | `isHandsVineClimbing` |
| 1 | `isFeetVineClimbing` |
| 3 | `angleJumpType` |
| 1 | `isSliding` |
| 1 | `isHeadJumping` |
| 1 | `isLevitating` |
| 1 | `isCeilingClimbing` |
| 1 | `doFlyingAnimation()` |
| 1 | `doFallingAnimation()` |
| 1 | `isSmall` (height < 1) |
| 1 | `isClimbing` |
| 1 | `isCrawling` |
| 1 | `isCrawlClimbing` |
| 1 | `isSwimming` |
| 1 | `isDipping` |
| 1 | `isDiving` |
| 1 | `isp.getIsJumpingField()` |
| 4 | `actualHandsClimbType` |
| 4 | `actualFeetClimbType` |

**전송 조건:** `state != prevPacketState` 또는 `playerEntities` 목록 변경 시.

---

## 기타 메서드 (3225-3345줄)

```java
@Override
public boolean isSneaking()
{
    if(forceIsSneaking != null)
        return forceIsSneaking;

    return (isSlow && (sp.onGround || isp.getIsInWebField())) ||
        (!Config._sneak.value && wouldIsSneaking && jumpCharge > 0) ||
        ((sp.ridingEntity != null || !Config.enabled) && isp.localIsSneaking()) ||
        (!Config._crawlOverEdge.value && isCrawling && !isClimbing);
}

public boolean isStandupSprintingOrRunning()
{
    return (isFast || sp.isSprinting()) && sp.onGround && !isSliding && !isCrawling;
}

public boolean isRunning()
{
    return sp.isSprinting() && !isFast && (sp.onGround || vanilla());
}

public void beforeGetSleepTimer()
{
    SmartMovingRender.renderGuiIngame(isp.getMcField());
}

public void jump()
{
    jumpAvoided = true;
    jumpPending = true;
}

public void writeEntityToNBT(NBTTagCompound nBTTagCompound)
{
    isp.localWriteEntityToNBT(nBTTagCompound);
    NBTTagCompound abilities = nBTTagCompound.getCompoundTag("abilities");
    if(abilities != null && abilities.hasKey("flying"))
        abilities.setBoolean("flying", sp.capabilities.isFlying);
}

@Override
public boolean isJumping() { return isp.getIsJumpingField(); }

@Override
public boolean doFlyingAnimation()
{
    if(Config.isFlyingEnabled() || Config.isLevitationAnimationEnabled())
        return sp.capabilities.isFlying;
    return false;
}

@Override
public boolean doFallingAnimation()
{
    if(Config.isFallAnimationEnabled())
        return !sp.onGround && sp.fallDistance > Config._fallAnimationDistanceMinimum.value;
    return false;
}

public void onLivingJump()
{
    net.minecraftforge.common.ForgeHooks.onLivingJump(sp);
}

public float getFOVMultiplier()
{
    if(!Config.enabled)
        return isp.localGetFOVMultiplier();
    float landMovmentFactor = getLandMovementFactor();
    setLandMovementFactor(fadingPerspectiveFactor);
    float result = isp.localGetFOVMultiplier();
    setLandMovementFactor(landMovmentFactor);
    return result;
}

public float getLandMovementFactor()
{
    return sp.getAIMoveSpeed();
}

public void setLandMovementFactor(float landMovementFactor)
{
    Reflect.SetField(ModifiableAttributeInstance.class, sp.getEntityAttribute(SharedMonsterAttributes.movementSpeed),
        SmartMovingInstall.ModifiableAttributeInstance_attributeValue, landMovementFactor);
}

public static boolean isRopeSliding()
{
    return onZipLine != null && Reflect.GetField(onZipLine, null) != null;
}

public void beforeActivateBlockOrUseItem()
{
    forceIsSneaking = isp.localIsSneaking();
}

public void afterActivateBlockOrUseItem()
{
    forceIsSneaking = null;
}

private boolean vanilla()
{
    return !Config.enabled || Config._vanillaStyle.value;
}

private boolean isLevitating()
{
    return !Config.isFlyingEnabled() && sp.capabilities.isFlying;
}

public boolean isActive()
{
    return isActive && Config.enabled;
}

private Boolean forceIsSneaking;

// Ropes+ 호환 (static)
private static Class<?> ropesPlusClient = Reflect.LoadClass(SmartMovingInstall.class, SmartMovingInstall.RopesPlusClient, false);
private static Field onZipLine = ropesPlusClient != null ? Reflect.GetField(ropesPlusClient, SmartMovingInstall.RopesPlusClient_onZipLine, false) : null;
```

---

## import

```java
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.block.*;
import net.minecraft.block.Block.*;
import net.minecraft.client.*;
import net.minecraft.client.entity.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.*;
import net.minecraft.client.settings.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.player.*;
import net.minecraft.entity.player.EntityPlayer.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.network.play.client.*;
import net.minecraft.potion.*;
import net.minecraft.stats.*;
import net.minecraft.util.*;
import net.smart.moving.config.*;
import net.smart.moving.render.*;
import net.smart.utilities.*;
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMoving` | 상위 클래스. 상수(HoldMotion 등), 추상 메서드(isOnLadder, isOnVine, getGapUnderneight 등) |
| `ISmartMovingSelf` | 구현 인터페이스 |
| `IEntityPlayerSP` (`isp`) | vanilla EntityPlayerSP 액세스 (localMoveEntity, getIsJumpingField 등) |
| `EntityPlayer` (`sp`) | 플레이어 상태 직접 접근 |
| `SmartMovingConfig` (`Config`) | 설정값 전체 |
| `SmartMovingOptions` (`Options`) | 옵션값 전체 |
| `SmartMovingComm` | 패킷 통신 인스턴스 |
| `SmartMovingPacketStream` | 패킷 전송 (sendState, sendHungerChange, sendSound, ...) |
| `SmartMovingInstall` | 리플렉션 필드 이름 상수 |
| `SmartMovingRender` | `renderGuiIngame` 호출 |
| `Orientation` | 클라이밍 방향 판정 (seekClimbGap, getOrientation 등) |
| `HandsClimbing`, `FeetClimbing` | 클라이밍 타입 열거 |
| `ClimbGap` | 클라이밍 갭 정보 |
| `Button` | 버튼 상태 추적 |
| `Reflect` | 리플렉션 필드 접근 |
| `Compat` | 비호환 모드 감지 |
| `net.smart.render.statistics.SmartStatisticsFactory` | 클라이밍 스프린트 속도 판정 (`getTickDistance()`) |

---

## 주요 관찰 사항

1. **`beforeMoveEntityPosY = sp.posZ`, `beforeMoveEntityPosZ = sp.posY`**: 필드명과 실제 저장 축이 뒤바뀜. `afterMoveEntity`에서 `d12 = posZ - beforeMoveEntityPosY`, `d13 = posY - beforeMoveEntityPosZ`로 올바르게 복원되어 거리 계산은 정상 동작. 원본 버그이나 대칭적으로 처리되어 기능상 문제 없음.

2. **`setHeightOffset(-1F)` 패턴**: 크롤링/수영/다이빙/헤드점프 시 공통적으로 `heightOffset = -1F` 적용. `boundingBox.minY += 1F`, `height += 1F`로 플레이어가 낮아짐.

3. **`exhaustion`**: SmartMoving 자체 피로 시스템. vanilla 식량 통계와 별개. `foreignExhaustionFactor`를 통해 외부 모드가 피로를 추가할 수 있음.

4. **상태 직렬화 (addToSendQueue)**: 33비트 long 비트필드로 모든 이동 상태를 인코딩하여 서버에 전송.

5. **`isFast` 이중 카운팅**: `isFast = ... || isClimbSprinting || ... || isClimbSprinting` — `isClimbSprinting` 두 번 OR. 동작에 영향 없음(동일 값).

6. **`forceIsSneaking`**: `beforeActivateBlockOrUseItem`에서 `localIsSneaking()` 값으로 고정 후 블록 사용 처리, `after`에서 null 복원. 블록 상호작용 중 스닉 상태가 다른 로직에 의해 변경되지 않도록 보호.

7. **1.21.1 이식 고려 사항**:
   - `moveEntityWithHeading` → Mixin `@Inject` into `LivingEntity.travel()` 또는 `PlayerEntity.travel()`
   - `handleJumping` → `PlayerEntity.jump()` Mixin
   - `updateEntityActionState` → `ClientPlayerEntity.tickMovement()` 또는 입력 처리 경로
   - `heightOffset` (`boundingBox.minY`, `height` 조작) → `Entity.getDimensions()` + `Entity.calculateDimensions()` 패턴으로 대체 필요
   - `setLandMovementFactor` (Reflect로 attributeValue 직접 변경) → `AttributeInstance` API로 대체
   - `SmartMovingPacketStream` → Fabric `ServerPlayNetworking`/`ClientPlayNetworking`으로 대체
   - `MathHelper.floor_double`, `sqrt_double` → `MathHelper.floor`, `sqrt` (1.21.1 Fabric Yarn API)
   - `sp.isInWater()`, `sp.handleLavaMovement()` 등 → Fabric 동등 메서드 확인 필요

---

## R-05 추가 리서치 — isAerodynamic / SlideToHeadJumping / HorizontalAirodynamicDamping / canTriggerWalking (2026-04-23)

### `isAerodynamic` 세팅 로직 (행 2524~2560)

```java
// isHeadJumping 조건 재평가 — 매 틱
isHeadJumping = isHeadJumping &&
    !sp.onGround &&
    !(isSwimming || isDiving) &&
    !(isFlying || sp.capabilities.isFlying) &&
    !(sp.handleWaterMovement() && sp.motionY < 0) &&
    !sp.handleLavaMovement();

// isHeadJumping이 꺼지면 isAerodynamic도 리셋
if(!isHeadJumping)
    isAerodynamic = false;                          // 행 2533

// 슬라이드 → 헤드점프 전환 (SlideToHeadJumpingFallDistance = 0.05F)
if(isSliding && sp.fallDistance > SlideToHeadJumpingFallDistance)
{
    isSliding = false;
    isHeadJumping = true;
    isAerodynamic = true;                           // 행 2550
}

// 슬라이드 시작 시 isAerodynamic 리셋
if(Config.isSlidingEnabled() && grabButton.Pressed && ...)
{
    isSliding = true;
    isHeadJumping = false;
    isAerodynamic = false;                          // 행 2560
}
```

**요약**:
- `isAerodynamic = true`: 슬라이딩 중 `fallDistance > 0.05F` → 헤드점프로 전환될 때만
- `isAerodynamic = false`: 헤드점프가 꺼질 때마다 / 슬라이딩 새로 시작할 때

### `landMotion` 내 HorizontalAirodynamicDamping 적용 (행 751~755)

```java
// 공중 + isSliding=false 분기
else if(isAerodynamic)
    horizontalDamping = HorizontalAirodynamicDamping;   // 0.999F
else
    horizontalDamping = HorizontalAirDamping;           // 0.91F
```

`setLandMotions(horizontalDamping)`에서 `sp.motionX *= horizontalDamping; sp.motionZ *= horizontalDamping` 적용.

**1.21.1 대응**: vanilla travel() 종료 후 이미 0.91F 감쇠가 적용되어 있으므로,
TAIL inject에서 `velocity *= (0.999F / 0.91F)` 보정으로 교체.

### `canTriggerWalking` (행 1469~1471)

```java
public boolean canTriggerWalking()
{
    return !isClimbing && !isDiving;
}
```

클라이밍 또는 다이빙 중 발소리·발자국 파티클 억제.
1.21.1 `Entity.canTriggerWalking()` Mixin HEAD inject + cancellable로 구현.

---

## R-06 추가 리서치 — SwimCrawlWater 전환 로직 원본 소스 (2026-04-23)

### `handleSwimming()` 내 크롤링 분기 전체 (SmartMovingSelf.java L229~432)

```java
private boolean handleSwimming(float moveForward, float moveStrafing, ...)
{
    boolean handleSwimmingRejected = false;
    boolean handleSwimming = !isFlying && !isLiquidClimbing
            && (sp.isInWater() || (wasSwimming && isInLiquid()) || ...);
    if(handleSwimming)
    {
        resetClimbing();

        float wasHeightOffset = heightOffset;   // ← 크롤링 hitbox 오프셋 캡처

        boolean useStandard = !Config.isSwimmingEnabled() && !Config.isDivingEnabled();
        if(sp.ridingEntity != null) { resetSwimming(); useStandard = true; }

        if(useStandard && isCrawling)
            standupIfPossible();
        else
            resetHeightOffset();   // heightOffset = 0 으로 리셋

        if(!useStandard)
        {
            resetSwimming();

            // ... j, j_offset, totalSwimWaterBorder 계산 ...
            double playerSwimWaterBorder = totalSwimWaterBorder - j - j_offset;

            // ① standupIfPossible: 크롤링 중 수심 > TopBorder(0.65F) → 서기 시도
            if(isCrawling && playerSwimWaterBorder > SwimCrawlWaterTopBorder)
                standupIfPossible();

            double motionYDiff = 0;

            // ② 크롤링/크롤클라이밍 중이면 isDipping 강제, 그 외 수심 기반 분기
            if(isCrawling || isClimbCrawling || isCrawlClimbing)
                isDipping = true;
            else if(playerSwimWaterBorder >= 0 && playerSwimWaterBorder <= 2)
            {
                double offset = playerSwimWaterBorder + 0.1625D;
                // ... dipping/swimming/diving 3분류 및 motionYDiff 계산 ...
            }
            // ... playerSwimWaterBorder > 2 → diving ...

            // ③ dippingDepth 저장 및 playerCrawlWaterBorder 전환 판정
            dippingDepth = (float)playerSwimWaterBorder;
            float playerCrawlWaterBorder = dippingDepth + wasHeightOffset;

            if((isCrawling || isSliding) && playerCrawlWaterBorder < SwimCrawlWaterMaxBorder)
            {
                if(playerCrawlWaterBorder < SwimCrawlWaterTopBorder)
                {
                    // 얕은 물 — 계속 크롤링
                    setHeightOffset(wasHeightOffset);
                    handleSwimmingRejected = true;
                }
                else
                {
                    // 크롤링 → 수영 전환
                    if(wantShallowSwim) move(0, 0.1, 0, true);
                    isCrawling = false;
                    isDiving   = false;
                    isSwimming = true;
                    isDipping  = false;
                }
            }

            // ④ 수영/다이빙 상태 확정 후 setHeightOffset(-1F)
            if(isDiving || isSwimming)
                setHeightOffset(-1F);
        }
        // ... 실제 이동 물리 적용 ...
    }
    return !handleSwimmingRejected;
}
```

### `canCrawl` 조건 (SmartMovingSelf.java L2434~2445)

```java
boolean canCrawl =
    !isSwimming &&
    !isDiving &&
    (!isDipping || (dippingDepth + heightOffset) < SwimCrawlWaterTopBorder) &&
    !isClimbing &&
    sp.fallDistance < Config._fallingDistanceMinimum.value;

wasCrawling = isCrawling;
isCrawling  = canCrawl && (wantCrawl || mustCrawl);
```

### `dippingDepth` 필드 (SmartMovingSelf.java L89)

```java
public float dippingDepth;   // resetSwimming()에서 -1 로 초기화
```

### `wasHeightOffset` — 지역변수, `heightOffset` 필드 캡처

`handleSwimming()` 진입 시 `float wasHeightOffset = heightOffset;` 로 캡처.  
`heightOffset`은 이전 틱에서 `setHeightOffset()` 로 설정된 값:
- 수영/다이빙: `setHeightOffset(-1F)` → `heightOffset = -1F`
- 크롤링: 외부 `setHeightOffset()` 호출에 따라 결정 (원본 크롤링 코드 미수록)
- **1.21.1 근사**: bounding box minY 이동 없음 → `wasHeightOffset = 0` 으로 근사

### 1.21.1 대응 요약

| 원본 | 1.21.1 대응 |
|------|------------|
| `playerSwimWaterBorder` | `player.getFluidHeight(FluidTags.WATER)` |
| `wasHeightOffset` (크롤링) | `0F` (pose 기반 hitbox, minY 이동 없음) |
| `playerCrawlWaterBorder` | `dippingDepth + 0 = dippingDepth` |
| `standupIfPossible()` | `sm.isCrawling = false` (공간 체크 근사) |
| `setHeightOffset(wasHeightOffset)` (얕은 물) | `sm.isCrawling = true; return false` |
| `isCrawling=false; isSwimming=true` (전환) | `sm.isCrawling=false; sm.isSwimming_sm=true` |

**전환 임계값** (wasHeightOffset=0 가정):
- `dippingDepth < 0.65F` → 계속 크롤링 (얕은 물)
- `0.65F ≤ dippingDepth < 1.0F` → 수영으로 전환
- `dippingDepth ≥ 1.0F` → 일반 다이빙 물리 적용

---

## R-10 추가 리서치 — isFast/isSlow/wouldIsSneaking 의존 체인 완전 덤프 (2026-04-24 세션 31 — 포커스 #2 B-0)

포커스 #2 A-1 에서 isFast/isSlow/wouldIsSneaking 3건 불일치 확정. 1:1 이식을 위한
전수 덤프 — 원본 `SmartMovingSelf.java` 에서 필드 선언 + 계산 블록 + 사용처 전체.

### R-10.1 관련 필드 선언 (L1413-L1448)

```java
// --- SmartMovingSelf.java L1413-L1448 ---
public boolean wantClimbUp;
public boolean wantClimbDown;
public boolean wantSprint;                  // L1415 — R-10.5 계산
public boolean wantCrawlNotClimb;
public boolean wantClimbCeiling;

public boolean isStanding;
public boolean wouldIsSneaking;             // L1420 — R-10.9 계산
public boolean isVineOnlyClimbing;
public boolean isVineAnyClimbing;

public boolean isClimbingStill;
public boolean isClimbHolding;
public boolean isNeighborClimbing;
public boolean hasClimbGap;
public boolean hasClimbCrawlGap;
public boolean hasNeighborClimbGap;
public boolean hasNeighborClimbCrawlGap;

public float dippingDepth;

public boolean isJumping;
public boolean isJumpingOutOfWater;
public boolean isShallowDiveOrSwim;
public boolean isFakeShallowWaterSneaking;
public boolean isStillSwimmingJump;
public boolean isGroundSprinting;           // L1439 — R-10.7 계산 (public 필드)
public boolean isSprintJump;
public boolean isAerodynamic;
```

**주의**: `isGroundSprinting` 만 public 필드. 나머지 5 Sprint 변종
(`isClimbSprinting`/`isSwimSprinting`/`isDiveSprinting`/`isCeilingSprinting`/
`isFlyingSprinting`) + `isClimbSprintSpeed` / `canAnySprint` / `canVerticallySprint` /
`canHorizontallySprint` / `canAllSprint` / `standing` / `preferSprint` / `disabled` /
`wasGroundSprinting` 은 **지역 변수**.

### R-10.2 `disabled` 지역 변수 (L2373-L2375)

```java
// --- SmartMovingSelf.java L2373-L2375 ---
boolean isRiding = sp.ridingEntity != null;
boolean isSleeping = isp.getSleepingField();
boolean disabled = !Config.enabled || isRiding || isSleeping || startSleeping;
```

`wantSprint` L2615 에서 사용. 그 외 `wouldWantClimb` / `wantClimbCeiling` 등에서도 사용.

### R-10.3 `sneakContinueInput` / `wouldWantSneak` / `wantSneak` (L2576-L2590)

```java
// --- SmartMovingSelf.java L2576-L2590 ---
boolean sneakContinueInput = Options.isSneakToggleEnabled() ? sneakToggled || sneakButton.StartPressed : sneakButton.Pressed;
boolean wouldWantSneak =
    !isFlying &&
    !isSliding &&
    !isHeadJumping &&
    !(isDiving && Config._diveDownOnSneak.value) &&
    !(isSwimming && Config._swimDownOnSneak.value && !isFakeShallowWaterSneaking) &&
    sneakContinueInput &&
    !wantCrawl &&
    !mustCrawl &&
    (!Config.isCrawlingEnabled() || !grabButton.Pressed);

boolean wantSneak =
    Config.isSneakingEnabled() &&
    wouldWantSneak;
```

**핵심**: `wantSneak` = `Config.isSneakingEnabled() && wouldWantSneak`. 1.21.1 매핑:
`Config.isSneakingEnabled()` = `cfg.sneak && cfg.enabled` (Config 에 헬퍼 신설 권장).

### R-10.4 `moveButtonPressed` / `moveForwardButtonPressed` (L2592-L2593)

```java
// --- SmartMovingSelf.java L2592-L2593 ---
boolean moveButtonPressed = esp.movementInput.moveForward != 0F || esp.movementInput.moveStrafe != 0F;
boolean moveForwardButtonPressed = esp.movementInput.moveForward > 0F;
```

**1.21.1 매핑**: `MinecraftClient.getInstance().options.forwardKey` 등 KeyBinding 직접 참조
또는 `player.input.movementForward`. `esp.movementInput` 은 vanilla `MovementInput` — 1.21.1 `Input` 클래스.

### R-10.5 `wantSprint` 6조건 OR 계산 (L2595-L2615)

```java
// --- SmartMovingSelf.java L2595-L2615 ---
wantSprint =
    Config.isSprintingEnabled() &&
    !isSliding &&
    sprintButton.Pressed &&
    (
        moveForwardButtonPressed ||
        isClimbing ||
        (
            isSwimming &&
            (moveButtonPressed || (sneakButton.Pressed && Config._swimDownOnSneak.value))
        ) ||
        (
            isDiving &&
            (moveButtonPressed || jumpButton.Pressed || (sneakButton.Pressed && Config._diveDownOnSneak.value))
        ) ||
        (
            isFlying &&
            (moveButtonPressed || jumpButton.Pressed || sneakButton.Pressed)
        )
    ) &&
    !disabled;
```

**조건 분해** (6 게이트):
1. `Config.isSprintingEnabled()` — 1.21.1 매핑 `cfg.sprint && cfg.enabled`
2. `!isSliding` — ClientState 기존 필드
3. `sprintButton.Pressed` — vanilla sprint KeyBinding
4. **상황별 OR 5갈래**:
   - 지상: `moveForwardButtonPressed`
   - 등반: `isClimbing`
   - 수영: `isSwimming && (moveAny || (sneakDown && Config._swimDownOnSneak))`
   - 잠수: `isDiving && (moveAny || jump || (sneakDown && Config._diveDownOnSneak))`
   - 비행: `isFlying && (moveAny || jump || sneakDown)`
5. `!disabled` — R-10.2 지역 변수

### R-10.6 `isSprintJump` / `exhaustionAllowsSprinting` / `preferSprint` (L2633-L2657)

```java
// --- SmartMovingSelf.java L2633-L2657 ---
if(!sp.onGround && isFast && !isClimbing && !isCeilingClimbing && !isDiving && !isSwimming)
    isSprintJump = true;

boolean exhaustionAllowsSprinting =
    !Config.isSprintExhaustionEnabled() ||
    (
            exhaustion <= Config._sprintExhaustionStop.value &&
            (isFast || isSprintJump || exhaustion <= Config._sprintExhaustionStart.value)
    );

if(sp.onGround || isFlying || sp.capabilities.isFlying || isSwimming || isDiving || sp.handleLavaMovement())
    isSprintJump = false;

boolean preferSprint = false;
if(wantSprint && !wantSneak)
{
    if(!isSprintJump && Config.isSprintExhaustionEnabled())
    {
        maxExhaustionForAction = Math.min(maxExhaustionForAction, Config._sprintExhaustionStop.value);
        maxExhaustionToStartAction = Math.min(maxExhaustionToStartAction, Config._sprintExhaustionStart.value);
    }

    if(exhaustionAllowsSprinting)
        preferSprint = true;
}
```

**핵심**: `preferSprint = wantSprint && !wantSneak && exhaustionAllowsSprinting`.
`isSprintJump` 갱신은 `isFast` 이전 틱 값 참조 (순환 의존 주의). Sprint 변종 전부
`preferSprint` (또는 `canAnySprint` 파생) 에 의존.

**순환 의존 처리**: `isSprintJump` 은 이전 틱 `isFast` 사용. 1.21.1 이식 시 `isFast` 는 매 틱
계산되므로 이전 틱 값 저장 필요 (`wasFast` 또는 `isFast` 이 덮어쓰기 전).

### R-10.7 `isClimbSprintSpeed` + can* 4 판정 + 6 Sprint 변종 (L2659-L2684)

```java
// --- SmartMovingSelf.java L2659-L2684 ---
boolean isClimbSprintSpeed = true;
if(isClimbing && preferSprint)
{
    double minTickDistance;
    if(wantClimbUp)
        minTickDistance = 0.07 * Config._freeClimbingUpSpeedFactor.value;
    else if(wantClimbDown)
        minTickDistance = 0.11 * Config._freeClimbingDownSpeedFactor.value;
    else
        minTickDistance = 0.07;

    isClimbSprintSpeed = net.smart.render.statistics.SmartStatisticsFactory.getInstance(sp).getTickDistance() >= minTickDistance;
}

boolean canAnySprint = preferSprint && !sp.isBurning() && (Config._sprintDuringItemUsage.value || !sp.isUsingItem());
boolean canVerticallySprint = canAnySprint && !sp.isCollidedVertically;
boolean canHorizontallySprint = canAnySprint && collidedHorizontallyTickCount < 3;
boolean canAllSprint = canHorizontallySprint && canVerticallySprint;

boolean wasGroundSprinting = isGroundSprinting;
isGroundSprinting = canHorizontallySprint && (sp.onGround || isLevitating()) && !isSwimming && !isDiving && !isClimbing;
boolean isSwimSprinting = canHorizontallySprint && isSwimming;
boolean isDiveSprinting = canAllSprint && isDiving;
boolean isCeilingSprinting = canHorizontallySprint && isCeilingClimbing;
boolean isFlyingSprinting = canAllSprint && isFlying;
boolean isClimbSprinting = canAnySprint && isClimbing && isClimbSprintSpeed;
```

**isClimbSprintSpeed 의존** (등반 전용 속도 게이트):
- `SmartStatisticsFactory.getInstance(sp).getTickDistance()` 는 SmartRender 측 통계.
  1.21.1 에 이식된 statistics 있는지 확인 필요. 없으면 `true` 기본값 유지 (원본 게이트 의미 약화).
- `_freeClimbingUpSpeedFactor` / `_freeClimbingDownSpeedFactor` Config 필드.

**can* 4 판정 의존**:
- `sp.isBurning()` → `player.isOnFire()`
- `Config._sprintDuringItemUsage` Config 필드
- `sp.isUsingItem()` → `player.isUsingItem()` (1.21.1)
- `sp.isCollidedVertically` → `player.verticalCollision`
- `collidedHorizontallyTickCount` — ClientState 또는 별도 필드 (미이식 가능성 높음)

**6 Sprint 변종 의존**:
- `isLevitating()` — Self 메서드 (potion 레비테이션 체크)
- `isSwimming` / `isDiving` / `isClimbing` / `isCeilingClimbing` / `isFlying` — 전부 ClientState 기존 필드
- `sp.onGround` → `player.isOnGround()`

**참고**: `isClimbSprinting` 은 isFast 공식 L2690/L2695 에 **2회 등장** (중복). 원본 그대로 유지.

### R-10.8 `standing` + `isFast` (L2686-L2695) + isGroundSprinting 전환 후처리 (L2697-L2709)

```java
// --- SmartMovingSelf.java L2686-L2709 ---
boolean standing = sp.onGround && !isSliding && !isCrawling;

isFast =
    (isGroundSprinting && (!standing || Config._sprintEnableStanding.value)) ||
    isClimbSprinting ||
    isSwimSprinting ||
    isDiveSprinting ||
    isCeilingSprinting ||
    isFlyingSprinting ||
    isClimbSprinting;

if(isGroundSprinting && !wasGroundSprinting)
{
    wasRunningWhenSprintStarted = sp.isSprinting();
    sp.setSprinting(isStandupSprintingOrRunning());
}
else if(wasGroundSprinting && !isGroundSprinting)
{
    sp.setSprinting(Options._runOnSprintRelease.value || wasRunningWhenSprintStarted);
}
if(Options._walkOnSprintRelease.value && sprintButton.StopPressed)
{
    sp.setSprinting(false);
}
```

**`_sprintEnableStanding`**: `Config.java` L313 `Unmodified("move.sprint.enable.ground")` (기본값
false). true 로 설정 시 standing 상태에서도 Ground Sprint 인정.

**isGroundSprinting 전환 후처리**:
- Sprint 시작 엣지: `wasRunningWhenSprintStarted` 저장 + vanilla sprinting 재설정
- Sprint 종료 엣지: `_runOnSprintRelease` 에 따라 vanilla sprinting 유지
- `walkOnSprintRelease` + `sprintButton.StopPressed` → vanilla sprinting false

이 후처리는 vanilla `setSprinting()` 호출. 1.21.1 이식 시 `player.setSprinting(...)` 직접 호출.

### R-10.9 `wouldIsSneaking` + `isSlow` (L2711-L2719)

```java
// --- SmartMovingSelf.java L2711-L2719 ---
wouldIsSneaking =
    wouldWantSneak &&
    !wantSprint &&
    !isClimbing;

boolean wasSneaking = isSlow;
isSlow =
    wantSneak &&
    wouldIsSneaking;
```

**핵심**:
- `wouldIsSneaking` = `wouldWantSneak && !wantSprint && !isClimbing` (L2712 `!wantSprint` —
  SM 복합 6조건, vanilla `isSprinting()` 아님)
- `isSlow` = `wantSneak && wouldIsSneaking` (L2718 — `wantSneak` 은 R-10.3, 이미
  `Config.isSneakingEnabled() && wouldWantSneak` 포함)
- `wasSneaking` 지역 변수 — R-09 토글 블록 L3008 (`if(isSlow && !wasSneaking)`) 에서 사용

### R-10.10 `isFast` / `isSlow` 주요 사용처 (grep 전수)

```
L188     else if(isSlow)                                             — getSlowInputSpeedFactor
L201     if(isFast)                                                  — getSlowInputSpeedFactor
L321     motionYDiff = -0.05D * (isFast ? Config._sprintFactor.value : 1F);  — handleSwimming
L353     motionYDiff = 0.05D * (isFast ? Config._sprintFactor.value : 1F);   — handleSwimming
L404     if(isFast && playerSwimWaterBorder < 2.5 && sp.worldObj.isAirBlock(i, j + 3, k))  — handleSwimming
L486     wantJumpOutOfWater = (...) && diveUp && !isSlow;            — handleSwimming
L515     if(isSlow)                                                  — handleSwimming (dive jump height)
L686     if(esp.movementInput.jump && isFast && Config.isJumpingEnabled(Config.Sprinting, Config.Up))  — handleLand
L712     if(Config.isRunningEnabled() && isRunning() && !isFast)     — handleLand
L1202    hungerGainFactor = Config.getFactor(..., isSlow, isRunning, isFast, ...);  — handleExhaustion
L1239    if(isFast && Config.isSprintExhaustionEnabled())            — handleExhaustion
L1278    exhaustionLossFactor = Config.getFactor(..., isSlow, isRunning, isFast, ...);  — handleExhaustion
L1395    if(isSlow && crawlStandUpBottom > sp.boundingBox.minY + 0.5D)  — landMotionPost
L1523    if(isFast)                                                  — handleJumping (sprint jump factor)
L1573    else if(isSlow)                                             — handleJumping
L1779    if(isFast || isSprintJump || isRunning())                   — tryJump
L1784    if(isFast || isSprintJump)                                  — tryJump
L1860    isJumpCharging = isJumpChargingPossible && wouldIsSneaking;  — handleJumping (jumpCharge)
L1862    boolean actualJumpCharging = isJumpChargingPossible && (!Config._jumpChargeCancelOnSneakRelease.value || wouldIsSneaking);
L1864    if(esp.movementInput.jump && (Config._jumpChargeCancelOnSneakRelease.value || wouldIsSneaking))
L1902    if(sp.posY - MathHelper.floor_double(sp.posY) > (isSlow ? 0.37 : 0.6))  — handleJumping (dip jump offset)
L2014    int speed = getJumpSpeed(isStanding, isSlow, isRunning, isFast, angle);
L2117    isSprintJump = isFast;                                      — tryJump
L2275    this.isSlow = false;                                        — resetState
L2276    this.isFast = false;                                        — resetState
L2369    !(Config.isJumpChargingEnabled() && wouldIsSneaking && sp.onGround && isStanding)  — updateEntityActionState
L2633    if(!sp.onGround && isFast && ...)                           — R-10.6 (순환 의존 이전 틱)
L2640    (isFast || isSprintJump || ...)                             — R-10.6 exhaustionAllowsSprinting
L2990    if(wantSneak && wantSprint && sneakButton.StartPressed && sneakToggled)  — R-09 토글
L3006    if(isFast && sneakButton.StopPressed && !ignoreNextStopSneakButtonPressed)
L3008    if(isSlow && !wasSneaking)                                  — R-09 토글
L3122    state |= isFast ? 1 : 0;                                    — StatePacket 송신
L3125    state |= isSlow ? 1 : 0;                                    — StatePacket 송신
L3231    return (isSlow && (sp.onGround || isp.getIsInWebField())) || ...;  — localIsSneaking override
L3236    return (isFast || sp.isSprinting()) && sp.onGround && !isSliding && !isCrawling;  — getIsSprinting override
L3241    return sp.isSprinting() && !isFast && (sp.onGround || vanilla());  — isRunning override
```

**포커스 #2 중요 사용처**:
- L2275/L2276 `resetState` 리셋 (1.21.1 동일 위치 대응 필요)
- L2716-L2719 **공식** 자체 (A-1 에서 발견된 불일치 위치)
- L3122/L3125 StatePacket 송신 (state 필드 비트 매핑)
- L3231/L3236 override 메서드 (vanilla 메서드 재정의)

### R-10.11 1.21.1 이식 매핑 예비안

| 원본 | 1.21.1 매핑 | 상태 |
|---|---|---|
| `Config._sprintEnableStanding.value` | `SmartMovingConfig.sprintEnableStanding` (미이식) | **신설 B-1a** |
| `Config.isSprintingEnabled()` | `cfg.sprint && cfg.enabled` 헬퍼 신설 | **신설 B-3a** |
| `Config.isSneakingEnabled()` | `cfg.sneak && cfg.enabled` 헬퍼 신설 | **신설 B-2** |
| `Config._swimDownOnSneak.value` | `cfg.swimDownOnSneak` | 이식됨 (`SmartMovingConfig`) |
| `Config._diveDownOnSneak.value` | `cfg.diveDownOnSneak` | 이식됨 |
| `Config._sprintDuringItemUsage.value` | 미이식 가능성 | **신설 검토 (B-1c)** |
| `Config._sprintFactor.value` | `cfg.sprintFactor` 확인 | 이식됨 추정 |
| `Config._freeClimbingUpSpeedFactor.value` | 이식됨 확인 필요 | — |
| `Config._freeClimbingDownSpeedFactor.value` | 이식됨 확인 필요 | — |
| `sprintButton.Pressed` | `MinecraftClient.options.sprintKey.isPressed()` | **B-1b 매핑** |
| `jumpButton.Pressed` | `MinecraftClient.options.jumpKey.isPressed()` | **B-1b 매핑** |
| `sneakButton.Pressed` | `MinecraftClient.options.sneakKey.isPressed()` | **B-1b 매핑** |
| `grabButton.Pressed` | `SmartMovingKeys.grab.isPressed()` | 이식됨 |
| `sprintButton.StopPressed` | `SmartMovingKeys.*` 엣지 또는 이전 틱 비교 | **B-1b 매핑** |
| `esp.movementInput.moveForward/moveStrafe` | `player.input.movementForward/movementSideways` | **매핑** |
| `sp.isCollidedVertically` | `player.verticalCollision` | — |
| `collidedHorizontallyTickCount` | 별도 필드 필요 (미이식) | **신설 검토 (B-1c)** |
| `isLevitating()` | vanilla levitation status effect 체크 | — |
| `SmartStatisticsFactory.getTickDistance()` | SmartRender 측 — 1.21.1 이식 여부 확인 | — |
| `sp.isBurning()` | `player.isOnFire()` | — |
| `sp.isUsingItem()` | `player.isUsingItem()` | — |
| `isSprintJump` (이전 틱 순환) | `wasFast` 저장 또는 동일 틱 이전 계산 | **B-1 설계 시 주의** |

**1.21.1 이식 시 주의사항**:
- **계산 순서 엄수**: `disabled` → `wouldWantSneak/wantSneak` → `wantSprint` → Sprint 변종 →
  `standing` → `isFast` → `wouldIsSneaking` → `isSlow`. 순환 의존 (`isSprintJump`) 은
  이전 틱 값 저장으로 해결.
- **isGroundSprinting 전환 후처리** (L2697-L2709) 의 vanilla `setSprinting(...)` 호출은
  1.21.1 이식 시 동일하게 `player.setSprinting(...)`. Options 필드 매핑 필요.
- **collidedHorizontallyTickCount** / **SmartStatisticsFactory** 미이식 시 각각
  `0` / `true` 근사 또는 신설 원자로 분리 — 포커스 #2 범위 내 판단.

---

## R-11 추가 리서치 — isSwimming/isDiving/isDipping 수중 3상태 전수 덤프 (2026-04-24 세션 32 — 포커스 #2 A-2)

포커스 #2 A-2 감사 — 수중 3상태 갱신 로직 전수 매핑. 기존 `handleSwimming` 섹션 (L414-L499)
보완 + 1.21.1 `SmartMovingSwimmer.updateSwimState` side-by-side.

### R-11.1 관련 필드 선언

```java
// --- SmartMoving.java (부모) L38-L41 ---
public boolean isDipping;               // 수면 경계 (발만 잠김)
public boolean isSwimming;              // 수영 중
public boolean isDiving;                // 잠수 중
public boolean isLevitating;            // 부양 (diving 내 정지)

// --- SmartMovingSelf.java L1432, L1435-L1437, L1448 ---
public float dippingDepth;              // = playerSwimWaterBorder
public boolean isJumpingOutOfWater;     // 수면 점프 진행 중
public boolean isShallowDiveOrSwim;     // couldStandUp && (isDiving || isSwimming)
public boolean isFakeShallowWaterSneaking;  // 얕은 물 sneak 가짜 플래그
public int waterMovementTicks;          // swimming||diving 지속 틱 카운터
public boolean isStillSwimmingJump;     // 수영 점프 hold 상태
```

**1.21.1 이식 상태**:
- `isDipping` / `isSwimming_sm` (접미사) / `isDiving` / `dippingDepth` — ClientState 이식됨
- `isFakeShallowWaterSneaking` / `waterMovementTicks` / `isCrawlClimbing` / `isClimbCrawling` — 이식됨
- **`isShallowDiveOrSwim` / `isJumpingOutOfWater` / `isStillSwimmingJump` / `isLiquidClimbing` / `isLevitating` — 미이식** (grep 확인)

### R-11.2 handleSwimming 진입 조건 (L232-L249)

```java
// --- SmartMovingSelf.java L232-L249 ---
boolean handleSwimming = !isFlying && !isLiquidClimbing && (sp.isInWater() || (wasSwimming && isInLiquid()) || (Config.isLavaLikeWaterEnabled() && sp.handleLavaMovement()));
if(handleSwimming)
{
    resetClimbing();

    float wasHeightOffset = heightOffset;

    boolean useStandard = !Config.isSwimmingEnabled() && !Config.isDivingEnabled();
    if(sp.ridingEntity != null)
    {
        resetSwimming();
        useStandard = true;
    }

    if(useStandard && isCrawling)
        standupIfPossible();
    else
        resetHeightOffset();
```

**진입 조건 (3-OR 안쪽)**:
1. `sp.isInWater()` — 현재 물속
2. `wasSwimming && isInLiquid()` — 이전 틱 swimming + 여전히 액체
3. `Config.isLavaLikeWaterEnabled() && sp.handleLavaMovement()` — 라바 수영 옵션 + 라바 접촉

**진입 차단 (2-AND 바깥)**:
- `!isFlying` — 비행 중 아님
- `!isLiquidClimbing` — 물 등반 중 아님

**useStandard 게이트**:
- `Config.isSwimmingEnabled()` / `Config.isDivingEnabled()` 둘 다 false → useStandard
- `sp.ridingEntity != null` → resetSwimming + useStandard

### R-11.3 SM 경로 사전 준비 (L251-L296)

```java
// --- SmartMovingSelf.java L251-L296 (요약) ---
if(!useStandard)
{
    resetSwimming();

    int i = MathHelper.floor_double(sp.posX);
    int j = MathHelper.floor_double(sp.boundingBox.minY);
    int k = MathHelper.floor_double(sp.posZ);

    boolean swimming = false;  // 지역변수
    boolean diving = false;
    boolean dipping = false;

    double j_offset = sp.boundingBox.minY - j;

    double totalSwimWaterBorder = getMaxPlayerLiquidBetween(sp.boundingBox.maxY - 1.8, sp.boundingBox.maxY + 1.2);
    double minPlayerSwimWaterCeiling = getMinPlayerSolidBetween(sp.boundingBox.maxY - 1.8, sp.boundingBox.maxY + 1.2, 0);
    double realTotalSwimWaterBorder = Math.min(totalSwimWaterBorder, minPlayerSwimWaterCeiling);
    double minPlayerSwimWaterDepth = totalSwimWaterBorder - getMaxPlayerSolidBetween(totalSwimWaterBorder - 2, totalSwimWaterBorder, 0);
    double realMinPlayerSwimWaterDepth = totalSwimWaterBorder - getMaxPlayerSolidBetween(realTotalSwimWaterBorder - 2, realTotalSwimWaterBorder, 0);
    double playerSwimWaterBorder = totalSwimWaterBorder - j - j_offset;

    if(isCrawling && playerSwimWaterBorder > SwimCrawlWaterTopBorder)
        standupIfPossible();

    double motionYDiff = 0;
    boolean couldStandUp = playerSwimWaterBorder >= 0 && minPlayerSwimWaterDepth <= 1.5;

    boolean diveUp = isp.getIsJumpingField();
    boolean diveDown = esp.movementInput.sneak && Config._diveDownOnSneak.value;
    boolean swimDown = esp.movementInput.sneak && Config._swimDownOnSneak.value;

    boolean wantShallowSwim = couldStandUp && (wasSwimming || wasDiving);
    if(wantShallowSwim) {
        HashSet<Orientation> orientations = Orientation.getClimbingOrientations(sp, true, true);
        // 4방향+대각 8방향 isTunnelAhead 검사
        while(iterator.hasNext())
            if(!(wantShallowSwim &= !iterator.next().isTunnelAhead(sp.worldObj, i, j, k))) break;
    }

    if(wasSwimming && wantShallowSwim && swimDown) {
        swimDown = false;
        isFakeShallowWaterSneaking = true;
    }

    if(isDiving && diveUp && diveDown)
        diveUp = diveDown = false;
```

**핵심 개념**:
- **SM 정밀 AABB**: `getMaxPlayerLiquidBetween` / `getMinPlayerSolidBetween` / `getMaxPlayerSolidBetween`
  — 반-블록 단위 Y 범위 스캔. 1.21.1 에 대응 없음 → `player.getFluidHeight(WATER)` 근사 (§7 기록).
- **`playerSwimWaterBorder` = 플레이어 minY 기준 액체 경계 Y 오프셋**
- **`couldStandUp` = border>=0 && depth<=1.5** — 얕은 물 판정
- **`wantShallowSwim`** — 얕은 물 continuity (이전 틱 swim/dive 였을 때만)
- **`isFakeShallowWaterSneaking`** = wasSwimming + wantShallowSwim + swimDown 엣지 (swimDown 억제)

### R-11.4 크롤/ClimbCrawl/CrawlClimb 강제 isDipping (L301-L302)

```java
// --- SmartMovingSelf.java L301-L302 ---
if(isCrawling || isClimbCrawling || isCrawlClimbing)
    isDipping = true;
```

**3-OR 조건** — 크롤/등반크롤/크롤등반 중 어느 것이든 수중 진입 시 강제로 isDipping.
**1.21.1 불일치**: Swimmer L78 은 `isCrawling || isCrawlClimbing` 만 (isClimbCrawling 누락).

### R-11.5 메인 분류 블록 (L303-L414) — 3-갈래

```java
// --- SmartMovingSelf.java L303-L414 (요약) ---
else if(playerSwimWaterBorder >= 0 && playerSwimWaterBorder <= 2)
{
    double offset = playerSwimWaterBorder + 0.1625D;
    boolean moveSwim = sp.rotationPitch < 0F && esp.movementInput.moveForward > 0F
                    || sp.rotationPitch > 0F && esp.movementInput.moveForward < 0F;
    if(diveUp || moveSwim || wantShallowSwim) {
        // A경로 — 활성 수영 경계 테이블
        if(offset < 1.4)        { dipping = true;  /* motionYDiff: offset<1 → -0.02, else -0.01 */ }
        else if(offset < 1.9)   { swimming = true; /* motionYDiff: 11-단계 offset 테이블 */ }
        else                    { diving = true;   /* motionYDiff: diveUp/diveDown/moveSwim 조합 */ }
    } else {
        // B경로 — 비활성 수영 경계 테이블
        if(offset < 1.5)        { dipping = true;  /* motionYDiff: -0.02 일괄 */ }
        else                    { diving = true;   /* motionYDiff: 10-단계 offset 테이블 */ }
    }
}
else if(playerSwimWaterBorder > 2)
{
    diving = true;
    // motionYDiff: diveUp + isFast 분기 (물속 스프린트 점프 특수)
    if(diveUp && isFast && playerSwimWaterBorder < 2.5 && isAirBlock(j+3))
        motionYDiff = 0.11D / Config._sprintFactor.value;
    else if(diveUp)   motionYDiff = 0.01 + 0.1 * speedFactor;
    else if(diveDown) motionYDiff = 0.01 - 0.1 * speedFactor;
    else              motionYDiff = 0.01D;
}
else
    handleSwimmingRejected = true;   // border < 0 → SM 비처리
```

**3-갈래 게이트**:
1. **[0, 2]** 구간 — 경계 부근 수영/다이빙 세분화
2. **(2, ∞)** 구간 — 항상 diving (깊은 물)
3. **(-∞, 0)** 구간 — handleSwimmingRejected (물밖)

**A/B 서브경로** (구간 1 내부):
- A: `diveUp || moveSwim || wantShallowSwim` — 활성 수직 의도
- B: else — 중립 (피동)

**offset 테이블 (A/swimming, offset 1.4~1.9)** — 11단계 motionYDiff:
1.4<=o<1.5:-0.02, <1.6:-0.01, <1.62:-0.005, <1.64:-0.0025, <1.66:-0.00125, <1.664:-0.000625, <1.668:0,
<1.672:+0.000625, <1.676:+0.00125, <1.68:+0.0025, <1.7:+0.005, <1.8:+0.01, else:+0.02.
swimDown 억제 시 `-0.05 * (isFast ? sprintFactor : 1)` 덮어쓰기.

### R-11.6 크롤↔수영 전환 (L416-L434) — R-06 구간

```java
// --- SmartMovingSelf.java L416-L434 ---
dippingDepth = (float)playerSwimWaterBorder;
float playerCrawlWaterBorder = dippingDepth + wasHeightOffset;
if((isCrawling || isSliding) && playerCrawlWaterBorder < SwimCrawlWaterMaxBorder)
    if(playerCrawlWaterBorder < SwimCrawlWaterTopBorder)
    {
        // continue crawling in shallow water
        setHeightOffset(wasHeightOffset);
        handleSwimmingRejected = true;
    }
    else
    {
        // from crawling in shallow water to swimming/diving
        if(wantShallowSwim) move(0, 0.1, 0, true);
        isCrawling = false;
        isDiving = false;
        isSwimming = true;
        isDipping = false;
    }
```

**진입 조건**: `(isCrawling || isSliding) && playerCrawlWaterBorder < SwimCrawlWaterMaxBorder(=1.0)`
**내부 2분기**:
- `playerCrawlWaterBorder < SwimCrawlWaterTopBorder(=0.65)` — 얕은 물 유지 (crawl)
- else — 크롤→수영 전환 (isCrawling=false, isSwimming=true, isDipping=false)

### R-11.7 Config 게이트 + useStandard 재판정 (L436-L441)

```java
// --- SmartMovingSelf.java L436-L441 ---
if(!handleSwimmingRejected)
{
    swimming = !useStandard && swimming && Config.isSwimmingEnabled();
    diving = !useStandard && diving && Config.isDivingEnabled();
    dipping = !useStandard && dipping && Config.isSwimmingEnabled();
    useStandard = !swimming && !diving && !dipping;
```

**분류 이후 Config 재게이트** — 원본 L303-L414 분류만으로는 부족, Config 옵션 게이트 통과해야 함.

### R-11.8 수중 3상태 갱신 (L504-L511)

```java
// --- SmartMovingSelf.java L481-L511 ---
if(swimming || diving)
    waterMovementTicks++;
else
    waterMovementTicks = 0;                          // dipping 에서는 리셋!

boolean wantJumpOutOfWater = (moveForward != 0 || moveStrafing != 0)
    && sp.isCollidedHorizontally && diveUp && !isSlow;
isJumpingOutOfWater = wantJumpOutOfWater
    && (waterMovementTicks > 10 || sp.onGround || wasJumpingOutOfWater);

// ... diving/swimming motionY 처리 후:
isDiving = diving;
isLevitating = levitating;
isSwimming = swimming;
isShallowDiveOrSwim = couldStandUp && (isDiving || isSwimming);
isDipping = dipping;

if(isDiving || isSwimming)
    setHeightOffset(-1F);
```

**공식**:
- `isDiving = diving` (지역변수 → 필드)
- `isSwimming = swimming`
- `isDipping = dipping`
- `isShallowDiveOrSwim = couldStandUp && (isDiving || isSwimming)` — 원본 필드
- `waterMovementTicks` 증분: `swimming || diving` 만 (dipping 시 리셋)
- `isJumpingOutOfWater` — 수면 탈출 점프 진행 조건

### R-11.9 얕은 물 특수 분기 (L513-L536)

```java
// --- SmartMovingSelf.java L513-L536 ---
if(isShallowDiveOrSwim && realMinPlayerSwimWaterDepth < SwimCrawlWaterBottomBorder)
{
    if(isSlow)
    {
        // from swimming/diving in shallow water to crawling in shallow water
        setHeightOffset(-1F);
        isCrawling = true;
        isDiving = false;
        isSwimming = false;
        isShallowDiveOrSwim = false;
        isDipping = true;
    }
    else
    {
        // from swimming/diving in shallow water to walking in shallow water
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

**얕은 물 전환** (shallow depth < SwimCrawlWaterBottomBorder):
- `isSlow` → crawl 진입 (isCrawling=true + isDipping=true)
- else → walking in shallow water (isCrawling=false + isDipping=true)

### R-11.10 useStandard=true 경로 + 진입 실패 (L544-L551)

```java
// --- SmartMovingSelf.java L544-L551 ---
}   // if(!useStandard)
else    // useStandard=true 경로
{
    isDiving = false;
    isSwimming = false;
    isShallowDiveOrSwim = false;
    isDipping = false;
    isStillSwimmingJump = false;
}
```

**useStandard 진입 시 3상태 + isStillSwimmingJump 리셋**.

### R-11.11 리셋 위치 (resetSwimming / resetState / landMotionPost / fromSwimmingOrDiving)

```java
// --- SmartMovingSelf.java L1488-L1498 resetSwimming ---
private void resetSwimming()
{
    dippingDepth = -1;
    isDipping = false;
    isSwimming = false;
    isDiving = false;
    isLevitating = false;
    isShallowDiveOrSwim = false;
    isFakeShallowWaterSneaking = false;
    isJumpingOutOfWater = false;
}

// --- SmartMovingSelf.java L2290-L2297 resetState ---
this.isDipping = false;
this.isSwimming = false;
this.isDiving = false;
this.isLevitating = false;
// ... 그 외 모든 상태 리셋

// --- SmartMovingSelf.java L1377-L1403 landMotionPost / fromSwimmingOrDiving ---
if(crawlStandUpCeiling - crawlStandUpBottom < sp.height) {
    // from diving in deep water to crawling in small hole
    isCrawling = true;
    isDipping = false;
    setHeightOffset(-1F);
} else if(crawlStandUpLiquidCeiling - crawlStandUpBottom < sp.height) {
    // from diving in deep water to crawling below the water
    isCrawling = true;
    contextContinueCrawl = true;
    isDipping = false;
    setHeightOffset(-1F);
} else if(crawlStandUpBottom > sp.boundingBox.minY) {
    if(isSlow && crawlStandUpBottom > sp.boundingBox.minY + 0.5D) {
        isCrawling = true;
        isDipping = false;
        setHeightOffset(-1F);
    }
    move(0, (crawlStandUpBottom - sp.boundingBox.minY), 0, true);
}
```

**수중 퇴장 시 crawl 로 전환** — landMotionPost 내 3분기:
1. 천장 낮음 → crawl + 공간 없음
2. 액체 천장 낮음 → crawl + contextContinueCrawl=true
3. 공간 있음 → isSlow 면 crawl 전환 + 이동 조정

### R-11.12 1.21.1 `SmartMovingSwimmer.updateSwimState` side-by-side + 불일치 11건

**1.21.1 코드** (`SmartMovingSwimmer.java` L64-L91):

```java
public static void updateSwimState(ClientPlayerEntity player, SmartMovingClientState sm) {
    if (!player.isTouchingWater()) {
        sm.isDipping = false;
        sm.isSwimming_sm = false;
        sm.isDiving = false;
        sm.waterMovementTicks = 0;
        sm.dippingDepth = -1F;
        return;
    }
    double fluidHeight = player.getFluidHeight(FluidTags.WATER);
    sm.dippingDepth = (float)fluidHeight;
    if (sm.isCrawling || sm.isCrawlClimbing) {    // ★ isClimbCrawling 누락
        sm.isDipping = true;
        sm.isSwimming_sm = false;
        sm.isDiving = false;
        sm.waterMovementTicks++;
        return;
    }
    double offset = fluidHeight + 0.1625D;
    sm.isDipping = offset < OFFSET_SWIMMING;        // 1.4
    sm.isSwimming_sm = offset >= 1.4 && offset < OFFSET_DIVING;   // 1.4~1.9
    sm.isDiving = offset >= OFFSET_DIVING;          // 1.9+
    sm.waterMovementTicks++;
}
```

**불일치 11건**:

| # | 원본 위치 | 원본 동작 | 1.21.1 실제 | 분류 |
|---|---|---|---|---|
| 1 | L232 | 진입 조건 `!isFlying && !isLiquidClimbing && (isInWater \|\| (wasSwimming && isInLiquid) \|\| (LavaLikeWaterEnabled && lavaMovement))` | `player.isTouchingWater()` 만 | [오역] |
| 2 | L239 | `useStandard = !isSwimmingEnabled && !isDivingEnabled` 게이트 후 resetSwimming | Config 게이트 없음 | [누락] |
| 3 | L301 | `isCrawling \|\| isClimbCrawling \|\| isCrawlClimbing` → `isDipping=true` | `isCrawling \|\| isCrawlClimbing` (isClimbCrawling 누락) | [누락] |
| 4 | L303-L414 | 3-갈래 (`[0,2]`/`(2,∞)`/`(-∞,0)`) + A/B 서브 분기 + 11-단계 offset 테이블 | 단순 `offset<1.4 / [1.4,1.9) / >=1.9` 3분류 | [오역] |
| 5 | L507 | `isShallowDiveOrSwim = couldStandUp && (isDiving \|\| isSwimming)` 필드 갱신 | **필드 미이식** | [누락] |
| 6 | L487 | `isJumpingOutOfWater` 필드 갱신 | **필드 미이식** | [누락] |
| 7 | L550 | `isStillSwimmingJump = false` (useStandard 경로) | **필드 미이식** | [누락] |
| 8 | L513-L536 | 얕은 물 특수 분기 (isSlow → crawl / else → walking) | 미이식 | [누락] |
| 9 | L481-L484 | `waterMovementTicks++` 은 `swimming \|\| diving` 만, dipping 에서는 `=0` 리셋 | updateSwimState 는 dipping 에서도 증분 | [오역] |
| 10 | L418 | 크롤↔수영 전환 조건 `(isCrawling \|\| isSliding) && playerCrawlWaterBorder < 1.0` | handleSwimming L119 에 `sm.isCrawling && sm.dippingDepth > 0.65` 대응 있으나 `isSliding` 조건 없음 | [누락] |
| 11 | L505 | `isLevitating = levitating` where levitating = `diving && !diveUp && !diveDown && moveStrafing==0 && moveForward==0` | **필드 미이식** | [누락] |

**추가 확인 필요** (미해결):
- `isLiquidClimbing` 1.21.1 대응 여부 (원본 handleSwimming 진입 차단 조건)
- `isInLiquid()` 메서드 1.21.1 대응 (물 외 용암 포함 액체)
- `Config.isLavaLikeWaterEnabled()` 1.21.1 Config 이식 여부
- `Orientation.getClimbingOrientations + isTunnelAhead` 는 1.21.1 handleSwimming L170-L175 에 4방향만 존재 (대각 4방향 누락)

**1.21.1 이식 우선순위 (B-N 원자 분해 예비안)**:
- **B-6**: Swimmer L78 `isClimbCrawling` 조건 추가 (원본 L301 3-OR)
- **B-7**: updateSwimState 진입 조건 `isFlying/isLiquidClimbing/isLavaLikeWater` 복원
- **B-8**: `Config.isSwimmingEnabled()/isDivingEnabled()` 게이트 추가 (Config 헬퍼 신설)
- **B-9**: 메인 분류 공식 원본 L303-L414 로 교체 (A/B 서브 + 11-단계 offset 테이블)
- **B-10**: `isShallowDiveOrSwim` / `isJumpingOutOfWater` / `isStillSwimmingJump` /
  `isLevitating` 필드 ClientState 이식 + 갱신 로직
- **B-11**: 얕은 물 특수 분기 L513-L536 이식 (isSlow 조합 crawl/walking)
- **B-12**: `waterMovementTicks` 증분 조건 정정 (dipping 시 0 리셋)
- **B-13**: 크롤↔수영 전환 `isSliding` 조건 추가
