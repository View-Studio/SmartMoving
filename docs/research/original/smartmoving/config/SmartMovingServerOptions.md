# SmartMovingServerOptions.java (net.smart.moving.config) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/config/SmartMovingServerOptions.java  
패키지: `net.smart.moving.config`  
종류: `class` (상속 없음 — `Object` 직접 상속)  
실행 위치: 서버

---

## 전체 소스

```java
package net.smart.moving.config;

import java.io.*;
import java.util.*;

import cpw.mods.fml.common.*;

import net.smart.moving.*;
import net.smart.properties.*;
import net.smart.properties.Properties;

public class SmartMovingServerOptions
{
    public final SmartMovingConfig config;
    public final File optionsPath;
    private final Property<Map<String,String>> _userConfigKeys;

    public SmartMovingServerOptions(SmartMovingConfig config, File optionsPath, int gameType)
    {
        this.config = config;
        this.optionsPath = optionsPath;

        config.loadFromOptionsFile(optionsPath);
        config.saveToOptionsFile(optionsPath);

        Property<String> configKey   = null;
        Property<String[]> configKeys = null;
        switch(gameType)
        {
            default:
            case SmartMovingConfig.Survival:
                configKey       = config._survivalDefaultConfigKey;
                configKeys      = config._survivalConfigKeys;
                _userConfigKeys = config._survivalDefaultConfigUserKeys;
                break;
            case SmartMovingConfig.Creative:
                configKey       = config._creativeDefaultConfigKey;
                configKeys      = config._creativeConfigKeys;
                _userConfigKeys = config._creativeDefaultConfigUserKeys;
                break;
            case SmartMovingConfig.Adventure:
                configKey       = config._adventureDefaultConfigKey;
                configKeys      = config._adventureConfigKeys;
                _userConfigKeys = config._adventureDefaultConfigUserKeys;
                break;
        }

        config.setKeys(configKeys.value);
        config.setCurrentKey(configKey != null && !configKey.value.isEmpty() ? configKey.value : null);

        logConfigState(config, null, false);
    }

    public void toggle(IEntityPlayerMP player)
    {
        config.toggle();
        config.saveToOptionsFile(optionsPath);
        logConfigState(config, player.getUsername(), true);
    }

    public void changeSpeed(int difference, IEntityPlayerMP player)
    {
        config.changeSpeed(difference);
        config.saveToOptionsFile(optionsPath);
        logSpeedState(config, player.getUsername());
    }

    private static void logConfigState(SmartMovingConfig config, String username, boolean reconfig)
    {
        String message = "Smart Moving ";
        if(config._globalConfig.value)
        {
            if(!reconfig)
                FMLLog.info(message + "overrides client configurations");

            String postfix = getPostfix(username);

            if(config.enabled)
            {
                String currentKey = config.getCurrentKey();

                message += reconfig ? "changed to " : "uses ";
                if(currentKey == null)
                    FMLLog.info(message + "default server configuration" + postfix);
                else
                {
                    String configName = config._configKeyName.value;

                    message += "server configuration ";
                    if(configName.isEmpty())
                        FMLLog.info(message + "with key \"" + currentKey + "\"" + postfix);
                    else
                        FMLLog.info(message + "\"" + configName + "\"" + postfix);
                }
            }
            else
                FMLLog.info(message + "disabled" + postfix);
        }
        else
            FMLLog.info(message + "allows client configurations");
    }

    private static void logSpeedState(SmartMovingConfig config, String username)
    {
        FMLLog.info("Smart Moving speed set to " + config.getSpeedPercent() + "%" + getPostfix(username));
    }

    private static String getPostfix(String username)
    {
        if(username == null)
            return "";
        return " by user '" + username + "'";
    }

    public String[] writeToProperties()
    {
        return writeToProperties(null, null);
    }

    public String[] writeToProperties(IEntityPlayerMP mp, String key)
    {
        if(key == null ? !config.enabled : key == SmartMovingProperties.Disabled)
            return new String[] { config._globalConfig.getCurrentKey(), config._globalConfig.getValueString() };

        Properties properties = new Properties();
        config.write(properties, key);

        String[] result = new String[properties.size() * 2];
        Iterator<Map.Entry<Object, Object>> keys = properties.entrySet().iterator();

        String speedUserExponentKey = mp != null ? config._speedUserExponent.getCurrentKey() : null;
        int i = 0;
        while(keys.hasNext())
        {
            Map.Entry<Object, Object> entry = keys.next();
            String propertyKey = result[i++] = entry.getKey().toString();
            if(mp != null && propertyKey.equals(speedUserExponentKey))
            {
                Integer userExponent = config._speedUsersExponents.value.get(mp.getUsername());
                if(userExponent != null)
                    entry.setValue(config._speedUserExponent.getValueString(userExponent));
            }
            result[i++] = entry.getValue().toString();
        }
        return result;
    }

    public void changeSingleSpeed(IEntityPlayerMP player, int difference)
    {
        Integer exponent = getPlayerSpeedExponent(player);
        if(exponent == null)
            exponent = config._speedUserExponent.value;

        exponent += difference;
        setPlayerSpeedExponent(player, exponent);
    }

    public Integer getPlayerSpeedExponent(IEntityPlayerMP player)
    {
        return config._speedUsersExponents.value.get(player.getUsername());
    }

    public synchronized void setPlayerSpeedExponent(IEntityPlayerMP player, Integer exponent)
    {
        config._speedUsersExponents.value.put(player.getUsername(), exponent);
        config.saveToOptionsFile(optionsPath);
    }

    public String[] writeToProperties(IEntityPlayerMP player, boolean toggle)
    {
        String key = getPlayerConfigurationKey(player);
        if(key == null || !config.hasKey(key))
        {
            key = config.getCurrentKey();
            setPlayerConfigurationKey(player, key);
        }

        if(toggle)
        {
            key = config.getNextKey(key);
            setPlayerConfigurationKey(player, key);
        }

        return writeToProperties(player, key);
    }

    public String getPlayerConfigurationKey(IEntityPlayerMP player)
    {
        return _userConfigKeys.value.get(player.getUsername());
    }

    public synchronized void setPlayerConfigurationKey(IEntityPlayerMP player, String key)
    {
        _userConfigKeys.value.put(player.getUsername(), key);
        config.saveToOptionsFile(optionsPath);
    }
}
```

