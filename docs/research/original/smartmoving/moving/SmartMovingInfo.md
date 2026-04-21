# SmartMovingInfo.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMovingInfo.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: 없음 (`Object` 직접 상속)

---

## 전체 소스

```java
package net.smart.moving;

import cpw.mods.fml.common.*;

public class SmartMovingInfo
{
	public static final byte StatePacketId = 0;
	public static final byte ConfigInfoPacketId = 1;
	public static final byte ConfigContentPacketId = 2;
	public static final byte ConfigChangePacketId = 3;
	public static final byte SpeedChangePacketId = 4;
	public static final byte HungerChangePacketId = 5;
	public static final byte SoundPacketId = 6;

	private static final Mod Mod = SmartMovingMod.class.getAnnotation(Mod.class);

	public static final String ModId = Mod.modid();
	public static final String ModName = Mod.name();
	public static final String ModVersion = Mod.version();
	public static final String ModComVersion = SmartMovingMod.ModComVersion;

	public static final String ModComMessage = ModName + " uses communication protocol " + ModComVersion;
	public static final String ModComId = ModName.replace(" ", "") + " " + ModComVersion;

	public static final int DefaultChatId = 0;
	public static final int ConfigChatId = ModName.hashCode();
	public static final int SpeedChatId = ModName.hashCode() + 1;
}
```

---

## 역할

SmartMoving 전체에서 사용하는 **전역 상수 모음 클래스**.

- 패킷 타입 ID (byte, 7개)
- 모드 메타데이터 (ModId, ModName, ModVersion, ModComVersion)
- 조합된 문자열 상수 (ModComMessage, ModComId)
- 채팅 오버레이 ID (DefaultChatId, ConfigChatId, SpeedChatId)

인스턴스 생성 없이 모두 `public static final`. 메서드 없음.

---

## import

```java
import cpw.mods.fml.common.*;  // Mod (FML @Mod 어노테이션 타입)
```

---

## 패킷 타입 ID (public static final byte)

SmartMoving 통신 프로토콜의 패킷 종류를 구분하는 1바이트 ID:

| 상수 | 값 | 용도 |
|------|----|------|
| `StatePacketId` | `0` | 이동 상태 패킷 |
| `ConfigInfoPacketId` | `1` | 설정 정보(버전) 패킷 |
| `ConfigContentPacketId` | `2` | 설정 내용 패킷 |
| `ConfigChangePacketId` | `3` | 설정 변경 요청 패킷 |
| `SpeedChangePacketId` | `4` | 속도 변경 패킷 |
| `HungerChangePacketId` | `5` | 배고픔 패킷 |
| `SoundPacketId` | `6` | 사운드 패킷 |

`SmartMovingPacketStream`에서 패킷 직렬화/역직렬화 시 사용됨.

---

## 모드 메타데이터

### `private static final Mod Mod`

```java
private static final Mod Mod = SmartMovingMod.class.getAnnotation(Mod.class);
```

`SmartMovingMod` 클래스에 붙은 FML `@Mod` 어노테이션을 리플렉션으로 가져옴.  
필드 이름 `Mod`와 타입 이름 `Mod`가 동일 — FML의 `cpw.mods.fml.common.Mod` 어노테이션 타입.  
`private` — 내부 참조 전용.

---

### 모드 정보 (public static final String)

```java
public static final String ModId = Mod.modid();
public static final String ModName = Mod.name();
public static final String ModVersion = Mod.version();
public static final String ModComVersion = SmartMovingMod.ModComVersion;
```

| 상수 | 출처 | 값 |
|------|------|----|
| `ModId` | `SmartMovingMod` `@Mod.modid()` | 모드 ID 문자열 |
| `ModName` | `SmartMovingMod` `@Mod.name()` | 모드 이름 문자열 |
| `ModVersion` | `SmartMovingMod` `@Mod.version()` | 모드 버전 문자열 |
| `ModComVersion` | `SmartMovingMod.ModComVersion` (static 필드 직접 참조) | 통신 프로토콜 버전 |

실제 값은 `SmartMovingMod`에서 결정됨 — 이 파일에서 확인 불가.

---

### 조합 상수 (public static final String)

```java
public static final String ModComMessage = ModName + " uses communication protocol " + ModComVersion;
public static final String ModComId = ModName.replace(" ", "") + " " + ModComVersion;
```

**`ModComMessage`**: 서버/클라이언트 로그 출력 메시지.  
형식: `"<ModName> uses communication protocol <ModComVersion>"`  
사용처: `SmartMovingContext.initialize()`, `SmartMovingServer.initialize()`에서 FMLLog/stdout 출력.

**`ModComId`**: 네트워크 채널 등록 ID.  
형식: `"<ModName(공백제거)> <ModComVersion>"` (예: `"SmartMoving 1.0"`)  
`ModName.replace(" ", "")` — 공백 제거 후 버전 붙임.  
사용처: `SmartMovingPacketStream.Id`의 값 소스 (SmartMovingPacketStream 리서치 시 확인 필요).

---

## 채팅 오버레이 ID (public static final int)

```java
public static final int DefaultChatId = 0;
public static final int ConfigChatId = ModName.hashCode();
public static final int SpeedChatId = ModName.hashCode() + 1;
```

| 상수 | 값 | 용도 |
|------|----|------|
| `DefaultChatId` | `0` | 기본 채팅 ID |
| `ConfigChatId` | `ModName.hashCode()` | 설정 관련 채팅 메시지 ID |
| `SpeedChatId` | `ModName.hashCode() + 1` | 속도 관련 채팅 메시지 ID |

vanilla MC 채팅 시스템에서 동일 ID의 메시지는 이전 메시지를 교체함.  
`ConfigChatId`와 `SpeedChatId`는 각각 설정/속도 변경 채팅 메시지가 중복 누적되지 않도록 ID를 지정.  
`ModName.hashCode()`: Java String hashCode — 실행마다 동일 값 보장됨.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingMod` | `@Mod` 어노테이션 소스 + `ModComVersion` static 필드 |
| `cpw.mods.fml.common.Mod` | `@Mod` 어노테이션 타입 (리플렉션 대상) |

---

## 주요 관찰 사항

1. **`private static final Mod Mod` 필드**: 타입과 이름이 모두 `Mod`. FML `@Mod` 어노테이션을 리플렉션으로 읽어 모드 메타데이터를 중앙화. 리플렉션 실패(어노테이션 없음)이면 NPE.

2. **`ModComVersion`은 `SmartMovingMod.ModComVersion`**: `@Mod.version()`이 아닌 별도 static 필드에서 읽음 — 통신 프로토콜 버전과 모드 버전을 분리.

3. **`ConfigChatId = ModName.hashCode()`**: 런타임 계산이지만 `static final` — 클래스 초기화 시 1회 계산 후 고정.

4. **1.21.1 이식**: FML `@Mod` → Fabric `@Mod` 어노테이션으로 교체. 패킷 ID byte 상수는 그대로 유지 가능. `ModComId`는 Fabric 네트워크 채널 식별자(`Identifier`)로 변환 필요. 채팅 ID 시스템은 Fabric에도 존재하므로 그대로 사용 가능.
