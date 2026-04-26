package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 9-6: getPositionOffset() — 크롤링/헤드점프 렌더 Y 오프셋.
 * 12-3: setupTransforms() — SM 이동 상태별 body X 기울기 + bodyYaw stub.
 * 12-6: getPositionOffset() — heightOffset 렌더 적용.
 * renderName(): 타인 플레이어 이름 태그 — 크롤 숨김 + heightOffset Y 보정.
 */
@Mixin(PlayerEntityRenderer.class)
@Environment(EnvType.CLIENT)
public class MixinPlayerEntityRenderer {

    @Unique private static boolean smBodyYawActive;
    @Unique private static float smBodyYawOverride;
    /** BUG-27/32 (세션 47): 비행 시 추가 Y 회전 (horizontalAngle - lerpedYaw, 라디안). 0 = 추가 회전 없음. */
    @Unique private static float smFlyingExtraYaw;
    /**
     * 🔴 (세션 52): partial tick 캐시 — setupTransforms 에서 저장 → setAngles 에서 사용.
     * 원본 SmartRenderRender.renderPlayer L56-L57: getCurrentSpeed/getTotalDistance(renderPartialTicks)
     * 매 프레임 lerped 값 → modelPlayer.currentSpeed/totalDistance 저장. 우리 매핑은 setAngles 인자에
     * tickDelta 없어 캐시 통해 전달.
     */
    @Unique public static float smCachedTickDelta = 0f;

