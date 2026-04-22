package choco.ratel.smartmoving.stat;

/**
 * 렌더 틱마다 플레이어별 이동 데이터를 추적.
 * 원본 SmartStatistics 필드 목록을 그대로 이식 (animation_system.md 기반).
 * 계산: afterMoveEntityWithHeading 대응 → ClientPlayerEntity.move() TAIL 이후 호출.
 */
public class SmartStatistics {

    // ── 누적 거리 ─────────────────────────────────────────────
    public float totalHorizontalDistance;   // limbSwing 등가 (animateArmSwinging 입력)
    public float totalVerticalDistance;
    public float totalDistance;

    // ── 현재 속도 (0~1 정규화) ──────────────────────────────────
    public float currentHorizontalSpeed;         // limbSwingAmount 등가
    public float currentHorizontalSpeedFlattened;
    public float currentVerticalSpeed;
    public float currentSpeed;

    // ── 프레임 단위 이동량 ────────────────────────────────────
    public double horizontalDistance;
    public double verticalDistance;
    public double distance;

    // ── 각도 (라디안) ──────────────────────────────────────────
    public float currentCameraAngle;
    public float currentVerticalAngle;
    public float currentHorizontalAngle;

    // ── 기타 ───────────────────────────────────────────────────
    public float smallOverGroundHeight;

    /**
     * move() TAIL 이후 매 틱 호출 — 위치 델타로 이동 통계 갱신.
     * 원본: SmartStatisticsPlayerBase.afterMoveEntityWithHeading() → statistics.calculateAllStats(false)
     *
     * prevX/prevY/prevZ는 tickMovement() HEAD에서 현재 위치로 저장되므로,
     * move() 이후 delta = currentPos - prevPos = 이번 틱 실제 이동량.
     */
    public void calculate(double prevX, double prevY, double prevZ,
                          double x, double y, double z) {
        double diffX = x - prevX;
        double diffY = y - prevY;
        double diffZ = z - prevZ;

        horizontalDistance = Math.sqrt(diffX * diffX + diffZ * diffZ);
        verticalDistance = Math.abs(diffY);
        distance = Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);

        // 원본: SmartStatisticsData.calcualte() — distance *= 4F; legYaw += (dist - legYaw) * 0.4F
        // legYaw = EMA(rawDistance * 4, factor=0.4). 일반 보행(~0.22 b/t) → ~0.88, 비행(~0.3 b/t) → 1.0(clamp).
        // sm_setupTransforms/sm_animateFlying에서 walkFactor = min(1, currentSpeed)으로 사용됨.
        currentHorizontalSpeed += ((float) horizontalDistance * 4f - currentHorizontalSpeed) * 0.4f;
        currentVerticalSpeed   += ((float) verticalDistance   * 4f - currentVerticalSpeed)   * 0.4f;
        currentSpeed           += ((float) distance           * 4f - currentSpeed)           * 0.4f;

        // 평탄화 수평 속도 (EMA on EMA: factor=0.5)
        currentHorizontalSpeedFlattened = currentHorizontalSpeedFlattened * 0.5f + currentHorizontalSpeed * 0.5f;

        // 수직 이동 각도 (라디안): 수평 이동 방향에서 위/아래 각도
        // 원본: atan(yDiff/h), h==0 → NaN → Quarter(π/2). 순수 수직 이동 시 항상 Quarter 반환.
        currentVerticalAngle = (horizontalDistance > 1e-4)
                ? (float) Math.atan2(diffY, horizontalDistance)
                : (float) (Math.PI / 2f);

        // 수평 이동 방향 각도 (world Y 기준): 원본 currentHorizontalAngle
        currentHorizontalAngle = (horizontalDistance > 1e-4)
                ? (float) Math.atan2(diffX, diffZ)
                : currentHorizontalAngle;

        // 누적 거리
        totalHorizontalDistance += (float) horizontalDistance;
        totalVerticalDistance += (float) verticalDistance;
        totalDistance += (float) distance;
    }

    public void reset() {
        totalHorizontalDistance = 0;
        totalVerticalDistance = 0;
        totalDistance = 0;
        currentHorizontalSpeed = 0;
        currentHorizontalSpeedFlattened = 0;
        currentVerticalSpeed = 0;
        currentSpeed = 0;
        horizontalDistance = 0;
        verticalDistance = 0;
        distance = 0;
        currentCameraAngle = 0;
        currentVerticalAngle = 0;
        currentHorizontalAngle = 0;
        smallOverGroundHeight = 0;
    }
}
