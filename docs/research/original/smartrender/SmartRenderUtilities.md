# SmartRenderUtilities.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/SmartRenderUtilities.java  
패키지: `net.smart.render`  
종류: `abstract class` (상속 없음)

---

## 역할

SmartRender 전체에서 공유하는 수학 상수와 유틸리티 메서드를 제공하는 최상위 추상 클래스.  
`SmartRenderContext extends SmartRenderUtilities` 상속을 통해 `SmartRenderModel`, `SmartRenderRender`에 상수가 전달된다.  
`ModelRotationRenderer`는 상속 없이 이 클래스의 상수를 직접 `protected static final`로 복사 참조한다.

---

## 상수 필드 — 실제 값 전체 확인

```java
public static final float Whole          = (float)Math.PI * 2F;   // 2π
public static final float Half           = (float)Math.PI;         // π
public static final float Quarter        = Half / 2F;              // π/2
public static final float Eighth         = Quarter / 2F;           // π/4
public static final float Sixteenth      = Eighth / 2F;            // π/8
public static final float Thirtytwoth   = Sixteenth / 2F;          // π/16
public static final float Sixtyfourth   = Thirtytwoth / 2F;        // π/32

public static final float RadiantToAngle = 360F / Whole;           // 360 / (2π) = 180/π
```

### 수치 정리

| 상수 | 수식 | 근사값 |
|------|------|--------|
| `Whole` | 2π | 6.2831855 |
| `Half` | π | 3.1415927 |
| `Quarter` | π/2 | 1.5707964 |
| `Eighth` | π/4 | 0.7853982 |
| `Sixteenth` | π/8 | 0.3926991 |
| `Thirtytwoth` | π/16 | 0.1963496 |
| `Sixtyfourth` | π/32 | 0.0981748 |
| `RadiantToAngle` | 180/π | 57.2957795 |

### `RadiantToAngle` 사용 패턴 (이제 모두 확인됨)

- `angle / RadiantToAngle` → 각도(degree) → 라디안 변환
  - 예: `bipedOuter.rotateAngleY = actualRotation / RadiantToAngle`
  - 예: `bipedHead.rotateAngleY = (actualRotation + viewHorizontalAngelOffset) / RadiantToAngle`
- `radiant * RadiantToAngle` → 라디안 → 각도(degree) 변환
  - 예: `GL11.glRotatef(rotateAngleX * RadiantToAngle, 1.0F, 0.0F, 0.0F)`

→ SmartRender 내부는 **라디안**으로 회전값을 저장하고, GL 호출 시 각도로 변환.

---

## 메서드 전체

### `getHorizontalCollisionangle(boolean isCollidedPositiveX, boolean isCollidedNegativeX, boolean isCollidedPositiveZ, boolean isCollidedNegativeZ)` — static

4방향 충돌 조합에 따라 각도(도 단위)를 반환한다.

```java
public static float getHorizontalCollisionangle(boolean isCollidedPositiveX, boolean isCollidedNegativeX,
                                                  boolean isCollidedPositiveZ, boolean isCollidedNegativeZ)
{
    if(isCollidedPositiveX)
        if(isCollidedNegativeX)
            if(isCollidedPositiveZ)
                if(isCollidedNegativeZ)
                    ;                    // +X -X +Z -Z → NaN
                else
                    return 90F;          // +X -X +Z    → 90
            else
                if(isCollidedNegativeZ)
                    return 270F;         // +X -X -Z    → 270
                else
                    ;                    // +X -X       → NaN
        else
            if(isCollidedPositiveZ)
                if(isCollidedNegativeZ)
                    return 0F;           // +X +Z -Z    → 0
                else
                    return 45F;          // +X +Z       → 45
            else
                if(isCollidedNegativeZ)
                    return 315F;         // +X -Z       → 315
                else
                    return 0F;           // +X only     → 0
    else
        if(isCollidedNegativeX)
            if(isCollidedPositiveZ)
                if(isCollidedNegativeZ)
                    return 180F;         // -X +Z -Z    → 180
                else
                    return 135F;         // -X +Z       → 135
            else
                if(isCollidedNegativeZ)
                    return 225F;         // -X -Z       → 225
                else
                    return 180F;         // -X only     → 180
        else
            if(isCollidedPositiveZ)
                if(isCollidedNegativeZ)
                    ;                    // +Z -Z       → NaN
                else
                    return 90F;          // +Z only     → 90
            else
                if(isCollidedNegativeZ)
                    return 270F;         // -Z only     → 270
                else
                    ;                    // 충돌 없음   → NaN

    return Float.NaN;
}
```

