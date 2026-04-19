package choco.ratel.smartmoving.physics;

public enum HandsClimbing {
    None(-3), Sink(-2), TopHold(-1), BottomHold(0), Up(1), FastUp(2);

    public final int value;

    HandsClimbing(int value) { this.value = value; }

    public static HandsClimbing fromValue(int value) {
        for (HandsClimbing h : values()) {
            if (h.value == value) return h;
        }
        return None;
    }
}
