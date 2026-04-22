# 점프 전체 — 교차 분석

원본 소스: SmartMoving (1.7.10 Forge) + SmartRender  
대응 대상: vanilla 1.21.1 Fabric (Yarn 매핑)  
관련 원본 파일: SmartMovingSelf.md, SmartMoving.md, SmartMovingBase.md, SmartMovingContext.md, Button.md, SmartMovingModel.md, SmartMovingRender.md  
관련 vanilla 파일: LivingEntity_jump.md, PlayerEntity_pose_dimensions.md, Entity_calculateDimensions.md, PlayerEntityRenderer_setupTransforms.md, PlayerEntityModel_setAngles.md, PlayerEntityRenderer_getPositionOffset.md

---

## 1. SM 원본 점프 시스템 개요

SM은 vanilla의 `jump()` 메서드를 완전히 차단하고 자체 점프 파이프라인으로 대체한다.

### 1-1. jump() 인터셉트

**원본 동작**:
```java
// SmartMovingSelf.java:3225
@Override
public void jump() {
    jumpAvoided = true;
    jumpPending = true;
}
```
- vanilla `jump()`가 호출될 때 실제 속도 변경 없이 플래그만 세팅
- `jumpPending`이 true이면 다음 `handleJumping()` 실행 시 실제 점프 처리
- `jumpAvoided`는 vanilla 점프가 "회피되었음"을 표시

**1.21.1 대응**:
- `LivingEntity.jump()` (method_6043) 또는 `PlayerEntity.jump()`에 `@Inject` Mixin 적용
- SM 로직이 활성화된 상태에서는 `@Inject(at = @At("HEAD"), cancellable = true)`로 vanilla 본문 취소
- 취소 후 `jumpAvoided = true; jumpPending = true` 세팅

**동작 차이**:
- vanilla: `jump()` 호출 시 Y 속도 즉시 교체 (`setVelocity(x, getJumpVelocity(), z)`)
- SM: `jump()` 호출 시 아무 속도 변경 없이 플래그만 세팅, 실제 속도는 `handleJumping() → tryJump()`에서 처리

**포팅 주의사항**:
- `jumpPending` 처리가 `handleJumping()`보다 먼저 실행되는 틱 순서를 유지해야 한다
- vanilla `PlayerEntity.jump()`는 `super.jump()` 후 `addExhaustion()`을 추가 호출한다. SM 인터셉트 시 이 exhaustion도 함께 차단된다. SM 자체 exhaustion 시스템(`Config.getJumpExhaustionGain`)이 대신 적용된다
- `Stats.JUMP` 통계 기록도 vanilla `PlayerEntity.jump()`에서 수행하므로, SM이 jump()를 차단하면 통계 기록도 별도 처리 필요

---

### 1-2. updateEntityActionState() — jumping 필드 제어

**원본 동작**:
```java
isp.setIsJumpingField(
    esp.movementInput.jump
    && !isCrawling
    && !isSliding
    && !(Config.isHeadJumpingEnabled() && grabButton.Pressed && sp.isSprinting())
    && !(Config.isJumpChargingEnabled() && wouldIsSneaking && sp.onGround && isStanding)
    && !blockJumpTillButtonRelease
);
```
- vanilla의 `jumping` 필드(`field_6282`)를 직접 조작
- SM 전용 조건들이 추가됨: 크롤링 중, 슬라이딩 중, 헤드점프 차지 중, 점프 차지 중, 버튼 릴리즈 대기 중에는 jumping=false

**1.21.1 대응**:
- `LivingEntity.jumping` (field_6282, intermediary)에 해당하는 필드를 Mixin으로 조작
- `ClientPlayerEntity.tickMovement()` 또는 `KeyboardInput` 처리 시점에 Mixin 적용
- `isp.setIsJumpingField()` → Mixin으로 `this.jumping = ...` 직접 설정

**동작 차이**:
- vanilla: `jumping = input.jumping` (단순 입력 반영)
- SM: jumping 필드가 다수의 SM 상태 조건으로 필터링됨

**포팅 주의사항**:
- `jumping` 필드 조작 타이밍: `tickMovement()`의 점프 분기 실행 전에 반드시 세팅되어야 함
- `blockJumpTillButtonRelease` 구현이 필요. 점프 버튼을 뗄 때까지 다음 점프를 차단하는 로직

---

### 1-3. handleJumping() — 점프 판정 진입점

원본 위치: SmartMovingSelf.java:1842-1943

**점프 타입별 처리 흐름**:

#### 차지 점프 (Charge Jump)
```
isJumpChargingPossible = onGround && isStanding
isJumpCharging = isJumpChargingPossible && wouldIsSneaking && Config.isJumpChargingEnabled()
→ Sneak 버튼 홀드 중: jumpCharge 누적 (최대 Config.MaxJumpCharge)
→ 버튼 뗌(StopPressed): tryJump(Config.ChargeUp, ...)
```
- `jumpCharge`: 0.0~1.0 누적, 최대 `Config.MaxJumpCharge`
- 차지 중 `blockJumpTillButtonRelease = true` 설정

