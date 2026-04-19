package choco.ratel.smartmoving.client.input;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public final class InputHandler {

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(InputHandler::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null) return;

        SmartMovingState state = player.getAttachedOrCreate(SmartMovingAttachments.STATE);

        state.forwardButton.update(client.options.forwardKey.isPressed());
        state.backButton.update(client.options.backKey.isPressed());
        state.leftButton.update(client.options.leftKey.isPressed());
        state.rightButton.update(client.options.rightKey.isPressed());
        state.jumpButton.update(client.options.jumpKey.isPressed());
        state.sprintButton.update(client.options.sprintKey.isPressed());
        state.sneakButton.update(client.options.sneakKey.isPressed());
        state.grabButton.update(SmartMovingKeys.GRAB.isPressed());
    }

    private InputHandler() {}
}
