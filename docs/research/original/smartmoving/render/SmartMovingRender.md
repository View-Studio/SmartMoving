# SmartMovingRender.java (net.smart.moving.render) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/render/SmartMovingRender.java  
패키지: `net.smart.moving.render`  
종류: `class`  
상속: `net.smart.render.SmartRenderContext` (extends)  
실행 위치: 클라이언트 (렌더링)

---

## 전체 소스

소스는 GitHub 링크 참조.

---

## 역할

SmartMoving의 플레이어 렌더링 훅 메서드 모음. SmartMoving 이동 상태를 읽어 `SmartMovingModel` 플래그에 반영하고, 위치/회전 보정 및 커스텀 HUD(소진 바, 점프 차지 바)를 렌더링한다.

`SmartMovingRenderPlayerBase` 또는 `SmartMovingModelPlayerBase`로부터 메서드들이 호출된다.

---

## import

```java
import org.lwjgl.opengl.GL11;

import net.minecraft.block.*;                    // Block
import net.minecraft.block.material.*;           // Material
import net.minecraft.client.*;                   // Minecraft
import net.minecraft.client.entity.*;            // AbstractClientPlayer, EntityOtherPlayerMP, EntityPlayerSP
import net.minecraft.client.gui.*;               // ScaledResolution
import net.minecraft.client.gui.inventory.*;     // GuiInventory
import net.minecraft.entity.*;                   // EntityLivingBase
import net.minecraft.entity.player.*;            // EntityPlayer
import net.minecraft.util.*;                     // ResourceLocation

import net.smart.moving.*;                       // SmartMoving, SmartMovingFactory, SmartMovingSelf
import net.smart.render.statistics.*;            // SmartStatistics, SmartStatisticsFactory
```

---

## 상속 관계

```
SmartRenderContext  (net.smart.render)
    └─ SmartMovingRender  (net.smart.moving.render)
```

`SmartRenderContext`에서 상속받는 것: 각도 상수(Half/Quarter/…), 스케일 타입 상수(Scale/NoScaleStart/NoScaleEnd)

---

## 필드

```java
public static SmartMovingModel CurrentMainModel;  // 현재 렌더 중인 주 바디 모델 (static)

public IRenderPlayer irp;                          // SmartMoving render player 인터페이스

public final SmartMovingModel modelBipedMain;      // 주 바디 SmartMovingModel (final)

private static int _iOffset, _jOffset;             // GUI 아이콘 렌더 좌표
private static Minecraft _minecraft;               // drawIcon()에서 사용
```

**`CurrentMainModel` 사용 패턴**:
- `renderPlayer()` 진입 시 `CurrentMainModel = modelBipedMain` 설정
- `irp.superRenderRenderPlayer(...)` 호출 (SmartMovingModel 생성자에서 CurrentMainModel 읽음)
- 호출 완료 후 `CurrentMainModel = null`로 리셋

---

## 생성자

```java
public SmartMovingRender(IRenderPlayer irp)
{
    this.irp = irp;

    modelBipedMain = irp.getPlayerModelBipedMain().getMovingModel();
    SmartMovingModel modelArmorChestplate = irp.getPlayerModelArmorChestplate().getMovingModel();
    SmartMovingModel modelArmor = irp.getPlayerModelArmor().getMovingModel();

    modelBipedMain.scaleArmType         = Scale;
    modelBipedMain.scaleLegType         = Scale;
    modelArmorChestplate.scaleArmType   = NoScaleStart;
    modelArmorChestplate.scaleLegType   = NoScaleEnd;
    modelArmor.scaleArmType             = NoScaleStart;
    modelArmor.scaleLegType             = Scale;
}
```

**스케일 타입 할당**:

| 모델 | scaleArmType | scaleLegType |
|------|-------------|-------------|
| modelBipedMain (주 바디) | Scale | Scale |
| modelArmorChestplate (흉갑 갑옷) | NoScaleStart | NoScaleEnd |
| modelArmor (나머지 갑옷) | NoScaleStart | Scale |