**1.21.1 대응**:
- `KeyBinding` (Sneak) 상태를 매 틱 확인
- Mixin으로 `tickMovement()` 내부에 차지 로직 삽입
- `jumpCharge` 필드를 SM 컴포넌트에 보관

#### 헤드 점프 차지 (Head Jump Charge)
```
isHeadJumpCharging = grabButton.Pressed && (isGroundSprinting || isSprintJump || isRunning) && !isCrawling
→ Config.isHeadJumpingEnabled() 필요
→ 버튼 뗌: tryJump(Config.HeadUp, ...)
```
- headJumpCharge 누적 (최대 Config.MaxHeadJumpCharge)

**1.21.1 대응**:
- `grabButton` = SM 전용 Grab 키바인딩. 1.21.1에서 Fabric `KeyBinding` 등록
- `grabButton.Pressed` = `keyBindGrab.isPressed()`

#### 수면 점프 (Surface Jump, isDipping)
```
isDipping && jumpButton.StartPressed && (posY - floor(posY)) > (isSlow ? 0.37 : 0.6):
    motionY -= 0.0399999...
    if(onGround): tryJump(Config.Up, true, null, null)
```
- 수면에서 점프 시 약간의 Y 속도 감소 후 지상 점프 처리

**1.21.1 대응**:
- `isDipping` 판정 로직 SM이 자체 계산
- motionY → `player.setVelocity(x, currentVelocity.y - 0.04, z)` 형태로 적용

#### 일반 점프
```
!blockJumpTillButtonRelease && !isJumpCharging && !isHeadJumpCharging && !isVineAnyClimbing
&& jumpButton.StartPressed (→ jumpPending이 true)
→ tryJump(Config.Up, false, null, null)
```
- 방향 점프 각도 계산: `angleJumpType = ((360 - movementAngle) / 45) % 8`
- angleJumpType > 1 && < 7 → isAngleJumping()=true → tryJump(Config.Up, false, null, angle)

| 이동 방향 | movementAngle | angleJumpType |
|----------|--------------|---------------|
| 좌 | 270° | 6 |
| 좌+후 | 225° | 5 |
| 우 | 90° | 2 |
| 우+후 | 135° | 3 |
| 후 | 180° | 4 |
| 전 (직진) | 0° | 0 (각도 점프 아님) |

---

### 1-4. tryJump() — 실제 속도 계산

원본 위치: SmartMovingSelf.java:1999-2136

```java
boolean up   = type == Config.Up || type == Config.ChargeUp || type == Config.HeadUp || ...
boolean head = type == Config.HeadUp || type == Config.ClimbBackHead || ...
boolean side = type == Config.Left || type == Config.Right || ...

// verticalMotion 기본 공식
double verticalMotion = -0.078 + 0.498 * verticalJumpFactor * jumpChargeFactor;

// vanilla Up 점프 (inWater=false, type=Config.Up)
double verticalMotion = 0.41999998688697815D + potionJump * 0.1F;
// potionJump = getActivePotionEffect(JUMP_BOOST).getAmplifier() (없으면 0)
```

**스프린트 점프 수평 보정**:
```java
motionX -= Math.sin(Math.toRadians(rotationYaw)) * 0.2F;
motionZ += Math.cos(Math.toRadians(rotationYaw)) * 0.2F;
isSprintJump = isFast;
```
→ vanilla jump()의 스프린트 가속과 동일한 방향/수치. SM이 자체 계산하므로 vanilla와 중복되지 않도록 주의

**헤드 점프 각도 재계산**:
```java
float normalAngle = (float) Math.atan(verticalMotion / horizontalSpeed);
float newAngle = Config.getHeadJumpFactor() * normalAngle;
verticalMotion = totalMotion * Math.sin(newAngle);
horizontalMotion = totalMotion * Math.cos(newAngle);
```
- 더 가파른 각도로 재조정

**각도 점프 수평 벡터**:
```java
// angleJump 방향 벡터
double jumpX = -Math.sin(jumpAngle / RadiantToAngle);
double jumpZ =  Math.cos(jumpAngle / RadiantToAngle);
motionX = getJumpMoving(jumpMotionX, moveX, reset, horizontal, horizontalJumpFactor);
motionZ = getJumpMoving(jumpMotionZ, moveZ, reset, horizontal, horizontalJumpFactor);
```

**tryJump() 결과 적용**:
```java
sp.motionY = verticalMotion;
addStat(jumpStat);
isSprintJump = isFast;
exhaustion += Config.getJumpExhaustionGain(speed, type, jumpCharge);
if(head) {
    isHeadJumping = true;
    setHeightOffset(-1F);  // bounding box minY += 1F, height += 1F → 천장에 머리가 닿는 효과
}
isAirBorne = true;
isJumping = true;
onLivingJump();  // ForgeHooks.onLivingJump()
```

