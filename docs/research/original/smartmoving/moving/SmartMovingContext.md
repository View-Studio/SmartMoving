# SmartMovingContext.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMovingContext.java  
패키지: `net.smart.moving`  
종류: `abstract class`  
상속: `SmartMovingContext extends SmartRenderContext`

---

## 전체 소스

```java
package net.smart.moving;

import cpw.mods.fml.client.registry.*;
import cpw.mods.fml.common.*;

import net.minecraft.client.*;
import net.minecraft.server.*;

import net.smart.moving.config.*;
import net.smart.render.*;

public abstract class SmartMovingContext extends SmartRenderContext
{
	public static final float ClimbPullMotion = 0.3F;

	public static final double FastUpMotion = 0.2D;
	public static final double MediumUpMotion = 0.14D;
	public static final double SlowUpMotion = 0.1D;
	public static final double HoldMotion = 0.08D;
	public static final double SinkDownMotion = 0.05D;
	public static final double ClimbDownMotion = 0.01D;
	public static final double CatchCrawlGapMotion = 0.17D;

	public static final float SwimCrawlWaterMaxBorder = 1F;
	public static final float SwimCrawlWaterTopBorder = 0.65F;
	public static final float SwimCrawlWaterMediumBorder = 0.6F;
	public static final float SwimCrawlWaterBottomBorder = 0.55F;

	public static final float HorizontalGroundDamping = 0.546F;
	public static final float HorizontalAirDamping = 0.91F;
	public static final float HorizontalAirodynamicDamping = 0.999F;

	public static final float SwimSoundDistance = 1F / 0.7F;
	public static final float SlideToHeadJumpingFallDistance = 0.05F;


	public static final SmartMovingClient Client = new SmartMovingClient();
	public static final SmartMovingOptions Options = new SmartMovingOptions();
	public static final SmartMovingServerConfig ServerConfig = new SmartMovingServerConfig();
	public static SmartMovingClientConfig Config = Options;


	private static boolean wasInitialized;

	public static void onTickInGame()
	{
		Minecraft minecraft = Minecraft.getMinecraft();

		if(minecraft.theWorld != null && minecraft.theWorld.isRemote)
			SmartMovingFactory.handleMultiPlayerTick(minecraft);

		Options.initializeForGameIfNeccessary();

		initializeServerIfNecessary();
	}

	public static void initialize()
	{
		if(!wasInitialized)
			net.smart.render.statistics.SmartStatisticsContext.setCalculateHorizontalStats(true);

		ClientRegistry.registerKeyBinding(Options.keyBindGrab);
		ClientRegistry.registerKeyBinding(Options.keyBindConfigToggle);
		ClientRegistry.registerKeyBinding(Options.keyBindSpeedIncrease);
		ClientRegistry.registerKeyBinding(Options.keyBindSpeedDecrease);

		if(wasInitialized)
			return;

		wasInitialized = true;

		System.out.println(SmartMovingInfo.ModComMessage);
		FMLLog.getLogger().info(SmartMovingInfo.ModComMessage);
	}

	public static void initializeServerIfNecessary()
	{
		MinecraftServer currentMinecraftServer = MinecraftServer.getServer();
		if(currentMinecraftServer != null && currentMinecraftServer != lastMinecraftServer)
			SmartMovingServer.initialize(SmartMovingOptions.optionsPath, currentMinecraftServer.getGameType().getID(), Options);
		lastMinecraftServer = currentMinecraftServer;
	}

	public static void registerRenderers()
	{
		registerRenderers(net.smart.moving.render.RenderPlayer.class);
	}

	private static MinecraftServer lastMinecraftServer = null;
}
```

---

## 역할

전체 SmartMoving 계층의 **루트 컨텍스트 클래스**.

- 전역 싱글톤 객체(`Client`, `Options`, `ServerConfig`, `Config`) 선언
- 전역 상수(이동 속도, 물 경계, 감쇠 계수 등) 선언
- 클라이언트 초기화, 서버 초기화, 틱 처리 static 메서드 제공
- `SmartRenderContext`를 상속 — SmartRender 계층과의 연결점

이 클래스의 모든 멤버는 static이다. 인스턴스 메서드/필드 없음.

