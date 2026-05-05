package choco.ratel.smartmoving.server;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import choco.ratel.smartmoving.network.SmartMovingNetwork;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 서버 플레이어당 SM 상태 컴포넌트.
 * 원본 SmartMovingServerPlayerBase + SmartMovingServer를 Map 기반으로 통합.
 * Mixin @Unique 주입 대신 UUID→SmartMovingServer 맵 방식을 사용 —
 * ServerPlayerEntity 계층에 필드를 직접 주입하면 mixins 충돌 위험이 크고,
 * 플레이어 생명주기(JOIN/DISCONNECT)와 결합하면 Map 방식이 더 안전하다.
 */
public final class SmartMovingServer {

    /** 서버 콘솔 로그. 원본: FMLLog (Forge). 1.21.1: SLF4J (vanilla 로거 체인). */
    private static final Logger LOGGER = LoggerFactory.getLogger("smartmoving");

    /** 크롤링 중 아이템 습득 Y방향 확장 범위. 원본: SmartMovingServer.SmallSizeItemGrabHeight = 0.25F */
    public static final double SMALL_SIZE_ITEM_GRAB_HEIGHT = 0.25;

    // ── 3-1 서버 상태 필드 ────────────────────────────────────────

    /** afterOnUpdate()에서 낙하 거리 리셋 여부 (isClimbing 계열이 true인 동안 유지) */
    public boolean resetFallDistance;

    /** afterOnUpdate()에서 floatingTicks 리셋 여부 (클라이밍 3종) */
    public boolean resetTicksForFloatKick;

    /** initialize() 완료 여부 */
    public boolean initialized;

    /** onLivingUpdate 진행 중 여부 */
    public boolean withinOnLivingUpdate;

    /** 크롤링 종료 후 쿨다운 (10틱) — isInsideWall 억제에 사용 */
    public int crawlingCooldown;

    /** 서버 측 크롤링 상태 */
    public boolean isCrawling;

    /** 서버 측 작은 크기 상태. setSmall() → calculateDimensions() 경로로 서버 AABB 갱신. */
    public boolean isSmall;

    /** 클라이언트 소진값. -1=억제 해제, 0=소진 없음 */
    public float hunger = -1F;

    /** 소진 억제 중첩 깊이 */
    public int disableAddExhaustionDepth;

    /** 소진 추가 비활성화 여부 */
    public boolean disableAddExhaustion;

    /** 클라이언트 스니킹 버튼 상태 (State 패킷 bit 33) */
    public boolean isSneakButtonPressed;

    /** isSneaking() 강제 오버라이드. null=비강제, true/false=강제 반환값 */
    public Boolean forceIsSneaking;

    /** 클라이밍 이동 거리 누적 (피로도 계산용). 원본 distanceClimbedModified 이식. */
    public double distanceClimbedModified;

    /** 클라이언트가 전송한 SM 버전 문자열 (ConfigInfo 패킷). null=미수신 */
    public String clientVersion;

    // ── 패킷에서 디코딩된 이동 상태 ──────────────────────────────

    public boolean isClimbing;
    public boolean isCrawlClimbing;
    public boolean isCeilingClimbing;
    public boolean isWallJumping;

    // ── 포커스 #2.7 D-1 (세션 4): 클라/서버 POSE 완전 대칭화 — 5 필드 추가 ──
    // 원본은 PacketStream 7 비트 sync 만, 서버 dimensions 결정은 isSmall 단일 의존.
    // 그러나 1.21.1 vanilla POSE 시스템은 양쪽 동일 분기 매핑 필요 (datatracker sync 진동
    // 방지). Phase 1 client 측이 이미 8 SM 상태 → 4 분기 (SWIMMING/SLIDING) 매핑하므로,
    // 서버도 동일 분기 처리하려면 추가 비트 디코딩 필요. SmartMovingState 는 이미 21 bit
    // 인코딩 (bit 9 isDiving / 11 isSwimming / 19 isLevitating / 20 isHeadJumping /
    // 21 isSliding) — 서버에서 디코딩만 추가하면 됨.

