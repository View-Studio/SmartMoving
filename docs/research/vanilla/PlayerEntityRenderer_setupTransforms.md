# PlayerEntityRenderer.setupTransforms() vanilla 1.21.1 리서치

소스: 리맵된 jar `SmartMoving/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-48f5f74c97/1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/minecraft-clientOnly-48f5f74c97-1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2.jar`에서 `javap -c`로 분석  
Yarn 매핑: `~/.gradle/caches/fabric-loom/1.21.1/net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/mappings.tiny`  
Java 소스 파일: 미존재 (클래스 파일만 있음 — 바이트코드 역분석)

---

## 1. 메서드 시그니처 (Yarn 이름)

### PlayerEntityRenderer.setupTransforms

- Yarn: `setupTransforms` (intermediary: `method_4212`, obf: `a`)
- 클래스: `net.minecraft.client.render.entity.PlayerEntityRenderer`
- 실행 위치: **클라이언트 전용**
- 시그니처:
  ```
  protected void setupTransforms(
      AbstractClientPlayerEntity player,
      MatrixStack matrices,
      float animationProgress,   // local 3
      float bodyYaw,             // local 4
      float tickDelta,           // local 5
      float scale                // local 6
  )
  ```

파라미터명 근거: `LivingEntityRenderer.setupTransforms` (method_4058)의 mappings.tiny `p` 레코드에서 `p6 = scale` 확인. PlayerEntityRenderer는 파라미터명이 mappings.tiny에 없으나 super 호출 시 동일 순서로 전달함.

---

## 2. PlayerEntityRenderer.setupTransforms 전체 로직

바이트코드에서 재구성한 코드:

```java
protected void setupTransforms(AbstractClientPlayerEntity player, MatrixStack matrices,
                                float animationProgress, float bodyYaw, float tickDelta, float scale) {
    float leaningPitch = player.getLeaningPitch(tickDelta);   // local 7
    float pitch = player.getPitch(tickDelta);                  // local 8

    if (player.isFallFlying()) {
        // ── Branch 1: 엘리트라 ──────────────────────────────────────────
        super.setupTransforms(player, matrices, animationProgress, bodyYaw, tickDelta, scale);

        float f9 = (float)player.getFallFlyingTicks() + tickDelta;
        float f10 = MathHelper.clamp(f9 * f9 / 100.0f, 0.0f, 1.0f);

        if (!player.isUsingRiptide()) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f10 * (-90.0f - pitch)));
        }

        Vec3d rotationVec = player.getRotationVec(tickDelta);
        Vec3d velocity = player.lerpVelocity(tickDelta);
        double velHSq = velocity.horizontalLengthSquared();
        double rotHSq = rotationVec.horizontalLengthSquared();

        if (velHSq > 0.0 && rotHSq > 0.0) {
            double dot   = (velocity.x * rotationVec.x + velocity.z * rotationVec.z)
                           / Math.sqrt(velHSq * rotHSq);
            double cross = velocity.x * rotationVec.z - velocity.z * rotationVec.x;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(
                (float)(Math.signum(cross) * Math.acos(dot))
            ));
        }

    } else if (leaningPitch > 0.0f) {
        // ── Branch 2: 수영 / 크롤링 (leaningPitch > 0) ─────────────────
        super.setupTransforms(player, matrices, animationProgress, bodyYaw, tickDelta, scale);

        float targetAngle = player.isTouchingWater()
            ? (-90.0f - pitch)   // 수중: 몸통 pitch 반영
            : -90.0f;            // 비수중(크롤링): 수평 고정

        float f10 = MathHelper.lerp(leaningPitch, 0.0f, targetAngle);
        // = leaningPitch * targetAngle  (0→targetAngle 선형 보간)

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f10));

        if (player.isInSwimmingPose()) {
            matrices.translate(0.0f, -1.0f, 0.3f);
        }

    } else {
        // ── Branch 3: 일반 (서기 / 웅크리기) ────────────────────────────
        super.setupTransforms(player, matrices, animationProgress, bodyYaw, tickDelta, scale);
    }
}
```

