# 원본 SmartMoving 1.7.10 / 1.12.2 팔 휘두름 (swing) 애니메이션 처리 리서치

작성일: 2026-05-27.
요청자 메모: "1.12.2의 스마트무빙일 때 팔 휘두르는 에니메이션을 어떻게 처리하는지 완전 꼼꼼히 리서치".

## 결론 한 줄

**원본 1.7.10 + 1.12.2 모두 SM phase (isFly/isCrawl/isSlide/isClimb/isHeadJump/isFalling) 진입 시 vanilla swing 호출 자체 차단** — 즉 **원본은 SM phase 시 좌클릭 swing 시각 안 보임**.

## 원본 코드 위치

- 1.7.10: `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\render\SmartMovingModel.java`
- 1.12.2: `C:\Work\minecraft\porting\sm_porting_1_12_2\SmartMovingReboot\src\main\java\net\smart\moving\model\SMModel.java`

두 버전 거의 1:1 동일 식.

## 핵심 식 — `isStandard` flag

`setRotationAngles` 안:
```java
// SmartMovingModel.java L137~L552 (1.7.10) / SMModel.java L82~L587 (1.12.2)
isStandard = false;
if (isClimb || isCrawlClimb) { ... }
else if (isClimbJump) { ... }
else if (isCrawl) { ... }
else if (isSlide) { ... }
else if (isFly) { ... }      // 1.12.2 = isFlying
else if (isHeadJump) { ... }
else if (isFalling) { ... }
else
    isStandard = true;       // ← 일반 standing 만 isStandard=true
```

`isStandard` = SM phase 진입 시 false. 일반 standing/sprint/sneak/jump 시 true.

## `animateArmSwinging` — vanilla swing 호출 가드

```java
// 1.7.10 L640-L647 / 1.12.2 L677-L685
public void animateArmSwinging(...) {
    if (isStandard)                                  // ← SM phase 시 false → skip
        if (isAngleJumping)
            animateAngleJumping();                   // Angle Jump 분기
        else
            imp.superAnimateArmSwinging(...);        // ← vanilla swing 호출
}
```

→ **SM phase 시 vanilla `animateArmSwinging` 호출 안 됨** → swing 효과 (`bipedBody.rotateAngleY` / `arm.rotateAngleX/Y/Z` / `arm.rotationPointX/Z` 진폭) 적용 X.

### 추가 가드 — `animateArms`, `animateHeadRotation` 등도 동일 식

```java
public void animateArms(...) {
    if (isStandard)
        imp.superApplyAnimationOffsets(...);   // ← idle offset 진동도 SM phase 시 skip
}

public void animateHeadRotation(...) {
    setRotationAngles(...);
    if (isStandard)
        imp.superAnimateHeadRotation(...);
}

public void animateSneaking(...) {
    if (isStandard && !isAngleJumping)
        imp.superAnimateSneaking(...);
}
```

→ SM phase 시 모든 vanilla 애니메이션 시퀀스 (swing / sneak pivot / idle offset / 머리 회전) 차단.

## 예외 — `isWorking` (= swing 진행 중) 시 일부 분기 활성

```java
private boolean isWorking() {
    return mb.swingProgress > 0F;
}

public void animateWorkingBody(...) {
    if (isStandard)
        imp.superAnimateWorkingBody(...);
    else if (isWorking())
        animateNonStandardWorking(viewVerticalAngelOffset);  // ← SM phase + swing 시 별도 자세
}

public void animateWorkingArms(...) {
    if (isStandard || isWorking())
        imp.superAnimateWorkingArms(...);            // ← SM phase + swing 시 vanilla 호출
}

public void animateBowAiming(...) {
    if (isStandard)
        imp.superAnimateBowAiming(...);
    else
        animateNonStandardBowAiming(...);            // ← SM phase 시 별도 활 자세
}
```

### `animateNonStandardWorking` 식

```java
private void animateNonStandardWorking(float viewVerticalAngelOffset) {
    md.bipedRightShoulder.ignoreSuperRotation = true;
    md.bipedRightShoulder.rotateAngleX = viewVerticalAngelOffset / RadiantToAngle;
    md.bipedRightShoulder.rotateAngleY = md.workingAngle / RadiantToAngle;
    md.bipedRightShoulder.rotateAngleZ = Half;     // π/2 = 90° 직립
    md.bipedRightShoulder.rotationOrder = ZYX;
    md.bipedRightArm.reset();
}
```

