package choco.ratel.smartmoving.mixin;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * SmartMoving 수영/잠수/딥핑 활성 시 vanilla SWIMMING 포즈 관련 동작 차단.
 *
 * isInSwimmingPose() = getPose() == SWIMMING 이 true일 때 vanilla가 하는 것:
 *   1. PlayerEntityRenderer.setupTransforms: -90° 엔티티 회전 (leaningPitch 기반)
 *   2. LivingEntity.tick: leaningPitch 증가 → setupTransforms 회전 점점 강해짐
 *   3. 0.6×0.6 히트박스
 * SM이 수영을 직접 제어할 때는 이 모든 동작이 불필요하고 충돌함.
 *
 * isInSwimmingPose()를 false로 오버라이드 →
 *   leaningPitch가 0으로 서서히 감소 → vanilla setupTransforms 회전 없음 → pivotZ 방식 정상 동작.
 */
@Mixin(LivingEntity.class)
public abstract class PlayerEntityPoseMixin {

    @Inject(method = "isInSwimmingPose", at = @At("RETURN"), cancellable = true)
    private void smartMoving_suppressSwimmingPose(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;

        SmartMovingState state = player.getAttached(SmartMovingAttachments.STATE);
        if (state == null) return;

        if (state.isSwimming || state.isDiving || state.isDipping) {
            cir.setReturnValue(false);
        }
    }
}
