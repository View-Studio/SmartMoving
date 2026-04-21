# SmartCoreTransformation.java (net.smart.core) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/core/SmartCoreTransformation.java  
패키지: `net.smart.core`  
종류: `class`  
상속 없음

---

## 전체 소스

```java
package net.smart.core;

import java.util.*;

public class SmartCoreTransformation
{
    public final String className;
    public final String methodName;
    public final String[] parameterTypeNames;
    public final String returnType;
    public final String hookClassName;
    public final String beforeHookMethodName;
    public final String afterHookMethodName;

    public SmartCoreTransformation(String className, String methodName, String[] parameterTypeNames, String returnType, String hookClassName, String beforeHookMethodName, String afterHookMethodName)
    {
        this.className = className;
        this.methodName = methodName;
        this.parameterTypeNames = parameterTypeNames;
        this.returnType = returnType;
        this.hookClassName = hookClassName;
        this.beforeHookMethodName = beforeHookMethodName;
        this.afterHookMethodName = afterHookMethodName;
    }

    private static String getDescription(String type)
    {
        if(type == null || type == "void")
            return "V";
        if(type.endsWith("[]"))
            return "[" + getDescription(type.substring(0, type.length() - 2));
        if(type.equals("boolean"))
            return "Z";
        if(type.equals("byte"))
            return "B";
        if(type.equals("short"))
            return "S";
        if(type.equals("int"))
            return "I";
        if(type.equals("float"))
            return "F";
        if(type.equals("long"))
            return "J";
        if(type.equals("double"))
            return "D";
        return "L" + type.replace(".", "/") + ";";
    }

    private static String getDescriptions(String[] parameterTypes)
    {
        String result = "";
        for(int i=0; i<parameterTypes.length; i++)
            result += getDescription(parameterTypes[i]);
        return result + "";
    }

    private static String getMethodDescription(String[] parameterTypes, String resultType)
    {
        return "(" + getDescriptions(parameterTypes) + ")" + getDescription(resultType);
    }

    public String getMethodDesc()
    {
        return getMethodDescription(parameterTypeNames, returnType);
    }

    public String getHookClassDesc()
    {
        return hookClassName.replace(".", "/");
    }

    public String getHookMethodDesc()
    {
        List names = new ArrayList();
        names.add(className);
        for(int i=0; i<parameterTypeNames.length; i++)
            names.add(parameterTypeNames[i]);

        String[] result = new String[names.size()];
        names.toArray(result);
        return getMethodDescription(result, null);
    }
}
```

---

## 역할

`SmartCoreClassVisitor`/`SmartCoreMethodVisitor`에 전달되는 **변환 명세(descriptor) 객체**.  
ASM이 vanilla 클래스의 어느 메서드에, 어느 hook 클래스의 어느 메서드를 주입할지에 대한 모든 정보를 담는다.

생성(SmartCoreTransformer에서) → ClassVisitor에 전달 → MethodVisitor가 소비하는 단방향 데이터 흐름.

---

## 필드 (전부 `public final`)

```java
public final String className;           // 변환 대상 vanilla 클래스 (점 구분, e.g. "net.minecraft.network.NetHandlerPlayServer")
public final String methodName;          // 변환 대상 메서드 이름
public final String[] parameterTypeNames;// 변환 대상 메서드의 파라미터 타입 이름 배열 (Java primitive 이름 또는 FQN)
public final String returnType;          // 변환 대상 메서드의 반환 타입
public final String hookClassName;       // hook 정적 메서드가 있는 클래스 (점 구분)
public final String beforeHookMethodName;// before hook 메서드 이름 (null 허용 → 삽입 없음)
public final String afterHookMethodName; // after hook 메서드 이름 (null 허용 → 삽입 없음)
```

모두 `final` — 생성 후 변경 없음.

---

## 생성자

```java
public SmartCoreTransformation(String className, String methodName, String[] parameterTypeNames,
    String returnType, String hookClassName, String beforeHookMethodName, String afterHookMethodName)
{
    this.className = className;
    this.methodName = methodName;
    this.parameterTypeNames = parameterTypeNames;
    this.returnType = returnType;
    this.hookClassName = hookClassName;
    this.beforeHookMethodName = beforeHookMethodName;
    this.afterHookMethodName = afterHookMethodName;
}
```

