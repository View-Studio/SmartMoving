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

### Exhaustion 시스템 (Phase C) — **세션 18: skip 결정**

**결정 (2026-04-25, 세션 18)**: Phase C 전체 skip. 사용자 1:1 룰의 "Phase C skip 금지"
보다 상위 메타 전제인 "Easy 1:1 충실 + focus_05 일관" 우선.

**근거**:
1. Easy default 에서 모든 jumpExhaustion=false (Boolean 17개) → `isJumpExhaustionEnabled`
   항상 false → Phase D-5 (점프 차단 게이트) + D-14 (점프 후 누적) 의 안쪽 블록 100% dead.
2. focus_05 §6.5 P-9~P-19 + 685-700 줄에서 동일 시스템 명시적 배제 ("14종 점프 피로 전체").
3. HUD 피로도 바: 1.21.1 `SmartMovingHud.java` L61 이 `cfg.climbExhaustionStop` 사용
   (climbing 한정). `getMaxExhaustion()` 호출처 0건 → Phase C 의 HUD 의존 0.
4. `maxExhaustionToStartAction/ForAction` 변수: 1.21.1 ClientState 에 없음. 다른 액션
   (climb/run/sprint exhaustion) 도 Easy default false 라 dead. 미래 그쪽 시스템 이식
   시점에 그때 추가하면 됨 (작업량 동일).

**§7 근사 등록**: D-5 + D-14 두 블록 통째로 skip. 사용자 수동 활성화 시 미작동 명시.

**미이식 항목 목록 (참조용 — 실제 작업 X)**:
- `exhaustion` ✅ (이미 ClientState 이식, focus_05)
- `maxExhaustionToStartAction` / `maxExhaustionForAction` 미이식 (skip)
- `isJumpExhaustionEnabled(speed, type)` 미이식 (skip)
- `getJumpExhaustionGain(speed, type, charge)` 미이식 (skip)
- `getJumpExhaustionStop(speed, type, charge)` 미이식 (skip)
- `getMaxExhaustion()` 미이식 (skip)
- Exhaustion 관련 Config 51+ 필드 미이식 (skip)

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
- [x] A-7a. `climbUpJump = true` (원본 L274) — 세션 7 (신규)
- [x] A-7b. `climbUpJumpVerticalFactor = 1F` (원본 L275, DecreasingFactor 기본) — 세션 7 (신규)
- [x] A-7c. `climbUpJumpHandsOnlyVerticalFactor = 0.8F` ★ (원본 L276, defaults(0.8F)) — 세션 7 (신규)

**A-8. ClimbBackUp 필드 5건**
- [x] A-8a. `climbBackUpJump = true` (원본 L279) — 세션 8 (신규)
- [x] A-8b. `climbBackUpJumpVerticalFactor = 0.2F` ★ (원본 L280, sm_3_1 오버라이드) — 세션 8 (신규)
- [x] A-8c. `climbBackUpJumpHorizontalFactor = 0.3F` ★ (원본 L281, sm_3_1 오버라이드) — 세션 8 (신규)
- [x] A-8d. `climbBackUpJumpHandsOnlyVerticalFactor = 0.8F` ★ (원본 L282) — 세션 8 (신규)
- [x] A-8e. `climbBackUpJumpHandsOnlyHorizontalFactor = 1F` (원본 L283, DecreasingFactor 기본) — 세션 8 (신규)

**A-9. ClimbBackHead 필드 5건**
- [x] A-9a. `climbBackHeadJump = true` (원본 L286) — 세션 9 (신규)
- [x] A-9b. `climbBackHeadJumpVerticalFactor = 0.2F` ★ (원본 L287, sm_3_1 오버라이드) — 세션 9 (신규)
- [x] A-9c. `climbBackHeadJumpHorizontalFactor = 0.3F` ★ (원본 L288, sm_3_1 오버라이드) — 세션 9 (신규)
- [x] A-9d. `climbBackHeadJumpHandsOnlyVerticalFactor = 0.8F` ★ (원본 L289) — 세션 9 (신규)
- [x] A-9e. `climbBackHeadJumpHandsOnlyHorizontalFactor = 1F` (원본 L290, DecreasingFactor 기본) — 세션 9 (신규)

**A-10. WallUp 필드 5건** (이미 전부 이식)
- [x] A-10a. `wallUpJump = true` (원본 L293) — 세션 10 (이미 이식 + IO 확인)
- [x] A-10b. `wallUpJumpVerticalFactor = 0.4F` (원본 L294) — 세션 10 (이미 이식 + IO 확인)
- [x] A-10c. `wallUpJumpHorizontalFactor = 0.15F` ★ (원본 L295) — 세션 10 (이미 이식 + IO 확인, §3 "미이식" 표기는 오기)
- [x] A-10d. `wallUpJumpFallMaximumDistance = 2F` (원본 L296) — 세션 10 (필드/값 이식 + **IO key '.distance' suffix 누락 정정** `move.jump.wall.fall.maximum` → `move.jump.wall.fall.maximum.distance`)
- [x] A-10e. `wallUpJumpOrthogonalTolerance = 5F` (원본 L297) — 세션 10 (이미 이식 + IO 확인)

**A-11. WallHead 필드 4건** (이미 전부 이식)
- [x] A-11a. `wallHeadJump = true` (원본 L300) — 세션 11 (이미 이식 + IO 확인)
- [x] A-11b. `wallHeadJumpVerticalFactor = 0.3F` (원본 L301) — 세션 11 (이미 이식 + IO 확인)
- [x] A-11c. `wallHeadJumpHorizontalFactor = 0.15F` ★ (원본 L302) — 세션 11 (이미 이식 + IO 확인, §3 "미이식" 표기는 오기)
- [x] A-11d. `wallHeadJumpFallMaximumDistance = 3F` (원본 L303) — 세션 11 (필드/값 이식 + **IO key '.distance' suffix 누락 정정** `move.jump.wall.head.fall.maximum` → `move.jump.wall.head.fall.maximum.distance`)

**A-12. Properties IO 등록** (추가된 모든 필드 `load()` + `save()` 양방향)
- [x] A-12. 세션 1~11 진행 중 매 원자마다 load/save IO 양방향 동시 등록 완료. A-1 (3건) ~ A-11 (4건) 합계 50건 + 그룹 헤더 / 주석 정비 / key 정정 (A-10d, A-11d). Phase A 전수 IO 등록 완결. — 세션 11

---

### Phase B. 판정 헬퍼 메서드 전수 이식 (SmartMovingConfig.java)

**B-1. `getJumpSpeed` 헬퍼 (SmartMovingSelf L2148-L2163)** — 세션 12 완료
- [x] B-1. 신설 — isStanding/isSneaking/isRunning/isSprinting/angle 기반 speed 반환:
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

**B-2. Speed 상수** (원본 L172-L176) — 세션 12 완료
- [x] B-2. `SmartMovingConfig` 정적 상수 `SPEED_SPRINTING=0, SPEED_RUNNING=1,
     SPEED_WALKING=2, SPEED_SNEAKING=3, SPEED_STANDING=4` 5개 신설 (B-1 과 묶어 처리).

**B-2.5. Jump Type 상수** (원본 L178-L192, B-3 의존으로 신규 원자 추가) — 세션 13 완료
- [x] B-2.5. `SmartMovingConfig` 정적 상수 15개 — `JUMP_TYPE_UP=0, CHARGE_UP=1, ANGLE=2,
     HEAD_UP=3, SLIDE_DOWN=4, CLIMB_UP=5, CLIMB_UP_HANDS_ONLY=6, CLIMB_BACK_UP=7,
     CLIMB_BACK_UP_HANDS_ONLY=8, CLIMB_BACK_HEAD=9, CLIMB_BACK_HEAD_HANDS_ONLY=10,
     WALL_UP=11, WALL_HEAD=12, WALL_UP_SLIDE=13, WALL_HEAD_SLIDE=14`. 1.21.1 SmartMovingJumper
     기존 jumpType 매핑 정렬은 Phase E 처리.

**B-3. `isJumpingEnabled(speed, type)`** (원본 L194-L227) — 세션 13 완료
- [x] B-3. SmartMovingConfig 에 instance 메서드 신설 (B-2.5 와 묶음). 11 type 분기 전수 + speed 5 분기:
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

**B-4. `getJumpHorizontalFactor(speed, type)`** (원본 L465-L505) — 세션 14 완료
- [x] B-4. SmartMovingConfig 에 instance 메서드 신설. `!enabled` 시 `speed==Running ? 2F : 1F`.
  `enabled` 시 base `jumpHorizontalFactor` × type 분기 (Angle/ClimbBackUp×2/ClimbBackHead×2/WallUp/WallHead) +
  early return (9 type) → speed 분기 (Sprint/Run/Walk/Sneak/Stand). L501 의 `speed==Standing && type!=ClimbBack*`
  → `* 0F` 특수 처리 포함.

**B-5. `getJumpVerticalFactor(speed, type)`** (원본 L418-L463) — 세션 15 완료
- [x] B-5. SmartMovingConfig 에 instance 메서드 신설. base `jumpVerticalFactor` × type 분기
  (Angle 즉시 early return / ClimbUp×2 / ClimbBackUp×2 / ClimbBackHead×2 / WallUp+WallHead 누적) +
  9 type early return → speed 분기 (Sprint/Run/Walk/Sneak/Stand). WallHead = base × wallUp × wallHead 특수.

**B-6. `getMaxHorizontalMotion(speed, type, inWater)`** (원본 L508-L525) — 세션 16 완료
- [x] B-6. SmartMovingConfig 에 instance 메서드 신설. baseMaxMotion = `0.117852041920949F`
  (inWater 시 `0.07839602977037292F`) × speed 분기 (Sprint→sprintFactor / Run→runFactor /
  Sneak→sneakFactor). `!enabled` 시 speed==Running → baseMaxMotion × 1.3F. type 파라미터 미사용.

**B-7. `getJumpChargeFactor(charge)`** (원본 L401-L408) — 세션 17 완료
- [x] B-7. SmartMovingConfig 에 instance 메서드 신설. `1F + (charge / max) × (factor - 1F)` 선형
  보간. Jumper 호출 교체는 Phase D-6 에서.

**B-8. `getHeadJumpFactor(charge)`** (원본 L410-L416) — 세션 17 완료
- [x] B-8. SmartMovingConfig 에 instance 메서드 신설. `(charge - 1) / (max - 1)` 공식. Jumper
  호출 교체는 Phase D-10 에서.

---

### Phase C. Exhaustion 시스템 — **세션 18: skip 결정 (Easy 1:1 + focus_05 일관)**

> 사용자 결정 (2026-04-25, 세션 18): Easy default 에서 모든 jumpExhaustion=false →
> Phase D-5 + D-14 의 안쪽 블록 100% dead. focus_05 §6.5 P-9~P-19 와 일관되게 skip.
> §7 D-5/D-14 두 블록 근사 등록. 사용자 수동 활성화 시 미작동 명시.

**C-1. Exhaustion Config 필드 전수** — skip
- [~] C-1a. Jump exhaustion 활성/종료 필드 17건 — skip (Easy default false → dead)
- [~] C-1b. Jump exhaustion gain factor 필드 17건 — skip (호출처 0)
- [~] C-1c. Jump exhaustion stop factor 필드 17건 — skip (호출처 0)
- [~] C-1d. ChargeUp exhaustion 특수 3건 — skip
- [~] C-1e. base exhaustion loss — focus_05 H-3/H-4/H-5 에서 27건 이미 이식 완료. 추가 skip 없음.

