# Value.java (net.smart.properties) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/properties/Value.java  
패키지: `net.smart.properties`  
종류: `class` (제네릭)  
전체 라인: 682줄

---

## 역할

`Property<T>`가 보유하는 실제 값 컨테이너. 단일 값(`value`) + 키별 값(`keyValues: Dictionary<String, T>`) 구조로, 블록 설정처럼 키마다 다른 값을 가질 수 있다. 파싱, 연산(and/or/plus 등), 범위 적용, 출력까지 담당한다.

---

## import

```java
import java.io.*;
import java.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
```

---

## 클래스 선언 및 상수

```java
public class Value<T>

public final static String Null = "null";
private final static List<String> _allkeys = new LinkedList<String>();
```

**`Null`**: 키 없는(기본) 슬롯을 나타내는 sentinel 문자열. `null` 리터럴 대신 이 상수를 키로 사용.  
**`_allkeys`**: `GetAllKeys()` 호출마다 재사용하는 static 공유 리스트 (스레드 안전하지 않음).

---

## 필드

```java
private int type;                         // Properties.Boolean / Float / Integer 등
private T value;                          // 기본값 (키 없는 슬롯)
private Dictionary<String, T> keyValues;  // 키별 값 (Hashtable, 지연 생성)
private List<String> unparsableStrings;   // 파싱 실패 문자열 목록
```

---

## 생성자 3개

### `public Value(int type)`

```java
public Value(int type) {
    this.type = type;
}
```

타입만 설정. `load()` 후 사용.

### `public Value(T value)`

```java
public Value(T value) {
    this.value = value;
}
```

단일 값 직접 설정. `type`은 0(기본값).

### `public Value(Value<T> value)` — 복사 생성자

```java
public Value(Value<T> value) {
    type = value.type;
    this.value = value.value;
    if(value.keyValues != null) {
        keyValues = new Hashtable<String, T>();
        Enumeration<String> keys = value.keyValues.keys();
        while(keys.hasMoreElements()) {
            String key = keys.nextElement();
            keyValues.put(key, value.keyValues.get(key));
        }
    }
}
```

`type`, `value`, `keyValues` 복사. `unparsableStrings`는 복사 안 함.

---

## `clone()`

```java
@Override
public Value<T> clone() {
    return new Value<T>(this);
}
```

복사 생성자 위임.

---

## put / get

### `put(T value)` / `put(String key, T value)`

```java
public Value<T> put(T value) {
    return put(null, value);
}

public Value<T> put(String key, T value) {
    if(key == null || key == Null || key.isEmpty())
        this.value = value;
    else {
        if(keyValues == null)
            keyValues = new Hashtable<String, T>();
        keyValues.put(key, value);
    }
    return this;
}
```

`key == null || key == Null || key.isEmpty()` → `this.value`에 저장.  
그 외 → `keyValues`에 저장 (지연 생성).  
반환값: `this` (체인 가능).

### `get(String key)` (public)

```java
public T get(String key) {
    if(key != null && keyValues != null) {
        T value = keyValues.get(key);
        if(value != null)
            return value;
    }
    return value;
}
```

키별 값 없으면 기본값(`this.value`) 폴백.

### `getStored(String key)` (public)

```java
public T getStored(String key) {
    if(key == null || key == Null)
        return value;
    if(keyValues != null)
        return keyValues.get(key);
    return null;
}
```

폴백 없이 저장된 값만 반환. 키 없으면 `null`.

### `get(String key, Value<T> defaultValue)` (private)

```java
private T get(String key, Value<T> defaultValue) {
    T result = get(key);
    if(result == null) {
        result = value;
        if(result == null)
            result = defaultValue.value;
    }
    return result;
}
```

`get(key)` → null이면 `this.value` → null이면 `defaultValue.value`. 3단계 폴백.

---

## withDependency / withMinimum / withMaximum

### `withDependency(Value<T> dependency, Value<T> defaultValue)`

```java
public void withDependency(Value<T> dependency, Value<T> defaultValue) {
    Iterator<String> iterator = GetAllKeys(this, dependency);
    while(iterator.hasNext()) {
        String key = iterator.next();
        if(get(key, defaultValue).equals(true) && dependency.get(key).equals(false))
            put(key, (T)(Object)false);
    }
}
```

