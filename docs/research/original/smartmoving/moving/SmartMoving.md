# SmartMoving.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMoving.java  
패키지: `net.smart.moving`  
종류: `abstract class`  
상속: `SmartMoving extends SmartMovingBase`

---

## 전체 소스

```java
package net.smart.moving;

import net.minecraft.block.*;
import net.minecraft.client.*;
import net.minecraft.client.particle.*;
import net.minecraft.entity.player.*;
import net.minecraft.util.*;

public abstract class SmartMoving extends SmartMovingBase
{
	public boolean isSlow;
	public boolean isFast;

	public boolean isClimbing;
	public boolean isHandsVineClimbing;
	public boolean isFeetVineClimbing;

	public boolean isClimbJumping;
	public boolean isClimbBackJumping;
	public boolean isWallJumping;
	public boolean isClimbCrawling;
	public boolean isCrawlClimbing;
	public boolean isCeilingClimbing;
	public boolean isRopeSliding;

	public boolean isDipping;
	public boolean isSwimming;
	public boolean isDiving;
	public boolean isLevitating;
	public boolean isHeadJumping;
	public boolean isCrawling;
	public boolean isSliding;
	public boolean isFlying;

	public int actualHandsClimbType;
	public int actualFeetClimbType;

	public int angleJumpType;

	public float heightOffset;

	private float spawnSlindingParticle;
	private float spawnSwimmingParticle;

	public SmartMoving(EntityPlayer sp, IEntityPlayerSP isp)
	{
		super(sp, isp);
	}

	public boolean isAngleJumping()
	{
		return angleJumpType > 1 && angleJumpType < 7;
	}

	public abstract boolean isJumping();

	public abstract boolean doFlyingAnimation();

	public abstract boolean doFallingAnimation();

	protected void spawnParticles(Minecraft minecraft, double playerMotionX, double playerMotionZ)
	{
		float horizontalSpeedSquare = 0;
		if(isSliding || isSwimming)
			horizontalSpeedSquare = (float)(playerMotionX * playerMotionX + playerMotionZ * playerMotionZ);

		if(isSliding)
		{
			int i = MathHelper.floor_double(sp.posX);
			int j = MathHelper.floor_double(sp.boundingBox.minY - 0.1F);
			int k = MathHelper.floor_double(sp.posZ);
			Block block = sp.worldObj.getBlock(i, j, k);
			if(block != null)
			{
				double posY = sp.boundingBox.minY + 0.1D;
				double motionX = -playerMotionX * 4D;
				double motionY = 1.5D;
				double motionZ = -playerMotionZ * 4D;

				spawnSlindingParticle += horizontalSpeedSquare;

				float maxSpawnSlindingParticle = Config._slideParticlePeriodFactor.value * 0.1F;
				while(spawnSlindingParticle > maxSpawnSlindingParticle)
				{
					double posX = sp.posX + getSpawnOffset();
					double posZ = sp.posZ + getSpawnOffset();
					int metaData = sp.worldObj.getBlockMetadata(i, j, k);
					sp.worldObj.spawnParticle("blockcrack_" + Block.getIdFromBlock(block) + "_" + metaData, posX, posY, posZ, motionX, motionY, motionZ);
					spawnSlindingParticle -= maxSpawnSlindingParticle;
				}
			}
		}

		if(isSwimming)
		{
			float posY = MathHelper.floor_double(sp.boundingBox.minY) + 1.0F;
			int i = (int)Math.floor(sp.posX);
			int j = (int)Math.floor(posY - 0.5);
			int k = (int)Math.floor(sp.posZ);

			Block block = sp.worldObj.getBlock(i, j, k);

			boolean isLava = block != null && isLava(block);
			spawnSwimmingParticle += horizontalSpeedSquare;

			float maxSpawnSwimmingParticle = (isLava ? Config._lavaSwimParticlePeriodFactor.value : Config._swimParticlePeriodFactor.value) * 0.01F;
			while(spawnSwimmingParticle > maxSpawnSwimmingParticle)
			{
				double posX = sp.posX + getSpawnOffset();
				double posZ = sp.posZ + getSpawnOffset();
				EntityFX splash = isLava ? new EntityLavaFX(sp.worldObj, posX, posY, posZ) : new EntitySplashFX(sp.worldObj, posX, posY, posZ, 0, 0, 0);
				splash.motionX = 0;
				splash.motionY = 0.2;
				splash.motionZ = 0;
				minecraft.effectRenderer.addEffect(splash);

				spawnSwimmingParticle -= maxSpawnSwimmingParticle;
			}
		}
	}

	private float getSpawnOffset()
	{
		return (sp.getRNG().nextFloat() - 0.5F) * 2F * sp.width;
	}

	protected void onStartClimbBackJump()
	{
		net.smart.render.SmartRenderRender.getPreviousRendererData(sp).rotateAngleY += isHeadJumping ? Half : Quarter;
		isClimbBackJumping = true;
	}

	protected void onStartWallJump(Float angle)
	{
		if (angle != null)
			net.smart.render.SmartRenderRender.getPreviousRendererData(sp).rotateAngleY = angle / RadiantToAngle;
		isWallJumping = true;
		sp.fallDistance = 0F;
	}
}
```

