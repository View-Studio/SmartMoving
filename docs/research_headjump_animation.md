# 헤드점프 애니메이션 — 원본 1.7.10 / 1.12.2 1:1 전수 리서치

> **목적**: 원본 SmartMoving 1.7.10 (Forge) + SmartMovingReboot 1.12.2 의 *헤드점프 애니메이션* 관련 모든 라인을 누락없이 추출. 1.21.1 Fabric 매핑 시 1:1 번역 기준 문서.
>
> **수집 일자**: 2026-05-11
>
> **요약**: 원본 1.7.10 와 1.12.2 의 헤드점프 애니메이션 *식 자체는 완전히 동일*. 유일한 API 차이는 1.12.2 의 Material 접근 경로 (`getMaterial()` → `getBlockState().getBaseState().getMaterial()`).

---

## 0. 원본 파일 경로

### 1.7.10
| 항목 | 경로 |
|------|------|
| 모델 본체 (회전식) | `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\render\SmartMovingModel.java` |
| 렌더러 (상태 전달) | `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\render\SmartMovingRender.java` |
| 거리 측정 헬퍼 | `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingBase.java` |
| 각도 통계 (Vertical/Horizontal) | `C:\Work\minecraft\porting\sm_original\SmartRender\src\main\java\net\smart\render\SmartRenderRender.java` |
| 상수 정의 | `C:\Work\minecraft\porting\sm_original\SmartRender\src\main\java\net\smart\render\SmartRenderUtilities.java` |
| Fade lerp | `C:\Work\minecraft\porting\sm_original\SmartRender\src\main\java\net\smart\render\ModelRotationRenderer.java` |

### 1.12.2 (SmartMovingReboot)
| 항목 | 경로 |
|------|------|
| 모델 본체 | `C:\Work\minecraft\porting\sm_porting_1_12_2\SmartMovingReboot\src\main\java\net\smart\moving\model\SMModel.java` |
| 렌더러 | `C:\Work\minecraft\porting\sm_porting_1_12_2\SmartMovingReboot\src\main\java\net\smart\moving\render\SMRender.java` |
| 각도 통계 | `C:\Work\minecraft\porting\sm_porting_1_12_2\SmartMovingReboot\SmartRender\src\main\java\net\smart\render\render\SRRenderer.java` |
| 상수 | `SRUtilities` (1.7.10 `SmartRenderUtilities` 와 동일) |

---

## 1. 헤드점프 분기 본체 — 회전식 (1.7.10)

**파일**: `SmartMovingModel.java`
**메서드**: `private void setRotationAngles(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)`
**분기**: `else if(isHeadJump)` — L505-530.

```java
505:		else if(isHeadJump)
506:		{
507:			bipedOuter.fadeRotateAngleX = true;
508:			bipedOuter.rotateAngleX = (Quarter - currentVerticalAngle);
509:			bipedOuter.rotateAngleY = currentHorizontalAngle;
510:
511:			bipedHead.rotateAngleX = -bipedOuter.rotateAngleX / 2F;
512:
513:			float bendFactor = Math.min(Factor(currentVerticalAngle, Quarter, 0), Factor(currentVerticalAngle, -Quarter, 0));
514:			bipedRightArm.rotateAngleX = bendFactor * -Eighth;
515:			bipedLeftArm.rotateAngleX = bendFactor * -Eighth;
516:
517:			bipedRightLeg.rotateAngleX = bendFactor * -Eighth;
518:			bipedLeftLeg.rotateAngleX = bendFactor * -Eighth;
519:
520:			float armFactorZ = Factor(currentVerticalAngle, Quarter, -Quarter);
521:			if(overGroundBlock != null && overGroundBlock.getMaterial().isSolid())
522:				armFactorZ = Math.min(armFactorZ, smallOverGroundHeight / 5F);
523:
524:			bipedRightArm.rotateAngleZ = Half - Sixteenth + armFactorZ * Eighth;
525:			bipedLeftArm.rotateAngleZ = Sixteenth - Half - armFactorZ * Eighth;
526:
527:			float legFactorZ = Factor(currentVerticalAngle, -Quarter, Quarter);
528:			bipedRightLeg.rotateAngleZ = Sixtyfourth * legFactorZ;
529:			bipedLeftLeg.rotateAngleZ = -Sixtyfourth * legFactorZ;
530:		}
```

### 라인별 의미

