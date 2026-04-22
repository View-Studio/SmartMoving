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

---

## R-03 리서치 결과 추가 (A-09, A-10)

> 소스 재확인 결과: PENDING_RESEARCH A-09의 원본 위치가 `SmartMovingClient.java`로 잘못 기재됨.
> `processBlockCode`의 실제 위치는 **`SmartMovingComm.java`**이며, `SmartMovingComm.md`에 이미 완전히 기록되어 있음.

---

### A-09 — `processBlockCode` 위치 및 전체 구현

**실제 위치**: `SmartMovingComm.java` → `SmartMovingComm.md`에 기록됨  
**SmartMovingClient.java에는 이 메서드가 존재하지 않음** (확인됨).

요약 (상세 내용은 SmartMovingComm.md 참조):

```java
// SmartMovingComm.java (public static)
public static boolean processBlockCode(String text)
{
    if(!text.startsWith("§0§1") || !text.endsWith("§f§f"))
        return false;

    String codes = text.substring(4, text.length() - 4);
    processBlockCode(codes, "§0", Options._baseClimb, "standard");
    processBlockCode(codes, "§1", Options._freeClimb);
    processBlockCode(codes, "§2", Options._ceilingClimbing);
    processBlockCode(codes, "§3", Options._swim);
    processBlockCode(codes, "§4", Options._dive);
    processBlockCode(codes, "§5", Options._crawl);
    processBlockCode(codes, "§6", Options._slide);
    processBlockCode(codes, "§7", Options._fly);
    processBlockCode(codes, "§8", Options._jumpCharge);
    processBlockCode(codes, "§9", Options._headJump);
    processBlockCode(codes, "§a", Options._angleJumpSide);
    processBlockCode(codes, "§b", Options._angleJumpBack);
    return true;
}
```

**채팅 마커 포맷 (확인됨):**
- 시작 마커: `"§0§1"` (정확히 4글자)
- 끝 마커: `"§f§f"` (정확히 4글자)
- 유효 코드 추출: `text.substring(4, text.length() - 4)`

**12개 기능 블록코드 → Property 매핑 (확인됨):**

| 블록코드 | Options 필드 | 적용 값 |
|---------|-------------|---------|
| `"§0"` | `Options._baseClimb` | `"standard"` (유일하게 false가 아님) |
| `"§1"` | `Options._freeClimb` | `"false"` |
| `"§2"` | `Options._ceilingClimbing` | `"false"` |
| `"§3"` | `Options._swim` | `"false"` |
| `"§4"` | `Options._dive` | `"false"` |
| `"§5"` | `Options._crawl` | `"false"` |
| `"§6"` | `Options._slide` | `"false"` |
| `"§7"` | `Options._fly` | `"false"` |
| `"§8"` | `Options._jumpCharge` | `"false"` |
| `"§9"` | `Options._headJump` | `"false"` |
| `"§a"` | `Options._angleJumpSide` | `"false"` |
| `"§b"` | `Options._angleJumpBack` | `"false"` |

**내부 처리 경로:**
`processBlockCode(codes, blockCode, property, value...)` (private overload):
```java
if(text.contains(blockCode))
    processConfigPacket(new String[] { property.getCurrentKey(), value.length > 0 ? value[0] : "false" }, null, true);
```
→ `processConfigPacket(content, null, blockCode=true)` → `ServerConfig.loadFromProperties(content, true)` → `Config = ServerConfig`

---

### A-10 — `updateEntityActionState()` 내 processBlockCode 호출 경로

**소스 위치**: `SmartMovingSelf.java` 2347~2357줄 (updateEntityActionState 내부)

```java
if(sp.worldObj.isRemote && updateCounter < 10)
{
    List<?> chatMessageList = (List<?>)Reflect.GetField(
        GuiNewChat.class,
        isp.getMcField().ingameGUI.getChatGUI(),
        SmartMovingInstall.GuiNewChat_chatMessageList);
    for(int i=0; i<chatMessageList.size(); i++)
        if(SmartMovingComm.processBlockCode(
            ((ChatLine)chatMessageList.get(i)).func_151461_a().getUnformattedText()))
            chatMessageList.remove(i--);
    updateCounter++;
}
```

**실행 조건:**
- `sp.worldObj.isRemote == true` — 클라이언트 월드에서만 실행
- `updateCounter < 10` — SmartMovingSelf 인스턴스 필드, 접속 후 첫 10틱만 실행
- 10틱 이후 `updateCounter >= 10`이 되면 이 블록은 영원히 실행되지 않음

**동작:**
1. `Reflect.GetField`로 `GuiNewChat.chatMessageList` (private 필드)에 직접 접근
2. 채팅 히스토리 전체를 역순 없이 0번부터 순회
3. 각 `ChatLine`의 텍스트를 `getUnformattedText()`로 추출 (§코드 포함 raw 문자열)
4. `SmartMovingComm.processBlockCode(text)`가 `true` 반환 → `chatMessageList.remove(i--)` (플레이어에게 채팅이 보이지 않게 즉시 제거)
5. `updateCounter++`

**processBlockCode 결과가 반영되는 방식:**
- SmartMovingSelf의 특정 boolean 필드에 **직접 쓰이지 않는다**
- `processBlockCode` → `processConfigPacket(blockCode=true)` → `ServerConfig.loadFromProperties()` → `Config = ServerConfig` (전역 교체)
- 이후 같은 틱의 `updateEntityActionState` 내 `Config.isXxxEnabled()` 호출들이 새 값을 반환함
- 결과적으로 `wantClimbCeiling`, `canCrawl`, `isHeadJumping` 등의 판정 조건이 변경됨

**목적**: 서버가 접속 초기에 채팅 채널로 전송한 설정 메시지(§마커 포함)를 클라이언트가 파싱해 Config에 반영하고, 해당 메시지를 채팅 창에서 제거하는 메커니즘.

**1.21.1 이식 고려:**
- `GuiNewChat.chatMessageList` Reflect 접근 → Fabric `ClientReceiveMessageEvents`로 대체 필요
- `updateCounter < 10` 조건도 이식 필요 (초기 10틱 스캔)
- 단, 1.21.1에서는 채팅 이벤트 훅이 있으므로 채팅 히스토리 직접 스캔 대신 이벤트 수신 시 즉시 처리 가능
