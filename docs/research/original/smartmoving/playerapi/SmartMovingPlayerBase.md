# SmartMovingPlayerBase.java (net.smart.moving.playerapi) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/playerapi/SmartMovingPlayerBase.java  
패키지: `net.smart.moving.playerapi`  
종류: `class`  
상속: `ClientPlayerBase` (extends), `IEntityPlayerSP` (implements)  
실행 위치: 클라이언트

---

## 전체 소스

```java
package net.smart.moving.playerapi;

import api.player.client.*;

import net.minecraft.block.material.*;
import net.minecraft.client.*;
import net.minecraft.client.entity.*;
import net.minecraft.entity.player.EntityPlayer.*;
import net.minecraft.nbt.*;

import net.smart.moving.*;

public class SmartMovingPlayerBase extends ClientPlayerBase implements IEntityPlayerSP
{
    public static void registerPlayerBase()
    {
        ClientPlayerAPI.register(SmartMovingInfo.ModName, SmartMovingPlayerBase.class);
    }

    public static SmartMovingPlayerBase getPlayerBase(EntityPlayerSP player)
    {
        return (SmartMovingPlayerBase)((IClientPlayerAPI)player).getClientPlayerBase(SmartMovingInfo.ModName);
    }

    public SmartMovingPlayerBase(ClientPlayerAPI playerApi)
    {
        super(playerApi);
        moving = new SmartMovingSelf(player, this);
    }

    @Override
    public void beforeMoveEntity(double d, double d1, double d2)
    {
        if(!moving.isActive()) return;
        moving.beforeMoveEntity(d, d1, d2);
    }

    @Override
    public void afterMoveEntity(double d, double d1, double d2)
    {
        if(!moving.isActive()) return;
        moving.afterMoveEntity(d, d1, d2);
    }

    @Override
    public void localMoveEntity(double d, double d1, double d2)
    {
        super.moveEntity(d, d1, d2);
    }

    @Override
    public void beforeSleepInBedAt(int i, int j, int k)
    {
        if(!moving.isActive()) return;
        moving.beforeSleepInBedAt(i, j, k);
    }

    @Override
    public EnumStatus localSleepInBedAt(int i, int j, int k)
    {
        return super.sleepInBedAt(i, j, k);
    }

    @Override
    public float getBrightness(float f)
    {
        if(!moving.isActive()) return localGetBrightness(f);
        return moving.getBrightness(f);
    }

    @Override
    public float localGetBrightness(float f)
    {
        return super.getBrightness(f);
    }

    @Override
    public int getBrightnessForRender(float f)
    {
        if(!moving.isActive()) return localGetBrightnessForRender(f);
        return moving.getBrightnessForRender(f);
    }

    @Override
    public int localGetBrightnessForRender(float f)
    {
        return super.getBrightnessForRender(f);
    }

    @Override
    public boolean pushOutOfBlocks(double d, double d1, double d2)
    {
        if(!moving.isActive()) return super.pushOutOfBlocks(d, d1, d2);
        return moving.pushOutOfBlocks(d, d1, d2);
    }

    @Override
    public void beforeOnUpdate()
    {
        if(!moving.isActive()) return;
        moving.beforeOnUpdate();
    }

    @Override
    public void afterOnUpdate()
    {
        if(!moving.isActive()) return;
        moving.afterOnUpdate();
    }

    @Override
    public void beforeOnLivingUpdate()
    {
        if(!moving.isActive()) return;
        moving.beforeOnLivingUpdate();
    }

    @Override
    public void afterOnLivingUpdate()
    {
        if(!moving.isActive()) return;
        moving.afterOnLivingUpdate();
    }

    @Override
    public boolean getSleepingField()
    {
        return playerAPI.getSleepingField();
    }

    @Override
    public boolean getIsInWebField()
    {
        return playerAPI.getIsInWebField();
    }

    @Override
    public void setIsInWebField(boolean newIsInWeb)
    {
        playerAPI.setIsInWebField(newIsInWeb);
    }

    @Override
    public boolean getIsJumpingField()
    {
        return playerAPI.getIsJumpingField();
    }

    @Override
    public Minecraft getMcField()
    {
        return playerAPI.getMcField();
    }

    @Override
    public void moveEntityWithHeading(float f, float f1)
    {
        if(!moving.isActive()) {
            super.moveEntityWithHeading(f, f1);
            return;
        }
        moving.moveEntityWithHeading(f, f1);
    }

    @Override
    public boolean canTriggerWalking()
    {
        if(!moving.isActive()) return super.canTriggerWalking();
        return moving.canTriggerWalking();
    }

    @Override
    public boolean isOnLadder()
    {
        if(!moving.isActive()) return super.isOnLadder();
        return moving.isOnLadderOrVine();
    }

    @Override
    public SmartMovingSelf getMoving()
    {
        return moving;
    }

    @Override
    public void updateEntityActionState()
    {
        moving.tickEssential();

        if(!moving.isActive()) {
            localUpdateEntityActionState();
            return;
        }
        moving.updateEntityActionState(false);
    }

    @Override
    public void localUpdateEntityActionState()
    {
        super.updateEntityActionState();
    }

    @Override
    public void setIsJumpingField(boolean flag)
    {
        playerAPI.setIsJumpingField(flag);
    }

    @Override
    public void setMoveForwardField(float f)
    {
        player.moveForward = f;
    }

    @Override
    public void setMoveStrafingField(float f)
    {
        player.moveStrafing = f;
    }

    @Override
    public boolean isInsideOfMaterial(Material material)
    {
        if(!moving.isActive()) return localIsInsideOfMaterial(material);
        return moving.isInsideOfMaterial(material);
    }

    @Override
    public boolean localIsInsideOfMaterial(Material material)
    {
        return super.isInsideOfMaterial(material);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nBTTagCompound)
    {
        moving.writeEntityToNBT(nBTTagCompound);
    }

    @Override
    public void localWriteEntityToNBT(NBTTagCompound nBTTagCompound)
    {
        super.writeEntityToNBT(nBTTagCompound);
    }

    @Override
    public boolean isSneaking()
    {
        if(!moving.isActive()) return localIsSneaking();
        return moving.isSneaking();
    }

    @Override
    public boolean localIsSneaking()
    {
        return super.isSneaking();
    }

    @Override
    public float getFOVMultiplier()
    {
        if(!moving.isActive()) return localGetFOVMultiplier();
        return moving.getFOVMultiplier();
    }

    @Override
    public float localGetFOVMultiplier()
    {
        return playerAPI.localGetFOVMultiplier();
    }

    @Override
    public void beforeSetPositionAndRotation(double d, double d1, double d2, float f, float f1)
    {
        if(!moving.isActive()) return;
        moving.beforeSetPositionAndRotation();
    }

    @Override
    public void beforeGetSleepTimer()
    {
        if(!moving.isActive()) return;
        moving.beforeGetSleepTimer();
    }

    @Override
    public void jump()
    {
        if(!moving.isActive()) {
            super.jump();
            return;
        }
        moving.jump();
    }

    public SmartMovingSelf moving;
}
```

