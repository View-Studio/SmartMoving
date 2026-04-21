# SmartMovingCoreEventHandler.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMovingCoreEventHandler.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: `SmartMovingCoreEventHandler extends SmartCoreEventHandler`

---

## 전체 소스

```java
package net.smart.moving;

import net.minecraft.client.entity.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.network.*;
import net.minecraft.network.play.client.*;
import net.minecraft.server.management.*;

import net.smart.core.*;
import net.smart.moving.playerapi.*;

public class SmartMovingCoreEventHandler extends SmartCoreEventHandler
{
	
	@Override
	public void beforeProcessPlayer(NetHandlerPlayServer netServerHandler)
	{
		SmartMovingServerPlayerBase playerBase = SmartMovingServerPlayerBase.getPlayerBase(netServerHandler.playerEntity);
		playerBase.moving.beforeAddMovingHungerBatch();
	}

	@Override
	public void afterProcessPlayer(NetHandlerPlayServer netServerHandler)
	{
		SmartMovingServerPlayerBase playerBase = SmartMovingServerPlayerBase.getPlayerBase(netServerHandler.playerEntity);
		playerBase.moving.afterAddMovingHungerBatch();
	}

	@Override
	public void beforeProcessPlayerBlockPlacement(NetHandlerPlayServer netServerHandler, C08PacketPlayerBlockPlacement packet15place)
	{
		if (packet15place.func_149568_f() == 255)
		{
			ItemStack itemstack = netServerHandler.playerEntity.inventory.getCurrentItem();
			if (itemstack != null)
			{
				float offset = 1.62F - netServerHandler.playerEntity.getEyeHeight();
				netServerHandler.playerEntity.yOffset += offset;
			}
		}
	}

	@Override
	public void afterProcessPlayerBlockPlacement(NetHandlerPlayServer netServerHandler, C08PacketPlayerBlockPlacement packet15place)
	{
		if (packet15place.func_149568_f() == 255)
		{
			ItemStack itemstack = netServerHandler.playerEntity.inventory.getCurrentItem();
			if (itemstack != null)
			{
				float offset = 1.62F - netServerHandler.playerEntity.getEyeHeight();
				netServerHandler.playerEntity.yOffset -= offset;
			}
		}
	}

	@Override
	public void beforeOnPlayerRightClick(PlayerControllerMP playerControllerMP, EntityPlayer entityPlayerSP)
	{
		SmartMovingPlayerBase playerBase = SmartMovingPlayerBase.getPlayerBase((EntityPlayerSP)entityPlayerSP);
		playerBase.moving.beforeActivateBlockOrUseItem();
	}

	@Override
	public void afterOnPlayerRightClick(PlayerControllerMP playerControllerMP, EntityPlayer entityPlayerSP)
	{
		SmartMovingPlayerBase playerBase = SmartMovingPlayerBase.getPlayerBase((EntityPlayerSP)entityPlayerSP);
		playerBase.moving.afterActivateBlockOrUseItem();
	}

	@Override
	public void beforeActivateBlockOrUseItem(ItemInWorldManager itemInWorldManager, EntityPlayer entityPlayer)
	{
		SmartMovingServerPlayerBase playerBase = SmartMovingServerPlayerBase.getPlayerBase(entityPlayer);
		playerBase.moving.beforeActivateBlockOrUseItem();
	}

	@Override
	public void afterActivateBlockOrUseItem(ItemInWorldManager itemInWorldManager, EntityPlayer entityPlayer)
	{
		SmartMovingServerPlayerBase playerBase = SmartMovingServerPlayerBase.getPlayerBase(entityPlayer);
		playerBase.moving.afterActivateBlockOrUseItem();
	}
}
```

---

## 역할

`SmartCoreTransformer`가 주입한 ASM 훅의 **실제 실행 대상**.  
vanilla 클래스의 지정 메서드 앞/뒤에 삽입된 `INVOKESTATIC` 호출이 이 클래스의 메서드를 실행.  
`SmartCoreEventHandler`를 상속하며, SmartMoving 전용 before/after 훅 8개를 구현한다.

필드 없음. 모든 메서드 `@Override`.

---

## SmartCoreTransformer와의 대응

