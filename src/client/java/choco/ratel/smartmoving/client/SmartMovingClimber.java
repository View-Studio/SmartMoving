package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.climbing.ClimbGap;
import choco.ratel.smartmoving.climbing.FeetClimbing;
import choco.ratel.smartmoving.climbing.HandsClimbing;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.VineBlock;
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
     * TODO: _freeClimbingUpSpeedFactor / _freeClimbingDownSpeedFactor 기본값 미확인.
     *       현재 1.0D로 고정 — SmartMovingConfig.md 재확인 필요.
     *
     * @return true if relevant (속도를 변경함)
     */
    public static boolean setShouldClimbSpeed(ClientPlayerEntity player, SmartMovingClientState sm,
                                               double value, boolean isUp, double combinedFactor) {
        // climbIntoCount > 0 이면 HOLD_MOTION 강제 (crawl gap 진입 중)
        // TODO: sm.climbIntoCount 미구현 — Phase 9 크롤-클라이밍에서 구현
        // if (sm.climbIntoCount > 0) { value = HOLD_MOTION; isUp = true; }

        double motionY = player.getVelocity().y;
        boolean relevant = value < 0 || value > motionY;

        if (relevant) {
            double newMotionY;
            if (isUp) {
                // 상방 보간
                // TODO: _freeClimbingUpSpeedFactor 기본값 미확인 — 1.0D로 임시 설정
                double upFactor = 1.0D;
                newMotionY = (value - HOLD_MOTION) * upFactor * combinedFactor + HOLD_MOTION;
            } else {
                // 하방 보간
                // TODO: _freeClimbingDownSpeedFactor 기본값 미확인 — 1.0D로 임시 설정
                double downFactor = 1.0D;
                newMotionY = HOLD_MOTION - (HOLD_MOTION - value) * downFactor * combinedFactor;
            }

            // hasClimbCrawlGap && isClimbCrawling && value > HOLD_MOTION → CATCH_CRAWL_GAP_MOTION으로 상한
            // TODO: sm.hasClimbCrawlGap, sm.isClimbCrawling 미구현 — Phase 9에서 구현

            player.setVelocity(player.getVelocity().x, newMotionY, player.getVelocity().z);
        }

        // isClimbJumping = !relevant && !isClimbHolding
        // TODO: sm.isClimbHolding, sm.isClimbJumping 미구현

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
     * 1. exhaustion 체크 (TODO: SM 독자 exhaustion 미구현)
     * 2. 8방향 탐색 (현재 4방향 구현, 대각 4방향 TODO)
     * 3. HandsClimbing/FeetClimbing 상태 집계
     * 4. setShouldClimbSpeed로 Y 속도 결정
     * 5. fallDistance = 0 (낙하 데미지 방지)
     *
     * TODO: wantClimbUp / wantClimbDown (키 입력 기반) 미구현 — Phase에서 구현
     * TODO: 대각 4방향 탐색 미구현
     * TODO: Standard/Simple/Smart 모드 미구현
     * TODO: climbBackJump, wallJump 미구현
     * TODO: SM exhaustion 체크 미구현
     */
    public static void handleClimbing(ClientPlayerEntity player, SmartMovingClientState sm) {
        SmartMovingConfig cfg = SmartMovingConfig.INSTANCE;

        // exhaustion 체크
        // TODO: SM 독자 exhaustion 필드 미구현 — 항상 허용으로 처리
        // if (cfg.climbExhaustion) {
        //     if (!(sm.exhaustion <= cfg.climbExhaustionStop
        //           && (sm.wasClimbing || sm.exhaustion <= cfg.climbExhaustionStart))) return;
        // }

        if (!cfg.freeClimb) {
            // Standard 모드: 기본 속도 적용
            // TODO: getCombinedSpeedFactor() 미구현
            // setOnlyShouldClimbSpeed(player, sm, FAST_UP_MOTION * combinedFactor, true, 1.0D);
            return;
        }

        World world = player.getWorld();
        boolean isSmall = sm.isSmall || sm.isCrawling;

        // 8방향 탐색 결과 집계
        HandsClimbing handsClimbing = HandsClimbing.NONE;
        FeetClimbing  feetClimbing  = FeetClimbing.NONE;
        ClimbGap[] handsGap = {new ClimbGap()};
        ClimbGap[] feetGap  = {new ClimbGap()};

        // 4방향(North/South/East/West) 탐색
        // TODO: 대각 4방향(NE,NW,SE,SW) 탐색 추가 필요
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

        // 클라이밍 가능한 표면이 없으면 처리하지 않음
        if (!handsClimbing.isRelevant() && !feetClimbing.isRelevant()) {
            return;
        }

        // 속도 결정
        // TODO: wantClimbUp / wantClimbDown 키 입력 미구현 → 기본적으로 상승
        double value;
        boolean isUp;

        if (handsClimbing == HandsClimbing.FAST_UP || feetClimbing == FeetClimbing.FAST_UP) {
            value = FAST_UP_MOTION;
            isUp = true;
        } else if (handsClimbing.isUp() && feetClimbing.isUp()) {
            // 손+발 모두 상승 가능: 중간 속도
            value = MEDIUM_UP_MOTION;
            isUp = true;
        } else if (handsClimbing.isUp()) {
            value = SLOW_UP_MOTION;
            isUp = true;
        } else if (feetClimbing.isIndependentlyRelevant()) {
            value = SLOW_UP_MOTION;
            isUp = true;
        } else {
            // 제자리 유지
            value = HOLD_MOTION;
            isUp = true;
        }

        setOnlyShouldClimbSpeed(player, sm, value, isUp, 1.0D);

        // fallDistance 리셋 (클라이밍 중 낙하 데미지 방지)
        player.fallDistance = 0;

        // TODO: climbBackJump 처리
        // TODO: wallJump 처리
        // TODO: handleCrash() 호출
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
     * TODO: wantClimbCeiling (grabButton.Pressed 기반) 미구현 — Phase 10에서 구현
     * TODO: jgap 계산 (AABB 충돌 쿼리) 미구현
     * TODO: 수평 속도 방향 벡터 적용 미구현
     */
    public static void handleCeilingClimbing(ClientPlayerEntity player, SmartMovingClientState sm) {
        SmartMovingConfig cfg = SmartMovingConfig.INSTANCE;

        if (!cfg.ceilingClimbing) return;

        // exhaustion 체크
        // TODO: SM 독자 exhaustion (_ceilingClimbExhaustionStart/Stop) 미구현

        // wantClimbCeiling 조건: grabButton.Pressed && !wantCrawlNotClimb && !isSneaking() && !disabled
        // TODO: grabButton (SM 전용 키) 미구현 — Phase 10에서 구현
        // if (!sm.wantClimbCeiling) return;

        // 조건: !isClimbing && (!isCrawling || conflict) && !isCrawlClimbing
        if (sm.isClimbing || sm.isCrawlClimbing) return;

        // jgap 계산 (머리 위 공간 크기)
        // TODO: AABB 충돌 쿼리로 정확한 jgap 계산 필요
        // 미확인 — 현재 stub으로 처리
        double jgap = computeJgap(player);

        double horizontalSpeed;
        if (jgap > 1.2) {
            horizontalSpeed = 0.12D;
        } else if (jgap > 1.115) {
            horizontalSpeed = 0.08D;
        } else {
            horizontalSpeed = 0.04D;
        }

        // 수평 속도 적용
        // TODO: 이동 방향 기반으로 수평 벡터 분해 필요 — 미구현
        Vec3d vel = player.getVelocity();
        // player.setVelocity(horizontalVecX * horizontalSpeed, vel.y, horizontalVecZ * horizontalSpeed);

        // fallDistance = 0 (필수 — 낙하 데미지 방지)
        player.fallDistance = 0;

        sm.isCeilingClimbing = true;
    }

    /**
     * 머리 위 갭(공간) 크기 계산.
     * 원본: boundingBox 임시 확장 후 AABB 충돌 쿼리로 천장까지 거리 계산.
     *
     * TODO: 정확한 AABB 충돌 쿼리 구현 필요 — 미확인
     *       1.21.1에서 World.getBlockCollisions() 또는 World.canPlace() 계열 메서드 사용 검토
     */
    private static double computeJgap(ClientPlayerEntity player) {
        // TODO: 정확한 jgap 계산 미구현
        // 임시: 1.0을 반환하여 수평 속도 0.04 분기로 처리
        return 1.0D;
    }
}
