# SmartMovingFactory.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMovingFactory.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: `SmartMovingFactory extends SmartMovingContext`

---

## 전체 소스

```java
package net.smart.moving;

import java.util.*;

import net.minecraft.client.*;
import net.minecraft.client.entity.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;

public class SmartMovingFactory extends SmartMovingContext
{
	private static SmartMovingFactory factory;

	private Hashtable<Integer, SmartMovingOther> otherSmartMovings;

	public SmartMovingFactory()
	{
		if(factory != null)
			throw new RuntimeException("FATAL: Can only create one instance of type 'SmartMovingFactory'");
		factory = this;
	}

	protected static boolean isInitialized()
	{
		return factory != null;
	}

	public static void initialize()
	{
		if(!isInitialized())
			new SmartMovingFactory();
	}

	public static void handleMultiPlayerTick(Minecraft minecraft)
	{
		factory.doHandleMultiPlayerTick(minecraft);
	}

	public static SmartMoving getInstance(EntityPlayer entityPlayer)
	{
		return factory.doGetInstance(entityPlayer);
	}

	public static SmartMoving getOtherSmartMoving(int entityId)
	{
		return factory.doGetOtherSmartMoving(entityId);
	}

	public static SmartMovingOther getOtherSmartMoving(EntityOtherPlayerMP entity)
	{
		return factory.doGetOtherSmartMoving(entity);
	}

	protected void doHandleMultiPlayerTick(Minecraft minecraft)
	{
		Iterator<?> others = minecraft.theWorld.playerEntities.iterator();
		while(others.hasNext())
		{
			Entity player = (Entity)others.next();
			if(player instanceof EntityOtherPlayerMP)
			{
				EntityOtherPlayerMP otherPlayer = (EntityOtherPlayerMP)player;
				SmartMovingOther moving = doGetOtherSmartMoving(otherPlayer);
				moving.spawnParticles(minecraft, otherPlayer.posX - otherPlayer.prevPosX, otherPlayer.posZ - otherPlayer.prevPosZ);
				moving.foundAlive = true;
			}
		}

		if(otherSmartMovings == null || otherSmartMovings.isEmpty())
			return;

		Iterator<Integer> entityIds = otherSmartMovings.keySet().iterator();
		while(entityIds.hasNext())
		{
			Integer entityId = entityIds.next();
			SmartMovingOther moving = otherSmartMovings.get(entityId);
			if(moving.foundAlive)
				moving.foundAlive = false;
			else
				entityIds.remove();
		}
	}

	protected SmartMoving doGetInstance(EntityPlayer entityPlayer)
	{
		if(entityPlayer instanceof EntityOtherPlayerMP)
			return doGetOtherSmartMoving(entityPlayer.getEntityId());
		else if(entityPlayer instanceof IEntityPlayerSP)
			return ((IEntityPlayerSP)entityPlayer).getMoving();
		return null;
	}

	protected SmartMoving doGetOtherSmartMoving(int entityId)
	{
		SmartMoving moving = tryGetOtherSmartMoving(entityId);
		if(moving == null)
		{
			Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(entityId);
			if(entity != null && entity instanceof EntityOtherPlayerMP)
				moving = addOtherSmartMoving((EntityOtherPlayerMP)entity);
		}
		return moving;
	}

	protected SmartMovingOther doGetOtherSmartMoving(EntityOtherPlayerMP entity)
	{
		SmartMovingOther moving = tryGetOtherSmartMoving(entity.getEntityId());
		if(moving == null)
			moving = addOtherSmartMoving(entity);
		return moving;
	}

	protected final SmartMovingOther tryGetOtherSmartMoving(int entityId)
	{
		if(otherSmartMovings == null)
			otherSmartMovings = new Hashtable<Integer, SmartMovingOther>();
		return otherSmartMovings.get(entityId);
	}

	protected final SmartMovingOther addOtherSmartMoving(EntityOtherPlayerMP entity)
	{
		SmartMovingOther moving = new SmartMovingOther(entity);
		otherSmartMovings.put(entity.getEntityId(), moving);
		return moving;
	}
}
```