---

## 역할

서버 측 SmartMoving 설정 관리 클래스. `SmartMovingConfig`를 내부에 보유하며(상속 아님), 서버 전역 config 토글/속도 조정, 플레이어별 config key/속도 관리, config를 flat 배열로 직렬화하여 클라이언트 전송 준비를 담당한다.

`SmartMovingServer`가 이 클래스의 인스턴스를 생성하여 사용하는 것으로 추정 → SmartMovingServer 리서치에서 확인.

---

## import

```java
import java.io.*;            // File
import java.util.*;          // Map, Iterator, Map.Entry
import cpw.mods.fml.common.*; // FMLLog (Forge 로그)
import net.smart.moving.*;   // IEntityPlayerMP, SmartMovingProperties
import net.smart.properties.*;
import net.smart.properties.Properties;
```

---

## 필드

```java
public final SmartMovingConfig config;
public final File optionsPath;
private final Property<Map<String,String>> _userConfigKeys;
```

| 필드 | 타입 | 의미 |
|------|------|------|
| `config` | `SmartMovingConfig` | 서버 전역 설정 객체 (SmartMovingClientConfig 또는 하위 인스턴스) |
| `optionsPath` | `File` | 서버 설정 파일 저장 경로 |
| `_userConfigKeys` | `Property<Map<String,String>>` | 플레이어 이름 → 현재 config key 맵 |

`_userConfigKeys`는 생성자에서 gameType에 따라 `config._survivalDefaultConfigUserKeys` / `_creativeDefaultConfigUserKeys` / `_adventureDefaultConfigUserKeys` 중 하나를 참조.

---

## 생성자

```java
public SmartMovingServerOptions(SmartMovingConfig config, File optionsPath, int gameType)
{
    this.config     = config;
    this.optionsPath = optionsPath;

    config.loadFromOptionsFile(optionsPath);
    config.saveToOptionsFile(optionsPath);  // 버전 마이그레이션 결과 즉시 저장

    Property<String> configKey   = null;
    Property<String[]> configKeys = null;
    switch(gameType)
    {
        default:
        case SmartMovingConfig.Survival:    // 0
            configKey       = config._survivalDefaultConfigKey;
            configKeys      = config._survivalConfigKeys;
            _userConfigKeys = config._survivalDefaultConfigUserKeys;
            break;
        case SmartMovingConfig.Creative:    // 1
            configKey       = config._creativeDefaultConfigKey;
            configKeys      = config._creativeConfigKeys;
            _userConfigKeys = config._creativeDefaultConfigUserKeys;
            break;
        case SmartMovingConfig.Adventure:   // 2
            configKey       = config._adventureDefaultConfigKey;
            configKeys      = config._adventureConfigKeys;
            _userConfigKeys = config._adventureDefaultConfigUserKeys;
            break;
    }

    config.setKeys(configKeys.value);
    config.setCurrentKey(configKey != null && !configKey.value.isEmpty() ? configKey.value : null);
    // configKey가 null이거나 빈 문자열이면 null 전달 → setCurrentKey(null) → toggler = -1 (disabled)

    logConfigState(config, null, false);  // 서버 콘솔에 초기 상태 출력
}
```

