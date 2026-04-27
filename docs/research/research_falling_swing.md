# Falling 분기 swing arm 1:1 매핑 리서치

> 작업 일자: 2026-04-27
> 사용자 보고: "공중 낙하할 때 공격 휘두름 시 오른팔만 지금처럼 휘둘러지고
>   몸통이 휘두르는 에니메이션 때문에 움찔움찔 움직이는건 없어야함.
>   (몸통은 그냥 낙하 에니메이션만 그대로, 휘두르는 에니메이션은 오른팔만 지금처럼 그대로 적용)"
> 원칙: 1.21.1 vanilla 효과 분석 + 비행 swing 패턴 1:1 재사용.

---

## 1. vanilla `BipedEntityModel.animateArms` 동작 (1.21.1, javap 디스어셈블)

setAngles Step 10 — `super.setAngles()` 마지막에 호출.

```java
protected void animateArms(LivingEntity entity, float ageInTicks) {
    if (handSwingProgress <= 0) return;     // swing 진행 시에만 실행

    Arm preferredArm = getPreferredArm(entity);
    ModelPart preferred = getArm(preferredArm);
    float swing = handSwingProgress;

    // 🔴 body.yaw 흔들림 (사용자 "몸통 움찔움찔" 의 직접 원인)
    body.yaw = sin(sqrt(swing) * 2π) * 0.2f;
    if (preferredArm == LEFT) body.yaw *= -1f;

    // 양 팔 pivot 회전 (어깨 위치를 body 회전에 맞춰 이동)
    rightArm.pivotZ =  sin(body.yaw) * 5f;
    rightArm.pivotX =  cos(body.yaw) * 5f;
    leftArm.pivotZ  = -sin(body.yaw) * 5f;
    leftArm.pivotX  = -cos(body.yaw) * 5f;

    // 모든 팔 yaw + body.yaw 누적
    rightArm.yaw  += body.yaw;
    leftArm.yaw   += body.yaw;
    leftArm.pitch += body.yaw;

    // preferred arm 의 pitch/yaw/roll — 실제 휘두르는 효과
    float swingDecay = 1f - swing^4;     // 회전량 감쇠
    preferred.pitch -= sin(sqrt(swing) * π) * 1.2f
                    + sin(swing * π) * (0.7f - head.pitch);
    preferred.yaw   += body.yaw * 2f;
    preferred.roll  += sin(swing * π) * -0.4f;
}
```

→ swing 진행 시 body 가 좌우로 약 ±11° (0.2 rad) 흔들림. 사용자가 본 "움찔움찔" 의 직접 원인.

---

## 2. falling 분기와 vanilla swing 의 충돌 흐름

`MixinPlayerEntityModelClient.sm_setAngles` 흐름 (`@Inject TAIL`):

1. **vanilla setAngles 실행** (super) → vanilla animateArms 가 `body.yaw`, preferred arm pivot/회전 변경.
2. TAIL inject 진입 → sm_setAngles 본체.
3. `anySmState || !cfgEnabled` reset 블록 (L105-110) — `body.yaw = 0` 등 reset.
   하지만 **falling 은 anySmState 에 포함 안 됨** (L92-94 — 11개 SM 상태 + flyingCreative 만):
   ```java
   boolean anySmState = sm.isRopeSliding || sm.isClimbing || sm.isCrawlClimbing
           || sm.isCeilingClimbing || sm.isClimbJumping || sm.isSwimming_sm
           || sm.isDiving || sm.isCrawling || sm.isSliding || sm.isHeadJumping
           || flyingCreative;
   ```
   → **falling 에선 body.yaw reset 안 됨** = 사용자 보고 "몸통 움찔움찔" 그대로 잔존.
4. `if-else` 체인 진입 → falling 분기 (else) → `sm_animateFalling` 호출.
5. `sm_animateFalling` 의 `setAnglesXZY(rightArm, ...)` / `setAnglesXZY(leftArm, ...)` 가
   양 팔 회전 모두 **덮어씀** → vanilla swing (preferred arm 의 pitch/yaw/roll) 손실.
   사용자 표현 "오른팔만 지금처럼 휘둘러지고" 는 아마 이 덮어쓴 결과 + `pivotX/Z` 잔존 효과로
   미세한 흔들림이 보이는 상태.

---

## 3. 해결 패턴 — 비행 분기 (`sm_animateFlying` L611-708) 1:1 차용

비행 분기에서 이미 검증된 패턴 (메모리 `project_flying_complete.md` — 13단계 시행착오 정착):

```java
float swing = player.handSwingProgress;
Arm preferredArm = player.getMainArm();
boolean preserveRight = swing > 0F && preferredArm == Arm.RIGHT;
boolean preserveLeft  = swing > 0F && preferredArm == Arm.LEFT;

if (!preserveRight) setAnglesXZY(rightArm, 0f, rYaw, rRoll);
if (!preserveLeft)  setAnglesXZY(leftArm,  0f, lYaw, lRoll);
```

