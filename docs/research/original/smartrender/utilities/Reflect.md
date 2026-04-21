# Reflect.java (net.smart.utilities) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/utilities/Reflect.java  
패키지: `net.smart.utilities`  
종류: `class`  
상속 없음  
라이선스 헤더: "Smart Render and Smart Moving" — **두 모드가 공유하는 파일**

---

## 역할

`Name` 인스턴스(obfuscated/forgefuscated/deobfuscated 세 이름)를 활용하여 실행 환경에 맞는 이름으로 reflection을 수행하는 static 유틸리티 클래스.  
모든 메서드 static, 인스턴스화 없이 사용.

---

## 이름 해석 우선순위

### 클래스 로딩 (`LoadClass`)
```
obfuscated → deobfuscated
```
forgefuscated는 클래스 로딩에 사용하지 않음 (클래스 전체 경로명은 SRG가 없음).

### 필드 조회 (`GetRawField`) / 메서드 조회 (`GetRawMethod`)
```
obfuscated → forgefuscated → deobfuscated
```
각 단계에서 실패하면(NoSuchField/Method) 다음 이름으로 시도. 마지막 deobfuscated 실패 시 예외 전파.

---

## 메서드 전체

### `NewInstance(Class<?> base, Name name)` → Object

```java
public static Object NewInstance(Class<?> base, Name name)
{
    try
    {
        return LoadClass(base, name, true).getConstructor().newInstance();
    }
    catch (Exception e)
    {
        throw new RuntimeException(name.deobfuscated, e);
    }
}
```

- `LoadClass`로 클래스를 찾은 뒤 no-arg 생성자로 인스턴스 생성
- 실패 시: `RuntimeException(name.deobfuscated, cause)`

---

### `CheckClasses(Class<?> base, Name... names)` → boolean

```java
public static boolean CheckClasses(Class<?> base, Name... names)
{
    for(int i=0; i<names.length; i++)
        if(LoadClass(base, names[i], false) == null)
            return false;
    return true;
}
```

- 모든 `Name`에 대해 클래스가 존재하는지 확인
- 하나라도 없으면 `false`, 전부 있으면 `true`
- `throwException = false`이므로 못 찾아도 예외 없이 `null` 반환

---

### `LoadClass(Class<?> base, Name name, boolean throwException)` → Class<?>

```java
public static Class<?> LoadClass(Class<?> base, Name name, boolean throwException)
{
    ClassLoader loader = base.getClassLoader();

    if(name.obfuscated != null)
        try
        {
            return loader.loadClass(name.obfuscated);
        }
        catch (ClassNotFoundException cnfe)
        {
        }

    try
    {
        return loader.loadClass(name.deobfuscated);
    }
    catch (ClassNotFoundException cnfe)
    {
        if(throwException)
            throw new RuntimeException(cnfe);
        return null;
    }
}
```

- `base.getClassLoader()` 사용 — `base` 클래스와 동일한 ClassLoader로 로드
- 우선순위: `obfuscated` → `deobfuscated` (forgefuscated 미사용)
- `throwException = true`: 최종 실패 시 `RuntimeException(ClassNotFoundException)`
- `throwException = false`: 최종 실패 시 `null` 반환

---

### `SetField(Field field, Object object, Object value)` — void

```java
public static void SetField(Field field, Object object, Object value)
{
    try
    {
        field.set(object, value);
    }
    catch (IllegalAccessException e)
    {
        throw new RuntimeException(e);
    }
}
```

- 이미 조회된 `Field` 객체를 사용하는 저수준 setter

---

### `GetField(Field field, Object object)` → Object

```java
public static Object GetField(Field field, Object object)
{
    try
    {
        return field.get(object);
    }
    catch (IllegalAccessException e)
    {
        throw new RuntimeException(e);
    }
}
```

---

### `SetField(Class<?> theClass, Object object, Name name, Object value)` — void

```java
public static void SetField(Class<?> theClass, Object object, Name name, Object value)
{
    try
    {
        GetField(theClass, name).set(object, value);
    }
    catch (IllegalAccessException e)
    {
        throw new RuntimeException(e);
    }
}
```

- 클래스 + Name으로 Field를 조회한 뒤 즉시 set

---

### `GetField(Class<?> theClass, Object object, Name name)` → Object

```java
public static Object GetField(Class<?> theClass, Object object, Name name)
{
    try
    {
        return GetField(theClass, name).get(object);
    }
    catch (IllegalAccessException e)
    {
        throw new RuntimeException(e);
    }
}
```