---

## 역할

`SmartMovingBase`를 상속하고, `SmartMovingSelf`(클라이언트)와 기타 구현체가 상속하는 **중간 추상 클래스**.

- 모든 이동 상태 boolean/int 필드를 여기서 선언 (SmartMovingBase/SmartMovingContext는 물리 유틸리티만 담당)
- 파티클 스폰 로직 (`spawnParticles`)
- 점프 시작 시 렌더 상태 변경 (`onStartClimbBackJump`, `onStartWallJump`)
- `isJumping()`, `doFlyingAnimation()`, `doFallingAnimation()` 세 메서드는 abstract — 구현체에 위임

---

## 상속 구조 (이 파일 기준)

```
SmartMovingContext
  └─ SmartMovingBase
       └─ SmartMoving          ← 이 파일
            └─ SmartMovingSelf (implements ISmartMovingSelf)
```

---

## import

```java
import net.minecraft.block.*;         // Block, MathHelper
import net.minecraft.client.*;        // Minecraft
import net.minecraft.client.particle.*; // EntityFX, EntityLavaFX, EntitySplashFX
import net.minecraft.entity.player.*; // EntityPlayer
import net.minecraft.util.*;           // MathHelper (중복 가능)
```

SmartRender는 FQN으로 직접 참조:
- `net.smart.render.SmartRenderRender.getPreviousRendererData(sp).rotateAngleY`

---

## 상태 필드

### 속도 계열

```java
public boolean isSlow;   // 느리게 이동 중
public boolean isFast;   // 빠르게 이동 중
```

### 클라이밍 계열

```java
public boolean isClimbing;          // 일반 클라이밍
public boolean isHandsVineClimbing; // 손으로 덩굴 클라이밍
public boolean isFeetVineClimbing;  // 발로 덩굴 클라이밍
public boolean isClimbJumping;      // 클라이밍 중 점프
public boolean isClimbBackJumping;  // 클라이밍 중 뒤로 점프
public boolean isWallJumping;       // 벽 점프
public boolean isClimbCrawling;     // 클라이밍 + 크롤링
public boolean isCrawlClimbing;     // 크롤 클라이밍
public boolean isCeilingClimbing;   // 천장 클라이밍
public boolean isRopeSliding;       // 로프 슬라이딩
```

### 수영/잠수 계열

```java
public boolean isDipping;    // 수면에 살짝 잠김
public boolean isSwimming;   // 수영 중
public boolean isDiving;     // 잠수 중
public boolean isLevitating; // 부양 (물 위)
```

### 기타 동작

```java
public boolean isHeadJumping; // 헤드 점프
public boolean isCrawling;    // 크롤링
public boolean isSliding;     // 슬라이딩
public boolean isFlying;      // 비행
```

### int 상태

```java
public int actualHandsClimbType; // 손 클라이밍 타입 (4비트 — SmartMovingSelf addToSendQueue에서 확인)
public int actualFeetClimbType;  // 발 클라이밍 타입 (4비트)
public int angleJumpType;        // 각도 점프 타입 (3비트 — 1~6 범위 중 2~6이 angleJumping)
```

### 높이 오프셋

```java
public float heightOffset;
```

SmartMovingSelf에서 `setHeightOffset(-1F)` 패턴으로 사용:
- `boundingBox.minY += heightOffset * -1` → 실제로 minY가 1 증가
- `height += heightOffset * -1` → height 1 증가
크롤링/수영/헤드점프 시 적용.

### 파티클 누적 카운터

```java
private float spawnSlindingParticle;  // 슬라이딩 파티클 누적 (오타: Slinding)
private float spawnSwimmingParticle;  // 수영 파티클 누적
```

---

## 생성자

```java
public SmartMoving(EntityPlayer sp, IEntityPlayerSP isp)
{
    super(sp, isp);
}
```

`SmartMovingBase(EntityPlayer, IEntityPlayerSP)` 호출.  
`sp`, `isp`는 SmartMovingBase/SmartMovingContext에서 보관.

---

## 메서드

### `isAngleJumping()` → boolean

```java
public boolean isAngleJumping()
{
    return angleJumpType > 1 && angleJumpType < 7;
}
```

`angleJumpType` 값 2~6이면 각도 점프 중.  
`angleJumpType`은 3비트 필드(값 범위 0~7).  
- 0, 1: 각도 점프 아님
- 2~6: 각도 점프 (방향별 분류 추정)
- 7: 각도 점프 아님

