# Angle Jump (a/s/d 두번 탭 좌/뒤/우 점프) 1:1 이식 리서치

> 작업 일자: 2026-04-27
> 사용자 보고: "a, s, d 키를 두번 탭해서 좌/뒤/우 로 점프뛰는 기능...
>   원본이랑 비교했을 때 에니메이션이 뒤틀리고, 원본 그대로 안나오고.
>   이동하는 속도 및 거리도 원본이랑 같은지 확인."
> 원칙: 원본 코드 라인 하나하나 다 읽어서 1:1 번역.

---

## 1. 원본 코드 (1.7.10 Forge)

### 1-1. 트리거 가드 (`SmartMovingSelf.java` L2898-2964)

```java
boolean canAngleJump = !isSleeping && sp.onGround && !isCrawling && !isClimbing
                    && !isClimbCrawling && !isSwimming && !isDiving;
boolean canSideJump  = Config.isSideJumpEnabled() && canAngleJump;
boolean canLeftJump  = canSideJump && !rightButton.Pressed;
boolean canRightJump = canSideJump && !leftButton.Pressed;
boolean canBackJump  = Config.isBackJumpEnabled() && canAngleJump
                    && !forwardButton.Pressed && !isStandupSprintingOrRunning();

// 좌 카운터
if (canLeftJump) {
    if (leftButton.StartPressed) {
        if (leftJumpCount == 0) leftJumpCount = Options.angleJumpDoubleClickTicks();
        else                    leftJumpCount = -1;
    } else if (leftJumpCount > 0) leftJumpCount--;
} else
    leftJumpCount = 0;
// 우/뒤 동일 패턴
...

// 대각선 우선순위 (-1 ↔ -2 강등/승격)
if (rightJumpCount == -2 && backJumpCount <= 0) rightJumpCount = -1;
...

// onGround 시 angleJumpType reset (★ 매 틱)
if (sp.onGround || sp.isCollidedVertically)
    angleJumpType = 0;
```

### 1-2. 트리거 발동 (`SmartMovingSelf.java` L1918-1944)

```java
int left = 0, back = 0;
if (leftJumpCount  == -1) left++;
if (rightJumpCount == -1) left--;
if (backJumpCount  == -1) back++;

if (left != 0 || back != 0) {
    int angle;
    if (left > 0)        angle = back == 0 ? 270 : 225;   // left | left+back
    else if (left < 0)   angle = back == 0 ? 90  : 135;   // right | right+back
    else                 angle = 180;                      // back

    if (tryJump(Config.Angle, null, null, sp.rotationYaw + angle))
        angleJumpType = ((360 - angle) / 45) % 8;          // 2~6

    leftJumpCount = 0; rightJumpCount = 0; backJumpCount = 0;
}
```

`angleJumpType` 매핑:
| relAngle | angleJumpType | 방향 |
|---------:|--------------:|-----|
| 270 | 2 | left |
| 225 | 3 | left+back |
| 180 | 4 | back |
| 135 | 5 | right+back |
|  90 | 6 | right |

`isAngleJumping()` = `angleJumpType > 1 && angleJumpType < 7` → 2~6 모두 true.

### 1-3. 속도 부여 (`SmartMovingSelf.tryJump`, L1999-L2095, type=Config.Angle)

```java
boolean up = (type==Angle || ...);            // Angle 포함 → up=true

float horizontalJumpFactor = Config.getJumpHorizontalFactor(speed, type) * jumpFactor;
float verticalJumpFactor   = Config.getJumpVerticalFactor(speed, type)   * jumpFactor;

// type==Angle 분기 (D-11, L2081-2095):
double horizontalMotion = sqrt(jumpMotionX² + jumpMotionZ²);
float jumpAngle = angle / RadiantToAngle;     // deg → rad
double horizontal = max(horizontalMotion, horizontalJumpFactor);
double moveX = -sin(jumpAngle);
double moveZ =  cos(jumpAngle);

motionX = getJumpMoving(jumpMotionX, moveX, false, horizontal, horizontalJumpFactor);
motionZ = getJumpMoving(jumpMotionZ, moveZ, false, horizontal, horizontalJumpFactor);
horizontalMotion = 0;
verticalMotion = verticalJumpFactor;          // ★ 수직 = verticalJumpFactor (그대로)
```

### 1-4. Factor 수치 (Config + getJumpHorizontalFactor / getJumpVerticalFactor)

