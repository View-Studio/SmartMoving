# 점프 강화 시스템 리서치

> 원본 소스: `SmartMovingSelf.java`, `SmartMovingConfig.java`

---

## 관련 클래스/메서드

| 위치 | 메서드 | 역할 |
|------|--------|------|
| `SmartMovingSelf` | `tryJump(type, inWater, isRunning, angle)` | 점프 실행 핵심 메서드 |
| `SmartMovingSelf` | `handleJumping()` | 매 틱 점프 타입 감지 |
| `SmartMovingSelf` | `handleWallJumping()` | 벽 점프 반사 계산 |
| `SmartMovingConfig` | `getJumpChargeFactor()` | 차지 점프 배수 계산 |
| `SmartMovingConfig` | `getHeadJumpFactor()` | 헤드 점프 각도 배수 |
| `SmartMovingConfig` | `getJumpVerticalFactor()` | 점프 수직 속도 배수 |
| `SmartMovingConfig` | `getJumpHorizontalFactor()` | 점프 수평 속도 배수 |

---

## A. 점프 타입 목록

```java
// 기본 점프
Up              = 0   // 바닐라 스타일
ChargeUp        = 1   // 차지 점프
Angle           = 2   // 8방향 점프
HeadUp          = 3   // 헤드 점프
SlideDown       = 4   // 슬라이딩 중 점프

// 클라이밍 점프
ClimbUp                 = 5
ClimbUpHandsOnly        = 6
ClimbBackUp             = 7
ClimbBackUpHandsOnly    = 8
ClimbBackHead           = 9
ClimbBackHeadHandsOnly  = 10

// 벽 점프
WallUp          = 11  // 기본 벽 점프
WallHead        = 12  // 헤드 벽 점프
WallUpSlide     = 13  // 미끄러지며 벽 점프
WallHeadSlide   = 14  // 헤드 + 슬라이드 벽 점프
```

---

## B. tryJump() 핵심 메서드

```java
public boolean tryJump(int type, Boolean inWaterOrNull,
                       Boolean isRunningOrNull, Float angle) {
    // 1. 탈진 체크
    float maxExhaustion = Config.getJumpExhaustionStop(speed, type, jumpCharge);
    if (exhaustion > maxExhaustion) return false;

    // 2. 수직/수평 배수 계산
    float verticalFactor   = Config.getJumpVerticalFactor(speed, type)   * jumpPotionFactor;
    float horizontalFactor = Config.getJumpHorizontalFactor(speed, type) * jumpPotionFactor;

    // 3. 기본 수직 속도 공식
    double verticalMotion = -0.078 + 0.498 * verticalFactor * jumpChargeFactor;

    // 4. 바닐라 호환 점프 (type == Up)
    if (type == Config.Up && vanilla()) {
        verticalMotion = 0.41999998688697815D;  // 바닐라 고정값
        // 점프 물약: += (amplifier + 1) * 0.1F
        // 스프린트: motionX -= sin(yaw)*0.2, motionZ += cos(yaw)*0.2
    }

    // 5. 각도 기반 수평 속도
    if (angle != null) {
        double rad = angle / RadiantToAngle;
        double moveX = -Math.sin(rad);
        double moveZ =  Math.cos(rad);
        sp.motionX = getJumpMoving(jumpMotionX, moveX, reset, horizontal, horizontalFactor);
        sp.motionZ = getJumpMoving(jumpMotionZ, moveZ, reset, horizontal, horizontalFactor);
    }

    // 6. 수평 속도 상한 적용
    if (horizontalFactor > 1F) {
        double maxH = Config.getMaxHorizontalMotion(...) * getCombinedSpeedFactor();
        sp.motionX = clamp(sp.motionX, -maxH * ratioX, maxH * ratioX);
        sp.motionZ = clamp(sp.motionZ, -maxH * ratioZ, maxH * ratioZ);
    }

    // 7. 수직 속도 적용
    sp.motionY = verticalMotion;

    // 8. 탈진 소모
    exhaustion += Config.getJumpExhaustionGain(speed, type, jumpCharge);

    return true;
}
```

---

## C. 기본 수직 속도 공식

```
motionY = -0.078 + 0.498 × verticalFactor × jumpChargeFactor

바닐라 (type=Up): motionY = 0.41999998688697815D (고정)
```

**점프 물약 적용:**
```java
float jumpPotionFactor = isPotionActive(Potion.jump)
    ? 1 + (getAmplifier() + 1) * 0.2F
    : 1F;
// 레벨 0 (Lv.1): × 1.2F
// 레벨 1 (Lv.2): × 1.4F
// 레벨 2 (Lv.3): × 1.6F
```

