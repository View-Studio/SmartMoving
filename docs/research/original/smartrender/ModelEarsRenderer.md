# ModelEarsRenderer.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/ModelEarsRenderer.java  
패키지: `net.smart.render`  
상속: `ModelEarsRenderer extends ModelSpecialRenderer`

---

## 역할

귀(Ears) 스킨 파트 렌더러. `ModelSpecialRenderer`를 상속하므로 기본적으로 `ignoreRender = true`이며, `beforeRender()`/`afterRender()`로 렌더 구간을 제어한다.  
`_i` 카운터를 통해 호출 순서에 따라 X 오프셋을 교대로 적용하여 귀 두 개의 좌우 배치를 처리한다.

- **실행 위치**: 클라이언트 전용

---

## 필드

```java
private int _i = 0;   // 렌더 호출 카운터. preTransform마다 1씩 증가. reset()으로 초기화되지 않음.
```

---

## 생성자

```java
public ModelEarsRenderer(ModelBase modelBase, int i, int j, ModelRotationRenderer baseRenderer)
{
    super(modelBase, i, j, baseRenderer);
}
```

SmartRenderModel에서의 호출:
```java
bipedEars = new ModelEarsRenderer(mp, 24, 0, bipedHead);
```
- 텍스처 UV: (24, 0)
- 부모: `bipedHead`

---

## 메서드 전체

### `beforeRender()`

```java
public void beforeRender()
{
    super.beforeRender(true);   // doPopPush = true, ignoreRender = false
}
```

- 파라미터 없음 (ModelCapeRenderer의 `beforeRender(EntityPlayer, float)`와 다름)
- `doPopPush = true` — doRender 직전에 `glPopMatrix + glPushMatrix` 실행
- `SmartRenderRender.renderSpecials()`에서 `modelBipedMain.bipedEars.beforeRender()` 로 호출

---

### `doRender(float f, boolean useParentTransformations)` — override

```java
@Override
public void doRender(float f, boolean useParentTransformations)
{
    reset();
    super.doRender(f, useParentTransformations);
}
```

- `reset()` 호출: `ModelRotationRenderer.reset()`으로 rotateAngle, rotationPoint, scale, offset 전부 기본값으로 초기화. **단, `_i` 필드는 reset() 대상이 아니므로 초기화되지 않음**
- `super.doRender()`: `ModelSpecialRenderer.doRender()` → `doPopPush=true`이면 `glPop+glPush`, 이후 `ModelRotationRenderer.doRender(f, true)` 호출

---

### `preTransform(float factor, boolean push)` — override

```java
@Override
public void preTransform(float factor, boolean push)
{
    super.preTransform(factor, push);

    int i = _i++ % 2;
    GL11.glTranslatef(0.375F * (i * 2 - 1), 0.0F, 0.0F);
    GL11.glTranslatef(0.0F, -0.375F, 0.0F);
    GL11.glScalef(1.333333F, 1.333333F, 1.333333F);
}
```

**`_i`에 따른 X 오프셋:**

| `_i % 2` | `i * 2 - 1` | X 오프셋 | 방향 |
|----------|-------------|---------|------|
| 0 | -1 | `0.375F * (-1) = -0.375F` | 왼쪽 |
| 1 | +1 | `0.375F * (+1) = +0.375F` | 오른쪽 |

**적용되는 변환 순서 (super.preTransform 이후):**
1. `glTranslatef(±0.375F, 0, 0)` — 좌우 배치
2. `glTranslatef(0, -0.375F, 0)` — 위쪽(-Y 방향) 이동
3. `glScalef(1.333333F, 1.333333F, 1.333333F)` — 균일 확대 (= 4/3배)

**수치 정리:**

| 수치 | 의미 |
|------|------|
| `0.375F` | 귀 좌우 오프셋 거리 |
| `-0.375F` | 귀 Y축 오프셋 (위쪽) |
| `1.333333F` | 귀 확대 비율 (≈ 4/3) |

---

### `canBeRandomBoxSource()` — override

```java
@Override
public boolean canBeRandomBoxSource()
{
    return false;
}
```

---

## `_i` 카운터 동작 상세

- `_i`는 인스턴스 변수로, `reset()`으로 초기화되지 않음
- `preTransform()`이 호출될 때마다 1씩 증가, `% 2`로 0/1 교대
- `doRender()` 한 번 호출 당 `preTransform()`은 `ModelRotationRenderer.doRender()` 내부에서 한 번 호출됨
- 따라서 호출 횟수에 따라:
  - 1번째 doRender → `_i=0` → X=-0.375 (왼쪽)
  - 2번째 doRender → `_i=1` → X=+0.375 (오른쪽)
  - 3번째 doRender → `_i=0` → X=-0.375 ...

---

## SmartRenderModel 생성 시 주의 사항

SmartRenderModel 생성자에서:
```java
bipedEars = new ModelEarsRenderer(mp, 24, 0, bipedHead);
copy(bipedCloak, originalBipedEars);   // 버그: 첫 인자가 bipedCloak이어야 할 자리에 bipedCloak이 들어가 있음
```
원본 코드에서 `copy(bipedCloak, originalBipedEars)` — bipedEars가 아닌 bipedCloak에 originalBipedEars의 geometry를 복사하는 버그가 있음(SmartRenderModel 리서치에서 이미 기록).

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `ModelSpecialRenderer` | 상위 클래스 |
| `GL11` (LWJGL) | `glTranslatef`, `glScalef` |

---

## 주요 관찰 사항

1. **`_i`는 reset() 비대상**: `doRender()`에서 `reset()`을 먼저 호출하지만 `_i`는 초기화되지 않으므로, 호출마다 누적 증가하여 좌우를 교대함.

2. **ModelCapeRenderer와 시그니처 차이**: `beforeRender()` — 파라미터 없음 vs `beforeRender(EntityPlayer, float)`. 둘 다 `doPopPush = true`로 호출.

3. **`doRender` 내 `reset()` 호출**: 귀는 별도의 애니메이션 오프셋이 없으므로 매번 기본 상태에서 시작. `preTransform`에서 X/Y translate + scale만 적용.

4. **1.21.1 이식 포인트**:
   - `GL11.glTranslatef` → `MatrixStack.translate()`
   - `GL11.glScalef` → `MatrixStack.scale()`
   - `_i` 카운터 로직은 MatrixStack 방식에서도 동일하게 유지 가능
   - `doPopPush`의 `glPop + glPush` 패턴 → `MatrixStack.pop() + push()`

---

## SmartRender net.smart.render 패키지 리서치 완료

이 파일로 `net.smart.render` 패키지의 16개 파일 리서치가 모두 완료됨.  
다음 대상: `net.smart.render.playerapi` 패키지 3개 파일.
