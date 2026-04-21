# SmartMovingServer.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMovingServer.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: 없음 (`Object` 직접 상속)

---

## 전체 소스

```java
package net.smart.moving;

import java.io.*;
import java.util.*;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.network.internal.*;
import net.minecraft.entity.*;
import net.minecraft.util.*;
import net.smart.moving.config.*;
import net.smart.properties.*;

public class SmartMovingServer
{
	public static final float SmallSizeItemGrabHeight = 0.25F;

	protected final IEntityPlayerMP mp;
	private boolean resetFallDistance = false;
	private boolean resetTicksForFloatKick = false;
	private boolean initialized = false;
	private boolean withinOnLivingUpdate = false;

	public boolean crawlingInitialized;
	public int crawlingCooldown;
	public boolean isCrawling;
	public boolean isSmall;
	public float hunger;

	private int disableAddExhaustionDepth;
	private boolean disableAddExhaustion;
	private boolean isSneakButtonPressed;
	private Boolean forceIsSneaking;

	public SmartMovingServer(IEntityPlayerMP mp, boolean onTheFly)
	{
		this.mp = mp;
		if (onTheFly)
			initialize(true);
	}

	public void initialize(boolean alwaysSendMessage)
	{
		if(Options._globalConfig.value)
			SmartMovingPacketStream.sendConfigContent(mp, optionsHandler.writeToProperties(), null);
		else if(Options._serverConfig.value)
			SmartMovingPacketStream.sendConfigContent(mp, optionsHandler.writeToProperties(mp, false), null);
		else if(alwaysSendMessage)
			SmartMovingPacketStream.sendConfigContent(mp, Options.enabled ? new String[0] : null, null);
		initialized = true;
	}

	public void processStatePacket(FMLProxyPacket packet, long state)
	{
		if(!initialized)
			initialize(false);

		boolean isCrawling = ((state >>> 13) & 1) != 0;
		setCrawling(isCrawling);

		boolean isSmall = ((state >>> 15) & 1) != 0;
		setSmall(isSmall);

		boolean isClimbing = ((state >>> 14) & 1) != 0;
		boolean isCrawlClimbing = ((state >>> 12) & 1) != 0;
		boolean isCeilingClimbing = ((state >>> 18) & 1) != 0;

		boolean isWallJumping = ((state >>> 31) & 1) != 0;

		isSneakButtonPressed = ((state >>> 33) & 1) != 0;

		resetFallDistance = isClimbing || isCrawlClimbing || isCeilingClimbing || isWallJumping;
		resetTicksForFloatKick = isClimbing || isCrawlClimbing || isCeilingClimbing;
		mp.sendPacketToTrackedPlayers(packet);
	}

	public void processConfigPacket(String clientConfigurationVersion)
	{
		boolean warn = true;
		String type = "unknown";
		if(clientConfigurationVersion != null)
			for(int i = 0; i < SmartMovingConfig._all.length; i++)
				if(clientConfigurationVersion.equals(SmartMovingConfig._all[i]))
				{
					warn = i > 0;
					type = warn ? "outdated" : "matching";
					break;
				}

		String message = "Smart Moving player \"" + mp.getUsername() + "\" connected with " + type + " configuration system";
		if(clientConfigurationVersion != null)
			message += " version \"" + clientConfigurationVersion + "\"";

		if(warn)
			FMLLog.warning(message);
		else
			FMLLog.info(message);
	}

	public void processConfigChangePacket(String localUserName)
	{
		if(!Options._globalConfig.value)
		{
			toggleSingleConfig();
			return;
		}

		String username = mp.getUsername();

		if(localUserName == username)
		{
			toggleConfig();
			return;
		}

		String[] rightPlayerNames = Options._usersWithChangeConfigRights.value;
		for(int i=0; i<rightPlayerNames.length; i++)
			if(rightPlayerNames[i].equals(username))
			{
				toggleConfig();
				return;
			}

		SmartMovingPacketStream.sendConfigChange(mp);
	}

	public void processSpeedChangePacket(int difference, String localUserName)
	{
		if(!Options._globalConfig.value)
		{
			changeSingleSpeed(difference);
			return;
		}

		if(!hasRight(localUserName, Options._usersWithChangeSpeedRights))
			SmartMovingPacketStream.sendSpeedChange(mp, 0, null);
		else
			changeSpeed(difference);
	}

	public void processHungerChangePacket(float hunger)
	{
		this.hunger = hunger;
	}

	public void processSoundPacket(String soundId, float volume, float pitch)
	{
		mp.localPlaySound(soundId, volume, pitch);
	}

	private boolean hasRight(String localUserName, Property<String[]> rights)
	{
		String username = mp.getUsername();

		if(localUserName == username)
			return true;

		String[] rightPlayerNames = rights.value;
		for(int i=0; i<rightPlayerNames.length; i++)
			if(rightPlayerNames[i].equals(username))
				return true;

		return false;
	}

	public void toggleSingleConfig()
	{
		SmartMovingPacketStream.sendConfigContent(mp, optionsHandler.writeToProperties(mp, true), mp.getUsername());
	}

	public void toggleConfig()
	{
		optionsHandler.toggle(mp);
		String[] config = optionsHandler.writeToProperties();
		IEntityPlayerMP[] players = mp.getAllPlayers();
		for(int n=0; n<players.length; n++)
			SmartMovingPacketStream.sendConfigContent(players[n], config, mp.getUsername());
	}

	public void changeSingleSpeed(int difference)
	{
		optionsHandler.changeSingleSpeed(mp, difference);
		SmartMovingPacketStream.sendSpeedChange(mp, difference, mp.getUsername());
	}

	public void changeSpeed(int difference)
	{
		optionsHandler.changeSpeed(difference, mp);
		IEntityPlayerMP[] players = mp.getAllPlayers();
		for(int n=0; n<players.length; n++)
			SmartMovingPacketStream.sendSpeedChange(players[n], difference, mp.getUsername());
	}

	public void afterOnUpdate()
	{
		if(resetFallDistance)
			mp.resetFallDistance();
		if(resetTicksForFloatKick)
			mp.resetTicksForFloatKick();
	}

	public static void initialize(File optionsPath, int gameType, SmartMovingConfig config)
	{
		Options = config;
		optionsHandler = new SmartMovingServerOptions(Options, optionsPath, gameType);
		FMLLog.getLogger().info(SmartMovingInfo.ModComMessage);
	}

	public void setCrawling(boolean crawling)
	{
		if(!crawling && isCrawling)
			crawlingCooldown = 10;
		isCrawling = crawling;
	}

	public void setSmall(boolean isSmall)
	{
		mp.setHeight(isSmall ? 0.8F : 1.8F);
		this.isSmall = isSmall;
	}

	@SuppressWarnings("unused")
	public void afterSetPosition(double d, double d1, double d2)
	{
		if(!crawlingInitialized)
			mp.setMaxY(mp.getMinY() + mp.getHeight() - 1);
	}

	public void beforeIsPlayerSleeping()
	{
		if(!crawlingInitialized)
		{
			mp.setMaxY(mp.getMinY() + mp.getHeight());
			crawlingInitialized = true;
		}
	}

	public void beforeOnUpdate()
	{
		if (crawlingCooldown > 0)
			crawlingCooldown --;
	}

	public void beforeOnLivingUpdate()
	{
		withinOnLivingUpdate = true;
	}

	public void afterOnLivingUpdate()
	{
		withinOnLivingUpdate = false;

		if(!isSmall)
			return;

		if (mp.doGetHealth() <= 0)
			return;

		double offset = SmallSizeItemGrabHeight;
		AxisAlignedBB box = mp.expandBox(mp.getBox(), 1, offset, 1);

		List<?> offsetEntities = mp.getEntitiesExcludingPlayer(box);
		if (offsetEntities != null && offsetEntities.size() > 0)
		{
			Object[] offsetEntityArray = offsetEntities.toArray();

			box = mp.expandBox(box, 0, -offset, 0);
			List<?> standardEntities = mp.getEntitiesExcludingPlayer(box);

			for (int i=0; i<offsetEntityArray.length; i++)
			{
				Entity offsetEntity = (Entity)offsetEntityArray[i];
				if(standardEntities != null && standardEntities.contains(offsetEntity))
					continue;

				if (!mp.isDeadEntity(offsetEntity))
					mp.onCollideWithPlayer(offsetEntity);
			}
		}
	}

	public boolean isEntityInsideOpaqueBlock()
	{
		if(crawlingCooldown > 0)
			return false;

		return mp.localIsEntityInsideOpaqueBlock();
	}

	public void addMovementStat(double var1, double var3, double var5)
	{
		beforeAddMovingHungerBatch();
		mp.localAddMovementStat(var1, var3, var5);
		if(disableAddExhaustion && hunger != 0 && !withinOnLivingUpdate)
			mp.localAddExhaustion(hunger);
		afterAddMovingHungerBatch();
	}

	public void beforeAddMovingHungerBatch()
	{
		disableAddExhaustionDepth++;
		if(hunger != -1)
			disableAddExhaustion = true;
	}

	public void addExhaustion(float exhaustion)
	{
		if(!disableAddExhaustion)
			mp.localAddExhaustion(exhaustion);
	}

	public void afterAddMovingHungerBatch()
	{
		disableAddExhaustionDepth--;
		if(disableAddExhaustionDepth == 0)
			disableAddExhaustion = false;
	}

	public boolean isSneaking()
	{
		if(forceIsSneaking != null)
			return forceIsSneaking;

		return mp.localIsSneaking();
	}

	public void beforeActivateBlockOrUseItem()
	{
		forceIsSneaking = isSneakButtonPressed;
	}

	public void afterActivateBlockOrUseItem()
	{
		forceIsSneaking = null;
	}

	public static SmartMovingConfig Options = null;
	private static SmartMovingServerOptions optionsHandler = null;
}
```

