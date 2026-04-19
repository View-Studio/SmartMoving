# 이동 로직 / 물리 / 충돌박스 리서치

> 원본 소스: `SmartMovingBase.java`, `SmartMovingSelf.java`, `Orientation.java`, `FeetClimbing.java`, `HandsClimbing.java`, `ClimbGap.java`

---

## 관련 클래스/파일

| 클래스 | 역할 |
|--------|------|
| `SmartMovingBase` | 충돌 감지, 클라이밍 표면 탐색, 액체 경계 계산 |
| `SmartMovingSelf` | 속도 계산, 히트박스 변경, 점프 처리 |
| `Orientation` | 9방향 시스템 (ZZ + 8방향) 클라이밍 방향 관리 |
| `FeetClimbing` | 발 클라이밍 상태 열거 (-3~3) |
| `HandsClimbing` | 손 클라이밍 상태 열거 (-3~2) |
| `ClimbGap` | 클라이밍 갭 탐지 결과 데이터 |

---

## 동작 원리

```
매 틱 moveEntityWithHeading(strafe, forward) 호출
  ↓
handleJumping() — 점프 타입 결정 및 속도 인가
  ↓
상태별 분기 (수영/용암/비행/지면 이동)
  ↓
moveFlying() — 입력 → 방향 벡터 → 모션 적용
  ↓
calculateSeparateCollisions() — X/Y/Z 축 분리 충돌
  ↓
히트박스 상태 유지 (setHeightOffset)
  ↓
모션 감쇠 적용
```

---

## A. moveEntityWithHeading() 구조

### 상태별 모션 감쇠

| 상태 | X/Z 감쇠 | Y 감쇠 | 비고 |
|------|---------|--------|------|
| 지면 이동 | `0.546F × 블록미끄러움` | 중력 적용 | 블록별 미끄러움 반영 |
| 공중 | `0.91F` | 점프 제어 | 공중 조작 감소 |
| 수영 (dipping) | `0.80D` | `0.83D` | 얕은 물 |
| 수영 (swimming) | `0.85D` | `0.85D` | 중간 깊이 |
| 잠수 (diving) | `0.83D` | `0.83D` | 완전 잠수, 3D 이동 |
| 거미줄 | `0.25D` | `0.05D` | 이동 극감 |

### 속도 산출 공식
```java
// 기본 속도
float rawSpeed = sp.onGround ? 0.1F * f3 : sp.jumpMovementFactor;
// 최종 속도
float finalSpeed = rawSpeed * speedFactor; // speedFactor = Config + potion + exhaustion
sp.moveFlying(moveStrafing, moveForward, finalSpeed);
```

### moveFlying() 방향 변환
```java
// 입력 정규화
float len = sqrt(strafing² + forward²);
if (len >= 0.01F) {
    len = speed / max(len, 1.0F);
    strafing *= len;
    forward  *= len;
    float sin = sin(yaw × π/180);
    float cos = cos(yaw × π/180);
    motionX += strafing * cos - forward * sin;
    motionZ += forward * cos + strafing * sin;
}
```

---

## B. 충돌 감지: calculateSeparateCollisions()

### 바닐라와의 핵심 차이

| 항목 | 바닐라 | SmartMoving |
|------|--------|-------------|
| 축 처리 | 동시 처리 | **Y → X → Z 순차 분리** |
| 스텝 클라이밍 | 자동 (stepHeight) | **수평 막힘 시에만 발동** |
| 크롤 충돌 | 없음 | **히트박스 40% 축소** |
| 스니킹 | 고정 처리 | **0.05 단위 점진적 감소** |

### 충돌 결과 비트마스크
```
CollidedPositiveX = 1   (오른쪽)
CollidedNegativeX = 2   (왼쪽)
CollidedPositiveY = 4   (위)
CollidedNegativeY = 8   (아래)
CollidedPositiveZ = 16  (앞)
CollidedNegativeZ = 32  (뒤)
```

### 스텝 클라이밍 조건
```java
// 수평 막힘 + stepHeight 있을 때만 발동
if (stepHeight > 0.0F && flag1 && (flag || ySize < 0.05F)) {
    // 위쪽으로 stepHeight 만큼 이동 시도
    // 결과가 평면 이동보다 더 나아가면 step 적용
}
```

---

## C. 히트박스 변경 시스템

