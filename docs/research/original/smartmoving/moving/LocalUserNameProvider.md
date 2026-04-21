# LocalUserNameProvider.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/LocalUserNameProvider.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: `SmartMovingContext` (extends), `ILocalUserNameProvider` (implements)

---

## 전체 소스

```java
package net.smart.moving;

import net.minecraft.client.*;

public class LocalUserNameProvider extends SmartMovingContext implements ILocalUserNameProvider
{
	@Override
	public String getLocalConfigUserName()
	{
		return Options._localUserHasChangeConfigRight.value ? Minecraft.getMinecraft().thePlayer.getGameProfile().getName() : null;
	}

	@Override
	public String getLocalSpeedUserName()
	{
		return Options._localUserHasChangeSpeedRight.value ? Minecraft.getMinecraft().thePlayer.getGameProfile().getName() : null;
	}
}
```

---

## 역할

`ILocalUserNameProvider`의 구현체. 로컬 플레이어가 설정 변경 권한 또는 속도 변경 권한을 가지고 있을 때만 플레이어 이름을 반환하고, 권한이 없으면 `null`을 반환한다.

---

## import

```java
import net.minecraft.client.*;  // Minecraft
```

---

## 메서드

### `getLocalConfigUserName()`

```java
@Override
public String getLocalConfigUserName()
{
    return Options._localUserHasChangeConfigRight.value
        ? Minecraft.getMinecraft().thePlayer.getGameProfile().getName()
        : null;
}
```

**조건**: `Options._localUserHasChangeConfigRight.value == true`  
**true일 때**: `Minecraft.getMinecraft().thePlayer.getGameProfile().getName()` — 로컬 플레이어 이름 반환  
**false일 때**: `null` 반환

`null` 반환의 의미: `SmartMovingPacketStream.sendConfigContent(comm, content, username)`에서 `username`이 `null`이면 해당 플레이어는 설정 변경 권한이 없음을 서버에 알림.

---

### `getLocalSpeedUserName()`

```java
@Override
public String getLocalSpeedUserName()
{
    return Options._localUserHasChangeSpeedRight.value
        ? Minecraft.getMinecraft().thePlayer.getGameProfile().getName()
        : null;
}
```

**조건**: `Options._localUserHasChangeSpeedRight.value == true`  
**true일 때**: 로컬 플레이어 이름 반환  
**false일 때**: `null` 반환

구조는 `getLocalConfigUserName()`과 동일. 참조하는 Options 필드만 다름.

---

## 참조하는 Options 필드

| 필드 | 의미 |
|------|------|
| `Options._localUserHasChangeConfigRight` | 로컬 사용자의 설정 변경 권한 여부 |
| `Options._localUserHasChangeSpeedRight` | 로컬 사용자의 속도 변경 권한 여부 |

두 필드 모두 `SmartMovingOptions` 클래스의 필드로, `.value` 접근자로 boolean 값을 읽는다 (SmartMoving Properties 시스템 — `Property<Boolean>` 타입으로 추정, `SmartMovingOptions` 리서치에서 확인 필요).

`Options`는 `SmartMovingContext.Options` — 클라이언트 설정 싱글톤.

---

## 플레이어 이름 조회 경로

```
Minecraft.getMinecraft()         // Minecraft 싱글톤
  .thePlayer                     // EntityClientPlayerMP (로컬 플레이어)
  .getGameProfile()              // com.mojang.authlib.GameProfile
  .getName()                     // String (플레이어 이름)
```

`GameProfile.getName()`은 Mojang 인증 기반 플레이어 이름. 오프라인 모드에서도 설정된 이름 반환.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingContext` | 상속 — `Options` 싱글톤 접근 |
| `ILocalUserNameProvider` | 구현 인터페이스 |
| `Minecraft` | `thePlayer.getGameProfile().getName()` |
| `Options._localUserHasChangeConfigRight` | 설정 변경 권한 확인 |
| `Options._localUserHasChangeSpeedRight` | 속도 변경 권한 확인 |

호출처: `SmartMovingMod.init()`에서 `localUserNameProvider` 인스턴스로 생성 및 등록 (SmartMovingMod 리서치에서 확인).  
사용처: `SmartMovingPacketStream.sendConfigContent()`, `sendSpeedChange()`의 `username` 파라미터.

---

## 주요 관찰 사항

1. **권한 기반 null 반환**: 권한 없을 때 이름 대신 `null` 반환 — 서버에서 `username == null`이면 해당 플레이어의 설정/속도 변경 요청을 무시하는 패턴. `SmartMovingServerComm.processConfigContentPacket` 또는 `processSpeedChangePacket`에서 `null` 검사 여부는 이후 리서치에서 확인 필요.

2. **`SmartMovingContext` 상속**: `Options` 싱글톤에 접근하기 위해 상속. 실제로 사용하는 것은 `Options` 하나뿐이며, 생성자/필드 추가 없음.

3. **클라이언트 전용**: `Minecraft.getMinecraft().thePlayer` 접근 — 서버에서 호출 불가. `SmartMovingServerComm.localUserNameProvider`에 등록되지만 실제 `getLocalXxxUserName()` 호출은 클라이언트 컨텍스트에서만 발생.

4. **1.21.1 이식**:
   - `Minecraft.getMinecraft().thePlayer` → `MinecraftClient.getInstance().player`
   - `getGameProfile().getName()` → 동일 (`GameProfile.getName()`)
   - `Options._localUserHasChangeConfigRight.value` → 포팅된 Options 시스템에서 동일 패턴 유지