**1.21.1 대응**:
- `sp.motionY = verticalMotion` → `player.setVelocity(player.getVelocity().x, verticalMotion, player.getVelocity().z)` + `player.velocityDirty = true`
- `setHeightOffset(-1F)` → 포즈/치수 시스템으로 대체 (아래 참조)
- `onLivingJump()` → Fabric의 `LivingEntityJumpCallback` 또는 이벤트 없이 직접 처리

**동작 차이**:
- vanilla `jump()`: Y 속도 = `GENERIC_JUMP_STRENGTH(0.42) * jumpVelocityMultiplier + jumpBoostModifier`
- SM: Y 속도 = `-0.078 + 0.498 * verticalJumpFactor * jumpChargeFactor` (별도 공식)
- 일반 Up 점프 시에만 vanilla 수치(0.42) 사용. 나머지 타입은 SM 공식 사용

**포팅 주의사항**:
- SM이 vanilla `jump()` 를 인터셉트하므로 `velocityDirty = true` 수동 세팅 필요 (서버 속도 동기화)
- `JUMP_BOOST` 포션: vanilla `getJumpBoostVelocityModifier()`와 별도로 SM이 직접 읽어야 함
- 점프 차지 수치(`jumpCharge`) 직렬화 필요 (서버가 동일 값을 알아야 검증 가능)

---

### 1-5. getJumpMoving() — 각도 점프 수평 속도 계산

```java
private static double getJumpMoving(double actual, double move, boolean reset,
                                     double horizontal, float horizontalJumpFactor) {
    if (!reset)
        return actual + move * horizontal;
    else if (Math.signum(actual) != Math.signum(move))
        return move * horizontalJumpFactor;
    else
        return Math.max(Math.abs(actual), Math.abs(move) * horizontal) * Math.signum(move);
}
```

| 케이스 | 조건 | 결과 |
|-------|------|------|
| reset=false | 방향 유지 | 현재 속도 + 입력 * horizontal |
| reset=true, 반대 방향 | 방향 전환 | 입력 * horizontalJumpFactor |
| reset=true, 같은 방향 | 방향 유지 | max(현재, 입력*horizontal) |

**1.21.1 대응**: 순수 수학 함수, 이식 그대로 가능.

---

### 1-6. handleWallJumping() — 벽 점프

원본 위치: SmartMovingSelf.java:1946-1997

```java
// 벽 반사 각도 계산
float reflectedAngle = horizontalCollisionAngle * 2 - movementAngle + 180F;
// 직교 방향으로 반올림 (45° 단위)
float jumpAngle = Math.round(reflectedAngle / 90F) * 90F;

// 성공 조건: 충돌 방향이 이동 방향의 앞쪽
if(collisionInMovementDirection) {
    continueWallJumping = !isHeadJumping;
    isCollidedHorizontally = false;
    rotationYaw = jumpAngle;
    onStartWallJump(jumpAngle);  // isWallJumping = true; fallDistance = 0F
    tryJump(wallJumpType, ...);
}
```

**1.21.1 대응**:
- `horizontalCollisionAngle` 계산: Orientation.java의 로직 이식 (이전 클라이밍 분석 참조)
- `isCollidedHorizontally` → `player.horizontalCollision` (Entity 필드, Yarn: `field_6025`)
- `sp.fallDistance = 0F` → `player.fallDistance = 0` (Entity 필드, Yarn: `field_6023`)
- `rotationYaw = jumpAngle` → `player.setYaw(jumpAngle)` + `player.bodyYaw = jumpAngle`

**포팅 주의사항**:
- `wasCollidedHorizontally`/`continueWallJumping` 플래그 상태 유지가 틱 순서에 의존함
- 벽 점프 후 `isCollidedHorizontally = false` 강제 세팅으로 vanilla 충돌 판정 무력화

---

### 1-7. heightOffset 시스템 (헤드 점프 bounding box)

**원본 동작**:
```java
private void setHeightOffset(float offset) {
    resetHeightOffset();
    if(offset == 0F) return;
    heightOffset = offset;
    sp.boundingBox.minY -= heightOffset;  // offset=-1F → minY += 1F (아래로 확장)
    sp.height += heightOffset;            // offset=-1F → height += 1F → 높이 증가
}
```
- 헤드 점프 시 `setHeightOffset(-1F)`: bounding box의 minY를 1블록 내려 플레이어가 1블록 낮은 공간에서 시작한 것처럼 처리
- 착지/해제 시 `resetHeightOffset()` 호출

**1.21.1 대응 (차이 큼)**:
- 1.21.1에서 `boundingBox`를 직접 조작하는 대신 포즈/치수 시스템 사용
- `getBaseDimensions(EntityPose)` (method_55694) Mixin으로 SM 헤드점프 포즈용 치수 반환
- SM 전용 커스텀 `EntityPose`를 등록하거나, SWIMMING 포즈 재활용 후 `getBaseDimensions`에서 다른 치수 반환
- `setPose(customPose)` → `calculateDimensions()` 자동 트리거 → bounding box 재계산

