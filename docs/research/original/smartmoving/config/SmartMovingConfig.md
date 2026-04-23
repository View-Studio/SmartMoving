# SmartMovingConfig.java (net.smart.moving.config) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/config/SmartMovingConfig.java  
패키지: `net.smart.moving.config`  
종류: `class`  
상속: `SmartMovingProperties` (extends)

---

## 전체 소스

```java
package net.smart.moving.config;

import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

import net.smart.properties.*;
import net.smart.properties.Properties;

public class SmartMovingConfig extends SmartMovingProperties
{
    private static final String _smartLadderClimbingSpeedPropertiesFileName = "smart_ladder_climbing_speed_options.txt";
    private static final String _smartClimbingPropertiesFileName = "smart_climbing_options.txt";
    private static final String _smartMovingPropertiesFileName = "smart_moving_options.txt";
    private static final String _smartMovingClientServerPropertiesFileName = "smart_moving_server_options.txt";

    private static final String _slcs = "0.1";
    private static final String _sc = "0.2";
    private static final String _sm_1_0 = "1.0";
    private static final String _sm_1_1 = "1.1";
    private static final String _sm_1_2 = "1.2";
    private static final String _sm_1_3 = "1.3";
    private static final String _sm_1_4 = "1.4";
    private static final String _sm_1_5 = "1.5";
    private static final String _sm_1_6 = "1.6";
    private static final String _sm_1_7 = "1.7";
    private static final String _sm_1_8 = "1.8";
    private static final String _sm_1_9 = "1.9";
    private static final String _sm_1_10 = "1.10";
    private static final String _sm_1_11 = "1.11";
    public static final String _sm_2_0 = "2.0";
    public static final String _sm_2_1 = "2.1";
    public static final String _sm_2_2 = "2.2";
    public static final String _sm_2_3 = "2.3";
    public static final String _sm_2_4 = "2.4";
    public static final String _sm_2_5 = "2.5";
    public static final String _sm_2_6 = "2.6";
    public static final String _sm_3_0 = "3.0";
    public static final String _sm_3_1 = "3.1";
    public static final String _sm_3_2 = "3.2";
    public static final String _sm_current = _sm_3_2;

    private static final String[] _all_sm = new String[] { _sm_3_2, _sm_3_1, _sm_3_0, _sm_2_6, _sm_2_5, _sm_2_4, _sm_2_3, _sm_2_2, _sm_2_1, _sm_2_0, _sm_1_11, _sm_1_10, _sm_1_9, _sm_1_8, _sm_1_7, _sm_1_6, _sm_1_5, _sm_1_4, _sm_1_3, _sm_1_2, _sm_1_1, _sm_1_0 };
    private static final String[] _all_old = new String[] { _sc, _slcs };

    protected static final String[] _pre_sm_1_3 = new String[] { _sm_1_2, _sm_1_1, _sm_1_0 };
    protected static final String[] _pre_sm_1_4 = concat(_sm_1_3, _pre_sm_1_3);
    protected static final String[] _pre_sm_1_5 = concat(_sm_1_4, _pre_sm_1_4);
    protected static final String[] _pre_sm_1_6 = concat(_sm_1_5, _pre_sm_1_5);
    protected static final String[] _pre_sm_1_7 = concat(_sm_1_6, _pre_sm_1_6);
    protected static final String[] _pre_sm_1_8 = concat(_sm_1_7, _pre_sm_1_7);
    protected static final String[] _pre_sm_1_9 = concat(_sm_1_8, _pre_sm_1_8);
    protected static final String[] _pre_sm_1_10 = concat(_sm_1_9, _pre_sm_1_9);
    protected static final String[] _pre_sm_1_11 = concat(_sm_1_10, _pre_sm_1_10);
    protected static final String[] _pre_sm_2_0 = concat(_sm_1_11, _pre_sm_1_11);
    protected static final String[] _pre_sm_2_1 = concat(_sm_2_0, _pre_sm_2_0);
    protected static final String[] _pre_sm_2_2 = concat(_sm_2_1, _pre_sm_2_1);
    protected static final String[] _pre_sm_2_3 = concat(_sm_2_2, _pre_sm_2_2);
    protected static final String[] _pre_sm_2_4 = concat(_sm_2_3, _pre_sm_2_3);
    protected static final String[] _pre_sm_2_5 = concat(_sm_2_4, _pre_sm_2_4);
    protected static final String[] _pre_sm_2_6 = concat(_sm_2_5, _pre_sm_2_5);
    protected static final String[] _pre_sm_3_0 = concat(_sm_2_6, _pre_sm_2_6);
    protected static final String[] _pre_sm_3_1 = concat(_sm_3_0, _pre_sm_3_0);
    protected static final String[] _pre_sm_3_2 = concat(_sm_3_1, _pre_sm_3_1);

    private static final String[] _pre_sm_1_7_post_1_4 = new String[] { _sm_1_6, _sm_1_5 };
    private static final String[] _pre_sm_1_7_post_1_0 = new String[] { _sm_1_6, _sm_1_5, _sm_1_4, _sm_1_3, _sm_1_2, _sm_1_1 };

    public static final String[] _all = concat(_all_sm, _all_old);

    // ... (필드 및 메서드 생략 — 아래 섹션에서 분류 설명)
```

---

## 역할

모든 SmartMoving 설정 옵션을 `Property<T>` 필드로 선언하는 중앙 설정 클래스. `SmartMovingProperties`(상위)의 Property 팩토리 메서드를 이용해 옵션을 선언하고, 버전별 기본값·키 이름·의존성·제약을 메서드 체인으로 정의한다.

`SmartMovingClientConfig`와 `SmartMovingServerConfig`가 이 클래스를 extends한다.

---

## import

```java
import java.io.*;            // File, PrintWriter 등
import java.math.BigDecimal; // getSpeedPercent() 소수 처리
import java.text.DecimalFormat;
import java.util.*;          // List, Map, Dictionary, Set
import net.smart.properties.*;
import net.smart.properties.Properties; // SmartMoving 자체 Properties 시스템
```

---

## 버전 관리 시스템

### 버전 상수

| 상수 | 값 | 접근 |
|------|----|------|
| `_slcs` | `"0.1"` | private |
| `_sc` | `"0.2"` | private |
| `_sm_1_0` ~ `_sm_1_11` | `"1.0"` ~ `"1.11"` | private |
| `_sm_2_0` ~ `_sm_2_6` | `"2.0"` ~ `"2.6"` | **public static** |
| `_sm_3_0` ~ `_sm_3_2` | `"3.0"` ~ `"3.2"` | **public static** |
| `_sm_current` | `= _sm_3_2` = `"3.2"` | public static |