**C-2. Exhaustion 상태 필드 (ClientState)** — skip
- [x] C-2a. `exhaustion` ✅ focus_05 H-8 에서 이미 이식 완료
- [~] C-2b. `maxExhaustionToStartAction` — skip (호출처 0, 미래 climb/run/sprint exhaustion 이식 시 추가)
- [~] C-2c. `maxExhaustionForAction` — skip (동일)

**C-3. 판정 메서드 3개 (SmartMovingConfig)** — skip
- [~] C-3a. `isJumpExhaustionEnabled` — skip (D-5 호출처 skip)
- [~] C-3b. `getJumpExhaustionGain` — skip
- [~] C-3c. `getJumpExhaustionStop` — skip

**C-4. `getMaxExhaustion()`** — skip
- [~] C-4. 1.21.1 SmartMovingHud L61 이 `cfg.climbExhaustionStop` 직접 사용 (climbing 한정).
     원본 `getMaxExhaustion()` 미호출 → 이식 불필요.

**C-5. `getFactor(hunger, onGround, ...)`** — 이미 이식
- [x] C-5. focus_05 H-7 에서 SmartMovingConfig 에 이미 이식 완료. 본 포커스 추가 작업 0.

**C-6. Exhaustion 틱 갱신** — skip
- [~] C-6. C-2b/c skip 으로 리셋 대상 변수 자체 없음. focus_05 H-9 의 `handleExhaustion` 가
     `exhaustion -= exhaustionLoss` 로 자연 감소 → Phase C 와 무관하게 이미 동작.

**범례**: `[x]` = 완료, `[~]` = skip 결정, `[ ]` = 미완료 (현재 0건)

---

### Phase D. tryJump 전면 재작성 (SmartMovingJumper.java) — **세션 19: 일괄 완료**

> 사용자 결정 (2026-04-25, 세션 19): tryJump 단일 함수 분할 시 중간 커밋마다 빌드 깨짐 →
> D-1~D-18 + E-1 (trySlideDownJump 통합) 한 세션에 일괄 진행. D-5/D-14 skip (§7-1 적용).
> 결과: SmartMovingJumper.tryJump 새 시그니처로 완전 재작성 + 호출처 7곳 (내부 5 + 외부 2)
> 정비 + trySlideDownJump 제거 + vanilla() public 변경 + 상수 alias 통일.

**D-1. 시그니처 정비** — 세션 19 완료
- [x] D-1. 새 시그니처 `boolean tryJump(ClientPlayerEntity, SmartMovingClientState, int type,
     Boolean inWaterOrNull, Boolean isRunningOrNull, Float angle)` 이식. 반환: `enabled`.
     기존 `tryJump(player, sm, jumpType, charge)` 완전 대체.

**D-2. WallUpSlide/WallHeadSlide 변환** — 세션 19 완료
- [x] D-2. (세션 19 완료) 원본 L2002-L2006:
  ```java
  boolean noVertical = false;
  if (type == WallUpSlide || type == WallHeadSlide) {
      type = (type == WallUpSlide) ? WallUp : WallHead;
      noVertical = true;
  }
  ```

**D-3. 지역 변수 계산**
- [x] D-3. (세션 19 완료) 원본 L2008-L2012:
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
- [x] D-4. (세션 19 완료) 원본 L2014-L2015:
  ```java
  int speed = getJumpSpeed(sm.isStanding, sm.isSlow, isRunning, sm.isFast, angle);
  boolean enabled = cfg.isJumpingEnabled(speed, type);
  ```

**D-5. Exhaustion 체크 + maxExhaustion 조정** (Phase C 의존)
- [~] D-5. (세션 19 SKIP — §7-1 적용) 원본 L2018-L2027:
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
- [x] D-6. (세션 19 완료) 원본 L2029-L2032:
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
- [x] D-7. (세션 19 완료) 원본 L2034-L2038:
  ```java
  if (!up) {
      horizontalJumpFactor = (float) Math.sqrt(
          horizontalJumpFactor * horizontalJumpFactor
          + verticalJumpFactor * verticalJumpFactor);
      verticalJumpFactor = 0;
  }
  ```

**D-8. maxHorizontalMotion + verticalMotion 초기 계산**
- [x] D-8. (세션 19 완료) 원본 L2040-L2045:
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
- [x] D-9. (세션 19 완료, vanilla() public 변경) 원본 L2047-L2062:
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
- [x] D-10. (세션 19 완료) 원본 L2065-L2079:
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
- [x] D-11. (세션 19 완료) 원본 L2081-L2095:
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
- [x] D-12. (세션 19 완료) 원본 L2097-L2110:
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
- [x] D-13. (세션 19 완료) 원본 L2113-L2118:
  ```java
  if (up && !noVertical) {
      motionY = verticalMotion;
      player.incrementStat(Stats.JUMP);
      sm.isSprintJump = sm.isFast;
  }
  ```

**D-14. exhaustion gain 적용**
- [~] D-14. (세션 19 SKIP — §7-1 적용) 원본 L2120-L2124:
  ```java
  if (exhausionEnabled) {
      float gain = cfg.getJumpExhaustionGain(speed, type, sm.jumpCharge);
      sm.exhaustion += gain;
  }
  ```

**D-15. head → isHeadJumping + setHeightOffset**
- [x] D-15. (세션 19 완료) 원본 L2126-L2130:
  ```java
  if (head) {
      sm.isHeadJumping = true;
      setPoseSmall(player);
      sm.heightOffset = -1F;
  }
  ```

**D-16. 최종 setVelocity + isJumping + onLivingJump**
- [x] D-16. (세션 19 완료) 원본 L2131-L2134:
  ```java
  player.setVelocity(motionX, noVertical ? vel.y : motionY, motionZ);
  // sp.isAirBorne = true — vanilla 자동
  sm.isJumping = true;
  // onLivingJump() — PlayerEntity.jump() 호출 또는 vanilla 점프 이벤트
  ```

**D-17. 반환**
- [x] D-17. (세션 19 완료, return enabled) `return enabled;`

**D-18. 상태 클리어 (원본 tryJump 외부 호출 후 처리)**
- [x] D-18. (세션 19 완료, 1.21.1 동작 유지 — 내부 처리) 원본에는 tryJump 종료 후 호출측에서 `jumpCharge=0; headJumpCharge=0;
     blockJumpTillButtonRelease=true; jumpPending=false;` 리셋. 현 1.21.1 은 tryJump 내부에서
     이미 처리. 원본 순서 확인 후 정리.

---

### Phase E. 호출 경로 통합

**E-1. trySlideDownJump 통합 또는 유지** — 세션 19 완료
- [x] E-1. Phase D 일괄 진행과 함께 처리. `trySlideDownJump` 별도 메서드 삭제 + ClientState L1394
     호출처를 `tryJump(SLIDE_DOWN, false, wasRunning, null)` 직접 호출로 교체. B-42-B26 경량
     해소 대체 완료.

**E-2. 기존 호출처 재검토** — 세션 19/20 완료 (전수 처리)
- [x] E-2. 호출처 7곳 (내부 5 + 외부 2) 모두 새 시그니처 적용 완료. 추가 호출처 0 (grep 검증):
  - handleJumping CHARGE_UP / HEAD_UP / 수면 UP / 일반 UP — 세션 19
  - handleWallJumping WALL_UP/HEAD/UP_SLIDE/HEAD_SLIDE (jumpType 변수 + angle 파라미터) — 세션 19
  - ClientState L1349 Creative flying UP — 세션 19
  - ClientState L1394 SlideDown (trySlideDownJump 대체) — 세션 19 (E-1 통합)
  - 더블클릭 방향 점프 (LEFT/RIGHT/BACK) — 세션 20 E-4 ANGLE type 통합
  - **CLIMB_UP / CLIMB_BACK / CLIMB_BACK_HEAD 호출처 0** — 1.21.1 climb-jump 시스템 미이식

**E-3. HandsOnly 분기 추가** — 세션 20: N/A
- [x] E-3. 1.21.1 SmartMovingClimber 에 climb-jump 호출 자체 0 (`tryJump(CLIMB_UP*)` 호출 없음).
     HandsOnly 분기 추가할 호출처 자체가 존재하지 않음 → N/A. 미래 climb-jump 시스템 이식 시
     별도 포커스에서 처리.

**E-4. Angle 점프 통합** — 세션 20 완료
- [x] E-4. handleJumping 더블클릭 방향 점프 인라인 코드 (L329-L390 약 60줄) → 새 tryJump
     (ANGLE, null, null, worldAngleDeg) 단일 호출 통합. 기존 인라인의 vanilla Up 0.41999...
     수직 속도 → ANGLE type 의 angleJumpVerticalFactor=0.2F 기반 (D-8) 으로 1:1 정정.
     SmartMovingJumper.LEFT/RIGHT/BACK sentinel 상수 사용처 0 → 제거.

**E-5. Exhaustion 매 틱 리셋** — 세션 18: SKIP (Phase C 일관)
- [~] E-5. Phase C skip 결정과 함께 maxExhaustion* 변수 자체 미존재 → 리셋 대상 없음. SKIP.

---

### Phase F. 감사 + 플레이테스트

**F-1. 원본 tryJump L1999-L2136 side-by-side 비교** — 세션 21 완료
- [x] F-1. 138줄 line-by-line 감사 → **회귀 버그 2건 발견 + 즉시 정정**:
     1. **enabled 게이트 누락**: 원본 L2016 `if (enabled) { ... }` 가 D-5~D-16 전체를
        감싸지만 1.21.1 새 tryJump 는 enabled 게이트 없이 모두 실행. 사용자가 sub-jump 비활성
        (예: `walkJump=false`) 시에도 점프 처리됨. → enabled 안쪽으로 D-6~D-16 이동.
     2. **D-9 if/else 누락**: 원본 L2047-L2111 은 `if (Up && vanilla) {...} else {head/angle/scale}`
        구조. 1.21.1 은 if/else 없이 모든 블록 if. → 일반 Up 점프 (vanilla 경로) 시 D-12 스케일이
        추가 적용되어 sprint 점프 수평 속도 2배 증폭. → if/else 구조 복원.
     세션 21 정정 완료. 빌드 통과.

**F-2. 원본 getJumpHorizontalFactor L465-L505 side-by-side 비교** — 세션 14 완료 / 세션 21 재확인
- [x] F-2. 세션 14 line-by-line 이식 + L501 `speed==Standing && type!=ClimbBack*` → `*0F`
     엣지 케이스 정확 반영 확인. 세션 21 추가 검증 — 원본 ↔ 1.21.1 1:1 일치.

**F-3. 원본 getJumpVerticalFactor L418-L463 비교** — 세션 15 완료 / 세션 21 재확인
- [x] F-3. 세션 15 line-by-line 이식 + WallHead 누적 곱셈 (`base × wallUp × wallHead`) 특수
     반영 확인. Angle 즉시 early return + 9 type early return 모두 보존. 세션 21 추가 검증 1:1.

**F-4. Exhaustion 3-메서드 감사** — 세션 18: SKIP (Phase C 일관)
- [~] F-4. Phase C skip 결정으로 isJumpExhaustionEnabled / getJumpExhaustionGain /
     getJumpExhaustionStop 미이식 → 감사 대상 없음. §7-1 영구 등록.

