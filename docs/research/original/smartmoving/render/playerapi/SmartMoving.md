# SmartMoving.java (net.smart.moving.render.playerapi) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/render/playerapi/SmartMoving.java  
패키지: `net.smart.moving.render.playerapi`  
종류: `abstract class`  
실행 위치: 클라이언트 (렌더링, PlayerAPI 등록)

주의: 같은 이름의 `net.smart.moving.playerapi.SmartMoving`(이동 PlayerAPI 등록)과 다른 파일.

---

## 전체 소스

```java
package net.smart.moving.render.playerapi;

import api.player.model.*;
import api.player.render.*;

import net.smart.moving.*;
import net.smart.render.playerapi.*;

public abstract class SmartMoving
{
    public static final String ID = SmartMovingInfo.ModName;

    public static void register()
    {
        String[] inferiors = new String[] { SmartRender.ID };

        RenderPlayerBaseSorting renderSorting = new RenderPlayerBaseSorting();
        renderSorting.setAfterLocalConstructingInferiors(inferiors);
        renderSorting.setOverrideRenderPlayerInferiors(inferiors);
        renderSorting.setOverrideRotatePlayerInferiors(inferiors);
        renderSorting.setOverrideRenderPlayerSleepInferiors(inferiors);
        RenderPlayerAPI.register(ID, SmartMovingRenderPlayerBase.class, renderSorting);

        ModelPlayerBaseSorting modelSorting = new ModelPlayerBaseSorting();
        modelSorting.setAfterLocalConstructingInferiors(inferiors);
        ModelPlayerAPI.register(ID, SmartMovingModelPlayerBase.class, modelSorting);
    }

    public static SmartMovingRenderPlayerBase getPlayerBase(net.minecraft.client.renderer.entity.RenderPlayer renderPlayer)
    {
        return (SmartMovingRenderPlayerBase)((IRenderPlayerAPI)renderPlayer).getRenderPlayerBase(ID);
    }

    public static SmartMovingModelPlayerBase getPlayerBase(api.player.model.ModelPlayer modelPlayer)
    {
        return (SmartMovingModelPlayerBase)((IModelPlayerAPI)modelPlayer).getModelPlayerBase(ID);
    }
}
```

---

## 역할

`net.smart.moving.render.playerapi` 패키지의 PlayerAPI 등록 및 조회 진입점. `abstract`이므로 인스턴스화 불가, 모든 멤버가 static.

RenderPlayerAPI와 ModelPlayerAPI 두 곳에 SM을 등록하며, SmartRender보다 나중에(inferior) 실행되도록 순서를 설정한다.

---

## import

```java
import api.player.model.*;   // ModelPlayerBaseSorting, ModelPlayerAPI, IModelPlayerAPI, ModelPlayer
import api.player.render.*;  // RenderPlayerBaseSorting, RenderPlayerAPI, IRenderPlayerAPI

import net.smart.moving.*;         // SmartMovingInfo
import net.smart.render.playerapi.*; // SmartRender (ID 참조용)
```

---

## 정적 상수

```java
public static final String ID = SmartMovingInfo.ModName;
```

`SmartMovingInfo.ModName` = `"Smart Moving"` (SmartMovingInfo 리서치에서 확인됨).  
RenderPlayerAPI와 ModelPlayerAPI 양쪽 등록 키 및 PlayerBase 조회 키로 사용.

---

## `register()` (static)

```java
public static void register()
{
    String[] inferiors = new String[] { SmartRender.ID };

    RenderPlayerBaseSorting renderSorting = new RenderPlayerBaseSorting();
    renderSorting.setAfterLocalConstructingInferiors(inferiors);
    renderSorting.setOverrideRenderPlayerInferiors(inferiors);
    renderSorting.setOverrideRotatePlayerInferiors(inferiors);
    renderSorting.setOverrideRenderPlayerSleepInferiors(inferiors);
    RenderPlayerAPI.register(ID, SmartMovingRenderPlayerBase.class, renderSorting);

    ModelPlayerBaseSorting modelSorting = new ModelPlayerBaseSorting();
    modelSorting.setAfterLocalConstructingInferiors(inferiors);
    ModelPlayerAPI.register(ID, SmartMovingModelPlayerBase.class, modelSorting);
}
```

**inferiors**: `{ SmartRender.ID }` — SM이 SR보다 후순위(inferior)로 실행됨.

**RenderPlayerAPI 등록**:

| 소팅 설정 | 의미 |
|-----------|------|
| `setAfterLocalConstructingInferiors(inferiors)` | 생성자에서 SR 이후에 실행 |
| `setOverrideRenderPlayerInferiors(inferiors)` | `renderPlayer` 오버라이드에서 SR 이후 |
| `setOverrideRotatePlayerInferiors(inferiors)` | `rotatePlayer` 오버라이드에서 SR 이후 |
| `setOverrideRenderPlayerSleepInferiors(inferiors)` | `renderPlayerSleep` 오버라이드에서 SR 이후 |