→ **SM phase + swing 진행 시 `bipedRightShoulder` 라는 *별도 노드* 사용**:
- `bipedShoulder` = SR 모델 의 추가 노드 (= bipedArm 의 부모).
- `rotateAngleZ = π/2` = 어깨 90° 직립 (= 사용자 시점 정면).
- `rotateAngleX = vertical pitch` (= 화살표 위/아래 방향).
- `rotateAngleY = workingAngle` (= horizontal yaw).
- `ignoreSuperRotation = true` = SM phase 의 부모 회전 무시 (= 시각상 어깨가 사용자 시점 정면 향함).
- `bipedRightArm.reset()` = 자식 arm 의 추가 회전 제거.

**결과**: SM phase + swing 진행 시 `bipedRightShoulder` 가 사용자 시점 정면으로 어깨 회전 + arm reset → 화살 쏘기 / 채굴 / 공격 시각.

근데 1.21.1 vanilla biped 는 `bipedShoulder` 노드 없음 → 동일 식 적용 어려움.

## 1.7.10 / 1.12.2 vanilla `animateArmSwinging` 식 (참고)

```java
public void animateArmSwinging(EntityLivingBase entity) {
    float f = entity.swingProgress;
    bipedBody.rotateAngleY = MathHelper.sin(MathHelper.sqrt(f) * π * 2) * 0.2F;
    bipedRightArm.rotationPointZ = sin(body.rotateAngleY) * 5;
    bipedRightArm.rotationPointX = -cos(body.rotateAngleY) * 5;
    bipedLeftArm.rotationPointZ  = -sin(body.rotateAngleY) * 5;
    bipedLeftArm.rotationPointX  = cos(body.rotateAngleY) * 5;
    bipedRightArm.rotateAngleY += body.rotateAngleY;
    bipedLeftArm.rotateAngleY  += body.rotateAngleY;
    bipedLeftArm.rotateAngleX  += body.rotateAngleY;

    float f1 = 1.0F - f;
    f1 = f1 * f1 * f1 * f1;
    f1 = 1.0F - f1;
    float f2 = sin(f1 * π);
    float f3 = sin(f * π) * -(bipedHead.rotateAngleX - 0.7F) * 0.75F;
    ModelRenderer arm = getSwingingArm();
    arm.rotateAngleX -= f2 * 1.2F + f3;
    arm.rotateAngleY += body.rotateAngleY * 2.0F;
    arm.rotateAngleZ += sin(f * π) * -0.4F;
}
```

= vanilla 의 swing 진폭 식. swing 시 body / arm pivot+rotation 모두 변경.

## 우리 1.21.1 매핑 vs 원본 차이

### 원본 (1.7.10/1.12.2)
- SM phase 시 vanilla swing 호출 안 함 (= isStandard 가드).
- SM phase + swing 진행 시 `bipedShoulder` 라는 *별도 노드* 사용 (= 위 `animateNonStandardWorking`).
- 즉 SM phase 시 좌클릭 시각 = **어깨 90° 직립 자세** (= 정면 향함). swing 진폭 자체 없음.

### 우리 1.21.1 매핑
- **vanilla swing 항상 호출** (= 우리는 isStandard 가드 X). vanilla animateArms 가 모든 player 에 매 frame 적용.
- 비행 분기 (`sm_animateFlying`) 가 swing 일부 보존:
  - preferred arm 만 `setAnglesXZY` skip → vanilla swing 효과 잔존.
  - 부모 X 회전 `preCancelParentXPivot/Rotation` 으로 직립 자세 효과.
- 다른 SM phase (sliding/crawling/climbing) = swing 호출 자체는 됐지만 SM phase setAngles 가 즉시 override → swing 효과 사라짐.

**우리 매핑 = 원본 1:1 X**. 사용자 의도 (= 세션 65g `"휘두르는 팔만 처리"`) 우선 매핑.

