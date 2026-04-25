# 포커스 #2.5 — Jumper Factor 인프라 전수 이식

> **B-42-B26 확장 포커스**. 원본 `SmartMovingSelf.tryJump()` (L1999-L2136) +
> `SmartMovingClientConfig.isJumpingEnabled/getJumpHorizontalFactor/getJumpVerticalFactor`
> (L194-L547) 의 Factor 인프라 + Exhaustion 시스템을 1.21.1 Fabric 으로 **엄격 1:1** 이식.
>
> **목표**: 원본 Config 의 사용자 수정 (speed별 factor 변경, Jump 활성/비활성, ChargeUp 배율 등)
> 이 1.21.1 에서도 **동일하게 반영**되는 완전 이식.
>
> **진입 배경**: Extended #2 세션 134 완결 시 B-42-B26 은 경량 이식 (factor 기본 1F 근사 —
> 미수정 Config 에서는 1:1 동치) 으로 처리됐으나, 사용자 Config 수정 시 반영 안 됨이 확인됨.
> Extended 범위 외로 분리하여 별도 포커스로 완결.

---

## 0. 현재 상태 (진입 시점)

### 이식된 Factor 필드 (2026-04-25 기준)
| 원본 필드 | 1.21.1 필드 | 상태 |
|---|---|---|
| `_wallUpJumpFallMaximumDistance` | `wallUpJumpFallMaximumDistance = 2F` | ✅ 이식 |
| `_wallUpJumpOrthogonalTolerance` | `wallUpJumpOrthogonalTolerance = 5F` | ✅ 이식 |
| `_wallUpJumpVerticalFactor` | `wallUpJumpVerticalFactor = 0.4F` | ✅ 이식 |
| `_wallHeadJumpFallMaximumDistance` | `wallHeadJumpFallMaximumDistance = 3F` | ✅ 이식 |
| `_wallHeadJumpVerticalFactor` | `wallHeadJumpVerticalFactor = 0.3F` | ✅ 이식 |
| `_jumpChargeMaximum` | `jumpChargeMaximum` | ✅ 이식 |
| `_jumpChargeFactor` | `jumpChargeFactor` | ✅ 이식 |
| `_headJumpChargeMaximum` | `headJumpChargeMaximum` | ✅ 이식 |
| `_headJumpControlFactor` | `headJumpControlFactor` | ✅ 이식 |
| `_angleJumpDoubleClickTicks` | `angleJumpDoubleClickTicks` | ✅ 이식 |
| `_freeClimbingUpSpeedFactor` | `freeClimbingUpSpeedFactor` | ✅ 이식 |
| `_freeClimbingDownSpeedFactor` | `freeClimbingDownSpeedFactor` | ✅ 이식 |
| `_sprintFactor` | `sprintFactor` | ✅ 이식 |

### 미이식 Factor 필드 (이 포커스 범위)
| 원본 필드 | 기본값 | 미이식 이유 |
|---|---|---|
| `_jumpHorizontalFactor` | 1F (IncreasingFactor) | base factor — Jump 전체에 공통 |
| `_jumpVerticalFactor` | 1F (PositiveFactor) | base factor — Jump 전체에 공통 |
| `_jumpControlFactor` | 1F (DecreasingFactor) | 공중 이동 제어 factor |
| `_standJumpVerticalFactor` | 1F | Standing 점프 vertical |
| `_sneakJumpHorizontalFactor` | 1F | Sneaking 점프 horizontal |
| `_sneakJumpVerticalFactor` | 1F | Sneaking 점프 vertical |
| `_walkJumpHorizontalFactor` | 1F | Walking 점프 horizontal |
| `_walkJumpVerticalFactor` | 1F | Walking 점프 vertical |
| `_runJumpHorizontalFactor` | **2F** | Running 점프 horizontal (오버라이드) |
| `_runJumpVerticalFactor` | 1F | Running 점프 vertical |
| `_sprintJumpHorizontalFactor` | **2F** | Sprinting 점프 horizontal (오버라이드) |
| `_sprintJumpVerticalFactor` | 1F | Sprinting 점프 vertical |
| `_angleJumpHorizontalFactor` | **0.4F** (sm_1_3 이상 오버라이드, 이전 0.3F) | Side/Back 점프 horizontal |
| `_angleJumpVerticalFactor` | **0.2F** | Side/Back 점프 vertical |
| `_climbUpJumpVerticalFactor` | 1F | ClimbUp 점프 vertical |
| `_climbUpJumpHandsOnlyVerticalFactor` | **0.8F** | ClimbUp Hands-only 추가 |
| `_climbBackUpJumpVerticalFactor` | **0.2F** | ClimbBackUp vertical |
| `_climbBackUpJumpHorizontalFactor` | **0.3F** | ClimbBackUp horizontal |
| `_climbBackUpJumpHandsOnlyVerticalFactor` | **0.8F** | Hands-only 추가 v |
| `_climbBackUpJumpHandsOnlyHorizontalFactor` | 1F | Hands-only 추가 h |
| `_climbBackHeadJumpVerticalFactor` | **0.2F** | ClimbBackHead vertical |
| `_climbBackHeadJumpHorizontalFactor` | **0.3F** | ClimbBackHead horizontal |
| `_climbBackHeadJumpHandsOnlyVerticalFactor` | **0.8F** | Hands-only 추가 v |
| `_climbBackHeadJumpHandsOnlyHorizontalFactor` | 1F | Hands-only 추가 h |
| `_wallUpJumpHorizontalFactor` | **0.15F** | WallUp horizontal |
| `_wallHeadJumpHorizontalFactor` | **0.15F** | WallHead horizontal |
| `_standJump` | true | Standing 점프 활성 |
| `_sneakJump` | true | Sneaking 점프 활성 |
| `_walkJump` | true | Walking 점프 활성 |
| `_runJump` | true | Running 점프 활성 |
| `_sprintJump` | true | Sprinting 점프 활성 |
| `_jumpChargeCancelOnSneakRelease` | false (Modified) | ChargeUp 취소 동작 (Properties.java L173 Modified=false) |
| `_angleJumpSide` | true | Side 점프 활성 |
| `_angleJumpBack` | true | Back 점프 활성 |
| `_climbUpJump` | true | ClimbUp 점프 활성 |
| `_climbBackUpJump` | true | ClimbBackUp 활성 |
| `_climbBackHeadJump` | true | ClimbBackHead 활성 |
| `_wallHeadJump` | true | WallHead 활성 |
| `_headFallDamageStartDistance` | 2F | 헤드점프 낙하 데미지 시작 거리 |
| `_headFallDamageFactor` | **2F** | 헤드점프 낙하 데미지 배율 |

### 미이식 판정 메서드 (이 포커스 범위)
| 원본 메서드 | 소비처 |
|---|---|
| `isJumpingEnabled(speed, type)` | tryJump 진입 조건 |
| `getJumpHorizontalFactor(speed, type)` | tryJump horizontalJumpFactor 계산 |
| `getJumpVerticalFactor(speed, type)` | tryJump verticalJumpFactor 계산 |
| `getMaxHorizontalMotion(speed, type, inWater)` | tryJump maxHorizontalMotion 클램프 |
| `getJumpChargeFactor(charge)` | tryJump jumpChargeFactor (ChargeUp 만) |
| `getHeadJumpFactor(charge)` | tryJump head 재계산 분기 |
| `getJumpSpeed(isStanding, isSneaking, isRunning, isSprinting, angle)` | tryJump speed 결정 |

### 미이식 Exhaustion 시스템 (이 포커스 범위 — Phase C)
- `exhaustion` / `maxExhaustionToStartAction` / `maxExhaustionForAction` 필드
- `isJumpExhaustionEnabled(speed, type)`
- `getJumpExhaustionGain(speed, type, charge)`
- `getJumpExhaustionStop(speed, type, charge)`
- `getMaxExhaustion()`
- Exhaustion 관련 Config 30+ 필드

### 현 Jumper 구현 (1.21.1)
`SmartMovingJumper.tryJump(player, sm, jumpType, charge)` — 경량 이식 상태:
- jumpType: UP / CHARGE_UP / HEAD_UP / WALL_UP / WALL_HEAD / WALL_UP_SLIDE / WALL_HEAD_SLIDE / SLIDE_DOWN (세션 134)
- Exhaustion 완전 미이식
- Factor 인프라 하드코딩 (WALL_UP = 0.4F, WALL_HEAD = 0.3F 만 반영, 기타 speed 별 factor 미반영)
- `trySlideDownJump` 별도 메서드 (세션 134 B-42-B26 경량)

