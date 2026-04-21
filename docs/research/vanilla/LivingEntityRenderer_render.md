# LivingEntityRenderer.render() vanilla 1.21.1 리서치

소스: 리맵된 jar `SmartMoving/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-48f5f74c97/1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/minecraft-clientOnly-48f5f74c97-*.jar`에서 `javap -c`로 분석  
Yarn 매핑: `~/.gradle/caches/fabric-loom/1.21.1/net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/mappings.tiny`  
Java 소스 파일: 미존재 — 바이트코드 역분석

---

## 1. 클래스 계층 및 메서드 시그니처

### LivingEntityRenderer

- 클래스 obf: `glk` / intermediary: `net/minecraft/class_922` / Yarn: `net/minecraft/client/render/entity/LivingEntityRenderer`
- 제네릭: `LivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>>`
- 실행 위치: **클라이언트 전용**

### render

- Yarn: `render` (intermediary: `method_4054`)
- 시그니처: `public void render(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light)`

### PlayerEntityRenderer.render (오버라이드)

- Yarn: `render` (intermediary: `method_4054` 오버라이드)
- 추가 호출: `setModelPose(entity)` → `super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light)`

---

## 2. PlayerEntityRenderer.render 전체 로직

바이트코드에서 재구성:

```java
// PlayerEntityRenderer.render (LivingEntityRenderer.render 오버라이드)
public void render(AbstractClientPlayerEntity entity, float yaw, float tickDelta,
                   MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
    this.setModelPose(entity);      // model.sneaking/armPose 등 세팅
    super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
                                    // LivingEntityRenderer.render 호출
}
```

---

## 3. PlayerEntityRenderer.setModelPose 전체 로직

- Yarn: `setModelPose` (intermediary: `method_4218`)
- 시그니처: `private void setModelPose(AbstractClientPlayerEntity entity)`
- 호출 시점: `PlayerEntityRenderer.render()` 첫 번째 호출, `renderArm()` 내부에서도 호출

```java
private void setModelPose(AbstractClientPlayerEntity entity) {
    PlayerEntityModel model = (PlayerEntityModel) this.getModel();

    // 관전자 모드: 머리/모자만 표시
    if (entity.isSpectator()) {
        model.setVisible(false);
        model.head.visible = true;
        model.hat.visible  = true;
        return;
    }

    // 일반 모드: 전체 표시 + 개별 파트 PlayerModelPart 설정
    model.setVisible(true);
    model.hat.visible        = entity.isPartVisible(PlayerModelPart.HAT);
    model.jacket.visible     = entity.isPartVisible(PlayerModelPart.JACKET);
    model.leftPants.visible  = entity.isPartVisible(PlayerModelPart.LEFT_PANTS_LEG);
    model.rightPants.visible = entity.isPartVisible(PlayerModelPart.RIGHT_PANTS_LEG);
    model.leftSleeve.visible = entity.isPartVisible(PlayerModelPart.LEFT_SLEEVE);
    model.rightSleeve.visible= entity.isPartVisible(PlayerModelPart.RIGHT_SLEEVE);

    // ★ sneaking 세팅
    model.sneaking = entity.isInSneakingPose();

    // 손 포즈 결정 (MAIN_HAND, OFF_HAND)
    BipedEntityModel.ArmPose mainPose = getArmPose(entity, Hand.MAIN_HAND);
    BipedEntityModel.ArmPose offPose  = getArmPose(entity, Hand.OFF_HAND);

    // 양손 무기인 경우: 오프핸드 포즈 덮어쓰기
    if (mainPose.isTwoHanded()) {
        offPose = entity.getOffHandStack().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
    }

    // 주 손 방향에 따라 left/right ArmPose 배정
    if (entity.getMainArm() == Arm.RIGHT) {
        model.rightArmPose = mainPose;
        model.leftArmPose  = offPose;
    } else {
        model.rightArmPose = offPose;
        model.leftArmPose  = mainPose;
    }
}
```

**핵심**: `model.sneaking = entity.isInSneakingPose()` — 이 한 줄이 BipedEntityModel.setAngles의 sneaking 분기를 제어한다.

---

## 4. LivingEntityRenderer.render 전체 로직

바이트코드에서 재구성 (파라미터: entity, yaw, tickDelta, matrices, vertexConsumers, light):