**초기화 순서**:
1. 설정 파일 로드 + 저장 (버전 마이그레이션)
2. gameType별 configKey, configKeys, _userConfigKeys 결정
3. `config.setKeys(configKeys.value)` — 유효 config key 배열 설정, toggler=0으로 초기화
4. `config.setCurrentKey(...)` — 기본 key로 toggler 위치 설정
5. `logConfigState(config, null, false)` — 콘솔 로그

`gameType`의 기본 케이스(`default`)는 `Survival(0)`.

---

## 메서드

### `toggle(IEntityPlayerMP player)`

```java
public void toggle(IEntityPlayerMP player)
{
    config.toggle();                         // 전역 config key 순환
    config.saveToOptionsFile(optionsPath);   // 즉시 저장
    logConfigState(config, player.getUsername(), true);
}
```

서버 전역 config를 순환. `SmartMovingServerComm`에서 권한 있는 플레이어의 toggle 요청 처리 시 호출.

---

### `changeSpeed(int difference, IEntityPlayerMP player)`

```java
public void changeSpeed(int difference, IEntityPlayerMP player)
{
    config.changeSpeed(difference);          // _speedUserExponent.value += difference
    config.saveToOptionsFile(optionsPath);
    logSpeedState(config, player.getUsername());
}
```

서버 전역 속도 조정. 저장 + 콘솔 로그.

---

### `logConfigState(SmartMovingConfig config, String username, boolean reconfig)` (private static)

```java
private static void logConfigState(SmartMovingConfig config, String username, boolean reconfig)
{
    String message = "Smart Moving ";
    if(config._globalConfig.value)
    {
        if(!reconfig)
            FMLLog.info(message + "overrides client configurations");

        String postfix = getPostfix(username);

        if(config.enabled)
        {
            String currentKey = config.getCurrentKey();

            message += reconfig ? "changed to " : "uses ";
            if(currentKey == null)
                FMLLog.info(message + "default server configuration" + postfix);
            else
            {
                String configName = config._configKeyName.value;

                message += "server configuration ";
                if(configName.isEmpty())
                    FMLLog.info(message + "with key \"" + currentKey + "\"" + postfix);
                else
                    FMLLog.info(message + "\"" + configName + "\"" + postfix);
            }
        }
        else
            FMLLog.info(message + "disabled" + postfix);
    }
    else
        FMLLog.info(message + "allows client configurations");
}
```

**출력 분기표**:

| `_globalConfig.value` | `enabled` | `currentKey` | `configName` | `reconfig` | 출력 |
|---|---|---|---|---|---|
| false | - | - | - | - | `"Smart Moving allows client configurations"` |
| true | - | - | - | false | `"Smart Moving overrides client configurations"` (+ 아래 중 하나) |
| true | true | null | - | false | `"Smart Moving uses default server configuration"` |
| true | true | null | - | true | `"Smart Moving changed to default server configuration"` |
| true | true | "m" | "" | false | `"Smart Moving uses server configuration with key \"m\""` |
| true | true | "m" | "Medium" | false | `"Smart Moving uses server configuration \"Medium\""` |
| true | true | "m" | "" | true | `"Smart Moving changed to server configuration with key \"m\""` |
| true | true | "m" | "Medium" | true | `"Smart Moving changed to server configuration \"Medium\""` |
| true | false | - | - | - | `"Smart Moving disabled"` |

`reconfig=false` + `_globalConfig.value=true`: "overrides" 메시지 먼저 출력 후 상태 메시지.  
`reconfig=true`: "overrides" 메시지 없이 상태 메시지만.

---

### `logSpeedState(SmartMovingConfig config, String username)` (private static)

```java
private static void logSpeedState(SmartMovingConfig config, String username)
{
    FMLLog.info("Smart Moving speed set to " + config.getSpeedPercent() + "%" + getPostfix(username));
}
```

예: `"Smart Moving speed set to 150% by user 'Steve'"`

---

### `getPostfix(String username)` (private static)

```java
private static String getPostfix(String username)
{
    if(username == null)
        return "";
    return " by user '" + username + "'";
}
```

