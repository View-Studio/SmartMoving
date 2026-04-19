package choco.ratel.smartmoving.client.render;

public final class AnimationUtil {

    // 라디안 각도 상수
    public static final float WHOLE       = (float)(2 * Math.PI);   // 360°
    public static final float HALF        = (float)(Math.PI);        // 180°
    public static final float QUARTER     = (float)(Math.PI / 2);    // 90°
    public static final float EIGHTH      = (float)(Math.PI / 4);    // 45°
    public static final float SIXTEENTH   = (float)(Math.PI / 8);    // 22.5°
    public static final float THIRTYTWOTH = (float)(Math.PI / 16);   // 11.25°
    public static final float SIXTYFOURTH = (float)(Math.PI / 32);   //  5.6°

    /**
     * 선형 보간 함수 — 원본 SmartMovingModel.Factor()와 동일.
     * x0 > x1 이면 x가 클수록 0에 가까워지고, x0 <= x1 이면 x가 클수록 1에 가까워진다.
     */
    public static float factor(float x, float x0, float x1) {
        if (x0 > x1) {
            if (x <= x1) return 1F;
            if (x >= x0) return 0F;
            return (x0 - x) / (x0 - x1);
        } else {
            if (x >= x1) return 1F;
            if (x <= x0) return 0F;
            return (x - x0) / (x1 - x0);
        }
    }

    private AnimationUtil() {}
}
