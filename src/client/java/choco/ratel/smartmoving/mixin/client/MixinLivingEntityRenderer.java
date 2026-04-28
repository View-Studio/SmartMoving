package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * renderName() sneakNameTag 대응.
 *
 * 원본: SmartMovingRender.renderName() —
 *   스니킹 중 sneakNameTag=true이면 temporaryIsSneaking=false 취급.
 *   → isSneaky()=false → hasLabel에서 64 거리 기준 적용 (vanilla 32 대신).
 *
 * LivingEntityRenderer.hasLabel(T) 내부의 entity.isSneaky() 호출을 redirect해서
 * 타인 플레이어가 스니킹 중이어도 sneakNameTag=true이면 isSneaky()=false 반환.
 */
@Mixin(LivingEntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class MixinLivingEntityRenderer {

    @Redirect(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isSneaky()Z"))
    private boolean sm_isSneakyForLabel(LivingEntity entity) {
        if (!(entity instanceof AbstractClientPlayerEntity) || entity instanceof ClientPlayerEntity)
            return entity.isSneaky();
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!cfg.enabled || !cfg.sneakNameTag) return entity.isSneaky();
        // sneakNameTag=true: 스니킹 중이어도 isSneaky()=false → 64 거리 기준 적용
        return false;
    }

    /**
     * 🔴 (2026-04-27) 낙하/기본 상태 head 가 vanilla 동작 유지하도록 netHeadYaw 보정.
     *
     * 우리 매핑: setupTransforms force=0 + sm_setupTransforms TAIL 의 R_y(-yawLerped) 추가.
     * vertex 분석: head world yaw = yawLerped + netHeadYaw_deg.
     * vanilla head world yaw = headYaw_deg (cameraYaw 즉시 추적).
     *
     * 같으려면 netHeadYaw_force = headYaw - yawLerped = (netHeadYaw + bodyYaw_natural) - yawLerped.
     *   → netHeadYaw_force = vanilla_netHeadYaw + (bodyYaw_natural - yawLerped).
     *
     * 비행/SM force 분기는 smStandardFadeActive=false → 그대로 통과.
     *
     * 🔴 (2026-04-27) 낙하 sub-mode 분기 추가 (사용자 요청 "낙하는 비행처럼"):
     *   smFallingFadeMode=true 시 보정 skip → vanilla netHeadYaw 그대로 → head 가 fade matrix 영향
     *   받아 body 와 같이 lag (비행 패턴). sm_animateFalling 의 head.yaw=0 force 와 협동.
     */
    @ModifyArg(
        method = "render",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V"),
        index = 4
    )
    private float sm_modifyNetHeadYaw(float netHeadYaw) {
        // 🔴 (2026-04-27) head 가 vanilla cameraYaw 추적 유지하도록 보정.
        //   기본 상태: ModifyArg sm_modifyBodyYaw 가 bodyYaw → lagged 로 force.
        //   이전 b876203 매핑 (force=0 + R_y(-yawLerped)) 의 보정식:
        //     netHeadYaw + bodyYaw_natural - yawLerped (yawLerped = cameraAngle fade lag).
        //   새 매핑의 등가 변수: lagged = vanilla bodyYaw 의 fade lag → yawLerped 자리에 lagged.
        //   = netHeadYaw + bodyYaw_natural - lagged.
        //
        //   낙하 (smFallingFadeMode=true): sm_animateFalling head.yaw=0 force 가 덮어씀 → skip.
        if (!SmartMovingClientState.smStandardFadeActive) return netHeadYaw;
        if (SmartMovingClientState.smFallingFadeMode) return netHeadYaw;
        return netHeadYaw + SmartMovingClientState.smCachedBodyYawNaturalDeg
                - SmartMovingClientState.smCachedBodyYawLaggedDeg;
    }

    /**
     * 🔴 (2026-04-28) 원본 SmartRenderModel.animateArmSwinging (L269-275) 1:1 매핑.
     *   vanilla 1.21.1 limbAnimator (EMA + min(f*4,1) multiplier) 대신 SM stat 사용.
     *
     *   원본 식 (vanilla 동일):
     *     rightArm.pitch = cos(swingPos * 0.6662 + π) * 2.0 * speed * 0.5;
     *     leftLeg.pitch  = cos(swingPos * 0.6662 + π) * 1.4 * speed;
     *
     *   원본 SM 입력: totalHorizontalDistance (= EMA 누적 거리), currentHorizontalSpeed (= EMA 속도).
     *   vanilla 1.21.1 입력: limbAnimator.pos (= EMA + *4 multiplier), limbAnimator.speed.
     *
     *   ModifyArg 로 인자만 변경 → vanilla setAngles 식 그대로 + sneak/idle/heldItem 등 다른
     *   효과 다 보존.
     *
     *   ClientPlayerEntity (자기 자신) 만 적용. 다른 entity (mob, 다른 player) 는 vanilla.
     */
    /**
     * 🔴 (2026-04-28) entity 가 모든 player (AbstractClientPlayerEntity = 자기 + 다른 player)
     *   일 때 ModifyArg 발동. mob 등 다른 entity 는 vanilla 그대로.
     *
     *   자기 player: SmartMovingClientState.get(ClientPlayerEntity).
     *   다른 player: SmartMovingClientState.get(uuid) — server 에서 sync 된 stat.
     */
    @Unique private AbstractClientPlayerEntity sm_currentRenderPlayer;

    @Inject(method = "render", at = @At("HEAD"))
    private void sm_captureRenderEntity(LivingEntity entity, float yaw, float tickDelta,
                                         MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                         int light, CallbackInfo ci) {
        sm_currentRenderPlayer = entity instanceof AbstractClientPlayerEntity p ? p : null;
    }

    @ModifyArg(
        method = "render",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V"),
        index = 1
    )
    private float sm_modifyLimbSwing(float limbSwing) {
        if (sm_currentRenderPlayer == null) return limbSwing;
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!cfg.enabled) return limbSwing;
        SmartMovingClientState sm = SmartMovingClientState.get(sm_currentRenderPlayer.getUuid());
        // 🔴 (사용자 보고: 점프/낙하 시 팔다리 앞뒤 swing) — 원본 SmartRenderModel.animateArmSwinging
        //   입력 `totalHorizontalDistance` (수평만) 1:1. getTotalDistance (3D, 수직 포함) 사용 시
        //   점프/낙하 motionY 큰 값 → SM currentSpeed 큰 값 → vanilla setAngles 의 arm.pitch swing
        //   식 활성 → 사용자 보고 팔다리 swing.
        return sm.stats.getTotalHorizontalDistance(SmartMovingClientState.globalCachedTickDelta);
    }

    @ModifyArg(
        method = "render",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V"),
        index = 2
    )
    private float sm_modifyLimbSwingAmount(float limbSwingAmount) {
        if (sm_currentRenderPlayer == null) return limbSwingAmount;
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!cfg.enabled) return limbSwingAmount;
        SmartMovingClientState sm = SmartMovingClientState.get(sm_currentRenderPlayer.getUuid());
        // 🔴 동일 fix — `currentHorizontalSpeed` (수평만) 1:1. `getCurrentSpeed` (3D) 사용 시
        //   점프/낙하 시 팔다리 swing 발생 (사용자 보고).
        return sm.stats.getCurrentHorizontalSpeed(SmartMovingClientState.globalCachedTickDelta);
    }
}
