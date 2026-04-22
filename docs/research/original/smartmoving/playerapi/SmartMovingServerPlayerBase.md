# SmartMovingServerPlayerBase.java (net.smart.moving.playerapi) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/playerapi/SmartMovingServerPlayerBase.java  
패키지: `net.smart.moving.playerapi`  
종류: `class`  
상속: `ServerPlayerBase` (extends), `IEntityPlayerMP` (implements)  
실행 위치: 서버

---

## 전체 소스

```java
package net.smart.moving.playerapi;

import io.netty.buffer.*;

import java.util.*;

import cpw.mods.fml.common.network.internal.*;

import api.player.server.*;

import net.smart.moving.*;
import net.smart.utilities.*;
import net.minecraft.entity.*;
import net.minecraft.util.*;

public class SmartMovingServerPlayerBase extends ServerPlayerBase implements IEntityPlayerMP
{
    public static void registerPlayerBase()
    {
        ServerPlayerAPI.register(SmartMovingInfo.ModName, SmartMovingServerPlayerBase.class);
    }

    public static SmartMovingServerPlayerBase getPlayerBase(Object player)
    {
        return (SmartMovingServerPlayerBase)((IServerPlayerAPI)player).getServerPlayerBase(SmartMovingInfo.ModName);
    }

    public SmartMovingServerPlayerBase(ServerPlayerAPI playerApi)
    {
        super(playerApi);
        moving = new SmartMovingServer(this, false);
    }

    @Override
    public float getHeight()
    {
        return player.height;
    }

    @Override
    public double getMinY()
    {
        return player.boundingBox.minY;
    }

    @Override
    public void setMaxY(double maxY)
    {
        player.boundingBox.maxY = maxY;
    }

    @Override
    public void afterSetPosition(double d, double d1, double d2)
    {
        moving.afterSetPosition(d, d1, d2);
    }

    @Override
    public void beforeIsPlayerSleeping()
    {
        moving.beforeIsPlayerSleeping();
    }

    @Override
    public void beforeOnUpdate()
    {
        moving.beforeOnUpdate();
    }

    @Override
    public void afterOnUpdate()
    {
        moving.afterOnUpdate();
    }

    @Override
    public void beforeOnLivingUpdate()
    {
        moving.beforeOnLivingUpdate();
    }

    @Override
    public void afterOnLivingUpdate()
    {
        moving.afterOnLivingUpdate();
    }

    @Override
    public float doGetHealth()
    {
        return player.getHealth();
    }

    @Override
    public AxisAlignedBB getBox()
    {
        return player.boundingBox;
    }

    @Override
    public AxisAlignedBB expandBox(AxisAlignedBB box, double x, double y, double z)
    {
        return box.expand(x, y, z);
    }

    @Override
    public List<?> getEntitiesExcludingPlayer(AxisAlignedBB box)
    {
        return player.worldObj.getEntitiesWithinAABBExcludingEntity(player, box);
    }

    @Override
    public boolean isDeadEntity(Entity entity)
    {
        return entity.isDead;
    }

    @Override
    public void onCollideWithPlayer(Entity entity)
    {
        entity.onCollideWithPlayer(player);
    }

    @Override
    public float getEyeHeight()
    {
        return player.height - 0.18F;
    }

    @Override
    public boolean isEntityInsideOpaqueBlock()
    {
        return moving.isEntityInsideOpaqueBlock();
    }

    @Override
    public boolean localIsEntityInsideOpaqueBlock()
    {
        return super.isEntityInsideOpaqueBlock();
    }

    @Override
    public void addExhaustion(float exhaustion)
    {
        moving.addExhaustion(exhaustion);
    }

    @Override
    public void localAddExhaustion(float exhaustion)
    {
        super.addExhaustion(exhaustion);
    }

    @Override
    public void addMovementStat(double x, double y, double z)
    {
        moving.addMovementStat(x, y, z);
    }

    @Override
    public void localAddMovementStat(double x, double y, double z)
    {
        super.addMovementStat(x, y, z);
    }

    @Override
    public void localPlaySound(String soundId, float volume, float pitch)
    {
        player.playSound(soundId, volume, pitch);
    }

    @Override
    public void beforeUpdatePotionEffects()
    {
        moving.afterAddMovingHungerBatch();
    }

    @Override
    public void afterUpdatePotionEffects()
    {
        moving.beforeAddMovingHungerBatch();
    }

    @Override
    public boolean isSneaking()
    {
        return moving.isSneaking();
    }

    @Override
    public boolean localIsSneaking()
    {
        return playerAPI.localIsSneaking();
    }

    @Override
    public void setHeight(float height)
    {
        player.height = height;
    }

    @Override
    public void sendPacket(byte[] data)
    {
        player.playerNetServerHandler.sendPacket(
            new FMLProxyPacket(Unpooled.wrappedBuffer(data), SmartMovingPacketStream.Id));
    }

    @Override
    public String getUsername()
    {
        return player.getGameProfile().getName();
    }

    @Override
    public void resetFallDistance()
    {
        player.fallDistance = 0;
        player.motionY = 0.08;
    }

    @Override
    public void resetTicksForFloatKick()
    {
        Reflect.SetField(
            net.minecraft.network.NetHandlerPlayServer.class,
            player.playerNetServerHandler,
            SmartMovingInstall.NetServerHandler_ticksForFloatKick,
            0);
    }

    @Override
    public void sendPacketToTrackedPlayers(FMLProxyPacket packet)
    {
        player.mcServer.worldServerForDimension(player.dimension)
            .getEntityTracker().func_151247_a(player, packet);
    }

    @Override
    public SmartMovingServer getMoving()
    {
        return moving;
    }

    @Override
    public IEntityPlayerMP[] getAllPlayers()
    {
        List<?> playerEntityList = player.mcServer.getConfigurationManager().playerEntityList;
        IEntityPlayerMP[] result = new IEntityPlayerMP[playerEntityList.size()];
        for(int i=0; i<playerEntityList.size(); i++)
            result[i] = (IEntityPlayerMP)((IServerPlayerAPI)playerEntityList.get(i))
                            .getServerPlayerBase(SmartMovingInfo.ModName);
        return result;
    }

    public final SmartMovingServer moving;
}
```

