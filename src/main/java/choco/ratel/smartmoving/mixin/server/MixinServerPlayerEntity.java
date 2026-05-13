package choco.ratel.smartmoving.mixin.server;

import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 서버 플레이어 Mixin — ServerPlayerEntity에서 직접 override하는 메서드만 inject.
 *
 * - 3-1-B: tick() HEAD — crawlingCooldown 카운트다운
 * - C-22: travel() HEAD/TAIL — SM 소진 배치 처리 (tick, travel 모두 ServerPlayerEntity에서 직접 override)
 *
 * 주의: isSneaking(Entity), isInsideWall(LivingEntity), addExhaustion(PlayerEntity)은
 * ServerPlayerEntity에 선언되어 있지 않으므로 각 선언 클래스의 Mixin에서 처리한다.
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
     * 🔵 [TX-MULTI-SERVER-TICK] dump — server entity.y 가 self c2s packet 후 +1m 갱신 timing 추적.
     *   SERVER-RECV (HJ/SL/CR/ICC) 시점에 tranDumpRemainingTicks=10 set → 매 server tick TAIL 출력.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void sm_tranDumpServerTickTail(CallbackInfo ci) {
        ServerPlayerEntity sp = (ServerPlayerEntity)(Object)this;
        SmartMovingServer sm = SmartMovingServer.get(sp);
        if (sm.tranDumpRemainingTicks <= 0) return;
        sm.tranDumpRemainingTicks--;
        net.minecraft.entity.EntityPose _pose = sp.getPose();
        net.minecraft.entity.EntityDimensions _dim = sp.getDimensions(_pose);
        System.out.println(String.format(
                "[TX-MULTI-SERVER-TICK] uuid=%s tick=%d y=%.3f bbMinY=%.3f isHJ=%b isSL=%b isCR=%b isICC=%b POSE=%s dimH=%.2f dimEye=%.2f onG=%b",
                sp.getUuid(), sp.age, sp.getY(), sp.getBoundingBox().minY,
                sm.isHeadJumping, sm.isSliding, sm.isCrawling, sm.isCrawlClimbing,
                _pose, _dim.height(), _dim.eyeHeight(), sp.isOnGround()));
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