`SmartCoreTransformer`에서 확인한 4개 변환 대상과 이 파일의 메서드가 1:1 대응:

| 변환 대상 vanilla 메서드 | before 훅 | after 훅 |
|--------------------------|-----------|----------|
| `NetHandlerPlayServer.processPlayer` | `beforeProcessPlayer` | `afterProcessPlayer` |
| `NetHandlerPlayServer.processPlayerBlockPlacement` | `beforeProcessPlayerBlockPlacement` | `afterProcessPlayerBlockPlacement` |
| `PlayerControllerMP.onPlayerRightClick` | `beforeOnPlayerRightClick` | `afterOnPlayerRightClick` |
| `ItemInWorldManager.activateBlockOrUseItem` | `beforeActivateBlockOrUseItem` | `afterActivateBlockOrUseItem` |

---

## import

```java
import net.minecraft.client.entity.*;          // EntityPlayerSP
import net.minecraft.client.multiplayer.*;     // PlayerControllerMP
import net.minecraft.entity.player.*;          // EntityPlayer
import net.minecraft.item.*;                   // ItemStack
import net.minecraft.network.*;                // NetHandlerPlayServer
import net.minecraft.network.play.client.*;    // C08PacketPlayerBlockPlacement
import net.minecraft.server.management.*;      // ItemInWorldManager
import net.smart.core.*;                       // SmartCoreEventHandler
import net.smart.moving.playerapi.*;           // SmartMovingPlayerBase, SmartMovingServerPlayerBase
```

---

## 메서드

### `beforeProcessPlayer(NetHandlerPlayServer netServerHandler)`

```java
@Override
public void beforeProcessPlayer(NetHandlerPlayServer netServerHandler)
{
    SmartMovingServerPlayerBase playerBase = SmartMovingServerPlayerBase.getPlayerBase(netServerHandler.playerEntity);
    playerBase.moving.beforeAddMovingHungerBatch();
}
```

**트리거**: 서버에서 `NetHandlerPlayServer.processPlayer()` 진입 직전.  
`processPlayer`: 클라이언트 이동 패킷(`C03PacketPlayer`) 처리 메서드 — 서버 위치 갱신, `addMovementStat()` 호출 포함.

**동작**: `SmartMovingServer.beforeAddMovingHungerBatch()` 호출 → `disableAddExhaustionDepth++`, `hunger != -1`이면 `disableAddExhaustion = true`.

**목적**: `processPlayer` 내부의 `addMovementStat()` → `addExhaustion()` 경로를 억제하기 위해 before에서 비활성화 플래그 설정.

---

### `afterProcessPlayer(NetHandlerPlayServer netServerHandler)`

```java
@Override
public void afterProcessPlayer(NetHandlerPlayServer netServerHandler)
{
    SmartMovingServerPlayerBase playerBase = SmartMovingServerPlayerBase.getPlayerBase(netServerHandler.playerEntity);
    playerBase.moving.afterAddMovingHungerBatch();
}
```

**트리거**: `NetHandlerPlayServer.processPlayer()` 반환 직후.  
**동작**: `SmartMovingServer.afterAddMovingHungerBatch()` 호출 → `disableAddExhaustionDepth--`, depth=0이면 `disableAddExhaustion = false`.

`beforeProcessPlayer` / `afterProcessPlayer` 쌍이 `processPlayer` 전체를 감싸 소진 억제 구간을 형성.

---

### `beforeProcessPlayerBlockPlacement(NetHandlerPlayServer netServerHandler, C08PacketPlayerBlockPlacement packet15place)`

```java
@Override
public void beforeProcessPlayerBlockPlacement(NetHandlerPlayServer netServerHandler, C08PacketPlayerBlockPlacement packet15place)
{
    if (packet15place.func_149568_f() == 255)
    {
        ItemStack itemstack = netServerHandler.playerEntity.inventory.getCurrentItem();
        if (itemstack != null)
        {
            float offset = 1.62F - netServerHandler.playerEntity.getEyeHeight();
            netServerHandler.playerEntity.yOffset += offset;
        }
    }
}
```

**트리거**: `NetHandlerPlayServer.processPlayerBlockPlacement()` 진입 직전.  
`processPlayerBlockPlacement`: 블록 우클릭/아이템 사용 패킷(`C08PacketPlayerBlockPlacement`) 처리.

