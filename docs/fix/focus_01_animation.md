# Focus #1 — 애니메이션 망가짐 / 이상함

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #1 일 때 진입.
> 세션 1 (2026-04-25): 4 Agent 병렬 전수 리서치 + 통합 + 12 분기 + 1 default 라인별 1:1 검증 표 보강.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | 🟡 리서치 완료 / 통합 테스트 (#6 회귀 포함) 대기 |
| 현재 단계 | 시각 재현 케이스 수집 (P) |
| 선행 의존 | #2 (애니메이션 입력 상태가 정확해야 의미 있음) — 완료 |

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

### B. 수정 (잠재 위험 우선)
- [ ] B-1. (선행 후보) YXZ 헬퍼 추가 (`setAnglesYXZ()`) — isSwim head / isSlide body 영향
- [ ] B-2. (선행 후보) XZY 헬퍼 추가 (`setAnglesXZY()`) — isFlying / isFalling arm 영향
- [ ] B-3. (선행 후보) ModelPart.xScale/yScale/zScale 활용 setArmScales/setLegScales 이식 — isHandsVineClimbing / isFeetVineClimbing / isSwim / isCrawl
- [ ] B-N. (시각 재현 케이스별 공식 교체)

### C. 검증
- [ ] C-1. 빌드
- [ ] C-2. 각 상태 시각 검증 — 원본과 육안 비교
- [ ] C-3. 회귀 방지 감사 (§14)
- [ ] C-4. `playtest_fixes.md` "현재 포커스" → (다음 없음, 전체 완료)

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
| isFeetVineClimbing pitch/roll 비대칭 | 일반 경로 가드 + vine 블록 누적 (`+=`) | [ ] |
| isHandsVineClimbing armY *= 1.6662 + ±Eighth | sm_animateClimbing 추가 보정 | [ ] |
| isDive rotateAngleX 3-way (Levitate/Jump/일반) | sm_animateDiving 분기 | [ ] |
| isCeilingClimb rotateY + horizontalAngle (threshold 0.015) | setupTransforms HEAD + sm_captureBodyYaw | [ ] |
| 상태별 sm_captureBodyYaw 8 분기 | HEAD 우선순위 체인 (Ceiling > Swim/Dive > Flying > HeadJump/Crawl/Rope > Climb/Slide/AngleJump) | [ ] |
| setAnglesYZX 헬퍼 (7회 호출) | isClimb hand/Climb leg / isSwim arm / isCrawl arm / isSlide arm | [ ] |
| setAnglesZXY 헬퍼 (2회 호출) | animateAngleJumping leg | [ ] |
| sm_setAngles leaningPitch = 0 강제 | vanilla setupTransforms Branch 2 차단 | [ ] |
| sm_setupTransforms 5 상태 X 기울기 | isSwim/isDive/isSlide/isFlying/isHeadJump | [ ] |
| isHeadJumping/isFlying head pitch 역보정 (ANIM-01) | -(Quarter-angle)/2 | [ ] |
| getPositionOffset isCrawling -scale*0.125 / isHeadJumping heightOffset | HEAD cancellable | [ ] |
| renderName isCrawling 차단 / sneakNameTag 64블록 | hasLabel @Redirect | [ ] |

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

## 16. 신규 발견

### 세션 1 (2026-04-25)

1. **YXZ/XZY/ZYX 헬퍼 부재**: setAnglesYZX/ZXY 2개만 구현됨. 4 상태(isSwim head / isSlide body / isFlying arm / isFalling arm)가 일반 XYZ로 근사됨 → 시각 차이 가능성. ModelRotationRenderer.md L528 확인됨.
2. **bipedTorso 부재로 isCrawl ≈79° 기울기 단일 노드 근사**: 원본 `bipedTorso.rotateAngleX = Quarter-Thirtytwoth`가 자식 (head/breast/neck/shoulder/arm) 전체 그룹 회전이었음. 1.21.1 body 단일 노드만 적용 → head/arm 자세 정합 부재.
3. **bipedPelvic 부재로 isRopeSliding pelvicX 누락**: 원본 `bipedPelvic.rotateAngleX = bipedTorso.rotateAngleX`가 다리 그룹 기울기를 형성. 1.21.1 leg는 body 자식이 아니므로 body.pitch만 설정해도 다리 별도. (현재 sm_animateRopeSliding은 leg.pitch 별도 처리됨 — 검증됨)
4. **ModelPart xScale/yScale/zScale 미활용**: B-08 확인됨 (`xScale=1.0F, yScale=1.0F, zScale=1.0F` public field, rotate() 내부 condition으로 적용). setAngles @Inject(TAIL)에서 설정해도 setTransform 미호출이므로 안전. setArmScales/setLegScales 이식 가능.
5. **animateNonStandardWorking/BowAiming 어깨 처리 N/A**: bipedRightShoulder/LeftShoulder + ignoreSuperRotation 부재로 SM 비표준 상태에서 도구 사용/활 조준 시 어깨 고정 불가. 영향: 클라이밍/수영 중 활/석궁 사용. 우선순위 낮음 (희귀 케이스).

---

## 17. 잔여 / 후속

- SR 전용 노드(bipedOuter/torso 등 7개) 시각 재현을 위한 다층 모델 렌더는 1.21.1 단일 PlayerEntityModel 구조상 근본적으로 불가 → 구조적 N/A
- 애니메이션 속도(스윙 주기) 세밀 조정 — vanilla limbAnimator 기반이므로 #6 회귀(증가/감소 속도) 영향 가능 → 통합테스트 후 분리 평가
- ModelPart xScale/yScale/zScale 활용 setArmScales/setLegScales 이식 (B-3 선행 후보)
- YXZ/XZY/ZYX 헬퍼 추가 (B-1/B-2 선행 후보)
- bipedTorso isCrawl ≈79° 기울기를 자식 노드(head/arm) 일괄 보정으로 근사 (선택)
- fade 보간 0.2F*timeDelta 계수 vanilla lerpAngleDegrees 차이 — bodyYaw 부드러움 영향 (저우선)