---

## 1. 1:1 번역 룰

원본 `SmartMovingSelf.java` L1999-L2136 `tryJump()` 본체 + `SmartMovingClientConfig.java`
L172-L547 Jumper 인프라를 **축약·간소화·대체 매핑 금지**. vanilla API 표면 매핑만 허용.

### 표면 매핑 (허용)
| 원본 | 1.21.1 |
|---|---|
| `sp.motionX/Y/Z`, `sp.setMotion` | `player.getVelocity()`, `player.setVelocity(Vec3d)` |
| `sp.isPotionActive(Potion.jump)` | `player.hasStatusEffect(StatusEffects.JUMP_BOOST)` |
| `sp.getActivePotionEffect(Potion.jump).getAmplifier()` | `jumpBoost.getAmplifier()` |
| `sp.isSprinting()`, `sp.setSprinting` | `player.isSprinting()`, `player.setSprinting()` |
| `sp.onGround`, `sp.isCollidedHorizontally` | `player.isOnGround()`, `player.horizontalCollision` |
| `sp.fallDistance`, `sp.isAirBorne` | `player.fallDistance`, (vanilla 자동 관리) |
| `sp.rotationYaw` | `player.getYaw()` |
| `sp.isBurning`, `sp.isUsingItem` | `player.isOnFire`, `player.isUsingItem` |
| `sp.addStat(StatList.jumpStat, 1)` | `player.incrementStat(Stats.JUMP)` |
| `sp.ridingEntity != null` | `player.hasVehicle()` |
| `MathHelper.sqrt_float/sqrt_double` | `(float)Math.sqrt` / `Math.sqrt` |
| `esp.movementInput.moveForward` | `player.input.movementForward` |
| `Property<Float>.value` | `float` 필드 직접 |
| `Property<Boolean>.value` | `boolean` 필드 직접 |

### 금지
- `factor = 1F` 하드코딩 근사 (base + speed별 + override 전수 반영 필수)
- `!cfg.xxx` 단순 근사 (`isJumpingEnabled(speed, type)` 호출 체인 전수 이식)
- Exhaustion 누락 — Phase C 에서 전수 이식

### 예외 (1.21.1 API 제약)
- `Property.versionSources/versionDefaults/min/max/depends/chapter/book/comment` 등 속성 시스템
  완전 이식 불필요 — 단순 boolean/float 필드로 이식 (값만 정확히).
- `_sm_1_3` / `_pre_sm_1_7` 등 버전별 기본값 → 최신 버전 기본값 채택 (오버라이드는 모두 최신).
- Exhaustion 의 "책 UI (Book)" 은 Config GUI — 이식 불필요 (file-based properties 만).

---

## 2. 원본 구조 매핑

### 2.1 Jump Type 상수 (SmartMovingClientConfig L178-L192)
```java
Up=0, ChargeUp=1, Angle=2, HeadUp=3, SlideDown=4,
ClimbUp=5, ClimbUpHandsOnly=6, ClimbBackUp=7, ClimbBackUpHandsOnly=8,
ClimbBackHead=9, ClimbBackHeadHandsOnly=10,
WallUp=11, WallHead=12, WallUpSlide=13, WallHeadSlide=14
```

**1.21.1 `SmartMovingJumper` 현 상태**:
```java
UP=0, CHARGE_UP=1, HEAD_UP=2, WALL_UP=3,
CLIMB_UP=4, CLIMB_BACK=5, CLIMB_BACK_HEAD=6,
LEFT=7, RIGHT=8, BACK=9, WALL_HEAD=10,
WALL_UP_SLIDE=11, WALL_HEAD_SLIDE=12, SLIDE_DOWN=13
```

**매핑**:
- `Angle` 은 1.21.1 에서 `LEFT/RIGHT/BACK` 으로 분리 (각도 파라미터로 구별). 원본도 `tryAngleJump(angle)` 에서 `Angle` type + angle 파라미터 조합. **1:1 이식 시** 원본 `Angle` 단일 type + angle 파라미터 조합으로 복원 검토 필요. 현 분리는 결과 동일이나 type 매핑 재정렬 필요.
- `ClimbUpHandsOnly`, `ClimbBackUp`, `ClimbBackUpHandsOnly`, `ClimbBackHeadHandsOnly` 누락 — 현재 1.21.1 에는 `ClimbUp/ClimbBack/ClimbBackHead` 만. HandsOnly 분기는 `feetClimbing.isNone() && handsClimbing.isUp()` 상태에서 사용되는데 type 자체가 별도.

### 2.2 Speed 상수 (SmartMovingClientConfig L172-L176)
```java
Sprinting=0, Running=1, Walking=2, Sneaking=3, Standing=4
```

**1.21.1 미이식** — `getJumpSpeed` 헬퍼 신설 + 상수 추가 필요.

### 2.3 원본 tryJump 본체 (SmartMovingSelf L1999-L2136)
[`docs/research/original/jumper_tryjump_full.md`](../research/original/jumper_tryjump_full.md) 로
전수 복사 (Phase F 전수 감사 단계에서 참조).

요약 흐름:
1. **L2002-L2006**: WallUpSlide/WallHeadSlide → noVertical=true + 비-Slide type 변환
2. **L2008-L2012**: inWater/isRunning/charged/up/head 지역 변수
3. **L2014**: `speed = getJumpSpeed(...)` 호출
4. **L2015**: `enabled = isJumpingEnabled(speed, type)`
5. **L2016-L2027**: enabled 시 exhaustion 체크 + maxExhaustion 조정
6. **L2029-L2032**: jumpFactor (potion) + horizontalJumpFactor + verticalJumpFactor + jumpChargeFactor
7. **L2034-L2038**: !up → `horizontalJumpFactor = sqrt(h² + v²)`, verticalJumpFactor = 0
8. **L2040-L2045**: maxHorizontalMotion (horizontalJumpFactor > 1F && !collidedHorizontally 시)
9. **L2047-L2062**: type==Up && vanilla → verticalMotion = 0.41999... + potion + sprint 수평 보정
10. **L2063-L2111**: else (Up 아님 또는 !vanilla)
    - head → normalAngle 재계산
    - angle != null → moveX/Z 수평 이동
    - horizontalMotion > 0 → 수평 속도 스케일
11. **L2113-L2118**: up && !noVertical → motionY = verticalMotion + Stats.JUMP + isSprintJump
12. **L2120-L2124**: exhausionEnabled → exhaustion += gain
13. **L2126-L2130**: head → isHeadJumping=true + setHeightOffset(-1)
14. **L2131-L2134**: isAirBorne=true + isJumping=true + onLivingJump()

---

## 3. Phase 구조

### Phase A. Config Factor 필드 전수 이식 (SmartMovingConfig.java)

**A-1. Jump base factor 3건**
- [x] A-1a. `jumpHorizontalFactor = 1F` (원본 L230, IncreasingFactor >= 1) — 세션 1
- [x] A-1b. `jumpVerticalFactor = 1F` (원본 L231, PositiveFactor >= 0) — 세션 1
- [x] A-1c. `jumpControlFactor = 1F` (원본 L228, DecreasingFactor >= 0, <= 1) — 세션 1

**A-2. Speed 별 Horizontal/Vertical factor 9건**
- [x] A-2a. `standJumpVerticalFactor = 1F` (원본 L235) — 세션 2
- [x] A-2b. `sneakJumpHorizontalFactor = 1F` (원본 L238) — 세션 2
- [x] A-2c. `sneakJumpVerticalFactor = 1F` (원본 L239) — 세션 2
- [x] A-2d. `walkJumpHorizontalFactor = 1F` (원본 L242) — 세션 2
- [x] A-2e. `walkJumpVerticalFactor = 1F` (원본 L243) — 세션 2
- [x] A-2f. `runJumpHorizontalFactor = 2F` ★ (원본 L246, 오버라이드) — 세션 2
- [x] A-2g. `runJumpVerticalFactor = 1F` (원본 L247) — 세션 2
- [x] A-2h. `sprintJumpHorizontalFactor = 2F` ★ (원본 L250, 오버라이드) — 세션 2
- [x] A-2i. `sprintJumpVerticalFactor = 1F` (원본 L251) — 세션 2

