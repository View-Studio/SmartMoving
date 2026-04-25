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

    // ── jumpType 상수 (Phase B-2.5 / 세션 19 정리) ──────────────────────────
    // SmartMovingConfig.JUMP_TYPE_* alias (원본 SmartMovingClientConfig L178-L192).
    // 원본 값으로 통일: Up=0, ChargeUp=1, Angle=2, HeadUp=3, SlideDown=4, ClimbUp=5,
    //   ClimbUpHandsOnly=6, ClimbBackUp=7, ClimbBackUpHandsOnly=8, ClimbBackHead=9,
    //   ClimbBackHeadHandsOnly=10, WallUp=11, WallHead=12, WallUpSlide=13, WallHeadSlide=14.
    public static final int UP                          = SmartMovingConfig.JUMP_TYPE_UP;
    public static final int CHARGE_UP                   = SmartMovingConfig.JUMP_TYPE_CHARGE_UP;
    public static final int ANGLE                       = SmartMovingConfig.JUMP_TYPE_ANGLE;
    public static final int HEAD_UP                     = SmartMovingConfig.JUMP_TYPE_HEAD_UP;
    public static final int SLIDE_DOWN                  = SmartMovingConfig.JUMP_TYPE_SLIDE_DOWN;
    public static final int CLIMB_UP                    = SmartMovingConfig.JUMP_TYPE_CLIMB_UP;
    public static final int CLIMB_UP_HANDS_ONLY         = SmartMovingConfig.JUMP_TYPE_CLIMB_UP_HANDS_ONLY;
    public static final int CLIMB_BACK                  = SmartMovingConfig.JUMP_TYPE_CLIMB_BACK_UP;
    public static final int CLIMB_BACK_HANDS_ONLY       = SmartMovingConfig.JUMP_TYPE_CLIMB_BACK_UP_HANDS_ONLY;
    public static final int CLIMB_BACK_HEAD             = SmartMovingConfig.JUMP_TYPE_CLIMB_BACK_HEAD;
    public static final int CLIMB_BACK_HEAD_HANDS_ONLY  = SmartMovingConfig.JUMP_TYPE_CLIMB_BACK_HEAD_HANDS_ONLY;
    public static final int WALL_UP                     = SmartMovingConfig.JUMP_TYPE_WALL_UP;
    public static final int WALL_HEAD                   = SmartMovingConfig.JUMP_TYPE_WALL_HEAD;
    public static final int WALL_UP_SLIDE               = SmartMovingConfig.JUMP_TYPE_WALL_UP_SLIDE;
    public static final int WALL_HEAD_SLIDE             = SmartMovingConfig.JUMP_TYPE_WALL_HEAD_SLIDE;
    // Phase E-4 (세션 20) 통합 완료: 더블클릭 방향 점프 → tryJump(ANGLE, ..., worldAngleDeg).
    //   기존 LEFT/RIGHT/BACK sentinel 상수는 사용처 0 → 제거.

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

        // B-29 (세션 54): 원본 toSlidingOrCrawling L2226 조건 1:1 복원 —
        //   `Config.isSlidingEnabled() && (grabButton.Pressed || wasHeadJumping)`
        // 기존 `isSprinting() || isFast` 근사 매핑 제거. 원본은 "grab 잡기 or 이전 틱
        // headJumping" 조건 — 의미 완전히 다른 간소 매핑 제거.
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (cfg.slide && cfg.enabled
                && (SmartMovingKeys.grab.isPressed() || sm.wasHeadJumping)) {
            sm.isSliding = true;
        } else {
            Box standBox = player.getDimensions(EntityPose.STANDING)
                                 .getBoxAt(player.getPos())
                                 .contract(1.0E-7);
            if (!world.isSpaceEmpty(player, standBox)) {
                // B-45b (세션 45): 원본 toSlidingOrCrawling else 분기 `wasCrawling = toCrawling()`
                // 대응. 기존 inline (cfg.enabled 누락) → ClientState.toCrawling() 헬퍼 호출.
                sm.toCrawling();
            }
        }

        player.setPose(EntityPose.STANDING);
        player.calculateDimensions();
    }

    // ── [10-2] tryJump (Phase D 1:1 재작성, 세션 19) ────────────────────────

    /**
     * 실제 점프 속도를 계산하고 적용한다.
     *
     * 원본: `SmartMovingSelf.tryJump()` L1999-L2136 (포커스 #2.5 Phase D, 세션 19, 1:1 이식).
     *
     * **§7-1 근사 (D-5/D-14 skip)**: jumpExhaustion 게이트 + 누적 미이식.
     *   Easy 1:1 차원에서 모든 jumpExhaustion=false → exhausionEnabled 항상 false →
     *   D-5/D-14 안쪽 블록 100% dead. focus_05 §6.5 P-9~P-19 dead 분석 일관.
     *   사용자 수동 활성화 시 미작동 (focus_02_5 §7-1 영구 등록).
     *
     * @param type             SmartMovingConfig.JUMP_TYPE_* 상수 (15종)
     * @param inWaterOrNull    null → sm.isDipping 사용 / non-null → 강제 (SlideDown=false 등)
     * @param isRunningOrNull  null → sm.isRunning(player) 사용 / non-null → 강제 (SlideDown=wasRunning 등)
     * @param angle            null → 일반 점프 / non-null → 각도 점프 (Wall/Side/Back)
     * @return enabled         isJumpingEnabled(speed, type) 결과. 호출 측 fallback 판단용.
     */
    public static boolean tryJump(ClientPlayerEntity player, SmartMovingClientState sm,
                                   int type, Boolean inWaterOrNull, Boolean isRunningOrNull, Float angle) {
        SmartMovingConfig cfg = SmartMovingConfig.Config;

        // === D-2 (원본 L2002-L2006) — WallUpSlide/WallHeadSlide → noVertical 변환 ===
        boolean noVertical = false;
        if (type == SmartMovingConfig.JUMP_TYPE_WALL_UP_SLIDE
                || type == SmartMovingConfig.JUMP_TYPE_WALL_HEAD_SLIDE) {
            type = (type == SmartMovingConfig.JUMP_TYPE_WALL_UP_SLIDE)
                    ? SmartMovingConfig.JUMP_TYPE_WALL_UP
                    : SmartMovingConfig.JUMP_TYPE_WALL_HEAD;
            noVertical = true;
        }

        // === D-3 (원본 L2008-L2012) — 지역 변수 계산 ===
        boolean inWater   = inWaterOrNull   != null ? inWaterOrNull   : sm.isDipping;
        boolean isRunning = isRunningOrNull != null ? isRunningOrNull : sm.isRunning(player);
        boolean charged = type == SmartMovingConfig.JUMP_TYPE_CHARGE_UP;
        boolean up = type == SmartMovingConfig.JUMP_TYPE_UP
                  || type == SmartMovingConfig.JUMP_TYPE_CHARGE_UP
                  || type == SmartMovingConfig.JUMP_TYPE_HEAD_UP
                  || type == SmartMovingConfig.JUMP_TYPE_CLIMB_UP
                  || type == SmartMovingConfig.JUMP_TYPE_CLIMB_UP_HANDS_ONLY
                  || type == SmartMovingConfig.JUMP_TYPE_CLIMB_BACK_UP
                  || type == SmartMovingConfig.JUMP_TYPE_CLIMB_BACK_UP_HANDS_ONLY
                  || type == SmartMovingConfig.JUMP_TYPE_CLIMB_BACK_HEAD
                  || type == SmartMovingConfig.JUMP_TYPE_CLIMB_BACK_HEAD_HANDS_ONLY
                  || type == SmartMovingConfig.JUMP_TYPE_ANGLE
                  || type == SmartMovingConfig.JUMP_TYPE_WALL_UP
                  || type == SmartMovingConfig.JUMP_TYPE_WALL_HEAD;
        boolean head = type == SmartMovingConfig.JUMP_TYPE_HEAD_UP
                    || type == SmartMovingConfig.JUMP_TYPE_CLIMB_BACK_HEAD
                    || type == SmartMovingConfig.JUMP_TYPE_CLIMB_BACK_HEAD_HANDS_ONLY
                    || type == SmartMovingConfig.JUMP_TYPE_WALL_HEAD;

        // === D-4 (원본 L2014-L2015) — getJumpSpeed + isJumpingEnabled ===
        int speed = SmartMovingConfig.getJumpSpeed(sm.isStanding, sm.isSlow, isRunning, sm.isFast, angle);
        boolean enabled = cfg.isJumpingEnabled(speed, type);

        // === enabled 게이트 (원본 L2016 if(enabled)) — F-1 감사 정정 (세션 21) ===
        //   D-5 ~ D-16 모든 점프 처리 블록은 enabled 안쪽. 사용자가 sub-jump 비활성 (예:
        //   move.jump.run=false) 시 점프 자체 미작동 — 원본 의도. D-18 상태 클리어는
        //   enabled 와 무관하게 처리 (원본 호출측이 동일하게 처리하던 것을 1.21.1 내부 통합).
        if (enabled) {
            // === D-5 — SKIP (§7-1 근사) ===
            // 원본 L2018-L2027 jumpExhaustion 게이트/누적: Easy default 모든 jumpExhaustion=false →
            //   exhausionEnabled 항상 false → 안쪽 블록 100% dead. focus_05 §6.5 P-9~P-19 일관.
            //   사용자 수동 활성화 시 미작동 (focus_02_5 §7-1 영구 등록).

            // === D-6 (원본 L2029-L2032) — jumpFactor (potion) + horizontal/vertical factor + jumpChargeFactor ===
            float jumpFactor = 1F;
            StatusEffectInstance jumpBoost = player.getStatusEffect(StatusEffects.JUMP_BOOST);
            if (jumpBoost != null)
                jumpFactor = 1F + (jumpBoost.getAmplifier() + 1) * 0.2F;
            float horizontalJumpFactor = cfg.getJumpHorizontalFactor(speed, type) * jumpFactor;
            float verticalJumpFactor   = cfg.getJumpVerticalFactor(speed, type) * jumpFactor;
            float jumpChargeFactor = charged ? cfg.getJumpChargeFactor(sm.jumpCharge) : 1F;

            // === D-7 (원본 L2034-L2038) — !up 변환 (sqrt) ===
            if (!up) {
                horizontalJumpFactor = (float) Math.sqrt(
                        horizontalJumpFactor * horizontalJumpFactor
                                + verticalJumpFactor * verticalJumpFactor);
                verticalJumpFactor = 0F;
            }

            // === D-8 (원본 L2040-L2045) — maxHorizontalMotion + verticalMotion 초기 ===
            Double maxHorizontalMotion = null;
            double horizontalMotion = Math.sqrt(sm.jumpMotionX * sm.jumpMotionX
                                               + sm.jumpMotionZ * sm.jumpMotionZ);
            double verticalMotion = -0.078 + 0.498 * verticalJumpFactor * jumpChargeFactor;
            if (horizontalJumpFactor > 1F && !player.horizontalCollision) {
                maxHorizontalMotion = (double) cfg.getMaxHorizontalMotion(speed, type, inWater)
                                      * SmartMovingMover.getCombinedSpeedFactor(player, cfg);
            }

            // === D-9 + D-10/D-11/D-12 (원본 L2047-L2111) — Up && vanilla 분기 if/else 구조 ===
            //   F-1 감사 정정 (세션 21): 원본은 if(Up && vanilla) {...} else { head/angle/scale }
            //   1.21.1 기존: 4개 if 가 모두 순차 실행 → vanilla Up 점프 시 D-12 스케일 추가
            //   적용 회귀 (sprint 점프 수평 속도 2배 증폭). if/else 구조 복원.
            Vec3d vel = player.getVelocity();
            double motionX = vel.x;
            double motionZ = vel.z;
            if (type == SmartMovingConfig.JUMP_TYPE_UP && sm.vanilla()) {
                // === D-9 (원본 L2047-L2062) — vanilla Up 분기 ===
                verticalMotion = 0.41999998688697815D;
                if (jumpBoost != null) verticalMotion += (jumpBoost.getAmplifier() + 1) * 0.1F;
                if (player.isSprinting()) {
                    float f = player.getYaw() * 0.017453292F;
                    motionX -= Math.sin(f) * 0.2F;
                    motionZ += Math.cos(f) * 0.2F;
                }
            } else {
                // === D-10 (원본 L2065-L2079) — head 재계산 ===
                if (head) {
                    double normalAngle = Math.atan(verticalMotion / horizontalMotion);
                    double totalMotion = Math.sqrt(verticalMotion * verticalMotion
                                                   + horizontalMotion * horizontalMotion);
                    double newAngle = cfg.getHeadJumpFactor(sm.headJumpCharge) * normalAngle;
                    double newVerticalMotion = totalMotion * Math.sin(newAngle);
                    double newHorizontalMotion = totalMotion * Math.cos(newAngle);
                    if (maxHorizontalMotion != null)
                        maxHorizontalMotion = maxHorizontalMotion * (newHorizontalMotion / horizontalMotion);
                    verticalMotion = newVerticalMotion;
                    horizontalMotion = newHorizontalMotion;
                }

                // === D-11 (원본 L2081-L2095) — angle != null 분기 ===
                if (angle != null) {
                    // 원본 RadiantToAngle = 180 / PI ≈ 57.2957795
                    float jumpAngleRad = angle / 57.295776F;
                    boolean reset = type == SmartMovingConfig.JUMP_TYPE_WALL_UP
                                 || type == SmartMovingConfig.JUMP_TYPE_WALL_HEAD;
                    double horizontal = Math.max(horizontalMotion, horizontalJumpFactor);
                    double moveX = -Math.sin(jumpAngleRad);
                    double moveZ =  Math.cos(jumpAngleRad);
                    motionX = getJumpMoving(sm.jumpMotionX, moveX, reset, horizontal, horizontalJumpFactor);
                    motionZ = getJumpMoving(sm.jumpMotionZ, moveZ, reset, horizontal, horizontalJumpFactor);
                    horizontalMotion = 0;
                    verticalMotion = verticalJumpFactor;
                }

                // === D-12 (원본 L2097-L2110) — horizontalMotion > 0 스케일 ===
                if (horizontalMotion > 0) {
                    double absMotionX = Math.abs(motionX) * horizontalJumpFactor;
                    double absMotionZ = Math.abs(motionZ) * horizontalJumpFactor;
                    if (maxHorizontalMotion != null) {
                        absMotionX = Math.min(absMotionX, maxHorizontalMotion
                                        * (horizontalJumpFactor * (Math.abs(motionX) / horizontalMotion)));
                        absMotionZ = Math.min(absMotionZ, maxHorizontalMotion
                                        * (horizontalJumpFactor * (Math.abs(motionZ) / horizontalMotion)));
                    }
                    motionX = Math.signum(motionX) * absMotionX;
                    motionZ = Math.signum(motionZ) * absMotionZ;
                }
            }

            // === D-13 (원본 L2113-L2118) — up && !noVertical → motionY 적용 + Stats + isSprintJump ===
            double motionY = vel.y;  // !up 또는 noVertical 시 default 유지
            if (up && !noVertical) {
                motionY = verticalMotion;
                player.incrementStat(Stats.JUMP);
                sm.isSprintJump = sm.isFast;
            }

            // === D-14 — SKIP (§7-1 근사) ===
            // 원본 L2120-L2124 점프 후 exhaustion 누적: D-5 와 동일 dead.

            // === D-15 (원본 L2126-L2130) — head → isHeadJumping + setPoseSmall + heightOffset ===
            if (head) {
                sm.isHeadJumping = true;
                setPoseSmall(player);
                sm.heightOffset = -1F;
            }

            // === D-16 (원본 L2131-L2134) — setVelocity + isJumping ===
            player.setVelocity(motionX, noVertical ? vel.y : motionY, motionZ);
            // sp.isAirBorne = true — vanilla 자동 (velocityDirty + fallDistance)
            sm.isJumping = true;
            // onLivingJump() — vanilla PlayerEntity 자동 점프 이벤트 처리
        }

        // === D-18 (1.21.1 동작 유지) — 호출 측 상태 클리어를 내부 처리 ===
        //   원본은 호출 측 (handleJumping 등) 에서 jumpCharge=0 등 처리. 1.21.1 은 모든 호출처
        //   (handleJumping 4곳 + handleWallJumping + ClientState 2곳) 가 결과적으로 같은 후처리
        //   필요하므로 내부 통합. SlideDown / Creative flying 호출 시 차징 진행 중일 가능성 0
        //   이므로 부작용 없음.
        sm.jumpCharge = 0F;
        sm.headJumpCharge = 0F;
        sm.blockJumpTillButtonRelease = true;
        sm.jumpPending = false;

        // === D-17 (원본 L2135) — return enabled ===
        return enabled;
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

        // jumpMotionX/Z 저장 — 매 틱 최신 velocity 보존 (getJumpMoving 계산용, 원본: handleJumping 최상단)
        {
            Vec3d cv = player.getVelocity();
            sm.jumpMotionX = cv.x;
            sm.jumpMotionZ = cv.z;
        }

        // ── [IMPL-03] 더블클릭 방향 점프 → tryJump(ANGLE) 통합 (Phase E-4, 세션 20) ──
        //   원본 SmartMovingSelf.handleJumping 의 count==-1 분기 → tryJump(Angle, ..., angle 도)
        //   호출. 새 tryJump (Phase D, 세션 19) 의 D-11 (angle != null) 분기가 수평 속도
        //   재방향 + 수직 속도 (D-8 의 angleJumpVerticalFactor=0.2F 기반) + 스프린트 보정
        //   (D-9 vanilla 진입 안 됨, D-12 스케일 처리) + Stats.JUMP (D-13) + 상태 클리어 (D-18)
        //   모두 통합 처리. 기존 인라인 코드의 vanilla Up 0.41999... 수직 속도는 ANGLE type
        //   에 부적합 — D-8/D-11 의 angleJumpVerticalFactor 기반으로 1:1 정정.
        {
            int left = 0, back = 0;
            if (sm.leftJumpCount  == -1) left++;
            if (sm.rightJumpCount == -1) left--;
            if (sm.backJumpCount  == -1) back++;

            if (left != 0 || back != 0) {
                boolean canAngleJump = player.isOnGround() && !sm.isCrawling && !sm.isClimbing
                        && !sm.isCrawlClimbing && !sm.isSwimming_sm && !sm.isDiving;

                if (canAngleJump) {
                    int relAngle;
                    if (left > 0)      relAngle = back == 0 ? 270 : 225;
                    else if (left < 0) relAngle = back == 0 ? 90  : 135;
                    else               relAngle = 180;

                    // 애니메이션 타입 (원본: ((360 - relAngle) / 45) % 8)
                    sm.angleJumpType = ((360 - relAngle) / 45) % 8;

                    // 세계 공간 점프 방향 (rotationYaw + 상대 각도) — tryJump angle 파라미터로 전달
                    float worldAngleDeg = (float) ((player.getYaw() + relAngle) % 360.0);
                    if (worldAngleDeg < 0F) worldAngleDeg += 360F;

                    // Phase E-4 통합: 새 tryJump(ANGLE, null, null, worldAngleDeg) 단일 호출
                    tryJump(player, sm, ANGLE, null, null, worldAngleDeg);
                }

                sm.leftJumpCount  = 0;
                sm.rightJumpCount = 0;
                sm.backJumpCount  = 0;
                return;
            }
        }

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
                // Phase D 새 시그니처: charge 는 tryJump 내부에서 sm.jumpCharge 직접 사용
                tryJump(player, sm, CHARGE_UP, null, null, null);
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
            // Phase D 새 시그니처: charge 는 tryJump 내부에서 sm.headJumpCharge 직접 사용
            tryJump(player, sm, HEAD_UP, null, null, null);
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
                    // Phase D 새 시그니처: 수면 점프 — inWater null → sm.isDipping 사용
                    tryJump(player, sm, UP, null, null, null);
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
            // Phase D 새 시그니처: 일반 점프 — angle null (방향 점프는 위 더블클릭 인라인 처리)
            tryJump(player, sm, UP, null, null, null);
            return;
        }

        // ── f. jumpPending 클리어 ───────────────────────────────────────────
        sm.jumpPending = false;
    }

    // ── [10-5] 벽 점프 상태 갱신 ─────────────────────────────────────────────

    /**
     * 매 틱 벽 점프 상태(canWallJumping / wallJumpCount / triggerWallJumping /
     * wantWallJumping / continueWallJumping) 갱신.
     *
     * 원본: SmartMovingSelf (L2863-2897) + jump.md L629-668 1:1 이식.
     *
     *   canWallJumping = isWallJumpEnabled && !isHeadJumping && !onGround
     *                    && !isClimbing && !isSwimming && !isDiving
     *                    && !isLevitating && !isFlying
     *
     *   if (_wallJumpDoubleClick) {
     *     if (canWallJumping) {
     *       if (jumpStartPressed) { count==0 → count=ticks(); else trigger=true; count=0 }
     *       else if (count>0) count--
     *     } else count=0
     *   } else trigger = jumpStartPressed
     *
     *   wantWallJumping = canWallJumping &&
     *                     (trigger || continueWallJumping ||
     *                      (wantWallJumping && jumpPressed && !isCollidedHorizontally))
     *
     *   if (continue && (onGround || isClimbing || !jumpPressed)) continue=false
     */
    public static void updateWallJumpState(ClientPlayerEntity player, SmartMovingClientState sm) {
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        boolean isWallJumpEnabled = cfg.wallUpJump || cfg.wallHeadJump;
        boolean canWallJumping = isWallJumpEnabled && !sm.isHeadJumping && !player.isOnGround()
                && !sm.isClimbing && !sm.isSwimming_sm && !sm.isDiving
                && !sm.isLevitating && !sm.isFlying;

        // 더블클릭 모드 분기 (jump.md L629-643)
        if (cfg.wallJumpDoubleClick) {
            if (canWallJumping) {
                if (sm.jumpKeyStartPressed) {
                    if (sm.wallJumpCount == 0) {
                        sm.wallJumpCount = (int) Math.ceil(cfg.wallJumpDoubleClickTicks);
                    } else {
                        sm.triggerWallJumping = true;
                        sm.wallJumpCount = 0;
                    }
                } else if (sm.wallJumpCount > 0) {
                    sm.wallJumpCount--;
                }
            } else {
                sm.wallJumpCount = 0;
            }
        } else {
            sm.triggerWallJumping = sm.jumpKeyStartPressed;
        }

        boolean jumpPressed = MinecraftClient.getInstance().options.jumpKey.isPressed();
        // wantWallJumping 식 (SmartMovingSelf.md L1896-1898). 이전 틱 wantWallJumping 을 읽어
        // "유지 조건" 에 활용하는 자기참조 패턴 — jumpPressed && !collided 이면 계속 true 유지.
        sm.wantWallJumping = canWallJumping &&
                (sm.triggerWallJumping || sm.continueWallJumping ||
                 (sm.wantWallJumping && jumpPressed && !player.horizontalCollision));

        // continueWallJumping false 전환 (jump.md L667-668)
        if (sm.continueWallJumping && (player.isOnGround() || sm.isClimbing || !jumpPressed)) {
            sm.continueWallJumping = false;
        }
    }

    // ── [10-5] handleWallJumping ─────────────────────────────────────────────

    /**
     * 벽 점프 처리. sm_travel_client() 내 climbing 처리 전에 호출된다.
     * 원본: SmartMovingSelf.handleWallJumping() L1946-1997.
     *
     * 원본 진입 조건(L1450): `if (!wantWallJumping || NaN(horizontalCollisionAngle)) return;`
     * → wantWallJumping 은 updateWallJumpState 에서 갱신된 값.
     *
     * 반사 각도 공식: reflectedAngle = horizontalCollisionAngle * 2 - movementAngle + 180
     * jumpAngle = round(reflectedAngle / 90) * 90  (90° 단위 반올림)
     */
    public static void handleWallJumping(ClientPlayerEntity player, SmartMovingClientState sm) {
        SmartMovingConfig cfg = SmartMovingConfig.Config;

        // 원본 L1450: 최우선 조건 — wantWallJumping=false 이면 즉시 return.
        if (!sm.wantWallJumping) return;

        Vec3d vel = player.getVelocity();

        // 이동 방향 각도 (atan2 기반, 0=북, 시계 방향)
        float movementAngle = (float) Math.toDegrees(Math.atan2(-vel.x, vel.z));
        if (movementAngle < 0) movementAngle += 360F;

        // C-38: calculateSeparateCollisions() — 4방향 AABB 충돌 감지
        float horizontalCollisionAngle = calculateSeparateCollisionAngle(player, movementAngle);

        // grab=true → WallHead/WallHeadSlide, grab=false → WallUp/WallUpSlide
        // wasCollidedHorizontally: 이전 틱부터 벽에 닿아있던 경우 Slide 타입 (수직 속도 미적용)
        // 원본: isWallJumpEnabled() = _wallUpJump.value (grab=false), _wallHeadJump.value (grab=true)
        boolean grabPressed = SmartMovingKeys.grab.isPressed();
        int jumpType;
        if (grabPressed) {
            if (!cfg.wallHeadJump) return;
            if (player.fallDistance > cfg.wallHeadJumpFallMaximumDistance) return;
            jumpType = sm.wasCollidedHorizontally ? WALL_HEAD_SLIDE : WALL_HEAD;
        } else {
            if (!cfg.wallUpJump) return;
            if (player.fallDistance > cfg.wallUpJumpFallMaximumDistance) return;
            jumpType = sm.wasCollidedHorizontally ? WALL_UP_SLIDE : WALL_UP;
        }

        // 원본: wasCollidedHorizontally=false → 반사 각도; true → 벽 법선 각도 그대로
        float jumpAngle;
        if (!sm.wasCollidedHorizontally) {
            if (vel.horizontalLength() < 0.01D) return;  // 이동 속도 없으면 반사 각도 계산 불가
            jumpAngle = horizontalCollisionAngle * 2 - movementAngle + 180F;
            while (jumpAngle > 360F) jumpAngle -= 360F;
            // 원본: tolerance != 0 && abs(aligned) < tolerance 일 때만 90° 스냅
            if (cfg.wallUpJumpOrthogonalTolerance != 0F) {
                float aligned = jumpAngle;
                while (aligned > 45F) aligned -= 90F;
                if (Math.abs(aligned) < cfg.wallUpJumpOrthogonalTolerance)
                    jumpAngle = Math.round(jumpAngle / 90F) * 90F;
            }
        } else {
            jumpAngle = horizontalCollisionAngle;
            while (jumpAngle > 360F) jumpAngle -= 360F;
        }

        sm.isWallJumping = true;

        // Phase D 새 시그니처 (세션 19): tryJump 가 angle 파라미터로 D-11 분기 처리.
        //   기존 setVelocity + horizontalCollision/fallDistance 리셋은 tryJump 호출 후
        //   원본 SmartMovingSelf L2068+ 동등하게 처리됨. 사전 setVelocity/리셋 코드 제거.
        player.horizontalCollision = false;
        player.fallDistance = 0F;

        sm.isWallJumping = true;

        // 원본: tryJump(jumpType, null, null, jumpAngle) — angle != null 경로 (D-11)
        //   D-11 의 getJumpMoving 가 wallUp/HeadJumpHorizontalFactor (0.15F) 를 reset=true 로
        //   적용하여 수평 속도 재방향 설정. 기존 인라인 호출 대체.
        tryJump(player, sm, jumpType, null, null, jumpAngle);
        player.setYaw(jumpAngle);
        player.bodyYaw = jumpAngle;
        // 원본: continueWallJumping = !isHeadJumping (tryJump 성공 후 — WallHead 시 false)
        sm.continueWallJumping = !sm.isHeadJumping;
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
