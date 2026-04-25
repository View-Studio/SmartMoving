# Focus #1 — 애니메이션 망가짐 / 이상함

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #1 일 때 진입.
> 세션 1 (2026-04-25): 4 Agent 병렬 전수 리서치 + 통합 + 12 분기 + 1 default 라인별 1:1 검증 표 보강.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | 🟡 진행 중 — 1차 선행 보강 (B-1/B-2/B-3) 완결, **라인별 전수 1:1 대응 작업 진입 (Phase R / 세션 3+)** |
| 현재 단계 | Phase R — 애니메이션 관련 원본 모든 파일 라인별 read + 라인별 1.21.1 매핑 보강 |
| 선행 의존 | #2 (애니메이션 입력 상태가 정확해야 의미 있음) — 완료 |
| 자기 정정 | 세션 2 "AI 완결" 마킹은 **부적절** (3 누락만 메웠지 전수 1:1 감사 안 함) → 되돌림 |

---

## 2. 증상 — 원본 vs 현재

특정 상태의 애니메이션이 원본과 **시각적으로 다름**:
- 팔/다리/몸통 각도가 어긋남
- 회전축 방향 반대
- 애니메이션 정지 / 떨림
- 좌우 비대칭 오류

상태별 구분 필요 (수영/잠수/사다리/넝쿨/크롤/슬라이딩/헤드점프/비행/천장/벽점프/로프).

---

## 3. 재현 케이스 표 ⚠️ **진입 전 필수 수집**

| # | 상태 | 상황 | 원본 모습 (라인 + 수식 근거) | 1.21.1 실제 | 차이 |
|---|------|------|---------------------------|-----------|------|
| 1 | isCrawling | 전진 | 몸통 `Quarter - Thirtytwoth ≈ 1.374 rad` (≈79°), 팔 교차 `cos(dist*0.6662) * Sixteenth + Half + Eighth`, YZX 회전 | TBD (통합테스트) | TBD |
| 2 | isHeadJumping | 공중 이동 | bend `(Quarter - currentVerticalAngle)/2` 팔/다리, head pitch `-(Quarter-angle)/2`, body @TAIL `Quarter - currentVerticalAngle` | TBD | TBD |
| 3 | isFeetVineClimbing | 넝쿨 오르기 | pitch=`-(cos(totalDistance+Half)+1) * Thirtytwoth - Sixteenth` 덮어쓰기, roll `+=` 누적 `±max(0, cos(totalDistance-Quarter)) * Sixtyfourth` | 이식 완료 (B-vine) | (검증 대기) |
| 4 | isHandsVineClimbing | 넝쿨 매달림 | armY `*= 1.6662` + `±Eighth` + setArmScales(`abs(cos(armX))`) | 이식 완료 | (검증 대기) |
| 5 | isRopeSliding | 로프 매달림 | torso `Sixteenth + Sixtyfourth*cos(time)` (time=`totalTime*0.15`), 팔 `Half - torsoX`, 머리 Z 클램프 `[-Sixteenth, Sixteenth]` | TBD | TBD |
| 6 | isClimb | 사다리 오르기 (NoGrab+NoStep ≠) | NoGrab+!NoStep 분기에서 torso=`0.5F`, head -=`0.5F`, pelvic -=`0.5F`, torsoZ=`-6F` | TBD | TBD |
| 7 | isCrawlClimb | 가장자리 오르기 (height) | acos 식 3-way (`bodyLength=0.7F`, `legLength=0.55F`), smallOverGroundHeight 입력 | TBD | TBD |
| 8 | isCeilingClimb | 천장 매달리기 | walk/stand factor + cos(distance) `0.44F * factor` Y 회전, threshold `0.015` | TBD | TBD |
| 9 | isSwim_sm | 수평 수영 | swim/sneak factor, 팔 원형 운동 (head=YXZ, arm=YZX), threshold `0.005`(slow)/`0.015` | TBD | TBD |
| 10 | isDive | 잠수 | 3-way armZ (isLevitate `Quarter-Sixteenth` / isJump `0` / general `Quarter-currentVerticalAngle`) | 이식 완료 | (검증 대기) |
| 11 | isSlide | 미끄러짐 | body=YXZ `cos*Sixtyfourth*walkFactor`, arm=YZX `±Sixteenth ±Quarter`, @TAIL Quarter + translate(0,5/16,0) | TBD | TBD |
| 12 | isFlying (creative) | 창작 비행 | arm=XZY `Half-Sixteenth + cos`, head pitch=`-theta/2` 보정 (ANIM-01), threshold `0.05` | TBD | TBD |
| 13 | isClimbJump | 벽 점프 | 팔 X=`Half + Sixteenth`, Z=`±Thirtytwoth` (단순 고정각) | TBD | TBD |
| 14 | isFalling | 자유낙하 | arm=XZY `cos(dist+Quarter)*Eighth + Quarter` Y/Z, fallDistance `> 1.5F` 판정 | TBD | TBD |
| 15 | isAngleJumping | 공중 회전점프 | leg=ZXY `Z=Thirtytwoth*backness, X=Thirtytwoth*(1+left/right), Y=-angle` | TBD | TBD |
| 16 | isStandard (default) | 보행/정지 | vanilla `super.setAngles` 그대로 (BipedEntityModel 13단계) | 정상 (vanilla 위임) | — |

**원본 모습 근거**:
- 라인 번호 = `docs/research/original/smartmoving/render/SmartMovingModel.md` 기준
- 모든 수식 = SmartRenderContext 상속 라디안 상수 사용 (§5.4 표)

---

## 4. 의존 관계

| 관계 | 대상 |
|------|------|
| 선행 의존 | **#2** (렌더 입력 상태) — 완료 |
| 영향받는 후속 | 없음 |
| 영향 주는 완료 이식 | `MixinPlayerEntityModelClient` 전체 (sm_setAngles + 11 헬퍼) + `MixinPlayerEntityRenderer` (sm_captureBodyYaw / setupTransforms HEAD/ModifyArg/TAIL / getPositionOffset / renderName) + `MixinLivingEntityRenderer` (hasLabel @Redirect) |

---

## 5. 원본 근거

### 5.1. 리서치 파일 인덱스 (전수 라인별 검증 — 세션 1)

| 파일 | 줄수 | 역할 | 비고 |
|---|---|---|---|
| `docs/research/original/smartmoving/render/SmartMovingModel.md` | 1153 | SM 11+1 분기 (setRotationAngles) + animateAngleJumping/animateNonStandardWorking/animateNonStandardBowAiming + setArmScales/setLegScales | A-15 / R-05 검증 완료 |
| `docs/research/original/smartmoving/render/SmartMovingRender.md` | 636 | renderPlayer / renderPlayerAt / renderName / rotatePlayer / renderGuiIngame | bodyYaw + heightOffset 포팅 근거 |
| `docs/research/original/smartrender/SmartRenderModel.md` | (대형) | 15개 ModelRotationRenderer 노드 계층 (bipedOuter/Torso/Breast/Neck/Pelvic/Shoulder + cloak/ears/headwear) | 중간 노드 부재 근거 |
| `docs/research/original/smartrender/ModelRotationRenderer.md` | 528 | 6 회전순서 / scaleX/Y/Z / offsetX/Y/Z / ignoreSuperRotation / fade 보간 | C-08 검증 완료 |
| `docs/research/original/smartrender/SmartRenderRender.md` | (대형) | renderPlayer 파이프라인 / rotatePlayer / SmartStatistics 보간 | bodyYaw 진입점 |
| `docs/research/original/smartrender/SmartRenderContext.md` | (대형) | 각도 상수 + 스케일 상수 (Half/Quarter/Eighth/Sixteenth/Thirtytwoth/Sixtyfourth/Whole/RadiantToAngle) | §5.4 |
| `docs/research/original/smartrender/SmartRenderModelPlayerBase.md` | (대형) | 30+ 훅 메서드 (animateHeadRotation/animateArmSwinging/animateRiding/...) | Agent C |
| `docs/research/original/smartrender/SmartRenderRenderPlayerBase.md` | (대형) | super 호출 우회 (superRenderPlayer) | Agent C |
| `docs/research/original/smartrender/IModelPlayer.md` / `IRenderPlayer.md` | (대형) | 인터페이스 — playerapi 매핑 | Agent C |
| `docs/research/mapping/animation_system.md` | 1111 | 본 매핑 분석 (vanilla 4 메서드 + SR 노드 + setAngles + bodyYaw + setupTransforms + ModelPart + R-10/R-13 회전순서) | 1:1 표 누적 |

### 5.2. 분기별 SmartMovingModel.md 라인 인덱스 (재현 케이스 → 원본)

| 분기 | SmartMovingModel.md L | 1.21.1 헬퍼 (MixinPlayerEntityModelClient) | 핵심 수식 |
|------|----|----|----|
| 1. isRopeSliding | L269-L302 | `sm_animateRopeSliding()` L151 | torso = Sixteenth + Sixtyfourth*cos(totalTime*0.15F); arm = Half - torso |
| 2. isClimb / isCrawlClimb | L305-L444 | `sm_animateClimbing()` L190 | hands/feet 3-way (handsClimbType/feetClimbType) + vine 보정 + acos 3-way crawlClimb |
| 3. isClimbJump | L446-L454 | sm_setAngles L105-L108 (직접 할당) | 팔 X=Half+Sixteenth, Z=±Thirtytwoth |
| 4. isCeilingClimb | L457-L481 | `sm_animateCeilingClimbing()` L307 | walk/stand factor; cos(distance) 0.44F |
| 5. isSwim | L485-L545 | `sm_animateSwimming()` L331 | swimStandSneakFactor; arm 원형 (YZX) |
| 6. isDive | L547-L590 | `sm_animateDiving()` L367 | 3-way armZ; 다리 발차기 |
| 7. isCrawl | L593-L651 | `sm_animateCrawling()` L389 | torso = Quarter - Thirtytwoth (≈79°), arm 수평 (YZX) |
| 8. isSlide | L653-L694 | `sm_animateSliding()` L426 | body 진동 (YXZ); arm 고정각 (YZX) |
| 9. isFlying | L696-L733 | `sm_animateFlying()` L461 | head pitch = -theta/2 (ANIM-01); arm XZY |
| 10. isHeadJump | L735-L766 | `sm_animateHeadJumping()` L496 | bendFactor (currentVerticalAngle); armFactorZ (천장 고도 클램프) |
| 11. isFalling | L768-L792 | `sm_animateFalling()` L531 | arm Y/Z (XZY) `cos(dist+Quarter)*Eighth + Quarter` |
| 12. else (isStandard) | L794-L797 | (vanilla super 위임) | BipedEntityModel.setAngles 13단계 |
| 보조: animateAngleJumping | L814-L848 | `animateAngleJumping()` L555 | leg ZXY (직접 setAnglesZXY 헬퍼) |

### 5.3. SmartRenderModel 15 노드 계층 (생성자 직접 확인 — A-15)

```
bipedOuter (0,0,0, root, fadeEnabled=true)
└── bipedTorso (0,0,0)
    ├── bipedBody (0,0,0)
    ├── bipedBreast (0,0,0)
    │   ├── bipedNeck (0,0,0)
    │   │   └── bipedHead (0,0,0)
    │   │       ├── bipedEars (0,0,0)
    │   │       └── bipedHeadwear (0,0,0)
    │   ├── bipedCloak (0,0,2F)            ← Z=2 (등 쪽)
    │   ├── bipedRightShoulder (-5F,2F,0)
    │   │   └── bipedRightArm (0,0,0)
    │   └── bipedLeftShoulder (5F,2F,0, mirror=true)
    │       └── bipedLeftArm (0,0,0)
    └── bipedPelvic (0,12F,0)              ← Y=12 (허리 아래)
        ├── bipedRightLeg (-2F,0,0)
        └── bipedLeftLeg (2F,0,0)
```

**1.21.1 PlayerEntityModel** (대응 노드만 존재): head/hat/body/rightArm/leftArm/rightLeg/leftLeg + cloak/ear/sleeve/pants/jacket. **중간 노드 7개 부재**: bipedOuter/Torso/Breast/Neck/Pelvic/RightShoulder/LeftShoulder.

### 5.4. SmartRenderContext 각도 상수 (라디안)

| 상수 | 값 | 1.21.1 매핑 |
|---|---|---|
| `Half` | π ≈ 3.14159 | `MathHelper.PI` 또는 `(float)Math.PI` |
| `Quarter` | π/2 ≈ 1.57080 | `(float)Math.PI / 2F` |
| `Eighth` | π/4 ≈ 0.78540 | |
| `Sixteenth` | π/8 ≈ 0.39270 | |
| `Thirtytwoth` | π/16 ≈ 0.19635 | |
| `Sixtyfourth` | π/32 ≈ 0.09817 | |
| `Whole` | 2π ≈ 6.28319 | |
| `RadiantToAngle` | 180/π ≈ 57.29578 | `MathHelper.DEGREES_PER_RADIAN` 또는 `180F/(float)Math.PI` |
| `FrequenceFactor` | 0.6662F | (vanilla limbSwing 주기 계수와 동일) |

