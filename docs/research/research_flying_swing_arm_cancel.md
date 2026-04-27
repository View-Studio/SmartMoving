# 비행 중 공격 swing arm 매핑 — 좌표계 부호 반전 분석

## 배경

세션 65 (b ~ q) 비행 중 공격 swing arm 의 직립 자세 매핑. 사용자 의도:
- 다른 모델 (몸/다리/다른 팔/head) → sm 비행 자세 (X 기울기 + 날개짓) 그대로.
- preferred arm (오른팔, 휘두르는 팔) → "이동 방향 향한 직립 플레이어 오른팔" 처럼 vanilla swing.

## vanilla render 흐름 (1.21.1 LivingEntityRenderer.render)

setupTransforms 호출 전후의 matrices.* 호출 (디스어셈블리 분석):

```
1. matrices.translate (Direction.getOffsetZ * 어떤 값)        // X 축 보정
2. matrices.scale(entityScale, entityScale, entityScale)
3. setupTransforms(entity, matrices, animProgress, bodyYaw, tickDelta, scale)
   = matrices.multiply(POSITIVE_Y.rotationDegrees(180 - bodyYaw))
   = (우리 mixin TAIL: translate(0, 1.5, 0) → R_y(-yaw) → R_x(-θ) → translate(0, -1.5, 0))
4. matrices.scale(-1.0F, -1.0F, 1.0F)                         ← ★ 핵심: 좌표계 Y 부호 반전
5. scale(entity, matrices, tickDelta) (자식 클래스 추가 scale)
6. matrices.translate(0, -1.501F, 0)
7. animateModel, setAngles 호출 (← 우리 sm_setAngles inject 시점)
8. ModelPart.render → translate(pivot/16) + multiply(rotationZYX(roll, yaw, pitch))
```

`matrices.scale(-1, -1, 1)` 의 Y 부호 반전이 ModelPart 좌표계 ↔ world 좌표계 사이 부호를 반전시킨다.

## 핵심 수학

### 회전 부호

ModelPart R_x(theta) 가 vertex 에 적용된 후 scale(-1,-1,1) 에 의해 Y 좌표 반전:
- ModelPart vertex: (x, y, z)
- 회전 후: (x, y*cos(θ) - z*sin(θ), y*sin(θ) + z*cos(θ))
- scale 적용: (-x, -y*cos(θ) + z*sin(θ), y*sin(θ) + z*cos(θ))
- = world 시점에서 R_x(-θ) 가 적용된 것과 등가

→ **ModelPart R_x(+theta) = world R_x(-theta)**

### Cancel 부호 보정

부모 setupTransforms 의 R_x(-θ) (world 시점) cancel 위해서는:
- world 시점 R_x(+θ) 자식 회전 필요
- = ModelPart 시점 R_x(-theta) 적용

**잘못된 매핑** (세션 65b ~ 65k):
```java
Quaternionf qNew = new Quaternionf().rotationX(theta).mul(qOrig);  // ModelPart R_x(+θ)
```
→ world 적용: R_x(-θ) → 부모 R_x(-θ) * R_x(-θ) = R_x(-2θ).
→ θ = π/2 (수평 비행) 시 arm 이 -π 회전 (= 180° 더 뒤로) → 사용자 보고 "90° 뒤로 젖혀진 느낌" 정확히 매칭.

**정확한 매핑** (세션 65q):
```java
Quaternionf qNew = new Quaternionf().rotationX(-theta).mul(qOrig);  // ModelPart R_x(-θ)
```
→ world 적용: R_x(+θ) → 부모 R_x(-θ) cancel → identity. ✓

### Pivot 위치 보정

회전 cancel 만으로는 arm 위치는 부모 R_x(-θ) 의 영향을 받음 (회전 중심 (0, 1.5, 0) world 기준).

세션 65k 후 사용자 보고: 회전만 cancel → arm pivot 위치는 슈퍼맨 자세 어깨 위치 → arm 회전이 직립 효과지만 위치는 비행 자세.

**arm pivot 위치 보정**:
- 회전 중심 ModelPart 좌표 ≈ (0, 0, 0) (= world (0, 1.5, 0) 가 scale + translate 보정 후 ModelPart 좌표).
  - scale^-1 * (0, 1.5, 0) = (0, -1.5, 0).
  - translate(0, -1.501, 0)^-1 * (0, -1.5, 0) = (0, 0.001, 0) ≈ (0, 0, 0).
- ModelPart R_x(-θ) 적용 (회전 중심 (0, 0, 0)):
  ```
  new_pivotY = py * cos(θ) + pz * sin(θ)
  new_pivotZ = -py * sin(θ) + pz * cos(θ)
  pivotX 그대로 (X 축 회전이라 영향 없음)
  ```

