# SmartMovingPacketStream.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMovingPacketStream.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: 없음 (`Object` 직접 상속)

---

## 전체 소스

```java
package net.smart.moving;

import java.io.*;
import java.util.*;

import cpw.mods.fml.common.network.internal.*;

public class SmartMovingPacketStream
{
	public static final String Id;
	public static final Set<StackTraceElement> errors;

	static
	{
		String id = SmartMovingInfo.ModComId;
		if(id.length() > 15)
			id = id.substring(0, 15);
		Id = id;
		errors = new HashSet<StackTraceElement>();
	}

	public static void receivePacket(FMLProxyPacket packet, IPacketReceiver comm, IEntityPlayerMP player)
	{
		try
		{
			ByteArrayInputStream byteInput = new ByteArrayInputStream(packet.payload().array());
			ObjectInputStream objectInput = new ObjectInputStream(byteInput);
			byte packetId = objectInput.readByte();
			switch(packetId)
			{
				case SmartMovingInfo.StatePacketId:
					int entityId = objectInput.readInt();
					long state = objectInput.readLong();
					comm.processStatePacket(packet, player, entityId, state);
					break;
				case SmartMovingInfo.ConfigInfoPacketId:
					String info = (String)objectInput.readObject();
					comm.processConfigInfoPacket(packet, player, info);
					break;
				case SmartMovingInfo.ConfigContentPacketId:
					String[] content = (String[])objectInput.readObject();
					String username = (String)objectInput.readObject();
					comm.processConfigContentPacket(packet, player, content, username);
					break;
				case SmartMovingInfo.ConfigChangePacketId:
					comm.processConfigChangePacket(packet, player);
					break;
				case SmartMovingInfo.SpeedChangePacketId:
					int difference = objectInput.readInt();
					username = (String)objectInput.readObject();
					comm.processSpeedChangePacket(packet, player, difference, username);
					break;
				case SmartMovingInfo.HungerChangePacketId:
					float hunger = objectInput.readFloat();
					comm.processHungerChangePacket(packet, player, hunger);
					break;
				case SmartMovingInfo.SoundPacketId:
					String soundId = (String)objectInput.readObject();
					float volume = objectInput.readFloat();
					float pitch = objectInput.readFloat();
					comm.processSoundPacket(packet, player, soundId, volume, pitch);
					break;
				default:
					throw new RuntimeException("Unknown packet id '" + packetId + "' found");
			}
		}
		catch(Throwable t)
		{
			if(errors.add(t.getStackTrace()[0]))
				t.printStackTrace();
			else
				System.err.println(t.getClass().getName() + ": " + t.getMessage());
		}
	}

	public static void sendState(IPacketSender comm, int entityId, long state)
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeByte(SmartMovingInfo.StatePacketId);
			objectOutput.writeInt(entityId);
			objectOutput.writeLong(state);
			objectOutput.flush();
		}
		catch(Throwable t)
		{
			throw new RuntimeException(t);
		}
		comm.sendPacket(byteOutput.toByteArray());
	}

	public static void sendConfigInfo(IPacketSender comm, String info)
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeByte(SmartMovingInfo.ConfigInfoPacketId);
			objectOutput.writeObject(info);
			objectOutput.flush();
		}
		catch(Throwable t)
		{
			throw new RuntimeException(t);
		}
		comm.sendPacket(byteOutput.toByteArray());
	}

	public static void sendConfigContent(IPacketSender comm, String[] content, String username)
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeByte(SmartMovingInfo.ConfigContentPacketId);
			objectOutput.writeObject(content);
			objectOutput.writeObject(username);
			objectOutput.flush();
		}
		catch(Throwable t)
		{
			throw new RuntimeException(t);
		}
		comm.sendPacket(byteOutput.toByteArray());
	}

	public static void sendConfigChange(IPacketSender comm)
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeByte(SmartMovingInfo.ConfigChangePacketId);
			objectOutput.flush();
		}
		catch(Throwable t)
		{
			throw new RuntimeException(t);
		}
		comm.sendPacket(byteOutput.toByteArray());
	}

	public static void sendSpeedChange(IPacketSender comm, int difference, String username)
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeByte(SmartMovingInfo.SpeedChangePacketId);
			objectOutput.writeInt(difference);
			objectOutput.writeObject(username);
			objectOutput.flush();
		}
		catch(Throwable t)
		{
			throw new RuntimeException(t);
		}
		comm.sendPacket(byteOutput.toByteArray());
	}

	public static void sendHungerChange(IPacketSender comm, float hunger)
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeByte(SmartMovingInfo.HungerChangePacketId);
			objectOutput.writeFloat(hunger);
			objectOutput.flush();
		}
		catch(Throwable t)
		{
			throw new RuntimeException(t);
		}
		comm.sendPacket(byteOutput.toByteArray());
	}

	public static void sendSound(IPacketSender comm, String soundId, float volume, float pitch)
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeByte(SmartMovingInfo.SoundPacketId);
			objectOutput.writeObject(soundId);
			objectOutput.writeFloat(volume);
			objectOutput.writeFloat(pitch);
			objectOutput.flush();
		}
		catch(Throwable t)
		{
			throw new RuntimeException(t);
		}
		comm.sendPacket(byteOutput.toByteArray());
	}
}
```

