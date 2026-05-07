package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/**
 * 클라이언트 측 이동 속도 팩터 계산.
 * 원본: SmartMovingSelf.getConfigSpeedFactor / getPotionSpeedFactor /
 *       getNonSlowInputSpeedFactor / getSlowInputSpeedFactor 이식.
 *
 * 포함 항목:
 *   11-1: 속도 팩터 4단계 구조
 */
@Environment(EnvType.CLIENT)
public final class SmartMovingMover {

    private SmartMovingMover() {}

    // ── [11-1] 속도 팩터 4단계 ──────────────────────────────────────────────

    /**
     * 원본 `_speedUser = Creative("move.speed.user")` 팩토리 의미 재현.
     * `Creative` 팩토리는 Creative 게임 모드에서만 true 반환 → 1.21.1 에서는 런타임 gameMode
     * 감지로 대응. B-6 (세션 26) 신설.
     *
     * 원본 `isUserSpeedEnabled() = enabled && _speedUser.value` 에서 `_speedUser.value` 가
     * Creative 때만 true 이므로 이 조합이 Creative 체크 게이트가 됨.
     *
     * 1.21.1 대응:
     *   isUserSpeedEnabled = cfg.enabled && cfg.speedUser && (gameMode == CREATIVE)
     *   cfg.speedUser 는 사용자가 config 파일에서 허용 여부 설정 (기본 false, Easy 1:1).
     *   gameMode 체크는 매 호출 시 런타임 판정.
     */
    private static boolean isCreative(ClientPlayerEntity player) {
        if (player == null) return false;
        ClientPlayerInteractionManager im = MinecraftClient.getInstance().interactionManager;
        return im != null && im.getCurrentGameMode() == GameMode.CREATIVE;
    }

    /**
     * Config 기반 속도 배율.
     * 원본 `SmartMovingSelf.getConfigSpeedFactor()` (L154-157):
     *   `Config.enabled ? Config._speedFactor.value * Config.getUserSpeedFactor() : 1F`
     *
     * B-1 (세션 25): `cfg.enabled` 가드 추가.
     * B-6 (세션 26): Creative 전용 게이트 추가. `cfg.speedUser` 는 원본 `_speedUser.value`
     *   대응 — Creative 팩토리 의미 복원을 위해 런타임 gameMode 체크 병행.
     *   getUserSpeedFactor() 자체는 `!speedUser` 시 1F 반환하므로 Creative 아닐 때는
     *   사용자가 아무리 키 눌러도 factor=1F. 하지만 명시적 게이트가 원본 구조에 더 가까움.
     *
     * 공식 (B-6 이후):
     *   cfg.enabled && (gameMode == CREATIVE) ? speedFactor × (1+speedUserFactor)^exp : 1F
     *
     * Creative 외 게임 모드에서는 cfg.speedFactor (기본 1F) 반영도 안 됨 — 원본과 동일
     *   (`_speedFactor` 자체는 Creative 의존 아니지만, `getConfigSpeedFactor` 는 `Config.enabled`
     *    게이트만 있고, speedFactor × userSpeedFactor 이고 userSpeedFactor 가 Creative 때만
     *    1F 외값). speedFactor 는 전역 배율로 Creative 외에서도 적용되어야 하나 — 재검토.
     *
     * ⚠️ 엄밀 1:1: 원본 `getConfigSpeedFactor` 는 `Config.enabled` 만 체크하고
     *    `_speedFactor.value * getUserSpeedFactor()` 반환. `getUserSpeedFactor()` 가
     *    `!_speedUser.value` 시 1F 반환. 즉 Creative 외에서는 `_speedFactor.value` 만 반영됨.
     *    (`_speedFactor` 는 Creative 무관 전역 배율.)
     *    → 본 메서드에서 Creative 게이트는 userSpeedFactor 부분만 차단하는 게 정확.
     *    아래 구현은 speedFactor 는 전역 반영 + userSpeedFactor 는 Creative 게이트.
     */
    public static float getConfigSpeedFactor(ClientPlayerEntity player, SmartMovingConfig cfg) {
        if (!cfg.enabled) return 1F;
        float userFactor = isCreative(player) ? cfg.getUserSpeedFactor() : 1F;
        return cfg.speedFactor * userFactor;
    }

