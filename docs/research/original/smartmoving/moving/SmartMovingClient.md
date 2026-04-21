# SmartMovingClient.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMovingClient.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: `SmartMovingClient extends SmartMovingContext implements ISmartMovingClient`

---

## 전체 소스

```java
package net.smart.moving;

import java.util.*;

public class SmartMovingClient extends SmartMovingContext implements ISmartMovingClient
{
	private final Map<String, Float> maximumExhaustionValues = new HashMap<String, Float>();
	private boolean nativeUserInterfaceDrawing = true;

	@Override
	public float getMaximumExhaustion()
	{
		float maxExhaustion = Config.getMaxExhaustion();
		if(maximumExhaustionValues.size() > 0)
		{
			Iterator<Float> iterator = maximumExhaustionValues.values().iterator();
			while(iterator.hasNext())
				maxExhaustion = Math.max(iterator.next(), maxExhaustion);
		}
		return maxExhaustion;
	}

	@Override
	public float getMaximumUpJumpCharge()
	{
		return Config._jumpChargeMaximum.value;
	}

	@Override
	public float getMaximumHeadJumpCharge()
	{
		return Config._headJumpChargeMaximum.value;
	}

	@Override
	public void setMaximumExhaustionValue(String key, float value)
	{
		maximumExhaustionValues.put(key, value);
	}

	@Override
	public float getMaximumExhaustionValue(String key)
	{
		return maximumExhaustionValues.get(key);
	}

	@Override
	public boolean removeMaximumExhaustionValue(String key)
	{
		return maximumExhaustionValues.remove(key) != null;
	}

	@Override
	public void setNativeUserInterfaceDrawing(boolean value)
	{
		nativeUserInterfaceDrawing = value;
	}

	@Override
	public boolean getNativeUserInterfaceDrawing()
	{
		return nativeUserInterfaceDrawing;
	}
}
```

---

## 역할

클라이언트 전역 설정 값을 관리하는 싱글톤 컨텍스트.  
`ISmartMovingClient` 인터페이스를 구현한다.

두 가지 역할:
1. **최대 소진(exhaustion) 값 관리**: `Config.getMaxExhaustion()` 기본값에 외부에서 등록한 값들을 합산해 최대값 반환
2. **네이티브 UI 드로잉 플래그 관리**: `nativeUserInterfaceDrawing`

---

## 상속 구조

```
SmartMovingContext
  └─ SmartMovingClient implements ISmartMovingClient
```

`SmartMovingBase`/`SmartMoving`/`SmartMovingSelf`와는 **별개의 상속 라인**.  
`SmartMovingContext`를 직접 상속하며, 이동 처리(SmartMovingSelf)와 분리된 클라이언트 전역 관리 역할.

---

## import

```java
import java.util.*;  // Map, HashMap, Iterator
```

---

## 필드

```java
private final Map<String, Float> maximumExhaustionValues = new HashMap<String, Float>();
```

외부에서 등록한 소진 최대값 맵. 키: String 식별자, 값: float 최대 소진 값.  
`final` — 생성 후 교체 불가, 내용물은 변경 가능.  
초기값: 빈 HashMap.

```java
private boolean nativeUserInterfaceDrawing = true;
```

네이티브(Vanilla) UI 드로잉 사용 여부.  
초기값: `true` (기본적으로 vanilla UI 드로잉 활성).

---

## 메서드 (전부 `@Override` — ISmartMovingClient 구현)

### `getMaximumExhaustion()` → float

```java
@Override
public float getMaximumExhaustion()
{
    float maxExhaustion = Config.getMaxExhaustion();
    if(maximumExhaustionValues.size() > 0)
    {
        Iterator<Float> iterator = maximumExhaustionValues.values().iterator();
        while(iterator.hasNext())
            maxExhaustion = Math.max(iterator.next(), maxExhaustion);
    }
    return maxExhaustion;
}
```

반환값: `Config.getMaxExhaustion()`과 `maximumExhaustionValues` 맵의 모든 값 중 최대값.

