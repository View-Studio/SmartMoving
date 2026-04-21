# SmartMovingProperties.java (net.smart.moving.config) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/config/SmartMovingProperties.java  
패키지: `net.smart.moving.config`  
종류: `abstract class`  
상속: `Properties` (extends, `net.smart.properties.Properties`)

상속 계층 (하위→상위):  
`SmartMovingOptions → SmartMovingClientConfig → SmartMovingConfig → SmartMovingProperties → Properties`

---

## 전체 소스

```java
package net.smart.moving.config;

import java.io.*;
import java.util.*;

import net.smart.properties.*;
import net.smart.properties.Properties;

public abstract class SmartMovingProperties extends Properties
{
    public final static String Enabled  = "enabled";
    public final static String Disabled = "disabled";
    private final static String[] _defaultKeys = new String[1];

    private int toggler = -2;
    private String[] keys = _defaultKeys;

    public boolean enabled;

    protected void load(Properties... propertiesList) throws Exception
    {
        List<Property<?>> propertiesToLoad = getProperties();
        if(toggler != -2)
        {
            Iterator<Property<?>> iterator = propertiesToLoad.iterator();
            while(iterator.hasNext())
                iterator.next().reset();
        }

        while (propertiesToLoad.size() > 0)
        {
            Iterator<Property<?>> iterator = propertiesToLoad.iterator();
            while(iterator.hasNext())
                if(iterator.next().load(propertiesList))
                    iterator.remove();
        }

        toggler = 0;
        update();
    }

    protected void save(File file, String version, boolean header, boolean comments) throws Exception
    {
        List<Property<?>> propertiesToSave = getProperties();

        FileOutputStream stream = new FileOutputStream(file);
        PrintWriter printer = new PrintWriter(stream);

        if(header)
            printHeader(printer);

        if(version != null)
            printVersion(printer, version, comments);

        for(int i = 0; i < propertiesToSave.size(); i++)
            if(propertiesToSave.get(i).print(printer, keys, version, comments) && i < propertiesToSave.size() - 1)
                printer.println();

        printer.close();
    }

    protected abstract void printVersion(PrintWriter printer, String version, boolean comments);

    protected abstract void printHeader(PrintWriter printer);

    public void toggle()
    {
        int length = keys == null ? 0 : keys.length;
        toggler++;
        if(toggler == length)
            toggler = -1;
        update();
    }

    public void setKeys(String[] keys)
    {
        if(keys == null || keys.length == 0)
            keys = _defaultKeys;
        this.keys = keys;
        toggler = 0;
        update();
    }

    public String getKey(int index)
    {
        if(keys[index] == null)
            return Enabled;
        return keys[index];
    }

    public String getNextKey(String key)
    {
        if(key == null || key.equals("disabled"))
            return getKey(0);
        int index;
        for(index = 0; index < keys.length; index++)
            if(key.equals(keys[index]))
                break;
        index++;
        if(index < keys.length)
            return keys[index];
        return Disabled;
    }

    public void setCurrentKey(String key)
    {
        if(key == null || key.equals(Disabled))
            toggler = -1;
        else if(keys.length == 1 && keys[0] == null && key.equals(Enabled))
            toggler = 0;
        else
        {
            for(toggler = 0; toggler < keys.length; toggler++)
                if(key.equals(keys[toggler]))
                    break;

            if(toggler == keys.length)
                toggler = -1;
        }
        update();
    }

    public String getCurrentKey()
    {
        if(toggler == -1)
            return Disabled;
        return keys[toggler];
    }

    public boolean hasKey(String key)
    {
        if(Enabled.equals(key))
            return keys[0] == null;
        if(Disabled.equals(key))
            return true;

        for(int i = 0; i < keys.length; i++)
            if(key == null && keys[i] == null || key != null && key.equals(keys[i]))
                return true;
        return false;
    }

    public int getKeyCount()
    {
        return keys.length;
    }

    protected void update()
    {
        List<Property<?>> properties = getProperties();
        Iterator<Property<?>> iterator = properties.iterator();

        String currentKey = getCurrentKey();
        while(iterator.hasNext())
            iterator.next().update(currentKey);
        enabled = toggler != -1;
    }
}
```

---

## 역할

SmartMoving 설정 시스템의 config key 토글 메커니즘과 `enabled` 상태를 관리하는 추상 클래스. `net.smart.properties.Properties`를 상속하며, SmartMoving 전용 config key 순환(toggling) 로직과 파일 저장 로직을 추가한다.