등록 클래스: `SmartMovingRenderPlayerBase`

**ModelPlayerAPI 등록**:

| 소팅 설정 | 의미 |
|-----------|------|
| `setAfterLocalConstructingInferiors(inferiors)` | 생성자에서 SR 이후에 실행 |

등록 클래스: `SmartMovingModelPlayerBase`

RenderPlayerAPI에는 4개의 소팅 설정, ModelPlayerAPI에는 1개. 모델 측은 생성자 순서만 제어.

---

## `getPlayerBase()` (static, 오버로드 2개)

### RenderPlayer 버전

```java
public static SmartMovingRenderPlayerBase getPlayerBase(net.minecraft.client.renderer.entity.RenderPlayer renderPlayer)
{
    return (SmartMovingRenderPlayerBase)((IRenderPlayerAPI)renderPlayer).getRenderPlayerBase(ID);
}
```

`RenderPlayer` → `IRenderPlayerAPI` 캐스팅 → `getRenderPlayerBase(ID)` → `SmartMovingRenderPlayerBase` 캐스팅.

### ModelPlayer 버전

```java
public static SmartMovingModelPlayerBase getPlayerBase(api.player.model.ModelPlayer modelPlayer)
{
    return (SmartMovingModelPlayerBase)((IModelPlayerAPI)modelPlayer).getModelPlayerBase(ID);
}
```

`api.player.model.ModelPlayer` → `IModelPlayerAPI` 캐스팅 → `getModelPlayerBase(ID)` → `SmartMovingModelPlayerBase` 캐스팅.

---

## net.smart.moving.playerapi.SmartMoving과의 비교

| 항목 | `net.smart.moving.playerapi.SmartMoving` | `net.smart.moving.render.playerapi.SmartMoving` (이 파일) |
|------|------------------------------------------|----------------------------------------------------------|
| 등록 대상 | ClientPlayerAPI + ServerPlayerAPI | RenderPlayerAPI + ModelPlayerAPI |
| inferiors | `{ SmartRender.ID }` | `{ SmartRender.ID }` |
| 소팅 설정 수 | 클라이언트 3 / 서버 1 | 렌더 4 / 모델 1 |
| getPlayerBase 오버로드 | EntityPlayerSP용 | RenderPlayer용, ModelPlayer용 |

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingInfo.ModName` | ID 값 |
| `SmartRender.ID` | inferiors 배열 — SR보다 후순위 설정 |
| `RenderPlayerAPI` | 렌더 PlayerBase 등록 |
| `ModelPlayerAPI` | 모델 PlayerBase 등록 |
| `SmartMovingRenderPlayerBase` | 등록 클래스 + getPlayerBase 반환 타입 |
| `SmartMovingModelPlayerBase` | 등록 클래스 + getPlayerBase 반환 타입 |
| `IRenderPlayerAPI` | RenderPlayer에서 PlayerBase 조회 |
| `IModelPlayerAPI` | ModelPlayer에서 PlayerBase 조회 |

---

## 주요 관찰 사항

1. **SM이 SR의 inferior**: `inferiors = { SmartRender.ID }` — 모든 소팅 설정에서 SR이 먼저, SM이 나중. SmartRender가 기반 애니메이션을 설정한 후 SM이 그 위에 덮어쓰는 구조.

2. **두 PlayerAPI 동시 등록**: RenderPlayerAPI(렌더)와 ModelPlayerAPI(모델) 양쪽에 등록. `net.smart.moving.playerapi.SmartMoving`이 이동 PlayerAPI를 담당하고, 이 파일이 렌더 PlayerAPI를 담당.

3. **RenderPlayerAPI 소팅 4개 vs ModelPlayerAPI 소팅 1개**: 렌더 측은 renderPlayer/rotatePlayer/renderPlayerSleep 세 가지 오버라이드의 순서를 모두 제어. 모델 측은 생성자 순서만 제어.

4. **`abstract` + all-static 패턴**: `net.smart.moving.playerapi.SmartMoving`과 동일한 패턴. 인스턴스화 방지용 abstract.

5. **1.21.1 이식 관련**:
   - RenderPlayerAPI / ModelPlayerAPI → Mixin으로 대체
   - `inferiors` 순서 제어 → Mixin `@Inject` / `@Redirect` 우선순위로 대체
   - `IRenderPlayerAPI` / `IModelPlayerAPI` 캐스팅 → 사라짐
   - `register()` 호출 위치 → `ClientModInitializer.onInitializeClient()`
