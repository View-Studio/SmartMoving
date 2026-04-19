package choco.ratel.smartmoving.physics;

import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class SlidingHandler {

    // Phase 5 설정 시스템으로 이관 예정
    static final float SLIDING_SPEED_STOP_FACTOR = 0.2F;  // 속도 < 0.2 × 0.01 시 해제
    public static final float SLIDE_SLIPPERINESS_FACTOR = 1.0F;
    public static final float SLIDE_CONTROL_DEGREES = 1.0F;

    /**
     * 매 틱 슬라이딩 상태를 갱신한다.
     * SmartMovingState.tick() 에서 호출.
     */
    public static void update(SmartMovingState state, PlayerEntity player) {
        // 진입 조건
        if (!state.isSliding) {
            boolean canStart = state.grabButton.pressed
                    && state.isGroundSprinting
                    && state.sneakButton.startPressed
                    && !state.isCrawling
                    && !state.isDipping;
            if (canStart) {
                state.isSliding = true;
            }
        }

        // 해제 조건
        if (state.isSliding) {
            Vec3d vel = player.getVelocity();
            double hSpeedSq = vel.x * vel.x + vel.z * vel.z;
            if (!state.sneakButton.pressed || hSpeedSq < SLIDING_SPEED_STOP_FACTOR * 0.01) {
                state.isSliding = false;
            }
        }

        if (state.isSliding) {
            state.isSlow = true;
        }
    }

    /**
     * travel() 내부에서 슬라이딩 마찰 및 방향 조정을 적용한다.
     * LivingEntityTravelMixin 에서 호출.
     */
    public static Vec3d applySlidePhysics(SmartMovingState state, PlayerEntity player, Vec3d velocity) {
        if (!state.isSliding) return velocity;

        // 블록 미끄러움 기반 감쇠 계산
        BlockPos groundPos = player.getBlockPos().down();
        BlockState ground = player.getWorld().getBlockState(groundPos);
        float slipperiness = ground.getBlock().getSlipperiness();

        float inv = 1F / slipperiness;
        float damping = 1F / (((inv - 1F) / 25F) * SLIDE_SLIPPERINESS_FACTOR + 1F) * 0.98F;

        double newX = velocity.x * damping;
        double newZ = velocity.z * damping;

        // strafing 입력으로 방향 조정 (속도 크기 보존)
        float strafe = 0F;
        if (state.leftButton.pressed && !state.rightButton.pressed)  strafe =  1F;
        if (state.rightButton.pressed && !state.leftButton.pressed)  strafe = -1F;

        if (strafe != 0F && SLIDE_CONTROL_DEGREES > 0F) {
            double magnitude = Math.sqrt(newX * newX + newZ * newZ);
            if (magnitude > 1e-6) {
                double angle = -Math.atan2(newX, newZ);
                if (newZ < 0) angle += Math.PI;
                angle -= Math.toRadians(SLIDE_CONTROL_DEGREES) * Math.signum(strafe);
                newX = magnitude * -Math.sin(angle);
                newZ = magnitude *  Math.cos(angle);
            }
        }

        return new Vec3d(newX, velocity.y, newZ);
    }

    private SlidingHandler() {}
}
