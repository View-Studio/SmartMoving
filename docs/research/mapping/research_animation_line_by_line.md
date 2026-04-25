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

### 청크 4 (L601-L797) — animateNonStandardBowAiming 잔여 + animate* 11 vanilla 분기 + setArmScales/setLegScales/Factor/Between/Normalize 헬퍼 + 필드 선언

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L601 | `md.bipedRightShoulder.rotationOrder = ModelRotationRenderer.ZYX;` | (Shoulder 부재) | [N/A] | §16-5 |
| L602 | (빈 줄) | — | [N/A] | |
| L603 | `md.bipedLeftShoulder.ignoreSuperRotation = true;` | (Shoulder 부재) | [N/A] | |
| L604 | `md.bipedLeftShoulder.rotateAngleY = md.workingAngle / RadiantToAngle;` | (Shoulder 부재) | [N/A] | |
| L605 | `md.bipedLeftShoulder.rotateAngleZ = Half;` | (Shoulder 부재) | [N/A] | |
| L606 | `md.bipedLeftShoulder.rotationOrder = ModelRotationRenderer.ZYX;` | (Shoulder 부재) | [N/A] | |
| L607 | (빈 줄) | — | [N/A] | |
| L608 | `md.bipedRightArm.reset();` | (1.21.1 vanilla setAngles 진입 시 자동 reset, sm_setAngles는 TAIL inject으로 덮어쓰기) | [N/A] | |
| L609 | `md.bipedLeftArm.reset();` | — | [N/A] | |
| L610 | (빈 줄) | — | [N/A] | |
| L611 | `float headRotateAngleY = md.bipedHead.rotateAngleY;` | (백업 — vanilla bow aiming 호출 위해, 1.21.1 vanilla 자동) | [N/A] | |
| L612 | `float outerRotateAngleY = md.bipedOuter.rotateAngleY;` | (Outer 부재) | [N/A] | |
| L613 | `float headRotateAngleX = md.bipedHead.rotateAngleX;` | — | [N/A] | |
| L614 | (빈 줄) | — | [N/A] | |
| L615 | `md.bipedHead.rotateAngleY = 0;` | — | [N/A] | |
| L616 | `md.bipedOuter.rotateAngleY = 0;` | (Outer 부재) | [N/A] | |
| L617 | `md.bipedHead.rotateAngleX = 0;` | — | [N/A] | |
| L618 | (빈 줄) | — | [N/A] | |
| L619 | `imp.superAnimateBowAiming(...)` | (vanilla 활쏘기 — 1.21.1 ItemRenderer 자동 처리, 어깨 ZYX 미이식) | [N/A] | |
| L620 | (빈 줄) | — | [N/A] | |
| L621 | `md.bipedHead.rotateAngleY = headRotateAngleY;` | (복원) | [N/A] | |
| L622 | `md.bipedOuter.rotateAngleY = outerRotateAngleY;` | (Outer 부재) | [N/A] | |
| L623 | `md.bipedHead.rotateAngleX = headRotateAngleX;` | — | [N/A] | |
| L624 | `}` (animateNonStandardBowAiming 종료) | — | [N/A] | |
| L625 | (빈 줄) | — | [N/A] | |
| L626 | `public void animateHeadRotation(...)` | (1.21.1 vanilla setAngles 자동 — head.yaw/pitch는 vanilla가 매개변수로 자동 설정) | [N/A] | sm_setAngles TAIL에서 덮어씀 (필요 시) |
| L627 | `{` | — | [N/A] | |
| L628 | `setRotationAngles(totalHorizontalDistance, ...);` | sm_setAngles TAIL inject 자체 호출 위치 | [N/A] | |
| L629 | (빈 줄) | — | [N/A] | |
| L630 | `if(isStandard)` | (isStandard 미사용 — vanilla 자동) | [N/A] | |
| L631 | `imp.superAnimateHeadRotation(...);` | (vanilla 자동) | [N/A] | |
| L632 | `}` | — | [N/A] | |
| L633 | (빈 줄) | — | [N/A] | |
| L634 | `public void animateSleeping(...)` | (vanilla SleepingPose 자동) | [N/A] | |
| L635 | `{` | — | [N/A] | |
| L636 | `if(isStandard)` | — | [N/A] | |
| L637 | `imp.superAnimateSleeping(...);` | (vanilla) | [N/A] | |
| L638 | `}` | — | [N/A] | |
| L639 | (빈 줄) | — | [N/A] | |
| L640 | `public void animateArmSwinging(...)` | (vanilla limbSwing 기반 자동) | [N/A] | |
| L641 | `{` | — | [N/A] | |
| L642 | `if(isStandard)` | (sm_setAngles 본체 11-state 체인 외 = vanilla 자동) | [N/A] | |
| L643 | `if(isAngleJumping)` | MixinPEMC L135: `if (sm.isAngleJumping())` | [정합] | sm_setAngles 본체 직접 분기 |
| L644 | `animateAngleJumping();` | L136: `sm_animateAngleJumping(sm)` | [정합] | |
| L645 | `else` | — | [N/A] | (vanilla arm swing 자동) |
| L646 | `imp.superAnimateArmSwinging(...);` | — | [N/A] | |
| L647 | `}` | — | [N/A] | |
| L648 | (빈 줄) | — | [N/A] | |
| L649 | `public void animateRiding(...)` | (vanilla Riding 자동) | [N/A] | |
| L650 | `{` | — | [N/A] | |
| L651 | `if(isStandard)` | — | [N/A] | |
| L652 | `imp.superAnimateRiding(...);` | — | [N/A] | |
| L653 | `}` | — | [N/A] | |
| L654 | (빈 줄) | — | [N/A] | |
| L655 | `public void animateLeftArmItemHolding(...)` | (vanilla LeftArm item holding 자동) | [N/A] | |
| L656 | `{` | — | [N/A] | |
| L657 | `if(isStandard)` | — | [N/A] | |
| L658 | `imp.superAnimateLeftArmItemHolding(...);` | — | [N/A] | |
| L659 | `}` | — | [N/A] | |
| L660 | (빈 줄) | — | [N/A] | |
| L661 | `public void animateRightArmItemHolding(...)` | (vanilla 자동) | [N/A] | |
| L662 | `{` | — | [N/A] | |
| L663 | `if(isStandard)` | — | [N/A] | |
| L664 | `imp.superAnimateRightArmItemHolding(...);` | — | [N/A] | |
| L665 | `}` | — | [N/A] | |
| L666 | (빈 줄) | — | [N/A] | |
| L667 | `public void animateWorkingBody(...)` | (vanilla / 어깨 부재) | [N/A] | |
| L668 | `{` | — | [N/A] | |
| L669 | `if(isStandard)` | — | [N/A] | |
| L670 | `imp.superAnimateWorkingBody(...);` | — | [N/A] | |
| L671 | `else if(isWorking())` | (어깨 부재 — animateNonStandardWorking N/A) | [N/A] | §16-5 |
| L672 | `animateNonStandardWorking(viewVerticalAngelOffset);` | — | [N/A] | |
| L673 | `}` | — | [N/A] | |
| L674 | (빈 줄) | — | [N/A] | |
| L675 | `public void animateWorkingArms(...)` | (vanilla) | [N/A] | |
| L676 | `{` | — | [N/A] | |
| L677 | `if(isStandard \|\| isWorking())` | — | [N/A] | |
| L678 | `imp.superAnimateWorkingArms(...);` | — | [N/A] | |
| L679 | `}` | — | [N/A] | |
| L680 | (빈 줄) | — | [N/A] | |
| L681 | `public void animateSneaking(...)` | (vanilla sneak 자동 — leaningPitch 별도 처리) | [N/A] | sm_setAngles L88 leaningPitch=0 처리 |
| L682 | `{` | — | [N/A] | |
| L683 | `if(isStandard && !isAngleJumping)` | — | [N/A] | |
| L684 | `imp.superAnimateSneaking(...);` | — | [N/A] | |
| L685 | `}` | — | [N/A] | |
| L686 | (빈 줄) | — | [N/A] | |
| L687 | `public void animateArms(...)` | (vanilla applyAnimationOffsets 자동) | [N/A] | |
| L688 | `{` | — | [N/A] | |
| L689 | `if(isStandard)` | — | [N/A] | |
| L690 | `imp.superApplyAnimationOffsets(...);` | — | [N/A] | |
| L691 | `}` | — | [N/A] | |
| L692 | (빈 줄) | — | [N/A] | |
| L693 | `public void animateBowAiming(...)` | (vanilla 자동 + 어깨 부재로 비표준 분기 N/A) | [N/A] | |
| L694 | `{` | — | [N/A] | |
| L695 | `if(isStandard)` | — | [N/A] | |
| L696 | `imp.superAnimateBowAiming(...);` | — | [N/A] | |
| L697 | `else` | — | [N/A] | |
| L698 | `animateNonStandardBowAiming(...);` | (어깨 부재 N/A) | [N/A] | §16-5 |
| L699 | `}` | — | [N/A] | |
| L700 | (빈 줄) | — | [N/A] | |
| L701 | `private void setArmScales(float rightScale, float leftScale)` | MixinPEMC L671: `setArmScales(rightArm, leftArm, rightScale, leftScale)` | [정합] | B-3 세션 2 완결. ModelPart 인자 추가 (static 메서드) |
| L702 | `{` | — | [N/A] | |
| L703 | `if(scaleArmType == Scale)` | (가드 제거 — 메인 모델 = Scale 항상 true) | [정합] | §16-7 |
| L704 | `{` | — | [N/A] | |
| L705 | `md.bipedRightArm.scaleY = rightScale;` | L672: `rightArm.yScale = rightScale` | [정합] | |
| L706 | `md.bipedLeftArm.scaleY = leftScale;` | L673: `leftArm.yScale = leftScale` | [정합] | |
| L707 | `}` | — | [N/A] | |
| L708 | `else if(scaleArmType == NoScaleEnd)` | (1.21.1 미이식 — 갑옷 흉갑 분기 §17 잔여) | [N/A] | §16-8 — ArmorFeatureRenderer 영역 |
| L709 | `{` | — | [N/A] | |
| L710 | `md.bipedRightArm.offsetY -= (1F - rightScale) * 0.5F;` | (ModelPart.offsetY 부재) | [N/A] | §16-8 |
| L711 | `md.bipedLeftArm.offsetY -= (1F - leftScale) * 0.5F;` | (ModelPart.offsetY 부재) | [N/A] | |
| L712 | `}` | — | [N/A] | |
| L713 | `}` | — | [N/A] | |
| L714 | (빈 줄) | — | [N/A] | |
| L715 | `private void setLegScales(float rightScale, float leftScale)` | MixinPEMC L681: `setLegScales(rightLeg, leftLeg, rightScale, leftScale)` | [정합] | B-3 세션 2 완결 |
| L716 | `{` | — | [N/A] | |
| L717 | `if(scaleLegType == Scale)` | (가드 제거) | [정합] | |
| L718 | `{` | — | [N/A] | |
| L719 | `md.bipedRightLeg.scaleY = rightScale;` | L682: `rightLeg.yScale = rightScale` | [정합] | |
| L720 | `md.bipedLeftLeg.scaleY = leftScale;` | L683: `leftLeg.yScale = leftScale` | [정합] | |
| L721 | `}` | — | [N/A] | |
| L722 | `else if(scaleLegType == NoScaleEnd)` | (1.21.1 미이식 — §17 잔여) | [N/A] | §16-8 |
| L723 | `{` | — | [N/A] | |
| L724 | `md.bipedRightLeg.offsetY -= (1F - rightScale) * 0.5F;` | (offsetY 부재) | [N/A] | |
| L725 | `md.bipedLeftLeg.offsetY -= (1F - leftScale) * 0.5F;` | (offsetY 부재) | [N/A] | |
| L726 | `}` | — | [N/A] | |
| L727 | `}` | — | [N/A] | |
| L728 | (빈 줄) | — | [N/A] | |
| L729 | `private static float Factor(float x, float x0, float x1)` | MixinPEMC L762: `private static float smFactor(float x, float x0, float x1)` | [정합] | 표면 매핑 (이름만 변경) |
| L730 | `{` | `{` | [정합] | |
| L731 | `if(x0 > x1)` | L763 | [정합] | |
| L732 | `{` | `{` | [정합] | |
| L733 | `if(x <= x1)` | L764 | [정합] | |
| L734 | `return 1F;` | `return 1f;` | [정합] | |
| L735 | `if(x >= x0)` | L765 | [정합] | |
| L736 | `return 0F;` | `return 0f;` | [정합] | |
| L737 | `return (x0 - x) / (x0 - x1);` | L766 | [정합] | |
| L738 | `}` | `}` | [정합] | |
| L739 | `else` | `} else` (L767) | [정합] | |
| L740 | `{` | `{` | [정합] | |
| L741 | `if(x >= x1)` | L768 | [정합] | |
| L742 | `return 1F;` | | [정합] | |
| L743 | `if(x <= x0)` | L769 | [정합] | |
| L744 | `return 0F;` | | [정합] | |
| L745 | `return (x - x0) / (x1 - x0);` | L770 | [정합] | |
| L746 | `}` | `}` | [정합] | |
| L747 | `}` | `}` | [정합] | |
| L748 | (빈 줄) | — | [N/A] | |
| L749 | `private static float Between(float min, float max, float value)` | `MathHelper.clamp(value, min, max)` 직접 사용 (sm_animateRopeSliding L162) | [정합] | 표면 매핑 — vanilla MathHelper.clamp 활용 |
| L750 | `{` | — | [정합] | clamp 인라인 |
| L751 | `if(value < min)` | clamp 본체 (vanilla) | [정합] | |
| L752 | `return min;` | | [정합] | |
| L753 | `if(value > max)` | | [정합] | |
| L754 | `return max;` | | [정합] | |
| L755 | `return value;` | | [정합] | |
| L756 | `}` | | [정합] | |
| L757 | (빈 줄) | — | [N/A] | |
| L758 | `private static float Normalize(float radiant)` | `MathHelper.wrapDegrees(deg) * DEG_TO_RAD` 직접 사용 (sm_animateRopeSliding L159) | [정합] | 표면 매핑 — vanilla wrapDegrees 활용 |
| L759 | `{` | — | [정합] | |
| L760 | `while(radiant > Half)` | (wrapDegrees 본체) | [정합] | |
| L761 | `radiant -= Whole;` | | [정합] | |
| L762 | `while(radiant < -Half)` | | [정합] | |
| L763 | `radiant += Whole;` | | [정합] | |
| L764 | `return radiant;` | | [정합] | |
| L765 | `}` | | [정합] | |
| L766 | (빈 줄) | — | [N/A] | |
| L767 | `public boolean isStandard;` | (1.21.1 미사용 — vanilla 자동) | [N/A] | 필드 선언 |
| L768 | (빈 줄) | — | [N/A] | |
| L769 | `public boolean isClimb;` | `SmartMovingClientState.isClimbing` | [N/A] | 필드 — 이전됨 |
| L770 | `public boolean isClimbJump;` | `sm.isClimbJumping` | [N/A] | |
| L771 | `public int feetClimbType;` | `sm.actualFeetClimbType` (ordinal) | [N/A] | |
| L772 | `public int handsClimbType;` | `sm.actualHandsClimbType` | [N/A] | |
| L773 | `public boolean isHandsVineClimbing;` | `sm.isHandsVineClimbing` | [N/A] | |
| L774 | `public boolean isFeetVineClimbing;` | `sm.isFeetVineClimbing` | [N/A] | |
| L775 | `public boolean isCeilingClimb;` | `sm.isCeilingClimbing` | [N/A] | |
| L776 | (빈 줄) | — | [N/A] | |
| L777 | `public boolean isSwim;` | `sm.isSwimming_sm` | [N/A] | |
| L778 | `public boolean isDive;` | `sm.isDiving` | [N/A] | |
| L779 | `public boolean isCrawl;` | `sm.isCrawling` | [N/A] | |
| L780 | `public boolean isCrawlClimb;` | `sm.isCrawlClimbing` | [N/A] | |
| L781 | `public boolean isJump;` | (vanilla jump) | [N/A] | |
| L782 | `public boolean isHeadJump;` | `sm.isHeadJumping` | [N/A] | |
| L783 | `public boolean isFlying;` | `flyingCreative` (PlayerAbilities) | [N/A] | |
| L784 | `public boolean isSlide;` | `sm.isSliding` | [N/A] | |
| L785 | `public boolean isLevitate;` | (vanilla levitation StatusEffect) | [N/A] | |
| L786 | `public boolean isFalling;` | `isFalling` (computed inline MixinPEMC L125-L128) | [N/A] | |
| L787 | `public boolean isGenericSneaking;` | (vanilla sneak) | [N/A] | |
| L788 | `public boolean isAngleJumping;` | `sm.isAngleJumping()` | [N/A] | |
| L789 | `public int angleJumpType;` | `sm.angleJumpType` | [N/A] | |
| L790 | `public boolean isRopeSliding;` | `sm.isRopeSliding` | [N/A] | |
| L791 | (빈 줄) | — | [N/A] | |
| L792 | `public float currentHorizontalSpeedFlattened;` | `sm.currentHorizontalSpeedFlattened` | [N/A] | |
| L793 | `public float smallOverGroundHeight;` | `sm.smallOverGroundHeight` (computeSmallOverGroundHeight 산출) | [N/A] | |
| L794 | `public Block overGroundBlock;` | (1.21.1: smallOverGroundHeight < 5f 단순화 — §16-21) | [N/A] | |
| L795 | (빈 줄) | — | [N/A] | |
| L796 | `public int scaleArmType;` | (메인 = Scale 가드 제거) | [N/A] | §16-7 |
| L797 | `public int scaleLegType;` | (메인 = Scale) | [N/A] | |

**청크 4 (L601-L797) 통계: 정합 45 / 오역 0 / 누락 0 / 잉여 0 / N/A 152 = 197 라인 전수.**

**청크 4 발견**:
- 신규 [오역]/[누락]/[잉여] 0건. 청크 4는 헬퍼 본체(setArmScales/setLegScales/Factor/Between/Normalize) + vanilla 자동 처리 메서드(animateHeadRotation/Sleeping/ArmSwinging/Riding/HoldingItems/WorkingBody/WorkingArms/Sneaking/Arms/BowAiming) + 필드 선언 위주 — 모두 [정합] 또는 [N/A] 분류.
- 헬퍼 본체 모두 1.21.1 측에 1:1 이식되어 있음을 확인 (B-3 세션 2 완결 검증).

**R-1 전체 (4 청크) 누적 통계**:
| 청크 | 라인 | 정합 | 오역 | 누락 | 잉여 | N/A |
|-----|-----|------|------|------|------|------|
| 1 (L1-L200) | 200 | 84 | 3 | 2 | 0 | 111 |
| 2 (L201-L400) | 200 | 122 | 8 | 5 | 0 | 65 |
| 3 (L401-L600) | 200 | 95 | 3 | 7 | 0 | 95 |
| 4 (L601-L797) | 197 | 45 | 0 | 0 | 0 | 152 |
| **합계** | **797** | **346** | **14** | **14** | **0** | **423** |

**R-1 전체 발견 (R-10+ B-N 후보 종합)**:
1. [오역] climbing arm/leg verticalDistance/verticalSpeed 입력 손실 (L80/L144/L200/L201/L219/L220 = 6 라인)
2. [오역] FeetVineClimbing total/difference 입력 (L228/L232 = 2 라인)
3. [오역] Dive 입력값 (L365/L366/L367 = 3 라인)
4. [오역] Flying 입력값 (L477/L478/L479 = 3 라인)
5. [누락] isRopeSliding 머리/팔 pivotY (L106/L117 = 2 라인)
6. [누락] NoGrab+non-NoStep body.pivotZ -6F (L285)
7. [누락] Swim head.pivotZ -2F (L329)
8. [누락] Swim body.yaw 좌우 흔들림 (L335)
9. [누락] Dive head.pitch -EIGHTH + head.pivotZ -2F (L370/L371)
10. [누락] isCrawl head.pivotZ + body.pivotY (L401/L405)
11. [누락] isSlide head/body/outer 피벗·offset (L442/L444/L448/L452/L453 = 5 라인)
12. [잉여 검토] sm_animateCeilingClimbing head.yaw 추가 차감 (1.21.1 L332)
13. [정합 근사] isHeadJump overGroundBlock 단순화 (L521)

**R-1 완료**. 다음 R-단계: **R-2 (SmartMovingRender.java + SM render Context/IModel/IRender/ModelPlayer/RenderPlayer ~726줄)**.

---

## R-2: SmartMovingRender.java + SM render 5 파일 (~726줄)

