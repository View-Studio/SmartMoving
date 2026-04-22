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
}
