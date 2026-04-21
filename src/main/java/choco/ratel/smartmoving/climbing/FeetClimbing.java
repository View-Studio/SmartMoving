package choco.ratel.smartmoving.climbing;

/**
 * 발 클라이밍 상태 enum. 원본 Typesafe Enum (None=-3 ~ FastUp=3) 을 Java enum으로 변환.
 * ordinal 비교로 원본의 _value 비교 로직을 재현한다.
 */
public enum FeetClimbing {
    NONE,                          // 원본: None(-3)
    BASE_HOLD,                     // 원본: BaseHold(-2)
    BASE_WITH_HANDS,               // 원본: BaseWithHands(-1)
    TOP_WITH_HANDS,                // 원본: TopWithHands(0)
    SLOW_UP_WITH_HOLD_WITHOUT_HANDS,  // 원본: SlowUpWithHoldWithoutHands(1)
    SLOW_UP_WITH_SINK_WITHOUT_HANDS,  // 원본: SlowUpWithSinkWithoutHands(2)
    FAST_UP;                       // 원본: FastUp(3)

    // actualFeetClimbType 저장 및 네트워크 4비트 인코딩에 사용되는 int 상수
    public static final int NO_STEP = 0;
    public static final int DOWN_STEP = 1;

    /** None을 제외한 모든 상태. 원본: _value > None._value */
    public boolean isRelevant() {
        return this != NONE;
    }

    /**
     * 손 없이 발만으로 클라이밍 가능한 상태.
     * 원본: _value > BaseWithHands._value → TopWithHands(0) 이상.
     * handleClimbing Simple 모드에서 motionY 결정 기준.
     */
    public boolean isIndependentlyRelevant() {
        return this.ordinal() > BASE_WITH_HANDS.ordinal();
    }

    /** BaseHold 초과. 원본: _value > BaseHold._value → BaseWithHands(-1) 이상. IsIndependentlyRelevant와 다름. */
    public boolean isUp() {
        return this.ordinal() > BASE_HOLD.ordinal();
    }

    /**
     * 두 FeetClimbing 중 더 강한 쪽을 선택한다.
     * CanStand/MustCrawl은 OR로 합산하고, 더 강한 쪽의 ClimbGap 정보를 inout_thisGap에 복사한다.
     */
    public FeetClimbing max(FeetClimbing other, ClimbGap[] inout_thisGap, ClimbGap otherGap) {
        boolean canStand = inout_thisGap[0].canStand || otherGap.canStand;
        boolean mustCrawl = inout_thisGap[0].mustCrawl || otherGap.mustCrawl;
        if (other.ordinal() > this.ordinal()) {
            inout_thisGap[0].copyFrom(otherGap);
            inout_thisGap[0].canStand = canStand;
            inout_thisGap[0].mustCrawl = mustCrawl;
            return other;
        }
        inout_thisGap[0].canStand = canStand;
        inout_thisGap[0].mustCrawl = mustCrawl;
        return this;
    }
}