**조건 `packet15place.func_149568_f() == 255`:**  
`func_149568_f()`는 블록 face 값 반환. `255`는 공기(블록 없음)에 아이템 우클릭 시의 face 값.  
즉, 아이템 단독 사용(블록 상호작용 아님) 케이스에만 적용.

**조건 `itemstack != null`:** 손에 아이템이 있을 때.

**동작:**
```java
float offset = 1.62F - netServerHandler.playerEntity.getEyeHeight();
netServerHandler.playerEntity.yOffset += offset;
```
- `1.62F`: vanilla 일반 플레이어 eyeHeight
- `getEyeHeight()`: 현재 플레이어 eyeHeight (크롤링/isSmall 시 더 작음)
- `offset = 1.62F - eyeHeight`: 양수이면 눈 위치가 낮아진 만큼 보정값 발생
- `yOffset += offset`: 플레이어 Y 오프셋을 임시로 증가시켜 서버 측 아이템 사용 위치를 표준 높이로 보정

**목적**: 크롤링/isSmall 상태에서 아이템 사용 시 서버가 플레이어 위치를 잘못 계산하는 것을 방지.

---

### `afterProcessPlayerBlockPlacement(NetHandlerPlayServer netServerHandler, C08PacketPlayerBlockPlacement packet15place)`

```java
@Override
public void afterProcessPlayerBlockPlacement(NetHandlerPlayServer netServerHandler, C08PacketPlayerBlockPlacement packet15place)
{
    if (packet15place.func_149568_f() == 255)
    {
        ItemStack itemstack = netServerHandler.playerEntity.inventory.getCurrentItem();
        if (itemstack != null)
        {
            float offset = 1.62F - netServerHandler.playerEntity.getEyeHeight();
            netServerHandler.playerEntity.yOffset -= offset;
        }
    }
}
```

`beforeProcessPlayerBlockPlacement`에서 더한 `offset`을 복원.  
`yOffset -= offset` → 원래 yOffset으로 복귀.

---

### `beforeOnPlayerRightClick(PlayerControllerMP playerControllerMP, EntityPlayer entityPlayerSP)`

```java
@Override
public void beforeOnPlayerRightClick(PlayerControllerMP playerControllerMP, EntityPlayer entityPlayerSP)
{
    SmartMovingPlayerBase playerBase = SmartMovingPlayerBase.getPlayerBase((EntityPlayerSP)entityPlayerSP);
    playerBase.moving.beforeActivateBlockOrUseItem();
}
```

**트리거**: `PlayerControllerMP.onPlayerRightClick()` 진입 직전 (클라이언트 측).  
**동작**: `SmartMovingSelf.beforeActivateBlockOrUseItem()` 호출.

`SmartMovingSelf.beforeActivateBlockOrUseItem()`은 `SmartMovingServer`의 동명 메서드와 대칭 — 클라이언트 측 sneaking 오버라이드 처리.  
`entityPlayerSP`를 `(EntityPlayerSP)`로 캐스팅.

---

### `afterOnPlayerRightClick(PlayerControllerMP playerControllerMP, EntityPlayer entityPlayerSP)`

```java
@Override
public void afterOnPlayerRightClick(PlayerControllerMP playerControllerMP, EntityPlayer entityPlayerSP)
{
    SmartMovingPlayerBase playerBase = SmartMovingPlayerBase.getPlayerBase((EntityPlayerSP)entityPlayerSP);
    playerBase.moving.afterActivateBlockOrUseItem();
}
```

`SmartMovingSelf.afterActivateBlockOrUseItem()` 호출. 클라이언트 측 sneaking 오버라이드 해제.

---

### `beforeActivateBlockOrUseItem(ItemInWorldManager itemInWorldManager, EntityPlayer entityPlayer)`

```java
@Override
public void beforeActivateBlockOrUseItem(ItemInWorldManager itemInWorldManager, EntityPlayer entityPlayer)
{
    SmartMovingServerPlayerBase playerBase = SmartMovingServerPlayerBase.getPlayerBase(entityPlayer);
    playerBase.moving.beforeActivateBlockOrUseItem();
}
```

