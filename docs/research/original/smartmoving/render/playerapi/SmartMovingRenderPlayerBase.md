# SmartMovingRenderPlayerBase.java (net.smart.moving.render.playerapi) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/render/playerapi/SmartMovingRenderPlayerBase.java  
패키지: `net.smart.moving.render.playerapi`  
종류: `class`  
상속: `RenderPlayerBase` (extends) + `net.smart.moving.render.IRenderPlayer` (implements)  
실행 위치: 클라이언트 (렌더링, PlayerAPI)

---

## 전체 소스

소스는 GitHub 링크 참조.

---

## 역할

RenderPlayerAPI 기반 `SmartMovingRender` 연결 클래스. PlayerAPI 경로에서 렌더 메서드를 가로채어 SM 렌더 로직에 전달하고, SM이 vanilla 원본을 호출할 때(`superRender*`) PlayerAPI의 `super.*` 경로로 처리한다.

`RenderPlayer`(독립형)와 동일한 역할을 PlayerAPI 경로에서 담당한다.

---

## import

```java
import net.minecraft.client.entity.*;          // AbstractClientPlayer
import net.minecraft.client.renderer.entity.*; // RenderManager
import net.minecraft.entity.*;                 // EntityLivingBase

import api.player.render.*;                    // RenderPlayerBase, RenderPlayerAPI

import net.smart.moving.render.*;              // SmartMovingRender, IRenderPlayer, IModelPlayer
import net.smart.moving.render.IRenderPlayer;  // 명시적 import
```

---

## 상속/구현 관계

```
RenderPlayerBase  (api.player.render)
    └─ SmartMovingRenderPlayerBase  (이 파일)
         implements net.smart.moving.render.IRenderPlayer
```

---

## 필드

```java
private api.player.model.ModelPlayer[] allModelPlayers;  // getPlayerModels() 캐시 비교용
private IModelPlayer[] allIModelPlayers;                 // getPlayerModels() 캐시 반환값

private SmartMovingRender render;                        // 지연 초기화, non-final
```

---

## 생성자

```java
public SmartMovingRenderPlayerBase(RenderPlayerAPI renderPlayerAPI)
{
    super(renderPlayerAPI);
}
```

`RenderPlayerBase(RenderPlayerAPI)` 위임만. 렌더 초기화는 지연.

---

## `getRenderModel()` (지연 초기화)

```java
public SmartMovingRender getRenderModel()
{
    if(render == null)
        render = new SmartMovingRender(this);
    return render;
}
```

첫 호출 시 `new SmartMovingRender(this)` 생성. `this`가 `IRenderPlayer` 역할.  
`RenderPlayer`(독립형)는 생성자에서 즉시 `render = new SmartMovingRender(this)`.

---

## 메서드 구조

### 그룹 1 — PlayerAPI 훅 → SM 위임

RenderPlayerBase PlayerAPI 훅 메서드들을 오버라이드하여 `getRenderModel().*`로 전달.

| PlayerAPI 훅 메서드 | 위임 대상 |
|--------------------|-----------|
| `renderPlayer(AbstractClientPlayer, double, double, double, float, float)` | `getRenderModel().renderPlayer(...)` |
| `rotatePlayer(AbstractClientPlayer, float, float, float)` | `getRenderModel().rotatePlayer(...)` |
| `renderPlayerSleep(AbstractClientPlayer, double, double, double)` | `getRenderModel().renderPlayerAt(...)` |
| `passSpecialRender(EntityLivingBase, double, double, double)` | `getRenderModel().renderName(...)` |

**`renderPlayerSleep` → `renderPlayerAt` 주의**: `RenderPlayer`(독립형)에서는 `renderLivingAt` → `renderPlayerAt`이었지만, PlayerAPI에서는 `renderPlayerSleep` → `renderPlayerAt`. 같은 SM 메서드에 다른 vanilla 훅이 매핑됨.

### 그룹 2 — `superRender*()` (IRenderPlayer 구현 → PlayerAPI super 호출)

| IRenderPlayer 메서드 | 호출 대상 |
|---------------------|-----------|
| `superRenderRenderPlayer(AbstractClientPlayer, double, double, double, float, float)` | `super.renderPlayer(...)` |
| `superRenderRotatePlayer(AbstractClientPlayer, float, float, float)` | `super.rotatePlayer(...)` |
| `superRenderRenderPlayerAt(AbstractClientPlayer, double, double, double)` | `super.renderPlayerSleep(...)` |
| `superRenderRenderName(EntityLivingBase, double, double, double)` | `super.passSpecialRender(...)` |

