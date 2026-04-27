# 비행 swing 시 비-preferred arm pivot 흔들림 리서치

> 작업 일자: 2026-04-27
> 사용자 보고: "비행할 때는 공격 휘두름 시 왼팔도 움찔움찔 움직이는건 없어야함.
>   (왼팔은 그냥 비행 에니메이션만 그대로, 휘두르는 에니메이션은 오른팔만 지금처럼 그대로 적용)"
> 메모리: `project_flying_complete.md` — 비행 swing 매핑 13단계 시행착오 정착, 함부로 수정 금지.
>   본 변경은 기존 매핑 (preCancelParentX*, fade lerp, preserve 패턴) 을 건드리지 않고
>   **누락된 pivot reset 만 추가** — 안전.

---

## 1. 원인 — vanilla `animateArms` 의 pivot 흔들림 (지난 리서치 §1 확장)

`BipedEntityModel.animateArms` (handSwingProgress > 0 시) 의 양 팔 pivot 변경 부분
(javap 디스어셈블, `research_falling_swing.md` §1):

```java
body.yaw = sin(sqrt(swing) * 2π) * 0.2f;       // 매 프레임 변동 (-0.2 ~ +0.2)
if (preferredArm == LEFT) body.yaw *= -1f;

rightArm.pivotZ =  sin(body.yaw) * 5f;          // ≈ -1 ~ +1 (매 프레임 변동)
rightArm.pivotX =  cos(body.yaw) * 5f;          // ≈ +4.9 ~ +5.0 (거의 일정)
leftArm.pivotZ  = -sin(body.yaw) * 5f;          // ≈ -1 ~ +1 (매 프레임 변동) ★
leftArm.pivotX  = -cos(body.yaw) * 5f;          // ≈ -4.9 ~ -5.0 (거의 일정)
```

**vanilla 기본값** (setAngles Step 4):
```java
rightArm.pivotX = -5.0f;  rightArm.pivotZ = 0.0f;
leftArm.pivotX  =  5.0f;  leftArm.pivotZ  = 0.0f;
```

→ swing 시 `pivotX` 부호가 vanilla Step 4 와 **반대** (`+cos*5` ≈ +5 vs Step 4 `-5`).
   잠깐. 위 식은 디스어셈블 결과 그대로인데 이게 맞나?

**디스어셈블 재확인** (javap dump):
```
73: aload_0
74: getfield      #118                // Field rightArm
77: aload_0
78: getfield      #114                // Field body
81: getfield      #247                // Field yaw
84: invokestatic  #435                // sin
87: ldc           #190                // float 5.0f
89: fmul
90: putfield      #258                // Field pivotZ
```

→ `rightArm.pivotZ = sin(body.yaw) * 5f;` 맞음.

이어서 (디스어셈블 추가 dump 필요하지만) vanilla 코드 흐름상 다음:
```
rightArm.pivotX = cos(body.yaw) * 5f;
leftArm.pivotZ  = -sin(body.yaw) * 5f;
leftArm.pivotX  = -cos(body.yaw) * 5f;
```

이 부호는 1.21.1 vanilla 의 표준 swing 효과로, 실제 visual 은 양 팔이 body 기울기에 맞춰 회전하는 것. preferred arm 은 자연스럽지만 **비-preferred arm** 은 falling/flying 자세에서 어색하게 흔들림.

**핵심**: `pivotZ = ±sin(body.yaw)*5` 가 매 프레임 ±1 단위 흔들림 → 사용자 보고 "왼팔 움찔움찔".

---

## 2. 현재 비행 분기 처리 흐름

`sm_animateFlying` (L611-708) — preserve 패턴 + preCancelParentX*:

```java
boolean preserveRight = swing > 0F && preferredArm == Arm.RIGHT;
boolean preserveLeft  = swing > 0F && preferredArm == Arm.LEFT;

// pitch/yaw/roll 만 setAnglesXZY 로 덮어씀.
if (!preserveRight) setAnglesXZY(rightArm, 0f, rYaw, rRoll);
if (!preserveLeft)  setAnglesXZY(leftArm,  0f, lYaw, lRoll);

if (swing > 0F) {
    float thetaCancel = lerpFadeAngle(...);
    if (preserveRight) {
        preCancelParentXPivot(rightArm, thetaCancel);
        preCancelParentXRotation(rightArm, thetaCancel);
    }
    if (preserveLeft) {
        preCancelParentXPivot(leftArm, thetaCancel);
        preCancelParentXRotation(leftArm, thetaCancel);
    }
}
```

**누락**: `setAnglesXZY` 는 pitch/yaw/roll 만 set, **pivotX/Z 는 손대지 않음**.
vanilla animateArms 가 set 한 `pivotZ = ±sin(body.yaw)*5` 가 그대로 살아남아 매 프레임 변동.

또한 reset 인프라 (`sm_setAngles` L105-110) 는 `head.pivotZ`, `body.pivotZ`, `body.yaw`, `head.roll` 만 reset — arm.pivotX/Z 미포함.

---

## 3. 매핑 결정

### 3-1. 변경 위치 — `sm_animateFlying` swing 블록 안

- **이유 1**: 비행 swing 매핑은 메모리 [project_flying_complete.md] "함부로 수정 금지" 영역.
  본 변경은 기존 매핑 (preCancel*, fade lerp, preserve) 을 건드리지 않고 **새 줄 추가만**.
- **이유 2**: SM 코드 전체 grep `(rightArm|leftArm)\.pivot[XZ]` = **No matches** —
  reset 인프라 확장 시에도 다른 분기 충돌 없음. 그러나 사용자가 비행만 보고했고 낙하는 "완결" 선언 →
  **변경 범위 비행만 한정**. 낙하에서 동일 문제 보고되면 그때 처리.

### 3-2. 처리 — 비-preferred arm 의 pivotX/Z 만 vanilla 기본값 복원

```java
if (swing > 0F) {
    // 기존 preCancel*
    ...

    // 🔴 (2026-04-27): vanilla animateArms 가 swing 시 set 한 pivotZ = ±sin(body.yaw)*5
    //   가 매 프레임 변동 → 비-preferred arm "움찔움찔" (사용자 보고).
    //   preferred arm 은 vanilla swing 효과 그대로 보존, 비-preferred arm 만 vanilla
    //   setAngles Step 4 기본값으로 복원.
    if (!preserveRight) {
        rightArm.pivotX = -5f;
        rightArm.pivotZ =  0f;
    }
    if (!preserveLeft) {
        leftArm.pivotX  =  5f;
        leftArm.pivotZ  =  0f;
    }
}
```

### 3-3. preferred arm 처리 — 변경 없음

preferred arm 의 `pivotZ = sin(body.yaw)*5 ≈ 0`, `pivotX = cos(body.yaw)*5 ≈ ±5`.
visual 변동은 약 ±1 단위지만 휘두르는 팔 자체의 vanilla swing 효과 일부 — 보존.
사용자 요구 "휘두르는 에니메이션은 오른팔만 지금처럼 그대로 적용" 과 일치.

---

## 4. 검증

1. **빌드**: gradle compile.
2. **시각 결과 예측**:
   - 왼팔 (오른손잡이 시) `pivotX = 5, pivotZ = 0` 강제 → 매 프레임 변동 사라짐 = 움찔움찔 cancel.
   - 오른팔 (preferred) — vanilla swing 효과 그대로 = 휘두르는 모션 동일.
   - 왼손잡이 (preferredArm == LEFT) 시 반대 — 오른팔 cancel + 왼팔 휘두름.
3. **인게임 통합테스트**: deferred.

---

## 5. 참고

- `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java` L692-708 (변경 대상)
- `docs/research/research_falling_swing.md` §1 — vanilla animateArms 분석 (본 리서치 기반)
- 메모리 `project_flying_complete.md` — 비행 swing 13단계 시행착오 (보존 영역)