`Properties`(부모)가 제공하는 `getProperties()` — 선언된 모든 `Property<T>` 필드 목록 반환.

---

## import

```java
import java.io.*;            // File, FileOutputStream, PrintWriter
import java.util.*;          // List, Iterator
import net.smart.properties.*;
import net.smart.properties.Properties;
```

---

## 상수

```java
public final static String Enabled  = "enabled";
public final static String Disabled = "disabled";
private final static String[] _defaultKeys = new String[1];
// new String[1] → { null } — 길이 1, 값 null인 배열
```

`_defaultKeys`: 기본 keys 상태. `keys[0] == null`이면 단순 on/off 토글 모드 (config key 없음). `getKey(0)` 호출 시 `null` → `"enabled"` 반환.

---

## 인스턴스 필드

```java
private int toggler = -2;
private String[] keys = _defaultKeys;
public boolean enabled;
```

### `toggler` 상태표

| 값 | 의미 |
|----|------|
| `-2` | 초기화 전 (load() 미호출 상태) |
| `-1` | disabled 상태 (`enabled = false`) |
| `0 ~ keys.length-1` | enabled 상태, 현재 config key 인덱스 |

초기값 `-2`는 `load()` 내부에서 "이미 load된 적이 있는지" 판단하는 센티넬값으로만 사용.

### `keys`

현재 유효한 config key 배열. 예: `{"e", "m", "h"}` (easy/medium/hard) 또는 `{null}` (단순 on/off).

### `enabled`

`update()` 호출 시 `toggler != -1`로 설정. SmartMovingContext 및 각 클라이언트 메서드에서 직접 접근.

---

## 추상 메서드

```java
protected abstract void printVersion(PrintWriter printer, String version, boolean comments);
protected abstract void printHeader(PrintWriter printer);
```

`SmartMovingConfig`에서 구현.

---

## 메서드

### `load(Properties... propertiesList)`

```java
protected void load(Properties... propertiesList) throws Exception
{
    List<Property<?>> propertiesToLoad = getProperties();

    if(toggler != -2)                               // 이미 load된 적 있으면
    {
        Iterator<Property<?>> iterator = propertiesToLoad.iterator();
        while(iterator.hasNext())
            iterator.next().reset();                // 모든 Property 리셋
    }

    while (propertiesToLoad.size() > 0)             // 모든 Property가 로드될 때까지
    {
        Iterator<Property<?>> iterator = propertiesToLoad.iterator();
        while(iterator.hasNext())
            if(iterator.next().load(propertiesList))  // 로드 성공하면
                iterator.remove();                    // 목록에서 제거
    }

    toggler = 0;
    update();
}
```

**다중 패스 로딩**: `while (propertiesToLoad.size() > 0)` 반복 — 한 번에 로드 안 되는 Property가 있을 경우 반복 시도.  
이유: Property 간 의존성(`_isFreeBaseClimb = _baseClimb.is("free").and(_freeClimb)` 등)이 있어 참조 대상이 먼저 로드되어야 함.

**리셋 조건**: `toggler == -2`이면 최초 load → reset 건너뜀. `toggler != -2`이면 재로드 → 모든 Property를 기본값으로 reset 후 재로드.

**로드 완료 후**: `toggler = 0` (첫 번째 key로), `update()` 호출.

---

### `save(File file, String version, boolean header, boolean comments)`

```java
protected void save(File file, String version, boolean header, boolean comments) throws Exception
{
    List<Property<?>> propertiesToSave = getProperties();

    FileOutputStream stream = new FileOutputStream(file);
    PrintWriter printer = new PrintWriter(stream);

    if(header)
        printHeader(printer);

    if(version != null)
        printVersion(printer, version, comments);

    for(int i = 0; i < propertiesToSave.size(); i++)
        if(propertiesToSave.get(i).print(printer, keys, version, comments) && i < propertiesToSave.size() - 1)
            printer.println();   // 섹션 구분 빈 줄 (마지막 항목 제외)

    printer.close();
}
```

**출력 순서**:
1. 헤더 (header=true일 때)
2. 버전 (version != null일 때)
3. 각 Property의 `print(printer, keys, version, comments)` 호출

`print()` 반환값이 true → 해당 Property가 실제로 출력됨 → 다음 항목이 있으면 빈 줄 추가 (섹션 구분).