---

## 역할

PlayerAPI의 서버 플레이어 베이스 구현체. `ServerPlayerBase`를 상속하여 PlayerAPI가 제공하는 서버 측 vanilla 메서드 hook에 SmartMoving 로직을 삽입한다. `IEntityPlayerMP`를 구현하여 `SmartMovingServer`가 서버 플레이어에 접근할 수 있는 통합 인터페이스를 제공한다.

`ServerPlayerBase`가 제공하는 필드:
- `player`: `EntityPlayerMP` — 서버 플레이어 엔티티
- `playerAPI`: `ServerPlayerAPI` — 서버 PlayerAPI 인스턴스

---

## import

```java
import io.netty.buffer.*;                              // Unpooled
import java.util.*;                                    // List
import cpw.mods.fml.common.network.internal.*;         // FMLProxyPacket
import api.player.server.*;                            // ServerPlayerBase, ServerPlayerAPI, IServerPlayerAPI
import net.smart.moving.*;                             // IEntityPlayerMP, SmartMovingServer, SmartMovingPacketStream, SmartMovingInfo, SmartMovingInstall
import net.smart.utilities.*;                          // Reflect
import net.minecraft.entity.*;                         // Entity, AxisAlignedBB
import net.minecraft.util.*;                           // AxisAlignedBB (또는 위에서 import)
```

---

## 필드

```java
public final SmartMovingServer moving;
```

서버 SM 로직 객체. `SmartMovingPlayerBase.moving`(SmartMovingSelf, non-final)과 달리 `final` 선언.

---

## 정적 메서드

### `registerPlayerBase()`

```java
public static void registerPlayerBase()
{
    ServerPlayerAPI.register(SmartMovingInfo.ModName, SmartMovingServerPlayerBase.class);
}
```

PlayerAPI에 서버 PlayerBase를 등록. `SmartMoving.register()` → `SmartMovingServerPlayerBase.registerPlayerBase()` 순서로 호출.

---

### `getPlayerBase(Object player)`

