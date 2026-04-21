# FeetClimbing.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/FeetClimbing.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: 없음 (`Object` 직접 상속)

---

## 전체 소스

```java
package net.smart.moving;

import java.io.*;

public class FeetClimbing
{
	public static final int DownStep = 1;
	public static final int NoStep = 0;

	public static FeetClimbing None = new FeetClimbing(-3);
	public static FeetClimbing BaseHold = new FeetClimbing(-2);
	public static FeetClimbing BaseWithHands = new FeetClimbing(-1);
	public static FeetClimbing TopWithHands = new FeetClimbing(0);
	public static FeetClimbing SlowUpWithHoldWithoutHands = new FeetClimbing(1);
	public static FeetClimbing SlowUpWithSinkWithoutHands = new FeetClimbing(2);
	public static FeetClimbing FastUp = new FeetClimbing(3);

	private int _value;

	private FeetClimbing(int value)
	{
		_value = value;
	}

	public boolean IsRelevant()
	{
		return _value > None._value;
	}

	public boolean IsIndependentlyRelevant()
	{
		return _value > BaseWithHands._value;
	}

	public boolean IsUp()
	{
		return this == SlowUpWithHoldWithoutHands || this == SlowUpWithSinkWithoutHands || this == FastUp;
	}

	public FeetClimbing max(FeetClimbing other, ClimbGap inout_thisClimbGap, ClimbGap otherClimbGap)
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
		if(_value == BaseHold._value)
			return "BaseHold";
		if(_value == BaseWithHands._value)
			return "BaseWithHands";
		if(_value == TopWithHands._value)
			return "TopWithHands";
		if(_value == SlowUpWithHoldWithoutHands._value)
			return "SlowUpWithHoldWithoutHands";
		if(_value == SlowUpWithSinkWithoutHands._value)
			return "SlowUpWithSinkWithoutHands";
		return "FastUp";
	}

	public void print(String name)
	{
		PrintStream stream = System.err;
		if(name != null)
			stream.print(name + " = ");
		stream.println(this);
	}

	private static FeetClimbing get(int value)
	{
		if(value <= None._value)
			return None;
		if(value == BaseHold._value)
			return BaseHold;
		if(value == BaseWithHands._value)
			return BaseWithHands;
		if(value == TopWithHands._value)
			return TopWithHands;
		if(value == SlowUpWithHoldWithoutHands._value)
			return SlowUpWithHoldWithoutHands;
		if(value == SlowUpWithSinkWithoutHands._value)
			return SlowUpWithSinkWithoutHands;
		return FastUp;
	}
}
```

---

## 역할

발(feet)로 사다리/덩굴을 오르는 상태를 나타내는 **타입 안전 열거형 클래스**. `int _value`를 내부 순서 값으로 가지며, 7개의 공유 인스턴스(싱글톤 상수)로 존재한다.

`HandsClimbing`과 대응되는 쌍으로, 발 클라이밍 상태를 분류하고 여러 방향에서 계산된 값을 `max()`로 합산할 때 사용된다.

---

## import

```java
import java.io.*;  // PrintStream (print 메서드용)
```

---

## 상수 필드

### int 상수

```java
public static final int DownStep = 1;
public static final int NoStep = 0;
```

`DownStep`/`NoStep`은 `FeetClimbing` 인스턴스 값이 아닌 정수 상수. 클라이밍 중 발 스텝 종류를 나타내는 값으로, 외부에서 참조용으로 사용. (실제 사용처는 이 파일 외부에서 확인 필요)

### FeetClimbing 인스턴스 상수 (값 순서대로)

```java
public static FeetClimbing None                       = new FeetClimbing(-3);
public static FeetClimbing BaseHold                   = new FeetClimbing(-2);
public static FeetClimbing BaseWithHands              = new FeetClimbing(-1);
public static FeetClimbing TopWithHands               = new FeetClimbing(0);
public static FeetClimbing SlowUpWithHoldWithoutHands = new FeetClimbing(1);
public static FeetClimbing SlowUpWithSinkWithoutHands = new FeetClimbing(2);
public static FeetClimbing FastUp                     = new FeetClimbing(3);
```

| 상수 | `_value` | 의미 |
|------|----------|------|
| `None` | -3 | 발 클라이밍 없음 |
| `BaseHold` | -2 | 기저부 유지(정지) |
| `BaseWithHands` | -1 | 기저부, 손 필요 |
| `TopWithHands` | 0 | 꼭대기, 손 필요 |
| `SlowUpWithHoldWithoutHands` | 1 | 손 없이 천천히 위, 유지 모션 |
| `SlowUpWithSinkWithoutHands` | 2 | 손 없이 천천히 위, 가라앉는 모션 |
| `FastUp` | 3 | 빠르게 위 |

값이 높을수록 "더 강한" 상태. `max()`에서 더 큰 값을 선택한다.

모두 `public static` (non-final). `final`이 아니어서 외부에서 재할당 가능하지만, 실제로 재할당하는 코드는 이 파일에 없음.

---

## 생성자 (private)

```java
private FeetClimbing(int value)
{
    _value = value;
}
```

`private` — 외부에서 인스턴스 생성 불가. 7개 상수만 존재.

---

## 메서드

### `IsRelevant()`

```java
public boolean IsRelevant()
{
    return _value > None._value;  // _value > -3
}
```

`None`(-3)보다 크면 `true`. 즉, `BaseHold`(-2) 이상이면 관련 있는 클라이밍 상태.

### `IsIndependentlyRelevant()`

