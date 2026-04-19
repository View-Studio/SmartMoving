package choco.ratel.smartmoving.client.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public final class SmartMovingKeys {

    public static final KeyBinding GRAB = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.smartmoving.grab", GLFW.GLFW_KEY_LEFT_CONTROL, "key.categories.smartmoving")
    );

    public static final KeyBinding CONFIG_TOGGLE = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.smartmoving.config_toggle", GLFW.GLFW_KEY_F9, "key.categories.smartmoving")
    );

    public static final KeyBinding SPEED_INCREASE = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.smartmoving.speed_increase", GLFW.GLFW_KEY_O, "key.categories.smartmoving")
    );

    public static final KeyBinding SPEED_DECREASE = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.smartmoving.speed_decrease", GLFW.GLFW_KEY_I, "key.categories.smartmoving")
    );

    public static void register() {
        // 키바인딩 등록 보장 (static field 초기화 트리거)
    }

    private SmartMovingKeys() {}
}