```java
public static SmartMovingServerPlayerBase getPlayerBase(Object player)
{
    return (SmartMovingServerPlayerBase)((IServerPlayerAPI)player).getServerPlayerBase(SmartMovingInfo.ModName);
}
```

파라미터 타입이 `Object` — `SmartMovingFactory.doGetInstance(EntityPlayer)` 또는 `getAllPlayers()` 내부에서 실제 `EntityPlayerMP`를 전달. `IServerPlayerAPI`로 캐스팅하여 PlayerBase 조회.

클라이언트의 `getPlayerBase(EntityPlayerSP)`와 달리 `Object` 수신 — 서버 플레이어 타입이 컴파일 시 명확하지 않은 경우 대응.

---

## 생성자

```java
public SmartMovingServerPlayerBase(ServerPlayerAPI playerApi)
{
    super(playerApi);
    moving = new SmartMovingServer(this, false);
}
```

`new SmartMovingServer(this, false)`:
- `this`: `IEntityPlayerMP` 구현체
- `false`: SmartMovingServer 생성자 두 번째 파라미터 — SmartMovingServer 리서치에서 확인됨

---

## 메서드 — IEntityPlayerMP 구현

### 플레이어 크기/위치 접근

```java
public float  getHeight()               { return player.height; }
public double getMinY()                 { return player.boundingBox.minY; }
public void   setMaxY(double maxY)      { player.boundingBox.maxY = maxY; }
public void   setHeight(float height)   { player.height = height; }
public float  getEyeHeight()            { return player.height - 0.18F; }
public AxisAlignedBB getBox()           { return player.boundingBox; }
```

`getEyeHeight()` 공식: `player.height - 0.18F` — vanilla 고정 오프셋. height가 변경되면 eye height도 같이 이동.

---

### AABB 연산

```java
public AxisAlignedBB expandBox(AxisAlignedBB box, double x, double y, double z)
{
    return box.expand(x, y, z);
}
```

`box.expand(x, y, z)` — AABB 각 방향으로 확장. 충돌 엔티티 탐색 범위 확장 시 사용.

---

### 엔티티 조회

```java
public List<?> getEntitiesExcludingPlayer(AxisAlignedBB box)
{
    return player.worldObj.getEntitiesWithinAABBExcludingEntity(player, box);
}

public boolean isDeadEntity(Entity entity)
{
    return entity.isDead;
}

public void onCollideWithPlayer(Entity entity)
{
    entity.onCollideWithPlayer(player);
}
```

---

### 플레이어 상태 조회

```java
public float   doGetHealth()    { return player.getHealth(); }
public String  getUsername()    { return player.getGameProfile().getName(); }
public boolean isSneaking()     { return moving.isSneaking(); }
public boolean localIsSneaking(){ return playerAPI.localIsSneaking(); }
```

`getUsername()`: `GameProfile.getName()` — 인증 기반 플레이어 이름.  
`localIsSneaking()`: `playerAPI.localIsSneaking()` — PlayerAPI 내부 local 호출.

---

### 체력/이동 통계/소진

```java
public void addExhaustion(float exhaustion)     { moving.addExhaustion(exhaustion); }
public void localAddExhaustion(float exhaustion){ super.addExhaustion(exhaustion); }

public void addMovementStat(double x, double y, double z)     { moving.addMovementStat(x, y, z); }
public void localAddMovementStat(double x, double y, double z){ super.addMovementStat(x, y, z); }
```

`local*`: PlayerAPI 체인 우회 직접 vanilla 호출.

---

### 사운드

```java
public void localPlaySound(String soundId, float volume, float pitch)
{
    player.playSound(soundId, volume, pitch);
}
```

`local` 접두사이지만 `super.playSound()`가 아닌 `player.playSound()` 직접 호출.

---

### 블록 내부 판정

```java
public boolean isEntityInsideOpaqueBlock()
{
    return moving.isEntityInsideOpaqueBlock();
}

public boolean localIsEntityInsideOpaqueBlock()
{
    return super.isEntityInsideOpaqueBlock();
}
```

---

### 낙하/부유 리셋

```java
public void resetFallDistance()
{
    player.fallDistance = 0;
    player.motionY = 0.08;     // 약간의 상향 초기 속도
}
```

