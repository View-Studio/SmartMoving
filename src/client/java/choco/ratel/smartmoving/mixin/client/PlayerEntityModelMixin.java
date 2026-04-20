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
 * 원본 SmartMoving의 팔/다리 각도는 bipedOuter(X틸트)를 부모로 하는 LOCAL 좌표계 값이었다.
 * 1.21.1의 flat 모델에서는 모든 파트가 독립적이므로 3D 분해가 필요하다.
 *
 * 원본 local Z(roll) → 1.21.1 world 변환:
 *   world_yaw  = -localZ × sin(outerX)
 *   world_roll =  localZ × cos(outerX)
 *
 * 원본 local X(pitch) → world pitch는 X축 회전이 X축을 보존하므로 그대로 덧셈:
 *   world_pitch = outerX + localX
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

        // 이전 프레임 잔류값 초기화 (vanilla가 관리하지 않는 축만)
        model.body.pivotY  = 0F;
        model.body.yaw     = 0F;
        model.body.roll    = 0F;
        model.rightArm.yaw  = 0F;  model.leftArm.yaw  = 0F;
        model.rightArm.roll = 0F;  model.leftArm.roll = 0F;
        model.rightLeg.yaw  = 0F;  model.leftLeg.yaw  = 0F;
        model.rightLeg.roll = 0F;  model.leftLeg.roll = 0F;

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
    // 원본: bipedTorso(pivotY=3F)가 팔/다리 부모. outerX = QUARTER - THIRTYTWOTH.
    // local X pitch는 그대로 덧셈. local Z(roll)는 없음.

    private static void applyCrawlingAngles(BipedEntityModel<?> m, float dist, float speed) {
        float d = dist * 1.3F;
        float walkFactor  = factor(speed, 0F, 0.12951545F);
        float standFactor = factor(speed, 0.12951545F, 0F);

        float bodyPitch = QUARTER - THIRTYTWOTH;
        m.body.pitch  = bodyPitch;
        m.body.pivotY = 3F;
        m.body.roll   = (float) Math.cos(d + QUARTER) * SIXTYFOURTH * walkFactor;
        // flat 모델: head world pitch = 0
        m.head.pitch  = 0F;

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
    // 원본: bipedOuter.X 없음 → world 공간 직접. 보정 불필요.

    private static void applyClimbingAngles(BipedEntityModel<?> m, float dist, float speed) {
        float walkFactor = factor(speed, 0F, 0.3F);

        m.leftArm.pitch  = (float) Math.cos(dist + HALF) * 0.52F * walkFactor - QUARTER;
        m.rightArm.pitch = (float) Math.cos(dist) * 0.52F * walkFactor - QUARTER;

        m.leftLeg.pitch  = (float) Math.cos(dist) * 0.52F * walkFactor;
        m.rightLeg.pitch = (float) Math.cos(dist + HALF) * 0.52F * walkFactor;
    }

    // ── 천장 클라이밍 ─────────────────────────────────────────────────────────
    // 원본: bipedOuter.Y (yaw만). 팔 local Z = -rotateY로 부모 yaw 상쇄.
    // flat 모델: 부모 yaw 없으므로 팔 yaw = 0F (world yaw = 0 그대로).

    private static void applyCeilingClimbingAngles(BipedEntityModel<?> m, float dist, float speed) {
        float walkFactor  = factor(speed, 0F, 0.12951545F);
        float standFactor = factor(speed, 0.12951545F, 0F);

        float rotateY = (float) Math.cos(dist) * 0.44F * walkFactor;
        m.body.yaw = rotateY;

        m.leftArm.pitch  = ((float) Math.cos(dist) * 0.52F + HALF) * walkFactor + HALF * standFactor;
        m.leftArm.yaw    = 0F;
        m.rightArm.pitch = ((float) Math.cos(dist + HALF) * 0.52F - HALF) * walkFactor - HALF * standFactor;
        m.rightArm.yaw   = 0F;

        m.leftLeg.pitch  = -(float) Math.cos(dist) * 0.12F * walkFactor;
        m.rightLeg.pitch = -(float) Math.cos(dist + HALF) * 0.32F * walkFactor;
    }

    // ── 슬라이딩 ──────────────────────────────────────────────────────────────
    // 원본: bipedOuter(pivotY=5F).X = QUARTER. local X pitch는 그대로 덧셈.

    private static void applySlidingAngles(BipedEntityModel<?> m, float dist, float speed) {
        float walkFactor = factor(speed, 0F, 0.4F);
        float bodyPitch  = QUARTER;

        m.body.pitch  = bodyPitch;
        m.body.pivotY = 5F;
        // flat 모델: head world pitch = 0
        m.head.pitch  = 0F;

        m.rightArm.pitch = bodyPitch + (float) Math.cos(dist + QUARTER) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        m.rightArm.yaw   = -QUARTER;
        m.leftArm.pitch  = bodyPitch + (float) Math.cos(dist - HALF) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        m.leftArm.yaw    = QUARTER;

        m.rightLeg.roll  =  THIRTYTWOTH;
        m.leftLeg.roll   = -THIRTYTWOTH;
    }

    // ── 수영 ──────────────────────────────────────────────────────────────────
    // 원본: bipedOuter.X = outerX (QUARTER - SIXTEENTH*combined).
    // 팔 local X(pitch): X축 회전 → world pitch = outerX + localX (SIXTEENTH*combined 상쇄 → QUARTER + cycling)
    // 팔 local Z(spread): 3D 분해 → world_yaw = -localZ*sin(outerX), world_roll = localZ*cos(outerX)
    // 다리 local X: world pitch = outerX + localX (그대로 덧셈)

    private static void applySwimmingAngles(BipedEntityModel<?> m, float dist, float speed, float time) {
        float walkFactor  = factor(speed, 0.15679921F, 0.52264464F);
        float sneakFactor = Math.min(factor(speed, 0F, 0.15679921F), factor(speed, 0.52264464F, 0.15679921F));
        float standFactor = factor(speed, 0.15679921F, 0F);
        float combined    = standFactor + sneakFactor;
        float outerX      = QUARTER - SIXTEENTH * combined;
        float sinO        = (float) Math.sin(outerX);
        float cosO        = (float) Math.cos(outerX);

        m.body.pitch = outerX;

        // 팔 spread (local Z) → 3D 분해
        float timeCos = (float) Math.cos(time * 0.1F);
        float rArmZ   =  QUARTER + EIGHTH + timeCos * combined * 0.8F;
        float lArmZ   = -(QUARTER + EIGHTH) - timeCos * combined * 0.8F;
        m.rightArm.yaw  = -rArmZ * sinO;
        m.rightArm.roll =  rArmZ * cosO;
        m.leftArm.yaw   = -lArmZ * sinO;
        m.leftArm.roll  =  lArmZ * cosO;

        // 팔 pitch: outerX + local = QUARTER + cycling (SIXTEENTH*combined 상쇄)
        m.rightArm.pitch = QUARTER + (((dist * 0.5F) % WHOLE) - HALF) * walkFactor;
        m.leftArm.pitch  = QUARTER + (((dist * 0.5F + HALF) % WHOLE) - HALF) * walkFactor;

        // 다리: world pitch = outerX + local X
        m.rightLeg.pitch = outerX + (float) Math.cos(dist) * 0.52264464F * walkFactor;
        m.leftLeg.pitch  = outerX + (float) Math.cos(dist + HALF) * 0.52264464F * walkFactor;
    }

    // ── 잠수 ──────────────────────────────────────────────────────────────────
    // 원본: bipedOuter.X = outerX (QUARTER - vAngle). 팔/다리 모두 local Z(roll) 사용.
    // local Z → 3D 분해: world_yaw = -localZ*sin(outerX), world_roll = localZ*cos(outerX)
    // 팔/다리 pitch = outerX (몸통 방향으로 정렬, local X = 0이므로 outerX만)

    private static void applyDivingAngles(BipedEntityModel<?> m, float dist, float speed, float headPitch) {
        float vAngle      = (float) Math.toRadians(headPitch);
        float walkFactor  = factor(speed, 0.15679921F, 0.52264464F);
        float standFactor = factor(speed, 0.15679921F, 0F);
        float outerX      = QUARTER - vAngle;
        float sinO        = (float) Math.sin(outerX);
        float cosO        = (float) Math.cos(outerX);

        m.body.pitch = outerX;

        // 팔 local Z → 3D 분해
        float rArmZ = ((float) Math.cos(dist + HALF) * 0.52264464F * 2.5F + QUARTER) * walkFactor + (QUARTER + EIGHTH) * standFactor;
        float lArmZ = ((float) Math.cos(dist) * 0.52264464F * 2.5F - QUARTER) * walkFactor - (QUARTER + EIGHTH) * standFactor;
        m.rightArm.pitch = outerX;
        m.rightArm.yaw   = -rArmZ * sinO;
        m.rightArm.roll  =  rArmZ * cosO;
        m.leftArm.pitch  = outerX;
        m.leftArm.yaw    = -lArmZ * sinO;
        m.leftArm.roll   =  lArmZ * cosO;

        // 다리 local Z → 3D 분해
        float rLegZ = ((float) Math.cos(dist) + 1F) * 0.52264464F * walkFactor + SIXTEENTH * standFactor;
        float lLegZ = ((float) Math.cos(dist + HALF) - 1F) * 0.52264464F * walkFactor - SIXTEENTH * standFactor;
        m.rightLeg.pitch = outerX;
        m.rightLeg.yaw   = -rLegZ * sinO;
        m.rightLeg.roll  =  rLegZ * cosO;
        m.leftLeg.pitch  = outerX;
        m.leftLeg.yaw    = -lLegZ * sinO;
        m.leftLeg.roll   =  lLegZ * cosO;
    }

    // ── 헤드 점프 ────────────────────────────────────────────────────────────
    // 원본: bipedOuter.X = outerX (QUARTER - vAngle).
    // 팔 local X(pitch) = -bendFactor*EIGHTH → world pitch = outerX + local X
    // 팔 local Z(spread) → 3D 분해: world_yaw = -localZ*sin(outerX), world_roll = localZ*cos(outerX)
    // head: world pitch = outerX + (-outerX/2) = outerX/2

    private static void applyHeadJumpingAngles(BipedEntityModel<?> m, float headPitch) {
        float vAngle     = (float) Math.toRadians(headPitch);
        float bendFactor = Math.min(factor(vAngle, QUARTER, 0F), factor(vAngle, -QUARTER, 0F));
        float armFactorZ = factor(vAngle, QUARTER, -QUARTER);
        float outerX     = QUARTER - vAngle;
        float sinO       = (float) Math.sin(outerX);
        float cosO       = (float) Math.cos(outerX);

        m.body.pitch = outerX;
        // world head pitch = outerX + (-outerX/2) = outerX/2
        m.head.pitch = outerX / 2F;

        // 팔 pitch: world = outerX + (-bendFactor * EIGHTH)
        // 팔 Z(spread) → 3D 분해
        float rArmZ =  HALF - SIXTEENTH + armFactorZ * EIGHTH;
        float lArmZ =  SIXTEENTH - HALF - armFactorZ * EIGHTH;
        m.rightArm.pitch = outerX - bendFactor * EIGHTH;
        m.rightArm.yaw   = -rArmZ * sinO;
        m.rightArm.roll  =  rArmZ * cosO;
        m.leftArm.pitch  = outerX - bendFactor * EIGHTH;
        m.leftArm.yaw    = -lArmZ * sinO;
        m.leftArm.roll   =  lArmZ * cosO;
    }
}
