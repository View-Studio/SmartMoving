# ModelPlayer.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/ModelPlayer.java  
패키지: `net.smart.render`  
상속/구현: `ModelPlayer extends ModelBiped implements IModelPlayer`

---

## 역할

PlayerAPI를 사용하지 않는 경로에서 `ModelBiped`를 직접 상속하여 `IModelPlayer`를 구현하는 클래스.  
자신이 `ModelBiped`(mp)이자 `IModelPlayer`(imp)이므로, `SmartRenderModel` 생성자에 `this`를 두 번 전달한다.  
모든 메서드는 `SmartRenderModel model`에 위임하거나, `super.*`(vanilla)를 호출하는 thin wrapper다.

- **실행 위치**: 클라이언트 전용

---

## 필드

```java
private final SmartRenderModel model;
```

---

## 생성자

```java
public ModelPlayer(float f)
{
    super(f);  // vanilla ModelBiped 초기화 → bipedBody, bipedHead 등 vanilla 파트 생성

    model = new SmartRenderModel(
        this,          // ModelBiped mp
        this,          // IModelPlayer imp  ← 자기 자신을 두 역할로 전달
        bipedBody, bipedCloak, bipedHead, bipedEars, bipedHeadwear,
        bipedRightArm, bipedLeftArm, bipedRightLeg, bipedLeftLeg
    );
}
```

- `super(f)` 로 vanilla `bipedBody`, `bipedHead` 등 생성 후, 이를 `SmartRenderModel` 생성자에 `original*` 인자로 전달
- `SmartRenderModel` 생성자 내부에서 `imp.initialize(...)` 호출 → `initialize()` 메서드가 `this.biped*` 필드를 SmartRender 파트로 교체

---

## 메서드 전체

### render / superRender

```java
@Override
public void render(Entity entity, float totalHorizontalDistance, float currentHorizontalSpeed,
                   float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{
    model.render(entity, totalHorizontalDistance, currentHorizontalSpeed,
                 totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);
}

@Override
public void superRender(Entity entity, float totalHorizontalDistance, float currentHorizontalSpeed,
                         float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{
    super.render(entity, totalHorizontalDistance, currentHorizontalSpeed,
                 totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);
}
```

- `render()`: SmartRender 렌더로 위임
- `superRender()`: vanilla `ModelBiped.render()` 직접 호출 (SmartRenderModel.render()에서 ignoreRender 패턴용)

---

### getRenderModel

```java
@Override
public SmartRenderModel getRenderModel() { return model; }
```

---

### initialize

```java
@Override
public void initialize(ModelRenderer bipedBody, ModelRenderer bipedCloak, ModelRenderer bipedHead,
                        ModelRenderer bipedEars, ModelRenderer bipedHeadwear,
                        ModelRenderer bipedRightArm, ModelRenderer bipedLeftArm,
                        ModelRenderer bipedRightLeg, ModelRenderer bipedLeftLeg)
{
    this.bipedBody     = bipedBody;
    this.bipedCloak    = bipedCloak;
    this.bipedHead     = bipedHead;
    this.bipedEars     = bipedEars;
    this.bipedHeadwear = bipedHeadwear;
    this.bipedRightArm = bipedRightArm;
    this.bipedLeftArm  = bipedLeftArm;
    this.bipedRightLeg = bipedRightLeg;
    this.bipedLeftLeg  = bipedLeftLeg;
}
```

- `SmartRenderModel` 생성자에서 호출됨
- vanilla `ModelBiped`의 `biped*` 필드를 SmartRender `ModelRotationRenderer` 파트들로 교체
- 이후 vanilla `super.render()`/`super.setRotationAngles()`가 이 교체된 파트들을 사용하게 됨

---

### setRotationAngles / superSetRotationAngles

```java
@Override
public void setRotationAngles(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime,
                               float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor, Entity entity)
{
    model.setRotationAngles(totalHorizontalDistance, currentHorizontalSpeed, totalTime,
                            viewHorizontalAngelOffset, viewVerticalAngelOffset, factor, entity);
}

@Override
public void superSetRotationAngles(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime,
                                    float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor, Entity entity)
{
    super.setRotationAngles(totalHorizontalDistance, currentHorizontalSpeed, totalTime,
                            viewHorizontalAngelOffset, viewVerticalAngelOffset, factor, entity);
}
```

---

### renderCloak / superRenderCloak

```java
@Override
public void renderCloak(float f)      { model.renderCloak(f); }

@Override
public void superRenderCloak(float f) { super.renderCloak(f); }
```

---

### getRandomModelBox

```java
@Override
public ModelRenderer getRandomModelBox(Random random) { return model.getRandomBox(random); }
```