---

### `GetField(Class<?> theClass, Name name)` → Field

```java
public static Field GetField(Class<?> theClass, Name name)
{
    return GetField(theClass, name, true);
}
```

---

### `GetField(Class<?> theClass, Name name, boolean throwException)` → Field

```java
public static Field GetField(Class<?> theClass, Name name, boolean throwException)
{
    if(theClass == null && !throwException)
        return null;

    Field field = null;
    try
    {
        field = GetRawField(theClass, name);
        field.setAccessible(true);
    }
    catch (NoSuchFieldException oe)
    {
        if(throwException)
            throw new RuntimeException(GetFieldMessage(theClass, name), oe);
    }
    return field;
}
```

- `theClass == null && !throwException`: 조기 null 반환 (NPE 방지)
- 찾으면 `field.setAccessible(true)` 후 반환
- NoSuchFieldException + `throwException = true`: `GetFieldMessage()`로 기존 필드 목록 포함한 상세 메시지와 함께 RuntimeException

---

### `GetRawField(Class<?> theClass, Name name)` → Field (private)

```java
private static Field GetRawField(Class<?> theClass, Name name) throws NoSuchFieldException
{
    if(name.obfuscated != null)
        try
        {
            return theClass.getDeclaredField(name.obfuscated);
        }
        catch (NoSuchFieldException oe)
        {
        }

    if(name.forgefuscated != null)
        try
        {
            return theClass.getDeclaredField(name.forgefuscated);
        }
        catch (NoSuchFieldException oe)
        {
        }

    return theClass.getDeclaredField(name.deobfuscated);
}
```

- 우선순위: `obfuscated` → `forgefuscated` → `deobfuscated`
- 각 단계: NoSuchFieldException 삼킨 뒤 다음 이름 시도
- deobfuscated 실패 시: NoSuchFieldException 상위로 전파 (삼키지 않음)

---

### `GetMethod(Class<?> theClass, Name name, Class<?>... paramArrayOfClass)` → Method

```java
public static Method GetMethod(Class<?> theClass, Name name, Class<?>... paramArrayOfClass)
{
    return GetMethod(theClass, name, true, paramArrayOfClass);
}
```

---

### `GetMethod(Class<?> theClass, Name name, boolean throwException, Class<?>... paramArrayOfClass)` → Method

```java
public static Method GetMethod(Class<?> theClass, Name name, boolean throwException, Class<?>... paramArrayOfClass)
{
    if(theClass == null && !throwException)
        return null;

    Method method = null;
    try
    {
        method = GetRawMethod(theClass, name, paramArrayOfClass);
        method.setAccessible(true);
    }
    catch (NoSuchMethodException oe)
    {
        if(throwException)
            throw new RuntimeException(GetMethodMessage(theClass, name), oe);
    }
    return method;
}
```

- `GetField` 패밀리와 동일한 구조

---

### `GetRawMethod(Class<?> theClass, Name name, Class<?>... paramArrayOfClass)` → Method (private)

```java
private static Method GetRawMethod(Class<?> theClass, Name name, Class<?>... paramArrayOfClass)
    throws NoSuchMethodException
{
    if(name.obfuscated != null)
        try
        {
            return theClass.getDeclaredMethod(name.obfuscated, paramArrayOfClass);
        }
        catch (NoSuchMethodException oe)
        {
        }

    if(name.forgefuscated != null)
        try
        {
            return theClass.getDeclaredMethod(name.forgefuscated, paramArrayOfClass);
        }
        catch (NoSuchMethodException oe)
        {
        }

    return theClass.getDeclaredMethod(name.deobfuscated, paramArrayOfClass);
}
```

- `GetRawField`과 동일한 3단계 우선순위 패턴

---

### `Invoke(Method method, Object paramObject, Object... paramArrayOfObject)` → Object

```java
public static Object Invoke(Method method, Object paramObject, Object... paramArrayOfObject)
{
    try
    {
        return method.invoke(paramObject, paramArrayOfObject);
    }
    catch (Exception e)
    {
        throw new RuntimeException(method.getName(), e);
    }
}
```

- 조회된 `Method`를 실행
- 실패 시: `RuntimeException(method.getName(), cause)` — 모든 Exception 유형 포함

---

### 에러 메시지 헬퍼 3개 (private)

