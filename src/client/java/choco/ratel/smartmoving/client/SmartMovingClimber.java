package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.climbing.ClimbGap;
import choco.ratel.smartmoving.climbing.FeetClimbing;
import choco.ratel.smartmoving.climbing.HandsClimbing;
import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.util.math.Box;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 클라이언트 측 클라이밍 물리 로직.
 * 원본: SmartMovingBase / SmartMovingSelf의 클라이밍 관련 메서드 이식.
 *
 * 포함 항목:
 *   1-7: getOnLadderOrVine() — 사다리/넝쿨 탐색
 *   7-1: 클라이밍 속도 상수
 *   7-2: setShouldClimbSpeed() / handleClimbing()
 *   7-3: handleCeilingClimbing()
 */
@Environment(EnvType.CLIENT)
public final class SmartMovingClimber {

    // ── 7-1: 클라이밍 속도 상수 ─────────────────────────────────────────
    // 원본: SmartMovingContext 정적 상수

    /** 빠른 상승 (FAST_UP 상태) */
    public static final double FAST_UP_MOTION        = 0.2D;
    /** 크롤 갭 진입 시 속도 상한 */
    public static final double CATCH_CRAWL_GAP_MOTION = 0.17D;
    /** 중간 상승 */
    public static final double MEDIUM_UP_MOTION       = 0.14D;
    /** 느린 상승 */
    public static final double SLOW_UP_MOTION         = 0.1D;
    /** 제자리 유지 (홀딩) */
    public static final double HOLD_MOTION            = 0.08D;
    /** 천천히 하강 */
    public static final double SINK_DOWN_MOTION       = 0.05D;
    /** 사다리 아래로 내려가기 */
    public static final double CLIMB_DOWN_MOTION      = 0.01D;
    /** 넝쿨 당기기 moveForward 값 */
    public static final float  CLIMB_PULL_MOTION      = 0.3F;

    private SmartMovingClimber() {}

    // ── 1-7: getOnLadderOrVine ───────────────────────────────────────────