    /** SmartMovingState bit 9. SM 다이빙 상태. POSE.SWIMMING 매핑. */
    public boolean isDiving;
    /** SmartMovingState bit 11. SM 수영 상태. POSE.SWIMMING 매핑. */
    public boolean isSwimming;
    /** SmartMovingState bit 19. Levitate 포션 상태. POSE.SLIDING 매핑. */
    public boolean isLevitating;
    /** SmartMovingState bit 20. SM 헤드 점프 상태. POSE.SLIDING 매핑. */
    public boolean isHeadJumping;
    /** SmartMovingState bit 21. SM 슬라이딩 상태. POSE.SLIDING 매핑. */
    public boolean isSliding;
    /**
     * SmartMovingState bit 22. SM 클라이밍 크롤 상태. 박스 +1 mixin offset 적용.
     * 사용자 보고 fix (X/Z 땡김): ICC 활성 중 클라/서버 박스 1m Y 차이 → server reconcile.
     * 클라/서버 dim 동기화 위해 추가 비트.
     */
    public boolean isClimbCrawling;

    /**
     * 🔴 (Phase 2 multi network 최적화) server-side broadcast dirty 검사용.
     *   매 tick client 가 동일 state packet 보내도, server 가 마지막 broadcast 한 bits 와 비교 →
     *   변경 시만 다른 client 에게 broadcast. trafic 80-90% 절약 (= 100 player hub 환경 등).
     *   late join 처리: EntityTrackingEvents.START_TRACKING 시 lastBroadcastBits 를 신규 tracker 에게 replay.
     */
    public long lastBroadcastBits = 0L;

    // ── 인스턴스 관리 ─────────────────────────────────────────────

    private static final Map<UUID, SmartMovingServer> INSTANCES = new HashMap<>();

    /** 플레이어의 SmartMovingServer 인스턴스를 반환한다. 없으면 새로 생성한다. */
    public static SmartMovingServer get(ServerPlayerEntity player) {
        return INSTANCES.computeIfAbsent(player.getUuid(), id -> new SmartMovingServer());
    }

    /** 플레이어 접속 해제 시 인스턴스를 제거한다. */
    public static void remove(ServerPlayerEntity player) {
        INSTANCES.remove(player.getUuid());
    }

    // ── 3-1 + 3-9: State 패킷 처리 ──────────────────────────────

    /**
     * State 패킷의 34비트 long에서 서버가 필요한 비트만 추출한다.
     * 원본 SmartMovingServer.processStatePacket() 이식.
     *
     * 서버가 읽는 비트:
     *   bit 12: isCrawlClimbing  (원본)
     *   bit 13: isCrawling       (원본)
     *   bit 14: isClimbing       (원본)
     *   bit 15: isSmall          (원본)
     *   bit 18: isCeilingClimbing (원본)
     *   bit 31: isWallJumping    (원본)
     *   bit 33: isSneakButtonPressed (원본)
     *
     * **포커스 #2.7 D-1 (세션 4)**: 클라/서버 POSE 완전 대칭화 — 5 비트 추가 디코딩.
     * 원본 PacketStream 미사용 비트 (1.21.1 SmartMovingState 가 이미 인코딩) 를 서버에서
     * 디코딩하여 sm_updatePose_server 가 클라와 동일 4 분기 처리 가능.
     *   bit  9: isDiving        ★ 신규 (Phase D-1, POSE.SWIMMING 매핑)
     *   bit 11: isSwimming      ★ 신규 (Phase D-1, POSE.SWIMMING)
     *   bit 19: isLevitating    ★ 신규 (Phase D-1, POSE.SLIDING)
     *   bit 20: isHeadJumping   ★ 신규 (Phase D-1, POSE.SLIDING)
     *   bit 21: isSliding       ★ 신규 (Phase D-1, POSE.SLIDING)
     */
    public void processStatePacket(ServerPlayerEntity player, long bits) {
        isClimbing        = ((bits >> 14) & 1) != 0;
        isCrawlClimbing   = ((bits >> 12) & 1) != 0;
        isCeilingClimbing = ((bits >> 18) & 1) != 0;
        isWallJumping     = ((bits >> 31) & 1) != 0;
        setCrawling(((bits >> 13) & 1) != 0);
        // R-04: setSmall() 경유하여 calculateDimensions() 호출 → 서버 AABB 갱신
        boolean newSmall = ((bits >> 15) & 1) != 0;
        if (newSmall != isSmall) setSmall(player, newSmall);
        isSneakButtonPressed = ((bits >> 33) & 1) != 0;

        // ── 포커스 #2.7 D-1 신규 디코딩 (POSE 완전 대칭화) ──────────────
        isDiving       = ((bits >>  9) & 1) != 0;
        isSwimming     = ((bits >> 11) & 1) != 0;
        isLevitating   = ((bits >> 19) & 1) != 0;
        isHeadJumping  = ((bits >> 20) & 1) != 0;
        isSliding      = ((bits >> 21) & 1) != 0;
        // ── X/Z 땡김 fix: 클라/서버 박스 동기화 (bit 34, bit 22 는 angleJumpType 사용) ──
        boolean newClimbCrawling = ((bits >> 34) & 1) != 0;
        if (newClimbCrawling != isClimbCrawling) {
            isClimbCrawling = newClimbCrawling;
            player.calculateDimensions();
        }

        // 3-9: 낙하 거리 리셋 조건 (벽점프 포함)
        resetFallDistance     = isClimbing || isCrawlClimbing || isCeilingClimbing || isWallJumping;
        // 3-2: floatingTick 리셋 조건 (벽점프 제외 — 벽점프는 순간적이라 kick 위험 없음)
        resetTicksForFloatKick = isClimbing || isCrawlClimbing || isCeilingClimbing;
    }

