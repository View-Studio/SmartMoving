# SmartMovingComm.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMovingComm.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: `SmartMovingComm extends SmartMovingContext implements IPacketReceiver, IPacketSender`

---

## 전체 소스

```java
package net.smart.moving;

import cpw.mods.fml.common.network.internal.*;
import net.minecraft.client.*;
import net.minecraft.client.entity.*;
import net.minecraft.entity.*;
import net.minecraft.network.play.client.*;
import net.minecraft.server.*;
import net.smart.moving.config.*;
import net.smart.properties.*;

public class SmartMovingComm extends SmartMovingContext implements IPacketReceiver, IPacketSender
{
	@Override
	public boolean processStatePacket(FMLProxyPacket packet, IEntityPlayerMP player, int entityId, long state)
	{
		Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(entityId);
		if(entity == null)
			return true;

		SmartMovingOther moving = SmartMovingFactory.getOtherSmartMoving((EntityOtherPlayerMP)entity);
		if(moving != null)
			moving.processStatePacket(state);
		return true;
	}

	@Override
	public boolean processConfigInfoPacket(FMLProxyPacket packet, IEntityPlayerMP player, String info)
	{
		return false;
	}

	@Override
	public boolean processConfigContentPacket(FMLProxyPacket packet, IEntityPlayerMP player, String[] content, String username)
	{
		processConfigPacket(content, username, false);
		return true;
	}

	@Override
	public boolean processConfigChangePacket(FMLProxyPacket packet, IEntityPlayerMP player)
	{
		SmartMovingOptions.writeNoRightsToChangeConfigMessageToChat(isConnectedToRemoteServer());
		return true;
	}

	@Override
	public boolean processSpeedChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, int difference, String username)
	{
		if(difference == 0)
			SmartMovingOptions.writeNoRightsToChangeSpeedMessageToChat(isConnectedToRemoteServer());
		else
		{
			Config.changeSpeed(difference);
			Options.writeServerSpeedMessageToChat(username, Config._globalConfig.value);
		}
		return true;
	}

	@Override
	public boolean processHungerChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, float hunger)
	{
		return false;
	}

	@Override
	public boolean processSoundPacket(FMLProxyPacket packet, IEntityPlayerMP player, String soundId, float distance, float pitch)
	{
		return false;
	}

	private static boolean isConnectedToRemoteServer()
	{
		return MinecraftServer.getServer() == null
			|| Minecraft.getMinecraft().getIntegratedServer() == null
			|| !Minecraft.getMinecraft().getIntegratedServer().isSinglePlayer();
	}

	public static void processConfigPacket(String[] content, String username, boolean blockCode)
	{
		boolean isGloballyConfigured = false;
		if(content != null && content.length == 2 && Options._globalConfig.getCurrentKey().equals(content[0]))
		{
			isGloballyConfigured = "true".equals(content[1]);
			content = null;
		}

		boolean wasEnabled = Config.enabled;
		boolean first = Config != ServerConfig;
		if(first)
			ServerConfig.reset();

		if(content != null)
			if(content.length != 0)
			{
				ServerConfig.loadFromProperties(content, blockCode);
				isGloballyConfigured = ServerConfig._globalConfig.value;
			}
			else
			{
				Config = Options;
				Options.writeServerDeconfigMessageToChat();
				return;
			}
		else
		{
			ServerConfig.load(false);
			ServerConfig.setCurrentKey(null);
		}

		ServerConfig._globalConfig.value = isGloballyConfigured;

		if(!first)
		{
			Options.writeServerReconfigMessageToChat(wasEnabled, username, isGloballyConfigured);
			return;
		}

		Config = ServerConfig;
		Options.writeServerConfigMessageToChat();
		if (!blockCode)
			SmartMovingPacketStream.sendConfigInfo(SmartMovingComm.instance, SmartMovingConfig._sm_current);
	}

	@Override
	public void sendPacket(byte[] data)
	{
		Minecraft.getMinecraft().getNetHandler().addToSendQueue(new C17PacketCustomPayload(SmartMovingPacketStream.Id, data));
	}

	public static boolean processBlockCode(String text)
	{
		if(!text.startsWith("§0§1") || !text.endsWith("§f§f"))
			return false;

		String codes = text.substring(4, text.length() - 4);
		processBlockCode(codes, "§0", Options._baseClimb, "standard");
		processBlockCode(codes, "§1", Options._freeClimb);
		processBlockCode(codes, "§2", Options._ceilingClimbing);
		processBlockCode(codes, "§3", Options._swim);
		processBlockCode(codes, "§4", Options._dive);
		processBlockCode(codes, "§5", Options._crawl);
		processBlockCode(codes, "§6", Options._slide);
		processBlockCode(codes, "§7", Options._fly);
		processBlockCode(codes, "§8", Options._jumpCharge);
		processBlockCode(codes, "§9", Options._headJump);
		processBlockCode(codes, "§a", Options._angleJumpSide);
		processBlockCode(codes, "§b", Options._angleJumpBack);
		return true;
	}

	private static void processBlockCode(String text, String blockCode, Property<?> property, String... value)
	{
		if(text.contains(blockCode))
			processConfigPacket(new String[] { property.getCurrentKey(), value.length > 0 ? value[0] : "false" }, null, true);
	}

	public static final SmartMovingComm instance = new SmartMovingComm();
}
```