---

## 역할

PlayerAPI의 클라이언트 플레이어 베이스 구현체. `ClientPlayerBase`를 상속하여 PlayerAPI가 제공하는 모든 vanilla 메서드 hook에 SmartMoving 로직을 삽입한다. `IEntityPlayerSP`를 구현하여 SmartMovingSelf에서 역으로 이 객체에 접근할 수 있도록 한다.

`ClientPlayerBase`가 제공하는 필드:
- `playerAPI`: `ClientPlayerAPI` — 플레이어 필드 접근자 모음
- `player`: `EntityPlayerSP` — 로컬 플레이어 엔티티

---

## import

```java
import api.player.client.*;             // ClientPlayerBase, ClientPlayerAPI, IClientPlayerAPI
import net.minecraft.block.material.*;  // Material
import net.minecraft.client.*;          // Minecraft
import net.minecraft.client.entity.*;   // EntityPlayerSP
import net.minecraft.entity.player.EntityPlayer.*;  // EnumStatus (내부 enum)
import net.minecraft.nbt.*;             // NBTTagCompound
import net.smart.moving.*;              // IEntityPlayerSP, SmartMovingSelf, SmartMovingInfo
```

---

## 필드

```java
public SmartMovingSelf moving;
```

SmartMoving 핵심 로직 객체. 생성자에서 초기화. `public`이므로 외부에서 직접 접근 가능.