**vanilla 메서드명 매핑**:

| IRenderPlayer 이름 | PlayerAPI 훅 이름 | RenderPlayer(독립형) vanilla 이름 |
|-------------------|------------------|---------------------------------|
| `superRenderRenderPlayer` | `renderPlayer` | `doRender` |
| `superRenderRotatePlayer` | `rotatePlayer` | `rotateCorpse` |
| `superRenderRenderPlayerAt` | `renderPlayerSleep` | `renderLivingAt` |
| `superRenderRenderName` | `passSpecialRender` | `passSpecialRender` |

PlayerAPI 경로와 독립형 경로에서 `renderPlayerAt`에 매핑되는 vanilla 메서드명이 다름: PlayerAPI는 `renderPlayerSleep`, 독립형은 `renderLivingAt`.

---

## `getRenderManager()` (IRenderPlayer 구현)

```java
@Override
public RenderManager getRenderManager()
{
    return renderPlayerAPI.getRenderManagerField();
}
```

독립형 `RenderPlayer`는 상속받은 `renderManager` 필드를 직접 반환했지만, PlayerAPI 환경에서는 `renderPlayerAPI.getRenderManagerField()`를 통해 접근.

---

## `isRenderedWithBodyTopAlwaysInAccelerateDirection()`

```java
public boolean isRenderedWithBodyTopAlwaysInAccelerateDirection()
{
    SmartMovingRender render = getRenderModel();
    return render.modelBipedMain.isFlying
        || render.modelBipedMain.isSwim
        || render.modelBipedMain.isDive
        || render.modelBipedMain.isHeadJump;
}
```

**역할**: 몸통 상단이 항상 가속 방향을 향하는 상태인지 반환.

조건: `isFlying || isSwim || isDive || isHeadJump` — 이 4가지 상태에서는 렌더러가 body top을 이동 방향으로 고정.

이 메서드는 `IRenderPlayer` 인터페이스에 없음 — PlayerAPI 환경에서 외부(예: SmartRender RenderPlayerBase)가 직접 이 클래스를 참조하여 호출하는 용도.

---

## `getPlayerModel*()` (IRenderPlayer 구현)

```java
@Override
public IModelPlayer getPlayerModelArmor()
{
    return SmartMoving.getPlayerBase((api.player.model.ModelPlayer)renderPlayerAPI.getModelArmorField());
}

@Override
public IModelPlayer getPlayerModelArmorChestplate()
{
    return SmartMoving.getPlayerBase((api.player.model.ModelPlayer)renderPlayerAPI.getModelArmorChestplateField());
}

@Override
public IModelPlayer getPlayerModelBipedMain()
{
    return SmartMoving.getPlayerBase((api.player.model.ModelPlayer)renderPlayerAPI.getModelBipedMainField());
}
```

`renderPlayerAPI.getModel*Field()` → `api.player.model.ModelPlayer` 캐스팅 → `SmartMoving.getPlayerBase()` → `SmartMovingModelPlayerBase`.

독립형 `RenderPlayer`는 `super.getModel*()` → `(ModelPlayer)` 캐스팅이었지만, PlayerAPI 경로에서는 `renderPlayerAPI` 필드 getter를 사용.

---

## `getPlayerModels()` (IRenderPlayer 구현)

```java
@Override
public IModelPlayer[] getPlayerModels()
{
    api.player.model.ModelPlayer[] modelPlayers = api.player.model.ModelPlayerAPI.getAllInstances();
    if(allModelPlayers != null && (allModelPlayers == modelPlayers || modelPlayers.length == 0 && allModelPlayers.length == 0))
        return allIModelPlayers;

    allModelPlayers = modelPlayers;
    allIModelPlayers = new IModelPlayer[modelPlayers.length];
    for(int i=0; i<allIModelPlayers.length; i++)
        allIModelPlayers[i] = SmartMoving.getPlayerBase(allModelPlayers[i]);
    return allIModelPlayers;
}
```

독립형 `RenderPlayer`는 항상 3개(`bipedMain, armorChestplate, armor`) 고정 배열이었지만, PlayerAPI 경로에서는 `ModelPlayerAPI.getAllInstances()`로 모든 ModelPlayer 레이어를 동적으로 조회.