#### `GetMessage(Class<?> theClass, Name name, String elementName)` → StringBuffer

```java
private static StringBuffer GetMessage(Class<?> theClass, Name name, String elementName)
{
    StringBuffer message = new StringBuffer()
        .append("Can not find ")
        .append(elementName)
        .append(" \"")
        .append(name.deobfuscated)
        .append("\"");

    if(name.obfuscated != null)
        message
        .append(" (ofuscated \"")
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

출력 형식: `Can not find field "deobfName" (ofuscated "obfName") in class "className".\nExisting fields are:`  
※ "ofuscated" 오타 — 원본 그대로.

#### `AppendMethod(StringBuffer message, Method method)` — void (private)

```java
private static void AppendMethod(StringBuffer message, Method method)
{
    message
        .append("\n\t\t")
        .append(method.getReturnType().getName())
        .append(" ")
        .append(method.getName())
        .append("(");

    Class<?>[] types = method.getParameterTypes();
    for(int i=0; i<types.length; i++)
    {
        if(i != 0)
            message.append(", ");
        message.append(types[i].getName());
    }

    message.append(")");
}
```

출력 형식: `\n\t\treturnType name(param1, param2)`

#### `AppendField(StringBuffer message, Field field)` — void (private)

```java
private static void AppendField(StringBuffer message, Field field)
{
    message
        .append("\n\t\t")
        .append(field.getType().getName())
        .append(" ")
        .append(field.getName());
}
```

출력 형식: `\n\t\ttype name`

---

## 공개 API 요약

| 메서드 | 역할 |
|--------|------|
| `LoadClass(base, name, throw)` | 클래스 로드 (obf→deobf) |
| `CheckClasses(base, names...)` | 클래스 존재 여부 일괄 확인 |
| `NewInstance(base, name)` | 클래스 로드 + no-arg 생성자 실행 |
| `GetField(class, name)` | Field 조회 + setAccessible(true) (obf→srg→deobf) |
| `GetField(class, name, throw)` | 위와 동일, throwException 제어 |
| `GetField(class, object, name)` | Field 조회 후 즉시 get |
| `SetField(class, object, name, value)` | Field 조회 후 즉시 set |
| `GetField(field, object)` | Field 직접 get |
| `SetField(field, object, value)` | Field 직접 set |
| `GetMethod(class, name, params...)` | Method 조회 + setAccessible(true) (obf→srg→deobf) |
| `GetMethod(class, name, throw, params...)` | 위와 동일, throwException 제어 |
| `Invoke(method, object, args...)` | Method 실행 |

---

## 이름 해석 우선순위 정리

| 연산 | 시도 순서 | forgefuscated 사용 |
|------|-----------|-------------------|
| `LoadClass` | obfuscated → deobfuscated | ❌ |
| `GetRawField` | obfuscated → forgefuscated → deobfuscated | ✅ |
| `GetRawMethod` | obfuscated → forgefuscated → deobfuscated | ✅ |

---

## import

```java
import java.lang.reflect.*;   // Field, Method, Class
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `Name` (net.smart.utilities) | 모든 메서드의 이름 파라미터 타입 |

---

## 주요 관찰 사항

1. **환경 자동 감지**: `LoadClass`/`GetRawField`/`GetRawMethod` 모두 obfuscated를 먼저 시도하고 실패하면 다음 이름으로 폴백. 런타임에 별도 환경 감지 플래그 없이 이름 시도 순서만으로 처리.

2. **`getDeclaredField/Method` 사용**: `getField/getMethod`(상속 포함)가 아닌 `getDeclaredField/Method`(해당 클래스 선언만) 사용. 상속된 필드/메서드는 찾지 못함. `setAccessible(true)`로 private 접근.

3. **"ofuscated" 오타**: `GetMessage()` 내 `"ofuscated"` — 원본 코드의 오타. 에러 메시지에만 등장.

4. **두 모드 공유**: 라이선스 헤더에 "Smart Render and Smart Moving". SmartMoving의 `net.smart.utilities.Reflect`도 동일 파일.

5. **1.21.1 이식**: 1.21.1 Fabric은 obfuscation 없이 intermediary 이름 사용. `Name`의 obfuscated/forgefuscated 불필요. 그러나 `Reflect` 자체의 reflection 패턴(Mixin 이전에 vanilla 내부 필드 접근)은 1.21.1 Mixin의 `@Shadow`/`@Accessor`로 대체하는 것이 표준 방식.
