package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.physics.JumpHandler;
import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PlayerEntityJumpMixin {

    /**
     * 점프 호출을 인터셉트하여 SmartMoving의 tryJump() 시스템으로 위임.
     * 로컬 클라이언트 플레이어만 처리.
     */
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void smartMoving_jump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;
        if (player != MinecraftClient.getInstance().player) return;

        SmartMovingState state = player.getAttachedOrCreate(SmartMovingAttachments.STATE);

        if (JumpHandler.interceptJump(state, player)) {
            ci.cancel();
        }
    }
}
