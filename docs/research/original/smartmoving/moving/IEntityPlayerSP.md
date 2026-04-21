# IEntityPlayerSP.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/IEntityPlayerSP.java  
패키지: `net.smart.moving`  
종류: `interface`  
상속: 없음 (직접 `interface`)

---

## 전체 소스

```java
package net.smart.moving;

import net.minecraft.block.material.*;
import net.minecraft.client.*;
import net.minecraft.entity.player.EntityPlayer.*;
import net.minecraft.nbt.*;

public interface IEntityPlayerSP
{
	SmartMoving getMoving();

	boolean getSleepingField();

	boolean getIsJumpingField();

	boolean getIsInWebField();

	void setIsInWebField(boolean b);

	Minecraft getMcField();

	void setMoveForwardField(float f);

	void setMoveStrafingField(float f);

	void setIsJumpingField(boolean flag);

	void localMoveEntity(double d, double d1, double d2);

	EnumStatus localSleepInBedAt(int i, int j, int k);

	float localGetBrightness(float f);

	int localGetBrightnessForRender(float f);

	void localUpdateEntityActionState();

	boolean localIsInsideOfMaterial(Material material);

	void localWriteEntityToNBT(NBTTagCompound nBTTagCompound);

	boolean localIsSneaking();

	float localGetFOVMultiplier();
}
```

---

## 역할

**클라이언트 측 로컬 플레이어 엔티티**(`EntityPlayerSP`)에 대해 SmartMoving이 필요로 하는 기능을 추상화한 인터페이스. `IEntityPlayerMP`와 달리 `IPacketSender`를 확장하지 않는다.

`SmartMovingSelf`(클라이언트 이동 로직)가 이 인터페이스를 통해 로컬 플레이어 엔티티의 필드/메서드에 접근한다. 구현체는 `SmartMovingPlayerBase` (playerapi 패키지).

---

## import

```java
import net.minecraft.block.material.*;           // Material
import net.minecraft.client.*;                   // Minecraft
import net.minecraft.entity.player.EntityPlayer.*; // EnumStatus (내부 enum)
import net.minecraft.nbt.*;                      // NBTTagCompound
```

`EntityPlayer.*`에서 `EnumStatus`를 import — `EntityPlayer` 내부의 `EnumStatus` enum (침대 수면 결과).

---

## 메서드 목록

### SmartMoving 인스턴스 접근

```java
SmartMoving getMoving();
```
이 플레이어에 연결된 `SmartMoving` 인스턴스 반환. `SmartMovingComm.processStatePacket()`에서 다른 플레이어의 상태 패킷 처리 시, `SmartMovingFactory.doGetInstance()`에서 SP 플레이어의 SmartMoving 인스턴스 조회 시 사용.

---

### 필드 직접 접근 (getter)

```java
boolean getSleepingField();
```
`EntityPlayer.sleeping` 필드 직접 반환. PlayerAPI를 통한 오버라이드 체인 없이 원본 필드 값 읽기.

```java
boolean getIsJumpingField();
```
`EntityLivingBase.isJumping` 필드 직접 반환.

```java
boolean getIsInWebField();
```
`Entity.isInWeb` 필드 직접 반환 — 거미줄 안에 있는지 여부.

```java
Minecraft getMcField();
```
`EntityPlayerSP.mc` 필드 직접 반환 — `Minecraft` 인스턴스.

---

### 필드 직접 쓰기 (setter)

```java
void setIsInWebField(boolean b);
```
`Entity.isInWeb` 필드 직접 설정.

```java
void setMoveForwardField(float f);
```
`EntityLivingBase.moveForward` 필드 직접 설정 — 앞뒤 이동 입력값.

```java
void setMoveStrafingField(float f);
```
`EntityLivingBase.moveStrafing` 필드 직접 설정 — 좌우 이동 입력값.

```java
void setIsJumpingField(boolean flag);
```
`EntityLivingBase.isJumping` 필드 직접 설정.

---

### local 메서드 (vanilla 원본 직접 호출)

```java
void localMoveEntity(double d, double d1, double d2);
```
`Entity.moveEntity(double, double, double)` vanilla 원본 직접 호출. SmartMoving의 이동 오버라이드 없이 실제 이동 처리.

```java
EnumStatus localSleepInBedAt(int i, int j, int k);
```
`EntityPlayer.sleepInBedAt(int, int, int)` vanilla 원본 직접 호출. 반환값 `EnumStatus` — 수면 시작 결과 (OK, NOT_POSSIBLE_HERE, NOT_POSSIBLE_NOW, TOO_FAR_AWAY, OTHER_PROBLEM 등).

```java
float localGetBrightness(float f);
```
`Entity.getBrightness(float)` vanilla 원본 직접 호출.

```java
int localGetBrightnessForRender(float f);
```
`Entity.getBrightnessForRender(float)` vanilla 원본 직접 호출.

