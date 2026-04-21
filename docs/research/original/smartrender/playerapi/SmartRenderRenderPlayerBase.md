# SmartRenderRenderPlayerBase.java (net.smart.render.playerapi) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/playerapi/SmartRenderRenderPlayerBase.java  
패키지: `net.smart.render.playerapi`  
종류: `class`  
상속/구현: `SmartRenderRenderPlayerBase extends RenderPlayerBase implements IRenderPlayer`

---

## 역할

PlayerAPI의 `RenderPlayerBase`를 구현하여, vanilla `RenderPlayer`의 모든 렌더 메서드를 `SmartRenderRender`로 위임(delegate)하는 어댑터 클래스.  
PlayerAPI 경로에서 `IRenderPlayer` 인터페이스를 구현하는 실체이며, `SmartRenderModelPlayerBase`와 대칭 구조를 이룬다.

- **실행 위치**: 클라이언트 전용

---

## 필드

```java
private api.player.model.ModelPlayer[] allModelPlayers;   // getRenderModels() 캐시 — 이전 ModelPlayerAPI.getAllInstances() 반환값
private IModelPlayer[] allIModelPlayers;                  // getRenderModels() 캐시 — allModelPlayers에 대응하는 IModelPlayer[] 배열
private SmartRenderRender render;                         // lazy init. getRenderRender() 최초 호출 시 생성
```

세 필드 모두 클래스 맨 아래(메서드 정의 이후)에 선언됨. Java에서 선언 위치는 동작에 영향 없음.

---

## 생성자

```java
public SmartRenderRenderPlayerBase(RenderPlayerAPI renderPlayerAPI)
{
    super(renderPlayerAPI);
}
```

- `RenderPlayerBase(RenderPlayerAPI)` 생성자 호출
- `super()`에서 `renderPlayerAPI` 필드가 초기화됨 (PlayerAPI 제공)

---

## 메서드 전체

### `getRenderRender()` — lazy init

```java
public SmartRenderRender getRenderRender()
{
    if (render == null)
        render = new SmartRenderRender(this);
    return render;
}
```

- `this`(IRenderPlayer 구현체)를 SmartRenderRender 생성자에 전달
- `SmartRenderModelPlayerBase.getRenderModel()`과 동일한 lazy init 패턴

---

### `createModel(ModelBiped existing, float f)` — override

```java
@Override
public IModelPlayer createModel(ModelBiped existing, float f)
{
    return SmartRender.getPlayerBase((api.player.model.ModelPlayer)existing);
}
```

- `existing`(vanilla `ModelBiped`)을 `api.player.model.ModelPlayer`(PlayerAPI ModelPlayer)로 강제 캐스트
- `SmartRender.getPlayerBase(api.player.model.ModelPlayer)`로 `SmartRenderModelPlayerBase` 인스턴스 조회
- **전제조건**: PlayerAPI가 이미 설치되어 있고 `ModelPlayer`가 `ModelBiped`를 상속하는 상태. 그렇지 않으면 ClassCastException 발생
- `float f` 파라미터는 사용하지 않음

---

### `initialize(ModelBiped modelBipedMain, ModelBiped modelArmorChestplate, ModelBiped modelArmor, float shadowSize)` — override

```java
@Override
public void initialize(ModelBiped modelBipedMain, ModelBiped modelArmorChestplate, ModelBiped modelArmor, float shadowSize)
{
    renderPlayerAPI.setMainModelField(modelBipedMain);
    renderPlayerAPI.setShadowSizeField(0.5F);

    renderPlayerAPI.setModelBipedMainField(modelBipedMain);
    renderPlayerAPI.setModelArmorChestplateField(modelArmorChestplate);
    renderPlayerAPI.setModelArmorField(modelArmor);
}
```

- **`shadowSize` 파라미터 무시**: `renderPlayerAPI.setShadowSizeField(0.5F)` — 전달받은 `shadowSize` 대신 `0.5F` 하드코딩
- 설정하는 5개 필드:
  1. `mainModel` (RenderLivingBase의 필드) ← `modelBipedMain`
  2. `shadowSize` ← `0.5F` (하드코딩)
  3. `modelBipedMain` ← `modelBipedMain`
  4. `modelArmorChestplate` ← `modelArmorChestplate`
  5. `modelArmor` ← `modelArmor`
- `IRenderPlayer.initialize()` 구현

---

### `renderPlayer(...)` — override

```java
@Override
public void renderPlayer(AbstractClientPlayer entityplayer, double d, double d1, double d2, float f, float renderPartialTicks)
{
    getRenderRender().renderPlayer(entityplayer, d, d1, d2, f, renderPartialTicks);
}
```

