# SmartMovingClientConfig.java (net.smart.moving.config) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/config/SmartMovingClientConfig.java  
패키지: `net.smart.moving.config`  
종류: `class`  
상속: `SmartMovingConfig` (extends)

---

## 역할

`SmartMovingConfig`의 raw `_xxx.value` 필드들을 **기능별로 해석하는 API 레이어**. 클라이언트 이동 로직(`SmartMovingSelf` 등)에서 직접 호출하는 메서드들을 제공한다.

`SmartMovingContext.Options`와 `SmartMovingContext.Config`의 타입이 이 클래스 또는 `SmartMovingServerConfig`이며, SmartMoving 전체에서 설정 접근의 진입점이다.

---

## import

없음.

---

## 상수 — 이동 속도 종류 (int)

```java
public final static int Sprinting = 0;
public final static int Running   = 1;
public final static int Walking   = 2;
public final static int Sneaking  = 3;
public final static int Standing  = 4;
```

`isJumpingEnabled`, `isJumpExhaustionEnabled`, `getJumpExhaustionGain`, `getJumpExhaustionStop`, `getJumpVerticalFactor`, `getJumpHorizontalFactor`, `getMaxHorizontalMotion`, `getMaxExhaustion` 메서드의 `speed` 파라미터로 사용.

---

## 상수 — 점프 종류 (int)

```java
public final static int Up                    = 0;
public final static int ChargeUp              = 1;
public final static int Angle                 = 2;
public final static int HeadUp                = 3;
public final static int SlideDown             = 4;
public final static int ClimbUp               = 5;
public final static int ClimbUpHandsOnly      = 6;
public final static int ClimbBackUp           = 7;
public final static int ClimbBackUpHandsOnly  = 8;
public final static int ClimbBackHead         = 9;
public final static int ClimbBackHeadHandsOnly = 10;
public final static int WallUp                = 11;
public final static int WallHead              = 12;
public final static int WallUpSlide           = 13;
public final static int WallHeadSlide         = 14;
```

`isJumpingEnabled`, `isJumpExhaustionEnabled`, `getJumpExhaustionGain/Stop`, `getJumpVertical/HorizontalFactor` 메서드의 `type` 파라미터로 사용.

---

## 기능 활성화 메서드 (`isXxxEnabled`)

모든 메서드는 `enabled` 필드(상위 클래스)와 조합하여 반환한다.

### `enabled`가 false일 때 true를 반환하는 메서드 (항상 허용)

```java
public boolean isSneakingEnabled()   { return _sneak.value || !enabled; }
public boolean isStandardBaseClimb() { return _isStandardBaseClimb.value || !enabled; }
public boolean isRunningEnabled()    { return _run.value || !enabled; }
public boolean isHungerGainEnabled() { return _hungerGain.value || !enabled; }
```

`enabled == false`이면 SmartMoving이 비활성화된 상태 → 기본 vanilla 동작을 허용하기 위해 `true` 반환.

### `enabled`가 false일 때 false를 반환하는 메서드 (SmartMoving 전용 기능)

```java
public boolean isSimpleBaseClimb()            { return _isSimpleBaseClimb.value && enabled; }
public boolean isSmartBaseClimb()             { return _isSmartBaseClimb.value && enabled; }
public boolean isFreeBaseClimb()              { return _isFreeBaseClimb.value && enabled; }
public boolean isFreeClimbAutoLaddderEnabled(){ return _freeClimbingAutoLaddder.value && enabled; }
public boolean isFreeClimbAutoVineEnabled()   { return _freeClimbingAutoVine.value && enabled; }
public boolean isFreeClimbingEnabled()        { return _freeClimb.value && enabled; }
public boolean isCeilingClimbingEnabled()     { return _ceilingClimbing.value && enabled; }
public boolean isSwimmingEnabled()            { return _swim.value && enabled; }
public boolean isDivingEnabled()              { return _dive.value && enabled; }
public boolean isLavaLikeWaterEnabled()       { return _lavaLikeWater.value && enabled; }
public boolean isFlyingEnabled()              { return _fly.value && enabled; }
public boolean isLevitateSmallEnabled()       { return this._levitateSmall.value && enabled; }
public boolean isRunExhaustionEnabled()       { return _runExhaustion.value && enabled; }
public boolean isClimbExhaustionEnabled()     { return _climbExhaustion.value && enabled; }
public boolean isCeilingClimbExhaustionEnabled(){ return _ceilingClimbExhaustion.value && enabled; }
public boolean isSprintingEnabled()           { return _sprint.value && enabled; }
public boolean isSprintExhaustionEnabled()    { return _sprintExhaustion.value && enabled; }
public boolean isJumpChargingEnabled()        { return _jumpCharge.value && enabled; }
public boolean isHeadJumpingEnabled()         { return _headJump.value && enabled; }
public boolean isSlidingEnabled()             { return _slide.value && enabled; }
public boolean isCrawlingEnabled()            { return _crawl.value && enabled; }
public boolean isExhaustionLossHungerEnabled(){ return _exhaustionLossHunger.value && _hungerGain.value && enabled; }
public boolean isLevitationAnimationEnabled() { return _levitateAnimation.value && enabled; }
public boolean isFallAnimationEnabled()       { return _fallAnimation.value && enabled; }
```