`_sm_2_0` 이후는 public — 다른 클래스에서 버전 비교에 사용.

### 누적 버전 배열 (`_pre_sm_X_Y`)

```java
protected static final String[] _pre_sm_1_3 = new String[] { _sm_1_2, _sm_1_1, _sm_1_0 };
protected static final String[] _pre_sm_1_4 = concat(_sm_1_3, _pre_sm_1_3); // 1.3 + {1.2, 1.1, 1.0}
// ... 이하 같은 패턴으로 _pre_sm_3_2까지 누적
```

`_pre_sm_X_Y`는 "버전 X.Y 이전의 모든 버전" 배열. `concat(latestVersion, previousArray)` 패턴으로 누적.

`Property.defaults(value, versions[])` 호출 시 해당 버전들에 대한 기본값 지정에 사용.

### 특수 범위 배열

```java
private static final String[] _pre_sm_1_7_post_1_4 = new String[] { _sm_1_6, _sm_1_5 };        // 1.5, 1.6만
private static final String[] _pre_sm_1_7_post_1_0 = new String[] { _sm_1_6, _sm_1_5, _sm_1_4, _sm_1_3, _sm_1_2, _sm_1_1 }; // 1.1~1.6
```

구버전 점프 소진 마이그레이션 전용.

### 전체 버전 배열

```java
private static final String[] _all_sm = new String[] { _sm_3_2, _sm_3_1, ..., _sm_1_0 }; // 22개
private static final String[] _all_old = new String[] { _sc, _slcs };                       // 2개
public static final String[] _all = concat(_all_sm, _all_old);                              // 24개 전체
```

---

## 설정 파일 이름

```java
private static final String _smartLadderClimbingSpeedPropertiesFileName = "smart_ladder_climbing_speed_options.txt"; // v0.1
private static final String _smartClimbingPropertiesFileName            = "smart_climbing_options.txt";              // v0.2
private static final String _smartMovingPropertiesFileName              = "smart_moving_options.txt";                // 현재 메인 파일
private static final String _smartMovingClientServerPropertiesFileName  = "smart_moving_server_options.txt";         // 서버 공유 설정
```

---

## Property 선언 필드 목록

### 1. Generic Movement

```java
public final Property<Boolean> _vanillaStyle = Modified("move.general.vanilla")
    .comment("Whether movement on ground should be identical to vanilla")
    .book("Generic Movement", ...);
```

---

### 2. Global Speed

```java
public final Property<Float>   _speedFactor         = PositiveFactor("move.speed.factor").defaults(1F);
public final Property<Boolean> _speedUser           = Creative("move.speed.user").defaults(true, _pre_sm_3_2);
public final Property<Float>   _speedUserFactor     = PositiveFactor("move.speed.user.factor").singular().defaults(0.2F).min(0.0001F);
public final Property<Integer> _speedUserExponent   = Integer("move.speed.user.exponent").singular().defaults(0);
```

**`getUserSpeedFactor()` 공식**: `Math.pow(1F + _speedUserFactor.value, _speedUserExponent.value)`  
exponent=0이면 1F(배율 없음), exponent 양수면 배율 증가, 음수면 감소.

---

### 3. Climbing

```java
// 기본 클라이밍 모드 (free / smart / simple / standard)
public final Property<String>  _baseClimb           = String().defaults("free").key("move.climb.base")...;
public final Property<Boolean> _freeClimb           = Unmodified().key("move.climb.free")...;
public final Property<Boolean> _freeBaseLadderClimb = Modified().key("move.climb.free.base.ladder")...;
public final Property<Boolean> _freeBaseVineClimb   = Modified().key("move.climb.free.base.vine").defaults(true, _pre_sm_1_11)...;

// Computed boolean Properties (조합)
public final Property<Boolean> _isFreeBaseClimb     = _baseClimb.is("free").and(_freeClimb);
public final Property<Boolean> _isSmartBaseClimb    = _baseClimb.is("smart").andNot(_isFreeBaseClimb);
public final Property<Boolean> _isSimpleBaseClimb   = _baseClimb.is("simple").andNot(_isFreeBaseClimb).andNot(_isSmartBaseClimb);
public final Property<Boolean> _isStandardBaseClimb = _isFreeBaseClimb.not().andNot(_isSmartBaseClimb).andNot(_isSimpleBaseClimb);
```

**`_isFreeBaseClimb`**: `_baseClimb == "free" && _freeClimb == true`  
**`_isStandardBaseClimb`**: 나머지 3개 모두 false인 경우

```java
// 자유 클라이밍 속도
public final Property<Float>   _freeClimbingUpSpeedFactor         = PositiveFactor("move.climb.free.up.speed.factor");
public final Property<Float>   _freeClimbingDownSpeedFactor       = PositiveFactor("move.climb.free.down.speed.factor");
public final Property<Float>   _freeClimbingHorizontalSpeedFactor = PositiveFactor("move.climb.free.horizontal.speed.factor");
// 방향각
public final Property<Float>   _freeClimbingOrthogonalDirectionAngle = Positive(...).values(90F, 90F, 180F); // N,S,E,W
public final Property<Float>   _freeClimbingDiagonalDirectionAngle   = Positive(...).values(80F, 45F, 180F); // NW,SW,SE,NE
// 자동 트리거
public final Property<Boolean> _freeClimbingAutoLaddder = Unmodified("move.climb.free.ladder.auto").depends(_isFreeBaseClimb);
public final Property<Boolean> _freeClimbingAutoVine    = Unmodified("move.climb.free.vine.auto").depends(_isFreeBaseClimb);
// 래더 속도 배율
public final Property<Float>   _freeOneLadderClimbUpSpeedFactor  = PositiveFactor(...).defaults(1.0153F).defaults(0.71F, _pre_sm_2_4);
public final Property<Float>   _freeBothLadderClimbUpSpeedFactor = IncreasingFactor(...).defaults(1.43F); // >= 1
// 담장 클라이밍
public final Property<Boolean> _freeFenceClimbing = Unmodified("move.climb.free.fence");
```

```java
// 클라이밍 낙하 데미지
public final Property<Float>   _freeClimbFallDamageStartDistance = Positive(...).values(2F, 1F, 3F); // default 2F, min 1F, max 3F
public final Property<Float>   _freeClimbFallDamageFactor        = IncreasingFactor(...).defaults(2F); // >= 1
public final Property<Float>   _freeClimbFallMaximumDistance     = Positive(...).defaults(3F).min(_freeClimbFallDamageStartDistance);
```

