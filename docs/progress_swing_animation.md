# Swing 애니메이션 작업 진행 상황 (2026-05-27)

사용자 보고: "엎드리기/슬라이딩/클라이밍/비행 시 좌클릭 swing 시각 이상". 다중 fix 시도 후 dump 분석 단계.

## 현재 상태 (= 미해결)

**dump 결과 fact 확정**. 사용자 시각 인지 검증 미수신.

### 시도된 fix 들

| fix | 위치 | 상태 |
|-----|------|------|
| #177 | sm_animateClimbing/Crawling/Sliding 에 preferred arm preserve 추가 (= fix #177 식). | ✅ 적용됨 (= 메모리 verified) |
| #178 | `MixinLivingEntity.sm_extendHandSwingDuration` (= swing duration +4). | ❌ revert. 모든 mode 영향 → 헤드점프 등 회귀. |
| #179 v2 | sm_animateFlying 의 preferred arm `arm.roll += π/2` (= 어깨 90° 직립). | ❌ revert. dump 결과 vanilla swing pitch 보존 cancel → "한 축 까딱". |
| #180 v2 | reset 인프라의 swing 영향 항목 (body.yaw, arm.pivot, arm.roll) self+swing 시 cancel skip. | ✅ 적용됨. STAND swing 정상화. |
| #181 | vanilla animateArms 식 수동 재적용 (= 수영 fix #146 패턴 차용). | ❌ revert. 사용자 verbatim "swing 너무 크게 + 반대 방향". vanilla 식 2 배 누적. |
| #182 | sm_animateHeadJumping 에 fix #177 식 추가 (= preferred arm preserve + preCancelParentX). | ✅ 적용됨. HJ swing dump 결과 preserve 작동 확인. |
| #175 v5 | (sliding rapid toggle fix — swing 무관) | ✅ 별도 작업 정착 |

### 검증된 dump fact (= peak swing frame, swing=0.5)

| Phase | T1 vanilla rArm pitch | T5 우리 결과 | preCancelParentX theta | 비고 |
|-------|------|------|------|------|
| STAND | -0.97 | -0.97 (= vanilla 보존) | N/A | ✅ |
| FLY | -0.97 | -0.94 (= 거의 vanilla) | (QUARTER - verticalAngle) * speedFactor ≈ 0 | ✅ |
| HJ | -1.30 | **-2.82** | QUARTER - verticalAngle (= 큼) | preCancelParentX 큰 변형 |
| CR | -1.07 | **-2.45** | π/2 - π/16 = 1.37 | preCancelParentX 큰 변형 |
| SLD | (peak 미확정) | (preCancelParentX 변형) | π/2 = 1.57 | |
| CL | -1.07 추정 | -1.07 추정 (= R_x 없음 → preCancelParentX 미적용) | (적용 X) | ✅ |

### dump 5 단계 (= 디버그용)

