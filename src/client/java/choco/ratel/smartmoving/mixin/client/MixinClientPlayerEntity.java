package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 4-2: tickEssential() 무조건 호출 Mixin.
 *
 * ClientPlayerEntity.tickMovement() HEAD에서 SM isActive 여부 무관하게
 * tickEssential()을 항상 호출한다.
 *
 * 원본: SmartMovingPlayerBase.updateEntityActionState()
 *       → moving.tickEssential()  ← isActive() 체크 없이 항상 호출
 *       → isActive() ? moving.updateEntityActionState(false) : localUpdateEntityActionState()
 */
@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity {

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void sm_tickMovement(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        SmartMovingClientState.get(player).tickEssential(player);
    }

    // R-01: tickMovement TAIL — 모든 이동 처리 후 State 패킷 전송
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void sm_sendStatePacket(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        SmartMovingClientState.get(player).sendStatePacket(player);
    }
}
