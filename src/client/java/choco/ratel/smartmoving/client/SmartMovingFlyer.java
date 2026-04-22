package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * SM 비행 물리 — handleAlternativeFlying() 이식.
 *
 * 원본: SmartMovingSelf.handleAlternativeFlying() (602-631줄)
 *       + SmartMovingBase.moveFlying() (56-93줄)
 *
 * 활성 조건: sm.isFlying && cfg.fly (= capabilities.isFlying && Config.isFlyingEnabled())
 * 처리:
 *   1. sneak(하강): motionY += 0.15D, moveUpward -= 0.98F
 *   2. jump(상승): motionY -= 0.15D, moveUpward += 0.98F
 *   3. moveFlying(): yaw 기반 수평 + pitch 기반 수직(treeDimensional=true 시) 속도 합산
 *   4. 감쇠: motionX/Y/Z * HorizontalAirDamping(0.91F)
 */
public class SmartMovingFlyer {

    /**
     * SM 비행 물리를 적용한다.
     *
     * @param jumping LivingEntity.jumping 필드 (Space 키 보유 여부)
     * @return true → SM이 처리함, 호출 측에서 ci.cancel() + return
     */
    public static boolean handleFlying(ClientPlayerEntity player, SmartMovingClientState sm,
                                       Vec3d movementInput, boolean jumping) {
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!sm.isFlying || !cfg.fly) return false;

        float moveUpward = 0F;
        Vec3d vel = player.getVelocity();

        // sneak = 하강 (원본: esp.movementInput.sneak → motionY += 0.15D, moveUpward -= 0.98F)
        if (player.isSneaking()) {
            player.setVelocity(vel.x, vel.y + 0.14999999999999999D, vel.z);
            vel = player.getVelocity();
            moveUpward -= 0.98F;
        }
        // jump = 상승 (원본: esp.movementInput.jump → motionY -= 0.15D, moveUpward += 0.98F)
        if (jumping) {
            player.setVelocity(vel.x, vel.y - 0.14999999999999999D, vel.z);
            moveUpward += 0.98F;
        }

        float moveForward = (float) movementInput.z;
        float moveStrafe  = (float) movementInput.x;

        // speedFactor = getConfigSpeedFactor() × getPotionSpeedFactor()
        // getConfigSpeedFactor  = cfg.speedFactor × getUserSpeedFactor()
        // getPotionSpeedFactor  ≈ player.getMovementSpeed() × 10F / (sprinting ? 1.3F : 1F)
        //   → 기본값: 0.1F × 10F / 1F = 1.0F (포션 효과 자동 반영)
        float configFactor = cfg.speedFactor * cfg.getUserSpeedFactor();
        float potionFactor = player.getMovementSpeed() * 10F / (player.isSprinting() ? 1.3F : 1F);
        float flyingSpeed  = configFactor * potionFactor * 0.05F * cfg.flyingSpeedFactor;

        // 원본: moveFlying(moveUpward, moveStrafing, moveForward, speed, Options._flyControlVertical)
        moveFlying(player, moveUpward, moveStrafe, moveForward, flyingSpeed, cfg.flyControlVertical);

        // 감쇠 (HorizontalAirDamping = 0.91F)
        vel = player.getVelocity();
        player.setVelocity(vel.x * 0.91F, vel.y * 0.91F, vel.z * 0.91F);

        return true;
    }

    /**
     * 원본: SmartMovingBase.moveFlying(moveUpward, moveStrafing, moveForward, speedFactor, treeDimensional)
     *
     * 단계:
     *   1. YAW 기반 수평 방향 벡터 계산 (input 정규화 + sin/cos 분해)
     *   2. pitch 기반 수직 보정 (treeDimensional=true 시)
     *   3. 최종 속도 증분 계산 (비표준 정규화: sqrt(sqrt(x²+z²) + y²))
     *   4. player velocity에 합산
     */
    static void moveFlying(ClientPlayerEntity player,
                           float moveUpward, float moveStrafing, float moveForward,
                           float speedFactor, boolean treeDimensional) {
        float diffMotionXStrafing = 0F, diffMotionXForward = 0F;
        float diffMotionZStrafing = 0F, diffMotionZForward = 0F;

        // 1단계: YAW 기반 수평 방향 벡터
        float total = (float) Math.sqrt(moveStrafing * moveStrafing + moveForward * moveForward);
        if (total >= 0.01F) {
            if (total < 1.0F) total = 1.0F;  // 정규화 하한 (원본 동일)
            float moveStrafingFactor = moveStrafing / total;
            float moveForwardFactor  = moveForward  / total;
            float yawRad = (float) Math.toRadians(player.getYaw());
            float sin = (float) Math.sin(yawRad);
            float cos = (float) Math.cos(yawRad);
            diffMotionXStrafing =  moveStrafingFactor * cos;
            diffMotionXForward  = -moveForwardFactor  * sin;
            diffMotionZStrafing =  moveStrafingFactor * sin;
            diffMotionZForward  =  moveForwardFactor  * cos;
        }

        // 2단계: pitch 기반 수직 보정 (treeDimensional=true 시 pitch → 라디안)
        // RadiantToAngle = 180/π → rotationPitch / RadiantToAngle = toRadians(pitch)
        float rotation = treeDimensional ? (float) Math.toRadians(player.getPitch()) : 0F;
        float divingHorizontalFactor = (float) Math.cos(rotation);
        // signum(moveForward): 앞으로 볼 때(forward>0) 위를 보면 올라감, 뒤를 볼 때는 반전
        float divingVerticalFactor   = -(float) Math.sin(rotation) * Math.signum(moveForward);

        // 3단계: 최종 속도 증분
        float diffMotionX = diffMotionXForward * divingHorizontalFactor + diffMotionXStrafing;
        // diffMotionY: forward 수평 크기 × 수직 인자 + 스니크/점프 이동량
        float diffMotionY = (float) Math.sqrt(
                diffMotionXForward * diffMotionXForward + diffMotionZForward * diffMotionZForward
        ) * divingVerticalFactor + moveUpward;
        float diffMotionZ = diffMotionZForward * divingHorizontalFactor + diffMotionZStrafing;

        // 비표준 정규화: sqrt(sqrt(x²+z²) + y²) — 원본 SmartMovingBase 동일 공식
        float totalNorm = (float) Math.sqrt(
                Math.sqrt(diffMotionX * diffMotionX + diffMotionZ * diffMotionZ) + diffMotionY * diffMotionY
        );
        if (totalNorm > 0.01F) {
            float factor = speedFactor / totalNorm;
            Vec3d vel = player.getVelocity();
            player.setVelocity(
                    vel.x + diffMotionX * factor,
                    vel.y + diffMotionY * factor,
                    vel.z + diffMotionZ * factor
            );
        }
    }
}
