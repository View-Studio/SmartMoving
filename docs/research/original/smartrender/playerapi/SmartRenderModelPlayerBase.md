# SmartRenderModelPlayerBase.java (net.smart.render.playerapi) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/playerapi/SmartRenderModelPlayerBase.java  
패키지: `net.smart.render.playerapi`  
종류: `class`  
상속/구현: `SmartRenderModelPlayerBase extends ModelPlayerBase implements IModelPlayer`

---

## 역할

PlayerAPI의 `ModelPlayerBase`를 구현하여, vanilla `ModelPlayer`(ModelBiped)의 모든 렌더 메서드를 `SmartRenderModel`로 위임(delegate)하는 어댑터 클래스.  
PlayerAPI 경로에서 `IModelPlayer` 인터페이스를 구현하는 실체이며, SmartRender의 확장 모델 트리를 PlayerAPI 시스템에 연결하는 다리 역할을 한다.

- **실행 위치**: 클라이언트 전용 (PlayerAPI는 클라이언트 전용)

---

## 필드

```java
private SmartRenderModel model;   // lazy init. getRenderModel() 최초 호출 시 생성
```

---

## 생성자

```java
public SmartRenderModelPlayerBase(ModelPlayerAPI modelplayerapi)
{
    super(modelplayerapi);
}
```

- `ModelPlayerBase(ModelPlayerAPI)` 생성자 호출
- `super()`에서 `modelPlayerAPI`, `modelPlayer` 필드가 초기화됨 (PlayerAPI 제공)

---

## 메서드 전체

### `getRenderModel()` — lazy init

```java
public SmartRenderModel getRenderModel()
{
    if(model == null)
        model = new SmartRenderModel(modelPlayer, this,
            modelPlayer.bipedBody, modelPlayer.bipedCloak,
            modelPlayer.bipedHead, modelPlayer.bipedEars, modelPlayer.bipedHeadwear,
            modelPlayer.bipedRightArm, modelPlayer.bipedLeftArm,
            modelPlayer.bipedRightLeg, modelPlayer.bipedLeftLeg);
    return model;
}
```

**SmartRenderModel 생성자 인수 (순서대로):**

| 순서 | 값 | 의미 |
|------|-----|------|
| 1 | `modelPlayer` | vanilla ModelPlayer (ModelBiped) 인스턴스 |
| 2 | `this` | IModelPlayer 구현체 (this 자신) |
| 3 | `modelPlayer.bipedBody` | vanilla bipedBody |
| 4 | `modelPlayer.bipedCloak` | vanilla bipedCloak |
| 5 | `modelPlayer.bipedHead` | vanilla bipedHead |
| 6 | `modelPlayer.bipedEars` | vanilla bipedEars |
| 7 | `modelPlayer.bipedHeadwear` | vanilla bipedHeadwear |
| 8 | `modelPlayer.bipedRightArm` | vanilla bipedRightArm |
| 9 | `modelPlayer.bipedLeftArm` | vanilla bipedLeftArm |
| 10 | `modelPlayer.bipedRightLeg` | vanilla bipedRightLeg |
| 11 | `modelPlayer.bipedLeftLeg` | vanilla bipedLeftLeg |

- 첫 호출 시점에 `modelPlayer.biped*` 필드들이 아직 vanilla 값임. SmartRenderModel 생성자 내부에서 이 값들을 SmartRender 구현체로 교체하고, `initialize()`를 호출하여 `modelPlayer.biped*`를 덮어씀.

---

### `render(...)` — override

```java
@Override
public void render(Entity entity,
    float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset,
    float factor)
{
    getRenderModel().render(entity, totalHorizontalDistance, currentHorizontalSpeed,
        totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);
}
```

- `SmartRenderModel.render()`로 완전 위임

---

### `superRender(...)` — override

```java
@Override
public void superRender(Entity entity,
    float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset,
    float factor)
{
    super.render(entity, totalHorizontalDistance, currentHorizontalSpeed,
        totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);
}
```

- `ModelPlayerBase.render()` (PlayerAPI가 제공하는 vanilla 렌더 체인)을 호출
- IModelPlayer.superRender() 구현

---

### `initialize(...)` — override

