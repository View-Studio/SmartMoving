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
        // [HEADBROAD-DBG-T1] tryJump 진입 (HEAD_UP / SLIDE_DOWN 만) — speed/방향/motion 추적.
        if (type == SmartMovingConfig.JUMP_TYPE_HEAD_UP
                || type == SmartMovingConfig.JUMP_TYPE_SLIDE_DOWN) {
            String typeName = (type == SmartMovingConfig.JUMP_TYPE_HEAD_UP) ? "HEAD_UP" : "SLIDE_DOWN";
            net.minecraft.util.math.Vec3d _vel = player.getVelocity();
            // 방향 비교 — yaw vs jumpMotion vs velocity 의 deg.
            //   원본: yaw=0 ↔ -Z 방향. atan2(-jumpMotionX, jumpMotionZ)*180/pi - 180 = yaw 와 비교.
            double _jumpMotionAngleDeg = Math.toDegrees(Math.atan2(-sm.jumpMotionX, sm.jumpMotionZ));
            double _velAngleDeg = (_vel.x == 0 && _vel.z == 0) ? 0
                    : Math.toDegrees(Math.atan2(-_vel.x, _vel.z));
            System.out.println("[HEADBROAD-DBG-T1] type=" + typeName
                    + " speed=" + speed + " (Sprint=0/Run=1/Walk=2/Sneak=3/Stand=4)"
                    + " up=" + up + " head=" + head + " angle=" + angle + " enabled=" + enabled
                    + " preMotion=" + _vel
                    + " jumpMotion=(" + sm.jumpMotionX + "," + sm.jumpMotionZ + ")"
                    + " yaw=" + String.format("%.1f", player.getYaw())
                    + " jumpMotionDeg=" + String.format("%.1f", _jumpMotionAngleDeg)
                    + " velDeg=" + String.format("%.1f", _velAngleDeg)
                    + " isFast=" + sm.isFast + " isRunning=" + isRunning);
        }

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
                    double factor = cfg.getHeadJumpFactor(sm.headJumpCharge);
                    double newAngle = factor * normalAngle;
                    double newVerticalMotion = totalMotion * Math.sin(newAngle);
                    double newHorizontalMotion = totalMotion * Math.cos(newAngle);
                    // [HEADBROAD-DBG-J3] head 재계산 — factor / normalAngle / newAngle 분석.
                    System.out.println("[HEADBROAD-DBG-J3] hJF=" + horizontalJumpFactor + " vJF=" + verticalJumpFactor
                            + " hM(pre)=" + String.format("%.4f", horizontalMotion)
                            + " vM(pre)=" + String.format("%.4f", verticalMotion)
                            + " normalDeg=" + String.format("%.2f", Math.toDegrees(normalAngle))
                            + " factor=" + factor
                            + " newDeg=" + String.format("%.2f", Math.toDegrees(newAngle))
                            + " newH=" + String.format("%.4f", newHorizontalMotion)
                            + " newV=" + String.format("%.4f", newVerticalMotion));
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
                // 🔴 fix (2026-05-08, 사용자 보고 "진입 시 카메라 확 올라감 + 착지 튕김"):
                //   이전 매핑: setPos(y+1m) + calculateDimensions() → player.y +1m 즉시 점프 +
                //     박스 갱신. 결과:
                //     (a) 진입 카메라 = vanilla getCameraPosVec lerp(prevY=old, y=old+1) 가 1 tick
                //         동안 +1m 보간 → "확 올라감" 시각 (1인칭/3인칭 모두).
                //     (b) 종료 시 player.y 가 +1m 잔존 → STANDING POSE 복원 시 박스 발 +1m 떠 있음
                //         → vanilla collision 으로 1m 떨어짐 → 박스 갑자기 커짐 + 튕김.
                //   원본 동작: setHeightOffset(-1F) 가 boundingBox.minY +=1m + height -=1m,
                //     posY 변경 X. 카메라 = posY + 1.62F (STANDING eyeHeight) = 변화 X. vy 만큼만
                //     부드럽게. 종료 resetHeightOffset 도 boundingBox 만 변경, posY 변경 X →
                //     박스 자연 복원.
                //   진짜 매핑: player.y 변화 X + dim eyeHeight=1.62F (STANDING 동일) 처리.
                //     박스 콜리전 발 위치는 STANDING 와 동일 (1m 차이 — 원본과 콜리전 차이는 추후
                //     calculateBoundingBox 가로채기로 별도 fix 필요 시).
                //   eyeHeight 1.62F 처리는 MixinPlayerEntityClient.sm_getBaseDimensions_client 에서
                //     isHeadJumping 별도 분기.
                // setPos / calculateDimensions 제거 — player.y 변화 X + 카메라 = player.y + 1.62F.
            }

            // === D-16 (원본 L2131-L2134) — setVelocity + isJumping ===
            player.setVelocity(motionX, noVertical ? vel.y : motionY, motionZ);
            // sp.isAirBorne = true — vanilla 자동 (velocityDirty + fallDistance)
            sm.isJumping = true;
            // onLivingJump() — vanilla PlayerEntity 자동 점프 이벤트 처리
        }

        // === D-18 제거 (Phase C 1:1 정정) — 호출 측 분리 처리로 원본 1:1 복원 ===
        //   기존: tryJump 끝에 jumpCharge=0 / headJumpCharge=0 / blockJumpTillButtonRelease=true /
        //         jumpPending=false 통합 set. 원본은 호출 측 (handleJumping 차지/헤드 release
        //         분기) 에서 분리 처리 — 다른 호출처 (wall/climb/angle/slide/water/CreativeFlying)
        //         에서는 set 안 함.
        //   부작용 (제거 전): blockJumpTillButtonRelease=true 가 모든 tryJump 후 set →
        //         wall/climb back/angle jump 후 점프 키 hold 중 일반 점프 가드 fail → 차이.
        //   복원 후: 원본 동작 정확히 일치. handleJumping 차지/헤드 분기 호출 측 reset 은
        //         이미 정상 매핑됨 (L413-L416 / L463-L468 / L419-L423 / L469-L473).
        //         jumpPending=false 는 handleJumping 시작 (L367) 에서 매 tick reset.

        // === D-17 (원본 L2135) — return enabled ===
        return enabled;
    }

    // ── [10-1] handleJumping ─────────────────────────────────────────────────

    /**
     * 점프 판정 진입점. 매 틱 sm_travel_client() HEAD에서 호출된다.
     * 원본: SmartMovingSelf.handleJumping() L1842-L1944.
     *
     * **포커스 #4 B-3 (세션 2 재작성)**: 사용자 인게임 보고 — "shift만 눌러도 차지 점프
     *   발동" → 7+ 결함 일괄 정정.
     *
     * 처리 순서 (원본 1:1):
     *   1. jumpPending = false (메서드 시작, 원본 L1844)
     *   2. blockJumpTillButtonRelease 해제 (jumpKey release 시, 원본 L1846)
     *   3. isSwimming/isDiving early return (원본 L1849)
     *   4. jumpMotionX/Z 저장 (원본 L1853-L1854)
     *   5. 차지 점프 (원본 L1856-L1878) — jumpKey hold 트리거 + wouldIsSneaking 게이트
     *   6. 헤드 점프 (원본 L1880-L1899) — jumpKey hold 트리거 + grab+sprint 게이트
     *   7. 수면 점프 (원본 L1901-L1913) — jumpKey hold + isDipping
     *   8. 일반 점프 (원본 L1915-L1916) — jumpAvoided + !isVineAnyClimbing
     *   9. 더블클릭 방향 점프 (원본 L1918-L1943) — leftJumpCount/rightJumpCount/backJumpCount
     */
    public static void handleJumping(ClientPlayerEntity player, SmartMovingClientState sm) {
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        // BUG-14 (세션 36): SM disabled 시 점프 처리 안 함 → vanilla 점프 정상 작동.
        //   기존: 가드 부재 → SM disabled 시에도 jumpPending 클리어 / jumpAvoided 세팅 등 →
        //   vanilla jump() 가로챔 + SM 점프도 작동 안 함 → 점프 자체 작동 안 함 (사용자 보고).
        //   해결: cfg.enabled false 시 즉시 return → vanilla 점프 그대로.
        if (!cfg.enabled) return;

        // B-7 (Phase B 1:1 정정): 원본 esp.movementInput.jump 매핑 — raw KeyBinding 대신
        //   player.input.jumping 사용. `feedback_movementInput_vs_isSneaking` 패턴 적용
        //   (vanilla 측 가공 후 값 — 비행/sleeping 등 시나리오 정합).
        boolean jumpKeyPressed  = player.input.jumping;
        boolean grabKeyPressed  = SmartMovingKeys.grab.isPressed();

        // 1. jumpPending 클리어 (원본 L1844 — 메서드 시작) ★ B-3 정정
        sm.jumpPending = false;

        // 2. blockJumpTillButtonRelease 해제 (원본 L1846-L1847)
        if (sm.blockJumpTillButtonRelease && !jumpKeyPressed) {
            sm.blockJumpTillButtonRelease = false;
        }

        // 3. isSwimming/isDiving early return (원본 L1849-L1850) ★ B-3 신규
        if (sm.isSwimming_sm || sm.isDiving) return;

        // 4. jump = jumpAvoided && isJumpingField && !isInWater && !isInLava (원본 L1852)
        //    + jumpMotionX/Z 저장 (원본 L1853-L1854)
        boolean jump = sm.jumpAvoided && sm.jumpPending  // sm_jump 가로채기 시 둘 다 set
                && !player.isTouchingWater() && !player.isInLava();
        // 단 sm.jumpPending 은 위 L1 에서 false 로 클리어됨 → 의미 차이.
        // 1.21.1 매핑: jumpAvoided 만 사용 (sm_jump 인터셉트 시 set, 매 tick 시작 false 초기화).
        jump = sm.jumpAvoided && !player.isTouchingWater() && !player.isInLava();
        {
            Vec3d cv = player.getVelocity();
            sm.jumpMotionX = cv.x;
            sm.jumpMotionZ = cv.z;
        }

        // 5. 차지 점프 (원본 L1856-L1878) ★ B-3 1:1 정정
        // 원본 식:
        //   isJumpChargingPossible = sp.onGround && isStanding
        //   isJumpCharging = isJumpChargingPossible && wouldIsSneaking
        //   actualJumpCharging = isJumpChargingPossible
        //                      && (!_jumpChargeCancelOnSneakRelease || wouldIsSneaking)
        //   if (actualJumpCharging)
        //     if (esp.movementInput.jump && (cancelOnSneak || wouldIsSneaking))  ★ jump 키 hold
        //         jumpCharge++
        //     else  if (jumpCharge > 0)  tryJump(ChargeUp); jumpCharge = 0;
        //   else  if (jumpCharge > 0) blockJumpTillButtonRelease = true; jumpCharge = 0;
        boolean isJumpCharging = false;
        if (cfg.jumpCharge) {
            boolean isJumpChargingPossible = player.isOnGround() && sm.isStanding;
            isJumpCharging = isJumpChargingPossible && sm.wouldIsSneaking;

            boolean actualJumpCharging = isJumpChargingPossible
                    && (!cfg.jumpChargeCancelOnSneakRelease || sm.wouldIsSneaking);
            if (actualJumpCharging) {
                if (jumpKeyPressed
                        && (cfg.jumpChargeCancelOnSneakRelease || sm.wouldIsSneaking)) {
                    sm.jumpCharge = Math.min(sm.jumpCharge + 1F, cfg.jumpChargeMaximum);
                } else {
                    if (sm.jumpCharge > 0) {
                        tryJump(player, sm, CHARGE_UP, null, null, null);
                    }
                    sm.jumpCharge = 0;
                }
            } else {
                if (sm.jumpCharge > 0) {
                    sm.blockJumpTillButtonRelease = true;
                }
                sm.jumpCharge = 0;
            }
        }

        // 6. 헤드 점프 (원본 L1880-L1899) ★ B-3 1:1 정정 — jumpKey 트리거
        // 원본 식:
        //   isHeadJumpCharging = grabButton.Pressed && (isGroundSprinting || isSprintJump
        //                       || (isRunning() && sp.onGround)) && !isCrawling
        //   if (isHeadJumpCharging)
        //     if (esp.movementInput.jump)         ★ jump 키 hold
        //         headJumpCharge++
        //     else  if (headJumpCharge > 0 && sp.onGround) tryJump(HeadUp); headJumpCharge = 0;
        //   else  if (headJumpCharge > 0) blockJumpTillButtonRelease = true; headJumpCharge = 0;
        //
        // B-2 / B-3 (Phase B 1:1 정정): isGroundSprinting/isRunning 로컬 재정의 제거.
        //   기존: 로컬 변수로 잘못된 식 재정의 (isGroundSprinting 은 원본
        //         isStandupSprintingOrRunning 식 + isRunning 은 vanilla() 대신 isFlying 잘못 사용).
        //   원본은 isGroundSprinting **필드** (L2679 set: canHorizontallySprint
        //   && (onGround||isLevitating) && !swimming && !diving && !climbing) +
        //   isRunning() **메서드** (L3239: isSprinting && !isFast && (onGround||vanilla())).
        //   1.21.1 측 sm.isGroundSprinting / sm.isRunning(player) 매핑 이미 존재 — 직접 사용.
        //
        // B-Slide-HeadJump-fix (2026-05-04): `!sm.isSliding` 가드 유지 — 사용자 보고 BUG fix.
        //   원본은 `!isCrawling` 만 명시 가드. 1.21.1 vanilla sprint 자동 종료 동작 차이로
        //   슬라이딩 후 forward hold 시 isRunning 잔존 → 헤드점프 차징 활성 BUG 발생.
        //   본 가드는 의도적 원본 1:1 위반.
        boolean isHeadJumpCharging = false;
        if (cfg.headJump) {
            // 🔴 fix #37 revert (2026-05-08, 사용자 의도 — 두 점프 누적 방향 유지):
            //   사용자 인정: fix #33 시점 (= 가드 제거 + 두 점프 누적) 이 원본과 가장 비슷.
            //   잔존 증상 (튕김/박힘/낮은 점프) 만 별도 fix 방향. 가드 다시 제거.
            isHeadJumpCharging = grabKeyPressed
                    && (sm.isGroundSprinting || sm.isSprintJump
                        || (sm.isRunning(player) && player.isOnGround()))
                    && !sm.isCrawling;
            if (isHeadJumpCharging) {
                if (jumpKeyPressed) {
                    // B-3 (Phase B 1:1 정정): 원본 L1886 `headJumpCharge++` — clamp 없음.
                    //   getHeadJumpFactor 안에서만 clamp. set 단계 Math.min 제거.
                    sm.headJumpCharge++;
                } else {
                    if (sm.headJumpCharge > 0 && player.isOnGround()) {
                        // [HEADBROAD-DBG-HJ-FIRE] HEAD_UP 발사 시점 dump — 거리/방향 분석용.
                        long _hjTick = (player.getWorld() != null) ? player.getWorld().getTime() : -1L;
                        net.minecraft.util.math.Vec3d _preHj = player.getVelocity();
                        double _preHjAngle = (_preHj.x == 0 && _preHj.z == 0) ? 0
                                : Math.toDegrees(Math.atan2(-_preHj.x, _preHj.z));
                        // 발사 시점 위치 + yaw + pitch + 사용자 입력 + 누적 ADD 초기화.
                        sm.dbgHeadJumpStartX = player.getX();
                        sm.dbgHeadJumpStartZ = player.getZ();
                        sm.dbgHeadJumpStartY = player.getY();
                        sm.dbgHeadJumpStartYaw = player.getYaw();
                        sm.dbgHeadJumpStartPitch = player.getPitch();
                        sm.dbgHeadJumpStartTick = _hjTick;
                        sm.dbgHeadJumpMaxY = player.getY();
                        sm.dbgHeadJumpWasFox = false;
                        sm.dbgHeadJumpAccumAddH = 0.0;
                        sm.dbgHeadJumpAddCount = 0;
                        sm.dbgHeadJumpWPressed = player.input.movementForward > 0F;
                        sm.dbgHeadJumpSPressed = player.input.movementForward < 0F;
                        System.out.println("[HEADBROAD-DBG-HJ-FIRE] tick=" + _hjTick
                                + " charge=" + sm.headJumpCharge
                                + " preMotion=" + _preHj
                                + " preDeg=" + String.format("%.1f", _preHjAngle)
                                + " yaw=" + String.format("%.1f", player.getYaw())
                                + " pitch=" + String.format("%.1f", player.getPitch())
                                + " W=" + sm.dbgHeadJumpWPressed
                                + " S=" + sm.dbgHeadJumpSPressed
                                + " strafe=" + player.input.movementSideways
                                + " pos=(" + String.format("%.3f,%.3f,%.3f", player.getX(), player.getY(), player.getZ())
                                + ") isFast=" + sm.isFast);
                        tryJump(player, sm, HEAD_UP, null, null, null);
                        net.minecraft.util.math.Vec3d _postHj = player.getVelocity();
                        double _postHjAngle = Math.toDegrees(Math.atan2(-_postHj.x, _postHj.z));
                        double _postHjHorizontal = Math.sqrt(_postHj.x * _postHj.x + _postHj.z * _postHj.z);
                        System.out.println("[HEADBROAD-DBG-HJ-POST] tick=" + _hjTick
                                + " postMotion=" + _postHj
                                + " postDeg=" + String.format("%.1f", _postHjAngle)
                                + " postH=" + String.format("%.4f", _postHjHorizontal)
                                + " yaw=" + String.format("%.1f", player.getYaw())
                                + " yaw-postDeg=" + String.format("%.1f", player.getYaw() - _postHjAngle));
                    }
                    sm.headJumpCharge = 0;
                }
            } else {
                if (sm.headJumpCharge > 0) {
                    sm.blockJumpTillButtonRelease = true;
                }
                sm.headJumpCharge = 0;
            }
        }

        // 7. 수면 점프 (원본 L1901-L1913) ★ B-3 1:1 정정 — jumpKey hold 트리거
        // 원본 식:
        //   if (esp.movementInput.jump && sp.isInWater() && isDipping)
        //     if (posY - floor(posY) > (isSlow ? 0.37 : 0.6))
        //       sp.motionY -= 0.04F
        //       if (!isStillSwimmingJump && sp.onGround && jumpCharge == 0)
        //         tryJump(Up, true, null, null) → splash sound
        if (jumpKeyPressed && player.isTouchingWater() && sm.isDipping) {
            double frac = player.getY() - Math.floor(player.getY());
            double threshold = sm.isSlow ? 0.37D : 0.6D;
            if (frac > threshold) {
                Vec3d vel = player.getVelocity();
                player.setVelocity(vel.x, vel.y - 0.04D, vel.z);
                if (!sm.isStillSwimmingJump && player.isOnGround() && sm.jumpCharge == 0) {
                    tryJump(player, sm, UP, Boolean.TRUE, null, null);
                    // 원본 L1910 splash sound 는 1.21.1 미이식 (효과음만, 게임플레이 무관)
                }
            }
        }

        // 8. 일반 점프 (원본 L1915-L1916) ★ B-3 1:1 정정 — !isVineAnyClimbing 추가
        if (jump && !sm.blockJumpTillButtonRelease && !isJumpCharging && !isHeadJumpCharging
                && !sm.isVineAnyClimbing) {
            tryJump(player, sm, UP, Boolean.FALSE, null, null);
        }

        // 9. 더블클릭 방향 점프 (원본 L1918-L1943)
        // [IMPL-03] 더블클릭 leftJumpCount/rightJumpCount/backJumpCount == -1 시 발동.
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

                    sm.angleJumpType = ((360 - relAngle) / 45) % 8;

                    float worldAngleDeg = (float) ((player.getYaw() + relAngle) % 360.0);
                    if (worldAngleDeg < 0F) worldAngleDeg += 360F;

                    tryJump(player, sm, ANGLE, null, null, worldAngleDeg);
                }

                sm.leftJumpCount  = 0;
                sm.rightJumpCount = 0;
                sm.backJumpCount  = 0;
            }
        }
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

        // 원본 L1948: 최우선 조건 — wantWallJumping=false 이면 즉시 return.
        if (!sm.wantWallJumping) return;

        // calculateSeparateCollisionAngle 의 fallback 용 movementAngle (vel 기반).
        // 원본은 horizontalCollisionAngle 필드를 별도 계산해 NaN 시 함수 시작에서 return.
        // 1.21.1 매핑은 calculateSeparateCollisionAngle 안에서 NaN fallback 처리하므로
        // 여기 fallback movementAngle 만 vel 기반 유지.
        Vec3d vel = player.getVelocity();
        float fallbackAngle = (float) Math.toDegrees(Math.atan2(-vel.x, vel.z));
        if (fallbackAngle < 0) fallbackAngle += 360F;
        float horizontalCollisionAngle = calculateSeparateCollisionAngle(player, fallbackAngle);

        // 원본 L1952-L1963: grab=true → WallHead/WallHeadSlide, grab=false → WallUp/WallUpSlide.
        // wasCollidedHorizontally: 이전 틱부터 벽에 닿아있던 경우 Slide 타입 (수직 속도 미적용).
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

        // 원본 L1965-L1975: wasCollidedHorizontally=false → 반사 각도; true → 벽 법선 각도 그대로.
        // Phase G 차이 1 1:1 정정: movementAngle 을 jumpMotion 기반으로 (원본 L1968 — 점프 시점
        //   motion 보존). 기존 vel 기반은 같은 tick 내 motion 변동 영향.
        float jumpAngle;
        if (!sm.wasCollidedHorizontally) {
            float movementAngle = (float) Math.toDegrees(Math.atan2(-sm.jumpMotionX, sm.jumpMotionZ));
            if (movementAngle < 0) movementAngle += 360F;
            // 원본 L1969-L1970 NaN 가드 (jumpMotion 둘 다 0 시 atan2(0,0)=0 → NaN 안 발생하나 1:1)
            if (Float.isNaN(movementAngle)) return;
            jumpAngle = horizontalCollisionAngle * 2 - movementAngle + 180F;
        } else {
            jumpAngle = horizontalCollisionAngle;
        }

        // Phase G BUG-1 1:1 정정: while>360 + orthogonalTolerance 분기 외부 적용 (원본 L1977-L1988).
        //   기존: !wasCollidedHorizontally 분기 안에만 적용 → wasCollidedHorizontally=true (Slide 타입)
        //         시 90° 정렬 미적용 → 점프 각도 부정확. 원본은 둘 다 적용.
        while (jumpAngle > 360F) jumpAngle -= 360F;
        if (cfg.wallUpJumpOrthogonalTolerance != 0F) {
            float aligned = jumpAngle;
            while (aligned > 45F) aligned -= 90F;
            if (Math.abs(aligned) < cfg.wallUpJumpOrthogonalTolerance)
                jumpAngle = Math.round(jumpAngle / 90F) * 90F;
        }

        // Phase G 차이 2/3 1:1 정정: tryJump 결과 if 분기 + 후처리는 성공 시에만 (원본 L1990-L1996).
        //   기존: tryJump 결과 무시 + isWallJumping 이중 set + tryJump 호출 전 후처리.
        if (tryJump(player, sm, jumpType, null, null, jumpAngle)) {
            // 원본 L1992: continueWallJumping = !isHeadJumping (WallHead 시 false)
            sm.continueWallJumping = !sm.isHeadJumping;
            // 원본 L1993: sp.isCollidedHorizontally = false
            player.horizontalCollision = false;
            // 원본 L1994: sp.rotationYaw = jumpAngle
            player.setYaw(jumpAngle);
            player.bodyYaw = jumpAngle;       // (Phase G 차이 4 보류 — 회전 동기화 의도 유지)
            // 원본 L1995 onStartWallJump(jumpAngle) inline:
            //   - prev rotateAngleY = angle / RadiantToAngle  (Phase G 차이 5 보류 — 애니메이션 사이클)
            sm.isWallJumping = true;          // 원본 onStartWallJump (SmartMoving.java L154)
            player.fallDistance = 0F;          // 원본 onStartWallJump (SmartMoving.java L155)
        }
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