| 라인 | 코드 | 의미 |
|------|------|------|
| 507 | `bipedOuter.fadeRotateAngleX = true;` | 다음 프레임으로 X 회전을 *fade lerp* (5-tick 시정수). Y 회전은 fade 안 함. |
| 508 | `bipedOuter.rotateAngleX = (Quarter - currentVerticalAngle);` | 몸 전체 X 기울기 = π/2 − 수직각. 상승 시 < π/2, 수평 시 = π/2, 하강 시 > π/2 (다이브). |
| 509 | `bipedOuter.rotateAngleY = currentHorizontalAngle;` | 몸 전체 Y 회전 = 이동 방향 (마우스 yaw 아님). |
| 511 | `bipedHead.rotateAngleX = -bipedOuter.rotateAngleX / 2F;` | 머리는 몸 기울기 *역방향 절반* (= θ/2). 몸이 다이브 자세일 때 머리는 정면 약간 위. |
| 513 | `bendFactor = Math.min(Factor(currentVerticalAngle, Quarter, 0), Factor(currentVerticalAngle, -Quarter, 0));` | 수직각=0 (수평 다이브) 시 1, ±π/2 (수직 상승/하강) 시 0. **두 Factor 의 min**. |
| 514-515 | `bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = bendFactor * -Eighth;` | 양팔 X = -π/4 × bendFactor (= 0 ~ -45°). 수평 다이브에서 팔이 약간 굽혀짐. |
| 517-518 | `bipedRightLeg.rotateAngleX = bipedLeftLeg.rotateAngleX = bendFactor * -Eighth;` | 양다리 X 도 동일하게 굽힘. |
| 520 | `armFactorZ = Factor(currentVerticalAngle, Quarter, -Quarter);` | 수직각=π/2 (수직 상승) 시 0, -π/2 (수직 하강) 시 1, 0 (수평) 시 0.5. **상승→하강 0→1 선형**. |
| 521-522 | `if(overGroundBlock != null && overGroundBlock.getMaterial().isSolid()) armFactorZ = Math.min(armFactorZ, smallOverGroundHeight / 5F);` | 머리 아래 5블록 내에 *솔리드* 블록 있으면 armFactorZ 를 거리/5 로 clamp. **착지 직전 팔 모음 효과**. |
| 524 | `bipedRightArm.rotateAngleZ = Half - Sixteenth + armFactorZ * Eighth;` | 오른팔 Z = π - π/8 + π/4 × factor = 7π/8 ~ 9π/8 (157.5° ~ 202.5°). 머리 앞쪽으로 팔 모음. |
| 525 | `bipedLeftArm.rotateAngleZ = Sixteenth - Half - armFactorZ * Eighth;` | 왼팔 Z = -7π/8 ~ -9π/8 (오른팔 대칭). |
| 527 | `legFactorZ = Factor(currentVerticalAngle, -Quarter, Quarter);` | 수직각=-π/2 (수직 하강) 시 0, π/2 (수직 상승) 시 1, 0 (수평) 시 0.5. **하강→상승 0→1 선형** (armFactorZ 와 *반대 방향*). |
| 528-529 | `bipedRightLeg.rotateAngleZ = Sixtyfourth * legFactorZ; bipedLeftLeg.rotateAngleZ = -Sixtyfourth * legFactorZ;` | 다리 Z = ±π/32 × factor (= 0 ~ ±5.625°). 상승 시 다리 약간 벌어짐. |

---

## 2. 헤드점프 분기 본체 — 1.12.2

**파일**: `SMModel.java` L544-568

```java
544:			} else if (isHeadJump) {
545:				bipedOuter.fadeRotateAngleX = true;
546:				bipedOuter.rotateAngleX = (Quarter - currentVerticalAngle);
547:				bipedOuter.rotateAngleY = currentHorizontalAngle;
548:
549:				bipedHead.rotateAngleX = -bipedOuter.rotateAngleX / 2F;
550:
551:				float bendFactor = Math.min(Factor(currentVerticalAngle, Quarter, 0),
552:						Factor(currentVerticalAngle, -Quarter, 0));
553:				bipedRightArm.rotateAngleX = bendFactor * -Eighth;
554:				bipedLeftArm.rotateAngleX = bendFactor * -Eighth;
555:
556:				bipedRightLeg.rotateAngleX = bendFactor * -Eighth;
557:				bipedLeftLeg.rotateAngleX = bendFactor * -Eighth;
558:
559:				float armFactorZ = Factor(currentVerticalAngle, Quarter, -Quarter);
560:				if (overGroundBlock != null && overGroundBlock.getBlockState().getBaseState().getMaterial().isSolid())
561:					armFactorZ = Math.min(armFactorZ, smallOverGroundHeight / 5F);
562:
563:				bipedRightArm.rotateAngleZ = Half - Sixteenth + armFactorZ * Eighth;
564:				bipedLeftArm.rotateAngleZ = Sixteenth - Half - armFactorZ * Eighth;
565:
566:				float legFactorZ = Factor(currentVerticalAngle, -Quarter, Quarter);
567:				bipedRightLeg.rotateAngleZ = Sixtyfourth * legFactorZ;
568:				bipedLeftLeg.rotateAngleZ = -Sixtyfourth * legFactorZ;
569:			} else if (isFalling) {
```

### 1.7.10 vs 1.12.2 차이

| 항목 | 1.7.10 | 1.12.2 |
|------|--------|--------|
| 회전식 라인 수 | 26 라인 (L505-530) | 26 라인 (L544-568) |
| 식 자체 | 완전 동일 | 완전 동일 |
| Material 접근 (L521 vs L560) | `overGroundBlock.getMaterial().isSolid()` | `overGroundBlock.getBlockState().getBaseState().getMaterial().isSolid()` |
| 상수 import | `SmartRenderContext` (extends) | `SRUtilities` static import |
| Block 타입 | `net.minecraft.block.Block` | `net.minecraft.block.Block` |
| ModelRotationRenderer | `net.smart.render.ModelRotationRenderer` | `net.smart.render.model.SRModelRotationRenderer` |
| 모델 부모 | `SmartRenderContext` (extends) | (extends 없음, static method 호출) |

**결론**: 헤드점프 애니메이션 *수식*은 1:1 동일. 1.12.2 는 단순 API 마이그레이션 (Material 한 곳).

