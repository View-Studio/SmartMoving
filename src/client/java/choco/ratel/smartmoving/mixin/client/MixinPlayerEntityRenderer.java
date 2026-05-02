package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.client.SmartMovingRenderContext;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
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
        SmartMovingClientState.smStandardFadeActive = false;  // 낙하/기본 상태 fade flag reset.
        SmartMovingClientState.smFallingFadeMode = false;     // 낙하 mode flag reset.
        // 🔴 (세션 52b): partial tick 캐시 저장 (Mixin private static 제약 우회 — SmartMovingClientState 사용).
        //   setAngles inject (sm_animateFlying 등) 에서 getCurrentSpeed/getTotalDistance lerped getter 호출용.
        SmartMovingClientState.globalCachedTickDelta = tickDelta;
        // 🔴 (2026-04-27) animationProgress 캐시 — 낙하/기본 상태 body fade 식 deltaT 계산용.
        SmartMovingClientState.smCachedAnimationProgress = animationProgress;
        // body fade adjustment skip 가드 (force 분기 시 true).
        SmartMovingClientState.smBodyYawActive_publicShared = false;
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
        if (!smActive) {
            // 🔴 (2026-04-27) 낙하/기본 상태 분기 — 두 모드:
            //   - 낙하: force 매핑 (smBodyYawActive=true; smBodyYawOverride=0) + head force.
            //          비행 패턴과 동일. 머리/몸 같이 fade lag.
            //   - 기본 상태 (땅 위): smBodyYawActive=false 유지 → ModifyArg 가 vanilla bodyYaw 를
            //          받아 fade lerp 적용 후 force (sm_modifyBodyYaw 내부).
            //          = vanilla 자연 동작 (키보드 이동 시 몸 회전) + 추가 fade lag (마우스 부드러움).
            //   사용자 보고 흐름:
            //     1) "마우스 회전 시 머리/몸 회전 속도 차이 부드럽게" → fade lag 필요.
            //     2) "키보드 좌우 이동 시 몸 경직" → vanilla force 0 무력화 안 해야.
            //     3) "마우스 회전 시 lag 가 없어짐" → fade 다시 활성화.
            //   해결: ModifyArg 가 force=0 안 하고 vanilla bodyYaw 에 fade 만 추가.
            boolean isFalling = !localPlayer.isOnGround()
                    && localPlayer.fallDistance > SmartMovingConfig.Config.fallAnimationDistanceMinimum
                    && !localPlayer.isTouchingWater();
            if (isFalling) {
                // 🔴 (2026-04-27) 낙하 매핑 변경 — standard 와 동일한 ModifyArg lerp 모드.
                //   이전 (force=0 + 추가 R_y) 매핑은 vanilla setupTransforms 의 자연 회전을
                //   무력화 → 원본의 "vanilla 즉시 + bipedOuter fade" 이중 효과 중 즉시 부분 빠짐.
                //   사용자 보고: 낙하 시 WASD 회전이 원본보다 lag.
                //   해결: smBodyYawActive=false 유지 → sm_modifyBodyYaw 가 vanilla bodyYaw 위에
                //         fade lerp 만 적용 → vanilla 즉시 효과 + 추가 fade lag (원본 1:1).
                //   smFallingFadeMode=true 는 유지 — sm_animateFalling head.yaw=0 force +
                //   sm_modifyNetHeadYaw 보정 skip 발동용 (머리/몸 같이 회전).
                SmartMovingClientState.smStandardFadeActive = true;
                SmartMovingClientState.smFallingFadeMode = true;
            } else {
                // 기본 상태: ModifyArg 가 fade lerp 적용 모드. smBodyYawActive=false 유지.
                SmartMovingClientState.smStandardFadeActive = true;
                SmartMovingClientState.smFallingFadeMode = false;
            }
            return;
        }

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
            // 🔴 fade 보간 적용 (사용자 보고 — 마우스 회전 시 몸통 회전 보간 원본과 다름):
            //   원본 SmartRenderModel L209: bipedOuter.fadeRotateAngleY = true (기본).
            //   ModelRotationRenderer.fadeIntermediate 가 매 frame target 으로 0.2*deltaTime lerp.
            //   1.21.1 매핑: lerpFadeAngle 헬퍼 (라디안 단위, 0.2 factor) 사용. 비행 패턴 차용.
            float targetYawRad = rotateY + horizontalAngle;
            float laggedYawRad = lerpFadeAngle(sm.smCeilingYaw_prev, targetYawRad,
                                               sm.smCeilingFade_prevTime, animationProgress);
            sm.smCeilingYaw_prev = laggedYawRad;
            sm.smCeilingFade_prevTime = animationProgress;
            smBodyYawActive = true;
            smBodyYawOverride = (float) Math.toDegrees(laggedYawRad);
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
        // 🔴 마우스 회전 delay 정밀 매핑 (Flying Phase / 세션 61):
        //   원본 흐름:
        //     1. SmartRenderRender.rotatePlayer L162-L167: actualRotation=0 강제
        //        → vanilla super.rotatePlayer 가 GL 회전 cancel.
        //     2. SmartMovingModel.isFlying L486: bipedOuter.rotateAngleY = horizontalAngle
        //        (절대값, fade 보간 적용).
        //     3. SmartRenderModel L247: fadeIntermediate → bipedOuter.Y_lerped 추적.
        //     4. 모델 회전 = bipedOuter.Y_lerped = horizontalAngle 추적 (fade).
        //   = 마우스 회전 시 모델 fade 천천히 추적 (delay + smooth).
        //
        //   1.21.1 vanilla 차이: setupTransforms 의 POSITIVE_Y(180-bodyYaw) cancel 하면
        //   (bodyYaw=180 시 POSITIVE_Y(0)) 모델 정면 = vanilla model default (north = -Z 좌표) →
        //   정면 반대 (세션 58 사용자 보고).
        //   1.21.1 vanilla 가 POSITIVE_Y(180) 적용 시 모델 정면 = south (정상). 즉 vanilla 의
        //   "no rotation" 효과 = bodyYaw=0 (POSITIVE_Y(180-0)=POSITIVE_Y(180)).
        //
        //   매핑:
        //     - smBodyYawOverride = 0f → vanilla POSITIVE_Y(180) = 모델 정면 정상.
        //     - entity.bodyYaw / prevBodyYaw 강제 제거 (vanilla 자연 처리, ModifyArg 가 0 으로 무력화).
        //     - smFlyingExtraYaw = horizontalAngle (절대값) — fade 보간 추적.
        //     - sm_animateFlying head.yaw = 0 강제 (세션 60 — bipedHead reset 효과).
        //   결과: 모델 회전 = POSITIVE_Y(180) (정면 정상) + extraYaw_lerped (= horizontalAngle 추적, fade).
        //         vanilla 자연 bodyYaw 무력화 (ModifyArg 0).
        //         마우스 회전 시 horizontalAngle 변화 → fade 천천히 추적 → 모델 delay.
        if (sm.isFlying) {
            float horizontalAngle = sm.stats.horizontalDistance < 0.05F
                    ? sm.stats.currentCameraAngle
                    : sm.stats.currentHorizontalAngle;
            smBodyYawActive = true;
            smBodyYawOverride = 0f;  // vanilla POSITIVE_Y(180-0)=POSITIVE_Y(180) → 모델 정면 정상.
            // bipedOuter.rotateAngleY 효과: horizontalAngle (절대값, fade 보간).
            smFlyingExtraYaw = horizontalAngle;
            // 🔴 (2026-04-27) 원본 SmartMovingRender L145-L148 1:1 복구.
            //   `entity.renderYawOffset = forwardRotation (=lerpedYaw)` 매 frame 강제.
            //   비행 동작 자체엔 영향 없음 (sm_modifyBodyYaw ModifyArg 가 0 으로 덮어씀,
            //   sm_animateFlying TAIL 이 head.yaw=0 force).
            //   효과: 비행 종료 → standard/falling 진입 시 vanilla netHeadYaw ≈ 0 →
            //   setAngles 의 head.yaw 점프 사라짐 (사용자 보고 "비행→낙하 사이 머리 끊김" 해소).
            //   세션 61 에서 제거됐던 코드를 원본 1:1 분석 후 복구.
            float lerpedYaw = localPlayer.prevYaw + (localPlayer.getYaw() - localPlayer.prevYaw) * tickDelta;
            localPlayer.setBodyYaw(lerpedYaw);
            localPlayer.prevBodyYaw = lerpedYaw;
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
        // vanilla 1 frame lerped bodyYaw 캐시 (sm_modifyNetHeadYaw 의 head 보정에 사용).
        SmartMovingClientState.smCachedBodyYawNaturalDeg = bodyYaw;
        if (smBodyYawActive) {
            SmartMovingClientState.smBodyYawActive_publicShared = true;
            SmartMovingClientState.smStandardBodyYawPrev = smBodyYawOverride;
            SmartMovingClientState.smStandardFadeTimePrev = SmartMovingClientState.smCachedAnimationProgress;
            // 비행 외 분기에서 비행 fade prev 갱신용 — ModifyArg 의 실제 적용 결과 캐시.
            SmartMovingClientState.smCachedBodyYawLaggedDeg = smBodyYawOverride;
            return smBodyYawOverride;
        }
        // 🔴 (2026-04-27) 기본 상태 fade lerp — vanilla bodyYaw 위에 추가 lag (factor 0.2).
        //   smStandardFadeActive=true && smBodyYawActive=false ⇔ 땅 위 기본 상태.
        //   vanilla bodyYaw 의 자연 lerp (키보드 이동 시 몸 회전) 보존 + 마우스 회전 시 부드러움.
        if (SmartMovingClientState.smStandardFadeActive) {
            float lagged = SmartMovingClientState.applyFadeAngleDegrees(bodyYaw);
            // head 보정용 캐시 — sm_modifyNetHeadYaw 가 lagged - natural 차이만큼 보정.
            SmartMovingClientState.smCachedBodyYawLaggedDeg = lagged;
            return lagged;
        }
        // vanilla 통과 — 비행 fade prev 갱신용 캐시도 vanilla bodyYaw.
        SmartMovingClientState.smCachedBodyYawLaggedDeg = bodyYaw;
        return bodyYaw;
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

            // 🔴 (2026-04-27) 비행 → 비행 외 전환 시 standard fade prev 자연 시작점 매핑.
            //   비행 모델 회전 = POSITIVE_Y(180-0) + POSITIVE_Y(-yawLerped) = POSITIVE_Y(180-yawLerped_deg).
            //   = setupTransforms 등가 bodyYaw 인자 = yawLerped_deg.
            //   sm_modifyBodyYaw force 분기는 smBodyYawOverride=0 으로 갱신 → 잘못된 값.
            //   여기서 yawLerped_deg 로 덮어쓰기 → 비행 → standard 전환 시 lerp 자연 시작.
            //   사용자 보고 "비행 릴리즈 시 몸통 한번 돌아감" 해소.
            float yawLerpedDeg = (float) Math.toDegrees(yawLerped);
            SmartMovingClientState.smCachedBodyYawLaggedDeg = yawLerpedDeg;
            SmartMovingClientState.smStandardBodyYawPrev = yawLerpedDeg;
            SmartMovingClientState.smStandardFadeTimePrev = animationProgress;
        }

        // isHeadJumping body X 기울기: θ = Quarter - currentVerticalAngle (C-42, SmartMovingModel.md 10번 분기)
        if (sm.isHeadJumping) {
            float theta = (float) Math.PI / 2f - sm.stats.currentVerticalAngle;
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(theta));
            sm.smOuterTiltX = theta;
        }

        // 🔴 D-4 매트릭스 변환 시도 → 회전 중심 차이로 자세 잘못 (사용자 보고 5회차).
        //   원본 bipedTorso 회전 중심 = bipedTorso pivot (0,0,-6) = 허리/머리 위치 (작은 visual 효과).
        //   1.21.1 matrices 회전 중심 = vanilla setupTransforms 후 위치 = entity 발 부근 (큰 효과).
        //   같은 0.5rad 이라도 visual 차이 → 매트릭스 방식 폐기.
        //   이전 직접 매핑 (body.pitch, pivotZ, arm 누적) 으로 복원. 분리는 그대로 (수용 가능 수준).
        //   sm_animateClimbing 의 D-4 분기 본체 참조.

        // 🔴 (2026-04-27) falling matrix 분기 비활성화 — sm_modifyBodyYaw 의 fade lerp 로 통합.
        //   이전: force=0 + 추가 R_y(-yawLerped) → vanilla setupTransforms 무력화 → 원본 동작 차이.
        //   새 매핑: smBodyYawActive=false → sm_modifyBodyYaw 가 vanilla bodyYaw 에 fade lerp 만
        //   적용 → vanilla 즉시 + 추가 fade (원본 1:1).
        //   추가 R_y 불필요. smCachedYawLerpedRad / smStandardFadeYaw_prev 도 사용 안 함.

        // 🔴 (2026-04-27) 비행 외 분기에서 비행 fade prev 매 frame 갱신.
        //   사용자 보고: 비행 진입 시 부드럽게 안 됨, 중간 끊김.
        //   prev = ModifyArg 의 실제 적용 결과 (smCachedBodyYawLaggedDeg) 라디안.
        //   비행 외 모델 회전 = POSITIVE_Y(180 - applied_deg) = π - applied_rad.
        //   비행 첫 frame 모델 회전 = POSITIVE_Y(180) + POSITIVE_Y(-yawLerped) = π - yawLerped_rad.
        //   yawLerped 시작 = applied_rad → 같음 → 끊김 없음.
        //   원본 SmartRender bipedOuter.previous 가 모든 분기 공통 단일 변수인 것을 매핑.
        if (!sm.isFlying) {
            sm.smOuterTiltX_prev = 0f;
            sm.smOuterExtraYaw_prev = (float) Math.toRadians(SmartMovingClientState.smCachedBodyYawLaggedDeg);
            sm.smOuterFade_prevTime = animationProgress;
        }

        // 🔴 천장 등반 fade prev 매 frame 갱신 (비행 prev 갱신 패턴과 동일).
        //   천장 등반 외 분기에서 prev = 직전 vanilla 또는 SM bodyYaw (라디안).
        //   진입 첫 frame fade 자연 시작 (이전값 → target lerp).
        if (!sm.isCeilingClimbing) {
            sm.smCeilingYaw_prev = (float) Math.toRadians(SmartMovingClientState.smCachedBodyYawLaggedDeg);
            sm.smCeilingFade_prevTime = animationProgress;
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

    // ── 1인칭 손 렌더 컨텍스트 마킹 ────────────────────────────────────────────
    //
    // PlayerEntityRenderer.renderArm 은 HeldItemRenderer 가 1인칭 손을 그릴 때
    //   model.setAngles(player, 0,0,0,0,0)
    //   arm.pitch = 0F; arm.render(...);
    //   sleeve.pitch = 0F; sleeve.render(...);
    // 흐름으로 호출. 이 setAngles 호출 시 SM 의 sm_setAnglesHead/sm_setAngles 가
    // 발동하면 1인칭 손이 SM 분기 자세로 덮여 사라지거나 이상해짐.
    //
    // HEAD 에서 firstPersonArmRender=true → sm_setAngles 가 즉시 return → 손은 vanilla
    // 기본 자세. RETURN 에서 false 로 복원 → 같은 frame 의 3인칭 본체 렌더는 정상 SM 적용.
    //
    // private 메서드지만 Mixin 으로 인젝트 가능. renderRightArm/renderLeftArm 둘 다 이
    // private renderArm 으로 위임하므로 한 곳만 후킹하면 양쪽 모두 커버.
    @Inject(method = "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/model/ModelPart;)V",
            at = @At("HEAD"))
    private void sm_renderArmHead(MatrixStack matrices, VertexConsumerProvider vc, int light,
                                  AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve,
                                  CallbackInfo ci) {
        SmartMovingRenderContext.firstPersonArmRender = true;
    }

    @Inject(method = "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/model/ModelPart;)V",
            at = @At("RETURN"))
    private void sm_renderArmReturn(MatrixStack matrices, VertexConsumerProvider vc, int light,
                                    AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve,
                                    CallbackInfo ci) {
        SmartMovingRenderContext.firstPersonArmRender = false;
    }
}
