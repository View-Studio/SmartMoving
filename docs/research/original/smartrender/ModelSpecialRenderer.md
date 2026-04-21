# ModelSpecialRenderer.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/ModelSpecialRenderer.java  
패키지: `net.smart.render`  
상속: `ModelSpecialRenderer extends ModelRotationRenderer`

---

## 역할

`ModelCapeRenderer`(망토)와 `ModelEarsRenderer`(귀)의 공통 베이스 클래스.  
`ignoreRender = true`를 기본값으로 설정하여 평소에는 렌더링되지 않으며,  
`beforeRender()` / `afterRender()` 쌍으로 렌더링 구간을 명시적으로 열고 닫는 패턴을 구현한다.  
`doPopPush = true`일 때 렌더 직전에 `glPopMatrix + glPushMatrix`로 GL 변환 스택을 리셋한다.

- **실행 위치**: 클라이언트 전용

---

## 필드

```java
public boolean doPopPush;   // 기본값 false (Java boolean 기본값)
```

상속 필드 중 동작에 중요한 것:
- `ignoreRender` — 생성자에서 `true`로 초기화. beforeRender()에서 false, afterRender()에서 true로 토글

---

## 생성자

```java
public ModelSpecialRenderer(ModelBase modelBase, int i, int j, ModelRotationRenderer baseRenderer)
{
    super(modelBase, i, j, baseRenderer);
    ignoreRender = true;   // 기본적으로 렌더 비활성화
}
```

- `ModelRotationRenderer` 생성자 호출 후 `ignoreRender = true`로 강제 설정
- 평소에는 `render()` 호출이 있어도 실제 렌더링이 일어나지 않음

---

## 메서드 전체

### `beforeRender(boolean popPush)`

```java
public void beforeRender(boolean popPush)
{
    doPopPush = popPush;
    ignoreRender = false;   // 렌더 활성화
}
```

- 렌더링 구간 시작 — `ignoreRender = false`로 설정하여 다음 `render()` 호출 시 실제 렌더 허용
- `doPopPush`: true이면 `doRender()`에서 GL 스택 리셋 실행

---

### `doRender(float f, boolean useParentTransformations)` — override

```java
@Override
public void doRender(float f, boolean useParentTransformations)
{
    if(doPopPush)
    {
        GL11.glPopMatrix();
        GL11.glPushMatrix();
    }
    super.doRender(f, true);   // useParentTransformations 항상 true 강제
}
```

- `doPopPush = true`이면: `glPopMatrix()` 후 `glPushMatrix()` — 현재 GL 매트릭스 스택의 최상단을 교체하여 이전 변환 누적을 초기화하는 효과
- `super.doRender(f, true)` — 파라미터 `useParentTransformations`를 무시하고 항상 `true`로 강제. 즉 부모(base) 변환 체인을 항상 적용

---

### `afterRender()`

```java
public void afterRender()
{
    ignoreRender = true;    // 렌더 비활성화 복원
    doPopPush = false;
}
```

- 렌더링 구간 종료

---

## 사용 흐름

`SmartRenderRender.renderSpecials()`에서:

```java
modelBipedMain.bipedEars.beforeRender();               // ignoreRender=false
modelBipedMain.bipedCloak.beforeRender(entityplayer, f); // ignoreRender=false
irp.superRenderSpecials(entityplayer, f);              // vanilla preRenderCallback → 내부에서 ears/cloak 렌더
modelBipedMain.bipedCloak.afterRender();               // ignoreRender=true
modelBipedMain.bipedEars.afterRender();                // ignoreRender=true
```

- `beforeRender()` 호출로 창을 열고, vanilla `superRenderSpecials()` 안에서 렌더가 발생하고, `afterRender()`로 창을 닫는 구조
- `ModelEarsRenderer.beforeRender()` 시그니처: `beforeRender()` (파라미터 없음 — [미확인, ModelEarsRenderer 리서치에서 확인])
- `ModelCapeRenderer.beforeRender(entityplayer, f)` 시그니처: 추가 파라미터 있음 — [미확인, ModelCapeRenderer 리서치에서 확인]

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `ModelRotationRenderer` | 상위 클래스 |
| `GL11` (LWJGL) | `glPopMatrix`, `glPushMatrix` |
| `ModelCapeRenderer` | 하위 클래스 (망토) |
| `ModelEarsRenderer` | 하위 클래스 (귀) |

---

## 주요 관찰 사항

1. **ignoreRender 토글 패턴**: 생성자에서 `ignoreRender = true`로 잠금 → `beforeRender()`로 열기 → vanilla 렌더 호출 → `afterRender()`로 닫기. 이 창이 열린 동안만 실제 렌더링 발생.

2. **`doPopPush` GL 스택 리셋**: vanilla `preRenderCallback`(= superRenderSpecials) 내부에서 GL 변환이 이미 쌓여 있을 때, `glPop + glPush`로 최상단 행렬을 교체하여 이전 누적 변환을 초기화. 언제 `true`/`false`로 호출되는지는 `ModelCapeRenderer` 리서치에서 확인 필요 — [미확인]

3. **`super.doRender(f, true)` 강제**: `useParentTransformations`를 항상 `true`로 강제하므로, 부모(bipedBreast 등)의 변환 체인이 항상 적용됨.

4. **1.21.1 이식 포인트**: `glPopMatrix / glPushMatrix` → `MatrixStack.pop() / push()`. 단, `doRender` 시점에 MatrixStack 인스턴스에 접근하는 방법이 필요 — 1.7.10은 GL 글로벌 상태이지만 1.21.1은 메서드 파라미터로 전달되는 구조.
