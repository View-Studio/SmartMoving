package choco.ratel.smartmoving.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 클라이언트 플레이어당 SM 상태 컴포넌트.
 * 서버의 SmartMovingServer에 대응하는 클라이언트 측 상태 관리 클래스.
 * Map<UUID, SmartMovingClientState> 방식 — @Unique 필드 주입 대신 사용.
 */
@Environment(EnvType.CLIENT)
public final class SmartMovingClientState {

    // ── 4-1: 점프 관련 필드 ──────────────────────────────────────────

    /** 다음 처리 틱에 점프 실행 */
    public boolean jumpPending;

    /** vanilla jump() 회피 여부 */
    public boolean jumpAvoided;

    /** 차지 점프 누적 (0.0 ~ Config.MaxJumpCharge) */
    public float jumpCharge;

    /** 헤드점프 차지 누적 */
    public float headJumpCharge;

    /** 버튼 릴리즈까지 점프 차단 */
    public boolean blockJumpTillButtonRelease;

    /** 스프린트 점프 상태 */
    public boolean isSprintJump;

    /** 헤드점프 상태 */
    public boolean isHeadJumping;

    /** 벽점프 상태 */
    public boolean isWallJumping;

    /** 방향 점프 타입 (0~7) */
    public int angleJumpType;

    /** 벽점프 연속 여부 */
    public boolean continueWallJumping;

    // ── 4-1: 이동 상태 필드 ──────────────────────────────────────────

    /** 히트박스 오프셋 (헤드점프 시 -1F) */
    public float heightOffset;

    /** 크롤링 상태 */
    public boolean isCrawling;

    /** 슬라이딩 상태 */
    public boolean isSliding;

    /** 클라이밍 상태 */
    public boolean isClimbing;

    /** 크롤-클라이밍 상태 */
    public boolean isCrawlClimbing;

    /** 천장 클라이밍 상태 */
    public boolean isCeilingClimbing;

    /** 작은 크기 상태 (크롤링/슬라이딩) */
    public boolean isSmall;

    // ── 5-8: 클라이밍 이동 거리 누적 (클라이언트 측) ──────────────────────
    /** 클라이밍 이동 거리 누적 (피로도 계산용). */
    public double distanceClimbedModified;

    // ── 8-1: 수중 상태 3분류 (SM 고유, vanilla isSwimming()과 별개) ─────────
    // 원본: SmartMoving.isDipping / isSwimming / isDiving 필드
    // offset = playerSwimWaterBorder + 0.1625D 기준:
    //   isDipping: offset < 1.4 (발만 물속)
    //   isSwimming_sm: 1.4 ≤ offset < 1.9 (수면 수영)
    //   isDiving: offset ≥ 1.9 (완전 잠수)

    /** 수면에 발만 잠긴 상태. offset < 1.4 */
    public boolean isDipping;

    /** 수면 수영 상태. 1.4 ≤ offset < 1.9. vanilla isSwimming()과 이름 충돌 방지를 위해 _sm 접미사 사용. */
    public boolean isSwimming_sm;

    /** 완전 잠수 상태. offset ≥ 1.9 */
    public boolean isDiving;

    // ── 8-2: 수중 이동 카운터 ─────────────────────────────────────────
    /** 물속 틱 카운터. isJumpingOutOfWater 조건(>10)에 사용. */
    public int waterMovementTicks;

    // ── 8-6: 수영 소리 거리 누적 ──────────────────────────────────────
    /** 수영 소리 누적 거리. SwimSoundDistance(≈1.4286F) 초과 시 소리 재생. */
    public double distanceSwom;

    // ── 인스턴스 관리 ─────────────────────────────────────────────────

    private static final Map<UUID, SmartMovingClientState> INSTANCES = new HashMap<>();

    public static SmartMovingClientState get(ClientPlayerEntity player) {
        return INSTANCES.computeIfAbsent(player.getUuid(), id -> new SmartMovingClientState());
    }

    public static void remove(ClientPlayerEntity player) {
        INSTANCES.remove(player.getUuid());
    }

    // ── 4-2: tickEssential() ─────────────────────────────────────────

    /**
     * isActive 여부 무관하게 매 틱 실행되는 필수 처리.
     * 원본: SmartMovingPlayerBase.updateEntityActionState() → moving.tickEssential()
     *
     * TODO Phase 5: 상태 패킷 전송, 키 입력 처리, jumpAvoided 리셋 등 구현
     */
    public void tickEssential() {
    }

    // ── 4-3: isConnectedToRemoteServer() ─────────────────────────────

    /**
     * 원격 서버(멀티플레이)에 접속 중인지 판별한다.
     *
     * 원본: MinecraftServer.getServer() == null
     *       || getIntegratedServer() == null
     *       || !getIntegratedServer().isSinglePlayer()
     * 1.21.1: MinecraftClient.getServer()는 IntegratedServer를 반환.
     *         싱글플레이어/LAN 서버 시 non-null, 원격 서버 시 null.
     */
    public static boolean isConnectedToRemoteServer() {
        return MinecraftClient.getInstance().getServer() == null;
    }
}