---

## 상속 구조 (이 파일 기준)

```
SmartRenderContext (net.smart.render)
  └─ SmartMovingContext            ← 이 파일
       ├─ SmartMovingBase
       │    └─ SmartMoving
       │         └─ SmartMovingSelf
       ├─ SmartMovingClient
       └─ SmartMovingComm
```

---

## import

```java
import cpw.mods.fml.client.registry.*;  // ClientRegistry
import cpw.mods.fml.common.*;           // FMLLog
import net.minecraft.client.*;          // Minecraft
import net.minecraft.server.*;          // MinecraftServer
import net.smart.moving.config.*;       // SmartMovingClient, SmartMovingOptions 등
import net.smart.render.*;              // SmartRenderContext (간접 — 상속)
```

`net.smart.moving.render.RenderPlayer`는 FQN으로 직접 참조 (`registerRenderers` 내).  
`net.smart.render.statistics.SmartStatisticsContext`도 FQN으로 직접 참조.

---

## 전역 상수 (public static final)

### 이동 속도 상수 (double)

| 상수 | 값 | 용도 |
|------|----|------|
| `FastUpMotion` | `0.2D` | 빠른 상승 속도 |
| `CatchCrawlGapMotion` | `0.17D` | 크롤 갭 잡기 상승 속도 |
| `MediumUpMotion` | `0.14D` | 중간 상승 속도 |
| `SlowUpMotion` | `0.1D` | 느린 상승 속도 |
| `HoldMotion` | `0.08D` | 제자리 유지 속도 |
| `SinkDownMotion` | `0.05D` | 서서히 하강 속도 |
| `ClimbDownMotion` | `0.01D` | 클라이밍 하강 속도 |

### 클라이밍 상수 (float)

| 상수 | 값 |
|------|----|
| `ClimbPullMotion` | `0.3F` |

### 수영/크롤 물 경계 상수 (float)

수영+크롤링 전환 시 사용하는 물 높이 경계값:

| 상수 | 값 |
|------|----|
| `SwimCrawlWaterMaxBorder` | `1F` |
| `SwimCrawlWaterTopBorder` | `0.65F` |
| `SwimCrawlWaterMediumBorder` | `0.6F` |
| `SwimCrawlWaterBottomBorder` | `0.55F` |

### 수평 감쇠 상수 (float)

| 상수 | 값 | 상황 |
|------|----|------|
| `HorizontalGroundDamping` | `0.546F` | 지면 |
| `HorizontalAirDamping` | `0.91F` | 공중 |
| `HorizontalAirodynamicDamping` | `0.999F` | 공기역학적 (오타: Airodynamic) |

### 기타 상수 (float)

| 상수 | 값 | 비고 |
|------|----|------|
| `SwimSoundDistance` | `1F / 0.7F` | 컴파일 시 계산 → ≈1.4286F |
| `SlideToHeadJumpingFallDistance` | `0.05F` | 슬라이드→헤드점프 전환 낙하 거리 |

---

## playerSwimWaterBorder — 런타임 계산값 (상수 아님)

`SmartMovingSelf.handleSwimming()` 내 지역 변수. 매 틱 계산.

```java
// SmartMovingSelf.handleSwimming() 내부
double playerSwimWaterBorder = totalSwimWaterBorder - j - j_offset;
// totalSwimWaterBorder = getMaxPlayerLiquidBetween(sp.boundingBox.maxY - 1.8, sp.boundingBox.maxY + 1.2)
// j           = MathHelper.floor_double(sp.boundingBox.minY)  (발 밑 블록 Y좌표)
// j_offset    = sp.boundingBox.minY - j                       (블록 내 소수점 오프셋)
```

사용 위치 (SmartMovingSelf.java):
- `if(isCrawling && playerSwimWaterBorder > SwimCrawlWaterTopBorder)`
- `dippingDepth = (float)playerSwimWaterBorder`
- `float playerCrawlWaterBorder = dippingDepth + wasHeightOffset`
- `else if(playerSwimWaterBorder >= 0 && playerSwimWaterBorder <= 2)`

---

## 전역 싱글톤 객체

