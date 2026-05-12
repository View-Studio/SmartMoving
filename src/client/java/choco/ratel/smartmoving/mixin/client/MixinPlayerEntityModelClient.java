package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmModelPartOverride;
import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.client.SmartMovingRenderContext;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
    @Shadow public ModelPart hat;
    @Shadow public ModelPart body;
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightLeg;
    @Shadow public ModelPart leftLeg;

    /** BipedEntityModel.leaningPitch — animateModel()에서 entity.getLeaningPitch()로 세팅됨 */
    @Shadow protected float leaningPitch;

    // 🔴 (Phase 2 multi BUG-4) ModelPart pivot leak 차단 — single-instance 모델의 player 간 leak 방지.
    //   원인: PlayerEntityModel 은 모든 player render 가 공유하는 single instance.
    //   한 player 의 SM 분기 (= sm_animateCrawling 등) 가 head.pivotZ/body.pivotZ/leg.pivotX 등 변경 →
    //   vanilla setAngles 가 일부 field 만 매 호출 시 reset → 다음 player render 시 잔존 → A 의 자세가 B 에 leak.
    //   해결: 첫 setAngles HEAD 시 default pivot 캐시 → 매 setAngles HEAD 마다 caching 값 복원.
    //   vanilla setAngles 가 sneak 분기 등에서 pivot 추가 변경 → SM 분기가 추가 변경 → 정상 흐름.
    @Unique private boolean sm_pivotDefaultsCached = false;
    @Unique private float sm_headPivotX, sm_headPivotY, sm_headPivotZ;
    @Unique private float sm_bodyPivotX, sm_bodyPivotY, sm_bodyPivotZ;
    @Unique private float sm_rArmPivotX, sm_rArmPivotY, sm_rArmPivotZ;
    @Unique private float sm_lArmPivotX, sm_lArmPivotY, sm_lArmPivotZ;
    @Unique private float sm_rLegPivotX, sm_rLegPivotY, sm_rLegPivotZ;
    @Unique private float sm_lLegPivotX, sm_lLegPivotY, sm_lLegPivotZ;

    @Unique
    private void sm_cachePivotDefaultsIfNeeded() {
        if (sm_pivotDefaultsCached) return;
        sm_headPivotX = head.pivotX; sm_headPivotY = head.pivotY; sm_headPivotZ = head.pivotZ;
        sm_bodyPivotX = body.pivotX; sm_bodyPivotY = body.pivotY; sm_bodyPivotZ = body.pivotZ;
        sm_rArmPivotX = rightArm.pivotX; sm_rArmPivotY = rightArm.pivotY; sm_rArmPivotZ = rightArm.pivotZ;
        sm_lArmPivotX = leftArm.pivotX;  sm_lArmPivotY = leftArm.pivotY;  sm_lArmPivotZ = leftArm.pivotZ;
        sm_rLegPivotX = rightLeg.pivotX; sm_rLegPivotY = rightLeg.pivotY; sm_rLegPivotZ = rightLeg.pivotZ;
        sm_lLegPivotX = leftLeg.pivotX;  sm_lLegPivotY = leftLeg.pivotY;  sm_lLegPivotZ = leftLeg.pivotZ;
        sm_pivotDefaultsCached = true;
    }

    @Unique
    private void sm_restorePivotDefaults() {
        head.pivotX = sm_headPivotX; head.pivotY = sm_headPivotY; head.pivotZ = sm_headPivotZ;
        body.pivotX = sm_bodyPivotX; body.pivotY = sm_bodyPivotY; body.pivotZ = sm_bodyPivotZ;
        rightArm.pivotX = sm_rArmPivotX; rightArm.pivotY = sm_rArmPivotY; rightArm.pivotZ = sm_rArmPivotZ;
        leftArm.pivotX  = sm_lArmPivotX; leftArm.pivotY  = sm_lArmPivotY; leftArm.pivotZ  = sm_lArmPivotZ;
        rightLeg.pivotX = sm_rLegPivotX; rightLeg.pivotY = sm_rLegPivotY; rightLeg.pivotZ = sm_rLegPivotZ;
        leftLeg.pivotX  = sm_lLegPivotX; leftLeg.pivotY  = sm_lLegPivotY; leftLeg.pivotZ  = sm_lLegPivotZ;
        // vanilla setAngles 가 reset 안 하는 회전 field 도 복원 (= roll/yaw 일부) — leak 차단.
        head.roll = 0f;
        body.roll = 0f;
        rightArm.roll = 0f; leftArm.roll = 0f;
        rightLeg.roll = 0f; leftLeg.roll = 0f;
    }

    /**
     * BipedEntityModel.sneaking — PlayerEntityRenderer.setModelPose 가 매 프레임
     * `model.sneaking = entity.isInSneakingPose()` 로 set. setAngles 의 sneak 분기
     * (body.pitch=0.5, leg.pivotZ=4, head.pivotY=4.2, body.pivotY=3.2, arm.pivotY=5.2 등)
     * 가 이 필드를 본다.
     */
    @Shadow public boolean sneaking;

    /**
     * 낙하 중 vanilla sneak pose 차단을 위해 setAngles HEAD 진입 시 원본 sneaking 값을
     * 보관. TAIL 시작부에서 원복하여 layer renderer 등 외부 참조와의 일관성 유지.
     *
     * 원본 1.7.10 1:1 근거 — SmartMovingModel.animateSneaking (L681-685):
     *   `if(isStandard && !isAngleJumping) imp.superAnimateSneaking(...);`
     *   isFalling 분기 진입 시 isStandard=false → vanilla sneak super 호출 SKIP.
     *
     * 1.21.1 vanilla 는 setAngles 가 통합 메서드라 SKIP 불가 → sneaking 필드를 false 로
     * 임시 set 해 vanilla `if(this.sneaking)` 분기를 else 분기 (정상 standing 값) 로
     * 우회. 결과적으로 body.pitch=0, leg.pivotZ=0, leg.pivotY=12.0, head.pivotY=0,
     * body.pivotY=0, arm.pivotY=2.0F 가 적용되어 sneak 자세 잔존 cancel.
     */
    @Unique
    private boolean smOriginalSneakingForFalling;

    // ── [12-1] setAngles Mixin (HEAD — 낙하 중 vanilla sneak 분기 차단) ─────────
    //
    // 원본 1.7.10 SmartMovingModel.animateSneaking 가 isStandard=false 시 vanilla
    // superAnimateSneaking 호출을 skip 한 것과 1:1 등가. 1.21.1 에서는 setAngles 가
    // 단일 메서드라 직접 SKIP 불가하므로 sneaking 필드를 false 로 임시 set.
    //
    // 조건: cfgEnabled && !onGround && fallDistance > fallAnimationDistanceMinimum
    //       && !climbing 계열 && !touchingWater (= isFallingForReset 와 동일).
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",
            at = @At("HEAD"))
    private void sm_setAnglesHead(
            LivingEntity entity,
            float limbSwing, float limbSwingAmount,
            float animationProgress, float headYaw, float headPitch,
            CallbackInfo ci) {
        // 🔴 (BUG A fix, 2026-05-13) ModelPart override clear — firstPersonArmRender 가드 *전* 위치.
        //   PlayerEntityModel 은 single instance — 모든 player 공유. sm_animateSliding 이 직전 frame
        //   remote 의 rightArm/leftArm 에 override quat (= slide 자세 90° 회전) 적용했으면 그 인스턴스에
        //   잔존. 같은 frame self 1인칭 hand 렌더 시 model.setAngles 호출되지만 firstPersonArmRender
        //   가드로 return → clear 미실행 → 1인칭 손이 slide 자세로 그려져 *사라진 것처럼 보임*.
        //   해결: 가드 *전*으로 이동. 1인칭 hand 렌더 시도 매 frame clear → vanilla default 자세 유지.
        ((SmModelPartOverride)(Object) rightArm).sm_clearOverrideQuat();
        ((SmModelPartOverride)(Object) leftArm).sm_clearOverrideQuat();

        // 🔴 (2026-05-05 Phase 1-A) 다른 player render 시 SM 자세 적용 위해 가드 변경:
        //   AbstractClientPlayerEntity = local + remote player 모두 포함.
        if (!(entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity player)) return;

        // 1인칭 손 렌더 컨텍스트 (PlayerEntityRenderer.renderArm 진입 중) — SM 후킹 일체 skip.
        //   사용자 의도: 1인칭 시점의 손은 항상 vanilla 기본 자세 유지.
        //   다른 player 는 firstPersonArmRender 컨텍스트 미진입 → 영향 X (가드 그대로).
        if (SmartMovingRenderContext.firstPersonArmRender) return;

        // 🔴 (Phase 2 multi BUG-9 회귀 차단) sm_cachePivotDefaultsIfNeeded + sm_restorePivotDefaults 비활성.
        //   원인: 첫 setAngles 시점에 cache 한 default 가 sneak 자세 등 vanilla 변경값일 가능성 →
        //         이후 매 frame 잘못된 default 로 복원 → 슬라이딩 등 self 자세도 회귀.
        //   대안: 가드 제거된 reset chunk 패턴 (L301 / cleanup 분기) — BUG-8 재처리 항목.

        // 항상 원본 저장 (cfgEnabled=false 분기에서도 TAIL 원복 안전 보장)
        smOriginalSneakingForFalling = sneaking;

        // 🔴 (2026-05-05) self → Config.enabled / remote → 항상 true.
        if (!choco.ratel.smartmoving.client.SmartMovingClient.isSmRenderEnabled(player)) return;

        SmartMovingClientState sm = SmartMovingClientState.get(player);
        // 🔴 (Phase 2 multi BUG-1) fallDistance 직접 검사 → sm.doFallingAnimation 으로.
        //   vanilla 1.21.1 server 가 다른 player 의 fallDistance 를 sync 안 함 → remote 측 항상 0
        //   → sm_animateFalling 분기 미진입 → 사용자 보고 "낙하 애니메이션 안 보임".
        //   sm.doFallingAnimation 은 SmartMovingState bit 16 으로 server-relay sync (자기 client 매 tick
        //   계산 결과 송신). 정의 자체가 isFallingForReset 식 등가로 확장됨 (sendStatePacket 참조).
        boolean isFallingForReset = sm.doFallingAnimation;

        // 🔴 weeping/twisting vines 등반 + sneak 시 사다리 자세와 동일 매핑 (사용자 보고 마무리):
        //   사다리 등반 + sneak: sm_resetSneakInClimb (MixinClientPlayerEntity L90) 가 input.sneaking
        //     /isSneaking/pose 강제 false → vanilla setAngles sneak 분기 skip → sneak 자세 X.
        //   weeping/twisting vines: sm_resetSneakInClimb 미트리거 (sm.isClimbing=false) →
        //     vanilla sneak 자세 + 사다리 자세 결합 시각.
        //   해결: 자세만 분리 — vanilla setAngles 의 sneak 분기만 skip 위해 sneaking=false 임시 set.
        //     motion 은 vanilla 그대로 (사용자 의도 — 기능 안 건드림).
        Block blockAtPos = player.getWorld().getBlockState(player.getBlockPos()).getBlock();
        boolean isWeepingTwistingVines = (blockAtPos == Blocks.WEEPING_VINES
                || blockAtPos == Blocks.WEEPING_VINES_PLANT
                || blockAtPos == Blocks.TWISTING_VINES
                || blockAtPos == Blocks.TWISTING_VINES_PLANT);

        if (isFallingForReset || isWeepingTwistingVines) {
            sneaking = false;
        }
    }

    // ── [12-1] setAngles Mixin (TAIL — vanilla 애니메이션 완료 후 SM이 덮어씀) ──

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",
            at = @At("TAIL"))
    private void sm_setAngles(
            LivingEntity entity,
            float limbSwing, float limbSwingAmount,
            float animationProgress, float headYaw, float headPitch,
            CallbackInfo ci) {
        // 🔴 (2026-05-05 Phase 1-A) AbstractClientPlayerEntity = local + remote.
        //   다른 player render 시 SM 자세 적용을 위해 가드 확장.
        if (!(entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity player)) return;

        // 🔴 (Phase 2 multi BUG-13) firstPersonArmRender 가드를 reset chunk 후로 이동.
        //   기존: 진입 즉시 return → reset chunk skip → 다른 player 의 ModelPart 변경
        //         (arm.roll, scales, leg.roll 등) 잔존 → 본인 1인칭 손에 leak.
        //   변경: reset chunk 적용 후 SM 분기 진입 직전 가드 → 1인칭 손 render 시도 reset 적용 →
        //         ModelPart 가 default 로 → 본인 1인칭 손 정상 자세.

        // 🔴 sneaking 원복 — HEAD 에서 임시 false 로 set 한 값을 vanilla setAngles 종료 직후
        //   복구. 같은 프레임 내 layer renderer (cape/armor) 가 copyBipedStateTo 등으로
        //   sneaking 을 참조해도 entity 의 실제 pose 와 일관 유지.
        //   (조건 무관 무조건 set — HEAD 에서 항상 원본 저장하므로 안전.)
        sneaking = smOriginalSneakingForFalling;

        SmartMovingClientState sm = SmartMovingClientState.get(player);

        // ── [8-3][6-4] SM 활성 상태에서 leaningPitch 강제 0 ─────────────────
        // leaningPitch > 0이면 setupTransforms Branch 2(-90° X회전)와
        // setAngles Step 13(수영 팔 애니메이션)이 활성화된다. SM 상태에서는 억제.
        // BUG-7 (세션 36): flyingCreative 에 Config.enabled 가드 추가.
        //   기존: vanilla `player.getAbilities().flying` 만 → SM disabled 시에도 anySmState true →
        //   sm_animateFlying 호출 + reset 인프라 작동 → SM 비행 애니메이션 잔존 (= 토글 무용지물).
        //   해결: cfgEnabled && capabilities.flying 으로 변경 (SM enabled 일 때만 SM 비행 처리).
        // 🔴 (2026-05-05) self → Config.enabled / remote → 항상 true.
        boolean cfgEnabled = choco.ratel.smartmoving.client.SmartMovingClient.isSmRenderEnabled(player);
        // 🔴 (Phase 1-A) 다른 player 도 비행 자세 적용 위해 sm.isFlying 사용.
        //   이전 `player.getAbilities().flying` 은 Mojang datatracker 가 다른 player 에 미동기화
        //   → 다른 player 항상 false → sm_animateFlying 미호출 BUG. sm.isFlying 은
        //   server-relay sync 정상 (= local + remote 모두).
        boolean flyingCreative = cfgEnabled && sm.isFlying;
        // 🔴 (2026-04-27): falling 도 anySmState 에 포함 — vanilla animateArms 가 swing 시
        //   body.yaw 흔들리는 효과 (사용자 보고 "공중 낙하 휘두름 시 몸통 움찔움찔") 를 reset
        //   인프라로 cancel. 진입 조건은 if-else 체인 falling 가드 (아래) 와 동일.
        // 🔴 (Phase 2 multi BUG-1) fallDistance 직접 검사 → sm.doFallingAnimation (server-relay sync).
        boolean isFallingForReset = cfgEnabled && sm.doFallingAnimation;
        // 🔴 늘어진/휘어진 덩굴 (weeping_vines / twisting_vines) 시 사다리 자세 애니메이션 적용.
        //   사용자 의도: SM 자유 클라이밍 / 자동 진입 / sneak hold 등 기능 일체 적용 안 함.
        //   vanilla 1.21.1 사다리 등반 동작 그대로 (sm_travel_client 의 vanilla bypass 유지).
        //   애니메이션만 sm_animateClimbing (isHandsVineClimbing=false → 사다리 자세) 호출.
        //   매 frame block 검사 (player 위치).
        boolean isWeepingTwistingVines = false;
        if (cfgEnabled) {
            Block blockAtPos = player.getWorld().getBlockState(player.getBlockPos()).getBlock();
            isWeepingTwistingVines = (blockAtPos == Blocks.WEEPING_VINES
                    || blockAtPos == Blocks.WEEPING_VINES_PLANT
                    || blockAtPos == Blocks.TWISTING_VINES
                    || blockAtPos == Blocks.TWISTING_VINES_PLANT);
            // 🔴 사용자 보고 fix (2026-05-04 — "비행 중 vine 애니메이션 안 나오게"):
            //   비행 중 vine 자세 적용 X → 비행 자세 (= isFlying 분기) 진입.
            if (isWeepingTwistingVines && sm.isFlying) {
                isWeepingTwistingVines = false;
            }
        }

        boolean anySmState = sm.isRopeSliding || sm.isClimbing || sm.isCrawlClimbing || sm.isCeilingClimbing
                || sm.isClimbJumping || sm.isSwimming_sm || sm.isDiving
                || sm.isCrawling || sm.isSliding || sm.isHeadJumping || flyingCreative
                || isFallingForReset
                || isWeepingTwistingVines
                || sm.isAngleJumping();   // 🔴 (2026-04-27) angle jump 시 reset 인프라 활성화 —
                                          // head.pivotZ/body.yaw/head.roll/arm.pivot 0 reset →
                                          // 이전 SM 분기 잔존 + vanilla animateArms swing body.yaw 흔들림 cancel.
        if (anySmState) {
            this.leaningPitch = 0f;
        }

        // 🔴 D-4 종료 엣지 cleanup (사용자 보고 — 등반 종료 후 standing 시 몸통 앞으로 빠진 잔존):
        //   D-4 분기 진입 시 set 한 모든 노드 pivotZ=-6 가 등반 종료 시 reset 안 됨.
        //   이전 frame climbing 상태 추적 → 종료 엣지 (true → false) 한 번 명시 cleanup.
        //   leg.pivotX 도 sm_animateClimbing 진입부에서 ±2.0 로 set 됐으므로 vanilla default
        //   ±1.9 로 복원 (1.21.1 PlayerEntityModel layer definition 의 정의 값).
        // 🔴 weeping/twisting vines 도 cleanup 추적 — 종료 시 leg.pivotX/Y / pivotZ vanilla 복원.
        boolean smIsClimbingNow = sm.isClimbing || sm.isCrawlClimbing || sm.isCeilingClimbing
                || isWeepingTwistingVines;
        if (sm.smWasClimbingForCleanup && !smIsClimbingNow) {
            body.pivotZ     = 0f;
            head.pivotZ     = 0f;
            rightArm.pivotZ = 0f;
            leftArm.pivotZ  = 0f;
            rightLeg.pivotZ = 0f;
            leftLeg.pivotZ  = 0f;
            rightLeg.pivotX = -1.9f;
            leftLeg.pivotX  =  1.9f;
            // D-4 leg.pivotY 정정 매핑 (12*cos(0.5)≈10.529) → vanilla default 12 reset.
            rightLeg.pivotY = 12f;
            leftLeg.pivotY  = 12f;
            // 🔴 사용자 보고 fix (2026-05-04 — "사다리/덩굴 sneak+s 후 다시 붙으면 가끔 팔 짧아짐"):
            //   sm_animateClimbing 의 isHandsVineClimbing 분기 (L591) = setArmScales(abs(cos), abs(cos)) →
            //   arm yScale 변경. vine 종료 시 vanilla setAngles 가 yScale reset 안 함 → 잔존.
            //   isFeetVineClimbing 분기 (L631) 의 setLegScales 도 동일 패턴.
            //   fix: 클라이밍 종료 엣지 cleanup 에 scale reset 추가 (= 1.0 default 복원).
            setArmScales(rightArm, leftArm, 1f, 1f);
            setLegScales(rightLeg, leftLeg, 1f, 1f);
        }
        sm.smWasClimbingForCleanup = smIsClimbingNow;

        // 🔴 isCrawling 종료 cleanup (사용자 보고 fix — "엎드린 상태에서 벗어났을 때 잔존", 2026-05-03):
        //   sm_animateCrawling 가 set 한 vanilla 비-reset 값들 (head.pivotZ, head.roll, body.yaw/roll,
        //   leg.roll, arm.yaw/roll, scales) 종료 엣지 한 번 vanilla default reset.
        //   메모리 `feedback_animation_porting.md` 의 잘못된 패턴 (pivotY/leg.pivotZ 무조건 reset)
        //   회피 — sneaking 시 vanilla 가 set 하는 값 (pivotY 3.2 등) 안 건드림.
        //   D-4 cleanup 패턴 그대로 따름 (= 종료 엣지 한 번만 적용).
        if (sm.smWasCrawlingForCleanup && !sm.isCrawling) {
            head.pivotZ = 0f;
            head.roll   = 0f;
            body.yaw    = 0f;
            body.roll   = 0f;
            rightLeg.roll = 0f;
            leftLeg.roll  = 0f;
            rightArm.yaw  = 0f;
            leftArm.yaw   = 0f;
            rightArm.roll = 0f;
            leftArm.roll  = 0f;
            // scales = 1.0 (default) 복원 — sm_animateCrawling 의 setLegScales/setArmScales cancel
            setLegScales(rightLeg, leftLeg, 1f, 1f);
            setArmScales(rightArm, leftArm, 1f, 1f);
        }
        sm.smWasCrawlingForCleanup = sm.isCrawling;
        // pivot/yaw/roll reset 인프라 (B-9/B-11/B-13 부속): vanilla setAngles 는
        //   head/body pivotZ, body.yaw, head.roll 을 매 프레임 reset 하지 않는다
        //   (sneak 분기는 leg.pivotZ 만 변경 / body.yaw 는 animateArms 안
        //   handSwingProgress > 0 분기에서만 설정 / head.roll 은 vanilla 미사용).
        //   이전 sm 분기의 변경이 다음 분기까지 누적되는 위험을 막기 위해 SM 분기 진입
        //   직전 또는 SM disabled 전환 시 (BUG-7) vanilla 기본값(0) 으로 reset.
        //   anySmState 외 (!cfgEnabled) 도 포함: SM disabled 시 잔존 SM 변경 정리.
        // 🔴 (Phase 2 multi BUG-8) anySmState/cfgEnabled 가드 제거 — 매 frame 무조건 reset.
        //   원인: PlayerEntityModel single instance — A 의 SM 분기에서 변경된 ModelPart field 가
        //         B 의 setAngles 진입 시 잔존 → 사용자 보고 "a 가 엎드릴 때 b 도 같은 자세".
        //         가드 (anySmState || !cfgEnabled) 가 self only 검증 → multi 환경 leak.
        //   해결: 가드 제거. SM 분기 진입 안 한 player 의 setAngles 도 매번 reset → leak 차단.
        //         vanilla standing 자세에서도 head.pivotZ=0/body.pivotZ=0/body.yaw=0/head.roll=0 default 유지 →
        //         vanilla 영향 없음. arm.pivot 기본값 ±5/0 도 vanilla setAngles Step 4 의 default.
        head.pivotZ = 0f;
        body.pivotZ = 0f;
        body.yaw    = 0f;
        head.roll   = 0f;
        leftArm.pivotX  =  5f;
        leftArm.pivotZ  =  0f;
        rightArm.pivotX = -5f;
        rightArm.pivotZ =  0f;
        // 🔴 (Phase 2 multi BUG-8 추가) sm_animateCrawling 의 set field 추가 reset:
        //   sm_animateCrawling L1051-1052/L1062-1063/L1068-1073 가 leg.roll/arm.roll/scales 변경 →
        //   vanilla setAngles 매 호출 시 reset 안 함 → 다른 player render 시 잔존 → 자세 leak.
        //   매 frame default (0/1.0) 강제 → SM 분기 진입 시 sm_animateXxx 이 다시 set → 정상 흐름.
        //   ※ arm.yaw 는 vanilla animateArms swing 시 set 하므로 reset 안 함 (= swing 효과 보존).
        //   ※ self 도 reset 적용 — 다른 player 의 SM 자세 영향이 self render 에 leak 차단.
        rightLeg.roll = 0f;
        leftLeg.roll  = 0f;
        rightArm.roll = 0f;
        leftArm.roll  = 0f;
        setLegScales(rightLeg, leftLeg, 1f, 1f);
        setArmScales(rightArm, leftArm, 1f, 1f);

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

        // 🔴 (Phase 2 multi BUG-13) 1인칭 손 render 시 SM 분기 skip — reset chunk 는 위에서 이미 적용.
        //   다른 player setAngles 결과 (= ModelPart arm.roll/scales/leg.roll 등) 잔존이 본인 1인칭 손에 leak →
        //   reset chunk 만 적용하고 SM 분기 진입 안 함 → ModelPart default → 본인 1인칭 손 정상.
        if (SmartMovingRenderContext.firstPersonArmRender) return;

        // ── [12-7] smallOverGroundHeight 계산 ─────────────────────────────────
        // 원본: SmartMovingRender.rotatePlayer() → moving.getOverGroundHeight(5D)
        // isCrawlClimbing/isHeadJumping 상태에서만 발 아래 지면까지의 거리를 계산한다.
        if (sm.isCrawlClimbing || sm.isHeadJumping) {
            // 🔴 (Phase 2 multi BUG-6) ClientPlayerEntity 가드 제거 — 모든 player 자체 계산.
            //   computeSmallOverGroundHeight = entity 위치 + world block scan → server-sync entity.y +
            //   client-side world 로 remote 도 정확 계산 가능.
            //   기존: self only → remote 의 smallOverGroundHeight=0 → height=0.25 → bodyAngleX 큰 값 →
            //         isCrawlClimbing 자세 시각상 잘못 (사용자 보고 "엎드려서 앞으로 가는 애니메이션만").
            sm.smallOverGroundHeight = computeSmallOverGroundHeight(player, player.getWorld());
        }

        // ── SM 11-state if-else 체인 (SmartMovingModel.setRotationAngles 우선순위) ──
        // 🔴 사용자 보고 후 (천장 등반): isCeilingClimbing 을 isClimbing 보다 먼저 검사.
        //   원본 SmartMovingModel L127-L316 은 isClimb 우선이지만 isClimb && isCeilingClimb
        //   둘 다 true 발생 안 함을 가정. 1.21.1 측은 천장 등반 시 sm.isClimbing 도 true 로
        //   매핑되어 둘 다 true 발생 → isClimbing 분기 진입 → 일반 클라이밍 식 적용 → 천장
        //   등반인데 팔/다리 가만히. 우선순위 변경으로 isCeilingClimbing 시 sm_animateCeilingClimbing
        //   호출 보장.
        if (sm.isRopeSliding) {
            sm_animateRopeSliding(animationProgress, player);
        } else if (sm.isCeilingClimbing) {
            sm_animateCeilingClimbing(sm, headYaw);
        } else if (sm.isClimbing || sm.isCrawlClimbing) {
            sm_animateClimbing(sm, limbSwing, limbSwingAmount, headPitch, animationProgress);
        } else if (isWeepingTwistingVines) {
            // 🔴 늘어진/휘어진 덩굴 — 사다리 자세 애니메이션 적용.
            //   사용자 의도: 기능은 vanilla 등반 그대로 (sm_travel_client bypass 유지). 애니메이션만 SM 사다리.
            //   sm.actualHandsClimbType / actualFeetClimbType 임시 1/1 (UpGrab+DownStep) set 후
            //   sm_animateClimbing 호출 → 호출 후 원래 값 복원 (다른 코드 영향 0).
            //   handleClimbing 호출 안 됨 → 매 tick 잔존값 = NoGrab+NoStep (NONE.ordinal()=0).
            //   임시 1/1 set 으로 사다리 식 (cos*0.3-0.3 leg pitch 등) 적용. isHandsVineClimbing=false
            //   유지 → vine 분기 (arm scale boost / leg total 식) 안 들어감 = 사다리 자세.
            int origHandsType = sm.actualHandsClimbType;
            int origFeetType  = sm.actualFeetClimbType;
            sm.actualHandsClimbType = 1;  // UpGrab
            sm.actualFeetClimbType  = 1;  // DownStep
            sm_animateClimbing(sm, limbSwing, limbSwingAmount, headPitch, animationProgress);
            sm.actualHandsClimbType = origHandsType;
            sm.actualFeetClimbType  = origFeetType;
        } else if (sm.isClimbJumping) {
            // [isClimbJump] 클라이밍 점프 — 팔을 위로 뻗은 자세
            rightArm.pitch = HALF + SIXTEENTH;
            leftArm.pitch  = HALF + SIXTEENTH;
            rightArm.roll  = -THIRTYTWOTH;
            leftArm.roll   =  THIRTYTWOTH;
        } else if (sm.isSwimming_sm) {
            sm_animateSwimming(sm, limbSwing, limbSwingAmount, animationProgress);
        } else if (sm.isDiving) {
            sm_animateDiving(sm, limbSwing, limbSwingAmount);
        } else if (sm.isCrawling) {
            sm_animateCrawling(sm, headYaw);
        } else if (sm.isSliding) {
            sm_animateSliding(sm, headYaw);
        } else if (flyingCreative) {
            sm_animateFlying(sm, player, limbSwing, limbSwingAmount, animationProgress);
        } else if (sm.isHeadJumping) {
            sm_animateHeadJumping(sm, player);
        } else {
            // 🔴 (Phase 2 multi BUG-1) fallDistance 직접 검사 → sm.doFallingAnimation (server-relay sync).
            //   vanilla 1.21.1 server 가 remote 의 fallDistance 를 미동기 → 사용자 보고 "낙하 애니 안 보임".
            //   doFallingAnimation 정의 자체가 isFalling 식 등가로 확장됨 (sendStatePacket 참조).
            if (sm.doFallingAnimation) {
                sm_animateFalling(sm, player);
            }
            // 🔴 (2026-04-28) isStandard 분기 swing 식 덮어쓰기 제거.
            //   vanilla setAngles 의 sneak/idle 진동/heldItem 등 += 누적 효과 보존.
            //   limbSwing/limbSwingAmount 인자만 ModifyArg 로 SM stat 으로 변경 (MixinLivingEntityRenderer).
        }

        // ── [12-2] animateAngleJumping — 방향 점프 팔/다리 포즈 ──────────────
        if (sm.isAngleJumping()) {
            sm_animateAngleJumping(sm, animationProgress);
        }

        // 🔴 (2026-04-27) head 는 vanilla 그대로 — sm_setupTransforms TAIL 의 fade 가
        //   모든 ModelPart 부모 변환 영향. head 만 cancel 안 함 → vanilla netHeadYaw 그대로
        //   (사용자 의도 "머리만 바닐라 코드"). 즉 head 도 fade lag 따라 회전 (의도된 동작).

        // [B-16 / §16-24 / BUG-7] cloak.pitch 처리는 위로 이동 (BUG-13/16 cfgEnabled return 가드 위).
        //   원본 SmartRenderModel L251 = SM 상태 무관 항상 적용. cfgEnabled false 시에도 0 reset 보장.

        // ── outer layer 재동기화 (hat / jacket / sleeves / pants) ──────────────
        // 1.21.1 PlayerEntityModel 의 outer layer 6개는 root 의 직접 자식 — 부모
        //   transform 자동 상속 X. vanilla 는 super.setAngles 의 마지막에서 한 번만
        //   copyTransform 으로 동기화 (BipedEntityModel 끝의 hat ← head, PlayerEntityModel
        //   super 직후의 jacket/sleeves/pants ← body/arms/legs).
        // SM 의 sm_setAngles 는 @At("TAIL") 진입 → super 동기화가 끝난 뒤 head/body/arm/leg
        //   를 광범위 변경 → outer layer 가 super 종료 시점의 vanilla 값으로 정지 → 분리.
        // 해결: SM 의 모든 변경을 마친 직후 outer layer 6개 동기화를 다시 한번 실행.
        //   cfgEnabled=false 분기는 위에서 return 했으므로 여기는 SM 활성 분기 한정.
        hat.copyTransform(head);
        if ((Object) this instanceof PlayerEntityModel<?> playerModel) {
            playerModel.leftPants.copyTransform(leftLeg);
            playerModel.rightPants.copyTransform(rightLeg);
            playerModel.leftSleeve.copyTransform(leftArm);
            playerModel.rightSleeve.copyTransform(rightArm);
            playerModel.jacket.copyTransform(body);
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
    private void sm_animateRopeSliding(float animationProgress, net.minecraft.client.network.AbstractClientPlayerEntity player) {
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
    private void sm_animateClimbing(SmartMovingClientState sm, float limbSwing, float limbSwingAmount, float headPitch, float animationProgress) {
        // B-4 / §16-10 + 부드러움 1:1 (사용자 보고 후, 그랩 클라이밍 진자운동 부드러움 정정):
        //   원본 SmartRenderModel 은 매 frame `getCurrentSpeed(partialTicks)` lerp getter 로
        //   prev/current EMA 사이를 partial tick 비율로 보간 = 60Hz 부드러움.
        //   이전 매핑 `sm.stats.currentVerticalSpeed` 직접 참조 = 매 tick 갱신값 = 20Hz 띡띡.
        //   비행 (sm_animateFlying) 와 동일 패턴 적용 — getCurrentVerticalSpeed/getTotalVerticalDistance
        //   lerp getter 로 통일.
        float partialTicks = SmartMovingClientState.globalCachedTickDelta;
        float verticalSpeed   = Math.min(0.5f, sm.stats.getCurrentVerticalSpeed(partialTicks));
        float horizontalSpeed = Math.min(0.5f, sm.stats.getCurrentHorizontalSpeed(partialTicks));
        float totalVerticalDistance   = sm.stats.getTotalVerticalDistance(partialTicks);
        float totalHorizontalDistance = sm.stats.getTotalHorizontalDistance(partialTicks);

        // leg.pivotZ reset — D-4 분기 빠진 frame 에서 -6 누적 차단.
        //   (head/body/arm pivotZ 는 sm_setAngles TAIL 의 reset 인프라가 0 으로 reset.)
        rightLeg.pivotZ = 0f;
        leftLeg.pivotZ  = 0f;

        // 🔴 사용자 보고 fix (2026-05-04 — "사다리/덩굴 sneak+s 후 다시 붙으면 가끔 팔 짧아짐"):
        //   isHandsVineClimbing/isFeetVineClimbing 분기 (L591/L631) 의 setArmScales/setLegScales 가
        //   abs(cos) 식 적용. 분기 미진입 frame (= ladder grab + vine 미접촉 등) 시 호출 X →
        //   이전 frame 변경값 잔존 → 시각적 팔/다리 짧음.
        //   fix: 매 frame default 1.0 reset → 분기 진입 시 cos 식이 덮어쓰기. 미진입 시 1.0 유지.
        setArmScales(rightArm, leftArm, 1f, 1f);
        setLegScales(rightLeg, leftLeg, 1f, 1f);

        // 🔴 다리 pivotX 1:1 매핑 (사용자 보고 9회차 — 다리 디테일 차이 정밀 분석):
        //   원본 SmartRenderModel.java L80-L87:
        //     bipedRightLeg.setRotationPoint(-2F, 0.0F, 0.0F);  // 부모 bipedPelvic (0,12,0)
        //     bipedLeftLeg.setRotationPoint(2.0F, 0.0F, 0.0F);
        //     → 절대 leg pivot = (±2.0, 12, 0)
        //   1.21.1 vanilla PlayerEntityModel layer definition:
        //     right_leg.pivot(-1.9F, 12.0F, 0.0F)
        //     left_leg.pivot(1.9F, 12.0F, 0.0F)
        //     → 절대 leg pivot = (±1.9, 12, 0)
        //   차이 0.1 픽셀 — 사용자 시각 미세 영향 가능. 원본 1:1 매핑 위해 ±2.0 명시 set.
        //   (sm_setAngles TAIL 의 종료 엣지 cleanup 에서 vanilla default ±1.9 로 reset.)
        rightLeg.pivotX = -2f;
        leftLeg.pivotX  =  2f;

        // 🔴 (사용자 보고 8회차 — 기본 블록 그랩 클라이밍 완전 1:1 매핑):
        //   원본 SmartMovingModel.setRotationAngles L127-L237 의 isClimb 분기를 라인별 1:1 풀어씀.
        //   변수명 / 식 / 분기 모두 원본 그대로 (수학적 단순화 제거).

        // 원본 L131-L132: 머리 yaw=0, pitch=mouse.
        head.yaw   = 0f;
        head.pitch = headPitch * DEG_TO_RAD;

        // 원본 L137-L138: hands/feet Frequence/Distance Up/Side Factor/Offset 변수 선언.
        float handsFrequenceUpFactor, handsDistanceUpFactor, handsDistanceUpOffset;
        float feetFrequenceUpFactor, feetDistanceUpFactor, feetDistanceUpOffset;
        float handsFrequenceSideFactor, handsDistanceSideFactor, handsDistanceSideOffset;
        float feetFrequenceSideFactor, feetDistanceSideFactor, feetDistanceSideOffset;

        // 원본 L140-L142: handsClimbType + vine MiddleGrab → UpGrab 변환.
        //   1.21.1 도메인: 0=NoGrab, 1=UpGrab, 2=MiddleGrab (handleClimbing 분기별 set 매핑).
        int handsClimbType = sm.actualHandsClimbType;
        if (sm.isHandsVineClimbing && handsClimbType == 2 /* MiddleGrab */) {
            handsClimbType = 1 /* UpGrab */;
        }

        // 원본 L144-L145: speed clamp (Math.min(0.5, currentVerticalSpeed/currentHorizontalSpeed)).
        //   1.21.1: partial tick lerp getter (60Hz 부드러움 — 원본 SmartRenderRender.renderPlayer
        //     L56-L57 의 getCurrentSpeed(renderPartialTicks) 등가).
        // (위에서 이미 verticalSpeed/horizontalSpeed/totalVerticalDistance/totalHorizontalDistance 정의됨.)

        // 원본 L147-L176: handsClimbType switch — 3-way 분기.
        final float FrequenceFactor = 0.6662f;  // 원본 SmartMovingModel.FrequenceFactor.
        switch (handsClimbType) {
            case 2:  // MiddleGrab — 원본 L149-L156
                handsFrequenceSideFactor = FrequenceFactor;
                handsDistanceSideFactor  = 1.0f;
                handsDistanceSideOffset  = 0.0f;
                handsFrequenceUpFactor   = FrequenceFactor;
                handsDistanceUpFactor    = 2f;
                handsDistanceUpOffset    = -QUARTER;
                break;
            case 1:  // UpGrab — 원본 L158-L165
                handsFrequenceSideFactor = FrequenceFactor;
                handsDistanceSideFactor  = 1.0f;
                handsDistanceSideOffset  = 0.0f;
                handsFrequenceUpFactor   = FrequenceFactor;
                handsDistanceUpFactor    = 2f;
                handsDistanceUpOffset    = -2.5f;
                break;
            default: // NoGrab (0) — 원본 L167-L175
                handsFrequenceSideFactor = FrequenceFactor;
                handsDistanceSideFactor  = 1.0f;
                handsDistanceSideOffset  = 0.0f;
                handsFrequenceUpFactor   = FrequenceFactor;
                handsDistanceUpFactor    = 0f;
                handsDistanceUpOffset    = -0.5f;
                break;
        }

        // 원본 L178-L198: feetClimbType switch — 2-way 분기 (UpGrab vs default).
        //   1.21.1 도메인: 0=NoStep, 1=DownStep (handleClimbing 분기별 set).
        //   원본 case `HandsClimbing.UpGrab` (=1) ≡ 1.21.1 DownStep (=1).
        int feetClimbType = sm.actualFeetClimbType;
        switch (feetClimbType) {
            case 1:  // UpGrab/DownStep — 원본 L180-L187
                feetFrequenceUpFactor   = FrequenceFactor;
                feetDistanceUpFactor    = 0.3f / verticalSpeed;  // 원본 L182 그대로 (verticalSpeed=0 시 NaN 동작도 1:1)
                feetDistanceUpOffset    = -0.3f;
                feetFrequenceSideFactor = FrequenceFactor;
                feetDistanceSideFactor  = 0.5f;
                feetDistanceSideOffset  = 0.0f;
                break;
            default: // NoStep — 원본 L189-L196
                feetFrequenceUpFactor   = FrequenceFactor;
                feetDistanceUpFactor    = 0.0f;
                feetDistanceUpOffset    = 0.0f;
                feetFrequenceSideFactor = FrequenceFactor;
                feetDistanceSideFactor  = 0.0f;
                feetDistanceSideOffset  = 0.0f;
                break;
        }

        // 원본 L200-L204: 팔 X(pitch)/Y(yaw) — 변수 풀어쓰기.
        float rArmPitch = MathHelper.cos(totalVerticalDistance * handsFrequenceUpFactor + HALF) * verticalSpeed * handsDistanceUpFactor + handsDistanceUpOffset;
        float lArmPitch = MathHelper.cos(totalVerticalDistance * handsFrequenceUpFactor)        * verticalSpeed * handsDistanceUpFactor + handsDistanceUpOffset;
        float rArmYaw   = MathHelper.cos(totalHorizontalDistance * handsFrequenceSideFactor + QUARTER) * horizontalSpeed * handsDistanceSideFactor + handsDistanceSideOffset;
        float lArmYaw   = MathHelper.cos(totalHorizontalDistance * handsFrequenceSideFactor)            * horizontalSpeed * handsDistanceSideFactor + handsDistanceSideOffset;
        setAnglesYZX(rightArm, rArmPitch, rArmYaw, 0f);
        setAnglesYZX(leftArm,  lArmPitch, lArmYaw, 0f);

        // 원본 L206-L215: isHandsVineClimbing 추가 보정.
        if (sm.isHandsVineClimbing) {
            // 원본 L208-L209: arm.Y *= 1F + handsFrequenceSideFactor.
            leftArm.yaw  *= 1f + handsFrequenceSideFactor;
            rightArm.yaw *= 1f + handsFrequenceSideFactor;
            // 원본 L211-L212: leftArm.Y += Eighth, rightArm.Y -= Eighth.
            leftArm.yaw  += EIGHTH;
            rightArm.yaw -= EIGHTH;
            // 원본 L214: setArmScales(abs(cos(rightArm.X)), abs(cos(leftArm.X))).
            setArmScales(rightArm, leftArm,
                    Math.abs(MathHelper.cos(rightArm.pitch)),
                    Math.abs(MathHelper.cos(leftArm.pitch)));
        }

        // 원본 L217-L221: 다리 X(pitch) — `if(!isFeetVineClimbing)` 가드. 무조건 식 적용.
        //   verticalSpeed=0 시 `0.3F/0 = Infinity, cos*Infinity*0 = NaN` 발생. 원본 1.7.10
        //   GL11.glRotatef(NaN) = drivers 보통 무회전 (identity) → leg.pitch=0 시각 효과 ("다리 1자").
        //   1.21.1 JOML Quaternionf(NaN) = matrix NaN → vertex 깨짐. NaN 가드로 0 fallback —
        //   원본 1.7.10 OpenGL drivers 의 무회전 동작과 시각 일치 (사용자 보고 "처음 시작 시 다리 1자").
        //
        // 🔴 fix #76 (BUG #2.5, 2026-05-10, 사용자 보고 "weeping/twisting vines + sneak 시 다리 사라짐"):
        //   verticalSpeed 가 정확히 0 이 아닌 **매우 작은 양수** (float underflow 수준) 시 식 결과:
        //     0.3f / tiny_positive = +Infinity (float overflow, NaN 아님)
        //     cos * +Infinity * tiny_positive = ±Infinity (Infinity × positive = Infinity 유지)
        //     + offset = ±Infinity
        //   기존 Float.isNaN 가드는 Infinity 를 잡지 못함 → leg.pitch=±Infinity → 다리 transform 망가짐.
        //   해결: `!Float.isFinite(...)` 로 가드 강화 — NaN + ±Infinity 모두 0 fallback (= 원본
        //   "다리 1자" 의도 1:1).
        //   dump 검증 ([BUG2.5-DBG]): tick 168786 부터 rL.pitch=-Infinity / lL.pitch=Infinity 발생.
        //   사용자 시나리오 = weeping/twisting vines + sneak (정지 상태). 우리 sm_isSneaking fix #75
        //   후 sm.stats.currentVerticalSpeed 가 작은 underflow 양수로 갱신되는 것이 직접 원인.
        if (!sm.isFeetVineClimbing) {
            rightLeg.pitch = MathHelper.cos(totalVerticalDistance * feetFrequenceUpFactor)        * feetDistanceUpFactor * verticalSpeed + feetDistanceUpOffset;
            leftLeg.pitch  = MathHelper.cos(totalVerticalDistance * feetFrequenceUpFactor + HALF) * feetDistanceUpFactor * verticalSpeed + feetDistanceUpOffset;
            if (!Float.isFinite(rightLeg.pitch)) rightLeg.pitch = 0f;
            if (!Float.isFinite(leftLeg.pitch))  leftLeg.pitch  = 0f;
        }

        // 원본 L223-L224: 다리 Z(roll) — 무조건 식 적용.
        rightLeg.roll = -(MathHelper.cos(totalHorizontalDistance * feetFrequenceSideFactor) - 1.0f)          * horizontalSpeed * feetDistanceSideFactor + feetDistanceSideOffset;
        leftLeg.roll  = -(MathHelper.cos(totalHorizontalDistance * feetFrequenceSideFactor + QUARTER) + 1.0f) * horizontalSpeed * feetDistanceSideFactor + feetDistanceSideOffset;
        // fix #76 일관성: roll 도 isFinite 가드.
        if (!Float.isFinite(rightLeg.roll)) rightLeg.roll = 0f;
        if (!Float.isFinite(leftLeg.roll))  leftLeg.roll  = 0f;

        // vine 전용 — 원본 L228-L239.
        // B-5 / §16-12: 원본 L228/L232 cos 입력은 totalDistance (3D 누적). 이전 limbSwing
        //   (수평 누적) 잘못 매핑 → sm.stats.totalDistance 로 교체. SmartStatistics.calculate
        //   L91 = totalDistance 누적 1.7.10 등가.
        if (sm.isFeetVineClimbing) {
            // 부드러움: totalDistance 도 partial tick lerp getter 로 교체.
            float totalDistanceLerped = sm.stats.getTotalDistance(partialTicks);
            float total = (MathHelper.cos(totalDistanceLerped + HALF) + 1f) * THIRTYTWOTH + SIXTEENTH;
            rightLeg.pitch = -total;   // pitch 덮어쓰기
            leftLeg.pitch  = -total;

            float diff = Math.max(0f, MathHelper.cos(totalDistanceLerped - QUARTER)) * SIXTYFOURTH;
            leftLeg.roll  += -diff;    // roll 누적 (원본 `+=`)
            rightLeg.roll +=  diff;

            // 원본 SmartMovingModel.java L391: setLegScales(abs(cos(rightLeg.X)), abs(cos(leftLeg.X)))
            setLegScales(rightLeg, leftLeg,
                    Math.abs(MathHelper.cos(rightLeg.pitch)),
                    Math.abs(MathHelper.cos(leftLeg.pitch)));
        }

        // (D-2) 원본 SmartMovingModel L134-L135: bipedLeft/RightLeg.rotationOrder = YZX.
        //   ModelPart 기본 회전 순서는 ZYX. 다리 yaw 가 0 으로 명시되어 X/Z 회전 순서 차이는
        //   결과에 영향 없음 (Y=0 이면 Y 위치 무관) → 헬퍼 미사용 OK.
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
            // 🔴 fade 보간 (2026-05-04 — 사용자 보고 fix "다리 땅 침투"):
            //   bodyAngleX 만 fade 시 legAngleX = QUARTER - bodyAngleX 식 → bodyAngleX=0 시작 시
            //   legAngleX = π/4 = 큰 앞쪽 회전 → leg vertex 박스 침투.
            //   fix: legAngleX/legAngleZ 도 별도 fade 적용 (= 각각 prev=0 시작 → target lerp).
            //   setupTransforms 가 먼저 호출 → bodyAngleX_faded store 됨. 여기서 read.
            bodyAngleX = sm.smCrawlClimbBodyAngleXFaded;
            // legAngleX/legAngleZ target = 위 if/else if/else 분기 결과. fade 0 시작 → target 점진.
            legAngleX = sm.applyCrawlClimbLegAngleXFade(legAngleX, animationProgress);
            legAngleZ = sm.applyCrawlClimbLegAngleZFade(legAngleZ, animationProgress);
            // 옵션 D 정확 1:1 매핑 (원본 isCrawlClimb 분기 L265-L276):
            //   L265 bipedTorso.X = bodyAngleX → setupTransforms 의 root R_x (별도 inject).
            //   L267-L268 bipedShoulder.X = -bodyAngleX → arm.pitch += -bodyAngleX (shoulder cancel).
            //   L270 bipedHead.X = -bodyAngleX → head.pitch 직접 set.
            //   L272-L273 bipedLeg.X = legAngleX → leg.pitch 직접 set (= 부모 R_x 효과 setupTransforms).
            //   L275-L276 bipedLeg.Z = ±legAngleZ → leg.roll 직접 set.

            body.pitch     =  0f;                     // L265 부모 매핑 (setupTransforms)
            head.pitch     = -bodyAngleX;             // L270 head cancel = 무회전
            rightLeg.pitch =  legAngleX;              // L272 leg local
            leftLeg.pitch  =  legAngleX;              // L273
            rightLeg.roll  =  legAngleZ;              // L275
            leftLeg.roll   = -legAngleZ;              // L276
            rightArm.pitch += -bodyAngleX;            // L267 shoulder cancel
            leftArm.pitch  += -bodyAngleX;            // L268
            // leg/arm pivot = vanilla default (= setupTransforms 부모 회전이 자동 적용).
            //   sm_animateClimbing L494-L495 의 leg.pivotX = ±2.0 그대로.
            //   leg.pivotY/Z, arm.pivotY/Z 는 vanilla default (= 12, 0) / (2, 0).
        }

        // NoGrab + non-NoStep 추가 보정 (원본 SmartMovingModel L279-L286).
        //   ⚠️ 원본은 `if(isCrawlClimb)` 블록 **밖** — 모든 클라이밍 (CrawlClimb 여부 무관) 에서 적용.
        //   이전 구현은 `if(isCrawlClimb)` **안** 에 두어 일반 그랩 클라이밍에서 보정 누락. 정정 (D-4).
        //   원본 식:
        //     bipedTorso.X = 0.5F           → body.pitch = 0.5f
        //     bipedHead.X -= 0.5F           → head.pitch -= 0.5f
        //     bipedPelvic.X -= 0.5F         → leg.pitch -= 0.5f (1.21.1 pelvic 부재로 다리 직접)
        //     bipedTorso.rotationPointZ = -6F → body.pivotZ = -6f
        //   조건 매핑:
        //     handsClimbType == NoGrab → 우리 ordinal NONE(0)/SINK(1) (h < 2).
        //     feetClimbType != NoStep ≡ DownStep — 원본 setShouldClimbSpeed 채널은 NoStep/DownStep
        //       2-way 만 set. "발 등반 중" 의미 = 우리 enum 의 SLOW_UP_*/FAST_UP (ordinal >= 4).
        //       기존 `> 0` 은 BASE_HOLD/BASE_WITH_HANDS/TOP_WITH_HANDS (발 가만히) 도 포함하는 오역.
        // 🔴 D-4 분기 도메인 변경: 원본 L279 = `handsClimbType == NoGrab && feetClimbType != NoStep`.
        //   새 0/1/2 도메인: hands == 0 (NoGrab) && feet != 0 (DownStep).
        if (sm.actualHandsClimbType == 0 && sm.actualFeetClimbType != 0) {
            // 🔴 D-4 1:1 매핑 (사용자 보고 7회차 — 자세 1대1 OK + 종료 후 잔존 fix):
            //   원본 SmartMovingModel L279-L286 분석:
            //     bipedTorso.rotateAngleX = 0.5F;        → body, head, arm, leg 모두 +0.5 회전 (자식 효과)
            //     bipedTorso.rotationPointZ = -6F;       → 모든 자식 z 평행이동
            //     bipedHead.rotateAngleX -= 0.5F;        → head 자식 효과 cancel → vanilla 결과
            //     bipedPelvic.rotateAngleX -= 0.5F;      → pelvic 자식 효과 cancel → leg = vanilla
            //     (arm cancel 없음 → arm 만 +0.5 그대로 노출)
            //
            //   1.21.1 parallel 노드 매핑 (사용자 자세 1대1 검증 OK):
            //     - body.pitch = 0.5             → bipedTorso.X 직접 매핑
            //     - 모든 노드 pivotZ = -6         → bipedTorso.rotationPointZ 평행이동
            //     - arm.pitch += 0.5             → 원본 arm 자식 효과 노출
            //     - head/leg.pitch 변경 안 함     → 원본 자식 효과 + cancel = 0 = vanilla 결과
            //
            //   등반 종료 후 standing 시 잔존 (사용자 명시) 은 위 reset 인프라 영역의 종료 엣지
            //   cleanup (smWasClimbingForCleanup 추적) 에서 처리.
            body.pitch  = 0.5f;
            body.pivotZ     = -6f;
            head.pivotZ     = -6f;
            rightArm.pivotZ = -6f;
            leftArm.pivotZ  = -6f;
            // 🔴 leg pivot 정확 매핑 (사용자 보고 — 다리 벽 안 뚫림 fix):
            //   원본 매트릭스: T(0,0,-6) * R_x(0.5) * T(0,12,0) * T(±2,0,0).
            //   bipedTorso 회전 (R_x 0.5) 가 자식 (leg) 위치에 적용 → leg 절대 위치 =
            //     (±2, 12*cos(0.5), -6 + 12*sin(0.5)) ≈ (±2, 10.529, -0.247).
            //   1.21.1 parallel 구조라 R_x(0.5) 자동 누적 안 됨. leg.pivotZ=-6 만 적용 시 leg
            //     절대 = (±2, 12, -6). Z 차이 -5.75 픽셀 = 사용자 보고 "다리 벽 안 5.75 뚫림".
            //   해결: leg.pivotY/Z 를 원본 매트릭스 결과 위치로 명시 set.
            rightLeg.pivotY = 12f * MathHelper.cos(0.5f);  // ≈ 10.529
            leftLeg.pivotY  = 12f * MathHelper.cos(0.5f);
            rightLeg.pivotZ = -6f + 12f * MathHelper.sin(0.5f);  // ≈ -0.247
            leftLeg.pivotZ  = -6f + 12f * MathHelper.sin(0.5f);
            rightArm.pitch += 0.5f;
            leftArm.pitch  += 0.5f;
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
    private void sm_animateCeilingClimbing(SmartMovingClientState sm, float headYaw) {
        // 🔴 (사용자 보고 후, 천장 등반 1:1 정정): 원본 L298-L300 입력은 SM 통계.
        //   원본: distance = totalHorizontalDistance * 0.7F
        //         walkFactor = Factor(currentHorizontalSpeed, 0, 0.12951545)
        //         standFactor = Factor(currentHorizontalSpeed, 0.12951545, 0)
        //   이전 매핑 limbSwing/limbSwingAmount (vanilla limbAnimator) → 천장 매달려 좌우 이동 시
        //   ground walk 기준 vanilla 누적 안 됨 → walkFactor=0 → 팔/다리 진자운동 항 0 → 사용자 보고
        //   "팔다리 가만히". sm_animateClimbing 동일 패턴 — partial tick lerp getter 적용.
        float partialTicks = SmartMovingClientState.globalCachedTickDelta;
        float horizontalSpeedLerped = sm.stats.getCurrentHorizontalSpeed(partialTicks);
        float distance    = sm.stats.getTotalHorizontalDistance(partialTicks) * 0.7f;
        float walkFactor  = smFactor(horizontalSpeedLerped, 0f, 0.12951545f);
        float standFactor = smFactor(horizontalSpeedLerped, 0.12951545f, 0f);

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
    private void sm_animateCrawling(SmartMovingClientState sm, float headYaw) {
        // 🔴 (Phase 1, Crawl 애니메이션 1:1):
        //   원본 SmartMovingModel L395-L397 입력 = totalHorizontalDistance + currentHorizontalSpeedFlattened.
        //   이전 매핑 limbSwing/limbSwingAmount (vanilla limbAnimator) 잘못. 그랩 클라이밍 패턴 동일.
        float partialTicks = SmartMovingClientState.globalCachedTickDelta;
        float distance    = sm.stats.getTotalHorizontalDistance(partialTicks) * 1.3f;
        // 🔴 (2026-05-03 crawl 디테일 fix): partial ticks lerp getter 사용 (= 60Hz 부드러움).
        //   원본 SmartStatistics.getCurrentHorizontalSpeedFlattened(partialTicks, -1) 매핑.
        float speedFlat   = sm.stats.getCurrentHorizontalSpeedFlattened(partialTicks);
        float walkFactor  = smFactor(speedFlat, 0f, 0.12951545f);
        float standFactor = smFactor(speedFlat, 0.12951545f, 0f);

        // 머리
        // 🔴 사용자 보고 fix (2026-05-03 — "엎드린 상태에서 머리 움직임 원본과 다름"):
        //   원본 SmartMovingModel.java L399-L401 isCrawl 분기:
        //     bipedHead.rotateAngleZ = -viewHorizontalAngelOffset / RadiantToAngle
        //     bipedHead.rotateAngleX = -Eighth
        //     bipedHead.rotationPointZ = -2F
        //     ⚠ bipedHead.rotateAngleY 는 미설정 → reset() 값 0 유지.
        //   원본 animateHeadRotation (L626-L632) 의 isStandard 가드 → isCrawl != isStandard →
        //     superAnimateHeadRotation 호출 X → SR default head.rotateAngleY 매핑 미적용.
        //   우리 sm_setAngles 가 @At("TAIL") → vanilla BipedEntityModel.setAngles 이미
        //     head.yaw = headYaw * π/180 설정. cancel 안 하면 살아있음 → 마우스 좌우 회전이
        //     head.roll (= -headYaw 매핑) + head.yaw (= vanilla 잔존) 양쪽 적용 → 어긋남.
        //   fix: head.yaw = 0 명시.
        head.roll  = -headYaw * DEG_TO_RAD;
        head.pitch = -EIGHTH;
        head.yaw   = 0f;
        head.pivotZ = -2f;   // 원본 bipedHead.rotationPointZ = -2F (B-12 / §16-18)

        // 🔴 (Phase 1, Crawl 애니메이션 1:1): 몸통 회전 순서 YZX 매핑.
        //   원본 L403 `bipedTorso.rotationOrder = ModelRotationRenderer.YZX` 명시.
        //   1.21.1 ModelPart 기본 ZYX → setAnglesYZX_v2 (정확 매핑) 사용.
        // 🔴 Phase 3 자식 효과 매핑 (2026-05-03): body.pitch 의 QUARTER-THIRTYTWOTH 항 제거 +
        //   body.pivotY = 3f 제거. entity 회전 + translate 가 sm_setupTransforms 의 isCrawling 분기에서
        //   대신 적용 (= 모든 자식 노드 일괄 처리). 원본 bipedTorso 부모 효과 1:1.
        //   본 위치는 LOCAL 미세 진동 (Y/Z cos) 만 유지.
        // 🔴 setAnglesYZX → setAnglesYZX_v2 변경 (2026-05-03 사용자 보고 fix — "팔/몸통 디테일"):
        //   기존 setAnglesYZX BUG (vertex 적용 X→Z→Y, 원본 Y→Z→X 와 반대). v2 = 정확 매핑.
        //   body 의 yaw/roll 작은 cos 라 영향 미세하지만 1:1 정확.
        setAnglesYZX_v2(body,
                0f,                                                              // ← entity 회전이 대신 적용
                MathHelper.cos(distance + HALF) * SIXTYFOURTH * walkFactor,    // 원본 L407 bipedBody.Y
                0f);                                                             // L406 bipedTorso.Z 는 sm_setupTransforms 의 matrices.rotate(R_z) 에서 매핑 (= 부모 효과 1:1)
        // body.pivotY = 3f 제거 — entity translate 가 대신 적용

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
        // 🔴 setAnglesYZX_v2 사용 (2026-05-03 사용자 보고 fix — "팔이 하늘로 들림"):
        //   원본 YZX vertex 적용 = Y → Z → X. arm yaw=±90° 큰 회전 + pitch=225° 큰 회전 →
        //   회전 순서 차이가 팔 위치에 큰 visual 영향. v2 = 원본 1:1.
        setAnglesYZX_v2(rightArm, HALF + EIGHTH, -QUARTER, rRoll);
        setAnglesYZX_v2(leftArm,  HALF + EIGHTH,  QUARTER, lRoll);

        // 원본 SmartMovingModel.java L622-L626 + L645-L648: 다리/팔 yScale.
        // 원본 가드 `if (scaleLegType != NoScaleStart)` / `scaleArmType != NoScaleStart` — 메인 모델 = Scale.
        // 좌우 위상 다름 — 원본 표기 그대로 보존 (cos(distance + Quarter - Quarter) = cos(distance) 등 단순화 안 함).
        setLegScales(rightLeg, leftLeg,
                1f + (MathHelper.cos(distance + QUARTER - QUARTER) - 1f) * 0.25f * walkFactor,
                1f + (MathHelper.cos(distance - QUARTER - QUARTER) - 1f) * 0.25f * walkFactor);
        setArmScales(rightArm, leftArm,
                1f + (MathHelper.cos(distance + QUARTER) - 1f) * 0.15f * walkFactor,
                1f + (MathHelper.cos(distance - QUARTER) - 1f) * 0.15f * walkFactor);

        // 🔴 (Phase 2 multi BUG-12) vanilla setAngles 의 sneak 분기 set field cancel.
        //   진입 edge frame 에 server-sync pose 가 CROUCHING 잔존 (= 직전 sneak 키 누름) +
        //   sm.isCrawling=true (= SM packet 도착) 인 경우 vanilla setAngles 가 sneak 분기 진입 →
        //   body.pivotY=3.2/head.pivotY=4.2/leg.pivotZ=4/arm.pivotY=5.2/leg.pivotY=12.2 set.
        //   sm_animateCrawling 가 회전만 set → vanilla sneak pivotY 잔존 → 자세 부분별 어긋남.
        //   해결: sneak 분기 set field 를 standing default 값 으로 강제 cancel.
        //   self 측은 pose=SWIMMING 즉시 → vanilla sneak 분기 미진입 → 영향 없음.
        body.pivotY = 0f;
        head.pivotY = 0f;
        rightArm.pivotY = 2f;
        leftArm.pivotY = 2f;
        rightLeg.pivotY = 12f;
        leftLeg.pivotY = 12f;
        rightLeg.pivotZ = 0f;
        leftLeg.pivotZ = 0f;
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
    private void sm_animateSliding(SmartMovingClientState sm, float headYaw) {
        // 🔴 BUG-Slide-Anim-6 fix (2026-05-04 사용자 보고 — "디테일 값 다름"):
        //   원본 SmartMovingModel.java L439-L440 입력 = totalHorizontalDistance + currentHorizontalSpeed.
        //   SmartMovingModel L62/L84: `currentHorizontalSpeedFlattened` 가 NaN 아닐 때
        //     `currentHorizontalSpeed = currentHorizontalSpeedFlattened` 덮어씀.
        //   원본 SmartRenderRender L56-L57: getTotalDistance/getCurrentSpeed(renderPartialTicks) lerped getter.
        //   이전 매핑 limbSwing/limbSwingAmount (vanilla limbAnimator) 잘못 — sm_animateCrawling
        //     동일 패턴으로 정정 (= partial tick lerp + Flattened 속도).
        float partialTicks = SmartMovingClientState.globalCachedTickDelta;
        float distance   = sm.stats.getTotalHorizontalDistance(partialTicks) * 0.7f;
        float speedFlat  = sm.stats.getCurrentHorizontalSpeedFlattened(partialTicks);
        float walkFactor = smFactor(speedFlat, 0f, 1f) * 0.8f;

        // 🔴 BUG-Slide-Anim-7 fix (2026-05-04 사용자 보고 — "마우스 움직임에 따른 머리 고정"):
        //   원본 SmartMovingModel.java L442-L444 isSlide 분기:
        //     bipedHead.rotateAngleZ = -viewHorizontalAngelOffset / RadiantToAngle
        //     bipedHead.rotateAngleX = -Eighth - Sixteenth
        //     bipedHead.rotationPointZ = -2F
        //     ⚠ bipedHead.rotateAngleY 는 미설정 → SR reset() 효과로 0 유지.
        //   우리 sm_setAngles 가 @At("TAIL") → vanilla BipedEntityModel.setAngles 이미
        //     head.yaw = headYaw * π/180 설정. cancel 안 하면 잔존 → 마우스 좌우 회전이
        //     head 따라옴 → 사용자 보고 BUG.
        //   메모리 feedback_smbody_yaw_force_head_yaw_zero.md: smBodyYawOverride force +
        //     head.yaw=0 force 동시 적용 패턴.
        //   sm_animateCrawling (L928) 동일 패턴 차용 — head.yaw=0 명시.
        head.pitch  = -EIGHTH - SIXTEENTH;
        head.roll   = -headYaw * DEG_TO_RAD;   // 원본 bipedHead.rotateAngleZ = -viewHorizontalAngelOffset/RadiantToAngle (B-13)
        head.yaw    = 0f;                      // ★ vanilla netHeadYaw cancel
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
        // 🔴 사용자 보고 fix (2026-05-04 — "몸통 자체가 다리쪽으로 내려감"):
        //   원본 `bipedBody.rotationPointY = +6.5F` (L453) 직접 매핑은 body cuboid 를 modelpart Y
        //   +6.5 (다리쪽) 으로 이동 → R_x(-π/2) 회전 후 modelpart Z -6.5 (= 다리 방향) → body 가
        //   다리쪽 너머로 이동 BUG.
        //   원본 SR 다층 구조 (bipedOuter → bipedTorso → bipedBody) 의 부모-자식 누적 효과가
        //   1.21.1 vanilla biped 단일 노드 매핑에서 다른 결과. body.pivotY 직접 매핑은 부정확.
        //   회전 origin 변경 (sm_setupTransforms pivotY) 이 이미 모든 노드 +5 효과 내포 →
        //   body.pivotY 추가 적용 = 누적 효과 + 6.5 = body 만 다리쪽 너무 많이.
        //   fix: body.pivotY = 0 (default, body 자체 추가 이동 제거) → 어깨 부근 정상 위치.
        body.pivotY = 0f;

        // 다리
        rightLeg.pitch = MathHelper.cos(distance + HALF) * SIXTYFOURTH * walkFactor + SIXTYFOURTH;
        leftLeg.pitch  = MathHelper.cos(distance + QUARTER) * SIXTYFOURTH * walkFactor + SIXTYFOURTH;
        rightLeg.roll  =  THIRTYTWOTH;
        leftLeg.roll   = -THIRTYTWOTH;

        // ★★ 원본 SmartMovingModel.java L463-L473 원자 단위 1:1 번역 ★★
        //   bipedRightArm.rotationOrder = ModelRotationRenderer.YZX  ← R_x * R_z * R_y (vertex Y→Z→X)
        //   bipedRightArm.rotateAngleX = cos(distance + Quarter) * Sixtyfourth * walkFactor + Half - Sixtyfourth
        //   bipedLeftArm.rotateAngleX  = cos(distance - Half)    * Sixtyfourth * walkFactor + Half - Sixtyfourth
        //   bipedRightArm.rotateAngleZ =  Sixteenth     bipedLeftArm.rotateAngleZ = -Sixteenth
        //   bipedRightArm.rotateAngleY = -Quarter       bipedLeftArm.rotateAngleY =  Quarter
        //
        // 🔴 사용자 보고 fix v4 (2026-05-04 — "팔 흔들림 좌우 → 위아래" 13단계 시행착오 정착):
        //   원인: vanilla ModelPart.rotate 의 rotationZYX(roll, yaw, pitch) hardcoded.
        //         원본 yaw = -π/2 정확히 ZYX gimbal lock 영역.
        //         JOML getEulerAnglesZYX 분해 부정확 → qDiff angle = π (정반대 회전 손실).
        //         setAnglesYZX/YXZ 헬퍼 + 직접 set 모두 손실 발생.
        //   디버그 dump 검증:
        //     INTENT (의도 1.7.10 YZX) hand X = -8.83 정적 (어깨 옆 펼침), slidY 진동 1.4 (위아래 ✓).
        //     REAL (분해 후 vanilla ZYX) hand X = -1.36~-2.85 (가운데 모임 ✗), slidY 정적 (위아래 X ✗).
        //   해결: MixinModelPart 으로 ModelPart.rotate 의 rotationZYX 우회 → 의도 q 직접 적용.
        //         메모리 feedback_zxy_zyx_rotation_order.md 가이드라인 (큰 yaw 헬퍼 금지) 준수.
        float armYaw = -QUARTER;
        float rPitch = MathHelper.cos(distance + QUARTER) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        float lPitch = MathHelper.cos(distance - HALF)    * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;

        // 의도 q = q_x(pitch) * q_z(roll) * q_y(yaw) — 원본 1.7.10 YZX 매트릭스.
        Quaternionf qRight = new Quaternionf().rotationY(armYaw);
        qRight.premul(new Quaternionf().rotationZ(SIXTEENTH));
        qRight.premul(new Quaternionf().rotationX(rPitch));
        ((SmModelPartOverride)(Object) rightArm).sm_setOverrideQuat(qRight);

        Quaternionf qLeft = new Quaternionf().rotationY(-armYaw);
        qLeft.premul(new Quaternionf().rotationZ(-SIXTEENTH));
        qLeft.premul(new Quaternionf().rotationX(lPitch));
        ((SmModelPartOverride)(Object) leftArm).sm_setOverrideQuat(qLeft);
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
    private void sm_animateFlying(SmartMovingClientState sm, net.minecraft.client.network.AbstractClientPlayerEntity player, float limbSwing, float limbSwingAmount, float totalTime) {
        // 🔴 (세션 65g): preferred arm 만 vanilla swing 처리. 다른 모델 (몸/다리/다른 팔/head)
        //   은 sm 비행 자세 그대로 유지.
        //   사용자 요구: "오른팔 (휘두르는 팔만 그런거야). 모든 에니메이션을 갑자기 초기화하지
        //   말고. 휘두르는 에니메이션만 처리".
        //   = preferred arm 만 setAnglesXZY skip (vanilla swing 잔존) + 부모 X 회전 cancel
        //     (직립 자세). Y 회전은 그대로 (이동 방향 따라감, 모델과 같이 회전).

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
        // 🔴 (세션 65g): preferred arm 의 setAnglesXZY skip → vanilla setAngles + animateArms
        //   가 set 한 swing 모션 잔존. 다른 팔만 날개짓 자세 적용.
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

        // 🔴 (세션 65k revert 65j): vanilla 효과 직접 set 매핑은 모든 vanilla setAngles 효과
        //   (limbSwing 진폭, ArmPose ITEM offset, sneaking 등) 를 cancel → 사용자 보고
        //   "전체적으로 휘두르는게 아예 이상해짐". 65h 매핑 (= setAnglesXZY skip + X cancel)
        //   으로 되돌림. vanilla setAngles + animateArms 가 set 한 모든 효과 그대로 보존.
        //   X/Z 시 미세 차이는 더 자세한 보고 후 추가 조정.
        if (swing > 0F) {
            float thetaCancel = lerpFadeAngle(sm.smOuterTiltX_prev, theta,
                                              sm.smOuterFade_prevTime, totalTime);
            // 🔴 (세션 65q): 회전 부호 반전 + 위치 보정 → 시각 결과가 수직/수평 동일.
            //   사용자 의견: "수직 시 결과 올바름. 수평 값 조정해서 같게".
            //   65k 의 R_x(+theta) cancel 은 scale Y 부호 반전 영향으로 실제 world R_x(-θ)
            //   적용 → 부모 R_x(-θ) * R_x(-θ) = R_x(-2θ) → "90° 뒤로 젖혀짐".
            //   부호 반전 (R_x(-theta)) + 위치 보정 → 시각 결과가 직립 swing 효과.
            if (preserveRight) {
                preCancelParentXPivot(rightArm, thetaCancel);
                preCancelParentXRotation(rightArm, thetaCancel);
            }
            if (preserveLeft) {
                preCancelParentXPivot(leftArm, thetaCancel);
                preCancelParentXRotation(leftArm, thetaCancel);
            }

            // 🔴 (2026-04-27): vanilla animateArms 가 swing 시 양 팔 pivotZ = ±sin(body.yaw)*5
            //   를 매 프레임 set → 비-preferred arm "움찔움찔" (사용자 보고 비행 중 왼팔).
            //   비-preferred arm 만 vanilla setAngles Step 4 기본값 복원 (preferred arm 은
            //   vanilla swing 효과 그대로 보존).
            if (!preserveRight) {
                rightArm.pivotX = -5f;
                rightArm.pivotZ =  0f;
            }
            if (!preserveLeft) {
                leftArm.pivotX  =  5f;
                leftArm.pivotZ  =  0f;
            }
        }
    }

    /**
     * isHeadJumping: 헤드점프 (낮은 천장 아래 점프).
     * 원본: SmartMovingModel.setRotationAngles() 10번 분기 (isHeadJump).
     * bipedOuter X/Y 기울기는 setupTransforms에서 처리.
     * smallOverGroundHeight로 팔 Z 각도 클램프.
     */
    private void sm_animateHeadJumping(SmartMovingClientState sm, AbstractClientPlayerEntity player) {
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

        // 🔴 head.yaw=0 강제 (2026-05-11, 사용자 보고 "헤드점프 시 머리가 마우스 따라 움직임"):
        //   feedback_smbody_yaw_force_head_yaw_zero.md 패턴 1:1 적용.
        //   setupTransforms 에서 smBodyYawOverride 가 ModifyArg 인자만 force (entity.bodyYaw
        //   필드는 자유 값) → vanilla setAngles netHeadYaw = (headYaw - 자유 bodyYaw lerp)
        //   잔존 → head.yaw 가 마우스 따라 회전.
        //   원본 1.7.10 은 entity.renderYawOffset 필드 자체 force → netHeadYaw = (마우스 -
        //   force 된 이동 방향) → 이동 방향 기준 회전 (사용자 시각 "거의 고정").
        //   head.yaw=0 강제로 vanilla netHeadYaw 효과 cancel → head world yaw = force bodyYaw
        //   (= 이동 방향) → 마우스 무관 고정. 다른 SM 분기 (sliding/flying/crawling/angleJump)
        //   와 동일 패턴.
        head.yaw = 0f;

        // 팔 Z: Factor(angle, Quarter, -Quarter). 원본 SmartMovingModel L520-522:
        //   armFactorZ = Factor(currentVerticalAngle, Quarter, -Quarter);
        //   if (overGroundBlock != null && overGroundBlock.getMaterial().isSolid())
        //       armFactorZ = Math.min(armFactorZ, smallOverGroundHeight / 5F);
        //
        // 원본 getOverGroundBlockId 는 박스 발 위치 (= floor(bb.minY)) 의 블록 1회 검사 후
        //   즉시 반환 (= 1.7.10 air 도 non-null 이라 for 첫 iteration 에서 반환).
        //   박스 발 = 헤드점프 진행 중 일반적으로 공중 → air block → isSolid=false →
        //   원본 clamp 미적용 (= armFactorZ = factorZraw 그대로).
        //   feedback_headjump_overground_air_is_solid.md.
        float armFactorZ = smFactor(angle, QUARTER, -QUARTER);
        if (sm.smallOverGroundHeight < 5f && isOverGroundBlockSolid(player)) {
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
     * 원본 SmartMovingBase.getOverGroundBlockId(distance).getMaterial().isSolid() 1:1 매핑.
     *
     * 원본 식 (L862-882):
     *   int y = floor(bb.minY);
     *   for(; y >= minY; y--) {
     *       Block block = world.getBlock(x, y, z);   // 1.7.10 air 도 non-null
     *       if(block != null) return block;            // 항상 true → 첫 iteration 즉시 반환
     *   }
     *   → 박스 발 위치 (= floor(bb.minY)) 의 블록 1회 검사 후 반환.
     *
     * 1.21.1 등가:
     *   - getBlockState(BlockPos) 항상 BlockState 반환 (null 안 됨).
     *   - isSolid() 등가 = collision shape 비어있지 않음 (air → false, solid → true).
     *
     * 헤드점프 진행 동안 박스 발 = 일반적으로 공중 → air → false → clamp 미적용.
     *
     * 멀티 친화: AbstractClientPlayerEntity (self + remote 공통).
     *   박스 발 위치 self/remote 모두 entity.y + 1m (mixin offset 활성, POSE=SLIDING).
     *   원본 1.7.10 의 esp==null 보정 (y++) 은 우리 매핑에서 박스 발 위치 동일이라 불필요.
     */
    private static boolean isOverGroundBlockSolid(AbstractClientPlayerEntity player) {
        World world = player.getWorld();
        if (world == null) return false;
        Box bb = player.getBoundingBox();
        int x = MathHelper.floor(player.getX());
        int z = MathHelper.floor(player.getZ());
        int y = MathHelper.floor(bb.minY);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        pos.set(x, y, z);
        BlockState state = world.getBlockState(pos);
        // 1.21.1 isSolid 등가 (feedback_isFullCube_partial_solid_pattern.md):
        //   collision shape 비어있지 않음. air → 비어있음 → false. solid → 비어있지 않음 → true.
        return !state.getCollisionShape(world, pos).isEmpty();
    }

    /**
     * isFalling: 자유 낙하.
     * 원본: SmartMovingModel.setRotationAngles() L531-549 (isFalling 분기).
     *
     * 🔴 distance 입력 1:1 정정 (2026-04-27):
     *   원본 L533 `distance = totalDistance * 0.1F` 의 totalDistance 는
     *   SmartRenderRender L56 `statistics.getTotalDistance(renderPartialTicks)` 호출 결과.
     *   = `total - legYaw * (1.0F - partialTicks)` (SmartStatisticsData L33-36).
     *
     *   - 매 틱 `total += legYaw` 누적 (legYaw = currentSpeed EMA, factor 0.4, 0~1 clamp).
     *   - 매 프레임 partialTicks lerp → 60Hz 부드러움 (사용자 "fade 보간 부드러움" 의 실체).
     *   - 낙하 시작 직후 legYaw 가 0→1 점진 증가 → 휘저음 가속도 자연스러움
     *     (사용자 "팔/다리 휘저어지는 속도 및 가속도" 의 직접 원인).
     *
     *   이전 매핑 `animationProgress * 0.1F` 는 vanilla `entity.age + tickDelta` —
     *   이동 가속도 무관 + 시간 단조 증가 → 가속도 부재 + 정지 시에도 휘저음 지속 (원본과 다름).
     *
     *   정정: `sm.stats.getTotalDistance(partialTicks) * 0.1F` (이미 인프라 존재).
     *
     * 회전 순서: 팔 XZY (setAnglesXZY 헬퍼). 회전식은 원본 L538-548 라인별 1:1.
     *
     * 🔴 swing 처리 (2026-04-27):
     *   vanilla `BipedEntityModel.animateArms` 가 handSwingProgress > 0 시 body.yaw 를
     *   `sin(sqrt(swing) * 2π) * 0.2` 로 흔든다 → 사용자 보고 "공중 낙하 휘두름 시 몸통 움찔움찔".
     *   비행 분기 sm_animateFlying 의 preserve 패턴 차용:
     *   - body.yaw cancel = sm_setAngles 의 anySmState reset 인프라 (isFallingForReset 추가).
     *   - preferred arm 의 setAnglesXZY skip → vanilla swing pitch/yaw/roll 그대로 보존.
     *   - falling 은 setupTransforms X 회전 없음 → preCancelParentX* 불필요.
     */
    private void sm_animateFalling(SmartMovingClientState sm, net.minecraft.client.network.AbstractClientPlayerEntity player) {
        float partialTicks = net.minecraft.client.MinecraftClient.getInstance()
                .getRenderTickCounter().getTickDelta(false);
        float totalDistance = sm.stats.getTotalDistance(partialTicks);
        float distance = totalDistance * 0.1f;

        // 🔴 (2026-04-27) 낙하 시 비행 패턴 — 머리/몸 같이 fade lag.
        //   sm_setupTransforms TAIL 의 fade matrix 가 head 포함 모든 ModelPart 영향.
        //   head.yaw=0 force → vanilla netHeadYaw 무력화 → head world yaw = body world yaw.
        //   head.pitch=0 force → vanilla headPitch 무력화 → 마우스 위아래 시 머리 끄덕임 고정.
        //   사용자 요청 (2026-04-27): "낙하일 때는 비행일때랑 같게 처리" + "마우스 위아래 끄덕임 고정".
        //   비행 분기 (sm_animateFlying) 는 setupTransforms X tilt 보정 위해 head.pitch=-theta/2 set,
        //   낙하는 X tilt 없음 → 단순 0 force.
        head.yaw = 0f;
        head.pitch = 0f;
        head.roll = 0f;

        // preferred arm 만 vanilla swing 보존. swing > 0 시에만 활성.
        float swing = player.handSwingProgress;
        Arm preferredArm = player.getMainArm();
        boolean preserveRight = swing > 0F && preferredArm == Arm.RIGHT;
        boolean preserveLeft  = swing > 0F && preferredArm == Arm.LEFT;

        // 팔 (XZY 순서) — 원본 L535-542.
        // rotationOrder = XZY → GL call Y, Z, X → setAnglesXZY 헬퍼.
        float rYaw  = MathHelper.cos(distance + QUARTER) * EIGHTH;
        float lYaw  = MathHelper.cos(distance + QUARTER) * EIGHTH;
        float rRoll = MathHelper.cos(distance) * EIGHTH + QUARTER;
        float lRoll = MathHelper.cos(distance) * EIGHTH - QUARTER;
        if (!preserveRight) setAnglesXZY(rightArm, 0f, rYaw, rRoll);
        if (!preserveLeft)  setAnglesXZY(leftArm,  0f, lYaw, lRoll);

        // 다리 X (앞뒤 흔들림) — 원본 L544-545.
        rightLeg.pitch = MathHelper.cos(distance + HALF + QUARTER) * SIXTEENTH + THIRTYTWOTH;
        leftLeg.pitch  = MathHelper.cos(distance + QUARTER)        * SIXTEENTH + THIRTYTWOTH;
        // 다리 Z (좌우 흔들림) — 원본 L547-548.
        rightLeg.roll  = MathHelper.cos(distance) * SIXTEENTH + THIRTYTWOTH;
        leftLeg.roll   = MathHelper.cos(distance) * SIXTEENTH - THIRTYTWOTH;
    }

    /**
     * [12-2] animateAngleJumping: 방향 점프 시 팔/다리 각도.
     * 원본: SmartMovingModel.animateAngleJumping() L559-584.
     *
     * 🔴 단순 LOCAL 매핑 (2026-04-27, 사용자 통찰 — "팔처럼 처리"):
     *   원본 L569-574 (LOCAL 각도, ZXY rotationOrder):
     *     leg.rotateAngleX = THIRTYTWOTH * (1 + ?ness);     // 다리 들어올림
     *     leg.rotateAngleY = -angle;                         // 다리 방향
     *     leg.rotateAngleZ = ±THIRTYTWOTH * backness;        // 다리 좌우 (back jump)
     *
     *   원본의 bipedPelvic.Y = -bipedOuter.Y + cameraAngle 보정은 1.7.10 SmartMoving 이
     *   별도로 entity.renderYawOffset = forwardRotation 으로 bodyYaw=cameraAngle force
     *   하기 때문에 동작. 1.21.1 vanilla 는 bodyYaw 자유 lerp → 그 보정을 leg.yaw 에
     *   추가하면 오히려 다리가 entity 와 분리 (사용자 보고 "다리 몸통 방향과 다름").
     *
     *   해결: 팔과 동일하게 LOCAL 각도 직접 set. cameraDelta 보정 제거.
     *   leg 는 entity.bodyYaw 따라 자연스럽게 회전 (vanilla setupTransforms).
     *
     *   ZXY rotationOrder 는 ModelPart 의 ZYX 와 미세 차이 (작은 pitch/roll ~π/16 시
     *   시각 거의 동일). gimbal lock 회피용 ModelPart 직접 set 패턴 유지.
     */
    private void sm_animateAngleJumping(SmartMovingClientState sm, float animationProgress) {
        float angle    = sm.angleJumpType * EIGHTH;
        float backness  = 1f - Math.abs(angle - HALF) / QUARTER;
        float leftness  = -Math.min(angle - HALF, 0f) / QUARTER;
        float rightness =  Math.max(angle - HALF, 0f) / QUARTER;

        // 🔴 head.yaw = 0 강제 (2026-04-27): vanilla setAngles 가 head.yaw = netHeadYaw =
        //   headYaw_lerped - bodyYaw_natural_lerped 로 매 프레임 set. angle jumping 시
        //   smBodyYawOverride=rotationYaw_lerped force 로 setupTransforms 는 cameraYaw 향하지만
        //   bodyYaw_natural 은 옆으로 lerp 진행 → netHeadYaw 가 0 아님 → head 가 entity 의
        //   반대 방향으로 회전 (사용자 보고 "좌 점프 시 고개 우로 돌아감"). 비행 매핑 패턴
        //   (sm_animateFlying head.yaw=0) 차용 — head.yaw 강제로 0 → setupTransforms force
        //   결과만 살아남아 head 가 cameraYaw 향함 (1.7.10 원본 효과 1:1).
        head.yaw = 0f;

        // 다리 — 팔과 동일한 LOCAL 매핑 (원본 L569-574 1:1).
        // 🔴 leg.roll 부호 반전 (2026-04-27 fix): 원본 ZXY (vertex 적용 R_z→R_x→R_y) 와
        //   ModelPart ZYX (R_x→R_y→R_z) 의 회전 순서 차이가 back jump (leg.yaw=-π) 시
        //   R_y(-π) 의 X 부호 반전으로 인해 R_z 적용 결과 정반대 → 다리가 가운데로 모임
        //   (사용자 보고). 부호 반전으로 1.7.10 원본과 동일한 "바깥쪽 기울임" 효과 복원.
        leftLeg.pitch  = THIRTYTWOTH * (1f + rightness);
        leftLeg.yaw    = -angle;
        leftLeg.roll   = -THIRTYTWOTH * backness;
        rightLeg.pitch = THIRTYTWOTH * (1f + leftness);
        rightLeg.yaw   = -angle;
        rightLeg.roll  =  THIRTYTWOTH * backness;

        // 팔 (원본 L579-583 1:1) + idle 진동 누적 (원본 SmartRenderModel.animateArms L334-339).
        // 🔴 idle 진동 (2026-04-27 fix): 원본 1.7.10 흐름 = animateAngleJumping 가 X/Z set
        //   후 imp.animateArms 가 += idle 진동 (cos*0.05+0.05 / sin*0.067) 누적. 1.21.1
        //   vanilla CrossbowPosing.swingArm 동등 식이 setAngles Step 12 에서 += 적용했으나
        //   우리 inject TAIL 의 = set 으로 cancel → "미세 진동 안 보임" (사용자 보고).
        //   해결: 우리 식에 idle 진동 += 누적해 1.7.10 의 `+=` 효과 1:1 재현.
        //   ageInTicks = animationProgress (vanilla CrossbowPosing 입력과 동일).
        float ageInTicks = animationProgress;
        float idleZ = MathHelper.cos(ageInTicks * 0.09f)  * 0.05f + 0.05f;
        float idleX = MathHelper.sin(ageInTicks * 0.067f) * 0.05f;

        leftArm.roll   = -SIXTEENTH * rightness - idleZ;     // 원본 leftArm.Z -= cos*0.05+0.05
        rightArm.roll  =  SIXTEENTH * leftness  + idleZ;     // 원본 rightArm.Z += cos*0.05+0.05
        leftArm.pitch  = -EIGHTH * backness - idleX;          // 원본 leftArm.X -= sin*0.067
        rightArm.pitch = -EIGHTH * backness + idleX;          // 원본 rightArm.X += sin*0.067
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
    private static float computeSmallOverGroundHeight(net.minecraft.client.network.AbstractClientPlayerEntity player, World world) {
        // 🔴 사용자 보고 fix (2026-05-04 — "다리가 처음부터 모임"):
        //   원본 SmartMovingBase.getOverGroundHeight: bb.minY - getMaxPlayerSolidBetween(minY-5, minY, 0).
        //   getMaxPlayerSolidBetween = 박스 horizontal area (= 0.6 x 0.6) 안 max solid Y.
        //   이전 매핑은 single column (player.x/z floor) 만 → 등반 블록이 column 밖이면 측정 못 함.
        //   fix: SmartMovingClientState.getMaxPlayerSolidBetween 사용 (= horizontal area 정확).
        net.minecraft.util.math.Box bb = player.getBoundingBox();
        double minY = bb.minY;
        double solidTop = SmartMovingClientState.getMaxPlayerSolidBetween(player, minY - 5D, minY, 0);
        return (float) Math.max(0D, minY - solidTop);
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
     *
     * ⚠️ BUG 가능성 (메모리 `feedback_zxy_zyx_rotation_order.md` 의 BUG-30 패턴 동일):
     *   원본 ModelRotationRenderer YZX 분기 (L145/L151/L154) GL call 순서 = X → Z → Y.
     *   GL post-multiply: vertex 적용 = call 역순 = Y → Z → X.
     *   현재 q = q_Y*q_Z*q_X → vertex 적용 X → Z → Y (원본 반대).
     *   사용처: 그랩 클라이밍 (`project_grab_climbing_complete.md` 16라운드 정착) — 함부로 수정 금지.
     *   crawl 분기는 정확 매핑 위해 `setAnglesYZX_v2` 별도 사용.
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
     * 원본 YZX rotationOrder 의 정확 1:1 매핑 — vertex 적용 순서 Y → Z → X.
     *
     * 원본 ModelRotationRenderer YZX (L145, L151, L154) GL call 순서 = X → Z → Y.
     * GL post-multiply: vertex 적용 = call 역순 = Y → Z → X.
     * JOML: q = q_X * q_Z * q_Y → vertex 적용 right-most first = Y → Z → X. ✓
     *
     * 사용자 보고 fix (2026-05-03 — "엎드린 자세 팔이 하늘로 들림"):
     *   crawl arm yaw=±90° (= 큰 회전) 시 setAnglesYZX (= q_Y*q_Z*q_X) 의 vertex 적용 X → Z → Y
     *   가 원본 vertex 적용 Y → Z → X 와 정반대 → arm 위치 잘못. 메모리 BUG-30 (setAnglesXZY)
     *   와 동일 패턴. crawl 만 별도 함수로 정확 매핑 — 다른 사용처 (climbing/sliding) 영향 없음.
     */
    private static void setAnglesYZX_v2(ModelPart part, float pitch, float yaw, float roll) {
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
     * 부모 setupTransforms POSITIVE_X(-theta) 회전을 자식 ModelPart 단계에서 정확히 cancel.
     *
     * vanilla ModelPart.rotate: matrices.multiply(Quaternionf.rotationZYX(roll, yaw, pitch))
     *   → 자식 회전 q_arm = R_z(roll) * R_y(yaw) * R_x(pitch) (vertex 에 X 가 가장 먼저 적용).
     * 부모 R_x(-theta) 적용 후 자식 q_arm → 결과 R_x(-theta) * q_arm * v.
     * 원하는 효과 = q_arm * v (부모 X 회전 무시) → 새 자식 q_new = R_x(theta) * q_arm.
     * ZYX Euler 분해 → (pitch, yaw, roll).
     *
     * 단순 `pitch += theta` 는 회전 비교환성으로 yaw/roll 이 0 이 아닐 때 부정확
     * (vanilla animateArms 의 roll = sin(swing*π)*-0.4 ~ -23° 와 합성 시 오차).
     *
     * 사용처: 비행 swing 진행 중 preferred arm — 직립 자세로 vanilla swing 효과 잔존.
     */
    private static void preCancelParentXRotation(ModelPart part, float theta) {
        // 🔴 (세션 65q): 부호 반전 + scale 보정.
        //   vanilla render: setupTransforms → scale(-1,-1,1) → ModelPart.rotate.
        //   scale Y 부호 반전 → ModelPart R_x(theta) = world R_x(-theta).
        //   부모 world R_x(-θ) cancel 위해 ModelPart R_x(-theta) 적용 (= world R_x(+θ)).
        Quaternionf qOrig = new Quaternionf().rotationZYX(part.roll, part.yaw, part.pitch);
        Quaternionf qNew = new Quaternionf().rotationX(-theta).mul(qOrig);
        Vector3f e = qNew.getEulerAnglesZYX(new Vector3f());
        part.pitch = e.x;
        part.yaw   = e.y;
        part.roll  = e.z;
    }

    /**
     * arm pivot 위치를 부모 X 회전 cancel — 회전 중심 ModelPart (0, 0, 0).
     *
     * vanilla render 흐름: setupTransforms(R_x(-θ) at world (0,1.5,0)) → scale(-1,-1,1) →
     * translate(0, -1.501, 0) → ModelPart.rotate.
     * 회전 중심 (0, 1.5, 0) world 가 ModelPart 좌표계에서 ≈ (0, 0, 0) (scale + translate 보정).
     * scale Y 부호 반전 → ModelPart R_x(-theta) = world R_x(+theta) → 부모 cancel.
     */
    private static void preCancelParentXPivot(ModelPart part, float theta) {
        float py = part.pivotY;
        float pz = part.pivotZ;
        float cos = MathHelper.cos(theta);
        float sin = MathHelper.sin(theta);
        part.pivotY = py * cos + pz * sin;
        part.pivotZ = -py * sin + pz * cos;
    }

    /**
     * setupTransforms 의 fade 보간 (lerpFadeAngle) 과 동일한 계산.
     *
     * sm_setAngles 가 setupTransforms 보다 먼저 호출되므로, 같은 frame 의 fade 결과를
     * setAngles 시점에 직접 계산해야 정확한 X cancel 이 가능. setupTransforms 도 같은
     * input (smOuterTiltX_prev, smOuterFade_prevTime) 으로 같은 결과 산출 후 prev 갱신.
     *
     * 원본 ModelRotationRenderer.GetIntermediateAngle (L347-L364) 1:1.
     */
    private static float lerpFadeAngle(float prev, float target, float prevTime, float currentTime) {
        if (prevTime < -100f) return target;
        float deltaTime = currentTime - prevTime;
        if (deltaTime > 2f || deltaTime < 0f) return target;
        if (target == prev) return target;
        final float W = (float) (2.0 * Math.PI);
        final float H = (float) Math.PI;
        float p = prev, s = target;
        while (p >= W) p -= W;
        while (p < 0f) p += W;
        while (s >= W) s -= W;
        while (s < 0f) s += W;
        if (s > p && (s - p) > H) p += W;
        if (s < p && (p - s) > H) s += W;
        return p + (s - p) * deltaTime * 0.2f;
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