---

## 3. 각 Branch 상세 분석

### Branch 1: 엘리트라 (`isFallFlying() = true`)

| 단계 | 변환 | 수치 |
|------|------|------|
| super.setupTransforms | Y축 방향 정렬 (bodyYaw 기반) | |
| `f9 = getFallFlyingTicks() + tickDelta` | 비행 경과 틱(보간) | |
| `f10 = clamp(f9² / 100, 0, 1)` | 비행 강도 0→1 (약 10틱에 1.0 도달) | |
| `isUsingRiptide()` 아닐 때: X 회전 | `f10 * (-90° - pitch)` | max: `-90° - pitch` |
| Y 회전 (속도-시선 각도) | `signum(cross) * acos(dot)` | 라디안 단위 |

엘리트라 비행 방향 보정: 속도 벡터와 시선 벡터의 수평 각도 차이(cross product 부호 × acos)를 Y축으로 회전.

### Branch 2: 수영 / 크롤링 (`leaningPitch > 0`)

**진입 조건**: `leaningPitch > 0` — 즉 `isInSwimmingPose()`가 true인 틱이 최소 하나 있었고 아직 0으로 돌아오지 않은 상태.

| 단계 | 변환 | 수치 |
|------|------|------|
| super.setupTransforms | Y축 방향 정렬 | |
| `targetAngle` 결정 | 수중: `-90° - pitch` / 비수중: `-90°` | |
| `f10 = lerp(leaningPitch, 0, targetAngle)` | X축 기울임 각도 | 0° → targetAngle |
| `POSITIVE_X.rotationDegrees(f10)` | 몸통 수평 기울임 | max: -90° |
| `isInSwimmingPose()` 시 추가: `translate(0, -1, 0.3)` | 렌더 위치 오프셋 | Y: -1, Z: +0.3 |

**`translate(0, -1, 0.3)` 의미** (로컬 좌표 기준, X회전 이후 적용):
- X회전 후 로컬 공간에서 Y = 아래방향, Z = 앞방향
- → 렌더링 위치를 1블록 아래, 0.3블록 앞으로 이동
- 수영 히트박스(0.6×0.6)에 맞게 모델을 시각적으로 맞추는 오프셋

**lerp 계산**:
```
MathHelper.lerp(t, a, b) = a + t * (b - a)
lerp(leaningPitch, 0, targetAngle) = 0 + leaningPitch * (targetAngle - 0) = leaningPitch * targetAngle
```

- leaningPitch = 0: 0° (직립)
- leaningPitch = 1: targetAngle (완전 수평)

### Branch 3: 일반 (`leaningPitch == 0`)

`super.setupTransforms()` 만 호출. 추가 변환 없음.

---

## 4. LivingEntityRenderer.setupTransforms() — super 구현

- Yarn: `setupTransforms` (intermediary: `method_4058`, obf: `a`)
- 클래스: `net.minecraft.client.render.entity.LivingEntityRenderer<T, M>`
- 파라미터: `(T entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta, float scale)`
- 파라미터명: mappings.tiny `p` 레코드에서 확인 (p1=entity, p2=matrices, p3=animationProgress, p4=bodyYaw, p5=tickDelta, p6=scale)

