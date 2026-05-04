# 리서치 — `currentHorizontalSpeedFlattened` 원본 1:1 매핑 (FIR 10-tick)

## 작업 범위

원본 SmartStatistics 의 `getCurrentHorizontalSpeedFlattened(partialTicks, -1)` 1:1 매핑.

**현재 (EMA 0.5)** → **원본 (FIR 10-tick history average)**.

## 원본 분석

### `SmartStatistics.java`
```java
private final SmartStatisticsDatas[] datas = new SmartStatisticsDatas[10];
private int currentDataIndex = -1;

public void calculateAllStats(boolean remote) {
    SmartStatisticsDatas previous = get();
    currentDataIndex++;
    if (currentDataIndex >= datas.length) currentDataIndex = 0;
    SmartStatisticsDatas data = datas[currentDataIndex];
    if (data == null) data = datas[currentDataIndex] = new SmartStatisticsDatas();
    data.initialize(previous);

    data.horizontal.calcualte(sqrt(dx² + dz²));
    data.vertical.calcualte(|dy|);
    data.all.calcualte(sqrt(dx² + dy² + dz²));
}

public float getCurrentHorizontalSpeedFlattened(float renderPartialTicks, int strength) {
    strength = Math.min(strength, datas.length);
    if (strength < 0) strength = datas.length;
    get(renderPartialTicks);  // setReady on current data
    float sum = 0; int count = 0;
    for (int i = 0, dataIndex = currentDataIndex; i < strength; i++, dataIndex--) {
        if (dataIndex < 0) dataIndex = datas.length - 1;
        SmartStatisticsDatas data = datas[dataIndex];
        if (data == null || !data.isReady()) break;
        sum += data.getCurrentHorizontalSpeed();  // = min(1.0, prevLegYaw + (legYaw-prev)*pt)
        count++;
    }
    return sum / count;
}
```

### `SmartStatisticsData.java` (각 horizontal/vertical/all 1개씩)
```java
public float prevLegYaw, legYaw, total;

public float getCurrentSpeed(float partialTicks) {
    return Math.min(1.0F, prevLegYaw + (legYaw - prevLegYaw) * partialTicks);
}

public float getTotalDistance(float partialTicks) {
    return total - legYaw * (1.0F - partialTicks);
}

public void initialize(SmartStatisticsData previous) {
    prevLegYaw = previous.legYaw;
    legYaw = previous.legYaw;
    total = previous.total;
}

public float calcualte(float distance) {
    distance = distance * 4F;
    legYaw += (distance - legYaw) * 0.4F;  // EMA(0.4)
    total += legYaw;
    return distance;
}
```

### `SmartStatisticsDatas.java`
```java
public final SmartStatisticsData horizontal = new SmartStatisticsData();
public final SmartStatisticsData vertical = new SmartStatisticsData();
public final SmartStatisticsData all = new SmartStatisticsData();
private float renderPartialTicks;

public void setReady(float pt) { this.renderPartialTicks = pt; }
public boolean isReady() { return !Float.isNaN(renderPartialTicks); }

public void initialize(SmartStatisticsDatas previous) {
    renderPartialTicks = Float.NaN;
    horizontal.initialize(previous.horizontal);
    vertical.initialize(previous.vertical);
    all.initialize(previous.all);
}
```

## 우리 현재 매핑

```java
public float currentHorizontalSpeedFlattened;
public float prevCurrentHorizontalSpeedFlattened;

// calculate() 안:
currentHorizontalSpeedFlattened = currentHorizontalSpeedFlattened * 0.5f + currentHorizontalSpeed * 0.5f;

public float getCurrentHorizontalSpeedFlattened(float partialTicks) {
    return Math.min(1.0F, prevCurrentHorizontalSpeedFlattened
            + (currentHorizontalSpeedFlattened - prevCurrentHorizontalSpeedFlattened) * partialTicks);
}
```

## 원본 1:1 매핑 설계

