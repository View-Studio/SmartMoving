# 교차 분석 — 이동 속도/물리

## 분석 대상

| 원본 (1.7.10) | 1.21.1 vanilla 대응 |
|---|---|
| `SmartMovingSelf.moveEntityWithHeading()` | `LivingEntity.travel(Vec3d)` |
| `SmartMovingBase.moveFlying()` | (동일 로직 직접 이식) |
| `SmartMovingBase.reverseHandleMaterialAcceleration()` | `FluidState.getVelocity()` 기반 재구현 필요 |
| `SmartMovingContext` 이동 상수 | vanilla 상수와 대조 |
| `SmartMovingConfig` 속도 설정 | `EntityAttributeModifier` 기반 재설계 검토 |

---

## 1. 진입점 — moveEntityWithHeading → travel()

### 원본 동작
- `EntityPlayerSP.moveEntityWithHeading(float moveStrafing, float moveForward)` 오버라이드
- PlayerAPI의 `superMoveEntityWithHeading()` 를 직접 호출하여 처리 순서 제어
- 처리 순서: `handleJumping → handleSwimming → handleLava → handleAlternativeFlying → handleLand → handleWallJumping → addMovementStat → handleExhaustion`
- `isFlying && !Config.isFlyingEnabled()` 시: `jumpMovementFactor = 0.05F`, `motionY *= 0.5999…`
- 메서드 시작 시 `prevMotionX/Z` 저장

### 1.21.1 대응
- `LivingEntity.travel(Vec3d movementInput)` 가 진입점
- PlayerAPI 없음 → **Mixin으로 `travel()` 오버라이드** 필요
- 내부 분기: 수영(isTouchingWater) → 용암(isInLava) → 엘리트라(isFallFlying) → 지상/공중

### 동작 차이
- 1.21.1은 travel() 하나로 4분기 처리. SM은 각 상태를 별도 handle*() 메서드로 명시적 분리
- SM의 `handleJumping`/`handleWallJumping` 등 추가 상태가 없음

### 포팅 주의사항
- Mixin 포인트: `@Inject` at HEAD of `travel()`, 또는 `@Overwrite` (위험) 대신 상태별 별도 Mixin 권장
- `isFlying` 처리: 1.21.1 `getAbilities().flying` 과 대응 확인 필요
- SM의 명시적 처리 순서(handleJumping 최우선 등)를 Mixin 조합으로 재현해야 함

---

## 2. 속도 팩터 시스템

### 원본 동작

최종 speedFactor = `getConfigSpeedFactor() × getPotionSpeedFactor() × getNonSlowInputSpeedFactor() × getSlowInputSpeedFactor()`

#### getConfigSpeedFactor()
- `_speedFactor` (config 값) × `getUserSpeedFactor()`
- `getUserSpeedFactor()` = `(1 + _speedUserFactor) ^ _speedUserExponent`

#### getPotionSpeedFactor()
- `getLandMovementFactor() × 10F / (isSprinting ? 1.3F : 1F)`
- `getLandMovementFactor()`: vanilla 이동 속도 포션 효과 포함 기본값 반환 (~0.1F)
- 이 값은 포션 효과, 슬로우니스 등을 반영한 vanilla 값 기반

#### getNonSlowInputSpeedFactor()
- 미끄러운 블록(slipperiness > 0.6): `_iceSpeedFactor` 적용
- 스프린팅 중: `_sprintFactor (1.5F)`
- 달리기 중: `_runFactor (1.3F)`
- 이 팩터들은 중첩 불가 (우선순위 있음)

#### getSlowInputSpeedFactor()
- 아이템 사용 중: `× 0.2F`
- 크롤링 중: `× _crawlFactor (0.15F)`
- 스니킹 중: `× _sneakFactor (0.3F)`
- 천장 클라이밍 중: `× _ceilingClimbingSpeedFactor (0.2F)`
- 이 팩터들은 순서대로 누적 적용

### 1.21.1 대응
- 이동 속도: `EntityAttributes.GENERIC_MOVEMENT_SPEED` 속성
- 스니킹: `isSneaking()` 시 `LivingEntity.getMovementSpeed()` 별도 분기 (0.3 배수 아님, vanilla는 별도 공식)
- 포션 효과: `Speed`, `Slowness` 효과는 GENERIC_MOVEMENT_SPEED에 직접 modifier로 적용됨
- 스프린트: `sprintingSpeedBoostModifier` = +0.3 곱셈 modifier

