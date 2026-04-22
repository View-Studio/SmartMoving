# SmartMovingModel.java (net.smart.moving.render) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/render/SmartMovingModel.java  
패키지: `net.smart.moving.render`  
종류: `class`  
상속: `net.smart.render.SmartRenderContext` (extends)  
실행 위치: 클라이언트 (렌더링)

---

## 전체 소스

```java
package net.smart.moving.render;

import net.minecraft.block.*;
import net.minecraft.client.model.*;
import net.minecraft.util.*;
import net.smart.moving.*;
import net.smart.render.*;

public class SmartMovingModel extends SmartRenderContext
{
    public IModelPlayer imp;
    public ModelBiped mp;
    public net.smart.render.SmartRenderModel md;

    public SmartMovingModel(net.smart.render.IModelPlayer md, IModelPlayer imp)
    {
        this.imp = imp;
        this.md = md.getRenderModel();
        this.mp = this.md.mp;

        if(SmartMovingRender.CurrentMainModel != null)
        {
            isClimb = SmartMovingRender.CurrentMainModel.isClimb;
            isClimbJump = SmartMovingRender.CurrentMainModel.isClimbJump;
            handsClimbType = SmartMovingRender.CurrentMainModel.handsClimbType;
            feetClimbType = SmartMovingRender.CurrentMainModel.feetClimbType;
            isHandsVineClimbing = SmartMovingRender.CurrentMainModel.isHandsVineClimbing;
            isFeetVineClimbing = SmartMovingRender.CurrentMainModel.isFeetVineClimbing;
            isCeilingClimb = SmartMovingRender.CurrentMainModel.isCeilingClimb;
            isSwim = SmartMovingRender.CurrentMainModel.isSwim;
            isDive = SmartMovingRender.CurrentMainModel.isDive;
            isCrawl = SmartMovingRender.CurrentMainModel.isCrawl;
            isCrawlClimb = SmartMovingRender.CurrentMainModel.isCrawlClimb;
            isJump = SmartMovingRender.CurrentMainModel.isJump;
            isHeadJump = SmartMovingRender.CurrentMainModel.isHeadJump;
            isSlide = SmartMovingRender.CurrentMainModel.isSlide;
            isFlying = SmartMovingRender.CurrentMainModel.isFlying;
            isLevitate = SmartMovingRender.CurrentMainModel.isLevitate;
            isFalling = SmartMovingRender.CurrentMainModel.isFalling;
            isGenericSneaking = SmartMovingRender.CurrentMainModel.isGenericSneaking;
            isAngleJumping = SmartMovingRender.CurrentMainModel.isAngleJumping;
            angleJumpType = SmartMovingRender.CurrentMainModel.angleJumpType;
            isRopeSliding = SmartMovingRender.CurrentMainModel.isRopeSliding;

            currentHorizontalSpeedFlattened = SmartMovingRender.CurrentMainModel.currentHorizontalSpeedFlattened;
            smallOverGroundHeight = SmartMovingRender.CurrentMainModel.smallOverGroundHeight;
            overGroundBlock = SmartMovingRender.CurrentMainModel.overGroundBlock;
        }
    }

    @SuppressWarnings("unused")
    private void setRotationAngles(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
    {
        // ... (아래 상세 분석)
    }

    // ... (아래 상세 분석)
}
```

---

## 역할

SmartMoving의 모든 특수 이동 상태에 대한 **모델 애니메이션 로직**을 담당한다. SmartRenderContext를 상속받아 각도 상수와 상태 플래그를 공유하며, `IModelPlayer` 인터페이스를 통해 SmartRender의 vanilla 애니메이션 메서드(super 계열)를 호출하거나 SM 자체 애니메이션으로 대체한다.

SmartMovingRender의 렌더링 파이프라인에서 `animate*()` 메서드들이 순서대로 호출된다.

---

## import

```java
import net.minecraft.block.*;         // Block (overGroundBlock 타입)
import net.minecraft.client.model.*;  // ModelBiped
import net.minecraft.util.*;          // MathHelper
import net.smart.moving.*;            // HandsClimbing, FeetClimbing
import net.smart.render.*;            // SmartRenderContext (부모), IModelPlayer (SR쪽)
```

---

## 상속 관계

```
SmartRenderContext  (net.smart.render)
    └─ SmartMovingModel  (net.smart.moving.render)
```

`SmartRenderContext`에서 상속받는 것:
- 각도 상수: `Half`, `Quarter`, `Eighth`, `Sixteenth`, `Thirtytwoth`, `Sixtyfourth`, `Whole`, `RadiantToAngle`
- 스케일 타입 상수: `Scale`, `NoScaleStart`, `NoScaleEnd`

---

## 공개 필드

```java
public IModelPlayer imp;                    // net.smart.moving.render.IModelPlayer
public ModelBiped mp;                       // vanilla ModelBiped (mp.onGround 등 접근용)
public net.smart.render.SmartRenderModel md; // SmartRender 모델 (뼈대 노드 접근용)
```

### 상태 플래그 필드 (모두 public)

```java
public boolean isStandard;         // 아무 특수 상태도 아닌 경우 true

// 클라이밍
public boolean isClimb;
public boolean isClimbJump;
public int feetClimbType;          // FeetClimbing.* 상수
public int handsClimbType;         // HandsClimbing.* 상수
public boolean isHandsVineClimbing;
public boolean isFeetVineClimbing;
public boolean isCeilingClimb;

// 수영/다이브
public boolean isSwim;
public boolean isDive;

// 크롤/슬라이드/점프/기타
public boolean isCrawl;
public boolean isCrawlClimb;
public boolean isJump;
public boolean isHeadJump;
public boolean isFlying;
public boolean isSlide;
public boolean isLevitate;
public boolean isFalling;
public boolean isGenericSneaking;
public boolean isAngleJumping;
public int angleJumpType;
public boolean isRopeSliding;

// 물리 수치
public float currentHorizontalSpeedFlattened;  // NaN이면 파라미터 currentHorizontalSpeed 사용
public float smallOverGroundHeight;            // 머리 위 블록까지의 거리
public Block overGroundBlock;                  // 머리 위 블록

// 스케일 타입
public int scaleArmType;
public int scaleLegType;
```

---

## 생성자