**A-3. Jump 활성 Boolean 5건**
- [x] A-3a. `standJump = true` (원본 L234) — 세션 3
- [x] A-3b. `sneakJump = true` (원본 L237) — 세션 3
- [x] A-3c. `walkJump = true` (원본 L241) — 세션 3
- [x] A-3d. `runJump = true` (원본 L245) — 세션 3
- [x] A-3e. `sprintJump = true` (원본 L249) — 세션 3

**A-4. ChargeUp 필드 4건**
- [x] A-4a. `jumpCharge = true` (원본 L254) — 세션 4 (이미 이식됨 확인 + 주석 정비)
- [x] A-4b. `jumpChargeMaximum = 20F` (원본 L255) — 세션 4 (이미 이식됨 확인 + 주석 정비)
- [x] A-4c. `jumpChargeFactor = 1.3F` (원본 L256) — 세션 4 (이미 이식됨 확인 + 주석 정비)
- [x] A-4d. `jumpChargeCancelOnSneakRelease = false` (원본 L257, Modified=false) — 세션 4 (신규 이식, 문서 오기 'true' 정정)

**A-5. HeadUp 필드 5건**
- [x] A-5a. `headJump = true` (원본 L260) — 세션 5 (이미 이식 + IO 확인)
- [x] A-5b. `headJumpControlFactor = 0.2F` (원본 L261) — 세션 5 (이미 이식 + IO 확인, legacy key 유지)
- [x] A-5c. `headJumpChargeMaximum = 10F` (원본 L262) — 세션 5 (이미 이식 + IO 확인, legacy key 유지)
- [x] A-5d. `headFallDamageStartDistance = 2F` (원본 L264) — 세션 5 (필드 이미 이식 + IO 신규 + 주석 라인 정정)
- [x] A-5e. `headFallDamageFactor = 2F` ★ (원본 L265, IncreasingFactor.defaults(2F) 오버라이드) — 세션 5 (필드 이미 이식 + IO 신규 + 주석 라인 정정)

**A-6. Angle 필드 4건**
- [x] A-6a. `angleJumpSide = true` (원본 L268) — 세션 6 (이미 이식 + IO 확인)
- [x] A-6b. `angleJumpBack = true` (원본 L269) — 세션 6 (이미 이식 + IO 확인)
- [x] A-6c. `angleJumpHorizontalFactor = 0.4F` ★ (원본 L270, sm_1_3 오버라이드) — 세션 6 (**값 오역 정정** 0.3F → 0.4F + 주석 정비)
- [x] A-6d. `angleJumpVerticalFactor = 0.2F` ★ (원본 L271) — 세션 6 (이미 이식 + IO 확인 + 주석 정비)

**A-7. ClimbUp 필드 3건**
- [ ] A-7a. `climbUpJump = true` (원본 L274)
- [ ] A-7b. `climbUpJumpVerticalFactor = 1F` (원본 L275)
- [ ] A-7c. `climbUpJumpHandsOnlyVerticalFactor = 0.8F` ★ (원본 L276)

**A-8. ClimbBackUp 필드 5건**
- [ ] A-8a. `climbBackUpJump = true` (원본 L279)
- [ ] A-8b. `climbBackUpJumpVerticalFactor = 0.2F` ★ (원본 L280)
- [ ] A-8c. `climbBackUpJumpHorizontalFactor = 0.3F` ★ (원본 L281)
- [ ] A-8d. `climbBackUpJumpHandsOnlyVerticalFactor = 0.8F` ★ (원본 L282)
- [ ] A-8e. `climbBackUpJumpHandsOnlyHorizontalFactor = 1F` (원본 L283)

**A-9. ClimbBackHead 필드 5건**
- [ ] A-9a. `climbBackHeadJump = true` (원본 L286)
- [ ] A-9b. `climbBackHeadJumpVerticalFactor = 0.2F` ★ (원본 L287)
- [ ] A-9c. `climbBackHeadJumpHorizontalFactor = 0.3F` ★ (원본 L288)
- [ ] A-9d. `climbBackHeadJumpHandsOnlyVerticalFactor = 0.8F` ★ (원본 L289)
- [ ] A-9e. `climbBackHeadJumpHandsOnlyHorizontalFactor = 1F` (원본 L290)

**A-10. WallUp 필드 5건** (일부 이식)
- [ ] A-10a. `wallUpJump = true` (원본 L293)
- [ ] A-10b. `wallUpJumpVerticalFactor` ✅ 이미 이식됨 — 확인만
- [ ] A-10c. `wallUpJumpHorizontalFactor = 0.15F` ★ (원본 L295) — **미이식** 추가
- [ ] A-10d. `wallUpJumpFallMaximumDistance` ✅ 이미 이식됨 — 확인만
- [ ] A-10e. `wallUpJumpOrthogonalTolerance` ✅ 이미 이식됨 — 확인만

**A-11. WallHead 필드 4건** (일부 이식)
- [ ] A-11a. `wallHeadJump = true` (원본 L300)
- [ ] A-11b. `wallHeadJumpVerticalFactor` ✅ 이미 이식됨 — 확인만
- [ ] A-11c. `wallHeadJumpHorizontalFactor = 0.15F` ★ (원본 L302) — **미이식** 추가
- [ ] A-11d. `wallHeadJumpFallMaximumDistance` ✅ 이미 이식됨 — 확인만

**A-12. Properties IO 등록** (추가된 모든 필드 `load()` + `save()` 양방향)
- [ ] A-12. 모든 신규 필드를 `SmartMovingConfig.load(Properties p)` + `save(Properties p)` 에
     등록. 원본 `Unmodified/Modified/Positive/PositiveFactor/IncreasingFactor/DecreasingFactor`
     정확한 key 복사 (`move.jump.wall.horizontal.factor` 등).

---

### Phase B. 판정 헬퍼 메서드 전수 이식 (SmartMovingConfig.java)

**B-1. `getJumpSpeed` 헬퍼 (SmartMovingSelf L2148-L2163)**
- [ ] B-1. 신설 — isStanding/isSneaking/isRunning/isSprinting/angle 기반 speed 반환:
  ```java
  public static int getJumpSpeed(boolean isStanding, boolean isSneaking,
                                 boolean isRunning, boolean isSprinting, Float angle) {
      isSprinting &= angle == null;
      isRunning   &= angle == null;
      if (isSprinting) return SPRINTING;
      else if (isRunning) return RUNNING;
      else if (isSneaking) return SNEAKING;
      else if (isStanding) return STANDING;
      else return WALKING;
  }
  ```

**B-2. Speed 상수** (원본 L172-L176)
- [ ] B-2. `SmartMovingConfig` 또는 `SmartMovingJumper` 에 `SPEED_SPRINTING=0, SPEED_RUNNING=1,
     SPEED_WALKING=2, SPEED_SNEAKING=3, SPEED_STANDING=4` 상수 5개 신설.

**B-3. `isJumpingEnabled(speed, type)`** (원본 L194-L227)
- [ ] B-3. SmartMovingConfig 에 메서드 신설 — 11 type 분기 전수 + speed 5 분기:
  - ChargeUp → `jumpCharge`
  - SlideDown → `slide`
  - ClimbUp/ClimbUpHandsOnly → `climbUpJump`
  - ClimbBackUp/ClimbBackUpHandsOnly → `climbBackUpJump`
  - ClimbBackHead/ClimbBackHeadHandsOnly → `climbBackHeadJump`
  - WallUp → `wallUpJump`
  - WallHead → `wallHeadJump`
  - speed=Sprinting → `sprintJump`
  - speed=Running → `runJump`
  - speed=Walking → `walkJump`
  - speed=Sneaking → `sneakJump`
  - speed=Standing → `standJump`

**B-4. `getJumpHorizontalFactor(speed, type)`** (원본 L465-L505)
- [ ] B-4. SmartMovingConfig 에 메서드 신설 — `!enabled` 시 `speed==Running ? 2F : 1F`.
  `enabled` 시 base `_jumpHorizontalFactor` × type 분기 (Angle/ClimbBack/WallUp/WallHead)
  × speed 분기 (Sprint/Run/Walk/Sneak/Stand). L501 의 `speed==Standing && type!=ClimbBack*`
  → `* 0F` 특수 처리 포함.

