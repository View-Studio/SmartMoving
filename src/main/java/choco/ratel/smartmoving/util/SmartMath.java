package choco.ratel.smartmoving.util;

import net.minecraft.util.math.Vec3d;

/** SM 공통 수학 유틸. 순수 함수로만 구성. */
public final class SmartMath {
    private SmartMath() {}

    /**
     * SM 비표준 정규화 이동 공식.
     * 원본: total = sqrt(sqrt(x²+z²) + y²) — 표준 3D 유클리드 거리가 아님.
     * 수직 성분에 더 큰 가중치를 주는 의도적 설계. vanilla applyMovementInput() 사용 금지.
     *
     * @return 정규화된 속도 벡터 (속도 방향 * speed). 입력 벡터가 너무 작으면 Vec3d.ZERO.
     */
    public static Vec3d moveFlying(float speed, float strafe, float upward, float forward) {
        float hLen = (float) Math.sqrt(strafe * strafe + forward * forward);
        float total = (float) Math.sqrt(hLen + upward * upward);
        if (total < speed) return Vec3d.ZERO;
        float scale = speed / total;
        return new Vec3d(strafe * scale, upward * scale, forward * scale);
    }

    /**
     * 선형 보간 팩터. 원본: (x - x0) / (x1 - x0), 0~1 클램프.
     * 애니메이션 시스템 전반에서 사용.
     */
    public static float factor(float x, float x0, float x1) {
        float result = (x - x0) / (x1 - x0);
        return Math.max(0f, Math.min(1f, result));
    }

    /**
     * 각도 점프 수평 속도 계산.
     * reset=false: 현재 속도에 입력을 더함.
     * reset=true + 반대 방향: 방향 전환 (입력 * horizontalJumpFactor).
     * reset=true + 같은 방향: max(현재, 입력*horizontal) 유지.
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
}