    // ── 3-1-A: 크롤링 상태 전환 + cooldown 설정 ──────────────────────

    /**
     * 크롤링 상태를 갱신하고, 종료 시 crawlingCooldown을 10으로 설정한다.
     * 원본: SmartMovingServer.setCrawling(boolean)
     * cooldown 중에는 isInsideWall()이 false를 반환하여 크롤링 출구 벽 충돌 오판 방지.
     */
    public void setCrawling(boolean crawling) {
        if (!crawling && isCrawling) crawlingCooldown = 10;
        isCrawling = crawling;
    }

    // ── 3-9: 낙하 거리 리셋 ──────────────────────────────────────

    /**
     * 클라이밍/크롤클라이밍/천장클라이밍/벽점프 중 낙하 거리 리셋.
     * 원본: fallDistance=0, motionY=0.08 (중력 상쇄 최소 상향 초기 속도).
     */
    public void applyFallDistanceReset(ServerPlayerEntity player) {
        player.fallDistance = 0;
        double vx = player.getVelocity().x;
        double vz = player.getVelocity().z;
        player.setVelocity(vx, player.getAttributeValue(EntityAttributes.GENERIC_GRAVITY), vz);
    }

    // ── 3-3: 소진 배치 시스템 ────────────────────────────────────

    /**
     * 이동 허기 배치 시작 직전 호출 — 소진 추가를 막는다.
     * 원본 SmartMovingServerPlayerBase.afterUpdatePotionEffects() → moving.beforeAddMovingHungerBatch()
     * (before/after가 의도적으로 역전되어 있음 — 포션 업데이트 사이클 타이밍 맞춤)
     */
    public void beforeAddMovingHungerBatch() {
        disableAddExhaustionDepth++;
        // 원본: if(hunger != -1) disableAddExhaustion = true;
        // hunger = -1F (초기/미수신) → vanilla 소진 허용, hunger >= 0 → SM이 override
        if (hunger >= 0F) disableAddExhaustion = true;
    }

    /**
     * 이동 허기 배치 완료 후 호출 — 소진 추가 차단을 해제한다.
     * 원본 SmartMovingServerPlayerBase.beforeUpdatePotionEffects() → moving.afterAddMovingHungerBatch()
     */
    public void afterAddMovingHungerBatch() {
        disableAddExhaustionDepth--;
        disableAddExhaustion = disableAddExhaustionDepth > 0;
    }

    // ── 3-6: 서버 히트박스 동적 변경 ─────────────────────────────

    /**
     * 서버 측 isSmall 히트박스 갱신.
     * calculateDimensions()가 getBaseDimensions()(MixinPlayerEntity)를 호출하여
     * 서버 bounding box 갱신 + 클라이언트 동기화가 자동으로 이루어진다.
     */
    public void setSmall(ServerPlayerEntity player, boolean small) {
        this.isSmall = small;
        player.calculateDimensions();
    }

    // ── 3-7: 플레이어 접속 초기화 ────────────────────────────────

