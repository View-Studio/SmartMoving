# RenderPlayer.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/RenderPlayer.java  
패키지: `net.smart.render`  
상속/구현: `RenderPlayer extends net.minecraft.client.renderer.entity.RenderPlayer implements IRenderPlayer`

---

## 역할

PlayerAPI를 사용하지 않는 경로에서 vanilla `RenderPlayer`를 직접 상속하여 `IRenderPlayer`를 구현하는 클래스.  
vanilla 렌더러의 핵심 메서드(`doRender`, `rotateCorpse`, `preRenderCallback`, `handleRotationFloat`, `renderFirstPersonArm`)를 오버라이드하여 `SmartRenderRender render`에 위임한다.  
`ModelPlayer`와 대응되는 구조 — ModelPlayer가 모델 측 wrapper이면, 이 클래스는 렌더러 측 wrapper다.

- **실행 위치**: 클라이언트 전용

---

## 필드

```java
private final SmartRenderRender render;
private IModelPlayer[] allIModelPlayers;   // getRenderModels() lazy init 캐시
```

---

## 생성자

```java
public RenderPlayer()
{
    render = new SmartRenderRender(this);
    // SmartRenderRender 생성자 내부에서:
    //   irp.createModel(irp.getModelBipedMain(), 0.0F)  → this.createModel() 호출
    //   irp.initialize(...)                             → this.initialize() 호출
}
```

- `SmartRenderRender(this)` 생성 시 `this`(IRenderPlayer)를 전달
- SmartRenderRender 생성자가 `createModel()`과 `initialize()`를 콜백으로 호출함

---

## 메서드 전체

### `createModel(ModelBiped existing, float f)`

```java
@Override
public IModelPlayer createModel(ModelBiped existing, float f)
{
    return new ModelPlayer(f);
}
```

- `existing` 파라미터는 사용하지 않음 — 새 `ModelPlayer`를 생성하여 반환
- SmartRenderRender 생성자에서 메인(0.0F), 흉갑(1.0F), 갑옷(0.5F) 세 번 호출됨

---

### `initialize(ModelBiped modelBipedMain, ModelBiped modelArmorChestplate, ModelBiped modelArmor, float shadowSize)`

```java
@Override
public void initialize(ModelBiped modelBipedMain, ModelBiped modelArmorChestplate,
                        ModelBiped modelArmor, float shadowSize)
{
    this.mainModel             = modelBipedMain;
    this.shadowSize            = shadowSize;
    this.modelBipedMain        = modelBipedMain;
    this.modelArmorChestplate  = modelArmorChestplate;
    this.modelArmor            = modelArmor;
}
```

- vanilla `RenderPlayer`의 `mainModel`, `shadowSize`, `modelBipedMain`, `modelArmorChestplate`, `modelArmor` 필드를 SmartRender ModelPlayer들로 교체
- SmartRenderRender 생성자에서 호출됨

---

### `doRender` / `superRenderPlayer`

```java
@Override
public void doRender(AbstractClientPlayer entityplayer, double d, double d1, double d2,
                      float f, float renderPartialTicks)
{
    render.renderPlayer(entityplayer, d, d1, d2, f, renderPartialTicks);
}

@Override
public void superRenderPlayer(AbstractClientPlayer entityplayer, double d, double d1, double d2,
                               float f, float renderPartialTicks)
{
    super.doRender(entityplayer, d, d1, d2, f, renderPartialTicks);
}
```

- `doRender` → SmartRenderRender.renderPlayer() (SmartRender 로직 실행)
- `superRenderPlayer` → vanilla super.doRender() (SmartRenderRender가 콜백으로 호출)

---

### `renderFirstPersonArm` / `superDrawFirstPersonHand`

```java
@Override
public void renderFirstPersonArm(EntityPlayer entityPlayer)
{
    render.drawFirstPersonHand(entityPlayer);
}

@Override
public void superDrawFirstPersonHand(EntityPlayer entityPlayer)
{
    super.renderFirstPersonArm(entityPlayer);
}
```

---

### `rotateCorpse` / `superRotatePlayer` — protected 오버라이드

```java
@Override
protected void rotateCorpse(AbstractClientPlayer entityplayer, float totalTime,
                             float actualRotation, float f2)
{
    render.rotatePlayer(entityplayer, totalTime, actualRotation, f2);
}

@Override
public void superRotatePlayer(AbstractClientPlayer entityplayer, float totalTime,
                               float actualRotation, float f2)
{
    super.rotateCorpse(entityplayer, totalTime, actualRotation, f2);
}
```

- vanilla의 `rotateCorpse()`를 오버라이드하여 SmartRender의 회전 로직으로 대체

---

### `preRenderCallback` / `superRenderSpecials` — protected 오버라이드

```java
@Override
protected void preRenderCallback(AbstractClientPlayer entityplayer, float f)
{
    render.renderSpecials(entityplayer, f);
}

@Override
public void superRenderSpecials(AbstractClientPlayer entityplayer, float f)
{
    super.preRenderCallback(entityplayer, f);
}
```

