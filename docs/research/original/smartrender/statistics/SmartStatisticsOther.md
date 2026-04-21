# SmartStatisticsOther.java (net.smart.render.statistics) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/statistics/SmartStatisticsOther.java  
패키지: `net.smart.render.statistics`  
종류: `class`  
상속: `SmartStatisticsOther extends SmartStatistics`

---

## 전체 소스

```java
package net.smart.render.statistics;

import net.minecraft.client.entity.*;

public class SmartStatisticsOther extends SmartStatistics
{
    public boolean foundAlive;

    public SmartStatisticsOther(EntityOtherPlayerMP sp)
    {
        super(sp);
    }
}
```

---

## 역할

다른 플레이어(`EntityOtherPlayerMP`)를 위한 `SmartStatistics` 서브클래스.  
`SmartStatistics`에 `foundAlive` 필드 하나만 추가한 최소 확장.  
`SmartStatisticsFactory.doHandleMultiPlayerTick()`의 마크-앤-스윕 GC에서 "이번 틱에 월드에 존재했는가"를 표시하는 용도로만 사용.

- **실행 위치**: 클라이언트 전용

---

## 필드

```java
public boolean foundAlive;   // 기본값 false. SmartStatisticsFactory.doHandleMultiPlayerTick()에서 직접 쓰기
```

**사용 흐름 (SmartStatisticsFactory.doHandleMultiPlayerTick 내):**

```
Phase 1 — 살아있는 플레이어 처리:
    statistics.foundAlive = true;    // 월드에 존재함을 마크

Phase 2 — 마크-앤-스윕 정리:
    if(statistics.foundAlive)
        statistics.foundAlive = false;   // 다음 틱을 위해 리셋
    else
        entityIds.remove();              // 사라진 플레이어 항목 제거
```

---

## 생성자

```java
public SmartStatisticsOther(EntityOtherPlayerMP sp)
{
    super(sp);
}
```

- `EntityOtherPlayerMP`는 `EntityPlayer`를 상속하므로 `SmartStatistics(EntityPlayer sp)` 생성자에 그대로 전달 가능
- `super(sp)`에서 `this.sp = sp` 설정

---

## import

```java
import net.minecraft.client.entity.*;   // EntityOtherPlayerMP
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartStatistics` | 상위 클래스. 모든 통계 계산 로직 보유 |
| `SmartStatisticsFactory` | `otherStatistics` 맵의 값 타입. `foundAlive` 직접 접근 |
| `EntityOtherPlayerMP` (vanilla) | 생성자 파라미터 타입. `EntityPlayer` 서브클래스 |

---

## 주요 관찰 사항

1. **추가 로직 없음**: `SmartStatistics`의 모든 기능(링 버퍼, `calculateAllStats`, `getXxx` getter 등)을 그대로 상속. `foundAlive` 필드 하나만 추가.

2. **`foundAlive`가 public인 이유**: `SmartStatisticsFactory`가 패키지 내부에서 직접 `statistics.foundAlive = true/false`로 쓰기 접근. getter/setter 없이 필드 직접 접근.

3. **`calculateAllStats(true)` 호출**: `SmartStatisticsFactory.doHandleMultiPlayerTick()`에서 `remote=true`로 호출되므로, `data.horizontal.apply(sp)` 실행 안 됨. 다른 플레이어의 vanilla `limbSwing*` 필드는 SmartStatistics가 건드리지 않음.

4. **1.21.1 이식**: `EntityOtherPlayerMP` → 1.21.1 Yarn 이름 [미확인]. 클래스 구조 자체는 단순하므로 그대로 이식 가능.