```java
public SmartMovingModel(net.smart.render.IModelPlayer md, IModelPlayer imp)
{
    this.imp = imp;
    this.md = md.getRenderModel();  // SmartRenderModel 취득
    this.mp = this.md.mp;           // ModelBiped 취득

    if(SmartMovingRender.CurrentMainModel != null)
    {
        // 현재 주 모델(메인 바디)에서 상태를 복사
        isClimb = SmartMovingRender.CurrentMainModel.isClimb;
        isClimbJump = SmartMovingRender.CurrentMainModel.isClimbJump;
        handsClimbType = SmartMovingRender.CurrentMainModel.handsClimbType;
        feetClimbType = SmartMovingRender.CurrentMainModel.feetClimbType;
        isHandsVineClimbing = SmartMovingRender.CurrentMainModel.isHandsVineClimbing;
        isFeetVineClimbing = SmartMovingRender.CurrentMainModel.isFeetVineClimbing;
        isCeilingClimb = SmartMovingRender.CurrentMainModel.isCeilingClimb;
        isSwim = SmartMovingRender.CurrentMainModel.isSwim;
        isDive = SmartMovingRender.CurrentMainModel.isDive;
        isCrawl = SmartMovingRender.CurrentMainModel.isCrawl;
        isCrawlClimb = SmartMovingRender.CurrentMainModel.isCrawlClimb;
        isJump = SmartMovingRender.CurrentMainModel.isJump;
        isHeadJump = SmartMovingRender.CurrentMainModel.isHeadJump;
        isSlide = SmartMovingRender.CurrentMainModel.isSlide;
        isFlying = SmartMovingRender.CurrentMainModel.isFlying;
        isLevitate = SmartMovingRender.CurrentMainModel.isLevitate;
        isFalling = SmartMovingRender.CurrentMainModel.isFalling;
        isGenericSneaking = SmartMovingRender.CurrentMainModel.isGenericSneaking;
        isAngleJumping = SmartMovingRender.CurrentMainModel.isAngleJumping;
        angleJumpType = SmartMovingRender.CurrentMainModel.angleJumpType;
        isRopeSliding = SmartMovingRender.CurrentMainModel.isRopeSliding;

        currentHorizontalSpeedFlattened = SmartMovingRender.CurrentMainModel.currentHorizontalSpeedFlattened;
        smallOverGroundHeight = SmartMovingRender.CurrentMainModel.smallOverGroundHeight;
        overGroundBlock = SmartMovingRender.CurrentMainModel.overGroundBlock;
    }
}
```

**파라미터**:
- `md`: `net.smart.render.IModelPlayer` — SmartRender 쪽 인터페이스. `md.getRenderModel()`로 `SmartRenderModel` 취득
- `imp`: `net.smart.moving.render.IModelPlayer` — SmartMoving 쪽 인터페이스. `super*()` 호출 위임 대상

**`SmartMovingRender.CurrentMainModel`**: 주 바디 모델의 `SmartMovingModel` 인스턴스. null이면 상태 복사 스킵(모든 플래그 기본값 false/0/NaN 유지).

---

## 메서드 상세

### `setRotationAngles()` (private)

```java
@SuppressWarnings("unused")
private void setRotationAngles(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime,
    float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
```

**역할**: SM 상태에 따른 뼈대 각도를 계산하고 `md.*` 필드에 직접 적용. `isStandard` 플래그를 설정한다.

**진입 시 고정 처리**:
```java
final float FrequenceFactor = 0.6662F;
isStandard = false;
```
항상 false로 시작, 마지막 `else` 블록에서만 `isStandard = true` 설정.

**md에서 로컬로 복사하는 값들**:
```java
float currentCameraAngle = md.currentCameraAngle;
float currentHorizontalAngle = md.currentHorizontalAngle;
float currentVerticalAngle = md.currentVerticalAngle;
float forwardRotation = md.forwardRotation;
float currentVerticalSpeed = md.currentVerticalSpeed;
float totalVerticalDistance = md.totalVerticalDistance;
float totalDistance = md.totalDistance;
double horizontalDistance = md.horizontalDistance;
float currentSpeed = md.currentSpeed;
```

**currentHorizontalSpeedFlattened 적용**:
```java
if(!Float.isNaN(currentHorizontalSpeedFlattened))
    currentHorizontalSpeed = currentHorizontalSpeedFlattened;
```
NaN이 아니면 파라미터로 받은 currentHorizontalSpeed를 오버라이드.

**뼈대 노드 로컬 참조**:
```java
ModelRotationRenderer bipedOuter = md.bipedOuter;
ModelRotationRenderer bipedTorso = md.bipedTorso;
ModelRotationRenderer bipedBody = md.bipedBody;
ModelRotationRenderer bipedBreast = md.bipedBreast;
ModelRotationRenderer bipedHead = md.bipedHead;
ModelRotationRenderer bipedRightShoulder = md.bipedRightShoulder;
ModelRotationRenderer bipedRightArm = md.bipedRightArm;
ModelRotationRenderer bipedLeftShoulder = md.bipedLeftShoulder;
ModelRotationRenderer bipedLeftArm = md.bipedLeftArm;
ModelRotationRenderer bipedPelvic = md.bipedPelvic;
ModelRotationRenderer bipedRightLeg = md.bipedRightLeg;
ModelRotationRenderer bipedLeftLeg = md.bipedLeftLeg;
```

---

#### 상태별 분기 — if-else 체인 (우선순위 순)

##### 1. `isRopeSliding`

```java
float time = totalTime * 0.15F;

bipedHead.rotateAngleZ = Between(-Sixteenth, Sixteenth, Normalize(currentCameraAngle - currentHorizontalAngle));
bipedHead.rotateAngleX = Eighth;
bipedHead.rotationPointY = 2F;

bipedOuter.fadeRotateAngleY = false;
bipedOuter.rotateAngleY = currentHorizontalAngle;
bipedTorso.rotateAngleX = Sixteenth + Sixtyfourth * MathHelper.cos(time);

bipedLeftArm.rotateAngleX = bipedRightArm.rotateAngleX = Half - bipedTorso.rotateAngleX;

bipedRightArm.rotateAngleZ = Sixteenth + Thirtytwoth;
bipedLeftArm.rotateAngleZ = -Sixteenth - Thirtytwoth;

bipedRightArm.rotationPointY = bipedLeftArm.rotationPointY = -2F;

bipedPelvic.rotateAngleX = bipedTorso.rotateAngleX;

bipedLeftLeg.rotateAngleZ = -Thirtytwoth;
bipedRightLeg.rotateAngleZ = Thirtytwoth;

bipedLeftLeg.rotateAngleX = Sixtyfourth * MathHelper.cos(time + Quarter);
bipedRightLeg.rotateAngleX = Sixtyfourth * MathHelper.cos(time - Quarter);
```

time = `totalTime * 0.15F` (느린 흔들림).  
토르소 X 각도 = `Sixteenth + Sixtyfourth * cos(time)` (주기적 미세 흔들림).  
팔 X = `Half - 토르소X` (매달린 자세).  
머리 Z는 카메라-이동 방향 차이를 [-Sixteenth, Sixteenth] 범위로 클램프.

---

##### 2. `isClimb || isCrawlClimb`

```java
bipedOuter.rotateAngleY = forwardRotation / RadiantToAngle;
bipedHead.rotateAngleY = 0.0F;
bipedHead.rotateAngleX = viewVerticalAngelOffset / RadiantToAngle;
bipedLeftLeg.rotationOrder = ModelRotationRenderer.YZX;
bipedRightLeg.rotationOrder = ModelRotationRenderer.YZX;
```

