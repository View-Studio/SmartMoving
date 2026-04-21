# ModelRotationRenderer.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/ModelRotationRenderer.java  
패키지: `net.smart.render`  
상속: `ModelRotationRenderer extends ModelRenderer`

---

## 역할

SmartRender의 모든 모델 파트 타입. vanilla `ModelRenderer`를 상속하여 다음을 추가한다:
- 부모(base) 변환 재귀 적용 구조 (계층 트리의 실제 변환 로직)
- 6가지 회전 순서 지원 (XYZ, XZY, YXZ, YZX, ZXY, ZYX)
- scale/offset 필드
- ignoreBase / ignoreRender / forceRender 플래그
- ignoreSuperRotation (GL_MODELVIEW_MATRIX 에서 translation 만 추출하여 WorldSpace 회전)
- fade 보간 시스템 (RendererData previous에 이전 프레임 상태 저장 후 보간)
- reflection으로 부모 클래스의 private `compiled`, `displayList`, `compileDisplayList` 접근

- **실행 위치**: 클라이언트 전용

---

## 상수

```java
protected final static float RadiantToAngle = SmartRenderUtilities.RadiantToAngle;
protected final static float Whole = SmartRenderUtilities.Whole;
protected final static float Half = SmartRenderUtilities.Half;
```

실제 값은 `SmartRenderUtilities` 리서치 후 확인 필요 — [미확인]

---

## 회전 순서 상수 (static int)

```java
public static int XYZ = 0;
public static int XZY = 1;
public static int YXZ = 2;
public static int YZX = 3;
public static int ZXY = 4;
public static int ZYX = 5;
```

기본값: `XYZ`

---

## 필드

```java
protected ModelRotationRenderer base;      // 부모 노드 (계층 트리)

public boolean ignoreRender;              // true이면 render()에서 렌더 안 함 (forceRender 없으면)
public boolean forceRender;              // true이면 ignoreRender/ignoreBase 무시하고 강제 렌더

public boolean compiled;                 // displayList 컴파일 여부 (reflection으로 부모 값 동기화)
public int displayList;                  // GL displayList ID (reflection으로 부모 값 동기화)
public int rotationOrder;               // 회전 순서 (기본 XYZ=0)

public float scaleX;                    // 기본값 1.0F
public float scaleY;                    // 기본값 1.0F
public float scaleZ;                    // 기본값 1.0F

public boolean ignoreBase;              // true이면 부모 변환 체인을 사용하지 않음
public boolean ignoreSuperRotation;     // true이면 GL_MODELVIEW_MATRIX에서 translation만 추출

public boolean fadeEnabled;             // 생성자에서 false로 초기화 (bipedOuter에서만 true)

// fade 보간 활성화 플래그 (reset()에서 전부 false로 초기화)
public boolean fadeOffsetX, fadeOffsetY, fadeOffsetZ;
public boolean fadeRotateAngleX, fadeRotateAngleY, fadeRotateAngleZ;
public boolean fadeRotationPointX, fadeRotationPointY, fadeRotationPointZ;

public RendererData previous;           // 이전 프레임 상태 저장 (null이면 fade 없음)

// 정적 — ignoreSuperRotation용 GL_MODELVIEW_MATRIX 읽기
private static FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
private static float[] array = new float[16];

// reflection으로 부모 클래스 접근
private static Field _compiled = Reflect.GetField(ModelRenderer.class, SmartRenderInstall.ModelRenderer_compiled);
private static Method _compileDisplayList = Reflect.GetMethod(ModelRenderer.class, SmartRenderInstall.ModelRenderer_compileDisplayList, float.class);
private static Field _displayList = Reflect.GetField(ModelRenderer.class, SmartRenderInstall.ModelRenderer_displayList);
```

---

## 생성자

```java
public ModelRotationRenderer(ModelBase modelBase, int i, int j, ModelRotationRenderer baseRenderer)
{
    super(modelBase, i, j);
    rotationOrder = XYZ;
    compiled = false;

    base = baseRenderer;
    if(base != null)
        base.addChild(this);   // 부모에 자신을 자식으로 등록

    scaleX = 1.0F;
    scaleY = 1.0F;
    scaleZ = 1.0F;

    fadeEnabled = false;
}
```