---

## 역할

서버 측 플레이어 1인당 1개 생성되는 **서버 처리 객체**.  
클라이언트에서 보내는 패킷 처리, 서버 측 상태 관리, 소진(exhaustion)/배고픔 인터셉트, 크롤링/스몰 크기 관리, 설정 배포를 담당한다.

- `SmartMovingContext`/`SmartMovingBase`를 상속하지 않음 — 물리 계산 없음
- 모든 플레이어 상태 접근은 `IEntityPlayerMP mp` 인터페이스를 통함

---

## import

```java
import java.io.*;                               // File
import java.util.*;                             // List
import cpw.mods.fml.common.*;                   // FMLLog
import cpw.mods.fml.common.network.internal.*;  // FMLProxyPacket
import net.minecraft.entity.*;                  // Entity, AxisAlignedBB
import net.minecraft.util.*;                    // AxisAlignedBB
import net.smart.moving.config.*;               // SmartMovingConfig, SmartMovingServerOptions
import net.smart.properties.*;                  // Property
```

---

## 정적(static) 필드

```java
public static final float SmallSizeItemGrabHeight = 0.25F;
public static SmartMovingConfig Options = null;
private static SmartMovingServerOptions optionsHandler = null;
```

| 필드 | 값/타입 | 설명 |
|------|---------|------|
| `SmallSizeItemGrabHeight` | `0.25F` | isSmall 상태에서 아이템 획득 범위 Y 확장량 |
| `Options` | `SmartMovingConfig` | 서버 설정 객체 (static initialize로 주입) |
| `optionsHandler` | `SmartMovingServerOptions` | 설정 읽기/쓰기/토글 핸들러 |

