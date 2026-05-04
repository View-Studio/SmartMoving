package choco.ratel.smartmoving.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * SmartMoving 설정 파일 로드/저장.
 * 원본 net.smart.properties 시스템을 java.util.Properties로 단순화.
 * 설정 파일: config/smart_moving_options.properties
 */
public class SmartMovingConfig {

    // ── 버전 (config_system.md: SmartMovingConfig._sm_current = "3.2" → 1.21.1 포트는 "1.0") ──
    public static final String SM_VERSION = "1.0";

    // ═══════════════════════════════════════════════════════════════════════════
    // Phase B (포커스 #2.5) — Jumper 판정 헬퍼 정적 인프라
    // 원본 SmartMovingClientConfig L172-L192 (Speed/Type 상수) +
    //      SmartMovingSelf L2148-L2163 (getJumpSpeed 헬퍼).
    // ═══════════════════════════════════════════════════════════════════════════

    // === Speed 상수 (원본 SmartMovingClientConfig L172-L176, B-2) ===
    //   getJumpSpeed 반환 + isJumpingEnabled / getJumpHorizontalFactor /
    //   getJumpVerticalFactor / getMaxHorizontalMotion 의 speed 파라미터 도메인.
    public static final int SPEED_SPRINTING = 0;
    public static final int SPEED_RUNNING   = 1;
    public static final int SPEED_WALKING   = 2;
    public static final int SPEED_SNEAKING  = 3;
    public static final int SPEED_STANDING  = 4;

    // === Jump Type 상수 (원본 SmartMovingClientConfig L178-L192, B-2.5) ===
    //   tryJump / isJumpingEnabled / getJumpHorizontalFactor / getJumpVerticalFactor /
    //   getMaxHorizontalMotion / getHeadJumpFactor 의 type 파라미터 도메인 (15종).
    //   주: 1.21.1 SmartMovingJumper 의 기존 jumpType 상수와 매핑 정렬은 Phase E 에서 처리.
    public static final int JUMP_TYPE_UP                     = 0;
    public static final int JUMP_TYPE_CHARGE_UP              = 1;
    public static final int JUMP_TYPE_ANGLE                  = 2;
    public static final int JUMP_TYPE_HEAD_UP                = 3;
    public static final int JUMP_TYPE_SLIDE_DOWN             = 4;
    public static final int JUMP_TYPE_CLIMB_UP               = 5;
    public static final int JUMP_TYPE_CLIMB_UP_HANDS_ONLY    = 6;
    public static final int JUMP_TYPE_CLIMB_BACK_UP          = 7;
    public static final int JUMP_TYPE_CLIMB_BACK_UP_HANDS_ONLY = 8;
    public static final int JUMP_TYPE_CLIMB_BACK_HEAD        = 9;
    public static final int JUMP_TYPE_CLIMB_BACK_HEAD_HANDS_ONLY = 10;
    public static final int JUMP_TYPE_WALL_UP                = 11;
    public static final int JUMP_TYPE_WALL_HEAD              = 12;
    public static final int JUMP_TYPE_WALL_UP_SLIDE          = 13;
    public static final int JUMP_TYPE_WALL_HEAD_SLIDE        = 14;

    /**
     * Phase B-1 — getJumpSpeed 헬퍼.
     *
     * 원본 `SmartMovingSelf.java` L2148-L2163 (1:1 번역):
     * <pre>
     * private static int getJumpSpeed(boolean isStanding, boolean isSneaking,
     *                                 boolean isRunning, boolean isSprinting, Float angle) {
     *     isSprinting &amp;= angle == null;
     *     isRunning   &amp;= angle == null;
     *     if (isSprinting)      return Config.Sprinting;
     *     else if (isRunning)   return Config.Running;
     *     else if (isSneaking)  return Config.Sneaking;
     *     else if (isStanding)  return Config.Standing;
     *     else                  return Config.Walking;
     * }
     * </pre>
     *
     * angle != null (사이드/백 점프) 일 때 sprint/run 게이팅을 강제로 끄고 sneaking/standing
     * /walking 로만 판정 — 원본 의도: angle 점프는 항상 base speed 로 분류.
     *
     * 호출처: tryJump (Phase D-4) — `int speed = getJumpSpeed(sm.isStanding, sm.isSlow,
     *                                                          isRunning, sm.isFast, angle);`
     */
    public static int getJumpSpeed(boolean isStanding, boolean isSneaking,
                                    boolean isRunning, boolean isSprinting, Float angle) {
        isSprinting &= angle == null;
        isRunning   &= angle == null;

        if (isSprinting)      return SPEED_SPRINTING;
        else if (isRunning)   return SPEED_RUNNING;
        else if (isSneaking)  return SPEED_SNEAKING;
        else if (isStanding)  return SPEED_STANDING;
        else                  return SPEED_WALKING;
    }

    /**
     * Phase B-3 — isJumpingEnabled(speed, type).
     *
     * 원본 `SmartMovingClientConfig.java` L194-L227 (1:1 번역):
     * <pre>
     * public boolean isJumpingEnabled(int speed, int type) {
     *     if (!enabled)                                  return true;   // SM 비활성 = vanilla 동작 = 모두 허용
     *     if (type == ChargeUp)                          return _jumpCharge.value;
     *     if (type == SlideDown)                         return _slide.value;
     *     if (type == ClimbUp || type == ClimbUpHandsOnly)             return _climbUpJump.value;
     *     if (type == ClimbBackUp || type == ClimbBackUpHandsOnly)     return _climbBackUpJump.value;
     *     if (type == ClimbBackHead || type == ClimbBackHeadHandsOnly) return _climbBackHeadJump.value;
     *     if (type == WallUp)                            return _wallUpJump.value;
     *     if (type == WallHead)                          return _wallHeadJump.value;
     *     if (speed == Sprinting)                        return _sprintJump.value;
     *     else if (speed == Running)                     return _runJump.value;
     *     else if (speed == Walking)                     return _walkJump.value;
     *     else if (speed == Sneaking)                    return _sneakJump.value;
     *     else if (speed == Standing)                    return _standJump.value;
     *     return true;   // type=Up/HeadUp/Angle/WallUpSlide/WallHeadSlide & speed 분기 미통과 시
     * }
     * </pre>
     *
     * 원본 의도: !enabled (SM 비활성) → 모든 점프 vanilla 위임 (true). type 우선 분기 →
     * speed 분기 → fallthrough true.
     *
     * 호출처: tryJump (Phase D-4) — `boolean enabled = cfg.isJumpingEnabled(speed, type);`
     */
    public boolean isJumpingEnabled(int speed, int type) {
        if (!enabled)
            return true;

        if (type == JUMP_TYPE_CHARGE_UP)
            return jumpCharge;
        if (type == JUMP_TYPE_SLIDE_DOWN)
            return slide;
        if (type == JUMP_TYPE_CLIMB_UP || type == JUMP_TYPE_CLIMB_UP_HANDS_ONLY)
            return climbUpJump;
        if (type == JUMP_TYPE_CLIMB_BACK_UP || type == JUMP_TYPE_CLIMB_BACK_UP_HANDS_ONLY)
            return climbBackUpJump;
        if (type == JUMP_TYPE_CLIMB_BACK_HEAD || type == JUMP_TYPE_CLIMB_BACK_HEAD_HANDS_ONLY)
            return climbBackHeadJump;

        if (type == JUMP_TYPE_WALL_UP)
            return wallUpJump;
        if (type == JUMP_TYPE_WALL_HEAD)
            return wallHeadJump;

        if (speed == SPEED_SPRINTING)
            return sprintJump;
        else if (speed == SPEED_RUNNING)
            return runJump;
        else if (speed == SPEED_WALKING)
            return walkJump;
        else if (speed == SPEED_SNEAKING)
            return sneakJump;
        else if (speed == SPEED_STANDING)
            return standJump;

        return true;
    }

    /**
     * Phase B-6 — getMaxHorizontalMotion(speed, type, inWater).
     *
     * 원본 `SmartMovingClientConfig.java` L508-L525 (1:1 번역):
     * <pre>
     * public float getMaxHorizontalMotion(int speed, int type, boolean inWater) {
     *     float maxMotion = 0.117852041920949F;   // 육상 base (vanilla 기본 수평 속도 한계)
     *     if (!enabled) return speed == Running ? maxMotion * 1.3F : maxMotion;
     *     if (inWater) maxMotion = 0.07839602977037292F;   // 수중 덮어쓰기
     *     if (speed == Sprinting)  maxMotion *= _sprintFactor.value;
     *     else if (speed == Running)   maxMotion *= _runFactor.value;
     *     else if (speed == Sneaking)  maxMotion *= _sneakFactor.value;
     *     return maxMotion;   // Walking/Standing 분기 없음 — base 그대로
     * }
     * </pre>
     *
     * @param type 사용되지 않음 (원본도 `@SuppressWarnings("unused")` — 시그니처 호환만 유지).
     *
     * 호출처: tryJump (Phase D-8) — `maxHorizontalMotion = (double) cfg.getMaxHorizontalMotion(speed, type, inWater) * SmartMovingMover.getCombinedSpeedFactor(player, cfg);`
     */
    @SuppressWarnings("unused")
    public float getMaxHorizontalMotion(int speed, int type, boolean inWater) {
        float maxMotion = 0.117852041920949F;
        if (!enabled)
            return speed == SPEED_RUNNING ? maxMotion * 1.3F : maxMotion;

        if (inWater)
            maxMotion = 0.07839602977037292F;

        if (speed == SPEED_SPRINTING)
            maxMotion *= sprintFactor;
        else if (speed == SPEED_RUNNING)
            maxMotion *= runFactor;
        else if (speed == SPEED_SNEAKING)
            maxMotion *= sneakFactor;

        return maxMotion;
    }

    /**
     * Phase B-7 — getJumpChargeFactor(jumpCharge).
     *
     * 원본 `SmartMovingClientConfig.java` L401-L408 (1:1 번역):
     * <pre>
     * public float getJumpChargeFactor(float jumpCharge) {
     *     if (!enabled || !_jumpCharge.value) return 1F;
     *     jumpCharge = Math.min(jumpCharge, _jumpChargeMaximum.value);
     *     return 1F + jumpCharge / _jumpChargeMaximum.value * (_jumpChargeFactor.value - 1F);
     * }
     * </pre>
     *
     * 공식: charge=0 → 1F, charge=max → jumpChargeFactor (1.3F 기본). 선형 보간.
     *
     * 호출처: tryJump (Phase D-6) — `float jumpChargeFactor = charged ? cfg.getJumpChargeFactor(sm.jumpCharge) : 1F;`
     */
    public float getJumpChargeFactor(float jumpCharge) {
        if (!enabled || !this.jumpCharge)
            return 1F;

        jumpCharge = Math.min(jumpCharge, jumpChargeMaximum);
        return 1F + jumpCharge / jumpChargeMaximum * (jumpChargeFactor - 1F);
    }

    /**
     * Phase B-8 — getHeadJumpFactor(headJumpCharge).
     *
     * 원본 `SmartMovingClientConfig.java` L410-L416 (1:1 번역):
     * <pre>
     * public float getHeadJumpFactor(float headJumpCharge) {
     *     if (!enabled || !_headJump.value) return 1F;
     *     headJumpCharge = Math.min(headJumpCharge, _headJumpChargeMaximum.value);
     *     return (headJumpCharge - 1) / (_headJumpChargeMaximum.value - 1);
     * }
     * </pre>
     *
     * 공식: charge=1 → 0, charge=max → 1. 헤드점프의 normalAngle 회전 비율 (Phase D-10).
     *
     * 주의: charge=0 일 때 결과 -1 / (max-1) (음수). 호출 측 (Phase D-10) 에서 head 분기 진입
     * 전 이미 charge >= 1 보장된다고 가정 (원본 그대로).
     *
     * 호출처: tryJump head 재계산 (Phase D-10) — `double newAngle = cfg.getHeadJumpFactor(sm.headJumpCharge) * normalAngle;`
     */
    public float getHeadJumpFactor(float headJumpCharge) {
        if (!enabled || !this.headJump)
            return 1F;

        headJumpCharge = Math.min(headJumpCharge, headJumpChargeMaximum);
        return (headJumpCharge - 1) / (headJumpChargeMaximum - 1);
    }

    /**
     * Phase B-5 — getJumpVerticalFactor(speed, type).
     *
     * 원본 `SmartMovingClientConfig.java` L418-L463 (1:1 번역):
     * <pre>
     * public float getJumpVerticalFactor(int speed, int type) {
     *     if (!enabled) return 1F;
     *     float result = _jumpVerticalFactor.value;
     *     if (type == Angle)                                              return result * _angleJumpVerticalFactor.value;   // 즉시 early return
     *     if (type == ClimbUp || type == ClimbUpHandsOnly)                result *= _climbUpJumpVerticalFactor.value;
     *     if (type == ClimbUpHandsOnly)                                   result *= _climbUpJumpHandsOnlyVerticalFactor.value;
     *     if (type == ClimbBackUp || type == ClimbBackUpHandsOnly)        result *= _climbBackUpJumpVerticalFactor.value;
     *     if (type == ClimbBackUpHandsOnly)                               result *= _climbBackUpJumpHandsOnlyVerticalFactor.value;
     *     if (type == ClimbBackHead || type == ClimbBackHeadHandsOnly)    result *= _climbBackHeadJumpVerticalFactor.value;
     *     if (type == ClimbBackHeadHandsOnly)                             result *= _climbBackHeadJumpHandsOnlyVerticalFactor.value;
     *     if (type == WallUp || type == WallHead)                         result *= _wallUpJumpVerticalFactor.value;        // ※ 둘 다 wallUp 사용
     *     if (type == WallHead)                                           result *= _wallHeadJumpVerticalFactor.value;       // ※ WallHead = base × wallUp × wallHead 누적
     *     if (type == Angle || type == ClimbUp || ... || type == WallHead) return result;   // type 매칭 시 speed 분기 skip
     *     if (speed == Sprinting)  result *= _sprintJumpVerticalFactor.value;
     *     else if (speed == Running)  result *= _runJumpVerticalFactor.value;
     *     else if (speed == Walking)  result *= _walkJumpVerticalFactor.value;
     *     else if (speed == Sneaking) result *= _sneakJumpVerticalFactor.value;
     *     else if (speed == Standing) result *= _standJumpVerticalFactor.value;
     *     return result;
     * }
     * </pre>
     *
     * 호출처: tryJump (Phase D-6) — `float verticalJumpFactor = cfg.getJumpVerticalFactor(speed, type) * jumpFactor;`
     */
    public float getJumpVerticalFactor(int speed, int type) {
        if (!enabled)
            return 1F;

        float result = jumpVerticalFactor;

        if (type == JUMP_TYPE_ANGLE)
            return result * angleJumpVerticalFactor;

        if (type == JUMP_TYPE_CLIMB_UP || type == JUMP_TYPE_CLIMB_UP_HANDS_ONLY)
            result *= climbUpJumpVerticalFactor;
        if (type == JUMP_TYPE_CLIMB_UP_HANDS_ONLY)
            result *= climbUpJumpHandsOnlyVerticalFactor;

        if (type == JUMP_TYPE_CLIMB_BACK_UP || type == JUMP_TYPE_CLIMB_BACK_UP_HANDS_ONLY)
            result *= climbBackUpJumpVerticalFactor;
        if (type == JUMP_TYPE_CLIMB_BACK_UP_HANDS_ONLY)
            result *= climbBackUpJumpHandsOnlyVerticalFactor;

        if (type == JUMP_TYPE_CLIMB_BACK_HEAD || type == JUMP_TYPE_CLIMB_BACK_HEAD_HANDS_ONLY)
            result *= climbBackHeadJumpVerticalFactor;
        if (type == JUMP_TYPE_CLIMB_BACK_HEAD_HANDS_ONLY)
            result *= climbBackHeadJumpHandsOnlyVerticalFactor;

        if (type == JUMP_TYPE_WALL_UP || type == JUMP_TYPE_WALL_HEAD)
            result *= wallUpJumpVerticalFactor;
        if (type == JUMP_TYPE_WALL_HEAD)
            result *= wallHeadJumpVerticalFactor;

        if (type == JUMP_TYPE_ANGLE
            || type == JUMP_TYPE_CLIMB_UP || type == JUMP_TYPE_CLIMB_UP_HANDS_ONLY
            || type == JUMP_TYPE_CLIMB_BACK_UP || type == JUMP_TYPE_CLIMB_BACK_UP_HANDS_ONLY
            || type == JUMP_TYPE_CLIMB_BACK_HEAD || type == JUMP_TYPE_CLIMB_BACK_HEAD_HANDS_ONLY
            || type == JUMP_TYPE_WALL_UP || type == JUMP_TYPE_WALL_HEAD)
            return result;

        if (speed == SPEED_SPRINTING)
            result *= sprintJumpVerticalFactor;
        else if (speed == SPEED_RUNNING)
            result *= runJumpVerticalFactor;
        else if (speed == SPEED_WALKING)
            result *= walkJumpVerticalFactor;
        else if (speed == SPEED_SNEAKING)
            result *= sneakJumpVerticalFactor;
        else if (speed == SPEED_STANDING)
            result *= standJumpVerticalFactor;

        return result;
    }

