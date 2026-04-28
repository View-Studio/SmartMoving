package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClimber;
import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.client.SmartMovingFlyer;
import choco.ratel.smartmoving.client.SmartMovingJumper;
import choco.ratel.smartmoving.client.SmartMovingMover;
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
import net.minecraft.entity.MovementType;
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

        // BUG-11 (세션 36): SM disabled 시 travel 전체 작동 안 함 → vanilla 정상.
        //   기존: cfg.enabled 가드 없이 L67 vanilla flying motionY 0.6 감쇠 작동 → SM disabled
        //   상태에서도 creative 비행 약화 (50%). 또한 handleSwimming/handleLava/handleSliding/
        //   handleFlying/climbing 등 모든 SM 분기 진입 가능 (sm.* 필드는 false 이지만 무용 계산).
        //   해결: cfg.enabled false 면 즉시 return — vanilla travel 정상 작동.
        if (!cfg.enabled) return;

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

        // B-7a (세션 127): 원본 SmartMovingSelf L132 `isLiquidClimbing` 지역 변수 승격.
        //   isLiquidClimbing = Config.isFreeClimbingEnabled() && sp.fallDistance <= 3.0
        //                   && wantClimbUp && sp.isCollidedHorizontally && !isDiving;
        // updateSwimState L232 진입 조건 `!isLiquidClimbing` 항 활성화용 (B-7c 에서 복원).
        sm.isLiquidClimbing = (cfg.freeClimb && cfg.enabled)
                            && player.fallDistance <= 3.0
                            && sm.wantClimbUp
                            && player.horizontalCollision
                            && !sm.isDiving;

        // [8-1] 매 틱 수영 상태 3분류 갱신
        SmartMovingSwimmer.updateSwimState(player, sm);

        // [8-2] 수중 이동 처리 — SM이 처리하면 vanilla travel() 취소
        if (SmartMovingSwimmer.handleSwimming(player, sm, movementInput, this.jumping,
                                               wasSwimming, wasDiving)) {
            ci.cancel();
            return;
        }

        // [8-2b] lava 이동 처리 (Phase C-3, 포커스 #2.6 세션 4) — handleSwimming false 분기.
        //   원본 SmartMovingSelf L578-L600 `handleLava` 1:1 이식.
        //   진입 조건 (원본 L580): !isFlying && !handledSwimming && !isLiquidClimbing && isInLava
        //   - lavaLikeWater=true (Creative): handleSwimming 이 lava 처리 → 여기 진입 X
        //   - lavaLikeWater=false (Survival): handleSwimming 통과 → 여기 진입 → SM 자체 lava motion
        //   부작용 0 (전수 조사 완료): swimUpward / damage / sound / particle 모두 travel() 외부
        if (SmartMovingSwimmer.handleLava(player, sm, movementInput)) {
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
        // 🔴 isClimbCrawling 은 SmartMovingClientState L1877 메인 식이 매 틱 갱신 + 진입/해제
        //   엣지 (L1880-1908) 가 heightOffset 처리. G-01 강제 reset 하면 wasClimbCrawling 추적
        //   불가 → 해제 엣지 미발동 → heightOffset=-1 잔존 → vanilla CROUCHING 자세에 박스
        //   잔존이 결합되어 사용자 보고 "엎드리기" 자세. 원본 resetClimbing (L1474-1486) 도
        //   isClimbCrawling 미포함 → 1:1 일치하도록 라인 삭제.
        // sm.isClimbCrawling   = false;
        sm.isClimbBackJumping = false;

        // [11-5] SM 비행 물리 — isFlying && cfg.fly 시 pitch 방향 3D 이동
        // 원본: handleAlternativeFlying() → !handledSwimming && !handledLava && isFlying && Config.isFlyingEnabled()
        // 수영/슬라이딩이 이미 ci.cancel()로 반환됐으므로 이 지점 = !handledSwimming && !handledLava 조건 충족
        if (SmartMovingFlyer.handleFlying(player, sm, movementInput, this.jumping)) {
            ci.cancel();
            return;
        }

        // **포커스 #4 B-4 (세션 3) — 결함 #2 정정**: vanilla Creative 비행 + sprint+jump 가속.
        // 원본 SmartMovingSelf L633-L640 `handleLand` 진입 시 `!handledAlternativeFlying` 분기:
        //   if (esp.movementInput.jump && Config.isSprintingEnabled() && sprintButton.Pressed
        //       && sp.capabilities.isFlying)
        //       sp.motionY += sprintFactorLevitate * sprintFactorLevitateVertical;
        //
        // 1.21.1 매핑 위치: Flyer.handleFlying 가 false 반환 후 (= SM 비행 비활성, vanilla
        // Creative 비행 활성 가능 시점). cfg.sprint && abilities.flying && jumping && sprint 키.
        // 매 tick 적용 — jump 키 hold 중 motionY 가속 (기본 1.5 * 0.185 = 0.2775F).
        if (cfg.sprint && this.jumping
                && net.minecraft.client.MinecraftClient.getInstance().options.sprintKey.isPressed()
                && player.getAbilities().flying) {
            net.minecraft.util.math.Vec3d _v = player.getVelocity();
            double _boost = cfg.sprintFactorLevitate * cfg.sprintFactorLevitateVertical;
            player.setVelocity(_v.x, _v.y + _boost, _v.z);
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
        // 🔴 (2026-04-27) Free Climb (일반 벽 grab) 진입 게이트 — 사용자 보고
        //   "그랩 키 클라이밍 작동 안 함" 의 핵심 원인.
        //   원본 SmartMovingSelf L657 은 handleClimbing 무조건 호출. 그 안의 8방향
        //   seekClimbGap 검사로 일반 벽도 detect → handsClimbing/feetClimbing 갱신.
        //   우리는 onClimbable (사다리/덩굴 인접) 만 가드라 일반 벽에서 진입 불가.
        //   sm.wantClimb 는 tickEssential L1262 에서 `freeClimb && enabled && wouldWantClimb`
        //   (= grab 키 또는 자동사다리/덩굴 검증) 으로 매 tick 갱신. 추가하면 일반 벽에서도
        //   handleClimbing 진입 → 안의 seekClimbGap 8방향 검사 발동 → Free Climb 정상.
        boolean wantFreeClimb = sm.wantClimb;
        if (!onClimbable && !sm.isCeilingClimbing && !wantFreeClimb) {
            // 🔴 (2026-04-28) Land 분기 — 비행과 동일 패턴 (vanilla travel 통째로 cancel + SM 자체 식).
            //   원본 SmartMovingSelf.handleLand + landMotion + setLandMotions 1:1 매핑.
            //   사용자 의도 "비행처럼 1:1": vanilla 흐름 + 부분 inject 의 곱셈 누적 문제 근본 해결.
            SmartMovingMover.handleLand(player, sm, movementInput);
            ci.cancel();
            return;
        }

        if (onClimbable || wantFreeClimb) SmartMovingClimber.handleClimbing(player, sm);
        SmartMovingClimber.handleCeilingClimbing(player, sm);

        // 클라이밍 미발동 시도 land 분기로.
        if (!sm.isClimbing && !sm.isCeilingClimbing) {
            SmartMovingMover.handleLand(player, sm, movementInput);
            ci.cancel();
            return;
        }

        // 🔴 (2026-04-28) 원본 SmartMovingSelf.handleLand + landMotion 정밀 1:1 매핑.
        //   원본 흐름:
        //     1. landMotion (L675-810): horizontalDamping 결정 + moveFlying (movementInput → motion).
        //     2. move (L655): vanilla move() — 위치 갱신.
        //     3. handleClimbing (L657): motionY 새로 set.
        //     4. handleCeilingClimbing.
        //     5. setLandMotions (L659): motionY -= 0.08, *= 0.98, horizontalDamping.
        //
        //   우리는 HEAD inject 라 순서 변경:
        //     a. handleClimbing (motionY set) — 이미 호출됨.
        //     b. movementInput → motion (원본 L718 moveFlying 등가).
        //     c. ladder/vine 시 motion clamp -0.15 ~ 0.15 (원본 L757-775).
        //     d. notTotalFreeClimbing 시 fallDistance=0 + vertical clamp (원본 L776-781).
        //     e. sneak 시 motionY=0 (원본 L782-794).
        //     f. setLandMotions (원본 L1176-1182): motionY -= 0.08, *= 0.98, horizontal *= 0.91.
        //     g. player.move (원본 L655 등가).

        // b. 🔴 (2026-04-28) 원본 L718 `sp.moveFlying(strafe, forward, rawSpeed * speedFactor)` 1:1.
        //   기존 `0.1F` hardcoded 는 vanilla 1.21.1 walk 속도. 사용자 보고 "원본보다 빠름".
        //   원본 식: rawSpeed * speedFactor.
        //     rawSpeed = onGround ? 0.1 * f3 : jumpMovementFactor / sprintDiv.
        //     speedFactor = configFactor * potionFactor * nonSlowFactor * slowFactor.
        //     + landMotion runFactor (isRunning && !isFast).
        //     + !onGround 시 /= potionFactor.
        //
        //   handleLand 와 동일 식.
        SmartMovingConfig cfg2 = SmartMovingConfig.Config;
        float climbSpeedFactor = SmartMovingMover.getConfigSpeedFactor(player, cfg2)
                * SmartMovingMover.getPotionSpeedFactor(player)
                * SmartMovingMover.getNonSlowInputSpeedFactor(player, sm, cfg2);
        float climbSlowFactor = SmartMovingMover.getSlowInputSpeedFactor(player, sm, cfg2);
        if (sm.vanilla()) {
            // vanilla mode 시 movementInput 자체 곱 — 본 매핑 무관 (cfg.enabled 일 때 vanilla=false).
        } else {
            climbSpeedFactor *= climbSlowFactor;
        }
        // damping/rawSpeed 결정.
        float climbDamping;
        if (player.isOnGround() && (!sm.isJumping || sm.vanilla())) {
            net.minecraft.block.BlockState below = player.getWorld().getBlockState(player.getSteppingPos());
            float slip = below.getBlock().getSlipperiness();
            climbDamping = slip > 0F ? slip * 0.91F : 0.546F;
        } else {
            climbDamping = 0.91F;
        }
        float climbF3 = 0.1627714F / (climbDamping * climbDamping * climbDamping);
        float climbRawSpeed;
        if (player.isOnGround()) {
            climbRawSpeed = 0.1F * climbF3;
        } else {
            float jumpMovementFactor = 0.02F;
            climbRawSpeed = jumpMovementFactor / (player.isSprinting() && !player.getAbilities().flying ? 1.3F : 1F);
        }
        // runFactor (isRunning && !isFast).
        if (cfg2.run && SmartMovingMover.isRunning(player, sm, cfg2) && !sm.isFast) {
            climbSpeedFactor *= cfg2.runFactor;
        }
        // !onGround 시 potionFactor 정상화.
        if (!player.isOnGround()) {
            float potion = SmartMovingMover.getPotionSpeedFactor(player);
            if (potion > 0F) climbSpeedFactor /= potion;
        }
        player.updateVelocity(climbRawSpeed * climbSpeedFactor, movementInput);

        // c. 원본 L757-775: ladder/vine 시 motion clamp -0.15 ~ 0.15.
        //   onClimbable=true (사다리/덩굴 인접) 시만 적용. free climb 일반 벽엔 미적용.
        if (onClimbable) {
            Vec3d v = player.getVelocity();
            double clampH = 0.15D;
            double mx = Math.max(-clampH, Math.min(clampH, v.x));
            double mz = Math.max(-clampH, Math.min(clampH, v.z));
            if (mx != v.x || mz != v.z) {
                player.setVelocity(mx, v.y, mz);
            }
        }

        // d. fallDistance reset (사다리/덩굴 매달림 시 낙하 데미지 방지)
        if (onClimbable) {
            player.fallDistance = 0;
        }

        // f. 원본 setLandMotions L1176-1182:
        //   sp.motionY -= 0.08;  sp.motionY *= 0.98;
        //   sp.motionX *= horizontalDamping;  (= 0.91F air damping)
        //   sp.motionZ *= horizontalDamping;
        Vec3d vel = player.getVelocity();
        double newY = (vel.y - 0.08D) * 0.98D;
        player.setVelocity(vel.x * 0.91F, newY, vel.z * 0.91F);

        // 🔴 사다리/덩굴 motion 가드 — 원본 L757-794 (landMotion) 1:1.
        //   원본 흐름: landMotion (motion 가드) → move (player.move) → setLandMotions.
        //   우리 흐름: setLandMotions → player.move (역순). 따라서 motion 가드는
        //   setLandMotions 후 player.move 전 위치에서 적용 → player.move 시점 motion 보장.
        //
        //   가드 1 (L757-775): motionX/Z ±0.15 clamp.
        //   가드 2 (L779-780): fallDistance=0 + motionY = max(motionY, -0.15*factor).
        //   가드 3 (L782-794): sneak 시 motionY=0 (떨어지지 않게).
        if (onClimbable) {
            Vec3d v = player.getVelocity();
            double clampH = 0.15D;
            double mx = Math.max(-clampH, Math.min(clampH, v.x));
            double mz = Math.max(-clampH, Math.min(clampH, v.z));
            double my = v.y;

            // 🔴 sneak 가드 — 원본 L782-794 1:1 매핑.
            //   원본은 esp.movementInput.sneak (input source) 직접 사용.
            //   우리 매핑이 player.isSneaking() (SNEAKING flag) 사용 → 자세 차단
            //   (input.sneaking=false 강제) 과 충돌 (motion hold 미활성).
            //   해결: vanilla sneak 키 직접 체크 (자세 차단과 독립).
            boolean sneakKeyPressed = net.minecraft.client.MinecraftClient.getInstance()
                    .options.sneakKey.isPressed();
            if (sneakKeyPressed && my < 0) {
                my = 0;
            } else {
                // sneak 아닐 때 vertical clamp -0.15*factor.
                double clampFactor = -0.15D * SmartMovingMover.getCombinedSpeedFactor(
                        player, SmartMovingConfig.Config);
                if (my < clampFactor) my = clampFactor;
            }

            if (mx != v.x || mz != v.z || my != v.y) {
                player.setVelocity(mx, my, mz);
            }
            player.fallDistance = 0;  // setLandMotions 후 다시 한번 안전 장치
        }

        // g. 원본 L655 등가: vanilla move() 호출로 위치 갱신.
        player.move(MovementType.SELF, player.getVelocity());

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
        // BUG-18 (세션 36): SM disabled 시 jumping 필터 안 함 → vanilla jumping 그대로 보존.
        //   기존: sm.* 자체가 disabled 시 false 라 영향 거의 없으나 명시성 + jumpCharge/
        //   blockJumpTillButtonRelease 잔존 가능성 차단 (resetState 가 reset 하지만 안전망).
        if (!SmartMovingConfig.Config.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isCrawling || sm.isSliding || sm.isHeadJumping
                || sm.jumpCharge > 0 || sm.blockJumpTillButtonRelease) {
            this.jumping = false;
        }
        // 🔴 누락 정정 (Flying Phase F-6 / 세션 40): 비행 중 fallDistance reset.
        //   원본 SmartMovingSelf L1803-L1804: `if (sp.capabilities.isFlying) sp.fallDistance = 0F;`
        //   1.21.1 매핑 부재 → 비행 종료 직후 착지 시 누적 fallDistance 로 낙하 데미지 가능.
        if (player.getAbilities().flying) {
            player.fallDistance = 0F;
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
        // 🔴 BUG-17 (세션 36): SM disabled 시 vanilla jump() 차단 안 함 → vanilla 점프 정상 작동.
        //   기존: cfg.enabled 가드 부재 → SM disabled 시에도 ci.cancel() 으로 vanilla 점프
        //   완전 차단 + jumpAvoided/jumpPending 세팅 → 점프 자체 작동 안 함 (사용자 보고 직접 원인).
        //   해결: cfg.enabled false 시 즉시 return → vanilla 점프 ci.cancel 안 됨 → 정상 작동.
        if (!SmartMovingConfig.Config.enabled) return;
        // 🔴 BUG-28 정정 (Flying Phase / 세션 42): vanilla 1.21.1 ClientPlayerEntity.tickMovement
        //   디컴파일 L791-L800 — double-tap fly 진입 시 isOnGround=true 면 this.jump() 직접
        //   호출 (1.21.1 특화 코드). 원본 1.7.10 EntityPlayerSP 에는 없음.
        //   기존: 가드 부재 → 첫 비행 진입 (지면에서 더블탭) 시 sm_jump 발화 → jumpAvoided=true →
        //   handleJumping 의 tryJump(UP) → motionY=0.42 + 매 틱 vanilla 비행 boost +0.15 +
        //   SM handleFlying = 첫 비행 무한 상승 (사용자 BUG-28 직접 원인).
        //   원본 1.7.10 의 vanilla jump() 호출 없음 = handleJumping 에 비행 가드 불필요.
        //   해결: 1.21.1 차이 보정 — vanilla 비행 중 sm_jump 처리 skip → 원본 1:1 동작.
        if (player.getAbilities().flying) return;
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
        // BUG-23 (세션 36): SM disabled 시 vanilla isClimbing 그대로 — sm.* 잔존 timing 차단.
        if (!SmartMovingConfig.Config.enabled) return;
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
        // BUG-23 (세션 36): SM disabled 시 vanilla applyClimbingSpeed 그대로 — sm.* 잔존 차단.
        if (!SmartMovingConfig.Config.enabled) return;
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
        // BUG-23 (세션 36): SM disabled 시 vanilla updateLimbs 그대로 — sm.* 잔존 차단.
        if (!SmartMovingConfig.Config.enabled) return;
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
        // 🔴 (2026-04-28) 임시 비활성 — 사용자 보고 "달리다 점프 시 엄청 빨라짐" 디버깅.
        //   isAerodynamic 또는 isHeadJumping 잔존 가능성 → 매 tick motion * 1.098 가속.
        //   land + 점프 동작 검증 후 head jump 별도 매핑.
        // if (!sm.isAerodynamic || !sm.isHeadJumping || player.isOnGround()) return;
        // float factor = 0.999F / 0.91F;
        // Vec3d vel = player.getVelocity();
        // player.setVelocity(vel.x * factor, vel.y, vel.z * factor);
    }

    /**
     * B-2 (세션 26): Land 이동 User 배율 이식. 원본 `SmartMovingSelf.java` L119
     *   `speedFactor = getConfigSpeedFactor() * getPotionSpeedFactor() * ...`
     * 의 `getConfigSpeedFactor()` 부분을 vanilla travel() 이 처리하는 Land 경로에 주입.
     *
     * **옵션 A (Mixin inject on getMovementSpeed)** 선택: vanilla 의 `LivingEntity.
     * getMovementSpeed()` HEAD inject 로 반환값에 `getConfigSpeedFactor(player, cfg)` 곱셈.
     *
     * 적용 범위:
     *   - Land (걷기/달리기/스프린트/점프) — vanilla travel() 이 getMovementSpeed() 참조 → 영향
     *   - 비행/수영/잠수/클라이밍 — SM 이 `sm_travel_client` 에서 vanilla travel() cancel
     *     → getMovementSpeed 안 불림 → 영향 0 (각자 경로에서 getCombinedSpeedFactor 별도 적용)
     *
     * Creative 게이트는 `Mover.getConfigSpeedFactor` 가 내부 `isCreative()` 로 처리 —
     *   Creative 아니면 userSpeedFactor=1F → speedFactor(기본 1F) 만 반영. 체감 변화 0.
     *
     * ⚠️ 이중 적용 주의: SM 경로 (Swimmer/Climber/Flyer) 는 getPotionSpeedFactor 에서
     *   GENERIC_MOVEMENT_SPEED attribute 를 별도 읽음. 이 mixin 은 getMovementSpeed 반환값만
     *   바꾸는데, getPotionSpeedFactor 는 attribute 를 직접 읽으므로 이중 곱 안 됨. OK.
     *
     * ClientPlayerEntity 만 대상 — 서버 플레이어는 server-side travel 처리 (클라→서버 동기).
     */
    @Inject(method = "getMovementSpeed", at = @At("HEAD"), cancellable = true)
    private void sm_getMovementSpeed(CallbackInfoReturnable<Float> cir) {
        // 🔴 (2026-04-28) 비활성화 — handleLand 가 vanilla travel 을 cancel + 자체 식 적용
        //   하므로 vanilla travel 의 getMovementSpeed 호출 경로 없음.
        //   다른 서브시스템 (애니메이션/발자국/네트워크) 이 호출 시 sprint 1.154x 부수효과
        //   → 원본보다 살짝 빠른 느낌의 가능성. handleLand 에서 sprintFactor 직접 적용하므로
        //   여기서 추가 곱은 이중 적용 위험.
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
        // BUG-23 (세션 36): SM disabled 시 vanilla isInSwimmingPose 그대로 — sm.* 잔존 차단.
        if (!SmartMovingConfig.Config.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        // [6-2][8-3] 크롤링/수영/잠수/크롤클라이밍 중 setupTransforms Branch 2 진입 차단.
        // isCrawling/isCrawlClimbing: SWIMMING 포즈 사용하되 vanilla -90° 자동 회전 방지
        // isSwimming_sm/isDiving: SM 자체 body X 기울기 애니메이션과 충돌 방지
        if (sm.isCrawling || sm.isCrawlClimbing || sm.isSwimming_sm || sm.isDiving) {
            cir.setReturnValue(false);
        }
    }
}
