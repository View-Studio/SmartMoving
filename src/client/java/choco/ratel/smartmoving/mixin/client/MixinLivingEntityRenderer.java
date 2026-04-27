package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

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
}