**F-5. 빌드 + 회귀 감사** — 세션 21 완료
- [x] F-5. `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s).
     `grep "// TODO\|// \[미확인\]\|// 아마\|// 추정"` in `src/` — **0건**.
     포커스 #2 Extended 회귀 0건. F-1 회귀 정정 후에도 빌드 통과.

**F-6. 사용자 인게임 플레이테스트** — 사용자 작업 (대기)
- [ ] F-6. 점프 모든 타입 (일반/스프린트/헤드/벽점프/벽점프슬라이드/사이드/백/슬라이드다운/
     클라이밍점프/클라이밍백점프) × Config 수정 시 배율 반영 확인. **인게임 테스트 체크리스트
     (세션 21 제공)**:
   - **일반 점프**: walking/sprinting 시 수평·수직 속도 정상 (`sprintJumpHorizontalFactor=2F` 반영).
   - **차징 점프 (Sneak 홀드 → 릴리즈)**: 차징 시간 비례 수직 속도 증가 (`jumpChargeFactor=1.3F` 반영).
   - **헤드 점프 (Grab 홀드 → 릴리즈)**: 차징 시간 비례 수평/수직 회전 (D-10).
   - **벽 점프 (수직 벽 → Jump 더블클릭)**: WallUp/WallHead 시 반사 각도 정상 (0.4F/0.3F V).
   - **벽 점프 슬라이드 (벽 닿은 채 Jump)**: WallUpSlide/WallHeadSlide 시 수직 속도 유지.
   - **사이드/백 점프 (방향키 더블클릭)**: ★ **세션 20 정정 — angleJumpVerticalFactor=0.2F 기반**.
     이전보다 수직 속도 약 5분의 1 감소. 의도된 원본 동작.
   - **슬라이드 다운 점프 (sprint + grab + sneak)**: 수평 속도 증폭 (vanilla 1F base + jumpFactor).
   - **사용자 Config 수정 테스트**: `sprintJumpHorizontalFactor=3F` 로 변경 → sprint 점프 수평
     속도 50% 증가 확인 (Phase A 인프라 활성).
   - **JUMP_BOOST 포션 테스트**: amplifier 별 수직 속도 증가 (`jumpFactor` D-6 + vanilla bonus D-9).
   - **Creative 모드 테스트**: vanilla() = `!enabled || vanillaStyle` → SM 활성/Creative 분기.
   - **F-1 회귀 검증**: sprint 점프 수평 속도 2배 증폭 버그 (세션 21 정정 전) 가 정정됐는지 확인.

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

**전체 규모 (세션 18 갱신)**: Phase A 52 필드 (완료) + Phase B 7 메서드 + 상수 20 (완료) + ~~Phase C~~ (skip 결정)
+ Phase D 18 서브 (D-5/D-14 skip) + Phase E 5 원자 + Phase F 6 감사. **약 80+ 원자 / 예상 8-15 세션**.

**의존 그래프 (세션 18 갱신)**:
```
Phase A (Config factor 필드, 완료) → Phase B (판정 헬퍼, 완료)
   ↓
~~Phase C (Exhaustion)~~ — skip 결정 (Easy 1:1 + focus_05 일관)
   ↓
Phase D (tryJump 재작성, D-5/D-14 skip + §7 등록)
   ↓
Phase E → Phase F
```

**권장 진행**: Phase A/B 완결 (세션 11/17). Phase C skip → Phase D 직접 진입. Phase D 의 D-5
(점프 차단 게이트) + D-14 (점프 후 누적) 두 블록만 통째로 skip + §7 근사 등록. 나머지 16개
서브 원자 (D-1~D-4, D-6~D-13, D-15~D-18) 정상 1:1 이식. Phase E/F 는 연결/검증.

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

### 세션 7 — 2026-04-25 — Phase A-7 (ClimbUp 3건: 전부 신규)

사용자 지시: "엄격 1:1" 유지 + Phase A-7 3건 (A-7a~A-7c) 이식.

진행한 작업:
1. 원본 라인 + 기본값 확보 (`SmartMovingConfig.java` L273-L276):
   - L274 `_climbUpJump = Unmodified("move.jump.climb.up")` → 기본 true
   - L275 `_climbUpJumpVerticalFactor = DecreasingFactor("move.jump.climb.up.vertical.factor")` (defaults 미지정) → DecreasingFactor 기본 1F (Properties.java L191-L192)
   - L276 `_climbUpJumpHandsOnlyVerticalFactor = DecreasingFactor(...).defaults(0.8F)` ★ → 0.8F
2. 1.21.1 이식 위치 확인:
   - climbUpJump 관련 필드 1.21.1 에 **전무** (grep 결과 0건). 모두 신규.
3. 1.21.1 이식 (`src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java`):
   - 필드 선언 — angle 그룹 (L262-L263) 직후 (L265-L274) 에 ClimbUp 그룹 신설 (헤더 + 3 항목)
   - `load()` IO L1058-L1060 — A-7a/b/c 신규 3건
   - `save()` IO L1204-L1206 — A-7a/b/c 신규 3건
   - key 명: `move.jump.climb.up{|.vertical.factor|.hands.only.vertical.factor}` (원본 그대로)
4. 근사 여부: 없음. 1:1.

완료 전 검증 체크리스트 (세션 7 기준):
- [근거] 원본 라인 확보 — 로컬 `SmartMovingConfig.java` L273-L276 + Properties.java L191-L192 (DecreasingFactor 기본 1F)
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingConfig.java` 필드 L265-L274 / load L1058-L1060 / save L1204-L1206
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (필드명 / 기본값 / key 명 일치)
- [분기] 분기 없음 (단순 boolean/float 3개)
- [상수] true / 1F (DecreasingFactor 기본) / **0.8F** ★ (오버라이드 정확 반영)
- [타이밍] 필드 선언만. Phase B getJumpVerticalFactor (ClimbUp/ClimbUpHandsOnly 분기) + Phase D ClimbUp 분기에 사용.
- [근사] 근사 없음. §7 등록 없음.
- [신규] 추가 의존 없음. §3 변경 없음.
- [회귀] 신규 필드만 추가 — 기존 코드 영향 0. ClimbUp/ClimbUpHandsOnly 점프 type 자체가 1.21.1 에 아직 없음 (Phase E 에서 처리). 현재 사용처 0.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: Phase A-8 (ClimbBackUp 5건 — 전부 신규: A-8a `climbBackUpJump=true` + A-8b `climbBackUpJumpVerticalFactor=0.2F` ★ + A-8c `climbBackUpJumpHorizontalFactor=0.3F` ★ + A-8d `climbBackUpJumpHandsOnlyVerticalFactor=0.8F` ★ + A-8e `climbBackUpJumpHandsOnlyHorizontalFactor=1F`). 원본 L279-L283.

진행률: Phase A 33/40+ (~82.5%), 전체 #2.5 33/~110 (~30%).

### 세션 8 — 2026-04-25 — Phase A-8 (ClimbBackUp 5건: 전부 신규)

사용자 지시: "엄격 1:1" 유지 + Phase A-8 5건 (A-8a~A-8e) 이식.

진행한 작업:
1. 원본 라인 + 기본값 확보 (`SmartMovingConfig.java` L278-L283):
   - L279 `_climbBackUpJump = Unmodified("move.jump.climb.back.up")` → 기본 true
   - L280 `_climbBackUpJumpVerticalFactor = DecreasingFactor(...).defaults(0.2F).defaults(1F, _pre_sm_3_1)` ★ → **0.2F** (sm_3_1 이상). _pre_sm_3_1 만 1F.
   - L281 `_climbBackUpJumpHorizontalFactor = DecreasingFactor(...).defaults(0.3F).defaults(1F, _pre_sm_3_1)` ★ → **0.3F** (sm_3_1 이상). _pre_sm_3_1 만 1F.
   - L282 `_climbBackUpJumpHandsOnlyVerticalFactor = DecreasingFactor(...).defaults(0.8F)` ★ → 0.8F
   - L283 `_climbBackUpJumpHandsOnlyHorizontalFactor = DecreasingFactor(...)` (defaults 미지정) → DecreasingFactor 기본 1F (Properties.java L191-L192)
2. 1.21.1 이식 위치 확인:
   - climbBackUp* 1.21.1 에 **전무** (grep 결과 0건). 모두 신규.
3. 1.21.1 이식 (`src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java`):
   - 필드 선언 — ClimbUp 그룹 (L274) 직후 ClimbBackUp 그룹 신설 (헤더 + 5 항목)
   - `load()` IO 신규 5건 — climbUpJumpHandsOnlyVerticalFactor 다음
   - `save()` IO 신규 5건 — 동일 위치
   - key 명: `move.jump.climb.back.up{|.vertical.factor|.horizontal.factor|.hands.only.vertical.factor|.hands.only.horizontal.factor}` (원본 그대로)
4. 근사 여부: 없음. 1:1.

