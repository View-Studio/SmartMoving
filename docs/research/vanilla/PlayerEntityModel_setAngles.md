# PlayerEntityModel.setAngles() vanilla 1.21.1 리서치

소스: 리맵된 jar `SmartMoving/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-48f5f74c97/1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/minecraft-clientOnly-48f5f74c97-*.jar`에서 `javap -c`로 분석  
Yarn 매핑: `~/.gradle/caches/fabric-loom/1.21.1/net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/mappings.tiny`  
Java 소스 파일: 미존재 — 바이트코드 역분석

---

## 1. 클래스 계층

```
EntityModel<T>          (field: riding)
  └── AnimalModel<T>    (field: handSwingProgress)
        └── BipedEntityModel<T>  (field: sneaking, leaningPitch, head/body/arm/leg)
              └── PlayerEntityModel<T>  (field: cloak, ear, sleeve, pants, jacket, thinArms)
```

- `PlayerEntityModel` 클래스: obf `fwp` / intermediary `net/minecraft/class_591` / Yarn `net/minecraft/client/render/entity/model/PlayerEntityModel`
- `BipedEntityModel` 클래스: obf `fvx` / intermediary `net/minecraft/class_572` / Yarn `net/minecraft/client/render/entity/model/BipedEntityModel`

---

## 2. 메서드 시그니처 (Yarn 이름)

### PlayerEntityModel.setAngles

- Yarn: `setAngles` (intermediary: `method_17087`)
- 클래스: `PlayerEntityModel`
- 실행 위치: **클라이언트 전용**
- 시그니처: `public void setAngles(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)`
- 파라미터:
  - `limbSwing`: 누적 이동 카운터 (발 애니메이션 위상)
  - `limbSwingAmount`: 이동 속도 계수 (0~1 범위)
  - `ageInTicks`: 엔티티 나이 (틱)
  - `netHeadYaw`: 몸체 대비 머리 yaw (도)
  - `headPitch`: 머리 pitch (도)

---

## 3. PlayerEntityModel.setAngles 전체 로직

바이트코드에서 재구성한 코드:

```java
public void setAngles(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    // 1. 부모 클래스에 위임
    super.setAngles(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    // BipedEntityModel.setAngles 호출 (전체 로직은 섹션 4 참조)

    // 2. 오버레이 파트 트랜스폼 복사 (옷 레이어)
    leftPants.copyTransform(leftLeg);
    rightPants.copyTransform(rightLeg);
    leftSleeve.copyTransform(leftArm);
    rightSleeve.copyTransform(rightArm);
    jacket.copyTransform(body);

    // 3. 클로크(망토) pivot 위치 설정
    // 조건: 가슴 장비 착용 여부 × 웅크리기 여부 4분기
    boolean hasChestArmor = !entity.getEquippedStack(EquipmentSlot.CHEST).isEmpty();
    if (hasChestArmor) {
        if (entity.isInSneakingPose()) {
            cloak.pivotZ = 1.4f;
            cloak.pivotY = 1.85f;
        } else {
            cloak.pivotZ = 0.0f;
            cloak.pivotY = 0.0f;
        }
    } else {
        if (entity.isInSneakingPose()) {
            cloak.pivotZ = 0.3f;
            cloak.pivotY = 0.8f;
        } else {
            cloak.pivotZ = -1.1f;
            cloak.pivotY = -0.85f;
        }
    }
}
```

**클로크 pivot 수치 요약:**

| 가슴 장비 | 웅크리기 | pivotZ | pivotY |
|----------|---------|--------|--------|
| 있음 | true | 1.4f | 1.85f |
| 있음 | false | 0.0f | 0.0f |
| 없음 | true | 0.3f | 0.8f |
| 없음 | false | -1.1f | -0.85f |

---

## 4. BipedEntityModel.setAngles 전체 로직

PlayerEntityModel의 `super.setAngles()` 대상. Yarn: `setAngles` (intermediary: `method_17087`, 클래스: `BipedEntityModel`)