### 동작 차이
- SM의 4단계 팩터 구조는 vanilla의 단일 속성 + modifier 구조와 근본적으로 다름
- SM의 `getPotionSpeedFactor()` 공식(`getLandMovementFactor() × 10F / ...`)은 vanilla 내부 값을 역산해 SM 배율로 재해석하는 방식
- 1.21.1에서 포션 효과는 이미 `GENERIC_MOVEMENT_SPEED` 에 반영되므로 이중 적용 위험

### 포팅 주의사항
- `getPotionSpeedFactor()` 공식을 1.21.1 속성 시스템과 충돌 없이 재현하려면 별도 SM 전용 속성 추가 검토
- `getNonSlowInputSpeedFactor()` 의 스프린트 배율은 vanilla 스프린트 modifier와 중복 가능 → 기존 modifier 제거 후 SM 배율 단독 적용 여부 결정 필요
- `getSlowInputSpeedFactor()` 는 travel() 내부에서 movementInput 스케일을 조정하거나 별도 multiplier로 구현 가능

---

## 3. 수평 감쇠 상수

### 원본 상수 (SmartMovingContext)

| 상수명 | 값 |
|---|---|
| `HorizontalGroundDamping` | `0.546F` |
| `HorizontalAirDamping` | `0.91F` |
| `HorizontalAirodynamicDamping` | `0.999F` |

### 1.21.1 대응

| vanilla 경우 | 적용 감쇠 |
|---|---|
| 지상 (기본 블록 slip=0.6) | `slip × 0.91F = 0.6 × 0.91 = 0.546F` |
| 공중 | `0.91F` |
| 엘리트라 비행 | `0.9900001F` (별도 분기) |

### 동작 차이
- SM의 `HorizontalGroundDamping = 0.546F` = vanilla 기본 지상 감쇠 (slip=0.6 × 0.91F) — **값 동일**
- SM의 `HorizontalAirDamping = 0.91F` = vanilla 공중 감쇠 — **값 동일**
- SM의 `HorizontalAirodynamicDamping = 0.999F` — vanilla 엘리트라 0.9900001F와 근사값으로 사실상 동일
- 미끄러운 블록(얼음 등): SM은 별도 slip 처리, vanilla는 `slip × 0.91F` 공식 그대로 적용

### 포팅 주의사항
- 기본값 기준으로 SM과 vanilla의 감쇠값은 일치하므로 별도 상수 재정의 불필요
- 미끄러운 블록 처리: vanilla의 `BlockState.getSlipperiness()` 값을 SM의 ice 특수처리와 통합 필요

---

## 4. 중력 적용 — setLandMotions()

### 원본 동작
```
motionY -= 0.08D;   // 중력 적용
motionY *= 0.98D;   // 수직 감쇠
motionX *= horizontalDamping;
motionZ *= horizontalDamping;
```
- `horizontalDamping`: 지상이면 `block.slipperiness × HorizontalAirDamping`, 공중이면 `HorizontalAirDamping`
- 지상 속도 원시값(rawSpeed): `0.1627714F / (horizontalDamping³) × 0.1F`
  - 기본 지상: `0.1627714F / (0.546)³ × 0.1F = 0.1F` (정규화된 값)

### 1.21.1 대응
- `travel()` 지상/공중 분기:
  - `q -= getFinalGravity()` (GENERIC_GRAVITY 속성, 기본값 추정 0.08D — **미확인**)
  - `q *= 0.98F`
  - `velocity.multiply(friction, 0.98F, friction)` — `friction = isOnGround ? slip*0.91F : 0.91F`
- `getMovementSpeed(slip)`: 지상 = `movementSpeed × 0.21600002F / slip³`
  - slip=0.6 기준: `movementSpeed × 0.21600002F / 0.216F = movementSpeed × 1.0F`

### 동작 차이
- 중력 상수 0.08D: SM 하드코딩 vs vanilla GENERIC_GRAVITY 속성 (속성값이 0.08D인지 **미확인**)
- 수직 감쇠 0.98D/F: 양쪽 동일
- 지상 속도 공식: SM `0.1627714F / d³ × 0.1F` ↔ vanilla `movementSpeed × 0.21600002F / slip³` — 기본값 기준 동치이나 `movementSpeed` 기준점이 다름

