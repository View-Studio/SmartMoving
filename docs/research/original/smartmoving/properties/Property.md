# Property.java (net.smart.properties) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/properties/Property.java  
패키지: `net.smart.properties`  
종류: `class` (제네릭)  
전체 라인: 826줄

---

## 역할

설정 프로퍼티 하나를 표현하는 선언형 타입. 단순 값 보유자가 아니라 연산자 조합(And/Or/Plus 등), 버전별 소스/기본값, 의존성(depends), 범위(min/max), 파일 출력 포맷까지 통합한다.

---

## import

```java
import java.io.*;
import java.util.*;
```

---

## 연산자 상수 (static final int, i++ 패턴)

```java
private static int i = 0;
private static final int Is = i++;           // 0
private static final int And = i++;          // 1
private static final int Or = i++;           // 2
private static final int Not = i++;          // 3
private static final int Plus = i++;         // 4
private static final int EitherOr = i++;     // 5
private static final int Maximum = i++;      // 6
private static final int Minimum = i++;      // 7
private static final int ToKeyName = i++;    // 8
private static final int ToKeyCode = i++;    // 9
private static final int ToBlockConfig = i++;// 10
```

총 11개. `Properties.java`의 타입 상수(Boolean=0~IntegerMap=17)와는 별개의 i 카운터.

---

## 필드

```java
private static final int printWidth = 69;
private String comment;
private String[] header;
private int gap;                            // section=1, chapter=2, book=3

private boolean explicitlyModified;         // "!" suffix가 있었을 때
private boolean implicitlyModified;         // 값이 기본값과 다를 때
private String aquiredString;              // 파일에서 읽은 원본 문자열 (오타 아님, 원본 그대로)
private boolean singular;                  // singular() 호출 여부

private final int type;                    // Properties.Boolean 등 타입 상수
private String currentVersion;             // key() 첫 호출 시 설정
private Map<String, Object> versionSources;  // 버전 → 소스(Property<Key> 또는 Value)
private Map<String, Object> versionDefaults; // 버전 → 기본값

private static final int Is = ...; // 위 참조

public T value;                            // 현재 유효값 (public)
private Value<T> systemValue;             // 로드 완료 후 최종 Value
private Value<T> aquiredValue;            // 파일에서 파싱된 Value

private Object minValue;
private Object maxValue;

private Object local;                     // EitherOr 연산자의 조건 피연산자
private Object left;                      // 연산자 왼쪽 피연산자
private int operator;                     // Is/And/Or 등
private Object right;                     // 연산자 오른쪽 피연산자 (Not 등은 null)

private List<Property<Boolean>> depends = null; // 의존 프로퍼티 목록

private static final String Current = "";          // 현재 버전 키 (빈 문자열)
private static final String[] CurrentArray = new String[] { Current };
```

---

## 생성자 4개

### `public Property(int type)`

```java
public Property(int type) {
    this.type = type;
}
```

기본 생성자. `Properties.java`의 팩토리 메서드에서 이 생성자를 호출.

### `private Property(String key)` — Key 타입

```java
private Property(String key) {
    this(Properties.Key);
    set(new Value(key));
}
```

`Properties.Key` 타입의 Property 생성. `value`에 키 문자열을 저장.  
용도: `key()` 빌더 메서드에서 내부적으로 생성.

### `private Property(Object left, int operator, Object right)` — Operator 타입 (2항)

```java
private Property(Object left, int operator, Object right) {
    this(Properties.Operator);
    this.left = left;
    this.operator = operator;
    this.right = right;
}
```

Is/And/Or/Not/Plus/Maximum/Minimum/ToKeyName/ToKeyCode/ToBlockConfig에 사용.  
`Not`/`ToKeyName`/`ToKeyCode`/`ToBlockConfig`는 `right = null`로 호출됨.

### `private Property(Object local, int operator, Object left, Object right)` — Operator 타입 (3항)

