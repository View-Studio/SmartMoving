# ModelPlayer.java (net.smart.moving.render) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/render/ModelPlayer.java  
패키지: `net.smart.moving.render`  
종류: `class`  
상속: `net.smart.render.ModelPlayer` (extends) + `IModelPlayer` (implements)  
실행 위치: 클라이언트 (렌더링)

주의: 같은 이름의 `net.smart.render.ModelPlayer`와 다른 파일.

---

## 전체 소스

소스는 GitHub 링크 참조.

---

## 역할

SmartRender의 `ModelPlayer`를 확장하면서 SM의 `IModelPlayer`를 구현. `this` 하나가 두 인터페이스(`net.smart.render.IModelPlayer`와 `net.smart.moving.render.IModelPlayer`) 모두를 만족하므로, `SmartMovingModel` 생성자에 `(this, this)`로 전달할 수 있다.

PlayerAPI 없이도 동작하는 독립형(standalone) 모델 클래스. PlayerAPI 기반 경로에서는 `SmartMovingModelPlayerBase`가 역할을 담당하고, 이 클래스는 PlayerAPI 없는 환경에서 사용된다.

---

## import

없음 (패키지 내 클래스만 참조).

---

## 상속/구현 관계

```
net.smart.render.ModelPlayer  (SmartRender)
    └─ net.smart.moving.render.ModelPlayer  (이 파일)
         implements net.smart.moving.render.IModelPlayer
```

`net.smart.render.ModelPlayer`는 `net.smart.render.IModelPlayer`를 구현하므로:
- `this`는 `net.smart.render.IModelPlayer` ← 상속으로 충족
- `this`는 `net.smart.moving.render.IModelPlayer` ← 이 클래스가 직접 구현

---

## 필드

```java
private final SmartMovingModel model;
```

생성자에서 초기화 후 변경 없음.

---

## 생성자

```java
public ModelPlayer(float f)
{
    super(f);
    model = new SmartMovingModel(this, this);
}
```

- `super(f)`: `net.smart.render.ModelPlayer(float)` 호출 — SmartRender 모델 초기화
- `new SmartMovingModel(this, this)`:
  - 첫 번째 `this`: `net.smart.render.IModelPlayer md` — SmartRender 쪽 모델 인터페이스
  - 두 번째 `this`: `net.smart.moving.render.IModelPlayer imp` — SM 쪽 super 위임 인터페이스

---

## 메서드 구조

두 그룹의 11개 메서드 쌍으로 구성. 모든 메서드의 파라미터 시그니처:

```
(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime,
 float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
```

### 그룹 1 — `animate*()` (SR ModelPlayer override → SM 위임)

SR `ModelPlayer`의 `animate*()` 메서드를 오버라이드하여 `model.animate*()`로 전달.

| 메서드 | 위임 대상 |
|--------|-----------|
| `animateHeadRotation(...)` | `model.animateHeadRotation(...)` |
| `animateSleeping(...)` | `model.animateSleeping(...)` |
| `animateArmSwinging(...)` | `model.animateArmSwinging(...)` |
| `animateRiding(...)` | `model.animateRiding(...)` |
| `animateLeftArmItemHolding(...)` | `model.animateLeftArmItemHolding(...)` |
| `animateRightArmItemHolding(...)` | `model.animateRightArmItemHolding(...)` |
| `animateWorkingBody(...)` | `model.animateWorkingBody(...)` |
| `animateWorkingArms(...)` | `model.animateWorkingArms(...)` |
| `animateSneaking(...)` | `model.animateSneaking(...)` |
| `animateArms(...)` | `model.animateArms(...)` |
| `animateBowAiming(...)` | `model.animateBowAiming(...)` |

### 그룹 2 — `superAnimate*()` (SM IModelPlayer 구현 → SR super 호출)

SM `IModelPlayer` 인터페이스의 `superAnimate*()` 메서드를 구현하여 `super.animate*()`(SR ModelPlayer 원본)를 호출.