단순 대입. 검증 없음.

---

## private static 메서드

### `getDescription(String type)` → String

```java
private static String getDescription(String type)
{
    if(type == null || type == "void")
        return "V";
    if(type.endsWith("[]"))
        return "[" + getDescription(type.substring(0, type.length() - 2));
    if(type.equals("boolean"))
        return "Z";
    if(type.equals("byte"))
        return "B";
    if(type.equals("short"))
        return "S";
    if(type.equals("int"))
        return "I";
    if(type.equals("float"))
        return "F";
    if(type.equals("long"))
        return "J";
    if(type.equals("double"))
        return "D";
    return "L" + type.replace(".", "/") + ";";
}
```

Java 타입 이름 문자열 → JVM 바이트코드 타입 디스크립터 변환.

**타입 → 디스크립터 매핑:**

| 입력 | 출력 | 비고 |
|------|------|------|
| `null` | `"V"` | |
| `"void"` | `"V"` | `type == "void"` — 문자열 참조 동등성 비교 (`==`) |
| `"xxx[]"` | `"[" + getDescription("xxx")` | 재귀. `type.length() - 2`로 `[]` 제거 |
| `"boolean"` | `"Z"` | |
| `"byte"` | `"B"` | |
| `"short"` | `"S"` | |
| `"int"` | `"I"` | |
| `"float"` | `"F"` | |
| `"long"` | `"J"` | |
| `"double"` | `"D"` | |
| 그 외 (클래스 FQN) | `"L" + type.replace(".", "/") + ";"` | e.g. `"net.minecraft.Foo"` → `"Lnet/minecraft/Foo;"` |

**주의:** `type == "void"` 는 `equals`가 아닌 `==` (참조 동등성). 리터럴 문자열 인터닝에 의존.  
실제 사용 시 `null`을 전달하거나 리터럴 `"void"` 문자열을 전달하므로 동작하지만, 일반적으로 안전하지 않은 패턴.

---

### `getDescriptions(String[] parameterTypes)` → String

```java
private static String getDescriptions(String[] parameterTypes)
{
    String result = "";
    for(int i=0; i<parameterTypes.length; i++)
        result += getDescription(parameterTypes[i]);
    return result + "";
}
```

파라미터 타입 배열 전체를 연결한 JVM 디스크립터 문자열 생성.  
`return result + ""` — `result`가 이미 String이므로 불필요한 연결이지만 동작에는 영향 없음.

**예시:** `["net.minecraft.network.NetHandlerPlayServer", "net.minecraft.network.play.client.C03PacketPlayer"]`  
→ `"Lnet/minecraft/network/NetHandlerPlayServer;Lnet/minecraft/network/play/client/C03PacketPlayer;"`

---

### `getMethodDescription(String[] parameterTypes, String resultType)` → String

```java
private static String getMethodDescription(String[] parameterTypes, String resultType)
{
    return "(" + getDescriptions(parameterTypes) + ")" + getDescription(resultType);
}
```

JVM 메서드 디스크립터 형식 `(파라미터들)반환타입` 생성.

**예시:** `(["int"], "void")` → `"(I)V"`

---

## public 메서드

### `getMethodDesc()` → String

```java
public String getMethodDesc()
{
    return getMethodDescription(parameterTypeNames, returnType);
}
```

변환 대상 vanilla 메서드의 JVM 메서드 디스크립터.  
`SmartCoreClassVisitor.visitMethod()`에서 메서드 매칭 시 사용.

---

### `getHookClassDesc()` → String

```java
public String getHookClassDesc()
{
    return hookClassName.replace(".", "/");
}
```

hook 클래스의 JVM 내부 이름 (슬래시 구분).  
`SmartCoreMethodVisitor.invokeHook()`의 `INVOKESTATIC` 명령어에서 사용.

**예시:** `"net.smart.core.SmartCoreEventHandler"` → `"net/smart/core/SmartCoreEventHandler"`

