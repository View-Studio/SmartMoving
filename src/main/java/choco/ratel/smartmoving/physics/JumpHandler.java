package choco.ratel.smartmoving.physics;

import choco.ratel.smartmoving.config.ConfigManager;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class JumpHandler {

    // 점프 타입 상수 (research_jumping.md 섹션 A)
    public static final int UP               = 0;
    public static final int CHARGE_UP        = 1;
    public static final int ANGLE            = 2;
    public static final int HEAD_UP          = 3;
    public static final int SLIDE_DOWN       = 4;
    public static final int CLIMB_UP         = 5;
    public static final int WALL_UP          = 11;

    private static float jumpChargeMax()      { return ConfigManager.INSTANCE.jumpChargeMaximum; }
    private static float jumpChargeFactor()   { return ConfigManager.INSTANCE.jumpChargeFactor; }
    private static float headJumpChargeMax()  { return ConfigManager.INSTANCE.headJumpChargeMaximum; }
    private static int doubleClickTicks()     { return ConfigManager.INSTANCE.angleJumpDoubleClickTicks; }

    // 바닐라 기본 수직 속도
    static final double VANILLA_JUMP_Y       = 0.41999998688697815D;

    /**
     * 매 틱 점프 충전 상태를 갱신한다.
     * SmartMovingState.tick() 에서 호출.
     */
    public static void update(SmartMovingState state, PlayerEntity player) {
        // 착지 시 공중 점프 상태 초기화
        if (player.isOnGround()) {
            state.isHeadJumping = false;
            state.isWallJumping = false;
            state.isSprintJump  = false;
        }

        // ChargeUp 충전
        boolean chargingCondition = state.grabButton.pressed
                && state.jumpButton.pressed
                && player.isOnGround()
                && !state.isSliding
                && !state.isCrawling;

        if (chargingCondition) {
            if (state.jumpCharge < jumpChargeMax()) {
                state.jumpCharge++;
            }
            state.blockJumpTillButtonRelease = true;
        } else if (state.jumpCharge > 0 && player.isOnGround() && !state.jumpButton.pressed) {
            tryJump(state, player, CHARGE_UP, null, null, null);
            state.jumpCharge = 0;
        } else if (!chargingCondition && !player.isOnGround()) {
            state.jumpCharge = 0;
        }

        // HeadUp 충전 (그랩 + 스프린트 + 점프)
        boolean headChargingCondition = state.grabButton.pressed
                && state.isGroundSprinting
                && state.jumpButton.pressed;

        if (headChargingCondition) {
            if (state.headJumpCharge < headJumpChargeMax()) {
                state.headJumpCharge++;
            }
            state.blockJumpTillButtonRelease = true;
        } else if (state.headJumpCharge > 0 && player.isOnGround() && !state.jumpButton.pressed) {
            tryJump(state, player, HEAD_UP, null, null, null);
            state.headJumpCharge = 0;
        } else if (!headChargingCondition && !player.isOnGround()) {
            state.headJumpCharge = 0;
        }

        // 벽 점프: 공중 + 수평충돌 + grab + jump
        if (!player.isOnGround()
                && player.horizontalCollision
                && state.grabButton.pressed
                && state.jumpButton.startPressed
                && ConfigManager.INSTANCE.wallJumpEnabled) {
            Vec3d wVel = player.getVelocity();
            float wJumpY = (float)(VANILLA_JUMP_Y * getJumpPotionFactor(player));
            double hMag = Math.sqrt(wVel.x * wVel.x + wVel.z * wVel.z);
            double kickX = 0, kickZ = 0;
            if (hMag > 1e-6) {
                // 벽 반대 방향으로 튕겨나옴
                kickX = -wVel.x / hMag * 0.3;
                kickZ = -wVel.z / hMag * 0.3;
            }
            player.setVelocity(kickX, wJumpY, kickZ);
            player.fallDistance = 0F;
            state.isWallJumping = true;
            state.blockJumpTillButtonRelease = true;
        }

        // 클라이밍 점프: 클라이밍 중 점프 키 (wantClimbUp 제외)
        if (state.isClimbing && state.jumpButton.startPressed && !state.wantClimbUp) {
            state.isClimbing = false;
            float jumpY = (float)(VANILLA_JUMP_Y * getJumpPotionFactor(player));
            Vec3d vel = player.getVelocity();
            if (state.backButton.pressed) {
                // 뒤로 점프: 벽에서 반대 방향으로 튀어나옴
                double rad = Math.toRadians(player.getYaw() + 180);
                player.setVelocity(-Math.sin(rad) * 0.4, jumpY, Math.cos(rad) * 0.4);
            } else {
                player.setVelocity(vel.x, jumpY, vel.z);
            }
            player.fallDistance = 0F;
            state.blockJumpTillButtonRelease = true;
        }

        // 각도 점프 더블탭 감지 (매 틱 startPressed 체크)
        if (state.leftButton.startPressed && player.isOnGround()) {
            if (state.leftJumpCount > 0) state.leftJumpCount = -1;
            else state.leftJumpCount = doubleClickTicks();
        }
        if (state.rightButton.startPressed && player.isOnGround()) {
            if (state.rightJumpCount > 0) state.rightJumpCount = -1;
            else state.rightJumpCount = doubleClickTicks();
        }
        if (state.backButton.startPressed && player.isOnGround()) {
            if (state.backJumpCount > 0) state.backJumpCount = -1;
            else state.backJumpCount = doubleClickTicks();
        }

        // 카운터 감소
        if (state.leftJumpCount  > 0) state.leftJumpCount--;
        if (state.rightJumpCount > 0) state.rightJumpCount--;
        if (state.backJumpCount  > 0) state.backJumpCount--;
    }

    /**
     * 일반 점프(jump() 인터셉트)를 처리한다.
     * @return true이면 바닐라 jump() 취소
     */
    public static boolean interceptJump(SmartMovingState state, PlayerEntity player) {
        // ChargeUp/HeadUp 충전 중이면 일반 점프 차단
        if (state.blockJumpTillButtonRelease) {
            if (!state.jumpButton.pressed) {
                state.blockJumpTillButtonRelease = false;
            }
            return true;
        }

        // 각도 점프 더블탭 감지
        if (handleAngleJump(state, player)) return true;

        // 바닐라 호환 Up 점프
        return tryJump(state, player, UP, null, null, null);
    }

    /**
     * 8방향 각도 점프 발동 (update()에서 더블탭 감지 완료 후 -1 신호 확인).
     */
    private static boolean handleAngleJump(SmartMovingState state, PlayerEntity player) {
        float yaw = player.getYaw();

        if (state.leftJumpCount < 0 && ConfigManager.INSTANCE.angleJumpSideEnabled) {
            state.leftJumpCount = 0;
            return tryJump(state, player, ANGLE, null, null, (yaw + 270F) % 360F);
        }
        if (state.rightJumpCount < 0 && ConfigManager.INSTANCE.angleJumpSideEnabled) {
            state.rightJumpCount = 0;
            return tryJump(state, player, ANGLE, null, null, (yaw + 90F) % 360F);
        }
        if (state.backJumpCount < 0 && ConfigManager.INSTANCE.angleJumpBackEnabled) {
            state.backJumpCount = 0;
            return tryJump(state, player, ANGLE, null, null, (yaw + 180F) % 360F);
        }

        return false;
    }

    /**
     * 실제 점프 속도를 적용한다.
     * research_jumping.md 섹션 B 참조.
     * @return true이면 점프 성공 (바닐라 jump() 취소)
     */
    public static boolean tryJump(SmartMovingState state, PlayerEntity player,
                                   int type, Boolean inWater, Boolean isRunning, Float angle) {
        float jumpPotionFactor = getJumpPotionFactor(player);
        float chargeFactor = getJumpChargeFactor(state, type);
        float headFactor = getHeadJumpFactor(state, type);

        float verticalFactor = getVerticalFactor(player, type);
        float horizontalFactor = getHorizontalFactor(player, type);

        // 바닐라 호환 Up 점프
        if (type == UP) {
            Vec3d vel = player.getVelocity();
            double motionY = VANILLA_JUMP_Y * jumpPotionFactor;
            // 스프린트 보너스
            if (player.isSprinting()) {
                float yaw = (float) Math.toRadians(player.getYaw());
                player.setVelocity(
                        vel.x - Math.sin(yaw) * 0.2,
                        motionY,
                        vel.z + Math.cos(yaw) * 0.2
                );
            } else {
                player.setVelocity(vel.x, motionY, vel.z);
            }
            state.isSprintJump = player.isSprinting();
            return true;
        }

        // 기본 수직 속도 공식: -0.078 + 0.498 × vertFactor × chargeFactor
        double verticalMotion = -0.078 + 0.498 * verticalFactor * chargeFactor * jumpPotionFactor;

        Vec3d vel = player.getVelocity();
        double motionX = vel.x;
        double motionZ = vel.z;

        if (angle != null) {
            double rad = Math.toRadians(angle);
            double moveX = -Math.sin(rad);
            double moveZ =  Math.cos(rad);
            motionX = moveX * horizontalFactor;
            motionZ = moveZ * horizontalFactor;
        }

        // HeadUp: 수평 속도를 수직 방향으로 재분배 (충전량에 비례해 더 수직으로)
        if (type == HEAD_UP) {
            double hMag = Math.sqrt(motionX * motionX + motionZ * motionZ);
            double totalMotion = Math.sqrt(verticalMotion * verticalMotion + hMag * hMag);
            if (hMag > 1e-6 && headFactor > 0F) {
                double normalAngle = Math.atan2(verticalMotion, hMag);
                // 충전 0 → 기존 각도 유지, 충전 max → 90°(수직)에 가까워짐
                double newAngle = normalAngle + headFactor * (Math.PI / 2 - normalAngle);
                double newVertical   = totalMotion * Math.sin(newAngle);
                double newHorizontal = totalMotion * Math.cos(newAngle);
                double ratio = newHorizontal / hMag;
                motionX *= ratio;
                motionZ *= ratio;
                verticalMotion = newVertical;
            }
            state.isHeadJumping = true;
        }

        player.setVelocity(motionX, verticalMotion, motionZ);
        return true;
    }

    private static float getJumpPotionFactor(PlayerEntity player) {
        var jumpBoost = player.getStatusEffect(StatusEffects.JUMP_BOOST);
        if (jumpBoost == null) return 1F;
        return 1F + (jumpBoost.getAmplifier() + 1) * 0.2F;
    }

    private static float getJumpChargeFactor(SmartMovingState state, int type) {
        if (type != CHARGE_UP || state.jumpCharge <= 0F) return 1F;
        float max = jumpChargeMax();
        float charge = Math.min(state.jumpCharge, max);
        return 1F + (charge / max) * (jumpChargeFactor() - 1F);
    }

    private static float getHeadJumpFactor(SmartMovingState state, int type) {
        if (type != HEAD_UP || state.headJumpCharge <= 0F) return 0F;
        float max = headJumpChargeMax();
        float charge = Math.min(state.headJumpCharge, max);
        return (charge - 1F) / (max - 1F);
    }

    private static float getVerticalFactor(PlayerEntity player, int type) {
        // 간소화: 속도 상태별 배수 (Phase 5 설정으로 이관)
        if (type == CHARGE_UP || type == HEAD_UP) return 1.2F;
        if (type == ANGLE) return 1.0F;
        if (player.isSprinting()) return 1.1F;
        return 1.0F;
    }

    private static float getHorizontalFactor(PlayerEntity player, int type) {
        if (type == ANGLE) return 0.4F;
        return 0F;
    }

    private JumpHandler() {}
}
