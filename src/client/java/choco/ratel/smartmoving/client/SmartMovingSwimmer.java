package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 클라이언트 측 수영/잠수 물리 로직.
 * 원본: SmartMovingSelf.handleSwimming(), SmartMovingBase.moveFlying() 이식.
 *
 * 포함 항목:
 *   8-1: updateSwimState() — isDipping / isSwimming_sm / isDiving 3분류
 *   8-2: handleSwimming() — 수중 이동 physics (damping, moveFlying, motionYDiff)
 *   8-5: isJumpingOutOfWater — 수평 충돌 + waterMovementTicks > 10 → motionY 상승
 *   8-6: 수영 소리 누적 → MixinEntityClient.sm_afterMove_client 에서 처리
 */
@Environment(EnvType.CLIENT)
public final class SmartMovingSwimmer {

    // ── 8-1: offset 임계값 ───────────────────────────────────────────────────
    // 원본: SmartMovingContext의 SwimCrawlWaterBorder 상수
    // offset = getFluidHeight(WATER) + 0.1625D
    private static final double OFFSET_SWIMMING = 1.4D;  // isDipping/isSwimming 경계
    private static final double OFFSET_DIVING   = 1.9D;  // isSwimming/isDiving 경계

    // SwimCrawlWater 경계 상수 (SmartMovingContext L41-L44, R-06)
    private static final float SWIM_CRAWL_MAX    = 1.0F;    // SwimCrawlWaterMaxBorder
    private static final float SWIM_CRAWL_TOP    = 0.65F;   // SwimCrawlWaterTopBorder
    private static final float SWIM_CRAWL_MEDIUM = 0.6F;    // SwimCrawlWaterMediumBorder (B-36 분기 b/c)
    private static final float SWIM_CRAWL_BOTTOM = 0.55F;   // SwimCrawlWaterBottomBorder (B-11 조건)

    // ── 8-2: 수중 이동 상수 ──────────────────────────────────────────────────
    // 원본: SmartMovingSelf.handleSwimming() 내 motionX/Y/Z 감쇠값
    private static final double DAMPING_DIPPING_XZ = 0.80D;
    private static final double DAMPING_DIPPING_Y  = 0.83D;
    private static final double DAMPING_SWIMMING    = 0.85D;
    private static final double DAMPING_DIVING      = 0.83D;

    // 원본: SM swim 속도 팩터 기본 이동 속도
    private static final float  BASE_SWIM_SPEED    = 0.02F;

    // 원본: isJumpingOutOfWater motionY 값 (SM 정확 상수 유지)
    private static final double JUMP_OUT_OF_WATER_VELOCITY = 0.30000001192092896D;

    private SmartMovingSwimmer() {}

    // ── [8-1] updateSwimState ────────────────────────────────────────────────

    /**
     * 매 travel() 틱마다 호출하여 수영 상태 3분류를 갱신한다.
     * 원본: SmartMoving.java의 isDipping/isSwimming/isDiving 갱신 로직.
     *
     * offset = getFluidHeight(WATER) + 0.1625D
     *   offset <  1.4 → isDipping   (수면 경계 미만, 발만 잠김)
     *   1.4 ≤ offset < 1.9 → isSwimming_sm (수면 수영)
     *   offset ≥  1.9 → isDiving    (완전 잠수)
     */
    /**
     * 원본 SmartMovingSelf L1488-L1498 `resetSwimming()` 완전 이식 (B-10-reset-post 세션 114).
     *
     * 원본 8 필드 리셋:
     *   dippingDepth = -1
     *   isDipping = false
     *   isSwimming = false
     *   isDiving = false
     *   isLevitating = false
     *   isShallowDiveOrSwim = false
     *   isFakeShallowWaterSneaking = false
     *   isJumpingOutOfWater = false
     *
     * 원본 호출 지점 6 개 중 updateSwimState 진입 `!isInWater` 분기 (원본 L242/L253) 대응.
     * handleSwimming 내부 호출 (L555/L585/L607/L645) 은 B-9 Phase 5 재작성 시 활용 예정.
     */
    private static void resetSwimming(SmartMovingClientState sm) {
        sm.dippingDepth                = -1F;
        sm.isDipping                   = false;
        sm.isSwimming_sm               = false;
        sm.isDiving                    = false;
        sm.isLevitating                = false;
        sm.isShallowDiveOrSwim         = false;
        sm.isFakeShallowWaterSneaking  = false;
        sm.isJumpingOutOfWater         = false;
    }