```java
// 클라이밍 소진
public final Property<Boolean> _climbExhaustion          = Hard("move.climb.exhaustion");
public final Property<Float>   _climbExhaustionStart      = Positive(...).defaults(60F).defaults(0F, _pre_sm_1_4);
public final Property<Float>   _climbExhaustionStop       = Positive(...).up(80F, _climbExhaustionStart).defaults(100F, _pre_sm_1_4);
public final Property<Float>   _climbStrafeExhaustionGain = Positive(...).defaults(1.1F).defaults(0.75F, _pre_sm_1_4);
public final Property<Float>   _climbUpExhaustionGain     = Positive(...).defaults(1.2F).defaults(1F, _pre_sm_1_4);
public final Property<Float>   _climbDownExhaustionGain   = Positive(...).defaults(1.05F).defaults(0.5F, _pre_sm_1_4);
public final Property<Float>   _climbStrafeUpExhaustionGain   = Positive(...).defaults(1.3F);
public final Property<Float>   _climbStrafeDownExhaustionGain = Positive(...).defaults(1.25F);
```

---

### 4. Ceiling Climbing

```java
public final Property<Boolean>  _ceilingClimbing            = Unmodified("move.climb.ceiling");
public final Property<Float>    _ceilingClimbingSpeedFactor = PositiveFactor(...).defaults(0.2F);
// 블록 설정: 문자열 → Dictionary<Object, Set<Integer>>
public final Property<String[]> _ceilingClimbConfigurationString = Strings(...).defaults(new String[] { "tile.fenceIron", "tile.trapdoor/0/1/2/3", "tile.trapdoor_iron/0/1/2/3" });
public final Property<Dictionary<Object, Set<Integer>>> _ceilingClimbConfigurationObject = _ceilingClimbConfigurationString.toBlockConfig();
// 소진
public final Property<Boolean>  _ceilingClimbExhaustion          = Hard("move.climb.ceiling.exhaustion");
public final Property<Float>    _ceilingClimbExhaustionStart      = Positive(...).defaults(40F).defaults(0F, _pre_sm_1_4);
public final Property<Float>    _ceilingClimbExhaustionStop       = Positive(...).up(60F, _ceilingClimbExhaustionStart).defaults(100F, _pre_sm_1_4);
public final Property<Float>    _ceilingClimbExhaustionGain       = Positive(...).defaults(1.3F).defaults(2F, _pre_sm_1_4);
```

`_ceilingClimbConfigurationString.toBlockConfig()`: 문자열 배열을 블록ID/이름 → 메타 Set 딕셔너리로 변환 (SmartMovingProperties에서 제공).

---

### 5. Swimming / Diving / Lava

```java
// 수영
public final Property<Boolean> _swim                    = Unmodified().key("move.swim").key("climbing.swim", _all_old);
public final Property<Float>   _swimSpeedFactor         = PositiveFactor("move.swim.speed.factor");
public final Property<Boolean> _swimDownOnSneak         = Unmodified().key("move.swim.down.sneak").defaults(false, _pre_sm_1_6);
public final Property<Float>   _swimParticlePeriodFactor = PositiveFactor("move.swim.particle.period.factor");
// 다이빙
public final Property<Boolean> _dive                   = Unmodified().key("move.dive").key("climbing.dive", _all_old);
public final Property<Float>   _diveSpeedFactor        = PositiveFactor("move.dive.speed.factor");
public final Property<Boolean> _diveDownOnSneak        = Unmodified().key("move.dive.down.sneak").defaults(false, _pre_sm_1_6);
// 용암
public final Property<Boolean> _lavaLikeWater              = Creative("move.lava.water");
public final Property<Float>   _lavaSwimParticlePeriodFactor = PositiveFactor("move.lava.swim.particle.period.factor").defaults(4F);
```

`.key("climbing.swim", _all_old)` 패턴 — 구버전(0.1, 0.2)에서 다른 키를 사용했음을 나타냄.

---

### 6. Running (Standard Sprint)

```java
public final Property<Boolean> _run                      = Unmodified("move.run");
public final Property<Float>   _runFactor                = PositiveFactor("move.run.factor").defaults(1.3F).min(1.1F);
public final Property<Float>   _runFactorLevitate        = PositiveFactor("move.run.factor.levitate").defaults(Value(1.3F).c(2.0F)).min(1.1F);
public final Property<Boolean> _runExhaustion            = Hard("move.run.exhaustion").depends(_run);
public final Property<Float>   _runExhaustionStart       = Positive("move.exhaustion.run.start").defaults(75F);
public final Property<Float>   _runExhaustionStop        = Positive("move.exhaustion.run.stop").up(100F, _runExhaustionStart);
public final Property<Float>   _runExhaustionGainFactor  = Positive("move.exhaustion.run.gain.factor").defaults(1.5F);
```

---

### 7. Sprinting (Generic Sprint)

```java
public final Property<Boolean> _sprint                          = Unmodified("move.sprint");
public final Property<Float>   _sprintFactor                   = PositiveFactor(...).defaults(1.5F).min(_run.eitherOr(_runFactor.plus(0.1F), 1.1F));
public final Property<Float>   _sprintFactorLevitate           = PositiveFactor(...).defaults(Value(1.5F).c(3F)).min(_run.eitherOr(_runFactorLevitate.plus(0.1F), 1.1F));
public final Property<Float>   _sprintFactorLevitateVertical   = PositiveFactor(...).defaults(0.185F);
public final Property<Boolean> _sprintExhaustion               = Medium("move.sprint.exhaustion").depends(_sprint);
public final Property<Boolean> _sprintEnableStanding           = Unmodified("move.sprint.enable.ground");
public final Property<Float>   _sprintExhaustionStart          = Positive(...).defaults(50F);
public final Property<Float>   _sprintExhaustionStop           = Positive(...).up(100F, _sprintExhaustionStart);
public final Property<Float>   _sprintExhaustionGainFactor     = IncreasingFactor(...).defaults(2F);
```

`_run.eitherOr(A, B)`: `_run.value == true`이면 A, false이면 B — sprint의 최소값이 run 활성화 여부에 따라 달라짐.

---

### 8. Sneaking / Crawling / Sliding / Flying / Levitating / Falling