### 포팅 주의사항
- `getFinalGravity()` 반환값이 정확히 0.08D인지 실제 속성 기본값 확인 필수
- SM의 중력 하드코딩을 GENERIC_GRAVITY 속성으로 교체하면 중력 포션 효과와 자동 연동 가능
- `rawSpeed` 공식은 SM 내부에서만 사용되는 것으로 보임 — vanilla `getMovementSpeed()` 와 동일 역할이면 vanilla 것 사용 권장

---

## 5. 지상 이동 — landMotion() / handleLand()

### 원본 동작
- 블록 슬립: `block.slipperiness` (vanilla `getBlockState().getSlipperiness(world, pos, entity)`)
- 지상 damping: `block.slipperiness × HorizontalAirDamping (0.91F)`
- 공중 damping: `HorizontalAirDamping (0.91F)`
- 지상 `rawSpeed = 0.1627714F / (damping³) × 0.1F`
- 공중 `rawSpeed`: `jumpMovementFactor` (vanilla `offGroundSpeed`) 사용
- 사다리 상태: `motionX/Z ±0.15F 클램프`

### 1.21.1 대응
- `applyClimbingSpeed()`: `isClimbing()` (CLIMBABLE 태그 블록만 해당) 시 x/z ±0.15F 클램프
- `isHoldingOntoLadder()` == `isSneaking()` 이면 y 속도=0

### 동작 차이
- SM의 사다리 클램프는 SM 이동 파이프라인 내에서 적용
- vanilla `applyClimbingSpeed()`는 `isClimbing()` 기준 → SM 커스텀 클라이밍(벽 등)은 해당 안 됨
- vanilla `applyClimbingSpeed()`가 SM 커스텀 클라이밍을 간섭할 경우: SM 클라이밍 중 `isClimbing()` false 보장 필요 (또는 `applyClimbingSpeed()` Mixin으로 우회)

### 포팅 주의사항
- SM 클라이밍 중 vanilla `isClimbing()` 이 false여야 `applyClimbingSpeed()` 간섭 없음
- `BlockState.getSlipperiness()` → 1.21.1에서 `Block.getSlipperiness()` 또는 `AbstractBlock.Settings.slipperiness` 로 접근 방식 변경 확인 필요

---

## 6. 속도 컷오프 — 0.005 우회

### 원본 동작
```java
// moveEntityWithHeading() 시작
double prevMotionX = sp.motionX;
double prevMotionZ = sp.motionZ;
// ... (처리 후)
// vanilla가 |motionX| < 0.005 → 0 처리하므로:
if (sp.motionX == 0 && prevMotionX < 0.005) sp.motionX = prevMotionX;
```
- vanilla 1.7.10에서 `|motionX/Z| < 0.005` 이면 0으로 잘라냄
- SM은 prevMotionX 저장 후 복원으로 우회

### 1.21.1 대응
- `tickMovement()`: `|velocity| < 0.003` 이면 0으로 잘라냄 (축별 아닌 전체 벡터 길이 기준 — 정확한 구현 **미확인**)
- 컷오프 임계값 0.003 (1.7.10의 0.005보다 작음)

### 동작 차이
- 임계값 변경: 0.005 → 0.003
- 컷오프 적용 시점: 1.7.10은 moveEntityWithHeading 내부, 1.21.1은 tickMovement에서 travel() 호출 전
- SM 클라이밍 속도 상수: `ClimbDownMotion=0.01D`, `SinkDownMotion=0.05D`, `HoldMotion=0.08D` → 모두 0.003 초과이므로 직접 컷오프 피해는 없음
- 단, SM 처리 후 velocity가 0.003 이하로 줄어드는 경우는 여전히 0으로 스냅될 수 있음

### 포팅 주의사항
- 0.003 스냅이 SM 클라이밍 중 의도치 않게 발동하는 경우 확인 필요
- SM의 0.005 우회 로직은 1.21.1에서 0.003 기준으로 재검토 필요

---

## 7. 수영 처리 — handleSwimming()

### 원본 동작
- `playerSwimWaterBorder + 0.1625D` 기준 3상태 분기:
  - `dipping` (수면 경계 미만): 감쇠 0.85D, 수평 이동 가능
  - `swimming` (1.4 ~ 1.9 범위): 감쇠 0.83D
  - `diving` (1.9 이상, 완전 수중): 수평 0.80D/수직 0.83D
