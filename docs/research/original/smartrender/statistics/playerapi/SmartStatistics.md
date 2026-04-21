# SmartStatistics.java (net.smart.render.statistics.playerapi) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/statistics/playerapi/SmartStatistics.java  
패키지: `net.smart.render.statistics.playerapi`  
종류: `abstract class`  
상속 없음 (인스턴스화 방지용 abstract)

---

## 전체 소스

```java
package net.smart.render.statistics.playerapi;

import net.minecraft.client.entity.*;
import net.minecraft.entity.player.*;
import api.player.client.*;
import net.smart.render.*;
import net.smart.render.statistics.*;

public abstract class SmartStatistics
{
    public final static String ID = SmartRenderInfo.ModName;

    public static void register()
    {
        ClientPlayerAPI.register(ID, SmartStatisticsPlayerBase.class);
    }

    public static IEntityPlayerSP getPlayerBase(EntityPlayer entityPlayer)
    {
        if(entityPlayer instanceof EntityPlayerSP)
            return (SmartStatisticsPlayerBase)((IClientPlayerAPI)entityPlayer).getClientPlayerBase(ID);
        return null;
    }
}
```

---

## 역할

`net.smart.render.playerapi.SmartRender`의 statistics 버전.  
PlayerAPI의 **ClientPlayerAPI**에 `SmartStatisticsPlayerBase`를 등록하는 static helper 클래스.  
`ClientPlayerAPI`는 `EntityPlayerSP`(로컬 클라이언트 플레이어 엔티티)를 패치하는 PlayerAPI 컴포넌트이며, `RenderPlayerAPI`/`ModelPlayerAPI`와는 별도.

- **실행 위치**: 클라이언트 전용

---

## 필드

```java
public final static String ID = SmartRenderInfo.ModName;   // = "Smart Render"
```

- `net.smart.render.playerapi.SmartRender.ID`와 동일한 값
- PlayerAPI에서 이 모드의 `ClientPlayerBase`를 식별하는 키

---

## 메서드 전체

### `register()` — static

```java
public static void register()
{
    ClientPlayerAPI.register(ID, SmartStatisticsPlayerBase.class);
}
```

- `ClientPlayerAPI.register("Smart Render", SmartStatisticsPlayerBase.class)` — 클라이언트 플레이어 PlayerBase 등록
- `net.smart.render.playerapi.SmartRender.register()`가 `RenderPlayerAPI` + `ModelPlayerAPI`에 두 개를 등록하는 것과 달리, 이 쪽은 `ClientPlayerAPI` 하나에만 등록
- **호출 위치**: `SmartRenderMod` 또는 관련 초기화 코드에서 reflection으로 호출 — [SmartRenderMod 리서치 확인. SmartRenderMod.md에서 `SmartStatistics.register()` 호출 방식 미확인 — 추가 확인 필요]

---

### `getPlayerBase(EntityPlayer entityPlayer)` → IEntityPlayerSP — static

```java
public static IEntityPlayerSP getPlayerBase(EntityPlayer entityPlayer)
{
    if(entityPlayer instanceof EntityPlayerSP)
        return (SmartStatisticsPlayerBase)((IClientPlayerAPI)entityPlayer).getClientPlayerBase(ID);
    return null;
}
```

- `entityPlayer instanceof EntityPlayerSP`: 로컬 플레이어인지 확인 (`EntityPlayerSP` ≠ `EntityOtherPlayerMP`)
- 로컬 플레이어인 경우:
  - `(IClientPlayerAPI)entityPlayer` — `EntityPlayerSP`를 `IClientPlayerAPI`로 캐스트 (ClientPlayerAPI가 패치한 결과)
  - `.getClientPlayerBase(ID)` — "Smart Render" ID로 등록된 `SmartStatisticsPlayerBase` 인스턴스 조회
  - `(SmartStatisticsPlayerBase)` 캐스트 후 `IEntityPlayerSP`로 반환 (SmartStatisticsPlayerBase가 IEntityPlayerSP를 구현하므로 가능)