---

### `getHookMethodDesc()` → String

```java
public String getHookMethodDesc()
{
    List names = new ArrayList();
    names.add(className);
    for(int i=0; i<parameterTypeNames.length; i++)
        names.add(parameterTypeNames[i]);

    String[] result = new String[names.size()];
    names.toArray(result);
    return getMethodDescription(result, null);
}
```

hook 메서드(before/after 공용)의 JVM 메서드 디스크립터.

**파라미터 구성 규칙:**
1. `className` (변환 대상 클래스) → hook 메서드 첫 번째 파라미터 (`this`에 해당)
2. `parameterTypeNames` 전체 → hook 메서드 나머지 파라미터 (vanilla 메서드 파라미터 그대로)
3. 반환 타입: `null` → `"V"` (항상 void)

**예시 — `processPlayer(C03PacketPlayer)` 변환:**
- `className = "net.minecraft.network.NetHandlerPlayServer"`
- `parameterTypeNames = ["net.minecraft.network.play.client.C03PacketPlayer"]`
- `getHookMethodDesc()` 결과:  
  `"(Lnet/minecraft/network/NetHandlerPlayServer;Lnet/minecraft/network/play/client/C03PacketPlayer;)V"`

이는 `SmartCoreEventHandler.NetHandlerPlayServer_beforeProcessPlayer(NetHandlerPlayServer, C03PacketPlayer)` 시그니처와 일치.

---

## import

```java
import java.util.*;  // List, ArrayList
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartCoreTransformer` | 이 객체를 생성 (추정 아닌 구조적 필연 — SmartCoreTransformer가 ClassVisitor에 SmartCoreTransformation을 전달) |
| `SmartCoreClassVisitor` | `getMethodDesc()`로 vanilla 메서드 매칭 |
| `SmartCoreMethodVisitor` | `getHookClassDesc()`, `getHookMethodDesc()`, `beforeHookMethodName`, `afterHookMethodName`, `parameterTypeNames` 소비 |

---

## 전체 데이터 흐름 (이 파일에서 확인된 것)

```
SmartCoreTransformer (생성자 호출)
  → SmartCoreTransformation 객체 (변환 명세 보관)
       ├─ SmartCoreClassVisitor.visitMethod()
       │    └─ transformation.methodName + getMethodDesc() → 메서드 매칭
       └─ SmartCoreMethodVisitor (생성자 파라미터)
            ├─ transformation.beforeHookMethodName → before hook 이름
            ├─ transformation.afterHookMethodName  → after hook 이름
            ├─ transformation.parameterTypeNames   → 파라미터 로드 반복
            ├─ getHookClassDesc()                  → INVOKESTATIC 클래스
            └─ getHookMethodDesc()                 → INVOKESTATIC 메서드 디스크립터
```

---

## 주요 관찰 사항

1. **`type == "void"` 참조 비교**: `getDescription()`에서 `"void"` 문자열을 `==`로 비교. Java 문자열 인터닝으로 리터럴 비교 시 동작하지만, `new String("void")`처럼 생성한 경우 `false`. 실제 사용처(SmartCoreTransformer)에서 리터럴로 전달하는 경우에만 안전.

2. **raw List 사용**: `getHookMethodDesc()`에서 raw `List`/`ArrayList` 사용 (제네릭 없음). Java 1.5 이전 스타일 또는 의도적 raw 타입.

3. **before/after 공용 hook 디스크립터**: `getHookMethodDesc()`는 before, after 모두 동일 시그니처를 반환. 두 hook 메서드가 항상 같은 파라미터를 받는 설계.

4. **배열 타입 지원**: `getDescription()`이 `type.endsWith("[]")` 분기를 갖고 재귀 처리. 파라미터에 배열이 있어도 올바른 디스크립터 생성 (`"int[]"` → `"[I"`).

5. **1.21.1 이식**: 이 클래스 전체 불필요. Mixin이 타입 디스크립터를 자동 처리. 변환 명세 개념 자체가 Mixin 어노테이션(`@Inject`, `@At`, `method`, `target`)으로 대체.