**handsClimbType 스위치** (vine MiddleGrab → UpGrab로 변환 처리):
```java
int handsClimbType = this.handsClimbType;
if(isHandsVineClimbing && handsClimbType == HandsClimbing.MiddleGrab)
    handsClimbType = HandsClimbing.UpGrab;

float verticalSpeed = Math.min(0.5f, currentVerticalSpeed);
float horizontalSpeed = Math.min(0.5f, currentHorizontalSpeed);
```

| handsClimbType | 손 FrequenceUpFactor | 손 DistanceUpFactor | 손 DistanceUpOffset | 손 FrequenceSideFactor | 손 DistanceSideFactor | 손 DistanceSideOffset |
|---|---|---|---|---|---|---|
| MiddleGrab | 0.6662F | 2F | -Quarter | 0.6662F | 1.0F | 0.0F |
| UpGrab | 0.6662F | 2F | -2.5F | 0.6662F | 1.0F | 0.0F |
| default(NoGrab 등) | 0.6662F | 0F | -0.5F | 0.6662F | 1.0F | 0.0F |

| feetClimbType | 발 FrequenceUpFactor | 발 DistanceUpFactor | 발 DistanceUpOffset | 발 FrequenceSideFactor | 발 DistanceSideFactor | 발 DistanceSideOffset |
|---|---|---|---|---|---|---|
| UpGrab | 0.6662F | 0.3F/verticalSpeed | -0.3F | 0.6662F | 0.5F | 0.0F |
| default | 0.6662F | 0.0F | 0.0F | 0.6662F | 0.0F | 0.0F |

**팔 각도 계산**:
```java
bipedRightArm.rotateAngleX = cos(totalVerticalDistance * handsFrequenceUpFactor + Half) * verticalSpeed * handsDistanceUpFactor + handsDistanceUpOffset;
bipedLeftArm.rotateAngleX  = cos(totalVerticalDistance * handsFrequenceUpFactor) * verticalSpeed * handsDistanceUpFactor + handsDistanceUpOffset;

bipedRightArm.rotateAngleY = cos(totalHorizontalDistance * handsFrequenceSideFactor + Quarter) * horizontalSpeed * handsDistanceSideFactor + handsDistanceSideOffset;
bipedLeftArm.rotateAngleY  = cos(totalHorizontalDistance * handsFrequenceSideFactor) * horizontalSpeed * handsDistanceSideFactor + handsDistanceSideOffset;
```

**isHandsVineClimbing 추가 보정**:
```java
bipedLeftArm.rotateAngleY  *= 1F + handsFrequenceSideFactor;   // 1.6662배
bipedRightArm.rotateAngleY *= 1F + handsFrequenceSideFactor;

bipedLeftArm.rotateAngleY  += Eighth;
bipedRightArm.rotateAngleY -= Eighth;

setArmScales(Math.abs(cos(bipedRightArm.rotateAngleX)), Math.abs(cos(bipedLeftArm.rotateAngleX)));
```

**발 각도 계산**:
```java
// isFeetVineClimbing 아닌 경우:
bipedRightLeg.rotateAngleX = cos(totalVerticalDistance * feetFrequenceUpFactor) * feetDistanceUpFactor * verticalSpeed + feetDistanceUpOffset;
bipedLeftLeg.rotateAngleX  = cos(totalVerticalDistance * feetFrequenceUpFactor + Half) * feetDistanceUpFactor * verticalSpeed + feetDistanceUpOffset;

bipedRightLeg.rotateAngleZ = -(cos(totalHorizontalDistance * feetFrequenceSideFactor) - 1.0F) * horizontalSpeed * feetDistanceSideFactor + feetDistanceSideOffset;
bipedLeftLeg.rotateAngleZ  = -(cos(totalHorizontalDistance * feetFrequenceSideFactor + Quarter) + 1.0F) * horizontalSpeed * feetDistanceSideFactor + feetDistanceSideOffset;
```

**isFeetVineClimbing 대체**:
```java
float total = (cos(totalDistance + Half) + 1) * Thirtytwoth + Sixteenth;
bipedRightLeg.rotateAngleX = -total;
bipedLeftLeg.rotateAngleX  = -total;

float difference = Math.max(0, cos(totalDistance - Quarter)) * Sixtyfourth;
bipedLeftLeg.rotateAngleZ += -difference;
bipedRightLeg.rotateAngleZ += difference;

setLegScales(Math.abs(cos(bipedRightLeg.rotateAngleX)), Math.abs(cos(bipedLeftLeg.rotateAngleX)));
```

**isCrawlClimb 추가 보정**:
```java
float height = smallOverGroundHeight + 0.25F;
float bodyLength = 0.7F;
float legLength = 0.55F;

float bodyAngleX, legAngleX, legAngleZ;
if(height < bodyLength)
{
    bodyAngleX = Math.max(0, (float)Math.acos(height / bodyLength));
    legAngleX = Quarter - bodyAngleX;
    legAngleZ = Thirtytwoth;
}
else if(height < bodyLength + legLength)  // height < 1.25F
{
    bodyAngleX = 0F;
    legAngleX = Math.max(0, (float)Math.acos((height - bodyLength) / legLength));
    legAngleZ = Thirtytwoth * (legAngleX / 1.537F);
}
else
{
    bodyAngleX = 0F;
    legAngleX = 0F;
    legAngleZ = 0F;
}

bipedTorso.rotateAngleX = bodyAngleX;
bipedRightShoulder.rotateAngleX = -bodyAngleX;
bipedLeftShoulder.rotateAngleX  = -bodyAngleX;
bipedHead.rotateAngleX = -bodyAngleX;
bipedRightLeg.rotateAngleX = legAngleX;
bipedLeftLeg.rotateAngleX  = legAngleX;
bipedRightLeg.rotateAngleZ = legAngleZ;
bipedLeftLeg.rotateAngleZ  = -legAngleZ;
```

**handsClimbType == NoGrab && feetClimbType != NoStep 추가 보정**:
```java
bipedTorso.rotateAngleX = 0.5F;
bipedHead.rotateAngleX -= 0.5F;
bipedPelvic.rotateAngleX -= 0.5F;
bipedTorso.rotationPointZ = -6.0F;
```

---

##### 3. `isClimbJump`

```java
bipedRightArm.rotateAngleX = Half + Sixteenth;
bipedLeftArm.rotateAngleX  = Half + Sixteenth;
bipedRightArm.rotateAngleZ = -Thirtytwoth;
bipedLeftArm.rotateAngleZ  = Thirtytwoth;
```

---

##### 4. `isCeilingClimb`

