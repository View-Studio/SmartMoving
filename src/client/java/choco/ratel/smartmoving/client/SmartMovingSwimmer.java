package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
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

    // SwimCrawlWater 경계 상수 (SmartMovingContext, R-06)
    // 1.21.1: wasHeightOffset=0 근사이므로 playerCrawlWaterBorder = dippingDepth 그대로 비교
    private static final float SWIM_CRAWL_TOP = 0.65F;  // 크롤→수영 전환 하한 (SwimCrawlWaterTopBorder)
    private static final float SWIM_CRAWL_MAX = 1.0F;   // 크롤 수경계 최대 (SwimCrawlWaterMaxBorder)

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
            return;
        }

        double fluidHeight = player.getFluidHeight(FluidTags.WATER);
        sm.dippingDepth = (float)fluidHeight;

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
            return;
        }

        double offset = fluidHeight + 0.1625D;
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
            for (Direction d : Direction.Type.HORIZONTAL) {
                if (isTunnelAhead(world, px, py, pz, d)) {
                    wantShallowSwim = false;
                    break;
                }
            }
        }
        // swimDown 지역 변수는 diving 분기에서 diveDown 과 분리되어 사용 — 여기서는 설정 블록만.
        if (wasSwimming && wantShallowSwim
                && player.isSneaking() && cfg.swimDownOnSneak) {
            sm.isFakeShallowWaterSneaking = true;
            // 원본 L244: swimDown=false — handleSwimming 내부 이후 로직에서 sneak-down 억제에
            //   사용. 1.21.1 는 isSwimming 분기의 수직 속도에 swimDown 이 영향 주지 않으므로 불필요.
        } else {
            sm.isFakeShallowWaterSneaking = false;
        }

        float moveForward = (float) movementInput.z;
        float moveStrafe  = (float) movementInput.x;

        // B-3 (세션 27): User 배율 주입. 원본 SmartMovingSelf L476-L494 에서 speedFactor 는
        // Self L119 지역변수(getConfigSpeedFactor * getPotionSpeedFactor * ...) 에서 시작,
        // 이후 `*= _diveSpeedFactor.value` 또는 `*= _swimSpeedFactor.value` 로 곱셈.
        // 1.21.1 는 vanilla travel() cancel 로 직접 계산 — getMovementSpeed inject (B-2) 영향
        // 없음. Mover.getCombinedSpeedFactor(player, cfg) 로 User 배율 + 포션 효과 포함.
        // Creative 게이트는 getConfigSpeedFactor 내부에서 자동 처리 (B-6).
        float speedFactor = (sm.isDiving ? cfg.diveSpeedFactor : cfg.swimSpeedFactor)
                          * SmartMovingMover.getCombinedSpeedFactor(player, cfg);

        Vec3d vel = player.getVelocity();
        double motionX = vel.x;
        double motionY = vel.y;
        double motionZ = vel.z;

        // 점프(다이브업) / 스닉(다이브다운) 키
        boolean diveUp   = jumping;
        boolean diveDown = player.isSneaking() && sm.isDiving;

        if (sm.isDipping) {
            // 수면 경계 — 약간 아래로 당기는 힘 + 수평 이동
            // 원본: offset < 1.0 → motionYDiff = -0.02D, else → -0.01D
            Vec3d fly = moveFlying(player, moveStrafe, moveForward, BASE_SWIM_SPEED * speedFactor);
            motionX += fly.x;
            motionZ += fly.z;
            motionX *= DAMPING_DIPPING_XZ;
            double dippingOffset = player.getFluidHeight(FluidTags.WATER) + 0.1625D;
            double dippingYDiff = dippingOffset < 1.0D ? -0.02D : -0.01D;
            motionY = (motionY + dippingYDiff) * DAMPING_DIPPING_Y;
            motionZ *= DAMPING_DIPPING_XZ;

        } else if (sm.isSwimming_sm) {
            // 수면 수영 — offset 구간에 따라 수직력 세분화
            // 원본: SmartMovingSelf.handleSwimming() 13단계 테이블 (229-576줄)
            double offset = player.getFluidHeight(FluidTags.WATER) + 0.1625D;
            double motionYDiff;
            if      (offset < 1.5D)   motionYDiff = -0.02D;
            else if (offset < 1.6D)   motionYDiff = -0.01D;
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

            Vec3d fly = moveFlying(player, moveStrafe, moveForward, BASE_SWIM_SPEED * speedFactor);
            motionX += fly.x;
            motionY += motionYDiff;
            motionZ += fly.z;
            motionX *= DAMPING_SWIMMING;
            motionY *= DAMPING_SWIMMING;
            motionZ *= DAMPING_SWIMMING;

            sm.heightOffset = -1F;

        } else { // isDiving
            // 완전 잠수 — diveUp/diveDown 수직 제어
            // 원본: diveDown = 0.01 - 0.1 * speedFactor (SmartMovingSelf 229-576줄)
            double motionYDiff = 0D;
            if (diveUp) {
                motionYDiff = 0.05D * speedFactor;
            } else if (diveDown) {
                motionYDiff = 0.01D - 0.1D * speedFactor;
            }

            // 원본 SmartMovingSelf.md L476:
            //   moveFlying((float)motionYDiff, strafe, forward, 0.02F * speedFactor, _diveControlVertical.value)
            // moveUpward 파라미터에 motionYDiff 를 전달 → 5-인자 diffMY 에 포함되어 반환됨.
            // treeDimensional=true 이면 pitch 반영 수직 이동도 추가.
            Vec3d fly = moveFlying(player, (float) motionYDiff, moveStrafe, moveForward,
                    BASE_SWIM_SPEED * speedFactor, cfg.diveControlVertical);
            motionX += fly.x;
            motionY += fly.y; // moveUpward(=motionYDiff) 이미 fly.y 에 포함 — 별도 가산 금지
            motionZ += fly.z;
            motionX *= DAMPING_DIVING;
            motionY *= DAMPING_DIVING;
            motionZ *= DAMPING_DIVING;

            sm.heightOffset = -1F;
        }

        // ── [8-5] isJumpingOutOfWater ────────────────────────────────────────
        // 원본: wantJumpOutOfWater = 수평이동 + 벽충돌 + diveUp + !isSlow
        //       isJumpingOutOfWater = wantJumpOutOfWater && (waterMovementTicks > 10 || onGround)
        boolean wantJumpOut = (moveForward != 0 || moveStrafe != 0)
                && player.horizontalCollision
                && diveUp;
        if (wantJumpOut && (sm.waterMovementTicks > 10 || player.isOnGround())) {
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

    /**
     * 원본 Orientation.isTunnelAhead(world, i, j, k) 1:1 이식.
     *   remoteId = world.getBlock(i+_i, j+1, k+_k)
     *   if (isFullEmpty(remoteId)) {
     *     aboveMat = world.getBlock(i+_i, j+2, k+_k).getMaterial()
     *     if (aboveMat != null && isSolid(aboveMat)) return true
     *   }
     *   return false
     *
     * 1.21.1 매핑:
     *   _i/_k → dir.getOffsetX/Z
     *   isFullEmpty(block) → collisionShape.isEmpty() (간소; 간판/압력판 예외는 생략)
     *   isSolid(material) → state.isOpaqueFullCube() 근사 (정확 대응 API 부재 시)
     */
    private static boolean isTunnelAhead(World world, int i, int j, int k, Direction dir) {
        int ox = dir.getOffsetX();
        int oz = dir.getOffsetZ();
        BlockPos one = new BlockPos(i + ox, j + 1, k + oz);
        BlockPos two = new BlockPos(i + ox, j + 2, k + oz);
        boolean emptyAtOne = world.getBlockState(one).getCollisionShape(world, one).isEmpty();
        if (!emptyAtOne) return false;
        return !world.getBlockState(two).getCollisionShape(world, two).isEmpty();
    }
}