---

## 역할

**클라이언트 측 패킷 수신/송신 디스패처**.  
`SmartMovingServerComm`의 클라이언트 대응물.

두 가지 역할:
1. **수신(`IPacketReceiver`)**: 서버에서 오는 패킷을 받아 처리 — 다른 플레이어 상태 갱신, 서버 설정 적용, 속도 변경 등
2. **송신(`IPacketSender`)**: 클라이언트→서버 방향 패킷을 `C17PacketCustomPayload`로 전송

`SmartMovingServerComm`과의 차이:
- `SmartMovingServerComm`: 서버에서 클라이언트 패킷 수신 → `SmartMovingServer` 위임
- `SmartMovingComm`: 클라이언트에서 서버 패킷 수신 → 직접 처리 (설정 적용, 다른 플레이어 상태)

---

## import

```java
import cpw.mods.fml.common.network.internal.*;  // FMLProxyPacket
import net.minecraft.client.*;                  // Minecraft
import net.minecraft.client.entity.*;           // EntityOtherPlayerMP
import net.minecraft.entity.*;                  // Entity
import net.minecraft.network.play.client.*;     // C17PacketCustomPayload
import net.minecraft.server.*;                  // MinecraftServer
import net.smart.moving.config.*;               // SmartMovingConfig, SmartMovingOptions
import net.smart.properties.*;                  // Property
```

---

## 정적(static) 필드

```java
public static final SmartMovingComm instance = new SmartMovingComm();
```

싱글톤 인스턴스. 상태 없음.

---

## IPacketReceiver 구현

### `processStatePacket(FMLProxyPacket, IEntityPlayerMP player, int entityId, long state)` → boolean

```java
@Override
public boolean processStatePacket(FMLProxyPacket packet, IEntityPlayerMP player, int entityId, long state)
{
    Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(entityId);
    if(entity == null)
        return true;

    SmartMovingOther moving = SmartMovingFactory.getOtherSmartMoving((EntityOtherPlayerMP)entity);
    if(moving != null)
        moving.processStatePacket(state);
    return true;
}
```

**서버가 릴레이한 다른 플레이어의 상태 패킷 처리.**

1. 클라이언트 월드에서 `entityId`로 엔티티 조회
2. 엔티티가 없으면 `true` 반환 (무시)
3. `SmartMovingFactory.getOtherSmartMoving((EntityOtherPlayerMP)entity)` → `SmartMovingOther` 인스턴스
4. `moving != null`이면 `moving.processStatePacket(state)` 호출

`player` 파라미터: 사용하지 않음 (로컬 플레이어).  
`packet` 파라미터: 사용하지 않음.

---

### `processConfigInfoPacket(FMLProxyPacket, IEntityPlayerMP, String info)` → boolean

```java
@Override
public boolean processConfigInfoPacket(FMLProxyPacket packet, IEntityPlayerMP player, String info)
{
    return false;
}
```

아무 처리 없음. 반환: `false`.  
(서버 측 `SmartMovingServerComm`이 처리하는 메서드 — 클라이언트는 config info를 수신하지 않음)

---

### `processConfigContentPacket(FMLProxyPacket, IEntityPlayerMP, String[] content, String username)` → boolean

```java
@Override
public boolean processConfigContentPacket(FMLProxyPacket packet, IEntityPlayerMP player, String[] content, String username)
{
    processConfigPacket(content, username, false);
    return true;
}
```

서버에서 보내온 설정 내용 패킷 → `processConfigPacket(content, username, false)` 호출.  
반환: `true`.

---

### `processConfigChangePacket(FMLProxyPacket, IEntityPlayerMP)` → boolean