완료 전 검증 체크리스트 (세션 8 기준):
- [근거] 원본 라인 확보 — 로컬 `SmartMovingConfig.java` L279-L283 + Properties.java L191-L192 (DecreasingFactor 기본 1F)
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingConfig.java` 필드 ClimbUp 그룹 직후 / load + save 양방향 신규
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (필드명 / 기본값 / key 명 일치)
- [분기] 분기 없음 (단순 boolean/float 5개)
- [상수] true / **0.2F** ★ (sm_3_1 오버라이드) / **0.3F** ★ (sm_3_1 오버라이드) / **0.8F** ★ (defaults 오버라이드) / 1F (DecreasingFactor 기본)
- [타이밍] 필드 선언만. Phase B getJumpVertical/HorizontalFactor (ClimbBackUp/HandsOnly 분기) + Phase D ClimbBackUp 분기에 사용.
- [근사] 근사 없음. §7 등록 없음.
- [신규] 추가 의존 없음. §3 변경 없음.
- [회귀] 신규 필드만 추가 — 기존 코드 영향 0. ClimbBackUp/HandsOnly 점프 type 자체가 1.21.1 에 아직 없음 (Phase E).
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: Phase A-9 (ClimbBackHead 5건 — 전부 신규: A-9a `climbBackHeadJump=true` + A-9b `climbBackHeadJumpVerticalFactor=0.2F` ★ + A-9c `climbBackHeadJumpHorizontalFactor=0.3F` ★ + A-9d `climbBackHeadJumpHandsOnlyVerticalFactor=0.8F` ★ + A-9e `climbBackHeadJumpHandsOnlyHorizontalFactor=1F`). 원본 L286-L290 — 구조 A-8 과 동일.

진행률: Phase A 38/40+ (~95%), 전체 #2.5 38/~110 (~34.5%).

### 세션 9 — 2026-04-25 — Phase A-9 (ClimbBackHead 5건: 전부 신규 + §3 A-9e 누락 보완)

사용자 지시: "엄격 1:1" 유지 + Phase A-9 5건 (A-9a~A-9e) 이식.

진행한 작업:
1. 원본 라인 + 기본값 확보 (`SmartMovingConfig.java` L285-L290) — 구조 A-8 ClimbBackUp 과 동일:
   - L286 `_climbBackHeadJump = Unmodified("move.jump.climb.back.head")` → 기본 true
   - L287 `_climbBackHeadJumpVerticalFactor = DecreasingFactor(...).defaults(0.2F).defaults(1F, _pre_sm_3_1)` ★ → **0.2F**
   - L288 `_climbBackHeadJumpHorizontalFactor = DecreasingFactor(...).defaults(0.3F).defaults(1F, _pre_sm_3_1)` ★ → **0.3F**
   - L289 `_climbBackHeadJumpHandsOnlyVerticalFactor = DecreasingFactor(...).defaults(0.8F)` ★ → 0.8F
   - L290 `_climbBackHeadJumpHandsOnlyHorizontalFactor = DecreasingFactor(...)` → DecreasingFactor 기본 1F
2. 1.21.1 이식 위치 확인:
   - climbBackHead* 1.21.1 에 **전무** (grep 결과 0건). 모두 신규.
3. 1.21.1 이식 (`src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java`):
   - 필드 선언 — ClimbBackUp 그룹 (L290) 직후 ClimbBackHead 그룹 신설 (헤더 + 5 항목)
   - `load()` IO 신규 5건 — climbBackUpJumpHandsOnlyHorizontalFactor 다음
   - `save()` IO 신규 5건 — 동일 위치
   - key 명: `move.jump.climb.back.head{|.vertical.factor|.horizontal.factor|.hands.only.vertical.factor|.hands.only.horizontal.factor}`
4. §3 보완: A-9e 항목 §3 누락 발견 → 추가 + 완료 표시.
5. 근사 여부: 없음. 1:1.

완료 전 검증 체크리스트 (세션 9 기준):
- [근거] 원본 라인 확보 — 로컬 `SmartMovingConfig.java` L286-L290 + Properties.java L191-L192 (DecreasingFactor 기본 1F)
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingConfig.java` 필드 ClimbBackUp 그룹 직후 / load + save 양방향 신규
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (필드명 / 기본값 / key 명 일치)
- [분기] 분기 없음 (단순 boolean/float 5개)
- [상수] true / **0.2F** ★ / **0.3F** ★ / **0.8F** ★ / 1F (DecreasingFactor 기본)
- [타이밍] 필드 선언만. Phase B getJumpVertical/HorizontalFactor (ClimbBackHead/HandsOnly 분기) + Phase D ClimbBackHead 분기에 사용.
- [근사] 근사 없음. §7 등록 없음.
- [신규] 발견: §3 A-9e 항목 누락 → 보완 추가.
- [회귀] 신규 필드만 추가 — 기존 코드 영향 0. ClimbBackHead/HandsOnly 점프 type 자체가 1.21.1 에 아직 없음 (Phase E).
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: Phase A-10 (WallUp 5건 — A-10a `wallUpJump=true` ✅ 확인 / A-10b `wallUpJumpVerticalFactor=0.4F` ✅ 확인 / A-10c `wallUpJumpHorizontalFactor=0.15F` ★ ✅ 확인 (이미 세션 1 이전에 이식됨) / A-10d `wallUpJumpFallMaximumDistance=2F` ✅ 확인 / A-10e `wallUpJumpOrthogonalTolerance=5F` ✅ 확인). 원본 L293-L297 — 모두 이미 이식 확인 + 주석 정비만.

진행률: Phase A 43/40+ (Phase A 완료 임박, 5건 초과 — 실제 약 50건), 전체 #2.5 43/~110 (~39%).

### 세션 10 — 2026-04-25 — Phase A-10 (WallUp 5건: 확인 4 + IO key 정정 1)

사용자 지시: "엄격 1:1" 유지 + Phase A-10 5건 (A-10a~A-10e) 검증 + 정정.

진행한 작업:
1. 원본 라인 + 기본값 확보 (`SmartMovingConfig.java` L293-L297):
   - L293 `_wallUpJump = Unmodified("move.jump.wall")` → 기본 true
   - L294 `_wallUpJumpVerticalFactor = DecreasingFactor("move.jump.wall.vertical.factor").defaults(0.4F)` → 0.4F
   - L295 `_wallUpJumpHorizontalFactor = DecreasingFactor("move.jump.wall.horizontal.factor").defaults(0.15F)` ★ → 0.15F
   - L296 `_wallUpJumpFallMaximumDistance = Positive("move.jump.wall.fall.maximum.distance").defaults(2F)` → 2F (key 끝 `.distance`)
   - L297 `_wallUpJumpOrthogonalTolerance = Positive("move.jump.wall.orthogonal.tolerance").defaults(5F)` → 5F
2. 1.21.1 이식 위치 확인:
   - A-10a 필드 L220 = true ✅ + IO L1080 ✅ + save L1239 ✅
   - A-10b 필드 L231 = 0.4F ✅ + IO L1085 ✅ + save L1244 ✅
   - A-10c 필드 L236 = 0.15F ✅ + IO L1087 ✅ + save L1246 ✅ (§3 "미이식 추가" 표기는 오기 — 이미 이식됨)
   - A-10d 필드 L224 = 2F ✅ + **IO key 오역**: `move.jump.wall.fall.maximum` (suffix `.distance` 누락). save L1241 동일 오역.
   - A-10e 필드 L228 = 5F ✅ + IO L1084 ✅ + save L1243 ✅
3. 1.21.1 정정 (`src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java`):
   - load L1082 key: `move.jump.wall.fall.maximum` → `move.jump.wall.fall.maximum.distance`
   - save L1241 key: 동일 정정
4. 발견: A-11d (`wallHeadJumpFallMaximumDistance`) 도 동일 오역 (load L1083 / save L1242) — 다음 세션 (A-11) 에서 정정.
5. 근사 여부: 없음. 1:1.

완료 전 검증 체크리스트 (세션 10 기준):
- [근거] 원본 라인 확보 — 로컬 `SmartMovingConfig.java` L293-L297 + Properties.java L181-L186 (Positive/PositiveFactor/DecreasingFactor 기본값)
- [근거] 1.21.1 이식 위치 확정 — 필드 L220-L236 / load L1080-L1087 / save L1239-L1246
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (값/key 모두 일치 후)
- [분기] 분기 없음 (단순 boolean/float 5개)
- [상수] true / 0.4F / **0.15F** ★ / 2F / 5F — 모두 정확
- [타이밍] 필드 선언만. Phase B getJumpVertical/HorizontalFactor (WallUp 분기) + Phase D WallUp 분기에 사용. WallUp 점프 type 은 1.21.1 이미 존재.
- [근사] 근사 없음. §7 등록 없음.
- [신규] 발견: A-10d IO key 오역 (`.distance` 누락) → 정정. A-11d 도 동일 오역 발견 (다음 세션).
- [회귀] IO key 변경 — 사용자가 이전에 잘못된 key (`move.jump.wall.fall.maximum`) 로 설정 파일에 값을 적었다면 정정 후 그 값은 무시됨. 그러나 이전 key 자체가 원본과 다르므로 호환성 깨짐 상태였음 → 원본 일치 정정이 옳음.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: Phase A-11 (WallHead 4건 — A-11a `wallHeadJump=true` ✅ + A-11b `wallHeadJumpVerticalFactor=0.3F` ✅ + A-11c `wallHeadJumpHorizontalFactor=0.15F` ★ ✅ + **A-11d IO key '.distance' 누락 정정** `move.jump.wall.head.fall.maximum` → `move.jump.wall.head.fall.maximum.distance`). 원본 L300-L303. **Phase A 완결 임박**.

진행률: Phase A 48/~50 (~96%), 전체 #2.5 48/~110 (~43.6%).

### 세션 11 — 2026-04-25 — Phase A-11 (WallHead 4건: 확인 3 + IO key 정정 1) + Phase A 완결

사용자 지시: "엄격 1:1" 유지 + Phase A-11 4건 검증 + 정정 + Phase A 완결 마킹.

진행한 작업:
1. 원본 라인 + 기본값 확보 (`SmartMovingConfig.java` L300-L303):
   - L300 `_wallHeadJump = Unmodified("move.jump.wall.head")` → 기본 true
   - L301 `_wallHeadJumpVerticalFactor = DecreasingFactor("move.jump.wall.head.vertical.factor").defaults(0.3F)` → 0.3F
   - L302 `_wallHeadJumpHorizontalFactor = DecreasingFactor("move.jump.wall.head.horizontal.factor").defaults(0.15F)` ★ → 0.15F
   - L303 `_wallHeadJumpFallMaximumDistance = Positive("move.jump.wall.head.fall.maximum.distance").defaults(3F).min(_wallUpJumpFallMaximumDistance)` → 3F (key 끝 `.distance`)
2. 1.21.1 이식 위치 확인:
   - A-11a 필드 L222 = true ✅ + IO L1081 ✅ + save L1240 ✅
   - A-11b 필드 L234 = 0.3F ✅ + IO L1086 ✅ + save L1245 ✅
   - A-11c 필드 L238 = 0.15F ✅ + IO L1088 ✅ + save L1247 ✅ (§3 "미이식 추가" 표기는 오기)
   - A-11d 필드 L226 = 3F ✅ + **IO key 오역**: `move.jump.wall.head.fall.maximum` (suffix `.distance` 누락). save L1242 동일.
3. 1.21.1 정정 (`src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java`):
   - load L1083 key: `move.jump.wall.head.fall.maximum` → `move.jump.wall.head.fall.maximum.distance`
   - save L1242 key: 동일 정정
4. Phase A-12 (Properties IO 등록) 사후 검증: 세션 1~11 진행 중 매 원자마다 IO 동시 등록 — 누락 0. [x] 마킹.
5. **Phase A 완결**: A-1 ~ A-12 전수 [x]. 약 52 원자 (필드 50건 + IO 등록 1건 + key 정정 2건).
6. 근사 여부: 없음. 1:1.

완료 전 검증 체크리스트 (세션 11 기준):
- [근거] 원본 라인 확보 — 로컬 `SmartMovingConfig.java` L300-L303 + Properties.java L181-L186 (Positive/DecreasingFactor 기본값)
- [근거] 1.21.1 이식 위치 확정 — 필드 L222-L238 / load L1081-L1088 / save L1240-L1247
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (값/key 모두 일치 후)
- [분기] 분기 없음 (단순 boolean/float 4개)
- [상수] true / 0.3F / **0.15F** ★ / 3F — 모두 정확
- [타이밍] 필드 선언만. Phase B getJumpVertical/HorizontalFactor (WallHead 분기) + Phase D WallHead 분기에 사용.
- [근사] 근사 없음. §7 등록 없음.
- [신규] A-10 발견 사항 (A-11d 동일 오역) 해소.
- [회귀] IO key 변경 — A-10d 와 동일 영향 (이전 잘못된 key 무시, 원본 일치 정정).
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: **Phase B 진입**. B-1 `getJumpSpeed(isStanding, isSneaking, isRunning, isSprinting, angle)` 헬퍼 신설. 원본 `SmartMovingSelf.java` L2148-L2163 (1:1 번역). 위치: `SmartMovingConfig.java` 또는 `SmartMovingJumper.java` 정적 메서드 (전자 권장 — 원본 ClientConfig 위치).

진행률: **Phase A 완결 (~52 원자)**, 전체 #2.5 52/~110 (~47.3%) — 약 절반 통과.

### 세션 12 — 2026-04-25 — Phase B-1 + B-2 (getJumpSpeed 헬퍼 + Speed 상수 5건)

사용자 지시: "엄격 1:1" 유지 + Phase B 진입, B-1 + B-2 묶어 이식.