### 복합 조건

```java
public boolean isTotalFreeLadderClimb() { return isFreeBaseClimb() && _freeBaseLadderClimb.value; }
public boolean isTotalFreeVineClimb()   { return isFreeBaseClimb() && _freeBaseVineClimb.value; }
public boolean isSideJumpEnabled()      { return enabled && this._angleJumpSide.value; }
public boolean isBackJumpEnabled()      { return enabled && this._angleJumpBack.value; }
public boolean isWallJumpEnabled()      { return enabled && this._wallUpJump.value; }
```

---

## `isJumpingEnabled(int speed, int type)`

```java
public boolean isJumpingEnabled(int speed, int type)
{
    if(!enabled) return true;  // 비활성 시 항상 허용

    if(type == ChargeUp)   return _jumpCharge.value;
    if(type == SlideDown)  return _slide.value;
    if(type == ClimbUp || type == ClimbUpHandsOnly)             return _climbUpJump.value;
    if(type == ClimbBackUp || type == ClimbBackUpHandsOnly)     return _climbBackUpJump.value;
    if(type == ClimbBackHead || type == ClimbBackHeadHandsOnly) return _climbBackHeadJump.value;
    if(type == WallUp)  return _wallUpJump.value;
    if(type == WallHead) return _wallHeadJump.value;

    // type이 Up, Angle, HeadUp, WallUpSlide, WallHeadSlide인 경우 speed로 분기
    if(speed == Sprinting) return _sprintJump.value;
    else if(speed == Running)  return _runJump.value;
    else if(speed == Walking)  return _walkJump.value;
    else if(speed == Sneaking) return _sneakJump.value;
    else if(speed == Standing) return _standJump.value;

    return true;
}
```

type 우선, 그 다음 speed 기반 판정.

---

## `isJumpExhaustionEnabled(int speed, int type)`

```java
public boolean isJumpExhaustionEnabled(int speed, int type)
{
    if(!enabled) return false;

    boolean result = _jumpExhaustion.value;

    if(type == SlideDown) return result && _jumpSlideExhaustion.value;
    else if(type == Angle) result &= _angleJumpExhaustion.value;
    else if(type == ClimbUp || type == ClimbUpHandsOnly) result &= _climbJumpUpExhaustion.value;
    else if(type == ClimbBackUp || type == ClimbBackUpHandsOnly) result &= _climbJumpBackUpExhaustion.value;
    else if(type == ClimbBackHead || type == ClimbBackHeadHandsOnly) result &= _climbJumpBackHeadExhaustion.value;
    else if(type == WallUp) result &= _wallUpJumpExhaustion.value;
    else if(type == WallHead) result &= _wallHeadJumpExhaustion.value;
    else result &= _upJumpExhaustion.value;

    // 클라이밍 점프 공통 추가 조건
    if(type == ClimbUp || ... || type == ClimbBackHead || type == ClimbBackHeadHandsOnly)
        return result && _climbJumpExhaustion.value;
    // 벽 점프 공통 추가 조건
    if(type == WallUp || type == WallHead)
        return result && _wallJumpExhaustion.value;

    // 일반 점프: speed 기반 AND
    if(speed == Sprinting) result &= _sprintJumpExhaustion.value;
    // ... 나머지 speed

    if(type == ChargeUp) result |= _jumpChargeExhaustion.value;  // ChargeUp은 OR

    return result;
}
```

`_jumpExhaustion`이 기본값이고, type별로 AND 조합. ClimbXxx/WallXxx는 공통 추가 조건(`_climbJumpExhaustion`, `_wallJumpExhaustion`)을 AND. `ChargeUp`은 마지막에 OR.

---

