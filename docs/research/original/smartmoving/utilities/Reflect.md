# Reflect.java (net.smart.utilities) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/utilities/Reflect.java  
패키지: `net.smart.utilities`  
종류: `class` (모든 메서드 static)  
전체 라인: 268줄  
비고: 헤더 주석에 "Smart Render and Smart Moving" — 두 모드 공유 파일

---

## 역할

`Name` 객체(obfuscated / forgefuscated / deobfuscated 3가지 이름)를 받아 리플렉션으로 클래스/필드/메서드에 접근하는 유틸리티. 난독화 이름 우선 → SRG 이름 → 평문 이름 순으로 시도한다.

---

## import

```java
import java.lang.reflect.*;
```

---

## 메서드 목록

---

### `NewInstance(Class<?> base, Name name)` (public static)

```java
public static Object NewInstance(Class<?> base, Name name) {
    try {
        return LoadClass(base, name, true).getConstructor().newInstance();
    } catch (Exception e) {
        throw new RuntimeException(name.deobfuscated, e);
    }
}
```

- `LoadClass(base, name, true)` → 클래스 로드 (실패 시 예외)
- `.getConstructor().newInstance()` → 기본 생성자(인자 없음)로 인스턴스 생성
- 실패 시: `RuntimeException(name.deobfuscated, e)`

---

### `CheckClasses(Class<?> base, Name... names)` (public static)

```java
public static boolean CheckClasses(Class<?> base, Name... names) {
    for(int i = 0; i < names.length; i++)
        if(LoadClass(base, names[i], false) == null)
            return false;
    return true;
}
```

모든 `names`를 `LoadClass(..., false)`로 시도. 하나라도 로드 실패(`null` 반환)이면 `false`.

---

### `LoadClass(Class<?> base, Name name, boolean throwException)` (public static)

```java
public static Class<?> LoadClass(Class<?> base, Name name, boolean throwException) {
    ClassLoader loader = base.getClassLoader();

    if(name.obfuscated != null)
        try {
            return loader.loadClass(name.obfuscated);
        } catch (ClassNotFoundException cnfe) {}

    try {
        return loader.loadClass(name.deobfuscated);
    } catch (ClassNotFoundException cnfe) {
        if(throwException)
            throw new RuntimeException(cnfe);
        return null;
    }
}
```

**시도 순서**:
1. `name.obfuscated != null` → 난독화 이름으로 로드 시도
2. `name.deobfuscated`로 로드 시도

`forgefuscated`는 클래스 로드에서는 시도하지 않음 (필드/메서드 조회에서만 사용).

`throwException == true`이고 실패 시: `RuntimeException(cnfe)`.  
`throwException == false`이고 실패 시: `null` 반환.

---

### `SetField(Field, Object, Object)` (public static)

```java
public static void SetField(Field field, Object object, Object value) {
    try {
        field.set(object, value);
    } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
    }
}
```

이미 `setAccessible(true)` 처리된 `Field` 객체를 받아 값 설정. 실패 시 `RuntimeException`.

---

### `GetField(Field, Object)` (public static)

```java
public static Object GetField(Field field, Object object) {
    try {
        return field.get(object);
    } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
    }
}
```

이미 `setAccessible(true)` 처리된 `Field` 객체로 값 조회.

---

### `SetField(Class<?>, Object, Name, Object)` (public static)

```java
public static void SetField(Class<?> theClass, Object object, Name name, Object value) {
    try {
        GetField(theClass, name).set(object, value);
    } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
    }
}
```

`GetField(theClass, name)` (throwException=true)으로 `Field` 취득 후 바로 값 설정.

---

### `GetField(Class<?>, Object, Name)` (public static)

```java
public static Object GetField(Class<?> theClass, Object object, Name name) {
    try {
        return GetField(theClass, name).get(object);
    } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
    }
}
```

`GetField(theClass, name)`으로 `Field` 취득 후 값 조회.

---

### `GetField(Class<?>, Name)` (public static) — throwException=true 고정

```java
public static Field GetField(Class<?> theClass, Name name) {
    return GetField(theClass, name, true);
}
```

---

### `GetField(Class<?>, Name, boolean)` (public static)