`username == null` → 서버 초기화 시 (`toggle` 호출 없음) → 접미사 없음.

---

### `writeToProperties()` (인수 없음)

```java
public String[] writeToProperties()
{
    return writeToProperties(null, null);
}
```

전역 config를 플레이어 개인화 없이 직렬화. → `writeToProperties(null, null)` 위임.

---

### `writeToProperties(IEntityPlayerMP mp, String key)`

```java
public String[] writeToProperties(IEntityPlayerMP mp, String key)
{
    if(key == null ? !config.enabled : key == SmartMovingProperties.Disabled)
        return new String[] { config._globalConfig.getCurrentKey(), config._globalConfig.getValueString() };

    Properties properties = new Properties();
    config.write(properties, key);

    String[] result = new String[properties.size() * 2];
    Iterator<Map.Entry<Object, Object>> keys = properties.entrySet().iterator();

    String speedUserExponentKey = mp != null ? config._speedUserExponent.getCurrentKey() : null;
    int i = 0;
    while(keys.hasNext())
    {
        Map.Entry<Object, Object> entry = keys.next();
        String propertyKey = result[i++] = entry.getKey().toString();
        if(mp != null && propertyKey.equals(speedUserExponentKey))
        {
            Integer userExponent = config._speedUsersExponents.value.get(mp.getUsername());
            if(userExponent != null)
                entry.setValue(config._speedUserExponent.getValueString(userExponent));
        }
        result[i++] = entry.getValue().toString();
    }
    return result;
}
```

**disabled 단축 경로**:

```java
if(key == null ? !config.enabled : key == SmartMovingProperties.Disabled)
    return new String[] { config._globalConfig.getCurrentKey(), config._globalConfig.getValueString() };
```

- `key == null && !config.enabled`: config 전체 비활성 → globalConfig 설정만 2원소로 반환
- `key == SmartMovingProperties.Disabled`: 키가 정확히 `"disabled"` 상수 참조(`==` 비교, `equals` 아님)

반환 `{globalConfig.getCurrentKey(), globalConfig.getValueString()}` — 클라이언트가 이것을 받으면 disabled 상태임을 인식.

**정상 경로**:
1. `config.write(properties, key)` — key에 해당하는 모든 설정 값을 `Properties` 맵에 기록
2. `String[] result = new String[properties.size() * 2]` — flat 배열 할당
3. 이터레이터로 순회하며 `result[i++] = key`, `result[i++] = value` 채움
4. **플레이어별 속도 치환**: `mp != null`이고 현재 entry의 키가 `_speedUserExponent.getCurrentKey()`와 같으면:
   - `config._speedUsersExponents.value.get(mp.getUsername())` — 해당 플레이어의 개인 속도 지수
   - null이 아니면 `entry.setValue(config._speedUserExponent.getValueString(userExponent))`로 전역 속도 값 대신 개인 속도 값 삽입
5. `result[i++] = entry.getValue().toString()` — (치환된 경우 개인 값, 아니면 전역 값)

반환 형식: `[key1, val1, key2, val2, ...]` — `SmartMovingServerConfig.loadFromProperties(String[], boolean)`이 파싱하는 형식과 1:1 대응.

---

### `changeSingleSpeed(IEntityPlayerMP player, int difference)`

```java
public void changeSingleSpeed(IEntityPlayerMP player, int difference)
{
    Integer exponent = getPlayerSpeedExponent(player);
    if(exponent == null)
        exponent = config._speedUserExponent.value;  // 전역 지수를 기준으로 시작

    exponent += difference;
    setPlayerSpeedExponent(player, exponent);
}
```

플레이어 개인 속도 지수 조정. 개인 값이 없으면 전역 값에서 시작.

---

### `getPlayerSpeedExponent(IEntityPlayerMP player)`

```java
public Integer getPlayerSpeedExponent(IEntityPlayerMP player)
{
    return config._speedUsersExponents.value.get(player.getUsername());
}
```

`config._speedUsersExponents.value` — `Map<String, Integer>`. 없으면 null 반환.

---

### `setPlayerSpeedExponent(IEntityPlayerMP player, Integer exponent)` (synchronized)

```java
public synchronized void setPlayerSpeedExponent(IEntityPlayerMP player, Integer exponent)
{
    config._speedUsersExponents.value.put(player.getUsername(), exponent);
    config.saveToOptionsFile(optionsPath);
}
```

멀티스레드 서버 환경에서 플레이어 속도 맵 동시 접근 방지.

---

### `writeToProperties(IEntityPlayerMP player, boolean toggle)`

