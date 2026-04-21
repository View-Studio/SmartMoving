# IEntityPlayerSP.java (net.smart.render.statistics) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/statistics/IEntityPlayerSP.java  
패키지: `net.smart.render.statistics`  
종류: `interface`

---

## 전체 소스

```java
package net.smart.render.statistics;

public interface IEntityPlayerSP
{
    SmartStatistics getStatistics();
}
```

---

## 역할

`EntityPlayerSP`(싱글플레이어 로컬 플레이어 엔티티)가 `SmartStatistics` 인스턴스를 노출하기 위한 인터페이스.  
`SmartStatisticsFactory` 또는 유사 코드에서 `EntityPlayerSP`를 이 인터페이스로 캐스트하여 `getStatistics()`를 호출하는 패턴에 사용된다.

- **실행 위치**: 클라이언트 전용 (`EntityPlayerSP`는 클라이언트 전용 클래스)

---

## 메서드

```java
SmartStatistics getStatistics();
```

- 반환 타입: `SmartStatistics` (같은 패키지 `net.smart.render.statistics`)
- 파라미터: 없음
- import 없음 (동일 패키지이므로)

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartStatistics` (net.smart.render.statistics) | 반환 타입 |

---

## 주요 관찰 사항

1. **단일 메서드 인터페이스**: `getStatistics()` 하나만 정의. vanilla `EntityPlayerSP`에 이 인터페이스를 믹스인(PlayerAPI 또는 ASM bytecode 패치)하여 `SmartStatistics` 인스턴스를 외부에서 접근 가능하게 하는 패턴.

2. **SmartMoving의 동명 인터페이스와 다른 패키지**: SmartMoving에도 `net.smart.moving` 패키지에 `IEntityPlayerSP.java`가 존재한다(`PROGRESS.md` 확인). 이 파일은 SmartRender의 `net.smart.render.statistics` 패키지 소속이며, `SmartStatistics`(SmartRender statistics)를 반환한다.

3. **1.21.1 이식**: 1.21.1에서는 `ClientPlayerEntity`(Yarn)가 `EntityPlayerSP`에 대응. Mixin의 `@Implements`/인터페이스 주입으로 동일 패턴 구현 가능. 단, SmartStatistics 자체가 1.21.1에 이식될 경우에만 의미 있음.