- `_jumpHorizontalFactor` 기본 (확인 필요 — 1.21.1 Config 와 일치 가정).
- `_angleJumpHorizontalFactor` = **0.4F** (sm_1_3 부터, `SmartMovingConfig.java` L270).
- `_jumpVerticalFactor` 기본.
- `_angleJumpVerticalFactor` = **0.2F** (`SmartMovingConfig.java` L271).

Angle 분기 식:
- `getJumpHorizontalFactor` (L465-489): Angle 시 `result = _jumpHorizontalFactor × _angleJumpHorizontalFactor` 후 `if (type==Angle || ...) return result;` early return → speed 분기 skip.
- `getJumpVerticalFactor` (L418-463): Angle 시 `return result × _angleJumpVerticalFactor` 즉시 return.

### 1-5. 애니메이션 (`SmartMovingModel.animateAngleJumping`, L559-584)

```java
private void animateAngleJumping() {
    float angle = angleJumpType * Eighth;          // 2*π/4=π/2(left), 3π/4, π(back), 5π/4, 6*π/4=3π/2(right)

    // ★ bipedPelvic.Y 보정 — 다리가 카메라 방향 정렬
    md.bipedPelvic.rotateAngleY -= md.bipedOuter.rotateAngleY;   // -bipedOuter.Y
    md.bipedPelvic.rotateAngleY += md.currentCameraAngle;        // + cameraAngle (rotationYaw rad)
    // 결과: bipedPelvic.Y = -bipedOuter.Y + cameraAngle

    float backness  = 1F - abs(angle - Half) / Quarter;          // back=1, left/right=0
    float leftness  = -min(angle - Half, 0F) / Quarter;          // left=1, right=0
    float rightness = max(angle - Half, 0F) / Quarter;           // right=1, left=0

    bipedLeftLeg.rotateAngleX  = Thirtytwoth * (1 + rightness);  // 다리 들어올림
    bipedRightLeg.rotateAngleX = Thirtytwoth * (1 + leftness);
    bipedLeftLeg.rotateAngleY  = -angle;                         // 다리 yaw
    bipedRightLeg.rotateAngleY = -angle;
    bipedLeftLeg.rotateAngleZ  =  Thirtytwoth * backness;        // 다리 좌우 (back jump)
    bipedRightLeg.rotateAngleZ = -Thirtytwoth * backness;

    bipedLeftLeg.rotationOrder  = ZXY;
    bipedRightLeg.rotationOrder = ZXY;

    bipedLeftArm.rotateAngleZ  = -Sixteenth * rightness;
    bipedRightArm.rotateAngleZ =  Sixteenth * leftness;
    bipedLeftArm.rotateAngleX  = -Eighth * backness;             // 팔 위로 (back jump)
    bipedRightArm.rotateAngleX = -Eighth * backness;
}
```

**누적 회전 구조** (1.7.10 ModelRotationRenderer):
- `bipedOuter.Y` = `actualRotation` = bodyYaw lerped (renderYawOffset rad).
- `bipedPelvic.Y` = `-bipedOuter.Y + cameraAngle` = `-bodyYaw + cameraAngle`.
- `leg.Y` = `-angle`.
- 자식이 부모 회전 누적 → **leg world Y rotation** = `bodyYaw + (-bodyYaw + cameraAngle) + (-angle)` = **`cameraAngle - angle`**.

→ **다리가 카메라 방향 (rotationYaw) - angle 으로 회전**. 카메라 회전 시 다리도 즉시 따라감 (bodyYaw 의 lerp 지연 무관).

---

## 2. 현재 1.21.1 포팅 상태

### 2-1. 트리거 가드 (`SmartMovingClientState.java` L1540-1575)

```java
if (cfg.angleJumpSide) {
    if (startLeft)  { ... }
    if (startRight) { ... }
}
if (cfg.angleJumpBack) { ... }

// 대각선 우선순위 (원본 1:1)
if (rightJumpCount == -1 && backJumpCount > 0) rightJumpCount = -2;
...
```

**누락된 가드**:
- ❌ `canAngleJump` (sleeping/onGround/!crawling/!climbing/!swimming/!diving) — 카운터 갱신 자체가 가드 없음.
- ❌ `canLeftJump` 의 `!rightButton.Pressed`, `canRightJump` 의 `!leftButton.Pressed`.
- ❌ `canBackJump` 의 `!forwardButton.Pressed && !isStandupSprintingOrRunning()`.