**동작 차이**:
- 원본: `boundingBox.minY` 직접 조작 (Forge 1.7.10)
- 1.21.1: `EntityDimensions` 기반 불변 치수 시스템. 직접 박스 조작 불가
- `LivingEntity.getDimensions(EntityPose)` 는 **final** → `getBaseDimensions()` Mixin 필수

**포팅 주의사항**:
- 헤드점프 시 눈 높이(eyeHeight) 변화: SWIMMING 포즈 사용 시 0.4로 낮아짐 → 카메라 위치 변경 영향
- 헤드점프 → 착지 시 `standUp()`: SWIMMING → STANDING 포즈 전환 필요. PlayerEntity는 `recalculateDimensions()`가 **절대 호출되지 않으므로** 공간 체크를 SM이 직접 수행해야 함
- `getPositionOffset()`: CROUCHING 포즈 사용 시 Y -0.125 자동 오프셋 적용됨. 헤드점프에 CROUCHING을 쓰면 추가 오프셋 주의

---

## 2. 애니메이션 교차 분석

### 2-1. isFlying 애니메이션

**원본 동작** (SmartMovingModel.java):
```
bipedOuter.rotateAngleX = (Quarter - verticalAngle) * walkFactor
bipedOuter.rotateAngleY = horizontalAngle
bipedHead.rotateAngleX  = -outer.X / 2
```
- `isJump` 일 때: `verticalAngle = Math.abs(currentVerticalAngle)` (절대값)
- `currentVerticalAngle` = `SmartStatistics`에서 보간된 수직 속도 각도

**1.21.1 대응**:
- `PlayerEntityModel.setAngles()` (method_17087) 이후 Mixin `@Inject`로 파트 값 덮어쓰기
- 1.21.1 모델 파트: `head`, `body`, `rightArm`, `leftArm`, `rightLeg`, `leftLeg` (BipedEntityModel 필드)
- 파트 각도 필드: `pitch`(X축), `yaw`(Y축), `roll`(Z축) (라디안 단위)
- `bipedOuter` → SM이 자체 논리적 outer 개념을 body 또는 root 파트로 대응해야 함

**포팅 주의사항**:
- `setAngles()` 후 SM Mixin이 덮어쓰는 방식이어야 vanilla 공식과 충돌 없음
- `leaningPitch > 0` 이면 BipedEntityModel Step 13에서 수영 팔 애니메이션이 추가 적용됨. SM이 leaningPitch를 직접 제어하지 않으면 예상치 않은 팔 자세 발생
- SM 애니메이션이 활성화된 상태에서 `model.leaningPitch = 0` 으로 유지하면 Step 13 블록 전체를 건너뜀

---

### 2-2. isHeadJump 애니메이션

**원본 동작** (SmartMovingModel.java):
```
bipedOuter.fadeRotateAngleX = true  (부드러운 전환)
bipedOuter.rotateAngleX = Quarter - currentVerticalAngle
bipedOuter.rotateAngleY = currentHorizontalAngle
bipedHead.rotateAngleX  = -outer.X / 2

bendFactor = min(Factor(vAngle, Quarter, 0), Factor(vAngle, -Quarter, 0))
armFactorZ = Factor(vAngle, Quarter, -Quarter)
if(solid block above): armFactorZ = min(armFactorZ, smallOverGroundHeight / 5F)
bipedRightArm.rotateAngleZ = Half - Sixteenth + armFactorZ * Eighth
bipedLeftArm.rotateAngleZ  = Sixteenth - Half - armFactorZ * Eighth
legFactorZ = Factor(vAngle, -Quarter, Quarter)
bipedRightLeg.rotateAngleZ = Sixtyfourth * legFactorZ
bipedLeftLeg.rotateAngleZ  = -Sixtyfourth * legFactorZ
```

**smallOverGroundHeight** (SmartMovingRender.java):
```java
float smallOverGroundHeight = isCrawlClimb || isHeadJump
    ? (float) moving.getOverGroundHeight(5D) : 0F;
Block overGroundBlock = isHeadJump && smallOverGroundHeight < 5F
    ? moving.getOverGroundBlockId(smallOverGroundHeight) : null;
```
- 최대 5블록 범위에서 위 블록까지의 거리 측정
- 블록이 있으면 팔 Z 각도 제한 (공간에 맞게 팔을 접음)

**1.21.1 대응**:
- `getOverGroundHeight()` 로직: `world.getBlockState(BlockPos)`로 위 블록 탐색
- `setAngles()` 이후 파트 값 Mixin으로 덮어쓰기
- `Factor(x, x0, x1)` = SM 선형 보간 유틸: `(x - x0) / (x1 - x0)` (0~1 클램프)

---

