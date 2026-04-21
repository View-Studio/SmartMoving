# SmartStatistics.java (net.smart.render.statistics) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/statistics/SmartStatistics.java  
패키지: `net.smart.render.statistics`  
종류: `class`  
상속: `SmartStatistics extends SmartStatisticsContext`

---

## 역할

플레이어의 매 틱 이동 거리(수평/수직/전체)를 계산하고, 최근 10틱의 링 버퍼(ring buffer)에 보관하는 통계 클래스.  
렌더 시점에 `renderPartialTicks`를 적용한 보간값을 `SmartRenderModel`의 animate* 메서드에 공급한다.

- **실행 위치**: 클라이언트 전용

---

## 필드

```java
private final EntityPlayer sp;                          // 대상 플레이어 (생성자에서 주입)
private float tickDistance;                             // 이번 틱의 전체 3D 이동 거리 (calcualte 반환값)

public int ticksRiding;                                 // 탑승 틱 누적 카운터. calculateRiddenStats()마다 +1
public float prevHorizontalAngle = Float.NaN;           // [이 파일에서 직접 사용하는 코드 없음. 외부에서 읽기용]

private final static SmartStatisticsDatas dummy = new SmartStatisticsDatas();  // currentDataIndex == -1일 때 반환용 더미
private final SmartStatisticsDatas[] datas = new SmartStatisticsDatas[10];     // 링 버퍼, 크기 10
private int currentDataIndex = -1;                      // 현재 링 버퍼 인덱스. 초기값 -1 (데이터 없음)
```

**`calculateHorizontalStats`**: `SmartStatisticsContext`에서 상속. 이 파일에서 직접 선언하지 않음.

---

## 생성자

```java
public SmartStatistics(EntityPlayer sp)
{
    this.sp = sp;
}
```

---

## 메서드 전체

### `calculateAllStats(boolean remote)`

```java
public void calculateAllStats(boolean remote)
{
    double diffX = sp.posX - sp.prevPosX;
    double diffY = sp.posY - sp.prevPosY;
    double diffZ = sp.posZ - sp.prevPosZ;

    SmartStatisticsDatas previous = get();

    currentDataIndex++;
    if(currentDataIndex >= datas.length)
        currentDataIndex = 0;

    SmartStatisticsDatas data = datas[currentDataIndex];
    if(data == null)
        data = datas[currentDataIndex] = new SmartStatisticsDatas();
    data.initialize(previous);

    data.horizontal.calcualte(MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ));
    data.vertical.calcualte((float)Math.abs(diffY));
    tickDistance = data.all.calcualte(MathHelper.sqrt_double(diffX * diffX + diffY * diffY + diffZ * diffZ));

    if(calculateHorizontalStats && !remote)
        data.horizontal.apply(sp);
}
```

**계산 내용:**

| 필드 | 계산식 | 의미 |
|------|--------|------|
| `data.horizontal` | `sqrt(diffX² + diffZ²)` | XZ 평면 수평 이동 거리 |
| `data.vertical` | `abs(diffY)` | Y축 수직 이동 거리 (절댓값) |
| `tickDistance` | `sqrt(diffX² + diffY² + diffZ²)` | 전체 3D 이동 거리 |

**링 버퍼 관리:**
- `currentDataIndex++` 후 `>= 10`이면 0으로 래핑
- 해당 슬롯이 `null`이면 새 `SmartStatisticsDatas` 생성 후 저장
- `data.initialize(previous)` — 이전 슬롯 데이터를 prev 상태로 복사

**`calculateHorizontalStats && !remote` 조건:**
- `calculateHorizontalStats`: `SmartStatisticsContext`에서 상속된 필드
- `!remote`: 원격 플레이어(다른 플레이어)는 horizontal apply 생략
- 만족 시: `data.horizontal.apply(sp)` — 수평 통계를 `sp`(EntityPlayer) 필드에 적용

**주의: 메서드명 오타**: `calcualte` (calculate의 오타) — 원본 소스 그대로.  
**주의**: `MathHelper.sqrt_double` — vanilla 1.7.10 MathHelper의 double 인수 sqrt. `Math.sqrt`가 아님.

---

### `calculateRiddenStats()`

```java
public void calculateRiddenStats()
{
    ticksRiding++;
}
```

- 탑승 중인 틱마다 외부에서 호출. `ticksRiding` 카운터만 증가.

---

### getter — vanilla EntityPlayer 필드 직접 참조 (수평)

```java
public float getHorizontalPrevLegYaw() { return sp.prevLimbSwingAmount; }
public float getHorizontalLegYaw()     { return sp.limbSwingAmount; }
public float getHorizontalTotal()      { return sp.limbSwing; }
```