```java
float distance = totalHorizontalDistance * 0.7F;
float walkFactor = Factor(currentHorizontalSpeed, 0F, 0.12951545F);
float standFactor = Factor(currentHorizontalSpeed, 0.12951545F, 0F);
float horizontalAngle = horizontalDistance < 0.015F ? currentCameraAngle : currentHorizontalAngle;
```

걷기/서있기 임계값: `0.12951545F`

```java
bipedLeftArm.rotateAngleX  = (cos(distance) * 0.52F + Half) * walkFactor + Half * standFactor;
bipedRightArm.rotateAngleX = (cos(distance + Half) * 0.52F - Half) * walkFactor - Half * standFactor;

bipedLeftLeg.rotateAngleX  = -cos(distance) * 0.12F * walkFactor;
bipedRightLeg.rotateAngleX = -cos(distance + Half) * 0.32F * walkFactor;

float rotateY = cos(distance) * 0.44F * walkFactor;
bipedOuter.rotateAngleY = rotateY + horizontalAngle;

bipedRightArm.rotateAngleY = bipedLeftArm.rotateAngleY = -rotateY;
bipedRightLeg.rotateAngleY = bipedLeftLeg.rotateAngleY = -rotateY;
bipedHead.rotateAngleY = -rotateY;
```

---

##### 5. `isSwim`

```java
float distance = totalHorizontalDistance;
float walkFactor = Factor(currentHorizontalSpeed, 0.15679921F, 0.52264464F);
float sneakFactor = Math.min(
    Factor(currentHorizontalSpeed, 0, 0.15679921F),
    Factor(currentHorizontalSpeed, 0.52264464F, 0.15679921F));
float standFactor = Factor(currentHorizontalSpeed, 0.15679921F, 0F);
float standSneakFactor = standFactor + sneakFactor;
float horizontalAngle = horizontalDistance < (isGenericSneaking ? 0.005 : 0.015F) ? currentCameraAngle : currentHorizontalAngle;
```

속도 구간:
- 0 ~ 0.15679921F: standSneakFactor 영역 (sneakFactor ↑, walkFactor 0)
- 0.15679921F ~ 0.52264464F: walkFactor 영역
- 0.52264464F 이상: sneakFactor 다시 ↑

```java
bipedHead.rotationOrder = ModelRotationRenderer.YXZ;
bipedHead.rotateAngleY = cos(distance / 2.0F - Quarter) * walkFactor;
bipedHead.rotateAngleX = -Eighth * standSneakFactor;
bipedHead.rotationPointZ = -2F;

bipedOuter.fadeRotateAngleX = true;
bipedOuter.rotateAngleX = Quarter - Sixteenth * standSneakFactor;
bipedOuter.rotateAngleY = horizontalAngle;

bipedBreast.rotateAngleY = bipedBody.rotateAngleY = cos(distance / 2.0F - Quarter) * walkFactor;

bipedRightArm.rotationOrder = ModelRotationRenderer.YZX;
bipedLeftArm.rotationOrder  = ModelRotationRenderer.YZX;

bipedRightArm.rotateAngleZ = Quarter + Eighth + cos(totalTime * 0.1F) * standSneakFactor * 0.8F;
bipedLeftArm.rotateAngleZ  = -Quarter - Eighth - cos(totalTime * 0.1F) * standSneakFactor * 0.8F;

bipedRightArm.rotateAngleX = ((distance * 0.5F) % Whole - Half) * walkFactor + Sixteenth * standSneakFactor;
bipedLeftArm.rotateAngleX  = ((distance * 0.5F + Half) % Whole - Half) * walkFactor + Sixteenth * standSneakFactor;

bipedRightLeg.rotateAngleX = cos(distance) * 0.52264464F * walkFactor;
bipedLeftLeg.rotateAngleX  = cos(distance + Half) * 0.52264464F * walkFactor;

float rotateFeetAngleZ = Sixteenth * standSneakFactor + cos(totalTime * 0.1F) * 0.4F * (standFactor - sneakFactor);
bipedRightLeg.rotateAngleZ = rotateFeetAngleZ;
bipedLeftLeg.rotateAngleZ  = -rotateFeetAngleZ;
```

스케일 적용:
```java
if(scaleLegType != NoScaleStart)
    setLegScales(
        1F + (cos(totalTime * 0.1F + Quarter) - 1F) * 0.15F * sneakFactor,
        1F + (cos(totalTime * 0.1F + Quarter) - 1F) * 0.15F * sneakFactor);

if(scaleArmType != NoScaleStart)
    setArmScales(
        1F + (cos(totalTime * 0.1F - Quarter) - 1F) * 0.15F * sneakFactor,
        1F + (cos(totalTime * 0.1F - Quarter) - 1F) * 0.15F * sneakFactor);
```

---

##### 6. `isDive`

```java
float distance = totalDistance * 0.7F;
float walkFactor = Factor(currentSpeed, 0F, 0.15679921F);
float standFactor = Factor(currentSpeed, 0.15679921F, 0F);
float horizontalAngle = totalDistance < (isGenericSneaking ? 0.005 : 0.015F) ? currentCameraAngle : currentHorizontalAngle;
```

isSwim과 달리 `totalDistance` (수평+수직 합산) 기준으로 walkFactor 계산.

```java
bipedHead.rotateAngleX = -Eighth;
bipedHead.rotationPointZ = -2F;

bipedOuter.fadeRotateAngleX = true;
bipedOuter.rotateAngleX = isLevitate ? Quarter - Sixteenth : (isJump ? 0F : Quarter - currentVerticalAngle);
bipedOuter.rotateAngleY = horizontalAngle;

bipedRightLeg.rotateAngleZ = (cos(distance) + 1F) * 0.52264464F * walkFactor + Sixteenth * standFactor;
bipedLeftLeg.rotateAngleZ  = (cos(distance + Half) - 1F) * 0.52264464F * walkFactor - Sixteenth * standFactor;
```

스케일:
```java
if(scaleLegType != NoScaleStart)
    setLegScales(
        1F + (cos(distance - Quarter) - 1F) * 0.25F * walkFactor,
        1F + (cos(distance - Quarter) - 1F) * 0.25F * walkFactor);

bipedRightArm.rotateAngleZ = (cos(distance + Half) * 0.52264464F * 2.5F + Quarter) * walkFactor + (Quarter + Eighth) * standFactor;
bipedLeftArm.rotateAngleZ  = (cos(distance) * 0.52264464F * 2.5F - Quarter) * walkFactor - (Quarter + Eighth) * standFactor;

if(scaleArmType != NoScaleStart)
    setArmScales(
        1F + (cos(distance + Quarter) - 1F) * 0.15F * walkFactor,
        1F + (cos(distance + Quarter) - 1F) * 0.15F * walkFactor);
```

bipedOuter.rotateAngleX 결정:
- `isLevitate`: `Quarter - Sixteenth`
- `isJump`: `0F`
- 그 외: `Quarter - currentVerticalAngle`