---

### `abstract boolean isJumping()`

구현체(`SmartMovingSelf`)에서 점프 여부 반환.

---

### `abstract boolean doFlyingAnimation()`

구현체에서 비행 애니메이션 여부 반환.

---

### `abstract boolean doFallingAnimation()`

구현체에서 낙하 애니메이션 여부 반환.

---

### `spawnParticles(Minecraft minecraft, double playerMotionX, double playerMotionZ)`

슬라이딩/수영 파티클 스폰 처리.

**공통 전처리:**
```java
float horizontalSpeedSquare = 0;
if(isSliding || isSwimming)
    horizontalSpeedSquare = (float)(playerMotionX * playerMotionX + playerMotionZ * playerMotionZ);
```
수평 속도 제곱 — 두 상태 중 하나라도 활성이면 계산.

---

**슬라이딩 파티클:**
```java
if(isSliding)
{
    int i = MathHelper.floor_double(sp.posX);
    int j = MathHelper.floor_double(sp.boundingBox.minY - 0.1F); // 발 아래 블록
    int k = MathHelper.floor_double(sp.posZ);
    Block block = sp.worldObj.getBlock(i, j, k);
    if(block != null)
    {
        double posY = sp.boundingBox.minY + 0.1D;
        double motionX = -playerMotionX * 4D;  // 이동 반대 방향
        double motionY = 1.5D;
        double motionZ = -playerMotionZ * 4D;  // 이동 반대 방향

        spawnSlindingParticle += horizontalSpeedSquare;

        float maxSpawnSlindingParticle = Config._slideParticlePeriodFactor.value * 0.1F;
        while(spawnSlindingParticle > maxSpawnSlindingParticle)
        {
            double posX = sp.posX + getSpawnOffset();
            double posZ = sp.posZ + getSpawnOffset();
            int metaData = sp.worldObj.getBlockMetadata(i, j, k);
            sp.worldObj.spawnParticle("blockcrack_" + Block.getIdFromBlock(block) + "_" + metaData, ...);
            spawnSlindingParticle -= maxSpawnSlindingParticle;
        }
    }
}
```

| 항목 | 값 |
|------|----|
| 발 아래 블록 Y | `floor(boundingBox.minY - 0.1F)` |
| 파티클 종류 | `"blockcrack_<blockId>_<meta>"` (블록 부수는 효과) |
| 파티클 위치 Y | `boundingBox.minY + 0.1D` |
| 파티클 속도 X/Z | 이동 방향 반대 × 4 |
| 파티클 속도 Y | 1.5D (위로) |
| 스폰 조건 | `spawnSlindingParticle > Config._slideParticlePeriodFactor.value * 0.1F` |
| 누적 방식 | `spawnSlindingParticle += horizontalSpeedSquare`, 스폰할 때마다 threshold 차감 |

---

**수영 파티클:**
```java
if(isSwimming)
{
    float posY = MathHelper.floor_double(sp.boundingBox.minY) + 1.0F;
    int i = (int)Math.floor(sp.posX);
    int j = (int)Math.floor(posY - 0.5);  // posY - 0.5 블록 위치
    int k = (int)Math.floor(sp.posZ);

    Block block = sp.worldObj.getBlock(i, j, k);
    boolean isLava = block != null && isLava(block);
    spawnSwimmingParticle += horizontalSpeedSquare;

    float maxSpawnSwimmingParticle = (isLava ? Config._lavaSwimParticlePeriodFactor.value
                                             : Config._swimParticlePeriodFactor.value) * 0.01F;
    while(spawnSwimmingParticle > maxSpawnSwimmingParticle)
    {
        double posX = sp.posX + getSpawnOffset();
        double posZ = sp.posZ + getSpawnOffset();
        EntityFX splash = isLava
            ? new EntityLavaFX(sp.worldObj, posX, posY, posZ)
            : new EntitySplashFX(sp.worldObj, posX, posY, posZ, 0, 0, 0);
        splash.motionX = 0;
        splash.motionY = 0.2;
        splash.motionZ = 0;
        minecraft.effectRenderer.addEffect(splash);
        spawnSwimmingParticle -= maxSpawnSwimmingParticle;
    }
}
```

| 항목 | 값 |
|------|----|
| 파티클 Y 위치 | `floor(boundingBox.minY) + 1.0F` (수면 근처) |
| 블록 확인 Y | `floor(posY - 0.5)` |
| 물 파티클 | `EntitySplashFX` (motionX/Z=0, motionY=0.2D) |
| 용암 파티클 | `EntityLavaFX` (motionX/Z=0, motionY=0.2D) |
| 판별 | `isLava(block)` — `SmartMovingBase`에 정의된 메서드 |
| 스폰 threshold | `Config._lavaSwimParticlePeriodFactor.value * 0.01F` 또는 `Config._swimParticlePeriodFactor.value * 0.01F` |
| splash 초기 속도 | motionX=0, motionY=0.2, motionZ=0 (수직 상승) |