`fallDistance` 초기화 + `motionY = 0.08` 동시 설정. 클라이밍 착지 후 낙하 데미지 방지 목적. `motionY = 0.08`은 vanilla의 수직 운동 초기값(바닥에서 `motionY`가 -0.08인 상태에서 올라오는 역방향 보정).

```java
public void resetTicksForFloatKick()
{
    Reflect.SetField(
        net.minecraft.network.NetHandlerPlayServer.class,
        player.playerNetServerHandler,
        SmartMovingInstall.NetServerHandler_ticksForFloatKick,
        0);
}
```

`NetHandlerPlayServer.ticksForFloatKick` — 플레이어가 공중에 떠 있는 틱 수 카운터. 일정 값 이상이면 서버가 플레이어를 kick. 클라이밍/비행 중 의도치 않은 kick 방지를 위해 리셋.  
`SmartMovingInstall.NetServerHandler_ticksForFloatKick` — 필드 이름 문자열 (리플렉션 탐색용).

---

### 패킷 전송

```java
public void sendPacket(byte[] data)
{
    player.playerNetServerHandler.sendPacket(
        new FMLProxyPacket(Unpooled.wrappedBuffer(data), SmartMovingPacketStream.Id));
}
```

`byte[] data` → `Unpooled.wrappedBuffer(data)` (Netty ByteBuf 래핑, 복사 없음) → `FMLProxyPacket` → `NetServerHandler.sendPacket()`.  
`SmartMovingPacketStream.Id` — 채널 ID. 15자 제한 문자열 (SmartMovingPacketStream 리서치에서 확인됨).

```java
public void sendPacketToTrackedPlayers(FMLProxyPacket packet)
{
    player.mcServer.worldServerForDimension(player.dimension)
        .getEntityTracker().func_151247_a(player, packet);
}
```

`func_151247_a` — 세레나이즈드(난독화) EntityTracker 메서드. 이 플레이어를 추적 중인 모든 플레이어에게 패킷 전송. `player.dimension` — 현재 차원 ID.

---

### 전체 플레이어 조회

```java
public IEntityPlayerMP[] getAllPlayers()
{
    List<?> playerEntityList = player.mcServer.getConfigurationManager().playerEntityList;
    IEntityPlayerMP[] result = new IEntityPlayerMP[playerEntityList.size()];
    for(int i=0; i<playerEntityList.size(); i++)
        result[i] = (IEntityPlayerMP)((IServerPlayerAPI)playerEntityList.get(i))
                        .getServerPlayerBase(SmartMovingInfo.ModName);
    return result;
}
```

서버 전체 플레이어 목록(`player.mcServer.getConfigurationManager().playerEntityList`) 순회.  
각 `EntityPlayerMP`를 `IServerPlayerAPI`로 캐스팅 → `getServerPlayerBase(SmartMovingInfo.ModName)` → `SmartMovingServerPlayerBase` → `IEntityPlayerMP` 캐스팅.

---

### PlayerAPI hook 메서드

```java
public void afterSetPosition(double d, double d1, double d2) { moving.afterSetPosition(d, d1, d2); }
public void beforeIsPlayerSleeping()                         { moving.beforeIsPlayerSleeping(); }
public void beforeOnUpdate()                                 { moving.beforeOnUpdate(); }
public void afterOnUpdate()                                  { moving.afterOnUpdate(); }
public void beforeOnLivingUpdate()                           { moving.beforeOnLivingUpdate(); }
public void afterOnLivingUpdate()                            { moving.afterOnLivingUpdate(); }
```

`SmartMovingPlayerBase`와 달리 `moving.isActive()` 체크 없이 **모두 무조건** `moving.*` 위임.

### `beforeUpdatePotionEffects` / `afterUpdatePotionEffects` ⭐

```java
public void beforeUpdatePotionEffects()
{
    moving.afterAddMovingHungerBatch();   // before hook → moving.after*
}

public void afterUpdatePotionEffects()
{
    moving.beforeAddMovingHungerBatch();  // after hook → moving.before*
}
```

