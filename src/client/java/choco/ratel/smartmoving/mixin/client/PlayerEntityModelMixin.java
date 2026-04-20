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
 * 1.21.1 ModelPart 회전 적용 순서: Rx(pitch) → Ry(yaw) → Rz(roll) (pitch-first 내재적 회전)
 *
 * 이 덕분에 원본 1.7.10 계층 구조를 그대로 이식할 수 있다:
 *   bipedOuter.rotateAngleX = outerX  →  body.pitch = outerX
 *   child.rotateAngleX = localX       →  part.pitch  = outerX + localX
 *   child.rotateAngleY = localY       →  part.yaw    = localY
 *   child.rotateAngleZ = localZ       →  part.roll   = localZ  (변환 불필요!)
 *
 * pivotY 처리 원칙:
 *   - 1.21.1 flat 모델에서 모든 파트가 독립적이므로, pivotY 오프셋은 관련 파트 전체에 설정해야 함.
 *   - 원본 bipedTorso.rotationPointY = 3F → head/body/arm 전체에 +3F
 *   - 원본 bipedOuter.rotationPointY = 5F → head/body/arm/leg 전체에 +5F
 *   - SmartMoving 상태 진입 시 모든 pivotY를 비-스니킹 기본값으로 리셋 후 상태별 오프셋 적용.
 *   - 상태 해제 시 vanilla setAngles가 sneaking/non-sneaking 블록에서 자동 복원하므로 별도 cleanup 불필요.
 *
 * 바닐라 기본 pivotY: head=0, body=0, rightArm=2, leftArm=2, rightLeg=12, leftLeg=12
 * 바닐라 스니킹 pivotY: head=4.2, body=3.2, rightArm=5.2, leftArm=5.2, rightLeg=12.2, leftLeg=12.2
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

        // 이전 프레임 잔류값 초기화 — vanilla가 관리하지 않는 yaw/roll 축만.
        model.body.yaw    = 0F;
        model.body.roll   = 0F;
        model.rightArm.yaw  = 0F;  model.leftArm.yaw  = 0F;
        model.rightArm.roll = 0F;  model.leftArm.roll = 0F;
        model.rightLeg.yaw  = 0F;  model.leftLeg.yaw  = 0F;
        model.rightLeg.roll = 0F;  model.leftLeg.roll = 0F;

        float speed = limbDistance;

        if (state.isCrawling) {
            applyCrawlingAngles(model, limbAngle, speed);
        } else if (state.isCeilingClimbing) {
            resetPivots(model);
            applyCeilingClimbingAngles(model, limbAngle, speed);
        } else if (state.isClimbing) {
            resetPivots(model);
            applyClimbingAngles(model, limbAngle, speed);
        } else if (state.isSliding) {
            applySlidingAngles(model, limbAngle, speed);
        } else if (state.isSwimming) {
            resetPivots(model);
            applySwimmingAngles(model, limbAngle, speed, animationProgress);
        } else if (state.isDiving) {
            resetPivots(model);
            applyDivingAngles(model, limbAngle, speed, headPitch);
        } else if (state.isHeadJumping) {
            resetPivots(model);
            applyHeadJumpingAngles(model, headPitch);
        }
    }

    // ── pivotY 기본값 복원 ───────────────────────────────────────────────────
    // vanilla setAngles가 sneaking 블록에서 pivotY를 변경할 수 있으므로
    // SmartMoving 상태 진입 시 비-스니킹 기본값으로 리셋.
    private static void resetPivots(BipedEntityModel<?> m) {
        m.head.pivotY     = 0F;
        m.body.pivotY     = 0F;
        m.rightArm.pivotY = 2F;
        m.leftArm.pivotY  = 2F;
        m.rightLeg.pivotY = 12F;
        m.leftLeg.pivotY  = 12F;
    }

    // ── 기어가기 ──────────────────────────────────────────────────────────────
    // 원본: bipedTorso.rotationPointY = 3F (상체 그룹 전체 이동)
    // 1.21.1 flat 모델: bipedTorso 그룹(head+body+arm)에 해당하는 파트 pivotY +3F
    // 다리는 bipedTorso 자식이 아니었으므로 pivotY 변경 없음.

    private static void applyCrawlingAngles(BipedEntityModel<?> m, float dist, float speed) {
        float d           = dist * 1.3F;
        float walkFactor  = factor(speed, 0F, 0.12951545F);
        float standFactor = factor(speed, 0.12951545F, 0F);

        // bipedTorso 그룹 pivotY +3F (head/body/arm 모두 이동)
        m.head.pivotY     = 3F;
        m.body.pivotY     = 3F;
        m.rightArm.pivotY = 5F;
        m.leftArm.pivotY  = 5F;
        m.rightLeg.pivotY = 12F;
        m.leftLeg.pivotY  = 12F;

        float bodyPitch = QUARTER - THIRTYTWOTH;
        m.body.pitch  = bodyPitch;
        m.body.roll   = (float) Math.cos(d + QUARTER) * SIXTYFOURTH * walkFactor;
        m.head.pitch  = 0F;  // flat 모델: head world pitch = 0

        // 팔: world pitch = bodyPitch + local(HALF+EIGHTH), local Z → roll
        m.rightArm.pitch = bodyPitch + HALF + EIGHTH;
        m.rightArm.yaw   = -QUARTER;
        m.rightArm.roll  = ((float) Math.cos(d + HALF) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor
                         + SIXTEENTH * standFactor;

        m.leftArm.pitch  = bodyPitch + HALF + EIGHTH;
        m.leftArm.yaw    = QUARTER;
        m.leftArm.roll   = ((float) Math.cos(d + HALF) * SIXTYFOURTH - THIRTYTWOTH) * walkFactor
                         - SIXTEENTH * standFactor;

        // 다리: world pitch = bodyPitch + local X, local Z → roll
        m.rightLeg.pitch = bodyPitch
                         + ((float) Math.cos(d - QUARTER) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor
                         + THIRTYTWOTH * standFactor;
        m.rightLeg.roll  = ((float) Math.cos(d - QUARTER) + 1F) * 0.25F * walkFactor
                         + THIRTYTWOTH * standFactor;

        m.leftLeg.pitch  = bodyPitch
                         + ((float) Math.cos(d - HALF - QUARTER) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor
                         + THIRTYTWOTH * standFactor;
        m.leftLeg.roll   = ((float) Math.cos(d - QUARTER) - 1F) * 0.25F * walkFactor
                         - THIRTYTWOTH * standFactor;
    }

    // ── 클라이밍 ──────────────────────────────────────────────────────────────
    // 원본: bipedOuter.X 없음. 팔/다리 값이 이미 world 공간.

    private static void applyClimbingAngles(BipedEntityModel<?> m, float dist, float speed) {
        float walkFactor = factor(speed, 0F, 0.3F);

        m.leftArm.pitch  = (float) Math.cos(dist + HALF) * 0.52F * walkFactor - QUARTER;
        m.rightArm.pitch = (float) Math.cos(dist) * 0.52F * walkFactor - QUARTER;

        m.leftLeg.pitch  = (float) Math.cos(dist) * 0.52F * walkFactor;
        m.rightLeg.pitch = (float) Math.cos(dist + HALF) * 0.52F * walkFactor;
    }

    // ── 천장 클라이밍 ─────────────────────────────────────────────────────────
    // 원본: bipedOuter.Y = rotateY, 팔 local Z = -rotateY (부모 yaw 상쇄)
    // flat 모델: 팔은 body 자식이 아니므로 yaw = 0F (world yaw = 0)

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
    // 원본: bipedOuter.rotationPointY = 5F (전체 그룹 이동)
    // 1.21.1 flat 모델: 모든 파트 pivotY +5F

    private static void applySlidingAngles(BipedEntityModel<?> m, float dist, float speed) {
        float walkFactor = factor(speed, 0F, 0.4F);
        float bodyPitch  = QUARTER;

        // bipedOuter 그룹 pivotY +5F (전체 파트 이동)
        m.head.pivotY     = 5F;
        m.body.pivotY     = 5F;
        m.rightArm.pivotY = 7F;
        m.leftArm.pivotY  = 7F;
        m.rightLeg.pivotY = 17F;
        m.leftLeg.pivotY  = 17F;

        m.body.pitch  = bodyPitch;
        m.head.pitch  = 0F;  // flat 모델: head world pitch = 0

        m.rightArm.pitch = bodyPitch + (float) Math.cos(dist + QUARTER) * SIXTYFOURTH * walkFactor
                         + HALF - SIXTYFOURTH;
        m.rightArm.yaw   = -QUARTER;

        m.leftArm.pitch  = bodyPitch + (float) Math.cos(dist - HALF) * SIXTYFOURTH * walkFactor
                         + HALF - SIXTYFOURTH;
        m.leftArm.yaw    = QUARTER;

        m.rightLeg.roll  =  THIRTYTWOTH;
        m.leftLeg.roll   = -THIRTYTWOTH;
    }

    // ── 수영 ──────────────────────────────────────────────────────────────────
    // 원본: bipedOuter.X = outerX (QUARTER - SIXTEENTH*combined)
    // 팔 local X → pitch = outerX + localX (SIXTEENTH*combined 상쇄 → QUARTER + cycling)
    // 팔 local Z (spread) → roll 직접 대입
    // 다리 local X → pitch = outerX + localX

    private static void applySwimmingAngles(BipedEntityModel<?> m, float dist, float speed, float time) {
        float walkFactor  = factor(speed, 0.15679921F, 0.52264464F);
        float sneakFactor = Math.min(factor(speed, 0F, 0.15679921F),
                                     factor(speed, 0.52264464F, 0.15679921F));
        float standFactor = factor(speed, 0.15679921F, 0F);
        float combined    = standFactor + sneakFactor;
        float outerX      = QUARTER - SIXTEENTH * combined;

        m.body.pitch = outerX;

        // 팔 spread (원본 arm.Z) → roll 직접
        float timeCos    = (float) Math.cos(time * 0.1F);
        m.rightArm.roll  =  QUARTER + EIGHTH + timeCos * combined * 0.8F;
        m.leftArm.roll   = -(QUARTER + EIGHTH) - timeCos * combined * 0.8F;

        // 팔 pitch: outerX + localX → SIXTEENTH*combined 상쇄 → QUARTER + cycling
        m.rightArm.pitch = QUARTER + (((dist * 0.5F) % WHOLE) - HALF) * walkFactor;
        m.leftArm.pitch  = QUARTER + (((dist * 0.5F + HALF) % WHOLE) - HALF) * walkFactor;

        // 다리: outerX + local X
        m.rightLeg.pitch = outerX + (float) Math.cos(dist) * 0.52264464F * walkFactor;
        m.leftLeg.pitch  = outerX + (float) Math.cos(dist + HALF) * 0.52264464F * walkFactor;
    }

    // ── 잠수 ──────────────────────────────────────────────────────────────────
    // 원본: bipedOuter.X = outerX (QUARTER - vAngle)
    // 팔/다리 모두 local Z 사용 → roll 직접 대입
    // 팔/다리 pitch = outerX (local X = 0이므로 outerX만)

    private static void applyDivingAngles(BipedEntityModel<?> m, float dist, float speed, float headPitch) {
        float vAngle      = (float) Math.toRadians(headPitch);
        float walkFactor  = factor(speed, 0.15679921F, 0.52264464F);
        float standFactor = factor(speed, 0.15679921F, 0F);
        float outerX      = QUARTER - vAngle;

        m.body.pitch = outerX;

        // 팔: pitch = outerX, 원본 arm.Z → roll 직접
        m.rightArm.pitch = outerX;
        m.rightArm.roll  = ((float) Math.cos(dist + HALF) * 0.52264464F * 2.5F + QUARTER) * walkFactor
                         + (QUARTER + EIGHTH) * standFactor;

        m.leftArm.pitch  = outerX;
        m.leftArm.roll   = ((float) Math.cos(dist) * 0.52264464F * 2.5F - QUARTER) * walkFactor
                         - (QUARTER + EIGHTH) * standFactor;

        // 다리: pitch = outerX, 원본 leg.Z → roll 직접
        m.rightLeg.pitch = outerX;
        m.rightLeg.roll  = ((float) Math.cos(dist) + 1F) * 0.52264464F * walkFactor
                         + SIXTEENTH * standFactor;

        m.leftLeg.pitch  = outerX;
        m.leftLeg.roll   = ((float) Math.cos(dist + HALF) - 1F) * 0.52264464F * walkFactor
                         - SIXTEENTH * standFactor;
    }

    // ── 헤드 점프 ────────────────────────────────────────────────────────────
    // 원본: bipedOuter.X = outerX (QUARTER - vAngle)
    // 팔 local X = -bendFactor*EIGHTH → pitch = outerX + localX
    // 팔 local Z (spread) → roll 직접 대입
    // head: world pitch = outerX + (-outerX/2) = outerX/2

    private static void applyHeadJumpingAngles(BipedEntityModel<?> m, float headPitch) {
        float vAngle     = (float) Math.toRadians(headPitch);
        float bendFactor = Math.min(factor(vAngle, QUARTER, 0F), factor(vAngle, -QUARTER, 0F));
        float armFactorZ = factor(vAngle, QUARTER, -QUARTER);
        float outerX     = QUARTER - vAngle;

        m.body.pitch = outerX;
        // world head pitch = outerX + (-outerX/2) = outerX/2
        m.head.pitch = outerX / 2F;

        // 팔: pitch = outerX + localX, 원본 arm.Z → roll 직접
        m.rightArm.pitch = outerX - bendFactor * EIGHTH;
        m.rightArm.roll  =  HALF - SIXTEENTH + armFactorZ * EIGHTH;

        m.leftArm.pitch  = outerX - bendFactor * EIGHTH;
        m.leftArm.roll   =  SIXTEENTH - HALF - armFactorZ * EIGHTH;
    }
}
