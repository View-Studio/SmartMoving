# SmartMovingServerConfig.java (net.smart.moving.config) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/config/SmartMovingServerConfig.java  
패키지: `net.smart.moving.config`  
종류: `class`  
상속: `SmartMovingClientConfig` (extends)

상속 계층:  
`SmartMovingServerConfig → SmartMovingClientConfig → SmartMovingConfig → SmartMovingProperties → Properties`

---

## 전체 소스

```java
package net.smart.moving.config;

import java.util.Iterator;

import net.smart.properties.*;

public class SmartMovingServerConfig extends SmartMovingClientConfig
{
    private Properties properties    = new Properties();
    private Properties topProperties = new Properties();

    public void loadFromProperties(String[] propertyArray, boolean top)
    {
        for(int i = 0; i < propertyArray.length - 1; i += 2)
        {
            String key   = propertyArray[i];
            String value = propertyArray[i + 1];
            properties.put(key, value);
            if(top)
                topProperties.put(key, value);
        }

        load(top);
    }

    public void load(boolean top)
    {
        if(!top && !topProperties.isEmpty())
        {
            Iterator<?> iterator = topProperties.keySet().iterator();
            while(iterator.hasNext())
            {
                Object topKey = iterator.next();
                properties.put(topKey, topProperties.get(topKey));
            }
        }
        super.loadFromProperties(properties);
    }

    public void reset()
    {
        properties.clear();
        topProperties.clear();
    }
}
```

---

## 역할

서버에서 클라이언트로 전송된 설정 데이터를 `SmartMovingClientConfig` 필드 구조에 로드하는 클래스. 두 개의 우선순위 레이어(`top`/일반)를 관리하며, `top` 설정이 일반 설정보다 항상 우선 적용된다.

`SmartMovingContext.Config`와 별개로, 서버 수신 설정 파싱 전용 인스턴스로 사용된다.

---

## import

```java
import java.util.Iterator;
import net.smart.properties.*;   // Properties (net.smart.properties.Properties)
```

---

## 인스턴스 필드

```java
private Properties properties    = new Properties();
private Properties topProperties = new Properties();
```

둘 다 `net.smart.properties.Properties` — key-value 문자열 맵.

| 필드 | 의미 |
|------|------|
| `properties` | 수신된 모든 설정(top + 일반) 누적 저장소 |
| `topProperties` | 서버 전역(global) 설정만 별도 저장 — top 우선순위 적용에 사용 |

---

## 메서드

### `loadFromProperties(String[] propertyArray, boolean top)`

```java
public void loadFromProperties(String[] propertyArray, boolean top)
{
    for(int i = 0; i < propertyArray.length - 1; i += 2)
    {
        String key   = propertyArray[i];
        String value = propertyArray[i + 1];
        properties.put(key, value);
        if(top)
            topProperties.put(key, value);
    }

    load(top);
}
```

**입력**: `propertyArray` — `[key1, val1, key2, val2, ...]` 형식의 flat 문자열 배열.

**파싱**: `i += 2`로 2칸씩 이동하며 key-value 쌍 추출. 종료 조건 `i < propertyArray.length - 1` — 홀수 길이 배열에서 마지막 단독 원소 무시.

**저장**:
- 모든 쌍을 `properties`에 저장
- `top == true`이면 `topProperties`에도 동시에 저장

**완료 후**: `load(top)` 호출.

`propertyArray`는 `SmartMovingPacketStream`의 패킷 직렬화 결과물 — 서버→클라이언트 config 패킷에서 추출된 key-value 배열.

---

### `load(boolean top)`

```java
public void load(boolean top)
{
    if(!top && !topProperties.isEmpty())
    {
        Iterator<?> iterator = topProperties.keySet().iterator();
        while(iterator.hasNext())
        {
            Object topKey = iterator.next();
            properties.put(topKey, topProperties.get(topKey));  // top 값으로 덮어씌우기
        }
    }
    super.loadFromProperties(properties);
}
```

**top=true인 경우**:
- `if(!top ...)` 조건 불만족 → topProperties → properties 덮어씌우기 없음
- 바로 `super.loadFromProperties(properties)` 호출

**top=false이고 topProperties가 비어있지 않은 경우**:
- topProperties의 모든 항목을 `properties`에 덮어씌움
- 그 후 `super.loadFromProperties(properties)` 호출