## `getJumpExhaustionGain(int speed, int type, float jumpCharge)` → `float`

```java
float result = _baseExhautionGainFactor.value * _jumpExhaustionGainFactor.value;
// type별 result *= typeGainFactor
// ClimbXxx: return result * _climbJumpExhaustionGainFactor.value
// WallXxx: return result * _wallJumpExhaustionGainFactor.value
// 일반: speed별 result *= speedGainFactor

if(type == ChargeUp)
{
    if(!isJumpExhaustionEnabled(speed, Up)) result = 0;
    result +=
        _baseExhautionGainFactor.value *
        _jumpExhaustionGainFactor.value *
        _upJumpExhaustionGainFactor.value *
        _jumpChargeExhaustionGainFactor.value *
        Math.min(jumpCharge, _jumpChargeMaximum.value) / _jumpChargeMaximum.value;
}
return result;
```

`ChargeUp`에서 `jumpCharge`를 `_jumpChargeMaximum`으로 clamp 후 비례 계산.

---

## `getJumpExhaustionStop(int speed, int type, float jumpCharge)` → `float`

```java
float result = _jumpExhaustionStopFactor.value;
// type별 result *= typeStopFactor
// 구조는 getJumpExhaustionGain과 동일

if(type == ChargeUp)
{
    if(!isJumpExhaustionEnabled(speed, Up))
        result += getJumpExhaustionGain(speed, Up, 0);
    result -=
        _jumpExhaustionStopFactor.value *
        _upJumpExhaustionStopFactor.value *
        _jumpChargeExhaustionStopFactor.value *
        Math.min(jumpCharge, _jumpChargeMaximum.value) / _jumpChargeMaximum.value;
}
return result;
```

`enabled` 체크 없음 (항상 계산). `ChargeUp`에서 stop은 charge 비례로 감소(빼기).

---

## `getJumpChargeFactor(float jumpCharge)` → `float`

```java
public float getJumpChargeFactor(float jumpCharge)
{
    if(!enabled || !_jumpCharge.value) return 1F;
    jumpCharge = Math.min(jumpCharge, _jumpChargeMaximum.value);
    return 1F + jumpCharge / _jumpChargeMaximum.value * (_jumpChargeFactor.value - 1F);
}
```

`jumpCharge / _jumpChargeMaximum`(0~1) 비율 × `(_jumpChargeFactor - 1)` + 1.  
charge가 0이면 1F, charge가 최대이면 `_jumpChargeFactor.value`.

---

## `getHeadJumpFactor(float headJumpCharge)` → `float`

```java
public float getHeadJumpFactor(float headJumpCharge)
{
    if(!enabled || !_headJump.value) return 1F;
    headJumpCharge = Math.min(headJumpCharge, _headJumpChargeMaximum.value);
    return (headJumpCharge - 1) / (_headJumpChargeMaximum.value - 1);
}
```

`headJumpCharge - 1`이 분자 — charge가 1부터 시작하는 구조. 최대이면 1F.

---

## `getJumpVerticalFactor(int speed, int type)` → `float`

```java
float result = _jumpVerticalFactor.value;

if(type == Angle) return result * _angleJumpVerticalFactor.value;  // 즉시 반환

// ClimbXxx: result *= _climbXxxJumpVerticalFactor (손만 있으면 추가 factor)
// WallXxx: result *= _wallUpJumpVerticalFactor, WallHead += _wallHeadJumpVerticalFactor

if(특수 타입에 해당하면) return result;  // speed 분기 없이 반환

// 일반 점프: speed별 result *= speedVerticalFactor
return result;
```

`Standing` + Up 점프: speed=Standing이면 `_standJumpVerticalFactor.value` 곱.

---

## `getJumpHorizontalFactor(int speed, int type)` → `float`

```java
float result = _jumpHorizontalFactor.value;
// type별 result *= typeHorizontalFactor

// Standing + Up 점프 특수 처리:
else if(speed == Standing && type != ClimbBackXxx && type != WallXxx)
    result *= 0F;  // 제자리 점프는 수평 0

return result;
```

`!enabled`이면: `speed == Running ? 2F : 1F`. `Standing`에서 일반 점프는 `0F` (제자리).

---

## `getMaxHorizontalMotion(int speed, int type, boolean inWater)` → `float`

