package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClimber;
import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.client.SmartMovingFlyer;
import choco.ratel.smartmoving.client.SmartMovingJumper;
import choco.ratel.smartmoving.client.SmartMovingSlider;
import choco.ratel.smartmoving.client.SmartMovingSwimmer;
import choco.ratel.smartmoving.climbing.ClimbGap;
import choco.ratel.smartmoving.climbing.FeetClimbing;
import choco.ratel.smartmoving.climbing.HandsClimbing;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 5-1 (클라이언트): travel() 인터셉트 — SM 클라이밍 시 vanilla 취소 + SM 물리 적용
 * 5-2: jump() 인터셉트 — jumpAvoided/jumpPending 세팅 + vanilla 취소
 * 5-3: jumpingCooldown 우회 — jump() 취소로 10틱 쿨다운 발동 자체 차단
 * 5-4: jumping 필드 @Shadow — SM 조건 필터 적용 시 참조 (tickEssential 내부)
 * 5-5 (클라이언트): isClimbing() 오버라이드 — SM 클라이밍 중 false 반환
 * 5-6: applyClimbingSpeed() 취소 — SM 클라이밍 중 x/z ±0.15F 클램프 이중 차단
 * 6-2 (클라이언트): isInSwimmingPose() 오버라이드 — SM 크롤링 중 false 반환
 */
@Mixin(LivingEntity.class)
@Environment(EnvType.CLIENT)
public abstract class MixinLivingEntityClient {

    /**
     * 5-1 (클라이언트): travel() 인터셉트 — SM 클라이밍 물리 파이프라인.
     *
     * 클라이밍 가능 표면(사다리/넝쿨) 탐지 시 vanilla travel()을 취소하고
     * SM 클라이밍 물리를 직접 적용한다.
     *
     * 적용 물리:
     *   - handleClimbing(): Y 속도 설정 + fallDistance 리셋
     *   - handleCeilingClimbing(): 천장 클라이밍 조건 처리
     *   - x/z 감속: 0.91F 배율 (공기 저항)
     *   - motionY 하한: max(motionY, -0.15D) (과도한 낙하 방지)
     *
     */
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void sm_travel_client(Vec3d movementInput, CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        SmartMovingConfig cfg = SmartMovingConfig.Config;

        // 비호환 상태 차단 (원본: Compat.isBlockedByIncompatibility — isSpectator + isElytraFlying)
        // 1.7.10 Compat 스펙테이터: Et Futurum Requiem 전용 → 1.21.1은 vanilla isSpectator()
        // 1.7.10 Compat 엘리트라: Et Futurum Requiem IElytraPlayer → 1.21.1은 vanilla isFallFlying()
        if (player.isSpectator() || player.isFallFlying()) return;

        // [11-4] 비행 억제 — SM 비행 비활성화 시 vanilla creative 비행 motionY 감쇠
        // 1.21.1: jumpMovementFactor 필드 없음 → getOffGroundSpeed() 메서드(PlayerEntity) (A-30 확인)
        // C-41: getOffGroundSpeed() @HEAD Mixin으로 0.05F 반환하여 공중 이동 억제
        if (player.getAbilities().flying && !cfg.fly) {
            Vec3d vel = player.getVelocity();
            player.setVelocity(vel.x, vel.y * 0.5999755859375D, vel.z);
        }

        // [10-1] 점프 판정 — 수영 체크 전 매 틱 실행
        SmartMovingJumper.handleJumping(player, sm);

        // 원본 SmartMovingSelf.superMoveEntityWithHeading L97-L100: 진입 직후 isSwimming/isDiving 스냅샷.
        // handleSwimming 의 isFakeShallowWaterSneaking 판정(원본 L243) + fromSwimmingOrDiving 전환 감지에 사용.
        boolean wasSwimming = sm.isSwimming_sm;
        boolean wasDiving   = sm.isDiving;
        boolean wasShortInWater = wasSwimming || wasDiving;

        // [8-1] 매 틱 수영 상태 3분류 갱신
        SmartMovingSwimmer.updateSwimState(player, sm);

        // [8-2] 수중 이동 처리 — SM이 처리하면 vanilla travel() 취소
        if (SmartMovingSwimmer.handleSwimming(player, sm, movementInput, this.jumping,
                                               wasSwimming, wasDiving)) {
            ci.cancel();
            return;
        }

        // 원본 handleLand L648: `!handledSwimming && !grabButton.Pressed` 조건에서 호출.
        // 1.21.1 은 handleSwimming=false 도달 시점이 원본 `!handledSwimming` 대응 — 여기서 호출.
        // grab 분기(원본 L1354 landMotionPost)는 현재 단일 호출 지점으로 통합(효과 동일).
        sm.fromSwimmingOrDiving(player, wasShortInWater);

        // [9-4] 슬라이딩 처리 — SM이 처리하면 vanilla travel() 취소
        if (SmartMovingSlider.handleSliding(player, sm)) {
            ci.cancel();
            return;
        }

        // [10-4] 헤드점프 착지 감지 — 포즈 복원
        if (player.isOnGround() && sm.isHeadJumping) {
            SmartMovingJumper.resetHeightOffset(player, sm);
        }

        // [10-5] 벽점프 처리 (클라이밍 전)
        // ── 매 틱 벽점프 상태 리셋 (G-01) ─────────────────────────────────
        // 원본: resetState() this.isWallJumping = false → handleWallJumping()이 조건 충족 시 재세팅
        sm.isWallJumping = false;
        // 원본 SmartMovingSelf L2863-2897: canWallJumping/wallJumpCount/triggerWallJumping/
        // wantWallJumping 갱신을 handleWallJumping 진입 직전에 수행.
        SmartMovingJumper.updateWallJumpState(player, sm);
        SmartMovingJumper.handleWallJumping(player, sm);

        // ── 매 틱 클라이밍 상태 리셋 (G-01) ─────────────────────────────────
        // 원본: SmartMovingSelf.resetClimbing() + resetState() 호출 (updateEntityActionState 상단)
        // 클라이밍 표면을 벗어나도 상태가 true로 남는 버그 방지
        sm.isClimbing        = false;
        sm.isCrawlClimbing   = false;
        sm.isCeilingClimbing = false;
        sm.isClimbJumping    = false;
        sm.isClimbHolding    = false;
        sm.isClimbCrawling   = false;
        sm.isClimbBackJumping = false;

        // [11-5] SM 비행 물리 — isFlying && cfg.fly 시 pitch 방향 3D 이동
        // 원본: handleAlternativeFlying() → !handledSwimming && !handledLava && isFlying && Config.isFlyingEnabled()
        // 수영/슬라이딩이 이미 ci.cancel()로 반환됐으므로 이 지점 = !handledSwimming && !handledLava 조건 충족
        if (SmartMovingFlyer.handleFlying(player, sm, movementInput, this.jumping)) {
            ci.cancel();
            return;
        }

        // [5-1] 클라이밍 처리
        World world = player.getWorld();
        boolean isSmall = sm.isSmall || sm.isCrawling;

        HandsClimbing[] hands = {HandsClimbing.NONE};
        FeetClimbing[]  feet  = {FeetClimbing.NONE};
        ClimbGap[] handsGap   = {new ClimbGap()};
        ClimbGap[] feetGap    = {new ClimbGap()};
        boolean[] handsVine = {false};
        boolean[] feetVine  = {false};
        SmartMovingClimber.getOnLadderOrVine(player, world, isSmall, false, hands, feet, handsGap, feetGap, handsVine, feetVine);

        // R-01: 클라이밍 타입을 State 패킷 인코딩용으로 저장
        sm.actualHandsClimbType = hands[0].ordinal();
        sm.actualFeetClimbType  = feet[0].ordinal();
        sm.isHandsVineClimbing  = handsVine[0];
        sm.isFeetVineClimbing   = feetVine[0];

        boolean onClimbable = hands[0].isRelevant() || feet[0].isRelevant();
        if (!onClimbable && !sm.isCeilingClimbing) return;

        if (onClimbable) SmartMovingClimber.handleClimbing(player, sm);
        SmartMovingClimber.handleCeilingClimbing(player, sm);

        Vec3d vel = player.getVelocity();
        player.setVelocity(vel.x * 0.91F, Math.max(vel.y, -0.15D), vel.z * 0.91F);
        ci.cancel();
    }

