# SmartMovingMod.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMovingMod.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: 없음 (`Object` 직접 상속)  
어노테이션: `@Mod`

---

## 전체 소스

```java
package net.smart.moving;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.Mod.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.*;
import cpw.mods.fml.common.gameevent.TickEvent.*;
import cpw.mods.fml.common.network.*;
import cpw.mods.fml.common.network.FMLNetworkEvent.*;
import net.minecraft.entity.player.*;
import net.minecraft.network.*;
import net.smart.core.*;
import net.smart.moving.config.*;
import net.smart.utilities.*;

@Mod(modid = "SmartMoving", name = "Smart Moving", version = "15.6", dependencies = "required-after:PlayerAPI@[1.3,);required-after:SmartRender@[2.1,)")
public class SmartMovingMod
{
	protected static String ModComVersion = "2.3.1";

	private final boolean isClient;

	private boolean hasRenderer = false;

	public SmartMovingMod()
	{
		isClient = FMLCommonHandler.instance().getSide().isClient();
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void init(FMLInitializationEvent event)
	{
		NetworkRegistry.INSTANCE.newEventDrivenChannel(SmartMovingPacketStream.Id).register(this);

		if(isClient)
		{
			hasRenderer = Loader.isModLoaded("RenderPlayerAPI");

			net.smart.moving.playerapi.SmartMoving.register();

			if(hasRenderer)
			{
				Class<?> type = Reflect.LoadClass(SmartMovingMod.class, new Name("net.smart.moving.render.playerapi.SmartMoving"), true);
				Method method = Reflect.GetMethod(type, new Name("register"));
				Reflect.Invoke(method, null);
			}
			else
				net.smart.render.SmartRenderMod.doNotAddRenderer();

			SmartMovingServerComm.localUserNameProvider = new LocalUserNameProvider();
			if(!hasRenderer)
				SmartMovingContext.registerRenderers();

			registerGameTicks();

			net.smart.moving.playerapi.SmartMovingFactory.initialize();

			checkForPresentModsAndInitializeOptions();

			SmartMovingContext.initialize();
		}
		else
			SmartMovingServer.initialize(new File("."), FMLCommonHandler.instance().getMinecraftServerInstance().getGameType().getID(), new SmartMovingConfig());

		SmartCoreEventHandler.Add(new SmartMovingCoreEventHandler());
		
		Compat.init();
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void postInit(FMLPostInitializationEvent event)
	{
		if(!isClient)
			net.smart.moving.playerapi.SmartMovingServerPlayerBase.registerPlayerBase();
	}

	@SubscribeEvent
	@SuppressWarnings({ "static-method", "unused" })
	public void tickStart(ClientTickEvent event)
	{
		SmartMovingContext.onTickInGame();
	}

	@SubscribeEvent
	@SuppressWarnings("static-method")
	public void onPacketData(ServerCustomPacketEvent event)
	{
		SmartMovingPacketStream.receivePacket(event.packet, SmartMovingServerComm.instance, net.smart.moving.playerapi.SmartMovingServerPlayerBase.getPlayerBase(((NetHandlerPlayServer)event.handler).playerEntity));
	}

	@SubscribeEvent
	@SuppressWarnings("static-method")
	public void onPacketData(ClientCustomPacketEvent event)
	{
		SmartMovingPacketStream.receivePacket(event.packet, SmartMovingComm.instance, null);
	}

	public void registerGameTicks()
	{
		FMLCommonHandler.instance().bus().register(this);
	}

	@SuppressWarnings("static-method")
	public Object getInstance(EntityPlayer entityPlayer)
	{
		return SmartMovingFactory.getInstance(entityPlayer);
	}

	@SuppressWarnings("static-method")
	public Object getClient()
	{
		return SmartMovingContext.Client;
	}

	@SuppressWarnings("static-method")
	public void checkForPresentModsAndInitializeOptions()
	{
		List<ModContainer> modList = Loader.instance().getActiveModList();
		boolean hasRedPowerWiring = false;
		boolean hasBuildCraftTransport = false;
		boolean hasFiniteLiquid = false;
		boolean hasBetterThanWolves = false;
		boolean hasSinglePlayerCommands = false;
		boolean hasRopesPlus = false;
		boolean hasASGrapplingHook = false;
		boolean hasBetterMisc = false;

		for(int i = 0; i < modList.size(); i++)
		{
			ModContainer mod = modList.get(i);
			String name = mod.getName();

			if(name.contains("RedPowerWiring"))
				hasRedPowerWiring = true;
			else if(name.contains("BuildCraftTransport"))
				hasBuildCraftTransport = true;
			else if(name.contains("Liquid"))
				hasFiniteLiquid = true;
			else if(name.contains("FCBetterThanWolves"))
				hasBetterThanWolves = true;
			else if(name.contains("SinglePlayerCommands"))
				hasSinglePlayerCommands = true;
			else if(name.contains("ASGrapplingHook"))
				hasASGrapplingHook = true;
			else if(name.contains("BetterMisc"))
				hasBetterMisc = true;
		}

		hasRopesPlus = Reflect.CheckClasses(SmartMovingMod.class, SmartMovingInstall.RopesPlusCore);

		SmartMovingOptions.initialize(hasRedPowerWiring, hasBuildCraftTransport, hasFiniteLiquid, hasBetterThanWolves, hasSinglePlayerCommands, hasRopesPlus, hasASGrapplingHook, hasBetterMisc);
	}
}
```

