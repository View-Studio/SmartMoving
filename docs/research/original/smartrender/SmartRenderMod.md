# SmartRenderMod.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/SmartRenderMod.java  
패키지: `net.smart.render`  
종류: FML `@Mod` 클래스 (진입점)

---

## 어노테이션

```java
@Mod(
    modid       = "SmartRender",
    name        = "Smart Render",
    version     = "@VERSION@",
    dependencies = "required-after:PlayerAPI@[1.3,)"
)
```

- `modid` = `"SmartRender"` ← SmartRenderInfo.ModId 실제 값 확인
- `name` = `"Smart Render"` ← SmartRenderInfo.ModName 실제 값 확인
- `version` = `"@VERSION@"` — 빌드 시 치환 ← SmartRenderInfo.ModVersion 실제 값 확인
- `dependencies` = `"required-after:PlayerAPI@[1.3,)"` — PlayerAPI 1.3 이상 **필수 의존**

---

## 필드

```java
private static boolean addRenderer = true;   // 기본값 true
private boolean hasRenderer = false;          // PlayerAPI("RenderPlayerAPI") 로드 여부
```

---

## 메서드 전체

### `doNotAddRenderer()` — static

```java
public static void doNotAddRenderer()
{
    addRenderer = false;
}
```

- SmartMoving 등 외부 모드에서 자체 렌더러를 제공할 때 이 메서드를 호출하여 SmartRender의 렌더러 자동 등록을 억제
- `addRenderer = false`로 설정하면 `init()`의 렌더러 등록 조건이 false가 됨

---

### `init(FMLInitializationEvent event)` — @EventHandler

```java
@EventHandler
public void init(FMLInitializationEvent event)
{
    // 1. 클라이언트 사이드 체크
    if(!FMLCommonHandler.instance().getSide().isClient())
        return;

    // 2. PlayerAPI(RenderPlayerAPI) 로드 여부 확인
    hasRenderer = Loader.isModLoaded("RenderPlayerAPI");

    // 3. SmartStatistics PlayerAPI 등록
    net.smart.render.statistics.playerapi.SmartStatistics.register();

    // 4. PlayerAPI가 로드된 경우: SmartRender PlayerAPI 경로 등록
    if(hasRenderer)
    {
        Class<?> type = Reflect.LoadClass(SmartRenderMod.class,
            new Name("net.smart.render.playerapi.SmartRender"), true);
        Method method = Reflect.GetMethod(type, new Name("register"));
        Reflect.Invoke(method, null);
    }

    // 5. PlayerAPI 없고 addRenderer=true인 경우: 직접 렌더러 등록
    if(!hasRenderer && addRenderer)
        SmartRenderContext.registerRenderers(null);

    // 6. SmartStatisticsFactory 초기화
    net.smart.render.statistics.playerapi.SmartStatisticsFactory.initialize();

    // 7. 이벤트 버스 등록 (tickStart 수신용)
    FMLCommonHandler.instance().bus().register(this);
}
```

**단계별 상세:**

**단계 4 — PlayerAPI 경로 (reflection):**
- `net.smart.render.playerapi.SmartRender` 클래스를 reflection으로 로드
- `register()` 메서드를 reflection으로 호출
- `Reflect.LoadClass(SmartRenderMod.class, new Name("net.smart.render.playerapi.SmartRender"), true)` — 세 번째 인자 true의 의미는 Reflect 리서치 후 확인 필요 [미확인]
- 이 경로가 PlayerAPI를 통한 `SmartRenderModelPlayerBase`, `SmartRenderRenderPlayerBase` 등록 경로임

**단계 5 — 비-PlayerAPI 경로:**
- 조건: `!hasRenderer && addRenderer`
- `SmartRenderContext.registerRenderers(null)` 호출
- **코드 동작 분석**: `SmartRenderContext.registerRenderers(Class<?> type)`에서 `type = null`이면 `null.newInstance()` → `NullPointerException` → `catch (Exception e)` → `return`. 결과적으로 렌더러가 등록되지 않음.
- 인자 없는 `SmartRenderContext.registerRenderers()`(= `net.smart.render.RenderPlayer.class`를 넘기는 버전)와 다른 경로. null을 명시적으로 전달.

