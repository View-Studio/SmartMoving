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
        if (self != MinecraftClient.getInstance().player) return;

        SmartMovingState state = self.getAttachedOrCreate(SmartMovingAttachments.STATE);
        boolean wasSmall = state.isSmall();
        state.tick(self);
        // 히트박스 크기 전환 시 실제 BoundingBox 갱신 (1블록 공간 통과 가능)
        if (state.isSmall() != wasSmall) {
            self.calculateDimensions();
        }
    }
}