### setHeightOffset()
```java
private void setHeightOffset(float offset) {
    resetHeightOffset();
    if (offset == 0F) return;
    heightOffset = offset;
    sp.boundingBox.minY -= heightOffset;  // offset=-1F → minY += 1.0
    sp.height += heightOffset;             // offset=-1F → height -= 1.0
}

private void resetHeightOffset() {
    sp.boundingBox.minY += heightOffset;
    sp.height -= heightOffset;
    heightOffset = 0F;
}
```

### 상태별 히트박스 크기

| 상태 | 너비 | 높이 | heightOffset | 비고 |
|------|------|------|-------------|------|
| 기본 서있기 | 0.6 | 1.8 | 0F | 바닐라 동일 |
| 기어가기 | 0.6 | **0.8** | **-1F** | minY +1.0 이동 |
| 슬라이딩 | 0.6 | **0.8** | **-1F** | 크롤과 동일 |
| 헤드 점프 | 0.6 | **0.8** | **-1F** | 점프 중 히트박스 축소 |
| 천장 클라이밍 | 0.6 | **0.8** | **-1F** | 뒤집힌 위치 |
| 수영/잠수 | 0.6 | 1.8 | 0F | 변경 없음 |
| 클라이밍 | 0.6 | 1.8 | 0F | 변경 없음 |

### 눈 높이 보정
```java
// 밝기 계산 시 카메라 위치 보정
sp.posY -= heightOffset;   // -1F → posY += 1F (카메라 올림)
float result = isp.localGetBrightness(f);
sp.posY += heightOffset;   // 복구
```

---

## D. 클라이밍 물리

### 수직 이동 속도 상수
```
FastUpMotion      = 0.20D   // 빠른 상승
MediumUpMotion    = 0.14D   // 중간 상승
SlowUpMotion      = 0.10D   // 느린 상승
HoldMotion        = 0.08D   // 정지 유지
SinkDownMotion    = 0.05D   // 비제어 하강
ClimbDownMotion   = 0.01D   // 제어 하강
ClimbPullMotion   = 0.30F   // 손 당김
CatchCrawlGap     = 0.17D   // 크롤→클라임 전환
```

### 발 클라이밍 상태 (FeetClimbing)
| 상수 | 값 | 설명 |
|------|-----|------|
| `None` | -3 | 클라이밍 없음 |
| `BaseHold` | -2 | 기본 접촉 |
| `BaseWithHands` | -1 | 손 보조 |
| `TopWithHands` | 0 | 상체 개입 |
| `SlowUpWithHoldWithoutHands` | 1 | 느린 상승 + 그립 |
| `SlowUpWithSinkWithoutHands` | 2 | 느린 상승 + 하강 |
| `FastUp` | 3 | 빠른 상승 |

### 손 클라이밍 상태 (HandsClimbing)
| 상수 | 값 | 설명 |
|------|-----|------|
| `None` | -3 | 없음 |
| `Sink` | -2 | 가라앉기 |
| `TopHold` | -1 | 상단 그립 |
| `BottomHold` | 0 | 하단 그립 |
| `Up` | 1 | 상승 |
| `FastUp` | 2 | 빠른 상승 |

### 표면 탐지 (Orientation 8방향)
```java
// 정면/후면/좌/우 + 대각선 4방향 탐색
Orientation.PZ.seekClimbGap(...)  // 앞
Orientation.NZ.seekClimbGap(...)  // 뒤
Orientation.ZP.seekClimbGap(...)  // 오른쪽
Orientation.ZN.seekClimbGap(...)  // 왼쪽
Orientation.PP.seekClimbGap(...)  // 앞-오른쪽 (크롤 아닐 때)
Orientation.PN.seekClimbGap(...)  // 앞-왼쪽
Orientation.NP.seekClimbGap(...)  // 뒤-오른쪽
Orientation.NN.seekClimbGap(...)  // 뒤-왼쪽
```

### 천장 클라이밍 중력 반전
```java
// 일반 중력: motionY -= 0.08D (아래로)
// 천장 클라이밍: motionY = +0.04 ~ +0.12D (위로)
sp.motionY = -value; // value는 음수 → 실제로 위로 이동
```

---

## E. 수중 물리

### 수면 높이 기준 상태 전환