```java
public String[] writeToProperties(IEntityPlayerMP player, boolean toggle)
{
    String key = getPlayerConfigurationKey(player);
    if(key == null || !config.hasKey(key))
    {
        key = config.getCurrentKey();           // 전역 현재 key로 초기화
        setPlayerConfigurationKey(player, key); // 플레이어 key 저장
    }

    if(toggle)
    {
        key = config.getNextKey(key);           // 다음 key로 순환
        setPlayerConfigurationKey(player, key); // 새 key 저장
    }

    return writeToProperties(player, key);
}
```

플레이어 개인 config 토글 흐름:
1. 플레이어의 현재 key 조회
2. null이거나 유효하지 않으면 전역 현재 key로 초기화 및 저장
3. `toggle=true`이면 `getNextKey(key)`로 다음 key 설정 및 저장
4. 해당 key로 config 직렬화하여 반환

---

### `getPlayerConfigurationKey(IEntityPlayerMP player)`

```java
public String getPlayerConfigurationKey(IEntityPlayerMP player)
{
    return _userConfigKeys.value.get(player.getUsername());
}
```

`_userConfigKeys.value` — `Map<String, String>`. 없으면 null 반환.

---

### `setPlayerConfigurationKey(IEntityPlayerMP player, String key)` (synchronized)

```java
public synchronized void setPlayerConfigurationKey(IEntityPlayerMP player, String key)
{
    _userConfigKeys.value.put(player.getUsername(), key);
    config.saveToOptionsFile(optionsPath);
}
```

플레이어 config key 저장 + 파일 즉시 저장 (서버 재시작 시 복원 목적).

---

## 직렬화 흐름 요약

```
서버 → 클라이언트 전송 시:
  writeToProperties(player, toggle) → writeToProperties(player, key) → config.write(properties, key)
  → flat String[] [k1,v1,k2,v2,...] → SmartMovingPacketStream 전송

클라이언트 수신 시:
  SmartMovingServerConfig.loadFromProperties(String[], boolean top) → Properties 맵에 저장
  → super.loadFromProperties(properties) → SmartMovingClientConfig 필드에 값 반영
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingConfig` | 서버 설정 객체 (보유, 상속 아님) |
| `SmartMovingProperties.Disabled` | `"disabled"` 상수 (`==` 참조 비교) |
| `IEntityPlayerMP` | 플레이어 이름 조회 (`getUsername()`) |
| `FMLLog` | 서버 콘솔 로그 (Forge FML) |
| `net.smart.properties.Properties` | key-value 직렬화 맵 |

---

## 주요 관찰 사항

1. **상속이 아닌 조합(Composition)**: `SmartMovingServerConfig`(상속)와 달리 `SmartMovingConfig` 인스턴스를 필드로 보유. 서버 측 기능(플레이어별 관리, 로그 등)을 별도 클래스로 분리하는 설계.

2. **`key == SmartMovingProperties.Disabled` 참조 비교**: `equals()` 대신 `==` 사용. `SmartMovingProperties.Disabled`는 `"disabled"` String 상수. 이 비교는 `getNextKey()`, `setCurrentKey()` 등에서 반환하는 값이 반드시 이 상수 레퍼런스임을 전제. Java String 상수 풀 활용.

3. **플레이어별 속도 치환**: `writeToProperties(mp, key)` 내에서 `entry.setValue()`로 이터레이션 중 값을 직접 교체. 전역 속도 대신 개인 속도를 삽입 — 클라이언트는 자신의 개인 속도로 동작.

4. **`synchronized` 적용 범위**: `setPlayerSpeedExponent`, `setPlayerConfigurationKey`만 synchronized. 읽기 메서드(`getPlayerSpeedExponent`, `getPlayerConfigurationKey`)는 synchronized 없음 — 가시성 문제 가능성 있음.

5. **`config.write(properties, key)`**: `SmartMovingProperties` 또는 그 부모 `net.smart.properties.Properties`에서 제공. key에 해당하는 config 값을 Properties 맵에 씀. 이 메서드는 이후 `net.smart.properties` 리서치에서 확인 필요.

6. **1.21.1 이식 관련**:
   - `FMLLog` → `LOGGER` (Fabric은 SLF4J 기반 `LogManager.getLogger()`)
   - `IEntityPlayerMP` → Fabric 서버 플레이어 API
   - `config.write(properties, key)` → `net.smart.properties` 전체 이식 필요
   - `synchronized` 메서드 → Fabric 환경에서도 멀티스레드 서버이므로 유지
   - gameType 판별 → Fabric `ServerPlayerEntity.interactionManager.getGameMode()`