`Options`, `optionsHandler`는 static — 서버 전체에 하나.

---

## 인스턴스 필드

```java
protected final IEntityPlayerMP mp;         // 담당 플레이어 인터페이스
private boolean resetFallDistance = false;  // afterOnUpdate에서 낙하 거리 리셋 여부
private boolean resetTicksForFloatKick = false; // afterOnUpdate에서 floatKick 틱 리셋 여부
private boolean initialized = false;        // initialize() 호출 여부
private boolean withinOnLivingUpdate = false; // onLivingUpdate 진행 중 여부

public boolean crawlingInitialized;   // 크롤링 초기화 완료 여부 (boundingBox maxY 조정)
public int crawlingCooldown;          // 크롤링 종료 후 쿨다운 틱 수
public boolean isCrawling;            // 현재 크롤링 중
public boolean isSmall;               // 현재 작은 크기 (isSmall)
public float hunger;                  // 클라이언트에서 보내온 배고픔 값

private int disableAddExhaustionDepth;    // addMovingHungerBatch 중첩 깊이 카운터
private boolean disableAddExhaustion;     // 소진 추가 비활성화 여부
private boolean isSneakButtonPressed;     // 클라이언트 상태 패킷의 sneaking 버튼 상태
private Boolean forceIsSneaking;          // isSneaking() 오버라이드 값 (null=오버라이드 없음)
```

