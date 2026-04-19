package choco.ratel.smartmoving.mixin;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityIsClimbingMixin {

    /**
     * isClimbing() 또는 isCeilingClimbing() 상태면 true 반환.
     * 바닐라가 이 플래그를 체크해 낙하 대미지를 방지하고 낙하 속도를 제한한다.
     */
    @Inject(method = "isClimbing", at = @At("HEAD"), cancellable = true)
    private void smartMoving_isClimbing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;

        SmartMovingState state = player.getAttached(SmartMovingAttachments.STATE);
        if (state != null && (state.isClimbing || state.isCeilingClimbing)) {
            cir.setReturnValue(true);
        }
    }
}