hook 이름과 호출 메서드 이름이 의도적으로 역전:
- `beforeUpdatePotionEffects()`: 포션 업데이트 실행 **전** → `moving.afterAddMovingHungerBatch()` (허기 배치 **완료** 처리)
- `afterUpdatePotionEffects()`: 포션 업데이트 실행 **후** → `moving.beforeAddMovingHungerBatch()` (허기 배치 **시작** 준비)

**의미**: `beforeUpdatePotionEffects` 시점에서 허기 배치가 이미 소비되어야 하고, `afterUpdatePotionEffects` 시점에 다음 배치를 준비. 포션 업데이트(`updatePotionEffects`)가 허기 처리를 포함하므로, SM의 이동 기반 허기 계산을 그 전후에 올바른 순서로 처리.

---

### `getMoving()`

```java
public SmartMovingServer getMoving()
{
    return moving;
}
```

`IEntityPlayerMP.getMoving()` 구현 — 반환 타입 `SmartMovingServer`.

---

## 메서드 전체 목록

| 메서드 | 종류 | 구현 |
|--------|------|------|
| `getHeight()` | 필드 접근 | `player.height` |
| `getMinY()` | 필드 접근 | `player.boundingBox.minY` |
| `setMaxY(maxY)` | 필드 설정 | `player.boundingBox.maxY = maxY` |
| `setHeight(height)` | 필드 설정 | `player.height = height` |
| `getEyeHeight()` | 계산 | `player.height - 0.18F` |
| `getBox()` | 필드 접근 | `player.boundingBox` |
| `expandBox(box,x,y,z)` | 위임 | `box.expand(x,y,z)` |
| `afterSetPosition(d,d1,d2)` | hook | `moving.afterSetPosition()` |
| `beforeIsPlayerSleeping()` | hook | `moving.beforeIsPlayerSleeping()` |
| `beforeOnUpdate()` | hook | `moving.beforeOnUpdate()` |
| `afterOnUpdate()` | hook | `moving.afterOnUpdate()` |
| `beforeOnLivingUpdate()` | hook | `moving.beforeOnLivingUpdate()` |
| `afterOnLivingUpdate()` | hook | `moving.afterOnLivingUpdate()` |
| `doGetHealth()` | 필드 접근 | `player.getHealth()` |
| `getEntitiesExcludingPlayer(box)` | 조회 | `worldObj.getEntitiesWithinAABBExcludingEntity()` |
| `isDeadEntity(entity)` | 조회 | `entity.isDead` |
| `onCollideWithPlayer(entity)` | 위임 | `entity.onCollideWithPlayer(player)` |
| `isEntityInsideOpaqueBlock()` | 위임 | `moving.isEntityInsideOpaqueBlock()` |
| `localIsEntityInsideOpaqueBlock()` | local | `super.isEntityInsideOpaqueBlock()` |
| `addExhaustion(exhaustion)` | 위임 | `moving.addExhaustion()` |
| `localAddExhaustion(exhaustion)` | local | `super.addExhaustion()` |
| `addMovementStat(x,y,z)` | 위임 | `moving.addMovementStat()` |
| `localAddMovementStat(x,y,z)` | local | `super.addMovementStat()` |
| `localPlaySound(id,vol,pitch)` | 위임 | `player.playSound()` |
| `beforeUpdatePotionEffects()` | hook | `moving.afterAddMovingHungerBatch()` |
| `afterUpdatePotionEffects()` | hook | `moving.beforeAddMovingHungerBatch()` |
| `isSneaking()` | 위임 | `moving.isSneaking()` |
| `localIsSneaking()` | local | `playerAPI.localIsSneaking()` |
| `sendPacket(data)` | 네트워크 | `FMLProxyPacket` 생성 후 전송 |
| `getUsername()` | 조회 | `player.getGameProfile().getName()` |
| `resetFallDistance()` | 상태 초기화 | `fallDistance=0; motionY=0.08` |
| `resetTicksForFloatKick()` | 리플렉션 | `Reflect.SetField(..., 0)` |
| `sendPacketToTrackedPlayers(packet)` | 네트워크 | `getEntityTracker().func_151247_a()` |
| `getMoving()` | 접근자 | `moving` 반환 |
| `getAllPlayers()` | 조회 | 서버 전체 플레이어 → `IEntityPlayerMP[]` |

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `ServerPlayerBase` | 상속 — `player`, `playerAPI` 필드, `super.*` 메서드 |
| `ServerPlayerAPI` | PlayerBase 등록 |
| `IServerPlayerAPI` | 플레이어에서 PlayerBase 조회 |
| `IEntityPlayerMP` | 구현 인터페이스 |
| `SmartMovingServer` | 서버 SM 로직 위임 대상 |
| `SmartMovingPacketStream.Id` | 패킷 채널 ID |
| `SmartMovingInstall.NetServerHandler_ticksForFloatKick` | 리플렉션 필드 이름 |
| `Reflect.SetField()` | 리플렉션 필드 설정 |
| `FMLProxyPacket` | Forge 패킷 래퍼 |
| `Unpooled` | Netty ByteBuf 생성 |