---

### getter 메서드 16개 (전부 model.biped* 반환)

```java
@Override public ModelRenderer getOuter()         { return model.bipedOuter; }
@Override public ModelRenderer getTorso()         { return model.bipedTorso; }
@Override public ModelRenderer getBody()          { return model.bipedBody; }
@Override public ModelRenderer getBreast()        { return model.bipedBreast; }
@Override public ModelRenderer getNeck()          { return model.bipedNeck; }
@Override public ModelRenderer getHead()          { return model.bipedHead; }
@Override public ModelRenderer getHeadwear()      { return model.bipedHeadwear; }
@Override public ModelRenderer getRightShoulder() { return model.bipedRightShoulder; }
@Override public ModelRenderer getRightArm()      { return model.bipedRightArm; }
@Override public ModelRenderer getLeftShoulder()  { return model.bipedLeftShoulder; }
@Override public ModelRenderer getLeftArm()       { return model.bipedLeftArm; }
@Override public ModelRenderer getPelvic()        { return model.bipedPelvic; }
@Override public ModelRenderer getRightLeg()      { return model.bipedRightLeg; }
@Override public ModelRenderer getLeftLeg()       { return model.bipedLeftLeg; }
@Override public ModelRenderer getEars()          { return model.bipedEars; }
@Override public ModelRenderer getCloak()         { return model.bipedCloak; }
```

---

### animate* 메서드 11개 — 파라미터 전달 주목

IModelPlayer 인터페이스 시그니처는 6개 파라미터를 받지만, model의 실제 메서드에는 필요한 것만 전달한다.

| 메서드 | model에 전달하는 파라미터 |
|--------|--------------------------|
| `animateHeadRotation` | `viewHorizontalAngelOffset`, `viewVerticalAngelOffset` |
| `animateSleeping` | 없음 |
| `animateArmSwinging` | `totalHorizontalDistance`, `currentHorizontalSpeed` |
| `animateRiding` | 없음 |
| `animateLeftArmItemHolding` | 없음 |
| `animateRightArmItemHolding` | 없음 |
| `animateWorkingBody` | 없음 |
| `animateWorkingArms` | 없음 |
| `animateSneaking` | 없음 |
| `animateArms` | `totalTime` |
| `animateBowAiming` | `totalTime` |

전체 코드:

```java
@Override
public void animateHeadRotation(float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{ model.animateHeadRotation(viewHorizontalAngelOffset, viewVerticalAngelOffset); }

@Override
public void animateSleeping(float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{ model.animateSleeping(); }

@Override
public void animateArmSwinging(float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{ model.animateArmSwinging(totalHorizontalDistance, currentHorizontalSpeed); }

@Override
public void animateRiding(float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{ model.animateRiding(); }

@Override
public void animateLeftArmItemHolding(float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{ model.animateLeftArmItemHolding(); }

@Override
public void animateRightArmItemHolding(float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{ model.animateRightArmItemHolding(); }

@Override
public void animateWorkingBody(float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{ model.animateWorkingBody(); }

@Override
public void animateWorkingArms(float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{ model.animateWorkingArms(); }

@Override
public void animateSneaking(float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{ model.animateSneaking(); }

@Override
public void animateArms(float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{ model.animateArms(totalTime); }

@Override
public void animateBowAiming(float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{ model.animateBowAiming(totalTime); }
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `ModelBiped` (vanilla) | 상위 클래스 — biped* 필드 및 super.render/setRotationAngles/renderCloak 제공 |
| `IModelPlayer` | 구현 인터페이스 |
| `SmartRenderModel` | 모든 실제 로직 위임 대상 |

---

## 주요 관찰 사항

1. **이중 역할**: `this`가 `ModelBiped`(mp)이자 `IModelPlayer`(imp)로 동시에 동작. `SmartRenderModel(this, this, ...)` 생성자 호출이 이를 보여줌.

2. **initialize()의 의미**: vanilla `ModelBiped`의 `biped*` public 필드를 SmartRender `ModelRotationRenderer` 파트로 교체. 이후 vanilla `super.render()`가 이 교체된 파트를 렌더링하게 되는 구조.

3. **PlayerAPI 없는 경로**: `net.smart.render.playerapi.SmartRenderModelPlayerBase`가 PlayerAPI 있는 경로, 이 `ModelPlayer`는 PlayerAPI 없는 경로. 로직은 동일하게 `SmartRenderModel`에 위임.

4. **파라미터 취사선택**: animate* 메서드는 IModelPlayer 인터페이스의 6개 파라미터 중 실제로 필요한 것만 model에 전달. 나머지는 무시.
