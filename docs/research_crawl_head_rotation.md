# Crawl 머리 회전 — 원본 1:1 매핑 리서치

## 1. 원본 코드 (1.7.10)

### 1.1 `SmartMovingModel.java` L393-L401 — isCrawl 분기 head 처리

```java
else if(isCrawl)
{
    float distance = totalHorizontalDistance * 1.3F;
    float walkFactor = Factor(currentHorizontalSpeedFlattened, 0F, 0.12951545F);
    float standFactor = Factor(currentHorizontalSpeedFlattened, 0.12951545F, 0F);

    bipedHead.rotateAngleZ = -viewHorizontalAngelOffset / RadiantToAngle;  // L399
    bipedHead.rotateAngleX = -Eighth;                                       // L400
    bipedHead.rotationPointZ = -2F;                                          // L401
    // ⚠ bipedHead.rotateAngleY 는 설정하지 않음 → reset() 값 0 유지
    ...
}
```

**핵심:**
- `head.rotateAngleZ = -viewHorizontalAngelOffset / RadiantToAngle` — 마우스 좌우 회전을 **Z 축 회전**으로 매핑
- `head.rotateAngleX = -Eighth` (= -π/8 = -22.5°) — 머리가 정면 위쪽으로 살짝 들림 (엎드린 채 앞을 보는 자세)
- `head.rotationPointZ = -2F` — 머리 pivot Z 보정
- **`head.rotateAngleY` 는 설정 안 함 → 기본값 0**
- **`viewVerticalAngelOffset` (= 마우스 상하/headPitch) 무시**

### 1.2 `SmartMovingModel.java` L626-L632 — animateHeadRotation override

```java
public void animateHeadRotation(...)
{
    setRotationAngles(...);  // SM 분기 처리 (위 isCrawl 등)
    if(isStandard)
        imp.superAnimateHeadRotation(...);  // SR default head 회전
}
```

### 1.3 `SmartRenderModel.java` L254-L259 — SR default `animateHeadRotation`

```java
public void animateHeadRotation(float viewHorizontalAngelOffset, float viewVerticalAngelOffset)
{
    bipedNeck.ignoreBase = true;
    bipedHead.rotateAngleY = (actualRotation + viewHorizontalAngelOffset) / RadiantToAngle;
    bipedHead.rotateAngleX = viewVerticalAngelOffset / RadiantToAngle;
}
```

**isCrawl 시**: `isCrawl != isStandard` 이므로 **superAnimateHeadRotation 호출 안 됨** → vanilla head 회전 미적용.

### 1.4 결론 — 원본 isCrawl 시 최종 head 상태

| 필드 | 값 | 의미 |
|------|-----|------|
| `head.rotateAngleX` | `-π/8` | 머리 위쪽으로 들림 |
| `head.rotateAngleY` | `0` | reset 값 (= 마우스 상하 무시) |
| `head.rotateAngleZ` | `-netHeadYaw rad` | 마우스 좌우 → Z 회전 |
| `head.rotationPointZ` | `-2F` (px) | pivot Z 보정 |

## 2. 우리 매핑 (현재)

### 2.1 `MixinPlayerEntityModelClient.java` `sm_animateCrawling` L876-L879

```java
// 머리
head.roll  = -headYaw * DEG_TO_RAD;
head.pitch = -EIGHTH;
head.pivotZ = -2f;
// ⚠ head.yaw 안 덮어쓰기 → vanilla setAngles 값 (= netHeadYaw * π/180) 그대로 살아있음
```

### 2.2 vanilla `BipedEntityModel.setAngles` (참고)

```java
head.yaw   = headYaw * 0.017453292F;   // = netHeadYaw rad
head.pitch = headPitch * 0.017453292F; // = headPitch rad
head.roll  = 0;
```

우리 sm_setAngles 가 `@At("TAIL")` 이라 vanilla 적용 후 inject. head.yaw 의 vanilla 값이 **cancel 안 됨**.

## 3. 차이 (BUG 의 원인)

| 필드 | 원본 | 우리 (BUG) | 차이 |
|------|------|-----------|------|
| head.yaw | 0 | netHeadYaw * π/180 | **마우스 좌우 회전 시 head.roll 매핑 + head.yaw 양쪽 적용** → 회전이 두 배 또는 어긋남 |
| head.pitch | -π/8 | -π/8 (덮어쓰기) | OK |
| head.roll | -netHeadYaw * π/180 | -netHeadYaw * π/180 (덮어쓰기) | OK |
| head.pivotZ | -2/16 블록 | -2 (px) ≈ -2/16 블록 | OK |

## 4. ModelPart 회전 순서 (참고)

- vanilla 1.21.1 `ModelPart.rotate`: `matrix.rotateZYX(pitch, yaw, roll)` = R_z * R_y * R_x → vertex 적용 = R_x first → R_y → R_z.
- 원본 SR head 의 `rotationOrder`: default XYZ → GL `glRotatef(Z), glRotatef(Y), glRotatef(X)` 호출 → vertex 적용 = R_x first → R_y → R_z. **동일**.

## 5. scale(-1,-1,1) 부호 반전 (참고)

