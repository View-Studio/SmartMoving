package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * B-17 / §16-25 — 망토 X 회전 클램프.
 *
 * 원본 SmartRender ModelCapeRenderer.java L72-L73:
 *   localAngleMax = max(70.523F - outer.rotateAngleX * RadiantToAngle, 6F)
 *   realLocalAngle = min(localAngle, localAngleMax)
 * = SR 다층 모델 bipedOuter.rotateAngleX (전체 몸 X 기울기) 에 따라 망토 X 회전 상한 적용
 *   (큰 X 기울기 상태 — Climb/Swim/Dive/Slide/Flying/HeadJump — 에서 망토 과도 펴짐 방지).
 *
 * 1.21.1 vanilla CapeFeatureRenderer.render (yarn 1.21.1+build.3 디스어셈블리 검증):
 *   line 69: matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0F + r/2.0F + q));
 *           → 1.7.10 원본 `localAngle = 6F + f3/2.0F + f2` 와 정확히 동일 패턴.
 *   line 70: Z 회전 (s/2)
 *   line 71: Y 회전 (180 - s/2)
 *   vanilla 클램프 미적용 → outer.X 기반 상한 추가 적용이 본 작업.
 *
 * 구현:
 *   - outer.X 등가 = sm.smOuterTiltX (라디안), MixinPlayerEntityRenderer.sm_setupTransforms 의
 *     5 분기 (isSwim/isDive/isSlide/isFlying/isHeadJumping) 에서 capture.
 *   - @Inject(HEAD) 으로 entity 받아 static field 에 저장 (cape 렌더는 메인 render thread 단일
 *     스레드 / nested 없음 — static 안전).
 *   - @ModifyArg(rotationDegrees, ordinal=0) — 첫 번째 호출 (X 회전) 인자 가로채기.
 *   - @Inject(RETURN) 으로 static 정리 (방어적).
 *
 * 자기 자신 (ClientPlayerEntity) 한정 적용 — 다른 플레이어는 sm_setupTransforms 가 SM tilt 적용
 * 안 하므로 outer.X = 0 → 클램프 무관 → vanilla 동작 유지 (일관성).
 */
@Environment(EnvType.CLIENT)
@Mixin(CapeFeatureRenderer.class)
public abstract class MixinCapeFeatureRenderer {

    /** render 진입 entity capture (static 안전: cape 렌더는 단일 render thread / nested 없음) */
    private static AbstractClientPlayerEntity sm_currentCapeEntity;

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V",
            at = @At("HEAD"))
    private void sm_captureCapeEntity(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                       AbstractClientPlayerEntity entity, float limbAngle, float limbDistance,
                                       float tickDelta, float animationProgress, float headYaw, float headPitch,
                                       CallbackInfo ci) {
        sm_currentCapeEntity = entity;
    }

    /**
     * 첫 번째 RotationAxis.rotationDegrees(F) 호출 = vanilla line 69 X 회전.
     * angleDeg = `6.0F + r/2.0F + q` (vanilla 식, 1.7.10 원본 등가).
     * 클램프: min(angleDeg, max(70.523F - outerXDeg, 6F)) = 1.7.10 원본 L72-L73 1:1.
     */
    @ModifyArg(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V",
               at = @At(value = "INVOKE",
                        target = "Lnet/minecraft/util/math/RotationAxis;rotationDegrees(F)Lorg/joml/Quaternionf;",
                        ordinal = 0))
    private float sm_clampCapeXAngle(float angleDeg) {
        AbstractClientPlayerEntity entity = sm_currentCapeEntity;
        if (!(entity instanceof ClientPlayerEntity)) return angleDeg;   // 타인 plr — vanilla 그대로
        SmartMovingClientState sm = SmartMovingClientState.get(entity.getUuid());
        if (sm.smOuterTiltX == 0f) return angleDeg;   // SM tilt 없음 — vanilla 그대로
        float outerXDeg = (float) Math.toDegrees(sm.smOuterTiltX);
        float localAngleMax = Math.max(70.523f - outerXDeg, 6f);
        return Math.min(angleDeg, localAngleMax);
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V",
            at = @At("RETURN"))
    private void sm_clearCapeEntity(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                     AbstractClientPlayerEntity entity, float limbAngle, float limbDistance,
                                     float tickDelta, float animationProgress, float headYaw, float headPitch,
                                     CallbackInfo ci) {
        sm_currentCapeEntity = null;
    }
}