---

## 생성자

```java
public SmartMovingServer(IEntityPlayerMP mp, boolean onTheFly)
{
    this.mp = mp;
    if (onTheFly)
        initialize(true);
}
```

`onTheFly = true`이면 즉시 `initialize(true)` 호출.  
`onTheFly = false`이면 initialize 지연 — 첫 패킷 수신 시(`processStatePacket`) 자동 호출.

---

## 정적 초기화 메서드

### `static initialize(File optionsPath, int gameType, SmartMovingConfig config)`

```java
public static void initialize(File optionsPath, int gameType, SmartMovingConfig config)
{
    Options = config;
    optionsHandler = new SmartMovingServerOptions(Options, optionsPath, gameType);
    FMLLog.getLogger().info(SmartMovingInfo.ModComMessage);
}
```

서버 시작 시 1회 호출. `Options`와 `optionsHandler` static 필드 초기화.  
`SmartMovingInfo.ModComMessage`를 INFO 로그로 출력.

---

## 인스턴스 초기화

### `initialize(boolean alwaysSendMessage)`

```java
public void initialize(boolean alwaysSendMessage)
{
    if(Options._globalConfig.value)
        SmartMovingPacketStream.sendConfigContent(mp, optionsHandler.writeToProperties(), null);
    else if(Options._serverConfig.value)
        SmartMovingPacketStream.sendConfigContent(mp, optionsHandler.writeToProperties(mp, false), null);
    else if(alwaysSendMessage)
        SmartMovingPacketStream.sendConfigContent(mp, Options.enabled ? new String[0] : null, null);
    initialized = true;
}
```

플레이어 접속 시 설정 전송 분기:

| 조건 | 전송 내용 |
|------|-----------|
| `Options._globalConfig.value == true` | 전체 서버 설정 (`writeToProperties()`) |
| `Options._serverConfig.value == true` | 이 플레이어 개별 설정 (`writeToProperties(mp, false)`) |
| `alwaysSendMessage == true` | `Options.enabled ? new String[0] : null` (빈 배열=활성, null=비활성) |
| 나머지 | 전송 없음 |

`initialized = true` — 이후 `processStatePacket`에서 재호출 방지.

---

## 패킷 처리 메서드

### `processStatePacket(FMLProxyPacket packet, long state)`

```java
public void processStatePacket(FMLProxyPacket packet, long state)
{
    if(!initialized)
        initialize(false);

    boolean isCrawling = ((state >>> 13) & 1) != 0;
    setCrawling(isCrawling);

    boolean isSmall = ((state >>> 15) & 1) != 0;
    setSmall(isSmall);

    boolean isClimbing = ((state >>> 14) & 1) != 0;
    boolean isCrawlClimbing = ((state >>> 12) & 1) != 0;
    boolean isCeilingClimbing = ((state >>> 18) & 1) != 0;

    boolean isWallJumping = ((state >>> 31) & 1) != 0;

    isSneakButtonPressed = ((state >>> 33) & 1) != 0;

    resetFallDistance = isClimbing || isCrawlClimbing || isCeilingClimbing || isWallJumping;
    resetTicksForFloatKick = isClimbing || isCrawlClimbing || isCeilingClimbing;
    mp.sendPacketToTrackedPlayers(packet);
}
```

클라이언트 상태 long에서 서버가 실제로 사용하는 비트만 추출:

| 비트 위치 | 상태 |
|----------|------|
| `>>> 12` | isCrawlClimbing |
| `>>> 13` | isCrawling |
| `>>> 14` | isClimbing |
| `>>> 15` | isSmall |
| `>>> 18` | isCeilingClimbing |
| `>>> 31` | isWallJumping |
| `>>> 33` | isSneakButtonPressed |

**`resetFallDistance` 조건:** `isClimbing || isCrawlClimbing || isCeilingClimbing || isWallJumping`  
**`resetTicksForFloatKick` 조건:** `isClimbing || isCrawlClimbing || isCeilingClimbing` (벽점프 제외)