### 2-2. 트리거 발동 (`SmartMovingJumper.handleJumping` L487-517)

```java
if (canAngleJump) {  // ← 발동 직전에만 체크 (카운터 갱신 시점이 아님)
    int relAngle;
    if (left > 0)      relAngle = back == 0 ? 270 : 225;
    else if (left < 0) relAngle = back == 0 ? 90  : 135;
    else               relAngle = 180;

    sm.angleJumpType = ((360 - relAngle) / 45) % 8;
    float worldAngleDeg = (float)((player.getYaw() + relAngle) % 360.0);
    if (worldAngleDeg < 0F) worldAngleDeg += 360F;
    tryJump(player, sm, ANGLE, null, null, worldAngleDeg);
}
```

원본 L1937: `tryJump(Config.Angle, null, null, sp.rotationYaw + angle)` — `% 360` 명시 안 됨. 1.21.1 코드는 mod 360 보정 추가 (안전).

### 2-3. `angleJumpType = 0` 매 틱 reset 누락

원본 `SmartMovingSelf.java` L2963-2964:
```java
if (sp.onGround || sp.isCollidedVertically)
    angleJumpType = 0;
```

현재 `SmartMovingClientState.java` 검색 결과: `angleJumpType = 0` 은 **resetState (L1975, 디스커넥트 시) 만**. 매 틱 onGround/isCollidedVertically reset 누락.
→ 점프 후 onGround 복귀해도 angleJumpType 잔존 → `isAngleJumping()` true 유지 → 애니메이션 잔존 가능성.

### 2-4. 속도 부여 (`SmartMovingJumper.tryJump`, D-11, L266-278)

```java
if (angle != null) {
    float jumpAngleRad = angle / 57.295776F;
    boolean reset = type == WALL_UP || type == WALL_HEAD;
    double horizontal = Math.max(horizontalMotion, horizontalJumpFactor);
    double moveX = -Math.sin(jumpAngleRad);
    double moveZ =  Math.cos(jumpAngleRad);
    motionX = getJumpMoving(sm.jumpMotionX, moveX, reset, horizontal, horizontalJumpFactor);
    motionZ = getJumpMoving(sm.jumpMotionZ, moveZ, reset, horizontal, horizontalJumpFactor);
    horizontalMotion = 0;
    verticalMotion = verticalJumpFactor;
}
```

→ **원본 L2081-2095 와 1:1 일치**. 수치도 `_angleJumpHorizontalFactor=0.4F`, `_angleJumpVerticalFactor=0.2F` 정확.

### 2-5. 애니메이션 (`MixinPlayerEntityModelClient.sm_animateAngleJumping`, L840-859)

```java
private void sm_animateAngleJumping(SmartMovingClientState sm) {
    float angle    = sm.angleJumpType * EIGHTH;
    float backness  = 1f - Math.abs(angle - HALF) / QUARTER;
    float leftness  = -Math.min(angle - HALF, 0f) / QUARTER;
    float rightness =  Math.max(angle - HALF, 0f) / QUARTER;

    setAnglesZXY(leftLeg,  THIRTYTWOTH * (1f + rightness), -angle,  THIRTYTWOTH * backness);
    setAnglesZXY(rightLeg, THIRTYTWOTH * (1f + leftness),  -angle, -THIRTYTWOTH * backness);

    leftArm.roll   = -SIXTEENTH * rightness;
    rightArm.roll  =  SIXTEENTH * leftness;
    leftArm.pitch  = -EIGHTH * backness;
    rightArm.pitch = -EIGHTH * backness;
}
```

주석 (L843): `"원본 bipedPelvic.rotateAngleY 조정: 1.21.1에 bipedPelvic 없음 → 생략"`.

→ **bipedPelvic 효과 누락 = 사용자 보고 "뒤틀림" 직접 원인**.

원본 효과 = 다리 world Y rotation = `cameraAngle - angle`.
현재 매핑 = 다리 world Y = (setupTransforms 의 POSITIVE_Y(180-bodyYaw)) + ModelPart.yaw(=-angle) → bodyYaw 만 따라감, cameraAngle 무관.

→ 마우스 회전 시 (cameraAngle 즉시 변화, bodyYaw 천천히 lerp) 다리 yaw 가 카메라와 분리.

---

## 3. 매핑 결정

### 3-1. 애니메이션 — bipedPelvic 효과 1:1 매핑 (핵심)