---

## 3. 입력 상태 전달 — SmartMovingRender (1.7.10)

**파일**: `SmartMovingRender.java::renderPlayer`

### `isHeadJump` 진입 조건 (L78)
```java
78:	boolean isHeadJump = moving.isHeadJumping;
```

### `smallOverGroundHeight` / `overGroundBlock` 계산 (L89-90)
```java
89:	float smallOverGroundHeight = isCrawlClimb || isHeadJump ? (float)moving.getOverGroundHeight(5D) : 0F;
90:	Block overGroundBlock = isHeadJump && smallOverGroundHeight < 5F ? moving.getOverGroundBlockId(smallOverGroundHeight) : null;
```

- `getOverGroundHeight(5D)` — 최대 5m 까지 발 아래 지면 거리 측정 (`SmartMovingBase.java::getOverGroundHeight`).
- `getOverGroundBlockId(smallOverGroundHeight)` — 그 거리 안에 *첫 번째 non-null 블록* 반환 (`SmartMovingBase.java::getOverGroundBlockId`). **isHeadJump 일 때만 호출** 그리고 거리 < 5m 일 때만.

### 모델로 전달 (L96-122)
```java
96:	SmartMovingModel modelPlayer = modelPlayers[i].getMovingModel();
...
109:	modelPlayer.isHeadJump = isHeadJump;
...
119:	modelPlayer.currentHorizontalSpeedFlattened = currentHorizontalSpeedFlattened;
120:	modelPlayer.smallOverGroundHeight = smallOverGroundHeight;
121:	modelPlayer.overGroundBlock = overGroundBlock;
```

### renderYawOffset force (L147-148)
```java
147:	if(moving.isClimbing || moving.isClimbCrawling || moving.isCrawlClimbing || moving.isFlying || moving.isSwimming || moving.isDiving || moving.isCeilingClimbing || moving.isHeadJumping || moving.isSliding || moving.isAngleJumping())
148:		entityplayer.renderYawOffset = forwardRotation;
```

- `forwardRotation = entityplayer.prevRotationYaw + (entityplayer.rotationYaw - entityplayer.prevRotationYaw) * f2` (L145).
- isHeadJumping 포함된 모든 SM 분기는 `renderYawOffset` 을 *마우스 yaw lerp* 로 강제 → vanilla `bodyYaw lerp` 무력화 → setRotationAngles 에서 `bipedOuter.rotateAngleY = currentHorizontalAngle` 직접 적용 → **모델 yaw = 이동 방향 (마우스 무관)**.

---

## 3-1. 1.12.2 SMRender 동일 부분

**파일**: `SMRender.java::doRender` (L60-141)
- L80: `boolean isHeadJump = moving.isHeadJumping;` (1.7.10 과 동일).
- L93-96:
  ```java
  93:	float smallOverGroundHeight = isCrawlClimb || isHeadJump ? (float) moving.getOverGroundHeight(5D) : 0F;
  94:	Block overGroundBlock = isHeadJump && smallOverGroundHeight < 5F
  95:			? moving.getOverGroundBlockId(smallOverGroundHeight)
  96:			: null;
  ```
- L114: `modelPlayer.isHeadJump = isHeadJump;`
- L125-126: `modelPlayer.smallOverGroundHeight = smallOverGroundHeight; modelPlayer.overGroundBlock = overGroundBlock;`
- L152-155: renderYawOffset force 동일.

**완전히 1:1 동일** (라인 번호만 다름).

---

## 4. 각도 통계 — `currentVerticalAngle` / `currentHorizontalAngle` 계산

**파일 (1.7.10)**: `SmartRenderRender.java::renderPlayer`

```java
67:	if (!isInventory)
68:	{
69:		double xDiff = entityplayer.posX - entityplayer.prevPosX;
70:		double yDiff = entityplayer.posY - entityplayer.prevPosY;
71:		double zDiff = entityplayer.posZ - entityplayer.prevPosZ;
72:
73:		verticalDistance = Math.abs(yDiff);
74:		horizontalDistance = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
75:		distance = Math.sqrt(horizontalDistance * horizontalDistance + verticalDistance * verticalDistance);
76:
77:		currentCameraAngle = entityplayer.rotationYaw / RadiantToAngle;
78:		currentVerticalAngle = (float)Math.atan(yDiff / horizontalDistance);
79:		if(Float.isNaN(currentVerticalAngle))
80:			currentVerticalAngle = Quarter;
81:
82:		currentHorizontalAngle = (float)-Math.atan(xDiff / zDiff);
83:		if (Float.isNaN(currentHorizontalAngle))
84:			if(Float.isNaN(statistics.prevHorizontalAngle))
85:				currentHorizontalAngle = currentCameraAngle;
86:			else
87:				currentHorizontalAngle = statistics.prevHorizontalAngle;
88:		else if (zDiff < 0)
89:			currentHorizontalAngle += Half;
90:
91:	  statistics.prevHorizontalAngle = currentHorizontalAngle;
92:	}
```

### 의미
- **xDiff / yDiff / zDiff** = 현재 tick 위치 - 이전 tick 위치 (= velocity / per-tick motion).
- **horizontalDistance** = √(xDiff² + zDiff²) = 수평 이동 거리.
- **verticalDistance** = |yDiff| = 수직 이동 거리 절대값.