모든 키에 대해: `this`가 `true`이고 `dependency`가 `false`이면 → `false`로 덮어씀.  
`T`는 `Boolean` 전제 (명시적 캐스팅).

### `withMinimum(Value<T> minimum, Value<T> defaultValue)`

```java
public void withMinimum(Value<T> minimum, Value<T> defaultValue) {
    Iterator<String> iterator = GetAllKeys(this, minimum);
    while(iterator.hasNext()) {
        String key = iterator.next();
        put(key, (T)(Object)Math.max((Float)get(key, defaultValue), (Float)minimum.get(key)));
    }
}
```

`T`는 `Float` 전제. `Math.max(this, minimum)` 적용.

### `withMaximum(Value<T> maximum, Value<T> defaultValue)`

```java
public void withMaximum(Value<T> maximum, Value<T> defaultValue) {
    Iterator<String> iterator = GetAllKeys(this, maximum);
    while(iterator.hasNext()) {
        String key = iterator.next();
        put(key, (T)(Object)Math.min((Float)get(key, defaultValue), (Float)maximum.get(key)));
    }
}
```

`Math.min(this, maximum)` 적용.

---

## 연산 메서드 (Value → 새 Value 반환)

모든 연산은 `GetAllKeys(this, right)`로 키 집합을 구하고, 각 키에 대해 계산한 결과를 새 `Value`에 put.

### `is(Value<T> right)` → `Value<Boolean>`

```java
public Value<Boolean> is(Value<T> right) {
    Value<Boolean> result = new Value<Boolean>(Properties.Boolean);
    Iterator<String> iterator = GetAllKeys(this, right);
    while(iterator.hasNext()) {
        String key = iterator.next();
        T leftValue = get(key);
        T rightValue = right.get(key);
        result.put(key, leftValue == null && rightValue == null
            || leftValue != null && rightValue != null && leftValue.equals(rightValue));
    }
    return result;
}
```

동등 비교. 둘 다 null이면 true, 둘 다 non-null이고 equals이면 true.

### `and(Value<T> right)` → `Value<Boolean>`

```java
result.put(key, ((Boolean)get(key)) && ((Boolean)right.get(key)));
```

### `or(Value<T> right)` → `Value<Boolean>`

```java
result.put(key, ((Boolean)get(key)) || ((Boolean)right.get(key)));
```

### `not()` → `Value<Boolean>`

```java
// GetAllKeys(this) — 단항, right 없음
result.put(key, !((Boolean)get(key)));
```

### `plus(Value<T> right)` → `Value<Float>`

```java
result.put(key, ((Float)get(key)) + ((Float)right.get(key)));
```

### `eitherOr(Value<T> left, Value<T> right)` → `Value<T>`

```java
public Value<T> eitherOr(Value<T> left, Value<T> right) {
    Value<T> result = new Value<T>(Properties.getBaseType(left.type));
    Iterator<String> iterator = GetAllKeys(this, left, right);
    while(iterator.hasNext()) {
        String key = iterator.next();
        result.put(key, ((Boolean)get(key)) ? left.get(key) : right.get(key));
    }
    return result;
}
```

`this`가 조건(Boolean). `true`면 `left`, `false`면 `right`.  
결과 타입: `Properties.getBaseType(left.type)`.

### `maximum(Value<T> right)` → `Value<Float>`

```java
result.put(key, Math.max((Float)get(key), (Float)right.get(key)));
```

### `minimum(Value<T> right)` → `Value<Float>`

```java
result.put(key, Math.min((Float)get(key), (Float)right.get(key)));
```

### `toKeyName()` → `Value<String>`

```java
public Value<String> toKeyName() {
    Value<String> result = new Value<String>(Properties.String);
    Iterator<String> iterator = GetAllKeys(this);
    while(iterator.hasNext()) {
        String key = iterator.next();
        result.put(key, toKeyName((Integer)get(key)));
    }
    return result;
}
```

각 키의 Integer(키코드) → 키 이름 문자열. `toKeyName(Integer)` 위임.

### `toKeyCode()` → `Value<Integer>`

```java
public Value<Integer> toKeyCode() {
    Value<Integer> result = new Value<Integer>(Properties.Integer);
    Iterator<String> iterator = GetAllKeys(this);
    while(iterator.hasNext()) {
        String key = iterator.next();
        result.put(key, toKeyCode((String)get(key)));
    }
    return result;
}
```