---

##### 7. `isCrawl`

```java
float distance = totalHorizontalDistance * 1.3F;
float walkFactor = Factor(currentHorizontalSpeedFlattened, 0F, 0.12951545F);
float standFactor = Factor(currentHorizontalSpeedFlattened, 0.12951545F, 0F);
```

isSwim과 달리 `currentHorizontalSpeedFlattened`(파라미터 오버라이드 전 원본) 직접 사용.

```java
bipedHead.rotateAngleZ = -viewHorizontalAngelOffset / RadiantToAngle;
bipedHead.rotateAngleX = -Eighth;
bipedHead.rotationPointZ = -2F;

bipedTorso.rotationOrder = ModelRotationRenderer.YZX;
bipedTorso.rotateAngleX = Quarter - Thirtytwoth;
bipedTorso.rotationPointY = 3F;
bipedTorso.rotateAngleZ = cos(distance + Quarter) * Sixtyfourth * walkFactor;
bipedBody.rotateAngleY  = cos(distance + Half) * Sixtyfourth * walkFactor;

bipedRightLeg.rotateAngleX = (cos(distance - Quarter) * Sixtyfourth + Thirtytwoth) * walkFactor + Thirtytwoth * standFactor;
bipedLeftLeg.rotateAngleX  = (cos(distance - Half - Quarter) * Sixtyfourth + Thirtytwoth) * walkFactor + Thirtytwoth * standFactor;

bipedRightLeg.rotateAngleZ = (cos(distance - Quarter) + 1F) * 0.25F * walkFactor + Thirtytwoth * standFactor;
bipedLeftLeg.rotateAngleZ  = (cos(distance - Quarter) - 1F) * 0.25F * walkFactor - Thirtytwoth * standFactor;
```

스케일:
```java
if(scaleLegType != NoScaleStart)
    setLegScales(
        1F + (cos(distance + Quarter - Quarter) - 1F) * 0.25F * walkFactor,
        1F + (cos(distance - Quarter - Quarter) - 1F) * 0.25F * walkFactor);
```

```java
bipedRightArm.rotationOrder = ModelRotationRenderer.YZX;
bipedLeftArm.rotationOrder  = ModelRotationRenderer.YZX;

bipedRightArm.rotateAngleX = Half + Eighth;
bipedLeftArm.rotateAngleX  = Half + Eighth;

bipedRightArm.rotateAngleZ = ((cos(distance + Half)) * Sixtyfourth + Thirtytwoth) * walkFactor + Sixteenth * standFactor;
bipedLeftArm.rotateAngleZ  = ((cos(distance + Half)) * Sixtyfourth - Thirtytwoth) * walkFactor - Sixteenth * standFactor;

bipedRightArm.rotateAngleY = -Quarter;
bipedLeftArm.rotateAngleY  = Quarter;
```

스케일:
```java
if(scaleArmType != NoScaleStart)
    setArmScales(
        1F + (cos(distance + Quarter) - 1F) * 0.15F * walkFactor,
        1F + (cos(distance - Quarter) - 1F) * 0.15F * walkFactor);
```

---

##### 8. `isSlide`

```java
float distance = totalHorizontalDistance * 0.7F;
float walkFactor = Factor(currentHorizontalSpeed, 0F, 1F) * 0.8F;
```

walkFactor는 0~1 속도 범위 × 0.8.

```java
bipedHead.rotateAngleZ = -viewHorizontalAngelOffset / RadiantToAngle;
bipedHead.rotateAngleX = -Eighth - Sixteenth;
bipedHead.rotationPointZ = -2F;

bipedOuter.fadeRotateAngleY = false;
bipedOuter.rotateAngleY = currentHorizontalAngle;
bipedOuter.rotationPointY = 5F;
bipedOuter.rotateAngleX = Quarter;

bipedBody.rotationOrder = ModelRotationRenderer.YXZ;
bipedBody.offsetY = -0.4F;
bipedBody.rotationPointY = +6.5F;
bipedBody.rotateAngleX = cos(distance - Eighth) * Sixtyfourth * walkFactor;
bipedBody.rotateAngleY = cos(distance + Eighth) * Sixtyfourth * walkFactor;

bipedRightLeg.rotateAngleX = cos(distance + Half) * Sixtyfourth * walkFactor + Sixtyfourth;
bipedLeftLeg.rotateAngleX  = cos(distance + Quarter) * Sixtyfourth * walkFactor + Sixtyfourth;
bipedRightLeg.rotateAngleZ = Thirtytwoth;
bipedLeftLeg.rotateAngleZ  = -Thirtytwoth;

bipedRightArm.rotationOrder = ModelRotationRenderer.YZX;
bipedLeftArm.rotationOrder  = ModelRotationRenderer.YZX;

bipedRightArm.rotateAngleX = cos(distance + Quarter) * Sixtyfourth * walkFactor + Half - Sixtyfourth;
bipedLeftArm.rotateAngleX  = cos(distance - Half) * Sixtyfourth * walkFactor + Half - Sixtyfourth;
bipedRightArm.rotateAngleZ = Sixteenth;
bipedLeftArm.rotateAngleZ  = -Sixteenth;
bipedRightArm.rotateAngleY = -Quarter;
bipedLeftArm.rotateAngleY  = Quarter;
```

---

##### 9. `isFlying`

```java
float distance = totalDistance * 0.08F;
float walkFactor = Factor(currentSpeed, 0F, 1);
float standFactor = Factor(currentSpeed, 1F, 0F);
float time = totalTime * 0.15F;
float verticalAngle = isJump ? Math.abs(currentVerticalAngle) : currentVerticalAngle;
float horizontalAngle = horizontalDistance < 0.05F ? currentCameraAngle : currentHorizontalAngle;
```

isJump이면 수직 각도 절댓값 사용.  
horizontalDistance 임계값 0.05F (isSwim: 0.015F보다 큼).

```java
bipedOuter.fadeRotateAngleX = true;
bipedOuter.rotateAngleX = (Quarter - verticalAngle) * walkFactor;
bipedOuter.rotateAngleY = horizontalAngle;

bipedHead.rotateAngleX = -bipedOuter.rotateAngleX / 2F;

bipedRightArm.rotationOrder = ModelRotationRenderer.XZY;
bipedLeftArm.rotationOrder  = ModelRotationRenderer.XZY;

bipedRightArm.rotateAngleY = (cos(time) * Sixteenth) * standFactor;
bipedLeftArm.rotateAngleY  = (cos(time) * Sixteenth) * standFactor;

bipedRightArm.rotateAngleZ = (cos(distance + Half) * Sixtyfourth + (Half - Sixteenth)) * walkFactor + Quarter * standFactor;
bipedLeftArm.rotateAngleZ  = (cos(distance) * Sixtyfourth - (Half - Sixteenth)) * walkFactor - Quarter * standFactor;

bipedRightLeg.rotateAngleX = cos(distance) * Sixtyfourth * walkFactor + cos(time + Half) * Sixtyfourth * standFactor;
bipedLeftLeg.rotateAngleX  = cos(distance + Half) * Sixtyfourth * walkFactor + cos(time) * Sixtyfourth * standFactor;

bipedRightLeg.rotateAngleZ = Sixtyfourth;
bipedLeftLeg.rotateAngleZ  = -Sixtyfourth;
```