- 로컬 플레이어가 아니면: `null` 반환

**반환 타입이 `IEntityPlayerSP`인 이유**: `SmartStatisticsFactory.doGetInstance()`에서 `entityPlayer instanceof IEntityPlayerSP` 조건으로 접근하기 때문. 이 메서드의 반환값을 통해 `getStatistics()`를 호출.

---

## net.smart.render.playerapi.SmartRender와의 비교

| 항목 | `playerapi.SmartRender` | `statistics.playerapi.SmartStatistics` |
|------|-------------------------|----------------------------------------|
| PlayerAPI 종류 | `RenderPlayerAPI` + `ModelPlayerAPI` | `ClientPlayerAPI` |
| 등록 PlayerBase | `SmartRenderRenderPlayerBase`, `SmartRenderModelPlayerBase` | `SmartStatisticsPlayerBase` |
| 대상 vanilla 클래스 | `RenderPlayer` (렌더러), `ModelPlayer` (모델) | `EntityPlayerSP` (엔티티) |
| `getPlayerBase()` 반환 타입 | `SmartRenderRenderPlayerBase` / `SmartRenderModelPlayerBase` | `IEntityPlayerSP` |
| ID | `SmartRenderInfo.ModName` = "Smart Render" | `SmartRenderInfo.ModName` = "Smart Render" |

---

## import

```java
import net.minecraft.client.entity.*;   // EntityPlayerSP
import net.minecraft.entity.player.*;   // EntityPlayer
import api.player.client.*;             // ClientPlayerAPI, IClientPlayerAPI
import net.smart.render.*;              // SmartRenderInfo
import net.smart.render.statistics.*;  // IEntityPlayerSP, SmartStatisticsPlayerBase
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartRenderInfo.ModName` | ID = "Smart Render" |
| `ClientPlayerAPI` (PlayerAPI) | `ClientPlayerBase` 등록 |
| `IClientPlayerAPI` (PlayerAPI) | `getClientPlayerBase()` 호출을 위한 캐스트 |
| `SmartStatisticsPlayerBase` | 등록할 ClientPlayerBase 구현체 |
| `IEntityPlayerSP` | `getPlayerBase()` 반환 타입 |
| `EntityPlayerSP` (vanilla) | 로컬 플레이어 타입 판별 |
| `EntityPlayer` (vanilla) | `getPlayerBase()` 파라미터 타입 |

---

## 주요 관찰 사항

1. **`ClientPlayerAPI`**: `RenderPlayerAPI`/`ModelPlayerAPI`(렌더 계층)와 별개로 `EntityPlayerSP`(엔티티 계층)를 패치하는 PlayerAPI 컴포넌트. `SmartStatisticsPlayerBase`는 `EntityPlayerSP`의 tick/move 메서드에 끼어들어 `calculateAllStats()`를 호출하는 역할을 함 — [SmartStatisticsPlayerBase 리서치에서 확인].

2. **IEntityPlayerSP 연결**: `SmartStatisticsPlayerBase`가 `IEntityPlayerSP`를 구현 → `getPlayerBase()`가 `IEntityPlayerSP`를 반환 → `SmartStatisticsFactory.doGetInstance()`에서 `instanceof IEntityPlayerSP` 체크로 로컬 플레이어 통계 획득. 이 체인으로 `ClientPlayerAPI` 없이도 `SmartStatisticsFactory`가 `IEntityPlayerSP` 인터페이스만으로 추상화됨.

3. **1.21.1 이식**: `ClientPlayerAPI` 자체가 1.21.1에 존재하지 않음. 이 클래스 전체 불필요. `EntityPlayerSP` → `ClientPlayerEntity`(Yarn)에 Mixin `@Implements(IEntityPlayerSP.class)`로 `getStatistics()` 주입.
