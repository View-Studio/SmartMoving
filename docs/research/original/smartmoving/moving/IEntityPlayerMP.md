# IEntityPlayerMP.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/IEntityPlayerMP.java  
패키지: `net.smart.moving`  
종류: `interface`  
상속: `IPacketSender` (확장)

---

## 전체 소스

```java
package net.smart.moving;

import java.util.*;

import cpw.mods.fml.common.network.internal.FMLProxyPacket;

import net.minecraft.entity.*;
import net.minecraft.util.*;

public interface IEntityPlayerMP extends IPacketSender
{
	void sendPacketToTrackedPlayers(FMLProxyPacket packet);

	String getUsername();

	void resetFallDistance();

	void resetTicksForFloatKick();

	void setHeight(float height);

	double getMinY();

	float getHeight();

	void setMaxY(double maxY);

	boolean localIsEntityInsideOpaqueBlock();

	SmartMovingServer getMoving();

	IEntityPlayerMP[] getAllPlayers();

	float doGetHealth();

	AxisAlignedBB getBox();

	AxisAlignedBB expandBox(AxisAlignedBB box, double x, double y, double z);

	List<?> getEntitiesExcludingPlayer(AxisAlignedBB box);

	boolean isDeadEntity(Entity entity);

	void onCollideWithPlayer(Entity entity);

	void localAddExhaustion(float exhaustion);

	void localAddMovementStat(double x, double y, double z);

	void localPlaySound(String soundId, float volume, float pitch);

	boolean localIsSneaking();
}
```

---

## 역할

**서버 측 플레이어 엔티티**(`EntityPlayerMP`)에 대해 SmartMoving이 필요로 하는 기능을 추상화한 인터페이스. `IPacketSender`를 확장하므로 패킷 송신 기능도 포함한다.

`SmartMovingServer`가 이 인터페이스를 통해 플레이어 엔티티를 조작한다. 실제 구현체는 PlayerAPI의 `SmartMovingServerPlayerBase`에 있는 것으로 추정되지만, 이 파일에서는 인터페이스 선언만 확인 가능.

---

## import

```java
import java.util.*;                                          // List
import cpw.mods.fml.common.network.internal.FMLProxyPacket; // 패킷 타입
import net.minecraft.entity.*;                               // Entity
import net.minecraft.util.*;                                 // AxisAlignedBB
```

---

## 상속

```java
public interface IEntityPlayerMP extends IPacketSender
```

`IPacketSender`를 확장 → `IPacketSender.sendPacket(byte[])` 메서드도 구현 필수.

---

## 메서드 목록

### 패킷 관련

```java
void sendPacketToTrackedPlayers(FMLProxyPacket packet);
```
이 플레이어를 추적 중인 다른 플레이어들에게 패킷을 브로드캐스트.

---

### 플레이어 정보

```java
String getUsername();
```
플레이어 이름 반환.

```java
float doGetHealth();
```
플레이어 현재 체력 반환. `do` 접두사는 vanilla `getHealth()`를 직접 호출하는 래퍼임을 나타냄 (SmartMoving의 `do` 접두사 컨벤션).

```java
boolean localIsSneaking();
```
플레이어의 실제 스니킹 상태 반환. `SmartMovingServer.isSneaking()`에서 `forceIsSneaking`이 null일 때 이 메서드를 호출함 (SmartMovingServer 리서치에서 확인).

---

### 이동/물리 조작

```java
void resetFallDistance();
```
낙하 거리 초기화. `SmartMovingServer`에서 클라이밍/크롤클라이밍/천장클라이밍/벽점프 시 호출.

```java
void resetTicksForFloatKick();
```
부유 kick 틱 카운터 초기화. `SmartMovingInstall.NetServerHandler_ticksForFloatKick` 필드를 리셋. 클라이밍 3종 시 호출.

```java
void setHeight(float height);
```
플레이어 높이(`height`) 설정. `SmartMovingServer.setSmall()`에서 0.8F 또는 1.8F로 설정.

```java
double getMinY();
```
플레이어 AABB 최소 Y 좌표 반환.

```java
float getHeight();
```
플레이어 현재 높이 반환.

```java
void setMaxY(double maxY);
```
플레이어 AABB 최대 Y 좌표 설정.

---

### 충돌 박스 / 엔티티 감지

```java
AxisAlignedBB getBox();
```
플레이어의 현재 충돌 박스(AABB) 반환.

```java
AxisAlignedBB expandBox(AxisAlignedBB box, double x, double y, double z);
```
주어진 AABB를 x/y/z 방향으로 확장한 새 AABB 반환.

```java
List<?> getEntitiesExcludingPlayer(AxisAlignedBB box);
```
주어진 AABB 안에 있는 엔티티 목록 반환 (플레이어 자신 제외).

```java
boolean isDeadEntity(Entity entity);
```
해당 엔티티가 죽은 상태인지 확인.