    /**
     * 5-4: LivingEntity.jumping (field_6282).
     * SM 조건 필터가 적용될 때 이 필드를 false로 강제 설정한다.
     * sm_jumpingFilter()에서 조건 체크 후 false 설정.
     */
    @Shadow protected boolean jumping;

    /**
     * 5-4: jumping 필드 억제 — SM 상태 조건 필터.
     * 원본: SmartMovingSelf.updateEntityActionState() isp.setIsJumpingField() 로직.
     *
     * 억제 조건:
     *   isCrawling || isSliding || isHeadJumping || jumpCharge>0 || blockJumpTillButtonRelease
     *
     * LivingEntity.tickMovement() HEAD에서 실행 → travel() 내 jump() 호출 전에 확실히 세팅됨.
     */
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void sm_jumpingFilter(CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isCrawling || sm.isSliding || sm.isHeadJumping
                || sm.jumpCharge > 0 || sm.blockJumpTillButtonRelease) {
            this.jumping = false;
        }
    }

    /**
     * 5-2 + 5-3: jump() 인터셉트.
     *
     * ClientPlayerEntity에 대해서만 동작한다.
     * SM이 vanilla jump()를 차단하고 jumpPending 플래그를 세팅한다.
     * 실제 점프 처리는 tickEssential() → handleJumping() → tryJump()에서 수행된다.
     *
     * vanilla jump() 차단 부작용:
     *   - addExhaustion(0.05F * 2): SM 자체 소진 시스템으로 대체
     *   - Stats.JUMP 통계: SM tryJump()에서 별도 기록
     *   - 스프린트 점프 +0.2F: SM tryJump()에서 자체 계산
     *   - jumpingCooldown=10 (5-3): jump() 미실행으로 자동 차단
     */
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void sm_jump(CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        sm.jumpAvoided = true;
        sm.jumpPending = true;
        ci.cancel();
    }

    /**
     * 5-5 (클라이언트): SM 커스텀 클라이밍 중 isClimbing() = false 강제.
     *
     * 목적 1: applyClimbingSpeed() x/z ±0.15F 클램프 차단
     * 목적 2: applyMovementInput()에서 isClimbing() 시 y=0.2 강제 적용 차단
     *
     * ClientPlayerEntity 인스턴스 확인 → SmartMovingClientState 상태 기반 판정.
     * 서버 측 동등 로직 → MixinLivingEntity.sm_isClimbing_server()
     */
    @Inject(method = "isClimbing", at = @At("HEAD"), cancellable = true)
    private void sm_isClimbing_client(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isClimbing || sm.isCrawlClimbing || sm.isCeilingClimbing) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 5-6: applyClimbingSpeed() 이중 차단 — SM 클라이밍 시 Vec3d 그대로 반환.
     * Yarn: applyClimbingSpeed (intermediary: method_18801 확인 완료)
     *
     * 5-5(isClimbing=false)로 이미 applyClimbingSpeed 내부 클램프가 차단되지만,
     * applyClimbingSpeed가 isClimbing()을 우회해 직접 호출되는 경로 대비 이중 방어.
     */
    @Inject(method = "applyClimbingSpeed", at = @At("HEAD"), cancellable = true)
    private void sm_applyClimbingSpeed(Vec3d velocity, CallbackInfoReturnable<Vec3d> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isClimbing || sm.isCrawlClimbing || sm.isCeilingClimbing) {
            cir.setReturnValue(velocity);
        }
    }

    /**
     * 9-3: 크롤링/슬라이딩 중 LimbAnimator 갱신 억제.
     * vanilla updateLimbs()는 보행 진행 속도로 LimbAnimator를 갱신하는데,
     * 크롤링/슬라이딩 포즈에서 이를 그대로 실행하면 다리 애니메이션이 비정상 재생된다.
     */
    @Inject(method = "updateLimbs(Z)V", at = @At("HEAD"), cancellable = true)
    private void sm_updateLimbs_client(boolean serverSide, CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isCrawling || sm.isSliding) {
            ci.cancel();
        }
    }

    /**
     * HorizontalAerodynamicDamping — 슬라이드→헤드점프 전환 직후 수평 감속 보정.
     * 원본: SmartMovingBase.landMotion() (행 751-755) — isAerodynamic && !onGround && !isSliding 시
     *       motionX/Z *= HorizontalAerodynamicDamping(0.999F)
     *
     * 1.21.1: vanilla travel() 내 공기저항 0.91F 이후에 삽입.
     * isAerodynamic=true일 때 vanilla가 적용한 0.91F를 되돌리고 0.999F를 적용.
     * correction factor = 0.999F / 0.91F
     */
    @Inject(method = "travel", at = @At("TAIL"))
    private void sm_aerodynamicDamping(Vec3d movementInput, CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!cfg.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (!sm.isAerodynamic || player.isOnGround()) return;
        float factor = 0.999F / 0.91F;
        Vec3d vel = player.getVelocity();
        player.setVelocity(vel.x * factor, vel.y, vel.z * factor);
    }

    /**
     * 6-2 (클라이언트): SM 크롤링 중 isInSwimmingPose() = false 강제.
     *
     * SM 크롤링은 SWIMMING 포즈를 사용하지만, vanilla isInSwimmingPose()=true가 되면:
     *   - setupTransforms Branch 2: X-90° 회전 + translate(0,-1,0.3) → 렌더 깨짐
     *   - updateLeaningPitch(): leaningPitch가 1.0까지 상승 → 자동 회전 발생
     *   - animateModel(): model.leaningPitch 반영 → setAngles 전 pitch 왜곡
     *
     * 클라이언트 측: ClientPlayerEntity 인스턴스 확인.
     * 서버 측 → MixinLivingEntity.sm_isInSwimmingPose_server()
     * Yarn: isInSwimmingPose (intermediary: method_20232 확인 완료)
     */
    @Inject(method = "isInSwimmingPose", at = @At("HEAD"), cancellable = true)
    private void sm_isInSwimmingPose_client(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        // [6-2][8-3] 크롤링/수영/잠수/크롤클라이밍 중 setupTransforms Branch 2 진입 차단.
        // isCrawling/isCrawlClimbing: SWIMMING 포즈 사용하되 vanilla -90° 자동 회전 방지
        // isSwimming_sm/isDiving: SM 자체 body X 기울기 애니메이션과 충돌 방지
        if (sm.isCrawling || sm.isCrawlClimbing || sm.isSwimming_sm || sm.isDiving) {
            cir.setReturnValue(false);
        }
    }
}
