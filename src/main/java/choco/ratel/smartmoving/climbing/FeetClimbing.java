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

    // 원본: this == SlowUpWithHoldWithoutHands || this == SlowUpWithSinkWithoutHands || this == FastUp
    public boolean isUp() {
        return this == SLOW_UP_WITH_HOLD_WITHOUT_HANDS
            || this == SLOW_UP_WITH_SINK_WITHOUT_HANDS
            || this == FAST_UP;
    }

    /**
     * 두 FeetClimbing 중 더 강한 쪽을 선택한다.
     * 원본: !SkipGaps이면 CanStand/MustCrawl OR 합산. other가 더 강하면 Block/Meta/Direction 교체.
     */
    public FeetClimbing max(FeetClimbing other, ClimbGap[] inout_thisGap, ClimbGap otherGap) {
        if (!otherGap.skipGaps) {
            inout_thisGap[0].canStand |= otherGap.canStand;
            inout_thisGap[0].mustCrawl |= otherGap.mustCrawl;
        }
        if (other.ordinal() > this.ordinal()) {
            inout_thisGap[0].state = otherGap.state;
            inout_thisGap[0].direction = otherGap.direction;
            return other;
        }
        return this;
    }
}