**B-5. `getJumpVerticalFactor(speed, type)`** (원본 L418-L463)
- [ ] B-5. SmartMovingConfig 에 메서드 신설 — base `_jumpVerticalFactor` × type 분기
  (Angle/ClimbUp/ClimbUpHandsOnly/ClimbBackUp/ClimbBackUpHandsOnly/ClimbBackHead/
  ClimbBackHeadHandsOnly/WallUp/WallHead) × speed 분기 (Sprint/Run/Walk/Sneak/Stand).

**B-6. `getMaxHorizontalMotion(speed, type, inWater)`** (원본 L508-L525)
- [ ] B-6. SmartMovingConfig 에 메서드 신설 — baseMaxMotion = `0.117852041920949F`
  (inWater 시 `0.07839602977037292F`) × speed 분기 (Sprint→sprintFactor / Run→runFactor /
  Sneak→sneakFactor). `!enabled` 시 speed==Running → baseMaxMotion × 1.3F.

**B-7. `getJumpChargeFactor(charge)`** (원본 L401-L408)
- [ ] B-7. SmartMovingConfig 에 이식 — 기존 Jumper L180-L182 계산 로직과 동일. Jumper 에서
  이 헬퍼 호출로 교체.

**B-8. `getHeadJumpFactor(charge)`** (원본 L410-L416)
- [ ] B-8. SmartMovingConfig 에 이식 — `(charge - 1) / (max - 1)` 공식. Jumper 에서 호출.

---

### Phase C. Exhaustion 시스템 (대규모)

**C-1. Exhaustion Config 필드 전수**
- [ ] C-1a. Jump exhaustion 활성/종료 필드 (`_jumpExhaustion` + speed 5 + type 11)
- [ ] C-1b. Jump exhaustion gain factor 필드 (원본 _jumpExhaustionGainFactor + 하위 10+)
- [ ] C-1c. Jump exhaustion stop factor 필드 (원본 _jumpExhaustionStopFactor + 하위 10+)
- [ ] C-1d. ChargeUp exhaustion 특수 (`_jumpChargeExhaustion`,
     `_jumpChargeExhaustionGainFactor`, `_jumpChargeExhaustionStopFactor`)
- [ ] C-1e. 그 외: base exhaustion loss (sprint/run/walk/sneak/stand/fall 등)

**C-2. Exhaustion 상태 필드 (ClientState)**
- [ ] C-2a. `exhaustion` float — 현 상태 값
- [ ] C-2b. `maxExhaustionToStartAction` float — 틱 시작 시 Math.POSITIVE_INFINITY 로 리셋
- [ ] C-2c. `maxExhaustionForAction` float — 동일 리셋

**C-3. 판정 메서드 3개 (SmartMovingConfig)**
- [ ] C-3a. `isJumpExhaustionEnabled(speed, type)` (원본 L244-L289)
- [ ] C-3b. `getJumpExhaustionGain(speed, type, charge)` (원본 L291-L346)
- [ ] C-3c. `getJumpExhaustionStop(speed, type, charge)` (원본 L348-L399)

**C-4. `getMaxExhaustion()`** (원본 L527-L547)
- [ ] C-4. SmartMovingConfig 에 메서드 신설 — jump/climb/ceilingClimb/run/sprint exhaustion
     최대치 계산.

**C-5. `getFactor(hunger, onGround, ...)`** (원본 L554-L596)
- [ ] C-5. SmartMovingConfig 에 메서드 신설 — 이식 전에 기존 `getCombinedSpeedFactor` 와
     중복 검증. 포커스 #6 완료 후 통합 가능성 검토.

**C-6. Exhaustion 틱 갱신 (MixinLivingEntityClient or ClientState.tickEssential)**
- [ ] C-6. 매 틱 진입 시 `maxExhaustionToStartAction = Float.POSITIVE_INFINITY;
     maxExhaustionForAction = Float.POSITIVE_INFINITY;` 리셋 (원본 L2025-L2026 이후 호출 시).

---

### Phase D. tryJump 전면 재작성 (SmartMovingJumper.java)

**기존 `tryJump(player, sm, jumpType, charge)` 는 경량 이식** — 전면 재작성하여 원본 L1999-L2136 1:1.

**D-1. 시그니처 정비**
- [ ] D-1. 원본 시그니처 `boolean tryJump(int type, Boolean inWaterOrNull, Boolean isRunningOrNull,
     Float angle)` 1:1 이식. 반환: `enabled` 여부 (원본 L2135). 내부에서 `jumpCharge` /
     `headJumpCharge` 등 필드 접근.

**D-2. WallUpSlide/WallHeadSlide 변환**
- [ ] D-2. 원본 L2002-L2006:
  ```java
  boolean noVertical = false;
  if (type == WallUpSlide || type == WallHeadSlide) {
      type = (type == WallUpSlide) ? WallUp : WallHead;
      noVertical = true;
  }
  ```

**D-3. 지역 변수 계산**
- [ ] D-3. 원본 L2008-L2012:
  ```java
  boolean inWater = inWaterOrNull != null ? inWaterOrNull : sm.isDipping;
  boolean isRunning = isRunningOrNull != null ? isRunningOrNull : sm.isRunning(player);
  boolean charged = type == ChargeUp;
  boolean up = type == Up || type == ChargeUp || type == HeadUp
            || type == ClimbUp || type == ClimbUpHandsOnly
            || type == ClimbBackUp || type == ClimbBackUpHandsOnly
            || type == ClimbBackHead || type == ClimbBackHeadHandsOnly
            || type == Angle || type == WallUp || type == WallHead;
  boolean head = type == HeadUp || type == ClimbBackHead
              || type == ClimbBackHeadHandsOnly || type == WallHead;
  ```

**D-4. getJumpSpeed 호출 + isJumpingEnabled**
- [ ] D-4. 원본 L2014-L2015:
  ```java
  int speed = getJumpSpeed(sm.isStanding, sm.isSlow, isRunning, sm.isFast, angle);
  boolean enabled = cfg.isJumpingEnabled(speed, type);
  ```

**D-5. Exhaustion 체크 + maxExhaustion 조정** (Phase C 의존)
- [ ] D-5. 원본 L2018-L2027:
  ```java
  if (enabled) {
      boolean exhausionEnabled = cfg.isJumpExhaustionEnabled(speed, type);
      if (exhausionEnabled) {
          float maxExhausionForJump = cfg.getJumpExhaustionStop(speed, type, sm.jumpCharge);
          if (sm.exhaustion > maxExhausionForJump) return false;
          sm.maxExhaustionToStartAction = Math.min(sm.maxExhaustionToStartAction, maxExhausionForJump);
          sm.maxExhaustionForAction = Math.min(sm.maxExhaustionForAction,
              maxExhausionForJump + cfg.getJumpExhaustionGain(speed, type, sm.jumpCharge));
      }
      ...
  }
  ```

**D-6. jumpFactor (potion) + horizontal/vertical factor + jumpChargeFactor**
- [ ] D-6. 원본 L2029-L2032:
  ```java
  float jumpFactor = 1F;
  StatusEffectInstance jumpBoost = player.getStatusEffect(StatusEffects.JUMP_BOOST);
  if (jumpBoost != null)
      jumpFactor = 1F + (jumpBoost.getAmplifier() + 1) * 0.2F;
  float horizontalJumpFactor = cfg.getJumpHorizontalFactor(speed, type) * jumpFactor;
  float verticalJumpFactor   = cfg.getJumpVerticalFactor(speed, type) * jumpFactor;
  float jumpChargeFactor = charged ? cfg.getJumpChargeFactor(sm.jumpCharge) : 1F;
  ```

**D-7. !up 변환 (horizontalJumpFactor = sqrt)**
- [ ] D-7. 원본 L2034-L2038:
  ```java
  if (!up) {
      horizontalJumpFactor = (float) Math.sqrt(
          horizontalJumpFactor * horizontalJumpFactor
          + verticalJumpFactor * verticalJumpFactor);
      verticalJumpFactor = 0;
  }
  ```

**D-8. maxHorizontalMotion + verticalMotion 초기 계산**
- [ ] D-8. 원본 L2040-L2045:
  ```java
  Double maxHorizontalMotion = null;
  double horizontalMotion = Math.sqrt(sm.jumpMotionX * sm.jumpMotionX
                                    + sm.jumpMotionZ * sm.jumpMotionZ);
  double verticalMotion = -0.078 + 0.498 * verticalJumpFactor * jumpChargeFactor;
  if (horizontalJumpFactor > 1F && !player.horizontalCollision) {
      maxHorizontalMotion = (double) cfg.getMaxHorizontalMotion(speed, type, inWater)
                          * SmartMovingMover.getCombinedSpeedFactor(player, cfg);
  }
  ```