마지막으로 `mp.sendPacketToTrackedPlayers(packet)` — 원본 패킷을 추적 중인 다른 플레이어에게 릴레이.

---

### `processConfigPacket(String clientConfigurationVersion)`

```java
public void processConfigPacket(String clientConfigurationVersion)
{
    boolean warn = true;
    String type = "unknown";
    if(clientConfigurationVersion != null)
        for(int i = 0; i < SmartMovingConfig._all.length; i++)
            if(clientConfigurationVersion.equals(SmartMovingConfig._all[i]))
            {
                warn = i > 0;
                type = warn ? "outdated" : "matching";
                break;
            }

    String message = "Smart Moving player \"" + mp.getUsername() + "\" connected with " + type + " configuration system";
    if(clientConfigurationVersion != null)
        message += " version \"" + clientConfigurationVersion + "\"";

    if(warn)
        FMLLog.warning(message);
    else
        FMLLog.info(message);
}
```

클라이언트 설정 버전 호환성 확인 및 로그 출력.

**판별 로직:**
- `clientConfigurationVersion == null` → `type = "unknown"`, `warn = true`
- `SmartMovingConfig._all[0]`과 일치 → `warn = false`, `type = "matching"` (최신 버전)
- `SmartMovingConfig._all[i > 0]`와 일치 → `warn = true`, `type = "outdated"` (구버전)
- 배열 내 어디에도 없음 → `warn = true`, `type = "unknown"`

로그: `warn == true` → `FMLLog.warning()`, `warn == false` → `FMLLog.info()`

---

### `processConfigChangePacket(String localUserName)`

```java
public void processConfigChangePacket(String localUserName)
{
    if(!Options._globalConfig.value)
    {
        toggleSingleConfig();
        return;
    }

    String username = mp.getUsername();

    if(localUserName == username)
    {
        toggleConfig();
        return;
    }

    String[] rightPlayerNames = Options._usersWithChangeConfigRights.value;
    for(int i=0; i<rightPlayerNames.length; i++)
        if(rightPlayerNames[i].equals(username))
        {
            toggleConfig();
            return;
        }

    SmartMovingPacketStream.sendConfigChange(mp);
}
```

설정 변경 요청 처리:

| 조건 | 동작 |
|------|------|
| `!Options._globalConfig.value` | `toggleSingleConfig()` (개인 설정만 토글) |
| `localUserName == username` (참조 동등성) | `toggleConfig()` (전체 설정 토글) |
| `username`이 `_usersWithChangeConfigRights`에 포함 | `toggleConfig()` |
| 그 외 | `SmartMovingPacketStream.sendConfigChange(mp)` (변경 거부 응답) |

**주의:** `localUserName == username` 는 `==` 참조 동등성 비교.

---

### `processSpeedChangePacket(int difference, String localUserName)`

```java
public void processSpeedChangePacket(int difference, String localUserName)
{
    if(!Options._globalConfig.value)
    {
        changeSingleSpeed(difference);
        return;
    }

    if(!hasRight(localUserName, Options._usersWithChangeSpeedRights))
        SmartMovingPacketStream.sendSpeedChange(mp, 0, null);
    else
        changeSpeed(difference);
}
```

속도 변경 요청 처리:

| 조건 | 동작 |
|------|------|
| `!Options._globalConfig.value` | `changeSingleSpeed(difference)` |
| 권한 없음 | `sendSpeedChange(mp, 0, null)` (차이 0 → 변경 무효화) |
| 권한 있음 | `changeSpeed(difference)` (전체 브로드캐스트) |

---

### `processHungerChangePacket(float hunger)`

```java
public void processHungerChangePacket(float hunger)
{
    this.hunger = hunger;
}
```

클라이언트가 보낸 배고픔 값을 `this.hunger`에 저장.  
이후 `addMovementStat`에서 소진 처리 시 사용.

---

### `processSoundPacket(String soundId, float volume, float pitch)`

```java
public void processSoundPacket(String soundId, float volume, float pitch)
{
    mp.localPlaySound(soundId, volume, pitch);
}
```

