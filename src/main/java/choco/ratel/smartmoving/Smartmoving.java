package choco.ratel.smartmoving;

import choco.ratel.smartmoving.config.ConfigManager;
import choco.ratel.smartmoving.network.SmartMovingServerNetworking;
import choco.ratel.smartmoving.state.SmartMovingAttachments;
import net.fabricmc.api.ModInitializer;

public class Smartmoving implements ModInitializer {

    public static final String MOD_ID = "smartmoving";

    @Override
    public void onInitialize() {
        ConfigManager.load();
        SmartMovingAttachments.register();
        SmartMovingServerNetworking.register();
    }
}
