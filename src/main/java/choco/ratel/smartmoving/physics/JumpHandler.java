package choco.ratel.smartmoving.physics;

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

    // Phase 5 설정 시스템으로 이관 예정
    static final float JUMP_CHARGE_MAX       = 20F;
    static final float JUMP_CHARGE_FACTOR    = 1.3F;
    static final float HEAD_JUMP_CHARGE_MAX  = 10F;
    static final float MAX_FALL_DISTANCE     = 3.0F;

    // 바닐라 기본 수직 속도
    static final double VANILLA_JUMP_Y       = 0.41999998688697815D;

    /**
     * 매 틱 점프 충전 상태를 갱신한다.
     * SmartMovingState.tick() 에서 호출.
     */
    public static void update(SmartMovingState state, PlayerEntity player) {
        // ChargeUp 충전
        boolean chargingCondition = state.grabButton.pressed
                && state.jumpButton.pressed
                && player.isOnGround()
                && !state.isSliding
                && !state.isCrawling;

        if (chargingCondition) {
            if (state.jumpCharge < JUMP_CHARGE_MAX) {
                state.jumpCharge++;
            }
            state.blockJumpTillButtonRelease = true;
        } else if (state.jumpCharge > 0 && player.isOnGround() && !state.jumpButton.pressed) {
            // 충전 해제 → ChargeUp 발동
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
            if (state.headJumpCharge < HEAD_JUMP_CHARGE_MAX) {
                state.headJumpCharge++;
            }
            state.blockJumpTillButtonRelease = true;
        } else if (state.headJumpCharge > 0 && player.isOnGround() && !state.jumpButton.pressed) {
            tryJump(state, player, HEAD_UP, null, null, null);
            state.headJumpCharge = 0;
        } else if (!headChargingCondition && !player.isOnGround()) {
            state.headJumpCharge = 0;
        }

        // 더블탭 각도 점프 카운터 감소
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
     * 8방향 각도 점프 더블탭 감지 및 발동.
     */
    private static boolean handleAngleJump(SmartMovingState state, PlayerEntity player) {
        int doubleClickTicks = 3;

        if (state.leftButton.startPressed) {
            if (state.leftJumpCount > 0) {
                state.leftJumpCount = -1; // 발동 신호
            } else {
                state.leftJumpCount = doubleClickTicks;
            }
        }
        if (state.rightButton.startPressed) {
            if (state.rightJumpCount > 0) {
                state.rightJumpCount = -1;
            } else {
                state.rightJumpCount = doubleClickTicks;
            }
        }
        if (state.backButton.startPressed) {
            if (state.backJumpCount > 0) {
                state.backJumpCount = -1;
            } else {
                state.backJumpCount = doubleClickTicks;
            }
        }

        float yaw = player.getYaw();
        if (state.leftJumpCount < 0) {
            state.leftJumpCount = 0;
            float angle = (yaw + 270F) % 360F;
            return tryJump(state, player, ANGLE, null, null, angle);
        }
        if (state.rightJumpCount < 0) {
            state.rightJumpCount = 0;
            float angle = (yaw + 90F) % 360F;
            return tryJump(state, player, ANGLE, null, null, angle);
        }
        if (state.backJumpCount < 0) {
            state.backJumpCount = 0;
            float angle = (yaw + 180F) % 360F;
            return tryJump(state, player, ANGLE, null, null, angle);
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

        // HeadUp: 각도를 수직으로 재분배
        if (type == HEAD_UP && headFactor > 0F) {
            double hMag = Math.sqrt(motionX * motionX + motionZ * motionZ);
            double totalMotion = Math.sqrt(verticalMotion * verticalMotion + hMag * hMag);
            if (hMag > 1e-6) {
                double normalAngle = Math.atan(verticalMotion / hMag);
                double newAngle = headFactor * normalAngle;
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
        float charge = Math.min(state.jumpCharge, JUMP_CHARGE_MAX);
        return 1F + (charge / JUMP_CHARGE_MAX) * (JUMP_CHARGE_FACTOR - 1F);
    }

    private static float getHeadJumpFactor(SmartMovingState state, int type) {
        if (type != HEAD_UP || state.headJumpCharge <= 0F) return 0F;
        float charge = Math.min(state.headJumpCharge, HEAD_JUMP_CHARGE_MAX);
        return (charge - 1F) / (HEAD_JUMP_CHARGE_MAX - 1F);
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