- **currentVerticalAngle = atan(yDiff / horizontalDistance)** (라디안)
  - 상승 (yDiff > 0): 양수, 최대 π/2 (수직 상승, horizontalDistance=0).
  - 수평 (yDiff = 0): 0.
  - 하강 (yDiff < 0): 음수, 최저 -π/2 (수직 하강).
  - **NaN 가드**: horizontalDistance=0 + yDiff=0 → atan(0/0)=NaN → `Quarter` (π/2) 로 대체.

- **currentHorizontalAngle = -atan(xDiff / zDiff)** (라디안)
  - 이동 방향 yaw. Minecraft 좌표계 기준 (북쪽=-Z, 동쪽=+X) → `-atan(x/z)`.
  - NaN (zDiff=0 + xDiff=0 = 정지): 이전 프레임 값 유지, 없으면 카메라 방향 사용.
  - `zDiff < 0` (북향): `+ Half` (= π) 보정 — atan range [-π/2, π/2] 을 [-π, π] 로 확장.

### 1.12.2 동일성
**파일**: `SRRenderer.java` L75-95
- `xDiff/yDiff/zDiff` 계산 동일 (L75-77).
- `currentVerticalAngle = (float) Math.atan(yDiff / horizontalDistance)` (L84) 동일.
- `Quarter` NaN 가드 동일 (L85-86, `SRUtilities.Quarter`).
- `currentHorizontalAngle = (float) -Math.atan(xDiff / zDiff)` (L88) 동일.
- `Half` 보정 동일 (L94-95).

**완전히 1:1 동일** (라인 번호와 import 만 다름).

---

## 5. `getOverGroundHeight` / `getOverGroundBlockId` 헬퍼

**파일 (1.7.10)**: `SmartMovingBase.java`

```java
855:	public double getOverGroundHeight(double maximum)
856:	{
857:		if(esp != null)
858:			return (sp.boundingBox.minY - getMaxPlayerSolidBetween(sp.boundingBox.minY - maximum, sp.boundingBox.minY, 0));
859:		return (sp.boundingBox.minY + 1D - getMaxPlayerSolidBetween(sp.boundingBox.minY - maximum + 1D, sp.boundingBox.minY + 1D, 0.1));
860:	}
861:
862:	public Block getOverGroundBlockId(double distance)
863:	{
864:		int x = MathHelper.floor_double(sp.posX);
865:		int y = MathHelper.floor_double(sp.boundingBox.minY);
866:		int z = MathHelper.floor_double(sp.posZ);
867:		int minY = y - (int)Math.ceil(distance);
868:
869:		if(esp == null)
870:		{
871:			y++;
872:			minY++;
873:		}
874:
875:		for(; y >= minY; y--)
876:		{
877:			Block block = sp.worldObj.getBlock(x, y, z);
878:			if(block != null)
879:				return block;
880:		}
881:		return null;
882:	}
```

### 의미
- **getOverGroundHeight(maximum)**: 발 (boundingBox.minY) 아래 *최대 maximum 거리* 까지 SOLID 면이 있는지 검사. 거리 반환 (0 ~ maximum).
  - `esp != null` (= self entity player): minY - solidTop.
  - `esp == null` (= remote 등): +1m 오프셋 (서로 다른 박스 위치 차이 보정 — `feedback_box_position_diff_breaks_grip.md` 참조).

- **getOverGroundBlockId(distance)**: 발 아래 `distance` 만큼 down-scan, 첫 non-null 블록 반환.
  - `esp == null` 일 때 y/minY +1 보정 (위와 동일한 이유).

### 1.12.2 동등 헬퍼
**파일**: `SMBase.java`
- 메서드 시그니처 동일.
- 위 1.7.10 의 `worldObj.getBlock(x, y, z)` → 1.12.2 `world.getBlockState(new BlockPos(x, y, z)).getBlock()` 등으로 변환 (1.12.2 BlockPos API).
- *의미는 1:1 동일*.

---

## 6. 상수 정의 (SmartRender 유틸)

**파일**: `SmartRenderUtilities.java` (1.7.10), `SRUtilities.java` (1.12.2)

```java
22:	public static final float Whole       = (float)Math.PI * 2F;        // 2π   ≈ 6.2832 rad = 360°
23:	public static final float Half        = (float)Math.PI;             // π    ≈ 3.1416 rad = 180°
24:	public static final float Quarter     = Half / 2F;                  // π/2  ≈ 1.5708 rad =  90°
25:	public static final float Eighth      = Quarter / 2F;               // π/4  ≈ 0.7854 rad =  45°
26:	public static final float Sixteenth   = Eighth / 2F;                // π/8  ≈ 0.3927 rad =  22.5°
27:	public static final float Thirtytwoth = Sixteenth / 2F;             // π/16 ≈ 0.1963 rad =  11.25°
28:	public static final float Sixtyfourth = Thirtytwoth / 2F;           // π/32 ≈ 0.0982 rad =   5.625°
30:	public static final float RadiantToAngle = 360F / Whole;            // 360/(2π) ≈ 57.2958 (rad → deg)
```