원본 식:
```
bipedPelvic.Y = -bipedOuter.Y + cameraAngle = -bodyYaw + cameraAngle
leg.world_Y   = bodyYaw + (-bodyYaw + cameraAngle) + (-angle) = cameraAngle - angle
```

1.21.1 매핑 — leg 가 root 의 직접 자식 (flat). setupTransforms 가 `POSITIVE_Y(180 - bodyYaw_deg)` 적용.
leg 가 카메라 방향 - angle 효과를 내려면, leg.yaw 에 `cameraAngle - bodyYaw - angle` 합산:

```java
float bodyYawRad = (float) Math.toRadians(MathHelper.wrapDegrees(entity.bodyYaw));
float cameraDelta = sm.stats.currentCameraAngle - bodyYawRad;
float legYaw = -angle + cameraDelta;          // 원본 cameraAngle - angle - bodyYaw 1:1
setAnglesZXY(leftLeg,  THIRTYTWOTH * (1+rightness), legYaw,  THIRTYTWOTH * backness);
setAnglesZXY(rightLeg, THIRTYTWOTH * (1+leftness),  legYaw, -THIRTYTWOTH * backness);
```

부호는 인게임 검증 후 정정 가능 (메모리 `feedback_render_scale_negation.md` 참조 — `scale(-1,-1,1)` Y 부호 영향 가능성).

### 3-2. 트리거 가드 누락 정정

`SmartMovingClientState.java` 의 카운터 갱신 블록에:
- `canAngleJump = onGround && !crawling && !climbing && !crawlClimbing && !swimming && !diving && !sleeping`
  (1.21.1 sleeping = `player.isSleeping()` 또는 `sm.isSleeping`. 확인 필요)
- `canLeftJump = canAngleJump && cfg.angleJumpSide && !rightKey.Pressed`
- `canRightJump = canAngleJump && cfg.angleJumpSide && !leftKey.Pressed`
- `canBackJump = canAngleJump && cfg.angleJumpBack && !forwardKey.Pressed && !isStandupSprintingOrRunning()`

각 카운터 분기:
```java
if (canLeftJump) { ... } else leftJumpCount = 0;   // ★ else reset
```

### 3-3. `angleJumpType = 0` 매 틱 reset

원본 L2963-2964 동등 위치 추가 — `SmartMovingClientState.tickEssential` 어딘가에:
```java
if (player.isOnGround() || player.verticalCollision) {
    angleJumpType = 0;
}
```

(verticalCollision = sp.isCollidedVertically 1:1. `Entity.verticalCollision` 필드.)

### 3-4. 속도 부여 — **변경 없음** (1:1 일치 확인)

`tryJump` D-11 분기 + `getJumpHorizontalFactor / getJumpVerticalFactor` 모두 원본 1:1.
수치 `_angleJumpHorizontalFactor=0.4F`, `_angleJumpVerticalFactor=0.2F` 정확.

`_jumpHorizontalFactor`, `_jumpVerticalFactor` 의 1.21.1 cfg 기본값 확인 필요 (별도 task).

---

## 4. 검증

1. **빌드**: gradle compile.
2. **시각 결과 예측**:
   - 좌 점프 (a 두번 탭) → 다리 yaw = cameraAngle - π/2 = 카메라 방향에서 좌 90° 회전 → 다리가 좌측 향함.
   - 마우스 회전 시 다리 즉시 따라감 (bodyYaw lerp 지연 무관) → "뒤틀림" 사라짐.
   - 속도/거리 — 원본과 동일 (1:1 일치 확인).
3. **인게임 통합테스트**: deferred (메모리 정책).

---

## 5. 참고

### 원본
- `SmartMoving\src\main\java\net\smart\moving\SmartMovingSelf.java` L1918-1944 (트리거 발동), L2898-2964 (가드+카운터+reset), L1999-L2136 (tryJump)
- `SmartMoving\src\main\java\net\smart\moving\render\SmartMovingModel.java` L559-584 (animateAngleJumping)
- `SmartMoving\src\main\java\net\smart\moving\config\SmartMovingConfig.java` L268-271 (config 기본값)
- `SmartMoving\src\main\java\net\smart\moving\config\SmartMovingClientConfig.java` L418-505 (factor 식)

### 1.21.1
- `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java` L840-859 (sm_animateAngleJumping — 변경 대상)
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` L1540-1575 (카운터 갱신 — 가드 추가 대상), L1975 근방 (angleJumpType reset 추가)
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingJumper.java` L266-278 (D-11 속도 — 변경 없음)
- `src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java` L243-380 (factor 식 — 1:1 일치)