```java
public void setAngles(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    // ── Step 1: 상태 플래그 계산 ──────────────────────────────
    boolean isFallFlying = entity.getFallFlyingTicks() > 4;
    boolean isSwimming   = entity.isInSwimmingPose();

    // ── Step 2: 머리 yaw 설정 ─────────────────────────────────
    head.yaw = netHeadYaw * 0.017453292f;  // π/180, 도→라디안

    // ── Step 3: 머리 pitch 설정 ──────────────────────────────
    if (isFallFlying) {
        head.pitch = -0.7853982f;  // -π/4 = -45°, 고정
    } else if (leaningPitch > 0) {
        if (isSwimming) {
            // 수영 포즈: pitch → -45° 방향으로 lerp
            head.pitch = lerpAngle(leaningPitch, head.pitch, -0.7853982f);
        } else {
            // 비수영 (크롤링 아닌 수평): 실제 headPitch로 lerp
            head.pitch = lerpAngle(leaningPitch, head.pitch, headPitch * 0.017453292f);
        }
    } else {
        head.pitch = headPitch * 0.017453292f;
    }

    // ── Step 4: body/arm pivot 초기화 ────────────────────────
    body.yaw = 0.0f;
    rightArm.pivotZ = 0.0f;
    rightArm.pivotX = -5.0f;
    leftArm.pivotZ  = 0.0f;
    leftArm.pivotX  =  5.0f;

    // ── Step 5: 속도 계수 (fallFlying 시 사용) ────────────────
    float speedFactor = 1.0f;
    if (isFallFlying) {
        float velocityLenSq = (float) entity.getVelocity().lengthSquared();
        speedFactor = velocityLenSq / 0.2f;
        speedFactor = speedFactor * speedFactor * speedFactor;   // ^3
        if (speedFactor < 1.0f) speedFactor = 1.0f;             // min 1
    }

    // ── Step 6: 팔 swing 애니메이션 ──────────────────────────
    rightArm.pitch = MathHelper.cos(limbSwing * 0.6662f + (float)Math.PI) * 2.0f * limbSwingAmount * 0.5f / speedFactor;
    leftArm.pitch  = MathHelper.cos(limbSwing * 0.6662f)                  * 2.0f * limbSwingAmount * 0.5f / speedFactor;
    rightArm.roll  = 0.0f;
    leftArm.roll   = 0.0f;

    // ── Step 7: 다리 swing 애니메이션 ────────────────────────
    rightLeg.pitch = MathHelper.cos(limbSwing * 0.6662f)                  * 1.4f * limbSwingAmount / speedFactor;
    leftLeg.pitch  = MathHelper.cos(limbSwing * 0.6662f + (float)Math.PI) * 1.4f * limbSwingAmount / speedFactor;
    rightLeg.yaw   =  0.005f;
    leftLeg.yaw    = -0.005f;
    rightLeg.roll  =  0.005f;
    leftLeg.roll   = -0.005f;

    // ── Step 8: 탑승(riding) 오버라이드 ──────────────────────
    if (this.riding) {
        rightArm.pitch += -0.62831855f;   // -π/5 ≈ -36°
        leftArm.pitch  += -0.62831855f;
        rightLeg.pitch  = -1.4137167f;   // -0.45π ≈ -81°
        rightLeg.yaw    =  0.31415927f;   //  0.1π ≈ 18°
        rightLeg.roll   =  0.07853982f;   //  0.025π ≈ 4.5°
        leftLeg.pitch   = -1.4137167f;
        leftLeg.yaw     = -0.31415927f;
        leftLeg.roll    = -0.07853982f;
    }

    // ── Step 9: 팔 yaw 초기화, 아이템 사용 포즈 ─────────────
    rightArm.yaw = 0.0f;
    leftArm.yaw  = 0.0f;

    boolean isRightHanded = entity.getMainArm() == Arm.RIGHT;
    if (entity.isUsingItem()) {
        boolean isMainHand = entity.getActiveHand() == Hand.MAIN_HAND;
        if (isMainHand == isRightHanded) {
            positionRightArm(entity);
        } else {
            positionLeftArm(entity);
        }
    } else {
        boolean isTwoHandedLeft  = isRightHanded  && leftArmPose.isTwoHanded();
        boolean isTwoHandedRight = !isRightHanded && rightArmPose.isTwoHanded();
        if (isTwoHandedLeft || isTwoHandedRight) {
            positionLeftArm(entity);
            positionRightArm(entity);
        } else {
            positionRightArm(entity);
            positionLeftArm(entity);
        }
    }

    // ── Step 10: 팔 swing 보조 애니메이션 ───────────────────
    animateArms(entity, ageInTicks);

    // ── Step 11: 웅크리기(sneaking) 파트 위치 조정 ───────────
    if (this.sneaking) {
        body.pitch     = 0.5f;
        rightArm.pitch += 0.4f;
        leftArm.pitch  += 0.4f;
        rightLeg.pivotZ = 4.0f;
        leftLeg.pivotZ  = 4.0f;
        rightLeg.pivotY = 12.2f;
        leftLeg.pivotY  = 12.2f;
        head.pivotY     = 4.2f;
        body.pivotY     = 3.2f;
        leftArm.pivotY  = 5.2f;
        rightArm.pivotY = 5.2f;
    } else {
        body.pitch      = 0.0f;
        rightLeg.pivotZ = 0.0f;
        leftLeg.pivotZ  = 0.0f;
        rightLeg.pivotY = 12.0f;
        leftLeg.pivotY  = 12.0f;
        head.pivotY     = 0.0f;
        body.pivotY     = 0.0f;
        leftArm.pivotY  = 2.0f;
        rightArm.pivotY = 2.0f;
    }

    // ── Step 12: 스파이글라스 팔 swing ───────────────────────
    if (rightArmPose != ArmPose.SPYGLASS) {
        CrossbowPosing.swingArm(rightArm, ageInTicks,  1.0f);
    }
    if (leftArmPose != ArmPose.SPYGLASS) {
        CrossbowPosing.swingArm(leftArm,  ageInTicks, -1.0f);
    }

    // ── Step 13: leaningPitch > 0 (수영/크롤링) 팔 애니메이션 ─
    if (leaningPitch > 0) {
        float swimCycle = limbSwing % 26.0f;
        Arm preferredArm = getPreferredArm(entity);
        // handSwingProgress > 0이면 해당 팔 leanFactor = 0
        float rightLeanFactor = (preferredArm == Arm.RIGHT && handSwingProgress > 0) ? 0 : leaningPitch;
        float leftLeanFactor  = (preferredArm == Arm.LEFT  && handSwingProgress > 0) ? 0 : leaningPitch;

        if (!entity.isUsingItem()) {
            if (swimCycle < 14.0f) {
                // 구간 [0, 14): 팔 pitch/yaw → 0으로 lerp
                leftArm.pitch  = lerpAngle(leftLeanFactor,  leftArm.pitch,  0.0f);
                rightArm.pitch = MathHelper.lerp(rightLeanFactor, rightArm.pitch, 0.0f);
                leftArm.yaw    = lerpAngle(leftLeanFactor,  leftArm.yaw,   (float)Math.PI);
                rightArm.yaw   = MathHelper.lerp(rightLeanFactor, rightArm.yaw, (float)Math.PI);
                // roll: π + 1.8707964f * sin(swimCycle/sin(14)) lerp → π
                leftArm.roll   = lerpAngle(leftLeanFactor,  leftArm.roll,  (float)Math.PI + 1.8707964f * method_2807(swimCycle) / method_2807(14.0f));
                rightArm.roll  = MathHelper.lerp(rightLeanFactor, rightArm.roll, (float)Math.PI - 1.8707964f * method_2807(swimCycle) / method_2807(14.0f));
            } else if (swimCycle >= 14.0f && swimCycle < 22.0f) {
                // 구간 [14, 22): t = (swimCycle-14)/8, 팔 pitch → 90°*t
                float t = (swimCycle - 14.0f) / 8.0f;
                leftArm.pitch  = lerpAngle(leftLeanFactor,  leftArm.pitch,  1.5707964f * t);  // 90°*t
                rightArm.pitch = MathHelper.lerp(rightLeanFactor, rightArm.pitch, 1.5707964f * t);
                leftArm.yaw    = lerpAngle(leftLeanFactor,  leftArm.yaw,   (float)Math.PI);
                rightArm.yaw   = MathHelper.lerp(rightLeanFactor, rightArm.yaw, (float)Math.PI);
                // roll 계산 (복잡한 lerp 로직 생략 — 같은 구조)
            }
            // swimCycle >= 22 구간도 존재하나 패턴 동일
        } else {
            // isUsingItem 시에도 별도 팔 포즈 처리 (leaningPitch lerp 기반)
        }
        // hat.copyTransform(head) 추가 처리
        hat.copyTransform(head);
    }
}
```