---

### `tickStart(ClientTickEvent event)` — @SubscribeEvent

```java
@SubscribeEvent
public void tickStart(ClientTickEvent event)
{
    SmartStatisticsContext.onTickInGame();
}
```

- 클라이언트 틱마다 호출
- `SmartStatisticsContext.onTickInGame()` — 이동 통계 틱 처리

---

## import 목록

```java
import java.lang.reflect.*;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.Mod.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.*;
import cpw.mods.fml.common.gameevent.TickEvent.*;
import net.smart.render.statistics.*;
import net.smart.utilities.*;
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| FML `@Mod`, `@EventHandler`, `@SubscribeEvent` | 모드 진입점 및 이벤트 |
| `FMLCommonHandler` | 사이드 확인, 이벤트 버스 등록 |
| `Loader.isModLoaded("RenderPlayerAPI")` | PlayerAPI 로드 여부 확인 |
| `Reflect` (net.smart.utilities) | PlayerAPI 경로 클래스/메서드 reflection 로드 |
| `Name` (net.smart.utilities) | 클래스/메서드 이름 래퍼 |
| `net.smart.render.statistics.playerapi.SmartStatistics` | PlayerAPI 통계 등록 |
| `net.smart.render.statistics.playerapi.SmartStatisticsFactory` | 통계 팩토리 초기화 |
| `SmartRenderContext` | 비-PlayerAPI 렌더러 등록 |
| `SmartStatisticsContext` | 틱 처리 |

---

## 초기화 흐름 요약

```
FMLInitializationEvent
  ├─ 클라이언트 체크
  ├─ hasRenderer = Loader.isModLoaded("RenderPlayerAPI")
  ├─ statistics.playerapi.SmartStatistics.register()
  ├─ [PlayerAPI 있음] → reflection으로 playerapi.SmartRender.register()
  ├─ [PlayerAPI 없음 && addRenderer] → SmartRenderContext.registerRenderers(null)
  │    → null 전달이므로 NPE → catch → return (렌더러 미등록)
  ├─ statistics.playerapi.SmartStatisticsFactory.initialize()
  └─ FMLCommonHandler.bus().register(this)  → tickStart 이벤트 수신

ClientTickEvent (매 틱)
  └─ SmartStatisticsContext.onTickInGame()
```

---

## 주요 관찰 사항

1. **SmartRenderInfo 미확인 항목 해결**: ModId=`"SmartRender"`, ModName=`"Smart Render"`, ModVersion=`"@VERSION@"` 확인.

2. **PlayerAPI 필수 의존**: `dependencies = "required-after:PlayerAPI@[1.3,)"` — SmartRender는 PlayerAPI 없이 로드 자체가 불가능. (비-PlayerAPI 경로 코드가 있지만, 이 의존 선언으로 실제 운용에서는 항상 PlayerAPI와 함께 실행됨)

3. **`registerRenderers(null)` 패턴**: 비-PlayerAPI 경로에서 null을 전달하여 실질적으로 렌더러 미등록. `addRenderer=false`인 SmartMoving 연동 시나리오에서는 이 분기 자체에 진입하지 않음.

4. **PlayerAPI 경로 reflection 로드**: `net.smart.render.playerapi.SmartRender`를 컴파일 타임 의존 없이 reflection으로 로드. PlayerAPI 없을 때 클래스 로딩 실패를 방지하기 위한 패턴.

5. **1.21.1 이식 포인트**: FML `@Mod`, `@EventHandler`, `@SubscribeEvent` 전부 Fabric 방식으로 교체 필요. Fabric 진입점은 `fabric.mod.json`의 `entrypoints` + `ModInitializer.onInitialize()`. 틱 이벤트는 `ClientTickEvents.START_CLIENT_TICK.register(...)`.