```java
protected void setupTransforms(T entity, MatrixStack matrices,
                                float animationProgress, float bodyYaw, float tickDelta, float scale) {
    // 1. 떨림 효과 (isShaking = Creeper 폭발 등)
    if (this.isShaking(entity)) {
        bodyYaw = bodyYaw + (float)(Math.cos((double)entity.age * 3.25d * Math.PI) * 0.4d);
    }

    // 2. 수면 중이 아니면 Y축 방향 정렬
    if (!entity.isInPose(EntityPose.SLEEPING)) {
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
    }

    // 3. 사망 중: Z축 쓰러짐 + 종료
    if (entity.deathTime > 0) {
        float f7 = ((float)entity.deathTime + tickDelta - 1.0f) / 20.0f * 1.6f;
        f7 = MathHelper.sqrt(f7);
        if (f7 > 1.0f) f7 = 1.0f;
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f7 * this.getLyingAngle(entity)));
        return;
    }

    // 4. 삼지창 회전: X + Y 회전 + 종료
    if (entity.isUsingRiptide()) {
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f - entity.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(entity.age + tickDelta) * -75.0f));
        return;
    }

    // 5. 수면 중: 침대 방향 + 눕기 + 종료
    if (entity.isInPose(EntityPose.SLEEPING)) {
        Direction dir = entity.getSleepingDirection();
        float yaw = (dir != null) ? getYaw(dir) : bodyYaw;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.getLyingAngle(entity)));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270.0f));
        return;
    }

    // 6. 뒤집기 (Dinnerbone 이름)
    if (shouldFlipUpsideDown(entity)) {
        matrices.translate(0.0f, (entity.getHeight() + 0.1f) / scale, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
    }
}
```

**참고: SLEEPING 처리 흐름**
- SLEEPING 포즈일 때: 단계 2에서 Y회전 **스킵** (bodyYaw 정렬 없음)
- 단계 5에서 침대 방향으로 별도 Y회전 적용

**수치 목록:**
| 수치 | 의미 |
|------|------|
| `3.25d * Math.PI` | 떨림 주파수 계수 |
| `0.4d` | 떨림 진폭 (bodyYaw ±0.4 라디안) |
| `/ 20.0f * 1.6f` | 사망 20틱 → 최대 1.6의 쓰러짐 계수 |
| `sqrt()` | 사망 쓰러짐 곡선 이징 |
| `-75.0f` | 삼지창 Y회전 속도 (-75°/틱) |
| `270.0f` | 수면 Y축 보정각 |
| `height + 0.1f` | Dinnerbone 뒤집기 Y오프셋 마진 |

---

## 5. 관련 Yarn 이름 목록

| Yarn 이름 | intermediary | obf | desc | 클래스 |
|-----------|-------------|-----|------|--------|
| `setupTransforms` (PlayerEntity) | `method_4212` | `a` | `(Lgdy;Lfbi;FFFF)V` | PlayerEntityRenderer |
| `setupTransforms` (LivingEntity) | `method_4058` | `a` | `(Lbtn;Lfbi;FFFF)V` | LivingEntityRenderer |
| `getLeaningPitch` | `method_6024` | `a` | `(F)F` | LivingEntity |
| `isFallFlying` | `method_6128` | `fA` | `()Z` | LivingEntity |
| `isUsingRiptide` | — | — | `()Z` | LivingEntity |
| `getFallFlyingTicks` | `method_6003` | `fB` | `()I` | LivingEntity |
| `getRotationVec` | `method_5828` | `g` | `(F)Lexc;` | Entity |
| `lerpVelocity` | `method_49339` | `G` | `(F)Lexc;` | Entity |
| `horizontalLengthSquared` | `method_37268` | `i` | `()D` | Vec3d |
| `isInSwimmingPose` | `method_20232` | `ce` | `()Z` | LivingEntity |
| `isTouchingWater` | `method_5799` | `bf` | `()Z` | Entity |
| `isShaking` (LivingEntity) | `method_25450` | `a` | `(Lbtn;)Z` | LivingEntityRenderer |
| `getLyingAngle` (LivingEntity) | `method_4039` | `c` | `(Lbtn;)F` | LivingEntityRenderer |
| `shouldFlipUpsideDown` | `method_38563` | `e` | `(Lbtn;)Z` | LivingEntityRenderer |

---

## 6. 호출 순서 (렌더 파이프라인에서의 위치)