**트리거**: 서버 `ItemInWorldManager.activateBlockOrUseItem()` 진입 직전.  
**동작**: `SmartMovingServer.beforeActivateBlockOrUseItem()` → `forceIsSneaking = isSneakButtonPressed` 설정.

---

### `afterActivateBlockOrUseItem(ItemInWorldManager itemInWorldManager, EntityPlayer entityPlayer)`

```java
@Override
public void afterActivateBlockOrUseItem(ItemInWorldManager itemInWorldManager, EntityPlayer entityPlayer)
{
    SmartMovingServerPlayerBase playerBase = SmartMovingServerPlayerBase.getPlayerBase(entityPlayer);
    playerBase.moving.afterActivateBlockOrUseItem();
}
```

`SmartMovingServer.afterActivateBlockOrUseItem()` → `forceIsSneaking = null` 해제.

---

## before/after 쌍 요약

| 쌍 | 실행 측 | 목적 |
|----|---------|------|
| `beforeProcessPlayer` / `afterProcessPlayer` | 서버 | `processPlayer` 구간 동안 소진 억제 (`beforeAddMovingHungerBatch` / `afterAddMovingHungerBatch`) |
| `beforeProcessPlayerBlockPlacement` / `afterProcessPlayerBlockPlacement` | 서버 | 아이템 사용 시 임시 `yOffset` 보정 (`1.62F - eyeHeight`) |
| `beforeOnPlayerRightClick` / `afterOnPlayerRightClick` | 클라이언트 | 우클릭 구간 동안 sneaking 오버라이드 |
| `beforeActivateBlockOrUseItem` / `afterActivateBlockOrUseItem` | 서버 | 블록 활성화 구간 동안 sneaking 오버라이드 |

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartCoreEventHandler` | 상속 (before/after 훅 인터페이스) |
| `SmartMovingServerPlayerBase` | 서버 측 playerBase 조회 |
| `SmartMovingPlayerBase` | 클라이언트 측 playerBase 조회 |
| `SmartMovingServer` | `beforeAddMovingHungerBatch`, `afterAddMovingHungerBatch`, `beforeActivateBlockOrUseItem`, `afterActivateBlockOrUseItem` |
| `SmartMovingSelf` | `beforeActivateBlockOrUseItem`, `afterActivateBlockOrUseItem` (클라이언트) |
| `NetHandlerPlayServer` | `playerEntity` 필드 |
| `C08PacketPlayerBlockPlacement` | `func_149568_f()` (face 값) |
| `PlayerControllerMP` | 파라미터 타입 |
| `ItemInWorldManager` | 파라미터 타입 |
| `EntityPlayer` / `EntityPlayerSP` | 파라미터 타입 및 캐스팅 |

---

## 주요 관찰 사항

1. **`func_149568_f() == 255`**: 1.7.10 난독화 메서드명. 블록 face 값. `255` = 공기에 아이템 우클릭 (face -1이 아닌 255로 인코딩됨). 블록 직접 우클릭(face 0~5)이 아닌 아이템 단독 사용 케이스만 처리.

2. **`1.62F` 하드코딩**: vanilla 일반 플레이어 eyeHeight. 크롤링/isSmall 상태에서 eyeHeight가 낮아지면 `yOffset` 보정으로 서버 위치 계산 오류를 방지.

3. **클라이언트 ↔ 서버 대칭**: `onPlayerRightClick`(클라이언트) + `activateBlockOrUseItem`(서버) — 양쪽에서 모두 sneaking 오버라이드 처리. 서버-클라이언트 sneaking 상태 동기화.

4. **`SmartCoreEventHandler` 상속**: `SmartCoreEventHandler`는 SmartCore에 정의된 기반 클래스. before/after 메서드 시그니처 정의는 그쪽에 있음 (SmartCore 리서치 시 확인).

5. **1.21.1 이식**: ASM 훅 → Mixin `@Inject`로 대체. `NetHandlerPlayServer` → `ServerCommonNetworkHandler`, `PlayerControllerMP` → `ClientPlayerInteractionManager`, `ItemInWorldManager` → `ServerPlayerInteractionManager`. `func_149568_f()` → Yarn 매핑 기준 메서드명으로 교체 필요.