**D-9. Up && vanilla 분기**
- [ ] D-9. 원본 L2047-L2062:
  ```java
  if (type == Up && sm.vanilla()) {
      verticalMotion = 0.41999998688697815D;
      if (jumpBoost != null) verticalMotion += (jumpBoost.getAmplifier() + 1) * 0.1F;
      if (player.isSprinting()) {
          float f = player.getYaw() * 0.017453292F;
          motionX -= MathHelper.sin(f) * 0.2F;
          motionZ += MathHelper.cos(f) * 0.2F;
      }
  }
  ```

**D-10. else: head 재계산**
- [ ] D-10. 원본 L2065-L2079:
  ```java
  if (head) {
      double normalAngle = Math.atan(verticalMotion / horizontalMotion);
      double totalMotion = Math.sqrt(verticalMotion * verticalMotion
                                   + horizontalMotion * horizontalMotion);
      double newAngle = cfg.getHeadJumpFactor(sm.headJumpCharge) * normalAngle;
      double newVerticalMotion = totalMotion * Math.sin(newAngle);
      double newHorizontalMotion = totalMotion * Math.cos(newAngle);
      if (maxHorizontalMotion != null)
          maxHorizontalMotion = maxHorizontalMotion * (newHorizontalMotion / horizontalMotion);
      verticalMotion = newVerticalMotion;
      horizontalMotion = newHorizontalMotion;
  }
  ```

**D-11. angle != null 분기**
- [ ] D-11. 원본 L2081-L2095:
  ```java
  if (angle != null) {
      float jumpAngleRad = angle / RadiantToAngle;  // RadiantToAngle = 180/PI
      boolean reset = type == WallUp || type == WallHead;
      double horizontal = Math.max(horizontalMotion, horizontalJumpFactor);
      double moveX = -Math.sin(jumpAngleRad);
      double moveZ = Math.cos(jumpAngleRad);
      motionX = getJumpMoving(sm.jumpMotionX, moveX, reset, horizontal, horizontalJumpFactor);
      motionZ = getJumpMoving(sm.jumpMotionZ, moveZ, reset, horizontal, horizontalJumpFactor);
      horizontalMotion = 0;
      verticalMotion = verticalJumpFactor;
  }
  ```

**D-12. horizontalMotion > 0 스케일**
- [ ] D-12. 원본 L2097-L2110:
  ```java
  if (horizontalMotion > 0) {
      double absMotionX = Math.abs(motionX) * horizontalJumpFactor;
      double absMotionZ = Math.abs(motionZ) * horizontalJumpFactor;
      if (maxHorizontalMotion != null) {
          absMotionX = Math.min(absMotionX, maxHorizontalMotion
                        * (horizontalJumpFactor * (Math.abs(motionX) / horizontalMotion)));
          absMotionZ = Math.min(absMotionZ, maxHorizontalMotion
                        * (horizontalJumpFactor * (Math.abs(motionZ) / horizontalMotion)));
      }
      motionX = Math.signum(motionX) * absMotionX;
      motionZ = Math.signum(motionZ) * absMotionZ;
  }
  ```

**D-13. up && !noVertical → motionY 적용 + Stats + isSprintJump**
- [ ] D-13. 원본 L2113-L2118:
  ```java
  if (up && !noVertical) {
      motionY = verticalMotion;
      player.incrementStat(Stats.JUMP);
      sm.isSprintJump = sm.isFast;
  }
  ```

**D-14. exhaustion gain 적용**
- [ ] D-14. 원본 L2120-L2124:
  ```java
  if (exhausionEnabled) {
      float gain = cfg.getJumpExhaustionGain(speed, type, sm.jumpCharge);
      sm.exhaustion += gain;
  }
  ```

**D-15. head → isHeadJumping + setHeightOffset**
- [ ] D-15. 원본 L2126-L2130:
  ```java
  if (head) {
      sm.isHeadJumping = true;
      setPoseSmall(player);
      sm.heightOffset = -1F;
  }
  ```

**D-16. 최종 setVelocity + isJumping + onLivingJump**
- [ ] D-16. 원본 L2131-L2134:
  ```java
  player.setVelocity(motionX, noVertical ? vel.y : motionY, motionZ);
  // sp.isAirBorne = true — vanilla 자동
  sm.isJumping = true;
  // onLivingJump() — PlayerEntity.jump() 호출 또는 vanilla 점프 이벤트
  ```

**D-17. 반환**
- [ ] D-17. `return enabled;`

**D-18. 상태 클리어 (원본 tryJump 외부 호출 후 처리)**
- [ ] D-18. 원본에는 tryJump 종료 후 호출측에서 `jumpCharge=0; headJumpCharge=0;
     blockJumpTillButtonRelease=true; jumpPending=false;` 리셋. 현 1.21.1 은 tryJump 내부에서
     이미 처리. 원본 순서 확인 후 정리.

---

### Phase E. 호출 경로 통합

**E-1. trySlideDownJump 통합 또는 유지**
- [ ] E-1. Phase D 의 `tryJump(SLIDE_DOWN, false, wasRunning, null)` 호출이 B-42-B26 경량 해소를
     대체. 기존 `trySlideDownJump` 별도 메서드는 **삭제** 후 `tryJump` 단일 진입 정리.

**E-2. 기존 호출처 재검토**
- [ ] E-2. 현재 `Jumper.tryJump(player, sm, jumpType, charge)` 호출 전수 grep. 새 시그니처
     `tryJump(player, sm, type, inWater, isRunning, angle)` 에 맞춰 재호출:
  - handleJumping 내부 UP / CHARGE_UP / HEAD_UP 경로
  - WALL_UP / WALL_HEAD / WALL_UP_SLIDE / WALL_HEAD_SLIDE (handleWallJumping)
  - LEFT/RIGHT/BACK → Angle type + angle 파라미터 변경
  - CLIMB_UP / CLIMB_BACK / CLIMB_BACK_HEAD → 해당 type + HandsOnly 분기 추가

**E-3. HandsOnly 분기 추가**
- [ ] E-3. `feetClimbing.isNone() && handsClimbing.isUp()` 상태에서 `ClimbUpHandsOnly` /
     `ClimbBackUpHandsOnly` / `ClimbBackHeadHandsOnly` type 으로 분기. Climber / Jumper
     경로 확인.

**E-4. Angle 점프 통합**
- [ ] E-4. LEFT=7, RIGHT=8, BACK=9 → Angle type + angle 파라미터 (+90/-90/+180 등).
     handleWallJumping / Jumper.tryAngleJump 호출 경로 재정리.

**E-5. Exhaustion 매 틱 리셋**
- [ ] E-5. `MixinLivingEntityClient.sm_beforeTravel` 또는 `ClientState.tickEssential` 진입 시
     `sm.maxExhaustionToStartAction = Float.POSITIVE_INFINITY; sm.maxExhaustionForAction =
     Float.POSITIVE_INFINITY;`.

---

### Phase F. 감사 + 플레이테스트

**F-1. 원본 tryJump L1999-L2136 side-by-side 비교**
- [ ] F-1. 1.21.1 `SmartMovingJumper.tryJump` 와 원본 L1999-L2136 각 줄 대조 감사 — 누락 라인
     / 조건 / 상수 / 분기 / motionY 적용 순서 / isAirBorne 시멘틱 전수 확인.

**F-2. 원본 getJumpHorizontalFactor L465-L505 side-by-side 비교**
- [ ] F-2. `SmartMovingConfig.getJumpHorizontalFactor` 와 원본 대조. 특히 L501 `speed==Standing
     && type!=ClimbBack*` → `*0F` 엣지 케이스 확인.

**F-3. 원본 getJumpVerticalFactor L418-L463 비교**
- [ ] F-3. 동일 감사.

**F-4. Exhaustion 3-메서드 감사**
- [ ] F-4. `isJumpExhaustionEnabled` / `getJumpExhaustionGain` / `getJumpExhaustionStop`
     원본 L244-L399 전수 대조.

**F-5. 빌드 + 회귀 감사**
- [ ] F-5. `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL. 기존 테스트
     (있다면) 통과. 포커스 #2 Extended 회귀 없음 확인.

**F-6. 사용자 인게임 플레이테스트**
- [ ] F-6. 점프 모든 타입 (일반/스프린트/헤드/벽점프/벽점프슬라이드/사이드/백/슬라이드다운/
     클라이밍점프/클라이밍백점프) × Config 수정 시 배율 반영 확인.

---

## 4. 의존 순서

```
Phase A (Config 필드) — 40+ 필드
   ↓
