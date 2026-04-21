# 수영/잠수 시스템 교차 분석 — SmartMoving → 1.21.1 Fabric

## 원본 파일
- `SmartMovingSelf.java` (handleSwimming, waterMovementTicks, resetSwimming, correctOnUpdate)
- `SmartMoving.java` (isDipping/isSwimming/isDiving/isLevitating 상태 필드, spawnParticles)
- `SmartMovingBase.java` (getLiquidBorder, getMaxPlayerLiquidBetween, getMinPlayerLiquidBetween, reverseHandleMaterialAcceleration, isInLiquid)
- `SmartMovingContext.java` (SwimCrawlWater* 상수, SwimSoundDistance)
- `SmartMovingConfig.java` (_swim, _dive, _swimSpeedFactor 등)
- `SmartMovingModel.java` (isSwim / isDive 애니메이션)
- `SmartMovingRender.java` (isSwim/isDive 플래그 세팅)

## vanilla 파일
- `LivingEntity_travel.md` (물속 물리 분기)
- `LivingEntity_tick_tickMovement.md` (swimUpward, jump 조건)
- `LivingEntity_updatePose.md` (SWIMMING 포즈, updatePose 로직)
- `LivingEntity_isInSwimmingPose.md` (leaningPitch, setupTransforms 연동)
- `Entity_calculateDimensions.md` (SWIMMING 포즈 히트박스 변경)

---

## [isDipping / isSwimming / isDiving — 수중 상태 3분류]

- **원본 동작:**  
  `offset = playerSwimWaterBorder + 0.1625D`로 물 높이 오프셋을 계산.  
  - offset < 1.4 → `isDipping` (수면에 살짝 잠김, 발만 물속)  
  - 1.4 ≤ offset < 1.9 → `isSwimming` (수면 수영)  
  - offset ≥ 1.9 → `isDiving` (완전 잠수)  
  각 상태는 `SmartMoving.java`에 `public boolean isDipping / isSwimming / isDiving`으로 선언.  
  `resetSwimming()`으로 초기화: dippingDepth=-1, 모든 플래그 false, isLevitating=false.

- **1.21.1 대응:**  
  - `isTouchingWater()` (Yarn: `method_5799`) — 물에 닿아있음  
  - `getFluidHeight(FluidTags.WATER)` — 물 높이 (0.0~블록높이)  
  - `isSwimming()` (LivingEntity) — 물속 스프린트 중인 vanilla 정의 (SM의 isSwimming과 의미 다름)  
  - SM의 isDipping/isSwimming/isDiving 3분류에 직접 대응하는 vanilla API 없음 — **직접 구현 필요**

- **동작 차이:**  
  vanilla `isSwimming()`은 "물속에서 충분히 빠르게 이동 중"을 뜻하므로 SM의 isSwimming (수면 수영)과 개념이 다르다.  
  vanilla에는 SM의 `isDipping`(발만 물속) 개념이 없다.  
  SM은 `playerSwimWaterBorder`를 기반으로 정밀 상태 분류하지만, 1.21.1에서는 `getFluidHeight()`를 이용해 유사하게 재구현해야 한다.

- **포팅 주의사항:**  
  `getFluidHeight(FluidTags.WATER)` 값과 플레이어 키(1.8블록), boundingBox 위치를 조합하여 3분류를 재구현해야 한다.  
  vanilla `isSwimming()` 메서드는 그대로 사용하면 안 되고, SM 자체 판정 로직이 필요하다.  
  `isSwimming()` Mixin @Inject/@Redirect 등으로 SM 상태와 혼동되지 않도록 주의 필요.

---

## [getLiquidBorder / getMaxPlayerLiquidBetween / getMinPlayerLiquidBetween]

- **원본 동작:**  
  `SmartMovingBase.getLiquidBorder(i, j, k)`: 블록 좌표의 액체 높이를 0.0~1.0F로 반환.  
  - `water`/`flowing_water` → `getNormalWaterBorder()`: 메타 0+위공기=0.8875F, 메타 0+위비공기=1F, 메타 1~7=(8-meta)/8F, 메타≥8=1F  
  - `lava`/`flowing_lava` → `_lavaLikeWater ? getNormalWaterBorder() : 0F`  
  - `Material.water` → `getNormalWaterBorder()`  
  - FiniteLiquid 모드 지원 (`getFiniteLiquidWaterBorder`)  
  `getMaxPlayerLiquidBetween(yMin, yMax)`: yMax→yMin 방향으로 탐색, 첫 액체 `j + liquidBorder` 반환.  
  `getMinPlayerLiquidBetween(yMin, yMax)`: yMin→yMax 방향으로 탐색.  
  `isInLiquid()`: 위 두 메서드로 boundingBox 범위 내 액체 존재 여부 확인.

- **1.21.1 대응:**  
  - `FluidState.getHeight(ShapeContext)` (Yarn: `method_15789`) — 액체 높이  
  - `FluidState.getLevel()` (Yarn: `method_15778`) — 레벨값 (0~8)  
  - `world.getFluidState(BlockPos)` — BlockPos의 FluidState 취득  
  - `Entity.getFluidHeight(FluidTag)` (Yarn: `method_7467`) — 엔티티 위치 기준 유체 높이  
  - `Entity.isTouchingWater()`, `Entity.isInLava()` — 유체 감지  
  - SM의 `getNormalWaterBorder` 메타 기반 공식에 직접 대응하는 API 없음 — **직접 구현 필요**

