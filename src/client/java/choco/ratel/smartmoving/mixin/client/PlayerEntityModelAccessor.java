package choco.ratel.smartmoving.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * PlayerEntityModel.cloak (private ModelPart) Accessor.
 * 사용처: B-16 / §16-24 — 망토 기본 기울임 (cloak.pitch = SIXTYFOURTH).
 */
@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityModel.class)
public interface PlayerEntityModelAccessor {
    @Accessor("cloak")
    ModelPart sm_getCloak();
}
