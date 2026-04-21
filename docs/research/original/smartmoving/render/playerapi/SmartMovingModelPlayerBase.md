# SmartMovingModelPlayerBase.java (net.smart.moving.render.playerapi) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/render/playerapi/SmartMovingModelPlayerBase.java  
패키지: `net.smart.moving.render.playerapi`  
종류: `class`  
상속: `ModelPlayerBase` (extends) + `net.smart.moving.render.IModelPlayer` (implements)  
실행 위치: 클라이언트 (렌더링, PlayerAPI)

---

## 전체 소스

소스는 GitHub 링크 참조.

---

## 역할

ModelPlayerAPI 기반 `SmartMovingModel` 연결 클래스. PlayerAPI 경로에서 모델 animate 메서드를 가로채어(`dynamicOverride*`) SM 애니메이션 로직에 전달하고, SM이 vanilla 원본을 호출할 때(`superAnimate*`) PlayerAPI의 `dynamic()` 디스패처를 통해 처리한다.

`SmartMovingModelPlayerBase`는 `ModelPlayer`(독립형)와 동일한 역할을 PlayerAPI 경로에서 담당한다.

---

## import

```java
import net.minecraft.client.model.*;  // ModelRenderer

import api.player.model.*;            // ModelPlayerBase, ModelPlayerAPI, IModelPlayerAPI

import net.smart.moving.render.*;           // SmartMovingModel, IModelPlayer
import net.smart.moving.render.IModelPlayer; // 명시적 import (패키지 내 동명 혼동 방지)
import net.smart.render.playerapi.*;        // SmartRender (getPlayerBase 조회용)
```

---

## 상속/구현 관계

```
ModelPlayerBase  (api.player.model)
    └─ SmartMovingModelPlayerBase  (이 파일)
         implements net.smart.moving.render.IModelPlayer
```

`ModelPlayerBase`는 PlayerAPI 모델 훅의 기반 클래스. `modelPlayer` 필드(PlayerAPI가 관리하는 모델 인스턴스)를 보유.

---

## 필드

```java
private SmartMovingModel model;  // 지연 초기화, non-final
```

`ModelPlayer`(독립형)는 `final`로 생성자에서 즉시 초기화.  
이 클래스는 `null`로 시작하여 `getMovingModel()` 첫 호출 시 초기화.

---

## 생성자

```java
public SmartMovingModelPlayerBase(ModelPlayerAPI modelplayerapi)
{
    super(modelplayerapi);
}
```

`ModelPlayerBase(ModelPlayerAPI)` 위임만. 모델 초기화는 지연.

---

## `getMovingModel()` (IModelPlayer 구현)

```java
@Override
public SmartMovingModel getMovingModel()
{
    if(model == null)
        model = new SmartMovingModel(SmartRender.getPlayerBase(modelPlayer), this);
    return model;
}
```

**지연 초기화**: `model == null`이면 생성, 이후 캐싱.

**`SmartMovingModel` 생성자 인자**:
- 첫 번째: `SmartRender.getPlayerBase(modelPlayer)` → `SmartRenderModelPlayerBase` (SR의 PlayerBase, `net.smart.render.IModelPlayer` 구현체)
- 두 번째: `this` → `SmartMovingModelPlayerBase` (SM의 `IModelPlayer` 구현체)

`ModelPlayer`(독립형)와의 차이:
- 독립형: `new SmartMovingModel(this, this)` — 자신이 두 인터페이스 모두 충족
- PlayerAPI: `new SmartMovingModel(SmartRender.getPlayerBase(modelPlayer), this)` — SR PlayerBase가 첫 번째 인자

**이유**: PlayerAPI 환경에서는 SR의 모델 데이터(`SmartRenderModel`)가 `SmartRenderModelPlayerBase`에 있으므로, 그것을 첫 번째 인자로 전달해야 `md.getRenderModel()`로 `SmartRenderModel`을 취득할 수 있다.

---

## 메서드 구조

두 그룹의 11개 메서드 쌍. 모든 메서드 파라미터:

```
(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime,
 float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
```

### 그룹 1 — `dynamicOverride*()` (PlayerAPI 훅 → SM 위임)

PlayerAPI가 모델 메서드를 가로챌 때 호출하는 메서드. SM 애니메이션으로 전달.

