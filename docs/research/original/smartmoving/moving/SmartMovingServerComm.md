# SmartMovingServerComm.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMovingServerComm.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: `SmartMovingServerComm implements IPacketReceiver`

---

## 전체 소스

```java
package net.smart.moving;

import cpw.mods.fml.common.network.internal.*;

public class SmartMovingServerComm implements IPacketReceiver
{
	public static ILocalUserNameProvider localUserNameProvider = null;

	@Override
	public boolean processStatePacket(FMLProxyPacket packet, IEntityPlayerMP player, int entityId, long state)
	{
		player.getMoving().processStatePacket(packet, state);
		return true;
	}

	@Override
	public boolean processConfigInfoPacket(FMLProxyPacket packet, IEntityPlayerMP player, String info)
	{
		player.getMoving().processConfigPacket(info);
		return true;
	}

	@Override
	public boolean processConfigContentPacket(FMLProxyPacket packet, IEntityPlayerMP player, String[] content, String username)
	{
		return false;
	}

	@Override
	public boolean processConfigChangePacket(FMLProxyPacket packet, IEntityPlayerMP player)
	{
		player.getMoving().processConfigChangePacket(localUserNameProvider != null ? localUserNameProvider.getLocalConfigUserName() : null);
		return true;
	}

	@Override
	public boolean processSpeedChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, int difference, String username)
	{
		player.getMoving().processSpeedChangePacket(difference, localUserNameProvider != null ? localUserNameProvider.getLocalSpeedUserName() : null);
		return true;
	}

	@Override
	public boolean processHungerChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, float hunger)
	{
		player.getMoving().processHungerChangePacket(hunger);
		return true;
	}

	@Override
	public boolean processSoundPacket(FMLProxyPacket packet, IEntityPlayerMP player, String soundId, float volume, float pitch)
	{
		player.getMoving().processSoundPacket(soundId, volume, pitch);
		return true;
	}

	public static final SmartMovingServerComm instance = new SmartMovingServerComm();
}
```

---

## 역할

서버 측 **패킷 수신 디스패처**.  
`IPacketReceiver`를 구현하여 클라이언트에서 도착한 각 패킷을 해당 플레이어의 `SmartMovingServer` 인스턴스로 라우팅하는 역할만 한다.  
로직은 없고 전부 `player.getMoving().processXxx()`로 위임.

---

## import

```java
import cpw.mods.fml.common.network.internal.*;  // FMLProxyPacket
```

---

## 정적(static) 필드

```java
public static ILocalUserNameProvider localUserNameProvider = null;
```

로컬 유저 이름 제공자. 초기값 `null`.  
`processConfigChangePacket`, `processSpeedChangePacket`에서 로컬 플레이어 이름을 가져올 때 사용.  
외부에서 주입(설정)되어야 함.

```java
public static final SmartMovingServerComm instance = new SmartMovingServerComm();
```

싱글톤 인스턴스. `SmartMovingServerComm`은 상태가 없으므로 1개 공유.

---

## 메서드 (전부 `@Override` — IPacketReceiver 구현)

### `processStatePacket(FMLProxyPacket packet, IEntityPlayerMP player, int entityId, long state)` → boolean

```java
@Override
public boolean processStatePacket(FMLProxyPacket packet, IEntityPlayerMP player, int entityId, long state)
{
    player.getMoving().processStatePacket(packet, state);
    return true;
}
```

- `entityId` 파라미터: 사용하지 않음
- `player.getMoving()` → `SmartMovingServer` 인스턴스로 위임
- `packet`과 `state`를 그대로 전달
- 반환: `true`

---

### `processConfigInfoPacket(FMLProxyPacket packet, IEntityPlayerMP player, String info)` → boolean

```java
@Override
public boolean processConfigInfoPacket(FMLProxyPacket packet, IEntityPlayerMP player, String info)
{
    player.getMoving().processConfigPacket(info);
    return true;
}
```

- `info` = 클라이언트 설정 버전 문자열
- `SmartMovingServer.processConfigPacket(info)`로 위임 (버전 호환 로그 처리)
- 반환: `true`

---

### `processConfigContentPacket(FMLProxyPacket packet, IEntityPlayerMP player, String[] content, String username)` → boolean

```java
@Override
public boolean processConfigContentPacket(FMLProxyPacket packet, IEntityPlayerMP player, String[] content, String username)
{
    return false;
}
```

- **아무 처리도 하지 않음**
- 반환: `false`
- 서버는 클라이언트가 보낸 config content 패킷을 무시함

---