```java
private Property(Object local, int operator, Object left, Object right) {
    this(Properties.Operator);
    this.local = local;
    this.operator = operator;
    this.left = left;
    this.right = right;
}
```

`EitherOr` 전용. `local`이 조건, `left`/`right`가 either/or 값.

---

## 빌더 메서드 (연산자 생성)

모두 새 `Property` 인스턴스를 반환. 원본 Property는 수정되지 않음.

```java
public Property<Boolean> is(Object value)       → new Property<Boolean>(this, Is, value)
public Property<Boolean> and(Object value)      → new Property<Boolean>(this, And, value)
public Property<Boolean> or(Object value)       → new Property<Boolean>(this, Or, value)
public Property<Boolean> andNot(Property<?> v)  → and(v.not())
public Property<Boolean> not()                  → new Property<Boolean>(this, Not, null)
public Property<Float> plus(Object value)       → new Property<Float>(this, Plus, value)
public Property<?> eitherOr(Object either, Object or) → new Property<Float>(this, EitherOr, either, or)
public Property<Float> maximum(Object value)    → new Property<Float>(this, Maximum, value)
public Property<Float> minimum(Object value)    → new Property<Float>(this, Minimum, value)
public Property<String> toKeyName()             → new Property<String>(this, ToKeyName, null)
public Property<Integer> toKeyCode(Integer defaultValue) → new Property<Integer>(this, ToKeyCode, null).defaults(defaultValue)
public Property<Dictionary<Object, Set<Integer>>> toBlockConfig() → new Property<...>(this, ToBlockConfig, null)
```

---

## 빌더 메서드 (설정)

모두 `this`를 반환하여 체인 가능.

### `depends(Property<Boolean>... conditions)`

```java
public Property<T> depends(Property<Boolean>... conditions) {
    if(depends == null)
        depends = new LinkedList<Property<Boolean>>();
    for(int i = 0; i < conditions.length; i++)
        depends.add(conditions[i]);
    return this;
}
```

의존 조건 추가. 복수 호출 시 누적.

### `values(Object defaultValue, Object minValue, Object maxValue)`

```java
public Property<T> values(...) { return defaults(defaultValue).range(minValue, maxValue); }
```

### `up(Object defaultValue, Object minValue)`

```java
public Property<T> up(...) { return defaults(defaultValue).min(minValue); }
```

### `down(Object defaultValue, Object maxValue)`

```java
public Property<T> down(...) { return defaults(defaultValue).max(maxValue); }
```

### `range(Object minValue, Object maxValue)`

```java
public Property<T> range(...) { return min(minValue).max(maxValue); }
```

### `defaults(Object defaultValue, String... versions)`

```java
public Property<T> defaults(Object defaultValue, String... versions) {
    versionDefaults = addVersioned(versionDefaults, defaultValue, versions);
    return this;
}
```

버전별 기본값 설정. `versions`가 없으면 `Current`(빈 문자열) 키로 저장.

### `min(Object minValue)` / `max(Object maxValue)`

필드 직접 설정.

### `key(String key, String... versions)`

```java
public Property<T> key(String key, String... versions) {
    if(currentVersion == null)
        if(versions != null && versions.length > 0)
            currentVersion = versions[0];
        else
            currentVersion = Current;
    return source(new Property(key), versions);
}
```

**`currentVersion` 설정**: 첫 `key()` 호출 시에만 설정. `versions`가 있으면 `versions[0]`, 없으면 `Current`("").  
내부적으로 `new Property(key)` (Key 타입)을 생성하여 `source()`에 전달.

### `source(Object source, String... versions)`

```java
public Property<T> source(Object source, String... versions) {
    versionSources = addVersioned(versionSources, source, versions);
    return this;
}
```

`versionSources` 맵에 버전→소스 등록.

### `comment(String)` / `section(String...)` / `chapter(String...)` / `book(String...)`

