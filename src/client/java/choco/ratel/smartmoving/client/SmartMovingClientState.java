package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.climbing.FeetClimbing;
import choco.ratel.smartmoving.climbing.HandsClimbing;
import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import choco.ratel.smartmoving.network.SmartMovingNetwork;
import choco.ratel.smartmoving.network.SmartMovingState;
import choco.ratel.smartmoving.stat.SmartStatistics;
import net.minecraft.stat.Stats;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket;
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

    // 🔴 (Phase 3, Crawl 1:1 fix): 원본 SmartMovingContext L43-L44 의 SwimCrawl 상수 정의.
    //   1.21.1 에 별도 SmartMovingContext 파일 없음 → SmartMovingClientState 에 static 정의.
    /** 원본 SwimCrawlWaterTopBorder = 0.65F. canCrawl/mustCrawl 의 dippingDepth 검사에 사용. */
    public static final float SWIM_CRAWL_WATER_TOP_BORDER = 0.65F;
    /** 원본 SwimCrawlWaterMediumBorder = 0.6F. B-36 (b) dipping → swim/dive 전환 임계. */
    public static final float SWIM_CRAWL_WATER_MEDIUM_BORDER = 0.6F;
    /** 원본 SwimCrawlWaterBottomBorder = 0.55F. B-36 (c) dipping → 얕은 물 crawl 전환 임계. */
    public static final float SWIM_CRAWL_WATER_BOTTOM_BORDER = 0.55F;
    /**
     * 원본 SmartMovingContext L51 SlideToHeadJumpingFallDistance = 0.05F.
     * 슬라이딩 중 낙하거리 임계값 — 초과 시 isSliding=false + isHeadJumping=true + isAerodynamic=true.
     * (헤드점프 Phase A-4 — 원본 SmartMovingSelf L2546)
     */
    public static final float SLIDE_TO_HEADJUMPING_FALL_DISTANCE = 0.05F;

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
     * 🔴 fix #19 (2026-05-08): 헤드점프 종료 → 슬라이딩 진입 매핑.
     *   handleCrash 분기 진입 시 1 tick 만 true.
     *   standupIfPossible 의 setPos(y+1) 분기 가드에 사용 → 헤드점프 종료 직후 1 tick 만
     *   setPos 호출 (비행/Levitate 종료와 동일 효과). 다음 tick reset 으로 무한 setPos 차단
     *   (이전 fix #16 무한 루프 BUG 회피).
     *   다음 tick 의 L1854 tryLanding 분기 standupIfPossible 호출 시 미매치 → setPos X →
     *   vanilla 자동 처리.
     */
    public boolean justEndedHeadJump;

    /**
     * 🔴 fix #21 (2026-05-08): SlideToHeadJumping 자동 전환 cooldown.
     *   슬라이딩 → 절벽 → 자동 전환 → 헤드점프 종료 → setPos(y+1) → 사용자 환경 (다층 절벽)
     *   에서 박스 ground+1m 공중 → 다음 tick fallDistance 누적 → 자동 전환 재발 → 무한 루프.
     *   첫 자동 전환은 OK (사용자 의도). 종료 후 N tick 동안 자동 전환 차단.
     *   handleCrash 분기에서 set (5 tick). tickEssential 시작 시 카운터 감소.
     */
    public int slideToHeadCooldown;

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
     * **포커스 #3 B-5 (세션 5 검증)**: true 설정 경로 (fromSwimmingOrDiving 분기 2 L2800)
     *   이미 이식 완료 (B-42-B39 세션 124). 원본 L1388 `contextContinueCrawl = true` 1:1.
     *   stale 주석 정정.
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

    /**
     * 사다리/덩굴 ICC EXIT toCrawling 직후 isCrawlClimbing 식 차단 플래그.
     *
     * 우리 1.21.1 매핑: ICC EXIT 후 박스 발 = ladder maxY 부근 (= grip 영역 안).
     *   → handleClimbing 결과 isClimbing=true 잔존 → L1970 isCrawlClimbing 식 매치
     *   (wasCrawling=true && isClimbing=true && ...) → L1985 _canStandUp=false (= ladder solid)
     *   → L1989 분기 진입 → L1992 isCrawling=false reset BUG.
     *
     * 원본 1.7.10: 박스 발 = posY+1 = ladder maxY+1 (= 빈공간) → grip X → isClimbing=false 자연
     *   → isCrawlClimbing 식 미매치.
     *
     * 해결: ICC EXIT toCrawling 시 플래그 set → 다음 tick L1970 식 false 강제 → L1992 reset 회피.
     * isCrawling=false 자연 시 reset (= 사용자 sneak release).
     */
    public boolean iccExitJustToCrawl;


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
    /** grab 키 직전 프레임 isPressed() 값 — rising edge 검출용 (timesPressed 카운터 누적 회피). */
    public boolean prevGrabKeyPressed;

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

    /**
     * 🔴 사다리/덩굴 등반 중 sneak 키가 눌렸으면 set. sneak 키 뗄 때 reset.
     *   원본 1.7.10 의 isSneaking() override 는 isClimbing=true 시 false 리턴 →
     *   1.21.1 vanilla CROUCHING 자세도 차단. 그러나 등반 끝난 직후 (isClimbing=false)
     *   에는 원본도 isSneaking()=true → sneak 자세. 1.7.10 vanilla sneak 자세는
     *   시각적으로 미미 → 사용자 인식 X. 1.21.1 vanilla CROUCHING 은 매우 두드러짐
     *   → 사용자 보고 "엎드리기".
     *   사용자 의도 (원본보다 엄격) 반영: 등반 중 sneak 누르고 있으면 등반 끝나도
     *   sneak 키를 한 번 떼야 sneak 효과 활성화. 떼면 자연 reset.
     */
    public boolean sneakHeldDuringClimb;
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

    /**
     * 원본 SmartRender bipedOuter.rotateAngleX (라디안) 등가 — sm_setupTransforms 의 X tilt
     * (isSwim/isDive/isSlide/isFlying/isHeadJumping 5 분기 마지막 활성 값).
     * 사용처: B-17 — MixinCapeFeatureRenderer 의 망토 X 회전 클램프
     *   `localAngleMax = max(70.523° - smOuterTiltX_deg, 6°)` 적용 (원본
     *   ModelCapeRenderer.java L72-L73).
     * 갱신: sm_setupTransforms 진입 시 0 reset → 5 분기에서 = 으로 할당.
     * 한 분기만 활성 (SM 11-state if-else 우선순위).
     */
    public float smOuterTiltX = 0f;

    // ── 🔴 fade 보간 캐시 (Flying Phase / 세션 49): 원본 ModelRotationRenderer 의
    //   fadeStore/fadeIntermediate 시스템 매핑. 매 render 프레임 prev → target 사이 점진적
    //   보간 (factor = (currentTime - prevTime) * 0.2F). 비행 시 bipedOuter.X / Y 회전 보간.
    //   원본 SmartMovingModel L484: bipedOuter.fadeRotateAngleX = true (X 회전 fade 활성).
    //   원본 SmartRenderModel L209: bipedOuter.fadeRotateAngleY = true (기본, Pig 안 타면).
    public float smOuterTiltX_prev = 0f;       // 이전 프레임 보간 결과 (X 회전)
    public float smOuterExtraYaw = 0f;         // 현재 프레임 Y 회전 target (smFlyingExtraYaw)
    public float smOuterExtraYaw_prev = 0f;    // 이전 프레임 보간 결과 (Y 회전)
    public float smOuterFade_prevTime = -999f; // 이전 프레임 totalTime (-999 = 미초기화)

    // 🔴 헤드점프 전용 fade prev (2026-05-11, 사용자 보고 "하강 시 몸 너무 수직"):
    //   원본 SmartMovingModel L507 bipedOuter.fadeRotateAngleX = true (X 회전 5-tick lerp).
    //   비행 fade prev 와 분리 — 비행 ↔ 헤드점프 직접 전환 시 prev 값 leak 차단.
    //   feedback_headjump_overground_air_is_solid.md 와 같은 시리즈 fix.
    public float smHeadJumpTiltX_prev = 0f;       // 이전 프레임 헤드점프 X 회전 lerp 결과
    public float smHeadJumpFade_prevTime = -999f; // 이전 프레임 totalTime (-999 = 미초기화)

    // 🔵 (2026-05-20, BUG-Swim-Anim-1/2 fix #115) swim/dive 전용 fade prev.
    //   원본 SmartMovingModel L331 (isSwim) + L373 (isDive): `bipedOuter.fadeRotateAngleX = true`.
    //   원본 ModelRotationRenderer.fadeIntermediate L315-L345: 매 frame `current = prev +
    //     (target - prev) * deltaTime * 0.2F`. 진입/전환 시 점진 lerp.
    //   비행/헤드점프 fade prev 와 분리 — 비행/헤드점프 ↔ swim/dive 전환 시 prev 값 leak 차단.
    //   사용자 보고: "수면 도달 시 갑자기 1자" (= dive tilt 즉시 0 변환) + "wasd 시 팔다리 이상"
    //     (= currentVerticalAngle 동적 변화 즉시 적용) 모두 fade 부재 cause.
    public float smSwimDiveTiltX_prev = 0f;       // 이전 프레임 swim/dive X 회전 lerp 결과
    public float smSwimDiveFade_prevTime = -999f; // 이전 프레임 totalTime (-999 = 미초기화)

    // 🔵 (2026-05-21, fix #118) swim/dive Y 회전 fade — 마우스 회전 lag 매핑.
    //   원본 SmartMovingModel L333/L375 `bipedOuter.rotateAngleY = horizontalAngle` 가 fadeRotateAngleY=true
    //   로 매 frame lerp (= 0.2 * deltaT). 우리 매핑 = sm_setupTransforms TAIL 의 swim/dive 분기 Y 회전
    //   추가 + lerpFadeAngle. isFlying 분기 (smFlyingExtraYaw/smOuterExtraYaw_prev) 동일 패턴.
    public float smSwimDiveExtraYaw_target = 0f;  // 현재 프레임 Y 회전 target (= horizontalAngle, rad)
    public float smSwimDiveExtraYaw_prev = 0f;    // 이전 프레임 보간 결과

    // 🟡 DEBUG (2026-05-21) jump 꾹누름 헤엄 시 팔 회전 BUG 진단용 카운터.
    //   sm_animateDiving 매 5 frame, sm_setupTransforms tick 단위 dump 위한 throttle field.
    public int smDbgFrameCounterArm = 0;   // sm_animateDiving frame counter (5 frame 마다 dump)
    public int smDbgJumpHeadTick = -1;     // sm_setupTransforms tick 1회 dump 보장 (last dumped tick)


    // 🔴 fix #91 (2026-05-13, 사용자 보고 "헤드점프 + 벽 박을 때 몸 회전 중간 이어짐 없음"):
    //   원본 SmartRenderModel L208-209: bipedOuter.fadeRotateAngleY = true (= EntityPig 외).
    //   isHeadJump 분기 (SmartMovingModel L509): bipedOuter.rotateAngleY = currentHorizontalAngle.
    //   fadeIntermediate 가 자연 fade lerp (0.2 factor) 적용 → 벽 박은 후 vx 반전 시
    //   currentHorizontalAngle 매 frame 변화 → 모델 자연 회전 추적.
    //   우리 1.21.1 매핑 누락: isHeadJumping 분기 (MixinPlayerEntityRenderer L342-347) 가
    //   smBodyYawOverride = currentHorizontalAngle 직접 set → instant 회전.
    //   해결: isCeilingClimbing fade 패턴 차용 — smHeadJumpYaw_prev field + lerpFadeAngle.
    public float smHeadJumpYaw_prev = 0f;            // 이전 프레임 헤드점프 Y 회전 (라디안) lerp 결과
    public float smHeadJumpYawFade_prevTime = -999f; // 이전 프레임 totalTime (-999 = 미초기화)

    // 🔴 fix #95 (2026-05-13): 여우무빙 발사 frame Y 회전 snap done flag.
    //   fix #94 의 wasSelfSlideFire 가드가 매 frame prev=target 강제 → 진행 중 fade 사라짐.
    //   해결: 발사 frame 만 prev=target snap + flag=true. 그 후 자연 fade 진행 (= 벽 박을 때
    //   currentHorizontalAngle 변화 시 fix #91 패턴 자연 lerp).
    //   reset: 외 분기 진입 시 (= !isHeadJumping && !isRopeSliding) false 로.
    public boolean smHeadJumpYawSnapDone = false;

    // 🔴 fix #81 (2026-05-12): 자체 슬라이딩 발사 → 자동 cycle 진입 시 시각 수평 강제 플래그.
    //   자체 슬라이딩 발사 분기 (L2055+) 에서 true set. 헤드점프 종료 시 (isHeadJumping false 전환) reset.
    //   setupTransforms 헤드점프 분기에서 검사 → thetaTarget=π/2 (= Quarter) 고정 강제.
    //   원본 isSlide 분기 (= rotateAngleX = Quarter 즉시 적용) 시각 1:1.
    public boolean wasSelfSlideFire = false;

    // 🔵 (2026-05-14, fix #98 위치 기반) REMOTE 측 헤드점프 종료 자세 visual hold flag (위치 기반).
    //   사용자 보고 (3번 BUG): "리모트 콜리전이 땅에 닿기 전에 헤드점프 끝나버림".
    //   원인: vanilla lerpPosAndRotation 가 REMOTE.y 를 분할 보간 → SmartMovingState packet 의
    //     isHJ=false 도착 시점에 REMOTE.y > groundY (= 공중) 상태. 자세 즉시 standing → 공중에
    //     standing 자세 → 사용자 시각 부자연.
    //   메모리 패턴 정통 (사용자 지시 — 메모리 정독 후 적용):
    //     - feedback_remote_phase_fix_ground_basis: ground top 직접 측정 (getMaxPlayerSolidBetween).
    //     - project_remote_landing_sink_pattern: fix #92 = 잠긴 상태 (= ground 아래) 만 setPos.
    //     - 공중 상태 (= ground 위) 시 = 자세 visual hold + 위치 자연 lerp.
    //   fix: packet 처리 시 isHJ=true → false + 공중 상태 (= curY > groundY+0.005) 검출 시
    //     hold=true + groundY 저장. 자세 visual hold 적용. hold 해제 = 위치 기반 (= 매 tick
    //     REMOTE.y <= groundY+0.005 도달 시 hold=false). 사용자 원칙 "틱기반 답없다" 일치.
    //   적용 위치: MixinPlayerEntityRenderer 의 X/Y 회전 분기 가드 OR 추가, reset 가드 !hold AND 추가.
    //   해제 위치: MixinPlayerEntityClient.sm_tickStatsForRemote TAIL 의 위치 비교.
    public boolean smRemoteHJVisualHold = false;
    public double smRemoteHJExitGroundY = 0.0;

    // 🔵 (2026-05-14, Option F fix #97) StatsPayload 동기화 — self → REMOTE.
    //   self 의 sm.stats.currentVerticalAngle/HorizontalAngle 매 tick packet 으로 송신 → REMOTE 가
    //     자체 stats.calculate 결과를 덮어쓰기.
    //   원인: vanilla lerpPosAndRotation 가 REMOTE 의 위치를 5-tick 균등 분할 보간 → realDxYz 가
    //     첫 4 tick 거의 0 + 마지막 1 tick spike → atan2 결과 thetaT 가 self trajectory 와 다른
    //     흐름 → lerpFadeAngle 0.2 lerp 가 peak 추격 시간 부족 → REMOTE 헤드점프 마지막 회전 부족.
    //   초기값 NaN — packet 받기 전 vanilla 자체 계산 사용 (= 회귀 차단).
    //   set 위치: SmartMovingClient.java StatsPayload receiver (= packet 도착 시).
    //   적용 위치: MixinPlayerEntityClient.sm_tickStatsForRemote TAIL (= stats.calculate 후).
    public float smRemoteStatsVerticalAngle_fromPacket = Float.NaN;
    public float smRemoteStatsHorizontalAngle_fromPacket = Float.NaN;

    /**
     * 🔴 (2026-04-27) 낙하/기본 상태 fade lag — 비행 fade 패턴 그대로 차용 (head 만 vanilla).
     * 비행과 별개 prev 필드 (서로 다른 시점 활성, 같은 필드 공유 시 분기 전환 시 lerp 부정확).
     */
    public float smStandardFadeYaw_prev = 0f;
    public float smStandardFadeYaw_prevTime = -999f;

    /**
     * 🔴 D-4 종료 엣지 cleanup 추적 (사용자 보고 — 등반 종료 후 standing 시 몸통 분리 잔존):
     *   D-4 분기 (NoGrab+non-NoStep) 진입 시 모든 노드 pivotZ=-6 set. 등반 종료 시 sm_animateClimbing
     *   호출 안 됨 → reset 안 됨. reset 인프라 (sm_setAngles TAIL) 는 anySmState 활성 시만 진입 →
     *   종료 엣지 frame 에 anySmState=false 면 진입 안 함 → pivotZ 잔존.
     *   이전 frame 의 climbing 상태 추적 → 종료 엣지 (true → false) 한 번 모든 pivotZ=0 reset.
     */
    public boolean smWasClimbingForCleanup = false;

    /**
     * 🔴 isCrawling 종료 엣지 cleanup 추적 (사용자 보고 — 엎드림 해제 시 자세 잔존, 2026-05-03):
     *   sm_animateCrawling 가 head.pivotZ=-2, head.roll, body.yaw/roll, leg.roll, arm.yaw/roll, scales 등
     *   set. vanilla setAngles 는 이 값들 자동 reset 안 함 (= pivotZ/yaw/roll/scales 비reset).
     *   isCrawling 종료 후 sm_animateCrawling 미호출 → 잔존 값으로 자세 고정 BUG.
     *   해결: D-4 cleanup 패턴 따라 종료 엣지 (true → false) 한 번 모든 변경값 vanilla default reset.
     */
    public boolean smWasCrawlingForCleanup = false;

    /**
     * 🔴 천장 등반 bodyYaw fade 보간 (사용자 보고 — 마우스 회전 시 몸통 회전 보간 원본과 다름):
     *   원본 SmartRenderModel L209: bipedOuter.fadeRotateAngleY = true (기본).
     *   원본 SmartMovingModel L310: bipedOuter.rotateAngleY = rotateY + horizontalAngle.
     *   ModelRotationRenderer.fadeIntermediate 가 매 frame `prev + (target - prev) * deltaTime * 0.2F`
     *   lerp 적용 → 마우스 회전 시 몸통 부드럽게 추적.
     *   1.21.1 매핑: sm_captureBodyYaw 의 isCeilingClimbing 분기에 lerpFadeAngle 적용.
     *   prev = 이전 frame lerped 라디안, prevTime = animationProgress (tick 단위).
     */
    public float smCeilingYaw_prev = 0f;
    public float smCeilingFade_prevTime = -999f;

    /**
     * 🔴 (세션 52b): partial tick 캐시 — setupTransforms 에서 저장 → setAngles 에서 사용.
     * Mixin static field 제약 (private 만 허용) 우회 — SmartMovingClientState 에 저장.
     * 원본 SmartRenderRender.renderPlayer L56-L57: getCurrentSpeed/getTotalDistance(renderPartialTicks)
     * lerped 매핑용.
     */
    public static float globalCachedTickDelta = 0f;

    /**
     * 🔴 (2026-04-27) 낙하/기본 상태 body fade 매핑 — 원본 SmartRenderModel L208/247
     *   `bipedOuter.fadeIntermediate(totalTime)` 1:1 (factor 0.2 lag).
     * 머리는 vanilla 그대로 (원본도 head 처리는 vanilla 동등 — actualRotation + netHeadYaw =
     *   headYaw_raw absolute). 여기서는 setupTransforms bodyYaw 인자만 fade 적용.
     * MixinPlayerEntityRenderer.sm_modifyBodyYaw 가 fade 계산 + 갱신.
     */
    // 🔴 (Phase 2 fix-3-1) static → instance: player 별 fade prev 분리.
    //   원본 1.7.10 single player 환경에서는 static 으로 충분했으나, multiplayer 시
    //   두 player 동시 SM 자세 시 마지막 entity 값으로 mixed → fade jitter (BUG-C 보조).
    public float smCachedAnimationProgress = 0f;
    public float smStandardBodyYawPrev = Float.NaN;
    public float smStandardFadeTimePrev = Float.NaN;

    /**
     * 🔴 (2026-05-03) isCrawl 전용 flag — body fade lag 활성화 + head 보정 skip.
     *
     * 원본 SmartRender bipedOuter.fadeRotateAngleY = true → bodyYaw 0.2 lerp 적용 (= 부드러움).
     * 단 isCrawl 의 head 매핑 (head.rotateAngleZ = -netHeadYaw / RadToAngle) 은 vanilla netHeadYaw
     * (= bodyYaw_natural 기준) 사용해야 max 50° clamp 가 head.roll 에 그대로 반영됨.
     * sm_modifyNetHeadYaw 의 head 보정 (netHeadYaw + bodyYaw_diff) 은 isCrawl 에서 max 깨짐 →
     * smCrawlMode=true 시 보정 skip.
     */
    public boolean smCrawlMode = false;

    /**
     * 🔴 (2026-05-04) crawl-climbing 의 bodyAngleX fade lerp 보간용 prev field.
     *   원본 ModelRotationRenderer.GetIntermediateAngle 식 (= prev + (target - prev) * deltaT * 0.2F)
     *   적용. height 변화 (= smallOverGroundHeight 매 tick 갱신) 의 시각 부드러움 추가.
     */
    public float smCrawlClimbBodyAngleXFaded = Float.NaN;
    public float smCrawlClimbFadeTimePrev = Float.NaN;

    // 🔴 (2026-05-04) 사용자 보고 fix — "다리가 살짝 땅으로 잠김":
    //   bodyAngleX 만 fade 시 legAngleX = QUARTER - bodyAngleX 식 → bodyAngleX=0 시작 시
    //   legAngleX = π/4 = 큰 앞쪽 회전 → leg vertex 박스 침투.
    //   fix: legAngleX/legAngleZ 도 별도 fade 적용 → prev=0 시작 → 점진적 target 도달.
    public float smCrawlClimbLegAngleXFaded = Float.NaN;
    public float smCrawlClimbLegAngleXFadeTimePrev = Float.NaN;
    public float smCrawlClimbLegAngleZFaded = Float.NaN;
    public float smCrawlClimbLegAngleZFadeTimePrev = Float.NaN;

    /**
     * 🔴 (Phase 2 multi BUG-11) remote 측 비행 종료 edge 감지용. self 측은 자체 처리 (standUp setPos), remote 만.
     *   비행 종료 시 self standUp setPos(y+1) → server broadcast → remote 측 entity.y server-sync.
     *   단 server-relay SM packet (= sm.isFlying=false) 와 vanilla EntityPositionS2CPacket timing 차이로
     *   sm.isFlying=false sync 후 entity.y server-sync 까지 lag → 그 사이 lerp 진행 → 모델 땅 아래.
     *   해결: 비행 종료 후 N tick 동안 매 tick 큰 dy 감지 → lastRenderY/prevY 즉시 동기화.
     */
    public boolean smPrevWasFlyingForLerpFix = false;
    public int smFlyingExitYSyncTicks = 0;


    /**
     * crawl-climbing 의 bodyAngleX fade lerp helper.
     * setupTransforms 에서 호출 → faded 값 저장 + 반환. setAngles 가 같은 frame 에서 read.
     */
    /** isCrawling setupTransforms 의 부모 R_x 회전값 (= π/2 - π/16). isCrawlClimbing fade 시작값. */
    public static final float CRAWL_TILT_ANGLE = (float) (Math.PI / 2 - Math.PI / 16);

    public float applyCrawlClimbFade(float target, float curTime) {
        float prev = smCrawlClimbBodyAngleXFaded;
        float prevTime = smCrawlClimbFadeTimePrev;
        // 🔴 fade 보간 (2026-05-04 — 사용자 보고 fix "다리 땅 침투"):
        //   진입 직전 = isCrawling 자세 (= setupTransforms R_x(-CRAWL_TILT_ANGLE) ≈ 78.75°).
        //   prev=0 시작 시 = R_x(0) = 직선 → isCrawling R_x(-tilt) 와 ~78° 차이 → leg vertex 박스 침투.
        //   fix: prev = CRAWL_TILT_ANGLE 시작 → 진입 시 isCrawling 자세와 동일 시작 → 점진적 target.
        //   isCrawlClimbing 진입 = 항상 isCrawling 상태에서 가능 (= ICC = crawl + climbing).
        if (Float.isNaN(prev) || Float.isNaN(prevTime)) {
            smCrawlClimbBodyAngleXFaded = CRAWL_TILT_ANGLE;
            smCrawlClimbFadeTimePrev = curTime;
            return CRAWL_TILT_ANGLE;
        }
        float deltaT = curTime - prevTime;
        if (deltaT <= 0F || deltaT > 2F) {
            // timeout/역방향 → prev 유지하고 그대로 반환 (= jump 차단).
            smCrawlClimbFadeTimePrev = curTime;
            return prev;
        }
        float faded = prev + (target - prev) * deltaT * 0.2F;
        smCrawlClimbBodyAngleXFaded = faded;
        smCrawlClimbFadeTimePrev = curTime;
        return faded;
    }

    public float applyCrawlClimbLegAngleXFade(float target, float curTime) {
        float prev = smCrawlClimbLegAngleXFaded;
        float prevTime = smCrawlClimbLegAngleXFadeTimePrev;
        if (Float.isNaN(prev) || Float.isNaN(prevTime)) {
            smCrawlClimbLegAngleXFaded = 0f;
            smCrawlClimbLegAngleXFadeTimePrev = curTime;
            return 0f;
        }
        float deltaT = curTime - prevTime;
        if (deltaT <= 0F || deltaT > 2F) {
            smCrawlClimbLegAngleXFadeTimePrev = curTime;
            return prev;
        }
        float faded = prev + (target - prev) * deltaT * 0.2F;
        smCrawlClimbLegAngleXFaded = faded;
        smCrawlClimbLegAngleXFadeTimePrev = curTime;
        return faded;
    }

    public float applyCrawlClimbLegAngleZFade(float target, float curTime) {
        float prev = smCrawlClimbLegAngleZFaded;
        float prevTime = smCrawlClimbLegAngleZFadeTimePrev;
        if (Float.isNaN(prev) || Float.isNaN(prevTime)) {
            smCrawlClimbLegAngleZFaded = 0f;
            smCrawlClimbLegAngleZFadeTimePrev = curTime;
            return 0f;
        }
        float deltaT = curTime - prevTime;
        if (deltaT <= 0F || deltaT > 2F) {
            smCrawlClimbLegAngleZFadeTimePrev = curTime;
            return prev;
        }
        float faded = prev + (target - prev) * deltaT * 0.2F;
        smCrawlClimbLegAngleZFaded = faded;
        smCrawlClimbLegAngleZFadeTimePrev = curTime;
        return faded;
    }
    /**
     * sm_captureBodyYaw 가 비행/SM force 분기 활성 시 true 로 set.
     * MixinPlayerEntityModelClient.sm_setAngles 가 body.yaw fade adjustment skip 위해 사용.
     */
    public boolean smBodyYawActive_publicShared = false;
    /**
     * 🔴 (Phase 2 fix-3-1) 기존 MixinPlayerEntityRenderer 의 mixin static 2 개
     *   (smBodyYawActive, smBodyYawOverride) 를 player 별 instance 로 이동.
     *   sm_captureBodyYaw 가 set, sm_modifyBodyYaw (ModifyArg) 가 read.
     */
    public boolean smBodyYawActive = false;
    public float smBodyYawOverride = 0f;
    /** BUG-27/32: 비행 시 추가 Y 회전 (horizontalAngle - lerpedYaw, 라디안). 0 = 추가 회전 없음. */
    public float smFlyingExtraYaw = 0f;
    /**
     * 🔴 (2026-04-27) 낙하/기본 상태 fade lag 활성 플래그.
     * sm_captureBodyYaw 가 set, sm_setupTransforms TAIL 가 fade 적용 가드용.
     */
    public boolean smStandardFadeActive = false;
    /**
     * 🔴 (2026-04-27) 낙하 시 비행 패턴(머리/몸 같이 fade lag) 활성 플래그.
     * smStandardFadeActive=true 의 sub-mode — 활성 시:
     *   - sm_animateFalling 가 head.yaw=0 force (머리/몸 같이 회전).
     *   - sm_modifyNetHeadYaw 가 head 보정 skip (vanilla netHeadYaw 그대로 → fade matrix 영향 받음).
     * false (기본 상태) 시: 머리는 vanilla 동작 유지 (sm_modifyNetHeadYaw 가 보정 적용).
     */
    public boolean smFallingFadeMode = false;
    /**
     * 🔴 (2026-04-27) sm_setupTransforms TAIL 가 set, sm_modifyNetHeadYaw 가 head 보정에 사용.
     */
    public float smCachedYawLerpedRad = 0f;
    /**
     * 🔴 (2026-04-27) sm_modifyBodyYaw 의 input (vanilla 1 frame lerped bodyYaw, degrees).
     * sm_modifyNetHeadYaw 의 head 보정식에 사용.
     */
    public float smCachedBodyYawNaturalDeg = 0f;
    /**
     * 🔴 (2026-04-27) sm_modifyBodyYaw 의 output (fade lerp 적용 후 lagged bodyYaw, degrees).
     * sm_modifyNetHeadYaw 의 head 보정식에 사용 (lagged - natural 차이로 head world yaw 보정).
     */
    public float smCachedBodyYawLaggedDeg = 0f;

    /**
     * 원본 ModelRotationRenderer.GetIntermediateAngle (L347-365) 1:1 매핑 (degrees 단위).
     *   prev + (target - prev) * deltaT * 0.2F  (factor 0.2 lag).
     *   ±180° wrap 처리 (짧은 방향 보간).
     *   deltaT > 2F (또는 NaN/<=0) 가드 → 즉시 (lerp 없음).
     *
     * 호출처: MixinPlayerEntityModelClient.sm_setAngles — body.yaw 에 fade adjustment.
     *           머리는 vanilla 그대로 (사용자 "원본도 머리는 vanilla").
     *           setupTransforms bodyYaw 인자 그대로 → head/arm/leg 부모 변환 영향 없음.
     */
    public float applyFadeAngleDegrees(float target) {
        float curTime = smCachedAnimationProgress;
        float prev = smStandardBodyYawPrev;
        float prevTime = smStandardFadeTimePrev;

        // 첫 호출 또는 timeout (>2 ticks): 즉시 적용 + prev 갱신.
        if (Float.isNaN(prev) || Float.isNaN(prevTime)) {
            smStandardBodyYawPrev = target;
            smStandardFadeTimePrev = curTime;
            return target;
        }
        float deltaT = curTime - prevTime;
        if (deltaT <= 0F || deltaT > 2F) {
            smStandardBodyYawPrev = target;
            smStandardFadeTimePrev = curTime;
            return target;
        }

        // ±180° wrap (원본 L352-362, Whole=360, Half=180).
        float p = prev, t = target;
        while (p >= 360F) p -= 360F;
        while (p < 0F)    p += 360F;
        while (t >= 360F) t -= 360F;
        while (t < 0F)    t += 360F;
        if (t > p && t - p > 180F) p += 360F;
        if (t < p && p - t > 180F) t += 360F;

        // 원본 L364: prev + (target - prev) * deltaT * 0.2F.
        float faded = p + (t - p) * deltaT * 0.2F;
        smStandardBodyYawPrev = faded;
        smStandardFadeTimePrev = curTime;
        return faded;
    }

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

    /**
     * 🔴 (Phase 2 fix-3-1) 현재 render 중인 player cursor.
     *   ModifyArg 등 entity 인자 없는 mixin point 에서 player 별 instance 접근 위해 사용.
     *   MixinLivingEntityRenderer.render HEAD 에서 set, TAIL 에서 clear. render 직렬 처리이므로
     *   매 시점에 current render entity 한 명만 가리킴.
     */
    public static net.minecraft.client.network.AbstractClientPlayerEntity currentRenderTarget;

    /** currentRenderTarget 의 SmartMovingClientState instance. null 일 시 null 반환. */
    public static SmartMovingClientState currentRenderTargetSm() {
        return currentRenderTarget == null ? null : get(currentRenderTarget);
    }

    private static final Map<UUID, SmartMovingClientState> INSTANCES = new HashMap<>();

    public static SmartMovingClientState get(ClientPlayerEntity player) {
        return INSTANCES.computeIfAbsent(player.getUuid(), id -> new SmartMovingClientState());
    }

    /** 🔴 Phase 1-A: 다른 player render 시 SM state 조회 — local + remote 모두 처리. */
    public static SmartMovingClientState get(net.minecraft.client.network.AbstractClientPlayerEntity player) {
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
        // 🔴 (Phase 2 multi BUG-7) bit 34 isClimbCrawling 갱신 추가.
        //   server-side 가 bit 34 broadcast — self 측 박스 +1m offset 동기화용 (메모리
        //   project_isclimbcrawling_complete.md). remote 측 client-side 도 isClimbCrawling 종료
        //   edge 감지 위해 갱신 필수 — sm_handleRemoteIccCrawlExitYSync inject 가 검사.
        isClimbCrawling   = ((bits >> 34) & 1) != 0;
        // 🔴 fix #96 (2026-05-13): bit 35 wasSelfSlideFire 갱신 — remote 측 여우무빙 시각 fix
        //   #81/#94/#95 (= X 회전 즉시 π/2 + Y 회전 즉시 이동 방향) 적용 조건 동기화.
        //   self 측 tickEssential L2144 에서 자체슬라이딩 발사 시 set / 슬라이딩 phase 진입 시 false.
        //   remote 측은 packet 으로만 갱신 (= tickEssential 호출 안 됨).
        wasSelfSlideFire  = ((bits >> 35) & 1) != 0;
    }

    // ── 4-2: tickEssential() ─────────────────────────────────────────

    /**
     * isActive 여부 무관하게 매 틱 실행되는 필수 처리.
     * 원본: SmartMovingPlayerBase.updateEntityActionState() → moving.tickEssential()
     */
    public void tickEssential(ClientPlayerEntity player) {
        // 이전 틱 값 초기화 — vanilla jump() 가로채기(sm_jump)에서 당 틱에 새로 설정됨
        jumpAvoided = false;
        // 🔴 (세션 55): isJumping 매 틱 false reset (원본 SmartMovingSelf L1744 1:1).
        //   원본: tryJump 시 true (L2132), updateEntityActionState 시작 시 false reset.
        //   tryJump 안에서만 true → 한 틱만 유지 (다음 틱 시작 시 reset).
        isJumping = false;
        // 🔴 fix #19 (2026-05-08): justEndedHeadJump 1-tick 플래그 reset.
        //   handleCrash 분기 진입 시 set, 다음 tick 시작 시 reset → setPos(y+1) 1 tick 한정.
        this.justEndedHeadJump = false;
        // 🔴 fix #21 (2026-05-08): slideToHeadCooldown 카운터 감소.
        //   handleCrash 분기 set 후 N tick 동안 자동 전환 차단.
        if (this.slideToHeadCooldown > 0) this.slideToHeadCooldown--;

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
        // 🔴 (세션 145 BUG-1+2 진짜 원인): `KeyBinding.wasPressed()` 의 timesPressed 카운터가
        //   누적되어 grabJust=true 가 40+ 틱 stuck → isCrawling 진동/풀림 BUG.
        //   sprintKey/sneakKey/jumpKey 와 동일 rising-edge 패턴으로 통일.
        boolean curGrabPressed = SmartMovingKeys.grab.isPressed();
        grabJustPressed = curGrabPressed && !prevGrabKeyPressed;
        prevGrabKeyPressed = curGrabPressed;

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
        //
        // ⚠️ DEFER (세션 35 통합테스트 BUG-6): I/O 키 비활성화 — 사용자 임시 비활성화 요청.
        //   재활성화 = 아래 `boolean userSpeedEnabled = false;` 라인 삭제 + 원래 조건 주석 복원.
        //   상세 = docs/fix/integration_test_bugs.md §BUG-6 참조.
        boolean userSpeedEnabled = false;   // 임시 비활성화 (세션 35)
        // boolean userSpeedEnabled = SmartMovingConfig.Config.enabled
        //         && SmartMovingConfig.Config.speedUser
        //         && MinecraftClient.getInstance().interactionManager != null
        //         && MinecraftClient.getInstance().interactionManager.getCurrentGameMode()
        //                 == net.minecraft.world.GameMode.CREATIVE;
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
            // **B-31c-post 해소 (세션 134)**: 원본 L2346 `getMaxPlayerSolidBetween(minY, maxY, 0)
            //   > minY` (머리 위 AABB 내 고체 존재) 정밀 복원. B-42a 헬퍼 소비로 기존
            //   `!canStandUp` 근사 해소.
            this.initializeCrawling = false;
            // 🔴 사용자 보고 fix (2026-05-04 — "5 tick 동안 SWIMMING 잔존"):
            //   원본 SmartMovingSelf L2344: `!(remote && multiPlayerInitialized != 0)`.
            //   `remote` = multi player (외부 서버) — vanilla 1.7.10 의 worldObj.isRemote 동등.
            //   1.21.1 매핑: `world.isClient()` 가 항상 true (integrated server 도 client world)
            //   → single player 도 multiPlayerInitialized=5 (= server sync) 동안 init 미진입.
            //   정확 매핑: MinecraftClient.getInstance().getServer() == null = multi player.
            boolean isRemoteServer = net.minecraft.client.MinecraftClient.getInstance().getServer() == null;
            if (!this.initialized
                    && !(isRemoteServer && this.multiPlayerInitialized != 0)
                    && !player.hasVehicle()) {
                // 🔴 사용자 보고 fix (2026-05-03 — "1칸 공간 엎드린 채 게임 나갔다 들어오면
                //   SM crawl 미인식, vanilla SWIMMING pose"):
                //   원인: player.getBoundingBox() 가 동적 pose 박스 (= SWIMMING 0.6m). 1칸 공간
                //     안 fit → bb 안 solid 검사 false → toCrawling 미호출 → SM 인식 X.
                //   원본 1.7.10 vanilla 박스는 항상 STANDING (1.8m) → 자연스럽게 천장 검사 매칭.
                //   fix: STANDING height 로 yMax 명시 → 원본 동작 1:1 매핑.
                Box bb31c = player.getBoundingBox();
                double standingHeight = player.getDimensions(net.minecraft.entity.EntityPose.STANDING).height();
                double standMinY = bb31c.minY;
                double standMaxY = standMinY + standingHeight;
                double solidTop = getMaxPlayerSolidBetween(player, standMinY, standMaxY, 0);
                boolean shouldCrawl = solidTop > standMinY;
                if (shouldCrawl) {
                    this.initializeCrawling = true;
                    this.toCrawling();
                    // 🔴 사용자 보고 fix #2 (2026-05-03 — "콜리전 작음, shift 뗀 후 안 풀림"):
                    //   원본 SmartMovingSelf L2827-L2836 의 후속 처리:
                    //     setHeightOffset(-1F);
                    //     if(!initializeCrawling || sp.worldObj.isRemote) move(0, -1D, 0);
                    //     if(initializeCrawling) wasCrawling = toCrawling();
                    //   = setHeightOffset(-1F) + move(0,-1,0). 이전 우리 매핑은 toCrawling() 만 호출 →
                    //     heightOffset=0 잔존 → 콜리전 = SWIMMING (0.6m) 잔존.
                    //   fix: heightOffset=-1F + calculateDimensions → SM crawl 박스 (0.6 x 0.8) 적용.
                    //   ignoreNextStopSneakButtonPressed (toCrawling 안 set) → 다음 sneak release 무시
                    //     = 만약 이게 standUp 막는다면 추가 검토 필요.
                    this.heightOffset = -1F;
                    player.calculateDimensions();
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
                // 🔴 사용자 보고 fix (2026-05-04 — "ICC 후 1칸 공간 shift 뗌 → isCrawling 풀림"):
                //   디버그 로그 분석 = heightOffset=0 잔존인데 isCrawling=true 케이스 발생.
                //   mustCrawl 식 = ceiling-bottom < height - heightOffset. heightOffset=0 시
                //   우측 = 1.8 → 1칸 공간 (ceiling-bottom = 1.8) 정확 → false → mustCrawl=false →
                //   shift release 시 isCrawling=false 풀림 → vanilla SWIMMING POSE BUG.
                //   fix: mustCrawl 계산 전 isCrawling=true 잔존 시 heightOffset=-1 강제 동기화.
                if (isCrawling && heightOffset != -1F) {
                    heightOffset = -1F;
                }
                // 🔴 원본 1:1 fix (디버그 로그 분석 결과):
                //   원본 SmartMovingSelf L2395-L2402:
                //     boolean mustCrawl = false;
                //     if (isCrawling || isClimbCrawling) {
                //         mustCrawl = crawlStandUpCeiling - crawlStandUpBottom < sp.height - heightOffset;
                //     }
                //   원본은 isCrawling || isClimbCrawling 시에만 mustCrawl 검사. 그 외 false 유지.
                //
                //   기존 1.21.1 매핑: 가드 없이 항상 검사. 비행 종료 엣지 frame N 시작 시점
                //   entity.y=y_ground-1 (비행 중 위치) 기반 standBox 검사 → dirt block 안 박힘 →
                //   canStandUp=false → mustCrawl=true. 그 후 standUp 으로 entity.y 보정 됐어도
                //   L1559 isCrawling 갱신 시점 mustCrawl=true 잔존 → isCrawling=true 활성 →
                //   다음 frame sm_updatePose_client SWIMMING POSE → 엎드리기 자세.
                //
                //   해결: 원본 1:1 가드 (isCrawling || isClimbCrawling) 로 변경. 비행 중/종료
                //   직후 frame 은 isCrawling=false 라 검사 안 함 → mustCrawl=false 유지 →
                //   isCrawling 잘못 활성 차단.
                if (isCrawling || isClimbCrawling) {
                    // 🔴 (Phase 3, Crawl 1:1 fix — task #10): 원본 mustCrawl 정확 식 적용.
                    //   원본 SmartMovingSelf L2399-L2401 라인별 1:1:
                    //     crawlStandUpBottom = getMaxPlayerSolidBetween(minY - (init?0:1), minY,
                    //                          crawlOverEdge ? 0 : -0.05);
                    //     crawlStandUpCeiling = getMinPlayerSolidBetween(maxY, maxY + 1.1, 0);
                    //     mustCrawl = crawlStandUpCeiling - crawlStandUpBottom < sp.height - heightOffset;
                    //   이전 매핑 `!canStandUp(player)` 는 STANDING box 빈 공간 검사 (~0.1 블록 근사).
                    //   정확 식 = 천장 - 바닥 < height - heightOffset (height=1.8F, heightOffset=-1 시
                    //   가용 공간 < 2.8 검사 = 정확 매핑).
                    Box bb = player.getBoundingBox();
                    double minYR = bb.minY;
                    double maxYR = bb.maxY;
                    double horizontalTolerance = cfg0.crawlOverEdge ? 0 : -0.05;
                    double crawlStandUpBottom = getMaxPlayerSolidBetween(player,
                            minYR - (initializeCrawling ? 0D : 1D), minYR, horizontalTolerance);
                    double crawlStandUpCeiling = SmartMovingClimber.getMinPlayerSolidBetween(player,
                            maxYR, maxYR + 1.1D, 0);
                    float playerHeight = player.getDimensions(net.minecraft.entity.EntityPose.STANDING).height();
                    mustCrawl = crawlStandUpCeiling - crawlStandUpBottom < playerHeight - heightOffset;
                    if (player.getAbilities().flying
                            && (cfg0.isFlyingEnabled() || cfg0.isLevitateSmallEnabled())) {
                        mustCrawl = false;
                    }
                } else {
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
            //   else if (isCrawling) → 액체 천장 검사 (정확 식, task #9 정정)
            // 🔴 (Phase 3, Crawl 1:1 fix — task #9): 원본 L2412-L2417 정확 식 적용.
            //   crawlStandUpLiquidCeiling = getMinPlayerLiquidBetween(maxY, maxY + 1.1)
            //   if (liquidCeiling - crawlStandUpBottom >= height + 1F) → false
            //   이전 매핑 `!isDipping` 단순 검사 (근사) → 정확 식 (액체 천장 - 바닥 >= 2.8 검사).
            if (contextContinueCrawl) {
                if (inputContinueCrawl || player.isTouchingWater() || mustCrawl) {
                    contextContinueCrawl = false;
                } else if (isCrawling) {
                    Box bbCtx = player.getBoundingBox();
                    double crawlStandUpLiquidCeiling = getMinPlayerLiquidBetween(player,
                            bbCtx.maxY, bbCtx.maxY + 1.1D);
                    // crawlStandUpBottom 은 mustCrawl 분기에서 isCrawling||isClimbCrawling 시 계산됨.
                    //   여기서는 contextContinueCrawl=true 이고 isCrawling 가드 안 — 따라서
                    //   원본 동일 시점 (mustCrawl 계산 후) 의 crawlStandUpBottom 재계산.
                    double horizontalToleranceCtx = cfg0.crawlOverEdge ? 0 : -0.05;
                    double crawlStandUpBottomCtx = getMaxPlayerSolidBetween(player,
                            bbCtx.minY - (initializeCrawling ? 0D : 1D), bbCtx.minY, horizontalToleranceCtx);
                    float playerHeightCtx = player.getDimensions(net.minecraft.entity.EntityPose.STANDING).height();
                    if (crawlStandUpLiquidCeiling - crawlStandUpBottomCtx >= playerHeightCtx + 1F) {
                        contextContinueCrawl = false;
                    }
                }
            }

            // wouldWantCrawl (원본 L2419-L2430): isCrawling 유지 경로 + 신규 진입 경로.
            // wantCrawl = isCrawlingEnabled && wouldWantCrawl (원본 L2431-L2432).
            boolean crawlingEnabled_  = cfg0.crawl && cfg0.enabled;
            // B-36-pre (세션 77): 지역 `wouldWantCrawl_` → `this.wouldWantCrawl` 필드 승격.
            // B-36 grab.StartPressed 3분기 (원본 L2838-L2861) 에서 필드 참조 예정.
            // 🔴 사용자 의도: 사다리/덩굴 정면 + grab + sneak → crawl 진입 X (등반 우선).
            //   원본 SmartMovingSelf L2419-2428 의 분기 2 (grabJustPressed && sneak && onGround)
            //   는 사다리/덩굴 인접 케이스에서도 첫 틱 isCrawling=true 활성 → SM crawl 자세
            //   1 틱 visible (사용자 "엎드리기 발동" 인식).
            //   해결: grabJustPressed 분기에 사다리/덩굴 정면 가드 추가. 사용자 grab+sneak
            //   누름 시 등반 의도 (사다리/덩굴 정면) 면 crawl 진입 차단.
            boolean facedClimbable_ = SmartMovingClimber.isFacedToLadder(player, isClimbCrawling)
                    || SmartMovingClimber.isFacedToSolidVine(player, isClimbCrawling);
            wouldWantCrawl =
                    !player.getAbilities().flying &&
                    !isSliding &&  // 🔴 (2026-05-04 사용자 보고 — "비행 → grab+sneak 착지 슬라이딩 90° 꺾임"):
                    //   원본은 분기 1 (isCrawling 자가유지) + 분기 2 (grab rising edge) 모두
                    //   sliding 진입 시점에 자연 false (grabHold 라 rising edge 안 발생 + 진입
                    //   직전 isCrawling=false 잔존). 우리 매핑은 어떤 path 가 비행 종료 시 1
                    //   tick isCrawling=true 만들어 분기 1 자가유지 → 슬라이딩 + isCrawling=true
                    //   동시 잔존 → setupTransforms 가 sliding R_x(-π/2) + crawling R_x(-78.75°)
                    //   둘 다 적용 (별도 if) → 90° 꺾임 + setAngles 가 isCrawling 분기 우선 →
                    //   엎드리기 자세. 명시 가드로 cycle 자체 차단.
                    (
                        (isCrawling && (inputContinueCrawl || contextContinueCrawl))
                        ||
                        (grabJustPressed && (sneakToggled || sneakPressedRaw) && player.isOnGround()
                                && !facedClimbable_)
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

                // 원본 L2721-L2732 3-OR — 원본 1:1 매핑.
                //
                // 이전 매핑은 forwardPressed_holdGuard 가드 추가. 가드 의도 = "사다리 등반 + W + sneak
                // 시 찔끔찔끔 등반 BUG fix". 그러나 가드의 순환 결함:
                //   일반 갭 시나리오 (hG=true, hCG=false) 에서 isClimbHolding=false 시작 →
                //   needClimbCrawling17=false → 가드 활성 → wCH=false → isClimbHolding=false 유지 →
                //   영원히 ICC 진입 X.
                //
                // 사용자 보고 (2026-05-02) — "사다리 끝 + sneak → 엎드리기 전환" 동작이 가드 때문에
                // 차단됨. 원본 1.7.10 은 가드 없이 정상 작동 → 가드 제거 = 원본 동작 회복.
                //
                // 이전 BUG ("찔끔찔끔 등반") 는 다른 매핑 차이로 발생했던 부수효과로 추정 (= ICC 메인 식
                // 정상 동작인 cic 카운트다운 + 해제 + 재진입 사이클을 사용자가 부정적으로 인식했을 가능성).
                wantClimbHolding = (isClimbHolding && sneakPressedRaw)
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
            // ※ **포커스 #3 B-2 (세션 3 검증)**: B-10d (isLevitating 갱신 로직) 이미 완전
            //   이식됨 확정 — SmartMovingSwimmer.updateSwimState L192-L196 (세션 71) 에서
            //   매 tick `isDiving && !diveUp && !diveDown && moveStrafe==0 && moveForward==0`
            //   공식으로 갱신. 원본 SmartMovingSelf L505 `isLevitating = levitating` 1:1.
            //   호출: sm_travel_client L92. isGroundSprinting 공식의 isLevitating 분기 정상 활성.
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
            //
            // 🔴 BUG-28 + BUG-29 진짜 원인 정정 (Flying Phase / 세션 44): 비행/Levitate 진입 시
            //   heightOffset=-1F set 자체 제거.
            //   원본 SmartMovingSelf.setHeightOffset (L1694-L1704) = `boundingBox.minY -= heightOffset
            //   + height += heightOffset` — **박스 크기만 변경 (player 위치 불변)**.
            //   1.21.1 매핑 = MixinEntityClient.afterMoveEntity L99-L101 의 `player.setPos(y -
            //   heightOffset)` — **player 위치 변경 + 매 틱 호출 = 매 틱 +1 누적 → 무한 상승**!
            //   사용자 보고 BUG-28 (첫 비행 무한 상승) + BUG-29 (몸 중심점이 발끝으로 바뀐 느낌)
            //   의 진짜 직접 원인. 1.21.1 vanilla Creative 비행 자동 hitbox 처리 → SM 박스 보정
            //   불필요. 진입 엣지 setHeightOffset 호출 제거 = 1.21.1 vanilla 정상 동작 + 누적 0.
            //   해제 엣지 restoreFromFlying = true 는 그대로 유지 (다른 메커니즘 영향 없음).
            if (!isFlying && wasFlying) {
                restoreFromFlying = true;
            }
            if (!cfg0.isFlyingEnabled() && cfg0.isLevitateSmallEnabled()) {
                if (!isLevitating && wasLevitating) {
                    restoreFromFlying = true;
                }
            }

            // 🔴 비행/Levitate 진입 엣지: heightOffset = -1F + calculateDimensions (단계 3+):
            //   원본 SmartMovingSelf L2511-L2512 `setHeightOffset(-1)` 매핑.
            //   - heightOffset = -1F: standupIfPossible 가드 (`if (heightOffset >= 0) return`)
            //     통과 신호 (종료 엣지에서 standUp 호출되도록).
            //   - calculateDimensions: dimensions/boundingBox 갱신. 우리 inject 가
            //     (0.6, 0.8, 1.62) 반환 + MixinEntity.sm_offsetBoundingBoxForFlying 가
            //     box.offset(0, 1, 0) 적용 → 박스 (y+1, y+1.8).
            //
            //   ⚠️ 종료 엣지에서는 calculateDimensions 호출하지 않음 — standupIfPossible 의
            //   standUp 안 player.move 가 비행 modified 박스 기준 gap 측정 → entity.y 보정
            //   필요. 박스를 미리 복원하면 gap 측정 부정확. 종료 시 dimensions 복원은
            //   standupIfPossible 끝에서 수행 (B-N-standup 메서드 본체에 추가).
            if ((isFlying && !wasFlying) || (isLevitating && !wasLevitating)) {
                this.heightOffset = -1F;
                // 🔴 stale restoreFromFlying 클리어 (자동 착지 1회 한정 BUG fix):
                //   직전 비행 종료 엣지에서 restoreFromFlying=true 로 set 되었는데, 자동 착지
                //   (tryLanding 트리거) 없이 사용자가 수동으로 비행 해제한 경우, 이 플래그가
                //   클리어되지 않은 채 다음 비행 진입까지 이월된다. 이 상태로 다음 비행 진입 직후
                //   첫 standupIfPossible 호출 시 `restoreFromFlying=true && tryLanding=false &&
                //   !groundClose && !sneak` 분기가 resetHeightOffset() 을 호출 → heightOffset=0
                //   reset → 이후 모든 standupIfPossible 가드 (heightOffset >= 0) 미통과 →
                //   조기 return → 자동 착지 영구 불가능.
                //   해결: 비행 진입 엣지에서 stale 플래그 클리어 → restoreFromFlying 은 진입한
                //   비행 세션 내에서만 의미 있는 신호로 유지.
                this.restoreFromFlying = false;
                player.calculateDimensions();
            }
            if ((!isFlying && wasFlying) || (!isLevitating && wasLevitating)) {
                this.heightOffset = -1F;
            }

            // **포커스 #3 B-3 (세션 4)**: 원본 L2542-L2544 tryLanding 계산 + standupIfPossible
            //   호출 이식. 1.21.1 SmartMovingClientState 의 isFlying 엣지 처리 (위 블록) 직후
            //   원본 흐름과 동일하게 배치.
            //   원본 식 (L2542):
            //     boolean tryLanding = isFlying && !Options._flyCloseToGround.value
            //                       && horizontalSpeedSquare < 0.003D && sp.motionY > -0.03D;
            //   원본 호출 (L2543-L2544):
            //     if (restoreFromFlying || tryLanding) standupIfPossible(tryLanding, restoreFromFlying);
            //   상수: 0.003D (수평 속력 임계) / -0.03D (motionY 임계) 정확 보존.
            {
                double _vX = player.getVelocity().x;
                double _vZ = player.getVelocity().z;
                double _horizontalSpeedSquare = _vX * _vX + _vZ * _vZ;
                // BUG-26 정정 취소 (세션 42): 사용자 핵심 지시 "원본 코드 보고 1:1 번역, 막 코드
                //   넣지마" 따라 세션 41 의 `!cfg.flyCloseToGround` 가드 삭제 정정 취소 (1:1 위반).
                //   원본 SmartMovingSelf 전수 grep (L2199 만 capabilities.isFlying=false) 결과
                //   원본의 자동 착지 메커니즘 = standupIfPossible (tryLanding, flyCloseToGround=false
                //   전제) 만. flyCloseToGround=true (기본값) = 의도된 자동 착지 비활성.
                //   사용자 보고 "원본도 자동 착지" = 사용자가 1.7.10 에서 config 변경 사용 또는
                //   인지 오류 가능. 자동 착지 원하면 config flyCloseToGround=false 설정 권장.
                boolean tryLanding = isFlying
                        && !cfg0.flyCloseToGround
                        && _horizontalSpeedSquare < 0.003D
                        && player.getVelocity().y > -0.03D;
                if (restoreFromFlying || tryLanding) {
                    standupIfPossible(player, tryLanding, restoreFromFlying);
                    // 🔴 (사용자 보고 — 비행 → 엎드리기 박스 0.6 BUG / 2026-04-30):
                    //   `restoreFromFlying = true` 는 비행/헤드점프 종료 엣지 (L1573/L1577/L1804)
                    //   에서 set 되는데 어디서도 클리어 안 됨 → stale 잔존 → 매 틱 standupIfPossible
                    //   호출 → 끝부분 player.calculateDimensions() (L3137) 가 sm.isCrawling 갱신
                    //   (L1697) 전 시점에 호출되어 mixin 가드 매치 못 함 → vanilla SWIMMING POSE
                    //   dim (0.6×0.6) 적용 → box height 0.6 → mustCrawl=true 오발동 → sneak 릴리즈
                    //   해도 isCrawling 유지.
                    //   해결: 호출 후 즉시 클리어 → 종료 엣지 1회만 유지.
                    //   원본 1.7.10 은 setHeightOffset 가 box 직접 조작 → calculateDimensions 호출
                    //   자체 없음. 1.21.1 EntityPose 호환 위해 L3137 추가했고 stale 결합이 BUG 발현.
                    this.restoreFromFlying = false;
                }

                // 🔴 비행 종료 엣지 안전망 (단계 4):
                //   standupIfPossible 의 standUp / toSlidingOrCrawling 분기 안에서 dimensions
                //   복원 호출 (player.calculateDimensions) 이 어떤 이유로 작동 안 했을 경우
                //   강제 보강. 종료 엣지에서 무조건 calculateDimensions 호출 → dimensions 가
                //   잔존 (0.6, 0.8, 1.62) 라도 STANDING (1.8) 으로 복원.
                //
                //   호출 위치: standupIfPossible 후 (gap 측정 시점에는 비행 modified 박스 유지
                //   필요했으므로 진입 엣지 처리만으로는 부족).
                //   중복 호출 무해 (vanilla calculateDimensions 는 idempotent).
                if ((!isFlying && wasFlying) || (!isLevitating && wasLevitating)) {
                    player.calculateDimensions();
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
                // 🔴 (Phase 1, Crawl 1:1 fix): 원본 L2437 의 `(dippingDepth + heightOffset)` 1:1 매핑.
                //   이전 매핑은 heightOffset 누락 → 크롤 중 (heightOffset=-1) 시 실제 유효 물
                //   깊이 보정 안 됨 → 깊이 0.65 이상에서 canCrawl=false (원본은 1.65 이상).
                //   원본 의도: 크롤 중 물 깊이 -1 보정 → 더 깊은 물에서도 크롤 유지 가능.
                // 🔴 사다리/덩굴 ICC EXIT crawl 유지 fix (사용자 보고 — 사다리 끝 + sneak crawl 안 됨):
                //   원본 1.7.10 박스 위치 = (posY+1, posY+1.8) (= mixin offset 박스 +1) → ladder Y range
                //   밖 → 사다리 grip X → isClimbing=false → canCrawl=true → isCrawling 유지.
                //   1.21.1 우리 매핑 박스 = (entity.y, entity.y+0.8) (= bottom snap 후 ladder maxY 표면) →
                //   ladder grip 영역 안 → 다음 tick handleClimbing → isClimbing=true → canCrawl=false →
                //   isCrawling 즉시 false 복원 BUG.
                //   해결: `!isClimbing` 가드 → `(!isClimbing || isCrawling)` = isCrawling 유지 시
                //   isClimbing 가드 우회. 1.7.10 ↔ 1.21.1 사다리 collision 영역 차이 보정.
                //   부수효과 분석: 일반 ICC 케이스 isClimbing=false 라 영향 X. 사용자 사다리 옆 crawl →
                //   climbing 전환 (= isCrawlClimbing Feature 3) 시 isClimbing+isCrawling 동시 활성 OK.
                boolean canCrawl = !isSwimming_sm
                        && !isDiving
                        && (!isDipping || (dippingDepth + heightOffset) < SWIM_CRAWL_WATER_TOP_BORDER)
                        && (!isClimbing || isCrawling)
                        && player.fallDistance < cfg.fallingDistanceMinimum;
                wasCrawling = isCrawling;                              // 원본 L2441
                // 🔴 (2026-05-04 사용자 보고 — "비행 → 슬라이딩 시 5 frame 90° 꺾인 엎드리기 자세"):
                //   dump 분석: f=29~25 ROTATION_ACCUMULATE 알람 — slide=true + crawl=true 동시
                //   잔존 (5 frame). root = mustCrawl=true 강제 set (project_restoreFromFlying_complete.md
                //   "same-tick mustCrawl=true 강제"). 이전 fix 의 wouldWantCrawl !isSliding 가드는
                //   wantCrawl 만 차단 → mustCrawl=true 매치 시 isCrawling=true.
                //   해결: isCrawling 결정에 !isSliding 추가 → wantCrawl + mustCrawl 모두 차단.
                //   wouldWantCrawl 가드와 동일 패턴 (메모리 feedback_slide_crawl_concurrent_state.md).
                isCrawling = !isSliding && canCrawl && (wantCrawl || mustCrawl);
                // ICC EXIT 후 isCrawling 자연 false 시 iccExitJustToCrawl 플래그 reset.
                if (!isCrawling) iccExitJustToCrawl = false;
                // contextContinueCrawl 해제 (L2446-L2447) 는 L822 pre-compute 블록에 이미 이식.

                // 🔴 heightOffset 잔존 cleanup (사용자 보고 BUG: 가만히 standing 시 heightOffset=-1F 잔존):
                //   isCrawling/isClimbCrawling/isHeadJumping/isFlying/isLevitating/isSwimming_sm/isDiving/
                //   isSliding 모두 false = 정상 standing → heightOffset=0F 강제. 어떤 SM state 진입
                //   후 reset 안 된 잔존 케이스 fallback. 다음 SM state 진입 시 set 다시 가능.
                boolean anySmallSmState = isCrawling || isClimbCrawling || isHeadJumping
                        || isFlying || isLevitating
                        || isSwimming_sm || isDiving || isSliding;
                if (!anySmallSmState && heightOffset != 0F) {
                    heightOffset = 0F;
                }

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
                // Phase D 새 시그니처 (포커스 #2.5 세션 19): tryJump(Up, null, null, null) 1:1
                SmartMovingJumper.tryJump(player, this, SmartMovingJumper.UP, null, null, null);
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
            // B-26 (세션 75) → **B-42-B26 해소 (세션 134)**: 원본 L2555-L2557 전수 이식.
            //   원본: setHeightOffset(-1) + move(0, -1D, 0) + tryJump(Config.SlideDown, false,
            //         wasRunning, null).
            //   SlideDown tryJump 경량 이식 — `SmartMovingJumper.trySlideDownJump` 로 수평
            //   속도 증폭 + isJumping=true 설정. factor 인프라 (§7 B-42-B26-approx) 는
            //   기본 1F 가정.
            // B-Slide-FlyToSlide-fix (2026-05-04): `!isSliding` 가드 제거 — 원본 L2553 1:1.
            //   원본은 `!isSliding` 가드 없음. 비행 → 착지 + grab+sneak 시나리오에서
            //   restoreFromFlying 으로 toSlidingOrCrawling → isSliding=true (motion 부스트 X)
            //   직후 같은 tick 직접 진입 6-AND 매치 → tryJump(SLIDE_DOWN) → motion 부스트.
            //   `!isSliding` 가드 가 있으면 이 두 번째 매치 차단 → motion 부스트 안 됨 →
            //   다음 종료 분기 (sneak hold + speed²<0.01) 매치 → 즉시 종료. 사용자 보고 BUG.
            // 🔴 fix #32 (2026-05-08): 자체 슬라이딩 분기 가드 — wasGroundSprinting 직전 tick 값 사용.
            // 🔴 fix #32 (2026-05-08, 사용자 시나리오 timing race): isGroundSprinting →
            //   wasGroundSprinting 변경. 원본 L2553 자체 슬라이딩 분기가 L2679 isGroundSprinting
            //   갱신 *이전* 평가 (= 직전 tick 값 사용). 우리 매핑은 갱신 후 평가 (= 현재 tick 값).
            //   사용자 시나리오: SPACE 떼는 tick 에 SHIFT 동시 누름:
            //     - 직전 tick (차징 마지막): isGroundSprinting=true.
            //     - 현재 tick (발사): onGround=false → isGroundSprinting=false (갱신 후).
            //     - 원본은 자체 슬라이딩 평가 시점 직전 값=true → 매치.
            //     - 우리 매핑은 갱신 후 false → 미매치 BUG.
            //   해결: wasGroundSprinting 사용 (= L1758 의 갱신 직전 저장 = 직전 tick 값).
            // 🔴 fix #36 (2026-05-08, "앞에 부딪혀 튕김" BUG): `!isHeadJumping` 가드 추가.
            //   fix #32 의 wasGroundSprinting 가 두 case 매치:
            //     case A (= 같은 tick, isHeadJumping=false): HEAD_UP fire 직전 → 두 점프 누적 (사용자 의도).
            //     case B (= 다음 tick, isHeadJumping=true): 진행 중 추가 SLIDE_DOWN → motion boost
            //       → 사용자 인지 "튕김" + isSliding=true 잔존 → 종료 push up race → "땅 콜리전".
            //   해결: !isHeadJumping 가드 추가 → case A 만 매치, case B 차단.
            //   원본 1.7.10 가드 (L2553) 에는 !isHeadJumping 없지만 isGroundSprinting (= onGround 의존)
            //   가 다음 tick (onGround=false) 자동 미매치. 우리 fix #32 의 wasGroundSprinting 는
            //   다음 tick 매치 가능 → 원본 효과 위해 !isHeadJumping 가드 보강 (1.7.10 → 1.21.1
            //   timing 차이 정합).
            if (cfg0.slide && cfg0.enabled
                    && SmartMovingKeys.grab.isPressed()
                    && (wasGroundSprinting
                            || (wasRunning && !isRunning(player) && player.isOnGround()))
                    && !isCrawling
                    && sneakKeyStartPressed
                    && !isDipping) {
                // 🔴 fix #53 (2026-05-10, 사용자 보고 "여우무빙 거리 짧음 + 변동 + Jump Boost 시 극명"):
                //   = fix #40 reverse — 1.12.2 SMReboot SMSelf.java L2303-L2312 1:1 복원.
                //
                //   원인 분석:
                //     fix #40 = case B 분기 (isHeadJumping=true 잔존) 진입 시 setHeightOffset/move
                //     skip + isHeadJumping=true 잔존. 의도: dim eye=1.62 유지 → 박스 +1m up 정상.
                //     사용자 case A 시도 (jump+sneak 같은 tick) 시도 우리 매핑 = case B 분기 작동
                //     (= 진입 직전 isHeadJumping=true → !isHeadJumping=false → fix #40 skip 분기).
                //     → 박스 +1m 공중 부양 (mixin offset isHeadJumping 우선) → vy 자유 낙하 누적
                //     → fallDistance > 0.05 빨리 매치 → SS-B25 자동 전환 빨리 → sliding 단계 짧음.
                //     1.12.2 case A = 박스 ground 위 (= setHeightOffset + move) + isHeadJumping=false
                //     강제 → vy=0 (ground reset) → fallDistance=0 → SS-B25 매치 X → 사용자 SHIFT
                //     까지 sliding 길게 진행.
                //
                //   해결: 1.12.2 1:1 식 복원 (setHeightOffset + move + isHeadJumping=false 강제).
                //
                //   부작용 위험: fix #40 의 history "박스 -1m down jump → ground 박힘" 재발 가능.
                //   재발 시 mixin offset 식 변경 (= isHeadJumping && isSliding 시 SLIDING dim 우선)
                //   별도 fix 진행. fix #29 + fix #38 의 calculateDimensions + cameraY 강제 식은
                //   유지 (= 박스 dim 갱신 시 시각 안정).
                // 🔴 fix #60 (2026-05-10, dump 분석 결과 — 사용자 보고 "슬라이딩 진입 직후 콜리전
                //   처음 조금 위 + 땅으로 들어가버림"):
                //
                //   원인: 1.12.2 의 setHeightOffset(-1) 은 boundingBox.minY 직접 += 1m. 우리 매핑
                //   은 vanilla 1.21.1 의 boundingBox 자동 식 + mixin offset 으로 등가 처리.
                //   mixin offset 활성 조건 = dim.height<1 && (dim.eye>1 || POSE.SLIDING).
                //
                //   기존 순서 BUG:
                //     1. heightOffset=-1F. (= 단지 필드 set, 박스 영향 X)
                //     2. player.move(0,-1,0).  ← 이 시점 dim 캐시 = STANDING (1.8). mixin offset
                //                                  차단. 박스 발 = entity.y. vanilla collision →
                //                                  ground 위 시도 -1m → push back → entity.y 변경 X.
                //     3. isSliding=true + isHeadJumping=false 강제.
                //     4. fix #29 calculateDimensions.  ← 이제 dim 갱신 → isSliding=true → fix #59
                //                                          → eye=1.62 → mixin offset 활성 →
                //                                          bbMinY = entity.y + 1m.
                //   결과: entity.y = ground (변경 X). 박스 발 = ground + 1m 공중. vy 자유 낙하 →
                //   fallDistance 누적 → SS-B25 자동 전환 → isSliding 1 tick 만에 isHeadJumping.
                //
                //   해결: SM state 와 calculateDimensions 를 move 전에 — mixin offset 활성 상태
                //   에서 move 호출 → 박스 발 = entity.y + 1m = ground. move(-1) 시도 → 새 박스 발
                //   = (entity.y-1)+1 = entity.y = ground (정렬). collision X. entity.y -= 1 적용.
                //   결과: entity.y = ground - 1m. 박스 발 = ground. 정상.
                //   1.12.2 의 boundingBox 직접 변경 효과 1:1 매핑.
                heightOffset = -1F;
                isSliding = true;                                        // 원본 L2558 — move 전에 set.
                isHeadJumping = false;                                   // 원본 L2559 (fix #53).
                isAerodynamic = false;                                   // 원본 L2560.
                player.calculateDimensions();                            // dim 갱신 → mixin offset 활성.
                player.move(MovementType.SELF, new Vec3d(0, -1D, 0));    // 원본 L2556 — entity.y -1m + 박스 발 ground.
                // 🔴 fix #33 (2026-05-08) 유지: wasGroundSprinting=true 시 isFast 임시 true 강제.
                boolean _savedIsFast = this.isFast;
                if (wasGroundSprinting) this.isFast = true;
                SmartMovingJumper.tryJump(player, this, SmartMovingJumper.SLIDE_DOWN,
                                           false, wasRunning, null);   // 원본 L2557.
                this.isFast = _savedIsFast;
                // 🔴 fix #81 (2026-05-12, 사용자 보고 "여우무빙 진입 시 몸 수평 안 됨"):
                //   자체 슬라이딩 발사 → 같은 tick L2271 자동 cycle 즉시 매치 → isHeadJumping=true 진입.
                //   이때 시각상 일반 헤드점프 회전 (= thetaTarget = π/2 - currentVerticalAngle, 점진 lerp)
                //   적용 → 12 tick 회전 변화. 사용자 보고 BUG.
                //   원본은 *자체 슬라이딩 발사 시점부터 isSliding 분기 매치* (= bipedOuter.rotateAngleX
                //   = Quarter 즉시) 시각 → 수평 유지.
                //   해결 (시각 영역만): wasSelfSlideFire 플래그 set → setupTransforms 헤드점프 분기에서
                //   thetaTarget=Quarter 고정 강제 → 수평 유지. 기능 영역 (= state/cycle/fall reset) 무영향.
                this.wasSelfSlideFire = true;
                // 🔴 fix #29 (2026-05-08, 사용자 보고 "슬라이딩 중 떨어질 때 덜컹"):
                //   진입 시점 cameraY=1.62 (= 직전 standing 잔존) vs 새 dim eye=0.62 →
                //   vanilla updateEyeHeight 0.5 step lerp 으로 N tick 추격 → 카메라 시점 ~1m
                //   천천히 하강 (~350ms over 7 tick) → 사용자 인지 "덜컹".
                // 🔴 fix #38 (2026-05-08, vanilla disassembly + dim race 분석 결과):
                //   사용자 시나리오 (= SLIDE_DOWN+HEAD_UP 두 점프 누적, headJumpCharge>0) 시:
                //     1. fix #29 calculateDimensions → dim eye=0.62. cameraY=0.62 강제.
                //     2. 같은 tick HEAD_UP fire → isHeadJumping=true.
                //     3. PlayerEntity.tick L384 updatePose → POSE 변경 → calculateDimensions 자동
                //        → dim eye=1.62 (isHeadJumping 우선) → 박스 +1m up + cached eye 변화.
                //     4. vanilla updateEyeHeight: cameraY = 0.62 + (1.62-0.62)*0.5 = 1.12. 추격.
                //     → 시점 0.62 → 1.12 → 1.37 → ... → 1.62 진행. 사용자 인지 "원본보다 낮게+천천히 올라옴".
                //   해결: headJumpCharge==0 가드 → 정상 슬라이딩만 fix #29 적용.
                //   사용자 시나리오 (headJumpCharge>0) 시 cameraY=직전 standing 1.62 유지 → 다음 tick
                //   dim 갱신 후 cameraY 추격 X (= standingEyeHeight=1.62 매치). 시점 안정.
                if (headJumpCharge == 0F) {
                    player.calculateDimensions();
                    net.minecraft.client.render.Camera _cam =
                            net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera();
                    if (_cam != null) {
                        float _eye = player.getStandingEyeHeight();
                        ((choco.ratel.smartmoving.mixin.client.MixinCamera) (Object) _cam).sm_setCameraY(_eye);
                        ((choco.ratel.smartmoving.mixin.client.MixinCamera) (Object) _cam).sm_setLastCameraY(_eye);
                    }
                }
            }

            // B-23 (세션 46): isHeadJumping 매 틱 재평가 5-AND 해제 공식 (원본 L2524-L2530)
            // `isHeadJumping = isHeadJumping && !onGround && !(swim||dive) && !(flying||capabilities.flying)
            //                  && !(waterMovement && motionY<0) && !lavaMovement`
            // - onGround: 지면 착지 시 해제
            // - swimming/diving: 수중 진입 시 해제
            // - flying/capabilities.isFlying: 비행 모드 진입 시 해제
            // - waterMovement && motionY<0: 물 접촉 + 하강 중 → 수중 진입 예정 해제
            // - lavaMovement: 라바 접촉 시 해제
            boolean _preIsHeadJumping = isHeadJumping;
            wasHeadJumping = isHeadJumping;
            // 🔴 fix #47 (2026-05-09, 사용자 보고 "원본은 점프강화 fox 시 착지까지 쭉, 우리는 끊김"):
            //   원본 SS-SlideStop (L2553-L2561) 무조건 isHeadJumping=false 강제 → 다음 tick 의
            //   wasHeadJumping=false → SlideToHeadJumping 자동 전환 매치 (= isAerodynamic=true) →
            //   0.999 damping → "착지까지 쭉".
            //   우리 fix #40 case B (= sneak 1 tick 늦음) 시 isHeadJumping=true 잔존 → 매 tick
            //   wasHeadJumping=true 갱신 → SlideToHeadJumping 영원히 차단 → isAerodynamic=false
            //   → 매 tick handleSliding 0.954 damping → ~30 tick 후 motion 0.1 → "끊김".
            //   해결: fox movement 진행 중 (isHeadJumping=true && isSliding=true) wasHeadJumping=false
            //     강제 → 자동 전환 가드 통과 → fallDistance>0.05 도달 시 자동 전환 발동 → isSliding=false,
            //     isHeadJumping=true, isAerodynamic=true → handleLand fix #46 0.999 damping → 길게 유지.
            //   부작용 검증: handleCrash(L2184)는 isHeadJumping=false 조건 → 매치 X (영향 없음).
            //     resetHeightOffset(Mixin L159)는 onGround 조건 → fox 진행 중 onGround=false → 매치 X.
            //     wouldWantSliding(L3635)/Jumper L123 의 wasHeadJumping 항은 grab.isPressed() 와 OR
            //     라 fox 진행 중 grab 누름 → 영향 없음. dump (L1225) 만 영향 (cosmetic).
            if (isHeadJumping && isSliding) {
                wasHeadJumping = false;
            }
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
                // 🔴 fix #81 (2026-05-12): 자체 슬라이딩 fire 플래그 reset — 헤드점프 종료 시.
                //   다음 헤드점프 발사 시 일반 시각 (= thetaTarget 점진 lerp) 유지.
                this.wasSelfSlideFire = false;
                handleCrash(player, cfg0.headFallDamageStartDistance, cfg0.headFallDamageFactor);
                restoreFromFlying = true;
                // 🔴 fix #19 (2026-05-08): 헤드점프 종료 직후 1-tick 플래그 set.
                //   standupIfPossible 의 setPos(y+1) 분기 가드 — 비행/Levitate 종료와 동일
                //   매핑 적용 (= 박스 박힘 보정). 다음 tick reset → 무한 setPos 호출 차단.
                this.justEndedHeadJump = true;
                // 🔴 fix #21 (2026-05-08): SlideToHead 자동 전환 cooldown set.
                //   사용자 환경 (다층 절벽) 에서 setPos(y+1) → 박스 공중 → fallDistance 누적
                //   → 자동 전환 재발 무한 루프 차단. 5 tick 동안 자동 전환 X.
                //   첫 자동 전환 (= 슬라이딩 → 절벽 시나리오) 은 cooldown=0 라 통과.
                this.slideToHeadCooldown = 5;
                // B-N-standup 연결: restoreFromFlying 전환 시 가능하면 즉시 서기 시도.
                standupIfPossible(player, false, true);
                // 🔴 fix #72 (2026-05-10, dump 분석 — 사용자 보고 "여우무빙 → 1칸 공간 진동"):
                //   기존: 5-AND 매치 분기 안 restoreFromFlying=true set + standupIfPossible 호출.
                //   BUG: clear 식 미발동 (= L1978 clear 는 L1951 분기 안만). restoreFromFlying=true
                //     잔존 → 다음 tick L1951 가드 매치 → standupIfPossible 재호출 → toSlidingOrCrawling
                //     → isSliding=true → SS-MATCH → toCrawling + fix #70 push +1m → cycle.
                //   해결: 5-AND 분기 안 standupIfPossible 호출 후도 clear (= 1-tick 의도 매핑 1:1).
                this.restoreFromFlying = false;
            }

            // SlideToHeadJumping 전환 (원본: SmartMovingSelf 행 2546~2550)
            // 슬라이딩 중 낙하거리가 SlideToHeadJumpingFallDistance(0.05F) 초과 → 헤드점프 + 공기역학 모드 전환
            // 🔴 fix #17 (2026-05-08): `!wasHeadJumping` 가드 추가.
            //   헤드점프 종료 직후 (wasHeadJumping=true) standupIfPossible setPos(y+1) → entity.y +1m
            //   → 박스 ground+1m 공중 → vanilla travel gravity → 박스 발 ground 도달 → fallDistance
            //   누적 → 본 자동 전환 매치 → isHeadJumping=true 재진입 → 5-AND 종료 → 무한 루프 BUG.
            //   비행 종료는 entity.y = Y_floor - 1 (vanilla 1칸 공간 push down) 라 setPos(y+1) →
            //   ground 위 정상. 헤드점프는 entity.y -1m 처리 X 라 자동 전환 발동.
            //   해결: `!wasHeadJumping` 가드 — 헤드점프 종료 직후 1 tick 자동 전환 차단.
            //   BUG 1 시나리오 (슬라이딩 → 절벽 → 헤드점프 자동 전환) 는 wasHeadJumping=false 라 통과.
            // 🔴 fix #66 v2 (2026-05-10, dump 분석 — 사용자 보고 "헤드점프 자세 1 tick + 모델 안 보임 cycle"):
            //   기존 가드: isSliding && fall>0.05 && !wasHJ && cooldown=0.
            //   BUG: isSliding 진행 중 fall reset race 로 fall=1.0 누적 (handleSliding 의 fall reset
            //   미발동 또는 vanilla travel cancel 후 fall 잔존). cooldown=5 → 0 도달 시 매치 →
            //   isHeadJumping=true 변경 → fox movement cycle.
            //   원본 1.7.10 도 vy 가드 없으나 fall reset 정상 → ground 위 isSliding 시 fall=0 → 매치 X.
            //   우리 매핑: fall=1.0 잔존 → 매치 → cycle.
            //
            //   해결: fall reset 강제 (isSliding && onGround 시 fall=0).
            //   fox movement 진입 시: onGround=false (= vy=+0.42 점프 motion) → fall reset 안 함 →
            //     fall>0.05 잔존 → AUTO 매치 정상 (= isHeadJumping=true 변경, fox movement 작동).
            //   시나리오 B (정상 슬라이딩): onGround=true → fall=0 reset → cooldown=0 도달 시 fall=0 →
            //     AUTO 매치 X → cycle 차단.
            //
            //   v1 시도 vy<-0.1 가드: fox movement 진입 시 vy=+0.42 → 가드 X → AUTO 매치 X →
            //     isHeadJumping=true 변경 안 됨 → fox movement 작동 안 함. 사용자 보고 "여우무빙 망가짐". revert.
            // 🔴 fix #67 (2026-05-10): atSolidTop 가드 추가 — box 발 = ground top 동등 (touching) 시도 fall reset.
            if (isSliding && player.fallDistance > 0F) {
                net.minecraft.util.math.Box _bbF67 = player.getBoundingBox();
                double _solidTopF67 = getMaxPlayerSolidBetween(player, _bbF67.minY - 1.0, _bbF67.minY, 0);
                boolean _atSolidTopF67 = Math.abs(_bbF67.minY - _solidTopF67) < 1.0E-3;
                if (player.isOnGround() || _atSolidTopF67) {
                    player.fallDistance = 0F;
                }
            }
            if (isSliding && player.fallDistance > SLIDE_TO_HEADJUMPING_FALL_DISTANCE
                    && !wasHeadJumping
                    && this.slideToHeadCooldown == 0) {
                isSliding = false;
                isHeadJumping = true;
                isAerodynamic = true;
            }

            // B-Slide-Stop (2026-05-04): 원본 L2563-L2567 sneak 떼기 / 속도² 임계 종료 분기.
            //   if (isSliding && (!sneakButton.Pressed
            //                     || horizontalSpeedSquare < Config._slidingSpeedStopFactor.value * 0.01))
            //   {
            //       isSliding   = false;
            //       wasCrawling = toCrawling();   // 종료 즉시 크롤 진입
            //   }
            // 단위: 원본 horizontalSpeedSquare = motionX² + motionZ² (L2389), stopFactor 기본 1F →
            //   임계 = 0.01 (= speed 0.1 m/tick).
            // 위치: SlideToHeadJumping (위) 와 큰 낙하 → crawl (아래) 사이. 원본 L2563 자리 1:1.
            // ※ 이전 매핑: SmartMovingSlider.handleSliding 안에 horizontalSpeed (linear) 단위 + sneak
            //   누락 + toCrawling() 미호출 의 부분 매핑 → 원본 1:1 로 정정.
            if (isSliding) {
                Vec3d _vel2563 = player.getVelocity();
                double horizontalSpeedSquare = _vel2563.x * _vel2563.x + _vel2563.z * _vel2563.z;
                // 🔴 fix #87 (2026-05-12, BUG = "슬라이딩 phase + 비행 진입 시 cycle"):
                //   vanilla 1.21.1 ClientPlayerEntity.tickMovement 안 신규 @AUTOLAND 분기
                //   (`isOnGround() && abilities.flying → flying=false`) 가 슬라이딩 phase 시 매 frame
                //   매치 → @TOGGLE ↔ @AUTOLAND cycle.
                //   원본 1.7.10 vanilla 에는 @AUTOLAND 분기 없음 (또는 다른 동작) 이라 BUG 안 됨.
                //   원본 SmartMovingSelf.wouldWantCrawl (L2419) 의 `!isFlying` 가드 패턴 차용 — 비행
                //   진입 시 SM phase 자동 종료 → 박스/POSE STANDING 전환 → @AUTOLAND 매치 X → 비행 안정.
                //   엎드리기는 wouldWantCrawl 가드로 자동 처리 (dump 검증). 슬라이딩 동일 path 보강.
                if (!sneakPressedRaw
                        || horizontalSpeedSquare < cfg0.slidingSpeedStopFactor * 0.01
                        || isFlying) {
                    isSliding   = false;
                    // 🔴 fix #48 (2026-05-09, 사용자 보고 "원본은 키 떼도 쭉, 우리는 키 떼면 끊김"):
                    //   원본 SlideToHeadJumping 자동 전환 (L2546) 은 ~10 tick (vy<0 + fallDistance>0.05) 후
                    //   발동 → isAerodynamic=true. 사용자 SHIFT 누름 timing 자연스럽게 0.5s 이상이라
                    //   원본은 자동 전환 *후* SHIFT 뗌 → isAerodynamic=true 잔존 → 0.999 damping → 길게.
                    //   사용자 빨리 (< 10 tick) SHIFT 떼는 시나리오에선 원본도 끊김 발생. 우리 case B
                    //   fox movement (= isHeadJumping=true 잔존) 시 사용자 보고 "키 떼면 끊김" = 빨리 떼는
                    //   case 다발 (= 사용자 인지 차이).
                    //   해결: SHIFT 뗌 시점 (= SS-SlideStop 종료 매치) 에 case B fox movement 진행 중이면
                    //     자동 전환 효과 (isAerodynamic=true) 직접 발동 → 사용자 SHIFT timing 무관 길게.
                    //     조건: isHeadJumping=true (= case B fox movement) && !onGround (= air) &&
                    //           !wasHeadJumping (= 자동 전환 첫 발동 시점, fix #17 가드 1:1).
                    if (isHeadJumping && !player.isOnGround() && !wasHeadJumping) {
                        isAerodynamic = true;
                    }
                    // 🔴 fix #41 (2026-05-08, 여우무빙 fix): toCrawling() 호출에 !isHeadJumping 가드.
                    //   Why: 자체 슬라이딩 + 헤드점프 중첩 (= 여우무빙) 종료 시 toCrawling() →
                    //     isCrawling=true (1 tick) → 다음 tick wasCrawling=true + isCrawling 재계산 false
                    //     → L2768 B-35 분기 매치 → heightOffset=-1F→0F reset → 그 다음 tick handleCrash
                    //     진입 시 standupIfPossible 가드 (heightOffset >= 0) 매치 → 즉시 return →
                    //     push up 분기 미진입 → 박스 ground+1m 부유 잔존 → 사용자 인지 "땅 박힘".
                    //   원본 1.7.10 은 mixin offset 없어 박스 +1m up 효과 X → 동일 흐름이지만 박힘 X.
                    //   우리 매핑은 mixin offset 활성 잔존 시점 따라 박스 부유 → push up 필수.
                    //   해결: 헤드점프 진행 중 (= 여우무빙) 시 toCrawling() skip → wasCrawling=false 잔존
                    //     → L2768 B-35 미매치 → heightOffset=-1F 잔존 → 다음 tick handleCrash 시
                    //     standupIfPossible 가드 통과 → standUp 매치 → 정상 push up + dim 갱신.
                    // 🔴 fix #87 v2 (2026-05-12, BUG 추적): fix #87 적용 후 slide→fly transition 시
                    //   *1 frame CRAWL 자세 플리킹*. 원인 = toCrawling() 호출 → isCrawling=T 1 tick →
                    //   setupTransforms 매 frame 6회 호출 중 isCr=T 시점 매치 = CRAWL 분기 (엎드리기 자세).
                    //   비행 진입 시 toCrawling 도 skip → isCr=F 유지 → setupTransforms 다음 frame
                    //   즉시 FLY 분기 → 플리킹 X.
                    //   wouldWantCrawl 의 `!isFlying` 가드 (원본 L2419) 다음 tick 에 어차피 isCr=F
                    //   강제하므로 toCrawling 호출 효과 1 tick 후 사라짐 → skip 해도 동일 결과.
                    if (!isHeadJumping && !isFlying) {
                        wasCrawling = toCrawling();
                    }
                    // 🔴 fix #62 v2 (2026-05-10, dump 분석 — server 박스 정상 확인 후):
                    //   server reconcile 가능성 X (= fix #63 으로 server/client 박스 동등 검증).
                    //   진짜 BUG = SS-SlideStop 종료 직후 *다음 tick* 박스 박힘:
                    //     - dump 시점 (종료 직후): entityY=-61, bbMinY=-60 (= POSE=SLIDING 잔존,
                    //       mixin offset 활성). 박스 정상.
                    //     - 다음 tick: vanilla updatePose → POSE=STANDING → calculateDimensions →
                    //       mixin offset 가드 height>=1 매치 → 차단 → bbMinY = entity.y = -61 →
                    //       박스 ground 안 1m 박힘.
                    //   해결: 종료 시 entity.y +1m push (= 1.7.10/1.12.2 resetHeightOffset 등가).
                    //   case B fox (isHeadJumping=true) 는 fix #58 헤드점프 종료 push 처리.
                    if (!isHeadJumping && this.heightOffset == -1F) {
                        // 🔴 fix #70 (2026-05-10, dump 분석 — 사용자 보고 "슬라이딩→엎드리기 전환 시 땅에 박힘"):
                        //   기존 fix #69 식: predictedMinY < solidBelow (= 박힘 검사). touching 시 false.
                        //   BUG: isSliding 시 box.minY = entity.y+1m (= mixin offset 활성).
                        //     POSE 변경 (SLIDING→SWIMMING) 시 mixin offset 차단 → box.minY = entity.y.
                        //     **box.minY 1m 하강 → 사용자 시각 *땅에 박힘***.
                        //   fix #69 가 *현재 ground 위 정확 (touching)* 시 push X 였지만, 사실은
                        //     POSE 변경 시 box 1m drop 발생 → push 필수.
                        //
                        //   해결: predictive *1m drop* 검사 — currentBoxMinY 와 predictedMinY 차이.
                        //     drop > 0.5m 시 push +1m (= isSliding box → isCrawling box 변화 차단).
                        //
                        //   비행 → 1칸 공간 진입 시나리오 (= entity.y=ground, currentBox=ground+1m):
                        //     drop = ground+1m - ground = 1m → push +1m.
                        //     그러나 push 후 entity.y=ground+1m → box=ground+1m+1m=ground+2m? 아님.
                        //     mixin offset 차단 시 box=entity.y=ground+1m. ✓ 변화 X.
                        double _currentBoxMinY70 = player.getBoundingBox().minY;
                        double _predictedMinY70 = player.getY();
                        boolean _willDrop70 = (_currentBoxMinY70 - _predictedMinY70) > 0.5;
                        if (_willDrop70) {
                            player.setPosition(player.getX(), player.getY() + 1.0, player.getZ());
                            player.lastRenderY += 1.0;
                            player.prevY += 1.0;
                        }
                        this.heightOffset = 0F;
                        player.calculateDimensions();
                        // 🔴 fix #64 (2026-05-10, dump 분석 — 사용자 보고 "전환 사이 카메라 올라갔다가 내려옴"):
                        //   entity.y push +1m 은 즉시. Camera.cameraY field 는 0.5 step lerp 추격
                        //   (= 5 frame 수렴) → 비대칭 spike.
                        //   transition spike: -59.38 → -58.38 (+1m 점프 up) → -58.88 → -59.13 →
                        //     -59.26 → -59.32 → -59.38 (안정). = "올라갔다가 내려옴" 정확.
                        //   해결: 종료 시 cameraY/lastCameraY = isCrawling eyeHeight (0.62) 강제 set
                        //     → cameraY 즉시 0.62 → camera 절대 위치 = entity.y(-60) + 0.62 = -59.38
                        //     (안정 위치 직진).
                        //   ICC 진입 fix / restoreFromFlying fix 와 동일 패턴 (= 메모리
                        //   feedback_standingEyeHeight_cached.md 참조).
                        net.minecraft.client.MinecraftClient _mc64 = net.minecraft.client.MinecraftClient.getInstance();
                        net.minecraft.client.render.Camera _cam64 = (_mc64 != null && _mc64.gameRenderer != null)
                                ? _mc64.gameRenderer.getCamera() : null;
                        if (_cam64 != null) {
                            float _crawlEye = player.getDimensions(net.minecraft.entity.EntityPose.SWIMMING).eyeHeight();
                            ((choco.ratel.smartmoving.mixin.client.MixinCamera) (Object) _cam64).sm_setCameraY(_crawlEye);
                            ((choco.ratel.smartmoving.mixin.client.MixinCamera) (Object) _cam64).sm_setLastCameraY(_crawlEye);
                        }
                    }
                }
            }

            // B-27 (세션 54): 원본 L2569-L2574 fallDistance > _fallingDistanceMinimum 분기.
            //   isSliding && fallDistance > fallingDistanceMinimum →
            //     isSliding=false, wasCrawling=true, isCrawling=false.
            //   SlideToHeadJumping 의 0.05F 임계값보다 훨씬 큰 3F (fallingDistanceMinimum).
            //   둘 다 isSliding 해제지만 SlideToHeadJumping 은 "살짝 낙하 → 헤드점프 전환",
            //   B-27 은 "큰 낙하 → 크롤 전환 준비".
            if (isSliding && player.fallDistance > cfg0.fallingDistanceMinimum) {
                isSliding = false;
                // 🔴 fix #43 (2026-05-09, 사용자 보고 "여우무빙 착지 시 SNEAK 시 땅 반 잠김"):
                //   fix #41 (= SS-SlideStop) 와 동일 패턴 — wasCrawling=true 강제 set 에 isHeadJumping 가드.
                //   Why: 자체 슬라이딩 + 헤드점프 중첩 (= 여우무빙) + 큰 낙하 시 SS-B27 매치 →
                //     wasCrawling=true 강제 set → 다음 tick L2776 B-35 분기 매치 → heightOffset=
                //     -1F→0F reset → 그 다음 tick handleCrash 시 standupIfPossible 가드
                //     (heightOffset >= 0) 매치 → 즉시 return → calculateDimensions 미호출 →
                //     dim.h=0.8 + mixin offset 잔존 → 박스 (entity.y+1, entity.y+1.8) 정상
                //     위치 + 모델 발 (entity.y) = ground - 1m 박힘 → 사용자 인지 "땅 반 잠김".
                //   해결: 헤드점프 진행 중 wasCrawling 변경 skip → L2776 B-35 미매치 →
                //     heightOffset=-1F 잔존 → handleCrash 시 standupIfPossible 가드 통과 →
                //     standUp 매치 → 정상 push up + dim 갱신.
                //   메모리 feedback_server_reconcile_box_sync + project_isclimbcrawling_complete 패턴.
                if (!isHeadJumping) {
                    wasCrawling = true;
                    isCrawling = false;
                }
            }

            // IMPL-03: 더블클릭 방향 점프 카운터 갱신
            // 원본: updateEntityActionState() 내 방향키 StartPressed → count 갱신
            {
                MinecraftClient mc = MinecraftClient.getInstance();
                boolean pressLeft    = mc.options.leftKey.isPressed();
                boolean pressRight   = mc.options.rightKey.isPressed();
                boolean pressBack    = mc.options.backKey.isPressed();
                boolean pressForward = mc.options.forwardKey.isPressed();

                boolean startLeft  = pressLeft  && !prevPressLeft;
                boolean startRight = pressRight && !prevPressRight;
                boolean startBack  = pressBack  && !prevPressBack;

                prevPressLeft  = pressLeft;
                prevPressRight = pressRight;
                prevPressBack  = pressBack;

                // 🔴 (2026-04-27) 원본 SmartMovingSelf L2898-2902 가드 1:1 추가:
                //   canAngleJump = !isSleeping && onGround && !isCrawling && !isClimbing
                //                  && !isClimbCrawling && !isSwimming && !isDiving
                //   canLeftJump  = canSideJump && !rightButton.Pressed
                //   canRightJump = canSideJump && !leftButton.Pressed
                //   canBackJump  = canAngleJump && !forwardButton.Pressed && !isStandupSprintingOrRunning()
                // 가드 false 시 else 분기에서 count = 0 reset (원본 L2917/L2932/L2947).
                boolean canAngleJump = !player.isSleeping()
                        && player.isOnGround()
                        && !isCrawling && !isClimbing && !isCrawlClimbing
                        && !isSwimming_sm && !isDiving;
                boolean canSideJump  = cfg.angleJumpSide && canAngleJump;
                boolean canLeftJump  = canSideJump && !pressRight;
                boolean canRightJump = canSideJump && !pressLeft;
                boolean canBackJump  = cfg.angleJumpBack && canAngleJump
                        && !pressForward && !isStandupSprintingOrRunning(player);

                // 원본: if(StartPressed) { count==0→angleJumpDoubleClickTicks(), else→-1 } else if(count>0) count--
                // 원본 _angleJumpDoubleClickTicks: Positive("...").up(3F, 2F), (int)Math.ceil(value) 정수화.
                int angleTicks = (int) Math.ceil(cfg.angleJumpDoubleClickTicks);
                if (canLeftJump) {
                    if (startLeft) {
                        if (leftJumpCount  == 0) leftJumpCount  = angleTicks; else leftJumpCount  = -1;
                    } else if (leftJumpCount  > 0) leftJumpCount--;
                } else {
                    leftJumpCount  = 0;
                }
                if (canRightJump) {
                    if (startRight) {
                        if (rightJumpCount == 0) rightJumpCount = angleTicks; else rightJumpCount = -1;
                    } else if (rightJumpCount > 0) rightJumpCount--;
                } else {
                    rightJumpCount = 0;
                }
                if (canBackJump) {
                    if (startBack) {
                        if (backJumpCount  == 0) backJumpCount  = angleTicks; else backJumpCount  = -1;
                    } else if (backJumpCount  > 0) backJumpCount--;
                } else {
                    backJumpCount  = 0;
                }

                // 대각선 우선순위: -1 중복 시 -2로 강등 (좌/우+후 동시 방지)
                if (rightJumpCount == -1 && backJumpCount  > 0) rightJumpCount = -2;
                if (leftJumpCount  == -1 && backJumpCount  > 0) leftJumpCount  = -2;
                if (backJumpCount  == -1 && (leftJumpCount > 0 || rightJumpCount > 0)) backJumpCount = -2;
                // -2 → -1 승격 (다른 방향이 해소되면)
                if (rightJumpCount == -2 && backJumpCount  <= 0) rightJumpCount = -1;
                if (leftJumpCount  == -2 && backJumpCount  <= 0) leftJumpCount  = -1;
                if (backJumpCount  == -2 && leftJumpCount  <= 0 && rightJumpCount <= 0) backJumpCount = -1;

                // 🔴 (2026-04-27) 원본 SmartMovingSelf L2963-2964 1:1 추가:
                //   if (sp.onGround || sp.isCollidedVertically) angleJumpType = 0;
                // 점프 후 onGround 복귀 시 angleJumpType reset → isAngleJumping() 즉시 false.
                // 이전 누락 → angleJumpType 잔존으로 애니메이션 잔존 가능.
                if (player.isOnGround() || player.verticalCollision) {
                    angleJumpType = 0;
                }
            }

            // R-04: isSmall 갱신.
            // 원본 SmartMovingSelf L3110 `boolean isSmall = sp.height < 1;` —
            // heightOffset(-1F) 가 적용되면 sp.height = 1.8 + (-1) = 0.8 < 1 → isSmall=true.
            // setHeightOffset(-1F) 호출처 14건 (원본 SmartMovingSelf L511/L518/L1369/L1382/
            // L1390/L1400/L2129/L2512/L2519/L2555/L2798/L2829/L2851/L2858) 의 진입 SM 상태
            // 8 가지: isCrawling/isClimbCrawling/isHeadJumping/isSliding/isSwimming_sm/
            // isDiving/isFlying/isLevitating. 1.21.1 매핑 = 이 8 SM OR.
            //
            // **포커스 #2.7 Phase H (세션 5)**: 이전 식 (isCrawling || isSliding || isHeadJumping)
            // 은 3 SM OR — 5 SM 누락 (isClimbCrawling/isSwimming_sm/isDiving/isFlying/
            // isLevitating). Phase 1 client Mixin (MixinPlayerEntityClient L62-L65) smSmall
            // 식 (8 SM OR) 과 정합 + StatePayload bit 15 isSmall 정확성 확보.
            isSmall = isCrawling || isClimbCrawling
                   || isHeadJumping || isSliding
                   || isSwimming_sm || isDiving
                   || isFlying || isLevitating;

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
            // **포커스 #3 B-1 (세션 2 검증)**: B-19 (isNeighborClimbing 갱신) 이미 완전 이식됨
            //   확인 — SmartMovingClimber.handleClimbing L385-L455 (B-19a4 세션 108) 에서
            //   8방향 seekClimbGap 결과로 매 tick 갱신. 호출: sm_travel_client L177.
            //   진입 조건: cfg.freeClimb || cfg.simpleClimb || cfg.smartClimb (3 클라이밍
            //   모드 중 1개 활성 시). 따라서 본 5-AND 공식 결과도 cfg 활성 시 정상 평가됨.
            //   이전 stale 주석 ("미이식 → 항상 false") 정정.
            {
                boolean _sneakPressed17 = net.minecraft.client.MinecraftClient.getInstance()
                        .options.sneakKey.isPressed();
                boolean _moveForward17 = player.input.movementForward > 0F;
                // 원본 L2736 지역 변수 — 전환 블록에서 사용 (공식 직전 저장).
                boolean _wasCrawlClimbing17 = isCrawlClimbing;
                // 🔴 사다리/덩굴 ICC EXIT 직후 isCrawlClimbing 식 차단 가드 (사용자 보고 fix —
                //   사다리 끝 + sneak → crawl 안 됨):
                //   1.21.1 우리 매핑 박스 발 = ladder maxY 부근 → handleClimbing 결과 isClimbing=true
                //   잔존 → 식 매치 → L1985-L1992 분기 isCrawling=false reset BUG. 원본 1.7.10
                //   박스 발 = ladder maxY+1 (빈공간) → grip 미인식 → 식 미매치. 박스 위치 차이.
                //   해결: iccExitJustToCrawl 시 식 false 강제 → reset 회피. isCrawling=false 자연 시
                //   플래그 reset.
                isCrawlClimbing = !iccExitJustToCrawl
                        && (wasCrawling || isCrawlClimbing)
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
                // 🔴 isCrawlClimbing 가드 추가 (사용자 보고 fix — 크롤 클라이밍 안 올라감/확 빨리 올라감,
                //   2026-05-03):
                //   isCrawlClimbing 시나리오 (= 평지 엎드림 → grab+W+sneak) 에서 ICC 자동 진입 →
                //   박스 +1m offset (mixin offset) → 사용자 시점 +1m 점프. 카운트다운 6 tick 후 ICC EXIT
                //   → setPos(y+1) → entity.y +1m → iccExitJustToCrawl=true 영구 잔존 (mustCrawl 강제로
                //   isCrawling=true 유지 → reset 안 됨) → isCrawlClimbing 식 영구 차단 → "안 올라감".
                //   해결: isCrawlClimbing 활성 시 ICC 진입 차단 → ICC EXIT cycle 자체 안 일어남 →
                //   iccExit 잔존 X → isCrawlClimbing 정상 자가유지.
                //   원본 1.7.10 도 ICC 발동되나 박스 +1m / 시점 +1m 비대칭 없음 (= STANDING eye cached
                //   1.62 유지). 우리 매핑은 dim eye 0.62 ↔ 1.62 토글로 시점 점프 발생.
                boolean canClimbCrawling = wantClimbHolding && wantClimbUp && !isCrawlClimbing;

                if (climbIntoCount > 1) {
                    climbIntoCount--;
                } else if (isClimbCrawling && !needClimbCrawling && climbIntoCount == 0) {
                    climbIntoCount = 6;
                }

                isClimbCrawling = canClimbCrawling
                        && ((needClimbCrawling && climbIntoCount == 0) || climbIntoCount > 1);

                if (isClimbCrawling && !wasClimbCrawling) {
                    // 진입 엣지 (원본 L2797-L2803) — 박스 발 +1m 매핑 (원본 1.7.10 1:1).
                    //
                    // 원본 SmartMovingSelf.setHeightOffset L1694-L1704:
                    //   sp.boundingBox.minY -= heightOffset  // -= -1 = +=1 (박스 발 +1)
                    //   sp.height += heightOffset            // 1.8 - 1 = 0.8 (박스 작아짐)
                    // → 박스 = (foot+1, foot+1.8). 박스 발 +1m, 머리 그대로.
                    // → 핵심: posY (entity 위치) 변경 X. 충돌 박스만 위로 이동. 사용자 시점 그대로.
                    //
                    // 1.21.1 매핑 (사용자 추측 1:1):
                    //   - heightOffset=-1F set → sm_getBaseDimensions inject 가 ICC 시
                    //     dimensions=(0.6, 0.8, eyeHeight=1.62) 반환 (박스 작아짐 + 시점 1.62 유지).
                    //   - calculateDimensions() 명시 호출 → dimensions 즉시 갱신 (vanilla setPose
                    //     는 ICC 시 POSE 변경 안 하므로 자동 호출 안 됨).
                    //   - dimensions 갱신 후 MixinEntity.sm_offsetBoundingBoxForFlying 가드
                    //     (dim.height < 1 && dim.eyeHeight > 1) 통과 → calculateBoundingBox
                    //     결과 box.offset(0, 1, 0) → 박스만 +1 위로 (entity.y 변경 X).
                    //   결과: 박스 = (entity.y+1, entity.y+1.8) ⊆ 좁은 갭. 사용자 시점 그대로.
                    heightOffset = -1F;
                    // 🔴 사용자 의도 매핑 (2026-05-02):
                    //   ICC 활성 = 박스만 작아짐, 사용자 시점 그대로 (= standing 1.62).
                    //   - dim = (0.6, 0.8, 1.62) (sm_getBaseDimensions).
                    //   - MixinEntity.sm_offsetBoundingBoxForFlying 가드 (eyeHeight > 1) 통과 → 박스 +1.
                    //   - 박스 = (entity.y+1, entity.y+1.8) ⊆ 좁은 갭. entity.y 변경 X.
                    //   - 사용자 시점 = entity.y + 1.62 = old.y + 1.62 (변경 X).
                    //   사다리 grip 효과는 sm_travel_client 의 ICC clamp 분기에서 직접 매핑.
                    player.calculateDimensions();
                    // 원본 L2800: `boolean wasCollidedHorizontally = sp.isCollidedHorizontally;`
                    boolean wasColH = player.horizontalCollision;
                    // 원본 L2801: move(0, 0.05, 0) — solid 머리 위에 서있을 때 crawl 진입 방지
                    double iccEnterYBefore = player.getY();
                    player.move(MovementType.SELF, new Vec3d(0, 0.05, 0));
                    // 🔴 entity.y 복원 (사용자 보고 fix — grab climbing 중 sneak 시 카메라 움찔움찔):
                    //   ICC 진입/해제 매 tick 진동 (= isClimbing/isClimbHolding 매 tick true↔false) 시
                    //   매 ICC ENTER edge 마다 player.move(0, 0.05, 0) → entity.y +0.05m 누적.
                    //   다음 tick baseTick 가 prevY=y 갱신 → 매 tick 경계 lerp baseline +0.05m 점프 →
                    //   사용자 시점 매 tick 0.05m 진동 = "움찔움찔".
                    //   이전 fix v1 (lastRenderY/prevY 동기화) 시도 → 같은 tick 안 frame Δ 일정 했지만
                    //   tick 경계는 baseTick 가 prevY=y 강제 갱신 → +0.05m 점프 잔존.
                    //   해결: player.move 의 entity.y 변경 자체 cancel (setPosition 복원). 박스 위치
                    //   (= mixin offset +1m) + horizontalCollision 효과 보존. 원본 의도 = entity.y
                    //   +0.05m 영구 적용 (= 박스 천장 close 검사 안전 마진) 부분 손실 가능.
                    if (player.getY() != iccEnterYBefore) {
                        player.setPosition(player.getX(), iccEnterYBefore, player.getZ());
                    }
                    // 원본 L2802: sp.isCollidedHorizontally = wasCollidedHorizontally;
                    //   (water 밖으로 crawl 탈출 버그 방지)
                    player.horizontalCollision = wasColH;
                    // 🔴 ICC 진입 cameraY 강제 set (사용자 보고 fix — 크롤 클라이밍 확 빨리 올라감, 2026-05-03):
                    //   isCrawlClimbing 시나리오 → ICC 자동 진입 → dim eye 0.62 → 1.62 변화 →
                    //   vanilla Camera.updateEyeHeight 가 cameraY 를 0.5 step lerp 추격 →
                    //   6+ frame 동안 사용자 시점 +1m 점진 점프 = "확 올라가는 느낌".
                    //   해결: ICC 해제 시 cameraY 0.62 강제 fix 와 동일한 패턴 — ICC 진입 직후
                    //   cameraY/lastCameraY = ICC eye (1.62) 강제 set → lerp 차단 → 시점 안정.
                    //   getDimensions(STANDING).eyeHeight() 사용 (= cached getStandingEyeHeight 회피,
                    //   메모리 feedback_standingEyeHeight_cached.md).
                    {
                        net.minecraft.client.render.Camera camEnter =
                                net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera();
                        if (camEnter != null) {
                            float iccEye = player.getDimensions(net.minecraft.entity.EntityPose.STANDING).eyeHeight();
                            ((choco.ratel.smartmoving.mixin.client.MixinCamera) (Object) camEnter).sm_setCameraY(iccEye);
                            ((choco.ratel.smartmoving.mixin.client.MixinCamera) (Object) camEnter).sm_setLastCameraY(iccEye);
                        }
                    }
                } else if (!isClimbCrawling && wasClimbCrawling) {
                    // 해제 엣지 (원본 L2804-L2820) — B-42-B18a 해소 완전 이식 (AABB 정밀).
                    climbIntoCount = 0;
                    // 🔴 sneak hold 가드 (사용자 보고 fix — grab 떼고 sneak hold 시 W 뗀 시점 떨어짐 BUG):
                    //   ICC 활성 중 사용자 W 뗌 → wantClimbUp=false → canClimbCrawling=false → ICC EXIT.
                    //   ICC EXIT toCrawling → 박스 (entity.y, entity.y+0.8) + isCrawling=true → 사용자
                    //   "엎드리며 grab 풀림". 사용자 의도 = 그 자리 hold.
                    //   해결: isClimbHolding=true && fwd<=0 && !mustCrawl 시 ICC EXIT 분기 skip +
                    //   isClimbCrawling=true 강제 유지 → ICC 활성 박스 + dim 1.62 + mixin offset 박스 +1
                    //   유지. 다음 tick wasICC=true && ICC 식 결과 false → 또 가드 활성 → 무한 hold.
                    //   사용자 sneak release 또는 W 누르거나 grab 누름 시 가드 false → 정상 처리.
                    boolean sneakHoldGuard = isClimbHolding
                            && player.input.movementForward <= 0F
                            && !mustCrawl;
                    if (sneakHoldGuard) {
                        // ICC 활성 박스 + dim 매핑 유지 위해 isClimbCrawling=true 강제 복원.
                        isClimbCrawling = true;
                    } else if (mustCrawl || sneakPressedRaw || crawlToggled) {
                        // 🔴 gap 검사 후 분기 (원본 L2807-L2819 1:1 — 사용자 보고 fix):
                        //   원본: gap 측정 → gap < 1 시 toCrawling, gap >= 1 시 resetHeightOffset.
                        //   사용자 시나리오 — sneak hold 중 좌/우/후 이동으로 grab 블록 영역 밖 → 박스 발
                        //   아래 빈공간 (gap >= 1) → 원본은 resetHeightOffset (= 박스 1.8 복원) 으로
                        //   떨어지면서 sneak. 우리 매핑이 강제 toCrawling 했음 → 잘못된 엎드리기 BUG.
                        Box bbExit = player.getBoundingBox();
                        double exitGap = bbExit.minY
                                - getMaxPlayerSolidBetween(player, bbExit.minY - 1D, bbExit.minY, 0);
                        // 🔴 사다리/덩굴 끝 + sneak crawl 진입 시 RESET-HEIGHT 분기 차단 가드:
                        //   사용자 보고 — 사다리 끝 + sneak 시 콜리전 한번 커졌다가 작아짐.
                        //   진동 cycle: 첫 ICC EXIT (gap<1) → TO-CRAWLING → entity.y+1m + isCrawling=true.
                        //   다음 ICC EXIT 시 박스 발 = entity.y+1+1 = 사다리 위 위 → gap=1m → 일반
                        //   RESET-HEIGHT 분기 진입 → 박스 1.8 잠시 → 다음 tick B-35 진입 → 박스 0.8.
                        //   해결: gap>=1 분기에 !isCrawling 가드 추가. isCrawling=true 잔존 (= 사다리/
                        //   덩굴 끝 + sneak 케이스) 시 RESET-HEIGHT 차단 → toCrawling 강제 → 박스 0.8
                        //   유지. 좌/우/후 이동 (= isCrawling=false) 케이스는 기존 분기 유지 (= Fix 4).
                        if (!(exitGap >= 0D && exitGap < 1D) && !isCrawling) {
                            // gap >= 1 (또는 음수) && isCrawling=false → 박스 발 아래 빈공간 (= 좌/우/후
                            //   이동 standing 떨어짐) → resetHeightOffset.
                            heightOffset = 0F;
                            player.calculateDimensions();
                        } else {
                        // 🔴 ICC EXIT toCrawling 매핑 (원본 1.7.10 setHeightOffset(-1F) 1:1):
                        //   원본 1.7.10: posY 변경 X + box.minY +=1 + height -=1 + ySize/yOffset 동기화.
                        //   효과 = 박스 +1m 위, 사용자 시점 변경 X.
                        //
                        //   1.21.1 매핑: entity.y +=1 + dim 0.62 (smSmall) → 박스 = (entity.y, entity.y+0.8)
                        //   = (old+1, old+1.8) = ICC 활성 박스 위치 동일. 사용자 시점 = entity.y + 0.62
                        //   = old+1.62 = ICC 시점 동일.
                        //
                        //   X/Z 땡김 fix (server reconcile 차단):
                        //     ICC 활성 중 클라 박스 (mixin offset 박스 +1) ↔ 서버 박스 1m Y 차이 →
                        //     서버 ladder collision 차단 → server position correction → 클라 reset.
                        //     해결: SmartMovingState bit 34 (isClimbCrawling) 추가 동기화 +
                        //     MixinPlayerEntity.sm_getBaseDimensions_server 에 isClimbCrawling 분기
                        //     추가 (= 클라와 동일 dim 1.62 + mixin offset 박스 +1) → 서버 박스 동기화.
                        wasCrawling = toCrawling();
                        // 🔴 사다리/덩굴 grip 회피 가드 (사용자 보고 fix — 사다리 끝 + sneak crawl 안 됨):
                        //   ICC EXIT 직전 isClimbing=true → 사다리/덩굴 grip 영역. bottom snap 적용 시
                        //   박스 발 = ladder maxY 표면 → 다음 tick handleClimbing → isClimbing=true 갱신
                        //   → L1970 isCrawlClimbing 식 매치 (wasCrawling=true && isClimbing=true && ...)
                        //   → L1985 _canStandUp=false (= ladder solid) → L1992 isCrawling=false reset → BUG.
                        //   원본 1.7.10 = setHeightOffset(-1F) 매핑 = posY 변경 X + box.minY +=1 → 박스 발
                        //   = posY+1 = ladder maxY+1 (= 빈공간 위). ladder grip 미인식. 자연 isClimbing=false.
                        //   해결: 사다리/덩굴 grip 시 snap 미적용 → 박스 발 = entity.y_new = old+1 = ladder
                        //   maxY+1 유지. 일반 블록 케이스 (isClimbing=false) snap 적용 정상.
                        boolean wasClimbingBeforeFix = isClimbing;
                        // 🔴 사다리/덩굴 ICC EXIT 케이스 fix (사용자 보고 — 사다리 끝 + sneak → crawl 진입 안 됨):
                        //   사다리/덩굴 ICC EXIT 시점 isClimbing=true 잔존 (= 사다리 grip 마지막 tick).
                        //   setPos(y+1) + calculateDimensions 시점 dim 매핑 결정:
                        //     L97 분기 `isClimbCrawling || (isCrawling=true && isClimbing=true)` 매치 →
                        //     dim 1.62 → mixin offset 박스 +1 적용 → 박스 = (entity.y+1, entity.y+1.8) =
                        //     (old.y+2, old.y+2.8) → **박스 +2m double mixin offset BUG**.
                        //   일반 블록 ICC 케이스는 isClimbing=false 라 smSmall 매치 → dim 0.62 → mixin
                        //   offset 미적용 → 박스 정상. 사다리/덩굴 케이스만 BUG.
                        //
                        //   해결: setPos + calculateDimensions 전에 isClimbing=false 강제 → dim 매핑이
                        //   smSmall (isCrawling) 매치 → dim 0.62 → mixin offset 미적용 → 박스 정상.
                        //
                        //   같은 tick L1683 isCrawling 재계산 BUG (Feature 2 동일 패턴) 도 함께 차단:
                        //     `canCrawl = !isClimbing` 가드 통과 (isClimbing=false) + mustCrawl=true 강제
                        //     → `canCrawl && (false || true)` = true 유지. 다음 tick 부터는 박스 위치
                        //     검사로 자연 유지.
                        // 🔴 isClimbHolding=true 가드 추가 (사용자 보고 fix — grab sneak hold 깨짐):
                        //   사용자 grab 떼고 sneak hold 시 isClimbHolding=true. ICC EXIT 처리에서
                        //   isClimbing=false 강제 시 isClimbHolding = wantClimbHolding && isClimbing
                        //   재계산 → false → 자가 hold 메커니즘 깨짐 → 떨어짐 BUG.
                        //   해결: isClimbHolding=true 시 (= 자가 hold) isClimbing=false 강제 미적용.
                        //   사다리 grip 잔존 케이스 (= isClimbHolding=false) 만 강제 → Feature 3
                        //   사다리 ICC EXIT crawl 진입 fix 유지.
                        // 🔴 isClimbHolding 가드 제거 (사용자 보고 fix — 사다리 끝 + sneak 시 콜리전 한번 커짐):
                        //   기존 가드: isClimbHolding=true 시 isClimbing=true 잔존 → dim 매핑 =
                        //   (isCrawling && isClimbing) 매치 → ICC dim + mixin offset 박스 +1m →
                        //   entity.y +1m + 박스 +1m 위로 → 사용자 시점 +1m 점프 BUG.
                        //   해결: isClimbing=false 항상 강제 → dim 매핑 = smSmall (isCrawling) 매치 →
                        //   crawl dim (eye=0.62) + mixin offset 미적용 → 박스 = (entity.y, entity.y+0.8)
                        //   = (old+1, old+1.8) = ICC 박스 위치 동일 → 사용자 시점 변화 X (= 덩굴 케이스
                        //   와 동일).
                        //   회귀 우려 분석:
                        //   - 자가 hold (= grab 떼고 sneak hold, fwd<=0) 시나리오 = sneakHoldGuard
                        //     분기 (L2137-L2142) 매치 → ICC EXIT 분기 미진입 → fix 영향 없음.
                        //   - 자가 hold + W (= fwd>0) 시나리오 = 사다리 climbing 끝 도달 후 crawl 진입
                        //     의도 (= 메모리 명시). isClimbing=false 강제 = 의도 일치.
                        this.isClimbing = false;
                        this.mustCrawl = true;
                        // 🔴 iccExit fwd>0 가드 (사용자 보고 fix — isClimbCrawling → isCrawlClimbing
                        //   자연 전환 차단 BUG, 2026-05-03):
                        //   본래 iccExit 의도 = 사다리/덩굴 끝 + sneak hold (fwd<=0) 시 다음 tick
                        //   isCrawlClimbing 식 잘못 매치 → L1992 isCrawling reset BUG 차단
                        //   (project_ladder_vine_iccexit_complete.md).
                        //   그러나 fwd>0 (= climbing 계속 의도) 시나리오 = 좁은 갭 통과 후 사용자
                        //   의도 isCrawlClimbing 자연 전환. iccExit set 시 식 영구 차단 → 진동 BUG.
                        //   해결: fwd<=0 시만 set → 사다리/덩굴 끝 hold 케이스 한정 → 좁은 갭 통과
                        //   시나리오 자연 전환 허용. 원본 1.7.10 은 ICC EXIT 분기에 isClimbing/
                        //   mustCrawl/iccExit 같은 강제 set 없음 (= 자연 전환).
                        if (player.input.movementForward <= 0F) {
                            this.iccExitJustToCrawl = true;
                        }
                        player.setPosition(player.getX(), player.getY() + 1.0, player.getZ());
                        player.lastRenderY += 1.0;
                        player.prevY += 1.0;
                        player.calculateDimensions();
                        // Camera.cameraY 보간 baseline 보정 (= 1.62 → 0.62 lerp 점프 차단).
                        net.minecraft.client.render.Camera cam =
                                net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera();
                        if (cam != null) {
                            float eye = player.getStandingEyeHeight();
                            ((choco.ratel.smartmoving.mixin.client.MixinCamera) (Object) cam).sm_setCameraY(eye);
                            ((choco.ratel.smartmoving.mixin.client.MixinCamera) (Object) cam).sm_setLastCameraY(eye);
                        }
                        // 박스 = (entity.y, entity.y+0.8). 1m 아래 솔리드 max → bottom snap (원본 L2813).
                        // 🔴 사다리/덩굴 케이스 (wasClimbingBeforeFix=true) snap 미적용 — 박스 발이 ladder maxY
                        //   표면에 정렬되면 다음 tick handleClimbing 결과 isClimbing=true 재갱신 → isCrawlClimbing
                        //   식 매치 → B-17b1 분기 isCrawling=false reset BUG. 박스 발 ladder maxY+1 위 유지.
                        if (!wasClimbingBeforeFix) {
                            double minY18 = player.getBoundingBox().minY;
                            double gapUnderneight = minY18
                                    - getMaxPlayerSolidBetween(player, minY18 - 1D, minY18, 0);
                            if (gapUnderneight >= 0D && gapUnderneight < 1D) {
                                player.move(MovementType.SELF, new Vec3d(0, -gapUnderneight, 0));
                            }
                        }
                        }  // toCrawling 분기 (gap < 1) 닫기
                    } else {
                        // sneak 안 누름 — 박스 1.8 복원 (원본 resetHeightOffset).
                        heightOffset = 0F;
                        player.calculateDimensions();
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
                double b35Dy = crawlStandUpBottom - minY;
                // 🔴 (벽 충돌 fix, 2026-05-13) onGround + dy<0 시 push skip.
                //   사용자 보고 BUG: REMOTE 측 slide → 벽 충돌 시 모델 1m down lerp 추격 (= ~15 tick
                //     모델 위치 변동 = 시각 "모델 덜컹").
                //   root cause (= self side dump 검증, stack trace L2991):
                //     - 벽 충돌 → mustCrawl=true 자동 crawl 진입.
                //     - crawl 자동 종료 frame (= B-35 분기) 에 getMaxPlayerSolidBetween 측정.
                //     - 벽 충돌 시 vanilla collision 으로 bb.X/Z 정렬 + horizontalTolerance -0.05 좁힘
                //       가드 → ground block X/Z range 매트릭스 매치 X → fallback yMin (= bb.minY-1).
                //     - b35Dy = (minY-1) - minY = -1m → player.move(-1) → self.y -1m drop.
                //     - server broadcast → remote.y lerp 71→70 추격 ~15 tick = 모델 덜컹.
                //   원래 의도 (= project_crawl_release_camera_jump_fix): 떨어지면서 풀림 시
                //     (= onG=false + solid 없음) -1m drop 정상 (= 박스 발 정렬).
                //   해결: onGround=true + dy<0 시 push skip. 떨어지면서 풀림 (= onG=false) 시 적용.
                if (b35Dy < 0D && player.isOnGround()) {
                    // skip — 지면 위 + dy<0 = 벽 충돌 시 fallback 잘못 매트릭스. self.y 보존.
                } else {
                player.move(MovementType.SELF, new Vec3d(0, b35Dy, 0));
                // 🔴 Camera lerp 점프 차단 (사용자 보고 fix — 엎드린 채 떨어지면서 풀림 시 1인칭 덜컹):
                //   B-35 분기 `move(0, crawlStandUpBottom - minY, 0)` = 박스 발 정렬. 떨어지는 중
                //   solid 없으면 dy=-1m → entity.y -1m. lastRenderY/prevY 미동기화 → lerp baseline
                //   1m 차이 → 시점 점프. + dim eyeHeight 0.62 → 1.62 변화로 cameraY lerp 0.5 추격
                //   → +0.5m 시점 점프. 사용자 "덜컹" 인식.
                //   원본 1.7.10: setHeightOffset 매핑 = box.minY 만 변경 (entity.posY 변경 X) +
                //   lerp 보간 없음 → 시점 점프 0.
                //   해결: dy 만큼 lastRenderY/prevY 동기화 + Camera.cameraY/lastCameraY = STANDING
                //   dim eye 직접 사용 (= 1.62, getStandingEyeHeight() cached 우회) 강제.
                if (b35Dy != 0D) {
                    player.lastRenderY += b35Dy;
                    player.prevY += b35Dy;
                    net.minecraft.client.render.Camera cam =
                            net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera();
                    if (cam != null) {
                        float eye = player.getDimensions(net.minecraft.entity.EntityPose.STANDING).eyeHeight();
                        ((choco.ratel.smartmoving.mixin.client.MixinCamera) (Object) cam).sm_setCameraY(eye);
                        ((choco.ratel.smartmoving.mixin.client.MixinCamera) (Object) cam).sm_setLastCameraY(eye);
                    }
                }
                }  // 벽 충돌 fix else 분기 닫기.
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
                } else if (isDipping && wouldWantCrawl && dippingDepth >= SWIM_CRAWL_WATER_BOTTOM_BORDER) {
                    if (dippingDepth >= SWIM_CRAWL_WATER_MEDIUM_BORDER) {
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
        prevGrabKeyPressed  = false;
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
        // BUG-19 (세션 36): 점프 관련 잔존 상태 reset — sm_jump 가로채기 / handleJumping 의
        //   세팅 잔존이 SM disabled 진입 후에도 유지되는 위험 차단.
        jumpAvoided                  = false;
        jumpPending                  = false;
        blockJumpTillButtonRelease   = false;
        jumpCharge                   = 0F;
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
    public static double getMaxPlayerSolidBetween(net.minecraft.client.network.AbstractClientPlayerEntity player,
                                                   double yMin, double yMax,
                                                   double horizontalTolerance) {
        Box pb = player.getBoundingBox();
        Box checkBox = new Box(
                pb.minX - horizontalTolerance, yMin, pb.minZ - horizontalTolerance,
                pb.maxX + horizontalTolerance, yMax, pb.maxZ + horizontalTolerance);

        // **B-42a 근사 해소 (세션 135)**: 원본 L247-L262 박스 순회는 블록당 단일 AABB.
        // 1.21.1 VoxelShape 는 multi-shape 가능 → `getBoundingBoxes()` 리스트 전수 순회로
        // 정밀화. wall/fence/chain 같은 블록에서 외접 box 오차 해소.
        double result = yMin;
        double minXLimit = pb.minX - horizontalTolerance;
        double maxXLimit = pb.maxX + horizontalTolerance;
        double minZLimit = pb.minZ - horizontalTolerance;
        double maxZLimit = pb.maxZ + horizontalTolerance;
        for (net.minecraft.util.shape.VoxelShape shape
                : player.getWorld().getBlockCollisions(player, checkBox)) {
            if (shape.isEmpty()) continue;
            for (Box box : shape.getBoundingBoxes()) {
                // 원본 L299-L306 `isCollided(box, yMin, yMax, horizontalTolerance)` 인라인
                if (box.maxX >= minXLimit
                        && box.minX <= maxXLimit
                        && box.maxY >= yMin
                        && box.minY <= yMax
                        && box.maxZ >= minZLimit
                        && box.minZ <= maxZLimit) {
                    result = Math.max(result, box.maxY);
                }
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
    // 🔴 (BUG D fix, 2026-05-13) signature 확장: ClientPlayerEntity → AbstractClientPlayerEntity.
    public static double getMinPlayerSolidBetween(net.minecraft.client.network.AbstractClientPlayerEntity player,
                                                   double yMin, double yMax,
                                                   double horizontalTolerance) {
        Box pb = player.getBoundingBox();
        Box checkBox = new Box(
                pb.minX - horizontalTolerance, yMin, pb.minZ - horizontalTolerance,
                pb.maxX + horizontalTolerance, yMax, pb.maxZ + horizontalTolerance);

        // **B-42b 근사 해소 (세션 135)**: VoxelShape.getBoundingBoxes() 전수 순회 — B-42a 대칭.
        double result = yMax;
        double minXLimit = pb.minX - horizontalTolerance;
        double maxXLimit = pb.maxX + horizontalTolerance;
        double minZLimit = pb.minZ - horizontalTolerance;
        double maxZLimit = pb.maxZ + horizontalTolerance;
        for (net.minecraft.util.shape.VoxelShape shape
                : player.getWorld().getBlockCollisions(player, checkBox)) {
            if (shape.isEmpty()) continue;
            for (Box box : shape.getBoundingBoxes()) {
                if (box.maxX >= minXLimit
                        && box.minX <= maxXLimit
                        && box.maxY >= yMin
                        && box.minY <= yMax
                        && box.maxZ >= minZLimit
                        && box.minZ <= maxZLimit) {
                    result = Math.min(result, box.minY);
                }
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
     *   - `FluidState.isIn(FluidTags.WATER/LAVA)` + `getHeight(world, pos)` → 액체 높이 (0~1)
     *   - **§7 근사 (B-42c-a-mod)**: FiniteLiquid mod 분기 생략 (mod 1.21.1 미이식 — 영구).
     *   - ~~**§7 근사 (B-42c-b)**: lava 처리 생략~~ → **포커스 #2.6 세션 2 해소** (A-1):
     *     `fluid.isIn(FluidTags.LAVA) && lavaLikeWater ? fluid.getHeight : 0F` 분기 복원.
     *     원본 SmartMovingBase L139-L140 + L142-L144 통합 (lava block + Material.lava).
     *   - **§7 근사 (B-42c-c)**: Normal/Material.water 분기 통합 — FluidState 는 항상
     *     `getHeight()` 반환. `getNormalWaterBorder` 의 metadata 기반 구분
     *     (`>=8→1F / ==0+air→0.8875F / 기타→(8-meta)/8F`) 은 1.21.1 `FluidState.getHeight`
     *     내부 로직에 흡수됨 (FlowableFluid 구현). 동치 — 영구 근사.
     *   - ~~modded liquid (`material.isLiquid()` → 1F) 누락~~ → **포커스 #2.6 세션 2 해소** (A-2):
     *     water/lava 도 아닌 fluid 발견 시 `1F` 반환. Petroleum/Oil/Honey 등 modded fluid 지원.
     *
     * B-42c (세션 119) — Phase 6 세 번째 원자 (액체 경계 헬퍼).
     * 포커스 #2.6 A-1+A-2 (세션 2): lava + modded 분기 복원, §7 근사 B-42c-b 해소.
     */
    private static float getLiquidBorder(ClientPlayerEntity player, int i, int j, int k) {
        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(i, j, k);
        net.minecraft.fluid.FluidState fluid = player.getWorld().getFluidState(pos);

        // 분기 1 (원본 L135-L136): empty → 0F
        if (fluid.isEmpty()) return 0F;

        // 분기 2 (원본 L135-L136 + L145-L146): water (water + flowing_water + Material.water 통합)
        if (fluid.isIn(net.minecraft.registry.tag.FluidTags.WATER)) {
            return fluid.getHeight(player.getWorld(), pos);
        }

        // 분기 3 + 4 (원본 L139-L140 + L142-L144): lava (lava + flowing_lava + Material.lava 통합)
        //   _lavaLikeWater ? getNormalWaterBorder : 0F
        //   1.21.1 fluid.getHeight 가 LavaFluid 도 올바른 높이 반환 (level 기반).
        //   포커스 #2.6 A-1: §7 근사 B-42c-b 해소 (lava 분기 복원, 세션 2).
        if (fluid.isIn(net.minecraft.registry.tag.FluidTags.LAVA)) {
            SmartMovingConfig cfg = SmartMovingConfig.Config;
            return cfg.isLavaLikeWaterEnabled() ? fluid.getHeight(player.getWorld(), pos) : 0F;
        }

        // 분기 6 (원본 L147-L148): modded liquid (`material.isLiquid()` → 1F)
        //   1.21.1 `!fluid.isEmpty()` 가 modded fluid 모두 포함. water/lava 둘 다 false 라면
        //   modded fluid (Petroleum/Oil/Honey 등) — 원본은 1F (꽉찬 fluid) 반환.
        //   포커스 #2.6 A-2: §7 근사 B-42c-a-modded 해소 (modded 분기 복원, 세션 2).
        return 1F;
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
        // 🔵 (2026-05-20, BUG 2 fix #104) 박스 측정 좌표계 — entity.y 기준 STANDING 박스 직접 사용.
        //   원본 1.7.10 mechanism: handleSwimming L256 `j = floor(sp.boundingBox.minY)` 측정 시점 =
        //     swim 분기 진입 시 박스 (= heightOffset=0 잔존, = entity.y 기준 STANDING).
        //     `setHeightOffset(-1F)` 적용 (L511) 은 swim 분기 *마지막* — 측정/박스 변경 분리.
        //   1.21.1 매핑 BUG: fix #102 적용 후 `player.getBoundingBox()` 가 mixin offset 활성 박스
        //     (= box.minY = entity.y + 1m) 반환 → playerSwimWaterBorder 1m 작게 계산 → 사용자
        //     보고 "수면 1칸 아래까지만 올라감". 측정-박스 변경 분리 메커니즘 못 구현.
        //   해결: player.getY() (= entity.y, mixin offset 무관) 직접 사용. STANDING height=1.8
        //     hardcoded. 측정 = 원본 standing 박스 1:1.
        double standingMinY = player.getY();
        double standingMaxY = player.getY() + 1.8;
        int i = net.minecraft.util.math.MathHelper.floor(player.getX());
        int j = net.minecraft.util.math.MathHelper.floor(standingMinY);
        int k = net.minecraft.util.math.MathHelper.floor(player.getZ());
        double j_offset = standingMinY - j;

        double totalSwimWaterBorder      = getMaxPlayerLiquidBetween(player, standingMaxY - 1.8, standingMaxY + 1.2);
        double minPlayerSwimWaterCeiling = getMinPlayerSolidBetween(player, standingMaxY - 1.8, standingMaxY + 1.2, 0);
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
    public boolean vanilla() {
        // 가시성 변경 (private → public, 포커스 #2.5 Phase D-9 의존, 세션 19):
        //   tryJump 의 D-9 (`if (type == Up && sm.vanilla())`) 외부 호출.
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
     * 원본 `SmartMovingBase.getGapUnderneight()` L845-L848 정밀 이식.
     *   return sp.boundingBox.minY - getMaxPlayerSolidBetween(minY - 1.1D, minY, 0);
     *
     * 발 아래 고체까지 거리 (0~1.1D). `groundClose = gap < 1D` 판정용.
     * B-N-standup-3 (세션 136) — B-42a 헬퍼 소비로 §7 근사 해소.
     */
    // 🔴 (BUG D fix, 2026-05-13) signature 확장: ClientPlayerEntity → AbstractClientPlayerEntity.
    //   remote (= AbstractClientPlayerEntity, !ClientPlayerEntity) 에서도 gap 측정 가능 →
    //   비행 종료 시 self side standupIfPossible 1:1 매핑.
    public static double getGapUnderneight(net.minecraft.client.network.AbstractClientPlayerEntity player) {
        Box bb = player.getBoundingBox();
        // 🔴 yMax clamping 회피 fix: yMax = bb.minY + 0.5 (박스 발 위 0.5m 까지 검사).
        //   원본 yMax = bb.minY 였지만, getMaxPlayerSolidBetween 의 Math.min(result, yMax)
        //   clamping 때문에 박스 발이 솔리드 안 살짝 박힌 case (vanilla 충돌 처리 epsilon
        //   등) 에서 solidMax 가 bb.minY 로 잘못 clamp → gap=0 잘못 산출 → standUp 의
        //   entity.y +=1 보정 후도 박스 발이 솔리드 안 박힌 채 잔존 (가라앉음).
        //   fix: yMax 를 박스 발 위 0.5m 로 → solidMax 가 bb.minY 보다 위에 있으면 음수 gap
        //   측정 가능 → standUp 의 entity.y += (1-(-gap)) = +1+gap 보정으로 정확히 solidMax
        //   위치 도달.
        return bb.minY - getMaxPlayerSolidBetween(player, bb.minY - 1.1D, bb.minY + 0.5D, 0);
    }

    /**
     * 원본 `SmartMovingBase.getGapOverneight()` L850-L853 정밀 이식.
     *   return getMinPlayerSolidBetween(maxY, maxY + 1.1D, 0) - maxY;
     *
     * 머리 위 고체까지 거리 (0~1.1D). `standUpPossible = gap + overGap >= 1D` 판정용.
     * B-N-standup-3 (세션 136) — B-42b 헬퍼 소비로 §7 근사 해소.
     */
    // 🔴 (BUG D fix, 2026-05-13) signature 확장: ClientPlayerEntity → AbstractClientPlayerEntity.
    public static double getGapOverneight(net.minecraft.client.network.AbstractClientPlayerEntity player) {
        Box bb = player.getBoundingBox();
        return getMinPlayerSolidBetween(player, bb.maxY, bb.maxY + 1.1D, 0) - bb.maxY;
    }

    /**
     * 원본 L2214-L2219 `standUp(double gapUnderneight)` 정밀 이식:
     *   move(0, (1D - gapUnderneight), 0, true);
     *   isCrawling = false;
     *   isHeadJumping = false;
     *   resetHeightOffset();
     *
     * heightOffset=-1F 상태의 hitbox 를 원위치 복원 + 바닥 스냅. 1.21.1 vanilla POSE 가
     * hitbox 자체는 자동 복원하므로 `move` 는 바닥 스냅 전용 효과.
     * B-N-standup-3 (세션 136).
     */
    public void standUp(ClientPlayerEntity player, double gapUnderneight) {
        // 🔴 원본 1:1 매핑 (SmartMovingSelf L2214-L2219):
        //   move(0, 1-gap, 0): entity.posY += (1-gap). 박스 평행이동.
        //   resetHeightOffset: boundingBox.minY -= -1 → 박스 minY = entity.posY_new.
        //   결과: entity.posY_new = entity.posY_old + (1-gap) = solidMax (땅).
        //
        //   1.21.1 매핑: setPosition(entity.y + (1-gap)) + calculateDimensions(STANDING)
        //   (calculateDimensions 는 standupIfPossible 끝에서 호출).
        //
        //   ⚠️ 가라앉음 fix: gap 측정 정확성에 의존. getGapUnderneight 의 yMax clamping
        //   (Math.min(result, yMax) 에서 yMax=bb.minY) 때문에 박스 발이 솔리드 안 살짝 박힌
        //   case 에서 gap=0 잘못 산출 → 보정 부족 → 가라앉음. getGapUnderneight 의 yMax 를
        //   bb.minY+0.5 로 정정해 음수 gap 측정 가능 → 정확한 보정.
        player.setPosition(player.getX(), player.getY() + (1D - gapUnderneight), player.getZ());
        this.isCrawling    = false;
        this.isHeadJumping = false;
        resetHeightOffset();
    }

    /**
     * 원본 L2222-L2230 `toSlidingOrCrawling(double gapUnderneight)` 정밀 이식:
     *   move(0, (-gapUnderneight), 0, true);
     *   if (Config.isSlidingEnabled() && (grabButton.Pressed || wasHeadJumping))
     *       isSliding = true;
     *   else
     *       wasCrawling = toCrawling();
     *
     * 서기 공간 부족 시 슬라이딩(grab + slide 활성 or 헤드점프 착지) 또는 크롤링 전환.
     * B-N-standup-3 (세션 136).
     */
    public void toSlidingOrCrawling(ClientPlayerEntity player, double gapUnderneight) {
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        // 원본 L2224: move(0, -gapUnderneight, 0, true)
        player.move(MovementType.SELF, new Vec3d(0, -gapUnderneight, 0));
        // 원본 L2226-L2229: grabPressed || wasHeadJumping 이면 isSliding, 아니면 toCrawling
        if (cfg.slide && cfg.enabled
                && (SmartMovingKeys.grab.isPressed() || this.wasHeadJumping)) {
            // 🔴 (2026-05-04 사용자 보고 — "비행 → grab+sneak 착지 슬라이딩 시 90° 꺾임 +
            //   엎드리기 애니"): 진입 시점에 isCrawling=true 잔존 (다른 path 가 set 한 것)
            //   할 수 있음. 원본은 standupIfPossible 호출 시점 isCrawling=false 보장 invariant.
            //   우리 매핑은 잔존 가능 → setupTransforms 가 sliding R_x(-π/2) + crawling
            //   R_x(-78.75°) 둘 다 적용 (별도 if) → 90° 꺾임. setAngles 도 isCrawling 분기
            //   가 isSliding 보다 먼저 (= 엎드리기 자세). 진입 시 isCrawling=false 명시.
            this.isCrawling = false;
            this.isSliding = true;
        } else {
            this.wasCrawling = this.toCrawling();
        }
    }

    /**
     * 원본 L2165-L2184 `standupIfPossible()` 무인자 오버로드 정밀 이식:
     *   if (heightOffset >= 0) return;
     *   double gap = getGapUnderneight();
     *   boolean groundClose = gap < 1D;
     *   if (!groundClose) resetHeightOffset();
     *   else {
     *       double overGap = getGapOverneight();
     *       boolean standUpPossible = gap + overGap >= 1D;
     *       if (standUpPossible) standUp(gap);
     *       else toSlidingOrCrawling(gap);
     *   }
     *
     * B-N-standup-3 해소 (세션 136): AABB 정밀 gap 측정 복원. 기존 `canStandUp` 근사 제거.
     */
    public void standupIfPossible(ClientPlayerEntity player) {
        if (this.heightOffset >= 0) return;

        double gapUnderneight = getGapUnderneight(player);
        boolean groundClose = gapUnderneight < 1D;
        if (!groundClose) {
            resetHeightOffset();
        } else {
            double gapOverneight = getGapOverneight(player);
            boolean standUpPossible = gapUnderneight + gapOverneight >= 1D;
            if (standUpPossible) {
                standUp(player, gapUnderneight);
            } else {
                toSlidingOrCrawling(player, gapUnderneight);
            }
        }
    }

    /**
     * 원본 L2186-L2212 `standupIfPossible(boolean tryLanding, boolean restoreFromFlying)` —
     * 비행 해제 포함 오버로드 정밀 이식:
     *   if (heightOffset >= 0) return;
     *   gap = getGapUnderneight();
     *   groundClose = gap < 1D;
     *   overGap = groundClose ? getGapOverneight() : -1D;
     *   standUpPossible = gap + overGap >= 1D;
     *
     *   if (tryLanding && groundClose && standUpPossible) {
     *       isFlying = false;
     *       sp.capabilities.isFlying = false;   // ★ focus_06 후속 (vanilla 비행 해제 sync)
     *       restoreFromFlying = true;
     *   }
     *   if (!restoreFromFlying) return;
     *
     *   if (!groundClose && !sneakPressed) resetHeightOffset();
     *   else if (standUpPossible && !(sneakPressed && grabPressed)) standUp(gap);
     *   else toSlidingOrCrawling(gap);
     *
     * B-N-standup-3 해소 (세션 136): gap/overGap 정밀 + sneak/grab 조건 분기 전수 복원.
     *
     * **포커스 #3 B-3 (세션 4) §18.1 해소**: `sp.capabilities.isFlying = false` (원본 L2199)
     *   매핑 — 1.21.1 `player.getAbilities().flying = false` (public 직접 할당) +
     *   `UpdatePlayerAbilitiesC2SPacket` 송신으로 server sync. flying field 직접 할당
     *   패턴은 MixinClientPlayerEntity L53 에서 이미 사용 중 (착지 후 flying 복원).
     */
    public void standupIfPossible(ClientPlayerEntity player, boolean tryLanding, boolean restoreFromFlying) {
        if (this.heightOffset >= 0) return;

        double gapUnderneight = getGapUnderneight(player);
        boolean groundClose = gapUnderneight < 1D;
        double gapOverneight = groundClose ? getGapOverneight(player) : -1D;
        boolean standUpPossible = gapUnderneight + gapOverneight >= 1D;

        if (tryLanding && groundClose && standUpPossible) {
            this.isFlying = false;
            // 포커스 #3 B-3 (세션 4): 원본 L2199 `sp.capabilities.isFlying = false` 매핑.
            //   1.21.1 PlayerAbilities.flying public field 직접 할당 + 서버 sync 패킷.
            player.getAbilities().flying = false;
            player.networkHandler.sendPacket(
                    new UpdatePlayerAbilitiesC2SPacket(player.getAbilities()));
            restoreFromFlying = true;
        }

        if (!restoreFromFlying) return;

        // 🔴 BUG-Fly2Slide (2026-05-04): `player.isSneaking()` → raw key state.
        //   원본 L2207-L2208 `sneakButton.Pressed` = raw sneak key. 우리 매핑이 잘못
        //   `player.isSneaking()` 사용 → `MixinClientPlayerEntity.sm_isSneaking_ClientPlayer`
        //   inject 가 비행 직후 `isSlow=false` 등으로 false 반환 → `(sneak&&grab)=false` →
        //   standUp 분기 잘못 매치 → heightOffset reset → 슬라이딩 진입 안 됨.
        //   메모리 feedback_movementInput_vs_isSneaking.md 패턴 적용.
        boolean sneakPressed = net.minecraft.client.MinecraftClient.getInstance()
                .options.sneakKey.isPressed();
        boolean grabPressed  = SmartMovingKeys.grab.isPressed();

        if (!groundClose && !sneakPressed) {
            // 🔴 fix #88 (2026-05-12): 헤드점프 종료 직후 블록 모서리 시나리오 잠김 BUG.
            //   사용자 보고 — 헤드점프 착지 시 블록 모서리 걸치며 → forward → 아래 ground 착지 →
            //     모델 ground 아래 잠김.
            //   진단 dump (research_headjump_landing_edge.md):
            //     모서리 시점 박스 발 = 모서리 top y, 박스 X-Z 영역은 모서리 *외부* → getGapUnderneight
            //     결과 = 1.0 (= 박스 영역 안 가장 깊은 ground 까지 거리). groundClose=false → standUp
            //     분기 미매치 → resetHO 분기 매치.
            //   resetHO 는 heightOffset=0 reset 만. POSE/dim/entity.y 미보정. POSE=SLIDING 잔존 →
            //     mixin offset 활성 → 박스 발 = entity.y+1 (= 모서리 ground) 정상. 그러나 *모델
            //     origin = entity.y* (= ground -1m) → 모델 1m 잠김.
            //   비행 종료 + 공중 + sneak release 시도 동일 분기 매치하지만 그때는 POSE=STANDING
            //     이라 mixin offset 차단 → 박스/모델 정렬 → 잠김 X.
            //   해결: standUp 분기와 동일 후처리 (POSE=STANDING 강제 + dim 갱신 + entity.y +1m
            //     push). 가드 = justEndedHeadJump + heightOffset==-1F + 모든 SM phase 종료 →
            //     헤드점프 종료 직후 1 tick 안 + 모서리 시나리오 만 매치.
            boolean _wasSmallBox = (this.heightOffset == -1F);
            resetHeightOffset();
            if (_wasSmallBox && this.justEndedHeadJump
                    && !isHeadJumping && !isSliding && !isCrawling && !isCrawlClimbing
                    && !isSwimming_sm && !isDiving && !isFlying && !isLevitating) {
                player.setPose(net.minecraft.entity.EntityPose.STANDING);
                player.calculateDimensions();
                player.setPosition(player.getX(), player.getY() + 1.0, player.getZ());
                player.lastRenderY += 1.0;
                player.prevY += 1.0;
            }
        } else if (standUpPossible && !(sneakPressed && grabPressed)) {
            standUp(player, gapUnderneight);
            // 🔴 fix #58 (2026-05-10, dump 분석 결과 — root cause 확정):
            //   진입 직전 sm_updatePose 결과 POSE=SLIDING (= isHeadJumping=true 시점). 5-AND
            //   식 직후 isHeadJumping=false 가 됐는데 POSE 는 *다음 vanilla updatePose 호출까지*
            //   잔존. mixin sm_getBaseDimensions L106 가드 (`pose==SLIDING` 보강) → dim
            //   (0.6, 0.8, 0.62). mixin offset 가드 (fix #56) `POSE.SLIDING` 매치 → 활성.
            //   bb.minY = entity.y + 1m. standUp push +1m 후 entity.y = ground 인데 박스 발 =
            //   ground + 1m → 박스 1칸 공중 부양 (사용자 시각 "1칸 박힘").
            //   dump 검증: [POST-STANDUP] entityY=-60, bbMinY=-59, pose=SLIDING, isHJ=false.
            //   해결: standUp 호출 후 SM state 모두 false 시 POSE=STANDING 강제 + dim 갱신 →
            //   mixin offset 차단 (height 1.8 ≥ 1) → 박스 발 = entity.y = ground 정상.
            if (!isHeadJumping && !isSliding && !isCrawling && !isCrawlClimbing
                    && !isSwimming_sm && !isDiving && !isFlying && !isLevitating) {
                player.setPose(net.minecraft.entity.EntityPose.STANDING);
                player.calculateDimensions();
            }
        } else {
            // 🔴 비행 박스 → small 박스 전환 entity.y +1 보정 (사용자 보고 fix —
            //   비행 → 1칸 공간 → 비행 풀림 → 엎드리기 안 됨, 블록 아래 파고들기):
            //
            //   비행 박스 (mixin offset 박스 +1 적용) = (entity.y+1, entity.y+1.8). entity.y =
            //   Y_floor - 1 (= 디딤발 minY 위치). 1칸 공간 fit.
            //   toSlidingOrCrawling 후 isCrawling=true → smSmall 분기 → dim eyeHeight=0.62 →
            //   mixin offset 가드 미통과 → 박스 = (entity.y, entity.y+0.8) = (Y_floor-1, Y_floor-0.2).
            //   박스 -1m 떨어짐 + 디딤발 안 박힘 → vanilla push out → 사용자 보고 "1칸 공간 밖으로
            //   나와짐, 블록 아래 파고들기".
            //
            //   해결: ICC EXIT toCrawling 매핑과 동일 = setPos(y+1) + lastRenderY/prevY 동기화 +
            //   Camera baseline 보정. 박스 위치 = (entity.y_new, entity.y_new+0.8) =
            //   (Y_floor, Y_floor+0.8) = 비행 박스 위치 (Y 차원) 와 동등 → 1칸 공간 안 유지.
            // 🔴 비행/Levitate/HeadJumping 종료 → toSlidingOrCrawling 매핑 (사용자 보고 fix —
            //   "비행 → 1칸 공간 → 비행 풀림 → 즉시 엎드리기"):
            //
            //   비행 박스 (mixin offset 박스 +1 적용) = (entity.y+1, entity.y+1.8). entity.y =
            //   Y_floor - 1 (= 디딤발 minY 위치). 1칸 공간 fit.
            //   toSlidingOrCrawling 후 isCrawling=true → smSmall 분기 → dim eyeHeight=0.62 →
            //   mixin offset 가드 미통과 → 박스 = (entity.y, entity.y+0.8) = (Y_floor-1, Y_floor-0.2).
            //   박스 -1m 떨어짐 + 디딤발 안 박힘 → vanilla push out → 사용자 보고 "1칸 공간 밖으로
            //   나와짐, 블록 아래 파고들기".
            //
            //   해결 1: ICC EXIT toCrawling 매핑과 동일 = setPos(y+1) + lastRenderY/prevY 동기화 +
            //   Camera baseline 보정. 박스 = (entity.y_new, entity.y_new+0.8) = (Y_floor, Y_floor+0.8)
            //   = 비행 박스 위치 (Y) 동등 → 1칸 공간 안 유지.
            //
            //   해결 2 (same-tick isCrawling 재계산 BUG): 원본 1.7.10 = isCrawling 재계산 (L2441) 이
            //   standupIfPossible (L2543) 이전 → toCrawling() 의 isCrawling=true 가 다음 tick 까지
            //   유지 → 다음 tick mustCrawl 재계산 (1칸 공간 박스 기반) 시 자연 유지.
            //   1.21.1 매핑 순서 차이: isCrawling 재계산이 standupIfPossible 후 → 같은 tick
            //   mustCrawl=false (이미 위에서 결정) → isCrawling 즉시 false 복원 BUG.
            //   해결: mustCrawl=true 강제 set → 같은 tick L1683 isCrawling 재계산 시
            //   `canCrawl && (false || true)` = true 유지. 다음 tick 부터는 박스 위치 검사로 자연 유지.
            boolean wasSmallBox = (this.heightOffset == -1F);
            toSlidingOrCrawling(player, gapUnderneight);
            // 🔴 사용자 보고 fix (2026-05-04 — "비행 중 1칸 공간 + shift+grab 동시 → 땅속"):
            //   toSlidingOrCrawling 가 grab pressed 시 isSliding=true 설정 (= isCrawling=false).
            //   기존 가드 `wasSmallBox && isCrawling` → false → setPos(y+1) 미적용 →
            //   entity.y 비행 박스 위치 (Y_floor-1) + small box 0.8 적용 → 박스 = (Y_floor-1, Y_floor-0.2)
            //   = 디딤발 안 박힘. fix: isSliding 케이스 도 가드 통과.
            // 🔴 fix #22 (2026-05-08, 사용자 보고 "착지해서 슬라이딩 진입 시 살짝 끊김"):
            //   비행 종료 시 entity.y = Y_floor - 1 (vanilla 1칸 공간 push down) 라 setPos(y+1)
            //   가 ground 정상 보정. 헤드점프 종료 시 entity.y = ground (vanilla move 박스 발
            //   ground 도달 시 갱신) 라 setPos(y+1) → ground+1m 공중 → 1 tick 떨어짐 시각 끊김.
            //   해결: setPos(y+1) 가드를 `wasFlying || wasLevitating` 만 (= justEndedHeadJump 제거).
            //   justEndedHeadJump 시는 별도 분기 — dim 갱신 + vy/fallDistance reset 만.
            //   박스 ground 안 박힘 시 메서드 끝의 안전망이 push up (이미 매핑됨).
            // 🔴 fix #68 (2026-05-10, dump 분석 — 사용자 보고 "비행 + grab+sneak 슬라이딩 시 헤드점프 잠깐"):
            //   기존 가드: wasSmallBox && (isCrawling || isSliding) && (wasFly || wasLev).
            //   BUG: isSliding 진입 시 dim eyeHeight=1.62 (fix #59) → mixin offset 활성 →
            //     박스 = (entity.y+1, entity.y+1.8) 이미 정상 (1칸 공간 fit). push +1m 무용 + 천장
            //     위로 박스 부유 → gravity 매 tick fall 누적 → AUTO-SLIDETOHJ 매치 → isHJ=true 1 tick
            //     (= 사용자 *헤드점프 잠깐* 보고).
            //   해결: isSliding 분기 제거. isCrawling 만 push (= dim eye=0.62 → mixin offset 차단 →
            //     박스 발 = entity.y = ground - 1m 박힘 → push 필수).
            if (wasSmallBox && this.isCrawling
                    && (wasFlying || wasLevitating)) {
                player.calculateDimensions();
                player.setPosition(player.getX(), player.getY() + 1.0, player.getZ());
                player.lastRenderY += 1.0;
                player.prevY += 1.0;
                net.minecraft.client.render.Camera cam =
                        net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera();
                if (cam != null) {
                    float eye = player.getStandingEyeHeight();
                    ((choco.ratel.smartmoving.mixin.client.MixinCamera) (Object) cam).sm_setCameraY(eye);
                    ((choco.ratel.smartmoving.mixin.client.MixinCamera) (Object) cam).sm_setLastCameraY(eye);
                }
                this.mustCrawl = true;
            }
            // 🔴 fix #25 복원 (fix #26 revert 후): push up + Camera baseline 동기화.
            //   사용자 보고 잔존 덜컹은 vanilla 매핑 한계 (= baseTick prevY 갱신 race) — 별도
            //   사이클로 deferred. 회귀 (= 박스 박힘) 우선 fix.
            if (this.justEndedHeadJump && wasSmallBox && (this.isCrawling || this.isSliding)) {
                player.calculateDimensions();
                net.minecraft.util.math.Box bbAfter = player.getBoundingBox();
                // 🔴 fix #30 (2026-05-08, 사용자 보고 "슬라이딩→헤드점프→슬라이딩 뚝 끊김"):
                //   fix #28 의 hardcoded +1.0 가정 = 헤드점프 진행 중 dim eye=1.62 (mixin 매치) →
                //   entity.y = ground_top - 1. 그러나 슬라이딩→헤드점프 시나리오는 fix #29 의
                //   calculateDimensions 부작용으로 dim eye=0.62 잔존 (= mixin 미매치) → 헤드점프
                //   진행 중 박스 발 = entity.y → 종료 시 entity.y = ground_top.
                //   fix #28 의 hardcoded +1.0 강제 시 박스 1m 공중 부양 → 다음 tick vanilla move
                //   가 떨어뜨림 → frame lerp -0.6m 시점 점프 → 사용자 인지 "뚝 끊김".
                //   해결: 박스 박힘 (bb.minY < solidUnder) 시만 push. yMax=bb.minY+1.5 (= clamp
                //   회피, feedback_solid_under_ymax_clamp 패턴). push 양은 hardcoded +1.0 유지
                //   (= 박힘 시 mixin 매치 박스 발 ground_top 보장으로 정확).
                //   박힘 X 시: push 안 함, cameraY/vy/fallDistance 만 강제.
                double solidUnder = getMaxPlayerSolidBetween(player, bbAfter.minY - 1.5, bbAfter.minY + 1.5, 0);
                // 🔴 fix #65 (2026-05-10, 사용자 보고 "헤드점프 → 1칸 공간 진입 시 위로 순간 이동"):
                //   dump 분석 결과: mixin offset 활성 (= height<1 + (eye>1 || POSE==SLIDING)) 시
                //   박스 발 = entity.y+1m = ground (정상). 그러나 yMax=bbMin+1.5 가 1칸 공간 천장
                //   블록 top 매치 → solidUnder=bbMin+1.5 (clamp) → stuck=true 잘못 → push +1m.
                //   해결: mixin offset 활성 시 박스 정상 → push 가드 (MixinEntity 의 활성 식 1:1).
                net.minecraft.entity.EntityDimensions _dim25 = player.getDimensions(player.getPose());
                boolean isMixinOffsetActive = _dim25.height() < 1.0F
                        && (_dim25.eyeHeight() > 1.0F
                            || player.getPose() == net.minecraft.entity.EntityPose.SLIDING);
                boolean stuck = !isMixinOffsetActive && bbAfter.minY < solidUnder - 1.0E-5;
                if (stuck) {
                    double pushY = 1.0;
                    player.setPosition(player.getX(), player.getY() + pushY, player.getZ());
                    player.lastRenderY += pushY;
                    player.prevY += pushY;
                }
                // cameraY 강제는 stuck 무관 — 진입 시점 시점 안정 위해 항상 강제.
                net.minecraft.client.render.Camera cam =
                        net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera();
                if (cam != null) {
                    float eye = player.getStandingEyeHeight();
                    ((choco.ratel.smartmoving.mixin.client.MixinCamera) (Object) cam).sm_setCameraY(eye);
                    ((choco.ratel.smartmoving.mixin.client.MixinCamera) (Object) cam).sm_setLastCameraY(eye);
                }
                net.minecraft.util.math.Vec3d _v = player.getVelocity();
                player.setVelocity(_v.x, 0.0, _v.z);
                player.fallDistance = 0F;
            }
        }

        // 🔴 비행 종료 dimensions 복원 (사용자 요청, 단계 3+):
        //   원본 setHeightOffset / resetHeightOffset 가 boundingBox/height 직접 조작 →
        //   1.21.1 우리 매핑은 calculateBoundingBox modify + sm_getBaseDimensions inject 으로
        //   대체. standUp 의 player.move() 가 entity.y 보정 + 박스 평행이동까지 수행하지만
        //   dimensions (height 0.8 → 1.8) 복원은 별도 calculateDimensions 호출 필요.
        //   호출 시점: 본 메서드 끝 — 가드 (heightOffset>=0) 미통과해 정상 처리된 경우만.
        //   호출 시점 abilities.flying=false → MixinEntity.sm_offsetBoundingBoxForFlying 가드
        //   미통과 → 박스 modify 미적용 → 박스 = (entity.y, entity.y+1.8) 정상 STANDING.
        player.calculateDimensions();

        // 🔴 가라앉음 안전망 (사용자 요청 — "위로 콜리전을 늘려서 원복" 보장):
        //   standUp 의 setPosition 보정 후에도 박스 발이 솔리드 안 박힐 가능성 (motion 처리
        //   차이로 미세 epsilon 박힘 등) 대비. 박스 발 아래 1m 범위 솔리드 max 검사 → 박스
        //   발보다 위 솔리드 발견 시 entity.y push up.
        //
        // 🔴 fix #57 (2026-05-10, 사용자 보고 "fix #56 후 정상 헤드점프 + 여우무빙 헤드점프 착지
        //   시 땅에 1칸 아래 박힘"):
        //   원인: 헤드점프 진행 중 mixin offset 활성 (= POSE.SLIDING 매치) → 박스 발 = entity.y+1m.
        //   vanilla collision 결과 entity.y = ground_top - 1m. 종료 시 isHeadJumping=false →
        //   POSE=STANDING → mixin offset 차단 → 박스 발 = entity.y = ground - 1m → 박힘.
        //   (가드 #2 = justEndedHeadJump && wasSmallBox && (isCrawling||isSliding) 가 정상 헤드
        //   점프 시 isCrawling=false+isSliding=false 라 매치 X → L3891 안전망만 발동.)
        //
        //   기존 yMax = bb.minY + 0.5 = (ground-1) + 0.5 = ground - 0.5. ground_top (=ground) >
        //   yMax → getMaxPlayerSolidBetween 의 yMax clamp (Math.min(result, yMax)) 발동 →
        //   solidUnder ≈ bb.minY → bb.minY < solidUnder-1e-5 매치 X → push 안 함 → 박힘 잔존.
        //   fix #30 (L3851) 와 동일 패턴 — yMax 를 +1.5 로 늘려 clamp 회피.
        //
        //   추가: setPosition 후 lastRenderY/prevY 동기화 (= feedback_pushup_lastRenderY_prevY_sync).
        Box bbAfter = player.getBoundingBox();
        double solidUnder = getMaxPlayerSolidBetween(player, bbAfter.minY - 1.0, bbAfter.minY + 1.5, 0);
        // 🔴 fix #65 (2026-05-10): fix #25 와 동일 가드. mixin offset 활성 시 안전망 push X.
        //   dump 분석: 1칸 공간 진입 시 mixin offset 활성 (= dimEye=1.62) → 박스 발 = ground 정상.
        //   yMax=bbMin+1.5 가 천장 블록 매치 → solidUnder=bbMin+1.5 (clamp) → 잘못 push +1.5m.
        //   해결: mixin offset 활성 시 push skip. fix #57 진짜 시나리오 (= POSE=STANDING + 박스
        //   발 ground-1) 는 dimEye=1.62 (= STANDING dim) 라 mixin 가드 매치 → height 1.8 ≥ 1
        //   라 mixin offset 차단. 그러나 dim eye 자체는 1.62. 즉 *pose=STANDING + dimH>=1.0F*
        //   가 진짜 mixin offset 차단 시나리오. dimEye 단독 검사 X — height 검사 추가.
        boolean isMixinOffsetActive_safety = player.getDimensions(player.getPose()).height() < 1.0F
                && (player.getDimensions(player.getPose()).eyeHeight() > 1.0F
                    || player.getPose() == net.minecraft.entity.EntityPose.SLIDING);
        // 🔴 fix #71 (2026-05-10, dump 분석 — 사용자 보고 "비행 → 1칸 공간 → 엎드리기 안됨"):
        //   기존 가드: bbMin < solidUnder → push (= 박힘 차단).
        //   BUG: 비행 → 1칸 공간 진입 시 fix #68 push +1m 후 entity.y=ground+1m (= 정상).
        //     안전망 yMax=bbMin+1.5 가 천장 위 블록 top 검사 매치 (clamp) → solidUnder=bbMin+1.5 →
        //     stuck=true → pushY=1.5m → standing 공간으로 이동 → 엎드리기 안됨.
        //   해결: pushY > 1.0m 시 차단 (= clamp 매치 무관 push 발동). 진짜 박힘 시나리오 push 양 ≤ 1m.
        double _potentialPushY71 = solidUnder - bbAfter.minY;
        boolean stuckSafetyNet = !isMixinOffsetActive_safety
                && bbAfter.minY < solidUnder - 1.0E-5
                && _potentialPushY71 <= 1.0;
        if (stuckSafetyNet) {
            double pushY = solidUnder - bbAfter.minY;
            player.setPosition(player.getX(), player.getY() + pushY, player.getZ());
            player.lastRenderY += pushY;
            player.prevY += pushY;
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
        // Phase E BUG-2 1:1 정정: 원본 L2234 `>= 2.0F` Stats 갱신 (정수 cm 거리)
        if (player.fallDistance >= 2.0F) {
            player.increaseStat(Stats.FALL_ONE_CM, (int) Math.round(player.fallDistance * 100D));
        }
        // Phase E BUG-2 1:1 정정: 원본 L2237 `>= startDistance` (>= 가 아닌 > 였음 — 경계값 1 tick 차이 fix)
        if (player.fallDistance >= startDistance) {
            // Phase E 1:1 정정: 원본 L2239 `(int)Math.ceil((fallDistance-startDistance)*factor)` 정수 데미지
            float damage = (float) Math.ceil((player.fallDistance - startDistance) * factor);
            player.damage(player.getDamageSources().fall(), damage);
            // 원본 L2240 `distanceClimbedModified = nextClimbDistance` (step sound 강제) — 별도 시스템 deferred
        }
        // Phase E BUG-1 1:1 정정: 원본 L2242 `sp.fallDistance = 0F` 함수 끝 무조건 reset 누락 fix
        player.fallDistance = 0F;
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
        // 🔴 (세션 55): 이전 잘못 매핑 `isJumping = !onGround && !climb && !swim && !dive && !dipping`
        //   완전 제거. 원본 SmartMovingSelf.isJumping 은 tryJump 시 true (L2132), tickEssential
        //   시작 시 false reset (L1744). 매 틱 흐름:
        //     1) tickEssential HEAD: isJumping = false reset.
        //     2) handleJumping → tryJump (조건 충족 시): isJumping = true (한 틱 동안 유지).
        //     3) sendStatePacket TAIL: 현재 isJumping (true 또는 false) 그대로 packet 전송.
        //   = 우리 1.21.1 매핑: tickEssential L835+ 에 `isJumping = false` 추가 (이미 적용),
        //     SmartMovingJumper.tryJump L316 `sm.isJumping = true` (이미 적용).
        //   sendStatePacket 에서는 매핑 안 함 — 현재 값 그대로 전송.
        // 🔴 (Phase 2 multi BUG-1) doFallingAnimation 정의 확장:
        //   기존: velocity.y < -0.1 만 검사 → SM 분기 진입 조건과 다름.
        //   변경: sm_animateFalling 진입 조건 (= isFallingForReset) 과 동일 식 포함.
        //         local 에서 매 tick 계산 + packet 으로 remote sync → remote 의 sm.doFallingAnimation 도
        //         정확한 진입 조건 표현 (vanilla fallDistance 가 remote 미동기여서 자체 검사 불가).
        doFallingAnimation = !player.isOnGround()
                              && player.fallDistance > SmartMovingConfig.Config.fallAnimationDistanceMinimum
                              && !isClimbing && !isCrawlClimbing && !isCeilingClimbing
                              && !isSwimming_sm && !isDiving
                              && !player.isTouchingWater();
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
        s.isClimbCrawling      = isClimbCrawling;
        s.wasSelfSlideFire     = wasSelfSlideFire;  // fix #96 (remote 측 시각 fix #81/#94/#95 동기화)

        long bits = SmartMovingState.encode(s);
        // 🔴 (Phase 1-B fix-1) dirty bit 검사 제거 — 매 tick 무조건 송신.
        //   기존: `if (bits != lastSentBits)` 만족 시만 송신. 안정 상태 (= 같은 SM state 유지)
        //         에서 송신 stop → late join player 가 latest state 영영 못 받음 (BUG-A/B).
        //   변경: 매 tick 송신 (12 byte/tick × player 수 ≈ 1KB/s 미만 — 부하 무시 가능).
        //         late join 자동 해결.
        ClientPlayNetworking.send(new SmartMovingNetwork.StatePayload(player.getId(), bits));
        lastSentBits = bits;
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
