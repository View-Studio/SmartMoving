# HandsClimbing.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/HandsClimbing.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: 없음 (`Object` 직접 상속)

---

## 전체 소스

```java
package net.smart.moving;

import java.io.*;

public class HandsClimbing
{
	public static final int MiddleGrab = 2;
	public static final int UpGrab = 1;
	public static final int NoGrab = 0;

	public static HandsClimbing None = new HandsClimbing(-3);
	public static HandsClimbing Sink = new HandsClimbing(-2);
	public static HandsClimbing TopHold = new HandsClimbing(-1);
	public static HandsClimbing BottomHold = new HandsClimbing(0);
	public static HandsClimbing Up = new HandsClimbing(1);
	public static HandsClimbing FastUp = new HandsClimbing(2);

	private int _value;

	private HandsClimbing(int value)
	{
		_value = value;
	}

	public boolean IsRelevant()
	{
		return _value > None._value;
	}

	public boolean IsUp()
	{
		return this == Up || this == FastUp;
	}

	public HandsClimbing ToUp()
	{
		if(this == BottomHold)
			return Up;
		return this;
	}

	public HandsClimbing ToDown()
	{
		if(this == TopHold)
			return Sink;
		return this;
	}

	public HandsClimbing max(HandsClimbing other, ClimbGap inout_thisClimbGap, ClimbGap otherClimbGap)
	{
		if(!otherClimbGap.SkipGaps)
		{
			inout_thisClimbGap.CanStand |= otherClimbGap.CanStand;
			inout_thisClimbGap.MustCrawl |= otherClimbGap.MustCrawl;
		}
		if(_value < other._value)
		{
			inout_thisClimbGap.Block = otherClimbGap.Block;
			inout_thisClimbGap.Meta = otherClimbGap.Meta;
			inout_thisClimbGap.Direction = otherClimbGap.Direction;
		}
		return get(Math.max(_value, other._value));
	}

	@Override
	public String toString()
	{
		if(_value <= None._value)
			return "None";
		if(_value == Sink._value)
			return "Sink";
		if(_value == BottomHold._value)
			return "BottomHold";
		if(_value == TopHold._value)
			return "TopHold";
		if(_value == Up._value)
			return "Up";
		return "FastUp";
	}

	public void print(String name)
	{
		PrintStream stream = System.err;
		if(name != null)
			stream.print(name + " = ");
		stream.println(this);
	}

	private static HandsClimbing get(int value)
	{
		if(value <= None._value)
			return None;
		if(value == Sink._value)
			return Sink;
		if(value == BottomHold._value)
			return BottomHold;
		if(value == TopHold._value)
			return TopHold;
		if(value == Up._value)
			return Up;
		return FastUp;
	}
}
```

---

## 역할

손(hands)으로 사다리/덩굴을 오르는 상태를 나타내는 **Typesafe Enum 패턴 클래스**. `FeetClimbing`과 동일한 구조로, 손 클라이밍 상태를 6개 인스턴스로 분류한다.

---

## import

```java
import java.io.*;  // PrintStream (print 메서드용)
```

---

## 상수 필드

### int 상수 (그랩 종류)

```java
public static final int MiddleGrab = 2;
public static final int UpGrab = 1;
public static final int NoGrab = 0;
```

손 위치/그랩 종류를 나타내는 정수 상수. `HandsClimbing` 인스턴스 값이 아닌 별도 정수. 실제 사용처는 이 파일 외부에서 확인 필요.

| 상수 | 값 | 의미 |
|------|----|------|
| `NoGrab` | 0 | 잡지 않음 |
| `UpGrab` | 1 | 위 잡기 |
| `MiddleGrab` | 2 | 가운데 잡기 |

### HandsClimbing 인스턴스 상수 (값 순서대로)

```java
public static HandsClimbing None       = new HandsClimbing(-3);
public static HandsClimbing Sink       = new HandsClimbing(-2);
public static HandsClimbing TopHold    = new HandsClimbing(-1);
public static HandsClimbing BottomHold = new HandsClimbing(0);
public static HandsClimbing Up         = new HandsClimbing(1);
public static HandsClimbing FastUp     = new HandsClimbing(2);
```

| 상수 | `_value` | 의미 |
|------|----------|------|
| `None` | -3 | 손 클라이밍 없음 |
| `Sink` | -2 | 가라앉음(내려감) |
| `TopHold` | -1 | 꼭대기에서 유지 |
| `BottomHold` | 0 | 아래에서 유지 |
| `Up` | 1 | 위로 이동 |
| `FastUp` | 2 | 빠르게 위로 이동 |

`FeetClimbing`(7개)보다 하나 적은 6개. 모두 `public static` (non-final).

---

## 생성자 (private)

```java
private HandsClimbing(int value)
{
    _value = value;
}
```

`private` — 외부에서 인스턴스 생성 불가. 6개 상수만 존재.

---

## 메서드

### `IsRelevant()`

```java
public boolean IsRelevant()
{
    return _value > None._value;  // _value > -3
}
```

`None`(-3)보다 크면 `true`. `Sink`(-2) 이상이면 관련 있는 손 클라이밍 상태.

### `IsUp()`

```java
public boolean IsUp()
{
    return this == Up || this == FastUp;
}
```

인스턴스 동일성(`==`) 비교. 위로 이동하는 2가지 상태(`_value` 1, 2)에서만 `true`.

### `ToUp()`

```java
public HandsClimbing ToUp()
{
    if(this == BottomHold)
        return Up;
    return this;
}
```