**결과**: 일반 설정이 먼저 `properties`에 들어가더라도, `top=false`로 로드할 때 topProperties 값이 properties를 덮어씌워 최종적으로 top 설정이 우선 적용됨.

`super.loadFromProperties(properties)` → `SmartMovingConfig.loadFromProperties(Properties)` → `load(properties)` (SmartMovingProperties).

---

### `reset()`

```java
public void reset()
{
    properties.clear();
    topProperties.clear();
}
```

두 Properties 저장소 모두 비움. 서버 연결 해제 또는 새 설정 수신 준비 시 호출.

---

## top / 일반 우선순위 시스템

### 호출 시나리오

| 상황 | `top` 값 | 동작 |
|------|----------|------|
| 서버 전역 config(`_globalConfig=true`) 수신 | `true` | properties + topProperties 양쪽에 저장 |
| 클라이언트별 config 수신 | `false` | properties에만 저장 → 로드 시 topProperties로 덮어씌움 |

### 우선순위 작동 예시

1. 서버가 전역 설정 `{speed=1.5}`를 `top=true`로 전송:  
   → `properties = {speed: 1.5}`, `topProperties = {speed: 1.5}`

2. 서버가 이후 클라이언트별 설정 `{speed=1.0}`을 `top=false`로 전송:  
   → `properties = {speed: 1.0}` (일반 저장)  
   → `load(false)`: topProperties를 properties에 덮어씌움 → `properties = {speed: 1.5}`  
   → `super.loadFromProperties({speed: 1.5})` — top 값이 최종 적용

즉, **전역 설정(top)은 클라이언트별 설정보다 항상 우선**한다.

---

## 상속 이유 분석

`SmartMovingServerConfig`가 `SmartMovingClientConfig`를 상속하는 이유: 서버가 클라이언트에 전송하는 설정 데이터는 `SmartMovingClientConfig`와 동일한 Property 필드 집합을 가져야 한다. `super.loadFromProperties(properties)` 호출 시 부모의 모든 `Property<T>` 필드가 수신된 값으로 채워진다.

`SmartMovingServerComm`(서버 측) 또는 `SmartMovingClient`(클라이언트 측)에서 이 인스턴스를 통해 서버 전송 설정을 파싱하고, 그 값을 실제 `SmartMovingContext.Config`에 반영한다.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingClientConfig` | 상속 — Property 필드 구조 제공 |
| `SmartMovingConfig.loadFromProperties(Properties)` | super 호출로 실제 로드 수행 |
| `net.smart.properties.Properties` | key-value 저장소 |

호출처: `SmartMovingPacketStream` 또는 `SmartMovingClient`에서 config 패킷 수신 시 (`SmartMovingServerComm` 리서치에서 확인 필요).

---

## 주요 관찰 사항

1. **`SmartMovingClientConfig` 상속**: 서버가 전송하는 설정이 클라이언트 설정과 동일한 구조. `SmartMovingServerConfig`는 서버에서 수신한 데이터를 클라이언트 config 필드 구조로 파싱하는 어댑터 역할.

2. **flat 배열 파싱**: `propertyArray[i], propertyArray[i+1]` 쌍 방식 — `SmartMovingPacketStream`의 직렬화 형식과 1:1 대응. 홀수 길이 배열의 마지막 원소는 묵시적으로 무시(`i < length - 1` 조건).

3. **top 우선순위 적용 시점**: `load(top=false)` 호출 시 topProperties → properties 덮어씌우기가 발생. `loadFromProperties(array, top=false)` 호출 순서 이전에 `topProperties`가 이미 채워져 있어야 정상 동작.

4. **`reset()` 필요성**: `properties`와 `topProperties`가 누적 저장소이므로, 새 서버 접속 또는 설정 초기화 시 반드시 `reset()`을 먼저 호출해야 이전 설정이 섞이지 않음.

5. **1.21.1 이식 관련**:
   - 구조 자체는 단순 — `Properties`(맵)와 로드 로직만 유지하면 됨
   - `net.smart.properties.Properties` → `java.util.Properties` 또는 동등한 맵으로 대체 가능
   - `super.loadFromProperties(Properties)` 체인은 SmartMovingConfig 계층 전체 이식 후 그대로 유지