| 수면 오프셋 | 상태 | 설명 |
|------------|------|------|
| 0 ~ 1.4 | `isDipping` | 부분 잠수 |
| 1.4 ~ 1.9 | `isSwimming` | 수영 |
| 1.9+ | `isDiving` | 완전 잠수 |

### getLiquidBorder() 계산
```java
// 흐르는 물 (metadata >= 8): 1.0F
// 고인 물 (metadata 0~7): (8 - meta) / 8F
//   meta=0 → 0.875F (꽉 찬 칸)
//   meta=7 → 0.125F (거의 빈 칸)
// 비액체: 0F
float border = (8 - blockMetaData) / 8F;
```

### 부력 모션 그라디언트 (Swimming 상태)
```
수면 오프셋 1.50: motionY -= 0.000625D
수면 오프셋 1.60: motionY -= 0.00125D
수면 오프셋 1.62: motionY -= 0.0025D
수면 오프셋 1.64: motionY -= 0.005D
수면 오프셋 1.66: motionY -= 0.01D
수면 오프셋 1.668: motionY -= 0.015D
수면 오프셋 1.672: motionY -= 0.02D
```
→ 수면에 가까울수록 부력 증가 (단계별 그라디언트)

### 물 탈출 점프
```java
// isDipping 상태에서 위로 이동 시
motionY = 0.30000001192092896D; // 고정 탈출 속도
// 소리: "random.splash", volume 0.05F, pitch 1.0F ± 0.4F
```

### reverseHandleMaterialAcceleration()
```java
// 바닐라 물 가속 효과를 역으로 적용 (흐름 저항)
entity.motionX += vec3d.xCoord * (-0.014D); // 바닐라는 +0.014D
entity.motionY += vec3d.yCoord * (-0.014D);
entity.motionZ += vec3d.zCoord * (-0.014D);
```

---

## F. 점프 메커니즘

### 기본 점프 공식
```java
// verticalMotion = -0.078 + 0.498 * verticalJumpFactor * jumpChargeFactor
// 바닐라 호환 (Config.Up): motionY = 0.41999998688697815D
// 점프 물약: += (amplifier + 1) * 0.1F
// 스프린트 보너스: motionX -= sin(yaw) * 0.2F; motionZ += cos(yaw) * 0.2F
```

### 차지 점프 (ChargeUp)
```
매 틱 jumpCharge++ (조건 충족 시)
발동 시: jumpChargeFactor = Config.getJumpChargeFactor(jumpCharge)
효과: 수직/수평 속도 모두 선형 증가
```

### 헤드 점프 (HeadUp) — 궤적 수직화
```java
// 동일한 총 운동량으로 궤적 각도만 가파르게 변환
double normalAngle = Math.atan(verticalMotion / horizontalMotion);
double totalMotion = Math.sqrt(verticalMotion² + horizontalMotion²);
double newAngle    = Config.getHeadJumpFactor(headJumpCharge) * normalAngle;
double newVertical   = totalMotion * Math.sin(newAngle);  // 수직 증가
double newHorizontal = totalMotion * Math.cos(newAngle);  // 수평 감소
```

### 8방향 각도 점프 (Angle Jump)
```
방향 → 각도 (yaw 기준 상대각)
Forward     : 0°
Right       : 90°
RightBack   : 135°
Back        : 180°
LeftBack    : 225°
Left        : 270°
LeftForward : 315°
RightForward: 45°

더블탭 감지: leftJumpCount / rightJumpCount / backJumpCount == -1 시 발동
속도 적용:
  motionX = getJumpMoving(jumpMotionX, -sin(jumpAngle), ...)
  motionZ = getJumpMoving(jumpMotionZ,  cos(jumpAngle), ...)
```

### 벽 점프 반사 계산
```java
if (!wasCollidedHorizontally) {
    // 반사 공식: 2 * 벽법선 - 진입각
    float movementAngle = getAngle(jumpMotionZ, -jumpMotionX);
    jumpAngle = horizontalCollisionAngle * 2 - movementAngle + 180F;
} else {
    jumpAngle = horizontalCollisionAngle; // 법선 방향 직접 반사
}
// 직각 정렬 옵션: tolerance 내에서 가장 가까운 90°로 스냅
```

---

## G. 전체 속도 값 정리

### 이동 속도 (지면)

