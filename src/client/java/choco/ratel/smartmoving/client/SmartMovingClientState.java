package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import choco.ratel.smartmoving.network.SmartMovingNetwork;
import choco.ratel.smartmoving.network.SmartMovingState;
import choco.ratel.smartmoving.stat.SmartStatistics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

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

    /**
     * 원본: SmartMovingSelf.wallJumpCount — 더블클릭 타이머.
     * 0 일 때 jump StartPressed → wallJumpDoubleClickTicks(정수) 로 설정,
     * 그 다음 jump StartPressed → triggerWallJumping=true + 0 리셋. else 매 틱 --.
     */
    public int wallJumpCount;

    /**
     * 원본: SmartMovingSelf.triggerWallJumping — 더블클릭 감지 플래그.
     * wantWallJumping 식에 반영된 뒤 역할 종료. 매 틱 시작에 자동 리셋(원본 리서치
     * 파일에 명시적 리셋 위치 기록 없음 — 보수적 채택).
     */
    public boolean triggerWallJumping;

    /**
     * 원본: SmartMovingSelf.wantWallJumping — handleWallJumping 진입 조건.
     * canWallJumping && (trigger || continue || (prev wantWallJumping && jumpPressed && !collided)).
     */
    public boolean wantWallJumping;

    /**
     * 원본 jumpButton.StartPressed (이번 틱에 새로 눌림) 에 대응하는 프레임 이벤트.
     * tickEssential 초반에 prevJumpKeyPressed 와 비교해 계산하며, sm_travel_client 내
     * 여러 핸들러(handleJumping, updateWallJumpState 등)에서 재참조 가능.
     */
    public boolean jumpKeyStartPressed;

    /** jumpKey.isPressed() 이전 틱 값 — jumpKeyStartPressed 엣지 감지용 내부 추적. */
    public boolean prevJumpKeyPressed;

    // ── 4-1: 이동 상태 필드 ──────────────────────────────────────────

    /** 히트박스 오프셋 (헤드점프 시 -1F) */
    public float heightOffset;

    /**
     * 현재 수심 — 발 기준 물 높이(m). isTouchingWater()=false이면 -1F.
     * 원본: SmartMovingSelf.dippingDepth (행 89), handleSwimming()에서 매 틱 갱신.
     * 1.21.1: player.getFluidHeight(FluidTags.WATER) 로 근사.
     * canCrawl / handleSwimming SwimCrawlWater 전환 판정에 사용.
     */
    public float dippingDepth = -1F;

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

    /** 클라이밍 홀딩 — 수직 이동 없이 제자리 유지. setShouldClimbSpeed에서 relevant=false 시 참조. */
    public boolean isClimbHolding;

    /** 클라이밍 중 크롤 공간 전환 상태 (손이 낮은 천장 아래로 들어갈 때). */
    public boolean isClimbCrawling;

    /** 머리 위에 크롤 갭이 있는지 여부. setShouldClimbSpeed 속도 상한 판정용. */
    public boolean hasClimbCrawlGap;

    /** 크롤 갭 진입 카운터. > 0이면 setShouldClimbSpeed에서 HoldMotion 강제. */
    public int climbIntoCount;

    /** 로프 슬라이딩 상태 */
    public boolean isRopeSliding;

    /**
     * 원본: SmartMovingSelf.isJumping (bit 8 of State 패킷).
     * 일반 점프/공중 상태. 애니메이션(isDive)에서 수직각 0 적용 조건.
     * 로컬 플레이어는 sendStatePacket 에서 즉석 계산, 원격 플레이어는 processStatePacket 에서 갱신.
     */
    public boolean isJumping;

    /**
     * 원본: SmartMovingSelf.doFallingAnimation (bit 16 of State 패킷).
     * 낙하 애니메이션 플래그. 원격 플레이어 추락 애니메이션에 사용.
     */
    public boolean doFallingAnimation;

    /**
     * 원본: SmartMovingSelf.isLevitating (bit 19 of State 패킷).
     * 로프 등 부양 상태. SM 1.21.1 로프 미구현이므로 로컬은 항상 false.
     * 애니메이션(isDive)에서 Quarter-Sixteenth 수직각 적용 조건.
     */
    public boolean isLevitating;

    // ── R-01: State 패킷 인코딩용 클라이밍 타입 필드 ─────────────────────────
    /** 현재 발 클라이밍 타입 (FeetClimbing.ordinal()). getOnLadderOrVine() 결과 저장. */
    public int actualFeetClimbType;
    /** 현재 손 클라이밍 타입 (HandsClimbing.ordinal()). getOnLadderOrVine() 결과 저장. */
    public int actualHandsClimbType;
    /** 발이 넝쿨 클라이밍 중인지 여부 (사다리와 구분). */
    public boolean isFeetVineClimbing;
    /** 손이 넝쿨 클라이밍 중인지 여부 (사다리와 구분). */
    public boolean isHandsVineClimbing;
    /** 클라이밍 백점프 상태 (SmartMovingJumper에서 갱신). */
    public boolean isClimbBackJumping;
    /** 마지막으로 전송한 State 비트맵 (중복 전송 방지). */
    private long lastSentBits = 0L;

    // ── IMPL-01: 크롤링 토글 상태 ────────────────────────────────────────
    /** 크롤링이 토글로 진입됨 — 다음 grab.wasPressed()로 해제 */
    public boolean crawlToggled;
    /** toCrawling() 직후 한 틱 스니크 StopPressed 무시 플래그 */
    public boolean ignoreNextStopSneakButtonPressed;

    /**
     * 원본 SmartMovingSelf L3099 `sneakToggled = false`.
     * 스닉 토글 모드에서 sneak 활성 상태 유지. L2576 sneakContinueInput 계산에 사용:
     *   sneakContinueInput = isSneakToggleEnabled ? sneakToggled || sneakStartPressed : sneakPressed.
     */
    public boolean sneakToggled;

    /**
     * 원본 SmartMovingSelf L95 `isFakeShallowWaterSneaking`.
     * 얕은 물에서의 가짜 스니킹 제스처 판정. wouldWantSneak 의
     *   !(isSwimming && swimDownOnSneak && !isFakeShallowWaterSneaking)
     * 조건에 사용 — true 면 수영 + swimDownOnSneak 상태에서도 스니크 허용.
     * 1.21.1: true 설정 경로는 현재 리서치 불충분 → 기본 false 유지, 후속 이식.
     */
    public boolean isFakeShallowWaterSneaking;

    /**
     * 원본 SmartMovingSelf L1797-L1802/L2419-L2432 `wantCrawl` = isCrawlingEnabled && wouldWantCrawl.
     * wouldWantSneak 조건(L2584)에 `&& !wantCrawl` 로 사용. IMPL-01 크롤링 진입 조건과 동일 값.
     */
    public boolean wantCrawl;

    /**
     * 원본 SmartMovingSelf L1792-L1794 `mustCrawl` = 머리 위 공간 부족 여부.
     * wouldWantSneak 조건(L2585)에 `&& !mustCrawl` 로 사용. IMPL-01 크롤링 유지 강제 조건과 동일.
     */
    public boolean mustCrawl;

    /**
     * 원본 SmartMovingSelf L3077 `private boolean contextContinueCrawl`.
     * 깊은 물 다이빙 → 물 아래 포복 전환 시 fromSwimmingOrDiving(L1388) 에서 true 로 설정.
     * wouldWantCrawl 식(L2419)의 `isCrawling && (inputContinueCrawl || contextContinueCrawl)` 조건에 사용.
     * 해제 경로: L2411(inputContinue/물속/mustCrawl), L2416(천장 액체 여유), L2447(!isCrawling).
     *
     * 1.21.1: true 설정 경로(fromSwimmingOrDiving)는 후속 이식 — 현재는 항상 false 유지,
     * 해제 로직만 선제 이식(원본 1:1).
     */
    public boolean contextContinueCrawl;

    /** 원본 L2717 `wasSneaking = isSlow` (이전 틱 isSlow 저장, willStartSneak/willStopSneak 조건용). */
    public boolean wasSneaking;

    /** 원본 wasCrawling — 이전 틱 isCrawling 저장 (willStartCrawl 조건 `isCrawling && !wasCrawling`). */
    public boolean wasCrawling_st;

    /** 원본 wasClimbCrawling — 이전 틱 isClimbCrawling 저장. */
    public boolean wasClimbCrawling;

    /** 원본 sneakButton.StartPressed — 이번 틱 스닉키 엣지(새로 눌림). */
    public boolean sneakKeyStartPressed;
    /** 원본 sneakButton.StopPressed — 이번 틱 스닉키 엣지(새로 뗌). */
    public boolean sneakKeyStopPressed;
    /** sneakKey.isPressed() 이전 틱 값 — 엣지 감지 내부 추적. */
    public boolean prevSneakKeyPressed;

    /** 원본 jumpButton.StopPressed — 이번 틱 점프키 엣지(새로 뗌). StartPressed는 jumpKeyStartPressed. */
    public boolean jumpKeyStopPressed;

    // ── IMPL-03: 더블클릭 방향 점프 카운터 ──────────────────────────────
    /** A키 더블클릭 카운터. 0=비활성, >0=첫 클릭 대기, -1=발동 예약, -2=대각선 대기. */
    public int leftJumpCount;
    /** D키 더블클릭 카운터. */
    public int rightJumpCount;
    /** S키 더블클릭 카운터. */
    public int backJumpCount;
    /** 점프 직전 저장 velocity.x (getJumpMoving 계산용). */
    public double jumpMotionX;
    /** 점프 직전 저장 velocity.z. */
    public double jumpMotionZ;
    /** 이전 틱 A키 상태 (rising-edge 감지용). */
    private boolean prevPressLeft;
    /** 이전 틱 D키 상태. */
    private boolean prevPressRight;
    /** 이전 틱 S키 상태. */
    private boolean prevPressBack;

    // ── C-33: SM 독자 exhaustion (클라이밍 피로도) ──────────────────────────
    /** 이전 틱 클라이밍 여부 — exhaustion 허용 조건 판정용. */
    public boolean wasClimbing;
    /** SM 독자 피로도 (0 ~ climbExhaustionStop 사이). 매 틱 감소, 클라이밍 중 증가. */
    public float exhaustion;

    // ── H-8: SM 허기 축적 (원본 SmartMovingSelf L863/L893/L911) ────────────
    /**
     * 원본 SmartMovingSelf 의 `hungerIncrease` 필드. 매 틱 `handleExhaustion` 에서 누적:
     *   L863: `hungerIncrease += _alwaysHungerGain + movement * 0.0001F * hungerGainFactor`
     *   L893: `hungerIncrease += _exhaustionLossHungerFactor * exhaustionLoss`
     * 틱 끝에서 `lastHungerIncrease` 와 비교하여 변화 시 서버 전송 (원본 L911-L915).
     * 누적값 — reset 없음 (서버가 수신해서 vanilla addExhaustion 연동).
     */
    public float hungerIncrease;

    /**
     * 원본 SmartMovingSelf.lastHungerIncrease — 이전 전송값 캐시. 변화 감지로 중복 전송 방지.
     * 원본 L911 `if (hungerIncrease != lastHungerIncrease) { sendHungerChange; lastHungerIncrease = hungerIncrease; }`.
     */
    public float lastHungerIncrease;

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

    /**
     * 수영 애니메이션 standSneakFactor 캐시.
     * sm_animateSwimming()에서 매 프레임 계산하여 저장하고,
     * sm_setupTransforms()의 bipedOuter X 기울기 계산에서 1프레임 지연으로 소비된다.
     * 원본: SmartMovingModel.setRotationAngles() isSwim 분기의 standSneakFactor.
     * 정지/스니킹=1, 보행=0.
     */
    public float swimStandSneakFactor = 0f;

    // ── FOV / perspective ──────────────────────────────────────────────
    /**
     * 속도 기반 FOV 배율의 EMA 누적값 (원본: SmartMovingSelf.fadingPerspectiveFactor).
     * 초기값 -1F = "아직 미초기화" 표시 → 첫 틱에 landMovementFactor로 직접 초기화.
     */
    public float fadingPerspectiveFactor = -1F;

    // ── isSneaking / forceIsSneaking ───────────────────────────────────
    /**
     * 스니크 의도 여부 (토글 포함). 원본: wouldIsSneaking = wouldWantSneak && !wantSprint && !isClimbing.
     * 1.21.1 구현에서는 isSlow와 동일.
     */
    public boolean wouldIsSneaking;

    /**
     * isSneaking() 강제 재정의. null=미사용. true/false=강제 반환.
     * 원본: SmartMovingSelf.forceIsSneaking (Boolean).
     */
    public Boolean forceIsSneaking = null;

    // ── flyWhileOnGround ───────────────────────────────────────────────
    /**
     * beforeOnLivingUpdate에서 저장한 직전 capabilities.isFlying 값.
     * afterOnLivingUpdate에서 flyWhileOnGround 복원 판정에 사용.
     */
    public boolean wasCapabilitiesIsFlying;

    /**
     * 직전 틱의 수평 충돌 여부 (벽점프 판정용).
     * 원본: SmartMovingSelf.beforeOnUpdate() → wasCollidedHorizontally = sp.isCollidedHorizontally
     * tickEssential()에서 vanilla physics 실행 전(HEAD)에 캡처 → 벽에 닿아 있던 이전 틱 상태를 반영.
     * 1.21.1 대응: player.horizontalCollision
     */
    public boolean wasCollidedHorizontally;

    /**
     * 슬라이드→헤드점프 전환 시 true — 공기역학적 수평 감쇠(0.999F) 적용.
     * 원본: SmartMovingSelf.isAerodynamic (행 2533~2560)
     * true 조건: isSliding && fallDistance > 0.05F 로 헤드점프 전환됐을 때만.
     * false 조건: 헤드점프가 꺼질 때 / 슬라이딩 새로 시작할 때.
     */
    public boolean isAerodynamic;

    // ── multiPlayerInitialized ─────────────────────────────────────────
    /**
     * 서버→클라이언트 위치 동기화 직후 pushOutOfBlocks 억제 카운터.
     * beforeSetPositionAndRotation에서 5로 세팅, pushOutOfBlocks 호출마다 1씩 감소.
     * 원본: SmartMovingSelf.multiPlayerInitialized.
     */
    public int multiPlayerInitialized;

    // ── C-25: SmartStatistics ──────────────────────────────────────────
    /** 이동 통계 인스턴스. move() TAIL 이후 calculate()로 갱신. */
    public final SmartStatistics stats = new SmartStatistics();

    // ── 인스턴스 관리 ─────────────────────────────────────────────────

    private static final Map<UUID, SmartMovingClientState> INSTANCES = new HashMap<>();

    public static SmartMovingClientState get(ClientPlayerEntity player) {
        return INSTANCES.computeIfAbsent(player.getUuid(), id -> new SmartMovingClientState());
    }

    /** 타 플레이어용 — UUID로 직접 조회/생성 (C-24: State 패킷 수신) */
    public static SmartMovingClientState get(java.util.UUID uuid) {
        return INSTANCES.computeIfAbsent(uuid, id -> new SmartMovingClientState());
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

    // ── C-24: processStatePacket() ────────────────────────────────────

    /**
     * 타 플레이어 State 패킷의 34비트 long에서 클라이언트가 필요한 비트를 추출한다.
     * 원본: SmartMovingOther.processStatePacket(long state)
     * 렌더링/애니메이션에 사용되는 필드만 갱신한다.
     */
    public void processStatePacket(long bits) {
        actualFeetClimbType  = (int) (bits & 0xF);           // bits 0-3
        actualHandsClimbType = (int) ((bits >> 4) & 0xF);    // bits 4-7
        isJumping         = ((bits >>  8) & 1) != 0;   // 원본 SmartMovingOther bit 8 _isJumping
        isDiving          = ((bits >>  9) & 1) != 0;
        isDipping         = ((bits >> 10) & 1) != 0;
        isSwimming_sm     = ((bits >> 11) & 1) != 0;
        isCrawlClimbing   = ((bits >> 12) & 1) != 0;
        isCrawling        = ((bits >> 13) & 1) != 0;
        isClimbing        = ((bits >> 14) & 1) != 0;
        isSmall           = ((bits >> 15) & 1) != 0;
        doFallingAnimation = ((bits >> 16) & 1) != 0;  // 원본 bit 16 _doFallingAnimation
        isFlying          = ((bits >> 17) & 1) != 0;   // doFlyingAnimation bit
        isCeilingClimbing = ((bits >> 18) & 1) != 0;
        isLevitating      = ((bits >> 19) & 1) != 0;   // 원본 bit 19 isLevitating
        isHeadJumping     = ((bits >> 20) & 1) != 0;
        isSliding         = ((bits >> 21) & 1) != 0;
        angleJumpType     = (int) ((bits >> 22) & 0x7);
        isFeetVineClimbing   = ((bits >> 25) & 1) != 0;  // bit 25
        isHandsVineClimbing  = ((bits >> 26) & 1) != 0;  // bit 26
        isClimbJumping    = ((bits >> 27) & 1) != 0;
        isClimbBackJumping   = ((bits >> 28) & 1) != 0;  // bit 28 (onStartClimbBackJump 미이식)
        isSlow            = ((bits >> 29) & 1) != 0;
        isFast            = ((bits >> 30) & 1) != 0;
        isWallJumping     = ((bits >> 31) & 1) != 0;
        isRopeSliding     = ((bits >> 32) & 1) != 0;
    }

    // ── 4-2: tickEssential() ─────────────────────────────────────────

    /**
     * isActive 여부 무관하게 매 틱 실행되는 필수 처리.
     * 원본: SmartMovingPlayerBase.updateEntityActionState() → moving.tickEssential()
     */
    public void tickEssential(ClientPlayerEntity player) {
        // 이전 틱 값 초기화 — vanilla jump() 가로채기(sm_jump)에서 당 틱에 새로 설정됨
        jumpAvoided = false;

        // 원본 SmartMovingContext.interceptTick() L264: `Options.initializeForGameIfNeccessary()`.
        // 매 tick gameType 변경 폴링. 로컬 설정(Config == INSTANCE) 일 때만 (서버 설정 덮어쓰기 방지).
        // 리플렉션 `PlayerControllerMP.currentGameType` → `interactionManager.getCurrentGameMode().getId()`.
        MinecraftClient client = MinecraftClient.getInstance();
        if (SmartMovingConfig.Config == SmartMovingConfig.INSTANCE
                && client.interactionManager != null) {
            SmartMovingConfig.INSTANCE.initializeForGameIfNeccessary(
                    client.interactionManager.getCurrentGameMode().getId());
        }

        // 원본 Button.update() 대응 — 이번 틱 키 엣지 감지.
        // jumpButton.StartPressed / StopPressed / sneakButton.StartPressed / StopPressed 대응.
        // sm_travel_client 내 여러 핸들러에서 재참조 가능.
        var opts = MinecraftClient.getInstance().options;
        boolean curJumpPressed = opts.jumpKey.isPressed();
        jumpKeyStartPressed = curJumpPressed && !prevJumpKeyPressed;
        jumpKeyStopPressed  = !curJumpPressed && prevJumpKeyPressed;
        prevJumpKeyPressed = curJumpPressed;

        boolean curSneakPressed = opts.sneakKey.isPressed();
        sneakKeyStartPressed = curSneakPressed && !prevSneakKeyPressed;
        sneakKeyStopPressed  = !curSneakPressed && prevSneakKeyPressed;
        prevSneakKeyPressed = curSneakPressed;

        // 원본 SmartMovingSelf triggerWallJumping — 매 틱 시작에 리셋.
        // 리서치 파일에 원본 리셋 위치 기록 없음 → 보수적으로 "매 틱 1회용 이벤트" 로 처리.
        triggerWallJumping = false;

        // C-33: wasClimbing = 이전 틱의 isClimbing 값 저장
        wasClimbing = isClimbing;
        // 원본 간소 감소 `exhaustion -= 1.0F` 은 H-9 handleExhaustion 의
        // `exhaustion -= exhaustionLoss(=1F*factor)` 로 교체됨 (tickEssential 말미 호출).
        // 비활성 경로에서는 resetState() 가 exhaustion=0 리셋.

        // IMPL-04: 설정 토글 키 처리 (원본: toggleButton.update() + StartPressed 분기)
        // 싱글: toggle() 직접 호출 + 채팅 피드백 / 멀티: 서버에 변경 요청 패킷 전송
        if (SmartMovingKeys.configToggle.wasPressed()) {
            if (SmartMovingConfig.Config == SmartMovingConfig.INSTANCE) {
                SmartMovingConfig.INSTANCE.toggle();
                // 원본 SmartMovingOptions.writeClientConfigMessageToChat(false) (L560-L584) 1:1.
                //   Config.enabled=false                                            → "...disabled"
                //   name = _configKeyName.value (Map.getOrDefault 근사); isEmpty → null
                //   unnamed && keyCount==1                                          → "...enabled"
                //   name!=null (named)                                              → "...named" + name
                //   name==null (unnamed) + keyCount>1                               → "...unnamed" + currentKey
                SmartMovingConfig cfg = SmartMovingConfig.INSTANCE;
                Text msg;
                if (!cfg.enabled) {
                    msg = Text.translatable("smartmoving.message.config.client.disabled");
                } else {
                    String currentKey = cfg.getCurrentKey();
                    String name = cfg.configKeyName.getOrDefault(currentKey, "");
                    if (name.isEmpty()) name = null;
                    boolean unnamed = name == null;
                    if (unnamed) name = currentKey;
                    int keyCount = cfg.configKeys.length;
                    if (SmartMovingConfig.CONFIG_KEY_ENABLED.equals(name)
                            || (unnamed && keyCount == 1)) {
                        msg = Text.translatable("smartmoving.message.config.client.enabled");
                    } else if (unnamed) {
                        msg = Text.translatable("smartmoving.message.config.client.unnamed", name);
                    } else {
                        msg = Text.translatable("smartmoving.message.config.client.named", name);
                    }
                }
                if (player != null) player.sendMessage(msg);
            } else {
                ClientPlayNetworking.send(new SmartMovingNetwork.ConfigChangePayload());
            }
        }

        // IMPL-05: 속도 키 처리 (원본: speedIncreaseButton/speedDecreaseButton StartPressed → changeSpeed)
        // 서버에 SpeedChangePayload 전송 → 서버가 권한 확인 후 결과를 S2C로 돌려줌
        if (SmartMovingKeys.speedIncrease.wasPressed()) {
            if (ClientPlayNetworking.canSend(SmartMovingNetwork.SpeedChangePayload.ID)) {
                ClientPlayNetworking.send(new SmartMovingNetwork.SpeedChangePayload(1, null));
            }
        }
        if (SmartMovingKeys.speedDecrease.wasPressed()) {
            if (ClientPlayNetworking.canSend(SmartMovingNetwork.SpeedChangePayload.ID)) {
                ClientPlayNetworking.send(new SmartMovingNetwork.SpeedChangePayload(-1, null));
            }
        }

        // SM 비활성 시 이동 상태 전체 초기화 (원본: !isActive() → resetState())
        // isActive = !Compat.isBlockedByIncompatibility(sp) && Config.enabled
        // 1.7.10 Compat: StarMiner/Ships → N/A, EtFuturum elytra → isFallFlying(), 스펙테이터 → isSpectator()
        if (!SmartMovingConfig.Config.enabled || player.isSpectator() || player.isFallFlying()) {
            resetState();
        } else {
            // 원본 R-09 토글 블록 이전 값 저장 (willStartSneak/willStartCrawl 엣지 계산용).
            // 원본 L2717 `wasSneaking = isSlow`, 원본 willStartCrawl `isCrawling && !wasCrawling`.
            wasSneaking = isSlow;
            wasCrawling_st = isCrawling;
            wasClimbCrawling = isClimbCrawling;

            // ── mustCrawl / inputContinueCrawl / contextContinueCrawl 해제 / wantCrawl pre-compute ─
            // 원본 L1792-L1794 (mustCrawl), L2407-L2418 (inputContinueCrawl + contextContinueCrawl 해제),
            // L2419-L2432 (wouldWantCrawl + wantCrawl) 1:1 이식.
            //
            // `player.isSneaking()` 은 sm_isSneaking override 가 isSlow 를 참조하여 순환 가능 →
            // 원본처럼 raw sneakKey + sneakToggled 사용(원본 L1801).
            boolean sneakPressedRaw = net.minecraft.client.MinecraftClient.getInstance().options.sneakKey.isPressed();
            SmartMovingConfig cfg0 = SmartMovingConfig.Config;
            boolean grabJustPressed0 = SmartMovingKeys.grab.wasPressed();
            boolean grabHeld0 = SmartMovingKeys.grab.isPressed();

            // mustCrawl (원본 L1792-L1794). 1.21.1 은 AABB 기반 canStandUp 으로 근사.
            // 원본 L2404: `if (flying && (flyingEnabled || levitateSmallEnabled)) mustCrawl = false;` 도 반영.
            if (cfg0.crawl && cfg0.enabled) {
                mustCrawl = !canStandUp(player)
                        && !isSwimming_sm && !isDiving
                        && (!isDipping || dippingDepth < 0.65F);
                if (isFlying && cfg0.fly) mustCrawl = false;
            } else {
                mustCrawl = false;
            }

            // inputContinueCrawl (원본 L2407 1:1):
            //   isCrawlToggleEnabled ? crawlToggled : sneakPressed || (!freeClimbEnabled && grabPressed)
            boolean isCrawlToggleEnabled0 = cfg0.crawlToggle && cfg0.enabled;
            boolean freeClimbingEnabled0  = cfg0.freeClimb  && cfg0.enabled;
            boolean inputContinueCrawl = isCrawlToggleEnabled0
                    ? crawlToggled
                    : (sneakPressedRaw || (!freeClimbingEnabled0 && grabHeld0));

            // contextContinueCrawl 해제 (원본 L2408-L2418).
            //   if (inputContinueCrawl || isInWater() || mustCrawl) → false
            //   crawlStandUpLiquidCeiling 조건은 물속 천장 감지 — 1.21.1 근사: isDipping 으로 축소.
            if (contextContinueCrawl) {
                if (inputContinueCrawl || player.isTouchingWater() || mustCrawl) {
                    contextContinueCrawl = false;
                } else if (isCrawling && !isDipping) {
                    // 원본: 포복 위 천장까지 액체 여유가 충분하면 해제. 1.21.1: 물 밖이면 해제 근사.
                    contextContinueCrawl = false;
                }
            }

            // wouldWantCrawl (원본 L2419-L2430): isCrawling 유지 경로 + 신규 진입 경로.
            // wantCrawl = isCrawlingEnabled && wouldWantCrawl (원본 L2431-L2432).
            boolean crawlingEnabled_  = cfg0.crawl && cfg0.enabled;
            boolean wouldWantCrawl_ =
                    !player.getAbilities().flying &&
                    (
                        (isCrawling && (inputContinueCrawl || contextContinueCrawl))
                        ||
                        (grabJustPressed0 && (sneakToggled || sneakPressedRaw) && player.isOnGround())
                    );
            // 원본 진입 경로 추가 가드(!flying/!swim/!dive/!dipping/!climbing/!crawlClimbing/!ceilingClimbing/
            //   !sliding/!headJumping): 1.21.1 에서는 canCrawl(원본 L1805)과 중복. 여기선 상태 전환을
            //   IMPL-01 에 맡기고 pre-compute 는 원본 wouldWantCrawl 그대로 유지.
            wantCrawl = crawlingEnabled_ && wouldWantCrawl_;

            // 원본 L2446-L2447: !isCrawling 시 contextContinueCrawl=false.
            if (!isCrawling) contextContinueCrawl = false;

            // C-15: isSlow / isFast / isFlying 매 틱 계산
            // 원본 L2576-L2586 sneakContinueInput + wouldWantSneak + L2711-2719 wouldIsSneaking/isSlow 1:1.
            //   sneakContinueInput = isSneakToggleEnabled ? (sneakToggled || sneakStartPressed) : sneakPressed
            //   wouldWantSneak = !flying && !sliding && !headJumping
            //                    && !(diving && diveDownOnSneak)
            //                    && !(swimming && swimDownOnSneak && !isFakeShallowWaterSneaking)
            //                    && sneakContinueInput
            //                    && !wantCrawl && !mustCrawl
            //                    && (!isCrawlingEnabled || !grabPressed)
            //   wouldIsSneaking = wouldWantSneak && !wantSprint && !isClimbing
            //   isSlow = wantSneak && wouldIsSneaking (wantSneak 은 sneakContinueInput 로 매핑)
            boolean sneakContinueInput = cfg0.sneakToggle
                    ? (sneakToggled || sneakKeyStartPressed)
                    : sneakPressedRaw;
            boolean grabPressed0 = SmartMovingKeys.grab.isPressed();
            boolean crawlingEnabled0 = cfg0.crawl && cfg0.enabled;
            boolean wouldWantSneak =
                    !isFlying
                    && !isSliding
                    && !isHeadJumping
                    && !(isDiving && cfg0.diveDownOnSneak)
                    && !(isSwimming_sm && cfg0.swimDownOnSneak && !isFakeShallowWaterSneaking)
                    && sneakContinueInput
                    && !wantCrawl
                    && !mustCrawl
                    && (!crawlingEnabled0 || !grabPressed0);
            wouldIsSneaking = wouldWantSneak && !player.isSprinting() && !isClimbing;
            isSlow = sneakContinueInput && wouldIsSneaking;
            // isFast 원본: grabButton.Pressed && isSprinting()
            isFast = SmartMovingKeys.grab.isPressed() && player.isSprinting();
            // isFlying 원본: sp.capabilities.isFlying
            isFlying = player.getAbilities().flying;
            // wasCapabilitiesIsFlying: beforeOnLivingUpdate에서 저장 (vanilla tickMovement 실행 전)
            wasCapabilitiesIsFlying = isFlying;
            // wasCollidedHorizontally: 이전 틱 물리 결과 (HEAD에서 캡처 → 원본 beforeOnUpdate)
            wasCollidedHorizontally = player.horizontalCollision;

            // IMPL-01: 크롤링 진입/유지/해제 — wantCrawl/mustCrawl 필드는 isSlow 계산 앞에서 이미 확정됨.
            SmartMovingConfig cfg = SmartMovingConfig.Config;
            if (cfg.crawl) {
                boolean grabJustPressed = SmartMovingKeys.grab.wasPressed();
                if (!isCrawling) {
                    // 원본 L1805-L1809 canCrawl = !swim && !dive && (!dipping || shallow)
                    //                             && !climbing && fallDistance < minimum.
                    // 1.21.1 근사: crawlClimbing/ceilingClimbing/sliding/headJumping/flying 도 제외.
                    boolean canCrawl = !isSwimming_sm && !isDiving
                            && (!isDipping || dippingDepth < 0.65F)
                            && !isClimbing && !isCrawlClimbing && !isCeilingClimbing
                            && !isSliding && !isHeadJumping && !isFlying;
                    if (canCrawl && (wantCrawl || mustCrawl)) {
                        isCrawling = true;
                        // 원본: Options.isCrawlToggleEnabled() 게이트 — _crawlToggle 기본값 false(홀드)
                        if (SmartMovingConfig.Config.crawlToggle) crawlToggled = true;
                        ignoreNextStopSneakButtonPressed = true;
                    }
                } else {
                    // wantCrawl/mustCrawl 은 위 pre-compute 블록에서 확정 — mustCrawl 은 canCrawl 게이트 포함.
                    if (mustCrawl) {
                        // 공간 부족 — 강제 유지
                    } else if (crawlToggled) {
                        if (grabJustPressed) {
                            isCrawling = false;
                            crawlToggled = false;
                        }
                    } else {
                        if (!player.isSneaking()) {
                            isCrawling = false;
                        }
                    }
                }
            }

            // IMPL-02: 슬라이딩 진입 (스프린트+스니크 직접 진입)
            // 원본: wantSlide = isSneaking && isSprinting && onGround && !isClimbing && !isHeadJumping
            if (!isSliding && cfg.slide && !isCrawling) {
                boolean wantSlide = player.isSneaking() && player.isSprinting()
                        && player.isOnGround() && !isClimbing && !isHeadJumping;
                if (wantSlide) {
                    isSliding = true;
                    isAerodynamic = false;  // 원본: 슬라이드 시작 시 isAerodynamic 리셋 (행 2560)
                }
            }

            // SlideToHeadJumping 전환 (원본: SmartMovingSelf 행 2546~2550)
            // 슬라이딩 중 낙하거리가 0.05F 초과 → 헤드점프 + 공기역학 모드 전환
            if (isSliding && player.fallDistance > 0.05F) {
                isSliding = false;
                isHeadJumping = true;
                isAerodynamic = true;
            }
            // isHeadJumping이 꺼지면 isAerodynamic도 리셋 (원본: 행 2533)
            if (!isHeadJumping) {
                isAerodynamic = false;
            }

            // IMPL-03: 더블클릭 방향 점프 카운터 갱신
            // 원본: updateEntityActionState() 내 방향키 StartPressed → count 갱신
            {
                MinecraftClient mc = MinecraftClient.getInstance();
                boolean pressLeft  = mc.options.leftKey.isPressed();
                boolean pressRight = mc.options.rightKey.isPressed();
                boolean pressBack  = mc.options.backKey.isPressed();

                boolean startLeft  = pressLeft  && !prevPressLeft;
                boolean startRight = pressRight && !prevPressRight;
                boolean startBack  = pressBack  && !prevPressBack;

                prevPressLeft  = pressLeft;
                prevPressRight = pressRight;
                prevPressBack  = pressBack;

                // 원본: if(StartPressed) { count==0→angleJumpDoubleClickTicks(), else→-1 } else if(count>0) count--
                // 원본 _angleJumpDoubleClickTicks: Positive("...").up(3F, 2F), (int)Math.ceil(value) 정수화.
                int angleTicks = (int) Math.ceil(cfg.angleJumpDoubleClickTicks);
                if (cfg.angleJumpSide) {
                    if (startLeft) {
                        if (leftJumpCount  == 0) leftJumpCount  = angleTicks; else leftJumpCount  = -1;
                    } else if (leftJumpCount  > 0) leftJumpCount--;

                    if (startRight) {
                        if (rightJumpCount == 0) rightJumpCount = angleTicks; else rightJumpCount = -1;
                    } else if (rightJumpCount > 0) rightJumpCount--;
                }
                if (cfg.angleJumpBack) {
                    if (startBack) {
                        if (backJumpCount  == 0) backJumpCount  = angleTicks; else backJumpCount  = -1;
                    } else if (backJumpCount  > 0) backJumpCount--;
                }

                // 대각선 우선순위: -1 중복 시 -2로 강등 (좌/우+후 동시 방지)
                if (rightJumpCount == -1 && backJumpCount  > 0) rightJumpCount = -2;
                if (leftJumpCount  == -1 && backJumpCount  > 0) leftJumpCount  = -2;
                if (backJumpCount  == -1 && (leftJumpCount > 0 || rightJumpCount > 0)) backJumpCount = -2;
                // -2 → -1 승격 (다른 방향이 해소되면)
                if (rightJumpCount == -2 && backJumpCount  <= 0) rightJumpCount = -1;
                if (leftJumpCount  == -2 && backJumpCount  <= 0) leftJumpCount  = -1;
                if (backJumpCount  == -2 && leftJumpCount  <= 0 && rightJumpCount <= 0) backJumpCount = -1;
            }

            // R-04: isSmall 원본: isCrawling || isSliding || isHeadJumping
            // isCrawling/isSliding이 확정된 후 계산해야 정확함
            isSmall = isCrawling || isSliding || isHeadJumping;

            // wouldIsSneaking 은 위 isSlow 계산 블록에서 이미 원본 L2711 공식으로 설정됨.

            // ── 원본 R-09 스닉/크롤 토글 블록 (SmartMovingSelf L2966-L3045) 1:1 이식 ────
            // isSlow/isCrawling/isClimbCrawling 이 이 시점에 확정되어 있어야 함 (위에서 계산됨).
            // wasSneaking/wasCrawling_st/wasClimbCrawling 는 else 블록 진입부에서 저장됨.
            {
                boolean isSneakToggleEnabled = cfg.sneakToggle && cfg.enabled;
                boolean isCrawlToggleEnabled = cfg.crawlToggle && cfg.enabled;

                boolean willStopCrawl = false;
                boolean willStopCrawlStartSneak = false;
                if (isSneakToggleEnabled || isCrawlToggleEnabled) {
                    if (isCrawling && jumpKeyStopPressed)
                        willStopCrawlStartSneak = true;
                    if (isCrawling && sneakKeyStopPressed && !ignoreNextStopSneakButtonPressed)
                        willStopCrawlStartSneak = true;
                    if (!isCrawling && !isCrawlClimbing && !isClimbCrawling)
                        willStopCrawl = true;
                    willStopCrawl |= willStopCrawlStartSneak;
                }

                // 원본 L2986: wantSneak/wantSprint 참조. 간소 매핑: wantSneak=sneakContinueInput, wantSprint=isSprinting.
                boolean wantSneak_ = cfg.sneakToggle
                        ? (sneakToggled || sneakKeyStartPressed)
                        : MinecraftClient.getInstance().options.sneakKey.isPressed();
                boolean wantSprint_ = player.isSprinting();

                boolean willStopSneak = false;
                if (isSneakToggleEnabled) {
                    if (isCrawling && !willStopCrawlStartSneak)
                        willStopSneak = true;
                    if (wantSneak_ && wantSprint_ && sneakKeyStartPressed && sneakToggled) {
                        willStopSneak = true;
                        ignoreNextStopSneakButtonPressed = true;
                    }
                    if (wasSneaking && sneakKeyStartPressed)
                        willStopSneak = true;
                    if (!isSwimming_sm && !isDiving && jumpKeyStopPressed)
                        willStopSneak = true;
                }

                boolean willStartSneak = false;
                if (isSneakToggleEnabled) {
                    if (willStopCrawlStartSneak && sneakKeyStopPressed)
                        willStartSneak = true;
                    if (isFast && sneakKeyStopPressed && !ignoreNextStopSneakButtonPressed)
                        willStartSneak = true;
                    if (isSlow && !wasSneaking)
                        willStartSneak = true;
                }

                boolean willStartCrawl = false;
                if (isCrawlToggleEnabled) {
                    if (isCrawling && !wasCrawling_st)
                        willStartCrawl = true;
                    if (isClimbCrawling && !wasClimbCrawling)
                        willStartCrawl = true;
                }

                if (isSneakToggleEnabled) {
                    if (willStartSneak) sneakToggled = true;
                    if (willStopSneak)  sneakToggled = false;
                }

                if (isCrawlToggleEnabled) {
                    if (willStartCrawl) {
                        crawlToggled = true;
                        ignoreNextStopSneakButtonPressed = MinecraftClient.getInstance().options.sneakKey.isPressed();
                    }
                    if (willStopCrawl) crawlToggled = false;
                }

                if (sneakKeyStopPressed) ignoreNextStopSneakButtonPressed = false;
            }

            // fadingPerspectiveFactor EMA 계산 (원본: SmartMovingSelf.tickEssential L1317-1336)
            // getLandMovementFactor() → 1.21.1: player.getMovementSpeed()
            float landMovementFactor = player.getMovementSpeed();
            float perspectiveFactor = landMovementFactor;
            if (player.isSprinting()) perspectiveFactor /= 1.3F;
            perspectiveFactor = 0.1f + ((perspectiveFactor - 0.1f) * cfg.perspectiveSpeedFactor);
            if (cfg.perspectiveSpeedFactorMax > 0F) {
                perspectiveFactor = net.minecraft.util.math.MathHelper.clamp(
                        perspectiveFactor,
                        0.1f - cfg.perspectiveSpeedFactorMax * 0.1f,
                        0.1f + cfg.perspectiveSpeedFactorMax * 0.1f);
            }
            if (player.isSprinting()) perspectiveFactor *= 1.3F;
            if (isFast || isSprintJump) {
                if (player.isSprinting()) perspectiveFactor /= 1.3F;
                perspectiveFactor *= cfg.perspectiveSprintFactor;
            }
            if (fadingPerspectiveFactor != -1F)
                fadingPerspectiveFactor += (perspectiveFactor - fadingPerspectiveFactor) * cfg.perspectiveFadeFactor;
            else
                fadingPerspectiveFactor = landMovementFactor;

            // H-10: 허기/소진 계산 — 원본 SmartMovingSelf.onLivingUpdate 말미에서 updateHunger 호출.
            // 모든 이동 상태(isSlow/isFast/isClimbing/etc) 결정 후 이 시점에서 실행.
            // cfg.enabled 블록 안 — 비활성 상태에선 호출 안 됨.
            handleExhaustion(player);
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
        isClimbHolding = false;
        isClimbCrawling = false;
        hasClimbCrawlGap = false;
        climbIntoCount = 0;
        isWallJumping = false;
        isCrawlClimbing = false;
        isCeilingClimbing = false;
        isRopeSliding = false;
        actualFeetClimbType = 0;
        actualHandsClimbType = 0;
        isFeetVineClimbing = false;
        isHandsVineClimbing = false;
        isClimbBackJumping = false;
        isDipping = false;
        isSwimming_sm = false;
        isDiving = false;
        isHeadJumping = false;
        isCrawling = false;
        crawlToggled = false;
        sneakToggled = false;
        isFakeShallowWaterSneaking = false;
        wantCrawl = false;
        mustCrawl = false;
        contextContinueCrawl = false;
        ignoreNextStopSneakButtonPressed = false;
        wasSneaking = false;
        wasCrawling_st = false;
        wasClimbCrawling = false;
        sneakKeyStartPressed = false;
        sneakKeyStopPressed = false;
        prevSneakKeyPressed = false;
        jumpKeyStopPressed = false;
        isSliding = false;
        leftJumpCount  = 0;
        rightJumpCount = 0;
        backJumpCount  = 0;
        jumpMotionX    = 0D;
        jumpMotionZ    = 0D;
        prevPressLeft  = false;
        prevPressRight = false;
        prevPressBack  = false;
        isSmall = false;
        angleJumpType = 0;
        wasClimbing  = false;
        exhaustion   = 0F;
        hungerIncrease     = 0F;   // 원본 SmartMovingSelf.resetState — hungerIncrease 누적 리셋
        lastHungerIncrease = 0F;   // 원본 SmartMovingSelf.resetState — 변화 감지 캐시 리셋
        fadingPerspectiveFactor = -1F;
        wouldIsSneaking = false;
        forceIsSneaking = null;
        wasCapabilitiesIsFlying = false;
        wasCollidedHorizontally = false;
        isAerodynamic = false;
        dippingDepth = -1F;
        multiPlayerInitialized  = 0;
    }

    private static boolean canStandUp(ClientPlayerEntity player) {
        Box standBox = player.getDimensions(EntityPose.STANDING)
                             .getBoxAt(player.getPos())
                             .contract(1.0E-7);
        return player.getWorld().isSpaceEmpty(player, standBox);
    }

    /**
     * 원본 `SmartMovingSelf.handleExhaustion` (L849-L916, updateHunger 섹션 L1184-L1310) 의
     * Easy 실행 경로만 이식한 축소판. 점프/클라이밍/천장/스프린트 피로 블록은 Easy 에서
     * 게이트 false 라 진입 안 함 — 포커스 #5 범위 외로 전부 제외.
     *
     * **원본 대응 라인** (focus_05 §5.4):
     * - L1184-L1191: 이동 거리 계산 + 로컬 상태(isStill/isRunning) 파생
     * - L1858: `isStanding = horizontalSpeedSquare < 0.0005`
     * - L862: `hungerGainFactor = Config.getFactor(true, ...)`
     * - L863: `hungerIncrease += alwaysHungerGain + movement*0.0001F*hungerGainFactor`
     * - L888: `exhaustionLossFactor = Config.getFactor(false, ...)`
     * - L889-L890: `exhaustionLoss = 1F * factor`, `exhaustion -= exhaustionLoss`
     * - L893: `hungerIncrease += exhaustionLossHungerFactor * exhaustionLoss`
     *
     * **제외된 원본 블록** (Easy 진입 안 함):
     * - L866-L876 클라이밍 피로 (`_climbExhaustion=false`)
     * - L877-L879 천장 피로 (`_ceilingClimbExhaustion=false`)
     * - L881-L883 스프린트 피로 (`_sprintExhaustion=false`)
     * - L897-L899 exhaustion==0 리셋 (maxExhaustionForAction 필드 N/A)
     * - L911-L915 허기 패킷 전송 (H-11 별도)
     *
     * **getFactor 파라미터 매핑** (원본 L862/L888 호출):
     * - isSneaking 자리 → `isSlow` (SM 의 sneak+!sprint+!climbing)
     * - isRunning  자리 → 로컬 `isRunning` (`isSprinting() && !isFast && onGround`)
     * - isSprinting 자리 → `isFast` (SM 의 grab+sprint)
     * - isSwimming 자리 → `isSwimming_sm`
     *
     * @param player 현재 틱의 클라이언트 플레이어
     */
    public void handleExhaustion(ClientPlayerEntity player) {
        // 원본 L1184-L1188 이동 거리 (sp.lastTickPosX 등 → vanilla prevX/Y/Z)
        double diffX = player.getX() - player.prevX;
        double diffY = player.getY() - player.prevY;
        double diffZ = player.getZ() - player.prevZ;
        float horizontalMovement = (float) Math.sqrt(diffX * diffX + diffZ * diffZ);
        float movement = (float) Math.sqrt(horizontalMovement * horizontalMovement + (float)(diffY * diffY));
        int relevantMovementFactor = Math.round(movement * 100F);

        // 원본 L1858 isStanding = horizontalSpeedSquare < 0.0005
        boolean onGround = player.isOnGround();
        double vX = player.getVelocity().x;
        double vZ = player.getVelocity().z;
        double horizontalSpeedSquare = vX * vX + vZ * vZ;
        boolean isStanding = horizontalSpeedSquare < 0.0005;
        // 원본 L1190 isVerticalStill = Math.abs(diffY) < 0.007
        boolean isVerticalStill = Math.abs(diffY) < 0.007;
        // 원본 L1191 isStill = isStanding && isVerticalStill
        boolean isStill = isStanding && isVerticalStill;
        // 원본 L3241 isRunning() = sp.isSprinting() && !isFast && (sp.onGround || vanilla())
        //   vanilla() = SM 비활성 상태. handleExhaustion 은 cfg.enabled 때만 호출되므로 false.
        boolean isRunning = player.isSprinting() && !isFast && onGround;

        SmartMovingConfig cfg = SmartMovingConfig.Config;

        // 원본 L862 hunger 배율
        float hungerGainFactor = cfg.getFactor(true, onGround, isStanding, isStill,
                isSlow, isRunning, isFast,
                isClimbing, isClimbCrawling, isCeilingClimbing,
                isDipping, isSwimming_sm, isDiving,
                isCrawling, isCrawlClimbing);
        // 원본 L863 상시 허기 + 이동량 허기
        hungerIncrease += cfg.alwaysHungerGain + relevantMovementFactor * 0.0001F * hungerGainFactor;

        // 원본 L888 exhaustion 배율
        float exhaustionLossFactor = cfg.getFactor(false, onGround, isStanding, isStill,
                isSlow, isRunning, isFast,
                isClimbing, isClimbCrawling, isCeilingClimbing,
                isDipping, isSwimming_sm, isDiving,
                isCrawling, isCrawlClimbing);
        // 원본 L889 exhaustionLoss = 1F * factor
        float exhaustionLoss = 1F * exhaustionLossFactor;
        // 원본 L890 exhaustion -= exhaustionLoss. 1.21.1 기존 패턴 유지 (Math.max 0 클램프).
        exhaustion = Math.max(0F, exhaustion - exhaustionLoss);

        // 원본 L893 허기-소진 연동
        hungerIncrease += cfg.exhaustionLossHungerFactor * exhaustionLoss;
    }

    /**
     * 원본 SmartMovingSelf L1363-L1391 `fromSwimmingOrDiving(wasShortInWater)` 1:1 이식.
     *
     * 수영/잠수 종료 후 육상 전환 시 머리 위 공간 부족 판정:
     *   - 고체 천장 < 플레이어 높이 → 작은 구멍 크롤링 (isCrawling=true, heightOffset=-1)
     *   - 액체 천장 < 플레이어 높이 → 물 아래 크롤링 (contextContinueCrawl=true 추가)
     *
     * 원본 get*PlayerSolidBetween / get*PlayerLiquidBetween 은 AABB 범위 내
     * 고체/액체 Y 경계 계산 — 1.21.1 은 canStandUp(고체) / hasLiquidAbove(액체) 로 근사.
     *
     * 호출: sm_travel_client 내 handleSwimming 반환 false 분기 (원본 handleLand L648).
     */
    public void fromSwimmingOrDiving(ClientPlayerEntity player, boolean wasShortInWater) {
        boolean isShortInWater = isSwimming_sm || isDiving;
        if (wasShortInWater && !isShortInWater && !player.isSleeping()) {
            if (!canStandUp(player)) {
                // 고체 천장 — 작은 구멍 크롤링
                isCrawling = true;
                isDipping = false;
                heightOffset = -1F;
            } else if (hasLiquidCeiling(player)) {
                // 액체 천장 — 물 아래 크롤링 (원본 L1385-L1389)
                isCrawling = true;
                contextContinueCrawl = true;
                isDipping = false;
                heightOffset = -1F;
            }
        }
    }

    /**
     * bounding box 위쪽 ~1.1m 범위에 액체 블록이 있는지 검사.
     * 원본 getMinPlayerLiquidBetween(bb.maxY, bb.maxY + 1.1) 근사.
     */
    private static boolean hasLiquidCeiling(ClientPlayerEntity player) {
        Box bb = player.getBoundingBox();
        int minX = (int) Math.floor(bb.minX);
        int maxX = (int) Math.floor(bb.maxX);
        int minY = (int) Math.floor(bb.maxY);
        int maxY = (int) Math.floor(bb.maxY + 1.1D);
        int minZ = (int) Math.floor(bb.minZ);
        int maxZ = (int) Math.floor(bb.maxZ);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!player.getWorld().getFluidState(new net.minecraft.util.math.BlockPos(x, y, z)).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ── R-01: sendStatePacket() ───────────────────────────────────────

    /**
     * 현재 SmartMovingClientState를 34비트 long으로 인코딩해 서버로 전송한다.
     * 상태가 이전 틱과 달라진 경우에만 전송 (lastSentBits 비교).
     * 원본: SmartMovingPlayerBase.updateEntityActionState() 끝부분 writeEntityState().
     */
    public void sendStatePacket(ClientPlayerEntity player) {
        if (!ClientPlayNetworking.canSend(SmartMovingNetwork.StatePayload.ID)) return;

        // 로컬 플레이어 인스턴스 필드 갱신 — sendStatePacket 호출 시점에 즉석 계산된 값을
        // 자신의 상태 필드에도 반영하여 로컬 렌더/애니메이션(isDive 분기 등)에서 참조 가능하게 한다.
        isJumping          = !player.isOnGround() && !isClimbing && !isSwimming_sm && !isDiving && !isDipping;
        doFallingAnimation = !player.isOnGround() && player.getVelocity().y < -0.1D
                              && !isClimbing && !isSwimming_sm && !isDiving;
        isLevitating       = false; // 로프 미구현

        SmartMovingState s = new SmartMovingState();
        s.actualFeetClimbType  = actualFeetClimbType;
        s.actualHandsClimbType = actualHandsClimbType;
        s.isJumping            = isJumping;
        s.isDiving             = isDiving;
        s.isDipping            = isDipping;
        s.isSwimming           = isSwimming_sm;
        s.isCrawlClimbing      = isCrawlClimbing;
        s.isCrawling           = isCrawling;
        s.isClimbing           = isClimbing;
        s.isSmall              = isSmall;
        s.doFallingAnimation   = doFallingAnimation;
        s.doFlyingAnimation    = isFlying;
        s.isCeilingClimbing    = isCeilingClimbing;
        s.isLevitating         = isLevitating;
        s.isHeadJumping        = isHeadJumping;
        s.isSliding            = isSliding;
        s.angleJumpType        = angleJumpType;
        s.isFeetVineClimbing   = isFeetVineClimbing;
        s.isHandsVineClimbing  = isHandsVineClimbing;
        s.isClimbJumping       = isClimbJumping;
        s.isClimbBackJumping   = isClimbBackJumping;
        s.isSlow               = isSlow;
        s.isFast               = isFast;
        s.isWallJumping        = isWallJumping;
        s.isRopeSliding        = isRopeSliding;
        s.isSneakButtonPressed = player.isSneaking();

        long bits = SmartMovingState.encode(s);
        if (bits != lastSentBits) {
            ClientPlayNetworking.send(new SmartMovingNetwork.StatePayload(player.getId(), bits));
            lastSentBits = bits;
        }
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
