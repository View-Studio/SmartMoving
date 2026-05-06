package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import choco.ratel.smartmoving.network.SmartMovingNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

public class SmartMovingClient implements ClientModInitializer {

    /**
     * 🔴 (Phase 2 multi BUG-7 fix) remote ICC EXIT 시 entity.y up offset.
     *   ICC dim offset (= bb +1m, MixinEntity.sm_offsetBoundingBoxForFlying) 와 일치 →
     *   bb 일관성 유지. 그랩 climbing (= 일반 블록) ICC EXIT 시 사용.
     */
    public static final double ICC_EXIT_REMOTE_Y_OFFSET = 1.0;

    /**
     * 🔴 (Phase 2 multi BUG-7 fix) remote ICC EXIT 사다리류 (= ladder/vine/scaffolding) 전용 offset.
     *   사용자 보고 사다리에서 +1.0m fix 시 살짝 진동 → server-side 가 setPos +1m + gravity 후
     *   broadcast 까지 2 단계 (= +1m → 위 칸 바닥). client v25.3 fix 가 +1.0m 즉시 → 0.16m 너무 위 →
     *   vanilla lerp -0.16m down 추적 (= 5 tick = 250ms) 진동 인지.
     *   사다리류는 server.y diff 가 정확 +0.84m 정도 (= setPos +1m - gravity 0.16m).
     *   사용처: entity.isClimbing()=true (= ladder/vine/scaffolding) 시 적용.
     */
    public static final double ICC_EXIT_LADDER_Y_OFFSET = 0.84;

    /**
     * 🔴 v25.7.9 — watchfix Y threshold (= 진짜 풀림 detect 임계).
     *   svY < ourSetY - thresh = 진짜 위 칸 바닥 도달 (= +1m offset 풀려 빈 공간) → 즉시 setPos.
     *   svY < ourSetY 미세 차이 (= self gravity 결과 와 offset 차이, ladder 0.016m, 그랩 0.08m
     *   등) 는 thresh 안에 흡수 → ourSetY reset 만 → vanilla broadcast lerp 자연 흡수 (= dip 차단).
     *   ladder/그랩 별도 상수 (= self side 매핑 본질적 차이로 미세 조정 가능하게).
     */
    public static final double ICC_EXIT_LADDER_Y_THRESH = 0.5;
    public static final double ICC_EXIT_GRAB_Y_THRESH = 0.5;