- **동작 차이:**  
  1.21.1에서 물 레벨은 `FluidState.getLevel()` (0=최소, 8=가득참)이며 SM의 메타 기반 공식 `(8-meta)/8`과 반대 방향.  
  1.21.1 `FluidState.getHeight()`는 블록 공간 내 액체의 실제 높이(0.0~1.0F)를 반환하므로 SM의 `getNormalWaterBorder()`를 대체할 수 있다.  
  FiniteLiquid 모드 호환성은 1.21.1 포팅에서 제외 가능 (모드 특화 코드).

- **포팅 주의사항:**  
  `FluidState.getHeight()`를 사용하되, `world.getBlockState(pos).getFluidState().getHeight(ShapeContext.absent())`로 취득한다.  
  SM의 `getMaxPlayerLiquidBetween`은 Y 범위를 플레이어 boundingBox로 직접 지정하므로, `getFluidHeight(FluidTags.WATER)`(엔티티 중심 기준)와 다르다 — boundingBox 기반 수동 탐색 로직 유지 필요.  
  `water`/`flowing_water` 이름 비교 → 1.21.1은 `FluidTags.WATER`로 대체.

---

## [handleSwimming — 수중 이동 처리 전체]

- **원본 동작:**  
  `SmartMovingSelf.superMoveEntityWithHeading()`에서 `handleSwimming(moveForward, moveStrafing, speedFactor, isLiquidClimbing)` 호출.  
  수중 상태(isDipping/isSwimming/isDiving)에 따라 `motionX`, `motionY`, `motionZ`를 직접 조작.  
  **dipping:** motionYDiff = -0.02D ~ -0.01D. 감쇠: x/z=0.80D, y=0.83D.  
  **swimming motionYDiff:** offset 구간에 따라 세분화된 값 (-0.02D ~ +0.02D). 감쇠: x/z/y=0.85D.  
  **diving motionYDiff:** diveUp=0.05D*sprintFactor, diveDown=0.01-0.1*speedFactor, moveSwim=0.04D. 감쇠: 0.83D.  
  수평 이동: `moveFlying(moveStrafing, moveForward, 0.02F * speedFactor)` (treeDimensional=false).  
  잠수(diving) 수직 제어: `moveFlying(motionYDiff, moveStrafing, moveForward, 0.02F * speedFactor, Options._diveControlVertical.value)` (treeDimensional=true 가능).  
  `diveUp` = `isp.getIsJumpingField()` (점프 키). `diveDown` = `sneak && Config._diveDownOnSneak`.  
  `isDiving || isSwimming`이면 `setHeightOffset(-1F)`.  
  `isJumpingOutOfWater`: 수평 벽에 부딪힌 채 점프 시 `motionY = 0.30000001192092896D`.  
  **useStandard 경로** (수영 실패/라이딩): vanilla처럼 moveFlying+moveEntity+0.80D+motionY-0.02D.

- **1.21.1 대응:**  
  - `Entity.addVelocity(x, y, z)` / `Entity.setVelocity(Vec3d)` (Yarn: `method_6021` / `method_6034`)  
  - `LivingEntity.travel(Vec3d movementInput)` — 물속 분기 (isTouchingWater → updateVelocity → move → multiply(f, 0.8F, f))  
  - `Entity.move(MovementType, Vec3d)` (Yarn: `method_5784`) — 충돌 포함 이동  
  - SM의 `moveFlying()` → `Entity.updateVelocity(speed, movementInput)` + 피치 보정 수동 추가  
  - SM의 직접 motionX/Y/Z 조작 → `entity.setVelocity(Vec3d)` 또는 `entity.addVelocity()`

- **동작 차이:**  
  1.21.1 vanilla `travel()` 물속 분기는 SM이 완전히 대체한다. SM은 `handleSwimming()`에서 직접 motionX/Y/Z를 계산하므로 vanilla travel()의 물속 분기가 실행되지 않아야 한다.  
  `motionX/Y/Z`(double 필드) → `getVelocity()`/`setVelocity(Vec3d)` API로 교체.  
  SM의 `moveFlying()` 공식(수평거리 루트 후 Y 추가 후 다시 루트 — 비표준 공식)은 그대로 유지해야 한다.  
  vanilla travel()는 SM handleSwimming이 처리한 틱에서 호출되지 않아야 한다(원본도 별도 경로).

- **포팅 주의사항:**  
  `travel()` Mixin에서 SM이 처리하는 경우 vanilla 물속 분기를 건너뛰어야 한다.  
  `isTouchingWater()` → `shouldSwimInFluids()` 조건도 확인 필요 (vanilla는 두 조건을 모두 검사).  
  `moveFlying()` 공식의 비표준 total 계산(`sqrt(sqrt(x²+z²) + y²)`)을 그대로 재구현해야 한다.  
  `Options._diveControlVertical.value`에 따라 treeDimensional 3D 이동 vs 2D 이동이 결정된다.

---

## [setHeightOffset(-1F) — 수중 Y 위치 오프셋]

- **원본 동작:**  
  `isDiving || isSwimming`이면 매 틱 `setHeightOffset(-1F)` 호출.  
  `afterMoveEntity()`: `sp.posY += heightOffset`으로 실제 Y 위치를 조정.  
  `correctOnUpdate(isSwimming || isDiving || isDipping || isCrawling, isSwimming)` 내부에서 `reverseHandleMaterialAcceleration()` 호출 (isSwimming일 때만).  
  heightOffset은 `SmartMoving.java`에 `public float heightOffset`으로 선언.  
  이 오프셋으로 플레이어가 수면에서 절반 정도 잠겨있는 시각적 위치를 조정한다.

