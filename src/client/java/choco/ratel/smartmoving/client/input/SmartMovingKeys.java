package choco.ratel.smartmoving.client.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * SM 전용 키바인딩 등록.
 * 원본 LWJGL2 키코드 → GLFW 키코드 변환:
 *   LCONTROL(29) → GLFW_KEY_LEFT_CONTROL(341)
 *   F9(67)       → GLFW_KEY_F9(298)
 *   O(24)        → GLFW_KEY_O(79)
 *   I(23)        → GLFW_KEY_I(73)
 */
public class SmartMovingKeys {

    private static final String CATEGORY_GAMEPLAY     = "key.categories.gameplay";
    private static final String CATEGORY_SMARTMOVING  = "key.categories.smartmoving";

    public static KeyBinding grab;
    public static KeyBinding configToggle;
    public static KeyBinding speedIncrease;
    public static KeyBinding speedDecrease;

    public static void register() {
        grab = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.smartmoving.grab",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY_GAMEPLAY
        ));
        configToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.smartmoving.config_toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            CATEGORY_SMARTMOVING
        ));
        speedIncrease = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.smartmoving.speed_increase",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY_SMARTMOVING
        ));
        speedDecrease = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.smartmoving.speed_decrease",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            CATEGORY_SMARTMOVING
        ));
    }
}