- `S * R_x(θ) = R_x(-θ) * S` → ModelPart R_x = world R_x(-θ) 효과.
- `S * R_y(θ) = R_y(-θ) * S` → ModelPart R_y = world R_y(-θ) 효과.
- `S * R_z(θ) = R_z(θ) * S` → ModelPart R_z 부호 변화 X (= S commute).

원본/우리 둘 다 같은 scale(-1,-1,1) 적용 → 둘 다 동일 부호 효과. 매핑 1:1.

## 6. Fix

`sm_animateCrawling` 의 head 매핑에 **`head.yaw = 0;`** 한 줄 추가.

```java
head.roll  = -headYaw * DEG_TO_RAD;
head.pitch = -EIGHTH;
head.yaw   = 0f;       // 🔴 추가 — 원본 reset 후 isCrawl 분기 미설정 (= 0)
head.pivotZ = -2f;
```

## 7. cleanup 영향 (확인)

- `smWasCrawlingForCleanup && !sm.isCrawling` cleanup (L247-L262) 은 **종료 엣지** 한정.
- crawling 풀린 후 다음 frame 의 vanilla setAngles 가 `head.yaw = netHeadYaw * π/180` 정상 설정 → cleanup 불필요.
- 그래도 일관성 위해 종료 cleanup 에 `head.yaw = 0` 추가 가능하지만 vanilla 가 다음 frame 즉시 덮어쓰니까 무영향.

## 8. 확인할 추가 사항

- 원본 isCrawl 시 viewVerticalAngelOffset (= 마우스 상하) 완전 무시. 우리 매핑도 head.pitch = -EIGHTH 로 vanilla 의 headPitch 를 덮어씀 → **마우스 상하 무시** 일관.
- 마우스 좌우 회전 한계 (= 원본 reset 안 head.yaw clamp 없음) — 우리 매핑도 head.roll = -headYaw * DEG_TO_RAD 그대로 → 동일.

## 9. 추가 BUG (2026-05-03 사용자 보고 — "좌우 머리회전 최대값")

### 9.1 원본 — `SmartMovingRender.java` L137-L151 `rotatePlayer`

```java
public void rotatePlayer(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2)
{
    SmartMoving moving = SmartMovingFactory.getInstance(entityplayer);
    if(moving != null)
    {
        boolean isInventory = ...;
        if(!isInventory)
        {
            float forwardRotation = entityplayer.prevRotationYaw + (entityplayer.rotationYaw - entityplayer.prevRotationYaw) * f2;

            if(moving.isClimbing || moving.isClimbCrawling || moving.isCrawlClimbing || moving.isFlying ||
               moving.isSwimming || moving.isDiving || moving.isCeilingClimbing || moving.isHeadJumping ||
               moving.isSliding || moving.isAngleJumping())
                entityplayer.renderYawOffset = forwardRotation;
        }
    }
    irp.superRenderRotatePlayer(entityplayer, totalTime, actualRotation, f2);
}
```

**isCrawl 단독은 분기에 포함 안 됨** → vanilla `LivingEntity.tickHeadTurn` 의 75° 한계 자연 동작.

### 9.2 우리 매핑 (BUG) — `MixinPlayerEntityRenderer.java` L297-L302

```java
if (sm.isHeadJumping || sm.isCrawling || sm.isRopeSliding) {  // ⚠ isCrawling 잘못 포함
    smBodyYawActive = true;
    smBodyYawOverride = (float) Math.toDegrees(sm.stats.currentHorizontalAngle);
    return;
}
```

→ isCrawl 시 bodyYaw 를 entity.rotationYaw 로 매 frame 강제 → body 가 즉시 head 따라옴 → netHeadYaw ≈ 0 → head.roll = -netHeadYaw 매핑이 ≈ 0 → 마우스 좌우 회전이 누운 자세 머리에 안 보임.

### 9.3 Fix

- L298 의 isCrawling 제거.
- 또는 isCrawling 단독 분기를 명시적으로 vanilla 동작 (= smBodyYawActive=false) 으로 처리.

```java
if (sm.isHeadJumping || sm.isRopeSliding) {  // isCrawling 제거
    smBodyYawActive = true;
    smBodyYawOverride = (float) Math.toDegrees(sm.stats.currentHorizontalAngle);
    return;
}
```

근데 fallback (L308-L309 — `smBodyYawActive=true; smBodyYawOverride=lerpedYaw;`) 가 isCrawling 도 잡음 → 추가로 isCrawling 단독 가드 필요:

```java
// 원본 L147 강제 분기에 isCrawl 단독 미포함 → vanilla bodyYaw 자연 동작 (= 75° 한계).
if (sm.isCrawling && !sm.isClimbing) {
    smBodyYawActive = false;
    return;
}
```

L298 isHeadJumping 분기 위에 추가.

### 9.4 Fix 후 효과

- vanilla bodyYaw 자연 동작 = body 가 head 천천히 따라옴 (75° 한계).
- 마우스 좌우 0~75°: body 가 head 못 따라옴 → netHeadYaw 0~75° → head.roll = -0~-75° → 머리 좌우 회전 보임.
- 마우스 좌우 75° 이상: body 가 head 따라옴 → netHeadYaw clamp 75° → head.roll = -75° → 한계 + body 도 함께 회전.
- = 원본 정확 매핑.
