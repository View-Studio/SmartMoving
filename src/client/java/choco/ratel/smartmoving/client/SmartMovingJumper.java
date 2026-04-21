package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 클라이언트 측 점프 물리 로직.
 * 원본: SmartMovingSelf.handleJumping(), tryJump(), getJumpMoving(), handleWallJumping()
 *
 * 포함 항목:
 *   10-1: handleJumping() — 점프 판정 진입점
 *   10-2: tryJump() — 실제 속도 계산
 *   10-3: getJumpMoving() — 각도 점프 수평 속도 헬퍼
 *   10-4: setPoseSmall() / resetHeightOffset() — 헤드점프 hitbox
 *   10-5: handleWallJumping() — 벽 점프
 */
@Environment(EnvType.CLIENT)
public final class SmartMovingJumper {

    private SmartMovingJumper() {}

    // ── jumpType 상수 ────────────────────────────────────────────────────────
    // 원본: SmartMovingConfig.Up / ChargeUp / HeadUp / WallUp 등 정수 상수
    public static final int UP = 0, CHARGE_UP = 1, HEAD_UP = 2,
            WALL_UP = 3, CLIMB_UP = 4, CLIMB_BACK = 5, CLIMB_BACK_HEAD = 6,
            LEFT = 7, RIGHT = 8, BACK = 9;

    // ── [10-3] getJumpMoving ─────────────────────────────────────────────────

    /**
     * 각도 점프 수평 속도 계산.
     * 원본: SmartMovingSelf.getJumpMoving() 공식 그대로 이식.
     *
     * | reset=false      | actual + move * horizontal                          |
     * | reset, 반대 방향 | move * horizontalJumpFactor                         |
     * | reset, 같은 방향 | max(|actual|, |move| * horizontal) * signum(move)   |
     */
    public static double getJumpMoving(double actual, double move, boolean reset,
                                        double horizontal, float horizontalJumpFactor) {
        if (!reset)
            return actual + move * horizontal;
        else if (Math.signum(actual) != Math.signum(move))
            return move * horizontalJumpFactor;
        else
            return Math.max(Math.abs(actual), Math.abs(move) * horizontal) * Math.signum(move);
    }

    // ── [10-4] setPoseSmall / resetHeightOffset ──────────────────────────────

    /**
     * 헤드점프 시작: SWIMMING 포즈로 전환하여 hitbox 재계산.
     * 원본: setHeightOffset(-1F) → boundingBox.minY 직접 조작 (1.7.10)
     * 1.21.1: getBaseDimensions() Mixin + 포즈 전환으로 대체.
     */
    public static void setPoseSmall(ClientPlayerEntity player) {
        player.setPose(EntityPose.SWIMMING);
        player.calculateDimensions();
    }

    /**
     * 헤드점프 종료(착지): STANDING 포즈 복원. 공간 부족 시 SWIMMING 유지.
     * 원본: resetHeightOffset() + standUp()
     * PlayerEntity는 recalculateDimensions()가 자동 호출되지 않으므로 공간 체크를 SM이 직접 수행.
     */
    public static void resetHeightOffset(ClientPlayerEntity player, SmartMovingClientState sm) {
        sm.isHeadJumping = false;
        sm.heightOffset = 0F;

        World world = player.getWorld();
        int px = (int) Math.floor(player.getX());
        int pz = (int) Math.floor(player.getZ());
        int fromY = (int) Math.ceil(player.getY() + 1.0D);
        int toY   = (int) Math.ceil(player.getY() + 1.8D);

        boolean canStand = true;
        for (int by = fromY; by <= toY; by++) {
            BlockPos pos = new BlockPos(px, by, pz);
            if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
                canStand = false;
                break;
            }
        }

