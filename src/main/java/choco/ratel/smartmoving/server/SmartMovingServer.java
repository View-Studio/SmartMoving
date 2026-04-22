package choco.ratel.smartmoving.server;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import choco.ratel.smartmoving.network.SmartMovingNetwork;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

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
     *   bit 12: isCrawlClimbing
     *   bit 13: isCrawling
     *   bit 14: isClimbing
     *   bit 15: isSmall
     *   bit 18: isCeilingClimbing
     *   bit 31: isWallJumping
     *   bit 33: isSneakButtonPressed
     * 원본 미추출: isSliding(bit 21), angleJumpType(bits 22-24) 등 — 서버 물리에 불필요
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
        String[] lines = SmartMovingConfig.INSTANCE.globalConfig
                ? SmartMovingConfig.INSTANCE.toArray()
                : new String[0];
        ServerPlayNetworking.send(player,
                new SmartMovingNetwork.ConfigContentPayload(lines, null));
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
     * 클라이언트 속도 변경 요청 처리.
     * speedUser=true → 허용: difference 그대로 반환 → 클라이언트가 changeSpeed() 적용.
     * speedUser=false → 거부: difference=0 반환 → 클라이언트 "no rights" 메시지 표시.
     */
    public static void processSpeedChangePacket(ServerPlayerEntity player, int difference) {
        int response = SmartMovingConfig.Config.speedUser ? difference : 0;
        ServerPlayNetworking.send(player, new SmartMovingNetwork.SpeedChangePayload(response, null));
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