진행한 작업:
1. 원본 라인 + 본체 확보:
   - `SmartMovingClientConfig.java` L172-L176 — Speed 상수 5개 (Sprinting=0, Running=1, Walking=2, Sneaking=3, Standing=4)
   - `SmartMovingSelf.java` L2148-L2163 — `getJumpSpeed(isStanding, isSneaking, isRunning, isSprinting, Float angle)` 정적 헬퍼
2. 1.21.1 의존 grep 확인:
   - `sm.isStanding` ✅ (`SmartMovingClientState.java` L137)
   - `sm.isSlow` ✅ (sneaking 의미, L121)
   - `sm.isFast` ✅ (sprinting 의미, L128)
   - `sm.isRunning(player)` 메서드는 Phase D-3 (`isRunningOrNull` 분기) 진입 시 별도 확인.
3. 1.21.1 이식 (`src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java` L21-L74, SM_VERSION 직후):
   - 그룹 헤더 주석 (Phase B 인프라)
   - `SPEED_SPRINTING/RUNNING/WALKING/SNEAKING/STANDING = 0~4` (B-2)
   - `public static int getJumpSpeed(...)` (B-1) — 원본 L2148-L2163 1:1
4. 명명: `Sprinting/Running/...` → `SPEED_*` (Java 컨벤션, 표면 매핑 허용. 값/로직 1:1 유지).
5. 가시성: 원본 `private static` → 1.21.1 `public static` (Jumper 에서 정적 호출 필요).
6. 근사 여부: 없음. 1:1 (`isSprinting &= angle == null`, `isRunning &= angle == null`, 5-way if/else 순서 모두 보존).

완료 전 검증 체크리스트 (세션 12 기준):
- [근거] 원본 라인 확보 — `SmartMovingSelf.java` L2148-L2163 + `SmartMovingClientConfig.java` L172-L176
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingConfig.java` L21-L74
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (값/연산자/순서 모두 일치)
- [분기] 5-way if/else 전수 + `&=` 게이팅 2건 모두 이식
- [상수] 0/1/2/3/4 정확 반영
- [타이밍] 정적 헬퍼 — 호출 타이밍 없음. Phase D-4 에서 호출.
- [근사] 근사 없음. §7 등록 없음.
- [신규] 의존 발견 없음 — `sm.isRunning(player)` 메서드 확인은 Phase D-3 미루기.
- [회귀] 신규 정적 상수/메서드 추가 — 기존 코드 영향 0.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: Phase B-3 (`isJumpingEnabled(speed, type)` 신설). 원본 `SmartMovingClientConfig.java` L194-L227 — 11 type 분기 + 5 speed 분기 전수 이식. 의존 추가 — Jump Type 상수 (Up=0~WallHeadSlide=14, 15개) 도 같이 이식 필요 (B-3 의존). 별도 원자 B-2.5 (Type 상수) 추가 후 B-3 진행 권장.

진행률: Phase B 2/8 (B-1 + B-2 완료, ~25%), 전체 #2.5 54/~110 (~49.1%).

### 세션 13 — 2026-04-25 — Phase B-2.5 + B-3 (Jump Type 상수 15건 + isJumpingEnabled)

사용자 지시: "엄격 1:1" 유지 + Phase B-3 진입 + 의존 B-2.5 (Type 상수 15) 묶어 이식.

진행한 작업:
1. 원본 라인 + 본체 확보:
   - `SmartMovingClientConfig.java` L178-L192 — Jump Type 상수 15개 (Up=0 ~ WallHeadSlide=14)
   - `SmartMovingClientConfig.java` L194-L227 — `isJumpingEnabled(speed, type)` instance 메서드
2. 1.21.1 의존 grep 확인:
   - `enabled` ✅ (L692)
   - `slide` ✅ (L675)
   - `jumpCharge` / `climbUpJump` / `climbBackUpJump` / `climbBackHeadJump` / `wallUpJump` /
     `wallHeadJump` / `sprintJump` / `runJump` / `walkJump` / `sneakJump` / `standJump` —
     모두 Phase A 에서 추가 ✅
3. 1.21.1 이식 (`src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java`):
   - B-2.5 Type 상수 15개 — Speed 상수 직후 Phase B 인프라 그룹 내
   - B-3 isJumpingEnabled instance 메서드 — getJumpSpeed 직후 (Phase B 인프라 그룹)
4. 명명: `Up/ChargeUp/...` → `JUMP_TYPE_UP/JUMP_TYPE_CHARGE_UP/...` (Java 컨벤션, 표면 매핑).
5. 가시성: 원본 `public boolean isJumpingEnabled` → 1.21.1 `public boolean` (instance 메서드 — `enabled`, `jumpCharge` 등 instance 필드 의존).
6. 분기 순서 보존:
   - `!enabled → return true` (SM 비활성 = vanilla 위임)
   - Type 우선 (ChargeUp / SlideDown / ClimbUp* / ClimbBackUp* / ClimbBackHead* / WallUp / WallHead) 7 분기
   - Speed 5 분기 (Sprint/Run/Walk/Sneak/Stand)
   - fallthrough `return true` (Up/HeadUp/Angle/WallUpSlide/WallHeadSlide & speed 미통과 시)
7. 근사 여부: 없음. 1:1 (분기 / 순서 / 반환값 모두 보존).

완료 전 검증 체크리스트 (세션 13 기준):
- [근거] 원본 라인 확보 — `SmartMovingClientConfig.java` L178-L192 + L194-L227
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingConfig.java` Phase B 인프라 그룹 (SPEED_* 다음 Type 상수, getJumpSpeed 다음 isJumpingEnabled)
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (값/분기/순서/반환 모두 일치)
- [분기] 14개 분기 전수 (`!enabled` + Type 7 + Speed 5 + fallthrough)
- [상수] 0~14 정확 반영
- [타이밍] instance 메서드 — Phase D-4 호출. 호출 타이밍 변경 없음.
- [근사] 근사 없음. §7 등록 없음.
- [신규] 추가 의존 없음 (모든 sub-jump boolean Phase A 완료).
- [회귀] 신규 정적 상수 + instance 메서드 추가 — 기존 코드 영향 0.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: Phase B-4 `getJumpHorizontalFactor(speed, type)` 신설 (원본 `SmartMovingClientConfig.java` L465-L505). `!enabled` 시 `speed==Running ? 2F : 1F`, `enabled` 시 base × type × speed 분기. L501 `speed==Standing && type!=ClimbBack*` → `*0F` 특수 처리 포함.

진행률: Phase B 4/9 (B-1 + B-2 + B-2.5 + B-3 완료, ~44%), 전체 #2.5 70/~110 (~63.6%) — Type 상수 15건 + isJumpingEnabled 본체 합산.

### 세션 14 — 2026-04-25 — Phase B-4 (getJumpHorizontalFactor)

사용자 지시: "엄격 1:1" 유지 + Phase B-4 이식.

진행한 작업:
1. 원본 라인 + 본체 확보 (`SmartMovingClientConfig.java` L465-L505):
   - L467-L468: `!enabled` 조기 분기 (`speed==Running ? 2F : 1F`)
   - L470: `result = _jumpHorizontalFactor.value` (base)
   - L472-L473: type==Angle → ×= angleJumpHorizontalFactor
   - L475-L476: type==ClimbBackUp || ClimbBackUpHandsOnly → ×= climbBackUpJumpHorizontalFactor
   - L477-L478: type==ClimbBackUpHandsOnly → ×= climbBackUpJumpHandsOnlyHorizontalFactor (추가)
   - L480-L481: type==ClimbBackHead || ClimbBackHeadHandsOnly → ×= climbBackHeadJumpHorizontalFactor
   - L482-L483: type==ClimbBackHeadHandsOnly → ×= climbBackHeadJumpHandsOnlyHorizontalFactor (추가)
   - L485-L488: WallUp/WallHead 각각 ×= wallUp/Head HorizontalFactor
   - L490-L491: 9 type (Angle/ClimbUp×2/ClimbBackUp×2/ClimbBackHead×2/WallUp/WallHead) → return result (early)
   - L493-L500: speed 분기 (Sprint/Run/Walk/Sneak)
   - L501-L502: speed==Standing && type ∉ ClimbBack 4종 → ×= 0F (※ Climb 4종은 위 early return 통과)
   - L504: return result
2. 1.21.1 의존 grep 확인:
   - 모든 factor 필드 (jumpHorizontalFactor / angle / climbBack* / wall*  / sprint/run/walk/sneak Jump 별 H factor) Phase A 에서 추가 ✅
   - `enabled` ✅ (L692)
3. 1.21.1 이식 (`SmartMovingConfig.java`, isJumpingEnabled 직후):
   - instance 메서드 `public float getJumpHorizontalFactor(int speed, int type)` 신설
   - 원본 L465-L505 절대 순서 보존 (1:1)
   - 표면 매핑: 상수명 `Angle/Sprinting/...` → `JUMP_TYPE_ANGLE/SPEED_SPRINTING/...`
   - 필드명 `_xxxFactor.value` → `xxxFactor`
4. 근사 여부: 없음. 1:1.

완료 전 검증 체크리스트 (세션 14 기준):
- [근거] 원본 라인 확보 — `SmartMovingClientConfig.java` L465-L505 (전체)
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingConfig.java` Phase B 인프라 그룹 (isJumpingEnabled 직후)
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (분기/순서/연산자/early return 모두 일치)
- [분기] 9 if 분기 + 1 early return + 5-way speed 분기 + 1 Standing 특수 — 전수 이식
- [상수] 0F 곱셈 (L501) 정확 반영, base 1F 의존 (jumpHorizontalFactor)
- [타이밍] 정적 헬퍼 — Phase D-6 호출 (`horizontalJumpFactor = cfg.getJumpHorizontalFactor(...) * jumpFactor`).
- [근사] 근사 없음. §7 등록 없음.
- [신규] 추가 의존 없음.
- [회귀] 신규 instance 메서드 추가 — 기존 코드 영향 0.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: Phase B-5 `getJumpVerticalFactor(speed, type)` 신설 (원본 `SmartMovingClientConfig.java` L418-L463). 구조는 B-4 와 유사 — base × type 분기 (Angle/ClimbUp×2/ClimbBackUp×2/ClimbBackHead×2/WallUp/WallHead) × speed 분기.

진행률: Phase B 5/9 (~56%), 전체 #2.5 71/~110 (~64.5%).

### 세션 15 — 2026-04-25 — Phase B-5 (getJumpVerticalFactor)

사용자 지시: "엄격 1:1" 유지 + Phase B-5 이식.

진행한 작업:
1. 원본 라인 + 본체 확보 (`SmartMovingClientConfig.java` L418-L463):
   - L420-L421: `!enabled` → return 1F
   - L423: base = jumpVerticalFactor
   - L425-L426: type==Angle → **return result × angleJumpVerticalFactor (즉시 early return)**
   - L428-L431: ClimbUp/ClimbUpHandsOnly + ClimbUpHandsOnly 추가 — climbUp 그룹
   - L433-L436: ClimbBackUp 그룹 (동일 패턴)
   - L438-L441: ClimbBackHead 그룹 (동일 패턴)
   - L443-L446: WallUp/WallHead → ×= wallUp + WallHead 추가 → **WallHead = base × wallUp × wallHead 누적**
   - L448-L449: 9 type (Angle 포함) early return — Angle 은 위에서 이미 처리됨
   - L451-L460: speed 분기 (Sprint/Run/Walk/Sneak/Stand) — Standing 도 정상 곱셈 (B-4 와 달리 0F 특수 없음)
2. 1.21.1 의존 grep 확인:
   - 모든 V factor 필드 (jump/angle/climbUp×2/climbBackUp×2/climbBackHead×2/wall×2/sprint/run/walk/sneak/stand Jump V) Phase A 에서 추가 ✅
3. 1.21.1 이식 (`SmartMovingConfig.java`, getJumpHorizontalFactor 직전):
   - instance 메서드 신설 (Phase B 인프라 그룹)
   - 원본 L418-L463 절대 순서 보존 (1:1)
4. 근사 여부: 없음. 1:1.

WallHead 특이점 (현 1.21.1 주석에도 명시됨):
- `verticalMotion = -0.078 + 0.498 × (wallUpJumpVerticalFactor + wallHeadJumpVerticalFactor)` — 이는 tryJump 출구 공식 (Phase D)
- getJumpVerticalFactor 단계에서는 `result = base × wallUp × wallHead` (곱셈 누적). 별개 공식.

완료 전 검증 체크리스트 (세션 15 기준):
- [근거] 원본 라인 확보 — `SmartMovingClientConfig.java` L418-L463 (전체)
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingConfig.java` Phase B 인프라 그룹 (getJumpHorizontalFactor 직전)
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (분기/순서/연산자/early return/누적 곱셈 모두 일치)
- [분기] `!enabled` + Angle early return + 8 type-pair if + 9 type early return + 5-way speed — 전수
- [상수] base 1F (jumpVerticalFactor) + 모든 V factor 곱셈
- [타이밍] instance 메서드 — Phase D-6 호출 (`verticalJumpFactor = cfg.getJumpVerticalFactor(...) * jumpFactor`).
- [근사] 근사 없음. §7 등록 없음.
- [신규] 추가 의존 없음.
- [회귀] 신규 instance 메서드 추가 — 기존 코드 영향 0.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: Phase B-6 `getMaxHorizontalMotion(speed, type, inWater)` 신설 (원본 L508-L525). baseMaxMotion = 0.117852041920949F (육상) / 0.07839602977037292F (수중) × speed 분기 (Sprint→sprintFactor / Run→runFactor / Sneak→sneakFactor). `!enabled` 시 speed==Running → baseMaxMotion × 1.3F.