    /**
     * Phase B-4 — getJumpHorizontalFactor(speed, type).
     *
     * 원본 `SmartMovingClientConfig.java` L465-L505 (1:1 번역):
     * <pre>
     * public float getJumpHorizontalFactor(int speed, int type) {
     *     if (!enabled) return speed == Running ? 2F : 1F;
     *
     *     float result = _jumpHorizontalFactor.value;
     *
     *     if (type == Angle)                                                 result *= _angleJumpHorizontalFactor.value;
     *
     *     if (type == ClimbBackUp || type == ClimbBackUpHandsOnly)           result *= _climbBackUpJumpHorizontalFactor.value;
     *     if (type == ClimbBackUpHandsOnly)                                  result *= _climbBackUpJumpHandsOnlyHorizontalFactor.value;
     *
     *     if (type == ClimbBackHead || type == ClimbBackHeadHandsOnly)       result *= _climbBackHeadJumpHorizontalFactor.value;
     *     if (type == ClimbBackHeadHandsOnly)                                result *= _climbBackHeadJumpHandsOnlyHorizontalFactor.value;
     *
     *     if (type == WallUp)                                                result *= _wallUpJumpHorizontalFactor.value;
     *     if (type == WallHead)                                              result *= _wallHeadJumpHorizontalFactor.value;
     *
     *     if (type == Angle || type == ClimbUp || type == ClimbUpHandsOnly
     *         || type == ClimbBackUp || type == ClimbBackUpHandsOnly
     *         || type == ClimbBackHead || type == ClimbBackHeadHandsOnly
     *         || type == WallUp || type == WallHead)
     *         return result;   // type 매칭 시 speed 분기 skip (early return)
     *
     *     if (speed == Sprinting)                                            result *= _sprintJumpHorizontalFactor.value;
     *     else if (speed == Running)                                         result *= _runJumpHorizontalFactor.value;
     *     else if (speed == Walking)                                         result *= _walkJumpHorizontalFactor.value;
     *     else if (speed == Sneaking)                                        result *= _sneakJumpHorizontalFactor.value;
     *     else if (speed == Standing && type != ClimbBackUp && type != ClimbBackUpHandsOnly
     *                                && type != ClimbBackHead && type != ClimbBackHeadHandsOnly)
     *         result *= 0F;   // L501 — Standing 점프는 수평 0 (Climb 4종은 위에서 early return)
     *     return result;
     * }
     * </pre>
     *
     * 호출처: tryJump (Phase D-6) — `float horizontalJumpFactor = cfg.getJumpHorizontalFactor(speed, type) * jumpFactor;`
     */
    public float getJumpHorizontalFactor(int speed, int type) {
        if (!enabled)
            return speed == SPEED_RUNNING ? 2F : 1F;

        float result = jumpHorizontalFactor;

        if (type == JUMP_TYPE_ANGLE)
            result *= angleJumpHorizontalFactor;

        if (type == JUMP_TYPE_CLIMB_BACK_UP || type == JUMP_TYPE_CLIMB_BACK_UP_HANDS_ONLY)
            result *= climbBackUpJumpHorizontalFactor;
        if (type == JUMP_TYPE_CLIMB_BACK_UP_HANDS_ONLY)
            result *= climbBackUpJumpHandsOnlyHorizontalFactor;

        if (type == JUMP_TYPE_CLIMB_BACK_HEAD || type == JUMP_TYPE_CLIMB_BACK_HEAD_HANDS_ONLY)
            result *= climbBackHeadJumpHorizontalFactor;
        if (type == JUMP_TYPE_CLIMB_BACK_HEAD_HANDS_ONLY)
            result *= climbBackHeadJumpHandsOnlyHorizontalFactor;

        if (type == JUMP_TYPE_WALL_UP)
            result *= wallUpJumpHorizontalFactor;
        if (type == JUMP_TYPE_WALL_HEAD)
            result *= wallHeadJumpHorizontalFactor;

        if (type == JUMP_TYPE_ANGLE
            || type == JUMP_TYPE_CLIMB_UP || type == JUMP_TYPE_CLIMB_UP_HANDS_ONLY
            || type == JUMP_TYPE_CLIMB_BACK_UP || type == JUMP_TYPE_CLIMB_BACK_UP_HANDS_ONLY
            || type == JUMP_TYPE_CLIMB_BACK_HEAD || type == JUMP_TYPE_CLIMB_BACK_HEAD_HANDS_ONLY
            || type == JUMP_TYPE_WALL_UP || type == JUMP_TYPE_WALL_HEAD)
            return result;

        if (speed == SPEED_SPRINTING)
            result *= sprintJumpHorizontalFactor;
        else if (speed == SPEED_RUNNING)
            result *= runJumpHorizontalFactor;
        else if (speed == SPEED_WALKING)
            result *= walkJumpHorizontalFactor;
        else if (speed == SPEED_SNEAKING)
            result *= sneakJumpHorizontalFactor;
        else if (speed == SPEED_STANDING
                 && type != JUMP_TYPE_CLIMB_BACK_UP && type != JUMP_TYPE_CLIMB_BACK_UP_HANDS_ONLY
                 && type != JUMP_TYPE_CLIMB_BACK_HEAD && type != JUMP_TYPE_CLIMB_BACK_HEAD_HANDS_ONLY)
            result *= 0F;

        return result;
    }


    // ── 서버 배포 제어 플래그 (config_system.md 2-1, 5-3) ───────────
    /** true → 접속한 모든 클라이언트에게 이 설정을 강제 적용. */
    public boolean globalConfig = false;
    /** true → 플레이어별 개인 설정 관리 활성화 (현재 미구현 — globalConfig 우선). */
    public boolean serverConfig = false;

    // ── Global Speed ────────────────────────────────────────────
    public float speedFactor = 1F;
    /**
     * 원본 `_speedUser` L96 = `Creative("move.speed.user").defaults(true, _pre_sm_3_2)`.
     * `Creative(key)` 팩토리 = `Modified(key).defaults(Value(false).c(true))`.
     *   → 난이도별: default=false, Easy=false, Medium=false, Hard=false, Creative=true.
     *   → SM 3.2 이후: Creative 에서만 true, 나머지(Easy 포함) 전부 false.
     * 버전 폴백 `_pre_sm_3_2`: SM 3.1 이하에서는 전 난이도 true 고정.
     * Easy 1:1 → **false** (세션 17 정정, 기존 `true` 는 오역).
     * 소비처: `getUserSpeedFactor()` L580 — `!speedUser || speedUserFactor==1F || exponent==0`
     *   조기 반환 1F. Easy=false 면 사용자 속도 조정 기능 전체 OFF.
     */
    public boolean speedUser = false;
    public float speedUserFactor = 0.2F;
    public int speedUserExponent = 0;
    /**
     * 원본: Property<Map<String,Integer>> _speedUsersExponents = IntegerMap("move.speed.users.exponents").singular();
     * 플레이어별 속도 지수 맵. username → exponent.
     * 파일 저장 형식: 원본 Value.tryParseIntegerMap() 대응 — CSV `"user1,5,user2,-2,..."`.
     * toArray(username) 시 move.speed.user.exponent 값을 해당 플레이어 개인 값으로 치환.
     */
    public Map<String, Integer> playerSpeedExponents = new HashMap<>();

    // ── Movement Modes ──────────────────────────────────────────
    public boolean vanillaStyle = false;
    public float sneakFactor = 0.3F;
    public float crawlFactor = 0.15F;
    public float runFactor = 1.3F;
    public float sprintFactor = 1.5F;

    /**
     * 원본 SmartMovingConfig L180 `_sprintFactorLevitate = PositiveFactor("move.sprint.factor.levitate")
     *   .defaults(Value(1.5F).c(3F))` — 부유/비행 중 sprint 가속 배율. Survival 1.5F / Creative 3F.
     * **포커스 #4 B-4 (세션 3)**: vanilla Creative 비행 + sprint+jump 가속 분기 미이식 정정.
     *   (#4 감사 결과 결함 #2 — 원본 SmartMovingSelf L637-L639)
     */
    public float sprintFactorLevitate = 1.5F;

    /**
     * 원본 SmartMovingConfig L181 `_sprintFactorLevitateVertical = PositiveFactor("move.sprint.factor.levitate.vertical")
     *   .defaults(0.185F)` — 부유/비행 중 sprint 시 수직 상승 속도 배율 (sprintFactorLevitate 와 곱).
     * 매 tick 적용: motionY += sprintFactorLevitate * sprintFactorLevitateVertical.
     */
    public float sprintFactorLevitateVertical = 0.185F;

    // ── Climbing ────────────────────────────────────────────────
    // 원본: Options._baseClimb = "standard" (processBlockCode §0 코드). "standard" → true, 그 외 → false
    public boolean baseClimb = true;
    public boolean freeClimb = true;
    /** 원본: _baseClimb = "simple" — grab 없이 표면 접촉만으로 자동 클라이밍 */
    public boolean simpleClimb = false;
    /** 원본: _baseClimb = "smart" — 인접 블록 substitute 판정으로 자동 클라이밍 */
    public boolean smartClimb = false;
    /**
     * 원본 SmartMovingConfig L119 `_freeClimbingAutoLaddder` Unmodified — 기본값 `true`.
     * "사다리를 향할 때 grab 키 없이도 자동 클라이밍 진입". `wouldWantClimb` 4-OR 의 3번째 분기
     * `(isFreeClimbAutoLadderEnabled && isFacedToLadder)` 활성화 게이트. B-16b (세션 69).
     */
    public boolean freeClimbAutoLadder = true;
    /**
     * 원본 SmartMovingConfig L120 `_freeClimbingAutoVine` Unmodified — 기본값 `true`.
     * "solid vine 을 향할 때 grab 키 없이도 자동 클라이밍 진입". `wouldWantClimb` 4-OR 의 4번째
     * 분기 `(isFreeClimbAutoVineEnabled && isFacedToSolidVine)` 활성화 게이트. B-16b (세션 69).
     */
    public boolean freeClimbAutoVine = true;
    public float freeClimbingUpSpeedFactor   = 1.0F;  // PositiveFactor 기본값 1F (A-18 확인)
    public float freeClimbingDownSpeedFactor = 1.0F;  // PositiveFactor 기본값 1F (A-18 확인)
    /**
     * 원본 SmartMovingConfig L115 `_freeClimbingHorizontalSpeedFactor = PositiveFactor("move.climb.free.horizontal.speed.factor")` — 기본값 `1F`.
     * (Properties.java L185-186: PositiveFactor type → default 1F)
     * SmartMovingSelf.java L205 `if(isClimbing) speedFactor *= _freeClimbingHorizontalSpeedFactor.value` 에서
     * 클라이밍 중 횡 이동 (moveStrafing/moveForward) 입력에 곱해지는 인자.
     */
    public float freeClimbingHorizontalSpeedFactor = 1.0F;
    /**
     * 원본 SmartMovingConfig L121 `_freeOneLadderClimbUpSpeedFactor = PositiveFactor("move.climb.free.ladder.one.up.speed.factor").defaults(1.0153F)`.
     * setOnlyShouldClimbSpeed 의 MediumUpMotion 분기에서 ladder 1개 시 factor 곱.
     * 사다리 1줄 등반 시 약간의 추가 속도. 사다리/덩굴 통합 작업 1-2.
     */
    public float freeOneLadderClimbUpSpeedFactor = 1.0153F;
    /**
     * 원본 SmartMovingConfig L122 `_freeBothLadderClimbUpSpeedFactor = IncreasingFactor("move.climb.free.ladder.two.up.speed.factor").defaults(1.43F)`.
     * setOnlyShouldClimbSpeed 의 MediumUpMotion 분기에서 ladder 2개 시 factor 곱.
     * 사다리 2줄 (양쪽) 등반 시 큰 추가 속도. 사다리/덩굴 통합 작업 1-2.
     */
    public float freeBothLadderClimbUpSpeedFactor = 1.43F;
    /**
     * 원본 SmartMovingConfig L104 `_freeBaseLadderClimb = Modified().key("move.climb.free.base.ladder")`
     * (Modified type default = false). isTotalFreeLadderClimb() = isFreeBaseClimb && _freeBaseLadderClimb.
     * MixinLivingEntityClient.travel 의 notTotalFreeClimbing 가드에서 사용:
     *   true → 사다리에서 motion clamp / fallDistance reset / sneak hold 미적용 (vanilla 자유 매달림).
     *   false (default) → SM 보호 매달림 (clamp + hold 적용).
     */
    public boolean freeBaseLadderClimb = false;
    /**
     * 원본 SmartMovingConfig L105 `_freeBaseVineClimb = Modified().key("move.climb.free.base.vine").defaults(true, _pre_sm_1_11)`
     * (current default = false, pre 1.11 default true). isTotalFreeVineClimb() = isFreeBaseClimb && _freeBaseVineClimb.
     */
    public boolean freeBaseVineClimb = false;
    /**
     * 원본 SmartMovingConfig L129 `_freeClimbingOrthogonalDirectionAngle` Positive — 기본값 `90F`.
     * "클라이밍 N/S/E/W 붙잡기 각도(도)". `Orientation.setClimbingAngles` 가 orthogonal 방향
     * (PZ/NZ/ZP/ZN) 의 등반 유효 각도 범위를 계산할 때 사용. 기본값 90F → 각 방향마다 ±45도
     * 범위 내 시선일 때 등반 가능. B-19a0 (세션 90).
     */
    /**
     * 원본 SmartMovingConfig L123 `_freeFenceClimbing = Unmodified("move.climb.free.fence")` — 기본값 `true`.
     * (Properties.java L171-172: Unmodified type → default true)
     * "펜스 타기 허용" 옵션. `Orientation.hasHalfHold` / `hasBottomHold` 에서 펜스/벽 기반
     * grab 경로 활성화 게이트. fence 자체 등반 + 위 잡기 둘 다 이 게이트로 제어됨.
     */
    public boolean freeFenceClimbing = true;
    public float freeClimbingOrthogonalDirectionAngle = 90F;
    /**
     * 원본 SmartMovingConfig L130 `_freeClimbingDiagonalDirectionAngle` Positive — 기본값 `80F`.
     * "클라이밍 NE/NW/SE/SW 붙잡기 각도(도)". `Orientation.setClimbingAngles` 가 diagonal 방향
     * (PP/NN/PN/NP) 의 등반 유효 각도 범위를 계산할 때 사용. 기본값 80F → 각 대각 방향마다 ±40도.
     * B-19a0 (세션 90).
     */
    public float freeClimbingDiagonalDirectionAngle = 80F;
    public float ceilingClimbingSpeedFactor = 0.2F;
    // 원본: Config._freeClimbFallDamageStartDistance = 2F, _freeClimbFallDamageFactor = 2F
    public float freeClimbFallDamageStartDistance = 2.0F;
    public float freeClimbFallDamageFactor        = 2.0F;
    // 원본: Options._climbJumpBackHeadOnGrab — false=grabNotPressed시 headJump, true=grabbed시 headJump
    public boolean climbJumpBackHead = false;
    public boolean climbExhaustion = false;
    public float climbExhaustionStart = 60F;
    public float climbExhaustionStop = 100F;
    public boolean ceilingClimbExhaustion = false;
    public float ceilingClimbExhaustionStart = 40F;
    public float ceilingClimbExhaustionStop = 100F;

    // ── Swimming / Diving ───────────────────────────────────────
    public boolean swim = true;
    public float swimSpeedFactor = 1F;
    public boolean dive = true;
    public float diveSpeedFactor = 1F;
    /**
     * 원본: _swimDownOnSneak = Unmodified("move.swim.down.sneak") → 기본값 true
     * true → 수영 중 스니크 시 하강. SmartMovingSelf wouldWantSneak 및 swimDown 계산에 사용.
     */
    public boolean swimDownOnSneak = true;
    /**
     * 원본: _diveDownOnSneak = Unmodified("move.dive.down.sneak") → 기본값 true
     * true → 잠수 중 스니크 시 하강. SmartMovingSelf wouldWantSneak 및 diveDown 계산에 사용.
     */
    public boolean diveDownOnSneak = true;
    /**
     * 원본: _lavaLikeWater = Creative("move.lava.water") → Survival 기본 false / Creative 기본 true.
     * 1.21.1 단순 boolean 필드 이식 (Survival 기본 false). true 시 lava 에서 수영/잠수 가능.
     *
     * 소비처:
     *   - `Swimmer.updateSwimState` 진입 조건 (원본 L232 `Config.isLavaLikeWaterEnabled() &&
     *     sp.handleLavaMovement()`) — B-7c 에서 복원.
     *   - `ClientState.getLiquidBorder` lava 분기 (원본 SmartMovingBase L140/L144) — 현재 0F
     *     근사 (§7 B-42c 근사 (2)). 이 필드 활성 후 lava border 계산 복원 가능 (B-7b 후속).
     * B-7b (세션 127).
     */
    public boolean lavaLikeWater = false;
    /**
     * 원본: `_lavaSwimParticlePeriodFactor = PositiveFactor("move.lava.swim.particle.period.factor")
     *   .defaults(4F)` (SmartMovingConfig L164).
     * lava 수영 시 splash/bubble 파티클 생성 주기 배수.
     * 공식 (원본 SmartMoving.java L123): `maxSpawnSwimmingParticle = factor × 0.01F`.
     * `spawnSwimmingParticle += horizontalSpeedSquare` 가 임계 초과 시 파티클 생성.
     *   - water (`_swimParticlePeriodFactor`, default `1F`): 임계 `0.01`
     *   - lava (이 필드, default `4F`): 임계 `0.04` → water 대비 4배 드물게 생성.
     * 1.21.1 사용처: 0건 (`spawnSwimmingParticle` 시스템 자체 미이식). 미래 SM 자체 파티클
     *   시스템 이식 시 `_swimParticlePeriodFactor` 와 함께 활용. 포커스 #2.6 A-5 (세션 2).
     */
    public float lavaSwimParticlePeriodFactor = 4F;
    /**
     * 원본: _runOnSprintRelease = _sprintKeyReleaseAction.is("run").and(_run)
     *   → _sprintKeyReleaseAction 기본값 "run" + _run 기본 true → 기본 true.
     * sprint 해제 엣지 (원본 L2702-L2704) 시 `setSprinting(runOnSprintRelease ||
     * wasRunningWhenSprintStarted)` 적용. B-48b-dep (세션 131).
     */
    public boolean runOnSprintRelease = true;
    /**
     * 원본: _walkOnSprintRelease = _sprintKeyReleaseAction.is("walk").andNot(_runOnSprintRelease)
     *   → "walk" 기본 아님 → 기본 false. 두 필드는 상호 배타적.
     * sprintKeyStopPressed 엣지 (원본 L2706) 시 `setSprinting(false)` 강제.
     * B-48b-dep (세션 131).
     */
    public boolean walkOnSprintRelease = false;
    /**
     * 원본: _levitateSmall = Unmodified("move.levitate.small") → 기본값 true.
     * "standard flying small size" 옵션. vanilla creative flying 이 SM flying 비활성
     * 상태일 때 isLevitating 전환 엣지 (원본 L2516-L2522) + mustCrawl=false 조건
     * (원본 L2404) 에서 사용.
     * B-51 (세션 133).
     */
    public boolean levitateSmall = true;