출력 포맷용. `gap`: section=1, chapter=2, book=3. `header`: 제목/설명 배열.

---

## `addVersioned()` (private static)

```java
private static Map<String, Object> addVersioned(Map<String, Object> versioned, Object value, String... versions) {
    if(versioned == null)
        versioned = new Hashtable<String, Object>(1);
    if(versions == null || versions.length == 0)
        versions = CurrentArray;
    for(int i = 0; i < versions.length; i++)
        versioned.put(versions[i], value);
    return versioned;
}
```

`versionSources`/`versionDefaults` 공통 헬퍼. `Hashtable` 사용 (null 키/값 불허).

---

## `reset()`

```java
public void reset() {
    explicitlyModified = false;
    implicitlyModified = false;
    value = null;
    systemValue = null;
    aquiredValue = null;
    reset(minValue);
    reset(maxValue);
    reset(left);
    reset(right);
    if(depends != null)
        for(int i = 0; i < depends.size(); i++)
            reset(depends.get(i));
}

private static void reset(Object value) {
    if(value instanceof Property)
        ((Property<?>)value).reset();
}
```

재귀적 리셋. `local`은 리셋 대상에 없음.

---

## `load(Properties... propertiesList)`

```java
public boolean load(Properties... propertiesList)
```

반환값: 로드 성공 여부 (`boolean`).  
`systemValue != null`이거나 `type == Constant`이면 즉시 `true`.

### Operator 타입 처리

```java
if(type == Properties.Operator) {
    // EitherOr: local/left/right 모두 필요
    // Is/And/Or/Plus/Maximum/Minimum: left/right 필요
    // Not/ToKeyName/ToKeyCode/ToBlockConfig: left만 필요
    
    if(getValue(left) == null || ...) return false;
    
    Object operatorValue = null;
    if(operator == Is)          operatorValue = getValue(left).is(getValue(right));
    else if(operator == And)    operatorValue = getValue(left).and(getValue(right));
    else if(operator == Or)     operatorValue = getValue(left).or(getValue(right));
    else if(operator == Not)    operatorValue = getValue(left).not();
    else if(operator == Plus)   operatorValue = getValue(left).plus(getValue(right));
    else if(operator == EitherOr) operatorValue = getValue(local).eitherOr(getValue(left), getValue(right));
    else if(operator == Maximum) operatorValue = getValue(left).maximum(getValue(right));
    else if(operator == Minimum) operatorValue = getValue(left).minimum(getValue(right));
    else if(operator == ToKeyName)    operatorValue = getValue(left).toKeyName();
    else if(operator == ToKeyCode)    operatorValue = getValue(left).toKeyCode();
    else if(operator == ToBlockConfig) operatorValue = getValue(left).toBlockConfig();
    
    if(operatorValue == null)
        throw new RuntimeException("Unknown operator '" + operator + "'");
    return set(getValue(operatorValue));
}
```

`getValue(left)` 등은 `Value<T>` 반환. 연산 결과도 `Value`이며 `set()`으로 `systemValue` 확정.

### 일반 타입 처리

```java
if(propertiesList == null || versionSources == null) return false;

// depends 확인
if(depends != null)
    for(int i = 0; i < depends.size(); i++)
        if(getValue(depends.get(i)) == null) return false;

// min/max Value 취득 (null이면 통과, 취득 실패면 return false)
Object minObject = getMinimumValue();
Value<T> minValue = getValue(minObject);
if(minObject != null && minValue == null) return false;

Object maxObject = getMaximumValue();
Value<T> maxValue = getValue(maxObject);
if(maxObject != null && maxValue == null) return false;

Value<T> defaultValue = getValue(getDefaultValue());

// propertiesList 순서대로 버전 매칭
for(int i = 0; i < propertiesList.length; i++) {
    Properties properties = propertiesList[i];
    Object source = getVersionSource(properties.version);
    if(source != null) {
        String key = getKey(source);
        aquiredValue = key != null ? getPropertyValue(properties, key) : getValue(source);
        if(aquiredValue != null) {
            Value<T> initValue = aquiredValue.clone();
            if(depends != null)
                for(int n = 0; n < depends.size(); n++)
                    initValue.withDependency(getValue(depends.get(n)), defaultValue);
            if(minObject != null) initValue.withMinimum(minValue, defaultValue);
            if(maxObject != null) initValue.withMaximum(maxValue, defaultValue);
            return set(initValue);
        } else if(key == null)
            return false;
    }
}
return set(defaultValue);
```

