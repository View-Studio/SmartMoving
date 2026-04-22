package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import choco.ratel.smartmoving.network.SmartMovingNetwork;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

    /**
     * 스니킹 속도로 이동 중 여부 (스프린트 없음 + 클라이밍 없음).
     * 원본: isSlow = wantSneak && !wantSprint && !isClimbing
     * C-15: tickEssential()에서 매 틱 계산.
     */
    public boolean isSlow;

    /**
     * 스프린팅+점프로 빠른 이동 중 여부 (스프린트 점프보다 빠름).
     * 원본: isFast = grabButton.Pressed && isSprinting() (또는 config 속도 임계값)
     * C-15: tickEssential()에서 매 틱 계산.
     */
    public boolean isFast;

    /**
     * 비행 중 여부 (vanilla flight 또는 SM fly).
     * 원본: flying = sp.capabilities.isFlying
     * C-15: tickEssential()에서 매 틱 계산.
     */
    public boolean isFlying;

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

    /** 클라이밍 점프 상태 */
    public boolean isClimbJumping;

    /** 로프 슬라이딩 상태 */
    public boolean isRopeSliding;

    // ── 12-7: 위 블록까지의 거리 (isCrawlClimbing || isHeadJumping 시 사용) ─
    /** 머리 위 블록까지의 거리. 최대 5.0F. */
    public float smallOverGroundHeight;

    // ── 9-5: 슬라이딩 파티클 타이머 ──────────────────────────────────────
    // 원본 필드명 오타(Slinding) 그대로 보존
    /** 슬라이딩 파티클 누적 타이머. _slideParticlePeriodFactor × 0.1F 초과 시 파티클 생성. */
    public float spawnSlindingParticle;

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

    // ── 12-2: isAngleJumping() ───────────────────────────────────────────
    /**
     * 방향 점프 중인지 여부.
     * 원본: SmartMoving.isAngleJumping() → angleJumpType > 1 && angleJumpType < 7
     */
    public boolean isAngleJumping() {
        return angleJumpType > 1 && angleJumpType < 7;
    }

    // ── 4-2: tickEssential() ─────────────────────────────────────────

    /**
     * isActive 여부 무관하게 매 틱 실행되는 필수 처리.
     * 원본: SmartMovingPlayerBase.updateEntityActionState() → moving.tickEssential()
     */
    public void tickEssential() {
        // 이전 틱 값 초기화 — vanilla jump() 가로채기(sm_jump)에서 당 틱에 새로 설정됨
        jumpAvoided = false;

        // 설정 토글 키 처리 (원본: toggleButton.update() + StartPressed 분기)
        if (SmartMovingKeys.configToggle.wasPressed()) {
            if (SmartMovingConfig.Config == SmartMovingConfig.INSTANCE) {
                SmartMovingConfig.INSTANCE.toggle();
            } else {
                ClientPlayNetworking.send(new SmartMovingNetwork.ConfigChangePayload());
            }
        }

        // SM 비활성 시 이동 상태 전체 초기화 (원본: !isActive() → resetState())
        if (!SmartMovingConfig.Config.enabled) {
            resetState();
        }
    }

    // 원본: SmartMovingSelf.resetState() — 비활성 시 모든 이동 상태를 기본값으로 리셋.
    // heightOffset 위치 복원(resetHeightOffset)은 C-20에서 처리.
    private void resetState() {
        heightOffset = 0F;
        isSlow = false;
        isFast = false;
        isFlying = false;
        isClimbing = false;
        isClimbJumping = false;
        isWallJumping = false;
        isCrawlClimbing = false;
        isCeilingClimbing = false;
        isRopeSliding = false;
        isDipping = false;
        isSwimming_sm = false;
        isDiving = false;
        isHeadJumping = false;
        isCrawling = false;
        isSliding = false;
        angleJumpType = 0;
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
