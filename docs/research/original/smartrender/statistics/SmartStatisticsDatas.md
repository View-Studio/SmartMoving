# SmartStatisticsDatas.java (net.smart.render.statistics) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/statistics/SmartStatisticsDatas.java  
패키지: `net.smart.render.statistics`  
종류: `class`  
상속 없음

---

## 전체 소스

```java
package net.smart.render.statistics;

public class SmartStatisticsDatas
{
    public final SmartStatisticsData horizontal = new SmartStatisticsData();
    public final SmartStatisticsData vertical = new SmartStatisticsData();
    public final SmartStatisticsData all = new SmartStatisticsData();

    private float renderPartialTicks;

    public float getTotalHorizontalDistance()
    {
        return horizontal.getTotalDistance(renderPartialTicks);
    }

    public float getTotalVerticalDistance()
    {
        return vertical.getTotalDistance(renderPartialTicks);
    }

    public float getTotalDistance()
    {
        return all.getTotalDistance(renderPartialTicks);
    }

    public float getCurrentHorizontalSpeed()
    {
        return horizontal.getCurrentSpeed(renderPartialTicks);
    }

    public float getCurrentVerticalSpeed()
    {
        return vertical.getCurrentSpeed(renderPartialTicks);
    }

    public float getCurrentSpeed()
    {
        return all.getCurrentSpeed(renderPartialTicks);
    }

    public void setReady(float renderPartialTicks)
    {
        this.renderPartialTicks = renderPartialTicks;
    }

    public boolean isReady()
    {
        return !Float.isNaN(renderPartialTicks);
    }

    public void initialize(SmartStatisticsDatas previous)
    {
        renderPartialTicks = Float.NaN;

        horizontal.initialize(previous.horizontal);
        vertical.initialize(previous.vertical);
        all.initialize(previous.all);
    }
}
```

---

## 역할

한 링 버퍼 슬롯(한 틱) 분량의 수평/수직/전체 통계를 세 개의 `SmartStatisticsData`로 묶은 컨테이너.  
`renderPartialTicks`를 단일 지점에서 보관하고, 6개의 보간된 getter를 외부에 제공한다.  
`isReady()`/`setReady()`로 "렌더 프레임에서 보간값이 설정되었는가"를 추적한다.

- **실행 위치**: 클라이언트 전용

---

## 필드

```java
public final SmartStatisticsData horizontal = new SmartStatisticsData();  // 수평(XZ) 통계
public final SmartStatisticsData vertical   = new SmartStatisticsData();  // 수직(Y) 통계
public final SmartStatisticsData all        = new SmartStatisticsData();  // 전체 3D 통계

private float renderPartialTicks;  // Java float 기본값 0.0F. initialize() 후 NaN으로 전환됨
```

**`renderPartialTicks` 상태 전이:**

| 상태 | 값 | 의미 | `isReady()` |
|------|-----|------|-------------|
| 생성 직후 | `0.0F` (Java 기본값) | 아직 initialize() 미호출 | `true` |
| `initialize()` 후 | `Float.NaN` | 이번 틱 계산 시작, 아직 렌더 미경유 | `false` |
| `setReady(f)` 후 | `f` (0.0~1.0) | 렌더 프레임에서 보간 준비 완료 | `true` |

---

## 메서드 전체

### 보간 getter 6개

```java
public float getTotalHorizontalDistance() { return horizontal.getTotalDistance(renderPartialTicks); }
public float getTotalVerticalDistance()   { return vertical.getTotalDistance(renderPartialTicks); }
public float getTotalDistance()           { return all.getTotalDistance(renderPartialTicks); }
public float getCurrentHorizontalSpeed()  { return horizontal.getCurrentSpeed(renderPartialTicks); }
public float getCurrentVerticalSpeed()    { return vertical.getCurrentSpeed(renderPartialTicks); }
public float getCurrentSpeed()            { return all.getCurrentSpeed(renderPartialTicks); }
```