- **1.21.1 대응:**  
  - 직접 대응 API 없음 — **직접 구현 필요**  
  - `Entity.setPosition(x, y, z)` (Yarn: `method_5530`) — Y 위치 직접 설정 가능  
  - SWIMMING 포즈(0.6×0.6 히트박스)로 전환하면 vanilla가 자동으로 카메라/히트박스를 조정하므로, SM의 heightOffset과 중복 문제 발생 가능

- **동작 차이:**  
  vanilla 1.21.1에서 수영 포즈(SWIMMING)는 `updatePose()` → `calculateDimensions()`를 통해 히트박스와 카메라 높이를 자동 조정한다(높이 1.8→0.6, 눈높이 1.62→0.4).  
  SM의 heightOffset은 이와 별개로 추가 Y 오프셋을 적용하므로, 1.21.1 포팅 시 이중 오프셋 충돌 가능성이 있다.  
  SM 원본은 SWIMMING 포즈 시스템이 없는 1.7.10 기반이므로 heightOffset으로 직접 위치를 조정했지만, 1.21.1에서는 포즈 시스템 연동을 우선 고려해야 한다.

- **포팅 주의사항:**  
  SM의 수영 시 heightOffset(-1F)과 vanilla SWIMMING 포즈의 자동 히트박스 조정이 겹치지 않도록 설계해야 한다.  
  1.21.1 포팅에서 SM 수영 상태를 SWIMMING 포즈로 매핑하면 heightOffset이 불필요해질 수 있다.  
  단, SM isDiving(완전 잠수) 시에도 heightOffset을 적용하므로, vanilla SWIMMING 포즈가 잠수에도 적합한지 검토 필요.

---

## [reverseHandleMaterialAcceleration — 물 흐름 상쇄]

- **원본 동작:**  
  `SmartMovingBase.reverseHandleMaterialAcceleration()`: vanilla `handleMaterialAcceleration()`의 물 흐름 가속 `+0.014D`를 역방향 `-0.014D`로 상쇄.  
  바운딩박스를 `expand(0, -0.4D, 0).contract(0.001D, 0.001D, 0.001D)`로 수정 후 물 블록들의 flow 벡터를 합산.  
  `correctOnUpdate()`에서 `reverseMaterialAcceleration = isSwimming`일 때만 호출.  
  조건: `vec3d.lengthVector() > 0.0D` → normalize 후 `motionX/Y/Z -= vec3d * 0.014D`.

- **1.21.1 대응:**  
  - `FluidState.getVelocity(BlockView, BlockPos)` (Yarn: `method_15787`) — 유체 흐름 속도 벡터  
  - `Entity.isTouchingWater()` 상태에서 vanilla가 이미 흐름 가속을 적용  
  - SM의 역방향 상쇄 로직을 1.21.1 FluidState API 기반으로 재구현 필요

- **동작 차이:**  
  1.21.1에서 물 흐름 가속은 `LivingEntity.travel()` 물속 분기 내부에서 `applyFluidMovingSpeed()`와 별개로 `Entity.updateMovementInFluid()` 계통에서 처리됨.  
  SM이 handleSwimming으로 travel()을 대체할 경우, vanilla 흐름 가속 자체가 적용되지 않으므로 역방향 상쇄가 필요 없을 수도 있다.  
  단, SM이 travel()을 부분적으로만 대체하는 경우 vanilla 흐름 가속이 여전히 적용될 수 있다 — **확인 필요**.

- **포팅 주의사항:**  
  SM handleSwimming이 travel()을 완전 대체하는지, 부분 대체인지에 따라 역방향 상쇄 필요 여부가 달라진다.  
  1.21.1에서 vanilla가 물 흐름을 언제 적용하는지 추가 확인 필요 (미확인 — 추가 리서치 필요).

---

## [handleLava — 용암 이동]

- **원본 동작:**  
  `SmartMovingSelf.handleLava()`: `!isFlying && !handledSwimming && !isLiquidClimbing && sp.handleLavaMovement()`.  
  용암 내: `moveFlying(strafe, forward, 0.02F)` → `moveEntity(motionX, motionY, motionZ)` → `motionX/Y/Z *= 0.5D` → `motionY -= 0.02D`.  
  수직 벽 탈출: `isCollidedHorizontally && isOffsetPositionInLiquid(...)` → `motionY = 0.30000001192092896D`.  
  호출 시 `resetClimbing()`, `resetSwimming()` 실행.

- **1.21.1 대응:**  
  - `Entity.isInLava()` (Yarn: `method_5768`) — 용암 감지  
  - `LivingEntity.travel()` 용암 분기 (isInLava → updateVelocity(0.02F) → move → multiply(0.5/0.8) → applyFluidMovingSpeed)  
  - vanilla travel() 용암 분기가 SM handleLava와 유사하게 동작

- **동작 차이:**  
  vanilla travel() 용암 분기의 감쇠: `0.5D`(깊을 때) 또는 `0.5, 0.8, 0.5`(얕을 때).  
  SM handleLava: `motionX/Y/Z *= 0.5D` 고정 (깊이 무관).  
  vanilla는 `getSwimHeight()` 기반으로 얕은/깊은 용암을 구분하지만 SM은 구분 없이 `0.5D` 적용.

- **포팅 주의사항:**  
  SM이 handleLava 처리 시 travel()의 용암 분기와 충돌하지 않도록 해야 한다.  
  용암 내 동작을 vanilla 기본 동작으로 허용할지, SM 커스텀으로 대체할지 결정 필요.  
  `sp.handleLavaMovement()` → `entity.isInLava()` 변환 시 의미 동일함 확인.

---

