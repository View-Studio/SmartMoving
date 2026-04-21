# RenderPlayer.java (net.smart.moving.render) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/render/RenderPlayer.java  
패키지: `net.smart.moving.render`  
종류: `class`  
상속: `net.smart.render.RenderPlayer` (extends) + `IRenderPlayer` (implements)  
실행 위치: 클라이언트 (렌더링)

주의: 같은 이름의 `net.smart.render.RenderPlayer`와 다른 파일.

---

## 전체 소스

소스는 GitHub 링크 참조.

---

## 역할

SmartRender의 `RenderPlayer`를 확장하면서 SM의 `IRenderPlayer`를 구현. `ModelPlayer`와 동일한 `(this, this)` 패턴으로 `SmartMovingRender`를 생성하여 렌더 파이프라인 훅과 super 콜백을 연결한다.

PlayerAPI 없는 독립형 렌더 클래스. PlayerAPI 경로에서는 `SmartMovingRenderPlayerBase`가 동일한 역할을 담당.

---

## import

```java
import net.minecraft.client.entity.*;          // AbstractClientPlayer
import net.minecraft.client.model.*;           // ModelBiped
import net.minecraft.client.renderer.entity.*; // RenderManager
import net.minecraft.entity.*;                 // EntityLivingBase
```

---

## 상속/구현 관계

```
net.smart.render.RenderPlayer  (SmartRender)
    └─ net.smart.moving.render.RenderPlayer  (이 파일)
         implements net.smart.moving.render.IRenderPlayer
```

`net.smart.render.RenderPlayer`는 `net.smart.render.IRenderPlayer`를 구현하므로:
- `this`는 `net.smart.render.IRenderPlayer` ← 상속으로 충족 (SmartMovingRender 생성자의 두 번째 인자 아님 — 실제로 SmartMovingRender는 `net.smart.moving.render.IRenderPlayer`만 받음)
- `this`는 `net.smart.moving.render.IRenderPlayer` ← 이 클래스가 직접 구현

---

## 필드

```java
private IModelPlayer[] allIModelPlayers;  // getPlayerModels() 지연 초기화 캐시

private final SmartMovingRender render;   // SM 렌더 로직 보유 (final)
```

---

## 생성자

```java
public RenderPlayer()
{
    render = new SmartMovingRender(this);
}
```

`this`를 `IRenderPlayer`로 전달 — `SmartMovingRender.irp = this`.  
`super()` 명시 없음 → `net.smart.render.RenderPlayer()` 묵시적 호출.

---

## 메서드 구조

`ModelPlayer`와 대칭되는 두 그룹 구조.

### 그룹 1 — vanilla 훅 → SM 위임

SR `RenderPlayer`의 protected/override 메서드를 오버라이드하여 `render.*`로 전달.

| 오버라이드 메서드 | 위임 대상 |
|------------------|-----------|
| `doRender(AbstractClientPlayer, double, double, double, float, float)` | `render.renderPlayer(...)` |
| `rotateCorpse(AbstractClientPlayer, float, float, float)` | `render.rotatePlayer(...)` |
| `renderLivingAt(AbstractClientPlayer, double, double, double)` | `render.renderPlayerAt(...)` |
| `passSpecialRender(EntityLivingBase, double, double, double)` | `render.renderName(...)` |

### 그룹 2 — `super*()` (IRenderPlayer 구현 → SR super 호출)

`IRenderPlayer`의 `superRender*()` 메서드를 구현하여 SR `RenderPlayer` 원본을 호출.

| IRenderPlayer 메서드 | 호출 대상 |
|----------------------|-----------|
| `superRenderRenderPlayer(AbstractClientPlayer, double, double, double, float, float)` | `super.doRender(...)` |
| `superRenderRotatePlayer(AbstractClientPlayer, float, float, float)` | `super.rotateCorpse(...)` |
| `superRenderRenderPlayerAt(AbstractClientPlayer, double, double, double)` | `super.renderLivingAt(...)` |
| `superRenderRenderName(EntityLivingBase, double, double, double)` | `super.passSpecialRender(...)` |

**vanilla 메서드명 매핑**:

| SM IRenderPlayer 이름 | 실제 vanilla/SR 메서드명 |
|-----------------------|-------------------------|
| `superRenderRenderPlayer` | `doRender` |
| `superRenderRotatePlayer` | `rotateCorpse` |
| `superRenderRenderPlayerAt` | `renderLivingAt` |
| `superRenderRenderName` | `passSpecialRender` |

### `createModel()` (override)

```java
@Override
public net.smart.render.IModelPlayer createModel(ModelBiped existing, float f)
{
    return new ModelPlayer(f);
}
```

SR `RenderPlayer`의 모델 팩토리 메서드 오버라이드. SR의 기본 `SmartRenderModel` 대신 SM의 `ModelPlayer`를 생성. `existing` 파라미터는 사용하지 않는다.