모두 `SmartStatisticsData.getTotalDistance(renderPartialTicks)` 또는 `getCurrentSpeed(renderPartialTicks)`로 위임.  
`renderPartialTicks`가 NaN인 상태(`initialize()` 후 `setReady()` 전)에서 호출하면 NaN 반환.

---

### `setReady(float renderPartialTicks)`

```java
public void setReady(float renderPartialTicks)
{
    this.renderPartialTicks = renderPartialTicks;
}
```

- 렌더 프레임에서 보간 계수를 주입
- 호출 위치: `SmartStatistics.get(float renderPartialTicks)` → `data.setReady(renderPartialTicks)`

---

### `isReady()` → boolean

```java
public boolean isReady()
{
    return !Float.isNaN(renderPartialTicks);
}
```

- `NaN`이면 `false` (= `initialize()` 후 아직 `setReady()` 미호출)
- NaN 이외의 값이면 `true`
- **사용처**: `SmartStatistics.getCurrentXxxSpeedFlattened()` 링 버퍼 순회 시 `!data.isReady()`이면 break

---

### `initialize(SmartStatisticsDatas previous)`

```java
public void initialize(SmartStatisticsDatas previous)
{
    renderPartialTicks = Float.NaN;

    horizontal.initialize(previous.horizontal);
    vertical.initialize(previous.vertical);
    all.initialize(previous.all);
}
```

- `renderPartialTicks = Float.NaN` — "not ready" 상태로 초기화 → `isReady() = false`
- `horizontal/vertical/all` 각각 이전 슬롯의 대응 `SmartStatisticsData`를 인수로 `initialize()` 호출
- **호출 위치**: `SmartStatistics.calculateAllStats()` 내 링 버퍼 슬롯 재사용 시

---

## 전체 데이터 흐름 (틱 → 렌더)

```
[틱 처리]
SmartStatistics.calculateAllStats()
  └─ data.initialize(previous)          // renderPartialTicks = NaN, 이전값 복사
  └─ data.horizontal.calcualte(xzDist)  // horizontal EMA 갱신
  └─ data.vertical.calcualte(|yDist|)   // vertical EMA 갱신
  └─ data.all.calcualte(3dDist)         // all EMA 갱신
  └─ data.horizontal.apply(sp)          // [조건부] vanilla limbSwing* 덮어쓰기

[렌더 처리]
SmartStatistics.get(renderPartialTicks)
  └─ data.setReady(renderPartialTicks)  // renderPartialTicks 주입, isReady() = true
  └─ data.getCurrentHorizontalSpeed()  // horizontal.getCurrentSpeed(renderPartialTicks)
     등 getter 사용
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartStatisticsData` | `.horizontal`, `.vertical`, `.all` 필드 타입 |
| `SmartStatistics` | 링 버퍼(`datas[]`)의 슬롯 타입으로 사용. `initialize()`, `setReady()`, `isReady()`, getter 호출 |

---

## 주요 관찰 사항

1. **`renderPartialTicks`의 NaN 센티넬**: `initialize()`에서 `Float.NaN`으로 설정하여 "이번 틱 계산은 완료됐지만 렌더 보간값 미설정" 상태를 표현. `isReady()` = `!Float.isNaN()`으로 이를 검사.

2. **`dummy` 객체의 `isReady()` 주의**: `SmartStatistics`의 `dummy = new SmartStatisticsDatas()`는 Java float 기본값 `0.0F`로 생성되므로 `isReady() = true`. 링 버퍼 순회는 `datas[]`만 접근하므로 `dummy`가 순회에 포함되지는 않음.

3. **getter의 `renderPartialTicks` 공유**: 세 축(horizontal/vertical/all) 모두 하나의 `renderPartialTicks` 값을 공유. `setReady()` 한 번으로 세 getter 모두 동일한 보간 계수를 사용.

4. **1.21.1 이식**: 구조 자체는 vanilla API와 독립적이므로 클래스 내부 로직은 그대로 이식 가능. `SmartStatisticsData`의 `apply(EntityPlayer)` 및 이 클래스를 사용하는 코드의 vanilla 필드명만 교체하면 됨.