- `baseRenderer`가 null이 아니면 `base.addChild(this)` 호출 → vanilla childModels 리스트에 등록됨

---

## 메서드 전체

### `render(float f)` — override

```java
public void render(float f)
{
    if((!ignoreRender && !ignoreBase) || forceRender)
        doRender(f, ignoreBase);
}
```

조건 정리:

| ignoreRender | ignoreBase | forceRender | 동작 |
|---|---|---|---|
| false | false | - | `doRender(f, false)` — 부모 변환 포함 정상 렌더 |
| false | true | false | 렌더 안 함 (renderIgnoreBase로 처리) |
| true | any | false | 렌더 안 함 |
| any | any | true | `doRender(f, ignoreBase)` 강제 실행 |

---

### `renderIgnoreBase(float f)`

```java
public void renderIgnoreBase(float f)
{
    if(ignoreBase)
        doRender(f, false);
}
```

- `ignoreBase=true`인 파트만 렌더. 부모 변환 없이(`useParentTransformations=false`) 렌더.
- SmartRenderModel.render()에서 각 파트별로 명시적으로 호출됨.

---

### `doRender(float f, boolean useParentTransformations)`

```java
public void doRender(float f, boolean useParentTransformations)
{
    if(!preRender(f)) return;
    preTransforms(f, true, useParentTransformations);
    GL11.glCallList(displayList);
    if(childModels != null)
        for(int i = 0; i < childModels.size(); i++)
            ((ModelRenderer)childModels.get(i)).render(f);
    postTransforms(f, true, useParentTransformations);
}
```

1. preRender로 유효성 확인 + displayList 컴파일 보장
2. preTransforms (부모 → 자신 순으로 변환 적용)
3. glCallList (geometry 렌더)
4. childModels 렌더 (각 자식의 render(f) 호출)
5. postTransforms (자신 → 부모 순으로 변환 복원)

---

### `preRender(float f)`

```java
public boolean preRender(float f)
{
    if(isHidden) return false;
    if(!showModel) return false;

    if(!compiled) UpdateCompiled();

    if(!compiled)
    {
        Reflect.Invoke(_compileDisplayList, this, f);  // 부모의 private compileDisplayList 호출
        UpdateDisplayList();
        compiled = true;
    }

    return true;
}
```

---

### `preTransforms(float f, boolean push, boolean useParentTransformations)`

```java
public void preTransforms(float f, boolean push, boolean useParentTransformations)
{
    if(base != null && !ignoreBase && useParentTransformations)
        base.preTransforms(f, push, true);   // 부모 먼저 재귀 호출
    preTransform(f, push);
}
```

- 부모부터 시작해서 자신까지 변환을 쌓음 (루트 → 자신 순서)

---

### `preTransform(float f, boolean push)`

```java
public void preTransform(float f, boolean push)
{
    if(rotateAngleX != 0.0F || rotateAngleY != 0.0F || rotateAngleZ != 0.0F || ignoreSuperRotation)
    {
        if(push) GL11.glPushMatrix();

        GL11.glTranslatef(rotationPointX * f, rotationPointY * f, rotationPointZ * f);

        if(ignoreSuperRotation)
        {
            // GL_MODELVIEW_MATRIX에서 현재 translation만 추출
            buffer.rewind();
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
            buffer.get(array);
            GL11.glLoadIdentity();
            GL11.glTranslatef(array[12] / array[15], array[13] / array[15], array[14] / array[15]);
        }

        rotate(rotationOrder, rotateAngleX, rotateAngleY, rotateAngleZ);

        GL11.glScalef(scaleX, scaleY, scaleZ);
        GL11.glTranslatef(offsetX, offsetY, offsetZ);
    }
    else if(rotationPointX != 0.0F || rotationPointY != 0.0F || rotationPointZ != 0.0F
         || scaleX != 1.0F || scaleY != 1.0F || scaleZ != 1.0F
         || offsetX != 0.0F || offsetY != 0.0F || offsetZ != 0.0F)
    {
        // 회전 없음. push/pop 없이 translate+scale만
        GL11.glTranslatef(rotationPointX * f, rotationPointY * f, rotationPointZ * f);
        GL11.glScalef(scaleX, scaleY, scaleZ);
        GL11.glTranslatef(offsetX, offsetY, offsetZ);
    }
    // 모든 값이 기본값이면 GL 호출 없음
}
```

