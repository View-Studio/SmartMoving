# IPacketSender.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/IPacketSender.java  
패키지: `net.smart.moving`  
종류: `interface`  
상속: 없음 (직접 `interface`)

---

## 전체 소스

```java
package net.smart.moving;

public interface IPacketSender
{
	void sendPacket(byte[] byteArray);
}
```

---

## 역할

`SmartMovingPacketStream`의 모든 `sendXxx()` 메서드가 마지막에 호출하는 **패킷 송신 추상화 인터페이스**. 직렬화된 바이트 배열을 실제 네트워크로 전송하는 역할을 구현체에 위임한다.

---

## import

없음.

---

## 메서드

```java
void sendPacket(byte[] byteArray);
```

직렬화 완료된 패킷 바이트 배열을 받아 전송. 반환값 없음(`void`).

호출처: `SmartMovingPacketStream`의 모든 `sendXxx()` 메서드 마지막 줄:
```java
comm.sendPacket(byteOutput.toByteArray());
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| 없음 | import 없음 |

구현체:
- `SmartMovingComm implements IPacketReceiver, IPacketSender` — 클라이언트 측 송신: `C17PacketCustomPayload(SmartMovingPacketStream.Id, data)` 생성 후 전송 (SmartMovingComm 리서치에서 확인)
- `IEntityPlayerMP extends IPacketSender` — 서버 측 송신: PlayerBase에서 클라이언트로 패킷 전송

---

## 주요 관찰 사항

1. **단일 메서드 인터페이스**: `sendPacket(byte[])` 하나만 존재. 직렬화(`ObjectOutputStream`)와 송신(`sendPacket`)을 완전히 분리하는 설계 — `SmartMovingPacketStream`이 직렬화 담당, 구현체가 실제 전송 담당.

2. **`IEntityPlayerMP extends IPacketSender`**: 서버 측 플레이어가 직접 `IPacketSender`를 구현 — 서버 플레이어 인스턴스를 `comm`으로 넘겨 `SmartMovingPacketStream.sendXxx(player, ...)`를 호출하면 해당 플레이어 클라이언트로 패킷이 전송되는 구조.

3. **1.21.1 이식**: `byte[]` 기반 인터페이스를 그대로 유지하거나, Fabric `PacketByteBuf`/`CustomPayload`를 직접 다루는 방식으로 교체. 인터페이스 자체는 단순하므로 변경 최소화 가능.
