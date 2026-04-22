package choco.ratel.smartmoving.mixin;

import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 5-5 (서버): isClimbing() 오버라이드 — SM 커스텀 클라이밍 중 false 반환.
 * 6-2 (서버): isInSwimmingPose() 오버라이드 — SM 크롤링 중 false 반환.
 * 3-3-C: tickStatusEffects HEAD/TAIL — 소진 배치 역전 처리.
 * 3-7: tickMovement TAIL — isSmall 시 아이템 습득 범위 확장.
 *
 * 참고: 서버 이동 파이프라인(floatKick/낙하거리/위치검사)은 MixinServerPlayNetworkHandler에서 처리.
 * 클라이언트 전용 항목(jump, applyClimbingSpeed, isClimbing) → MixinLivingEntityClient
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

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

    /**
     * 6-2 (서버): SM 크롤링 중 isInSwimmingPose() = false 강제.
     *
     * SM 크롤링은 SWIMMING 포즈를 사용하지만, vanilla isInSwimmingPose()=true가 되면:
     *   - setupTransforms Branch 2: X-90° 회전 + translate(0,-1,0.3) → 렌더 깨짐
     *   - updateLeaningPitch(): leaningPitch가 1.0까지 상승 → 자동 회전 발생
     *
     * 서버 측: ServerPlayerEntity 인스턴스 확인.
     * 클라이언트 측 → MixinLivingEntityClient.sm_isInSwimmingPose_client()
     * Yarn: isInSwimmingPose (intermediary: method_20232 확인 완료)
     */
    @Inject(method = "isInSwimmingPose", at = @At("HEAD"), cancellable = true)
    private void sm_isInSwimmingPose_server(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);
        if (sm.isCrawling || sm.isCrawlClimbing) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 3-7: afterOnLivingUpdate() — isSmall 시 아이템 습득 범위 Y+0.25F 확장.
     * 크롤링 중 바닥에 가까워 기본 AABB로 닿지 않는 아이템을 획득할 수 있도록 보정.
     * 원본: SmartMovingServer.afterOnLivingUpdate() / SmallSizeItemGrabHeight = 0.25F
     *
     * 알고리즘:
     *   offsetBox  = playerBox + (1, 0.25, 1) — Y방향 0.25 확장
     *   standardBox = offsetBox.expand(0, -0.25, 0) — Y 원상복구
     *   offsetBox에만 있고 standardBox에는 없는 엔티티 → onPlayerCollision
     */
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void sm_afterTickMovement(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);
        if (!sm.isSmall) return;
        if (player.getHealth() <= 0) return;

        Box offsetBox = player.getBoundingBox().expand(1.0, SmartMovingServer.SMALL_SIZE_ITEM_GRAB_HEIGHT, 1.0);
        List<Entity> offsetEntities = player.getWorld().getOtherEntities(player, offsetBox);
        if (offsetEntities.isEmpty()) return;

        Box standardBox = offsetBox.expand(0.0, -SmartMovingServer.SMALL_SIZE_ITEM_GRAB_HEIGHT, 0.0);
        List<Entity> standardEntities = player.getWorld().getOtherEntities(player, standardBox);

        for (Entity entity : offsetEntities) {
            if (standardEntities.contains(entity)) continue;
            if (!entity.isRemoved()) entity.onPlayerCollision(player);
        }
    }

    // ── C-23: tickStatusEffects() HEAD/TAIL — 포션 업데이트 사이클 역전 처리 ──
    // 원본: beforeUpdatePotionEffects→afterAddMovingHungerBatch,
    //       afterUpdatePotionEffects→beforeAddMovingHungerBatch (의도적 역전, A-28 확인)
    // HEAD: afterAddMovingHungerBatch (depth 감소)
    // TAIL: beforeAddMovingHungerBatch (depth 증가)

    @Inject(method = "tickStatusEffects", at = @At("HEAD"))
    private void sm_beforeTickStatusEffects(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer.get(player).afterAddMovingHungerBatch();
    }

    @Inject(method = "tickStatusEffects", at = @At("TAIL"))
    private void sm_afterTickStatusEffects(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer.get(player).beforeAddMovingHungerBatch();
    }

    /**
     * 3-5: isInsideWall() 억제.
     * 크롤링 종료 직후 crawlingCooldown 틱 동안 블록 내부 판정을 억제하여
     * 크롤링 출구에서 플레이어가 벽 안에 갇히는 현상을 방지한다.
     *
     * isInsideWall()은 LivingEntity에서 마지막으로 override되므로 여기서 inject.
     * ServerPlayerEntity에는 선언되어 있지 않아 MixinServerPlayerEntity에서 inject 불가.
     */
    @Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
    private void sm_isInsideWall(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);
        if (sm.crawlingCooldown > 0) {
            cir.setReturnValue(false);
        }
    }
}