---

##### 10. `isHeadJump`

```java
bipedOuter.fadeRotateAngleX = true;
bipedOuter.rotateAngleX = (Quarter - currentVerticalAngle);
bipedOuter.rotateAngleY = currentHorizontalAngle;

bipedHead.rotateAngleX = -bipedOuter.rotateAngleX / 2F;

float bendFactor = Math.min(
    Factor(currentVerticalAngle, Quarter, 0),
    Factor(currentVerticalAngle, -Quarter, 0));
bipedRightArm.rotateAngleX = bendFactor * -Eighth;
bipedLeftArm.rotateAngleX  = bendFactor * -Eighth;
bipedRightLeg.rotateAngleX = bendFactor * -Eighth;
bipedLeftLeg.rotateAngleX  = bendFactor * -Eighth;

float armFactorZ = Factor(currentVerticalAngle, Quarter, -Quarter);
if(overGroundBlock != null && overGroundBlock.getMaterial().isSolid())
    armFactorZ = Math.min(armFactorZ, smallOverGroundHeight / 5F);

bipedRightArm.rotateAngleZ = Half - Sixteenth + armFactorZ * Eighth;
bipedLeftArm.rotateAngleZ  = Sixteenth - Half - armFactorZ * Eighth;

float legFactorZ = Factor(currentVerticalAngle, -Quarter, Quarter);
bipedRightLeg.rotateAngleZ = Sixtyfourth * legFactorZ;
bipedLeftLeg.rotateAngleZ  = -Sixtyfourth * legFactorZ;
```

overGroundBlock이 solid이면 armFactorZ를 `smallOverGroundHeight / 5F`로 제한.

---

##### 11. `isFalling`

```java
float distance = totalDistance * 0.1F;

bipedRightArm.rotationOrder = ModelRotationRenderer.XZY;
bipedLeftArm.rotationOrder  = ModelRotationRenderer.XZY;

bipedRightArm.rotateAngleY = (cos(distance + Quarter) * Eighth);
bipedLeftArm.rotateAngleY  = (cos(distance + Quarter) * Eighth);

bipedRightArm.rotateAngleZ = (cos(distance) * Eighth + Quarter);
bipedLeftArm.rotateAngleZ  = (cos(distance) * Eighth - Quarter);

bipedRightLeg.rotateAngleX = (cos(distance + Half + Quarter) * Sixteenth + Thirtytwoth);
bipedLeftLeg.rotateAngleX  = (cos(distance + Quarter) * Sixteenth + Thirtytwoth);

bipedRightLeg.rotateAngleZ = (cos(distance) * Sixteenth + Thirtytwoth);
bipedLeftLeg.rotateAngleZ  = (cos(distance) * Sixteenth - Thirtytwoth);
```

---

##### 12. else (표준 상태)

```java
isStandard = true;
```

위 11가지 조건 중 하나도 해당하지 않으면 isStandard = true.

---

### `isWorking()` (private)

```java
private boolean isWorking()
{
    return mp.onGround > 0F;
}
```

`ModelBiped.onGround` 필드 사용. vanilla ModelBiped의 onGround는 도구 사용 애니메이션에 관련된 float 필드.

---

### `animateAngleJumping()` (private)

```java
private void animateAngleJumping()
{
    float angle = angleJumpType * Eighth;
    md.bipedPelvic.rotateAngleY -= md.bipedOuter.rotateAngleY;
    md.bipedPelvic.rotateAngleY += md.currentCameraAngle;

    float backness = 1F - Math.abs(angle - Half) / Quarter;
    float leftness = -Math.min(angle - Half, 0F) / Quarter;
    float rightness = Math.max(angle - Half, 0F) / Quarter;

    md.bipedLeftLeg.rotateAngleX  = Thirtytwoth * (1F + rightness);
    md.bipedRightLeg.rotateAngleX = Thirtytwoth * (1F + leftness);
    md.bipedLeftLeg.rotateAngleY  = -angle;
    md.bipedRightLeg.rotateAngleY = -angle;
    md.bipedLeftLeg.rotateAngleZ  = Thirtytwoth * backness;
    md.bipedRightLeg.rotateAngleZ = -Thirtytwoth * backness;

    md.bipedLeftLeg.rotationOrder  = ModelRotationRenderer.ZXY;
    md.bipedRightLeg.rotationOrder = ModelRotationRenderer.ZXY;

    md.bipedLeftArm.rotateAngleZ  = -Sixteenth * rightness;
    md.bipedRightArm.rotateAngleZ = Sixteenth * leftness;

    md.bipedLeftArm.rotateAngleX  = -Eighth * backness;
    md.bipedRightArm.rotateAngleX = -Eighth * backness;
}
```

`angle = angleJumpType * Eighth` — angleJumpType에 따른 각도.  
`backness / leftness / rightness` — angle = Half(전방)일 때 backness=1, leftness=0, rightness=0.  
pelvis Y는 외부 회전 제거 후 카메라 방향으로 재설정.

---

### `animateNonStandardWorking()` (private)

```java
private void animateNonStandardWorking(float viewVerticalAngelOffset)
{
    md.bipedRightShoulder.ignoreSuperRotation = true;
    md.bipedRightShoulder.rotateAngleX = viewVerticalAngelOffset / RadiantToAngle;
    md.bipedRightShoulder.rotateAngleY = md.workingAngle / RadiantToAngle;
    md.bipedRightShoulder.rotateAngleZ = Half;
    md.bipedRightShoulder.rotationOrder = ModelRotationRenderer.ZYX;
    md.bipedRightArm.reset();
}
```

오른쪽 어깨만 작업(도구 사용) 애니메이션 적용. 오른팔은 reset().  
`ignoreSuperRotation = true`: 부모 회전 무시.

---

### `animateNonStandardBowAiming()` (private)

