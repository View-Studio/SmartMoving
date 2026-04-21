# IModelPlayer.java (net.smart.moving.render) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/render/IModelPlayer.java  
패키지: `net.smart.moving.render`  
종류: `interface`  
실행 위치: 클라이언트 (렌더링)

주의: 같은 이름의 `net.smart.render.IModelPlayer`와 다른 파일.

---

## 전체 소스

```java
package net.smart.moving.render;

public interface IModelPlayer
{
    SmartMovingModel getMovingModel();

    void superAnimateHeadRotation(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
    void superAnimateSleeping(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
    void superAnimateArmSwinging(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
    void superAnimateRiding(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
    void superAnimateLeftArmItemHolding(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
    void superAnimateRightArmItemHolding(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
    void superAnimateWorkingBody(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
    void superAnimateWorkingArms(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
    void superAnimateSneaking(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
    void superApplyAnimationOffsets(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
    void superAnimateBowAiming(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
}
```

---

## 역할

`SmartMovingModel`이 vanilla 애니메이션 메서드(super 계열)를 호출할 때 사용하는 인터페이스. `SmartMovingModelPlayerBase`가 이 인터페이스를 구현하며, SM 상태가 아닌 표준(isStandard) 상태에서 vanilla 원본 애니메이션을 위임 호출한다.

`SmartMovingModel`의 `imp` 필드 타입이 이 인터페이스.

---

## 메서드 목록

모든 `super*()` 메서드의 파라미터 시그니처는 동일:

```
(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime,
 float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
```

| 메서드 | 역할 |
|--------|------|
| `getMovingModel()` | 이 모델에 연결된 `SmartMovingModel` 인스턴스 반환 |
| `superAnimateHeadRotation(...)` | vanilla 머리 회전 애니메이션 |
| `superAnimateSleeping(...)` | vanilla 수면 애니메이션 |
| `superAnimateArmSwinging(...)` | vanilla 팔 스윙 애니메이션 |
| `superAnimateRiding(...)` | vanilla 탑승 애니메이션 |
| `superAnimateLeftArmItemHolding(...)` | vanilla 왼팔 아이템 보유 애니메이션 |
| `superAnimateRightArmItemHolding(...)` | vanilla 오른팔 아이템 보유 애니메이션 |
| `superAnimateWorkingBody(...)` | vanilla 작업(도구 사용) 몸통 애니메이션 |
| `superAnimateWorkingArms(...)` | vanilla 작업(도구 사용) 팔 애니메이션 |
| `superAnimateSneaking(...)` | vanilla 스니킹 애니메이션 |
| `superApplyAnimationOffsets(...)` | vanilla 애니메이션 오프셋 적용 |
| `superAnimateBowAiming(...)` | vanilla 활 조준 애니메이션 |

---

## 사용 관계

```
SmartMovingModelPlayerBase  ─implements─▶  IModelPlayer (SM쪽)
                                                  │
SmartMovingModel.imp  ─────────────────────────▶ │
  (isStandard일 때 imp.super*() 호출)             │
```

`SmartMovingModel.animateHeadRotation()` 등에서:
```java
if(isStandard)
    imp.superAnimateHeadRotation(...);
```

`animateNonStandardBowAiming()` 에서:
```java
imp.superAnimateBowAiming(...);  // 비표준 상태에서도 호출됨
```

---

## SmartRender의 동명 인터페이스와 비교

| 항목 | `net.smart.render.IModelPlayer` | `net.smart.moving.render.IModelPlayer` (이 파일) |
|------|--------------------------------|------------------------------------------------|
| 패키지 | net.smart.render | net.smart.moving.render |
| `getMovingModel()` 반환 타입 | `net.smart.render.SmartRenderModel` | `SmartMovingModel` |
| super 메서드 수 | 동일 11개 | 동일 11개 |
| 구현체 | `SmartRenderModelPlayerBase` | `SmartMovingModelPlayerBase` |

---

## 주요 관찰 사항

1. **두 IModelPlayer 공존**: `net.smart.render.IModelPlayer`(SmartRender)와 이 파일(SmartMoving). `SmartMovingModel` 생성자 파라미터에서 둘 다 사용:
   ```java
   public SmartMovingModel(net.smart.render.IModelPlayer md, IModelPlayer imp)
   ```
   `md`는 SR쪽, `imp`는 SM쪽.

2. **`getMovingModel()` 반환 타입 차이**: SR쪽은 `SmartRenderModel`, SM쪽은 `SmartMovingModel`. SM의 `SmartMovingModel`은 SR의 `SmartRenderModel`을 내부 필드(`md`)로 보유.

3. **1.21.1 이식 관련**:
   - PlayerAPI(ModelPlayerBase) → Mixin으로 대체
   - `super*()` 메서드들은 Mixin에서 `@Invoker` 또는 CallbackInfo 패턴으로 vanilla 원본 호출
   - 인터페이스 자체는 유지하거나 직접 함수형 인터페이스/콜백으로 교체 가능
