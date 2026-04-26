package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 12-1: setAngles() Mixin — SM 11가지 이동 상태 애니메이션.
 * 12-2: animateAngleJumping() — 방향 점프 시 팔/다리 각도.
 * 8-3 / 6-4: leaningPitch 강제 0 — 수영/잠수/크롤링 중 vanilla -90° 자동 회전 차단.
 *
 * 원본: SmartMovingModel.setRotationAngles() 이식.
 * 상태 우선순위 (if-else 체인 순서):
 *   isClimbing/isCrawlClimbing → isClimbJumping → isCeilingClimbing →
 *   isSwimming_sm → isDiving → isCrawling → isSliding →
 *   isFlying → isHeadJumping → isFalling → else(standard)
 *
 * 각도 상수 (SmartRenderContext 기반 라디안):
 *   Half=π, Quarter=π/2, Eighth=π/4, Sixteenth=π/8,
 *   Thirtytwoth=π/16, Sixtyfourth=π/32
 *
 * NOTE: 회전 순서 변환은 setAnglesYZX/setAnglesZXY 헬퍼로 처리.
 *       원본 YZX GL 순서 → JOML qY*qZ*qX → getEulerAnglesZYX → ModelPart pitch/yaw/roll.
 *       원본 ZXY GL 순서 → JOML qZ*qX*qY → 동일 변환.
 */
@Environment(EnvType.CLIENT)
@Mixin(BipedEntityModel.class)
public abstract class MixinPlayerEntityModelClient {

    // ── 각도 상수 (SmartRenderContext/SmartRenderUtilities 기준) ─────────────
    private static final float HALF        = (float) Math.PI;        // π
    private static final float QUARTER     = HALF / 2f;              // π/2
    private static final float EIGHTH      = HALF / 4f;              // π/4
    private static final float SIXTEENTH   = HALF / 8f;              // π/8
    private static final float THIRTYTWOTH = HALF / 16f;             // π/16
    private static final float SIXTYFOURTH = HALF / 32f;             // π/32
    private static final float WHOLE       = HALF * 2f;              // 2π
    private static final float DEG_TO_RAD  = (float) (Math.PI / 180.0); // 도→라디안

    // ── BipedEntityModel 파트 @Shadow ────────────────────────────────────────
    @Shadow public ModelPart head;
    @Shadow public ModelPart body;
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightLeg;
    @Shadow public ModelPart leftLeg;

    /** BipedEntityModel.leaningPitch — animateModel()에서 entity.getLeaningPitch()로 세팅됨 */
    @Shadow protected float leaningPitch;