**분기 요약:**

| 조건 | push | GL 호출 |
|---|---|---|
| rotateAngle 하나라도 != 0 또는 ignoreSuperRotation | O | translate → (ignoreSuperRotation이면 identity+position) → rotate → scale → offsetTranslate |
| rotateAngle 전부 0, 나머지 하나라도 != 기본값 | X | translate → scale → offsetTranslate |
| 전부 기본값 | X | GL 호출 없음 |

---

### `rotate(int rotationOrder, float X, float Y, float Z)` — private static

OpenGL은 행렬을 오른쪽에서 곱하므로, 코드 마지막 glRotatef가 가장 먼저 적용된다.  
아래 표는 각 rotationOrder에 대한 **GL 호출 코드 순서** → **실제 적용 순서**:

```java
// 코드 순서 (각 if조건별 GL 호출):
// [1] (ZXY) → glRotatef(Y)
// [2] (YXZ) → glRotatef(Z)
// [3] (YZX|YXZ|ZXY|ZYX) → glRotatef(X)
// [4] (XZY|ZYX) → glRotatef(Y)
// [5] (XYZ|XZY|YZX|ZXY|ZYX) → glRotatef(Z)
// [6] (XYZ|YXZ|YZX) → glRotatef(Y)
// [7] (XYZ|XZY) → glRotatef(X)
```

| rotationOrder | GL 호출 순서(코드순) | OpenGL 적용 순서(역순) |
|---|---|---|
| XYZ (0) | Z[5] → Y[6] → X[7] | X → Y → Z |
| XZY (1) | Y[4] → Z[5] → X[7] | X → Z → Y |
| YXZ (2) | Z[2] → X[3] → Y[6] | Y → X → Z |
| YZX (3) | X[3] → Z[5] → Y[6] | Y → Z → X |
| ZXY (4) | Y[1] → X[3] → Z[5] | Z → X → Y |
| ZYX (5) | X[3] → Y[4] → Z[5] | Z → Y → X |

---

### `postTransform(float f, boolean pop)`

```java
public void postTransform(float f, boolean pop)
{
    if(rotateAngleX != 0.0F || rotateAngleY != 0.0F || rotateAngleZ != 0.0F || ignoreSuperRotation)
    {
        if(pop) GL11.glPopMatrix();
    }
    else if(rotationPointX != 0.0F || rotationPointY != 0.0F || rotationPointZ != 0.0F
         || scaleX != 1.0F || scaleY != 1.0F || scaleZ != 1.0F
         || offsetX != 0.0F || offsetY != 0.0F || offsetZ != 0.0F)
    {
        GL11.glTranslatef(-offsetX, -offsetY, -offsetZ);
        GL11.glScalef(1F / scaleX, 1F / scaleY, 1F / scaleZ);
        GL11.glTranslatef(-rotationPointX * f, -rotationPointY * f, -rotationPointZ * f);
    }
}
```

- 회전이 있으면 glPopMatrix (preTransform에서 Push했으므로)
- 회전 없이 translate/scale만 했으면 수동으로 역변환

---

### `postTransforms(float f, boolean pop, boolean useParentTransformations)`

```java
public void postTransforms(float f, boolean pop, boolean useParentTransformations)
{
    postTransform(f, pop);
    if(base != null && !ignoreBase && useParentTransformations)
        base.postTransforms(f, pop, true);
}
```

- 자신 복원 후 부모 복원 (preTransforms와 반대 순서)

---

### `reset()`

```java
public void reset()
{
    rotationOrder = XYZ;
    scaleX = scaleY = scaleZ = 1.0F;
    rotationPointX = rotationPointY = rotationPointZ = 0F;
    rotateAngleX = rotateAngleY = rotateAngleZ = 0F;
    ignoreBase = false;
    ignoreSuperRotation = false;
    forceRender = false;
    offsetX = offsetY = offsetZ = 0;
    fadeOffsetX = fadeOffsetY = fadeOffsetZ = false;
    fadeRotateAngleX = fadeRotateAngleY = fadeRotateAngleZ = false;
    fadeRotationPointX = fadeRotationPointY = fadeRotationPointZ = false;
    previous = null;
}
```