클라이언트에서 요청한 사운드를 서버 측에서 재생.  
`mp.localPlaySound()` 호출 → 해당 플레이어 위치 기준으로 사운드 브로드캐스트.

---

## 권한 확인

### `hasRight(String localUserName, Property<String[]> rights)` → boolean (private)

```java
private boolean hasRight(String localUserName, Property<String[]> rights)
{
    String username = mp.getUsername();

    if(localUserName == username)   // 참조 동등성
        return true;

    String[] rightPlayerNames = rights.value;
    for(int i=0; i<rightPlayerNames.length; i++)
        if(rightPlayerNames[i].equals(username))
            return true;

    return false;
}
```

- `localUserName == username`: 참조 동등성 (`equals` 아님)
- 권한 목록(`rights.value`)에 `username`이 있으면 `true`

---

## 설정 토글/속도 변경

### `toggleSingleConfig()`

```java
public void toggleSingleConfig()
{
    SmartMovingPacketStream.sendConfigContent(mp, optionsHandler.writeToProperties(mp, true), mp.getUsername());
}
```

이 플레이어에게만 개인 설정 전송 (토글).

---

### `toggleConfig()`

```java
public void toggleConfig()
{
    optionsHandler.toggle(mp);
    String[] config = optionsHandler.writeToProperties();
    IEntityPlayerMP[] players = mp.getAllPlayers();
    for(int n=0; n<players.length; n++)
        SmartMovingPacketStream.sendConfigContent(players[n], config, mp.getUsername());
}
```

`optionsHandler.toggle(mp)` → 설정 토글.  
토글 후 `writeToProperties()`로 새 설정 직렬화 → 모든 플레이어에게 브로드캐스트.

---

### `changeSingleSpeed(int difference)`

```java
public void changeSingleSpeed(int difference)
{
    optionsHandler.changeSingleSpeed(mp, difference);
    SmartMovingPacketStream.sendSpeedChange(mp, difference, mp.getUsername());
}
```

이 플레이어만의 속도 변경 후 해당 플레이어에게만 전송.

---

### `changeSpeed(int difference)`

```java
public void changeSpeed(int difference)
{
    optionsHandler.changeSpeed(difference, mp);
    IEntityPlayerMP[] players = mp.getAllPlayers();
    for(int n=0; n<players.length; n++)
        SmartMovingPacketStream.sendSpeedChange(players[n], difference, mp.getUsername());
}
```

전체 속도 변경 후 모든 플레이어에게 브로드캐스트.

---

## 틱 훅 메서드

### `beforeOnUpdate()`

```java
public void beforeOnUpdate()
{
    if (crawlingCooldown > 0)
        crawlingCooldown --;
}
```

매 틱 `crawlingCooldown` 감소.

---

### `afterOnUpdate()`

```java
public void afterOnUpdate()
{
    if(resetFallDistance)
        mp.resetFallDistance();
    if(resetTicksForFloatKick)
        mp.resetTicksForFloatKick();
}
```

매 틱 끝에 낙하 거리 / floatKick 틱 리셋.  
`resetFallDistance`/`resetTicksForFloatKick`은 직전 상태 패킷에서 설정됨.

---

### `beforeOnLivingUpdate()` / `afterOnLivingUpdate()`

```java
public void beforeOnLivingUpdate()
{
    withinOnLivingUpdate = true;
}

public void afterOnLivingUpdate()
{
    withinOnLivingUpdate = false;

    if(!isSmall)
        return;

    if (mp.doGetHealth() <= 0)
        return;

    double offset = SmallSizeItemGrabHeight;  // 0.25F
    AxisAlignedBB box = mp.expandBox(mp.getBox(), 1, offset, 1);

    List<?> offsetEntities = mp.getEntitiesExcludingPlayer(box);
    if (offsetEntities != null && offsetEntities.size() > 0)
    {
        Object[] offsetEntityArray = offsetEntities.toArray();

        box = mp.expandBox(box, 0, -offset, 0);
        List<?> standardEntities = mp.getEntitiesExcludingPlayer(box);

        for (int i=0; i<offsetEntityArray.length; i++)
        {
            Entity offsetEntity = (Entity)offsetEntityArray[i];
            if(standardEntities != null && standardEntities.contains(offsetEntity))
                continue;

            if (!mp.isDeadEntity(offsetEntity))
                mp.onCollideWithPlayer(offsetEntity);
        }
    }
}
```