    // ── [12-1] setAngles Mixin (TAIL — vanilla 애니메이션 완료 후 SM이 덮어씀) ──

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",
            at = @At("TAIL"))
    private void sm_setAngles(
            LivingEntity entity,
            float limbSwing, float limbSwingAmount,
            float animationProgress, float headYaw, float headPitch,
            CallbackInfo ci) {
        if (!(entity instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);

        // ── [8-3][6-4] SM 활성 상태에서 leaningPitch 강제 0 ─────────────────
        // leaningPitch > 0이면 setupTransforms Branch 2(-90° X회전)와
        // setAngles Step 13(수영 팔 애니메이션)이 활성화된다. SM 상태에서는 억제.
        boolean flyingCreative = player.getAbilities().flying;
        boolean anySmState = sm.isRopeSliding || sm.isClimbing || sm.isCrawlClimbing || sm.isCeilingClimbing
                || sm.isClimbJumping || sm.isSwimming_sm || sm.isDiving
                || sm.isCrawling || sm.isSliding || sm.isHeadJumping || flyingCreative;
        if (anySmState) {
            this.leaningPitch = 0f;
            // pivot/yaw reset 인프라 (B-9/B-11 부속): vanilla setAngles 는 head/body pivotZ 와
            //   body.yaw 를 매 프레임 reset 하지 않는다 (sneak 분기는 leg.pivotZ 만 변경 /
            //   body.yaw 는 animateArms 안 handSwingProgress > 0 분기에서만 설정).
            //   이전 sm 분기의 변경이 다음 분기까지 누적되는 위험을 막기 위해
            //   SM 분기 진입 직전에 vanilla 기본값(0) 으로 reset 한다.
            head.pivotZ = 0f;
            body.pivotZ = 0f;
            body.yaw    = 0f;
        }

        // ── [12-7] smallOverGroundHeight 계산 ─────────────────────────────────
        // 원본: SmartMovingRender.rotatePlayer() → moving.getOverGroundHeight(5D)
        // isCrawlClimbing/isHeadJumping 상태에서만 발 아래 지면까지의 거리를 계산한다.
        if (sm.isCrawlClimbing || sm.isHeadJumping) {
            sm.smallOverGroundHeight = computeSmallOverGroundHeight(player, player.getWorld());
        }

        // ── SM 11-state if-else 체인 (SmartMovingModel.setRotationAngles 우선순위) ──
        if (sm.isRopeSliding) {
            sm_animateRopeSliding(animationProgress, player);
        } else if (sm.isClimbing || sm.isCrawlClimbing) {
            sm_animateClimbing(sm, limbSwing, limbSwingAmount, headPitch);
        } else if (sm.isClimbJumping) {
            // [isClimbJump] 클라이밍 점프 — 팔을 위로 뻗은 자세
            rightArm.pitch = HALF + SIXTEENTH;
            leftArm.pitch  = HALF + SIXTEENTH;
            rightArm.roll  = -THIRTYTWOTH;
            leftArm.roll   =  THIRTYTWOTH;
        } else if (sm.isCeilingClimbing) {
            sm_animateCeilingClimbing(limbSwing, limbSwingAmount, headYaw);
        } else if (sm.isSwimming_sm) {
            sm_animateSwimming(sm, limbSwing, limbSwingAmount, animationProgress);
        } else if (sm.isDiving) {
            sm_animateDiving(limbSwing, limbSwingAmount);
        } else if (sm.isCrawling) {
            sm_animateCrawling(limbSwing, limbSwingAmount, headYaw);
        } else if (sm.isSliding) {
            sm_animateSliding(limbSwing, limbSwingAmount);
        } else if (flyingCreative) {
            sm_animateFlying(sm, limbSwing, limbSwingAmount, animationProgress);
        } else if (sm.isHeadJumping) {
            sm_animateHeadJumping(sm);
        } else {
            // isFalling: 낙하 중(낙하거리 > 1.5블록, 지면/물 아님, SM 이동 아님)
            boolean isFalling = !player.isOnGround()
                    && player.fallDistance > 1.5f
                    && !sm.isClimbing && !sm.isCrawlClimbing && !sm.isCeilingClimbing
                    && !player.isTouchingWater();
            if (isFalling) {
                sm_animateFalling(animationProgress);
            }
        }

        // ── [12-2] animateAngleJumping — 방향 점프 팔/다리 포즈 ──────────────
        if (sm.isAngleJumping()) {
            sm_animateAngleJumping(sm);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 상태별 애니메이션 헬퍼 메서드
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * isRopeSliding: 로프를 잡고 슬라이딩.
     * 원본: SmartMovingModel.setRotationAngles() 1번 분기 (isRopeSliding).
     * bipedOuter.rotateAngleY(이동 방향 정렬)는 MixinPlayerEntityRenderer bodyYaw에서 처리.
     * bipedPelvic.rotateAngleX: bipedPelvic 없음 → 생략.
     * pivotY 보정 (B-8 / §16-11): vanilla setAngles 가 매 프레임 sneak 분기로 head/arm pivotY 를
     *   명시적 할당하므로 TAIL inject 에서 덮어쓰기 안전 (다음 프레임 자동 reset).
     *   원본 SR 어깨 노드(pivotY=2) 부재로 1.21.1 arm pivotY 기본=2 → -2 차감 = 0 (어깨 절대 위치 0 등가).
     */
    private void sm_animateRopeSliding(float animationProgress, ClientPlayerEntity player) {
        float time = animationProgress * 0.15f;

        // 머리 X/Z
        head.pitch = EIGHTH;
        Vec3d vel = player.getVelocity();
        if (vel.x * vel.x + vel.z * vel.z >= 1e-4) {
            // 카메라-이동 방향 차이를 [-Sixteenth, Sixteenth] 범위로 클램프
            float diff = MathHelper.wrapDegrees(
                    player.getYaw() - (float) Math.toDegrees(Math.atan2(-vel.x, vel.z))
            ) * DEG_TO_RAD;
            head.roll = MathHelper.clamp(diff, -SIXTEENTH, SIXTEENTH);
        } else {
            head.roll = 0f;
        }

        // 몸통 X (주기적 미세 흔들림)
        float torsoX = SIXTEENTH + SIXTYFOURTH * MathHelper.cos(time);
        body.pitch = torsoX;

        // 팔 X = Half - torsoX (매달린 자세), Z (좌우 고정)
        rightArm.pitch = HALF - torsoX;
        leftArm.pitch  = HALF - torsoX;
        rightArm.roll  =  SIXTEENTH + THIRTYTWOTH;
        leftArm.roll   = -(SIXTEENTH + THIRTYTWOTH);

        // 다리 Z (고정), X (흔들림)
        rightLeg.roll  =  THIRTYTWOTH;
        leftLeg.roll   = -THIRTYTWOTH;
        rightLeg.pitch =  SIXTYFOURTH * MathHelper.cos(time - QUARTER);
        leftLeg.pitch  =  SIXTYFOURTH * MathHelper.cos(time + QUARTER);

        // 매달린 자세 pivotY 보정 (원본 SmartMovingModel L106/L117)
        head.pivotY     =  2f;   // 원본 bipedHead.rotationPointY = 2F
        rightArm.pivotY =  0f;   // 원본 bipedRightArm.rotationPointY = -2F (어깨 노드 부재로 vanilla 기본 2F - 2 = 0)
        leftArm.pivotY  =  0f;   // 원본 bipedLeftArm.rotationPointY = -2F
    }

    /**
     * isClimbing / isCrawlClimbing: 사다리/넝쿨 클라이밍.
     * 원본: SmartMovingModel.setRotationAngles() 2번 분기.
     * handsClimbType/feetClimbType ordinal로 손/발 포즈 분기 (R-10/R-10b).
     * isCrawlClimbing 시 legAngleZ(roll) 보정 (R-10c).
     */
    private void sm_animateClimbing(SmartMovingClientState sm, float limbSwing, float limbSwingAmount, float headPitch) {
        float verticalSpeed = Math.min(0.5f, limbSwingAmount);
        float horizontalSpeed = Math.min(0.5f, limbSwingAmount);

        // 머리: 시야 수직 각도 반영, Y=0(몸 방향 고정)
        head.yaw   = 0f;
        head.pitch = headPitch * DEG_TO_RAD;

        // 팔: handsClimbType 3-way 분기 (R-10)
        // ordinal 매핑: UP(4)/FAST_UP(5)→UpGrab, TOP_HOLD(2)/BOTTOM_HOLD(3)→MiddleGrab, NONE(0)/SINK(1)→NoGrab
        int h = sm.actualHandsClimbType;
        // vine climbing: MiddleGrab → UpGrab 전환 (SmartMovingModel L317-319)
        if (sm.isHandsVineClimbing && h >= 2 && h < 4) h = 4;
        float handsDistUp, handsOffset;
        if (h >= 4) {         // UP_GRAB: UP(4), FAST_UP(5)
            handsDistUp = 2f;
            handsOffset = -2.5f;
        } else if (h >= 2) {  // MIDDLE_GRAB: TOP_HOLD(2), BOTTOM_HOLD(3)
            handsDistUp = 2f;
            handsOffset = -QUARTER;
        } else {              // NO_GRAB: NONE(0), SINK(1)
            handsDistUp = 0f;
            handsOffset = -0.5f;
        }
        float rPitch = MathHelper.cos(limbSwing * 0.6662f + HALF) * verticalSpeed * handsDistUp + handsOffset;
        float lPitch = MathHelper.cos(limbSwing * 0.6662f)        * verticalSpeed * handsDistUp + handsOffset;
        float rYaw   = MathHelper.cos(limbSwing * 0.6662f + QUARTER) * horizontalSpeed;
        float lYaw   = MathHelper.cos(limbSwing * 0.6662f)            * horizontalSpeed;
        setAnglesYZX(rightArm, rPitch, rYaw, 0f);
        setAnglesYZX(leftArm,  lPitch, lYaw, 0f);
        // isHandsVineClimbing: yaw 추가 보정 (원본 SmartMovingModel.md L346-352)
        if (sm.isHandsVineClimbing) {
            rightArm.yaw = rightArm.yaw * (1f + 0.6662f) - EIGHTH;
            leftArm.yaw  = leftArm.yaw  * (1f + 0.6662f) + EIGHTH;
            // 원본 SmartMovingModel.java L353: setArmScales(abs(cos(rightArm.X)), abs(cos(leftArm.X)))
            setArmScales(rightArm, leftArm,
                    Math.abs(MathHelper.cos(rightArm.pitch)),
                    Math.abs(MathHelper.cos(leftArm.pitch)));
        }

        // 발 각도 — 원본 SmartMovingModel.java L219-L239 1:1 이식.
        // 핵심: pitch(rotateAngleX)는 `if(!isFeetVineClimbing)` 가드 블록에서만 할당 →
        //       vine 시 일반 경로 스킵 + vine 블록에서 `= -total` 단독 할당.
        // 핵심: roll(rotateAngleZ)은 **무조건** 일반 경로 할당 →
        //       vine 시 그 위에 `+=` 로 누적.
        // FeetClimbing ordinal≥4: SLOW_UP_WITH_HOLD_WITHOUT_HANDS(4)/SLOW_UP_WITH_SINK_WITHOUT_HANDS(5)/FAST_UP(6) → UpGrab.
        // UpGrab 파라미터(SmartMovingModel.md L331-333): feetDistSideFactor=0.5, feetDistSideOffset=0.
        int fOrd = sm.actualFeetClimbType;
        boolean isUpGrab = fOrd >= 4;

        // pitch 일반 경로 — 원본 L219-L223 `if(!isFeetVineClimbing)` 가드.
        if (!sm.isFeetVineClimbing) {
            if (isUpGrab && verticalSpeed > 0f) {
                float feetDistUp = 0.3f / verticalSpeed;
                rightLeg.pitch = MathHelper.cos(limbSwing * 0.6662f)        * feetDistUp * verticalSpeed - 0.3f;
                leftLeg.pitch  = MathHelper.cos(limbSwing * 0.6662f + HALF) * feetDistUp * verticalSpeed - 0.3f;
            } else {
                rightLeg.pitch = 0f;
                leftLeg.pitch  = 0f;
            }
        }

        // roll 일반 경로 — 원본 L225-L226 무조건 할당.
        // default feetClimbType: feetDistSideFactor=0 → roll=0. UpGrab: 0.5.
        float feetDistSideFactor = isUpGrab ? 0.5f : 0f;
        rightLeg.roll = -(MathHelper.cos(limbSwing * 0.6662f) - 1f)          * horizontalSpeed * feetDistSideFactor;
        leftLeg.roll  = -(MathHelper.cos(limbSwing * 0.6662f + QUARTER) + 1f) * horizontalSpeed * feetDistSideFactor;

        // vine 전용 — 원본 L228-L239.
        if (sm.isFeetVineClimbing) {
            float total = (MathHelper.cos(limbSwing + HALF) + 1f) * THIRTYTWOTH + SIXTEENTH;
            rightLeg.pitch = -total;   // pitch 덮어쓰기
            leftLeg.pitch  = -total;

            float diff = Math.max(0f, MathHelper.cos(limbSwing - QUARTER)) * SIXTYFOURTH;
            leftLeg.roll  += -diff;    // roll 누적 (원본 `+=`)
            rightLeg.roll +=  diff;

            // 원본 SmartMovingModel.java L391: setLegScales(abs(cos(rightLeg.X)), abs(cos(leftLeg.X)))
            setLegScales(rightLeg, leftLeg,
                    Math.abs(MathHelper.cos(rightLeg.pitch)),
                    Math.abs(MathHelper.cos(leftLeg.pitch)));
        }

        rightLeg.yaw = 0f;
        leftLeg.yaw  = 0f;

        // isCrawlClimbing 추가 보정: 몸통 X 기울기 + 다리 roll (R-10c)
        if (sm.isCrawlClimbing) {
            float height = sm.smallOverGroundHeight + 0.25f;
            float bodyLength = 0.7f, legLength = 0.55f;
            float bodyAngleX, legAngleX, legAngleZ;
            if (height < bodyLength) {
                bodyAngleX = Math.max(0f, (float) Math.acos(height / bodyLength));
                legAngleX  = QUARTER - bodyAngleX;
                legAngleZ  = THIRTYTWOTH;
            } else if (height < bodyLength + legLength) {
                bodyAngleX = 0f;
                legAngleX  = Math.max(0f, (float) Math.acos((height - bodyLength) / legLength));
                legAngleZ  = THIRTYTWOTH * (legAngleX / 1.537f);
            } else {
                bodyAngleX = 0f;
                legAngleX  = 0f;
                legAngleZ  = 0f;
            }
            body.pitch     =  bodyAngleX;
            head.pitch     = -bodyAngleX;
            rightLeg.pitch =  legAngleX;
            leftLeg.pitch  =  legAngleX;
            rightLeg.roll  =  legAngleZ;
            leftLeg.roll   = -legAngleZ;

            // NoGrab + non-NoStep 추가 보정 (원본 SmartMovingModel L279-L286)
            // 원본: bipedTorso.X=0.5F (L281), head.X-=0.5F (L282), bipedPelvic.X-=0.5F (L283),
            //       bipedTorso.rotationPointZ = -6F (L285).
            // bipedPelvic.X-=0.5F: 1.21.1 다리는 body 자식이 아니므로 leg 별도 처리 필요 (§16-26 후속).
            // body.pivotZ = -6F: B-9 / §16-14 — 원본 bipedTorso 가 root(bipedOuter)의 자식으로 모든
            //   visual 노드를 자식으로 거느리므로 -6 = 전체 visual 이동. 1.21.1 단일 PlayerEntityModel
            //   에서는 body 단일 노드만 대응 (head/arm/leg 이동 누락 = SR 다층 부재 근사).
            if (sm.actualHandsClimbType < 2 && sm.actualFeetClimbType > 0) {
                body.pitch = 0.5f;
                head.pitch -= 0.5f;
                body.pivotZ = -6f;   // 원본 bipedTorso.rotationPointZ = -6F (B-9 / §16-14)
            }
        }
    }

    /**
     * isCeilingClimbing: 천장 매달리기 클라이밍.
     * 원본: SmartMovingModel.setRotationAngles() 4번 분기 (L284-L316).
     * head.yaw 는 원본 L315 `bipedHead.rotateAngleY = -rotateY;` 절대 할당으로
     *   vanilla setAngles 의 net head yaw (i*PI/180) 결과를 덮어쓴다 — 매달림 자세에서
     *   머리는 ceiling climb 동작에만 따르며 마우스 head yaw 입력 무시 (의도).
     * B-18 / §16-17: 이전 구현의 `head.yaw -= headYaw * DEG_TO_RAD` 차감은 원본에 없는
     *   잉여 보정이었으므로 제거 (세션 25).
     */
    private void sm_animateCeilingClimbing(float limbSwing, float limbSwingAmount, float headYaw) {
        float distance    = limbSwing * 0.7f;
        float walkFactor  = smFactor(limbSwingAmount, 0f, 0.12951545f);
        float standFactor = smFactor(limbSwingAmount, 0.12951545f, 0f);

        // 팔: 천장을 향해 위로 (XYZ 근사)
        leftArm.pitch  = (MathHelper.cos(distance) * 0.52f + HALF) * walkFactor + HALF * standFactor;
        rightArm.pitch = (MathHelper.cos(distance + HALF) * 0.52f - HALF) * walkFactor - HALF * standFactor;
        leftLeg.pitch  = -MathHelper.cos(distance) * 0.12f * walkFactor;
        rightLeg.pitch = -MathHelper.cos(distance + HALF) * 0.32f * walkFactor;

        float rotateY = MathHelper.cos(distance) * 0.44f * walkFactor;
        rightArm.yaw = leftArm.yaw = -rotateY;
        rightLeg.yaw = leftLeg.yaw = -rotateY;
        head.yaw     = -rotateY;
    }

    /**
     * isSwimming_sm: SM 수면 수영.
     * 원본: SmartMovingModel.setRotationAngles() 5번 분기 (isSwim).
     * bipedOuter X 기울기는 setupTransforms Mixin에서 처리 (standSneakFactor 경유).
     */
    private void sm_animateSwimming(SmartMovingClientState sm, float limbSwing, float limbSwingAmount, float totalTime) {
        float walkFactor  = smFactor(limbSwingAmount, 0.15679921f, 0.52264464f);
        float sneakFactor = Math.min(
                smFactor(limbSwingAmount, 0f, 0.15679921f),
                smFactor(limbSwingAmount, 0.52264464f, 0.15679921f));
        float standFactor = smFactor(limbSwingAmount, 0.15679921f, 0f);
        float standSneakFactor = standFactor + sneakFactor;
        sm.swimStandSneakFactor = standSneakFactor;

        // 머리 (YXZ 순서): rotateAngleY = cos(distance/2 - Quarter) * walkFactor,
        //                  rotateAngleX = -Eighth * standSneakFactor (원본 SmartMovingModel.java L504-L506).
        // R-17: YXZ → GL call Z, X, Y → setAnglesYXZ 헬퍼.
        setAnglesYXZ(head,
                -EIGHTH * standSneakFactor,
                MathHelper.cos(limbSwing / 2f - QUARTER) * walkFactor,
                0f);
        head.pivotZ = -2f;   // 원본 bipedHead.rotationPointZ = -2F (B-10 / §16-15, 수영 자세 머리 앞으로 2px)

        // 몸통 yaw (B-11 / §16-16): 자유형 영법 좌우 흔들림.
        // 원본 SmartMovingModel.java L335: bipedBreast.rotateAngleY = bipedBody.rotateAngleY = cos(distance/2 - Quarter) * walkFactor
        //   (Breast 부재 — body 단일 노드만 적용).
        body.yaw = MathHelper.cos(limbSwing / 2f - QUARTER) * walkFactor;

        // 팔 (YZX 순서): pitch=X(앞뒤 젓기), yaw=0, roll=Z(좌우 펼침)
        float dist2      = limbSwing * 0.5f;
        float rightPitch = ((dist2 % WHOLE) - HALF) * walkFactor + SIXTEENTH * standSneakFactor;
        float leftPitch  = (((dist2 + HALF) % WHOLE) - HALF) * walkFactor + SIXTEENTH * standSneakFactor;
        float rightRoll  = QUARTER + EIGHTH + MathHelper.cos(totalTime * 0.1f) * standSneakFactor * 0.8f;
        float leftRoll   = -QUARTER - EIGHTH - MathHelper.cos(totalTime * 0.1f) * standSneakFactor * 0.8f;
        setAnglesYZX(rightArm, rightPitch, 0f, rightRoll);
        setAnglesYZX(leftArm,  leftPitch,  0f, leftRoll);

        // 다리 X (앞뒤 발차기)
        rightLeg.pitch = MathHelper.cos(limbSwing) * 0.52264464f * walkFactor;
        leftLeg.pitch  = MathHelper.cos(limbSwing + HALF) * 0.52264464f * walkFactor;

        float feetZ = SIXTEENTH * standSneakFactor
                + MathHelper.cos(totalTime * 0.1f) * 0.4f * (standFactor - sneakFactor);
        rightLeg.roll =  feetZ;
        leftLeg.roll  = -feetZ;

        // 원본 SmartMovingModel.java L532-L542: 다리/팔 yScale 호흡 패턴 (sneakFactor 기반).
        // 원본 가드 `if (scaleLegType != NoScaleStart)` — 메인 모델 = Scale 이므로 항상 true.
        float legSc = 1f + (MathHelper.cos(totalTime * 0.1f + QUARTER) - 1f) * 0.15f * sneakFactor;
        setLegScales(rightLeg, leftLeg, legSc, legSc);
        float armSc = 1f + (MathHelper.cos(totalTime * 0.1f - QUARTER) - 1f) * 0.15f * sneakFactor;
        setArmScales(rightArm, leftArm, armSc, armSc);
    }

    /**
     * isDiving: SM 완전 잠수.
     * 원본: SmartMovingModel.setRotationAngles() 6번 분기 (isDive, L363-L390).
     * bipedOuter X 기울기는 setupTransforms에서 처리.
     * 머리 자세 (B-10 / §16-15):
     *   - head.pitch = -EIGHTH (원본 L370): 다이빙 시 머리 살짝 위로 들기
     *   - head.pivotZ = -2F (원본 L371): 머리 앞으로 2px 이동
     *   vanilla 가 매 프레임 head.pitch (j*PI/180) 와 head.pivotZ (B-9 reset 인프라) 모두 reset → 안전.
     */
    private void sm_animateDiving(float limbSwing, float limbSwingAmount) {
        float distance    = limbSwing * 0.7f;
        float walkFactor  = smFactor(limbSwingAmount, 0f, 0.15679921f);
        float standFactor = smFactor(limbSwingAmount, 0.15679921f, 0f);

        // 머리 자세 (원본 L370-L371)
        head.pitch  = -EIGHTH;   // 원본 bipedHead.rotateAngleX = -Eighth
        head.pivotZ = -2f;       // 원본 bipedHead.rotationPointZ = -2F

        // 다리 Z (발차기)
        rightLeg.roll = (MathHelper.cos(distance) + 1f) * 0.52264464f * walkFactor + SIXTEENTH * standFactor;
        leftLeg.roll  = (MathHelper.cos(distance + HALF) - 1f) * 0.52264464f * walkFactor - SIXTEENTH * standFactor;

        // 팔 Z (젓기 — 원본은 YZX 근사)
        rightArm.roll = (MathHelper.cos(distance + HALF) * 0.52264464f * 2.5f + QUARTER) * walkFactor
                + (QUARTER + EIGHTH) * standFactor;
        leftArm.roll  = (MathHelper.cos(distance) * 0.52264464f * 2.5f - QUARTER) * walkFactor
                - (QUARTER + EIGHTH) * standFactor;

        // 원본 SmartMovingModel.java L572-L583: 다리/팔 yScale (walkFactor 기반).
        // 원본 가드 `if (scaleLegType != NoScaleStart)` — 메인 모델 = Scale.
        // 다리: 0.25F * walkFactor / 팔: 0.15F * walkFactor (계수 다름).
        float legSc = 1f + (MathHelper.cos(distance - QUARTER) - 1f) * 0.25f * walkFactor;
        setLegScales(rightLeg, leftLeg, legSc, legSc);
        float armSc = 1f + (MathHelper.cos(distance + QUARTER) - 1f) * 0.15f * walkFactor;
        setArmScales(rightArm, leftArm, armSc, armSc);
    }

    /**
     * isCrawling: 바닥 크롤링.
     * 원본: SmartMovingModel.setRotationAngles() 7번 분기 (isCrawl, L395-L431).
     * 몸통 X 기울기 (QUARTER-THIRTYTWOTH ≈ 79°)로 수평 자세 재현.
     * 원본 YZX 회전 순서 → setAnglesYZX 헬퍼로 정확하게 변환.
     * pivot 보정 (B-12 / §16-18):
     *   - head.pivotZ = -2F (원본 L401): 몸이 수평이므로 머리 앞쪽 2 픽셀 이동
     *   - body.pivotY = +3F (원본 L405): 수평 자세에서 몸통 위치 보정 (SR bipedTorso 단일 노드 근사)
     */
    private void sm_animateCrawling(float limbSwing, float limbSwingAmount, float headYaw) {
        float distance    = limbSwing * 1.3f;
        float walkFactor  = smFactor(limbSwingAmount, 0f, 0.12951545f);
        float standFactor = smFactor(limbSwingAmount, 0.12951545f, 0f);

        // 머리
        head.roll  = -headYaw * DEG_TO_RAD;
        head.pitch = -EIGHTH;
        head.pivotZ = -2f;   // 원본 bipedHead.rotationPointZ = -2F (B-12 / §16-18)

        // 몸통: 앞으로 78° 기울임 (수평 자세)
        body.pitch = QUARTER - THIRTYTWOTH;
        body.roll  = MathHelper.cos(distance + QUARTER) * SIXTYFOURTH * walkFactor;
        body.yaw   = MathHelper.cos(distance + HALF) * SIXTYFOURTH * walkFactor;
        body.pivotY = 3f;    // 원본 bipedTorso.rotationPointY = +3F (B-12 / §16-18, SR 다층 부재로 body 단일 노드 근사)

        // 다리
        rightLeg.pitch = (MathHelper.cos(distance - QUARTER) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor
                + THIRTYTWOTH * standFactor;
        leftLeg.pitch  = (MathHelper.cos(distance - HALF - QUARTER) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor
                + THIRTYTWOTH * standFactor;
        rightLeg.roll  = (MathHelper.cos(distance - QUARTER) + 1f) * 0.25f * walkFactor + THIRTYTWOTH * standFactor;
        leftLeg.roll   = (MathHelper.cos(distance - QUARTER) - 1f) * 0.25f * walkFactor - THIRTYTWOTH * standFactor;

        // 팔 (YZX 순서): pitch=X(앞뒤), yaw=Y(±Quarter), roll=Z(좌우)
        float rRoll = (MathHelper.cos(distance + HALF) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor
                + SIXTEENTH * standFactor;
        float lRoll = (MathHelper.cos(distance + HALF) * SIXTYFOURTH - THIRTYTWOTH) * walkFactor
                - SIXTEENTH * standFactor;
        setAnglesYZX(rightArm, HALF + EIGHTH, -QUARTER, rRoll);
        setAnglesYZX(leftArm,  HALF + EIGHTH,  QUARTER, lRoll);

        // 원본 SmartMovingModel.java L622-L626 + L645-L648: 다리/팔 yScale.
        // 원본 가드 `if (scaleLegType != NoScaleStart)` / `scaleArmType != NoScaleStart` — 메인 모델 = Scale.
        // 좌우 위상 다름 — 원본 표기 그대로 보존 (cos(distance + Quarter - Quarter) = cos(distance) 등 단순화 안 함).
        setLegScales(rightLeg, leftLeg,
                1f + (MathHelper.cos(distance + QUARTER - QUARTER) - 1f) * 0.25f * walkFactor,
                1f + (MathHelper.cos(distance - QUARTER - QUARTER) - 1f) * 0.25f * walkFactor);
        setArmScales(rightArm, leftArm,
                1f + (MathHelper.cos(distance + QUARTER) - 1f) * 0.15f * walkFactor,
                1f + (MathHelper.cos(distance - QUARTER) - 1f) * 0.15f * walkFactor);
    }

    /**
     * isSliding: 미끄러지기.
     * 원본: SmartMovingModel.setRotationAngles() 8번 분기 (isSlide).
     * bipedOuter X 기울기(Quarter)는 setupTransforms에서 처리.
     * 원본 YZX 회전 순서 → setAnglesYZX 헬퍼로 정확하게 변환.
     */
    private void sm_animateSliding(float limbSwing, float limbSwingAmount) {
        float distance   = limbSwing * 0.7f;
        float walkFactor = smFactor(limbSwingAmount, 0f, 1f) * 0.8f;

        head.pitch = -EIGHTH - SIXTEENTH;

        // 몸통 (YXZ 순서) — 원본 SmartMovingModel.java L672-L676:
        //   bipedBody.rotationOrder = YXZ
        //   bipedBody.rotateAngleX = cos(distance - Eighth) * Sixtyfourth * walkFactor
        //   bipedBody.rotateAngleY = cos(distance + Eighth) * Sixtyfourth * walkFactor
        // R-17: YXZ → GL call Z, X, Y → setAnglesYXZ 헬퍼.
        setAnglesYXZ(body,
                MathHelper.cos(distance - EIGHTH) * SIXTYFOURTH * walkFactor,
                MathHelper.cos(distance + EIGHTH) * SIXTYFOURTH * walkFactor,
                0f);

        // 다리
        rightLeg.pitch = MathHelper.cos(distance + HALF) * SIXTYFOURTH * walkFactor + SIXTYFOURTH;
        leftLeg.pitch  = MathHelper.cos(distance + QUARTER) * SIXTYFOURTH * walkFactor + SIXTYFOURTH;
        rightLeg.roll  =  THIRTYTWOTH;
        leftLeg.roll   = -THIRTYTWOTH;

        // 팔 (YZX 순서): pitch=X(앞뒤), yaw=Y(±Quarter), roll=Z(±Sixteenth)
        float rPitch = MathHelper.cos(distance + QUARTER) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        float lPitch = MathHelper.cos(distance - HALF) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        setAnglesYZX(rightArm, rPitch, -QUARTER,  SIXTEENTH);
        setAnglesYZX(leftArm,  lPitch,  QUARTER, -SIXTEENTH);
    }

    /**
     * isFlying (creative): 창작 모드 비행.
     * 원본: SmartMovingModel.setRotationAngles() 9번 분기 (isFlying).
     * bipedOuter X 기울기는 setupTransforms에서 처리.
     * 원본 팔 회전 순서 XZY → 여기서는 XYZ 근사.
     *
     * ANIM-01: head.pitch 보정.
     * setupTransforms @TAIL: matrices.multiply(POSITIVE_X.rotation(θ)) 전역 적용.
     * θ = (Quarter - currentVerticalAngle) * walkFactor
     * 원본: bipedHead.X = -bipedOuter.X / 2 = -θ/2 → world-space head X = θ/2
     * 1.21.1 등가: head.pitch = -θ/2 (전역 θ 상쇄 후 최종 θ/2)
     */
    private void sm_animateFlying(SmartMovingClientState sm, float limbSwing, float limbSwingAmount, float totalTime) {
        float distance    = limbSwing * 0.08f;
        float walkFactor  = smFactor(limbSwingAmount, 0f, 1f);
        float standFactor = smFactor(limbSwingAmount, 1f, 0f);

        // 팔 (XZY 순서) — 원본 SmartMovingModel.java L696-L733:
        //   bipedRightArm.rotationOrder = XZY
        //   rotateAngleY = cos(time*0.15) * Sixteenth * standFactor   (정지 시 미세 흔들림)
        //   rotateAngleZ = (cos(distance + Half) * Sixtyfourth + Half - Sixteenth) * walkFactor + Quarter * standFactor   (날개짓)
        // R-17: XZY → GL call Y, Z, X → setAnglesXZY 헬퍼.
        float rYaw  = MathHelper.cos(totalTime * 0.15f) * SIXTEENTH * standFactor;
        float lYaw  = MathHelper.cos(totalTime * 0.15f) * SIXTEENTH * standFactor;
        float rRoll = (MathHelper.cos(distance + HALF) * SIXTYFOURTH + HALF - SIXTEENTH) * walkFactor
                + QUARTER * standFactor;
        float lRoll = (MathHelper.cos(distance) * SIXTYFOURTH - HALF + SIXTEENTH) * walkFactor
                - QUARTER * standFactor;
        setAnglesXZY(rightArm, 0f, rYaw, rRoll);
        setAnglesXZY(leftArm,  0f, lYaw, lRoll);

        // 다리
        rightLeg.pitch = MathHelper.cos(distance) * SIXTYFOURTH * walkFactor
                + MathHelper.cos(totalTime * 0.15f + HALF) * SIXTYFOURTH * standFactor;
        leftLeg.pitch  = MathHelper.cos(distance + HALF) * SIXTYFOURTH * walkFactor
                + MathHelper.cos(totalTime * 0.15f) * SIXTYFOURTH * standFactor;
        rightLeg.roll  =  SIXTYFOURTH;
        leftLeg.roll   = -SIXTYFOURTH;

        // ANIM-01: head pitch 보정 — setupTransforms theta의 절반 역보정
        float speedFactor = Math.min(1f, Math.max(0f, sm.stats.currentSpeed));
        float theta = (QUARTER - sm.stats.currentVerticalAngle) * speedFactor;
        head.pitch = -theta / 2f;
    }

    /**
     * isHeadJumping: 헤드점프 (낮은 천장 아래 점프).
     * 원본: SmartMovingModel.setRotationAngles() 10번 분기 (isHeadJump).
     * bipedOuter X/Y 기울기는 setupTransforms에서 처리.
     * smallOverGroundHeight로 팔 Z 각도 클램프.
     */
    private void sm_animateHeadJumping(SmartMovingClientState sm) {
        float angle = sm.stats.currentVerticalAngle;

        // bendFactor: Factor(angle, Quarter, 0) ∩ Factor(angle, -Quarter, 0)
        // 수직 각도 0°(수평)일 때 최대 1, ±Quarter(수직)일 때 0. (SmartMovingModel.md 10번 분기)
        float bendFactor = Math.min(smFactor(angle, QUARTER, 0f), smFactor(angle, -QUARTER, 0f));
        rightArm.pitch = bendFactor * -EIGHTH;
        leftArm.pitch  = bendFactor * -EIGHTH;
        rightLeg.pitch = bendFactor * -EIGHTH;
        leftLeg.pitch  = bendFactor * -EIGHTH;

        // 머리 X 보정: setupTransforms에서 θ=Quarter-angle이 전역 적용됨.
        // 원본 head world-X = θ/2 → head.pitch = -θ/2 (C-09-2 등가 증명)
        head.pitch = -(QUARTER - angle) / 2f;

        // 팔 Z: Factor(angle, Quarter, -Quarter). 머리 위 고체 블록이면 smallOverGroundHeight/5로 클램프.
        float armFactorZ = smFactor(angle, QUARTER, -QUARTER);
        if (sm.smallOverGroundHeight < 5f) {
            armFactorZ = Math.min(armFactorZ, sm.smallOverGroundHeight / 5f);
        }
        rightArm.roll  =  HALF - SIXTEENTH + armFactorZ * EIGHTH;
        leftArm.roll   = -(HALF - SIXTEENTH) - armFactorZ * EIGHTH;

        // 다리 Z: Factor(angle, -Quarter, Quarter)
        float legFactorZ = smFactor(angle, -QUARTER, QUARTER);
        rightLeg.roll =  SIXTYFOURTH * legFactorZ;
        leftLeg.roll  = -SIXTYFOURTH * legFactorZ;
    }

    /**
     * isFalling: 자유 낙하.
     * 원본: SmartMovingModel.setRotationAngles() 11번 분기 (isFalling).
     * totalTime(animationProgress)으로 totalDistance 근사.
     * 원본 팔 회전 순서 XZY → 여기서는 XYZ 근사.
     */
    private void sm_animateFalling(float animationProgress) {
        float distance = animationProgress * 0.1f;

        // 팔 (XZY 순서) — 원본 SmartMovingModel.java L768-L792:
        //   bipedRightArm.rotationOrder = XZY
        //   rotateAngleY = cos(distance + Quarter) * Eighth   (좌우 흔들림)
        //   rotateAngleZ = cos(distance) * Eighth ± Quarter   (Z, 좌우 부호 반대)
        // R-17: XZY → GL call Y, Z, X → setAnglesXZY 헬퍼.
        float rYaw  = MathHelper.cos(distance + QUARTER) * EIGHTH;
        float lYaw  = MathHelper.cos(distance + QUARTER) * EIGHTH;
        float rRoll = MathHelper.cos(distance) * EIGHTH + QUARTER;
        float lRoll = MathHelper.cos(distance) * EIGHTH - QUARTER;
        setAnglesXZY(rightArm, 0f, rYaw, rRoll);
        setAnglesXZY(leftArm,  0f, lYaw, lRoll);

        // 다리 X (앞뒤 흔들림)
        rightLeg.pitch = MathHelper.cos(distance + HALF + QUARTER) * SIXTEENTH + THIRTYTWOTH;
        leftLeg.pitch  = MathHelper.cos(distance + QUARTER) * SIXTEENTH + THIRTYTWOTH;
        // 다리 Z (좌우 흔들림)
        rightLeg.roll  = MathHelper.cos(distance) * SIXTEENTH + THIRTYTWOTH;
        leftLeg.roll   = MathHelper.cos(distance) * SIXTEENTH - THIRTYTWOTH;
    }

    /**
     * [12-2] animateAngleJumping: 방향 점프 시 팔/다리 각도.
     * 원본: SmartMovingModel.animateAngleJumping().
     * 원본 다리 ZXY 회전 순서 → setAnglesZXY 헬퍼로 정확하게 변환.
     * 원본 bipedPelvic.rotateAngleY 조정: 1.21.1에 bipedPelvic 없음 → 생략.
     */
    private void sm_animateAngleJumping(SmartMovingClientState sm) {
        float angle    = sm.angleJumpType * EIGHTH;
        float backness  = 1f - Math.abs(angle - HALF) / QUARTER;
        float leftness  = -Math.min(angle - HALF, 0f) / QUARTER;
        float rightness =  Math.max(angle - HALF, 0f) / QUARTER;

        // 다리 (ZXY 순서): pitch=X, yaw=Y(-angle), roll=Z
        setAnglesZXY(leftLeg,  THIRTYTWOTH * (1f + rightness), -angle,  THIRTYTWOTH * backness);
        setAnglesZXY(rightLeg, THIRTYTWOTH * (1f + leftness),  -angle, -THIRTYTWOTH * backness);

        // 팔
        leftArm.roll   = -SIXTEENTH * rightness;
        rightArm.roll  =  SIXTEENTH * leftness;
        leftArm.pitch  = -EIGHTH * backness;
        rightArm.pitch = -EIGHTH * backness;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 내부 유틸리티
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * [12-7] 발 아래 지면까지의 거리 계산 (최대 5블록).
     *
     * 원본: SmartMovingBase.getOverGroundHeight(5D) 클라이언트 경로
     *   = sp.boundingBox.minY - getMaxPlayerSolidBetween(minY - 5D, minY, 0)
     *
     * 1.21.1: 플레이어 중심 열(column)을 최대 5블록 아래까지 스캔하여
     *         첫 고체 블록의 top surface Y를 구하고, playerY와의 차를 반환한다.
     * 반환값 범위: 0.0F (지면 위) ~ 5.0F (5블록 아래까지 고체 없음).
     */
    private static float computeSmallOverGroundHeight(ClientPlayerEntity player, World world) {
        double playerY = player.getY(); // feet Y (= bounding box minY)
        int px = MathHelper.floor(player.getX());
        int pz = MathHelper.floor(player.getZ());
        int startY = MathHelper.floor(playerY) - 1;

        for (int i = 0; i < 5; i++) {
            BlockPos pos = new BlockPos(px, startY - i, pz);
            VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos);
            if (!shape.isEmpty()) {
                double blockTopY = (startY - i) + shape.getMax(Direction.Axis.Y);
                return (float) Math.max(0D, playerY - blockTopY);
            }
        }
        return 5f;
    }

    /**
     * 원본 SmartMovingModel.setArmScales(rightScale, leftScale) 1:1 이식.
     *
     * 원본 본체 (SmartMovingModel.java L948-L963):
     *   if (scaleArmType == Scale)         { rightArm.scaleY = rs; leftArm.scaleY = ls; }
     *   else if (scaleArmType == NoScaleEnd) { rightArm.offsetY -= (1F - rs) * 0.5F; ... }
     *
     * 메인 모델 = `Scale` 타입 (SmartMovingRender.java L88-L89: modelBipedMain.scaleArmType=Scale)
     *   → 1.21.1 PlayerEntityModel 도 메인 플레이어 모델이므로 Scale 분기 본체만 적용.
     *
     * 근사 이식 — 원본과 차이: scaleArmType=NoScaleEnd 분기 (갑옷 흉갑) 미이식.
     *   ModelPart 에 offsetY 필드 부재 + 갑옷 레이어는 별도 ArmorFeatureRenderer 처리.
     *   → 갑옷 yScale 보정은 본 포커스 #1 (메인 애니메이션) 범위 외 (§7 등재 / 잔여).
     */
    private static void setArmScales(ModelPart rightArm, ModelPart leftArm, float rightScale, float leftScale) {
        rightArm.yScale = rightScale;
        leftArm.yScale  = leftScale;
    }

    /**
     * 원본 SmartMovingModel.setLegScales(rightScale, leftScale) 1:1 이식.
     * 원본 본체 (SmartMovingModel.java L972-L987) — setArmScales 와 동일 패턴, 대상이 다리.
     * 메인 모델 = Scale (SmartMovingRender.java L89: modelBipedMain.scaleLegType=Scale).
     */
    private static void setLegScales(ModelPart rightLeg, ModelPart leftLeg, float rightScale, float leftScale) {
        rightLeg.yScale = rightScale;
        leftLeg.yScale  = leftScale;
    }

    /**
     * 원본 YZX GL 순서(glRotate Y → Z → X) → ModelPart pitch/yaw/roll 변환.
     * JOML: qY*qZ*qX → getEulerAnglesZYX → (e.x=pitch, e.y=yaw, e.z=roll).
     */
    private static void setAnglesYZX(ModelPart part, float pitch, float yaw, float roll) {
        Quaternionf q = new Quaternionf()
                .rotationY(yaw)
                .mul(new Quaternionf().rotationZ(roll))
                .mul(new Quaternionf().rotationX(pitch));
        Vector3f e = q.getEulerAnglesZYX(new Vector3f());
        part.pitch = e.x;
        part.yaw   = e.y;
        part.roll  = e.z;
    }

    /**
     * 원본 YXZ GL 순서(glRotate Y → X → Z) → ModelPart pitch/yaw/roll 변환.
     * post-multiply 규칙: GL call 순서 = MatrixStack call 순서 = vertex 적용 역순.
     * vertex 적용 Y→X→Z 가 필요하므로 GL call 순서는 Z, X, Y → JOML qY * qX * qZ.
     * JOML: qY*qX*qZ → getEulerAnglesZYX → (e.x=pitch, e.y=yaw, e.z=roll).
     *
     * R-17 검증 (ModelRotationRenderer.rotate() GitHub 직접):
     *   YXZ(2): glRotatef(Z) → glRotatef(X) → glRotatef(Y) ✓
     *
     * 사용처: isSwim head, isSlide body.
     */
    private static void setAnglesYXZ(ModelPart part, float pitch, float yaw, float roll) {
        Quaternionf q = new Quaternionf()
                .rotationY(yaw)
                .mul(new Quaternionf().rotationX(pitch))
                .mul(new Quaternionf().rotationZ(roll));
        Vector3f e = q.getEulerAnglesZYX(new Vector3f());
        part.pitch = e.x;
        part.yaw   = e.y;
        part.roll  = e.z;
    }

    /**
     * 원본 XZY GL 순서(glRotate X → Z → Y) → ModelPart pitch/yaw/roll 변환.
     * vertex 적용 X→Z→Y → GL call 순서 Y, Z, X → JOML qX * qZ * qY.
     *
     * R-17 검증 (ModelRotationRenderer.rotate() GitHub 직접):
     *   XZY(1): glRotatef(Y) → glRotatef(Z) → glRotatef(X) ✓
     *
     * 사용처: isFlying arm, isFalling arm.
     */
    private static void setAnglesXZY(ModelPart part, float pitch, float yaw, float roll) {
        Quaternionf q = new Quaternionf()
                .rotationX(pitch)
                .mul(new Quaternionf().rotationZ(roll))
                .mul(new Quaternionf().rotationY(yaw));
        Vector3f e = q.getEulerAnglesZYX(new Vector3f());
        part.pitch = e.x;
        part.yaw   = e.y;
        part.roll  = e.z;
    }

    /**
     * 원본 ZXY GL 순서(glRotate Z → X → Y) → ModelPart pitch/yaw/roll 변환.
     * JOML: qZ*qX*qY → getEulerAnglesZYX → (e.x=pitch, e.y=yaw, e.z=roll).
     */
    private static void setAnglesZXY(ModelPart part, float pitch, float yaw, float roll) {
        Quaternionf q = new Quaternionf()
                .rotationZ(roll)
                .mul(new Quaternionf().rotationX(pitch))
                .mul(new Quaternionf().rotationY(yaw));
        Vector3f e = q.getEulerAnglesZYX(new Vector3f());
        part.pitch = e.x;
        part.yaw   = e.y;
        part.roll  = e.z;
    }

    /**
     * SmartMovingModel.Factor() 이식 — 선형 보간 팩터.
     * x0~x1 범위에서 0→1(또는 1→0) 보간. 범위 밖 클램프.
     */
    private static float smFactor(float x, float x0, float x1) {
        if (x0 > x1) {
            if (x <= x1) return 1f;
            if (x >= x0) return 0f;
            return (x0 - x) / (x0 - x1);
        } else {
            if (x >= x1) return 1f;
            if (x <= x0) return 0f;
            return (x - x0) / (x1 - x0);
        }
    }
}