```java
public void render(T entity, float yaw, float tickDelta,
                   MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {

    matrices.push();

    // ── Step 1: 모델 기본 상태 세팅 ──────────────────────────
    model.handSwingProgress = this.getHandSwingProgress(entity, tickDelta);
    // → entity.getHandSwingProgress(tickDelta)

    model.riding = entity.hasVehicle();
    model.child  = entity.isBaby();

    // ── Step 2: bodyYaw / headYaw 보간 ──────────────────────
    float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw);
    float headYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevHeadYaw, entity.headYaw);
    float netHeadYaw = headYaw - bodyYaw;

    // ── Step 3: 탑승 시 bodyYaw를 vehicle에서 가져옴 ─────────
    if (entity.hasVehicle() && entity.getVehicle() instanceof LivingEntity vehicle) {
        bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, vehicle.prevBodyYaw, vehicle.bodyYaw);
        netHeadYaw = headYaw - bodyYaw;

        float wrappedDiff = MathHelper.wrapDegrees(netHeadYaw);
        // 클램프: ±85°
        wrappedDiff = MathHelper.clamp(wrappedDiff, -85.0f, 85.0f);
        bodyYaw = headYaw - wrappedDiff;

        // 차이가 크면(²>2500) bodyYaw += wrappedDiff * 0.2
        if (wrappedDiff * wrappedDiff > 2500.0f) {
            bodyYaw += wrappedDiff * 0.2f;
        }
        netHeadYaw = headYaw - bodyYaw;
    }

    // ── Step 4: 머리 pitch 보간 ──────────────────────────────
    float headPitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());

    // ── Step 5: 뒤집힌 경우 pitch/yaw 반전 ──────────────────
    if (shouldFlipUpsideDown(entity)) {
        headPitch   *= -1.0f;
        netHeadYaw  *= -1.0f;
    }
    netHeadYaw = MathHelper.wrapDegrees(netHeadYaw);

    // ── Step 6: SLEEPING 포즈 추가 translate ─────────────────
    if (entity.isInPose(EntityPose.SLEEPING)) {
        Direction sleepDir = entity.getSleepingDirection();
        if (sleepDir != null) {
            float eyeHeight = entity.getEyeHeight(EntityPose.STANDING) - 0.1f;
            matrices.translate(
                -sleepDir.getOffsetX() * eyeHeight,
                0.0f,
                -sleepDir.getOffsetZ() * eyeHeight
            );
        }
    }

    // ── Step 7: scale ────────────────────────────────────────
    float scale = entity.getScale();
    matrices.scale(scale, scale, scale);

    // ── Step 8: animationProgress, setupTransforms ───────────
    float animationProgress = this.getAnimationProgress(entity, tickDelta);
    // → entity.age + tickDelta

    this.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta, scale);
    // → PlayerEntityRenderer.setupTransforms() (leaningPitch 기반 X회전 등)

    // ── Step 9: 좌표계 반전 + 커스텀 scale ──────────────────
    matrices.scale(-1.0f, -1.0f, 1.0f);     // MC는 X, Y 반전
    this.scale(entity, matrices, tickDelta); // PlayerEntityRenderer: ×0.9375

    // ── Step 10: Y 오프셋 ────────────────────────────────────
    matrices.translate(0.0f, -1.501f, 0.0f);

    // ── Step 11: 걸음 애니메이션 파라미터 계산 ──────────────
    float limbSwingAmount = 0.0f;
    float limbSwing       = 0.0f;

    if (!entity.hasVehicle() && entity.isAlive()) {
        limbSwingAmount = entity.limbAnimator.getSpeed(tickDelta);
        limbSwing       = entity.limbAnimator.getPos(tickDelta);

        if (entity.isBaby()) {
            limbSwing *= 3.0f;
        }
        if (limbSwingAmount > 1.0f) {
            limbSwingAmount = 1.0f;
        }
    }

    // ── Step 12: animateModel → leaningPitch 세팅 ───────────
    model.animateModel(entity, limbSwing, limbSwingAmount, tickDelta);
    // BipedEntityModel.animateModel:
    //   model.leaningPitch = entity.getLeaningPitch(tickDelta)
    //   super.animateModel() (AnimalModel)

    // ── Step 13: setAngles ───────────────────────────────────
    model.setAngles(entity, limbSwing, limbSwingAmount, animationProgress, netHeadYaw, headPitch);
    // PlayerEntityModel.setAngles → BipedEntityModel.setAngles (모든 파트 각도 설정)

    // ── Step 14: 가시성 판정 ─────────────────────────────────
    MinecraftClient client = MinecraftClient.getInstance();
    boolean isVisible    = this.isVisible(entity);   // !entity.isInvisible()
    boolean isGlowing    = !isVisible && !entity.isInvisibleTo(client.player);
    boolean hasOutline   = client.hasOutline(entity);

    // ── Step 15: 렌더 레이어 결정 ────────────────────────────
    RenderLayer renderLayer = this.getRenderLayer(entity, isVisible, isGlowing, hasOutline);

    if (renderLayer != null) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
        int overlay = getOverlay(entity, this.getAnimationCounter(entity, tickDelta));
        // getAnimationCounter 기본값 = 0.0F
        // overlay = OverlayTexture.packUv(getU(whiteFlash), getV(hurtTime>0||deathTime>0))

        // 반투명 ghost: color = 0x9B9B9BFF, 일반: 0xFFFFFFFF (-1)
        int color = isGlowing ? 0x9B9B9BFF : -1;
        model.render(matrices, vertexConsumer, light, overlay, color);
    }

    // ── Step 16: 피처 렌더러 (장비, 망토 등) ─────────────────
    if (!entity.isSpectator()) {
        for (FeatureRenderer<T, M> feature : this.features) {
            feature.render(matrices, vertexConsumers, light, entity,
                           limbSwing, limbSwingAmount, tickDelta,
                           animationProgress, netHeadYaw, headPitch);
        }
    }

    matrices.pop();

    // ── Step 17: 라벨 렌더링 ────────────────────────────────
    super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    // EntityRenderer.render() → 이름표 등
}
```

