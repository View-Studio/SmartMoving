package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.render.AnimationUtil;
import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static choco.ratel.smartmoving.client.render.AnimationUtil.*;

@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin {

    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart body;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;

    // LivingEntity 브릿지 메서드를 타겟팅하여 리매핑 경고 방지
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

        float speed = limbDistance;

        if (state.isCrawling) {
            applyCrawlingAngles(limbAngle, speed);
        } else if (state.isCeilingClimbing) {
            applyCeilingClimbingAngles(limbAngle, speed);
        } else if (state.isClimbing) {
            applyClimbingAngles(limbAngle, speed);
        } else if (state.isSliding) {
            applySlidingAngles(limbAngle, speed);
        } else if (state.isSwimming) {
            applySwimmingAngles(limbAngle, speed);
        } else if (state.isDiving) {
            applyDivingAngles(limbAngle, speed);
        } else if (state.isHeadJumping) {
            applyHeadJumpingAngles(headPitch);
        }
    }

    // ── 기어가기 ──────────────────────────────────────────────────────────────

    private void applyCrawlingAngles(float dist, float speed) {
        float walkFactor  = factor(speed, 0F, 0.12951545F);
        float standFactor = factor(speed, 0.12951545F, 0F);

        // 몸통을 앞으로 숙임
        body.pitch    = QUARTER - THIRTYTWOTH;
        body.pivotY   = 3F;

        // 머리는 몸통 기울기에 반하여 앞을 바라봄
        head.pitch    = -(QUARTER - THIRTYTWOTH);

        // 왼팔: 앞으로 뻗음
        leftArm.pitch  = HALF + EIGHTH;
        leftArm.yaw    = -QUARTER;
        leftArm.roll   = (float)(Math.cos(dist + HALF)) * SIXTYFOURTH * walkFactor - THIRTYTWOTH;

        // 오른팔
        rightArm.pitch = HALF + EIGHTH;
        rightArm.yaw   = QUARTER;
        rightArm.roll  = (float)(Math.cos(dist + HALF)) * SIXTYFOURTH * walkFactor + THIRTYTWOTH;

        // 왼다리
        leftLeg.pitch  = (float)(Math.cos(dist - QUARTER)) * SIXTYFOURTH * walkFactor + THIRTYTWOTH * standFactor;
        leftLeg.roll   = ((float)(Math.cos(dist - QUARTER)) + 1F) * 0.25F * walkFactor + THIRTYTWOTH;

        // 오른다리
        rightLeg.pitch = (float)(Math.cos(dist + HALF - QUARTER)) * SIXTYFOURTH * walkFactor + THIRTYTWOTH * standFactor;
        rightLeg.roll  = -((float)(Math.cos(dist - QUARTER)) + 1F) * 0.25F * walkFactor - THIRTYTWOTH;
    }

    // ── 클라이밍 ──────────────────────────────────────────────────────────────

    private void applyClimbingAngles(float dist, float speed) {
        float walkFactor  = factor(speed, 0F, 0.3F);

        // 왼팔 진동 (사다리 오르기 동작)
        leftArm.pitch  = (float)(Math.cos(dist + HALF)) * 0.52F * walkFactor - QUARTER;
        leftArm.yaw    = 0F;

        // 오른팔은 위상 반전
        rightArm.pitch = (float)(Math.cos(dist)) * 0.52F * walkFactor - QUARTER;
        rightArm.yaw   = 0F;

        // 다리는 팔과 반대 위상
        leftLeg.pitch  = (float)(Math.cos(dist)) * 0.52F * walkFactor;
        rightLeg.pitch = (float)(Math.cos(dist + HALF)) * 0.52F * walkFactor;
    }

    // ── 천장 클라이밍 ─────────────────────────────────────────────────────────

    private void applyCeilingClimbingAngles(float dist, float speed) {
        float walkFactor  = factor(speed, 0F, 0.12951545F);
        float standFactor = factor(speed, 0.12951545F, 0F);

        // 팔을 위로 들어 천장 잡기
        leftArm.pitch  = ((float)(Math.cos(dist)) * 0.52F + HALF) * walkFactor + HALF * standFactor;
        rightArm.pitch = ((float)(Math.cos(dist + HALF)) * 0.52F - HALF) * walkFactor - HALF * standFactor;

        // 다리는 아래로 늘어짐
        leftLeg.pitch  = -(float)(Math.cos(dist)) * 0.12F * walkFactor;
        rightLeg.pitch = -(float)(Math.cos(dist + HALF)) * 0.32F * walkFactor;

        // 몸통 Y 회전 흔들림
        body.yaw = (float)(Math.cos(dist)) * 0.44F * walkFactor;
    }

    // ── 슬라이딩 ──────────────────────────────────────────────────────────────

    private void applySlidingAngles(float dist, float speed) {
        float walkFactor = factor(speed, 0F, 0.4F);

        // 몸통을 앞으로 수평 눕힘
        body.pitch    = QUARTER;
        body.pivotY   = 5F;

        // 머리
        head.pitch    = -QUARTER;

        // 팔: 옆으로 약간 들기
        leftArm.pitch  = (float)(Math.cos(dist + QUARTER)) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        leftArm.yaw    = -QUARTER;
        rightArm.pitch = (float)(Math.cos(dist + QUARTER)) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        rightArm.yaw   = QUARTER;

        // 다리: 약간 벌림
        leftLeg.roll   = THIRTYTWOTH;
        rightLeg.roll  = -THIRTYTWOTH;
    }

    // ── 수영 ──────────────────────────────────────────────────────────────────

    private void applySwimmingAngles(float dist, float speed) {
        float walkFactor  = factor(speed, 0.15679921F, 0.52264464F);
        float standFactor = factor(speed, 0.15679921F, 0F);

        // 몸통을 앞으로 눕힘
        body.pitch = -QUARTER * walkFactor;

        // 팔: 앞으로 뻗음
        leftArm.pitch  = -EIGHTH + (float)(Math.cos(dist + HALF)) * SIXTYFOURTH * walkFactor;
        rightArm.pitch = -EIGHTH + (float)(Math.cos(dist)) * SIXTYFOURTH * walkFactor;

        // 다리: 위아래로 파동
        leftLeg.pitch  = (float)(Math.cos(dist)) * 0.3F * walkFactor;
        rightLeg.pitch = (float)(Math.cos(dist + HALF)) * 0.3F * walkFactor;

        // 정지 시: 양팔을 옆으로 약간 들기
        if (standFactor > 0F) {
            leftArm.yaw  = -EIGHTH * standFactor;
            rightArm.yaw =  EIGHTH * standFactor;
        }
    }

    // ── 잠수 ──────────────────────────────────────────────────────────────────

    private void applyDivingAngles(float dist, float speed) {
        body.pitch    = -HALF;
        head.pitch    =  QUARTER;

        leftArm.pitch  = HALF;
        rightArm.pitch = HALF;

        leftLeg.pitch  = (float)(Math.cos(dist)) * 0.2F;
        rightLeg.pitch = (float)(Math.cos(dist + HALF)) * 0.2F;
    }

    // ── 헤드 점프 ────────────────────────────────────────────────────────────

    private void applyHeadJumpingAngles(float headPitch) {
        // 수직 각도 (카메라 pitch → 라디안 변환)
        float vAngle = (float) Math.toRadians(headPitch);
        float bendFactor = Math.min(factor(vAngle, QUARTER, 0F), factor(vAngle, -QUARTER, 0F));

        body.pitch  = vAngle;
        head.pitch  = -vAngle * 0.5F;

        // 팔: 머리 위 방향으로
        float armZ = HALF - SIXTEENTH + factor(vAngle, QUARTER, -QUARTER) * EIGHTH;
        leftArm.pitch  = -armZ;
        rightArm.pitch = -armZ;
        leftArm.roll   =  THIRTYTWOTH * bendFactor;
        rightArm.roll  = -THIRTYTWOTH * bendFactor;
    }
}