---

## 5. 주요 수치 전체

| 수치 | 의미 |
|------|------|
| `0.017453292f` | π/180, 도→라디안 변환 계수 |
| `-0.7853982f` | -π/4 = -45°, fallFlying/수영 시 머리 pitch |
| `0.6662f` | 보행 애니메이션 주파수 계수 |
| `(float)Math.PI = 3.1415927f` | π |
| `-0.62831855f` | -π/5 ≈ -36°, riding 팔 pitch 추가 오프셋 |
| `-1.4137167f` | -0.45π ≈ -81°, riding 다리 pitch |
| `0.31415927f` | 0.1π ≈ 18°, riding 다리 yaw |
| `0.07853982f` | 0.025π ≈ 4.5°, riding 다리 roll |
| `0.005f` | 보행 시 다리 yaw/roll (분리감 표현) |
| `0.4f` | sneaking 팔 pitch 추가 오프셋 |
| `0.5f` | sneaking body pitch (앞으로 기울기) |
| `pivotZ = 4.0f` | sneaking 다리 앞쪽 이동 |
| `pivotY = 12.2f` | sneaking 다리 Y |
| `pivotY = 4.2f` | sneaking 머리 Y |
| `pivotY = 3.2f` | sneaking 몸체 Y |
| `pivotY = 5.2f` | sneaking 팔 Y |
| `pivotY = 12.0f` | 기본 다리 Y |
| `pivotY = 2.0f` | 기본 팔 Y |
| `0.2f` | fallFlying speedFactor 분모 |
| `26.0f` | 수영 애니메이션 한 사이클 (limbSwing % 26) |
| `14.0f`, `22.0f` | 수영 팔 애니메이션 구간 경계 |
| `1.8707964f` | 수영 팔 roll 최대값 (≈ 0.595π) |
| `1.5707964f` | π/2 = 90° |
| `0.25f` | 팔 Dilation 추가 (thinArms 오버레이용) |