---

## 역할

FML이 인식하는 **모드 진입점**. SmartMoving 전체의 초기화 흐름을 총괄한다.

- `@Mod` 어노테이션으로 FML에 등록
- `init` / `postInit`에서 클라이언트/서버 분기 초기화
- FML 이벤트 버스 등록으로 틱과 패킷 처리

---

## `@Mod` 어노테이션

```java
@Mod(
    modid = "SmartMoving",
    name = "Smart Moving",
    version = "15.6",
    dependencies = "required-after:PlayerAPI@[1.3,);required-after:SmartRender@[2.1,)"
)
```

| 속성 | 값 |
|------|----|
| `modid` | `"SmartMoving"` |
| `name` | `"Smart Moving"` |
| `version` | `"15.6"` |
| `dependencies` | PlayerAPI 1.3 이상 필수, SmartRender 2.1 이상 필수 |

이 어노테이션 값이 `SmartMovingInfo`에서 `SmartMovingMod.class.getAnnotation(Mod.class)`로 읽힘.

---

## import

```java
import java.io.*;                               // File
import java.lang.reflect.*;                     // Method
import java.util.*;                             // List
import cpw.mods.fml.common.*;                   // Loader, FMLCommonHandler
import cpw.mods.fml.common.Mod.*;               // EventHandler
import cpw.mods.fml.common.event.*;             // FMLInitializationEvent, FMLPostInitializationEvent
import cpw.mods.fml.common.eventhandler.*;      // SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent.*;// ClientTickEvent
import cpw.mods.fml.common.network.*;           // NetworkRegistry
import cpw.mods.fml.common.network.FMLNetworkEvent.*; // ServerCustomPacketEvent, ClientCustomPacketEvent
import net.minecraft.entity.player.*;           // EntityPlayer
import net.minecraft.network.*;                 // NetHandlerPlayServer
import net.smart.core.*;                        // SmartCoreEventHandler
import net.smart.moving.config.*;               // SmartMovingConfig
import net.smart.utilities.*;                   // Reflect, Name
```

---

## 필드

```java
protected static String ModComVersion = "2.3.1";
```

통신 프로토콜 버전. `SmartMovingInfo.ModComVersion`이 이 값을 참조.  
`protected static` — 서브클래스에서 오버라이드 가능.

```java
private final boolean isClient;
```

클라이언트 측이면 `true`. 생성자에서 `FMLCommonHandler.instance().getSide().isClient()`로 결정.  
`final` — 이후 변경 불가.

```java
private boolean hasRenderer = false;
```

RenderPlayerAPI 모드 로드 여부. `init`에서 `Loader.isModLoaded("RenderPlayerAPI")`로 설정.

---

## 생성자

```java
public SmartMovingMod()
{
    isClient = FMLCommonHandler.instance().getSide().isClient();
}
```

FML이 모드 인스턴스를 생성할 때 호출. 클라이언트/서버 판별.

---

## @EventHandler 메서드

### `init(FMLInitializationEvent event)`

**전체 초기화 흐름:**