    // ── Jumping ─────────────────────────────────────────────────
    // === Jump base factor (원본 SmartMovingConfig L228-L231) ===
    // 원본 L228: _jumpControlFactor = DecreasingFactor("move.jump.control.factor").defaults(1F)
    //   공중 점프 제어 이동 factor (>= 0, <= 1). DecreasingFactor 기본 1F.
    public float jumpControlFactor = 1F;
    // 원본 L230: _jumpHorizontalFactor = IncreasingFactor("move.jump.horizontal.factor")
    //   tryJump getJumpHorizontalFactor 의 base factor (>= 1). IncreasingFactor 기본 1F.
    public float jumpHorizontalFactor = 1F;
    // 원본 L231: _jumpVerticalFactor = PositiveFactor("move.jump.vertical.factor")
    //   tryJump getJumpVerticalFactor 의 base factor (>= 0). PositiveFactor 기본 1F.
    public float jumpVerticalFactor = 1F;

    // === Speed 별 Jump 활성 + factor (원본 SmartMovingConfig L233-L251) ===
    // 원본 L234: _standJump = Unmodified("move.jump.stand") → 기본 true
    //   isJumpingEnabled(speed=Standing, type) 분기 진입.
    public boolean standJump = true;
    // 원본 L235: _standJumpVerticalFactor = PositiveFactor("move.jump.stand.vertical.factor")
    //   Standing 점프 vertical factor (>= 0). PositiveFactor 기본 1F.
    //   주: Stand 는 horizontal 없음 — getJumpHorizontalFactor 에서 speed=Standing 시 *0F.
    public float standJumpVerticalFactor = 1F;

    // 원본 L237: _sneakJump = Unmodified("move.jump.sneak") → 기본 true
    public boolean sneakJump = true;
    // 원본 L238: _sneakJumpHorizontalFactor = IncreasingFactor("move.jump.sneak.horizontal.factor")
    //   Sneaking 점프 horizontal factor (>= 1). IncreasingFactor 기본 1F.
    public float sneakJumpHorizontalFactor = 1F;
    // 원본 L239: _sneakJumpVerticalFactor = PositiveFactor("move.jump.sneak.vertical.factor")
    //   Sneaking 점프 vertical factor (>= 0). PositiveFactor 기본 1F.
    public float sneakJumpVerticalFactor = 1F;

    // 원본 L241: _walkJump = Unmodified("move.jump.walk") → 기본 true
    public boolean walkJump = true;
    // 원본 L242: _walkJumpHorizontalFactor = IncreasingFactor("move.jump.walk.horizontal.factor")
    //   Walking 점프 horizontal factor (>= 1). IncreasingFactor 기본 1F.
    public float walkJumpHorizontalFactor = 1F;
    // 원본 L243: _walkJumpVerticalFactor = PositiveFactor("move.jump.walk.vertical.factor")
    //   Walking 점프 vertical factor (>= 0). PositiveFactor 기본 1F.
    public float walkJumpVerticalFactor = 1F;

    // 원본 L245: _runJump = Unmodified("move.jump.run") → 기본 true
    public boolean runJump = true;
    // 원본 L246: _runJumpHorizontalFactor = IncreasingFactor("move.jump.run.horizontal.factor").defaults(2F) ★ 오버라이드
    //   Running 점프 horizontal factor (>= 1). 기본 2F (sprint 와 동일).
    public float runJumpHorizontalFactor = 2F;
    // 원본 L247: _runJumpVerticalFactor = PositiveFactor("move.jump.run.vertical.factor")
    //   Running 점프 vertical factor (>= 0). PositiveFactor 기본 1F.
    public float runJumpVerticalFactor = 1F;

    // 원본 L249: _sprintJump = Unmodified("move.jump.sprint") → 기본 true
    public boolean sprintJump = true;
    // 원본 L250: _sprintJumpHorizontalFactor = IncreasingFactor("move.jump.sprint.horizontal.factor").defaults(2F) ★ 오버라이드
    //   Sprinting 점프 horizontal factor (>= 1). 기본 2F.
    public float sprintJumpHorizontalFactor = 2F;
    // 원본 L251: _sprintJumpVerticalFactor = PositiveFactor("move.jump.sprint.vertical.factor")
    //   Sprinting 점프 vertical factor (>= 0). PositiveFactor 기본 1F.
    public float sprintJumpVerticalFactor = 1F;

    // 원본: _wallUpJump = Unmodified("move.jump.wall") → 기본값 true
    public boolean wallUpJump = true;
    // 원본: _wallHeadJump = Unmodified("move.jump.wall.head") → 기본값 true
    public boolean wallHeadJump = true;
    // 원본: _wallUpJumpFallMaximumDistance = Positive(...).defaults(2F)
    public float wallUpJumpFallMaximumDistance = 2F;
    // 원본: _wallHeadJumpFallMaximumDistance = Positive(...).defaults(3F)
    public float wallHeadJumpFallMaximumDistance = 3F;
    // 원본: _wallUpJumpOrthogonalTolerance = Positive(...).defaults(5F)
    public float wallUpJumpOrthogonalTolerance = 5F;
    // 원본: _wallUpJumpVerticalFactor = DecreasingFactor(...).defaults(0.4F)
    // tryJump(WallUp, angle!=null): verticalMotion = -0.078 + 0.498 * wallUpJumpVerticalFactor
    public float wallUpJumpVerticalFactor = 0.4F;
    // 원본: _wallHeadJumpVerticalFactor = DecreasingFactor(...).defaults(0.3F)
    // tryJump(WallHead): verticalMotion = -0.078 + 0.498 * (wallUpJumpVerticalFactor + wallHeadJumpVerticalFactor)
    public float wallHeadJumpVerticalFactor = 0.3F;
    // 원본: _wallUpJumpHorizontalFactor = DecreasingFactor(...).defaults(0.15F)
    public float wallUpJumpHorizontalFactor = 0.15F;
    // 원본: _wallHeadJumpHorizontalFactor = DecreasingFactor(...).defaults(0.15F)
    public float wallHeadJumpHorizontalFactor = 0.15F;
    // === ChargeUp 필드 (원본 SmartMovingConfig L254-L257) ===
    // 원본 L254: _jumpCharge = Unmodified("move.jump.charge") → 기본 true
    public boolean jumpCharge = true;
    // 원본 L255: _jumpChargeMaximum = Positive("move.jump.charge.maximum").defaults(20F)
    //   ChargeUp 최대 카운트 (틱당 1 증가, >= 0).
    public float jumpChargeMaximum = 20F;
    // 원본 L256: _jumpChargeFactor = IncreasingFactor("move.jump.charge.factor").defaults(1.3F)
    //   ChargeUp 완충 시 점프 속도 배율 (>= 1).
    public float jumpChargeFactor = 1.3F;
    // 원본 L257: _jumpChargeCancelOnSneakRelease = Modified("move.jump.charge.sneak.release.cancel") → 기본 false (Modified)
    //   true=sneak 키 떼면 차징 점프 발동, false=sneak 키 떼면 차징 취소.
    public boolean jumpChargeCancelOnSneakRelease = false;
    public boolean headJump = true;
    public float headJumpControlFactor = 0.2F;
    public float headJumpChargeMaximum = 10F;
    // === Angle (Side/Back) 점프 (원본 SmartMovingConfig L268-L271) ===
    // 원본 L268: _angleJumpSide = Unmodified("move.jump.angle.side") → 기본 true
    public boolean angleJumpSide = true;
    // 원본 L269: _angleJumpBack = Unmodified("move.jump.angle.back") → 기본 true
    public boolean angleJumpBack = true;
    // 원본 L270: _angleJumpHorizontalFactor = PositiveFactor(...).defaults(0.3F).defaults(0.4F, _sm_1_3)
    //   ★ _sm_1_3 이상 오버라이드 = 0.4F (이전 0.3F 는 sm_1_3 미만 — 1.7.10 은 sm_1_3 이후라 0.4F 채택).
    public float angleJumpHorizontalFactor = 0.4F;
    // 원본 L271: _angleJumpVerticalFactor = PositiveFactor(...).defaults(0.2F)
    public float angleJumpVerticalFactor = 0.2F;

    // === ClimbUp 점프 (원본 SmartMovingConfig L273-L276) ===
    // 원본 L274: _climbUpJump = Unmodified("move.jump.climb.up") → 기본 true
    public boolean climbUpJump = true;
    // 원본 L275: _climbUpJumpVerticalFactor = DecreasingFactor("move.jump.climb.up.vertical.factor")
    //   ClimbUp 점프 vertical factor (>= 0, <= 1). DecreasingFactor 기본 1F.
    public float climbUpJumpVerticalFactor = 1F;
    // 원본 L276: _climbUpJumpHandsOnlyVerticalFactor = DecreasingFactor(...).defaults(0.8F) ★
    //   ClimbUp Hands-only 추가 vertical factor (>= 0, <= 1). 기본 0.8F.
    //   getJumpVerticalFactor(speed, ClimbUpHandsOnly) = ... × (climbUpJumpVerticalFactor × climbUpJumpHandsOnlyVerticalFactor).
    public float climbUpJumpHandsOnlyVerticalFactor = 0.8F;

    // === ClimbBackUp 점프 (원본 SmartMovingConfig L278-L283) ===
    // 원본 L279: _climbBackUpJump = Unmodified("move.jump.climb.back.up") → 기본 true
    public boolean climbBackUpJump = true;
    // 원본 L280: _climbBackUpJumpVerticalFactor = DecreasingFactor(...).defaults(0.2F).defaults(1F, _pre_sm_3_1) ★
    //   ClimbBackUp vertical factor (>= 0, <= 1). 0.2F (sm_3_1 이상). _pre_sm_3_1 만 1F — 1.7.10 은 sm_3_1 이후라 0.2F 채택.
    public float climbBackUpJumpVerticalFactor = 0.2F;
    // 원본 L281: _climbBackUpJumpHorizontalFactor = DecreasingFactor(...).defaults(0.3F).defaults(1F, _pre_sm_3_1) ★
    //   ClimbBackUp horizontal factor (>= 0, <= 1). 0.3F (sm_3_1 이상). _pre_sm_3_1 만 1F.
    public float climbBackUpJumpHorizontalFactor = 0.3F;
    // 원본 L282: _climbBackUpJumpHandsOnlyVerticalFactor = DecreasingFactor(...).defaults(0.8F) ★
    //   ClimbBackUp Hands-only 추가 vertical factor (>= 0, <= 1). 기본 0.8F.
    public float climbBackUpJumpHandsOnlyVerticalFactor = 0.8F;
    // 원본 L283: _climbBackUpJumpHandsOnlyHorizontalFactor = DecreasingFactor("move.jump.climb.back.up.hands.only.horizontal.factor")
    //   ClimbBackUp Hands-only 추가 horizontal factor (>= 0, <= 1). DecreasingFactor 기본 1F.
    public float climbBackUpJumpHandsOnlyHorizontalFactor = 1F;

    // === ClimbBackHead 점프 (원본 SmartMovingConfig L285-L290) — 구조 ClimbBackUp 와 동일 ===
    // 원본 L286: _climbBackHeadJump = Unmodified("move.jump.climb.back.head") → 기본 true
    public boolean climbBackHeadJump = true;
    // 원본 L287: _climbBackHeadJumpVerticalFactor = DecreasingFactor(...).defaults(0.2F).defaults(1F, _pre_sm_3_1) ★
    //   ClimbBackHead vertical factor (>= 0, <= 1). 0.2F (sm_3_1 이상). _pre_sm_3_1 만 1F.
    public float climbBackHeadJumpVerticalFactor = 0.2F;
    // 원본 L288: _climbBackHeadJumpHorizontalFactor = DecreasingFactor(...).defaults(0.3F).defaults(1F, _pre_sm_3_1) ★
    //   ClimbBackHead horizontal factor (>= 0, <= 1). 0.3F (sm_3_1 이상). _pre_sm_3_1 만 1F.
    public float climbBackHeadJumpHorizontalFactor = 0.3F;
    // 원본 L289: _climbBackHeadJumpHandsOnlyVerticalFactor = DecreasingFactor(...).defaults(0.8F) ★
    //   ClimbBackHead Hands-only 추가 vertical factor (>= 0, <= 1). 기본 0.8F.
    public float climbBackHeadJumpHandsOnlyVerticalFactor = 0.8F;
    // 원본 L290: _climbBackHeadJumpHandsOnlyHorizontalFactor = DecreasingFactor("move.jump.climb.back.head.hands.only.horizontal.factor")
    //   ClimbBackHead Hands-only 추가 horizontal factor (>= 0, <= 1). DecreasingFactor 기본 1F.
    public float climbBackHeadJumpHandsOnlyHorizontalFactor = 1F;

    /**
     * 원본: _angleJumpDoubleClickTicks = Positive("move.jump.angle.double.click.ticks").singular().up(3F, 2F)
     * 각도 점프 더블클릭 감지 타이머(틱). 첫 클릭 후 이 시간 내에 두 번째 클릭 시 트리거.
     * 기본 3F, 최솟값 2F. 사용 시 (int)Math.ceil(value).
     */
    public float angleJumpDoubleClickTicks = 3F;
    /**
     * 원본: _wallJumpDoubleClick = Unmodified("move.jump.wall.double.click").singular()
     * true=더블클릭+홀드로 벽 점프 발동 / false=싱글클릭+홀드. Unmodified 기본값 true.
     */
    public boolean wallJumpDoubleClick = true;
    /**
     * 원본: _wallJumpDoubleClickTicks = Positive("move.jump.wall.double.click.ticks").singular().up(3F, 2F)
     * 벽 점프 더블클릭 타이머(틱). 기본 3F, 최솟값 2F.
     */
    public float wallJumpDoubleClickTicks = 3F;

    // ── Sliding ─────────────────────────────────────────────────
    // 원본: SmartMovingConfig._slideSlipperinessFactor (PositiveFactor, 기본값 1F)
    public float slideSlipperinessFactor = 1.0F;
    // 원본: SmartMovingConfig._slideParticlePeriodFactor (PositiveFactor, 기본값 0.5F)
    public float slideParticlePeriodFactor = 0.5F;
    // 원본: SmartMovingConfig._slidingSpeedStopFactor (PositiveFactor, 기본값 1F)
    public float slidingSpeedStopFactor = 1.0F;
    // 원본: SmartMovingConfig._slideControlDegrees (PositiveFactor, 기본값 1F, deg/tick)
    //   슬라이딩 중 좌우 입력으로 방향 회전. SmartMovingSelf L730-L744.
    public float slideControlDegrees = 1.0F;

    // ── Flying ──────────────────────────────────────────────────
    // 원본: Config._flyingSpeedFactor (PositiveFactor, 기본값 1F)
    public float flyingSpeedFactor = 1.0F;
    // 원본: Options._flyControlVertical — pitch 방향 3D 이동 활성화 여부 (기본값 true)
    public boolean flyControlVertical = true;
    /**
     * 원본: _diveControlVertical = Unmodified("move.dive.control.vertical").singular()
     * 기본값 true. 잠수 중 pitch 방향 3D 이동 활성화 여부.
     * SmartMovingSelf.handleSwimming (isDiving 분기) 에서 moveFlying 5-인자 treeDimensional 파라미터로 전달.
     */
    public boolean diveControlVertical = true;

    // ── Crawl edge protection ──────────────────────────────────
    // 원본: SmartMovingConfig._crawlOverEdge = Unmodified("move.crawl.edge") → 기본값 true
    // false → 크롤링 중 엣지에서 isSneaking=true 강제 (추락 방지)
    public boolean crawlOverEdge = true;

    // ── Crawl / Sneak toggle ────────────────────────────────────
    // 원본: Options._crawlToggle = Modified("move.crawl.toggle") → 기본값 false
    // false=홀드(스닉키 누름 유지), true=토글(grab 한 번 눌러 크롤링 고정/해제)
    public boolean crawlToggle = false;
    /**
     * 원본: _sneakToggle = Modified("move.sneak.toggle") → 기본값 false
     * false=홀드(스닉키 누름 유지), true=토글(스닉키 한 번 눌러 스닉 고정/해제).
     * 원본 isSneakToggleEnabled() = _sneakToggle.value && enabled.
     */
    public boolean sneakToggle = false;

    // ── Name tag display ────────────────────────────────────────
    // 원본: _sneakNameTag = Modified("move.sneak.name") → 기본값 true
    // true → 스니킹 중에도 이름 태그 64 거리 기준 (vanilla는 32)
    public boolean sneakNameTag = true;
    // 원본: _crawlNameTag = Modified("move.crawl.name") → 기본값 true
    // true → 크롤 중 이름 태그 표시, false → 숨김
    public boolean crawlNameTag = true;