    /**
     * 포션 효과 포함 이동속도 역산 배율.
     * 원본 `SmartMovingSelf.getPotionSpeedFactor()` (L160-162) 1:1:
     *   `Config.enabled ? getLandMovementFactor() * 10F / (sp.isSprinting() ? 1.3F : 1F) : 1F`
     * B-1 (세션 25): `cfg.enabled` 가드 추가.
     *
     * GENERIC_MOVEMENT_SPEED 속성에 포션 효과가 이미 반영됨 → 이중 적용 주의.
     * 스프린트 modifier(+0.3)도 포함된 상태이므로 스프린팅 시 1.3F로 나눠 제거.
     */
    public static float getPotionSpeedFactor(ClientPlayerEntity player) {
        if (!SmartMovingConfig.Config.enabled) return 1F;
        float landMovementFactor = (float) player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        return landMovementFactor * 10F / (player.isSprinting() ? 1.3F : 1F);
    }

    /**
     * 원본 `SmartMovingSelf.getCombinedSpeedFactor()` (L149-152) 1:1:
     *   `return getConfigSpeedFactor() * getPotionSpeedFactor();`
     * B-1 (세션 25) 신설 — Climber/Jumper 등 6곳에서 인라인 곱셈 대체.
     * B-6 (세션 26): getConfigSpeedFactor 시그니처 변경에 따른 player 전달.
     */
    public static float getCombinedSpeedFactor(ClientPlayerEntity player, SmartMovingConfig cfg) {
        return getConfigSpeedFactor(player, cfg) * getPotionSpeedFactor(player);
    }

    /**
     * 비감속 입력 배율 (얼음/스프린트/달리기).
     * 팩터 적용 우선순위: 얼음 > 스프린트 > 달리기 (중첩 불가).
     *
     * isRunning = isSprinting() && !isFast (A-21 확인값).
     * iceSpeedFactor: 원본 config에 없음 — 1.5F 고정 (A-25 확인).
     */
    public static float getNonSlowInputSpeedFactor(ClientPlayerEntity player, SmartMovingClientState sm,
                                                    SmartMovingConfig cfg) {
        // 🔴 (2026-04-28) 원본 SmartMovingSelf L197-227 1:1 정밀 매핑.
        //   원본: isFast 시 sprintFactor (1.5). !isFast 시 1.0.
        //   landMotion L712 에서 별도: isRunning && !isFast 시 runFactor (1.3) 곱.
        float speedFactor = 1.0F;
        if (sm.isFast) {
            // 원본: !isLevitating ? sprintFactor : sprintFactorLevitate. Levitate 는 SmartMovingFlyer 에서 별도 처리.
            speedFactor *= cfg.sprintFactor;  // 1.5
        }
        // 🔴 원본 SmartMovingSelf L203-205: isClimbing 시 _freeClimbingHorizontalSpeedFactor 곱.
        //   원본 가드 `if(moveStrafing != 0F || moveForward != 0F)` 는 movementInput 정보 필요.
        //   우리 호출자의 climbSpeedFactor 는 updateVelocity(speed, movementInput) 매개로만
        //   motion 에 영향 → 입력 0 시 곱셈 결과 무관. 가드 생략해도 동일 효과.
        //   사용자 보고 #2/#5: 우리 코드에 이 곱셈 누락 → fence/iron_bars 등반·횡이동 속도 원본과 다름.
        if (sm.isClimbing) {
            speedFactor *= cfg.freeClimbingHorizontalSpeedFactor;
        }
        return speedFactor;
    }

    /**
     * 감속 입력 배율 (크롤링/스니킹/아이템사용/천장클라이밍).
     * 팩터들은 순서대로 누적 곱셈 적용.
     */
    public static float getSlowInputSpeedFactor(ClientPlayerEntity player, SmartMovingClientState sm,
                                                 SmartMovingConfig cfg) {
        float f = 1.0F;
        if (player.isUsingItem()) f *= 0.2F;
        if (sm.isCrawling) f *= cfg.crawlFactor;
        else if (player.isSneaking()) f *= cfg.sneakFactor;
        if (sm.isCeilingClimbing) f *= cfg.ceilingClimbingSpeedFactor;
        return f;
    }