```java
public boolean IsIndependentlyRelevant()
{
    return _value > BaseWithHands._value;  // _value > -1
}
```

`BaseWithHands`(-1)보다 크면 `true`. 즉 `TopWithHands`(0) 이상. "손 없이도 독립적으로 의미 있는" 상태.

### `IsUp()`

```java
public boolean IsUp()
{
    return this == SlowUpWithHoldWithoutHands || this == SlowUpWithSinkWithoutHands || this == FastUp;
}
```

인스턴스 동일성(`==`) 비교. 위로 이동하는 3가지 상태(`_value` 1, 2, 3)에서만 `true`.

### `max(FeetClimbing other, ClimbGap inout_thisClimbGap, ClimbGap otherClimbGap)`

```java
public FeetClimbing max(FeetClimbing other, ClimbGap inout_thisClimbGap, ClimbGap otherClimbGap)
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

두 `FeetClimbing` 상태 중 더 강한 것을 반환하면서, 동시에 `ClimbGap`(갭 정보)도 합산한다.

**ClimbGap 처리:**
- `otherClimbGap.SkipGaps == false`이면: `inout_thisClimbGap.CanStand |= otherClimbGap.CanStand` (OR 합산), `inout_thisClimbGap.MustCrawl |= otherClimbGap.MustCrawl` (OR 합산)
- `otherClimbGap.SkipGaps == true`이면: CanStand/MustCrawl 합산 건너뜀

**Block/Meta/Direction 처리:**
- `_value < other._value` (other가 더 강함)이면: `inout_thisClimbGap`의 Block/Meta/Direction을 `otherClimbGap`의 값으로 교체

**반환:** `Math.max(_value, other._value)`에 해당하는 `FeetClimbing` 인스턴스를 `get()`으로 조회해서 반환.

`inout_` 접두사는 in-out 파라미터(입출력 모두 사용)를 나타내는 네이밍 컨벤션.

### `toString()`

```java
@Override
public String toString()
{
    if(_value <= None._value)  // <= -3
        return "None";
    if(_value == BaseHold._value)  // == -2
        return "BaseHold";
    if(_value == BaseWithHands._value)  // == -1
        return "BaseWithHands";
    if(_value == TopWithHands._value)  // == 0
        return "TopWithHands";
    if(_value == SlowUpWithHoldWithoutHands._value)  // == 1
        return "SlowUpWithHoldWithoutHands";
    if(_value == SlowUpWithSinkWithoutHands._value)  // == 2
        return "SlowUpWithSinkWithoutHands";
    return "FastUp";  // >= 3
}
```

`_value <= -3`이면 "None" (None 미만도 None으로 처리), `>= 3`이면 "FastUp" (FastUp 초과도 FastUp으로 처리). `get()`과 동일한 범위 처리.

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

`System.err`에 출력. `name != null`이면 `"name = 상태명"`, `null`이면 상태명만 출력. 디버그용.

### `get(int value)` (private static)

```java
private static FeetClimbing get(int value)
{
    if(value <= None._value)  // <= -3
        return None;
    if(value == BaseHold._value)  // == -2
        return BaseHold;
    if(value == BaseWithHands._value)  // == -1
        return BaseWithHands;
    if(value == TopWithHands._value)  // == 0
        return TopWithHands;
    if(value == SlowUpWithHoldWithoutHands._value)  // == 1
        return SlowUpWithHoldWithoutHands;
    if(value == SlowUpWithSinkWithoutHands._value)  // == 2
        return SlowUpWithSinkWithoutHands;
    return FastUp;  // >= 3
}
```

정수 값 → `FeetClimbing` 인스턴스 변환. `max()`의 `Math.max()` 결과를 인스턴스로 복원하기 위해 사용. 범위 밖 값은 경계 인스턴스로 클램프(`<= -3` → None, `>= 3` → FastUp).

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `net.smart.moving.ClimbGap` | `max()` 파라미터 — 갭 정보 합산 |

---

## 주요 관찰 사항

1. **Typesafe Enum 패턴**: Java 5 이전의 `enum` 대안 패턴. `private` 생성자 + `public static` 인스턴스 상수 + `get(int)` 역조회. Java `enum`처럼 사용하지만 `int _value`로 비교 가능.

2. **`==` 인스턴스 동일성 비교**: `IsUp()`에서 `this == SlowUpWithHoldWithoutHands` 등 참조 비교 사용. 인스턴스가 7개만 존재하므로 안전.

3. **`max()`의 ClimbGap 합산**: 단순히 더 큰 값을 반환하는 게 아니라, 두 ClimbGap의 CanStand/MustCrawl을 OR로 합산하고, 더 강한 쪽의 Block/Meta/Direction으로 교체. 여러 방향(4방향)에서 계산한 클라이밍 결과를 하나로 합칠 때 사용.

4. **`SkipGaps` 조건**: `otherClimbGap.SkipGaps == true`이면 CanStand/MustCrawl 합산을 건너뜀 — 갭 정보가 무시되어야 하는 특수 케이스.

5. **`DownStep`/`NoStep` 상수**: `FeetClimbing` 인스턴스와 별도로 존재하는 `int` 상수. 발 스텝 방향 구분용. 실제 사용처(`SmartMovingSelf` 등)에서 확인 필요.

6. **1.21.1 이식**: Java `enum`으로 직접 교체 가능. `_value` 기반 비교를 `ordinal()` 또는 명시적 우선순위 필드로 대체. `max()`의 ClimbGap 합산 로직은 그대로 유지.