---

### `getSpawnOffset()` → float (private)

```java
private float getSpawnOffset()
{
    return (sp.getRNG().nextFloat() - 0.5F) * 2F * sp.width;
}
```

플레이어 너비 범위 내 랜덤 오프셋.  
범위: `[-sp.width, sp.width]` (플레이어 width = 0.6F → `[-0.6, 0.6]`)

---

### `onStartClimbBackJump()`

```java
protected void onStartClimbBackJump()
{
    net.smart.render.SmartRenderRender.getPreviousRendererData(sp).rotateAngleY += isHeadJumping ? Half : Quarter;
    isClimbBackJumping = true;
}
```

클라이밍 중 뒤로 점프 시작 시 호출.

| 조건 | rotateAngleY 증분 |
|------|-------------------|
| `isHeadJumping == true` | `Half` (= π, 180°) |
| `isHeadJumping == false` | `Quarter` (= π/2, 90°) |

`Half`, `Quarter`는 `SmartMovingContext` 또는 상위 클래스에서 정의된 상수 (라디안 단위).  
`SmartRenderRender.getPreviousRendererData(sp)`: SmartRender 렌더러에서 이전 프레임 데이터 가져옴 → `rotateAngleY` 직접 수정.

---

### `onStartWallJump(Float angle)`

```java
protected void onStartWallJump(Float angle)
{
    if (angle != null)
        net.smart.render.SmartRenderRender.getPreviousRendererData(sp).rotateAngleY = angle / RadiantToAngle;
    isWallJumping = true;
    sp.fallDistance = 0F;
}
```

벽 점프 시작 시 호출.

| 항목 | 동작 |
|------|------|
| `angle != null` | `rotateAngleY = angle / RadiantToAngle` (각도 → 라디안 변환) |
| `angle == null` | rotateAngleY 변경 없음 |
| 항상 | `isWallJumping = true` |
| 항상 | `sp.fallDistance = 0F` (낙하 거리 리셋 — 낙하 데미지 방지) |

`RadiantToAngle`은 SmartMovingContext의 상수 (`180 / Math.PI` 추정 — 라디안을 도로 변환할 때 곱하는 값, 여기서는 나눔으로 도→라디안).

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingBase` | 상속 (물리 유틸리티, `sp`, `isp`, `isLava()` 등) |
| `SmartMovingContext` | 간접 상속 (상수 `Half`, `Quarter`, `RadiantToAngle` 등) |
| `SmartMovingSelf` | 이 클래스를 상속하는 구체 구현체 |
| `net.smart.render.SmartRenderRender` | FQN 직접 참조, `getPreviousRendererData(sp)` |
| `Config` | `_slideParticlePeriodFactor`, `_swimParticlePeriodFactor`, `_lavaSwimParticlePeriodFactor` |
| `Minecraft` | `effectRenderer.addEffect()` |
| `EntityFX`, `EntityLavaFX`, `EntitySplashFX` | 파티클 객체 |
| `MathHelper` | `floor_double()` |

---

## 주요 관찰 사항

1. **오타 `spawnSlindingParticle`**: 필드명에 `Slinding` (Sliding 오타). 원본에서도 동일.

2. **파티클 누적 방식**: `spawnXxxParticle += horizontalSpeedSquare` → threshold 초과 시마다 while로 연속 스폰 후 차감. 속도가 빠를수록 더 많은 파티클 스폰.

3. **SmartRender FQN 직접 참조**: SmartRender 의존성을 import 없이 FQN으로 처리. SmartRender 없이도 컴파일 가능하도록 의도한 것으로 보임 (실제론 런타임 의존).

4. **`sp.fallDistance = 0F`**: 벽 점프 시작 시 낙하 거리 리셋 → 착지 시 데미지 없음.

5. **abstract 3개**: `isJumping()`, `doFlyingAnimation()`, `doFallingAnimation()` — 실제 구현은 `SmartMovingSelf`. 서버 측 구현체도 별도로 존재할 수 있음.

6. **isSmall 미포함**: 이 클래스에는 `isSmall` 필드 없음 — `SmartMovingContext` 또는 `SmartMovingBase`에서 관리.

7. **1.21.1 이식**: boolean/int 상태 필드들은 그대로 이식 가능. `spawnParticles`는 1.21.1 파티클 API로 교체 필요 (`EntityFX` → `ParticleEffect` 계열). SmartRenderRender 참조는 렌더 시스템 재설계 필요.