---

## 주요 관찰 사항

1. **isActive() 체크 없음**: `SmartMovingPlayerBase`의 hook 메서드들은 `moving.isActive()` 체크 후 분기하지만, 이 클래스의 모든 hook은 isActive 체크 없이 무조건 `moving.*` 위임. 서버 측 SmartMovingServer는 클라이언트 SmartMovingSelf와 달리 항상 active 상태로 동작하는 것으로 추정 → SmartMovingServer 리서치에서 확인 필요.

2. **`beforeUpdatePotionEffects`/`afterUpdatePotionEffects` 역전**: before hook → `moving.after*`, after hook → `moving.before*`. 포션 업데이트 사이클과 SM 허기 배치 처리 사이클의 타이밍을 맞추기 위한 의도적 역전.

3. **`resetFallDistance()` — motionY=0.08**: 낙하 거리만 초기화하지 않고 `motionY`도 0.08로 설정. 클라이밍에서 착지 직전의 하강 속도를 무효화하는 방어 코드.

4. **`func_151247_a`**: Forge 세레나이즈 이름. EntityTracker에서 해당 플레이어를 추적하는 모든 클라이언트에 패킷 전송하는 메서드. 1.21.1에서는 역매핑으로 실제 이름 확인 필요.

5. **`getPlayerBase(Object player)`**: 파라미터 `Object` — 컴파일 타임 타입 무관하게 서버 플레이어라면 동작. `getAllPlayers()`에서 `playerEntityList.get(i)` 결과를 그대로 전달 가능.

6. **1.21.1 이식 관련**:
   - `ServerPlayerBase` / `ServerPlayerAPI` → Fabric Mixin으로 대체
   - `player.playerNetServerHandler` → `serverPlayer.networkHandler`
   - `FMLProxyPacket` + `Unpooled` → Fabric CustomPayload packet API
   - `func_151247_a` → `ServerWorld.getChunkManager().sendToOtherNearbyPlayers()` 또는 `PlayerLookup.tracking()`
   - `NetHandlerPlayServer.ticksForFloatKick` → `ServerPlayNetworkHandler.floatingTicks` (Yarn 이름 확인 필요)
   - `player.mcServer.getConfigurationManager().playerEntityList` → `server.getPlayerManager().getPlayerList()`
   - `player.worldObj.getEntitiesWithinAABBExcludingEntity()` → `world.getOtherEntities(entity, box)`

---

## R-04 리서치 결과 추가 (A-11, A-12, A-17)

---

### A-11 — `getPoses()` 원본 SmartMoving 내 존재 여부

**결론: SmartMoving 전체 계층에 `getPoses()` 메서드가 존재하지 않는다** (직접 확인됨).

확인한 파일 전체:
- `SmartMovingContext.java` — 없음
- `SmartMovingBase.java` (전체 소스 확인) — 없음
- `SmartMoving.java` (abstract, 전체 소스 확인) — 없음
- `SmartMovingSelf.java` (전체 소스 확인) — 없음
- `SmartMovingPlayerBase.java` (전체 소스 확인) — 없음
- `SmartMovingServerPlayerBase.java` (전체 소스 확인) — 없음
- `SmartMovingServer.java` (전체 소스 확인) — 없음

**이유**: 1.7.10 Minecraft에 `EntityPose` 열거형 및 `LivingEntity.getPoses()` API 자체가 존재하지 않음.

