package choco.ratel.smartmoving.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 🔴 (Phase 2 multi BUG-11) LivingEntity 의 serverX/Y/Z + bodyTrackingIncrements accessor.
 *   remote 측 비행 종료 시 server 보고 위치 직접 적용 + lerp 차단 위해 사용.
 */
@Mixin(LivingEntity.class)
@Environment(EnvType.CLIENT)
public interface MixinLivingEntityAccessor {
    @Accessor("serverX") double sm_getServerX();
    @Accessor("serverY") double sm_getServerY();
    @Accessor("serverZ") double sm_getServerZ();
    @Accessor("serverY") void sm_setServerY(double value);
    @Accessor("bodyTrackingIncrements") int sm_getBodyTrackingIncrements();
    @Accessor("bodyTrackingIncrements") void sm_setBodyTrackingIncrements(int value);
}