## [isJumpingOutOfWater — 물 밖으로 점프]

- **원본 동작:**  
  `wantJumpOutOfWater = (moveForward != 0 || moveStrafing != 0) && sp.isCollidedHorizontally && diveUp && !isSlow`  
  `isJumpingOutOfWater = wantJumpOutOfWater && (waterMovementTicks > 10 || sp.onGround || wasJumpingOutOfWater)`  
  조건 성립 시: `sp.motionY = 0.30000001192092896D` — 강한 상승 속도.  
  물속 이동 중 벽에 막혔을 때 자동으로 물 밖으로 탈출하는 기능.

- **1.21.1 대응:**  
  - 직접 대응 API 없음 — **직접 구현 필요**  
  - `entity.isHorizontalCollision()` 또는 `entity.horizontalCollision` 필드  
  - `entity.setVelocity(vx, 0.3, vz)` — 상승 속도 설정

- **동작 차이:**  
  vanilla에는 이 기능이 없다. SM 독자 기능.  
  `waterMovementTicks > 10` 조건으로 물속에서 충분히 이동한 후에만 발동.

- **포팅 주의사항:**  
  `waterMovementTicks` 카운터를 직접 구현해야 한다.  
  `sp.isCollidedHorizontally` → Mixin에서 `entity.horizontalCollision` 필드 접근 또는 별도 감지 로직 필요.  
  `isJumpingOutOfWater` 상태를 패킷으로 서버에 전송해야 함 (현재 addToSendQueue에서 비트 포함).

---

## [waterMovementTicks]

- **원본 동작:**  
  `SmartMovingSelf`의 `public int waterMovementTicks`.  
  물속 틱마다 증가, 물 밖이면 0으로 리셋 (실제 리셋 로직은 SmartMovingSelf 내 확인 필요 — 미확인).  
  `isJumpingOutOfWater` 조건에서 `waterMovementTicks > 10` 사용.

- **1.21.1 대응:**  
  직접 대응 없음 — **직접 구현 필요**.  
  별도 카운터 필드를 Mixin으로 추가.

- **동작 차이:**  
  vanilla에는 이 카운터가 없다.

- **포팅 주의사항:**  
  물 진입/탈출 시 카운터 리셋 로직을 명확히 구현해야 함.

---

## [moveFlying — 3D 수중 이동 벡터 계산]

- **원본 동작:**  
  `SmartMovingBase.moveFlying(moveUpward, moveStrafing, moveForward, speedFactor, treeDimensional)`.  
  수평: yaw 기반 sin/cos로 strafe/forward 분해 → 수평 합산.  
  수직: `treeDimensional=true` 시 피치(rotationPitch) 기반 cos/sin으로 수직 성분 추가.  
  **total 계산: `sqrt(sqrt(x²+z²) + y²)` — 표준 유클리드 거리 아님 (의도적 비대칭)**.  
  `total > 0.01F`이면: `factor = speedFactor / total`, `motionX/Y/Z += diffMotion * factor`.  
  `treeDimensional=false` → 수평 이동만.

- **1.21.1 대응:**  
  - `Entity.updateVelocity(speed, movementInput)` (Yarn: Entity 클래스) — 입력 기반 속도 증분  
  - 하지만 updateVelocity는 피치 기반 수직 분해 없음 — **moveFlying 로직 직접 구현 필요**

- **동작 차이:**  
  1.21.1 `updateVelocity`는 yaw만 고려한 수평 이동.  
  SM의 `moveFlying(treeDimensional=true)`는 pitch 기반 수직 분해를 포함하여 진정한 3D 방향 이동을 구현.  
  `sqrt(sqrt(x²+z²) + y²)` 공식은 수직보다 수평을 더 강하게 보정하는 의도적 비대칭 공식.

- **포팅 주의사항:**  
  `moveFlying()` 메서드를 그대로 재구현해야 한다. `updateVelocity()`로 대체 불가.  
  `motionX/Y/Z += ...` → `entity.addVelocity(...)` 또는 `entity.setVelocity(entity.getVelocity().add(...))`.  
  `sp.rotationYaw` → `entity.getYaw()`, `sp.rotationPitch` → `entity.getPitch()`.

---

## [SWIMMING 포즈 + isInSwimmingPose() — 히트박스 및 렌더 자세]

- **원본 동작:**  
  SM 원본(1.7.10)에는 EntityPose 시스템이 없다.  
  수영/잠수 시 `setHeightOffset(-1F)`로 boundingBox minY를 직접 조정.  
  렌더는 SmartMovingModel의 isSwim/isDive 상태로 별도 처리.

- **1.21.1 대응:**  
  - `EntityPose.SWIMMING` (Yarn: enum 값) — 히트박스 0.6×0.6, 눈높이 0.4  
  - `Entity.setPose(EntityPose)` (Yarn: `method_18380`) — 포즈 설정  
  - `Entity.isInSwimmingPose()` (Yarn: `method_20232`) — `pose==SWIMMING` 또는 `!isFallFlying() && pose==FALL_FLYING`  
  - `LivingEntity.updateLeaningPitch()`: isInSwimmingPose() → leaningPitch +0.09/틱 → 최대 1.0  
  - `PlayerEntityRenderer.setupTransforms()`: leaningPitch>0 → X축 -90° 회전 + isInSwimmingPose → translate(0,-1,0.3)  
  - `PlayerEntity.updatePose()`: isSwimming() → SWIMMING 포즈; 공간 부족 → SWIMMING 강제 (크롤링)