---

## 역할

SmartMoving 네트워크 프로토콜의 **직렬화/역직렬화 및 디스패치 레이어**.

- 7종 패킷 직렬화 (`sendXxx`) — Java `ObjectOutputStream` 사용
- 수신 패킷 역직렬화 + packetId 기반 `IPacketReceiver` 디스패치 (`receivePacket`)
- 채널 ID(`Id`) 관리

모든 멤버 static. 인스턴스 생성 없음.

---

## import

```java
import java.io.*;    // ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream
import java.util.*;  // Set, HashSet, StackTraceElement
import cpw.mods.fml.common.network.internal.*;  // FMLProxyPacket
```

---

## static 필드 및 초기화

```java
public static final String Id;
public static final Set<StackTraceElement> errors;

static
{
    String id = SmartMovingInfo.ModComId;
    if(id.length() > 15)
        id = id.substring(0, 15);
    Id = id;
    errors = new HashSet<StackTraceElement>();
}
```

**`Id`**: `SmartMovingInfo.ModComId`를 최대 15자로 자름.  
FML 네트워크 채널 이름 길이 제한(15자)을 맞추기 위함.  
`SmartMovingInfo.ModComId = ModName.replace(" ","") + " " + ModComVersion` → 예: `"SmartMoving 2.3.1"` (17자) → `"SmartMoving 2.3."` (15자).

**`errors`**: `HashSet<StackTraceElement>` — 에러 중복 출력 방지용 집합.  
`receivePacket` catch에서 동일 스택 위치의 에러는 첫 번째만 전체 스택 출력, 이후 간단 출력.

---

## 패킷 포맷

모든 패킷은 Java `ObjectOutputStream`/`ObjectInputStream` 기반 직렬화. 첫 1바이트가 packetId.

| packetId | 상수 | 페이로드 (packetId 이후) |
|----------|------|------------------------|
| `0` | `StatePacketId` | `int(entityId)` + `long(state)` |
| `1` | `ConfigInfoPacketId` | `Object(String info)` |
| `2` | `ConfigContentPacketId` | `Object(String[] content)` + `Object(String username)` |
| `3` | `ConfigChangePacketId` | (없음) |
| `4` | `SpeedChangePacketId` | `int(difference)` + `Object(String username)` |
| `5` | `HungerChangePacketId` | `float(hunger)` |
| `6` | `SoundPacketId` | `Object(String soundId)` + `float(volume)` + `float(pitch)` |

`writeByte`/`writeInt`/`writeLong`/`writeFloat`는 Java 원시 직렬화.  
`writeObject`는 Java 객체 직렬화 (String, String[] → Serializable).

---

## `receivePacket(FMLProxyPacket packet, IPacketReceiver comm, IEntityPlayerMP player)`

```java
public static void receivePacket(FMLProxyPacket packet, IPacketReceiver comm, IEntityPlayerMP player)
{
    try
    {
        ByteArrayInputStream byteInput = new ByteArrayInputStream(packet.payload().array());
        ObjectInputStream objectInput = new ObjectInputStream(byteInput);
        byte packetId = objectInput.readByte();
        switch(packetId)
        {
            case SmartMovingInfo.StatePacketId:
                int entityId = objectInput.readInt();
                long state = objectInput.readLong();
                comm.processStatePacket(packet, player, entityId, state);
                break;
            case SmartMovingInfo.ConfigInfoPacketId:
                String info = (String)objectInput.readObject();
                comm.processConfigInfoPacket(packet, player, info);
                break;
            case SmartMovingInfo.ConfigContentPacketId:
                String[] content = (String[])objectInput.readObject();
                String username = (String)objectInput.readObject();
                comm.processConfigContentPacket(packet, player, content, username);
                break;
            case SmartMovingInfo.ConfigChangePacketId:
                comm.processConfigChangePacket(packet, player);
                break;
            case SmartMovingInfo.SpeedChangePacketId:
                int difference = objectInput.readInt();
                username = (String)objectInput.readObject();
                comm.processSpeedChangePacket(packet, player, difference, username);
                break;
            case SmartMovingInfo.HungerChangePacketId:
                float hunger = objectInput.readFloat();
                comm.processHungerChangePacket(packet, player, hunger);
                break;
            case SmartMovingInfo.SoundPacketId:
                String soundId = (String)objectInput.readObject();
                float volume = objectInput.readFloat();
                float pitch = objectInput.readFloat();
                comm.processSoundPacket(packet, player, soundId, volume, pitch);
                break;
            default:
                throw new RuntimeException("Unknown packet id '" + packetId + "' found");
        }
    }
    catch(Throwable t)
    {
        if(errors.add(t.getStackTrace()[0]))
            t.printStackTrace();
        else
            System.err.println(t.getClass().getName() + ": " + t.getMessage());
    }
}
```