각 키의 String(키 이름) → Integer(키코드). `toKeyCode(String)` 위임.

### `toBlockConfig()` → `Value<Dictionary<Object, Set<Integer>>>`

```java
public Value<Dictionary<Object, Set<Integer>>> toBlockConfig() {
    Value<Dictionary<Object, Set<Integer>>> result = new Value<Dictionary<Object, Set<Integer>>>(Properties.Strings);
    Iterator<String> iterator = GetAllKeys(this);
    while(iterator.hasNext()) {
        String key = iterator.next();
        result.put(key, toBlockConfig((String[])get(key)));
    }
    return result;
}
```

`String[]` → `Dictionary<Object, Set<Integer>>`. `toBlockConfig(String[])` 위임.

---

## 키 단축 메서드 (e/m/h/c/a)

```java
public Value<T> e(T e) { return this.put("e", e); }
public Value<T> m(T m) { return this.put("m", m); }
public Value<T> h(T h) { return this.put("h", h); }
public Value<T> c(T c) { return this.put("c", c); }
public Value<T> a(T a) { return this.put("a", a); }
```

각각 키 `"e"`, `"m"`, `"h"`, `"c"`, `"a"`로 값 설정. 체인 가능.  
(용도: SmartMovingProperties 등에서 이동 모드별 키를 설정할 때 사용하는 것으로 보임 — 코드 내 호출처 확인 필요)

---

## `GetAllKeys()` (public static)

```java
public static Iterator<String> GetAllKeys(Value<?>... values) {
    return GetAllKeys(null, values);
}

public static Iterator<String> GetAllKeys(String[] sorted, Value<?>... values) {
    _allkeys.clear();
    for(int i = 0; i < values.length; i++) {
        Value<?> value = values[i];
        if(value.keyValues != null) {
            Enumeration<String> keys = value.keyValues.keys();
            while(keys.hasMoreElements())
                _allkeys.add(keys.nextElement());
        }
    }
    Collections.sort(_allkeys);
    _allkeys.add(0, Null);  // Null 키("null")를 맨 앞에 삽입

    for(int i = 0, n = 1; sorted != null && i < sorted.length; i++) {
        String sort = sorted[i];
        if(sort == null || sort == Null)
            continue;
        if(_allkeys.remove(sort))
            _allkeys.add(n++, sort);
    }
    return _allkeys.iterator();
}
```

**동작**:
1. 인자로 받은 모든 `Value`의 `keyValues` 키를 `_allkeys`에 수집 (중복 허용).
2. 알파벳 정렬.
3. 맨 앞에 `Null`("null") 삽입 → 기본값 슬롯을 첫 번째로 처리.
4. `sorted` 배열이 있으면, 해당 키들을 Null 뒤(n=1부터)에 순서대로 재배치.

**주의**: `_allkeys`는 static 공유 리스트. 이 메서드는 non-reentrant (단일 스레드 전제).

---

## `load(String content, boolean singular)`

```java
public Value<T> load(String content, boolean singular) {
    if(content == null || singular) {
        value = parsePropertyElement(content);
        return this;
    }

    String[] elements = content.split(";");
    for(int i = 0; i < elements.length; i++) {
        String element = elements[i];
        int seperatorIndex = element.indexOf(':');
        String key = null;
        String keyValue = null;
        if(seperatorIndex > 0) {
            key = element.substring(0, seperatorIndex);
            keyValue = element.substring(seperatorIndex + 1);
        } else
            keyValue = element;

        T value = parsePropertyElement(keyValue);
        if(value != null)
            put(key, value);
        else {
            if(unparsableStrings == null)
                unparsableStrings = new ArrayList<String>();
            unparsableStrings.add(keyValue);
        }
    }
    return this;
}
```

**`singular == true` 또는 `content == null`**: `value = parsePropertyElement(content)` — 단일 파싱.

**`singular == false` + `content != null`**: `;` 구분자로 분할.  
각 요소에서 `:` 위치 탐색:
- `seperatorIndex > 0`: `key = element[0..sep)`, `keyValue = element[sep+1..]`
- `seperatorIndex <= 0`: `key = null` (기본 슬롯), `keyValue = element`

파싱 성공 → `put(key, value)`. 실패 → `unparsableStrings`에 추가.

**오타**: `seperatorIndex` — `separator`의 오타. 원본 코드 그대로.

---

## `parsePropertyElement(String)` (private)