- **동작 차이:**  
  SM 수영(isSwimming)을 SWIMMING 포즈로 매핑하면 vanilla leaningPitch가 자동으로 -90° 기울기를 적용한다.  
  SM의 isSwim 애니메이션(bipedOuter.rotateAngleX=Quarter, 45°)과 vanilla의 -90° 기울기가 충돌한다.  
  vanilla SWIMMING 포즈는 수중 스프린트(isSwimming())에만 자동 전환되며, SM의 isDiving(완전 잠수)에는 자동으로 적용되지 않는다.  
  vanilla SWIMMING 포즈는 크롤링에도 사용되므로, SM 수영과 SM 크롤링이 동일 포즈 값을 공유하는 문제 발생.

- **포팅 주의사항:**  
  SM isSwimming → SWIMMING 포즈 매핑 시 vanilla leaningPitch(-90° 기울기)를 억제하거나, SM의 isSwim 애니메이션을 leaningPitch 기반으로 재구현해야 한다.  
  SM isDiving → 별도 포즈 없음. SWIMMING 포즈 재사용 시 isDiving에서도 -90° 자동 적용.  
  SM isDipping → 포즈 변경 불필요 (수면에 발만 잠김, 히트박스 유지).  
  SWIMMING 포즈 히트박스(0.6×0.6)가 SM의 heightOffset(-1F) 보정과 충돌하지 않는지 확인 필요.  
  `setupTransforms()`의 `translate(0,-1,0.3)` 오프셋이 SM 수영 렌더에 원하지 않게 적용될 수 있다.

---

## [updatePose() — 포즈 자동 전환과 SM 수영 충돌]

- **원본 동작:**  
  SM 원본에는 `updatePose()` 시스템이 없다. `heightOffset`과 `boundingBox` 직접 조작으로 대응.

- **1.21.1 대응:**  
  - `PlayerEntity.updatePose()` (Yarn: `method_7318`) — 매 틱 호출  
  - `isSwimming()` true → SWIMMING 포즈  
  - `isSneaking() && !flying` → CROUCHING 포즈  
  - 공간 부족 → SWIMMING 강제 (SM 크롤링과 동일 경로)

- **동작 차이:**  
  SM 수영 중 `isSwimming()` Mixin 미적용 시 vanilla updatePose()가 SWIMMING 포즈로 전환하지 않을 수 있다.  
  SM 잠수(isDiving) 중 vanilla isSwimming() 기준 미충족 시 포즈가 STANDING으로 유지될 수 있다.  
  SM canCrawl 조건: `!isSwimming && !isDiving` — 수영/잠수 중 크롤링 차단.

- **포팅 주의사항:**  
  `updatePose()` Mixin에서 SM isSwimming/isDiving 상태에 따라 포즈를 명시적으로 설정해야 한다.  
  SM isDiving 시 SWIMMING 포즈 강제 설정이 필요할 수 있다.  
  vanilla updatePose()가 SM 상태를 모르고 STANDING으로 복귀하는 것을 방지해야 한다.

---

## [calculateDimensions() — SWIMMING 포즈 히트박스]

- **원본 동작:**  
  SM 원본: EntityPose 없음. boundingBox를 `setHeightOffset(-1F)`로 직접 조작.  
  `height = 1.8` 유지 (수영 중에도 논리 히트박스 높이 동일, 단 렌더/충돌은 heightOffset으로 보정).

- **1.21.1 대응:**  
  - SWIMMING 포즈 설정 시 `calculateDimensions()` 자동 호출 → 히트박스 0.6×0.6으로 즉시 교체  
  - `PlayerEntity.getBaseDimensions(EntityPose.SWIMMING)` → `EntityDimensions(0.6, 0.6, eyeHeight=0.4)`  
  - PlayerEntity는 `recalculateDimensions()` 호출 제외 (`!(this instanceof PlayerEntity)` 조건)

- **동작 차이:**  
  SM 원본에서 수영 중 히트박스는 1.8H 유지. 1.21.1 SWIMMING 포즈 사용 시 0.6H로 대폭 감소.  
  SM의 수영→기어서 걷기 전환 로직(`fromSwimmingOrDiving()`)이 히트박스 크기 기반이므로, 0.6H 히트박스를 전제로 재구현해야 한다.  
  카메라 높이(standingEyeHeight)가 1.62→0.4로 즉시 변경. SM에서 이 동작이 원하는 것인지 확인 필요.

- **포팅 주의사항:**  
  SWIMMING 포즈 히트박스(0.6H)가 SM 수영 이동 판정과 호환되는지 확인.  
  서버 측에서도 SWIMMING 포즈가 동기화되어 히트박스가 일치해야 한다 (DataTracker 동기화 자동).  
  SM 수영 중 카메라가 0.4H에 위치하는 것이 플레이어 경험에 미치는 영향 검토 필요.

---

## [swimUpward — 점프 키로 수중 상승]

- **원본 동작:**  
  `diveUp = isp.getIsJumpingField()` (점프 키 눌림).  
  isDiving 시 `motionYDiff = 0.05D * (isFast ? sprintFactor : 1F)`.  
  isSwimming 시 offset에 따라 세분화된 motionYDiff.  
  `diveUp` 상태에서 먼저 `motionY -= 0.039999999105930328D` 적용 후 motionYDiff 추가.

- **1.21.1 대응:**  
  - `LivingEntity.swimUpward(FluidTag)` (Yarn: `method_6010`): `velocity.add(0, 0.04F, 0)`  
  - `tickMovement()` 내부: `jumping && isTouchingWater && fluidHeight > 0` → `swimUpward(WATER)`  
  - 기본 상승 속도: +0.04F/틱

