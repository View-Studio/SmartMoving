# SmartRenderRender.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/SmartRenderRender.java  
패키지: `net.smart.render`  
상속: `SmartRenderRender extends SmartRenderContext`

---

## 역할

클라이언트 전용 플레이어 렌더러 클래스.  
`renderPlayer`, `rotatePlayer`, `renderSpecials`, `drawFirstPersonHand` 등 vanilla 렌더러 메서드를 가로채서 SmartRender 로직을 삽입한다.  
`SmartStatistics`에서 이동 데이터를 수집하여 모든 모델 레이어에 전달하고, `CurrentMainModel` 전역 변수를 통해 SmartRenderModel 생성자에 컨텍스트를 제공한다.

- **실행 위치**: 클라이언트 전용

---

## 필드

```java
public static SmartRenderModel CurrentMainModel;
// 현재 superRenderPlayer() 호출 중에 유효한 메인 모델 레퍼런스.
// SmartRenderModel 생성자에서 이 값을 읽어 상태를 복사한다.
// superRenderPlayer() 호출 전 설정, 호출 후 null로 초기화.

public IRenderPlayer irp;
// PlayerAPI 래퍼 인터페이스 — super 메서드 호출 및 모델 생성에 사용

public final SmartRenderModel modelBipedMain;
// 메인(스킨) 모델

private static Map<EntityPlayer, RendererData> previousRendererData = new HashMap<>();
// 플레이어별 이전 프레임 bipedOuter 회전 데이터 저장 맵

private static int previousRendererDataAccessCounter = 0;
// 1000번마다 맵 정리 트리거용 카운터
```

---

## 생성자

```java
public SmartRenderRender(IRenderPlayer irp)
{
    this.irp = irp;

    modelBipedMain = irp.createModel(irp.getModelBipedMain(), 0.0F).getRenderModel();
    SmartRenderModel modelArmorChestplate = irp.createModel(irp.getModelArmorChestplate(), 1.0F).getRenderModel();
    SmartRenderModel modelArmor = irp.createModel(irp.getModelArmor(), 0.5F).getRenderModel();

    irp.initialize(modelBipedMain.mp, modelArmorChestplate.mp, modelArmor.mp, 0.5F);
}
```

- 총 3개 모델 생성: 메인(0.0F), 갑옷흉갑(1.0F), 갑옷(0.5F) — 파라미터는 레이어 두께(factor)
- `irp.initialize()`로 vanilla ModelBiped 3개 + factor를 PlayerAPI에 전달

---

## 메서드 전체

### `renderPlayer(AbstractClientPlayer, double d, double d1, double d2, float f, float renderPartialTicks)`

