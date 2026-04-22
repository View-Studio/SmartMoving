package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import choco.ratel.smartmoving.network.SmartMovingNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

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
        // State: 다른 플레이어의 이동 상태 수신 (서버 릴레이) — C-24
        // 원본: SmartMovingFactory.getOtherSmartMoving(entityId).processStatePacket(state)
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.StatePayload.ID,
            (payload, context) -> {
                MinecraftClient client = context.client();
                client.execute(() -> {
                    ClientWorld world = client.world;
                    if (world == null) return;
                    Entity entity = world.getEntityById(payload.entityId());
                    if (entity == null) return;
                    SmartMovingClientState.get(entity.getUuid()).processStatePacket(payload.state());
                });
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
        // A-26: 원문 확인 — remote/local 구분 메시지 (en_us.json smartmoving.message.config.illegal.*)
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.ConfigChangePayload.ID,
            (payload, context) -> {
                MinecraftClient client = context.client();
                boolean isRemote = isRemoteServer(client);
                String key = isRemote
                    ? "smartmoving.message.config.illegal.remote"
                    : "smartmoving.message.config.illegal.local";
                client.execute(() -> {
                    if (client.player != null) client.player.sendMessage(Text.translatable(key));
                });
            });

        // SpeedChange: 서버에서 속도 변경 동기화 (C-12)
        // 원본: difference==0 → 권한없음, !=0 → Config.changeSpeed(difference) + writeClientSpeedMessageToChat
        // A-26: 권한없음 메시지 원문 확인 (en_us.json smartmoving.message.speed.illegal.*)
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.SpeedChangePayload.ID,
            (payload, context) -> {
                MinecraftClient client = context.client();
                int difference = payload.difference();
                if (difference != 0) {
                    client.execute(() -> {
                        SmartMovingConfig.Config.changeSpeed(difference);
                        // IMPL-05: 속도 변경 채팅 피드백 (원본: writeClientSpeedMessageToChat)
                        // 100% = 기본값("reset"), 그 외 = 변경된 값("change")
                        String percent = SmartMovingConfig.Config.getSpeedPercent();
                        String msgKey = "100".equals(percent)
                            ? "smartmoving.message.speed.client.reset"
                            : "smartmoving.message.speed.client.change";
                        if (client.player != null)
                            client.player.sendMessage(Text.translatable(msgKey, percent));
                    });
                } else {
                    boolean isRemote = isRemoteServer(client);
                    String key = isRemote
                        ? "smartmoving.message.speed.illegal.remote"
                        : "smartmoving.message.speed.illegal.local";
                    client.execute(() -> {
                        if (client.player != null) client.player.sendMessage(Text.translatable(key));
                    });
                }
            });
    }

    // 원본: SmartMovingComm.isConnectedToRemoteServer() — MinecraftServer.getServer()==null 등 3조건
    // 1.21.1: client.getServer()가 null이면 원격 서버 (통합 서버 없음)
    private static boolean isRemoteServer(MinecraftClient client) {
        return client.getServer() == null;
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
    // A-26: 메시지 문자열 원본 확인 완료. 키 → en_us.json smartmoving.message.config.server.*
    private static void processConfigContentPacket(String[] content) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (content == null) {
            // SM 완전 비활성 — Config = INSTANCE 유지
            return;
        }
        if (content.length == 0) {
            // 서버가 클라이언트 자체 설정에 위임
            // 원본 메시지: "Using local Smart Moving configurations"
            SmartMovingConfig.Config = SmartMovingConfig.INSTANCE;
            if (client.player != null)
                client.player.sendMessage(Text.translatable("smartmoving.message.config.server.local"));
            return;
        }
        // 첫 수신 여부 추적 (원본: first = Config != ServerConfig)
        boolean first = SmartMovingConfig.Config != SmartMovingConfig.SERVER_CONFIG;
        boolean wasEnabled = SmartMovingConfig.Config.enabled;
        // 서버 설정 수신 → SERVER_CONFIG 갱신
        SmartMovingConfig.SERVER_CONFIG.loadFromArray(content);
        if (first) {
            // 최초 서버 설정 적용 → Config = SERVER_CONFIG 전환
            SmartMovingConfig.Config = SmartMovingConfig.SERVER_CONFIG;
            // 원본 메시지: "Using Smart Moving server configuration"
            if (client.player != null)
                client.player.sendMessage(Text.translatable("smartmoving.message.config.server.global"));
            // 클라이언트 버전 정보를 서버에 전송 (원본: sendConfigInfo(instance, _sm_current))
            ClientPlayNetworking.send(new SmartMovingNetwork.ConfigInfoPayload(SmartMovingConfig.SM_VERSION));
        } else {
            // first=false: 재설정 — Config = SERVER_CONFIG는 이미 유지됨
            // 원본은 wasEnabled/username/isGloballyConfigured로 세분화; 여기서는 단순화.
            // 원본 단순 메시지: enabled→"update", disabled→"enable" or "disable"
            String key;
            if (wasEnabled && SmartMovingConfig.Config.enabled)
                key = "smartmoving.message.config.server.update";
            else if (SmartMovingConfig.Config.enabled)
                key = "smartmoving.message.config.server.enable";
            else
                key = "smartmoving.message.config.server.disable";
            if (client.player != null)
                client.player.sendMessage(Text.translatable(key));
        }
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
