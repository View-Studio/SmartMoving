# vanilla BipedEntityModel / ModelPart 상세 — R-09 리서치

소스: Fabric Loom 디컴파일 (Vineflower 1.11.1)  
Yarn: `net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2`  
확인 파일: `clientOnly-unpicked.jar` →
- `net/minecraft/client/render/entity/model/BipedEntityModel.class`
- `net/minecraft/client/model/ModelPart.class`
- `net/minecraft/client/render/entity/LivingEntityRenderer.class` (호출 측 확인)

실행 위치: 클라이언트 전용 (`@Environment(EnvType.CLIENT)`)

---

## B-07 — `setAngles()` 시그니처 (확인됨)

### Yarn 매핑

| Yarn 이름 | intermediary | 디스크립터 |
|-----------|-------------|-----------|
| `setAngles` | `method_17087` | `(Lbtn;FFFFF)V` |
| `animateArms` | `method_29353` | `(Lbtn;F)V` |

`Lbtn;` = `LivingEntity`

### 시그니처

```java
public void setAngles(T livingEntity, float f, float g, float h, float i, float j)
```

파라미터:
| 변수 | 실제 값 | 설명 |
|-----|---------|------|
| `livingEntity` | entity | 대상 엔티티 |
| `f` | `limbAnimator.getPos(tickDelta)` | 팔다리 애니메이션 위치(진행각) |
| `g` | `limbAnimator.getSpeed(tickDelta)` | 팔다리 애니메이션 속도(진폭) |
| `h` | `entity.age + tickDelta` | 애니메이션 진행값 (getAnimationProgress) |
| `i` | `headYaw - bodyYaw` (도 단위) | net head yaw |
| `j` | `lerp(tickDelta, prevPitch, pitch)` (도 단위) | head pitch |

LivingEntityRenderer.render() 내 호출:
```java
float o = livingEntity.limbAnimator.getSpeed(g);     // g = speed
float p = livingEntity.limbAnimator.getPos(g);       // p = pos
float n = this.getAnimationProgress(livingEntity, g); // n = age+tickDelta
// k = headYaw - bodyYaw (wrapped), m = pitch
this.model.animateModel(livingEntity, p, o, g);
this.model.setAngles(livingEntity, p, o, n, k, m);
```

### 1.7.10 대비 차이

1.7.10 `setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entity)`:
- **파라미터 7개 → 6개** (entity 포함)
- `scaleFactor` (항상 `0.0625F` = `1/16`) **제거됨**
- `onGround` 파라미터 **없음** — 1.7.10에서도 파라미터가 아닌 `ModelBase.onGround` 필드였음

**1.21.1에서 `onGround` float 파라미터 없음 (확인됨)**  
스니킹/포즈는 `BipedEntityModel.sneaking` (boolean 필드)으로 대체됨.

---

## B-06 — `animateArms()` 전체 코드 (확인됨)

```java
// BipedEntityModel.animateArms(T entity, float animationProgress)
protected void animateArms(T entity, float animationProgress) {
    if (!(this.handSwingProgress <= 0.0F)) {
        Arm arm = this.getPreferredArm(entity);
        ModelPart modelPart = this.getArm(arm);
        float f = this.handSwingProgress;
        this.body.yaw = MathHelper.sin(MathHelper.sqrt(f) * (float) (Math.PI * 2)) * 0.2F;
        if (arm == Arm.LEFT) {
            this.body.yaw *= -1.0F;
        }

        this.rightArm.pivotZ = MathHelper.sin(this.body.yaw) * 5.0F;
        this.rightArm.pivotX = -MathHelper.cos(this.body.yaw) * 5.0F;
        this.leftArm.pivotZ = -MathHelper.sin(this.body.yaw) * 5.0F;
        this.leftArm.pivotX = MathHelper.cos(this.body.yaw) * 5.0F;
        this.rightArm.yaw = this.rightArm.yaw + this.body.yaw;
        this.leftArm.yaw = this.leftArm.yaw + this.body.yaw;
        this.leftArm.pitch = this.leftArm.pitch + this.body.yaw;
        f = 1.0F - this.handSwingProgress;
        f *= f;
        f *= f;
        f = 1.0F - f;
        float g = MathHelper.sin(f * (float) Math.PI);
        float h = MathHelper.sin(this.handSwingProgress * (float) Math.PI) * -(this.head.pitch - 0.7F) * 0.75F;
        modelPart.pitch -= g * 1.2F + h;
        modelPart.yaw = modelPart.yaw + this.body.yaw * 2.0F;
        modelPart.roll = modelPart.roll + MathHelper.sin(this.handSwingProgress * (float) Math.PI) * -0.4F;
    }
}
```