---

## 역할

**다른 플레이어(`EntityOtherPlayerMP`)의 SmartMovingOther 인스턴스 관리 팩토리**.

- 싱글톤 패턴: `private static SmartMovingFactory factory`
- `entityId → SmartMovingOther` 매핑을 `Hashtable`로 유지
- 매 클라이언트 틱(`handleMultiPlayerTick`)에서 다른 플레이어 파티클 처리 + 사라진 플레이어 정리
- `EntityPlayer → SmartMoving` 변환 (`getInstance`) — 로컬/원격 플레이어 분기

---

## import

```java
import java.util.*;                     // Hashtable, Iterator
import net.minecraft.client.*;          // Minecraft
import net.minecraft.client.entity.*;   // EntityOtherPlayerMP
import net.minecraft.entity.*;          // Entity
import net.minecraft.entity.player.*;   // EntityPlayer
```

---

## 필드

```java
private static SmartMovingFactory factory;
```

싱글톤 인스턴스. 클래스 로드 시 `null`, 생성자 호출 시 설정됨.

```java
private Hashtable<Integer, SmartMovingOther> otherSmartMovings;
```

`entityId(Integer) → SmartMovingOther` 맵. 초기값 `null` — `tryGetOtherSmartMoving` 최초 호출 시 lazy 초기화됨.  
`Hashtable`: 동기화된 컬렉션 (HashMap이 아닌 Hashtable 사용).

---

## 생성자

```java
public SmartMovingFactory()
{
    if(factory != null)
        throw new RuntimeException("FATAL: Can only create one instance of type 'SmartMovingFactory'");
    factory = this;
}
```

두 번째 인스턴스 생성 시 `RuntimeException("FATAL: Can only create one instance of type 'SmartMovingFactory'")` 던짐.  
정상 경로: `factory = this` 설정.

---

## 정적 메서드

### `isInitialized()` → boolean (protected static)

```java
protected static boolean isInitialized()
{
    return factory != null;
}
```

싱글톤 초기화 여부.

---

### `initialize()` (public static)

```java
public static void initialize()
{
    if(!isInitialized())
        new SmartMovingFactory();
}
```

초기화되지 않은 경우에만 인스턴스 생성. `new SmartMovingFactory()`가 `factory = this`를 실행.

---

### `handleMultiPlayerTick(Minecraft minecraft)` (public static)

```java
public static void handleMultiPlayerTick(Minecraft minecraft)
{
    factory.doHandleMultiPlayerTick(minecraft);
}
```

`SmartMovingContext.onTickInGame()`에서 매 클라이언트 틱 호출됨.  
`factory.doHandleMultiPlayerTick(minecraft)`로 위임.

---

### `getInstance(EntityPlayer entityPlayer)` → SmartMoving (public static)

```java
public static SmartMoving getInstance(EntityPlayer entityPlayer)
{
    return factory.doGetInstance(entityPlayer);
}
```

`EntityPlayer`로부터 해당 SmartMoving 인스턴스를 반환.

---

### `getOtherSmartMoving(int entityId)` → SmartMoving (public static)

```java
public static SmartMoving getOtherSmartMoving(int entityId)
{
    return factory.doGetOtherSmartMoving(entityId);
}
```

entityId로 SmartMoving 반환 (반환 타입 `SmartMoving` — 부모 타입).

---

### `getOtherSmartMoving(EntityOtherPlayerMP entity)` → SmartMovingOther (public static)

```java
public static SmartMovingOther getOtherSmartMoving(EntityOtherPlayerMP entity)
{
    return factory.doGetOtherSmartMoving(entity);
}
```

entity로 SmartMovingOther 반환 (반환 타입 `SmartMovingOther` — 구체 타입).

---

## 인스턴스 메서드

### `doHandleMultiPlayerTick(Minecraft minecraft)` (protected)