### 옵션 A — Datas 클래스 추가 (원본 정확)
- `SmartStatisticsData[10]` (또는 비슷한) history 저장.
- 복잡 + 유지보수 부담.

### 옵션 B — 단순 ring buffer (legYaw history 만)
- `currentHorizontalSpeed` history 10-tick 만 저장.
- `getCurrentHorizontalSpeedFlattened(pt)` = 10-tick legYaw FIR 평균 + pt lerp.
- 단순. 기존 단일 EMA 매핑 + history 추가.

### 권장 = 옵션 B

원본 의도 = "legYaw EMA(0.4) 의 10-tick 평균". 우리는 `currentHorizontalSpeed` 가 이미 legYaw 등가 (EMA 0.4 적용됨). history 만 추가하면 됨.

## 우리 매핑 변경

```java
// Field
private static final int HISTORY_SIZE = 10;
private final float[] currentHorizontalSpeedHistory = new float[HISTORY_SIZE];
private final float[] prevCurrentHorizontalSpeedHistory = new float[HISTORY_SIZE];
private int historyIndex = -1;
private boolean[] historyReady = new boolean[HISTORY_SIZE];

// calculate() 안에서 매 tick history 저장:
historyIndex++;
if (historyIndex >= HISTORY_SIZE) historyIndex = 0;
prevCurrentHorizontalSpeedHistory[historyIndex] = prevCurrentHorizontalSpeed;
currentHorizontalSpeedHistory[historyIndex] = currentHorizontalSpeed;
historyReady[historyIndex] = true;

// 원본 getCurrentHorizontalSpeedFlattened (FIR + partial tick lerp):
public float getCurrentHorizontalSpeedFlattened(float partialTicks) {
    if (historyIndex < 0) return 0f;
    float sum = 0f;
    int count = 0;
    for (int i = 0, idx = historyIndex; i < HISTORY_SIZE; i++, idx--) {
        if (idx < 0) idx = HISTORY_SIZE - 1;
        if (!historyReady[idx]) break;
        float prevLegYaw = prevCurrentHorizontalSpeedHistory[idx];
        float legYaw = currentHorizontalSpeedHistory[idx];
        sum += Math.min(1.0F, prevLegYaw + (legYaw - prevLegYaw) * partialTicks);
        count++;
    }
    return count > 0 ? sum / count : 0f;
}

// Flattened EMA(0.5) 식 + prev field 제거 (단일 EMA 더 이상 안 씀).
```

## 회귀 위험

다른 분기 (= crawl, fly, falling) 도 같은 메서드 사용. transient 시점 walkFactor 변화.

- **crawl (sm_animateCrawling)**: 일정 속도 → 영향 미비.
- **fly (sm_animateFlying)**: walkFactor 사용. 가속/감속 transient 시 visual 변화. 메모리 `project_flying_complete.md` 완결 분기 → 회귀 가능.
- **falling (sm_animateFalling)**: walkFactor 사용. 메모리 `project_falling_complete.md` 완결 분기 → 회귀 가능.

★ 사용자 명시 "이상하면 롤백" — 진행 후 검증 ★

## 작업 단계

1. SmartStatistics.java 변경:
   - history field 추가.
   - calculate() 안 history 저장.
   - getCurrentHorizontalSpeedFlattened(pt) FIR 평균 매핑.
   - 기존 EMA(0.5) 식 제거.
   - currentHorizontalSpeedFlattened/prevCurrentHorizontalSpeedFlattened field 제거 (또는 deprecated).
2. 빌드 + 인게임 검증.

## 인게임 검증 시나리오

- (a) **슬라이딩 진입 transient**: 진자 amplitude ramp.
- (b) **슬라이딩 감속 phase**: 진자 amplitude decay.
- (c) **회귀**: 비행 (날개짓 amplitude transient).
- (d) **회귀**: 낙하 (팔/다리 amplitude transient).
- (e) **회귀**: 엎드리기 (영향 미비).