- 호출: `setAngles()` 내 line 191 (`this.animateArms(livingEntity, h)`)
- 조건: `handSwingProgress > 0.0F`일 때만 실행 (스윙 중일 때)
- body.yaw 회전으로 팔이 앞뒤로 이동하는 효과 구현
- 스윙하는 팔: `pitch -= g*1.2F + h`, `yaw += body.yaw*2.0F`, `roll += ...*-0.4F`

**SM 관련성**: SM의 클라이밍/수영 중 팔 애니메이션이 handSwingProgress > 0이면 이 코드가 개입함. SM setRotationAngles에서 animateArms()를 super 호출로 받을 것인지 별도 처리할 것인지 결정 필요.

---

## B-08 — `ModelPart` xScale/yScale/zScale 필드 (확인됨)

```java
// ModelPart.java 필드 목록
public float pivotX;
public float pivotY;
public float pivotZ;
public float pitch;
public float yaw;
public float roll;
public float xScale = 1.0F;   // ← 존재 확인 (field_37938)
public float yScale = 1.0F;   // ← 존재 확인 (field_37939)
public float zScale = 1.0F;   // ← 존재 확인 (field_37940)
public boolean visible = true;
public boolean hidden;
```

| Yarn 이름 | intermediary | 기본값 |
|-----------|-------------|--------|
| `xScale` | `field_37938` | 1.0F |
| `yScale` | `field_37939` | 1.0F |
| `zScale` | `field_37940` | 1.0F |

**`xScale`, `yScale`, `zScale` 필드 모두 존재 (확인됨)**.

`setTransform(ModelTransform)` 호출 시 스케일이 1.0F로 리셋됨:
```java
public void setTransform(ModelTransform rotationData) {
    // ...
    this.xScale = 1.0F;
    this.yScale = 1.0F;
    this.zScale = 1.0F;
}
```

`copyTransform(ModelPart part)` 는 스케일도 복사함:
```java
public void copyTransform(ModelPart part) {
    this.xScale = part.xScale;
    this.yScale = part.yScale;
    this.zScale = part.zScale;
    // ...
}
```

---

## B-09 — `ModelPart.rotate(MatrixStack)` 회전 순서 (확인됨)

```java
// ModelPart.java
public void rotate(MatrixStack matrices) {
    matrices.translate(this.pivotX / 16.0F, this.pivotY / 16.0F, this.pivotZ / 16.0F);
    if (this.pitch != 0.0F || this.yaw != 0.0F || this.roll != 0.0F) {
        matrices.multiply(new Quaternionf().rotationZYX(this.roll, this.yaw, this.pitch));
    }

    if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
        matrices.scale(this.xScale, this.yScale, this.zScale);
    }
}
```

Yarn 이름: `rotate` (intermediary: `method_22703`)  
파라미터: `(Lfbi;)V` = `(MatrixStack)V`

### JOML `rotationZYX(z, y, x)` 의미

`new Quaternionf().rotationZYX(roll, yaw, pitch)`:
- JOML 메서드명: `rotationZYX(angleZ, angleY, angleX)`
- **적용 순서: X(pitch) → Y(yaw) → Z(roll)** (내부적으로 오른쪽에서 왼쪽으로 행렬 곱)
- 즉, 모델 파트 좌표계 기준 **pitch 먼저, yaw 다음, roll 마지막**

### 전체 적용 순서

```
1. translate(pivotX/16, pivotY/16, pivotZ/16)   ← pivot 이동
2. multiply(Quaternionf.rotationZYX(roll, yaw, pitch))  ← 회전 (pitch→yaw→roll 순)
3. scale(xScale, yScale, zScale)                 ← 스케일 (값이 1.0이 아닐 때만)
```

