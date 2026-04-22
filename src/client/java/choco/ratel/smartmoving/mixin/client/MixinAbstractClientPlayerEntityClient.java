package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * getFovMultiplier() 오버라이드 — fadingPerspectiveFactor 기반 부드러운 FOV 배율.
 * 원본: SmartMovingSelf.getFOVMultiplier (SmartMovingSelf.md L2036-2044)
 *
 * getFovMultiplier()는 AbstractClientPlayerEntity에 정의됨 — ClientPlayerEntity 타깃 불가.
 *
 * vanilla 로직 (AbstractClientPlayerEntity.getFovMultiplier() 바이트코드 역산):
 *   f = 1.0; if flying f*=1.1;
 *   f *= (MOVEMENT_SPEED / walkSpeed + 1) / 2;
 *   bow: f *= 1 - t²*0.15; spyglass 1인칭: return 0.1;
 *   return lerp(fovEffectScale, 1.0, f)
 */
@Mixin(AbstractClientPlayerEntity.class)
@Environment(EnvType.CLIENT)
public abstract class MixinAbstractClientPlayerEntityClient {

    @Inject(method = "getFovMultiplier", at = @At("HEAD"), cancellable = true)
    private void sm_getFovMultiplier(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!cfg.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.fadingPerspectiveFactor == -1F) return;

        float f = 1.0F;
        if (player.getAbilities().flying) f *= 1.1F;
        float walkSpeed = player.getAbilities().getWalkSpeed();
        if (walkSpeed != 0F) {
            f *= (sm.fadingPerspectiveFactor / walkSpeed + 1F) / 2F;
        }
        if (walkSpeed == 0F || Float.isNaN(f) || Float.isInfinite(f)) f = 1.0F;

        if (player.isUsingItem()) {
            var activeItem = player.getActiveItem();
            if (activeItem.isOf(Items.BOW)) {
                float t = Math.min(player.getItemUseTime() / 20.0F, 1.0F);
                t = t * t;
                f *= 1.0F - t * 0.15F;
            } else if (MinecraftClient.getInstance().options.getPerspective().isFirstPerson()
                    && player.isUsingSpyglass()) {
                cir.setReturnValue(0.1F);
                return;
            }
        }

        float fovScale = ((Double) MinecraftClient.getInstance().options.getFovEffectScale().getValue()).floatValue();
        cir.setReturnValue(MathHelper.lerp(fovScale, 1.0F, f));
    }
}