```java
public static Field GetField(Class<?> theClass, Name name, boolean throwException) {
    if(theClass == null && !throwException)
        return null;

    Field field = null;
    try {
        field = GetRawField(theClass, name);
        field.setAccessible(true);
    } catch (NoSuchFieldException oe) {
        if(throwException)
            throw new RuntimeException(GetFieldMessage(theClass, name), oe);
    }
    return field;
}
```

`theClass == null && !throwException` → `null` 반환 (조용한 실패).  
`GetRawField()`로 `Field` 취득 후 `setAccessible(true)`.  
실패 + `throwException` → `RuntimeException(GetFieldMessage(...), oe)` — 클래스 내 전체 필드 목록을 메시지에 포함.

---

### `GetRawField(Class<?>, Name)` (private static)

```java
private static Field GetRawField(Class<?> theClass, Name name) throws NoSuchFieldException {
    if(name.obfuscated != null)
        try {
            return theClass.getDeclaredField(name.obfuscated);
        } catch (NoSuchFieldException oe) {}

    if(name.forgefuscated != null)
        try {
            return theClass.getDeclaredField(name.forgefuscated);
        } catch (NoSuchFieldException oe) {}

    return theClass.getDeclaredField(name.deobfuscated);
}
```

**시도 순서**: `obfuscated` → `forgefuscated` → `deobfuscated`.  
마지막 `deobfuscated` 시도에서 실패하면 `NoSuchFieldException` 그대로 전파 (catch 없음).

---

### `GetMethod(Class<?>, Name, Class<?>...)` (public static) — throwException=true 고정

```java
public static Method GetMethod(Class<?> theClass, Name name, Class<?>... paramArrayOfClass) {
    return GetMethod(theClass, name, true, paramArrayOfClass);
}
```

---

### `GetMethod(Class<?>, Name, boolean, Class<?>...)` (public static)

```java
public static Method GetMethod(Class<?> theClass, Name name, boolean throwException, Class<?>... paramArrayOfClass) {
    if(theClass == null && !throwException)
        return null;

    Method method = null;
    try {
        method = GetRawMethod(theClass, name, paramArrayOfClass);
        method.setAccessible(true);
    } catch (NoSuchMethodException oe) {
        if(throwException)
            throw new RuntimeException(GetMethodMessage(theClass, name), oe);
    }
    return method;
}
```

`GetField()`와 구조 동일. `GetRawMethod()`로 `Method` 취득 후 `setAccessible(true)`.  
실패 + `throwException` → `RuntimeException(GetMethodMessage(...), oe)` — 클래스 내 전체 메서드 목록 포함.

---

### `GetRawMethod(Class<?>, Name, Class<?>...)` (private static)

```java
private static Method GetRawMethod(Class<?> theClass, Name name, Class<?>... paramArrayOfClass) throws NoSuchMethodException {
    if(name.obfuscated != null)
        try {
            return theClass.getDeclaredMethod(name.obfuscated, paramArrayOfClass);
        } catch (NoSuchMethodException oe) {}

    if(name.forgefuscated != null)
        try {
            return theClass.getDeclaredMethod(name.forgefuscated, paramArrayOfClass);
        } catch (NoSuchMethodException oe) {}

    return theClass.getDeclaredMethod(name.deobfuscated, paramArrayOfClass);
}
```

**시도 순서**: `obfuscated` → `forgefuscated` → `deobfuscated`.  
`GetRawField()`와 동일 패턴. 마지막 시도 실패 시 `NoSuchMethodException` 전파.

---

### `Invoke(Method, Object, Object...)` (public static)

```java
public static Object Invoke(Method method, Object paramObject, Object... paramArrayOfObject) {
    try {
        return method.invoke(paramObject, paramArrayOfObject);
    } catch (Exception e) {
        throw new RuntimeException(method.getName(), e);
    }
}
```

이미 `setAccessible(true)` 처리된 `Method` 호출. 실패 시 `RuntimeException(method.getName(), e)`.

---

## 오류 메시지 헬퍼 (private static)

### `GetMessage(Class<?>, Name, String)` — 공통 메시지 생성

```java
private static StringBuffer GetMessage(Class<?> theClass, Name name, String elementName) {
    StringBuffer message = new StringBuffer()
        .append("Can not find ")
        .append(elementName)
        .append(" \"")
        .append(name.deobfuscated)
        .append("\"");

    if(name.obfuscated != null)
        message
        .append(" (ofuscated \"")      // "ofuscated" — 원본 오타
        .append(name.obfuscated)
        .append("\")");

    message
        .append(" in class \"")
        .append(theClass.getName())
        .append("\".\nExisting ")
        .append(elementName)
        .append("s are:");

    return message;
}
```