```java
@SuppressWarnings("unused")
public float getMaxHorizontalMotion(int speed, int type, boolean inWater)
{
    float maxMotion = 0.117852041920949F;  // 기본 이동 속도
    if(!enabled) return speed == Running ? maxMotion * 1.3F : maxMotion;

    if(inWater) maxMotion = 0.07839602977037292F;  // 물속 이동 속도

    if(speed == Sprinting) maxMotion *= _sprintFactor.value;
    else if(speed == Running) maxMotion *= _runFactor.value;
    else if(speed == Sneaking) maxMotion *= _sneakFactor.value;

    return maxMotion;
}
```

`@SuppressWarnings("unused")` — 현재 사용되지 않는 메서드. 수평 이동 속도 상한:
- 기본: `0.117852041920949F`
- 물속: `0.07839602977037292F`
- Sprinting/Running/Sneaking만 factor 적용, Walking/Standing은 기본값 그대로.

---

## `getMaxExhaustion()` → `float`

```java
public float getMaxExhaustion()
{
    float result = 0F;
    if(_run.value && _runExhaustion.value)
        result = max(result, _runExhaustionStop.value);
    if(_sprint.value && _sprintExhaustion.value)
        result = max(result, _sprintExhaustionStop.value);

    if(_jump.value)
        for(int i = Sprinting; i <= Standing; i++)       // speed: 0~4
            for(int n = Up; n <= WallHeadSlide; n++)     // type: 0~14
                if(isJumpExhaustionEnabled(i, n))
                    for(int t = 0; t <= 1; t++)           // jumpCharge: 0, 1
                        result = max(result, getJumpExhaustionStop(i, n, t) + getJumpExhaustionGain(i, n, t));

    if(_freeClimb.value && _climbExhaustion.value)
        result = max(result, _climbExhaustionStop.value);
    if(_ceilingClimbing.value && _ceilingClimbExhaustion.value)
        result = max(result, _ceilingClimbExhaustionStop.value);
    return result;
}

private static float max(float value, float valueOrInfinite) {
    return valueOrInfinite == java.lang.Float.POSITIVE_INFINITY ? value : Math.max(value, valueOrInfinite);
}
```

모든 활성화된 기능의 소진 정지값(`ExhaustionStop`) + 소진 획득값(`ExhaustionGain`) 최댓값 계산. `POSITIVE_INFINITY`인 stop 값은 제외. `ISmartMovingClient.getMaximumExhaustion()`에서 이 값과 외부 등록값 중 최댓값 반환.

---

## `getFactor(...)` → `float`

```java
public float getFactor(boolean hunger, boolean onGround, boolean isStanding, boolean isStill,
    boolean isSneaking, boolean isRunning, boolean isSprinting,
    boolean isClimbing, boolean isClimbCrawling, boolean isCeilingClimbing,
    boolean isDipping, boolean isSwimming, boolean isDiving,
    boolean isCrawling, boolean isCrawlClimbing)
```

소진 손실(`exhaustionLoss`) 또는 허기 획득(`hunger`) 배율 계산.

**전처리:**
```java
isClimbing |= isClimbCrawling;
isCrawling |= isCrawlClimbing;
boolean actionOverGound = isClimbing || isCeilingClimbing || isDiving || isSwimming;
boolean airBorne = !onGround && !actionOverGound;
isStanding = actionOverGound ? isStill : isStanding;
isSneaking = isSneaking & !isStanding;
```

**1단계 — 이동 속도 factor:**
```java
float factor = hunger ? _baseHungerGainFactor.value : _baseExhautionLossFactor.value;
if(airBorne)    factor *= hunger ? 0F : _fallExhautionLossFactor.value;
else if(isSprinting) factor *= hunger ? _sprintingHungerGainFactor.value : _sprintingExhautionLossFactor.value;
else if(isRunning)   factor *= ...
else if(isSneaking)  factor *= ...
else if(isStanding)  factor *= ...
else                 factor *= ...  // walking
```

`hunger`이면 허기 획득 배율, 아니면 소진 손실 배율. 공중(`airBorne`)이면 허기 0 배율.

**2단계 — 행동 factor (1단계에 곱):**
```java
if(isClimbing)         factor *= hunger ? _climbingHungerGainFactor.value : _climbingExhaustionLossFactor.value;
else if(isCrawling)    factor *= ...
else if(isCeilingClimbing) factor *= ...
else if(isSwimming)    factor *= ...
else if(isDiving)      factor *= ...
else if(isDipping)     factor *= ...
else if(onGround)      factor *= ...  // normal
else                   factor *= ...  // normal (공중이지만 actionOverGround 없는 경우)
```

