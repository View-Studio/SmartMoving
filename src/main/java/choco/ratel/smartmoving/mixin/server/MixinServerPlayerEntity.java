package choco.ratel.smartmoving.mixin.server;

import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 3-3, 3-4, 3-5: 서버 플레이어 Mixin.
 *
 * ServerPlayerEntity를 대상으로 하여:
 * - 3-4: isSneaking() 오버라이드 (forceIsSneaking)
 * - 3-5: isInsideWall() 억제 (crawlingCooldown)
 * - 3-3: addExhaustion() 차단 (disableAddExhaustion)
 *
 * 이 Mixin이 ServerPlayerEntity를 대상으로 하는 이유:
 * isSneaking/isInsideWall/addExhaustion은 부모 클래스에 정의되어 있지만,
 * Mixin이 ServerPlayerEntity에 새 오버라이드를 삽입하므로
 * 서버 플레이어에만 SM 동작이 적용된다.
 */
@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity {

    /**
     * 3-4: isSneaking() 오버라이드.
     * forceIsSneaking이 null이 아닐 때만 강제 반환값을 덮어쓴다.
     * null이면 vanilla 동작 그대로 유지.
     *
     * 원본: SmartMovingServerPlayerBase.isSneaking() → moving.isSneaking()
     *       SmartMovingServer.isSneaking(): forceIsSneaking != null ? forceIsSneaking : localIsSneaking()
     */
    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void sm_isSneaking(CallbackInfoReturnable<Boolean> cir) {
        SmartMovingServer sm = SmartMovingServer.get((ServerPlayerEntity)(Object)this);
        if (sm.forceIsSneaking != null) {
            cir.setReturnValue(sm.forceIsSneaking);
        }
    }

    /**
     * 3-5: isInsideWall() 억제.
     * 크롤링 종료 직후 crawlingCooldown 틱 동안 블록 내부 판정을 억제하여
     * 크롤링 출구에서 플레이어가 벽 안에 갇히는 현상을 방지한다.
     *
     * 원본: SmartMovingServerPlayerBase.isEntityInsideOpaqueBlock()
     *       → moving.isEntityInsideOpaqueBlock()
     *       → crawlingCooldown > 0이면 false 반환
     */
    @Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
    private void sm_isInsideWall(CallbackInfoReturnable<Boolean> cir) {
        SmartMovingServer sm = SmartMovingServer.get((ServerPlayerEntity)(Object)this);
        if (sm.crawlingCooldown > 0) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 3-3: addExhaustion() 소진 차단.
     * disableAddExhaustion이 true인 동안 vanilla 소진 추가를 전부 차단한다.
     * SM이 beforeAddMovingHungerBatch()를 호출하여 이 플래그를 세우면,
     * SM 자체 허기 계산이 완료될 때까지 vanilla 소진이 적용되지 않는다.
     *
     * 원본: SmartMovingServerPlayerBase.addExhaustion(exhaustion)
     *       → moving.addExhaustion(exhaustion)
     *       → disableAddExhaustion이면 return (localAddExhaustion 호출 안 함)
     *
     * TODO Phase 3 (미완): addMovementStat HEAD+TAIL에서 배치 처리 및 포션 효과 메서드 역전 처리.
     *   addMovementStat → 1.21.1 Yarn 메서드명 확인 후 구현
     *   tickStatusEffects HEAD/TAIL → beforeAddMovingHungerBatch/afterAddMovingHungerBatch 역전 처리
     */
    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void sm_addExhaustion(float exhaustion, CallbackInfo ci) {
        SmartMovingServer sm = SmartMovingServer.get((ServerPlayerEntity)(Object)this);
        if (sm.disableAddExhaustion) {
            ci.cancel();
        }
    }
}