```java
// 잠행
public final Property<Boolean> _sneak      = Unmodified("move.sneak");
public final Property<Float>   _sneakFactor = DecreasingFactor("move.sneak.factor").defaults(0.3F); // <= 1
public final Property<Boolean> _sneakNameTag = Modified("move.sneak.name");
// 기어가기
public final Property<Boolean> _crawl         = Unmodified("move.crawl");
public final Property<Float>   _crawlFactor   = DecreasingFactor("move.crawl.factor").defaults(0.15F);
public final Property<Boolean> _crawlNameTag  = Modified("move.crawl.name");
public final Property<Boolean> _crawlOverEdge = Unmodified("move.crawl.edge");
// 슬라이딩
public final Property<Boolean> _slide                    = Unmodified("move.slide");
public final Property<Float>   _slideControlDegrees      = PositiveFactor("move.slide.control.angle").defaults(1F);
public final Property<Float>   _slideSlipperinessFactor  = PositiveFactor("move.slide.glide.factor");
public final Property<Float>   _slidingSpeedStopFactor   = PositiveFactor("move.slide.speed.stop.factor");
public final Property<Float>   _slideParticlePeriodFactor = PositiveFactor(...).defaults(0.5F).defaults(1F, _pre_sm_1_6);
// 스마트 비행
public final Property<Boolean> _fly              = Unmodified("move.fly");
public final Property<Float>   _flyingSpeedFactor = PositiveFactor("move.fly.speed.factor");
// 바닐라 비행(levitate)
public final Property<Boolean> _levitateSmall     = Unmodified("move.levitate.small");
public final Property<Boolean> _levitateAnimation = Unmodified("move.levitate.animation").key("move.fly.animation");
// 낙하
public final Property<Float>   _fallingDistanceMinimum       = Positive("move.fall.distance.minimum").defaults(3F);
public final Property<Boolean> _fallAnimation                 = Unmodified("move.fall.animation");
public final Property<Float>   _fallAnimationDistanceMinimum = Positive("move.fall.animation.distance.minimum").min(_fallingDistanceMinimum).defaults(3F, _pre_sm_1_6);
```

---

### 9. Jumping

#### 기본 점프

```java
public final Property<Boolean> _jump               = Unmodified("move.jump");
public final Property<Float>   _jumpControlFactor  = DecreasingFactor("move.jump.control.factor").defaults(1F); // <= 1
public final Property<Float>   _jumpHorizontalFactor = IncreasingFactor("move.jump.horizontal.factor"); // >= 1
public final Property<Float>   _jumpVerticalFactor   = PositiveFactor("move.jump.vertical.factor");
```

#### 이동 상태별 점프 (각각 `enabled + horizontal/vertical factor`)

| 필드 접두사 | 설명 |
|-------------|------|
| `_standJump` | 서있을 때 점프 |
| `_sneakJump` | 잠행 중 점프 |
| `_walkJump` | 걷기 중 점프 |
| `_runJump` | 달리기(run) 중 점프 |
| `_sprintJump` | 질주(sprint) 중 점프 |

각 타입: `.depends(이동여부, _jump)`, 수평/수직 배율 Property 각 1개.

`_runJumpHorizontalFactor` 기본값 2F, `_sprintJumpHorizontalFactor` 기본값 2F — 나머지는 기본값 미지정(SmartMovingProperties 기본 사용).

#### 차지 점프

```java
public final Property<Boolean> _jumpCharge               = Unmodified("move.jump.charge").depends(_jump);
public final Property<Float>   _jumpChargeMaximum        = Positive("move.jump.charge.maximum").defaults(20F);
public final Property<Float>   _jumpChargeFactor         = IncreasingFactor("move.jump.charge.factor").defaults(1.3F); // >= 1
public final Property<Boolean> _jumpChargeCancelOnSneakRelease = Modified("move.jump.charge.sneak.release.cancel");
```

#### 헤드 점프

```java
public final Property<Boolean> _headJump               = Unmodified("move.jump.head.charge").key("move.forward.jump.charge", _pre_sm_1_8).depends(_jump);
public final Property<Float>   _headJumpControlFactor  = DecreasingFactor(...).key("move.forward.jump.control.factor", _pre_sm_1_8).defaults(0.2F);
public final Property<Float>   _headJumpChargeMaximum  = Positive(...).key("move.forward.jump.charge.maximum", _pre_sm_1_8).defaults(10F);
public final Property<Float>   _headFallDamageStartDistance = Positive(...).key("move.forward.fall.damage.start.distance", _pre_sm_1_8).values(2F, 1F, 3F);
public final Property<Float>   _headFallDamageFactor        = IncreasingFactor(...).key("move.forward.fall.damage.factor", _pre_sm_1_8).defaults(2F);
```

#### 각도 점프 (Side / Back)

```java
public final Property<Boolean> _angleJumpSide             = Unmodified("move.jump.angle.side");
public final Property<Boolean> _angleJumpBack             = Unmodified("move.jump.angle.back");
public final Property<Float>   _angleJumpHorizontalFactor = PositiveFactor(...).defaults(0.3F).defaults(0.4F, _sm_1_3);
public final Property<Float>   _angleJumpVerticalFactor   = PositiveFactor(...).defaults(0.2F);
```

#### 클라이밍 점프 (Climb Up / Back Up / Back Head)

```java
// Climb Up Jump
public final Property<Boolean> _climbUpJump                        = Unmodified("move.jump.climb.up");
public final Property<Float>   _climbUpJumpVerticalFactor          = DecreasingFactor(...); // <= 1
public final Property<Float>   _climbUpJumpHandsOnlyVerticalFactor = DecreasingFactor(...).defaults(0.8F);
// Climb Back Up Jump
public final Property<Boolean> _climbBackUpJump                         = Unmodified("move.jump.climb.back.up");
public final Property<Float>   _climbBackUpJumpVerticalFactor           = DecreasingFactor(...).defaults(0.2F).defaults(1F, _pre_sm_3_1);
public final Property<Float>   _climbBackUpJumpHorizontalFactor         = DecreasingFactor(...).defaults(0.3F).defaults(1F, _pre_sm_3_1);
public final Property<Float>   _climbBackUpJumpHandsOnlyVerticalFactor  = DecreasingFactor(...).defaults(0.8F);
public final Property<Float>   _climbBackUpJumpHandsOnlyHorizontalFactor = DecreasingFactor(...);
// Climb Back Head Jump
public final Property<Boolean> _climbBackHeadJump                         = Unmodified("move.jump.climb.back.head");
public final Property<Float>   _climbBackHeadJumpVerticalFactor           = DecreasingFactor(...).defaults(0.2F).defaults(1F, _pre_sm_3_1);
public final Property<Float>   _climbBackHeadJumpHorizontalFactor         = DecreasingFactor(...).defaults(0.3F).defaults(1F, _pre_sm_3_1);
public final Property<Float>   _climbBackHeadJumpHandsOnlyVerticalFactor  = DecreasingFactor(...).defaults(0.8F);
public final Property<Float>   _climbBackHeadJumpHandsOnlyHorizontalFactor = DecreasingFactor(...);
```