`keys` 파라미터: 현재 활성 config key 배열. Property가 각 key별로 값을 다르게 출력할 때 사용.

---

### `toggle()`

```java
public void toggle()
{
    int length = keys == null ? 0 : keys.length;
    toggler++;
    if(toggler == length)
        toggler = -1;
    update();
}
```

**순환 패턴**:  
`0 → 1 → ... → keys.length-1 → -1 → 0 → 1 → ...`

`keys.length == 3` (e/m/h)일 때: `0 → 1 → 2 → -1 → 0 → ...`  
`keys == _defaultKeys` (길이 1)일 때: `0 → -1 → 0 → ...` (on/off 토글)

`SmartMovingOptions.toggle()`이 이를 override하여 super.toggle() 호출.

---

### `setKeys(String[] keys)`

```java
public void setKeys(String[] keys)
{
    if(keys == null || keys.length == 0)
        keys = _defaultKeys;          // 기본 단순 on/off 모드
    this.keys = keys;
    toggler = 0;                      // 항상 첫 번째 key로 초기화
    update();
}
```

`SmartMovingOptions.initializeForGameIfNeccessary()`에서 게임 타입별 keys 배열을 전달.

---

### `getKey(int index)`

```java
public String getKey(int index)
{
    if(keys[index] == null)
        return Enabled;     // null key → "enabled" 문자열
    return keys[index];
}
```

`_defaultKeys[0] == null` → `getKey(0)` 반환값 = `"enabled"`.  
실제 key 배열(`{"e","m","h"}`)에서는 해당 문자열 그대로 반환.

---

### `getNextKey(String key)`

```java
public String getNextKey(String key)
{
    if(key == null || key.equals("disabled"))
        return getKey(0);                    // disabled/null → 첫 번째 key
    int index;
    for(index = 0; index < keys.length; index++)
        if(key.equals(keys[index]))
            break;
    index++;
    if(index < keys.length)
        return keys[index];                  // 다음 key
    return Disabled;                         // 마지막 key → "disabled"
}
```

**순환 없음**: 마지막 key 다음은 "disabled"로 끝남. "disabled" 다음은 첫 번째 key.  
UI에서 "다음 설정 키 표시"에 사용.

---

### `setCurrentKey(String key)`

```java
public void setCurrentKey(String key)
{
    if(key == null || key.equals(Disabled))
        toggler = -1;                              // disabled 설정
    else if(keys.length == 1 && keys[0] == null && key.equals(Enabled))
        toggler = 0;                               // 단순 on/off 모드에서 "enabled" 설정
    else
    {
        for(toggler = 0; toggler < keys.length; toggler++)
            if(key.equals(keys[toggler]))
                break;

        if(toggler == keys.length)                 // 매칭되는 key 없으면
            toggler = -1;                          // disabled로 처리
    }
    update();
}
```

**세 가지 경우**:
1. `key == null || key == "disabled"` → `toggler = -1`
2. `keys == _defaultKeys && key == "enabled"` → `toggler = 0` (단순 on/off 모드)
3. 그 외 → keys 배열 선형 탐색. 없으면 `toggler = -1`

`SmartMovingOptions.initializeForGameIfNeccessary()`에서 저장된 defaultKey로 호출.

---

### `getCurrentKey()`

```java
public String getCurrentKey()
{
    if(toggler == -1)
        return Disabled;     // "disabled"
    return keys[toggler];    // null 또는 실제 key 문자열
}
```

`toggler = 0`, `keys = _defaultKeys`일 때: `keys[0] == null` → `null` 반환.  
(`getKey(0)`과 달리 null을 "enabled"로 변환하지 않음.)

---

### `hasKey(String key)`

```java
public boolean hasKey(String key)
{
    if(Enabled.equals(key))               // "enabled" 확인
        return keys[0] == null;           // 단순 on/off 모드일 때만 true

    if(Disabled.equals(key))              // "disabled" 확인
        return true;                      // 항상 true

    for(int i = 0; i < keys.length; i++)
        if(key == null && keys[i] == null || key != null && key.equals(keys[i]))
            return true;
    return false;
}
```

"enabled" key: `_defaultKeys`({null}) 사용 중일 때만 존재. 실제 키 배열(`{"e","m","h"}`)에서는 false.  
"disabled" key: 항상 존재.