### 헤드점프 사용 상수
- `Quarter` (π/2): 수직 다이브 기준각, Factor 경계.
- `Half` (π): 팔 Z 기본값 (`Half - Sixteenth`).
- `Eighth` (π/4): 굽힘각 (`bendFactor * -Eighth`), 팔 Z 가산.
- `Sixteenth` (π/8): 팔 Z 보정.
- `Sixtyfourth` (π/32): 다리 Z 벌어짐 (작은 각).

---

## 7. `Factor` 헬퍼 (SmartMovingModel L729-747, 1.7.10)

```java
729:	private static float Factor(float x, float x0, float x1)
730:	{
731:		if(x0 > x1)
732:		{
733:			if(x <= x1)
734:				return 1F;
735:			if(x >= x0)
736:				return 0F;
737:			return (x0 - x) / (x0 - x1);
738:		}
739:		else
740:		{
741:			if(x >= x1)
742:				return 1F;
743:			if(x <= x0)
744:				return 0F;
745:			return (x - x0) / (x1 - x0);
746:		}
747:	}
```

### 의미
- **선형 보간 + clamp**: x 를 [x0, x1] 구간에서 [0, 1] 로 매핑.
- `x0 < x1` (정방향): x ≤ x0 → 0, x ≥ x1 → 1, 그 사이는 선형.
- `x0 > x1` (역방향): x ≥ x0 → 0, x ≤ x1 → 1, 그 사이는 선형.

### 헤드점프 사용
| 호출 | 결과 |
|------|------|
| `Factor(currentVerticalAngle, Quarter, 0)` | angle=π/2 → 0, angle=0 → 1, 그 사이 선형 (역방향). |
| `Factor(currentVerticalAngle, -Quarter, 0)` | angle=-π/2 → 0, angle=0 → 1, 그 사이 선형 (정방향). |
| **bendFactor = min(위 두 개)** | angle=0 (수평) → 1 (최대 굽힘), angle=±π/2 (수직) → 0 (안 굽힘). 삼각형 모양. |
| `Factor(currentVerticalAngle, Quarter, -Quarter)` (armFactorZ) | angle=π/2 → 0, angle=-π/2 → 1 (역방향). **상승=0, 하강=1**. |
| `Factor(currentVerticalAngle, -Quarter, Quarter)` (legFactorZ) | angle=-π/2 → 0, angle=π/2 → 1 (정방향). **하강=0, 상승=1**. |

---

## 8. Fade lerp 메커니즘 (`fadeRotateAngleX = true` 의 효과)

**파일**: `ModelRotationRenderer.java` L347-365 (1.7.10)

```java
347:	private static float GetIntermediateAngle(float prevAngle, float shouldAngle, boolean fade, float lastTotalTime, float totalTime)
348:	{
349:		if(!fade || shouldAngle == prevAngle)
350:			return shouldAngle;
351:
352:		while(prevAngle >= Whole) prevAngle -= Whole;
353:		while(prevAngle < 0F) prevAngle += Whole;
354:
355:		while(shouldAngle >= Whole) shouldAngle -= Whole;
356:		while(shouldAngle < 0F) shouldAngle += Whole;
357:
358:		if(shouldAngle > prevAngle && (shouldAngle - prevAngle) > Half)
359:			prevAngle += Whole;
360:
361:		if(shouldAngle < prevAngle && (prevAngle - shouldAngle) > Half)
362:			shouldAngle += Whole;
363:
364:		return prevAngle + (shouldAngle - prevAngle) * (totalTime - lastTotalTime) * 0.2F;
365:	}
```

### 의미
- **fade=false** 또는 `prev == should`: 즉시 `should` 반환 (lerp 없음).
- **fade=true**: 두 각도를 [0, 2π) 로 정규화한 뒤 *최단 경로 lerp* (180° 이상 차이 시 ±2π 보정), `* 0.2F` 시정수 적용.
- `totalTime` 은 tick 단위 → 약 5 tick 정도면 should 에 근접.

### 헤드점프 적용
- 헤드점프 분기에서 명시적으로 `bipedOuter.fadeRotateAngleX = true;` 만 설정.
- `bipedOuter.fadeRotateAngleY` 는 명시 안 함 → 직전 프레임 reset 결과에 의존 (기본 `false`).
- **즉, 헤드점프 진입/이탈 시 X 기울기만 부드럽게 lerp, Y 회전은 즉시 변화**.

---

## 9. ModelBiped 좌표계 / 회전축 의미

### bipedOuter (= 전체 player 바디)
- `rotateAngleX`: **앞으로 기울기** (X축 회전). 양수 = 앞으로 굽힘 (다이브 자세).
- `rotateAngleY`: **Y 축 회전** (yaw). 양수 = 왼쪽으로.

### bipedHead
- `rotateAngleX`: 머리 위아래 (pitch). 양수 = 아래로 끄덕임.
- `rotateAngleY`: 머리 좌우 (yaw).

### bipedRightArm / bipedLeftArm
- `rotateAngleX`: 팔 앞뒤 (어깨에서 본 pitch). 양수 = 뒤로.
- `rotateAngleY`: 팔 좌우.
- `rotateAngleZ`: 팔 벌림. Right 는 양수 = 바깥쪽, Left 는 양수 = 안쪽.

### bipedRightLeg / bipedLeftLeg
- `rotateAngleX`: 다리 앞뒤 (걷기). 양수 = 뒤로.
- `rotateAngleZ`: 다리 벌림. Right 양수 = 바깥, Left 양수 = 안쪽.

