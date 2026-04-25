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

### 청크 2 (L201-L400) — climbing 본체 잔여 + isClimbJump + isCeilingClimb + isSwim + isDive + isCrawl 시작

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L201 | `bipedLeftArm.rotateAngleX = cos(totalVerticalDistance * handsFrequenceUpFactor) * verticalSpeed * handsDistanceUpFactor + handsDistanceUpOffset;` | sm_animateClimbing L215: `lPitch = cos(limbSwing * 0.6662f) * verticalSpeed * handsDistUp + handsOffset` | [오역] | ⚠️ 청크 1 L200과 동일 패턴 — totalVerticalDistance → limbSwing |
| L202 | (빈 줄) | — | [N/A] | |
| L203 | `bipedRightArm.rotateAngleY = cos(totalHorizontalDistance * handsFrequenceSideFactor + Quarter) * horizontalSpeed * handsDistanceSideFactor + handsDistanceSideOffset;` | L216: `rYaw = cos(limbSwing * 0.6662f + QUARTER) * horizontalSpeed` | [정합] | totalHorizontalDistance ≈ limbSwing (수평) / Side factor=1.0 묵시 / Side offset=0 묵시 |
| L204 | `bipedLeftArm.rotateAngleY = cos(totalHorizontalDistance * handsFrequenceSideFactor) * horizontalSpeed * handsDistanceSideFactor + handsDistanceSideOffset;` | L217: `lYaw = cos(limbSwing * 0.6662f) * horizontalSpeed` | [정합] | |
| L205 | (빈 줄) | — | [N/A] | |
| L206 | `if(isHandsVineClimbing)` | L221: `if (sm.isHandsVineClimbing)` | [정합] | |
| L207 | `{` | — | [N/A] | |
| L208 | `bipedLeftArm.rotateAngleY *= 1F + handsFrequenceSideFactor;` | L223 통합: `leftArm.yaw = leftArm.yaw * (1f + 0.6662f) + EIGHTH` | [정합] | 곱셈+오프셋 한 줄 통합 (등가) |
| L209 | `bipedRightArm.rotateAngleY *= 1F + handsFrequenceSideFactor;` | L222 통합: `rightArm.yaw = rightArm.yaw * (1f + 0.6662f) - EIGHTH` | [정합] | 동일 |
| L210 | (빈 줄) | — | [N/A] | |
| L211 | `bipedLeftArm.rotateAngleY += Eighth;` | L223 통합 | [정합] | |
| L212 | `bipedRightArm.rotateAngleY -= Eighth;` | L222 통합 | [정합] | |
| L213 | (빈 줄) | — | [N/A] | |
| L214 | `setArmScales(abs(cos(rightArm.X)), abs(cos(leftArm.X)));` | L225-L227: `setArmScales(rightArm, leftArm, abs(cos(rightArm.pitch)), abs(cos(leftArm.pitch)))` | [정합] | B-3 세션 2 완결 |
| L215 | `}` | — | [N/A] | |
| L216 | (빈 줄) | — | [N/A] | |
| L217 | `if(!isFeetVineClimbing)` | L241: `if (!sm.isFeetVineClimbing)` | [정합] | |
| L218 | `{` | — | [N/A] | |
| L219 | `bipedRightLeg.rotateAngleX = cos(totalVerticalDistance * feetFrequenceUpFactor) * feetDistanceUpFactor * verticalSpeed + feetDistanceUpOffset;` | L244: `cos(limbSwing * 0.6662f) * feetDistUp * verticalSpeed - 0.3f` | [오역] | ⚠️ totalVerticalDistance → limbSwing. feetDistUp=0.3/verticalSpeed → 진폭 0.3 일정 (verticalSpeed 약분), offset=-0.3 정합 |
| L220 | `bipedLeftLeg.rotateAngleX = cos(totalVerticalDistance * feetFrequenceUpFactor + Half) * feetDistanceUpFactor * verticalSpeed + feetDistanceUpOffset;` | L245: `cos(limbSwing * 0.6662f + HALF) * feetDistUp * verticalSpeed - 0.3f` | [오역] | ⚠️ 동일 |
| L221 | `}` | — | [N/A] | |
| L222 | (빈 줄) | — | [N/A] | |
| L223 | `bipedRightLeg.rotateAngleZ = -(cos(totalHorizontalDistance * feetFrequenceSideFactor) - 1.0F) * horizontalSpeed * feetDistanceSideFactor + feetDistanceSideOffset;` | L255: `-(cos(limbSwing * 0.6662f) - 1f) * horizontalSpeed * feetDistSideFactor` | [정합] | feetDistSideFactor = isUpGrab ? 0.5f : 0f |
| L224 | `bipedLeftLeg.rotateAngleZ = -(cos(totalHorizontalDistance * feetFrequenceSideFactor + Quarter) + 1.0F) * horizontalSpeed * feetDistanceSideFactor + feetDistanceSideOffset;` | L256 | [정합] | |
| L225 | (빈 줄) | — | [N/A] | |
| L226 | `if(isFeetVineClimbing)` | L259: `if (sm.isFeetVineClimbing)` | [정합] | |
| L227 | `{` | — | [N/A] | |
| L228 | `float total = (cos(totalDistance + Half) + 1) * Thirtytwoth + Sixteenth;` | L260: `float total = (cos(limbSwing + HALF) + 1f) * THIRTYTWOTH + SIXTEENTH` | [오역] | ⚠️ `totalDistance` ≠ `limbSwing` — totalDistance는 SmartRenderModel.md L80 별도 변수 (수평+수직 누적). cos 인자 위상 차이 |
| L229 | `bipedRightLeg.rotateAngleX = -total;` | L261 | [정합] | |
| L230 | `bipedLeftLeg.rotateAngleX = -total;` | L262 | [정합] | |
| L231 | (빈 줄) | — | [N/A] | |
| L232 | `float difference = max(0, cos(totalDistance - Quarter)) * Sixtyfourth;` | L264: `max(0f, cos(limbSwing - QUARTER)) * SIXTYFOURTH` | [오역] | ⚠️ totalDistance → limbSwing |
| L233 | `bipedLeftLeg.rotateAngleZ += -difference;` | L265 | [정합] | |
| L234 | `bipedRightLeg.rotateAngleZ += difference;` | L266 | [정합] | |
| L235 | (빈 줄) | — | [N/A] | |
| L236 | `setLegScales(abs(cos(rightLeg.X)), abs(cos(leftLeg.X)));` | L269-L271 | [정합] | B-3 세션 2 완결 |
| L237 | `}` | — | [N/A] | |
| L238 | (빈 줄) | — | [N/A] | |
| L239 | `if(isCrawlClimb)` | L278: `if (sm.isCrawlClimbing)` | [정합] | |
| L240 | `{` | — | [N/A] | |
| L241 | `float height = smallOverGroundHeight + 0.25F;` | L279 | [정합] | smallOverGroundHeight: MixinPEMC L94-L96 capture |
| L242 | `float bodyLength = 0.7F;` | L280 | [정합] | |
| L243 | `float legLength = 0.55F;` | L280 | [정합] | |
| L244 | (빈 줄) | — | [N/A] | |
| L245 | `float bodyAngleX, legAngleX, legAngleZ;` | L281 | [정합] | |
| L246 | `if(height < bodyLength)` | L282 | [정합] | |
| L247 | `{` | — | [N/A] | |
| L248 | `bodyAngleX = max(0, acos(height / bodyLength));` | L283 | [정합] | |
| L249 | `legAngleX = Quarter - bodyAngleX;` | L284 | [정합] | |
| L250 | `legAngleZ = Thirtytwoth;` | L285 | [정합] | |
| L251 | `}` | — | [N/A] | |
| L252 | `else if(height < bodyLength + legLength)` | L286 | [정합] | |
| L253 | `{` | — | [N/A] | |
| L254 | `bodyAngleX = 0F;` | L287 | [정합] | |
| L255 | `legAngleX = max(0, acos((height - bodyLength) / legLength));` | L288 | [정합] | |
| L256 | `legAngleZ = Thirtytwoth * (legAngleX / 1.537F);` | L289 | [정합] | |
| L257 | `}` | — | [N/A] | |
| L258 | `else` | L290 | [정합] | |
| L259 | `{` | — | [N/A] | |
| L260 | `bodyAngleX = 0F;` | L291 | [정합] | |
| L261 | `legAngleX = 0F;` | L292 | [정합] | |
| L262 | `legAngleZ = 0F;` | L293 | [정합] | |
| L263 | `}` | — | [N/A] | |
| L264 | (빈 줄) | — | [N/A] | |
| L265 | `bipedTorso.rotateAngleX = bodyAngleX;` | L295: `body.pitch = bodyAngleX` | [정합] | bipedTorso → body 단일 노드 근사 (§16-2) |
| L266 | (빈 줄) | — | [N/A] | |
| L267 | `bipedRightShoulder.rotateAngleX = -bodyAngleX;` | (Shoulder 부재) | [N/A] | §16-5 — 구조 부재 |
| L268 | `bipedLeftShoulder.rotateAngleX = -bodyAngleX;` | (Shoulder 부재) | [N/A] | §16-5 |
| L269 | (빈 줄) | — | [N/A] | |
| L270 | `bipedHead.rotateAngleX = -bodyAngleX;` | L296: `head.pitch = -bodyAngleX` | [정합] | |
| L271 | (빈 줄) | — | [N/A] | |
| L272 | `bipedRightLeg.rotateAngleX = legAngleX;` | L297 | [정합] | |
| L273 | `bipedLeftLeg.rotateAngleX = legAngleX;` | L298 | [정합] | |
| L274 | (빈 줄) | — | [N/A] | |
| L275 | `bipedRightLeg.rotateAngleZ = legAngleZ;` | L299 | [정합] | |
| L276 | `bipedLeftLeg.rotateAngleZ = -legAngleZ;` | L300 | [정합] | |
| L277 | `}` | — | [N/A] | |
| L278 | (빈 줄) | — | [N/A] | |
| L279 | `if(handsClimbType == HandsClimbing.NoGrab && feetClimbType != FeetClimbing.NoStep)` | L305: `if (sm.actualHandsClimbType < 2 && sm.actualFeetClimbType > 0)` | [정합] | NoGrab=ordinal{0,1}<2 / NoStep=0이므로 >0 |
| L280 | `{` | — | [N/A] | |
| L281 | `bipedTorso.rotateAngleX = 0.5F;` | L306: `body.pitch = 0.5f` | [정합] | bipedTorso → body 근사 |
| L282 | `bipedHead.rotateAngleX -= 0.5F;` | L307: `head.pitch -= 0.5f` | [정합] | |
| L283 | `bipedPelvic.rotateAngleX -= 0.5F;` | (Pelvic 부재) | [N/A] | §16-3 |
| L284 | (빈 줄) | — | [N/A] | |
| L285 | `bipedTorso.rotationPointZ = -6.0F;` | (1.21.1 미이식 — sm_animateClimbing L304 주석 명시) | [누락] | ⚠️ body.pivotZ = -6F 가능 (ModelPart.pivotZ public field). R-10+ B-N 후보 |
| L286 | `}` | — | [N/A] | |
| L287 | `}` (isClimb 분기 종료) | — | [N/A] | |
| L288 | `else if(isClimbJump)` | L103: `else if (sm.isClimbJumping)` | [정합] | |
| L289 | `{` | — | [N/A] | |
| L290 | `bipedRightArm.rotateAngleX = Half + Sixteenth;` | L105: `rightArm.pitch = HALF + SIXTEENTH` | [정합] | |
| L291 | `bipedLeftArm.rotateAngleX = Half + Sixteenth;` | L106 | [정합] | |
| L292 | (빈 줄) | — | [N/A] | |
| L293 | `bipedRightArm.rotateAngleZ = -Thirtytwoth;` | L107: `rightArm.roll = -THIRTYTWOTH` | [정합] | |
| L294 | `bipedLeftArm.rotateAngleZ = Thirtytwoth;` | L108 | [정합] | |
| L295 | `}` | — | [N/A] | |
| L296 | `else if(isCeilingClimb)` | L109: `else if (sm.isCeilingClimbing)` → sm_animateCeilingClimbing | [정합] | |
| L297 | `{` | — | [N/A] | |
| L298 | `float distance = totalHorizontalDistance * 0.7F;` | L317: `float distance = limbSwing * 0.7f` | [정합] | totalHorizontalDistance ≈ limbSwing |
| L299 | `float walkFactor = Factor(currentHorizontalSpeed, 0F, 0.12951545F);` | L318: `smFactor(limbSwingAmount, 0f, 0.12951545f)` | [정합] | currentHorizontalSpeed ≈ limbSwingAmount |
| L300 | `float standFactor = Factor(currentHorizontalSpeed, 0.12951545F, 0F);` | L319 | [정합] | |
| L301 | `float horizontalAngle = horizontalDistance < 0.015F ? currentCameraAngle : currentHorizontalAngle;` | MixinPlayerEntityRenderer.sm_captureBodyYaw ceilingClimb 분기 (threshold 0.015F) | [정합] | Mixin 분리 처리 |
| L302 | (빈 줄) | — | [N/A] | |
| L303 | `bipedLeftArm.rotateAngleX = (cos(distance) * 0.52F + Half) * walkFactor + Half * standFactor;` | L322 | [정합] | |
| L304 | `bipedRightArm.rotateAngleX = (cos(distance + Half) * 0.52F - Half) * walkFactor - Half * standFactor;` | L323 | [정합] | |
| L305 | (빈 줄) | — | [N/A] | |
| L306 | `bipedLeftLeg.rotateAngleX = -cos(distance) * 0.12F * walkFactor;` | L324 | [정합] | |
| L307 | `bipedRightLeg.rotateAngleX = -cos(distance + Half) * 0.32F * walkFactor;` | L325 | [정합] | 좌우 계수 비대칭 (0.12 vs 0.32) 보존 |
| L308 | (빈 줄) | — | [N/A] | |
| L309 | `float rotateY = cos(distance) * 0.44F * walkFactor;` | L327 | [정합] | |
| L310 | `bipedOuter.rotateAngleY = rotateY + horizontalAngle;` | MixinPlayerEntityRenderer.sm_captureBodyYaw ceilingClimb 분기 (rotateY + horizontalAngle 합성) | [정합] | Mixin 분리 |
| L311 | (빈 줄) | — | [N/A] | |
| L312 | `bipedRightArm.rotateAngleY = bipedLeftArm.rotateAngleY = -rotateY;` | L328: `rightArm.yaw = leftArm.yaw = -rotateY` | [정합] | |
| L313 | `bipedRightLeg.rotateAngleY = bipedLeftLeg.rotateAngleY = -rotateY;` | L329 | [정합] | |
| L314 | (빈 줄) | — | [N/A] | |
| L315 | `bipedHead.rotateAngleY = -rotateY;` | L330: `head.yaw = -rotateY;` | [정합] | ⚠️ 1.21.1 L332 추가 보정 `head.yaw -= headYaw * DEG_TO_RAD` 는 [잉여] — 원본 미존재 (sm_setAngles TAIL inject 시점에 vanilla setAngles가 이미 head.yaw 설정 → 추가 차감은 의도되지 않음). R-10+ 검토 |
| L316 | `}` | — | [N/A] | |
| L317 | `else if(isSwim)` | L111: `else if (sm.isSwimming_sm)` → sm_animateSwimming | [정합] | |
| L318 | `{` | — | [N/A] | |
| L319 | `float distance = totalHorizontalDistance;` | (sm_animateSwimming L354에서 limbSwing 직접 사용) | [정합] | |
| L320 | `float walkFactor = Factor(currentHorizontalSpeed, 0.15679921F, 0.52264464F);` | L341 | [정합] | |
| L321 | `float sneakFactor = min(Factor(.., 0, 0.15679921F), Factor(.., 0.52264464F, 0.15679921F));` | L342-L344 | [정합] | |
| L322 | `float standFactor = Factor(currentHorizontalSpeed, 0.15679921F, 0F);` | L345 | [정합] | |
| L323 | `float standSneakFactor = standFactor + sneakFactor;` | L346 + sm.swimStandSneakFactor capture | [정합] | sm_setupTransforms 에 전달 |
| L324 | `float horizontalAngle = horizontalDistance < (isGenericSneaking ? 0.005 : 0.015F) ? currentCameraAngle : currentHorizontalAngle;` | MixinPlayerEntityRenderer.sm_captureBodyYaw swim 분기 (threshold 0.005/0.015 isGenericSneaking) | [정합] | Mixin 분리 |
| L325 | (빈 줄) | — | [N/A] | |
| L326 | `bipedHead.rotationOrder = ModelRotationRenderer.YXZ;` | L352 setAnglesYXZ 헬퍼 (B-1 세션 2) | [정합] | |
| L327 | `bipedHead.rotateAngleY = cos(distance / 2.0F - Quarter) * walkFactor;` | L354 (Y 인자) | [정합] | |
| L328 | `bipedHead.rotateAngleX = -Eighth * standSneakFactor;` | L353 (X 인자) | [정합] | |
| L329 | `bipedHead.rotationPointZ = -2F;` | (1.21.1 미이식) | [누락] | ⚠️ head.pivotZ = -2F 미적용. R-10+ B-N 후보 |
| L330 | (빈 줄) | — | [N/A] | |
| L331 | `bipedOuter.fadeRotateAngleX = true;` | (Outer 부재 + fade 메커니즘 부재) | [N/A] | §17 잔여 — fade 보간 |
| L332 | `bipedOuter.rotateAngleX = Quarter - Sixteenth * standSneakFactor;` | MixinPlayerEntityRenderer.sm_setupTransforms swim 분기 (matrices.multiply POSITIVE_X) | [정합] | Mixin 분리 — sm.swimStandSneakFactor 활용 |
| L333 | `bipedOuter.rotateAngleY = horizontalAngle;` | sm_captureBodyYaw swim 분기 | [정합] | Mixin 분리 |
| L334 | (빈 줄) | — | [N/A] | |
| L335 | `bipedBreast.rotateAngleY = bipedBody.rotateAngleY = cos(distance / 2.0F - Quarter) * walkFactor;` | (sm_animateSwimming 본체에 body.yaw 설정 부재) | [누락] | ⚠️ Breast 부재이지만 `bipedBody.rotateAngleY` 미이식. body.yaw = cos(limbSwing/2 - QUARTER) * walkFactor 가능. R-10+ B-N 후보 |
| L336 | (빈 줄) | — | [N/A] | |
| L337 | `bipedRightArm.rotationOrder = ModelRotationRenderer.YZX;` | L363 setAnglesYZX (B-08) | [정합] | |
| L338 | `bipedLeftArm.rotationOrder = ModelRotationRenderer.YZX;` | L364 setAnglesYZX | [정합] | |
| L339 | (빈 줄) | — | [N/A] | |
| L340 | `bipedRightArm.rotateAngleZ = Quarter + Eighth + cos(totalTime * 0.1F) * standSneakFactor * 0.8F;` | L361 rightRoll (Z 인자) | [정합] | totalTime → animationProgress |
| L341 | `bipedLeftArm.rotateAngleZ = -Quarter - Eighth - cos(totalTime * 0.1F) * standSneakFactor * 0.8F;` | L362 leftRoll | [정합] | |
| L342 | (빈 줄) | — | [N/A] | |
| L343 | `bipedRightArm.rotateAngleX = ((distance * 0.5F) % Whole - Half) * walkFactor + Sixteenth * standSneakFactor;` | L359 rightPitch (X 인자) | [정합] | dist2 = limbSwing * 0.5f |
| L344 | `bipedLeftArm.rotateAngleX = ((distance * 0.5F + Half) % Whole - Half) * walkFactor + Sixteenth * standSneakFactor;` | L360 leftPitch | [정합] | |
| L345 | (빈 줄) | — | [N/A] | |
| L346 | `bipedRightLeg.rotateAngleX = cos(distance) * 0.52264464F * walkFactor;` | L367 | [정합] | |
| L347 | `bipedLeftLeg.rotateAngleX = cos(distance + Half) * 0.52264464F * walkFactor;` | L368 | [정합] | |
| L348 | (빈 줄) | — | [N/A] | |
| L349 | `float rotateFeetAngleZ = Sixteenth * standSneakFactor + cos(totalTime * 0.1F) * 0.4F * (standFactor - sneakFactor);` | L370-L371 | [정합] | |
| L350 | `bipedRightLeg.rotateAngleZ = rotateFeetAngleZ;` | L372 | [정합] | |
| L351 | `bipedLeftLeg.rotateAngleZ = -rotateFeetAngleZ;` | L373 | [정합] | |
| L352 | (빈 줄) | — | [N/A] | |
| L353 | `if(scaleLegType != NoScaleStart)` | (1.21.1: 메인 모델 = Scale 이므로 가드 항상 true → 가드 제거 = 1:1 동작) | [정합] | 세션 2 분석 — §16-7 |
| L354 | `setLegScales(...)` | L378 | [정합] | |
| L355 | `1F + (cos(totalTime * 0.1F + Quarter) - 1F) * 0.15F * sneakFactor,` | L377 legSc | [정합] | |
| L356 | `1F + (cos(totalTime * 0.1F + Quarter) - 1F) * 0.15F * sneakFactor);` | L378 (좌우 동일) | [정합] | |
| L357 | (빈 줄) | — | [N/A] | |
| L358 | `if(scaleArmType != NoScaleStart)` | (가드 제거 — 메인 모델 = Scale) | [정합] | |
| L359 | `setArmScales(...)` | L380 | [정합] | |
| L360 | `1F + (cos(totalTime * 0.1F - Quarter) - 1F) * 0.15F * sneakFactor,` | L379 armSc | [정합] | |
| L361 | `1F + (cos(totalTime * 0.1F - Quarter) - 1F) * 0.15F * sneakFactor);` | L380 (좌우 동일) | [정합] | |
| L362 | `}` | — | [N/A] | |
| L363 | `else if(isDive)` | L113: `else if (sm.isDiving)` → sm_animateDiving | [정합] | |
| L364 | `{` | — | [N/A] | |
| L365 | `float distance = totalDistance * 0.7F;` | L389: `float distance = limbSwing * 0.7f` | [오역] | ⚠️ totalDistance ≠ limbSwing — totalDistance 는 SmartRenderModel 별도 변수 (수평+수직). 다이빙 위상 계산 입력 잘못 |
| L366 | `float walkFactor = Factor(currentSpeed, 0F, 0.15679921F);` | L390: `smFactor(limbSwingAmount, 0f, 0.15679921f)` | [오역] | ⚠️ currentSpeed = 3D 속도 (md.currentSpeed) ≠ limbSwingAmount = 수평 속도 |
| L367 | `float standFactor = Factor(currentSpeed, 0.15679921F, 0F);` | L391 | [오역] | ⚠️ 동일 |
| L368 | `float horizontalAngle = totalDistance < (isGenericSneaking ? 0.005 : 0.015F) ? currentCameraAngle : currentHorizontalAngle;` | MixinPlayerEntityRenderer.sm_captureBodyYaw dive 분기 | [정합] | Mixin 분리 |
| L369 | (빈 줄) | — | [N/A] | |
| L370 | `bipedHead.rotateAngleX = -Eighth;` | (sm_animateDiving 본체에 head.pitch 설정 부재) | [누락] | ⚠️ head.pitch = -EIGHTH 미이식. R-10+ B-N 후보 |
| L371 | `bipedHead.rotationPointZ = -2F;` | (1.21.1 미이식) | [누락] | ⚠️ head.pivotZ = -2F 미적용. R-10+ B-N 후보 |
| L372 | (빈 줄) | — | [N/A] | |
| L373 | `bipedOuter.fadeRotateAngleX = true;` | (Outer/fade 부재) | [N/A] | |
| L374 | `bipedOuter.rotateAngleX = isLevitate ? Quarter - Sixteenth : (isJump ? 0F : Quarter - currentVerticalAngle);` | MixinPlayerEntityRenderer.sm_setupTransforms dive 3-way | [정합] | Mixin 분리 — Levitate/Jump/일반 |
| L375 | `bipedOuter.rotateAngleY = horizontalAngle;` | sm_captureBodyYaw dive 분기 | [정합] | Mixin 분리 |
| L376 | (빈 줄) | — | [N/A] | |
| L377 | `bipedRightLeg.rotateAngleZ = (cos(distance) + 1F) * 0.52264464F * walkFactor + Sixteenth * standFactor;` | L394 | [정합] | (distance/walkFactor/standFactor 입력은 L365-L367 [오역] 영향 전파) |
| L378 | `bipedLeftLeg.rotateAngleZ = (cos(distance + Half) - 1F) * 0.52264464F * walkFactor - Sixteenth * standFactor;` | L395 | [정합] | |
| L379 | (빈 줄) | — | [N/A] | |
| L380 | `if(scaleLegType != NoScaleStart)` | (가드 제거) | [정합] | |
| L381 | `setLegScales(...)` | L407 | [정합] | |
| L382 | `1F + (cos(distance - Quarter) - 1F) * 0.25F * walkFactor,` | L406 legSc | [정합] | 0.25F 다리 계수 |
| L383 | `1F + (cos(distance - Quarter) - 1F) * 0.25F * walkFactor);` | L407 | [정합] | |
| L384 | (빈 줄) | — | [N/A] | |
| L385 | `bipedRightArm.rotateAngleZ = (cos(distance + Half) * 0.52264464F * 2.5F + Quarter) * walkFactor + (Quarter + Eighth) * standFactor;` | L398-L399 | [정합] | |
| L386 | `bipedLeftArm.rotateAngleZ = (cos(distance) * 0.52264464F * 2.5F - Quarter) * walkFactor - (Quarter + Eighth) * standFactor;` | L400-L401 | [정합] | |
| L387 | (빈 줄) | — | [N/A] | |
| L388 | `if(scaleArmType != NoScaleStart)` | (가드 제거) | [정합] | |
| L389 | `setArmScales(...)` | L409 | [정합] | |
| L390 | `1F + (cos(distance + Quarter) - 1F) * 0.15F * walkFactor,` | L408 armSc | [정합] | 0.15F 팔 계수 (다리와 비대칭) |
| L391 | `1F + (cos(distance + Quarter) - 1F) * 0.15F * walkFactor);` | L409 | [정합] | |
| L392 | `}` | — | [N/A] | |
| L393 | `else if(isCrawl)` | L115: `else if (sm.isCrawling)` → sm_animateCrawling | [정합] | |
| L394 | `{` | — | [N/A] | |
| L395 | `float distance = totalHorizontalDistance * 1.3F;` | L419: `float distance = limbSwing * 1.3f` | [정합] | |
| L396 | `float walkFactor = Factor(currentHorizontalSpeedFlattened, 0F, 0.12951545F);` | L420: `smFactor(limbSwingAmount, 0f, 0.12951545f)` | [정합] | currentHorizontalSpeedFlattened ≈ currentHorizontalSpeed ≈ limbSwingAmount (NaN 아닌 일반 케이스) |
| L397 | `float standFactor = Factor(currentHorizontalSpeedFlattened, 0.12951545F, 0F);` | L421 | [정합] | |
| L398 | (빈 줄) | — | [N/A] | |
| L399 | `bipedHead.rotateAngleZ = -viewHorizontalAngelOffset / RadiantToAngle;` | L424: `head.roll = -headYaw * DEG_TO_RAD` | [정합] | viewHorizontalAngelOffset(deg) → headYaw(deg) → rad |
| L400 | `bipedHead.rotateAngleX = -Eighth;` | L425: `head.pitch = -EIGHTH` | [정합] | |

