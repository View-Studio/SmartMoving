# SmartStatisticsData.java (net.smart.render.statistics) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/statistics/SmartStatisticsData.java  
패키지: `net.smart.render.statistics`  
종류: `class`  
상속 없음

---

## 전체 소스

```java
package net.smart.render.statistics;

import net.minecraft.entity.player.*;

public class SmartStatisticsData
{
    public float prevLegYaw;
    public float legYaw;
    public float total;

    public float getCurrentSpeed(float renderPartialTicks)
    {
        return Math.min(1.0F, prevLegYaw + (legYaw - prevLegYaw) * renderPartialTicks);
    }

    public float getTotalDistance(float renderPartialTicks)
    {
        return total - legYaw * (1.0F - renderPartialTicks);
    }

    public void initialize(SmartStatisticsData previous)
    {
        prevLegYaw = previous.legYaw;
        legYaw = previous.legYaw;
        total = previous.total;
    }

    public float calcualte(float distance)
    {
        distance = distance * 4F;

        legYaw += (distance - legYaw) * 0.4F;
        total += legYaw;

        return distance;
    }

    public void apply(EntityPlayer sp)
    {
        sp.prevLimbSwingAmount = prevLegYaw;
        sp.limbSwingAmount = legYaw;
        sp.limbSwing = total;
    }
}
```

---

## 역할

한 축(수평/수직/전체)의 이동 통계를 한 슬롯(한 틱) 분량으로 보관하는 데이터 클래스.  
`SmartStatisticsDatas` 내 `.horizontal`, `.vertical`, `.all` 필드로 사용된다.  
vanilla `EntityPlayer`의 `limbSwing*` 필드 구조를 모방한 필드셋(`prevLegYaw`, `legYaw`, `total`)과 지수 이동 평균(EMA) 계산을 제공한다.

- **실행 위치**: 클라이언트 전용

---

## 필드

```java
public float prevLegYaw;   // 이전 틱의 legYaw 값 (= vanilla prevLimbSwingAmount에 대응)
public float legYaw;       // EMA 처리된 현재 속도 (= vanilla limbSwingAmount에 대응)
public float total;        // legYaw의 누적 합계 (= vanilla limbSwing에 대응)
```

**이름 주의**: `legYaw`는 각도(yaw)가 아닌 속도(speed)를 담는다. vanilla `limbSwingAmount`(팔다리 흔들림 진폭)에 대응하는 필드명을 그대로 가져온 것.

---

## 메서드 전체

### `calcualte(float distance)` → float

```java
public float calcualte(float distance)
{
    distance = distance * 4F;

    legYaw += (distance - legYaw) * 0.4F;
    total += legYaw;

    return distance;
}
```

**(메서드명 오타: `calcualte` — 원본 그대로)**

**단계별 계산:**
1. `distance *= 4F` — 입력 거리를 4배 스케일
2. `legYaw += (distance - legYaw) * 0.4F` — 지수 이동 평균(EMA):
   - 평활 계수(smoothing factor): `0.4F`
   - 공식: `legYaw = legYaw * 0.6F + distance * 0.4F`
3. `total += legYaw` — EMA 처리된 값 누적
4. 반환값: `distance` (4배 스케일된 원시 거리)

**반환값 사용처**: `SmartStatistics.calculateAllStats()`에서 `data.all.calcualte(...)` 반환값을 `tickDistance`에 저장.

**수치 상수:**

| 값 | 위치 | 의미 |
|----|------|------|
| `4F` | `distance * 4F` | 입력 거리 스케일 계수 |
| `0.4F` | EMA 평활 계수 | 새 값 반영 비율 (이전값 유지 비율: 0.6F) |

---

### `initialize(SmartStatisticsData previous)`

```java
public void initialize(SmartStatisticsData previous)
{
    prevLegYaw = previous.legYaw;
    legYaw = previous.legYaw;
    total = previous.total;
}
```

- `prevLegYaw = previous.legYaw` — 이전 슬롯의 legYaw를 prev로 복사
- `legYaw = previous.legYaw` — legYaw도 이전 슬롯 값으로 초기화 (새 틱 시작점)
- `total = previous.total` — 누적값 이어받음
- **`calcualte()` 이전에 호출됨** — initialize로 prev를 설정한 뒤 calcualte로 legYaw/total 갱신