**캐싱 조건** (`allIModelPlayers` 재사용):
```java
allModelPlayers != null
  && (allModelPlayers == modelPlayers            // 배열 동일 참조
      || modelPlayers.length == 0 && allModelPlayers.length == 0)  // 둘 다 비어있음
```

`==` 비교: 배열 인스턴스가 동일하면 캐시 유효. `ModelPlayerAPI.getAllInstances()`가 동일 인스턴스를 반환하는 동안은 재구성 안 함.

**배열 재구성**: 새 `ModelPlayer[]`이면 `allIModelPlayers` 배열을 새로 생성, 각 원소를 `SmartMoving.getPlayerBase()`로 변환.

---

## RenderPlayer(독립형)와의 비교

| 항목 | `net.smart.moving.render.RenderPlayer` | `SmartMovingRenderPlayerBase` (이 파일) |
|------|----------------------------------------|----------------------------------------|
| 부모 클래스 | `net.smart.render.RenderPlayer` | `RenderPlayerBase` (PlayerAPI) |
| `render` 초기화 | 생성자에서 즉시 | 지연 초기화(`getRenderModel()`) |
| PlayerAPI 훅 접두사 | `@Override doRender` 등 | `renderPlayer`/`rotatePlayer`/`renderPlayerSleep`/`passSpecialRender` |
| renderPlayerAt 매핑 | `renderLivingAt` | `renderPlayerSleep` |
| `getRenderManager()` | `return renderManager` | `return renderPlayerAPI.getRenderManagerField()` |
| `getPlayerModel*()` | `(ModelPlayer)super.getModel*()` | `SmartMoving.getPlayerBase(renderPlayerAPI.getModel*Field())` |
| `getPlayerModels()` | 3개 고정 배열, null 체크 지연 초기화 | `ModelPlayerAPI.getAllInstances()` 동적 조회, 배열 참조 비교 캐싱 |
| 추가 메서드 | 없음 | `isRenderedWithBodyTopAlwaysInAccelerateDirection()` |

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `RenderPlayerBase` | 상속 — `renderPlayerAPI` 필드, `super.*` 원본 호출 |
| `net.smart.moving.render.IRenderPlayer` | 구현 — SM 렌더 콜백 경로 |
| `SmartMovingRender` | 지연 생성 보유 — SM 렌더 로직 |
| `SmartMoving.getPlayerBase(ModelPlayer)` | `getPlayerModel*()` / `getPlayerModels()` 변환 |
| `ModelPlayerAPI.getAllInstances()` | 전체 모델 레이어 동적 조회 |
| `renderPlayerAPI.getRenderManagerField()` | RenderManager 접근 |
| `renderPlayerAPI.getModel*Field()` | 개별 모델 레이어 접근 |

---

## 주요 관찰 사항

1. **지연 초기화 이유**: `SmartMovingModelPlayerBase`와 동일. 생성자 시점에 SM PlayerBase가 아직 등록되지 않았을 수 있어, 첫 `renderPlayer()` 호출 시점에 초기화.

2. **`renderPlayerSleep` vs `renderLivingAt`**: PlayerAPI RenderPlayerBase에서는 위치 보정 훅이 `renderPlayerSleep`으로 노출됨. 독립형 SR `RenderPlayer`의 `renderLivingAt`과 다른 이름이지만 `SmartMovingRender.renderPlayerAt()`에 동일하게 연결됨.

3. **`getPlayerModels()` 동적 조회**: 독립형은 항상 3개 고정. PlayerAPI 환경에서는 등록된 모든 ModelPlayer 레이어를 동적으로 가져옴. 갑옷 레이어 수가 다를 수 있는 환경 대응.

4. **`isRenderedWithBodyTopAlwaysInAccelerateDirection()`**: `IRenderPlayer` 외부에 존재하는 메서드 — SmartRenderRenderPlayerBase 등 외부에서 이 클래스 타입으로 직접 호출하는 것으로 추정(확인 필요).

5. **1.21.1 이식 관련**:
   - `RenderPlayerBase` + PlayerAPI 훅 → Mixin `@Inject` 대체
   - `renderPlayerAPI.getRenderManagerField()` → `MinecraftClient.getInstance().getEntityRenderDispatcher()`
   - `ModelPlayerAPI.getAllInstances()` → Fabric Armor Feature Layer 시스템으로 대체
   - `renderPlayerSleep` 대응 → 1.21.1에서 `render()` 메서드 내 위치 변환 부분 Mixin
   - `isRenderedWithBodyTopAlwaysInAccelerateDirection()` → SM 내부 상태 직접 접근으로 대체
