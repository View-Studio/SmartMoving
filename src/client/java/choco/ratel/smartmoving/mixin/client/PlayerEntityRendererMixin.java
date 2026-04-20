package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static choco.ratel.smartmoving.client.render.AnimationUtil.*;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    /**
     * 크롤링/슬라이딩 시 엔티티 전체를 회전하여 모든 파트가 연결되어 보이게 한다.
     * 바닐라 수영이 setupTransforms에서 -90° 회전을 적용하는 것과 동일한 원리.
     */
    @Inject(
            method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V",
            at = @At("TAIL")
    )
    private void smartMoving_setupTransforms(
            AbstractClientPlayerEntity entity,
            MatrixStack matrices,
            float animationProgress,
            float bodyYaw,
            float tickDelta,
            float handSwingProgress,
            CallbackInfo ci
    ) {
        SmartMovingState state = entity.getAttached(SmartMovingAttachments.STATE);
        if (state == null) return;

        // 바닐라 수영과 동일: 음수 = 앞으로 기울기 (face down)
        if (state.isCrawling) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(-(QUARTER - THIRTYTWOTH)));
        } else if (state.isSliding) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(-QUARTER));
        }
    }

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