```
LivingEntityRenderer.render()
  └─ setupTransforms()   ← PlayerEntityRenderer에서 오버라이드
       ├─ Branch 1 (isFallFlying): super() + X회전 + Y회전
       ├─ Branch 2 (leaningPitch > 0): super() + X회전 [-90°] + translate(0,-1,0.3)
       └─ Branch 3 (else): super() only
            └─ LivingEntityRenderer.setupTransforms():
                 ├─ isShaking() → bodyYaw 보정
                 ├─ !SLEEPING → Y회전 (180-bodyYaw)
                 ├─ deathTime > 0 → Z 쓰러짐, return
                 ├─ isUsingRiptide() → X+Y 회전, return
                 ├─ SLEEPING → 침대 방향 회전, return
                 └─ shouldFlipUpsideDown() → translate + Z 180°
```

---

## 7. SM 포팅 충돌 분석

### 충돌 1: Branch 2 (leaningPitch > 0) 자동 활성화

SM 크롤링이 SWIMMING 포즈를 사용하면 `isInSwimmingPose()` = true → `updateLeaningPitch()`에서 `leaningPitch` 증가.  
→ 다음 렌더 틱에 Branch 2 진입 → `super.setupTransforms()` + X축 -90° 회전 자동 적용.  
→ SM 원본의 자체 렌더 회전 코드와 이중 적용 위험.

**vanilla가 SM 크롤링의 X축 회전을 자동 처리한다는 의미**: SM은 추가 회전을 하지 않아야 한다.

### 충돌 2: translate(0, -1, 0.3) 위치 오프셋

Branch 2 내에서 `isInSwimmingPose()` = true이면 항상 실행.  
SM 크롤링 시 이 오프셋이 올바른지 확인 필요:  
- Y -1.0: 히트박스가 0.6×0.6이므로 모델을 0.6 히트박스 위치에 맞추는 용도로 보임  
- Z +0.3: 앞쪽으로 0.3 이동

### 충돌 3: 크롤링 시 pitch 포함 여부

수중일 때: `targetAngle = -90° - pitch` → 머리 피치가 기울기에 반영됨.  
비수중(크롤링)일 때: `targetAngle = -90°` → pitch 무시, 완전 수평 고정.  
SM 원본에서 크롤링 시 pitch 반영 여부 확인 필요.

### 충돌 4: Branch 1 (엘리트라)의 속도 방향 Y회전

SM의 대각선 점프나 속도 변경 기능이 `velocity.horizontalLengthSquared() > 0` 조건을 항상 만족하게 되면 Y 회전이 추가 적용됨.  
SM에서 엘리트라 비행을 별도 처리하지 않으면 이 분기가 간섭할 수 있음.

### 충돌 5: LivingEntityRenderer.super 변환과의 관계

모든 Branch가 `super.setupTransforms()` 를 먼저 호출한다.  
즉 Y축 방향 정렬 (`POSITIVE_Y.rotationDegrees(180 - bodyYaw)`)은 반드시 먼저 실행됨.  
SM이 bodyYaw를 변경하고 싶다면 `setupTransforms()` Mixin 진입 시점에 해야 한다.

---

## 8. 수치 전체 목록

| 수치 | 위치 | 의미 |
|------|------|------|
| `f9² / 100.0f` | Branch 1 | 엘리트라 비행 강도 계수 |
| `0.0f, 1.0f` | Branch 1 clamp | 강도 범위 |
| `-90.0f - pitch` | Branch 1 | 엘리트라 X회전 최대각 |
| `leaningPitch * targetAngle` | Branch 2 | X축 기울임 (lerp 결과) |
| `-90.0f` | Branch 2 (비수중) | 크롤링 목표 X각 |
| `-90.0f - pitch` | Branch 2 (수중) | 수영 목표 X각 |
| `translate(0, -1.0f, 0.3f)` | Branch 2 | isInSwimmingPose 시 위치 오프셋 |
| `3.25d * PI` | super | 떨림 주파수 |
| `0.4d` | super | 떨림 진폭 |
| `/ 20.0f * 1.6f` | super | 사망 쓰러짐 정규화 |
| `-75.0f` | super | 삼지창 Y회전 속도 |
| `270.0f` | super | 수면 Y보정 |