    /**
     * 플레이어 접속 시 서버→클라이언트 초기화 패킷 전송.
     * 원본: SmartMovingServer.initialize(player) (config_system.md 2-1 기반)
     *
     * globalConfig=true: 서버 설정 전체를 전송 → 클라이언트 Config = ServerConfig로 전환됨.
     * 그 외: new String[0] → 클라이언트에 위임 (Config = Options 유지).
     * username=null: 설정 편집 권한 없음.
     *
     * serverConfig(개인별 설정) 지원은 미구현 — globalConfig 우선.
     */
    public static void initialize(ServerPlayerEntity player, MinecraftServer server) {
        SmartMovingServer sm = SmartMovingServer.get(player);
        if (sm.initialized) return;
        sm.initialized = true;
        // 🔴 (2026-05-05 사용자 요청 — "config 는 자기 자체 toggle. 다른 player 무영향"):
        //   server INSTANCE.globalConfig=true (default) 라 모든 client 에 config broadcast →
        //   client Config = SERVER_CONFIG 강제 → 모든 client SM enabled/disabled 통제 BUG.
        //   해결: broadcast 자체 stop. 각 client 가 자기 INSTANCE 사용.
        //   server-side block-code 등 일부 옵션 사용자 명시 시 별도 작업.
        logConfigState(SmartMovingConfig.INSTANCE, null, false);
    }

    // ── 3-12: 관리자 자발적 config/speed 변경 + 전체 재전송 ───────────────────

    /**
     * 현재 접속 중인 모든 initialized 플레이어에게 ConfigContent 를 재전송한다.
     * 원본: SmartMovingServerComm 이 config 변경 후 각 플레이어 session 을 순회하며 writeToProperties 전송.
     * 1.21.1: initialize 와 동일 경로를 재사용하되, sm.initialized 체크 대신 강제 전송.
     */
    public static void broadcastConfig(MinecraftServer server) {
        // 🔴 (2026-05-05 사용자 요청 — "config 자기 자체 toggle. 다른 player 무영향"):
        //   adminToggleConfig (= server console / admin 권한) 가 호출하면 모든 client 의
        //   Config 변경 → 모든 player SM disabled BUG. broadcast 자체 stop.
        //   server-side INSTANCE 변경은 server-only 옵션 (= block-code) 용으로 유지.
    }

    /**
     * 관리자 자발적 config 토글.
     * 원본: SmartMovingServerOptions.toggle(player) (L66-L71):
     *   config.toggle(); saveToOptionsFile; logConfigState(config, username, true);
     * 1.21.1: SmartMovingConfig.INSTANCE.toggle() + save() + log + 전체 재전송.
     */
    public static void adminToggleConfig(ServerPlayerEntity player) {
        SmartMovingConfig.INSTANCE.toggle();
        SmartMovingConfig.save();
        logConfigState(SmartMovingConfig.INSTANCE, player.getName().getString(), true);
        MinecraftServer server = player.getServer();
        if (server != null) broadcastConfig(server);
    }

    /**
     * 관리자 자발적 전역 속도 변경.
     * 원본: SmartMovingServerOptions.changeSpeed(diff, player) (L73-L78):
     *   config.changeSpeed(diff); saveToOptionsFile; logSpeedState(config, username);
     * 1.21.1: INSTANCE.changeSpeed(diff) + save + log + 전체 재전송.
     * 클라이언트 요청 수락 경로(processSpeedChangePacket=changeSingleSpeed)와 별개 — 전역 변경.
     */
    public static void adminChangeSpeed(ServerPlayerEntity player, int difference) {
        SmartMovingConfig.INSTANCE.changeSpeed(difference);
        SmartMovingConfig.save();
        logSpeedState(SmartMovingConfig.INSTANCE, player.getName().getString());
        MinecraftServer server = player.getServer();
        if (server != null) broadcastConfig(server);
    }

    // ── 3-11: 서버 콘솔 로그 (원본: SmartMovingServerOptions 로그 메서드) ─────────

