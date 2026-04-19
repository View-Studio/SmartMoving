package choco.ratel.smartmoving.network;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class SmartMovingServerNetworking {

    public static void register() {
        PayloadTypeRegistry.playC2S().register(
                SmartMovingStatePayload.ID,
                SmartMovingStatePayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                SmartMovingStatePayload.ID,
                SmartMovingStatePayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                SmartMovingStatePayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity sender = context.player();
                    // 서버 스레드에서 실행
                    context.server().execute(() -> {
                        // 서버 측 상태 갱신 (히트박스, 낙하 거리 등)
                        SmartMovingState state = sender.getAttachedOrCreate(SmartMovingAttachments.STATE);
                        StateEncoder.decode(payload.state(), state);

                        // 낙하 거리 리셋
                        if (state.isClimbing || state.isCeilingClimbing || state.isWallJumping) {
                            sender.fallDistance = 0F;
                        }

                        // 주변 플레이어들에게 relay (자신 제외)
                        SmartMovingStatePayload relayPayload =
                                new SmartMovingStatePayload(sender.getId(), payload.state());
                        for (ServerPlayerEntity watcher : PlayerLookup.tracking(sender)) {
                            if (watcher != sender) {
                                ServerPlayNetworking.send(watcher, relayPayload);
                            }
                        }
                    });
                }
        );
    }

    private SmartMovingServerNetworking() {}
}
