package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityClientMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void smartMoving_tick(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        // 로컬 클라이언트 플레이어만 상태 기계 실행
        if (self != MinecraftClient.getInstance().player) return;

        SmartMovingState state = self.getAttachedOrCreate(SmartMovingAttachments.STATE);
        state.tick(self);
    }
}