진행률: Phase B 6/9 (~67%), 전체 #2.5 72/~110 (~65.5%).

### 세션 16 — 2026-04-25 — Phase B-6 (getMaxHorizontalMotion)

사용자 지시: "엄격 1:1" 유지 + Phase B-6 이식.

진행한 작업:
1. 원본 라인 + 본체 확보 (`SmartMovingClientConfig.java` L507-L525):
   - L510: `maxMotion = 0.117852041920949F` (육상 base)
   - L511-L512: `!enabled` → return Running ? `maxMotion×1.3F` : maxMotion
   - L514-L515: `inWater` → `maxMotion = 0.07839602977037292F` (수중 덮어쓰기)
   - L517-L522: speed 분기 (Sprint/Run/Sneak — Walking/Standing 분기 없음)
   - L507 `@SuppressWarnings("unused")` — type 파라미터 미사용 (시그니처 호환만)
2. 1.21.1 의존 grep 확인:
   - `sprintFactor` ✅ (L354 = 1.5F) / `runFactor` ✅ (L353 = 1.3F) / `sneakFactor` ✅ (L351 = 0.3F)
3. 1.21.1 이식 (`SmartMovingConfig.java`, getJumpVerticalFactor 직전):
   - instance 메서드 신설
   - 원본 L508-L525 1:1 (특수 상수 0.117852041920949F / 0.07839602977037292F 정확 보존)
   - `@SuppressWarnings("unused")` 주석 정확 이식
4. 근사 여부: 없음. 1:1 (특수 상수 그대로, 분기 순서 보존).

완료 전 검증 체크리스트 (세션 16 기준):
- [근거] 원본 라인 확보 — `SmartMovingClientConfig.java` L507-L525
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingConfig.java` Phase B 인프라 (getJumpVerticalFactor 직전)
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (특수 상수 / 분기 / 순서 모두 일치)
- [분기] `!enabled` 조기 + `inWater` 덮어쓰기 + 3-way speed 분기 — 전수
- [상수] 0.117852041920949F (육상) / 0.07839602977037292F (수중) / 1.3F — 정확 보존
- [타이밍] instance 메서드 — Phase D-8 호출 (`maxHorizontalMotion = ... * SmartMovingMover.getCombinedSpeedFactor(...)`).
- [근사] 근사 없음. §7 등록 없음.
- [신규] 추가 의존 없음.
- [회귀] 신규 instance 메서드 추가 — 기존 코드 영향 0.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (7s)

다음 세션 권고: Phase B-7 + B-8 (`getJumpChargeFactor(charge)` + `getHeadJumpFactor(charge)`) — 묶어 처리. 원본 L401-L408 (`getJumpChargeFactor`) + L410-L416 (`getHeadJumpFactor`). 둘 다 단순 (charge / max) 보간 공식. 기존 Jumper L180-L182 에 동등 계산 있음 — 이식 후 Jumper 호출 교체는 Phase D 에서.

진행률: Phase B 7/9 (~78%), 전체 #2.5 73/~110 (~66.4%).

### 세션 17 — 2026-04-25 — Phase B-7 + B-8 (getJumpChargeFactor + getHeadJumpFactor) — Phase B 완결

사용자 지시: "엄격 1:1" 유지 + Phase B-7 + B-8 묶어 이식 → Phase B 완결.

진행한 작업:
1. 원본 라인 + 본체 확보:
   - `SmartMovingClientConfig.java` L401-L408 — `getJumpChargeFactor(float jumpCharge)`
   - `SmartMovingClientConfig.java` L410-L416 — `getHeadJumpFactor(float headJumpCharge)`
2. 1.21.1 의존 grep 확인:
   - B-7: `enabled` ✅ / `jumpCharge` boolean (A-4a) ✅ / `jumpChargeMaximum` (A-4b) ✅ / `jumpChargeFactor` (A-4c) ✅
   - B-8: `enabled` ✅ / `headJump` (A-5a) ✅ / `headJumpChargeMaximum` (A-5c) ✅
3. 1.21.1 이식 (`SmartMovingConfig.java`, getJumpVerticalFactor 직전):
   - `public float getJumpChargeFactor(float jumpCharge)` instance 메서드
   - `public float getHeadJumpFactor(float headJumpCharge)` instance 메서드
   - 원본 공식 그대로:
     - B-7: `1F + jumpCharge / jumpChargeMaximum * (jumpChargeFactor - 1F)` (선형 보간 0→1F, max→factor)
     - B-8: `(headJumpCharge - 1) / (headJumpChargeMaximum - 1)` (charge=1→0, max→1)
   - `Math.min(charge, max)` 클램프 보존
4. 명명 충돌 처리: 인자 `jumpCharge` (float) vs 필드 `jumpCharge` (boolean) 동명 → `this.jumpCharge` 명시 / `headJump` 동일 처리.
5. 근사 여부: 없음. 1:1 (공식/클램프/조기반환 모두 보존).

완료 전 검증 체크리스트 (세션 17 기준):
- [근거] 원본 라인 확보 — `SmartMovingClientConfig.java` L401-L416
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingConfig.java` Phase B 인프라 그룹 (getJumpVerticalFactor 직전)
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (공식/클램프/조기반환 모두 일치)
- [분기] `!enabled || !jumpCharge` (B-7) / `!enabled || !headJump` (B-8) 조기 반환 1F
- [상수] `1F` 보간 base / `(charge - 1) / (max - 1)` 공식
- [타이밍] instance 메서드 — Phase D-6 (B-7) / Phase D-10 (B-8) 호출.
- [근사] 근사 없음. §7 등록 없음.
- [신규] 명명 충돌 발견: 인자 vs 필드 동명 — `this.` 명시로 해소.
- [회귀] 신규 instance 메서드 2건 추가 — 기존 코드 영향 0. 기존 Jumper L180-L182 동등 계산은 Phase D 진입 시 본 헬퍼 호출로 교체 예정.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (5s)

**Phase B 완결**: B-1 + B-2 + B-2.5 + B-3 + B-4 + B-5 + B-6 + B-7 + B-8 = 9 원자 모두 [x]. Speed 상수 5 + Type 상수 15 + 6 메서드 (getJumpSpeed / isJumpingEnabled / getJumpHorizontalFactor / getJumpVerticalFactor / getMaxHorizontalMotion / getJumpChargeFactor / getHeadJumpFactor) = 7 메서드. tryJump 재작성 (Phase D) 의존 인프라 100% 충족.

다음 세션 권고: **Phase C 진입** — Exhaustion 시스템 대규모 이식. C-1a (Jump exhaustion 활성/종료 필드 — `_jumpExhaustion` + speed 5 + type 11) 부터. 원본 `SmartMovingConfig.java` L323+ (jumpExhaustion 필드) 참조. 또는 Phase D 우선 진행 옵션 검토 (Phase C 가 D-5 의존이지만 D 의 다른 17 서브 원자는 독립).

**대안 진입**: Phase D 직접 진입 + Phase C 병행 — D-5 만 임시 stub 으로 두고 (계획 §7 근사로 임시 등록) 나머지 D 원자 진행 후 C 완결 시 D-5 복구. 단, 사용자 룰 "Phase C skip 금지 — Phase D 이전에 전수 이식" 명시 → 정공법 (Phase C 먼저).

진행률: **Phase B 완결** (9/9 = 100%), 전체 #2.5 75/~110 (~68.2%).

### 세션 18 — 2026-04-25 — Phase C skip 결정 (Easy 1:1 + focus_05 일관)

사용자 지시: Phase C 진입 전 jumpExhaustion 시스템 검토. "Easy 모드만 쓴다는 전제 +
다른 포커스 (focus_05) 의 명시적 배제 결정과 일관 유지" 메타 전제 도출 → Phase C 전체 skip.

진행한 작업:
1. 포커스 파일 전수 grep — `exhaustion|Exhaustion|허기|hunger` (5개 파일 매칭)
2. 두 시스템 분리 확인:
   - **Base Exhaustion + Hunger Sync** (focus_05 H 시리즈, 세션 23 완료) — 매 틱 자연 소진 / 자연 회복
   - **Jump Exhaustion** (Phase C, 본 포커스) — tryJump 시점 게이팅 + type-specific 누적
   - 두 시스템은 같은 `exhaustion` 변수 공유하지만 메커니즘 분리
3. 원본 grep — `maxExhaustionToStartAction|maxExhaustionForAction|isJumpExhaustionEnabled|getJumpExhaustionGain|getJumpExhaustionStop|getMaxExhaustion`
   - tryJump (D-5 + D-14): jumpExhaustion 호출
   - HUD `SmartMovingRender` L239-L244 + `getMaxExhaustion()` (Render L30): 점프 피로 시각화
   - 다른 액션 (Climbing/CeilingClimbing/Running/Sprinting): 같은 maxExhaustion* 변수 공유
4. 1.21.1 SmartMovingHud L61 검증: `cfg.climbExhaustionStop` 직접 사용 (climbing 한정).
   `getMaxExhaustion()` 호출처 0건 → Phase C 의 HUD 의존 0
5. focus_05 §6.5 P-9~P-19 + 685-700 줄 검증: "14종 점프 피로 전체" 명시적 배제 선례

