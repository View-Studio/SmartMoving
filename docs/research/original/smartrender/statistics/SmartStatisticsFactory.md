# SmartStatisticsFactory.java (net.smart.render.statistics) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/statistics/SmartStatisticsFactory.java  
패키지: `net.smart.render.statistics`  
종류: `class`  
상속 없음

---

## 역할

`SmartStatistics` 인스턴스의 생성·조회를 담당하는 싱글톤 팩토리.  
두 종류의 플레이어를 다르게 처리한다:
- **로컬 플레이어** (`IEntityPlayerSP` 구현체): `getStatistics()`로 직접 조회 — PlayerAPI 또는 bytecode 패치로 주입된 SmartStatistics 인스턴스
- **다른 플레이어** (`EntityOtherPlayerMP`): `Hashtable<entityId, SmartStatisticsOther>`로 자체 관리

멀티플레이어 틱마다 다른 플레이어의 통계를 갱신하고, 접속 종료한 플레이어를 마크-앤-스윕으로 정리한다.

- **실행 위치**: 클라이언트 전용

---

## 필드

```java
private static SmartStatisticsFactory factory;                    // 싱글톤 인스턴스

private Hashtable<Integer, SmartStatisticsOther> otherStatistics; // entityId → SmartStatisticsOther. lazy init
```

- `factory`: 클래스 로드 시 `null`. `initialize()` 또는 생성자 최초 호출 시 설정됨
- `otherStatistics`: `tryGetOtherStatistics()`에서 최초 접근 시 `new Hashtable<>()`으로 초기화

---

## 싱글톤 패턴

```java
public SmartStatisticsFactory()
{
    if(factory != null)
        throw new RuntimeException("FATAL: Can only create one instance of type 'StatisticsFactory'");
    factory = this;
}

protected static boolean isInitialized()
{
    return factory != null;
}

public static void initialize()
{
    if(!isInitialized())
        new SmartStatisticsFactory();
}
```

- 생성자에서 중복 생성을 `RuntimeException`으로 차단
- `initialize()`는 미초기화 상태에서만 `new SmartStatisticsFactory()` 호출
- **`protected` 인스턴스 메서드들**: 서브클래스에서 override 가능한 구조. SmartMoving이 이 클래스를 상속하여 확장할 가능성 있음 — [SmartMoving 리서치에서 확인 필요]

---

## static 공개 API

```java
public static void handleMultiPlayerTick(Minecraft minecraft)
public static SmartStatistics getInstance(EntityPlayer entityPlayer)
public static SmartStatisticsOther getOtherStatistics(int entityId)
public static SmartStatisticsOther getOtherStatistics(EntityOtherPlayerMP entity)
```

모두 `factory.doXxx(...)` 인스턴스 메서드로 위임.

---

## 메서드 전체

### `handleMultiPlayerTick(Minecraft minecraft)` — static

```java
public static void handleMultiPlayerTick(Minecraft minecraft)
{
    factory.doHandleMultiPlayerTick(minecraft);
}
```

호출 위치: `SmartStatisticsContext.onTickInGame()`에서 `theWorld.isRemote == true` 조건 시.

---

