# SmartRenderModel.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/SmartRenderModel.java  
패키지: `net.smart.render`  
상속: `SmartRenderModel extends SmartRenderContext`

---

## 역할

클라이언트 전용 플레이어 모델 클래스.  
vanilla `ModelBiped`의 파트 구조를 해체하고, 부모-자식 계층 구조로 재조립한다.  
실제 애니메이션 메서드는 이 클래스에 직접 구현되어 있고, `IModelPlayer imp`를 통해 PlayerAPI 시스템으로 위임/호출된다.

- **실행 위치**: 클라이언트 전용

---

## 필드

```java
public IModelPlayer imp;         // PlayerAPI 래퍼 인터페이스
public ModelBiped mp;            // vanilla ModelBiped 레퍼런스

public boolean isInventory;
public int scaleArmType;
public int scaleLegType;

public float totalVerticalDistance;
public float currentVerticalSpeed;
public float totalDistance;
public float currentSpeed;

public double distance;
public double verticalDistance;
public double horizontalDistance;
public float currentCameraAngle;
public float currentVerticalAngle;
public float currentHorizontalAngle;

public float actualRotation;     // 플레이어의 실제 yaw 회전 (각도 단위)
public float forwardRotation;
public float workingAngle;

// 모델 파트 (전부 ModelRotationRenderer)
public ModelRotationRenderer bipedOuter;
public ModelRotationRenderer bipedTorso;
public ModelRotationRenderer bipedBody;
public ModelRotationRenderer bipedBreast;
public ModelRotationRenderer bipedNeck;
public ModelRotationRenderer bipedHead;
public ModelRotationRenderer bipedHeadwear;
public ModelRotationRenderer bipedRightShoulder;
public ModelRotationRenderer bipedRightArm;
public ModelRotationRenderer bipedLeftShoulder;
public ModelRotationRenderer bipedLeftArm;
public ModelRotationRenderer bipedPelvic;
public ModelRotationRenderer bipedRightLeg;
public ModelRotationRenderer bipedLeftLeg;
public ModelEarsRenderer bipedEars;
public ModelCapeRenderer bipedCloak;

public boolean disabled;
public boolean attemptToCallRenderCape;
public RendererData prevOuterRenderData;
public boolean isSleeping;
public boolean firstPerson;
```

---

## 모델 계층 구조 (생성자에서 구성)

```
bipedOuter  (root, fadeEnabled=true, parent=null)
└── bipedTorso  (텍스처 16,16)
    ├── bipedBody  (텍스처 16,16, originalBipedBody 큐브/자식 복사)
    ├── bipedBreast  (텍스처 없음 -1,-1)
    │   ├── bipedNeck  (텍스처 없음 -1,-1)
    │   │   └── bipedHead  (텍스처 0,0, originalBipedHead 복사)
    │   │       ├── bipedEars  (ModelEarsRenderer, 텍스처 24,0)  ← 주의: copy()가 bipedCloak을 source로 쓰는 버그 있음
    │   │       └── bipedHeadwear  (텍스처 32,0, originalBipedHeadwear 복사)
    │   ├── bipedCloak  (ModelCapeRenderer, rotationPoint 0,0,2, originalBipedCloak 복사)
    │   ├── bipedRightShoulder  (텍스처 40,16, rotationPoint -5, 2, 0)
    │   │   └── bipedRightArm  (텍스처 40,16, originalBipedRightArm 복사)
    │   └── bipedLeftShoulder  (텍스처 -1,-1, mirror=true, rotationPoint 5, 2, 0)
    │       └── bipedLeftArm  (텍스처 40,16, originalBipedLeftArm 복사)
    └── bipedPelvic  (텍스처 없음 -1,-1, rotationPoint 0, 12, 0)
        ├── bipedRightLeg  (텍스처 0,16, originalBipedRightLeg 복사, rotationPoint -2, 0, 0)
        └── bipedLeftLeg  (텍스처 0,16, originalBipedLeftLeg 복사, rotationPoint 2, 0, 0)
```