---

## 5. BipedEntityModel.animateModel 전체 로직

- Yarn: `animateModel` (intermediary: `method_17086`)
- 시그니처: `public void animateModel(T entity, float limbSwing, float limbSwingAmount, float tickDelta)`

```java
public void animateModel(T entity, float limbSwing, float limbSwingAmount, float tickDelta) {
    // ★ model.leaningPitch 세팅 — setAngles보다 먼저 실행됨
    this.leaningPitch = entity.getLeaningPitch(tickDelta);
    // getLeaningPitch = MathHelper.lerp(tickDelta, lastLeaningPitch, leaningPitch)
    // (LivingEntity에서 매 틱 ±0.09씩 업데이트되는 값)

    super.animateModel(entity, limbSwing, limbSwingAmount, tickDelta);
    // AnimalModel.animateModel — 기타 처리
}
```

---

## 6. LivingEntityRenderer 보조 메서드

### getHandSwingProgress (method_4044)
```java
protected float getHandSwingProgress(T entity, float tickDelta) {
    return entity.getHandSwingProgress(tickDelta);
}
```

### getAnimationProgress (method_4045)
```java
protected float getAnimationProgress(T entity, float tickDelta) {
    return entity.age + tickDelta;
}
```
→ `setAngles`의 `ageInTicks` 파라미터로 전달됨

### getAnimationCounter (method_23185)
```java
protected float getAnimationCounter(T entity, float tickDelta) {
    return 0.0f;  // 기본값 0. 흰색 플래시(hurt flash) 진행도
}
```

### getOverlay (static, method_23622)
```java
public static int getOverlay(LivingEntity entity, float whiteFlashProgress) {
    return OverlayTexture.packUv(
        OverlayTexture.getU(whiteFlashProgress),
        OverlayTexture.getV(entity.hurtTime > 0 || entity.deathTime > 0)
    );
}
```

### scale (method_4042)
```java
// LivingEntityRenderer 기본 구현 — 아무것도 하지 않음
protected void scale(T entity, MatrixStack matrices, float tickDelta) { }

// PlayerEntityRenderer 오버라이드
@Override
protected void scale(AbstractClientPlayerEntity entity, MatrixStack matrices, float tickDelta) {
    matrices.scale(0.9375f, 0.9375f, 0.9375f);
}
```

### setupTransforms (LivingEntityRenderer, method_4058)
```java
// 기본 LivingEntityRenderer 구현:
protected void setupTransforms(T entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta, float scale) {
    // 1. isShaking → bodyYaw += cos(age * 3.25π) * 0.4 (진동)
    // 2. !SLEEPING → POSITIVE_Y.rotationDegrees(180 - bodyYaw)
    // 3. deathTime > 0 → Z rotate(sqrt(min((deathTime+tickDelta-1)/20*1.6, 1)) * getLyingAngle()), return
    // 4. isUsingRiptide → X(-90-pitch), Y((age+tickDelta)*-75), return
    // 5. SLEEPING → Y(sleepDir), Z(getLyingAngle()), Y(270°), return
    // 6. shouldFlipUpsideDown → translate(0, (height+0.1)/scale, 0), Z(180°)
}
// → PlayerEntityRenderer.setupTransforms가 오버라이드하여 수영/엘리트라 분기 처리
```