```java
NetworkRegistry.INSTANCE.newEventDrivenChannel(SmartMovingPacketStream.Id).register(this);
```
네트워크 채널 등록 (클라이언트/서버 공통). `this`를 채널 이벤트 리스너로 등록.

---

**클라이언트 측 (`isClient == true`):**

```java
hasRenderer = Loader.isModLoaded("RenderPlayerAPI");
```
RenderPlayerAPI 로드 여부 확인.

```java
net.smart.moving.playerapi.SmartMoving.register();
```
PlayerAPI에 SmartMoving 플레이어 베이스 등록.

```java
if(hasRenderer)
{
    Class<?> type = Reflect.LoadClass(SmartMovingMod.class, new Name("net.smart.moving.render.playerapi.SmartMoving"), true);
    Method method = Reflect.GetMethod(type, new Name("register"));
    Reflect.Invoke(method, null);
}
else
    net.smart.render.SmartRenderMod.doNotAddRenderer();
```

| 조건 | 동작 |
|------|------|
| `hasRenderer == true` | `net.smart.moving.render.playerapi.SmartMoving.register()` 리플렉션 호출 |
| `hasRenderer == false` | `SmartRenderMod.doNotAddRenderer()` — SmartRender 렌더러 비활성화 |

리플렉션 사용 이유: 컴파일 타임에 render.playerapi 패키지가 없을 수 있음 (RenderPlayerAPI 선택 의존성).

```java
SmartMovingServerComm.localUserNameProvider = new LocalUserNameProvider();
```
서버 Comm의 로컬 유저명 제공자 설정.

```java
if(!hasRenderer)
    SmartMovingContext.registerRenderers();
```
RenderPlayerAPI 없을 때만 직접 렌더러 등록.  
(RenderPlayerAPI 있을 경우 render.playerapi.SmartMoving.register()가 처리)

```java
registerGameTicks();
net.smart.moving.playerapi.SmartMovingFactory.initialize();
checkForPresentModsAndInitializeOptions();
SmartMovingContext.initialize();
```

| 순서 | 메서드 | 동작 |
|------|--------|------|
| 1 | `registerGameTicks()` | FML 이벤트 버스에 `this` 등록 (`@SubscribeEvent` 활성화) |
| 2 | `SmartMovingFactory.initialize()` | 팩토리 싱글톤 생성 |
| 3 | `checkForPresentModsAndInitializeOptions()` | 호환 모드 감지 → `SmartMovingOptions.initialize()` |
| 4 | `SmartMovingContext.initialize()` | 키 바인딩 등록, 통계 활성화, 로그 출력 |

---

**서버 측 (`isClient == false`):**

```java
SmartMovingServer.initialize(new File("."), FMLCommonHandler.instance().getMinecraftServerInstance().getGameType().getID(), new SmartMovingConfig());
```
서버 정적 초기화. `new File(".")` = 현재 디렉토리, 게임 타입 ID, 기본 SmartMovingConfig.

---

**클라이언트/서버 공통:**

```java
SmartCoreEventHandler.Add(new SmartMovingCoreEventHandler());
Compat.init();
```

| 동작 |
|------|
| SmartCore에 SmartMoving의 before/after 훅 핸들러 등록 |
| 호환성 초기화 (`Compat.init()`) |

---

### `postInit(FMLPostInitializationEvent event)`

```java
@EventHandler
public void postInit(FMLPostInitializationEvent event)
{
    if(!isClient)
        net.smart.moving.playerapi.SmartMovingServerPlayerBase.registerPlayerBase();
}
```

서버 측에서만: `SmartMovingServerPlayerBase.registerPlayerBase()` 호출.  
클라이언트는 `init`에서 이미 `SmartMoving.register()` 완료.

---

## @SubscribeEvent 메서드

### `tickStart(ClientTickEvent event)`

```java
@SubscribeEvent
public void tickStart(ClientTickEvent event)
{
    SmartMovingContext.onTickInGame();
}
```

클라이언트 틱마다 `SmartMovingContext.onTickInGame()` 호출.  
`registerGameTicks()`로 등록된 후 활성화.

---

### `onPacketData(ServerCustomPacketEvent event)` — 서버 수신

