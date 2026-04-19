package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.sound.SmartMovingSounds;
import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
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
        state.tick(self);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void smartMoving_tickSounds(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self != MinecraftClient.getInstance().player) return;

        SmartMovingState state = self.getAttached(SmartMovingAttachments.STATE);
        if (state == null) return;

        // 14틱마다 소리 재생 (걷기 소리와 비슷한 주기)
        if (state.updateCounter % 14 != 0) return;

        if (state.isClimbing && (state.wantClimbUp || state.wantClimbDown)) {
            self.getWorld().playSound(self, self.getBlockPos(),
                    SmartMovingSounds.CLIMB, SoundCategory.PLAYERS, 0.4F, 1.0F);
        } else if (state.isCeilingClimbing) {
            self.getWorld().playSound(self, self.getBlockPos(),
                    SmartMovingSounds.CEILING_CLIMB, SoundCategory.PLAYERS, 0.3F, 1.1F);
        } else if (state.isSliding) {
            self.getWorld().playSound(self, self.getBlockPos(),
                    SmartMovingSounds.SLIDE, SoundCategory.PLAYERS, 0.6F, 0.9F);
        }
    }
}
