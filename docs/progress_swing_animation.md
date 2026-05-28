# Swing 애니메이션 작업 진행 상황 (= 완결, 마지막 업데이트: 2026-05-29)

**완결 선언** — 사용자 인게임 검증 후 OK. 상세 메모리:
- [[project_swim_swing_complete]] — 수영 좌클릭 swing 시각 매핑 fix #186~#195.
- [[project_swim_sneak_jump_priority]] — 수영 sneak+jump 입력 우선순위 fix #201+#202.

## 갈무리 — 최종 fix 시리즈

| Fix | 영역 | 결과 |
|-----|------|------|
| #186 | fix #177 패러다임 복원 | preserve + preCancelParentX (메모리 검증 13단계). |
| #188 | animateArms override | @Inject HEAD @Cancellable + 원본 cubic + sin*π 식 1:1. |
| #189 | walking arm swing SKIP | anySmState 시 limbSwingAmount=0 force. |
| #190 | body sway SKIP | animateWorkingBody 식 anySmState 가드. |
| #191 | swim/dive preserve 가드 | preferred arm preserve + cubic 식 보존. |
| #192 | swim/dive preCancelParentX | thetaCancel = sm.smSwimDiveTiltX_prev. |
| #193 | swim head 직접 set | ZYX singular 회피. head.yaw=2*bodySway. |
| #194 | arm.pivot R_y(sway) 변환 유지 | 매 frame pivot 변환. 어깨 body 따라 이동. |
| #195 | swim preCancelParentXPivot SKIP | Y/Z swap 차단. world y 변동 복원. |
| #201 | sneak+jump 입력 우선순위 | swimDown/diveDown 식에 jump 가드. |
| #202 | motion -= 0.04 sneak 가드 | 1.7.10 부분 적용. swim+jump 단독 수면 도달 차단. |

## 보존 (= 회귀 차단)

본 작업 시점 fix 전부 정착. 다른 작업 시 위 영역 함부로 수정 금지.

## (역사적 기록) — fix #195 ROOT CAUSE 정리