---

### `renderWithRotation(float f)` — override

```java
public void renderWithRotation(float f)
{
    boolean update = !compiled;
    super.renderWithRotation(f);
    if(update) UpdateLocals();
}
```

---

### `postRender(float f)` — override

```java
public void postRender(float f)
{
    boolean update = !compiled;
    if(!preRender(f)) return;
    if(update) UpdateLocals();
    preTransforms(f, false, true);
}
```

---

### `fadeStore(float totalTime)`

현재 프레임 상태를 `previous`(RendererData)에 저장:

```java
public void fadeStore(float totalTime)
{
    if(previous != null)
    {
        previous.offsetX = offsetX;
        previous.offsetY = offsetY;
        previous.offsetZ = offsetZ;
        previous.rotateAngleX = rotateAngleX;
        previous.rotateAngleY = rotateAngleY;
        previous.rotateAngleZ = rotateAngleZ;
        previous.rotationPointX = rotationPointX;
        previous.rotationPointY = rotationPointY;
        previous.rotationPointZ = rotationPointZ;
        previous.totalTime = totalTime;
    }
}
```

---

### `fadeIntermediate(float totalTime)`

`totalTime - previous.totalTime <= 2F` 조건일 때만 보간 적용:

```java
public void fadeIntermediate(float totalTime)
{
    if(previous != null && totalTime - previous.totalTime <= 2F)
    {
        offsetX         = GetIntermediatePosition(previous.offsetX, offsetX, fadeOffsetX, previous.totalTime, totalTime);
        offsetY         = GetIntermediatePosition(previous.offsetY, offsetY, fadeOffsetY, previous.totalTime, totalTime);
        offsetZ         = GetIntermediatePosition(previous.offsetZ, offsetZ, fadeOffsetZ, previous.totalTime, totalTime);
        rotateAngleX    = GetIntermediateAngle(previous.rotateAngleX, rotateAngleX, fadeRotateAngleX, previous.totalTime, totalTime);
        rotateAngleY    = GetIntermediateAngle(previous.rotateAngleY, rotateAngleY, fadeRotateAngleY, previous.totalTime, totalTime);
        rotateAngleZ    = GetIntermediateAngle(previous.rotateAngleZ, rotateAngleZ, fadeRotateAngleZ, previous.totalTime, totalTime);
        rotationPointX  = GetIntermediatePosition(previous.rotationPointX, rotationPointX, fadeRotationPointX, previous.totalTime, totalTime);
        rotationPointY  = GetIntermediatePosition(previous.rotationPointY, rotationPointY, fadeRotationPointY, previous.totalTime, totalTime);
        rotationPointZ  = GetIntermediatePosition(previous.rotationPointZ, rotationPointZ, fadeRotationPointZ, previous.totalTime, totalTime);
    }
}
```

---

### `GetIntermediatePosition(prevPosition, shouldPosition, fade, lastTotalTime, totalTime)` — private static

```java
private static float GetIntermediatePosition(float prevPosition, float shouldPosition,
                                              boolean fade, float lastTotalTime, float totalTime)
{
    if(!fade || shouldPosition == prevPosition)
        return shouldPosition;
    return prevPosition + (shouldPosition - prevPosition) * (totalTime - lastTotalTime) * 0.2F;
}
```

- `fade=false` 또는 값이 동일하면 현재 값 그대로 반환
- 보간: `prev + (current - prev) * timeDelta * 0.2F`

---

### `GetIntermediateAngle(prevAngle, shouldAngle, fade, lastTotalTime, totalTime)` — private static

```java
private static float GetIntermediateAngle(float prevAngle, float shouldAngle,
                                           boolean fade, float lastTotalTime, float totalTime)
{
    if(!fade || shouldAngle == prevAngle)
        return shouldAngle;

    // 0~Whole 범위로 정규화
    while(prevAngle >= Whole) prevAngle -= Whole;
    while(prevAngle < 0F)    prevAngle += Whole;
    while(shouldAngle >= Whole) shouldAngle -= Whole;
    while(shouldAngle < 0F)     shouldAngle += Whole;

    // 최단 경로 선택 (180도 이상이면 반대 방향)
    if(shouldAngle > prevAngle && (shouldAngle - prevAngle) > Half)
        prevAngle += Whole;
    if(shouldAngle < prevAngle && (prevAngle - shouldAngle) > Half)
        shouldAngle += Whole;

    return prevAngle + (shouldAngle - prevAngle) * (totalTime - lastTotalTime) * 0.2F;
}
```