```java
public static final SmartMovingClient Client = new SmartMovingClient();
public static final SmartMovingOptions Options = new SmartMovingOptions();
public static final SmartMovingServerConfig ServerConfig = new SmartMovingServerConfig();
public static SmartMovingClientConfig Config = Options;
```

| 필드 | 타입 | 초기값 | final | 설명 |
|------|------|--------|-------|------|
| `Client` | `SmartMovingClient` | `new SmartMovingClient()` | `final` | 클라이언트 전역 상태 (소진 최대값, UI 플래그) |
| `Options` | `SmartMovingOptions` | `new SmartMovingOptions()` | `final` | 클라이언트 로컬 설정 |
| `ServerConfig` | `SmartMovingServerConfig` | `new SmartMovingServerConfig()` | `final` | 서버에서 받은 설정 |
| `Config` | `SmartMovingClientConfig` | `Options` (초기값) | **non-final** | 현재 활성 설정 — 서버 설정 수신 시 `ServerConfig`로 교체 |

**`Config` 전환 패턴** (SmartMovingComm에서 확인):
- 초기: `Config = Options` (클라이언트 로컬 설정 사용)
- 서버 설정 첫 수신: `Config = ServerConfig` (서버 설정 사용)
- 서버 설정 해제: `Config = Options` (로컬 설정으로 복귀)

`SmartMovingOptions extends SmartMovingClientConfig`이므로 `Config`에 `Options` 대입 가능.

---

## private static 필드

```java
private static boolean wasInitialized;       // initialize() 호출 여부
private static MinecraftServer lastMinecraftServer = null;  // 마지막으로 확인한 서버 인스턴스
```

---

## 메서드

### `onTickInGame()` (public static)

```java
public static void onTickInGame()
{
    Minecraft minecraft = Minecraft.getMinecraft();

    if(minecraft.theWorld != null && minecraft.theWorld.isRemote)
        SmartMovingFactory.handleMultiPlayerTick(minecraft);

    Options.initializeForGameIfNeccessary();

    initializeServerIfNecessary();
}
```

매 게임 틱 호출.

| 순서 | 동작 | 조건 |
|------|------|------|
| 1 | `SmartMovingFactory.handleMultiPlayerTick(minecraft)` | `theWorld != null && theWorld.isRemote` (클라이언트 월드) |
| 2 | `Options.initializeForGameIfNeccessary()` | 항상 |
| 3 | `initializeServerIfNecessary()` | 항상 |

`theWorld.isRemote`: 클라이언트 사이드 월드인 경우 `true`. 서버 사이드에서는 `false`.

---

### `initialize()` (public static)

```java
public static void initialize()
{
    if(!wasInitialized)
        net.smart.render.statistics.SmartStatisticsContext.setCalculateHorizontalStats(true);

    ClientRegistry.registerKeyBinding(Options.keyBindGrab);
    ClientRegistry.registerKeyBinding(Options.keyBindConfigToggle);
    ClientRegistry.registerKeyBinding(Options.keyBindSpeedIncrease);
    ClientRegistry.registerKeyBinding(Options.keyBindSpeedDecrease);

    if(wasInitialized)
        return;

    wasInitialized = true;

    System.out.println(SmartMovingInfo.ModComMessage);
    FMLLog.getLogger().info(SmartMovingInfo.ModComMessage);
}
```

클라이언트 초기화.

**최초 호출 시 (`wasInitialized == false`):**
1. `SmartStatisticsContext.setCalculateHorizontalStats(true)` 호출
2. 키 바인딩 4개 등록
3. `wasInitialized = true`
4. `SmartMovingInfo.ModComMessage` → stdout + FMLLog INFO

**재호출 시 (`wasInitialized == true`):**
1. 키 바인딩 4개 재등록 (항상 실행됨)
2. `wasInitialized` 체크 후 return (ModComMessage 출력 안 함)

등록되는 키 바인딩:
- `Options.keyBindGrab`
- `Options.keyBindConfigToggle`
- `Options.keyBindSpeedIncrease`
- `Options.keyBindSpeedDecrease`

---

### `initializeServerIfNecessary()` (public static)