---

## 정적 메서드

### `registerPlayerBase()`

```java
public static void registerPlayerBase()
{
    ClientPlayerAPI.register(SmartMovingInfo.ModName, SmartMovingPlayerBase.class);
}
```

PlayerAPI에 이 클래스를 등록. `SmartMovingInfo.ModName` 키로 등록 — 이후 `getPlayerBase()`에서 동일 키로 조회.  
`SmartMoving.register()` → `SmartMovingPlayerBase.registerPlayerBase()` 순서로 호출됨.

---

### `getPlayerBase(EntityPlayerSP player)`

```java
public static SmartMovingPlayerBase getPlayerBase(EntityPlayerSP player)
{
    return (SmartMovingPlayerBase)((IClientPlayerAPI)player).getClientPlayerBase(SmartMovingInfo.ModName);
}
```

PlayerAPI가 `EntityPlayerSP`에 주입한 `IClientPlayerAPI` 인터페이스를 통해 `SmartMovingPlayerBase` 인스턴스를 조회.  
`(IClientPlayerAPI)player` — PlayerAPI가 `EntityPlayerSP`를 수정하여 이 인터페이스를 구현하도록 만든 것.  
`SmartMovingInfo.ModName` 키로 등록된 PlayerBase 반환 후 `SmartMovingPlayerBase`로 캐스팅.

---

## 생성자

```java
public SmartMovingPlayerBase(ClientPlayerAPI playerApi)
{
    super(playerApi);
    moving = new SmartMovingSelf(player, this);
}
```

PlayerAPI가 `EntityPlayerSP` 생성 시 이 생성자를 호출.  
`player` — `super(playerApi)` 이후 `ClientPlayerBase`가 설정한 `EntityPlayerSP` 참조.  
`new SmartMovingSelf(player, this)` — `this`(IEntityPlayerSP)를 전달하여 SmartMovingSelf가 역참조 가능하게 함.

---

## PlayerAPI hook 메서드 분류

PlayerAPI는 세 종류의 접두사 메서드를 제공:

| 접두사 | 의미 |
|--------|------|
| `before` | vanilla 메서드 실행 직전에 호출 |
| `after` | vanilla 메서드 실행 직후에 호출 |
| `local` | vanilla(super) 메서드를 PlayerAPI 체인 없이 직접 호출 |
| (없음) | vanilla 메서드를 완전히 대체 |

`moving.isActive()` — SmartMoving이 활성화 상태인지 확인. 비활성이면 대부분의 hook이 vanilla 동작으로 폴백.

---

## 메서드 상세

### `beforeMoveEntity` / `afterMoveEntity` / `localMoveEntity`

```java
public void beforeMoveEntity(double d, double d1, double d2)
{
    if(!moving.isActive()) return;
    moving.beforeMoveEntity(d, d1, d2);
}

public void afterMoveEntity(double d, double d1, double d2)
{
    if(!moving.isActive()) return;
    moving.afterMoveEntity(d, d1, d2);
}

public void localMoveEntity(double d, double d1, double d2)
{
    super.moveEntity(d, d1, d2);   // vanilla Entity.moveEntity() 직접 호출
}
```

`Entity.moveEntity(dx, dy, dz)` — AABB 충돌 처리 핵심 메서드.

---

### `beforeSleepInBedAt` / `localSleepInBedAt`

```java
public void beforeSleepInBedAt(int i, int j, int k)
{
    if(!moving.isActive()) return;
    moving.beforeSleepInBedAt(i, j, k);
}

public EnumStatus localSleepInBedAt(int i, int j, int k)
{
    return super.sleepInBedAt(i, j, k);   // EnumStatus 반환
}
```