    /**
     * 최종 속도 팩터 = 4단계 곱셈.
     * 호출 위치: travel() SM 파이프라인 내 각 handle*() 메서드에서 speedFactor 계산 시.
     * B-6 (세션 26): getConfigSpeedFactor 시그니처 변경에 따른 player 전달.
     */
    public static float getSpeedFactor(ClientPlayerEntity player, SmartMovingClientState sm,
                                        SmartMovingConfig cfg) {
        return getConfigSpeedFactor(player, cfg)
             * getPotionSpeedFactor(player)
             * getNonSlowInputSpeedFactor(player, sm, cfg)
             * getSlowInputSpeedFactor(player, sm, cfg);
    }

    // ── 상수 (원본 SmartMovingContext.java) ───────────────────────────────
    private static final float HORIZONTAL_GROUND_DAMPING = 0.546F;
    private static final float HORIZONTAL_AIR_DAMPING    = 0.91F;

    // ── isRunning (원본 SmartMovingSelf.isRunning) ────────────────────────
    /**
     * 원본 `SmartMovingSelf.isRunning()` (L3239-3242) 1:1:
     *   `return sp.isSprinting() && !isFast && (sp.onGround || vanilla())`.
     *
     * cfg.run 체크는 호출 측 (handleLand L712) 에서 별도로 수행 — 원본과 동일.
     */
    public static boolean isRunning(ClientPlayerEntity player, SmartMovingClientState sm,
                                     SmartMovingConfig cfg) {
        return player.isSprinting() && !sm.isFast && (player.isOnGround() || sm.vanilla());
    }

    // ── 11-2: handleLand (원본 superMoveEntityWithHeading + landMotion + setLandMotions) ──

    /**
     * 🔴 (2026-04-28) 원본 SmartMovingSelf.superMoveEntityWithHeading + landMotion +
     *   setLandMotions 통째로 1:1 매핑.
     *
     * 원본 흐름 (`SmartMovingSelf.handleLand` L633-663):
     *   1. landMotion (L675-810): horizontalDamping 결정 + moveFlying (movementInput → motion).
     *   2. move (L655): vanilla move() 호출.
     *   3. handleClimbing (L657): motionY 조정 (별도 분기에서 호출).
     *   4. setLandMotions (L1176-1182): motionY -= 0.08, *= 0.98, motionX/Z *= damping.
     *
     * 비행과 동일 패턴 — vanilla travel 통째로 cancel + SM 자체 식 적용.
     * @return true = SM 처리 완료 (호출 측 ci.cancel 대상).
     */
    public static boolean handleLand(ClientPlayerEntity player, SmartMovingClientState sm,
                                      Vec3d movementInput) {
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!cfg.enabled) return false;

        float moveStrafing = (float) movementInput.x;
        float moveForward  = (float) movementInput.z;

        // 원본 superMoveEntityWithHeading L119-130: speedFactor 계산.
        float speedFactor = getConfigSpeedFactor(player, cfg)
                * getPotionSpeedFactor(player)
                * getNonSlowInputSpeedFactor(player, sm, cfg);
        float slowFactor = getSlowInputSpeedFactor(player, sm, cfg);
        if (sm.vanilla()) {
            moveForward  *= slowFactor;
            moveStrafing *= slowFactor;
        } else {
            speedFactor *= slowFactor;
        }

        // 원본 landMotion L677-690: horizontalDamping 결정 + L686-687 sprintJumpVertical.
        float horizontalDamping;
        if (player.isOnGround() && (!sm.isJumping || sm.vanilla())) {
            BlockState below = player.getWorld().getBlockState(player.getSteppingPos());
            float slip = below.getBlock().getSlipperiness();
            horizontalDamping = slip > 0F ? slip * HORIZONTAL_AIR_DAMPING
                                          : HORIZONTAL_GROUND_DAMPING;
            // 원본 L686-687: jump 키 hold + isFast 시 sprintJumpVerticalFactor 적용.
            boolean jumpKeyPressed = MinecraftClient.getInstance().options.jumpKey.isPressed();
            if (jumpKeyPressed && sm.isFast) {
                speedFactor *= cfg.sprintJumpVerticalFactor;
            }
        } else {
            horizontalDamping = HORIZONTAL_AIR_DAMPING;
        }

