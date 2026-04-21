# ISmartMovingSelf.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/ISmartMovingSelf.java  
패키지: `net.smart.moving`  
종류: `interface`  
상속: 없음 (직접 `interface`)

---

## 전체 소스

```java
package net.smart.moving;

public interface ISmartMovingSelf
{
	float getExhaustion();

	float getUpJumpCharge();

	float getHeadJumpCharge();

	void addExhaustion(float factor);

	void setMaxExhaustionForAction(float maxExhaustionForAction);

	void setMaxExhaustionToStartAction(float maxExhaustionToStartAction);
}
```

---

## 역할

로컬 플레이어의 SmartMoving 이동 상태(`SmartMovingSelf`)가 외부에 제공하는 **공개 API 인터페이스**. 외부 모드가 SmartMoving의 소진(exhaustion)·충전(charge) 상태를 조회하고, 소진 추가 및 행동 제한 임계값을 설정할 수 있도록 한다.

`ISmartMovingClient`가 클라이언트 설정/상한값 API라면, `ISmartMovingSelf`는 현재 틱의 실시간 상태 및 행동 제한 조작 API다.

---

## import

없음.

---

## 메서드 목록

### 상태 조회 (getter)

```java
float getExhaustion();
```
현재 SmartMoving 소진값 반환.

```java
float getUpJumpCharge();
```
현재 위 점프(up jump) 충전값 반환.

```java
float getHeadJumpCharge();
```
현재 머리 점프(head jump) 충전값 반환.

---

### 상태 조작

```java
void addExhaustion(float factor);
```
SmartMoving 소진값에 `factor`를 추가. 외부 모드가 SmartMoving 이동에 추가 소진을 부과할 때 사용.

```java
void setMaxExhaustionForAction(float maxExhaustionForAction);
```
특정 행동을 수행하는 동안 허용하는 최대 소진값 설정. 소진이 이 값을 초과하면 해당 행동 중단.

```java
void setMaxExhaustionToStartAction(float maxExhaustionToStartAction);
```
특정 행동을 **시작**하기 위한 최대 소진값 설정. 소진이 이 값을 초과하면 해당 행동 시작 불가.

---

## `ISmartMovingClient`와의 비교

| 항목 | `ISmartMovingClient` | `ISmartMovingSelf` |
|------|---------------------|-------------------|
| 구현체 | `SmartMovingClient` | `SmartMovingSelf` |
| 대상 | 클라이언트 설정/상한값 | 현재 틱 실시간 상태 |
| 소진 | 상한값 등록/조회 (key-value) | 현재값 조회 + 추가 |
| 충전 | 최대 충전값 조회 | 현재 충전값 조회 |
| 행동 제한 | 없음 | `setMaxExhaustionForAction`, `setMaxExhaustionToStartAction` |

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| 없음 | import 없음 |

구현체: `SmartMovingSelf` (`net.smart.moving.playerapi` 패키지 — 이후 리서치 대상).

호출처: 외부 모드가 SmartMoving의 실시간 상태에 접근할 때. `SmartMovingFactory.doGetInstance(IEntityPlayerSP)` 등을 통해 로컬 플레이어의 `SmartMovingSelf` 인스턴스를 `ISmartMovingSelf`로 캐스팅해서 사용하는 것으로 추정되나, 실제 접근 경로는 이 파일에서 확인 불가.

---

## 주요 관찰 사항

1. **`setMaxExhaustionForAction` vs `setMaxExhaustionToStartAction`**: 두 임계값의 역할 구분 — "행동 중 최대 소진"(진행 중단 임계값)과 "행동 시작 최대 소진"(시작 가능 임계값). 외부 모드가 SmartMoving 이동의 허용 조건을 세밀하게 제어할 수 있는 API.

2. **`addExhaustion(float factor)`**: `factor`가 소진 추가량인지 배율(factor)인지는 이 파일만으로 확인 불가. 파라미터 이름이 `factor`이므로 배율일 가능성도 있으나, `SmartMovingSelf` 구현에서 확인 필요.

3. **1.21.1 이식**: 인터페이스 자체는 그대로 유지 가능. 구현체(`SmartMovingSelf`)에서 1.21.1 플레이어 소진 시스템(`HungerManager` 등)과 연동하는 방식 변경 필요.
