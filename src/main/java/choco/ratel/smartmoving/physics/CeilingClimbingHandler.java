package choco.ratel.smartmoving.physics;

import choco.ratel.smartmoving.config.ConfigManager;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;

public final class CeilingClimbingHandler {

    // 천장 클라이밍 수직 속도 (갭 거리별)
    public static final double MOTION_Y_FAR    = 0.12D; // jgap > 1.2
    public static final double MOTION_Y_MID    = 0.08D; // jgap > 1.115
    public static final double MOTION_Y_NEAR   = 0.04D; // 그 이하 (거의 닿음)

    public static final float CEILING_SPEED_FACTOR    = 0.4F;
    static final float MAX_FALL_DISTANCE       = 3.0F;

    /**
     * 매 틱 천장 클라이밍 상태를 갱신한다.
     * ClimbingHandler.update() 이후 호출 (isClimbing이 결정된 뒤).
     */
    public static void update(SmartMovingState state, PlayerEntity player) {
        // 이미 일반 클라이밍 중이면 천장 클라이밍 불가
        if (state.isClimbing) {
            state.isCeilingClimbing = false;
            state.wantClimbCeiling = false;
            return;
        }

        state.wantClimbCeiling = state.grabButton.pressed
                && !state.wantCrawlNotClimb
                && !state.sneakButton.pressed
                && player.fallDistance < MAX_FALL_DISTANCE;

        if (!state.wantClimbCeiling) {
            state.isCeilingClimbing = false;
            return;
        }

        double playerY = player.getY();
        BlockPos headPos = player.getBlockPos().up(); // 플레이어 머리 블록 (Y+1)
        BlockPos aboveHead = headPos.up();            // 머리 위 한 칸 더 (Y+2)

        boolean topSupport    = supportsCeilingClimbing(player, headPos);
        boolean bottomSupport = supportsCeilingClimbing(player, aboveHead);

        if (!topSupport && !bottomSupport) {
            state.isCeilingClimbing = false;
            return;
        }

        // 천장까지의 갭 계산: 플레이어 Y + (headPos.getY() - playerY 의 나머지)
        // headPos = floor(playerY) + 1 이므로
        // jgap = 1 - (playerY - floor(playerY)) + (bottomSupport ? 1 : 0)
        double jd = playerY - Math.floor(playerY); // 블록 내 소수 위치
        double jgap = 1.0 - jd;
        if (bottomSupport) jgap += 1.0;

        // 장애물 체크: 플레이어 바로 위 0.5 이내에 단단한 블록이 없어야 함
        double solidHeight = getSolidHeightAbove(player, playerY);
        if (solidHeight < playerY + 0.5) {
            state.isCeilingClimbing = false;
            return;
        }

        if (jgap < 1.9) {
            state.isCeilingClimbing = true;
        } else {
            state.isCeilingClimbing = false;
            return;
        }

        // 탈진 시스템
        updateExhaustion(state, player);
        if (state.isCeilingClimbing
                && state.maxExhaustionForAction > 0F
                && state.exhaustion >= state.maxExhaustionForAction) {
            state.isCeilingClimbing = false;
        }
    }

    /**
     * 천장 클라이밍 속도를 계산하여 반환한다.
     * LivingEntityTravelMixin 에서 move() 직전에 호출.
     */
    public static double getCeilingMotionY(PlayerEntity player) {
        double playerY = player.getY();
        double jd = playerY - Math.floor(playerY);
        BlockPos aboveHead = player.getBlockPos().up().up();
        boolean bottomSupport = supportsCeilingClimbing(player, aboveHead);

        double jgap = 1.0 - jd;
        if (bottomSupport) jgap += 1.0;

        if (jgap > 1.2)   return MOTION_Y_FAR;
        if (jgap > 1.115) return MOTION_Y_MID;
        return MOTION_Y_NEAR;
    }

    private static boolean supportsCeilingClimbing(PlayerEntity player, BlockPos pos) {
        BlockState state = player.getWorld().getBlockState(pos);
        return state.isIn(BlockTags.CLIMBABLE);
    }

    /**
     * 플레이어 위 0.6 높이 범위에서 솔리드 블록의 최소 Y 경계를 반환한다.
     * 솔리드 블록이 없으면 Double.MAX_VALUE 반환.
     */
    private static double getSolidHeightAbove(PlayerEntity player, double playerY) {
        double x = player.getX();
        double z = player.getZ();
        double checkTop = playerY + 0.6;

        // 플레이어 머리 위 블록 위치들 확인
        int blockY = (int) Math.floor(playerY) + 1;
        int maxBlockY = (int) Math.floor(checkTop) + 1;

        for (int by = blockY; by <= maxBlockY; by++) {
            BlockPos pos = BlockPos.ofFloored(x, by, z);
            BlockState bs = player.getWorld().getBlockState(pos);
            if (bs.isSolidBlock(player.getWorld(), pos)) {
                return by; // 솔리드 블록의 Y 좌표
            }
        }
        return Double.MAX_VALUE;
    }

    private static void updateExhaustion(SmartMovingState state, PlayerEntity player) {
        if (!ConfigManager.INSTANCE.ceilingClimbExhaustionEnabled) {
            state.maxExhaustionForAction     = 0F;
            state.maxExhaustionToStartAction = 0F;
            return;
        }
        state.maxExhaustionForAction     = ConfigManager.INSTANCE.ceilingClimbExhaustionStop;
        state.maxExhaustionToStartAction = ConfigManager.INSTANCE.ceilingClimbExhaustionStart;

        if (state.isCeilingClimbing) {
            state.exhaustion += ConfigManager.INSTANCE.ceilingClimbExhaustionGain;
            state.exhaustion = Math.min(state.exhaustion, state.maxExhaustionForAction);
        } else if (player.isOnGround()) {
            state.exhaustion = Math.max(0F, state.exhaustion - 0.002F);
        }
    }

    private CeilingClimbingHandler() {}
}