**처리 순서**: depends → min/max → propertiesList 순회 → 매칭 source에서 key 추출 → `getPropertyValue()` → depends/min/max 적용 → `set()`.  
`propertiesList` 전부 매칭 실패 시 `defaultValue`로 세팅.

---

## `getValue(Object value)` (private)

```java
private Value<T> getValue(Object value) {
    if(value instanceof Property) {
        Property<?> property = (Property<?>)value;
        property.load((Properties[])null);  // null 전달로 캐시된 systemValue만 반환
        return (Value<T>)property.systemValue;
    }
    if(value instanceof Value<?>)
        return (Value<T>)value;
    return new Value<T>((T)value);  // 원시값 → Value 래핑
}
```

`null` propertiesList로 `load()`를 호출해 이미 로드된 `systemValue`만 취득.

---

## `getPropertyValue(Properties, String)` (private)

```java
private Value<T> getPropertyValue(Properties properties, String key) {
    String propertyString = properties.getProperty(key);
    if(propertyString != null) aquiredString = propertyString;

    String stringToParse = propertyString;
    if(propertyString != null) {
        stringToParse = propertyString.trim();
        explicitlyModified = stringToParse.endsWith("!");
        if(explicitlyModified)
            stringToParse = stringToParse.substring(0, stringToParse.length() - 1);
        stringToParse = stringToParse.trim();
    }

    Value<T> value = parsePropertyValue(stringToParse);
    implicitlyModified = stringToParse != null && (value == null || !value.equals(getValue(getDefaultValue(properties.version))));
    if(!explicitlyModified && !implicitlyModified)
        return getValue(getDefaultValue());
    return value;
}
```

**`"!"` suffix**: `explicitlyModified = true`, suffix 제거 후 파싱.  
**`implicitlyModified`**: 파싱된 값이 기본값과 다를 때 true.  
**기본값 반환 조건**: `!explicitlyModified && !implicitlyModified` → 기본값 반환 (파싱 결과 무시).

---

## `parsePropertyValue(String)` (private)

```java
private Value<T> parsePropertyValue(String stringToParse) {
    if(stringToParse != null)
        return new Value<T>(type).load(stringToParse, singular);
    return null;
}
```

`null` 입력 → `null` 반환. 아니면 `new Value<T>(type).load(string, singular)`.

---

## `set(Value<T>)` (private)

```java
private boolean set(Value<T> initValue) {
    systemValue = initValue;
    update(null);
    return true;
}
```

`update(null)`: `value = systemValue.get(null)` — null 키(기본값)로 `value` 필드 갱신.

---

## `update(String key)`

```java
public void update(String key) {
    value = getKeyValue(key);
}
```

### `getKeyValue(String key)`

```java
public T getKeyValue(String key) {
    T value = systemValue.get(key);
    if(value == null)
        value = getValue(getDefaultValue()).get(null);
    if(value == null)
        value = (T)Properties.getDefaultValue(type);
    return value;
}
```

**조회 순서**: systemValue.get(key) → defaultValue(null 키) → Properties 전역 기본값.

---

## `setValue(T value)`

```java
public void setValue(T value) {
    this.value = value;
    systemValue = new Value<T>(value);
    aquiredValue = new Value<T>(value);
    implicitlyModified = false;
    explicitlyModified = false;
}
```