### 2-3. isFalling 애니메이션

**원본 동작** (SmartMovingModel.java):
```
distance = totalDistance * 0.1F  (누적 낙하 거리)
arm X, arm Z, arm Y: cos 기반 진동
leg X: cos(distance + Half + Quarter) * Sixteenth + Thirtytwoth  (right)
       cos(distance + Quarter)         * Sixteenth + Thirtytwoth  (left)
```

**doFallingAnimation() 조건** (SmartMovingSelf.java):
```java
if(Config.isFallAnimationEnabled())
    return !sp.onGround && sp.fallDistance > Config._fallAnimationDistanceMinimum.value;
return false;
```

**1.21.1 대응**:
- `entity.isOnGround()` + `entity.fallDistance` (Entity 필드, Yarn: `field_6023`)
- 낙하 거리 최솟값 = `Config._fallAnimationDistanceMinimum.value`

---

### 2-4. isClimbJump 애니메이션

**원본 동작** (SmartMovingModel.java):
```
bipedRightArm.rotateAngleX = Half + Sixteenth
bipedLeftArm.rotateAngleX  = Half + Sixteenth
bipedRightArm.rotateAngleZ = -Thirtytwoth
bipedLeftArm.rotateAngleZ  = Thirtytwoth
```
- 클라이밍 중 위로 점프할 때 팔을 위로 들어올리는 자세

**1.21.1 대응**:
- `setAngles()` 후 파트 pitch/roll 덮어쓰기

---

### 2-5. animateAngleJumping()

**원본 동작** (SmartMovingModel.java):
```java
float angle = angleJumpType * Eighth;  // Eighth = π/4
float backness  = 1F - Math.abs(angle - Half) / Quarter;
float leftness  = -Math.min(angle - Half, 0F) / Quarter;
float rightness =  Math.max(angle - Half, 0F) / Quarter;
// pelvis(bipedBody) Y 재설정 + 다리/팔 방향 각도 조정
```
- 호출 위치: `animateArmSwinging()` 내부, isStandard && isAngleJumping()=true 일 때
- isAngleJumping() = `angleJumpType > 1 && angleJumpType < 7`

**1.21.1 대응**:
- `setAngles()` Mixin에서 SM isAngleJumping 상태이면 이 계산 추가 적용
- `angleJumpType` 값은 SM 네트워크 패킷에서 수신 (3비트)

---

### 2-6. setupTransforms 관련

**SM 원본** (SmartMovingRender.java의 `rotatePlayer()`):
```java
if(moving.isHeadJumping || moving.isFlying || moving.isSwimming || ...)
    entityplayer.renderYawOffset = forwardRotation;
```
- 헤드점프/비행/수영 등 상태에서 몸통 yaw를 이동 방향으로 고정

**1.21.1 대응**:
- `setupTransforms()` 의 `super.setupTransforms()` 내부에서 `bodyYaw` 파라미터로 방향이 결정됨
- SM이 `bodyYaw`를 변경하려면 `setupTransforms()` @Inject 시 파라미터 수정 또는 별도 처리
- `PlayerEntityRenderer.setupTransforms()` Branch 2 (leaningPitch > 0): `translate(0, -1, 0.3)` 적용. 헤드점프가 SWIMMING 포즈를 쓰면 이 오프셋도 자동 적용됨

---

### 2-7. setAngles 충돌 분석

**leaningPitch 간섭**:
- `BipedEntityModel.leaningPitch > 0` 이면 Step 13 수영 팔 애니메이션 실행
- SM 크롤링/헤드점프가 SWIMMING 포즈를 사용하면 `entity.getLeaningPitch()` 가 증가
- SM 자체 팔 자세와 vanilla 수영 팔 애니메이션이 충돌 → `model.leaningPitch = 0` 강제 세팅 또는 Mixin으로 Step 13 취소 필요

**sneaking 간섭**:
- `model.sneaking = true` 이면 모든 파트의 pivotY 이동, body.pitch = 0.5, 팔 pitch +0.4
- SM이 CROUCHING 포즈 미사용 시에도 렌더러가 `model.sneaking = entity.isInSneakingPose()` 세팅함
- SM이 CROUCHING 포즈를 쓰는 동작(느린 이동 등)에서 sneaking 애니메이션 자동 적용됨

---

## 3. 렌더링 교차 분석

### 3-1. heightOffset 렌더 처리

**원본 동작** (SmartMovingRender.java):
```java
// renderPlayerAt()에서
if(moving.heightOffset != 0) {
    d1 += moving.heightOffset;  // 헤드점프 시 heightOffset=-1F → Y += -1
}
// renderName()에서
if(moving.heightOffset == -1) {
    d1 -= 0.2F;  // 이름표 위치 보정
}
```