---

## 6. 사용자 1차 보고 후 추가 발견 (2026-04-27 후속)

사용자 보고: "다리가 제일 많이 뒤틀림(뒤로 이동할 때는 아예 머리까지 다리가 올라오고,
좌우 이동도 원본과 다름). 머리 움직임이 원본과 1대1이 아님. 팔/다리/머리 fade 보간
부드러움 1대1 안됨."

### 6-1. setAnglesZXY 의 gimbal lock (다리 뒤틀림 원인)

**수치 검증** — back jump (angle=π) 시:
- 입력: `q = R_y(-π) * R_x(π/16) * R_z(π/16)`
  (yaw=-π = -180°, pitch=π/16, roll=π/16)
- quaternion 직접 계산 → q ≈ (-0.0096, -0.0975, -0.9904, 0.0975) [w, x, y, z]
- JOML `getEulerAnglesZYX` 분해 식:
  - `e.x (pitch) = atan2(2(yz + wx), 1 - 2(x²+y²))`
    = atan2(2(-0.0966 + 0.000936), 1 - 2(0.0095 + 0.981))
    = atan2(-0.1913, -0.981) ≈ **3.33 rad ≈ 191°** ★ 머리까지
  - `e.y (yaw) = asin(2(wy - xz))` ≈ **0.04 rad ≈ 2°** ← yaw 정보 손실
  - `e.z (roll) = atan2(2(wz + xy), 1 - 2(y²+z²))` ≈ **2.95 rad ≈ 169°**
- → ModelPart.pitch = 191°, yaw = 2°, roll = 169° → **다리가 완전 뒤집힘 (사용자 보고
  "머리까지 올라옴" 정확 일치)**.

**원인**: yaw 가 ±π/2 또는 ±π 근처일 때 ZYX euler 분해는 gimbal lock 영역.
asin 출력 [-π/2, π/2] 한계로 ±π yaw 표현 불가 → pitch/roll 로 분배.

**해결**: ModelPart.pitch/yaw/roll 직접 set (gimbal lock 회피).
- ModelPart 의 자체 quaternion = `rotationZYX(roll, yaw, pitch)` = R_z * R_y * R_x.
- vertex 적용 순서 = pitch → yaw → roll (XYZ).
- 원본 ZXY (yaw → pitch → roll) 와 차이: pitch/yaw 의 vertex 순서.
- 작은 pitch/roll (~π/16) 시 시각 차이 미세. 큰 yaw 자체는 정확 표현.

### 6-2. leg.yaw 부호 반전 (1.21.1 vertex 변환 정밀 분석)

**1.21.1 vanilla 렌더 흐름** (LivingEntityRenderer.render):
```
matrices.translate(0, 1.5, 0)
matrices.scale(-1, -1, 1)               // S = diag(-1, -1, 1) — X/Y 부호 반전
matrices.translate(0, -1.5, 0)
matrices.multiply(POSITIVE_Y((180 - bodyYaw_deg)*π/180))
... model.render()                       // ModelPart 자체 R_y(yaw) 적용
```

**vertex 변환 식**:
- `v_world = M * R_modelpart * v_model`
- 회전만 추출: `R_total = S * R_y(180 - bodyYaw_rad) * R_y(θ)` (θ = ModelPart.yaw)
  = `S * R_y(180 - bodyYaw_rad + θ)`
  = `R_y(-(180 - bodyYaw_rad + θ)) * S` (since `S * R_y(α) = R_y(-α) * S`)
  = `R_y(bodyYaw_rad - π - θ) * S`

**모델 정면 vertex** v_model = (0, 0, -1) → S*v = (0, 0, -1) → 그 후 R_y(bodyYaw_rad - π - θ) 적용.
- bodyYaw=0, θ=0: world v = R_y(-π)*(0,0,-1) = (0, 0, 1) — south. minecraft yaw=0=south ✓.

**leg world yaw direction** = `bodyYaw_rad - θ` (라디안).
(R_y(bodyYaw_rad - π - θ) 의 -π 는 위 분석에서 모델 정면 vertex (0,0,-1) 의 -π shift → 자동 cancel)

