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
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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
     * 헤드점프 시작: SLIDING 포즈로 전환하여 hitbox 재계산.
     * 원본: setHeightOffset(-1F) → boundingBox.minY 직접 조작 (1.7.10)
     * 1.21.1: getBaseDimensions() Mixin(SLIDING→0.6×0.8) + 포즈 전환으로 대체.
     * pose_strategy.md C-02 최종 결정: EntityPose.SLIDING + getBaseDimensions Mixin.
     */
    public static void setPoseSmall(ClientPlayerEntity player) {
        player.setPose(EntityPose.SLIDING);
        player.calculateDimensions();
    }

    /**
     * 헤드점프 종료(착지): STANDING 포즈 복원. 공간 부족 시 SLIDING 유지.
     * 원본: resetHeightOffset() + standUp()
     * PlayerEntity는 Entity.calculateDimensions()에서 제외(pose_strategy.md 5-3) →
     * SM이 직접 공간 확인 후 포즈 전환.
     *
     * 호출 측(MixinLivingEntityClient)이 isOnGround && isHeadJumping 매 틱 체크하므로
     * 공간 확보될 때까지 isHeadJumping = true 유지하여 updatePose Mixin이 SLIDING 지속 강제.
     */
    public static void resetHeightOffset(ClientPlayerEntity player, SmartMovingClientState sm) {
        World world = player.getWorld();
        int px = (int) Math.floor(player.getX());
        int pz = (int) Math.floor(player.getZ());
        int fromY = (int) Math.ceil(player.getY() + 0.8D);
        int toY   = (int) Math.ceil(player.getY() + 1.8D);

        for (int by = fromY; by <= toY; by++) {
            BlockPos pos = new BlockPos(px, by, pz);
            if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
                return; // 공간 부족 — isHeadJumping 유지, SLIDING 포즈 유지
            }
        }

        // 공간 확보 — STANDING 복원
        sm.isHeadJumping = false;
        sm.heightOffset = 0F;
        player.setPose(EntityPose.STANDING);
        player.calculateDimensions();
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
        SmartMovingConfig cfg = SmartMovingConfig.Config;

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
            // verticalJumpFactor = Config._jumpVerticalFactor.value = PositiveFactor 기본값 1F (A-19 확인)
            // jumpChargeFactor = 1F + charge/max * (factor-1F) (getJumpChargeFactor 공식)
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

        // 원본: sp.addStat(StatList.jumpStat, 1) — up(수직 점프)일 때만 기록 (C-37)
        if (up) player.incrementStat(Stats.JUMP);

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
        SmartMovingConfig cfg = SmartMovingConfig.Config;
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
        //   isGroundSprinting = (isFast || isSprinting()) && onGround && !isSliding && !isCrawling
        //   isRunning() = isSprinting() && !isFast && (onGround || vanilla()) (A-21 확인)
        boolean isGroundSprinting = (sm.isFast || player.isSprinting())
                && player.isOnGround() && !sm.isSliding && !sm.isCrawling;
        boolean isRunning = player.isSprinting() && !sm.isFast
                && (player.isOnGround() || sm.isFlying);
        boolean isHeadJumpCharging = false;
        if (cfg.headJump && grabKeyPressed
                && (isGroundSprinting || sm.isSprintJump || isRunning)
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
        //   isSlow = wantSneak && !wantSprint && !isClimbing (A-22 확인)
        //   sm.isSlow는 C-15(tickEssential)에서 매 틱 계산됨
        if (sm.isDipping && sm.jumpPending) {
            double frac = player.getY() - Math.floor(player.getY());
            double threshold = sm.isSlow ? 0.37D : 0.6D;
            if (frac > threshold) {
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
     * horizontalCollisionAngle 알고리즘은 A-23(확인 완료) 참조.
     * C-38: calculateSeparateCollisions() 이식으로 방향별 충돌 감지 후 완성.
     */
    public static void handleWallJumping(ClientPlayerEntity player, SmartMovingClientState sm) {
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!player.horizontalCollision) return;
        if (!cfg.angleJumpSide && !cfg.angleJumpBack) return;

        Vec3d vel = player.getVelocity();
        if (vel.horizontalLength() < 0.01D) return;

        // 이동 방향 각도 (atan2 기반, 0=북, 시계 방향)
        float movementAngle = (float) Math.toDegrees(Math.atan2(-vel.x, vel.z));
        if (movementAngle < 0) movementAngle += 360F;

        // C-38: calculateSeparateCollisions() — 4방향 AABB 충돌 감지
        // 원본 call site: (posZ, negZ, posX, negX) 순서로 swap하여 getHorizontalCollisionangle 호출
        float horizontalCollisionAngle = calculateSeparateCollisionAngle(player, movementAngle);

        // 원본 반사 공식
        float reflectedAngle = horizontalCollisionAngle * 2 - movementAngle + 180F;
        // 90° 단위 반올림
        float jumpAngle = Math.round(reflectedAngle / 90F) * 90F;

        sm.isWallJumping = true;
        sm.continueWallJumping = !sm.isHeadJumping;

        // 원본: rotationYaw = jumpAngle; isCollidedHorizontally = false; fallDistance = 0F
        player.setYaw(jumpAngle);
        player.bodyYaw = jumpAngle;  // LivingEntity.bodyYaw = public float (A-24 확인)
        player.horizontalCollision = false;
        player.fallDistance = 0F;

        tryJump(player, sm, WALL_UP, 0F);
    }

    /**
     * 4방향 AABB 충돌 감지 후 벽 법선 각도를 반환한다.
     * 원본: SmartMovingSelf.calculateSeparateCollisions() + getHorizontalCollisionangle() 호출.
     *
     * 원본 call site: getHorizontalCollisionangle(posZ, negZ, posX, negX) — X/Z swap 유지.
     */
    private static float calculateSeparateCollisionAngle(ClientPlayerEntity player, float movementAngle) {
        World world = player.getWorld();
        Box bb = player.getBoundingBox();
        double delta = 0.001D;
        boolean posX = !world.isSpaceEmpty(player, bb.offset( delta, 0, 0));
        boolean negX = !world.isSpaceEmpty(player, bb.offset(-delta, 0, 0));
        boolean posZ = !world.isSpaceEmpty(player, bb.offset(0, 0,  delta));
        boolean negZ = !world.isSpaceEmpty(player, bb.offset(0, 0, -delta));
        float angle = getHorizontalCollisionangle(posZ, negZ, posX, negX);
        return Float.isNaN(angle) ? (movementAngle + 180F) % 360F : angle;
    }

    /**
     * 4방향 충돌 조합 → 벽 법선 각도(도) 변환.
     * 원본: SmartRenderUtilities.getHorizontalCollisionangle() — A-23 확인 완료.
     *
     * 주의: 원본 call site(SmartMovingSelf.beforeMoveEntity)에서 X/Z 파라미터가 swap됨.
     * 여기서도 같은 순서 유지: (posZ, negZ, posX, negX) → (param1, param2, param3, param4)
     *
     * 단일 방향 충돌 결과: +Z→0°(남벽), -Z→180°(북벽), +X→90°(동벽), -X→270°(서벽)
     */
    public static float getHorizontalCollisionangle(
            boolean isCollidedPositiveX, boolean isCollidedNegativeX,
            boolean isCollidedPositiveZ, boolean isCollidedNegativeZ) {
        if (isCollidedPositiveX) {
            if (isCollidedNegativeX) {
                if (isCollidedPositiveZ)  return isCollidedNegativeZ ? Float.NaN : 90F;
                else                       return isCollidedNegativeZ ? 270F : Float.NaN;
            } else {
                if (isCollidedPositiveZ)  return isCollidedNegativeZ ? 0F : 45F;
                else                       return isCollidedNegativeZ ? 315F : 0F;
            }
        } else {
            if (isCollidedNegativeX) {
                if (isCollidedPositiveZ)  return isCollidedNegativeZ ? 180F : 135F;
                else                       return isCollidedNegativeZ ? 225F : 180F;
            } else {
                if (isCollidedPositiveZ)  return isCollidedNegativeZ ? Float.NaN : 90F;
                else                       return isCollidedNegativeZ ? 270F : Float.NaN;
            }
        }
    }
}