```java
@Override
public void initialize(
    ModelRenderer bipedBody, ModelRenderer bipedCloak,
    ModelRenderer bipedHead, ModelRenderer bipedEars, ModelRenderer bipedHeadwear,
    ModelRenderer bipedRightArm, ModelRenderer bipedLeftArm,
    ModelRenderer bipedRightLeg, ModelRenderer bipedLeftLeg)
{
    modelPlayer.bipedBody = bipedBody;
    modelPlayer.bipedCloak = bipedCloak;
    modelPlayer.bipedHead = bipedHead;
    modelPlayer.bipedEars = bipedEars;
    modelPlayer.bipedHeadwear = bipedHeadwear;
    modelPlayer.bipedRightArm = bipedRightArm;
    modelPlayer.bipedLeftArm = bipedLeftArm;
    modelPlayer.bipedRightLeg = bipedRightLeg;
    modelPlayer.bipedLeftLeg = bipedLeftLeg;
}
```

- `modelPlayer`(vanilla `ModelPlayer`)의 `biped*` 필드 9개를 SmartRenderModel이 만든 `ModelRotationRenderer` 인스턴스로 교체
- SmartRenderModel 생성자가 이 메서드를 호출: `imp.initialize(bipedBody, bipedCloak, ...)` (`imp = this` = SmartRenderModelPlayerBase)
- PlayerAPI 비-PlayerAPI 공통 인터페이스 `IModelPlayer.initialize()` 구현

---

### `setRotationAngles(...)` — override

```java
@Override
public void setRotationAngles(
    float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset,
    float factor, Entity entity)
{
    getRenderModel().setRotationAngles(totalHorizontalDistance, currentHorizontalSpeed,
        totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor, entity);
}
```

- `SmartRenderModel.setRotationAngles()`로 완전 위임

---

### `superSetRotationAngles(...)` — override

```java
@Override
public void superSetRotationAngles(
    float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset,
    float factor, Entity entity)
{
    super.setRotationAngles(totalHorizontalDistance, currentHorizontalSpeed,
        totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor, entity);
}
```

- `ModelPlayerBase.setRotationAngles()` (vanilla 체인) 호출

---

### `renderCloak(float f)` — override

```java
@Override
public void renderCloak(float f)
{
    getRenderModel().renderCloak(f);
}
```

---

### `superRenderCloak(float factor)` — override

```java
@Override
public void superRenderCloak(float factor)
{
    super.renderCloak(factor);
}
```

---

### `getRandomModelBox(Random random)` — override

```java
@Override
public ModelRenderer getRandomModelBox(Random random)
{
    return getRenderModel().getRandomBox(random);
}
```

---

## IModelPlayer getter 메서드 (16개)

모두 `getRenderModel().biped*` 필드를 반환. 모두 한 줄 구현.

```java
@Override public ModelRenderer getOuter()         { return getRenderModel().bipedOuter; }
@Override public ModelRenderer getTorso()         { return getRenderModel().bipedTorso; }
@Override public ModelRenderer getBody()          { return getRenderModel().bipedBody; }
@Override public ModelRenderer getBreast()        { return getRenderModel().bipedBreast; }
@Override public ModelRenderer getNeck()          { return getRenderModel().bipedNeck; }
@Override public ModelRenderer getHead()          { return getRenderModel().bipedHead; }
@Override public ModelRenderer getHeadwear()      { return getRenderModel().bipedHeadwear; }
@Override public ModelRenderer getRightShoulder() { return getRenderModel().bipedRightShoulder; }
@Override public ModelRenderer getRightArm()      { return getRenderModel().bipedRightArm; }
@Override public ModelRenderer getLeftShoulder()  { return getRenderModel().bipedLeftShoulder; }
@Override public ModelRenderer getLeftArm()       { return getRenderModel().bipedLeftArm; }
@Override public ModelRenderer getPelvic()        { return getRenderModel().bipedPelvic; }
@Override public ModelRenderer getRightLeg()      { return getRenderModel().bipedRightLeg; }
@Override public ModelRenderer getLeftLeg()       { return getRenderModel().bipedLeftLeg; }
@Override public ModelRenderer getEars()          { return getRenderModel().bipedEars; }
@Override public ModelRenderer getCloak()         { return getRenderModel().bipedCloak; }
```

**vanilla에 없는 SmartRender 전용 필드 (getters로 노출):**  
`bipedOuter`, `bipedTorso`, `bipedBreast`, `bipedNeck`, `bipedRightShoulder`, `bipedLeftShoulder`, `bipedPelvic`

---

## animate* 메서드 — PlayerAPI dynamic dispatch 패턴 (11쌍)

각 animate* 메서드는 두 개로 구성된다:
1. **`animate*(6개 float params)`** — `modelPlayerAPI.dynamic("animate*", new Object[]{...6개 params...})` 호출 → PlayerAPI가 `dynamicVirtual*`을 reflection으로 dispatch
2. **`@SuppressWarnings("unused") dynamicVirtual*(6개 float params)`** — 실제 로직 수행: `getRenderModel().animate*(선택된 파라미터)`