**1.21.1 대응**:
- `getPositionOffset()` (method_23206) Mixin으로 SM 헤드점프 상태 시 Y 오프셋 반환
- vanilla `getPositionOffset()`은 CROUCHING 시 `Vec3d(0, -0.125, 0)` 반환
- SM 헤드점프용 포즈가 SWIMMING이면 `getPositionOffset() = Vec3d.ZERO` (isInSneakingPose() = false)
- 별도 Y 오프셋이 필요하면 `getPositionOffset()` Mixin 필수

**포팅 주의사항**:
- `getPositionOffset()`의 오프셋은 `EntityRenderDispatcher.render()`에서 `matrices.translate()` 전에 적용
- 이후 `setupTransforms()` → Branch 2에서 추가 `translate(0, -1, 0.3)` (isInSwimmingPose 시) 누적됨
- 두 오프셋 누적 계산 필요

---

### 3-2. HUD 렌더링 (차지 바)

**원본 동작** (SmartMovingRender.java의 `renderGuiIngame()`):
- SM 독자 HUD: `jumpCharge` 바 + `exhaustion` 바 렌더링
- `jumpCharge / Config.MaxJumpCharge` 비율로 바 길이 계산

**1.21.1 대응**:
- Fabric `HudRenderCallback` 이벤트 (또는 `DrawContext` Mixin)로 HUD 추가
- `DrawContext.drawTexture()` 또는 `drawHorizontalLine()` 등으로 바 렌더링

---

## 4. 네트워크/직렬화 교차 분석

### 4-1. 점프 관련 비트 (addToSendQueue)

원본에서 SM 패킷(33비트 중 점프 관련):

| 비트 | 필드 | 설명 |
|-----|------|------|
| 1 | `isWallJumping` | 벽 점프 상태 |
| 1 | `isFast` / `isSprintJump` | 스프린트 점프 상태 |
| 1 | `isClimbBackJumping` | 클라이밍 뒤 점프 |
| 1 | `isClimbJumping` | 클라이밍 위 점프 |
| 3 | `angleJumpType` | 방향 점프 타입 (0~7) |
| 1 | `isHeadJumping` | 헤드 점프 상태 |
| 1 | `doFlyingAnimation()` | 비행 애니메이션 |
| 1 | `doFallingAnimation()` | 낙하 애니메이션 |
| 1 | `jumping` (isp.getIsJumpingField) | vanilla jumping 필드 |

**1.21.1 대응**:
- SM 전용 커스텀 패킷 (`PacketByteBuf` 기반) 구성
- `FabricPacket` 또는 `ClientToServerPacket` 형태
- `angleJumpType` 3비트 → `buf.writeByte(angleJumpType & 0x7)` 형태

---

## 5. 포팅 주의사항 종합

| 항목 | SM 원본 | 1.21.1 대응 방법 | 위험도 |
|-----|--------|----------------|-------|
| jump() 인터셉트 | PlayerBase 재정의 | `@Inject` + `ci.cancel()` | 높음 |
| Y 속도 직접 세팅 | `sp.motionY = v` | `setVelocity(x, v, z)` + `velocityDirty = true` | 중간 |
| jumping 필드 제어 | `isp.setIsJumpingField()` | Mixin accessor로 `this.jumping` 세팅 | 중간 |
| jumpingCooldown | 없음 (SM이 처리) | vanilla 10틱 쿨다운이 SM 다중 점프 차단 가능 → vanilla jump() 우회로 해결 | 높음 |
| heightOffset | `boundingBox.minY` 직접 조작 | `getBaseDimensions()` Mixin + 포즈 전환 | 높음 |
| 스프린트 점프 0.2F | SM 자체 계산 | vanilla jump()가 추가하는 0.2F 차단 필수 (jump() 인터셉트로 해결) | 높음 |
| onLivingJump() | `ForgeHooks.onLivingJump()` | Fabric 이벤트 없음 → 직접 호출 또는 생략 | 낮음 |
| addExhaustion | SM 자체 시스템 | PlayerEntity.jump()의 exhaustion 차단 필수 (jump() 인터셉트로 해결) | 중간 |
| Button 입력 | LWJGL2 기반 에지 감지 | `KeyBinding.wasPressed()` (rising edge), 직접 `WasPressed`/`Pressed` 구현 | 낮음 |
| angleJumpType 3비트 직렬화 | Forge 패킷 | Fabric `PacketByteBuf` | 낮음 |
| 헤드점프 렌더 Y 오프셋 | `renderPlayerAt()` 직접 오프셋 | `getPositionOffset()` Mixin | 중간 |
| leaningPitch 간섭 | 해당 없음 (Forge) | SWIMMING 포즈 사용 시 자동 증가 → `model.leaningPitch` 강제 0 또는 수영 팔 취소 Mixin | 높음 |
| recalculateDimensions 없음 | 직접 박스 조작 | 포즈 전환 시 공간 체크를 SM이 직접 수행 필요 | 중간 |
| getDimensions final | 없음 | `getBaseDimensions()` Mixin이 유일한 진입점 | 중간 |
| HUD 차지 바 | `renderGuiIngame()` | `HudRenderCallback` | 낮음 |