Easy default 영향 분석:
- 모든 jumpExhaustion=false (Boolean 17개) → `isJumpExhaustionEnabled` 항상 false
- D-5 안쪽 6줄 + D-14 안쪽 3줄 모두 100% dead
- HUD 피로도 바: max=0 라 표시 안 됨 (어차피 Easy 에서 안 보임)
- 사용자 체감 영향: 0건

문서 갱신:
1. §0 미이식 Exhaustion 시스템 항목 → "skip 결정" 표기 + 근거 + 미이식 항목 목록
2. §3 Phase C 전체 → `[~]` (skip 결정) 표기. C-2a/C-5 는 `[x]` (focus_05 H-8/H-7 이미 이식)
3. §4 의존 그래프 → Phase C 제거 (A → B → D → E → F)
4. §7-1 근사 등록: D-5 + D-14 통합 근사. 원본 코드 + 1.21.1 처리 + Easy 영향 분석 +
   사용자 수동 활성화 영향 + 미래 작업 시 추가 필요 항목 + 근거 + 해소 조건 전수 기록
5. 본 세션 18 로그 추가

근사 여부: §7-1 등록 (1건). 이 근사는 사용자 메타 결정에 따른 영구 등록 — 포커스 완결 후에도 유지.

완료 전 검증 체크리스트 (세션 18 기준):
- [근거] 원본 grep 전수 — `SmartMovingClientConfig.java` L244-L399 + `SmartMovingSelf.java` L2018-L2027 + L2120-L2124 + `SmartMovingClient.java` L30 + `SmartMovingRender.java` L239-L244
- [근거] focus_05 §6.5 P-9~P-19 dead 분석 + 685-700 줄 명시 배제 인용
- [근거] 1.21.1 SmartMovingHud L61 (`cfg.climbExhaustionStop`) 직접 검증
- [대응] 코드 변경 0건 — 문서 정리만
- [분기] N/A (skip 결정)
- [상수] N/A
- [타이밍] N/A
- [근사] §7-1 등록 + 주석 (원본 코드 + Easy 영향 + 미해소 명시)
- [신규] §0/§3/§4/§7 동시 갱신
- [회귀] 코드 변경 0 → 회귀 0
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (변경 없음 검증)

다음 세션 권고: **Phase D 진입** — D-1 (시그니처 정비) 부터. `tryJump(int type, Boolean inWaterOrNull, Boolean isRunningOrNull, Float angle)` 원본 시그니처 1:1 이식. 호출처 정비는 Phase E.

진행률: **Phase A + Phase B 완결, Phase C skip 결정**, 전체 #2.5 76/~80 (~95%) — Phase C 32개 원자 제거로 분모 감소. 실질 남은 작업 Phase D (16 서브 — D-5/D-14 skip) + Phase E (5) + Phase F (6) = 27 원자.

### 세션 19 — 2026-04-25 — Phase D 일괄 + E-1 통합 (D-5/D-14 skip)

사용자 지시: "Phase D 진입" — Phase C skip 결정 직후. D-1~D-18 단일 함수 분할 불가 →
일괄 진행 결정. D-5/D-14 skip (§7-1 적용). E-1 (trySlideDownJump 통합) 같이 처리.

진행한 작업:
1. **의존 grep 검증**:
   - `sm.jumpMotionX/Z` ✅ (ClientState L595/L597)
   - `sm.jumpCharge` (float, L42) / `sm.headJumpCharge` (float, L45) ✅
   - `sm.isStanding` ✅ (L137)
   - `sm.vanilla()` ⚠️ private (L2274) → **public 변경** (D-9 외부 호출 위해)
   - `sm.isRunning(player)` ✅ public (L2322)
   - `SmartMovingMover.getCombinedSpeedFactor` ✅ (Climber/Flyer 사용)
   - `sm.heightOffset` ✅ (L106)
   - Phase B 메서드 모두 ✅ (B-1~B-8 세션 12-17 완료)
   - Phase C — skip 결정으로 의존 제거
2. **상수 충돌 발견**:
   - 기존 `SmartMovingJumper.WALL_UP=3` vs 새 `SmartMovingConfig.JUMP_TYPE_WALL_UP=11`
   - 외부 사용처 1곳 (`ClientState L1349`) — 호환성 유지
   - 해결: `SmartMovingJumper` 상수를 `SmartMovingConfig.JUMP_TYPE_*` alias 로 통일
3. **`SmartMovingClientState.vanilla()` private → public** (D-9 의존)
4. **`SmartMovingJumper.tryJump` 새 시그니처 본체 재작성** (~150줄):
   - 새: `boolean tryJump(player, sm, int type, Boolean inWaterOrNull, Boolean isRunningOrNull, Float angle)`
   - D-2: WallUpSlide/WallHeadSlide → noVertical 변환
   - D-3: 지역 변수 (inWater, isRunning, charged, up, head)
   - D-4: getJumpSpeed + isJumpingEnabled
   - **D-5: SKIP** (§7-1 — jumpExhaustion 게이트, 안쪽 6줄 dead)
   - D-6: jumpFactor (potion) + h/v factor + jumpChargeFactor
   - D-7: !up sqrt 변환
   - D-8: maxHorizontalMotion + verticalMotion 초기 + getCombinedSpeedFactor
   - D-9: Up && vanilla 분기 (verticalMotion = 0.41999... + sprint 보정)
   - D-10: head 재계산 (getHeadJumpFactor)
   - D-11: angle != null (getJumpMoving via wallUp/Head H factor)
   - D-12: horizontalMotion > 0 스케일
   - D-13: up && !noVertical → motionY + Stats.JUMP + isSprintJump
   - **D-14: SKIP** (§7-1 — exhaustion 누적, 안쪽 3줄 dead)
   - D-15: head → isHeadJumping + setPoseSmall + heightOffset=-1F
   - D-16: setVelocity + isJumping
   - D-17: return enabled
   - D-18: 1.21.1 동작 유지 — 내부 상태 클리어 (jumpCharge=0 등)
5. **`trySlideDownJump` 메서드 제거** (E-1 통합)
6. **호출처 7곳 갱신**:
   - SmartMovingJumper 내부 5곳:
     - handleJumping CHARGE_UP (L412): `tryJump(player, sm, CHARGE_UP, null, null, null)`
     - handleJumping HEAD_UP (L434): 동일 패턴
     - handleJumping 수면 UP (L449): 동일
     - handleJumping 일반 UP (L470): 동일
     - handleWallJumping (L620): `tryJump(player, sm, jumpType, null, null, jumpAngle)` — angle 전달
   - SmartMovingClientState 외부 2곳:
     - L1349 Creative flying UP: `tryJump(player, this, UP, null, null, null)`
     - L1394 SlideDown: `tryJump(player, this, SLIDE_DOWN, false, wasRunning, null)` (trySlideDownJump 대체)
7. **상수 alias 통일**: SmartMovingJumper.UP/CHARGE_UP/... → SmartMovingConfig.JUMP_TYPE_*
   값 정렬 (예: WALL_UP 3→11). LEFT/RIGHT/BACK 은 deprecated sentinel (Phase E-4 후속).
8. **handleWallJumping 사전 setVelocity/horizontalCollision/fallDistance 리셋 코드 제거**:
   - tryJump 내부 D-9~D-12 가 동등 처리 (원본 SmartMovingSelf L2068+ 1:1)
   - 사전 인라인 코드는 중복 — 제거

근사 여부: §7-1 (D-5/D-14 skip) 1건 — 세션 18 등록분 적용 완료. 신규 근사 0.

완료 전 검증 체크리스트 (세션 19 기준):
- [근거] 원본 라인 확보 — `SmartMovingSelf.java` L1999-L2136 (전체 138줄) + Phase B 메서드 호출 매핑
- [근거] 1.21.1 이식 위치 확정 — `SmartMovingJumper.java` 새 tryJump (L130-L300) + 호출처 7곳
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (D-5/D-14 skip 외 모든 라인 1:1, 원본 L 주석 명시)
- [분기] 모든 if/else 전수 (D-2 type 변환 + D-3 up/head 12종 + D-9 Up && vanilla + D-10 head + D-11 angle != null + D-12 maxHorizontalMotion null + D-13 up && !noVertical + D-15 head)
- [상수] 0.41999998688697815D / 0.498 / -0.078 / 0.017453292F (yaw deg→rad) / 57.295776F (180/PI) / 0.2F (sprint 보정) — 모두 정확
- [타이밍] 절대 순서 보존 (D-2 → D-3 → D-4 → [D-5 skip] → D-6 → ... → D-17 → D-18)
- [근사] §7-1 (세션 18 등록) 적용. 신규 0.
- [신규] vanilla() public 변경 발견 → 처리. 상수 충돌 발견 → alias 처리.
- [회귀] 호출처 7곳 + 상수 매핑 변경 — 빌드 통과로 컴파일 회귀 0. 동작 회귀는 사용자 인게임 검증 (Phase F-6).
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: **Phase E 진입** — E-2 (호출처 재검토 — 일부 세션 19 진행, ANGLE/CLIMB/HandsOnly 분기 잔여) + E-3 (HandsOnly 분기 추가) + E-4 (Angle 점프 LEFT/RIGHT/BACK → ANGLE type 통합) + E-5 (skip — Phase C 와 함께). 또는 Phase F 직접 진입 (감사 + 인게임 플레이테스트).

진행률: **Phase A 완결 + Phase B 완결 + Phase C skip + Phase D 일괄 완결 + E-1 통합**, 전체 #2.5 92/~80 (~115%) — Phase C 32 제거 + Phase D 16 추가 = 분모 갱신 필요. 실질 남은: Phase E (E-2/E-3/E-4 = 3 원자) + Phase F (5 — F-6 사용자) = 8 원자.

### 세션 20 — 2026-04-25 — Phase E 완결 (E-2/E-3 N/A + E-4 통합 + E-5 skip)

사용자 지시: Phase E 잔여 진행 (D 직후 자연 흐름).

진행한 작업:
1. **E-2 호출처 재검토**: grep 결과 7곳 모두 세션 19 갱신 완료. CLIMB_UP/CLIMB_BACK/
   CLIMB_BACK_HEAD 호출처 0건 (1.21.1 climb-jump 미이식). 추가 호출처 0 → N/A.
2. **E-3 HandsOnly 분기**: 1.21.1 SmartMovingClimber 에 climb-jump 호출 자체 0 → 분기 추가
   대상 없음. N/A. 미래 climb-jump 이식 시 별도 포커스 처리.
3. **E-4 ANGLE 통합**: handleJumping L329-L390 인라인 더블클릭 방향 점프 코드 (~60줄) →
   `tryJump(ANGLE, null, null, worldAngleDeg)` 단일 호출 통합. 새 tryJump 의 D-11 분기가
   수평 + 수직 + 스프린트 + Stats + 상태 클리어 모두 처리.
4. **버그 수정 (E-4 부산물)**: 기존 인라인 코드의 vanilla Up 0.41999... 수직 속도 → ANGLE type
   의 angleJumpVerticalFactor=0.2F 기반 (D-8) 으로 1:1 정정. 사이드/백 점프 수직 속도 약 5분의 1
   감소 (의도된 원본 동작). 사용자 인게임 체감 변화 가능.
5. **LEFT/RIGHT/BACK sentinel 정리**: SmartMovingJumper 의 `LEFT=-1, RIGHT=-2, BACK=-3`
   sentinel 상수 사용처 0 (E-4 통합으로 마지막 사용처 인라인 코드 제거) → 삭제.
