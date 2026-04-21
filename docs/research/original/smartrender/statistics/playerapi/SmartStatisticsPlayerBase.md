# SmartStatisticsPlayerBase.java (net.smart.render.statistics.playerapi) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/statistics/playerapi/SmartStatisticsPlayerBase.java  
패키지: `net.smart.render.statistics.playerapi`  
종류: `class`  
상속/구현: `SmartStatisticsPlayerBase extends ClientPlayerBase implements IEntityPlayerSP`

---

## 전체 소스

```java
package net.smart.render.statistics.playerapi;

import api.player.client.*;
import net.smart.render.statistics.*;

public class SmartStatisticsPlayerBase extends ClientPlayerBase implements IEntityPlayerSP
{
    public SmartStatisticsPlayerBase(ClientPlayerAPI playerApi)
    {
        super(playerApi);
        statistics = new net.smart.render.statistics.SmartStatistics(player);
    }

    @Override
    public void afterMoveEntityWithHeading(float f, float f1)
    {
        statistics.calculateAllStats(false);
    }

    @Override
    public void afterUpdateRidden()
    {
        statistics.calculateRiddenStats();
    }

    @Override
    public net.smart.render.statistics.SmartStatistics getStatistics()
    {
        return statistics;
    }

    public net.smart.render.statistics.SmartStatistics statistics;
}
```

---

## 역할

PlayerAPI의 `ClientPlayerBase`를 구현하여 `EntityPlayerSP`의 이동 메서드 이후 통계를 수집하는 클래스.  
`IEntityPlayerSP`를 구현하여 `SmartStatisticsFactory`가 통계 인스턴스에 접근할 수 있게 한다.

- **실행 위치**: 클라이언트 전용 (`ClientPlayerBase` = `EntityPlayerSP` 계층)

---

## 필드

```java
public net.smart.render.statistics.SmartStatistics statistics;
```

- `public` — 외부에서 직접 접근 가능
- 생성자에서 `new SmartStatistics(player)`로 초기화

---

## 생성자

```java
public SmartStatisticsPlayerBase(ClientPlayerAPI playerApi)
{
    super(playerApi);
    statistics = new net.smart.render.statistics.SmartStatistics(player);
}
```

- `super(playerApi)`: `ClientPlayerBase` 생성자 호출 → `player` 필드(= `EntityPlayerSP` 인스턴스) 초기화
- `statistics = new SmartStatistics(player)`: `player`(EntityPlayerSP, EntityPlayer 서브클래스)를 인수로 통계 인스턴스 생성

---

## 메서드 전체

### `afterMoveEntityWithHeading(float f, float f1)` — override

```java
@Override
public void afterMoveEntityWithHeading(float f, float f1)
{
    statistics.calculateAllStats(false);
}
```

- **PlayerAPI `after*` 패턴**: vanilla `EntityPlayerSP.moveEntityWithHeading(float, float)` 실행 **후** 호출
- `calculateAllStats(false)`: `remote = false` → 로컬 플레이어이므로 `calculateHorizontalStats` 조건 시 `data.horizontal.apply(sp)` 실행
- 파라미터 `f`, `f1`(`strafe`, `forward` — moveEntityWithHeading의 인수)은 이 메서드에서 사용하지 않음

---

### `afterUpdateRidden()` — override

```java
@Override
public void afterUpdateRidden()
{
    statistics.calculateRiddenStats();
}
```

- **PlayerAPI `after*` 패턴**: vanilla `EntityPlayerSP.updateRidden()` 실행 **후** 호출
- `calculateRiddenStats()`: `ticksRiding++` 만 실행

---

### `getStatistics()` → SmartStatistics — override (IEntityPlayerSP 구현)

```java
@Override
public net.smart.render.statistics.SmartStatistics getStatistics()
{
    return statistics;
}
```

- `IEntityPlayerSP` 인터페이스 구현
- `SmartStatisticsFactory.doGetInstance()` → `IEntityPlayerSP.getStatistics()`로 호출됨

---

## 통계 수집 흐름

```
[매 틱 — 이동 시]
EntityPlayerSP.moveEntityWithHeading(strafe, forward)   ← vanilla 실행
  └─ (PlayerAPI hook) afterMoveEntityWithHeading(f, f1)
       └─ statistics.calculateAllStats(false)
            └─ 링 버퍼 갱신 + horizontal.apply(sp) (calculateHorizontalStats=true 시)

[매 틱 — 탑승 중]
EntityPlayerSP.updateRidden()                            ← vanilla 실행
  └─ (PlayerAPI hook) afterUpdateRidden()
       └─ statistics.calculateRiddenStats()
            └─ ticksRiding++
```

---

## import

```java
import api.player.client.*;             // ClientPlayerBase, ClientPlayerAPI
import net.smart.render.statistics.*;  // IEntityPlayerSP, net.smart.render.statistics.SmartStatistics
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `ClientPlayerBase` (PlayerAPI) | 상위 클래스. `player`(EntityPlayerSP) 필드 제공 |
| `ClientPlayerAPI` (PlayerAPI) | 생성자 파라미터 |
| `IEntityPlayerSP` | 구현 인터페이스. `getStatistics()` 메서드 정의 |
| `net.smart.render.statistics.SmartStatistics` | 실제 통계 보관/계산 인스턴스 |

---

## 주요 관찰 사항

1. **`afterMoveEntityWithHeading` vs `afterUpdateRidden`**: 이동과 탑승을 별도 훅으로 구분. 이동 시 거리 통계(`calculateAllStats`) 계산, 탑승 시 탑승 틱(`ticksRiding`) 증가.

2. **`remote = false`**: `calculateAllStats(false)`이므로 로컬 플레이어 경로. `calculateHorizontalStats == true`이면 `data.horizontal.apply(sp)`로 vanilla `limbSwing*` 필드를 SmartStatistics 값으로 덮어씀.

3. **`statistics` 필드가 `public`**: `SmartStatisticsFactory` 등이 직접 접근할 수 있도록. 단, 외부 접근은 `getStatistics()`를 통하는 것이 일반 경로.

4. **비-PlayerAPI 경로**: PlayerAPI 없이 bytecode 패치로 `EntityPlayerSP`에 직접 `IEntityPlayerSP`를 구현하는 경우, 이 클래스 없이 `EntityPlayerSP` 자체가 `getStatistics()`를 제공. 그 경로의 `after*` 훅은 다른 방법(ASM 등)으로 주입 — [해당 패치 코드 리서치 필요].

5. **1.21.1 이식**: `ClientPlayerAPI` 없으므로 이 클래스 불필요. Mixin으로 `ClientPlayerEntity`에 `afterMoveEntityWithHeading`/`afterUpdateRidden`에 해당하는 지점에 `@Inject`하고, `IEntityPlayerSP.getStatistics()`를 `@Implements`로 주입.