```java
private T parsePropertyElement(String stringToParse) {
    int baseType = Properties.getBaseType(type);
    if(baseType == Properties.Boolean)  return (T)tryParseBoolean(stringToParse);
    if(baseType == Properties.Float)    return (T)tryParseFloat(stringToParse);
    if(baseType == Properties.Integer)  return (T)tryParseInteger(stringToParse);
    if(baseType == Properties.Strings)  return (T)tryParseStrings(stringToParse);
    if(baseType == Properties.StringMap) return (T)tryParseStringMap(stringToParse);
    if(baseType == Properties.IntegerMap) return (T)tryParseIntegerMap(stringToParse);
    if(baseType == Properties.String)   return (T)tryParseString(stringToParse);
    return null;
}
```

`Properties.getBaseType(type)`으로 타입 매핑 후 파서 선택.

---

## 파서 메서드 (public static)

### `tryParseBoolean(String value)`

```java
public static Boolean tryParseBoolean(String value) {
    try {
        if(value != null)
            return value.equals("true") ? Boolean.TRUE : value.equals("false") ? Boolean.FALSE : null;
    } catch(Exception e) {}
    return null;
}
```

`"true"` → `true`, `"false"` → `false`, 그 외 → `null`.

### `tryParseFloat(String value)`

```java
public static Float tryParseFloat(String value) {
    try {
        if(value != null) return Float.parseFloat(value);
    } catch(Exception e) {}
    return null;
}
```

파싱 실패 시 `null`.

### `tryParseInteger(String value)`

```java
public static Integer tryParseInteger(String value) {
    try {
        if(value != null) return Integer.parseInt(value);
    } catch(Exception e) {}
    return null;
}
```

### `tryParseString(String value)`

```java
public static String tryParseString(String value) {
    return value == null ? null : value;
}
```

`null` → `null`, 그 외 → 그대로.

### `tryParseStrings(String value)`

```java
public static String[] tryParseStrings(String value) {
    return value == null ? null : (value.isEmpty() ? new String[0] : value.split(","));
}
```

`,` 구분자로 분할. 빈 문자열 → 빈 배열.

### `tryParseStringMap(String value)`

```java
public static Map<String,String> tryParseStringMap(String value) {
    String[] keyValues = value.split(",");
    Map<String,String> result = new HashMap<String, String>();
    for(int i = 0; i < keyValues.length; i++) {
        String key = keyValues[i++];
        if(i < keyValues.length)
            result.put(key, keyValues[i]);
    }
    return result;
}
```

`,`으로 분할 후 짝수 인덱스=키, 홀수 인덱스=값으로 Map 구성.

### `tryParseIntegerMap(String value)`

```java
public static Map<String,Integer> tryParseIntegerMap(String value) {
    String[] keyValues = value.split(",");
    Map<String,Integer> result = new HashMap<String, Integer>();
    for(int i = 0; i < keyValues.length; i++) {
        String key = keyValues[i++];
        if(i < keyValues.length)
            result.put(key, Integer.parseInt(keyValues[i]));
    }
    return result;
}
```

키=String, 값=Integer.

---

## 키 ↔ 코드 변환 (static)

### `toKeyName(Integer keyCode)` (public static)

```java
public static String toKeyName(Integer keyCode) {
    if(keyCode == null) return null;
    if(keyCode >= 0)
        return Keyboard.getKeyName(keyCode);
    return Mouse.getButtonName(keyCode + 100);
}
```

- `keyCode >= 0` → `Keyboard.getKeyName(keyCode)`
- `keyCode < 0` → `Mouse.getButtonName(keyCode + 100)`

마우스 버튼은 음수 키코드 + 100 오프셋으로 저장.

### `toKeyCode(String keyName)` (private static)

```java
private static Integer toKeyCode(String keyName) {
    if(keyName == null) return null;
    keyName = keyName.toUpperCase();
    int keyCode = Keyboard.getKeyIndex(keyName);
    if(keyCode > 0) return keyCode;
    keyCode = Mouse.getButtonIndex(keyName);
    if(keyCode >= 0) return keyCode - 100;
    return null;
}
```

- 키보드: `Keyboard.getKeyIndex(keyName)` > 0 → 반환
- 마우스: `Mouse.getButtonIndex(keyName)` >= 0 → `index - 100` (음수로 저장)
- 매칭 없으면 `null`

