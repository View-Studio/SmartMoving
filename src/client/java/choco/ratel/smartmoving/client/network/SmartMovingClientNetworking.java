package choco.ratel.smartmoving.client.network;

import choco.ratel.smartmoving.network.RemotePlayerManager;
import choco.ratel.smartmoving.network.SmartMovingStatePayload;
import choco.ratel.smartmoving.network.StateEncoder;
import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class SmartMovingClientNetworking {

    /** 이전 프레임에 전송된 상태값 (delta compression). */
    private static long prevPacketState = Long.MIN_VALUE;

    public static void register() {
        // 서버로부터 원격 플레이어 상태 수신
        ClientPlayNetworking.registerGlobalReceiver(
                SmartMovingStatePayload.ID,
                (payload, context) -> {
                    MinecraftClient client = context.client();
                    client.execute(() -> {
                        ClientWorld world = client.world;
                        if (world == null) return;

                        Entity entity = world.getEntityById(payload.entityId());
                        if (!(entity instanceof PlayerEntity player)) return;
                        // 로컬 플레이어는 서버 relay를 무시 (자신의 상태는 직접 계산)
                        if (player == client.player) return;

                        RemotePlayerManager.applyRemoteState(player, payload.state());
                    });
                }
        );
    }

    /**
     * 로컬 플레이어 상태를 서버로 전송 (변경된 경우에만).
     * ClientTickEvents.END_CLIENT_TICK 에서 호출.
     */
    public static void sendStateIfChanged(PlayerEntity player) {
        SmartMovingState state = player.getAttached(SmartMovingAttachments.STATE);
        if (state == null) return;

        long current = StateEncoder.encode(state);
        if (current == prevPacketState) return;
        prevPacketState = current;

        ClientPlayNetworking.send(new SmartMovingStatePayload(player.getId(), current));
    }

    /** 월드 전환 시 이전 상태 초기화. */
    public static void resetPrevState() {
        prevPacketState = Long.MIN_VALUE;
    }

    private SmartMovingClientNetworking() {}
}