```java
private void animateNonStandardBowAiming(float totalHorizontalDistance, float currentHorizontalSpeed,
    float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
{
    md.bipedRightShoulder.ignoreSuperRotation = true;
    md.bipedRightShoulder.rotateAngleY = md.workingAngle / RadiantToAngle;
    md.bipedRightShoulder.rotateAngleZ = Half;
    md.bipedRightShoulder.rotationOrder = ModelRotationRenderer.ZYX;

    md.bipedLeftShoulder.ignoreSuperRotation = true;
    md.bipedLeftShoulder.rotateAngleY = md.workingAngle / RadiantToAngle;
    md.bipedLeftShoulder.rotateAngleZ = Half;
    md.bipedLeftShoulder.rotationOrder = ModelRotationRenderer.ZYX;

    md.bipedRightArm.reset();
    md.bipedLeftArm.reset();

    // 머리/outer 회전 임시 저장 후 0으로 설정
    float headRotateAngleY = md.bipedHead.rotateAngleY;
    float outerRotateAngleY = md.bipedOuter.rotateAngleY;
    float headRotateAngleX = md.bipedHead.rotateAngleX;

    md.bipedHead.rotateAngleY = 0;
    md.bipedOuter.rotateAngleY = 0;
    md.bipedHead.rotateAngleX = 0;

    imp.superAnimateBowAiming(totalHorizontalDistance, currentHorizontalSpeed, totalTime,
        viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);

    // 복원
    md.bipedHead.rotateAngleY = headRotateAngleY;
    md.bipedOuter.rotateAngleY = outerRotateAngleY;
    md.bipedHead.rotateAngleX = headRotateAngleX;
}
```

양쪽 어깨 모두 ignoreSuperRotation + workingAngle 설정.  
vanilla superAnimateBowAiming 호출 전 머리/outer Y 회전을 0으로 임시 재설정, 호출 후 복원.

---

### 공개 `animate*()` 메서드 — 전체 목록

모든 메서드는 동일한 파라미터 시그니처:
```java
(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime,
 float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
```

| 메서드명 | isStandard일 때 | !isStandard일 때 |
|---------|----------------|-----------------|
| `animateHeadRotation` | `setRotationAngles(...)` 후 `imp.superAnimateHeadRotation(...)` | `setRotationAngles(...)` 만 |
| `animateSleeping` | `imp.superAnimateSleeping(...)` | 아무 것도 안 함 |
| `animateArmSwinging` | isAngleJumping이면 `animateAngleJumping()`, 아니면 `imp.superAnimateArmSwinging(...)` | 아무 것도 안 함 |
| `animateRiding` | `imp.superAnimateRiding(...)` | 아무 것도 안 함 |
| `animateLeftArmItemHolding` | `imp.superAnimateLeftArmItemHolding(...)` | 아무 것도 안 함 |
| `animateRightArmItemHolding` | `imp.superAnimateRightArmItemHolding(...)` | 아무 것도 안 함 |
| `animateWorkingBody` | `imp.superAnimateWorkingBody(...)` | `isWorking()`이면 `animateNonStandardWorking(viewVerticalAngelOffset)` |
| `animateWorkingArms` | `imp.superAnimateWorkingArms(...)` | `isWorking()`이면 `imp.superAnimateWorkingArms(...)` |
| `animateSneaking` | isAngleJumping 아니면 `imp.superAnimateSneaking(...)` | 아무 것도 안 함 |
| `animateArms` | `imp.superApplyAnimationOffsets(...)` | 아무 것도 안 함 |
| `animateBowAiming` | `imp.superAnimateBowAiming(...)` | `animateNonStandardBowAiming(...)` |

**`animateHeadRotation()`만** setRotationAngles를 호출한다.

```java
public void animateHeadRotation(...)
{
    setRotationAngles(...);  // 상태 플래그 기반 뼈대 각도 계산
    if(isStandard)
        imp.superAnimateHeadRotation(...);
}
```

---

### `setArmScales()` (private)

```java
private void setArmScales(float rightScale, float leftScale)
{
    if(scaleArmType == Scale)
    {
        md.bipedRightArm.scaleY = rightScale;
        md.bipedLeftArm.scaleY  = leftScale;
    }
    else if(scaleArmType == NoScaleEnd)
    {
        md.bipedRightArm.offsetY -= (1F - rightScale) * 0.5F;
        md.bipedLeftArm.offsetY  -= (1F - leftScale) * 0.5F;
    }
}
```

`scaleArmType == Scale`: scaleY를 직접 설정.  
`scaleArmType == NoScaleEnd`: scaleY 변경 없이 offsetY를 보정. `(1 - scale) * 0.5` 만큼 Y를 이동 (스케일 없는 환경에서 시각적 보상).  
`scaleArmType == NoScaleStart` 또는 그 외: 아무 것도 안 함.

---

### `setLegScales()` (private)

```java
private void setLegScales(float rightScale, float leftScale)
{
    if(scaleLegType == Scale)
    {
        md.bipedRightLeg.scaleY = rightScale;
        md.bipedLeftLeg.scaleY  = leftScale;
    }
    else if(scaleLegType == NoScaleEnd)
    {
        md.bipedRightLeg.offsetY -= (1F - rightScale) * 0.5F;
        md.bipedLeftLeg.offsetY  -= (1F - leftScale) * 0.5F;
    }
}
```

setArmScales와 동일한 패턴, 대상이 다리.

---

### `Factor()` (static, private)

```java
private static float Factor(float x, float x0, float x1)
{
    if(x0 > x1)
    {
        // 감소 방향 (x0에서 0, x1에서 1)
        if(x <= x1) return 1F;
        if(x >= x0) return 0F;
        return (x0 - x) / (x0 - x1);
    }
    else
    {
        // 증가 방향 (x0에서 0, x1에서 1)
        if(x >= x1) return 1F;
        if(x <= x0) return 0F;
        return (x - x0) / (x1 - x0);
    }
}
```

x0~x1 구간에서 0→1(또는 1→0) 선형 보간 팩터. 범위 밖은 클램프.  
x0 > x1이면 감소 방향.

---

### `Between()` (static, private)

```java
private static float Between(float min, float max, float value)
{
    if(value < min) return min;
    if(value > max) return max;
    return value;
}
```

단순 클램프.

---

### `Normalize()` (static, private)

```java
private static float Normalize(float radiant)
{
    while(radiant > Half)
        radiant -= Whole;
    while(radiant < -Half)
        radiant += Whole;
    return radiant;
}
```

라디안 값을 [-Half, Half] 범위로 정규화. `Half` = π, `Whole` = 2π 기준.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `net.smart.render.SmartRenderContext` | 상속 — 각도 상수(Half/Quarter/…), 스케일 타입 상수 |
| `net.smart.moving.render.IModelPlayer` (imp) | super animate 메서드 위임 |
| `net.smart.render.SmartRenderModel` (md) | 뼈대 노드(bipedOuter/Torso/…) 접근, 현재 카메라·이동 각도·속도 |
| `ModelBiped` (mp) | `mp.onGround` — isWorking() 판정 |
| `SmartMovingRender.CurrentMainModel` | 생성 시 상태 복사 원본 |
| `ModelRotationRenderer` | 뼈대 노드 타입, 회전 순서 상수(YZX/XZY/ZXY/YXZ 등) |
| `MathHelper` | `MathHelper.cos()` |

