package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

/**
 * 클라이언트 측 슬라이딩 물리 로직.
 * 원본: SmartMovingSelf.landMotion() 슬라이딩 분기, SmartMoving.spawnParticles() 이식.
 *
 * 포함 항목:
 *   9-4: handleSliding() — 슬라이딩 감쇠 공식 + strafe 강제 회전 + travel() 대체
 *   9-5: spawnSlidingParticle() — BlockState 기반 파티클 생성
 *
 * 종료 조건 (sneak 떼기 / 속도 임계) 은 SmartMovingClientState 메인 tick 의
 * SlideToHeadJumping 다음 위치에서 처리한다. 원본 L2563-L2567 1:1.
 */
@Environment(EnvType.CLIENT)
public final class SmartMovingSlider {

    private SmartMovingSlider() {}

    // ── [9-4] handleSliding ──────────────────────────────────────────────────

    /**
     * 슬라이딩 수평 감쇠를 적용한다.
     * vanilla travel()을 대체하여 직접 move()를 호출한다.
     *
     * 감쇠 공식 (원본 SmartMovingSelf.landMotion() L729 isSliding 분기):
     *   damping = 1 / (((1/slip - 1) / 25) * _slideSlipperinessFactor + 1) * 0.98F
     *   - slip: 발 아래 블록의 slipperiness (얼음=0.98, 일반=0.6)
     *   - _slideSlipperinessFactor: Config 값 (기본 1.0F)
     *   - 0.98F: 공기 저항 유사 계수
     *
     * 종료 조건은 SmartMovingClientState 메인 tick 에서 처리 (원본 L2563-L2567 1:1).
     *
     * @return true if SM이 처리 (travel() cancel 대상)
     */
    public static boolean handleSliding(ClientPlayerEntity player, SmartMovingClientState sm) {
        if (!sm.isSliding) return false;
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!cfg.slide) {
            sm.isSliding = false;
            return false;
        }

        Vec3d vel = player.getVelocity();

        // 발 아래 블록 slipperiness
        BlockState below = player.getWorld().getBlockState(player.getSteppingPos());
        float slip = below.getBlock().getSlipperiness();

        // 원본 감쇠 공식
        float damping = 1F / (((1F / slip - 1F) / 25F) * cfg.slideSlipperinessFactor + 1F) * 0.98F;

        double newVx = vel.x * damping;
        double newVz = vel.z * damping;
        // 수직: 중력 적용 (슬라이딩 중 공중에 있으면 낙하)
        double newVy = vel.y - player.getAttributeValue(EntityAttributes.GENERIC_GRAVITY);
        newVy *= 0.98D;

        // 원본 L730-L744: strafe 강제 회전 (좌우 키로 슬라이딩 방향 컨트롤).
        //   moveStrafing != 0 && _slideControlDegrees > 0 일 때 motionX/Z 를 회전.
        //   _slideControlDegrees: tick 당 회전량 (deg). 기본 1F.
        //   moveStrafing 부호 = Math.signum() → 좌/우 회전 방향 결정.
        //   원본 RadiantToAngle = 180/π → Math.toRadians 로 1:1.
        //   damping 적용 후 newVx/newVz 에 회전 (스칼라 damping × 단위 보존 회전 = commute → 결과 동일).
        float moveStrafing = player.input.movementSideways;
        if (moveStrafing != 0F && cfg.slideControlDegrees > 0F) {
            double angle = -Math.atan(newVx / newVz);
            if (!Double.isNaN(angle)) {
                if (newVz < 0) angle += Math.PI;
                angle -= Math.toRadians(cfg.slideControlDegrees) * Math.signum(moveStrafing);
                double hMotion = Math.sqrt(newVx * newVx + newVz * newVz);
                newVx = hMotion * -Math.sin(angle);
                newVz = hMotion *  Math.cos(angle);
            }
        }

        player.setVelocity(newVx, newVy, newVz);
        player.move(MovementType.SELF, player.getVelocity());

        // [9-5] 슬라이딩 파티클
        spawnSlidingParticle(player, sm, new Vec3d(newVx, 0, newVz));

        // 종료 조건은 SmartMovingClientState 메인 tick (SlideToHeadJumping 다음 위치, 원본 L2563-L2567)
        // 에서 sneak 떼기 + horizontalSpeedSquare < stopFactor*0.01 + wasCrawling=toCrawling() 1:1 처리.
        return true;
    }

    // ── [9-5] spawnSlidingParticle ───────────────────────────────────────────

    /**
     * 슬라이딩 파티클 생성.
     * 원본: SmartMoving.spawnParticles() isSliding 분기.
     *
     * 원본 동작:
     *   spawnSlindingParticle += dt (타이머 누적)
     *   if > _slideParticlePeriodFactor * 0.1F (= 0.5F * 0.1F = 0.05F):
     *       파티클: blockcrack_<id>_<meta> → 1.21.1 ParticleTypes.BLOCK + BlockState
     *       motionX/Z = 이동방향 * -4D, motionY = 1.5D
     *       타이머 리셋
     */
    private static void spawnSlidingParticle(ClientPlayerEntity player, SmartMovingClientState sm, Vec3d horizontal) {
        if (!player.getWorld().isClient) return;

        SmartMovingConfig cfg = SmartMovingConfig.Config;
        sm.spawnSlindingParticle += 1F;
        float threshold = cfg.slideParticlePeriodFactor * 0.1F;

        if (sm.spawnSlindingParticle > threshold) {
            sm.spawnSlindingParticle = 0F;

            BlockState below = player.getWorld().getBlockState(player.getBlockPos().down());
            if (below.isAir()) return;

            double speed = horizontal.horizontalLength();
            if (speed < 0.01D) return;

            // 이동 방향 반대로 파티클 발사 (원본: motionX/Z = direction * -4D)
            double nx = horizontal.x / speed;
            double nz = horizontal.z / speed;

            player.getWorld().addParticle(
                    new BlockStateParticleEffect(ParticleTypes.BLOCK, below),
                    player.getX(), player.getY(), player.getZ(),
                    nx * -4D, 1.5D, nz * -4D
            );
        }
    }
}