```java
@Override
public boolean processConfigChangePacket(FMLProxyPacket packet, IEntityPlayerMP player)
{
    SmartMovingOptions.writeNoRightsToChangeConfigMessageToChat(isConnectedToRemoteServer());
    return true;
}
```

서버가 설정 변경 권한 없음을 알려올 때 클라이언트 채팅창에 메시지 출력.  
`isConnectedToRemoteServer()` 결과를 메시지에 전달.  
반환: `true`.

---

### `processSpeedChangePacket(FMLProxyPacket, IEntityPlayerMP, int difference, String username)` → boolean

```java
@Override
public boolean processSpeedChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, int difference, String username)
{
    if(difference == 0)
        SmartMovingOptions.writeNoRightsToChangeSpeedMessageToChat(isConnectedToRemoteServer());
    else
    {
        Config.changeSpeed(difference);
        Options.writeServerSpeedMessageToChat(username, Config._globalConfig.value);
    }
    return true;
}
```

| 조건 | 동작 |
|------|------|
| `difference == 0` | 채팅에 "권한 없음" 메시지 |
| `difference != 0` | `Config.changeSpeed(difference)` + 속도 변경 메시지 |

`Config`는 `SmartMovingContext`의 static 필드.  
반환: `true`.

---

### `processHungerChangePacket(FMLProxyPacket, IEntityPlayerMP, float hunger)` → boolean

```java
@Override
public boolean processHungerChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, float hunger)
{
    return false;
}
```

아무 처리 없음. 반환: `false`.  
(hunger 패킷은 클라이언트→서버 단방향 — 클라이언트가 수신할 일 없음)

---

### `processSoundPacket(FMLProxyPacket, IEntityPlayerMP, String soundId, float distance, float pitch)` → boolean

```java
@Override
public boolean processSoundPacket(FMLProxyPacket packet, IEntityPlayerMP player, String soundId, float distance, float pitch)
{
    return false;
}
```

아무 처리 없음. 반환: `false`.  
(sound 패킷도 클라이언트→서버 단방향)

---

## IPacketReceiver 반환값 요약

| 메서드 | 반환값 |
|--------|--------|
| `processStatePacket` | `true` |
| `processConfigInfoPacket` | **`false`** |
| `processConfigContentPacket` | `true` |
| `processConfigChangePacket` | `true` |
| `processSpeedChangePacket` | `true` |
| `processHungerChangePacket` | **`false`** |
| `processSoundPacket` | **`false`** |

---

## IPacketSender 구현

### `sendPacket(byte[] data)`

```java
@Override
public void sendPacket(byte[] data)
{
    Minecraft.getMinecraft().getNetHandler().addToSendQueue(new C17PacketCustomPayload(SmartMovingPacketStream.Id, data));
}
```

클라이언트→서버 방향 패킷 전송.  
`C17PacketCustomPayload(SmartMovingPacketStream.Id, data)` — SmartMoving 전용 채널 ID로 래핑 후 전송 큐에 추가.

---

## 정적 유틸리티 메서드

### `isConnectedToRemoteServer()` → boolean (private static)

```java
private static boolean isConnectedToRemoteServer()
{
    return MinecraftServer.getServer() == null
        || Minecraft.getMinecraft().getIntegratedServer() == null
        || !Minecraft.getMinecraft().getIntegratedServer().isSinglePlayer();
}
```

원격(멀티) 서버에 접속 중인지 판별.

| 조건 | 의미 |
|------|------|
| `MinecraftServer.getServer() == null` | 서버 없음 → 원격 |
| `getIntegratedServer() == null` | 통합 서버 없음 → 원격 |
| `!getIntegratedServer().isSinglePlayer()` | 통합 서버지만 싱글이 아님 → 원격 |

세 조건 중 하나라도 true면 `true` 반환 (원격 서버).  
채팅 메시지 내용이 싱글/멀티에 따라 달라질 때 사용.

---

### `processConfigPacket(String[] content, String username, boolean blockCode)` (public static)

```java
public static void processConfigPacket(String[] content, String username, boolean blockCode)
{
    boolean isGloballyConfigured = false;
    if(content != null && content.length == 2 && Options._globalConfig.getCurrentKey().equals(content[0]))
    {
        isGloballyConfigured = "true".equals(content[1]);
        content = null;
    }

    boolean wasEnabled = Config.enabled;
    boolean first = Config != ServerConfig;
    if(first)
        ServerConfig.reset();

    if(content != null)
        if(content.length != 0)
        {
            ServerConfig.loadFromProperties(content, blockCode);
            isGloballyConfigured = ServerConfig._globalConfig.value;
        }
        else
        {
            Config = Options;
            Options.writeServerDeconfigMessageToChat();
            return;
        }
    else
    {
        ServerConfig.load(false);
        ServerConfig.setCurrentKey(null);
    }

    ServerConfig._globalConfig.value = isGloballyConfigured;

    if(!first)
    {
        Options.writeServerReconfigMessageToChat(wasEnabled, username, isGloballyConfigured);
        return;
    }

    Config = ServerConfig;
    Options.writeServerConfigMessageToChat();
    if (!blockCode)
        SmartMovingPacketStream.sendConfigInfo(SmartMovingComm.instance, SmartMovingConfig._sm_current);
}
```

