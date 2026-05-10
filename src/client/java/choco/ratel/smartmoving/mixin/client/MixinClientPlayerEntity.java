package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 4-2: tickEssential() 무조건 호출 Mixin.
 *
 * ClientPlayerEntity.tickMovement() HEAD에서 SM isActive 여부 무관하게
 * tickEssential()을 항상 호출한다.
 *
 * 원본: SmartMovingPlayerBase.updateEntityActionState()
 *       → moving.tickEssential()  ← isActive() 체크 없이 항상 호출
 *       → isActive() ? moving.updateEntityActionState(false) : localUpdateEntityActionState()
 */
@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity {

    /**
     * 🔴 비행 재진입 시 vanilla 의 자동 yaw 회전 차단 (사용자 보고 fix):
     *   사용자 motion 잔존 (떨어지는 큰 motionY) + 비행 재진입 시 vanilla
     *   LivingEntity.tickMovement 안의 어떤 처리 가 motion 방향 기반 yaw 보정.
     *   결과: 앞 (W) → +180°, 뒤 (S) → ?, 좌우 (A/D) → ±90°.
     *
     *   해결:
     *   1. tickMovement HEAD 시점 yaw + abilFly 저장.
     *   2. TAIL 시점 비행 진입 엣지 (HEAD 시 abilFly=false → TAIL 시 abilFly=true)
     *      검출 → 그 시점만 yaw 복구.
     *   3. 임계값 30° (좌우 ±90° 회전 케이스도 잡음).
     *
     *   mouse 입력은 별도 메서드 (changeLookDirection) 라 tick 안 yaw 변경은
     *   vanilla 자동 처리만 → 안전하게 복구 가능.
     */
    @org.spongepowered.asm.mixin.Unique
    private float sm_yawAtTickStart;
    @org.spongepowered.asm.mixin.Unique
    private boolean sm_abilFlyAtTickStart;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void sm_tickMovement(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        sm_yawAtTickStart = player.getYaw();
        sm_abilFlyAtTickStart = player.getAbilities().flying;
        SmartMovingClientState.get(player).tickEssential(player);
    }

    /**
     * 🔴 (세션 145 BUG-sneak속도): vanilla 1.21.1 `KeyboardInput.tick(slowDown, factor)` 가
     *   `slowDown=true` 시 movementForward/Sideways *= **PLAYER_SNEAKING_SPEED** (=**0.3**)
     *   곱셈 적용. `slowDown` 결정자 = `ClientPlayerEntity.shouldSlowDown()` =
     *   `isInSneakingPose() || isCrawling()`.
     *
     * SM 의 `getSlowInputSpeedFactor` 도 sneak/crawl 시 sneakFactor (0.3) / crawlFactor (0.15)
     * 곱 추가 → vanilla 0.3 × SM 0.3 = **0.09** 이중 감속 BUG. SM 활성/비활성 시 약 3.3x
     * 차이 (사용자 보고).
     *
     * 해결: SM 활성 시 vanilla 의 `shouldSlowDown()` 결과 강제 false → vanilla movement 곱
     * 차단. SM 자체 sneakFactor/crawlFactor 만 적용 → 0.3/0.15 base. SM 비활성 시 inject
     * 통과 → vanilla 정상 작동 (0.3) + SM 가드 자동 비활성 → 0.3 base. 동일 효과.
     */
    @Inject(method = "shouldSlowDown", at = @At("HEAD"), cancellable = true)
    private void sm_blockVanillaSlowDown(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!SmartMovingConfig.Config.enabled) return;
        cir.setReturnValue(false);
    }

    /**
     * 🔴 (세션 145 BUG-edge): vanilla `ClientPlayerEntity.isSneaking()` override 가
     *   `input.sneaking` 직접 검사 — Entity.isSneaking() 의 DataTracker flag 우회.
     *   우리 MixinEntityClient.sm_isSneaking inject (Entity.isSneaking 대상) 가
     *   ClientPlayerEntity 에서 우회됨 → shift 누름 시 무조건 isSneaking()=true →
     *   `clipAtLedge()=true` → vanilla edge protection 발동 → 엎드린 채 안 떨어짐.
     *
     * 원본 SmartMovingSelf.isSneaking() L3226-L3232 1:1 동일 식 적용.
     *   crawlOverEdge=true (기본) + isCrawling 시 모든 항 false → return false →
     *   edge protection 미발동 → 떨어짐.
     */
    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void sm_isSneaking_ClientPlayer(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        if (!SmartMovingConfig.Config.enabled || player.hasVehicle()) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.forceIsSneaking != null) { cir.setReturnValue(sm.forceIsSneaking); return; }
        SmartMovingConfig cfg = SmartMovingConfig.Config;

        // 🔴 fix #75 (BUG #2): weeping/twisting vines 위 + sneak 시 vanilla 위임.
        //   상세는 MixinEntityClient.sm_isSneaking 주석 참조.
        net.minecraft.block.Block _blockAtPos = player.getWorld()
                .getBlockState(player.getBlockPos()).getBlock();
        if (_blockAtPos == net.minecraft.block.Blocks.WEEPING_VINES
                || _blockAtPos == net.minecraft.block.Blocks.WEEPING_VINES_PLANT
                || _blockAtPos == net.minecraft.block.Blocks.TWISTING_VINES
                || _blockAtPos == net.minecraft.block.Blocks.TWISTING_VINES_PLANT) {
            return;  // override skip → vanilla isSneaking 그대로
        }

        boolean result = (sm.isSlow && player.isOnGround())
                || (!cfg.sneak && sm.wouldIsSneaking && sm.jumpCharge > 0)
                || (!cfg.crawlOverEdge && sm.isCrawling && !sm.isClimbing);
        cir.setReturnValue(result);
    }

    /**
     * 🔴 (세션 145 BUG-점프): 크롤/슬라이드 등 SM 상태 시 vanilla 점프 차단 1:1 매핑.
     *
     * 원본 SmartMovingSelf L2366-L2370 `setIsJumpingField(...)` 가드:
     *   movementInput.jump && !isCrawling && !isSliding
     *   && !(headJumpEnabled && grab && sprint)
     *   && !(jumpChargeEnabled && wouldIsSneaking && onGround && isStanding)
     *   && !blockJumpTillButtonRelease
     *
     * 1.21.1 vanilla 흐름:
     *   ClientPlayerEntity.tickNewAi() 안에서 `this.jumping = this.input.jumping` (offset 38-41).
     *   기존 sm_jumpingFilter (LivingEntity.tickMovement HEAD) 는 tickNewAi 보다 먼저 실행 →
     *   가드 적용 후 vanilla 가 다시 input.jumping 으로 set → 무력화. 점프 발동 BUG 직접 원인.
     *
     * 해결: ClientPlayerEntity.tickNewAi TAIL inject — vanilla `this.jumping = input.jumping`
     *       이후 우리 가드 적용해 false 로 강제 set.
     */
    @Inject(method = "tickNewAi", at = @At("TAIL"))
    private void sm_jumpingFilter_tickNewAi(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!cfg.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);

        // Phase E BUG-3 1:1 정정: 원본 L2367-L2370 setIsJumpingField 5-AND 식의 부정 (= setJumping(false) 조건).
        //   원본 식 (양수): movementInput.jump && !isCrawling && !isSliding
        //                && !(headJumpEnabled && grab && sprint)
        //                && !(jumpChargeEnabled && wouldIsSneaking && onGround && isStanding)
        //                && !blockJumpTillButtonRelease
        //   부정 (= jumping false 강제): isCrawling || isSliding
        //                || (headJumpEnabled && grab && sprint)         ★ 차징 시작 가능 시점부터 차단
        //                || (jumpChargeEnabled && wouldIsSneaking && onGround && isStanding)  ★ 동일
        //                || blockJumpTillButtonRelease
        //   기존 단순화 (isHeadJumping / jumpCharge>0) 는 "이미 발사/차징 중" 상태만 가드 → 시작 frame
        //   에 vanilla 점프 발사 잠재 가능. 1:1 복원.
        boolean shouldBlock =
                sm.isCrawling
             || sm.isSliding
             || (cfg.headJump && SmartMovingKeys.grab.isPressed() && player.isSprinting())
             || (cfg.jumpCharge && sm.wouldIsSneaking && player.isOnGround() && sm.isStanding)
             || sm.blockJumpTillButtonRelease;
        if (shouldBlock) {
            ((net.minecraft.entity.LivingEntity)(Object)this).setJumping(false);
        }
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void sm_tickMovementYawRestore(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        // 비행 진입 엣지 (HEAD: abilFly=false, TAIL: abilFly=true) 만 검사.
        boolean flyEntryEdge = !sm_abilFlyAtTickStart && player.getAbilities().flying;
        if (!flyEntryEdge) return;
        // 비행 진입 엣지 시점의 모든 yaw 변경 = vanilla 자동 보정 (사용자 mouse 입력 X).
        // 임계값 1° (부동소수점 잡음 회피, 그 외 모든 vanilla 자동 회전 catch).
        // 사용자 mouse 입력은 별도 메서드 (changeLookDirection) 라 tick 안 yaw 변경 X →
        // 안전하게 모든 변화 복구.
        float currentYaw = player.getYaw();
        float diff = currentYaw - sm_yawAtTickStart;
        while (diff > 180F) diff -= 360F;
        while (diff < -180F) diff += 360F;
        if (Math.abs(diff) > 1F) {
            player.setYaw(sm_yawAtTickStart);
            player.bodyYaw = sm_yawAtTickStart;
            player.prevBodyYaw = sm_yawAtTickStart;
        }
    }


    /**
     * 🔴 사다리/덩굴 등반 중 sneak 자세 변경 차단.
     *
     * 원본 SmartMovingSelf.isSneaking() L3226-3232:
     *   return ... || (!_crawlOverEdge && isCrawling && !isClimbing) || ...;
     * 원본 1.7.10 vanilla 자세 = isSneaking() 결과 직접 사용 → SM override false →
     * 자세 STANDING. **W 무관 모든 사다리 등반 케이스 자세 변경 X**.
     *
     * 1.21.1 vanilla 자세 결정 source 추적 불가 → 모든 sneak source 일괄 reset.
     * 가드: sm.isClimbing 만 (W 무관 — 원본 1:1).
     */
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void sm_resetSneakInClimb(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        if (!SmartMovingConfig.Config.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        // 🔴 등반 직후 1 틱 reset 유지 (wasClimbing). 사용자 보고: 등반 끝난 직후 vanilla
        //   CROUCHING 발동. KeyboardInput.tick 은 tickMovement 보다 먼저 실행되므로 KeyboardInput
        //   에서 sneaking=false 강제해도 그 틱의 vanilla 자세 처리가 이미 진행되어 자세 잔존.
        //   tickMovement TAIL 에서도 wasClimbing 가드로 reset.
        if (sm.isClimbing || sm.wasClimbing || sm.sneakHeldDuringClimb) {
            // 1. input.sneaking 강제 false
            player.input.sneaking = false;
            // 2. SNEAKING flag 강제 false
            if (player.isSneaking()) player.setSneaking(false);
            // 3. CROUCHING 자세 강제 STANDING
            if (player.getPose() == EntityPose.CROUCHING) {
                player.setPose(EntityPose.STANDING);
            }
            // 🔴 isCrawling/crawlToggled 강제 reset 제거: 사다리 등반 막바지 isClimbCrawling
            //   해제 엣지 (L1890-1908) 의 toCrawling() 호출 결과 (isCrawling=true) 를 즉시
            //   무효화 → SM crawl 모드 진입 차단 + heightOffset=-1 잔존 (B-35 reset 미발동)
            //   → vanilla CROUCHING + 작은 박스 잔존이 사용자 보고 "엎드리기" 자세.
            //   isCrawling 은 메인 식 (L1535) 의 canCrawl `!isClimbing` 가드로 등반 중 자동
            //   차단되므로 추가 강제 불필요.
        }
    }

    /**
     * afterOnLivingUpdate: flyWhileOnGround 처리.
     * 원본: SmartMovingSelf.afterOnLivingUpdate() (SmartMovingPlayerBase.java, 1368-1377줄)
     *
     * vanilla tickMovement 실행 후 착지로 isFlying이 false가 됐지만
     * SM flyWhileOnGround=true이면 flying을 복원한다.
     * sneakButton+grabButton 동시 누름 시 착지 허용.
     *
     * 원본: sp.cameraYaw=0; sp.prevCameraYaw=0 — 1.21.1에서 해당 필드 없음 (삭제됨).
     */
    @Inject(method = "tickMovement", at = @At("TAIL"), order = 900)
    private void sm_flyWhileOnGround(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        SmartMovingConfig cfg = SmartMovingConfig.Config;

        // 🔴 잉여 정정 (Flying Phase F-6 / 세션 40): !cfg.flyCloseToGround 가드 삭제.
        //   원본 SmartMovingSelf L1827 = `_flyWhileOnGround.value && !(sneak+grab) &&
        //   wasCapabilitiesIsFlying && !sp.capabilities.isFlying && sp.onGround` 만 검사 —
        //   _flyCloseToGround 가드 없음. 1.21.1 의 추가 `!cfg.flyCloseToGround` 가드는 원본에
        //   없는 잉여 → flyCloseToGround=true (기본값) 시 자동 비행 복원 차단 = BUG 영향.
        //   해결: 가드 삭제 (cfg.flyWhileOnGround 만 검사).
        if (!cfg.enabled || !cfg.flyWhileOnGround) return;
        // sneakButton.Pressed: vanilla 키 직접 (SM isSneaking override 우회)
        if (net.minecraft.client.MinecraftClient.getInstance().options.sneakKey.isPressed()
                && SmartMovingKeys.grab.isPressed()) return;
        if (sm.wasCapabilitiesIsFlying && !player.getAbilities().flying && player.isOnGround()) {
            player.getAbilities().flying = true;
            player.sendAbilitiesUpdate();
        }
    }

    /**
     * correctOnUpdate: 수영/크롤링 등 낮은 속도 이동 시 bodyYaw 보정.
     * 원본: SmartMovingBase.correctOnUpdate(isSmall, reverseMaterialAcceleration)
     *       isSmall = isSwimming||isDiving||isDipping||isCrawling
     *       reverseMaterialAcceleration(isSwimming) → 1.21.1 N/A
     *
     * 0.02 < f < 0.05 구간(느린 이동)에서 renderYawOffset(=bodyYaw)을 이동 방향으로 서서히 정렬.
     * sp.swingProgress > 0 시에는 rotationYaw 고정.
     */
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void sm_correctOnUpdate(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!cfg.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);

        boolean isSmall = sm.isSwimming_sm || sm.isDiving || sm.isDipping || sm.isCrawling;
        if (!isSmall) return;

        double d  = player.getX() - player.prevX;
        double d1 = player.getZ() - player.prevZ;
        float f = (float) Math.sqrt(d * d + d1 * d1);
        if (f <= 0.02F || f >= 0.05F) return;

        float f1 = (float)(Math.atan2(d1, d) * 180.0 / Math.PI) - 90F;
        if (player.handSwingProgress > 0.0F) f1 = player.getYaw();

        float f4 = f1 - player.bodyYaw;
        for (; f4 < -180F; f4 += 360F) {}
        for (; f4 >= 180F;  f4 -= 360F) {}
        float x = player.bodyYaw + f4 * 0.3F;

        float f5 = player.getYaw() - x;
        for (; f5 < -180F; f5 += 360F) {}
        for (; f5 >= 180F;  f5 -= 360F) {}
        if (f5 < -75F) f5 = -75F;
        if (f5 >= 75F)  f5 = 75F;

        player.bodyYaw = player.getYaw() - f5;
        if (f5 * f5 > 2500F) player.bodyYaw += f5 * 0.2F;

        for (; player.bodyYaw - player.prevBodyYaw < -180F; player.prevBodyYaw -= 360F) {}
        for (; player.bodyYaw - player.prevBodyYaw >= 180F; player.prevBodyYaw += 360F) {}
    }

    // R-01: tickMovement TAIL — 모든 이동 처리 후 State 패킷 전송
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void sm_sendStatePacket(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        SmartMovingClientState.get(player).sendStatePacket(player);
    }

    /**
     * 낙하 중 sneak 시 모델/카메라 Y -0.125 점프 차단 — `inSneakingPose` 필드 결정 가로채기.
     *
     * 배경: ClientPlayerEntity 가 자체 `inSneakingPose` boolean 필드를 보유하며,
     * `isInSneakingPose()` 를 override 해 entity.pose 가 아니라 이 필드를 반환.
     * `PlayerEntityRenderer.getPositionOffset` 가 `isInSneakingPose()=true` 시
     * Y = scale × -2/16 = -0.125 offset 적용 → 모델/카메라 점프.
     *
     * `inSneakingPose` 필드는 ClientPlayerEntity.tickMovement (bytecode offset 80-145)
     * 에서 매 tick 별도로 결정:
     *   inSneakingPose = !flying && !swimming && !hasVehicle && canChangeIntoPose(CROUCHING)
     *                    && (isSneaking() || (!isSleeping() && !canChangeIntoPose(STANDING)));
     *
     * MixinPlayerEntity.sm_redirectIsSneakingForFallingPose 가 PlayerEntity.updatePose 의
     * isSneaking() 만 가로챘으나, 이 ClientPlayerEntity.tickMovement 의 isSneaking() 호출은
     * 별도라 그대로 통과 → inSneakingPose=true → getPositionOffset Y -0.125 적용.
     *
     * 해결: tickMovement 안의 `isSneaking()` 호출 (offset 114) 만 redirect → 낙하 중 false
     * 반환 → inSneakingPose 의 isSneaking() term 무력화 → inner = (!isSleeping &&
     * !canChangeIntoPose(STANDING)) 만 남음 → 일반 공중 낙하 시 false → inSneakingPose=false.
     *
     * 좁은 천장 시나리오 (canChangeIntoPose(STANDING)=false) 에서는 vanilla 가 자체 판단으로
     * inSneakingPose=true 강제 — vanilla 안전 fallback 으로 사용자 강제 STANDING 시 끼임 방지.
     */
    @Redirect(method = "tickMovement",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSneaking()Z"))
    private boolean sm_redirectIsSneakingForInSneakingPose(ClientPlayerEntity self) {
        boolean original = self.isSneaking();
        if (!original) return false;
        if (!SmartMovingConfig.Config.enabled) return original;

        boolean isFalling = !self.isOnGround()
                && self.fallDistance > SmartMovingConfig.Config.fallAnimationDistanceMinimum
                && !self.isTouchingWater()
                && !self.getAbilities().flying;
        if (isFalling) return false;

        // 🔴 fix #78 (BUG #2.6, 2026-05-11, 사용자 보고 "weeping/twisting vines + sneak 시 모델 살짝 아래"):
        //   ClientPlayerEntity 의 자체 inSneakingPose 필드는 entity.pose 무관하게 별도 식으로 결정:
        //     inSneakingPose = !flying && !swimming && !hasVehicle && canChangeIntoPose(CROUCHING)
        //                      && (isSneaking() || (!isSleeping() && !canChangeIntoPose(STANDING)));
        //   fix #77 은 PlayerEntity.updatePose 의 isSneaking() 만 redirect (= entity.pose=STANDING).
        //   하지만 여기 ClientPlayerEntity.tickMovement 의 isSneaking() 호출은 별도 → fix #75 후
        //   vanilla 위임 → true → inSneakingPose=true → PlayerEntityRenderer.getPositionOffset
        //   `Y = -2/16 = -0.125` offset 적용 → 모델 살짝 아래.
        //
        //   해결: 같은 redirect 에 weeping/twisting vines 가드 추가 → 해당 isSneaking() 호출만 false
        //   반환 → inSneakingPose=false → 모델 정상 위치.
        //   다른 isSneaking() 호출 (isHoldingOntoLadder 등) 영향 X → 매달림 유지 (= 기능 보존).
        net.minecraft.block.Block blockAtPos = self.getWorld()
                .getBlockState(self.getBlockPos()).getBlock();
        if (blockAtPos == net.minecraft.block.Blocks.WEEPING_VINES
                || blockAtPos == net.minecraft.block.Blocks.WEEPING_VINES_PLANT
                || blockAtPos == net.minecraft.block.Blocks.TWISTING_VINES
                || blockAtPos == net.minecraft.block.Blocks.TWISTING_VINES_PLANT) {
            return false;
        }

        return original;
    }
}