    public static void updateSwimState(ClientPlayerEntity player, SmartMovingClientState sm) {
        // 🔵 (2026-05-20, BUG 1 fix #103) calculateDimensions 호출 트리거.
        //   원본 1.7.10 `setHeightOffset(-1F)` 는 boundingBox.minY 직접 변경 → 즉시 박스 갱신.
        //   1.21.1 매핑은 dim provider 패턴 → `Entity.calculateDimensions()` 호출 시점에만
        //   캐시 갱신 + boundingBox recompute. updateSwimState 가 isSwimming_sm/isDiving 만 set
        //   하고 호출 누락 → POSE/dim 갱신 1+ tick lag → 사용자 보고 "물 진입 시 콜리전 즉시 X,
        //   jump 키 후 작아짐" (= 다음 tick vanilla updatePose 가 마침내 SWIMMING set).
        //   해결: 진입 시 prev 저장 → return 직전 변화 검사 → calculateDimensions 호출.
        //   메모리 [[feedback_processStatePacket_calculateDimensions]] 패턴 1:1 적용.
        boolean prevSwim103 = sm.isSwimming_sm;
        boolean prevDive103 = sm.isDiving;

        // B-10b-pre (세션 110): 원본 L105 `boolean wasJumpingOutOfWater = isJumpingOutOfWater`
        // 지역 snapshot. 1.21.1 은 updateSwimState + handleSwimming 분리 → 필드로 승격.
        // §7 B-10b-pre 근사. B-10b-post 공식 `isJumpingOutOfWater = ... || wasJumpingOutOfWater`
        // 이식 시 이 이전 틱 값 참조.
        sm.wasJumpingOutOfWater = sm.isJumpingOutOfWater;

        // **B-7c 해소 (세션 127)**: 원본 L232 진입 조건 정밀 복원.
        //   boolean handleSwimming = !isFlying && !isLiquidClimbing
        //                         && (isInWater() || (wasSwimming && isInLiquid())
        //                             || (lavaLikeWater && handleLavaMovement()));
        // - isFlying (SM 비행) / isLiquidClimbing (B-7a 세션 127) 게이트 추가
        // - wasSwimming 은 updateSwimState 진입 시점에 아직 갱신되지 않은 sm.isSwimming_sm
        //   (MixinLivingEntityClient L77 `boolean wasSwimming = sm.isSwimming_sm;` 스냅샷과 등가)
        // - isInLiquid (B-7d 세션 127) — AABB 액체 판정 (vanilla isTouchingWater 보다 정밀)
        // - player.isInLava() → 원본 handleLavaMovement() 대응 (vanilla 액체 교차 판정)
        SmartMovingConfig cfg7c = SmartMovingConfig.Config;
        boolean handleSwim = !sm.isFlying
                && !sm.isLiquidClimbing
                && (player.isTouchingWater()
                    || (sm.isSwimming_sm && SmartMovingClientState.isInLiquid(player))
                    || (cfg7c.isLavaLikeWaterEnabled() && player.isInLava()));
        if (!handleSwim) {
            // B-10-reset-post (세션 114): 원본 L1488-L1498 `resetSwimming()` 메서드 호출.
            // updateSwimState 진입 `!handleSwim` 분기 (원본 L242/L253) 대응 — 물 밖 전환 시
            // 8 수중 관련 필드 일괄 리셋 (이전 개별 할당에서 isLevitating / isFakeShallowWaterSneaking
            // / isJumpingOutOfWater 3 필드 누락 해소).
            resetSwimming(sm);
            // B-10c-post (세션 112): 원본 L550 `isStillSwimmingJump = false` — useStandard
            // 경로 별도 리셋. 원본 resetSwimming 자체에는 없으나 1.21.1 `!handleSwim`
            // 경로가 useStandard 대응이라 여기서 함께 리셋.
            sm.isStillSwimmingJump = false;
            // 1.21.1 추가 — waterMovementTicks 리셋 (원본 resetSwimming 에 없음. B-12 정정
            // 경로와 일관성 유지를 위해 물 밖에서 0).
            sm.waterMovementTicks  = 0;
            // 🔵 (BUG 1 fix #103) dim 변화 시 calculateDimensions — 물 나감 시 swim/dive→false.
            if (sm.isSwimming_sm != prevSwim103 || sm.isDiving != prevDive103) {
                player.calculateDimensions();
            }
            return;
        }

        // **B-9a 해소 (세션 128)**: `dippingDepth` 시멘틱을 원본 `playerSwimWaterBorder`
        //   (AABB 정밀, 0~∞ 범위) 로 교체. 기존 `fluidHeight` 근사는 블록 내 액체 높이
        //   (0~1 범위) — 원본과 시멘틱 다름.
        //   원본 L270: `playerSwimWaterBorder = totalSwimWaterBorder - j - j_offset`
        //   원본 L416: `dippingDepth = (float)playerSwimWaterBorder`
        //   B-42d `SwimBorderValues` 소비. 아래 분기 공식 + dippingDepth 필드 공유.
        SmartMovingClientState.SwimBorderValues sbv9a =
                SmartMovingClientState.computeSwimBorderValues(player);
        sm.dippingDepth = (float) sbv9a.playerSwimWaterBorder;

        // 원본 L301 3-OR: isCrawling || isClimbCrawling || isCrawlClimbing → isDipping 강제
        // (handleSwimming L308-L309). B-6 (세션 60): 누락된 `sm.isClimbCrawling` 추가.
        // isClimbCrawling 공식 이식은 B-18 범위 — 현재 항상 false 유지이나 조건은 1:1 복원.
        if (sm.isCrawling || sm.isClimbCrawling || sm.isCrawlClimbing) {
            sm.isDipping     = true;
            sm.isSwimming_sm = false;
            sm.isDiving      = false;
            // B-10a-post (세션 109): 원본 L507 공식 `couldStandUp && (isDiving || isSwimming)`.
            // 강제 isDipping 경로는 swim/dive 모두 false → 자동 false.
            sm.isShallowDiveOrSwim = false;
            // B-12 (세션 64): 원본 L481-L484 `if(swimming||diving) ticks++; else ticks=0;`.
            //   dipping 강제 경로는 swimming/diving 아님 → ticks=0 리셋.
            sm.waterMovementTicks = 0;
            // 🔵 (BUG 1 fix #103) dim 변화 시 calculateDimensions — 강제 dipping (crawl 등) 진입.
            if (sm.isSwimming_sm != prevSwim103 || sm.isDiving != prevDive103) {
                player.calculateDimensions();
            }
            return;
        }

        // **B-9a 해소 (세션 128)**: 원본 L305 `offset = playerSwimWaterBorder + 0.1625D`
        //   공식 복원. fluidHeight 근사 → AABB 정밀 playerSwimWaterBorder.
        //   A/B 서브 분기 + (2, ∞) 구간 + (<0) 분기는 B-9b/c/d/e/f 후속 원자에서 재구성.
        //   현재는 A 경로 threshold (1.4 / 1.9) 만 유지.
        double offset = sbv9a.playerSwimWaterBorder + 0.1625D;
        sm.isDipping     = offset < OFFSET_SWIMMING;
        sm.isSwimming_sm = offset >= OFFSET_SWIMMING && offset < OFFSET_DIVING;
        sm.isDiving      = offset >= OFFSET_DIVING;

        // B-8 (세션 67): 원본 L436-L441 Config 게이트 이식.
        //   swimming = !useStandard && swimming && Config.isSwimmingEnabled();
        //   diving   = !useStandard && diving   && Config.isDivingEnabled();
        //   dipping  = !useStandard && dipping  && Config.isSwimmingEnabled();
        // Config 비활성화 시 상태 플래그를 false 로 정화 → 소비처 (#1/#3/#4) 가 잘못된
        // true 를 참조하지 않도록. 기존은 handleSwimming 에서 return false 로 근사만 —
        // 상태 플래그 자체는 정화 안 됨 (오역).
        // useStandard 는 B-9 (메인 분류 재작성) 범위 — 여기선 플래그 정화만.
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!cfg.isSwimmingEnabled()) {
            sm.isSwimming_sm = false;
            sm.isDipping     = false;
        }
        if (!cfg.isDivingEnabled()) {
            sm.isDiving = false;
        }

        // B-10d (세션 71): 원본 L474 + L505 `isLevitating = levitating` 이식.
        //   levitating = diving && !diveUp && !diveDown && moveStrafing==0 && moveForward==0
        // 원본 L468-L469:
        //   diveUp   = isp.getIsJumpingField()       → player.input.jumping
        //   diveDown = sneak && Config._diveDownOnSneak.value
        // 완전 정적 잠수 상태 (입력 전혀 없음) 판정. 애니메이션(isDive Quarter-Sixteenth 수직
        // 각도 적용 조건) 등에 사용. R-11 A-2 불일치 #11 해소. 필드는 L179 기존 이식됨.
        boolean diveUp16   = player.input.jumping;
        boolean diveDown16 = player.isSneaking() && cfg.diveDownOnSneak;
        sm.isLevitating = sm.isDiving
                && !diveUp16
                && !diveDown16
                && player.input.movementSideways == 0F
                && player.input.movementForward == 0F;