---

### `superRenderPlayer(...)` — override

```java
@Override
public void superRenderPlayer(AbstractClientPlayer entityplayer, double d, double d1, double d2, float f, float renderPartialTicks)
{
    super.renderPlayer(entityplayer, d, d1, d2, f, renderPartialTicks);
}
```

- `RenderPlayerBase.renderPlayer()` (vanilla 체인) 호출

---

### `renderFirstPersonArm(EntityPlayer entityPlayer)` — override

```java
@Override
public void renderFirstPersonArm(EntityPlayer entityPlayer)
{
    getRenderRender().drawFirstPersonHand(entityPlayer);
}
```

- PlayerAPI 메서드명: `renderFirstPersonArm`
- SmartRenderRender 메서드명: `drawFirstPersonHand` — **이름 불일치**

---

### `superDrawFirstPersonHand(EntityPlayer entityPlayer)` — override

```java
@Override
public void superDrawFirstPersonHand(EntityPlayer entityPlayer)
{
    super.renderFirstPersonArm(entityPlayer);
}
```

- IRenderPlayer 인터페이스 메서드명: `superDrawFirstPersonHand`
- 실제 호출: `super.renderFirstPersonArm()` (PlayerAPI RenderPlayerBase의 vanilla 래퍼)
- **이름 불일치**: `superDrawFirstPersonHand` (IRenderPlayer) ↔ `renderFirstPersonArm` (PlayerAPI/vanilla)

---

### `rotatePlayer(AbstractClientPlayer, float, float, float)` — override

```java
@Override
public void rotatePlayer(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2)
{
    getRenderRender().rotatePlayer(entityplayer, totalTime, actualRotation, f2);
}
```

---

### `superRotatePlayer(AbstractClientPlayer, float, float, float)` — override

```java
@Override
public void superRotatePlayer(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2)
{
    super.rotatePlayer(entityplayer, totalTime, actualRotation, f2);
}
```

---

### `renderSpecials(AbstractClientPlayer, float)` — override

```java
@Override
public void renderSpecials(AbstractClientPlayer entityplayer, float f)
{
    getRenderRender().renderSpecials(entityplayer, f);
}
```

---

### `superRenderSpecials(AbstractClientPlayer, float)` — override

```java
@Override
public void superRenderSpecials(AbstractClientPlayer entityplayer, float f)
{
    super.renderSpecials(entityplayer, f);
}
```

---

### `beforeHandleRotationFloat(EntityLivingBase, float)` — override

```java
@Override
public void beforeHandleRotationFloat(EntityLivingBase entityliving, float f)
{
    getRenderRender().beforeHandleRotationFloat(entityliving, f);
}
```

---

### `afterHandleRotationFloat(EntityLivingBase, float)` — override

```java
@Override
public void afterHandleRotationFloat(EntityLivingBase entityliving, float f)
{
    getRenderRender().afterHandleRotationFloat(entityliving, f);
}
```

---

### getter 메서드 4개

```java
@Override
public RenderManager getRenderManager()
{
    return renderPlayerAPI.getRenderManagerField();
}

@Override
public ModelBiped getModelBipedMain()
{
    return renderPlayerAPI.getModelBipedMainField();
}

@Override
public ModelBiped getModelArmorChestplate()
{
    return renderPlayerAPI.getModelArmorChestplateField();
}

@Override
public ModelBiped getModelArmor()
{
    return renderPlayerAPI.getModelArmorField();
}
```

- 모두 `renderPlayerAPI`의 field getter를 통해 PlayerAPI가 관리하는 필드에 접근

---

### `getRenderModels()` — override

```java
@Override
public IModelPlayer[] getRenderModels()
{
    api.player.model.ModelPlayer[] modelPlayers = api.player.model.ModelPlayerAPI.getAllInstances();
    if(allModelPlayers != null
        && (allModelPlayers == modelPlayers
            || modelPlayers.length == 0 && allModelPlayers.length == 0))
        return allIModelPlayers;

    allModelPlayers = modelPlayers;
    allIModelPlayers = new IModelPlayer[modelPlayers.length];
    for(int i = 0; i < allIModelPlayers.length; i++)
        allIModelPlayers[i] = SmartRender.getPlayerBase(allModelPlayers[i]);
    return allIModelPlayers;
}
```