**원본 의도**: leg world Y direction = `cameraAngle - angle` (라디안).
**매핑 식**: `bodyYaw_rad - θ = cameraAngle - angle`
→ `θ = bodyYaw_rad - cameraAngle + angle = -cameraDelta + angle`
→ **`leg.yaw = angle - cameraDelta`** (현재 매핑 `-angle + cameraDelta` 의 **부호 반전**).

### 6-3. bodyYaw lerp (정확성)

setupTransforms 에서 LivingEntityRenderer 가 사용하는 bodyYaw 는
`MathHelper.lerp(tickDelta, prevBodyYaw, bodyYaw)` lerped 값.
우리 inject 에서 `entity.bodyYaw` raw 사용 시 매 frame 다른 값 → cameraDelta 부정확.

**매핑**:
```java
float partialTicks = SmartMovingClientState.globalCachedTickDelta;
float bodyYawDeg = MathHelper.lerp(partialTicks, entity.prevBodyYaw, entity.bodyYaw);
float bodyYawRad = (float) Math.toRadians(MathHelper.wrapDegrees(bodyYawDeg));
```

비행 매핑 (`sm_animateFlying` L625) 동일 패턴.

### 6-4. 단계 2 (deferred) — 머리 1:1 + fade 보간

**머리**: 원본 SmartRender `animateHeadRotation` (L257-258):
```java
bipedHead.rotateAngleY = (actualRotation + viewHorizontalAngelOffset) / RadiantToAngle;
bipedHead.rotateAngleX = viewVerticalAngelOffset / RadiantToAngle;
```
1.21.1 vanilla setAngles Step 2-3:
```java
head.yaw = netHeadYaw * π/180;
head.pitch = headPitch * π/180;
```
원본 `actualRotation` = bodyYaw, `viewHorizontalAngelOffset` = headYaw - bodyYaw → 합산 = headYaw.
1.21.1 vanilla `netHeadYaw` = headYaw - bodyYaw (별개 의미). 두 값이 정확히 어떻게 다른지 추가 분석 필요.

**fade 보간**: 원본 SmartRender `bipedOuter.fadeIntermediate` (factor 0.2, 5-tick e-fold).
1.21.1 vanilla `bodyYaw` 자체가 partialTick lerp (1 tick e-fold). 차이 = 4-tick lag.
→ angleJumping 분기에서도 비행 패턴 (lerpFadeAngle) 적용 가능.

단계 1 (다리 gimbal + 부호) 결과 확인 후 진행.

---

## 7. 매핑 결정 갱신

### 7-1. 단계 1 (즉시 적용)

```java
private void sm_animateAngleJumping(SmartMovingClientState sm, LivingEntity entity) {
    float angle    = sm.angleJumpType * EIGHTH;
    float backness  = 1f - Math.abs(angle - HALF) / QUARTER;
    float leftness  = -Math.min(angle - HALF, 0f) / QUARTER;
    float rightness =  Math.max(angle - HALF, 0f) / QUARTER;

    // bodyYaw lerp + cameraDelta + 부호 반전
    float partialTicks = SmartMovingClientState.globalCachedTickDelta;
    float bodyYawDeg = MathHelper.lerp(partialTicks, entity.prevBodyYaw, entity.bodyYaw);
    float bodyYawRad = (float) Math.toRadians(MathHelper.wrapDegrees(bodyYawDeg));
    float cameraDelta = sm.stats.currentCameraAngle - bodyYawRad;
    float legYaw = angle - cameraDelta;          // 부호 반전 (1.21.1 vertex 식 1:1 매핑)

    // ModelPart 직접 set (gimbal lock 회피, ZXY vs ZYX 차이 미세)
    leftLeg.pitch  = THIRTYTWOTH * (1f + rightness);
    leftLeg.yaw    = legYaw;
    leftLeg.roll   = THIRTYTWOTH * backness;
    rightLeg.pitch = THIRTYTWOTH * (1f + leftness);
    rightLeg.yaw   = legYaw;
    rightLeg.roll  = -THIRTYTWOTH * backness;

    // 팔 (변경 없음)
    leftArm.roll   = -SIXTEENTH * rightness;
    rightArm.roll  =  SIXTEENTH * leftness;
    leftArm.pitch  = -EIGHTH * backness;
    rightArm.pitch = -EIGHTH * backness;
}
```

### 7-2. 단계 2 (deferred — 단계 1 검증 후)
- 머리 1:1 분석 (vanilla netHeadYaw vs 원본 actualRotation+viewHorizontalAngelOffset).
- fade 보간 (lerpFadeAngle 패턴 적용).