**1.7.10 SM의 히트박스/크기 변경 메커니즘 (대응 코드, 확인됨):**

```java
// SmartMovingServerPlayerBase (IEntityPlayerMP 구현)
public float  getHeight()               { return player.height; }
public void   setHeight(float height)   { player.height = height; }
public double getMinY()                 { return player.boundingBox.minY; }
public void   setMaxY(double maxY)      { player.boundingBox.maxY = maxY; }

// SmartMovingServer.setSmall()
public void setSmall(boolean isSmall)
{
    mp.setHeight(isSmall ? 0.8F : 1.8F);
    this.isSmall = isSmall;
}

// SmartMovingServer.afterSetPosition() — crawling 중 maxY 보정
mp.setMaxY(mp.getMinY() + mp.getHeight() - 1);

// SmartMovingServer.beforeIsPlayerSleeping() — 복원
mp.setMaxY(mp.getMinY() + mp.getHeight());
crawlingInitialized = true;
```

**1.21.1 대응 메커니즘**:
- `player.height` 직접 변경 → `getDimensions(EntityPose)` override + `calculateDimensions()` 호출
- `boundingBox.maxY` 직접 변경 → 1.21.1에서는 `EntityDimensions`로 관리됨
- `getPoses()`를 Mixin하여 SM 커스텀 포즈 허용 여부 확인 필요 (B-01~B-04 미확인 → [미확인])

---

### A-12 — `reverseHandleMaterialAcceleration()` 실제 로직 및 1.21.1 필요 여부

**실제 위치**: `SmartMovingBase.java` (직접 확인됨). 이미 `SmartMovingBase.md` 884~932줄 섹션에 완전히 기록됨.

**요약 (SmartMovingBase.md에서 확인된 코드):**

```java
// SmartMovingBase.java (공개 메서드)
public void reverseHandleMaterialAcceleration()
{
    AxisAlignedBB axisalignedbb = sp.boundingBox.expand(0.0D, -0.40000000596046448D, 0.0D).contract(0.001D, 0.001D, 0.001D);
    Material material = Material.water;
    // ... (물 블록 탐색) ...
    if(vec3d.lengthVector() > 0.0D)
    {
        vec3d = vec3d.normalize();
        double d = -0.014D; // vanilla는 +0.014D — 여기서 역전
        entity.motionX += vec3d.xCoord * d;
        entity.motionY += vec3d.yCoord * d;
        entity.motionZ += vec3d.zCoord * d;
    }
}
```

**호출 위치** (`SmartMovingBase.correctOnUpdate()`):
```java
public void correctOnUpdate(boolean isSmall, boolean reverseMaterialAcceleration)
{
    // ... renderYawOffset 보정 ...
    if(reverseMaterialAcceleration)
        reverseHandleMaterialAcceleration();
}
```

**목적**: 수영 중 vanilla `handleMaterialAcceleration`이 적용하는 물 흐름 가속(`+0.014D`)을 `-0.014D`로 역전하여 상쇄.
- AABB: `boundingBox.expand(0, -0.4D, 0).contract(0.001D, 0.001D, 0.001D)` — Y 방향 0.4D 축소
- Material.water 전용

**1.21.1 필요 여부**:
- B-11 (`LivingEntity.travel()` 내에 1.21.1에서 reverseHandleMaterialAcceleration에 해당하는 코드 경로 존재 여부) 미확인 → [미확인]
- SM이 `travel()`을 완전 대체하면 vanilla 물 흐름 가속 자체가 실행 안 됨 → `reverseHandleMaterialAcceleration` 불필요
- SM이 `travel()`을 부분 인터셉트만 하면 vanilla 흐름 가속이 그대로 적용됨 → 상쇄 필요
- **최종 판단**: B-11 리서치(R-08 청크) 완료 후 결정

---

### A-17 — 서버 물리 재현 범위 (SmartMovingServer.java 전체 확인됨)

`SmartMovingServerPlayerBase`는 모든 물리 계산을 `SmartMovingServer` 객체(`moving`)에 위임.  
`SmartMovingServer.java` 전체 소스 확인됨 (SmartMovingServer.md에 기록됨).