| 메서드 | 호출 대상 |
|--------|-----------|
| `superAnimateHeadRotation(...)` | `super.animateHeadRotation(...)` |
| `superAnimateSleeping(...)` | `super.animateSleeping(...)` |
| `superAnimateArmSwinging(...)` | `super.animateArmSwinging(...)` |
| `superAnimateRiding(...)` | `super.animateRiding(...)` |
| `superAnimateLeftArmItemHolding(...)` | `super.animateLeftArmItemHolding(...)` |
| `superAnimateRightArmItemHolding(...)` | `super.animateRightArmItemHolding(...)` |
| `superAnimateWorkingBody(...)` | `super.animateWorkingBody(...)` |
| `superAnimateWorkingArms(...)` | `super.animateWorkingArms(...)` |
| `superAnimateSneaking(...)` | `super.animateSneaking(...)` |
| `superApplyAnimationOffsets(...)` | `super.animateArms(...)` ← **이름 불일치** |
| `superAnimateBowAiming(...)` | `super.animateBowAiming(...)` |

**`superApplyAnimationOffsets` 특이사항**: `super.animateArms(...)`를 호출한다. 인터페이스 메서드 이름(`superApplyAnimationOffsets`)과 실제 호출 메서드 이름(`animateArms`)이 다르다. SR `ModelPlayer`에서 `animateArms()`가 "오프셋 적용" 역할을 한다.

---

## 호출 흐름

```
SR ModelPlayer가 animate*()를 호출 (vanilla 렌더링 파이프라인)
    │
    ▼
net.smart.moving.render.ModelPlayer.animate*()  [오버라이드]
    │
    ▼
SmartMovingModel.animate*()  [SM 상태 기반 분기]
    │
    ├─ isStandard == true
    │       │
    │       ▼
    │   imp.superAnimate*()  [IModelPlayer 콜백]
    │       │
    │       ▼
    │   net.smart.moving.render.ModelPlayer.superAnimate*()
    │       │
    │       ▼
    │   super.animate*()  [SR ModelPlayer 원본 = vanilla 동작]
    │
    └─ isStandard == false → SM 커스텀 각도 적용, super 호출 안 함
```

---

## `getMovingModel()`

```java
@Override
public SmartMovingModel getMovingModel()
{
    return model;
}
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `net.smart.render.ModelPlayer` | 상속 — SR 모델 초기화, vanilla animate 원본 |
| `net.smart.moving.render.IModelPlayer` | 구현 — super 위임 경로 제공 |
| `SmartMovingModel` | 생성 보유 — SM 애니메이션 로직 |

---

## 주요 관찰 사항

1. **`(this, this)` 패턴**: 생성자에서 `new SmartMovingModel(this, this)`. 이 클래스가 SR `IModelPlayer`와 SM `IModelPlayer` 두 인터페이스를 동시에 만족하므로 가능. 동일 객체가 두 역할(모델 데이터 제공자, super 위임 대상) 수행.

2. **두 방향의 위임**: 
   - SR → SM: `animate*()` 오버라이드로 SM 애니메이션 시스템에 진입
   - SM → SR: `superAnimate*()` 구현으로 vanilla 원본으로 복귀
   이 두 경로가 `SmartMovingModel.isStandard` 플래그로 전환된다.

3. **`superApplyAnimationOffsets` → `super.animateArms()`**: 인터페이스 메서드명과 실제 SR 메서드명이 불일치. SR `ModelPlayer`에서 `animateArms()`가 오프셋 적용 역할임을 의미.

4. **PlayerAPI 없는 경로**: PlayerAPI 기반 경로에서는 `SmartMovingModelPlayerBase`가 동일한 역할(두 IModelPlayer 구현)을 담당. 이 클래스는 PlayerAPI 없는 환경(또는 갑옷 레이어 모델)에 사용.

5. **1.21.1 이식 관련**:
   - SR `ModelPlayer` 상속 구조 전체가 Mixin 기반으로 재설계 필요
   - `(this, this)` 패턴 — Mixin 환경에서는 인터페이스 주입(`@Implements`) 또는 별도 어댑터로 대체
   - `super.animate*()` 접근 — Mixin `@Invoker`로 vanilla 원본 메서드 직접 호출
