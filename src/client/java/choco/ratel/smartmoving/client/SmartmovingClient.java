package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import choco.ratel.smartmoving.network.SmartMovingNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class SmartMovingClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SmartMovingKeys.register();
        SmartMovingHud.register();
        registerClientReceivers();
        registerConnectionEvents();
        registerGameMessageHandler();
    }

    private static void registerConnectionEvents() {
        // 클라이언트 접속 해제 시 상태 인스턴스 정리 + Config 복원
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.player != null) {
                SmartMovingClientState.remove(client.player);
            }
            // 서버 설정 전환을 복원 — 다음 서버 접속까지 클라이언트 설정 사용
            SmartMovingConfig.Config = SmartMovingConfig.INSTANCE;
        });
    }

    private static void registerClientReceivers() {
        // State: 다른 플레이어의 이동 상태 수신 (서버 릴레이)
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.StatePayload.ID,
            (payload, context) -> {
                // TODO Phase 4/6: SmartMovingFactory.getOtherSmartMoving(entityId).processStatePacket(state)
            });

        // ConfigContent: 서버 설정 수신 → Config 전환 (C-11)
        // 원본: SmartMovingComm.processConfigContentPacket(content, username, blockCode=false)
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.ConfigContentPayload.ID,
            (payload, context) -> {
                String[] lines = payload.lines();
                MinecraftClient client = context.client();
                client.execute(() -> processConfigContentPacket(lines));
            });

        // ConfigChange: 서버가 "설정 변경 권한 없음"을 알림 (C-12)
        // 원본: SmartMovingOptions.writeNoRightsToChangeConfigMessageToChat(isConnectedToRemoteServer())
        // 1.21.1: 메시지 문자열 미확인 → 수신만 처리 (no-op)
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.ConfigChangePayload.ID,
            (payload, context) -> { /* no rights to change config — message content unconfirmed */ });

        // SpeedChange: 서버에서 속도 변경 동기화 (C-12)
        // 원본: difference==0 → 권한없음, !=0 → Config.changeSpeed(difference) (SmartMovingComm.md 확인)
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.SpeedChangePayload.ID,
            (payload, context) -> {
                int difference = payload.difference();
                if (difference != 0) {
                    context.client().execute(() -> SmartMovingConfig.Config.changeSpeed(difference));
                }
                // difference==0: 서버가 권한없음 알림 — 메시지 문자열 미확인 → 생략
            });
    }

    // ── 13-1/13-2: processConfigContentPacket ────────────────────────────────
    //
    // 원본: SmartMovingComm.processConfigContentPacket / processConfigPacket (SmartMovingComm.md 확인)
    // config_system.md 2-4 / 3-4 기반.
    //
    // content 값별 처리:
    //   null         → SM 완전 비활성. Config = INSTANCE 유지.
    //   length == 0  → 서버 설정 없음, 클라이언트에 위임. Config = INSTANCE.
    //   length > 0   → SERVER_CONFIG.loadFromArray(content) → Config = SERVER_CONFIG.
    //
    // 첫 수신(first=true) 시: sendConfigInfo 패킷 전송 (원본: SmartMovingConfig._sm_current = "3.2")
    // 주의: 서버 연결 해제 시 Config 복원은 registerConnectionEvents() DISCONNECT에서 처리.
    private static void processConfigContentPacket(String[] content) {
        if (content == null) {
            // SM 완전 비활성 — Config = INSTANCE 유지
            return;
        }
        if (content.length == 0) {
            // 서버가 클라이언트 자체 설정에 위임
            SmartMovingConfig.Config = SmartMovingConfig.INSTANCE;
            return;
        }
        // 첫 수신 여부 추적 (원본: first = Config != ServerConfig)
        boolean first = SmartMovingConfig.Config != SmartMovingConfig.SERVER_CONFIG;
        // 서버 설정 수신 → SERVER_CONFIG 갱신
        SmartMovingConfig.SERVER_CONFIG.loadFromArray(content);
        if (first) {
            // 최초 서버 설정 적용 → Config = SERVER_CONFIG 전환
            SmartMovingConfig.Config = SmartMovingConfig.SERVER_CONFIG;
            // 클라이언트 버전 정보를 서버에 전송 (원본: sendConfigInfo(instance, _sm_current))
            ClientPlayNetworking.send(new SmartMovingNetwork.ConfigInfoPayload(SmartMovingConfig.SM_VERSION));
        }
        // first=false: 재설정 — Config = SERVER_CONFIG는 이미 유지됨, 채팅 메시지 생략(문자열 미확인)
    }

    // ── 4-4: processBlockCode ────────────────────────────────────────────────

    // 원본: SmartMovingComm.processBlockCode 를 채팅 히스토리 직접 스캔(Reflect+GuiNewChat)으로 처리.
    // 1.21.1: ClientReceiveMessageEvents.GAME 이벤트로 대체. 메시지 수신 시 즉시 처리 (updateCounter<10 스캔 불필요).
    // 원본 채팅 메시지 제거(chatMessageList.remove) 대응:
    //   ALLOW_GAME 이벤트에서 false 반환으로 억제 가능하나, 미확인 항목(mapping 미기재) → 미구현.
    private static void registerGameMessageHandler() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            // overlay=true는 액션바(핫바 위 표시) → 블록 코드 대상 아님 (B-19 확인)
            if (overlay) return;
            processBlockCode(message.getString());
        });
    }

    // 원본: SmartMovingComm.processBlockCode(String text) — public static boolean
    // 1.21.1: SmartMovingConfig.Config 필드 직접 설정으로 단순화 (C-11: Config 전환 연동).
    //
    // 형식: "§0§1...§f§f" (앞 4자=시작마커, 뒤 4자=끝마커)
    // codes에 포함된 §코드에 해당하는 기능을 설정값으로 변경:
    //   §0 → baseClimb = true ("standard"), §1~§b → 각 기능 = false
    // codes에 없는 코드는 변경하지 않음 (현재 값 유지).
    //
    // Text.getString() §코드 포함 여부: 서버가 LiteralText로 전송한 경우 포함 가능 (B-19 확인).
    private static void processBlockCode(String text) {
        if (!text.startsWith("§0§1") || !text.endsWith("§f§f")) return;

        String codes = text.substring(4, text.length() - 4);
        SmartMovingConfig cfg = SmartMovingConfig.Config;

        if (codes.contains("§0")) cfg.baseClimb = true;
        if (codes.contains("§1")) cfg.freeClimb = false;
        if (codes.contains("§2")) cfg.ceilingClimbing = false;
        if (codes.contains("§3")) cfg.swim = false;
        if (codes.contains("§4")) cfg.dive = false;
        if (codes.contains("§5")) cfg.crawl = false;
        if (codes.contains("§6")) cfg.slide = false;
        if (codes.contains("§7")) cfg.fly = false;
        if (codes.contains("§8")) cfg.jumpCharge = false;
        if (codes.contains("§9")) cfg.headJump = false;
        if (codes.contains("§a")) cfg.angleJumpSide = false;
        if (codes.contains("§b")) cfg.angleJumpBack = false;
    }
}