---

### `getBrightness` / `localGetBrightness`

```java
public float getBrightness(float f)
{
    if(!moving.isActive()) return localGetBrightness(f);
    return moving.getBrightness(f);
}

public float localGetBrightness(float f)
{
    return super.getBrightness(f);
}
```

SM 비활성 시 vanilla fallback에 `super` 대신 `localGetBrightness()` 사용 — 명시적 위임 패턴.

---

### `getBrightnessForRender` / `localGetBrightnessForRender`

```java
public int getBrightnessForRender(float f)
{
    if(!moving.isActive()) return localGetBrightnessForRender(f);
    return moving.getBrightnessForRender(f);
}

public int localGetBrightnessForRender(float f)
{
    return super.getBrightnessForRender(f);
}
```

---

### `pushOutOfBlocks`

```java
public boolean pushOutOfBlocks(double d, double d1, double d2)
{
    if(!moving.isActive()) return super.pushOutOfBlocks(d, d1, d2);
    return moving.pushOutOfBlocks(d, d1, d2);
}
```

블록 내부에서 밀어내기 — SM 활성 시 SmartMovingSelf가 처리(크롤링/클라이밍 중 억제 등).

---

### `beforeOnUpdate` / `afterOnUpdate` / `beforeOnLivingUpdate` / `afterOnLivingUpdate`

```java
public void beforeOnUpdate()
{
    if(!moving.isActive()) return;
    moving.beforeOnUpdate();
}

public void afterOnUpdate()
{
    if(!moving.isActive()) return;
    moving.afterOnUpdate();
}

public void beforeOnLivingUpdate()
{
    if(!moving.isActive()) return;
    moving.beforeOnLivingUpdate();
}

public void afterOnLivingUpdate()
{
    if(!moving.isActive()) return;
    moving.afterOnLivingUpdate();
}
```

`Entity.onUpdate()` → `EntityLivingBase.onLivingUpdate()` 순서로 호출. 각각 전/후 hook.

---

### 필드 접근자 메서드 (playerAPI 위임)

```java
public boolean getSleepingField()   { return playerAPI.getSleepingField(); }
public boolean getIsInWebField()    { return playerAPI.getIsInWebField(); }
public void setIsInWebField(boolean newIsInWeb) { playerAPI.setIsInWebField(newIsInWeb); }
public boolean getIsJumpingField()  { return playerAPI.getIsJumpingField(); }
public Minecraft getMcField()       { return playerAPI.getMcField(); }
public void setIsJumpingField(boolean flag) { playerAPI.setIsJumpingField(flag); }
```

`playerAPI`(`ClientPlayerAPI`)를 통해 `EntityPlayerSP`의 private 필드에 접근. `isActive()` 체크 없음 — 항상 실행.

---

### `setMoveForwardField` / `setMoveStrafingField`

```java
public void setMoveForwardField(float f)    { player.moveForward = f; }
public void setMoveStrafingField(float f)   { player.moveStrafing = f; }
```

`playerAPI`가 아닌 `player`(`EntityPlayerSP`) 필드에 직접 접근. `playerAPI` 경유 없이 직접 할당.

---

### `moveEntityWithHeading`

```java
public void moveEntityWithHeading(float f, float f1)
{
    if(!moving.isActive()) {
        super.moveEntityWithHeading(f, f1);
        return;
    }
    moving.moveEntityWithHeading(f, f1);
}
```

이동 + 물리 처리 핵심 메서드. SM 활성 시 SmartMovingSelf가 완전 대체.

---

### `canTriggerWalking`

```java
public boolean canTriggerWalking()
{
    if(!moving.isActive()) return super.canTriggerWalking();
    return moving.canTriggerWalking();
}
```

걷기 소리/파티클 트리거 여부. 크롤링/클라이밍 중 억제됨.

---

### `isOnLadder`