- **동작 차이:**  
  SM은 `0.05D * sprintFactor` (스프린트 시 더 빠름), vanilla는 `+0.04F` 고정.  
  SM은 offset(현재 수위)에 따라 미세하게 조정하지만 vanilla는 단순 +0.04F.  
  SM의 `motionY -= 0.039999999105930328D` 선행 처리 → 순 상승 = 약 0.01D (매우 느린 상승). 이후 moveFlying에서 추가 속도.

- **포팅 주의사항:**  
  SM의 수중 상승을 vanilla swimUpward()로 대체하면 동작이 달라진다.  
  SM의 세분화된 motionYDiff 로직을 그대로 재구현하려면 `swimUpward()` 오버라이드 또는 Mixin 필요.  
  `jumping` 필드 → `entity.jumping` (LivingEntity protected 필드, Mixin accesswidener 필요).

---

## [fromSwimmingOrDiving — 물 탈출 시 크롤링/서기 전환]

- **원본 동작:**  
  `wasShortInWater && !isShortInWater`이면 실행.  
  `setHeightOffset(-1F)` → `resetHeightOffset()`.  
  천장까지 거리 vs 플레이어 height 비교:  
  - 공간 < height → 크롤링 전환  
  - 액체 천장 < height → 크롤링 전환  
  - `crawlStandUpBottom > boundingBox.minY` → 걷기/크롤링  
  `getMinPlayerLiquidBetween(maxY, maxY+1.1D)`: 머리 위 액체 하단.  
  `getMinPlayerSolidBetween(maxY, maxY+1.1D, 0)`: 머리 위 고체 하단.

- **1.21.1 대응:**  
  - `World.isSpaceEmpty(Entity, Box)` (Yarn: `method_8587`) — 공간 비어있음 확인  
  - `PlayerEntity.canChangeIntoPose(EntityPose)` (Yarn: `method_52558`) — 포즈 전환 가능 여부  
  - `EntityPose.STANDING` 전환 가능 여부 → SM의 "서있을 공간 있음" 대응  
  - SM의 `getMinPlayerSolidBetween` → `World.getBlockCollisions()` 또는 `VoxelShape` 기반 재구현

- **동작 차이:**  
  SM은 물 탈출 시 공간 계산으로 크롤링/서기를 결정한다. 1.21.1에서는 `updatePose()`가 매 틱 이를 자동으로 처리한다.  
  SM의 수동 전환 로직과 vanilla의 자동 updatePose()가 충돌하지 않도록 조정 필요.

- **포팅 주의사항:**  
  물 탈출 시 크롤링 전환은 `updatePose()` Mixin 내에서 SM 상태를 확인하여 처리하는 것이 자연스럽다.  
  `getMinPlayerSolidBetween` 구현은 `World.getBlockCollisions(entity, box)` 기반으로 재구현 가능.

---

## [isLiquidClimbing — 물속 클라이밍 조건]

- **원본 동작:**  
  `isLiquidClimbing = Config.isFreeClimbingEnabled() && sp.fallDistance <= 3.0 && wantClimbUp && sp.isCollidedHorizontally && !isDiving`  
  물속 벽에 붙어서 위로 올라갈 수 있는 특수 조건.  
  handleSwimming보다 우선하여 처리되지 않음 (`handleSwimming`이 먼저 호출됨).

- **1.21.1 대응:**  
  - 직접 대응 없음 — SM 독자 기능  
  - `entity.fallDistance` (필드) → accesswidener 또는 `entity.getFallDistance()` 미확인

- **동작 차이:**  
  vanilla에는 이 개념이 없다.

- **포팅 주의사항:**  
  클라이밍 교차 분석에서도 다루어야 하는 항목.  
  `sp.fallDistance` → `entity.fallDistance` (protected 필드, accesswidener 필요) 또는 Mixin 접근.

---

## [수영 소리 — SmartMoving 커스텀]

- **원본 동작:**  
  `distanceSwom` 누적. `distanceSwom > SwimSoundDistance (= 1/0.7F ≈ 1.4286F)`이면 소리 재생.  
  소리: `"random.splash"`, 볼륨 `0.05F`, 피치 `1.0F ± rand*0.4F`.  
  `distanceSwom -= SwimSoundDistance` 후 계속 누적.  
  isSwimming 상태에서만 발동 (`handleExhaustion()` 후 별도 블록).

- **1.21.1 대응:**  
  - `SoundEvents.ENTITY_PLAYER_SWIM` (Yarn: `SoundEvents` 클래스 상수) — 수영 소리  
  - `Entity.playSound(SoundEvent, volume, pitch)` (Yarn: `method_5485`)  
  - vanilla는 자체 수영 소리 재생 로직 보유 (확인 필요 — 중복 여부)

- **동작 차이:**  
  vanilla 1.21.1에 자체 수영 소리 재생이 있는지 확인 필요 (미확인 — 추가 리서치 필요).  
  SM의 커스텀 소리(`"random.splash"`)는 1.21.1 SoundEvent로 매핑해야 한다.

- **포팅 주의사항:**  
  vanilla가 수영 소리를 자동으로 재생하는 경우 SM과 중복 재생될 수 있다.

---

## [수영 파티클 — spawnParticles()]

- **원본 동작:**  
  `SmartMoving.spawnParticles()`: isSwimming 상태에서 `EntitySplashFX` 또는 `EntityLavaFX` 스폰.  
  Y 위치: `floor(boundingBox.minY) + 1.0F` (수면 근처).  
  초기 속도: motionX=0, motionY=0.2, motionZ=0 (위로 수직 상승).  
  누적 방식: `spawnSwimmingParticle += horizontalSpeedSquare`, threshold 초과 시 스폰.  
  threshold: `Config._swimParticlePeriodFactor.value * 0.01F`.

