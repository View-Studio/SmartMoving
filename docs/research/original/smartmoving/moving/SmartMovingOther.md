# SmartMovingOther.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMovingOther.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: `SmartMovingOther extends SmartMoving`

---

## 전체 소스

```java
package net.smart.moving;

import net.minecraft.client.entity.*;

public class SmartMovingOther extends SmartMoving
{
	public boolean foundAlive;

	public SmartMovingOther(EntityOtherPlayerMP sp)
	{
		super(sp, null);
	}

	public void processStatePacket(long state)
	{
		actualFeetClimbType = (int)(state & 15);
		state >>>= 4;

		actualHandsClimbType = (int)(state & 15);
		state >>>= 4;

		_isJumping = (state & 1) != 0;
		state >>>= 1;

		isDiving = (state & 1) != 0;
		state >>>= 1;

		isDipping = (state & 1) != 0;
		state >>>= 1;

		isSwimming = (state & 1) != 0;
		state >>>= 1;

		isCrawlClimbing = (state & 1) != 0;
		state >>>= 1;

		isCrawling = (state & 1) != 0;
		state >>>= 1;

		isClimbing = (state & 1) != 0;
		state >>>= 1;

		boolean isSmall = (state & 1) != 0;
		heightOffset = isSmall ? -1 : 0;
		sp.height = 1.8F + heightOffset;
		state >>>= 1;

		_doFallingAnimation = (state & 1) != 0;
		state >>>= 1;

		_doFlyingAnimation = (state & 1) != 0;
		state >>>= 1;

		isCeilingClimbing = (state & 1) != 0;
		state >>>= 1;

		isLevitating = (state & 1) != 0;
		state >>>= 1;

		isHeadJumping = (state & 1) != 0;
		state >>>= 1;

		isSliding = (state & 1) != 0;
		state >>>= 1;

		angleJumpType = (int)(state & 7);
		state >>>= 3;

		isFeetVineClimbing = (state & 1) != 0;
		state >>>= 1;

		isHandsVineClimbing = (state & 1) != 0;
		state >>>= 1;

		isClimbJumping = (state & 1) != 0;
		state >>>= 1;

		boolean wasClimbBackJumping = isClimbBackJumping;
		isClimbBackJumping = (state & 1) != 0;
		if(!wasClimbBackJumping && isClimbBackJumping)
			onStartClimbBackJump();
		state >>>= 1;

		isSlow = (state & 1) != 0;
		state >>>= 1;

		isFast = (state & 1) != 0;
		state >>>= 1;

		boolean wasWallJumping = isWallJumping;
		isWallJumping = (state & 1) != 0;
		if(!wasWallJumping && isWallJumping)
			onStartWallJump(null);
		state >>>= 1;

		isRopeSliding = (state & 1) != 0;
	}

	@Override
	public boolean isJumping()
	{
		return _isJumping;
	}

	@Override
	public boolean doFlyingAnimation()
	{
		return _doFlyingAnimation;
	}

	@Override
	public boolean doFallingAnimation()
	{
		return _doFallingAnimation;
	}

	private boolean _isJumping = false;
	private boolean _doFlyingAnimation = false;
	private boolean _doFallingAnimation = false;
}
```

---

## 역할

클라이언트 측에서 **다른 플레이어(`EntityOtherPlayerMP`)의 SmartMoving 상태를 표현**하는 구체 클래스.

- 서버가 릴레이한 상태 패킷(`long state`)을 비트 역직렬화 → `SmartMoving`의 상태 필드들에 적용
- `SmartMovingFactory`의 `otherSmartMovings` 맵에서 entityId → SmartMovingOther로 관리
- `SmartMoving`의 3개 abstract 메서드 구현: `isJumping()`, `doFlyingAnimation()`, `doFallingAnimation()`

---

## import

```java
import net.minecraft.client.entity.*;  // EntityOtherPlayerMP
```

---

## 필드

```java
public boolean foundAlive;
```