**생성자 특이 사항:**
- `mp.boxList.clear()` — vanilla boxList를 완전히 비운다.
- `copy(bipedCloak, originalBipedEars)` — bipedEars에 originalBipedEars를 복사해야 하는데 실제 코드는 `copy(bipedCloak, originalBipedEars)`로 되어 있어 첫 인자가 bipedCloak이다. 버그로 보임.
- 생성자 마지막에 `SmartRenderRender.CurrentMainModel`이 null이 아니면, 해당 모델에서 거리/속도/회전 등 상태값을 복사한다. (레이어 모델이 메인 모델 상태를 이어받는 구조)
- `imp.initialize(bipedBody, bipedCloak, bipedHead, bipedEars, bipedHeadwear, bipedRightArm, bipedLeftArm, bipedRightLeg, bipedLeftLeg)` 호출.

---

## 생성자 시그니처

```java
public SmartRenderModel(
    ModelBiped mp,
    IModelPlayer imp,
    ModelRenderer originalBipedBody,
    ModelRenderer originalBipedCloak,
    ModelRenderer originalBipedHead,
    ModelRenderer originalBipedEars,
    ModelRenderer originalBipedHeadwear,
    ModelRenderer originalBipedRightArm,
    ModelRenderer originalBipedLeftArm,
    ModelRenderer originalBipedRightLeg,
    ModelRenderer originalBipedLeftLeg
)
```

---

## 메서드 전체

### `render(Entity, float, float, float, float, float, float)`

```java
public void render(Entity entity, float totalHorizontalDistance, float currentHorizontalSpeed,
                   float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{
    // 1. vanilla 파트들 ignoreRender=true → vanilla superRender 호출 (vanilla 파트 렌더링 억제)
    bipedBody.ignoreRender = bipedHead.ignoreRender = bipedHeadwear.ignoreRender =
        bipedRightArm.ignoreRender = bipedLeftArm.ignoreRender =
        bipedRightLeg.ignoreRender = bipedLeftLeg.ignoreRender = true;
    imp.superRender(entity, totalHorizontalDistance, currentHorizontalSpeed,
        totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);
    bipedBody.ignoreRender = ... = false;

    // 2. SmartRender 계층 전체 렌더링
    bipedOuter.render(factor);

    // 3. 각 노드의 ignoreBase 렌더링
    bipedOuter.renderIgnoreBase(factor);
    bipedTorso.renderIgnoreBase(factor);
    // ... (모든 파트 순서대로)
    bipedLeftLeg.renderIgnoreBase(factor);
}
```

- **역할**: vanilla 렌더를 억제하고 SmartRender 계층 트리로 대체 렌더링
- **호출 시점**: `SmartRenderRenderPlayerBase` 또는 `ModelPlayer`에서 vanilla `render()` 오버라이드로 호출됨

---

### `setRotationAngles(float, float, float, float, float, float, Entity)`

```java
public void setRotationAngles(...) {
    reset();  // 모든 파트 초기화

    if(firstPerson || isInventory) {
        // 모든 파트 ignoreBase=true, forceRender=firstPerson
        // 팔/다리 rotationPoint를 vanilla 위치로 복원:
        //   bipedRightArm: (-5, 2, 0), bipedLeftArm: (5, 2, 0)
        //   bipedRightLeg: (-2, 12, 0), bipedLeftLeg: (2, 12, 0)
        imp.superSetRotationAngles(...);
        return;  // 조기 리턴
    }

    if(isSleeping) {
        prevOuterRenderData.rotateAngleX = 0;
        prevOuterRenderData.rotateAngleY = 0;
        prevOuterRenderData.rotateAngleZ = 0;
    }

    bipedOuter.previous = prevOuterRenderData;
    bipedOuter.rotateAngleY = actualRotation / RadiantToAngle;
    bipedOuter.fadeRotateAngleY = !(entity.ridingEntity instanceof EntityPig);
    // EntityPig를 타고 있으면 fadeRotateAngleY = false

    // 애니메이션 순서 (항상):
    imp.animateHeadRotation(...);

    if(isSleeping) imp.animateSleeping(...);

    imp.animateArmSwinging(...);

    if(mp.isRiding) imp.animateRiding(...);
    if(mp.heldItemLeft != 0) imp.animateLeftArmItemHolding(...);
    if(mp.heldItemRight != 0) imp.animateRightArmItemHolding(...);

    if(mp.onGround > -9990F) {
        imp.animateWorkingBody(...);
        imp.animateWorkingArms(...);
    }

    if(mp.isSneak) imp.animateSneaking(...);

    imp.animateArms(...);  // 항상 (미세 흔들림)

    if(mp.aimedBow) imp.animateBowAiming(...);

    // fade 처리
    if(bipedOuter.previous != null && !bipedOuter.fadeRotateAngleX)
        bipedOuter.previous.rotateAngleX = bipedOuter.rotateAngleX;
    if(bipedOuter.previous != null && !bipedOuter.fadeRotateAngleY)
        bipedOuter.previous.rotateAngleY = bipedOuter.rotateAngleY;

    bipedOuter.fadeIntermediate(totalTime);
    bipedOuter.fadeStore(totalTime);

    bipedCloak.ignoreBase = false;
    bipedCloak.rotateAngleX = Sixtyfourth;  // 망토 기본 X 회전
}
```