**동작 단계:**
1. `ModelPlayerAPI.getAllInstances()` — 현재 등록된 모든 `api.player.model.ModelPlayer` 인스턴스 배열 조회
2. **캐시 유효성 검사**:
   - `allModelPlayers != null` — 캐시가 초기화된 상태
   - AND (`allModelPlayers == modelPlayers` — **참조 동등성** (같은 배열 객체) OR `modelPlayers.length == 0 && allModelPlayers.length == 0` — 둘 다 빈 배열)
   - 조건 만족 시: 캐시된 `allIModelPlayers` 반환
3. 캐시 미스 시: `allModelPlayers = modelPlayers`, 새 `IModelPlayer[]` 배열 할당
4. 각 `ModelPlayer`에 대해 `SmartRender.getPlayerBase(allModelPlayers[i])` 호출하여 대응하는 `SmartRenderModelPlayerBase` 조회
5. `allIModelPlayers` 반환

**캐시 무효화 조건**: `ModelPlayerAPI.getAllInstances()`가 이전과 다른 배열 객체를 반환할 때 (= PlayerAPI 내부에서 인스턴스 목록이 변경된 경우).

---

## import

```java
import net.minecraft.client.entity.*;          // AbstractClientPlayer
import net.minecraft.client.model.*;           // ModelBiped
import net.minecraft.client.renderer.entity.*; // RenderManager
import net.minecraft.entity.*;                 // Entity
import net.minecraft.entity.player.*;          // EntityPlayer, EntityLivingBase
import api.player.render.*;                    // RenderPlayerBase, RenderPlayerAPI
import net.smart.render.*;                     // SmartRender, SmartRenderRender
import net.smart.render.IRenderPlayer;
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `RenderPlayerBase` (PlayerAPI) | 상위 클래스. `renderPlayerAPI` 필드 제공 |
| `IRenderPlayer` | 구현 인터페이스 (메서드 시그니처 정의) |
| `SmartRenderRender` | 실제 렌더 로직 보유. 대부분의 메서드가 이쪽으로 위임 |
| `SmartRender` (playerapi) | `getPlayerBase()` — ModelPlayer→IModelPlayer 조회 |
| `RenderPlayerAPI` (PlayerAPI) | `renderPlayerAPI` 필드 via setter/getter |
| `ModelPlayerAPI` (PlayerAPI) | `getAllInstances()` — 등록된 ModelPlayer 목록 |

---

## 주요 관찰 사항

1. **SmartRenderModelPlayerBase와 대칭 구조**: `getRenderRender()` vs `getRenderModel()`, `initialize()` 콜백, `createModel()` vs 없음. 렌더 경로(RenderPlayer)와 모델 경로(ModelPlayer) 각각을 PlayerAPI에 연결하는 두 개의 어댑터.

2. **`shadowSize` 파라미터 무시**: `initialize(modelBipedMain, modelArmorChestplate, modelArmor, shadowSize)`에서 `shadowSize`를 쓰지 않고 `0.5F` 하드코딩. vanilla RenderPlayer의 기본 shadowSize가 0.5F이므로 의도적 고정.

3. **메서드명 불일치 — `renderFirstPersonArm` / `drawFirstPersonHand`**:
   - PlayerAPI 메서드: `renderFirstPersonArm(EntityPlayer)`
   - IRenderPlayer 인터페이스: `superDrawFirstPersonHand(EntityPlayer)`, `renderFirstPersonArm(EntityPlayer)`
   - SmartRenderRender 메서드: `drawFirstPersonHand(EntityPlayer)`
   - 세 레이어가 각자 다른 이름을 사용하지만 모두 같은 기능을 가리킴.

4. **`getRenderModels()` 참조 동등성 캐시**: `allModelPlayers == modelPlayers`는 값이 아닌 참조 비교. PlayerAPI가 `getAllInstances()`에서 매번 같은 배열 객체를 반환하는 동안은 캐시가 유지됨. 플레이어 접속/이탈로 배열이 교체되면 캐시 무효화.

5. **`createModel()`의 캐스트**: `(api.player.model.ModelPlayer)existing` — PlayerAPI가 vanilla `ModelPlayer`(= `ModelBiped`)를 자신의 `api.player.model.ModelPlayer` 서브클래스로 교체했다는 전제가 필요. PlayerAPI 없이 이 코드가 실행되면 ClassCastException.

6. **1.21.1 이식**: PlayerAPI가 없으므로 이 클래스 전체 불필요. `SmartRenderRender`를 직접 Mixin으로 vanilla `PlayerEntityRenderer`에 연결하는 방식으로 대체. `getRenderModels()` 패턴 → 단일 플레이어 렌더러 인스턴스로 단순화.