        // B-12 (세션 64): 원본 L481-L484 정정 — swimming/diving 만 증분, dipping 포함
        //   else 는 ticks=0 리셋. 기존 무조건 증분 (dipping 포함) 은 오역.
        //   isJumpingOutOfWater 공식 (원본 L486-L487) 이식 시 이 ticks 값이 정확해야 함.
        if (sm.isSwimming_sm || sm.isDiving) {
            sm.waterMovementTicks++;

            // B-10b-post (세션 111): 원본 L486-L487 공식 이식:
            //   wantJumpOutOfWater = (moveForward != 0 || moveStrafing != 0)
            //                     && sp.isCollidedHorizontally && diveUp && !isSlow
            //   isJumpingOutOfWater = wantJumpOutOfWater
            //                     && (waterMovementTicks > 10 || sp.onGround || wasJumpingOutOfWater)
            // 수면 탈출 점프 진행 판정. handleSwimming L500 `motionY = 0.30000001192092896D`
            // 설정의 게이트. 의존 전수 충족 (B-10d diveUp16 + B-12 ticks + B-10b-pre
            // wasJumpingOutOfWater + vanilla horizontalCollision/isOnGround).
            boolean wantJumpOutOfWater = (player.input.movementForward != 0F
                                       || player.input.movementSideways != 0F)
                    && player.horizontalCollision
                    && diveUp16
                    && !sm.isSlow;
            sm.isJumpingOutOfWater = wantJumpOutOfWater
                    && (sm.waterMovementTicks > 10
                        || player.isOnGround()
                        || sm.wasJumpingOutOfWater);
        } else {
            sm.waterMovementTicks = 0;
        }

        // B-10a-post (세션 109): 원본 L507 `isShallowDiveOrSwim = couldStandUp && (isDiving ||
        // isSwimming);` 이식. `couldStandUp` = 원본 L276 공식.
        // B-42-B5 해소 (세션 121): 기존 `dippingDepth >= 0F && dippingDepth <= 1.5F` 근사를
        // `SwimBorderValues` 로 원본 공식 복원 — `playerSwimWaterBorder >= 0 &&
        // minPlayerSwimWaterDepth <= 1.5` (AABB 정밀 파생값).
        // 소비처: B-36 분기 (a) 얕은 물 swim/dive → walking 전환 활성화. B-11 Phase 5
        // (얕은 물 특수 분기) 에서도 진입 게이트로 소비.
        SmartMovingClientState.SwimBorderValues swimVals =
                SmartMovingClientState.computeSwimBorderValues(player);
        boolean couldStandUp = swimVals.playerSwimWaterBorder >= 0
                            && swimVals.minPlayerSwimWaterDepth <= 1.5;
        sm.isShallowDiveOrSwim = couldStandUp && (sm.isDiving || sm.isSwimming_sm);