**애니메이션 호출 순서 정리:**
1. `animateHeadRotation` — 항상
2. `animateSleeping` — isSleeping일 때
3. `animateArmSwinging` — 항상
4. `animateRiding` — mp.isRiding일 때
5. `animateLeftArmItemHolding` — mp.heldItemLeft != 0
6. `animateRightArmItemHolding` — mp.heldItemRight != 0
7. `animateWorkingBody` — mp.onGround > -9990F
8. `animateWorkingArms` — mp.onGround > -9990F
9. `animateSneaking` — mp.isSneak
10. `animateArms` — 항상 (호흡/미세 흔들림)
11. `animateBowAiming` — mp.aimedBow

---

### 개별 애니메이션 메서드 (concrete 구현)

#### `animateHeadRotation(float viewHorizontalAngelOffset, float viewVerticalAngelOffset)`
```java
bipedNeck.ignoreBase = true;
bipedHead.rotateAngleY = (actualRotation + viewHorizontalAngelOffset) / RadiantToAngle;
bipedHead.rotateAngleX = viewVerticalAngelOffset / RadiantToAngle;
```
- bipedNeck을 ignoreBase로 설정 (neck 자체의 base 회전 무시)
- head Y = (actualRotation + horizontal offset) / RadiantToAngle
- head X = vertical offset / RadiantToAngle

#### `animateSleeping()`
```java
bipedNeck.ignoreBase = false;
bipedHead.rotateAngleY = 0F;
bipedHead.rotateAngleX = Eighth;          // π/8 ≈ 0.3927 rad
bipedTorso.rotationPointZ = -17F;
```

#### `animateArmSwinging(float totalHorizontalDistance, float currentHorizontalSpeed)`
```java
bipedRightArm.rotateAngleX = cos(totalHorizontalDistance * 0.6662F + Half) * 2.0F * currentHorizontalSpeed * 0.5F;
bipedLeftArm.rotateAngleX  = cos(totalHorizontalDistance * 0.6662F)        * 2.0F * currentHorizontalSpeed * 0.5F;
bipedRightLeg.rotateAngleX = cos(totalHorizontalDistance * 0.6662F)        * 1.4F * currentHorizontalSpeed;
bipedLeftLeg.rotateAngleX  = cos(totalHorizontalDistance * 0.6662F + Half) * 1.4F * currentHorizontalSpeed;
```
- Half = π/2 (오른팔과 왼다리가 같은 위상, 왼팔과 오른다리가 반대 위상)

#### `animateRiding()`
```java
bipedRightArm.rotateAngleX += -0.6283185F;   // += -π/5 ≈ -2π/10
bipedLeftArm.rotateAngleX  += -0.6283185F;
bipedRightLeg.rotateAngleX = -1.256637F;      // = -2π/5
bipedLeftLeg.rotateAngleX  = -1.256637F;
bipedRightLeg.rotateAngleY =  0.3141593F;     // = π/10
bipedLeftLeg.rotateAngleY  = -0.3141593F;
```

#### `animateLeftArmItemHolding()`
```java
bipedLeftArm.rotateAngleX = bipedLeftArm.rotateAngleX * 0.5F - 0.3141593F * mp.heldItemLeft;
```

#### `animateRightArmItemHolding()`
```java
bipedRightArm.rotateAngleX = bipedRightArm.rotateAngleX * 0.5F - 0.3141593F * mp.heldItemRight;
```