```java
public boolean isOnLadder()
{
    if(!moving.isActive()) return super.isOnLadder();
    return moving.isOnLadderOrVine();   // 주목: isOnLadder()가 아닌 isOnLadderOrVine()
}
```

SM 활성 시 `moving.isOnLadder()` 대신 `moving.isOnLadderOrVine()` 호출 — vine도 포함하는 확장 판정.

---

### `getMoving`

```java
public SmartMovingSelf getMoving()
{
    return moving;
}
```

`IEntityPlayerSP.getMoving()` 구현. 외부에서 이 PlayerBase의 SmartMovingSelf 인스턴스 접근.

---

### `updateEntityActionState` / `localUpdateEntityActionState` ⭐

```java
public void updateEntityActionState()
{
    moving.tickEssential();            // isActive() 체크 없이 항상 호출

    if(!moving.isActive()) {
        localUpdateEntityActionState();
        return;
    }
    moving.updateEntityActionState(false);
}

public void localUpdateEntityActionState()
{
    super.updateEntityActionState();
}
```

**`moving.tickEssential()`**: `isActive()` 여부와 무관하게 **항상** 호출. SmartMoving이 비활성일 때도 실행되어야 하는 최소 처리(버튼 상태 업데이트 등 추정 → SmartMovingSelf.tickEssential() 리서치에서 확인 필요).

SM 활성 시 `moving.updateEntityActionState(false)` — false 파라미터의 의미는 SmartMovingSelf 리서치에서 확인.

---

### `isInsideOfMaterial` / `localIsInsideOfMaterial`

```java
public boolean isInsideOfMaterial(Material material)
{
    if(!moving.isActive()) return localIsInsideOfMaterial(material);
    return moving.isInsideOfMaterial(material);
}

public boolean localIsInsideOfMaterial(Material material)
{
    return super.isInsideOfMaterial(material);
}
```

수영/다이빙 판정에 사용. SM 활성 시 SmartMovingSelf가 물/용암 판정 조정.

---

### `writeEntityToNBT` / `localWriteEntityToNBT` ⭐

```java
public void writeEntityToNBT(NBTTagCompound nBTTagCompound)
{
    moving.writeEntityToNBT(nBTTagCompound);   // isActive() 체크 없음
}

public void localWriteEntityToNBT(NBTTagCompound nBTTagCompound)
{
    super.writeEntityToNBT(nBTTagCompound);
}
```

`moving.isActive()` 체크 없이 **항상** `moving.writeEntityToNBT()` 호출. 세이브 데이터 기록은 SM 활성 여부와 무관하게 항상 수행.

---

### `isSneaking` / `localIsSneaking`

```java
public boolean isSneaking()
{
    if(!moving.isActive()) return localIsSneaking();
    return moving.isSneaking();
}

public boolean localIsSneaking()
{
    return super.isSneaking();
}
```

SM 활성 시 크롤링·클라이밍 중 잠행 판정 조정.

---

### `getFOVMultiplier` / `localGetFOVMultiplier`

```java
public float getFOVMultiplier()
{
    if(!moving.isActive()) return localGetFOVMultiplier();
    return moving.getFOVMultiplier();
}

public float localGetFOVMultiplier()
{
    return playerAPI.localGetFOVMultiplier();   // super.getFOVMultiplier()가 아님
}
```

`localGetFOVMultiplier()` → `playerAPI.localGetFOVMultiplier()` 호출. `super.getFOVMultiplier()` 대신 PlayerAPI 내부 local 메서드 사용 — PlayerAPI 체인 내에서 "한 단계 아래" 호출 방식.

---

### `beforeSetPositionAndRotation` ⭐

```java
public void beforeSetPositionAndRotation(double d, double d1, double d2, float f, float f1)
{
    if(!moving.isActive()) return;
    moving.beforeSetPositionAndRotation();   // 파라미터 없이 호출
}
```

`setPositionAndRotation(d, d1, d2, f, f1)` 전 hook. 파라미터 5개를 받지만 `moving.beforeSetPositionAndRotation()`에는 아무 파라미터도 전달하지 않음 — SmartMovingSelf 내부에서 player 필드를 통해 위치를 직접 읽는 방식.

