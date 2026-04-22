package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * beforeSetPositionAndRotation: 서버→클라이언트 위치 동기화 직후 multiPlayerInitialized 세팅.
 * 원본: SmartMovingPlayerBase.beforeSetPositionAndRotation (SmartMovingPlayerBase.java)
 *       → moving.initialized = false; moving.multiPlayerInitialized = 5;
 *
 * 1.21.1: setPositionAndRotation 단일 메서드 없음.
 * onPlayerPositionLook이 setPosition+setYaw+setPitch를 개별 호출하므로
 * HEAD에서 multiPlayerInitialized = 5 세팅.
 */
@Mixin(ClientPlayNetworkHandler.class)
@Environment(EnvType.CLIENT)
public abstract class MixinClientPlayNetworkHandler {

    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"))
    private void sm_beforePlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        sm.multiPlayerInitialized = 5;
    }
}