**청크 2 (L201-L400) 통계: 정합 122 / 오역 8 / 누락 5 / 잉여 0 / N/A 65 = 200 라인 전수.**

(참고: L315 자체는 [정합]으로 분류했으나 1.21.1 sm_animateCeilingClimbing L332 `head.yaw -= headYaw * DEG_TO_RAD` 추가 보정은 원본 미존재 → R-10+ [잉여] 후보로 비고에 별도 명시. 통계는 라인-원본 기준이므로 [잉여] 0건으로 카운트.)

**청크 2 발견 (R-10+ B-N 후보, §16 등재)**:
- B-N (climb arm/leg verticalDistance 패턴 확장): L201/L219/L220 — 청크 1 발견과 동일 패턴 (totalVerticalDistance → limbSwing). 동일 B-N 그룹.
- B-N (FeetVine totalDistance): L228/L232 — `totalDistance` (SmartRenderModel 별도 변수, 수평+수직 누적) → `limbSwing` (수평) 잘못 매핑. 넝쿨 발 흔들림 위상 차이.
- B-N (Dive 입력값): L365/L366/L367 — `totalDistance`/`currentSpeed` → `limbSwing`/`limbSwingAmount` 잘못 매핑. 다이빙 진폭/위상 영향.
- B-N (NoGrab+non-NoStep bipedTorso.rotationPointZ = -6F): L285. body.pivotZ = -6F 미이식. NoGrab 매달림에서 몸 위치 6 픽셀 차이.
- B-N (Swim head pivotZ = -2F): L329. head.pivotZ 미적용.
- B-N (Swim body yaw): L335. `bipedBody.rotateAngleY = cos(distance/2 - Quarter) * walkFactor` 미이식. 수영 중 몸통 좌우 흔들림 부재.
- B-N (Dive head pitch + pivotZ): L370/L371. head.pitch=-EIGHTH + head.pivotZ=-2F 미이식. 다이빙 머리 자세 부재.
- (잉여) sm_animateCeilingClimbing L332 `head.yaw -= headYaw * DEG_TO_RAD` — 원본 L315 미존재 추가 보정. R-10+ 검토.