`SmartMovingFactory.doHandleMultiPlayerTick()`에서 mark-and-sweep에 사용.  
매 틱 살아있는 플레이어 발견 시 `true` → 다음 단계에서 `false`로 리셋.  
틱에 발견 안 되면 `false` 유지 → 맵에서 제거.

```java
private boolean _isJumping = false;
private boolean _doFlyingAnimation = false;
private boolean _doFallingAnimation = false;
```

`SmartMoving` abstract 메서드의 구현용 backing 필드.  
`processStatePacket`에서 설정됨.

---

## 생성자

```java
public SmartMovingOther(EntityOtherPlayerMP sp)
{
    super(sp, null);
}
```

`SmartMovingBase(EntityPlayer sp, IEntityPlayerSP isp)` 에 `isp = null` 전달.  
`SmartMovingBase`에서 `esp = isp` → `null` — 서버 측 로직 비활성화.  
`sp`는 `EntityOtherPlayerMP` — 다른 클라이언트 플레이어 엔티티.

---

## `processStatePacket(long state)` — 비트 역직렬화

서버가 릴레이한 `long state`를 LSB부터 MSB 방향으로 순서대로 파싱.  
`>>>` (부호 없는 우시프트)로 파싱 완료된 비트 제거.

### 비트 레이아웃 (LSB=bit0 → MSB=bit32)

| 비트 위치 | 폭 | 필드 |
|----------|----|------|
| 0~3 | 4 | `actualFeetClimbType` |
| 4~7 | 4 | `actualHandsClimbType` |
| 8 | 1 | `_isJumping` |
| 9 | 1 | `isDiving` |
| 10 | 1 | `isDipping` |
| 11 | 1 | `isSwimming` |
| 12 | 1 | `isCrawlClimbing` |
| 13 | 1 | `isCrawling` |
| 14 | 1 | `isClimbing` |
| 15 | 1 | `isSmall` (→ `heightOffset`, `sp.height`) |
| 16 | 1 | `_doFallingAnimation` |
| 17 | 1 | `_doFlyingAnimation` |
| 18 | 1 | `isCeilingClimbing` |
| 19 | 1 | `isLevitating` |
| 20 | 1 | `isHeadJumping` |
| 21 | 1 | `isSliding` |
| 22~24 | 3 | `angleJumpType` |
| 25 | 1 | `isFeetVineClimbing` |
| 26 | 1 | `isHandsVineClimbing` |
| 27 | 1 | `isClimbJumping` |
| 28 | 1 | `isClimbBackJumping` |
| 29 | 1 | `isSlow` |
| 30 | 1 | `isFast` |
| 31 | 1 | `isWallJumping` |
| 32 | 1 | `isRopeSliding` |

총 33비트 (0~32). SmartMovingOther는 bit 32(isRopeSliding)까지만 읽음.  
서버(`SmartMovingServer.processStatePacket`)는 bit 33을 `isSneakButtonPressed`로 추가 읽음.

### 파싱 코드 (전체)