- 물 탈출 점프: `motionY = 0.3D`
- 다이빙 중 상승(diveUp): `isp.getIsJumpingField()` 사용
- 수영 속도: `moveFlying(speedFactor × _swimSpeedFactor, moveStrafing, (diveUp?...:0), moveForward)`
- 수영 소리: `SwimSoundDistance` 누적 후 임계값 도달 시 재생

### 1.21.1 대응
- `travel()` 수영 분기: `isTouchingWater() && !canWalkOnFluid(waterFluid)`
- 수평 감쇠: sprinting ? 0.9F : 0.8F
- 수직 감쇠: 0.8F (gravity/16 감산)
- 상승: Space 키 → `movementInput.y` 양수값
- `WATER_MOVEMENT_EFFICIENCY` 속성 존재 (돌핀의 은혜 등)

### 동작 차이
- SM의 3상태 분기(dipping/swimming/diving) vs vanilla 단일 수영 분기
- 감쇠값: SM 0.85D/0.83D/0.80D vs vanilla 0.9F/0.8F (스프린팅 여부)
- SM의 수면 경계 오프셋 계산 로직이 없음 (vanilla는 eyeHeight 기준)
- SM `diveUp` = 점프 키 기준 vs vanilla `movementInput.y` = Space 키

### 포팅 주의사항
- SM의 3상태 수영을 1.21.1에서 재현하려면 플레이어 눈 높이와 수면 높이 계산 필요
- vanilla 수영 감쇠(0.8F, 0.9F)와 SM 감쇠(0.83D, 0.85D)가 다르므로 SM 고유 값 적용 시 travel() 내부 override 필요
- `_swimSpeedFactor`, `_diveSpeedFactor` 설정값 반영 위치 결정 필요

---

## 8. 용암 처리 — handleLava()

### 원본 동작
- 속도: 0.02F
- 수평 감쇠: `× 0.5D`
- 중력: `motionY -= 0.02D`
- 벽 충돌 감지 → `motionY = 0.3D` (탈출 시도)

### 1.21.1 대응
- `travel()` 용암 분기: `isInLava() && !canWalkOnFluid(lavaFluid)`
- 수평 감쇠: 0.5F
- 수직: gravity/4 감산

### 동작 차이
- 용암 처리 기본 구조 유사
- SM의 벽 충돌 → 상승 로직은 vanilla에 없음
- gravity 처리: SM 0.02D 하드코딩 vs vanilla gravity/4

### 포팅 주의사항
- SM의 용암 벽 충돌 상승 로직은 1.21.1에서 재현 가능 (충돌 감지 방법 확인 필요)

---

## 9. 대안 비행 — handleAlternativeFlying()

### 원본 동작
- 스니킹: `motionY += 0.15D`, `moveUpward -= 0.98F`
- 점프: `motionY -= 0.15D`, `moveUpward += 0.98F`
- 비행 속도: `speedFactor × 0.05F × _flyingSpeedFactor`
- 수평 감쇠: `HorizontalAirDamping (0.91F)`
- `moveFlying(speed, moveStrafing, moveUpward, moveForward)` 호출

### 1.21.1 대응
- `getAbilities().flying` 시 `travel()` 내부 별도 분기
- 스니킹/점프로 수직 이동

### 동작 차이
- SM은 config 활성화된 경우에만 대안 비행 허용
- vanilla creative 비행과 동작 방식 유사하나 속도 팩터 차이

### 포팅 주의사항
- `isFlying && !Config.isFlyingEnabled()` 시 `motionY *= 0.5999…` 처리 (비행 억제) 재현 필요

---

## 10. 클라이밍 속도 — handleClimbing() / setOnlyShouldClimbSpeed()

### 원본 속도 상수 (SmartMovingContext)

| 상수명 | 값 |
|---|---|
| `FastUpMotion` | `0.2D` |
| `MediumUpMotion` | `0.14D` |
| `SlowUpMotion` | `0.1D` |
| `HoldMotion` | `0.08D` |
| `SinkDownMotion` | `0.05D` |
| `ClimbDownMotion` | `0.01D` |

### handleClimbing() 분기

#### Standard 클라이밍 (사다리/덩굴)
- 상승: `0.2D × combinedSpeedFactor`

#### Simple 클라이밍
- 빠른 상승: `FastUpMotion (0.2D)`
- 느린 상승: `SlowUpMotion (0.1D)`

#### Free 클라이밍 (벽 오르기)
- `ClimbGap` 탐색 후 이동 가능한 공간 계산
- 속도는 공간 크기에 따라 결정

