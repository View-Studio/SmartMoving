package choco.ratel.smartmoving.physics;

public enum FeetClimbing {
    None(-3), BaseHold(-2), BaseWithHands(-1), TopWithHands(0),
    SlowUpWithHoldWithoutHands(1), SlowUpWithSinkWithoutHands(2), FastUp(3);

    public final int value;

    FeetClimbing(int value) { this.value = value; }

    public static FeetClimbing fromValue(int value) {
        for (FeetClimbing f : values()) {
            if (f.value == value) return f;
        }
        return None;
    }
}