`MixinPlayerEntityModelClient.sm_setAngles`:
- **T1 (`SWING-T1-VANILLA`)**: inject 진입 직후 (= vanilla setAngles + animateArms 결과).
- **T2 (`SWING-T2-RESET`)**: reset 인프라 직후 (= fix #180 v2 가드 작동 확인).
- **T3 (`SWING-T3-PRE`)**: SM phase 분기 진입 직전.
- **T4 (`SWING-T4-POST`)**: sm_animateXxx 호출 직후.
- **T5 (`SWING-T5-END`)**: inject 끝 (= outer layer copy 후).

self + `handSwingProgress > 0` 가드.

### 결정 미수신 — 사용자 시각 인지

dump fact 만으로는 사용자 의도 식별 불가. 사용자 verbatim "swing 안 보임 / 한 축 까딱 작음" 모호. 정확 cause:
- (A) **swing motion 자체 작음** (= FLY peak -55° 도 작음) → vanilla 1.21.1 식 자체 부족.
- (B) **SM phase 의 swing 방향 부정확** → preCancelParentX 부호/식 검토.
- (C) **HJ/CR 의 preCancelParentX 큰 변형 (= pitch ~vanilla*2)** 이 시각상 큰 motion 정상 vs 비정상.
- (D) **lArm 정적 SM 자세** 가 사용자 인지 "한 축" cause.

각 phase 별 사용자 시각 인지 명시 보고 필요.

## 사용자 verbatim 관련 메모

- "엎드리기/슬라이딩/클라이밍 좌클릭 시 팔 안 휘둘러짐" (= 작업 시작).
- "비행 코드 무조건 참고. 1.7.10/1.12.2 원본 코드도 참고".
- "비행 swing 도 좀 빠르게 보임. 속도 줄여" (= fix #178 시도 → 회귀).
- "팔이 한축만으로 까딱까딱 완전 조금 움직임" (= fix #179 v2 후 dump).
- "지금 비행도 잘 안 되고 있다".
- "STAND swing 도 좀 이상" (= fix #180 v2 검증 결과 vanilla 정상화).
- "(궁금증) STAND swing 은 그냥 기본으로 두면 안 되나?" (= 통찰 매치, fix #180 v2 가드 식).
- "스마트무빙 시 반대로 휘두르는 느낌도 남" (= fix #181 시 보고).
- "객관식 X. 추측 X. 메모리 + 원본 + dump fact 만".

## 메모리 verified 패턴 (= 검증된 식)

- `feedback_animateArms_cancel`: reset 인프라가 body.yaw / arm.pivot / arm.roll cancel + preferred arm preserve 패턴 ("움찔움찔" 차단 fix 검증).
- `feedback_render_scale_negation`: vanilla scale(-1,-1,1) Y 부호 반전.
- `feedback_zxy_zyx_rotation_order`: vanilla ZYX 회전 순서 + 작은 yaw (< π/4) 직접 set 안전.
- `feedback_setupTransforms_translate_axis`: TAIL inject translate 의 축 변환 (= modelpart Y → world Z).
- 수영 분기 `fix #146`: arm.pivot 의 R_y(bodySway) 변환 패턴 (= vanilla animateArms 의 식과 동일).

## 원본 1.7.10/1.12.2 fact 정리

`SRModel.animateWorkingArms` (= SM phase + swing 시도 호출):
```java
public void animateWorkingArms() {
    float f6 = 1 - swingProgress;
    f6 = 1 - f6³;
    float f7 = sin(f6 * π/2);
    float f8 = sin(swing * π/2) * -(head.pitch - 0.7) * 0.75;
    rArm.pitch -= f7 * 1.2 + f8;             // ← 큰 swing 진폭 (-1.2 rad)
    rArm.yaw   += sin(sqrt(swing) * π) * 0.4;
    rArm.roll  -= sin(swing * π/2) * 0.4;
}
```

`SMModel.animateNonStandardWorking` (= SM phase + swing 시도 호출):
```java
bipedRightShoulder.rotateAngleZ = π/2;   // 어깨 90° 직립 정면
bipedRightShoulder.rotateAngleX = verticalAngle;
bipedRightShoulder.rotateAngleY = workingAngle;
bipedRightShoulder.ignoreSuperRotation = true;
bipedRightArm.reset();
```

- 1.21.1 vanilla biped 에 `bipedShoulder` 없음 → arm 자체에 매핑 필요.
- `ignoreSuperRotation = true` 등가 = 부모 X 회전 cancel (= preCancelParentX 식).

## 다음 작업 — 사용자 시각 인지 보고 받기

1. 각 phase 인게임 swing 시각 명시 보고 (= STAND/FLY/HJ/CR/SLD/CL 각각 정상/작음/큼/방향 부정확/etc).
2. 보고 받은 후 cause 식별:
   - swing 진폭 작음 → vanilla 식 자체 부족 (= 별도 식 추가 또는 swing duration 조정).
   - preCancelParentX 변형 부정확 → 부호/theta 식 변경.
   - lArm 정적 자세 → 비-preferred arm 도 vanilla pitch 보존.
3. 정확 fix 진행.

## dump 정리 — 미제거

`MixinPlayerEntityModelClient.sm_setAngles` 안 5 단계 dump (T1-T5) 미제거. 다음 검증 시 사용. 작업 완결 후 일괄 제거.

## 핵심 파일

- `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java`:
  - L262-L298: T1 dump.
  - L385-L410: reset 인프라 + fix #180 v2 가드.
  - L460-L482: T2 dump.
  - L486-L508: T3 dump.
  - L543-L566: T4 dump.
  - L591-L613: T5 dump.
  - L800-L843: sm_animateClimbing + fix #177.
  - L1310-L1355: sm_animateCrawling + fix #177.
  - L1453-L1517: sm_animateSliding + fix #177.
  - L1531-L1638: sm_animateFlying + fix #177.
  - L1639-L1707: sm_animateHeadJumping + fix #182.

## 회귀 차단

- fix #175 v5 (= sliding rapid toggle BUG) 관련 부분 건드리지 말 것.
- fix #176 (= 슬라이딩 모델 dz=-3/16) 보존.
- fix #180 v2 의 preserveSwing 가드 보존 (= STAND swing 정상화 식).
- fix #182 의 sm_animateHeadJumping preserve 식 보존.