**동작:**
1. `Config.getMaxExhaustion()`을 기준값으로 시작
2. `maximumExhaustionValues`가 비어있지 않으면 Iterator로 모든 값을 순회
3. 각 값과 `Math.max()` 비교 → 계속 갱신
4. 최종 최대값 반환

**목적**: 다른 모드가 `setMaximumExhaustionValue(key, value)`로 소진 최대값을 등록하면, 그 중 가장 큰 값이 실제 한계로 사용됨 (모드 간 호환성).

---

### `getMaximumUpJumpCharge()` → float

```java
@Override
public float getMaximumUpJumpCharge()
{
    return Config._jumpChargeMaximum.value;
}
```

위쪽 점프 차지 최대값을 `Config._jumpChargeMaximum.value`에서 직접 반환.

---

### `getMaximumHeadJumpCharge()` → float

```java
@Override
public float getMaximumHeadJumpCharge()
{
    return Config._headJumpChargeMaximum.value;
}
```

헤드 점프 차지 최대값을 `Config._headJumpChargeMaximum.value`에서 직접 반환.

---

### `setMaximumExhaustionValue(String key, float value)`

```java
@Override
public void setMaximumExhaustionValue(String key, float value)
{
    maximumExhaustionValues.put(key, value);
}
```

`maximumExhaustionValues` 맵에 key → value 등록.  
동일 key로 재호출하면 덮어씀.

---

### `getMaximumExhaustionValue(String key)` → float

```java
@Override
public float getMaximumExhaustionValue(String key)
{
    return maximumExhaustionValues.get(key);
}
```

key에 해당하는 값 반환.  
key가 없으면 `NullPointerException` (Map.get → null → float 언박싱).

---

### `removeMaximumExhaustionValue(String key)` → boolean

```java
@Override
public boolean removeMaximumExhaustionValue(String key)
{
    return maximumExhaustionValues.remove(key) != null;
}
```

key 제거. key가 실제로 존재했으면 `true`, 없었으면 `false`.

---

### `setNativeUserInterfaceDrawing(boolean value)`

```java
@Override
public void setNativeUserInterfaceDrawing(boolean value)
{
    nativeUserInterfaceDrawing = value;
}
```

`nativeUserInterfaceDrawing` 플래그 설정.

---

### `getNativeUserInterfaceDrawing()` → boolean

```java
@Override
public boolean getNativeUserInterfaceDrawing()
{
    return nativeUserInterfaceDrawing;
}
```

`nativeUserInterfaceDrawing` 값 반환. 초기값 `true`.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingContext` | 상속 |
| `ISmartMovingClient` | 구현 인터페이스 |
| `Config` | `getMaxExhaustion()`, `_jumpChargeMaximum.value`, `_headJumpChargeMaximum.value` |

---

## 주요 관찰 사항

1. **모드 간 소진 최대값 등록 API**: `setMaximumExhaustionValue(key, value)` / `removeMaximumExhaustionValue(key)` — 외부 모드가 고유 key로 값을 등록/해제하는 방식. `getMaximumExhaustion()`은 모든 등록값과 Config 기본값의 최대를 반환.

2. **`getMaximumExhaustionValue(key)` NullPointerException 위험**: 등록되지 않은 key로 호출하면 `Map.get()` → `null` → float 언박싱 → NPE. 호출 측에서 key 존재 여부를 보장해야 함.

3. **SmartMovingBase/SmartMovingSelf와 별도 계층**: 이동 처리 계층(`SmartMovingContext → SmartMovingBase → SmartMoving → SmartMovingSelf`)과 달리, 이 클래스는 `SmartMovingContext`에서 직접 분기. 이동 상태 필드 없음.

4. **생성자 없음**: 기본 생성자만 존재. `SmartMovingContext`의 생성자 요구사항 확인 필요 (SmartMovingContext 리서치 시).

5. **1.21.1 이식**: 구조 자체는 단순하므로 그대로 이식 가능. `Config` 참조는 1.21.1 Config 시스템으로 교체. `ISmartMovingClient`도 그대로 유지 가능.