    // ── HUD display ────────────────────────────────────────────
    /**
     * 원본: _displayExhaustionBar = Unmodified("move.gui.exhaustion.bar").singular()
     * 기본값 true (Unmodified). 소진 바 HUD 표시 여부.
     * SmartMovingRender.renderGuiIngame 진입 조건:
     *   Config.enabled && (displayExhaustionBar || displayJumpChargeBar)
     */
    public boolean displayExhaustionBar = true;
    /**
     * 원본: _displayJumpChargeBar = Unmodified("move.gui.jump.charge.bar").singular()
     * 기본값 true. 점프 차지 바 HUD 표시 여부.
     */
    public boolean displayJumpChargeBar = true;

    // ── Flying close-to-ground ─────────────────────────────────
    // 🔴 BUG-26 정정 (Flying Phase / 세션 43): default 값 1:1 정정.
    //   원본 SmartMovingOptions L68 `Modified("move.fly.ground.close")` (defaults 미지정) →
    //   net.smart.properties.Properties.getDefaultValue(Modified) = **false** (Properties.java
    //   L173-L174). 이전 1.21.1 잘못 매핑: true → tryLanding 가드 `!flyCloseToGround` 가 false →
    //   자동 착지 영원히 비활성 = 비행 중 정지해도 비행 종료 안 됨 (BUG-26 직접 원인).
    //   정정: false 로 변경 → 원본 1:1 동작 (정지 시 자동 착지 + ground 닿으면 vanilla 자동 착지).
    public boolean flyCloseToGround = false;
    // 🔴 BUG-26 정정 (Flying Phase / 세션 43): 원본 default = false.
    //   _flyWhileOnGround = "ground 위에서도 비행 가능" 의 활성 옵션. true 이면 vanilla 자동 착지
    //   (ClientPlayerEntity.tickMovement L1293-L1331) 직후 sm_flyWhileOnGround 가 abilities.flying
    //   복원 → ground 닿아도 비행 유지. false (원본 default) 이면 vanilla 자동 착지 그대로 작동.
    public boolean flyWhileOnGround = false;

    // ── Perspective (FOV) ──────────────────────────────────────
    // 원본: _perspectiveFadeFactor = PositiveFactor.values(0.5F, 0.1F, 1F)
    public float perspectiveFadeFactor = 0.5F;
    // 원본: _perspectiveSpeedFactor = Float.defaults(1F)
    public float perspectiveSpeedFactor = 1F;
    // 원본: _perspectiveSpeedFactorMax = PositiveFactor.defaults(0F)
    public float perspectiveSpeedFactorMax = 0F;
    // 원본: _perspectiveRunFactor = Float.defaults(1F)
    public float perspectiveRunFactor = 1F;
    // 원본: _perspectiveSprintFactor = Float.defaults(1.5F)
    public float perspectiveSprintFactor = 1.5F;

    // ── Exhaustion/Hunger factor 1단계 (원본 getFactor L563-L575 이동속도 배율) ──
    // 원본 SmartMovingConfig.java 의 Property<Float> factor 필드들을 1.21.1 에 단순 필드로 이식.
    // Easy 1:1 원칙: 난이도 체인 `.e(x).h(y)` 이 있는 base 2개는 Easy 값을 default 로. 나머지는
    // 원본 최상위 defaults(Value(x)) 값. 원본 `Value(default).e(...).h(...)` 전체 key 별 값은
    // javadoc 에 기록. ⚠️ 1단계는 `Exhaution` 오타 — 원본 필드명/키 그대로 유지 (1:1 호환).
    // up() 동적 하한 종속성은 Property 시스템 부재로 단순 default 근사 — config 편집 시 원본의
    // 하한 체크는 작동 안 함.

    /**
     * 원본 `_baseExhautionLossFactor` L399 = `PositiveFactor("move.exhaustion.loss.factor")
     *   .defaults(Value(1F).e(1.2F).h(0.8F)).defaults(1F, _pre_sm_1_5)`.
     * 난이도별: default=1F, Easy=**1.2F**, Hard=0.8F. 버전 폴백 `_pre_sm_1_5` → 1F 고정.
     * Easy 1:1 → 1.2F 채택.
     */
    public float baseExhautionLossFactor = 1.2F;

    /**
     * 원본 `_baseHungerGainFactor` L423 = `PositiveFactor("move.hunger.gain.factor")
     *   .defaults(Value(1F).e(0.8F).h(1.2F)).defaults(1F, _pre_sm_1_5)`.
     * 난이도별: default=1F, Easy=**0.8F**, Hard=1.2F. 버전 폴백 _pre_sm_1_5 → 1F.
     * Easy 1:1 → 0.8F 채택.
     */
    public float baseHungerGainFactor = 0.8F;

    /**
     * 원본 `_fallExhautionLossFactor` L406 = `PositiveFactor("move.exhaustion.fall.loss.factor")
     *   .up(2.5F, _standingExhautionLossFactor)`.
     * default=2.5F. 난이도 체인 없음. up() 하한 = `_standingExhautionLossFactor` 값 이상 — 근사.
     * ⚠️ airBorne + hunger 대응 필드 **없음** — `getFactor` L565 에서 `0F` 하드코딩.
     */
    public float fallExhautionLossFactor = 2.5F;

    /**
     * 원본 `_sprintingHungerGainFactor` L425 = `PositiveFactor("move.hunger.sprint.gain.factor")
     *   .key("move.hunger.gain.sprint.factor", _pre_sm_1_3)`.
     * default=PF 기본(1F). 난이도 체인 없음. _pre_sm_1_3 키이명(과거 세이브 호환).
     */
    public float sprintingHungerGainFactor = 1F;

    /**
     * 원본 `_sprintingExhautionLossFactor` L401 = `PositiveFactor("move.exhaustion.sprint.loss.factor")
     *   .defaults(0F)`.
     * default=0F. 난이도 체인 없음.
     */
    public float sprintingExhautionLossFactor = 0F;

    /**
     * 원본 `_runningHungerGainFactor` L426 = `PositiveFactor("move.hunger.run.gain.factor")
     *   .key("move.hunger.gain.run.factor", _pre_sm_1_3).defaults(10F)`.
     * default=10F. 난이도 체인 없음. ⚠️ 필드명 `running`, 키는 `run`.
     */
    public float runningHungerGainFactor = 10F;

    /**
     * 원본 `_runningExhautionLossFactor` L402 = `PositiveFactor("move.exhaustion.run.loss.factor")
     *   .up(0.5F, _sprintingExhautionLossFactor)`.
     * default=0.5F. 난이도 체인 없음. up() 하한 = `_sprintingExhautionLossFactor` 값 이상 — 근사.
     */
    public float runningExhautionLossFactor = 0.5F;

    /**
     * 원본 `_sneakingHungerGainFactor` L428 = `PositiveFactor("move.hunger.sneak.gain.factor")
     *   .key("move.hunger.gain.sneak.factor", _pre_sm_1_3)`.
     * default=PF 기본(1F). 난이도 체인 없음.
     */
    public float sneakingHungerGainFactor = 1F;

    /**
     * 원본 `_sneakingExhautionLossFactor` L404 = `PositiveFactor("move.exhaustion.sneak.loss.factor")
     *   .up(1.5F, _walkingExhautionLossFactor).defaults(1F, _sm_1_1)`.
     * default=1.5F. 난이도 체인 없음. up() 하한 = `_walkingExhautionLossFactor` 값 이상 — 근사.
     * 버전 폴백 `_sm_1_1` → 1F.
     */
    public float sneakingExhautionLossFactor = 1.5F;

    /**
     * 원본 `_standingHungerGainFactor` L429 = `PositiveFactor("move.hunger.stand.gain.factor")
     *   .defaults(0F)`.
     * default=0F. 난이도 체인 없음.
     */
    public float standingHungerGainFactor = 0F;

    /**
     * 원본 `_standingExhautionLossFactor` L405 = `PositiveFactor("move.exhaustion.stand.loss.factor")
     *   .up(2F, _sneakingExhautionLossFactor.maximum(1F))`.
     * default=2F. 난이도 체인 없음. up() 하한 = `_sneakingExhautionLossFactor` 값을 최대 1F 로
     * 클램프한 값 이상 — 근사.
     */
    public float standingExhautionLossFactor = 2F;

    /**
     * 원본 `_walkingHungerGainFactor` L427 = `PositiveFactor("move.hunger.walk.gain.factor")
     *   .key("move.hunger.gain.walk.factor", _pre_sm_1_3)`.
     * default=PF 기본(1F). 난이도 체인 없음.
     */
    public float walkingHungerGainFactor = 1F;

    /**
     * 원본 `_walkingExhautionLossFactor` L403 = `PositiveFactor("move.exhaustion.walk.loss.factor")
     *   .up(1F, _runningExhautionLossFactor)`.
     * default=1F. 난이도 체인 없음. up() 하한 = `_runningExhautionLossFactor` 값 이상 — 근사.
     */
    public float walkingExhautionLossFactor = 1F;

    // ── Exhaustion/Hunger factor 2단계 (원본 getFactor L577-L592 행동상태 배율) ──
    // ⚠️ 2단계는 `Exhaustion` 정타 (1단계 `Exhaution` 오타와 혼재). 원본 그대로 유지.
    // ⚠️ `ceilClimbing` 축약 필드명 (`ceilingClimbing` 아님). 원본 그대로 유지.
    // 난이도 체인 없음 — 전 난이도 동일(default = Easy).

    /**
     * 원본 `_climbingHungerGainFactor` L431 = `PositiveFactor("move.hunger.climb.gain.factor")
     *   .key("move.hunger.gain.climb.factor", _pre_sm_1_3)`. default=PF 기본(1F).
     */
    public float climbingHungerGainFactor = 1F;

    /**
     * 원본 `_climbingExhaustionLossFactor` L409 = `PositiveFactor("move.exhaustion.climb.loss.factor")`.
     * default=PF 기본(1F).
     */
    public float climbingExhaustionLossFactor = 1F;

    /**
     * 원본 `_crawlingHungerGainFactor` L432 = `PositiveFactor("move.hunger.crawl.gain.factor")
     *   .key("move.hunger.gain.crawl.factor", _pre_sm_1_3)`. default=PF 기본(1F).
     */
    public float crawlingHungerGainFactor = 1F;

    /**
     * 원본 `_crawlingExhaustionLossFactor` L410 = `PositiveFactor("move.exhaustion.crawl.loss.factor")`.
     * default=PF 기본(1F).
     */
    public float crawlingExhaustionLossFactor = 1F;

    /**
     * 원본 `_ceilClimbingHungerGainFactor` L433 = `PositiveFactor("move.hunger.climb.gain.ceiling.factor")
     *   .key("move.hunger.gain.climb.ceiling.factor", _pre_sm_1_3)`. default=PF 기본(1F).
     * ⚠️ 필드명 `ceilClimbing` 축약. 키 이름 `climb.gain.ceiling` 순서 (gain 이 ceiling 앞).
     */
    public float ceilClimbingHungerGainFactor = 1F;

    /**
     * 원본 `_ceilClimbingExhaustionLossFactor` L408 = `PositiveFactor("move.exhaustion.climb.ceiling.loss.factor")`.
     * default=PF 기본(1F). 키 이름 `climb.ceiling.loss` 순서 (ceiling 이 loss 앞).
     */
    public float ceilClimbingExhaustionLossFactor = 1F;

    /**
     * 원본 `_swimmingHungerGainFactor` L434 = `PositiveFactor("move.hunger.swim.gain.factor")
     *   .key("move.hunger.gain.swim.factor", _pre_sm_1_3).defaults(1.5F)`. default=**1.5F**.
     */
    public float swimmingHungerGainFactor = 1.5F;

    /**
     * 원본 `_swimmingExhaustionLossFactor` L412 = `PositiveFactor("move.exhaustion.swim.loss.factor")`.
     * default=PF 기본(1F).
     */
    public float swimmingExhaustionLossFactor = 1F;

    /**
     * 원본 `_divingHungerGainFactor` L435 = `PositiveFactor("move.hunger.dive.gain.factor")
     *   .key("move.hunger.gain.dive.factor", _pre_sm_1_3).defaults(1.5F)`. default=**1.5F**.
     */
    public float divingHungerGainFactor = 1.5F;

    /**
     * 원본 `_divingExhaustionLossFactor` L413 = `PositiveFactor("move.exhaustion.dive.loss.factor")`.
     * default=PF 기본(1F).
     */
    public float divingExhaustionLossFactor = 1F;

    /**
     * 원본 `_dippingHungerGainFactor` L436 = `PositiveFactor("move.hunger.dip.gain.factor")
     *   .key("move.hunger.gain.dip.factor", _pre_sm_1_3).defaults(1.5F)`. default=**1.5F**.
     * ⚠️ 키에서 `dip` 축약 (dipping 아님).
     */
    public float dippingHungerGainFactor = 1.5F;

    /**
     * 원본 `_dippingExhaustionLossFactor` L411 = `PositiveFactor("move.exhaustion.dip.loss.factor")`.
     * default=PF 기본(1F). 키에서 `dip` 축약.
     */
    public float dippingExhaustionLossFactor = 1F;

    /**
     * 원본 `_normalHungerGainFactor` L437 = `PositiveFactor("move.hunger.normal.gain.factor")
     *   .key("move.hunger.gain.ground.factor", _pre_sm_1_3)`. default=PF 기본(1F).
     * ⚠️ getFactor L589 onGround + L591 else 둘 다 참조 (의도된 중복).
     */
    public float normalHungerGainFactor = 1F;

    /**
     * 원본 `_normalExhaustionLossFactor` L414 = `PositiveFactor("move.exhaustion.normal.loss.factor")`.
     * default=PF 기본(1F). getFactor L590/L592 onGround + else 둘 다 참조 (의도된 중복).
     */
    public float normalExhaustionLossFactor = 1F;

    // ── handleExhaustion 기타 필드 (원본 SmartMovingSelf L849-L916 허기 공식) ─────

    /**
     * 원본 `_alwaysHungerGain` L439 = `Positive("move.hunger.always.gain")
     *   .defaults(Value(0F).h(0.005F)).defaults(0F, _pre_sm_1_5)`.
     * 난이도별: default=0F, Easy=0F, Hard=0.005F. 버전 폴백 `_pre_sm_1_5` → 0F 고정.
     * Easy 1:1 → 0F (default = Easy).
     * 소비처: `SmartMovingSelf.handleExhaustion` L863 —
     *   `hungerIncrease += _alwaysHungerGain.value + relevantMovementFactor * 0.0001F * hungerGainFactor`.
     * 매 틱 상시 허기 증가 (Easy 에선 0 → 상시 증가 없음, Hard 에선 0.005 누적).
     */
    public float alwaysHungerGain = 0F;

    /**
     * 원본 `_exhaustionLossHungerFactor` L417 = `PositiveFactor("move.exhaustion.hunger.factor")
     *   .defaults(Value(0.05F).e(0.02F).h(0.08F)).defaults(0.05F, _pre_sm_1_5)`.
     * 난이도별: default=0.05F, Easy=**0.02F**, Hard=0.08F. 버전 폴백 `_pre_sm_1_5` → 0.05F 고정.
     * Easy 1:1 → 0.02F 채택.
     * 소비처: `SmartMovingSelf.handleExhaustion` L893 —
     *   `hungerIncrease += _exhaustionLossHungerFactor.value * exhaustionLoss`.
     * exhaustion 감소분 × 이 factor 만큼 허기 추가 증가.
     */
    public float exhaustionLossHungerFactor = 0.02F;

    // ── Misc ────────────────────────────────────────────────────
    public boolean fly = true;
    public boolean slide = true;
    public boolean crawl = true;
    public boolean sneak = true;
    public boolean run = true;
    public boolean sprint = true;
    public boolean ceilingClimbing = true;

    /**
     * 원본 `SmartMovingConfig.java` L313:
     *   `public final Property<Boolean> _sprintEnableStanding = Unmodified("move.sprint.enable.ground");`
     * 기본값 false — `Unmodified` 는 기본 생성자에서 `value=false`.
     * 사용처: isFast 공식 `isGroundSprinting && (!standing || _sprintEnableStanding)` (원본 L2689).
     * true 설정 시 standing 상태에서도 Ground Sprint 인정 (정지 스프린트 허용).
     * B-1a (세션 40).
     *
     * 🔴 (2026-04-28) default false → true 변경. 사용자 의도 "SM 모드 시 vanilla 보다 빠름".
     *   원본 default false 시 onGround 일반 sprint = isFast=false → nonSlow=1.0 → vanilla 동일.
     *   true 시 isFast=true → nonSlow=sprintFactor (1.5) → vanilla * 1.154x.
     *   원본 식 (isFast 시 1.5x) 그대로 발동되도록 default 변경.
     */
    public boolean sprintEnableStanding = true;

    /**
     * 원본 `SmartMovingConfig.java` L348:
     *   `public final Property<Float> _fallingDistanceMinimum = Positive("move.fall.distance.minimum").defaults(3F);`
     * 기본값 3F. 사용처:
     *   - canCrawl (원본 L2439) `sp.fallDistance < _fallingDistanceMinimum` — 크롤 진입 차단
     *   - isSliding fallDistance 분기 (원본 L2569) — 슬라이드 해제 + 크롤 전환
     * B-32 (세션 44).
     */
    public float fallingDistanceMinimum = 3F;

