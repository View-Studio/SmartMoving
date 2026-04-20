package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static choco.ratel.smartmoving.client.render.AnimationUtil.*;

/**
 * 1.21.1 flat 모델 애니메이션 적용 방식:
 *
 * [크롤링/슬라이딩]
 *   setupTransforms에서 엔티티 전체를 X축 회전. 모든 파트가 같은 좌표계에서 회전하므로 연결됨.
 *   setAngles에서는 1.7.10 원본 LOCAL 값을 그대로 사용 (엔티티가 이미 회전됐으므로).
 *   pivotY는 건드리지 않음 (vanilla 스니킹 pivotY를 resetPivots로 초기화).
 *
 * [수영/잠수/헤드점프]
 *   setupTransforms에서 별도 회전 없음. 대신 각 파트의 pivotY/Z를 수학적으로 계산해 설정.
 *   단, entity가 이미 vanilla SWIMMING EntityPose 상태라면 setupTransforms에서
 *   PlayerEntityRendererMixin이 vanilla 회전을 상쇄한 뒤 이 방식으로 처리.
 *   pivotZ 공식 (outer 회전 원점=(0,0,0), 파트 기본 Y 기준):
 *     arm:  pivotY = 2*cos(outerX), pivotZ = 2*sin(outerX)
 *     leg:  pivotY = 12*cos(outerX), pivotZ = 12*sin(outerX)
 *
 * [클라이밍/천장클라이밍]
 *   setupTransforms 없음, pivotZ 없음.
 *   setAngles에서 world-space 각도 직접 설정.
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

        // 이전 프레임 잔류값 초기화 — vanilla가 관리하지 않는 축만
        model.body.yaw    = 0F;
        model.body.roll   = 0F;
        model.rightArm.yaw  = 0F;  model.leftArm.yaw  = 0F;
        model.rightArm.roll = 0F;  model.leftArm.roll = 0F;
        model.rightLeg.yaw  = 0F;  model.leftLeg.yaw  = 0F;
        model.rightLeg.roll = 0F;  model.leftLeg.roll = 0F;
        // arm pivotZ 잔류값 초기화 (leg.pivotZ는 vanilla 스니킹이 4.0F로 설정하므로 건드리지 않음)
        model.rightArm.pivotZ = 0F; model.leftArm.pivotZ = 0F;

        float speed = limbDistance;

        if (state.isCrawling) {
            resetPivots(model);
            applyCrawlingAngles(model, limbAngle, speed, headPitch);
        } else if (state.isCeilingClimbing) {
            resetPivots(model);
            applyCeilingClimbingAngles(model, limbAngle, speed);
        } else if (state.isClimbing) {
            resetPivots(model);
            applyClimbingAngles(model, limbAngle, speed);
        } else if (state.isSliding) {
            resetPivots(model);
            applySlidingAngles(model, limbAngle, speed, headPitch);
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

    // ── 기본 pivotY 복원 (vanilla 스니킹 pivotY 간섭 방지) ──────────────────
    private static void resetPivots(BipedEntityModel<?> m) {
        m.head.pivotY     = 0F;
        m.body.pivotY     = 0F;
        m.rightArm.pivotY = 2F;
        m.leftArm.pivotY  = 2F;
        m.rightLeg.pivotY = 12F;
        m.leftLeg.pivotY  = 12F;
        m.rightLeg.pivotZ = 0F;
        m.leftLeg.pivotZ  = 0F;
    }

    // ── 기어가기 ─────────────────────────────────────────────────────────────
    // setupTransforms: 엔티티 Rx(-(QUARTER-THIRTYTWOTH)) 적용됨.
    // pivotY: resetPivots로 초기화 후 변경 없음 (기본 pivotY 유지).
    // 각도: 1.7.10 원본 LOCAL 값 그대로 (엔티티가 이미 79° 회전됐으므로).
    private static void applyCrawlingAngles(BipedEntityModel<?> m, float dist, float speed, float headPitch) {
        float d           = dist * 1.3F;
        float walkFactor  = factor(speed, 0F, 0.12951545F);
        float standFactor = factor(speed, 0.12951545F, 0F);
        float bodyPitch   = QUARTER - THIRTYTWOTH;

        // body: 엔티티가 이미 bodyPitch만큼 회전 → local pitch = 0
        m.body.pitch = 0F;
        m.body.roll  = (float) Math.cos(d + QUARTER) * SIXTYFOURTH * walkFactor;

        // head: vanilla이 headPitch_radians로 설정 → 엔티티 회전 상쇄분 추가
        m.head.pitch += bodyPitch;

        // 팔: 1.7.10 bipedTorso LOCAL 값 (엔티티 회전 기준 local)
        m.rightArm.pitch = HALF + EIGHTH;
        m.rightArm.yaw   = -QUARTER;
        m.rightArm.roll  = ((float) Math.cos(d + HALF) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor
                         + SIXTEENTH * standFactor;

        m.leftArm.pitch  = HALF + EIGHTH;
        m.leftArm.yaw    = QUARTER;
        m.leftArm.roll   = ((float) Math.cos(d + HALF) * SIXTYFOURTH - THIRTYTWOTH) * walkFactor
                         - SIXTEENTH * standFactor;

        // 다리: 1.7.10 bipedOuter LOCAL 값 (bipedOuter = 엔티티 회전 기준)
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
    // setupTransforms: 엔티티 Rx(-QUARTER) 적용됨.
    // pivotY: resetPivots로 초기화 후 변경 없음 (기본 pivotY 유지).
    // 각도: 1.7.10 원본 LOCAL 값 그대로.
    private static void applySlidingAngles(BipedEntityModel<?> m, float dist, float speed, float headPitch) {
        float walkFactor = factor(speed, 0F, 0.4F);

        // body: 엔티티가 이미 QUARTER만큼 회전 → local pitch = 0
        m.body.pitch = 0F;

        // head: vanilla headPitch + 엔티티 회전 상쇄분
        m.head.pitch += QUARTER;

        // 팔: 1.7.10 bipedOuter LOCAL 값
        m.rightArm.pitch = (float) Math.cos(dist + QUARTER) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        m.rightArm.yaw   = -QUARTER;

        m.leftArm.pitch  = (float) Math.cos(dist - HALF) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        m.leftArm.yaw    = QUARTER;

        // 다리
        m.rightLeg.pitch = 0F;
        m.leftLeg.pitch  = 0F;
        m.rightLeg.roll  =  THIRTYTWOTH;
        m.leftLeg.roll   = -THIRTYTWOTH;
    }

    // ── 수영 ─────────────────────────────────────────────────────────────────
    // setupTransforms 없음 (PlayerEntityRendererMixin이 vanilla SWIMMING 회전을 상쇄함).
    // pivotY/Z: outerX 회전 후 각 파트의 world 위치로 보정.
    //   arm: (0, 2, 0) → (0, 2*cos, 2*sin)
    //   leg: (0, 12, 0) → (0, 12*cos, 12*sin)
    private static void applySwimmingAngles(BipedEntityModel<?> m, float dist, float speed, float time) {
        float walkFactor  = factor(speed, 0.15679921F, 0.52264464F);
        float sneakFactor = Math.min(factor(speed, 0F, 0.15679921F),
                                     factor(speed, 0.52264464F, 0.15679921F));
        float standFactor = factor(speed, 0.15679921F, 0F);
        float combined    = standFactor + sneakFactor;
        float outerX      = QUARTER - SIXTEENTH * combined;

        float cosOuter = (float) Math.cos(outerX);
        float sinOuter = (float) Math.sin(outerX);

        // pivotY/Z 보정
        m.rightArm.pivotY = 2F * cosOuter;
        m.leftArm.pivotY  = 2F * cosOuter;
        m.rightArm.pivotZ = 2F * sinOuter;
        m.leftArm.pivotZ  = 2F * sinOuter;
        m.rightLeg.pivotY = 12F * cosOuter;
        m.leftLeg.pivotY  = 12F * cosOuter;
        m.rightLeg.pivotZ = 12F * sinOuter;
        m.leftLeg.pivotZ  = 12F * sinOuter;

        m.body.pitch = outerX;

        // 머리: outerX + 원본 LOCAL (-Eighth * combined)
        m.head.pitch = outerX - EIGHTH * combined;

        float timeCos    = (float) Math.cos(time * 0.1F);
        m.rightArm.roll  =  QUARTER + EIGHTH + timeCos * combined * 0.8F;
        m.leftArm.roll   = -(QUARTER + EIGHTH) - timeCos * combined * 0.8F;

        // arm pitch: world = outerX + LOCAL
        // LOCAL: ((dist*0.5)%WHOLE - HALF) * walkFactor + SIXTEENTH*combined
        m.rightArm.pitch = outerX + (((dist * 0.5F) % WHOLE) - HALF) * walkFactor + SIXTEENTH * combined;
        m.leftArm.pitch  = outerX + (((dist * 0.5F + HALF) % WHOLE) - HALF) * walkFactor + SIXTEENTH * combined;

        m.rightLeg.pitch = outerX + (float) Math.cos(dist) * 0.52264464F * walkFactor;
        m.leftLeg.pitch  = outerX + (float) Math.cos(dist + HALF) * 0.52264464F * walkFactor;
    }

    // ── 잠수 ─────────────────────────────────────────────────────────────────
    private static void applyDivingAngles(BipedEntityModel<?> m, float dist, float speed, float headPitch) {
        float vAngle      = (float) Math.toRadians(headPitch);
        float walkFactor  = factor(speed, 0.15679921F, 0.52264464F);
        float standFactor = factor(speed, 0.15679921F, 0F);
        float outerX      = QUARTER - vAngle;

        float cosOuter = (float) Math.cos(outerX);
        float sinOuter = (float) Math.sin(outerX);

        // pivotY/Z 보정
        m.rightArm.pivotY = 2F * cosOuter;
        m.leftArm.pivotY  = 2F * cosOuter;
        m.rightArm.pivotZ = 2F * sinOuter;
        m.leftArm.pivotZ  = 2F * sinOuter;
        m.rightLeg.pivotY = 12F * cosOuter;
        m.leftLeg.pivotY  = 12F * cosOuter;
        m.rightLeg.pivotZ = 12F * sinOuter;
        m.leftLeg.pivotZ  = 12F * sinOuter;

        m.body.pitch = outerX;
        m.head.pitch = outerX;  // 잠수 시 머리도 몸통과 같은 방향

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
    private static void applyHeadJumpingAngles(BipedEntityModel<?> m, float headPitch) {
        float vAngle     = (float) Math.toRadians(headPitch);
        float bendFactor = Math.min(factor(vAngle, QUARTER, 0F), factor(vAngle, -QUARTER, 0F));
        float armFactorZ = factor(vAngle, QUARTER, -QUARTER);
        float outerX     = QUARTER - vAngle;

        float cosOuter = (float) Math.cos(outerX);
        float sinOuter = (float) Math.sin(outerX);

        // pivotY/Z 보정
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