**다음 청크**: R-1 청크 3 (L401-L600) — isCrawl 본체 잔여 + isJump + isHeadJump + isSlide + isFalling 시작.

### 청크 3 (L401-L600) — isCrawl 본체 잔여 + isSlide + isFlying + isHeadJump + isFalling + else isStandard + isWorking + animateAngleJumping + animateNonStandardWorking + animateNonStandardBowAiming 시작

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L401 | `bipedHead.rotationPointZ = -2F;` (isCrawl 머리 피벗) | (sm_animateCrawling 미이식) | [누락] | ⚠️ head.pivotZ = -2F. R-10+ B-N 후보 |
| L402 | (빈 줄) | — | [N/A] | |
| L403 | `bipedTorso.rotationOrder = ModelRotationRenderer.YZX;` | (1.21.1 body 단일 노드 — rotationOrder 분리 메커니즘 부재) | [N/A] | §16-2 — body는 XYZ 기본. setAnglesYZX(body) 적용 가능 여부는 R-10+ 검토 |
| L404 | `bipedTorso.rotateAngleX = Quarter - Thirtytwoth;` | sm_animateCrawling L428: `body.pitch = QUARTER - THIRTYTWOTH` | [정합] | bipedTorso → body 단일 노드 근사 (§16-2) |
| L405 | `bipedTorso.rotationPointY = 3F;` | (1.21.1 미이식) | [누락] | ⚠️ body.pivotY = 3F 가능. R-10+ B-N 후보 |
| L406 | `bipedTorso.rotateAngleZ = cos(distance + Quarter) * Sixtyfourth * walkFactor;` | L429: `body.roll = ...` | [정합] | |
| L407 | `bipedBody.rotateAngleY = cos(distance + Half) * Sixtyfourth * walkFactor;` | L430: `body.yaw = ...` | [정합] | |
| L408 | (빈 줄) | — | [N/A] | |
| L409 | `bipedRightLeg.rotateAngleX = (cos(distance - Quarter) * Sixtyfourth + Thirtytwoth) * walkFactor + Thirtytwoth * standFactor;` | L433-L434: `rightLeg.pitch` | [정합] | |
| L410 | `bipedLeftLeg.rotateAngleX = (cos(distance - Half - Quarter) * Sixtyfourth + Thirtytwoth) * walkFactor + Thirtytwoth * standFactor;` | L435-L436 | [정합] | |
| L411 | (빈 줄) | — | [N/A] | |
| L412 | `bipedRightLeg.rotateAngleZ = (cos(distance - Quarter) + 1F) * 0.25F * walkFactor + Thirtytwoth * standFactor;` | L437 | [정합] | |
| L413 | `bipedLeftLeg.rotateAngleZ = (cos(distance - Quarter) - 1F) * 0.25F * walkFactor - Thirtytwoth * standFactor;` | L438 | [정합] | |
| L414 | (빈 줄) | — | [N/A] | |
| L415 | `if(scaleLegType != NoScaleStart)` | (가드 제거 — 메인 = Scale) | [정합] | §16-7 |
| L416 | `setLegScales(...)` | L451 | [정합] | |
| L417 | `1F + (cos(distance + Quarter - Quarter) - 1F) * 0.25F * walkFactor,` | L452 | [정합] | 원본 표기 보존 (Quarter-Quarter 단순화 안 함) |
| L418 | `1F + (cos(distance - Quarter - Quarter) - 1F) * 0.25F * walkFactor);` | L453 | [정합] | |
| L419 | (빈 줄) | — | [N/A] | |
| L420 | `bipedRightArm.rotationOrder = ModelRotationRenderer.YZX;` | L445 setAnglesYZX | [정합] | |
| L421 | `bipedLeftArm.rotationOrder = ModelRotationRenderer.YZX;` | L446 setAnglesYZX | [정합] | |
| L422 | (빈 줄) | — | [N/A] | |
| L423 | `bipedRightArm.rotateAngleX = Half + Eighth;` | L445 (X 인자 = HALF+EIGHTH) | [정합] | |
| L424 | `bipedLeftArm.rotateAngleX = Half + Eighth;` | L446 | [정합] | |
| L425 | (빈 줄) | — | [N/A] | |
| L426 | `bipedRightArm.rotateAngleZ = ((cos(distance + Half)) * Sixtyfourth + Thirtytwoth)* walkFactor + Sixteenth * standFactor;` | L441-L442 rRoll (Z 인자) | [정합] | |
| L427 | `bipedLeftArm.rotateAngleZ = ((cos(distance + Half)) * Sixtyfourth - Thirtytwoth) * walkFactor - Sixteenth * standFactor;` | L443-L444 lRoll | [정합] | |
| L428 | (빈 줄) | — | [N/A] | |
| L429 | `bipedRightArm.rotateAngleY = -Quarter;` | L445 (Y 인자 = -QUARTER) | [정합] | |
| L430 | `bipedLeftArm.rotateAngleY = Quarter;` | L446 (Y 인자 = QUARTER) | [정합] | |
| L431 | (빈 줄) | — | [N/A] | |
| L432 | `if(scaleArmType != NoScaleStart)` | (가드 제거) | [정합] | |
| L433 | `setArmScales(...)` | L454 | [정합] | |
| L434 | `1F + (cos(distance + Quarter) - 1F) * 0.15F * walkFactor,` | L455 | [정합] | |
| L435 | `1F + (cos(distance - Quarter) - 1F) * 0.15F * walkFactor);` | L456 | [정합] | |
| L436 | `}` (isCrawl 종료) | — | [N/A] | |
| L437 | `else if(isSlide)` | L117: `else if (sm.isSliding)` → sm_animateSliding | [정합] | |
| L438 | `{` | — | [N/A] | |
| L439 | `float distance = totalHorizontalDistance * 0.7F;` | L466: `limbSwing * 0.7f` | [정합] | |
| L440 | `float walkFactor = Factor(currentHorizontalSpeed, 0F, 1F) * 0.8F;` | L467 | [정합] | |
| L441 | (빈 줄) | — | [N/A] | |
| L442 | `bipedHead.rotateAngleZ = -viewHorizontalAngelOffset / RadiantToAngle;` | (sm_animateSliding 본체 head.roll 미설정) | [누락] | ⚠️ head.roll = -headYaw * DEG_TO_RAD 미이식. R-10+ B-N 후보 |
| L443 | `bipedHead.rotateAngleX = -Eighth - Sixteenth;` | L469: `head.pitch = -EIGHTH - SIXTEENTH` | [정합] | |
| L444 | `bipedHead.rotationPointZ = -2F;` | (1.21.1 미이식) | [누락] | ⚠️ head.pivotZ = -2F. R-10+ B-N 후보 |
| L445 | (빈 줄) | — | [N/A] | |
| L446 | `bipedOuter.fadeRotateAngleY = false;` | (Outer/fade 부재) | [N/A] | |
| L447 | `bipedOuter.rotateAngleY = currentHorizontalAngle;` | sm_captureBodyYaw slide 분기 | [정합] | Mixin 분리 |
| L448 | `bipedOuter.rotationPointY = 5F;` | (1.21.1 미이식) | [누락] | ⚠️ Outer 부재이지만 entity-level translate 가능 (setupTransforms / getPositionOffset 검토). R-10+ B-N 후보 |
| L449 | `bipedOuter.rotateAngleX = Quarter;` | sm_setupTransforms slide 분기 | [정합] | Mixin 분리 (matrices.multiply POSITIVE_X.rotation(QUARTER)) |
| L450 | (빈 줄) | — | [N/A] | |
| L451 | `bipedBody.rotationOrder = ModelRotationRenderer.YXZ;` | L476 setAnglesYXZ (B-2 세션 2) | [정합] | |
| L452 | `bipedBody.offsetY = -0.4F;` | (ModelPart.offsetY 부재) | [누락] | ⚠️ ModelPart 에 offsetY 필드 부재 — MatrixStack translate 보정 필요. §17 잔여 등재 — 갑옷 외 일반 body offsetY 영향. R-10+ 검토 |
| L453 | `bipedBody.rotationPointY = +6.5F;` | (1.21.1 미이식) | [누락] | ⚠️ body.pivotY = 6.5F 가능. R-10+ B-N 후보 |
| L454 | `bipedBody.rotateAngleX = cos(distance - Eighth) * Sixtyfourth * walkFactor;` | L477 setAnglesYXZ X 인자 | [정합] | |
| L455 | `bipedBody.rotateAngleY = cos(distance + Eighth) * Sixtyfourth * walkFactor;` | L478 Y 인자 | [정합] | |
| L456 | (빈 줄) | — | [N/A] | |
| L457 | `bipedRightLeg.rotateAngleX = cos(distance + Half) * Sixtyfourth * walkFactor + Sixtyfourth;` | L482 | [정합] | |
| L458 | `bipedLeftLeg.rotateAngleX = cos(distance + Quarter) * Sixtyfourth * walkFactor + Sixtyfourth;` | L483 | [정합] | |
| L459 | (빈 줄) | — | [N/A] | |
| L460 | `bipedRightLeg.rotateAngleZ = Thirtytwoth;` | L484: `rightLeg.roll = THIRTYTWOTH` | [정합] | |
| L461 | `bipedLeftLeg.rotateAngleZ = -Thirtytwoth;` | L485 | [정합] | |
| L462 | (빈 줄) | — | [N/A] | |
| L463 | `bipedRightArm.rotationOrder = ModelRotationRenderer.YZX;` | L490 setAnglesYZX | [정합] | |
| L464 | `bipedLeftArm.rotationOrder = ModelRotationRenderer.YZX;` | L491 setAnglesYZX | [정합] | |
| L465 | (빈 줄) | — | [N/A] | |
| L466 | `bipedRightArm.rotateAngleX = cos(distance + Quarter) * Sixtyfourth * walkFactor + Half - Sixtyfourth;` | L488 rPitch (X 인자) | [정합] | |
| L467 | `bipedLeftArm.rotateAngleX = cos(distance - Half) * Sixtyfourth * walkFactor + Half - Sixtyfourth;` | L489 lPitch | [정합] | |
| L468 | (빈 줄) | — | [N/A] | |
| L469 | `bipedRightArm.rotateAngleZ = Sixteenth;` | L490 (Z 인자 = SIXTEENTH) | [정합] | |
| L470 | `bipedLeftArm.rotateAngleZ = -Sixteenth;` | L491 (Z 인자 = -SIXTEENTH) | [정합] | |
| L471 | (빈 줄) | — | [N/A] | |
| L472 | `bipedRightArm.rotateAngleY = -Quarter;` | L490 (Y 인자 = -QUARTER) | [정합] | |
| L473 | `bipedLeftArm.rotateAngleY = Quarter;` | L491 (Y 인자 = QUARTER) | [정합] | |
| L474 | `}` (isSlide 종료) | — | [N/A] | |
| L475 | `else if(isFlying)` | L119: `else if (flyingCreative)` → sm_animateFlying | [정합] | |
| L476 | `{` | — | [N/A] | |
| L477 | `float distance = totalDistance * 0.08F;` | L507: `limbSwing * 0.08f` | [오역] | ⚠️ totalDistance ≠ limbSwing. R-10+ B-N 후보 |
| L478 | `float walkFactor = Factor(currentSpeed, 0F, 1);` | L508: `smFactor(limbSwingAmount, 0f, 1f)` | [오역] | ⚠️ currentSpeed(3D) ≠ limbSwingAmount(수평) |
| L479 | `float standFactor = Factor(currentSpeed, 1F, 0F);` | L509 | [오역] | ⚠️ 동일 |
| L480 | `float time = totalTime * 0.15F;` | L516-L517 인라인: `cos(totalTime * 0.15f)` | [정합] | |
| L481 | `float verticalAngle = isJump ? abs(currentVerticalAngle) : currentVerticalAngle;` | sm_setupTransforms flying 분기 | [정합] | Mixin 분리 — isJump 가드 |
| L482 | `float horizontalAngle = horizontalDistance < 0.05F ? currentCameraAngle : currentHorizontalAngle;` | sm_captureBodyYaw flying 분기 | [정합] | Mixin 분리 — threshold 0.05 |
| L483 | (빈 줄) | — | [N/A] | |
| L484 | `bipedOuter.fadeRotateAngleX = true;` | (fade 부재) | [N/A] | |
| L485 | `bipedOuter.rotateAngleX = (Quarter - verticalAngle) * walkFactor;` | sm_setupTransforms flying (matrices.multiply POSITIVE_X) | [정합] | Mixin 분리 — speedFactor capture |
| L486 | `bipedOuter.rotateAngleY = horizontalAngle;` | sm_captureBodyYaw flying | [정합] | Mixin 분리 |
| L487 | (빈 줄) | — | [N/A] | |
| L488 | `bipedHead.rotateAngleX = -bipedOuter.rotateAngleX / 2F;` | L535-L536: ANIM-01 `head.pitch = -theta / 2f` | [정합] | speedFactor + sm.stats.currentVerticalAngle 활용 |
| L489 | (빈 줄) | — | [N/A] | |
| L490 | `bipedRightArm.rotationOrder = ModelRotationRenderer.XZY;` | L522 setAnglesXZY (B-2 세션 2) | [정합] | |
| L491 | `bipedLeftArm.rotationOrder = ModelRotationRenderer.XZY;` | L523 | [정합] | |
| L492 | (빈 줄) | — | [N/A] | |
| L493 | `bipedRightArm.rotateAngleY = (cos(time) * Sixteenth) * standFactor;` | L516 rYaw (Y 인자) | [정합] | |
| L494 | `bipedLeftArm.rotateAngleY = (cos(time) * Sixteenth) * standFactor;` | L517 lYaw | [정합] | |
| L495 | (빈 줄) | — | [N/A] | |
| L496 | `bipedRightArm.rotateAngleZ = (cos(distance + Half) * Sixtyfourth + (Half - Sixteenth)) * walkFactor + Quarter * standFactor;` | L518-L519 rRoll (Z 인자) | [정합] | |
| L497 | `bipedLeftArm.rotateAngleZ = (cos(distance) * Sixtyfourth - (Half - Sixteenth)) * walkFactor - Quarter * standFactor;` | L520-L521 lRoll | [정합] | |
| L498 | (빈 줄) | — | [N/A] | |
| L499 | `bipedRightLeg.rotateAngleX = cos(distance) * Sixtyfourth * walkFactor + cos(time + Half) * Sixtyfourth * standFactor;` | L526-L527 | [정합] | |
| L500 | `bipedLeftLeg.rotateAngleX = cos(distance + Half) * Sixtyfourth * walkFactor + cos(time) * Sixtyfourth * standFactor;` | L528-L529 | [정합] | |
| L501 | (빈 줄) | — | [N/A] | |
| L502 | `bipedRightLeg.rotateAngleZ = Sixtyfourth;` | L530: `rightLeg.roll = SIXTYFOURTH` | [정합] | |
| L503 | `bipedLeftLeg.rotateAngleZ = -Sixtyfourth;` | L531 | [정합] | |
| L504 | `}` (isFlying 종료) | — | [N/A] | |
| L505 | `else if(isHeadJump)` | L121: `else if (sm.isHeadJumping)` → sm_animateHeadJumping | [정합] | |
| L506 | `{` | — | [N/A] | |
| L507 | `bipedOuter.fadeRotateAngleX = true;` | (fade 부재) | [N/A] | |
| L508 | `bipedOuter.rotateAngleX = (Quarter - currentVerticalAngle);` | sm_setupTransforms headJump 분기 | [정합] | Mixin 분리 |
| L509 | `bipedOuter.rotateAngleY = currentHorizontalAngle;` | sm_captureBodyYaw headJump | [정합] | Mixin 분리 |
| L510 | (빈 줄) | — | [N/A] | |
| L511 | `bipedHead.rotateAngleX = -bipedOuter.rotateAngleX / 2F;` | L558: `head.pitch = -(QUARTER - angle) / 2f` | [정합] | ANIM-01 |
| L512 | (빈 줄) | — | [N/A] | |
| L513 | `float bendFactor = min(Factor(currentVerticalAngle, Quarter, 0), Factor(currentVerticalAngle, -Quarter, 0));` | L550 | [정합] | |
| L514 | `bipedRightArm.rotateAngleX = bendFactor * -Eighth;` | L551 | [정합] | |
| L515 | `bipedLeftArm.rotateAngleX = bendFactor * -Eighth;` | L552 | [정합] | |
| L516 | (빈 줄) | — | [N/A] | |
| L517 | `bipedRightLeg.rotateAngleX = bendFactor * -Eighth;` | L553 | [정합] | |
| L518 | `bipedLeftLeg.rotateAngleX = bendFactor * -Eighth;` | L554 | [정합] | |
| L519 | (빈 줄) | — | [N/A] | |
| L520 | `float armFactorZ = Factor(currentVerticalAngle, Quarter, -Quarter);` | L561 | [정합] | |
| L521 | `if(overGroundBlock != null && overGroundBlock.getMaterial().isSolid())` | L562: `if (sm.smallOverGroundHeight < 5f)` | [정합] | (근사 — 원본은 머리 위 고체 블록 검사 / 1.21.1 단순 높이 검사. computeSmallOverGroundHeight 가 5블록 스캔 후 첫 고체 블록 거리 반환 → 5f 미만 = 머리 위 고체 블록 존재 ≈ 등가) |
| L522 | `armFactorZ = min(armFactorZ, smallOverGroundHeight / 5F);` | L563 | [정합] | |
| L523 | (빈 줄) | — | [N/A] | |
| L524 | `bipedRightArm.rotateAngleZ = Half - Sixteenth + armFactorZ * Eighth;` | L565: `rightArm.roll = HALF - SIXTEENTH + armFactorZ * EIGHTH` | [정합] | |
| L525 | `bipedLeftArm.rotateAngleZ = Sixteenth - Half - armFactorZ * Eighth;` | L566: `leftArm.roll = -(HALF - SIXTEENTH) - armFactorZ * EIGHTH` | [정합] | (원본 `Sixteenth - Half = -(HALF - SIXTEENTH)` 등가) |
| L526 | (빈 줄) | — | [N/A] | |
| L527 | `float legFactorZ = Factor(currentVerticalAngle, -Quarter, Quarter);` | L569 | [정합] | |
| L528 | `bipedRightLeg.rotateAngleZ = Sixtyfourth * legFactorZ;` | L570 | [정합] | |
| L529 | `bipedLeftLeg.rotateAngleZ = -Sixtyfourth * legFactorZ;` | L571 | [정합] | |
| L530 | `}` (isHeadJump 종료) | — | [N/A] | |
| L531 | `else if(isFalling)` | MixinPEMC L129: `if (isFalling)` → sm_animateFalling | [정합] | |
| L532 | `{` | — | [N/A] | |
| L533 | `float distance = totalDistance * 0.1F;` | L581: `float distance = animationProgress * 0.1f` | [정합] | (의도된 근사 — sm_animateFalling 주석에 "totalTime(animationProgress)으로 totalDistance 근사" 명시. totalDistance ≠ animationProgress 이지만 isFalling 시 시간 기반 위상이 합당) |
| L534 | (빈 줄) | — | [N/A] | |
| L535 | `bipedRightArm.rotationOrder = ModelRotationRenderer.XZY;` | L592 setAnglesXZY (B-2 세션 2) | [정합] | |
| L536 | `bipedLeftArm.rotationOrder = ModelRotationRenderer.XZY;` | L593 | [정합] | |
| L537 | (빈 줄) | — | [N/A] | |
| L538 | `bipedRightArm.rotateAngleY = (cos(distance + Quarter) * Eighth);` | L588 rYaw | [정합] | |
| L539 | `bipedLeftArm.rotateAngleY = (cos(distance + Quarter) * Eighth);` | L589 lYaw | [정합] | |
| L540 | (빈 줄) | — | [N/A] | |
| L541 | `bipedRightArm.rotateAngleZ = (cos(distance) * Eighth + Quarter);` | L590 rRoll | [정합] | |
| L542 | `bipedLeftArm.rotateAngleZ = (cos(distance) * Eighth - Quarter);` | L591 lRoll | [정합] | |
| L543 | (빈 줄) | — | [N/A] | |
| L544 | `bipedRightLeg.rotateAngleX = (cos(distance + Half + Quarter) * Sixteenth + Thirtytwoth);` | L596 | [정합] | |
| L545 | `bipedLeftLeg.rotateAngleX = (cos(distance + Quarter) * Sixteenth + Thirtytwoth);` | L597 | [정합] | |
| L546 | (빈 줄) | — | [N/A] | |
| L547 | `bipedRightLeg.rotateAngleZ = (cos(distance) * Sixteenth + Thirtytwoth);` | L599 | [정합] | |
| L548 | `bipedLeftLeg.rotateAngleZ = (cos(distance) * Sixteenth - Thirtytwoth);` | L600 | [정합] | |
| L549 | `}` (isFalling 종료) | — | [N/A] | |
| L550 | `else` | (1.21.1 11-state 체인 끝 — 아무것도 안함) | [N/A] | |
| L551 | `isStandard = true;` | (1.21.1 isStandard 미사용 — vanilla 자동 처리) | [N/A] | |
| L552 | `}` (setRotationAngles 종료) | sm_setAngles 종료 | [N/A] | |
| L553 | (빈 줄) | — | [N/A] | |
| L554 | `private boolean isWorking()` | (1.21.1 미이식 — 어깨 부재로 비표준 working/bowAiming 자체 N/A) | [N/A] | §16-5 — 우선순위 낮음 |
| L555 | `{` | — | [N/A] | |
| L556 | `return mp.onGround > 0F;` | — | [N/A] | |
| L557 | `}` | — | [N/A] | |
| L558 | (빈 줄) | — | [N/A] | |
| L559 | `private void animateAngleJumping()` | sm_animateAngleJumping (L609-L624) | [정합] | |
| L560 | `{` | — | [N/A] | |
| L561 | `float angle = angleJumpType * Eighth;` | L610: `float angle = sm.angleJumpType * EIGHTH` | [정합] | |
| L562 | `md.bipedPelvic.rotateAngleY -= md.bipedOuter.rotateAngleY;` | (Pelvic 부재) | [N/A] | §16-3 — 골반 분리 yaw 보정 N/A |
| L563 | `md.bipedPelvic.rotateAngleY += md.currentCameraAngle;` | (Pelvic 부재) | [N/A] | |
| L564 | (빈 줄) | — | [N/A] | |
| L565 | `float backness = 1F - Math.abs(angle - Half) / Quarter;` | L611 | [정합] | |
| L566 | `float leftness = -Math.min(angle - Half, 0F) / Quarter;` | L612 | [정합] | |
| L567 | `float rightness = Math.max(angle - Half, 0F) / Quarter;` | L613 | [정합] | |
| L568 | (빈 줄) | — | [N/A] | |
| L569 | `md.bipedLeftLeg.rotateAngleX = Thirtytwoth * (1F + rightness);` | L616 setAnglesZXY X 인자 | [정합] | |
| L570 | `md.bipedRightLeg.rotateAngleX = Thirtytwoth * (1F + leftness);` | L617 | [정합] | |
| L571 | `md.bipedLeftLeg.rotateAngleY = -angle;` | L616 (Y 인자 = -angle) | [정합] | |
| L572 | `md.bipedRightLeg.rotateAngleY = -angle;` | L617 | [정합] | |
| L573 | `md.bipedLeftLeg.rotateAngleZ = Thirtytwoth * backness;` | L616 (Z 인자) | [정합] | |
| L574 | `md.bipedRightLeg.rotateAngleZ = -Thirtytwoth * backness;` | L617 | [정합] | |
| L575 | (빈 줄) | — | [N/A] | |
| L576 | `md.bipedLeftLeg.rotationOrder = ModelRotationRenderer.ZXY;` | L616 setAnglesZXY 헬퍼 (B-08) | [정합] | |
| L577 | `md.bipedRightLeg.rotationOrder = ModelRotationRenderer.ZXY;` | L617 | [정합] | |
| L578 | (빈 줄) | — | [N/A] | |
| L579 | `md.bipedLeftArm.rotateAngleZ = -Sixteenth * rightness;` | L620: `leftArm.roll = -SIXTEENTH * rightness` | [정합] | |
| L580 | `md.bipedRightArm.rotateAngleZ = Sixteenth * leftness;` | L621: `rightArm.roll = SIXTEENTH * leftness` | [정합] | |
| L581 | (빈 줄) | — | [N/A] | |
| L582 | `md.bipedLeftArm.rotateAngleX = -Eighth * backness;` | L622: `leftArm.pitch = -EIGHTH * backness` | [정합] | |
| L583 | `md.bipedRightArm.rotateAngleX = -Eighth * backness;` | L623: `rightArm.pitch = -EIGHTH * backness` | [정합] | |
| L584 | `}` (animateAngleJumping 종료) | — | [N/A] | |
| L585 | (빈 줄) | — | [N/A] | |
| L586 | `private void animateNonStandardWorking(float viewVerticalAngelOffset)` | (1.21.1 미이식 — 어깨 부재) | [N/A] | §16-5 — 클라이밍/수영 중 도구 사용 시 어깨 고정 N/A |
| L587 | `{` | — | [N/A] | |
| L588 | `md.bipedRightShoulder.ignoreSuperRotation = true;` | (Shoulder 부재) | [N/A] | |
| L589 | `md.bipedRightShoulder.rotateAngleX = viewVerticalAngelOffset / RadiantToAngle;` | (Shoulder 부재) | [N/A] | |
| L590 | `md.bipedRightShoulder.rotateAngleY = md.workingAngle / RadiantToAngle;` | (Shoulder 부재) | [N/A] | |
| L591 | `md.bipedRightShoulder.rotateAngleZ = Half;` | (Shoulder 부재) | [N/A] | |
| L592 | `md.bipedRightShoulder.rotationOrder = ModelRotationRenderer.ZYX;` | (Shoulder 부재) | [N/A] | |
| L593 | `md.bipedRightArm.reset();` | (Shoulder 의존) | [N/A] | |
| L594 | `}` | — | [N/A] | |
| L595 | (빈 줄) | — | [N/A] | |
| L596 | `private void animateNonStandardBowAiming(...)` | (1.21.1 미이식 — 어깨 부재) | [N/A] | §16-5 |
| L597 | `{` | — | [N/A] | |
| L598 | `md.bipedRightShoulder.ignoreSuperRotation = true;` | (Shoulder 부재) | [N/A] | |
| L599 | `md.bipedRightShoulder.rotateAngleY = md.workingAngle / RadiantToAngle;` | (Shoulder 부재) | [N/A] | |
| L600 | `md.bipedRightShoulder.rotateAngleZ = Half;` | (Shoulder 부재) | [N/A] | |