---

### `getCurrentSpeed(float renderPartialTicks)` → float

```java
public float getCurrentSpeed(float renderPartialTicks)
{
    return Math.min(1.0F, prevLegYaw + (legYaw - prevLegYaw) * renderPartialTicks);
}
```

- `prevLegYaw`와 `legYaw` 사이를 `renderPartialTicks`로 선형 보간
- 결과를 `1.0F`로 상한 클램프
- **`SmartStatisticsDatas.getCurrentHorizontalSpeed()` 등에서 호출** (SmartStatisticsDatas 리서치에서 확인 필요)

---

### `getTotalDistance(float renderPartialTicks)` → float

```java
public float getTotalDistance(float renderPartialTicks)
{
    return total - legYaw * (1.0F - renderPartialTicks);
}
```

- `total`은 이미 이번 틱의 `legYaw`가 더해진 상태
- 아직 경과하지 않은 부분(`legYaw * (1.0F - renderPartialTicks)`)을 빼서 보간
- `renderPartialTicks = 1.0F` → `total` 그대로
- `renderPartialTicks = 0.0F` → `total - legYaw` (이번 틱 기여분 전체 제거)

---

### `apply(EntityPlayer sp)`

```java
public void apply(EntityPlayer sp)
{
    sp.prevLimbSwingAmount = prevLegYaw;
    sp.limbSwingAmount = legYaw;
    sp.limbSwing = total;
}
```

- vanilla `EntityPlayer`의 세 걷기 애니메이션 필드를 SmartStatistics 계산값으로 덮어씀
- **`SmartStatistics.calculateAllStats()`에서 수평(horizontal) 데이터에만 호출**: `data.horizontal.apply(sp)` (조건: `calculateHorizontalStats && !remote`)
- 이 호출 이후 `SmartStatistics.getHorizontalPrevLegYaw()` 등의 getter는 SmartStatistics가 기록한 값을 vanilla 필드에서 읽어오는 구조

**vanilla 필드 대응:**

| SmartStatisticsData 필드 | vanilla EntityPlayer 필드 |
|--------------------------|--------------------------|
| `prevLegYaw` | `prevLimbSwingAmount` |
| `legYaw` | `limbSwingAmount` |
| `total` | `limbSwing` |

---

## import

```java
import net.minecraft.entity.player.*;   // EntityPlayer
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartStatisticsDatas` | 이 클래스의 인스턴스를 `.horizontal`, `.vertical`, `.all` 필드로 보관 |
| `SmartStatistics` | `calcualte()`, `initialize()`, `apply()` 호출 |
| `EntityPlayer` (vanilla) | `apply()`에서 `limbSwing*` 필드 덮어쓰기 |

---

## 주요 관찰 사항

1. **EMA 평활 계수 0.4F**: 매 틱 입력 거리의 40%만 legYaw에 반영. 급격한 속도 변화를 완화하는 저역 필터. vanilla에서는 `limbSwingAmount`가 `0.4F` 계수로 갱신되는 동일한 패턴이 있음.

2. **`distance * 4F`**: vanilla에서 `limbSwingAmount` 업데이트 시 이동 거리에 4를 곱하는 것과 동일한 스케일링.

3. **`apply()` 오버라이드 구조**: SmartStatistics가 horizontal 통계를 계산한 뒤 vanilla `limbSwing*` 필드를 덮어써서, 이후 vanilla 렌더 코드가 SmartStatistics 값을 그대로 사용하도록 함. vertical/all 통계는 vanilla 필드를 덮어쓰지 않고 SmartStatisticsDatas에서만 보관.

4. **`initialize()`에서 `legYaw = previous.legYaw`**: 새 슬롯의 시작점을 이전 슬롯의 현재값으로 맞춤. 이후 `calcualte()`가 `prevLegYaw`(= 이전 legYaw)와 새 `legYaw` 사이의 보간 기준을 확립.

5. **1.21.1 이식**: `EntityPlayer` → `PlayerEntity`(Yarn), `prevLimbSwingAmount`/`limbSwingAmount`/`limbSwing` → 1.21.1 Yarn 이름으로 교체 필요 [미확인].