```java
public void renderPlayer(AbstractClientPlayer entityplayer, double d, double d1, double d2,
                          float f, float renderPartialTicks)
{
    SmartStatistics statistics = SmartStatisticsFactory.getInstance(entityplayer);
    if(statistics != null)
    {
        // 인벤토리 화면 감지
        boolean isInventory = d == 0.0F && d1 == 0.0F && d2 == 0.0F && f == 0.0F && renderPartialTicks == 1.0F;
        boolean isSleeping = entityplayer.isPlayerSleeping();

        float totalVerticalDistance = statistics.getTotalVerticalDistance(renderPartialTicks);
        float currentVerticalSpeed  = statistics.getCurrentVerticalSpeed(renderPartialTicks);
        float totalDistance         = statistics.getTotalDistance(renderPartialTicks);
        float currentSpeed          = statistics.getCurrentSpeed(renderPartialTicks);

        double distance = 0, verticalDistance = 0, horizontalDistance = 0;
        float currentCameraAngle = 0, currentVerticalAngle = 0, currentHorizontalAngle = 0;

        if(!isInventory)
        {
            double xDiff = entityplayer.posX - entityplayer.prevPosX;
            double yDiff = entityplayer.posY - entityplayer.prevPosY;
            double zDiff = entityplayer.posZ - entityplayer.prevPosZ;

            verticalDistance   = Math.abs(yDiff);
            horizontalDistance = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
            distance           = Math.sqrt(horizontalDistance * horizontalDistance + verticalDistance * verticalDistance);

            currentCameraAngle    = entityplayer.rotationYaw / RadiantToAngle;
            currentVerticalAngle  = (float)Math.atan(yDiff / horizontalDistance);
            if(Float.isNaN(currentVerticalAngle))
                currentVerticalAngle = Quarter;   // horizontalDistance==0일 때

            currentHorizontalAngle = (float)-Math.atan(xDiff / zDiff);
            if(Float.isNaN(currentHorizontalAngle))
            {
                if(Float.isNaN(statistics.prevHorizontalAngle))
                    currentHorizontalAngle = currentCameraAngle;
                else
                    currentHorizontalAngle = statistics.prevHorizontalAngle;
            }
            else if(zDiff < 0)
                currentHorizontalAngle += Half;   // zDiff < 0이면 +Half

            statistics.prevHorizontalAngle = currentHorizontalAngle;
        }

        // 모든 모델 레이어에 데이터 세팅
        IModelPlayer[] modelPlayers = irp.getRenderModels();
        for(int i = 0; i < modelPlayers.length; i++)
        {
            SmartRenderModel modelPlayer = modelPlayers[i].getRenderModel();

            modelPlayer.isInventory           = isInventory;
            modelPlayer.totalVerticalDistance = totalVerticalDistance;
            modelPlayer.currentVerticalSpeed  = currentVerticalSpeed;
            modelPlayer.totalDistance         = totalDistance;
            modelPlayer.currentSpeed          = currentSpeed;
            modelPlayer.distance              = distance;
            modelPlayer.verticalDistance      = verticalDistance;
            modelPlayer.horizontalDistance    = horizontalDistance;
            modelPlayer.currentCameraAngle    = currentCameraAngle;
            modelPlayer.currentVerticalAngle  = currentVerticalAngle;
            modelPlayer.currentHorizontalAngle = currentHorizontalAngle;
            modelPlayer.prevOuterRenderData   = getPreviousRendererData(entityplayer);
            modelPlayer.isSleeping            = isSleeping;
        }
    }

    CurrentMainModel = modelBipedMain;
    irp.superRenderPlayer(entityplayer, d, d1, d2, f, renderPartialTicks);
    CurrentMainModel = null;
}
```

**인벤토리 감지 조건:** `d == 0 && d1 == 0 && d2 == 0 && f == 0 && renderPartialTicks == 1.0F`

**각도 계산 상세:**
- `currentVerticalAngle = atan(yDiff / horizontalDistance)` — NaN이면 `Quarter` 대입
- `currentHorizontalAngle = -atan(xDiff / zDiff)` — NaN이면 prevHorizontalAngle(또는 cameraAngle) 사용, zDiff<0이면 `+Half`

**CurrentMainModel 패턴:**
- `superRenderPlayer()` 호출 직전에 `CurrentMainModel = modelBipedMain` 설정
- 호출 중에 SmartRenderModel 생성자가 이 값을 읽어 상태 복사
- 호출 후 즉시 `null` 복원

---

### `drawFirstPersonHand(EntityPlayer entityPlayer)`

```java
public void drawFirstPersonHand(EntityPlayer entityPlayer)
{
    modelBipedMain.firstPerson = true;
    irp.superDrawFirstPersonHand(entityPlayer);
    modelBipedMain.firstPerson = false;
}
```

- 1인칭 손 렌더링 전후로 `modelBipedMain.firstPerson` 플래그를 토글
- SmartRenderModel.setRotationAngles()에서 firstPerson이면 조기 리턴하여 SmartRender 애니메이션 스킵

---

### `rotatePlayer(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2)`

