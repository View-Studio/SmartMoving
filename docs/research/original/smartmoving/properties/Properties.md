# Properties.java (net.smart.properties) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/properties/Properties.java  
패키지: `net.smart.properties`  
종류: `class`  
상속: `java.util.Properties` (extends)  
실행 위치: 클라이언트 / 서버

---

## 전체 소스

소스는 GitHub 링크 참조.

---

## 역할

SmartMoving 설정 시스템의 기반 클래스. JDK `java.util.Properties`를 확장하여 타입이 있는 `Property<T>` 필드 수집, 파일 로드/저장, 타입 상수 정의, 팩토리 메서드를 제공한다.

`SmartMovingProperties → SmartMovingClientConfig → SmartMovingConfig → SmartMovingOptions`/`SmartMovingServerConfig` 계층의 최하위 기반.

---

## import

```java
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import net.smart.utilities.*;  // Reflect
```

---

## 타입 상수 (static final int, i++ 패턴)

```java
private static int i = 0;
public static final int Boolean        = i++;  // 0
public static final int Unmodified     = i++;  // 1
public static final int Modified       = i++;  // 2
public static final int Float          = i++;  // 3
public static final int Positive       = i++;  // 4
public static final int Negative       = i++;  // 5
public static final int PositiveFactor = i++;  // 6
public static final int NegativeFactor = i++;  // 7
public static final int IncreasingFactor = i++; // 8
public static final int DecreasingFactor = i++; // 9
public static final int Integer        = i++;  // 10
public static final int String         = i++;  // 11
public static final int Strings        = i++;  // 12
public static final int Operator       = i++;  // 13
public static final int Constant       = i++;  // 14
public static final int Key            = i++;  // 15
public static final int StringMap      = i++;  // 16
public static final int IntegerMap     = i++;  // 17
```

총 18개. `Operator(13)`, `Constant(14)`, `Key(15)`는 팩토리 메서드가 이 파일에 없음.

---

## 필드

```java
private static final long serialVersionUID = 5319578641402091067L;

public final String version;
```

`version`: 설정 파일의 버전 문자열. 파일 로드 시 `"move.options.version"` 프로퍼티에서 읽음.

---

## 생성자

```java
public Properties()
{
    version = null;
}

public Properties(File file)
{
    load(file);
    version = getProperty("move.options.version");
}

public Properties(String version, File file)
{
    load(file);
    this.version = version;
}
```

- 기본 생성자: `version = null`
- `(File)`: 파일 로드 후 `"move.options.version"` 프로퍼티를 version으로 사용
- `(String, File)`: 파일 로드 후 파라미터 version을 그대로 사용 (파일 내 version 프로퍼티 무시)

---

## Property 수집 메서드

### `getProperties()` / `getProperties(Class<?>)`

```java
protected List<Property<?>> getProperties()
{
    return getProperties(null);
}

protected List<Property<?>> getProperties(Class<?> type)
{
    List<Property<?>> properties = new ArrayList<Property<?>>();
    if(type != null)
        return addProperties(properties, type, false);
    return addProperties(properties, getClass(), true);
}
```

`type == null`이면 `getClass()`(자신)에서 시작하고 `base=true` (슈퍼클래스 재귀 포함).  
`type != null`이면 지정 클래스만 (`base=false`, 슈퍼클래스 미포함).

### `addProperties(List, Class<?>, boolean base)` (private)

```java
private List<Property<?>> addProperties(List<Property<?>> list, Class<?> type, boolean base)
{
    if(base && type.getSuperclass() != null)
        addProperties(list, type.getSuperclass(), base);

    Field[] fields = type.getDeclaredFields();
    for(int i = 0; i < fields.length; i++)
    {
        fields[i].setAccessible(true);
        Object value = Reflect.GetField(fields[i], this);
        addProperties(list, value);
    }
    return list;
}
```

**탐색 순서**: `base=true`이면 슈퍼클래스를 **먼저** 재귀 처리한 뒤 자신의 필드를 추가 → 결과 리스트에서 부모 클래스 Property가 앞에 위치.

`fields[i].setAccessible(true)`: private 필드도 접근.  
`Reflect.GetField(Field, Object)`: 필드 값 읽기 (예외 없이 null 반환).

### `addProperties(List, Object)` (private)

```java
private void addProperties(List<Property<?>> list, Object value)
{
    if(value instanceof Property)
        list.add((Property<?>)value);
    else if(value instanceof Collection)
    {
        Iterator<?> iterator = ((Collection<?>)value).iterator();
        while(iterator.hasNext())
            addProperties(list, iterator.next());
    }
}
```

`Property` 인스턴스이면 리스트에 추가.  
`Collection` 이면 각 원소를 재귀 처리 (Collection 안에 Property가 있는 구조 지원).  
그 외는 무시.

---

## `write()` 메서드

```java
public void write(Properties properties)
{
    write(properties, null);
}

public void write(Properties properties, String key)
{
    List<Property<?>> propertiesToWrite = getProperties();
    for(int i = 0; i < propertiesToWrite.size(); i++)
    {
        Property<?> property = propertiesToWrite.get(i);
        if(property.isPersistent())
            properties.put(property.getCurrentKey(), key == null ? property.getValueString() : property.getKeyValueString(key));
    }
}
```

