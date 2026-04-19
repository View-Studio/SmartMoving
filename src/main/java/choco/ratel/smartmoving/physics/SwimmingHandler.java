package choco.ratel.smartmoving.physics;

import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class SwimmingHandler {

    // Phase 5 설정 시스템으로 이관 예정
    public static final float SWIM_SPEED_FACTOR = 1.0F;
    public static final float DIVE_SPEED_FACTOR = 1.0F;

    static final float SWIMMING_THRESHOLD = 1.4F;
    static final float DIVING_THRESHOLD   = 1.9F;
    static final float J_OFFSET           = 0.1625F;
    static final double WATER_EXIT_MOTION_Y = 0.30000001192092896D;

    /**
     * 매 틱 수영/잠수 상태를 갱신한다.
     */
    public static void update(SmartMovingState state, PlayerEntity player) {
        double waterBorder = getMaxLiquidBetween(player);
        double playerSwimOffset = waterBorder - player.getY() - J_OFFSET;

        state.isDiving   = false;
        state.isSwimming = false;
        state.isDipping  = false;

        if (waterBorder > 0) {
            if (playerSwimOffset >= DIVING_THRESHOLD) {
                state.isDiving = true;
            } else if (playerSwimOffset >= SWIMMING_THRESHOLD) {
                state.isSwimming = true;
            } else if (playerSwimOffset > 0) {
                state.isDipping = true;
                state.dippingDepth = (float) playerSwimOffset;
            }
        }
    }

    /**
     * travel() 내부에서 수영/잠수 물리를 적용한다.
     * LivingEntityTravelMixin에서 호출.
     */
    public static Vec3d applySwimPhysics(SmartMovingState state, PlayerEntity player, Vec3d velocity) {
        if (!state.isSwimming && !state.isDiving && !state.isDipping) return velocity;

        double vx = velocity.x;
        double vy = velocity.y;
        double vz = velocity.z;

        if (state.isDipping) {
            vx *= 0.80D;
            vy *= 0.83D;
            vz *= 0.80D;

            // 수면 탈출 점프
            if (state.jumpButton.pressed && vy <= 0) {
                vy = WATER_EXIT_MOTION_Y;
            }

        } else if (state.isSwimming) {
            vx *= 0.85D;
            vy *= 0.85D;
            vz *= 0.85D;
            vy += getBuoyancyAdjustment(state.dippingDepth);

        } else if (state.isDiving) {
            vx *= 0.83D;
            vy *= 0.83D;
            vz *= 0.83D;

            // 3D 잠수: 피치 각도로 수직 속도 결정
            float pitch = player.getPitch();
            float rotation = (float) Math.toRadians(pitch);
            float forwardFactor = state.forwardButton.pressed ? 1F : (state.backButton.pressed ? -1F : 0F);
            float vertFactor = -(float) Math.sin(rotation) * forwardFactor;
            vy += vertFactor * DIVE_SPEED_FACTOR;
        }

        return new Vec3d(vx, vy, vz);
    }

    /**
     * 수면 높이에 따른 부력 조정값 (7단계 그라디언트).
     * research_swimming.md 섹션 C 참조.
     */
    private static double getBuoyancyAdjustment(float offset) {
        if (offset > 1.672F) return -0.02D;
        if (offset > 1.668F) return -0.015D;
        if (offset > 1.660F) return -0.01D;
        if (offset > 1.640F) return -0.005D;
        if (offset > 1.620F) return -0.0025D;
        if (offset > 1.600F) return -0.00125D;
        if (offset > 1.500F) return -0.000625D;
        return 0D;
    }

    /**
     * 플레이어 Y 범위 내 최대 수위를 반환한다.
     * research_swimming.md 섹션 A 참조.
     */
    private static double getMaxLiquidBetween(PlayerEntity player) {
        double maxY = player.getBoundingBox().maxY + 1.2;
        double minY = player.getBoundingBox().maxY - 1.8;
        double maxBorder = 0;
        World world = player.getWorld();

        int x0 = (int) Math.floor(player.getX() - 0.3);
        int x1 = (int) Math.floor(player.getX() + 0.3);
        int z0 = (int) Math.floor(player.getZ() - 0.3);
        int z1 = (int) Math.floor(player.getZ() + 0.3);
        int iy0 = (int) Math.floor(minY);
        int iy1 = (int) Math.floor(maxY);

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = iy0; y <= iy1; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    FluidState fluid = world.getFluidState(pos);
                    if (fluid.isIn(FluidTags.WATER)) {
                        float height = fluid.getHeight(world, pos);
                        double border = y + height;
                        if (border > maxBorder) maxBorder = border;
                    }
                }
            }
        }
        return maxBorder;
    }

    private SwimmingHandler() {}
}
