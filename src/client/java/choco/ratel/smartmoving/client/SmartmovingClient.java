package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import choco.ratel.smartmoving.network.SmartMovingNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class SmartMovingClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SmartMovingKeys.register();
        registerClientReceivers();
    }

    private static void registerClientReceivers() {
        // State: 다른 플레이어의 이동 상태 수신 (서버 릴레이)
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.StatePayload.ID,
            (payload, context) -> {
                // TODO Phase 4/6: SmartMovingFactory.getOtherSmartMoving(entityId).processStatePacket(state)
            });

        // ConfigContent: 서버 설정 수신 → Config = ServerConfig 전환
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.ConfigContentPayload.ID,
            (payload, context) -> {
                // TODO Phase 7/13: SmartMovingComm.processConfigContentPacket(payload.lines(), payload.username())
            });

        // ConfigChange: 서버 설정 변경 알림 수신
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.ConfigChangePayload.ID,
            (payload, context) -> {
                // TODO Phase 7/13: SmartMovingComm.processConfigChangePacket()
            });

        // SpeedChange: 서버에서 속도 변경 동기화
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.SpeedChangePayload.ID,
            (payload, context) -> {
                // TODO Phase 7: SmartMovingComm.processSpeedChangePacket(payload.difference(), payload.username())
            });
    }
}
