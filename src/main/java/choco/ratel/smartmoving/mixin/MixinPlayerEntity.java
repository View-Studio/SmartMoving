package choco.ratel.smartmoving.mixin;

import choco.ratel.smartmoving.config.SmartMovingConfig;
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
     * **포커스 #2.7 Phase C 재작성 (세션 4, 2026-04-25)**: 클라와 완전 대칭 (옵션 3).
     *   원본 `setHeightOffset(-1F)` (SmartMovingSelf L1694-L1704) → `sp.height = 1.8F + (-1F)
     *   = 0.8F` + `getEyeHeight() = height - 0.18F = 0.62F` (ServerPlayerBase L142-L145).
     *   → 모든 small 상태 (isCrawling/isCrawlClimbing/isHeadJumping/isSliding/isSwimming/
     *   isDiving/isFlying/isLevitating) 통합 0.6×0.8 + 0.62F.
     *
     * 클라 (MixinPlayerEntityClient.sm_getBaseDimensions_client) 와 동일 8 SM OR 분기.
     * isFlying 은 vanilla `abilities.flying + cfg.fly + !isSwimming + !isDiving` 원본 공식
     * (SmartMovingSelf L2509-L2515) 1:1.
     */
    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void sm_getBaseDimensions_server(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);

        // 🔴 BUG-29 진짜 원인 정정 (Flying Phase / 세션 45): 비행/Levitate 분기 제거 (서버측).
        //   클라측 sm_getBaseDimensions_client 와 동일 정정 — 비행 시 vanilla STANDING 통과.
        //   원본 1.7.10 vanilla 에 EntityPose 시스템 없음 → 비행 시 STANDING 모델 + eyeHeight
        //   1.62 그대로. 1.21.1 잘못 매핑 = SLIDING 강제 → 카메라 발끝.
        //   클라/서버 대칭 유지: 둘 다 비행 시 vanilla STANDING POSE 통과 → datatracker 일관.
        //
        // 원본 setHeightOffset(-1F) 상태 전수 — Crawling/Sliding/Swimming 등만 0.6 × 0.8 +
        // eyeHeight 0.62F. isFlying/isLevitating 은 vanilla 통과.
        boolean smSmall = sm.isCrawling || sm.isCrawlClimbing
                       || sm.isHeadJumping || sm.isSliding
                       || sm.isSwimming || sm.isDiving;
        if (smSmall) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
            return;
        }

        // SLIDING POSE 가 vanilla 가 아닌 경로로 들어온 경우 보강 (외부 모드 등).
        if (pose == EntityPose.SLIDING) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
        }
    }

    /**
     * 6-4 (서버): SM 이동 상태에서 vanilla updatePose() 취소 후 SM 포즈 강제 설정.
     *
     * **포커스 #2.7 Phase D-1 (세션 4, 2026-04-25)**: 클라와 완전 대칭 (옵션 3).
     *   클라 (MixinPlayerEntityClient.sm_updatePose_client) 와 동일 4 분기 매핑:
     *     isCrawling || isCrawlClimbing            → SWIMMING (엎드림)
     *     isHeadJumping || isSliding               → SLIDING  (납작)
     *     isSwimming || isDiving                   → SWIMMING (수영)
     *     isFlying (원본 공식) || isLevitating     → SLIDING  (비행)
     *     이외                                     → vanilla 통과
     *
     * 클라/서버 POSE 동일 sync → datatracker 진동 0.
     */
    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void sm_updatePose_server(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);

        if (sm.isCrawling || sm.isCrawlClimbing) {
            player.setPose(EntityPose.SWIMMING);
            ci.cancel();
        } else if (sm.isHeadJumping || sm.isSliding) {
            player.setPose(EntityPose.SLIDING);
            ci.cancel();
        } else if (sm.isSwimming || sm.isDiving) {
            player.setPose(EntityPose.SWIMMING);
            ci.cancel();
        }
        // 🔴 BUG-29 진짜 원인 정정 (Flying Phase / 세션 45): 비행/Levitate 분기 제거 (서버측).
        //   클라측 sm_updatePose_client 와 동일 정정 — 비행 시 vanilla STANDING POSE 통과.
        //   원본 1.7.10 vanilla 에 POSE 시스템 없음 → 비행 시 STANDING 그대로. 1.21.1 잘못 매핑
        //   = SLIDING 강제 → 카메라 발끝. 정정: vanilla 통과 → STANDING POSE → eyeHeight 1.62.
        //   클라/서버 대칭 유지 → datatracker 진동 0.
        // isFlying/isLevitating/isDipping/그 외 → vanilla updatePose() 통과 (STANDING 등)
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