반환 타입이 `net.smart.render.IModelPlayer`이지만 실제 반환 객체는 `net.smart.moving.render.ModelPlayer`(두 인터페이스 모두 구현).

### `getRenderManager()`

```java
@Override
public RenderManager getRenderManager()
{
    return renderManager;
}
```

상속받은 `renderManager` 필드를 그대로 노출.

### `getPlayerModel*()` (IRenderPlayer 구현)

```java
@Override
public IModelPlayer getPlayerModelBipedMain()
{
    return (ModelPlayer)super.getModelBipedMain();
}

@Override
public IModelPlayer getPlayerModelArmorChestplate()
{
    return (ModelPlayer)super.getModelArmorChestplate();
}

@Override
public IModelPlayer getPlayerModelArmor()
{
    return (ModelPlayer)super.getModelArmor();
}
```

SR `RenderPlayer`의 `getModel*()` getter를 호출하여 `ModelPlayer`로 캐스팅.  
캐스팅이 안전한 이유: `createModel()`이 항상 `ModelPlayer`를 반환하므로 SR이 보관하는 모델도 반드시 `ModelPlayer` 인스턴스.

### `getPlayerModels()` (IRenderPlayer 구현)

```java
@Override
public IModelPlayer[] getPlayerModels()
{
    if(allIModelPlayers == null)
        allIModelPlayers = new IModelPlayer[] {
            getPlayerModelBipedMain(),
            getPlayerModelArmorChestplate(),
            getPlayerModelArmor()
        };
    return allIModelPlayers;
}
```

3개 모델([0]=bipedMain, [1]=armorChestplate, [2]=armor)을 배열로 반환.  
지연 초기화: 첫 호출 시 배열 생성 후 캐싱. 이후 호출은 캐시 반환.

---

## 호출 흐름

```
vanilla 렌더링 파이프라인
    │ doRender() 호출
    ▼
net.smart.moving.render.RenderPlayer.doRender()
    │
    ▼
SmartMovingRender.renderPlayer()
    │ 상태 설정 후
    ▼
irp.superRenderRenderPlayer()  [IRenderPlayer 콜백]
    │
    ▼
net.smart.moving.render.RenderPlayer.superRenderRenderPlayer()
    │
    ▼
super.doRender()  [SR RenderPlayer 원본 = vanilla 렌더링]
```

`rotateCorpse`, `renderLivingAt`, `passSpecialRender`도 동일한 패턴.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `net.smart.render.RenderPlayer` | 상속 — vanilla 렌더 원본, `getModel*()`, `renderManager` 필드 |
| `net.smart.moving.render.IRenderPlayer` | 구현 — SM 렌더 콜백 경로 |
| `SmartMovingRender` | 보유 — SM 렌더 로직 |
| `net.smart.moving.render.ModelPlayer` | `createModel()` 반환 타입, `getPlayerModel*()` 캐스팅 대상 |

---

## 주요 관찰 사항

1. **ModelPlayer와 대칭 구조**: `ModelPlayer`가 모델 파이프라인(animate 계열)을 중개하듯, `RenderPlayer`는 렌더 파이프라인(doRender/rotateCorpse 등)을 중개. 두 파일 모두 `(this, this)` 또는 `this`를 SM 로직 객체 생성자에 전달하는 패턴.

2. **`createModel()` 오버라이드**: SR의 모델 생성 팩토리를 오버라이드하여 SM `ModelPlayer`를 반환. 이로 인해 SR이 내부적으로 보관하는 모든 모델이 `ModelPlayer` 인스턴스가 되어 `getPlayerModel*()`의 캐스팅이 안전.

3. **`allIModelPlayers` 지연 초기화**: null 체크 후 첫 호출 시 배열 생성. 생성자에서 바로 만들지 않는 이유는 모델이 생성자 시점에 아직 초기화되지 않았을 수 있기 때문.

4. **vanilla 메서드명 은닉**: `doRender` → `renderPlayer`, `rotateCorpse` → `rotatePlayer` 등 SM 측 네이밍으로 래핑. vanilla 메서드명은 `super*()` 구현 내부에서만 노출.

5. **1.21.1 이식 관련**:
   - PlayerAPI 없는 경로이므로 Mixin으로 직접 대체 가능
   - `doRender` → `render()` (1.21.1 EntityRenderer)
   - `rotateCorpse` → `setupTransforms()` (1.21.1 LivingEntityRenderer)
   - `renderLivingAt` → 위치 변환은 `render()` 내부로 통합됨
   - `passSpecialRender` → `renderLabelIfPresent()` 또는 `renderName()` (1.21.1)
   - `renderManager.livingPlayer` → `MinecraftClient.getInstance().player`
   - `allIModelPlayers` 배열 — Fabric Armor Layer 시스템과 연동 방식 재설계 필요