출력 형식 예시:
```
Can not find field "myField" (ofuscated "a") in class "net.minecraft.entity.Entity".
Existing fields are:
```

`"ofuscated"`: 원본 오타 (`obfuscated`의 오타).

### `GetFieldMessage(Class<?>, Name)` (private static)

```java
private static String GetFieldMessage(Class<?> theClass, Name name) {
    Field[] fields = theClass.getDeclaredFields();
    StringBuffer message = GetMessage(theClass, name, "field");
    for(int i = 0; i < fields.length; i++)
        AppendField(message, fields[i]);
    return message.toString();
}
```

`GetMessage()` + 클래스의 모든 선언 필드 목록 추가.

### `GetMethodMessage(Class<?>, Name)` (private static)

```java
private static String GetMethodMessage(Class<?> theClass, Name name) {
    Method[] methods = theClass.getDeclaredMethods();
    StringBuffer message = GetMessage(theClass, name, "method");
    for(int i = 0; i < methods.length; i++)
        AppendMethod(message, methods[i]);
    return message.toString();
}
```

`GetMessage()` + 클래스의 모든 선언 메서드 목록 추가.

### `AppendField(StringBuffer, Field)` (private static)

```java
private static void AppendField(StringBuffer message, Field field) {
    message
        .append("\n\t\t")
        .append(field.getType().getName())
        .append(" ")
        .append(field.getName());
}
```

출력 형식: `\n\t\tTypeName fieldName`

### `AppendMethod(StringBuffer, Method)` (private static)

```java
private static void AppendMethod(StringBuffer message, Method method) {
    message
        .append("\n\t\t")
        .append(method.getReturnType().getName())
        .append(" ")
        .append(method.getName())
        .append("(");

    Class<?>[] types = method.getParameterTypes();
    for(int i = 0; i < types.length; i++) {
        if(i != 0) message.append(", ");
        message.append(types[i].getName());
    }
    message.append(")");
}
```

출력 형식: `\n\t\tReturnType methodName(ParamType1, ParamType2)`

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `java.lang.reflect.*` | `Field`, `Method`, `Constructor` |
| `net.smart.utilities.Name` | 3가지 이름(obfuscated/forgefuscated/deobfuscated) 묶음 |

---

## 이름 시도 순서 요약

| 동작 | obfuscated | forgefuscated | deobfuscated |
|------|-----------|--------------|-------------|
| `LoadClass()` | 1순위 | 시도 안 함 | 2순위 |
| `GetRawField()` | 1순위 | 2순위 | 3순위 |
| `GetRawMethod()` | 1순위 | 2순위 | 3순위 |

---

## 주요 관찰 사항

1. **`getDeclaredField/Method`**: 상속된 멤버는 탐색하지 않음. 선언된 클래스에서만 탐색.

2. **`setAccessible(true)` 위치**: `GetField()` / `GetMethod()` 내부에서 취득 직후 실행. 호출자가 따로 처리할 필요 없음.

3. **`LoadClass`에서 `forgefuscated` 미사용**: 클래스 로드는 obfuscated → deobfuscated만 시도. SRG 이름으로 클래스 자체를 로드하는 경우는 없음.

4. **`"ofuscated"` 오타**: `GetMessage()` 내 `"(ofuscated \""` — 원본 코드 그대로의 오타.

5. **`Invoke()`의 Exception catch**: `InvocationTargetException` / `IllegalAccessException` 등 리플렉션 예외를 모두 `Exception`으로 포괄. 원인 예외는 `RuntimeException`으로 래핑.

6. **1.21.1 이식 관련**:
   - Yarn 매핑 환경에서는 단일 이름만 필요 → `Name.obfuscated`, `Name.forgefuscated` 미사용
   - Mixin을 사용하면 `Reflect` 클래스 전체가 불필요 (Mixin accessor가 직접 접근 제공)
   - 리플렉션이 필요한 경우 Java 표준 `Field`/`Method` API 그대로 사용 가능
   - `getDeclaredField(name.deobfuscated)` 경로만 남기고 나머지 제거하면 됨