---

## R-05 리서치 결과 추가 (A-15, A-16)

### A-16 — isRopeSliding 분기

**소스 위치**: `SmartMovingModel.java` → `setRotationAngles()` 첫 번째 분기 (lines 269~301, 이미 본 문서 "1. isRopeSliding" 섹션에 완전 기록됨)

**추가 확인 사항 없음** — 해당 분기 전체가 이미 정확하게 기록되어 있음.

---

### A-15 — SmartRenderModel 계층 구조 (pivot 값 포함)

**소스 위치**: `SmartRenderModel.java` (net.smart.render) → 생성자 전체 직접 확인

**계층 구조 및 초기 pivot (확인됨)**:

```
bipedOuter          parent=null   pivot=(0, 0, 0)    fadeEnabled=true
  └─ bipedTorso     parent=bipedOuter   pivot=(0, 0, 0)
       ├─ bipedBody  parent=bipedTorso   pivot=(0, 0, 0)   [mesh: originalBipedBody 복사]
       ├─ bipedBreast parent=bipedTorso  pivot=(0, 0, 0)
       │    ├─ bipedNeck    parent=bipedBreast  pivot=(0, 0, 0)
       │    │    └─ bipedHead  parent=bipedNeck  pivot=(0, 0, 0)  [mesh: originalBipedHead 복사]
       │    │         ├─ bipedEars      parent=bipedHead  pivot=(0, 0, 0)
       │    │         └─ bipedHeadwear  parent=bipedHead  pivot=(0, 0, 0)  [mesh: originalBipedHeadwear]
       │    ├─ bipedCloak         parent=bipedBreast  pivot=(0, 0, 2F)
       │    ├─ bipedRightShoulder parent=bipedBreast  pivot=(-5F, 2F, 0)
       │    │    └─ bipedRightArm  parent=bipedRightShoulder  pivot=(0, 0, 0)  [mesh: originalBipedRightArm]
       │    └─ bipedLeftShoulder  parent=bipedBreast  pivot=(5F, 2F, 0)   mirror=true
       │         └─ bipedLeftArm   parent=bipedLeftShoulder   pivot=(0, 0, 0)  [mesh: originalBipedLeftArm]
       └─ bipedPelvic  parent=bipedTorso  pivot=(0, 12F, 0)
            ├─ bipedRightLeg  parent=bipedPelvic  pivot=(-2F, 0, 0)  [mesh: originalBipedRightLeg]
            └─ bipedLeftLeg   parent=bipedPelvic  pivot=(2F, 0, 0)   [mesh: originalBipedLeftLeg]
```

**초기 rotateAngle**: 모두 0.0F  
- `create()` = `new ModelRotationRenderer(mp, i, j, base)` — rotateAngle 기본값 0  
- `copy()` — childModels/cubeList/mirror/isHidden/showModel만 복사, rotationPoint·rotateAngle은 복사하지 않음

**핵심 pivot 값**:

| 노드 | pivotX | pivotY | pivotZ | 비고 |
|------|--------|--------|--------|------|
| bipedOuter | 0 | 0 | 0 | root, fadeEnabled |
| bipedTorso | 0 | 0 | 0 | 상체 그룹 루트 |
| bipedBody | 0 | 0 | 0 | |
| bipedBreast | 0 | 0 | 0 | |
| bipedNeck | 0 | 0 | 0 | |
| bipedHead | 0 | 0 | 0 | |
| bipedEars | 0 | 0 | 0 | |
| bipedHeadwear | 0 | 0 | 0 | |
| bipedCloak | 0 | 0 | 2F | Z=2 (등 쪽) |
| bipedRightShoulder | -5F | 2F | 0 | X=-5 (오른쪽) |
| bipedRightArm | 0 | 0 | 0 | Shoulder 기준 |
| bipedLeftShoulder | 5F | 2F | 0 | X=+5 (왼쪽), mirror=true |
| bipedLeftArm | 0 | 0 | 0 | Shoulder 기준 |
| bipedPelvic | 0 | 12F | 0 | Y=12 (허리 아래) |
| bipedRightLeg | -2F | 0 | 0 | Pelvic 기준 |
| bipedLeftLeg | 2F | 0 | 0 | Pelvic 기준 |
| `HandsClimbing` | `MiddleGrab`, `UpGrab`, `NoGrab` 상수 |
| `FeetClimbing` | `NoStep` 상수 |
| `Block` | `overGroundBlock.getMaterial().isSolid()` |

---

## 주요 관찰 사항

1. **단일 진입점**: 모든 `animate*()` 메서드 중 `animateHeadRotation()`만 `setRotationAngles()`를 호출한다. 따라서 `animateHeadRotation()`이 반드시 먼저 호출되어야 이후 모든 메서드의 `isStandard` 판정이 올바르다.

2. **isStandard 패턴**: `setRotationAngles()` 진입 시 `isStandard = false`로 리셋, 마지막 `else`에서만 `true`. 이 값에 따라 모든 `animate*()` 메서드가 SM 커스텀 vs vanilla super 중 택일.

3. **상태 우선순위** (if-else 체인 순서):  
   isRopeSliding → isClimb/isCrawlClimb → isClimbJump → isCeilingClimb → isSwim → isDive → isCrawl → isSlide → isFlying → isHeadJump → isFalling → else(isStandard)

4. **`@SuppressWarnings("unused")`**: `setRotationAngles()`의 `factor` 파라미터가 메서드 본문에서 사용되지 않아 IDE 경고 억제. 파라미터 제거 대신 경고 억제로 인터페이스 일관성 유지.

5. **currentHorizontalSpeedFlattened**: NaN이면 파라미터 currentHorizontalSpeed를 그대로 사용, NaN 아니면 오버라이드. isCrawl에서는 `currentHorizontalSpeedFlattened` 필드를 직접 Factor 인자로 전달(파라미터 오버라이드 전 값).

6. **스케일 시스템**: `Scale`(scaleY 직접 설정), `NoScaleStart`(스케일 없이 진입, setLegScales/setArmScales 호출 자체 스킵), `NoScaleEnd`(스케일 없이, offsetY 보정으로 대체). `SmartRenderContext`에서 상수값 정의.

7. **1.21.1 이식 관련**:
   - `ModelBiped` → `PlayerEntityModel` (Fabric 기준)
   - `ModelRotationRenderer` → 커스텀 구현 필요 (vanilla에 없음)
   - `SmartRenderModel`의 bipedOuter/Torso 등 확장 뼈대 구조 전체가 SM/SR 자체 정의
   - `MathHelper.cos()` → `MathHelper.cos()` (Fabric yarn 동일)
   - `mp.onGround` → `PlayerEntityModel`에 동등 필드 존재 여부 확인 필요
   - `animateHeadRotation()` 등의 호출 순서는 SmartMovingModelPlayerBase에서 결정됨