| 메서드 | 위임 대상 |
|--------|-----------|
| `dynamicOverrideAnimateHeadRotation(...)` | `getMovingModel().animateHeadRotation(...)` |
| `dynamicOverrideAnimateSleeping(...)` | `getMovingModel().animateSleeping(...)` |
| `dynamicOverrideAnimateArmSwinging(...)` | `getMovingModel().animateArmSwinging(...)` |
| `dynamicOverrideAnimateRiding(...)` | `getMovingModel().animateRiding(...)` |
| `dynamicOverrideAnimateLeftArmItemHolding(...)` | `getMovingModel().animateLeftArmItemHolding(...)` |
| `dynamicOverrideAnimateRightArmItemHolding(...)` | `getMovingModel().animateRightArmItemHolding(...)` |
| `dynamicOverrideAnimateWorkingBody(...)` | `getMovingModel().animateWorkingBody(...)` |
| `dynamicOverrideAnimateWorkingArms(...)` | `getMovingModel().animateWorkingArms(...)` |
| `dynamicOverrideAnimateSneaking(...)` | `getMovingModel().animateSneaking(...)` |
| `dynamicOverrideAnimateArms(...)` | `getMovingModel().animateArms(...)` |
| `dynamicOverrideAnimateBowAiming(...)` | `getMovingModel().animateBowAiming(...)` |

`dynamicOverride` 접두사는 ModelPlayerAPI 규약. PlayerAPI가 이 이름으로 호출 대상을 식별하여 원본 메서드를 대체한다.

### 그룹 2 — `superAnimate*()` (IModelPlayer 구현 → PlayerAPI dynamic 디스패치)

SM `IModelPlayer` 인터페이스 구현. `super.dynamic("메서드명", args)`를 통해 PlayerAPI 체인의 다음 단계(또는 vanilla 원본)를 호출.

| 메서드 | `super.dynamic()` 인자 |
|--------|------------------------|
| `superAnimateHeadRotation(...)` | `"animateHeadRotation"` |
| `superAnimateSleeping(...)` | `"animateSleeping"` |
| `superAnimateArmSwinging(...)` | `"animateArmSwinging"` |
| `superAnimateRiding(...)` | `"animateRiding"` |
| `superAnimateLeftArmItemHolding(...)` | `"animateLeftArmItemHolding"` |
| `superAnimateRightArmItemHolding(...)` | `"animateRightArmItemHolding"` |
| `superAnimateWorkingBody(...)` | `"animateWorkingBody"` |
| `superAnimateWorkingArms(...)` | `"animateWorkingArms"` |
| `superAnimateSneaking(...)` | `"animateSneaking"` |
| `superApplyAnimationOffsets(...)` | `"animateArms"` ← **이름 불일치** |
| `superAnimateBowAiming(...)` | `"animateBowAiming"` |

**`super.dynamic(String, Object[])` 동작**: PlayerAPI의 동적 디스패치 메서드. 현재 PlayerBase 체인에서 이 PlayerBase보다 하위 우선순위의 처리기(또는 vanilla 원본)를 호출.

**`superApplyAnimationOffsets` → `"animateArms"` 이름 불일치**: `ModelPlayer`(독립형)에서도 동일하게 `super.animateArms(...)` 호출. SR `ModelPlayer`에서 `animateArms()`가 오프셋 적용 역할.

---

## `@Deprecated` getter 메서드 (16개)

`SmartRenderModel`의 뼈대 노드를 직접 노출하는 deprecated getter. 외부에서 뼈대에 접근하던 과거 API 호환용.