1. `packet.payload().array()` → ByteArrayInputStream → ObjectInputStream
2. `readByte()` → packetId
3. switch → 각 패킷 타입별 역직렬화 + `comm.processXxx()` 호출
4. `default` → `RuntimeException("Unknown packet id '...' found")`

**에러 처리:**
```java
catch(Throwable t)
{
    if(errors.add(t.getStackTrace()[0]))
        t.printStackTrace();
    else
        System.err.println(t.getClass().getName() + ": " + t.getMessage());
}
```
- `t.getStackTrace()[0]` = 에러 발생 첫 번째 스택 원소
- `errors.add()` → 처음 발생한 위치면 `true` → 전체 스택 출력
- 이미 있는 위치면 `false` → `클래스명: 메시지`만 출력

---

## sendXxx 메서드 (전부 static)

모든 send 메서드의 공통 구조:
```java
ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
try {
    ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
    objectOutput.writeByte(<packetId>);
    objectOutput.writeXxx(<data>);
    objectOutput.flush();
} catch(Throwable t) {
    throw new RuntimeException(t);
}
comm.sendPacket(byteOutput.toByteArray());
```

에러 발생 시 `RuntimeException`으로 감싸 재throw.

### `sendState(IPacketSender comm, int entityId, long state)`
```
writeByte(0) + writeInt(entityId) + writeLong(state)
```

### `sendConfigInfo(IPacketSender comm, String info)`
```
writeByte(1) + writeObject(info)
```

### `sendConfigContent(IPacketSender comm, String[] content, String username)`
```
writeByte(2) + writeObject(content) + writeObject(username)
```

### `sendConfigChange(IPacketSender comm)`
```
writeByte(3)
```
페이로드 없음 — packetId만.

### `sendSpeedChange(IPacketSender comm, int difference, String username)`
```
writeByte(4) + writeInt(difference) + writeObject(username)
```

### `sendHungerChange(IPacketSender comm, float hunger)`
```
writeByte(5) + writeFloat(hunger)
```

### `sendSound(IPacketSender comm, String soundId, float volume, float pitch)`
```
writeByte(6) + writeObject(soundId) + writeFloat(volume) + writeFloat(pitch)
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingInfo` | `ModComId`, 7개 packetId 상수 |
| `IPacketReceiver` | 수신 시 `processXxx()` 호출 대상 |
| `IPacketSender` | 송신 시 `sendPacket()` 호출 대상 |
| `FMLProxyPacket` | `packet.payload().array()` — 수신 페이로드 |
| `SmartMovingServerComm` | `IPacketReceiver` 구현체 (서버 수신) |
| `SmartMovingComm` | `IPacketReceiver` + `IPacketSender` 구현체 (클라이언트) |

---

## 주요 관찰 사항

1. **Java `ObjectOutputStream` 직렬화**: 커스텀 바이너리 프로토콜이 아닌 Java 표준 객체 직렬화 사용. `String`, `String[]` 모두 `writeObject`. 버전 호환성 문제 가능.

2. **`Id` 15자 제한**: FML 채널 이름 최대 15자 제한에 맞춰 자름. `SmartMovingInfo.ModComId`가 15자를 초과하면 잘림.

3. **에러 중복 방지 패턴**: `HashSet<StackTraceElement> errors` — 동일 스택 위치의 반복 에러를 한 번만 전체 출력. 패킷 파싱 실패가 매 틱 반복되는 경우 로그 폭발 방지.

4. **`receivePacket` catch(Throwable)**: `Exception`이 아닌 `Throwable` — `Error`도 잡음. 패킷 처리 실패가 서버/클라이언트를 다운시키지 않도록.

5. **send 메서드 catch(Throwable) → RuntimeException**: 송신 실패는 unchecked로 재throw. 호출자가 명시적 처리 없이 크래시.

6. **`ObjectOutputStream` 헤더**: Java 직렬화는 스트림 시작에 4바이트 매직 + 버전 헤더를 씀. 실제 데이터 앞에 이 오버헤드 포함.

7. **1.21.1 이식**: `FMLProxyPacket` → Fabric `PacketByteBuf`. `ObjectOutputStream`/`ObjectInputStream` → Fabric `PacketByteBufs`의 직접 바이너리 인코딩으로 교체 권장 (Java 직렬화 보안/성능 문제). `IPacketReceiver`/`IPacketSender` 인터페이스는 그대로 유지 가능.