```java
void onCollideWithPlayer(Entity entity);
```
엔티티와 플레이어가 충돌했을 때 처리. `SmartMovingServer.afterOnLivingUpdate()`에서 isSmall 상태의 아이템 획득 범위 확장 처리 시 사용.

---

### 행동 억제/우회

```java
boolean localIsEntityInsideOpaqueBlock();
```
플레이어가 불투명 블록 안에 있는지 vanilla 로직으로 직접 확인. `local` 접두사는 SmartMoving의 오버라이드 없이 vanilla 원본 호출.

```java
SmartMovingServer getMoving();
```
이 플레이어에 연결된 `SmartMovingServer` 인스턴스 반환. `SmartMovingServerComm`에서 수신 패킷을 `player.getMoving().processXxx()`로 위임할 때 사용.

```java
IEntityPlayerMP[] getAllPlayers();
```
현재 서버에 접속한 모든 플레이어 배열 반환.

---

### 소진/통계/사운드

```java
void localAddExhaustion(float exhaustion);
```
소진값 직접 추가. SmartMoving 소진 인터셉트(`disableAddExhaustionDepth`) 우회 없이 vanilla 소진 직접 적용.

```java
void localAddMovementStat(double x, double y, double z);
```
이동 통계에 x/y/z 거리 직접 추가.

```java
void localPlaySound(String soundId, float volume, float pitch);
```
플레이어 위치에서 사운드 재생. `local` 접두사 → vanilla 사운드 시스템 직접 호출.

---

## 메서드 분류 요약

| 분류 | 메서드 |
|------|--------|
| 패킷 | `sendPacketToTrackedPlayers` |
| 정보 조회 | `getUsername`, `doGetHealth`, `localIsSneaking`, `getHeight`, `getMinY`, `getBox` |
| 상태 조작 | `setHeight`, `setMaxY`, `resetFallDistance`, `resetTicksForFloatKick` |
| 엔티티/충돌 | `getEntitiesExcludingPlayer`, `expandBox`, `isDeadEntity`, `onCollideWithPlayer`, `localIsEntityInsideOpaqueBlock` |
| 서버 유틸 | `getMoving`, `getAllPlayers` |
| 소진/통계/사운드 | `localAddExhaustion`, `localAddMovementStat`, `localPlaySound` |

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `IPacketSender` | 상위 인터페이스 — `sendPacket(byte[])` 포함 |
| `FMLProxyPacket` | `sendPacketToTrackedPlayers` 파라미터 |
| `SmartMovingServer` | `getMoving()` 반환 타입 |
| `IEntityPlayerMP` | `getAllPlayers()` 반환 타입 (자기 참조) |
| `Entity` | `isDeadEntity`, `onCollideWithPlayer` 파라미터 |
| `AxisAlignedBB` | `getBox`, `expandBox`, `getEntitiesExcludingPlayer` |

구현체: `SmartMovingServerPlayerBase` (PlayerAPI, `net.smart.moving.playerapi` 패키지 — 이후 리서치 대상).

---

## 주요 관찰 사항

1. **`local` 접두사 컨벤션**: `localIsEntityInsideOpaqueBlock`, `localAddExhaustion`, `localAddMovementStat`, `localPlaySound`, `localIsSneaking` — 모두 SmartMoving의 오버라이드/인터셉트를 거치지 않고 vanilla 원본을 직접 호출하는 메서드. SmartMoving이 특정 동작을 오버라이드한 뒤 원본을 선택적으로 호출할 수 있도록 분리.

2. **`do` 접두사**: `doGetHealth()` — PlayerAPI 패턴에서 `do` 접두사는 PlayerBase 체인을 거치지 않고 vanilla 원본 메서드를 직접 호출함을 나타냄.

3. **`IPacketSender` 확장**: 서버 플레이어는 `sendPacket(byte[])` (클라이언트로 패킷 송신)과 `sendPacketToTrackedPlayers(FMLProxyPacket)` (주변 플레이어들에게 브로드캐스트) 두 가지 송신 경로를 가짐.

4. **`getAllPlayers()`**: 서버의 전체 플레이어 목록을 단일 플레이어 인스턴스로부터 조회하는 패턴. `MinecraftServer.getServer().getConfigurationManager().playerEntityList` 등을 래핑하는 것으로 보임.

5. **1.21.1 이식**:
   - `FMLProxyPacket` → Fabric `CustomPayload` / `PacketByteBuf`
   - `AxisAlignedBB` → 1.21.1 `Box`
   - `IPacketSender` 상속 관계 유지하되 Fabric 패킷 API로 교체
   - `resetTicksForFloatKick` → 1.21.1 서버에서 해당 kick 메커니즘 재확인 필요 (vanilla 리서치 대상)
   - `localAddMovementStat` → 1.21.1 `StatHandler` 계열로 교체
