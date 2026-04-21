# IModelPlayer.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/IModelPlayer.java  
패키지: `net.smart.render`  
종류: `interface`

---

## 역할

`SmartRenderModel`이 실제 모델 구현체(`ModelPlayer` 또는 PlayerAPI `SmartRenderModelPlayerBase`)를 호출할 때 사용하는 인터페이스.  
`SmartRenderModel.imp` 필드의 타입이 이 인터페이스이므로, SmartRenderModel은 구현체가 PlayerAPI 경로인지 비-PlayerAPI 경로인지 구분하지 않고 동일하게 호출한다.

- **실행 위치**: 클라이언트 전용

---

## 구현체

| 구현체 | 경로 |
|--------|------|
| `ModelPlayer` | `net.smart.render.ModelPlayer` — PlayerAPI 없는 경로 |
| `SmartRenderModelPlayerBase` | `net.smart.render.playerapi.SmartRenderModelPlayerBase` — PlayerAPI 경로 |

---

## 메서드 전체 (전부 추상)

### 모델 접근

```java
SmartRenderModel getRenderModel();
```

---

### 초기화

```java
void initialize(
    ModelRenderer bipedBody,
    ModelRenderer bipedCloak,
    ModelRenderer bipedHead,
    ModelRenderer bipedEars,
    ModelRenderer bipedHeadwear,
    ModelRenderer bipedRightArm,
    ModelRenderer bipedLeftArm,
    ModelRenderer bipedRightLeg,
    ModelRenderer bipedLeftLeg
);
```

- 9개 파라미터 — SmartRender의 `ModelRotationRenderer` 파트들을 구현체의 `biped*` 필드에 주입
- `SmartRenderModel` 생성자 마지막에 호출됨

---

### vanilla super 호출용 (3개)

```java
void superRender(Entity entity,
    float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset,
    float factor);

void superSetRotationAngles(
    float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset,
    float factor, Entity entity);

void superRenderCloak(float f);
```

- `SmartRenderModel.render()` → `imp.superRender()` → 구현체에서 `super.render()` (vanilla ModelBiped.render())
- `SmartRenderModel.setRotationAngles()` → `imp.superSetRotationAngles()` → 구현체에서 `super.setRotationAngles()`
- `SmartRenderModel.renderCloak()` → `imp.superRenderCloak()` → 구현체에서 `super.renderCloak()`

---

### 파트 getter (16개, 전부 `ModelRenderer` 반환)

```java
ModelRenderer getOuter();
ModelRenderer getTorso();
ModelRenderer getBody();
ModelRenderer getBreast();
ModelRenderer getNeck();
ModelRenderer getHead();
ModelRenderer getHeadwear();
ModelRenderer getRightShoulder();
ModelRenderer getRightArm();
ModelRenderer getLeftShoulder();
ModelRenderer getLeftArm();
ModelRenderer getPelvic();
ModelRenderer getRightLeg();
ModelRenderer getLeftLeg();
ModelRenderer getEars();
ModelRenderer getCloak();
```

- SmartRender 계층 파트 16개 전부 노출
- 반환 타입은 `ModelRenderer` (실제로는 `ModelRotationRenderer` 또는 `ModelCapeRenderer`/`ModelEarsRenderer`)
- 외부 코드(SmartMoving 렌더 등)에서 파트에 직접 접근할 때 사용

---

### animate* (11개, 파라미터 시그니처 동일)

모두 동일한 6개 파라미터:  
`(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)`

```java
void animateHeadRotation(float, float, float, float, float, float);
void animateSleeping(float, float, float, float, float, float);
void animateArmSwinging(float, float, float, float, float, float);
void animateRiding(float, float, float, float, float, float);
void animateLeftArmItemHolding(float, float, float, float, float, float);
void animateRightArmItemHolding(float, float, float, float, float, float);
void animateWorkingBody(float, float, float, float, float, float);
void animateWorkingArms(float, float, float, float, float, float);
void animateSneaking(float, float, float, float, float, float);
void animateArms(float, float, float, float, float, float);
void animateBowAiming(float, float, float, float, float, float);
```

- `SmartRenderModel.setRotationAngles()`에서 조건에 따라 순서대로 호출 (SmartRenderModel.md 참조)
- 구현체(ModelPlayer)는 이 6개 파라미터 중 필요한 것만 `model.animateXxx()`에 전달

---

## `getRandomModelBox`는 이 인터페이스에 없음

- `ModelPlayer.getRandomModelBox()`는 IModelPlayer 인터페이스가 아닌 `ModelBiped.getRandomModelBox()` 오버라이드로 존재
- SmartRenderModel에서는 `imp`를 통하지 않고 직접 `model.getRandomBox()`를 호출

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartRenderModel` | 반환 타입 (getRenderModel) |
| `ModelRenderer` (vanilla) | 파트 getter 반환 타입, initialize 파라미터 타입 |
| `Entity` (vanilla) | superRender, superSetRotationAngles 파라미터 |

---

## 호출 흐름 요약

```
SmartRenderModel.render()
  └─ imp.superRender()          ← ignoreRender 패턴으로 vanilla 렌더 억제용

SmartRenderModel.setRotationAngles()
  ├─ imp.superSetRotationAngles()   ← firstPerson/isInventory 조기 리턴 경로
  ├─ imp.animateHeadRotation()
  ├─ imp.animateSleeping()          (isSleeping일 때)
  ├─ imp.animateArmSwinging()
  ├─ imp.animateRiding()            (mp.isRiding일 때)
  ├─ imp.animateLeftArmItemHolding()
  ├─ imp.animateRightArmItemHolding()
  ├─ imp.animateWorkingBody()       (mp.onGround > -9990F)
  ├─ imp.animateWorkingArms()       (mp.onGround > -9990F)
  ├─ imp.animateSneaking()          (mp.isSneak)
  ├─ imp.animateArms()
  └─ imp.animateBowAiming()         (mp.aimedBow)

SmartRenderModel.renderCloak()
  └─ imp.superRenderCloak()
```