#### 벽 점프 (Wall Up / Wall Head)

```java
public final Property<Boolean> _wallUpJump                    = Unmodified("move.jump.wall");
public final Property<Float>   _wallUpJumpVerticalFactor      = DecreasingFactor(...).defaults(0.4F);
public final Property<Float>   _wallUpJumpHorizontalFactor    = DecreasingFactor(...).defaults(0.15F);
public final Property<Float>   _wallUpJumpFallMaximumDistance = Positive(...).defaults(2F);
public final Property<Float>   _wallUpJumpOrthogonalTolerance = Positive(...).defaults(5F); // <= 45°

public final Property<Boolean> _wallHeadJump                    = Unmodified("move.jump.wall.head");
public final Property<Float>   _wallHeadJumpVerticalFactor      = DecreasingFactor(...).defaults(0.3F);
public final Property<Float>   _wallHeadJumpHorizontalFactor    = DecreasingFactor(...).defaults(0.15F);
public final Property<Float>   _wallHeadJumpFallMaximumDistance = Positive(...).defaults(3F).min(_wallUpJumpFallMaximumDistance);
```

---

### 10. 점프 소진 (구버전 마이그레이션용)

`_pre_sm_1_7` 버전 전용, 모두 `private`:

```java
private final Property<Boolean> _old_jumpExhaustion;
private final Property<Float>   _old_jumpExhaustionGain;      // defaults 40F (pre_1_4: 10F)
private final Property<Float>   _old_jumpExhaustionStop;      // defaults 60F (pre_1_4: 100F)
private final Property<Boolean> _old_sneakJumpExhaustion;
private final Property<Float>   _old_sneakJumpExhaustionGain; // down(45F, _old_jumpExhaustionGain)
private final Property<Float>   _old_sneakJumpExhaustionStop; // up(55F, _old_jumpExhaustionStop)
private final Property<Boolean> _old_runJumpExhaustion;
private final Property<Float>   _old_runJumpExhaustionGain;   // defaults 60F
private final Property<Float>   _old_runJumpExhaustionStop;   // defaults 40F
private final Property<Boolean> _old_sprintJumpExhaustion;
private final Property<Float>   _old_sprintJumpExhaustionGain; // up(65F, _old_runJumpExhaustionGain)
private final Property<Float>   _old_sprintJumpExhaustionStop; // down(35F, _old_runJumpExhaustionStop)
```

현재 버전 점프 소진에서 `.source(_old_XXX, _pre_sm_1_7)` 체인으로 구버전 파일 값을 마이그레이션.

---

### 11. 점프 소진 (현재)

```java
// 공통
public final Property<Boolean> _jumpExhaustion          = Unmodified("move.jump.exhaustion");
public final Property<Float>   _jumpExhaustionGainFactor = PositiveFactor(...);
public final Property<Float>   _jumpExhaustionStopFactor = PositiveFactor(...);

// Up Jump
public final Property<Boolean> _upJumpExhaustion          = Unmodified("move.jump.up.exhaustion").depends(_jumpExhaustion);
public final Property<Float>   _upJumpExhaustionGainFactor = PositiveFactor(...);
public final Property<Float>   _upJumpExhaustionStopFactor = PositiveFactor(...);

// Climb Jump
public final Property<Boolean> _climbJumpExhaustion          = Hard("move.jump.climb.exhaustion").depends(_jumpExhaustion);
public final Property<Float>   _climbJumpExhaustionGainFactor = PositiveFactor(...);
public final Property<Float>   _climbJumpExhaustionStopFactor = PositiveFactor(...);

// 세부: climbJumpUp, climbJumpBackUp, climbJumpBackHead (각각 enabled + Gain/Stop factor)
// climbJumpUpExhaustionGainFactor defaults 40F (pre_sm_3_1: 1F), StopFactor defaults 60F
// climbJumpBackUpExhaustionGainFactor defaults 40F (pre_sm_3_1: 1F), StopFactor defaults 60F
// climbJumpBackHeadExhaustionGainFactor defaults 20F (pre_sm_3_1: 1F), StopFactor defaults 80F

// Angle Jump
public final Property<Boolean> _angleJumpExhaustion          = Unmodified("move.jump.angle.exhaustion").depends(_jumpExhaustion);
public final Property<Float>   _angleJumpExhaustionGainFactor = PositiveFactor(...);
public final Property<Float>   _angleJumpExhaustionStopFactor = PositiveFactor(...);

// Wall Jump
public final Property<Boolean> _wallJumpExhaustion          = Medium("move.jump.wall.exhaustion").depends(_jumpExhaustion);
public final Property<Float>   _wallJumpExhaustionGainFactor = PositiveFactor(...);
public final Property<Float>   _wallJumpExhaustionStopFactor = PositiveFactor(...);

// 세부: wallUpJump (Gain 40F, Stop 60F), wallHeadJump (Gain 20F, Stop 80F)

// Stand / Sneak / Walk / Run / Sprint Jump (각각 enabled + Gain/Stop)
// .source(_old_XXX, _pre_sm_1_7) 로 구버전 값 마이그레이션
// gain 계층: stand(40F) <= sneak(40F up) <= walk(45F up) <= run(60F up) <= sprint(65F up)
// stop 계층: stand(60F) >= sneak(60F down) >= walk(55F down) >= run(40F down) >= sprint(35F down)

// Charge
public final Property<Boolean> _jumpChargeExhaustion          = Medium("move.jump.charge.exhaustion").depends(_jumpExhaustion).defaults(true, _pre_sm_1_11);
public final Property<Float>   _jumpChargeExhaustionGainFactor = PositiveFactor(...).defaults(Value(30F));
public final Property<Float>   _jumpChargeExhaustionStopFactor = PositiveFactor(...).defaults(Value(30F));

// Slide Jump
public final Property<Boolean> _jumpSlideExhaustion          = Medium("move.jump.slide.exhaustion").depends(_jumpExhaustion).defaults(true, _pre_sm_1_11);
public final Property<Float>   _jumpSlideExhaustionGainFactor = PositiveFactor(...).defaults(Value(10F));
public final Property<Float>   _jumpSlideExhaustionStopFactor = PositiveFactor(...).defaults(Value(90F));
```

