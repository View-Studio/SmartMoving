package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
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
     * SmartMoving 상태별 엔티티 회전 처리:
     *
     * [크롤링/슬라이딩]
     *   엔티티 전체를 X축 회전. 모든 파트가 함께 회전하므로 연결됨 (vanilla 수영과 동일 원리).
     *
     * [수영/잠수/헤드점프]
     *   setAngles에서 pivotZ 보정 방식 사용 (setupTransforms 회전 없음).
     *   단, vanilla가 SWIMMING EntityPose로 인해 이미 회전을 적용했다면 상쇄해야 함.
     *   vanilla 수영 회전: RotationAxis.POSITIVE_X.rotationDegrees(-90 - entity.pitch)
     *   상쇄: 역회전 적용.
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

        if (state.isCrawling) {
            // 엔티티를 -(QUARTER-THIRTYTWOTH) ≈ -79° 회전 (face-down crawling)
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(-(QUARTER - THIRTYTWOTH)));

        } else if (state.isSliding) {
            // 엔티티를 -QUARTER = -90° 회전 (prone sliding)
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(-QUARTER));

        } else if (state.isSwimming || state.isDiving || state.isHeadJumping) {
            // pivotZ 보정 방식 사용 중 — vanilla SWIMMING 엔티티 회전이 있으면 상쇄
            if (entity.getPose() == EntityPose.SWIMMING) {
                // vanilla: RotationAxis.POSITIVE_X.rotationDegrees(-90 - entity.pitch)
                // 상쇄: 반대 방향 회전
                float vanillaAngleDeg = -90.0F - entity.getPitch();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-vanillaAngleDeg));
            }
        }
    }

    /**
     * heightOffset Y 보정.
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