Phase B (판정 헬퍼) — isJumpingEnabled / getJumpH/VFactor / getMaxHorizontalMotion /
                     getJumpChargeFactor / getHeadJumpFactor / getJumpSpeed
   ↓
Phase C (Exhaustion 시스템) — Config 필드 + 상태 필드 + 3 판정 메서드 + 매 틱 리셋
   ↓
Phase D (tryJump 재작성) — 원본 L1999-L2136 전면 1:1
   ↓
Phase E (호출 경로 통합) — 기존 경량 메서드 대체 + HandsOnly + Angle 분기
   ↓
Phase F (감사 + 플레이테스트) — side-by-side 대조 + 빌드 + 인게임 검증
```

**전체 규모**: Phase A 40+ 필드 + Phase B 8 메서드 + Phase C 30+ 필드 + 3 메서드 + 상태 필드
+ 리셋 + Phase D 18 서브 + Phase E 5 원자 + Phase F 6 감사. **약 110+ 원자 / 예상 10-20 세션**.

**권장 진행**: Phase A/B 먼저 끝내서 기존 경량 `tryJump` 에 factor 인프라 주입 (DB 전수
이식 전 factor 반영). Phase C 는 독립 대규모 — 별도 세션 집중. Phase D/E/F 는 연결/검증.

---

## 5. 1:1 번역 체크리스트 (원자별 적용)

각 원자 이식 시 아래 10항 검증:
- [근거] 원본 라인 확보 (로컬 `C:\Work\minecraft\porting\sm_original\SmartMoving\`)
- [근거] 1.21.1 이식 위치 확정
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 대조
- [분기] 모든 if/else/ternary 전수 이식
- [상수] 모든 factor 기본값 + 오버라이드 정확 반영
- [타이밍] 계산 순서 (원본 L1999-L2136 절대 순서) 보존
- [근사] 근사 불가피 시 §7 등록 (§7 은 이 포커스 별도 §7 로 관리 — focus_02_5 §7)
- [신규] 발견한 추가 의존 필드/메서드는 본 문서 §3 에 원자 추가
- [회귀] 기존 Extended 이식 (B-26 경량 / WallUp / WallHead / ChargeUp / HeadUp 경량) 영향 확인
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` 성공

---

## 6. 작업 기록 (세션별)

### 세션 0 — 2026-04-25 — 포커스 #2.5 신설

- 포커스 #2 Extended 완결 후 세션 135 에서 잔존 근사 감사 수행.
- 실질 "1:1 미이식" 은 B-42-B26 (Jumper factor 인프라) 과 B-42c (2) (lava border) 두 가지.
- B-42-B26 은 Jumper factor 인프라 약 40+ Config 필드 + 6+ 판정 메서드 + Exhaustion 시스템 +
  tryJump 전면 재작성 규모 → **별도 포커스 2.5 로 분리**.
- 원본 `SmartMovingClientConfig.java` L172-L547 + `SmartMovingSelf.java` L1999-L2136 전수 감사
  후 Phase A/B/C/D/E/F 약 110+ 원자 구조 확정.
- `playtest_fixes.md` 현재 포커스 → `#2.5` 전환.

**다음 세션 권고**: Phase A-1 (jumpHorizontalFactor / jumpVerticalFactor / jumpControlFactor
3건 base factor 추가).

### 세션 1 — 2026-04-25 — Phase A-1 (Jump base factor 3건)

사용자 지시: "엄격 1:1" 유지 + Phase A-1 3건 이식.

진행한 작업:
1. 원본 라인 + 기본값 확보:
   - `SmartMovingConfig.java` L228 `_jumpControlFactor = DecreasingFactor("move.jump.control.factor").defaults(1F)` (>= 0, <= 1)
   - `SmartMovingConfig.java` L230 `_jumpHorizontalFactor = IncreasingFactor("move.jump.horizontal.factor")` — defaults 미지정 → IncreasingFactor 기본 1F (`Properties.java` L189-L190)
   - `SmartMovingConfig.java` L231 `_jumpVerticalFactor = PositiveFactor("move.jump.vertical.factor")` — defaults 미지정 → PositiveFactor 기본 1F (`Properties.java` L185-L186)
2. 1.21.1 이식 (`src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java`):
   - 필드 선언 (L163-L172, ── Jumping ── 주석 직하단, wallUpJump 위)
   - `load()` Properties IO 등록 (L949-L951)
   - `save()` Properties IO 등록 (L1078-L1080)
   - key 명: `move.jump.control.factor` / `move.jump.horizontal.factor` / `move.jump.vertical.factor`
3. 근사 여부: 없음. 1:1.

완료 전 검증 체크리스트 (세션 1 기준):
- [근거] 원본 라인 확보 — 로컬 `C:\Work\minecraft\porting\sm_original\SmartMoving\` L228/L230/L231 + Properties.java L185-L192
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingConfig.java` L163-L172 / L949-L951 / L1078-L1080
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (필드명 / 기본값 / key 명 일치)
- [분기] 분기 없음 (단순 필드 3개)
- [상수] 1F × 3 (DecreasingFactor / IncreasingFactor / PositiveFactor 모두 기본 1F)
- [타이밍] 필드 선언만 — 호출 타이밍 없음. 실제 사용은 Phase B/D 에서 발생.
- [근사] 근사 없음. §7 등록 없음.
- [신규] 추가 의존 없음. §3 변경 없음.
- [회귀] 신규 필드만 추가 — 기존 코드 영향 없음. wallUpJump 등 기존 jumping 필드 무영향.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (17s)

다음 세션 권고: Phase A-2 (Speed 별 Horizontal/Vertical factor 9건 — A-2a~A-2i, 기존 A-2 헤딩 "10건"은 9건 오기).

진행률: Phase A 3/40+ (~7.5%), 전체 #2.5 3/~110 (~2.7%).

### 세션 2 — 2026-04-25 — Phase A-2 (Speed 별 Jump factor 9건)

사용자 지시: "엄격 1:1" 유지 + Phase A-2 9건 (A-2a~A-2i) 이식.

진행한 작업:
1. 원본 라인 + 기본값 확보 (`SmartMovingConfig.java` L235-L251):
   - L235 `_standJumpVerticalFactor = PositiveFactor(...)` → 기본 1F
   - L238 `_sneakJumpHorizontalFactor = IncreasingFactor(...)` → 기본 1F
   - L239 `_sneakJumpVerticalFactor = PositiveFactor(...)` → 기본 1F
   - L242 `_walkJumpHorizontalFactor = IncreasingFactor(...)` → 기본 1F
   - L243 `_walkJumpVerticalFactor = PositiveFactor(...)` → 기본 1F
   - L246 `_runJumpHorizontalFactor = IncreasingFactor(...).defaults(2F)` ★ → 기본 2F (오버라이드)
   - L247 `_runJumpVerticalFactor = PositiveFactor(...)` → 기본 1F
   - L250 `_sprintJumpHorizontalFactor = IncreasingFactor(...).defaults(2F)` ★ → 기본 2F (오버라이드)
   - L251 `_sprintJumpVerticalFactor = PositiveFactor(...)` → 기본 1F
2. 1.21.1 이식 (`src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java`):
   - 필드 선언 9건 (A-1 직후, wallUpJump 위)
   - `load()` Properties IO 9건 (A-1 jumpVerticalFactor 다음)
   - `save()` Properties IO 9건 (동일 위치)
   - key 명: `move.jump.{stand|sneak|walk|run|sprint}.{horizontal|vertical}.factor` (legacy `move.{speed}.jump.{...}.factor` 는 _pre_sm_1_7 버전이라 채택 X)
3. 근사 여부: 없음. 1:1.