isClimbing, isCrawling, isCeilingClimbing, isSwimming, isDiving, isDipping 순 우선순위.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingConfig` | 상위 클래스 — 모든 `_xxx.value` 필드 |

`SmartMovingConfig`의 필드를 직접 읽는다. `SmartMovingConfig` 자체는 이 리서치에서 아직 읽지 않음 — 다음 리서치 대상.

---

## 주요 관찰 사항

1. **`enabled` 패턴**: SmartMoving 전체 비활성화 시 vanilla 동작 허용하는 메서드는 `|| !enabled`, SmartMoving 전용 기능은 `&& enabled`. `isSneakingEnabled`, `isStandardBaseClimb`, `isRunningEnabled`, `isHungerGainEnabled`가 전자.

2. **점프 타입 14종**: 일반/차지/각도/헤드/슬라이드/클라이밍 6종/벽 점프 4종. `isJumpingEnabled`, `isJumpExhaustionEnabled`, `getJumpExhaustionGain/Stop`, `getJumpVertical/HorizontalFactor`가 모두 이 종류에 따라 분기.

3. **소진 계층 구조**: `getJumpExhaustionGain`에서 `_baseExhautionGainFactor × _jumpExhaustionGainFactor × typeGainFactor × speedGainFactor` 4단계 곱. 오타: `_baseExhautionGainFactor` (Exhaust**i**on이 아닌 Exhaut**i**on).

4. **`getMaxHorizontalMotion` 미사용**: `@SuppressWarnings("unused")` — 코드에 정의되어 있지만 실제로 호출되지 않음.

5. **`getMaxExhaustion`의 순회**: speed 5 × type 15 × jumpCharge 2 = 150회 조합을 순회해서 최대 소진값 계산. `POSITIVE_INFINITY` stop 값은 max에서 제외.

6. **`getFactor` 의미**: 허기/소진 배율을 이동 상태와 행동 상태 두 단계로 계산. `SmartMovingSelf`에서 매 틱 소진/허기 계산 시 사용.

---

## R-18 추가 리서치 — SmartMovingConfig 미확인 기본값 (2026-04-22)

### PositiveFactor 기본값 확인 (Properties.java)

`Properties.getDefaultValue(type)` 반환값 (타입별):
- `PositiveFactor` → **1F**
- `NegativeFactor` → 1F
- `IncreasingFactor` → 1F
- `DecreasingFactor` → 1F

### A-18: `_freeClimbingUpSpeedFactor` / `_freeClimbingDownSpeedFactor`

```java
// SmartMovingConfig.java Section 3 (Climbing)
public final Property<Float> _freeClimbingUpSpeedFactor   = PositiveFactor("move.climb.free.up.speed.factor");
public final Property<Float> _freeClimbingDownSpeedFactor = PositiveFactor("move.climb.free.down.speed.factor");
```

`.defaults()` 호출 없음 → `Properties.getDefaultValue(PositiveFactor)` = **1F**.  
→ 현재 SmartMovingClimber.java의 `1.0D`가 정확함. **A-18 확인 완료.**

### A-25: `_iceSpeedFactor`

SmartMovingConfig.java 전체에 `_iceSpeedFactor` 필드 **없음**.  
원본 SmartMovingMover.java에서 ice speed 처리 방식 별도 확인 필요 (R-19 또는 별도 청크).  
현재 SmartMovingMover.java의 `1.5F`는 config에서 오는 값이 아님. **A-25 닫힘 — config 필드 없음.**

### A-31: `_slideSlipperinessFactor`

```java
// SmartMovingConfig.java Section 8 (Sliding)
public final Property<Float> _slideSlipperinessFactor = PositiveFactor("move.slide.glide.factor");
```

`.defaults()` 없음 → **1F**.  
→ 현재 SmartMovingConfig.java의 `slideSlipperinessFactor = 1.0F` 정확함. **A-31 확인 완료.**

### A-32: `_slidingSpeedStopFactor`

```java
public final Property<Float> _slidingSpeedStopFactor = PositiveFactor("move.slide.speed.stop.factor");
```

`.defaults()` 없음 → **1F**.  
→ 이전 `0.01F`는 오류. SmartMovingConfig.java에서 **1.0F**로 수정 완료. **A-32 확인 완료.**

7. **1.21.1 이식**: 메서드 구조는 그대로 유지 가능. 상위 클래스 `SmartMovingConfig`의 필드 시스템을 이식하면 이 클래스도 자동으로 동작. `_baseExhautionGainFactor` 오타는 이식 시 수정 검토.