---

## `toBlockConfig(String[])` (private static)

```java
private static Dictionary<Object, Set<Integer>> toBlockConfig(String[] config) {
    if(config == null) return null;
    Dictionary<Object, Set<Integer>> result = new Hashtable<Object, Set<Integer>>();
    for(int i = 0; i < config.length; i++) {
        String[] elements = config[i].split("/");
        if(elements.length > 0) {
            Set<Integer> metaDatas = new HashSet<Integer>();
            String blockName = elements[0];
            result.put(blockName, metaDatas);
            if(blockName.matches("[0-9]+"))
                result.put(Integer.parseInt(blockName), metaDatas);
            for(int n = 1; n < elements.length; n++) {
                String metaDataText = elements[n];
                if(metaDataText.matches("[0-9]+"))
                    metaDatas.add(Integer.parseInt(metaDataText));
            }
        }
    }
    return result;
}
```

**각 원소 형식**: `"blockName/meta1/meta2/..."` — `/`로 분할.  
`elements[0]` = 블록 이름(String 키로 저장). 숫자이면 Integer 키로도 저장.  
`elements[1..]` = 메타데이터(숫자이면 Set에 추가).  
결과: `Dictionary<Object, Set<Integer>>` (String 또는 Integer 키 → 메타데이터 집합).

---

## `getUnparsableStrings()`

```java
public Iterator<String> getUnparsableStrings() {
    if(unparsableStrings != null)
        return unparsableStrings.iterator();
    return null;
}
```

파싱 실패 항목 없으면 `null` 반환 (빈 Iterator가 아님).

---

## `print(PrintWriter, String[])` / `toString()`

### `print(PrintWriter printer, String[] sorted)`

```java
public void print(PrintWriter printer, String[] sorted) {
    boolean first = true;
    Iterator<String> keys = GetAllKeys(sorted, this);
    while(keys.hasNext()) {
        String key = keys.next();
        if(key == Null && value == null)
            continue;  // 기본 슬롯이 null이면 생략

        if(!first)
            printer.print(";");
        else
            first = false;

        if(key != Null) {
            printer.print(key);
            printer.print(":");
        }
        printDisplayString(printer, get(key));
    }
}
```

출력 형식: `key:value;key2:value2;...`  
기본 슬롯(`Null` 키)은 키 접두사 없이 값만 출력.

### `toString()`

```java
@Override
public String toString() {
    StringWriter result = new StringWriter();
    print(new PrintWriter(result), null);
    return result.toString();
}
```

---

## `createDisplayString(Object value)` (public static)

```java
public static String createDisplayString(Object value) {
    if(value instanceof String[] || value instanceof Map) {
        StringWriter result = new StringWriter();
        printDisplayString(new PrintWriter(result), value);
        return result.toString();
    }
    return getDisplayString(value);
}
```

`String[]` / `Map` → `printDisplayString()`. 그 외 → `getDisplayString()`.

### `printDisplayString(PrintWriter, Object)` (private static)

```java
private static void printDisplayString(PrintWriter printer, Object value) {
    if(value instanceof String[]) {
        boolean first = true;
        String[] values = (String[])value;
        for(int i = 0; i < values.length; i++) {
            if(first) first = false;
            else printer.print(",");
            printer.print(getDisplayString(values[i]));
        }
    } else if(value instanceof Map) {
        boolean first = true;
        Map<?,?> values = (Map<?,?>)value;
        List<String> keys = new ArrayList<String>(values.size());
        for(Object key : values.keySet())
            keys.add((String)key);
        Collections.sort(keys);
        for(int i = 0; i < keys.size(); i++) {
            if(first) first = false;
            else printer.print(",");
            String key = keys.get(i);
            printer.print(getDisplayString(key));
            printer.print(",");
            printer.print(getDisplayString(values.get(key)));
        }
    } else
        printer.print(getDisplayString(value));
}
```

**`String[]`**: `,` 구분자로 연결.  
**`Map`**: 키 정렬 후 `key,value,key,value,...` 형식으로 출력.  
**그 외**: `getDisplayString()`.

### `getDisplayString(Object value)` (private static)

```java
private static String getDisplayString(Object value) {
    String result = value.toString();
    if(result.endsWith(".0"))
        result = result.substring(0, result.length() - 2);
    return result;
}
```

`.0` suffix 제거 (예: `"1.0"` → `"1"`).