```java
protected void doHandleMultiPlayerTick(Minecraft minecraft)
{
    Iterator<?> others = minecraft.theWorld.playerEntities.iterator();
    while(others.hasNext())
    {
        Entity player = (Entity)others.next();
        if(player instanceof EntityOtherPlayerMP)
        {
            EntityOtherPlayerMP otherPlayer = (EntityOtherPlayerMP)player;
            SmartMovingOther moving = doGetOtherSmartMoving(otherPlayer);
            moving.spawnParticles(minecraft, otherPlayer.posX - otherPlayer.prevPosX, otherPlayer.posZ - otherPlayer.prevPosZ);
            moving.foundAlive = true;
        }
    }

    if(otherSmartMovings == null || otherSmartMovings.isEmpty())
        return;

    Iterator<Integer> entityIds = otherSmartMovings.keySet().iterator();
    while(entityIds.hasNext())
    {
        Integer entityId = entityIds.next();
        SmartMovingOther moving = otherSmartMovings.get(entityId);
        if(moving.foundAlive)
            moving.foundAlive = false;
        else
            entityIds.remove();
    }
}
```

**1단계 — 현재 살아있는 다른 플레이어 처리:**

`minecraft.theWorld.playerEntities` 전체 순회 → `EntityOtherPlayerMP`인 경우:
1. `doGetOtherSmartMoving(otherPlayer)` — 캐시에서 조회 또는 lazy 생성
2. `moving.spawnParticles(minecraft, posX - prevPosX, posZ - prevPosZ)`
   - 속도 근사치: `(posX - prevPosX, posZ - prevPosZ)` = 이번 틱 이동 벡터
3. `moving.foundAlive = true` — 이번 틱에 살아있었음을 표시

**2단계 — 사라진 플레이어 정리:**

`otherSmartMovings` 맵을 Iterator로 순회:
- `foundAlive == true` → `foundAlive = false` (다음 틱 초기화)
- `foundAlive == false` → `entityIds.remove()` (맵에서 제거 — 이번 틱에 못 찾은 플레이어)

Iterator 도중 `entityIds.remove()` 사용 — `ConcurrentModificationException` 없이 안전하게 제거.

---

### `doGetInstance(EntityPlayer entityPlayer)` → SmartMoving (protected)

```java
protected SmartMoving doGetInstance(EntityPlayer entityPlayer)
{
    if(entityPlayer instanceof EntityOtherPlayerMP)
        return doGetOtherSmartMoving(entityPlayer.getEntityId());
    else if(entityPlayer instanceof IEntityPlayerSP)
        return ((IEntityPlayerSP)entityPlayer).getMoving();
    return null;
}
```

| 타입 | 동작 |
|------|------|
| `EntityOtherPlayerMP` | `doGetOtherSmartMoving(entityId)` → SmartMovingOther |
| `IEntityPlayerSP` | `((IEntityPlayerSP)entityPlayer).getMoving()` → SmartMovingSelf |
| 그 외 | `null` |

---

### `doGetOtherSmartMoving(int entityId)` → SmartMoving (protected)

```java
protected SmartMoving doGetOtherSmartMoving(int entityId)
{
    SmartMoving moving = tryGetOtherSmartMoving(entityId);
    if(moving == null)
    {
        Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(entityId);
        if(entity != null && entity instanceof EntityOtherPlayerMP)
            moving = addOtherSmartMoving((EntityOtherPlayerMP)entity);
    }
    return moving;
}
```

1. `tryGetOtherSmartMoving(entityId)` — 캐시 조회
2. 없으면: `theWorld.getEntityByID(entityId)` → `EntityOtherPlayerMP`이면 `addOtherSmartMoving()`
3. 엔티티가 없거나 `EntityOtherPlayerMP`가 아니면 `null` 반환 가능

반환 타입: `SmartMoving` (SmartMovingOther의 부모 타입으로 업캐스트).

---

### `doGetOtherSmartMoving(EntityOtherPlayerMP entity)` → SmartMovingOther (protected)

```java
protected SmartMovingOther doGetOtherSmartMoving(EntityOtherPlayerMP entity)
{
    SmartMovingOther moving = tryGetOtherSmartMoving(entity.getEntityId());
    if(moving == null)
        moving = addOtherSmartMoving(entity);
    return moving;
}
```

