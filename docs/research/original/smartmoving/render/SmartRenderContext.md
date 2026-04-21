# SmartRenderContext.java (net.smart.moving.render) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/render/SmartRenderContext.java  
패키지: `net.smart.moving.render`  
종류: `abstract class`  
상속: `net.smart.moving.SmartMovingContext` (extends)  
실행 위치: 클라이언트 (렌더링)

주의: 같은 이름의 `net.smart.render.SmartRenderContext`와 다른 파일.  
`net.smart.render.SmartRenderContext`는 각도 상수(Half/Quarter/…)를 정의.  
이 파일은 `SmartMovingContext`를 확장하고 스케일 타입 상수 3개만 추가 정의.

---

## 전체 소스

```java
package net.smart.moving.render;

import net.smart.moving.*;

public abstract class SmartRenderContext extends SmartMovingContext
{
    public static final int Scale = 0;
    public static final int NoScaleStart = 1;
    public static final int NoScaleEnd = 2;
}
```

---

## 역할

`net.smart.moving.render` 패키지의 렌더 클래스들(`SmartMovingModel`, `SmartMovingRender`)의 공통 부모. `SmartMovingContext`를 상속하며 스케일 타입 상수 3개를 추가 정의한다.

직접 인스턴스화 불가(`abstract`).

---

## import

```java
import net.smart.moving.*;  // SmartMovingContext
```

---

## 상속 관계

```
SmartMovingContext  (net.smart.moving)
    └─ SmartRenderContext  (net.smart.moving.render)  [abstract]
         ├─ SmartMovingModel
         └─ SmartMovingRender
```

`SmartMovingContext`에서 상속받는 것은 SmartMovingContext 리서치 참조.

`net.smart.render.SmartRenderContext`(SmartRender 모드)에서 상속받는 것은 별개:
- 이 클래스는 `SmartMovingContext`를 상속
- `SmartMovingModel`은 이 클래스를 상속하면서도 SmartRender의 각도 상수(Half/Quarter/…)를 사용 → SmartRender의 `SmartRenderContext`도 별도로 상속 경로에 있음

---

## 정적 상수

```java
public static final int Scale = 0;
public static final int NoScaleStart = 1;
public static final int NoScaleEnd = 2;
```

| 상수 | 값 | 의미 |
|------|----|------|
| `Scale` | 0 | scaleY를 직접 변경 (팔·다리가 실제로 신축) |
| `NoScaleStart` | 1 | 스케일 적용 자체를 스킵 — `setArmScales()`/`setLegScales()` 호출 안 함 |
| `NoScaleEnd` | 2 | scaleY 미변경, offsetY를 `(1 - scale) * 0.5F`만큼 보정하여 시각적으로 맞춤 |

**사용 위치**:
- `SmartMovingRender` 생성자에서 각 모델 레이어에 할당
- `SmartMovingModel.setArmScales()` / `SmartMovingModel.setLegScales()`에서 분기 조건으로 사용
- `SmartMovingModel.setRotationAngles()`에서 `if(scaleLegType != NoScaleStart)` 조건으로 호출 여부 결정

**할당 내역** (`SmartMovingRender` 생성자):
- `modelBipedMain`: Scale / Scale
- `modelArmorChestplate`: NoScaleStart / NoScaleEnd
- `modelArmor`: NoScaleStart / Scale

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `net.smart.moving.SmartMovingContext` | 상속 |

---

## 주요 관찰 사항

1. **abstract 선언**: 직접 인스턴스화 방지. 서브클래스(`SmartMovingModel`, `SmartMovingRender`)에서만 사용.

2. **상수만 추가**: `SmartMovingContext`에 스케일 타입 상수 3개를 추가하는 것이 이 클래스의 전부다.

3. **SmartRender의 동명 클래스와 혼동 주의**:
   - `net.smart.render.SmartRenderContext`: Half/Quarter/Eighth/… 각도 상수 + SmartRender 공통 부모
   - `net.smart.moving.render.SmartRenderContext` (이 파일): Scale/NoScaleStart/NoScaleEnd 상수 + SmartMovingContext 확장

4. **1.21.1 이식 관련**:
   - 상수 정의만 있으므로 1:1로 포팅 가능
   - `SmartMovingContext`의 이식 방향에 따라 상속 구조 변경 필요