```java
@SubscribeEvent
public void onPacketData(ServerCustomPacketEvent event)
{
    SmartMovingPacketStream.receivePacket(
        event.packet,
        SmartMovingServerComm.instance,
        net.smart.moving.playerapi.SmartMovingServerPlayerBase.getPlayerBase(((NetHandlerPlayServer)event.handler).playerEntity)
    );
}
```

클라이언트→서버 패킷 수신.  
`event.handler`를 `NetHandlerPlayServer`로 캐스팅 → `playerEntity` → `SmartMovingServerPlayerBase` 조회.  
`SmartMovingPacketStream.receivePacket(..., SmartMovingServerComm.instance, playerBase)`.

---

### `onPacketData(ClientCustomPacketEvent event)` — 클라이언트 수신

```java
@SubscribeEvent
public void onPacketData(ClientCustomPacketEvent event)
{
    SmartMovingPacketStream.receivePacket(event.packet, SmartMovingComm.instance, null);
}
```

서버→클라이언트 패킷 수신.  
`SmartMovingPacketStream.receivePacket(..., SmartMovingComm.instance, null)`.  
세 번째 파라미터 `null` — 클라이언트 측에서는 playerBase 불필요.

---

## 기타 메서드

### `registerGameTicks()`

```java
public void registerGameTicks()
{
    FMLCommonHandler.instance().bus().register(this);
}
```

FML 이벤트 버스에 `this` 등록. 이후 `@SubscribeEvent`가 붙은 `tickStart`, `onPacketData` 두 메서드가 이벤트를 수신.

---

### `getInstance(EntityPlayer entityPlayer)` → Object

```java
public Object getInstance(EntityPlayer entityPlayer)
{
    return SmartMovingFactory.getInstance(entityPlayer);
}
```

외부에서 `SmartMoving` 인스턴스 접근 시 사용. 반환 타입 `Object` (의존성 없이 접근 가능하도록).

---

### `getClient()` → Object

```java
public Object getClient()
{
    return SmartMovingContext.Client;
}
```

외부에서 `SmartMovingClient` 접근. 반환 타입 `Object`.

---

### `checkForPresentModsAndInitializeOptions()`

```java
public void checkForPresentModsAndInitializeOptions()
{
    List<ModContainer> modList = Loader.instance().getActiveModList();
    boolean hasRedPowerWiring = false;
    boolean hasBuildCraftTransport = false;
    boolean hasFiniteLiquid = false;
    boolean hasBetterThanWolves = false;
    boolean hasSinglePlayerCommands = false;
    boolean hasRopesPlus = false;
    boolean hasASGrapplingHook = false;
    boolean hasBetterMisc = false;

    for(int i = 0; i < modList.size(); i++)
    {
        ModContainer mod = modList.get(i);
        String name = mod.getName();

        if(name.contains("RedPowerWiring"))
            hasRedPowerWiring = true;
        else if(name.contains("BuildCraftTransport"))
            hasBuildCraftTransport = true;
        else if(name.contains("Liquid"))
            hasFiniteLiquid = true;
        else if(name.contains("FCBetterThanWolves"))
            hasBetterThanWolves = true;
        else if(name.contains("SinglePlayerCommands"))
            hasSinglePlayerCommands = true;
        else if(name.contains("ASGrapplingHook"))
            hasASGrapplingHook = true;
        else if(name.contains("BetterMisc"))
            hasBetterMisc = true;
    }

    hasRopesPlus = Reflect.CheckClasses(SmartMovingMod.class, SmartMovingInstall.RopesPlusCore);

    SmartMovingOptions.initialize(hasRedPowerWiring, hasBuildCraftTransport, hasFiniteLiquid, hasBetterThanWolves, hasSinglePlayerCommands, hasRopesPlus, hasASGrapplingHook, hasBetterMisc);
}
```

활성 모드 목록을 순회하여 호환 모드 감지:

| 감지 문자열 | boolean 변수 | 모드 |
|------------|-------------|------|
| `"RedPowerWiring"` | `hasRedPowerWiring` | RedPower2 |
| `"BuildCraftTransport"` | `hasBuildCraftTransport` | BuildCraft |
| `"Liquid"` | `hasFiniteLiquid` | Finite Liquid |
| `"FCBetterThanWolves"` | `hasBetterThanWolves` | Better Than Wolves |
| `"SinglePlayerCommands"` | `hasSinglePlayerCommands` | SPC |
| `"ASGrapplingHook"` | `hasASGrapplingHook` | AS GrapplingHook |
| `"BetterMisc"` | `hasBetterMisc` | BetterMisc |

