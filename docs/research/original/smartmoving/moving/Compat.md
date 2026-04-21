# Compat.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/Compat.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: 없음 (`Object` 직접 상속)

---

## 전체 소스

```java
package net.smart.moving;

import cpw.mods.fml.common.Loader;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipLocator;
import ganymedes01.etfuturum.api.elytra.IElytraPlayer;
import jp.mc.ancientred.starminer.api.Gravity;
import jp.mc.ancientred.starminer.api.GravityDirection;
import jp.mc.ancientred.starminer.core.entity.ExtendedPropertyGravity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class Compat {

	private static boolean isStarMinerPresent;
	private static boolean isShipsModPresent;
	private static boolean isEtFuturumRequiemPresent;
	private static boolean isEtFuturumRequiemElytraPresent;
	
	public static void init()
	{
		isStarMinerPresent = Loader.isModLoaded("modJ_StarMiner");
		isShipsModPresent = Loader.isModLoaded("cuchaz.ships");
		isEtFuturumRequiemPresent = Loader.isModLoaded("etfuturum");
		isEtFuturumRequiemElytraPresent = isEtFuturumRequiemPresent && classExists("ganymedes01.etfuturum.api.elytra.IElytraPlayer");
	}
	
	private static boolean classExists(String className)
	{
		return Compat.class.getClassLoader().getResourceAsStream(className.replaceAll("\\.", "/") + ".class") != null;
	}
	
	public static boolean isBlockedByIncompatibility(EntityPlayer sp)
	{
		return isStarMinerGravitized(sp) || isOnCuchazShip(sp) || isElytraFlying(sp) || isSpectator(sp);
	}
	
	public static boolean isStarMinerGravitized(Entity player)
	{
		return isStarMinerPresent && StarMinerCompat.isGravitized(player);
	}
	
	public static boolean isOnCuchazShip(Entity player)
	{
		return isShipsModPresent && ShipsCompat.isOnShip(player);
	}
	
	public static boolean isElytraFlying(Entity player)
	{
		return isEtFuturumRequiemElytraPresent && EtFuturumRequiemElytraCompat.isElytraFlying(player);
	}
	
	public static boolean isSpectator(Entity player)
	{
		return isEtFuturumRequiemPresent && Minecraft.getMinecraft().playerController.currentGameType.getID() == 3;
	}
	
	private static class StarMinerCompat
	{
		public static boolean isGravitized(Entity player)
		{
			Gravity gravity = ExtendedPropertyGravity.getExtendedPropertyGravity(player);
			return gravity.gravityDirection != GravityDirection.upTOdown_YN || gravity.isZeroGravity();
		}
	}
	
	private static class ShipsCompat
	{
		public static boolean isOnShip(Entity player)
		{
			for (EntityShip ship : ShipLocator.getFromEntityLocation(player))
			{
				if (ship.getCollider().isEntityAboard(player))
				{
					return true;
				}
			}
			return false;
		}	
	}
	
	private static class EtFuturumRequiemElytraCompat
	{
		public static boolean isElytraFlying(Entity player)
		{
			return player instanceof IElytraPlayer && ((IElytraPlayer)player).etfu$isElytraFlying();
		}
	}

}
```

---

## 역할

SmartMoving과 **비호환 상태**를 감지하는 클래스. 특정 조건(다른 모드의 기능이 활성화된 상태)에서 SmartMoving 동작을 막아야 할 때 `isBlockedByIncompatibility()`로 판별한다.

모든 멤버 static. 인스턴스 생성 없음.

---

## import

```java
import cpw.mods.fml.common.Loader;                            // 모드 로드 여부 확인
import cuchaz.ships.EntityShip;                               // Ships 모드 배 엔티티
import cuchaz.ships.ShipLocator;                              // Ships 모드 배 위치 조회
import ganymedes01.etfuturum.api.elytra.IElytraPlayer;        // Et Futurum Requiem 엘리트라 인터페이스
import jp.mc.ancientred.starminer.api.Gravity;                // StarMiner 중력 정보
import jp.mc.ancientred.starminer.api.GravityDirection;       // StarMiner 중력 방향 enum
import jp.mc.ancientred.starminer.core.entity.ExtendedPropertyGravity; // StarMiner 중력 ExtendedProperty
import net.minecraft.client.Minecraft;                        // playerController.currentGameType
import net.minecraft.entity.Entity;                           // 플레이어 엔티티 공통 타입
import net.minecraft.entity.player.EntityPlayer;              // isBlockedByIncompatibility 파라미터
```

---

## static 필드

```java
private static boolean isStarMinerPresent;
private static boolean isShipsModPresent;
private static boolean isEtFuturumRequiemPresent;
private static boolean isEtFuturumRequiemElytraPresent;
```

4개 모두 `private static boolean`. `init()` 호출 전까지 `false`.

---

## `init()`