### `doHandleMultiPlayerTick(Minecraft minecraft)` — protected

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
            SmartStatisticsOther statistics = doGetOtherStatistics(otherPlayer);
            statistics.calculateAllStats(true);
            statistics.foundAlive = true;
        }
    }

    if(otherStatistics == null || otherStatistics.isEmpty())
        return;

    Iterator<Integer> entityIds = otherStatistics.keySet().iterator();
    while(entityIds.hasNext())
    {
        Integer entityId = entityIds.next();
        SmartStatisticsOther statistics = otherStatistics.get(entityId);
        if(statistics.foundAlive)
            statistics.foundAlive = false;
        else
            entityIds.remove();
    }
}
```

**Phase 1 — 살아있는 플레이어 통계 갱신:**
- `minecraft.theWorld.playerEntities` 전체 순회
- `EntityOtherPlayerMP`만 처리 (로컬 플레이어 제외)
- `doGetOtherStatistics(otherPlayer)` — 없으면 생성, 있으면 조회
- `statistics.calculateAllStats(true)` — `remote=true`이므로 `data.horizontal.apply(sp)` 생략
- `statistics.foundAlive = true` — "이번 틱에 살아있음" 마크

**Phase 2 — 접속 종료 플레이어 정리 (마크-앤-스윕):**
- `otherStatistics`가 null 또는 비어있으면 조기 반환
- `otherStatistics` 전체 키 순회
- `foundAlive == true`: `foundAlive = false`로 리셋 (다음 틱 준비)
- `foundAlive == false`: `entityIds.remove()` — Iterator를 통한 안전한 맵 항목 제거

**`SmartStatisticsOther.foundAlive`**: factory에서 직접 `statistics.foundAlive = true/false`로 접근 → public 필드.

---

### `getInstance(EntityPlayer entityPlayer)` — static

```java
public static SmartStatistics getInstance(EntityPlayer entityPlayer)
{
    return factory.doGetInstance(entityPlayer);
}
```

---

### `doGetInstance(EntityPlayer entityPlayer)` → SmartStatistics — protected

```java
protected SmartStatistics doGetInstance(EntityPlayer entityPlayer)
{
    if(entityPlayer instanceof EntityOtherPlayerMP)
        return doGetOtherStatistics(entityPlayer.getEntityId());
    else if(entityPlayer instanceof IEntityPlayerSP)
        return ((IEntityPlayerSP)entityPlayer).getStatistics();
    return null;
}
```

**분기 로직:**

| 조건 | 처리 | 반환 |
|------|------|------|
| `entityPlayer instanceof EntityOtherPlayerMP` | `doGetOtherStatistics(entityId)` | `SmartStatisticsOther` (SmartStatistics 서브클래스) |
| `entityPlayer instanceof IEntityPlayerSP` | `((IEntityPlayerSP)entityPlayer).getStatistics()` | 로컬 플레이어의 `SmartStatistics` |
| 그 외 | — | `null` |

- `IEntityPlayerSP`: bytecode 패치(PlayerAPI 또는 ASM)로 `EntityPlayerSP`에 주입된 인터페이스. `getStatistics()`로 해당 플레이어의 `SmartStatistics` 반환.
- **반환 타입**: `SmartStatistics`. `SmartStatisticsOther`는 `SmartStatistics`를 상속하므로 캐스트 없이 반환 가능.

---

### `getOtherStatistics(int entityId)` — static

```java
public static SmartStatisticsOther getOtherStatistics(int entityId)
{
    return factory.doGetOtherStatistics(entityId);
}
```

---

### `doGetOtherStatistics(int entityId)` → SmartStatisticsOther — protected

```java
protected SmartStatisticsOther doGetOtherStatistics(int entityId)
{
    SmartStatisticsOther statistics = tryGetOtherStatistics(entityId);
    if(statistics == null)
    {
        Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(entityId);
        if(entity != null && entity instanceof EntityOtherPlayerMP)
            statistics = addOtherStatistics((EntityOtherPlayerMP)entity);
    }
    return statistics;
}
```

- 맵에 없고, 월드에서 entityId로 찾은 엔티티가 `EntityOtherPlayerMP`이면 추가
- 월드에서도 찾을 수 없으면 `null` 반환

---

### `getOtherStatistics(EntityOtherPlayerMP entity)` — static

```java
public static SmartStatisticsOther getOtherStatistics(EntityOtherPlayerMP entity)
{
    return factory.doGetOtherStatistics(entity);
}
```

---

### `doGetOtherStatistics(EntityOtherPlayerMP entity)` → SmartStatisticsOther — protected

```java
protected SmartStatisticsOther doGetOtherStatistics(EntityOtherPlayerMP entity)
{
    SmartStatisticsOther statistics = tryGetOtherStatistics(entity.getEntityId());
    if(statistics == null)
        statistics = addOtherStatistics(entity);
    return statistics;
}
```

- 엔티티를 직접 받으므로 월드 조회 없이 바로 `addOtherStatistics()` — **항상 non-null 반환**

---

### `tryGetOtherStatistics(int entityId)` → SmartStatisticsOther — protected final

```java
protected final SmartStatisticsOther tryGetOtherStatistics(int entityId)
{
    if(otherStatistics == null)
        otherStatistics = new Hashtable<Integer, SmartStatisticsOther>();
    return otherStatistics.get(entityId);
}
```

- `otherStatistics` lazy init
- `Hashtable` 사용 (레거시 thread-safe 컬렉션)
- `final` — 서브클래스 override 불가

---

### `addOtherStatistics(EntityOtherPlayerMP entity)` → SmartStatisticsOther — protected final

```java
protected final SmartStatisticsOther addOtherStatistics(EntityOtherPlayerMP entity)
{
    SmartStatisticsOther statistics = new SmartStatisticsOther(entity);
    otherStatistics.put(entity.getEntityId(), statistics);
    return statistics;
}
```

- `new SmartStatisticsOther(entity)` — 엔티티를 생성자에 전달
- `final` — 서브클래스 override 불가

---

## import

```java
import java.util.*;
import net.minecraft.client.*;                  // Minecraft
import net.minecraft.client.entity.*;           // EntityOtherPlayerMP
import net.minecraft.entity.*;                  // Entity
import net.minecraft.entity.player.*;           // EntityPlayer
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartStatistics` | `doGetInstance()` 반환 타입, `SmartStatisticsOther`의 상위 타입 |
| `SmartStatisticsOther` | `otherStatistics` 맵 값 타입. `foundAlive` 필드 직접 접근 |
| `IEntityPlayerSP` | 로컬 플레이어 식별 및 `getStatistics()` 호출 |
| `SmartStatisticsContext` | `onTickInGame()`에서 `handleMultiPlayerTick()` 호출 |
| `Minecraft` (vanilla) | `getMinecraft()`, `theWorld`, `theWorld.playerEntities`, `theWorld.getEntityByID()` |
| `EntityOtherPlayerMP` (vanilla) | 다른 플레이어 타입 식별 |
| `EntityPlayer` (vanilla) | `getInstance()` 파라미터 타입, `getEntityId()` |

---

## 주요 관찰 사항

1. **`protected` 인스턴스 메서드**: `doHandleMultiPlayerTick`, `doGetInstance`, `doGetOtherStatistics` 둘 다 `protected`. SmartMoving이 이 클래스를 상속하여 `doGetInstance()` 등을 override하는 패턴으로 확장할 가능성 있음 — [SmartMoving 리서치에서 확인 필요].

2. **마크-앤-스윕 GC 패턴**: `foundAlive` 플래그로 이번 틱에 월드에 존재하는 플레이어를 마크, 마크 안 된 항목을 Iterator로 안전하게 제거. 접속 종료 플레이어의 `SmartStatisticsOther` 메모리 누수 방지.

3. **로컬 플레이어 통계 갱신 경로**: `doHandleMultiPlayerTick`은 `EntityOtherPlayerMP`만 처리. 로컬 플레이어(`EntityPlayerSP`)의 `calculateAllStats()`는 다른 경로(PlayerAPI 또는 이벤트 핸들러)에서 호출됨 — [SmartStatisticsOther 또는 이벤트 핸들러 리서치에서 확인 필요].

4. **`Hashtable` 사용**: `HashMap` 대신 thread-safe `Hashtable`. 클라이언트 전용이므로 thread-safety 필요성이 낮으나 레거시 코드 스타일.

5. **1.21.1 이식**: `EntityOtherPlayerMP` → 1.21.1 대응 타입(Yarn: `OtherClientPlayerEntity` 등 — [미확인]), `IEntityPlayerSP` 주입 → Mixin으로 대체, `playerEntities` → 1.21.1 Yarn 이름 [미확인], `Hashtable` → `HashMap`으로 교체 권장.