완료 전 검증 체크리스트 (세션 2 기준):
- [근거] 원본 라인 확보 — 로컬 `SmartMovingConfig.java` L235/L238/L239/L242/L243/L246/L247/L250/L251 + Properties.java L185-L192
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingConfig.java` 필드 L174-L209 / `load()` L962-L970 / `save()` L1091-L1099
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (필드명 / 기본값 / key 명 일치)
- [분기] 분기 없음 (단순 필드 9개)
- [상수] 1F × 7 + 2F × 2 (run/sprint H 오버라이드 ★ 정확 반영)
- [타이밍] 필드 선언만 — 호출 타이밍 없음. 실제 사용은 Phase B/D 에서 발생.
- [근사] 근사 없음. §7 등록 없음.
- [신규] 추가 의존 없음. §3 변경 없음.
- [회귀] 신규 필드만 추가 — 기존 코드 영향 없음. A-1 base factor 와 wallUp/wallHead/jumpCharge 등 기존 jumping 필드 무영향.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (5s)

다음 세션 권고: Phase A-3 (Jump 활성 Boolean 5건 — standJump/sneakJump/walkJump/runJump/sprintJump 모두 true). 원본 L234/L237/L241/L245/L249 Unmodified 기본 true.

진행률: Phase A 12/40+ (~30%), 전체 #2.5 12/~110 (~10.9%).

### 세션 3 — 2026-04-25 — Phase A-3 (Jump 활성 Boolean 5건)

사용자 지시: "엄격 1:1" 유지 + Phase A-3 5건 (A-3a~A-3e) 이식.

진행한 작업:
1. 원본 라인 + 기본값 확보 (`SmartMovingConfig.java` L234-L249):
   - L234 `_standJump = Unmodified("move.jump.stand")` → 기본 true
   - L237 `_sneakJump = Unmodified("move.jump.sneak")` → 기본 true
   - L241 `_walkJump = Unmodified("move.jump.walk")` → 기본 true
   - L245 `_runJump = Unmodified("move.jump.run")` → 기본 true
   - L249 `_sprintJump = Unmodified("move.jump.sprint")` → 기본 true
   - Properties.java L171-L172 → Unmodified 기본 true
2. 1.21.1 이식 (`src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java`):
   - 필드 선언 5건 — 각 speed 그룹 첫 줄 (V/H factor 위) 에 추가하여 원본 L234/L237/L241/L245/L249 순서 보존
   - `load()` Properties IO 5건 — 동일 순서 (각 speed 그룹 첫 줄)
   - `save()` Properties IO 5건 — 동일 순서
   - key 명: `move.jump.{stand|sneak|walk|run|sprint}` (legacy `move.{speed}.jump` 는 _pre_sm_1_7 — 채택 X)
3. 근사 여부: 없음. 1:1.

완료 전 검증 체크리스트 (세션 3 기준):
- [근거] 원본 라인 확보 — 로컬 `SmartMovingConfig.java` L234/L237/L241/L245/L249 + Properties.java L171-L172
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingConfig.java` 필드 L177/L184/L192/L200/L208 / `load()` L996/L998/L1001/L1004/L1007 (정확한 라인은 grep 가능, 묶음 추가) / `save()` 동일 그룹
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (필드명 / 기본값 / key 명 일치)
- [분기] 분기 없음 (단순 Boolean 5개)
- [상수] true × 5 (Unmodified 기본 true 정확 반영)
- [타이밍] 필드 선언만 — 호출 타이밍 없음. 실제 사용은 Phase B (`isJumpingEnabled`) 에서 발생.
- [근사] 근사 없음. §7 등록 없음.
- [신규] 추가 의존 없음. §3 변경 없음.
- [회귀] 신규 필드만 추가 — 기존 코드 영향 없음. A-1/A-2 + 기존 wallUp/wallHead/jumpCharge 무영향.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (7s)

다음 세션 권고: Phase A-4 (ChargeUp 필드 4건 — A-4a `jumpCharge=true` + A-4d `jumpChargeCancelOnSneakRelease=true` 이식, A-4b/c 는 ✅ 이미 이식됨 확인만). 원본 L254/L257.

진행률: Phase A 17/40+ (~42.5%), 전체 #2.5 17/~110 (~15.5%).

### 세션 4 — 2026-04-25 — Phase A-4 (ChargeUp 4건: 신규 1 + 확인 3 + 문서 오기 정정)

사용자 지시: "엄격 1:1" 유지 + Phase A-4 4건 (A-4a~A-4d) 이식.

진행한 작업:
1. 원본 라인 + 기본값 확보 (`SmartMovingConfig.java` L254-L257):
   - L254 `_jumpCharge = Unmodified("move.jump.charge")` → 기본 true
   - L255 `_jumpChargeMaximum = Positive(...).defaults(20F)` → 20F
   - L256 `_jumpChargeFactor = IncreasingFactor(...).defaults(1.3F)` → 1.3F
   - L257 `_jumpChargeCancelOnSneakRelease = Modified("move.jump.charge.sneak.release.cancel")` → **false** (Properties.java L173 `Modified=false`, 기존 §0/§3 의 "true" 는 오기)
2. 1.21.1 이식 위치 확인:
   - A-4a/b/c (L239-L241 + load L1030-L1032 + save L1173-L1175) — 모두 이미 이식됨. 값 일치 (true / 20F / 1.3F).
   - A-4d (신규 — L242 위치에 필드, load/save 양방향 추가)
3. 1.21.1 이식 (`src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java`):
   - 필드 선언 — A-4 ChargeUp 그룹 헤더 주석 추가 + A-4a/b/c 원본 라인 주석 정비 + A-4d 신규 추가
   - `load()` IO — A-4d 1건 추가 (key: `move.jump.charge.sneak.release.cancel`)
   - `save()` IO — A-4d 1건 추가
4. 문서 정정: §0 미이식 표 + §3 A-4d "true (Modified)" → "false (Modified=false)" + 근거 명시.
5. 근사 여부: 없음. 1:1.