- `isPersistent()` 인 Property만 저장
- `key == null`: `property.getValueString()` 사용
- `key != null`: `property.getKeyValueString(key)` 사용 (키별 값 형식)
- `properties.put(getCurrentKey(), ...)`: 대상 `Properties` 객체에 직접 put

---

## `load(File)` (private)

```java
private boolean load(File file)
{
    try
    {
        if(file.exists())
            load(new FileInputStream(file));
        return true;
    }
    catch (Exception e)
    {
    }
    return false;
}
```

파일이 존재하면 `java.util.Properties.load(InputStream)` 호출.  
예외는 완전히 무시(빈 catch). 성공 시 `true`, 실패 시 `false`.  
파일 미존재도 `true` 반환 (예외 없이 정상 종료).

---

## 타입 분류 메서드

### `getBaseType(int type)`

```java
public static int getBaseType(int type)
{
    if(type == Boolean || type == Unmodified || type == Modified)
        return Properties.Boolean;
    if(type == Float || type == Positive || type == Negative
        || type == PositiveFactor || type == NegativeFactor
        || type == IncreasingFactor || type == DecreasingFactor)
        return Properties.Float;
    if(type == Integer)
        return Properties.Integer;
    if(type == Strings)
        return Properties.Strings;
    if(type == StringMap)
        return Properties.StringMap;
    if(type == IntegerMap)
        return Properties.IntegerMap;
    return Properties.String;  // String, Operator, Constant, Key, 그 외
}
```

| 입력 타입 | 기반 타입 |
|-----------|-----------|
| Boolean, Unmodified, Modified | Boolean |
| Float, Positive, Negative, PositiveFactor, NegativeFactor, IncreasingFactor, DecreasingFactor | Float |
| Integer | Integer |
| Strings | Strings |
| StringMap | StringMap |
| IntegerMap | IntegerMap |
| String, Operator, Constant, Key, 기타 | String |

### `getBaseTypeName(int baseType)`

```java
public static String getBaseTypeName(int baseType)
{
    if(baseType == Properties.Boolean)  return "boolean";
    if(baseType == Properties.Float)    return "floating point";
    if(baseType == Properties.Integer)  return "integer";
    return "string";
}
```

Boolean→`"boolean"`, Float→`"floating point"`, Integer→`"integer"`, 그 외→`"string"`.

---

## 기본값 / 최솟값 / 최댓값

### `getDefaultValue(int type)`

| 타입 | 기본값 |
|------|--------|
| Boolean | `false` |
| Unmodified | `true` |
| Modified | `false` |
| Integer | `0` |
| Float | `0F` |
| Positive | `0F` |
| Negative | `0F` |
| PositiveFactor | `1F` |
| NegativeFactor | `1F` |
| IncreasingFactor | `1F` |
| DecreasingFactor | `1F` |
| String | `""` |
| Strings | `new String[0]` |
| StringMap | `new HashMap<String, String>()` |
| IntegerMap | `new HashMap<String, Integer>()` |
| Operator, Constant, Key, 기타 | `null` |

### `getMinimumValue(int type)`

| 타입 | 최솟값 |
|------|--------|
| Positive | `0F` |
| PositiveFactor | `0F` |
| IncreasingFactor | `1F` |
| DecreasingFactor | `0F` |
| 그 외 | `null` |

### `getMaximumValue(int type)`

| 타입 | 최댓값 |
|------|--------|
| Negative | `0F` |
| NegativeFactor | `0F` |
| DecreasingFactor | `1F` |
| 그 외 | `null` |

**Factor 타입의 범위 정리**:
- `PositiveFactor`: [0F, ∞) — 최솟값만
- `NegativeFactor`: (-∞, 0F] — 최댓값만
- `IncreasingFactor`: [1F, ∞) — 최솟값만
- `DecreasingFactor`: [0F, 1F] — 양쪽 모두

---

## 팩토리 메서드 (static)

모두 `new Property(type)` 또는 `new Property(type).key(key, versions)` 반환.

### Property 팩토리 (타입 없는 버전 + 키 있는 버전 쌍)

