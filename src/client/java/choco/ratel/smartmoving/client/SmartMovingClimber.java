package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.climbing.ClimbGap;
import choco.ratel.smartmoving.climbing.FeetClimbing;
import choco.ratel.smartmoving.climbing.HandsClimbing;
import choco.ratel.smartmoving.climbing.Orientation;
import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import choco.ratel.smartmoving.climbing.CeilingClimbBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.util.math.Box;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
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

    // ── B-16a: isFacedToLadder / isFacedToSolidVine (세션 69) ─────────────

    /**
     * 원본 SmartMovingBase L184-L186 `isFacedToLadder(boolean isSmall)`:
     *   return getOnLadder(1, true, isSmall) > 0;
     *   where getOnLadder(maxResult, faceOnly=true, isSmall) =
     *     getOnLadderOrVine(maxResult, faceOnly=true, onlyLadder=true, onlyVine=false, isSmall)
     *
     * 1.21.1 `getOnLadderOrVine` 은 ladder/vine 필터 파라미터가 없어 둘 다 감지하나
     * `out_handsVine[0]` / `out_feetVine[0]` 플래그로 vine 여부 구분 가능 → **ladder 만 선택**
     * (vine 제외) 하는 근사 이식. `faceOnly=true` 로 플레이어 정면 4방향 중 바라보는 방향만 탐색.
     *
     * 사용처: `wouldWantClimb` 4-OR 의 3번째 분기 (원본 L2471) — 자동 사다리 진입.
     * B-16a (세션 69).
     */
    public static boolean isFacedToLadder(ClientPlayerEntity player, boolean isSmall) {
        World world = player.getWorld();
        HandsClimbing[] h = {HandsClimbing.NONE};
        FeetClimbing[]  f = {FeetClimbing.NONE};
        ClimbGap[] hg = {new ClimbGap()};
        ClimbGap[] fg = {new ClimbGap()};
        boolean[]  hv = {false};
        boolean[]  fv = {false};
        getOnLadderOrVine(player, world, isSmall, true, h, f, hg, fg, hv, fv);
        // 근사: ladder 전용 필터 불가능 → relevant 이면서 vine 아닌 경우만 ladder.
        return (h[0].isRelevant() && !hv[0]) || (f[0].isRelevant() && !fv[0]);
    }

    /**
     * 원본 SmartMovingBase L189-L192 `isFacedToSolidVine(boolean isSmall)`:
     *   return getOnVine(1, true, isSmall) > 0;
     *   where getOnVine(maxResult, faceOnly=true, isSmall) =
     *     getOnLadderOrVine(maxResult, faceOnly=true, onlyLadder=false, onlyVine=true, isSmall)
     *
     * 1.21.1 `getOnLadderOrVine` L143-L144 에서 이미 "solid 블록 뒤 vine" 만 검출 → solidVine
     * 조건 내장. `out_handsVine/out_feetVine=true` = solid vine 감지됨.
     *
     * 사용처: `wouldWantClimb` 4-OR 의 4번째 분기 (원본 L2472) — 자동 솔리드 덩굴 진입.
     * B-16a (세션 69).
     */
    public static boolean isFacedToSolidVine(ClientPlayerEntity player, boolean isSmall) {
        World world = player.getWorld();
        HandsClimbing[] h = {HandsClimbing.NONE};
        FeetClimbing[]  f = {FeetClimbing.NONE};
        ClimbGap[] hg = {new ClimbGap()};
        ClimbGap[] fg = {new ClimbGap()};
        boolean[]  hv = {false};
        boolean[]  fv = {false};
        getOnLadderOrVine(player, world, isSmall, true, h, f, hg, fg, hv, fv);
        return (h[0].isRelevant() && hv[0]) || (f[0].isRelevant() && fv[0]);
    }

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
     *   LadderBlock.FACING.getOpposite() == dir → 탐색 방향(dir)에서 붙은 사다리인지 확인
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
            ClimbGap[] out_handsGap, ClimbGap[] out_feetGap,
            boolean[] out_handsVine, boolean[] out_feetVine) {

        out_hands[0] = HandsClimbing.NONE;
        out_feet[0]  = FeetClimbing.NONE;
        out_handsGap[0].reset();
        out_feetGap[0].reset();
        out_handsVine[0] = false;
        out_feetVine[0]  = false;

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
                    // 탐색 방향(dir)에서 접근할 수 있는 사다리인지 확인
                    // 예: dir=EAST 탐색 → 사다리 FACING=WEST(동쪽 벽에 붙음) → getOpposite()=EAST == dir
                    if (ladderFacing.getOpposite() == dir) {
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
                                out_handsVine[0] = true;
                            } else {
                                out_feet[0] = out_feet[0].max(FeetClimbing.SLOW_UP_WITH_HOLD_WITHOUT_HANDS, out_feetGap, gap);
                                out_feetVine[0] = true;
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
    /**
     * 🔴 (2026-04-27) 원본 SmartMovingSelf L1500-1551 1:1 재구성:
     *   원본 호출 흐름: setShouldClimbSpeed → setOnlyShouldClimbSpeed → `isClimbing=true` 무조건.
     *   이전 매핑은 `isClimbing=true` 를 relevant 시만 set → motionY 가 이미 충분 시
     *   isClimbing 안 set → travel inject 가 vanilla 진행 → 사다리 댐핑 X → 떨어짐 →
     *   다음 tick 다시 발동 → on/off 진동 = 끊김 (사용자 보고 BUG-1).
     */
    public static boolean setShouldClimbSpeed(ClientPlayerEntity player, SmartMovingClientState sm,
                                               double value, boolean isUp, double combinedFactor) {
        // 원본 L1502: setShouldClimbSpeed(value) → setShouldClimbSpeed(value, UpGrab, DownStep).
        // 우리는 hands/feet type 별도 처리 안 함 (R-01 패킷 인코딩만 영향) — setOnly 직접 호출.
        return setOnlyShouldClimbSpeed(player, sm, value, isUp, combinedFactor);
    }

    /**
     * 원본: SmartMovingSelf.setOnlyShouldClimbSpeed L1513-1551.
     *   `isClimbing = true` 진입 시 무조건. relevant 일 때만 motionY 적용.
     */
    public static boolean setOnlyShouldClimbSpeed(ClientPlayerEntity player, SmartMovingClientState sm,
                                                   double value, boolean isUp, double combinedFactor) {
        // 원본 L1515: isClimbing=true 무조건 (climbing 발동 의도가 있는 어떤 호출이든).
        sm.isClimbing = true;

        // 원본 L1517-1518: climbIntoCount>0 이면 HoldMotion 으로 강제.
        if (sm.climbIntoCount > 0) {
            value = HOLD_MOTION;
            isUp = true;
        }

        double motionY = player.getVelocity().y;

        // 원본 L1520-1543: value != HoldMotion 시 factor 보간. HoldMotion 인 경우는 isClimbingStill.
        if (value != HOLD_MOTION) {
            // 원본 L1541-1542: hasClimbCrawlGap && isClimbCrawling 시 catchCrawlGap 상한.
            if (sm.hasClimbCrawlGap && sm.isClimbCrawling && value > HOLD_MOTION) {
                value = Math.min(CATCH_CRAWL_GAP_MOTION, value);
            }
            SmartMovingConfig cfg2 = SmartMovingConfig.Config;
            if (isUp) {
                value = (value - HOLD_MOTION) * cfg2.freeClimbingUpSpeedFactor * combinedFactor + HOLD_MOTION;
            } else {
                value = HOLD_MOTION - (HOLD_MOTION - value) * cfg2.freeClimbingDownSpeedFactor * combinedFactor;
            }
        } else {
            sm.isClimbingStill = true;
        }

        // 원본 L1547-1550: relevant = value<0 || value>motionY. relevant 시만 motionY 적용.
        boolean relevant = value < 0 || value > motionY;
        if (relevant) {
            player.setVelocity(player.getVelocity().x, value, player.getVelocity().z);
        }
        sm.isClimbJumping = !relevant && !sm.isClimbHolding;

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

        // B-14 (세션 57): 원본 L816 `resetClimbing()` 매 틱 리셋 이식.
        // Standard/Simple/Smart/Free Base Climb 공통 — handleClimbing 진입 직후 무조건
        // 10 등반 필드 false/0 리셋. 이후 분기에서 실제 값 재계산.
        // B-21 (isCeilingClimbing 해제 엣지) 자동 해소.
        sm.resetClimbing();

        // C-33: SM 독자 exhaustion 체크
        // 원본: exhaustionAllowsClimbing = !enabled || (exhaustion<=stop && (wasClimbing||exhaustion<=start))
        if (cfg.climbExhaustion) {
            boolean allowed = sm.exhaustion <= cfg.climbExhaustionStop
                    && (sm.wasClimbing || sm.exhaustion <= cfg.climbExhaustionStart);
            if (!allowed) return;
        }

        if (!cfg.freeClimb && !cfg.simpleClimb && !cfg.smartClimb) {
            // B-20 (세션 76) → **B-42-B20 해소 (세션 125)**: 원본 L820-L823 Standard Base
            //   Climb 정밀 복원. 원본: if (isOnLadderOrVine && isCollidedHorizontally)
            //     motionY = 0.2 * combinedFactor
            //   `isOnLadderOrVine` 조건은 1.21.1 `handleClimbing` 호출 상위 (MixinLivingEntity
            //   L153 `onClimbable`) 에서 이미 내포 → 남은 조건 `player.horizontalCollision` 복원.
            //   `setShouldClimbSpeed` 사용 (isClimbing 안 건드림) 은 세션 76 정정 유지.
            // B-1 (세션 25): Mover.getCombinedSpeedFactor User 배율 주입.
            if (player.horizontalCollision) {
                double combinedFactor = SmartMovingMover.getCombinedSpeedFactor(player, cfg);
                setShouldClimbSpeed(player, sm, FAST_UP_MOTION * combinedFactor, true, 1.0D);
                player.fallDistance = 0;
            }
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

        boolean[] _hv = {false}, _fv = {false};
        getOnLadderOrVine(player, world, isSmall, false, tempH, tempF, tempHG, tempFG, _hv, _fv);

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
                        // 원본 Orientation.hasVineOrientation(): 탐색 방향 == vine face 방향
                        // PZ(동쪽) → East, NZ(서쪽) → West, ZP(남쪽) → South, ZN(북쪽) → North
                        boolean hasFace = (d[0] > 0 && Boolean.TRUE.equals(state.get(VineBlock.EAST)))
                                       || (d[0] < 0 && Boolean.TRUE.equals(state.get(VineBlock.WEST)))
                                       || (d[1] > 0 && Boolean.TRUE.equals(state.get(VineBlock.SOUTH)))
                                       || (d[1] < 0 && Boolean.TRUE.equals(state.get(VineBlock.NORTH)));
                        // 🔴 사다리/덩굴 1-1: 4방향 탐색과 동일하게 solid 뒤 블록 검증 추가
                        //   (원본 SmartMovingBase L284: isRemoteSolid 체크). vine 이 매달릴 solid 벽 필수.
                        if (hasFace) {
                            BlockPos solidPos = new BlockPos(px + d[0] * 2, by, pz + d[1] * 2);
                            BlockState solidState = world.getBlockState(solidPos);
                            if (solidState.isSolidBlock(world, solidPos)) {
                                ClimbGap gap = new ClimbGap(); gap.state = state;
                                if (isHandsLevel) {
                                    handsClimbing = handsClimbing.max(HandsClimbing.UP, handsGap, gap);
                                    // 🔴 사다리/덩굴 1-1 BUG: 대각 vine flag 갱신 누락 → 추가.
                                    //   isHandsVineClimbing 가 4방향 탐색만 갱신되어 대각 vine 잡힘 시
                                    //   isFacedToSolidVine / isVineAnyClimbing 오판정 → vine jump 등반 등 실패.
                                    sm.isHandsVineClimbing = true;
                                } else {
                                    feetClimbing = feetClimbing.max(FeetClimbing.SLOW_UP_WITH_HOLD_WITHOUT_HANDS, feetGap, gap);
                                    sm.isFeetVineClimbing = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        // B-19a4 (세션 108): Orientation.seekClimbGap 기반 8방향 탐색 → ClientState 필드 5 대입.
        // 원본 SmartMovingSelf L926-L961 (Free Climb preferClimb 블록) 이식.
        // **B-19 도미노 해소 완결** — B-17 (isCrawlClimbing) / B-18 (isClimbCrawling) /
        // B-16c (wouldWantClimb 4-OR) 공식이 실제 값 기반 평가 활성화.
        // 기존 getOnLadderOrVine 결과 변수 (handsClimbing/feetClimbing) 는 속도 결정에
        // 계속 사용됨 — 이 블록은 필드 대입만 추가 (동작 영향 최소).
        {
            double id = player.getX();
            double jd = player.getBoundingBox().minY;
            double kd = player.getZ();
            int ix = (int) Math.floor(id);
            int iz = (int) Math.floor(kd);

            // 원본 L922-L925: rotation 정규화 (0 ~ 360F)
            float rotation = player.getYaw() % 360F;
            if (rotation < 0) rotation += 360F;

            // 원본 L918: isSmallClimbing = isCrawling || isSliding
            boolean isSmallClimbing = sm.isCrawling || sm.isSliding;

            // 원본 L919-L920: isClimbCrawling || isCrawlClimbing || isSmallClimbing → jd += -1D
            // initializeOffset 에 전달할 jhd = jd * 2D + 1 (원본 L926).
            // jd 가 -1 이면 jhd = (jd - 1) * 2 + 1 = jh - 2
            double jh = jd * 2D + 1;
            if (sm.isClimbCrawling || sm.isCrawlClimbing || isSmallClimbing) {
                jh += -2D;
            }

            // 🔴 (2026-04-27) 원본 SmartMovingSelf L928-961 정밀 1:1 매핑:
            //   원본은 main handsClimbing/feetClimbing 와 inout 변수가 동일 (별도 NONE 시작 후
            //   inout[0] 에 대입). 우리는 inout 만 NONE 시작 → 8방향 결과 main 에 누적 안 됨
            //   → setShouldClimbSpeed 결정 시 일반 벽 인식 못 함 → 사용자 보고 "grab 키 정지".
            //   해결: inout 시작값을 main (getOnLadderOrVine 결과) 으로 + 8방향 후 main 갱신.
            HandsClimbing[] inoutH = { handsClimbing };
            FeetClimbing[]  inoutF = { feetClimbing };
            ClimbGap outHandsGap = handsGap[0];
            ClimbGap outFeetGap  = feetGap[0];

            // 원본 L937-L940: 4방향 (PZ/NZ/ZP/ZN) seekClimbGap 호출
            Orientation.PZ.seekClimbGap(rotation, world, ix, id, jh, iz, kd,
                    sm.isClimbCrawling, sm.isCrawlClimbing, isSmallClimbing,
                    inoutH, inoutF, outHandsGap, outFeetGap);
            Orientation.NZ.seekClimbGap(rotation, world, ix, id, jh, iz, kd,
                    sm.isClimbCrawling, sm.isCrawlClimbing, isSmallClimbing,
                    inoutH, inoutF, outHandsGap, outFeetGap);
            Orientation.ZP.seekClimbGap(rotation, world, ix, id, jh, iz, kd,
                    sm.isClimbCrawling, sm.isCrawlClimbing, isSmallClimbing,
                    inoutH, inoutF, outHandsGap, outFeetGap);
            Orientation.ZN.seekClimbGap(rotation, world, ix, id, jh, iz, kd,
                    sm.isClimbCrawling, sm.isCrawlClimbing, isSmallClimbing,
                    inoutH, inoutF, outHandsGap, outFeetGap);

            // 원본 L942-L943: 4방향 후 main 변수로 다시 가져오기
            handsClimbing = inoutH[0];
            feetClimbing  = inoutF[0];
            handsGap[0]   = outHandsGap;
            feetGap[0]    = outFeetGap;

            // 원본 L945-L947: 4방향 결과 → ClientState 필드 3 대입
            sm.isNeighborClimbing = handsClimbing.isRelevant() || feetClimbing.isRelevant();
            sm.hasNeighborClimbGap = outHandsGap.canStand || outFeetGap.canStand;
            sm.hasNeighborClimbCrawlGap = outHandsGap.mustCrawl || outFeetGap.mustCrawl;

            // 원본 L949-L955: isSmallClimbing 아닐 때 대각 4방향 (PP/NP/NN/PN) 추가 탐색
            if (!isSmallClimbing) {
                Orientation.PP.seekClimbGap(rotation, world, ix, id, jh, iz, kd,
                        sm.isClimbCrawling, sm.isCrawlClimbing, isSmallClimbing,
                        inoutH, inoutF, outHandsGap, outFeetGap);
                Orientation.NP.seekClimbGap(rotation, world, ix, id, jh, iz, kd,
                        sm.isClimbCrawling, sm.isCrawlClimbing, isSmallClimbing,
                        inoutH, inoutF, outHandsGap, outFeetGap);
                Orientation.NN.seekClimbGap(rotation, world, ix, id, jh, iz, kd,
                        sm.isClimbCrawling, sm.isCrawlClimbing, isSmallClimbing,
                        inoutH, inoutF, outHandsGap, outFeetGap);
                Orientation.PN.seekClimbGap(rotation, world, ix, id, jh, iz, kd,
                        sm.isClimbCrawling, sm.isCrawlClimbing, isSmallClimbing,
                        inoutH, inoutF, outHandsGap, outFeetGap);
            }

            // 원본 L957-L958: 8방향 후 main 변수로 다시 가져오기
            handsClimbing = inoutH[0];
            feetClimbing  = inoutF[0];
            handsGap[0]   = outHandsGap;
            feetGap[0]    = outFeetGap;

            // 원본 L960-L961: 8방향 합산 결과 → ClientState 필드 2 대입
            sm.hasClimbGap = outHandsGap.canStand || outFeetGap.canStand;
            sm.hasClimbCrawlGap = outHandsGap.mustCrawl || outFeetGap.mustCrawl;
        }

        // **포커스 #3 B-7 (세션 6)**: 원본 L963-L976 누락 2 분기 이식 (★ #3 감사 발견).
        // 원본 SmartMovingSelf L963-L976 — 8방향 탐색 후 클라이밍 상태 보정.
        {
            int ix2 = (int) Math.floor(player.getX());
            int jd2 = (int) Math.floor(player.getBoundingBox().minY);
            int iz2 = (int) Math.floor(player.getZ());

            // 원본 L963-L970: BottomHold + 위 2 블록 ladder + 옆 2 블록 비고체 → handsClimbing 해제
            //   if (handsClimbing == BottomHold && Orientation.isLadder(world.getBlock(i, j+2, k))) {
            //     ladderOrientation = getKnownLadderOrientation(...);
            //     remote_i = i + ladderOrientation._i; remote_k = k + ladderOrientation._k;
            //     if (!world.getBlock(remote_i, j, remote_k).isSolid()
            //         && !world.getBlock(remote_i, j+1, remote_k).isSolid())
            //         handsClimbing = None;
            //   }
            // RedPower 와이어 / 복층 사다리 특수 케이스. 1.21.1 isSolid 매핑 = state.isSolidBlock().
            if (handsClimbing == HandsClimbing.BOTTOM_HOLD
                    && Orientation.isLadder(world.getBlockState(new BlockPos(ix2, jd2 + 2, iz2)))) {
                Orientation ladderOrientation = Orientation.getKnownLadderOrientation(
                        world, ix2, jd2 + 2, iz2);
                if (ladderOrientation != null) {
                    int remoteI = ix2 + ladderOrientation.getOffsetI();
                    int remoteK = iz2 + ladderOrientation.getOffsetK();
                    BlockPos pos0 = new BlockPos(remoteI, jd2, remoteK);
                    BlockPos pos1 = new BlockPos(remoteI, jd2 + 1, remoteK);
                    BlockState s0 = world.getBlockState(pos0);
                    BlockState s1 = world.getBlockState(pos1);
                    if (!s0.isSolidBlock(world, pos0) && !s1.isSolidBlock(world, pos1)) {
                        handsClimbing = HandsClimbing.NONE;
                    }
                }
            }

            // 원본 L972-L976: !grabPressed && handsClimbing==Up && feetClimbing==None →
            //   주변 공기 + !horizontalCollision 시 handsClimbing 해제 (스티키 클라이밍 방지).
            if (!SmartMovingKeys.grab.isPressed()
                    && handsClimbing == HandsClimbing.UP
                    && feetClimbing == FeetClimbing.NONE) {
                if (!player.horizontalCollision
                        && world.isAir(new BlockPos(ix2, jd2, iz2))
                        && world.isAir(new BlockPos(ix2, jd2 + 1, iz2))) {
                    handsClimbing = HandsClimbing.NONE;
                }
            }
        }

        // 클라이밍 가능한 표면이 없으면 처리하지 않음
        if (!handsClimbing.isRelevant() && !feetClimbing.isRelevant()) {
            return;
        }

        // B-1 (세션 25): Mover.getCombinedSpeedFactor 단일 헬퍼로 교체.
        double combinedFactor = SmartMovingMover.getCombinedSpeedFactor(player, cfg);

        // **B-20b 해소 (세션 132)**: Simple Base Climb 원본 L825-L844 전면 정밀 재작성.
        //   원본: if (isSimpleBaseClimb && isCollidedHorizontally && isOnLadderOrVine) {
        //     feet = isClimbable(i, j, k); hands = isClimbable(i, j+1, k);
        //     feet&&hands → Fast / feet → Fast / hands → Slow / else → 0
        //     motionY *= combinedFactor
        //   }
        //   isOnLadderOrVine 은 상위 `onClimbable` 에 내포 (B-42-B20 확인). 남은 조건
        //   `isCollidedHorizontally` 복원 + feet/hands isClimbable 정밀 판정.
        if (cfg.simpleClimb) {
            if (player.horizontalCollision) {
                int i = (int) Math.floor(player.getX());
                int j = (int) Math.floor(player.getBoundingBox().minY);
                int k = (int) Math.floor(player.getZ());
                boolean feet  = Orientation.isClimbable(world, i, j, k);
                boolean hands = Orientation.isClimbable(world, i, j + 1, k);
                double value;
                if (feet && hands)      value = FAST_UP_MOTION;
                else if (feet)          value = FAST_UP_MOTION;
                else if (hands)         value = SLOW_UP_MOTION;
                else                    value = 0.0D;
                value *= combinedFactor;
                setOnlyShouldClimbSpeed(player, sm, value, true, 1.0D);
                player.fallDistance = 0;
            }
            return;
        }

        // **B-20c 해소 (세션 132)**: Smart Base Climb 원본 L856-L894 전면 정밀 재작성.
        //   원본: if (isSmartBaseClimb && isOnLadderOrVine && isCollidedHorizontally) {
        //     feet = isClimbable(i, j, k); hands = isClimbable(i, j+1, k);
        //     feet&&hands → Fast
        //     feet only → handsSubstitute (PZ/NZ/ZP/ZN at j+1) ? Fast : Slow
        //     hands only → feetSubstitute (ZZ/PZ/NZ/ZP/ZN at j) ? Fast : Slow
        //     else → 0
        //     motionY *= combinedFactor
        //   }
        //   handsSubstitute 는 원본 L866-L869: PZ/NZ/ZP/ZN 4방향 (ZZ 없음).
        //   feetSubstitute 는 원본 L879-L883: ZZ/PZ/NZ/ZP/ZN 5방향.
        if (cfg.smartClimb) {
            if (player.horizontalCollision) {
                int i = (int) Math.floor(player.getX());
                int j = (int) Math.floor(player.getBoundingBox().minY);
                int k = (int) Math.floor(player.getZ());
                boolean feet  = Orientation.isClimbable(world, i, j, k);
                boolean hands = Orientation.isClimbable(world, i, j + 1, k);
                double value;
                if (feet && hands) {
                    value = FAST_UP_MOTION;
                } else if (feet) {
                    // 원본 L866-L869: 4방향 (PZ/NZ/ZP/ZN) at j+1
                    boolean handsSubstitute =
                            Orientation.PZ.isHandsLadderSubstitute(world, i, j + 1, k)
                         || Orientation.NZ.isHandsLadderSubstitute(world, i, j + 1, k)
                         || Orientation.ZP.isHandsLadderSubstitute(world, i, j + 1, k)
                         || Orientation.ZN.isHandsLadderSubstitute(world, i, j + 1, k);
                    value = handsSubstitute ? FAST_UP_MOTION : SLOW_UP_MOTION;
                } else if (hands) {
                    // 원본 L879-L883: 5방향 (ZZ/PZ/NZ/ZP/ZN) at j
                    boolean feetSubstitute =
                            Orientation.ZZ.isFeetLadderSubstitute(world, i, j, k)
                         || Orientation.PZ.isFeetLadderSubstitute(world, i, j, k)
                         || Orientation.NZ.isFeetLadderSubstitute(world, i, j, k)
                         || Orientation.ZP.isFeetLadderSubstitute(world, i, j, k)
                         || Orientation.ZN.isFeetLadderSubstitute(world, i, j, k);
                    value = feetSubstitute ? FAST_UP_MOTION : SLOW_UP_MOTION;
                } else {
                    value = 0.0D;
                }
                value *= combinedFactor;
                setOnlyShouldClimbSpeed(player, sm, value, true, 1.0D);
                player.fallDistance = 0;
            }
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
            // 🔴 (2026-04-27) 원본 SmartMovingSelf L981-1027 1:1 정밀 매핑.

            // 원본 L983-987: 슬라이딩 중에 grab+전진 → 크롤 전환.
            if (sm.isSliding && handsClimbing.isRelevant()) {
                sm.isSliding  = false;
                sm.isCrawling = true;
            }

            // 원본 L989: handsClimbing = handsClimbing.ToUp(); (BottomHold → Up 전환)
            handsClimbing = handsClimbing.toUp();

            // 원본 L991-995: feetClimbing.FastUp + 특수 조건 → fast climb.
            //   원본 조건: !(handsClimbing == None && onGround && feetClimbGap.Block != bed)
            //   1.21.1 매핑: bed 검사 생략 (근사).
            boolean handsNoneOnGround = (handsClimbing == HandsClimbing.NONE) && player.isOnGround();
            if (feetClimbing == FeetClimbing.FAST_UP && !handsNoneOnGround) {
                value = FAST_UP_MOTION; isUp = true;
            }
            // ★ 원본 L996-1000: hasClimbGap || hasClimbCrawlGap + handsClimbing.FastUp +
            //   feetClimbing(None or BaseWithHands) → climb into crawl gap.
            //   = 2칸 벽 위에 갭 있을 때 자동 등반 분기. 사용자 보고 "2칸 벽 안 올라감" 직접 원인.
            else if ((sm.hasClimbGap || sm.hasClimbCrawlGap)
                    && handsClimbing == HandsClimbing.FAST_UP
                    && (feetClimbing == FeetClimbing.NONE
                            || feetClimbing == FeetClimbing.BASE_WITH_HANDS)) {
                value = (feetClimbing == FeetClimbing.NONE) ? SLOW_UP_MOTION : FAST_UP_MOTION;
                isUp = true;
            }
            // 원본 L1001-1005: feet.IsRelevant && hands.IsRelevant + 3 예외 조합 → MediumUp.
            else if (feetClimbing.isRelevant() && handsClimbing.isRelevant()
                    && !(feetClimbing == FeetClimbing.BASE_HOLD && handsClimbing == HandsClimbing.SINK)
                    && !(handsClimbing == HandsClimbing.SINK && feetClimbing == FeetClimbing.TOP_WITH_HANDS)
                    && !(handsClimbing == HandsClimbing.TOP_HOLD && feetClimbing == FeetClimbing.TOP_WITH_HANDS)) {
                value = MEDIUM_UP_MOTION; isUp = true;
            }
            // 원본 L1006-1010: handsClimbing.IsUp() → SlowUpMotion.
            else if (handsClimbing.isUp()) {
                value = SLOW_UP_MOTION; isUp = true;
            }
            // 원본 L1011-1021: TopHold || BaseHold || (SlowUpWithHoldWithoutHands && hands None) → Hold.
            //   원본은 jumpButton.StartPressed 시 climbJump 시도 (현재 미이식).
            else if (handsClimbing == HandsClimbing.TOP_HOLD
                    || feetClimbing == FeetClimbing.BASE_HOLD
                    || (feetClimbing == FeetClimbing.SLOW_UP_WITH_HOLD_WITHOUT_HANDS
                            && handsClimbing == HandsClimbing.NONE)) {
                value = HOLD_MOTION; isUp = true;
            }
            // 원본 L1022-1026: Sink || (SlowUpWithSinkWithoutHands && hands None) → SinkDown.
            else if (handsClimbing == HandsClimbing.SINK
                    || (feetClimbing == FeetClimbing.SLOW_UP_WITH_SINK_WITHOUT_HANDS
                            && handsClimbing == HandsClimbing.NONE)) {
                value = SINK_DOWN_MOTION; isUp = false;
            }
            else {
                // fallback (어디에도 안 걸리면 정지) — 원본은 기본값 setShouldClimbSpeed 호출 안 함.
                // 우리는 isClimbing 발동 위해 HOLD_MOTION fallback.
                value = HOLD_MOTION; isUp = true;
            }
        } else {
            // 원본 L1028-1053 wantClimbDown 분기 매핑.
            handsClimbing = handsClimbing.toDown();

            if (handsClimbing == HandsClimbing.BOTTOM_HOLD && !feetClimbing.isIndependentlyRelevant()) {
                value = HOLD_MOTION; isUp = false;
            } else if (handsClimbing.isRelevant()) {
                if (feetClimbing == FeetClimbing.FAST_UP) {
                    value = CLIMB_DOWN_MOTION; isUp = false;
                } else if (feetClimbing == FeetClimbing.SLOW_UP_WITH_HOLD_WITHOUT_HANDS) {
                    value = CLIMB_DOWN_MOTION; isUp = false;
                } else if (feetClimbing == FeetClimbing.TOP_WITH_HANDS) {
                    value = CLIMB_DOWN_MOTION; isUp = false;
                } else if (feetClimbing == FeetClimbing.BASE_WITH_HANDS
                        || feetClimbing == FeetClimbing.BASE_HOLD) {
                    if ((handsClimbing != HandsClimbing.NONE && handsClimbing != HandsClimbing.UP)
                            || (handsClimbing == HandsClimbing.UP && feetClimbing == FeetClimbing.BASE_HOLD)) {
                        value = CLIMB_DOWN_MOTION; isUp = false;
                    } else {
                        value = SINK_DOWN_MOTION; isUp = false;
                    }
                } else {
                    value = SINK_DOWN_MOTION; isUp = false;
                }
            } else {
                value = SINK_DOWN_MOTION; isUp = false;
            }

            // 🔴 원본 L1055-1058 isClimbHolding HoldMotion 분기 매핑 (sneak 키 가드).
            //   원본 isClimbHolding = wantClimb && (sneak || crawlToggled) && isClimbing.
            //
            // 🔴 사용자 요구 추가 (handsClimbing.SINK 통합):
            //   "iron_bars 위 잡고 그랩만 누르면 안떨어져야됨 (일반 블록 그랩 홀드 코드 참고)".
            //   원본 동작:
            //     일반 블록 grab → handsClimbing=BottomHold (jh_offset 작음) → ToDown 변환 없음
            //                   → wantClimbDown 분기 L694 BottomHold + !feet → HoldMotion (자동 hold).
            //     iron_bars/fence 위 잡기 → handsClimbing=Sink/TopHold (jh_offset 큼)
            //                              → ToDown: TopHold→Sink, Sink→Sink → SinkDownMotion (떨어짐).
            //   사용자 의도 = 두 케이스 모두 grab만으로 hold → handsClimbing.SINK 시도 HoldMotion 강제.
            //   원본은 sneak 필요했지만 사용자 명시 의도 우선.
            if (sm.isClimbHolding || handsClimbing == HandsClimbing.SINK) {
                value = HOLD_MOTION; isUp = true;
            }

            // 🔴 사용자 요구 추가 (handsEdgeBlock/feetEdgeBlock = fence/iron_bars):
            //   handsClimbing 매핑이 SINK 가 아닌 다른 enum (TopHold/Up/None 등) 이어도
            //   잡은 블록이 fence/iron_bars 면 grab만으로 hold. 사용자 명시 의도 — 일반
            //   블록 grab hold 와 동일 동작.
            //   handsEdgeBlock/feetEdgeBlock 은 ClimbGap.state → 클라이밍 진입 시 잡은
            //   실제 블록 (BlockState). null 가드 필수.
            boolean grabbedFenceOrBars =
                    (sm.handsEdgeBlock != null
                            && (sm.handsEdgeBlock.getBlock() == net.minecraft.block.Blocks.IRON_BARS
                                || Orientation.isFence(sm.handsEdgeBlock)))
                 || (sm.feetEdgeBlock != null
                            && (sm.feetEdgeBlock.getBlock() == net.minecraft.block.Blocks.IRON_BARS
                                || Orientation.isFence(sm.feetEdgeBlock)));
            if (grabbedFenceOrBars) {
                value = HOLD_MOTION; isUp = true;
            }
        }

        // 원본 L1522 factor = getCombinedSpeedFactor() + L1523-1524 isFast sprint factor.
        double freeFactor = SmartMovingMover.getCombinedSpeedFactor(player, cfg);
        if (sm.isFast) {
            freeFactor *= cfg.sprintFactor;
        }
        setOnlyShouldClimbSpeed(player, sm, value, isUp, freeFactor);

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
     * Smart 모드: 주어진 Y 레벨에서 인접 블록에 사다리/넝쿨이 있는지 확인한다.
     * 원본: SmartMovingSelf isHandsLadderSubstitute / isFeetLadderSubstitute 판정.
     * includeCenter=true → 현재 위치(ZZ)도 포함 (feetSubstitute 용).
     */
    private static boolean hasSubstituteLadderOrVine(World world, int px, int py, int pz, boolean includeCenter) {
        if (includeCenter) {
            BlockState s = world.getBlockState(new BlockPos(px, py, pz));
            if (s.getBlock() instanceof LadderBlock || s.getBlock() instanceof VineBlock) return true;
        }
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockState s = world.getBlockState(new BlockPos(px + dir.getOffsetX(), py, pz + dir.getOffsetZ()));
            if (s.getBlock() instanceof LadderBlock || s.getBlock() instanceof VineBlock) return true;
        }
        return false;
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

        // 🔴 (2026-04-27) 천장 블록 종류 검사 — 원본 SmartMovingSelf L1139-1145 1:1.
        //   원본: topBlock = supportsCeilingClimbing(i, j, k);
        //         bottomBlock = supportsCeilingClimbing(i, j+1, k);
        //         if (topBlock != null || bottomBlock != null) { ... }
        //   기본 dictionary (SmartMovingConfig L142): iron bars + closed trapdoor.
        //   기존 CeilingClimbBlocks.supports(state) 헬퍼 호출 — 이미 dictionary 이식됨.
        World world = player.getWorld();
        Box bb = player.getBoundingBox();
        int px = (int) Math.floor(player.getX());
        int pz = (int) Math.floor(player.getZ());
        int topY = (int) Math.floor(bb.maxY);
        BlockState topState    = world.getBlockState(new BlockPos(px, topY, pz));
        BlockState bottomState = world.getBlockState(new BlockPos(px, topY + 1, pz));
        boolean topClimb    = CeilingClimbBlocks.supports(topState);
        boolean bottomClimb = CeilingClimbBlocks.supports(bottomState);
        if (!topClimb && !bottomClimb) return;

        // C-35: jgap = 플레이어 머리(bb.maxY)에서 천장 블록까지의 거리
        // 원본: ceil(bb.maxY) 이상에서 첫 번째 솔리드 블록 Y - bb.maxY
        double jgap = computeJgap(player);

        // 🔴 원본 SmartMovingSelf L1151-1153 가드 2 + 3 — 천장 매달림 진입 범위 제한.
        //   가드 2: jgap < 1.9 — 매달림 위치가 너무 아래면 비활성. bottomCeilingClimbing
        //           시 jgap += 1 → bb.maxY 가 j+1 의 0.1 위쪽까지만 허용 (j+1 이 천장 블록).
        //   가드 3: actuallySolidHeight < jd + 0.5 — 머리 위 0.5 이내에 솔리드 블록 있어야.
        //           실제 천장 (fence/iron_bars + 일반 솔리드) 가 머리 가까이 있어야 매달림.
        //   사용자 보고 #: "천장 그랩 범위가 원본보다 더 아래까지 잡힘" 직접 원인 = 두 가드 누락.
        double jd = bb.maxY;
        double actuallySolidHeight = getMinPlayerSolidBetween(player, jd, jd + 0.6, 0.2);
        if (jgap >= 1.9 || actuallySolidHeight >= jd + 0.5) return;

        // 🔴 motionY = jgap 기반 anchor 보간 (원본 SmartMovingSelf L1145-1166 정밀 1:1).
        //   원본 메커니즘 = 자연스러운 anchor 수렴:
        //     jgap > 1.2  (천장에서 멀음) → motionY=0.12 → setLandMotions 후 +0.0392 (위로)
        //     jgap > 1.115 (anchor 범위)  → motionY=0.08 → 0 (정지)
        //     else        (천장 가까움)   → motionY=0.04 → -0.0392 (아래로 → jgap 회복)
        //   → 평형점 jgap=1.115~1.2 로 수렴. 사용자 가설 "부드러운 anchor 보간" 의 원본 구현.
        //   case 3 의 0.04 떨어짐이 anchor 보간의 핵심 (이전 0.08 변경은 anchor 보간 깨뜨림).
        double climbValue;
        if (jgap > 1.2) {
            climbValue = 0.12D;
        } else if (jgap > 1.115) {
            climbValue = HOLD_MOTION;  // 0.08
        } else {
            climbValue = 0.04D;  // 원본 1:1 — anchor 보간 (천천히 아래로 → jgap 증가)
        }

        // 🔴 motionX/Z: 원본 1:1 — handleCeilingClimbing 안에서 직접 set 하지 않음.
        //   원본 SmartMovingSelf L658 handleCeilingClimbing 은 motionY 만 set.
        //   motionX/Z 는 별도 흐름 (L653 landMotion 의 L718 sp.moveFlying — vanilla
        //   addVelocity 식). 우리 코드도 MixinLivingEntityClient L290 updateVelocity 가
        //   동일 식 처리 → handleCeilingClimbing 에서 추가 직접 set 하면 누적 → 원본보다 빠름.
        //   해결: 직접 set 제거. L290 만 사용 → 원본 1:1.

        // fallDistance = 0 (필수 — 낙하 데미지 방지)
        player.fallDistance = 0;

        // 🔴 motionY 만 setShouldClimbSpeed 호출 — 일반 grab hold 와 동일 메커니즘
        //   (relevant 가드: value < 0 || value > motionY).
        double combinedFactor = SmartMovingMover.getCombinedSpeedFactor(player, cfg);
        boolean climbIsUp = climbValue >= HOLD_MOTION;
        setShouldClimbSpeed(player, sm, climbValue, climbIsUp, combinedFactor);

        sm.isCeilingClimbing = true;
        // B-38 (세션 58): 원본 L1170 `isCrawling = false` — handleCeilingClimbing 성공
        // 분기 종료부에서 크롤 해제. 원본 L1162 isCeilingClimbing=true 와 함께 성공 분기
        // 내부 (L1162-L1170 사이 속도 세팅 L571/fallDistance L575 완료 후) 배치.
        sm.isCrawling = false;
    }

    /**
     * 천장 블록까지의 갭 계산 (C-35).
     * 원본: sp.boundingBox.maxY 기준, 위쪽으로 첫 솔리드 블록 Y - bb.maxY.
     * jgap이 클수록 방이 높음 → 천장 클라이밍 속도 증가.
     */
    private static double computeJgap(ClientPlayerEntity player) {
        // 🔴 원본 SmartMovingSelf L1147-1149 정밀 1:1:
        //   double jgap = 1D - jd + j;
        //   if (bottomCeilingClimbing) jgap++;
        //
        //   jd = bb.maxY (사용자 머리 y), j = floor(jd).
        //   jgap = 1 - jd + j = (다음 정수 경계) - jd = 사용자 머리에서 다음 블록 경계까지 거리.
        //   bottomCeilingClimbing (= bottomBlock supports() true) 시 jgap += 1.
        //
        //   anchor 평형점 (motionY=0.08, jgap > 1.115 ~ 1.2) 으로 수렴:
        //     - 사용자 매달림 시 maxY = fence.y → jgap = 1 → case 3 → 떨어짐
        //     - 떨어지면서 bottom climbing 활성 + jgap 점차 1.116 진입 → case 2 → 정지
        //     - 평형점 = maxY = fence.y - 0.116 (사용자 머리가 fence 의 0.116 아래)
        //
        //   이전 식 (collision shape 거리 계산) 은 원본과 결과 다름 → 평형점 안 잡힘.
        World world = player.getWorld();
        Box bb = player.getBoundingBox();
        int px = (int) Math.floor(player.getX());
        int pz = (int) Math.floor(player.getZ());

        double jd = bb.maxY;
        int j = (int) Math.floor(jd);
        double jgap = 1D - jd + j;

        // 원본 L1148-1149: bottomCeilingClimbing 시 jgap += 1
        BlockState bottomState = world.getBlockState(new BlockPos(px, j + 1, pz));
        if (CeilingClimbBlocks.supports(bottomState)) {
            jgap += 1.0D;
        }
        return jgap;
    }

    /**
     * 원본 SmartMovingBase L398-L409 `getMinPlayerSolidBetween(yMin, yMax, horizontalTolerance)` 1:1.
     *
     * player AABB 의 Y 범위를 [yMin, yMax] 로 임시 변경 + X/Z `horizontalTolerance` 만큼
     * 확장한 box 와 충돌하는 모든 solid 블록의 minY (블록 하단) 중 최솟값 반환.
     * 결과 ≥ yMin 으로 clamp.
     *
     * 1.21.1 매핑: world.getBlockCollisions(entity, box) → Iterable<VoxelShape> (world 좌표).
     * 각 shape 의 getBoundingBox().minY 중 최솟값.
     *
     * 천장 매달림 가드 3 에서 호출: getMinPlayerSolidBetween(jd, jd + 0.6, 0.2).
     * 머리 위 0.6 범위, X/Z 0.2 확장으로 머리 가까운 솔리드 블록 검출.
     */
    private static double getMinPlayerSolidBetween(ClientPlayerEntity player,
                                                    double yMin, double yMax,
                                                    double horizontalTolerance) {
        Box bb = player.getBoundingBox();
        Box checkBox = new Box(
                bb.minX - horizontalTolerance, yMin, bb.minZ - horizontalTolerance,
                bb.maxX + horizontalTolerance, yMax, bb.maxZ + horizontalTolerance
        );
        double result = yMax;
        Iterable<VoxelShape> collisions = player.getWorld().getBlockCollisions(player, checkBox);
        for (VoxelShape shape : collisions) {
            if (shape.isEmpty()) continue;
            Box shapeBox = shape.getBoundingBox();
            // shape 가 yMin~yMax 와 Y 범위 겹침 (X/Z 는 getBlockCollisions 가 처리)
            if (shapeBox.maxY > yMin && shapeBox.minY < yMax) {
                result = Math.min(result, shapeBox.minY);
            }
        }
        return Math.max(result, yMin);
    }
}