---

## 6. 확인 완료 항목 (구 미확인)

### jumpMotionX/Z 저장 타이밍 (A-07 확인 완료)

`handleJumping()` 메서드 최상단에서 `tryJump()` 호출 전에 무조건 저장.

```java
// SmartMovingSelf.handleJumping() 내부
jumpMotionX = sp.motionX;  // tryJump() 호출 전, 단 한 곳에서만 저장
jumpMotionZ = sp.motionZ;
```

`tryJump()` 내부에서는 저장하지 않고 읽기만 함.
`handleWallJumping()`에서 벽 반사 각도 계산에 사용:
```java
float movementAngle = getAngle(jumpMotionZ, -jumpMotionX);
jumpAngle = horizontalCollisionAngle * 2 - movementAngle + 180F;
```

---

### 더블클릭 카운터 임계값 (A-05 확인 완료)

`Options.angleJumpDoubleClickTicks()` = `(int)Math.ceil(_angleJumpDoubleClickTicks.value)`
`_angleJumpDoubleClickTicks.up(3F, 2F)` → 기본값 **3틱**, 최솟값 2틱

카운터 동작:
```java
if(leftButton.StartPressed) {
    if(leftJumpCount == 0)
        leftJumpCount = Options.angleJumpDoubleClickTicks(); // 첫 클릭: 타이머 세팅
    else
        leftJumpCount = -1; // 두 번째 클릭: 발동 예약
} else if(leftJumpCount > 0)
    leftJumpCount--;  // 매 틱 감소
// rightJumpCount, backJumpCount 동일 패턴
```

발동 조건 (`handleJumping()` 내):
```java
int left = 0, back = 0;
if(leftJumpCount == -1)  left++;
if(rightJumpCount == -1) left--;
if(backJumpCount == -1)  back++;

if(left != 0 || back != 0) {
    int angle;
    if(left > 0)      angle = back == 0 ? 270 : 225;
    else if(left < 0) angle = back == 0 ? 90  : 135;
    else              angle = 180;
    if(tryJump(Config.Angle, null, null, sp.rotationYaw + angle))
        angleJumpType = ((360 - angle) / 45) % 8;
    leftJumpCount = 0; rightJumpCount = 0; backJumpCount = 0;
}
```

대각선 우선순위 처리 (좌/우와 뒤가 동시 -1 → -2 대기):
```java
if(rightJumpCount == -2 && backJumpCount <= 0) rightJumpCount = -1;
if(leftJumpCount  == -2 && backJumpCount <= 0) leftJumpCount  = -1;
if(backJumpCount  == -2 && (leftJumpCount <= 0 || rightJumpCount <= 0)) backJumpCount = -1;

if(rightJumpCount == -1 && backJumpCount > 0) rightJumpCount = -2;
if(leftJumpCount  == -1 && backJumpCount > 0) leftJumpCount  = -2;
if(backJumpCount  == -1 && (leftJumpCount > 0 || rightJumpCount > 0)) backJumpCount = -2;
```

각도 점프 가능 조건:
```java
boolean canAngleJump = !isSleeping && sp.onGround && !isCrawling && !isClimbing
                       && !isClimbCrawling && !isSwimming && !isDiving;
boolean canSideJump  = Config.isSideJumpEnabled() && canAngleJump;
boolean canLeftJump  = canSideJump && !rightButton.Pressed;
boolean canRightJump = canSideJump && !leftButton.Pressed;
boolean canBackJump  = Config.isBackJumpEnabled() && canAngleJump
                       && !forwardButton.Pressed && !isStandupSprintingOrRunning();
```

---

### wallJumpCount + continueWallJumping (A-06 확인 완료)

`Options._wallJumpDoubleClickTicks.up(3F, 2F)` → 기본값 **3틱**, 최솟값 2틱 (카운트다운 타이머)

```java
// 더블클릭 모드
if(Options._wallJumpDoubleClick.value) {
    if(canWallJumping) {
        if(jumpButton.StartPressed) {
            if(wallJumpCount == 0)
                wallJumpCount = Options.wallJumpDoubleClickTicks(); // 첫 클릭: 타이머
            else {
                triggerWallJumping = true; // 두 번째 클릭: 발동
                wallJumpCount = 0;
            }
        } else if(wallJumpCount > 0)
            wallJumpCount--;
    } else
        wallJumpCount = 0;
} else
    triggerWallJumping = jumpButton.StartPressed; // 싱글클릭 모드
```

`canWallJumping` 조건:
```java
boolean canWallJumping = Config.isWallJumpEnabled() && !isHeadJumping
                         && !sp.onGround && !isClimbing && !isSwimming
                         && !isDiving && !isLevitating && !isFlying;
```

`wantWallJumping` 유지 조건:
```java
wantWallJumping = canWallJumping &&
    (triggerWallJumping || continueWallJumping ||
    (wantWallJumping && jumpButton.Pressed && !sp.isCollidedHorizontally));
```

