# SmartRenderInstall.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/SmartRenderInstall.java  
패키지: `net.smart.render`  
종류: 일반 클래스 (상속 없음)

---

## 역할

vanilla `ModelRenderer`의 private 필드/메서드에 reflection으로 접근할 때 사용하는 난독화 이름 매핑 상수 홀더.  
`ModelRotationRenderer`에서 이 상수들을 사용하여 `Reflect.GetField()` / `Reflect.GetMethod()`를 호출한다.  
메서드 없음. 생성자 없음(기본 생성자만).

- **실행 위치**: 클라이언트 전용 (ModelRenderer는 클라이언트 전용)

---

## 필드 전체

```java
public final static Name ModelRenderer_compiled =
    new Name("compiled", "field_78812_q", "t");

public final static Name ModelRenderer_compileDisplayList =
    new Name("compileDisplayList", "func_78788_d", "d");

public final static Name ModelRenderer_displayList =
    new Name("displayList", "field_78811_r", "u");
```

`Name` 생성자 파라미터 순서 (net.smart.utilities.Name):

| 인덱스 | 의미 | 예시 |
|--------|------|------|
| 0 | 개발 환경 이름 (deobf) | `"compiled"` |
| 1 | Searge/MCP 이름 (SRG) | `"field_78812_q"` |
| 2 | 프로덕션 난독화 이름 | `"t"` |

---

## 각 Name 상수 상세

### `ModelRenderer_compiled`
```java
new Name("compiled", "field_78812_q", "t")
```
- 대상: `net.minecraft.client.model.ModelRenderer`의 `compiled` boolean 필드
- `ModelRotationRenderer.UpdateCompiled()`에서 `Reflect.GetField(_compiled, this)`로 읽음
- 역할: displayList 컴파일 여부 플래그

### `ModelRenderer_compileDisplayList`
```java
new Name("compileDisplayList", "func_78788_d", "d")
```
- 대상: `net.minecraft.client.model.ModelRenderer`의 `compileDisplayList(float)` 메서드
- `ModelRotationRenderer.preRender()`에서 `Reflect.Invoke(_compileDisplayList, this, f)`로 호출
- 역할: GL displayList 컴파일 실행

### `ModelRenderer_displayList`
```java
new Name("displayList", "field_78811_r", "u")
```
- 대상: `net.minecraft.client.model.ModelRenderer`의 `displayList` int 필드
- `ModelRotationRenderer.UpdateDisplayList()`에서 `Reflect.GetField(_displayList, this)`로 읽음
- 역할: 컴파일된 GL displayList ID

---

## 사용 위치

`ModelRotationRenderer.java` 클래스 레벨 static 초기화:

```java
private static Field _compiled =
    Reflect.GetField(ModelRenderer.class, SmartRenderInstall.ModelRenderer_compiled);

private static Method _compileDisplayList =
    Reflect.GetMethod(ModelRenderer.class, SmartRenderInstall.ModelRenderer_compileDisplayList, float.class);

private static Field _displayList =
    Reflect.GetField(ModelRenderer.class, SmartRenderInstall.ModelRenderer_displayList);
```

---

## import

```java
import net.smart.utilities.*;   // Name 클래스
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `net.smart.utilities.Name` | 난독화 이름 3종을 하나로 묶는 래퍼 클래스 |
| `ModelRotationRenderer` | 이 상수들의 소비처 |

---

## 주요 관찰 사항

1. **난독화 대응 방식**: 개발 환경·SRG·프로덕션 세 가지 이름을 `Name` 객체에 묶어 보관. `Reflect.GetField()`가 런타임 환경에 따라 적절한 이름을 선택하는 구조로 추정됨 — [미확인, `net.smart.utilities.Name` / `Reflect` 리서치 후 확인]

2. **1.21.1 이식 포인트**: 이 클래스 전체가 1.7.10 전용. 1.21.1에서는 `ModelRenderer`(GL displayList 방식)가 존재하지 않으므로, `compiled`/`displayList`/`compileDisplayList` 개념 자체가 없어진다. `ModelRotationRenderer`를 1.21.1 방식으로 재구현하면 이 클래스는 불필요.