6. **E-5 skip 마킹**: Phase C skip 결정과 함께 maxExhaustion* 변수 자체 미존재 → 리셋 대상 0.

근사 여부: 신규 0. E-4 의 verticalMotion 수정은 원본 1:1 복원 (근사 해소).

완료 전 검증 체크리스트 (세션 20 기준):
- [근거] 원본 라인 — `SmartMovingSelf.tryJump` ANGLE type 분기 + Phase D 새 본체 D-11
- [근거] 1.21.1 이식 위치 — `SmartMovingJumper.handleJumping` L350-L388 (E-4 통합)
- [대응] 원본 ↔ 1.21.1 1:1 — 새 tryJump 가 모든 처리 담당, 인라인 60줄 → tryJump 1줄 호출
- [분기] left/back 4 분기 (relAngle 270/225/90/135/180) + canAngleJump 6-AND 게이트 — 보존
- [상수] worldAngleDeg = (yaw + relAngle) % 360 — 보존
- [타이밍] tryJump 호출 후 leftJumpCount/rightJumpCount/backJumpCount 리셋 + return — 원본 순서
- [근사] 신규 0. E-4 부산물로 vanilla Up 수치 근사 해소 (원본 1:1 복원).
- [신규] verticalMotion 불일치 → E-4 통합 자동 정정. LEFT/RIGHT/BACK sentinel 0 사용 → 제거.
- [회귀] 빌드 통과 (컴파일 회귀 0). 사이드/백 점프 인게임 체감 변화 (의도, F-6 검증).
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: **Phase F 진입** (감사 + 플레이테스트).
- F-1 원본 tryJump L1999-L2136 side-by-side 감사 (AI 자동)
- F-2 getJumpHorizontalFactor L465-L505 감사 (AI 자동)
- F-3 getJumpVerticalFactor L418-L463 감사 (AI 자동)
- F-4 Exhaustion 감사 — Phase C skip 으로 N/A
- F-5 빌드 + 회귀 감사 (AI 자동)
- F-6 사용자 인게임 플레이테스트 (사용자 작업, 체크리스트 제공)

진행률: **Phase A + B 완결 + C skip + D 완결 + E 완결**, 전체 #2.5 95/110 (~86%). 남은: Phase F (5 — F-6 사용자).

### 세션 21 — 2026-04-25 — Phase F 진입 + F-1 회귀 정정 + F-2/F-3/F-5 완료

사용자 지시: Phase F 진입 — F-1~F-5 AI 자동 감사.

진행한 작업:
1. **F-1 원본 tryJump L1999-L2136 line-by-line 감사** → **회귀 버그 2건 발견**:
   - **회귀 #1 enabled 게이트 누락**: 원본 L2016 `if (enabled) { ... }` 가 D-5~D-16 전체를
     감싸지만 세션 19 1:1 재작성 시 누락. 사용자가 sub-jump 비활성 (예: `walkJump=false`)
     시에도 점프 setVelocity/Stats.JUMP/isJumping 모두 실행되는 회귀.
   - **회귀 #2 D-9 if/else 누락**: 원본 L2047-L2111 은 `if (Up && vanilla) {vanilla Up} else
     {head/angle/scale}` 구조. 세션 19 코드는 if/else 없이 4개 if 순차 실행 → vanilla Up
     점프 시 D-12 horizontalMotion>0 스케일이 추가 적용되어 **sprint 점프 수평 속도 2배 증폭**.
     원본 의도와 명백히 다른 동작 회귀.
2. **F-1 정정**:
   - D-5~D-16 전체를 `if (enabled) { ... }` 안으로 이동
   - D-9 if 에 else 추가, D-10/D-11/D-12 를 else 블록 안으로 묶음
   - D-18 (상태 클리어) 는 enabled 와 무관하게 실행 — 원본 호출측 처리 동작 유지
3. **F-2 getJumpHorizontalFactor 재확인**: 세션 14 line-by-line 이식 (L501 Standing/non-ClimbBack
   `*0F` 엣지 포함) + 세션 21 재검증 — 1:1 일치. 회귀 0.
4. **F-3 getJumpVerticalFactor 재확인**: 세션 15 line-by-line 이식 (Angle early return + WallHead
   누적 곱셈 + 9 type early return) + 세션 21 재검증 — 1:1 일치. 회귀 0.
5. **F-4 Exhaustion 감사**: Phase C skip 결정으로 N/A. §7-1 영구 등록.
6. **F-5 빌드 + 회귀 감사**:
   - `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)
   - `grep "// TODO\|// \[미확인\]\|// 아마\|// 추정"` in `src/` — **0건**
   - F-1 회귀 정정 후 빌드 통과
7. **F-6 사용자 인게임 플레이테스트 체크리스트 작성**: §3 F-6 항목에 9개 테스트 시나리오 명시.

근사 여부: 신규 0. F-1 회귀 정정으로 잠재 근사 2건 해소.

발견 + 정정 (회귀 가능 영역, 매우 중대)
- **회귀 #1**: 사용자가 sub-jump 비활성 시 점프 처리됨 → 정정 후 점프 자체 미작동 (원본 의도)
- **회귀 #2**: vanilla Up 점프 (handleJumping → tryJump UP) sprint 수평 속도 2배 증폭 버그
  → 정정 후 원본 1:1 (sprint 수평 보정 0.2F 만 적용, D-12 스케일 미적용)
- **사용자 인게임 영향**: 일반 sprint 점프 거리 약 50% 감소 (정정 전이 비정상). 원본 1.7.10
  체감 복원. F-6 검증 필수.

완료 전 검증 체크리스트 (세션 21 기준):
- [근거] 원본 라인 — `SmartMovingSelf.tryJump` L1999-L2136 (138줄) line-by-line read
- [근거] 1.21.1 이식 위치 — `SmartMovingJumper.tryJump` 새 본체 (세션 19 작성)
- [대응] 원본 ↔ 1.21.1 1:1 (F-1 정정 후) — enabled 게이트 + if/else 구조 모두 일치
- [분기] enabled 게이트 1 + D-9 if/else 1 — 회귀 정정으로 누락 해소
- [상수] 수정 없음 (회귀는 구조 차원)
- [타이밍] D-13 motionY 적용은 D-9 분기 외부 — 원본 L2113 도 외부. 정확.
- [근사] 신규 0. 회귀 정정으로 잠재 근사 해소.
- [신규] 회귀 2건 발견 → 즉시 정정. F-6 체크리스트 작성.
- [회귀] F-1 정정 자체가 회귀 정정. F-2/F-3/F-5 회귀 0.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (4s)

다음 세션 권고: **F-6 사용자 인게임 플레이테스트 대기**. 사용자가 §3 F-6 체크리스트 9 시나리오
   확인 후 보고. 이상 발견 시 재이식. 정상 시 **포커스 #2.5 완결** 마킹 + `playtest_fixes.md`
   다음 포커스 (#3 또는 #2.6/#2.7) 전환.

진행률: **Phase A + B 완결 + C skip + D 완결 + E 완결 + F (F-1~F-5) 완결**, 전체 #2.5 100/110
   (~91%). 남은: F-6 사용자 인게임 (1).

---

## 7. 근사 이식 지점 (이 포커스)

**§7 이 파일**: Phase 진행 중 불가피한 근사 이식 지점을 여기 등록. 포커스 완결 시 0건 목표.
현재 시작 시점: 0건. **세션 18 갱신: 1건 등록 (Phase C skip 결정에 따른 D-5/D-14 통합 근사)**.

### §7-1. Phase D-5 + D-14 jumpExhaustion 게이트/누적 미이식 — 세션 18

**원본 위치**: `SmartMovingSelf.java` L2018-L2027 (D-5) + L2120-L2124 (D-14)

**원본 코드**:
```java
// D-5 (L2018-L2027)
if (enabled) {
    boolean exhausionEnabled = Config.isJumpExhaustionEnabled(speed, type);
    if (exhausionEnabled) {
        float maxExhausionForJump = Config.getJumpExhaustionStop(speed, type, jumpCharge);
        if (exhaustion > maxExhausionForJump) return false;
        maxExhaustionToStartAction = Math.min(maxExhaustionToStartAction, maxExhausionForJump);
        maxExhaustionForAction = Math.min(maxExhaustionForAction,
            maxExhausionForJump + Config.getJumpExhaustionGain(speed, type, jumpCharge));
    }
    ...
}

// D-14 (L2120-L2124)
if (exhausionEnabled) {
    float exhaustionFromJump = Config.getJumpExhaustionGain(speed, type, jumpCharge);
    exhaustion += exhaustionFromJump;
}
```

**1.21.1 처리**: 두 블록 통째로 skip. `exhausionEnabled` 변수 자체 미선언. **세션 19 적용 완료** —
`SmartMovingJumper.tryJump` 새 본체에서 D-5/D-14 위치에 SKIP 주석 + §7-1 참조 명시.

**근사 이식 — 원본과 차이**:
- jumpExhaustion 게이트 미작동 (점프 시도 차단 안 됨)
- 점프 후 `exhaustion += gain` 누적 미작동
- `maxExhaustionToStartAction` / `maxExhaustionForAction` 변수 미존재 → HUD 점프 임계값 시각화 미작동

**Easy default 영향 분석 (세션 18)**:
- Easy default 에서 모든 jumpExhaustion=false (Boolean 17개) → `isJumpExhaustionEnabled` 항상 false
- → `exhausionEnabled = false` 상태로 D-5/D-14 안쪽 블록 100% dead
- → **Easy default 동작 영향 0** (focus_05 §6.5 P-9~P-19 dead 분석 참조)

**사용자 수동 활성화 영향**:
- 사용자가 properties 파일에 `move.jump.exhaustion=true` + `move.jump.{up|sneak|...}.exhaustion=true` 등 수동 추가 시 → 1.21.1 에 해당 key/로직 없음 → **작동 안 함**

**미래 작업 시 추가 필요 항목**:
- 다른 액션 (climb/run/sprint) exhaustion 게이팅 이식 시:
  - `maxExhaustionToStartAction` / `maxExhaustionForAction` ClientState 필드 (~5줄)
  - 매 틱 리셋 (`= Float.MAX_VALUE`, ~2줄)
  - 그때 함께 추가 (작업량 동일)
- HUD 피로도 바 점프 임계값 시각화 원할 시:
  - `getMaxExhaustion()` Config 메서드
  - SmartMovingHud / Render 의 `maxExhaustion` 참조 갱신
  - 본 포커스 외 별도 작업

**근거 (skip 결정)**:
1. 사용자 명시 결정 (2026-04-25, 세션 18 대화): Easy 1:1 + focus_05 일관 우선
2. focus_05 §6.5 P-9~P-19 + 685-700 줄 — 14종 점프 피로 명시적 배제 선례
3. 1.21.1 SmartMovingHud L61 이 `cfg.climbExhaustionStop` 직접 사용 → `getMaxExhaustion()` 호출처 0
4. Phase C 의 51 Config 필드 + 3 메서드 + 2 변수 + 리셋 모두 호출처 없는 dead code

**해소 조건 (포커스 완결 시 0건 목표)**:
- 미해소. 본 §7-1 은 사용자 메타 결정에 따른 영구 등록 — 포커스 #2.5 완결 후에도 유지.
- 추후 Hard 모드 / 사용자 수동 활성화 / HUD 피로도 바 시각화 요구 발생 시 별도 포커스로 해소 가능.

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