---

### `getKeyCount()`

```java
public int getKeyCount()
{
    return keys.length;
}
```

`SmartMovingOptions.writeClientConfigMessageToChat()`에서 `getKeyCount() == 1`이면 단순 on 메시지 출력.

---

### `update()`

```java
protected void update()
{
    List<Property<?>> properties = getProperties();
    Iterator<Property<?>> iterator = properties.iterator();

    String currentKey = getCurrentKey();
    while(iterator.hasNext())
        iterator.next().update(currentKey);   // 각 Property에 현재 key 전달
    enabled = toggler != -1;
}
```

모든 `toggle()`, `setKeys()`, `setCurrentKey()`, `load()` 완료 후 호출.

**`Property.update(currentKey)`**: 각 Property가 currentKey에 해당하는 값으로 자신의 `.value`를 갱신.  
`currentKey == null` (단순 on/off 모드, toggler=0) → Property는 기본값으로 갱신.  
`currentKey == "e"` → easy 설정값으로 갱신.

`enabled = toggler != -1` — 유일하게 `enabled` 필드를 갱신하는 지점.

---

## 부모 클래스(`Properties`)에서 제공하는 메서드

`SmartMovingConfig.writeToProperties()`에서 `write(properties)` 호출:

```java
protected void writeToProperties(Properties properties, List<Property<?>> except)
{
    write(properties);   // ← Properties 클래스에서 제공
    ...
}
```

`write(Properties)` — SmartMovingProperties에는 없고 부모 `net.smart.properties.Properties`에서 제공. 리서치 대상(`net.smart.properties.Properties.java`)에서 확인 필요.

마찬가지로 `getProperties()` — 현재 인스턴스에 선언된 모든 `Property<?>` 필드를 List로 반환하는 메서드도 부모 `Properties` 제공.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `net.smart.properties.Properties` | 상속 — `getProperties()`, `write()` 제공 |
| `net.smart.properties.Property<T>` | `reset()`, `load()`, `update()`, `print()` 호출 |

서브클래스: `SmartMovingConfig` (extends this, abstract가 아님).

---

## 주요 관찰 사항

1. **`toggler = -2` 센티넬**: 최초 로드 여부 구분. `-2`이면 reset 건너뜀 (처음 로드). `0~` 또는 `-1`이면 재로드 — 모든 Property reset 후 재로드.

2. **다중 패스 로딩**: `while (propertiesToLoad.size() > 0)` 반복. `Property.load()` 반환값이 false이면 해당 Property를 다음 패스로 넘김. Property 간 의존성(computed Property 등)이 있을 때 참조 대상 먼저 로드되도록 보장. 무한 루프 방지 조건은 `Property.load()` 구현에 위임 (미확인).

3. **`null` key의 의미**: `keys[i] == null`은 "named key 없는 단순 enabled 상태"를 의미. `getKey(index)`만 "enabled" 문자열로 변환. `getCurrentKey()`는 null 그대로 반환 — 이를 받는 `Property.update(null)`이 기본값 설정을 담당.

4. **`getNextKey` vs `toggle` 방향**: `getNextKey`는 "disabled" 다음 첫 번째 key → (keys 순서대로) → "disabled" 순서. `toggle`은 현재 toggler에서 +1 → keys 끝 도달 시 -1. 둘 다 같은 방향.

5. **`save` 섹션 구분**: `print()` 반환 true이고 마지막이 아닐 때만 빈 줄 추가. `print()` 반환 false (출력 없는 Property)인 경우 빈 줄 안 추가.

6. **`enabled` 갱신 위치**: `update()` 한 곳에서만 변경. `toggle()`, `setKeys()`, `setCurrentKey()`, `load()` 모두 `update()`를 마지막에 호출하므로 항상 일관성 유지.

7. **1.21.1 이식 관련**:
   - SmartMoving의 config key 토글 시스템은 vanilla에 없는 독자 시스템 — 전체 이식 필요
   - `getProperties()`로 리플렉션으로 필드를 수집하는 방식은 `net.smart.properties.Properties` 구현에 의존 (이후 리서치 필요)
   - `Property.update(String key)` — key별 값 전환 로직. Fabric 설정 시스템(AutoConfig 등)과 별개로 유지해야 함
   - `FileOutputStream` / `PrintWriter` 기반 파일 I/O → 1.21.1에서도 동일하게 사용 가능
