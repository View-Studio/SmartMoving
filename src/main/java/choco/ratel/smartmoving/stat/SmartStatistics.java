package choco.ratel.smartmoving.stat;

/**
 * 렌더 틱마다 플레이어별 이동 데이터를 추적.
 * 원본 SmartStatistics 필드 목록을 그대로 이식 (animation_system.md 기반).
 * 계산 로직은 Phase 4(렌더 Mixin)에서 채운다.
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
