package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.config.SmartMovingConfig;
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
        // BUG-9 (세션 36): SM disabled 시 multiPlayerInitialized 갱신 안 함 →
        //   sm_travel_client 의 pushOutOfBlocks 억제가 작동하지 않도록 보장 (BUG-7 확장).
        if (!SmartMovingConfig.Config.enabled) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        sm.multiPlayerInitialized = 5;
    }

    // [FLY-DBG-CLIENT-ENTITY-POS] vanilla EntityPositionS2CPacket 도착 시점 추적.
    @Inject(method = "onEntityPosition",
            at = @At("HEAD"))
    private void sm_dbgEntityPosition(net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket packet,
                                       CallbackInfo ci) {
        if (!choco.ratel.smartmoving.config.SmartMovingConfig.Config.enabled) return;
        net.minecraft.client.MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        net.minecraft.entity.Entity entity = mc.world.getEntityById(packet.getEntityId());
        if (entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity remote
                && !(entity instanceof ClientPlayerEntity)) {
            SmartMovingClientState sm = SmartMovingClientState.get(remote);
            if (sm.isFlying || sm.isSliding || sm.isCrawling) {
                System.out.println(String.format(
                    "[FLY-DBG-CLIENT-ENTITY-POS] tick=%d entId=%d cur.y=%.4f bb.minY=%.4f isFly=%b isSld=%b isCr=%b",
                    remote.age, entity.getId(), remote.getY(),
                    remote.getBoundingBox().minY,
                    sm.isFlying, sm.isSliding, sm.isCrawling));
            }
        }
    }

    // [FLY-DBG-CLIENT-ENTITY-S2C] vanilla EntityS2CPacket (= small delta) 도착 시점.
    @Inject(method = "onEntity",
            at = @At("HEAD"))
    private void sm_dbgEntityS2C(net.minecraft.network.packet.s2c.play.EntityS2CPacket packet,
                                  CallbackInfo ci) {
        if (!choco.ratel.smartmoving.config.SmartMovingConfig.Config.enabled) return;
        net.minecraft.client.MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        net.minecraft.entity.Entity entity = packet.getEntity(mc.world);
        if (entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity remote
                && !(entity instanceof ClientPlayerEntity)) {
            SmartMovingClientState sm = SmartMovingClientState.get(remote);
            if (sm.isFlying || sm.isSliding || sm.isCrawling) {
                // packet 이 small delta 송신. accessor 어렵 → 직전 cur.y 와 다음 tick lerp 결과로 추정.
                System.out.println(String.format(
                    "[FLY-DBG-CLIENT-ENTITY-S2C] tick=%d entId=%d cur.y=%.4f bb.minY=%.4f isFly=%b isSld=%b",
                    remote.age, entity.getId(), remote.getY(), remote.getBoundingBox().minY,
                    sm.isFlying, sm.isSliding));
            }
        }
    }
}