```java
actualFeetClimbType = (int)(state & 15);   // bits 0-3
state >>>= 4;

actualHandsClimbType = (int)(state & 15);  // bits 4-7
state >>>= 4;

_isJumping = (state & 1) != 0;             // bit 8
state >>>= 1;

isDiving = (state & 1) != 0;               // bit 9
state >>>= 1;

isDipping = (state & 1) != 0;              // bit 10
state >>>= 1;

isSwimming = (state & 1) != 0;             // bit 11
state >>>= 1;

isCrawlClimbing = (state & 1) != 0;        // bit 12
state >>>= 1;

isCrawling = (state & 1) != 0;             // bit 13
state >>>= 1;

isClimbing = (state & 1) != 0;             // bit 14
state >>>= 1;

boolean isSmall = (state & 1) != 0;        // bit 15
heightOffset = isSmall ? -1 : 0;
sp.height = 1.8F + heightOffset;
state >>>= 1;

_doFallingAnimation = (state & 1) != 0;    // bit 16
state >>>= 1;

_doFlyingAnimation = (state & 1) != 0;     // bit 17
state >>>= 1;

isCeilingClimbing = (state & 1) != 0;      // bit 18
state >>>= 1;

isLevitating = (state & 1) != 0;           // bit 19
state >>>= 1;

isHeadJumping = (state & 1) != 0;          // bit 20
state >>>= 1;

isSliding = (state & 1) != 0;              // bit 21
state >>>= 1;

angleJumpType = (int)(state & 7);          // bits 22-24
state >>>= 3;

isFeetVineClimbing = (state & 1) != 0;     // bit 25
state >>>= 1;

isHandsVineClimbing = (state & 1) != 0;    // bit 26
state >>>= 1;

isClimbJumping = (state & 1) != 0;         // bit 27
state >>>= 1;

boolean wasClimbBackJumping = isClimbBackJumping;
isClimbBackJumping = (state & 1) != 0;     // bit 28
if(!wasClimbBackJumping && isClimbBackJumping)
    onStartClimbBackJump();
state >>>= 1;

isSlow = (state & 1) != 0;                 // bit 29
state >>>= 1;

isFast = (state & 1) != 0;                 // bit 30
state >>>= 1;

boolean wasWallJumping = isWallJumping;
isWallJumping = (state & 1) != 0;          // bit 31
if(!wasWallJumping && isWallJumping)
    onStartWallJump(null);
state >>>= 1;

isRopeSliding = (state & 1) != 0;          // bit 32
// (마지막 — shift 없음)
```

### 특수 처리 필드

**`isSmall` (bit 15):**
```java
boolean isSmall = (state & 1) != 0;
heightOffset = isSmall ? -1 : 0;
sp.height = 1.8F + heightOffset;
```
- `isSmall == true` → `heightOffset = -1`, `sp.height = 0.8F`
- `isSmall == false` → `heightOffset = 0`, `sp.height = 1.8F`
- `sp.height`를 직접 변경 — 다른 플레이어 hitbox 크기 갱신

**`isClimbBackJumping` (bit 28) — 전환 감지:**
```java
boolean wasClimbBackJumping = isClimbBackJumping;
isClimbBackJumping = (state & 1) != 0;
if(!wasClimbBackJumping && isClimbBackJumping)
    onStartClimbBackJump();
```
`false → true` 전환 시 `onStartClimbBackJump()` 호출 (렌더 rotateAngleY 갱신).

**`isWallJumping` (bit 31) — 전환 감지:**
```java
boolean wasWallJumping = isWallJumping;
isWallJumping = (state & 1) != 0;
if(!wasWallJumping && isWallJumping)
    onStartWallJump(null);
```
`false → true` 전환 시 `onStartWallJump(null)` 호출.  
`angle = null` — 다른 플레이어는 각도 정보 없음 → `rotateAngleY` 변경 안 함, `fallDistance = 0F`만 설정.

---

## abstract 구현

```java
@Override
public boolean isJumping()
{
    return _isJumping;
}

@Override
public boolean doFlyingAnimation()
{
    return _doFlyingAnimation;
}

@Override
public boolean doFallingAnimation()
{
    return _doFallingAnimation;
}
```

`SmartMoving`의 3개 abstract 메서드를 private backing 필드로 구현.  
`_isJumping`, `_doFlyingAnimation`, `_doFallingAnimation` — `processStatePacket`에서 설정.

---

## R-06 리서치 결과 추가 (A-13, A-14)

### A-13 — isCrawling State 패킷 비트 위치

**확인 방법**: SmartMovingOther.processStatePacket 역직렬화 순서 직접 계산 + SmartMovingSelf.addToSendQueue 인코딩 측 교차 확인

**결과**: `isCrawling` = **bit 13** (LSB 기준)

본 문서 "비트 레이아웃" 표에 이미 기록됨 (bit 13). 인코딩·디코딩 양쪽 코드에서 동일 위치 확인.

---

### A-14 — isSliding State 패킷 비트 위치