---

### 12. Exhaustion (소진 전반)

```java
// gain/loss base factor
public final Property<Float> _baseExhautionGainFactor = PositiveFactor("move.exhaustion.gain.factor");
public final Property<Float> _baseExhautionLossFactor = PositiveFactor("move.exhaustion.loss.factor").defaults(Value(1F).e(1.2F).h(0.8F)).defaults(1F, _pre_sm_1_5);

// 이동 상태별 loss factor (낮을수록 소진 느리게 회복)
// Sprinting(0F) <= Running(0.5F up) <= Walking(1F up) <= Sneaking(1.5F up) <= Standing(2F up, >= 1) <= Falling(2.5F up)
public final Property<Float> _sprintingExhautionLossFactor = PositiveFactor(...).defaults(0F);
public final Property<Float> _runningExhautionLossFactor   = PositiveFactor(...).up(0.5F, _sprintingExhautionLossFactor);
public final Property<Float> _walkingExhautionLossFactor   = PositiveFactor(...).up(1F, _runningExhautionLossFactor);
public final Property<Float> _sneakingExhautionLossFactor  = PositiveFactor(...).up(1.5F, _walkingExhautionLossFactor).defaults(1F, _sm_1_1);
public final Property<Float> _standingExhautionLossFactor  = PositiveFactor(...).up(2F, _sneakingExhautionLossFactor.maximum(1F)); // minimum 1
public final Property<Float> _fallExhautionLossFactor      = PositiveFactor(...).up(2.5F, _standingExhautionLossFactor);

// 행동 상태별 loss factor
public final Property<Float> _ceilClimbingExhaustionLossFactor = PositiveFactor(...);
public final Property<Float> _climbingExhaustionLossFactor     = PositiveFactor(...);
public final Property<Float> _crawlingExhaustionLossFactor     = PositiveFactor(...);
public final Property<Float> _dippingExhaustionLossFactor      = PositiveFactor(...);
public final Property<Float> _swimmingExhaustionLossFactor     = PositiveFactor(...);
public final Property<Float> _divingExhaustionLossFactor       = PositiveFactor(...);
public final Property<Float> _normalExhaustionLossFactor       = PositiveFactor(...);

// hunger 연동
public final Property<Boolean> _exhaustionLossHunger        = Unmodified("move.exhaustion.hunger");
public final Property<Float>   _exhaustionLossHungerFactor  = PositiveFactor(...).defaults(Value(0.05F).e(0.02F).h(0.08F)).defaults(0.05F, _pre_sm_1_5);
public final Property<Float>   _exhaustionLossFoodLevelMinimum = Positive(...).defaults(4F);
```

---

### 13. Hunger

```java
public final Property<Boolean> _hungerGain            = Medium("move.hunger.gain");
public final Property<Float>   _baseHungerGainFactor  = PositiveFactor(...).defaults(Value(1F).e(0.8F).h(1.2F)).defaults(1F, _pre_sm_1_5);

// 이동 상태별 gain factor
public final Property<Float> _sprintingHungerGainFactor = PositiveFactor(...).key(..., _pre_sm_1_3);
public final Property<Float> _runningHungerGainFactor   = PositiveFactor(...).defaults(10F);
public final Property<Float> _walkingHungerGainFactor   = PositiveFactor(...);
public final Property<Float> _sneakingHungerGainFactor  = PositiveFactor(...);
public final Property<Float> _standingHungerGainFactor  = PositiveFactor(...).defaults(0F);

// 행동 상태별 gain factor
public final Property<Float> _climbingHungerGainFactor     = PositiveFactor(...);
public final Property<Float> _crawlingHungerGainFactor     = PositiveFactor(...);
public final Property<Float> _ceilClimbingHungerGainFactor = PositiveFactor(...);
public final Property<Float> _swimmingHungerGainFactor     = PositiveFactor(...).defaults(1.5F);
public final Property<Float> _divingHungerGainFactor       = PositiveFactor(...).defaults(1.5F);
public final Property<Float> _dippingHungerGainFactor      = PositiveFactor(...).defaults(1.5F);
public final Property<Float> _normalHungerGainFactor       = PositiveFactor(...);

public final Property<Float> _alwaysHungerGain = Positive(...).defaults(Value(0F).h(0.005F)).defaults(0F, _pre_sm_1_5);
```

---

### 14. Item Usage

```java
public final Property<Float>   _usageSpeedFactor         = DecreasingFactor(...).defaults(0.2F); // 기본 아이템 사용 중 속도
public final Property<Float>   _usageSwordSpeedFactor    = DecreasingFactor(...).defaults(_usageSpeedFactor); // 기본값 = _usageSpeedFactor
public final Property<Float>   _usageBowSpeedFactor      = DecreasingFactor(...).defaults(_usageSpeedFactor);
public final Property<Float>   _usageFoodSpeedFactor     = DecreasingFactor(...).defaults(_usageSpeedFactor);
public final Property<Boolean> _sprintDuringItemUsage   = Modified("move.usage.sprint");
```

`.defaults(_usageSpeedFactor)` — 다른 Property를 기본값으로 참조.

---

### 15. Mod Compatibility

```java
public final Property<Boolean> _replaceRopeClimbing = Unmodified("move.mod.rope.replace.climb.rope");
```

Ropes+ 모드의 로프 클라이밍 대체 여부.

---

### 16. Configuration Management

```java
// survival / creative / adventure별 config key 목록과 기본값
// (키 이름은 Agent WebFetch 세션 2 에서 확인 — originSource L456-L463)
public final Property<String[]> _survivalConfigKeys       = Strings("move.config.survival.keys").singular().defaults(new String[]{"e", "m", "h"});
public final Property<String>   _survivalDefaultConfigKey = String("move.config.survival.keys.default").singular().defaults("m");

public final Property<String[]> _creativeConfigKeys       = Strings("move.config.creative.keys").singular().defaults(new String[]{"c"}).defaults(new String[0], _pre_sm_2_3);
public final Property<String>   _creativeDefaultConfigKey = String("move.config.creative.keys.default").singular().defaults("c").defaults("", _pre_sm_2_3);

public final Property<String[]> _adventureConfigKeys       = Strings("move.config.adventure.keys").singular().defaults(new String[]{"e", "m", "h"});
public final Property<String>   _adventureDefaultConfigKey = String("move.config.adventure.keys.default").singular().defaults("m");

// key별 표시 이름: e=Easy, m=Medium, h=Hard
public final Property<String> _configKeyName = String(...).defaults(Value((String)null).e("Easy").m("Medium").h("Hard"));
```