    /**
     * 원본 `SmartMovingConfig.java` L223:
     *   `public final Property<Float> _fallAnimationDistanceMinimum = Positive("move.fall.animation.distance.minimum").min(_fallingDistanceMinimum).defaults(3F, _pre_sm_1_6);`
     * 기본값 3F. `_fallingDistanceMinimum` 과 별도 — 애니메이션 트리거 전용.
     * 사용처: doFallingAnimation (원본 SmartMovingSelf.java L3281)
     *   `!sp.onGround && sp.fallDistance > _fallAnimationDistanceMinimum.value`.
     * 1.21.1 매핑: MixinPlayerEntityModelClient.sm_setAngles isFalling 진입 조건.
     */
    public float fallAnimationDistanceMinimum = 3F;

    // ── B-1c1 (세션 50) — 원본 SmartMovingConfig.java 피로/스프린트 관련 필드 ─────────

    /**
     * 원본 L298 `_runExhaustionStart = Positive("move.exhaustion.run.start").defaults(75F)`.
     * 사용처: exhaustionAllowsRunning (원본 L2621-L2622) 판정 하한.
     */
    public float runExhaustionStart = 75F;

    /**
     * 원본 L299 `_runExhaustionStop = Positive("move.exhaustion.run.stop").up(100F, _runExhaustionStart)`.
     * `up(100F, ...)` — 기본값 100F, 최소 runExhaustionStart. 사용처: maxExhaustionForAction 상한.
     */
    public float runExhaustionStop = 100F;

    /**
     * 원본 L314 `_sprintExhaustionStart = Positive(...).defaults(50F)`.
     * 사용처: exhaustionAllowsSprinting (원본 L2640) 판정 하한.
     */
    public float sprintExhaustionStart = 50F;

    /**
     * 원본 L315 `_sprintExhaustionStop = Positive(...).up(100F, _sprintExhaustionStart)`.
     * 기본값 100F. 사용처: exhaustionAllowsSprinting (원본 L2639) + maxExhaustionForAction 상한.
     */
    public float sprintExhaustionStop = 100F;

    /**
     * 원본 L589 `_sprintDuringItemUsage = Modified("move.usage.sprint")`.
     * Modified 기본값 false. 사용처: canAnySprint (원본 L2673) — true 면 아이템 사용 중에도
     * sprint 허용.
     */
    public boolean sprintDuringItemUsage = false;

    /**
     * 원본 `SmartMovingClientConfig.java` L90 `isRunExhaustion = Modified(...)`.
     * 1.21.1 기본값 false (SM 기본 비활성).
     */
    public boolean runExhaustion = false;

    /**
     * 원본 `SmartMovingClientConfig.java` L94 `_sprintExhaustion = Modified(...)`.
     * 1.21.1 기본값 false.
     */
    public boolean sprintExhaustion = false;

    /**
     * 원본 `SmartMovingClientConfig.java` L92 `isRunExhaustionEnabled() = _runExhaustion && enabled`.
     * AND 패턴. B-1c1 (세션 50).
     */
    public boolean isRunExhaustionEnabled() {
        return runExhaustion && enabled;
    }

    /**
     * 원본 `SmartMovingClientConfig.java` L93 `isClimbExhaustionEnabled() = _climbExhaustion && enabled`.
     * AND 패턴. B-1c1 (세션 50).
     */
    public boolean isClimbExhaustionEnabled() {
        return climbExhaustion && enabled;
    }

    /**
     * 원본 `SmartMovingClientConfig.java` L96 `isSprintExhaustionEnabled() = _sprintExhaustion && enabled`.
     * AND 패턴. B-1c1 (세션 50).
     */
    public boolean isSprintExhaustionEnabled() {
        return sprintExhaustion && enabled;
    }

    // ── B-24 (세션 53) — isHeadJumping 해제 엣지 handleCrash Config 필드 ───────────

    /**
     * 원본 `SmartMovingConfig.java` L264 (포커스 #2.5 A-5d 이식 완결):
     *   `_headFallDamageStartDistance = Positive("move.fall.head.damage.start.distance").values(2F, 1F, 3F);`
     * 기본값 2F (values: default=2F, min=1F, max=3F). 사용처: `handleCrash(startDistance, factor)`
     * (원본 L2538) — 헤드점프 해제 엣지 + 자유 클라이밍 낙하 데미지 시작 거리.
     */
    public float headFallDamageStartDistance = 2F;

    /**
     * 원본 `SmartMovingConfig.java` L265 (포커스 #2.5 A-5e 이식 완결):
     *   `_headFallDamageFactor = IncreasingFactor("move.fall.head.damage.factor").defaults(2F);` ★ 오버라이드
     * 기본값 2F (IncreasingFactor 기본 1F → defaults(2F) 오버라이드). 사용처: handleCrash 낙하 데미지 배율.
     */
    public float headFallDamageFactor = 2F;

    // ── 활성화 플래그 ──────────────────────────────────────────
    /**
     * SM 활성화 상태. 원본 SmartMovingProperties.enabled (L33).
     * `update()` 호출 시 `toggler != -1` 로 파생 — 직접 쓰기 금지, toggle()/setCurrentKey 경유.
     */
    public boolean enabled = true;

    // ── B 단계 Phase 1 헬퍼 (세션 39) — 원본 SmartMovingClientConfig.java ────────
    // 원본 `enabled` 패턴 (리서치 L428):
    //   SM 비활성 시 vanilla 동작 허용 → `|| !enabled` (isSneakingEnabled 등)
    //   SM 전용 기능 → `&& enabled` (isSwim/Diving/Sprinting 등)

    /**
     * 원본 SmartMovingClientConfig L69 `isSneakingEnabled() = _sneak.value || !enabled`.
     * **OR 패턴** — SM 비활성 시에도 vanilla sneak 허용.
     * 사용처: wantSneak (원본 L2588-L2590) → isSlow 공식 (원본 L2718).
     * B-2 의존.
     */
    public boolean isSneakingEnabled() {
        return sneak || !enabled;
    }

    /**
     * 원본 SmartMovingClientConfig L95 `isSprintingEnabled() = _sprint.value && enabled`.
     * **AND 패턴** — SM 비활성 시 SM sprint 비허용.
     * 사용처: wantSprint 6조건 OR (원본 L2595-L2615) → wouldIsSneaking (L2712).
     * B-3a 의존.
     */
    public boolean isSprintingEnabled() {
        return sprint && enabled;
    }

    /**
     * 원본 SmartMovingClientConfig L87 `isSwimmingEnabled() = _swim.value && enabled`.
     * 사용처: handleSwimming 진입 게이트 (원본 L239/L438) — useStandard 진입 판정.
     * B-7 / B-8 의존.
     */
    public boolean isSwimmingEnabled() {
        return swim && enabled;
    }

    /**
     * 원본 SmartMovingClientConfig L88 `isDivingEnabled() = _dive.value && enabled`.
     * 사용처: handleSwimming 진입 게이트 + dive 분류 활성화 판정.
     * B-7 / B-8 의존.
     */
    public boolean isDivingEnabled() {
        return dive && enabled;
    }

    /**
     * 원본 SmartMovingClientConfig L87-L90 `isLavaLikeWaterEnabled() = _lavaLikeWater.value && enabled`.
     * 사용처: Swimmer.updateSwimState 진입 조건 (원본 L232 `Config.isLavaLikeWaterEnabled()
     * && sp.handleLavaMovement()`). B-7c 에서 복원. Lava 를 물처럼 수영/잠수 가능한지.
     * B-7b (세션 127).
     */
    public boolean isLavaLikeWaterEnabled() {
        return lavaLikeWater && enabled;
    }

    /**
     * 원본 SmartMovingClientConfig L97-L100 `isLevitateSmallEnabled() = _levitateSmall.value && enabled`.
     * 사용처: mustCrawl=false 조건 확장 (원본 L2404) + isLevitating 전환 엣지 블록 (원본 L2516-L2522).
     * B-51 (세션 133).
     */
    public boolean isLevitateSmallEnabled() {
        return levitateSmall && enabled;
    }

    /**
     * 원본 SmartMovingClientConfig L92-L95 `isFlyingEnabled() = _fly.value && enabled`.
     * 사용처: isFlying 공식 (원본 L2510) + isLevitating 전환 엣지 (원본 L2516) +
     * mustCrawl=false 조건 (원본 L2404). B-51 (세션 133).
     */
    public boolean isFlyingEnabled() {
        return fly && enabled;
    }

    /**
     * 원본 SmartMovingClientConfig L57-L60 `isFreeClimbAutoLaddderEnabled() =
     *   _freeClimbingAutoLaddder.value && enabled`.
     * 사용처: `wouldWantClimb` 4-OR 의 3번째 분기 (원본 L2471) 자동 사다리 진입 게이트.
     * B-16b (세션 69).
     */
    public boolean isFreeClimbAutoLadderEnabled() {
        return freeClimbAutoLadder && enabled;
    }

    /**
     * 원본 SmartMovingClientConfig L62-L65 `isFreeClimbAutoVineEnabled() =
     *   _freeClimbingAutoVine.value && enabled`.
     * 사용처: `wouldWantClimb` 4-OR 의 4번째 분기 (원본 L2472) 자동 솔리드 덩굴 진입 게이트.
     * B-16b (세션 69).
     */
    public boolean isFreeClimbAutoVineEnabled() {
        return freeClimbAutoVine && enabled;
    }

    /**
     * 원본 SmartMovingConfig `_isFreeBaseClimb = _baseClimb.is("free").and(_freeClimb)` Property
     * (`baseClimb` String 이 "free" AND `freeClimb` true).
     *
     * **§7 근사** (B-19a2c-approx-config): 1.21.1 에서는 `baseClimb` 를 String 대신
     * 4 boolean (freeClimb/simpleClimb/smartClimb/standardClimb) 으로 이식. `freeClimb`
     * 단독으로 "Free Base Climb 모드" 판정 가능 — String "free" 일 때 `freeClimb=true`
     * 설정이라 동치. B-19a2c (세션 104).
     */
    public boolean isFreeBaseClimb() {
        // 근사 이식 — 원본과 차이: `_baseClimb.is("free")` 체크 생략 (boolean 4 필드 이식 구조)
        return freeClimb;
    }

    /**
     * 원본 SmartMovingClientConfig L47-50 `isTotalFreeLadderClimb()`:
     *   return isFreeBaseClimb() && _freeBaseLadderClimb.value;
     * MixinLivingEntityClient.travel notTotalFreeClimbing 가드에서 사용.
     */
    public boolean isTotalFreeLadderClimb() {
        return isFreeBaseClimb() && freeBaseLadderClimb;
    }

    /**
     * 원본 SmartMovingClientConfig L52-55 `isTotalFreeVineClimb()`:
     *   return isFreeBaseClimb() && _freeBaseVineClimb.value;
     */
    public boolean isTotalFreeVineClimb() {
        return isFreeBaseClimb() && freeBaseVineClimb;
    }

    /**
     * 원본 `SmartMovingOptions.java` L449-L455 `isSneakToggleEnabled()`:
     *   return _sneakToggle.value && enabled;
     * **AND 패턴** — SM 비활성 시 토글 모드 비허용.
     * 사용처: sneakContinueInput (원본 L2576) / R-09 블록 진입 (원본 L2968) / wantSneak_
     * 계산 (원본 L2981 부근).
     * B-45a (세션 45).
     */
    public boolean isSneakToggleEnabled() {
        return sneakToggle && enabled;
    }

    /**
     * 원본 `SmartMovingOptions.java` L462-L469 `isCrawlToggleEnabled()`:
     *   return _crawlToggle.value && enabled;
     * **AND 패턴**. 사용처: inputContinueCrawl (원본 L2407) / R-09 블록 (원본 L2969) /
     * toCrawling() (원본 L3050).
     * B-45a (세션 45).
     */
    public boolean isCrawlToggleEnabled() {
        return crawlToggle && enabled;
    }

    // ── Config key 토글 시스템 (원본 SmartMovingProperties L26-L31) ─────────────────
    // H-19 (세션 23): CONFIG_KEY_ENABLED/DISABLED 상수 삭제. getCurrentKey/getKey/getNextKey/
    //   hasKey/setCurrentKey 메서드 5개도 삭제 (외부 호출처 0건 확인). DEFAULT_KEYS 만 유지
    //   — setKeys(null) 시 fallback 으로 사용됨.
    /**
     * 원본 SmartMovingProperties._defaultKeys = new String[1] = {null}.
     * 단일 null 요소 — `keys[0] == null` 이면 단순 on/off 토글 모드.
     */
    private static final String[] DEFAULT_KEYS = new String[] { null };

    /**
     * 원본 SmartMovingProperties.keys (L31) 대응.
     * 현재 유효한 config key 배열. 초기값 {null} (단순 on/off) — `setKeys(...)` 호출로
     * 게임타입별 배열({"e","m","h"} / {"c"} 등) 로 교체. `initializeForGameIfNeccessary()`
     * 이식(F 섹션)에서 gameType 판정 후 setKeys 호출.
     */
    public String[] configKeys = DEFAULT_KEYS;

    /**
     * 원본 SmartMovingProperties.toggler (L30) 대응. 초기값 -2 (load 전 센티넬).
     * 상태표:
     *   -2 : 초기화 전 (load() 미호출)
     *   -1 : disabled (enabled = false)
     *   0..configKeys.length-1 : enabled, 현재 key 인덱스
     * 파생: `enabled = (toggler != -1)` — `update()` 에서 동기화. 직접 쓰기 금지, toggle()/
     *   setCurrentKey() 경유.
     */
    public int toggler = -2;

    // H-18 (세션 23): configKeyName Map 삭제. 2상태 토글(configKeys={null}) 에서 currentKey
    // 는 항상 null 이므로 "Easy"/"Medium"/"Hard" 라벨 표시 경로 전부 제거됨. Medium/Hard
    // 프리셋 복원 시(`focus_09_difficulty_presets.md` 후속) 재도입.
    //
    // H-20 (세션 23): gameType 시스템 전체 삭제. 2상태 토글에선 gameType 감지/전환이
    // 불필요. 원본 SmartMovingOptions.initializeForGameIfNeccessary / resetForNewGame /
    // gameType 캐시 + GAME_TYPE_UNKNOWN/SURVIVAL/CREATIVE/ADVENTURE 4상수 + 게임타입별
    // 6필드 (survival/creative/adventure × configKeys+defaultConfigKey) 전부 제거.
    // 세이브 파일 호환: 기존 키(move.config.{survival,creative,adventure}.keys{,.default})
    // 는 단순히 무시됨 (readFrom 호출 삭제). Medium/Hard 프리셋 복원 시 재도입.


    // ── Singleton / Config 전환 ────────────────────────────────
    /** 클라이언트 파일 기반 설정 (Options). 불변 싱글톤. */
    public static final SmartMovingConfig INSTANCE = new SmartMovingConfig();

    /**
     * 서버 수신 설정 인스턴스. loadFromArray()로 갱신.
     * 연결 해제 시 resetServerConfig() 호출로 새 인스턴스 교체 — 원본 SmartMovingServerConfig.reset() 대응.
     * volatile — 클라이언트 렌더/틱 스레드 간 가시성 보장.
     */
    public static volatile SmartMovingConfig SERVER_CONFIG = new SmartMovingConfig();

    /**
     * 현재 활성 설정 참조 (원본: SmartMovingContext.Config).
     * 초기값: INSTANCE (클라이언트 자체 설정).
     * 서버 설정 수신 시: SERVER_CONFIG로 전환.
     * 서버 설정 해제 시: INSTANCE로 복원.
     * volatile — 클라이언트 렌더/틱 스레드 간 가시성 보장.
     */
    public static volatile SmartMovingConfig Config = INSTANCE;

    private static final String FILE_NAME = "smart_moving_options.properties";

    private SmartMovingConfig() {}