---

## 6. 필드 Yarn 이름 목록

### BipedEntityModel 필드

| Yarn 이름 | intermediary | obf | desc | 의미 |
|-----------|-------------|-----|------|------|
| `head` | `field_3398` | `k` | `Lfyk;` | 머리 ModelPart |
| `hat` | `field_3394` | `l` | `Lfyk;` | 모자 ModelPart |
| `body` | `field_3391` | `m` | `Lfyk;` | 몸통 ModelPart |
| `rightArm` | `field_3401` | `n` | `Lfyk;` | 오른팔 ModelPart |
| `leftArm` | `field_27433` | `o` | `Lfyk;` | 왼팔 ModelPart |
| `rightLeg` | `field_3392` | `p` | `Lfyk;` | 오른다리 ModelPart |
| `leftLeg` | `field_3397` | `q` | `Lfyk;` | 왼다리 ModelPart |
| `sneaking` | `field_3400` | `t` | `Z` | 웅크리기 여부 |
| `leaningPitch` | `field_3396` | `u` | `F` | 수영/크롤링 기울기 계수 (0~1) |
| `leftArmPose` | `field_3399` | `r` | `Lfvx$a;` | 왼팔 포즈 (ArmPose enum) |
| `rightArmPose` | `field_3395` | `s` | `Lfvx$a;` | 오른팔 포즈 (ArmPose enum) |

