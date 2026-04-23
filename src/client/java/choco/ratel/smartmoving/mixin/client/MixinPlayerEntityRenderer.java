package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
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
 * renderName(): 타인 플레이어 이름 태그 — 크롤 숨김 + heightOffset Y 보정.
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
                || sm.isHeadJumping || sm.isCrawling
                || sm.isFlying || sm.isAngleJumping();
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
        // 원본: fadeRotateAngleX = true + rotateAngleX = Quarter - Sixteenth * standSneakFactor
        // standSneakFactor: 정지/스니킹=1 → 67.5°, 보행=0 → 90°(완전 수평)
        if (sm.isSwimming_sm) {
            float tiltAngle = (float) Math.PI / 2f - (float) Math.PI / 8f * sm.swimStandSneakFactor;
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(tiltAngle));
        }

        // SM 잠수(isDiving): 원본 SmartMovingModel.md L542 —
        //   bipedOuter.rotateAngleX = isLevitate ? Quarter - Sixteenth
        //                           : (isJump ? 0F : Quarter - currentVerticalAngle)
        if (sm.isDiving) {
            float tiltAngle;
            if (sm.isLevitating) {
                // Quarter - Sixteenth ≈ 7π/16 (살짝 덜 수평)
                tiltAngle = (float) Math.PI / 2f - (float) Math.PI / 16f;
            } else if (sm.isJumping) {
                // 점프 중 잠수: 수직각 0 (정자세)
                tiltAngle = 0f;
            } else {
                // 일반 잠수: Quarter - currentVerticalAngle (수직각 반영)
                tiltAngle = (float) Math.PI / 2f - sm.stats.currentVerticalAngle;
            }
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(tiltAngle));
        }

        // SM 슬라이딩(isSliding): bipedOuter.rotateAngleX = Quarter
        if (sm.isSliding) {
            float tiltAngle = (float) Math.PI / 2f; // Quarter
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(tiltAngle));
            // bipedOuter.rotationPointY = 5F
            matrices.translate(0f, 5f / 16f, 0f);
        }

        // isFlying body X 기울기: θ = (Quarter - verticalAngle) * walkFactor (C-42, A-30 SmartStatistics)
        // walkFactor = Factor(currentSpeed, 0F, 1) — 속도 0~1 범위 정규화 (SmartMovingModel.md 9번 분기)
        if (sm.isFlying) {
            float walkFactor = Math.min(1f, Math.max(0f, sm.stats.currentSpeed));
            float theta = ((float) Math.PI / 2f - sm.stats.currentVerticalAngle) * walkFactor;
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(theta));
        }

        // isHeadJumping body X 기울기: θ = Quarter - currentVerticalAngle (C-42, SmartMovingModel.md 10번 분기)
        if (sm.isHeadJumping) {
            float theta = (float) Math.PI / 2f - sm.stats.currentVerticalAngle;
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(theta));
        }
    }

    /**
     * renderName() 대응 — 타인 플레이어 이름 태그 Y 보정 + 크롤 중 이름 숨김.
     *
     * 원본: SmartMovingRender.renderName()
     *   - isCrawling && !isClimbing && !crawlNameTag → 이름 숨김
     *   - heightOffset == -1 → d1 -= 0.2F
     *   - originalSneaking && sneakNameTag → d1 -= 0.05F (비스니킹 취급 시 y 보정)
     *
     * sneakNameTag(스니킹 중 64 거리 기준 확장)은 MixinLivingEntityRenderer에서 처리.
     */
    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V",
            at = @At("HEAD"), cancellable = true)
    private void sm_renderLabel(AbstractClientPlayerEntity entity, Text text,
            MatrixStack matrices, VertexConsumerProvider consumers, int light, float tickDelta,
            CallbackInfo ci) {
        if (entity instanceof ClientPlayerEntity) return;
        SmartMovingClientState sm = SmartMovingClientState.get(entity.getUuid());
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!cfg.enabled) return;

        // 크롤 중 이름 숨김 (crawlNameTag=false)
        if (sm.isCrawling && !sm.isClimbing && !cfg.crawlNameTag) {
            ci.cancel();
            return;
        }

        // heightOffset == -1: 헤드점프 상태 이름 태그 y -0.2F 보정
        if (sm.heightOffset == -1f) {
            matrices.translate(0.0, -0.2, 0.0);
        }
        // 스니킹 중 sneakNameTag=true: 비스니킹 취급으로 이름 표시 → y -0.05F 보정
        // (원본: 스니킹→비스니킹 포즈 전환 시 이름 태그 위치 차이 보정)
        else if (entity.isSneaking() && cfg.sneakNameTag) {
            matrices.translate(0.0, -0.05, 0.0);
        }
    }
}