        // 🔵 (BUG 1 fix #103) 메인 분기 끝 — dim 변화 시 calculateDimensions.
        //   첫 swim/dive 진입 frame 에서 즉시 박스 갱신 → 원본 setHeightOffset(-1F) 동일 timing.
        if (sm.isSwimming_sm != prevSwim103 || sm.isDiving != prevDive103) {
            player.calculateDimensions();
        }
    }

    // ── [8-2] handleSwimming ─────────────────────────────────────────────────

    /**
     * 수중 이동 물리를 직접 처리한다. vanilla travel() 물속 분기를 완전 대체.
     * 원본: SmartMovingSelf.handleSwimming(moveForward, moveStrafing, speedFactor,
     *                                      wasSwimming, wasDiving, isLiquidClimbing, wasJumpingOutOfWater)
     *
     * @param jumping jump 키가 현재 눌려있는 상태 (LivingEntity.jumping)
     * @param wasSwimming superMoveEntityWithHeading 진입 시 isSwimming 스냅샷(원본 L97).
     * @param wasDiving   동일 시점 isDiving 스냅샷(원본 L99).
     * @return SM이 처리했으면 true (호출자가 travel() cancel)
     */
    public static boolean handleSwimming(
            ClientPlayerEntity player,
            SmartMovingClientState sm,
            Vec3d movementInput,
            boolean jumping,
            boolean wasSwimming,
            boolean wasDiving) {

        // ── SwimCrawlWater 전환 (원본: handleSwimming L274-279, L416-432, R-06) ──────
        // wasHeightOffset = heightOffset (크롤링 hitbox 오프셋 캡처).
        // 1.21.1: bounding box minY 이동 없음 → wasHeightOffset=0, playerCrawlWaterBorder=dippingDepth
        boolean wasCrawling = sm.isCrawling;

        // ① standupIfPossible 상당: 수심 > TopBorder → 크롤링 해제 시도
        if (sm.isCrawling && sm.dippingDepth > SWIM_CRAWL_TOP) {
            sm.isCrawling = false;
        }

        // ② playerCrawlWaterBorder 판정 (이전 틱에서 크롤링 중이었던 경우)
        // B-13 (세션 61): 원본 L2434 `(isCrawling || isSliding) && playerCrawlWaterBorder <
        // SwimCrawlWaterMaxBorder` 에 맞춰 `|| sm.isSliding` 추가. 원본 isCrawling 은
        // L2415 분기에서 해제 후 시점이라 1.21.1 의 진입 시 스냅샷 wasCrawling 과 등가.
        // isSliding 은 L121 에서 수정 안 되므로 원본과 동일 시점.
        if ((wasCrawling || sm.isSliding) && sm.dippingDepth >= 0F) {
            float playerCrawlWaterBorder = sm.dippingDepth;
            if (playerCrawlWaterBorder < SWIM_CRAWL_MAX) {
                if (playerCrawlWaterBorder < SWIM_CRAWL_TOP) {
                    // 얕은 물 — 계속 크롤링 (handleSwimmingRejected = true)
                    sm.isCrawling = true;
                    return false;
                } else {
                    // 크롤링 → 수영 전환 (원본: isCrawling=false; isSwimming=true; isDipping=false)
                    sm.isCrawling    = false;
                    sm.isDiving      = false;
                    sm.isSwimming_sm = true;
                    sm.isDipping     = false;
                }
            }
        }
        // ────────────────────────────────────────────────────────────────────────────

        if (!sm.isDipping && !sm.isSwimming_sm && !sm.isDiving) return false;

        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (sm.isDiving && !cfg.dive) return false;
        if ((sm.isSwimming_sm || sm.isDipping) && !cfg.swim) return false;

        // ── 원본 SmartMovingSelf L226-L246 (isFakeShallowWaterSneaking = true 설정 경로) ──
        // 원본:
        //   couldStandUp = playerSwimWaterBorder >= 0 && minPlayerSwimWaterDepth <= 1.5
        //   swimDown = sneak && _swimDownOnSneak
        //   wantShallowSwim = couldStandUp && (wasSwimming || wasDiving)
        //   if (wantShallowSwim) {
        //       for (Orientation o : getClimbingOrientations(sp, true, true))
        //           if (o.isTunnelAhead(world, i, j, k)) wantShallowSwim = false;   // 터널 앞에선 차단
        //   }
        //   if (wasSwimming && wantShallowSwim && swimDown) { swimDown=false; isFakeShallowWaterSneaking=true; }
        //
        // B-5 (세션 63) → **B-42-B5 해소 (세션 121)**: 근사 (1) couldStandUp 수심 측정
        //   원본 L276 `playerSwimWaterBorder >= 0 && minPlayerSwimWaterDepth <= 1.5` 복원.
        //   `SwimBorderValues` (B-42d) 를 사용한 AABB 정밀 파생값.
        // 잔존 근사 2건 (§7 B-5 갱신 — 이들은 B-42 범위 외):
        //   2) getClimbingOrientations 방향 집합 — 원본 8방향, 1.21.1 4방향 (별도 원자)
        //   3) swimDown=false (원본 L244) 미이식 — 동작 영향 없음 (1.21.1 swim 수직 속도는
        //      swimDown 무관). B-9 메인 분류 재작성 시 재검토.
        //   isTunnelAhead → 아래 private 헬퍼.
        SmartMovingClientState.SwimBorderValues swimVals =
                SmartMovingClientState.computeSwimBorderValues(player);
        boolean couldStandUp = swimVals.playerSwimWaterBorder >= 0
                            && swimVals.minPlayerSwimWaterDepth <= 1.5;
        boolean wantShallowSwim = couldStandUp && (wasSwimming || wasDiving);
        if (wantShallowSwim) {
            int px = (int) Math.floor(player.getX());
            int py = (int) Math.floor(player.getY());
            int pz = (int) Math.floor(player.getZ());
            World world = player.getWorld();
            // **B-5 (2) 해소 (세션 135)**: 원본 `getClimbingOrientations(sp, true, true)` 는
            //   대각 포함 8방향 (L285 `true, true` 파라미터). 기존 4방향 (Direction.HORIZONTAL)
            //   근사 → 대각 4방향 추가. Orientation 의 8 방향 상수 대응:
            //   PZ/NZ/ZP/ZN (4 straight) + PP/PN/NP/NN (4 대각).
            int[][] dirs8 = {
                    { 1, 0}, {-1, 0}, {0,  1}, {0, -1},   // 4-HORIZONTAL (PZ/NZ/ZP/ZN)
                    { 1, 1}, { 1,-1}, {-1, 1}, {-1, -1}   // 4-대각       (PP/PN/NP/NN)
            };
            for (int[] d : dirs8) {
                if (isTunnelAhead(world, px, py, pz, d[0], d[1])) {
                    wantShallowSwim = false;
                    break;
                }
            }
        }
        // **B-9h 해소 (세션 130)**: 원본 L243-L244 `swimDown = sneak && _swimDownOnSneak` +
        //   L292-L296 `if (wasSwimming && wantShallowSwim && swimDown) { swimDown=false;
        //   isFakeShallowWaterSneaking=true; }` 통합. swimDown 은 swimming A 경로
        //   motionYDiff 계산 시 소비 (원본 L320-L321).
        boolean swimDown = player.isSneaking() && cfg.swimDownOnSneak;
        if (wasSwimming && wantShallowSwim && swimDown) {
            swimDown = false;
            sm.isFakeShallowWaterSneaking = true;
        } else {
            sm.isFakeShallowWaterSneaking = false;
        }

        float moveForward = (float) movementInput.z;
        float moveStrafe  = (float) movementInput.x;

        // 🔵 (2026-05-20, BUG 3 fix #105) speedFactor 4-factor 곱 + swim/dive 분기 별도 곱셈.
        //   원본 SmartMovingSelf L119: `speedFactor = getConfigSpeedFactor() * getPotionSpeedFactor()
        //     * getNonSlowInputSpeedFactor(moveForward, moveStrafing)` 후 L129
        //     `speedFactor *= getSlowInputSpeedFactor()` (vanilla 아닐 때).
        //   원본 L477-479: `if(diving) sf *= _diveSpeedFactor; if(swimming) sf *= _swimSpeedFactor;` —
        //     swim/dive 분기만 별도 곱. dipping 시 적용 X.
        //   기존 매핑 BUG:
        //     1. `getCombinedSpeedFactor` (= config × potion) 만 사용 → nonSlow (sprint 1.5×)
        //        + slow factor 누락 → 원본보다 ~33% 느림 (= 사용자 보고 "속도 느림").
        //     2. dipping 시도 `swimSpeedFactor` 곱셈 (= sm.isDiving ? dive : swim, dipping false 매치) —
        //        원본은 dipping 시 적용 X.
        //   해결: `Mover.getSpeedFactor` (= 4 factor 곱 1:1) 사용 + swim/dive 매치 시만 분기별 곱셈.
        float speedFactor = SmartMovingMover.getSpeedFactor(player, sm, cfg);
        if (sm.isDiving)         speedFactor *= cfg.diveSpeedFactor;
        else if (sm.isSwimming_sm) speedFactor *= cfg.swimSpeedFactor;
        // dipping 시 분기별 factor 적용 X (원본 L477-479)

        Vec3d vel = player.getVelocity();
        double motionX = vel.x;
        double motionY = vel.y;
        double motionZ = vel.z;

        // 점프(다이브업) / 스닉(다이브다운) 키
        boolean diveUp   = jumping;
        boolean diveDown = player.isSneaking() && sm.isDiving;

        // **B-9b 해소 (세션 129)**: 원본 L306 `moveSwim` + A/B 경로 판정 이식.
        //   moveSwim = pitch < 0 && forward > 0 || pitch > 0 && forward < 0
        //   (pitch 와 forward 부호 반대 — 위 보며 후진 또는 아래 보며 전진 = 입수 자세)
        //   isPathA = diveUp || moveSwim || wantShallowSwim
        //     A 경로: 능동적 수영 (< 1.4 dipping, < 1.9 swimming, else diving)
        //     B 경로: 수동적 떠있기 (< 1.5 dipping, else diving — swimming 없음)
        boolean moveSwim = (player.getPitch() < 0F && moveForward > 0F)
                        || (player.getPitch() > 0F && moveForward < 0F);
        boolean isPathA = diveUp || moveSwim || wantShallowSwim;

        // **B-9b 상태 재분류 (세션 129)**: A/B 경로에 따라 isDipping/isSwimming_sm/isDiving
        //   을 updateSwimState 의 A 경로 기본 분류에서 재조정. [0, 2] 구간에서만 재조정
        //   (updateSwimState 는 A 경로 threshold 1.4/1.9 기본. B 경로는 1.5 + swimming 없음).
        //   isCrawling||isClimbCrawling||isCrawlClimbing 강제 dipping (원본 L301) 은
        //   updateSwimState L135 에서 이미 처리됨 — 여기선 수정 금지.
        double playerSwimWaterBorder9b = swimVals.playerSwimWaterBorder;
        boolean isForceDipping9b = sm.isCrawling || sm.isClimbCrawling || sm.isCrawlClimbing;
        if (!isForceDipping9b
                && playerSwimWaterBorder9b >= 0 && playerSwimWaterBorder9b <= 2) {
            double offset9b = playerSwimWaterBorder9b + 0.1625D;
            if (isPathA) {
                // A 경로 (원본 L307-L358): 1.4 / 1.9 threshold
                sm.isDipping     = offset9b < 1.4D;
                sm.isSwimming_sm = offset9b >= 1.4D && offset9b < 1.9D;
                sm.isDiving      = offset9b >= 1.9D;
            } else {
                // B 경로 (원본 L360-L398): 1.5 threshold, swimming 없음
                sm.isDipping     = offset9b < 1.5D;
                sm.isSwimming_sm = false;
                sm.isDiving      = offset9b >= 1.5D;
            }
            // Config 게이트 재적용 (원본 L436-L441)
            if (!cfg.isSwimmingEnabled()) {
                sm.isSwimming_sm = false;
                sm.isDipping     = false;
            }
            if (!cfg.isDivingEnabled()) {
                sm.isDiving = false;
            }
        }
        // **B-9f 해소 (세션 130)**: (<0) 구간 handleSwimmingRejected (원본 L413-L414).
        //   playerSwimWaterBorder < 0 이면 물 위/밖 → SM 미처리, vanilla travel() 위임.
        //   강제 dipping (isCrawling 계열) 경로는 예외 (원본 L301-L302 이미 isDipping=true).
        else if (!isForceDipping9b && playerSwimWaterBorder9b < 0) {
            return false;
        }

        // **B-9g 해소 (세션 130)**: 원본 L445-L446 `if (diveUp) motionY -= 0.04` 보정.
        //   swimming/diving/dipping 공통 — motion 계산 진입 전 수직 모션 감쇠.
        if (diveUp) {
            motionY -= 0.039999999105930328D;
        }

        // **B-9a 해소 (세션 128)**: handleSwimming 내부 offset 계산도 AABB 정밀화.
        //   `dippingDepth` 가 이미 `playerSwimWaterBorder` 시멘틱이므로 직접 사용.
        //   원본 L305: `offset = playerSwimWaterBorder + 0.1625D`
        if (sm.isDipping) {
            // **B-9c dipping 분기 (세션 129)**: 원본 L309-L316 (A) / L362-L369 (B).
            //   A 경로: offset < 1.0 → -0.02D, else → -0.01D
            //   B 경로: 양 branch 모두 -0.02D (원본 L365-L368 — 중복이지만 원본 그대로)
            // 🔵 (BUG 4 fix #106) damping 순서 정정 — 원본 L450-471 (damping 먼저) →
            //   L502 (motionY += motionYDiff). 기존 (motion+diff)*damp 식은 terminal velocity
            //   ~15% 작음. damp 먼저 → diff/fly 가산 식 1:1 매핑.
            double dippingOffset = sm.dippingDepth + 0.1625D;
            double dippingYDiff;
            if (isPathA) {
                dippingYDiff = dippingOffset < 1.0D ? -0.02D : -0.01D;
            } else {
                dippingYDiff = -0.02D;
            }
            motionX *= DAMPING_DIPPING_XZ;
            motionY *= DAMPING_DIPPING_Y;
            motionZ *= DAMPING_DIPPING_XZ;
            Vec3d fly = moveFlying(player, moveStrafe, moveForward, BASE_SWIM_SPEED * speedFactor);
            motionX += fly.x;
            motionY += dippingYDiff;
            motionZ += fly.z;

        } else if (sm.isSwimming_sm) {
            // **B-9c 해소 (세션 129, B-9h 갱신 세션 130)**: 원본 L317-L348 A 경로 swimming
            //   11단계 테이블 복원 + swimDown 분기 (원본 L320-L321).
            //   B 경로는 isSwimming_sm=false 로 재분류되므로 이 분기 진입 없음.
            //   원본 L320: if (swimDown) motionYDiff = -0.05 * (isFast ? sprintFactor : 1)
            //   원본 L322 부터 `if (offset < 1.6)` 등은 swimDown 분기 뒤 별도 if — swimDown 이
            //   true 여도 offset 조건 충족하면 덮어쓰기 (원본 구조 그대로 이식).
            double offset = sm.dippingDepth + 0.1625D;
            double motionYDiff;
            if (swimDown) {
                motionYDiff = -0.05D * (sm.isFast ? cfg.sprintFactor : 1F);
            } else if (offset < 1.5D) {
                motionYDiff = -0.02D;
            } else {
                motionYDiff = 0D;   // 원본 L320 들어가지 않은 경로의 초기값
            }
            // 원본 L324 이후는 `if` (not else-if) — offset 조건 충족 시 덮어쓰기
            if      (offset < 1.6D)   motionYDiff = -0.01D;
            else if (offset < 1.62D)  motionYDiff = -0.005D;
            else if (offset < 1.64D)  motionYDiff = -0.0025D;
            else if (offset < 1.66D)  motionYDiff = -0.00125D;
            else if (offset < 1.664D) motionYDiff = -0.000625D;
            else if (offset < 1.668D) motionYDiff =  0D;
            else if (offset < 1.672D) motionYDiff =  0.000625D;
            else if (offset < 1.676D) motionYDiff =  0.00125D;
            else if (offset < 1.68D)  motionYDiff =  0.0025D;
            else if (offset < 1.7D)   motionYDiff =  0.005D;
            else if (offset < 1.8D)   motionYDiff =  0.01D;
            else                       motionYDiff =  0.02D;

            // 🔵 (BUG 4 fix #106) damping 순서 정정 — 원본 L450-471 (damping 먼저) →
            //   L502 (motionY += motionYDiff). damp 먼저 → fly + diff 가산.
            motionX *= DAMPING_SWIMMING;
            motionY *= DAMPING_SWIMMING;
            motionZ *= DAMPING_SWIMMING;
            Vec3d fly = moveFlying(player, moveStrafe, moveForward, BASE_SWIM_SPEED * speedFactor);
            motionX += fly.x;
            motionY += motionYDiff;
            motionZ += fly.z;

            sm.heightOffset = -1F;

        } else { // isDiving
            // **B-9d 해소 (세션 129, B-9e 갱신 세션 130)**: 원본 L349-L358 (A 경로 diving
            //   [1.9, 2]) + L370-L397 (B 경로 diving 10단계 [1.5, 2]) + L400-L412 ((2, ∞)
            //   구간 diving) 모두 복원.
            //   A 경로: diveUp / diveDown / default = moveSwim ? 0.04 : 0.02
            //   B 경로: diveDown / offset 10단계 (< 1.8 -0.02 ~ else 0.01)
            //   (2, ∞): diveUp 시 isFast+playerSwimWaterBorder<2.5+isAir(j+3) → 0.11/sprintFactor
            //          / else → 0.01+0.1*sf / diveDown → 0.01-0.1*sf / default → 0.01
            double motionYDiff = 0D;
            double offset9d = sm.dippingDepth + 0.1625D;
            double psw9e = swimVals.playerSwimWaterBorder;
            if (psw9e > 2) {
                // **B-9e 해소 (세션 130)**: (2, ∞) 구간 diving (원본 L400-L412)
                if (diveUp) {
                    // 원본 L404: isFast && psw < 2.5 && isAirBlock(i, j + 3, k) → 스프린트 점프 부스트
                    if (sm.isFast && psw9e < 2.5D
                            && player.getWorld().isAir(new net.minecraft.util.math.BlockPos(
                                    swimVals.i, swimVals.j + 3, swimVals.k))) {
                        motionYDiff = 0.11D / cfg.sprintFactor;
                    } else {
                        motionYDiff = 0.01D + 0.1D * speedFactor;
                    }
                } else if (diveDown) {
                    motionYDiff = 0.01D - 0.1D * speedFactor;
                } else {
                    motionYDiff = 0.01D;
                }
            } else if (isPathA) {
                // A 경로 diving (원본 L349-L358)
                if (diveUp) {
                    motionYDiff = 0.05D * (sm.isFast ? cfg.sprintFactor : 1F);
                } else if (diveDown) {
                    motionYDiff = 0.01D - 0.1D * speedFactor;
                } else {
                    motionYDiff = moveSwim ? 0.04D : 0.02D;
                }
            } else {
                // B 경로 diving (원본 L370-L397) — offset 10단계
                if (diveDown) {
                    motionYDiff = 0.01D - 0.1D * speedFactor;
                } else if (offset9d < 1.8D)  motionYDiff = -0.02D;
                else if (offset9d < 1.82D) motionYDiff = -0.01D;
                else if (offset9d < 1.84D) motionYDiff = -0.005D;
                else if (offset9d < 1.86D) motionYDiff = -0.0025D;
                else if (offset9d < 1.864D) motionYDiff = -0.00125D;
                else if (offset9d < 1.868D) motionYDiff = 0D;
                else if (offset9d < 1.872D) motionYDiff = 0.00125D;
                else if (offset9d < 1.876D) motionYDiff = 0.0025D;
                else if (offset9d < 1.88D)  motionYDiff = 0.005D;
                else if (offset9d < 1.9D)   motionYDiff = 0.01D;
                else                        motionYDiff = 0.01D;
            }

            // 🔵 (BUG 4 fix #106) diving levitating 분기 별도 처리 + damping 순서 정정.
            //   원본 L489-496:
            //     if(diving) {
            //         if(diveUp || diveDown || levitating)
            //             motionY = (motionY + motionYDiff) * 0.6;  // 강한 수직 제어 + 수평 0
            //         else
            //             moveFlying((float)motionYDiff, ..., _diveControlVertical);
            //         moveFlying = false;  // 후속 horizontal moveFlying 차단
            //     }
            //   기존 매핑 BUG: levitating 분기 없이 항상 5인자 moveFlying → 가만히 + jump 홀딩 시
            //     surface 도달 안 됨 (= 사용자 보고 (5) "호흡 안 차감 위치 수면" 미달성).
            //   해결: (a) diveUp || diveDown || levitating → (motionY + diff) * 0.6 식 + 수평 damping
            //           만 (= moveFlying 호출 X). (b) 일반 → 5인자 moveFlying (= 마우스 방향 이동).
            //   damping 순서도 정정: motion *= 0.83 (먼저) → fly 가산 (= 원본 L450 → L494).
            motionX *= DAMPING_DIVING;
            motionY *= DAMPING_DIVING;
            motionZ *= DAMPING_DIVING;
            if (diveUp || diveDown || sm.isLevitating) {
                // 원본 L491-492: (motionY * 0.83 + motionYDiff) * 0.6 식 → terminal v = 1.2 m/s
                //   surface 도달 빠름. 수평 motion 변경 X (= 원본 moveFlying=false 효과).
                motionY = (motionY + motionYDiff) * 0.6D;
            } else {
                // 원본 L494: 5인자 moveFlying (pitch 반영 수직 + horizontal). moveUpward=motionYDiff.
                Vec3d fly = moveFlying(player, (float) motionYDiff, moveStrafe, moveForward,
                        BASE_SWIM_SPEED * speedFactor, cfg.diveControlVertical);
                motionX += fly.x;
                motionY += fly.y;
                motionZ += fly.z;
            }

            sm.heightOffset = -1F;
        }

        // **B-11 해소 (세션 130)**: 원본 L513-L536 얕은 물 특수 분기.
        //   isShallowDiveOrSwim && realMinPlayerSwimWaterDepth < SwimCrawlWaterBottomBorder(0.55)
        //   → isSlow 면 크롤 전환, 아니면 걷기 전환 + 바닥 위치로 이동.
        //   isShallowDiveOrSwim 는 B-9b 재분류 후 재계산 (원본 L507).
        //   B-42a `getMaxPlayerSolidBetween` + B-42d `realMinPlayerSwimWaterDepth` 소비.
        boolean isShallowDiveOrSwim11 = couldStandUp && (sm.isDiving || sm.isSwimming_sm);
        sm.isShallowDiveOrSwim = isShallowDiveOrSwim11;
        if (isShallowDiveOrSwim11
                && swimVals.realMinPlayerSwimWaterDepth < SWIM_CRAWL_BOTTOM) {
            if (sm.isSlow) {
                // 얕은 물 swim/dive → 크롤 전환 (원본 L515-L524)
                sm.heightOffset          = -1F;
                sm.isCrawling            = true;
                sm.isDiving              = false;
                sm.isSwimming_sm         = false;
                sm.isShallowDiveOrSwim   = false;
                sm.isDipping             = true;
            } else {
                // 얕은 물 swim/dive → 걷기 전환 (원본 L525-L535)
                sm.heightOffset = 0F;
                double minY11 = player.getBoundingBox().minY;
                double maxY11 = player.getBoundingBox().maxY;
                double groundY11 = SmartMovingClientState.getMaxPlayerSolidBetween(
                        player, minY11, maxY11, 0);
                player.move(MovementType.SELF, new Vec3d(0, groundY11 - minY11, 0));
                sm.isCrawling            = false;
                sm.isDiving              = false;
                sm.isSwimming_sm         = false;
                sm.isShallowDiveOrSwim   = false;
                sm.isDipping             = true;
            }
        }

        // ── [8-5] isJumpingOutOfWater ────────────────────────────────────────
        // 🔵 (2026-05-20, Phase 0 검증 fix): 원본 L499-500 `else if(isJumpingOutOfWater) sp.motionY = 0.3`
        //   1:1 매핑. sm.isJumpingOutOfWater 필드는 updateSwimState L212-220 에서 원본 L486-487
        //   완전 식 (`!isSlow` + `wasJumpingOutOfWater` 포함) 으로 이미 갱신됨.
        //   기존 매핑은 식 재계산 — `!sm.isSlow` 가드 누락 + `sm.wasJumpingOutOfWater` OR 항 누락 →
        //   sneak hold 중 수면 점프 차단 안 됨 + 연속 frame jump 유지 매치 안 됨. 사용자 자각
        //   가능 차이 (sneak 잠수 중 모서리 벽 충돌 시 의도치 않은 surface 점프).
        if (sm.isJumpingOutOfWater) {
            motionY = JUMP_OUT_OF_WATER_VELOCITY;
        }

        player.setVelocity(motionX, motionY, motionZ);
        player.move(MovementType.SELF, player.getVelocity());
        player.fallDistance = 0F;
        return true;
    }

    // ── moveFlying (원본 SmartMovingBase.md L77-114 — 5-인자 버전 1:1) ──────

    /**
     * 원본 SmartMovingBase.moveFlying(moveUpward, strafe, forward, speed, treeDimensional)
     * 1:1 이식. 기존 4-인자 버전(수평 전용)의 정규화 누락 [오역] 도 함께 복원.
     *
     * 1단계: 수평 방향 벡터 (YAW 기반) — 정규화 하한 1.0F 포함
     *   total = sqrt(strafe² + forward²); if (total < 0.01) skip; if (total < 1.0) total = 1.0
     *   factor 기반 strafe/forward 성분 × (cos, -sin, sin, cos) 조합
     *
     * 2단계: 피치 기반 수직 보정 (treeDimensional=true 시)
     *   rotation = toRadians(pitch); horizFactor = cos; vertFactor = -sin * signum(forward)
     *
     * 3단계: 최종 모션 합산
     *   diffMY = sqrt(diffMXForward² + diffMZForward²) * vertFactor + moveUpward
     *
     * 4단계: 비대칭 total 공식 sqrt(sqrt(dx²+dz²) + dy²) 로 factor 적용
     */
    private static Vec3d moveFlying(ClientPlayerEntity player, float moveUpward,
                                     float strafe, float forward, float speed, boolean treeDimensional) {
        float diffMXStrafing = 0F, diffMXForward = 0F, diffMZStrafing = 0F, diffMZForward = 0F;
        float total = (float) Math.sqrt(strafe * strafe + forward * forward);
        if (total >= 0.01F) {
            if (total < 1.0F) total = 1.0F;
            float strafeFactor  = strafe  / total;
            float forwardFactor = forward / total;
            float yawRad = (float) Math.toRadians(player.getYaw());
            float sin = (float) Math.sin(yawRad);
            float cos = (float) Math.cos(yawRad);
            diffMXStrafing =  strafeFactor  * cos;
            diffMXForward  = -forwardFactor * sin;
            diffMZStrafing =  strafeFactor  * sin;
            diffMZForward  =  forwardFactor * cos;
        }

        float rotation = treeDimensional ? (float) Math.toRadians(player.getPitch()) : 0F;
        float horizFactor = (float) Math.cos(rotation);
        float vertFactor  = (float) (-Math.sin(rotation) * Math.signum(forward));

        float diffMX = diffMXForward * horizFactor + diffMXStrafing;
        float diffMY = (float) Math.sqrt(diffMXForward * diffMXForward + diffMZForward * diffMZForward)
                       * vertFactor + moveUpward;
        float diffMZ = diffMZForward * horizFactor + diffMZStrafing;

        // 비대칭 total 공식 — 원본 주의 사항: sqrt(sqrt(x²+z²) + y²), NOT sqrt(x²+y²+z²)
        float total2 = (float) Math.sqrt(Math.sqrt(diffMX * diffMX + diffMZ * diffMZ) + diffMY * diffMY);
        if (total2 > 0.01F) {
            float factor = speed / total2;
            return new Vec3d(diffMX * factor, diffMY * factor, diffMZ * factor);
        }
        return Vec3d.ZERO;
    }

    /** 4-인자 wrapper (수평 전용) — moveUpward=0, treeDimensional=false. */
    private static Vec3d moveFlying(ClientPlayerEntity player, float strafe, float forward, float speed) {
        return moveFlying(player, 0F, strafe, forward, speed, false);
    }

    // ── [Phase C-3] handleLava (포커스 #2.6 세션 4) ───────────────────────────

    /**
     * Phase C-3 (포커스 #2.6 세션 4) — handleLava 본체 1:1 이식.
     *
     * 원본 `SmartMovingSelf.handleLava()` L578-L600 (③ 리서치 §2 발췌):
     * <pre>
     * private boolean handleLava(float moveForward, float moveStrafing,
     *                            boolean handledSwimming, boolean isLiquidClimbing) {
     *     boolean handleLava = !isFlying && !handledSwimming && !isLiquidClimbing
     *                       && sp.handleLavaMovement();
     *     if (handleLava) {
     *         standupIfPossible();
     *         resetClimbing();
     *         resetSwimming();
     *         double d1 = sp.posY;
     *         sp.moveFlying(moveStrafing, moveForward, 0.02F);
     *         sp.moveEntity(sp.motionX, sp.motionY, sp.motionZ);
     *         sp.motionX *= 0.5D;
     *         sp.motionY *= 0.5D;
     *         sp.motionZ *= 0.5D;
     *         sp.motionY -= 0.02D;
     *         if (sp.isCollidedHorizontally && sp.isOffsetPositionInLiquid(
     *                 sp.motionX, ((sp.motionY + 0.60000002384185791D) - sp.posY) + d1, sp.motionZ)) {
     *             sp.motionY = 0.30000001192092896D;
     *         }
     *     }
     *     return handleLava;
     * }
     * </pre>
     *
     * <p><b>호출 위치</b>: {@link choco.ratel.smartmoving.mixin.client.MixinLivingEntityClient}
     * 의 {@code sm_travel_client} 에서 handleSwimming 직후 (handledSwimming=false 분기).
     * 진입 시 ci.cancel() 로 vanilla travel 차단.
     *
     * <p><b>진입 조건 (4-AND)</b>:
     * <ul>
     *   <li>{@code !sm.isFlying} (SM 비행 X)</li>
     *   <li>{@code !handledSwimming} — 호출 위치 보장 (handleSwimming false 후 진입)</li>
     *   <li>{@code !sm.isLiquidClimbing} (액체 클라이밍 X)</li>
     *   <li>{@code player.isInLava()} (lava 안)</li>
     * </ul>
     *
     * <p><b>부작용 평가</b> (포커스 #2.6 세션 4 전수 조사):
     * <ul>
     *   <li>swimUpward (점프 +0.04F): tickMovement L2649 외부 → vanilla 자동 처리 ✅</li>
     *   <li>lava damage (setOnFireFromLava): {@code Entity.tick} 별개 → 차단 안 됨 ✅</li>
     *   <li>lava sound / particle: 별개 시스템 → 차단 안 됨 ✅</li>
     *   <li>vanilla travel() lava 분기 (L2123-L2142): SM 이 동등 처리 (벽 점프 motionY=0.3 포함)</li>
     * </ul>
     *
     * <p><b>vanilla 와의 미세 차이 (원본 1.7.10 충실)</b>:
     * <ul>
     *   <li>damping Y: 원본 {@code 0.5D} vs vanilla {@code 0.8F} (SM 가 더 빠른 낙하)</li>
     *   <li>중력: 원본 {@code -0.02D} 고정 vs vanilla {@code -d/4} (gravity attribute, slow_falling 영향)</li>
     * </ul>
     *
     * <p><b>호출 컨텍스트</b>:
     * <ul>
     *   <li>{@code lavaLikeWater = true} (Creative): handleSwimming 가 lava 처리 →
     *     handledSwimming=true → 본 메서드 진입 X</li>
     *   <li>{@code lavaLikeWater = false} (Survival 기본): handleSwimming false →
     *     본 메서드 진입 → SM 자체 lava 이동 (즉사 전 짧은 motion)</li>
     * </ul>
     *
     * @return true 시 호출 측에서 ci.cancel() 처리하여 vanilla travel 차단
     */
    public static boolean handleLava(ClientPlayerEntity player, SmartMovingClientState sm,
                                      Vec3d movementInput) {
        // 진입 조건 (원본 L580): handledSwimming 은 호출 위치에서 보장
        boolean handleLavaFlag = !sm.isFlying && !sm.isLiquidClimbing && player.isInLava();
        if (!handleLavaFlag) return false;

        // 원본 L582 standupIfPossible — heightOffset >= 0 확인 후 -1F 인 경우 복원
        sm.standupIfPossible(player);
        // 원본 L583 resetClimbing — 클라이밍 상태 리셋
        sm.resetClimbing();
        // 원본 L584 resetSwimming — 수영 8 필드 리셋 (private static, 같은 클래스)
        resetSwimming(sm);

        // 원본 L586: d1 = posY 저장 (벽 점프 isOffsetPositionInLiquid 체크용 prev posY)
        double d1 = player.getY();

        // 원본 L587: moveFlying(strafe=movementInput.x, forward=movementInput.z, 0.02F)
        Vec3d moved = moveFlying(player, (float) movementInput.x, (float) movementInput.z, 0.02F);
        player.setVelocity(moved);

        // 원본 L588: moveEntity(motionX, motionY, motionZ) — 실제 이동
        player.move(MovementType.SELF, player.getVelocity());

        // 원본 L589-L591: damping 0.5 (X/Y/Z 모두)
        Vec3d vel = player.getVelocity();
        double mx = vel.x * 0.5D;
        double my = vel.y * 0.5D;
        double mz = vel.z * 0.5D;

        // 원본 L592: 중력 -0.02D (vanilla 의 -gravity/4 와 미세 차이 — 원본 충실)
        my -= 0.02D;

        // 원본 L594-L597: lava 벽 점프
        //   isCollidedHorizontally && isOffsetPositionInLiquid(motionX,
        //       ((motionY + 0.60000002384185791D) - posY) + d1, motionZ)
        //   → motionY = 0.30000001192092896D
        // 1.21.1 매핑: isOffsetPositionInLiquid → BoundingBox.offset() + world.containsFluid()
        if (player.horizontalCollision) {
            Box bb = player.getBoundingBox().offset(
                    mx,
                    ((my + 0.60000002384185791D) - player.getY()) + d1,
                    mz);
            if (player.getWorld().containsFluid(bb)) {
                my = 0.30000001192092896D;
            }
        }

        // 최종 setVelocity (원본은 motionX/Y/Z 직접 변경, 1.21.1 은 setVelocity 일괄)
        player.setVelocity(mx, my, mz);

        // 원본 L599 return handleLava
        return true;
    }

    /**
     * 원본 Orientation.isTunnelAhead(world, i, j, k) 1:1 이식.
     *   remoteId = world.getBlock(i+_i, j+1, k+_k)
     *   if (isFullEmpty(remoteId)) {
     *     aboveMat = world.getBlock(i+_i, j+2, k+_k).getMaterial()
     *     if (aboveMat != null && isSolid(aboveMat)) return true
     *   }
     *   return false
     *
     * **B-5 (2) 해소 (세션 135)**: ox/oz 오프셋 파라미터 버전 — 대각 포함 8방향 지원.
     * 기존 `Direction` 파라미터 (4방향만) → offset 직접 받아 원본 `Orientation._i/_k` 대응.
     *
     * 1.21.1 매핑:
     *   isFullEmpty(block) → collisionShape.isEmpty() (간소; 간판/압력판 예외는 생략)
     *   isSolid(material) → state.isOpaqueFullCube() 근사 (정확 대응 API 부재 시)
     */
    private static boolean isTunnelAhead(World world, int i, int j, int k, int ox, int oz) {
        BlockPos one = new BlockPos(i + ox, j + 1, k + oz);
        BlockPos two = new BlockPos(i + ox, j + 2, k + oz);
        boolean emptyAtOne = world.getBlockState(one).getCollisionShape(world, one).isEmpty();
        if (!emptyAtOne) return false;
        return !world.getBlockState(two).getCollisionShape(world, two).isEmpty();
    }
}