외부에서 직접 값 설정 (파일 로드 없이).

---

## `getDefaultValue()` (private, 오버로드 2개)

```java
private Object getDefaultValue(String version) {
    Object defaultValue = null;
    if(versionDefaults != null) {
        if(version != null) defaultValue = versionDefaults.get(version);
        if(defaultValue == null) defaultValue = versionDefaults.get(Current);
    }
    if(defaultValue == null) defaultValue = Properties.getDefaultValue(type);
    return defaultValue;
}

private Object getDefaultValue() { return getDefaultValue(Current); }
```

**조회 순서**: versionDefaults[version] → versionDefaults[""] → Properties.getDefaultValue(type).

---

## `getMinimumValue()` / `getMaximumValue()` (private)

```java
private Object getMinimumValue() {
    if(minValue != null) return minValue;
    return Properties.getMinimumValue(type);
}

private Object getMaximumValue() {
    if(maxValue != null) return maxValue;
    return Properties.getMaximumValue(type);
}
```

인스턴스 min/max 없으면 Properties 전역 min/max 사용.

---

## `getVersionSource(String version)` (private)

```java
private Object getVersionSource(String version) {
    if(versionSources == null) return null;
    Object source = null;
    if(version != null) source = versionSources.get(version);
    if(source == null) source = versionSources.get(Current);
    return source;
}
```

버전 매칭 없으면 Current("") 폴백.

---

## `print(PrintWriter, String[], String, boolean)` (public)

```java
public boolean print(PrintWriter printer, String[] sorted, String version, boolean comments)
```

반환값: 출력 여부.

**출력 조건**: `isPersistent() && systemValue != null && getVersionSource(version) != null`.

### gap/header 출력

```java
int gap = this.gap + (comment == null ? -1 : 1);
for(int i = 0; i < gap; i++) printer.println();
if(header != null && header.length > 0) printHeader(printer);
if(comment != null && comments) { printer.print("# "); printer.println(comment); }
```

### 값 출력 경우 분기 (`aquiredString == null`이면 직접 출력)

`aquiredString != null`(파일에서 읽힌 경우):

1. **파싱 실패 항목** (`aquiredValue.getUnparsableStrings()`):
   ```
   #!! Could not interpret string "X" as <type> value, used local/system default ... !!!#
   ```

2. **범위 초과 또는 depend 실패** (systemValue ≠ aquiredValue):
   - depend 실패 → `#! Interpreted value "X" is ignored because ... !#` (경고)
   - 범위 초과 → `#!! Interpreted value "X" was out of range, used minimum/maximum/in-range ... !!!#` (오류)

3. **`printValue()`**:
   ```java
   printer.print(getCurrentKey());
   printer.print(":");
   if(aquiredString != null && (error || explicitlyModified))
       printer.print(aquiredString);
   else if(implicitlyModified && systemValue.equals(getValue(getDefaultValue()))) {
       printer.print(aquiredString);
       printer.print("!");
   } else
       systemValue.print(printer, sorted);
   ```

---

## `printHeader(PrintWriter)` (private)

```java
private void printHeader(PrintWriter printer) {
    String title = header[0];
    String body = header.length > 1 ? header[1] : null;
    char separator = gap == 3 ? '=' : (gap == 2 ? '-' : ' ');
    printSeparation(printer, separator, printWidth);
    printer.print("# ");
    printer.println(title);
    if(body != null) {
        printSeparation(printer, '-', title.length());
        int lineWidth = printWidth - 2;
        while(true) {
            printer.print("# ");
            if(body.length() <= lineWidth) { printer.println(body); break; }
            int i;
            for(i = lineWidth; i > 0; i--)
                if(body.charAt(i) == ' ') break;
            printer.println(body.substring(0, i));
            body = body.substring(i + 1);
        }
    }
    printSeparation(printer, separator, printWidth);
    printer.println();
}
```