## 사용자 의도 시나리오 별 비교

| 시나리오 | 원본 1.7.10/1.12.2 | 우리 1.21.1 (현재) |
|---------|---------------------|--------------------|
| 일반 standing swing | vanilla 정상 swing | vanilla 정상 swing |
| 비행 + swing | `bipedShoulder` 90° 직립 자세 (= 어깨 정면) | preferred arm vanilla swing 효과 잔존 + 부모 X cancel |
| 슬라이딩 + swing | `bipedShoulder` 90° 직립 자세 | SM 자세 set → vanilla swing 효과 cancel → swing 시각 X |
| 엎드리기 + swing | `bipedShoulder` 90° 직립 자세 | SM 자세 set → swing 시각 X |
| 클라이밍 + swing | `bipedShoulder` 90° 직립 자세 | SM 자세 set → swing 시각 X |

## 분석 — 사용자 의도 본질

사용자가 비행 swing 시각상 "빠르다" 인지한 cause:
1. vanilla `animateArmSwinging` 가 매 frame 호출 → swing progress 진행 (= 6 tick = 300ms).
2. 우리 sm_animateFlying 의 preferred arm preserve = vanilla 6 tick swing 그대로 시각화.
3. **원본 = `bipedShoulder` 직립 자세 + arm reset = swing 진폭 자체 없음 = 시각상 짧은 motion**.

원본 시각:
- swing 시 `bipedShoulder` 회전 (= 90° 직립). 한 번 set 후 swing 종료까지 *고정* (= 진폭 X).
- arm reset → arm 의 추가 회전 X.
- 사용자 시각 = "어깨 정면 + 팔 직립" 짧은 자세 변환.

우리 시각:
- swing 시 vanilla animateArmSwinging 식 → arm X 1.2 rad swing + Z 0.4 rad swing + Y rotation.
- 진폭 큰 motion → "빠르게 휘두름" 인지.

**즉 사용자 의도 = 원본 `bipedShoulder` 식 매핑**. 우리 매핑이 vanilla swing 그대로 두는 게 원본과 차이.

## 1.21.1 매핑 옵션

### 옵션 A — 원본 1:1 매핑 시도
- vanilla `animateArmSwinging` 호출 차단 (= mixin redirect).
- SM phase + swing 시 우리 식: `arm.pitch = -π/2` (= 어깨 정면, 직립), `arm.reset` (= 추가 회전 X).
- 진폭 없음 → 짧은 motion.

**문제**:
- 1.21.1 vanilla biped 는 `bipedShoulder` 노드 없음 → arm 자체로 매핑 필요.
- arm 의 부모 = bipedBody (= 회전 영향 받음). 부모 X cancel 필수.

### 옵션 B — 현재 비행 매핑 유지 + 진폭 줄임
- vanilla animateArmSwinging 그대로 호출.
- preferred arm preserve.
- swing 진행 식 변경 (= duration 늘림 또는 진폭 scale).

**문제**:
- duration 늘림 = 모든 mode 영향 (= 이전 fix #178 시도 결과 = 헤드점프 등 영향).
- 진폭 scale = vanilla 식 redirect 필요.

### 옵션 C — 현재 비행 매핑 유지 + 다른 SM phase 동일 식 적용
- 슬라이딩/엎드리기/클라이밍 분기에 비행 패턴 차용 (= fix #177).
- 원본과 다른 식이지만 사용자 의도 = "비행 코드 1:1 차용" 일치.

## 결론

- **원본 1.7.10/1.12.2 = SM phase 시 vanilla swing 차단** + `bipedShoulder` 별도 자세 사용.
- 우리 1.21.1 매핑 = vanilla swing 잔존 + preferred arm preserve (= 원본 X 식, 사용자 명시 요구).
- 사용자 의도 정확 매트릭스 필요:
  - "원본 1:1" = 옵션 A 진행. swing 시각 = 어깨 직립 자세 + 진폭 X.
  - "현재 비행 코드 식 일관성" = 옵션 C 진행. preferred arm preserve + 부모 X cancel.
  - "현재 비행 swing 진폭 줄임" = 옵션 B 진행 (= 다른 mode 영향 검증 필요).