**전체 조합 표 (+X/-X/+Z/-Z 순서):**

| +X | -X | +Z | -Z | 반환값 |
|----|----|----|----|----|
| T | T | T | T | NaN |
| T | T | T | F | 90F |
| T | T | F | T | 270F |
| T | T | F | F | NaN |
| T | F | T | T | 0F |
| T | F | T | F | 45F |
| T | F | F | T | 315F |
| T | F | F | F | 0F |
| F | T | T | T | 180F |
| F | T | T | F | 135F |
| F | T | F | T | 225F |
| F | T | F | F | 180F |
| F | F | T | T | NaN |
| F | F | T | F | 90F |
| F | F | F | T | 270F |
| F | F | F | F | NaN |

- 양쪽 동시 충돌(+X와 -X 동시, 또는 +Z와 -Z 동시)이면서 추가 정보 없으면 NaN
- 단일 방향 충돌: +X→0, -X→180, +Z→90, -Z→270 (Minecraft Z축: +Z가 남쪽)

---

### `getAngle(double x, double y)` — static

(x, y) 2D 벡터의 각도를 0~360도로 반환한다.

```java
public static float getAngle(double x, double y)
{
    if(x == 0)
    {
        if(y == 0) return Float.NaN;
        if(y < 0)  return 270;
        return 90;
    }

    if(y == 0)
    {
        if(x < 0) return 180;
        return 0;
    }

    float angle = (float)Math.atan(y / x) * RadiantToAngle;
    if(x < 0)           return 180F + angle;
    if(y < 0 && x > 0) return 360F + angle;
    return angle;
}
```

**각도 범위 분석:**
- x=0, y=0 → NaN
- x=0, y>0 → 90
- x=0, y<0 → 270
- y=0, x>0 → 0
- y=0, x<0 → 180
- 일반: `atan(y/x) * RadiantToAngle` + 사분면 보정
  - x<0 → +180 (2,3사분면)
  - y<0, x>0 → +360 (4사분면, atan 음수 결과를 양수 범위로)

---

## 의존 관계

없음. 외부 의존 없이 `Math.PI`와 `Math.atan`만 사용.

---

## 이전 리서치의 미확인 항목 해결

지금까지 `[미확인 — SmartRenderContext 읽기 전]`으로 표기했던 상수값이 모두 확정됨:

| 사용처 | 상수 | 실제 값 |
|--------|------|---------|
| SmartRenderModel — head 회전 나눗셈 | `RadiantToAngle` | 180/π ≈ 57.296 |
| SmartRenderModel — arm swinging | `Half` | π ≈ 3.14159 |
| SmartRenderModel — sleeping head X | `Eighth` | π/4 ≈ 0.78540 |
| SmartRenderModel — working body/arms sin | `Whole` | 2π ≈ 6.28319 |
| SmartRenderModel — cloak X 회전 | `Sixtyfourth` | π/32 ≈ 0.09817 |
| SmartRenderRender — currentVerticalAngle NaN 대체 | `Quarter` | π/2 ≈ 1.57080 |
| SmartRenderRender — currentHorizontalAngle += Half | `Half` | π ≈ 3.14159 |
| ModelRotationRenderer — GL rotate 변환 | `RadiantToAngle` | 180/π ≈ 57.296 |
| ModelRotationRenderer — GetIntermediateAngle | `Whole`, `Half` | 확인됨 |

---

## 주요 관찰 사항

1. **SmartRenderContext에 상수 없음 확인**: 이전 리서치에서 SmartRenderContext 파일을 읽고 상수가 없다고 기록한 것이 맞았음. 상수는 전부 이 파일에 있음.

2. **네이밍 규칙**: `Whole=2π`, `Half=π`, `Quarter=π/2`, ... 피(π)의 분수를 이름으로 사용. 분자는 항상 1, 분모가 이름에 반영됨 (`Eighth`=π/4가 아니라 π/4인 이유: Half의 절반의 절반 = π/4).

3. **`getHorizontalCollisionangle`**: SmartMoving 이동 로직에서 벽 충돌 방향을 각도로 변환할 때 사용할 가능성 높음. SmartRender 렌더 코드에서는 직접 참조 확인 못 함 — [미확인, SmartMoving 리서치에서 확인]

4. **1.21.1 이식**: 이 클래스는 순수 수학 상수이므로 그대로 유지 가능. `Math.PI`와 삼각함수는 Java 표준이므로 변경 불필요.