1. `tryGetOtherSmartMoving(entity.getEntityId())` — 캐시 조회
2. 없으면: `addOtherSmartMoving(entity)` — 항상 생성 (world 조회 불필요, entity 이미 있음)

반환 타입: `SmartMovingOther` (구체 타입).

---

### `tryGetOtherSmartMoving(int entityId)` → SmartMovingOther (protected final)

```java
protected final SmartMovingOther tryGetOtherSmartMoving(int entityId)
{
    if(otherSmartMovings == null)
        otherSmartMovings = new Hashtable<Integer, SmartMovingOther>();
    return otherSmartMovings.get(entityId);
}
```

`otherSmartMovings == null`이면 `new Hashtable<>()` lazy 초기화 후 `get(entityId)`.  
없으면 `null` 반환.

---

### `addOtherSmartMoving(EntityOtherPlayerMP entity)` → SmartMovingOther (protected final)

```java
protected final SmartMovingOther addOtherSmartMoving(EntityOtherPlayerMP entity)
{
    SmartMovingOther moving = new SmartMovingOther(entity);
    otherSmartMovings.put(entity.getEntityId(), moving);
    return moving;
}
```

`new SmartMovingOther(entity)` 생성 → `otherSmartMovings.put(entityId, moving)` → 반환.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingContext` | 상속 |
| `SmartMovingOther` | 다른 플레이어 SmartMoving 구현체 생성 및 관리 |
| `IEntityPlayerSP` | 로컬 플레이어 인터페이스 (`getMoving()`) |
| `Minecraft` | `theWorld`, `getMinecraft()` |
| `EntityOtherPlayerMP` | 다른 플레이어 엔티티 타입 |
| `EntityPlayer` | `doGetInstance` 파라미터 타입 |
| `Entity` | `getEntityByID` 반환 타입 |

---

## 주요 관찰 사항

1. **싱글톤 강제**: 생성자에서 `factory != null`이면 `RuntimeException` ("FATAL" 메시지). 두 번째 인스턴스 생성 자체를 금지.

2. **`Hashtable` 선택**: `HashMap` 대신 `Hashtable` 사용 — 동기화됨. 멀티스레드 환경(렌더 스레드와 게임 스레드)을 고려한 선택으로 보임.

3. **`foundAlive` 패턴**: 매 틱에 `foundAlive = true` 표시 → 다음 단계에서 false인 것 제거. 단순 mark-and-sweep 방식으로 사라진 플레이어 정리.

4. **`doGetOtherSmartMoving` 반환 타입 차이**: `int` 파라미터 버전 → `SmartMoving` (부모), `EntityOtherPlayerMP` 파라미터 버전 → `SmartMovingOther` (구체). 전자는 엔티티 조회 실패 시 null 반환 가능.

5. **파티클 속도 근사치**: `posX - prevPosX`, `posZ - prevPosZ` — 1틱 변위. `SmartMoving.spawnParticles`의 `playerMotionX`, `playerMotionZ` 파라미터로 전달.

6. **`doHandleMultiPlayerTick` 로컬 플레이어 제외**: `instanceof EntityOtherPlayerMP` 체크로 로컬 플레이어(`EntityPlayerSP`)는 건너뜀. 로컬 플레이어 파티클은 `SmartMovingSelf`가 직접 처리.

7. **`protected` 메서드 설계**: `doXxx` 메서드들이 `protected` — 서브클래스에서 오버라이드 가능한 구조. 1.21.1 이식 시 서브클래스 확장 없이 동일 구조 유지 가능.

8. **1.21.1 이식**: `EntityOtherPlayerMP` → `OtherClientPlayerEntity`, `minecraft.theWorld.playerEntities` → `world.getPlayers()`, `Hashtable` → `HashMap` 또는 `ConcurrentHashMap`. `IEntityPlayerSP.getMoving()` 인터페이스 패턴 유지.