```java
public static void init()
{
    isStarMinerPresent = Loader.isModLoaded("modJ_StarMiner");
    isShipsModPresent = Loader.isModLoaded("cuchaz.ships");
    isEtFuturumRequiemPresent = Loader.isModLoaded("etfuturum");
    isEtFuturumRequiemElytraPresent = isEtFuturumRequiemPresent && classExists("ganymedes01.etfuturum.api.elytra.IElytraPlayer");
}
```

`SmartMovingMod.init()`에서 호출됨 (SmartMovingMod 리서치에서 확인).

4개 모드 존재 여부를 `Loader.isModLoaded()`로 확인해서 캐싱:
- `"modJ_StarMiner"` → StarMiner (비표준 중력 모드)
- `"cuchaz.ships"` → Ships (탑승 가능한 배 모드)
- `"etfuturum"` → Et Futurum Requiem (1.7.10용 미래 버전 기능 백포트 모드)
- `isEtFuturumRequiemElytraPresent`: Et Futurum Requiem이 존재하고 **추가로** `IElytraPlayer` 클래스가 클래스패스에 존재할 때만 `true`

`isEtFuturumRequiemElytraPresent`에 `classExists()` 검사를 추가하는 이유: Et Futurum Requiem의 버전에 따라 엘리트라 API가 없을 수 있기 때문. 모드 자체 존재와 엘리트라 기능 존재를 분리해서 확인.

---

## `classExists(String className)` (private static)

```java
private static boolean classExists(String className)
{
    return Compat.class.getClassLoader().getResourceAsStream(className.replaceAll("\\.", "/") + ".class") != null;
}
```

FQN의 `.`를 `/`로 변환 후 `.class`를 붙여서 클래스패스에서 리소스 스트림으로 존재 여부 확인.  
예: `"ganymedes01.etfuturum.api.elytra.IElytraPlayer"` → `"ganymedes01/etfuturum/api/elytra/IElytraPlayer.class"`

`Class.forName()` 대신 리소스 스트림으로 확인하는 이유: static initializer 실행 없이 클래스 존재만 확인 가능.

---

## `isBlockedByIncompatibility(EntityPlayer sp)`

```java
public static boolean isBlockedByIncompatibility(EntityPlayer sp)
{
    return isStarMinerGravitized(sp) || isOnCuchazShip(sp) || isElytraFlying(sp) || isSpectator(sp);
}
```

4가지 조건 중 하나라도 `true`이면 SmartMoving 동작 차단:

| 조건 | 의미 |
|------|------|
| `isStarMinerGravitized(sp)` | 비표준 중력(StarMiner 중력 방향 변경 또는 무중력) |
| `isOnCuchazShip(sp)` | 배 위에 타고 있음 |
| `isElytraFlying(sp)` | 엘리트라 비행 중 |
| `isSpectator(sp)` | 스펙테이터 모드 |

---

## `isStarMinerGravitized(Entity player)`

```java
public static boolean isStarMinerGravitized(Entity player)
{
    return isStarMinerPresent && StarMinerCompat.isGravitized(player);
}
```

StarMiner 미설치 시 `false`. 설치 시 → `StarMinerCompat.isGravitized(player)`.

---

## `isOnCuchazShip(Entity player)`

```java
public static boolean isOnCuchazShip(Entity player)
{
    return isShipsModPresent && ShipsCompat.isOnShip(player);
}
```

Ships 모드 미설치 시 `false`. 설치 시 → `ShipsCompat.isOnShip(player)`.

---

## `isElytraFlying(Entity player)`

```java
public static boolean isElytraFlying(Entity player)
{
    return isEtFuturumRequiemElytraPresent && EtFuturumRequiemElytraCompat.isElytraFlying(player);
}
```

`isEtFuturumRequiemElytraPresent`(모드 + 클래스 둘 다 있을 때)가 `false`면 `false`. `true`면 → `EtFuturumRequiemElytraCompat.isElytraFlying(player)`.

---

## `isSpectator(Entity player)`

```java
public static boolean isSpectator(Entity player)
{
    return isEtFuturumRequiemPresent && Minecraft.getMinecraft().playerController.currentGameType.getID() == 3;
}
```

Et Futurum Requiem이 없으면 `false`. 있으면 `playerController.currentGameType.getID() == 3` 확인.  
게임 타입 ID 3 = 스펙테이터 모드 (Et Futurum Requiem이 1.7.10에 스펙테이터 모드를 추가).

`player` 파라미터를 실제로 사용하지 않음 — 클라이언트 전역 `currentGameType`만 확인.

---

## 내부 클래스

### `StarMinerCompat` (private static class)

```java
private static class StarMinerCompat
{
    public static boolean isGravitized(Entity player)
    {
        Gravity gravity = ExtendedPropertyGravity.getExtendedPropertyGravity(player);
        return gravity.gravityDirection != GravityDirection.upTOdown_YN || gravity.isZeroGravity();
    }
}
```