비행 분기는 추가로 **부모 X 회전 cancel** (`preCancelParentXRotation/Pivot`) 필요 — `setupTransforms` 에서
`POSITIVE_X(-θ)` 적용되기 때문. **falling 분기는 setupTransforms 에 없음** (확인됨,
`MixinPlayerEntityRenderer.sm_setupTransforms` L321/L330 — `isFlying` 분기만 X 기울기 적용).

→ falling 은 **preCancel* 불필요**. 단순히 setAnglesXZY skip 만 하면 충분.

---

## 4. body.yaw cancel — falling 만의 추가 단계

비행은 `flyingCreative` 가 anySmState 에 포함되어 자동으로 `body.yaw = 0` reset (L92-110).
falling 은 anySmState 미포함 → falling 분기 안에서 별도로 처리 필요.

**선택지**:
- (a) `anySmState` 에 isFalling 추가 → reset 인프라가 자동 처리.
- (b) `sm_animateFalling` 안에서 `body.yaw = 0` 직접 set (swing > 0 시에만).

(a) 는 reset 인프라가 `head.pivotZ / body.pivotZ / head.roll` 까지 함께 reset 하지만,
falling 분기는 head/body 를 안 건드리므로 jacket reset 으로 vanilla 기본값 (0) 강제 안전.
다만 `head.pivotZ = 0` reset 이 vanilla animateArms 가 head.pivotZ 변경 안 하므로 중립 (영향 없음).

→ **(a) 가 일관성 있음** (다른 분기와 동일 처리).

---

## 5. 매핑 결정

### 5-1. anySmState 에 isFalling 추가

`sm_setAngles` L92-94:
```java
boolean isFallingForReset = !player.isOnGround()
        && player.fallDistance > SmartMovingConfig.Config.fallAnimationDistanceMinimum
        && !sm.isClimbing && !sm.isCrawlClimbing && !sm.isCeilingClimbing
        && !player.isTouchingWater();
boolean anySmState = ... || sm.isHeadJumping || flyingCreative || isFallingForReset;
```

→ falling 진입 시 body.yaw / head.pivotZ / body.pivotZ / head.roll 모두 0 reset.
사용자 "몸통 움찔움찔" 원인 = body.yaw 흔들림 → cancel 됨.

### 5-2. sm_animateFalling 에 preferred arm preserve

```java
private void sm_animateFalling(SmartMovingClientState sm, ClientPlayerEntity player) {
    ...
    float swing = player.handSwingProgress;
    Arm preferredArm = player.getMainArm();
    boolean preserveRight = swing > 0F && preferredArm == Arm.RIGHT;
    boolean preserveLeft  = swing > 0F && preferredArm == Arm.LEFT;
    ...
    if (!preserveRight) setAnglesXZY(rightArm, 0f, rYaw, rRoll);
    if (!preserveLeft)  setAnglesXZY(leftArm,  0f, lYaw, lRoll);
    ...
}
```

→ preferred arm (오른손잡이면 오른팔) 의 vanilla swing pitch/yaw/roll 그대로 보존.
다른 팔은 falling 진동 자세 유지.

### 5-3. 부모 X 회전 cancel — **불필요**

falling 은 setupTransforms 에서 X 회전 안 하므로 preCancel* skip.

### 5-4. 다리 / head 처리 — **변경 없음**

vanilla animateArms 는 다리 변경 안 함 (Step 6/7 의 limb swing 만, falling 의 setAngles 가 덮어씀).
head 도 마찬가지. → falling 의 다리/머리 자세 그대로 유지 (사용자 요구 "낙하 에니메이션 그대로").

---

## 6. 검증 계획

1. **빌드**: gradle compile.
2. **시각 결과 예측**:
   - swing 시 `body.yaw = 0` (안 흔들림) — 몸통 움찔움찔 사라짐.
   - 오른팔 (preferred=RIGHT 시) — vanilla swing pitch/yaw/roll 그대로 = 현재 비행 swing 과 동일한 휘두름.
   - 왼팔 — falling 진동 자세 유지 (vanilla animateArms 의 leftArm.yaw += body.yaw 효과는 setAnglesXZY 가 덮어씀).
   - 다리 — falling 진동 자세 유지.
3. **인게임 통합테스트**: 메모리 정책에 따라 deferred.

---

## 7. 참고

- `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java`
  - L92-94: anySmState 정의
  - L105-110: reset 인프라 (body.yaw = 0 등)
  - L161-170: falling 분기 (sm_animateFalling 호출)
  - L610-708: sm_animateFlying — preserve 패턴 참조
  - L749-787: sm_animateFalling — 변경 대상
- `docs/research/vanilla/PlayerEntityModel_setAngles.md` — vanilla setAngles 전체 흐름
- 메모리 `project_flying_complete.md` — 비행 swing 13단계 시행착오 정착 (참고만, falling 은 더 단순)
- vanilla `BipedEntityModel.animateArms` — javap 디스어셈블 결과 (본 문서 §1)