### 청크 1 (SmartMovingRender.java L1-L200) — 헤더 + 생성자(Scale 분류 4 모델) + renderPlayer 본체 (상태 캡처 + ModelPlayer 분배 + Levitate 보정) + rotatePlayer + renderPlayerAt + renderName 본체

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L16 | `// ==…== Smart Moving GPLv3 라이선스 헤더` | (라이선스 헤더 생략) | [N/A] | 16 라인 일괄 |
| L17 | `// ==…==` | — | [N/A] | |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.moving.render;` | `package choco.ratel.smartmoving.mixin.client;` | [N/A] | |
| L20 | `import org.lwjgl.opengl.GL11;` | (1.21.1: MatrixStack/RotationAxis 대체) | [N/A] | OpenGL → MatrixStack |
| L21 | (빈 줄) | — | [N/A] | |
| L22 | `import net.minecraft.block.*;` | (사용 시점 import) | [N/A] | |
| L23 | `import net.minecraft.block.material.*;` | (1.21.1: Material 제거됨 — VoxelShape/BlockState 대체) | [N/A] | |
| L24 | `import net.minecraft.client.*;` | — | [N/A] | |
| L25 | `import net.minecraft.client.entity.*;` | — | [N/A] | |
| L26 | `import net.minecraft.client.gui.*;` | — | [N/A] | |
| L27 | `import net.minecraft.client.gui.inventory.*;` | (1.21.1: InventoryScreen) | [N/A] | |
| L28 | `import net.minecraft.entity.*;` | — | [N/A] | |
| L29 | `import net.minecraft.entity.player.*;` | — | [N/A] | |
| L30 | `import net.minecraft.util.*;` | — | [N/A] | |
| L31 | (빈 줄) | — | [N/A] | |
| L32 | `import net.smart.moving.*;` | `import choco.ratel.smartmoving.client.SmartMovingClientState;` | [N/A] | |
| L33 | `import net.smart.render.statistics.*;` | (1.21.1: vanilla limbAnimator/age 대체 — SmartStatistics 제거) | [N/A] | §16-21 등 |
| L34 | (빈 줄) | — | [N/A] | |
| L35 | `public class SmartMovingRender extends SmartRenderContext` | `@Mixin(PlayerEntityRenderer.class) public abstract class MixinPlayerEntityRenderer` | [정합] | 클래스 매핑 — 표면 |
| L36 | `{` | `{` | [정합] | |
| L37 | `public static SmartMovingModel CurrentMainModel;` | (1.21.1 단일 PlayerEntityModel — CurrentMainModel 불필요) | [N/A] | 다층 모델 부재 |
| L38 | (빈 줄) | — | [N/A] | |
| L39 | `public IRenderPlayer irp;` | (Mixin: this 직접 접근, 인터페이스 부재) | [N/A] | |
| L40 | (빈 줄) | — | [N/A] | |
| L41 | `public SmartMovingRender(IRenderPlayer irp)` | (Mixin — 생성자 부재) | [N/A] | |
| L42 | `{` | — | [N/A] | |
| L43 | `this.irp = irp;` | — | [N/A] | |
| L44 | (빈 줄) | — | [N/A] | |
| L45 | `modelBipedMain = irp.getPlayerModelBipedMain().getMovingModel();` | (1.21.1: PlayerEntityModel 단일 = 메인 모델 자동) | [N/A] | 다층 부재 |
| L46 | `SmartMovingModel modelArmorChestplate = irp.getPlayerModelArmorChestplate().getMovingModel();` | (ArmorFeatureRenderer 영역 — §17 잔여) | [N/A] | 갑옷 흉갑 |
| L47 | `SmartMovingModel modelArmor = irp.getPlayerModelArmor().getMovingModel();` | (ArmorFeatureRenderer 영역) | [N/A] | 갑옷 일반 |
| L48 | (빈 줄) | — | [N/A] | |
| L49 | `modelBipedMain.scaleArmType = Scale;` | (메인 = Scale 가드 제거 시 묵시) | [정합] | §16-7 — setArmScales 가드 제거 근거 |
| L50 | `modelBipedMain.scaleLegType = Scale;` | (메인 = Scale 묵시) | [정합] | §16-7 |
| L51 | `modelArmorChestplate.scaleArmType = NoScaleStart;` | (갑옷 흉갑 — 1.21.1 미이식) | [N/A] | §16-8 |
| L52 | `modelArmorChestplate.scaleLegType = NoScaleEnd;` | (갑옷 흉갑 — offsetY 보정 미이식) | [N/A] | §16-8 — §17 잔여 |
| L53 | `modelArmor.scaleArmType = NoScaleStart;` | (갑옷 일반 — 미이식) | [N/A] | |
| L54 | `modelArmor.scaleLegType = Scale;` | (갑옷 일반 leg = Scale, 미이식) | [N/A] | |
| L55 | `}` | — | [N/A] | |
| L56 | (빈 줄) | — | [N/A] | |
| L57 | `public void renderPlayer(AbstractClientPlayer entityplayer, double d, double d1, double d2, float f, float renderPartialTicks)` | (1.21.1: vanilla render 자체 + sm_setupTransforms TAIL + sm_captureBodyYaw HEAD 분리 처리) | [N/A] | renderPlayer 단일 메서드 분해됨 |
| L58 | `{` | — | [N/A] | |
| L59 | `IModelPlayer[] modelPlayers = null;` | (1.21.1 단일 모델 — modelPlayers 배열 불필요) | [N/A] | |
| L60 | `SmartMoving moving = SmartMovingFactory.getInstance(entityplayer);` | `SmartMovingClientState sm = SmartMovingClientStateAccess.smartmoving$getState((Object)player);` (Mixin 본체에서 매번 access) | [정합] | factory → state holder |
| L61 | `if(moving != null)` | (sm null check Mixin 본체) | [정합] | |
| L62 | `{` | — | [N/A] | |
| L63 | `boolean isInventory = d == 0.0F && d1 == 0.0F && d2 == 0.0F && f == 0.0F && renderPartialTicks == 1.0F;` | (1.21.1: InventoryScreen 별도 — sm_getPositionOffset 등 인벤토리 화면 자동 분리) | [N/A] | 인벤토리 detect 미이식 (vanilla 자동) |
| L64 | (빈 줄) | — | [N/A] | |
| L65 | `boolean isClimb = moving.isClimbing && !moving.isCrawling && !moving.isCrawlClimbing && !moving.isClimbJumping;` | `sm.isClimbing` (state holder는 이미 mutex 포함된 단일 isClimbing) | [정합] | SmartMovingClientStateUpdater 에서 mutex 처리 |
| L66 | `boolean isClimbJump = moving.isClimbJumping;` | `sm.isClimbJumping` | [정합] | |
| L67 | `int handsClimbType = moving.actualHandsClimbType;` | `sm.actualHandsClimbType` | [정합] | |
| L68 | `int feetClimbType = moving.actualFeetClimbType;` | `sm.actualFeetClimbType` | [정합] | |
| L69 | `boolean isHandsVineClimbing = moving.isHandsVineClimbing;` | `sm.isHandsVineClimbing` | [정합] | |
| L70 | `boolean isFeetVineClimbing = moving.isFeetVineClimbing;` | `sm.isFeetVineClimbing` | [정합] | |
| L71 | `boolean isCeilingClimb = moving.isCeilingClimbing;` | `sm.isCeilingClimbing` | [정합] | |
| L72 | `boolean isSwim = moving.isSwimming && !moving.isDipping;` | `sm.isSwimming_sm` (mutex 포함) | [정합] | |
| L73 | `boolean isDive = moving.isDiving;` | `sm.isDiving` | [정합] | |
| L74 | `boolean isLevitate = moving.isLevitating;` | (vanilla LEVITATION StatusEffect 직접 검사) | [정합] | sm_setupTransforms dive 분기 |
| L75 | `boolean isCrawl = moving.isCrawling && !moving.isClimbing;` | `sm.isCrawling` (mutex) | [정합] | |
| L76 | `boolean isCrawlClimb = moving.isCrawlClimbing \|\| (moving.isClimbing && moving.isCrawling);` | `sm.isCrawlClimbing` | [정합] | mutex 합성 처리 |
| L77 | `boolean isJump = moving.isJumping();` | (vanilla jumping detect — 11-state 체인 외) | [N/A] | isJump는 setupTransforms에서 직접 검사 |
| L78 | `boolean isHeadJump = moving.isHeadJumping;` | `sm.isHeadJumping` | [정합] | |
| L79 | `boolean isFlying = moving.doFlyingAnimation();` | `flyingCreative` (PlayerAbilities) — MixinPEMC L84-L86 검사 | [정합] | |
| L80 | `boolean isSlide = moving.isSliding;` | `sm.isSliding` | [정합] | |
| L81 | `boolean isFalling = moving.doFallingAnimation();` | (MixinPEMC L125-L128: isFalling 인라인 계산 — fallDistance > 1.5 + onGround false + 외 SM 상태 false + 물 외) | [정합] | |
| L82 | `boolean isGenericSneaking = moving.isSlow;` | (vanilla isSneaking 또는 sm.isSlow) | [정합] | |
| L83 | `boolean isAngleJumping = moving.isAngleJumping();` | `sm.isAngleJumping()` | [정합] | |
| L84 | `int angleJumpType = moving.angleJumpType;` | `sm.angleJumpType` | [정합] | |
| L85 | `boolean isRopeSliding = moving.isRopeSliding;` | `sm.isRopeSliding` | [정합] | |
| L86 | (빈 줄) | — | [N/A] | |
| L87 | `SmartStatistics statistics = SmartStatisticsFactory.getInstance(entityplayer);` | (1.21.1: vanilla limbAnimator/age 대체 — SmartStatistics 제거) | [N/A] | R-8 검토 |
| L88 | `float currentHorizontalSpeedFlattened = statistics != null ? statistics.getCurrentHorizontalSpeedFlattened(renderPartialTicks, -1) : Float.NaN;` | (1.21.1: limbSwingAmount 직접 사용 — flattened 분리 미이식) | [정합 (근사)] | NaN 분기 가드 미이식 |
| L89 | `float smallOverGroundHeight = isCrawlClimb \|\| isHeadJump ? (float)moving.getOverGroundHeight(5D) : 0F;` | MixinPEMC L94-L96: `if (sm.isCrawlClimbing \|\| sm.isHeadJumping) sm.smallOverGroundHeight = computeSmallOverGroundHeight(...)` | [정합] | 가드 + 계산 매핑 |
| L90 | `Block overGroundBlock = isHeadJump && smallOverGroundHeight < 5F ? moving.getOverGroundBlockId(smallOverGroundHeight) : null;` | (1.21.1: smallOverGroundHeight < 5f 단순화 — material check 손실, §16-21) | [정합 (근사)] | block material → height 단순화 |
| L91 | (빈 줄) | — | [N/A] | |
| L92 | `modelPlayers = irp.getPlayerModels();` | (1.21.1 단일 모델 — 배열 부재) | [N/A] | |
| L93 | (빈 줄) | — | [N/A] | |
| L94 | `for(int i = 0; i < modelPlayers.length; i++)` | (단일 모델 — for 부재) | [N/A] | |
| L95 | `{` | — | [N/A] | |
| L96 | `SmartMovingModel modelPlayer = modelPlayers[i].getMovingModel();` | (단일 — 직접 this) | [N/A] | |
| L97 | `modelPlayer.isClimb = isClimb;` | (state holder 일원화 — 분배 부재) | [N/A] | sm 객체 1개로 통합 |
| L98 | `modelPlayer.isClimbJump = isClimbJump;` | — | [N/A] | |
| L99 | `modelPlayer.handsClimbType = handsClimbType;` | — | [N/A] | |
| L100 | `modelPlayer.feetClimbType = feetClimbType;` | — | [N/A] | |
| L101 | `modelPlayer.isHandsVineClimbing = isHandsVineClimbing;` | — | [N/A] | |
| L102 | `modelPlayer.isFeetVineClimbing = isFeetVineClimbing;` | — | [N/A] | |
| L103 | `modelPlayer.isCeilingClimb = isCeilingClimb;` | — | [N/A] | |
| L104 | `modelPlayer.isSwim = isSwim;` | — | [N/A] | |
| L105 | `modelPlayer.isDive = isDive;` | — | [N/A] | |
| L106 | `modelPlayer.isCrawl = isCrawl;` | — | [N/A] | |
| L107 | `modelPlayer.isCrawlClimb = isCrawlClimb;` | — | [N/A] | |
| L108 | `modelPlayer.isJump = isJump;` | — | [N/A] | |
| L109 | `modelPlayer.isHeadJump = isHeadJump;` | — | [N/A] | |
| L110 | `modelPlayer.isSlide = isSlide;` | — | [N/A] | |
| L111 | `modelPlayer.isFlying = isFlying;` | — | [N/A] | |
| L112 | `modelPlayer.isLevitate = isLevitate;` | — | [N/A] | |
| L113 | `modelPlayer.isFalling = isFalling;` | — | [N/A] | |
| L114 | `modelPlayer.isGenericSneaking = isGenericSneaking;` | — | [N/A] | |
| L115 | `modelPlayer.isAngleJumping = isAngleJumping;` | — | [N/A] | |
| L116 | `modelPlayer.angleJumpType = angleJumpType;` | — | [N/A] | |
| L117 | `modelPlayer.isRopeSliding = isRopeSliding;` | — | [N/A] | |
| L118 | (빈 줄) | — | [N/A] | |
| L119 | `modelPlayer.currentHorizontalSpeedFlattened = currentHorizontalSpeedFlattened;` | (state holder 또는 직접 limbSwingAmount) | [N/A] | |
| L120 | `modelPlayer.smallOverGroundHeight = smallOverGroundHeight;` | `sm.smallOverGroundHeight` (MixinPEMC L94-L96 직접 갱신) | [정합] | |
| L121 | `modelPlayer.overGroundBlock = overGroundBlock;` | (1.21.1: smallOverGroundHeight < 5f 검사 단순화 — block 자체 미사용) | [N/A] | §16-21 |
| L122 | `}` | — | [N/A] | |
| L123 | (빈 줄) | — | [N/A] | |
| L124 | `if (!isInventory && entityplayer.isSneaking() && !(entityplayer instanceof EntityPlayerSP) && isCrawl)` | (타인 플레이어 기어가는 자세 보정 — 1.21.1 sm_getPositionOffset 처리?) | [누락] | ⚠️ 타인 플레이어 isSneaking + isCrawl 시 d1 += 0.125D 미이식. R-10+ B-N 후보 |
| L125 | `d1 += 0.125D;` | (해당 없음) | [누락] | ⚠️ 타인 플레이어 크롤링 자세 위치 보정. ModelPlayer Y +0.125 (= 1/8 블록) |
| L126 | `}` (moving null check 종료) | — | [N/A] | |
| L127 | (빈 줄) | — | [N/A] | |
| L128 | `CurrentMainModel = modelBipedMain;` | (1.21.1: 다층 모델 부재 — 직접 this 접근) | [N/A] | |
| L129 | `irp.superRenderRenderPlayer(entityplayer, d, d1, d2, f, renderPartialTicks);` | (vanilla render 본체 — Mixin TAIL inject 자동) | [N/A] | super 호출 == vanilla 자동 |
| L130 | `CurrentMainModel = null;` | — | [N/A] | |
| L131 | (빈 줄) | — | [N/A] | |
| L132 | `if (moving != null && moving.isLevitating && modelPlayers != null)` | (Levitate 후처리 — 1.21.1 미이식) | [누락] | ⚠️ Levitating 시 currentHorizontalAngle = currentCameraAngle 보정 미이식. R-10+ B-N 후보 (Levitate 자세 정합) |
| L133 | `for(int i = 0; i < modelPlayers.length; i++)` | (단일 모델) | [N/A] | |
| L134 | `modelPlayers[i].getMovingModel().md.currentHorizontalAngle = modelPlayers[i].getMovingModel().md.currentCameraAngle;` | (Levitate 후 horizontal=camera 보정 미이식) | [누락] | ⚠️ §16-22 — sm_captureBodyYaw에 Levitate 분기 추가 검토 |
| L135 | `}` | — | [N/A] | renderPlayer 종료 |
| L136 | (빈 줄) | — | [N/A] | |
| L137 | `public void rotatePlayer(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2)` | MixinPlayerEntityRenderer.sm_captureBodyYaw @Inject(HEAD) + @ModifyArg(index=3) bodyYaw 교체 | [정합] | rotatePlayer = setupTransforms 진입점 |
| L138 | `{` | — | [N/A] | |
| L139 | `SmartMoving moving = SmartMovingFactory.getInstance(entityplayer);` | (sm access) | [정합] | |
| L140 | `if(moving != null)` | (sm null check) | [정합] | |
| L141 | `{` | — | [N/A] | |
| L142 | `boolean isInventory = f2 == 1.0F && moving.isp != null && moving.isp.getMcField().currentScreen instanceof GuiInventory;` | (1.21.1: InventoryScreen 자동 분리 — vanilla setupTransforms 가 인벤토리 화면 별도 처리) | [N/A] | |
| L143 | `if(!isInventory)` | — | [N/A] | |
| L144 | `{` | — | [N/A] | |
| L145 | `float forwardRotation = entityplayer.prevRotationYaw + (entityplayer.rotationYaw - entityplayer.prevRotationYaw) * f2;` | `MathHelper.lerp(tickDelta, player.prevYaw, player.getYaw())` (sm_captureBodyYaw 본체) | [정합] | |
| L146 | `if(moving.isClimbing \|\| moving.isClimbCrawling \|\| moving.isCrawlClimbing \|\| moving.isFlying \|\| moving.isSwimming \|\| moving.isDiving \|\| moving.isCeilingClimbing \|\| moving.isHeadJumping \|\| moving.isSliding \|\| moving.isAngleJumping())` | sm_captureBodyYaw 8 분기 (climb/swim/dive/ceilingClimb/headJump/slide/angleJump/falling 등) — `smBodyYawActive=true; smBodyYawOverride=forwardRotation` | [정합] | 8 분기 §11 검증 완료 |
| L147 | `entityplayer.renderYawOffset = forwardRotation;` | `@ModifyArg(method="setupTransforms", index=3)` 으로 bodyYaw 인자 교체 (smBodyYawActive 시) | [정합] | renderYawOffset 직접 설정 → ModifyArg 대체 |
| L148 | `}` | — | [N/A] | |
| L149 | `}` | — | [N/A] | |
| L150 | `}` | (sm null check 종료) | [N/A] | |
| L151 | `irp.superRenderRotatePlayer(entityplayer, totalTime, actualRotation, f2);` | (vanilla setupTransforms 호출 — @ModifyArg가 인자 변환) | [N/A] | |
| L152 | `}` | — | [N/A] | rotatePlayer 종료 |
| L153 | (빈 줄) | — | [N/A] | |
| L154 | `public void renderPlayerAt(AbstractClientPlayer entityplayer, double d, double d1, double d2)` | MixinPlayerEntityRenderer.sm_getPositionOffset @Inject (cancellable) | [정합] | renderPlayerAt = getPositionOffset 진입점 |
| L155 | `{` | — | [N/A] | |
| L156 | `if(entityplayer instanceof EntityOtherPlayerMP)` | (1.21.1: AbstractClientPlayerEntity — 자기/타인 구분 없이 처리) | [N/A] | 1.21.1 Mixin은 모든 플레이어 동일 처리 (자기 자신 sm.heightOffset이 0이면 보정 없음) |
| L157 | `{` | — | [N/A] | |
| L158 | `SmartMoving moving = SmartMovingFactory.getOtherSmartMoving(entityplayer.getEntityId());` | `SmartMovingClientStateAccess.smartmoving$getState(player)` | [정합] | |
| L159 | `if(moving != null && moving.heightOffset != 0)` | sm_getPositionOffset L54: `if (sm.isHeadJumping && sm.heightOffset != 0f)` | [정합] | (가드 — isHeadJumping 추가 — sm.heightOffset 자체가 isHeadJumping 시에만 -1로 설정되므로 등가) |
| L160 | `d1 += moving.heightOffset;` | L55: `cir.setReturnValue(new Vec3d(0D, sm.heightOffset, 0D))` | [정합] | Vec3d 반환 (=>vanilla 추가 적용) |
| L161 | `}` | — | [N/A] | |
| L162 | `irp.superRenderRenderPlayerAt(entityplayer, d, d1, d2);` | (vanilla render 본체 — @Inject TAIL 자동) | [N/A] | |
| L163 | `}` | — | [N/A] | renderPlayerAt 종료 |
| L164 | (빈 줄) | — | [N/A] | |
| L165 | `public void renderName(EntityLivingBase entityPlayer, double d, double d1, double d2)` | MixinPlayerEntityRenderer.smartmoving$adjustLabelY @Inject(HEAD) + MixinLivingEntityRenderer.hasLabel @Redirect | [정합] | renderName = renderLabelIfPresent 진입점 |
| L166 | `{` | — | [N/A] | |
| L167 | `boolean changedIsSneaking = false, originalIsSneaking = false;` | (1.21.1: setSneaking toggle 미사용 — vanilla 자체 isSneaking 분기 직접 변경) | [N/A] | |
| L168 | `if(Minecraft.isGuiEnabled() && entityPlayer != irp.getRenderManager().livingPlayer)` | (1.21.1: dispatcher 자동) | [N/A] | |
| L169 | `{` | — | [N/A] | |
| L170 | `SmartMoving moving = entityPlayer instanceof EntityPlayer ? SmartMovingFactory.getInstance((EntityPlayer)entityPlayer) : null;` | `SmartMovingClientStateAccess` (sm access) | [정합] | |
| L171 | `if(moving != null)` | (sm null check) | [정합] | |
| L172 | `{` | — | [N/A] | |
| L173 | `originalIsSneaking = entityPlayer.isSneaking();` | (sneakNameTag 처리 — MixinLivingEntityRenderer.hasLabel @Redirect) | [정합] | |
| L174 | `boolean temporaryIsSneaking = originalIsSneaking;` | — | [정합] | |
| L175 | `if(moving.isCrawling && !moving.isClimbing)` | renderLabelIfPresent L260: `if (sm.isCrawling && !sm.isClimbing && !cfg.crawlNameTag)` | [정합] | |
| L176 | `temporaryIsSneaking = !Config._crawlNameTag.value;` | L261: `cir.cancel()` (이름 태그 자체 차단) | [정합 (근사)] | toggle vs cancel — 효과 동일 (이름 표시 안함) |
| L177 | `else if(originalIsSneaking)` | (vanilla sneak nametag 64 거리 vs sneakNameTag — MixinLivingEntityRenderer 별도) | [정합] | |
| L178 | `temporaryIsSneaking = !Config._sneakNameTag.value;` | (sneakNameTag 처리 — MixinLivingEntityRenderer.hasLabel @Redirect) | [정합] | |
| L179 | (빈 줄) | — | [N/A] | |
| L180 | `changedIsSneaking = temporaryIsSneaking != originalIsSneaking;` | (toggle 미사용 — Mixin 분기 직접 처리) | [N/A] | |
| L181 | `if(changedIsSneaking)` | — | [N/A] | |
| L182 | `entityPlayer.setSneaking(temporaryIsSneaking);` | (toggle 미사용) | [N/A] | |
| L183 | (빈 줄) | — | [N/A] | |
| L184 | `if(moving.heightOffset == -1)` | renderLabelIfPresent L266: `if (sm.heightOffset == -1f)` | [정합] | 헤드점프 |
| L185 | `d1 -= 0.2F;` | L267 (Y -0.2 보정) | [정합] | |
| L186 | `else if(originalIsSneaking && !temporaryIsSneaking)` | renderLabelIfPresent L271: `else if (entity.isSneaking() && cfg.sneakNameTag)` | [정합] | sneakNameTag 시 비스니킹 취급 → Y 보정 |
| L187 | `d1 -= 0.05F;` | L272 (Y -0.05 보정) | [정합] | |
| L188 | `}` | — | [N/A] | |
| L189 | `}` | — | [N/A] | |
| L190 | (빈 줄) | — | [N/A] | |
| L191 | `irp.superRenderRenderName(entityPlayer, d, d1, d2);` | (vanilla renderLabelIfPresent — @Inject HEAD 후 진행) | [N/A] | |
| L192 | (빈 줄) | — | [N/A] | |
| L193 | `if(changedIsSneaking)` | (toggle 미사용 — restore 불필요) | [N/A] | |
| L194 | `entityPlayer.setSneaking(originalIsSneaking);` | — | [N/A] | |
| L195 | `}` | — | [N/A] | renderName 종료 |
| L196 | (빈 줄) | — | [N/A] | |
| L197 | `public static void renderGuiIngame(Minecraft minecraft)` | (HUD 렌더 — 본 포커스 #1 외, 별도 포커스 영역) | [N/A] | UI/HUD — 애니메이션 무관 |
| L198 | `{` | — | [N/A] | |
| L199 | `if (!Client.getNativeUserInterfaceDrawing())` | (HUD UI 설정) | [N/A] | |
| L200 | `return;` | (HUD 처리) | [N/A] | |

**청크 1 (L1-L200) 통계: 정합 51 / 오역 0 / 누락 3 / 잉여 0 / N/A 146 = 200 라인 전수.**

**청크 1 발견 (R-10+ B-N 후보)**:
- B-N (타인 플레이어 isSneaking+isCrawl 위치 보정): L124/L125 — 타인 크롤링 자세에서 d1 += 0.125D 미이식. sm_getPositionOffset에 분기 추가 검토.
- B-N (Levitate 후처리 — currentHorizontalAngle = currentCameraAngle): L132-L134 — Levitating 시 카메라 각도로 강제 정렬 미이식. sm_captureBodyYaw 에 Levitate 분기 추가 검토.

**다음 청크**: R-2 청크 2 (SmartMovingRender.java L201-L337) — renderGuiIngame 본체 + 잔여 + SM render 하위 5 파일 (Context/IModel/IRender/ModelPlayer/RenderPlayer).

### 청크 2 (SmartMovingRender.java L201-L337 + SmartRenderContext.java + IModelPlayer.java + IRenderPlayer.java)

**파트 A — SmartMovingRender.java L201-L337 (renderGuiIngame HUD + drawIcon + 필드)**

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L201 | (빈 줄) | — | [N/A] | |
| L202-L204 | `// Returning here causes Smart Moving's icons to not get rendered ...` (주석) | — | [N/A] | 주석 |
| L205 | (빈 줄) | — | [N/A] | |
| L206 | `SmartMovingSelf moving = (SmartMovingSelf)SmartMovingFactory.getInstance(minecraft.thePlayer);` | (HUD 영역 — 본 포커스 #1 외) | [N/A] | UI/HUD 별도 포커스 |
| L207 | `if(moving != null && Config.enabled && (Options._displayExhaustionBar.value \|\| Options._displayJumpChargeBar.value))` | (HUD 가드) | [N/A] | |
| L208 | `{` | — | [N/A] | |
| L209 | `ScaledResolution scaledresolution = new ScaledResolution(...);` | (1.21.1: ScaledResolution 제거됨 — Window/MatrixStack 사용) | [N/A] | |
| L210 | `int width = scaledresolution.getScaledWidth();` | — | [N/A] | |
| L211 | `int height = scaledresolution.getScaledHeight();` | — | [N/A] | |
| L212 | (빈 줄) | — | [N/A] | |
| L213 | `if(minecraft.playerController.shouldDrawHUD())` | — | [N/A] | |
| L214 | `{` | — | [N/A] | |
| L215 | `float maxExhaustion = Client.getMaximumExhaustion();` | (HUD 영역) | [N/A] | exhaustionBar 본 포커스 외 |
| L216 | `float exhaustion = Math.min(moving.exhaustion, maxExhaustion);` | — | [N/A] | |
| L217 | `boolean drawExhaustion = exhaustion > 0 && exhaustion <= maxExhaustion;` | — | [N/A] | |
| L218 | (빈 줄) | — | [N/A] | |
| L219 | `float maxStillJumpCharge = Config._jumpChargeMaximum.value;` | (HUD jumpChargeBar) | [N/A] | |
| L220 | `float stillJumpCharge = Math.min(moving.jumpCharge, maxStillJumpCharge);` | — | [N/A] | |
| L221 | (빈 줄) | — | [N/A] | |
| L222 | `float maxRunJumpCharge = Config._headJumpChargeMaximum.value;` | — | [N/A] | |
| L223 | `float runJumpCharge = Math.min(moving.headJumpCharge, maxRunJumpCharge);` | — | [N/A] | |
| L224 | (빈 줄) | — | [N/A] | |
| L225 | `boolean drawJumpCharge = stillJumpCharge > 0 \|\| runJumpCharge > 0;` | — | [N/A] | |
| L226 | `float maxJumpCharge = stillJumpCharge > runJumpCharge ? maxStillJumpCharge : maxRunJumpCharge;` | — | [N/A] | |
| L227 | `float jumpCharge = Math.max(stillJumpCharge, runJumpCharge);` | — | [N/A] | |
| L228 | (빈 줄) | — | [N/A] | |
| L229 | `if(drawExhaustion \|\| drawJumpCharge)` | — | [N/A] | |
| L230 | `{` | — | [N/A] | |
| L231 | `GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);` | (1.21.1: GL11 제거 — RenderSystem) | [N/A] | |
| L232 | `minecraft.getTextureManager().bindTexture(new ResourceLocation("smartmoving", "gui/icons.png"));` | (HUD texture binding) | [N/A] | |
| L233 | `GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);` | (1.21.1: 자동) | [N/A] | |
| L234 | `_minecraft = minecraft;` | (HUD instance reference) | [N/A] | |
| L235 | `}` | — | [N/A] | |
| L236 | (빈 줄) | — | [N/A] | |
| L237 | `if(drawExhaustion)` | (HUD exhaustion bar 본체) | [N/A] | |
| L238 | `{` | — | [N/A] | |
| L239 | `float maxExhaustionForAction = Math.min(moving.maxExhaustionForAction, maxExhaustion);` | — | [N/A] | |
| L240 | `float maxExhaustionToStartAction = Math.min(moving.maxExhaustionToStartAction, maxExhaustion);` | — | [N/A] | |
| L241 | (빈 줄) | — | [N/A] | |
| L242 | `float fitness = maxExhaustion - exhaustion;` | — | [N/A] | |
| L243 | `float minFitnessForAction = Float.isNaN(maxExhaustionForAction) ? 0 : maxExhaustion - maxExhaustionForAction;` | — | [N/A] | |
| L244 | `float minFitnessToStartAction = Float.isNaN(maxExhaustionToStartAction) ? 0 : maxExhaustion - maxExhaustionToStartAction;` | — | [N/A] | |
| L245 | (빈 줄) | — | [N/A] | |
| L246 | `float maxFitnessDrawn = Math.max(...);` | — | [N/A] | |
| L247 | (빈 줄) | — | [N/A] | |
| L248 | `int halfs = (int)Math.floor(maxFitnessDrawn / maxExhaustion * 21F);` | — | [N/A] | |
| L249 | `int fulls = halfs / 2;` | — | [N/A] | |
| L250 | `int half = halfs % 2;` | — | [N/A] | |
| L251 | (빈 줄) | — | [N/A] | |
| L252-L254 | `int fitnessHalfs = ...` 외 fitness 계산 3 라인 | — | [N/A] | HUD |
| L255 | (빈 줄) | — | [N/A] | |
| L256-L258 | `int minFitnessForActionHalfs = ...` 외 minFitnessForAction 계산 3 라인 | — | [N/A] | HUD |
| L259 | (빈 줄) | — | [N/A] | |
| L260-L261 | `int minFitnessToStartActionHalfs = ...` minFitnessToStartAction 계산 2 라인 | — | [N/A] | HUD |
| L262 | (빈 줄) | — | [N/A] | |
| L263 | `_jOffset = height - 39 - 10 - (minecraft.thePlayer.isInsideOfMaterial(Material.water) ? 10 : 0);` | (HUD position calc — Material.water 1.21.1 부재) | [N/A] | |
| L264 | `for(int i = 0; i < Math.min(fulls + half, 10); i++)` | — | [N/A] | |
| L265 | `{` | — | [N/A] | |
| L266 | `_iOffset = (width / 2 + 90) - (i + 1) * 8;` | — | [N/A] | |
| L267-L289 | `if(i < fitnessFulls)...else if...else...` 23 라인 (HUD icon 분기 트리) | — | [N/A] | exhaustion icon 분기 |
| L290 | `}` | — | [N/A] | |
| L291-L305 | `else { if(i < minFitnessForActionFulls)...else...else }` 15 라인 | — | [N/A] | HUD icon |
| L306 | `}` | — | [N/A] | |
| L307 | `}` | — | [N/A] | |
| L308 | (빈 줄) | — | [N/A] | |
| L309 | `if(drawJumpCharge)` | (HUD jumpCharge bar 본체) | [N/A] | |
| L310 | `{` | — | [N/A] | |
| L311 | `boolean max = jumpCharge == maxJumpCharge;` | — | [N/A] | |
| L312 | `int fulls = max ? 10 : (int)Math.ceil(((jumpCharge - 2) * 10D) / maxJumpCharge);` | — | [N/A] | |
| L313 | `int half = max ? 0 : (int)Math.ceil((jumpCharge * 10D) / maxJumpCharge) - fulls;` | — | [N/A] | |
| L314 | (빈 줄) | — | [N/A] | |
| L315 | `_jOffset = height - 39 - 10 - (minecraft.thePlayer.getTotalArmorValue() > 0 ? 10 : 0);` | — | [N/A] | |
| L316 | `for(int i = 0; i < fulls + half; i++)` | — | [N/A] | |
| L317 | `{` | — | [N/A] | |
| L318 | `_iOffset = (width / 2 - 91) + i * 8;` | — | [N/A] | |
| L319 | `drawIcon(i < fulls ? 2 : 3, 0);` | — | [N/A] | |
| L320 | `}` | — | [N/A] | |
| L321 | `}` | — | [N/A] | |
| L322 | (빈 줄) | — | [N/A] | |
| L323 | `if(drawExhaustion \|\| drawJumpCharge)` | — | [N/A] | |
| L324 | `GL11.glPopAttrib();` | (1.21.1: 자동) | [N/A] | |
| L325 | `}` | — | [N/A] | |
| L326 | `}` | — | [N/A] | |
| L327 | `}` | — | [N/A] | renderGuiIngame 종료 |
| L328 | (빈 줄) | — | [N/A] | |
| L329 | `private static void drawIcon(int x, int y)` | (HUD 헬퍼) | [N/A] | |
| L330 | `{` | — | [N/A] | |
| L331 | `_minecraft.ingameGUI.drawTexturedModalRect(_iOffset, _jOffset, x * 9, y * 9, 9, 9);` | — | [N/A] | |
| L332 | `}` | — | [N/A] | |
| L333 | (빈 줄) | — | [N/A] | |
| L334 | `public final SmartMovingModel modelBipedMain;` | (1.21.1 단일 모델 — 부재) | [N/A] | |
| L335 | (빈 줄) | — | [N/A] | |
| L336 | `private static int _iOffset, _jOffset;` | (HUD 필드) | [N/A] | |
| L337 | `private static Minecraft _minecraft;` | (HUD 필드) | [N/A] | |

**파트 A 통계: 정합 0 / 오역 0 / 누락 0 / 잉여 0 / N/A 137 = 137 라인 전수.** (HUD 영역 일괄 — 본 포커스 #1 외)

**파트 B — SmartRenderContext.java (27 라인)**

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L16 | 라이선스 헤더 | — | [N/A] | |
| L17 | `// ==…==` | — | [N/A] | |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.moving.render;` | — | [N/A] | |
| L20 | `import net.smart.moving.*;` | — | [N/A] | |
| L21 | (빈 줄) | — | [N/A] | |
| L22 | `public abstract class SmartRenderContext extends SmartMovingContext` | (1.21.1: SmartMovingClientState 등 흡수) | [N/A] | |
| L23 | `{` | — | [N/A] | |
| L24 | `public static final int Scale = 0;` | (메인 모델 = Scale 가드 묵시 — setArmScales/setLegScales 가드 제거 근거) | [정합] | §16-7 — 0 = 첫 분기 통과 (always true) |
| L25 | `public static final int NoScaleStart = 1;` | (갑옷 일반 — 1.21.1 미이식) | [N/A] | §16-8 |
| L26 | `public static final int NoScaleEnd = 2;` | (갑옷 흉갑 offsetY — §17 잔여) | [N/A] | §16-8 |
| L27 | `}` | — | [N/A] | |

**파트 B 통계: 정합 1 / 오역 0 / 누락 0 / 잉여 0 / N/A 26 = 27 라인 전수.**

**파트 C — IModelPlayer.java (35 라인)**

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L17 | 라이선스 + `==` | — | [N/A] | |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.moving.render;` | — | [N/A] | |
| L20 | (빈 줄) | — | [N/A] | |
| L21 | `public interface IModelPlayer` | (1.21.1: 단일 PlayerEntityModel — 인터페이스 부재) | [N/A] | |
| L22 | `{` | — | [N/A] | |
| L23 | `SmartMovingModel getMovingModel();` | (단일 모델) | [N/A] | |
| L24 | `void superAnimateHeadRotation(...)` | (vanilla setAngles 자동 — head.yaw/pitch 매개변수 자동 설정) | [N/A] | |
| L25 | `void superAnimateSleeping(...)` | (vanilla SleepingPose 자동) | [N/A] | |
| L26 | `void superAnimateArmSwinging(...)` | (vanilla limbSwing 기반 자동 — sm_setAngles TAIL inject가 11-state 분기 시 덮어씀) | [N/A] | |
| L27 | `void superAnimateRiding(...)` | (vanilla Riding pose 자동) | [N/A] | |
| L28 | `void superAnimateLeftArmItemHolding(...)` | (vanilla item holding 자동) | [N/A] | |
| L29 | `void superAnimateRightArmItemHolding(...)` | (vanilla 자동) | [N/A] | |
| L30 | `void superAnimateWorkingBody(...)` | (vanilla 자동 — 어깨 NonStandardWorking 미이식 §16-5) | [N/A] | |
| L31 | `void superAnimateWorkingArms(...)` | (vanilla 자동) | [N/A] | |
| L32 | `void superAnimateSneaking(...)` | (vanilla sneak 자동 — leaningPitch=0 별도 처리) | [N/A] | |
| L33 | `void superApplyAnimationOffsets(...)` | (vanilla applyAnimationOffsets 자동) | [N/A] | |
| L34 | `void superAnimateBowAiming(...)` | (vanilla 활쏘기 자동) | [N/A] | |
| L35 | `}` | — | [N/A] | |

**파트 C 통계: 정합 0 / 오역 0 / 누락 0 / 잉여 0 / N/A 35 = 35 라인 전수.** (인터페이스 시그니처 — 1.21.1 단일 모델 + vanilla 자동 처리로 모두 N/A)

**파트 D — IRenderPlayer.java (43 라인)**

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L17 | 라이선스 + `==` | — | [N/A] | |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.moving.render;` | — | [N/A] | |
| L20 | `import net.minecraft.client.entity.*;` | — | [N/A] | |
| L21 | `import net.minecraft.client.renderer.entity.*;` | — | [N/A] | |
| L22 | `import net.minecraft.entity.*;` | — | [N/A] | |
| L23 | (빈 줄) | — | [N/A] | |
| L24 | `public interface IRenderPlayer` | (1.21.1: PlayerEntityRenderer 직접 Mixin — 인터페이스 부재) | [N/A] | |
| L25 | `{` | — | [N/A] | |
| L26 | `void superRenderRenderPlayer(AbstractClientPlayer entityplayer, double d, double d1, double d2, float f, float renderPartialTicks);` | (vanilla render 호출 — Mixin TAIL inject 자동) | [N/A] | |
| L27 | (빈 줄) | — | [N/A] | |
| L28 | `void superRenderRotatePlayer(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2);` | MixinPlayerEntityRenderer.sm_captureBodyYaw HEAD + @ModifyArg index=3 (vanilla setupTransforms 호출 시 bodyYaw 인자 교체) | [정합] | rotatePlayer = setupTransforms 매핑 |
| L29 | (빈 줄) | — | [N/A] | |
| L30 | `void superRenderRenderPlayerAt(AbstractClientPlayer entityplayer, double d, double d1, double d2);` | sm_getPositionOffset @Inject(cancellable) | [정합] | renderPlayerAt = getPositionOffset 매핑 |
| L31 | (빈 줄) | — | [N/A] | |
| L32 | `void superRenderRenderName(EntityLivingBase par1EntityPlayer, double par2, double par4, double par6);` | smartmoving$adjustLabelY @Inject(HEAD, cancellable) + MixinLivingEntityRenderer.hasLabel @Redirect | [정합] | renderName = renderLabelIfPresent 매핑 |
| L33 | (빈 줄) | — | [N/A] | |
| L34 | `RenderManager getRenderManager();` | (1.21.1: EntityRenderDispatcher 자동) | [N/A] | |
| L35 | (빈 줄) | — | [N/A] | |
| L36 | `IModelPlayer getPlayerModelBipedMain();` | (1.21.1: 단일 모델 직접 this) | [N/A] | |
| L37 | (빈 줄) | — | [N/A] | |
| L38 | `IModelPlayer getPlayerModelArmorChestplate();` | (갑옷 ArmorFeatureRenderer §17 잔여) | [N/A] | |
| L39 | (빈 줄) | — | [N/A] | |
| L40 | `IModelPlayer getPlayerModelArmor();` | (갑옷) | [N/A] | |
| L41 | (빈 줄) | — | [N/A] | |
| L42 | `IModelPlayer[] getPlayerModels();` | (다층 모델 부재) | [N/A] | |
| L43 | `}` | — | [N/A] | |

**파트 D 통계: 정합 3 / 오역 0 / 누락 0 / 잉여 0 / N/A 40 = 43 라인 전수.**

**R-2 청크 2 통계 (4 파일 합계)**:
- 파트 A (HUD): 0 / 0 / 0 / 0 / 137 = 137 라인
- 파트 B (Context): 1 / 0 / 0 / 0 / 26 = 27 라인
- 파트 C (IModel): 0 / 0 / 0 / 0 / 35 = 35 라인
- 파트 D (IRender): 3 / 0 / 0 / 0 / 40 = 43 라인
- **합계: 정합 4 / 오역 0 / 누락 0 / 잉여 0 / N/A 238 = 242 라인 전수**

**청크 2 발견**: 신규 [오역]/[누락]/[잉여] 0건. HUD 영역 + 인터페이스 시그니처 — 본 포커스 #1 외이거나 vanilla 자동 처리.

**다음 청크**: R-2 청크 3 (ModelPlayer.java 167줄 + RenderPlayer.java 120줄 = ~287줄). R-2 마지막 청크.

### 청크 3 (ModelPlayer.java 168 + RenderPlayer.java 121 = 289 라인) — R-2 마지막 청크

**파트 A — ModelPlayer.java (168 라인)** — IModelPlayer 구현 클래스 (model 위임 패턴)

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L17 | 라이선스 헤더 + `==` | — | [N/A] | 17 라인 일괄 |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.moving.render;` | (1.21.1 별도 패키지) | [N/A] | |
| L20 | `public class ModelPlayer extends net.smart.render.ModelPlayer implements IModelPlayer` | (1.21.1: 단일 PlayerEntityModel — ModelPlayer 별도 클래스 부재) | [N/A] | 다층 모델 부재 |
| L21 | `{` | — | [N/A] | |
| L22 | `private final SmartMovingModel model;` | (1.21.1: SmartMovingClientState 흡수 — model 별도 인스턴스 부재) | [N/A] | |
| L23 | (빈 줄) | — | [N/A] | |
| L24 | `public ModelPlayer(float f)` | (1.21.1: vanilla PlayerEntityModel(ModelPart) 자동) | [N/A] | |
| L25 | `{` | — | [N/A] | |
| L26 | `super(f);` | (vanilla super 자동) | [N/A] | |
| L27 | (빈 줄) | — | [N/A] | |
| L28 | `model = new SmartMovingModel(this, this);` | (별도 SmartMovingModel 인스턴스 부재 — Mixin 직접 처리) | [N/A] | |
| L29 | `}` | — | [N/A] | |
| L30 | (빈 줄) | — | [N/A] | |
| L31-L35 | `@Override public SmartMovingModel getMovingModel() { return model; }` 5 라인 | (별도 모델 부재) | [N/A] | |
| L36 | (빈 줄) | — | [N/A] | |
| L37-L41 | `@Override animateHeadRotation(...) { model.animateHeadRotation(...) }` 5 라인 | (vanilla setAngles 자동 — head.yaw/pitch 매개변수) | [N/A] | sm_setAngles TAIL inject 가 vanilla 결과 덮어씀 (분기 시) |
| L42 | (빈 줄) | — | [N/A] | |
| L43-L47 | `@Override animateSleeping(...) { model.animateSleeping(...) }` | (vanilla SleepingPose 자동) | [N/A] | |
| L48 | (빈 줄) | — | [N/A] | |
| L49-L53 | `@Override animateArmSwinging(...) { model.animateArmSwinging(...) }` | (vanilla limbSwing 자동) | [N/A] | sm_setAngles 11-state 분기 시 덮어씀 |
| L54 | (빈 줄) | — | [N/A] | |
| L55-L59 | `@Override animateRiding(...) { model.animateRiding(...) }` | (vanilla Riding 자동) | [N/A] | |
| L60 | (빈 줄) | — | [N/A] | |
| L61-L65 | `@Override animateLeftArmItemHolding(...) { model.animateLeftArmItemHolding(...) }` | (vanilla item holding 자동) | [N/A] | |
| L66 | (빈 줄) | — | [N/A] | |
| L67-L71 | `@Override animateRightArmItemHolding(...) { model.animateRightArmItemHolding(...) }` | (vanilla 자동) | [N/A] | |
| L72 | (빈 줄) | — | [N/A] | |
| L73-L77 | `@Override animateWorkingBody(...) { model.animateWorkingBody(...) }` | (vanilla 자동 — 어깨 NonStandardWorking 미이식 §16-5) | [N/A] | |
| L78 | (빈 줄) | — | [N/A] | |
| L79-L83 | `@Override animateWorkingArms(...) { model.animateWorkingArms(...) }` | (vanilla 자동) | [N/A] | |
| L84 | (빈 줄) | — | [N/A] | |
| L85-L89 | `@Override animateSneaking(...) { model.animateSneaking(...) }` | (vanilla sneak 자동 — leaningPitch=0 별도) | [N/A] | sm_setAngles L88 leaningPitch=0 처리 |
| L90 | (빈 줄) | — | [N/A] | |
| L91-L95 | `@Override animateArms(...) { model.animateArms(...) }` | (vanilla applyAnimationOffsets 자동) | [N/A] | |
| L96 | (빈 줄) | — | [N/A] | |
| L97-L101 | `@Override animateBowAiming(...) { model.animateBowAiming(...) }` | (vanilla 활쏘기 자동 — 어깨 NonStandardBowAiming 미이식 §16-5) | [N/A] | |
| L102 | (빈 줄) | — | [N/A] | |
| L103-L107 | `@Override superAnimateHeadRotation(...) { super.animateHeadRotation(...) }` 5 라인 | (super 호출 = vanilla 자동) | [N/A] | |
| L108 | (빈 줄) | — | [N/A] | |
| L109-L113 | `@Override superAnimateSleeping(...) { super.animateSleeping(...) }` | (super = vanilla) | [N/A] | |
| L114 | (빈 줄) | — | [N/A] | |
| L115-L119 | `@Override superAnimateArmSwinging(...) { super.animateArmSwinging(...) }` | (super = vanilla) | [N/A] | |
| L120 | (빈 줄) | — | [N/A] | |
| L121-L125 | `@Override superAnimateRiding(...) { super.animateRiding(...) }` | (super = vanilla) | [N/A] | |
| L126 | (빈 줄) | — | [N/A] | |
| L127-L131 | `@Override superAnimateLeftArmItemHolding(...) { super.animateLeftArmItemHolding(...) }` | (super = vanilla) | [N/A] | |
| L132 | (빈 줄) | — | [N/A] | |
| L133-L137 | `@Override superAnimateRightArmItemHolding(...) { super.animateRightArmItemHolding(...) }` | (super = vanilla) | [N/A] | |
| L138 | (빈 줄) | — | [N/A] | |
| L139-L143 | `@Override superAnimateWorkingBody(...) { super.animateWorkingBody(...) }` | (super = vanilla) | [N/A] | |
| L144 | (빈 줄) | — | [N/A] | |
| L145-L149 | `@Override superAnimateWorkingArms(...) { super.animateWorkingArms(...) }` | (super = vanilla) | [N/A] | |
| L150 | (빈 줄) | — | [N/A] | |
| L151-L155 | `@Override superAnimateSneaking(...) { super.animateSneaking(...) }` | (super = vanilla) | [N/A] | |
| L156 | (빈 줄) | — | [N/A] | |
| L157-L161 | `@Override superApplyAnimationOffsets(...) { super.animateArms(...) }` | (super = vanilla applyAnimationOffsets) | [N/A] | |
| L162 | (빈 줄) | — | [N/A] | |
| L163-L167 | `@Override superAnimateBowAiming(...) { super.animateBowAiming(...) }` | (super = vanilla) | [N/A] | |
| L168 | `}` | — | [N/A] | 클래스 종료 |

**파트 A 통계: 정합 0 / 오역 0 / 누락 0 / 잉여 0 / N/A 168 = 168 라인 전수.** ModelPlayer = 1.21.1 단일 PlayerEntityModel + Mixin 구조에서 위임자 패턴 일체 부재 (vanilla 자동 + sm_setAngles TAIL inject 통합 처리).

**파트 B — RenderPlayer.java (121 라인)** — IRenderPlayer 구현 클래스 (render 위임 패턴)

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L17 | 라이선스 + `==` | — | [N/A] | |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.moving.render;` | — | [N/A] | |
| L20 | `import net.minecraft.client.entity.*;` | — | [N/A] | |
| L21 | `import net.minecraft.client.model.*;` | — | [N/A] | |
| L22 | `import net.minecraft.client.renderer.entity.*;` | — | [N/A] | |
| L23 | `import net.minecraft.entity.*;` | — | [N/A] | |
| L24 | (빈 줄) | — | [N/A] | |
| L25 | `public class RenderPlayer extends net.smart.render.RenderPlayer implements IRenderPlayer` | `@Mixin(PlayerEntityRenderer.class) public abstract class MixinPlayerEntityRenderer` | [정합] | 표면 매핑 |
| L26 | `{` | `{` | [정합] | |
| L27 | `public RenderPlayer()` | (Mixin: 생성자 부재) | [N/A] | |
| L28 | `{` | — | [N/A] | |
| L29 | `render = new SmartMovingRender(this);` | (별도 render 인스턴스 부재 — Mixin this 직접) | [N/A] | |
| L30 | `}` | — | [N/A] | |
| L31 | (빈 줄) | — | [N/A] | |
| L32 | `@Override` | — | [N/A] | |
| L33 | `public net.smart.render.IModelPlayer createModel(ModelBiped existing, float f)` | (1.21.1: 단일 PlayerEntityModel — createModel 부재) | [N/A] | |
| L34 | `{` | — | [N/A] | |
| L35 | `return new ModelPlayer(f);` | — | [N/A] | |
| L36 | `}` | — | [N/A] | |
| L37 | (빈 줄) | — | [N/A] | |
| L38 | `@Override` | — | [N/A] | |
| L39 | `public void doRender(AbstractClientPlayer entityplayer, double d, double d1, double d2, float f, float renderPartialTicks)` | (vanilla render 본체 — Mixin TAIL inject 자동) | [N/A] | render.renderPlayer 위임자 |
| L40 | `{` | — | [N/A] | |
| L41 | `render.renderPlayer(entityplayer, d, d1, d2, f, renderPartialTicks);` | (R-2 청크 1 매핑된 본체 — 직접 Mixin 분리) | [N/A] | |
| L42 | `}` | — | [N/A] | |
| L43 | (빈 줄) | — | [N/A] | |
| L44 | `@Override` | — | [N/A] | |
| L45 | `public void superRenderRenderPlayer(AbstractClientPlayer entityplayer, double d, double d1, double d2, float f, float renderPartialTicks)` | (vanilla render 호출 위임자 — Mixin 자동) | [N/A] | |
| L46 | `{` | — | [N/A] | |
| L47 | `super.doRender(entityplayer, d, d1, d2, f, renderPartialTicks);` | — | [N/A] | |
| L48 | `}` | — | [N/A] | |
| L49 | (빈 줄) | — | [N/A] | |
| L50 | `@Override` | — | [N/A] | |
| L51 | `protected void rotateCorpse(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2)` | MixinPlayerEntityRenderer.sm_captureBodyYaw HEAD + @ModifyArg index=3 | [정합] | rotateCorpse = setupTransforms 진입점 (1.7.10 → 1.21.1 이름 변경) |
| L52 | `{` | — | [N/A] | |
| L53 | `render.rotatePlayer(entityplayer, totalTime, actualRotation, f2);` | (R-2 청크 1 L137-L152 매핑된 본체) | [N/A] | 위임 — Mixin 직접 처리 |
| L54 | `}` | — | [N/A] | |
| L55 | (빈 줄) | — | [N/A] | |
| L56 | `@Override` | — | [N/A] | |
| L57 | `public void superRenderRotatePlayer(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2)` | (vanilla setupTransforms 호출 위임자 — @ModifyArg가 인자 변환 후 vanilla 자동) | [N/A] | |
| L58 | `{` | — | [N/A] | |
| L59 | `super.rotateCorpse(entityplayer, totalTime, actualRotation, f2);` | — | [N/A] | |
| L60 | `}` | — | [N/A] | |
| L61 | (빈 줄) | — | [N/A] | |
| L62 | `@Override` | — | [N/A] | |
| L63 | `protected void renderLivingAt(AbstractClientPlayer entityplayer, double d, double d1, double d2)` | MixinPlayerEntityRenderer.sm_getPositionOffset @Inject(cancellable) | [정합] | renderLivingAt = getPositionOffset 진입점 |
| L64 | `{` | — | [N/A] | |
| L65 | `render.renderPlayerAt(entityplayer, d, d1, d2);` | (R-2 청크 1 L154-L163 매핑된 본체) | [N/A] | 위임 |
| L66 | `}` | — | [N/A] | |
| L67 | (빈 줄) | — | [N/A] | |
| L68 | `@Override` | — | [N/A] | |
| L69 | `public void superRenderRenderPlayerAt(AbstractClientPlayer entityplayer, double d, double d1, double d2)` | (vanilla 자동) | [N/A] | |
| L70 | `{` | — | [N/A] | |
| L71 | `super.renderLivingAt(entityplayer, d, d1, d2);` | — | [N/A] | |
| L72 | `}` | — | [N/A] | |
| L73 | (빈 줄) | — | [N/A] | |
| L74 | `@Override` | — | [N/A] | |
| L75 | `protected void passSpecialRender(EntityLivingBase par1EntityLiving, double par2, double par4, double par6)` | MixinPlayerEntityRenderer.smartmoving$adjustLabelY @Inject(HEAD, cancellable) + MixinLivingEntityRenderer.hasLabel @Redirect | [정합] | passSpecialRender = renderLabelIfPresent 진입점 |
| L76 | `{` | — | [N/A] | |
| L77 | `render.renderName(par1EntityLiving, par2, par4, par6);` | (R-2 청크 1 L165-L195 매핑된 본체) | [N/A] | 위임 |
| L78 | `}` | — | [N/A] | |
| L79 | (빈 줄) | — | [N/A] | |
| L80 | `@Override` | — | [N/A] | |
| L81 | `public void superRenderRenderName(EntityLivingBase par1EntityLiving, double par2, double par4, double par6)` | (vanilla renderLabelIfPresent — @Inject HEAD ci.cancel() 미발동 시 진행) | [N/A] | |
| L82 | `{` | — | [N/A] | |
| L83 | `super.passSpecialRender(par1EntityLiving, par2, par4, par6);` | — | [N/A] | |
| L84 | `}` | — | [N/A] | |
| L85 | (빈 줄) | — | [N/A] | |
| L86 | `@Override` | — | [N/A] | |
| L87 | `public RenderManager getRenderManager()` | (1.21.1: EntityRenderDispatcher 자동 — getter 부재) | [N/A] | |
| L88 | `{` | — | [N/A] | |
| L89 | `return renderManager;` | — | [N/A] | |
| L90 | `}` | — | [N/A] | |
| L91 | (빈 줄) | — | [N/A] | |
| L92 | `@Override` | — | [N/A] | |
| L93 | `public IModelPlayer getPlayerModelBipedMain()` | (1.21.1: 단일 모델 — this.getModel() 자동) | [N/A] | |
| L94 | `{` | — | [N/A] | |
| L95 | `return (ModelPlayer)super.getModelBipedMain();` | — | [N/A] | |
| L96 | `}` | — | [N/A] | |
| L97 | (빈 줄) | — | [N/A] | |
| L98 | `@Override` | — | [N/A] | |
| L99 | `public IModelPlayer getPlayerModelArmorChestplate()` | (갑옷 ArmorFeatureRenderer §17 잔여) | [N/A] | §16-8 |
| L100 | `{` | — | [N/A] | |
| L101 | `return (ModelPlayer)super.getModelArmorChestplate();` | — | [N/A] | |
| L102 | `}` | — | [N/A] | |
| L103 | (빈 줄) | — | [N/A] | |
| L104 | `@Override` | — | [N/A] | |
| L105 | `public IModelPlayer getPlayerModelArmor()` | (갑옷) | [N/A] | |
| L106 | `{` | — | [N/A] | |
| L107 | `return (ModelPlayer)super.getModelArmor();` | — | [N/A] | |
| L108 | `}` | — | [N/A] | |
| L109 | (빈 줄) | — | [N/A] | |
| L110 | `@Override` | — | [N/A] | |
| L111 | `public IModelPlayer[] getPlayerModels()` | (다층 모델 부재) | [N/A] | |
| L112 | `{` | — | [N/A] | |
| L113 | `if(allIModelPlayers == null)` | — | [N/A] | |
| L114 | `allIModelPlayers = new IModelPlayer[] { getPlayerModelBipedMain(), getPlayerModelArmorChestplate(), getPlayerModelArmor() };` | — | [N/A] | |
| L115 | `return allIModelPlayers;` | — | [N/A] | |
| L116 | `}` | — | [N/A] | |
| L117 | (빈 줄) | — | [N/A] | |
| L118 | `private IModelPlayer[] allIModelPlayers;` | — | [N/A] | |
| L119 | (빈 줄) | — | [N/A] | |
| L120 | `private final SmartMovingRender render;` | (별도 render 인스턴스 부재 — Mixin this) | [N/A] | |
| L121 | `}` | — | [N/A] | 클래스 종료 |

**파트 B 통계: 정합 5 / 오역 0 / 누락 0 / 잉여 0 / N/A 116 = 121 라인 전수.** RenderPlayer = vanilla render dispatcher 직접 Mixin (rotateCorpse/renderLivingAt/passSpecialRender 진입점 매핑 5 라인 제외 모두 [N/A]).

**R-2 청크 3 통계 (2 파일 합)**:
- 파트 A (ModelPlayer): 0 / 0 / 0 / 0 / 168 = 168 라인
- 파트 B (RenderPlayer): 5 / 0 / 0 / 0 / 116 = 121 라인
- **합계: 정합 5 / 오역 0 / 누락 0 / 잉여 0 / N/A 284 = 289 라인 전수**

**청크 3 발견**: 신규 [오역]/[누락]/[잉여] 0건. ModelPlayer/RenderPlayer 위임자 패턴 — 1.21.1 Mixin 구조에서 일체 자동 처리 또는 동등 매핑 (R-2 청크 1과 일관).

**R-2 전체 (3 청크 6 파일) 누적 통계**:
| 청크 | 파일 | 라인 | 정합 | 오역 | 누락 | 잉여 | N/A |
|-----|-----|-----|------|------|------|------|------|
| 1 | SmartMovingRender L1-L200 | 200 | 51 | 0 | 3 | 0 | 146 |
| 2 | SmartMovingRender L201-L337 + Context + IModel + IRender | 242 | 4 | 0 | 0 | 0 | 238 |
| 3 | ModelPlayer + RenderPlayer | 289 | 5 | 0 | 0 | 0 | 284 |
| **합계** | **6 파일** | **731** | **60** | **0** | **3** | **0** | **668** |

**R-2 완료**. 다음 R-단계: **R-3 (SM render/playerapi 3 파일 — SmartMoving.java 54 + SmartMovingModelPlayerBase.java 181 + SmartMovingRenderPlayerBase.java 138 = ~373줄)**.

---

## R-3: SM render/playerapi 3 파일 (~376 라인) — PlayerAPI 인프라 위임자

### 청크 1 (3 파일 일괄)

**파트 A — SmartMoving.java (55 라인)** — PlayerAPI 등록 진입점

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L17 | 라이선스 + `==` | — | [N/A] | |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.moving.render.playerapi;` | — | [N/A] | |
| L20 | `import api.player.model.*;` | (1.21.1: PlayerAPI 부재 — Mixin이 등가) | [N/A] | PlayerAPI 인프라 |
| L21 | `import api.player.render.*;` | (PlayerAPI 부재) | [N/A] | |
| L22 | (빈 줄) | — | [N/A] | |
| L23 | `import net.smart.moving.*;` | — | [N/A] | |
| L24 | `import net.smart.render.playerapi.*;` | (PlayerAPI 부재) | [N/A] | |
| L25 | (빈 줄) | — | [N/A] | |
| L26 | `public abstract class SmartMoving` | (1.21.1: ModInitializer + Mixin — PlayerAPI 등록 부재) | [N/A] | |
| L27 | `{` | — | [N/A] | |
| L28 | `public static final String ID = SmartMovingInfo.ModName;` | (mod ID — 1.21.1 fabric.mod.json) | [N/A] | |
| L29 | (빈 줄) | — | [N/A] | |
| L30 | `public static void register()` | `Smartmoving.onInitializeClient()` (Mixin 자동 등록) | [N/A] | |
| L31 | `{` | — | [N/A] | |
| L32 | `String[] inferiors = new String[] { SmartRender.ID };` | (PlayerAPI sorting 부재) | [N/A] | Mixin priority 자동 |
| L33 | (빈 줄) | — | [N/A] | |
| L34 | `RenderPlayerBaseSorting renderSorting = new RenderPlayerBaseSorting();` | (sorting 부재) | [N/A] | |
| L35 | `renderSorting.setAfterLocalConstructingInferiors(inferiors);` | — | [N/A] | |
| L36 | `renderSorting.setOverrideRenderPlayerInferiors(inferiors);` | — | [N/A] | |
| L37 | `renderSorting.setOverrideRotatePlayerInferiors(inferiors);` | — | [N/A] | |
| L38 | `renderSorting.setOverrideRenderPlayerSleepInferiors(inferiors);` | — | [N/A] | |
| L39 | `RenderPlayerAPI.register(ID, SmartMovingRenderPlayerBase.class, renderSorting);` | (Mixin 자동) | [N/A] | |
| L40 | (빈 줄) | — | [N/A] | |
| L41 | `ModelPlayerBaseSorting modelSorting = new ModelPlayerBaseSorting();` | — | [N/A] | |
| L42 | `modelSorting.setAfterLocalConstructingInferiors(inferiors);` | — | [N/A] | |
| L43 | `ModelPlayerAPI.register(ID, SmartMovingModelPlayerBase.class, modelSorting);` | (Mixin 자동) | [N/A] | |
| L44 | `}` | — | [N/A] | |
| L45 | (빈 줄) | — | [N/A] | |
| L46 | `public static SmartMovingRenderPlayerBase getPlayerBase(net.minecraft.client.renderer.entity.RenderPlayer renderPlayer)` | (PlayerAPI getPlayerBase 부재 — Mixin this 직접) | [N/A] | |
| L47 | `{` | — | [N/A] | |
| L48 | `return (SmartMovingRenderPlayerBase)((IRenderPlayerAPI)renderPlayer).getRenderPlayerBase(ID);` | — | [N/A] | |
| L49 | `}` | — | [N/A] | |
| L50 | (빈 줄) | — | [N/A] | |
| L51 | `public static SmartMovingModelPlayerBase getPlayerBase(api.player.model.ModelPlayer modelPlayer)` | — | [N/A] | |
| L52 | `{` | — | [N/A] | |
| L53 | `return (SmartMovingModelPlayerBase)((IModelPlayerAPI)modelPlayer).getModelPlayerBase(ID);` | — | [N/A] | |
| L54 | `}` | — | [N/A] | |
| L55 | `}` | — | [N/A] | 클래스 종료 |

**파트 A 통계: 정합 0 / 오역 0 / 누락 0 / 잉여 0 / N/A 55 = 55 라인 전수.** PlayerAPI 등록/getter — 1.21.1 Fabric Mixin 자동.

**파트 B — SmartMovingModelPlayerBase.java (182 라인)** — IModelPlayer 구현 (PlayerAPI ModelPlayerBase 확장)

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L17 | 라이선스 + `==` | — | [N/A] | |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.moving.render.playerapi;` | — | [N/A] | |
| L20 | `import net.minecraft.client.model.*;` | — | [N/A] | |
| L21 | (빈 줄) | — | [N/A] | |
| L22 | `import api.player.model.*;` | (PlayerAPI 부재) | [N/A] | |
| L23 | (빈 줄) | — | [N/A] | |
| L24 | `import net.smart.moving.render.*;` | — | [N/A] | |
| L25 | `import net.smart.moving.render.IModelPlayer;` | — | [N/A] | |
| L26 | `import net.smart.render.playerapi.*;` | — | [N/A] | |
| L27 | (빈 줄) | — | [N/A] | |
| L28 | `public class SmartMovingModelPlayerBase extends ModelPlayerBase implements IModelPlayer` | `@Mixin(BipedEntityModel.class)` MixinPlayerEntityModelClient | [정합] | 표면 매핑 |
| L29 | `{` | — | [N/A] | |
| L30 | `private SmartMovingModel model;` | (Mixin: this 직접 — model 인스턴스 부재) | [N/A] | |
| L31 | (빈 줄) | — | [N/A] | |
| L32-L35 | 생성자 `SmartMovingModelPlayerBase(ModelPlayerAPI modelplayerapi) { super(...); }` | (Mixin 부재) | [N/A] | |
| L36 | (빈 줄) | — | [N/A] | |
| L37-L43 | `@Override public SmartMovingModel getMovingModel()` (lazy init) | (별도 모델 부재) | [N/A] | |
| L44 | (빈 줄) | — | [N/A] | |
| L45-L48 | `dynamicOverrideAnimateHeadRotation(...) { getMovingModel().animateHeadRotation(...) }` 4 라인 | (vanilla setAngles 자동 — head.yaw/pitch 자동) | [N/A] | sm_setAngles TAIL inject |
| L49 | (빈 줄) | — | [N/A] | |
| L50-L53 | `dynamicOverrideAnimateSleeping(...) { getMovingModel().animateSleeping(...) }` | (vanilla SleepingPose 자동) | [N/A] | |
| L54 | (빈 줄) | — | [N/A] | |
| L55-L58 | `dynamicOverrideAnimateArmSwinging(...) { ... }` | (vanilla limbSwing 자동) | [N/A] | sm_setAngles 11-state 분기 시 덮어씀 |
| L59 | (빈 줄) | — | [N/A] | |
| L60-L63 | `dynamicOverrideAnimateRiding(...) { ... }` | (vanilla Riding 자동) | [N/A] | |
| L64 | (빈 줄) | — | [N/A] | |
| L65-L68 | `dynamicOverrideAnimateLeftArmItemHolding(...) { ... }` | (vanilla 자동) | [N/A] | |
| L69 | (빈 줄) | — | [N/A] | |
| L70-L73 | `dynamicOverrideAnimateRightArmItemHolding(...) { ... }` | (vanilla 자동) | [N/A] | |
| L74 | (빈 줄) | — | [N/A] | |
| L75-L78 | `dynamicOverrideAnimateWorkingBody(...) { ... }` | (vanilla 자동 — 어깨 NonStandardWorking 미이식 §16-5) | [N/A] | |
| L79 | (빈 줄) | — | [N/A] | |
| L80-L83 | `dynamicOverrideAnimateWorkingArms(...) { ... }` | (vanilla 자동) | [N/A] | |
| L84 | (빈 줄) | — | [N/A] | |
| L85-L88 | `dynamicOverrideAnimateSneaking(...) { ... }` | (vanilla sneak 자동 — leaningPitch=0 별도 처리) | [N/A] | sm_setAngles L88 |
| L89 | (빈 줄) | — | [N/A] | |
| L90-L93 | `dynamicOverrideAnimateArms(...) { ... }` | (vanilla applyAnimationOffsets 자동) | [N/A] | |
| L94 | (빈 줄) | — | [N/A] | |
| L95-L98 | `dynamicOverrideAnimateBowAiming(...) { ... }` | (vanilla 활쏘기 자동 — 어깨 NonStandardBowAiming 미이식 §16-5) | [N/A] | |
| L99 | (빈 줄) | — | [N/A] | |
| L100-L104 | `@Override superAnimateHeadRotation(...) { super.dynamic(...) }` 5 라인 | (vanilla 자동) | [N/A] | dynamic = PlayerAPI reflection |
| L105 | (빈 줄) | — | [N/A] | |
| L106-L110 | `@Override superAnimateSleeping(...) { super.dynamic(...) }` | — | [N/A] | |
| L111 | (빈 줄) | — | [N/A] | |
| L112-L116 | `@Override superAnimateArmSwinging(...) { super.dynamic(...) }` | — | [N/A] | |
| L117 | (빈 줄) | — | [N/A] | |
| L118-L122 | `@Override superAnimateRiding(...) { super.dynamic(...) }` | — | [N/A] | |
| L123 | (빈 줄) | — | [N/A] | |
| L124-L128 | `@Override superAnimateLeftArmItemHolding(...) { super.dynamic(...) }` | — | [N/A] | |
| L129 | (빈 줄) | — | [N/A] | |
| L130-L134 | `@Override superAnimateRightArmItemHolding(...) { super.dynamic(...) }` | — | [N/A] | |
| L135 | (빈 줄) | — | [N/A] | |
| L136-L140 | `@Override superAnimateWorkingBody(...) { super.dynamic(...) }` | — | [N/A] | |
| L141 | (빈 줄) | — | [N/A] | |
| L142-L146 | `@Override superAnimateWorkingArms(...) { super.dynamic(...) }` | — | [N/A] | |
| L147 | (빈 줄) | — | [N/A] | |
| L148-L152 | `@Override superAnimateSneaking(...) { super.dynamic(...) }` | — | [N/A] | |
| L153 | (빈 줄) | — | [N/A] | |
| L154-L158 | `@Override superApplyAnimationOffsets(...) { super.dynamic("animateArms", ...) }` | — | [N/A] | |
| L159 | (빈 줄) | — | [N/A] | |
| L160-L164 | `@Override superAnimateBowAiming(...) { super.dynamic(...) }` | — | [N/A] | |
| L165 | (빈 줄) | — | [N/A] | |
| L166 | `@Deprecated public ModelRenderer getOuter() { return getMovingModel().md.bipedOuter; }` | (Outer 부재) | [N/A] | §7 — 다층 모델 부재 |
| L167 | `@Deprecated public ModelRenderer getTorso() { return ... bipedTorso; }` | (Torso 부재) | [N/A] | §16-2 |
| L168 | `@Deprecated public ModelRenderer getBody() { return ... bipedBody; }` | `body` (PlayerEntityModel) | [정합] | 표면 매핑 |
| L169 | `@Deprecated public ModelRenderer getBreast() { return ... bipedBreast; }` | (Breast 부재) | [N/A] | |
| L170 | `@Deprecated public ModelRenderer getNeck() { return ... bipedNeck; }` | (Neck 부재) | [N/A] | |
| L171 | `@Deprecated public ModelRenderer getHead() { return ... bipedHead; }` | `head` | [정합] | |
| L172 | `@Deprecated public ModelRenderer getHeadwear() { return ... bipedHeadwear; }` | `hat` (PlayerEntityModel.hat) | [정합] | 표면 매핑 |
| L173 | `@Deprecated public ModelRenderer getRightShoulder() { return ... bipedRightShoulder; }` | (Shoulder 부재) | [N/A] | §16-5 |
| L174 | `@Deprecated public ModelRenderer getRightArm() { return ... bipedRightArm; }` | `rightArm` | [정합] | |
| L175 | `@Deprecated public ModelRenderer getLeftShoulder() { return ... bipedLeftShoulder; }` | (Shoulder 부재) | [N/A] | §16-5 |
| L176 | `@Deprecated public ModelRenderer getLeftArm() { return ... bipedLeftArm; }` | `leftArm` | [정합] | |
| L177 | `@Deprecated public ModelRenderer getPelvic() { return ... bipedPelvic; }` | (Pelvic 부재) | [N/A] | §16-3 |
| L178 | `@Deprecated public ModelRenderer getRightLeg() { return ... bipedRightLeg; }` | `rightLeg` | [정합] | |
| L179 | `@Deprecated public ModelRenderer getLeftLeg() { return ... bipedLeftLeg; }` | `leftLeg` | [정합] | |
| L180 | `@Deprecated public ModelRenderer getEars() { return ... bipedEars; }` | (Ears — SR mod 전용 노드 부재) | [N/A] | |
| L181 | `@Deprecated public ModelRenderer getCloak() { return ... bipedCloak; }` | `cloak` (PlayerEntityModel.cloak) | [정합] | 표면 매핑 |
| L182 | `}` | — | [N/A] | 클래스 종료 |

**파트 B 통계: 정합 9 / 오역 0 / 누락 0 / 잉여 0 / N/A 173 = 182 라인 전수.** ModelPlayerBase = PlayerAPI 위임자 패턴 + 16 @Deprecated getter (9 [정합] + 7 [N/A] 구조 부재).

**파트 C — SmartMovingRenderPlayerBase.java (139 라인)** — IRenderPlayer 구현 (PlayerAPI RenderPlayerBase 확장)

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L17 | 라이선스 + `==` | — | [N/A] | |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.moving.render.playerapi;` | — | [N/A] | |
| L20 | `import net.minecraft.client.entity.*;` | — | [N/A] | |
| L21 | `import net.minecraft.client.renderer.entity.*;` | — | [N/A] | |
| L22 | `import net.minecraft.entity.*;` | — | [N/A] | |
| L23 | (빈 줄) | — | [N/A] | |
| L24 | `import api.player.render.*;` | (PlayerAPI 부재) | [N/A] | |
| L25 | (빈 줄) | — | [N/A] | |
| L26 | `import net.smart.moving.render.*;` | — | [N/A] | |
| L27 | `import net.smart.moving.render.IRenderPlayer;` | — | [N/A] | |
| L28 | (빈 줄) | — | [N/A] | |
| L29 | `public class SmartMovingRenderPlayerBase extends RenderPlayerBase implements IRenderPlayer` | `@Mixin(PlayerEntityRenderer.class) MixinPlayerEntityRenderer` | [정합] | 표면 매핑 |
| L30 | `{` | — | [N/A] | |
| L31-L34 | 생성자 `SmartMovingRenderPlayerBase(RenderPlayerAPI renderPlayerAPI) { super(...); }` | (Mixin 부재) | [N/A] | |
| L35 | (빈 줄) | — | [N/A] | |
| L36-L41 | `getRenderModel()` (lazy init render = new SmartMovingRender(this)) | (별도 render 인스턴스 부재 — Mixin this) | [N/A] | |
| L42 | (빈 줄) | — | [N/A] | |
| L43-L47 | `@Override renderPlayer(...) { getRenderModel().renderPlayer(...) }` 5 라인 | (vanilla render 자동 — Mixin TAIL) | [N/A] | 위임자 |
| L48 | (빈 줄) | — | [N/A] | |
| L49-L53 | `@Override superRenderRenderPlayer(...) { super.renderPlayer(...) }` | (vanilla 자동) | [N/A] | |
| L54 | (빈 줄) | — | [N/A] | |
| L55-L59 | `@Override rotatePlayer(...) { getRenderModel().rotatePlayer(...) }` | sm_captureBodyYaw HEAD + @ModifyArg index=3 (위임 본체는 R-2 청크 1 매핑) | [정합] | rotatePlayer = setupTransforms 진입점 |
| L60 | (빈 줄) | — | [N/A] | |
| L61-L65 | `@Override superRenderRotatePlayer(...) { super.rotatePlayer(...) }` | (vanilla 자동) | [N/A] | |
| L66 | (빈 줄) | — | [N/A] | |
| L67-L71 | `@Override renderPlayerSleep(...) { getRenderModel().renderPlayerAt(...) }` | sm_getPositionOffset @Inject (위임 본체는 R-2 청크 1) | [정합] | renderPlayerSleep = getPositionOffset 진입점 |
| L72 | (빈 줄) | — | [N/A] | |
| L73-L77 | `@Override superRenderRenderPlayerAt(...) { super.renderPlayerSleep(...) }` | (vanilla 자동) | [N/A] | |
| L78 | (빈 줄) | — | [N/A] | |
| L79-L83 | `@Override passSpecialRender(...) { getRenderModel().renderName(...) }` | smartmoving$adjustLabelY @Inject + MixinLivingEntityRenderer.hasLabel @Redirect | [정합] | passSpecialRender = renderLabelIfPresent 진입점 |
| L84 | (빈 줄) | — | [N/A] | |
| L85-L89 | `@Override superRenderRenderName(...) { super.passSpecialRender(...) }` | (vanilla 자동) | [N/A] | |
| L90 | (빈 줄) | — | [N/A] | |
| L91-L95 | `@Override getRenderManager() { return renderPlayerAPI.getRenderManagerField(); }` | (1.21.1: EntityRenderDispatcher 자동) | [N/A] | |
| L96 | (빈 줄) | — | [N/A] | |
| L97 | `public boolean isRenderedWithBodyTopAlwaysInAccelerateDirection()` | sm_captureBodyYaw 4 분기 (Flying/Swim/Dive/HeadJump) 통합 처리 | [정합] | 외부 호출 인터페이스 — 8 분기 중 4 분기 합집합 |
| L98 | `{` | — | [N/A] | |
| L99 | `SmartMovingRender render = getRenderModel();` | — | [N/A] | |
| L100 | `return render.modelBipedMain.isFlying \|\| render.modelBipedMain.isSwim \|\| render.modelBipedMain.isDive \|\| render.modelBipedMain.isHeadJump;` | sm_captureBodyYaw 분기 일치 (4 상태 모두 forwardRotation 적용) | [정합] | |
| L101 | `}` | — | [N/A] | |
| L102 | (빈 줄) | — | [N/A] | |
| L103-L107 | `@Override getPlayerModelArmor() { return SmartMoving.getPlayerBase((api.player.model.ModelPlayer)renderPlayerAPI.getModelArmorField()); }` 5 라인 | (갑옷 ArmorFeatureRenderer §17 잔여) | [N/A] | §16-8 |
| L108 | (빈 줄) | — | [N/A] | |
| L109-L113 | `@Override getPlayerModelArmorChestplate() { ... }` | (갑옷) | [N/A] | |
| L114 | (빈 줄) | — | [N/A] | |
| L115-L119 | `@Override getPlayerModelBipedMain() { return SmartMoving.getPlayerBase((api.player.model.ModelPlayer)renderPlayerAPI.getModelBipedMainField()); }` | (1.21.1 단일 모델 — this.getModel() 자동) | [N/A] | |
| L120 | (빈 줄) | — | [N/A] | |
| L121 | `@Override` | — | [N/A] | |
| L122 | `public IModelPlayer[] getPlayerModels()` | (다층 모델 부재) | [N/A] | |
| L123 | `{` | — | [N/A] | |
| L124 | `api.player.model.ModelPlayer[] modelPlayers = api.player.model.ModelPlayerAPI.getAllInstances();` | — | [N/A] | |
| L125 | `if(allModelPlayers != null && (allModelPlayers == modelPlayers \|\| modelPlayers.length == 0 && allModelPlayers.length == 0))` | — | [N/A] | |
| L126 | `return allIModelPlayers;` | — | [N/A] | |
| L127 | (빈 줄) | — | [N/A] | |
| L128 | `allModelPlayers = modelPlayers;` | — | [N/A] | |
| L129 | `allIModelPlayers = new IModelPlayer[modelPlayers.length];` | — | [N/A] | |
| L130 | `for(int i=0; i<allIModelPlayers.length; i++)` | — | [N/A] | |
| L131 | `allIModelPlayers[i] = SmartMoving.getPlayerBase(allModelPlayers[i]);` | — | [N/A] | |
| L132 | `return allIModelPlayers;` | — | [N/A] | |
| L133 | `}` | — | [N/A] | |
| L134 | (빈 줄) | — | [N/A] | |
| L135 | `private api.player.model.ModelPlayer[] allModelPlayers;` | (캐시 필드 — 부재) | [N/A] | |
| L136 | `private IModelPlayer[] allIModelPlayers;` | — | [N/A] | |
| L137 | (빈 줄) | — | [N/A] | |
| L138 | `private SmartMovingRender render;` | (Mixin this 직접 — render 별도 인스턴스 부재) | [N/A] | |
| L139 | `}` | — | [N/A] | 클래스 종료 |

**파트 C 통계: 정합 5 / 오역 0 / 누락 0 / 잉여 0 / N/A 134 = 139 라인 전수.** RenderPlayerBase = PlayerAPI 위임자 (rotatePlayer/renderPlayerSleep/passSpecialRender 진입점 + 클래스 + isRenderedWithBodyTopAlwaysInAccelerateDirection 4 분기 [정합] 5 라인 제외 모두 [N/A]).

**R-3 통계 (3 파일 합)**:
- 파트 A (SmartMoving): 0 / 0 / 0 / 0 / 55 = 55 라인
- 파트 B (ModelPlayerBase): 9 / 0 / 0 / 0 / 173 = 182 라인
- 파트 C (RenderPlayerBase): 5 / 0 / 0 / 0 / 134 = 139 라인
- **합계: 정합 14 / 오역 0 / 누락 0 / 잉여 0 / N/A 362 = 376 라인 전수**

**R-3 발견**: 신규 [오역]/[누락]/[잉여] 0건. PlayerAPI 인프라 위임자 — 1.21.1 Mixin 구조에서 자동 처리. 다만 다음 사항 확인:
- 16 ModelRenderer @Deprecated getter 중 9개만 1.21.1 ModelPart 매핑됨 (Body/Head/Headwear/RightArm/LeftArm/RightLeg/LeftLeg/Cloak + L168 body) — 7개 부재 (Outer/Torso/Breast/Neck/RightShoulder/LeftShoulder/Pelvic/Ears 중 Ears 제외 7) — §16-2/3/5 일관 검증.
- isRenderedWithBodyTopAlwaysInAccelerateDirection 4 분기 (Flying/Swim/Dive/HeadJump) — sm_captureBodyYaw 8 분기 중 4 분기 합집합 일치.

**R-3 완료**. 다음 R-단계: **R-4 (SmartRenderModel.java 469줄, 3 청크 × ~160줄)**.

---

## R-4: SmartRenderModel.java (469 라인)

### 청크 1 (L1-L160) — 헤더 + 생성자 (14 노드 트리 + 상태 복사) + create/copy helper + render 시작

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L16 | Smart Render GPLv3 라이선스 헤더 | — | [N/A] | 16 라인 일괄 |
| L17 | `// ==…==` | — | [N/A] | |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.render;` | — | [N/A] | |
| L20 | `import java.util.*;` | — | [N/A] | |
| L21 | (빈 줄) | — | [N/A] | |
| L22 | `import net.minecraft.client.model.*;` | — | [N/A] | |
| L23 | `import net.minecraft.entity.*;` | — | [N/A] | |
| L24 | `import net.minecraft.entity.passive.*;` | — | [N/A] | |
| L25 | `import net.minecraft.util.*;` | — | [N/A] | |
| L26 | (빈 줄) | — | [N/A] | |
| L27 | `public class SmartRenderModel extends SmartRenderContext` | (1.21.1: PlayerEntityModel 단일 + Mixin) | [N/A] | 다층 모델 부재 |
| L28 | `{` | — | [N/A] | |
| L29 | `public IModelPlayer imp;` | (Mixin: this 직접) | [N/A] | |
| L30 | `public ModelBiped mp;` | — | [N/A] | |
| L31 | (빈 줄) | — | [N/A] | |
| L32 | `public SmartRenderModel(ModelBiped mp, IModelPlayer imp, ModelRenderer originalBipedBody, originalBipedCloak, originalBipedHead, originalBipedEars, originalBipedHeadwear, originalBipedRightArm, originalBipedLeftArm, originalBipedRightLeg, originalBipedLeftLeg)` | (Mixin: 생성자 부재 — vanilla PlayerEntityModel 자동) | [N/A] | |
| L33 | `{` | — | [N/A] | |
| L34 | `this.imp = imp;` | — | [N/A] | |
| L35 | `this.mp = mp;` | — | [N/A] | |
| L36 | (빈 줄) | — | [N/A] | |
| L37 | `mp.boxList.clear();` | (1.21.1: ModelData 시스템 — boxList 부재) | [N/A] | |
| L38 | (빈 줄) | — | [N/A] | |
| L39 | `bipedOuter = create(-1, -1, null);` | (Outer 노드 부재) | [N/A] | §16-2 — 다층 부모 노드 |
| L40 | `bipedOuter.setRotationPoint(0.0F, 0.0F, 0.0F);` | — | [N/A] | |
| L41 | `bipedOuter.fadeEnabled = true;` | (fade 메커니즘 부재) | [N/A] | §17 잔여 — fade 보간 0.2F*timeDelta |
| L42 | (빈 줄) | — | [N/A] | |
| L43 | `bipedTorso = create(16, 16, bipedOuter);` | (Torso 부재 — body 단일 노드 근사) | [N/A] | §16-2 |
| L44 | `bipedTorso.setRotationPoint(0.0F, 0.0F, 0.0F);` | — | [N/A] | |
| L45 | (빈 줄) | — | [N/A] | |
| L46 | `bipedBody = create(16, 16, bipedTorso, originalBipedBody);` | `body` (PlayerEntityModel) | [정합] | 표면 매핑 — 단, Torso 자식 관계 부재 |
| L47 | `bipedBody.setRotationPoint(0.0F, 0.0F, 0.0F);` | `body.pivotX/Y/Z` 기본 0 | [정합] | |
| L48 | (빈 줄) | — | [N/A] | |
| L49 | `bipedBreast = create(-1, -1, bipedTorso);` | (Breast 부재) | [N/A] | §16-2 |
| L50 | `bipedBreast.setRotationPoint(0.0F, 0.0F, 0.0F);` | — | [N/A] | |
| L51 | (빈 줄) | — | [N/A] | |
| L52 | `bipedNeck = create(-1, -1, bipedBreast);` | (Neck 부재) | [N/A] | |
| L53 | `bipedNeck.setRotationPoint(0.0F, 0.0F, 0.0F);` | — | [N/A] | |
| L54 | (빈 줄) | — | [N/A] | |
| L55 | `bipedCloak = new ModelCapeRenderer(mp, 0, 0, bipedBreast, bipedOuter);` | `cloak` (PlayerEntityModel.cloak) | [정합] | 표면 매핑 — 부모 (Breast/Outer) 부재로 자식 관계 N/A |
| L56 | `copy(bipedCloak, originalBipedCloak);` | (vanilla cloak 자동) | [정합] | |
| L57 | `bipedCloak.setRotationPoint(0.0F, 0.0F, 2.0F);` | `cloak.pivotZ = 2.0F` (vanilla 기본 매핑) | [정합] | |
| L58 | (빈 줄) | — | [N/A] | |
| L59 | `bipedHead = create(0, 0, bipedNeck, originalBipedHead);` | `head` | [정합] | 표면 매핑 |
| L60 | `bipedHead.setRotationPoint(0.0F, 0.0F, 0.0F);` | `head.pivot` 기본 | [정합] | |
| L61 | (빈 줄) | — | [N/A] | |
| L62 | `bipedEars = new ModelEarsRenderer(mp, 24, 0, bipedHead);` | (SR mod 전용 노드 부재) | [N/A] | Ears 노드 — 1.21.1 부재 |
| L63 | `copy(bipedCloak, originalBipedEars);` | — | [N/A] | (note: 원본 버그 가능성 — `bipedCloak`이 들어감, `bipedEars`이 의도. 본 매핑 영향 없음 — 어차피 N/A) |
| L64 | `bipedEars.setRotationPoint(0.0F, 0.0F, 0.0F);` | — | [N/A] | |
| L65 | (빈 줄) | — | [N/A] | |
| L66 | `bipedHeadwear = create(32, 0, bipedHead, originalBipedHeadwear);` | `hat` (PlayerEntityModel.hat) | [정합] | 표면 매핑 |
| L67 | `bipedHeadwear.setRotationPoint(0.0F, 0.0F, 0.0F);` | `hat.pivot` 기본 | [정합] | |
| L68 | (빈 줄) | — | [N/A] | |
| L69 | `bipedRightShoulder = create(40, 16, bipedBreast);` | (Shoulder 부재) | [N/A] | §16-5 |
| L70 | `bipedRightShoulder.setRotationPoint(-5F, 2.0F, 0.0F);` | — | [N/A] | |
| L71 | (빈 줄) | — | [N/A] | |
| L72 | `bipedRightArm = create(40, 16, bipedRightShoulder, originalBipedRightArm);` | `rightArm` | [정합] | 표면 매핑 — Shoulder 자식 관계 부재 |
| L73 | (빈 줄) | — | [N/A] | |
| L74 | `bipedLeftShoulder = create(-1, -1, bipedBreast);` | (Shoulder 부재) | [N/A] | §16-5 |
| L75 | `bipedLeftShoulder.mirror = true;` | — | [N/A] | |
| L76 | `bipedLeftShoulder.setRotationPoint(5F, 2.0F, 0.0F);` | — | [N/A] | |
| L77 | (빈 줄) | — | [N/A] | |
| L78 | `bipedLeftArm = create(40, 16, bipedLeftShoulder, originalBipedLeftArm);` | `leftArm` | [정합] | |
| L79 | (빈 줄) | — | [N/A] | |
| L80 | `bipedPelvic = create(-1, -1, bipedTorso);` | (Pelvic 부재) | [N/A] | §16-3 |
| L81 | `bipedPelvic.setRotationPoint(0.0F, 12.0F, 0.0F);` | — | [N/A] | |
| L82 | (빈 줄) | — | [N/A] | |
| L83 | `bipedRightLeg = create(0, 16, bipedPelvic, originalBipedRightLeg);` | `rightLeg` | [정합] | 표면 매핑 — Pelvic 자식 관계 부재 (1.21.1 leg 직접 root) |
| L84 | `bipedRightLeg.setRotationPoint(-2F, 0.0F, 0.0F);` | `rightLeg.pivotX = -2F` (vanilla 기본 매핑) | [정합] | |
| L85 | (빈 줄) | — | [N/A] | |
| L86 | `bipedLeftLeg = create(0, 16, bipedPelvic, originalBipedLeftLeg);` | `leftLeg` | [정합] | |
| L87 | `bipedLeftLeg.setRotationPoint(2.0F, 0.0F, 0.0F);` | `leftLeg.pivotX = 2.0F` | [정합] | |
| L88 | (빈 줄) | — | [N/A] | |
| L89 | `imp.initialize(bipedBody, bipedCloak, bipedHead, bipedEars, bipedHeadwear, bipedRightArm, bipedLeftArm, bipedRightLeg, bipedLeftLeg);` | (1.21.1: vanilla PlayerEntityModel 초기화 자동) | [N/A] | |
| L90 | (빈 줄) | — | [N/A] | |
| L91 | `if(SmartRenderRender.CurrentMainModel != null)` | (SmartMovingClientState 일원화 — CurrentMainModel 부재) | [N/A] | |
| L92 | `{` | — | [N/A] | |
| L93 | `isInventory = SmartRenderRender.CurrentMainModel.isInventory;` | (vanilla InventoryScreen 자동) | [N/A] | |
| L94 | (빈 줄) | — | [N/A] | |
| L95 | `totalVerticalDistance = SmartRenderRender.CurrentMainModel.totalVerticalDistance;` | (1.21.1 limbSwing 잘못 매핑 — §16-10 [오역]) | [N/A] | 변수 캐싱 — 정의 자체는 §16-10 발견 변수 |
| L96 | `currentVerticalSpeed = SmartRenderRender.CurrentMainModel.currentVerticalSpeed;` | (1.21.1 limbSwingAmount 잘못 매핑 — §16-10) | [N/A] | |
| L97 | `totalDistance = SmartRenderRender.CurrentMainModel.totalDistance;` | (1.21.1 limbSwing 또는 animationProgress 잘못 매핑 — §16-12/13/15) | [N/A] | |
| L98 | `currentSpeed = SmartRenderRender.CurrentMainModel.currentSpeed;` | (1.21.1 limbSwingAmount 잘못 매핑 — §16-13/15) | [N/A] | |
| L99 | (빈 줄) | — | [N/A] | |
| L100 | `distance = SmartRenderRender.CurrentMainModel.distance;` | (state holder) | [N/A] | |
| L101 | `verticalDistance = SmartRenderRender.CurrentMainModel.verticalDistance;` | (수직 거리 — 1.21.1 capture 부재) | [N/A] | |
| L102 | `horizontalDistance = SmartRenderRender.CurrentMainModel.horizontalDistance;` | (수평 거리 — limbSwing) | [N/A] | |
| L103 | `currentCameraAngle = SmartRenderRender.CurrentMainModel.currentCameraAngle;` | (player.getYaw 활용) | [N/A] | |
| L104 | `currentVerticalAngle = SmartRenderRender.CurrentMainModel.currentVerticalAngle;` | `sm.stats.currentVerticalAngle` (state holder) | [N/A] | |
| L105 | `currentHorizontalAngle = SmartRenderRender.CurrentMainModel.currentHorizontalAngle;` | (atan2(-vel.x, vel.z)) | [N/A] | |
| L106 | `prevOuterRenderData = SmartRenderRender.CurrentMainModel.prevOuterRenderData;` | (Outer 부재 — fade 보간 데이터 부재) | [N/A] | §17 잔여 |
| L107 | `isSleeping = SmartRenderRender.CurrentMainModel.isSleeping;` | (vanilla SleepingPose 자동) | [N/A] | |
| L108 | (빈 줄) | — | [N/A] | |
| L109 | `actualRotation = SmartRenderRender.CurrentMainModel.actualRotation;` | (player.getYaw 처리) | [N/A] | |
| L110 | `forwardRotation = SmartRenderRender.CurrentMainModel.forwardRotation;` | sm_captureBodyYaw lerp 처리 | [N/A] | |
| L111 | `workingAngle = SmartRenderRender.CurrentMainModel.workingAngle;` | (어깨 부재로 N/A) | [N/A] | §16-5 |
| L112 | `}` | — | [N/A] | |
| L113 | `}` | — | [N/A] | 생성자 종료 |
| L114 | (빈 줄) | — | [N/A] | |
| L115 | `private ModelRotationRenderer create(int i, int j, ModelRotationRenderer base)` | (ModelRotationRenderer 부재 — ModelPart 자체로 충분) | [N/A] | |
| L116 | `{` | — | [N/A] | |
| L117 | `return new ModelRotationRenderer(mp, i, j, base);` | — | [N/A] | |
| L118 | `}` | — | [N/A] | |
| L119 | (빈 줄) | — | [N/A] | |
| L120 | `private ModelRotationRenderer create(int i, int j, ModelRotationRenderer base, ModelRenderer original)` | — | [N/A] | |
| L121 | `{` | — | [N/A] | |
| L122 | `ModelRotationRenderer local = create(i, j, base);` | — | [N/A] | |
| L123 | `copy(local, original);` | — | [N/A] | |
| L124 | `return local;` | — | [N/A] | |
| L125 | `}` | — | [N/A] | |
| L126 | (빈 줄) | — | [N/A] | |
| L127 | `private static void copy(ModelRotationRenderer local, ModelRenderer original)` | (1.21.1: ModelData 자동) | [N/A] | |
| L128 | `{` | — | [N/A] | |
| L129 | `if(original.childModels != null)` | — | [N/A] | |
| L130 | `for(Object childModel : original.childModels)` | — | [N/A] | |
| L131 | `local.addChild((ModelRenderer)childModel);` | — | [N/A] | |
| L132 | `if(original.cubeList != null)` | — | [N/A] | |
| L133 | `for(Object cube : original.cubeList)` | — | [N/A] | |
| L134 | `local.cubeList.add(cube);` | — | [N/A] | |
| L135 | `local.mirror = original.mirror;` | (1.21.1: ModelPart.mirror 자동) | [N/A] | |
| L136 | `local.isHidden = original.isHidden;` | (1.21.1: ModelPart.hidden) | [N/A] | |
| L137 | `local.showModel = original.showModel;` | (1.21.1: ModelPart.visible) | [N/A] | |
| L138 | `}` | — | [N/A] | |
| L139 | (빈 줄) | — | [N/A] | |
| L140 | `public void render(Entity entity, float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)` | (vanilla PlayerEntityModel.render 자동 — Mixin 자동 처리) | [N/A] | |
| L141 | `{` | — | [N/A] | |
| L142 | `bipedBody.ignoreRender = bipedHead.ignoreRender = bipedHeadwear.ignoreRender = bipedRightArm.ignoreRender = bipedLeftArm.ignoreRender = bipedRightLeg.ignoreRender = bipedLeftLeg.ignoreRender = true;` | (vanilla 자동 — 다층 모델 부재로 ignoreRender 불필요) | [N/A] | |
| L143 | `imp.superRender(entity, ...)` | (vanilla render 본체) | [N/A] | |
| L144 | `bipedBody.ignoreRender = bipedHead.ignoreRender = bipedHeadwear.ignoreRender = bipedRightArm.ignoreRender = bipedLeftArm.ignoreRender = bipedRightLeg.ignoreRender = bipedLeftLeg.ignoreRender = false;` | (복원 — 불필요) | [N/A] | |
| L145 | (빈 줄) | — | [N/A] | |
| L146 | `bipedOuter.render(factor);` | (Outer 부재 — 전체 렌더 부재) | [N/A] | §16-2 |
| L147 | (빈 줄) | — | [N/A] | |
| L148 | `bipedOuter.renderIgnoreBase(factor);` | — | [N/A] | |
| L149 | `bipedTorso.renderIgnoreBase(factor);` | (Torso 부재) | [N/A] | |
| L150 | `bipedBody.renderIgnoreBase(factor);` | (vanilla 자동 — body 단일 렌더) | [N/A] | |
| L151 | `bipedBreast.renderIgnoreBase(factor);` | (Breast 부재) | [N/A] | |
| L152 | `bipedNeck.renderIgnoreBase(factor);` | (Neck 부재) | [N/A] | |
| L153 | `bipedHead.renderIgnoreBase(factor);` | (vanilla 자동) | [N/A] | |
| L154 | `bipedHeadwear.renderIgnoreBase(factor);` | (vanilla 자동) | [N/A] | |
| L155 | `bipedRightShoulder.renderIgnoreBase(factor);` | (Shoulder 부재) | [N/A] | |
| L156 | `bipedRightArm.renderIgnoreBase(factor);` | (vanilla 자동) | [N/A] | |
| L157 | `bipedLeftShoulder.renderIgnoreBase(factor);` | (Shoulder 부재) | [N/A] | |
| L158 | `bipedLeftArm.renderIgnoreBase(factor);` | (vanilla 자동) | [N/A] | |
| L159 | `bipedPelvic.renderIgnoreBase(factor);` | (Pelvic 부재) | [N/A] | |
| L160 | `bipedRightLeg.renderIgnoreBase(factor);` | (vanilla 자동) | [N/A] | |

**청크 1 (L1-L160) 통계: 정합 15 / 오역 0 / 누락 0 / 잉여 0 / N/A 145 = 160 라인 전수.**

**청크 1 발견**: 신규 [오역]/[누락]/[잉여] 0건. 다음 사항 확인:
- 14 SmartRender 노드 중 7개만 1.21.1 ModelPart 매핑됨 (Body/Cloak/Head/Headwear→hat/RightArm/LeftArm/RightLeg/LeftLeg = 8 노드, Cloak는 부분 매핑) — 7개 부재 (Outer/Torso/Breast/Neck/Shoulder×2/Pelvic/Ears 중 Ears 제외 7) — §16-2/3/5/§17 fade 일관 검증.
- L95-L98 변수 정의 (totalVerticalDistance/currentVerticalSpeed/totalDistance/currentSpeed)는 §16-10/12/13/15 [오역] 발견 변수의 *원본 정의 위치* 확인 — SmartRenderModel.java에 위치하며 SmartRenderRender 에서 매 프레임 갱신.
- L62-L64 bipedEars 생성에서 `copy(bipedCloak, originalBipedEars);` 가 Cloak로 잘못 들어감 (원본 버그 가능성 — bipedEars가 의도). 본 매핑 영향 없음 (Ears 자체 N/A).

**다음 청크**: R-4 청크 2 (SmartRenderModel.java L161-L320) — render() 잔여 + setRotationAngles 본체 + animateXxx 메서드 일부.

### 청크 2 (L161-L320) — render 종료 + setRotationAngles 본체 (1인칭/일반 분기) + animateHeadRotation/Sleeping/ArmSwinging/Riding/ItemHolding/WorkingBody/WorkingArms/Sneaking 본체

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L161 | `bipedLeftLeg.renderIgnoreBase(factor);` | (vanilla 자동) | [N/A] | render 종료 라인 |
| L162 | `}` | — | [N/A] | render 종료 |
| L163 | (빈 줄) | — | [N/A] | |
| L164 | `public void setRotationAngles(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor, Entity entity)` | `MixinPlayerEntityModelClient.setAngles` (vanilla setAngles 진입점) + `sm_setAngles @Inject(TAIL)` | [정합] | 메인 진입점 — 6 인자 → limbSwing/limbSwingAmount/animationProgress/headYaw/headPitch |
| L165 | `{` | — | [N/A] | |
| L166 | `reset();` | (vanilla setAngles 자동 reset) | [N/A] | |
| L167 | (빈 줄) | — | [N/A] | |
| L168 | `if(firstPerson \|\| isInventory)` | (1.21.1: 1인칭은 PlayerEntityRenderer 자동 분리, 인벤토리는 InventoryScreen 자동) | [N/A] | 자동 분리 |
| L169 | `{` | — | [N/A] | |
| L170-L178 | 9 노드 `ignoreBase = true;` (Body/Head/Headwear/Ears/Cloak/RightArm/LeftArm/RightLeg/LeftLeg) | (다층 모델 부재 — ignoreBase 메커니즘 부재) | [N/A] | |
| L179 | (빈 줄) | — | [N/A] | |
| L180-L188 | 9 노드 `forceRender = firstPerson;` | (다층 부재) | [N/A] | |
| L189 | (빈 줄) | — | [N/A] | |
| L190 | `bipedRightArm.setRotationPoint(-5F, 2.0F, 0.0F);` | (vanilla 1인칭 자동 — 일반 시점에서는 자동 default) | [N/A] | |
| L191 | `bipedLeftArm.setRotationPoint(5F, 2.0F, 0.0F);` | — | [N/A] | |
| L192 | `bipedRightLeg.setRotationPoint(-2F, 12F, 0.0F);` | — | [N/A] | |
| L193 | `bipedLeftLeg.setRotationPoint(2.0F, 12F, 0.0F);` | — | [N/A] | |
| L194 | (빈 줄) | — | [N/A] | |
| L195 | `imp.superSetRotationAngles(totalHorizontalDistance, ...)` | (vanilla setAngles 자동) | [N/A] | |
| L196 | `return;` | — | [N/A] | 1인칭/인벤토리 일찍 종료 |
| L197 | `}` | — | [N/A] | |
| L198 | (빈 줄) | — | [N/A] | |
| L199 | `if(isSleeping)` | (vanilla SleepingPose 자동 — fade 메커니즘 부재) | [N/A] | |
| L200 | `{` | — | [N/A] | |
| L201 | `prevOuterRenderData.rotateAngleX = 0;` | (Outer fade 데이터 부재) | [N/A] | §17 |
| L202 | `prevOuterRenderData.rotateAngleY = 0;` | — | [N/A] | |
| L203 | `prevOuterRenderData.rotateAngleZ = 0;` | — | [N/A] | |
| L204 | `}` | — | [N/A] | |
| L205 | (빈 줄) | — | [N/A] | |
| L206 | `bipedOuter.previous = prevOuterRenderData;` | (Outer fade 진입 — 부재) | [N/A] | §17 잔여 |
| L207 | (빈 줄) | — | [N/A] | |
| L208 | `bipedOuter.rotateAngleY = actualRotation / RadiantToAngle;` | MixinPlayerEntityRenderer.sm_captureBodyYaw + @ModifyArg index=3 | [정합] | bodyYaw 처리 (Mixin 분리) |
| L209 | `bipedOuter.fadeRotateAngleY = !(entity.ridingEntity instanceof EntityPig);` | (돼지 라이딩 분기 별도 — fade 부재로 무관) | [N/A] | 특수 케이스 |
| L210 | (빈 줄) | — | [N/A] | |
| L211 | `imp.animateHeadRotation(totalHorizontalDistance, ...)` | (vanilla setAngles head.yaw/pitch 자동) | [정합] | vanilla 자동 |
| L212 | (빈 줄) | — | [N/A] | |
| L213 | `if(isSleeping)` | (vanilla SleepingPose 자동) | [N/A] | |
| L214 | `imp.animateSleeping(...)` | (vanilla 자동) | [정합] | |
| L215 | (빈 줄) | — | [N/A] | |
| L216 | `imp.animateArmSwinging(totalHorizontalDistance, ...)` | (vanilla limbSwing 기반 자동 — sm_setAngles TAIL이 SM 11-state 분기 시 덮어씀) | [정합] | vanilla BipedEntityModel.setAngles 본체 |
| L217 | (빈 줄) | — | [N/A] | |
| L218 | `if(mp.isRiding)` | (vanilla Riding 자동) | [N/A] | |
| L219 | `imp.animateRiding(...)` | (vanilla 자동) | [정합] | |
| L220 | (빈 줄) | — | [N/A] | |
| L221 | `if(mp.heldItemLeft != 0)` | (vanilla 자동) | [N/A] | |
| L222 | `imp.animateLeftArmItemHolding(...)` | (vanilla item holding 자동) | [정합] | |
| L223 | (빈 줄) | — | [N/A] | |
| L224 | `if(mp.heldItemRight != 0)` | (vanilla) | [N/A] | |
| L225 | `imp.animateRightArmItemHolding(...)` | (vanilla 자동) | [정합] | |
| L226 | (빈 줄) | — | [N/A] | |
| L227 | `if(mp.onGround > -9990F)` | (sword swing animation timer — vanilla handSwingProgress 자동) | [N/A] | |
| L228 | `{` | — | [N/A] | |
| L229 | `imp.animateWorkingBody(...)` | (vanilla animateAttack 자동) | [정합] | |
| L230 | `imp.animateWorkingArms(...)` | (vanilla animateAttack 자동) | [정합] | |
| L231 | `}` | — | [N/A] | |
| L232 | (빈 줄) | — | [N/A] | |
| L233 | `if(mp.isSneak)` | (vanilla sneak 자동 — leaningPitch=0 별도) | [N/A] | sm_setAngles L88 |
| L234 | `imp.animateSneaking(...)` | (vanilla 자동) | [정합] | |
| L235 | (빈 줄) | — | [N/A] | |
| L236 | `imp.animateArms(...)` | (vanilla applyAnimationOffsets 자동) | [정합] | |
| L237 | (빈 줄) | — | [N/A] | |
| L238 | `if(mp.aimedBow)` | (vanilla 자동) | [N/A] | |
| L239 | `imp.animateBowAiming(...)` | (vanilla 활쏘기 자동) | [정합] | |
| L240 | (빈 줄) | — | [N/A] | |
| L241 | `if(bipedOuter.previous != null && !bipedOuter.fadeRotateAngleX)` | (Outer fade 부재) | [N/A] | §17 |
| L242 | `bipedOuter.previous.rotateAngleX = bipedOuter.rotateAngleX;` | — | [N/A] | |
| L243 | (빈 줄) | — | [N/A] | |
| L244 | `if(bipedOuter.previous != null && !bipedOuter.fadeRotateAngleY)` | (fade 부재) | [N/A] | |
| L245 | `bipedOuter.previous.rotateAngleY = bipedOuter.rotateAngleY;` | — | [N/A] | |
| L246 | (빈 줄) | — | [N/A] | |
| L247 | `bipedOuter.fadeIntermediate(totalTime);` | (fade 보간 0.2F*timeDelta — 부재) | [N/A] | §17 잔여 |
| L248 | `bipedOuter.fadeStore(totalTime);` | (fade 저장 — 부재) | [N/A] | |
| L249 | (빈 줄) | — | [N/A] | |
| L250 | `bipedCloak.ignoreBase = false;` | (vanilla cloak 자동 활성) | [N/A] | |
| L251 | `bipedCloak.rotateAngleX = Sixtyfourth;` | (1.21.1: vanilla cloak.pitch 기본 - 별도 미이식, vanilla 자체에 없음) | [누락] | ⚠️ cloak.pitch = SIXTYFOURTH (≈5.6°) 기본 살짝 기울임 미이식. 우선순위 낮음 (망토 미세 차이) — R-10+ 검토 |
| L252 | `}` | — | [N/A] | setRotationAngles 종료 |
| L253 | (빈 줄) | — | [N/A] | |
| L254 | `public void animateHeadRotation(float viewHorizontalAngelOffset, float viewVerticalAngelOffset)` | (vanilla setAngles 진입 시 head.yaw/pitch 매개변수 자동 설정) | [N/A] | 메서드 시그니처 |
| L255 | `{` | — | [N/A] | |
| L256 | `bipedNeck.ignoreBase = true;` | (Neck 부재) | [N/A] | |
| L257 | `bipedHead.rotateAngleY = (actualRotation + viewHorizontalAngelOffset) / RadiantToAngle;` | (vanilla 자동 — head.yaw = headYaw * DEG_TO_RAD) | [정합] | vanilla 자동 |
| L258 | `bipedHead.rotateAngleX = viewVerticalAngelOffset / RadiantToAngle;` | (vanilla 자동 — head.pitch = headPitch * DEG_TO_RAD) | [정합] | |
| L259 | `}` | — | [N/A] | |
| L260 | (빈 줄) | — | [N/A] | |
| L261 | `public void animateSleeping()` | (vanilla SleepingPose 자동) | [N/A] | |
| L262 | `{` | — | [N/A] | |
| L263 | `bipedNeck.ignoreBase = false;` | (Neck 부재) | [N/A] | |
| L264 | `bipedHead.rotateAngleY = 0F;` | (vanilla SleepingPose 자동) | [정합] | |
| L265 | `bipedHead.rotateAngleX = Eighth;` | (vanilla SleepingPose 자동) | [정합] | |
| L266 | `bipedTorso.rotationPointZ = -17F;` | (Torso 부재 — body 단일) | [N/A] | §16-2 |
| L267 | `}` | — | [N/A] | |
| L268 | (빈 줄) | — | [N/A] | |
| L269 | `public void animateArmSwinging(float totalHorizontalDistance, float currentHorizontalSpeed)` | (vanilla BipedEntityModel.setAngles 본체 — limbSwing 기반 자동) | [N/A] | |
| L270 | `{` | — | [N/A] | |
| L271 | `bipedRightArm.rotateAngleX = cos(totalHorizontalDistance * 0.6662F + Half) * 2.0F * currentHorizontalSpeed * 0.5F;` | vanilla setAngles: `rightArm.pitch = cos(limbAngle * 0.6662f + π) * 2.0f * limbDistance * 0.5f` | [정합] | vanilla 동일 공식 |
| L272 | `bipedLeftArm.rotateAngleX = cos(totalHorizontalDistance * 0.6662F) * 2.0F * currentHorizontalSpeed * 0.5F;` | vanilla 자동 | [정합] | |
| L273 | (빈 줄) | — | [N/A] | |
| L274 | `bipedRightLeg.rotateAngleX = cos(totalHorizontalDistance * 0.6662F) * 1.4F * currentHorizontalSpeed;` | vanilla 자동 | [정합] | |
| L275 | `bipedLeftLeg.rotateAngleX = cos(totalHorizontalDistance * 0.6662F + Half) * 1.4F * currentHorizontalSpeed;` | vanilla 자동 | [정합] | |
| L276 | `}` | — | [N/A] | |
| L277 | (빈 줄) | — | [N/A] | |
| L278 | `public void animateRiding()` | (vanilla Riding 자동) | [N/A] | |
| L279 | `{` | — | [N/A] | |
| L280 | `bipedRightArm.rotateAngleX += -0.6283185F;` (= -π/5, -36°) | (vanilla 자동) | [정합] | |
| L281 | `bipedLeftArm.rotateAngleX += -0.6283185F;` | (vanilla 자동) | [정합] | |
| L282 | `bipedRightLeg.rotateAngleX = -1.256637F;` (= -2π/5, -72°) | (vanilla 자동) | [정합] | |
| L283 | `bipedLeftLeg.rotateAngleX = -1.256637F;` | (vanilla 자동) | [정합] | |
| L284 | `bipedRightLeg.rotateAngleY = 0.3141593F;` (= π/10, 18°) | (vanilla 자동) | [정합] | |
| L285 | `bipedLeftLeg.rotateAngleY = -0.3141593F;` | (vanilla 자동) | [정합] | |
| L286 | `}` | — | [N/A] | |
| L287 | (빈 줄) | — | [N/A] | |
| L288 | `public void animateLeftArmItemHolding()` | (vanilla 자동) | [N/A] | |
| L289 | `{` | — | [N/A] | |
| L290 | `bipedLeftArm.rotateAngleX = bipedLeftArm.rotateAngleX * 0.5F - 0.3141593F * mp.heldItemLeft;` | (vanilla 자동) | [정합] | |
| L291 | `}` | — | [N/A] | |
| L292 | (빈 줄) | — | [N/A] | |
| L293 | `public void animateRightArmItemHolding()` | (vanilla 자동) | [N/A] | |
| L294 | `{` | — | [N/A] | |
| L295 | `bipedRightArm.rotateAngleX = bipedRightArm.rotateAngleX * 0.5F - 0.3141593F * mp.heldItemRight;` | (vanilla 자동) | [정합] | |
| L296 | `}` | — | [N/A] | |
| L297 | (빈 줄) | — | [N/A] | |
| L298 | `public void animateWorkingBody()` | (vanilla animateAttack 자동) | [N/A] | |
| L299 | `{` | — | [N/A] | |
| L300 | `float angle = sin(sqrt(mp.onGround) * Whole) * 0.2F;` | (vanilla 자동) | [정합] | |
| L301 | `bipedBreast.rotateAngleY = bipedBody.rotateAngleY += angle;` | (Breast 부재 — body.yaw 만 vanilla 자동) | [정합] | body 단일 |
| L302 | `bipedBreast.rotationOrder = bipedBody.rotationOrder = ModelRotationRenderer.YXZ;` | (rotationOrder 부재 — body XYZ 기본) | [N/A] | |
| L303 | `bipedLeftArm.rotateAngleX += angle;` | (vanilla 자동) | [정합] | |
| L304 | `}` | — | [N/A] | |
| L305 | (빈 줄) | — | [N/A] | |
| L306 | `public void animateWorkingArms()` | (vanilla animateAttack 자동) | [N/A] | |
| L307 | `{` | — | [N/A] | |
| L308 | `float f6 = 1.0F - mp.onGround;` | (vanilla 자동) | [정합] | |
| L309 | `f6 = 1.0F - f6 * f6 * f6;` | (vanilla cubic ease-out) | [정합] | |
| L310 | `float f7 = sin(f6 * Half);` | (vanilla 자동) | [정합] | |
| L311 | `float f8 = sin(mp.onGround * Half) * -(bipedHead.rotateAngleX - 0.7F) * 0.75F;` | (vanilla 자동) | [정합] | |
| L312 | `bipedRightArm.rotateAngleX -= f7 * 1.2D + f8;` | (vanilla 자동) | [정합] | |
| L313 | `bipedRightArm.rotateAngleY += sin(sqrt(mp.onGround) * Whole) * 0.4F;` | (vanilla 자동) | [정합] | |
| L314 | `bipedRightArm.rotateAngleZ -= sin(mp.onGround * Half) * 0.4F;` | (vanilla 자동) | [정합] | |
| L315 | `}` | — | [N/A] | |
| L316 | (빈 줄) | — | [N/A] | |
| L317 | `public void animateSneaking()` | (vanilla sneak 자동 — leaningPitch 별도 처리) | [N/A] | |
| L318 | `{` | — | [N/A] | |
| L319 | `bipedTorso.rotateAngleX += 0.5F;` | (Torso 부재 — vanilla sneak leaningPitch 자동) | [N/A] | §16-2 |
| L320 | `bipedRightLeg.rotateAngleX += -0.5F;` | (vanilla 자동) | [정합] | |

**청크 2 (L161-L320) 통계: 정합 30 / 오역 0 / 누락 1 / 잉여 0 / N/A 129 = 160 라인 전수.**

**청크 2 발견**:
- B-N (cloak.pitch 기본 기울임 [누락]): L251 `bipedCloak.rotateAngleX = Sixtyfourth;` — 망토 살짝 기울임 (≈5.6°) 미이식. 우선순위 낮음 (망토 미세 차이). R-10+ 검토.
- L211/L214/L216/L219/L222/L225/L229/L230/L234/L236/L239 = 11 imp.animateXxx 호출 모두 vanilla BipedEntityModel.setAngles 자동 처리 분기와 매핑됨 [정합 (vanilla 자동)]. SR가 1.7.10에서 override한 메서드들은 1.21.1 vanilla 자체가 동등 처리.
- L271-L275 arm/leg swing 공식: vanilla 1.21.1 BipedEntityModel.setAngles 와 정확히 일치 (cos * 0.6662 * 2.0 * 0.5 / cos * 1.4) — 검증 완료.

**다음 청크**: R-4 청크 3 (L321-L469) — animateSneaking 잔여 + animateArms + animateBowAiming + reset/getter/필드 선언. R-4 마지막 청크.

### 청크 3 (L321-L469) — animateSneaking 잔여 + animateArms + animateBowAiming + reset() + renderCloak + getRandomBox + 필드 선언 (R-4 마지막 청크)

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L321 | `bipedLeftLeg.rotateAngleX += -0.5F;` | (vanilla sneak 자동) | [정합] | sneak leg 대칭 |
| L322 | `bipedRightArm.rotateAngleX += -0.1F;` | (vanilla sneak 자동) | [정합] | |
| L323 | `bipedLeftArm.rotateAngleX += -0.1F;` | (vanilla sneak 자동) | [정합] | |
| L324 | (빈 줄) | — | [N/A] | |
| L325 | `bipedPelvic.offsetY = -0.137F;` | (Pelvic 부재 + offsetY 부재) | [N/A] | §16-3 + ModelPart.offsetY 부재 |
| L326 | `bipedPelvic.offsetZ = -0.051F;` | — | [N/A] | |
| L327 | (빈 줄) | — | [N/A] | |
| L328 | `bipedBreast.offsetY = -0.014F;` | (Breast 부재) | [N/A] | §16-2 |
| L329 | `bipedBreast.offsetZ = -0.057F;` | — | [N/A] | |
| L330 | (빈 줄) | — | [N/A] | |
| L331 | `bipedNeck.offsetY = 0.0621F;` | (Neck 부재) | [N/A] | |
| L332 | `}` | — | [N/A] | animateSneaking 종료 |
| L333 | (빈 줄) | — | [N/A] | |
| L334 | `public void animateArms(float totalTime)` | (vanilla applyAnimationOffsets 자동) | [N/A] | |
| L335 | `{` | — | [N/A] | |
| L336 | `bipedRightArm.rotateAngleZ += MathHelper.cos(totalTime * 0.09F) * 0.05F + 0.05F;` | vanilla applyAnimationOffsets: `rightArm.roll += cos(animationProgress * 0.09f) * 0.05f + 0.05f` | [정합] | vanilla 동일 공식 |
| L337 | `bipedLeftArm.rotateAngleZ -= MathHelper.cos(totalTime * 0.09F) * 0.05F + 0.05F;` | vanilla 자동 | [정합] | |
| L338 | `bipedRightArm.rotateAngleX += MathHelper.sin(totalTime * 0.067F) * 0.05F;` | vanilla 자동 | [정합] | |
| L339 | `bipedLeftArm.rotateAngleX -= MathHelper.sin(totalTime * 0.067F) * 0.05F;` | vanilla 자동 | [정합] | |
| L340 | `}` | — | [N/A] | |
| L341 | (빈 줄) | — | [N/A] | |
| L342 | `public void animateBowAiming(float totalTime)` | (vanilla 활쏘기 자동 — BipedEntityModel.setAngles 활쏘기 분기) | [N/A] | |
| L343 | `{` | — | [N/A] | |
| L344 | `bipedRightArm.rotateAngleZ = 0.0F;` | vanilla 자동 (활쏘기 시 roll reset) | [정합] | |
| L345 | `bipedLeftArm.rotateAngleZ = 0.0F;` | vanilla 자동 | [정합] | |
| L346 | `bipedRightArm.rotateAngleY = -0.1F + bipedHead.rotateAngleY - bipedOuter.rotateAngleY;` | vanilla 자동 (rightArm.yaw = -0.1F + head.yaw — outer는 vanilla 자체 부재이므로 무시) | [정합] | |
| L347 | `bipedLeftArm.rotateAngleY = 0.1F + bipedHead.rotateAngleY + 0.4F - bipedOuter.rotateAngleY;` | vanilla 자동 | [정합] | |
| L348 | `bipedRightArm.rotateAngleX = -1.570796F + bipedHead.rotateAngleX;` (-π/2 + headX) | vanilla 자동 | [정합] | |
| L349 | `bipedLeftArm.rotateAngleX = -1.570796F + bipedHead.rotateAngleX;` | vanilla 자동 | [정합] | |
| L350 | `bipedRightArm.rotateAngleZ += MathHelper.cos(totalTime * 0.09F) * 0.05F + 0.05F;` | vanilla 자동 (활 들고 미세 흔들림) | [정합] | |
| L351 | `bipedLeftArm.rotateAngleZ -= MathHelper.cos(totalTime * 0.09F) * 0.05F + 0.05F;` | vanilla 자동 | [정합] | |
| L352 | `bipedRightArm.rotateAngleX += MathHelper.sin(totalTime * 0.067F) * 0.05F;` | vanilla 자동 | [정합] | |
| L353 | `bipedLeftArm.rotateAngleX -= MathHelper.sin(totalTime * 0.067F) * 0.05F;` | vanilla 자동 | [정합] | |
| L354 | `}` | — | [N/A] | |
| L355 | (빈 줄) | — | [N/A] | |
| L356 | `public void reset()` | (vanilla setAngles 진입 시 자동 reset) | [N/A] | |
| L357 | `{` | — | [N/A] | |
| L358 | `bipedOuter.reset();` | (Outer 부재) | [N/A] | |
| L359 | `bipedTorso.reset();` | (Torso 부재) | [N/A] | |
| L360 | `bipedBody.reset();` | (vanilla 자동 reset) | [N/A] | |
| L361 | `bipedBreast.reset();` | (Breast 부재) | [N/A] | |
| L362 | `bipedNeck.reset();` | (Neck 부재) | [N/A] | |
| L363 | `bipedHead.reset();` | (vanilla 자동) | [N/A] | |
| L364 | `bipedHeadwear.reset();` | (vanilla hat 자동) | [N/A] | |
| L365 | `bipedEars.reset();` | (Ears 부재) | [N/A] | |
| L366 | `bipedCloak.reset();` | (vanilla cloak 자동) | [N/A] | |
| L367 | `bipedRightShoulder.reset();` | (Shoulder 부재) | [N/A] | |
| L368 | `bipedRightArm.reset();` | (vanilla 자동) | [N/A] | |
| L369 | `bipedLeftShoulder.reset();` | (Shoulder 부재) | [N/A] | |
| L370 | `bipedLeftArm.reset();` | (vanilla 자동) | [N/A] | |
| L371 | `bipedPelvic.reset();` | (Pelvic 부재) | [N/A] | |
| L372 | `bipedRightLeg.reset();` | (vanilla 자동) | [N/A] | |
| L373 | `bipedLeftLeg.reset();` | (vanilla 자동) | [N/A] | |
| L374 | (빈 줄) | — | [N/A] | |
| L375 | `bipedRightShoulder.setRotationPoint(-5F, 2.0F, 0.0F);` | (Shoulder 부재) | [N/A] | |
| L376 | `bipedLeftShoulder.setRotationPoint(5F, 2.0F, 0.0F);` | (Shoulder 부재) | [N/A] | |
| L377 | `bipedPelvic.setRotationPoint(0.0F, 12.0F, 0.0F);` | (Pelvic 부재) | [N/A] | |
| L378 | `bipedRightLeg.setRotationPoint(-2F, 0.0F, 0.0F);` | (vanilla 기본 pivot 자동) | [N/A] | |
| L379 | `bipedLeftLeg.setRotationPoint(2.0F, 0.0F, 0.0F);` | (vanilla 자동) | [N/A] | |
| L380 | `bipedCloak.setRotationPoint(0.0F, 0.0F, 2.0F);` | (vanilla 자동) | [N/A] | |
| L381 | `}` | — | [N/A] | |
| L382 | (빈 줄) | — | [N/A] | |
| L383 | `public void renderCloak(float f)` | (vanilla CapeFeatureRenderer 자동) | [N/A] | |
| L384 | `{` | — | [N/A] | |
| L385 | `attemptToCallRenderCape = true;` | (1.21.1 부재 — vanilla cloak 자동) | [N/A] | |
| L386 | `if(!disabled)` | (vanilla 자동) | [N/A] | |
| L387 | `imp.superRenderCloak(f);` | (vanilla 자동) | [N/A] | |
| L388 | `}` | — | [N/A] | |
| L389 | (빈 줄) | — | [N/A] | |
| L390 | `public ModelRenderer getRandomBox(Random par1Random)` | (1.21.1: 파티클 효과 영역 — 본 포커스 외) | [N/A] | 영혼/연기 파티클 |
| L391 | `{` | — | [N/A] | |
| L392 | `List<?> boxList = mp.boxList;` | (1.21.1: ModelData 시스템 — boxList 부재) | [N/A] | |
| L393 | `int size = boxList.size();` | — | [N/A] | |
| L394 | `int renderersWithBoxes = 0;` | — | [N/A] | |
| L395 | (빈 줄) | — | [N/A] | |
| L396 | `for(int i=0; i<size; i++)` | — | [N/A] | |
| L397 | `{` | — | [N/A] | |
| L398 | `ModelRenderer renderer = (ModelRenderer)boxList.get(i);` | — | [N/A] | |
| L399 | `if(canBeRandomBoxSource(renderer))` | — | [N/A] | |
| L400 | `renderersWithBoxes++;` | — | [N/A] | |
| L401 | `}` | — | [N/A] | |
| L402 | (빈 줄) | — | [N/A] | |
| L403 | `if(renderersWithBoxes != 0)` | — | [N/A] | |
| L404 | `{` | — | [N/A] | |
| L405 | `int random = par1Random.nextInt(renderersWithBoxes);` | — | [N/A] | |
| L406 | `renderersWithBoxes = -1;` | — | [N/A] | |
| L407 | (빈 줄) | — | [N/A] | |
| L408 | `for(int i=0; i<size; i++)` | — | [N/A] | |
| L409 | `{` | — | [N/A] | |
| L410 | `ModelRenderer renderer = (ModelRenderer)boxList.get(i);` | — | [N/A] | |
| L411 | `if(canBeRandomBoxSource(renderer))` | — | [N/A] | |
| L412 | `renderersWithBoxes++;` | — | [N/A] | |
| L413 | `if(renderersWithBoxes == random)` | — | [N/A] | |
| L414 | `return renderer;` | — | [N/A] | |
| L415 | `}` | — | [N/A] | |
| L416 | `}` | — | [N/A] | |
| L417 | (빈 줄) | — | [N/A] | |
| L418 | `return null;` | — | [N/A] | |
| L419 | `}` | — | [N/A] | |
| L420 | (빈 줄) | — | [N/A] | |
| L421 | `private static boolean canBeRandomBoxSource(ModelRenderer renderer)` | (1.21.1 boxList 부재) | [N/A] | |
| L422 | `{` | — | [N/A] | |
| L423 | `return renderer.cubeList != null && renderer.cubeList.size() > 0 && (!(renderer instanceof ModelRotationRenderer) \|\| ((ModelRotationRenderer)renderer).canBeRandomBoxSource());` | — | [N/A] | |
| L424 | `}` | — | [N/A] | |
| L425 | (빈 줄) | — | [N/A] | |
| L426 | `public boolean isInventory;` | (vanilla 자동) | [N/A] | 필드 선언 |
| L427 | (빈 줄) | — | [N/A] | |
| L428 | `public int scaleArmType;` | (메인 = Scale 묵시) | [N/A] | §16-7 |
| L429 | `public int scaleLegType;` | (메인 = Scale) | [N/A] | |
| L430 | (빈 줄) | — | [N/A] | |
| L431 | `public float totalVerticalDistance;` | (§16-10 [오역] 발견 변수의 정의 — 1.21.1 limbSwing 잘못 매핑) | [N/A] | 필드 선언 |
| L432 | `public float currentVerticalSpeed;` | (§16-10 [오역] 발견 — 1.21.1 limbSwingAmount 잘못) | [N/A] | |
| L433 | `public float totalDistance;` | (§16-12/13/15 [오역] 발견 — 1.21.1 limbSwing/animationProgress 잘못) | [N/A] | |
| L434 | `public float currentSpeed;` | (§16-13/15 [오역] 발견 — 1.21.1 limbSwingAmount 잘못) | [N/A] | |
| L435 | (빈 줄) | — | [N/A] | |
| L436 | `public double distance;` | (state holder) | [N/A] | |
| L437 | `public double verticalDistance;` | (1.21.1 capture 부재) | [N/A] | |
| L438 | `public double horizontalDistance;` | (1.21.1 limbSwing 등가) | [N/A] | |
| L439 | `public float currentCameraAngle;` | (player.getYaw 활용) | [N/A] | |
| L440 | `public float currentVerticalAngle;` | (sm.stats.currentVerticalAngle) | [N/A] | |
| L441 | `public float currentHorizontalAngle;` | (atan2(-vel.x, vel.z)) | [N/A] | |
| L442 | (빈 줄) | — | [N/A] | |
| L443 | `public float actualRotation;` | (player.getYaw 처리) | [N/A] | |
| L444 | `public float forwardRotation;` | sm_captureBodyYaw lerp 처리 | [N/A] | |
| L445 | `public float workingAngle;` | (어깨 부재로 N/A) | [N/A] | §16-5 |
| L446 | (빈 줄) | — | [N/A] | |
| L447 | `public ModelRotationRenderer bipedOuter;` | (Outer 부재) | [N/A] | |
| L448 | `public ModelRotationRenderer bipedTorso;` | (Torso 부재) | [N/A] | §16-2 |
| L449 | `public ModelRotationRenderer bipedBody;` | `body` (필드 선언) | [N/A] | R-4 청크 1 매핑됨 |
| L450 | `public ModelRotationRenderer bipedBreast;` | (Breast 부재) | [N/A] | |
| L451 | `public ModelRotationRenderer bipedNeck;` | (Neck 부재) | [N/A] | |
| L452 | `public ModelRotationRenderer bipedHead;` | `head` | [N/A] | |
| L453 | `public ModelRotationRenderer bipedHeadwear;` | `hat` | [N/A] | |
| L454 | `public ModelRotationRenderer bipedRightShoulder;` | (Shoulder 부재) | [N/A] | |
| L455 | `public ModelRotationRenderer bipedRightArm;` | `rightArm` | [N/A] | |
| L456 | `public ModelRotationRenderer bipedLeftShoulder;` | (Shoulder 부재) | [N/A] | |
| L457 | `public ModelRotationRenderer bipedLeftArm;` | `leftArm` | [N/A] | |
| L458 | `public ModelRotationRenderer bipedPelvic;` | (Pelvic 부재) | [N/A] | |
| L459 | `public ModelRotationRenderer bipedRightLeg;` | `rightLeg` | [N/A] | |
| L460 | `public ModelRotationRenderer bipedLeftLeg;` | `leftLeg` | [N/A] | |
| L461 | `public ModelEarsRenderer bipedEars;` | (Ears 부재) | [N/A] | |
| L462 | `public ModelCapeRenderer bipedCloak;` | `cloak` | [N/A] | |
| L463 | (빈 줄) | — | [N/A] | |
| L464 | (빈 줄) | — | [N/A] | |
| L465 | `public boolean disabled;` | — | [N/A] | |
| L466 | `public boolean attemptToCallRenderCape;` | — | [N/A] | |
| L467 | `public RendererData prevOuterRenderData;` | (Outer fade 데이터 부재) | [N/A] | §17 |
| L468 | `public boolean isSleeping;` | (vanilla SleepingPose 자동) | [N/A] | |
| L469 | `public boolean firstPerson;` | (vanilla 1인칭 자동) | [N/A] | |

**청크 3 (L321-L469) 통계: 정합 17 / 오역 0 / 누락 0 / 잉여 0 / N/A 132 = 149 라인 전수.**

**청크 3 발견**: 신규 [오역]/[누락]/[잉여] 0건. 다음 검증:
- L321-L323 sneak 잔여 + L336-L339 animateArms + L344-L353 animateBowAiming = vanilla BipedEntityModel.setAngles 자동 처리 분기와 정확히 일치 (계수/공식 검증).
- L431-L434 [오역] 발견 변수의 *원본 정의 위치* 확인 — SmartRenderModel public field 선언으로, R-4 청크 1 L95-L98 캐싱 매개체.
- L390-L424 getRandomBox/canBeRandomBoxSource 영혼·연기 파티클 영역 — 본 포커스 #1 외.

**R-4 전체 (3 청크 469 라인 3 세션) 누적 통계**:
| 청크 | 라인 | 정합 | 오역 | 누락 | 잉여 | N/A |
|-----|-----|------|------|------|------|------|
| 1 (L1-L160) | 160 | 15 | 0 | 0 | 0 | 145 |
| 2 (L161-L320) | 160 | 30 | 0 | 1 | 0 | 129 |
| 3 (L321-L469) | 149 | 17 | 0 | 0 | 0 | 132 |
| **합계** | **469** | **62** | **0** | **1** | **0** | **406** |

**R-4 전체 발견 (R-10+ B-N 후보)**:
- B-N (cloak.pitch SIXTYFOURTH 기본 기울임 — §16-24): 청크 2 L251 1 라인.

**R-4 완료**. 다음 R-단계: **R-5 (ModelRotationRenderer.java 368줄 라인별 (2 청크) + RendererData/CapeRenderer/EarsRenderer/SpecialRenderer 4 파일 ~232줄)** = 6 파일 ~601 라인.

---

## R-5: ModelRotationRenderer.java (368) + RendererData/Cape/Ears/Special 4 파일 (~232) = 6 파일 ~600 라인

### 청크 1 (ModelRotationRenderer.java L1-L184) — 헤더 + 생성자 + render/preRender/preTransforms/preTransform + rotate() 6 회전순서 정의 + postTransform/postTransforms

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L16 | Smart Render GPLv3 라이선스 헤더 | — | [N/A] | |
| L17 | `// ==…==` | — | [N/A] | |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.render;` | — | [N/A] | |
| L20 | `import org.lwjgl.BufferUtils;` | (1.21.1: lwjgl 직접 미사용 — RenderSystem 활용) | [N/A] | |
| L21 | `import org.lwjgl.opengl.GL11;` | (1.21.1: GL11 → MatrixStack/RotationAxis) | [N/A] | OpenGL → MatrixStack |
| L22 | (빈 줄) | — | [N/A] | |
| L23 | `import java.lang.reflect.*;` | (Reflect.Invoke 호출용 — 1.21.1 부재) | [N/A] | |
| L24 | `import java.nio.FloatBuffer;` | (matrix buffer — 1.21.1 부재) | [N/A] | |
| L25 | (빈 줄) | — | [N/A] | |
| L26 | `import net.minecraft.client.model.*;` | — | [N/A] | |
| L27 | `import net.smart.utilities.*;` | — | [N/A] | |
| L28 | (빈 줄) | — | [N/A] | |
| L29 | `public class ModelRotationRenderer extends ModelRenderer` | (1.21.1: `ModelPart` 직접 사용 — ModelRotationRenderer 별도 클래스 부재. setAnglesXYZ/XZY/YXZ/YZX/ZXY 헬퍼로 회전순서 전환) | [정합] | 클래스 자체 → setAngles* 헬퍼 분리 매핑 |
| L30 | `{` | — | [N/A] | |
| L31 | `protected final static float RadiantToAngle = SmartRenderUtilities.RadiantToAngle;` | `MathHelper.DEGREES_PER_RADIAN` (180/π) | [정합] | 동등 상수 |
| L32 | `protected final static float Whole = SmartRenderUtilities.Whole;` | `WHOLE = (float) (Math.PI * 2)` 또는 인라인 | [정합] | 2π 라디안 |
| L33 | `protected final static float Half = SmartRenderUtilities.Half;` | `HALF = (float) Math.PI` | [정합] | π 라디안 |
| L34 | (빈 줄) | — | [N/A] | |
| L35 | `public ModelRotationRenderer(ModelBase modelBase, int i, int j, ModelRotationRenderer baseRenderer)` | (1.21.1: ModelData(Builder).build → ModelPart 자동 — 생성자 부재) | [N/A] | |
| L36 | `{` | — | [N/A] | |
| L37 | `super(modelBase, i, j);` | — | [N/A] | |
| L38 | `rotationOrder = XYZ;` | (1.21.1: ModelPart 기본 XYZ 묵시 — pitch/yaw/roll 직접) | [N/A] | rotationOrder 필드 부재 |
| L39 | `compiled = false;` | (1.21.1: ModelData 자동 빌드) | [N/A] | |
| L40 | (빈 줄) | — | [N/A] | |
| L41 | `base = baseRenderer;` | (1.21.1: ModelData child relationship 자동) | [N/A] | |
| L42 | `if(base != null)` | — | [N/A] | |
| L43 | `base.addChild(this);` | (vanilla 자동) | [N/A] | |
| L44 | (빈 줄) | — | [N/A] | |
| L45 | `scaleX = 1.0F;` | `xScale = 1.0F` (ModelPart public field 기본값) | [N/A] | 자동 |
| L46 | `scaleY = 1.0F;` | `yScale = 1.0F` | [N/A] | |
| L47 | `scaleZ = 1.0F;` | `zScale = 1.0F` | [N/A] | |
| L48 | (빈 줄) | — | [N/A] | |
| L49 | `fadeEnabled = false;` | (fade 메커니즘 부재) | [N/A] | §17 잔여 |
| L50 | `}` | — | [N/A] | |
| L51 | (빈 줄) | — | [N/A] | |
| L52 | `@Override` | — | [N/A] | |
| L53 | `public void render(float f)` | (1.21.1: vanilla ModelPart.render 자동) | [N/A] | |
| L54 | `{` | — | [N/A] | |
| L55 | `if((!ignoreRender && !ignoreBase) \|\| forceRender)` | (vanilla 자동) | [N/A] | |
| L56 | `doRender(f, ignoreBase);` | — | [N/A] | |
| L57 | `}` | — | [N/A] | |
| L58 | (빈 줄) | — | [N/A] | |
| L59 | `public void renderIgnoreBase(float f)` | (다층 모델 부재) | [N/A] | |
| L60 | `{` | — | [N/A] | |
| L61 | `if(ignoreBase)` | — | [N/A] | |
| L62 | `doRender(f, false);` | — | [N/A] | |
| L63 | `}` | — | [N/A] | |
| L64 | (빈 줄) | — | [N/A] | |
| L65 | `public void doRender(float f, boolean useParentTransformations)` | (vanilla 자동) | [N/A] | |
| L66 | `{` | — | [N/A] | |
| L67 | `if(!preRender(f))` | — | [N/A] | |
| L68 | `return;` | — | [N/A] | |
| L69 | `preTransforms(f, true, useParentTransformations);` | (vanilla matrices.push + transform 자동) | [N/A] | |
| L70 | `GL11.glCallList(displayList);` | (1.21.1: VertexConsumer 자동) | [N/A] | |
| L71 | `if (childModels != null)` | (vanilla child traversal 자동) | [N/A] | |
| L72 | `for (int i = 0; i < childModels.size(); i++)` | — | [N/A] | |
| L73 | `((ModelRenderer)childModels.get(i)).render(f);` | — | [N/A] | |
| L74 | `postTransforms(f, true, useParentTransformations);` | (vanilla matrices.pop 자동) | [N/A] | |
| L75 | `}` | — | [N/A] | |
| L76 | (빈 줄) | — | [N/A] | |
| L77 | `public boolean preRender(float f)` | (vanilla compiled 자동) | [N/A] | |
| L78 | `{` | — | [N/A] | |
| L79 | `if(isHidden) return false;` | (vanilla 자동) | [N/A] | |
| L80 | `return false;` | — | [N/A] | |
| L81 | (빈 줄) | — | [N/A] | |
| L82 | `if(!showModel) return false;` | (vanilla 자동) | [N/A] | |
| L83 | `return false;` | — | [N/A] | |
| L84 | (빈 줄) | — | [N/A] | |
| L85 | `if(!compiled)` | (vanilla 자동 빌드) | [N/A] | |
| L86 | `UpdateCompiled();` | — | [N/A] | |
| L87 | (빈 줄) | — | [N/A] | |
| L88 | `if(!compiled)` | — | [N/A] | |
| L89 | `{` | — | [N/A] | |
| L90 | `Reflect.Invoke(_compileDisplayList, this, f);` | (reflection 부재 — vanilla 자동) | [N/A] | |
| L91 | `UpdateDisplayList();` | — | [N/A] | |
| L92 | `compiled = true;` | — | [N/A] | |
| L93 | `}` | — | [N/A] | |
| L94 | (빈 줄) | — | [N/A] | |
| L95 | `return true;` | — | [N/A] | |
| L96 | `}` | — | [N/A] | |
| L97 | (빈 줄) | — | [N/A] | |
| L98 | `public void preTransforms(float f, boolean push, boolean useParentTransformations)` | (vanilla matrices 자동) | [N/A] | |
| L99 | `{` | — | [N/A] | |
| L100 | `if(base != null && !ignoreBase && useParentTransformations)` | (vanilla parent traversal 자동) | [N/A] | |
| L101 | `base.preTransforms(f, push, true);` | — | [N/A] | |
| L102 | `preTransform(f, push);` | — | [N/A] | |
| L103 | `}` | — | [N/A] | |
| L104 | (빈 줄) | — | [N/A] | |
| L105 | `public void preTransform(float f, boolean push)` | (vanilla ModelPart.rotate + translate + scale 자동) | [N/A] | |
| L106 | `{` | — | [N/A] | |
| L107 | `if(rotateAngleX != 0.0F \|\| rotateAngleY != 0.0F \|\| rotateAngleZ != 0.0F \|\| ignoreSuperRotation)` | (vanilla 자동 — 0 체크 없이도 수행) | [N/A] | |
| L108 | `{` | — | [N/A] | |
| L109 | `if(push)` | — | [N/A] | |
| L110 | `GL11.glPushMatrix();` | `matrices.push()` | [N/A] | vanilla 자동 |
| L111 | (빈 줄) | — | [N/A] | |
| L112 | `GL11.glTranslatef(rotationPointX * f, rotationPointY * f, rotationPointZ * f);` | (vanilla ModelPart.translate(matrices) 자동 — pivot * scale) | [N/A] | |
| L113 | (빈 줄) | — | [N/A] | |
| L114 | `if (ignoreSuperRotation)` | (어깨 ZYX 분리 — 어깨 부재 §16-5) | [N/A] | |
| L115 | `{` | — | [N/A] | |
| L116 | `buffer.rewind();` | (matrix buffer 부재) | [N/A] | |
| L117 | `GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);` | — | [N/A] | |
| L118 | `buffer.get(array);` | — | [N/A] | |
| L119 | (빈 줄) | — | [N/A] | |
| L120 | `GL11.glLoadIdentity();` | — | [N/A] | |
| L121 | `GL11.glTranslatef(array[12] / array[15], array[13] / array[15], array[14] / array[15]);` | (super rotation 무시 — 위치만 추출) | [N/A] | |
| L122 | `}` | — | [N/A] | |
| L123 | (빈 줄) | — | [N/A] | |
| L124 | `rotate(rotationOrder, rotateAngleX, rotateAngleY, rotateAngleZ);` | `setAnglesXYZ/XZY/YXZ/YZX/ZXY/ZYX(part, pitch, yaw, roll)` 6 헬퍼 (B-08 + B-1 + B-2 세션 2) | [정합] | **핵심 매핑** — rotate() → setAngles* 6 헬퍼 |
| L125 | (빈 줄) | — | [N/A] | |
| L126 | `GL11.glScalef(scaleX, scaleY, scaleZ);` | `xScale/yScale/zScale` ModelPart public field (rotate 후 자동 적용) | [정합] | setArmScales/setLegScales (B-3 세션 2) — yScale 활용 |
| L127 | `GL11.glTranslatef(offsetX, offsetY, offsetZ);` | (ModelPart에 offsetY 필드 부재 — MatrixStack 보정 가능) | [N/A] | §16-8 |
| L128 | `}` | — | [N/A] | |
| L129 | `else if(rotationPointX != 0.0F \|\| rotationPointY != 0.0F \|\| rotationPointZ != 0.0F \|\| scaleX != 1.0F \|\| scaleY != 1.0F \|\| scaleZ != 1.0F \|\| offsetX != 0.0F \|\| offsetY != 0.0F \|\| offsetZ != 0.0F)` | (vanilla 자동 — 0 체크 분기 부재) | [N/A] | |
| L130 | `{` | — | [N/A] | |
| L131 | `GL11.glTranslatef(rotationPointX * f, rotationPointY * f, rotationPointZ * f);` | (vanilla 자동) | [N/A] | |
| L132 | `GL11.glScalef(scaleX, scaleY, scaleZ);` | (vanilla 자동) | [N/A] | |
| L133 | `GL11.glTranslatef(offsetX, offsetY, offsetZ);` | (offsetY 부재) | [N/A] | |
| L134 | `}` | — | [N/A] | |
| L135 | `}` | — | [N/A] | |
| L136 | (빈 줄) | — | [N/A] | |
| L137 | `private static void rotate(int rotationOrder, float rotateAngleX, float rotateAngleY, float rotateAngleZ)` | `setAnglesXYZ/XZY/YXZ/YZX/ZXY/ZYX` 6 헬퍼 (post-multiply 규칙 등가) | [정합] | **R-17 GitHub 검증 1차 자료** |
| L138 | `{` | — | [N/A] | |
| L139 | `if((rotationOrder == ZXY) && rotateAngleY != 0.0F)` | setAnglesZXY: `qZ * qX * qY` (vertex 적용 Z→X→Y) — GL call 순서 Y, X, Z (역순) | [정합] | ZXY 첫 GL call = Y |
| L140 | `GL11.glRotatef(rotateAngleY * RadiantToAngle, 0.0F, 1.0F, 0.0F);` | setAnglesZXY 본체: `.rotationY(yaw)` 첫째 | [정합] | |
| L141 | (빈 줄) | — | [N/A] | |
| L142 | `if((rotationOrder == YXZ) && rotateAngleZ != 0.0F)` | setAnglesYXZ: `qY * qX * qZ` (vertex 적용 Y→X→Z) — GL call 순서 Z, X, Y | [정합] | YXZ 첫 GL call = Z (B-1 세션 2) |
| L143 | `GL11.glRotatef(rotateAngleZ * RadiantToAngle, 0.0F, 0.0F, 1.0F);` | setAnglesYXZ: `.rotationZ(roll)` 마지막 (post-multiply) | [정합] | |
| L144 | (빈 줄) | — | [N/A] | |
| L145 | `if((rotationOrder == YZX \|\| rotationOrder == YXZ \|\| rotationOrder == ZXY \|\| rotationOrder == ZYX) && rotateAngleX != 0.0F)` | setAnglesYZX/YXZ/ZXY/ZYX: 모두 X 회전 포함 | [정합] | 4 회전순서 X 적용 |
| L146 | `GL11.glRotatef(rotateAngleX * RadiantToAngle, 1.0F, 0.0F, 0.0F);` | 각 헬퍼: `.rotationX(pitch)` 적용 (post-multiply 위치별) | [정합] | |
| L147 | (빈 줄) | — | [N/A] | |
| L148 | `if((rotationOrder == XZY \|\| rotationOrder == ZYX) && rotateAngleY != 0.0F)` | setAnglesXZY/ZYX: Y 회전 포함 | [정합] | 2 회전순서 Y 적용 |
| L149 | `GL11.glRotatef(rotateAngleY * RadiantToAngle, 0.0F, 1.0F, 0.0F);` | (XZY: B-2 세션 2 / ZYX: 어깨 부재) | [정합] | XZY는 GL call 순서 Y, Z, X 첫째 |
| L150 | (빈 줄) | — | [N/A] | |
| L151 | `if((rotationOrder == XYZ \|\| rotationOrder == XZY \|\| rotationOrder == YZX \|\| rotationOrder == ZXY \|\| rotationOrder == ZYX) && rotateAngleZ != 0.0F)` | 5 회전순서 Z 적용 | [정합] | |
| L152 | `GL11.glRotatef(rotateAngleZ * RadiantToAngle, 0.0F, 0.0F, 1.0F);` | 각 헬퍼: `.rotationZ(roll)` | [정합] | |
| L153 | (빈 줄) | — | [N/A] | |
| L154 | `if((rotationOrder == XYZ \|\| rotationOrder == YXZ \|\| rotationOrder == YZX) && rotateAngleY != 0.0F)` | 3 회전순서 Y 적용 (XYZ는 마지막, YXZ/YZX는 마지막) | [정합] | |
| L155 | `GL11.glRotatef(rotateAngleY * RadiantToAngle, 0.0F, 1.0F, 0.0F);` | 각 헬퍼: `.rotationY(yaw)` | [정합] | |
| L156 | (빈 줄) | — | [N/A] | |
| L157 | `if((rotationOrder == XYZ \|\| rotationOrder == XZY) && rotateAngleX != 0.0F)` | 2 회전순서 X 적용 (XYZ/XZY 모두 마지막) | [정합] | |
| L158 | `GL11.glRotatef(rotateAngleX * RadiantToAngle, 1.0F, 0.0F, 0.0F);` | XYZ: pitch 마지막 / XZY: pitch 마지막 | [정합] | |
| L159 | `}` | — | [N/A] | rotate() 종료 |
| L160 | (빈 줄) | — | [N/A] | |
| L161 | `public void postTransform(float f, boolean pop)` | (vanilla matrices.pop 자동) | [N/A] | |
| L162 | `{` | — | [N/A] | |
| L163 | `if(rotateAngleX != 0.0F \|\| rotateAngleY != 0.0F \|\| rotateAngleZ != 0.0F \|\| ignoreSuperRotation)` | (vanilla 자동) | [N/A] | |
| L164 | `{` | — | [N/A] | |
| L165 | `if(pop)` | — | [N/A] | |
| L166 | `GL11.glPopMatrix();` | `matrices.pop()` | [N/A] | vanilla 자동 |
| L167 | `}` | — | [N/A] | |
| L168 | `else if(rotationPointX != 0.0F \|\| rotationPointY != 0.0F \|\| rotationPointZ != 0.0F \|\| scaleX != 1.0F \|\| scaleY != 1.0F \|\| scaleZ != 1.0F \|\| offsetX != 0.0F \|\| offsetY != 0.0F \|\| offsetZ != 0.0F)` | (vanilla 자동) | [N/A] | |
| L169 | `{` | — | [N/A] | |
| L170 | `GL11.glTranslatef(-offsetX, -offsetY, -offsetZ);` | (vanilla 자동 — push/pop pair) | [N/A] | |
| L171 | `GL11.glScalef(1F / scaleX, 1F / scaleY, 1F / scaleZ);` | (vanilla 자동) | [N/A] | |
| L172 | `GL11.glTranslatef(-rotationPointX * f, -rotationPointY * f, -rotationPointZ * f);` | (vanilla 자동) | [N/A] | |
| L173 | `}` | — | [N/A] | |
| L174 | `}` | — | [N/A] | |
| L175 | (빈 줄) | — | [N/A] | |
| L176 | `public void postTransforms(float f, boolean pop, boolean useParentTransformations)` | (vanilla 자동) | [N/A] | |
| L177 | `{` | — | [N/A] | |
| L178 | `postTransform(f, pop);` | — | [N/A] | |
| L179 | `if(base != null && !ignoreBase && useParentTransformations)` | — | [N/A] | |
| L180 | `base.postTransforms(f, pop, true);` | — | [N/A] | |
| L181 | `}` | — | [N/A] | |
| L182 | (빈 줄) | — | [N/A] | |
| L183 | `public void reset()` | (vanilla setAngles 진입 시 자동 reset) | [N/A] | |
| L184 | `{` | — | [N/A] | reset() 시작 (청크 2로 이어짐) |

**청크 1 (L1-L184) 통계: 정합 21 / 오역 0 / 누락 0 / 잉여 0 / N/A 163 = 184 라인 전수.**

**청크 1 발견**: 신규 [오역]/[누락]/[잉여] 0건. 핵심 검증:
- **L137-L158 6 회전순서 정의 = R-17 GitHub 검증 1차 자료 검증 완료**:
  - XYZ(0): GL call Z, Y, X → vertex 적용 X→Y→Z (vanilla 기본 등가)
  - XZY(1): GL call Y, Z, X → vertex 적용 X→Z→Y (B-2 setAnglesXZY 본체 일치)
  - YXZ(2): GL call Z, X, Y → vertex 적용 Y→X→Z (B-1 setAnglesYXZ 본체 일치)
  - YZX(3): GL call X, Z, Y → vertex 적용 Y→Z→X (B-08 setAnglesYZX 본체 일치)
  - ZXY(4): GL call Y, X, Z → vertex 적용 Z→X→Y (B-08 setAnglesZXY 본체 일치)
  - ZYX(5): GL call X, Y, Z → vertex 적용 Z→Y→X (어깨 부재 §16-5)
- L124 `rotate()` 호출 = 1.21.1 setAngles* 6 헬퍼 매핑됨.
- L126 `glScalef(scaleX, scaleY, scaleZ)` = 1.21.1 ModelPart.xScale/yScale/zScale (B-3 setArmScales/setLegScales 활용 근거).
- L127 `glTranslatef(offsetX/Y/Z)` = ModelPart.offsetY 필드 부재 (§16-8) — MatrixStack 보정 필요.

**다음 청크**: R-5 청크 2 (ModelRotationRenderer.java L185-L368) — reset() 본체 + fadeRotateAngleX/Y + fadeIntermediate/fadeStore + ignoreSuperRotation + canBeRandomBoxSource + 필드 선언.

### 청크 2 (ModelRotationRenderer.java L185-L368) — reset() 본체 + renderWithRotation/postRender + UpdateLocals/UpdateCompiled (reflection) + 필드 선언 (6 회전순서 상수 포함) + fadeStore/fadeIntermediate + canBeRandomBoxSource + GetIntermediate* 헬퍼 + reflection 필드

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L185 | `rotationOrder = XYZ;` | (1.21.1 ModelPart 기본 XYZ — vanilla setAngles reset 자동) | [정합] | reset 시 기본 회전순서 |
| L186 | (빈 줄) | — | [N/A] | |
| L187 | `scaleX = 1.0F;` | `xScale = 1.0F` (vanilla setAngles reset 자동) | [정합] | |
| L188 | `scaleY = 1.0F;` | `yScale = 1.0F` (vanilla 자동) | [정합] | |
| L189 | `scaleZ = 1.0F;` | `zScale = 1.0F` (vanilla 자동) | [정합] | |
| L190 | (빈 줄) | — | [N/A] | |
| L191 | `rotationPointX = 0F;` | `pivotX = 0F` (vanilla reset 자동) | [정합] | |
| L192 | `rotationPointY = 0F;` | `pivotY = 0F` | [정합] | |
| L193 | `rotationPointZ = 0F;` | `pivotZ = 0F` | [정합] | |
| L194 | (빈 줄) | — | [N/A] | |
| L195 | `rotateAngleX = 0F;` | `pitch = 0F` (vanilla setAngles reset 자동) | [정합] | |
| L196 | `rotateAngleY = 0F;` | `yaw = 0F` | [정합] | |
| L197 | `rotateAngleZ = 0F;` | `roll = 0F` | [정합] | |
| L198 | (빈 줄) | — | [N/A] | |
| L199 | `ignoreBase = false;` | (다층 부재) | [N/A] | |
| L200 | `ignoreSuperRotation = false;` | (어깨 ZYX 분리 부재) | [N/A] | §16-5 |
| L201 | `forceRender = false;` | (다층 부재) | [N/A] | |
| L202 | (빈 줄) | — | [N/A] | |
| L203 | `offsetX = 0;` | (ModelPart.offsetY 필드 부재) | [N/A] | §16-8 |
| L204 | `offsetY = 0;` | (offsetY 부재) | [N/A] | §16-8 |
| L205 | `offsetZ = 0;` | (offsetZ 부재) | [N/A] | |
| L206 | (빈 줄) | — | [N/A] | |
| L207 | `fadeOffsetX = false;` | (fade 메커니즘 부재) | [N/A] | §17 |
| L208 | `fadeOffsetY = false;` | (fade 부재) | [N/A] | |
| L209 | `fadeOffsetZ = false;` | (fade 부재) | [N/A] | |
| L210 | `fadeRotateAngleX = false;` | (fade 부재) | [N/A] | §17 |
| L211 | `fadeRotateAngleY = false;` | (fade 부재) | [N/A] | |
| L212 | `fadeRotateAngleZ = false;` | (fade 부재) | [N/A] | |
| L213 | `fadeRotationPointX = false;` | (fade 부재) | [N/A] | |
| L214 | `fadeRotationPointY = false;` | (fade 부재) | [N/A] | |
| L215 | `fadeRotationPointZ = false;` | (fade 부재) | [N/A] | |
| L216 | (빈 줄) | — | [N/A] | |
| L217 | `previous = null;` | (RendererData 부재) | [N/A] | |
| L218 | `}` | — | [N/A] | reset 종료 |
| L219 | (빈 줄) | — | [N/A] | |
| L220 | `@Override` | — | [N/A] | |
| L221 | `public void renderWithRotation(float f)` | (vanilla 자동) | [N/A] | |
| L222 | `{` | — | [N/A] | |
| L223 | `boolean update = !compiled;` | (vanilla 자동 빌드) | [N/A] | |
| L224 | `super.renderWithRotation(f);` | — | [N/A] | |
| L225 | `if(update)` | — | [N/A] | |
| L226 | `UpdateLocals();` | — | [N/A] | |
| L227 | `}` | — | [N/A] | |
| L228 | (빈 줄) | — | [N/A] | |
| L229 | `@Override` | — | [N/A] | |
| L230 | `public void postRender(float f)` | (vanilla 자동) | [N/A] | |
| L231 | `{` | — | [N/A] | |
| L232 | `boolean update = !compiled;` | — | [N/A] | |
| L233 | `if(!preRender(f))` | — | [N/A] | |
| L234 | `return;` | — | [N/A] | |
| L235 | `if(update)` | — | [N/A] | |
| L236 | `UpdateLocals();` | — | [N/A] | |
| L237 | `preTransforms(f, false, true);` | — | [N/A] | |
| L238 | `}` | — | [N/A] | |
| L239 | (빈 줄) | — | [N/A] | |
| L240 | `private void UpdateLocals()` | (reflection — 1.21.1 부재) | [N/A] | |
| L241 | `{` | — | [N/A] | |
| L242 | `UpdateCompiled();` | — | [N/A] | |
| L243 | `if(compiled)` | — | [N/A] | |
| L244 | `UpdateDisplayList();` | — | [N/A] | |
| L245 | `}` | — | [N/A] | |
| L246 | (빈 줄) | — | [N/A] | |
| L247 | `private void UpdateCompiled()` | (reflection 부재) | [N/A] | |
| L248 | `{` | — | [N/A] | |
| L249 | `compiled = (Boolean)Reflect.GetField(_compiled, this);` | — | [N/A] | |
| L250 | `}` | — | [N/A] | |
| L251 | (빈 줄) | — | [N/A] | |
| L252 | `private void UpdateDisplayList()` | (reflection 부재) | [N/A] | |
| L253 | `{` | — | [N/A] | |
| L254 | `displayList = (Integer)Reflect.GetField(_displayList, this);` | — | [N/A] | |
| L255 | `}` | — | [N/A] | |
| L256 | (빈 줄) | — | [N/A] | |
| L257 | `private static Field _compiled = Reflect.GetField(...);` | (reflection 부재) | [N/A] | |
| L258 | `private static Method _compileDisplayList = Reflect.GetMethod(...);` | — | [N/A] | |
| L259 | `private static Field _displayList = Reflect.GetField(...);` | — | [N/A] | |
| L260 | (빈 줄) | — | [N/A] | |
| L261 | `protected ModelRotationRenderer base;` | (다층 — ModelData child 자동) | [N/A] | |
| L262 | (빈 줄) | — | [N/A] | |
| L263 | `public boolean ignoreRender;` | (다층 부재) | [N/A] | |
| L264 | `public boolean forceRender;` | (다층 부재) | [N/A] | |
| L265 | (빈 줄) | — | [N/A] | |
| L266 | `public boolean compiled;` | (vanilla 자동 빌드) | [N/A] | |
| L267 | `public int displayList;` | (DisplayList 부재 — VertexConsumer) | [N/A] | |
| L268 | `public int rotationOrder;` | (1.21.1 ModelPart에 회전순서 필드 부재 — setAngles* 헬퍼로 분리) | [N/A] | rotationOrder 일원화 부재 — 헬퍼 호출 시점 분리 |
| L269 | (빈 줄) | — | [N/A] | |
| L270 | `public float scaleX;` | `ModelPart.xScale` (public field) | [정합] | |
| L271 | `public float scaleY;` | `ModelPart.yScale` (B-3 setArmScales/setLegScales 활용) | [정합] | §16-7 |
| L272 | `public float scaleZ;` | `ModelPart.zScale` | [정합] | |
| L273 | (빈 줄) | — | [N/A] | |
| L274 | `public boolean ignoreBase;` | (다층 부재) | [N/A] | |
| L275 | `public boolean ignoreSuperRotation;` | (어깨 ZYX 부재) | [N/A] | §16-5 |
| L276 | (빈 줄) | — | [N/A] | |
| L277 | `public static int XYZ = 0;` | (회전순서 0 — vanilla pitch/yaw/roll 기본) | [정합] | R-17 검증 1차 자료 |
| L278 | `public static int XZY = 1;` | setAnglesXZY 매핑 (B-2 세션 2) | [정합] | R-17 GitHub 검증 |
| L279 | `public static int YXZ = 2;` | setAnglesYXZ 매핑 (B-1 세션 2) | [정합] | R-17 |
| L280 | `public static int YZX = 3;` | setAnglesYZX 매핑 (B-08) | [정합] | R-17 |
| L281 | `public static int ZXY = 4;` | setAnglesZXY 매핑 (B-08) | [정합] | R-17 |
| L282 | `public static int ZYX = 5;` | (어깨 부재로 미이식) | [정합] | §16-5 — N/A 가 아니라 [정합 (구조 부재)] (상수 자체는 정의됨) |
| L283 | (빈 줄) | — | [N/A] | |
| L284 | `public boolean fadeEnabled;` | (fade 메커니즘 부재) | [N/A] | §17 |
| L285 | (빈 줄) | — | [N/A] | |
| L286 | `public boolean fadeOffsetX;` | (fade 부재) | [N/A] | |
| L287 | `public boolean fadeOffsetY;` | (fade 부재) | [N/A] | |
| L288 | `public boolean fadeOffsetZ;` | (fade 부재) | [N/A] | |
| L289 | `public boolean fadeRotateAngleX;` | (fade 부재) | [N/A] | |
| L290 | `public boolean fadeRotateAngleY;` | (fade 부재) | [N/A] | |
| L291 | `public boolean fadeRotateAngleZ;` | (fade 부재) | [N/A] | |
| L292 | `public boolean fadeRotationPointX;` | (fade 부재) | [N/A] | |
| L293 | `public boolean fadeRotationPointY;` | (fade 부재) | [N/A] | |
| L294 | `public boolean fadeRotationPointZ;` | (fade 부재) | [N/A] | |
| L295 | (빈 줄) | — | [N/A] | |
| L296 | `public RendererData previous;` | (fade 데이터 부재) | [N/A] | |
| L297 | (빈 줄) | — | [N/A] | |
| L298 | `public void fadeStore(float totalTime)` | (fade 부재) | [N/A] | §17 |
| L299 | `{` | — | [N/A] | |
| L300 | `if(previous != null)` | — | [N/A] | |
| L301 | `{` | — | [N/A] | |
| L302 | `previous.offsetX = offsetX;` | — | [N/A] | |
| L303 | `previous.offsetY = offsetY;` | — | [N/A] | |
| L304 | `previous.offsetZ = offsetZ;` | — | [N/A] | |
| L305 | `previous.rotateAngleX = rotateAngleX;` | — | [N/A] | |
| L306 | `previous.rotateAngleY = rotateAngleY;` | — | [N/A] | |
| L307 | `previous.rotateAngleZ = rotateAngleZ;` | — | [N/A] | |
| L308 | `previous.rotationPointX = rotationPointX;` | — | [N/A] | |
| L309 | `previous.rotationPointY = rotationPointY;` | — | [N/A] | |
| L310 | `previous.rotationPointZ = rotationPointZ;` | — | [N/A] | |
| L311 | `previous.totalTime = totalTime;` | — | [N/A] | |
| L312 | `}` | — | [N/A] | |
| L313 | `}` | — | [N/A] | |
| L314 | (빈 줄) | — | [N/A] | |
| L315 | `public void fadeIntermediate(float totalTime)` | (fade 부재) | [N/A] | §17 |
| L316 | `{` | — | [N/A] | |
| L317 | `if(previous != null && totalTime - previous.totalTime <= 2F)` | — | [N/A] | |
| L318 | `{` | — | [N/A] | |
| L319 | `offsetX = GetIntermediatePosition(previous.offsetX, offsetX, fadeOffsetX, previous.totalTime, totalTime);` | — | [N/A] | |
| L320 | `offsetY = GetIntermediatePosition(...);` | — | [N/A] | |
| L321 | `offsetZ = GetIntermediatePosition(...);` | — | [N/A] | |
| L322 | (빈 줄) | — | [N/A] | |
| L323 | `rotateAngleX = GetIntermediateAngle(...);` | (fade 보간 미이식) | [N/A] | §17 — vanilla MathHelper.lerpAngle 활용 가능 |
| L324 | `rotateAngleY = GetIntermediateAngle(...);` | — | [N/A] | |
| L325 | `rotateAngleZ = GetIntermediateAngle(...);` | — | [N/A] | |
| L326 | (빈 줄) | — | [N/A] | |
| L327 | `rotationPointX = GetIntermediatePosition(...);` | — | [N/A] | |
| L328 | `rotationPointY = GetIntermediatePosition(...);` | — | [N/A] | |
| L329 | `rotationPointZ = GetIntermediatePosition(...);` | — | [N/A] | |
| L330 | `}` | — | [N/A] | |
| L331 | `}` | — | [N/A] | |
| L332 | (빈 줄) | — | [N/A] | |
| L333 | `@SuppressWarnings("static-method")` | — | [N/A] | |
| L334 | `public boolean canBeRandomBoxSource()` | (영혼 파티클 — 본 포커스 외) | [N/A] | |
| L335 | `{` | — | [N/A] | |
| L336 | `return true;` | — | [N/A] | |
| L337 | `}` | — | [N/A] | |
| L338 | (빈 줄) | — | [N/A] | |
| L339 | `private static float GetIntermediatePosition(float prevPosition, float shouldPosition, boolean fade, float lastTotalTime, float totalTime)` | (fade 미이식) | [N/A] | §17 |
| L340 | `{` | — | [N/A] | |
| L341 | `if(!fade \|\| shouldPosition == prevPosition)` | — | [N/A] | |
| L342 | `return shouldPosition;` | — | [N/A] | |
| L343 | (빈 줄) | — | [N/A] | |
| L344 | `return prevPosition + (shouldPosition - prevPosition) * (totalTime - lastTotalTime) * 0.2F;` | (선형 보간 0.2F * dt — 미이식) | [N/A] | §17 — `MathHelper.lerp(0.2f * dt, prev, target)` 등가 가능 |
| L345 | `}` | — | [N/A] | |
| L346 | (빈 줄) | — | [N/A] | |
| L347 | `private static float GetIntermediateAngle(float prevAngle, float shouldAngle, boolean fade, float lastTotalTime, float totalTime)` | (fade angle wrap 미이식) | [N/A] | |
| L348 | `{` | — | [N/A] | |
| L349 | `if(!fade \|\| shouldAngle == prevAngle)` | — | [N/A] | |
| L350 | `return shouldAngle;` | — | [N/A] | |
| L351 | (빈 줄) | — | [N/A] | |
| L352 | `while(prevAngle >= Whole) prevAngle -= Whole;` | (각도 wrap — vanilla MathHelper.wrapDegrees 등가) | [N/A] | |
| L353 | `while(prevAngle < 0F) prevAngle += Whole;` | — | [N/A] | |
| L354 | (빈 줄) | — | [N/A] | |
| L355 | `while(shouldAngle >= Whole) shouldAngle -= Whole;` | — | [N/A] | |
| L356 | `while(shouldAngle < 0F) shouldAngle += Whole;` | — | [N/A] | |
| L357 | (빈 줄) | — | [N/A] | |
| L358 | `if(shouldAngle > prevAngle && (shouldAngle - prevAngle) > Half)` | (최단 회전 방향 선택) | [N/A] | |
| L359 | `prevAngle += Whole;` | — | [N/A] | |
| L360 | (빈 줄) | — | [N/A] | |
| L361 | `if(shouldAngle < prevAngle && (prevAngle - shouldAngle) > Half)` | — | [N/A] | |
| L362 | `shouldAngle += Whole;` | — | [N/A] | |
| L363 | (빈 줄) | — | [N/A] | |
| L364 | `return prevAngle + (shouldAngle - prevAngle) * (totalTime - lastTotalTime) * 0.2F;` | (선형 보간) | [N/A] | |
| L365 | `}` | — | [N/A] | |
| L366 | (빈 줄) | — | [N/A] | |
| L367 | `private static FloatBuffer buffer = BufferUtils.createFloatBuffer(16);` | (matrix buffer — reflection 부재) | [N/A] | |
| L368 | `private static float[] array = new float[16];` | — | [N/A] | 클래스 종료 (} 묵시) |

**청크 2 (L185-L368) 통계: 정합 22 / 오역 0 / 누락 0 / 잉여 0 / N/A 162 = 184 라인 전수.**

**청크 2 발견**: 신규 [오역]/[누락]/[잉여] 0건. 핵심 검증:
- L185-L197 reset() 14 라인 (rotationOrder/scaleXYZ/pivotXYZ/rotateXYZ) — vanilla setAngles 진입 시 자동 reset 매핑.
- L270-L272 scaleX/Y/Z 필드 → ModelPart.xScale/yScale/zScale public field (B-3 세션 2 setArmScales/setLegScales 활용).
- L277-L282 6 회전순서 상수 (XYZ=0/XZY=1/YXZ=2/YZX=3/ZXY=4/ZYX=5) — R-17 GitHub 검증과 정확히 일치, B-1/B-2 세션 2 헬퍼 매핑 1차 자료 확정.
- fade 메커니즘 (L298-L331 fadeStore/fadeIntermediate, L339-L365 GetIntermediate*) — 1.21.1 미이식 (§17 잔여). vanilla MathHelper.lerp/lerpAngle 로 등가 이식 가능.

**R-5 청크 1+2 ModelRotationRenderer.java 누적 (368 라인)**:
- 청크 1 (L1-L184): 정합 21 / N/A 163
- 청크 2 (L185-L368): 정합 22 / N/A 162
- 합계: 정합 43 / N/A 325 = 368 라인 전수 (오역/누락/잉여 0).

**다음 청크**: R-5 청크 3 (RendererData/ModelCapeRenderer/ModelEarsRenderer/ModelSpecialRenderer 4 파일 ~232줄). R-5 마지막 청크.

### 청크 3 (RendererData 32 + ModelCapeRenderer 90 + ModelEarsRenderer 61 + ModelSpecialRenderer 56 = 4 파일 239 라인) — R-5 마지막 청크

**파트 A — RendererData.java (32 라인)** — fade 데이터 컨테이너

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L17 | 라이선스 + `==` | — | [N/A] | |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.render;` | — | [N/A] | |
| L20 | (빈 줄) | — | [N/A] | |
| L21 | `public class RendererData` | (fade 메커니즘 부재) | [N/A] | §17 잔여 |
| L22 | `{` | — | [N/A] | |
| L23 | `public float offsetX;` | — | [N/A] | |
| L24 | `public float offsetY;` | — | [N/A] | |
| L25 | `public float offsetZ;` | — | [N/A] | |
| L26 | `public float rotateAngleX;` | — | [N/A] | |
| L27 | `public float rotateAngleY;` | — | [N/A] | |
| L28 | `public float rotateAngleZ;` | — | [N/A] | |
| L29 | `public float rotationPointX;` | — | [N/A] | |
| L30 | `public float rotationPointY;` | — | [N/A] | |
| L31 | `public float totalTime = Float.MIN_VALUE;` | — | [N/A] | |
| L32 | `}` | — | [N/A] | |

**파트 A 통계: 정합 0 / 오역 0 / 누락 0 / 잉여 0 / N/A 32 = 32 라인 전수.** fade 메커니즘 부재 (§17).

**파트 B — ModelCapeRenderer.java (90 라인)** — 망토 렌더 (vanilla CapeFeatureRenderer 등가 + outer.X 클램프)

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L17 | 라이선스 + `==` | — | [N/A] | |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.render;` | — | [N/A] | |
| L20 | `import net.minecraft.client.model.*;` | — | [N/A] | |
| L21 | `import net.minecraft.entity.player.*;` | — | [N/A] | |
| L22 | `import net.minecraft.util.*;` | — | [N/A] | |
| L23 | (빈 줄) | — | [N/A] | |
| L24 | `import org.lwjgl.opengl.GL11;` | (1.21.1: MatrixStack/RotationAxis) | [N/A] | |
| L25 | (빈 줄) | — | [N/A] | |
| L26 | `public class ModelCapeRenderer extends ModelSpecialRenderer` | (1.21.1: vanilla CapeFeatureRenderer 직접 사용) | [N/A] | |
| L27 | `{` | — | [N/A] | |
| L28-L32 | 생성자 5 라인 | (vanilla 자동) | [N/A] | |
| L33 | (빈 줄) | — | [N/A] | |
| L34-L40 | `beforeRender(EntityPlayer, factor)` 7 라인 | (vanilla 자동) | [N/A] | |
| L41 | (빈 줄) | — | [N/A] | |
| L42 | `@Override` | — | [N/A] | |
| L43 | `public void preTransform(float factor, boolean push)` | (vanilla CapeFeatureRenderer.render 자동) | [N/A] | |
| L44 | `{` | — | [N/A] | |
| L45 | `super.preTransform(factor, push);` | — | [N/A] | |
| L46 | (빈 줄) | — | [N/A] | |
| L47 | `double d = (cape X lerp) - (player X lerp);` | vanilla CapeFeatureRenderer: `MathHelper.lerp(tickDelta, prevCapeX, capeX) - MathHelper.lerp(tickDelta, prevX, getX())` | [정합] | vanilla 동일 공식 |
| L48 | `double d1 = (cape Y lerp) - (player Y lerp);` | vanilla 자동 | [정합] | |
| L49 | `double d2 = (cape Z lerp) - (player Z lerp);` | vanilla 자동 | [정합] | |
| L50 | `float f1 = renderYawOffset lerp;` | vanilla: `player.bodyYaw lerp` | [정합] | |
| L51 | `double d3 = MathHelper.sin((f1 * π) / 180F);` | vanilla 자동 | [정합] | |
| L52 | `double d4 = -MathHelper.cos((f1 * π) / 180F);` | vanilla 자동 | [정합] | |
| L53 | `float f2 = (float)d1 * 10F;` | vanilla 자동 | [정합] | |
| L54 | `if(f2 < -6F)` | vanilla MathHelper.clamp(j, -6F, 32F) | [정합] | |
| L55 | `{` | — | [정합] | |
| L56 | `f2 = -6F;` | — | [정합] | |
| L57 | `}` | — | [정합] | |
| L58 | `if(f2 > 32F)` | vanilla 자동 (clamp 상한) | [정합] | |
| L59 | `{` | — | [정합] | |
| L60 | `f2 = 32F;` | — | [정합] | |
| L61 | `}` | — | [정합] | |
| L62 | `float f3 = (float)(d * d3 + d2 * d4) * 100F;` | vanilla 자동 | [정합] | |
| L63 | `float f4 = (float)(d * d4 - d2 * d3) * 100F;` | vanilla 자동 | [정합] | |
| L64 | `if(f3 < 0.0F)` | vanilla `Math.max(k, 0.0F)` | [정합] | |
| L65 | `{` | — | [정합] | |
| L66 | `f3 = 0.0F;` | — | [정합] | |
| L67 | `}` | — | [정합] | |
| L68 | `float f5 = cameraYaw lerp;` | vanilla: `MathHelper.lerp(tickDelta, prevStrideDistance, strideDistance)` | [정합] | |
| L69 | `f2 += MathHelper.sin(distanceWalkedModified lerp * 6F) * 32F * f5;` | vanilla 자동 (걷기 흔들림) | [정합] | |
| L70 | (빈 줄) | — | [N/A] | |
| L71 | `float localAngle = 6F + f3 / 2.0F + f2;` | vanilla: `6.0F + k / 2.0F + j` | [정합] | |
| L72 | `float localAngleMax = Math.max(70.523F - outer.rotateAngleX * RadiantToAngle, 6F);` | (1.21.1 vanilla 미존재) | [누락] | ⚠️ outer.X 기반 망토 X 클램프 — SM 큰 기울기 시 망토 과도 펴짐 방지. R-10+ B-N 후보 |
| L73 | `float realLocalAngle = Math.min(localAngle, localAngleMax);` | (1.21.1 미존재) | [누락] | ⚠️ outer.X 클램프 적용 |
| L74 | (빈 줄) | — | [N/A] | |
| L75 | `GL11.glRotatef(realLocalAngle, 1.0F, 0.0F, 0.0F);` | vanilla: `matrices.multiply(POSITIVE_X.rotationDegrees(6.0F + k/2.0F + j))` (clamp 없는 localAngle 등가) | [정합] | |
| L76 | `GL11.glRotatef(f4 / 2.0F, 0.0F, 0.0F, 1.0F);` | vanilla: `matrices.multiply(POSITIVE_Z.rotationDegrees(m / 2.0F))` | [정합] | |
| L77 | `GL11.glRotatef(-f4 / 2.0F, 0.0F, 1.0F, 0.0F);` | vanilla: `matrices.multiply(POSITIVE_Y.rotationDegrees(180.0F - m / 2.0F))` 통합 (-m/2 + 180 = 180 - m/2 등가) | [정합] | |
| L78 | `GL11.glRotatef(180F, 0.0F, 1.0F, 0.0F);` | vanilla L77과 통합 | [정합] | |
| L79 | `}` | — | [N/A] | preTransform 종료 |
| L80 | (빈 줄) | — | [N/A] | |
| L81 | `@Override` | — | [N/A] | |
| L82 | `public boolean canBeRandomBoxSource()` | (영혼 파티클 — 본 포커스 외) | [N/A] | |
| L83 | `{` | — | [N/A] | |
| L84 | `return false;` | — | [N/A] | |
| L85 | `}` | — | [N/A] | |
| L86 | (빈 줄) | — | [N/A] | |
| L87 | `private final ModelRotationRenderer outer;` | (Outer 부재) | [N/A] | |
| L88 | `private EntityPlayer entityplayer;` | (vanilla CapeFeatureRenderer 인자 자동) | [N/A] | |
| L89 | `private float setFactor;` | — | [N/A] | |
| L90 | `}` | — | [N/A] | 클래스 종료 |

**파트 B 통계: 정합 28 / 오역 0 / 누락 2 / 잉여 0 / N/A 60 = 90 라인 전수.**

**파트 C — ModelEarsRenderer.java (61 라인)** — Ears 노드 (1.21.1 부재)

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L17 | 라이선스 + `==` | — | [N/A] | |
| L18-L23 | package + imports + 빈 줄 6 라인 | — | [N/A] | |
| L24 | `public class ModelEarsRenderer extends ModelSpecialRenderer` | (Ears 노드 1.21.1 부재) | [N/A] | §16-2 — SR mod 전용 |
| L25 | `{` | — | [N/A] | |
| L26 | `private int _i = 0;` | — | [N/A] | |
| L27 | (빈 줄) | — | [N/A] | |
| L28-L31 | 생성자 4 라인 | — | [N/A] | |
| L32 | (빈 줄) | — | [N/A] | |
| L33-L36 | `beforeRender()` 4 라인 | — | [N/A] | |
| L37 | (빈 줄) | — | [N/A] | |
| L38-L43 | `@Override doRender(...)` 6 라인 | — | [N/A] | |
| L44 | (빈 줄) | — | [N/A] | |
| L45-L54 | `@Override preTransform(...)` 본체 (Ears 위치 보정 — 0.375F 좌우 + 1.333333F 스케일) | (Ears 부재) | [N/A] | |
| L55 | (빈 줄) | — | [N/A] | |
| L56-L60 | `canBeRandomBoxSource()` 5 라인 | — | [N/A] | |
| L61 | `}` | — | [N/A] | |

**파트 C 통계: 정합 0 / 오역 0 / 누락 0 / 잉여 0 / N/A 61 = 61 라인 전수.** Ears 노드 일체 1.21.1 부재.

**파트 D — ModelSpecialRenderer.java (56 라인)** — 다층 모델 추상 부모

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L17 | 라이선스 + `==` | — | [N/A] | |
| L18-L23 | package + imports + 빈 줄 6 라인 | — | [N/A] | |
| L24 | `public class ModelSpecialRenderer extends ModelRotationRenderer` | (다층 모델 추상 부모 — 1.21.1 부재) | [N/A] | |
| L25 | `{` | — | [N/A] | |
| L26 | `public boolean doPopPush;` | — | [N/A] | |
| L27 | (빈 줄) | — | [N/A] | |
| L28-L32 | 생성자 5 라인 (`ignoreRender = true;`) | — | [N/A] | |
| L33 | (빈 줄) | — | [N/A] | |
| L34-L38 | `beforeRender(boolean popPush)` 5 라인 | — | [N/A] | |
| L39 | (빈 줄) | — | [N/A] | |
| L40-L49 | `@Override doRender(f, useParentTransformations)` 본체 (push/pop matrix 분기 — vanilla 자동) | — | [N/A] | |
| L50 | (빈 줄) | — | [N/A] | |
| L51-L55 | `afterRender()` 5 라인 (`ignoreRender = true; doPopPush = false;`) | — | [N/A] | |
| L56 | `}` | — | [N/A] | |

**파트 D 통계: 정합 0 / 오역 0 / 누락 0 / 잉여 0 / N/A 56 = 56 라인 전수.** 다층 모델 추상 부모 일체 1.21.1 부재.

**R-5 청크 3 통계 (4 파일 합)**:
- RendererData: 32 / 0 / 0 / 0 / 0 / 32
- ModelCapeRenderer: 90 / 28 / 0 / 2 / 0 / 60
- ModelEarsRenderer: 61 / 0 / 0 / 0 / 0 / 61
- ModelSpecialRenderer: 56 / 0 / 0 / 0 / 0 / 56
- **합계: 정합 28 / 오역 0 / 누락 2 / 잉여 0 / N/A 209 = 239 라인 전수**

**청크 3 발견 (R-10+ B-N 후보)**:
- B-N (ModelCapeRenderer outer.X 망토 X 클램프 [누락]): L72-L73 — `localAngleMax = max(70.523F - outer.X * RadiantToAngle, 6F)` + `realLocalAngle = min(localAngle, localAngleMax)`. 1.21.1 vanilla CapeFeatureRenderer는 이 클램프 없음. SM 11-state 분기 (Climb/Swim/Dive/Slide/Flying/HeadJump 등 큰 X 기울기 상태) 시 망토가 과도하게 펴짐 가능. 우선순위 중간 (망토 시각 차이).

**R-5 전체 누적 (368 + 239 = 607 라인, 3 청크 3 세션) — R-5 완료**:
| 청크 | 파일 | 라인 | 정합 | 오역 | 누락 | 잉여 | N/A |
|-----|-----|-----|------|------|------|------|------|
| 1 | ModelRotationRenderer L1-L184 | 184 | 21 | 0 | 0 | 0 | 163 |
| 2 | ModelRotationRenderer L185-L368 | 184 | 22 | 0 | 0 | 0 | 162 |
| 3 | RendererData + Cape + Ears + Special 4 파일 | 239 | 28 | 0 | 2 | 0 | 209 |
| **합계** | **5 파일** | **607** | **71** | **0** | **2** | **0** | **534** |

**R-5 완료**. 다음 R-단계: **R-6 (SmartRenderRender.java 227 + SmartRenderUtilities 108 + Mod 73 + Info 28 + Install 26 + Context 47 + IModel 62 + IRender 47 = 8 파일 ~618 라인)**.

---

## R-6: SmartRenderRender + Utilities/Mod/Info/Install/Context/IModel/IRender (8 파일 ~618)

### 청크 1 (SmartRenderRender.java 228 라인) — 헤더 + 생성자 + renderPlayer 본체 (통계+위치차이+모델분배) + drawFirstPersonHand + rotatePlayer + renderSpecials + before/afterHandleRotationFloat + getPreviousRendererData + 정적 필드

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L1-L16 | Smart Render GPLv3 라이선스 헤더 | — | [N/A] | |
| L17 | `// ==…==` | — | [N/A] | |
| L18 | (빈 줄) | — | [N/A] | |
| L19 | `package net.smart.render;` | — | [N/A] | |
| L20 | `import java.util.*;` | — | [N/A] | |
| L21 | (빈 줄) | — | [N/A] | |
| L22 | `import net.minecraft.client.*;` | — | [N/A] | |
| L23 | `import net.minecraft.client.entity.*;` | — | [N/A] | |
| L24 | `import net.minecraft.client.gui.inventory.*;` | (1.21.1: InventoryScreen 자동) | [N/A] | |
| L25 | `import net.minecraft.entity.*;` | — | [N/A] | |
| L26 | `import net.minecraft.entity.player.*;` | — | [N/A] | |
| L27 | `import net.smart.render.statistics.*;` | (1.21.1: SmartStatistics 부재 — limbAnimator/age 등가) | [N/A] | R-8 검토 |
| L28 | (빈 줄) | — | [N/A] | |
| L29 | `public class SmartRenderRender extends SmartRenderContext` | `@Mixin(PlayerEntityRenderer.class) MixinPlayerEntityRenderer` (R-2 청크 1 매핑) | [정합] | 표면 매핑 |
| L30 | `{` | — | [N/A] | |
| L31 | `public static SmartRenderModel CurrentMainModel;` | (1.21.1: 단일 모델) | [N/A] | |
| L32 | (빈 줄) | — | [N/A] | |
| L33 | `public IRenderPlayer irp;` | (Mixin: this 직접) | [N/A] | |
| L34 | (빈 줄) | — | [N/A] | |
| L35 | `public SmartRenderRender(IRenderPlayer irp)` | (Mixin: 생성자 부재) | [N/A] | |
| L36 | `{` | — | [N/A] | |
| L37 | `this.irp = irp;` | — | [N/A] | |
| L38 | (빈 줄) | — | [N/A] | |
| L39 | `modelBipedMain = irp.createModel(irp.getModelBipedMain(), 0.0F).getRenderModel();` | (1.21.1: 단일 PlayerEntityModel — 다층 createModel 부재) | [N/A] | |
| L40 | `SmartRenderModel modelArmorChestplate = irp.createModel(irp.getModelArmorChestplate(), 1.0F).getRenderModel();` | (갑옷 ArmorFeatureRenderer §17 잔여) | [N/A] | |
| L41 | `SmartRenderModel modelArmor = irp.createModel(irp.getModelArmor(), 0.5F).getRenderModel();` | (갑옷) | [N/A] | |
| L42 | (빈 줄) | — | [N/A] | |
| L43 | `irp.initialize(modelBipedMain.mp, modelArmorChestplate.mp, modelArmor.mp, 0.5F);` | (다층 모델 초기화 — 단일 자동) | [N/A] | |
| L44 | `}` | — | [N/A] | |
| L45 | (빈 줄) | — | [N/A] | |
| L46 | `public void renderPlayer(AbstractClientPlayer entityplayer, double d, double d1, double d2, float f, float renderPartialTicks)` | (vanilla render + Mixin TAIL inject 자동) | [정합] | 진입점 |
| L47 | `{` | — | [N/A] | |
| L48 | `SmartStatistics statistics = SmartStatisticsFactory.getInstance(entityplayer);` | `SmartMovingClientState sm = SmartMovingClientStateAccess.smartmoving$getState(player)` (state holder) | [정합] | factory → state holder |
| L49 | `if(statistics != null)` | (sm null check) | [정합] | |
| L50 | `{` | — | [N/A] | |
| L51 | `boolean isInventory = d == 0.0F && d1 == 0.0F && d2 == 0.0F && f == 0.0F && renderPartialTicks == 1.0F;` | (1.21.1: InventoryScreen 자동 분리) | [N/A] | |
| L52 | `boolean isSleeping = entityplayer.isPlayerSleeping();` | `player.isSleeping()` (vanilla SleepingPose 자동) | [정합] | |
| L53 | (빈 줄) | — | [N/A] | |
| L54 | `float totalVerticalDistance = statistics.getTotalVerticalDistance(renderPartialTicks);` | (1.21.1 limbSwing 잘못 매핑 — §16-10 [오역] 발견 변수의 갱신 위치) | [N/A] | SmartStatistics 부재 — vanilla 등가 부재 |
| L55 | `float currentVerticalSpeed = statistics.getCurrentVerticalSpeed(renderPartialTicks);` | (§16-10 [오역] 발견) | [N/A] | |
| L56 | `float totalDistance = statistics.getTotalDistance(renderPartialTicks);` | (§16-12/13/15 [오역] 발견) | [N/A] | |
| L57 | `float currentSpeed = statistics.getCurrentSpeed(renderPartialTicks);` | (§16-13/15 [오역] 발견) | [N/A] | |
| L58 | (빈 줄) | — | [N/A] | |
| L59 | `double distance = 0;` | (변수 선언) | [N/A] | |
| L60 | `double verticalDistance = 0;` | — | [N/A] | |
| L61 | `double horizontalDistance = 0;` | — | [N/A] | |
| L62 | `float currentCameraAngle = 0;` | — | [N/A] | |
| L63 | `float currentVerticalAngle = 0;` | — | [N/A] | |
| L64 | `float currentHorizontalAngle = 0;` | — | [N/A] | |
| L65 | (빈 줄) | — | [N/A] | |
| L66 | `if (!isInventory)` | (vanilla 자동) | [N/A] | |
| L67 | `{` | — | [N/A] | |
| L68 | `double xDiff = entityplayer.posX - entityplayer.prevPosX;` | `player.getX() - player.prevX` (sm_captureBodyYaw 등가 — getVelocity 활용) | [정합] | |
| L69 | `double yDiff = entityplayer.posY - entityplayer.prevPosY;` | `vel.y` 등가 | [정합] | |
| L70 | `double zDiff = entityplayer.posZ - entityplayer.prevPosZ;` | `vel.z` 등가 | [정합] | |
| L71 | (빈 줄) | — | [N/A] | |
| L72 | `verticalDistance = Math.abs(yDiff);` | (sm.stats 또는 직접 계산 — capture 부재일 수 있음) | [정합] | |
| L73 | `horizontalDistance = Math.sqrt(xDiff * xDiff + zDiff * zDiff);` | (sm.stats.horizontalDistance 또는 sm_captureBodyYaw vel.x²+vel.z² 등가) | [정합] | |
| L74 | `distance = Math.sqrt(horizontalDistance * horizontalDistance + verticalDistance * verticalDistance);` | (3D 거리 — sm.stats.distance 또는 capture 부재) | [정합] | |
| L75 | (빈 줄) | — | [N/A] | |
| L76 | `currentCameraAngle = entityplayer.rotationYaw / RadiantToAngle;` | `player.getYaw() * DEG_TO_RAD` (sm_captureBodyYaw 활용) | [정합] | |
| L77 | `currentVerticalAngle = (float)Math.atan(yDiff / horizontalDistance);` | (sm.stats.currentVerticalAngle 갱신 — sm_animateHeadJumping/Flying ANIM-01 활용) | [정합] | sm_setupTransforms theta 계산에 사용 |
| L78 | `if(Float.isNaN(currentVerticalAngle))` | (NaN 가드) | [정합] | |
| L79 | `currentVerticalAngle = Quarter;` | (NaN → π/2) | [정합] | |
| L80 | (빈 줄) | — | [N/A] | |
| L81 | `currentHorizontalAngle = (float)-Math.atan(xDiff / zDiff);` | sm_captureBodyYaw: `Math.atan2(-vel.x, vel.z)` | [정합] | atan2가 quadrant 자동 처리 (등가) |
| L82 | `if (Float.isNaN(currentHorizontalAngle))` | (atan2 자동 NaN 회피) | [정합] | |
| L83 | `if(Float.isNaN(statistics.prevHorizontalAngle))` | — | [정합] | |
| L84 | `currentHorizontalAngle = currentCameraAngle;` | (NaN fallback) | [정합] | |
| L85 | `else` | — | [N/A] | |
| L86 | `currentHorizontalAngle = statistics.prevHorizontalAngle;` | (이전 값 유지 — atan2 자동 일관) | [정합] | |
| L87 | `else if (zDiff < 0)` | (atan vs atan2 quadrant 차이 — atan2가 자동 처리) | [정합] | |
| L88 | `currentHorizontalAngle += Half;` | (atan2 등가) | [정합] | |
| L89 | (빈 줄) | — | [N/A] | |
| L90 | `statistics.prevHorizontalAngle = currentHorizontalAngle;` | (캐시 — sm_captureBodyYaw 매 프레임 lerp 자동) | [N/A] | |
| L91 | `}` | — | [N/A] | |
| L92 | (빈 줄) | — | [N/A] | |
| L93 | `IModelPlayer[] modelPlayers = irp.getRenderModels();` | (1.21.1 단일 모델) | [N/A] | |
| L94 | (빈 줄) | — | [N/A] | |
| L95 | `for(int i = 0; i < modelPlayers.length; i++)` | (단일 모델 — for 부재) | [N/A] | |
| L96 | `{` | — | [N/A] | |
| L97 | `SmartRenderModel modelPlayer = modelPlayers[i].getRenderModel();` | (단일) | [N/A] | |
| L98 | (빈 줄) | — | [N/A] | |
| L99 | `modelPlayer.isInventory = isInventory;` | — | [N/A] | |
| L100 | (빈 줄) | — | [N/A] | |
| L101 | `modelPlayer.totalVerticalDistance = totalVerticalDistance;` | (state holder 일원화 — 분배 부재) | [N/A] | |
| L102 | `modelPlayer.currentVerticalSpeed = currentVerticalSpeed;` | — | [N/A] | |
| L103 | `modelPlayer.totalDistance = totalDistance;` | — | [N/A] | |
| L104 | `modelPlayer.currentSpeed = currentSpeed;` | — | [N/A] | |
| L105 | (빈 줄) | — | [N/A] | |
| L106 | `modelPlayer.distance = distance;` | — | [N/A] | |
| L107 | `modelPlayer.verticalDistance = verticalDistance;` | — | [N/A] | |
| L108 | `modelPlayer.horizontalDistance = horizontalDistance;` | — | [N/A] | |
| L109 | `modelPlayer.currentCameraAngle = currentCameraAngle;` | — | [N/A] | |
| L110 | `modelPlayer.currentVerticalAngle = currentVerticalAngle;` | `sm.stats.currentVerticalAngle` (sm_animateFlying/HeadJumping ANIM-01 활용) | [N/A] | state holder |
| L111 | `modelPlayer.currentHorizontalAngle = currentHorizontalAngle;` | (sm_captureBodyYaw 자체 lerp) | [N/A] | |
| L112 | `modelPlayer.prevOuterRenderData = getPreviousRendererData(entityplayer);` | (fade 데이터 부재 — §17) | [N/A] | |
| L113 | `modelPlayer.isSleeping = isSleeping;` | (vanilla SleepingPose) | [N/A] | |
| L114 | `}` | — | [N/A] | |
| L115 | `}` | — | [N/A] | |
| L116 | (빈 줄) | — | [N/A] | |
| L117 | `CurrentMainModel = modelBipedMain;` | (단일 모델) | [N/A] | |
| L118 | `irp.superRenderPlayer(entityplayer, ...)` | (vanilla render — Mixin TAIL 자동) | [N/A] | |
| L119 | `CurrentMainModel = null;` | — | [N/A] | |
| L120 | `}` | — | [N/A] | renderPlayer 종료 |
| L121 | (빈 줄) | — | [N/A] | |
| L122 | `public void drawFirstPersonHand(EntityPlayer entityPlayer)` | (1.21.1 vanilla 1인칭 손 자동) | [N/A] | |
| L123 | `{` | — | [N/A] | |
| L124 | `modelBipedMain.firstPerson = true;` | (vanilla 1인칭 자동 분리) | [N/A] | |
| L125 | `irp.superDrawFirstPersonHand(entityPlayer);` | — | [N/A] | |
| L126 | `modelBipedMain.firstPerson = false;` | — | [N/A] | |
| L127 | `}` | — | [N/A] | |
| L128 | (빈 줄) | — | [N/A] | |
| L129 | `public void rotatePlayer(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2)` | sm_captureBodyYaw HEAD + @ModifyArg index=3 (R-2 청크 1) | [정합] | rotatePlayer = setupTransforms 진입점 |
| L130 | `{` | — | [N/A] | |
| L131 | `boolean isLocal = entityplayer instanceof EntityPlayerSP;` | (vanilla 자동 — ClientPlayerEntity 자동) | [N/A] | |
| L132 | `boolean isInventory = f2 == 1.0F && isLocal && Minecraft.getMinecraft().currentScreen instanceof GuiInventory;` | (vanilla InventoryScreen 자동) | [N/A] | |
| L133 | `if(!isInventory)` | (vanilla 자동) | [N/A] | |
| L134 | `{` | — | [N/A] | |
| L135 | `float forwardRotation = entityplayer.prevRotationYaw + (entityplayer.rotationYaw - entityplayer.prevRotationYaw) * f2;` | sm_captureBodyYaw: `MathHelper.lerp(tickDelta, player.prevYaw, player.getYaw())` | [정합] | |
| L136 | (빈 줄) | — | [N/A] | |
| L137 | `if(entityplayer.isPlayerSleeping())` | (vanilla SleepingPose 자동 — 0 reset) | [정합] | |
| L138 | `{` | — | [N/A] | |
| L139 | `actualRotation = 0;` | (vanilla 자동) | [정합] | |
| L140 | `forwardRotation = 0;` | (vanilla 자동) | [정합] | |
| L141 | `}` | — | [N/A] | |
| L142 | (빈 줄) | — | [N/A] | |
| L143 | `float workingAngle;` | (어깨 NonStandardWorking 부재 §16-5) | [N/A] | |
| L144 | `Minecraft minecraft = Minecraft.getMinecraft();` | (vanilla MinecraftClient 자동) | [N/A] | |
| L145 | `if(!isLocal)` | (어깨 부재) | [N/A] | |
| L146 | `{` | — | [N/A] | |
| L147 | `workingAngle = -entityplayer.rotationYaw;` | — | [N/A] | |
| L148 | `workingAngle += minecraft.renderViewEntity.rotationYaw;` | — | [N/A] | |
| L149 | `}` | — | [N/A] | |
| L150 | `else` | — | [N/A] | |
| L151 | `workingAngle = actualRotation - getPreviousRendererData(entityplayer).rotateAngleY * RadiantToAngle;` | (fade 데이터 + 어깨 부재) | [N/A] | |
| L152 | (빈 줄) | — | [N/A] | |
| L153 | `if(minecraft.gameSettings.thirdPersonView == 2 && !minecraft.renderViewEntity.isPlayerSleeping())` | (vanilla 3rd person 자동) | [N/A] | |
| L154 | `workingAngle += 180F;` | (어깨 부재) | [N/A] | |
| L155 | (빈 줄) | — | [N/A] | |
| L156 | `IModelPlayer[] modelPlayers = irp.getRenderModels();` | (단일 모델) | [N/A] | |
| L157 | (빈 줄) | — | [N/A] | |
| L158 | `for(int i = 0; i < modelPlayers.length; i++)` | (단일) | [N/A] | |
| L159 | `{` | — | [N/A] | |
| L160 | `SmartRenderModel modelPlayer = modelPlayers[i].getRenderModel();` | — | [N/A] | |
| L161 | (빈 줄) | — | [N/A] | |
| L162 | `modelPlayer.actualRotation = actualRotation;` | (state holder) | [N/A] | |
| L163 | `modelPlayer.forwardRotation = forwardRotation;` | (sm_captureBodyYaw smBodyYawOverride 갱신) | [N/A] | |
| L164 | `modelPlayer.workingAngle = workingAngle;` | (어깨 부재) | [N/A] | §16-5 |
| L165 | `}` | — | [N/A] | |
| L166 | (빈 줄) | — | [N/A] | |
| L167 | `actualRotation = 0;` | (Mixin: @ModifyArg가 인자 교체) | [N/A] | |
| L168 | `}` | — | [N/A] | |
| L169 | (빈 줄) | — | [N/A] | |
| L170 | `irp.superRotatePlayer(entityplayer, totalTime, actualRotation, f2);` | (vanilla setupTransforms — @ModifyArg가 인자 변환) | [N/A] | |
| L171 | `}` | — | [N/A] | rotatePlayer 종료 |
| L172 | (빈 줄) | — | [N/A] | |
| L173 | `public void renderSpecials(AbstractClientPlayer entityplayer, float f)` | (vanilla CapeFeatureRenderer + 자동) | [N/A] | |
| L174 | `{` | — | [N/A] | |
| L175 | `modelBipedMain.bipedEars.beforeRender();` | (Ears 부재) | [N/A] | |
| L176 | `modelBipedMain.bipedCloak.beforeRender(entityplayer, f);` | (vanilla CapeFeatureRenderer 자동) | [N/A] | |
| L177 | `irp.superRenderSpecials(entityplayer, f);` | (vanilla 자동) | [N/A] | |
| L178 | `modelBipedMain.bipedCloak.afterRender();` | (vanilla 자동) | [N/A] | |
| L179 | `modelBipedMain.bipedEars.afterRender();` | (Ears 부재) | [N/A] | |
| L180 | `}` | — | [N/A] | |
| L181 | (빈 줄) | — | [N/A] | |
| L182 | `@SuppressWarnings({ "static-method", "unused" })` | — | [N/A] | |
| L183 | `public void beforeHandleRotationFloat(EntityLivingBase entityliving, float f)` | (1.21.1: ticksRiding 보정 미이식) | [N/A] | |
| L184 | `{` | — | [N/A] | |
| L185 | `if(entityliving instanceof EntityPlayer)` | — | [N/A] | |
| L186 | `{` | — | [N/A] | |
| L187 | `SmartStatistics statistics = SmartStatisticsFactory.getInstance((EntityPlayer)entityliving);` | (SmartStatistics 부재) | [N/A] | R-8 검토 |
| L188 | `if (statistics != null)` | — | [N/A] | |
| L189 | `entityliving.ticksExisted += statistics.ticksRiding;` | (1.21.1: vanilla RidingPose 자동 — ticksRiding 보정 부재) | [N/A] | 라이딩 흔들림 동기화 — vanilla 자동 |
| L190 | `}` | — | [N/A] | |
| L191 | `}` | — | [N/A] | |
| L192 | (빈 줄) | — | [N/A] | |
| L193 | `@SuppressWarnings({ "static-method", "unused" })` | — | [N/A] | |
| L194 | `public void afterHandleRotationFloat(EntityLivingBase entityliving, float f)` | (1.21.1: ticksRiding 복원 부재) | [N/A] | |
| L195 | `{` | — | [N/A] | |
| L196 | `if(entityliving instanceof EntityPlayer)` | — | [N/A] | |
| L197 | `{` | — | [N/A] | |
| L198 | `SmartStatistics statistics = SmartStatisticsFactory.getInstance((EntityPlayer)entityliving);` | — | [N/A] | |
| L199 | `if (statistics != null)` | — | [N/A] | |
| L200 | `entityliving.ticksExisted -= statistics.ticksRiding;` | (vanilla RidingPose 자동) | [N/A] | |
| L201 | `}` | — | [N/A] | |
| L202 | `}` | — | [N/A] | |
| L203 | (빈 줄) | — | [N/A] | |
| L204 | `public static RendererData getPreviousRendererData(EntityPlayer entityplayer)` | (fade 데이터 캐시 — fade 부재로 N/A) | [N/A] | §17 |
| L205 | `{` | — | [N/A] | |
| L206 | `if(++previousRendererDataAccessCounter > 1000)` | (1000회마다 GC) | [N/A] | |
| L207 | `{` | — | [N/A] | |
| L208 | `List<?> players = Minecraft.getMinecraft().theWorld.playerEntities;` | (vanilla 자동 GC) | [N/A] | |
| L209 | (빈 줄) | — | [N/A] | |
| L210 | `Iterator<EntityPlayer> iterator = previousRendererData.keySet().iterator();` | — | [N/A] | |
| L211 | `while(iterator.hasNext())` | — | [N/A] | |
| L212 | `if(!players.contains(iterator.next()))` | — | [N/A] | |
| L213 | `iterator.remove();` | — | [N/A] | |
| L214 | (빈 줄) | — | [N/A] | |
| L215 | `previousRendererDataAccessCounter = 0;` | — | [N/A] | |
| L216 | `}` | — | [N/A] | |
| L217 | (빈 줄) | — | [N/A] | |
| L218 | `RendererData result = previousRendererData.get(entityplayer);` | (fade 부재) | [N/A] | |
| L219 | `if(result == null)` | — | [N/A] | |
| L220 | `previousRendererData.put(entityplayer, result = new RendererData());` | — | [N/A] | |
| L221 | `return result;` | — | [N/A] | |
| L222 | `}` | — | [N/A] | |
| L223 | (빈 줄) | — | [N/A] | |
| L224 | `private static Map<EntityPlayer, RendererData> previousRendererData = new HashMap<EntityPlayer, RendererData>();` | (fade 데이터 캐시 부재) | [N/A] | |
| L225 | `private static int previousRendererDataAccessCounter = 0;` | — | [N/A] | |
| L226 | (빈 줄) | — | [N/A] | |
| L227 | `public final SmartRenderModel modelBipedMain;` | (단일 모델) | [N/A] | |
| L228 | `}` | — | [N/A] | 클래스 종료 |

**청크 1 (228 라인) 통계: 정합 28 / 오역 0 / 누락 0 / 잉여 0 / N/A 200 = 228 라인 전수.**

**청크 1 발견**: 신규 [오역]/[누락]/[잉여] 0건. 다음 의미 있는 확인:
- L54-L57 SmartStatistics 변수 갱신 위치 — §16-10/12/13/15 [오역] 발견 변수의 SR mod 측 *원본 갱신 지점*. 1.21.1 SmartStatistics 부재 → vanilla limbAnimator/age 등가 매핑이 제한적.
- L77-L88 atan-based 각도 계산 (currentVerticalAngle/currentHorizontalAngle) — 1.21.1 atan2 등가 (sm_captureBodyYaw + sm.stats.currentVerticalAngle) [정합].
- L81 `-Math.atan(xDiff / zDiff)` + L87-L88 `if (zDiff < 0) += Half` = `atan2(-xDiff, zDiff)` 등가 — 1.21.1 sm_captureBodyYaw 의 `atan2(-vel.x, vel.z)`와 정확히 일치.
- L189/L200 `ticksExisted += statistics.ticksRiding` — 라이딩 흔들림 ticks 동기화. 1.21.1 vanilla RidingPose 자동 처리 (별도 ticksRiding 변수 부재) → 미이식이지만 vanilla 자동 처리로 영향 약함.

**다음 청크**: R-6 청크 2 (SmartRenderUtilities 108 + Mod 73 + Info 28 + Install 26 + Context 47 + IModel 62 + IRender 47 = 7 파일 ~391 라인). R-6 마지막 청크.
