package choco.ratel.smartmoving;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.ModInitializer;

public class SmartMoving implements ModInitializer {

    public static final String MOD_ID = "smartmoving";

    @Override
    public void onInitialize() {
        SmartMovingConfig.load();
    }
}