    /**
     * 원본: SmartMovingBase.getOnLadderOrVine(isSmall, faceOnly)
     *
     * 플레이어 주변 4방향(North/South/East/West)을 탐색하여
     * 사다리/넝쿨 클라이밍 가능 여부와 HandsClimbing/FeetClimbing 상태를 반환한다.
     *
     * 탐색 범위:
     *   - 손 위치: player.getY() + 1 블록
     *   - 발 위치: player.getY()
     *   - isSmall=true: player.getY() - 1 블록까지 추가 탐색
     *
     * 사다리 판정:
     *   LadderBlock.FACING.getOpposite() == playerFacing → 해당 방향으로 클라이밍 가능
     *
     * 넝쿨 판정:
     *   해당 방향의 VineBlock.DIR property = true
     *   AND 그 방향 뒤 블록이 솔리드 → 클라이밍 가능
     *
     * faceOnly=true이면 player 정면 방향만 탐색.
     */
    public static void getOnLadderOrVine(
            ClientPlayerEntity player, World world,
            boolean isSmall, boolean faceOnly,
            HandsClimbing[] out_hands, FeetClimbing[] out_feet,
            ClimbGap[] out_handsGap, ClimbGap[] out_feetGap) {

        out_hands[0] = HandsClimbing.NONE;
        out_feet[0]  = FeetClimbing.NONE;
        out_handsGap[0].reset();
        out_feetGap[0].reset();

        int px = (int) Math.floor(player.getX());
        int py = (int) Math.floor(player.getY());
        int pz = (int) Math.floor(player.getZ());

        // 탐색 Y 범위: 발(py) ~ 손(py+1), isSmall이면 py-1까지 확장
        int minY = isSmall ? py - 1 : py;
        int maxY = py + 1; // 손 위치

        Direction playerFacing = player.getHorizontalFacing();

        for (Direction dir : Direction.Type.HORIZONTAL) {
            if (faceOnly && dir != playerFacing) continue;

            int bx = px + dir.getOffsetX();
            int bz = pz + dir.getOffsetZ();

            for (int by = minY; by <= maxY; by++) {
                BlockState state = world.getBlockState(new BlockPos(bx, by, bz));
                boolean isHandsLevel = (by == maxY);

                // ── 사다리 판정 ──────────────────────────────────────────
                if (state.getBlock() instanceof LadderBlock) {
                    Direction ladderFacing = state.get(LadderBlock.FACING);
                    // 사다리 방향의 반대가 플레이어 정면과 일치하면 클라이밍 가능
                    // 예: 플레이어 EAST → 사다리 FACING=WEST (서쪽 벽에 붙은 사다리)
                    if (ladderFacing.getOpposite() == playerFacing) {
                        ClimbGap gap = new ClimbGap();
                        gap.state = state;
                        gap.direction = dir;
                        if (isHandsLevel) {
                            ClimbGap[] temp = {gap};
                            out_hands[0] = out_hands[0].max(HandsClimbing.UP, out_handsGap, gap);
                        } else {
                            ClimbGap[] temp = {gap};
                            out_feet[0] = out_feet[0].max(FeetClimbing.SLOW_UP_WITH_HOLD_WITHOUT_HANDS, out_feetGap, gap);
                        }
                    }
                }

                // ── 넝쿨 판정 ───────────────────────────────────────────
                if (state.getBlock() instanceof VineBlock) {
                    // 해당 방향(dir)의 vine property 확인
                    boolean hasVineOnFace = switch (dir) {
                        case NORTH -> state.get(VineBlock.NORTH);
                        case SOUTH -> state.get(VineBlock.SOUTH);
                        case EAST  -> state.get(VineBlock.EAST);
                        case WEST  -> state.get(VineBlock.WEST);
                        default    -> false;
                    };

                    if (hasVineOnFace) {
                        // 넝쿨의 해당 방향 뒤 블록(solid)이 있어야 클라이밍 가능
                        BlockPos solidPos = new BlockPos(bx + dir.getOffsetX(), by, bz + dir.getOffsetZ());
                        if (world.getBlockState(solidPos).isSolidBlock(world, solidPos)) {
                            ClimbGap gap = new ClimbGap();
                            gap.state = state;
                            gap.direction = dir;
                            if (isHandsLevel) {
                                out_hands[0] = out_hands[0].max(HandsClimbing.UP, out_handsGap, gap);
                            } else {
                                out_feet[0] = out_feet[0].max(FeetClimbing.SLOW_UP_WITH_HOLD_WITHOUT_HANDS, out_feetGap, gap);
                            }
                        }
                    }
                }
            }
        }
    }

    // ── 7-2: setShouldClimbSpeed / setOnlyShouldClimbSpeed ──────────────

    /**
     * 원본: SmartMovingSelf.setShouldClimbSpeed(value, isUp, factor)
     *
     * 클라이밍 Y 속도를 팩터 보간하여 설정한다.
     *
     * relevant 조건: value < 0 || value > motionY
     * (더 느리거나 하강 방향일 때만 적용. 이미 충분히 빠른 경우 방치.)
     *
     * 상방 보간: motionY = (value - HOLD_MOTION) * upFactor * combinedFactor + HOLD_MOTION
     * 하방 보간: motionY = HOLD_MOTION - (HOLD_MOTION - value) * downFactor * combinedFactor
     *
     * _freeClimbingUpSpeedFactor / _freeClimbingDownSpeedFactor 원본 기본값 = 1F (PositiveFactor).
     * Standard/Simple/Smart 모드별 factored 읽기는 C-29에서 구현.
     *
     * @return true if relevant (속도를 변경함)
     */
    public static boolean setShouldClimbSpeed(ClientPlayerEntity player, SmartMovingClientState sm,
                                               double value, boolean isUp, double combinedFactor) {
        // climbIntoCount > 0이면 crawl gap 진입 중 — HoldMotion으로 강제 (C-26)
        if (sm.climbIntoCount > 0) {
            value = HOLD_MOTION;
            isUp = true;
        }

        double motionY = player.getVelocity().y;
        boolean relevant = value < 0 || value > motionY;

        if (relevant) {
            // hasClimbCrawlGap && isClimbCrawling: 상단 크롤 갭 도달 시 속도 상한 적용 (C-27)
            if (sm.hasClimbCrawlGap && sm.isClimbCrawling && value > HOLD_MOTION) {
                value = Math.min(CATCH_CRAWL_GAP_MOTION, value);
            }

            SmartMovingConfig cfg2 = SmartMovingConfig.Config;
            double newMotionY;
            if (isUp) {
                newMotionY = (value - HOLD_MOTION) * cfg2.freeClimbingUpSpeedFactor * combinedFactor + HOLD_MOTION;
            } else {
                newMotionY = HOLD_MOTION - (HOLD_MOTION - value) * cfg2.freeClimbingDownSpeedFactor * combinedFactor;
            }

            player.setVelocity(player.getVelocity().x, newMotionY, player.getVelocity().z);
        }

        // isClimbJumping = !relevant && !isClimbHolding (C-28)
        sm.isClimbJumping = !relevant && !sm.isClimbHolding;

        return relevant;
    }

