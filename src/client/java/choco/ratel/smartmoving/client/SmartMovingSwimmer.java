package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.Vec3d;

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
    public static void updateSwimState(ClientPlayerEntity player, SmartMovingClientState sm) {
        if (!player.isTouchingWater()) {
            sm.isDipping     = false;
            sm.isSwimming_sm = false;
            sm.isDiving      = false;
            sm.waterMovementTicks = 0;
            return;
        }

        double offset = player.getFluidHeight(FluidTags.WATER) + 0.1625D;
        sm.isDipping     = offset < OFFSET_SWIMMING;
        sm.isSwimming_sm = offset >= OFFSET_SWIMMING && offset < OFFSET_DIVING;
        sm.isDiving      = offset >= OFFSET_DIVING;
        sm.waterMovementTicks++;
    }

    // ── [8-2] handleSwimming ─────────────────────────────────────────────────

    /**
     * 수중 이동 물리를 직접 처리한다. vanilla travel() 물속 분기를 완전 대체.
     * 원본: SmartMovingSelf.handleSwimming(moveForward, moveStrafing, speedFactor, isLiquidClimbing)
     *
     * @param jumping jump 키가 현재 눌려있는 상태 (LivingEntity.jumping)
     * @return SM이 처리했으면 true (호출자가 travel() cancel)
     */
    public static boolean handleSwimming(
            ClientPlayerEntity player,
            SmartMovingClientState sm,
            Vec3d movementInput,
            boolean jumping) {

        if (!sm.isDipping && !sm.isSwimming_sm && !sm.isDiving) return false;

        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (sm.isDiving && !cfg.dive) return false;
        if ((sm.isSwimming_sm || sm.isDipping) && !cfg.swim) return false;

        float moveForward = (float) movementInput.z;
        float moveStrafe  = (float) movementInput.x;

        float speedFactor = sm.isDiving ? cfg.diveSpeedFactor : cfg.swimSpeedFactor;

        Vec3d vel = player.getVelocity();
        double motionX = vel.x;
        double motionY = vel.y;
        double motionZ = vel.z;

        // 점프(다이브업) / 스닉(다이브다운) 키
        boolean diveUp   = jumping;
        boolean diveDown = player.isSneaking() && sm.isDiving;

        if (sm.isDipping) {
            // 수면 경계 — 약간 아래로 당기는 힘 + 수평 이동
            Vec3d fly = moveFlying(player, moveStrafe, moveForward, BASE_SWIM_SPEED * speedFactor);
            motionX += fly.x;
            motionZ += fly.z;
            motionX *= DAMPING_DIPPING_XZ;
            motionY = (motionY - 0.02D) * DAMPING_DIPPING_Y;
            motionZ *= DAMPING_DIPPING_XZ;

        } else if (sm.isSwimming_sm) {
            // 수면 수영 — offset 구간에 따라 수직력 세분화
            double offset = player.getFluidHeight(FluidTags.WATER) + 0.1625D;
            double motionYDiff;
            if      (offset < 1.5D) motionYDiff = -0.02D;
            else if (offset < 1.6D) motionYDiff = -0.01D;
            else if (offset < 1.7D) motionYDiff =  0.00D;
            else if (offset < 1.8D) motionYDiff =  0.01D;
            else                     motionYDiff =  0.02D;

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
            double motionYDiff = 0D;
            if (diveUp) {
                motionYDiff = 0.05D * speedFactor;
            } else if (diveDown) {
                motionYDiff = -(0.01D + 0.1D * speedFactor);
            }

            Vec3d fly = moveFlying(player, moveStrafe, moveForward, BASE_SWIM_SPEED * speedFactor);
            motionX += fly.x;
            motionY += motionYDiff;
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

    // ── moveFlying (수평 전용, treeDimensional=false) ────────────────────────

    /**
     * 원본: SmartMovingBase.moveFlying(moveStrafing, moveForward, speedFactor)
     *
     * 비표준 total 공식: total = sqrt(sqrt(x²+z²))
     * → 대각선 이동 시 표준보다 약한 보정 (의도적 비대칭, 원본 그대로 유지)
     *
     * @return 추가할 속도 증분 (y=0)
     */
    private static Vec3d moveFlying(ClientPlayerEntity player, float strafe, float forward, float speed) {
        float yawRad = (float) Math.toRadians(player.getYaw());
        float sin = (float) Math.sin(yawRad);
        float cos = (float) Math.cos(yawRad);

        double dx = forward * cos - strafe * sin;
        double dz = forward * sin + strafe * cos;

        double horLen = Math.sqrt(dx * dx + dz * dz);
        double total  = Math.sqrt(horLen); // sqrt(sqrt(x²+z²)) — SM 비표준 공식

        if (total > 0.01D) {
            double factor = speed / total;
            return new Vec3d(dx * factor, 0, dz * factor);
        }
        return Vec3d.ZERO;
    }
}
