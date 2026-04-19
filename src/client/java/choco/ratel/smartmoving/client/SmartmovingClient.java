package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.client.input.InputHandler;
import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import net.fabricmc.api.ClientModInitializer;

public class SmartmovingClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SmartMovingKeys.register();
        InputHandler.register();
    }
}