서버에서 받은 설정 내용을 클라이언트에 적용하는 핵심 메서드.

**파라미터:**
- `content`: 설정 키-값 쌍 배열 (null=기본 로드, 빈 배열=설정 해제, 실제 배열=설정 적용)
- `username`: 설정 변경을 요청한 플레이어 이름 (채팅 메시지용)
- `blockCode`: 블록 코드 경유 여부 (true이면 configInfo 전송 안 함)

**단계별 동작:**

**1단계 — 전역 설정 감지:**
```java
if(content != null && content.length == 2 && Options._globalConfig.getCurrentKey().equals(content[0]))
{
    isGloballyConfigured = "true".equals(content[1]);
    content = null;
}
```
`content`가 `[globalConfig키, "true"/"false"]` 2-element 배열이면 → `isGloballyConfigured` 추출 후 `content = null`로 처리.

**2단계 — first 판별:**
```java
boolean first = Config != ServerConfig;
if(first)
    ServerConfig.reset();
```
`Config == ServerConfig`이면 이미 서버 설정 적용 중 (`first = false`).  
`first = true`이면 처음 서버 설정 수신 → `ServerConfig.reset()`.

**3단계 — content 분기:**

| `content` 값 | 동작 |
|--------------|------|
| `!= null` && `length != 0` | `ServerConfig.loadFromProperties(content, blockCode)` + `isGloballyConfigured = ServerConfig._globalConfig.value` |
| `!= null` && `length == 0` | `Config = Options` (설정 해제), `writeServerDeconfigMessageToChat()`, **return** |
| `null` | `ServerConfig.load(false)`, `ServerConfig.setCurrentKey(null)` |

`content.length == 0` (빈 배열): 서버 설정을 해제하고 Options(클라이언트 기본 설정)로 복귀.

**4단계 — 전역 설정 플래그 반영:**
```java
ServerConfig._globalConfig.value = isGloballyConfigured;
```

**5단계 — first 여부에 따른 분기:**

| `first` | 동작 |
|---------|------|
| `false` (재설정) | `writeServerReconfigMessageToChat(wasEnabled, username, isGloballyConfigured)` 후 return |
| `true` (첫 설정) | `Config = ServerConfig`, `writeServerConfigMessageToChat()` + (`!blockCode`이면) `sendConfigInfo()` |

`first = true`(최초): `Config`가 `ServerConfig`로 교체됨 — 이후 모든 설정 조회는 서버 설정 사용.  
`first = false`(재설정): `Config = ServerConfig`는 이미 설정된 상태이므로 채팅 메시지만 출력.

---

### `processBlockCode(String text)` → boolean (public static)

```java
public static boolean processBlockCode(String text)
{
    if(!text.startsWith("§0§1") || !text.endsWith("§f§f"))
        return false;

    String codes = text.substring(4, text.length() - 4);
    processBlockCode(codes, "§0", Options._baseClimb, "standard");
    processBlockCode(codes, "§1", Options._freeClimb);
    processBlockCode(codes, "§2", Options._ceilingClimbing);
    processBlockCode(codes, "§3", Options._swim);
    processBlockCode(codes, "§4", Options._dive);
    processBlockCode(codes, "§5", Options._crawl);
    processBlockCode(codes, "§6", Options._slide);
    processBlockCode(codes, "§7", Options._fly);
    processBlockCode(codes, "§8", Options._jumpCharge);
    processBlockCode(codes, "§9", Options._headJump);
    processBlockCode(codes, "§a", Options._angleJumpSide);
    processBlockCode(codes, "§b", Options._angleJumpBack);
    return true;
}
```

채팅 텍스트에 포함된 블록 코드로 설정을 적용.

**형식 감지:**
- 시작: `"§0§1"` (4글자)
- 끝: `"§f§f"` (4글자)
- 불일치 → `false` 반환

