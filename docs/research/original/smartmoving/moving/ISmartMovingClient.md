# ISmartMovingClient.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/ISmartMovingClient.java  
패키지: `net.smart.moving`  
종류: `interface`  
상속: 없음 (직접 `interface`)

---

## 전체 소스

```java
package net.smart.moving;

public interface ISmartMovingClient
{
	float getMaximumExhaustion();

	float getMaximumUpJumpCharge();

	float getMaximumHeadJumpCharge();

	void setMaximumExhaustionValue(String key, float value);

	float getMaximumExhaustionValue(String key);

	boolean removeMaximumExhaustionValue(String key);

	void setNativeUserInterfaceDrawing(boolean value);

	boolean getNativeUserInterfaceDrawing();
}
```

---

## 역할

SmartMoving 클라이언트(`SmartMovingClient`)가 외부에 제공하는 **공개 API 인터페이스**. 외부 모드가 SmartMoving의 소진(exhaustion) 상한값과 충전(charge) 값을 조회·조작하거나, UI 렌더링 방식을 제어할 수 있도록 공개된 계약.

`SmartMovingContext.Client` (static final) 필드의 타입이 `ISmartMovingClient`이므로, 외부 코드는 `SmartMovingContext.Client.getMaximumExhaustion()` 등으로 접근 가능.

---

## import

없음.

---

## 메서드 목록

### 소진(Exhaustion) 관련

```java
float getMaximumExhaustion();
```
SmartMoving이 허용하는 최대 소진값 반환. `SmartMovingClient` 리서치에서 확인: `Math.max(Config.getMaxExhaustion(), 등록된 모든 외부 값의 최댓값)`.

```java
void setMaximumExhaustionValue(String key, float value);
```
key-value 형태로 외부 모드가 소진 상한값을 등록. `SmartMovingClient.maximumExhaustionValues` Map에 저장.

```java
float getMaximumExhaustionValue(String key);
```
특정 key에 등록된 소진 상한값 반환. key가 없으면 어떤 값을 반환하는지는 `SmartMovingClient` 구현에서 확인 필요.

```java
boolean removeMaximumExhaustionValue(String key);
```
특정 key의 소진 상한값 등록 제거. 반환값 `boolean` — 해당 key가 존재했으면 `true`, 없었으면 `false` (Map.remove 동작).

---

### 충전(Charge) 관련

```java
float getMaximumUpJumpCharge();
```
위 점프(up jump) 최대 충전값 반환.

```java
float getMaximumHeadJumpCharge();
```
머리 점프(head jump) 최대 충전값 반환.

---

### UI 렌더링 제어

```java
void setNativeUserInterfaceDrawing(boolean value);
```
네이티브(기본) UI 드로잉 사용 여부 설정.

```java
boolean getNativeUserInterfaceDrawing();
```
현재 네이티브 UI 드로잉 사용 여부 반환. `SmartMovingClient` 리서치에서 확인: 초기값 `true`.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| 없음 | import 없음 |

구현체: `SmartMovingClient extends SmartMovingContext implements ISmartMovingClient`

호출처:
- `SmartMovingContext.Client` 필드 타입 (`ISmartMovingClient`) — 외부 모드가 이 인터페이스를 통해 SmartMoving 클라이언트에 접근
- SmartMoving 내부에서 `Client.getMaximumExhaustion()` 등 호출

---

## 주요 관찰 사항

1. **외부 API 설계**: `setMaximumExhaustionValue(String key, float value)` / `removeMaximumExhaustionValue(String key)` — 외부 모드가 자신의 key로 소진 상한값을 등록/해제하는 플러그인 방식. 여러 모드가 충돌 없이 각자의 소진 한계를 등록 가능.

2. **`getMaximumExhaustion()`**: 모든 등록된 외부 값과 `Config.getMaxExhaustion()`의 최댓값 반환 — 가장 관대한 소진 허용치를 사용. 외부 모드가 SmartMoving의 소진 한계를 높이는(더 많은 소진 허용) 방향으로만 영향 가능.

3. **`NativeUserInterfaceDrawing`**: SmartMoving이 기본 UI 렌더링을 수행할지 여부. 외부 모드가 SmartMoving UI를 커스텀 렌더링으로 교체할 때 `false`로 설정.

4. **`getMaximumUpJumpCharge` / `getMaximumHeadJumpCharge`**: 점프 충전 최대값 조회. 외부에서 읽기만 가능 (setter 없음). 실제 값은 `SmartMovingClient` 또는 Config에서 결정.

5. **1.21.1 이식**: 인터페이스 자체는 그대로 유지 가능. `NativeUserInterfaceDrawing` 관련 동작은 1.21.1 렌더링 방식(HUD 렌더링 이벤트 등)에 맞게 구현체를 수정.