    /**
     * 원본: SmartMovingSelf.setOnlyShouldClimbSpeed(value, isUp, factor)
     * setShouldClimbSpeed와 동일하나 sm.isClimbing=true도 설정한다.
     */
    public static boolean setOnlyShouldClimbSpeed(ClientPlayerEntity player, SmartMovingClientState sm,
                                                   double value, boolean isUp, double combinedFactor) {
        boolean relevant = setShouldClimbSpeed(player, sm, value, isUp, combinedFactor);
        if (relevant) sm.isClimbing = true;
        return relevant;
    }

    /**
     * 원본: SmartMovingSelf.handleClimbing() — Free 모드 핵심 로직.
     *
     * 처리 순서:
     * 1. exhaustion 체크
     * 2. 8방향 탐색 (4방향 + 대각 4방향)
     * 3. HandsClimbing/FeetClimbing 상태 집계
     * 4. grab 키 + movementForward로 wantClimbUp/wantClimbDown 판정
     * 5. setShouldClimbSpeed로 Y 속도 결정
     * 6. fallDistance = 0 (낙하 데미지 방지)
     */
    public static void handleClimbing(ClientPlayerEntity player, SmartMovingClientState sm) {
        SmartMovingConfig cfg = SmartMovingConfig.Config;

        // C-33: SM 독자 exhaustion 체크
        // 원본: exhaustionAllowsClimbing = !enabled || (exhaustion<=stop && (wasClimbing||exhaustion<=start))
        if (cfg.climbExhaustion) {
            boolean allowed = sm.exhaustion <= cfg.climbExhaustionStop
                    && (sm.wasClimbing || sm.exhaustion <= cfg.climbExhaustionStart);
            if (!allowed) return;
        }

        if (!cfg.freeClimb) {
            // Standard 모드: combinedFactor = getConfigSpeedFactor × getPotionSpeedFactor (SmartMovingSelf.md L712)
            // setOnlyShouldClimbSpeed(FAST_UP_MOTION * combinedFactor, true, 1.0D) → motionY = 0.2 * combinedFactor
            double combinedFactor = SmartMovingMover.getConfigSpeedFactor(cfg)
                                  * SmartMovingMover.getPotionSpeedFactor(player);
            setOnlyShouldClimbSpeed(player, sm, FAST_UP_MOTION * combinedFactor, true, 1.0D);
            player.fallDistance = 0;
            return;
        }

        World world = player.getWorld();
        boolean isSmall = sm.isSmall || sm.isCrawling;

        // 8방향 탐색 결과 집계
        HandsClimbing handsClimbing = HandsClimbing.NONE;
        FeetClimbing  feetClimbing  = FeetClimbing.NONE;
        ClimbGap[] handsGap = {new ClimbGap()};
        ClimbGap[] feetGap  = {new ClimbGap()};

        // 4방향(N/S/E/W) 탐색
        HandsClimbing[] tempH = {HandsClimbing.NONE};
        FeetClimbing[]  tempF = {FeetClimbing.NONE};
        ClimbGap[] tempHG = {new ClimbGap()};
        ClimbGap[] tempFG = {new ClimbGap()};

        getOnLadderOrVine(player, world, isSmall, false, tempH, tempF, tempHG, tempFG);

        if (tempH[0].isRelevant()) {
            handsClimbing = handsClimbing.max(tempH[0], handsGap, tempHG[0]);
        }
        if (tempF[0].isRelevant()) {
            feetClimbing = feetClimbing.max(tempF[0], feetGap, tempFG[0]);
        }

        // 대각 4방향(NE/NW/SE/SW) 탐색 — isSmall 시 생략 (C-30, SmartMovingSelf.md 745줄 조건)
        if (!isSmall) {
            int px = (int) Math.floor(player.getX());
            int py = (int) Math.floor(player.getY());
            int pz = (int) Math.floor(player.getZ());
            int[][] diags = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
            for (int[] d : diags) {
                for (int by = py; by <= py + 1; by++) {
                    BlockState state = world.getBlockState(new BlockPos(px + d[0], by, pz + d[1]));
                    boolean isHandsLevel = (by == py + 1);
                    if (state.getBlock() instanceof LadderBlock) {
                        Direction facing = state.get(LadderBlock.FACING);
                        boolean relevant = (d[0] > 0 && facing == Direction.WEST)
                                        || (d[0] < 0 && facing == Direction.EAST)
                                        || (d[1] > 0 && facing == Direction.NORTH)
                                        || (d[1] < 0 && facing == Direction.SOUTH);
                        if (relevant) {
                            ClimbGap gap = new ClimbGap(); gap.state = state;
                            if (isHandsLevel) handsClimbing = handsClimbing.max(HandsClimbing.UP, handsGap, gap);
                            else feetClimbing = feetClimbing.max(FeetClimbing.SLOW_UP_WITH_HOLD_WITHOUT_HANDS, feetGap, gap);
                        }
                    } else if (state.getBlock() instanceof VineBlock) {
                        boolean hasFace = (d[0] > 0 && Boolean.TRUE.equals(state.get(VineBlock.WEST)))
                                       || (d[0] < 0 && Boolean.TRUE.equals(state.get(VineBlock.EAST)))
                                       || (d[1] > 0 && Boolean.TRUE.equals(state.get(VineBlock.NORTH)))
                                       || (d[1] < 0 && Boolean.TRUE.equals(state.get(VineBlock.SOUTH)));
                        if (hasFace) {
                            ClimbGap gap = new ClimbGap(); gap.state = state;
                            if (isHandsLevel) handsClimbing = handsClimbing.max(HandsClimbing.UP, handsGap, gap);
                            else feetClimbing = feetClimbing.max(FeetClimbing.SLOW_UP_WITH_HOLD_WITHOUT_HANDS, feetGap, gap);
                        }
                    }
                }
            }
        }

        // 클라이밍 가능한 표면이 없으면 처리하지 않음
        if (!handsClimbing.isRelevant() && !feetClimbing.isRelevant()) {
            return;
        }

        // C-31: wantClimbUp / wantClimbDown 키 입력 기반 방향 제어
        // 원본: grabButton.Pressed + movementInput.moveForward 조합
        boolean wantClimb     = SmartMovingKeys.grab.isPressed();
        boolean wantClimbUp   = wantClimb && player.input.movementForward > 0F;
        boolean wantClimbDown = wantClimb && player.input.movementForward <= 0F && !sm.isCrawling;

        // grab 없이 사다리/넝쿨 위 → vanilla가 낙하 처리
        if (!wantClimbUp && !wantClimbDown) {
            return;
        }

        double value;
        boolean isUp;

        if (wantClimbUp) {
            if (handsClimbing == HandsClimbing.FAST_UP || feetClimbing == FeetClimbing.FAST_UP) {
                value = FAST_UP_MOTION; isUp = true;
            } else if (handsClimbing.isUp() && feetClimbing.isUp()) {
                value = MEDIUM_UP_MOTION; isUp = true;
            } else if (handsClimbing.isUp()) {
                value = SLOW_UP_MOTION; isUp = true;
            } else if (feetClimbing.isIndependentlyRelevant()) {
                value = SLOW_UP_MOTION; isUp = true;
            } else {
                value = HOLD_MOTION; isUp = true;
            }
        } else {
            // wantClimbDown: 내려가기
            if (feetClimbing == FeetClimbing.FAST_UP) {
                value = CLIMB_DOWN_MOTION; isUp = false;
            } else if (handsClimbing.isUp()) {
                value = CLIMB_DOWN_MOTION; isUp = false;
            } else {
                value = SINK_DOWN_MOTION; isUp = false;
            }
        }

        setOnlyShouldClimbSpeed(player, sm, value, isUp, 1.0D);

        // fallDistance 리셋 (클라이밍 중 낙하 데미지 방지)
        player.fallDistance = 0;

        // C-33: 클라이밍 중 exhaustion 증가 (피로 누적, 매 틱 1F 감소보다 큰 값)
        if (sm.isClimbing && cfg.climbExhaustion) {
            sm.exhaustion += 2.0F;
        }

        // C-32: climbBackJump — isClimbHolding 상태에서 jump 시 뒤로 점프
        handleClimbBackJump(player, sm);

        // C-32: handleCrash — 자유 클라이밍 낙하 데미지
        if (sm.isClimbing) handleCrash(player, cfg.freeClimbFallDamageStartDistance, cfg.freeClimbFallDamageFactor);
    }

