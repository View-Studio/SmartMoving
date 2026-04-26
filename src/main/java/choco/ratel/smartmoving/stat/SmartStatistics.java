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

    // ── 🔴 prev 필드 (Flying Phase / 세션 48): partial tick lerp 보간용 ─────
    //   원본 SmartStatisticsData (SmartRender) 는 prevLegYaw / legYaw 두 필드 + 매 프레임
    //   getter 가 `prevLegYaw + (legYaw - prevLegYaw) * partialTicks` lerp 적용.
    //   매 틱 EMA 갱신 + 매 프레임 lerp 처리 = 60Hz 부드러움.
    //   이전 1.21.1 매핑은 단일 필드 (current 만) → 매 틱 20Hz 띡띡 = 사용자 보고
    //   "원본보다 부드럽지 않음" 직접 원인. prev 필드 추가 + lerp getter 매핑.
    public float prevCurrentHorizontalSpeed;
    public float prevCurrentVerticalSpeed;
    public float prevCurrentSpeed;
    public float prevTotalHorizontalDistance;
    public float prevTotalVerticalDistance;
    public float prevTotalDistance;

    // ── 프레임 단위 이동량 ────────────────────────────────────
    public double horizontalDistance;
    public double verticalDistance;
    public double distance;

    // ── 각도 (라디안) ──────────────────────────────────────────
    public float currentCameraAngle;
    public float currentVerticalAngle;
    public float currentHorizontalAngle;
    /**
     * 원본: SmartRenderRender.statistics.prevHorizontalAngle — 이전 계산값.
     * atan(xDiff/zDiff) 가 NaN(xDiff=0 && zDiff=0)일 때 fallback 경로에서 사용.
     * 최초값 NaN → currentCameraAngle 사용, 그 이후 prev 사용.
     */
    public float prevHorizontalAngle = Float.NaN;

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
                          double x, double y, double z, float yawDegrees) {
        double diffX = x - prevX;
        double diffY = y - prevY;
        double diffZ = z - prevZ;

        horizontalDistance = Math.sqrt(diffX * diffX + diffZ * diffZ);
        verticalDistance = Math.abs(diffY);
        distance = Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);

        // 🔴 (세션 48): prev 필드 갱신 — 매 틱 EMA 적용 전 이전 값 저장 (lerp 보간용).
        //   원본 SmartStatisticsDatas.initialize: prevLegYaw = previous.legYaw, legYaw = previous.legYaw.
        //   1.21.1 동등: prev = 이번 틱 시작 시점의 current (= 이전 틱 EMA 결과).
        prevCurrentHorizontalSpeed = currentHorizontalSpeed;
        prevCurrentVerticalSpeed   = currentVerticalSpeed;
        prevCurrentSpeed           = currentSpeed;
        prevTotalHorizontalDistance = totalHorizontalDistance;
        prevTotalVerticalDistance   = totalVerticalDistance;
        prevTotalDistance           = totalDistance;

        // 원본: SmartStatisticsData.calcualte() — distance *= 4F; legYaw += (dist - legYaw) * 0.4F
        // legYaw = EMA(rawDistance * 4, factor=0.4). 일반 보행(~0.22 b/t) → ~0.88, 비행(~0.3 b/t) → 1.0(clamp).
        // sm_setupTransforms/sm_animateFlying에서 walkFactor = min(1, currentSpeed)으로 사용됨.
        currentHorizontalSpeed += ((float) horizontalDistance * 4f - currentHorizontalSpeed) * 0.4f;
        currentVerticalSpeed   += ((float) verticalDistance   * 4f - currentVerticalSpeed)   * 0.4f;
        currentSpeed           += ((float) distance           * 4f - currentSpeed)           * 0.4f;

        // 평탄화 수평 속도 (EMA on EMA: factor=0.5)
        currentHorizontalSpeedFlattened = currentHorizontalSpeedFlattened * 0.5f + currentHorizontalSpeed * 0.5f;

        // 원본 SmartRenderRender L95: currentCameraAngle = rotationYaw / RadiantToAngle (= Math.toRadians)
        currentCameraAngle = (float) Math.toRadians(yawDegrees);

        // 수직 이동 각도 (라디안): 수평 이동 방향에서 위/아래 각도
        // 원본 SmartRenderRender L96-98: atan(yDiff/h), h==0 → NaN → Quarter(π/2). 순수 수직 이동 시 Quarter.
        currentVerticalAngle = (horizontalDistance > 1e-4)
                ? (float) Math.atan2(diffY, horizontalDistance)
                : (float) (Math.PI / 2f);

        // 원본 SmartRenderRender L100-111: -atan(xDiff/zDiff). NaN(xDiff=0&&zDiff=0) 시
        //   prevHorizontalAngle NaN → currentCameraAngle, 아니면 prev 사용.
        //   정상 값이면 zDiff<0 일 때 +π 보정. Minecraft rotationYaw convention(+Z=0, +X=-π/2, -X=+π/2).
        float newH = (float) -Math.atan(diffX / diffZ);
        if (Float.isNaN(newH)) {
            newH = Float.isNaN(prevHorizontalAngle) ? currentCameraAngle : prevHorizontalAngle;
        } else if (diffZ < 0) {
            newH += (float) Math.PI;
        }
        prevHorizontalAngle = newH;
        currentHorizontalAngle = newH;

        // 누적 거리
        totalHorizontalDistance += (float) horizontalDistance;
        totalVerticalDistance += (float) verticalDistance;
        totalDistance += (float) distance;
    }

    // ── 🔴 partial tick lerp getter (Flying Phase / 세션 48): 60Hz 부드러움 보간 ────
    //   원본 SmartStatisticsData.getCurrentSpeed:
    //     return Math.min(1.0F, prevLegYaw + (legYaw - prevLegYaw) * renderPartialTicks);
    //   원본 SmartStatisticsData.getTotalDistance:
    //     return total - legYaw * (1.0F - renderPartialTicks);
    //   매 프레임 호출 시 prev/current 사이 partial tick 비율로 보간 = 부드러움.

    public float getCurrentSpeed(float partialTicks) {
        return Math.min(1.0F, prevCurrentSpeed + (currentSpeed - prevCurrentSpeed) * partialTicks);
    }

    public float getCurrentHorizontalSpeed(float partialTicks) {
        return Math.min(1.0F, prevCurrentHorizontalSpeed
                + (currentHorizontalSpeed - prevCurrentHorizontalSpeed) * partialTicks);
    }

    public float getCurrentVerticalSpeed(float partialTicks) {
        return Math.min(1.0F, prevCurrentVerticalSpeed
                + (currentVerticalSpeed - prevCurrentVerticalSpeed) * partialTicks);
    }

    public float getTotalDistance(float partialTicks) {
        return totalDistance - currentSpeed * (1.0F - partialTicks);
    }

    public float getTotalHorizontalDistance(float partialTicks) {
        return totalHorizontalDistance - currentHorizontalSpeed * (1.0F - partialTicks);
    }

    public float getTotalVerticalDistance(float partialTicks) {
        return totalVerticalDistance - currentVerticalSpeed * (1.0F - partialTicks);
    }

    public void reset() {
        totalHorizontalDistance = 0;
        totalVerticalDistance = 0;
        totalDistance = 0;
        currentHorizontalSpeed = 0;
        currentHorizontalSpeedFlattened = 0;
        currentVerticalSpeed = 0;
        currentSpeed = 0;
        prevCurrentHorizontalSpeed = 0;
        prevCurrentVerticalSpeed = 0;
        prevCurrentSpeed = 0;
        prevTotalHorizontalDistance = 0;
        prevTotalVerticalDistance = 0;
        prevTotalDistance = 0;
        horizontalDistance = 0;
        verticalDistance = 0;
        distance = 0;
        currentCameraAngle = 0;
        currentVerticalAngle = 0;
        currentHorizontalAngle = 0;
        prevHorizontalAngle = Float.NaN;
        smallOverGroundHeight = 0;
    }
}