`afterOnLivingUpdate` — `isSmall` 상태의 아이템 확장 획득 처리:

1. `isSmall == false` 또는 체력 ≤ 0 이면 리턴
2. `mp.getBox()`를 X=+1, Y=+0.25F, Z=+1로 확장한 `offsetBox` 구성
3. `offsetBox` 범위의 엔티티 목록 → `offsetEntities`
4. `offsetBox`에서 Y=-0.25F로 다시 축소한 `standardBox` 범위의 엔티티 목록 → `standardEntities`
5. `offsetEntities` 중 `standardEntities`에 없는 것 = 확장 덕분에 잡힌 엔티티
6. 해당 엔티티가 살아있으면 `mp.onCollideWithPlayer(offsetEntity)` → 아이템 획득 처리

**결과:** isSmall 상태에서도 0.25F 높이만큼 추가로 아이템 획득 가능.

---

## 크롤링 boundingBox 초기화 메서드

### `afterSetPosition(double, double, double)` @SuppressWarnings("unused")

```java
@SuppressWarnings("unused")
public void afterSetPosition(double d, double d1, double d2)
{
    if(!crawlingInitialized)
        mp.setMaxY(mp.getMinY() + mp.getHeight() - 1);
}
```

`crawlingInitialized == false`이면 `maxY = minY + height - 1` 설정.  
`@SuppressWarnings("unused")` — 직접 호출 없이 reflection/ASM 훅으로 호출됨.

---

### `beforeIsPlayerSleeping()`

```java
public void beforeIsPlayerSleeping()
{
    if(!crawlingInitialized)
    {
        mp.setMaxY(mp.getMinY() + mp.getHeight());
        crawlingInitialized = true;
    }
}
```

`crawlingInitialized == false`이면 `maxY = minY + height` (일반 높이)로 설정 후 `crawlingInitialized = true`.

---

### `setCrawling(boolean crawling)`

```java
public void setCrawling(boolean crawling)
{
    if(!crawling && isCrawling)
        crawlingCooldown = 10;
    isCrawling = crawling;
}
```

크롤링 → 비크롤링 전환 시: `crawlingCooldown = 10` 설정.  
이 쿨다운 동안 `isEntityInsideOpaqueBlock()`이 `false` 반환 (블록 내부 판정 억제).

---

### `setSmall(boolean isSmall)`

```java
public void setSmall(boolean isSmall)
{
    mp.setHeight(isSmall ? 0.8F : 1.8F);
    this.isSmall = isSmall;
}
```

| 상태 | 설정 높이 |
|------|-----------|
| `isSmall = true` | `0.8F` |
| `isSmall = false` | `1.8F` |

---

## 소진(exhaustion)/배고픔 인터셉트

### `addMovementStat(double var1, double var3, double var5)`

```java
public void addMovementStat(double var1, double var3, double var5)
{
    beforeAddMovingHungerBatch();
    mp.localAddMovementStat(var1, var3, var5);
    if(disableAddExhaustion && hunger != 0 && !withinOnLivingUpdate)
        mp.localAddExhaustion(hunger);
    afterAddMovingHungerBatch();
}
```

vanilla `addMovementStat` 훅:
1. `beforeAddMovingHungerBatch()` → `disableAddExhaustion = true` (hunger != -1 시)
2. `mp.localAddMovementStat()` 호출 → 내부에서 `addExhaustion()` 호출됨 → `disableAddExhaustion`로 억제됨
3. `disableAddExhaustion && hunger != 0 && !withinOnLivingUpdate` → `mp.localAddExhaustion(hunger)` (커스텀 소진값 직접 적용)
4. `afterAddMovingHungerBatch()` → `disableAddExhaustion = false`

**목적:** vanilla의 이동 소진 계산을 억제하고, 클라이언트가 보낸 `hunger` 값으로 대체.

---

### `beforeAddMovingHungerBatch()` / `afterAddMovingHungerBatch()`

```java
public void beforeAddMovingHungerBatch()
{
    disableAddExhaustionDepth++;
    if(hunger != -1)
        disableAddExhaustion = true;
}

public void afterAddMovingHungerBatch()
{
    disableAddExhaustionDepth--;
    if(disableAddExhaustionDepth == 0)
        disableAddExhaustion = false;
}
```