### EntityModel 필드

| Yarn 이름 | intermediary | obf | desc | 의미 |
|-----------|-------------|-----|------|------|
| `riding` | `field_3449` | `d` | `Z` | 탑승 여부 |

### AnimalModel 필드

| Yarn 이름 | intermediary | obf | desc | 의미 |
|-----------|-------------|-----|------|------|
| `handSwingProgress` | `field_3447` | `c` | `F` | 손 흔들기 진행도 |

### PlayerEntityModel 필드

| Yarn 이름 | intermediary | obf | desc | 의미 |
|-----------|-------------|-----|------|------|
| `cloak` | `field_3485` | `G` | `Lfyk;` | 망토 ModelPart |
| `ear` | `field_3481` | `H` | `Lfyk;` | 귀 ModelPart |
| `thinArms` | `field_3480` | `I` | `Z` | 슬림 팔 여부 (Alex 스킨) |
| `leftSleeve` | `field_3484` | `b` | `Lfyk;` | 왼소매 ModelPart |
| `rightSleeve` | `field_3486` | `w` | `Lfyk;` | 오른소매 ModelPart |
| `leftPants` | `field_3482` | `x` | `Lfyk;` | 왼바지 ModelPart |
| `rightPants` | `field_3479` | `y` | `Lfyk;` | 오른바지 ModelPart |
| `jacket` | `field_3483` | `z` | `Lfyk;` | 재킷 ModelPart |

### ModelPart 필드

| Yarn 이름 | 의미 |
|-----------|------|
| `pitch` | X축 회전 (라디안) |
| `yaw` | Y축 회전 (라디안) |
| `roll` | Z축 회전 (라디안) |
| `pivotX` | X 피봇 위치 |
| `pivotY` | Y 피봇 위치 |
| `pivotZ` | Z 피봇 위치 |

---

## 7. BipedEntityModel.setAngles 주요 메서드

| Yarn 이름 | intermediary | 시그니처 | 의미 |
|-----------|-------------|---------|------|
| `setAngles` | `method_17087` | `(T, FFFFF)V` | 파트 각도 설정 (이 메서드) |
| `lerpAngle` | `method_2804` | `(FFF)F` | 각도 lerp (최단 경로) |
| `positionRightArm` | `method_30154` | `(LivingEntity)V` | 오른팔 포즈 설정 |
| `positionLeftArm` | `method_30155` | `(LivingEntity)V` | 왼팔 포즈 설정 |
| `animateArms` | `method_29353` | `(LivingEntity, F)V` | 팔 스윙 보조 |
| `getPreferredArm` | `method_2806` | `(LivingEntity)Arm` | 선호 팔 반환 |
| `copyTransform` | (ModelPart 메서드) | `(ModelPart)V` | 트랜스폼 복사 |

---

## 8. setAngles가 호출되기 전에 외부에서 설정되는 필드

`BipedEntityModel`의 `sneaking`/`leaningPitch`/`riding`은 `setAngles`에서 읽히지만,  
setAngles 안에서 설정되지 않는다. 이 값들은 렌더러가 `setAngles` 호출 전에 세팅한다.

설정 위치: `LivingEntityRenderer.render()` 내부 (다음 리서치 항목)

기대 패턴 (LivingEntityRenderer에서):
```java
model.sneaking = entity.isInSneakingPose();     // BipedEntityModel.sneaking 세팅
model.leaningPitch = entity.getLeaningPitch(tickDelta);  // BipedEntityModel.leaningPitch 세팅
model.riding = entity.hasVehicle();             // EntityModel.riding 세팅
// ... 그 후 model.setAngles(entity, ...) 호출
```