---

### `beforeGetSleepTimer`

```java
public void beforeGetSleepTimer()
{
    if(!moving.isActive()) return;
    moving.beforeGetSleepTimer();
}
```

수면 타이머 접근 전 hook.

---

### `jump`

```java
public void jump()
{
    if(!moving.isActive()) {
        super.jump();
        return;
    }
    moving.jump();
}
```

점프 처리. SM 활성 시 SmartMovingSelf가 완전 대체 — 다양한 점프 타입 분기.

---

## hook 메서드 전체 목록

| 메서드 | 종류 | SM 비활성 시 | SM 활성 시 |
|--------|------|-------------|-----------|
| `beforeMoveEntity(d,d1,d2)` | before | return (아무것도 안 함) | `moving.beforeMoveEntity()` |
| `afterMoveEntity(d,d1,d2)` | after | return | `moving.afterMoveEntity()` |
| `localMoveEntity(d,d1,d2)` | local | `super.moveEntity()` | (동일) |
| `beforeSleepInBedAt(i,j,k)` | before | return | `moving.beforeSleepInBedAt()` |
| `localSleepInBedAt(i,j,k)` | local | `super.sleepInBedAt()` | (동일) |
| `getBrightness(f)` | override | `localGetBrightness(f)` | `moving.getBrightness(f)` |
| `localGetBrightness(f)` | local | `super.getBrightness(f)` | (동일) |
| `getBrightnessForRender(f)` | override | `localGetBrightnessForRender(f)` | `moving.getBrightnessForRender(f)` |
| `localGetBrightnessForRender(f)` | local | `super.getBrightnessForRender(f)` | (동일) |
| `pushOutOfBlocks(d,d1,d2)` | override | `super.pushOutOfBlocks()` | `moving.pushOutOfBlocks()` |
| `beforeOnUpdate()` | before | return | `moving.beforeOnUpdate()` |
| `afterOnUpdate()` | after | return | `moving.afterOnUpdate()` |
| `beforeOnLivingUpdate()` | before | return | `moving.beforeOnLivingUpdate()` |
| `afterOnLivingUpdate()` | after | return | `moving.afterOnLivingUpdate()` |
| `getSleepingField()` | 필드 접근 | `playerAPI.getSleepingField()` | (동일) |
| `getIsInWebField()` | 필드 접근 | `playerAPI.getIsInWebField()` | (동일) |
| `setIsInWebField(b)` | 필드 설정 | `playerAPI.setIsInWebField()` | (동일) |
| `getIsJumpingField()` | 필드 접근 | `playerAPI.getIsJumpingField()` | (동일) |
| `getMcField()` | 필드 접근 | `playerAPI.getMcField()` | (동일) |
| `moveEntityWithHeading(f,f1)` | override | `super.moveEntityWithHeading()` | `moving.moveEntityWithHeading()` |
| `canTriggerWalking()` | override | `super.canTriggerWalking()` | `moving.canTriggerWalking()` |
| `isOnLadder()` | override | `super.isOnLadder()` | `moving.isOnLadderOrVine()` |
| `getMoving()` | 접근자 | `moving` 반환 | (동일) |
| `updateEntityActionState()` | override | tickEssential() 후 localUpdate | tickEssential() 후 `moving.updateEntityActionState(false)` |
| `localUpdateEntityActionState()` | local | `super.updateEntityActionState()` | (동일) |
| `setIsJumpingField(b)` | 필드 설정 | `playerAPI.setIsJumpingField()` | (동일) |
| `setMoveForwardField(f)` | 필드 설정 | `player.moveForward = f` | (동일) |
| `setMoveStrafingField(f)` | 필드 설정 | `player.moveStrafing = f` | (동일) |
| `isInsideOfMaterial(m)` | override | `localIsInsideOfMaterial(m)` | `moving.isInsideOfMaterial(m)` |
| `localIsInsideOfMaterial(m)` | local | `super.isInsideOfMaterial(m)` | (동일) |
| `writeEntityToNBT(nbt)` | override | `moving.writeEntityToNBT()` (isActive 무관) | (동일) |
| `localWriteEntityToNBT(nbt)` | local | `super.writeEntityToNBT()` | (동일) |
| `isSneaking()` | override | `localIsSneaking()` | `moving.isSneaking()` |
| `localIsSneaking()` | local | `super.isSneaking()` | (동일) |
| `getFOVMultiplier()` | override | `localGetFOVMultiplier()` | `moving.getFOVMultiplier()` |
| `localGetFOVMultiplier()` | local | `playerAPI.localGetFOVMultiplier()` | (동일) |
| `beforeSetPositionAndRotation(d,d1,d2,f,f1)` | before | return | `moving.beforeSetPositionAndRotation()` (파라미터 없음) |
| `beforeGetSleepTimer()` | before | return | `moving.beforeGetSleepTimer()` |
| `jump()` | override | `super.jump()` | `moving.jump()` |

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `ClientPlayerBase` | 상속 — `player`, `playerAPI` 필드, `super.*` 메서드들 |
| `ClientPlayerAPI` | PlayerBase 등록 |
| `IClientPlayerAPI` | `EntityPlayerSP`에서 PlayerBase 조회 |
| `IEntityPlayerSP` | 구현 인터페이스 — SmartMovingSelf가 역참조 |
| `SmartMovingSelf` | 모든 SM 로직 위임 대상, 생성 및 보유 |
| `SmartMovingInfo.ModName` | PlayerAPI 등록/조회 키 |

