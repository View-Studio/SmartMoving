package choco.ratel.smartmoving.mixin;

import choco.ratel.smartmoving.physics.CeilingClimbingHandler;
import choco.ratel.smartmoving.physics.ClimbingHandler;
import choco.ratel.smartmoving.physics.CrawlingHandler;
import choco.ratel.smartmoving.physics.SlidingHandler;
import choco.ratel.smartmoving.physics.SwimmingHandler;
import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityTravelMixin {

    /**
     * 크롤링/천장 클라이밍 중 수평 이동 입력을 배율 조정.
     */
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3d smartMoving_scaleCrawlSpeed(Vec3d movementInput) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return movementInput;

        SmartMovingState state = player.getAttached(SmartMovingAttachments.STATE);
        if (state == null) return movementInput;

        if (state.isCrawling) {
            float f = CrawlingHandler.CRAWL_SPEED_FACTOR;
            return movementInput.multiply(f, 1.0, f);
        }
        if (state.isCeilingClimbing) {
            float f = CeilingClimbingHandler.CEILING_SPEED_FACTOR;
            return movementInput.multiply(f, 1.0, f);
        }
        return movementInput;
    }

    /**
     * move() 호출 직전에 클라이밍/슬라이딩 속도를 적용.
     * travel()은 this.move(MovementType.SELF, this.getVelocity())를 호출하므로
     * 이 시점에서 setVelocity()를 호출하면 실제 이동에 반영된다.
     */
    @Inject(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"
            )
    )
    private void smartMoving_applyMovementOverride(Vec3d movementInput, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;

        SmartMovingState state = player.getAttached(SmartMovingAttachments.STATE);
        if (state == null) return;

        if (state.isClimbing) {
            double motionY;
            if (state.wantClimbUp)        motionY = ClimbingHandler.FAST_UP_MOTION;
            else if (state.wantClimbDown) motionY = ClimbingHandler.SINK_DOWN_MOTION;
            else                           motionY = 0.0D;

            Vec3d vel = player.getVelocity();
            player.setVelocity(vel.x * 0.3, motionY, vel.z * 0.3);
            player.fallDistance = 0F;

        } else if (state.isSliding) {
            Vec3d vel = player.getVelocity();
            Vec3d newVel = SlidingHandler.applySlidePhysics(state, player, vel);
            player.setVelocity(newVel);
            player.fallDistance = 0F;

        } else if (state.isCeilingClimbing) {
            double motionY = CeilingClimbingHandler.getCeilingMotionY(player);
            Vec3d vel = player.getVelocity();
            player.setVelocity(vel.x, motionY, vel.z);
            player.fallDistance = 0F;

        } else if (state.isSwimming || state.isDiving || state.isDipping) {
            Vec3d vel = player.getVelocity();
            Vec3d newVel = SwimmingHandler.applySwimPhysics(state, player, vel);
            player.setVelocity(newVel);
        }
    }
}
