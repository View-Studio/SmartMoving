# 리서치: 울타리(Fence) / 철 창살(Iron Bars) 클라이밍

## 목표
사용자 보고 5건 버그를 원본 SM 1.7.10 코드와 1:1 대조해 근본 원인 확정.

## 사용자 보고 버그
1. **울타리 블록 자체 등반 안 됨** (창살은 됨)
2. **철 창살 자체 등반 속도가 원본과 다름** (꼼꼼히 1:1 수정 요구)
3. **울타리 위 잡기 안 됨** (창살은 됨)
4. **철 창살 위 잡기 시 점점 떨어짐** (홀드 안 됨)
5. **위 잡기 횡이동 속도 원본과 다름** (꼼꼼히 1:1 수정 요구)

사용자 요구: **fence/iron_bars 둘 다 자체 등반 + 위 잡기 모두 작동.** 원본은 옵션 분리되어 있을 수 있으나 우리 모드는 둘 다 활성.

---

## 원본 SM 1.7.10 사실 (전수 조사 결과)

원본 위치: `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\`

### A. 모션 상수 (`SmartMovingContext.java` L31-39)
```java
public static final float  ClimbPullMotion       = 0.3F;
public static final double FastUpMotion          = 0.2D;
public static final double MediumUpMotion        = 0.14D;
public static final double SlowUpMotion          = 0.1D;
public static final double HoldMotion            = 0.08D;
public static final double SinkDownMotion        = 0.05D;
public static final double ClimbDownMotion       = 0.01D;
public static final double CatchCrawlGapMotion   = 0.17D;
```

### B. 옵션 기본값 (`SmartMovingConfig.java` + `Properties.java`)

| 옵션 키 | 타입 | 기본값 | 라인 |
|--------|-----|------|-----|
| `move.climb.free.fence` (`_freeFenceClimbing`) | Unmodified Boolean | **`true`** | Config.L123, Properties.L171-172 |
| `move.climb.free.up.speed.factor` (`_freeClimbingUpSpeedFactor`) | PositiveFactor Float | `1F` | Config.L113 |
| `move.climb.free.down.speed.factor` (`_freeClimbingDownSpeedFactor`) | PositiveFactor Float | `1F` | Config.L114 |
| `move.climb.free.horizontal.speed.factor` (`_freeClimbingHorizontalSpeedFactor`) | PositiveFactor Float | `1F` | Config.L115 |
| `move.climb.free` (`_freeClimb`) | Unmodified Boolean | **`true`** | Config.L103 |

**핵심:** `Unmodified` 기본값은 `true` — 원본의 fence 클라이밍은 기본 활성.

### C. `hasHalfHold()` Fence/Iron Bars 분기 (`Orientation.java` L557-630)

iron_bars 무조건 분기 (3곳):
```java
// L561-562: remote isEmpty + remote==iron_bars + front
if(isEmpty(base_i, 0, base_k))
    if(remoteId == Block.getBlockFromName("iron_bars") && headedToFrontWall(remote_i, 0, remote_k, remoteId))
        return setHalfGrabType(HalfGrab, remoteId);

// L566-567: wallId==iron_bars + headedToBaseWall
if(wallId == Block.getBlockFromName("iron_bars") && headedToBaseWall(0, wallId))
    return setHalfGrabType(HalfGrab, wallId, false);
```

fence 분기 — `Config._freeFenceClimbing.value` 게이트:
```java
// L571: if(Config._freeFenceClimbing.value)
//   (a) remote fence + front → HalfGrab
//   (b) remoteBelow fence + front → HalfGrab
//   (c) wallId fence + headedToBaseWall → HalfGrab
//   (d) belowWallId fence + headedToBaseWall → HalfGrab
//   (e) cobblestone_wall remote + !headedToRemoteFlatWall → HalfGrab
//   (f) cobblestone_wall remoteBelow + !headedToRemoteFlatWall → HalfGrab
```

### D. 떨어짐 방지 (`SmartMovingSelf.java`)

#### handleClimbing TopHold 분기 (L1011-1020)
```java
else if(handsClimbing == HandsClimbing.TopHold ||
        feetClimbing == FeetClimbing.BaseHold ||
        (feetClimbing == FeetClimbing.SlowUpWithHoldWithoutHands && handsClimbing == HandsClimbing.None))
{
    // holding at top
    if (!jumpButton.StartPressed || !(isClimbJumping = tryJump(...)))
    {
        if(handsClimbing == HandsClimbing.Sink && feetClimbing == FeetClimbing.BaseHold ||
           handsClimbing == HandsClimbing.TopHold && feetClimbing == FeetClimbing.TopWithHands)
            setShouldClimbSpeed(HoldMotion, HandsClimbing.MiddleGrab, FeetClimbing.DownStep);
        else
            setShouldClimbSpeed(HoldMotion);   // ← motionY = 0.08 적용
    }
}
```

#### setOnlyShouldClimbSpeed relevant 가드 (L1547-1549)
```java
boolean relevant = value < 0 || value > sp.motionY;
if(relevant)
    sp.motionY = value;     // value=HoldMotion(0.08) > motionY(중력 -0.2) → 적용 → 안 떨어짐