| 메서드 | 반환 타입 | 내부 호출 |
|--------|-----------|-----------|
| `Unmodified()` | `Property<Boolean>` | `Property(Unmodified)` |
| `Unmodified(String key, String... versions)` | `Property<Boolean>` | `.key(key, versions)` |
| `Modified()` | `Property<Boolean>` | `Property(Modified)` |
| `Modified(String key, String... versions)` | `Property<Boolean>` | `.key(key, versions)` |
| `Integer()` | `Property<Integer>` | `Property(Integer)` |
| `Integer(String key, String... versions)` | `Property<Integer>` | `.key(key, versions)` |
| `Float()` | `Property<Float>` | `Property(Float)` |
| `Float(String key, String... versions)` | `Property<Float>` | `.key(key, versions)` |
| `Positive()` | `Property<Float>` | `Property(Positive)` |
| `Positive(String key, String... versions)` | `Property<Float>` | `.key(key, versions)` |
| `Negative()` | `Property<Float>` | `Property(Negative)` |
| `Negative(String key, String... versions)` | `Property<Float>` | `.key(key, versions)` |
| `PositiveFactor()` | `Property<Float>` | `Property(PositiveFactor)` |
| `PositiveFactor(String key, String... versions)` | `Property<Float>` | `.key(key, versions)` |
| `NegativeFactor()` | `Property<Float>` | `Property(NegativeFactor)` |
| `NegativeFactor(String key, String... versions)` | `Property<Float>` | `.key(key, versions)` |
| `IncreasingFactor()` | `Property<Float>` | `Property(IncreasingFactor)` |
| `IncreasingFactor(String key, String... versions)` | `Property<Float>` | `.key(key, versions)` |
| `DecreasingFactor()` | `Property<Float>` | `Property(DecreasingFactor)` |
| `DecreasingFactor(String key, String... versions)` | `Property<Float>` | `.key(key, versions)` |
| `String()` | `Property<String>` | `Property(String)` |
| `String(String key, String... versions)` | `Property<String>` | `.key(key, versions)` |
| `Strings()` | `Property<String[]>` | `Property(Strings)` |
| `Strings(String key, String... versions)` | `Property<String[]>` | `.key(key, versions)` |
| `StringMap()` | `Property<Map<String,String>>` | `Property(StringMap)` |
| `StringMap(String key, String... versions)` | `Property<Map<String,String>>` | `.key(key, versions)` |
| `IntegerMap()` | `Property<Map<String,Integer>>` | `Property(IntegerMap)` |
| `IntegerMap(String key, String... versions)` | `Property<Map<String,Integer>>` | `.key(key, versions)` |

`Boolean()` 팩토리 메서드 없음 — `Unmodified()`와 `Modified()`가 그 역할을 대신.  
`Operator()`, `Constant()`, `Key()` 팩토리 메서드도 없음.

### `private static Property Property(int type)`

```java
private static Property Property(int type)
{
    return new Property(type);
}
```

raw 타입 `Property` 반환. 각 팩토리 메서드에서 제네릭 타입 캐스팅.

### Value 팩토리

```java
public static Value<Boolean> Value(Boolean base) { return new Value<Boolean>(Boolean).put(base); }
public static Value<Float>   Value(Float base)   { return new Value<Float>(Float).put(base); }
public static Value<String>  Value(String base)  { return new Value<String>(String).put(base); }
```

각각 해당 타입의 `Value<T>` 생성 후 `put(base)`로 기본값 설정.

---

## `concat()` 유틸리티 (protected static)

```java
protected static String[] concat(String left, String[] right)
{
    return concat(new String[] { left }, right);
}

protected static String[] concat(String[] left, String[] right)
{
    String[] result = new String[left.length + right.length];
    int i = 0;
    for(; i < left.length; i++)
        result[i] = left[i];
    for(; i < result.length; i++)
        result[i] = right[i - left.length];
    return result;
}
```

두 배열을 연결하여 새 배열 반환. `SmartMovingConfig`에서 버전 배열 누적(`_pre_sm_X_Y = concat(version, prev)`)에 사용.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `java.util.Properties` | 상속 — 파일 로드/저장, put/get |
| `Property<T>` | 수집 대상 타입 |
| `Value<T>` | 팩토리 메서드 반환 타입 |
| `Reflect.GetField(Field, Object)` | 리플렉션 필드 값 읽기 |

---

## 주요 관찰 사항

1. **`addProperties` 탐색 순서**: 슈퍼클래스 필드가 리스트 앞에, 서브클래스 필드가 뒤에. `Properties → SmartMovingProperties → SmartMovingConfig → SmartMovingClientConfig → SmartMovingOptions` 계층에서 기반 Property가 앞에 오는 구조.

2. **`Collection` 내 Property 지원**: `addProperties(list, value)`에서 `Collection`을 재귀 탐색. Property를 리스트/컬렉션으로 묶어 보관하는 경우를 지원.

3. **`load(File)` 예외 무시**: 파일 로드 실패(IOException, 파일 손상 등) 시 완전 무시. 설정 없이 기본값으로 동작.

4. **타입 이름 충돌**: `Boolean`, `Integer`, `Float`, `String` 등이 `java.lang.*` 타입과 같은 이름. 이 클래스 내부에서 `Properties.Boolean`, `Properties.Integer` 등으로 정규화 필요.

5. **`Operator(13)`, `Constant(14)`, `Key(15)` 팩토리 없음**: 이 타입들의 Property는 다른 방식으로 생성되거나, 이 파일에서 직접 사용되지 않음.

6. **1.21.1 이식 관련**:
   - `java.util.Properties` 상속 구조는 유지 가능
   - 설정 파일 로드 방식은 Fabric Config API(FiberConfig 등) 또는 직접 유지 가능
   - 리플렉션 기반 Property 수집 → Fabric 환경에서도 동작하나 GraalVM AOT 등에서 문제 가능
   - 타입 상수 및 팩토리 메서드 구조는 그대로 이식 가능
