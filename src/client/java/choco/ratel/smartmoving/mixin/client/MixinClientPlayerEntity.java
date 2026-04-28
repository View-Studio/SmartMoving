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

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void sm_tickMovement(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        SmartMovingClientState.get(player).tickEssential(player);
    }

    /**
     * 🔴 사다리/덩굴 등반 + W + sneak 시 CROUCHING 자세 매 틱 강제 reset.
     *
     * 이전 시도 실패 (모두 효과 없음):
     *   1. shouldEnterCrouchingPose mixin (ClientPlayerEntity / LivingEntity) — method 없음
     *   2. Entity.setSneaking mixin — vanilla 자세 결정이 SNEAKING flag 사용 안 함
     *   3. KeyboardInput.tick mixin — vanilla 자세 결정이 input.sneaking 사용 안 함
     *
     * 진단:
     * vanilla 1.21.1 의 CROUCHING 자세 결정 source 를 정확히 추적 못 함 (yarn mapping
     * 검색 한계). 어떤 source 를 쓰든 "결과적 자세" 가 CROUCHING 인 것을 매 틱 강제 reset.
     *
     * tickMovement TAIL — vanilla 자세 결정 후 우리가 STANDING 으로 reset.
     * 가드: sm.isClimbing && forward > 0F (사용자 의도 정확히).
     */
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void sm_resetCrouchingInClimb(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        if (!SmartMovingConfig.Config.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isClimbing && player.input.movementForward > 0F) {
            if (player.getPose() == EntityPose.CROUCHING) {
                player.setPose(EntityPose.STANDING);
            }
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
}