**확인 방법**: SmartMovingOther.processStatePacket 역직렬화 순서 직접 계산 + SmartMovingSelf.addToSendQueue 인코딩 측 교차 확인

**결과**: `isSliding` = **bit 21** (LSB 기준)

본 문서 "비트 레이아웃" 표에 이미 기록됨 (bit 21). 인코딩·디코딩 양쪽 코드에서 동일 위치 확인.

**추가 확인**: `isSliding`은 SmartMovingOther(다른 클라이언트 플레이어 표현)에서 디코딩됨. SmartMovingServer는 isSliding을 processStatePacket에서 추출하지 않음 — 서버 물리에 불필요(A-17 확인).

---

### 인코딩 측 — SmartMovingSelf.addToSendQueue()

**소스 위치**: `SmartMovingSelf.java` → `addToSendQueue()` 메서드 (클라이언트 전용, `sp.worldObj.isRemote` 조건)

**인코딩 코드 전체 (비트 조립)**:

```java
boolean isSmall = sp.height < 1;

long state = 0;
state |= isp.localIsSneaking() ? 1 : 0;    // → bit 33

state <<= 1; state |= isRopeSliding ? 1 : 0;      // → bit 32
state <<= 1; state |= isWallJumping ? 1 : 0;       // → bit 31
state <<= 1; state |= isFast ? 1 : 0;              // → bit 30
state <<= 1; state |= isSlow ? 1 : 0;              // → bit 29
state <<= 1; state |= isClimbBackJumping ? 1 : 0;  // → bit 28
state <<= 1; state |= isClimbJumping ? 1 : 0;      // → bit 27
state <<= 1; state |= isHandsVineClimbing ? 1 : 0; // → bit 26
state <<= 1; state |= isFeetVineClimbing ? 1 : 0;  // → bit 25
state <<= 3; state |= angleJumpType;                // → bits 22-24
state <<= 1; state |= isSliding ? 1 : 0;           // → bit 21
state <<= 1; state |= isHeadJumping ? 1 : 0;       // → bit 20
state <<= 1; state |= isLevitating ? 1 : 0;        // → bit 19
state <<= 1; state |= isCeilingClimbing ? 1 : 0;   // → bit 18
state <<= 1; state |= doFlyingAnimation() ? 1 : 0; // → bit 17
state <<= 1; state |= doFallingAnimation() ? 1 : 0;// → bit 16
state <<= 1; state |= isSmall ? 1 : 0;             // → bit 15
state <<= 1; state |= isClimbing ? 1 : 0;          // → bit 14
state <<= 1; state |= isCrawling ? 1 : 0;          // → bit 13
state <<= 1; state |= isCrawlClimbing ? 1 : 0;     // → bit 12
state <<= 1; state |= isSwimming ? 1 : 0;          // → bit 11
state <<= 1; state |= isDipping ? 1 : 0;           // → bit 10
state <<= 1; state |= isDiving ? 1 : 0;            // → bit 9
state <<= 1; state |= isp.getIsJumpingField() ? 1 : 0; // → bit 8
state <<= 4; state |= actualHandsClimbType;         // → bits 4-7
state <<= 4; state |= actualFeetClimbType;          // → bits 0-3
```

인코딩 결과는 `processStatePacket()`의 역직렬화 비트 순서와 완전히 일치함.

**전송 조건**:
```java
boolean sendStatePacket = state != prevPacketState;
// 또는 worldObj.playerEntities 목록 변경 시
if(sendStatePacket)
    SmartMovingPacketStream.sendState(SmartMovingComm.instance, sp.getEntityId(), state);
```
상태 변경이 없으면 전송하지 않음 (`prevPacketState`와 비교).

---

### 릴레이 경로 전체

