# Swing 애니메이션 작업 진행 상황 (마지막 업데이트: 2026-05-28)

SM phase 좌클릭 swing 애니메이션 매핑. fix #177~#190 시행착오 진행.

## fix #190 (= body sway SKIP) — 2026-05-28 마지막

사용자 보고 (fix #189 후): "SM 중 팔 휘두를 때 몸통이 흔들린다. 원래 안 흔들림".

**원본 fact (SmartMovingModel.animateWorkingBody L667-673)**:
```java
if(isStandard)                                  // ← STAND 시만 vanilla body sway
    imp.superAnimateWorkingBody(...);
else if(isWorking())
    animateNonStandardWorking(viewVerticalAngelOffset);  // ← SM + swing: 어깨 90° 직립만
```

대비, `animateWorkingArms` 가드는 `isStandard || isWorking()` (= SM + swing 시도 호출). **두 식이 다른 가드**.

**우리 fix #188 매핑 오류**: `body.yaw += sin(sqrt(swing)*2π)*0.2` 와 `leftArm.pitch += angle` 식을 SM phase 시도 적용. 원본은 isStandard 가드로 SM phase 시 SKIP.

**fix #190**: `sm_animateArmsOverride` 안 animateWorkingBody 식만 `!anySmState` 가드로 분기. animateWorkingArms (= preferredArm.pitch/yaw/roll) 는 그대로 적용.

## fix #189 (= base arm 진동 cancel) — 2026-05-28 후반

**ROOT CAUSE 확정 (= 사용자 verbatim + dump fact)**:

사용자: "기본 STANDING 보다 SM 휘두르는게 더 빠르다 + 여우무빙 팔 지직거림".

dump 검증 (fix #188 후):
- **STAND** tick 1 swing 0.167: pitch -0.65 → -1.55 (변화 0.90)
- **HJ**    tick 1 swing 0.167: pitch -0.53 → -1.90 (변화 **1.37** = STAND 의 1.52배!)

cause: HJ/SLD/fox 등 SM phase + 빠른 motion 시 `sm.stats.currentHorizontalSpeed` 큰 값 → `MixinLivingEntityRenderer.sm_modifyLimbSwingAmount` 가 vanilla setAngles 에 큰 limbDistance 전달 → vanilla `arm.pitch = cos(limbSwing*0.6662+π)*2*limbDistance*0.5` 식이 큰 amplitude 진동 → swing 식 (fix #188 cubic + sin*π) 결과와 누적 → 시각적 "더 빠른 + 지직".

원본 1.7.10 `SmartMovingModel.animateArmSwinging` (L640-647):
```java
if(isStandard)                              // ← SM phase 시 SKIP!
    if(isAngleJumping) animateAngleJumping();
    else imp.superAnimateArmSwinging(...);  // walking arm swing
```
즉 SM phase 시 walking arm swing 식 자체 SKIP → base arm.pitch=0 유지 → swing 식만 적용.

**fix #189**: `MixinLivingEntityRenderer.sm_modifyLimbSwingAmount` 안 anySmState 시 `0f` force. STAND 는 vanilla 유지 (= currentSpeed≈0 라 원래 영향 X).

## fix #188 (= 1차 시도, 부분 효과)

**거의 완결**. 사용자 verbatim (2026-05-28): "거의 다 온거 같음 원본과 매우 유사해졌어 모든 동작의 팔휘두르는게".

**잔존 issue**: 팔회전/전환 속도가 약간 빠른 느낌. **전반적으로 빠르며 여우무빙 시 더 두드러짐**.

**최신 fix #188 (= 진짜 원인 fix)**: vanilla 1.21.1 `BipedEntityModel.animateArms` 자체를 `@Inject HEAD @Cancellable` 로 차단 + 원본 1.7.10/1.12.2 SR.animateWorking* 식 1:1 복원.

### ROOT CAUSE (= 돌려막기 X, 원본 + 메모리 + dump + disassembly 4 자료 교차)

| 항목 | vanilla 1.21.1 (현재) | 원본 1.7.10/1.12.2 |
|------|------|------|
| ease curve | **quartic** `f = 1 - (1-swing)⁴` | **cubic** `f6 = 1 - (1-swing)³` |
| PEAK 위치 | swing≈0.16 (= 1 tick = 0.05초) | swing=1 (= 6 tick = 0.3초) |
| 어깨 sway (arm.pivot) | ±5 단위 sin 진동 (= 큰 motion) | 없음 |
| arm.yaw 일괄 += body.yaw | 두 arm 모두 변동 | 없음 (= preferred arm 만) |

**사용자 verbatim 매핑**:
- "전반적으로 빠르다" = vanilla quartic 식의 1-tick spike + 어깨 sway 큰 motion.
- "여우무빙 두드러진다" = HJ + fox 카메라 빠른 motion + vanilla fast spike 결합 시각.

**dump 검증** (log_temp.txt L1-L100): t=1248 swing=0.1667 안 sub-frame pitch peak -1.71. quartic peak 위치 swing≈0.16 와 정확 매치.

### 자료 4건 교차 확인 (fact only)

1. **원본 1.7.10**: `C:\Work\minecraft\porting\sm_original\SmartRender\src\main\java\net\smart\render\SmartRenderModel.java:298-315`. mp.onGround=swingProgress, Half=π, Whole=2π. ease cubic.
2. **원본 1.12.2**: `C:\Work\minecraft\porting\sm_porting_1_12_2\...\SRModel.java:401-416`. mp.swingProgress, SRUtilities.Half=π. 동일 식.
3. **vanilla 1.21.1 disassembly**: `BipedEntityModel.animateArms` bytecode 추출. `f *= f; f *= f` → quartic. arm.pivot ±5 sway. arm.yaw += body.yaw 일괄.
4. **메모리**: feedback_animateArms_cancel, project_flying_complete 등 fix #186 의 preserve 패턴.

## 정착된 fix (= 회귀 차단)

### fix #186 (커밋 908a5c6, 2026-05-28)
**fix #177 패러다임 완전 복원** (= 메모리 검증된 13단계 시행착오 정착 패턴).

fix #183/#184/#185 의 잘못된 폐기 → revert.

각 phase 복원:
- **CL** (sm_animateClimbing): preserve 가드. preCancelParentX 미적용 (부모 R_x 없음, 메모리 명시).
- **CR** (sm_animateCrawling): preserve + preCancelParentXPivot/Rotation (θ = π/2 - π/16 상수).
- **SLD** (sm_animateSliding): quat override + preserve + preCancelParentX (θ = π/2 상수).
- **FLY** (sm_animateFlying): preserve + preCancelParentX (θ = `lerpFadeAngle(...)` fade lerped). 비-preferred pivot default 복원.
- **HJ** (sm_animateHeadJumping): preserve + preCancelParentX (θ = QUARTER - angle raw).
- **Falling**: preserve. 부모 R_x 없음 → preCancelParentX 미적용.

helper `smApplyOriginalSwingDelta` 제거. EntityModel cast 제거.

### fix #187 (미커밋, 2026-05-28)
**HJ preCancelParentX θ → setupTransforms fade lerped 값**.

**cause**: setupTransforms HJ 분기 (`MixinPlayerEntityRenderer.java` L914) 가 `lerpFadeAngle(smHeadJumpTiltX_prev, thetaTarget, ...)` 사용 → fade 보간. preCancelParentX 는 raw target (`QUARTER - angle`) 사용 → **mismatch**.

**여우무빙** (wasSelfSlideFire=true) 시 더 큰 mismatch:
- setupTransforms thetaTarget = π/2 고정.
- preCancelParentX = π/2 - currentVerticalAngle (= 다른 식).

**fix**: `float thetaCancelHJ = sm.smHeadJumpTiltX_prev;` (= setupTransforms 가 이번 frame 갱신한 fade lerped 값). setupTransforms 가 setAngles 보다 먼저 호출되므로 매핑 정확.

**파일**: `MixinPlayerEntityModelClient.java:1696`.

## 잠재 추가 cause (CR/SLD 도 동일 패턴?)

**CR/SLD 의 preCancelParentX 식이 상수**:
- CR: `thetaCancelCrawl = π/2 - π/16` (상수).
- SLD: `thetaCancelSlide = π/2` (상수).

setupTransforms CR/SLD 분기가 fade lerp 사용 시 동일 mismatch 가능. fix #187 검증 결과 따라 동일 패턴 적용 검토.

FLY 는 이미 fade lerp 사용 — 매핑 정확.

## dump fact 매트릭스 (fix #186 후)

| Phase | T1 vanilla | T4 우리 | 매핑 |
|-------|------|------|------|
| STAND | rArm pitch=-0.45 | T4 == T1 (sm_animateXxx 미진입) | ✅ vanilla swing |
| FLY peak | pitch=-1.0 | preserve + preCancelParentX (fade lerped) | ✅ |
| HJ peak | pitch=-1.45 | pitch +2.97 (wraparound, -π 부근) | ⚠ swing 진행 시 pivotY 매 sub-frame 변동 (= 사용자 "빠른 느낌") |
| SLD peak | pitch=-0.33 | quat override + preserve | (?) 검증 필요 |
| CR peak | pitch=-0.40 | preserve + preCancelParentX | (?) 검증 필요 |
| CL peak | pitch=-0.43 | preserve (preCancelParentX 미적용) | ✅ |

## 검증 절차

1. 인게임 HJ + 여우무빙 시각 검증 (fix #187 후 "빠른 느낌" 해소 여부).
2. dump T1-T5 다시 받기.
3. 해소 시 CR/SLD 도 동일 cause 확인 (= 사용자 verbatim 잔존 시).
4. 정착 시 메모리 추가 + commit.

## 미제거 dump

`MixinPlayerEntityModelClient.sm_setAngles` 안 5 단계 dump (T1-T5). 작업 완결 후 일괄 제거.

## 사용자 verbatim 핵심 가이드

- **"원본과 매우 유사해졌어"** — 큰 방향 정착.
- **"오른팔만 움직이는게 맞아"** — preferred arm only (= 메모리 검증 패턴).
- **"제발 좀 문제해결메모리도 확인하라고 했는데"** — 메모리 우선 확인 의무.
- **"한 축 회전이 더 들어가야 빠짐"** = preCancelParentX 의 부모 R_x cancel 회전축 (= fix #186 복원).
- **"HJ + 여우무빙 빠른 느낌"** = fade lerp 누락 mismatch (= fix #187 후보).

## 핵심 파일

- `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java`:
  - L262-L298: T1 dump.
  - L391-L410: fix #180 v2 reset 인프라 (preserveSwing 가드).
  - L460-L482: T2 dump.
  - L486-L508: T3 dump.
  - L543-L566: T4 dump.
  - L591-L613: T5 dump.
  - L809-L820: sm_animateClimbing preserve 가드 (CL).
  - L1326-L1351: sm_animateCrawling preserve + preCancelParentX (CR).
  - L1465-L1510: sm_animateSliding preserve + preCancelParentX (SLD).
  - L1546-L1633: sm_animateFlying preserve + preCancelParentX (FLY).
  - L1648-L1710: sm_animateHeadJumping preserve + preCancelParentX (HJ, fix #187).
  - L1750-L1775: sm_animateFalling preserve (Falling).
  - L2137-L2160: preCancelParentXRotation / preCancelParentXPivot 헬퍼.

- `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityRenderer.java`:
  - L899-L922: setupTransforms HJ 분기 (smHeadJumpTiltX_prev fade lerp).

- `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java`:
  - L780-L781: smHeadJumpTiltX_prev / smHeadJumpFade_prevTime field.

## 회귀 차단 (함부로 수정 금지)

- fix #186 의 fix #177 패러다임 (= preserve 가드 + preCancelParentX).
- 메모리 `feedback_animateArms_cancel`, `project_flying_complete`, `project_sliding_animation_complete` 의 검증 패턴.
- fix #180 v2 의 reset 인프라 (= swing 시 일부 항목 skip).

## 관련 메모리

- `feedback_animateArms_cancel` — preserve + preCancelParentX 검증 패턴 (30일 전).
- `project_flying_complete` — 비행 swing arm 13단계 시행착오 정착 (24일 전).
- `project_sliding_animation_complete` — 슬라이딩 팔 회전 13단계 (23일 전).
- `feedback_zxy_zyx_rotation_order` — ZYX gimbal lock 회피.
- `feedback_render_scale_negation` — vanilla scale(-1,-1,1) 부호 반전.
- `feedback_rotation_pivot_pattern` — head pivot 기준 회전 + ±1.5 translate.