- 각도를 0~2π로 정규화 후 최단 경로로 보간
- 보간 공식: `prev + (current - prev) * timeDelta * 0.2F` (Position과 동일)

---

### `canBeRandomBoxSource()`

```java
public boolean canBeRandomBoxSource() { return true; }
```

SmartRenderModel.getRandomBox() 에서 사용. 기본적으로 true 반환.

---

### private UpdateLocals / UpdateCompiled / UpdateDisplayList

```java
private void UpdateLocals()      { UpdateCompiled(); if(compiled) UpdateDisplayList(); }
private void UpdateCompiled()    { compiled = (Boolean)Reflect.GetField(_compiled, this); }
private void UpdateDisplayList() { displayList = (Integer)Reflect.GetField(_displayList, this); }
```

reflection으로 부모 `ModelRenderer`의 private 필드 `compiled`, `displayList`를 읽어 로컬 필드에 동기화.

---

## 의존 관계

| 의존 대상 | 용도 |
|---|---|
| `ModelRenderer` (vanilla) | 상위 클래스 — rotationPointX/Y/Z, rotateAngleX/Y/Z, offsetX/Y/Z, showModel, isHidden, childModels, cubeList 상속 |
| `SmartRenderUtilities` | RadiantToAngle, Whole, Half 상수 |
| `SmartRenderInstall` | reflection에서 사용하는 vanilla 필드/메서드 이름 문자열 |
| `Reflect` (net.smart.utilities) | GetField, GetMethod, Invoke 유틸 |
| `RendererData` | fade 시스템의 이전 프레임 상태 저장 |
| `GL11` (LWJGL) | 모든 GL 변환/렌더 호출 |
| `BufferUtils` (LWJGL) | GL_MODELVIEW_MATRIX 읽기용 FloatBuffer |

---

## 주요 관찰 사항

1. **계층 변환 재귀**: `preTransforms()`가 루트까지 재귀 올라가서 부모 변환을 모두 누적한 뒤 자신 변환을 추가. `postTransforms()`는 역순. 이 구조 덕분에 각 파트가 독립적으로 rotationPoint를 가질 수 있음.

2. **ignoreBase의 두 가지 사용**:
   - `render()`: ignoreBase=true이면 렌더 스킵 (부모 변환 없이 렌더하는 `renderIgnoreBase()`를 별도 경로로 사용)
   - `preTransforms()`: ignoreBase=true이면 base.preTransforms 재귀 호출 안 함

3. **ignoreSuperRotation**: glLoadIdentity 후 현재 modelview matrix의 translation 성분만 추출해 복원. 플레이어 회전과 무관하게 WorldSpace에서 정렬된 회전이 필요할 때 사용.

4. **fade 보간**: `previous == null`이면 보간 없음. `totalTime - previous.totalTime > 2F`이면 보간 스킵 (2틱 이상 경과하면 점프로 처리). 보간 계수 `* 0.2F * timeDelta`.

5. **회전 없을 때 push/pop 없음**: `preTransform()`에서 rotateAngle이 전부 0이면 glPushMatrix 없이 translate만. `postTransform()`에서는 수동 역변환. 이는 push/pop 스택 비용 절감.

6. **reflection 의존**: vanilla `ModelRenderer`의 `compiled`, `displayList`, `compileDisplayList`가 private이므로 reflection으로 접근. 필드 이름은 `SmartRenderInstall`에서 제공 (obfuscation 대응).

7. **1.21.1 이식 포인트**: GL11 직접 호출 전체가 교체 대상. 1.21.1은 MatrixStack 기반. `glPushMatrix/PopMatrix → matrices.push()/pop()`, `glTranslatef → matrices.translate()`, `glRotatef → matrices.multiply(RotationAxis....)`, `glScalef → matrices.scale()`. `ignoreSuperRotation`의 GL_MODELVIEW_MATRIX 읽기는 1.21.1에서 다른 방법 필요.