```
SmartMovingSelf.addToSendQueue()
  → SmartMovingPacketStream.sendState(SmartMovingComm.instance, entityId, state)
  → [C→S 패킷]
  → 서버: SmartMovingServerComm (서버 측 수신)
       → SmartMovingServer.processStatePacket(state)
            (hitbox 조정 / floatKick 억제 / 낙하리셋 / crawlingCooldown / 소진 필터 — A-17 참조)
       → 다른 클라이언트들에게 릴레이 (S→C)
  → 다른 클라이언트: SmartMovingComm.processStatePacket(packet, player, entityId, state)
       → SmartMovingFactory.getOtherSmartMoving(entity)
       → SmartMovingOther.processStatePacket(state)  ← 이 파일
```

**isCrawling / isSliding의 릴레이 처리**:
- **isCrawling (bit 13)**: 서버가 `setCrawling()` 호출 (hitbox 갱신). 다른 클라이언트는 `SmartMovingOther.isCrawling` 에 설정 → 렌더링에 사용.
- **isSliding (bit 21)**: 서버는 추출하지 않음. 다른 클라이언트가 `SmartMovingOther.isSliding`에 설정 → 렌더링에만 사용.

---

## SmartMovingServer 비트 비교

`SmartMovingServer.processStatePacket`은 동일한 `long state`에서 일부 비트만 선택적 읽기:

| 비트 | SmartMovingOther 필드 | SmartMovingServer 확인 |
|------|----------------------|----------------------|
| 12 | `isCrawlClimbing` | ✓ `resetFallDistance`, `resetTicksForFloatKick` |
| 13 | `isCrawling` | ✓ `setCrawling()` |
| 14 | `isClimbing` | ✓ `resetFallDistance`, `resetTicksForFloatKick` |
| 15 | `isSmall` | ✓ `setSmall()` |
| 18 | `isCeilingClimbing` | ✓ `resetFallDistance`, `resetTicksForFloatKick` |
| 31 | `isWallJumping` | ✓ `resetFallDistance` |
| 33 | (SmartMovingOther 미읽음) | ✓ `isSneakButtonPressed` |

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMoving` | 상속 (상태 필드, `onStartClimbBackJump`, `onStartWallJump`, `spawnParticles`) |
| `EntityOtherPlayerMP` | 다른 클라이언트 플레이어 엔티티 (`sp.height` 직접 수정) |
| `SmartMovingFactory` | 생성 및 `foundAlive` 관리 |
| `SmartMovingComm` | `processStatePacket(long state)` 호출 |

---

## 주요 관찰 사항

1. **`isp = null`**: `super(sp, null)` — `SmartMovingBase`의 `esp = null` → 서버 측 입력 없음. 이 플레이어는 로컬 입력 없이 수신 상태만 표현.

2. **`sp.height` 직접 수정**: `isSmall`에 따라 `sp.height = 1.8F` or `0.8F`. SmartMovingSelf의 `setHeightOffset` 패턴과 달리 직접 대입.

3. **전환 감지 패턴**: `isClimbBackJumping`과 `isWallJumping`만 `wasXxx` 변수로 false→true 전환 감지 후 onStart 호출. 다른 상태는 전환 감지 없이 덮어씀.

4. **`onStartWallJump(null)`**: 다른 플레이어의 벽 점프 시작 시 `angle=null` 전달 → `SmartMoving.onStartWallJump`에서 `angle != null` 분기 건너뜀 → `rotateAngleY` 변경 안 함. `fallDistance = 0F`만 설정.

5. **비트 33(isSneakButtonPressed) 미포함**: SmartMovingOther는 bit 32(isRopeSliding)까지만 읽음. `isSneakButtonPressed`는 다른 플레이어 표현에 불필요.

6. **`foundAlive` 필드**: `SmartMovingFactory`의 mark-and-sweep 전용. 이 클래스에서 직접 수정하지 않고 Factory가 외부에서 조작.

7. **1.21.1 이식**: `EntityOtherPlayerMP` → `OtherClientPlayerEntity`. 비트 역직렬화 로직은 그대로 유지. `sp.height` 직접 수정은 1.21.1 boundingBox 시스템과 충돌 가능 — `EntityDimensions`/`Pose` 기반으로 재설계 필요.