- **1.21.1 대응:**  
  - `ParticleEffect` 계열 (Yarn: 구체적 클래스는 파티클 종류별 상이)  
  - `World.addParticle(ParticleEffect, x, y, z, vx, vy, vz)` (Yarn: `method_17892`)  
  - `ParticleTypes.SPLASH` — 물 튀는 파티클  
  - `EntityLavaFX` → `ParticleTypes.DRIPPING_LAVA` 또는 `LAVA`

- **동작 차이:**  
  1.21.1 파티클 API는 `World.addParticle()`로 통일. `effectRenderer.addEffect()` 없음.  
  `EntitySplashFX` → `ParticleTypes.SPLASH` 대응 가능.

- **포팅 주의사항:**  
  클라이언트 전용 메서드에서만 파티클 스폰 필요 (`world.isClient` 확인).  
  `sp.getRNG().nextFloat()` → `entity.getRandom().nextFloat()`.

---

## [isSwim / isDive 애니메이션 — SmartMovingModel]

- **원본 동작:**  
  **isSwim:** `bipedOuter.fadeRotateAngleX = true`, `bipedOuter.rotateAngleX = Quarter - Sixteenth * standSneakFactor`.  
  `bipedOuter.rotateAngleY = horizontalAngle` (이동 방향).  
  팔: YZX 회전 순서, 팔 Z = Quarter+Eighth 방향 (옆으로 벌림), 팔 X = 수평 거리 기반 교대.  
  다리: 수평 거리 기반 cos 진동.  
  속도 구간: standSneakFactor(0~0.157), walkFactor(0.157~0.523), sneakFactor.  
  **isDive:** `bipedOuter.rotateAngleX = Quarter - currentVerticalAngle` (수직 각도 연동).  
  다리 Z = cos 기반 개방. 팔 Z = cos 기반 개방.  
  `isLevitate` 시 `Quarter - Sixteenth`, `isJump` 시 0F.

- **1.21.1 대응:**  
  - `BipedEntityModel`의 `head`, `body`, `rightArm`, `leftArm`, `rightLeg`, `leftLeg` 파트  
  - 1.21.1에 `bipedOuter`, `bipedTorso`, `bipedPelvic` 등 SM 중간 노드 없음  
  - vanilla SWIMMING 포즈에서 `setupTransforms()`가 leaningPitch 기반 -90° 회전 자동 적용  
  - `BipedEntityModel.setAngles()` (Yarn: `method_17087`) Mixin으로 커스텀 애니메이션 삽입 가능

- **동작 차이:**  
  SM의 isSwim 애니메이션은 약 45°(Quarter) 기울임이지만, vanilla의 leaningPitch 기반은 -90° 기울임.  
  SM은 `bipedOuter` 중간 노드로 전신 회전을 제어하지만 1.21.1에는 해당 노드 없음.  
  1.21.1에서 수영 기울기는 `setupTransforms()`의 MatrixStack 회전으로 처리됨 — SM이 추가 회전을 적용하면 이중 회전 발생.

- **포팅 주의사항:**  
  SM isSwim/isDive 애니메이션을 1.21.1로 이식할 때 `setupTransforms()` Mixin 또는 `setAngles()` Mixin을 선택해야 한다.  
  vanilla leaningPitch 기반 자동 기울기와 SM 커스텀 기울기의 중복을 방지해야 한다.  
  `bipedOuter` 중간 노드 없으므로 전신 회전은 `MatrixStack.multiply(rotation)` 또는 `ModelPart.yaw/pitch/roll` 직접 설정으로 대체.  
  SM의 `fadeRotateAngleX = true`(부드러운 전환) → leaningPitch 보간 방식과 유사 — vanilla leaningPitch 활용 가능성 검토.

---

## [setArmScales / setLegScales — 팔/다리 스케일 애니메이션]

- **원본 동작:**  
  SM isSwim: `setArmScales(1 + (cos(t-Q)-1)*0.15*sneakFactor, ...)`.  
  SM isDive: `setLegScales(1 + (cos(d-Q)-1)*0.25*walkFactor, ...)`.  
  `Scale` 타입: scaleY 직접 변경. `NoScaleEnd`: offsetY로 보정. `NoScaleStart`: 스킵.

- **1.21.1 대응:**  
  `ModelPart` 스케일 직접 없음. `MatrixStack.scale(x, y, z)`로 각 파트 렌더 시 스케일 적용.  
  또는 `ModelPart.xScale`, `yScale`, `zScale` 필드 (1.21.1에서 존재 여부 미확인 — 추가 리서치 필요).

- **동작 차이:**  
  1.21.1 ModelPart에 xScale/yScale/zScale이 있는지 미확인.  
  GL11 직접 스케일(SM) vs MatrixStack 스케일(1.21.1) — 애니메이션 시스템 교차 분석에서도 다룸.

- **포팅 주의사항:**  
  1.21.1 ModelPart의 스케일 필드 존재 여부 추가 리서치 필요 (미확인).

---

## [isSmall — 패킷 비트]

- **원본 동작:**  
  `addToSendQueue`에서 `isSmall = height < 1` 비트 전송.  
  수영/잠수 시 heightOffset=-1F → height가 1 미만이 됨 → isSmall=true.

- **1.21.1 대응:**  
  - 직접 대응 없음  
  - SM 패킷 시스템 재구현 필요 (SM 네트워크 교차 분석에서 별도 처리)

