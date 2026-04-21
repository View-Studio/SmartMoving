# SmartRender.java (net.smart.render.playerapi) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/playerapi/SmartRender.java  
패키지: `net.smart.render.playerapi`  
종류: `abstract class` (상속 없음, 인스턴스화 방지용)

---

## 역할

PlayerAPI에 SmartRender의 두 PlayerBase 구현체를 등록하는 static helper 클래스.  
`SmartRenderMod.init()`에서 reflection으로 `register()`를 호출한다.  
모든 메서드가 static이므로 인스턴스 생성 없이 사용한다. `abstract`는 인스턴스화를 방지하기 위한 선언.

- **실행 위치**: 클라이언트 전용 (PlayerAPI는 클라이언트 전용)

---

## 필드

```java
public static final String ID = SmartRenderInfo.ModName;   // = "Smart Render"
```

PlayerAPI에서 이 모드의 PlayerBase를 식별하는 ID 문자열.

---

## 메서드 전체

### `register()` — static

```java
public static void register()
{
    RenderPlayerAPI.register(ID, SmartRenderRenderPlayerBase.class);
    ModelPlayerAPI.register(ID, SmartRenderModelPlayerBase.class);
}
```

- `RenderPlayerAPI.register("Smart Render", SmartRenderRenderPlayerBase.class)` — 렌더러 PlayerBase 등록
- `ModelPlayerAPI.register("Smart Render", SmartRenderModelPlayerBase.class)` — 모델 PlayerBase 등록
- **호출 위치**: `SmartRenderMod.init()`에서 reflection으로 호출:
  ```java
  Class<?> type = Reflect.LoadClass(SmartRenderMod.class,
      new Name("net.smart.render.playerapi.SmartRender"), true);
  Method method = Reflect.GetMethod(type, new Name("register"));
  Reflect.Invoke(method, null);
  ```

---

### `getPlayerBase(net.minecraft.client.renderer.entity.RenderPlayer renderPlayer)` — static

```java
public static SmartRenderRenderPlayerBase getPlayerBase(
    net.minecraft.client.renderer.entity.RenderPlayer renderPlayer)
{
    return (SmartRenderRenderPlayerBase)
        ((IRenderPlayerAPI)renderPlayer).getRenderPlayerBase(ID);
}
```

- vanilla `RenderPlayer`를 `IRenderPlayerAPI`로 캐스트한 뒤, ID로 등록된 `SmartRenderRenderPlayerBase` 인스턴스를 조회

---

### `getPlayerBase(api.player.model.ModelPlayer renderPlayer)` — static

```java
public static SmartRenderModelPlayerBase getPlayerBase(
    api.player.model.ModelPlayer renderPlayer)
{
    return (SmartRenderModelPlayerBase)
        ((IModelPlayerAPI)renderPlayer).getModelPlayerBase(ID);
}
```

- PlayerAPI의 `ModelPlayer`를 `IModelPlayerAPI`로 캐스트한 뒤, ID로 등록된 `SmartRenderModelPlayerBase` 인스턴스를 조회
- 파라미터 타입이 위 오버로드와 다름: `api.player.model.ModelPlayer` vs `net.minecraft.client.renderer.entity.RenderPlayer`

---

## import

```java
import api.player.model.*;    // ModelPlayerAPI, IModelPlayerAPI, api.player.model.ModelPlayer
import api.player.render.*;   // RenderPlayerAPI, IRenderPlayerAPI
import net.smart.render.*;    // SmartRenderInfo, SmartRenderRenderPlayerBase, SmartRenderModelPlayerBase
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartRenderInfo.ModName` | ID = "Smart Render" |
| `RenderPlayerAPI` (PlayerAPI) | 렌더러 PlayerBase 등록 |
| `ModelPlayerAPI` (PlayerAPI) | 모델 PlayerBase 등록 |
| `IRenderPlayerAPI` (PlayerAPI) | getRenderPlayerBase() 호출을 위한 캐스트 |
| `IModelPlayerAPI` (PlayerAPI) | getModelPlayerBase() 호출을 위한 캐스트 |
| `SmartRenderRenderPlayerBase` | 렌더러 PlayerBase 구현체 |
| `SmartRenderModelPlayerBase` | 모델 PlayerBase 구현체 |

---

## 주요 관찰 사항

1. **반드시 reflection으로 호출**: `SmartRenderMod`는 PlayerAPI 없이도 로드될 수 있어야 하므로, 이 클래스를 컴파일 타임에 직접 참조하지 않고 reflection으로 동적 로드. PlayerAPI가 없으면 이 클래스 로딩 자체가 실패하므로 `Reflect.LoadClass(..., true)`(예외 무시)로 처리.

2. **ID = "Smart Render"** (SmartRenderInfo.ModName): PlayerAPI에서 모드를 식별하는 키. `SmartRenderInfo.ModId`("SmartRender")가 아닌 `ModName`("Smart Render") 사용.

3. **비-PlayerAPI 경로와의 분기**: `SmartRenderMod.init()`에서 `hasRenderer(= PlayerAPI 있음)`이면 이 클래스의 `register()`를 호출하고, `!hasRenderer && addRenderer`이면 `SmartRenderContext.registerRenderers(null)`(실질적으로 무효)을 호출.

4. **1.21.1 이식**: PlayerAPI 자체가 1.21.1에 존재하지 않음. 이 클래스 전체가 불필요. 1.21.1에서는 Mixin으로 vanilla RenderPlayer/ModelBiped를 직접 패치하는 방식으로 대체.