**청크 3 (L401-L600) 통계: 정합 95 / 오역 3 / 누락 7 / 잉여 0 / N/A 95 = 200 라인 전수.**

**청크 3 발견 (R-10+ B-N 후보, §16 등재)**:
- B-N (isCrawl head pivotZ): L401 — head.pivotZ = -2F 미이식.
- B-N (isCrawl body pivotY): L405 — body.pivotY = 3F 미이식 (bipedTorso → body 근사 추가 보정).
- B-N (isSlide head 자세): L442/L444 — head.roll = -headYaw + head.pivotZ = -2F 미이식.
- B-N (isSlide outer pivotY): L448 — bipedOuter.pivotY = 5F 미이식. entity-level translate (setupTransforms / getPositionOffset) 가능.
- B-N (isSlide body offset/pivot): L452/L453 — body.offsetY = -0.4F (ModelPart에 offsetY 부재 — MatrixStack 보정) + body.pivotY = 6.5F 미이식.
- B-N (isFlying 입력값 [오역]): L477/L478/L479 — totalDistance/currentSpeed → limbSwing/limbSwingAmount (청크 1/2 동일 패턴).

**다음 청크**: R-1 청크 4 (L601-L797) — animateNonStandardBowAiming 잔여 + setArmScales/setLegScales 본체 + 기타 유틸/내부 헬퍼.