사용자 verbatim (fix #194 후): "팔 휘두를 때 어깨가 그 위치에 고정되서 몸 움직임에 안 따라감".

**ROOT CAUSE** (= dump 정밀 + 매트릭스 합성 분석):

사용자 verbatim (fix #194 후): "팔 휘두를 때 어깨가 그 위치에 고정되서 몸 움직임에 안 따라감".

**ROOT CAUSE** (= dump 정밀 + 매트릭스 합성 분석):

vanilla 매트릭스 합성에서 ModelPart pivot 의 world 매핑:
- `pivot_world.x = -px, pivot_world.y = pz + 1.5, pivot_world.z = py - 0.001` (after `setupTransforms × scale × T(0, -1.501, 0)`).

원본 의도 어깨 world = `R_x(swimTilt) × R_y(sway) × (-5, 2, 0)` scale 후 = **(5cos, 5sin, 2)** = y 변동 (어깨 위/아래).

우리 fix #194 + fix #192 결과:
- pivot = (-5cos, **5sin**, **-2**) ← fix #192 의 preCancelParentXPivot 이 Y/Z swap
- world = (5cos, **-0.5 고정**, **5sin 변동**) ← y 고정, z 변동

**fix #195**: swim 분기 `preCancelParentXPivot` 호출 **SKIP**. `preCancelParentXRotation` 만 유지 (= swing rotation 자세 효과). fix #194 의 pivot (= -5cos, 2, 5sin) 그대로 → world = (5cos, **5sin + 1.5**, 2) ≈ 원본 (5cos, 5sin, 2) 미세 +1.5 y offset만.

## fix #194 (= swim swing 시 arm.pivot R_y(sway) 변환 유지) — 2026-05-28 후반

사용자 verbatim (fix #193 후): "팔 휘두를 때 어깨가 그 위치에 고정되서 몸 움직임에 안 따라감".

**ROOT CAUSE**: fix #191 의 preserve 가드가 arm.pivot 도 default (-5,2,0) / (5,2,0) 으로 reset → swing 시 어깨가 body sway 와 분리.

**원본 1.7.10 fact**: bipedRightArm 의 부모 = bipedBreast (R_y(sway)). swing 시 `animateNonStandardWorking` 는 `arm.reset()` 만 (= rotation 0 reset). 부모 breast.R(sway) 는 유지 → arm 어깨 vertex = R_y(sway) × shoulder.pivot 매 frame 변동.

**우리 매핑**: vanilla 1.21.1 에 breast 노드 없음. swim 의 arm.pivot R_y(sway) 변환 (fix #146) 가 breast 부모 효과 대체. **swing 시도 이 변환 유지** 해야 어깨가 body 따라 이동.

**fix #194**: preserve 가드의 if-else 분기 제거. arm.pivot 변환 매 frame 무조건 적용. rotation (pitch/yaw/roll) 만 preserve. preCancelParentXPivot 가 추가로 pivot Y/Z 변환 (R_x 부모 cancel).

## fix #193 (= swim head 직접 set, ZYX singular 회피) — 2026-05-28 후반

사용자 보고 (fix #192 후): "jump 꾹누름 + 수면 swim 시 swing 만 이상. 어깨 위/아래 토글".

**ROOT CAUSE 확정** (= 4 자료 + dump 강화 + agent 보고서):

`setAnglesRyRxRy_standard` 가 `R_y(swayBreast) × R_x(pitch) × R_y(swayHead)` 합성 후 ZYX 분해. yaw ≈ ±π/2 (= singular) 시 atan2 분기 변동으로:
- (pitch=π, yaw=π/2, roll=π) ↔ (pitch=0, yaw=π/2, roll=0) 토글.

같은 input 의도지만 vanilla `rotationZYX(roll, yaw, pitch)` 재합성 시 다른 quaternion → visual 토글.

**차이**:
- 일반 swim: walkFactor < 1 → bodySway 작음 → yaw 작음 → singular 미도달 → 정상.
- jump+swim: walkFactor=1 max → bodySway max → yaw ±π/2 도달 → singular 토글.

**dump 검증** (log_temp.txt 4회차):
- t=371 frame 10: head.pitch=**π**, head.yaw=1.55, head.roll=**π**
- t=371 frame 11: head.pitch=**0**, head.yaw=1.54, head.roll=**0**

**fix #193**: head 직접 set. `head.pitch = -EIGHTH * sSF; head.yaw = 2*bodySway; head.roll = 0;`. swimSSF=0 시 R_y(s)×R_y(s) commute = R_y(2s) 정확. swimSSF>0 시 sneak swim (walkFactor 작음, singular 미도달) → 미세 차이만.

## fix #192 (= swim/dive preCancelParentX) — 2026-05-28 마지막

사용자 보고 (fix #191 후): "수영 시 팔 휘두르는게 이상하다 (설명 어려움)".

**dump 분석 fact** (log_temp.txt 3회차):
- SW/DV phase, fix #191 preserve 작동 확인 (rArm T1=T4).
- lArm 은 swim/dive 자세 유지 (= 위로 뻗기, roll=-2.81/-2.36).
- **rArm 은 vanilla cubic swing 만 적용** = STAND swing 과 동일 visual.
- **양팔 비대칭 + rArm 이 SM 자세 안 됨** → 사용자 인지 "이상".

**ROOT CAUSE**: setupTransforms 의 swim/dive 분기에서 `R_x(67.5°~90°)` 부모 회전 적용 (= 누운 자세). 우리 fix #188 cubic 식은 직립 자세 가정 → 부모 R_x 안에서 적용 시 visual 어색.

**원본 1.7.10 흐름**: `animateNonStandardWorking` 의 `bipedRightShoulder.R(Z=π, Y=workAng, X=vert)` 가 부모 회전 보정. 그 다음 `arm.reset()` + cubic. 1.21.1 매핑 = preCancelParentX (= fix #186 패턴).

**fix #192**: sm_animateSwimming/Diving 끝에 preCancelParentX 추가. `thetaCancel = sm.smSwimDiveTiltX_prev` (= setupTransforms fade lerped). HJ/CR/SLD/FLY 와 동일.

## fix #191 (= swim/dive preserve 가드) — 2026-05-28 마지막

사용자 보고 (fix #190 후): "수영 시 팔 휘두르는게 안 고쳐졌다".

**원본 fact (SmartMovingModel.setRotationAngles L317-362 + L667-679)**:
- isSwim/isDive 분기 = arm 식 set.
- swing > 0 시 isWorking()=true → animateWorkingBody → animateNonStandardWorking → **`bipedRightArm.reset()`** (= arm 의 rotation/pivot/scale 모두 0 reset!).
- 그 다음 animateWorkingArms (isStandard || isWorking()) → vanilla cubic 식 적용.

**즉 원본 swing 시 swim/dive 자세 사라지고 cubic 식 결과만 남음**.

**우리 매핑 누락**: `sm_animateSwimming` / `sm_animateDiving` 가 vanilla animateArms (= fix #188 cubic) 후 호출 → arm 식 새로 set → cubic 결과 덮어씀 → swing visual 사라짐.

**fix #191**: preferred arm preserve 가드 추가 (= fix #186/#177 패러다임 차용).
- 시그너처: `sm_animateSwimming(sm, player, ...)` / `sm_animateDiving(sm, player, ...)`.
- swing > 0 시 preferred arm 의 setAnglesYXZ_breastSwim / arm.pitch / arm.roll / arm.pivot / arm.scale 식 SKIP.
- 비-preferred arm 은 swim/dive 자세 유지.

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
