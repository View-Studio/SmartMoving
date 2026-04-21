# SmartStatisticsFactory.java (net.smart.render.statistics.playerapi) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/statistics/playerapi/SmartStatisticsFactory.java  
패키지: `net.smart.render.statistics.playerapi`  
종류: `class`  
상속: `extends net.smart.render.statistics.SmartStatisticsFactory`

---

## 전체 소스

```java
package net.smart.render.statistics.playerapi;

import net.minecraft.entity.player.*;
import net.smart.render.statistics.*;

public class SmartStatisticsFactory extends net.smart.render.statistics.SmartStatisticsFactory
{
    public static void initialize()
    {
        if(!isInitialized())
            new SmartStatisticsFactory();
    }

    @Override
    protected net.smart.render.statistics.SmartStatistics doGetInstance(EntityPlayer entityPlayer)
    {
        net.smart.render.statistics.SmartStatistics statistics = super.doGetInstance(entityPlayer);
        if(statistics != null)
            return statistics;

        IEntityPlayerSP playerBase = SmartStatistics.getPlayerBase(entityPlayer);
        if(playerBase != null)
            return playerBase.getStatistics();

        return null;
    }
}
```

---

## 역할

`net.smart.render.statistics.SmartStatisticsFactory`의 PlayerAPI 확장 서브클래스.  
기본 팩토리가 처리하지 못한 플레이어(비-PlayerAPI 경로로 `IEntityPlayerSP`를 구현하지 않은 경우)에 대해, `ClientPlayerAPI`를 통해 `SmartStatisticsPlayerBase`를 조회하는 경로를 추가한다.

- **실행 위치**: 클라이언트 전용

---

## 메서드 전체

### `initialize()` — static

```java
public static void initialize()
{
    if(!isInitialized())
        new SmartStatisticsFactory();
}
```

- 부모 클래스의 `initialize()`와 동일한 코드처럼 보이지만, **`new SmartStatisticsFactory()`가 서브클래스 인스턴스**를 생성
- 부모 생성자에서 `factory = this`로 설정되므로, `factory`에는 이 서브클래스 인스턴스가 저장됨
- PlayerAPI가 있는 환경에서는 이 `initialize()`를 호출해야 `doGetInstance()` override가 동작

**호출 위치**: SmartRenderMod 또는 statistics PlayerAPI 초기화 코드 — [SmartRenderMod 리서치에서 확인 필요]

---

### `doGetInstance(EntityPlayer entityPlayer)` → SmartStatistics — protected override

```java
@Override
protected net.smart.render.statistics.SmartStatistics doGetInstance(EntityPlayer entityPlayer)
{
    net.smart.render.statistics.SmartStatistics statistics = super.doGetInstance(entityPlayer);
    if(statistics != null)
        return statistics;

    IEntityPlayerSP playerBase = SmartStatistics.getPlayerBase(entityPlayer);
    if(playerBase != null)
        return playerBase.getStatistics();

    return null;
}
```

**2단계 조회 로직:**

**Step 1 — 기본 경로 시도:**
```java
super.doGetInstance(entityPlayer)
```
`net.smart.render.statistics.SmartStatisticsFactory.doGetInstance()`:
- `EntityOtherPlayerMP` → `SmartStatisticsOther` 반환
- `IEntityPlayerSP` 구현체 → `IEntityPlayerSP.getStatistics()` 반환
- 그 외 → `null`

**Step 2 — PlayerAPI 경로 시도 (Step 1이 null인 경우에만):**
```java
IEntityPlayerSP playerBase = SmartStatistics.getPlayerBase(entityPlayer);
```
`net.smart.render.statistics.playerapi.SmartStatistics.getPlayerBase(entityPlayer)`:
- `entityPlayer instanceof EntityPlayerSP`이면 `IClientPlayerAPI.getClientPlayerBase("Smart Render")`로 `SmartStatisticsPlayerBase` 조회 → `IEntityPlayerSP`로 반환
- 아니면 `null`

```java
if(playerBase != null)
    return playerBase.getStatistics();
```
- `IEntityPlayerSP.getStatistics()` — `SmartStatisticsPlayerBase`가 보유한 `SmartStatistics` 인스턴스 반환

**Step 2가 필요한 이유**: PlayerAPI가 설치된 환경에서 `EntityPlayerSP`가 `IEntityPlayerSP`를 직접 구현하지 않을 수 있음. 대신 `SmartStatisticsPlayerBase`(ClientPlayerBase)가 `IEntityPlayerSP`를 구현하며 `SmartStatistics` 인스턴스를 보관.

---

## 비-PlayerAPI 경로 vs PlayerAPI 경로 비교

| 경로 | `EntityPlayerSP` 상태 | 조회 방법 |
|------|----------------------|-----------|
| 비-PlayerAPI | bytecode 패치로 `IEntityPlayerSP` 직접 구현 | `super.doGetInstance()` — `instanceof IEntityPlayerSP` 캐스트 |
| PlayerAPI | ClientPlayerAPI 패치. `IEntityPlayerSP`는 `SmartStatisticsPlayerBase`가 구현 | Step 2 — `SmartStatistics.getPlayerBase()` → `IClientPlayerAPI.getClientPlayerBase()` |

두 경로 모두 최종적으로 `IEntityPlayerSP.getStatistics()`를 통해 같은 `net.smart.render.statistics.SmartStatistics` 인스턴스를 반환한다.

---

## import

```java
import net.minecraft.entity.player.*;           // EntityPlayer
import net.smart.render.statistics.*;           // IEntityPlayerSP, net.smart.render.statistics.SmartStatisticsFactory
```

`net.smart.render.statistics.playerapi.SmartStatistics`는 동일 패키지이므로 import 없이 사용.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `net.smart.render.statistics.SmartStatisticsFactory` | 상위 클래스. `doGetInstance()` super 호출, `isInitialized()`, 싱글톤 `factory` |
| `net.smart.render.statistics.playerapi.SmartStatistics` | `getPlayerBase()` — PlayerAPI 경로 조회 |
| `IEntityPlayerSP` | `getStatistics()` 호출 인터페이스 |
| `EntityPlayer` (vanilla) | `doGetInstance()` 파라미터 타입 |

---

## 주요 관찰 사항

1. **`initialize()` override 핵심**: 이름은 같지만 `new SmartStatisticsFactory()`가 서브클래스 인스턴스를 생성하므로, PlayerAPI 환경에서 이쪽 `initialize()`를 호출하면 `factory`에 서브클래스가 설정되어 `doGetInstance()` override가 활성화됨.

2. **super 우선 원칙**: Step 1에서 기본 경로(`EntityOtherPlayerMP` 또는 직접 `IEntityPlayerSP`)를 먼저 시도. 성공하면 PlayerAPI 경로는 건드리지 않음. PlayerAPI 경로는 반드시 fallback.

3. **완전한 추상화**: 호출 측(`SmartStatisticsFactory.getInstance(entityPlayer)`)은 PlayerAPI 유무와 무관하게 동일한 static 메서드를 호출. 내부에서 팩토리 서브클래스가 두 경로를 투명하게 처리.

4. **1.21.1 이식**: PlayerAPI가 없으므로 이 클래스 전체 불필요. Mixin으로 `ClientPlayerEntity`에 `IEntityPlayerSP` 직접 구현 후 base factory 경로만 사용하면 됨.