**코드 추출:**
```java
String codes = text.substring(4, text.length() - 4);
```
앞 4자, 뒤 4자를 제거한 내용이 실제 블록 코드 문자열.

**블록 코드 → 설정 매핑:**

| 블록 코드 | 설정 Property | 적용 값 |
|----------|--------------|---------|
| `"§0"` | `Options._baseClimb` | `"standard"` |
| `"§1"` | `Options._freeClimb` | `"false"` |
| `"§2"` | `Options._ceilingClimbing` | `"false"` |
| `"§3"` | `Options._swim` | `"false"` |
| `"§4"` | `Options._dive` | `"false"` |
| `"§5"` | `Options._crawl` | `"false"` |
| `"§6"` | `Options._slide` | `"false"` |
| `"§7"` | `Options._fly` | `"false"` |
| `"§8"` | `Options._jumpCharge` | `"false"` |
| `"§9"` | `Options._headJump` | `"false"` |
| `"§a"` | `Options._angleJumpSide` | `"false"` |
| `"§b"` | `Options._angleJumpBack` | `"false"` |

`"§0"`만 `"standard"` 값, 나머지는 모두 `"false"` (기능 비활성화).  
반환: 블록 코드 패턴이면 `true`, 아니면 `false`.

---

### `processBlockCode(String text, String blockCode, Property<?> property, String... value)` (private static)

```java
private static void processBlockCode(String text, String blockCode, Property<?> property, String... value)
{
    if(text.contains(blockCode))
        processConfigPacket(new String[] { property.getCurrentKey(), value.length > 0 ? value[0] : "false" }, null, true);
}
```

`text`에 `blockCode`가 포함되면:
- `processConfigPacket([property.getCurrentKey(), value[0] or "false"], null, true)` 호출
- `value` 배열이 비어있으면 `"false"` 사용

`blockCode = true` → `sendConfigInfo()` 호출 안 함.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingContext` | 상속 (`Config`, `ServerConfig`, `Options` static 필드) |
| `IPacketReceiver` | 구현 인터페이스 |
| `IPacketSender` | 구현 인터페이스 |
| `SmartMovingOther` | 다른 플레이어 상태 갱신 |
| `SmartMovingFactory` | `getOtherSmartMoving()` |
| `SmartMovingPacketStream` | `sendConfigInfo()`, `Id` |
| `SmartMovingConfig` | `_sm_current`, `_all` |
| `SmartMovingOptions` | `writeNoRightsToXxxMessageToChat()` |
| `Property<?>` | 블록 코드 설정 |
| `Minecraft` | `theWorld`, `getNetHandler()`, `getIntegratedServer()` |
| `MinecraftServer` | `getServer()` |
| `EntityOtherPlayerMP` | 다른 플레이어 엔티티 타입 |
| `C17PacketCustomPayload` | 클라이언트→서버 커스텀 패킷 |

---

## 주요 관찰 사항

1. **클라이언트 측 comm**: `SmartMovingServerComm`은 서버 측, `SmartMovingComm`은 클라이언트 측. 대칭 구조.

2. **`processStatePacket`의 대상**: 로컬 플레이어가 아닌 **다른 플레이어**(`EntityOtherPlayerMP`) — 서버가 릴레이한 타인 상태 갱신. 로컬 플레이어 상태는 클라이언트가 직접 관리.

3. **설정 흐름**: 서버 설정 첫 수신 시 `Config = ServerConfig`로 전환. 이후 모든 설정 조회는 ServerConfig 기준. `content.length == 0` (빈 배열) 시 `Config = Options`로 복귀 (서버 설정 해제).

4. **블록 코드 메커니즘**: `§0§1...§f§f` 형식의 채팅 텍스트로 기능 on/off 제어. 기능 비활성화는 전부 `"false"`, `_baseClimb`만 `"standard"` 값으로 특이 처리.

5. **`blockCode` 파라미터 = `processConfigPacket` 재귀 방지**: 블록 코드 경유 시 `blockCode=true` → `sendConfigInfo()` 호출 안 함. 무한 루프 방지.

6. **`Config`, `ServerConfig`, `Options` static 필드**: 이 파일에서 직접 확인되지 않음 — `SmartMovingContext`에서 선언. `SmartMovingContext` 리서치 시 확인 필요.

7. **1.21.1 이식**: `C17PacketCustomPayload` → Fabric `PacketByteBuf` + `ClientPlayNetworking`. `EntityOtherPlayerMP` → `OtherClientPlayerEntity`. 블록 코드 메커니즘(채팅 텍스트 파싱)은 바닐라 채팅 이벤트 훅으로 대체.
