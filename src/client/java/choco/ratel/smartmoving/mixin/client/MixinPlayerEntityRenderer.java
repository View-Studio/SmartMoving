package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 9-6: getPositionOffset() — 크롤링/헤드점프 렌더 Y 오프셋.
 * 12-3: setupTransforms() — SM 이동 상태별 body X 기울기 + bodyYaw stub.
 * 12-6: getPositionOffset() — heightOffset 렌더 적용.
 */
@Mixin(PlayerEntityRenderer.class)
@Environment(EnvType.CLIENT)
public class MixinPlayerEntityRenderer {

    @Unique private static boolean smBodyYawActive;
    @Unique private static float smBodyYawOverride;

    /**
     * [9-6][12-6] getPositionOffset() 오버라이드.
     *
     * SM 크롤링: SWIMMING 포즈 오프셋(setupTransforms translate(0,-1,0.3) 제거됨) 대신
     *             vanilla 웅크리기 오프셋(-scale × 0.125)을 반환.
     * SM 헤드점프: heightOffset(-1F) 렌더 적용 (타인 플레이어 렌더 위치 보정).
     *
     * 원본: SmartMovingRender.renderPlayerAt() → d1 += heightOffset
     *       SmartMovingPlayerBase.getYOffset() → -0.125F
     */
    @Inject(method = "getPositionOffset(Lnet/minecraft/client/network/AbstractClientPlayerEntity;F)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"), cancellable = true)
    private void sm_getPositionOffset(AbstractClientPlayerEntity entity, float tickDelta,
                                       CallbackInfoReturnable<Vec3d> cir) {
        if (!(entity instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);

        // 헤드점프: heightOffset Y 오프셋 적용 (우선순위 높음)
        if (sm.isHeadJumping && sm.heightOffset != 0f) {
            cir.setReturnValue(new Vec3d(0D, sm.heightOffset, 0D));
            return;
        }

        // 크롤링: SWIMMING 포즈 오프셋 대신 SM 크롤링 오프셋
        if (sm.isCrawling) {
            cir.setReturnValue(new Vec3d(0D, -entity.getScale() * 0.125D, 0D));
        }
    }

    /**
     * [12-3] setupTransforms() HEAD 주입 — SM bodyYaw 오버라이드 계산.
     *
     * isClimbing/isCrawlClimbing/isCeilingClimbing/isSwimming_sm/isDiving/isSliding/
     * isHeadJumping/isCrawling 상태에서 forwardRotation(이동 방향 각도)으로 bodyYaw를 강제한다.
     * 계산 결과는 smBodyYawActive/smBodyYawOverride에 저장하여 @ModifyArg가 읽는다.
     *
     * 원본: SmartMovingRender.rotatePlayer() → forwardRotation = Math.atan2(-vel.x, vel.z)
     */
    @Inject(method = "setupTransforms", at = @At("HEAD"))
    private void sm_captureBodyYaw(AbstractClientPlayerEntity player, MatrixStack matrices,
                                    float animationProgress, float bodyYaw, float tickDelta, float scale,
                                    CallbackInfo ci) {
        smBodyYawActive = false;
        if (!(player instanceof ClientPlayerEntity localPlayer)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(localPlayer);
        boolean smActive = sm.isRopeSliding || sm.isClimbing || sm.isCrawlClimbing || sm.isCeilingClimbing
                || sm.isSwimming_sm || sm.isDiving || sm.isSliding
                || sm.isHeadJumping || sm.isCrawling;
        if (!smActive) return;
        Vec3d vel = localPlayer.getVelocity();
        if (vel.x * vel.x + vel.z * vel.z < 1e-4) return;
        smBodyYawActive = true;
        smBodyYawOverride = (float) Math.toDegrees(Math.atan2(-vel.x, vel.z));
    }

    /**
     * [12-3] setupTransforms() @ModifyArg — super.setupTransforms() 호출 시 bodyYaw(index=3) 교체.
     *
     * sm_captureBodyYaw에서 smBodyYawActive=true인 경우에만 forwardRotation으로 대체한다.
     * B-17 확인: index=3 = bodyYaw (entity=0, matrices=1, animationProgress=2, bodyYaw=3, tickDelta=4, scale=5)
     */
    @ModifyArg(
        method = "setupTransforms",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;setupTransforms(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V"),
        index = 3
    )
    private float sm_modifyBodyYaw(float bodyYaw) {
        return smBodyYawActive ? smBodyYawOverride : bodyYaw;
    }

    /**
     * [12-3] setupTransforms() TAIL 주입 — SM 이동 상태별 body X 기울기.
     *
     * vanilla setupTransforms()의 super.setupTransforms()가 이미 Y회전(bodyYaw)을
     * 적용했으므로, 여기에 추가로 SM 고유 X 기울기를 적용한다.
     *
     * 원본: SmartMovingRender.rotatePlayer() → bipedOuter.rotateAngleY 설정
     *       SmartMovingModel.setRotationAngles() → bipedOuter.rotateAngleX 설정
     */
    @Inject(method = "setupTransforms", at = @At("TAIL"))
    private void sm_setupTransforms(AbstractClientPlayerEntity player, MatrixStack matrices,
                                     float animationProgress, float bodyYaw, float tickDelta, float scale,
                                     CallbackInfo ci) {
        if (!(player instanceof ClientPlayerEntity localPlayer)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(localPlayer);

        // SM 수영(isSwimming_sm): bipedOuter.rotateAngleX = Quarter - Sixteenth * standSneakFactor
        // standSneakFactor 미추적 → 정지 시 기본값 Quarter 적용 (Phase 13 개선)
        if (sm.isSwimming_sm) {
            // 원본: fadeRotateAngleX = true + rotateAngleX = Quarter - Sixteenth * standSneakFactor
            // 이동 중에는 Quarter(약 78°) 기울임. 정지 시 약간 덜 기울임
            float tiltAngle = (float) Math.PI / 2f - (float) Math.PI / 8f; // Quarter - Sixteenth
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(tiltAngle));
        }

        // SM 잠수(isDiving): bipedOuter.rotateAngleX = Quarter - currentVerticalAngle (근사)
        if (sm.isDiving) {
            float tiltAngle = (float) Math.PI / 2f; // Quarter (전방 수평)
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(tiltAngle));
        }

        // SM 슬라이딩(isSliding): bipedOuter.rotateAngleX = Quarter
        if (sm.isSliding) {
            float tiltAngle = (float) Math.PI / 2f; // Quarter
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(tiltAngle));
            // bipedOuter.rotationPointY = 5F
            matrices.translate(0f, 5f / 16f, 0f);
        }

        // TODO Phase 13: isFlying body X 기울기 (verticalAngle 추적 필요)
        // TODO Phase 13: isHeadJumping body X/Y (currentVerticalAngle/horizontalAngle 추적 필요)
    }
}
