package choco.ratel.smartmoving.server;

import choco.ratel.smartmoving.network.SmartMovingNetwork;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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

    /** 서버 측 슬라이딩 상태 */
    public boolean isSliding;

    /** 서버 측 작은 크기 상태 (TODO Phase 6: EntityDimensions 적용) */
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
     *   bit 22: isSliding (원본 SM bit 22 위치 유지)
     *   bit 31: isWallJumping
     *   bit 33: isSneakButtonPressed
     */
    public void processStatePacket(long bits) {
        isClimbing        = ((bits >> 14) & 1) != 0;
        isCrawlClimbing   = ((bits >> 12) & 1) != 0;
        isCeilingClimbing = ((bits >> 18) & 1) != 0;
        isSliding         = ((bits >> 22) & 1) != 0;
        isWallJumping     = ((bits >> 31) & 1) != 0;
        isCrawling        = ((bits >> 13) & 1) != 0;
        isSmall           = ((bits >> 15) & 1) != 0;
        isSneakButtonPressed = ((bits >> 33) & 1) != 0;

        // 3-9: 낙하 거리 리셋 조건 (벽점프 포함)
        resetFallDistance     = isClimbing || isCrawlClimbing || isCeilingClimbing || isWallJumping;
        // 3-2: floatingTick 리셋 조건 (벽점프 제외 — 벽점프는 순간적이라 kick 위험 없음)
        resetTicksForFloatKick = isClimbing || isCrawlClimbing || isCeilingClimbing;
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
        player.setVelocity(vx, 0.08, vz);
    }

    // ── 3-3: 소진 배치 시스템 ────────────────────────────────────

    /**
     * 이동 허기 배치 시작 직전 호출 — 소진 추가를 막는다.
     * 원본 SmartMovingServerPlayerBase.afterUpdatePotionEffects() → moving.beforeAddMovingHungerBatch()
     * (before/after가 의도적으로 역전되어 있음 — 포션 업데이트 사이클 타이밍 맞춤)
     */
    public void beforeAddMovingHungerBatch() {
        disableAddExhaustionDepth++;
        disableAddExhaustion = disableAddExhaustionDepth > 0;
    }

    /**
     * 이동 허기 배치 완료 후 호출 — 소진 추가 차단을 해제한다.
     * 원본 SmartMovingServerPlayerBase.beforeUpdatePotionEffects() → moving.afterAddMovingHungerBatch()
     */
    public void afterAddMovingHungerBatch() {
        disableAddExhaustionDepth--;
        disableAddExhaustion = disableAddExhaustionDepth > 0;
    }

    // ── 3-7: 플레이어 접속 초기화 ────────────────────────────────

    /**
     * 플레이어 접속 시 서버→클라이언트 초기화 패킷 전송.
     * 원본: SmartMovingServer.initialize(player)
     *
     * ConfigContent 패킷으로 서버 설정 내용을 전달한다.
     *   lines = new String[0]: 빈 설정 (서버 독립 설정 없음)
     *   username = null: 설정 편집 권한 없음
     *
     * TODO: SmartMovingConfig 기반 실제 설정 배열 전송 구현 (Phase 12)
     */
    public static void initialize(ServerPlayerEntity player, MinecraftServer server) {
        SmartMovingServer sm = SmartMovingServer.get(player);
        if (sm.initialized) return;
        sm.initialized = true;
        ServerPlayNetworking.send(player,
                new SmartMovingNetwork.ConfigContentPayload(new String[0], null));
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