---

## D. 차지 점프 (ChargeUp)

### 충전 메커니즘
```java
// 조건 충족 시 매 틱 증가
if (isChargingJump && jumpButton.Pressed)
    jumpCharge++;
else if (jumpCharge > 0 && sp.onGround)
    tryJump(Config.ChargeUp, null, null, null); // 충전 해제 → 발동
```

### 배수 계산
```java
public float getJumpChargeFactor(float jumpCharge) {
    if (!_jumpCharge.value) return 1F;
    jumpCharge = Math.min(jumpCharge, _jumpChargeMaximum.value); // 최대 20틱
    return 1F + (jumpCharge / _jumpChargeMaximum.value) * (_jumpChargeFactor.value - 1F);
    //          └ 충전 비율 (0~1)     └ 추가 배수 (기본: 0.3)
}
// 기본값 결과:
// 0틱 충전:  × 1.0
// 10틱 충전: × 1.15
// 20틱 충전: × 1.3 (최대)
```

**기본 설정:**
- `_jumpChargeMaximum = 20F` (틱)
- `_jumpChargeFactor = 1.3F` (최대 배수)

---

## E. 헤드 점프 (HeadUp) — 궤적 수직화

### 충전 메커니즘
```java
// grabButton + 스프린트 + 점프 버튼 동시
if (grabButton.Pressed && isGroundSprinting && jumpButton.Pressed)
    headJumpCharge++;
else if (headJumpCharge > 0 && sp.onGround)
    tryJump(Config.HeadUp, null, null, null);
```

### 각도 변환 공식
```java
public float getHeadJumpFactor(float headJumpCharge) {
    if (!_headJump.value) return 1F;
    headJumpCharge = Math.min(headJumpCharge, _headJumpChargeMaximum.value); // 최대 10틱
    return (headJumpCharge - 1) / (_headJumpChargeMaximum.value - 1);
    // 0틱: 0.0F, 10틱: 1.0F
}

// 실제 적용 (tryJump 내부):
double normalAngle = Math.atan(verticalMotion / horizontalMotion);
double totalMotion = Math.sqrt(verticalMotion² + horizontalMotion²);
double newAngle    = headJumpFactor * normalAngle; // 각도를 수직 방향으로
double newVertical   = totalMotion * Math.sin(newAngle); // 수직 증가
double newHorizontal = totalMotion * Math.cos(newAngle); // 수평 감소
// 총 운동량(totalMotion) 보존, 방향만 위로 가파르게 변경
```

**기본 설정:** `_headJumpChargeMaximum = 10F`

---

## F. 8방향 각도 점프 (Angle Jump)

### 방향 인덱스 매핑
```java
angleJumpType = ((360 - angle) / 45) % 8;
```

| 인덱스 | 방향 | 각도 (yaw 기준) |
|--------|------|--------------|
| 0 | 전방 | 0° |
| 1 | 전방-우 | 45° |
| 2 | 우측 | 90° |
| 3 | 후방-우 | 135° |
| 4 | 후방 | 180° |
| 5 | 후방-좌 | 225° |
| 6 | 좌측 | 270° |
| 7 | 전방-좌 | 315° |

### 더블탭 감지
```java
// 왼쪽 방향키 더블탭 예시
if (leftButton.StartPressed) {
    if (leftJumpCount == 0)
        leftJumpCount = Options.angleJumpDoubleClickTicks(); // 3틱 카운트 시작
    else
        leftJumpCount = -1; // 두 번째 탭 → 점프 발동
}
// 매 틱 카운터 감소, 0 되면 리셋
if (leftJumpCount > 0) leftJumpCount--;
```

**기본 설정:** `_angleJumpDoubleClickTicks = 3F` (최소 2F)

### 속도 적용
```java
double rad = jumpAngle / RadiantToAngle;
sp.motionX = getJumpMoving(jumpMotionX, -Math.sin(rad), ...);
sp.motionZ = getJumpMoving(jumpMotionZ,  Math.cos(rad), ...);
// 수직: 일반 jump 공식 그대로
```

---

## G. 벽 점프 (Wall Jump)