### 헤드점프 자세 해석
| 항목 | 식 | 효과 |
|------|------|------|
| 몸 X | `Quarter - currentVerticalAngle` | 상승 시 < 90° (살짝 다이브), 하강 시 > 90° (역다이브). |
| 몸 Y | `currentHorizontalAngle` | 이동 방향 정렬 (마우스 무관). |
| 머리 X | `-bipedOuter.X / 2F` | 몸 기울기 절반 반대 → 머리 정면 유지. |
| 팔 X | `bendFactor * -Eighth` | 수평 다이브 시 -45° (뒤로 굽힘). |
| 다리 X | `bendFactor * -Eighth` | 동일 (다이브 자세 무릎 굽힘). |
| 팔 Z (오른쪽) | `Half - Sixteenth + armFactorZ * Eighth` | 157.5° 기본, 하강+착지 시 202.5° (앞으로 모음). |
| 팔 Z (왼쪽) | `Sixteenth - Half - armFactorZ * Eighth` | 오른팔 대칭. |
| 다리 Z (오른쪽) | `Sixtyfourth * legFactorZ` | 상승 시 5.625° 벌어짐. |
| 다리 Z (왼쪽) | `-Sixtyfourth * legFactorZ` | 좌우 대칭. |

---

## 10. 머리 위 솔리드 블록 감지 (armFactorZ 클램프)

### 메커니즘
1. `SmartMovingRender.renderPlayer` L89: `smallOverGroundHeight = (isHeadJump || isCrawlClimb) ? getOverGroundHeight(5D) : 0F` — 발 아래 ground 까지 거리 (0~5).
2. L90: `overGroundBlock = isHeadJump && smallOverGroundHeight < 5F ? getOverGroundBlockId(smallOverGroundHeight) : null` — 그 거리 안 첫 블록.
3. `SmartMovingModel` L521-522:
   ```java
   if(overGroundBlock != null && overGroundBlock.getMaterial().isSolid())
       armFactorZ = Math.min(armFactorZ, smallOverGroundHeight / 5F);
   ```

### 효과
- 헤드점프 하강 시: 기본 `armFactorZ = Factor(angle, Quarter, -Quarter)` 가 0~1 의 값 (하강 시 1 에 가까움).
- 발 아래 5m 안에 SOLID 블록 있으면: `armFactorZ ≤ height/5`.
  - 발이 솔리드에 닿는 순간 (height=0): armFactorZ → 0.
  - **착지 직전 팔이 다시 펴짐 (= 충격 흡수 자세)**.
- 솔리드 아닌 블록 (= material.isSolid()=false, 예: leaves, vines): 클램프 적용 안 됨.

### 1.12.2 차이
- L560: `overGroundBlock.getBlockState().getBaseState().getMaterial().isSolid()` — Material API 경로만 다름.
- 의미 동일.

---

## 11. 1.7.10 vs 1.12.2 종합 비교표

| 항목 | 1.7.10 (SmartMoving) | 1.12.2 (SmartMovingReboot) | 결론 |
|------|----------------------|----------------------------|------|
| `setRotationAngles` 분기 식 (헤드점프) | L505-530 | L544-568 | **완전 1:1** |
| `bipedOuter.fadeRotateAngleX = true` | ✅ | ✅ | 동일 |
| `bipedOuter.rotateAngleX = Quarter - currentVerticalAngle` | ✅ | ✅ | 동일 |
| `bipedOuter.rotateAngleY = currentHorizontalAngle` | ✅ | ✅ | 동일 |
| `bipedHead.rotateAngleX = -bipedOuter.X / 2F` | ✅ | ✅ | 동일 |
| `bendFactor = min(Factor(angle, Q, 0), Factor(angle, -Q, 0))` | ✅ | ✅ | 동일 |
| 양팔/양다리 X = `bendFactor * -Eighth` | ✅ | ✅ | 동일 |
| `armFactorZ = Factor(angle, Q, -Q)` | ✅ | ✅ | 동일 |
| 솔리드 검사 (Material.isSolid) | `getMaterial().isSolid()` | `getBlockState().getBaseState().getMaterial().isSolid()` | API 경로 차이만 |
| `armFactorZ` clamp `min(armFactorZ, height/5)` | ✅ | ✅ | 동일 |
| 팔 Z (오른쪽) = `Half - Sixteenth + armFactorZ * Eighth` | ✅ | ✅ | 동일 |
| 팔 Z (왼쪽) = `Sixteenth - Half - armFactorZ * Eighth` | ✅ | ✅ | 동일 |
| `legFactorZ = Factor(angle, -Q, Q)` | ✅ | ✅ | 동일 |
| 다리 Z = `±Sixtyfourth * legFactorZ` | ✅ | ✅ | 동일 |
| `smallOverGroundHeight = getOverGroundHeight(5D)` | ✅ | ✅ | 동일 |
| `overGroundBlock = getOverGroundBlockId(...)` | ✅ | ✅ | 동일 (block lookup API 만 다름) |
| `currentVerticalAngle = atan(yDiff / horizontalDistance)` | ✅ | ✅ | 동일 |
| NaN → Quarter 가드 | ✅ | ✅ | 동일 |
| `currentHorizontalAngle = -atan(xDiff / zDiff)` (zDiff<0 → +Half) | ✅ | ✅ | 동일 |
| renderYawOffset = forwardRotation force (isHeadJumping 포함) | ✅ | ✅ | 동일 |
| 상수 (Quarter/Half/Eighth/...) | `SmartRenderContext` 상속 | `SRUtilities` static import | 값 동일 |