| 이동 타입 | 기본 rawSpeed | 설정값 배수 | 비고 |
|-----------|-------------|----------|------|
| 일반 보행 | `0.1F × 블록미끄러움` | `0.546F` | 블록마다 다름 |
| 달리기/스프린트 | `0.1F` | `Config._runFactor` | |
| 공중 | `jumpMovementFactor` | `Config._jumpControlFactor` | |

### 클라이밍 수직 속도 (motionY)

| 상태 | 속도 (blocks/tick) |
|------|------------------|
| FastUp | 0.20D |
| MediumUp | 0.14D |
| SlowUp | 0.10D |
| Hold (정지) | 0.08D |
| SinkDown (비제어 하강) | -0.05D |
| ClimbDown (제어 하강) | -0.01D |
| ClimbPull | 0.30F |

### 수중 감쇠 계수

| 상태 | X/Z | Y |
|------|-----|---|
| Dipping | 0.80D | 0.83D |
| Swimming | 0.85D | 0.85D |
| Diving | 0.83D | 0.83D |
| 용암 | 0.50D | 0.50D |
| 거미줄 | 0.25D | 0.05D |

### 점프 속도 (motionY)

| 점프 타입 | 수직 속도 | 수평 변경 |
|-----------|---------|---------|
| 바닐라 Up | 0.42D | 스프린트 +0.2 |
| 일반 Up | `-0.078 + 0.498 × vFactor` | 없음 |
| ChargeUp | 차지에 비례 | 비례 증가 |
| HeadUp | 증가 (각도 변환) | 감소 |
| AngleJump | 일반 계산 | 방향 전환 |
| WallJump | 일반 계산 | 반사 방향 |
| 물 탈출 | 0.30D (고정) | 없음 |

---

## 바닐라 대비 핵심 차이점

1. **축 분리 충돌**: X/Y/Z 순차 처리 → 정밀한 마이크로 충돌, 크롤 중 계단 통과 가능
2. **조건부 스텝**: 수평 막힘 감지 시에만 스텝 클라이밍 활성화
3. **동적 히트박스**: `setHeightOffset(-1F)` 로 크롤/슬라이드/헤드점프 시 즉시 크기 변경
4. **8방향 자유 클라이밍**: 사다리 블록 없이도 8방향 표면 탐지
5. **천장 클라이밍**: motionY 부호 반전으로 중력 역전
6. **그라디언트 부력**: 단계별 수면 높이에 따른 세밀한 Y 속도 조정
7. **궤적 변환 점프**: 운동량 보존하면서 각도만 변경하는 HeadJump

---

## 1.21.1 마이그레이션 포인트

| 원본 | Fabric 1.21.1 대응 |
|------|-------------------|
| `sp.boundingBox.minY -= heightOffset` | `@Mixin(Entity)` → `getDimensions()` 오버라이드 또는 `EntityDimensions` 수정 |
| `sp.height += heightOffset` | `LivingEntity.getDimensions()` Mixin |
| `calculateSeparateCollisions()` | `@Mixin(Entity.move())` `@Inject` |
| `sp.motionY = FastUpMotion` | `LivingEntity.travel()` Mixin에서 `velocity.y` 직접 수정 |
| `reverseHandleMaterialAcceleration()` | `LivingEntity.travel()` 내 액체 처리 부분 `@Redirect` |
| 블록 미끄러움 `f3` | `BlockState.getSlipperiness()` (Fabric) |
| `moveFlying(strafing, forward, speed)` | `Entity.updateVelocity()` (1.21.1 동일 메서드 존재) |
| `getOnLadderOrVine()` | `@Mixin(LivingEntity.isClimbing())` |
| 물 높이 `getLiquidBorder()` | `FluidState.getHeight()` (Fabric) |

---

## 미확인 / 추가 조사 필요

- [ ] 1.21.1 `EntityDimensions`가 동적으로 변경 가능한지 확인 (크롤 히트박스)
- [ ] `Entity.move()` 내부에서 축 분리 충돌을 어디서 `@Inject` 할지 정확한 위치 확인
- [ ] `LivingEntity.travel()` 의 수중 처리 코드 위치 파악
- [ ] 1.21.1 블록 미끄러움 API (`Block.getSlipperiness` → `BlockState` 방식 변경 여부)
- [ ] 천장 클라이밍 시 gravity 반전: `LivingEntity.applyGravity()` Mixin 가능한지 확인
- [ ] `Orientation` 9방향 시스템을 Fabric에서 어떻게 구현할지 (enum vs 클래스)