```java
void localUpdateEntityActionState();
```
`EntityLivingBase.updateEntityActionState()` vanilla 원본 직접 호출 — 플레이어 액션 상태 업데이트 (수영, 점프 등 감지).

```java
boolean localIsInsideOfMaterial(Material material);
```
`Entity.isInsideOfMaterial(Material)` vanilla 원본 직접 호출 — 특정 재질(물, 용암 등) 안에 있는지 확인.

```java
void localWriteEntityToNBT(NBTTagCompound nBTTagCompound);
```
`Entity.writeEntityToNBT(NBTTagCompound)` vanilla 원본 직접 호출 — 엔티티 데이터를 NBT에 저장.

```java
boolean localIsSneaking();
```
`EntityLivingBase.isSneaking()` vanilla 원본 직접 호출 — SmartMoving sneaking 오버라이드 없이 실제 스니킹 상태.

```java
float localGetFOVMultiplier();
```
`EntityPlayerSP.getFOVMultiplier()` vanilla 원본 직접 호출 — 시야각 배율 계산.

---

## 메서드 분류 요약

| 분류 | 메서드 |
|------|--------|
| SmartMoving 접근 | `getMoving()` |
| 필드 getter | `getSleepingField`, `getIsJumpingField`, `getIsInWebField`, `getMcField` |
| 필드 setter | `setIsInWebField`, `setMoveForwardField`, `setMoveStrafingField`, `setIsJumpingField` |
| local 메서드 | `localMoveEntity`, `localSleepInBedAt`, `localGetBrightness`, `localGetBrightnessForRender`, `localUpdateEntityActionState`, `localIsInsideOfMaterial`, `localWriteEntityToNBT`, `localIsSneaking`, `localGetFOVMultiplier` |

총 19개 메서드. `local` 메서드가 9개로 가장 많음.

---

## IEntityPlayerMP와의 비교

| 항목 | IEntityPlayerMP | IEntityPlayerSP |
|------|----------------|----------------|
| 상속 | `IPacketSender` | 없음 |
| 실행 위치 | 서버 | 클라이언트 |
| 패킷 관련 | 있음 | 없음 |
| 필드 직접 접근 | 없음 (메서드만) | getter 4개 + setter 4개 |
| `local` 메서드 수 | 5개 | 9개 |
| `getMoving()` 반환 | `SmartMovingServer` | `SmartMoving` |

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMoving` | `getMoving()` 반환 타입 |
| `Minecraft` | `getMcField()` 반환 타입 |
| `Material` | `localIsInsideOfMaterial()` 파라미터 |
| `EnumStatus` | `localSleepInBedAt()` 반환 타입 |
| `NBTTagCompound` | `localWriteEntityToNBT()` 파라미터 |

구현체: `SmartMovingPlayerBase` (`net.smart.moving.playerapi` 패키지 — 이후 리서치 대상).

---

## 주요 관찰 사항

1. **필드 직접 접근 패턴**: `getXxxField()` / `setXxxField()` — PlayerAPI 체인을 거치지 않고 `EntityPlayerSP`/`EntityLivingBase`의 private/protected 필드에 직접 접근. 반사(reflection) 없이 접근하려면 `SmartMovingPlayerBase`가 같은 클래스 계층에 있어야 하므로 PlayerAPI의 `extends EntityPlayerSP` 구조를 활용.

2. **`local` 접두사 컨벤션**: `IEntityPlayerMP`와 동일. SmartMoving이 오버라이드한 메서드의 vanilla 원본을 선택적으로 호출하는 경로. `SmartMovingSelf`에서 특정 상황(클라이밍 중 수면 체크 등)에서 vanilla 원본 동작이 필요할 때 사용.

3. **`localUpdateEntityActionState()`**: PlayerAPI에서 오버라이드한 `updateEntityActionState()`의 vanilla 원본. SmartMoving이 이 메서드를 오버라이드해서 커스텀 이동 로직을 삽입하므로, 필요할 때 원본을 직접 호출하는 경로가 필요.

4. **`setMoveForwardField` / `setMoveStrafingField`**: `moveForward`, `moveStrafing`은 vanilla에서 이동 입력을 나타내는 필드. SmartMoving이 클라이밍/크롤링 중 이 값을 직접 조작해서 이동 방향을 제어하는 데 사용.

5. **1.21.1 이식**:
   - `Material` → 1.21.1에서 제거됨. `FluidState` 또는 `BlockState` 기반으로 재구현 필요.
   - `EnumStatus` → 1.21.1 `SleepFailureReason` (Optional 반환)으로 대체.
   - `NBTTagCompound` → 1.21.1 `NbtCompound`
   - `getMcField()` → `MinecraftClient.getInstance()`로 대체 가능.
   - 필드 직접 접근은 Mixin accessor 인터페이스로 대체.