완료 전 검증 체크리스트 (세션 4 기준):
- [근거] 원본 라인 확보 — 로컬 `SmartMovingConfig.java` L254-L257 + Properties.java L173 (Modified=false 결정적 근거)
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingConfig.java` 필드 L239-L249 / `load()` L1030-L1034 / `save()` L1173-L1177
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (필드명 / 기본값 / key 명 일치, A-4a/b/c 기존 이식 값 검증)
- [분기] 분기 없음 (단순 boolean/float 4개)
- [상수] true / 20F / 1.3F / false (Modified=false 정확 반영, 문서 오기 정정)
- [타이밍] 필드 선언만 — 호출 타이밍 없음.
- [근사] 근사 없음. §7 등록 없음.
- [신규] 발견: 문서 §0/§3 의 "Modified=true" 오기 → false 정정. (Properties.java 의 Modified() 기본값 결정적 근거)
- [회귀] 신규 필드 1건만 추가 + 기존 3건 주석 정비 — 동작 변경 0. 기존 코드 영향 0.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: Phase A-5 (HeadUp 5건 — A-5a `headJump=true` 확인 + A-5b/c 이미 이식 확인 + A-5d `headFallDamageStartDistance=2F` + A-5e `headFallDamageFactor=2F` ★ 신규). 원본 L260/L264/L265.

진행률: Phase A 21/40+ (~52.5%), 전체 #2.5 21/~110 (~19.1%).

### 세션 5 — 2026-04-25 — Phase A-5 (HeadUp 5건: 확인 3 + IO 추가 2 + 주석 라인 정정 2)

사용자 지시: "엄격 1:1" 유지 + Phase A-5 5건 (A-5a~A-5e) 이식.

진행한 작업:
1. 원본 라인 + 기본값 확보 (`SmartMovingConfig.java` L260-L265):
   - L260 `_headJump = Unmodified("move.jump.head.charge")` → 기본 true
   - L261 `_headJumpControlFactor = DecreasingFactor("move.jump.head.control.factor").defaults(0.2F)` → 0.2F
   - L262 `_headJumpChargeMaximum = Positive("move.jump.head.charge.maximum").defaults(10F)` → 10F
   - L264 `_headFallDamageStartDistance = Positive("move.fall.head.damage.start.distance").values(2F, 1F, 3F)` → 2F (default)
   - L265 `_headFallDamageFactor = IncreasingFactor("move.fall.head.damage.factor").defaults(2F)` ★ → 2F (오버라이드)
2. 1.21.1 이식 위치 확인:
   - A-5a: 필드 L251 = true ✅ + IO L1043 (key: `move.jump.head.charge` 최신) + save L1187 ✅
   - A-5b: 필드 L252 = 0.2F ✅ + IO L1044 (key: `move.forward.jump.control.factor` legacy) + save L1188 ✅
   - A-5c: 필드 L253 = 10F ✅ + IO L1045 (key: `move.forward.jump.charge.maximum` legacy) + save L1189 ✅
   - A-5d: 필드 L678 = 2F ✅ + IO **없음** ❌ → 신규 등록 필요
   - A-5e: 필드 L685 = 2F ✅ + IO **없음** ❌ → 신규 등록 필요
   - 발견: A-5d/e 필드 주석에 "원본 L395/L396" 으로 라인 번호 오기 (실제 L264/L265). 정정.
3. 1.21.1 이식 (`src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java`):
   - 필드 주석 정정 (L672-L685) — L395/L396 → L264/L265, ★ 오버라이드 명시
   - `load()` IO L1046-L1047 — A-5d/e 신규 (key 최신: `move.fall.head.damage.{start.distance|factor}`)
   - `save()` IO L1190-L1191 — A-5d/e 신규
4. key 명 결정:
   - 신규 A-5d/e 는 **최신 key** (`move.fall.head.damage.*`) 채택. 1:1 원칙상 원본의 주(default) key. 기존 사용자 config 에 해당 항목이 없을 가능성 大 (이전 IO 미등록) → 호환성 영향 0.
   - 기존 A-5b/c 의 legacy key 유지 (변경 시 사용자 config 호환성 깨짐 위험). head 그룹 일관성 정비는 별도 정리 원자로 후속 처리 가능.
5. 근사 여부: 없음. 1:1.

완료 전 검증 체크리스트 (세션 5 기준):
- [근거] 원본 라인 확보 — 로컬 `SmartMovingConfig.java` L260-L265 + Properties.java L189-L192 (IncreasingFactor 기본 1F)
- [근거] 1.21.1 이식 위치 확정 — 필드 L251-L253 (head*) + L678/L685 (headFallDamage*) / load L1043-L1047 / save L1187-L1191
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (값/기본값/오버라이드 모두 일치)
- [분기] 분기 없음 (단순 boolean/float 5개)
- [상수] true / 0.2F / 10F / 2F / 2F ★ (A-5e IncreasingFactor.defaults(2F) 오버라이드 정확 반영)
- [타이밍] 필드 선언만 — 호출 타이밍 없음. 실제 사용은 Phase B/D 와 handleCrash (B-24, 세션 53) 에서 발생.
- [근사] 근사 없음. §7 등록 없음.
- [신규] 발견: A-5d/e 필드 주석 라인 번호 오기 (L395/L396 → L264/L265) 정정. IO 누락 발견 → 등록.
- [회귀] IO 신규 등록 + 주석 정정 — 동작 변경 0. 기존 IO 등록 (A-5a/b/c) 무영향.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: Phase A-6 (Angle 4건 — A-6a `angleJumpSide=true` + A-6b `angleJumpBack=true` 이미 이식 확인 + A-6c `angleJumpHorizontalFactor=0.4F` ★ + A-6d `angleJumpVerticalFactor=0.2F` ★ 이미 이식 확인). 원본 L268-L271. (1.21.1 기존 값 0.3F vs 원본 최신 0.4F 검증 필요 — `_sm_1_3` 오버라이드 적용 여부.)

진행률: Phase A 26/40+ (~65%), 전체 #2.5 26/~110 (~23.6%).

### 세션 6 — 2026-04-25 — Phase A-6 (Angle 4건: 값 오역 정정 1 + 확인 3 + 주석 정비)

사용자 지시: "엄격 1:1" 유지 + Phase A-6 4건 (A-6a~A-6d) 이식.

진행한 작업:
1. 원본 라인 + 기본값 확보 (`SmartMovingConfig.java` L268-L271):
   - L268 `_angleJumpSide = Unmodified("move.jump.angle.side")` → 기본 true
   - L269 `_angleJumpBack = Unmodified("move.jump.angle.back")` → 기본 true
   - L270 `_angleJumpHorizontalFactor = PositiveFactor(...).defaults(0.3F).defaults(0.4F, _sm_1_3)` ★ → **0.4F** (sm_1_3 이상 오버라이드. 1.7.10 은 sm_1_3 이후라 0.4F 채택)
   - L271 `_angleJumpVerticalFactor = PositiveFactor(...).defaults(0.2F)` ★ → 0.2F
2. 1.21.1 이식 위치 확인 (변경 전):
   - A-6a 필드 L254 = true ✅ + IO L1048 ✅ + save L1194 ✅
   - A-6b 필드 L255 = true ✅ + IO L1049 ✅ + save L1195 ✅
   - A-6c 필드 L256 = **0.3F** ❌ — sm_1_3 오버라이드 미적용 오역 / IO L1050 ✅ + save L1196 ✅
   - A-6d 필드 L257 = 0.2F ✅ + IO L1051 ✅ + save L1197 ✅
3. 1.21.1 이식 (`src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java`):
   - 필드 L254-L262 — 그룹 헤더 주석 + 4 항목 주석 정비 + **A-6c 값 0.3F → 0.4F 정정**
   - load/save IO 변경 없음 (이미 ✅)
4. 근사 여부: 없음. 1:1.

발견 + 정정 (회귀 가능 영역)
- A-6c `angleJumpHorizontalFactor` 가 0.3F 로 이식되어 있어 사이드/백 점프 수평 배율이 ~25% 작은 상태였음. 0.4F 정정으로 원본 1.7.10 기본 동작 복구. 사용자 인게임 체감 변화 가능 (의도된 정정).

완료 전 검증 체크리스트 (세션 6 기준):
- [근거] 원본 라인 확보 — 로컬 `SmartMovingConfig.java` L268-L271 + Properties.java L185-L186 (PositiveFactor 기본 1F)
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingConfig.java` 필드 L254-L262 / load L1048-L1051 / save L1194-L1197
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (값/기본값/오버라이드 모두 일치 후)
- [분기] 분기 없음 (단순 boolean/float 4개)
- [상수] true / true / **0.4F** ★ (오버라이드) / 0.2F ★ — 모두 정확
- [타이밍] 필드 선언만 — 호출 타이밍 없음. Phase B isJumpingEnabled (Side/Back 분기) + Phase D Angle 분기에 사용.
- [근사] 근사 없음. §7 등록 없음.
- [신규] 발견: A-6c 값 오역 (0.3F → 0.4F). §0 미이식 표 "0.3-0.4F" 모호 표기 → "0.4F (sm_1_3 이상 오버라이드)" 명확화.
- [회귀] 값 정정 (0.3F → 0.4F) — 이전 경량 tryJump (`Jumper.tryAngleJump`) 가 이 값을 사용 중이면 인게임 동작 변화 (수평 ~33% 증가). 의도된 정정 (원본 일치).
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: Phase A-7 (ClimbUp 3건 — A-7a `climbUpJump=true` + A-7b `climbUpJumpVerticalFactor=1F` (DecreasingFactor 기본 1F) + A-7c `climbUpJumpHandsOnlyVerticalFactor=0.8F` ★ 신규). 원본 L274-L276.

진행률: Phase A 30/40+ (~75%), 전체 #2.5 30/~110 (~27.3%).

---

## 7. 근사 이식 지점 (이 포커스)

**§7 이 파일**: Phase 진행 중 불가피한 근사 이식 지점을 여기 등록. 포커스 완결 시 0건 목표.
현재 시작 시점: 0건.

---

## 8. 소비처 영향 감사

Jumper factor 변경은 다음에 영향:
- **포커스 #2 Extended B-26 경량** (세션 134): 기존 `trySlideDownJump` 완전 대체 — 제거 후 통합.
- **포커스 #1 애니메이션**: 점프 속도 변경 → `isSprintJump` 등 애니 트리거에 영향 가능.
- **포커스 #3 상태 전환**: 점프 후 `isHeadJumping` / `isJumping` 상태 타이밍 미세 차이.
- **포커스 #4 키 조합**: Angle 점프 (LEFT/RIGHT/BACK) 이중 클릭 판정 건드릴 수 있음 → 호출
  경로 유지 확인.

---

## 9. 참고 자료

### 원본 소스 경로 (로컬)
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingSelf.java` (L1999-L2136 tryJump)
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\config\SmartMovingClientConfig.java` (L172-L547 Jumper 인프라)
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\config\SmartMovingConfig.java` (L227-L310 Jumper factor 필드)
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\properties\Properties.java` (L180-L215 factor 기본값)

### 1.21.1 이식 대상
- `src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java`
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingJumper.java`
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` (exhaustion 필드)
- `src/client/java/choco/ratel/smartmoving/mixin/client/MixinLivingEntityClient.java` (exhaustion 리셋)
