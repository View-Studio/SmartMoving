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

### 스닉/크롤 토글 (2968-3041줄)

sneakToggled, crawlToggled 상태 전환. ignoreNextStopSneakButtonPressed로 오입력 방지.

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