- `Scale`: scaleY를 직접 변경 (팔·다리가 실제로 늘어남)
- `NoScaleStart`: 스케일 호출 자체를 스킵
- `NoScaleEnd`: scaleY 미변경, offsetY 보정으로 시각적 맞춤

---

## 메서드 상세

### `renderPlayer()`

```java
public void renderPlayer(AbstractClientPlayer entityplayer, double d, double d1, double d2, float f, float renderPartialTicks)
```

**역할**: SM 상태를 읽어 모든 SmartMovingModel에 플래그 반영 후 실제 렌더 위임.

#### 인벤토리 감지

```java
boolean isInventory = d == 0.0F && d1 == 0.0F && d2 == 0.0F && f == 0.0F && renderPartialTicks == 1.0F;
```

d/d1/d2 = 0, f = 0, renderPartialTicks = 1이면 인벤토리 화면 렌더로 판정.

#### SmartMoving 상태 → 로컬 boolean 변환

```java
SmartMoving moving = SmartMovingFactory.getInstance(entityplayer);
if(moving != null)
{
    boolean isClimb = moving.isClimbing && !moving.isCrawling && !moving.isCrawlClimbing && !moving.isClimbJumping;
    boolean isClimbJump = moving.isClimbJumping;
    int handsClimbType = moving.actualHandsClimbType;
    int feetClimbType = moving.actualFeetClimbType;
    boolean isHandsVineClimbing = moving.isHandsVineClimbing;
    boolean isFeetVineClimbing = moving.isFeetVineClimbing;
    boolean isCeilingClimb = moving.isCeilingClimbing;
    boolean isSwim = moving.isSwimming && !moving.isDipping;
    boolean isDive = moving.isDiving;
    boolean isLevitate = moving.isLevitating;
    boolean isCrawl = moving.isCrawling && !moving.isClimbing;
    boolean isCrawlClimb = moving.isCrawlClimbing || (moving.isClimbing && moving.isCrawling);
    boolean isJump = moving.isJumping();
    boolean isHeadJump = moving.isHeadJumping;
    boolean isFlying = moving.doFlyingAnimation();
    boolean isSlide = moving.isSliding;
    boolean isFalling = moving.doFallingAnimation();
    boolean isGenericSneaking = moving.isSlow;
    boolean isAngleJumping = moving.isAngleJumping();
    int angleJumpType = moving.angleJumpType;
    boolean isRopeSliding = moving.isRopeSliding;
```

**중요한 상태 변환 규칙**:
- `isClimb` = `isClimbing && !isCrawling && !isCrawlClimbing && !isClimbJumping` (크롤/점프 제외)
- `isSwim` = `isSwimming && !isDipping` (딥핑 제외)
- `isCrawl` = `isCrawling && !isClimbing` (클라이밍 중 크롤 제외)
- `isCrawlClimb` = `isCrawlClimbing || (isClimbing && isCrawling)` (두 경우 모두 포함)
- `isFlying` = `moving.doFlyingAnimation()` (메서드 호출, 단순 필드 아님)
- `isFalling` = `moving.doFallingAnimation()` (메서드 호출)
- `isAngleJumping` = `moving.isAngleJumping()` (메서드 호출)

#### 통계 / 지형 정보 취득

```java
SmartStatistics statistics = SmartStatisticsFactory.getInstance(entityplayer);
float currentHorizontalSpeedFlattened = statistics != null
    ? statistics.getCurrentHorizontalSpeedFlattened(renderPartialTicks, -1)
    : Float.NaN;

float smallOverGroundHeight = isCrawlClimb || isHeadJump
    ? (float)moving.getOverGroundHeight(5D)
    : 0F;

Block overGroundBlock = isHeadJump && smallOverGroundHeight < 5F
    ? moving.getOverGroundBlockId(smallOverGroundHeight)
    : null;
```

- `currentHorizontalSpeedFlattened`: SmartStatistics에서 보간된 수평 속도. 없으면 NaN (SmartMovingModel에서 NaN이면 파라미터 값 사용)
- `smallOverGroundHeight`: isCrawlClimb 또는 isHeadJump일 때만 계산, 최대 5D 범위
- `overGroundBlock`: isHeadJump이고 smallOverGroundHeight < 5F일 때만 취득

