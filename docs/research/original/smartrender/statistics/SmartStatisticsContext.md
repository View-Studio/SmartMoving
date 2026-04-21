# SmartStatisticsContext.java (net.smart.render.statistics) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/statistics/SmartStatisticsContext.java  
패키지: `net.smart.render.statistics`  
종류: `abstract class`

---

## 전체 소스

```java
package net.smart.render.statistics;

import net.minecraft.client.*;

public abstract class SmartStatisticsContext
{
    protected static boolean calculateHorizontalStats = false;

    public static void setCalculateHorizontalStats(boolean flag)
    {
        calculateHorizontalStats = flag;
    }

    public static void onTickInGame()
    {
        Minecraft minecraft = Minecraft.getMinecraft();

        if(minecraft.theWorld != null && minecraft.theWorld.isRemote)
            SmartStatisticsFactory.handleMultiPlayerTick(minecraft);
    }
}
```

---

## 역할

`SmartStatistics`의 상위 클래스. 두 가지 역할:
1. `calculateHorizontalStats` static 플래그 보관 및 setter 제공
2. 게임 틱마다 호출되는 `onTickInGame()` — 멀티플레이어 환경에서만 `SmartStatisticsFactory.handleMultiPlayerTick()` 트리거

- **실행 위치**: 클라이언트 전용 (`Minecraft.getMinecraft()` 사용)

---

## 필드

```java
protected static boolean calculateHorizontalStats = false;
```

- `static` — 모든 `SmartStatisticsContext` 서브클래스(`SmartStatistics` 포함)가 공유하는 단일 값
- `protected` — `SmartStatistics`에서 `calculateHorizontalStats` 이름으로 직접 접근
- 기본값 `false`
- `setCalculateHorizontalStats(true)` 호출 전까지 `SmartStatistics.calculateAllStats()` 내 `data.horizontal.apply(sp)`가 실행되지 않음

---

## 메서드 전체

### `setCalculateHorizontalStats(boolean flag)` — static

```java
public static void setCalculateHorizontalStats(boolean flag)
{
    calculateHorizontalStats = flag;
}
```

- `calculateHorizontalStats` 플래그를 외부에서 설정하는 단일 진입점
- 호출 위치: `SmartStatisticsFactory` 또는 SmartRender 초기화 코드에서 — [SmartStatisticsFactory 리서치에서 확인 필요]

---

### `onTickInGame()` — static

```java
public static void onTickInGame()
{
    Minecraft minecraft = Minecraft.getMinecraft();

    if(minecraft.theWorld != null && minecraft.theWorld.isRemote)
        SmartStatisticsFactory.handleMultiPlayerTick(minecraft);
}
```

- `minecraft.theWorld != null`: 월드가 로드된 상태인지 확인
- `minecraft.theWorld.isRemote`: 클라이언트가 원격 서버에 연결된 멀티플레이어 상태 (`true`) — 싱글플레이어 통합 서버 월드는 `false`
- 조건 만족 시에만 `SmartStatisticsFactory.handleMultiPlayerTick(minecraft)` 호출
- **호출 위치**: SmartRender의 FML 틱 이벤트 핸들러에서 매 틱 호출 — [SmartStatisticsOther 또는 이벤트 핸들러 리서치에서 확인 필요]

---

## import

```java
import net.minecraft.client.*;   // Minecraft
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartStatistics` | 서브클래스. `calculateHorizontalStats`를 `protected` 접근으로 읽음 |
| `SmartStatisticsFactory` | `handleMultiPlayerTick(Minecraft)` 호출 |
| `Minecraft` (vanilla) | `getMinecraft()`, `theWorld`, `theWorld.isRemote` |

---

## 주요 관찰 사항

1. **`calculateHorizontalStats`의 역할**: `SmartStatistics.calculateAllStats()` 내 `data.horizontal.apply(sp)` 실행 여부를 제어. 기본 `false`이므로, 이 플래그가 `true`로 설정되기 전까지 수평 통계가 vanilla `EntityPlayer` 필드에 기록되지 않음.

2. **`onTickInGame()` 멀티플레이어 전용**: `theWorld.isRemote == true` 조건으로 멀티플레이어에서만 `handleMultiPlayerTick`을 호출. 싱글플레이어(`isRemote == false`)에서는 호출하지 않음 — 싱글플레이어에서는 다른 경로로 통계가 처리될 것으로 보임 [SmartStatisticsFactory 리서치에서 확인 필요].

3. **1.21.1 이식**: `Minecraft.getMinecraft()` → `MinecraftClient.getInstance()`. `theWorld.isRemote` → `client.world != null && client.isIntegratedServerRunning() == false` 또는 동등한 조건으로 교체 필요 [Yarn 이름 미확인].
