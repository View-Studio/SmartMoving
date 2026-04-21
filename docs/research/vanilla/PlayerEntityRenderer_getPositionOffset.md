# PlayerEntityRenderer.getPositionOffset() vanilla 1.21.1 리서치

소스: 리맵된 jar `SmartMoving/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-48f5f74c97/1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/minecraft-clientOnly-48f5f74c97-*.jar`에서 `javap -c`로 분석  
Yarn 매핑: `~/.gradle/caches/fabric-loom/1.21.1/net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/mappings.tiny`  
Java 소스 파일: 미존재 — 바이트코드 역분석

---

## 1. 메서드 시그니처 (Yarn 이름)

### PlayerEntityRenderer.getPositionOffset

- Yarn: `getPositionOffset` (intermediary: `method_23206`, obf: `a`)
- 클래스: `net.minecraft.client.render.entity.PlayerEntityRenderer`
- 실행 위치: **클라이언트 전용**
- 시그니처: `public Vec3d getPositionOffset(AbstractClientPlayerEntity player, float tickDelta)`
- 반환: `Vec3d` — 렌더링 위치의 추가 오프셋

---

## 2. PlayerEntityRenderer.getPositionOffset 전체 로직

바이트코드에서 재구성한 코드:

```java
public Vec3d getPositionOffset(AbstractClientPlayerEntity player, float tickDelta) {
    if (player.isInSneakingPose()) {
        return new Vec3d(0.0, (double)(player.getScale() * -2.0f / 16.0f), 0.0);
    } else {
        return super.getPositionOffset(player, tickDelta);
        // super = LivingEntityRenderer → EntityRenderer.getPositionOffset() = Vec3d.ZERO
    }
}
```

**결과 요약:**

| 상태 | 반환값 |
|------|--------|
| `isInSneakingPose() = true` | `Vec3d(0, -scale * 2/16, 0)` |
| `isInSneakingPose() = false` | `Vec3d.ZERO` |

**수치 계산:**
```
Y offset = getScale() * (-2.0f) / 16.0f
         = getScale() * -0.125f

scale = 1.0 (일반 플레이어): Y = -0.125
```

---

## 3. 상위 클래스 구현

### EntityRenderer.getPositionOffset (베이스)

- Yarn: `getPositionOffset` (intermediary: `method_23169`, obf: `a`)
- 클래스: `net.minecraft.client.render.entity.EntityRenderer`

```java
public Vec3d getPositionOffset(T entity, float tickDelta) {
    return Vec3d.ZERO;
}
```

### LivingEntityRenderer.getPositionOffset

- LivingEntityRenderer에는 별도 오버라이드 없음
- PlayerEntityRenderer의 `super.getPositionOffset()` → EntityRenderer의 `Vec3d.ZERO` 반환

---

## 4. 호출 위치 — EntityRenderDispatcher.render()

- Yarn: `render` (intermediary: `method_3954`, obf: `a`)
- 클래스: `net.minecraft.client.render.entity.EntityRenderDispatcher`
- 시그니처: `public <E extends Entity> void render(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light)`

바이트코드에서 재구성한 호출 순서:

```java
public <E extends Entity> void render(E entity, double x, double y, double z,
                                       float yaw, float tickDelta,
                                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
    EntityRenderer<E> renderer = this.getRenderer(entity);

    // getPositionOffset() 호출 → 오프셋 취득
    Vec3d positionOffset = renderer.getPositionOffset(entity, tickDelta);

    // 오프셋을 렌더 좌표에 더함
    double renderX = x + positionOffset.getX();
    double renderY = y + positionOffset.getY();
    double renderZ = z + positionOffset.getZ();

    // MatrixStack에 절대 위치 적용
    matrices.push();
    matrices.translate(renderX, renderY, renderZ);

    // 실제 렌더링 (setupTransforms 등 이 안에서 호출됨)
    renderer.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);

    if (entity.doesRenderOnFire()) {
        // 불꽃 렌더링 ...
    }
    // ...
}
```

**핵심**: `getPositionOffset()`의 반환값은 `matrices.translate()`로 렌더 전체에 적용된다.  
즉 웅크리기 시 `Y -0.125` 오프셋 → 플레이어 렌더 전체가 0.125블록 아래로 이동.

---

## 5. isInSneakingPose() 의미