---

## 주요 관찰 사항

1. **`updateEntityActionState()` — `tickEssential()` 무조건 호출**: SM 비활성일 때도 `moving.tickEssential()`은 실행. 키 입력 상태 등 최소 처리를 항상 유지해야 하기 때문. 이후 isActive 체크로 SM 로직 여부 분기.

2. **`writeEntityToNBT()` — isActive 체크 없음**: 세이브 데이터 기록은 SM 활성 여부와 무관하게 항상 수행. 비활성 상태에서도 데이터 유실 방지.

3. **`isOnLadder()` → `moving.isOnLadderOrVine()`**: vanilla `isOnLadder()`에 대응하는 SM 메서드는 `isOnLadderOrVine()`. vine(덩굴) 판정이 포함됨.

4. **`beforeSetPositionAndRotation()` 파라미터 드롭**: hook 메서드는 `(d, d1, d2, f, f1)` 5개 파라미터를 받지만 `moving.beforeSetPositionAndRotation()`에는 전달 안 함. SmartMovingSelf가 `player` 필드로 직접 위치를 읽는 구조.

5. **`localGetFOVMultiplier()` → `playerAPI.localGetFOVMultiplier()`**: 다른 local 메서드들이 `super.xxx()`를 호출하는 것과 달리, FOV는 `playerAPI.localGetFOVMultiplier()` 사용. PlayerAPI 내부에서 한 단계 더 내려간 호출 방식.

6. **`setMoveForwardField` / `setMoveStrafingField`**: `playerAPI` 경유 없이 `player.moveForward`, `player.moveStrafing`에 직접 할당. 이 두 필드는 PlayerAPI가 접근자를 제공하지 않거나, 직접 접근이 더 적절한 경우.

7. **1.21.1 이식 관련**:
   - PlayerAPI 전체를 Mixin으로 대체해야 함
   - `beforeXxx` → `@Inject(at = @At("HEAD"))` Mixin
   - `afterXxx` → `@Inject(at = @At("TAIL"))` Mixin
   - `localXxx` → Mixin `@Invoker` 또는 Accessor
   - override 메서드 → `@Inject` + `CallbackInfo` / `CallbackInfoReturnable`
   - `ClientPlayerBase`의 `player`, `playerAPI` 필드 → Mixin의 `(ClientPlayerEntity) (Object) this`
   - `SmartMovingSelf moving` 첨부 → Mixin `@Unique` 필드 + 인터페이스 injection
