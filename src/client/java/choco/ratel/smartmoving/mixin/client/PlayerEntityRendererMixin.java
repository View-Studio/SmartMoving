package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
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
     *   엔티티 전체를 X축 회전. 모든 파트가 함께 회전하므로 연결됨.
     *   heightOffset=0 (모델 발이 수학적으로 지면에 정확히 위치함).
     *
     * [수영/잠수/헤드점프]
     *   setAngles에서 pivotZ 보정 방식 사용 (setupTransforms 회전 없음).
     *   PlayerEntityRenderer가 leaningPitch 기반으로 수영 회전을 적용하면 정확히 상쇄:
     *     적용된 각도 = lerp(leaningPitch, 0, isTouchingWater ? -90-pitch : -90)
     *     추가 translate(0,-1,0.3) if isInSwimmingPose → translate(0,+1,-0.3)으로 상쇄 후 역회전.
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
            // pivotZ 보정 방식 사용 중 — vanilla 수영 엔티티 회전이 있으면 정확히 상쇄
            // PlayerEntityRenderer.setupTransforms이 적용하는 회전:
            //   angle = lerp(leaningPitch, 0, isTouchingWater ? -90-pitch : -90)
            // 추가 translate: isInSwimmingPose() → translate(0, -1, 0.3)
            float leaningPitch = entity.getLeaningPitch(tickDelta);
            if (leaningPitch > 0F) {
                float baseAngle = entity.isTouchingWater()
                        ? -90.0F - entity.getPitch(tickDelta)
                        : -90.0F;
                float appliedAngle = MathHelper.lerp(leaningPitch, 0F, baseAngle);
                // 역순 상쇄: translate 먼저 → 그 다음 rotation
                if (entity.isInSwimmingPose()) {
                    matrices.translate(0F, 1.0F, -0.3F);
                }
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-appliedAngle));
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