        // 원본 landMotion L703-706: jump control factor.
        // Phase F 1:1 정정: 원본 L705 `Config.enabled && ...` 가드 명시 추가 (cosmetic — 함수 시작
        //   `if (!cfg.enabled) return false` 로 이미 함수 단위 가드, 동작 동일).
        if (sm.isHeadJumping) {
            speedFactor *= cfg.headJumpControlFactor;
        } else if (cfg.enabled && !player.isOnGround()
                && !player.getAbilities().flying && !sm.isFlying) {
            speedFactor *= cfg.jumpControlFactor;
        }

        // 원본 landMotion L708-709: rawSpeed.
        float f3 = 0.1627714F / (horizontalDamping * horizontalDamping * horizontalDamping);
        float rawSpeed;
        if (player.isOnGround()) {
            rawSpeed = 0.1F * f3;
        } else {
            float jumpMovementFactor = 0.02F;
            rawSpeed = jumpMovementFactor / (player.isSprinting() && !player.getAbilities().flying ? 1.3F : 1F);
        }

        // 원본 landMotion L712-713: isRunning && !isFast 시 runFactor 곱.
        // (원본 runFactorLevitate 는 levitate 시만 사용 — land 분기 무관, runFactor 만)
        if (cfg.run && isRunning(player, sm, cfg) && !sm.isFast) {
            speedFactor *= cfg.runFactor;
        }

        // 원본 landMotion L714-716: 공중 시 potionFactor 정상화.
        if (!player.isOnGround()) {
            float potionFactor = getPotionSpeedFactor(player);
            if (potionFactor > 0F) speedFactor /= potionFactor;
        }

        // 원본 L718: moveFlying(strafe, forward, rawSpeed * speedFactor) — motion 에 ADD.
        applyLandMoveFlying(player, moveStrafing, moveForward, rawSpeed * speedFactor);

        // 원본 L655: vanilla move() — 위치 갱신.
        player.move(MovementType.SELF, player.getVelocity());

        // 원본 setLandMotions L1176-1182: motionY -= 0.08, *= 0.98, motionX/Z *= damping.
        Vec3d vel = player.getVelocity();
        double newY = (vel.y - 0.08D) * 0.98D;
        double newX = vel.x * horizontalDamping;
        double newZ = vel.z * horizontalDamping;

        // vanilla LivingEntity.travel 마지막의 작은 motion 0 clamp (= 잔존 motion 정리).
        if (Math.abs(newX) < 0.003D) newX = 0.0D;
        if (Math.abs(newY) < 0.003D) newY = 0.0D;
        if (Math.abs(newZ) < 0.003D) newZ = 0.0D;

        player.setVelocity(newX, newY, newZ);

        return true;
    }

    /**
     * 원본 `SmartMovingBase.moveFlying` (L55-93) — yaw 기반 horizontal 분해 + ADD.
     * Land 시 treeDimensional=false → vertical 처리 없음.
     */
    private static void applyLandMoveFlying(ClientPlayerEntity player,
                                             float moveStrafing, float moveForward, float speed) {
        float total = MathHelper.sqrt(moveStrafing * moveStrafing + moveForward * moveForward);
        if (total < 0.01F) return;
        if (total < 1.0F) total = 1.0F;

        float strafeFactor = moveStrafing / total;
        float forwardFactor = moveForward / total;

        float yawRad = player.getYaw() * (float) Math.PI / 180F;
        float sin = MathHelper.sin(yawRad);
        float cos = MathHelper.cos(yawRad);

        float diffX = strafeFactor * cos - forwardFactor * sin;
        float diffZ = forwardFactor * cos + strafeFactor * sin;

        // 원본 L85: total = sqrt(sqrt(diffX^2 + diffZ^2) + diffY^2). diffY=0 (land).
        float horizontal2 = diffX * diffX + diffZ * diffZ;
        float total2 = MathHelper.sqrt(MathHelper.sqrt(horizontal2));
        if (total2 < 0.01F) return;

        float factor = speed / total2;
        Vec3d vel = player.getVelocity();
        player.setVelocity(vel.x + diffX * factor, vel.y, vel.z + diffZ * factor);
    }
}
