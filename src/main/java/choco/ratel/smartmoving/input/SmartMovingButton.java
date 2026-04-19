package choco.ratel.smartmoving.input;

public class SmartMovingButton {
    public boolean pressed;
    public boolean wasPressed;
    public boolean startPressed;
    public boolean stopPressed;

    public void update(boolean currentlyPressed) {
        wasPressed = pressed;
        pressed = currentlyPressed;
        startPressed = pressed && !wasPressed;
        stopPressed = !pressed && wasPressed;
    }

    public void reset() {
        pressed = wasPressed = startPressed = stopPressed = false;
    }
}
