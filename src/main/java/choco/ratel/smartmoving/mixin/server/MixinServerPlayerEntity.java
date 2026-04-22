package choco.ratel.smartmoving.mixin.server;

import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
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
     * 3-1-B: crawlingCooldown 카운트다운 (beforeOnUpdate 해당).
     * 크롤링 종료 후 10틱간 isInsideWall() 억제 타이머를 매 틱 감소한다.
     * 원본: SmartMovingServer.beforeOnUpdate() → if (crawlingCooldown > 0) crawlingCooldown--
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void sm_beforeTick(CallbackInfo ci) {
        SmartMovingServer sm = SmartMovingServer.get((ServerPlayerEntity)(Object)this);
        if (sm.crawlingCooldown > 0) sm.crawlingCooldown--;
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
     */
    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void sm_addExhaustion(float exhaustion, CallbackInfo ci) {
        SmartMovingServer sm = SmartMovingServer.get((ServerPlayerEntity)(Object)this);
        if (sm.disableAddExhaustion) {
            ci.cancel();
        }
    }

    // ── C-22: travel() HEAD/TAIL — SM 소진 배치 처리 ─────────────────────────
    // 원본: addMovementStat()이 없어짐 → travel() 인라인 처리 (A-27 확인)
    // HEAD: beforeAddMovingHungerBatch() — 소진 차단 시작
    // TAIL: hunger 반영 후 afterAddMovingHungerBatch() — 소진 차단 해제

    @Inject(method = "travel", at = @At("HEAD"))
    private void sm_beforeTravel(Vec3d movementInput, CallbackInfo ci) {
        SmartMovingServer.get((ServerPlayerEntity)(Object)this).beforeAddMovingHungerBatch();
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void sm_afterTravel(Vec3d movementInput, CallbackInfo ci) {
        SmartMovingServer sm = SmartMovingServer.get((ServerPlayerEntity)(Object)this);
        if (sm.hunger > 0F) {
            sm.disableAddExhaustion = false;
            ((PlayerEntity)(Object)this).addExhaustion(sm.hunger);
            sm.hunger = 0F;
        }
        sm.afterAddMovingHungerBatch();
    }
}