**구분자**: gap=3→`=`, gap=2→`-`, gap=1→` `(공백).  
**body 줄 나눔**: printWidth(69)-2=67자 이하 → 그대로; 초과 → 공백 기준으로 분리.

---

## 오류/경고 출력 헬퍼 (private static)

```java
printErrorPrefix: "#!! "   (오류)
printWarnPrefix:  "#! "    (경고)
printErrorPostfix: " !!!#" (오류)
printWarnPostfix:  " !#"   (경고)
```

---

## `isPersistent()`

```java
public boolean isPersistent() {
    return versionSources != null && versionSources.size() > 0;
}
```

`key()` 또는 `source()`가 한 번이라도 호출된 경우 true.

---

## `getCurrentKey()`

```java
public String getCurrentKey() {
    if(isPersistent())
        return getKey(getVersionSource(currentVersion));
    return null;
}

private static String getKey(Object candidate) {
    if(candidate instanceof Property) {
        Property<?> property = (Property<?>)candidate;
        if(property.type == Properties.Key)
            return (String)property.value;
    }
    return null;
}
```

`currentVersion` 버전 소스가 `Properties.Key` 타입이면 키 문자열 반환, 아니면 `null`.

---

## `toString()`

```java
@Override
public String toString() {
    if(isPersistent()) return getCurrentKey();
    return super.toString();
}
```

---

## 문자열 유틸리티

```java
public String getKeyValueString(String key) { return getValueString(getKeyValue(key)); }
public String getValueString() { return getValueString(value); }
public String getValueString(T value) { return value != null ? Value.createDisplayString(value) : null; }
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `Properties` | 타입 상수(Key/Operator/Constant 등), `getDefaultValue/getMinimumValue/getMaximumValue/getBaseTypeName/getBaseType` |
| `Value<T>` | 실제 값 보유, 파싱, 연산(is/and/or 등), clone/withDependency/withMinimum/withMaximum/getUnparsableStrings/GetAllKeys/createDisplayString |

---

## 주요 관찰 사항

1. **`Current = ""`**: 버전 키로 빈 문자열을 사용. `Hashtable` null 불허이므로 빈 문자열로 "버전 없음" 구분.

2. **`aquiredString` 오타**: `acquired` 대신 `aquired`. 원본 코드 전체 일관된 오타.

3. **Operator Property는 `propertiesList` 불필요**: `load(null)`로도 동작. `getValue(left/right)` 경로에서 좌우 피연산자의 `systemValue`만 참조.

4. **`key()` + `source()` 분리**: `key(String, String...)`은 내부적으로 `new Property(key)` 생성 후 `source()`에 전달. `source(Object, String...)`은 임의 객체를 소스로 등록 가능 (Key 타입 외 Value도 직접 소스 가능).

5. **버전 폴백**: `getVersionSource(version)` → 버전 없으면 `Current`("") 폴백. `getDefaultValue(version)`도 동일 패턴.

6. **`implicitlyModified` 판정**: `!explicitly && !implicitly` → 기본값 반환. 파싱 결과가 기본값과 같으면 수정 없은 것으로 취급.

7. **`singular`**: `parsePropertyValue()`에서 `Value.load(string, singular)` 에 전달. Value 내부에서 단수/복수 파싱 방식을 구분하는 것으로 추정 (Value.java 확인 필요).

8. **1.21.1 이식 관련**:
   - `Properties` 타입 상수 의존: Boolean/Float/Integer 등 — 1.21.1에서도 동일 타입 유지 가능
   - `Value<T>` 클래스 전체 이식 필요 (파싱/연산 로직 포함)
   - `Dictionary<Object, Set<Integer>>` (`ToBlockConfig` 반환 타입): 1.7.10 레거시 타입. 1.21.1에서는 `Map`으로 대체 가능
   - `printWidth = 69`, 구분자 문자('='/'~'/' ') 등 출력 포맷은 그대로 유지 가능
