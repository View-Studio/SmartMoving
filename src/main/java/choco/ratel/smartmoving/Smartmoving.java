package choco.ratel.smartmoving;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import net.fabricmc.api.ModInitializer;

public class Smartmoving implements ModInitializer {

    public static final String MOD_ID = "smartmoving";

    @Override
    public void onInitialize() {
        SmartMovingAttachments.register();
    }
}
