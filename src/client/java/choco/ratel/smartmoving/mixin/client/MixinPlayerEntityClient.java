package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 6-1 (클라이언트): getBaseDimensions() 커스텀 EntityDimensions 반환.
 *
 * getDimensions()는 final → getBaseDimensions()가 유일한 오버라이드 진입점.
 * Yarn: getBaseDimensions (intermediary: method_55694 확인 완료)
 *
 * 서버 측 → MixinPlayerEntity.sm_getBaseDimensions_server()
 */
@Mixin(PlayerEntity.class)
@Environment(EnvType.CLIENT)
public abstract class MixinPlayerEntityClient {

    /**
     * 6-1 (클라이언트): SM 크롤링 시 SWIMMING 포즈 hitbox를 1블록 높이로 교체.
     *
     * vanilla SWIMMING 포즈: 0.6W × 0.6H — SM 원본 크롤링 hitbox(1블록)와 다름.
     * SM 크롤링 중 SWIMMING 포즈가 설정된 상태에서 getBaseDimensions 호출 시:
     *   → 0.6W × 1.0H, eyeHeight 0.4F 반환
     *
     * 헤드점프용 EntityDimensions → TODO Phase 10 (exact dimensions 미확인)
     */
    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void sm_getBaseDimensions_client(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isHeadJumping && pose == EntityPose.SWIMMING) {
            // 원본: height += 1F (boundingBox 직접 조작 → 1.7.10)
            // [미확인 — 정확한 헤드점프 치수 추가 리서치 필요. 현재 1.5H 사용]
            cir.setReturnValue(EntityDimensions.changing(0.6F, 1.5F).withEyeHeight(0.4F));
        } else if (sm.isCrawling && pose == EntityPose.SWIMMING) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 1.0F).withEyeHeight(0.4F));
        }
    }
}