    public static void load() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        File file = configFile.toFile();
        if (!file.exists()) {
            save();
            return;
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            return;
        }
        INSTANCE.readFrom(props);
        // 🔴 BUG-26 자동 마이그레이션 (Flying Phase / 세션 44): 이전 잘못된 default (true) 가
        //   사용자 환경에 저장된 경우 강제 false 로 마이그레이션 + save.
        //   원본 net.smart.properties.Properties.getDefaultValue(Modified) = false 가 정답인데
        //   1.21.1 매핑이 true 로 잘못 저장되어 있으면 vanilla 자동 착지 무효화 (BUG-26 직접 원인).
        //   마이그레이션 마커: move.config.migration.flying = "session_44_done".
        String marker = props.getProperty("move.config.migration.flying", "");
        if (!"session_44_done".equals(marker)) {
            INSTANCE.flyCloseToGround = false;
            INSTANCE.flyWhileOnGround = false;
            save();
            try (FileOutputStream out = new FileOutputStream(configFile.toFile(), true)) {
                Properties marker2 = new Properties();
                marker2.setProperty("move.config.migration.flying", "session_44_done");
                marker2.store(out, null);
            } catch (IOException ignored) {}
        }
        // 🔴 BUG-27 자동 마이그레이션 (Flying Phase / 세션 48): _flyControlVertical / _diveControlVertical
        //   default = true (원본 Unmodified default = true, Properties.java L171-L172).
        //   사용자 환경 disk 에 이전 false 저장됐을 가능성 → pitch 방향 W 진행 효과 무효 (BUG-27 직접 원인).
        //   마이그레이션 마커: move.config.migration.flying.vertical = "session_48_done".
        // 세션 49 재실행 강제 — 세션 48 마이그레이션 효과 없을 가능성 대응 (사용자 환경 disk
        //   에 marker 가 이미 있으면 skip 됐을 수 있음). 새 marker 로 한 번 더 강제.
        String marker48 = props.getProperty("move.config.migration.flying.vertical", "");
        if (!"session_49_done".equals(marker48)) {
            INSTANCE.flyControlVertical = true;
            INSTANCE.diveControlVertical = true;
            save();
            try (FileOutputStream out = new FileOutputStream(configFile.toFile(), true)) {
                Properties marker2 = new Properties();
                marker2.setProperty("move.config.migration.flying.vertical", "session_49_done");
                marker2.store(out, null);
            } catch (IOException ignored) {}
        }
        // 🔴 fence 클라이밍 자동 마이그레이션 — _freeFenceClimbing default = true 강제.
        //   원본 SmartMovingConfig L123 `_freeFenceClimbing = Unmodified("move.climb.free.fence")`
        //   → Properties.java L171-172 Unmodified default true. 우리 1.21.1 이식 초기 default 가
        //   false 로 잘못 저장돼 있는 환경 → fence 자체등반/위잡기 작동 안 함 (사용자 보고 #1, #3).
        //   BUG-26/27 동일 패턴: 마커 없으면 default true 강제 + save + 마커 추가.
        //   사용자가 마이그레이션 후 의도적으로 false 로 바꾸면 그건 존중 (1:1 옵션 보존).
        String fenceMarker = props.getProperty("move.config.migration.fence", "");
        if (!"session_104_done".equals(fenceMarker)) {
            INSTANCE.freeFenceClimbing = true;
            save();
            try (FileOutputStream out = new FileOutputStream(configFile.toFile(), true)) {
                Properties marker2 = new Properties();
                marker2.setProperty("move.config.migration.fence", "session_104_done");
                marker2.store(out, null);
            } catch (IOException ignored) {}
        }
        // 🔴 사다리/덩굴 자동 진입 마이그레이션 — _freeClimbingAutoLaddder /
        //   _freeClimbingAutoVine default = true 강제.
        //   원본 SmartMovingConfig L119-120 `Unmodified("move.climb.free.ladder.auto" /
        //   "move.climb.free.vine.auto")` → Properties.java L171-172 Unmodified default true.
        //   사용자 환경 disk 에 이전 false 저장됐을 가능성 → 사다리/덩굴 자동 진입
        //   (grab 키 없이 정면 사다리/덩굴 시) 작동 안 함.
        //   fence 마이그레이션 동일 패턴 — 마커 없으면 default true 강제.
        String autoLadderVineMarker = props.getProperty("move.config.migration.ladder.vine.auto", "");
        if (!"session_ladder_vine_auto_done".equals(autoLadderVineMarker)) {
            INSTANCE.freeClimbAutoLadder = true;
            INSTANCE.freeClimbAutoVine   = true;
            save();
            try (FileOutputStream out = new FileOutputStream(configFile.toFile(), true)) {
                Properties marker2 = new Properties();
                marker2.setProperty("move.config.migration.ladder.vine.auto", "session_ladder_vine_auto_done");
                marker2.store(out, null);
            } catch (IOException ignored) {}
        }
    }

    /**
     * 서버 수신 설정 인스턴스를 기본값으로 리셋한다.
     * 원본: SmartMovingServerConfig.reset() — properties/topProperties 저장소 clear.
     * 1.21.1: 단일 저장소 구조이므로 새 인스턴스로 교체하여 모든 필드를 기본값으로 복원.
     * 호출: 서버 연결 해제 시 (SmartMovingClient.DISCONNECT).
     */
    public static void resetServerConfig() {
        SERVER_CONFIG = new SmartMovingConfig();
    }

    public static void save() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        Properties props = new Properties();
        INSTANCE.writeTo(props);
        try (FileOutputStream out = new FileOutputStream(configFile.toFile())) {
            props.store(out, "SmartMoving configuration");
        } catch (IOException ignored) {}
    }

    /**
     * 서버로부터 수신한 flat String[] 배열 [k1,v1,k2,v2,...] 을 이 인스턴스에 적용한다.
     * 원본: SmartMovingServerConfig.loadFromProperties(String[] content, boolean blockCode)
     * 패리티 수준: 홀수 길이 배열은 malformed로 간주하고 처리 중단.
     */
    public void loadFromArray(String[] content) {
        if (content.length % 2 != 0) return;
        Properties props = new Properties();
        for (int i = 0; i + 1 < content.length; i += 2) {
            props.setProperty(content[i], content[i + 1]);
        }
        readFrom(props);
    }

    /**
     * 이 인스턴스의 설정을 flat String[] [k1,v1,k2,v2,...] 배열로 직렬화한다.
     * 원본: SmartMovingServerOptions.writeToProperties() 정상 경로 (config_system.md M-12)
     * 서버가 클라이언트에게 ConfigContent 패킷으로 전송할 때 사용한다.
     */
    public String[] toArray() {
        return toArray(null);
    }

    /**
     * 플레이어별 속도 치환 포함 직렬화.
     * 원본: SmartMovingServerOptions.writeToProperties(mp, key) 내부 iteration 중
     *       `entry.setValue(config._speedUserExponent.getValueString(userExponent))` 로직.
     * username != null + playerSpeedExponents 에 값 존재 시 `move.speed.user.exponent` 키 값을
     * 해당 플레이어 개인 지수로 치환. 전역 전송(username=null)은 모든 플레이어 공통값.
     */
    public String[] toArray(String username) {
        Properties props = new Properties();
        writeTo(props);
        if (username != null) {
            Integer userExponent = playerSpeedExponents.get(username);
            if (userExponent != null) {
                props.setProperty("move.speed.user.exponent", String.valueOf(userExponent));
            }
        }
        String[] result = new String[props.size() * 2];
        int i = 0;
        for (String key : props.stringPropertyNames()) {
            result[i++] = key;
            result[i++] = props.getProperty(key);
        }
        return result;
    }

    private void readFrom(Properties p) {
        enabled                  = getBool(p,   "move.enabled",                   enabled);
        // H-20 (세션 23): 게임타입별 6키 (move.config.{survival,creative,adventure}.keys{,.default})
        // 읽기 삭제. 필드/메서드 제거와 함께. 기존 세이브 파일의 해당 키는 무시됨.
        speedFactor              = getFloat(p,  "move.speed.factor",              speedFactor);
        speedUser                = getBool(p,   "move.speed.user",                speedUser);
        speedUserFactor          = getFloat(p,  "move.speed.user.factor",         speedUserFactor);
        speedUserExponent        = getInt(p,    "move.speed.user.exponent",       speedUserExponent);
        playerSpeedExponents     = parseIntegerMap(p.getProperty("move.speed.users.exponents"));
        vanillaStyle             = getBool(p,   "move.general.vanilla",           vanillaStyle);
        sneakFactor              = getFloat(p,  "move.sneak.factor",              sneakFactor);
        crawlFactor              = getFloat(p,  "move.crawl.factor",              crawlFactor);
        runFactor                = getFloat(p,  "move.run.factor",                runFactor);
        sprintFactor             = getFloat(p,  "move.sprint.factor",             sprintFactor);
        sprintFactorLevitate         = getFloat(p, "move.sprint.factor.levitate",          sprintFactorLevitate);
        sprintFactorLevitateVertical = getFloat(p, "move.sprint.factor.levitate.vertical", sprintFactorLevitateVertical);
        baseClimb                = getBool(p,   "move.climb.base",                baseClimb);
        freeClimb                = getBool(p,   "move.climb.free",                freeClimb);
        simpleClimb              = getBool(p,   "move.climb.simple",              simpleClimb);
        smartClimb               = getBool(p,   "move.climb.smart",               smartClimb);
        freeClimbingUpSpeedFactor   = getFloat(p, "move.climb.free.up.factor",   freeClimbingUpSpeedFactor);
        freeClimbingDownSpeedFactor = getFloat(p, "move.climb.free.down.factor", freeClimbingDownSpeedFactor);
        freeClimbingHorizontalSpeedFactor = getFloat(p, "move.climb.free.horizontal.speed.factor", freeClimbingHorizontalSpeedFactor);
        freeOneLadderClimbUpSpeedFactor   = getFloat(p, "move.climb.free.ladder.one.up.speed.factor",  freeOneLadderClimbUpSpeedFactor);
        freeBothLadderClimbUpSpeedFactor  = getFloat(p, "move.climb.free.ladder.two.up.speed.factor",  freeBothLadderClimbUpSpeedFactor);
        freeBaseLadderClimb = getBool(p, "move.climb.free.base.ladder", freeBaseLadderClimb);
        freeBaseVineClimb   = getBool(p, "move.climb.free.base.vine",   freeBaseVineClimb);
        freeFenceClimbing = getBool(p, "move.climb.free.fence", freeFenceClimbing);
        freeClimbingOrthogonalDirectionAngle = getFloat(p, "move.climb.free.direction.orthogonal.angle", freeClimbingOrthogonalDirectionAngle);
        freeClimbingDiagonalDirectionAngle   = getFloat(p, "move.climb.free.direction.diagonal.angle",   freeClimbingDiagonalDirectionAngle);
        ceilingClimbingSpeedFactor = getFloat(p, "move.climb.ceiling.speed.factor", ceilingClimbingSpeedFactor);
        freeClimbFallDamageStartDistance = getFloat(p, "move.climb.free.fall.damage.start", freeClimbFallDamageStartDistance);
        freeClimbFallDamageFactor        = getFloat(p, "move.climb.free.fall.damage.factor", freeClimbFallDamageFactor);
        climbJumpBackHead        = getBool(p,   "move.climb.jump.back.head",      climbJumpBackHead);
        climbExhaustion          = getBool(p,   "move.climb.exhaustion",          climbExhaustion);
        climbExhaustionStart     = getFloat(p,  "move.climb.exhaustion.start",    climbExhaustionStart);
        climbExhaustionStop      = getFloat(p,  "move.climb.exhaustion.stop",     climbExhaustionStop);
        ceilingClimbExhaustion   = getBool(p,   "move.climb.ceiling.exhaustion",  ceilingClimbExhaustion);
        ceilingClimbExhaustionStart = getFloat(p, "move.climb.ceiling.exhaustion.start", ceilingClimbExhaustionStart);
        ceilingClimbExhaustionStop  = getFloat(p, "move.climb.ceiling.exhaustion.stop",  ceilingClimbExhaustionStop);
        swim                     = getBool(p,   "move.swim",                      swim);
        swimSpeedFactor          = getFloat(p,  "move.swim.speed.factor",         swimSpeedFactor);
        dive                     = getBool(p,   "move.dive",                      dive);
        diveSpeedFactor          = getFloat(p,  "move.dive.speed.factor",         diveSpeedFactor);
        swimDownOnSneak          = getBool(p,   "move.swim.down.sneak",           swimDownOnSneak);
        diveDownOnSneak          = getBool(p,   "move.dive.down.sneak",           diveDownOnSneak);
        lavaLikeWater            = getBool(p,   "move.lava.water",                lavaLikeWater);
        lavaSwimParticlePeriodFactor = getFloat(p, "move.lava.swim.particle.period.factor", lavaSwimParticlePeriodFactor);
        runOnSprintRelease       = getBool(p,   "move.sprint.key.release.run",    runOnSprintRelease);
        walkOnSprintRelease      = getBool(p,   "move.sprint.key.release.walk",   walkOnSprintRelease);
        levitateSmall            = getBool(p,   "move.levitate.small",            levitateSmall);
        jumpControlFactor              = getFloat(p, "move.jump.control.factor",          jumpControlFactor);
        jumpHorizontalFactor           = getFloat(p, "move.jump.horizontal.factor",       jumpHorizontalFactor);
        jumpVerticalFactor             = getFloat(p, "move.jump.vertical.factor",         jumpVerticalFactor);
        standJump                      = getBool(p,  "move.jump.stand",                   standJump);
        standJumpVerticalFactor        = getFloat(p, "move.jump.stand.vertical.factor",   standJumpVerticalFactor);
        sneakJump                      = getBool(p,  "move.jump.sneak",                   sneakJump);
        sneakJumpHorizontalFactor      = getFloat(p, "move.jump.sneak.horizontal.factor", sneakJumpHorizontalFactor);
        sneakJumpVerticalFactor        = getFloat(p, "move.jump.sneak.vertical.factor",   sneakJumpVerticalFactor);
        walkJump                       = getBool(p,  "move.jump.walk",                    walkJump);
        walkJumpHorizontalFactor       = getFloat(p, "move.jump.walk.horizontal.factor",  walkJumpHorizontalFactor);
        walkJumpVerticalFactor         = getFloat(p, "move.jump.walk.vertical.factor",    walkJumpVerticalFactor);
        runJump                        = getBool(p,  "move.jump.run",                     runJump);
        runJumpHorizontalFactor        = getFloat(p, "move.jump.run.horizontal.factor",   runJumpHorizontalFactor);
        runJumpVerticalFactor          = getFloat(p, "move.jump.run.vertical.factor",     runJumpVerticalFactor);
        sprintJump                     = getBool(p,  "move.jump.sprint",                  sprintJump);
        sprintJumpHorizontalFactor     = getFloat(p, "move.jump.sprint.horizontal.factor", sprintJumpHorizontalFactor);
        sprintJumpVerticalFactor       = getFloat(p, "move.jump.sprint.vertical.factor",  sprintJumpVerticalFactor);
        wallUpJump                     = getBool(p,  "move.jump.wall",                     wallUpJump);
        wallHeadJump                   = getBool(p,  "move.jump.wall.head",                wallHeadJump);
        wallUpJumpFallMaximumDistance  = getFloat(p, "move.jump.wall.fall.maximum.distance", wallUpJumpFallMaximumDistance);
        wallHeadJumpFallMaximumDistance = getFloat(p, "move.jump.wall.head.fall.maximum.distance", wallHeadJumpFallMaximumDistance);
        wallUpJumpOrthogonalTolerance   = getFloat(p, "move.jump.wall.orthogonal.tolerance", wallUpJumpOrthogonalTolerance);
        wallUpJumpVerticalFactor       = getFloat(p, "move.jump.wall.vertical.factor",     wallUpJumpVerticalFactor);
        wallHeadJumpVerticalFactor     = getFloat(p, "move.jump.wall.head.vertical.factor", wallHeadJumpVerticalFactor);
        wallUpJumpHorizontalFactor     = getFloat(p, "move.jump.wall.horizontal.factor",   wallUpJumpHorizontalFactor);
        wallHeadJumpHorizontalFactor   = getFloat(p, "move.jump.wall.head.horizontal.factor", wallHeadJumpHorizontalFactor);
        jumpCharge               = getBool(p,   "move.jump.charge",               jumpCharge);
        jumpChargeMaximum        = getFloat(p,  "move.jump.charge.maximum",       jumpChargeMaximum);
        jumpChargeFactor         = getFloat(p,  "move.jump.charge.factor",        jumpChargeFactor);
        jumpChargeCancelOnSneakRelease = getBool(p, "move.jump.charge.sneak.release.cancel", jumpChargeCancelOnSneakRelease);
        headJump                 = getBool(p,   "move.jump.head.charge",          headJump);
        headJumpControlFactor    = getFloat(p,  "move.forward.jump.control.factor", headJumpControlFactor);
        headJumpChargeMaximum    = getFloat(p,  "move.forward.jump.charge.maximum", headJumpChargeMaximum);
        headFallDamageStartDistance = getFloat(p, "move.fall.head.damage.start.distance", headFallDamageStartDistance);
        headFallDamageFactor     = getFloat(p,  "move.fall.head.damage.factor",   headFallDamageFactor);
        angleJumpSide            = getBool(p,   "move.jump.angle.side",           angleJumpSide);
        angleJumpBack            = getBool(p,   "move.jump.angle.back",           angleJumpBack);
        angleJumpHorizontalFactor = getFloat(p, "move.jump.angle.horizontal.factor", angleJumpHorizontalFactor);
        angleJumpVerticalFactor  = getFloat(p,  "move.jump.angle.vertical.factor", angleJumpVerticalFactor);
        climbUpJump              = getBool(p,   "move.jump.climb.up",             climbUpJump);
        climbUpJumpVerticalFactor = getFloat(p, "move.jump.climb.up.vertical.factor", climbUpJumpVerticalFactor);
        climbUpJumpHandsOnlyVerticalFactor = getFloat(p, "move.jump.climb.up.hands.only.vertical.factor", climbUpJumpHandsOnlyVerticalFactor);
        climbBackUpJump          = getBool(p,   "move.jump.climb.back.up",        climbBackUpJump);
        climbBackUpJumpVerticalFactor = getFloat(p, "move.jump.climb.back.up.vertical.factor", climbBackUpJumpVerticalFactor);
        climbBackUpJumpHorizontalFactor = getFloat(p, "move.jump.climb.back.up.horizontal.factor", climbBackUpJumpHorizontalFactor);
        climbBackUpJumpHandsOnlyVerticalFactor = getFloat(p, "move.jump.climb.back.up.hands.only.vertical.factor", climbBackUpJumpHandsOnlyVerticalFactor);
        climbBackUpJumpHandsOnlyHorizontalFactor = getFloat(p, "move.jump.climb.back.up.hands.only.horizontal.factor", climbBackUpJumpHandsOnlyHorizontalFactor);
        climbBackHeadJump        = getBool(p,   "move.jump.climb.back.head",      climbBackHeadJump);
        climbBackHeadJumpVerticalFactor = getFloat(p, "move.jump.climb.back.head.vertical.factor", climbBackHeadJumpVerticalFactor);
        climbBackHeadJumpHorizontalFactor = getFloat(p, "move.jump.climb.back.head.horizontal.factor", climbBackHeadJumpHorizontalFactor);
        climbBackHeadJumpHandsOnlyVerticalFactor = getFloat(p, "move.jump.climb.back.head.hands.only.vertical.factor", climbBackHeadJumpHandsOnlyVerticalFactor);
        climbBackHeadJumpHandsOnlyHorizontalFactor = getFloat(p, "move.jump.climb.back.head.hands.only.horizontal.factor", climbBackHeadJumpHandsOnlyHorizontalFactor);
        angleJumpDoubleClickTicks = getFloat(p, "move.jump.angle.double.click.ticks", angleJumpDoubleClickTicks);
        wallJumpDoubleClick      = getBool(p,   "move.jump.wall.double.click",    wallJumpDoubleClick);
        wallJumpDoubleClickTicks = getFloat(p,  "move.jump.wall.double.click.ticks", wallJumpDoubleClickTicks);
        slideSlipperinessFactor  = getFloat(p,  "move.slide.slipperiness.factor", slideSlipperinessFactor);
        slideParticlePeriodFactor = getFloat(p, "move.slide.particle.period.factor", slideParticlePeriodFactor);
        slidingSpeedStopFactor   = getFloat(p,  "move.slide.speed.stop.factor",  slidingSpeedStopFactor);
        slideControlDegrees      = getFloat(p,  "move.slide.control.angle",      slideControlDegrees);
        flyingSpeedFactor        = getFloat(p,  "move.fly.speed.factor",          flyingSpeedFactor);
        flyControlVertical       = getBool(p,   "move.fly.control.vertical",      flyControlVertical);
        diveControlVertical      = getBool(p,   "move.dive.control.vertical",     diveControlVertical);
        flyCloseToGround         = getBool(p,   "move.fly.ground.close",          flyCloseToGround);
        flyWhileOnGround         = getBool(p,   "move.fly.ground.collide",        flyWhileOnGround);
        fly                      = getBool(p,   "move.fly",                       fly);
        slide                    = getBool(p,   "move.slide",                     slide);
        crawl                    = getBool(p,   "move.crawl",                     crawl);
        crawlOverEdge            = getBool(p,   "move.crawl.edge",                crawlOverEdge);
        crawlToggle              = getBool(p,   "move.crawl.toggle",              crawlToggle);
        sneakToggle              = getBool(p,   "move.sneak.toggle",              sneakToggle);
        sneakNameTag             = getBool(p,   "move.sneak.name",                sneakNameTag);
        crawlNameTag             = getBool(p,   "move.crawl.name",                crawlNameTag);
        displayExhaustionBar     = getBool(p,   "move.gui.exhaustion.bar",        displayExhaustionBar);
        displayJumpChargeBar     = getBool(p,   "move.gui.jump.charge.bar",       displayJumpChargeBar);
        sneak                    = getBool(p,   "move.sneak",                     sneak);
        run                      = getBool(p,   "move.run",                       run);
        sprint                   = getBool(p,   "move.sprint",                    sprint);
        ceilingClimbing          = getBool(p,   "move.climb.ceiling",             ceilingClimbing);
        perspectiveFadeFactor    = getFloat(p,  "move.perspective.fade.factor",   perspectiveFadeFactor);
        perspectiveSpeedFactor   = getFloat(p,  "move.perspective.speed.factor",  perspectiveSpeedFactor);
        perspectiveSpeedFactorMax= getFloat(p,  "move.perspective.speed.factor.max", perspectiveSpeedFactorMax);
        perspectiveRunFactor     = getFloat(p,  "move.perspective.run.factor",    perspectiveRunFactor);
        perspectiveSprintFactor  = getFloat(p,  "move.perspective.sprint.factor", perspectiveSprintFactor);
        // Exhaustion/Hunger factor 1단계 (원본 getFactor L563-L575). ⚠️ Exhaution 오타 유지.
        baseExhautionLossFactor       = getFloat(p, "move.exhaustion.loss.factor",       baseExhautionLossFactor);
        baseHungerGainFactor          = getFloat(p, "move.hunger.gain.factor",           baseHungerGainFactor);
        fallExhautionLossFactor       = getFloat(p, "move.exhaustion.fall.loss.factor",  fallExhautionLossFactor);
        sprintingHungerGainFactor     = getFloat(p, "move.hunger.sprint.gain.factor",    sprintingHungerGainFactor);
        sprintingExhautionLossFactor  = getFloat(p, "move.exhaustion.sprint.loss.factor", sprintingExhautionLossFactor);
        runningHungerGainFactor       = getFloat(p, "move.hunger.run.gain.factor",       runningHungerGainFactor);
        runningExhautionLossFactor    = getFloat(p, "move.exhaustion.run.loss.factor",   runningExhautionLossFactor);
        sneakingHungerGainFactor      = getFloat(p, "move.hunger.sneak.gain.factor",     sneakingHungerGainFactor);
        sneakingExhautionLossFactor   = getFloat(p, "move.exhaustion.sneak.loss.factor", sneakingExhautionLossFactor);
        standingHungerGainFactor      = getFloat(p, "move.hunger.stand.gain.factor",     standingHungerGainFactor);
        standingExhautionLossFactor   = getFloat(p, "move.exhaustion.stand.loss.factor", standingExhautionLossFactor);
        walkingHungerGainFactor       = getFloat(p, "move.hunger.walk.gain.factor",      walkingHungerGainFactor);
        walkingExhautionLossFactor    = getFloat(p, "move.exhaustion.walk.loss.factor",  walkingExhautionLossFactor);
        // Exhaustion/Hunger factor 2단계 (원본 getFactor L577-L592). 2단계는 `Exhaustion` 정타.
        // 키 순서 이상 주의: ceilClimbing 은 `climb.gain.ceiling` / `climb.ceiling.loss` (원본).
        climbingHungerGainFactor      = getFloat(p, "move.hunger.climb.gain.factor",         climbingHungerGainFactor);
        climbingExhaustionLossFactor  = getFloat(p, "move.exhaustion.climb.loss.factor",     climbingExhaustionLossFactor);
        crawlingHungerGainFactor      = getFloat(p, "move.hunger.crawl.gain.factor",         crawlingHungerGainFactor);
        crawlingExhaustionLossFactor  = getFloat(p, "move.exhaustion.crawl.loss.factor",     crawlingExhaustionLossFactor);
        ceilClimbingHungerGainFactor  = getFloat(p, "move.hunger.climb.gain.ceiling.factor", ceilClimbingHungerGainFactor);
        ceilClimbingExhaustionLossFactor = getFloat(p, "move.exhaustion.climb.ceiling.loss.factor", ceilClimbingExhaustionLossFactor);
        swimmingHungerGainFactor      = getFloat(p, "move.hunger.swim.gain.factor",          swimmingHungerGainFactor);
        swimmingExhaustionLossFactor  = getFloat(p, "move.exhaustion.swim.loss.factor",      swimmingExhaustionLossFactor);
        divingHungerGainFactor        = getFloat(p, "move.hunger.dive.gain.factor",          divingHungerGainFactor);
        divingExhaustionLossFactor    = getFloat(p, "move.exhaustion.dive.loss.factor",      divingExhaustionLossFactor);
        dippingHungerGainFactor       = getFloat(p, "move.hunger.dip.gain.factor",           dippingHungerGainFactor);
        dippingExhaustionLossFactor   = getFloat(p, "move.exhaustion.dip.loss.factor",       dippingExhaustionLossFactor);
        normalHungerGainFactor        = getFloat(p, "move.hunger.normal.gain.factor",        normalHungerGainFactor);
        normalExhaustionLossFactor    = getFloat(p, "move.exhaustion.normal.loss.factor",    normalExhaustionLossFactor);
        // handleExhaustion 기타 (원본 SmartMovingSelf L863/L893). Easy 값 default.
        alwaysHungerGain              = getFloat(p, "move.hunger.always.gain",               alwaysHungerGain);
        exhaustionLossHungerFactor    = getFloat(p, "move.exhaustion.hunger.factor",         exhaustionLossHungerFactor);
    }

    private void writeTo(Properties p) {
        p.setProperty("move.enabled",                    String.valueOf(enabled));
        // H-20 (세션 23): 게임타입별 6키 writeTo 삭제 (readFrom 과 대칭).
        p.setProperty("move.speed.factor",               String.valueOf(speedFactor));
        p.setProperty("move.speed.user",                 String.valueOf(speedUser));
        p.setProperty("move.speed.user.factor",          String.valueOf(speedUserFactor));
        p.setProperty("move.speed.user.exponent",        String.valueOf(speedUserExponent));
        p.setProperty("move.speed.users.exponents",      serializeIntegerMap(playerSpeedExponents));
        p.setProperty("move.general.vanilla",            String.valueOf(vanillaStyle));
        p.setProperty("move.sneak.factor",               String.valueOf(sneakFactor));
        p.setProperty("move.crawl.factor",               String.valueOf(crawlFactor));
        p.setProperty("move.run.factor",                 String.valueOf(runFactor));
        p.setProperty("move.sprint.factor",              String.valueOf(sprintFactor));
        p.setProperty("move.sprint.factor.levitate",          String.valueOf(sprintFactorLevitate));
        p.setProperty("move.sprint.factor.levitate.vertical", String.valueOf(sprintFactorLevitateVertical));
        p.setProperty("move.climb.base",                 String.valueOf(baseClimb));
        p.setProperty("move.climb.free",                 String.valueOf(freeClimb));
        p.setProperty("move.climb.simple",               String.valueOf(simpleClimb));
        p.setProperty("move.climb.smart",                String.valueOf(smartClimb));
        p.setProperty("move.climb.free.up.factor",       String.valueOf(freeClimbingUpSpeedFactor));
        p.setProperty("move.climb.free.down.factor",     String.valueOf(freeClimbingDownSpeedFactor));
        p.setProperty("move.climb.free.horizontal.speed.factor", String.valueOf(freeClimbingHorizontalSpeedFactor));
        p.setProperty("move.climb.free.ladder.one.up.speed.factor", String.valueOf(freeOneLadderClimbUpSpeedFactor));
        p.setProperty("move.climb.free.ladder.two.up.speed.factor", String.valueOf(freeBothLadderClimbUpSpeedFactor));
        p.setProperty("move.climb.free.base.ladder", String.valueOf(freeBaseLadderClimb));
        p.setProperty("move.climb.free.base.vine",   String.valueOf(freeBaseVineClimb));
        p.setProperty("move.climb.free.fence", String.valueOf(freeFenceClimbing));
        p.setProperty("move.climb.free.direction.orthogonal.angle", String.valueOf(freeClimbingOrthogonalDirectionAngle));
        p.setProperty("move.climb.free.direction.diagonal.angle",   String.valueOf(freeClimbingDiagonalDirectionAngle));
        p.setProperty("move.climb.ceiling.speed.factor", String.valueOf(ceilingClimbingSpeedFactor));
        p.setProperty("move.climb.free.fall.damage.start", String.valueOf(freeClimbFallDamageStartDistance));
        p.setProperty("move.climb.free.fall.damage.factor", String.valueOf(freeClimbFallDamageFactor));
        p.setProperty("move.climb.jump.back.head",       String.valueOf(climbJumpBackHead));
        p.setProperty("move.climb.exhaustion",           String.valueOf(climbExhaustion));
        p.setProperty("move.climb.exhaustion.start",     String.valueOf(climbExhaustionStart));
        p.setProperty("move.climb.exhaustion.stop",      String.valueOf(climbExhaustionStop));
        p.setProperty("move.climb.ceiling.exhaustion",   String.valueOf(ceilingClimbExhaustion));
        p.setProperty("move.climb.ceiling.exhaustion.start", String.valueOf(ceilingClimbExhaustionStart));
        p.setProperty("move.climb.ceiling.exhaustion.stop",  String.valueOf(ceilingClimbExhaustionStop));
        p.setProperty("move.swim",                       String.valueOf(swim));
        p.setProperty("move.swim.speed.factor",          String.valueOf(swimSpeedFactor));
        p.setProperty("move.dive",                       String.valueOf(dive));
        p.setProperty("move.dive.speed.factor",          String.valueOf(diveSpeedFactor));
        p.setProperty("move.swim.down.sneak",            String.valueOf(swimDownOnSneak));
        p.setProperty("move.dive.down.sneak",            String.valueOf(diveDownOnSneak));
        p.setProperty("move.lava.water",                 String.valueOf(lavaLikeWater));
        p.setProperty("move.lava.swim.particle.period.factor", String.valueOf(lavaSwimParticlePeriodFactor));
        p.setProperty("move.sprint.key.release.run",     String.valueOf(runOnSprintRelease));
        p.setProperty("move.sprint.key.release.walk",    String.valueOf(walkOnSprintRelease));
        p.setProperty("move.levitate.small",             String.valueOf(levitateSmall));
        p.setProperty("move.jump.control.factor",            String.valueOf(jumpControlFactor));
        p.setProperty("move.jump.horizontal.factor",         String.valueOf(jumpHorizontalFactor));
        p.setProperty("move.jump.vertical.factor",           String.valueOf(jumpVerticalFactor));
        p.setProperty("move.jump.stand",                     String.valueOf(standJump));
        p.setProperty("move.jump.stand.vertical.factor",     String.valueOf(standJumpVerticalFactor));
        p.setProperty("move.jump.sneak",                     String.valueOf(sneakJump));
        p.setProperty("move.jump.sneak.horizontal.factor",   String.valueOf(sneakJumpHorizontalFactor));
        p.setProperty("move.jump.sneak.vertical.factor",     String.valueOf(sneakJumpVerticalFactor));
        p.setProperty("move.jump.walk",                      String.valueOf(walkJump));
        p.setProperty("move.jump.walk.horizontal.factor",    String.valueOf(walkJumpHorizontalFactor));
        p.setProperty("move.jump.walk.vertical.factor",      String.valueOf(walkJumpVerticalFactor));
        p.setProperty("move.jump.run",                       String.valueOf(runJump));
        p.setProperty("move.jump.run.horizontal.factor",     String.valueOf(runJumpHorizontalFactor));
        p.setProperty("move.jump.run.vertical.factor",       String.valueOf(runJumpVerticalFactor));
        p.setProperty("move.jump.sprint",                    String.valueOf(sprintJump));
        p.setProperty("move.jump.sprint.horizontal.factor",  String.valueOf(sprintJumpHorizontalFactor));
        p.setProperty("move.jump.sprint.vertical.factor",    String.valueOf(sprintJumpVerticalFactor));
        p.setProperty("move.jump.wall",                      String.valueOf(wallUpJump));
        p.setProperty("move.jump.wall.head",                 String.valueOf(wallHeadJump));
        p.setProperty("move.jump.wall.fall.maximum.distance", String.valueOf(wallUpJumpFallMaximumDistance));
        p.setProperty("move.jump.wall.head.fall.maximum.distance", String.valueOf(wallHeadJumpFallMaximumDistance));
        p.setProperty("move.jump.wall.orthogonal.tolerance", String.valueOf(wallUpJumpOrthogonalTolerance));
        p.setProperty("move.jump.wall.vertical.factor",      String.valueOf(wallUpJumpVerticalFactor));
        p.setProperty("move.jump.wall.head.vertical.factor", String.valueOf(wallHeadJumpVerticalFactor));
        p.setProperty("move.jump.wall.horizontal.factor",    String.valueOf(wallUpJumpHorizontalFactor));
        p.setProperty("move.jump.wall.head.horizontal.factor", String.valueOf(wallHeadJumpHorizontalFactor));
        p.setProperty("move.jump.charge",                String.valueOf(jumpCharge));
        p.setProperty("move.jump.charge.maximum",        String.valueOf(jumpChargeMaximum));
        p.setProperty("move.jump.charge.factor",         String.valueOf(jumpChargeFactor));
        p.setProperty("move.jump.charge.sneak.release.cancel", String.valueOf(jumpChargeCancelOnSneakRelease));
        p.setProperty("move.jump.head.charge",           String.valueOf(headJump));
        p.setProperty("move.forward.jump.control.factor", String.valueOf(headJumpControlFactor));
        p.setProperty("move.forward.jump.charge.maximum", String.valueOf(headJumpChargeMaximum));
        p.setProperty("move.fall.head.damage.start.distance", String.valueOf(headFallDamageStartDistance));
        p.setProperty("move.fall.head.damage.factor",    String.valueOf(headFallDamageFactor));
        p.setProperty("move.jump.angle.side",            String.valueOf(angleJumpSide));
        p.setProperty("move.jump.angle.back",            String.valueOf(angleJumpBack));
        p.setProperty("move.jump.angle.horizontal.factor", String.valueOf(angleJumpHorizontalFactor));
        p.setProperty("move.jump.angle.vertical.factor", String.valueOf(angleJumpVerticalFactor));
        p.setProperty("move.jump.climb.up",              String.valueOf(climbUpJump));
        p.setProperty("move.jump.climb.up.vertical.factor", String.valueOf(climbUpJumpVerticalFactor));
        p.setProperty("move.jump.climb.up.hands.only.vertical.factor", String.valueOf(climbUpJumpHandsOnlyVerticalFactor));
        p.setProperty("move.jump.climb.back.up",         String.valueOf(climbBackUpJump));
        p.setProperty("move.jump.climb.back.up.vertical.factor", String.valueOf(climbBackUpJumpVerticalFactor));
        p.setProperty("move.jump.climb.back.up.horizontal.factor", String.valueOf(climbBackUpJumpHorizontalFactor));
        p.setProperty("move.jump.climb.back.up.hands.only.vertical.factor", String.valueOf(climbBackUpJumpHandsOnlyVerticalFactor));
        p.setProperty("move.jump.climb.back.up.hands.only.horizontal.factor", String.valueOf(climbBackUpJumpHandsOnlyHorizontalFactor));
        p.setProperty("move.jump.climb.back.head",       String.valueOf(climbBackHeadJump));
        p.setProperty("move.jump.climb.back.head.vertical.factor", String.valueOf(climbBackHeadJumpVerticalFactor));
        p.setProperty("move.jump.climb.back.head.horizontal.factor", String.valueOf(climbBackHeadJumpHorizontalFactor));
        p.setProperty("move.jump.climb.back.head.hands.only.vertical.factor", String.valueOf(climbBackHeadJumpHandsOnlyVerticalFactor));
        p.setProperty("move.jump.climb.back.head.hands.only.horizontal.factor", String.valueOf(climbBackHeadJumpHandsOnlyHorizontalFactor));
        p.setProperty("move.jump.angle.double.click.ticks", String.valueOf(angleJumpDoubleClickTicks));
        p.setProperty("move.jump.wall.double.click",     String.valueOf(wallJumpDoubleClick));
        p.setProperty("move.jump.wall.double.click.ticks", String.valueOf(wallJumpDoubleClickTicks));
        p.setProperty("move.slide.slipperiness.factor",  String.valueOf(slideSlipperinessFactor));
        p.setProperty("move.slide.particle.period.factor", String.valueOf(slideParticlePeriodFactor));
        p.setProperty("move.slide.speed.stop.factor",    String.valueOf(slidingSpeedStopFactor));
        p.setProperty("move.slide.control.angle",        String.valueOf(slideControlDegrees));
        p.setProperty("move.fly.speed.factor",           String.valueOf(flyingSpeedFactor));
        p.setProperty("move.fly.control.vertical",       String.valueOf(flyControlVertical));
        p.setProperty("move.dive.control.vertical",      String.valueOf(diveControlVertical));
        p.setProperty("move.fly.ground.close",           String.valueOf(flyCloseToGround));
        p.setProperty("move.fly.ground.collide",         String.valueOf(flyWhileOnGround));
        p.setProperty("move.fly",                        String.valueOf(fly));
        p.setProperty("move.slide",                      String.valueOf(slide));
        p.setProperty("move.crawl",                      String.valueOf(crawl));
        p.setProperty("move.crawl.edge",                 String.valueOf(crawlOverEdge));
        p.setProperty("move.crawl.toggle",               String.valueOf(crawlToggle));
        p.setProperty("move.sneak.toggle",               String.valueOf(sneakToggle));
        p.setProperty("move.sneak.name",                 String.valueOf(sneakNameTag));
        p.setProperty("move.crawl.name",                 String.valueOf(crawlNameTag));
        p.setProperty("move.gui.exhaustion.bar",         String.valueOf(displayExhaustionBar));
        p.setProperty("move.gui.jump.charge.bar",        String.valueOf(displayJumpChargeBar));
        p.setProperty("move.sneak",                      String.valueOf(sneak));
        p.setProperty("move.run",                        String.valueOf(run));
        p.setProperty("move.sprint",                     String.valueOf(sprint));
        p.setProperty("move.climb.ceiling",              String.valueOf(ceilingClimbing));
        p.setProperty("move.perspective.fade.factor",    String.valueOf(perspectiveFadeFactor));
        p.setProperty("move.perspective.speed.factor",   String.valueOf(perspectiveSpeedFactor));
        p.setProperty("move.perspective.speed.factor.max", String.valueOf(perspectiveSpeedFactorMax));
        p.setProperty("move.perspective.run.factor",     String.valueOf(perspectiveRunFactor));
        p.setProperty("move.perspective.sprint.factor",  String.valueOf(perspectiveSprintFactor));
        // Exhaustion/Hunger factor 1단계 (원본 getFactor L563-L575). ⚠️ Exhaution 오타 유지.
        p.setProperty("move.exhaustion.loss.factor",         String.valueOf(baseExhautionLossFactor));
        p.setProperty("move.hunger.gain.factor",             String.valueOf(baseHungerGainFactor));
        p.setProperty("move.exhaustion.fall.loss.factor",    String.valueOf(fallExhautionLossFactor));
        p.setProperty("move.hunger.sprint.gain.factor",      String.valueOf(sprintingHungerGainFactor));
        p.setProperty("move.exhaustion.sprint.loss.factor",  String.valueOf(sprintingExhautionLossFactor));
        p.setProperty("move.hunger.run.gain.factor",         String.valueOf(runningHungerGainFactor));
        p.setProperty("move.exhaustion.run.loss.factor",     String.valueOf(runningExhautionLossFactor));
        p.setProperty("move.hunger.sneak.gain.factor",       String.valueOf(sneakingHungerGainFactor));
        p.setProperty("move.exhaustion.sneak.loss.factor",   String.valueOf(sneakingExhautionLossFactor));
        p.setProperty("move.hunger.stand.gain.factor",       String.valueOf(standingHungerGainFactor));
        p.setProperty("move.exhaustion.stand.loss.factor",   String.valueOf(standingExhautionLossFactor));
        p.setProperty("move.hunger.walk.gain.factor",        String.valueOf(walkingHungerGainFactor));
        p.setProperty("move.exhaustion.walk.loss.factor",    String.valueOf(walkingExhautionLossFactor));
        // Exhaustion/Hunger factor 2단계 (원본 getFactor L577-L592). 2단계는 `Exhaustion` 정타.
        p.setProperty("move.hunger.climb.gain.factor",           String.valueOf(climbingHungerGainFactor));
        p.setProperty("move.exhaustion.climb.loss.factor",       String.valueOf(climbingExhaustionLossFactor));
        p.setProperty("move.hunger.crawl.gain.factor",           String.valueOf(crawlingHungerGainFactor));
        p.setProperty("move.exhaustion.crawl.loss.factor",       String.valueOf(crawlingExhaustionLossFactor));
        p.setProperty("move.hunger.climb.gain.ceiling.factor",   String.valueOf(ceilClimbingHungerGainFactor));
        p.setProperty("move.exhaustion.climb.ceiling.loss.factor", String.valueOf(ceilClimbingExhaustionLossFactor));
        p.setProperty("move.hunger.swim.gain.factor",            String.valueOf(swimmingHungerGainFactor));
        p.setProperty("move.exhaustion.swim.loss.factor",        String.valueOf(swimmingExhaustionLossFactor));
        p.setProperty("move.hunger.dive.gain.factor",            String.valueOf(divingHungerGainFactor));
        p.setProperty("move.exhaustion.dive.loss.factor",        String.valueOf(divingExhaustionLossFactor));
        p.setProperty("move.hunger.dip.gain.factor",             String.valueOf(dippingHungerGainFactor));
        p.setProperty("move.exhaustion.dip.loss.factor",         String.valueOf(dippingExhaustionLossFactor));
        p.setProperty("move.hunger.normal.gain.factor",          String.valueOf(normalHungerGainFactor));
        p.setProperty("move.exhaustion.normal.loss.factor",      String.valueOf(normalExhaustionLossFactor));
        // handleExhaustion 기타 (원본 SmartMovingSelf L863/L893).
        p.setProperty("move.hunger.always.gain",                 String.valueOf(alwaysHungerGain));
        p.setProperty("move.exhaustion.hunger.factor",           String.valueOf(exhaustionLossHungerFactor));
    }

    /** getUserSpeedFactor() 공식: (1 + speedUserFactor)^speedUserExponent */
    public float getUserSpeedFactor() {
        // 원본: isUserSpeedAlwaysDefault() = !speedUser || speedUserFactor==1F
        if (!speedUser || speedUserFactor == 1F || speedUserExponent == 0) return 1F;
        return (float) Math.pow(1F + speedUserFactor, speedUserExponent);
    }

    /**
     * 원본 `SmartMovingClientConfig.getFactor(hunger, 14상태)` L554-L595 1:1 이식.
     * 허기/소진 배율 2단계 공식. 호출자는 `handleExhaustion` L862(hunger=true) /
     * L888(hunger=false) 에서 현재 이동/행동 상태를 전달.
     *
     * **원본 본체** (focus_05 §5.4 참조, SmartMovingClientConfig.java L554-L595):
     *
     * <pre>
     * isClimbing |= isClimbCrawling;
     * isCrawling |= isCrawlClimbing;
     * boolean actionOverGound = isClimbing || isCeilingClimbing || isDiving || isSwimming;
     * boolean airBorne = !onGround && !actionOverGound;
     * isStanding = actionOverGound ? isStill : isStanding;
     * isSneaking = isSneaking & !isStanding;
     *
     * float factor = hunger ? _baseHungerGainFactor : _baseExhautionLossFactor;
     * // 1단계 — 이동속도 배율 (airBorne / sprinting / running / sneaking / standing / walking)
     * // ⚠️ airBorne + hunger 는 0F 하드코딩 (대응 필드 없음)
     * // 2단계 — 행동 배율 (climbing / crawling / ceilClimbing / swimming / diving / dipping / normal)
     * // ⚠️ L589 onGround + L591 else 모두 normal 참조 (의도된 중복)
     * </pre>
     *
     * 파라미터 순서는 원본과 동일(14개 boolean).
     *
     * @param hunger true → hunger gain 배율, false → exhaustion loss 배율
     * @return 배율값 (base × 1단계 × 2단계)
     */
    public float getFactor(boolean hunger, boolean onGround, boolean isStanding, boolean isStill,
            boolean isSneaking, boolean isRunning, boolean isSprinting,
            boolean isClimbing, boolean isClimbCrawling, boolean isCeilingClimbing,
            boolean isDipping, boolean isSwimming, boolean isDiving,
            boolean isCrawling, boolean isCrawlClimbing) {
        // 원본 L556-L561 전처리
        isClimbing |= isClimbCrawling;
        isCrawling |= isCrawlClimbing;
        boolean actionOverGound = isClimbing || isCeilingClimbing || isDiving || isSwimming;
        boolean airBorne = !onGround && !actionOverGound;
        isStanding = actionOverGound ? isStill : isStanding;
        isSneaking = isSneaking & !isStanding;

        // 원본 L563 base factor
        float factor = hunger ? baseHungerGainFactor : baseExhautionLossFactor;

        // 원본 L564-L575 1단계 — 이동속도 배율
        if (airBorne)
            factor *= hunger ? 0F : fallExhautionLossFactor;           // ⚠️ hunger=0F 하드코딩 (원본 L565)
        else if (isSprinting)
            factor *= hunger ? sprintingHungerGainFactor : sprintingExhautionLossFactor;
        else if (isRunning)
            factor *= hunger ? runningHungerGainFactor : runningExhautionLossFactor;
        else if (isSneaking)
            factor *= hunger ? sneakingHungerGainFactor : sneakingExhautionLossFactor;
        else if (isStanding)
            factor *= hunger ? standingHungerGainFactor : standingExhautionLossFactor;
        else
            factor *= hunger ? walkingHungerGainFactor : walkingExhautionLossFactor;

        // 원본 L577-L592 2단계 — 행동 배율
        if (isClimbing)
            factor *= hunger ? climbingHungerGainFactor : climbingExhaustionLossFactor;
        else if (isCrawling)
            factor *= hunger ? crawlingHungerGainFactor : crawlingExhaustionLossFactor;
        else if (isCeilingClimbing)
            factor *= hunger ? ceilClimbingHungerGainFactor : ceilClimbingExhaustionLossFactor;
        else if (isSwimming)
            factor *= hunger ? swimmingHungerGainFactor : swimmingExhaustionLossFactor;
        else if (isDiving)
            factor *= hunger ? divingHungerGainFactor : divingExhaustionLossFactor;
        else if (isDipping)
            factor *= hunger ? dippingHungerGainFactor : dippingExhaustionLossFactor;
        else if (onGround)
            factor *= hunger ? normalHungerGainFactor : normalExhaustionLossFactor;
        else
            factor *= hunger ? normalHungerGainFactor : normalExhaustionLossFactor;  // L591 의도된 중복

        return factor;
    }

    /**
     * 원본 SmartMovingProperties.update() (L163-L172) 1:1 근사 이식.
     *
     * 원본:
     *   protected void update() {
     *       List<Property<?>> properties = getProperties();
     *       Iterator<Property<?>> iterator = properties.iterator();
     *       String currentKey = getCurrentKey();
     *       while (iterator.hasNext())
     *           iterator.next().update(currentKey);   // ① Property key-scoped 값 재계산
     *       enabled = toggler != -1;                   // ② enabled 파생
     *   }
     *
     * 1.21.1: Property 계층 부재 → ① 는 N/A (단일 필드 값 구조). ② 만 이식.
     * 호출: toggle() / setKeys() / setCurrentKey() / load() 등 toggler 변경 이후.
     */
    private void updateToggler() {
        enabled = toggler != -1;
    }

    /**
     * 원본 SmartMovingProperties.toggle() (L81-L88) 1:1 이식.
     *
     * 원본:
     *   public void toggle() {
     *       int length = keys == null ? 0 : keys.length;
     *       toggler++;
     *       if (toggler == length) toggler = -1;
     *       update();
     *   }
     *
     * 순환 패턴:
     *   configKeys.length == 3 (survival/adventure, "e"/"m"/"h"):
     *     0 → 1 → 2 → -1 → 0 → ...       (Easy → Medium → Hard → disabled → Easy ...)
     *   configKeys.length == 1 (creative "c" 또는 DEFAULT_KEYS {null}):
     *     0 → -1 → 0 → ...                 (on/off 토글)
     *
     * save(): 원본 Properties.toggle() 에는 없음. 원본 SmartMovingOptions.toggle() (override,
     *   SmartMovingOptions.md L500-L531) 에서 saveToOptionsFile(optionsPath) 호출. 1.21.1 은
     *   Properties/Options/Config 계층이 SmartMovingConfig 하나로 통합 — 여기서 save() 호출.
     *   SmartMovingOptions.toggle() 의 다른 추가 동작(채팅, defaultKey 갱신)은 별도 원자 작업.
     */
    public void toggle() {
        int length = configKeys == null ? 0 : configKeys.length;
        toggler++;
        if (toggler == length) toggler = -1;
        updateToggler();
        save();
    }

    /**
     * 원본 SmartMovingProperties.setKeys(String[]) (L90-L97) 1:1 이식.
     *
     * 원본:
     *   public void setKeys(String[] keys) {
     *       if (keys == null || keys.length == 0)
     *           keys = _defaultKeys;          // 단순 on/off 모드 ({null})
     *       this.keys = keys;
     *       toggler = 0;                      // 항상 첫 key 로 초기화
     *       update();
     *   }
     *
     * 호출: initializeForGameIfNeccessary(gameType) 에서 게임타입별 keys 배열 전달 (F 섹션).
     * `save()` 호출은 원본 setKeys 에 없음 — 생략.
     */
    public void setKeys(String[] keys) {
        if (keys == null || keys.length == 0)
            keys = DEFAULT_KEYS;
        this.configKeys = keys;
        this.toggler = 0;
        updateToggler();
    }

    // H-19 (세션 23): getCurrentKey / getKey / getNextKey / hasKey / setCurrentKey 메서드
    // 5개 삭제. H-18 후 외부 호출처 0건 확인. 2상태 토글(toggle/setKeys/updateToggler 만
    // 유지)에 불필요. Medium/Hard 프리셋 복원 시 `focus_09_difficulty_presets.md` 에서 재도입.
    // 원본 메서드 본체는 `docs/research/original/smartmoving/config/SmartMovingProperties.md`
    // L99-L156 에 보존.

    // H-20 (세션 23): initializeForGameIfNeccessary / resetForNewGame 메서드 삭제.
    // 2상태 토글(configKeys={null}) 유지가 목표이므로 gameType 기반 setKeys 호출 경로 불필요.
    // 원본 함수 본체는 focus_05 §5 리서치 파일에 보존. Medium/Hard 프리셋 복원 시
    // `focus_09_difficulty_presets.md` 후속 포커스에서 재도입.

    public void changeSpeed(int difference) {
        speedUserExponent += difference;
    }

    /**
     * 플레이어별 속도 지수 조정.
     * 원본: SmartMovingServerOptions.changeSingleSpeed(player, difference):
     *   Integer exp = _speedUsersExponents.get(username);
     *   if (exp == null) exp = _speedUserExponent.value;
     *   exp += diff;
     *   _speedUsersExponents.put(username, exp);
     *   saveToOptionsFile();
     * 개인 값이 없으면 전역 지수를 기준으로 시작.
     */
    public synchronized void changeSingleSpeed(String username, int difference) {
        Integer exponent = playerSpeedExponents.get(username);
        if (exponent == null) exponent = speedUserExponent;
        exponent += difference;
        playerSpeedExponents.put(username, exponent);
    }

    /** 현재 속도를 정수 퍼센트 문자열로 반환 (원본: SmartMovingOptions.getSpeedPercent()) */
    public String getSpeedPercent() {
        return String.valueOf((int)(getUserSpeedFactor() * 100));
    }

    private static float getFloat(Properties p, String key, float def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try { return Float.parseFloat(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private static boolean getBool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        return Boolean.parseBoolean(v.trim());
    }

    private static int getInt(Properties p, String key, int def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    /**
     * 원본: Value.tryParseIntegerMap(String value) (Value.md L555-568).
     * `,`으로 분할 → 짝수 인덱스=키, 홀수=값. parseInt 실패 항목은 스킵(원본은 NPE 가능).
     */
    private static Map<String, Integer> parseIntegerMap(String raw) {
        Map<String, Integer> result = new HashMap<>();
        if (raw == null || raw.isEmpty()) return result;
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            String key = parts[i++];
            if (i < parts.length) {
                try { result.put(key, Integer.parseInt(parts[i].trim())); }
                catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    /** parseIntegerMap 역방향 — `"user1,5,user2,-2,..."` 형식 생성. */
    private static String serializeIntegerMap(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (!first) sb.append(',');
            sb.append(e.getKey()).append(',').append(e.getValue());
            first = false;
        }
        return sb.toString();
    }

    // H-20 (세션 23): getCsvArray / csvJoin 헬퍼 삭제. 게임타입별 String[] 필드 제거와
    // 함께 호출처 전부 사라짐. (playerSpeedExponents 는 별도 헬퍼 parseIntegerMap/
    // serializeIntegerMap 사용)
}
