package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
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
        // 원본 SmartMovingSelf.handleAlternativeFlying L604:
        //   handleAlternativeFlying = !handledSwimming && !handledLava && capabilities.isFlying && Config.isFlyingEnabled()
        //   1.21.1: 호출 측 sm_travel_client 가 handleSwimming/handleLava 후 도달 = !handledSwimming
        //   && !handledLava 자동 충족. sm.isFlying = isFlyingEnabled() && abilities.flying && !swim
        //   && !dive — 원본 L2510 isFlying 공식 1:1 매핑. cfg.fly 중복 가드 (sm.isFlying 안에 포함).
        if (!sm.isFlying) return false;

        // 원본 L607-L608: resetSwimming(); resetClimbing();
        //   1.21.1: sm_travel_client 가 handleSwimming/handleLava 후 도달 = SM 수영 분기 ci.cancel
        //   되지 않은 경우만 → 수영/lava 처리 안 됨 = reset 의미 동일 (이미 false 유지). 매핑 생략.

        // 원본 L611-L620: sneak/jump → motionY ± 0.15 + moveUpward ± 0.98 (esp.movementInput 직접)
        // 🔴 sneak 검사 정정 (사용자 보고 fix — 비행 sneak 빠른 하강 BUG, 2026-05-03):
        //   기존 `player.isSneaking()` 은 SM `MixinClientPlayerEntity.sm_isSneaking_ClientPlayer`
        //   inject 가 비행 시 false 반환 → 분기 미진입 → vanilla -=0.15 cancel 실패 + SM moveFlying
        //   미적용 + damping x0.91 만 → 매 frame -=0.15 누적 → 수렴 -1.5/tick = -30m/sec 무시무시한 하강.
        //   원본 SmartMovingSelf L611 = `esp.movementInput.sneak` 직접 검사. player.input.sneaking
        //   으로 변경 = 원본 1:1.
        float moveUpward = 0F;
        if (player.input.sneaking) {
            Vec3d vel = player.getVelocity();
            player.setVelocity(vel.x, vel.y + 0.14999999999999999D, vel.z);
            moveUpward -= 0.98F;
        }
        if (jumping) {
            Vec3d vel = player.getVelocity();
            player.setVelocity(vel.x, vel.y - 0.14999999999999999D, vel.z);
            moveUpward += 0.98F;
        }

        float moveForward = (float) movementInput.z;
        float moveStrafe  = (float) movementInput.x;

        // 🔴 BUG-25 정정 (Flying Phase F-6 / 세션 40): getNonSlowInputSpeedFactor 누락 보강.
        //   원본 SmartMovingSelf L119 `speedFactor = getConfigSpeedFactor * getPotionSpeedFactor *
        //   getNonSlowInputSpeedFactor(moveForward, moveStrafing)`.
        //   getNonSlowInputSpeedFactor (L197-L227) 본체:
        //     if (isFast) speedFactor *= (!isLevitating ? sprintFactor : sprintFactorLevitate);
        //     if (isClimbing) ... (비행 무관)
        //   → 비행 시 sprint 키 hold + 비행 가능 = 1.5F (sprintFactor) 또는 sprintFactorLevitate (Levitate)
        //   곱셈. 1.21.1 SmartMovingMover.getCombinedSpeedFactor (= getConfigSpeedFactor *
        //   getPotionSpeedFactor) 만 있어 NonSlow 의 sprint 곱셈 누락 → 비행 sprint 시 1.5배 느림.
        //   해결: isFast 시 sprintFactor (또는 isLevitating 시 sprintFactorLevitate) 곱셈 추가.
        float combinedFactor = SmartMovingMover.getCombinedSpeedFactor(player, cfg);
        if (sm.isFast) {
            combinedFactor *= sm.isLevitating ? cfg.sprintFactorLevitate : cfg.sprintFactor;
        }
        float flyingSpeed    = combinedFactor * 0.05F * cfg.flyingSpeedFactor;

        // 원본: moveFlying(moveUpward, moveStrafing, moveForward, speed, Options._flyControlVertical)
        // 🔴 BUG-27 강제 정정 (Flying Phase / 세션 50): 사용자 환경 config 파일 disk 의존성 제거.
        //   원본 Unmodified default = true (Properties.java L171-L172). 자동 마이그레이션 (세션
        //   48/49) 효과 없을 가능성 (marker 이미 있거나 다른 disk 위치) → cfg 값 무시하고 강제
        //   true 적용 → 마우스 pitch → W 진행 방향 효과 (원본 default 1:1) 보장.
        boolean treeDimensional = true;  // 원본 default 강제. 사용자 옵션은 별도 코드 변경 시만.
        moveFlying(player, moveUpward, moveStrafe, moveForward, flyingSpeed, treeDimensional);

        // 🔴 BUG-1+8 (세션 37): 원본 SmartMovingSelf.handleAlternativeFlying L624
        //   `sp.moveEntity(sp.motionX, sp.motionY, sp.motionZ)` 매핑 — **이전 누락**.
        //   sm_travel_client 가 ci.cancel() 으로 vanilla travel() 차단 → vanilla move() 호출 안 됨
        //   → 기존 setVelocity 만으로는 motion 적용 안 됨 = 비행 시 몸 고정 (BUG-1).
        //   수정: motion 계산 후 player.move() 명시 호출 → vanilla collision/위치 갱신 처리.
        Vec3d motion = player.getVelocity();
        player.move(MovementType.SELF, motion);

        // 감쇠 (HorizontalAirDamping = 0.91F) — 원본 L626-L628
        Vec3d velAfterMove = player.getVelocity();
        player.setVelocity(velAfterMove.x * 0.91F, velAfterMove.y * 0.91F, velAfterMove.z * 0.91F);

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