### setOnlyShouldClimbSpeed() 로직
```
relevant = value < 0 || value > motionY  // 더 느리거나 하강 방향일 때만 적용
상방(value > 0):
  motionY = (value - HoldMotion) × upSpeedFactor × combinedSpeedFactor + HoldMotion
하방(value < 0):
  motionY = HoldMotion - (HoldMotion - value) × downSpeedFactor × combinedSpeedFactor
```
- `relevant` 조건을 만족하지 않으면 motionY 변경 안 함 (이미 충분히 빠른 경우 방치)

### handleCeilingClimbing() 속도
- jgap > 1.2 → 수평 속도 0.12 (상수)
- jgap > 1.115 → 수평 속도 0.08
- else → 수평 속도 0.04
- `fallDistance = 0` 강제 초기화

### 1.21.1 대응
- `applyClimbingSpeed()`: CLIMBABLE 태그 블록에서만 발동 → SM 커스텀 클라이밍과 무관
- SM 클라이밍 중에는 vanilla `isClimbing()` = false이어야 간섭 없음

### 동작 차이
- vanilla에 SM의 Free/Simple/Ceiling 클라이밍 개념 없음
- vanilla `applyClimbingSpeed()` 의 x/z ±0.15F 클램프가 SM 클라이밍 중 발동되면 수평 이동 제한
- 1.21.1 velocity snap 0.003: `ClimbDownMotion=0.01D`, `SinkDownMotion=0.05D` 는 0.003 초과이므로 직접 피해 없음. 단 `setOnlyShouldClimbSpeed()` 후 값이 0.003 이하로 떨어지는 극단 케이스는 위험

### 포팅 주의사항
- SM 커스텀 클라이밍 중 `isClimbing()` override 필수 (false 반환)
- `applyClimbingSpeed()` 를 Mixin으로 SM 클라이밍 상태 시 skip 처리
- `setOnlyShouldClimbSpeed()` 의 `relevant` 조건 로직은 1.21.1 Vec3d 기반으로 그대로 이식 가능

---

## 11. moveFlying() — 비표준 정규화 공식

### 원본 동작 (SmartMovingBase)
```java
float total = MathHelper.sqrt_float(MathHelper.sqrt_float(x*x + z*z) + y*y);
// ↑ sqrt(sqrt(x²+z²) + y²) — 일반 3D 유클리드 거리 아님
if (total < speed) return;
total = speed / total;
x *= total; y *= total; z *= total;
```

### 1.21.1 대응
- vanilla `applyMovementInput()` 내부:
  - `float g = sqrt(x*x + y*y + z*z)` — 표준 3D 유클리드 거리
  - SM과 다른 정규화 공식

### 동작 차이
- SM 공식 `sqrt(sqrt(x²+z²) + y²)` ≠ 표준 `sqrt(x²+y²+z²)`
- 예시: x=1, y=1, z=0 → SM: `sqrt(sqrt(1)+1) = sqrt(2.414) ≈ 1.554` / 표준: `sqrt(2) ≈ 1.414`
- SM 공식이 수직 성분에 더 큰 가중치를 줌 → 수직+수평 동시 이동 시 SM이 더 느림

### 포팅 주의사항
- SM의 `moveFlying()` 은 수영/비행/다이빙에서 호출됨
- 공식이 의도적 설계인지 버그인지 판단 필요 (원본 SmartRender/SmartMoving 공통 코드이므로 의도적 설계로 추정 — **확인 필요**)
- vanilla `applyMovementInput()` 사용 시 동작 차이 발생 → SM 공식 유지 원한다면 별도 메서드로 직접 구현

---

## 12. reverseHandleMaterialAcceleration()

### 원본 동작 (SmartMovingBase)
```java
// 물 흐름 가속을 역전
AxisAlignedBB box = entity.boundingBox.expand(0, -0.4D, 0).contract(0.001D);
// 해당 영역의 물 흐름 벡터 계산
Vec3d flowVec = world.getFlowingBlock(box); // 정규화된 흐름 벡터
// 역방향으로 -0.014D 크기만큼 적용
motionX += -flowVec.xCoord * 0.014D;
motionY += -flowVec.yCoord * 0.014D;
motionZ += -flowVec.zCoord * 0.014D;
```
- vanilla `handleMaterialAcceleration()` 에서 물 흐름 += 0.014D 적용 → SM이 이를 상쇄

