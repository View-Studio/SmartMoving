package choco.ratel.smartmoving.mixin;

import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 5-1: travel() HEAD 캔설러블 Mixin — SM 이동 파이프라인 진입점.
 * 5-5 (서버): isClimbing() 오버라이드 — SM 커스텀 클라이밍 중 false 반환.
 *
 * 클라이언트 전용 항목(jump, applyClimbingSpeed, isClimbing 클라이언트) → MixinLivingEntityClient
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    /**
     * 5-1: travel() 이동 파이프라인 진입점.
     *
     * SM 활성 시 vanilla travel() 취소 + SM 파이프라인 실행:
     *   handleJumping → handleSwimming → handleLava →
     *   handleAlternativeFlying → handleLand → handleWallJumping →
     *   addMovementStat → handleExhaustion
     *
     * TODO Phase 7/8: SM 파이프라인 전체 구현 후 ci.cancel() 활성화.
     *   현재는 vanilla travel()이 그대로 실행됨.
     */
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void sm_travel(Vec3d movementInput, CallbackInfo ci) {
        // TODO Phase 7: SM 활성 판정 + 파이프라인 실행
        // if (!(this instanceof PlayerEntity)) return;
        // SmartMoving.runPipeline((LivingEntity)(Object)this, movementInput, ci);
    }

    /**
     * 5-5 (서버): SM 커스텀 클라이밍 중 isClimbing() = false 강제.
     *
     * 목적 1: applyClimbingSpeed() x/z ±0.15F 클램프 차단
     * 목적 2: applyMovementInput()에서 isClimbing() 시 y=0.2 강제 적용 차단
     *
     * ServerPlayerEntity 인스턴스 확인 → SmartMovingServer 상태 기반 판정.
     * 클라이언트 측 동등 로직 → MixinLivingEntityClient.sm_isClimbing_client()
     */
    @Inject(method = "isClimbing", at = @At("HEAD"), cancellable = true)
    private void sm_isClimbing_server(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);
        if (sm.isClimbing || sm.isCrawlClimbing || sm.isCeilingClimbing) {
            cir.setReturnValue(false);
        }
    }
}