```java
public void rotatePlayer(AbstractClientPlayer entityplayer, float totalTime,
                          float actualRotation, float f2)
{
    boolean isLocal = entityplayer instanceof EntityPlayerSP;
    boolean isInventory = f2 == 1.0F && isLocal
        && Minecraft.getMinecraft().currentScreen instanceof GuiInventory;

    if(!isInventory)
    {
        float forwardRotation = entityplayer.prevRotationYaw
            + (entityplayer.rotationYaw - entityplayer.prevRotationYaw) * f2;

        if(entityplayer.isPlayerSleeping())
        {
            actualRotation = 0;
            forwardRotation = 0;
        }

        float workingAngle;
        Minecraft minecraft = Minecraft.getMinecraft();
        if(!isLocal)
        {
            workingAngle = -entityplayer.rotationYaw;
            workingAngle += minecraft.renderViewEntity.rotationYaw;
        }
        else
            workingAngle = actualRotation
                - getPreviousRendererData(entityplayer).rotateAngleY * RadiantToAngle;

        if(minecraft.gameSettings.thirdPersonView == 2
            && !minecraft.renderViewEntity.isPlayerSleeping())
            workingAngle += 180F;

        IModelPlayer[] modelPlayers = irp.getRenderModels();
        for(int i = 0; i < modelPlayers.length; i++)
        {
            SmartRenderModel modelPlayer = modelPlayers[i].getRenderModel();
            modelPlayer.actualRotation  = actualRotation;
            modelPlayer.forwardRotation = forwardRotation;
            modelPlayer.workingAngle    = workingAngle;
        }

        actualRotation = 0;   // vanilla에 0을 전달 (SmartRender가 회전을 직접 처리)
    }

    irp.superRotatePlayer(entityplayer, totalTime, actualRotation, f2);
}
```

**인벤토리 감지 조건:** `f2 == 1.0F && isLocal && currentScreen instanceof GuiInventory`

**workingAngle 계산:**
- 원격 플레이어: `-entityplayer.rotationYaw + renderViewEntity.rotationYaw`
- 로컬 플레이어: `actualRotation - prevOuterRenderData.rotateAngleY * RadiantToAngle`
- thirdPersonView==2이고 renderViewEntity가 수면 중이 아니면: `workingAngle += 180F`

**핵심:** `actualRotation = 0`으로 vanilla에 넘겨서 vanilla의 회전을 억제하고, SmartRender가 bipedOuter.rotateAngleY로 직접 회전을 처리.

**isInventory이면:** 이 블록 전체 스킵 → actualRotation 원래 값 그대로 superRotatePlayer에 전달

---

### `renderSpecials(AbstractClientPlayer entityplayer, float f)`

```java
public void renderSpecials(AbstractClientPlayer entityplayer, float f)
{
    modelBipedMain.bipedEars.beforeRender();
    modelBipedMain.bipedCloak.beforeRender(entityplayer, f);
    irp.superRenderSpecials(entityplayer, f);
    modelBipedMain.bipedCloak.afterRender();
    modelBipedMain.bipedEars.afterRender();
}
```

- 귀(Ears), 망토(Cloak) 렌더 전후로 before/afterRender 훅 실행
- 실제 렌더는 superRenderSpecials 내부에서 발생

---

### `beforeHandleRotationFloat(EntityLivingBase entityliving, float f)`

```java
@SuppressWarnings({ "static-method", "unused" })
public void beforeHandleRotationFloat(EntityLivingBase entityliving, float f)
{
    if(entityliving instanceof EntityPlayer)
    {
        SmartStatistics statistics = SmartStatisticsFactory.getInstance((EntityPlayer)entityliving);
        if(statistics != null)
            entityliving.ticksExisted += statistics.ticksRiding;
    }
}
```

---

### `afterHandleRotationFloat(EntityLivingBase entityliving, float f)`

```java
@SuppressWarnings({ "static-method", "unused" })
public void afterHandleRotationFloat(EntityLivingBase entityliving, float f)
{
    if(entityliving instanceof EntityPlayer)
    {
        SmartStatistics statistics = SmartStatisticsFactory.getInstance((EntityPlayer)entityliving);
        if(statistics != null)
            entityliving.ticksExisted -= statistics.ticksRiding;
    }
}
```