**최종 결론**: 1.12.2 의 헤드점프 애니메이션은 1.7.10 의 *정확한 1:1 포팅* (API 경로만 마이그레이션). **두 버전 어느 쪽을 기준으로 매핑해도 동일한 결과**.

---

## 12. 1.21.1 Fabric 매핑 — 현재 상태 (참고용)

**파일**: `src\client\java\choco\ratel\smartmoving\mixin\client\MixinPlayerEntityModelClient.java`
**메서드**: `sm_animateHeadJumping` (L1317-1344)

```java
1317:	private void sm_animateHeadJumping(SmartMovingClientState sm) {
1318:		float angle = sm.stats.currentVerticalAngle;
1319:
1320:		// bendFactor: Factor(angle, Quarter, 0) ∩ Factor(angle, -Quarter, 0)
1321:		// 수직 각도 0°(수평)일 때 최대 1, ±Quarter(수직)일 때 0. (SmartMovingModel.md 10번 분기)
1322:		float bendFactor = Math.min(smFactor(angle, QUARTER, 0f), smFactor(angle, -QUARTER, 0f));
1323:		rightArm.pitch = bendFactor * -EIGHTH;
1324:		leftArm.pitch  = bendFactor * -EIGHTH;
1325:		rightLeg.pitch = bendFactor * -EIGHTH;
1326:		leftLeg.pitch  = bendFactor * -EIGHTH;
1327:
1328:		// 머리 X 보정: setupTransforms에서 θ=Quarter-angle이 전역 적용됨.
1329:		// 원본 head world-X = θ/2 → head.pitch = -θ/2 (C-09-2 등가 증명)
1330:		head.pitch = -(QUARTER - angle) / 2f;
1331:
1332:		// 팔 Z: Factor(angle, Quarter, -Quarter). 머리 위 고체 블록이면 smallOverGroundHeight/5로 클램프.
1333:		float armFactorZ = smFactor(angle, QUARTER, -QUARTER);
1334:		if (sm.smallOverGroundHeight < 5f) {
1335:			armFactorZ = Math.min(armFactorZ, sm.smallOverGroundHeight / 5f);
1336:		}
1337:		rightArm.roll  =  HALF - SIXTEENTH + armFactorZ * EIGHTH;
1338:		leftArm.roll   = -(HALF - SIXTEENTH) - armFactorZ * EIGHTH;
1339:
1340:		// 다리 Z: Factor(angle, -Quarter, Quarter)
1341:		float legFactorZ = smFactor(angle, -QUARTER, QUARTER);
1342:		rightLeg.roll =  SIXTYFOURTH * legFactorZ;
1343:		leftLeg.roll  = -SIXTYFOURTH * legFactorZ;
1344:	}
```

### bipedOuter X/Y 처리
**파일**: `MixinPlayerEntityRenderer.java::setupTransforms` inject
- L702-706:
  ```java
  702:	if (sm.isHeadJumping) {
  703:		float theta = (float) Math.PI / 2f - sm.stats.currentVerticalAngle;
  704:		matrices.multiply(RotationAxis.POSITIVE_X.rotation(theta));
  705:		sm.smOuterTiltX = theta;
  706:	}
  ```
- L339-342 (body Y):
  ```java
  339:	if (sm.isHeadJumping || sm.isRopeSliding) {
  340:		sm.smBodyYawActive = true;
  341:		sm.smBodyYawOverride = (float) Math.toDegrees(sm.stats.currentHorizontalAngle);
  342:		return;
  343:	}
  ```

### 매핑 vs 원본 — 1:1 확인 사항

| 원본 식 (1.7.10 SmartMovingModel L505-530) | 1.21.1 매핑 | 상태 |
|----------------------------------------------|-------------|------|
| `bipedOuter.fadeRotateAngleX = true` (L507) | ❌ 명시적 fade 없음 (setupTransforms 직접 matrices) | ⚠️ **fade 누락 가능성** — 진입/이탈 시 X 기울기 즉시 변화. 원본은 5-tick lerp. |
| `bipedOuter.rotateAngleX = Quarter - currentVerticalAngle` (L508) | `setupTransforms` L703 `theta = π/2 - angle; matrices.multiply(R_x(theta))` | ✅ |
| `bipedOuter.rotateAngleY = currentHorizontalAngle` (L509) | `setupTransforms` L341 `smBodyYawOverride = toDegrees(angle)` (ModifyArg) | ✅ |
| `bipedHead.rotateAngleX = -bipedOuter.X / 2F` (L511) | `sm_animateHeadJumping` L1330 `head.pitch = -(π/2 - angle) / 2f` | ✅ (`-θ/2` 등가) |
| `bendFactor = min(Factor(angle,Q,0), Factor(angle,-Q,0))` (L513) | L1322 동일 | ✅ |
| 양팔/양다리 X = `bendFactor * -Eighth` (L514-518) | L1323-1326 동일 | ✅ |
| `armFactorZ = Factor(angle, Q, -Q)` (L520) | L1333 동일 | ✅ |
| `if(overGroundBlock != null && getMaterial().isSolid())` → clamp (L521-522) | L1334 `if (sm.smallOverGroundHeight < 5f)` → clamp | ⚠️ **isSolid 검사 누락** — 원본은 *솔리드 머티리얼* 만 클램프. 1.21.1 매핑은 단순 `< 5f` 검사 → leaves/vines 등 non-solid 블록에서도 팔 모음 → **원본과 다름**. |
| 팔 Z (오른쪽) = `Half - Sixteenth + armFactorZ * Eighth` (L524) | L1337 동일 | ✅ |
| 팔 Z (왼쪽) = `Sixteenth - Half - armFactorZ * Eighth` (L525) | L1338 `-(HALF - SIXTEENTH) - armFactorZ * EIGHTH` (= `Sixteenth - Half - armFactorZ * Eighth`) | ✅ |
| `legFactorZ = Factor(angle, -Q, Q)` (L527) | L1341 동일 | ✅ |
| 다리 Z = `±Sixtyfourth * legFactorZ` (L528-529) | L1342-1343 동일 | ✅ |