**주요 매직넘버 (수식 내 raw value)**:
- `0.5F` — verticalSpeed/horizontalSpeed clamp 상한 (isClimb)
- `0.15F` — isRopeSliding 시간 계수 (`totalTime * 0.15F`)
- `0.0625F` (= 1/16) / `0.125F` (= 1/8) — 미세 오프셋
- `0.4F` / `0.44F` / `0.55F` / `0.7F` / `0.85F` — height/length 계수 (isCrawlClimb / isCeilingClimb)
- `1.537F` — (acos legLength 정규화 분모, isCrawlClimb)
- `2F` — verticalSpeed 곱 (isClimb 팔 X)
- `1.4F` — limbSwing 다리 X 계수 (vanilla animateArmSwinging)

### 5.5. 분기별 회전 순서 표 (C-08-1 + Agent A 검증)

| SM 분기 | 노드 | rotationOrder | call 순서 (post-multiply 역순) | 1.21.1 헬퍼 |
|---|---|---|---|---|
| isClimb / isCrawlClimb | leg | YZX | X, Z, Y | `setAnglesYZX()` L607 |
| isSwim | head | YXZ | Z, X, Y | (수동 MatrixStack — 미이식) |
| isSwim | arm | YZX | X, Z, Y | `setAnglesYZX()` |
| isCrawl | torso | YZX | X, Z, Y | (구조 부재 — body 단일 근사) |
| isCrawl | arm | YZX | X, Z, Y | `setAnglesYZX()` |
| isSlide | body | YXZ | Z, X, Y | (미이식 — 일반 XYZ) |
| isSlide | arm | YZX | X, Z, Y | `setAnglesYZX()` |
| isFlying | arm | XZY | Y, Z, X | (미이식 — 일반 XYZ) |
| isFalling | arm | XZY | Y, Z, X | (미이식 — 일반 XYZ) |
| animateAngleJumping | leg | ZXY | Y, X, Z | `setAnglesZXY()` L622 |
| animateNonStandardWorking | rightShoulder | ZYX | X, Y, Z | (구조 부재 — N/A) |
| animateNonStandardBowAiming | both shoulders | ZYX | X, Y, Z | (구조 부재 — N/A) |

**검증 (R-17, ModelRotationRenderer.rotate() GitHub 직접)**: post-multiply 규칙으로 GL call 순서가 vertex 적용 순서의 역순임을 확인. JOML Quaternionf 결합 + getEulerAnglesZYX 역분해로 등가 변환됨 (Agent D 검증).

### 5.6. 1.21.1 SM Mixin inject 전수 표 (Agent D)

| Mixin 클래스 | 메서드 | 위치 | 역할 |
|---|---|---|---|
| MixinPlayerEntityModelClient | sm_setAngles | @Inject(TAIL) | 11 상태 if-else + isAngleJumping (Line 70-138) |
| MixinPlayerEntityRenderer | getPositionOffset | @Inject(HEAD, cancellable) | isHeadJumping heightOffset / isCrawling -scale*0.125 |
| MixinPlayerEntityRenderer | sm_captureBodyYaw | @Inject(setupTransforms HEAD) | smBodyYawActive/Override 8가지 SM 상태 |
| MixinPlayerEntityRenderer | (setupTransforms super 호출 인자) | @ModifyArg(index=3) | bodyYaw 교체 |
| MixinPlayerEntityRenderer | sm_setupTransforms | @Inject(setupTransforms TAIL) | 5 상태 X 기울기 (isSwim/isDive/isSlide/isFlying/isHeadJump) |
| MixinPlayerEntityRenderer | renderName | @Inject(HEAD, cancellable) | isCrawling 이름표 차단 / heightOffset Y / sneak Y |
| MixinLivingEntityRenderer | hasLabel | @Redirect(isSneaky) | sneakNameTag 64블록 거리 제어 |

---

## 6. 1:1 매핑 테이블

| 원본 | 1.21.1 | 비고 |
|---|---|---|
| `bipedHead` / `bipedBody` (ModelBiped) | `head` / `body` (PlayerEntityModel) | ✓ |
| `bipedLeftArm` / `bipedRightArm` | `leftArm` / `rightArm` | ✓ |
| `bipedLeftLeg` / `bipedRightLeg` | `leftLeg` / `rightLeg` | ✓ |
| `bipedOuter` (root) | **대응 없음** | `setupTransforms @ModifyArg(index=3)` + `sm_captureBodyYaw`로 단일 경로 근사 |
| `bipedTorso` | **대응 없음** | body + 모든 상체(head/arm) 개별 조작 |
| `bipedPelvic` | **대응 없음** | body 단일 노드로 근사 |
| `bipedBreast` / `bipedNeck` / `bipedShoulder` (R/L) | **대응 없음** | SR 전용 — N/A |
| `rotateAngleX/Y/Z` | `pitch`/`yaw`/`roll` | ✓ |
| `rotationPointX/Y/Z` | `pivotX/Y/Z` | ✓ |
| `rotationOrder = YZX` | `setAnglesYZX()` 헬퍼 | ✓ Quaternionf qY*qZ*qX → getEulerAnglesZYX |
| `rotationOrder = ZXY` | `setAnglesZXY()` 헬퍼 | ✓ Quaternionf qZ*qX*qY → getEulerAnglesZYX |
| `rotationOrder = YXZ/XZY/ZYX` | **헬퍼 부재** | ⚠️ 일반 XYZ 근사 (isSwim head, isSlide body, isFlying/isFalling arm 영향) |
| `setArmScales(rx, ry, rz, lx, ly, lz)` | (구현 안됨) | ⚠️ ModelPart `xScale/yScale/zScale` 가능 (B-08 확인) — 미이식 |
| `setLegScales(rx, ry, rz, lx, ly, lz)` | (구현 안됨) | ⚠️ 동일 |
| `bipedOuter.fadeRotateAngleY` (보간) | entity.bodyYaw / prevBodyYaw lerpAngleDegrees | 계수 `0.2F * timeDelta` 차이 (vanilla는 tickDelta) |
| `mp.onGround` (도구 사용 진행도) | 미확인 — entity.isUsingItem() / getActiveHand() / ArmPose | C-09 잔존 |
| `actualRotation = 0` (vanilla 회전 차단) | setupTransforms @ModifyArg index=3 직접 교체 | ✓ |
| `entity.setSneaking()` 임시 변경 (이름표) | hasLabel @Redirect | ✓ |

---

## 7. 구조적 차이 / 근사 이식 지점

### 7.1. 재현 불가 (구조적)

- **bipedOuter 계층 부재** → 몸 전체 회전 ≠ 머리/팔/다리 회전 분리 불가. (근사: setupTransforms TAIL matrices.multiply 전역 X 기울기 + @ModifyArg bodyYaw 교체)
- **SR 전용 노드 7개** (Outer/Torso/Breast/Neck/Pelvic/RightShoulder/LeftShoulder) 부재 → 그룹 회전 불가. 자식 파트 일일이 열거 필요.
- **ModelRotationRenderer displayList + 6 회전 순서** → MatrixStack `pitch(X)→yaw(Y)→roll(Z)` 고정. YZX/ZXY 2개만 헬퍼 구현, 4개 (YXZ/XZY/ZYX 누락) — 영향: isSwim head / isSlide body / isFlying·isFalling arm.
- **scaleY 런타임 변경** → ModelPart xScale/yScale/zScale 가능하지만 setAngles @Inject(TAIL)에서 설정해도 setTransform 미호출이므로 안전 (M-10 확인). 미이식.
- **animateNonStandardWorking / animateNonStandardBowAiming** (어깨 ignoreSuperRotation) → 구조 부재로 N/A. (도구 사용 활/석궁 SM 비표준 상태에서 이슈 가능)
- **fade 보간 0.2F * timeDelta 계수** → vanilla lerpAngleDegrees(tickDelta) 사용으로 미세 차이.

### 7.2. 근사 이식 (이미 적용)

- body yaw override → `sm_captureBodyYaw` HEAD에서 8 상태 분기 + `@ModifyArg(index=3)` 교체.
- pitch/yaw/roll YZX → `setAnglesYZX()` 헬퍼 (Quaternionf 결합 + Euler ZYX 역분해).
- pitch/yaw/roll ZXY → `setAnglesZXY()` 헬퍼 (animateAngleJumping leg).
- leaningPitch = 0 강제 (Line 88) → vanilla setupTransforms Branch 2 (-90°) 차단.
- isHeadJumping head pitch / isFlying head pitch = `-(Quarter-angle)/2` (ANIM-01 — setupTransforms 전역 X 회전을 head local에서 역보정).

### 7.3. 완전 재현 가능

- 개별 팔/다리 각도 공식 (cos/sin/상수) — 원본과 **동일 수식** 적용.
- sin/cos 분해 근사는 **금지** (메모리 기록된 실패 패턴).
- isFeetVineClimbing pitch=`-total` 덮어쓰기 + roll `+=` 누적 — 이식 완료.
- isHandsVineClimbing armY `*= 1.6662` + `±Eighth` — 이식 완료.

---

## 8. 현재 구현 스냅샷

`MixinPlayerEntityModelClient.java` (649줄) — Agent C 검증:
- L48-L55: 각도 상수 (HALF/QUARTER/EIGHTH/SIXTEENTH/THIRTYTWOTH/SIXTYFOURTH/WHOLE/DEG_TO_RAD)
- L70-L138: sm_setAngles TAIL inject (11 상태 체인 + isAngleJumping)
- L151-L188: sm_animateRopeSliding (XYZ)
- L190-L305: sm_animateClimbing (YZX, hands/feet 3-way + vine + crawlClimb)
- L307-L329: sm_animateCeilingClimbing (XYZ, walk/stand)
- L331-L365: sm_animateSwimming (YZX arm; head는 XYZ 근사 — YXZ 헬퍼 부재)
- L367-L387: sm_animateDiving (XYZ)
- L389-L424: sm_animateCrawling (YZX arm; torso는 body 단일 근사)
- L426-L459: sm_animateSliding (YZX arm; body는 XYZ 근사 — YXZ 헬퍼 부재)
- L461-L494: sm_animateFlying (XYZ — XZY 헬퍼 부재 / ANIM-01 head 역보정)
- L496-L529: sm_animateHeadJumping (XYZ + bendFactor)
- L531-L553: sm_animateFalling (XYZ — XZY 헬퍼 부재)
- L555-L570: animateAngleJumping (ZXY)
- L586-L601: computeSmallOverGroundHeight (5블록 column 스캔)
- L607-L616: setAnglesYZX
- L622-L631: setAnglesZXY
- L637-L647: smFactor (양방향 보간)

`MixinPlayerEntityRenderer.java` (275줄) — Agent C 검증:
- L46-L63: getPositionOffset HEAD (isHeadJumping heightOffset / isCrawling -scale*0.125)
- L74-L156: setupTransforms HEAD (sm_captureBodyYaw 8 분기)
- L164-L172: @ModifyArg index=3 (bodyYaw 교체)
- L183-L237: setupTransforms TAIL (5 상태 X 기울기 + isSliding translate)
- L249-L274: renderName HEAD (isCrawling 차단 / heightOffset / sneak)

---

## 9. 예상 수정 diff

