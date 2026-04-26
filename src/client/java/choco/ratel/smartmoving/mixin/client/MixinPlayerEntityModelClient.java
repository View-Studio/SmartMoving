package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
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
        // BUG-7 (세션 36): flyingCreative 에 Config.enabled 가드 추가.
        //   기존: vanilla `player.getAbilities().flying` 만 → SM disabled 시에도 anySmState true →
        //   sm_animateFlying 호출 + reset 인프라 작동 → SM 비행 애니메이션 잔존 (= 토글 무용지물).
        //   해결: cfgEnabled && capabilities.flying 으로 변경 (SM enabled 일 때만 SM 비행 처리).
        boolean cfgEnabled = SmartMovingConfig.Config.enabled;
        boolean flyingCreative = cfgEnabled && player.getAbilities().flying;
        boolean anySmState = sm.isRopeSliding || sm.isClimbing || sm.isCrawlClimbing || sm.isCeilingClimbing
                || sm.isClimbJumping || sm.isSwimming_sm || sm.isDiving
                || sm.isCrawling || sm.isSliding || sm.isHeadJumping || flyingCreative;
        if (anySmState) {
            this.leaningPitch = 0f;
        }
        // pivot/yaw/roll reset 인프라 (B-9/B-11/B-13 부속): vanilla setAngles 는
        //   head/body pivotZ, body.yaw, head.roll 을 매 프레임 reset 하지 않는다
        //   (sneak 분기는 leg.pivotZ 만 변경 / body.yaw 는 animateArms 안
        //   handSwingProgress > 0 분기에서만 설정 / head.roll 은 vanilla 미사용).
        //   이전 sm 분기의 변경이 다음 분기까지 누적되는 위험을 막기 위해 SM 분기 진입
        //   직전 또는 SM disabled 전환 시 (BUG-7) vanilla 기본값(0) 으로 reset.
        //   anySmState 외 (!cfgEnabled) 도 포함: SM disabled 시 잔존 SM 변경 정리.
        if (anySmState || !cfgEnabled) {
            head.pivotZ = 0f;
            body.pivotZ = 0f;
            body.yaw    = 0f;
            head.roll   = 0f;
        }

        // ── cloak.pitch 처리 (cfgEnabled 분기) — disabled 시 0 reset 작동 보장 ──
        // BUG-7 (세션 36): cfgEnabled 무관 매 호출 적용. cfgEnabled true → SIXTYFOURTH /
        //   false → 0 reset (vanilla 미reset 필드 — disabled 진입 시 잔존 정리).
        // 위치: 아래 cfgEnabled return 가드 위에 두어 disabled 시에도 적용 보장.
        if ((Object) this instanceof PlayerEntityModel<?> playerModel) {
            ((PlayerEntityModelAccessor) playerModel).sm_getCloak().pitch = cfgEnabled ? SIXTYFOURTH : 0f;
        }

        // BUG-13/16 (세션 36): SM disabled 시 모든 SM 분기 + isFalling + isAngleJumping 한 번에 skip.
        //   기존: if-else 체인의 sm.* 분기는 sm.* 자체가 cfg.enabled 후만 true 라 안전했지만,
        //   else 분기 (isFalling) 는 vanilla 조건만 검사 → SM disabled 시에도 sm_animateFalling
        //   호출 (사용자 보고 = 낙하 모션 잔존). 또한 isAngleJumping 도 SmartMovingJumper
        //   잔존 상태 기반이므로 disabled 시에도 false 보장 어려움.
        //   해결: cfgEnabled false 시 진입점에서 즉시 return — 모든 SM 분기 skip + vanilla 정상.
        //   reset 인프라 + cloak.pitch 는 위에서 이미 처리되었으므로 잔존 정리는 보장.
        if (!cfgEnabled) return;

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
            sm_animateDiving(sm, limbSwing, limbSwingAmount);
        } else if (sm.isCrawling) {
            sm_animateCrawling(limbSwing, limbSwingAmount, headYaw);
        } else if (sm.isSliding) {
            sm_animateSliding(limbSwing, limbSwingAmount, headYaw);
        } else if (flyingCreative) {
            sm_animateFlying(sm, player, limbSwing, limbSwingAmount, animationProgress);
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

        // [B-16 / §16-24 / BUG-7] cloak.pitch 처리는 위로 이동 (BUG-13/16 cfgEnabled return 가드 위).
        //   원본 SmartRenderModel L251 = SM 상태 무관 항상 적용. cfgEnabled false 시에도 0 reset 보장.
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
        // B-4 / §16-10: 원본 SmartMovingModel L144 = `Math.min(0.5f, currentVerticalSpeed)` —
        //   verticalSpeed 입력은 수직 속도 (sm.stats.currentVerticalSpeed) 가 정합. 이전 구현은
        //   limbSwingAmount (수평 속도) 로 잘못 매핑. SmartStatistics.calculate L61 이 vanilla
        //   limbAnimator 와 동일 EMA 공식 (4× + 0.4 보간) 으로 currentVerticalSpeed 갱신 → 안전.
        float verticalSpeed = Math.min(0.5f, sm.stats.currentVerticalSpeed);
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
        // B-4 / §16-10: 원본 L200/L201 — arm.pitch cos 입력은 totalVerticalDistance (수직 누적).
        //   이전 limbSwing (수평 누적) 잘못 매핑 → sm.stats.totalVerticalDistance 로 교체.
        float rPitch = MathHelper.cos(sm.stats.totalVerticalDistance * 0.6662f + HALF) * verticalSpeed * handsDistUp + handsOffset;
        float lPitch = MathHelper.cos(sm.stats.totalVerticalDistance * 0.6662f)        * verticalSpeed * handsDistUp + handsOffset;
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
                // B-4 / §16-10: 원본 L219/L220 — feet.pitch cos 입력도 totalVerticalDistance (수직 누적).
                rightLeg.pitch = MathHelper.cos(sm.stats.totalVerticalDistance * 0.6662f)        * feetDistUp * verticalSpeed - 0.3f;
                leftLeg.pitch  = MathHelper.cos(sm.stats.totalVerticalDistance * 0.6662f + HALF) * feetDistUp * verticalSpeed - 0.3f;
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
        // B-5 / §16-12: 원본 L228/L232 cos 입력은 totalDistance (3D 누적). 이전 limbSwing
        //   (수평 누적) 잘못 매핑 → sm.stats.totalDistance 로 교체. SmartStatistics.calculate
        //   L91 = totalDistance 누적 1.7.10 등가.
        if (sm.isFeetVineClimbing) {
            float total = (MathHelper.cos(sm.stats.totalDistance + HALF) + 1f) * THIRTYTWOTH + SIXTEENTH;
            rightLeg.pitch = -total;   // pitch 덮어쓰기
            leftLeg.pitch  = -total;

            float diff = Math.max(0f, MathHelper.cos(sm.stats.totalDistance - QUARTER)) * SIXTYFOURTH;
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
            // bipedPelvic.X-=0.5F: 1.21.1 다리는 body 자식이 아니므로 leg 별도 처리 (B-19 / §16-26).
            // body.pivotZ = -6F: B-9 / §16-14 — 원본 bipedTorso 가 root(bipedOuter)의 자식으로 모든
            //   visual 노드를 자식으로 거느리므로 -6 = 전체 visual 이동. 1.21.1 단일 PlayerEntityModel
            //   에서는 body 단일 노드만 대응 (head/arm/leg 이동 누락 = SR 다층 부재 근사).
            if (sm.actualHandsClimbType < 2 && sm.actualFeetClimbType > 0) {
                body.pitch = 0.5f;
                head.pitch -= 0.5f;
                body.pivotZ = -6f;   // 원본 bipedTorso.rotationPointZ = -6F (B-9 / §16-14)
                // 원본 bipedPelvic.rotateAngleX -= 0.5F (L283) — pelvic 부재로 다리 그룹에 직접 차감
                rightLeg.pitch -= 0.5f;   // B-19 / §16-26
                leftLeg.pitch  -= 0.5f;
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
    private void sm_animateDiving(SmartMovingClientState sm, float limbSwing, float limbSwingAmount) {
        // B-6 / §16-13: 원본 SmartMovingModel L365-L367 = totalDistance (3D 누적) +
        //   currentSpeed (3D 속도). 이전 limbSwing/limbSwingAmount (수평) 잘못 매핑 →
        //   sm.stats.totalDistance/currentSpeed 로 교체. 다이빙은 수직+수평 운동 모두 강한
        //   상태 → 차이 가시화 가능.
        float distance    = sm.stats.totalDistance * 0.7f;
        float walkFactor  = smFactor(sm.stats.currentSpeed, 0f, 0.15679921f);
        float standFactor = smFactor(sm.stats.currentSpeed, 0.15679921f, 0f);

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
     * 원본: SmartMovingModel.setRotationAngles() 8번 분기 (isSlide, L437-L478).
     * bipedOuter X 기울기(Quarter) + outer.pivotY=+5F 는 sm_setupTransforms 에서 처리.
     * 원본 YZX 회전 순서 → setAnglesYZX 헬퍼로 정확하게 변환.
     * 머리/몸통 피벗 (B-13 / §16-19):
     *   - head.roll  = -headYaw * DEG_TO_RAD (원본 L442 -viewHorizontalAngelOffset/RadiantToAngle)
     *   - head.pivotZ = -2F (원본 L444)
     *   - body.pivotY = +6.5F (원본 L453, SR 다층 부재로 body 단일 노드 근사)
     *   - body.offsetY = -0.4F (원본 L452): ModelPart 에 offsetY 필드 부재로 sm_setupTransforms 의
     *     matrices.translate(0, -0.4F/16F, 0) 로 보정 (slide 분기 안에 통합)
     */
    private void sm_animateSliding(float limbSwing, float limbSwingAmount, float headYaw) {
        float distance   = limbSwing * 0.7f;
        float walkFactor = smFactor(limbSwingAmount, 0f, 1f) * 0.8f;

        head.pitch  = -EIGHTH - SIXTEENTH;
        head.roll   = -headYaw * DEG_TO_RAD;   // 원본 bipedHead.rotateAngleZ = -viewHorizontalAngelOffset/RadiantToAngle (B-13)
        head.pivotZ = -2f;                     // 원본 bipedHead.rotationPointZ = -2F (B-13)

        // 몸통 (YXZ 순서) — 원본 SmartMovingModel.java L672-L676:
        //   bipedBody.rotationOrder = YXZ
        //   bipedBody.rotateAngleX = cos(distance - Eighth) * Sixtyfourth * walkFactor
        //   bipedBody.rotateAngleY = cos(distance + Eighth) * Sixtyfourth * walkFactor
        // R-17: YXZ → GL call Z, X, Y → setAnglesYXZ 헬퍼.
        setAnglesYXZ(body,
                MathHelper.cos(distance - EIGHTH) * SIXTYFOURTH * walkFactor,
                MathHelper.cos(distance + EIGHTH) * SIXTYFOURTH * walkFactor,
                0f);
        body.pivotY = 6.5f;   // 원본 bipedBody.rotationPointY = +6.5F (B-13 / §16-19, SR 다층 부재로 body 단일 노드 근사)

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
    private void sm_animateFlying(SmartMovingClientState sm, ClientPlayerEntity player, float limbSwing, float limbSwingAmount, float totalTime) {
        // 🔴 (세션 63 revert 세션 62): pivot reset 제거.
        //   세션 62 의 body.yaw/arm.pivot reset 은 vanilla animateArms swing 효과 자체를 cancel
        //   → 사용자 보고 "비행 중 공격 시 애니메이션 아예 없음".
        //   사용자 요구: 비행 중 공격 시 vanilla swing 모션 그대로 적용 (원본 1:1).
        //   원본은 super.setAngles 호출 안 함 → vanilla swing 무시 였으나, 사용자가 vanilla swing
        //   효과 원함 → vanilla 처리 유지.
        //   sm_animateFlying 은 회전만 덮어씀 (XZY arm rotation, leg pitch/roll, head pitch).
        //   vanilla animateArms 의 pivot/body.yaw 변화는 그대로 잔존 → swing 효과 보임.

        // 🔴 (세션 52): partial tick lerp 적용 — 원본 SmartRenderRender.renderPlayer L56-L57:
        //   `totalDistance = statistics.getTotalDistance(renderPartialTicks)` —
        //   `currentSpeed = statistics.getCurrentSpeed(renderPartialTicks)` 매 프레임 lerped 값.
        //   이전 1.21.1 매핑은 sm.stats.totalDistance/currentSpeed 직접 사용 (lerp 안 함) →
        //   매 틱 띡 변경 → 팔/다리/모든 애니메이션 부드럽지 않음 = 사용자 보고 직접 원인.
        //   정정: setupTransforms 에서 cached tickDelta 사용 → lerped getter 호출.
        //   세션 52b: SmartMovingClientState.globalCachedTickDelta 로 이동 (Mixin private static 제약).
        float partialTicks = SmartMovingClientState.globalCachedTickDelta;

        // B-7 / §16-20: 원본 L477-L479 = totalDistance (3D 누적) + currentSpeed (3D 속도).
        //   원본 SmartRenderRender L56-L57: getTotalDistance/getCurrentSpeed(renderPartialTicks).
        float currentSpeedLerped = sm.stats.getCurrentSpeed(partialTicks);
        float distance    = sm.stats.getTotalDistance(partialTicks) * 0.08f;
        float walkFactor  = smFactor(currentSpeedLerped, 0f, 1f);
        float standFactor = smFactor(currentSpeedLerped, 1f, 0f);

        // 팔 (XZY 순서) — 원본 SmartMovingModel.java L696-L733:
        //   bipedRightArm.rotationOrder = XZY
        //   rotateAngleY = cos(time*0.15) * Sixteenth * standFactor   (정지 시 미세 흔들림)
        //   rotateAngleZ = (cos(distance + Half) * Sixtyfourth + Half - Sixteenth) * walkFactor + Quarter * standFactor   (날개짓)
        // R-17: XZY → GL call Y, Z, X → setAnglesXZY 헬퍼.
        //
        // 🔴 (세션 65b): 비행 중 공격 시 preferred arm 만 setAnglesXZY skip.
        //   사용자 요구: "비행 중에도 그냥 땅에서 오른팔 휘두르는 그 모션 그대로".
        //   vanilla setAngles TAIL 의 animateArms 가 preferred arm 에 적용한 full
        //   vanilla swing 모션 (기본 setAngles arm.pitch + animateArms 효과) 을 그대로
        //   잔존시키기 위해 setAnglesXZY 자체를 skip. 다른 팔만 날개짓 자세 적용.
        //   세션 65 의 += 재적용 방식은 setAnglesXZY 가 vanilla 기본 arm.pitch 를 0 reset
        //   한 후에 animateArms 효과만 += 하는 결과 → vanilla swing 의 절반만 보임.
        float swing = player.handSwingProgress;
        Arm preferredArm = player.getMainArm();
        boolean preserveRight = swing > 0F && preferredArm == Arm.RIGHT;
        boolean preserveLeft  = swing > 0F && preferredArm == Arm.LEFT;

        float rYaw  = MathHelper.cos(totalTime * 0.15f) * SIXTEENTH * standFactor;
        float lYaw  = MathHelper.cos(totalTime * 0.15f) * SIXTEENTH * standFactor;
        float rRoll = (MathHelper.cos(distance + HALF) * SIXTYFOURTH + HALF - SIXTEENTH) * walkFactor
                + QUARTER * standFactor;
        float lRoll = (MathHelper.cos(distance) * SIXTYFOURTH - HALF + SIXTEENTH) * walkFactor
                - QUARTER * standFactor;
        if (!preserveRight) setAnglesXZY(rightArm, 0f, rYaw, rRoll);
        if (!preserveLeft)  setAnglesXZY(leftArm,  0f, lYaw, lRoll);

        // 다리
        rightLeg.pitch = MathHelper.cos(distance) * SIXTYFOURTH * walkFactor
                + MathHelper.cos(totalTime * 0.15f + HALF) * SIXTYFOURTH * standFactor;
        leftLeg.pitch  = MathHelper.cos(distance + HALF) * SIXTYFOURTH * walkFactor
                + MathHelper.cos(totalTime * 0.15f) * SIXTYFOURTH * standFactor;
        rightLeg.roll  =  SIXTYFOURTH;
        leftLeg.roll   = -SIXTYFOURTH;

        // ANIM-01: head pitch 보정 — setupTransforms theta의 절반 역보정
        // 🔴 1:1 정정 (세션 47b): 원본 SmartMovingModel L481 isJump 분기 추가.
        // 🔴 (세션 52): currentSpeedLerped 사용 — 매 프레임 부드러운 변화.
        // 🔴 (세션 56 revert): 세션 54 의 fade lerped 매핑 revert. 원본 1:1 (raw target / 2).
        //   원본 SmartMovingModel L488: bipedHead.rotateAngleX = -bipedOuter.rotateAngleX / 2F
        //   (이때 bipedOuter.X = setRotationAngles 시점 raw target, fadeIntermediate 호출 전).
        //   = head.pitch = -raw_theta / 2f. 우리 매핑 정확.
        float verticalAngle = sm.isJumping
                ? Math.abs(sm.stats.currentVerticalAngle)
                : sm.stats.currentVerticalAngle;
        float theta = (QUARTER - verticalAngle) * currentSpeedLerped;
        head.pitch = -theta / 2f;

        // 🔴 (세션 60): 원본 reset() 효과 매핑 — bipedHead.Y/Z = 0 강제.
        //   원본 SmartRenderModel.setRotationAngles L166: `reset()` 호출 → 모든 ModelPart 의
        //   rotation = 0 reset. SmartMovingModel.isFlying 분기 (L484-L488) 가 bipedHead.X 만 set,
        //   bipedHead.Y/Z 는 reset 후 0 유지 → 머리 정면 (몸 정렬) 완전 고정.
        //   1.21.1 vanilla setAngles 가 매 프레임 head.yaw = (headYaw - bodyYaw) * π/180 set →
        //   우리 entity.bodyYaw = lerpedYaw 강제 후 head.yaw ≈ 0 이지만 미묘한 차이 (lerping 등).
        //   원본 reset 효과 1:1 매핑 = 명시적 0 강제.
        head.yaw  = 0f;
        head.roll = 0f;

        // 🔴 (세션 65b): swing 처리 = preferred arm 의 setAnglesXZY skip (위 팔 블록 참조).
        //   vanilla setAngles + animateArms 가 preferred arm 에 적용한 full vanilla swing
        //   모션이 그대로 보존됨. 추가 += 적용 불필요.
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
     * 원본 YXZ rotationOrder → ModelPart pitch/yaw/roll 변환.
     *
     * 🔴 BUG-30 정정 확장 (Flying Phase / 세션 42):
     *   원본 ModelRotationRenderer.rotate YXZ 분기 (L143/L146/L155) GL call 순서 = Z, X, Y.
     *   GL post-multiply 규칙: vertex 적용 순서 = call 의 역순 = Y, X, Z.
     *   JOML quaternion 곱 (q = qA * qB * qC) 의 vertex 적용 = right-most 부터 →
     *     vertex 적용 순서 Y, X, Z 매칭 = qZ * qX * qY.
     *
     *   이전 R-17 매핑 = qY * qX * qZ → vertex 적용 Z, X, Y (원본과 반대 순서).
     *   setAnglesXZY (세션 41) 에서 발견된 동일 패턴 오류 — YXZ/ZXY 헬퍼도 같은 reversal.
     *   사용처 (isSwim head, isSlide body) 에서 큰 pitch/roll 시 가시 차이 가능.
     *
     *   정정: qZ * qX * qY 순서로 변경 → 원본 vertex 적용 Y → X → Z 1:1 매칭.
     *
     * 사용처: isSwim head, isSlide body.
     */
    private static void setAnglesYXZ(ModelPart part, float pitch, float yaw, float roll) {
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
     * 원본 XZY rotationOrder → ModelPart pitch/yaw/roll 변환.
     *
     * 🔴 BUG-30 정정 (Flying Phase / 세션 41):
     *   원본 ModelRotationRenderer.rotate XZY 분기 (L148/L151/L157) GL call 순서 = Y, Z, X.
     *   GL post-multiply 규칙: vertex 적용 순서 = call 의 역순 = X, Z, Y.
     *   JOML quaternion 곱 (q = qA * qB * qC): vertex 가 right-most 부터 적용 →
     *     vec → qC → qB → qA. 즉 vertex 적용 순서 X, Z, Y 매칭 = qY * qZ * qX.
     *
     *   이전 R-17 매핑 = qX * qZ * qY → vertex 적용 Y, Z, X (원본과 반대 순서).
     *   small angle 시 차이 미세하지만 큰 roll (π/2 = 90°) 비행 자세에서 가시화 — 사용자
     *   보고 BUG-30: "팔 방향과 같은 축으로 스크류 회전" (= 원본 X 회전이 아닌 잘못된 회전).
     *
     *   정정: qY * qZ * qX 순서로 변경 → 원본 vertex 적용 X → Z → Y 1:1 매칭.
     *
     * 사용처: isFlying arm, isFalling arm.
     */
    private static void setAnglesXZY(ModelPart part, float pitch, float yaw, float roll) {
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
     * 원본 ZXY rotationOrder → ModelPart pitch/yaw/roll 변환.
     *
     * 🔴 BUG-30 정정 확장 (Flying Phase / 세션 42):
     *   원본 ModelRotationRenderer.rotate ZXY 분기 (L140/L146/L152) GL call 순서 = Y, X, Z.
     *   GL post-multiply 규칙: vertex 적용 순서 = call 의 역순 = Z, X, Y.
     *   JOML 의 vertex 적용 = right-most 부터 → vertex 적용 순서 Z, X, Y 매칭 = qY * qX * qZ.
     *
     *   이전 R-17 매핑 = qZ * qX * qY → vertex 적용 Y, X, Z (원본과 반대 순서).
     *   setAnglesXZY/YXZ 동일 패턴 오류 — animateAngleJumping 다리 작은 각도라 차이 미세.
     *
     *   정정: qY * qX * qZ 순서로 변경 → 원본 vertex 적용 Z → X → Y 1:1 매칭.
     *
     * 사용처: animateAngleJumping 다리.
     */
    private static void setAnglesZXY(ModelPart part, float pitch, float yaw, float roll) {
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