**[미확인]** 정확한 세팅 코드는 LivingEntityRenderer.render() 리서치에서 확인 필요.

---

## 9. SM 포팅 충돌 분석

### 충돌 1: sneaking 필드가 setAngles 전체를 제어

`BipedEntityModel.sneaking = true`이면:
- `body.pitch = 0.5f` (앞으로 기울기)
- 모든 파트의 pivotY가 +3.2~+5.2만큼 이동
- 팔 pitch에 +0.4f 추가

SM이 웅크리기 포즈(CROUCHING)를 사용하면, vanilla가 이 변환을 자동 적용한다.  
SM이 별도 자세(예: 크롤링)에서 CROUCHING 포즈를 사용하지 않더라도 `model.sneaking`이 true면 동일 변환이 적용된다.

**핵심**: `model.sneaking`은 `entity.isInSneakingPose()`가 아니라 렌더러에서 독립적으로 설정된다. SM Mixin에서 model.sneaking을 직접 제어 가능.

### 충돌 2: leaningPitch가 머리 pitch를 가로챔

`leaningPitch > 0`이면:
- 수영 포즈: `head.pitch = lerpAngle(leaningPitch, head.pitch, -45°)`
- 비수영(크롤링): `head.pitch = lerpAngle(leaningPitch, head.pitch, headPitch*deg2rad)`

SM 크롤링이 SWIMMING 포즈를 쓰면 `isInSwimmingPose() = true` → 머리가 -45° 방향으로 lerp.  
SM 크롤링이 SWIMMING 포즈를 쓰지 않아도 `leaningPitch > 0`이면 위 수식이 적용됨.

### 충돌 3: 수영 팔 애니메이션 (leaningPitch > 0, swimCycle 기반)

`leaningPitch > 0`이면 Step 13의 수영 팔 애니메이션이 추가 적용된다.  
SM이 수평 자세(크롤링)를 구현할 때 이 애니메이션이 SM 자체 팔 자세와 충돌할 수 있다.  
`leaningPitch`를 0으로 유지하면 이 블록 전체를 건너뜀.

### 충돌 4: 다리 swing 공식

발 애니메이션: `cos(limbSwing * 0.6662) * 1.4 * limbSwingAmount / speedFactor`

SM에서 `limbSwing`/`limbSwingAmount` 값을 조작하거나 자체 다리 pitch를 덮어쓰면, vanilla 공식과의 이중 적용 문제가 발생한다. `setAngles` 후에 Mixin으로 덮어써야 함.

### 충돌 5: 탑승 시 팔/다리 오버라이드

`this.riding = true`이면 팔/다리 pitch가 riding 값으로 강제 세팅됨(+= 또는 =).  
SM이 어떤 이동 중에 entity가 탑승 상태이면 예상치 않은 모델 포즈가 나타날 수 있다.

---

## 10. 렌더 파이프라인에서의 위치

```
LivingEntityRenderer.render()
  │
  ├─ model.sneaking = entity.isInSneakingPose()   [미확인 — 다음 리서치]
  ├─ model.leaningPitch = entity.getLeaningPitch() [미확인 — 다음 리서치]
  ├─ model.riding = entity.hasVehicle()            [미확인 — 다음 리서치]
  │
  └─ model.setAngles(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch)
       └─ PlayerEntityModel.setAngles()
            ├─ super.setAngles()  → BipedEntityModel.setAngles()
            │    ├─ head pitch/yaw 설정
            │    ├─ 팔/다리 swing 애니메이션
            │    ├─ riding 오버라이드
            │    ├─ sneaking pivot 조정
            │    └─ leaningPitch > 0 수영 팔 애니메이션
            ├─ 오버레이 copyTransform (pants, sleeves, jacket)
            └─ cloak pivot 설정 (isInSneakingPose × chest 장비)
```