        if (canStand) {
            player.setPose(EntityPose.STANDING);
            player.calculateDimensions();
        }
        // 공간 부족 시 SWIMMING 포즈 유지
    }

    // ── [10-2] tryJump ───────────────────────────────────────────────────────

    /**
     * 실제 점프 속도를 계산하고 적용한다.
     * 원본: SmartMovingSelf.tryJump() 이식.
     *
     * @param jumpType UP / CHARGE_UP / HEAD_UP / WALL_UP 등 jumpType 상수
     * @param charge   차지 점프 누적값 (0 ~ cfg.jumpChargeMaximum)
     */
    public static void tryJump(ClientPlayerEntity player, SmartMovingClientState sm,
                                int jumpType, float charge) {
        SmartMovingConfig cfg = SmartMovingConfig.INSTANCE;

        boolean up   = jumpType == UP || jumpType == CHARGE_UP || jumpType == HEAD_UP;
        boolean head = jumpType == HEAD_UP;
        boolean fast = player.isSprinting();

        Vec3d vel = player.getVelocity();
        double motionX = vel.x;
        double motionZ = vel.z;

        // ── 수직 속도 계산 ──────────────────────────────────────────────────
        double verticalMotion;
        if (up && !player.isTouchingWater()) {
            // 원본: vanilla Up 점프 수치 그대로 사용
            // 0.41999998688697815D + potionJump * 0.1F
            StatusEffectInstance jumpBoost = player.getStatusEffect(StatusEffects.JUMP_BOOST);
            int potionJump = (jumpBoost != null) ? jumpBoost.getAmplifier() : 0;
            verticalMotion = 0.41999998688697815D + potionJump * 0.1F;
        } else {
            // 원본: -0.078 + 0.498 * verticalJumpFactor * jumpChargeFactor
            // [미확인 — jumpChargeFactor 보간 공식 원본 세부값 미확인]
            float jumpChargeFactor = 1F + (cfg.jumpChargeMaximum > 0
                    ? charge / cfg.jumpChargeMaximum * (cfg.jumpChargeFactor - 1F)
                    : 0F);
            verticalMotion = -0.078 + 0.498 * 1.0D * jumpChargeFactor;
        }

        // ── 스프린트 점프 수평 보정 ─────────────────────────────────────────
        // 원본: motionX -= sin(yaw) * 0.2F; motionZ += cos(yaw) * 0.2F
        if (fast) {
            double yawRad = Math.toRadians(player.getYaw());
            motionX -= Math.sin(yawRad) * 0.2F;
            motionZ += Math.cos(yawRad) * 0.2F;
        }

        // ── 헤드점프 각도 재계산 ────────────────────────────────────────────
        // 원본: normalAngle = atan(vMotion/hSpeed)
        //       newAngle = headJumpControlFactor * normalAngle
        //       vMotion = totalMotion * sin(newAngle)
        //       hMotion = totalMotion * cos(newAngle)
        if (head) {
            double horizontalSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
            if (horizontalSpeed > 0.01D) {
                float normalAngle = (float) Math.atan(verticalMotion / horizontalSpeed);
                float newAngle = cfg.headJumpControlFactor * normalAngle;
                double totalMotion = Math.sqrt(horizontalSpeed * horizontalSpeed + verticalMotion * verticalMotion);
                double newVertical    = totalMotion * Math.sin(newAngle);
                double newHorizontal  = totalMotion * Math.cos(newAngle);
                double scale = newHorizontal / horizontalSpeed;
                motionX *= scale;
                motionZ *= scale;
                verticalMotion = newVertical;
            }
        }

        // ── 속도 적용 ───────────────────────────────────────────────────────
        // setVelocity() 내부에서 velocityDirty = true 자동 세팅
        player.setVelocity(motionX, verticalMotion, motionZ);

        // ── 헤드점프 상태 세팅 ──────────────────────────────────────────────
        if (head) {
            sm.isHeadJumping = true;
            sm.heightOffset = -1F;
            setPoseSmall(player);
        }
        sm.isSprintJump = fast;

        // [미확인 — Stats.JUMP 1.21.1 Yarn명 확인 필요 → TODO Phase 13]

        // ── 상태 클리어 ─────────────────────────────────────────────────────
        sm.jumpCharge = 0F;
        sm.headJumpCharge = 0F;
        sm.blockJumpTillButtonRelease = true;
        sm.jumpPending = false;
    }

    // ── [10-1] handleJumping ─────────────────────────────────────────────────

    /**
     * 점프 판정 진입점. 매 틱 sm_travel_client() HEAD에서 호출된다.
     * 원본: SmartMovingSelf.handleJumping()
     *
     * 처리 순서:
     *   a. blockJumpTillButtonRelease 해제
     *   b. 차지 점프 (Sneak 홀드 → 릴리즈)
     *   c. 헤드점프 차지 (Grab 홀드 → 릴리즈)
     *   d. 수면 점프 (isDipping)
     *   e. 일반 점프 (jumpPending)
     *   f. jumpPending 클리어
     */
    public static void handleJumping(ClientPlayerEntity player, SmartMovingClientState sm) {
        SmartMovingConfig cfg = SmartMovingConfig.INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();

        boolean jumpKeyPressed  = mc.options.jumpKey.isPressed();
        boolean sneakKeyPressed = mc.options.sneakKey.isPressed();
        boolean grabKeyPressed  = SmartMovingKeys.grab.isPressed();

        // ── a. blockJumpTillButtonRelease 해제 ─────────────────────────────
        // 원본: jumpButton.StopPressed → blockJumpTillButtonRelease = false
        if (sm.blockJumpTillButtonRelease && !jumpKeyPressed) {
            sm.blockJumpTillButtonRelease = false;
        }

        // ── b. 차지 점프 ────────────────────────────────────────────────────
        // 원본: isJumpChargingPossible = onGround && isStanding
        //       isJumpCharging = possible && wouldIsSneaking && Config.isJumpChargingEnabled()
        boolean isJumpCharging = false;
        if (cfg.jumpCharge && player.isOnGround() && !sm.isCrawling && !sm.isSliding) {
            if (sneakKeyPressed && !sm.blockJumpTillButtonRelease) {
                sm.jumpCharge = Math.min(sm.jumpCharge + 1F, cfg.jumpChargeMaximum);
                isJumpCharging = true;
            } else if (sm.jumpCharge > 0 && !sneakKeyPressed) {
                tryJump(player, sm, CHARGE_UP, sm.jumpCharge);
                return;
            }
        }

        // ── c. 헤드점프 차지 ────────────────────────────────────────────────
        // 원본: isHeadJumpCharging = grabButton.Pressed && (isGroundSprinting || isSprintJump || isRunning) && !isCrawling
        // [미확인 — isRunning 조건 생략, isGroundSprinting → player.isSprinting() 근사]
        boolean isHeadJumpCharging = false;
        if (cfg.headJump && grabKeyPressed
                && (player.isSprinting() || sm.isSprintJump)
                && !sm.isCrawling) {
            if (!sm.blockJumpTillButtonRelease) {
                sm.headJumpCharge = Math.min(sm.headJumpCharge + 1F, cfg.headJumpChargeMaximum);
                isHeadJumpCharging = true;
            }
        } else if (sm.headJumpCharge > 0 && !grabKeyPressed) {
            tryJump(player, sm, HEAD_UP, sm.headJumpCharge);
            return;
        }

        // ── d. 수면 점프 ────────────────────────────────────────────────────
        // 원본: isDipping && jumpButton.StartPressed && (posY - floor(posY)) > (isSlow ? 0.37 : 0.6)
        //       motionY -= 0.0399999...; if(onGround) tryJump(Up, ...)
        // [미확인 — isSlow 여부 미구현, 0.6 고정값 사용]
        if (sm.isDipping && sm.jumpPending) {
            double frac = player.getY() - Math.floor(player.getY());
            if (frac > 0.6D) {
                Vec3d vel = player.getVelocity();
                player.setVelocity(vel.x, vel.y - 0.04D, vel.z);
                if (player.isOnGround()) {
                    tryJump(player, sm, UP, 0F);
                    return;
                }
            }
        }

        // ── e. 일반 점프 ────────────────────────────────────────────────────
        // 원본: !blockJumpTillButtonRelease && !isJumpCharging && !isHeadJumpCharging
        //       && !isVineAnyClimbing && jumpButton.StartPressed(=jumpPending)
        if (!sm.blockJumpTillButtonRelease && !isJumpCharging && !isHeadJumpCharging
                && sm.jumpPending && !sm.isClimbing && !sm.isCrawlClimbing) {
            // 방향 점프 각도 계산
            // 원본: angleJumpType = ((360 - movementAngle) / 45) % 8
            Vec3d vel = player.getVelocity();
            if (vel.horizontalLength() > 0.01D) {
                double movementAngle = Math.toDegrees(Math.atan2(-vel.x, vel.z));
                if (movementAngle < 0) movementAngle += 360D;
                sm.angleJumpType = (int) ((360D - movementAngle) / 45D) % 8;
            } else {
                sm.angleJumpType = 0;
            }
            tryJump(player, sm, UP, 0F);
            return;
        }

        // ── f. jumpPending 클리어 ───────────────────────────────────────────
        sm.jumpPending = false;
    }

    // ── [10-5] handleWallJumping ─────────────────────────────────────────────

    /**
     * 벽 점프 처리. sm_travel_client() 내 climbing 처리 전에 호출된다.
     * 원본: SmartMovingSelf.handleWallJumping()
     *
     * 반사 각도 공식: reflectedAngle = horizontalCollisionAngle * 2 - movementAngle + 180
     * jumpAngle = round(reflectedAngle / 90) * 90  (90° 단위 반올림)
     *
     * [미확인 — horizontalCollisionAngle: Orientation.java 로직 이식 필요]
     * 현재 임시값으로 이동 방향 반대(벽 법선)를 사용. TODO: Orientation 이식 후 완성.
     */
    public static void handleWallJumping(ClientPlayerEntity player, SmartMovingClientState sm) {
        SmartMovingConfig cfg = SmartMovingConfig.INSTANCE;
        if (!player.horizontalCollision) return;
        if (!cfg.angleJumpSide && !cfg.angleJumpBack) return;

        Vec3d vel = player.getVelocity();
        if (vel.horizontalLength() < 0.01D) return;

        // 이동 방향 각도 (atan2 기반, 0=북, 시계 방향)
        float movementAngle = (float) Math.toDegrees(Math.atan2(-vel.x, vel.z));
        if (movementAngle < 0) movementAngle += 360F;

        // [미확인 — horizontalCollisionAngle: Orientation.java 이식 필요]
        // 임시값: 이동 방향 반대 (벽 법선이 이동 방향과 반대인 단순 가정)
        float horizontalCollisionAngle = (movementAngle + 180F) % 360F;

        // 원본 반사 공식
        float reflectedAngle = horizontalCollisionAngle * 2 - movementAngle + 180F;
        // 90° 단위 반올림
        float jumpAngle = Math.round(reflectedAngle / 90F) * 90F;

        sm.isWallJumping = true;
        sm.continueWallJumping = !sm.isHeadJumping;

        // 원본: rotationYaw = jumpAngle; isCollidedHorizontally = false; fallDistance = 0F
        player.setYaw(jumpAngle);
        player.bodyYaw = jumpAngle;  // [미확인 — bodyYaw 직접 접근. 컴파일 오류 시 accessor 추가 필요]
        player.horizontalCollision = false;
        player.fallDistance = 0F;

        tryJump(player, sm, WALL_UP, 0F);
    }
}