#### 모든 모델에 상태 반영

```java
modelPlayers = irp.getPlayerModels();

for(int i = 0; i < modelPlayers.length; i++)
{
    SmartMovingModel modelPlayer = modelPlayers[i].getMovingModel();
    modelPlayer.isClimb = isClimb;
    modelPlayer.isClimbJump = isClimbJump;
    modelPlayer.handsClimbType = handsClimbType;
    modelPlayer.feetClimbType = feetClimbType;
    modelPlayer.isHandsVineClimbing = isHandsVineClimbing;
    modelPlayer.isFeetVineClimbing = isFeetVineClimbing;
    modelPlayer.isCeilingClimb = isCeilingClimb;
    modelPlayer.isSwim = isSwim;
    modelPlayer.isDive = isDive;
    modelPlayer.isCrawl = isCrawl;
    modelPlayer.isCrawlClimb = isCrawlClimb;
    modelPlayer.isJump = isJump;
    modelPlayer.isHeadJump = isHeadJump;
    modelPlayer.isSlide = isSlide;
    modelPlayer.isFlying = isFlying;
    modelPlayer.isLevitate = isLevitate;
    modelPlayer.isFalling = isFalling;
    modelPlayer.isGenericSneaking = isGenericSneaking;
    modelPlayer.isAngleJumping = isAngleJumping;
    modelPlayer.angleJumpType = angleJumpType;
    modelPlayer.isRopeSliding = isRopeSliding;

    modelPlayer.currentHorizontalSpeedFlattened = currentHorizontalSpeedFlattened;
    modelPlayer.smallOverGroundHeight = smallOverGroundHeight;
    modelPlayer.overGroundBlock = overGroundBlock;
}
```

`irp.getPlayerModels()`: 바디/갑옷 레이어 등 모든 모델 레이어를 반환. 각 레이어의 SmartMovingModel에 동일한 상태를 복사.

#### 크롤 중 타인 플레이어 위치 보정

```java
if (!isInventory && entityplayer.isSneaking() && !(entityplayer instanceof EntityPlayerSP) && isCrawl)
    d1 += 0.125D;
```

조건: 인벤토리 아님 && 스니킹 중 && 로컬 플레이어 아님(타인) && 크롤 중  
Y 오프셋 `+0.125D` (타인 크롤 시 위치 보정).

#### 렌더 실행

```java
CurrentMainModel = modelBipedMain;
irp.superRenderRenderPlayer(entityplayer, d, d1, d2, f, renderPartialTicks);
CurrentMainModel = null;
```

superRenderRenderPlayer 호출 전에 CurrentMainModel 설정 → SmartMovingModel 생성자에서 이 값을 읽어 상태 복사.

#### isLevitate 후처리

```java
if (moving != null && moving.isLevitating && modelPlayers != null)
    for(int i = 0; i < modelPlayers.length; i++)
        modelPlayers[i].getMovingModel().md.currentHorizontalAngle = modelPlayers[i].getMovingModel().md.currentCameraAngle;
```

levitate 상태에서 렌더 완료 후 `currentHorizontalAngle = currentCameraAngle`으로 강제 설정.

---

### `rotatePlayer()`

```java
public void rotatePlayer(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2)
{
    SmartMoving moving = SmartMovingFactory.getInstance(entityplayer);
    if(moving != null)
    {
        boolean isInventory = f2 == 1.0F && moving.isp != null && moving.isp.getMcField().currentScreen instanceof GuiInventory;
        if(!isInventory)
        {
            float forwardRotation = entityplayer.prevRotationYaw + (entityplayer.rotationYaw - entityplayer.prevRotationYaw) * f2;

            if(moving.isClimbing || moving.isClimbCrawling || moving.isCrawlClimbing
                || moving.isFlying || moving.isSwimming || moving.isDiving
                || moving.isCeilingClimbing || moving.isHeadJumping
                || moving.isSliding || moving.isAngleJumping())
                entityplayer.renderYawOffset = forwardRotation;
        }
    }
    irp.superRenderRotatePlayer(entityplayer, totalTime, actualRotation, f2);
}
```

