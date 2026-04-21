package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 9-6: 크롤링 중 렌더 Y 오프셋 적용.
 * 원본: SmartMovingPlayerBase.getYOffset() 이식.
 *
 * SWIMMING 포즈를 크롤링에 재사용하므로 vanilla getPositionOffset()이
 * 수영 오프셋을 그대로 적용한다. SM은 이를 취소하고
 * 자체 오프셋(-scale × 0.125D)만 적용한다.
 */
@Mixin(PlayerEntityRenderer.class)
@Environment(EnvType.CLIENT)
public class MixinPlayerEntityRenderer {

    /**
     * 크롤링 중 getPositionOffset() 오버라이드.
     * vanilla SWIMMING 포즈 오프셋(−1.0 × scale translate) 대신
     * SM 크롤링 오프셋(-scale × 0.125D)을 반환한다.
     *
     * 원본 getYOffset(): -0.125F (scale=1 기준).
     */
    @Inject(method = "getPositionOffset(Lnet/minecraft/client/network/AbstractClientPlayerEntity;F)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"), cancellable = true)
    private void sm_getPositionOffset(AbstractClientPlayerEntity entity, float tickDelta,
                                       CallbackInfoReturnable<Vec3d> cir) {
        if (!(entity instanceof net.minecraft.client.network.ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isCrawling) {
            cir.setReturnValue(new Vec3d(0D, -entity.getScale() * 0.125D, 0D));
        }
    }
}