`ExtendedPropertyGravity.getExtendedPropertyGravity(player)`로 플레이어의 중력 정보를 가져온다.

**비표준 중력 조건**:
- `gravity.gravityDirection != GravityDirection.upTOdown_YN` — 기본(위→아래, Y음수 방향)이 아닌 다른 방향
- `gravity.isZeroGravity()` — 무중력

둘 중 하나라도 `true`이면 "비표준 중력" → SmartMoving 차단.

---

### `ShipsCompat` (private static class)

```java
private static class ShipsCompat
{
    public static boolean isOnShip(Entity player)
    {
        for (EntityShip ship : ShipLocator.getFromEntityLocation(player))
        {
            if (ship.getCollider().isEntityAboard(player))
            {
                return true;
            }
        }
        return false;
    }	
}
```

`ShipLocator.getFromEntityLocation(player)`로 플레이어 위치에 있는 모든 배를 가져온다.  
각 배의 `getCollider().isEntityAboard(player)`로 플레이어가 실제로 그 배에 탑승 중인지 확인.  
하나라도 탑승 중이면 `true`.

---

### `EtFuturumRequiemElytraCompat` (private static class)

```java
private static class EtFuturumRequiemElytraCompat
{
    public static boolean isElytraFlying(Entity player)
    {
        return player instanceof IElytraPlayer && ((IElytraPlayer)player).etfu$isElytraFlying();
    }
}
```

`player instanceof IElytraPlayer` — Et Futurum Requiem이 `EntityPlayer`에 `IElytraPlayer` 인터페이스를 믹스인한 경우에만 `true`.  
`etfu$isElytraFlying()` — 엘리트라 비행 중인지 여부. 메서드명의 `etfu$` 접두사는 인터페이스 충돌 방지를 위한 Et Futurum Requiem의 네이밍 컨벤션.

---

## 내부 클래스를 분리한 이유

StarMiner, Ships, Et Futurum Requiem의 클래스(`ExtendedPropertyGravity`, `ShipLocator` 등)는 해당 모드가 없을 때 클래스 로더가 해당 클래스를 로드하려 하면 `NoClassDefFoundError`가 발생한다.

내부 static class에 실제 API 호출을 격리하면, `isStarMinerPresent == false`일 때 `StarMinerCompat` 클래스 자체가 로드되지 않으므로 에러가 발생하지 않는다. (Java 클래스 지연 로딩 특성 활용)

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `cpw.mods.fml.common.Loader` | `isModLoaded()` — FML 모드 레지스트리 |
| `cuchaz.ships.*` | Ships 모드 API (ShipsCompat 내부) |
| `jp.mc.ancientred.starminer.*` | StarMiner 모드 API (StarMinerCompat 내부) |
| `ganymedes01.etfuturum.*` | Et Futurum Requiem API (EtFuturumRequiemElytraCompat 내부) |
| `net.minecraft.client.Minecraft` | `playerController.currentGameType` |

호출처: `SmartMovingMod.init()` (Compat.init), `SmartMovingSelf` 또는 기타 이동 로직에서 `isBlockedByIncompatibility()` 호출 (이 파일 외부에서 확인 필요).

---

## 주요 관찰 사항

1. **내부 클래스로 클래스 로딩 격리**: 호환 모드 미설치 시 `NoClassDefFoundError` 방지를 위해 각 모드별 API 호출을 별도 내부 static class에 격리. Java 클래스 지연 로딩을 활용한 방어적 설계.

2. **`classExists()` 이중 검사**: `isModLoaded()`만으로 부족할 때(모드 버전에 따라 클래스가 없을 수 있을 때) 추가로 클래스패스 직접 확인. `IElytraPlayer`에만 적용.

3. **스펙테이터 모드 gameType ID 3**: 바닐라 1.7.10에는 스펙테이터가 없어서 Et Futurum Requiem 설치 여부를 전제로 확인. `player` 파라미터 미사용 — 클라이언트 전역 상태만 확인.

4. **`GravityDirection.upTOdown_YN`**: StarMiner의 기본 중력 방향 상수 이름. `upTOdown_YN`은 "위에서 아래로, Y 음수 방향". 이외의 방향(X축, Z축 등)이나 무중력이면 SmartMoving 차단.

5. **Ships 모드 탑승 확인**: 위치 기반으로 배 목록을 가져온 후 각 배에 실제로 탑승 중인지(`isEntityAboard`) 확인. 단순히 배 근처가 아닌 탑승 중인 경우만 차단.

6. **1.21.1 이식**: 이 파일의 호환 대상 모드들(StarMiner, Ships/cuchaz, Et Futurum Requiem)이 1.21.1에 존재하지 않으므로 대부분 제거. 1.21.1에는 바닐라 스펙테이터/엘리트라가 있으므로 `isSpectator`와 `isElytraFlying`은 바닐라 API(`GameMode.SPECTATOR`, `LivingEntity.isFallFlying()`)로 재구현 가능.
