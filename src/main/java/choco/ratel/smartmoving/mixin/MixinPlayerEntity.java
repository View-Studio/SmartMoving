package choco.ratel.smartmoving.mixin;

import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 6-1 (서버): getBaseDimensions() 커스텀 EntityDimensions 반환.
 *
 * LivingEntity.getDimensions()는 final → getBaseDimensions()가 유일한 오버라이드 진입점.
 * Yarn: getBaseDimensions (intermediary: method_55694 확인 완료)
 *
 * 클라이언트 측 → MixinPlayerEntityClient.sm_getBaseDimensions_client()
 */
@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity {

    /**
     * 6-1 (서버): SM 크롤링 시 SWIMMING 포즈 hitbox를 1블록 높이로 교체.
     *
     * vanilla SWIMMING 포즈: 0.6W × 0.6H — SM 원본 크롤링 hitbox(1블록)와 다름.
     * SM 크롤링 중 SWIMMING 포즈가 설정된 상태에서 getBaseDimensions 호출 시:
     *   → 0.6W × 1.0H, eyeHeight 0.4F 반환
     *   → calculateDimensions()가 이를 받아 bounding box 즉시 갱신
     *
     * 헤드점프용 EntityDimensions → TODO Phase 10 (exact dimensions 미확인)
     */
    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void sm_getBaseDimensions_server(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);
        if (sm.isCrawling && pose == EntityPose.SWIMMING) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 1.0F).withEyeHeight(0.4F));
        }
        // 헤드점프: sm.isSmall 또는 별도 필드 — TODO Phase 10
    }
}