    /**
     * [9-6][12-6] getPositionOffset() 오버라이드.
     *
     * SM 크롤링 (자기 자신): SWIMMING 포즈 오프셋(setupTransforms translate(0,-1,0.3) 제거됨)
     *             대신 vanilla 웅크리기 오프셋(-scale × 0.125) 반환.
     * SM 헤드점프 (자기 자신): heightOffset(-1F) 렌더 적용.
     * SM 타인 플레이어 sneak+crawl (B-14 / §16-22): 렌더 Y 위치 +0.125 보정 (지면 뚫림 방지).
     *
     * 원본: SmartMovingRender.renderPlayerAt() → d1 += heightOffset
     *       SmartMovingPlayerBase.getYOffset() → -0.125F
     *       SmartMovingRender L124-L125: 타인 플레이어 sneak+crawl → d1 += 0.125
     */
    @Inject(method = "getPositionOffset(Lnet/minecraft/client/network/AbstractClientPlayerEntity;F)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"), cancellable = true)
    private void sm_getPositionOffset(AbstractClientPlayerEntity entity, float tickDelta,
                                       CallbackInfoReturnable<Vec3d> cir) {
        // 자기 자신 (ClientPlayerEntity): 헤드점프 + 크롤링 분기
        if (entity instanceof ClientPlayerEntity player) {
            SmartMovingClientState sm = SmartMovingClientState.get(player);

            // 헤드점프: heightOffset Y 오프셋 적용 (우선순위 높음)
            if (sm.isHeadJumping && sm.heightOffset != 0f) {
                cir.setReturnValue(new Vec3d(0D, sm.heightOffset, 0D));
                return;
            }

            // 크롤링: SWIMMING 포즈 오프셋 대신 SM 크롤링 오프셋
            if (sm.isCrawling) {
                cir.setReturnValue(new Vec3d(0D, -entity.getScale() * 0.125D, 0D));
            }
            return;
        }

        // 타인 플레이어 (B-14 / §16-22 / 원본 SmartMovingRender L124-L125):
        // !isOwnPlayer && entity.isSneaking() && isCrawl → d1 += 0.125 (지면 뚫림 방지)
        // SM 상태는 C-24 State 패킷으로 동기화 (UUID 기반 조회).
        SmartMovingClientState sm = SmartMovingClientState.get(entity.getUuid());
        if (entity.isSneaking() && sm.isCrawling) {
            cir.setReturnValue(new Vec3d(0D, 0.125D, 0D));
        }
    }

    /**
     * [12-3] setupTransforms() HEAD 주입 — SM bodyYaw 오버라이드 계산.
     *
     * isClimbing/isCrawlClimbing/isCeilingClimbing/isSwimming_sm/isDiving/isSliding/
     * isHeadJumping/isCrawling 상태에서 forwardRotation(이동 방향 각도)으로 bodyYaw를 강제한다.
     * 계산 결과는 smBodyYawActive/smBodyYawOverride에 저장하여 @ModifyArg가 읽는다.
     *
     * 원본: SmartMovingRender.rotatePlayer() → forwardRotation = Math.atan2(-vel.x, vel.z)
     */
    @Inject(method = "setupTransforms", at = @At("HEAD"))
    private void sm_captureBodyYaw(AbstractClientPlayerEntity player, MatrixStack matrices,
                                    float animationProgress, float bodyYaw, float tickDelta, float scale,
                                    CallbackInfo ci) {
        smBodyYawActive = false;
        smFlyingExtraYaw = 0f;
        // 🔴 (세션 52): partial tick 캐시 저장. setAngles inject (sm_animateFlying 등) 에서
        //   getCurrentSpeed/getTotalDistance lerped getter 호출용.
        smCachedTickDelta = tickDelta;
        if (!(player instanceof ClientPlayerEntity localPlayer)) return;
        // BUG-12 (세션 36): SM disabled 시 bodyYaw 오버라이드 안 함 → vanilla bodyYaw 그대로 (BUG-7 확장).
        if (!SmartMovingConfig.Config.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(localPlayer);

        // 원본 SmartMovingRender.rotatePlayer L271-274 조건 1:1.
        // 원본에는 isRopeSliding/isCrawling 단독 없음, 대신 isClimbCrawling 포함.
        boolean smActive = sm.isClimbing || sm.isClimbCrawling || sm.isCrawlClimbing
                || sm.isFlying || sm.isSwimming_sm || sm.isDiving
                || sm.isCeilingClimbing || sm.isHeadJumping
                || sm.isSliding || sm.isAngleJumping();
        if (!smActive) return;

        // isLevitating 우선 처리 (B-15 / §16-23): 원본 SmartMovingRender L132-L134 —
        //   levitating 시 모든 모델의 currentHorizontalAngle = currentCameraAngle 강제 정렬.
        // 원본은 분기 처리 후 마지막에 덮어쓰기지만 결과 동등 (모든 다른 분기 결과를 카메라
        //   방향으로 덮어쓰는 효과 = 분기 진입 전 카메라 방향 단독 적용과 동일).
        // 일반적으로 levitate 는 dive 자세와 함께 발생 (Levitation status effect).
        if (sm.isLevitating) {
            smBodyYawActive = true;
            smBodyYawOverride = (float) Math.toDegrees(sm.stats.currentCameraAngle);
            return;
        }

        // ── 원본 SmartMovingModel 상태별 bipedOuter.rotateAngleY 1:1 이식 ──────
        // 1.21.1 은 bipedOuter 계층이 없어 bodyYaw 단일 경로로 근사. 라디안→도 변환.
        // 원본 if-else 체인 우선순위 순서:
        //   1. isRopeSliding (L279) — currentHorizontalAngle
        //   2. isClimb/isClimbCrawling (L308) — forwardRotation / RadiantToAngle
        //   4. isCeilingClimb (L463+L476) — rotateY + horizontalAngle (threshold 0.015)
        //   5. isSwim (L495/L511) — horizontalAngle (threshold 0.005/0.015 isGenericSneaking)
        //   6. isDive (L553/L564) — horizontalAngle (totalDistance 기준)
        //   7. isCrawl (L668) — currentHorizontalAngle
        //   9. isFlying (L704/L713) — horizontalAngle (threshold 0.05)
        //  10. isHeadJumping (L740) — currentHorizontalAngle

        // isCeilingClimb (원본 L463 + L475-L476):
        //   distance = totalHorizontalDistance * 0.7F
        //   walkFactor = Factor(currentHorizontalSpeed, 0, 0.12951545F)  // 0→1 보간
        //   horizontalAngle = horizontalDistance < 0.015F ? currentCameraAngle : currentHorizontalAngle
        //   rotateY = cos(distance) * 0.44F * walkFactor
        //   bipedOuter.rotateAngleY = rotateY + horizontalAngle
        if (sm.isCeilingClimbing) {
            float distance = sm.stats.totalHorizontalDistance * 0.7F;
            float x = sm.stats.currentHorizontalSpeed;
            float walkFactor = (x >= 0.12951545F) ? 1F : (x <= 0F ? 0F : x / 0.12951545F);
            float horizontalAngle = sm.stats.horizontalDistance < 0.015F
                    ? sm.stats.currentCameraAngle
                    : sm.stats.currentHorizontalAngle;
            float rotateY = (float) Math.cos(distance) * 0.44F * walkFactor;
            smBodyYawActive = true;
            smBodyYawOverride = (float) Math.toDegrees(rotateY + horizontalAngle);
            return;
        }

        // isSwim/isDive (원본 L495/L553)
        if (sm.isSwimming_sm || sm.isDiving) {
            float threshold = sm.isSlow ? 0.005F : 0.015F;
            double dist = sm.isDiving ? sm.stats.totalDistance : sm.stats.horizontalDistance;
            float horizontalAngle = dist < threshold
                    ? sm.stats.currentCameraAngle
                    : sm.stats.currentHorizontalAngle;
            smBodyYawActive = true;
            smBodyYawOverride = (float) Math.toDegrees(horizontalAngle);
            return;
        }

        // 🔴 BUG-27/32 진짜 원인 정정 (Flying Phase / 세션 47): 원본 1:1 정확 매핑.
        //   원본 SmartMovingRender.rotatePlayer L145-L148:
        //     forwardRotation = prevRotationYaw + (rotationYaw - prevRotationYaw) * f2 (= lerpedYaw)
        //     if (isFlying || ...) entityplayer.renderYawOffset = forwardRotation
        //   = **player.bodyYaw 를 lerpedYaw (마우스 yaw) 로 강제** — 이동 방향 (horizontalAngle) 아님!
        //   이로 인해 vanilla setAngles 의 head.yaw = headYaw - bodyYaw = headYaw - lerpedYaw ≈ 0
        //   = 머리가 몸과 정렬 = 마우스 좌우 시 몸+머리 같이 회전 = 슈퍼맨 자세 자연스러움.
        //
        //   원본 SmartMovingModel.isFlying L486:
        //     bipedOuter.rotateAngleY = horizontalAngle (이동 방향)
        //   = 모델 전체 추가 Y 회전 — 모델 root pivot (= head pivot) 기준.
        //
        //   이전 1.21.1 잘못 매핑: bodyYaw 인자만 ModifyArg 로 horizontalAngle 변경.
        //     1) entity.bodyYaw field 변경 안 함 → vanilla setAngles 의 head.yaw 가 자연 값
        //        (마우스 좌우 시 head.yaw 따로 변함) → 사용자 보고 BUG-32 "머리 이상 고정/이상".
        //     2) bipedOuter.Y (= horizontalAngle 추가 회전) 효과 부재 → 모델이 이동 방향으로
        //        안 따라감 → 사용자 보고 "전진 시 몸 중심점 다름" + BUG-27 효과.
        //   정정:
        //     - smBodyYawOverride = lerpedYaw (마우스 yaw, 도)
        //     - localPlayer.bodyYaw = localPlayer.prevBodyYaw = lerpedYaw 직접 강제 (vanilla
        //       setAngles 의 head.yaw 계산 영향)
        //     - 추가 Y 회전 (horizontalAngle - lerpedYaw) 은 sm_setupTransforms TAIL 에서 처리
        //       (smFlyingExtraYaw 캐시 통해 전달).
        if (sm.isFlying) {
            float lerpedYawDeg = localPlayer.prevYaw
                    + (localPlayer.getYaw() - localPlayer.prevYaw) * tickDelta;
            smBodyYawActive = true;
            smBodyYawOverride = lerpedYawDeg;
            // entity.bodyYaw field 강제 (vanilla setAngles 가 lerp 후 사용)
            localPlayer.bodyYaw = lerpedYawDeg;
            localPlayer.prevBodyYaw = lerpedYawDeg;
            // bipedOuter.rotateAngleY 효과: horizontalAngle (이동 방향) 추가 Y 회전.
            //   sm_setupTransforms TAIL 의 X 회전 직전에 적용 (회전 중심 = 머리 위치).
            float horizontalAngle = sm.stats.horizontalDistance < 0.05F
                    ? sm.stats.currentCameraAngle
                    : sm.stats.currentHorizontalAngle;
            smFlyingExtraYaw = horizontalAngle - (float) Math.toRadians(lerpedYawDeg);
            return;
        }

        // isHeadJumping/isCrawling/isRopeSliding (원본 L740/L668/L279) — threshold 없이 currentHorizontalAngle
        if (sm.isHeadJumping || sm.isCrawling || sm.isRopeSliding) {
            smBodyYawActive = true;
            smBodyYawOverride = (float) Math.toDegrees(sm.stats.currentHorizontalAngle);
            return;
        }

        // 나머지(isClimb/isClimbCrawling/isCeilingClimb/isSliding/isAngleJumping) — 원본 rotatePlayer L269:
        //   forwardRotation = prevRotationYaw + (rotationYaw - prevRotationYaw) * f2 (player yaw 보간, 도)
        // isClimb/ClimbCrawling 은 원본 L308 forwardRotation/RadiantToAngle(라디안) 과 동등.
        // isCeilingClimb 의 `rotateY + horizontalAngle`(L476) 은 rotateY 공식 이식 필요 — 후속.
        smBodyYawActive = true;
        smBodyYawOverride = localPlayer.prevYaw + (localPlayer.getYaw() - localPlayer.prevYaw) * tickDelta;
    }

    /**
     * [12-3] setupTransforms() @ModifyArg — super.setupTransforms() 호출 시 bodyYaw(index=3) 교체.
     *
     * sm_captureBodyYaw에서 smBodyYawActive=true인 경우에만 forwardRotation으로 대체한다.
     * B-17 확인: index=3 = bodyYaw (entity=0, matrices=1, animationProgress=2, bodyYaw=3, tickDelta=4, scale=5)
     */
    @ModifyArg(
        method = "setupTransforms",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;setupTransforms(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V"),
        index = 3
    )
    private float sm_modifyBodyYaw(float bodyYaw) {
        return smBodyYawActive ? smBodyYawOverride : bodyYaw;
    }

    /**
     * [12-3] setupTransforms() TAIL 주입 — SM 이동 상태별 body X 기울기.
     *
     * vanilla setupTransforms()의 super.setupTransforms()가 이미 Y회전(bodyYaw)을
     * 적용했으므로, 여기에 추가로 SM 고유 X 기울기를 적용한다.
     *
     * 원본: SmartMovingRender.rotatePlayer() → bipedOuter.rotateAngleY 설정
     *       SmartMovingModel.setRotationAngles() → bipedOuter.rotateAngleX 설정
     */
    @Inject(method = "setupTransforms", at = @At("TAIL"))
    private void sm_setupTransforms(AbstractClientPlayerEntity player, MatrixStack matrices,
                                     float animationProgress, float bodyYaw, float tickDelta, float scale,
                                     CallbackInfo ci) {
        if (!(player instanceof ClientPlayerEntity localPlayer)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(localPlayer);

        // BUG-12 (세션 36): SM disabled 시 모든 SM 분기 skip + smOuterTiltX = 0 reset →
        //   매 호출마다 0 으로 정리하므로 cape 클램프도 vanilla fallback (BUG-7 확장).
        sm.smOuterTiltX = 0f;
        if (!SmartMovingConfig.Config.enabled) return;

        // B-17 capture (§16-25): outer.X 등가 tiltAngle 을 SmartMovingClientState.smOuterTiltX 에
        //   저장해 MixinCapeFeatureRenderer 에서 망토 X 회전 클램프 (70.523° - outerX_deg) 적용.
        //   진입 시 reset — 5 분기 중 한 분기만 활성 시 = 으로 할당, 비활성 시 0 (vanilla 망토 그대로).

        // SM 수영(isSwimming_sm): bipedOuter.rotateAngleX = Quarter - Sixteenth * standSneakFactor
        // 원본: fadeRotateAngleX = true + rotateAngleX = Quarter - Sixteenth * standSneakFactor
        // standSneakFactor: 정지/스니킹=1 → 67.5°, 보행=0 → 90°(완전 수평)
        if (sm.isSwimming_sm) {
            float tiltAngle = (float) Math.PI / 2f - (float) Math.PI / 8f * sm.swimStandSneakFactor;
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(tiltAngle));
            sm.smOuterTiltX = tiltAngle;
        }

        // SM 잠수(isDiving): 원본 SmartMovingModel.md L542 —
        //   bipedOuter.rotateAngleX = isLevitate ? Quarter - Sixteenth
        //                           : (isJump ? 0F : Quarter - currentVerticalAngle)
        if (sm.isDiving) {
            float tiltAngle;
            if (sm.isLevitating) {
                // Quarter - Sixteenth ≈ 7π/16 (살짝 덜 수평)
                tiltAngle = (float) Math.PI / 2f - (float) Math.PI / 16f;
            } else if (sm.isJumping) {
                // 점프 중 잠수: 수직각 0 (정자세)
                tiltAngle = 0f;
            } else {
                // 일반 잠수: Quarter - currentVerticalAngle (수직각 반영)
                tiltAngle = (float) Math.PI / 2f - sm.stats.currentVerticalAngle;
            }
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(tiltAngle));
            sm.smOuterTiltX = tiltAngle;
        }

        // SM 슬라이딩(isSliding): bipedOuter.rotateAngleX = Quarter
        if (sm.isSliding) {
            float tiltAngle = (float) Math.PI / 2f; // Quarter
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(tiltAngle));
            sm.smOuterTiltX = tiltAngle;
            // bipedOuter.rotationPointY = 5F
            matrices.translate(0f, 5f / 16f, 0f);
            // bipedBody.offsetY = -0.4F (원본 SmartMovingModel.java L452, B-13 / §16-19)
            // ModelPart 에 offsetY 필드 부재 → MatrixStack translate 보정.
            // 단위: 픽셀 → 블록 (/16). slide 분기 안에서만 적용 (push/pop 자동 관리).
            matrices.translate(0f, -0.4f / 16f, 0f);
        }

        // isFlying body X 기울기: θ = (Quarter - verticalAngle) * walkFactor (C-42, A-30 SmartStatistics)
        // walkFactor = Factor(currentSpeed, 0F, 1) — 속도 0~1 범위 정규화 (SmartMovingModel.md 9번 분기)
        // 🔴 BUG-31 (Flying Phase F-6 / 세션 40): X 회전 부호 반전.
        //   원인: vanilla LivingEntityRenderer.setupTransforms L44-L59 = POSITIVE_Y.rotation(180-bodyYaw)
        //   기본 적용 (entity Y 180° 뒤집음). SM 의 sm_setupTransforms TAIL inject 로 추가 X 회전
        //   적용 시 좌표계 뒤집힘 영향 받음 — Y 180° 후 POSITIVE_X 회전 = 모델 좌표계의
        //   NEGATIVE_X 회전 등가 → 회전 방향 반대 (사용자 보고 "몸 앞쪽이 하늘").
        //   해결: -theta 적용 (POSITIVE_X 부호 반전) → 슈퍼맨 자세 (배 아래, 등 위).
        //   smOuterTiltX 는 그대로 (cape 클램프 — B-17 — 별도 의미 보존).
        if (sm.isFlying) {
            // 🔴 (세션 48): partial tick lerp 적용 — 60Hz 부드러움 (원본 매 프레임 보간 1:1).
            float walkFactor = sm.stats.getCurrentSpeed(tickDelta);
            // 🔴 1:1 정정 (세션 47b): 원본 SmartMovingModel L481 `verticalAngle =
            //   isJump ? Math.abs(currentVerticalAngle) : currentVerticalAngle`. 이전 매핑 누락.
            float verticalAngle = sm.isJumping
                    ? Math.abs(sm.stats.currentVerticalAngle)
                    : sm.stats.currentVerticalAngle;
            float thetaTarget = ((float) Math.PI / 2f - verticalAngle) * walkFactor;
            float yawTarget = smFlyingExtraYaw;

            // 🔴 fade 보간 적용 (Flying Phase / 세션 49): 원본 ModelRotationRenderer.fadeIntermediate
            //   매 프레임 호출 1:1 매핑.
            //   원본 GetIntermediateAngle (L347-L364):
            //     return prev + (target - prev) * (currentTime - prevTime) * 0.2F
            //   가드 (L317): currentTime - prevTime <= 2F 시만 보간 (그 이상은 즉시).
            //   원본 비행 분기 (L484): bipedOuter.fadeRotateAngleX = true → X 회전 보간.
            //   원본 rotatePlayer (L209): bipedOuter.fadeRotateAngleY = true (default) → Y 회전 보간.
            //   사용자 보고 "동작 사이 중간 처리 부재" / "프레임 드랍 느낌" 직접 원인 = fade 부재.
            //   정정: prev 저장 + 매 프레임 lerp factor 0.2 * deltaTime 적용.
            //     큰 각도 차이 (π 등) 의 wrapping 처리 (원본 L352-L362) 도 적용.
            float thetaLerped = lerpFadeAngle(sm.smOuterTiltX_prev, thetaTarget,
                                              sm.smOuterFade_prevTime, animationProgress);
            float yawLerped = lerpFadeAngle(sm.smOuterExtraYaw_prev, yawTarget,
                                            sm.smOuterFade_prevTime, animationProgress);

            matrices.translate(0f, 1.5f, 0f);
            // 🔴 BUG-27/32 좌우 바뀜 정정 (Flying Phase / 세션 47b): Y 회전 부호 반전.
            //   vanilla LivingEntityRenderer.render() L342: setupTransforms 후 scale(-1,-1,1) 적용
            //   → Y axis 반전 → POSITIVE_Y rotation mirror → 부호 반전 보정.
            if (yawLerped != 0f) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-yawLerped));
            }
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(-thetaLerped));
            matrices.translate(0f, -1.5f, 0f);

            // fade prev 갱신 (다음 프레임 보간용)
            sm.smOuterTiltX_prev = thetaLerped;
            sm.smOuterExtraYaw_prev = yawLerped;
            sm.smOuterFade_prevTime = animationProgress;

            sm.smOuterTiltX = thetaLerped;  // cape 클램프 (B-17) 도 보간된 값 사용
        }

        // isHeadJumping body X 기울기: θ = Quarter - currentVerticalAngle (C-42, SmartMovingModel.md 10번 분기)
        if (sm.isHeadJumping) {
            float theta = (float) Math.PI / 2f - sm.stats.currentVerticalAngle;
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(theta));
            sm.smOuterTiltX = theta;
        }
    }

    /**
     * renderName() 대응 — 타인 플레이어 이름 태그 Y 보정 + 크롤 중 이름 숨김.
     *
     * 원본: SmartMovingRender.renderName()
     *   - isCrawling && !isClimbing && !crawlNameTag → 이름 숨김
     *   - heightOffset == -1 → d1 -= 0.2F
     *   - originalSneaking && sneakNameTag → d1 -= 0.05F (비스니킹 취급 시 y 보정)
     *
     * sneakNameTag(스니킹 중 64 거리 기준 확장)은 MixinLivingEntityRenderer에서 처리.
     */
    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V",
            at = @At("HEAD"), cancellable = true)
    private void sm_renderLabel(AbstractClientPlayerEntity entity, Text text,
            MatrixStack matrices, VertexConsumerProvider consumers, int light, float tickDelta,
            CallbackInfo ci) {
        if (entity instanceof ClientPlayerEntity) return;
        SmartMovingClientState sm = SmartMovingClientState.get(entity.getUuid());
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!cfg.enabled) return;

        // 크롤 중 이름 숨김 (crawlNameTag=false)
        if (sm.isCrawling && !sm.isClimbing && !cfg.crawlNameTag) {
            ci.cancel();
            return;
        }

        // heightOffset == -1: 헤드점프 상태 이름 태그 y -0.2F 보정
        if (sm.heightOffset == -1f) {
            matrices.translate(0.0, -0.2, 0.0);
        }
        // 스니킹 중 sneakNameTag=true: 비스니킹 취급으로 이름 표시 → y -0.05F 보정
        // (원본: 스니킹→비스니킹 포즈 전환 시 이름 태그 위치 차이 보정)
        else if (entity.isSneaking() && cfg.sneakNameTag) {
            matrices.translate(0.0, -0.05, 0.0);
        }
    }

    /**
     * 🔴 fade 보간 헬퍼 (Flying Phase / 세션 49): 원본 ModelRotationRenderer.GetIntermediateAngle
     *   1:1 매핑 (L347-L364).
     *
     * 원본:
     * <pre>
     *   private static float GetIntermediateAngle(float prev, float should, boolean fade,
     *                                              float lastTotalTime, float totalTime) {
     *       if(!fade || should == prev) return should;
     *       while(prev >= Whole) prev -= Whole;
     *       while(prev < 0F) prev += Whole;
     *       while(should >= Whole) should -= Whole;
     *       while(should < 0F) should += Whole;
     *       if(should > prev && (should - prev) > Half) prev += Whole;
     *       if(should < prev && (prev - should) > Half) should += Whole;
     *       return prev + (should - prev) * (totalTime - lastTotalTime) * 0.2F;
     *   }
     * </pre>
     *
     * 호출처 가드 (fadeIntermediate L317): `totalTime - prevTotalTime <= 2F` 시만 보간.
     * Whole = 2π (라디안). Half = π. 큰 각도 차이 (반대편) 시 wrapping 처리.
     *
     * 우리 매핑: fade 항상 활성 (비행 시 fadeRotateAngleX/Y = true). prevTime 미초기화 (-999) 시
     *   즉시 적용 (보간 skip).
     */
    @Unique
    private static float lerpFadeAngle(float prev, float target, float prevTime, float currentTime) {
        // prevTime 미초기화 또는 2 ticks 이상 차이 → 즉시 적용 (보간 skip)
        if (prevTime < -100f) return target;
        float deltaTime = currentTime - prevTime;
        if (deltaTime > 2f || deltaTime < 0f) return target;
        if (target == prev) return target;
        // 큰 각도 차이 (반대편) wrapping — 원본 L352-L362
        final float WHOLE = (float) (2.0 * Math.PI);
        final float HALF = (float) Math.PI;
        float p = prev, s = target;
        while (p >= WHOLE) p -= WHOLE;
        while (p < 0f) p += WHOLE;
        while (s >= WHOLE) s -= WHOLE;
        while (s < 0f) s += WHOLE;
        if (s > p && (s - p) > HALF) p += WHOLE;
        if (s < p && (p - s) > HALF) s += WHOLE;
        return p + (s - p) * deltaTime * 0.2f;
    }
}