- before: `ticksExisted += ticksRiding`
- after: `ticksExisted -= ticksRiding`
- 목적: `handleRotationFloat()` 호출 중에만 ticksExisted에 탑승 틱을 임시로 더해서 회전 계산에 영향을 줌. 호출 후 즉시 복원.

---

### `getPreviousRendererData(EntityPlayer entityplayer)` — static

```java
public static RendererData getPreviousRendererData(EntityPlayer entityplayer)
{
    if(++previousRendererDataAccessCounter > 1000)
    {
        List<?> players = Minecraft.getMinecraft().theWorld.playerEntities;
        Iterator<EntityPlayer> iterator = previousRendererData.keySet().iterator();
        while(iterator.hasNext())
            if(!players.contains(iterator.next()))
                iterator.remove();
        previousRendererDataAccessCounter = 0;
    }

    RendererData result = previousRendererData.get(entityplayer);
    if(result == null)
        previousRendererData.put(entityplayer, result = new RendererData());
    return result;
}
```

- 접근 횟수가 1000을 넘으면 월드에 없는 플레이어의 RendererData를 맵에서 제거 (메모리 누수 방지)
- entityplayer에 해당하는 RendererData 반환 (없으면 new RendererData() 생성 후 저장)

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartRenderContext` | 상위 클래스 (RadiantToAngle, Quarter, Half 등 상수) |
| `IRenderPlayer` (irp) | createModel, getModelBipedMain/Chestplate/Armor, initialize, superRenderPlayer, superDrawFirstPersonHand, superRotatePlayer, superRenderSpecials, getRenderModels 호출 |
| `SmartRenderModel` | 모든 모델 레이어 타입 |
| `SmartStatistics` / `SmartStatisticsFactory` | 이동 거리/속도 데이터 조회 |
| `RendererData` | 플레이어별 이전 프레임 bipedOuter 회전 저장 |
| `IModelPlayer` | irp.getRenderModels() 반환 타입 |
| `Minecraft` | thirdPersonView, renderViewEntity, currentScreen, theWorld.playerEntities |

---

## 상수 (SmartRenderContext 상속, 실제 값은 SmartRenderContext 리서치 후 확인 필요)

| 이름 | 사용 위치 |
|------|-----------|
| `RadiantToAngle` | currentCameraAngle 계산, workingAngle 계산 |
| `Quarter` | currentVerticalAngle NaN 대체값 |
| `Half` | zDiff<0일 때 currentHorizontalAngle += Half |

모두 [미확인 — SmartRenderContext 읽기 전]

---

## 주요 관찰 사항

1. **CurrentMainModel 전역 패턴**: `superRenderPlayer()` 호출 중에만 `CurrentMainModel`이 유효. SmartRenderModel 생성자가 이 값을 읽어 상태를 복사하는 구조 — 렌더러가 모델 생성 타이밍을 제어함.

2. **actualRotation=0 전달**: `rotatePlayer()`에서 vanilla에 actualRotation=0을 넘겨 vanilla 회전을 억제. SmartRender가 `bipedOuter.rotateAngleY`로 직접 회전 처리.

3. **isInventory 이중 감지**: `renderPlayer()`와 `rotatePlayer()`에서 각각 다른 조건으로 인벤토리를 감지.
   - renderPlayer: `d==0 && d1==0 && d2==0 && f==0 && renderPartialTicks==1`
   - rotatePlayer: `f2==1.0F && isLocal && currentScreen instanceof GuiInventory`

4. **sleeping 처리**: rotatePlayer에서 sleeping이면 actualRotation=0, forwardRotation=0 강제 설정.

5. **ticksRiding 임시 적용**: `handleRotationFloat()` 전후로 ticksExisted를 조작. 탑승 틱 수가 회전 계산에 반영되도록 하는 우회 방법.

6. **모든 모델 레이어에 동일 데이터**: `irp.getRenderModels()`로 얻은 모든 레이어(메인 + 갑옷 2종)에 같은 이동 데이터를 세팅.