**SM 관련성**:
- SM 원본 1.7.10의 `GL11.glRotatef()` 호출 순서가 pitch→yaw→roll이었다면, vanilla의 `rotate()` 호출 하나로 동일하게 대응 가능.
- SM에서 비표준 순서(예: yaw→pitch→roll)가 필요한 부분은 `rotate(MatrixStack)` 대신 `matrices.multiply(RotationAxis.POSITIVE_X.rotation(pitch))` 등을 별도로 호출해야 함.
- `roll` 파라미터를 최초 인수로 넣는 것은 JOML API 규칙이며, 실제 roll이 가장 나중에 적용됨을 주의.

---

## setAngles() 전체 코드 요약

```java
public void setAngles(T livingEntity, float f, float g, float h, float i, float j) {
    // f = limbPos, g = limbSpeed, h = animationProgress, i = headYaw(deg), j = headPitch(deg)
    boolean bl = livingEntity.getFallFlyingTicks() > 4;  // 엘리트라 비행
    boolean bl2 = livingEntity.isInSwimmingPose();       // 수영 포즈
    this.leaningPitch = livingEntity.getLeaningPitch(h); // animateModel에서도 설정됨

    // 머리 yaw/pitch
    this.head.yaw = i * (float)(Math.PI / 180.0);
    // 머리 pitch (엘리트라 비행 시, 수영 포즈 시, 일반 시 분기)

    // 팔/다리 기본 스윙 애니메이션
    this.rightArm.pitch = MathHelper.cos(f * 0.6662F + PI) * 2.0F * g * 0.5F / k;
    this.leftArm.pitch  = MathHelper.cos(f * 0.6662F)      * 2.0F * g * 0.5F / k;
    this.rightLeg.pitch = MathHelper.cos(f * 0.6662F)      * 1.4F * g / k;
    this.leftLeg.pitch  = MathHelper.cos(f * 0.6662F + PI) * 1.4F * g / k;

    // 아이템 사용 포즈 (bow, crossbow, spyglass 등) positionRightArm/positionLeftArm 호출
    // animateArms() 호출 (스윙 중일 때)
    
    // 스니킹 시 pivot 오프셋 조정
    if (this.sneaking) {
        body.pitch = 0.5F;
        rightArm.pitch += 0.4F; leftArm.pitch += 0.4F;
        rightLeg.pivotZ = 4.0F; leftLeg.pivotZ = 4.0F;
        rightLeg.pivotY = 12.2F; leftLeg.pivotY = 12.2F;
        head.pivotY = 4.2F; body.pivotY = 3.2F;
        leftArm.pivotY = 5.2F; rightArm.pivotY = 5.2F;
    } else {
        body.pitch = 0.0F;
        rightLeg.pivotZ = 0.0F; leftLeg.pivotZ = 0.0F;
        rightLeg.pivotY = 12.0F; leftLeg.pivotY = 12.0F;
        head.pivotY = 0.0F; body.pivotY = 0.0F;
        leftArm.pivotY = 2.0F; rightArm.pivotY = 2.0F;
    }

    // 수영(leaningPitch > 0) 시 팔/다리 각도 별도 보간
    // hat.copyTransform(head)
}
```

---

## ModelPart 공개 API 전체 요약

| 메서드 | Yarn명 | 역할 |
|--------|--------|------|
| `rotate(MatrixStack)` | `method_22703` | pivot 이동 + 회전(pitch→yaw→roll) + scale |
| `render(MatrixStack, VertexConsumer, int, int)` | `method_22698` | rotate() 후 cuboid+자식 렌더링 |
| `setAngles(float, float, float)` | — | pitch/yaw/roll 일괄 설정 |
| `setPivot(float, float, float)` | `method_2851` | pivotX/Y/Z 설정 |
| `copyTransform(ModelPart)` | — | scale+pitch/yaw/roll+pivot 모두 복사 |
| `resetTransform()` | — | defaultTransform으로 복원 (xScale/yScale/zScale → 1.0F) |
| `getChild(String)` | — | 이름으로 자식 파트 반환 |
| `translate(Vector3f)` | — | pivotX/Y/Z에 더함 |
| `rotate(Vector3f)` | — | pitch/yaw/roll에 더함 |
| `scale(Vector3f)` | — | xScale/yScale/zScale에 더함 |
