package choco.ratel.smartmoving;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import choco.ratel.smartmoving.network.SmartMovingNetwork;
import choco.ratel.smartmoving.server.SmartMovingServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class SmartMoving implements ModInitializer {

    public static final String MOD_ID = "smartmoving";

    @Override
    public void onInitialize() {
        SmartMovingConfig.load();
        SmartMovingSounds.register();
        SmartMovingNetwork.register();
        registerServerReceivers();
        registerConnectionEvents();
    }

    private static void registerConnectionEvents() {
        // 3-7: 플레이어 접속 — SM 초기화 및 ConfigContent 패킷 전송
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                SmartMovingServer.initialize(handler.player, server));

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
                SmartMovingServer.get(sender).processStatePacket(sender, payload.state());
                context.server().execute(() -> {
                    for (ServerPlayerEntity tracker : PlayerLookup.tracking(sender)) {
                        if (tracker != sender) {
                            ServerPlayNetworking.send(tracker, payload);
                        }
                    }
                });
            });

        // ConfigInfo: 클라이언트 설정 정보 수신 → SM 버전 저장 (C-16)
        ServerPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.ConfigInfoPayload.ID,
            (payload, context) ->
                SmartMovingServer.get(context.player()).processConfigInfoPacket(payload.config()));

        // ConfigChange: 클라이언트 설정 변경 요청 수신 → 거부 응답 (C-17)
        ServerPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.ConfigChangePayload.ID,
            (payload, context) ->
                SmartMovingServer.processConfigChangePacket(context.player()));

        // SpeedChange: 클라이언트 속도 변경 요청 수신 → 권한 검증 후 응답 (C-18)
        ServerPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.SpeedChangePayload.ID,
            (payload, context) ->
                SmartMovingServer.processSpeedChangePacket(context.player(), payload.difference()));

        // HungerChange: 클라이언트 소진값 수신 — 서버 hunger 필드 갱신
        ServerPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.HungerChangePayload.ID,
            (payload, context) -> {
                SmartMovingServer.get(context.player()).hunger = payload.hunger();
            });

        // Sound: SM 사운드 요청 수신 → 주변 플레이어에게 재생 (C-19)
        ServerPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.SoundPayload.ID,
            (payload, context) -> {
                ServerPlayerEntity sender = context.player();
                SoundEvent event = Registries.SOUND_EVENT.get(Identifier.of(payload.soundId()));
                if (event == null) return;
                context.server().execute(() ->
                    sender.getServerWorld().playSound(
                        sender,
                        sender.getX(), sender.getY(), sender.getZ(),
                        event, SoundCategory.PLAYERS,
                        payload.volume(), payload.pitch()));
            });
    }
}