**역할**: 특수 이동 상태에서 `renderYawOffset`을 현재 회전 방향으로 강제 설정.

**인벤토리 감지**: `f2 == 1.0F && isp != null && currentScreen instanceof GuiInventory`

**forwardRotation**: `prevRotationYaw + (rotationYaw - prevRotationYaw) * f2` — 선형 보간

**renderYawOffset 강제 적용 조건** (OR):
- `isClimbing`, `isClimbCrawling`, `isCrawlClimbing`
- `isFlying`, `isSwimming`, `isDiving`
- `isCeilingClimbing`, `isHeadJumping`
- `isSliding`, `isAngleJumping()`

인벤토리 화면이면 적용 안 함.

---

### `renderPlayerAt()`

```java
public void renderPlayerAt(AbstractClientPlayer entityplayer, double d, double d1, double d2)
{
    if(entityplayer instanceof EntityOtherPlayerMP)
    {
        SmartMoving moving = SmartMovingFactory.getOtherSmartMoving(entityplayer.getEntityId());
        if(moving != null && moving.heightOffset != 0)
            d1 += moving.heightOffset;
    }
    irp.superRenderRenderPlayerAt(entityplayer, d, d1, d2);
}
```

**역할**: `EntityOtherPlayerMP`(타인 플레이어)에 대해 `heightOffset`이 있으면 Y 위치 보정.

- `SmartMovingFactory.getOtherSmartMoving(entityId)`: 엔티티 ID로 SM 인스턴스 조회
- `moving.heightOffset != 0`일 때만 d1 보정 (0이면 아무 것도 안 함)
- 로컬 플레이어(`EntityPlayerSP`)는 이 처리 없음

---

### `renderName()`

```java
public void renderName(EntityLivingBase entityPlayer, double d, double d1, double d2)
```

**역할**: 이름 태그 위치 및 스니킹 상태 임시 변경.

```java
boolean changedIsSneaking = false, originalIsSneaking = false;
if(Minecraft.isGuiEnabled() && entityPlayer != irp.getRenderManager().livingPlayer)
{
    SmartMoving moving = entityPlayer instanceof EntityPlayer
        ? SmartMovingFactory.getInstance((EntityPlayer)entityPlayer)
        : null;
    if(moving != null)
    {
        originalIsSneaking = entityPlayer.isSneaking();
        boolean temporaryIsSneaking = originalIsSneaking;

        if(moving.isCrawling && !moving.isClimbing)
            temporaryIsSneaking = !Config._crawlNameTag.value;
        else if(originalIsSneaking)
            temporaryIsSneaking = !Config._sneakNameTag.value;

        changedIsSneaking = temporaryIsSneaking != originalIsSneaking;
        if(changedIsSneaking)
            entityPlayer.setSneaking(temporaryIsSneaking);

        if(moving.heightOffset == -1)
            d1 -= 0.2F;
        else if(originalIsSneaking && !temporaryIsSneaking)
            d1 -= 0.05F;
    }
}

irp.superRenderRenderName(entityPlayer, d, d1, d2);

if(changedIsSneaking)
    entityPlayer.setSneaking(originalIsSneaking);
```

**적용 조건**: GUI 활성화 중 && 렌더 중인 플레이어가 로컬 플레이어 자신이 아님

**스니킹 상태 임시 변경 로직**:

| 상태 | temporaryIsSneaking |
|------|---------------------|
| 크롤 중(클라이밍 아님) | `!Config._crawlNameTag.value` |
| 원래 스니킹 중 | `!Config._sneakNameTag.value` |
| 그 외 | originalIsSneaking 유지 |

- `_crawlNameTag.value = true`: 크롤 중 이름 숨김(isSneaking=true → 이름 표시 안 함) → temporaryIsSneaking = true
- `_crawlNameTag.value = false`: 크롤 중에도 이름 표시 → temporaryIsSneaking = false

**d1 보정**:
- `heightOffset == -1` → `d1 -= 0.2F` (이름 태그 0.2 아래로)
- `originalIsSneaking && !temporaryIsSneaking` → `d1 -= 0.05F` (스니킹 해제 보정)

**복원**: super 호출 후 원래 isSneaking 값으로 복원.

---