---

## 7. 렌더 전 model 필드 세팅 전체 요약

`setAngles()` 호출 전까지 각 필드가 어디서 세팅되는지:

| 필드 | 세팅 위치 | 값 |
|------|----------|-----|
| `model.handSwingProgress` | `LivingEntityRenderer.render()` step 1 | `entity.getHandSwingProgress(tickDelta)` |
| `model.riding` | `LivingEntityRenderer.render()` step 1 | `entity.hasVehicle()` |
| `model.child` | `LivingEntityRenderer.render()` step 1 | `entity.isBaby()` |
| `model.sneaking` | `PlayerEntityRenderer.setModelPose()` | `entity.isInSneakingPose()` |
| `model.rightArmPose` | `PlayerEntityRenderer.setModelPose()` | `getArmPose(entity, MAIN/OFF)` |
| `model.leftArmPose` | `PlayerEntityRenderer.setModelPose()` | `getArmPose(entity, MAIN/OFF)` |
| `model.leaningPitch` | `BipedEntityModel.animateModel()` | `entity.getLeaningPitch(tickDelta)` |

---

## 8. Yarn 이름 목록

| Yarn 이름 | intermediary | 클래스 | 시그니처 | 의미 |
|-----------|-------------|--------|---------|------|
| `render` | `method_4054` | LivingEntityRenderer | `(T, FF, MatrixStack, VertexConsumerProvider, I)V` | 메인 렌더 |
| `setModelPose` | `method_4218` | PlayerEntityRenderer | `(AbstractClientPlayerEntity)V` | 모델 상태 세팅 |
| `animateModel` | `method_17086` | BipedEntityModel | `(T, FFF)V` | leaningPitch 세팅 |
| `getHandSwingProgress` | `method_4044` | LivingEntityRenderer | `(T, F)F` | 손 흔들기 진행도 |
| `getAnimationProgress` | `method_4045` | LivingEntityRenderer | `(T, F)F` | age+tickDelta |
| `getAnimationCounter` | `method_23185` | LivingEntityRenderer | `(T, F)F` | 흰 플래시 진행도 |
| `getOverlay` | `method_23622` | LivingEntityRenderer | `(LivingEntity, F)I` | 오버레이 UV |
| `scale` | `method_4042` | LivingEntityRenderer | `(T, MatrixStack, F)V` | 추가 scale |
| `setupTransforms` | `method_4058` | LivingEntityRenderer | `(T, MatrixStack, FFFF)V` | 변환 세팅 |
| `getLeaningPitch` | `method_6024` | LivingEntity | `(F)F` | lerp된 leaningPitch |
| `render` (model) | `method_2819` | EntityModel | `(MatrixStack, VertexConsumer, III)V` | 모델 렌더 |

---

## 9. 렌더 파이프라인 전체 순서

```
EntityRenderDispatcher.render(entity, x, y, z, yaw, tickDelta, matrices, ...)
  │
  ├─ getPositionOffset() → Y -0.125 (웅크리기) or ZERO
  ├─ matrices.translate(x+offX, y+offY, z+offZ)
  │
  └─ PlayerEntityRenderer.render(entity, yaw, tickDelta, matrices, ...)
       │
       ├─ setModelPose(entity)                           [sneaking, armPose 세팅]
       │
       └─ LivingEntityRenderer.render(...)
            │
            ├─ model.handSwingProgress = entity.getHandSwingProgress(tickDelta)
            ├─ model.riding = entity.hasVehicle()
            ├─ model.child  = entity.isBaby()
            │
            ├─ bodyYaw / headYaw / netHeadYaw 보간
            ├─ headPitch 보간
            ├─ SLEEPING translate
            ├─ matrices.scale(entity.getScale() × 3)
            │
            ├─ setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta, scale)
            │    └─ PlayerEntityRenderer.setupTransforms:
            │         Branch 1: isFallFlying → X회전
            │         Branch 2: leaningPitch > 0 → X회전 + translate(0,-1,0.3)
            │         Branch 3: else → super.setupTransforms만
            │
            ├─ matrices.scale(-1, -1, 1)               [MC 좌표계 반전]
            ├─ PlayerEntityRenderer.scale(): ×0.9375
            ├─ matrices.translate(0, -1.501, 0)
            │
            ├─ limbSwing/limbSwingAmount 계산 (limbAnimator)
            │
            ├─ model.animateModel(entity, limbSwing, limbSwingAmount, tickDelta)
            │    └─ BipedEntityModel.animateModel:
            │         model.leaningPitch = entity.getLeaningPitch(tickDelta)
            │
            ├─ model.setAngles(entity, limbSwing, limbSwingAmount, age+tickDelta, netHeadYaw, headPitch)
            │    └─ PlayerEntityModel.setAngles → BipedEntityModel.setAngles
            │
            ├─ model.render(matrices, vertexConsumer, light, overlay, color)
            │
            ├─ features 렌더 (ArmorFeatureRenderer, 망토, 화살 등)
            │
            └─ EntityRenderer.super.render() → 이름표
```