세션 1 리서치만 완료. 통합테스트(#1~#6)에서 시각 재현 케이스 수집 후 케이스별 작성.

**잠재 위험 영역 (회전순서 헬퍼 부재)**:
1. `isSwim head.pitch` (YXZ) — Z,X,Y 순서가 일반 XYZ로 근사되어 머리 기울기가 미세하게 다를 수 있음.
2. `isSlide body` (YXZ) — body는 vanilla setAngles에서도 사용되어 setupTransforms TAIL X 기울기와 합쳐지면 Vis 차이.
3. `isFlying / isFalling arm` (XZY) — `Z=cos(dist+Quarter)*Eighth + Quarter` 수식이 일반 XYZ로 적용되면 팔 흔들림 위상 다름.

**잠재 위험 영역 (구조 부재)**:
1. `isCrawl torso=Quarter-Thirtytwoth` (≈79° 상체 기울기) — body 단일 노드 근사. body 자식 노드(head/arm) 별도 보정 필요.
2. `setArmScales(abs(cos(armX)), abs(cos(armX)))` (isHandsVineClimbing) — ModelPart xScale/yScale 미사용.

---

## 10. 원자 단위 작업 목록

### P. 재현 케이스 수집 (통합테스트 단계)
- [ ] P-1. 각 상태별 in-game 시각 재현 + §3 표 채우기 (총 16 케이스)
- [ ] P-2. 원본 모습은 1.7.10 게임플레이 영상 참조 또는 §5.4-5.5 수식 재현

### A. 원인 분석 (시각 차이 발견 시)
- [ ] A-N. (케이스별 — 원본 공식 vs 현재 구현 side-by-side, 회전순서/구조부재 위험 표 §9 우선 점검)

### B. 1차 선행 1:1 보강 — 세션 2 완결 (식별된 누락 3 메움)
- [x] B-3. ModelPart yScale 활용 setArmScales/setLegScales 이식 (세션 2) — climbing arm vine + leg vine + swimming + diving + crawling 5 호출 지점 1:1
- [x] B-1. YXZ 헬퍼 추가 (`setAnglesYXZ()`) + 호출 교체 (세션 2) — isSwim head + isSlide body
- [x] B-2. XZY 헬퍼 추가 (`setAnglesXZY()`) + 호출 교체 (세션 2) — isFlying arm + isFalling arm

### R. 라인별 전수 1:1 대응 작업 (세션 3+) ★ 본격 1:1 대응 진입점
사용자 지시 (2026-04-26): "에니메이션과 관련된 모든 코드를 싹 다 가져와서 그걸 전부 1대1 대응 ... 모든 라인을 라인별로 청크 분리해서 읽고 ... 리서치 + 포커스 보강 후에 작업 진행".

대상 31 파일 ~5,000줄. 청크 분할 8-10 세션:

- [x] R-1. SmartMovingModel.java 라인별 read (4 청크 × ~200줄) + 1.21.1 매핑 (797줄) — **세션 6 완료**
  - [x] R-1 청크 1 (L1-L200) — 세션 3 (정합 84 / 오역 3 / 누락 2 / 잉여 0 / N/A 111)
  - [x] R-1 청크 2 (L201-L400) — 세션 4 (정합 122 / 오역 8 / 누락 5 / 잉여 0 / N/A 65)
  - [x] R-1 청크 3 (L401-L600) — 세션 5 (정합 95 / 오역 3 / 누락 7 / 잉여 0 / N/A 95)
  - [x] R-1 청크 4 (L601-L797) — 세션 6 (정합 45 / 오역 0 / 누락 0 / 잉여 0 / N/A 152)
  - **R-1 누적**: 정합 346 / 오역 14 / 누락 14 / 잉여 0 / N/A 423 = 797 라인 전수
- [x] R-2. SmartMovingRender.java + SM render Context/IModel/IRender/ModelPlayer/RenderPlayer (~726줄) — **세션 9 완료**
  - [x] R-2 청크 1 (SmartMovingRender.java L1-L200) — 세션 7 (정합 51 / 오역 0 / 누락 3 / 잉여 0 / N/A 146)
  - [x] R-2 청크 2 (SmartMovingRender.java L201-L337 + Context/IModel/IRender 4 파일) — 세션 8 (정합 4 / 오역 0 / 누락 0 / 잉여 0 / N/A 238)
  - [x] R-2 청크 3 (ModelPlayer.java + RenderPlayer.java) — 세션 9 (정합 5 / 오역 0 / 누락 0 / 잉여 0 / N/A 284)
  - **R-2 누적**: 정합 60 / 오역 0 / 누락 3 / 잉여 0 / N/A 668 = 731 라인 전수
- [x] R-3. SM render/playerapi 3 파일 (SmartMoving + ModelPlayerBase + RenderPlayerBase, ~373줄) — **세션 10 완료**
  - 합계: 정합 14 / 오역 0 / 누락 0 / 잉여 0 / N/A 362 = 376 라인 전수
- [x] R-4. SmartRenderModel.java 라인별 (3 청크, 469줄) — **세션 13 완료**
  - [x] R-4 청크 1 (L1-L160) — 세션 11 (정합 15 / 오역 0 / 누락 0 / 잉여 0 / N/A 145)
  - [x] R-4 청크 2 (L161-L320) — 세션 12 (정합 30 / 오역 0 / 누락 1 / 잉여 0 / N/A 129)
  - [x] R-4 청크 3 (L321-L469) — 세션 13 (정합 17 / 오역 0 / 누락 0 / 잉여 0 / N/A 132)
  - **R-4 누적**: 정합 62 / 오역 0 / 누락 1 / 잉여 0 / N/A 406 = 469 라인 전수
- [ ] R-5. ModelRotationRenderer.java 라인별 (2 청크, 368줄) + RendererData/Cape/Ears/Special (~233줄)
- [ ] R-6. SmartRenderRender.java + SmartRenderUtilities + Mod/Info/Install/Context/IModel/IRender (~564줄)
- [ ] R-7. SR ModelPlayer/RenderPlayer + SR playerapi 3 파일 (~788줄)
- [ ] R-8. SmartStatistics 일체 (7 파일 ~620줄)
- [ ] R-9. 통합 라인별 매핑 표 (animation_system.md 보강 또는 신규 research_animation_line_by_line.md) + focus_01 §5/§10 대폭 보강
- [ ] R-10+. 발견된 [오역]/[누락]/[잉여] B-N 원자로 등록 + 본격 1:1 대응 진입

### B. 본격 1:1 대응 (R-10 이후)
- [ ] B-N. (라인별 발견 시 atom 단위로 추가)

### C. 검증 (세션 2 부분 완결, 전수 감사 후 재검증 필요)
- [x] C-1. 빌드 — `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (세션 2 한정)
- [x] C-2. 재현 케이스 grep 정합 — setArmScales 4 / setLegScales 4 / setAnglesYXZ 2 / setAnglesXZY 2 호출 (세션 2 한정)

### C. 검증 (세션 2 완결)
- [x] C-1. 빌드 — `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL
- [x] C-2. 재현 케이스 grep 정합 — setArmScales 4 / setLegScales 4 / setAnglesYXZ 2 / setAnglesXZY 2 호출
- [x] C-3. 회귀 방지 감사 (§14)
- [x] C-4. `playtest_fixes.md` "현재 포커스" → (#1 AI 완결 / 통합테스트 인계) (세션 2)

---

## 11. 호출 타이밍 검증

| 훅 | 원본 | 1.21.1 |
|---|------|--------|
| setRotationAngles / setAngles | 매 렌더 프레임 (render → setAngles) | MixinPlayerEntityModelClient.setAngles TAIL inject |
| bodyYaw 계산 | rotatePlayer() (renderYawOffset 직접 설정) | MixinPlayerEntityRenderer.sm_captureBodyYaw HEAD + @ModifyArg(index=3) |
| X 기울기 (setupTransforms) | renderModel() matrix transform (bipedOuter.X) | MixinPlayerEntityRenderer.sm_setupTransforms TAIL (matrices.multiply POSITIVE_X) |
| heightOffset | renderPlayerAt d1 += heightOffset | MixinPlayerEntityRenderer.getPositionOffset HEAD return Vec3d |
| renderName | SmartMovingRender.renderName 직접 | MixinPlayerEntityRenderer.renderName HEAD (cancellable) |
| leaningPitch (vanilla 충돌) | (해당 없음 — vanilla 1.7.10에 leaningPitch 미존재) | sm_setAngles TAIL Line 88: `model.leaningPitch = 0F` 강제 → setupTransforms Branch 2 차단 |
| limbSwing 입력 | totalHorizontalDistance / currentHorizontalSpeed (SmartStatistics) | entity.limbAnimator.getPos/getSpeed(tickDelta) |
| animationProgress 입력 | totalTime (SmartStatistics) | entity.age + tickDelta (LivingEntityRenderer.render Step 8) |

일치: ✓ (Agent C 검증 — 95% bodyYaw 무결성 / 100% X tilt 무결성)

---

## 12. 테스트 프로토콜

```
[T-N] 상태별 애니메이션 육안 검증
  1. F3 + B 로 hitbox 표시 (정확한 위치 확인)
  2. 3인칭 시점으로 자신 관찰
  3. 원본 1.7.10 동일 상태와 비교 (레퍼런스 영상/스샷)
  4. 특히 다음 확인:
     - 팔 스윙 방향 (전진 vs 후진)
     - 다리 교차 패턴 (좌우 번갈아)
     - 몸통 pitch (앞뒤 기울기)
     - 몸통 yaw (좌우 회전)
     - 애니메이션 속도 (속도에 따른 스윙 주기)
     - 회전 순서 위험 케이스 (§9): isSwim head 기울기 / isSlide body / isFlying·isFalling arm 위상
     - 스케일 위험 케이스: isHandsVineClimbing 팔 스케일 변동 (벽 밀착감)
```

---

## 13. 완료 전 검증 체크리스트

- [ ] §3 표의 모든 케이스 시각 매칭 (근사 한도 내)
- [ ] 원본 공식이 sin/cos 분해 근사가 아닌 원본 그대로 사용
- [ ] 회전 순서(YZX 등) 원본과 일치 (헬퍼 사용 또는 명시적 미이식 §7.1 등재)
- [ ] 좌우 대칭/교차 패턴 원본과 일치
- [ ] 스케일 함수(setArmScales/setLegScales) 가시 영향 평가 (B-3 처리 또는 §7.1 등재)
- [ ] 회귀 방지 감사 통과
- [ ] 빌드 성공

---

## 14. 회귀 방지 감사

| 기존 이식 | 영향 포인트 | 확인 |
|----------|-----------|------|
| isFeetVineClimbing pitch/roll 비대칭 | 일반 경로 가드 + vine 블록 누적 (`+=`) | [x] (세션 2 grep — L240/259) |
| isHandsVineClimbing armY *= 1.6662 + ±Eighth | sm_animateClimbing 추가 보정 | [x] (세션 2 grep — L222) |
| isDive rotateAngleX 3-way (Levitate/Jump/일반) | sm_animateDiving 분기 (setupTransforms TAIL) | [x] (세션 2 — sm_animateDiving 본체 변경 없음) |
| isCeilingClimb rotateY + horizontalAngle (threshold 0.015) | setupTransforms HEAD + sm_captureBodyYaw | [x] (세션 2 — MixinPlayerEntityRenderer 변경 없음) |
| 상태별 sm_captureBodyYaw 8 분기 | HEAD 우선순위 체인 | [x] (세션 2 — MixinPlayerEntityRenderer 변경 없음) |
| setAnglesYZX 헬퍼 (7회 호출) | isClimb hand/Climb leg / isSwim arm / isCrawl arm / isSlide arm | [x] (세션 2 grep — 7 호출 그대로) |
| setAnglesZXY 헬퍼 (2회 호출) | animateAngleJumping leg | [x] (세션 2 grep — 2 호출 그대로) |
| sm_setAngles leaningPitch = 0 강제 | vanilla setupTransforms Branch 2 차단 | [x] (세션 2 grep — L88) |
| sm_setupTransforms 5 상태 X 기울기 | isSwim/isDive/isSlide/isFlying/isHeadJump | [x] (세션 2 — 변경 없음) |
| isHeadJumping/isFlying head pitch 역보정 (ANIM-01) | -(Quarter-angle)/2 | [x] (세션 2 grep — L536/L558) |
| getPositionOffset isCrawling -scale*0.125 / isHeadJumping heightOffset | HEAD cancellable | [x] (세션 2 — 변경 없음) |
| renderName isCrawling 차단 / sneakNameTag 64블록 | hasLabel @Redirect | [x] (세션 2 — 변경 없음) |
| **세션 2 신규**: setArmScales/setLegScales 5 호출 | climbing arm/leg vine + swimming + diving + crawling | [x] grep 정합 |
| **세션 2 신규**: setAnglesYXZ 호출 2회 | isSwim head + isSlide body | [x] grep 정합 |
| **세션 2 신규**: setAnglesXZY 호출 2회 | isFlying arm + isFalling arm | [x] grep 정합 |

---

## 15. 작업 기록

### 세션 1 — 2026-04-25 — 4 Agent 병렬 전수 리서치 + 통합

**진행**:
- Agent A: SmartMovingModel 797줄 + SmartMovingRender 337줄 라인별 리서치 → 12 분기 + 1 default + animateAngleJumping/animateNonStandardWorking/animateNonStandardBowAiming/setArmScales/setLegScales 모든 수식 추출. SmartRenderContext 8 각도 상수 + FrequenceFactor 0.6662F 확정.
- Agent B: SmartRender mod 11 핵심 파일 (SmartRenderModel/Render/Mod/Context/Info/Install + ModelRotationRenderer/CapeRenderer/EarsRenderer/SpecialRenderer + RendererData) 분석 → 7 SR 전용 노드(Outer/Torso/Breast/Neck/Pelvic/RightShoulder/LeftShoulder) + 6 회전순서(XYZ/XZY/YXZ/YZX/ZXY/ZYX) + ignoreSuperRotation(GL_MODELVIEW translation 추출) + fade 보간(0.2F*timeDelta) → MatrixStack 60-70% 이식 가능 평가.
- Agent C: SM render playerapi + interface (IModelPlayer/IRenderPlayer/ModelPlayer/RenderPlayer/SmartRenderModelPlayerBase/SmartRenderRenderPlayerBase + SmartRenderContext) 8 파일 ~760줄 → 30+ 훅 메서드 (animateHeadRotation/Sleeping/ArmSwinging/Riding/LeftArmItemHolding/RightArmItemHolding/WorkingBody/WorkingArms/Sneaking/Arms/BowAiming) 추출. 1.21.1 SM Mixin 무결성 검증: MixinPlayerEntityRenderer 275줄 + MixinPlayerEntityModelClient 649줄. bodyYaw 95% / X tilt 100% 무결성.
- Agent D: 1.21.1 vanilla (BipedEntityModel.setAngles 13단계 + PlayerEntityModel 추가 5단계 + LivingEntityRenderer.render 16단계 + PlayerEntityRenderer.setupTransforms 3 분기) + SM Mixin (MixinPlayerEntityModelClient 11 헬퍼 + MixinPlayerEntityRenderer 5 inject + MixinLivingEntityRenderer 1 redirect) 매핑 표 → 누락 4건 / 근사 3건 / 미확인 4건 (§7) 분류.

**결과**:
- focus_01_animation.md 본 문서 235→500+줄 보강 (§3 16 케이스 / §5.1-5.6 인덱스+노드+상수+회전순서+Mixin / §7 위험 분류 / §10 B-1/B-2/B-3 선행 후보).
- §9에 헬퍼 부재(YXZ/XZY/ZYX) 위험 3개 영역 식별: isSwim head / isSlide body / isFlying·isFalling arm.
- §9에 setArmScales/setLegScales 미이식 식별: ModelPart xScale/yScale 활용 가능(B-08 확인됨), B-3 후보로 등록.

**잔여**:
- P-1 시각 재현 케이스 수집 (통합테스트 단계 — 사용자 직접 in-game 검증 후 §3 채움)
- B-1/B-2/B-3 선행 후보 진행 여부 결정 (시각 재현에서 영향 확인 후 또는 선제적 이식)

---

### 세션 2 — 2026-04-26 — Phase B 3/3 + C 4/4 = 7 원자 완결 (#1 AI 완결)

**사용자 지시**: "무조건 1대1 보강을 하고 해야됨" — 통합테스트 전 §10 B-1/B-2/B-3 선제 이식. NoScaleEnd 갑옷 분기는 ModelPart offsetY 부재로 메인 모델 범위 외 (§7 등재).

**진행한 작업**:

1. **B-3 setArmScales / setLegScales 1:1 이식** (MixinPlayerEntityModelClient.java)
   - 헬퍼 메서드 신규 추가 (L671-L683): `rightArm.yScale = rs; leftArm.yScale = ls;` (메인 모델 = `Scale` 타입 한정 — SmartMovingRender.java L88-L89 확인).
   - 호출 5 지점 1:1:
     * 원본 L353 → MixinPEMC L225-L228 (climbing isHandsVineClimbing 직후, 팔 yScale = abs(cos(pitch)))
     * 원본 L391 → MixinPEMC L269-L272 (climbing isFeetVineClimbing 직후, 다리 yScale = abs(cos(pitch)))
     * 원본 L532-L542 → MixinPEMC L375-L380 (swimming, sneakFactor 기반 호흡 패턴 0.15F * sneakFactor)
     * 원본 L572-L583 → MixinPEMC L403-L409 (diving, walkFactor 기반 0.25F leg / 0.15F arm — 계수 비대칭)
     * 원본 L622-L626 + L645-L648 → MixinPEMC L448-L455 (crawling, 좌우 위상 다름 — `cos(distance + Quarter - Quarter)` 등 원본 표기 보존)
   - NoScaleEnd 분기 (갑옷 흉갑 다리 전용 offsetY 보정) — §17 잔여 등재 (포커스 #1 외).

2. **B-1 setAnglesYXZ() 헬퍼 + 호출 교체** (MixinPlayerEntityModelClient.java)
   - 헬퍼 신규 추가 (L702-L717): `qY * qX * qZ → getEulerAnglesZYX`. R-17 GitHub 검증 (`YXZ(2): glRotatef(Z) → glRotatef(X) → glRotatef(Y)`) — post-multiply 역순 → call 순서 Z, X, Y.
   - 호출 교체:
     * 원본 SmartMovingModel L504-L506 (isSwim head, rotationOrder = YXZ) → MixinPEMC L348-L356 (sm_animateSwimming head, setAnglesYXZ).
     * 원본 SmartMovingModel L672-L676 (isSlide body, rotationOrder = YXZ) → MixinPEMC L472-L480 (sm_animateSliding body, setAnglesYXZ).
   - 직접 할당 (head.pitch / body.pitch+yaw 분리) → 헬퍼 1 호출로 통합.

3. **B-2 setAnglesXZY() 헬퍼 + 호출 교체** (MixinPlayerEntityModelClient.java)
   - 헬퍼 신규 추가 (L725-L740): `qX * qZ * qY → getEulerAnglesZYX`. R-17 검증 (`XZY(1): glRotatef(Y) → glRotatef(Z) → glRotatef(X)`) — call 순서 Y, Z, X.
   - 호출 교체:
     * 원본 SmartMovingModel L696-L733 (isFlying arm, rotationOrder = XZY) → MixinPEMC L514-L523 (sm_animateFlying arm, setAnglesXZY).
     * 원본 SmartMovingModel L768-L792 (isFalling arm, rotationOrder = XZY) → MixinPEMC L583-L593 (sm_animateFalling arm, setAnglesXZY).

4. **C-1 빌드** ✓ — `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (3 actionable, 4s).

5. **C-2 재현 케이스 grep 정합**:
   - `setArmScales` 호출 4 건 (climbing arm vine / swimming / diving / crawling)
   - `setLegScales` 호출 4 건 (climbing leg vine / swimming / diving / crawling)
   - `setAnglesYXZ` 호출 2 건 (swimming head / sliding body)
   - `setAnglesXZY` 호출 4 건 (flying right/left arm / falling right/left arm — 각 상태 2 호출)

6. **C-3 회귀 방지 감사** — §14 12 기존 항목 + 세션 2 신규 3 항목 모두 [x]:
   - 기존: vine pitch/roll 비대칭 / isHandsVineClimbing armY 보정 / Dive 3-way / Ceiling threshold / sm_captureBodyYaw 8 분기 / setAnglesYZX 7 호출 / setAnglesZXY 2 호출 / leaningPitch=0 / sm_setupTransforms 5 상태 / ANIM-01 head pitch / getPositionOffset / renderName 모두 변경 없음.
   - 신규: setArmScales/setLegScales/setAnglesYXZ/setAnglesXZY 호출 5/2/2 지점 grep 정합 확인.

7. **C-4 playtest_fixes.md 현재 포커스 갱신** — #4 → "#1 AI 완결 / 통합테스트 인계".

**완료 전 검증 체크리스트 (세션 2 기준)**:
- [근거] ✓ 원본 라인 (SmartMovingModel.java L353/L391/L532/L572/L622/L504/L672/L696/L768) 확보
- [근거] ✓ 1.21.1 이식 위치 (MixinPlayerEntityModelClient.java) 확정
- [대응] ✓ 원본 ↔ 1.21.1 side-by-side 1:1 (setArmScales 시그니처 정정 / scaleArmType=Scale 가정 / 좌우 위상 보존)
- [분기] ✓ 모든 호출 지점 if/else 보존 (climbing isHandsVine/isFeetVine 가드 / swimming/diving/crawling 의 NoScaleStart 가드는 메인 모델 = Scale 이므로 항상 true → 가드 제거가 1:1 동작)
- [상수] ✓ 0.15F sneakFactor / 0.25F walkFactor / 0.6662F FrequenceFactor / Half/Quarter/Eighth 라디안 그대로
- [회전순서] ✓ R-17 GitHub 검증 표 매칭 (YXZ Z,X,Y / XZY Y,Z,X / YZX X,Z,Y / ZXY Y,X,Z)
- [타이밍] ✓ setAngles @Inject(TAIL) 진입점 변경 없음, leaningPitch=0 위치 (L88) 변경 없음
- [근사] ✓ NoScaleEnd 갑옷 분기 §17 잔여 등재 + 주석 "근사 이식 — 원본과 차이: 갑옷 흉갑 다리 offsetY 보정 미이식"
- [신규] ✓ §16 신규 발견 4건 추가 등록
- [회귀] ✓ #2 / #2.5 / #2.6 / #2.7 / #3 / #4 영향 없음 (sm_setAngles 호출 순서 / sm_captureBodyYaw 8 분기 / sm_setupTransforms 5 상태 변경 없음)
- [빌드] ✓ BUILD SUCCESSFUL

**진행률**: Phase B 3/3 (100%), Phase C 4/4 (100%), 전체 #1 7/7 (100%) — **#1 AI 완결**.

**다음 세션**: 통합테스트 (사용자 in-game 시각 검증) — §3 16 케이스 채움 + 발견 시 B-N 추가.

---

### 세션 3 — 2026-04-26 — Phase R 진입 / R-1 청크 1 (SmartMovingModel.java L1-L200)

**사용자 지시 (2026-04-26)**: "에니메이션과 관련된 모든 코드를 싹 다 가져와서 그걸 전부 1대1 대응 ... 모든 라인을 라인별로 청크 분리해서 읽고 ... 리서치 + 포커스 보강 후에 작업 진행". → Phase R 진입.

**진행한 작업**:

1. **산출물 신규 파일 생성**: `docs/research/mapping/research_animation_line_by_line.md` (Phase R 라인별 매핑 표 전용).
2. **R-1 청크 1 라인별 read**: 원본 `C:\Work\minecraft\porting\sm_original\SmartMoving\...\SmartMovingModel.java` L1-L200 (200 라인 전수 read).
3. **1.21.1 매핑 검증**: `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java` L99-L244 grep + Read.
4. **매핑 표 작성**: 200 라인 모두 5종 분류 등재 (skip 0건).

**청크 1 통계**:
- [정합] 84 (ModelPart 표면 매핑 + isRopeSliding 본체 + isClimb handsClimb/feetClimb switch 3-way×2-way 전수)
- [오역] 3 (L80 + L144 + L200 — climbing 입력값 수직 → 수평 잘못 매핑)
- [누락] 2 (L106 + L117 — isRopeSliding 머리/팔 rotationPointY 변경 미이식)
- [잉여] 0
- [N/A] 111 (라이선스 + 패키지/import + 생성자 + Outer/Torso/Breast/Shoulder/Pelvic 구조 부재 + 빈 줄)
- 합계: 200 라인 전수.

**R-10+ B-N 후보 등록 (§16-10/11 참조)**:
- B-N: climbing arm/feet 입력 [오역] — totalVerticalDistance/currentVerticalSpeed → limbSwing/limbSwingAmount 잘못 매핑 (3 라인).
- B-N: isRopeSliding 머리/팔 rotationPointY 누락 — head +2F / arm -2F (2 라인).

**검증 체크리스트 (세션 3 R-1 청크 1)**:
- [근거] ✓ 원본 로컬 read (offset=1, limit=200 정확)
- [전수] ✓ 청크 내 200 라인 모두 매핑 표 등재 (skip 0)
- [분류] ✓ 5종 분류 (정합/오역/누락/잉여/N/A) 합계 200 일치
- [발견] ✓ §16-10/11 등재 + R-10+ B-N 후보 5 라인 식별
- [통계] ✓ 청크 통계 매핑 표 + 본 §15 양쪽 기록
- [검증] ✓ 1.21.1 대응 위치 grep (sm_animateRopeSliding L151 / sm_animateClimbing L190 / sm_setAngles L99 chain)
- [회귀] N/A (코드 변경 없음 — 매핑 표 작성만)
- [빌드] N/A (코드 변경 없음)

**다음 청크**: R-1 청크 2 (L201-L400) — climbing 본체 (handsDistanceSide·feetDistance·legAngles·armScales·vine 보정) + isCeilingClimb + isSwim 시작.

---

### 세션 4 — 2026-04-26 — Phase R / R-1 청크 2 (SmartMovingModel.java L201-L400)

**진행한 작업**:

1. **R-1 청크 2 라인별 read**: 원본 L201-L400 (200 라인 전수 read).
2. **1.21.1 매핑 검증**: MixinPlayerEntityModelClient sm_animateClimbing(L240-L310) + sm_animateCeilingClimbing(L316-L333) + sm_animateSwimming(L340-L381) + sm_animateDiving(L388-L410) + sm_animateCrawling 시작(L418-L425) 본체 read + grep.
3. **매핑 표 추가**: research_animation_line_by_line.md 청크 2 섹션 — 200 라인 모두 5종 분류 등재 (skip 0건).

**청크 2 통계**:
- [정합] 122 (handsClimbing arm Side / feetVine pitch&roll / isCrawlClimb 본체 / isClimbJump / isCeilingClimb 본체 / isSwim 본체 (head YXZ + arm YZX + leg + scale) / isDive 본체 (leg/arm/scale) / isCrawl 시작)
- [오역] 8 (L201/L219/L220 climb verticalDistance + L228/L232 FeetVine totalDistance + L365/L366/L367 Dive totalDistance/currentSpeed)
- [누락] 5 (L285 NoGrab body pivotZ -6F / L329 Swim head pivotZ -2F / L335 Swim body yaw / L370 Dive head pitch -Eighth / L371 Dive head pivotZ -2F)
- [잉여] 0 (라인 기준 — 1.21.1 측 추가 보정 sm_animateCeilingClimbing L332 별도 비고)
- [N/A] 65 (Outer/Torso 일부/Shoulder/Pelvic/Breast 구조 부재 + fade 메커니즘 + 빈 줄/괄호)
- 합계: 200 라인 전수.

**R-10+ B-N 후보 등록 (§16-12~17 참조)**:
- B-N: FeetVine total/difference 입력 [오역] (totalDistance → limbSwing) — 2 라인.
- B-N: Dive 입력값 [오역] (totalDistance + currentSpeed → limbSwing + limbSwingAmount) — 3 라인.
- B-N: NoGrab+non-NoStep body.pivotZ = -6F [누락] — 1 라인.
- B-N: Swim/Dive head 자세 [누락] (head.pitch + head.pivotZ) — 3 라인.
- B-N: Swim body.yaw [누락] — 1 라인.
- (검토) sm_animateCeilingClimbing head.yaw 추가 차감 [잉여] — R-10+ 제거 또는 검증.

**검증 체크리스트 (세션 4 R-1 청크 2)**:
- [근거] ✓ 원본 로컬 read (offset=201, limit=200 정확)
- [전수] ✓ 청크 내 200 라인 모두 매핑 표 등재 (skip 0)
- [분류] ✓ 5종 분류 합계 200 일치 (122 + 8 + 5 + 0 + 65)
- [발견] ✓ §16-12~17 등재 + R-10+ B-N 후보 10 라인 식별
- [통계] ✓ 매핑 표 + 본 §15 양쪽 기록
- [검증] ✓ 1.21.1 대응 위치 grep 검증 (sm_animateClimbing/CeilingClimbing/Swimming/Diving/Crawling 시작)
- [회귀] N/A (코드 변경 없음)
- [빌드] N/A (코드 변경 없음)

**다음 청크**: R-1 청크 3 (L401-L600) — isCrawl 본체 잔여 + isJump + isHeadJump + isSlide + isFalling 시작.

---

### 세션 5 — 2026-04-26 — Phase R / R-1 청크 3 (SmartMovingModel.java L401-L600)

**진행한 작업**:

1. **R-1 청크 3 라인별 read**: 원본 L401-L600 (200 라인 전수 read).
2. **1.21.1 매핑 검증**: MixinPlayerEntityModelClient sm_animateCrawling(L418-L457) + sm_animateSliding(L465-L492) + sm_animateFlying(L506-L537) + sm_animateHeadJumping(L545-L572) + sm_animateFalling(L580-L601) + sm_animateAngleJumping(L609-L624) 본체 read + grep.
3. **매핑 표 추가**: research_animation_line_by_line.md 청크 3 섹션 — 200 라인 모두 5종 분류 등재 (skip 0건).

**청크 3 통계**:
- [정합] 95 (isCrawl 본체 / isSlide 본체 / isFlying 본체 (XZY+ANIM-01) / isHeadJump 본체 (ANIM-01+overGroundBlock 단순화) / isFalling 본체 (XZY) / animateAngleJumping 다리 ZXY+팔)
- [오역] 3 (L477/L478/L479 isFlying 입력값 — totalDistance/currentSpeed → limbSwing/limbSwingAmount)
- [누락] 7 (L401 Crawl head.pivotZ -2F / L405 Crawl body.pivotY 3F / L442 Slide head.roll / L444 Slide head.pivotZ -2F / L448 Slide outer.pivotY 5F / L452 Slide body.offsetY -0.4F / L453 Slide body.pivotY 6.5F)
- [잉여] 0
- [N/A] 95 (Outer/Pelvic/Shoulder 구조 부재 + fade 메커니즘 부재 + isWorking + animateNonStandardWorking + animateNonStandardBowAiming 일체 + 빈 줄/괄호)
- 합계: 200 라인 전수.

**R-10+ B-N 후보 등록 (§16-18~21 참조)**:
- B-N: isCrawl head/body 피벗 [누락] — L401 head.pivotZ + L405 body.pivotY (2 라인).
- B-N: isSlide head/body/outer 피벗·offset [누락] — L442/L444/L448/L452/L453 (5 라인).
- B-N: isFlying 입력값 [오역] — L477/L478/L479 (3 라인, 청크 1/2 동일 패턴 확장).

**검증 체크리스트 (세션 5 R-1 청크 3)**:
- [근거] ✓ 원본 로컬 read (offset=401, limit=200 정확)
- [전수] ✓ 청크 내 200 라인 모두 매핑 표 등재 (skip 0)
- [분류] ✓ 5종 분류 합계 200 일치 (95+3+7+0+95)
- [발견] ✓ §16-18~21 등재 + R-10+ B-N 후보 10 라인 식별
- [통계] ✓ 매핑 표 + 본 §15 양쪽 기록
- [검증] ✓ 1.21.1 대응 위치 grep 검증 (sm_animateCrawling/Sliding/Flying/HeadJumping/Falling/AngleJumping)
- [회귀] N/A (코드 변경 없음)
- [빌드] N/A (코드 변경 없음)

**다음 청크**: R-1 청크 4 (L601-L797) — animateNonStandardBowAiming 잔여 + setArmScales/setLegScales 본체 + 기타 유틸/내부 헬퍼. R-1 마지막 청크 → 완료 시 R-2 진입.

---

### 세션 6 — 2026-04-26 — Phase R / R-1 청크 4 (SmartMovingModel.java L601-L797) — **R-1 완료**

**진행한 작업**:

1. **R-1 청크 4 라인별 read**: 원본 L601-L797 (197 라인 전수 read).
2. **1.21.1 매핑 검증**: MixinPlayerEntityModelClient setArmScales(L671-L674) + setLegScales(L681-L684) + setAnglesYZX/YXZ/XZY/ZXY(L690-L756) + smFactor(L762-L772) 본체 read.
3. **매핑 표 추가**: research_animation_line_by_line.md 청크 4 섹션 — 197 라인 모두 5종 분류 등재 (skip 0건).

**청크 4 통계**:
- [정합] 45 (animateArmSwinging isAngleJumping 분기 / setArmScales+setLegScales Scale 분기 본체 / Factor 함수 19 라인 / Between 함수 8 라인 (MathHelper.clamp 매핑) / Normalize 함수 8 라인 (MathHelper.wrapDegrees 매핑))
- [오역] 0
- [누락] 0
- [잉여] 0
- [N/A] 152 (animateNonStandardBowAiming 잔여 24 라인 / animateHeadRotation/Sleeping/Riding/HoldingItems/WorkingBody/WorkingArms/Sneaking/Arms/BowAiming 11 vanilla 분기 ~80 라인 / setArmScales/setLegScales NoScaleEnd 분기 18 라인 / 필드 선언 31 라인 / 빈 줄/괄호)
- 합계: 197 라인 전수.

**청크 4 발견**: 신규 [오역]/[누락]/[잉여] 0건. 헬퍼 본체 모두 1:1 이식 검증 (B-3 세션 2 완결 사후 검증).

**R-1 전체 누적 통계**:
- 합계: 정합 346 / 오역 14 / 누락 14 / 잉여 0 / N/A 423 = 797 라인 전수 (skip 0).
- R-10+ B-N 후보 13 그룹 식별 (오역 4 + 누락 7 + 잉여 검토 1 + 정합 근사 1).

**검증 체크리스트 (세션 6 R-1 청크 4 + R-1 전체)**:
- [근거] ✓ 원본 로컬 read (offset=601, limit=197 정확)
- [전수] ✓ 청크 내 197 라인 + R-1 전체 797 라인 모두 매핑 표 등재 (skip 0)
- [분류] ✓ 5종 분류 청크 4 합계 197 / R-1 전체 합계 797 일치
- [발견] ✓ 청크 4 신규 발견 0건 (헬퍼 본체 1:1 검증)
- [통계] ✓ R-1 누적 표 매핑 표 + 본 §15 양쪽 기록
- [검증] ✓ 1.21.1 대응 위치 grep 검증 (setArmScales/setLegScales/setAnglesYZX/YXZ/XZY/ZXY/smFactor 헬퍼 본체)
- [회귀] N/A (코드 변경 없음)
- [빌드] N/A (코드 변경 없음)

**R-1 완료** — SmartMovingModel.java 797 라인 전수 라인별 1:1 매핑 표 작성 완료.

**다음 R-단계**: R-2 (SmartMovingRender.java 337줄 + SM render/SmartRenderContext.java 26줄 + IModelPlayer.java 34줄 + IRenderPlayer.java 42줄 + ModelPlayer.java 167줄 + RenderPlayer.java 120줄 = ~726줄). 6 파일 1 청크 또는 SmartMovingRender.java 단독 1 청크 + 나머지 5 파일 1 청크.

---

### 세션 7 — 2026-04-26 — Phase R / R-2 청크 1 (SmartMovingRender.java L1-L200)

**진행한 작업**:

1. **R-2 청크 1 라인별 read**: 원본 SmartMovingRender.java L1-L200 (200 라인 전수 read).
2. **1.21.1 매핑 검증**: MixinPlayerEntityRenderer (sm_getPositionOffset L46 / sm_captureBodyYaw L74 / setupTransforms @ModifyArg L164 / sm_setupTransforms L184 / smartmoving$adjustLabelY L249) + MixinPlayerEntityModelClient (state capture L60 + smallOverGroundHeight L94-L96 + isFalling L125-L128) + SmartMovingClientState/Updater (mutex 처리) read.
3. **매핑 표 추가**: research_animation_line_by_line.md R-2 청크 1 섹션 — 200 라인 모두 5종 분류 등재 (skip 0건).

**청크 1 통계**:
- [정합] 51 (class @Mixin 매핑 / Scale 가드 근거 / SM 19 상태 캡처 분배 / smallOverGroundHeight 가드+계산 / sm_captureBodyYaw 8 분기 / @ModifyArg index=3 / sm_getPositionOffset heightOffset / renderName 4 분기 (crawlNameTag/heightOffset/sneakNameTag/Y 보정))
- [오역] 0
- [누락] 3 (L124/L125 타인 플레이어 isSneaking+isCrawl 시 d1 += 0.125D / L132-L134 Levitate 후처리 currentHorizontalAngle = currentCameraAngle)
- [잉여] 0
- [N/A] 146 (라이선스 + 패키지/import + 다층 모델 분배 (CurrentMainModel/modelPlayers 배열) + ArmorChestplate/Armor 갑옷 분기 + isInventory detect + setSneaking toggle + renderGuiIngame HUD)
- 합계: 200 라인 전수.

**R-10+ B-N 후보 등록 (§16-22~23 참조)**:
- B-N: 타인 플레이어 크롤링 자세 위치 보정 [누락] — L124/L125 d1 += 0.125D.
- B-N: Levitate 후처리 [누락] — L132-L134 currentHorizontalAngle = currentCameraAngle.

**검증 체크리스트 (세션 7 R-2 청크 1)**:
- [근거] ✓ 원본 로컬 read (offset=1, limit=200 정확)
- [전수] ✓ 청크 내 200 라인 모두 매핑 표 등재 (skip 0)
- [분류] ✓ 5종 분류 합계 200 일치 (51+0+3+0+146)
- [발견] ✓ §16-22~23 등재 + R-10+ B-N 후보 5 라인 식별
- [통계] ✓ 매핑 표 + 본 §15 양쪽 기록
- [검증] ✓ 1.21.1 대응 위치 grep 검증 (sm_captureBodyYaw / sm_getPositionOffset / sm_setupTransforms / @ModifyArg / smartmoving$adjustLabelY / state holder access)
- [회귀] N/A (코드 변경 없음)
- [빌드] N/A (코드 변경 없음)

**다음 청크**: R-2 청크 2 (SmartMovingRender.java L201-L337 + SM render 5 파일 ~389줄).

---

### 세션 8 — 2026-04-26 — Phase R / R-2 청크 2 (SmartMovingRender.java L201-L337 + Context/IModel/IRender)

**진행한 작업**:

1. **R-2 청크 2 라인별 read** (4 파일):
   - SmartMovingRender.java L201-L337 (137 라인) — renderGuiIngame HUD + drawIcon + 필드 선언
   - SmartRenderContext.java (27 라인) — Scale/NoScaleStart/NoScaleEnd 상수
   - IModelPlayer.java (35 라인) — 11 superAnimate* 인터페이스
   - IRenderPlayer.java (43 라인) — 9 메서드 인터페이스

2. **1.21.1 매핑 검증**:
   - HUD 영역: 본 포커스 #1 외 (UI/HUD 별도 포커스 영역) → 일괄 [N/A]
   - SmartRenderContext.Scale → setArmScales/setLegScales 가드 제거 근거 [정합]
   - NoScaleStart/NoScaleEnd → 갑옷 ArmorFeatureRenderer 영역 (§17 잔여) [N/A]
   - IModelPlayer 11 superAnimate* → vanilla 자동 처리 [N/A]
   - IRenderPlayer superRenderRotatePlayer → MixinPlayerEntityRenderer.sm_captureBodyYaw + @ModifyArg [정합]
   - IRenderPlayer superRenderRenderPlayerAt → sm_getPositionOffset [정합]
   - IRenderPlayer superRenderRenderName → smartmoving$adjustLabelY + MixinLivingEntityRenderer.hasLabel @Redirect [정합]

3. **매핑 표 추가**: research_animation_line_by_line.md R-2 청크 2 4 파트 섹션 — 242 라인 모두 5종 분류 등재 (skip 0건).

**청크 2 통계 (4 파일 합)**:
- [정합] 4 (Context Scale 상수 1 + IRender 3 핵심 매핑 시그니처)
- [오역] 0 / [누락] 0 / [잉여] 0
- [N/A] 238 (HUD 137 + Context NoScale 갑옷 분기 + IModel 11 시그니처 + IRender 6 시그니처 + 라이선스/import/빈 줄)
- 합계: 242 라인 전수.

**청크 2 발견**: 신규 [오역]/[누락]/[잉여] 0건. 본 청크는 HUD/인터페이스 시그니처 위주 — 매핑 의미는 R-2 청크 1 검증과 일치 확인.

**검증 체크리스트 (세션 8 R-2 청크 2)**:
- [근거] ✓ 4 파일 모두 라인별 read 완료 (offset+limit 정확)
- [전수] ✓ 4 파일 합계 242 라인 모두 매핑 표 등재 (skip 0)
- [분류] ✓ 5종 분류 합계 242 일치 (4+0+0+0+238)
- [발견] ✓ 신규 발견 0건 (의미 없는 인터페이스/HUD)
- [통계] ✓ 매핑 표 + 본 §15 양쪽 기록
- [검증] ✓ 1.21.1 대응 위치 grep 검증 (R-2 청크 1과 동일 매핑 일관 검증)
- [회귀] N/A (코드 변경 없음)
- [빌드] N/A (코드 변경 없음)

**R-2 진행률**: 청크 1 + 청크 2 = SmartMovingRender 337 + Context 27 + IModel 35 + IRender 43 = 442 / 726 라인 (60.9%). 다음 청크: ModelPlayer.java 167 + RenderPlayer.java 120 = 287 라인 (R-2 마지막).

---

### 세션 9 — 2026-04-26 — Phase R / R-2 청크 3 (ModelPlayer + RenderPlayer) — **R-2 완료**

**진행한 작업**:

1. **R-2 청크 3 라인별 read** (2 파일):
   - ModelPlayer.java (168 라인) — IModelPlayer 구현 클래스 (model 위임 + 11 animateXxx + 11 superAnimateXxx)
   - RenderPlayer.java (121 라인) — IRenderPlayer 구현 클래스 (render 위임 + doRender/rotateCorpse/renderLivingAt/passSpecialRender + super 호출)

2. **1.21.1 매핑 검증**:
   - ModelPlayer 22 메서드 모두 vanilla 자동 처리 또는 sm_setAngles TAIL inject 통합 → 일괄 [N/A]
   - RenderPlayer 5 진입점 매핑 [정합]: 클래스 (Mixin) / rotateCorpse(setupTransforms+@ModifyArg) / renderLivingAt(getPositionOffset) / passSpecialRender(adjustLabelY)
   - 위임 본체는 R-2 청크 1 SmartMovingRender 매핑과 중복 → [N/A]

3. **매핑 표 추가**: research_animation_line_by_line.md R-2 청크 3 2 파트 섹션 — 289 라인 모두 5종 분류 등재 (skip 0건).

**청크 3 통계 (2 파일 합)**:
- [정합] 5 (RenderPlayer 클래스 + rotateCorpse + renderLivingAt + passSpecialRender 진입점 5 라인)
- [오역] 0 / [누락] 0 / [잉여] 0
- [N/A] 284 (ModelPlayer 168 + RenderPlayer 116)
- 합계: 289 라인 전수.

**청크 3 발견**: 신규 [오역]/[누락]/[잉여] 0건. ModelPlayer/RenderPlayer는 위임자 패턴 — 1.21.1 Mixin 구조에서 자동 처리.

**검증 체크리스트 (세션 9 R-2 청크 3 + R-2 전체)**:
- [근거] ✓ 2 파일 라인별 read 완료
- [전수] ✓ 청크 내 289 라인 + R-2 전체 731 라인 모두 매핑 표 등재 (skip 0)
- [분류] ✓ 5종 분류 청크 3 합계 289 / R-2 전체 합계 731 일치
- [발견] ✓ 청크 3 신규 발견 0건 (위임자 패턴)
- [통계] ✓ R-2 누적 표 매핑 표 + 본 §15 양쪽 기록
- [검증] ✓ 1.21.1 대응 위치 grep 검증 (R-2 청크 1 매핑과 일관 확인)
- [회귀] N/A (코드 변경 없음)
- [빌드] N/A (코드 변경 없음)

**R-2 전체 누적 (731 라인, 6 파일 3 청크 3 세션)**:
- 합계: 정합 60 / 오역 0 / 누락 3 / 잉여 0 / N/A 668 = 731 라인 전수 (skip 0).
- R-10+ B-N 후보 2 그룹 식별 (청크 1 §16-22~23 — 타인 크롤링 위치 보정 + Levitate 후처리).

**R-2 완료** — SmartMovingRender + SM render 5 파일 731 라인 전수 라인별 1:1 매핑 완료.

**다음 R-단계**: R-3 (SM render/playerapi 3 파일 ~373줄 — SmartMoving.java 54 + SmartMovingModelPlayerBase.java 181 + SmartMovingRenderPlayerBase.java 138). 1 청크 또는 2 청크로 처리.

---

### 세션 10 — 2026-04-26 — Phase R / R-3 (SM render/playerapi 3 파일) — **R-3 완료**

**진행한 작업**:

1. **R-3 라인별 read** (3 파일 1 청크):
   - SmartMoving.java (55 라인) — PlayerAPI 등록 진입점
   - SmartMovingModelPlayerBase.java (182 라인) — IModelPlayer 구현 (PlayerAPI ModelPlayerBase 확장 + 11 dynamicOverride + 11 super dynamic + 16 @Deprecated ModelRenderer getter)
   - SmartMovingRenderPlayerBase.java (139 라인) — IRenderPlayer 구현 (PlayerAPI RenderPlayerBase 확장 + renderPlayer/rotatePlayer/renderPlayerSleep/passSpecialRender 위임 + isRenderedWithBodyTopAlwaysInAccelerateDirection)

2. **1.21.1 매핑 검증**:
   - PlayerAPI 인프라 (api.player.model/render) → 1.21.1 Fabric Mixin이 등가 (PlayerAPI 자체 부재) → 일괄 [N/A]
   - 16 @Deprecated ModelRenderer getter — 9 [정합] (Body/Head/Headwear→hat/RightArm/LeftArm/RightLeg/LeftLeg/Cloak → ModelPart 매핑) + 7 [N/A] (Outer/Torso/Breast/Neck/Shoulder×2/Pelvic/Ears 구조 부재 §16-2/3/5)
   - 핵심 진입점 매핑 [정합]: 클래스 (Mixin) / rotatePlayer (sm_captureBodyYaw + @ModifyArg) / renderPlayerSleep (sm_getPositionOffset) / passSpecialRender (smartmoving$adjustLabelY)
   - isRenderedWithBodyTopAlwaysInAccelerateDirection 4 분기 — sm_captureBodyYaw 8 분기 중 4 분기 (Flying/Swim/Dive/HeadJump) 합집합 일치 [정합]

3. **매핑 표 추가**: research_animation_line_by_line.md R-3 청크 1 3 파트 섹션 — 376 라인 모두 5종 분류 등재 (skip 0건).

**R-3 통계 (3 파일 합)**:
- [정합] 14 (ModelPlayerBase 클래스 + 9 ModelRenderer getter / RenderPlayerBase 클래스 + 3 진입점 + isRendered... 2 라인)
- [오역] 0 / [누락] 0 / [잉여] 0
- [N/A] 362 (PlayerAPI 인프라 + 11 dynamicOverride + 11 super dynamic + 7 구조 부재 getter + 위임 본체 (R-2 청크 1과 중복) + 빈 줄/괄호)
- 합계: 376 라인 전수.

**R-3 발견**: 신규 [오역]/[누락]/[잉여] 0건. R-3는 PlayerAPI 인프라 위임자 패턴 — 1.21.1 Mixin 구조에서 자동 처리. R-1/R-2와 매핑 일관 확인.

**검증 체크리스트 (세션 10 R-3)**:
- [근거] ✓ 3 파일 라인별 read 완료
- [전수] ✓ 376 라인 모두 매핑 표 등재 (skip 0)
- [분류] ✓ 5종 분류 합계 376 일치 (14+0+0+0+362)
- [발견] ✓ 신규 발견 0건 (인프라 위임자)
- [통계] ✓ 매핑 표 + 본 §15 양쪽 기록
- [검증] ✓ 1.21.1 대응 위치 grep 검증 (R-2 매핑 일관)
- [회귀] N/A (코드 변경 없음)
- [빌드] N/A (코드 변경 없음)

**R-3 완료** — SM render/playerapi 3 파일 376 라인 전수 라인별 1:1 매핑.

**다음 R-단계**: R-4 (SmartRenderModel.java 469줄 — 3 청크 × ~160줄). SmartRender mod의 핵심 모델 클래스로 7 SR 전용 노드 (Outer/Torso/Breast/Neck/Pelvic/RightShoulder/LeftShoulder) + ModelRotationRenderer 6 회전순서 정의. 1.21.1 단일 PlayerEntityModel 구조 차이로 [N/A] 비중 크지만, 회전순서 매핑 검증과 fade 보간 메커니즘 분석은 의미 있음.

---

### 세션 11 — 2026-04-26 — Phase R / R-4 청크 1 (SmartRenderModel.java L1-L160)

**진행한 작업**:

1. **R-4 청크 1 라인별 read**: 원본 SmartRenderModel.java L1-L160 (160 라인 전수 read).
2. **1.21.1 매핑 검증**: PlayerEntityModel ModelPart 직접 grep + R-3 16 @Deprecated getter 매핑과 일관 확인.
3. **매핑 표 추가**: research_animation_line_by_line.md R-4 청크 1 섹션 — 160 라인 모두 5종 분류 등재 (skip 0건).

**청크 1 통계**:
- [정합] 15 (body 2 / cloak 3 / head 2 / hat 2 / rightArm 1 / leftArm 1 / rightLeg 2 / leftLeg 2 — vanilla ModelPart 표면 매핑 + setRotationPoint pivot 등가)
- [오역] 0 / [누락] 0 / [잉여] 0
- [N/A] 145 (Outer/Torso/Breast/Neck/Shoulder×2/Pelvic/Ears 7 SR 전용 노드 부재 + fade 메커니즘 부재 + create/copy helper + render 본체 + 상태 복사 22 라인 + 라이선스/import/빈 줄)
- 합계: 160 라인 전수.

**청크 1 발견**: 신규 [오역]/[누락]/[잉여] 0건. 다만 다음 의미 있는 확인:
- L95-L98: §16-10/12/13/15 [오역] 발견 변수 (totalVerticalDistance/currentVerticalSpeed/totalDistance/currentSpeed)의 원본 정의 위치 확인 — SmartRenderModel.java 멤버 변수, SmartRenderRender 에서 매 프레임 갱신. 1.21.1 limbSwing/limbSwingAmount/animationProgress 잘못 매핑 영향 변수.
- L62-L64 bipedEars 생성에서 `copy(bipedCloak, originalBipedEars);` (원본 버그 — bipedEars 가 들어가야 하나 bipedCloak 들어감). Ears 노드 1.21.1 부재로 본 매핑 영향 없음.
- L41 `bipedOuter.fadeEnabled = true;` — fade 보간 0.2F*timeDelta (§17 잔여) 진입점 확인.

**검증 체크리스트 (세션 11 R-4 청크 1)**:
- [근거] ✓ 원본 로컬 read (offset=1, limit=160 정확)
- [전수] ✓ 청크 내 160 라인 모두 매핑 표 등재 (skip 0)
- [분류] ✓ 5종 분류 합계 160 일치 (15+0+0+0+145)
- [발견] ✓ 신규 발견 0건 (구조 부재 일관 확인)
- [통계] ✓ 매핑 표 + 본 §15 양쪽 기록
- [검증] ✓ 1.21.1 대응 위치 grep 검증 (R-3 16 ModelRenderer getter 매핑과 일관)
- [회귀] N/A (코드 변경 없음)
- [빌드] N/A (코드 변경 없음)

**다음 청크**: R-4 청크 2 (SmartRenderModel.java L161-L320) — render() 잔여 + setRotationAngles 본체 + animateXxx 메서드 일부.

---

### 세션 12 — 2026-04-26 — Phase R / R-4 청크 2 (SmartRenderModel.java L161-L320)

**진행한 작업**:

1. **R-4 청크 2 라인별 read**: 원본 SmartRenderModel.java L161-L320 (160 라인 전수 read).
2. **1.21.1 매핑 검증**: vanilla BipedEntityModel.setAngles 본체 공식 비교 (arm cos*0.6662*2.0*0.5 / leg cos*1.4 / Riding -π/5, -2π/5, ±π/10 / SleepingPose head + animateAttack handSwingProgress 등) + sm_setAngles TAIL inject 매핑.
3. **매핑 표 추가**: research_animation_line_by_line.md R-4 청크 2 섹션 — 160 라인 모두 5종 분류 등재 (skip 0건).

**청크 2 통계**:
- [정합] 30 (setRotationAngles 진입점 / sm_captureBodyYaw bodyYaw / animateHeadRotation 2 / animateSleeping head 2 / animateArmSwinging arm+leg 4 + 호출 1 / animateRiding 6 + 호출 1 / ItemHolding 좌우 2 + 호출 2 / animateWorkingBody 3 + 호출 1 / animateWorkingArms 7 + 호출 1 / animateSneaking leg 1 + 호출 1)
- [오역] 0
- [누락] 1 (L251 cloak.pitch = SIXTYFOURTH 기본 기울임)
- [잉여] 0
- [N/A] 129 (1인칭/인벤토리 분기 + Outer fade 메커니즘 + Torso/Breast/Neck/Pelvic 구조 부재 + 빈 줄/괄호)
- 합계: 160 라인 전수.

**R-10+ B-N 후보 등재 (§16-24)**:
- B-N (cloak.pitch 기본 기울임 [누락]): L251 `bipedCloak.rotateAngleX = Sixtyfourth;` (≈5.6°). 1.21.1 vanilla cloak는 별도 기본값 없으므로 미이식. 우선순위 낮음 (망토 미세 차이).

**검증 체크리스트 (세션 12 R-4 청크 2)**:
- [근거] ✓ 원본 로컬 read (offset=161, limit=160 정확)
- [전수] ✓ 청크 내 160 라인 모두 매핑 표 등재 (skip 0)
- [분류] ✓ 5종 분류 합계 160 일치 (30+0+1+0+129)
- [발견] ✓ §16-24 등재 + R-10+ B-N 후보 1 라인 식별
- [통계] ✓ 매핑 표 + 본 §15 양쪽 기록
- [검증] ✓ 1.21.1 대응 위치 grep 검증 (vanilla BipedEntityModel.setAngles 공식 일치 + sm_setAngles TAIL)
- [회귀] N/A (코드 변경 없음)
- [빌드] N/A (코드 변경 없음)

**다음 청크**: R-4 청크 3 (L321-L469) — animateSneaking 잔여 + animateArms + animateBowAiming + reset/getter/필드 선언. R-4 마지막 청크.

---

### 세션 13 — 2026-04-26 — Phase R / R-4 청크 3 (SmartRenderModel.java L321-L469) — **R-4 완료**

**진행한 작업**:

1. **R-4 청크 3 라인별 read**: 원본 SmartRenderModel.java L321-L469 (149 라인 전수 read).
2. **1.21.1 매핑 검증**: vanilla BipedEntityModel.setAngles applyAnimationOffsets + 활쏘기 분기 + reset 자동 + ModelData 시스템 (boxList 부재) 비교.
3. **매핑 표 추가**: research_animation_line_by_line.md R-4 청크 3 섹션 — 149 라인 모두 5종 분류 등재 (skip 0건).

**청크 3 통계**:
- [정합] 17 (sneak leg/arm 잔여 3 / animateArms 4 / animateBowAiming 본체 10)
- [오역] 0 / [누락] 0 / [잉여] 0
- [N/A] 132 (Pelvic/Breast/Neck/Shoulder×2 구조 부재 + Outer fade + reset 16 노드 + getRandomBox 영혼 파티클 + 필드 선언 44 라인 + 빈 줄)
- 합계: 149 라인 전수.

**청크 3 발견**: 신규 [오역]/[누락]/[잉여] 0건. 다음 검증:
- L321-L323 sneak + L336-L339 animateArms + L344-L353 animateBowAiming 모두 vanilla BipedEntityModel.setAngles 자동 처리 분기 일치 검증.
- L431-L434 §16-10/12/13/15 [오역] 발견 변수 (totalVerticalDistance/currentVerticalSpeed/totalDistance/currentSpeed)의 원본 *필드 선언* 위치 확인 — public field, SmartRenderRender 에서 매 프레임 갱신.

**검증 체크리스트 (세션 13 R-4 청크 3 + R-4 전체)**:
- [근거] ✓ 원본 로컬 read (offset=321, limit=149 정확)
- [전수] ✓ 청크 내 149 라인 + R-4 전체 469 라인 모두 매핑 표 등재 (skip 0)
- [분류] ✓ 5종 분류 청크 3 합계 149 / R-4 전체 합계 469 일치
- [발견] ✓ 청크 3 신규 발견 0건
- [통계] ✓ R-4 누적 표 매핑 표 + 본 §15 양쪽 기록
- [검증] ✓ 1.21.1 대응 위치 grep 검증 (vanilla 본체 일관)
- [회귀] N/A (코드 변경 없음)
- [빌드] N/A (코드 변경 없음)

**R-4 전체 누적 (469 라인, 3 청크 3 세션)**:
- 합계: 정합 62 / 오역 0 / 누락 1 / 잉여 0 / N/A 406 = 469 라인 전수 (skip 0).
- R-10+ B-N 후보 1 그룹 식별 (§16-24 cloak.pitch SIXTYFOURTH).

**R-4 완료** — SmartRenderModel.java 469 라인 전수 라인별 1:1 매핑.

**다음 R-단계**: R-5 (ModelRotationRenderer.java 368줄 (2 청크 × ~184) + RendererData/CapeRenderer/EarsRenderer/SpecialRenderer 4 파일 ~232줄 = 6 파일 ~601 라인). ModelRotationRenderer는 6 회전순서(XYZ/XZY/YXZ/YZX/ZXY/ZYX) 직접 정의 클래스로 R-1 회전순서 검증의 핵심 1차 자료.

---

## 16. 신규 발견

### 세션 1 (2026-04-25)

1. **YXZ/XZY/ZYX 헬퍼 부재**: setAnglesYZX/ZXY 2개만 구현됨. 4 상태(isSwim head / isSlide body / isFlying arm / isFalling arm)가 일반 XYZ로 근사됨 → 시각 차이 가능성. ModelRotationRenderer.md L528 확인됨.
2. **bipedTorso 부재로 isCrawl ≈79° 기울기 단일 노드 근사**: 원본 `bipedTorso.rotateAngleX = Quarter-Thirtytwoth`가 자식 (head/breast/neck/shoulder/arm) 전체 그룹 회전이었음. 1.21.1 body 단일 노드만 적용 → head/arm 자세 정합 부재.
3. **bipedPelvic 부재로 isRopeSliding pelvicX 누락**: 원본 `bipedPelvic.rotateAngleX = bipedTorso.rotateAngleX`가 다리 그룹 기울기를 형성. 1.21.1 leg는 body 자식이 아니므로 body.pitch만 설정해도 다리 별도. (현재 sm_animateRopeSliding은 leg.pitch 별도 처리됨 — 검증됨)
4. **ModelPart xScale/yScale/zScale 미활용**: B-08 확인됨 (`xScale=1.0F, yScale=1.0F, zScale=1.0F` public field, rotate() 내부 condition으로 적용). setAngles @Inject(TAIL)에서 설정해도 setTransform 미호출이므로 안전. setArmScales/setLegScales 이식 가능.
5. **animateNonStandardWorking/BowAiming 어깨 처리 N/A**: bipedRightShoulder/LeftShoulder + ignoreSuperRotation 부재로 SM 비표준 상태에서 도구 사용/활 조준 시 어깨 고정 불가. 영향: 클라이밍/수영 중 활/석궁 사용. 우선순위 낮음 (희귀 케이스).

### 세션 2 (2026-04-26)

6. **setArmScales/setLegScales 시그니처 정정**: 이전 프롬프트 진술 "6 인자" → 실제 **2 인자** (rightScale, leftScale, scaleY 만 적용). SmartMovingRender.java L88-L93 확인 — 메인 모델은 `Scale` 타입 / 갑옷은 `NoScaleStart` 또는 `NoScaleEnd`. 본 포커스 #1 메인 애니메이션 범위에서 Scale 분기 본체만 1:1 이식 (= `yScale = scale` 직접 할당).
7. **호출 5 지점 정정**: 이전 진술 "4 상태 영향" → 실제 **5 호출 지점** (climbing arm vine / climbing leg vine / swimming / diving / crawling). climbing 은 vine 가드 안에서만 호출되므로 vine 모드일 때만 visual 영향.
8. **NoScaleEnd offsetY 보정 (갑옷 흉갑 다리 전용)**: ModelPart 에 `offsetY` 필드 부재 + 갑옷 레이어는 별도 ArmorFeatureRenderer 처리. 본 포커스 #1 (메인 애니메이션) 범위 외 → §17 잔여 등재.
9. **YXZ 헬퍼 = qY * qX * qZ** / **XZY 헬퍼 = qX * qZ * qY** (둘 다 getEulerAnglesZYX 역분해): R-17 GitHub 검증으로 GL post-multiply 역순 = MatrixStack call 역순 매칭. 라디안 그대로 사용 (도 단위 변환 없음).

### 세션 3 (2026-04-26) — Phase R 진입 / R-1 청크 1 라인별 매핑

10. **[오역] climbing 입력값 — totalVerticalDistance/currentVerticalSpeed → limbSwing/limbSwingAmount 잘못 매핑** (R-10+ B-N 후보). SmartMovingModel.java 원본:
    - L80 `float totalVerticalDistance = md.totalVerticalDistance;` — 수직 누적 거리
    - L144 `float verticalSpeed = Math.min(0.5f, currentVerticalSpeed);` — 수직 속도 클램프
    - L200 `bipedRightArm.rotateAngleX = cos(totalVerticalDistance * handsFrequenceUpFactor + Half) * verticalSpeed * handsDistanceUpFactor + handsDistanceUpOffset;` — 수직 거리 기반 팔 위상

    1.21.1 sm_animateClimbing (L191/L214):
    - `verticalSpeed = Math.min(0.5f, limbSwingAmount)` — limbSwingAmount = entity.limbAnimator.getSpeed (수평 속도)
    - `cos(limbSwing * 0.6662f + HALF) * verticalSpeed * handsDistUp + handsOffset` — limbSwing = entity.limbAnimator.getPos (수평 누적)

    영향: 사다리/넝쿨 클라이밍 시 수직으로 오를 때 팔이 수평 거리 입력으로만 흔들려 클라이밍 동작감 손실. handsClimbType=UpGrab 일 때 handsDistUp=2f 로 큰 영향. feet 분기 L242-L244 `0.3f / verticalSpeed` 동일 부정확 전파.

    이식 방안 (R-10+):
    - 수직 입력 별도 capture — MixinLivingEntityRenderer 또는 SmartMovingClientState 에 `verticalDistance/verticalSpeed` (player.getY() 누적/델타) 보존 → sm_setAngles 호출 시점에 헬퍼 인자 추가.

11. **[누락] isRopeSliding rotationPointY 변경 — 머리 +2F / 팔 -2F 미이식** (R-10+ B-N 후보). SmartMovingModel.java 원본:
    - L106 `bipedHead.rotationPointY = 2F;` — 매달린 자세 머리 피벗 +2
    - L117 `bipedRightArm.rotationPointY = bipedLeftArm.rotationPointY = -2F;` — 팔 피벗 -2

    1.21.1 sm_animateRopeSliding (MixinPEMC L151-L182): 주석에 "rotationPointY 변경: 피벗 이동 생략" 명시 — 의도된 생략이지만 매핑 표 분류상 [누락]. 영향: 로프 매달림 자세에서 머리/팔 위치 미세 차이 (2 픽셀).

    이식 방안: ModelPart `pivotY` public field 사용 가능. 단 vanilla `setAngles` 진입 시점에 매 프레임 reset 되므로 sm_setAngles @Inject(TAIL) 위치에서 +2/-2 보정 → 안전.

### 세션 4 (2026-04-26) — Phase R / R-1 청크 2

12. **[오역] FeetVineClimbing total/difference 입력 — totalDistance → limbSwing**: SmartMovingModel.java 원본 L228 `float total = (cos(totalDistance + Half) + 1) * Thirtytwoth + Sixteenth;` / L232 `cos(totalDistance - Quarter)`. 1.21.1 sm_animateClimbing L260/L264: `cos(limbSwing + HALF)` / `cos(limbSwing - QUARTER)`. `totalDistance`(SmartRenderModel.totalDistance — 수평+수직 누적) ≠ `limbSwing`(수평 누적). 넝쿨 클라이밍 발 흔들림 위상 차이. R-10+ B-N 후보.

13. **[오역] Dive 입력값 — totalDistance/currentSpeed → limbSwing/limbSwingAmount** (R-10+ B-N 후보). 원본:
    - L365 `float distance = totalDistance * 0.7F;` (수평+수직 누적)
    - L366 `float walkFactor = Factor(currentSpeed, 0F, 0.15679921F);` (3D 속도)
    - L367 `float standFactor = Factor(currentSpeed, 0.15679921F, 0F);`

    1.21.1 sm_animateDiving L389-L391: limbSwing/limbSwingAmount(수평만) 사용. 다이빙은 수직+수평 운동 모두 강한 상태이므로 차이 가시화 가능.

14. **[누락] bipedTorso.rotationPointZ = -6F — NoGrab+non-NoStep 분기** (R-10+ B-N 후보). 원본 L285: 손 NoGrab(NONE/SINK) + 발 not-NoStep 매달림 자세에서 몸 피벗 Z=-6 픽셀 (벽에서 떨어진 자세). 1.21.1 sm_animateClimbing L304 주석에 "bipedPelvic/rotationPointZ는 1.21.1 대응 없음"으로 명시되어 있으나 ModelPart `body.pivotZ`는 public field이므로 이식 가능.

15. **[누락] Swim/Dive head 자세 — head.rotationPointZ = -2F + Dive head.rotateAngleX = -Eighth** (R-10+ B-N 후보). 원본:
    - Swim L329 `bipedHead.rotationPointZ = -2F;` — 1.21.1 sm_animateSwimming 미이식
    - Dive L370 `bipedHead.rotateAngleX = -Eighth;` + L371 `bipedHead.rotationPointZ = -2F;` — 1.21.1 sm_animateDiving 미이식

    영향: 수영/다이빙 시 머리 위치 (2 픽셀 앞) + 다이빙 시 머리 살짝 위로 들기 (-Eighth) 부재 → 자세 정합 부족.

16. **[누락] Swim body yaw — body.rotateAngleY 좌우 흔들림 미이식** (R-10+ B-N 후보). 원본 L335: `bipedBreast.rotateAngleY = bipedBody.rotateAngleY = cos(distance / 2.0F - Quarter) * walkFactor;` (Breast는 부재이지만 Body 부분은 이식 가능). 1.21.1 sm_animateSwimming 본체에 `body.yaw` 설정 부재. 영향: 수영 시 몸통 좌우 흔들림 (자유형 영법 동작) 결손.

17. **[잉여] sm_animateCeilingClimbing head.yaw 추가 차감 — 원본 미존재 보정**. 1.21.1 L332: `head.yaw -= headYaw * DEG_TO_RAD;` (원본 L315에는 `bipedHead.rotateAngleY = -rotateY` 단순 설정만). vanilla setAngles가 진입 시 head.yaw를 자동 설정하지만, sm_setAngles는 TAIL inject이므로 vanilla 결과를 덮어쓰는 것이 정상. 추가 차감은 의도되지 않은 보정으로 판단. R-10+ 검토 (제거 또는 검증).

### 세션 5 (2026-04-26) — Phase R / R-1 청크 3

18. **[누락] isCrawl 머리/몸통 피벗** (R-10+ B-N 후보). 원본:
    - L401 `bipedHead.rotationPointZ = -2F;` — 크롤링 머리 Z 피벗 (몸이 수평이므로 머리 앞쪽으로 2 픽셀 이동)
    - L405 `bipedTorso.rotationPointY = 3F;` — 몸통 Y 피벗 +3 (수평 자세에서 몸통 위치 보정)

    1.21.1 sm_animateCrawling: head.pivotZ + body.pivotY 미적용. ModelPart `pivotY`/`pivotZ` public field 사용 가능. R-10+ 이식.

19. **[누락] isSlide 머리/몸통/Outer 피벗·offset** (R-10+ B-N 후보). 원본:
    - L442 `bipedHead.rotateAngleZ = -viewHorizontalAngelOffset / RadiantToAngle;` — head.roll 방향 정렬
    - L444 `bipedHead.rotationPointZ = -2F;`
    - L448 `bipedOuter.rotationPointY = 5F;` — outer 전체 Y 피벗 (entity-level translate 가능)
    - L452 `bipedBody.offsetY = -0.4F;` — ⚠️ ModelPart 에 offsetY 필드 부재 (MatrixStack translate 보정 필요)
    - L453 `bipedBody.rotationPointY = +6.5F;` — body.pivotY 가능

    1.21.1 sm_animateSliding 본체에 모두 미적용. 영향: 슬라이딩 자세에서 머리 방향/위치, 몸 전체 위치 (5 + 6.5 - 0.4 픽셀) 미세 차이.

20. **[오역] isFlying 입력값 — totalDistance/currentSpeed → limbSwing/limbSwingAmount** (R-10+ B-N 후보). 원본 L477-L479. 청크 1/2의 climb/dive 동일 패턴. 1.21.1 sm_animateFlying L507-L509에서 `limbSwing`/`limbSwingAmount` 사용. 비행 위상은 walkFactor/standFactor 분기로 영향이 크므로 가시 영향 가능.

21. **[정합 (근사)] isHeadJump overGroundBlock 단순화**. 원본 L521 `if(overGroundBlock != null && overGroundBlock.getMaterial().isSolid())` (머리 위 블록의 재질 검사) → 1.21.1 sm_animateHeadJumping L562 `if (sm.smallOverGroundHeight < 5f)` (단순 높이 검사). computeSmallOverGroundHeight 가 5블록 스캔 후 첫 고체 블록 거리 반환하므로 5f 미만 = 머리 위 고체 블록 존재 ≈ 등가. material 분류는 손실 (물·용암 등 비고체 콜리전이 다른 경우 차이 가능). R-10+ 정밀 검토 우선순위 낮음.

### 세션 7 (2026-04-26) — Phase R / R-2 청크 1 (SmartMovingRender.java)

22. **[누락] 타인 플레이어 크롤링 자세 위치 보정** (R-10+ B-N 후보). 원본 SmartMovingRender.java L124-L125: `if (!isInventory && entityplayer.isSneaking() && !(entityplayer instanceof EntityPlayerSP) && isCrawl) d1 += 0.125D;`. 다른 플레이어가 isSneaking + isCrawl 상태로 보일 때 렌더 Y 위치를 0.125 (1/8 블록) 위로 올림. 1.21.1 sm_getPositionOffset 미이식. 영향: 타인 플레이어가 크롤링 중 다리·발이 지면을 뚫고 보일 가능성. 자기 자신은 isSneaking 자동 false → 미적용 (영향 없음).

    이식 방안: sm_getPositionOffset 분기 추가 — `else if (!isOwnPlayer && entity.isSneaking() && sm.isCrawling) cir.setReturnValue(new Vec3d(0, 0.125, 0))`.

23. **[누락] Levitate 후처리 — currentHorizontalAngle = currentCameraAngle 강제 정렬** (R-10+ B-N 후보). 원본 SmartMovingRender.java L132-L134: `if (moving.isLevitating && modelPlayers != null) for(...) modelPlayers[i].getMovingModel().md.currentHorizontalAngle = modelPlayers[i].getMovingModel().md.currentCameraAngle;`. Levitating(공중부양 효과) 시 horizontal angle을 카메라 방향으로 강제 정렬 (= 플레이어가 카메라를 향해 떠 있게 보이도록). 1.21.1 sm_captureBodyYaw 본체 미이식.

    영향: 공중부양 status effect 발동 중 isDive 분기로 렌더되지만, dive horizontal angle이 이동 방향 기반으로 계산됨 → 카메라 회전과 분리. 원본은 카메라 방향과 동기화. 영향: 공중부양 시 몸 방향 정합성 손실.

    이식 방안: sm_captureBodyYaw 마지막에 `if (sm.isLevitating) smBodyYawOverride = MathHelper.lerp(tickDelta, player.prevYaw, player.getYaw())` 추가 (카메라 = player yaw 등가).

### 세션 12 (2026-04-26) — Phase R / R-4 청크 2 (SmartRenderModel.java L161-L320)

24. **[누락] cloak.pitch 기본 기울임 — SIXTYFOURTH (≈5.6°) 미이식** (R-10+ 우선순위 낮음). 원본 SmartRenderModel.java L251: `bipedCloak.rotateAngleX = Sixtyfourth;` (setRotationAngles 본체 마지막에 망토 X 살짝 기울임). 1.21.1 vanilla cloak (PlayerEntityModel.cloak)는 별도 기본 기울임 없이 그대로 사용 — 미이식. 망토 미세 시각 차이 (≈5.6°만큼 살짝 뒤로 기울임 부재). 우선순위 낮음 (UI 영향 적음, 메인 애니메이션 외).

    이식 방안: sm_setAngles @Inject(TAIL) 위치에서 `cloak.pitch = SIXTYFOURTH` 추가. 단 vanilla setAngles 가 cloak.pitch를 매 프레임 reset 하지 않으므로 누적 위험 검토 필요.

---

## 17. 잔여 / 후속

- SR 전용 노드(bipedOuter/torso 등 7개) 시각 재현을 위한 다층 모델 렌더는 1.21.1 단일 PlayerEntityModel 구조상 근본적으로 불가 → 구조적 N/A
- 애니메이션 속도(스윙 주기) 세밀 조정 — vanilla limbAnimator 기반이므로 #6 회귀(증가/감소 속도) 영향 가능 → 통합테스트 후 분리 평가
- ~~ModelPart xScale/yScale/zScale 활용 setArmScales/setLegScales 이식 (B-3 선행 후보)~~ ✅ 세션 2 완료
- ~~YXZ/XZY/ZYX 헬퍼 추가 (B-1/B-2 선행 후보)~~ ✅ 세션 2 완료 (YXZ + XZY). ZYX 헬퍼는 어깨 N/A 로 미이식.
- bipedTorso isCrawl ≈79° 기울기를 자식 노드(head/arm) 일괄 보정으로 근사 (선택)
- fade 보간 0.2F*timeDelta 계수 vanilla lerpAngleDegrees 차이 — bodyYaw 부드러움 영향 (저우선)
- **NoScaleEnd 갑옷 흉갑 다리 offsetY 보정** — ArmorFeatureRenderer Mixin 별도 작업 (포커스 #1 외, 갑옷 레이어 영역). ModelPart 에 offsetY 필드 부재 → MatrixStack translate 보정 필요.
- **animateNonStandardWorking / BowAiming 어깨 ZYX** — 1.21.1 PlayerEntityModel 에 어깨 노드 부재로 구조적 N/A. SM 비표준 상태(클라이밍/수영) 중 활/석궁 사용 시 어깨 고정 불가. 우선순위 낮음.
