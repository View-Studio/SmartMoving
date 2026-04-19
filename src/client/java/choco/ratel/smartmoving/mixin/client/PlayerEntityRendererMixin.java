package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    /**
     * 크롤링/슬라이딩 시 렌더 Y 오프셋을 보정하여 히트박스와 시각적 위치를 일치시킨다.
     */
    @Inject(method = "getPositionOffset", at = @At("RETURN"), cancellable = true)
    private void smartMoving_getPositionOffset(
            AbstractClientPlayerEntity entity,
            float tickDelta,
            CallbackInfoReturnable<Vec3d> cir
    ) {
        if (!(entity instanceof PlayerEntity player)) return;
        SmartMovingState state = player.getAttached(SmartMovingAttachments.STATE);
        if (state == null || state.heightOffset == 0F) return;

        Vec3d original = cir.getReturnValue();
        cir.setReturnValue(original.add(0, state.heightOffset, 0));
    }
}