### `renderGuiIngame()` (static)

```java
public static void renderGuiIngame(Minecraft minecraft)
```

**역할**: SM 커스텀 HUD — 소진(exhaustion) 바와 점프 차지 바를 화면에 그린다.

#### 조기 반환 조건

```java
if (!Client.getNativeUserInterfaceDrawing())
    return;
```

네이티브 UI 드로잉이 아니면 즉시 반환.

#### SM 인스턴스 취득

```java
SmartMovingSelf moving = (SmartMovingSelf)SmartMovingFactory.getInstance(minecraft.thePlayer);
if(moving != null && Config.enabled && (Options._displayExhaustionBar.value || Options._displayJumpChargeBar.value))
{
    ScaledResolution scaledresolution = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
    int width = scaledresolution.getScaledWidth();
    int height = scaledresolution.getScaledHeight();

    if(minecraft.playerController.shouldDrawHUD())
    {
        // ...
    }
}
```

조건: `Config.enabled` && (`_displayExhaustionBar.value` || `_displayJumpChargeBar.value`)

#### 소진 바 (Exhaustion Bar) 계산

```java
float maxExhaustion = Client.getMaximumExhaustion();
float exhaustion = Math.min(moving.exhaustion, maxExhaustion);
boolean drawExhaustion = exhaustion > 0 && exhaustion <= maxExhaustion;

float maxStillJumpCharge = Config._jumpChargeMaximum.value;
float stillJumpCharge = Math.min(moving.jumpCharge, maxStillJumpCharge);

float maxRunJumpCharge = Config._headJumpChargeMaximum.value;
float runJumpCharge = Math.min(moving.headJumpCharge, maxRunJumpCharge);

boolean drawJumpCharge = stillJumpCharge > 0 || runJumpCharge > 0;
float maxJumpCharge = stillJumpCharge > runJumpCharge ? maxStillJumpCharge : maxRunJumpCharge;
float jumpCharge = Math.max(stillJumpCharge, runJumpCharge);
```

소진 바: `exhaustion > 0 && exhaustion <= maxExhaustion`  
점프 차지 바: `stillJumpCharge > 0 || runJumpCharge > 0`  
두 차지 중 큰 값을 표시: `jumpCharge = Math.max(stillJumpCharge, runJumpCharge)`, 해당 max값도 함께 변경

#### 텍스처 바인딩

```java
if(drawExhaustion || drawJumpCharge)
{
    GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);
    minecraft.getTextureManager().bindTexture(new ResourceLocation("smartmoving", "gui/icons.png"));
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    _minecraft = minecraft;
}
```

텍스처: `smartmoving:gui/icons.png`

#### 소진 바 렌더링 로직

```java
float maxExhaustionForAction = Math.min(moving.maxExhaustionForAction, maxExhaustion);
float maxExhaustionToStartAction = Math.min(moving.maxExhaustionToStartAction, maxExhaustion);

float fitness = maxExhaustion - exhaustion;
float minFitnessForAction = Float.isNaN(maxExhaustionForAction) ? 0 : maxExhaustion - maxExhaustionForAction;
float minFitnessToStartAction = Float.isNaN(maxExhaustionToStartAction) ? 0 : maxExhaustion - maxExhaustionToStartAction;

float maxFitnessDrawn = Math.max(Math.max(minFitnessToStartAction, fitness), minFitnessForAction);

int halfs = (int)Math.floor(maxFitnessDrawn / maxExhaustion * 21F);
int fulls = halfs / 2;
int half = halfs % 2;

int fitnessHalfs = (int)Math.floor(fitness / maxExhaustion * 21F);
int fitnessFulls = fitnessHalfs / 2;
int fitnessHalf = fitnessHalfs % 2;

int minFitnessForActionHalfs = (int)Math.floor(minFitnessForAction / maxExhaustion * 21F);
int minFitnessForActionFulls = minFitnessForActionHalfs / 2;
int minFitnessForActionHalf = minFitnessForActionHalfs % 2;

int minFitnessToStartActionHalfs = (int)Math.floor(minFitnessToStartAction / maxExhaustion * 21F);
int minFitnessToStartActionFulls = minFitnessToStartActionHalfs / 2;
```