---

## `equals(Object)` / `hashCode()`

```java
@Override
public boolean equals(Object other) {
    if(!(other instanceof Value<?>)) return false;
    Value<T> otherValue = (Value<T>)other;
    if((value == null) != (otherValue.value == null)) return false;
    if(value != null && !valuesEqual(value, otherValue.value)) return false;
    if((keyValues == null ? 0 : keyValues.size()) != (otherValue.keyValues == null ? 0 : otherValue.keyValues.size()))
        return false;
    if(keyValues != null && keyValues.size() != 0) {
        Enumeration<String> keys = keyValues.keys();
        while(keys.hasMoreElements()) {
            String key = keys.nextElement();
            T otherKeyValue = otherValue.keyValues.get(key);
            if(otherKeyValue == null) return false;
            T keyValue = keyValues.get(key);
            if(!valuesEqual(keyValue, otherKeyValue)) return false;
        }
    }
    return true;
}

@Override
public int hashCode() {
    return value == null ? 0 : value.hashCode();
}
```

**비교 순서**: 기본값 null 여부 → `valuesEqual(value)` → keyValues 크기 → 각 키별 `valuesEqual`.

### `valuesEqual(T first, T second)` (private)

```java
private boolean valuesEqual(T first, T second) {
    if(!(first instanceof Object[]))
        return first.equals(second);
    Object[] firstArray = (Object[])first;
    Object[] secondArray = (Object[])second;
    if(firstArray.length != secondArray.length) return false;
    for(int i = 0; i < firstArray.length; i++)
        if(!firstArray[i].equals(secondArray[i])) return false;
    return true;
}
```

배열 타입(`Object[]`)은 원소별 비교. 그 외는 `equals()`.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `Properties` | `getBaseType(type)` — 파서 선택 기준; 타입 상수 (Boolean/Float/Integer 등) |
| `org.lwjgl.input.Keyboard` | `getKeyName()`, `getKeyIndex()` |
| `org.lwjgl.input.Mouse` | `getButtonName()`, `getButtonIndex()` |

---

## 주요 관찰 사항

1. **`Null = "null"` sentinel**: null 리터럴을 키로 사용하지 않고 `"null"` 문자열 상수를 사용. `Hashtable`이 null 키를 허용하지 않기 때문.

2. **`_allkeys` 공유 static 리스트**: `GetAllKeys()` 호출마다 `clear()` 후 재사용. 중첩 호출 시 결과가 오염됨 (non-reentrant). 실제로는 단일 스레드(클라이언트 틱)에서만 호출되므로 문제 없음.

3. **마우스 키코드 인코딩**: `keyCode < 0` → 마우스 버튼. `toKeyName`에서 `keyCode + 100`으로 LWJGL Mouse API 인덱스로 변환. `toKeyCode`에서 반대로 `index - 100`.

4. **`load()` `:` 위치 조건**: `seperatorIndex > 0` — `> 0`이므로 문자열 첫 글자가 `:` (`seperatorIndex == 0`)이면 키 없는 것으로 처리.

5. **`getDisplayString()` `.0` 제거**: Float 값의 정수 표현 정규화 (`1.0` → `1`).

6. **`tryParseStringMap()` / `tryParseIntegerMap()` null 미처리**: `value == null`인 경우 `NullPointerException` 발생 가능 (`value.split(",")`). `tryParseBoolean/Float/Integer`와 달리 null 체크 없음.

7. **`e/m/h/c/a` 단축 메서드**: 각각 키 `"e"`, `"m"`, `"h"`, `"c"`, `"a"`로 put. 실제 의미는 SmartMovingProperties 등 호출부에서 확인 필요.

8. **1.21.1 이식 관련**:
   - `org.lwjgl.input.Keyboard` / `Mouse` → `net.minecraft.client.util.InputUtil` (LWJGL3 기반)
   - `Dictionary<String, T>` → `Map<String, T>` 교체 권장 (Dictionary는 레거시)
   - `Hashtable` → `HashMap` 교체 가능 (null 키/값 사용 안 함이 확인되므로)
   - `toBlockConfig()` 반환 타입 `Dictionary<Object, Set<Integer>>` → 1.21.1 블록 식별 체계(Identifier 기반)로 교체 필요
   - `_allkeys` static 공유 리스트 패턴은 유지 가능 (단일 스레드 전제 동일)
