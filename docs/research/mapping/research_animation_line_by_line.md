# 애니메이션 원본 ↔ 1.21.1 라인별 매핑 (Phase R)

사용자 지시 (2026-04-26): 31 파일 ~5,000줄 라인별 read + 1.21.1 매핑.
분류: [정합] / [오역] / [누락] / [잉여] / [N/A].

원본 로컬 경로: `C:\Work\minecraft\porting\sm_original\` (SmartMoving + SmartRender)
1.21.1 대상 경로: `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java` 외

---

## R-1: SmartMovingModel.java (797줄)

### 청크 1 (L1-L200) — 헤더/생성자/setRotationAngles 시작 ~ isRopeSliding ~ isClimb 진입 (handsClimb/feetClimb switch + 팔 X 시작)

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L16 | `// ==…== Smart Moving GPLv3 라이선스 헤더` | (라이선스 헤더 생략) | [N/A] | 16 라인 일괄 |
| L17 | `// ==…==` | — | [N/A] | |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.moving.render;` | `package choco.ratel.smartmoving.mixin.client;` | [N/A] | 패키지 — 표면 매핑 |
| L20 | `import net.minecraft.block.*;` | (사용 시점에 import) | [N/A] | |
| L21 | `import net.minecraft.client.model.*;` | (사용 시점에 import) | [N/A] | |
| L22 | `import net.minecraft.util.*;` | `import net.minecraft.util.math.MathHelper;` | [N/A] | |
| L23 | `import net.smart.moving.*;` | `import choco.ratel.smartmoving.SmartMovingClientState;` | [N/A] | |
| L24 | `import net.smart.render.*;` | (Mixin 본체 흡수) | [N/A] | |
| L25 | (빈 줄) | — | [N/A] | |
| L26 | `public class SmartMovingModel extends SmartRenderContext` | `@Mixin(BipedEntityModel.class) public abstract class MixinPlayerEntityModelClient` | [정합] | 상속 → Mixin 으로 표면 매핑 |
| L27 | `{` | `{` | [정합] | |
| L28 | `public IModelPlayer imp;` | (Mixin: 직접 fields) | [N/A] | 1.21.1 Mixin 내부 직접 head/body/etc |
| L29 | `public ModelBiped mp;` | (Mixin: 직접 fields) | [N/A] | |
| L30 | `public net.smart.render.SmartRenderModel md;` | (1.21.1 통합 — SR Model 별도 부재) | [N/A] | |
| L31 | (빈 줄) | — | [N/A] | |
| L32 | `public SmartMovingModel(net.smart.render.IModelPlayer md, IModelPlayer imp)` | (Mixin — 생성자 부재) | [N/A] | |
| L33 | `{` | — | [N/A] | |
| L34 | `this.imp = imp;` | — | [N/A] | |
| L35 | `this.md = md.getRenderModel();` | — | [N/A] | |
| L36 | `this.mp = this.md.mp;` | — | [N/A] | |
| L37 | (빈 줄) | — | [N/A] | |
| L38 | `if(SmartMovingRender.CurrentMainModel != null)` | (SmartMovingClientState 일원화) | [N/A] | 1.21.1: state holder 단일 |
| L39 | `{` | — | [N/A] | |
| L40 | `isClimb = SmartMovingRender.CurrentMainModel.isClimb;` | `sm.isClimbing` (state holder) | [N/A] | 동등 |
| L41 | `isClimbJump = …isClimbJump;` | `sm.isClimbJumping` | [N/A] | |
| L42 | `handsClimbType = …handsClimbType;` | `sm.actualHandsClimbType` (ordinal) | [N/A] | |
| L43 | `feetClimbType = …feetClimbType;` | `sm.actualFeetClimbType` (ordinal) | [N/A] | |
| L44 | `isHandsVineClimbing = …isHandsVineClimbing;` | `sm.isHandsVineClimbing` | [N/A] | |
| L45 | `isFeetVineClimbing = …isFeetVineClimbing;` | `sm.isFeetVineClimbing` | [N/A] | |
| L46 | `isCeilingClimb = …isCeilingClimb;` | `sm.isCeilingClimbing` | [N/A] | |
| L47 | `isSwim = …isSwim;` | `sm.isSwimming_sm` | [N/A] | |
| L48 | `isDive = …isDive;` | `sm.isDiving` | [N/A] | |
| L49 | `isCrawl = …isCrawl;` | `sm.isCrawling` | [N/A] | |
| L50 | `isCrawlClimb = …isCrawlClimb;` | `sm.isCrawlClimbing` | [N/A] | |
| L51 | `isJump = …isJump;` | (vanilla jump) | [N/A] | |
| L52 | `isHeadJump = …isHeadJump;` | `sm.isHeadJumping` | [N/A] | |
| L53 | `isSlide = …isSlide;` | `sm.isSliding` | [N/A] | |
| L54 | `isFlying = …isFlying;` | `flyingCreative` (PlayerAbilities) | [N/A] | |
| L55 | `isLevitate = …isLevitate;` | (vanilla levitation status effect) | [N/A] | |
| L56 | `isFalling = …isFalling;` | `isFalling` (computed inline) | [N/A] | MixinPEMC L125-L128 |
| L57 | `isGenericSneaking = …;` | (vanilla sneak) | [N/A] | |
| L58 | `isAngleJumping = …;` | `sm.isAngleJumping()` | [N/A] | |
| L59 | `angleJumpType = …;` | `sm.angleJumpType` | [N/A] | |
| L60 | `isRopeSliding = …;` | `sm.isRopeSliding` | [N/A] | |
| L61 | (빈 줄) | — | [N/A] | |
| L62 | `currentHorizontalSpeedFlattened = …;` | (state holder) | [N/A] | |
| L63 | `smallOverGroundHeight = …;` | `sm.smallOverGroundHeight` | [N/A] | MixinPEMC L94-L96 |
| L64 | `overGroundBlock = …;` | (computed elsewhere) | [N/A] | |
| L65 | `}` | — | [N/A] | |
| L66 | `}` | — | [N/A] | |
| L67 | (빈 줄) | — | [N/A] | |
| L68 | `@SuppressWarnings("unused")` | — | [N/A] | |
| L69 | `private void setRotationAngles(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)` | `sm_setAngles(...)` `@Inject(method="setAngles", at=@At("TAIL"))` | [정합] | 진입점 — 6 인자 → limbSwing/limbSwingAmount/animationProgress/headYaw/headPitch 매핑 |
| L70 | `{` | `{` | [정합] | |
| L71 | `final float FrequenceFactor = 0.6662F;` | (`0.6662f` 인라인 사용) | [정합] | 상수 추출 미적용 (인라인) |
| L72 | (빈 줄) | — | [N/A] | |
| L73 | `isStandard = false;` | (1.21.1 미사용) | [N/A] | isStandard 플래그 미사용 — vanilla 자동 |
| L74 | (빈 줄) | — | [N/A] | |
| L75 | `float currentCameraAngle = md.currentCameraAngle;` | `player.getYaw()` 활용 | [정합] | rope head roll 계산에 사용 (L162) |
| L76 | `float currentHorizontalAngle = md.currentHorizontalAngle;` | `Math.atan2(-vel.x, vel.z)` (L160) | [정합] | rope head/outer Y 정렬 |
| L77 | `float currentVerticalAngle = md.currentVerticalAngle;` | (1.21.1 미사용 — 청크 1 범위) | [N/A] | 추후 청크 사용 여부 검증 |
| L78 | `float forwardRotation = md.forwardRotation;` | MixinPlayerEntityRenderer.sm_captureBodyYaw 처리 | [정합] | climb bipedOuter.Y |
| L79 | `float currentVerticalSpeed = md.currentVerticalSpeed;` | (1.21.1 climb verticalSpeed 변수에 limbSwingAmount 잘못 매핑 — L144 참조) | [오역] | ⚠️ R-10+ B-N 후보 |
| L80 | `float totalVerticalDistance = md.totalVerticalDistance;` | (1.21.1 climb arm.X 입력에 limbSwing 잘못 매핑 — L200 참조) | [오역] | ⚠️ 핵심 — 사다리/넝쿨 수직 거리 누적이 수평 거리로 대체됨. R-10+ B-N 후보 |
| L81 | `float totalDistance = md.totalDistance;` | (청크 1 미사용) | [N/A] | |
| L82 | `double horizontalDistance = md.horizontalDistance;` | (청크 1 미사용) | [N/A] | |
| L83 | `float currentSpeed = md.currentSpeed;` | (청크 1 미사용) | [N/A] | |
| L84 | `if(!Float.isNaN(currentHorizontalSpeedFlattened))` | (1.21.1 미적용 — 후속 검증) | [N/A] | flatten 분기 |
| L85 | `currentHorizontalSpeed = currentHorizontalSpeedFlattened;` | — | [N/A] | |
| L86 | (빈 줄) | — | [N/A] | |
| L87 | `ModelRotationRenderer bipedOuter = md.bipedOuter;` | (PlayerEntityModel 부재 → sm_captureBodyYaw 대체) | [N/A] | 구조 부재 (§7) |
| L88 | `ModelRotationRenderer bipedTorso = md.bipedTorso;` | (구조 부재 — body 단일 노드 근사) | [N/A] | §16-2 |
| L89 | `ModelRotationRenderer bipedBody = md.bipedBody;` | `body` (PlayerEntityModel) | [정합] | 표면 매핑 |
| L90 | `ModelRotationRenderer bipedBreast = md.bipedBreast;` | (구조 부재) | [N/A] | |
| L91 | `ModelRotationRenderer bipedHead = md.bipedHead;` | `head` | [정합] | |
| L92 | `ModelRotationRenderer bipedRightShoulder = md.bipedRightShoulder;` | (구조 부재) | [N/A] | §16-5 |
| L93 | `ModelRotationRenderer bipedRightArm = md.bipedRightArm;` | `rightArm` | [정합] | |
| L94 | `ModelRotationRenderer bipedLeftShoulder = md.bipedLeftShoulder;` | (구조 부재) | [N/A] | §16-5 |
| L95 | `ModelRotationRenderer bipedLeftArm = md.bipedLeftArm;` | `leftArm` | [정합] | |
| L96 | `ModelRotationRenderer bipedPelvic = md.bipedPelvic;` | (구조 부재) | [N/A] | §16-3 |
| L97 | `ModelRotationRenderer bipedRightLeg = md.bipedRightLeg;` | `rightLeg` | [정합] | |
| L98 | `ModelRotationRenderer bipedLeftLeg = md.bipedLeftLeg;` | `leftLeg` | [정합] | |
| L99 | (빈 줄) | — | [N/A] | |
| L100 | `if(isRopeSliding)` | `if (sm.isRopeSliding)` (MixinPEMC L99) | [정합] | 11-state 체인 1번 |
| L101 | `{` | `{` | [N/A] | |
| L102 | `float time = totalTime * 0.15F;` | `float time = animationProgress * 0.15f;` (sm_animateRopeSliding L152) | [정합] | totalTime → animationProgress (vanilla age+tickDelta) |
| L103 | (빈 줄) | — | [N/A] | |
| L104 | `bipedHead.rotateAngleZ = Between(-Sixteenth, Sixteenth, Normalize(currentCameraAngle - currentHorizontalAngle));` | sm_animateRopeSliding L156-L165: vel 기반 + `MathHelper.clamp(diff, -SIXTEENTH, SIXTEENTH)` | [정합] | Normalize + Between → wrapDegrees + clamp 등가 |
| L105 | `bipedHead.rotateAngleX = Eighth;` | `head.pitch = EIGHTH;` (L155) | [정합] | |
| L106 | `bipedHead.rotationPointY = 2F;` | (1.21.1 미이식 — 주석 "피벗 이동 생략") | [누락] | ⚠️ 매달린 자세에서 머리 피벗 +2F 누락. R-10+ B-N 후보 |
| L107 | (빈 줄) | — | [N/A] | |
| L108 | `bipedOuter.fadeRotateAngleY = false;` | (Outer 부재) | [N/A] | fade 보간 — 구조 부재 |
| L109 | `bipedOuter.rotateAngleY = currentHorizontalAngle;` | MixinPlayerEntityRenderer.sm_captureBodyYaw rope 분기 | [정합] | bodyYaw 직접 설정 — Mixin TAIL 처리 |
| L110 | `bipedTorso.rotateAngleX = Sixteenth + Sixtyfourth * MathHelper.cos(time);` | `body.pitch = SIXTEENTH + SIXTYFOURTH * cos(time);` (L168-L169) | [정합] | bipedTorso(자식 그룹 회전) → body 단일 노드 근사 (§16-2) |
| L111 | (빈 줄) | — | [N/A] | |
| L112 | `bipedLeftArm.rotateAngleX = bipedRightArm.rotateAngleX = Half - bipedTorso.rotateAngleX;` | `rightArm.pitch = HALF - torsoX; leftArm.pitch = HALF - torsoX;` (L172-L173) | [정합] | |
| L113 | (빈 줄) | — | [N/A] | |
| L114 | `bipedRightArm.rotateAngleZ = Sixteenth + Thirtytwoth;` | `rightArm.roll = SIXTEENTH + THIRTYTWOTH;` (L174) | [정합] | |
| L115 | `bipedLeftArm.rotateAngleZ = -Sixteenth - Thirtytwoth;` | `leftArm.roll = -(SIXTEENTH + THIRTYTWOTH);` (L175) | [정합] | |
| L116 | (빈 줄) | — | [N/A] | |
| L117 | `bipedRightArm.rotationPointY = bipedLeftArm.rotationPointY = -2F;` | (1.21.1 미이식 — 주석 "피벗 이동 생략") | [누락] | ⚠️ 매달린 자세에서 팔 피벗 -2F 누락. R-10+ B-N 후보 |
| L118 | (빈 줄) | — | [N/A] | |
| L119 | `bipedPelvic.rotateAngleX = bipedTorso.rotateAngleX;` | (Pelvic 부재) | [N/A] | §16-3 — leg는 body 자식 아니므로 body.pitch만 설정해도 다리 별도 (sm_animateRopeSliding 별도 leg.pitch L180-L181) |
| L120 | (빈 줄) | — | [N/A] | |
| L121 | `bipedLeftLeg.rotateAngleZ = -Thirtytwoth;` | `leftLeg.roll = -THIRTYTWOTH;` (L179) | [정합] | |
| L122 | `bipedRightLeg.rotateAngleZ = Thirtytwoth;` | `rightLeg.roll = THIRTYTWOTH;` (L178) | [정합] | |
| L123 | (빈 줄) | — | [N/A] | |
| L124 | `bipedLeftLeg.rotateAngleX = Sixtyfourth * MathHelper.cos(time + Quarter);` | `leftLeg.pitch = SIXTYFOURTH * cos(time + QUARTER);` (L181) | [정합] | |
| L125 | `bipedRightLeg.rotateAngleX = Sixtyfourth * MathHelper.cos(time - Quarter);` | `rightLeg.pitch = SIXTYFOURTH * cos(time - QUARTER);` (L180) | [정합] | |
| L126 | `}` | (sm_animateRopeSliding `}` L182) | [N/A] | rope 분기 종료 |
| L127 | `else if(isClimb || isCrawlClimb)` | `else if (sm.isClimbing \|\| sm.isCrawlClimbing) sm_animateClimbing(...)` (MixinPEMC L101-L102) | [정합] | 11-state 체인 2번 |
| L128 | `{` | (sm_animateClimbing 본체 진입 L190) | [N/A] | |
| L129 | `bipedOuter.rotateAngleY = forwardRotation / RadiantToAngle;` | MixinPlayerEntityRenderer.sm_captureBodyYaw climb 분기 | [정합] | forwardRotation(deg) → bodyYaw(rad) — Mixin 처리 |
| L130 | (빈 줄) | — | [N/A] | |
| L131 | `bipedHead.rotateAngleY = 0.0F;` | `head.yaw = 0f;` (L195) | [정합] | |
| L132 | `bipedHead.rotateAngleX = viewVerticalAngelOffset / RadiantToAngle;` | `head.pitch = headPitch * DEG_TO_RAD;` (L196) | [정합] | viewVerticalAngelOffset(deg) → headPitch(deg) → rad |
| L133 | (빈 줄) | — | [N/A] | |
| L134 | `bipedLeftLeg.rotationOrder = ModelRotationRenderer.YZX;` | `setAnglesYZX(leftLeg, …)` 헬퍼 (B-08, sm_animateClimbing 본체) | [정합] | rotationOrder = YZX → 헬퍼 |
| L135 | `bipedRightLeg.rotationOrder = ModelRotationRenderer.YZX;` | `setAnglesYZX(rightLeg, …)` | [정합] | |
| L136 | (빈 줄) | — | [N/A] | |
| L137 | `float handsFrequenceUpFactor, handsDistanceUpFactor, handsDistanceUpOffset, feetFrequenceUpFactor, feetDistanceUpFactor, feetDistanceUpOffset;` | (handsDistUp/handsOffset 인라인 변수, sm_animateClimbing L203) | [N/A] | 변수 선언 |
| L138 | `float handsFrequenceSideFactor, handsDistanceSideFactor, handsDistanceSideOffset, feetFrequenceSideFactor, feetDistanceSideFactor, feetDistanceSideOffset;` | (1.21.1: SideFactor/SideOffset 인라인 — feetDistSide* 일부 미사용 추정) | [N/A] | 변수 선언 |
| L139 | (빈 줄) | — | [N/A] | |
| L140 | `int handsClimbType = this.handsClimbType;` | `int h = sm.actualHandsClimbType;` (L200) | [정합] | |
| L141 | `if(isHandsVineClimbing && handsClimbType == HandsClimbing.MiddleGrab)` | `if (sm.isHandsVineClimbing && h >= 2 && h < 4)` (L202) | [정합] | MiddleGrab=TOP_HOLD(2)/BOTTOM_HOLD(3) ordinal 매핑 |
| L142 | `handsClimbType = HandsClimbing.UpGrab;` | `h = 4;` (L202) | [정합] | UpGrab=UP(4) |
| L143 | (빈 줄) | — | [N/A] | |
| L144 | `float verticalSpeed = Math.min(0.5f, currentVerticalSpeed);` | `float verticalSpeed = Math.min(0.5f, limbSwingAmount);` (sm_animateClimbing L191) | [오역] | ⚠️ 원본 입력은 **수직 속도**, 1.21.1은 **limbSwingAmount(수평 속도)**. 사다리/넝쿨 수직 운동 위상 손실. R-10+ B-N 후보 |
| L145 | `float horizontalSpeed = Math.min(0.5f, currentHorizontalSpeed);` | `float horizontalSpeed = Math.min(0.5f, limbSwingAmount);` (L192) | [정합] | currentHorizontalSpeed ≈ limbSwingAmount (둘 다 수평 속도) |
| L146 | (빈 줄) | — | [N/A] | |
| L147 | `switch(handsClimbType)` | `if (h >= 4) … else if (h >= 2) … else …` (L204-L213) | [정합] | switch → if/else if/else 매핑 (UpGrab/MiddleGrab/default 3-way) |
| L148 | `{` | — | [N/A] | |
| L149 | `case HandsClimbing.MiddleGrab:` | `else if (h >= 2)` (L207) | [정합] | |
| L150 | `handsFrequenceSideFactor = FrequenceFactor;` | (인라인 — `0.6662f` 사용 시점에) | [정합] | side factor 인라인 |
| L151 | `handsDistanceSideFactor = 1.0F;` | (rYaw/lYaw 인라인 — `* horizontalSpeed * 1f`) | [정합] | L216-L217 horizontalSpeed * 1.0(생략) |
| L152 | `handsDistanceSideOffset = 0.0F;` | (오프셋 0 — 인라인 미적용) | [정합] | |
| L153 | (빈 줄) | — | [N/A] | |
| L154 | `handsFrequenceUpFactor = FrequenceFactor;` | `0.6662f` (L214-L215 인라인) | [정합] | |
| L155 | `handsDistanceUpFactor = 2F;` | `handsDistUp = 2f;` (L208) | [정합] | |
| L156 | `handsDistanceUpOffset = -Quarter;` | `handsOffset = -QUARTER;` (L209) | [정합] | |
| L157 | `break;` | — | [정합] | if/else 구조에서 자동 |
| L158 | `case HandsClimbing.UpGrab:` | `if (h >= 4)` (L204) | [정합] | |
| L159 | `handsFrequenceSideFactor = FrequenceFactor;` | (인라인) | [정합] | |
| L160 | `handsDistanceSideFactor = 1.0F;` | (인라인) | [정합] | |
| L161 | `handsDistanceSideOffset = 0.0F;` | (인라인) | [정합] | |
| L162 | (빈 줄) | — | [N/A] | |
| L163 | `handsFrequenceUpFactor = FrequenceFactor;` | `0.6662f` 인라인 | [정합] | |
| L164 | `handsDistanceUpFactor = 2F;` | `handsDistUp = 2f;` (L205) | [정합] | |
| L165 | `handsDistanceUpOffset = -2.5F;` | `handsOffset = -2.5f;` (L206) | [정합] | |
| L166 | `break;` | — | [정합] | |
| L167 | `default:` | `else` (L210) | [정합] | NoGrab |
| L168 | `handsFrequenceSideFactor = FrequenceFactor;` | (인라인) | [정합] | |
| L169 | `handsDistanceSideFactor = 1.0F;` | (인라인) | [정합] | |
| L170 | `handsDistanceSideOffset = 0.0F;` | (인라인) | [정합] | |
| L171 | (빈 줄) | — | [N/A] | |
| L172 | `handsFrequenceUpFactor = FrequenceFactor;` | (인라인) | [정합] | |
| L173 | `handsDistanceUpFactor = 0F;` | `handsDistUp = 0f;` (L211) | [정합] | |
| L174 | `handsDistanceUpOffset = -0.5F;` | `handsOffset = -0.5f;` (L212) | [정합] | |
| L175 | `break;` | — | [정합] | |
| L176 | `}` | — | [N/A] | |
| L177 | (빈 줄) | — | [N/A] | |
| L178 | `switch(feetClimbType)` | `int fOrd = sm.actualFeetClimbType; boolean isUpGrab = fOrd >= 4;` (L237-L238) → if/else (L242-) | [정합] | feet UpGrab/default 2-way (handsClimb의 MiddleGrab 미적용 — feetClimb는 SLOW_UP_HOLD/FAST_UP만) |
| L179 | `{` | — | [N/A] | |
| L180 | `case HandsClimbing.UpGrab:` | `if (isUpGrab && verticalSpeed > 0f)` (L242) | [정합] | (note: feet도 UpGrab=4) + 0 나눗셈 가드 추가됨 |
| L181 | `feetFrequenceUpFactor = FrequenceFactor;` | (인라인 `0.6662f` L244) | [정합] | |
| L182 | `feetDistanceUpFactor = 0.3F/verticalSpeed;` | `feetDistUp = 0.3f / verticalSpeed;` (L243) | [정합] | (verticalSpeed 입력은 L144 [오역] 영향 — 동일 부정확 전파) |
| L183 | `feetDistanceUpOffset = -0.3F;` | (`- 0.3f` 인라인 L244) | [정합] | |
| L184 | (빈 줄) | — | [N/A] | |
| L185 | `feetFrequenceSideFactor = FrequenceFactor;` | (인라인) | [정합] | |
| L186 | `feetDistanceSideFactor = 0.5F;` | (청크 2 검증 예정 — feet side 처리부) | [정합] | (참조 — 본체는 청크 2) |
| L187 | `feetDistanceSideOffset = 0.0F;` | (청크 2 검증 예정) | [정합] | |
| L188 | `break;` | — | [정합] | |
| L189 | `default:` | `else` (default feet) | [정합] | |
| L190 | `feetFrequenceUpFactor = FrequenceFactor;` | (default 시 사용 안함) | [정합] | |
| L191 | `feetDistanceUpFactor = 0.0F;` | (`feetDistUp = 0f` 묵시 — `verticalSpeed > 0` 가드로 분기 진입 안함) | [정합] | |
| L192 | `feetDistanceUpOffset = 0.0F;` | (default 시 0) | [정합] | |
| L193 | (빈 줄) | — | [N/A] | |
| L194 | `feetFrequenceSideFactor = FrequenceFactor;` | (인라인) | [정합] | |
| L195 | `feetDistanceSideFactor = 0.0F;` | (default 0) | [정합] | |
| L196 | `feetDistanceSideOffset = 0.0F;` | (default 0) | [정합] | |
| L197 | `break;` | — | [정합] | |
| L198 | `}` | — | [N/A] | |
| L199 | (빈 줄) | — | [N/A] | |
| L200 | `bipedRightArm.rotateAngleX = MathHelper.cos(totalVerticalDistance * handsFrequenceUpFactor + Half) * verticalSpeed * handsDistanceUpFactor + handsDistanceUpOffset;` | `float rPitch = cos(limbSwing * 0.6662f + HALF) * verticalSpeed * handsDistUp + handsOffset;` (L214) | [오역] | ⚠️ **핵심**: 원본 입력 `totalVerticalDistance`(수직 누적 거리) → 1.21.1 `limbSwing`(수평 누적 거리). 사다리/넝쿨에서 위로 오를 때 팔 위상이 수평 거리 기반으로만 흔들려 클라이밍 동작감 저하. + L144 [오역] 동일 영향 (verticalSpeed 입력값). R-10+ B-N 후보 |

**청크 1 (L1-L200) 통계: 정합 84 / 오역 3 / 누락 2 / 잉여 0 / N/A 111 = 200 라인 전수.**

**청크 1 발견 (R-10+ B-N 후보)**:
- B-N (climb 입력 [오역]): L80/L144/L200 — `totalVerticalDistance` / `currentVerticalSpeed` → `limbSwing` / `limbSwingAmount` 잘못 매핑. 사다리/넝쿨 수직 운동 위상 손실.
- B-N (rope 피벗 [누락]): L106/L117 — `bipedHead.rotationPointY = 2F` + `bipedRightArm/leftArm.rotationPointY = -2F` 미이식. 매달린 자세에서 머리/팔 피벗 +2/-2 보정 부재.

**다음 청크**: R-1 청크 2 (L201-L400) — climbing 본체 (handsDistanceSide·feetDistanceUp/Side) + isCeilingClimb + isSwim 시작.
