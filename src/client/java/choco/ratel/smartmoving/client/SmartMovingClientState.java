package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.climbing.FeetClimbing;
import choco.ratel.smartmoving.climbing.HandsClimbing;
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
import net.minecraft.entity.MovementType;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

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

    /**
     * 원본 SmartMovingSelf L2524 `wasHeadJumping = isHeadJumping` — isHeadJumping 매 틱
     * 재평가 5-AND 공식 직전 저장. wasHeadJumping && !isHeadJumping && onGround 해제
     * 엣지 후처리(handleCrash + restoreFromFlying) 및 toSlidingOrCrawling 조건에 사용.
     * B-23 / B-24 / B-29 의존.
     */
    public boolean wasHeadJumping;

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
     * 원본 SmartMovingSelf L1419 + L2734 갱신 공식:
     *   isStanding = horizontalSpeedSquare < 0.0005
     *   (horizontalSpeedSquare = motionX² + motionZ²)
     * getJumpSpeed / handleExhaustion Config.getFactor / jumpChargeCondition 등에서 참조.
     * B-30 공식 이식 의존.
     */
    public boolean isStanding;

    /**
     * 원본 SmartMovingSelf L3043 `wasRunning = isRunning` — R-09 블록 종료부 저장.
     * isSliding 직접 진입 조건 `(isGroundSprinting || (wasRunning && !isRunning && onGround))`
     * 및 tryJump 파라미터에 사용.
     * B-25 / B-26 / B-43 (세션 56) 이식 완료.
     */
    public boolean wasRunning;

    /**
     * 원본 SmartMovingSelf L3044 `wasLevitating = isLevitating` — R-09 블록 종료부 저장.
     * 이전 틱 isLevitating 스냅샷. 사용처: isGroundSprinting 전환 후처리(L2699) 등.
     * isLevitating 공식 자체는 B-10d / B-9 범위 (현재 항상 false) — wasLevitating 도
     * 결과적으로 false 유지. 저장 라인 1:1 이식만 먼저 수행. B-43 (세션 56).
     */
    public boolean wasLevitating;

    /**
     * 원본 SmartMovingSelf `wasFlying` — 이전 틱 isFlying 저장 (원본 L2511 엣지 판정용).
     * B-51 (세션 133). isFlying 전환 엣지 (`isFlying && !wasFlying` / `!isFlying && wasFlying`)
     * 판정으로 setHeightOffset(-1) / restoreFromFlying=true 설정.
     */
    public boolean wasFlying;

    /**
     * 비행 중 여부 (vanilla flight 또는 SM fly).
     * 원본: flying = sp.capabilities.isFlying
     * C-15: tickEssential()에서 매 틱 계산.
     */
    public boolean isFlying;

    /**
     * 원본 SmartMovingSelf L1415 `public boolean wantSprint;` — **public 필드**.
     * 매 틱 tickEssential L2595-L2615 에서 계산 (6조건 OR).
     * 사용처:
     *   - L2647 `if(wantSprint && !wantSneak)` — preferSprint 판정
     *   - L2713 `wouldIsSneaking = wouldWantSneak && !wantSprint && !isClimbing`
     *   - L2990 R-09 `wantSneak && wantSprint && sneakStartPressed && sneakToggled`
     *   - handleLand / handleJumping 등
     * B-3a (세션 49).
     */
    public boolean wantSprint;

    /**
     * 원본 SmartMovingSelf L1439 `public boolean isGroundSprinting;` — **public 필드**.
     * 매 틱 tickEssential L2679 에서 계산:
     *   `canHorizontallySprint && (onGround || isLevitating()) && !isSwimming && !isDiving && !isClimbing`
     * 사용처: isFast (L2689) / isHeadJumpCharging (L1883 Jumper) / isSlidingEnable (L2553)
     * 및 B-1d/B-1f 의존.
     * B-1c3 (세션 51).
     */
    public boolean isGroundSprinting;

    /**
     * 원본 SmartMovingSelf `collidedHorizontallyTickCount` (필드 선언 위치 리서치 미확인).
     * 수평 충돌 연속 틱 카운터. 매 틱 `horizontalCollision ? ++count : 0`.
     * 사용처: canHorizontallySprint (원본 L2675) `canAnySprint && collidedHorizontallyTickCount < 3`.
     * B-1c2 (세션 51).
     */
    public int collidedHorizontallyTickCount;

    /**
     * 원본 SmartMovingSelf `restoreFromFlying` — standupIfPossible 트리거 플래그.
     * 설정 위치: L2200 (standupIfPossible 내부), L2539 (isHeadJumping 해제 엣지),
     *           L2543 (flying 착지 전 tryLanding).
     * 사용처: L2543 `if(restoreFromFlying || tryLanding) standupIfPossible(...)`.
     * 1.21.1 standupIfPossible 미이식 — 필드 설정만 이식, 소비는 별도 B-N.
     * B-24 (세션 53).
     */
    public boolean restoreFromFlying;

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

    /**
     * 원본 SmartMovingSelf L2721-L2732 `wantClimbHolding` 3-OR. B-16 세션 68 에서 지역 변수로
     * 이식 → B-18-pre 세션 81: B-18 본체 `canClimbCrawling = wantClimbHolding && wantClimbUp`
     * 에서 참조 필요 → public 필드 승격.
     */
    public boolean wantClimbHolding;

    /**
     * 원본 SmartMovingSelf L2479 `wantClimb = Config.isFreeClimbingEnabled() && wouldWantClimb`.
     * 등반 의도 (지역 변수에서 필드 승격). B-16c 세션 69 에서 `wouldWantClimb` 4-OR 완전 이식.
     * B-17b2-pre 세션 72: 지역 `wantClimb16` → public 필드 승격 (B-17b2 `else if (wasCrawlClimbing)`
     * 블록에서 참조 필요). 사용처: wantClimbUp/wantClimbDown 의존.
     */
    public boolean wantClimb;

    /**
     * 원본 SmartMovingSelf L2467-L2477 `wouldWantClimb` 4-OR + 4-AND 억제 조건.
     * B-16c (세션 69) 4-OR 완전 이식됨 (지역 변수 `wouldWantClimb16`). B-36-pre (세션 77):
     * 필드 승격 — B-36 grab.StartPressed 3분기 (원본 L2838-L2861) 의 분기 (a) 에서 참조.
     * 사용처: wantClimb 전구체 + B-36 walking 전환 분기 + 원본 wouldWantClimb 소비 전반.
     */
    public boolean wouldWantClimb;

    /**
     * 원본 SmartMovingSelf L2419-L2430 `wouldWantCrawl` = 4-OR 조건 (isCrawling 유지 또는
     * grab.StartPressed + sneak). 1.21.1 L855-L861 지역 변수 `wouldWantCrawl_` 이식 완료.
     * B-36-pre (세션 77): 필드 승격 — B-36 grab.StartPressed 3분기 의 분기 (b)/(c) 에서 참조.
     * 사용처: wantCrawl 전구체 + B-36 수영/다이빙 전환 분기 + 얕은 물 크롤 분기.
     */
    public boolean wouldWantCrawl;

    /**
     * 원본 SmartMovingSelf L2491-L2495 `wantClimbUp`:
     *   (wantClimb && moveForward > 0F)
     *   || (isVineAnyClimbing && jumpButton.Pressed
     *       && !(sneakButton.Pressed && isFacedToSolidVine))
     *      && (!isCrawling || isCollidedHorizontally)
     *      && (!isSliding  || isCollidedHorizontally)
     * 연산자 우선순위 주의 (&& > ||): 전진 등반 OR (덩굴+점프 + 크롤/슬라이딩 충돌 조건).
     * B-17b2-pre 세션 72. Climber 지역 변수 L374 은 동일 로직 축약 — 필드 값 사용 전환 예정.
     */
    public boolean wantClimbUp;

    /**
     * 원본 SmartMovingSelf L2497-L2500 `wantClimbDown = wantClimb && moveForward <= 0F && !wantCrawl`.
     * B-17b2-pre 세션 72. Climber 지역 변수 L375 은 단순 버전 — 필드 값 사용 전환 예정.
     */
    public boolean wantClimbDown;

    /**
     * 원본 SmartMovingSelf L132 `isLiquidClimbing` 지역 변수 필드 승격.
     *   isLiquidClimbing = Config.isFreeClimbingEnabled() && sp.fallDistance <= 3.0
     *                   && wantClimbUp && sp.isCollidedHorizontally && !isDiving;
     *
     * 원본은 `updateEntityActionState` 내부에서 계산되어 `handleSwimming` /
     * `handleLava` 에 파라미터로 전달. 1.21.1 은 updateSwimState + handleSwimming
     * 두 정적 메서드로 분리되어 지역 변수 공유 불가 → **public 필드로 승격**.
     *
     * 소비처:
     *   - `Swimmer.updateSwimState` 진입 조건 (원본 L232 `!isFlying && !isLiquidClimbing
     *     && (...)` 중 `!isLiquidClimbing` 항 — B-7c 에서 복원).
     *   - `handleLava` 진입 조건 (원본 L580, B-7 범위 외).
     *
     * 갱신 시점: MixinLivingEntityClient.sm_beforeTravel 의 updateSwimState 호출 직전.
     * B-7a (세션 127).
     */
    public boolean isLiquidClimbing;

    /** 클라이밍 중 크롤 공간 전환 상태 (손이 낮은 천장 아래로 들어갈 때). */
    public boolean isClimbCrawling;

    /** 머리 위에 크롤 갭이 있는지 여부. setShouldClimbSpeed 속도 상한 판정용. */
    public boolean hasClimbCrawlGap;

    /** 크롤 갭 진입 카운터. > 0이면 setShouldClimbSpeed에서 HoldMotion 강제. */
    public int climbIntoCount;

    // ── B Phase 1 등반 9 필드 (세션 40) — 원본 SmartMovingSelf.java L1421-L1446 ──────

    /**
     * 원본 SmartMovingSelf L1421 `public boolean isVineOnlyClimbing;`.
     * 덩굴만 잡은 상태 (사다리 없이). handleClimbing Free 분기에서 갱신.
     * 애니메이션 파라미터. B-15d.
     */
    public boolean isVineOnlyClimbing;

    /**
     * 원본 SmartMovingSelf L1422 `public boolean isVineAnyClimbing;`.
     * 덩굴 또는 사다리 클라이밍 중. wantClimbUp (L1827) 조건에 사용.
     * B-15d.
     */
    public boolean isVineAnyClimbing;

    /**
     * 원본 SmartMovingSelf L1424 `public boolean isClimbingStill;`.
     * 클라이밍 정지 상태. 애니메이션 파라미터.
     * B-15e.
     */
    public boolean isClimbingStill;

    /**
     * 원본 SmartMovingSelf L1426 `public boolean isNeighborClimbing;`.
     * 인접 블록 등반 가능 여부. handleClimbing Free 분기에서 갱신.
     * **isCrawlClimbing 메인 공식 (원본 L2737)** 의 5-AND 중 하나로 필수.
     * B-15a / B-17 의존.
     */
    public boolean isNeighborClimbing;

    /**
     * 원본 SmartMovingSelf L1427 `public boolean hasClimbGap;`.
     * 등반 갭 (중간 빈 공간) 존재 여부.
     * **isClimbCrawling 메인 공식 (원본 L2787)** 의 needClimbCrawling = hasClimbCrawlGap
     * || (hasClimbGap && isClimbHolding) 에 필수.
     * B-15b / B-18 의존.
     */
    public boolean hasClimbGap;

    /**
     * 원본 SmartMovingSelf L1429 `public boolean hasNeighborClimbGap;`.
     * 인접 블록 등반 갭. handleClimbing Free 분기 판정용.
     * B-15c.
     */
    public boolean hasNeighborClimbGap;

    /**
     * 원본 SmartMovingSelf L1430 `public boolean hasNeighborClimbCrawlGap;`.
     * 인접 블록 등반 크롤 갭.
     * B-15c.
     */
    public boolean hasNeighborClimbCrawlGap;

    /**
     * 원본 SmartMovingSelf L1443 `public Block handsEdgeBlock;`.
     * 손이 잡은 edge 블록. 원본 Block + L1444 meta 는 1.21.1 BlockState 에 흡수
     * (vanilla API 표면 매핑 — metadata 정보 BlockState 가 내포).
     * 애니메이션 파라미터 (hands edge 렌더).
     * B-15f.
     */
    public net.minecraft.block.BlockState handsEdgeBlock;

    /**
     * 원본 SmartMovingSelf L1445 `public Block feetEdgeBlock;`.
     * 발이 잡은 edge 블록. L1446 meta 는 BlockState 에 흡수.
     * B-15f.
     */
    public net.minecraft.block.BlockState feetEdgeBlock;

    // ─────────────────────────────────────────────────────────────────

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
     * 원본 L505 갱신 공식: `isLevitating = diving && !diveUp && !diveDown &&
     *   moveStrafing==0 && moveForward==0` (수중 정적 자세).
     * 로프 블록 용도는 1.21.1 미구현이나 주요 용도는 수중 정적 조건이라 유효.
     * 애니메이션(isDive)에서 Quarter-Sixteenth 수직각 적용 조건.
     * B-10d (세션 71): updateSwimState 에서 매 틱 갱신.
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
     * 원본 SmartMovingSelf L1436 / L507 갱신:
     *   isShallowDiveOrSwim = couldStandUp && (isDiving || isSwimming)
     * handleSwimming 얕은 물 특수 분기 (L513-L536) 및 grab.StartPressed 수영/걷기 전환
     * (L2839) 에 사용. B-10a / B-11 / B-36 의존.
     */
    public boolean isShallowDiveOrSwim;

    /**
     * 원본 SmartMovingSelf L1435 / L487 갱신:
     *   isJumpingOutOfWater = wantJumpOutOfWater && (waterMovementTicks > 10 || onGround
     *                                                || wasJumpingOutOfWater)
     * 수면 탈출 점프 진행 조건. handleSwimming L500 `motionY = 0.30000001192092896D` 설정에 사용.
     * B-10b / B-12 의존.
     */
    public boolean isJumpingOutOfWater;

    /**
     * 원본 SmartMovingSelf L105 `boolean wasJumpingOutOfWater = isJumpingOutOfWater`
     * updateEntityActionState 내부 **지역 snapshot**. 이후 handleSwimming L229 파라미터로
     * 전달되어 L487 `isJumpingOutOfWater = wantJumpOutOfWater && (... || wasJumpingOutOfWater)`
     * 공식에 사용 (틱 간 hysteresis).
     *
     * **§7 근사** (B-10b-pre): 1.21.1 은 Swimmer.updateSwimState + handleSwimming 으로 분리
     * → 지역 snapshot 공유 불가 → 이전 틱 값 저장용 **public 필드로 승격**. 저장은
     * `Swimmer.updateSwimState` 진입 첫 줄 (원본 L105 대응 위치). B-10b-post 공식 이식
     * (세션 미정) 에서 이 필드 참조.
     */
    public boolean wasJumpingOutOfWater;

    /**
     * 원본 SmartMovingSelf L1438 / L550 갱신:
     *   useStandard 경로에서 isStillSwimmingJump = false
     * grab.StartPressed 수영 전환 (L2844) 에서 true 설정 — 수영 점프 hold 상태.
     * B-10c / B-36 의존.
     */
    public boolean isStillSwimmingJump;

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
     * 원본 SmartMovingSelf `wantCrawlNotClimb` (L2452-L2461 갱신):
     *   wantCrawlNotClimb = wantCrawlNotClimb || (grab.StartPressed && !wasCrawling &&
     *                       onGround && <추가 조건>) ...
     * wouldWantClimb 조건(L1822) `!wantCrawlNotClimb` 및 wantClimbCeiling (L1837) 에 사용.
     * B-31b / B-41 의존.
     */
    public boolean wantCrawlNotClimb;

    /**
     * 원본 SmartMovingSelf `initializeCrawling` (L2399 / L2822 / L2831 / L2834).
     * 크롤링 초기화 진입 시 setHeightOffset(-1F) + move(0,-1D,0) 처리에 분기 조건으로 사용.
     * mustCrawl 계산 시 crawlStandUpBottom 계산의 `initializeCrawling ? 0D : 1D` 오프셋에도 사용.
     * B-31c / B-35 의존.
     */
    public boolean initializeCrawling;

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

    /**
     * 원본 SmartMovingSelf L3074 `public boolean wasCrawling;` — 다용도 필드.
     * 주요 역할:
     *   (1) L2441 tickEssential `wasCrawling = isCrawling` — isCrawling 공식 직전 저장
     *   (2) L3015 R-09 `isCrawling && !wasCrawling` — willStartCrawl 판정
     *   (3) L2449 `wasCrawling && !isCrawling && capabilities.flying` — tryJump(Up) 트리거 (B-34)
     *   (4) L2457 `!wasCrawling` — wantCrawlNotClimb 계산 (B-41)
     *   (5) L2566/L2572/L2751/L2760/L2767/L2812/L2835/L2860 — 여러 전환 블록 재설정
     * B-31a (세션 41): 기존 `wasCrawling_st` → `wasCrawling` 개명 (원본과 이름 일치).
     * 현재는 (1)+(2) 이식 상태 — 나머지는 해당 B-N 에서 추가 갱신 위치 등록.
     */
    public boolean wasCrawling;

    /** 원본 wasClimbCrawling — 이전 틱 isClimbCrawling 저장. */
    public boolean wasClimbCrawling;

    /** 원본 sneakButton.StartPressed — 이번 틱 스닉키 엣지(새로 눌림). */
    public boolean sneakKeyStartPressed;
    /** 원본 sneakButton.StopPressed — 이번 틱 스닉키 엣지(새로 뗌). */
    public boolean sneakKeyStopPressed;
    /** sneakKey.isPressed() 이전 틱 값 — 엣지 감지 내부 추적. */
    public boolean prevSneakKeyPressed;

    /**
     * 원본 sprintButton.StartPressed — 이번 틱 스프린트키 엣지(새로 눌림). B-48a (세션 131).
     * isGroundSprinting 전환 후처리 (원본 L2697-L2709) 에서 소비.
     */
    public boolean sprintKeyStartPressed;
    /**
     * 원본 sprintButton.StopPressed — 이번 틱 스프린트키 엣지(새로 뗌). B-48a (세션 131).
     * 원본 L2706 `if (_walkOnSprintRelease && sprintButton.StopPressed)` 조건용.
     */
    public boolean sprintKeyStopPressed;
    /** sprintKey.isPressed() 이전 틱 값 — 엣지 감지 내부 추적. B-48a (세션 131). */
    public boolean prevSprintKeyPressed;

    /**
     * 원본 wasGroundSprinting — 이전 틱 isGroundSprinting 저장 (원본 L2678).
     * B-48c (세션 131). isGroundSprinting 전환 후처리 (원본 L2697-L2702) 의
     * 엣지 판정 (`isGroundSprinting && !wasGroundSprinting` / `wasGroundSprinting &&
     * !isGroundSprinting`) 용.
     */
    public boolean wasGroundSprinting;

    /**
     * 원본 wasRunningWhenSprintStarted — sprint 시작 시점의 vanilla isSprinting() snapshot
     * (원본 L2699 저장 / L2704 복원). B-48b (세션 131). sprint 종료 엣지에서 복원됨.
     */
    public boolean wasRunningWhenSprintStarted;

    /** 원본 jumpButton.StopPressed — 이번 틱 점프키 엣지(새로 뗌). StartPressed는 jumpKeyStartPressed. */
    public boolean jumpKeyStopPressed;

    /**
     * 원본 `grabButton.StartPressed` — 이번 틱 grab 키 엣지(새로 눌림). vanilla
     * `KeyBinding.wasPressed()` 는 "press 이벤트 카운터에서 1 소비" 시멘틱이라 같은 틱에
     * 2회째 호출 시 false 반환. B-46 (세션 66) — tickEssential 초반 1회 호출 → 이 필드
     * 저장 → 모든 소비 지점 (pre-compute / IMPL-01 / 기타) 이 필드 참조로 통일.
     * 원본 `grabButton.StartPressed` 는 틱 내 불변 불리언이므로 시멘틱 등가.
     */
    public boolean grabJustPressed;

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

    /**
     * 원본 SmartMovingSelf private 필드 `initialized` — 플레이어 첫 틱 실행 마커.
     * 매 틱 tickEssential 에서 체크 후 초기 상태 판정 (원본 L2344 `!initialized` 가드).
     * 한 번 true 되면 유지 (resetState 리셋만 false). B-31c-post (세션 113).
     */
    public boolean initialized;

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

        // H-15 (세션 22): 2상태 토글 강제 복원. 원본 `initializeForGameIfNeccessary` 가
        // setKeys({"e","m","h"}) 로 configKeys 를 덮어써 configToggle 이 4상태 순환하는
        // 문제 — 사용자 결정 "disabled↔enabled 2상태" 에 위배. tickEssential 의 호출
        // 제거. `configKeys = DEFAULT_KEYS = {null}` 초기값 유지되어 toggle() 이 2상태
        // (0 ↔ -1) 로만 순환. F 섹션 메서드는 코드상 유지 (후속 포커스
        // `focus_10_config_toggle_cleanup.md` 에서 잉여 코드 일괄 정리).
        //
        // 원본 호출 (보존만, 실행 안 함):
        //   MinecraftClient client = MinecraftClient.getInstance();
        //   if (SmartMovingConfig.Config == SmartMovingConfig.INSTANCE
        //           && client.interactionManager != null) {
        //       SmartMovingConfig.INSTANCE.initializeForGameIfNeccessary(
        //               client.interactionManager.getCurrentGameMode().getId());
        //   }

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

        // B-48a (세션 131): sprintKey 엣지 필드 — sneakKey 패턴 그대로 적용.
        // 원본 sprintButton.StartPressed / StopPressed 대응.
        boolean curSprintPressed = opts.sprintKey.isPressed();
        sprintKeyStartPressed = curSprintPressed && !prevSprintKeyPressed;
        sprintKeyStopPressed  = !curSprintPressed && prevSprintKeyPressed;
        prevSprintKeyPressed = curSprintPressed;

        // B-46 (세션 66): 원본 `grabButton.StartPressed` 이식.
        // vanilla `KeyBinding.wasPressed()` 는 카운터 소비성이라 같은 틱 2회째부터 false.
        // 여기서 1회만 호출 → `grabJustPressed` 필드에 저장 → 모든 소비 지점에서 필드 참조.
        grabJustPressed = SmartMovingKeys.grab.wasPressed();

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
                // H-18 (세션 23): 2갈래 단순화. 2상태 토글(disabled↔enabled) 기준이므로
                // 원본 4갈래(named/unnamed/enabled/disabled) 분기 불필요. configKeyName /
                // CONFIG_KEY_ENABLED 등 잉여 코드 전부 H-19 에서 제거 예정.
                SmartMovingConfig cfg = SmartMovingConfig.INSTANCE;
                Text msg = Text.translatable(cfg.enabled
                        ? "smartmoving.message.config.client.enabled"
                        : "smartmoving.message.config.client.disabled");
                if (player != null) player.sendMessage(msg);
            } else {
                ClientPlayNetworking.send(new SmartMovingNetwork.ConfigChangePayload());
            }
        }

        // IMPL-05: 속도 키 처리 (원본: speedIncreaseButton/speedDecreaseButton StartPressed → changeSpeed)
        // 서버에 SpeedChangePayload 전송 → 서버가 권한 확인 후 결과를 S2C로 돌려줌
        // B-6 (세션 26): Creative 전용 게이트. 원본 `isUserSpeedEnabled() = enabled &&
        // _speedUser.value` 에서 `_speedUser` 가 Creative 때만 true 이므로 Creative 아니면
        // 키 입력 자체가 차단됨. 원본 SmartMovingSelf L2326 `if(Config.isUserSpeedEnabled()
        // && !Config.isUserSpeedAlwaysDefault() && ...)` 와 등가.
        boolean userSpeedEnabled = SmartMovingConfig.Config.enabled
                && SmartMovingConfig.Config.speedUser
                && MinecraftClient.getInstance().interactionManager != null
                && MinecraftClient.getInstance().interactionManager.getCurrentGameMode()
                        == net.minecraft.world.GameMode.CREATIVE;
        if (userSpeedEnabled) {
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
        }

        // SM 비활성 시 이동 상태 전체 초기화 (원본: !isActive() → resetState())
        // isActive = !Compat.isBlockedByIncompatibility(sp) && Config.enabled
        // 1.7.10 Compat: StarMiner/Ships → N/A, EtFuturum elytra → isFallFlying(), 스펙테이터 → isSpectator()
        if (!SmartMovingConfig.Config.enabled || player.isSpectator() || player.isFallFlying()) {
            resetState();
        } else {
            // 원본 R-09 토글 블록 이전 값 저장 (willStartSneak/willStartCrawl 엣지 계산용).
            // B-44 (세션 43): wasSneaking 저장은 isSlow 공식 직전(L2716)으로 이동 — B-2 함께.
            // B-44c (세션 82): wasClimbCrawling 저장은 B-18 isClimbCrawling 공식 직전
            //   (원본 L2786 대응) 으로 이동 완료.
            // B-44b (세션 84): wasCrawling 저장도 B-33 isCrawling 매 틱 공식 직전 (원본 L2441)
            //   으로 이동 완료 — 여기 일괄 저장 전부 제거됨.

            // ── mustCrawl / inputContinueCrawl / contextContinueCrawl 해제 / wantCrawl pre-compute ─
            // 원본 L1792-L1794 (mustCrawl), L2407-L2418 (inputContinueCrawl + contextContinueCrawl 해제),
            // L2419-L2432 (wouldWantCrawl + wantCrawl) 1:1 이식.
            //
            // `player.isSneaking()` 은 sm_isSneaking override 가 isSlow 를 참조하여 순환 가능 →
            // 원본처럼 raw sneakKey + sneakToggled 사용(원본 L1801).
            boolean sneakPressedRaw = net.minecraft.client.MinecraftClient.getInstance().options.sneakKey.isPressed();
            SmartMovingConfig cfg0 = SmartMovingConfig.Config;
            // B-46 (세션 66): `SmartMovingKeys.grab.wasPressed()` 지역 호출 제거 —
            // tickEssential 초반 1회 저장된 `grabJustPressed` 필드 참조.
            boolean grabHeld0 = SmartMovingKeys.grab.isPressed();

            // B-31c-post (세션 113): 원본 L2343-L2356 initializeCrawling true 설정 블록 이식.
            //   boolean initializeCrawling = false;
            //   if (!initialized && !(remote && multiPlayerInitialized != 0) && !isRiding()) {
            //       if (getMaxPlayerSolidBetween(minY, maxY, 0) > minY) {
            //           initializeCrawling = true; toCrawling();
            //       }
            //       initialized = true;
            //   }
            //   if (multiPlayerInitialized > 0) multiPlayerInitialized--;
            //
            // 원본은 지역 변수 `initializeCrawling = false` 매 틱 초기화 (뒤에서 B-35 등이 참조).
            // 1.21.1 은 필드 승격 (B-31c 세션 38) → 매 틱 진입 시 리셋.
            //
            // **§7 근사 B-31c-post**: 원본 `getMaxPlayerSolidBetween(minY, maxY, 0) > minY`
            // (머리 위 고체 블록 존재) AABB 정밀 스캔 미이식 → `!canStandUp(player)` 근사.
            // B-42 Phase 6 완료 시 정밀 복원 경로.
            this.initializeCrawling = false;
            if (!this.initialized
                    && !(player.getWorld().isClient() && this.multiPlayerInitialized != 0)
                    && !player.hasVehicle()) {
                // 근사 이식 — 원본과 차이: getMaxPlayerSolidBetween AABB → canStandUp
                if (!canStandUp(player)) {
                    this.initializeCrawling = true;
                    this.toCrawling();
                }
                this.initialized = true;
            }

            if (this.multiPlayerInitialized > 0) {
                this.multiPlayerInitialized--;
            }

            // mustCrawl (원본 L1792-L1794). 1.21.1 은 AABB 기반 canStandUp 으로 근사.
            // **B-51 해소 (세션 133)**: 원본 L2404 `if (capabilities.isFlying && (isFlyingEnabled
            //   || isLevitateSmallEnabled)) mustCrawl = false;` 정밀 복원.
            //   기존: `isFlying && cfg0.fly` — SM 내부 isFlying 필드는 `cfg.fly && capabilities.flying
            //   && !isSwimming && !isDiving` 으로 이미 cfg.fly 내포. 원본은 `capabilities.flying`
            //   vanilla 기반 + OR levitateSmall. 정밀 복원.
            if (cfg0.crawl && cfg0.enabled) {
                mustCrawl = !canStandUp(player)
                        && !isSwimming_sm && !isDiving
                        && (!isDipping || dippingDepth < 0.65F);
                if (player.getAbilities().flying
                        && (cfg0.isFlyingEnabled() || cfg0.isLevitateSmallEnabled())) {
                    mustCrawl = false;
                }
            } else {
                mustCrawl = false;
            }

            // inputContinueCrawl (원본 L2407 1:1):
            //   isCrawlToggleEnabled ? crawlToggled : sneakPressed || (!freeClimbEnabled && grabPressed)
            // B-45b (세션 45): Config.isCrawlToggleEnabled() 헬퍼로 치환.
            boolean freeClimbingEnabled0  = cfg0.freeClimb  && cfg0.enabled;
            boolean inputContinueCrawl = cfg0.isCrawlToggleEnabled()
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
            // B-36-pre (세션 77): 지역 `wouldWantCrawl_` → `this.wouldWantCrawl` 필드 승격.
            // B-36 grab.StartPressed 3분기 (원본 L2838-L2861) 에서 필드 참조 예정.
            wouldWantCrawl =
                    !player.getAbilities().flying &&
                    (
                        (isCrawling && (inputContinueCrawl || contextContinueCrawl))
                        ||
                        (grabJustPressed && (sneakToggled || sneakPressedRaw) && player.isOnGround())
                    );
            boolean wouldWantCrawl_ = wouldWantCrawl;  // 하위 호환용 지역 별칭
            // 원본 진입 경로 추가 가드(!flying/!swim/!dive/!dipping/!climbing/!crawlClimbing/!ceilingClimbing/
            //   !sliding/!headJumping): 1.21.1 에서는 canCrawl(원본 L1805)과 중복. 여기선 상태 전환을
            //   IMPL-01 에 맡기고 pre-compute 는 원본 wouldWantCrawl 그대로 유지.
            wantCrawl = crawlingEnabled_ && wouldWantCrawl_;

            // 원본 L2446-L2447: !isCrawling 시 contextContinueCrawl=false.
            if (!isCrawling) contextContinueCrawl = false;

            // C-15: isSlow / isFast / isFlying 매 틱 계산
            // 원본 L2576-L2590 sneakContinueInput + wouldWantSneak + wantSneak + L2711-L2719
            // wouldIsSneaking / wasSneaking / isSlow 1:1.
            //   sneakContinueInput = isSneakToggleEnabled ? (sneakToggled || sneakStartPressed) : sneakPressed
            //   wouldWantSneak = !flying && !sliding && !headJumping
            //                    && !(diving && diveDownOnSneak)
            //                    && !(swimming && swimDownOnSneak && !isFakeShallowWaterSneaking)
            //                    && sneakContinueInput
            //                    && !wantCrawl && !mustCrawl
            //                    && (!isCrawlingEnabled || !grabPressed)
            //   wantSneak = isSneakingEnabled() && wouldWantSneak                 (원본 L2588-L2590)
            //   wouldIsSneaking = wouldWantSneak && !wantSprint && !isClimbing    (원본 L2712)
            //   wasSneaking = isSlow                                              (원본 L2716 공식 직전)
            //   isSlow = wantSneak && wouldIsSneaking                              (원본 L2718)
            // B-45b (세션 45): Config.isSneakToggleEnabled() 헬퍼로 치환 — cfg.enabled AND 가드 포함.
            boolean sneakContinueInput = cfg0.isSneakToggleEnabled()
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
            // B-2 (세션 43): wantSneak 신설. Config.isSneakingEnabled() = sneak || !enabled (OR 패턴).
            boolean wantSneak = cfg0.isSneakingEnabled() && wouldWantSneak;

            // B-3a (세션 49): wantSprint 6조건 OR 공식 (원본 L2595-L2615).
            // 지역 변수 (원본 L2373-L2375 + L2592-L2593 + 여러 Button):
            //   disabled = !Config.enabled || isRiding || isSleeping || startSleeping
            //     - 1.21.1: isRiding=hasVehicle(), isSleeping=isSleeping(),
            //       startSleeping 은 정확 대응 없어 getSleepTimer()>0 으로 근사 확장.
            //   moveForwardButtonPressed = esp.movementInput.moveForward > 0F
            //   moveButtonPressed = moveForward != 0 || moveStrafe != 0
            //   sprintButton.Pressed = vanilla sprintKey.isPressed()
            //   jumpButton.Pressed   = vanilla jumpKey.isPressed()
            //   sneakButton.Pressed  = 이미 sneakPressedRaw 변수로 존재
            boolean _disabled3a = !cfg0.enabled
                    || player.hasVehicle()
                    || player.isSleeping()
                    || player.getSleepTimer() > 0;
            net.minecraft.client.MinecraftClient _mc3a = net.minecraft.client.MinecraftClient.getInstance();
            boolean _sprintPressed3a = _mc3a.options.sprintKey.isPressed();
            boolean _jumpPressed3a   = _mc3a.options.jumpKey.isPressed();
            boolean _moveForwardPressed3a = player.input.movementForward > 0F;
            boolean _movePressed3a = player.input.movementForward != 0F
                                   || player.input.movementSideways != 0F;
            // 원본 L2595-L2615: 6-AND — Config.isSprintingEnabled() && !isSliding && sprintPressed
            //   && (지면 전진 || 등반 || (수영+입력) || (잠수+입력+점프) || (비행+입력+점프/스니크))
            //   && !disabled.
            wantSprint = cfg0.isSprintingEnabled()
                    && !isSliding
                    && _sprintPressed3a
                    && (
                            _moveForwardPressed3a
                            || isClimbing
                            || (isSwimming_sm
                                    && (_movePressed3a
                                            || (sneakPressedRaw && cfg0.swimDownOnSneak)))
                            || (isDiving
                                    && (_movePressed3a || _jumpPressed3a
                                            || (sneakPressedRaw && cfg0.diveDownOnSneak)))
                            || (isFlying
                                    && (_movePressed3a || _jumpPressed3a || sneakPressedRaw))
                    )
                    && !_disabled3a;

            // B-3b (세션 49): wouldIsSneaking 정정 — 원본 L2712 `!wantSprint` (SM 복합)
            //   기존 `!player.isSprinting()` (vanilla 단순) 제거. 이제 SM 컨텍스트 조건 전부 반영.
            wouldIsSneaking = wouldWantSneak && !wantSprint && !isClimbing;
            // B-44a (세션 43): wasSneaking 저장을 isSlow 공식 직전으로 이동 (원본 L2716).
            wasSneaking = isSlow;
            // B-2 (세션 43): 기존 `sneakContinueInput && wouldIsSneaking` 중복 제거 → 원본 1:1.
            isSlow = wantSneak && wouldIsSneaking;

            // B-16 (세션 68): 원본 L2721-L2732 wantClimbHolding/isClimbHolding 3-OR 갱신 공식 이식.
            // 원본 L2718 isSlow 직후 L2721 wantClimbHolding 순서 복원.
            // B-16c (세션 69): wouldWantClimb 2-OR → 4-OR 확장 (자동 ladder/vine 분기).
            // B-16a 세션 69 에서 isFacedToLadder / isFacedToSolidVine Climber 이식 완료.
            // B-16b 세션 69 에서 Config.freeClimbAutoLadder/Vine 필드 + 헬퍼 이식 완료.
            // ※ 근사 1건 남음 (§7 B-16 근사): `blocked` 는 원본 `currentScreen!=null &&
            //   !currentScreen.allowUserInput` → 1.21.1 `allowUserInput` 제거됨 →
            //   `currentScreen != null` 단일 조건 근사. 모든 열린 screen 을 입력 차단으로 간주.
            {
                net.minecraft.client.MinecraftClient mc16 =
                        net.minecraft.client.MinecraftClient.getInstance();
                boolean blocked = mc16.currentScreen != null;

                // 원본 L2467-L2477 `wouldWantClimb` 4-OR + 억제:
                //   (grab || (isClimbHolding && sneak)
                //    || (Config.isFreeClimbAutoLadderEnabled && isFacedToLadder(isClimbCrawling))
                //    || (Config.isFreeClimbAutoVineEnabled && isFacedToSolidVine(isClimbCrawling)))
                //   && (!isSliding || grab+forward) && !isHeadJumping && !wantCrawlNotClimb
                //   && !disabled
                // B-36-pre (세션 77): 지역 `wouldWantClimb16` → `this.wouldWantClimb` 필드 승격.
                wouldWantClimb =
                        (grabPressed0
                                || (isClimbHolding && sneakPressedRaw)
                                || (cfg0.isFreeClimbAutoLadderEnabled()
                                        && SmartMovingClimber.isFacedToLadder(player, isClimbCrawling))
                                || (cfg0.isFreeClimbAutoVineEnabled()
                                        && SmartMovingClimber.isFacedToSolidVine(player, isClimbCrawling)))
                        && (!isSliding || (grabPressed0 && player.input.movementForward > 0F))
                        && !isHeadJumping
                        && !wantCrawlNotClimb
                        && !_disabled3a;
                // B-17b2-pre (세션 72): `wantClimb16` 지역 → public 필드 승격.
                wantClimb = cfg0.freeClimb && cfg0.enabled && wouldWantClimb;

                // 원본 L2721-L2732 3-OR:
                // B-18-pre (세션 81): 지역 `wantClimbHolding` → `this.wantClimbHolding` 필드 승격.
                wantClimbHolding =
                        (isClimbHolding && sneakPressedRaw)
                        || (isClimbing && blocked)
                        || (wantClimb && !isSwimming_sm && !isDiving && !isCrawling
                                && (sneakPressedRaw || crawlToggled));
                isClimbHolding = wantClimbHolding && isClimbing;
            }

            // B-17b2-pre (세션 72): 원본 L2491-L2500 `wantClimbUp/wantClimbDown` 계산 이식.
            // 원본 L2491-L2495 wantClimbUp:
            //   (wantClimb && moveForward > 0F)
            //   || ((isVineAnyClimbing && jumpPressed && !(sneakPressed && isFacedToSolidVine))
            //       && (!isCrawling || isCollidedHorizontally)
            //       && (!isSliding  || isCollidedHorizontally))
            // 연산자 우선순위 (&& > ||): 전진 등반 OR (덩굴+점프 + 크롤/슬라이딩 충돌 조건).
            // 원본 L2497-L2500 wantClimbDown: `wantClimb && moveForward <= 0F && !wantCrawl`.
            // 의존: isVineAnyClimbing (B-15d), isFacedToSolidVine (B-16a), 기타 모두 이식 완료.
            {
                boolean jumpPressed17 = _jumpPressed3a;  // B-3a 에서 계산된 지역 변수
                boolean isFacedVine17 = SmartMovingClimber.isFacedToSolidVine(player, isClimbCrawling);
                float   forward17     = player.input.movementForward;
                boolean hCollision17  = player.horizontalCollision;

                wantClimbUp =
                        (wantClimb && forward17 > 0F)
                        || ((isVineAnyClimbing && jumpPressed17
                                && !(sneakPressedRaw && isFacedVine17))
                            && (!isCrawling || hCollision17)
                            && (!isSliding  || hCollision17));
                wantClimbDown = wantClimb && forward17 <= 0F && !wantCrawl;
            }

            // B-1c2 (세션 51): collidedHorizontallyTickCount 매 틱 갱신.
            //   수평 충돌 연속 틱 카운터. can* 판정 (원본 L2675) 에 사용.
            if (player.horizontalCollision) collidedHorizontallyTickCount++;
            else                            collidedHorizontallyTickCount = 0;

            // B-1c3/B-1d/B-1e/B-1f (세션 51): 원본 L2617-L2695 블록 일괄 이식 —
            //   preferSprint + can* 4 + isClimbSprintSpeed + 6 Sprint 변종 + standing +
            //   isFast 6갈래 OR. 기존 `isFast = grab && isSprinting()` 대체.
            // ※ maxExhaustionForAction 조정 (원본 L2626-L2628, L2651-L2653) 은 handleExhaustion
            //   축소판이 참조하지 않으므로 현재 생략. B-N 확장 여지.
            // ※ isGroundSprinting 전환 후처리 (원본 L2697-L2709) 는 wasRunningWhenSprintStarted /
            //   Options._runOnSprintRelease / isStandupSprintingOrRunning() 미이식 → 별도 원자.
            // ※ isLevitating 필드는 이식(L179)되었으나 갱신 로직 (B-10d) 미이식 → 항상 false.
            //   B-10d 이식 후 isGroundSprinting 공식의 isLevitating 분기 자동 활성.
            {
                // 원본 L2633-L2634 / L2643-L2644: isSprintJump 매 틱 갱신.
                if (!player.isOnGround() && isFast && !isClimbing && !isCeilingClimbing
                        && !isDiving && !isSwimming_sm) {
                    isSprintJump = true;
                }
                if (player.isOnGround() || isFlying || player.getAbilities().flying
                        || isSwimming_sm || isDiving || player.isInLava()) {
                    isSprintJump = false;
                }

                // 원본 L2636-L2641 exhaustionAllowsSprinting (SM 피로 OFF 기본 → 항상 true).
                boolean _exhaustionAllowsSprinting17 = !cfg0.isSprintExhaustionEnabled()
                        || (exhaustion <= cfg0.sprintExhaustionStop
                                && (isFast || isSprintJump
                                        || exhaustion <= cfg0.sprintExhaustionStart));

                // 원본 L2646-L2657 preferSprint (maxExhaustion 조정 축소).
                boolean _preferSprint17 = false;
                if (wantSprint && !wantSneak) {
                    if (_exhaustionAllowsSprinting17) _preferSprint17 = true;
                }

                // **B-50 해소 (세션 133)**: 원본 L2659-L2671 isClimbSprintSpeed 정밀 복원.
                //   원본: isClimbing && preferSprint 일 때만 계산. minTickDistance = wantClimbUp 시
                //     0.07 * freeClimbingUpSpeedFactor / wantClimbDown 시 0.11 * ...Down / else 0.07
                //     isClimbSprintSpeed = getTickDistance() >= minTickDistance.
                //   기존 `true` 근사 → getTickDistance(player) (B-50 헬퍼) 기반 조건부 true.
                boolean _isClimbSprintSpeed17 = false;
                if (isClimbing && _preferSprint17) {
                    double _minTickDistance17;
                    if (wantClimbUp) {
                        _minTickDistance17 = 0.07 * cfg0.freeClimbingUpSpeedFactor;
                    } else if (wantClimbDown) {
                        _minTickDistance17 = 0.11 * cfg0.freeClimbingDownSpeedFactor;
                    } else {
                        _minTickDistance17 = 0.07;
                    }
                    _isClimbSprintSpeed17 = getTickDistance(player) >= _minTickDistance17;
                }

                // 원본 L2673-L2676 can* 4 판정.
                boolean _canAnySprint17 = _preferSprint17
                        && !player.isOnFire()
                        && (cfg0.sprintDuringItemUsage || !player.isUsingItem());
                boolean _canVerticallySprint17 = _canAnySprint17 && !player.verticalCollision;
                boolean _canHorizontallySprint17 = _canAnySprint17
                        && collidedHorizontallyTickCount < 3;
                boolean _canAllSprint17 = _canHorizontallySprint17 && _canVerticallySprint17;

                // 원본 L2678-L2684 6 Sprint 변종.
                // **B-48c 해소 (세션 131)**: 원본 L2678 `wasGroundSprinting = isGroundSprinting;`
                //   이전 틱 저장 — B-48b 전환 후처리 엣지 판정용.
                wasGroundSprinting = isGroundSprinting;
                isGroundSprinting = _canHorizontallySprint17
                        && (player.isOnGround() || isLevitating)
                        && !isSwimming_sm && !isDiving && !isClimbing;
                boolean _isSwimSprinting17    = _canHorizontallySprint17 && isSwimming_sm;
                boolean _isDiveSprinting17    = _canAllSprint17 && isDiving;
                boolean _isCeilingSprinting17 = _canHorizontallySprint17 && isCeilingClimbing;
                boolean _isFlyingSprinting17  = _canAllSprint17 && isFlying;
                boolean _isClimbSprinting17   = _canAnySprint17 && isClimbing && _isClimbSprintSpeed17;

                // 원본 L2686 standing 지역 변수.
                boolean _standing17 = player.isOnGround() && !isSliding && !isCrawling;

                // 원본 L2688-L2695 isFast 6갈래 OR (+ isClimbSprinting 중복 1:1 보존).
                isFast = (isGroundSprinting && (!_standing17 || cfg0.sprintEnableStanding))
                        || _isClimbSprinting17
                        || _isSwimSprinting17
                        || _isDiveSprinting17
                        || _isCeilingSprinting17
                        || _isFlyingSprinting17
                        || _isClimbSprinting17;  // 원본 L2695 중복 그대로 보존

                // **B-48b 해소 (세션 131)**: 원본 L2697-L2709 isGroundSprinting 전환 후처리.
                //   sprint 시작 엣지 → wasRunningWhenSprintStarted 저장 + vanilla setSprinting
                //     (isStandupSprintingOrRunning 조건).
                //   sprint 종료 엣지 → setSprinting(runOnSprintRelease || wasRunningWhenSprintStarted).
                //   walkOnSprintRelease + sprintKeyStopPressed → setSprinting(false) 강제.
                if (isGroundSprinting && !wasGroundSprinting) {
                    // 시작 엣지 (원본 L2697-L2701)
                    wasRunningWhenSprintStarted = player.isSprinting();
                    player.setSprinting(isStandupSprintingOrRunning(player));
                } else if (wasGroundSprinting && !isGroundSprinting) {
                    // 종료 엣지 (원본 L2702-L2705)
                    player.setSprinting(cfg0.runOnSprintRelease || wasRunningWhenSprintStarted);
                }
                // 원본 L2706-L2709: walkOnSprintRelease + sprintKeyStopPressed → 강제 해제
                if (cfg0.walkOnSprintRelease && sprintKeyStopPressed) {
                    player.setSprinting(false);
                }
            }

            // **B-51 해소 (세션 133)**: 원본 L2510 isFlying 공식 정밀화.
            //   원본: isFlying = Config.isFlyingEnabled() && sp.capabilities.isFlying && !isSwimming && !isDiving;
            //   기존 근사: `isFlying = player.getAbilities().flying` 단순 복사.
            //   정밀: `cfg.isFlyingEnabled() && player.getAbilities().flying && !isSwimming_sm && !isDiving`.
            wasFlying = isFlying;
            isFlying = cfg0.isFlyingEnabled()
                    && player.getAbilities().flying
                    && !isSwimming_sm && !isDiving;
            // wasCapabilitiesIsFlying: beforeOnLivingUpdate에서 저장 (vanilla tickMovement 실행 전)
            wasCapabilitiesIsFlying = player.getAbilities().flying;
            // wasCollidedHorizontally: 이전 틱 물리 결과 (HEAD에서 캡처 → 원본 beforeOnUpdate)
            wasCollidedHorizontally = player.horizontalCollision;

            // **B-51 해소 (세션 133)**: 원본 L2511-L2522 isFlying/isLevitating 전환 엣지.
            //   isFlying 전환 (원본 L2511-L2514):
            //     진입 엣지 → setHeightOffset(-1)
            //     해제 엣지 → restoreFromFlying = true
            //   isLevitating 전환 (원본 L2516-L2522, flying 비활성 + levitateSmall 시):
            //     동일 패턴.
            if (isFlying && !wasFlying) {
                heightOffset = -1F;
            } else if (!isFlying && wasFlying) {
                restoreFromFlying = true;
            }
            if (!cfg0.isFlyingEnabled() && cfg0.isLevitateSmallEnabled()) {
                if (isLevitating && !wasLevitating) {
                    heightOffset = -1F;
                } else if (!isLevitating && wasLevitating) {
                    restoreFromFlying = true;
                }
            }

            // B-33 + B-44b (세션 84): 원본 L2441-L2447 매 틱 공식으로 전환.
            // IMPL-01 이원화 구조 (if (!isCrawling) 진입 / else 유지/해제) 를 원본의 단일
            // 매 틱 재계산으로 교체. 해제 판정은 wantCrawl 의 `inputContinueCrawl` /
            // `contextContinueCrawl` 조건이 false 되면 자동 false.
            //
            // 원본 L2441-L2447:
            //   wasCrawling = isCrawling;
            //   isCrawling = canCrawl && (wantCrawl || mustCrawl);
            //   if (!isCrawling) contextContinueCrawl = false;  (L2446-L2447 — 이미 L822)
            //
            // 의존:
            //   wantCrawl / mustCrawl — pre-compute 블록 (L845-L892) 에서 이미 계산됨
            //   canCrawl (B-32 세션 44) — 원본 L2434-L2439 5-AND.
            //   SwimCrawlWaterTopBorder = 0.65F (SmartMovingContext).
            //
            // 기존 IMPL-01 의 별도 "grab 재 누름 → crawlToggled=false" 해제 분기는 제거:
            // 원본은 R-09 블록 (L2966-L3045) 의 `willStopCrawl → crawlToggled=false` 로 자동
            // 처리. willStopCrawl = `!isCrawling && !isCrawlClimbing && !isClimbCrawling` 조건
            // 은 매 틱 공식 결과를 참조하므로 일관성 유지.
            // crawlToggled 설정은 toCrawling() 호출 (B-40 / B-36 / B-18b / B-35 등) 에서 독립
            // 수행.
            //
            // B-44b (세션 84): wasCrawling 저장을 공식 직전으로 이동 (원본 L2441).
            // tickEssential 초반 L829 의 일괄 저장은 제거됨.
            SmartMovingConfig cfg = SmartMovingConfig.Config;
            if (cfg.crawl) {
                boolean canCrawl = !isSwimming_sm
                        && !isDiving
                        && (!isDipping || dippingDepth < 0.65F)
                        && !isClimbing
                        && player.fallDistance < cfg.fallingDistanceMinimum;
                wasCrawling = isCrawling;                              // 원본 L2441
                isCrawling = canCrawl && (wantCrawl || mustCrawl);     // 원본 L2442
                // contextContinueCrawl 해제 (L2446-L2447) 는 L822 pre-compute 블록에 이미 이식.
            }

            // B-34 (세션 59): 원본 L2449-L2450 이식 — `wasCrawling && !isCrawling &&
            // capabilities.flying → tryJump(Config.Up, null, null, null)`.
            // 크롤 해제 시점에 vanilla 비행 모드면 Up 점프 발동 (비행 상승 보조).
            // 원본에서는 isCrawling 공식 직후 + contextContinueCrawl 해제 뒤에 위치.
            // 1.21.1 에서는 IMPL-01 블록 (L988-L1021) 종료 직후 배치 — isCrawling 최종값
            // 확정 뒤. wasCrawling 은 L761 tickEssential 초반 일괄 저장된 이전 틱 값.
            // tryJump(Config.Up, null, null, null) → 1.21.1 tryJump(player, sm, UP, 0F)
            //   (angle==null → vanilla Up 경로).
            if (wasCrawling && !isCrawling && player.getAbilities().flying) {
                SmartMovingJumper.tryJump(player, this, SmartMovingJumper.UP, 0F);
            }

            // B-41 (세션 70): 원본 L2451-L2463 `wantCrawlNotClimb` 갱신 공식 이식.
            //   wantCrawlNotClimb = (wantCrawlNotClimb ||
            //                        (grabButton.StartPressed && !wasCrawling))
            //                    && grabButton.Pressed && moveForward > 0F
            //                    && isCrawling && isCollidedHorizontally;
            // 의미: 크롤 중 전진 입력 + grab + 수평 충돌 상황에서 "등반 아닌 크롤 선호" 플래그
            // 설정. climb 진입 억제 (wouldWantClimb 의 `!wantCrawlNotClimb` 억제 조건으로 사용).
            // 필드는 B-31b 세션 38 이식 완료. grabJustPressed (B-46 세션 66) 사용.
            // wasCrawling 은 L761 이전 틱 저장값. 위치: IMPL-01 종료 + B-34 뒤.
            wantCrawlNotClimb =
                    (wantCrawlNotClimb
                            || (grabJustPressed && !wasCrawling))
                    && SmartMovingKeys.grab.isPressed()
                    && player.input.movementForward > 0F
                    && isCrawling
                    && player.horizontalCollision;

            // B-25 (세션 55): IMPL-02 직접 진입 6-AND 조건 완전 복원 (원본 L2553-L2561).
            //   기존 `isSneaking && isSprinting && onGround && !isClimbing && !isHeadJumping`
            //   간소 매핑 제거 → 원본 공식:
            //     Config.isSlidingEnabled() && grabButton.Pressed &&
            //     (isGroundSprinting || (wasRunning && !isRunning && onGround)) &&
            //     !isCrawling && sneakButton.StartPressed && !isDipping
            // 필드 세팅 (원본 L2558-L2560): isSliding=true + isHeadJumping=false + isAerodynamic=false.
            // ※ wasRunning 저장 (원본 L3043) — B-43 (세션 56) 이식 완료. R-09 블록 종료부에서
            //   `wasRunning = isRunning(player)` 저장 중 → 이 분기 정상 활성.
            // B-26 (세션 75): 원본 L2555-L2557 부수 동작 이식 (근사).
            //   원본: setHeightOffset(-1) + move(0, -1D, 0) + tryJump(Config.SlideDown, false,
            //         wasRunning, null).
            //   ※ 근사 이식 (§7 B-26 근사 등록):
            //   tryJump(SlideDown) 호출 생략 — Jumper.SLIDE_DOWN 상수 + 전용 속도 공식 미이식.
            //   isFromRunning=wasRunning 파라미터 영향도 생략. 효과: 슬라이딩 진입 시 SlideDown
            //   전용 하강 점프 모션 누락 — gameplay 영향 제한 (주로 이펙트/추진).
            if (!isSliding && cfg0.slide && cfg0.enabled
                    && SmartMovingKeys.grab.isPressed()
                    && (isGroundSprinting
                            || (wasRunning && !isRunning(player) && player.isOnGround()))
                    && !isCrawling
                    && sneakKeyStartPressed
                    && !isDipping) {
                heightOffset = -1F;                                     // 원본 L2555
                player.move(MovementType.SELF, new Vec3d(0, -1D, 0));   // 원본 L2556
                // tryJump(Config.SlideDown, false, wasRunning, null) 생략 (§7 B-26 근사)
                isSliding = true;                                        // 원본 L2558
                isHeadJumping = false;                                   // 원본 L2559
                isAerodynamic = false;                                   // 원본 L2560
            }

            // B-23 (세션 46): isHeadJumping 매 틱 재평가 5-AND 해제 공식 (원본 L2524-L2530)
            // `isHeadJumping = isHeadJumping && !onGround && !(swim||dive) && !(flying||capabilities.flying)
            //                  && !(waterMovement && motionY<0) && !lavaMovement`
            // - onGround: 지면 착지 시 해제
            // - swimming/diving: 수중 진입 시 해제
            // - flying/capabilities.isFlying: 비행 모드 진입 시 해제
            // - waterMovement && motionY<0: 물 접촉 + 하강 중 → 수중 진입 예정 해제
            // - lavaMovement: 라바 접촉 시 해제
            wasHeadJumping = isHeadJumping;
            isHeadJumping = isHeadJumping
                    && !player.isOnGround()
                    && !(isSwimming_sm || isDiving)
                    && !(isFlying || player.getAbilities().flying)
                    && !(player.isTouchingWater() && player.getVelocity().y < 0)
                    && !player.isInLava();

            // 원본 L2532-L2533: !isHeadJumping 시 isAerodynamic 리셋 (재평가 뒤 위치로 이동)
            if (!isHeadJumping) isAerodynamic = false;

            // B-24 (세션 53): 원본 L2535-L2540 해제 엣지 후처리.
            //   wasHeadJumping && !isHeadJumping && onGround → handleCrash + restoreFromFlying=true
            // B-N-standup (세션 115): `restoreFromFlying = true` 직후 `standupIfPossible(player,
            //   false, true)` 호출 연결. 원본은 updateEntityActionState 내부에서 별도 위치
            //   호출이나 1.21.1 단일 위치 + 근사 이식이라 여기서 직접 호출.
            if (wasHeadJumping && !isHeadJumping && player.isOnGround()) {
                handleCrash(player, cfg0.headFallDamageStartDistance, cfg0.headFallDamageFactor);
                restoreFromFlying = true;
                // B-N-standup 연결: restoreFromFlying 전환 시 가능하면 즉시 서기 시도.
                standupIfPossible(player, false, true);
            }

            // SlideToHeadJumping 전환 (원본: SmartMovingSelf 행 2546~2550)
            // 슬라이딩 중 낙하거리가 0.05F 초과 → 헤드점프 + 공기역학 모드 전환
            if (isSliding && player.fallDistance > 0.05F) {
                isSliding = false;
                isHeadJumping = true;
                isAerodynamic = true;
            }

            // B-27 (세션 54): 원본 L2569-L2574 fallDistance > _fallingDistanceMinimum 분기.
            //   isSliding && fallDistance > fallingDistanceMinimum →
            //     isSliding=false, wasCrawling=true, isCrawling=false.
            //   SlideToHeadJumping 의 0.05F 임계값보다 훨씬 큰 3F (fallingDistanceMinimum).
            //   둘 다 isSliding 해제지만 SlideToHeadJumping 은 "살짝 낙하 → 헤드점프 전환",
            //   B-27 은 "큰 낙하 → 크롤 전환 준비".
            if (isSliding && player.fallDistance > cfg0.fallingDistanceMinimum) {
                isSliding = false;
                wasCrawling = true;
                isCrawling = false;
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

            // B-30 (세션 43): isStanding 매 틱 갱신 (원본 L2734 `horizontalSpeedSquare < 0.0005`).
            // horizontalSpeedSquare = motionX² + motionZ². 원본 L2389 참조. handleExhaustion
            // L1180 로컬 변수와 중복 계산되지만 isStanding 필드는 tickEssential 단일 진실 공급원.
            {
                double _motionX = player.getVelocity().x;
                double _motionZ = player.getVelocity().z;
                double _horizontalSpeedSquare = _motionX * _motionX + _motionZ * _motionZ;
                isStanding = _horizontalSpeedSquare < 0.0005;
            }

            // B-17a (세션 47): isCrawlClimbing 메인 5-AND 공식 (원본 L2737).
            //   isCrawlClimbing = (wasCrawling || isCrawlClimbing) && isClimbing
            //                    && isNeighborClimbing
            //                    && (sneakButton.Pressed || crawlToggled)
            //                    && esp.movementInput.moveForward > 0F;
            // B-17b1 (세션 48): canStandUp 분기 이식 (원본 L2738-L2754).
            // ※ isNeighborClimbing 갱신 로직 (B-19) 미이식 → 항상 false → 이 공식 결과도
            //   항상 false. B-19 완료 후 자동 활성화.
            {
                boolean _sneakPressed17 = net.minecraft.client.MinecraftClient.getInstance()
                        .options.sneakKey.isPressed();
                boolean _moveForward17 = player.input.movementForward > 0F;
                // 원본 L2736 지역 변수 — 전환 블록에서 사용 (공식 직전 저장).
                boolean _wasCrawlClimbing17 = isCrawlClimbing;
                isCrawlClimbing = (wasCrawling || isCrawlClimbing)
                        && isClimbing
                        && isNeighborClimbing
                        && (_sneakPressed17 || crawlToggled)
                        && _moveForward17;

                // B-17b1: 원본 L2738-L2754 — isCrawlClimbing true 시 canStandUp 판정.
                if (isCrawlClimbing) {
                    // 원본 L2740: canStandUp = !isPlayerInSolidBetween(
                    //     minY - (isClimbCrawling ? 0.95D : 1D), minY)
                    double _crawlOffset17 = isClimbCrawling ? 0.95D : 1D;
                    boolean _canStandUp17 = !isPlayerInSolidBetween(player,
                            player.getY() - _crawlOffset17, player.getY());
                    if (_canStandUp17) {
                        _wasCrawlClimbing17 = false;
                        isCrawlClimbing = false;
                        // 원본 L2746: if(!isClimbCrawling) resetHeightOffset();
                        if (!isClimbCrawling) heightOffset = 0F;
                    }
                    // 원본 L2749-L2753: !wasCrawlClimbing 시 wasCrawling=false, isCrawling=false.
                    if (!_wasCrawlClimbing17) {
                        wasCrawling = false;
                        isCrawling = false;
                    }
                }
                // B-17b2 (세션 73): 원본 L2755-L2783 `else if (wasCrawlClimbing)` 복합 전환 3분기.
                // wasCrawlClimbing 은 이전 틱 값 (L1307 `_wasCrawlClimbing17 = isCrawlClimbing`
                // 공식 직전 저장). isCrawlClimbing 이 이번 틱에 false 면서 이전 틱 true 였던
                // 경우 = "크롤 등반 해제" 시점. 3갈래 분기로 상황별 보정.
                else if (_wasCrawlClimbing17) {
                    boolean toCrawlingLocal = _sneakPressed17 || crawlToggled;
                    double minY = player.getBoundingBox().minY;

                    if (!isClimbing) {
                        // 분기 1 (원본 L2758-L2764): 등반 종료 — toCrawling() 메서드로 크롤 재진입
                        // 판정 + 바닥 스냅. 원본 `toCrawling()` 은 세션 44 B-40 이식 완료.
                        wasCrawling = toCrawling();
                        player.move(MovementType.SELF,
                                new Vec3d(0, -minY + Math.floor(minY), 0));
                    } else if (player.input.movementForward <= 0F) {
                        // 분기 2 (원본 L2765-L2777): 전진 해제 — 로컬 toCrawling boolean 반영,
                        // wantClimbUp/Down 리셋, 조건부 높이 복귀. toCrawling=true 면 크롤 유지,
                        // false 면 서기 (높이 +1).
                        wasCrawling = toCrawlingLocal;
                        isCrawling  = toCrawlingLocal;
                        wantClimbUp   = false;
                        wantClimbDown = false;
                        if (!toCrawlingLocal) heightOffset = 0F;
                        player.move(MovementType.SELF,
                                new Vec3d(0,
                                        -minY + Math.floor(minY) + (toCrawlingLocal ? 0F : 1F),
                                        0));
                    } else if (!toCrawlingLocal) {
                        // 분기 3 (원본 L2778-L2783): 스니크 해제 (전진은 유지) — 높이 리셋 후
                        // 위쪽(ceil) 스냅.
                        heightOffset = 0F;
                        player.move(MovementType.SELF,
                                new Vec3d(0, Math.ceil(minY) - minY, 0));
                    }
                }
            }

            // B-18 (세션 81) → **B-42-B18a/B18b 해소 (세션 126)**: 원본 L2786-L2820
            // `isClimbCrawling` 메인 공식 + climbIntoCount 카운터 + 진입/해제 엣지 정밀 이식.
            // B-17 (isCrawlClimbing) 블록 뒤 + B-35 앞 배치 — 원본 L2786 순서 복원.
            //
            // **B-42-B18b (Mixin setter) 해소 불필요 확정**: vanilla `Entity.horizontalCollision`
            //   은 `public boolean` (final 아님) — MixinExtras `@Accessor` 없이도 직접
            //   할당 가능. 별도 Mixin 신설 불필요 → 원자 단순 해소.
            // **B-42-B18a 해소**:
            //   (1) 진입 엣지 `wasColH` 저장/복원 — `boolean wasColH = player.horizontalCollision;`
            //       → `player.move(...)` → `player.horizontalCollision = wasColH;` 1:1 복원.
            //   (2) 해제 엣지 `gapUnderneight` AABB 정밀 복원 — B-42a `getMaxPlayerSolidBetween`
            //       소비. 원본 `gap >= 0D && gap < 1D` 조건 + `move(0, -gap, 0)` 이동량 활성.
            // ※ 의존 전수 충족: hasClimbCrawlGap (B-15 세션 40) / hasClimbGap (B-15b) /
            //   isClimbHolding (B-16) / wantClimbHolding (B-18-pre 세션 81) / wantClimbUp
            //   (B-17b2-pre 세션 72) / climbIntoCount 필드 (기존).
            {
                // B-44c (세션 82): 공식 직전 저장 (원본 L2786 대응).
                wasClimbCrawling = isClimbCrawling;
                boolean needClimbCrawling = hasClimbCrawlGap || (hasClimbGap && isClimbHolding);
                boolean canClimbCrawling = wantClimbHolding && wantClimbUp;

                if (climbIntoCount > 1) {
                    climbIntoCount--;
                } else if (isClimbCrawling && !needClimbCrawling && climbIntoCount == 0) {
                    climbIntoCount = 6;
                }

                isClimbCrawling = canClimbCrawling
                        && ((needClimbCrawling && climbIntoCount == 0) || climbIntoCount > 1);

                if (isClimbCrawling && !wasClimbCrawling) {
                    // 진입 엣지 (원본 L2797-L2803) — B-42-B18a 해소 완전 이식.
                    heightOffset = -1F;
                    // 원본 L2800: `boolean wasCollidedHorizontally = sp.isCollidedHorizontally;`
                    boolean wasColH = player.horizontalCollision;
                    // 원본 L2801: move(0, 0.05, 0) — solid 머리 위에 서있을 때 crawl 진입 방지
                    player.move(MovementType.SELF, new Vec3d(0, 0.05, 0));
                    // 원본 L2802: sp.isCollidedHorizontally = wasCollidedHorizontally;
                    //   (water 밖으로 crawl 탈출 버그 방지)
                    player.horizontalCollision = wasColH;
                } else if (!isClimbCrawling && wasClimbCrawling) {
                    // 해제 엣지 (원본 L2804-L2820) — B-42-B18a 해소 완전 이식 (AABB 정밀).
                    climbIntoCount = 0;
                    if (mustCrawl || sneakPressedRaw || crawlToggled) {
                        // 원본 L2809: getMaxPlayerSolidBetween(minY - 1D, minY, 0)
                        double minY18 = player.getBoundingBox().minY;
                        double gapUnderneight = minY18
                                - getMaxPlayerSolidBetween(player, minY18 - 1D, minY18, 0);
                        if (gapUnderneight >= 0D && gapUnderneight < 1D) {
                            // 원본 L2812-L2813: 크롤 전환 + move(0, -gap, 0)
                            wasCrawling = toCrawling();
                            player.move(MovementType.SELF, new Vec3d(0, -gapUnderneight, 0));
                        } else {
                            heightOffset = 0F;  // 원본 L2816 resetHeightOffset
                        }
                    } else {
                        heightOffset = 0F;  // 원본 L2819 resetHeightOffset
                    }
                }
            }

            // B-35 (세션 74) → **B-42-B35 해소 (세션 122)**: 원본 L2822-L2836 정밀 이식.
            // 원본 L2825 `move(0, (crawlStandUpBottom - sp.boundingBox.minY), 0, true)` 복원.
            // `crawlStandUpBottom` 은 원본 L2399 지역 변수 (isCrawling||isClimbCrawling 시에만
            // 계산). B-35 분기 A 진입 시점엔 `wasCrawling=true` 이고 L2399 계산 당시 isCrawling
            // 이 true 였으므로 값 확보 가능 — 1.21.1 에서는 분기 A 내부에서 직접 재계산.
            // 분기 A: wasCrawling && !isCrawling && !initializeCrawling && !flying
            //   → resetHeightOffset + move(0, crawlStandUpBottom - minY, 0).
            if (wasCrawling && !isCrawling && !initializeCrawling
                    && !player.getAbilities().flying) {
                heightOffset = 0F;
                // 원본 L2399: getMaxPlayerSolidBetween(minY - (initializeCrawling ? 0D : 1D), minY,
                //   crawlOverEdge ? 0 : -0.05). 분기 A 는 initializeCrawling=false 이므로 오프셋 1D.
                double minY = player.getBoundingBox().minY;
                double horizontalTolerance = cfg.crawlOverEdge ? 0 : -0.05;
                double crawlStandUpBottom = getMaxPlayerSolidBetween(player,
                        minY - 1D, minY, horizontalTolerance);
                player.move(MovementType.SELF, new Vec3d(0, crawlStandUpBottom - minY, 0));
            }
            // 분기 B: (isCrawling && !wasCrawling) || initializeCrawling
            //   → setHeightOffset(-1F) + move(0, -1D, 0) + (initializeCrawling → toCrawling())
            if ((isCrawling && !wasCrawling) || initializeCrawling) {
                heightOffset = -1F;
                player.move(MovementType.SELF, new Vec3d(0, -1D, 0));
                if (initializeCrawling) toCrawling();
            }

            // B-36 (세션 78): 원본 L2839-L2862 `grab.StartPressed` 수영/크롤 전환 3분기 이식.
            // 원본 구조 (중괄호 없는 체이닝 → 1.21.1 에서 중괄호 명시):
            //   if (grabButton.StartPressed)
            //     if (isShallowDiveOrSwim && wouldWantClimb) { (a) 얕은 물 swim/dive → walking }
            //     else if (isDipping && wouldWantCrawl && dippingDepth >= 0.55F)
            //       if (dippingDepth >= 0.6F) { (b) dipping → swimming/diving }
            //       else { (c) dipping → 얕은 물 crawl }
            // 상수 (원본 SmartMovingContext L43-L44): MediumBorder=0.6F / BottomBorder=0.55F.
            // 의존: B-10a isShallowDiveOrSwim 필드 (공식 미이식 → 항상 false → (a) 비활성),
            //       B-36-pre wouldWantClimb/wouldWantCrawl 필드 (세션 77), B-10c isStillSwimmingJump
            //       필드, B-40 toCrawling(), B-46 grabJustPressed.
            // **B-42-B36 해소 (세션 123)**: 분기 (a) 의 `getMaxPlayerSolidBetween(minY, maxY,
            //   0) - minY` 이동량 복원. B-42a 헬퍼 소비.
            if (grabJustPressed) {
                if (isShallowDiveOrSwim && wouldWantClimb) {
                    // (a) 얕은 물 swim/dive → walking 전환 (원본 L2841-L2847)
                    heightOffset = 0F;  // resetHeightOffset
                    // 원본 L2843: move(0, getMaxPlayerSolidBetween(minY, maxY, 0) - minY, 0, true)
                    double minY36a = player.getBoundingBox().minY;
                    double maxY36a = player.getBoundingBox().maxY;
                    double groundY36a = getMaxPlayerSolidBetween(player, minY36a, maxY36a, 0);
                    player.move(MovementType.SELF, new Vec3d(0, groundY36a - minY36a, 0));
                    if (_jumpPressed3a) isStillSwimmingJump = true;
                } else if (isDipping && wouldWantCrawl && dippingDepth >= 0.55F) {
                    if (dippingDepth >= 0.6F) {
                        // (b) dipping → swimming/diving 전환 (원본 L2850-L2855)
                        heightOffset = -1F;
                        player.move(MovementType.SELF,
                                new Vec3d(0, -1.6F + dippingDepth, 0));
                        isCrawling = false;
                    } else {
                        // (c) dipping → 얕은 물 crawl 전환 (원본 L2856-L2862)
                        heightOffset = -1F;
                        player.move(MovementType.SELF, new Vec3d(0, -1D, 0));
                        wasCrawling = toCrawling();
                    }
                }
            }

            // ── 원본 R-09 스닉/크롤 토글 블록 (SmartMovingSelf L2966-L3045) 1:1 이식 ────
            // isSlow/isCrawling/isClimbCrawling 이 이 시점에 확정되어 있어야 함 (위에서 계산됨).
            // wasSneaking/wasCrawling/wasClimbCrawling 는 else 블록 진입부에서 저장됨.
            {
                // B-45b (세션 45): Config 헬퍼 치환 (의미 동일).
                boolean isSneakToggleEnabled = cfg.isSneakToggleEnabled();
                boolean isCrawlToggleEnabled = cfg.isCrawlToggleEnabled();

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

                // B-4 (세션 52): 원본 L2990 간소 매핑 제거 — wantSneak_/wantSprint_ 로컬
                //   (간소 매핑) → B-2 wantSneak / B-3a wantSprint 실제 값 사용.
                //   wantSneak 은 L835 else 블록 직계 지역 변수 (이 블록과 동일 스코프).
                //   wantSprint 은 public 필드 (L1415 원본 대응). 둘 다 B-2/B-3a 에서
                //   원본 공식 1:1 이식 완료.

                boolean willStopSneak = false;
                if (isSneakToggleEnabled) {
                    if (isCrawling && !willStopCrawlStartSneak)
                        willStopSneak = true;
                    // 원본 L2990: `wantSneak && wantSprint && sneakButton.StartPressed && sneakToggled`
                    if (wantSneak && wantSprint && sneakKeyStartPressed && sneakToggled) {
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
                    if (isCrawling && !wasCrawling)
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

                // B-43 (세션 56): R-09 블록 종료부 저장 2건 이식 (원본 L3043-L3044).
                // wasRunning 은 B-25 isSliding 직접 진입 조건 `wasRunning && !isRunning && onGround`
                // 분기에서 사용 — 이 저장 없이는 항상 false 였음.
                wasRunning    = isRunning(player);
                wasLevitating = isLevitating;
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
        wasCrawling = false;
        wasClimbCrawling = false;
        sneakKeyStartPressed = false;
        sneakKeyStopPressed = false;
        grabJustPressed     = false;
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
        initialized             = false;
        // B Phase 1 (세션 38) 추가 필드 리셋
        wasHeadJumping          = false;
        isStanding              = false;
        wasRunning              = false;
        wasLevitating           = false;
        isShallowDiveOrSwim     = false;
        isJumpingOutOfWater     = false;
        wasJumpingOutOfWater    = false;
        isStillSwimmingJump     = false;
        wantCrawlNotClimb       = false;
        initializeCrawling      = false;
        wantSprint              = false;
        wantClimb               = false;
        wantClimbUp             = false;
        wantClimbDown           = false;
        wouldWantClimb          = false;
        wouldWantCrawl          = false;
        wantClimbHolding        = false;
        isGroundSprinting       = false;
        collidedHorizontallyTickCount = 0;
        restoreFromFlying       = false;
        // B Phase 1 (세션 40) 등반 9 필드 리셋
        isVineOnlyClimbing      = false;
        isVineAnyClimbing       = false;
        isClimbingStill         = false;
        isNeighborClimbing      = false;
        hasClimbGap             = false;
        hasNeighborClimbGap     = false;
        hasNeighborClimbCrawlGap = false;
        handsEdgeBlock          = null;
        feetEdgeBlock           = null;
    }

    private static boolean canStandUp(ClientPlayerEntity player) {
        Box standBox = player.getDimensions(EntityPose.STANDING)
                             .getBoxAt(player.getPos())
                             .contract(1.0E-7);
        return player.getWorld().isSpaceEmpty(player, standBox);
    }

    /**
     * 원본 `SmartMovingSelf.isPlayerInSolidBetween(y1, y2)`: 플레이어 x/z 범위 + y1~y2
     * Y 범위의 AABB 내 solid 블록 존재 여부.
     * 1.21.1: vanilla `World.isSpaceEmpty(entity, box)` 로 정밀 구현 (근사 아님).
     * 사용처: B-17b canStandUp 분기 (원본 L2740) — isCrawlClimbing 해제 판정.
     * B-17b (세션 48).
     */
    private static boolean isPlayerInSolidBetween(ClientPlayerEntity player, double y1, double y2) {
        Box box = new Box(
                player.getBoundingBox().minX, y1, player.getBoundingBox().minZ,
                player.getBoundingBox().maxX, y2, player.getBoundingBox().maxZ
        );
        return !player.getWorld().isSpaceEmpty(player, box);
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-42a (세션 117) — getMaxPlayerSolidBetween AABB 정밀 헬퍼
    // 원본: SmartMovingBase.java L229-L248 + L299-L306 (isCollided).
    // Phase 6 시작. §7 B-35/B-36/B-39/B-18/B-26 근사 승격 대상 선행 의존.
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 원본 L229-L231 `getPlayerSolidBetween(yMin, yMax, horizontalTolerance)` + L239-L248
     * `getMaxPlayerSolidBetween(yMin, yMax, horizontalTolerance)` 통합 이식.
     *
     * 플레이어 AABB (X/Z 범위 ± horizontalTolerance) 내 [yMin, yMax] Y 구간의
     * 최고 고체 블록 `box.maxY` 반환. 콜리전 없으면 `yMin`.
     *
     * 원본 동작:
     *   1. sp.boundingBox.minY/maxY 를 yMin/yMax 로 임시 변경 + contract(-h, 0, -h) 로 확장
     *   2. world.getCollidingBoundingBoxes(sp, box) 로 AABB 리스트 수집
     *   3. boundingBox 복원
     *   4. 각 box 에 isCollided 검증 (horizontal + yMin/yMax 범위 재확인)
     *   5. 통과한 box 의 maxY 최대값 (단, yMax 상한)
     *
     * 1.21.1 매핑:
     *   - `sp.boundingBox` 는 immutable → 새 `Box` 생성
     *   - `contract(-h, 0, -h)` (음수 contract = 확장) → `Box` 생성 시 x/z 범위에 ±h 적용
     *   - `world.getCollidingBoundingBoxes(sp, box)` → `world.getBlockCollisions(entity, box)`
     *     반환 `Iterable<VoxelShape>`
     *   - `VoxelShape.getBoundingBox()` 로 shape 단일 Box 추출
     *
     * **§7 근사** (B-42a-approx): VoxelShape → Box 단일 변환 — multi-shape stair/slab 블록
     * 은 여러 하위 box 로 구성되나 `getBoundingBox()` 는 전체 외접 box 반환. 대부분 full
     * block 은 단일 box 라 동치, slab/stair top-half 는 외접 box 로 근사 (정밀도 약간 상승 —
     * 실제 stair 상단부보다 크게 잡힘 가능성). 대안인 VoxelShape iteration 은 구조 복잡 —
     * 실용상 getBoundingBox() 충분.
     *
     * B-42a (세션 117) — Phase 6 시작 원자.
     */
    public static double getMaxPlayerSolidBetween(ClientPlayerEntity player,
                                                   double yMin, double yMax,
                                                   double horizontalTolerance) {
        Box pb = player.getBoundingBox();
        Box checkBox = new Box(
                pb.minX - horizontalTolerance, yMin, pb.minZ - horizontalTolerance,
                pb.maxX + horizontalTolerance, yMax, pb.maxZ + horizontalTolerance);

        double result = yMin;
        for (net.minecraft.util.shape.VoxelShape shape
                : player.getWorld().getBlockCollisions(player, checkBox)) {
            if (shape.isEmpty()) continue;
            // 근사 이식 — 원본과 차이: VoxelShape.getBoundingBox() 단일 box (multi-shape 외접)
            Box box = shape.getBoundingBox();
            // 원본 L299-L306 `isCollided(box, yMin, yMax, horizontalTolerance)` 인라인 이식
            if (box.maxX >= pb.minX - horizontalTolerance
                    && box.minX <= pb.maxX + horizontalTolerance
                    && box.maxY >= yMin
                    && box.minY <= yMax
                    && box.maxZ >= pb.minZ - horizontalTolerance
                    && box.minZ <= pb.maxZ + horizontalTolerance) {
                result = Math.max(result, box.maxY);
            }
        }
        return Math.min(result, yMax);
    }

    /**
     * 원본 L229-L231 `getPlayerSolidBetween` + L398-L409 `getMinPlayerSolidBetween` 통합 이식.
     *
     * 플레이어 AABB (X/Z 범위 ± horizontalTolerance) 내 [yMin, yMax] Y 구간의
     * 최저 고체 블록 `box.minY` 반환. 콜리전 없으면 `yMax`. B-42a 대칭.
     *
     * 원본 L398-L409:
     *   result = yMax
     *   for box in solids:
     *       if isCollided(box, yMin, yMax, horizontalTolerance):
     *           result = min(result, box.minY)
     *   return max(result, yMin)
     *
     * 호출처 (원본 SmartMovingSelf):
     *   - L266 `minPlayerSwimWaterCeiling` (swim ceiling 판정)
     *   - L1151 `actuallySolidHeight` (jump clearance)
     *   - L1373 / L2400 `crawlStandUpCeiling` (일어설 때 머리 위 천장)
     *
     * **§7 근사** (B-42a 와 동일): VoxelShape → Box 단일 외접 box. multi-shape 블록은
     * 외접 box 가 실제보다 범위가 넓어 minY 가 약간 낮게 잡힘 가능성 — crawlStandUpCeiling
     * 판정에서는 약간 더 보수적으로 작동 (실용 등가).
     *
     * B-42b (세션 118) — Phase 6 두 번째 원자.
     */
    public static double getMinPlayerSolidBetween(ClientPlayerEntity player,
                                                   double yMin, double yMax,
                                                   double horizontalTolerance) {
        Box pb = player.getBoundingBox();
        Box checkBox = new Box(
                pb.minX - horizontalTolerance, yMin, pb.minZ - horizontalTolerance,
                pb.maxX + horizontalTolerance, yMax, pb.maxZ + horizontalTolerance);

        double result = yMax;
        for (net.minecraft.util.shape.VoxelShape shape
                : player.getWorld().getBlockCollisions(player, checkBox)) {
            if (shape.isEmpty()) continue;
            // 근사 이식 — 원본과 차이: VoxelShape.getBoundingBox() 단일 box (multi-shape 외접)
            Box box = shape.getBoundingBox();
            // 원본 L299-L306 `isCollided(box, yMin, yMax, horizontalTolerance)` 인라인 이식
            if (box.maxX >= pb.minX - horizontalTolerance
                    && box.minX <= pb.maxX + horizontalTolerance
                    && box.maxY >= yMin
                    && box.minY <= yMax
                    && box.maxZ >= pb.minZ - horizontalTolerance
                    && box.minZ <= pb.maxZ + horizontalTolerance) {
                result = Math.min(result, box.minY);
            }
        }
        return Math.max(result, yMin);
    }

    /**
     * 원본 SmartMovingBase L131-L152 `getLiquidBorder(i, j, k)` 근사 이식.
     * 해당 블록의 액체 높이 (0.0~1.0F) 반환. 0 = 비액체.
     *
     * 원본 판정 순서:
     *   1. water / flowing_water → getNormalWaterBorder
     *   2. FiniteLiquid 모드 → getFiniteLiquidWaterBorder
     *   3. lava / flowing_lava → _lavaLikeWater ? getNormalWaterBorder : 0F
     *   4. Material.lava → _lavaLikeWater ? 1F : 0F
     *   5. Material.water → getNormalWaterBorder
     *   6. material.isLiquid() → 1F
     *   7. 그 외 → 0F
     *
     * 1.21.1 매핑:
     *   - `world.getBlock(i,j,k).getMaterial()` → `world.getFluidState(pos)` 로 단순화
     *   - `FluidState.isIn(FluidTags.WATER)` + `getHeight(world, pos)` → 물 높이 (0~1)
     *   - **§7 근사 (B-42c-a)**: FiniteLiquid mod 분기 생략 (mod 1.21.1 미이식)
     *   - **§7 근사 (B-42c-b)**: `_lavaLikeWater` Config 필드 미이식 → lava 처리 생략
     *     (lava 는 항상 `0F` 반환). 원본 default 값은 false 이므로 근사 영향 제한적.
     *   - **§7 근사 (B-42c-c)**: Normal/Material.water 분기 통합 — FluidState 는 항상
     *     `getHeight()` 반환. `getNormalWaterBorder` 의 metadata 기반 구분
     *     (`>=8→1F / ==0+air→0.8875F / 기타→(8-meta)/8F`) 은 1.21.1 `FluidState.getHeight`
     *     내부 로직에 흡수됨 (FlowableFluid 구현).
     *
     * B-42c (세션 119) — Phase 6 세 번째 원자 (액체 경계 헬퍼).
     */
    private static float getLiquidBorder(ClientPlayerEntity player, int i, int j, int k) {
        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(i, j, k);
        net.minecraft.fluid.FluidState fluid = player.getWorld().getFluidState(pos);
        if (fluid.isEmpty()) return 0F;
        // 근사 이식 — 원본과 차이: water 만 처리. lava 는 _lavaLikeWater 미이식으로 0F
        if (fluid.isIn(net.minecraft.registry.tag.FluidTags.WATER)) {
            return fluid.getHeight(player.getWorld(), pos);
        }
        return 0F;
    }

    /**
     * 원본 SmartMovingBase L418-L432 `getMaxPlayerLiquidBetween(yMin, yMax)` 이식.
     *
     * 플레이어 X/Z 위치의 [yMin, yMax] Y 구간에서 yMax→yMin 방향(위→아래) 탐색.
     * 첫 번째 액체 블록 발견 시 `j + liquidBorder` 반환. 없으면 `yMin`.
     *
     * 호출처:
     *   - 원본 SmartMovingSelf L265 `totalSwimWaterBorder` (swim 경계)
     *   - `isInLiquid()` (원본 L411-L416) 본체
     *
     * B-42c (세션 119).
     */
    public static double getMaxPlayerLiquidBetween(ClientPlayerEntity player,
                                                    double yMin, double yMax) {
        int i = net.minecraft.util.math.MathHelper.floor(player.getX());
        int jMin = net.minecraft.util.math.MathHelper.floor(yMin);
        int jMax = net.minecraft.util.math.MathHelper.floor(yMax);
        int k = net.minecraft.util.math.MathHelper.floor(player.getZ());

        for (int j = jMax; j >= jMin; j--) {   // 위에서 아래로 탐색
            float swimWaterBorder = getLiquidBorder(player, i, j, k);
            if (swimWaterBorder > 0) {
                return j + swimWaterBorder;
            }
        }
        return yMin;
    }

    /**
     * 원본 SmartMovingBase L434-L451 `getMinPlayerLiquidBetween(yMin, yMax)` 이식.
     *
     * 플레이어 X/Z 위치의 [yMin, yMax] Y 구간에서 yMin→yMax 방향(아래→위) 탐색.
     * 첫 액체 발견 시:
     *   - `j > yMin` : 블록이 yMin 보다 위 → `j` 반환 (블록 하단)
     *   - `j + border > yMin` : 블록 액체 상단이 yMin 초과 → `yMin` 반환
     * 없으면 `yMax`.
     *
     * 호출처:
     *   - 원본 SmartMovingSelf L1372 / L2414 `crawlStandUpLiquidCeiling`
     *   - `isInLiquid()` (원본 L411-L416) 본체
     *
     * B-42c (세션 119).
     */
    public static double getMinPlayerLiquidBetween(ClientPlayerEntity player,
                                                    double yMin, double yMax) {
        int i = net.minecraft.util.math.MathHelper.floor(player.getX());
        int jMin = net.minecraft.util.math.MathHelper.floor(yMin);
        int jMax = net.minecraft.util.math.MathHelper.floor(yMax);
        int k = net.minecraft.util.math.MathHelper.floor(player.getZ());

        for (int j = jMin; j <= jMax; j++) {   // 아래에서 위로 탐색
            float swimWaterBorder = getLiquidBorder(player, i, j, k);
            if (swimWaterBorder > 0) {
                if (j > yMin) {
                    return j;
                } else if (j + swimWaterBorder > yMin) {
                    return yMin;
                }
            }
        }
        return yMax;
    }

    /**
     * 원본 SmartMovingBase L411-L416 `isInLiquid()` 이식.
     *
     * 플레이어 AABB 세로 구간 [minY, maxY] 내 액체 존재 여부. water + lava (lavaLikeWater
     * 활성 시) 모두 포함. `isInWater()` 는 vanilla 필드 기반이라 전환 엣지에 누락 가능 →
     * SM 은 AABB 스캔으로 정확 판정.
     *
     * 원본:
     *   getMaxPlayerLiquidBetween(minY, maxY) != minY       // 상단 액체 경계 > minY
     *   || getMinPlayerLiquidBetween(minY, maxY) != maxY    // 하단 액체 경계 < maxY
     *
     * 소비처: Swimmer.updateSwimState 진입 조건 `wasSwimming && isInLiquid()` (원본 L232).
     * B-7c 에서 복원.
     *
     * B-7d (세션 127).
     */
    public static boolean isInLiquid(ClientPlayerEntity player) {
        Box pb = player.getBoundingBox();
        return getMaxPlayerLiquidBetween(player, pb.minY, pb.maxY) != pb.minY
            || getMinPlayerLiquidBetween(player, pb.minY, pb.maxY) != pb.maxY;
    }

    /**
     * 원본 SmartMovingSelf L255-L270 의 AABB 기반 파생값 9개 통합 계산.
     *
     * 원본:
     *   int i = MathHelper.floor_double(sp.posX);
     *   int j = MathHelper.floor_double(sp.boundingBox.minY);
     *   int k = MathHelper.floor_double(sp.posZ);
     *   double j_offset = sp.boundingBox.minY - j;
     *   double totalSwimWaterBorder = getMaxPlayerLiquidBetween(maxY - 1.8, maxY + 1.2);
     *   double minPlayerSwimWaterCeiling = getMinPlayerSolidBetween(maxY - 1.8, maxY + 1.2, 0);
     *   double realTotalSwimWaterBorder = Math.min(totalSwimWaterBorder, minPlayerSwimWaterCeiling);
     *   double minPlayerSwimWaterDepth = totalSwimWaterBorder
     *       - getMaxPlayerSolidBetween(totalSwimWaterBorder - 2, totalSwimWaterBorder, 0);
     *   double realMinPlayerSwimWaterDepth = totalSwimWaterBorder
     *       - getMaxPlayerSolidBetween(realTotalSwimWaterBorder - 2, realTotalSwimWaterBorder, 0);
     *   double playerSwimWaterBorder = totalSwimWaterBorder - j - j_offset;
     *
     * 1.21.1 은 원본의 단일 메서드 `handleSwimming` 이 `Swimmer.updateSwimState` +
     * `Swimmer.handleSwimming` 두 정적 메서드로 분리되어 있어, 두 곳에서 모두 접근
     * 가능한 struct-like 반환값으로 제공.
     *
     * B-42d (세션 120) — Phase 6 네 번째 원자. 소비는 후속 승격 원자
     * (B-42-B5 / B-42-B35 / B-42-B36 등) 에서.
     */
    public static final class SwimBorderValues {
        public final int i, j, k;
        public final double j_offset;
        public final double totalSwimWaterBorder;
        public final double minPlayerSwimWaterCeiling;
        public final double realTotalSwimWaterBorder;
        public final double minPlayerSwimWaterDepth;
        public final double realMinPlayerSwimWaterDepth;
        public final double playerSwimWaterBorder;

        private SwimBorderValues(int i, int j, int k,
                                 double j_offset,
                                 double totalSwimWaterBorder,
                                 double minPlayerSwimWaterCeiling,
                                 double realTotalSwimWaterBorder,
                                 double minPlayerSwimWaterDepth,
                                 double realMinPlayerSwimWaterDepth,
                                 double playerSwimWaterBorder) {
            this.i = i;
            this.j = j;
            this.k = k;
            this.j_offset = j_offset;
            this.totalSwimWaterBorder = totalSwimWaterBorder;
            this.minPlayerSwimWaterCeiling = minPlayerSwimWaterCeiling;
            this.realTotalSwimWaterBorder = realTotalSwimWaterBorder;
            this.minPlayerSwimWaterDepth = minPlayerSwimWaterDepth;
            this.realMinPlayerSwimWaterDepth = realMinPlayerSwimWaterDepth;
            this.playerSwimWaterBorder = playerSwimWaterBorder;
        }
    }

    public static SwimBorderValues computeSwimBorderValues(ClientPlayerEntity player) {
        Box pb = player.getBoundingBox();
        int i = net.minecraft.util.math.MathHelper.floor(player.getX());
        int j = net.minecraft.util.math.MathHelper.floor(pb.minY);
        int k = net.minecraft.util.math.MathHelper.floor(player.getZ());
        double j_offset = pb.minY - j;

        double totalSwimWaterBorder      = getMaxPlayerLiquidBetween(player, pb.maxY - 1.8, pb.maxY + 1.2);
        double minPlayerSwimWaterCeiling = getMinPlayerSolidBetween(player, pb.maxY - 1.8, pb.maxY + 1.2, 0);
        double realTotalSwimWaterBorder  = Math.min(totalSwimWaterBorder, minPlayerSwimWaterCeiling);
        double minPlayerSwimWaterDepth   = totalSwimWaterBorder
                - getMaxPlayerSolidBetween(player, totalSwimWaterBorder - 2, totalSwimWaterBorder, 0);
        double realMinPlayerSwimWaterDepth = totalSwimWaterBorder
                - getMaxPlayerSolidBetween(player, realTotalSwimWaterBorder - 2, realTotalSwimWaterBorder, 0);
        double playerSwimWaterBorder     = totalSwimWaterBorder - j - j_offset;

        return new SwimBorderValues(i, j, k, j_offset,
                totalSwimWaterBorder, minPlayerSwimWaterCeiling, realTotalSwimWaterBorder,
                minPlayerSwimWaterDepth, realMinPlayerSwimWaterDepth, playerSwimWaterBorder);
    }

    // ── B Phase 1 B-22c (세션 42) — 원본 SmartMovingSelf 메서드 2개 이식 ──────────

    /**
     * 원본 SmartMovingSelf L3327-L3330 `private boolean vanilla()`:
     *   return !Config.enabled || Config._vanillaStyle.value;
     * SM 비활성 상태 또는 명시적 vanilla 스타일 옵션 활성 시 true.
     * isRunning() / handleLand L678/L721 / tryJump L2047 등 여러 조건에 사용.
     */
    private boolean vanilla() {
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        return !cfg.enabled || cfg.vanillaStyle;
    }

    /**
     * 원본 SmartMovingSelf L3234-L3237 `public boolean isStandupSprintingOrRunning()`:
     *   return (isFast || sp.isSprinting()) && sp.onGround && !isSliding && !isCrawling;
     * **파생 메서드** — 필드 저장 없음. 매 호출마다 재계산.
     * 사용처: B-48b 전환 후처리 (원본 L2700 `sp.setSprinting(isStandupSprintingOrRunning())`) /
     *   원본 L2902 `canBackJump = ... && !isStandupSprintingOrRunning()`.
     * B-48b (세션 131).
     */
    public boolean isStandupSprintingOrRunning(ClientPlayerEntity player) {
        return (isFast || player.isSprinting())
                && player.isOnGround()
                && !isSliding
                && !isCrawling;
    }

    /**
     * 원본 SmartStatistics L43-L60 `calculateAllStats` 의 tickDistance 공식 1:1 이식.
     *   diffX = sp.posX - sp.prevPosX; (동일 Y/Z)
     *   tickDistance = data.all.calcualte(sqrt(diffX² + diffY² + diffZ²))
     *     = sqrt(...) * 4F   (SmartStatisticsData L47: `distance = distance * 4F`)
     *
     * **§7 근사** (B-50-approx): SmartStatistics 의 `legYaw` / `total` smoothing 버퍼 미이식.
     *   `calcualte()` 반환값 자체는 `distance * 4F` 단순 — 이 부분은 정확 1:1. 부수 효과
     *   (limbSwingAmount / limbSwing 갱신 — rendering용) 은 vanilla 가 자체 처리하므로
     *   SM 측 통계 캐시 불필요.
     *
     * B-50 (세션 133). SmartStatisticsFactory + SmartStatisticsDatas Hashtable 캐시 등
     * 인프라 이식 없이 매 호출 계산 — 호출처 1곳 (isClimbSprintSpeed) 이라 비용 무시 가능.
     */
    public static float getTickDistance(net.minecraft.entity.player.PlayerEntity player) {
        double dx = player.getX() - player.prevX;
        double dy = player.getY() - player.prevY;
        double dz = player.getZ() - player.prevZ;
        return (float) (Math.sqrt(dx * dx + dy * dy + dz * dz) * 4.0);
    }

    /**
     * 원본 SmartMovingSelf L3239-L3242 `public boolean isRunning() override`:
     *   return sp.isSprinting() && !isFast && (sp.onGround || vanilla());
     * **파생 메서드** — 필드 저장 없음. 매 호출마다 재계산.
     * 사용처: handleExhaustion Config.getFactor (원본 L1202/L1278) + L3043 `wasRunning = isRunning`.
     * B-22c (세션 42).
     */
    public boolean isRunning(ClientPlayerEntity player) {
        return player.isSprinting() && !isFast && (player.isOnGround() || vanilla());
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-N-standup (세션 115) — standupIfPossible() 메서드 이식 + resetHeightOffset +
    //                          standUp 보조
    // 원본: SmartMovingSelf.java L1681-L1686 (resetHeightOffset),
    //       L2165-L2183 (standupIfPossible()),
    //       L2186-L2212 (standupIfPossible(tryLanding, restoreFromFlying)),
    //       L2214-L2219 (standUp).
    //
    // **§7 근사 다수** — 원본은 boundingBox 직접 조작 + AABB 정밀 `getGapUnderneight` /
    // `getGapOverneight` 기반. 1.21.1 은 boundingBox API 제약 + AABB 헬퍼 미이식 →
    // `canStandUp(player)` 근사로 대체. 핵심 의도 (가능하면 서기 = heightOffset 리셋 +
    // crawl/headJump 해제) 는 보존.
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 원본 L1681-L1686 `resetHeightOffset()`:
     *   sp.boundingBox.minY += heightOffset;
     *   sp.height -= heightOffset;
     *   heightOffset = 0F;
     *
     * **§7 근사** (B-N-standup-approx-1): 1.21.1 boundingBox/height 직접 조작 불가 →
     * `heightOffset = 0F` 만. 기존 이식에서 인라인으로 이 패턴 사용 중이던 것을 메서드로
     * 추출. B-N-standup (세션 115).
     */
    public void resetHeightOffset() {
        // 근사 이식 — 원본과 차이: boundingBox.minY/height 조작 생략 (1.21.1 API 제약)
        this.heightOffset = 0F;
    }

    /**
     * 원본 L2214-L2219 `standUp(double gapUnderneight)`:
     *   move(0, (1D - gapUnderneight), 0, true);
     *   isCrawling = false;
     *   isHeadJumping = false;
     *   resetHeightOffset();
     *
     * **§7 근사** (B-N-standup-approx-2): 원본 `move(0, 1D - gapUnderneight, 0, true)`
     * 이동 생략 — 1.21.1 AABB `getGapUnderneight` 미이식. `canStandUp` 기반 근사에서
     * 정확한 gap 알 수 없음. 핵심 상태 전환 (crawl/headJump 해제 + heightOffset 리셋) 만.
     */
    public void standUp() {
        // 근사 이식 — 원본과 차이: move(0, 1D - gapUnderneight, 0) 이동 생략
        this.isCrawling    = false;
        this.isHeadJumping = false;
        resetHeightOffset();
    }

    /**
     * 원본 L2165-L2183 `standupIfPossible()` 무인자 오버로드:
     *   if (heightOffset >= 0) return;
     *   groundClose = gapUnderneight < 1D;
     *   if (!groundClose) resetHeightOffset();
     *   else {
     *       standUpPossible = gapUnderneight + gapOverneight >= 1D;
     *       if (standUpPossible) standUp(gapUnderneight);
     *       else toSlidingOrCrawling(gapUnderneight);
     *   }
     *
     * **§7 근사** (B-N-standup-approx-3): AABB 정밀 gap 측정 미이식 → `canStandUp(player)`
     * 근사. `canStandUp` 은 플레이어 머리 위 공간 여부 → `standUpPossible` 판정에 대응.
     * `!groundClose` 분기 (공중) 는 별도 판별 없이 단순 `canStandUp` 기준으로 통합.
     * `toSlidingOrCrawling` 호출은 추후 B-N 원자에서 이식 (현재 슬라이딩/크롤 재시도 생략).
     */
    public void standupIfPossible(ClientPlayerEntity player) {
        if (this.heightOffset >= 0) return;

        // 근사 이식 — 원본과 차이: gapUnderneight/gapOverneight AABB 스캔 → canStandUp 근사
        if (canStandUp(player)) {
            standUp();
        } else {
            // toSlidingOrCrawling 호출 생략 — 현재는 heightOffset 만 유지 (슬라이딩/크롤 재시도
            // 경로는 후속 원자 범위). 근사 이식 — 원본과 차이: toSlidingOrCrawling 미호출.
        }
    }

    /**
     * 원본 L2186-L2212 `standupIfPossible(boolean tryLanding, boolean restoreFromFlying)` —
     * 비행 해제 포함 오버로드:
     *   if (heightOffset >= 0) return;
     *   if (tryLanding && groundClose && standUpPossible) {
     *       isFlying = false;
     *       sp.capabilities.isFlying = false;
     *       restoreFromFlying = true;
     *   }
     *   if (!restoreFromFlying) return;
     *   ... standUp or toSlidingOrCrawling ...
     *
     * B-24 세션 53 에서 `restoreFromFlying = true` 설정 후 이 메서드 호출해야 standupIfPossible
     * 연결 완결. 현재 1.21.1 은 B-24 에서 이 호출이 빠져있음 (미이식 명시).
     *
     * **§7 근사** (B-N-standup-approx-4): tryLanding 경로 내 `sp.capabilities.isFlying = false`
     * 는 1.21.1 `player.getAbilities().flying = false` 로 설정 시 vanilla 쪽 동기화 필요
     * (Mixin / 네트워크) — 1.21.1 에서는 direct 조작 불허. 근사로 상태 필드만 리셋.
     */
    public void standupIfPossible(ClientPlayerEntity player, boolean tryLanding, boolean restoreFromFlying) {
        if (this.heightOffset >= 0) return;

        boolean canStand = canStandUp(player);

        if (tryLanding && canStand) {
            this.isFlying = false;
            // 근사 이식 — 원본과 차이: sp.capabilities.isFlying = false → vanilla 비행 상태
            // 직접 조작은 client-server sync 필요. `player.getAbilities().flying = false` 는
            // 서버 검증 미반영으로 효과 불확실. 현재는 SM 측 isFlying 만 false.
            restoreFromFlying = true;
        }

        if (!restoreFromFlying) return;

        if (canStand) {
            standUp();
        } else {
            // toSlidingOrCrawling 호출 생략 — 동일 근사 (위)
        }
    }

    /**
     * 원본 SmartMovingSelf L2232-L2243 `private void handleCrash(float startDistance, float factor)`:
     *   if(sp.fallDistance >= 2.0F) sp.addStat(...);
     *   if(sp.fallDistance >= startDistance)
     *       sp.attackEntityFrom(DamageSource.fall, (int)Math.ceil((fallDistance - startDistance) * factor));
     * 호출처 (원본): L1081 handleClimbing 내부 + L2538 isHeadJumping 해제 엣지.
     * 1.21.1: Climber L489 에 private static 이식됨 (free climb 용). B-24 에서 ClientState 에도
     * 추가 — isHeadJumping 해제 엣지 용. 두 복사본 모두 원본 L2232-L2243 동치.
     * B-24 (세션 53).
     */
    public static void handleCrash(ClientPlayerEntity player, float startDistance, float factor) {
        if (player.fallDistance > startDistance) {
            float damage = (player.fallDistance - startDistance) * factor;
            player.damage(player.getDamageSources().fall(), damage);
        }
    }

    /**
     * 원본 SmartMovingSelf L3047-L3054 `private boolean toCrawling()`:
     *   isCrawling = true;
     *   if(Options.isCrawlToggleEnabled()) crawlToggled = true;
     *   ignoreNextStopSneakButtonPressed = true;
     *   return true;
     * `Options.isCrawlToggleEnabled() = _crawlToggle.value && enabled` (원본
     * SmartMovingOptions L465-L468) — AND 패턴.
     * 호출 지점 (원본): L2349 / L2566 / L2760 / L2767 / L2812 / L2835 / L2860 — 여러 전환 블록.
     * 1.21.1 현재는 IMPL-01 진입 시 1회만 호출. B-35/B-36 이식 시 다른 위치에서도 사용 예정.
     * B-40 (세션 44).
     */
    public boolean toCrawling() {
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        isCrawling = true;
        // B-45b (세션 45): Config 헬퍼 치환.
        if (cfg.isCrawlToggleEnabled()) crawlToggled = true;
        ignoreNextStopSneakButtonPressed = true;
        return true;
    }

    /**
     * 원본 SmartMovingSelf L1474-L1486 `private void resetClimbing()`:
     *   isClimbing = false;
     *   isHandsVineClimbing = false;
     *   isFeetVineClimbing = false;
     *   isVineOnlyClimbing = false;
     *   isVineAnyClimbing = false;
     *   isClimbingStill = false;
     *   isNeighborClimbing = false;
     *   actualHandsClimbType = HandsClimbing.NoGrab;
     *   actualFeetClimbType = FeetClimbing.NoStep;
     *   isCeilingClimbing = false;
     *
     * 호출 지점 (원본): L816 handleClimbing() 진입 직후 매 틱 호출 (Free Climb 외에
     * Standard/Simple/Smart Base Climb 공통). 1.21.1 에서는 SmartMovingClimber.handleClimbing
     * 진입부에서 호출. 원본 10 필드 모두 이식 완료 (B-15 세션 40).
     *
     * B-21 (isCeilingClimbing 해제 엣지) 는 이 리셋으로 자동 해소.
     * B-28 (handleClimbing 진입 isSliding=false) 는 원본 L985 에서 `wantClimbUp +
     * handsClimbing.IsRelevant()` 조건부 — 별도 원자 (B-19 Free Climb 분기 의존).
     * B-14 (세션 57).
     */
    public void resetClimbing() {
        isClimbing           = false;
        isHandsVineClimbing  = false;
        isFeetVineClimbing   = false;
        isVineOnlyClimbing   = false;
        isVineAnyClimbing    = false;
        isClimbingStill      = false;
        isNeighborClimbing   = false;
        actualHandsClimbType = HandsClimbing.NO_GRAB;
        actualFeetClimbType  = FeetClimbing.NO_STEP;
        isCeilingClimbing    = false;
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
     * - L911-L915 허기 패킷 전송 → **H-11 에서 메서드 말미에 1:1 추가**
     *   (ClientPlayNetworking.send(HungerChangePayload) + lastHungerIncrease 갱신)
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
        // 원본 L3241 isRunning() override = sp.isSprinting() && !isFast && (sp.onGround || vanilla()).
        // B-22c (세션 42): 기존 로컬 `!isFast && onGround` 공식 → ClientState.isRunning(player) 호출.
        //   vanilla() 항목 복원 — cfg.vanillaStyle=true 경로 반영 (기존 부정확 주석 제거).
        boolean isRunning = isRunning(player);

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

        // 원본 L911-L915 허기 변화 패킷 전송 — 변화 감지로 중복 전송 방지.
        //   if (hungerIncrease != lastHungerIncrease) {
        //       SmartMovingPacketStream.sendHungerChange(SmartMovingComm.instance, hungerIncrease);
        //       lastHungerIncrease = hungerIncrease;
        //   }
        //
        // ⚠️ H-16 (세션 22) 폭주 수정: 원본은 누적값 hungerIncrease 전체를 전송하지만,
        // 1.21.1 서버측 `MixinServerPlayerEntity.sm_afterTravel` 이 매 틱 `addExhaustion
        // (sm.hunger) + sm.hunger = 0F` 로 리셋하는 구조라 누적값을 그대로 보내면 매 틱
        // 전체 누적이 addExhaustion 에 적용됨 (5초 내 ~12 food 소비). 원본은
        // `withinOnLivingUpdate` 스킵 로직으로 해결. 1.21.1 는 해당 플래그 미이식이라
        // **delta 전송** (`hungerIncrease - lastHungerIncrease`) 로 동등 결과 달성.
        // 서버 수신자는 sm.hunger += payload.hunger() 누적 (H-16 동반 수정).
        // 완전한 원본 구조 복원은 `focus_11_server_hunger_sync.md` 후속에서 withinOnLivingUpdate
        // 이식 + 이 delta 우회 해제.
        float delta = hungerIncrease - lastHungerIncrease;
        if (delta != 0F) {
            if (ClientPlayNetworking.canSend(SmartMovingNetwork.HungerChangePayload.ID)) {
                ClientPlayNetworking.send(new SmartMovingNetwork.HungerChangePayload(delta));
            }
            lastHungerIncrease = hungerIncrease;
        }
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
            // **B-42-B39 해소 (세션 124)**: 원본 L1363-L1404 전면 AABB 정밀 이식.
            // B-42a/b/c 헬퍼 소비 → crawlStandUpBottom / crawlStandUpLiquidCeiling /
            // crawlStandUpCeiling 정확 계산.

            // 원본 L1369 setHeightOffset(-1F) — 1.21.1 boundingBox 미조작, 필드만 설정.
            heightOffset = -1F;

            double minY = player.getBoundingBox().minY;
            double maxY = player.getBoundingBox().maxY;
            double crawlStandUpBottom        = getMaxPlayerSolidBetween(player, minY - 1D, minY, 0);
            double crawlStandUpLiquidCeiling = getMinPlayerLiquidBetween(player, maxY, maxY + 1.1D);
            double crawlStandUpCeiling       = getMinPlayerSolidBetween(player, maxY, maxY + 1.1D, 0);

            // 원본 L1375 resetHeightOffset()
            heightOffset = 0F;

            float playerHeight = player.getHeight();
            if (crawlStandUpCeiling - crawlStandUpBottom < playerHeight) {
                // 분기 1 (L1377-L1383): 깊은 물 → 작은 구멍 크롤
                isCrawling   = true;
                isDipping    = false;
                heightOffset = -1F;
            } else if (crawlStandUpLiquidCeiling - crawlStandUpBottom < playerHeight) {
                // 분기 2 (L1384-L1390): 깊은 물 → 물 아래 크롤
                isCrawling           = true;
                contextContinueCrawl = true;
                isDipping            = false;
                heightOffset         = -1F;
            } else if (crawlStandUpBottom > minY) {
                // 분기 3 (L1392-L1403): 깊은 물 → 걷기/크롤
                if (isSlow && crawlStandUpBottom > minY + 0.5D) {
                    // 분기 3a: isSlow + 높은 바닥 → 크롤
                    isCrawling   = true;
                    isDipping    = false;
                    heightOffset = -1F;
                }
                // 원본 L1402: move(0, crawlStandUpBottom - minY, 0, true)
                player.move(MovementType.SELF, new Vec3d(0, crawlStandUpBottom - minY, 0));
            }
        }
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
        // B-10d (세션 71): isLevitating 강제 false 제거. 원본 L505 `isLevitating = diving &&
        //   !diveUp && !diveDown && moveStrafe==0 && moveForward==0` (수중 정적 자세) 는
        //   updateSwimState (세션 71) 에서 이미 갱신됨. 로프 블록은 1.21.1 미구현이나 원본
        //   isLevitating 의 주요 용도 (L505) 는 수중 정적 조건이라 유효.

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
