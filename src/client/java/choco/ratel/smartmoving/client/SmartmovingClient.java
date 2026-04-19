package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.client.input.InputHandler;
import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import choco.ratel.smartmoving.client.network.SmartMovingClientNetworking;
import choco.ratel.smartmoving.client.render.SmartMovingHud;
import choco.ratel.smartmoving.network.RemotePlayerManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

public class SmartmovingClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SmartMovingKeys.register();
        InputHandler.register();
        SmartMovingClientNetworking.register();
        SmartMovingHud.register();

        // 매 틱 로컬 플레이어 상태를 서버로 전송
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.getNetworkHandler() != null) {
                SmartMovingClientNetworking.sendStateIfChanged(client.player);
            }
        });

        // 서버 연결 해제 시 상태 초기화
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SmartMovingClientNetworking.resetPrevState();
        });
    }
}
