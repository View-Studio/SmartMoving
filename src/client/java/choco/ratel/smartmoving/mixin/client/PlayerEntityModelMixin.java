package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static choco.ratel.smartmoving.client.render.AnimationUtil.*;

/**
 * 핵심 설계 원칙:
 * 원본 SmartMoving의 팔/다리 각도는 bipedOuter/bipedTorso를 부모로 하는 LOCAL 좌표계 값이었다.
 * 1.21.1의 flat 모델에서는 모든 파트가 world 좌표계에서 독립적이므로,
 * 각 파트의 월드 각도 = 부모 pitch(outerX) + 로컬 pitch 로 변환해야 한다.
 */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin {

    @Inject(
            method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",
            at = @At("TAIL")
    )
    private void smartMoving_setAngles(
            LivingEntity entity,
            float limbAngle,
            float limbDistance,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!(entity instanceof PlayerEntity player)) return;
        SmartMovingState state = player.getAttached(SmartMovingAttachments.STATE);
        if (state == null) return;

        @SuppressWarnings("unchecked")
        BipedEntityModel<LivingEntity> model = (BipedEntityModel<LivingEntity>) (Object) this;

        float speed = limbDistance;

        if (state.isCrawling) {
            applyCrawlingAngles(model, limbAngle, speed);
        } else if (state.isCeilingClimbing) {
            applyCeilingClimbingAngles(model, limbAngle, speed);
        } else if (state.isClimbing) {
            applyClimbingAngles(model, limbAngle, speed);
        } else if (state.isSliding) {
            applySlidingAngles(model, limbAngle, speed);
        } else if (state.isSwimming) {
            applySwimmingAngles(model, limbAngle, speed, animationProgress);
        } else if (state.isDiving) {
            applyDivingAngles(model, limbAngle, speed, headPitch);
        } else if (state.isHeadJumping) {
            applyHeadJumpingAngles(model, headPitch);
        }
    }

    // ── 기어가기 ──────────────────────────────────────────────────────────────
    // 원본: bipedTorso가 팔/다리의 부모. bodyPitch = bipedTorso.X = QUARTER - THIRTYTWOTH
    // 월드 공간 팔/다리 pitch = bodyPitch + local pitch

    private static void applyCrawlingAngles(BipedEntityModel<?> m, float dist, float speed) {
        float d = dist * 1.3F;
        float walkFactor  = factor(speed, 0F, 0.12951545F);
        float standFactor = factor(speed, 0.12951545F, 0F);

        float bodyPitch = QUARTER - THIRTYTWOTH;
        m.body.pitch = bodyPitch;
        m.body.roll  = (float) Math.cos(d + QUARTER) * SIXTYFOURTH * walkFactor;
        // head는 flat 모델에서 독립적 → world pitch=0 (앞 방향)으로 고정
        m.head.pitch = 0F;

        // 팔: world pitch = bodyPitch + local(HALF + EIGHTH)
        m.rightArm.pitch = bodyPitch + HALF + EIGHTH;
        m.rightArm.yaw   = -QUARTER;
        m.rightArm.roll  = ((float) Math.cos(d + HALF) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor + SIXTEENTH * standFactor;

        m.leftArm.pitch  = bodyPitch + HALF + EIGHTH;
        m.leftArm.yaw    = QUARTER;
        m.leftArm.roll   = ((float) Math.cos(d + HALF) * SIXTYFOURTH - THIRTYTWOTH) * walkFactor - SIXTEENTH * standFactor;

        // 다리: world pitch = bodyPitch + local
        m.rightLeg.pitch = bodyPitch + ((float) Math.cos(d - QUARTER) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor + THIRTYTWOTH * standFactor;
        m.rightLeg.roll  = ((float) Math.cos(d - QUARTER) + 1F) * 0.25F * walkFactor + THIRTYTWOTH * standFactor;

        m.leftLeg.pitch  = bodyPitch + ((float) Math.cos(d - HALF - QUARTER) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor + THIRTYTWOTH * standFactor;
        m.leftLeg.roll   = ((float) Math.cos(d - QUARTER) - 1F) * 0.25F * walkFactor - THIRTYTWOTH * standFactor;
    }

    // ── 클라이밍 ──────────────────────────────────────────────────────────────
    // 원본: bipedOuter.X 없음. 팔/다리 값이 직접 월드 공간. 보정 불필요.

    private static void applyClimbingAngles(BipedEntityModel<?> m, float dist, float speed) {
        float walkFactor = factor(speed, 0F, 0.3F);

        m.leftArm.pitch  = (float) Math.cos(dist + HALF) * 0.52F * walkFactor - QUARTER;
        m.leftArm.yaw    = 0F;
        m.rightArm.pitch = (float) Math.cos(dist) * 0.52F * walkFactor - QUARTER;
        m.rightArm.yaw   = 0F;

        m.leftLeg.pitch  = (float) Math.cos(dist) * 0.52F * walkFactor;
        m.rightLeg.pitch = (float) Math.cos(dist + HALF) * 0.52F * walkFactor;
    }

    // ── 천장 클라이밍 ─────────────────────────────────────────────────────────
    // 원본: bipedOuter.Y (yaw만). X 회전 없으므로 팔 pitch는 이미 월드 공간.
    // 팔 yaw = -rotateY 로 bipedOuter.Y 상쇄 (부모-자식 관계 증거).

    private static void applyCeilingClimbingAngles(BipedEntityModel<?> m, float dist, float speed) {
        float walkFactor  = factor(speed, 0F, 0.12951545F);
        float standFactor = factor(speed, 0.12951545F, 0F);

        float rotateY = (float) Math.cos(dist) * 0.44F * walkFactor;
        m.body.yaw = rotateY;

        m.leftArm.pitch  = ((float) Math.cos(dist) * 0.52F + HALF) * walkFactor + HALF * standFactor;
        // 원본에서 -rotateY는 bipedOuter.Y(부모 yaw)를 상쇄하기 위한 LOCAL 값이었다.
        // 1.21.1 flat 모델에서 팔은 body의 자식이 아니므로 world yaw = 0을 그냥 쓴다.
        m.leftArm.yaw    = 0F;
        m.rightArm.pitch = ((float) Math.cos(dist + HALF) * 0.52F - HALF) * walkFactor - HALF * standFactor;
        m.rightArm.yaw   = 0F;

        m.leftLeg.pitch  = -(float) Math.cos(dist) * 0.12F * walkFactor;
        m.rightLeg.pitch = -(float) Math.cos(dist + HALF) * 0.32F * walkFactor;
    }

    // ── 슬라이딩 ──────────────────────────────────────────────────────────────
    // 원본: bipedOuter.X = QUARTER (부모). bodyPitch = QUARTER.
    // 팔 world pitch = QUARTER + local pitch

    private static void applySlidingAngles(BipedEntityModel<?> m, float dist, float speed) {
        float walkFactor = factor(speed, 0F, 0.4F);
        float bodyPitch  = QUARTER;

        m.body.pitch = bodyPitch;
        // flat 모델 → head world pitch = 0 (앞 방향)
        m.head.pitch = 0F;

        m.rightArm.pitch = bodyPitch + (float) Math.cos(dist + QUARTER) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        m.rightArm.yaw   = -QUARTER;
        m.leftArm.pitch  = bodyPitch + (float) Math.cos(dist - HALF) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        m.leftArm.yaw    = QUARTER;

        m.rightLeg.roll  =  THIRTYTWOTH;
        m.leftLeg.roll   = -THIRTYTWOTH;
    }

    // ── 수영 ──────────────────────────────────────────────────────────────────
    // 원본: bipedOuter.X = QUARTER - SIXTEENTH*combined (부모 pitch = outerX).
    // 팔 world pitch: outerX + local. local에 SIXTEENTH*combined 항이 있어 outerX와 상쇄됨.
    // → 팔 world pitch = QUARTER + cycling (단순화).

    private static void applySwimmingAngles(BipedEntityModel<?> m, float dist, float speed, float time) {
        float walkFactor  = factor(speed, 0.15679921F, 0.52264464F);
        float sneakFactor = Math.min(factor(speed, 0F, 0.15679921F), factor(speed, 0.52264464F, 0.15679921F));
        float standFactor = factor(speed, 0.15679921F, 0F);
        float combined    = standFactor + sneakFactor;
        float outerX      = QUARTER - SIXTEENTH * combined;

        m.body.pitch = outerX;

        // 팔 roll: elbow/arm spread (LOCAL 그대로, 근사치)
        float timeCos = (float) Math.cos(time * 0.1F);
        m.rightArm.roll  =  QUARTER + EIGHTH + timeCos * combined * 0.8F;
        m.leftArm.roll   = -(QUARTER + EIGHTH) - timeCos * combined * 0.8F;

        // 팔 pitch: outerX + local → SIXTEENTH*combined 상쇄 → QUARTER + cycling
        m.rightArm.pitch = QUARTER + (((dist * 0.5F) % WHOLE) - HALF) * walkFactor;
        m.leftArm.pitch  = QUARTER + (((dist * 0.5F + HALF) % WHOLE) - HALF) * walkFactor;

        // 다리: outerX + local pitch
        m.rightLeg.pitch = outerX + (float) Math.cos(dist) * 0.52264464F * walkFactor;
        m.leftLeg.pitch  = outerX + (float) Math.cos(dist + HALF) * 0.52264464F * walkFactor;
    }

    // ── 잠수 ──────────────────────────────────────────────────────────────────
    // 원본: bipedOuter.X = QUARTER - vAngle (부모 pitch = outerX).
    // 원본의 LOCAL Z(roll) 동작 → 플레이어가 수평일 때 world X(pitch)로 매핑됨.
    // 팔/다리 world pitch = outerX + original_local_roll_formula

    private static void applyDivingAngles(BipedEntityModel<?> m, float dist, float speed, float headPitch) {
        float vAngle      = (float) Math.toRadians(headPitch);
        float walkFactor  = factor(speed, 0.15679921F, 0.52264464F);
        float standFactor = factor(speed, 0.15679921F, 0F);
        float outerX      = QUARTER - vAngle;

        m.body.pitch = outerX;

        // 팔: outerX + local Z(roll) → world X(pitch) 변환
        m.rightArm.pitch = outerX + ((float) Math.cos(dist + HALF) * 0.52264464F * 2.5F + QUARTER) * walkFactor + (QUARTER + EIGHTH) * standFactor;
        m.leftArm.pitch  = outerX + ((float) Math.cos(dist) * 0.52264464F * 2.5F - QUARTER) * walkFactor - (QUARTER + EIGHTH) * standFactor;

        // 다리: outerX + local Z(roll) → world X(pitch) 변환
        m.rightLeg.pitch = outerX + ((float) Math.cos(dist) + 1F) * 0.52264464F * walkFactor + SIXTEENTH * standFactor;
        m.leftLeg.pitch  = outerX + ((float) Math.cos(dist + HALF) - 1F) * 0.52264464F * walkFactor - SIXTEENTH * standFactor;
    }

    // ── 헤드 점프 ────────────────────────────────────────────────────────────
    // 원본: bipedOuter.X = QUARTER - vAngle (부모 pitch = outerX).
    // 팔 world pitch = outerX + local(-bendFactor * EIGHTH).

    private static void applyHeadJumpingAngles(BipedEntityModel<?> m, float headPitch) {
        float vAngle     = (float) Math.toRadians(headPitch);
        float bendFactor = Math.min(factor(vAngle, QUARTER, 0F), factor(vAngle, -QUARTER, 0F));
        float armFactorZ = factor(vAngle, QUARTER, -QUARTER);
        float outerX     = QUARTER - vAngle;

        m.body.pitch = outerX;
        // 원본: head child of bipedOuter → worldPitch = outerX + (-outerX/2) = outerX/2
        // 1.21.1 flat: world pitch = local pitch 그대로 → outerX/2
        m.head.pitch = outerX / 2F;

        // 팔 pitch: outerX + local(-bendFactor*EIGHTH)
        m.rightArm.pitch = outerX - bendFactor * EIGHTH;
        m.leftArm.pitch  = outerX - bendFactor * EIGHTH;
        // 팔 roll: LOCAL Z 그대로 (팔 벌림 자세)
        m.rightArm.roll  =  HALF - SIXTEENTH + armFactorZ * EIGHTH;
        m.leftArm.roll   =  SIXTEENTH - HALF - armFactorZ * EIGHTH;
    }
}