#### `animateWorkingBody()`
```java
float angle = MathHelper.sin(MathHelper.sqrt_float(mp.onGround) * Whole) * 0.2F;
bipedBreast.rotateAngleY = bipedBody.rotateAngleY += angle;
bipedBreast.rotationOrder = bipedBody.rotationOrder = ModelRotationRenderer.YXZ;
bipedLeftArm.rotateAngleX += angle;
```
- `Whole` = 2π
- bipedBreast와 bipedBody 둘 다 같은 angle로 Y 회전, 회전 순서를 YXZ로 변경

#### `animateWorkingArms()`
```java
float f6 = 1.0F - mp.onGround;
f6 = 1.0F - f6 * f6 * f6;                           // ease-in cubic
float f7 = MathHelper.sin(f6 * Half);
float f8 = MathHelper.sin(mp.onGround * Half) * -(bipedHead.rotateAngleX - 0.7F) * 0.75F;
bipedRightArm.rotateAngleX -= f7 * 1.2D + f8;
bipedRightArm.rotateAngleY += MathHelper.sin(MathHelper.sqrt_float(mp.onGround) * Whole) * 0.4F;
bipedRightArm.rotateAngleZ -= MathHelper.sin(mp.onGround * Half) * 0.4F;
```

#### `animateSneaking()`
```java
bipedTorso.rotateAngleX  += 0.5F;
bipedRightLeg.rotateAngleX += -0.5F;
bipedLeftLeg.rotateAngleX  += -0.5F;
bipedRightArm.rotateAngleX += -0.1F;
bipedLeftArm.rotateAngleX  += -0.1F;

bipedPelvic.offsetY  = -0.137F;
bipedPelvic.offsetZ  = -0.051F;
bipedBreast.offsetY  = -0.014F;
bipedBreast.offsetZ  = -0.057F;
bipedNeck.offsetY    =  0.0621F;
```

#### `animateArms(float totalTime)` — 항상 실행되는 미세 호흡 애니메이션
```java
bipedRightArm.rotateAngleZ += cos(totalTime * 0.09F) * 0.05F + 0.05F;
bipedLeftArm.rotateAngleZ  -= cos(totalTime * 0.09F) * 0.05F + 0.05F;
bipedRightArm.rotateAngleX += sin(totalTime * 0.067F) * 0.05F;
bipedLeftArm.rotateAngleX  -= sin(totalTime * 0.067F) * 0.05F;
```

#### `animateBowAiming(float totalTime)`
```java
bipedRightArm.rotateAngleZ = 0.0F;
bipedLeftArm.rotateAngleZ  = 0.0F;
bipedRightArm.rotateAngleY = -0.1F + bipedHead.rotateAngleY - bipedOuter.rotateAngleY;
bipedLeftArm.rotateAngleY  =  0.1F + bipedHead.rotateAngleY + 0.4F - bipedOuter.rotateAngleY;
bipedRightArm.rotateAngleX = -1.570796F + bipedHead.rotateAngleX;  // -π/2
bipedLeftArm.rotateAngleX  = -1.570796F + bipedHead.rotateAngleX;
// 그 다음 animateArms와 동일한 미세 흔들림 적용
bipedRightArm.rotateAngleZ += cos(totalTime * 0.09F) * 0.05F + 0.05F;
bipedLeftArm.rotateAngleZ  -= cos(totalTime * 0.09F) * 0.05F + 0.05F;
bipedRightArm.rotateAngleX += sin(totalTime * 0.067F) * 0.05F;
bipedLeftArm.rotateAngleX  -= sin(totalTime * 0.067F) * 0.05F;
```

---

### `reset()`

모든 ModelRotationRenderer에 `reset()` 호출 후, 기본 rotationPoint 복원:

```java
bipedRightShoulder.setRotationPoint(-5F, 2.0F, 0.0F);
bipedLeftShoulder.setRotationPoint(5F, 2.0F, 0.0F);
bipedPelvic.setRotationPoint(0.0F, 12.0F, 0.0F);
bipedRightLeg.setRotationPoint(-2F, 0.0F, 0.0F);
bipedLeftLeg.setRotationPoint(2.0F, 0.0F, 0.0F);
bipedCloak.setRotationPoint(0.0F, 0.0F, 2.0F);
```

---