    /**
     * 클라이밍 중 뒤로 점프 (climbBackJump).
     * 원본: SmartMovingSelf.handleClimbing() isClimbHolding && jumpButton.StartPressed 분기.
     *
     * jumpAngle = rotationYaw + 180F (정면 반대 방향).
     * cfg.climbJumpBackHead=false(기본): grab 미입력 → HEAD 점프, grab 입력 → 일반 뒤로 점프.
     * continueWallJumping = !isHeadJumping — 뒤로 점프 후 연속 벽 점프 허용.
     */
    private static void handleClimbBackJump(ClientPlayerEntity player, SmartMovingClientState sm) {
        if (!sm.isClimbHolding || !sm.jumpPending) return;

        SmartMovingConfig cfg = SmartMovingConfig.Config;
        boolean grabPressed = SmartMovingKeys.grab.isPressed();
        // climbJumpBackHead=false: !grab → headJump, grab → backUp
        boolean useHead = cfg.climbJumpBackHead ? grabPressed : !grabPressed;

        float jumpAngle = player.getYaw() + 180F;
        double jumpAngleRad = Math.toRadians(jumpAngle);

        // 수직 속도: 표준 점프 높이
        double verticalMotion = 0.41999998688697815D;

        // 수평 속도: 정면 반대 방향으로 0.3D push
        double motionX = -Math.sin(jumpAngleRad) * 0.3D;
        double motionZ =  Math.cos(jumpAngleRad) * 0.3D;

        player.setVelocity(motionX, verticalMotion, motionZ);
        player.setYaw(jumpAngle);
        player.bodyYaw = jumpAngle;

        if (useHead) {
            sm.isHeadJumping = true;
            sm.heightOffset  = -1F;
            SmartMovingJumper.setPoseSmall(player);
        }

        // 뒤로 점프 후 연속 벽 점프 허용 (원본: continueWallJumping = !isHeadJumping)
        sm.continueWallJumping = !sm.isHeadJumping;
        sm.isClimbing          = false;
        sm.blockJumpTillButtonRelease = true;
        sm.jumpPending         = false;
    }