`BottomHold`(0)이면 `Up`(1)으로 전환. 다른 상태는 그대로 반환. 아래에서 유지 중일 때 위로 이동 방향으로 전환하는 용도.

### `ToDown()`

```java
public HandsClimbing ToDown()
{
    if(this == TopHold)
        return Sink;
    return this;
}
```

`TopHold`(-1)이면 `Sink`(-2)로 전환. 다른 상태는 그대로 반환. 꼭대기에서 유지 중일 때 아래 방향으로 전환하는 용도.

### `max(HandsClimbing other, ClimbGap inout_thisClimbGap, ClimbGap otherClimbGap)`

```java
public HandsClimbing max(HandsClimbing other, ClimbGap inout_thisClimbGap, ClimbGap otherClimbGap)
{
    if(!otherClimbGap.SkipGaps)
    {
        inout_thisClimbGap.CanStand |= otherClimbGap.CanStand;
        inout_thisClimbGap.MustCrawl |= otherClimbGap.MustCrawl;
    }
    if(_value < other._value)
    {
        inout_thisClimbGap.Block = otherClimbGap.Block;
        inout_thisClimbGap.Meta = otherClimbGap.Meta;
        inout_thisClimbGap.Direction = otherClimbGap.Direction;
    }
    return get(Math.max(_value, other._value));
}
```

`FeetClimbing.max()`와 동일한 구조:
- `otherClimbGap.SkipGaps == false`이면: `CanStand`, `MustCrawl`을 OR로 합산
- `_value < other._value`이면: `inout_thisClimbGap`의 Block/Meta/Direction을 `otherClimbGap` 값으로 교체
- 반환: `Math.max(_value, other._value)`에 해당하는 인스턴스를 `get()`으로 반환

### `toString()`

```java
@Override
public String toString()
{
    if(_value <= None._value)  // <= -3
        return "None";
    if(_value == Sink._value)  // == -2
        return "Sink";
    if(_value == BottomHold._value)  // == 0
        return "BottomHold";
    if(_value == TopHold._value)  // == -1
        return "TopHold";
    if(_value == Up._value)  // == 1
        return "Up";
    return "FastUp";  // >= 2
}
```

주의: `toString()`의 검사 순서가 `_value` 순서와 다름. `BottomHold`(0)를 `TopHold`(-1)보다 먼저 검사. `get()`과는 다른 순서이지만, 값이 겹치지 않으므로 결과는 동일.

### `print(String name)`

```java
public void print(String name)
{
    PrintStream stream = System.err;
    if(name != null)
        stream.print(name + " = ");
    stream.println(this);
}
```

`System.err`에 출력. `name != null`이면 `"name = 상태명"`, `null`이면 상태명만. 디버그용.

### `get(int value)` (private static)

```java
private static HandsClimbing get(int value)
{
    if(value <= None._value)  // <= -3
        return None;
    if(value == Sink._value)  // == -2
        return Sink;
    if(value == BottomHold._value)  // == 0
        return BottomHold;
    if(value == TopHold._value)  // == -1
        return TopHold;
    if(value == Up._value)  // == 1
        return Up;
    return FastUp;  // >= 2
}
```

정수 값 → `HandsClimbing` 인스턴스 변환. `max()`의 `Math.max()` 결과를 인스턴스로 복원하는 데 사용. `toString()`과 마찬가지로 `BottomHold`(0)를 `TopHold`(-1)보다 먼저 검사.

**`get()`에서 `-1`(TopHold)이 `0`(BottomHold) 뒤에 오는 이유**: `value == BottomHold._value` (0 검사)를 먼저 하고, 통과하면 `value == TopHold._value` (-1 검사). 음수가 양수보다 먼저 검사되지 않아도, 값이 겹치지 않으므로 동작에 문제 없음. 단, 순서가 `_value` 오름차순이 아님에 주의.

---

## FeetClimbing과의 비교

| 항목 | FeetClimbing | HandsClimbing |
|------|-------------|---------------|
| 인스턴스 수 | 7개 | 6개 |
| int 상수 | `DownStep`, `NoStep` | `MiddleGrab`, `UpGrab`, `NoGrab` |
| 추가 메서드 | `IsIndependentlyRelevant()` | `ToUp()`, `ToDown()` |
| 값 범위 | -3 ~ 3 | -3 ~ 2 |
| `max()` 구조 | 동일 | 동일 |

`HandsClimbing`에는 `FeetClimbing.IsIndependentlyRelevant()`가 없고, 대신 `ToUp()`/`ToDown()` 상태 전환 메서드가 있음.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `net.smart.moving.ClimbGap` | `max()` 파라미터 — 갭 정보 합산 |

---

## 주요 관찰 사항

1. **`ToUp()`/`ToDown()` 상태 전환**: `FeetClimbing`에 없는 메서드. `BottomHold` → `Up`, `TopHold` → `Sink` 전환. 키 입력(위/아래 방향키)에 따라 유지 상태에서 이동 상태로 전환할 때 사용하는 것으로 보임.

2. **`toString()`/`get()` 순서**: `_value` 오름차순이 아닌 순서로 검사하지만 값이 고유하므로 결과는 동일. 코드 일관성은 낮음.

3. **`FeetClimbing`과 대칭 구조**: 두 클래스 모두 Typesafe Enum 패턴, private 생성자, `max()` 동일 구조, `print()` 디버그 메서드 공유. 차이는 인스턴스 개수와 일부 추가 메서드.

4. **1.21.1 이식**: Java `enum`으로 교체 가능. `ToUp()`/`ToDown()`은 `switch` 표현식으로 대체. `max()` 로직은 그대로 유지.