검증 (right arm pivot (-5, 2, 0), θ = π/2):
- new_pivotY = 2*0 + 0*1 = 0
- new_pivotZ = -2*1 + 0*0 = -2
- 새 pivot = (-5, 0, -2)

부모 transform 적용 후 world 위치:
- T(0, -1.501, 0) * T(-5/16, 0, -2/16) = (-0.3125, -1.501, -0.125)
- scale(-1,-1,1) 적용 = (0.3125, 1.501, -0.125)
- setupTransforms (R_x(-π/2) at center (0, 1.5, 0)):
  - T(0, -1.5, 0) * (0.3125, 1.501, -0.125) = (0.3125, 0.001, -0.125)
  - R_x(-π/2) * (0.3125, 0.001, -0.125) = (0.3125, -0.125, -0.001)
  - T(0, 1.5, 0) * = (0.3125, 1.375, -0.001)
- = world (0.3125, 1.376, 0) ≈ 직립 어깨 위치 ✓

## 최종 매핑 (세션 65q)

```java
// MixinPlayerEntityModelClient.java sm_animateFlying 안:
if (swing > 0F) {
    float thetaCancel = lerpFadeAngle(...);  // setupTransforms 와 동일한 fade 보간
    if (preserveRight) {
        preCancelParentXPivot(rightArm, thetaCancel);
        preCancelParentXRotation(rightArm, thetaCancel);
    }
    if (preserveLeft) {
        preCancelParentXPivot(leftArm, thetaCancel);
        preCancelParentXRotation(leftArm, thetaCancel);
    }
}

private static void preCancelParentXRotation(ModelPart part, float theta) {
    Quaternionf qOrig = new Quaternionf().rotationZYX(part.roll, part.yaw, part.pitch);
    Quaternionf qNew = new Quaternionf().rotationX(-theta).mul(qOrig);  // 부호 반전
    Vector3f e = qNew.getEulerAnglesZYX(new Vector3f());
    part.pitch = e.x; part.yaw = e.y; part.roll = e.z;
}

private static void preCancelParentXPivot(ModelPart part, float theta) {
    float py = part.pivotY, pz = part.pivotZ;
    float cos = MathHelper.cos(theta), sin = MathHelper.sin(theta);
    part.pivotY = py * cos + pz * sin;
    part.pivotZ = -py * sin + pz * cos;
}
```

## 결과

- 수직 비행 (theta=0): cancel = identity → vanilla swing 그대로 (변화 없음).
- 수평 비행 (theta=π/2): arm pivot 직립 어깨 위치 + arm 회전 직립 swing → 시각 결과 = 수직 시와 동일.
- 다른 모델 (몸/다리/다른 팔/head) → sm 비행 자세 그대로 유지.

## 시행착오 기록 (참고용)

세션 65b ~ 65q 13단계 매핑:
- 65b: setAnglesXZY skip (preferred arm 만).
- 65c: 단순 `pitch += theta` (회전 비교환성 무시).
- 65d: quaternion R_x(+theta) cancel (회전만, scale 부호 + 위치 무시).
- 65e: 부모 X+Y cancel + mouseYaw (사용자 의도 추측 잘못).
- 65f: 모든 모델 reset (사용자 의도 정반대).
- 65g: 65b + 65d 매핑 복원.
- 65h: 65g + 같은 frame fade 보간.
- 65i: limbSwing 진폭 cancel (잘못된 가정).
- 65j: vanilla animateArms 직접 set (모든 vanilla 효과 cancel).
- 65k: 65h 복원 (revert 65j).
- 65l: 위치 보정 시도 (회전 중심 24 가정 + 부호 잘못).
- 65m: yaw +90° (사용자 단순 요청).
- 65n: theta 비례 yawAdd (X/Z 한정).
- 65o: R_y → R_z (회전 축 변경).
- 65p: cancel 자체 제거 (사용자 발언 오해).
- **65q: 부호 반전 + 위치 보정 (정답)**.

근본 원인: vanilla render 의 `scale(-1, -1, 1)` 부호 반전을 65l 시점에야 디스어셈블리에서 발견. 그 전엔 회전 합성 수학만 검증.

## 향후 ModelPart 회전 cancel 매핑 시 체크리스트

1. vanilla render (LivingEntityRenderer.render 또는 PlayerEntityRenderer.render) 디스어셈블리에서 setupTransforms 전후의 모든 matrices.* 호출 파악.
2. scale(-1, -1, 1) 같은 부호 반전 transform 있는지 확인 → 있으면 ModelPart 회전 부호 반전 보정.
3. 부모 회전이 회전 중심 (= T(center) * R * T(-center)) 구조면 위치 보정도 필요.
4. 회전 중심을 world 좌표 → ModelPart 좌표 환산 시 scale + translate 모두 거꾸로 적용.
5. 적용 전 두 케이스 (cancel 0 vs cancel max) 의 수학 결과 직접 계산해서 검증.
