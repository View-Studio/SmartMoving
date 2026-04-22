package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 6-1 (클라이언트): getBaseDimensions() 커스텀 EntityDimensions 반환.
 * 6-4 (클라이언트): updatePose() Mixin — SM 상태별 포즈 강제 설정.
 *
 * getDimensions()는 final → getBaseDimensions()가 유일한 오버라이드 진입점.
 * Yarn: getBaseDimensions (intermediary: method_55694 확인 완료)
 * Yarn: updatePose (intermediary: method_7318 확인 완료)
 *
 * 서버 측 → MixinPlayerEntity.sm_getBaseDimensions_server(), sm_updatePose_server()
 */
@Mixin(PlayerEntity.class)
@Environment(EnvType.CLIENT)
public abstract class MixinPlayerEntityClient {

    /**
     * 6-1 (클라이언트): SM 포즈별 커스텀 EntityDimensions 반환.
     *
     * SLIDING 포즈: SM isHeadJumping/isSliding 전용 (vanilla PlayerEntity는 SLIDING을 설정하지 않음).
     *   → SM 원본: height=0.8F, eyeHeight=player.height-0.18F=0.62F (pose_strategy.md M-04 확인)
     *
     * SWIMMING 포즈 + isCrawling: vanilla 0.6H → SM 크롤링 1.0H로 교체.
     *   → SM 원본 크롤링 hitbox: width=0.6F, height=1.0F (1블록 공간 통과)
     */
    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void sm_getBaseDimensions_client(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (pose == EntityPose.SLIDING) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
            return;
        }
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isCrawling && pose == EntityPose.SWIMMING) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 1.0F).withEyeHeight(0.4F));
        }
    }

    /**
     * 11-4 (클라이언트): SM 비행 비활성화 시 공중 이동 억제.
     *
     * vanilla: flying → flySpeed, !flying+sprinting → 0.026F, otherwise → 0.02F.
     * SM: flying && !cfg.fly → 0.05F (A-30 확인값).
     * 이 메서드를 0.05F로 제한하면 vanilla travel()의 공중 XZ 가속이 억제된다.
     */
    @Inject(method = "getOffGroundSpeed", at = @At("HEAD"), cancellable = true)
    private void sm_getOffGroundSpeed(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        if (player.getAbilities().flying && !SmartMovingConfig.Config.fly) {
            cir.setReturnValue(0.05F);
        }
    }

    /**
     * 6-4 (클라이언트): SM 이동 상태에서 vanilla updatePose() 취소 후 SM 포즈 강제 설정.
     *
     * vanilla updatePose()는 매 틱 포즈를 STANDING/CROUCHING/SWIMMING 등으로 덮어쓰므로
     * SM 상태 활성 중 @HEAD에서 취소하여 SM 포즈를 유지한다.
     *
     * 우선순위 (pose_strategy.md 5-2):
     *   isCrawling → SWIMMING (1.0H, leaningPitch/setupTransforms는 sm_isInSwimmingPose_client로 억제)
     *   isHeadJumping | isSliding → SLIDING (0.8H, vanilla 충돌 없음)
     *   그 외 → vanilla updatePose() 정상 실행
     */
    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void sm_updatePose_client(CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);

        if (sm.isCrawling) {
            player.setPose(EntityPose.SWIMMING);
            ci.cancel();
        } else if (sm.isHeadJumping || sm.isSliding) {
            player.setPose(EntityPose.SLIDING);
            ci.cancel();
        }
    }

    /**
     * canTriggerWalking — SM 클라이밍/잠수 중 걷기 트리거 억제.
     * 원본: SmartMovingSelf.canTriggerWalking() (행 1469-1471): return !isClimbing && !isDiving
     *
     * 클라이밍/잠수 중 보행음·보행 파티클이 발생하지 않도록 false 반환.
     */
    @Inject(method = "canTriggerWalking", at = @At("HEAD"), cancellable = true)
    private void sm_canTriggerWalking(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!cfg.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isClimbing || sm.isDiving) {
            cir.setReturnValue(false);
        }
    }

    /**
     * pushOutOfBlocks: 서버→클라이언트 위치 동기화 직후 억제.
     * 원본: SmartMovingSelf.pushOutOfBlocks (SmartMovingSelf.md L1263-1277)
     *
     * multiPlayerInitialized > 0이면 실행 취소 후 카운터 1 감소.
     * 1.21.1: Entity.pushOutOfBlocks는 void — cancel()로 실행 억제.
     */
    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void sm_pushOutOfBlocks(double x, double y, double z, CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.multiPlayerInitialized > 0) {
            sm.multiPlayerInitialized--;
            ci.cancel();
        }
    }
}
