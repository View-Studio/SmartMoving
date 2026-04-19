package choco.ratel.smartmoving.physics;

import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;

public final class ClimbingHandler {

    // Phase 5 설정 시스템으로 이관 예정
    public static final double FAST_UP_MOTION   = 0.20D;
    public static final double SLOW_UP_MOTION   = 0.10D;
    public static final double HOLD_MOTION      = 0.08D;
    public static final double SINK_DOWN_MOTION = -0.05D;
    static final float MAX_FALL_DISTANCE = 3.0F;

    /**
     * 매 틱 클라이밍 상태를 갱신한다.
     * SmartMovingState.tick() 에서 호출.
     */
    public static void update(SmartMovingState state, PlayerEntity player) {
        state.wasClimbing = state.isClimbing;

        state.wantClimbUp   = state.grabButton.pressed && state.jumpButton.pressed;
        state.wantClimbDown = state.grabButton.pressed && state.sneakButton.pressed;

        boolean canClimb = state.grabButton.pressed
                && !state.isHeadJumping
                && !state.wantCrawlNotClimb
                && player.fallDistance < MAX_FALL_DISTANCE;

        boolean hasSurface = player.horizontalCollision || isOnClimbableBlock(player);

        state.isClimbing = canClimb && hasSurface;

        if (state.isClimbing) {
            // FeetClimbing 상태 결정 (애니메이션/네트워킹용)
            if (state.wantClimbUp) {
                state.feetClimbingType = FeetClimbing.FastUp.value;
                state.handsClimbingType = HandsClimbing.FastUp.value;
            } else if (state.wantClimbDown) {
                state.feetClimbingType = FeetClimbing.None.value;
                state.handsClimbingType = HandsClimbing.Sink.value;
            } else {
                state.feetClimbingType = FeetClimbing.BaseHold.value;
                state.handsClimbingType = HandsClimbing.BottomHold.value;
            }

            state.isClimbingStill = !state.wantClimbUp && !state.wantClimbDown;
        } else {
            state.feetClimbingType  = FeetClimbing.None.value;
            state.handsClimbingType = HandsClimbing.None.value;
        }

        // 덩굴 추적
        boolean onVine = isOnVine(player);
        state.isHandsVineClimbing = state.isClimbing && onVine;
        state.isFeetVineClimbing  = state.isClimbing && onVine;
        state.isRopeSliding       = onVine && !state.grabButton.pressed
                && !state.isClimbing && state.wantClimbDown;
    }

    private static boolean isOnClimbableBlock(PlayerEntity player) {
        BlockState below = player.getWorld().getBlockState(player.getBlockPos());
        BlockState atFeet = player.getWorld().getBlockState(player.getBlockPos().up());
        return below.isIn(BlockTags.CLIMBABLE) || atFeet.isIn(BlockTags.CLIMBABLE);
    }

    private static boolean isOnVine(PlayerEntity player) {
        BlockState at = player.getWorld().getBlockState(player.getBlockPos());
        return at.isOf(net.minecraft.block.Blocks.VINE);
    }

    private ClimbingHandler() {}
}