**Property `Strings` 직렬화 포맷**: `comment("... entries seperated by ','")` 명시 — CSV (쉼표 구분).

`.singular()` — 설정 파일에서 하나의 값만 허용(config key별 다중값 없음).

---

### 17. Server Management

```java
public final Property<Boolean>            _serverConfig                  = Modified("move.server.config").singular();
public final Property<Map<String,String>> _survivalDefaultConfigUserKeys = StringMap(...).singular();
public final Property<Map<String,String>> _creativeDefaultConfigUserKeys = StringMap(...).singular();
public final Property<Map<String,String>> _adventureDefaultConfigUserKeys = StringMap(...).singular();
public final Property<Map<String,Integer>> _speedUsersExponents          = IntegerMap(...).singular();
public final Property<Boolean>            _globalConfig                  = Modified(...).key("move.config.overwrite", _pre_sm_3_0).key("move.config.send", _pre_sm_2_2).singular().depends(_serverConfig);
public final Property<String[]>           _usersWithChangeConfigRights   = Strings(...).singular();
public final Property<String[]>           _usersWithChangeSpeedRights    = Strings(...).singular();
```

---

## 메서드

### `changeSpeed(int difference)`

```java
public void changeSpeed(int difference)
{
    _speedUserExponent.setValue(_speedUserExponent.value + difference);
}
```

인게임 속도 조정 UI에서 호출. exponent를 ±1씩 변경.

---

### `isUserSpeedEnabled()`

```java
public boolean isUserSpeedEnabled()
{
    return enabled && _speedUser.value;
}
```

`enabled`: 상위 `SmartMovingContext.enabled` — SmartMoving이 활성화된 경우만 인게임 속도 조정 사용 가능.

---

### `isUserSpeedAlwaysDefault()`

```java
public boolean isUserSpeedAlwaysDefault()
{
    return !_speedUser.value || _speedUserFactor.value == 1F;
}
```

속도 조정 비활성이거나 factor=1일 때 true → exponent 무관하게 배율 1F.

---

### `getUserSpeedFactor()`

```java
public float getUserSpeedFactor()
{
    if(isUserSpeedAlwaysDefault() || _speedUserExponent.value == 0)
        return 1F;
    return (float)Math.pow(1F + _speedUserFactor.value, _speedUserExponent.value);
}
```

**공식**: `(1 + factor)^exponent`  
- factor=0.2F, exponent=3 → `(1.2)^3 = 1.728F`  
- exponent<0 → 속도 감소

---

### `loadFromProperties(Properties properties)`

```java
protected void loadFromProperties(Properties properties)
{
    try { load(properties); }
    catch(Exception e) { throw new RuntimeException("Could not load Smart Moving properties from properties", e); }
}
```

단일 Properties 객체에서 로드 (서버→클라이언트 전송된 설정 적용 시 사용).

---

### `writeToProperties(Properties properties, List<Property<?>> except)`

```java
protected void writeToProperties(Properties properties, List<Property<?>> except)
{
    try
    {
        write(properties);
        if(except != null)
            for(int i = 0; i < except.size(); i++)
            {
                String key = except.get(i).getCurrentKey();
                if(key != null)
                    properties.remove(key);
            }
    }
    catch(Exception e) { throw new RuntimeException(..., e); }
}
```

전체 write 후 `except` 목록의 키를 제거 — 서버로 보낼 때 클라이언트 전용 설정 제외.

---

### `loadFromOptionsFile(File optionsPath)`

```java
protected void loadFromOptionsFile(File optionsPath)
{
    Properties slcs_properties = new Properties(_slcs, new File(optionsPath, _smartLadderClimbingSpeedPropertiesFileName));
    Properties sc_properties   = new Properties(_sc,   new File(optionsPath, _smartClimbingPropertiesFileName));
    Properties sm_cs_properties = new Properties(      new File(optionsPath, _smartMovingClientServerPropertiesFileName));
    Properties properties       = new Properties(      new File(optionsPath, _smartMovingPropertiesFileName));

    try { load(properties, sm_cs_properties, sc_properties, slcs_properties); }
    catch(Exception e) { throw new RuntimeException(..., e); }
}
```

4개 파일 동시 로드:
1. `smart_moving_options.txt` (현재 메인, 버전 없음)
2. `smart_moving_server_options.txt` (서버 공유, 버전 없음)
3. `smart_climbing_options.txt` (v0.2 구버전)
4. `smart_ladder_climbing_speed_options.txt` (v0.1 구버전)

`Properties(version, file)` 생성자 — 해당 버전 파일에서 읽음.

---

### `saveToOptionsFile(File optionsPath)`

```java
protected void saveToOptionsFile(File optionsPath)
{
    try { save(new File(optionsPath, _smartMovingPropertiesFileName), _sm_current, true, true); }
    catch(Exception e) { throw new RuntimeException(..., e); }
}
```

저장은 `smart_moving_options.txt` 하나에만, 버전 `_sm_current`(= "3.2")로 저장.

---

### `printHeader(PrintWriter printer)`

```java
@Override
protected void printHeader(PrintWriter printer)
{
    printer.println("#######################################################################");
    printer.println("# Smart Moving mod configuration file");
    // ... 설정 파일 헤더 주석 (키-값 구분자 ':', 쌍 구분자 ';', '!' 마크 설명 등)
    printer.println("#######################################################################");
}
```

`SmartMovingProperties.write()` → `printHeader()` 호출 시 설정 파일 상단 주석 출력.

---

### `printVersion(PrintWriter printer, String version, boolean comments)`

```java
@Override
protected void printVersion(PrintWriter printer, String version, boolean comments)
{
    if(comments)
        printer.println("# The current version of this Smart Moving options file");
    printer.print("move.options.version");
    printer.print(":");
    printer.print(version);
    printer.println();
}
```

`move.options.version:3.2` 형식으로 출력.

---

## private static 메서드 (Property 팩토리 헬퍼)

```java
private static Property<Boolean> Creative(String key, String... versions)
{
    return Modified(key, versions).defaults(Value(false).c(true));
    // creative 게임 모드일 때 true, 나머지 false
}

private static Property<Boolean> Hard(String key, String... versions)
{
    return Modified(key, versions).defaults(Value(false).h(true)).defaults(false, _pre_sm_1_5);
    // hard 난이도일 때 true, 나머지 false, v1.5 이전은 false
}

private static Property<Boolean> Medium(String key, String... versions)
{
    return Unmodified(key, versions).defaults(Value(true).e(false)).defaults(true, _pre_sm_1_5);
    // easy 난이도일 때 false, 나머지 true, v1.5 이전은 true
}
```

