# ILocalUserNameProvider.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/ILocalUserNameProvider.java  
패키지: `net.smart.moving`  
종류: `interface`  
상속: 없음 (직접 `interface`)

---

## 전체 소스

```java
package net.smart.moving;

public interface ILocalUserNameProvider
{
	String getLocalConfigUserName();

	String getLocalSpeedUserName();
}
```

---

## 역할

**로컬 플레이어 이름**을 두 가지 목적별로 제공하는 인터페이스.

- `getLocalConfigUserName()`: 설정(config) 관련 패킷에서 사용하는 로컬 유저명
- `getLocalSpeedUserName()`: 속도 변경(speed change) 패킷에서 사용하는 로컬 유저명

SmartMoving의 config/speed 관련 패킷(`SmartMovingPacketStream.sendConfigContent`, `sendSpeedChange`)에서 `username` 파라미터로 전달되는 값을 공급하는 역할을 한다.

---

## import

없음.

---

## 메서드 목록

```java
String getLocalConfigUserName();
```
설정 패킷(`ConfigContent`)에 포함할 로컬 유저명 반환.

```java
String getLocalSpeedUserName();
```
속도 변경 패킷(`SpeedChange`)에 포함할 로컬 유저명 반환.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| 없음 | import 없음 |

참조처:
- `SmartMovingServerComm.localUserNameProvider` 필드 (타입: `ILocalUserNameProvider`, 초기값 `null`) — SmartMovingServerComm 리서치에서 확인.
- `SmartMovingMod.init()`에서 `localUserNameProvider` 인스턴스를 생성해 등록 — SmartMovingMod 리서치에서 확인.

구현체: `LocalUserNameProvider` (`net.smart.moving` 패키지 — 이후 리서치 대상).

---

## 주요 관찰 사항

1. **두 메서드 분리 이유**: config 유저명과 speed 유저명을 별도 메서드로 분리. 구현체(`LocalUserNameProvider`)에서 두 값이 다를 수 있음을 설계상 허용. 실제로 다른지는 `LocalUserNameProvider` 리서치에서 확인 필요.

2. **Macro Mod 연동 용도**: `SmartMovingInstall.MacroModCore`가 존재하고, `SmartMovingMod.checkForPresentModsAndInitializeOptions()`에서 Macro Mod 감지 시 처리가 있음. `ILocalUserNameProvider`는 Macro Mod나 다른 유저명 소스에서 이름을 가져오는 추상화일 가능성이 있으나, 이 파일만으로는 확인 불가.

3. **1.21.1 이식**: 인터페이스 자체는 그대로 유지 가능. 구현체에서 1.21.1 플레이어 이름 API(`MinecraftClient.getInstance().getSession().getUsername()` 등)로 교체 필요.