| SmartStatistics getter | vanilla EntityPlayer 필드 | 의미 |
|------------------------|--------------------------|------|
| `getHorizontalPrevLegYaw()` | `sp.prevLimbSwingAmount` | 이전 프레임 팔다리 흔들림 진폭 |
| `getHorizontalLegYaw()` | `sp.limbSwingAmount` | 현재 팔다리 흔들림 진폭 |
| `getHorizontalTotal()` | `sp.limbSwing` | 팔다리 흔들림 누적 합계 |

수평 통계는 SmartStatistics가 직접 계산하는 것이 아니라, vanilla `EntityPlayer`의 걷기 애니메이션 필드를 그대로 읽는다.  
(`data.horizontal.apply(sp)`에서 이 필드들을 덮어쓰는 경우도 있음 — SmartStatisticsData 리서치에서 확인 필요.)

---

### getter — 현재 링 버퍼 슬롯 필드 직접 참조 (수직 / 전체)

```java
public float getVerticalPrevLegYaw() { return datas[currentDataIndex].vertical.prevLegYaw; }
public float getVerticalLegYaw()     { return datas[currentDataIndex].vertical.legYaw; }
public float getVerticalTotal()      { return datas[currentDataIndex].vertical.total; }

public float getAllPrevLegYaw() { return datas[currentDataIndex].all.prevLegYaw; }
public float getAllLegYaw()     { return datas[currentDataIndex].all.legYaw; }
public float getAllTotal()      { return datas[currentDataIndex].all.total; }
```

- `currentDataIndex == -1` 상태에서 호출 시 `ArrayIndexOutOfBoundsException` 발생 가능 (null 체크 없음)
- `SmartStatisticsData`의 필드: `prevLegYaw`, `legYaw`, `total` (SmartStatisticsData 리서치에서 확인 필요)

---

### `getTickDistance()`

```java
public float getTickDistance() { return tickDistance; }
```

---

### get*(float renderPartialTicks) — 보간값 getter 6개

```java
public float getTotalHorizontalDistance(float renderPartialTicks)
{
    return get(renderPartialTicks).getTotalHorizontalDistance();
}
public float getTotalVerticalDistance(float renderPartialTicks)
{
    return get(renderPartialTicks).getTotalVerticalDistance();
}
public float getTotalDistance(float renderPartialTicks)
{
    return get(renderPartialTicks).getTotalDistance();
}
public float getCurrentHorizontalSpeed(float renderPartialTicks)
{
    return get(renderPartialTicks).getCurrentHorizontalSpeed();
}
public float getCurrentVerticalSpeed(float renderPartialTicks)
{
    return get(renderPartialTicks).getCurrentVerticalSpeed();
}
public float getCurrentSpeed(float renderPartialTicks)
{
    return get(renderPartialTicks).getCurrentSpeed();
}
```

- 모두 `get(renderPartialTicks)` → `SmartStatisticsDatas.setReady(renderPartialTicks)` 후 해당 getter 위임

---

### private `get()` / `get(float renderPartialTicks)`

```java
private SmartStatisticsDatas get()
{
    return currentDataIndex == -1 ? dummy : datas[currentDataIndex];
}

private SmartStatisticsDatas get(float renderPartialTicks)
{
    SmartStatisticsDatas data = get();
    data.setReady(renderPartialTicks);
    return data;
}
```

- `currentDataIndex == -1`: 아직 `calculateAllStats`가 한 번도 호출 안 된 상태 → `dummy` 반환
- `data.setReady(renderPartialTicks)`: 렌더 보간을 위한 준비 호출

---

### `getCurrentHorizontalSpeedFlattened(float renderPartialTicks, int strength)`

```java
public float getCurrentHorizontalSpeedFlattened(float renderPartialTicks, int strength)
{
    strength = Math.min(strength, datas.length);
    if(strength < 0)
        strength = datas.length;

    get(renderPartialTicks);
    float sum = 0;
    int count = 0;
    for(int i = 0, dataIndex = currentDataIndex; i < strength; i++, dataIndex--)
    {
        if(dataIndex < 0)
            dataIndex = datas.length - 1;
        SmartStatisticsDatas data = datas[dataIndex];
        if(data == null || !data.isReady())
            break;

        sum += data.getCurrentHorizontalSpeed();
        count++;
    }
    return sum / count;
}
```

`getCurrentVerticalSpeedFlattened`과 `getCurrentSpeedFlattened`도 동일한 구조. 각각 `data.getCurrentVerticalSpeed()`, `data.getCurrentSpeed()` 호출.

**동작:**
- `strength`: 평균을 낼 틱 수. `< 0`이면 최대(10)로 설정. `datas.length`(10)으로 상한 고정
- `get(renderPartialTicks)`: 현재 슬롯 setReady
- 링 버퍼를 `currentDataIndex`부터 뒤로 `strength`번 순회
  - `dataIndex < 0`이면 `datas.length - 1` (= 9)로 래핑
  - `data == null || !data.isReady()`: 조건 만족 시 `break` (이후 슬롯은 포함하지 않음)
