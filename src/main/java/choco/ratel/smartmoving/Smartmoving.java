package choco.ratel.smartmoving;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import choco.ratel.smartmoving.network.SmartMovingNetwork;
import choco.ratel.smartmoving.server.SmartMovingServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public class SmartMoving implements ModInitializer {

    public static final String MOD_ID = "smartmoving";

    @Override
    public void onInitialize() {
        SmartMovingConfig.load();
        SmartMovingNetwork.register();
        registerServerReceivers();
        registerConnectionEvents();
    }

    private static void registerConnectionEvents() {
        // 3-7 stub: 플레이어 접속 — 향후 설정 전송 등 초기화에 활용
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // TODO Phase 7: SmartMovingServer 초기화 및 설정 패킷 전송
        });

        // 플레이어 접속 해제 — 인스턴스 정리
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SmartMovingServer.remove(handler.player);
        });
    }

    private static void registerServerReceivers() {
        // State: 클라이언트 이동 상태 수신 → 추적 플레이어에게 릴레이 + 서버 상태 갱신
        ServerPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.StatePayload.ID,
            (payload, context) -> {
                ServerPlayerEntity sender = context.player();
                SmartMovingServer.get(sender).processStatePacket(payload.state());
                context.server().execute(() -> {
                    for (ServerPlayerEntity tracker : PlayerLookup.tracking(sender)) {
                        if (tracker != sender) {
                            ServerPlayNetworking.send(tracker, payload);
                        }
                    }
                });
            });

        // ConfigInfo: 클라이언트 설정 정보 수신
        ServerPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.ConfigInfoPayload.ID,
            (payload, context) -> {
                // TODO Phase 7: SmartMovingServer.processConfigInfoPacket(context.player(), payload.config())
            });

        // ConfigChange: 클라이언트 설정 변경 요청 수신
        ServerPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.ConfigChangePayload.ID,
            (payload, context) -> {
                // TODO Phase 7: SmartMovingServer.processConfigChangePacket(context.player())
            });

        // SpeedChange: 클라이언트 속도 변경 요청 수신
        ServerPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.SpeedChangePayload.ID,
            (payload, context) -> {
                // TODO Phase 7: SmartMovingServer.processSpeedChangePacket(payload.difference(), payload.username())
            });

        // HungerChange: 클라이언트 소진값 수신 — 서버 hunger 필드 갱신
        ServerPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.HungerChangePayload.ID,
            (payload, context) -> {
                SmartMovingServer.get(context.player()).hunger = payload.hunger();
            });

        // Sound: SM 사운드 요청 수신
        ServerPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.SoundPayload.ID,
            (payload, context) -> {
                // TODO Phase 3: SmartMovingServer.processSoundPacket(context.player(), payload)
            });
    }
}