`Value(default).c(creativeValue).e(easyValue).h(hardValue).m(mediumValue)` — 설정 키별 게임 모드/난이도별 기본값.

---

## 게임 모드 상수

```java
protected final static int Unknown  = -1;
protected final static int Survival = 0;
protected final static int Creative = 1;
protected final static int Adventure = 2;
```

서브클래스에서 현재 게임 모드 분기에 사용.

---

### `getSpeedPercent()`

```java
public Object getSpeedPercent()
{
    float factor = getUserSpeedFactor() * 100F;
    int fraction = 0;
    while(factor < 100F)
    {
        fraction++;
        factor *= 10F;
    }
    int significant = Math.round(factor);
    BigDecimal decimal = new BigDecimal(significant);
    while(fraction-- > 0)
        decimal = decimal.divide(ten);
    return formatter.format(decimal);
}

private final static DecimalFormat formatter = new DecimalFormat("0.############################################################");
private final static BigDecimal ten = new BigDecimal(10);
public final static Object defaultSpeedPercent = "100";
```

`getUserSpeedFactor() * 100F`를 BigDecimal로 소수점 정밀도 유지하여 문자열 반환.  
factor < 100F일 때 10 곱셈 반복 → 부동소수점 오차 최소화.  
`defaultSpeedPercent = "100"` — 속도 변경 없을 때 UI 기본 표시값.

---

## Property 팩토리 메서드 체인 패턴 정리

| 메서드 | 의미 |
|--------|------|
| `Modified(key)` | 명시적으로 수정된 설정 (파일에 '!' 마크) |
| `Unmodified(key)` | 기본값 추적 설정 (기본값이 업데이트되면 자동 반영) |
| `PositiveFactor(key)` | `>= 0` 배율 |
| `IncreasingFactor(key)` | `>= 1` 배율 |
| `DecreasingFactor(key)` | `<= 1` 배율 |
| `Positive(key)` | `>= 0` 수치 |
| `.defaults(value)` | 현재 버전 기본값 |
| `.defaults(value, versions[])` | 지정 버전들에서의 기본값 |
| `.key(key, versions[])` | 구버전에서 다른 키 이름 |
| `.source(prop, versions[])` | 구버전에서 다른 Property 값 사용 |
| `.depends(props...)` | 다른 Property가 비활성이면 이 Property도 비활성 |
| `.singular()` | config key 시스템 밖의 단일 설정 |
| `.min(value/prop)` | 최솟값 제약 |
| `.up(value, prop)` | >= prop 제약 (default도 value) |
| `.down(value, prop)` | <= prop 제약 (default도 value) |
| `.values(default, min, max)` | 기본값+범위 한 번에 |
| `.comment(str)` | 설정 파일 주석 |
| `.book(title, desc)` | 설정 그룹(섹션) 시작 |
| `.chapter(title, desc)` | 하위 섹션 시작 |
| `.section()` | 더 작은 단위 구분 |
| `.is(str)` | 문자열 Property를 Boolean으로 변환 |
| `.and(prop)` / `.andNot(prop)` / `.not()` | Boolean Property 조합 |
| `.eitherOr(A, B)` | 조건부 값 선택 |
| `.maximum(value)` | 상한 클램프 |
| `.toBlockConfig()` | 문자열[] → `Dictionary<Object, Set<Integer>>` |

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingProperties` | 상속 — Property 팩토리 메서드, load/write/save 제공 |
| `net.smart.properties.Properties` | 파일 I/O |
| `net.smart.properties.Property<T>` | 설정 값 컨테이너 |
| `net.smart.properties.Value` | 게임 모드/난이도별 기본값 빌더 |

서브클래스: `SmartMovingClientConfig` (extends this), `SmartMovingServerConfig` (extends this).

---

## 주요 관찰 사항

1. **버전 마이그레이션 내장**: 모든 설정 필드에 이전 버전 키와 기본값이 체인으로 기록됨. Properties 시스템이 파일 버전을 읽어 자동 변환.

2. **`_pre_sm_X_Y` 누적 패턴**: `concat(version, previousArray)`로 누적. `Property.defaults(value, _pre_sm_X_Y)` 호출 시 해당 버전 이전 모든 버전에 적용.

3. **구버전 private 필드**: `_old_jumpExhaustion` 계열은 private으로 직접 접근 불가. `.source(_old_XXX, _pre_sm_1_7)`로만 현재 필드에 병합 — 마이그레이션 로직이 선언 자체에 내장.

4. **Computed Property**: `_isFreeBaseClimb = _baseClimb.is("free").and(_freeClimb)` — 두 값의 조합을 새 Property로 선언. `SmartMovingSelf` 등에서 직접 참조.

5. **`_ceilingClimbConfigurationObject`**: `.toBlockConfig()` — 문자열 파싱을 Property 레벨에서 자동 처리. 블록 ID 문자열 + 메타 → `Dictionary<Object, Set<Integer>>` 변환.

6. **`_usageSwordSpeedFactor.defaults(_usageSpeedFactor)`**: 다른 Property를 기본값으로 참조 — 파일에 해당 키가 없으면 `_usageSpeedFactor` 값을 사용.

7. **`_run.eitherOr(A, B)` 패턴**: `_sprintFactor.min(_run.eitherOr(_runFactor.plus(0.1F), 1.1F))` — run이 활성화된 경우 sprintFactor >= runFactor+0.1, 비활성이면 >= 1.1. 최솟값 제약이 다른 설정 상태에 동적으로 연동.

8. **파일 저장은 단일 파일**: `loadFromOptionsFile()`은 4개 파일을 모두 읽지만, `saveToOptionsFile()`은 현재 버전 파일 하나에만 저장. 구버전 파일 마이그레이션 후 신규 파일로 통합.

9. **1.21.1 이식 관련**:
   - Property 시스템 전체(`net.smart.properties`) 이식 필요
   - 버전 시스템은 SmartMoving 자체 설정 파일 포맷 버전이므로 Fabric 이식 시에도 유지 가능
   - 게임 모드 상수(`Survival=0, Creative=1, Adventure=2`)는 Fabric API 게임 모드와 별도 매핑 필요
   - `_ceilingClimbConfigurationString.toBlockConfig()` → Fabric 블록 레지스트리 기반으로 재구현 필요