- `sum / count` 반환 — 유효한 슬롯만 평균

**주의**: `count == 0`이면 `0 / 0` → `Float.NaN` 반환 가능. 가드 없음.

---

## SmartStatisticsDatas에서 파악된 구조 (이 파일에서 확인된 것만)

| 필드/메서드 | 타입 | 파악 경위 |
|-------------|------|-----------|
| `.horizontal` | `SmartStatisticsData` | `data.horizontal.calcualte(...)`, `data.horizontal.apply(sp)` |
| `.vertical` | `SmartStatisticsData` | `data.vertical.calcualte(...)` |
| `.all` | `SmartStatisticsData` | `data.all.calcualte(...)` |
| `.initialize(SmartStatisticsDatas previous)` | method | `data.initialize(previous)` |
| `.setReady(float renderPartialTicks)` | method | `data.setReady(renderPartialTicks)` |
| `.isReady()` | method → boolean | `!data.isReady()` |
| `.getTotalHorizontalDistance()` | method → float | 위임 getter |
| `.getTotalVerticalDistance()` | method → float | 위임 getter |
| `.getTotalDistance()` | method → float | 위임 getter |
| `.getCurrentHorizontalSpeed()` | method → float | 위임 getter + flattened |
| `.getCurrentVerticalSpeed()` | method → float | 위임 getter + flattened |
| `.getCurrentSpeed()` | method → float | 위임 getter + flattened |

## SmartStatisticsData에서 파악된 구조 (이 파일에서 확인된 것만)

| 필드/메서드 | 타입 | 파악 경위 |
|-------------|------|-----------|
| `.prevLegYaw` | float | `datas[currentDataIndex].vertical.prevLegYaw` |
| `.legYaw` | float | `datas[currentDataIndex].vertical.legYaw` |
| `.total` | float | `datas[currentDataIndex].vertical.total` |
| `.calcualte(float distance)` | method → float | `tickDistance = data.all.calcualte(...)` |
| `.apply(EntityPlayer sp)` | method | `data.horizontal.apply(sp)` |

---

## import

```java
import net.minecraft.entity.player.*;   // EntityPlayer
import net.minecraft.util.*;            // MathHelper
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartStatisticsContext` | 상위 클래스. `calculateHorizontalStats` 필드 포함 |
| `SmartStatisticsDatas` | 링 버퍼 슬롯 타입. 수평/수직/전체 통계 보관 |
| `SmartStatisticsData` | `SmartStatisticsDatas`의 `.horizontal/.vertical/.all` 필드 타입 |
| `EntityPlayer` (vanilla) | `sp` 레퍼런스. `posX/prevPosX`, `posY/prevPosY`, `posZ/prevPosZ`, `limbSwing`, `limbSwingAmount`, `prevLimbSwingAmount` |
| `MathHelper` (vanilla) | `sqrt_double` |

---

## 주요 관찰 사항

1. **링 버퍼 크기 10**: `datas.length = 10`. 최근 10틱의 이동 통계를 보관. flattened getter에서 최대 10틱 평균 계산.

2. **수평 통계의 이중 경로**: 수평 거리는 SmartStatisticsData가 계산하지만(`calcualte`), 실제 `getHorizontalLegYaw` 류 getter는 vanilla `sp.limbSwingAmount` 등을 직접 읽는다. `data.horizontal.apply(sp)`가 vanilla 필드를 SmartStatistics 계산값으로 덮어쓰는 구조로 보임 (SmartStatisticsData 리서치에서 확인 필요).

3. **`calculateHorizontalStats && !remote` 분기**: `remote = true`인 경우(다른 플레이어) `apply(sp)` 스킵. 원격 플레이어의 vanilla limbSwing 필드를 건드리지 않음.

4. **`prevHorizontalAngle = Float.NaN`**: 초기값이 NaN. 이 파일에서 직접 읽거나 쓰는 코드가 없음. SmartRenderModel/SmartRenderRender 등 외부에서 읽는 용도로 보임 — [확인 필요].

5. **`count == 0` 미가드**: `getCurrentXxxSpeedFlattened()`에서 유효 데이터가 하나도 없으면 `sum / 0` = `NaN` 반환. 호출 측에서 방어해야 함.

6. **1.21.1 이식**: `EntityPlayer` → `PlayerEntity`(Yarn), `MathHelper.sqrt_double` → `Math.sqrt` 또는 `MathHelper.sqrt`, `limbSwing/limbSwingAmount/prevLimbSwingAmount` → Yarn 매핑 이름으로 교체 필요 [미확인].