`continueWallJumping` true 설정 (handleWallJumping() 내 tryJump 성공 시):
```java
continueWallJumping = !isHeadJumping;
```

`continueWallJumping` false 설정:
```java
if(continueWallJumping && (sp.onGround || isClimbing || !jumpButton.Pressed))
    continueWallJumping = false;
```

---

### 미확인 (이 파일 범위 외)

- `getLeaningPitch()` 증가/감소 속도 → vanilla `LivingEntity.java` 리서치 (B-05, R-08)

---

## R-19 추가 리서치 — SmartMovingSelf 점프 미확인 값 (2026-04-22)

### A-19: `verticalJumpFactor` / `jumpChargeFactor` 보간 공식

**원본 공식 (`tryJump`):**
```java
verticalMotion = -0.078 + 0.498 * verticalJumpFactor * jumpChargeFactor;
```

**`verticalJumpFactor` 값:**
- `Config._jumpVerticalFactor = PositiveFactor("move.jump.vertical.factor")` — `.defaults()` 없음
- `Properties.getDefaultValue(PositiveFactor)` = **1F**
- Up 타입 기본 점프: 추가 배율 없음 → `verticalJumpFactor = 1F`
- `verticalMotion = -0.078 + 0.498 × 1.0 × jumpChargeFactor ≈ 0.42` (charge=0일 때) ← vanilla와 일치

**`jumpChargeFactor` 공식 (`getJumpChargeFactor`):**
```java
return 1F + jumpCharge / _jumpChargeMaximum.value * (_jumpChargeFactor.value - 1F);
```
- charge=0: 1F (vanilla 동일)
- charge=max: `_jumpChargeFactor.value` (기본 = IncreasingFactor → 1F, 명시적 `.defaults(1.3F)` 있음 → **1.3F**)

→ **현재 SmartMovingJumper.java L133-136 구현 정확함. A-19 확인 완료.**

---

### A-21: `isRunning` 원본 판정 조건

```java
// SmartMovingSelf.java L1988-1990
public boolean isRunning() {
    return sp.isSprinting() && !isFast && (sp.onGround || vanilla());
}

// isGroundSprinting (별도 inline)
boolean isGroundSprinting = (isFast || sp.isSprinting()) && sp.onGround && !isSliding && !isCrawling;
```

**헤드점프 차지 진입 조건:**
```java
isHeadJumpCharging = grabButton.Pressed && (isGroundSprinting || isSprintJump || isRunning()) && !isCrawling;
```

→ 1.21.1 대응:
- `isFast` → `sm.isFast` (C-15에서 계산)
- `sp.onGround` → `player.isOnGround()`
- `vanilla()` → `sm.isFlying` (비행 능력)

→ **SmartMovingJumper.java 수정 완료. A-21 확인 완료.**

---

### A-22: `isSlow` 원본 판정 조건

```java
// SmartMovingSelf.java L1863-1867
wouldIsSneaking = wouldWantSneak && !wantSprint && !isClimbing;
isSlow = wantSneak && wouldIsSneaking;
```

단순화: `isSlow = sneakButtonPressed && !sprintButtonPressed && !isClimbing`

**수면 점프 Y 임계값:**
```java
if(posY - floor(posY) > (isSlow ? 0.37 : 0.6))
```

→ 1.21.1 대응: `sm.isSlow` 필드 추가(C-15에서 계산), swim jump threshold 수정 완료.

→ **SmartMovingJumper.java 수정 완료. A-22 확인 완료.**

---

### A-23: `horizontalCollisionAngle` 계산 알고리즘

**출처:** `SmartRenderUtilities.getHorizontalCollisionangle()` (SmartRender 패키지)

**호출 방식 (SmartMovingSelf.beforeMoveEntity):**
```java
horizontalCollisionAngle = getHorizontalCollisionangle(
    (collisions & CollidedPositiveZ) != 0,   // isCollidedPositiveX param (X/Z swap!)
    (collisions & CollidedNegativeZ) != 0,   // isCollidedNegativeX param
    (collisions & CollidedPositiveX) != 0,   // isCollidedPositiveZ param
    (collisions & CollidedNegativeX) != 0);  // isCollidedNegativeZ param
```

**결과 매핑 (X/Z swap 포함, 실효 의미):**
- 남벽(+Z 충돌) → 0°
- 북벽(-Z 충돌) → 180°
- 동벽(+X 충돌) → 90°
- 서벽(-X 충돌) → 270°

**전체 lookup table:** SmartRenderUtilities.md 참조.

→ `getHorizontalCollisionangle()` 메서드 SmartMovingJumper.java에 구현 완료.  
→ C-38: `calculateSeparateCollisions()` 이식 후 handleWallJumping에서 실제 연결.

→ **A-23 확인 완료.**
- `getPoses()` 반환값 경로 → A-11, R-04