```java
@Deprecated public ModelRenderer getOuter()        { return getMovingModel().md.bipedOuter; }
@Deprecated public ModelRenderer getTorso()        { return getMovingModel().md.bipedTorso; }
@Deprecated public ModelRenderer getBody()         { return getMovingModel().md.bipedBody; }
@Deprecated public ModelRenderer getBreast()       { return getMovingModel().md.bipedBreast; }
@Deprecated public ModelRenderer getNeck()         { return getMovingModel().md.bipedNeck; }
@Deprecated public ModelRenderer getHead()         { return getMovingModel().md.bipedHead; }
@Deprecated public ModelRenderer getHeadwear()     { return getMovingModel().md.bipedHeadwear; }
@Deprecated public ModelRenderer getRightShoulder(){ return getMovingModel().md.bipedRightShoulder; }
@Deprecated public ModelRenderer getRightArm()     { return getMovingModel().md.bipedRightArm; }
@Deprecated public ModelRenderer getLeftShoulder() { return getMovingModel().md.bipedLeftShoulder; }
@Deprecated public ModelRenderer getLeftArm()      { return getMovingModel().md.bipedLeftArm; }
@Deprecated public ModelRenderer getPelvic()       { return getMovingModel().md.bipedPelvic; }
@Deprecated public ModelRenderer getRightLeg()     { return getMovingModel().md.bipedRightLeg; }
@Deprecated public ModelRenderer getLeftLeg()      { return getMovingModel().md.bipedLeftLeg; }
@Deprecated public ModelRenderer getEars()         { return getMovingModel().md.bipedEars; }
@Deprecated public ModelRenderer getCloak()        { return getMovingModel().md.bipedCloak; }
```

모두 `getMovingModel().md.*` 경로를 통해 `SmartRenderModel`의 `ModelRotationRenderer` 필드 반환.  
`md` = `SmartMovingModel`이 보유한 `net.smart.render.SmartRenderModel` 인스턴스.

---

## ModelPlayer(독립형)와의 비교

| 항목 | `net.smart.moving.render.ModelPlayer` | `SmartMovingModelPlayerBase` (이 파일) |
|------|---------------------------------------|----------------------------------------|
| 부모 클래스 | `net.smart.render.ModelPlayer` | `ModelPlayerBase` (PlayerAPI) |
| `model` 필드 | `final`, 생성자에서 초기화 | non-final, 지연 초기화 |
| SmartMovingModel 첫 인자 | `this` (자신이 SR IModelPlayer) | `SmartRender.getPlayerBase(modelPlayer)` |
| animate override 방식 | `@Override animate*()` | `dynamicOverride*()` (PlayerAPI 규약) |
| super 호출 방식 | `super.animate*(...)` | `super.dynamic("methodName", args)` |
| @Deprecated getter | 없음 | 16개 |

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `ModelPlayerBase` | 상속 — `modelPlayer` 필드, `dynamic()` 메서드 |
| `net.smart.moving.render.IModelPlayer` | 구현 — super 위임 콜백 경로 |
| `SmartMovingModel` | 지연 생성 보유 — SM 애니메이션 로직 |
| `SmartRender.getPlayerBase(modelPlayer)` | SmartMovingModel 첫 인자 — SR 모델 데이터 |

---

## 주요 관찰 사항

1. **지연 초기화 이유**: 생성자 시점(`ModelPlayerAPI` 초기화 중)에는 `modelPlayer`가 아직 완전히 초기화되지 않아 `SmartRender.getPlayerBase(modelPlayer)`가 null을 반환할 수 있다. 첫 `dynamicOverride*` 호출 시점에는 모든 PlayerBase가 등록되어 있으므로 그때 초기화.

2. **`super.dynamic()` 메커니즘**: PlayerAPI의 동적 디스패치. 문자열 메서드명으로 호출 체인을 탐색. `ModelPlayer`의 `super.animate*()`와 동일한 역할이지만, PlayerAPI 환경에서 다른 PlayerBase들의 처리를 거친 후 vanilla에 도달.

3. **`dynamicOverride` vs `@Override`**: PlayerAPI는 메서드 오버라이드를 `dynamicOverride` 접두사로 구분. Java의 `@Override`로는 PlayerAPI 훅을 등록할 수 없으므로 별도 네이밍 규약 사용.

4. **16개 @Deprecated getter**: `bipedNeck`이 포함되어 있고 `SmartMovingModel`의 직접 필드(isClimb 등)는 없음. 이 getter들은 외부 모드가 뼈대에 직접 접근하던 구버전 API 호환용.

5. **1.21.1 이식 관련**:
   - `ModelPlayerBase` + `dynamicOverride*` + `super.dynamic()` 전체가 Mixin `@Inject`/`@Redirect`로 대체
   - 지연 초기화 → Mixin 주입 시점에 초기화 가능
   - `SmartRender.getPlayerBase()` → SR Mixin 주입 객체 참조 방식으로 대체
   - `@Deprecated` getter → 제거 또는 Mixin accessor로 대체
   - `ModelRenderer` → `ModelPart` (1.21.1)