개념 정의:
- `fitness` = `maxExhaustion - exhaustion` (체력 여유분, 소진이 낮을수록 높음)
- `minFitnessForAction` = 행동 **유지** 가능 최소 체력
- `minFitnessToStartAction` = 행동 **시작** 가능 최소 체력
- `maxFitnessDrawn` = 그려야 할 최대 범위
- `halfs` = 총 그릴 칸 수 × 2 (반칸 단위), 최대 21 (= 10.5칸)
- 실제 그리는 칸: `Math.min(fulls + half, 10)`

위치:
```java
_jOffset = height - 39 - 10 - (minecraft.thePlayer.isInsideOfMaterial(Material.water) ? 10 : 0);
for(int i = 0; i < Math.min(fulls + half, 10); i++)
{
    _iOffset = (width / 2 + 90) - (i + 1) * 8;  // 오른쪽에서 왼쪽으로
    // drawIcon(x, y) 호출
}
```

Y: 화면 하단에서 39-10 = 49픽셀 위. 물속이면 추가로 10픽셀 위.  
X: 화면 중앙+90에서 왼쪽 방향으로 8픽셀씩.

**drawIcon(x, y) 선택 로직** (소진 바 각 칸):

```java
if(i < fitnessFulls)
{
    if(i < minFitnessForActionFulls)
        drawIcon(2, 2);
    else if(i == minFitnessForActionFulls && minFitnessForActionHalf > 0)
        drawIcon(3, 2);
    else
        drawIcon(0, 0);
}
else if(i == fitnessFulls && fitnessHalf > 0)
{
    if(i < minFitnessForActionFulls)
        drawIcon(1, 2);
    else if(i == minFitnessForActionFulls && minFitnessForActionHalf > 0)
        if(i < minFitnessToStartActionFulls)
            drawIcon(3, 1);
        else
            drawIcon(4, 2);
    else
        if(i < minFitnessToStartActionFulls)
            drawIcon(1, 1);
        else
            drawIcon(1, 0);
}
else
{
    if(i < minFitnessForActionFulls)
        drawIcon(0, 2);
    else if(i == minFitnessForActionFulls && minFitnessForActionHalf > 0)
        if(i < minFitnessToStartActionFulls)
            drawIcon(2, 1);
        else
            drawIcon(5, 2);
    else
        if(i < minFitnessToStartActionFulls)
            drawIcon(0, 1);
        else
            drawIcon(4, 1);
}
```

아이콘 스프라이트: `icons.png`의 9×9픽셀 격자, `drawIcon(x, y)` → 좌상단 `(x*9, y*9)`

#### 점프 차지 바 렌더링 로직

```java
boolean max = jumpCharge == maxJumpCharge;
int fulls = max ? 10 : (int)Math.ceil(((jumpCharge - 2) * 10D) / maxJumpCharge);
int half  = max ? 0  : (int)Math.ceil((jumpCharge * 10D) / maxJumpCharge) - fulls;

_jOffset = height - 39 - 10 - (minecraft.thePlayer.getTotalArmorValue() > 0 ? 10 : 0);
for(int i = 0; i < fulls + half; i++)
{
    _iOffset = (width / 2 - 91) + i * 8;  // 왼쪽에서 오른쪽으로
    drawIcon(i < fulls ? 2 : 3, 0);
}
```

- 최대 충전(`max`): fulls=10, half=0 — 10칸 전체 꽉 찬 아이콘
- 그 외: `fulls = ceil((charge-2) * 10 / max)`, `half = ceil(charge * 10 / max) - fulls`
- 아이콘: fulls 칸은 `drawIcon(2, 0)`, half 칸은 `drawIcon(3, 0)`

Y: 화면 하단에서 49픽셀 위. 갑옷이 있으면(getTotalArmorValue() > 0) 추가로 10픽셀 위.  
X: 화면 중앙-91에서 오른쪽 방향으로 8픽셀씩.

---

### `drawIcon()` (static, private)

```java
private static void drawIcon(int x, int y)
{
    _minecraft.ingameGUI.drawTexturedModalRect(_iOffset, _jOffset, x * 9, y * 9, 9, 9);
}
```

