package choco.ratel.smartmoving.sound;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class SmartMovingSounds {

    public static final SoundEvent CLIMB      = register("climb");
    public static final SoundEvent SLIDE       = register("slide");
    public static final SoundEvent CEILING_CLIMB = register("ceiling_climb");

    public static void register() {
        // 클래스 로딩을 통해 static 블록 초기화 트리거
    }

    private static SoundEvent register(String name) {
        Identifier id = Identifier.of("smartmoving", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    private SmartMovingSounds() {}
}