### 1.21.1 대응
- `FluidState.getVelocity(BlockView, BlockPos)` → 흐름 벡터 반환
- `Entity.applyFluidMovingSpeed(double gravity, boolean falling, Vec3d velocity)` 내에서 흐름 적용
- 흐름 상쇄를 위해 해당 Mixin 포인트 또는 별도 로직 필요

### 동작 차이
- 1.21.1 API: `FluidState.getVelocity()` 로 흐름 벡터 취득 방법 변경
- 흐름 가속 크기(0.014D)가 1.21.1에서 동일한지 **미확인**

### 포팅 주의사항
- 1.21.1에서 물 흐름 가속 적용 코드 위치 및 크기값 확인 필수
- `reverseHandleMaterialAcceleration()` 호출 시점: SM 수영 처리 전 (흐름 상쇄 후 SM 속도 적용)

---

## 13. beforeMoveEntity() / afterMoveEntity()

### 원본 동작

#### beforeMoveEntity()
- 스니킹 + 조건 충족 시: `ySize = 0F` (지면 보정 비활성화) 또는 `ySize = 0.6F`
- `wantWallJumping == true` → `calculateSeparateCollisions()` 호출 (벽 점프 방향 결정)

#### afterMoveEntity()
- `heightOffset > 0` → `posY += heightOffset` (슬라이딩, 크롤링 등 높이 보정 후 위치 수동 조정)
- 클라이밍 이동 거리 누적: 지상 1.2 / 공중 0.9 (피로도 계산용)
- 수영 소리: `SwimSoundDistance` 누적, `SwimSoundDistance > 1.0D` 시 소리 재생

### 1.21.1 대응
- `ySize` 개념 없음 (1.21.1 vanilla 가 step up 후 rendering 보간을 자체 처리)
- `calculateSeparateCollisions()` → `Entity.move()` 와 `VoxelShapes.calculateMaxOffset()` 기반으로 재구현
- `heightOffset` 처리: 1.21.1 `Entity.setPosition()` 또는 `Entity.setPos()` 로 위치 직접 조정

### 동작 차이 — ⚠️ 정정 (fix #73, 2026-05-10)

**기존 매핑 노트 (잘못됨)**:
> ~~`ySize` (1.7.10의 계단 오르기 높이): 1.21.1의 `stepHeight` 속성으로 대체됨 (기본값 0.6)~~
> ~~SM의 `ySize = 0F` 비활성화는 1.21.1에서 `STEP_HEIGHT` 속성을 0으로 수정하는 방식으로 재현 가능~~

**정정**:
- 1.7.10 vanilla `Entity.ySize` 와 `Entity.stepHeight` 는 **별개 변수**.
  - `stepHeight` (vanilla player 0.5F): 실제 step up 가능 높이.
  - `ySize`: step up **후** 박스 위치를 부드럽게 보간하기 위한 rendering offset.
- `SmartMovingBase.move:685` 의 step 검사식 `(flag || ySize < 0.05F)` 는 "직전 step 보간이 거의 끝나야 새 step 허용" 진동 방지 가드.
- 원본 `sp.ySize = 0F` 강제 효과는 **이 가드를 즉시 해제 = step up 더 자주 허용** (= step 차단의 정반대).
- 원본 `SmartMovingBase.move` + `SmartMovingSelf.beforeMoveEntity` 모두 `sp.stepHeight` 자체를 변경 안 함 → vanilla 0.5F 그대로 사용.
- 1.12.2 SMReboot 도 동일 (`SMBase.java:558` 만 `sp.stepHeight` 읽음, 변경 X).

**1.21.1 매핑 결론**: `ySize` 는 1.21.1 vanilla 가 step up 후 rendering 보간을 자체 처리하므로 등가 매핑 자체가 불필요. `STEP_HEIGHT` 변경은 잘못된 매핑이라 제거.

### 포팅 주의사항
- `beforeMoveEntity()` / `afterMoveEntity()` 호출 위치: vanilla `move()` 전후에 Mixin 삽입
- `heightOffset` 적용 타이밍: `move()` 완료 후 `setPos()` 호출 순서 중요

---

## 14. updateLeaningPitch() 간섭

### 원본 동작
- SM 수영/크롤링이 `EntityPose.SWIMMING` 사용 시 `isInSwimmingPose() = true`