---

## 10. SM 포팅 충돌 분석

### 충돌 1: model.sneaking은 isInSneakingPose() 기반

`model.sneaking = entity.isInSneakingPose()` — 이 값이 BipedEntityModel.setAngles의 모든 pivotY/pitch를 바꾼다.  
SM이 CROUCHING 포즈를 사용하는 동작에서는 이 sneaking 변환이 자동 적용된다.  
SM이 CROUCHING 포즈 없이 별도 자세를 만들려면 Mixin으로 `setModelPose` 후에 `model.sneaking = false`로 덮어써야 한다.

### 충돌 2: model.leaningPitch는 animateModel에서 세팅됨 (setAngles 직전)

`model.leaningPitch = entity.getLeaningPitch(tickDelta)` — entity.leaningPitch(LivingEntity 필드, 매 틱 ±0.09 변동)에서 옴.  
SM이 SWIMMING 포즈를 크롤링에 사용하면 leaningPitch가 1.0에 수렴 → setAngles에서 수영 팔 애니메이션이 활성화됨.  
SM이 leaningPitch 애니메이션을 원하지 않으면 SWIMMING 포즈를 쓰지 말거나 animateModel Mixin에서 `model.leaningPitch = 0`으로 재설정해야 한다.

### 충돌 3: matrices.translate(0, -1.501, 0)

렌더 전 고정 Y 오프셋 -1.501이 적용된다. 이는 sneaking Y 오프셋(-0.125, getPositionOffset)과 누적됨.  
SM 크롤링에서 별도 Y 오프셋이 필요하다면 이 값과의 합계를 고려해야 한다.

### 충돌 4: scale(-1, -1, 1) 후 추가 scale(0.9375)

플레이어 전체 렌더는 `0.9375` 배 축소된다. setupTransforms나 setAngles에서 compute하는 모든 오프셋 수치는 이 scale을 반영하지 않는다 — MatrixStack에 이미 들어있기 때문.  
SM의 카메라 오프셋이나 hitbox 변환 계산 시 이 scale 값을 고려해야 한다.

### 충돌 5: limbSwing 값의 의미

`limbSwing = limbAnimator.getPos(tickDelta)` — 발 애니메이션 위상(누적 이동 거리 기반 카운터).  
SM이 특수 이동(클라이밍, 크롤링) 시 이 값이 계속 업데이트되면 팔/다리 swing이 계속 돌아간다.  
특수 포즈에서 swing을 멈추려면 setAngles Mixin 내에서 팔/다리 pitch를 직접 덮어써야 한다.

### 충돌 6: features 렌더 (망토, 장비 등)

모든 FeatureRenderer는 setAngles 이후 동일한 limbSwing/limbSwingAmount/animationProgress 값을 받는다.  
SM이 모델 파트 transform을 setAngles 이후 Mixin으로 변경하면, 피처들도 그 변경된 파트 위치 기준으로 렌더된다.

---

## 11. 주요 수치

| 수치 | 위치 | 의미 |
|------|------|------|
| `-1.501f` | render Step 10 | Y translate 오프셋 |
| `-1.0f, -1.0f, 1.0f` | render Step 9 | MC 좌표계 반전 scale |
| `0.9375f` | PlayerEntityRenderer.scale() | 플레이어 모델 scale |
| `0.0f` | getAnimationCounter | 기본 흰 플래시 진행도 |
| `±85.0f` | bodyYaw 클램프 | 탑승 시 머리-몸 yaw 차이 최대값 |
| `2500.0f` | bodyYaw 추가 보정 임계값 | 차이² > 2500이면 bodyYaw += diff*0.2 |
| `3.0f` | limbSwing baby 배율 | 아기 엔티티 걸음 애니메이션 빠르게 |