`@SuppressWarnings("unused")`는 직접 호출되지 않고 PlayerAPI reflection으로만 호출되기 때문.

### 각 메서드의 실제 SmartRenderModel 호출 및 전달 파라미터

| animate* 메서드 | SmartRenderModel 호출 | 전달 파라미터 |
|-----------------|----------------------|--------------|
| `animateHeadRotation` | `animateHeadRotation(viewHorizontalAngelOffset, viewVerticalAngelOffset)` | 파라미터 4, 5 |
| `animateSleeping` | `animateSleeping()` | 없음 |
| `animateArmSwinging` | `animateArmSwinging(totalHorizontalDistance, currentHorizontalSpeed)` | 파라미터 1, 2 |
| `animateRiding` | `animateRiding()` | 없음 |
| `animateLeftArmItemHolding` | `animateLeftArmItemHolding()` | 없음 |
| `animateRightArmItemHolding` | `animateRightArmItemHolding()` | 없음 |
| `animateWorkingBody` | `animateWorkingBody()` | 없음 |
| `animateWorkingArms` | `animateWorkingArms()` | 없음 |
| `animateSneaking` | `animateSneaking()` | 없음 |
| `animateArms` | `animateArms(totalTime)` | 파라미터 3 |
| `animateBowAiming` | `animateBowAiming(totalTime)` | 파라미터 3 |

**6개 float params 순서 (모든 animate* 공통):**  
`totalHorizontalDistance(1), currentHorizontalSpeed(2), totalTime(3), viewHorizontalAngelOffset(4), viewVerticalAngelOffset(5), factor(6)`

---

## import

```java
import java.util.*;
import net.minecraft.client.model.*;   // ModelRenderer
import net.minecraft.entity.*;          // Entity
import api.player.model.*;              // ModelPlayerBase, ModelPlayerAPI
import net.smart.render.*;              // SmartRenderModel
import net.smart.render.IModelPlayer;
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `ModelPlayerBase` (PlayerAPI) | 상위 클래스. `modelPlayer`, `modelPlayerAPI` 필드 제공 |
| `IModelPlayer` | 구현 인터페이스 (getter 16개 + animate* 11개 + initialize + super* 정의) |
| `SmartRenderModel` | 실제 렌더 로직 보유. 모든 메서드가 이쪽으로 위임됨 |
| `ModelPlayerAPI` (PlayerAPI) | `modelPlayerAPI.dynamic()` — animate* dynamic dispatch |
| `ModelRenderer` (vanilla) | initialize() 파라미터 타입, getter 반환 타입 |

---

## 주요 관찰 사항

1. **완전 위임 구조**: `render`, `setRotationAngles`, `renderCloak`, `getRandomModelBox`, 16개 getter, 11개 animate* — 모두 `getRenderModel()`로 위임. 이 클래스 자체에는 로직이 없고, PlayerAPI와 SmartRenderModel 사이의 어댑터.

2. **PlayerAPI dynamic dispatch 패턴**: `animate*()` → `modelPlayerAPI.dynamic("animate*", Object[])` → PlayerAPI가 `dynamicVirtual*()` reflection 호출. `dynamicVirtual*`에 `@SuppressWarnings("unused")`가 붙는 이유.

3. **initialize() 콜백**: `getRenderModel()` → `new SmartRenderModel(modelPlayer, this, ...)` → SmartRenderModel 생성자 내부에서 `imp.initialize(...)` (= this.initialize()) 호출 → `modelPlayer.biped*` 9개 필드를 SmartRender 구현체로 교체. 이 시점에 vanilla biped* 는 SmartRenderModel의 커스텀 ModelRotationRenderer로 대체됨.

4. **getOuter/getTorso/getBreast/getNeck/getRightShoulder/getLeftShoulder/getPelvic**: vanilla ModelBiped에 없는 7개 SmartRender 전용 뼈대를 IModelPlayer 인터페이스로 노출. 비-PlayerAPI 경로(ModelPlayer.java)에서는 필드 직접 접근, PlayerAPI 경로에서는 이 getter로 접근.

5. **1.21.1 이식**: PlayerAPI가 1.21.1에 없으므로 이 클래스 전체 불필요. Mixin이 vanilla PlayerEntityModel을 직접 패치하는 방식으로 대체. `initialize()`의 biped* 교체 패턴 → Mixin으로 `PlayerEntityModel`의 필드를 교체하는 방식으로 구현.
