package choco.ratel.smartmoving.config;

/**
 * SmartMoving 설정값 컨테이너.
 * 기본값은 원본 SmartMoving 설정을 따름.
 */
public class SmartMovingConfig {

    // ── 기능 ON/OFF ──────────────────────────────────────────────────────────
    public boolean crawlingEnabled          = true;
    public boolean climbingEnabled          = true;
    public boolean ceilingClimbingEnabled   = true;
    public boolean slidingEnabled           = true;
    public boolean swimmingEnabled          = true;
    public boolean divingEnabled            = true;

    public boolean jumpChargeEnabled        = true;
    public boolean headJumpEnabled          = true;
    public boolean angleJumpSideEnabled     = true;
    public boolean angleJumpBackEnabled     = true;
    public boolean wallJumpEnabled          = true;

    // ── 물리 값 ──────────────────────────────────────────────────────────────
    public float crawlFactor                = 0.35F;
    public float swimSpeedFactor            = 0.8F;
    public float diveSpeedFactor            = 1.0F;

    public float jumpChargeMaximum          = 20F;
    public float jumpChargeFactor           = 1.3F;
    public float headJumpChargeMaximum      = 10F;
    public int   angleJumpDoubleClickTicks  = 3;

    public float slideControlDegrees        = 1.0F;
    public float slideSlipperinessFactor    = 1.0F;

    public float fallingDistanceMinimum     = 3.0F;
    public float freeClimbFallMaximumDistance = 3.0F;

    // ── 탈진 ─────────────────────────────────────────────────────────────────
    public boolean climbExhaustionEnabled   = false;
    public float climbExhaustionStart       = 0.4F;
    public float climbExhaustionStop        = 1.0F;
    public float climbUpExhaustionGain      = 0.004F;
    public float climbDownExhaustionGain    = 0.001F;

    public boolean ceilingClimbExhaustionEnabled = false;
    public float ceilingClimbExhaustionStart     = 0.4F;
    public float ceilingClimbExhaustionStop      = 1.0F;
    public float ceilingClimbExhaustionGain      = 0.005F;
}
