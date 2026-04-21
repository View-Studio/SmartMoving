# IPacketReceiver.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/IPacketReceiver.java  
패키지: `net.smart.moving`  
종류: `interface`  
상속: 없음 (직접 `interface`)

---

## 전체 소스

```java
package net.smart.moving;

import cpw.mods.fml.common.network.internal.*;

public interface IPacketReceiver
{
	boolean processStatePacket(FMLProxyPacket packet, IEntityPlayerMP player, int entityId, long state);

	boolean processConfigInfoPacket(FMLProxyPacket packet, IEntityPlayerMP player, String info);

	boolean processConfigContentPacket(FMLProxyPacket packet, IEntityPlayerMP player, String[] content, String username);

	boolean processConfigChangePacket(FMLProxyPacket packet, IEntityPlayerMP player);

	boolean processSpeedChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, int difference, String username);

	boolean processHungerChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, float hunger);

	boolean processSoundPacket(FMLProxyPacket packet, IEntityPlayerMP player, String soundId, float distance, float pitch);
}
```

---

## 역할

`SmartMovingPacketStream.receivePacket()`이 역직렬화한 패킷을 **처리 대상에게 디스패치하기 위한 인터페이스**. packetId별로 7개 메서드가 1:1 대응한다.

구현체:
- `SmartMovingServerComm` — 서버 측 수신 처리
- `SmartMovingComm` — 클라이언트 측 수신 처리

---

## import

```java
import cpw.mods.fml.common.network.internal.*;  // FMLProxyPacket
```

---

## 메서드 목록

모든 메서드가 `boolean` 반환. `FMLProxyPacket packet`과 `IEntityPlayerMP player`를 공통 파라미터로 가진다.

### `processStatePacket`

```java
boolean processStatePacket(FMLProxyPacket packet, IEntityPlayerMP player, int entityId, long state);
```

- `entityId`: 상태를 보낸 엔티티 ID (클라이언트→서버: 로컬 플레이어, 서버→클라이언트: 다른 플레이어)
- `state`: 33비트 long 직렬화 상태값 (`SmartMovingPacketStream`의 packetId 0)

### `processConfigInfoPacket`

```java
boolean processConfigInfoPacket(FMLProxyPacket packet, IEntityPlayerMP player, String info);
```

- `info`: 설정 정보 문자열 (packetId 1)

### `processConfigContentPacket`

```java
boolean processConfigContentPacket(FMLProxyPacket packet, IEntityPlayerMP player, String[] content, String username);
```

- `content`: 설정 내용 문자열 배열 (빈 배열이면 서버 설정 해제 신호)
- `username`: 설정을 보낸 플레이어 이름 (packetId 2)

### `processConfigChangePacket`

```java
boolean processConfigChangePacket(FMLProxyPacket packet, IEntityPlayerMP player);
```

- 페이로드 없음. 설정 변경 알림만 (packetId 3)

### `processSpeedChangePacket`

```java
boolean processSpeedChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, int difference, String username);
```

- `difference`: 속도 변경 차이값
- `username`: 속도 변경을 요청한 플레이어 이름 (packetId 4)

### `processHungerChangePacket`

```java
boolean processHungerChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, float hunger);
```

- `hunger`: 소진값 (`-1`이면 처리 억제 신호 — SmartMovingServer 리서치에서 확인) (packetId 5)

### `processSoundPacket`

```java
boolean processSoundPacket(FMLProxyPacket packet, IEntityPlayerMP player, String soundId, float distance, float pitch);
```

- `soundId`: 재생할 사운드 ID
- `distance`: 세 번째 파라미터 이름이 `distance`이나 `SmartMovingPacketStream`에서는 `volume`으로 직렬화됨 — 파라미터 이름 불일치 주의
- `pitch`: 음높이 (packetId 6)

---

## packetId ↔ 메서드 대응표

| packetId | 상수 | 메서드 |
|----------|------|--------|
| 0 | `StatePacketId` | `processStatePacket` |
| 1 | `ConfigInfoPacketId` | `processConfigInfoPacket` |
| 2 | `ConfigContentPacketId` | `processConfigContentPacket` |
| 3 | `ConfigChangePacketId` | `processConfigChangePacket` |
| 4 | `SpeedChangePacketId` | `processSpeedChangePacket` |
| 5 | `HungerChangePacketId` | `processHungerChangePacket` |
| 6 | `SoundPacketId` | `processSoundPacket` |

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `FMLProxyPacket` | 모든 메서드 첫 번째 파라미터 — 원본 패킷 객체 |
| `IEntityPlayerMP` | 모든 메서드 두 번째 파라미터 — 패킷을 보낸/받는 플레이어 |

호출처: `SmartMovingPacketStream.receivePacket()` — switch문에서 packetId에 따라 `comm.processXxx()` 호출.

구현체:
- `SmartMovingServerComm implements IPacketReceiver` — 서버
- `SmartMovingComm implements IPacketReceiver, IPacketSender` — 클라이언트

---

## 주요 관찰 사항

1. **`boolean` 반환값**: 모든 메서드가 `boolean`을 반환하지만, `SmartMovingPacketStream.receivePacket()`에서 반환값을 사용하지 않음 (결과 무시). `SmartMovingServerComm`에서 `processConfigContentPacket`은 `false` 반환, 나머지는 위임 후 암묵적 반환. 반환값의 실제 의미는 불명확.

2. **`processSoundPacket`의 파라미터 이름 불일치**: 인터페이스에서는 `float distance`이지만, `SmartMovingPacketStream`에서는 `float volume`으로 직렬화/역직렬화. 파라미터 이름만 다르고 타입과 순서는 동일하므로 동작에는 영향 없음.

3. **`player` 파라미터의 역할**: 서버 수신(`SmartMovingServerComm`)에서는 `player.getMoving().processXxx()`로 위임할 때 사용. 클라이언트 수신(`SmartMovingComm`)에서는 패킷 발신자 식별용. 일부 메서드에서는 `player`를 직접 사용하지 않고 `entityId`만 사용.

4. **1.21.1 이식**: `FMLProxyPacket` → Fabric `CustomPayload` 또는 처리된 데이터만 파라미터로 전달하는 방식으로 리팩토링 가능. `IEntityPlayerMP` → Fabric 서버 플레이어 타입으로 교체. 7개 메서드 구조는 그대로 유지 가능.