### ⚠️ 검토 필요 항목 (잠재적 누락)
1. **`bipedOuter.fadeRotateAngleX = true` 의 5-tick lerp**: 1.21.1 `matrices.multiply(R_x(theta))` 는 매 프레임 즉시 적용 → 헤드점프 *진입 시 X 기울기 jerk* 가능. 원본은 `fade` 로 부드러운 보간.
2. **`overGroundBlock.getMaterial().isSolid()` 검사 누락**: 1.21.1 매핑이 단순 `< 5f` 만 검사 → non-solid 블록 (예: 나뭇잎, 덩굴) 에서도 팔 모음 자세. 원본은 *solid 만*.

### 추가 확인 필요
- `smOuterTiltX` 의 fade 보간이 별도 인프라로 구현되어 있는지 확인 (`MixinPlayerEntityRenderer` 의 다른 위치).
- `overGroundBlock` 정보 자체가 `SmartMovingClientState` 에 전달되는지 (현재 `smallOverGroundHeight` 만 전달되는 것으로 보임).
- `bendFactor * -Eighth` 가 양팔 X 와 양다리 X 양쪽에 적용되는데, *vanilla setAngles* 의 `arm.pitch = cos(swing) * 2 * limbDistance * 0.5` 등이 *덮어쓰기* 되는 부분이 있는지 확인.

---

## 13. 다음 단계 (체크리스트로 분리)

위 검토 필요 항목 (12.1, 12.2) 을 *실제 1.21.1 매핑* 와 *원본 라인* 직접 대조하여 1:1 정정 항목 추출. `docs/checklist_headjump_animation.md` 로 별도 작성.

### 검토 우선순위
1. **`overGroundBlock.getMaterial().isSolid()` 검사 누락** (12.2) — 명백한 식 누락. 매핑 즉시 정정.
2. **`bipedOuter.fadeRotateAngleX = true` lerp** (12.1) — fade 인프라 확인 후 누락 시 추가.
3. **bipedHead.rotateAngleX = -θ/2 인자 일관성 검증** — vanilla setAngles 에서 head.pitch 가 *마우스 입력* 으로 set 되는데, sm_setAngles inject 위치/시점이 vanilla 식을 완전히 *덮어쓰는지* 확인.

### 검증 방법
1. `MixinPlayerEntityModelClient.java::sm_animateHeadJumping` 인접 위치에서 사용된 *입력 상태* (sm.smallOverGroundHeight, sm.stats.currentVerticalAngle) 가 `SmartMovingClient` / `SmartMovingClientState` 에서 *어떤 식으로 set* 되는지 추적.
2. `overGroundBlock` 자체 (block reference) 가 client state 에 전달되지 않는다면 → state 필드 추가 + isSolid 검사 추가.
3. `smOuterTiltX` 의 fade 인프라 추적 — `MixinPlayerEntityRenderer` 의 setupTransforms 외 추가 위치 grep.

---

## 부록 A — 헤드점프 분기 *외* 의 관련 코드 위치 (참고용)

### isHeadJump 상태 set/clear (`SmartMovingSelf.java`)
- L1880-1899: `isHeadJumpCharging` (charge 누적 + 점프 발사).
- L2069-2070: `Config.getHeadJumpFactor(headJumpCharge) * normalAngle` (점프 발사 각도 계산).
- L2127-2129: `isHeadJumping = true; setHeightOffset(-1)` (점프 발사).
- L2216-2218: `isHeadJumping = false; resetHeightOffset()` (착지).
- L2293-2294: `restoreState` 시 false.
- L2523-2532: 매 tick `isHeadJumping` 조건부 유지/해제 + onGround/swim/fly 가드.
- L2546-2551: `isSliding + fallDistance > SlideToHeadJumpingFallDistance` → headJump 진입.
- L2557-2561: `isHeadJumping + sneak` → sliding 으로 전환.
- L3146: state bit 직렬화.

### 외 (Comm / Client / Other)
- 멀티플레이어 sync: `SmartMovingComm`, `SmartMovingOther`, `SmartMovingClient` (state bit relay).

### HUD (charge bar 표시)
- `SmartMovingRender.renderGuiIngame` L222-227: `_headJumpChargeMaximum` 와 `headJumpCharge` 로 충전 게이지 렌더.

---

**문서 끝.**