**서버가 수행하는 물리 재현 목록 (확인됨):**

| 메서드 | 계산 내용 |
|--------|---------|
| `setSmall(isSmall)` | `player.height = isSmall ? 0.8F : 1.8F` — 서버 측 플레이어 높이 직접 변경 |
| `afterSetPosition()` | `!crawlingInitialized` → `maxY = minY + height - 1` — 크롤링 boundingBox 보정 |
| `beforeIsPlayerSleeping()` | `maxY = minY + height`, `crawlingInitialized = true` — 복원 |
| `afterOnUpdate()` | `resetFallDistance` → `fallDistance=0; motionY=0.08` (낙하 거리 리셋) |
| `afterOnUpdate()` | `resetTicksForFloatKick` → `ticksForFloatKick=0` (부유 kick 억제) |
| `beforeOnUpdate()` | `crawlingCooldown--` |
| `isEntityInsideOpaqueBlock()` | `crawlingCooldown > 0` → `false` (크롤링 종료 직후 10틱 관통 억제) |
| `afterOnLivingUpdate()` | `isSmall` 시 아이템 grab 범위 Y방향 `+0.25F` 확장 |
| `addExhaustion()` | `disableAddExhaustion` 조건으로 vanilla 소진 필터링 |
| `addMovementStat()` | `beforeAddMovingHungerBatch()` → `localAddMovementStat()` → `addExhaustion(hunger)` → `afterAddMovingHungerBatch()` |
| `isSneaking()` | `forceIsSneaking != null` → forceIsSneaking; 아니면 `localIsSneaking()` |
| `processStatePacket()` | 클라이언트 state 패킷에서 7개 비트 추출 → 서버 상태 동기화 + 릴레이 |

**`processStatePacket`에서 서버가 추출하는 비트 (state long, 직접 확인됨):**

| 비트 위치 (`state >>> N`) | 추출 상태 | 용도 |
|--------------------------|----------|------|
| 33 | `isSneakButtonPressed` | `forceIsSneaking` 설정 |
| 31 | `isWallJumping` | `resetFallDistance` 트리거 |
| 18 | `isCeilingClimbing` | `resetFallDistance` + `resetTicksForFloatKick` 트리거 |
| 15 | `isSmall` | `setSmall()` 호출 (height 변경) |
| 14 | `isClimbing` | `resetFallDistance` + `resetTicksForFloatKick` 트리거 |
| 13 | `isCrawling` | `setCrawling()` 호출 (crawlingCooldown 관리) |
| 12 | `isCrawlClimbing` | `resetFallDistance` + `resetTicksForFloatKick` 트리거 |

**isSliding 비트**: 서버가 `processStatePacket`에서 `isSliding`을 추출하지 않음 (확인됨). 서버는 슬라이딩 상태를 별도 추적하지 않음.

**`resetFallDistance` 트리거 조건**: `isClimbing || isCrawlClimbing || isCeilingClimbing || isWallJumping`  
**`resetTicksForFloatKick` 트리거 조건**: `isClimbing || isCrawlClimbing || isCeilingClimbing` (isWallJumping 미포함)

**서버가 수행하지 않는 것:**
- 클라이밍/수영/크롤링/다이빙의 실제 모션 벡터 계산 — 클라이언트가 담당
- 블록 충돌 계산 — 클라이언트가 담당
- 사운드 재생 판단 — 클라이언트가 담당
- `reverseHandleMaterialAcceleration()` 호출 — 서버 물리 계산 없음
- 피로/허기 계산 자체 — 클라이언트 이동 결과를 `hunger` 필드로 받아 `addMovementStat()` 시 적용

**`SmallSizeItemGrabHeight = 0.25F` (확인됨):**  
크롤링(isSmall) 중 `afterOnLivingUpdate`에서 AABB를 Y방향 +0.25F 확장하여 바닥 아이템 획득 범위 보정.

**`crawlingCooldown = 10` (확인됨):**  
크롤링 종료 후 10틱간 `isEntityInsideOpaqueBlock` → `false`. 크롤링 종료 시 boundingBox 복원 전 벽 충돌 오판정 방지.