`icons.png`에서 `(x*9, y*9)` 위치의 9×9 스프라이트를 `(_iOffset, _jOffset)`에 렌더링.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartRenderContext` | 상속 — Scale/NoScaleStart/NoScaleEnd 상수 |
| `IRenderPlayer` (irp) | 모델/렌더 메서드 접근 인터페이스 |
| `SmartMovingModel` | 상태 플래그 설정 대상 |
| `SmartMovingFactory.getInstance()` | SmartMoving 인스턴스 조회 |
| `SmartMovingFactory.getOtherSmartMoving()` | 타인 플레이어 SM 인스턴스 조회 |
| `SmartStatisticsFactory.getInstance()` | 속도 통계 조회 |
| `SmartStatistics.getCurrentHorizontalSpeedFlattened()` | 보간된 수평 속도 |
| `SmartMovingSelf` | HUD 렌더 대상 (exhaustion, jumpCharge 등) |
| `Config._crawlNameTag` / `Config._sneakNameTag` | 이름 태그 표시 설정 |
| `Config.enabled` / `Config._jumpChargeMaximum` / `Config._headJumpChargeMaximum` | HUD 설정 |
| `Options._displayExhaustionBar` / `Options._displayJumpChargeBar` | HUD 표시 여부 |
| `Client.getNativeUserInterfaceDrawing()` / `Client.getMaximumExhaustion()` | 클라이언트 유틸리티 |
| `GL11` | OpenGL 텍스처/속성 조작 |
| `ResourceLocation("smartmoving", "gui/icons.png")` | HUD 아이콘 텍스처 |

---

## 주요 관찰 사항

1. **`CurrentMainModel` 생명주기**: `renderPlayer()` 시작 시 설정 → `superRenderRenderPlayer()` 내부에서 SmartMovingModel 생성자가 읽음 → 완료 후 null 리셋. 렌더 중에만 유효하다.

2. **상태 변환 규칙**: `SmartMoving` 필드를 SmartMovingModel 플래그로 변환 시 조건 조합이 중요. 특히 `isClimb`는 4개 조건(crawling/crawlClimbing/climbJumping 모두 아님), `isCrawlClimb`는 2가지 경우를 OR.

3. **플레이어 구분**: 타인 플레이어(`EntityOtherPlayerMP`) vs 로컬 플레이어(`EntityPlayerSP`) — renderPlayerAt은 타인만 처리, renderName은 로컬 자신 제외, renderPlayer의 위치 보정은 타인+스니킹+크롤.

4. **HUD 레이아웃**:
   - 소진 바: 화면 오른쪽, 오른쪽→왼쪽 방향, 최대 10칸
   - 점프 차지 바: 화면 왼쪽, 왼쪽→오른쪽 방향
   - 물속/갑옷 여부에 따라 Y 위치 10픽셀씩 상향

5. **GL11 사용**: `glPushAttrib(GL_TEXTURE_BIT)` + `glColor4f(1,1,1,1)` 패턴 — 텍스처 상태 보존 후 복원

6. **`isInventory` 감지 두 가지 방식**:
   - `renderPlayer()`: `d==0 && d1==0 && d2==0 && f==0 && renderPartialTicks==1`
   - `rotatePlayer()`: `f2==1 && currentScreen instanceof GuiInventory`

7. **1.21.1 이식 관련**:
   - `GL11` 직접 사용 → Fabric에서 RenderSystem API로 교체
   - `GuiInventory` → `InventoryScreen` (1.21.1 클래스명 변경)
   - `ScaledResolution` → `MinecraftClient.getInstance().getWindow()` 기반
   - `drawTexturedModalRect` → `DrawContext.drawTexture()`
   - `EntityOtherPlayerMP` → `OtherClientPlayerEntity`
   - `renderYawOffset` 직접 설정 → Mixin으로 접근 필요
   - `SmartMovingFactory.getOtherSmartMoving()` → 타인 플레이어 SM 인스턴스 관리 방식 재설계 필요
   - `moving.isp.getMcField().currentScreen` → `MinecraftClient.getInstance().currentScreen`
