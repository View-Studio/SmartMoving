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
 * 1.21.1 flat 모델 + setupTransforms 엔티티 회전 방식:
 *
 * 크롤링/슬라이딩은 PlayerEntityRendererMixin.setupTransforms에서 엔티티 전체를 회전시킨다.
 * 이 때 setAngles의 각도는 1.7.10 원본 LOCAL 값을 그대로 사용할 수 있다.
 * (바닐라 수영이 -90° 엔티티 회전 후 팔/다리를 local 값으로 설정하는 것과 동일)
 *
 * 수영/잠수/헤드점프는 가변 outerX로 인해 setupTransforms 미사용.
 * 이들은 world-space 값 + pivotZ 보정으로 처리.
 *
 * pivotZ 공식 (outerX 회전, outer.pivotY=0):
 *   arm.pivotY = 2 * cos(outerX)
 *   arm.pivotZ = 2 * sin(outerX)
 *   leg.pivotY = 12 * cos(outerX)
 *   leg.pivotZ = 12 * sin(outerX)
 *
 * 바닐라 기본 pivotY: head=0, body=0, arm=2, leg=12
 * 바닐라 스니킹 pivotY: head=4.2, body=3.2, arm=5.2, leg=12.2, leg.pivotZ=4.0
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
        // pivotZ 잔류값 초기화 (수영/잠수/헤드점프에서 사용)
        model.rightArm.pivotZ = 0F; model.leftArm.pivotZ = 0F;
        model.rightLeg.pivotZ = 0F; model.leftLeg.pivotZ = 0F;

        float speed = limbDistance;

        if (state.isCrawling) {
            // setupTransforms에서 엔티티를 -(QUARTER-THIRTYTWOTH) 회전 → LOCAL 값 사용
            applyCrawlingAngles(model, limbAngle, speed, headPitch);
        } else if (state.isCeilingClimbing) {
            resetPivots(model);
            applyCeilingClimbingAngles(model, limbAngle, speed);
        } else if (state.isClimbing) {
            resetPivots(model);
            applyClimbingAngles(model, limbAngle, speed);
        } else if (state.isSliding) {
            // setupTransforms에서 엔티티를 -QUARTER 회전 → LOCAL 값 사용
            applySlidingAngles(model, limbAngle, speed, headPitch);
        } else if (state.isSwimming) {
            applySwimmingAngles(model, limbAngle, speed, animationProgress);
        } else if (state.isDiving) {
            applyDivingAngles(model, limbAngle, speed, headPitch);
        } else if (state.isHeadJumping) {
            resetPivots(model);
            applyHeadJumpingAngles(model, headPitch);
        }
    }

    // ── 기본 pivotY 복원 ─────────────────────────────────────────────────────
    private static void resetPivots(BipedEntityModel<?> m) {
        m.head.pivotY     = 0F;
        m.body.pivotY     = 0F;
        m.rightArm.pivotY = 2F;
        m.leftArm.pivotY  = 2F;
        m.rightLeg.pivotY = 12F;
        m.leftLeg.pivotY  = 12F;
    }

    // ── 기어가기 ─────────────────────────────────────────────────────────────
    // setupTransforms: 엔티티 Rx(-(Quarter-Thirtytwoth)) 적용됨
    // LOCAL 값 = 1.7.10 원본 bipedTorso/arm/leg LOCAL 값 그대로.
    // bipedTorso 그룹 오프셋: head.pivotY=3, body.pivotY=3, arm.pivotY=5
    // 다리: bipedOuter 자식이므로 pivotY=12 (로컬 프레임에서 뒤쪽으로 배치됨)
    private static void applyCrawlingAngles(BipedEntityModel<?> m, float dist, float speed, float headPitch) {
        float d           = dist * 1.3F;
        float walkFactor  = factor(speed, 0F, 0.12951545F);
        float standFactor = factor(speed, 0.12951545F, 0F);
        float bodyPitch   = QUARTER - THIRTYTWOTH;

        // bipedTorso 그룹 pivotY
        m.head.pivotY     = 3F;
        m.body.pivotY     = 3F;
        m.rightArm.pivotY = 5F;
        m.leftArm.pivotY  = 5F;
        // 다리: bipedOuter 자식 → 로컬 프레임에서 entity 회전 후 자연스럽게 뒤에 위치
        m.rightLeg.pivotY = 12F;
        m.leftLeg.pivotY  = 12F;

        // body: 엔티티가 이미 bodyPitch만큼 회전 → local pitch = 0
        m.body.pitch = 0F;
        m.body.roll  = (float) Math.cos(d + QUARTER) * SIXTYFOURTH * walkFactor;

        // head: 엔티티 회전 상쇄 + 실제 시선 피치 적용
        // world pitch = entity_rotation(-bodyPitch) + head.pitch_local
        // 원하는 world pitch = headPitch_radians → head.pitch_local = headPitch_radians + bodyPitch
        m.head.pitch += bodyPitch;  // vanilla이 이미 headPitch_radians로 설정했으므로 bodyPitch만 더함

        // 팔: LOCAL 값 (1.7.10 원본 bipedRightArm.rotateAngleX = Half+Eighth)
        m.rightArm.pitch = HALF + EIGHTH;
        m.rightArm.yaw   = -QUARTER;
        m.rightArm.roll  = ((float) Math.cos(d + HALF) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor
                         + SIXTEENTH * standFactor;

        m.leftArm.pitch  = HALF + EIGHTH;
        m.leftArm.yaw    = QUARTER;
        m.leftArm.roll   = ((float) Math.cos(d + HALF) * SIXTYFOURTH - THIRTYTWOTH) * walkFactor
                         - SIXTEENTH * standFactor;

        // 다리: LOCAL 값 (엔티티 회전 후 로컬 프레임에서 작은 각도)
        m.rightLeg.pitch = ((float) Math.cos(d - QUARTER) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor
                         + THIRTYTWOTH * standFactor;
        m.rightLeg.roll  = ((float) Math.cos(d - QUARTER) + 1F) * 0.25F * walkFactor
                         + THIRTYTWOTH * standFactor;

        m.leftLeg.pitch  = ((float) Math.cos(d - HALF - QUARTER) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor
                         + THIRTYTWOTH * standFactor;
        m.leftLeg.roll   = ((float) Math.cos(d - QUARTER) - 1F) * 0.25F * walkFactor
                         - THIRTYTWOTH * standFactor;
    }

    // ── 클라이밍 ─────────────────────────────────────────────────────────────
    private static void applyClimbingAngles(BipedEntityModel<?> m, float dist, float speed) {
        float walkFactor = factor(speed, 0F, 0.3F);

        m.body.pitch = 0F;

        m.leftArm.pitch  = (float) Math.cos(dist + HALF) * 0.52F * walkFactor - QUARTER;
        m.rightArm.pitch = (float) Math.cos(dist) * 0.52F * walkFactor - QUARTER;

        m.leftLeg.pitch  = (float) Math.cos(dist) * 0.52F * walkFactor;
        m.rightLeg.pitch = (float) Math.cos(dist + HALF) * 0.52F * walkFactor;
    }

    // ── 천장 클라이밍 ─────────────────────────────────────────────────────────
    private static void applyCeilingClimbingAngles(BipedEntityModel<?> m, float dist, float speed) {
        float walkFactor  = factor(speed, 0F, 0.12951545F);
        float standFactor = factor(speed, 0.12951545F, 0F);

        float rotateY = (float) Math.cos(dist) * 0.44F * walkFactor;
        m.body.pitch = 0F;
        m.body.yaw   = rotateY;

        m.leftArm.pitch  = ((float) Math.cos(dist) * 0.52F + HALF) * walkFactor + HALF * standFactor;
        m.leftArm.yaw    = 0F;
        m.rightArm.pitch = ((float) Math.cos(dist + HALF) * 0.52F - HALF) * walkFactor - HALF * standFactor;
        m.rightArm.yaw   = 0F;

        m.leftLeg.pitch  = -(float) Math.cos(dist) * 0.12F * walkFactor;
        m.rightLeg.pitch = -(float) Math.cos(dist + HALF) * 0.32F * walkFactor;
    }

    // ── 슬라이딩 ─────────────────────────────────────────────────────────────
    // setupTransforms: 엔티티 Rx(-QUARTER) 적용됨
    // LOCAL 값 = 1.7.10 원본 bipedOuter LOCAL 값 그대로.
    // bipedOuter 그룹 오프셋: head=5, body=5, arm=7, leg=17
    private static void applySlidingAngles(BipedEntityModel<?> m, float dist, float speed, float headPitch) {
        float walkFactor = factor(speed, 0F, 0.4F);

        // bipedOuter 그룹 pivotY
        m.head.pivotY     = 5F;
        m.body.pivotY     = 5F;
        m.rightArm.pivotY = 7F;
        m.leftArm.pivotY  = 7F;
        m.rightLeg.pivotY = 17F;
        m.leftLeg.pivotY  = 17F;

        // body: 엔티티가 이미 QUARTER만큼 회전 → local pitch = 0
        m.body.pitch = 0F;

        // head: 엔티티 회전 상쇄 + 실제 시선 피치
        m.head.pitch += QUARTER;

        // 팔: LOCAL 값 (1.7.10 원본 arm LOCAL X)
        m.rightArm.pitch = (float) Math.cos(dist + QUARTER) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        m.rightArm.yaw   = -QUARTER;

        m.leftArm.pitch  = (float) Math.cos(dist - HALF) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        m.leftArm.yaw    = QUARTER;

        // 다리: LOCAL 값 (작은 roll만)
        m.rightLeg.pitch = 0F;
        m.leftLeg.pitch  = 0F;
        m.rightLeg.roll  =  THIRTYTWOTH;
        m.leftLeg.roll   = -THIRTYTWOTH;
    }

    // ── 수영 ─────────────────────────────────────────────────────────────────
    // setupTransforms 미사용 (가변 outerX) → world-space + pivotZ 보정
    // pivotZ: arm.pivotZ = 2*sin(outerX), leg.pivotZ = 12*sin(outerX)
    private static void applySwimmingAngles(BipedEntityModel<?> m, float dist, float speed, float time) {
        float walkFactor  = factor(speed, 0.15679921F, 0.52264464F);
        float sneakFactor = Math.min(factor(speed, 0F, 0.15679921F),
                                     factor(speed, 0.52264464F, 0.15679921F));
        float standFactor = factor(speed, 0.15679921F, 0F);
        float combined    = standFactor + sneakFactor;
        float outerX      = QUARTER - SIXTEENTH * combined;

        float cosOuter = (float) Math.cos(outerX);
        float sinOuter = (float) Math.sin(outerX);

        // pivotY/Z 보정 (outer 회전 적용)
        m.head.pivotY     = 0F;
        m.body.pivotY     = 0F;
        m.rightArm.pivotY = 2F * cosOuter;
        m.leftArm.pivotY  = 2F * cosOuter;
        m.rightArm.pivotZ = 2F * sinOuter;
        m.leftArm.pivotZ  = 2F * sinOuter;
        m.rightLeg.pivotY = 12F * cosOuter;
        m.leftLeg.pivotY  = 12F * cosOuter;
        m.rightLeg.pivotZ = 12F * sinOuter;
        m.leftLeg.pivotZ  = 12F * sinOuter;

        m.body.pitch = outerX;

        float timeCos    = (float) Math.cos(time * 0.1F);
        m.rightArm.roll  =  QUARTER + EIGHTH + timeCos * combined * 0.8F;
        m.leftArm.roll   = -(QUARTER + EIGHTH) - timeCos * combined * 0.8F;

        m.rightArm.pitch = outerX + (((dist * 0.5F) % WHOLE) - HALF) * walkFactor;
        m.leftArm.pitch  = outerX + (((dist * 0.5F + HALF) % WHOLE) - HALF) * walkFactor;

        m.rightLeg.pitch = outerX + (float) Math.cos(dist) * 0.52264464F * walkFactor;
        m.leftLeg.pitch  = outerX + (float) Math.cos(dist + HALF) * 0.52264464F * walkFactor;
    }

    // ── 잠수 ─────────────────────────────────────────────────────────────────
    // setupTransforms 미사용 (가변 outerX) → world-space + pivotZ 보정
    private static void applyDivingAngles(BipedEntityModel<?> m, float dist, float speed, float headPitch) {
        float vAngle      = (float) Math.toRadians(headPitch);
        float walkFactor  = factor(speed, 0.15679921F, 0.52264464F);
        float standFactor = factor(speed, 0.15679921F, 0F);
        float outerX      = QUARTER - vAngle;

        float cosOuter = (float) Math.cos(outerX);
        float sinOuter = (float) Math.sin(outerX);

        // pivotY/Z 보정
        m.head.pivotY     = 0F;
        m.body.pivotY     = 0F;
        m.rightArm.pivotY = 2F * cosOuter;
        m.leftArm.pivotY  = 2F * cosOuter;
        m.rightArm.pivotZ = 2F * sinOuter;
        m.leftArm.pivotZ  = 2F * sinOuter;
        m.rightLeg.pivotY = 12F * cosOuter;
        m.leftLeg.pivotY  = 12F * cosOuter;
        m.rightLeg.pivotZ = 12F * sinOuter;
        m.leftLeg.pivotZ  = 12F * sinOuter;

        m.body.pitch = outerX;

        m.rightArm.pitch = outerX;
        m.rightArm.roll  = ((float) Math.cos(dist + HALF) * 0.52264464F * 2.5F + QUARTER) * walkFactor
                         + (QUARTER + EIGHTH) * standFactor;

        m.leftArm.pitch  = outerX;
        m.leftArm.roll   = ((float) Math.cos(dist) * 0.52264464F * 2.5F - QUARTER) * walkFactor
                         - (QUARTER + EIGHTH) * standFactor;

        m.rightLeg.pitch = outerX;
        m.rightLeg.roll  = ((float) Math.cos(dist) + 1F) * 0.52264464F * walkFactor
                         + SIXTEENTH * standFactor;

        m.leftLeg.pitch  = outerX;
        m.leftLeg.roll   = ((float) Math.cos(dist + HALF) - 1F) * 0.52264464F * walkFactor
                         - SIXTEENTH * standFactor;
    }

    // ── 헤드 점프 ─────────────────────────────────────────────────────────────
    // setupTransforms 미사용 → world-space + pivotZ 보정
    private static void applyHeadJumpingAngles(BipedEntityModel<?> m, float headPitch) {
        float vAngle     = (float) Math.toRadians(headPitch);
        float bendFactor = Math.min(factor(vAngle, QUARTER, 0F), factor(vAngle, -QUARTER, 0F));
        float armFactorZ = factor(vAngle, QUARTER, -QUARTER);
        float outerX     = QUARTER - vAngle;

        float cosOuter = (float) Math.cos(outerX);
        float sinOuter = (float) Math.sin(outerX);

        // pivotY/Z 보정
        m.head.pivotY     = 0F;
        m.body.pivotY     = 0F;
        m.rightArm.pivotY = 2F * cosOuter;
        m.leftArm.pivotY  = 2F * cosOuter;
        m.rightArm.pivotZ = 2F * sinOuter;
        m.leftArm.pivotZ  = 2F * sinOuter;
        m.rightLeg.pivotY = 12F * cosOuter;
        m.leftLeg.pivotY  = 12F * cosOuter;
        m.rightLeg.pivotZ = 12F * sinOuter;
        m.leftLeg.pivotZ  = 12F * sinOuter;

        m.body.pitch = outerX;
        m.head.pitch = outerX / 2F;

        m.rightArm.pitch = outerX - bendFactor * EIGHTH;
        m.rightArm.roll  =  HALF - SIXTEENTH + armFactorZ * EIGHTH;

        m.leftArm.pitch  = outerX - bendFactor * EIGHTH;
        m.leftArm.roll   =  SIXTEENTH - HALF - armFactorZ * EIGHTH;

        m.rightLeg.pitch = outerX;
        m.leftLeg.pitch  = outerX;
    }
}