    /**
     * 원본: SmartMovingServerOptions.logConfigState(config, username, reconfig)
     *       (SmartMovingServerOptions.md L80-L113).
     *
     * H-18 (세션 23) 2갈래 단순화: 포커스 #5 "disabled↔enabled 2상태" 결정 후 `currentKey`
     * 는 항상 null (configKeys={null}). 원본 3갈래 분기 중 `currentKey==null` 경로만 살아
     * 있으므로 `default server configuration` 한 줄로 단순화. 나머지 2갈래 (with key /
     * named) 는 Medium/Hard/Creative 프리셋 복원 시(`focus_09_difficulty_presets.md` 후속)
     * 재도입.
     *
     * globalConfig=true 경로:
     *   reconfig=false → "Smart Moving overrides client configurations" 먼저 출력.
     *   enabled=true  → "... default server configuration [by X]"
     *   enabled=false → "... disabled [by X]"
     * globalConfig=false:
     *   → "Smart Moving allows client configurations"
     */
    static void logConfigState(SmartMovingConfig config, String username, boolean reconfig) {
        String message = "Smart Moving ";
        if (config.globalConfig) {
            if (!reconfig) LOGGER.info("{}overrides client configurations", message);
            String postfix = getPostfix(username);
            if (config.enabled) {
                String action = reconfig ? "changed to " : "uses ";
                LOGGER.info("{}{}default server configuration{}", message, action, postfix);
            } else {
                LOGGER.info("{}disabled{}", message, postfix);
            }
        } else {
            LOGGER.info("{}allows client configurations", message);
        }
    }

    /** 원본: SmartMovingServerOptions.logSpeedState(config, username). */
    static void logSpeedState(SmartMovingConfig config, String username) {
        LOGGER.info("Smart Moving speed set to {}%{}", config.getSpeedPercent(), getPostfix(username));
    }

    /** 원본: SmartMovingServerOptions.getPostfix(username). username=null → 빈 문자열. */
    private static String getPostfix(String username) {
        if (username == null) return "";
        return " by user '" + username + "'";
    }

    // ── C-16: ConfigInfo 수신 — 클라이언트 SM 버전 저장 ─────────────

    /** 원본: SmartMovingComm.processConfigInfoPacket (server-side) — 클라이언트 버전 기록. */
    public void processConfigInfoPacket(String info) {
        clientVersion = info;
    }

    // ── C-17: ConfigChange 수신 — 서버 설정 변경 권한 거부 응답 ────────

    /**
     * 클라이언트가 서버 설정 토글을 요청했으나 서버 설정이 활성화된 경우 항상 거부.
     * 원본: 서버가 ConfigChange S2C를 전송 → 클라이언트 "no rights" 메시지 표시.
     */
    public static void processConfigChangePacket(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new SmartMovingNetwork.ConfigChangePayload());
    }

    // ── C-18: SpeedChange 수신 — 권한 검증 후 속도 변경 동기화 ─────────

    /**
     * 클라이언트 속도 변경 요청 처리 — 원본 SmartMovingServerOptions.changeSingleSpeed(player, diff) 대응.
     * speedUser=true → 허용: INSTANCE.changeSingleSpeed(username, diff) + save → difference 응답.
     * speedUser=false → 거부: difference=0 반환 → 클라이언트 "no rights" 메시지 표시.
     *
     * 원본 setPlayerSpeedExponent 흐름(개인 맵 put + saveToOptionsFile)만 수행, logSpeedState 호출 없음.
     * 관리자 자발적 전역 변경(원본 changeSpeed + logSpeedState)은 별도 서버 커맨드 이식 시 추가.
     */
    public static void processSpeedChangePacket(ServerPlayerEntity player, int difference) {
        // B-6 (세션 26): Creative 전용 게이트. 원본 `isUserSpeedEnabled() = enabled &&
        // _speedUser.value` 에서 `_speedUser` 가 Creative 팩토리라 Creative 에서만 true.
        // Creative 아니면 서버가 0 응답 → 클라 "no rights" 메시지 → 기능 거부. 원본 1:1.
        boolean isCreative = player.interactionManager.getGameMode()
                == net.minecraft.world.GameMode.CREATIVE;
        if (!SmartMovingConfig.Config.speedUser || !isCreative) {
            ServerPlayNetworking.send(player, new SmartMovingNetwork.SpeedChangePayload(0, null));
            return;
        }
        String username = player.getName().getString();
        SmartMovingConfig.INSTANCE.changeSingleSpeed(username, difference);
        SmartMovingConfig.save();
        ServerPlayNetworking.send(player, new SmartMovingNetwork.SpeedChangePayload(difference, null));
    }

    // ── 3-8: 권한 확인 ───────────────────────────────────────────

    /**
     * config/speed 변경 권한 확인.
     * 원본은 == 참조 비교였으나 1.21.1에서 String 인터닝 보장 없으므로 equals()로 교체.
     */
    public static boolean hasPermission(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return expected.equals(actual);
    }
}