- **포팅 주의사항:**  
  수영/잠수 상태를 서버에 알려야 한다. 1.21.1 Fabric 패킷 API(CustomPayload 등)로 재구현 필요.

---

## [sp.handleLavaMovement() / sp.isInWater()]

- **원본 동작:**  
  `sp.handleLavaMovement()` — 용암 내 이동 처리 (Entity 메서드). 용감 내 있으면 true.  
  `sp.isInWater()` — 물속 있음 여부.  
  두 메서드 모두 1.7.10 EntityPlayer API.

- **1.21.1 대응:**  
  - `entity.isInLava()` (Yarn: `method_5768`) — 용암 감지  
  - `entity.isTouchingWater()` (Yarn: `method_5799`) — 물 감지  
  - `entity.isSubmergedInWater()` — 완전 잠수 여부 (미확인)

- **동작 차이:**  
  `isInWater()`와 `isTouchingWater()`는 의미가 동일하다고 볼 수 있으나 내부 구현 확인 필요 (미확인).

- **포팅 주의사항:**  
  1.7.10 API 이름이 1.21.1에서 변경되었으므로 Yarn 이름으로 매핑 필요.

---

## [SmartMovingConfig — 수영/잠수 설정]

- **원본 동작:**  
  ```
  _swim: 수영 활성화 (기본 true)
  _swimSpeedFactor: 수영 속도 배율
  _swimDownOnSneak: 스닉 시 하강 (기본 false, v1.6 이전)
  _swimParticlePeriodFactor: 파티클 주기 배율
  _dive: 잠수 활성화
  _diveSpeedFactor: 잠수 속도 배율
  _diveDownOnSneak: 스닉 시 잠수 하강 (기본 false, v1.6 이전)
  _lavaLikeWater: 용암을 물처럼 취급
  _lavaSwimParticlePeriodFactor: 용암 수영 파티클 주기 (기본 4F)
  ```

- **1.21.1 대응:**  
  Fabric 모드 설정 파일로 재구현. SM의 Property 시스템(`net.smart.properties`)을 유지하거나 Fabric API 설정으로 대체.

- **동작 차이:**  
  Property 시스템 전체 재구현 또는 Cloth Config 등 Fabric 설정 라이브러리 사용 가능.

- **포팅 주의사항:**  
  설정 키 네이밍(`move.swim`, `move.dive`)은 하위호환 유지 또는 새 포맷으로 마이그레이션.

---

## 전체 포팅 난이도 요약

### 가장 어려운 부분과 이유

1. **SWIMMING 포즈 이중 사용 문제** (난이도: 최상)  
   1.21.1은 SWIMMING 포즈를 크롤링과 실제 수영에 모두 사용한다. SM의 수영(isSwimming), 잠수(isDiving), 크롤링(isCrawling)을 각각 구분해야 하는데, vanilla는 이 셋을 하나의 포즈 값으로 처리한다.  
   leaningPitch 자동 -90° 기울기가 SM 수영 애니메이션(Quarter=45°)과 충돌한다.  
   `updatePose()` Mixin에서 SM 상태별 포즈 강제 설정이 필요하며, leaningPitch를 SM이 직접 제어해야 한다.

2. **handleSwimming → travel() 대체** (난이도: 높음)  
   SM은 travel()의 물속 분기를 완전히 대체한다. Mixin으로 travel()에서 SM 수중 상태일 때 vanilla 물 처리를 건너뛰고, SM의 motionX/Y/Z 계산 로직을 Vec3d API로 재구현해야 한다.

3. **heightOffset vs SWIMMING 포즈 히트박스 충돌** (난이도: 높음)  
   SM의 heightOffset(-1F)은 수영 시 Y 위치를 직접 조작한다. 1.21.1의 SWIMMING 포즈는 히트박스를 자동으로 0.6H로 축소한다. 두 메커니즘의 중복을 해결해야 한다.

4. **getLiquidBorder 재구현** (난이도: 중간)  
   SM의 수정밀 물 높이 계산(0.8875F, meta 기반)을 1.21.1 FluidState API로 재구현해야 한다. 결과값이 SM 원본과 정확히 일치해야 isDipping/isSwimming/isDiving 전환 타이밍이 같아진다.

5. **moveFlying() 재구현** (난이도: 중간)  
   비표준 `sqrt(sqrt(x²+z²) + y²)` 공식과 피치 기반 3D 이동을 그대로 재구현해야 한다. updateVelocity()로 대체 불가.

### 권장 구현 순서

1. **상태 판정**: isDipping/isSwimming/isDiving 3분류 구현 (`FluidState.getHeight()` 기반)
2. **포즈 매핑**: `updatePose()` Mixin에서 SM 수중 상태 → SWIMMING 포즈 강제 + leaningPitch 제어
3. **hibox 조정**: heightOffset vs SWIMMING 포즈 히트박스 충돌 해결 방침 결정
4. **물속 이동**: `travel()` Mixin에서 SM 수중 상태 시 vanilla 물 분기 건너뜀 + `handleSwimming` 로직 구현
5. **moveFlying() 구현**: Vec3d API 기반으로 재구현
6. **수직 이동**: diveUp/diveDown 키 처리, swimUpward 대체
7. **물 탈출 전환**: `fromSwimmingOrDiving()` → `updatePose()` 연동
8. **애니메이션**: isSwim/isDive 애니메이션을 leaningPitch 또는 setAngles Mixin으로 구현
9. **파티클/소리**: 1.21.1 API로 교체
10. **네트워크**: isSwimming/isDiving 비트 → Fabric CustomPayload 패킷