### 반사 공식
```java
public void handleWallJumping() {
    if (Double.isNaN(horizontalCollisionAngle)) return;

    float jumpAngle;
    if (!wasCollidedHorizontally) {
        // 진입각 반사: 2 × 벽 법선 - 진입 방향
        float movementAngle = getAngle(jumpMotionZ, -jumpMotionX);
        jumpAngle = horizontalCollisionAngle * 2 - movementAngle + 180F;
    } else {
        jumpAngle = horizontalCollisionAngle; // 법선 방향 직접 반사
    }

    // 직각 정렬 옵션 (벽이 45도 이내면 90도로 스냅)
    if (Config._wallUpJumpOrthogonalTolerance.value != 0F) {
        float nearest90 = Math.round(jumpAngle / 90F) * 90F;
        if (Math.abs(jumpAngle - nearest90) < tolerance)
            jumpAngle = nearest90;
    }

    int jumpType = grabButton.Pressed
        ? (wasCollidedHorizontally ? Config.WallHeadSlide : Config.WallHead)
        : (wasCollidedHorizontally ? Config.WallUpSlide   : Config.WallUp);

    tryJump(jumpType, null, null, jumpAngle);
}
```

**필드:** `horizontalCollisionAngle` — 충돌 표면의 법선 각도 (degrees)

---

## H. 속도 배수 테이블 (이동 상태별)

### 수직 배수 (getJumpVerticalFactor)

| 이동 상태 | 설정값 |
|----------|--------|
| 서있음 (Standing) | `_standJumpVerticalFactor` |
| 걷기 (Walking) | `_walkJumpVerticalFactor` |
| 스니킹 (Sneaking) | `_sneakJumpVerticalFactor` |
| 달리기 (Running) | `_runJumpVerticalFactor` |
| 스프린트 (Sprinting) | `_sprintJumpVerticalFactor` |
| 각도 점프 | `_angleJumpVerticalFactor` |

### 수평 배수 (getJumpHorizontalFactor)

| 이동 상태 | 설정값 |
|----------|--------|
| 서있음 | `0F` (수평 부스트 없음) |
| 걷기 | `_walkJumpHorizontalFactor` |
| 달리기 | `_runJumpHorizontalFactor` |
| 스프린트 | `_sprintJumpHorizontalFactor` |
| 각도 점프 | `_angleJumpHorizontalFactor` |

---

## I. 탈진 게이트

```java
// 점프 가능 여부 확인
float maxExhaustion = Config.getJumpExhaustionStop(speed, type, jumpCharge);
if (exhaustion > maxExhaustion) return false; // 점프 차단

// 성공 시 탈진 소모
exhaustion += Config.getJumpExhaustionGain(speed, type, jumpCharge);
// 점프 타입/속도/충전량에 따라 소모량 다름
```

---

## J. 주요 설정 값

| 설정 | 기본값 | 설명 |
|------|--------|------|
| `_jumpChargeMaximum` | 20F | 차지 최대 틱 |
| `_jumpChargeFactor` | 1.3F | 최대 차지 배수 |
| `_headJumpChargeMaximum` | 10F | 헤드 점프 최대 틱 |
| `_angleJumpDoubleClickTicks` | 3F | 더블탭 허용 간격 (틱) |
| `_wallUpJumpOrthogonalTolerance` | ? | 벽 점프 각도 스냅 허용 오차 |

---

## 1.21.1 마이그레이션 포인트

| 원본 | Fabric 1.21.1 대응 |
|------|-------------------|
| `sp.motionY = verticalMotion` | `entity.setVelocity(vel.x, verticalMotion, vel.z)` |
| `isPotionActive(Potion.jump)` | `entity.hasStatusEffect(StatusEffects.JUMP_BOOST)` |
| `getAmplifier()` | `entity.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier()` |
| `jump()` 인터셉트 | `@Mixin(PlayerEntity)` `@Inject(method = "jump", at = @At("HEAD"), cancellable = true)` |
| `horizontalCollisionAngle` 추적 | `@Mixin(Entity.move())` `@Inject` — 충돌 발생 시 각도 저장 |
| 더블탭 카운터 | `AttachmentType`에 저장 또는 `ClientTickEvents`에서 관리 |
| `tryJump()` 로직 | `SmartMovingState` 클래스 메서드로 직접 포팅 |

---

## 미확인 / 추가 조사 필요

- [ ] 1.21.1 `PlayerEntity.jump()` 정확한 메서드 시그니처 확인
- [ ] `horizontalCollisionAngle` — `Entity.move()` 에서 충돌 법선 각도를 어떻게 추출하는지
- [ ] 더블탭 감지를 `ClientTickEvents`에서 처리 시 서버와 동기화 필요한지
- [ ] 벽 점프 시 `wasCollidedHorizontally` 플래그 1.21.1에서 어떻게 접근하는지 (`Entity.horizontalCollision`)
- [ ] 차지 점프 UI (HUD 바) 렌더링 — `DrawContext` API 확인
- [ ] 각도 점프 더블탭 감지가 고속 프레임에서 문제 없는지 (틱 기반이라 안전하지만 확인 필요)