    /**
     * 자유 클라이밍 낙하 데미지 처리.
     * 원본: SmartMovingSelf.handleCrash(startDistance, factor)
     * fallDistance > startDistance 시 초과분 * factor 데미지 적용.
     */
    private static void handleCrash(ClientPlayerEntity player, float startDistance, float factor) {
        if (player.fallDistance > startDistance) {
            float damage = (player.fallDistance - startDistance) * factor;
            player.damage(player.getDamageSources().fall(), damage);
        }
    }

    // ── 7-3: handleCeilingClimbing ───────────────────────────────────────

    /**
     * 원본: SmartMovingSelf.handleCeilingClimbing()
     *
     * 천장 클라이밍 메인 로직.
     * 조건: wantClimbCeiling && !isClimbing && (!isCrawling || conflict) && !isCrawlClimbing
     *
     * jgap(머리 위 갭 크기)에 따른 수평 속도:
     *   jgap > 1.2  → 0.12
     *   jgap > 1.115 → 0.08
     *   else         → 0.04
     * fallDistance = 0
     *
     */
    public static void handleCeilingClimbing(ClientPlayerEntity player, SmartMovingClientState sm) {
        SmartMovingConfig cfg = SmartMovingConfig.Config;

        if (!cfg.ceilingClimbing) return;

        // C-33: ceiling climb exhaustion 체크
        // 원본: exhaustion<=stop && (wasCeilingClimbing||exhaustion<=start)
        if (cfg.ceilingClimbExhaustion) {
            boolean allowed = sm.exhaustion <= cfg.ceilingClimbExhaustionStop
                    && (sm.isCeilingClimbing || sm.exhaustion <= cfg.ceilingClimbExhaustionStart);
            if (!allowed) return;
        }

        // C-34: wantClimbCeiling 조건 — grab 키 + !isCrawling + !isSneaking
        // 원본: grabButton.Pressed && !wantCrawlNotClimb && !isSneaking() && !disabled
        boolean wantClimbCeiling = SmartMovingKeys.grab.isPressed()
                && !sm.isCrawling && !player.isSneaking();
        if (!wantClimbCeiling) return;

        // 조건: !isClimbing && (!isCrawling || conflict) && !isCrawlClimbing
        if (sm.isClimbing || sm.isCrawlClimbing) return;

        // C-35: jgap = 플레이어 머리(bb.maxY)에서 천장 블록까지의 거리
        // 원본: ceil(bb.maxY) 이상에서 첫 번째 솔리드 블록 Y - bb.maxY
        double jgap = computeJgap(player);

        double horizontalSpeed;
        if (jgap > 1.2) {
            horizontalSpeed = 0.12D;
        } else if (jgap > 1.115) {
            horizontalSpeed = 0.08D;
        } else {
            horizontalSpeed = 0.04D;
        }

        // C-36: 이동 방향 기반 수평 벡터 분해 (원본: moveFlying(strafe, forward, speed))
        // movementForward / movementSideways 기반으로 yaw 방향 수평 속도 계산
        float forward = player.input.movementForward;
        float strafe  = player.input.movementSideways;
        float distSq  = forward * forward + strafe * strafe;
        if (distSq > 0.0001F) {
            float dist   = (float) Math.sqrt(distSq);
            float ratio  = (float) (horizontalSpeed / Math.max(dist, 1F));
            strafe  *= ratio;
            forward *= ratio;
            double yawRad = Math.toRadians(player.getYaw());
            double cos = Math.cos(yawRad);
            double sin = Math.sin(yawRad);
            Vec3d vel = player.getVelocity();
            double motionX = strafe * cos - forward * sin;
            double motionZ = forward * cos + strafe * sin;
            player.setVelocity(motionX, vel.y, motionZ);
        }

        // fallDistance = 0 (필수 — 낙하 데미지 방지)
        player.fallDistance = 0;

        sm.isCeilingClimbing = true;
    }

    /**
     * 천장 블록까지의 갭 계산 (C-35).
     * 원본: sp.boundingBox.maxY 기준, 위쪽으로 첫 솔리드 블록 Y - bb.maxY.
     * jgap이 클수록 방이 높음 → 천장 클라이밍 속도 증가.
     */
    private static double computeJgap(ClientPlayerEntity player) {
        World world = player.getWorld();
        Box bb = player.getBoundingBox();
        int px = (int) Math.floor(player.getX());
        int pz = (int) Math.floor(player.getZ());
        // ceil(bb.maxY): 머리 위 첫 블록 경계부터 스캔
        int startY = (int) Math.ceil(bb.maxY);

        for (int by = startY; by <= startY + 4; by++) {
            BlockPos pos = new BlockPos(px, by, pz);
            BlockState state = world.getBlockState(pos);
            if (!state.getCollisionShape(world, pos).isEmpty()) {
                return by - bb.maxY;
            }
        }
        return 2.0D; // 4블록 이내에 천장 없음 → 넉넉한 공간
    }
}