- Yarn: `isInSneakingPose` (intermediary: `method_18276`, obf: `cb`, desc: `()Z`)
- 클래스: `LivingEntity`
- `Entity.isInPose(EntityPose.CROUCHING)` 와 동일

플레이어가 CROUCHING 포즈일 때 true:
- 웅크리기(shift 키 누름 + 공간 있을 때)
- SM 크롤링이 CROUCHING을 사용하면 이 조건 해당

---

## 6. 관련 Yarn 이름 목록

| Yarn 이름 | intermediary | obf | desc | 클래스 |
|-----------|-------------|-----|------|--------|
| `getPositionOffset` (Player) | `method_23206` | `a` | `(Lgdy;F)Lexc;` | PlayerEntityRenderer |
| `getPositionOffset` (Entity) | `method_23169` | `a` | `(Lbsr;F)Lexc;` | EntityRenderer |
| `render` (Dispatcher) | `method_3954` | `a` | `(Lbsr;DDDFFLfbi;Lgez;I)V` | EntityRenderDispatcher |
| `isInSneakingPose` | `method_18276` | `cb` | `()Z` | LivingEntity |
| `getScale` | `method_55693` | `eb` | `()F` | LivingEntity |
| `Vec3d.ZERO` (필드) | `field_1353` | `b` | `Lexc;` | Vec3d |

---

## 7. 렌더 파이프라인에서의 위치

```
EntityRenderDispatcher.render(entity, x, y, z, yaw, tickDelta, matrices, ...)
  │
  ├─ getPositionOffset(entity, tickDelta)
  │    └─ PlayerEntityRenderer.getPositionOffset()
  │         ├─ isInSneakingPose() = true  → Vec3d(0, -scale*0.125, 0)
  │         └─ isInSneakingPose() = false → Vec3d.ZERO
  │
  ├─ matrices.translate(x+offsetX, y+offsetY, z+offsetZ)
  │
  └─ renderer.render(entity, yaw, tickDelta, matrices, ...)
       └─ PlayerEntityRenderer.render()
            ├─ setModelPose()
            └─ super.render()  (LivingEntityRenderer.render())
                 └─ setupTransforms()  ← Branch 2: leaningPitch > 0 → X회전 etc.
```

---

## 8. SM 포팅 충돌 분석

### 충돌 1: 웅크리기 Y 오프셋 (-0.125)

vanilla는 CROUCHING 포즈 시 `getPositionOffset()` 에서 Y -0.125를 반환, 렌더 전체가 아래로 이동.  
SM이 CROUCHING 포즈를 사용하는 동작(웅크리기 이동, SM 느린 이동 등)은 이 오프셋을 자동으로 받는다.

**SM 크롤링이 SWIMMING 포즈를 사용하면 이 오프셋은 적용되지 않는다** (`isInSneakingPose()` = false).  
크롤링 시 Y 오프셋이 필요하다면 `getPositionOffset()` Mixin으로 별도 처리해야 한다.

### 충돌 2: scale 의존성

오프셋 공식: `getScale() * -2 / 16 = -scale * 0.125`  
플레이어 scale이 1.0이 아닌 경우(attribute 변경 등) 오프셋이 달라진다.  
SM에서 플레이어 치수를 변경하더라도 scale attribute에는 영향 없으므로 offset = -0.125 고정.

### 충돌 3: getPositionOffset이 translate에 적용되는 시점

`EntityRenderDispatcher.render()`에서 먼저 translate 후 renderer.render() 호출.  
즉 setupTransforms() 내의 추가 translate(0, -1, 0.3)는 이 오프셋 **이후에** 적용된다.  
두 오프셋은 누적됨:
- 웅크리기: Y -0.125 (getPositionOffset) + setupTransforms에서의 추가 변환
- 크롤링(SWIMMING 포즈): getPositionOffset = 0 + setupTransforms Y -1.0 (isInSwimmingPose 시)

---

## 9. 수치 전체

| 수치 | 의미 |
|------|------|
| `-2.0f / 16.0f = -0.125f` | 웅크리기 시 Y 오프셋 (scale=1 기준) |
| `getScale()` | LivingEntity scale attribute 값 (플레이어 기본 1.0) |
| `Vec3d.ZERO` | 비웅크리기 시 오프셋 없음 |
