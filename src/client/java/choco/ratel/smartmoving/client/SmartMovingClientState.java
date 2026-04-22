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
        isClimbing        = ((bits >> 14) & 1) != 0;
        isCrawlClimbing   = ((bits >> 12) & 1) != 0;
        isCeilingClimbing = ((bits >> 18) & 1) != 0;
        isWallJumping     = ((bits >> 31) & 1) != 0;
        isCrawling        = ((bits >> 13) & 1) != 0;
        isSmall           = ((bits >> 15) & 1) != 0;
        isSliding         = ((bits >> 21) & 1) != 0;
        isHeadJumping     = ((bits >> 20) & 1) != 0;
        isDipping         = ((bits >> 10) & 1) != 0;
        isSwimming_sm     = ((bits >> 11) & 1) != 0;
        isDiving          = ((bits >>  9) & 1) != 0;
        isSlow            = ((bits >> 29) & 1) != 0;
        isFast            = ((bits >> 30) & 1) != 0;
        isFlying          = ((bits >> 17) & 1) != 0;   // doFlyingAnimation bit
        isClimbJumping    = ((bits >> 27) & 1) != 0;
        angleJumpType     = (int) ((bits >> 22) & 0x7);
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

        // C-33: wasClimbing = 이전 틱의 isClimbing 값 저장
        wasClimbing = isClimbing;
        // 매 틱 exhaustion 감소 (클라이밍 중 증가량으로 상쇄됨)
        exhaustion = Math.max(0F, exhaustion - 1.0F);

        // IMPL-04: 설정 토글 키 처리 (원본: toggleButton.update() + StartPressed 분기)
        // 싱글: toggle() 직접 호출 + 채팅 피드백 / 멀티: 서버에 변경 요청 패킷 전송
        if (SmartMovingKeys.configToggle.wasPressed()) {
            if (SmartMovingConfig.Config == SmartMovingConfig.INSTANCE) {
                SmartMovingConfig.INSTANCE.toggle();
                String msgKey = SmartMovingConfig.INSTANCE.enabled
                    ? "smartmoving.message.config.client.enabled"
                    : "smartmoving.message.config.client.disabled";
                if (player != null) player.sendMessage(Text.translatable(msgKey));
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
            // C-15: isSlow / isFast / isFlying 매 틱 계산
            // isSlow 원본: wantSneak && !wantSprint && !isClimbing
            // wantSneak = sneakButton.Pressed → 키 입력 직접 (sm_isSneaking override 순환 방지)
            boolean wantSneak = net.minecraft.client.MinecraftClient.getInstance().options.sneakKey.isPressed();
            isSlow = wantSneak && !player.isSprinting() && !isClimbing;
            // isFast 원본: grabButton.Pressed && isSprinting()
            isFast = SmartMovingKeys.grab.isPressed() && player.isSprinting();
            // isFlying 원본: sp.capabilities.isFlying
            isFlying = player.getAbilities().flying;
            // wasCapabilitiesIsFlying: beforeOnLivingUpdate에서 저장 (vanilla tickMovement 실행 전)
            wasCapabilitiesIsFlying = isFlying;
            // wasCollidedHorizontally: 이전 틱 물리 결과 (HEAD에서 캡처 → 원본 beforeOnUpdate)
            wasCollidedHorizontally = player.horizontalCollision;

            // IMPL-01: 크롤링 진입/유지/해제
            // 원본 트리거: grabButton.StartPressed && (sneakToggled || sneakButton.Pressed) && onGround
            SmartMovingConfig cfg = SmartMovingConfig.Config;
            if (cfg.crawl) {
                boolean grabJustPressed = SmartMovingKeys.grab.wasPressed();
                if (!isCrawling) {
                    boolean wantCrawl = grabJustPressed
                            && player.isSneaking()
                            && player.isOnGround()
                            && !isFlying && !isSwimming_sm && !isDiving && !isDipping
                            && !isClimbing && !isCrawlClimbing && !isCeilingClimbing
                            && !isSliding && !isHeadJumping;
                    // 원본 canCrawl: !isSwimming && !isDiving && (!isDipping || dippingDepth < 0.65F)
                    // mustCrawl도 canCrawl 게이트 적용 — 수영 중 mustCrawl이 크롤링을 강제하지 않도록
                    boolean mustCrawl = !canStandUp(player)
                            && !isSwimming_sm && !isDiving
                            && (!isDipping || dippingDepth < 0.65F);
                    if (wantCrawl || mustCrawl) {
                        isCrawling = true;
                        crawlToggled = true;
                        ignoreNextStopSneakButtonPressed = true;
                    }
                } else {
                    // 원본 canCrawl 게이트: 수심이 0.65F 이상이면 mustCrawl 해제 → 수영 전환 허용
                    boolean mustCrawl = !canStandUp(player)
                            && (!isDipping || dippingDepth < 0.65F);
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

                // 원본: if(StartPressed) { count==0→3, else→-1 } else if(count>0) count--
                if (cfg.angleJumpSide) {
                    if (startLeft) {
                        if (leftJumpCount  == 0) leftJumpCount  = 3; else leftJumpCount  = -1;
                    } else if (leftJumpCount  > 0) leftJumpCount--;

                    if (startRight) {
                        if (rightJumpCount == 0) rightJumpCount = 3; else rightJumpCount = -1;
                    } else if (rightJumpCount > 0) rightJumpCount--;
                }
                if (cfg.angleJumpBack) {
                    if (startBack) {
                        if (backJumpCount  == 0) backJumpCount  = 3; else backJumpCount  = -1;
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

            // wouldIsSneaking 원본: wouldWantSneak && !wantSprint && !isClimbing → isSlow와 동일
            wouldIsSneaking = isSlow;

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
        ignoreNextStopSneakButtonPressed = false;
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

    // ── R-01: sendStatePacket() ───────────────────────────────────────

    /**
     * 현재 SmartMovingClientState를 34비트 long으로 인코딩해 서버로 전송한다.
     * 상태가 이전 틱과 달라진 경우에만 전송 (lastSentBits 비교).
     * 원본: SmartMovingPlayerBase.updateEntityActionState() 끝부분 writeEntityState().
     */
    public void sendStatePacket(ClientPlayerEntity player) {
        if (!ClientPlayNetworking.canSend(SmartMovingNetwork.StatePayload.ID)) return;

        SmartMovingState s = new SmartMovingState();
        s.actualFeetClimbType  = actualFeetClimbType;
        s.actualHandsClimbType = actualHandsClimbType;
        s.isJumping            = !player.isOnGround() && !isClimbing && !isSwimming_sm && !isDiving && !isDipping;
        s.isDiving             = isDiving;
        s.isDipping            = isDipping;
        s.isSwimming           = isSwimming_sm;
        s.isCrawlClimbing      = isCrawlClimbing;
        s.isCrawling           = isCrawling;
        s.isClimbing           = isClimbing;
        s.isSmall              = isSmall;
        s.doFallingAnimation   = !player.isOnGround() && player.getVelocity().y < -0.1D
                                  && !isClimbing && !isSwimming_sm && !isDiving;
        s.doFlyingAnimation    = isFlying;
        s.isCeilingClimbing    = isCeilingClimbing;
        s.isLevitating         = false; // 로프 미구현
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