**depth 카운터**: 중첩 호출 가능 — depth가 0이 될 때만 `disableAddExhaustion = false`.  
`hunger == -1` → 소진 억제 비활성 (vanilla 소진 그대로 허용).

---

### `addExhaustion(float exhaustion)`

```java
public void addExhaustion(float exhaustion)
{
    if(!disableAddExhaustion)
        mp.localAddExhaustion(exhaustion);
}
```

vanilla `addExhaustion` 훅. `disableAddExhaustion == true`이면 소진 추가 무시.

---

## 스니킹 오버라이드

### `isSneaking()` → boolean

```java
public boolean isSneaking()
{
    if(forceIsSneaking != null)
        return forceIsSneaking;

    return mp.localIsSneaking();
}
```

`forceIsSneaking != null`이면 강제 값 반환, 아니면 vanilla `localIsSneaking()`.

---

### `beforeActivateBlockOrUseItem()` / `afterActivateBlockOrUseItem()`

```java
public void beforeActivateBlockOrUseItem()
{
    forceIsSneaking = isSneakButtonPressed;
}

public void afterActivateBlockOrUseItem()
{
    forceIsSneaking = null;
}
```

`activateBlockOrUseItem` 실행 동안만 `isSneakButtonPressed` 값으로 스니킹 강제.  
**목적:** 크롤링/스몰 상태에서도 블록 상호작용 시 스니킹 판정이 클라이언트 버튼 상태와 동기화되도록.

---

### `isEntityInsideOpaqueBlock()` → boolean

```java
public boolean isEntityInsideOpaqueBlock()
{
    if(crawlingCooldown > 0)
        return false;

    return mp.localIsEntityInsideOpaqueBlock();
}
```

`crawlingCooldown > 0`이면 항상 `false` (크롤링 종료 직후 10틱간 블록 내부 판정 억제).  
그 외엔 vanilla 로직에 위임.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `IEntityPlayerMP` | 플레이어 조작 인터페이스 전체 |
| `SmartMovingPacketStream` | 각종 패킷 전송 |
| `SmartMovingConfig` | `Options` 정적 필드 |
| `SmartMovingServerOptions` | `optionsHandler` 정적 필드, 설정 읽기/쓰기/토글 |
| `SmartMovingInfo` | `ModComMessage` 로그 메시지 |
| `FMLProxyPacket` | 상태 패킷 |
| `FMLLog` | 경고/정보 로그 |
| `Property<String[]>` | 권한 목록 필드 타입 |

---

## 주요 관찰 사항

1. **상태 패킷 비트 위치**: 서버가 추출하는 비트는 7개뿐 (12, 13, 14, 15, 18, 31, 33). 나머지 비트는 서버가 읽지 않고 `sendPacketToTrackedPlayers`로 그대로 릴레이.

2. **`crawlingCooldown = 10`**: 크롤링 종료 후 10틱간 `isEntityInsideOpaqueBlock` 억제. 크롤링 종료 시 boundingBox 복원 전 블록 충돌 판정 방지.

3. **isSmall 높이**: `0.8F` (isSmall) / `1.8F` (normal). 서버 측 boundingBox 높이도 직접 변경.

4. **소진 인터셉트 구조**: vanilla의 `addMovementStat → addExhaustion` 경로를 `disableAddExhaustion` 플래그로 차단하고, 클라이언트 `hunger` 값으로 대체. `withinOnLivingUpdate` 중에는 클라이언트 hunger 적용도 억제.

5. **`localUserName == username` 참조 비교**: `processConfigChangePacket`, `hasRight` 두 곳에서 모두 `==` 비교. 문자열 인터닝 의존.

6. **`afterSetPosition` @SuppressWarnings("unused")**: ASM 훅으로 호출되므로 IDE가 "미사용"으로 경고 → 어노테이션 억제.

7. **1.21.1 이식**: FML → Fabric 네트워크 API 교체 필요. `IEntityPlayerMP` 인터페이스는 Fabric의 `ServerPlayerEntity` wrapping으로 대체. `resetTicksForFloatKick`의 vanilla 동작 확인 필요.
