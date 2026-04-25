package choco.ratel.smartmoving.mixin;

import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 6-1 (서버): getBaseDimensions() 커스텀 EntityDimensions 반환.
 * 6-4 (서버): updatePose() Mixin — SM 상태별 포즈 강제 설정.
 *
 * LivingEntity.getDimensions()는 final → getBaseDimensions()가 유일한 오버라이드 진입점.
 * Yarn: getBaseDimensions (intermediary: method_55694 확인 완료)
 * Yarn: updatePose (intermediary: method_7318 확인 완료)
 *
 * 클라이언트 측 → MixinPlayerEntityClient.sm_getBaseDimensions_client(), sm_updatePose_client()
 */
@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity {

    /**
     * 6-1 (서버): SM 포즈별 커스텀 EntityDimensions 반환.
     *
     * **포커스 #2.7 Phase C-1 (세션 3, 2026-04-25)**: 원본 1:1 정정.
     *   원본 `setHeightOffset(-1F)` (SmartMovingSelf L1694-L1704) → `sp.height = 1.8F + (-1F)
     *   = 0.8F` + `getEyeHeight() = height - 0.18F = 0.62F` (ServerPlayerBase L142-L145).
     *   → 모든 small 상태 (isCrawling/isClimbCrawling/isHeadJumping/isSliding/isSwimming/
     *   isDiving/isFlying/isLevitating) 통합 0.6×0.8 + 0.62F 가 1:1 정확.
     *
     * 이전 (세션 137 ~ 세션 2): SWIMMING + isCrawling → 0.6×1.0 + 0.4F. 1:1 위반.
     * 정정: SWIMMING + isCrawling → 0.6×0.8 + 0.62F (원본 1:1).
     *
     * SLIDING 포즈: isSmall=true 시 서버가 SLIDING 포즈를 설정 (sm_updatePose_server).
     *   → 동일 0.6×0.8 + 0.62F.
     */
    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void sm_getBaseDimensions_server(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (pose == EntityPose.SLIDING) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
            return;
        }
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);
        if (sm.isCrawling && pose == EntityPose.SWIMMING) {
            // Phase C-1 (세션 3): 1.0F → 0.8F, 0.4F → 0.62F (원본 height - 0.18F)
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
        }
    }

    /**
     * 6-4 (서버): SM 이동 상태에서 vanilla updatePose() 취소 후 SM 포즈 강제 설정.
     *
     * 서버는 isHeadJumping을 직접 알 수 없으므로 isSmall 비트(State 패킷 bit 1)로 판단:
     *   isSmall=true → SLIDING (크롤링/슬라이딩/헤드점프 공통 0.8H 상태)
     *   isCrawling=true → SWIMMING (1.0H)
     *   그 외 → vanilla updatePose() 정상 실행
     */
    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void sm_updatePose_server(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);

        if (sm.isCrawling) {
            player.setPose(EntityPose.SWIMMING);
            ci.cancel();
        } else if (sm.isSmall) {
            player.setPose(EntityPose.SLIDING);
            ci.cancel();
        }
    }

    /**
     * 3-3: addExhaustion() 소진 차단.
     * disableAddExhaustion이 true인 동안 vanilla 소진 추가를 전부 차단한다.
     *
     * addExhaustion()은 PlayerEntity에서 선언되므로 여기서 inject.
     * ServerPlayerEntity에는 선언되어 있지 않아 MixinServerPlayerEntity에서 inject 불가.
     */
    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void sm_addExhaustion(float exhaustion, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);
        if (sm.disableAddExhaustion) {
            ci.cancel();
        }
    }
}
