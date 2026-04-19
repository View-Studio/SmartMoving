package choco.ratel.smartmoving.mixin;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDimensionsMixin {

    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void smartMoving_getBaseDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;

        SmartMovingState state = player.getAttached(SmartMovingAttachments.STATE);
        if (state != null && state.isSmall()) {
            // 크롤/슬라이딩/천장 클라이밍: 히트박스 0.6×0.8, 눈 높이 0.48
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.48F));
        }
    }
}