isClimbJumping = !relevant && !isClimbHolding;
```

### E. 횡이동 속도 (`SmartMovingSelf.java` L197-227)
```java
private float getNonSlowInputSpeedFactor(float moveForward, float moveStrafing)
{
    float speedFactor = 1f;
    if(isFast)
        speedFactor *= (!isLevitating() ? Config._sprintFactor.value : Config._sprintFactorLevitate.value);
    if(isClimbing)
        if(moveStrafing != 0F || moveForward != 0F)
            speedFactor *= Config._freeClimbingHorizontalSpeedFactor.value;   // ← 클라이밍 횡 속도
    return speedFactor;
}
```

**원본은 단순 0.15 클램프 아님.** `moveStrafing/moveForward` 입력에 `_freeClimbingHorizontalSpeedFactor`(기본 1F) 곱.

---

## 우리 현재 코드 사실

작업 디렉토리: `C:\Users\user\IdeaProjects\SmartMoving`

### F. 옵션 기본값 (`src/main/java/.../config/SmartMovingConfig.java`)
| 필드 | 현재 값 | 원본과 일치? |
|-----|--------|------------|
| `freeFenceClimbing` (L494) | `false` | **❌ 원본 `true`** |
| `freeClimbingUpSpeedFactor` (L481) | `1.0F` | ✓ |
| `freeClimbingDownSpeedFactor` (L482) | `1.0F` | ✓ |
| `freeClimbingHorizontalSpeedFactor` | **확인 필요** | TBD |

### G. `Orientation.hasHalfHold()` (`src/main/java/.../climbing/Orientation.java` L2403-2494)
- iron_bars 무조건 분기: ✓ 이식됨 (L2418-2426 부근)
- fence 분기 6개: ✓ 이식됨 (L2430-2462), `cfg.freeFenceClimbing` 게이트

### H. `setOnlyShouldClimbSpeed` (`src/client/java/.../client/SmartMovingClimber.java` L252-289)
- 원본 L1513-1551 1:1 이식됨
- `value > HoldMotion` → up factor 보간
- `value < HoldMotion` → down factor 보간
- relevant 가드 (L282-286) ✓

### I. `MixinLivingEntityClient.travel` (`src/client/java/.../mixin/client/MixinLivingEntityClient.java`)
- L181-198: `getOnLadderOrVine` 호출 → `onClimbable = hands.isRelevant() || feet.isRelevant()`
  - **이 경로는 사다리/덩굴만 감지.** fence/iron_bars 는 hasHalfHold 의 free climb 경로로 처리.
- L208: `wantFreeClimb = sm.wantClimb`
- L218-219: `if (onClimbable || wantFreeClimb) handleClimbing(player, sm)`
- L292-302: **`onClimbable`** 일 때만 horizontal velocity 0.15 클램프
  - free climb (fence/iron_bars) 의 경우 `onClimbable=false` 이므로 클램프 미적용
- L307-315: **`onClimbable`** 일 때만 `fallDistance=0` + Y 하한 클램프
  - free climb 의 경우 `onClimbable=false` 이므로 fallDistance 리셋 미적용

---

## 근본 원인 진단

### 버그 #1 (울타리 자체 등반 안 됨)
**원인**: `freeFenceClimbing = false` (우리) vs 원본 `true`
**위치**: `src/main/java/.../config/SmartMovingConfig.java` L494
**수정**: `false` → `true`

### 버그 #3 (울타리 위 잡기 안 됨)
**원인**: 동일. `cfg.freeFenceClimbing` 게이트로 `Orientation.hasHalfHold()` fence 6분기 전부 미진입.
**수정**: 동일.

### 버그 #4 (철 창살 위 잡기 떨어짐)
**원인**: `MixinLivingEntityClient.travel` L307-315 의 `fallDistance=0` 리셋이 **`onClimbable`** 조건부.
- `onClimbable` 은 사다리/덩굴 인접일 때만 true
- iron_bars 위 잡기는 free climb 경로 (`wantClimb=true` + handleClimbing → setShouldClimbSpeed(HoldMotion))
- `setOnlyShouldClimbSpeed` L727 에 `player.fallDistance = 0` 있긴 하나, **TopHold 진입 자체가 안 되거나 motionY 갱신 후 다음 틱 중력 누적** 가능성
- 실제 호출 흐름: `wantClimb=true` 시 `handleClimbing` 진입, hasHalfHold 결과로 handsClimbing/feetClimbing 결정. TopHold 분기에서 `setShouldClimbSpeed(HoldMotion)` → `setOnlyShouldClimbSpeed` → relevant 가드 (`value=0.08 > motionY(-중력)` ⇒ true) → `motionY=0.08` 적용
- 의심 포인트:
  - (i) 위 잡기가 `wantClimbUp` 분기 안에 있는데, 사용자가 forward 안 누르면 `wantClimbUp=false`, `wantClimbDown` 분기로 빠짐
  - (ii) `wantClimbDown` 분기에는 TopHold 케이스 없음 → fallback 으로 motion 미설정 → 떨어짐

### 버그 #2 (철 창살 자체 등반 속도 원본과 다름)
**원인 가설**:
- 자체 등반 = `wantClimbUp` 분기에서 hasHalfHold 결과로 handsClimbing=Up/FastUp 등 진입
- 우리 코드의 `climbRawSpeed * climbSpeedFactor` 계산이 원본 `setShouldClimbSpeed` 의 motionY 계산과 **별개로 horizontal 입력** 에 사용됨
- 원본은 horizontal 속도를 vanilla `moveFlying` 과 `_freeClimbingHorizontalSpeedFactor` 곱으로 처리. 우리는 `updateVelocity` + 0.15 클램프 단순 적용 → 원본과 차이.
- **확정 근거**: 원본은 motionY 만 setShouldClimbSpeed 가 제어하고 horizontal 은 vanilla movement 에 `_freeClimbingHorizontalSpeedFactor` 적용. 우리는 두 흐름이 섞여 있음.

### 버그 #5 (위 잡기 횡이동 속도 원본과 다름)
**원인**: 우리는 `MixinLivingEntityClient.L292-302` 에서 단순 `±0.15` 클램프.
**원본**: `_freeClimbingHorizontalSpeedFactor` (기본 1F) 곱 + vanilla 의 climbing applyMovementInput 이 그대로 작동 (다만 SM mixin 으로 `applyClimbingSpeed` 차단).

---

## 수정 방향 (상세는 체크리스트 참조)

1. **`freeFenceClimbing` 기본값 `true`** — 원본 1:1 (#1, #3)
2. **`fallDistance=0` 리셋을 `sm.isClimbing` 일 때도 적용** — TopHold 시 떨어짐 방지 (#4)
3. **`wantClimbDown` 분기에도 TopHold/HalfHold 시 HoldMotion 호출 추가** — 원본 L1032-1053 정밀 대조 (#4)
4. **horizontal 클램프를 0.15D 고정 → `_freeClimbingHorizontalSpeedFactor` 기반 보정** (#2, #5)
5. **`climbRawSpeed * climbSpeedFactor` 계산이 원본 1:1인지 재검증** (#2)

---

## 후속 정밀 조사 필요 항목 (체크리스트 작업 전 확인)
- [ ] `freeClimbingHorizontalSpeedFactor` 가 `SmartMovingConfig.java` 에 필드로 존재하는지
- [ ] `MixinLivingEntityClient` 에서 `wantClimbDown` 분기 처리 정확성 (원본 L1028-1053 재대조)
- [ ] `setShouldClimbSpeed` 가 `wantClimbDown` 의 BottomHold 케이스 (L1032-1036) 처리하는지
- [ ] iron_bars 위 잡기에서 `handsClimbing` 이 어떤 enum 값으로 들어가는지 (TopHold? UpGrab?)