### `processConfigChangePacket(FMLProxyPacket packet, IEntityPlayerMP player)` → boolean

```java
@Override
public boolean processConfigChangePacket(FMLProxyPacket packet, IEntityPlayerMP player)
{
    player.getMoving().processConfigChangePacket(
        localUserNameProvider != null ? localUserNameProvider.getLocalConfigUserName() : null
    );
    return true;
}
```

- `localUserNameProvider != null` → `localUserNameProvider.getLocalConfigUserName()` 전달
- `localUserNameProvider == null` → `null` 전달
- `SmartMovingServer.processConfigChangePacket(localUserName)`으로 위임
- 반환: `true`

---

### `processSpeedChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, int difference, String username)` → boolean

```java
@Override
public boolean processSpeedChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, int difference, String username)
{
    player.getMoving().processSpeedChangePacket(
        difference,
        localUserNameProvider != null ? localUserNameProvider.getLocalSpeedUserName() : null
    );
    return true;
}
```

- `username` 파라미터(패킷에 포함된 이름): **사용하지 않음**
- `localUserNameProvider.getLocalSpeedUserName()` 또는 `null`을 localUserName으로 전달
- `SmartMovingServer.processSpeedChangePacket(difference, localUserName)`으로 위임
- 반환: `true`

---

### `processHungerChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, float hunger)` → boolean

```java
@Override
public boolean processHungerChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, float hunger)
{
    player.getMoving().processHungerChangePacket(hunger);
    return true;
}
```

- `SmartMovingServer.processHungerChangePacket(hunger)`으로 위임
- 반환: `true`

---

### `processSoundPacket(FMLProxyPacket packet, IEntityPlayerMP player, String soundId, float volume, float pitch)` → boolean

```java
@Override
public boolean processSoundPacket(FMLProxyPacket packet, IEntityPlayerMP player, String soundId, float volume, float pitch)
{
    player.getMoving().processSoundPacket(soundId, volume, pitch);
    return true;
}
```

- `SmartMovingServer.processSoundPacket(soundId, volume, pitch)`으로 위임
- 반환: `true`

---

## IPacketReceiver 반환값 요약

| 메서드 | 반환값 | 의미 |
|--------|--------|------|
| `processStatePacket` | `true` | 처리 완료 |
| `processConfigInfoPacket` | `true` | 처리 완료 |
| `processConfigContentPacket` | **`false`** | 처리 안 함 (서버 무시) |
| `processConfigChangePacket` | `true` | 처리 완료 |
| `processSpeedChangePacket` | `true` | 처리 완료 |
| `processHungerChangePacket` | `true` | 처리 완료 |
| `processSoundPacket` | `true` | 처리 완료 |

`IPacketReceiver`의 boolean 반환값이 무엇을 의미하는지는 이 파일만으로 확인 불가 — `IPacketReceiver` 리서치 시 확인 필요.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `IPacketReceiver` | 구현 인터페이스 |
| `IEntityPlayerMP` | `getMoving()` → `SmartMovingServer` 접근 |
| `ILocalUserNameProvider` | `getLocalConfigUserName()`, `getLocalSpeedUserName()` |
| `SmartMovingServer` | 실제 처리 위임 대상 (`player.getMoving()`) |
| `FMLProxyPacket` | 패킷 파라미터 타입 |

---

## 주요 관찰 사항

1. **순수 디스패처**: 이 클래스 자체에는 상태와 로직이 없다. `player.getMoving()`으로 `SmartMovingServer`에 전부 위임.

2. **`processConfigContentPacket` → `false`**: 서버는 클라이언트가 보낸 설정 내용 패킷을 수신해도 처리하지 않는다. 설정은 서버→클라이언트 방향으로만 배포됨.

3. **`processSpeedChangePacket`의 `username` 파라미터 미사용**: 패킷에 포함된 username 대신 항상 `localUserNameProvider`를 통해 이름을 얻는다.

4. **`processStatePacket`의 `entityId` 미사용**: 서버는 `player` 객체로 이미 대상을 특정하므로 entityId 불필요.

5. **싱글톤 `instance`**: `SmartMovingServerComm`은 인스턴스 상태 없음 → 하나의 인스턴스를 공유.

6. **1.21.1 이식**: `FMLProxyPacket` → Fabric 네트워크 패킷 API로 교체. `IPacketReceiver` 인터페이스 → Fabric `ServerPlayNetworking.PlayChannelHandler`에 해당하는 구조로 재설계 필요. `player.getMoving()` 패턴은 그대로 유지 가능 (1.21.1 서버 플레이어 래퍼에 동일 메서드 추가).
