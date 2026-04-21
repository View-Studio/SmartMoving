package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import choco.ratel.smartmoving.network.SmartMovingNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class SmartMovingClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SmartMovingKeys.register();
        registerClientReceivers();
        registerConnectionEvents();
    }

    private static void registerConnectionEvents() {
        // 클라이언트 접속 해제 시 상태 인스턴스 정리
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.player != null) {
                SmartMovingClientState.remove(client.player);
            }
        });
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

    /**
     * 4-4: processBlockCode — 채팅 코드 기반 서버 설정 수신.
     * 원본: SmartMovingComm.processBlockCode(text)
     *   형식: "§0§1...§f§f" (앞 4자·뒤 4자 마커), 12개 기능 on/off 파싱
     *
     * [미확인 — 1.21.1 Text 시스템에서 §코드 접근 방법 확인 필요]
     * TODO Phase 7/13: ClientReceiveMessageEvents 훅 + Text.getString() 기반 파싱 구현
     */
    @SuppressWarnings("unused")
    private static void processBlockCode(String text) {
    }
}