    /**
     * 🔴 (2026-05-05 사용자 보고 — "a 콘피그 off 시 a 측에서 모두 vanilla 보임"):
     *   render-path 의 `Config.enabled` 검사가 자기 client cfg → a disabled 시 자기 측에서
     *   모든 player render 의 SM 분기 차단 → 모두 vanilla. remote 측은 자기 cfg=true 라 정상.
     *   해결: render-path enabled 검사를 self-only 로 한정.
     *     - self → 자기 Config.enabled
     *     - remote → 항상 true (= 자기 cfg 무관, remote 의 SM state 자체 분기)
     *   self 가 disabled 면 자기 SM state 모두 false (= state update / packet 송신 stop) 라
     *   remote 측에서 자기는 자동으로 vanilla 표시 (= 정확).
     */
    public static boolean isSmRenderEnabled(net.minecraft.entity.Entity entity) {
        if (SmartMovingConfig.Config.enabled) return true;
        // 자기 cfg disabled — remote player 만 통과 (= vanilla cfg 무관 + remote 의 SM 분기 정상).
        if (!(entity instanceof AbstractClientPlayerEntity ap)) return false;
        return ap != MinecraftClient.getInstance().player;
    }

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
            // 원본 SmartMovingServerConfig.reset() 대응 — SERVER_CONFIG 인스턴스 초기화.
            // 다음 서버 접속 시 이전 서버 설정 잔류 방지.
            SmartMovingConfig.resetServerConfig();
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
                    SmartMovingClientState target = SmartMovingClientState.get(entity.getUuid());
                    // 🔴 (Phase 2 multi BUG-7) remote ICC EXIT 시 박스/모델 1칸 down jump 차단.
                    //   원인: packet 처리 (= ICC=false 적용 + dim 변경) 시점과 PlayerEntity.tick HEAD
                    //         사이 1 tick lag → ICC dim offset (+1m bb) 사라진 후 entity.y 그대로 →
                    //         같은 tick 의 render frames 에서 bb=[entity.y..entity.y+0.8] (= 1m 아래) 보임.
                    //   해결: packet 처리 lambda 안에서 enter-edge 검출 + setPos +1m + lerp cancel
                    //         즉시 적용 → ICC=false 와 동시에 entity.y +1m → bb 일관성 유지.
                    //   self (= ClientPlayerEntity) 는 자체 fix (= 메모리 *project_isclimbcrawling_complete*).
                    boolean wasIcc = target.isClimbCrawling;
                    target.processStatePacket(payload.state());
                    // 🔴 (Phase 2 multi BUG-12/14) packet 도착 시 dim 즉시 갱신 → vanilla pose sync 대기
                    //   없이 sm.* 비트 따라 dim 결정. boolean OR 가드 제거 — 여러 비트 동시 변경 시
                    //   감지 누락 회피. dim 동일 시 vanilla 자체 영향 미미.
                    if (entity instanceof net.minecraft.entity.LivingEntity living) {
                        living.calculateDimensions();
                    }
                    if (wasIcc && !target.isClimbCrawling
                            && entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity remote
                            && !(entity instanceof net.minecraft.client.network.ClientPlayerEntity)) {
                        choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor acc =
                                (choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor) remote;
                        // 🔴 [TEMP DBG-7] fix 발동 직전 dump.
                        org.slf4j.LoggerFactory.getLogger("SM-DBG7-C").info(
                                "[B-FIX BEFORE] name={} | x={} y={} z={} | bodyYaw={} | svX={} svY={} svZ={} bti={} | bbY=[{}..{}] bbZ=[{}..{}]",
                                remote.getName().getString(),
                                String.format("%.4f", remote.getX()),
                                String.format("%.4f", remote.getY()),
                                String.format("%.4f", remote.getZ()),
                                String.format("%.2f", remote.getBodyYaw()),
                                String.format("%.4f", acc.sm_getServerX()),
                                String.format("%.4f", acc.sm_getServerY()),
                                String.format("%.4f", acc.sm_getServerZ()),
                                acc.sm_getBodyTrackingIncrements(),
                                String.format("%.3f", remote.getBoundingBox().minY),
                                String.format("%.3f", remote.getBoundingBox().maxY),
                                String.format("%.3f", remote.getBoundingBox().minZ),
                                String.format("%.3f", remote.getBoundingBox().maxZ)
                        );
                        // 사다리류 (= ladder/vine/scaffolding) detect → 작은 offset.
                        // 그랩류 (= 일반 블록 climbing) 는 ICC dim offset (= +1m) 그대로.
                        boolean isLadderType = remote.isClimbing();
                        double offset = isLadderType ? ICC_EXIT_LADDER_Y_OFFSET : ICC_EXIT_REMOTE_Y_OFFSET;
                        double newY = remote.getY() + offset;
                        remote.setPosition(remote.getX(), newY, remote.getZ());
                        // 🔴 v25.7.10 — render lerp prevY 동기화 (= 1 frame jump 차단).
                        //   사용자 보고 "ladder 덜컹": packet lambda 가 entity.y +0.84m setPos. 그러나
                        //   lastRenderY/prevY 는 ICC 마지막 값 (= 76.156) 그대로 → render = lerp(prevY,
                        //   y, pt) 가 partialTicks 0.18 안 +0.355m jump → 4 frame 매끄러운 +0.55m lerp →
                        //   "1회 충격 + 진행" = 덜컹. 동기화 시 render lerp dy=0 → 1 frame instant
                        //   teleport + 즉시 안정. ladder + 그랩 모두 적용 (= +0.84/+1.0 둘 다 같은 메커
                        //   니즘). Y 만 동기화 (= X/Z lerp 자유 진행).
                        remote.lastRenderY = newY;
                        remote.prevY = newY;
                        acc.sm_setServerY(newY);
                        // 🔴 v25.7.9 — ladder + 그랩 모두 X/Z 자유 lerp + watchfix threshold 적용.
                        //   v25.6 의 bti=0 부작용 (= "초반 뒤로 땡김"): broadcast 미도달 동안 X/Z lerp
                        //   차단 → 3 tick 정지 → 누적 lag → 다음 broadcast 도달 시 큰 catch-up.
                        //   ladder/그랩 별도 변수 (= self side 매핑 본질적 차이로 미세 조정 가능).
                        //   둘 다 setBti(0) skip + threshold 흡수 → vanilla setBti(3) + svY reset 으로
                        //   Y bobbing 만 차단, X/Z 자유 lerp.
                        // v25.6 watchfix — server-side 2 단계 broadcast 의 1차 ignore + 2차 즉시 setPos.
                        target.smIccExitWatchTicks = 10;
                        target.smIccExitLastSvY = newY;
                        target.smIccExitOurSetY = newY;
                        target.smIccExitBroadcastCount = 0;
                        target.smIccExitFirstBroadcastSvY = 0.0;
                        target.smIccExitIsLadder = isLadderType;
                        target.smIccExitYThresh = isLadderType ? ICC_EXIT_LADDER_Y_THRESH : ICC_EXIT_GRAB_Y_THRESH;

                        // v25.7 옵션 1 (revert) — bodyYaw force flip 시도. body vs head yaw mismatch
                        //   → 목 꺾임 BUG + body 가 vanilla turnHead lerp 으로 1 tick 후 server yaw 로
                        //   회복 → 사용자 시각 효과 미미. 다른 방향 검토 필요.
                        // 🔴 [TEMP DBG-7] fix 발동 직후 dump.
                        org.slf4j.LoggerFactory.getLogger("SM-DBG7-C").info(
                                "[B-FIX AFTER] name={} ladder={} offset={} | x={} y={} z={} | bodyYaw={} | svX={} svY={} svZ={} bti={} | bbY=[{}..{}] bbZ=[{}..{}]",
                                remote.getName().getString(),
                                isLadderType,
                                String.format("%.3f", offset),
                                String.format("%.4f", remote.getX()),
                                String.format("%.4f", remote.getY()),
                                String.format("%.4f", remote.getZ()),
                                String.format("%.2f", remote.getBodyYaw()),
                                String.format("%.4f", acc.sm_getServerX()),
                                String.format("%.4f", acc.sm_getServerY()),
                                String.format("%.4f", acc.sm_getServerZ()),
                                acc.sm_getBodyTrackingIncrements(),
                                String.format("%.3f", remote.getBoundingBox().minY),
                                String.format("%.3f", remote.getBoundingBox().maxY),
                                String.format("%.3f", remote.getBoundingBox().minZ),
                                String.format("%.3f", remote.getBoundingBox().maxZ)
                        );
                    }
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
        // 🔴 (2026-05-05 사용자 요청 — "config 는 자기 자체 toggle. 다른 player 무영향"):
        //   원본은 server admin 의 명시 활성 시만 broadcast 의도. 우리 server 매핑이
        //   무조건 broadcast → 모든 client Config 변경 → 모든 SM disabled BUG.
        //   해결: Config 자체 변경 제거. server-broadcast 무시 — 모든 client 자기 INSTANCE
        //   사용. 자기 disabled = 자기 SM state 갱신 안 함 → packet 송신 안 함 → 다른
        //   client 측에서 자기 vanilla 표시 (= 정확).
        //   SERVER_CONFIG 자체 갱신은 server-side 옵션 (= block-code 등) 이 사용 가능하도록
        //   유지. 그러나 client Config 는 INSTANCE 그대로.
        MinecraftClient client = MinecraftClient.getInstance();
        if (content == null || content.length == 0) {
            // 메시지만 (= 원본 message). Config 변경 안 함.
            return;
        }
        // SERVER_CONFIG 갱신 (= server-side 일부 옵션 read 용. client Config 무관).
        SmartMovingConfig.SERVER_CONFIG.loadFromArray(content);
        // ConfigInfo 송신 (= server 가 client 버전 인식).
        ClientPlayNetworking.send(new SmartMovingNetwork.ConfigInfoPayload(SmartMovingConfig.SM_VERSION));
    }

    // ── 4-4: processBlockCode ────────────────────────────────────────────────

    // 원본: SmartMovingComm.processBlockCode 를 채팅 히스토리 직접 스캔(Reflect+GuiNewChat)으로 처리.
    // 1.21.1: ClientReceiveMessageEvents.ALLOW_GAME 이벤트로 대체. 메시지 수신 시 즉시 처리 (updateCounter<10 스캔 불필요).
    // 원본 chatMessageList.remove(i--) 대응: ALLOW_GAME에서 false 반환으로 채팅 억제.
    private static void registerGameMessageHandler() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            // overlay=true는 액션바(핫바 위 표시) → 블록 코드 대상 아님 (B-19 확인)
            if (overlay) return true;
            String text = message.getString();
            if (text.startsWith("§0§1") && text.endsWith("§f§f")) {
                processBlockCode(text);
                return false; // 원본: chatMessageList.remove — 채팅창에서 억제
            }
            return true;
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
    // 호출 전 마커 검사(`§0§1`/`§f§f`)는 registerGameMessageHandler에서 완료됨.
    private static void processBlockCode(String text) {
        String codes = text.substring(4, text.length() - 4);
        SmartMovingConfig cfg = SmartMovingConfig.Config;

        // §0 → _baseClimb="standard" → _isFreeBaseClimb/Smart/Simple 모두 false
        if (codes.contains("§0")) { cfg.freeClimb = false; cfg.simpleClimb = false; cfg.smartClimb = false; }
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