```java
public static void initializeServerIfNecessary()
{
    MinecraftServer currentMinecraftServer = MinecraftServer.getServer();
    if(currentMinecraftServer != null && currentMinecraftServer != lastMinecraftServer)
        SmartMovingServer.initialize(SmartMovingOptions.optionsPath, currentMinecraftServer.getGameType().getID(), Options);
    lastMinecraftServer = currentMinecraftServer;
}
```

서버 인스턴스가 변경된 경우에만 `SmartMovingServer.initialize()` 호출.

| 조건 | 동작 |
|------|------|
| `currentMinecraftServer != null && != lastMinecraftServer` | `SmartMovingServer.initialize(optionsPath, gameType.getID(), Options)` |
| 항상 | `lastMinecraftServer = currentMinecraftServer` |

`onTickInGame()`에서 매 틱 호출 — 서버 인스턴스 변경 감지 방식.  
싱글플레이어 → 멀티플레이어 전환, 월드 변경 등에서 트리거.

---

### `registerRenderers()` (public static)

```java
public static void registerRenderers()
{
    registerRenderers(net.smart.moving.render.RenderPlayer.class);
}
```

SmartMoving 전용 플레이어 렌더러 등록.  
`net.smart.moving.render.RenderPlayer` — SmartMoving 렌더 패키지의 RenderPlayer.  
`registerRenderers(Class)` 메서드는 이 파일에 없음 — `SmartRenderContext`에서 상속.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartRenderContext` | 상속 (`registerRenderers(Class)` 포함) |
| `SmartMovingClient` | `Client` 싱글톤 |
| `SmartMovingOptions` | `Options` 싱글톤, 키 바인딩 4개 |
| `SmartMovingServerConfig` | `ServerConfig` 싱글톤 |
| `SmartMovingClientConfig` | `Config` 필드 타입 |
| `SmartMovingFactory` | `handleMultiPlayerTick(minecraft)` |
| `SmartMovingServer` | `initialize(optionsPath, gameType, options)` |
| `SmartMovingInfo` | `ModComMessage` |
| `SmartStatisticsContext` (SmartRender) | `setCalculateHorizontalStats(true)` |
| `net.smart.moving.render.RenderPlayer` | 렌더러 등록 대상 |
| `Minecraft` | `getMinecraft()`, `theWorld` |
| `MinecraftServer` | `getServer()`, `getGameType().getID()` |
| `ClientRegistry` | `registerKeyBinding()` |
| `FMLLog` | INFO 로그 |

---

## 주요 관찰 사항

1. **`Config` non-final**: `SmartMovingClientConfig Config = Options` — 서버 설정 수신 시 `SmartMovingComm.processConfigPacket()`이 `Config = ServerConfig`로 교체. 이 파일에서 선언만 하고 SmartMovingComm에서 값을 변경.

2. **`SwimSoundDistance = 1F / 0.7F`**: 상수 표현식이므로 컴파일 시 `1.4285714F`로 계산됨. 값이 1보다 크면 소리 거리가 더 멀리 들린다는 의미.

3. **`HorizontalAirodynamicDamping` 오타**: Aerodynamic → Airodynamic. 원본에도 동일.

4. **`initialize()` 키 바인딩 재등록**: `wasInitialized`가 true여도 키 바인딩 등록은 항상 실행. 월드 재진입 시 키 바인딩 재등록이 필요한 FML 동작 때문으로 보임.

5. **`SmartStatisticsContext.setCalculateHorizontalStats(true)`**: FQN 직접 참조. SmartRender 통계 모듈의 수평 속도 계산 활성화. 최초 초기화 시에만 호출.

6. **서버 인스턴스 변경 감지**: `lastMinecraftServer` 비교로 서버 교체를 감지. 매 틱(`onTickInGame`) 폴링 방식.

7. **1.21.1 이식**: 이 클래스의 상수들은 그대로 이식. `Config`/`Options`/`ServerConfig` static 패턴 유지. `ClientRegistry.registerKeyBinding` → Fabric `KeyBindingHelper.registerKeyBinding`. `MinecraftServer.getServer()` → Fabric `ServerLifecycleHooks` 또는 서버 이벤트 방식으로 대체.