### 1.21.1 동작
- `updateLeaningPitch()`: `isInSwimmingPose()` = true → `leaningPitch += 0.09F/틱` (11틱에 1.0 도달)
- `leaningPitch = 1.0` 시 `PlayerEntityRenderer.setupTransforms()` Branch 2 발동 → 몸 90도 회전

### 동작 차이
- SM 크롤링 중 SWIMMING 포즈 사용 시 leaningPitch가 자동 증가 → 렌더링에서 몸이 눕는 효과 발생
- 이것이 의도한 동작인지(크롤링 시 납작한 자세) 아닌지에 따라 처리 방식 결정 필요

### 포팅 주의사항
- SM 크롤링 = SWIMMING 포즈 사용 예정이라면 leaningPitch 증가는 자연스럽게 작동함
- 단, leaningPitch 전환 속도(0.09F/틱)가 SM 원본의 크롤링 진입 속도와 일치하는지 확인 필요

---

## 15. 서버 검증 연관성 (ServerPlayNetworkHandler)

### 원본 동작
- SM 클라이언트가 커스텀 이동(클라이밍, 슬라이딩 등) 시 서버에 패킷 전송
- `SmartMovingServerComm` / `SmartMovingPacketStream` 으로 별도 SM 상태 패킷 전송

### 1.21.1 vanilla 검증 제약

| 검증 | 임계값 | SM 영향 |
|---|---|---|
| moved too quickly | distanceSq - velocityLenSq > 100 × packetCount | 클라이밍 고속 이동 시 rubber-band 위험 |
| moved wrongly | postDistanceSq > 0.0625 | 서버 물리와 클라이언트 위치 불일치 |
| floating | floatingTicks > 80 | 클라이밍 중 공중 유지 시 kick |

### 포팅 주의사항
- SM 커스텀 이동 상태를 서버에서도 인식해야 검증 통과 가능
- 클라이밍 중 `floating` 판정 방지: 서버 측에서 SM 클라이밍 상태를 알고 floating=false 처리 필요
- 별도 SM 상태 패킷(33비트 직렬화) → 1.21.1 `CustomPayloadC2SPacket` 기반 재구현

---

## 16. 수영 경계 상수 (SmartMovingContext)

| 상수명 | 값 | 용도 |
|---|---|---|
| `SwimCrawlWaterBorder` | (미확인, 원본에 있음) | dipping/swimming 경계 |
| `playerSwimWaterBorder` | (미확인) | 수면 기준 오프셋 |
| `SwimSoundDistance` | 임계값 (미확인) | 수영 소리 재생 거리 |

**미확인 항목**: 실제 상수값은 SmartMovingContext.md에 정확한 값이 기재되어 있으나 본 분석 시점에 재확인 필요.

---

## 전체 포팅 난이도 요약

| 항목 | 난이도 | 이유 |
|---|---|---|
| 진입점 (moveEntityWithHeading → travel) | ★★★★☆ | Mixin 구조 재설계 + 처리 순서 재현 |
| 속도 팩터 4단계 | ★★★☆☆ | vanilla 속성 시스템과 통합 설계 필요 |
| 수평/수직 감쇠 | ★☆☆☆☆ | 기본값 일치, 특수 케이스만 주의 |
| 중력 적용 | ★★☆☆☆ | GENERIC_GRAVITY 속성값 확인 후 결정 |
| 수영 3상태 | ★★★☆☆ | 수면 경계 계산 재구현 필요 |
| 클라이밍 속도 | ★★★☆☆ | applyClimbingSpeed() 간섭 우회 필요 |
| moveFlying 비표준 공식 | ★★☆☆☆ | 코드 직접 이식 가능, 의도 확인 필요 |
| reverseHandleMaterialAcceleration | ★★★☆☆ | 1.21.1 FluidState API 재조사 필요 |
| velocity snap 0.003 | ★★☆☆☆ | 클라이밍 최소 속도값과 비교 — 현재 안전 |
| beforeMoveEntity/afterMoveEntity | ★★☆☆☆ | ySize 매핑 불필요 (정정 fix #73), heightOffset 처리만 |
| 서버 검증 통과 | ★★★★★ | floating kick, moved wrongly 모두 SM이 우회해야 함 |
| updateLeaningPitch 간섭 | ★★☆☆☆ | SWIMMING 포즈 크롤링이라면 의도적 동작 가능 |

**전체 난이도**: 높음. 특히 서버 검증 통과 (floating 판정, moved too quickly)와 travel() Mixin 구조 설계가 최우선 해결 과제.