- vanilla의 `preRenderCallback()`을 오버라이드하여 귀/망토 렌더 훅 적용

---

### `handleRotationFloat` — protected 오버라이드

```java
@Override
protected float handleRotationFloat(EntityLivingBase entityliving, float f)
{
    render.beforeHandleRotationFloat(entityliving, f);
    float result = super.handleRotationFloat(entityliving, f);
    render.afterHandleRotationFloat(entityliving, f);
    return result;
}
```

- vanilla `handleRotationFloat()` 전후로 `ticksExisted += ticksRiding` / `-= ticksRiding` 적용 (SmartRenderRender 참조)
- vanilla 계산 결과 `result`는 그대로 반환

---

### getter 메서드들

```java
@Override
public RenderManager getRenderManager() { return renderManager; }

@Override
public ModelBiped getModelBipedMain()        { return (ModelBiped)mainModel; }

@Override
public ModelBiped getModelArmorChestplate()  { return modelArmorChestplate; }

@Override
public ModelBiped getModelArmor()            { return modelArmor; }

public IModelPlayer getRenderModelBipedMain()        { return (ModelPlayer)getModelBipedMain(); }
public IModelPlayer getRenderModelArmorChestplate()  { return (ModelPlayer)getModelArmorChestplate(); }
public IModelPlayer getRenderModelArmor()            { return (ModelPlayer)getModelArmor(); }
```

- `getModelBipedMain()`: `mainModel`을 `ModelBiped`로 캐스트 반환
- `getRenderModelBipedMain()` 등: `ModelBiped`를 다시 `ModelPlayer`로 캐스트 — initialize()에서 ModelPlayer를 mainModel에 넣었으므로 유효

---

### `getRenderModels()`

```java
@Override
public IModelPlayer[] getRenderModels()
{
    if(allIModelPlayers == null)
        allIModelPlayers = new IModelPlayer[] {
            getRenderModelBipedMain(),
            getRenderModelArmorChestplate(),
            getRenderModelArmor()
        };
    return allIModelPlayers;
}
```

- lazy init — 첫 호출 시 배열 생성 후 캐시
- 반환 순서: [0] 메인, [1] 갑옷흉갑, [2] 갑옷
- SmartRenderRender.renderPlayer()와 rotatePlayer()에서 이 배열을 순회하여 모든 모델에 데이터 세팅

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `net.minecraft.client.renderer.entity.RenderPlayer` (vanilla) | 상위 클래스 — mainModel, shadowSize, modelBipedMain/Armor 필드 및 super.* 메서드 제공 |
| `IRenderPlayer` | 구현 인터페이스 |
| `SmartRenderRender` | 모든 실제 렌더 로직 위임 대상 |
| `ModelPlayer` | createModel()에서 생성, getRenderModel* 에서 캐스트 |

---

## vanilla 메서드 → SmartRender 메서드 대응 표

| vanilla 메서드 | 오버라이드 방식 | SmartRender 메서드 |
|---|---|---|
| `doRender()` | 직접 오버라이드 | `render.renderPlayer()` |
| `rotateCorpse()` | protected 오버라이드 | `render.rotatePlayer()` |
| `preRenderCallback()` | protected 오버라이드 | `render.renderSpecials()` |
| `handleRotationFloat()` | protected 오버라이드 | before/after 훅 + super 유지 |
| `renderFirstPersonArm()` | 직접 오버라이드 | `render.drawFirstPersonHand()` |

---

## 주요 관찰 사항

1. **ModelPlayer와 대칭 구조**: ModelPlayer가 `ModelBiped + IModelPlayer`이면, RenderPlayer는 `vanilla RenderPlayer + IRenderPlayer`. 둘 다 `this`를 SmartRenderXxx 생성자에 넘기는 동일한 패턴.

2. **생성자 콜백 순서**: `new SmartRenderRender(this)` 안에서 `this.createModel()` → `this.initialize()` 순서로 콜백 호출. 즉 생성자 내에서 이미 모델 교체까지 완료됨.

3. **이중 캐스트**: `mainModel`은 vanilla에서 `ModelBase` 타입이므로 `(ModelBiped)mainModel` 캐스트 후, 다시 `(ModelPlayer)getModelBipedMain()` 캐스트. initialize()에서 ModelPlayer를 넣었으므로 런타임에는 항상 성공.

4. **handleRotationFloat 유지**: 다른 메서드들과 달리 before/after 훅만 감싸고 vanilla `super.handleRotationFloat()` 결과를 그대로 반환. SmartRender가 이 메서드의 반환값을 바꾸지 않음.

5. **1.21.1 이식 포인트**: 1.21.1에서 vanilla `RenderPlayer`에 해당하는 클래스는 `PlayerEntityRenderer`. `doRender` → `render()`, `rotateCorpse` → `setupTransforms()`, `preRenderCallback` → `scale()`, `handleRotationFloat` → `getAnimationProgress()` 등으로 대응 (Yarn 매핑은 vanilla 리서치에서 확인 필요).
