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
            sm_animateFlying(limbSwing, limbSwingAmount, animationProgress);
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
     * rotationPointY 변경: 피벗 이동 생략.
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
        }

        // 발: isFeetVineClimbing → vine 공식, else feetClimbType 분기 (R-10b)
        // FeetClimbing ordinal: SLOW_UP_WITH_HOLD_WITHOUT_HANDS(4)/SLOW_UP_WITH_SINK_WITHOUT_HANDS(5)/FAST_UP(6) → UpGrab
        if (sm.isFeetVineClimbing) {
            float total = (MathHelper.cos(limbSwing + HALF) + 1f) * THIRTYTWOTH + SIXTEENTH;
            rightLeg.pitch = -total;
            leftLeg.pitch  = -total;
            float diff = Math.max(0f, MathHelper.cos(limbSwing - QUARTER)) * SIXTYFOURTH;
            rightLeg.roll  =  diff;
            leftLeg.roll   = -diff;
            rightLeg.yaw   = 0f;
            leftLeg.yaw    = 0f;
        } else {
            int fOrd = sm.actualFeetClimbType;
            if (fOrd >= 4 && verticalSpeed > 0f) {
                float feetDistUp = 0.3f / verticalSpeed;
                rightLeg.pitch = MathHelper.cos(limbSwing * 0.6662f)        * feetDistUp * verticalSpeed - 0.3f;
                leftLeg.pitch  = MathHelper.cos(limbSwing * 0.6662f + HALF) * feetDistUp * verticalSpeed - 0.3f;
                rightLeg.roll  = -(MathHelper.cos(limbSwing * 0.6662f) - 1f)          * horizontalSpeed * 0.5f;
                leftLeg.roll   = -(MathHelper.cos(limbSwing * 0.6662f + QUARTER) + 1f) * horizontalSpeed * 0.5f;
            } else {
                rightLeg.pitch = 0f;
                leftLeg.pitch  = 0f;
                rightLeg.roll  = 0f;
                leftLeg.roll   = 0f;
            }
            rightLeg.yaw = 0f;
            leftLeg.yaw  = 0f;
        }

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
        }
    }

    /**
     * isCeilingClimbing: 천장 매달리기 클라이밍.
     * 원본: SmartMovingModel.setRotationAngles() 4번 분기.
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
        head.yaw      = -rotateY;
        // head.yaw는 headYaw * DEG_TO_RAD + rotateY 보정
        head.yaw -= headYaw * DEG_TO_RAD;
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

        head.pitch = -EIGHTH * standSneakFactor;
        head.yaw   = MathHelper.cos(limbSwing / 2f - QUARTER) * walkFactor;

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
    }

    /**
     * isDiving: SM 완전 잠수.
     * 원본: SmartMovingModel.setRotationAngles() 6번 분기 (isDive).
     * bipedOuter X 기울기는 setupTransforms에서 처리.
     */
    private void sm_animateDiving(float limbSwing, float limbSwingAmount) {
        float distance    = limbSwing * 0.7f;
        float walkFactor  = smFactor(limbSwingAmount, 0f, 0.15679921f);
        float standFactor = smFactor(limbSwingAmount, 0.15679921f, 0f);

        // 다리 Z (발차기)
        rightLeg.roll = (MathHelper.cos(distance) + 1f) * 0.52264464f * walkFactor + SIXTEENTH * standFactor;
        leftLeg.roll  = (MathHelper.cos(distance + HALF) - 1f) * 0.52264464f * walkFactor - SIXTEENTH * standFactor;

        // 팔 Z (젓기 — 원본은 YZX 근사)
        rightArm.roll = (MathHelper.cos(distance + HALF) * 0.52264464f * 2.5f + QUARTER) * walkFactor
                + (QUARTER + EIGHTH) * standFactor;
        leftArm.roll  = (MathHelper.cos(distance) * 0.52264464f * 2.5f - QUARTER) * walkFactor
                - (QUARTER + EIGHTH) * standFactor;
    }

    /**
     * isCrawling: 바닥 크롤링.
     * 원본: SmartMovingModel.setRotationAngles() 7번 분기 (isCrawl).
     * 몸통 X 기울기 (QUARTER-THIRTYTWOTH ≈ 79°)로 수평 자세 재현.
     * 원본 YZX 회전 순서 → setAnglesYZX 헬퍼로 정확하게 변환.
     */
    private void sm_animateCrawling(float limbSwing, float limbSwingAmount, float headYaw) {
        float distance    = limbSwing * 1.3f;
        float walkFactor  = smFactor(limbSwingAmount, 0f, 0.12951545f);
        float standFactor = smFactor(limbSwingAmount, 0.12951545f, 0f);

        // 머리
        head.roll  = -headYaw * DEG_TO_RAD;
        head.pitch = -EIGHTH;

        // 몸통: 앞으로 78° 기울임 (수평 자세)
        body.pitch = QUARTER - THIRTYTWOTH;
        body.roll  = MathHelper.cos(distance + QUARTER) * SIXTYFOURTH * walkFactor;
        body.yaw   = MathHelper.cos(distance + HALF) * SIXTYFOURTH * walkFactor;

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

        // 몸통 미세 흔들림
        body.pitch = MathHelper.cos(distance - EIGHTH) * SIXTYFOURTH * walkFactor;
        body.yaw   = MathHelper.cos(distance + EIGHTH) * SIXTYFOURTH * walkFactor;

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
     */
    private void sm_animateFlying(float limbSwing, float limbSwingAmount, float totalTime) {
        float distance    = limbSwing * 0.08f;
        float walkFactor  = smFactor(limbSwingAmount, 0f, 1f);
        float standFactor = smFactor(limbSwingAmount, 1f, 0f);

        // 팔 Y (정지 시 미세 흔들림)
        rightArm.yaw = MathHelper.cos(totalTime * 0.15f) * SIXTEENTH * standFactor;
        leftArm.yaw  = MathHelper.cos(totalTime * 0.15f) * SIXTEENTH * standFactor;

        // 팔 Z (날개짓)
        rightArm.roll = (MathHelper.cos(distance + HALF) * SIXTYFOURTH + HALF - SIXTEENTH) * walkFactor
                + QUARTER * standFactor;
        leftArm.roll  = (MathHelper.cos(distance) * SIXTYFOURTH - HALF + SIXTEENTH) * walkFactor
                - QUARTER * standFactor;

        // 다리
        rightLeg.pitch = MathHelper.cos(distance) * SIXTYFOURTH * walkFactor
                + MathHelper.cos(totalTime * 0.15f + HALF) * SIXTYFOURTH * standFactor;
        leftLeg.pitch  = MathHelper.cos(distance + HALF) * SIXTYFOURTH * walkFactor
                + MathHelper.cos(totalTime * 0.15f) * SIXTYFOURTH * standFactor;
        rightLeg.roll  =  SIXTYFOURTH;
        leftLeg.roll   = -SIXTYFOURTH;
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

        // 팔 Y (좌우 흔들림)
        rightArm.yaw = MathHelper.cos(distance + QUARTER) * EIGHTH;
        leftArm.yaw  = MathHelper.cos(distance + QUARTER) * EIGHTH;
        // 팔 Z
        rightArm.roll = MathHelper.cos(distance) * EIGHTH + QUARTER;
        leftArm.roll  = MathHelper.cos(distance) * EIGHTH - QUARTER;

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