**RopesPlus는 별도 방법**: `Reflect.CheckClasses(SmartMovingMod.class, SmartMovingInstall.RopesPlusCore)` — 클래스 실제 존재 여부로 확인.

최종: `SmartMovingOptions.initialize(8개 boolean)` 호출.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingPacketStream` | 채널 ID, `receivePacket()` |
| `SmartMovingServerComm` | `instance`, `localUserNameProvider` 설정 |
| `SmartMovingComm` | `instance` |
| `SmartMovingContext` | `initialize()`, `registerRenderers()`, `onTickInGame()`, `Client` |
| `SmartMovingFactory` | `getInstance()` |
| `SmartMovingServer` | `initialize()` (서버) |
| `SmartMovingCoreEventHandler` | ASM 훅 핸들러 생성 |
| `SmartMovingOptions` | `initialize()` |
| `SmartMovingConfig` | 서버 기본 설정 생성 |
| `SmartMovingInstall` | `RopesPlusCore` 참조 |
| `SmartCoreEventHandler` | `Add()` |
| `Compat` | `init()` |
| `LocalUserNameProvider` | 생성 |
| `Reflect` | `LoadClass`, `GetMethod`, `Invoke`, `CheckClasses` |
| `Name` | 클래스/메서드 이름 래퍼 |
| `net.smart.moving.playerapi.SmartMoving` | `register()` |
| `net.smart.moving.playerapi.SmartMovingFactory` | `initialize()` |
| `net.smart.moving.playerapi.SmartMovingServerPlayerBase` | `getPlayerBase()`, `registerPlayerBase()` |
| `net.smart.render.SmartRenderMod` | `doNotAddRenderer()` |
| `NetworkRegistry` | 채널 등록 |
| `FMLCommonHandler` | `getSide()`, `bus()`, `getMinecraftServerInstance()` |
| `Loader` | `isModLoaded()`, `getActiveModList()` |

---

## 주요 관찰 사항

1. **`ModComVersion = "2.3.1"`**: `SmartMovingInfo.ModComVersion`이 이 값을 직접 참조. 모드 버전(`"15.6"`)과 통신 프로토콜 버전(`"2.3.1"`)이 별도로 관리됨.

2. **RenderPlayerAPI 선택적 의존**: `hasRenderer = Loader.isModLoaded("RenderPlayerAPI")` — 있으면 리플렉션으로 `render.playerapi.SmartMoving.register()` 호출, 없으면 `doNotAddRenderer()` + 직접 렌더러 등록.

3. **클라이언트 init 순서**: 채널 등록 → PlayerAPI register → RenderPlayerAPI 분기 → localUserNameProvider → registerRenderers(옵션) → registerGameTicks → Factory init → Options init → Context init → SmartCore 훅 등록 → Compat init

4. **서버 postInit**: `SmartMovingServerPlayerBase.registerPlayerBase()`가 `postInit`에서 실행. 클라이언트의 `SmartMoving.register()`는 `init`에서 실행 — 순서 차이.

5. **`checkForPresentModsAndInitializeOptions` else-if 체인**: `modList` 순회에서 `else if` 사용 — 하나의 모드가 두 조건을 동시에 만족해도 첫 번째 조건만 적용됨. `hasRopesPlus`만 별도 클래스 존재 확인.

6. **`getInstance`/`getClient` 반환 타입 `Object`**: 외부 모드가 SmartMoving 클래스에 직접 의존하지 않고 인스턴스에 접근할 수 있도록 `Object` 타입으로 노출.

7. **1.21.1 이식**: FML `@Mod` → Fabric `@Mod`. `@EventHandler` → Fabric 이벤트 훅(`ModInitializer.onInitialize` 등). `@SubscribeEvent` + FML bus → Fabric `ServerTickEvents`, `ServerPlayNetworking`, `ClientPlayNetworking`. `Loader.isModLoaded()` → Fabric `FabricLoader.getInstance().isModLoaded()`.