### `renderCloak(float f)`

```java
public void renderCloak(float f) {
    attemptToCallRenderCape = true;
    if(!disabled)
        imp.superRenderCloak(f);
}
```

---

### `getRandomBox(Random par1Random)`

- `mp.boxList`를 순회하여 `canBeRandomBoxSource(renderer)` 조건을 만족하는 렌더러를 랜덤으로 반환
- 조건: `renderer.cubeList != null && cubeList.size() > 0 && (!(renderer instanceof ModelRotationRenderer) || ((ModelRotationRenderer)renderer).canBeRandomBoxSource())`

---

### private `create(int i, int j, ModelRotationRenderer base)`

```java
return new ModelRotationRenderer(mp, i, j, base);
```

### private `create(int i, int j, ModelRotationRenderer base, ModelRenderer original)`

```java
ModelRotationRenderer local = create(i, j, base);
copy(local, original);
return local;
```

### private static `copy(ModelRotationRenderer local, ModelRenderer original)`

```java
if(original.childModels != null)
    for(Object childModel : original.childModels)
        local.addChild((ModelRenderer)childModel);
if(original.cubeList != null)
    for(Object cube : original.cubeList)
        local.cubeList.add(cube);
local.mirror = original.mirror;
local.isHidden = original.isHidden;
local.showModel = original.showModel;
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartRenderContext` | 상위 클래스 (RadiantToAngle, Half, Eighth, Whole, Sixtyfourth 등 상수 보유) |
| `IModelPlayer` (imp) | animateXxx, superRender, superSetRotationAngles, superRenderCloak, initialize 호출 |
| `ModelBiped` (mp) | isRiding, heldItemLeft, heldItemRight, onGround, isSneak, aimedBow, boxList 읽기 |
| `ModelRotationRenderer` | 모든 파트의 타입 |
| `ModelCapeRenderer` | bipedCloak |
| `ModelEarsRenderer` | bipedEars |
| `RendererData` | prevOuterRenderData (fade 시스템) |
| `SmartRenderRender.CurrentMainModel` | 생성자에서 상태 복사 |

---

## 상수 (SmartRenderContext에서 상속)

이 파일에서 직접 사용하는 상수 (실제 값은 SmartRenderContext 리서치 후 확인 필요):

| 이름 | 사용 위치 | 추정 값 (미확인) |
|------|-----------|-----------------|
| `RadiantToAngle` | head/outer Y 회전 나눗셈 | [미확인 — SmartRenderContext 읽기 전] |
| `Half` | arm swinging, working arms | [미확인 — SmartRenderContext 읽기 전] |
| `Eighth` | sleeping head X | [미확인 — SmartRenderContext 읽기 전] |
| `Whole` | working body/arms sin | [미확인 — SmartRenderContext 읽기 전] |
| `Sixtyfourth` | cloak X 회전 | [미확인 — SmartRenderContext 읽기 전] |

---

## 주요 관찰 사항

1. **vanilla boxList 완전 교체**: 생성자에서 `mp.boxList.clear()` 후 SmartRender 파트들만 등록. vanilla 파트는 SmartRender 파트 내부에 cubeList/childModels만 복사됨.

2. **ignoreRender 패턴**: `render()`에서 vanilla superRender 호출 시 bipedBody/Head/Arms/Legs를 일시적으로 ignoreRender=true로 설정하여 vanilla가 이들을 렌더링하지 못하게 막고, 대신 SmartRender 계층으로 렌더링.

3. **firstPerson/isInventory 조기 리턴**: `setRotationAngles()`에서 이 두 상태이면 vanilla superSetRotationAngles로만 처리하고 조기 리턴. SmartRender 애니메이션 로직 전체 스킵.

4. **bipedOuter의 역할**: 전체 모델의 루트. `actualRotation`으로 Y 회전, fade 시스템으로 부드러운 회전 전환. `prevOuterRenderData`로 이전 프레임 회전값 보존.

5. **어깨/골반 중간 노드**: 팔은 bipedShoulder, 다리는 bipedPelvic을 거쳐 계층화됨. vanilla에는 없는 구조.

6. **bipedNeck**: 기본적으로 bipedNeck.ignoreBase=true (animateHeadRotation에서 설정). sleeping 시에만 ignoreBase=false.
