package choco.ratel.smartmoving.climbing;

/**
 * 손 클라이밍 상태 enum. 원본 Typesafe Enum (None=-3 ~ FastUp=2) 을 Java enum으로 변환.
 * ordinal 비교로 원본의 _value 비교 로직을 재현한다.
 */
public enum HandsClimbing {
    NONE,        // 원본: None(-3)
    SINK,        // 원본: Sink(-2)
    TOP_HOLD,    // 원본: TopHold(-1)
    BOTTOM_HOLD, // 원본: BottomHold(0)
    UP,          // 원본: Up(1)
    FAST_UP;     // 원본: FastUp(2)

    // actualHandsClimbType 저장 및 네트워크 4비트 인코딩에 사용되는 int 상수
    public static final int NO_GRAB = 0;
    public static final int UP_GRAB = 1;
    public static final int MIDDLE_GRAB = 2;

    /** None을 제외한 모든 상태. 원본: _value > None._value */
    public boolean isRelevant() {
        return this != NONE;
    }

    /** Up 또는 FastUp. 원본: _value > BottomHold._value */
    public boolean isUp() {
        return this.ordinal() > BOTTOM_HOLD.ordinal();
    }

    /** BottomHold → Up 전환. 그 외 값은 그대로 반환. */
    public HandsClimbing toUp() {
        return this == BOTTOM_HOLD ? UP : this;
    }

    /** TopHold → Sink 전환. 그 외 값은 그대로 반환. */
    public HandsClimbing toDown() {
        return this == TOP_HOLD ? SINK : this;
    }

    /**
     * 두 HandsClimbing 중 더 강한 쪽을 선택한다.
     * 원본: !SkipGaps이면 CanStand/MustCrawl OR 합산. other가 더 강하면 Block/Meta/Direction 교체.
     */
    public HandsClimbing max(HandsClimbing other, ClimbGap[] inout_thisGap, ClimbGap otherGap) {
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
