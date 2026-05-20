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

    // 🔴 (Phase 2 fix-3-1) mixin static 3 개 → SmartMovingClientState instance field 로 이동.
    //   기존 smBodyYawActive, smBodyYawOverride, smFlyingExtraYaw 가 mixin 의 single instance 라
    //   multiplayer 시 두 player 동시 render 시 마지막 값으로 mixed → bodyYaw force 잘못 (BUG-C).
    //   사용처: sm_captureBodyYaw 가 sm 인스턴스 통해 set, sm_modifyBodyYaw / sm_setupTransforms_TAIL
    //   (ModifyArg / TAIL) 가 SmartMovingClientState.currentRenderTargetSm() 통해 read.

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
        // 🔴 (Phase 2 multi BUG-3) ClientPlayerEntity 분기 → 모든 player 통합.
        //   기존: self 만 isCrawling/isCrawlClimbing/isSliding 시 -1m 보정. remote 는 미적용 →
        //         사용자 보고 "슬라이딩 1블록 위 떠있음" + 엎드리기/CC 도 동일 BUG.
        //   변경: 모든 player 처리. 기존 remote +0.125 (= 지면 뚫림 방지) 는 별도 case.
        SmartMovingClientState sm = SmartMovingClientState.get(entity);

        // 헤드점프: 모델 origin = entity.y. 박스 처리 (mixin offset 활성 + POSE=SLIDING) 로
        //   box.minY = entity.y +1m. 추가 -1m offset 적용 시 모델 머리가 박스 발 보다 0.5m 아래
        //   = 박스 밖 아래 (사용자 보고 "1칸 낮음", 2026-05-11).
        //   isSliding 와 동일 패턴 (= entity.y origin) → 모델 root = entity.y +1.501m = 박스 안.
        //   feedback_sliding_entity_y_push_offset_zero.md 참조.
        if (sm.isHeadJumping && sm.heightOffset != 0f) {
            cir.setReturnValue(Vec3d.ZERO);
            return;
        }

        // 크롤링: SWIMMING 포즈 오프셋 대신 SM 크롤링 오프셋
        // 원본 SmartMovingRender L156-L161: heightOffset 적용 (= self+remote 모두).
        if (sm.isCrawling) {
            cir.setReturnValue(new Vec3d(0D, -1.0D - entity.getScale() * 0.06D, 0D));
            return;
        }
        if (sm.isCrawlClimbing) {
            cir.setReturnValue(new Vec3d(0D, -1.0D, 0D));
            return;
        }
        if (sm.isSliding) {
            // 🔴 fix #61 v3 (2026-05-10):
            //   isSliding 진입 시 entity.y -= 1m (= move(0,-1,0) 효과, 원본 1.7.10 1:1).
            //   mixin offset 가드 매치 → 박스 발 = entity.y + 1m = ground (정상).
            //   vanilla render 식 = 모델 origin = entity.y + offsetY. offsetY=0 시 모델 발
            //   = 박스 발 (= STANDING 시 entity.y=ground 와 동일 동작).
            //   isCrawling 분기 -1.06m 은 isCrawling 진입 시 entity.y unchange (= move 안 호출,
            //   원본 1:1) 라서 추가 -1m 보정 필요. isSliding 은 entity.y 이미 -1m push 됨 →
            //   추가 보정 X.
            cir.setReturnValue(Vec3d.ZERO);
            return;
        }
        // 🔵 (2026-05-20, fix #111 — fix #110 정정) swim/dive 모델 위치.
        //   기존 fix #110 매핑 `-1.0 - scale*0.06` 은 isCrawling 패턴 차용이었으나 박스 차이 1m
        //   고려 누락 → 모델이 박스보다 더 1m 아래 (= 사용자 보고 "1칸 아래" 잔존).
        //   - isCrawling: 박스 = (entity.y, entity.y+0.8) (mixin offset 차단, eye=0.62).
        //     모델 root 보정 = -1.06m → 모델 회전 후 박스 안 정렬.
        //   - swim/dive: 박스 = (entity.y+1, entity.y+1.8) (mixin offset 활성, eye=1.62 fix #102).
        //     박스가 1m 위로 push 됨 → 모델도 +1m 더 위로 = isCrawling 식 + 1m.
        //   결과: -1.06 + 1.0 = -0.06 ≈ Vec3d.ZERO (= 슬라이딩 패턴 동일).
        //   슬라이딩의 경우 entity.y 자체 -1m push → ZERO offset 으로 박스 안 정렬.
        //   swim/dive 는 entity.y unchange + 박스 +1m up → ZERO offset 으로 동일 시각 정렬.
        if (sm.isSwimming_sm || sm.isDiving) {
            cir.setReturnValue(Vec3d.ZERO);
            return;
        }

        // 타인 플레이어 (B-14 / §16-22 / 원본 SmartMovingRender L124-L125):
        // !isOwnPlayer && entity.isSneaking() && isCrawl → d1 += 0.125 (지면 뚫림 방지)
        //   위 isCrawling 분기에서 이미 처리되므로 도달 X. (legacy 보존만)
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
        // 🔴 (Phase 2 fix-3-1) flag reset 도 player 별 instance — multiplayer 시 두 player 분리.
        SmartMovingClientState sm = SmartMovingClientState.get(player);

        sm.smBodyYawActive = false;
        sm.smFlyingExtraYaw = 0f;
        sm.smStandardFadeActive = false;  // 낙하/기본 상태 fade flag reset.
        sm.smFallingFadeMode = false;     // 낙하 mode flag reset.
        sm.smCrawlMode = false;            // isCrawl 전용 flag reset (head 보정 skip).
        // 🔴 (세션 52b): partial tick 캐시 저장 (Mixin private static 제약 우회 — SmartMovingClientState 사용).
        //   setAngles inject (sm_animateFlying 등) 에서 getCurrentSpeed/getTotalDistance lerped getter 호출용.
        //   tickDelta 는 player 무관 cursor 라 static 유지.
        SmartMovingClientState.globalCachedTickDelta = tickDelta;
        // 🔴 (2026-04-27) animationProgress 캐시 — 낙하/기본 상태 body fade 식 deltaT 계산용.
        sm.smCachedAnimationProgress = animationProgress;
        // body fade adjustment skip 가드 (force 분기 시 true).
        sm.smBodyYawActive_publicShared = false;
        // 🔴 (Phase 1-A) bodyYaw force/fade 는 local 만 처리.
        //   force 값 = sm.stats.currentHorizontalAngle 등 — stats 는 매 tick local 계산만,
        //   다른 player 에 sync 안 됨 → force 시 default 0 적용 → 잘못된 yaw → "앞뒤 반대" BUG.
        //   다른 player 는 vanilla bodyYaw 그대로 (= server-sync entity.bodyYaw 사용).

        // 🔴 (Phase 2 fix-3-2) ClientPlayerEntity 가드 제거 — 모든 player force/fade 분기 처리.
        //   force 값 (sm.stats.currentXxx) 은 fix-2 의 position delta 기반 stats 로 remote 도 정확.
        //   localPlayer 변수명 그대로 두지만 실제 = 모든 player (local + remote, AbstractClientPlayerEntity).
        AbstractClientPlayerEntity localPlayer = player;
        // 🔴 (Phase 2 fix-3-2) cfg.enabled 검사 self-only 화 — BUG-CONFIG-2 fix 와 일관성.
        //   self disabled → 자기 SM state 모두 false → 자동으로 vanilla.
        //   remote 는 cfg 무관 SM 분기 진입 → SM state 따라 force 적용.
        if (!choco.ratel.smartmoving.client.SmartMovingClient.isSmRenderEnabled(player)) return;
        // 🔴 (Phase 2 fix-3-1) sm 인스턴스는 reset 단계에서 이미 정의 (L139). 재선언 제거.

        // 원본 SmartMovingRender.rotatePlayer L271-274 조건 1:1.
        // 원본에는 isRopeSliding/isCrawling 단독 없음, 대신 isClimbCrawling 포함.
        boolean smActive = sm.isClimbing || sm.isClimbCrawling || sm.isCrawlClimbing
                || sm.isFlying || sm.isSwimming_sm || sm.isDiving
                || sm.isCeilingClimbing || sm.isHeadJumping
                || sm.isSliding || sm.isAngleJumping();

        // 🔴 사용자 보고 fix 정정 (2026-05-03 — "max 후 body 가 마우스 따라 확 회전, 부드러운 lag 필요"):
        //   원본 SmartMovingRender L147 강제 분기에 isCrawl 단독 미포함 → vanilla 1.21.1 의
        //   LivingEntity.turnHead 자연 동작 (= getMaxRelativeHeadRotation = 50°).
        //
        //   원본 SmartRenderModel L208-L209: bipedOuter.rotateAngleY = actualRotation /
        //     RadiantToAngle (= bodyYaw 라디안), bipedOuter.fadeRotateAngleY = true (player).
        //   원본 fadeIntermediate (ModelRotationRenderer L323-L325) + GetIntermediateAngle (L347-L364):
        //     result = prev + (target - prev) * deltaT * 0.2F
        //   = bipedOuter.Y 가 0.2 lerp 추가 적용 → body 가 head 따라가는 속도 부드러움.
        //
        //   1차 fix (smStandardFadeActive=false) → vanilla 동작 그대로 → max 작동 OK 지만 body
        //     가 head 따라 확 회전 (사용자 보고).
        //   2차 fix (smStandardFadeActive=true + 그대로) → fade lag 적용되지만 sm_modifyNetHeadYaw
        //     의 head 보정 (netHeadYaw + bodyYaw_diff) 가 head.roll 에 영향 → max 50° 깨짐.
        //   진짜 fix: smStandardFadeActive=true 로 body fade lag + smCrawlMode=true 로 head 보정 skip.
        if (sm.isCrawling && !sm.isClimbing) {
            sm.smStandardFadeActive = true;   // body fade lag (= 부드러움)
            sm.smFallingFadeMode = false;
            sm.smCrawlMode = true;             // head 보정 skip (= max 50° 유지)
            return;
        }

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
                sm.smStandardFadeActive = true;
                sm.smFallingFadeMode = true;
            } else {
                // 기본 상태: ModifyArg 가 fade lerp 적용 모드. smBodyYawActive=false 유지.
                sm.smStandardFadeActive = true;
                sm.smFallingFadeMode = false;
            }
            return;
        }

        // isLevitating 우선 처리 (B-15 / §16-23): 원본 SmartMovingRender L132-L134 —
        //   levitating 시 모든 모델의 currentHorizontalAngle = currentCameraAngle 강제 정렬.
        // 원본은 분기 처리 후 마지막에 덮어쓰기지만 결과 동등 (모든 다른 분기 결과를 카메라
        //   방향으로 덮어쓰는 효과 = 분기 진입 전 카메라 방향 단독 적용과 동일).
        // 일반적으로 levitate 는 dive 자세와 함께 발생 (Levitation status effect).
        if (sm.isLevitating) {
            sm.smBodyYawActive = true;
            sm.smBodyYawOverride = (float) Math.toDegrees(sm.stats.currentCameraAngle);
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
            sm.smBodyYawActive = true;
            sm.smBodyYawOverride = (float) Math.toDegrees(laggedYawRad);
            return;
        }

        // isSwim/isDive (원본 L495/L553)
        // 🔵 (2026-05-21, fix #118) mouse 회전 + bodyYaw + 머리 회전 BUG fix — isFlying L333-L352
        //   검증된 4 단계 패턴 1:1 차용.
        //   사용자 보고: (1) 머리 mouse 추적 X, (2) 가만히 시 카메라만 (몸 회전 X = fade lag),
        //     (3) wasd + mouse 시 이동 방향 (lag).
        //   원본 mechanism (agent 실측 cause):
        //     - SmartMovingRender L148: renderYawOffset = forwardRotation (= lerpedYaw, camera).
        //     - SmartMovingModel L333/L375: bipedOuter.rotateAngleY = horizontalAngle (= 가만히 camera,
        //       이동 시 이동방향, fadeRotateAngleY=true 매 frame 0.2*deltaT lerp).
        //     - SmartRenderRender L167: actualRotation=0 → vanilla glRotatef(180) → bodyYaw 시각 효과 0.
        //     = 두 다른 yaw 값. 우리 매핑은 단일 smBodyYawOverride 으로 혼동 + fade 누락 + vanilla
        //       matrix cancel 누락.
        //   isFlying 패턴 1:1:
        //     - smBodyYawOverride = 0f → vanilla POSITIVE_Y(180) → 모델 정면 정상 (matrix cancel 등가).
        //     - smSwimDiveExtraYaw_target = horizontalAngle → sm_setupTransforms TAIL 에서 fade Y 회전.
        //     - entity.bodyYaw = lerpedYaw force → vanilla setAngles 의 netHeadYaw = headYaw-bodyYaw ≈ 0
        //       → 머리 mouse 추적 X.
        if (sm.isSwimming_sm || sm.isDiving) {
            float threshold = sm.isSlow ? 0.005F : 0.015F;
            double dist = sm.isDiving ? sm.stats.totalDistance : sm.stats.horizontalDistance;
            float horizontalAngle = dist < threshold
                    ? sm.stats.currentCameraAngle
                    : sm.stats.currentHorizontalAngle;
            sm.smBodyYawActive = true;
            sm.smBodyYawOverride = 0f;                              // matrix cancel 등가
            sm.smSwimDiveExtraYaw_target = horizontalAngle;         // fade 보간 target (radian)

            // entity.bodyYaw force — 원본 SmartMovingRender L148 1:1.
            //   vanilla netHeadYaw = (headYaw_lerped - bodyYaw_lerped) → bodyYaw=lerpedYaw 시 ≈ 0.
            float lerpedYaw = localPlayer.prevYaw + (localPlayer.getYaw() - localPlayer.prevYaw) * tickDelta;
            localPlayer.setBodyYaw(lerpedYaw);
            localPlayer.prevBodyYaw = lerpedYaw;
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
            sm.smBodyYawActive = true;
            sm.smBodyYawOverride = 0f;  // vanilla POSITIVE_Y(180-0)=POSITIVE_Y(180) → 모델 정면 정상.
            // bipedOuter.rotateAngleY 효과: horizontalAngle (절대값, fade 보간).
            sm.smFlyingExtraYaw = horizontalAngle;
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

        // (2026-05-03 정리) 이전 isCrawling 단독 가드는 위 !smActive 분기에서 이미 처리됨 → 도달 X.
        //   진짜 fix 는 sm_captureBodyYaw 의 !smActive 분기 위 isCrawl && !isClimbing 가드 (위 참조).
        // isHeadJumping/isRopeSliding (원본 L740/L279) — threshold 없이 currentHorizontalAngle
        // 🔴 fix #91 (2026-05-13, 사용자 보고 "헤드점프 중 벽 박을 때 몸 회전 중간 이어짐 없음"):
        //   원본 SmartRenderModel L208-209: bipedOuter.fadeRotateAngleY = true (기본).
        //   ModelRotationRenderer.fadeIntermediate → 0.2 * deltaTime lerp. 벽 박은 후
        //   vx 반전 시 currentHorizontalAngle 점진 변화 → 모델 자연 회전 추적.
        //   isCeilingClimbing 동일 패턴 (= L250-256) 차용. 외 분기 prev 갱신은 아래 (= 비행 prev 갱신 패턴).
        // 🔵 (2026-05-14, fix #98 위치 기반) smRemoteHJVisualHold OR 추가 — REMOTE 공중 자세 hold.
        //   사용자 보고 3번 BUG (= 콜리전 땅 닿기 전 헤드점프 끝남) fix. SmartMovingClient packet
        //   처리 안 공중 상태 (= curY > groundY+0.005) 검출 시 hold=true.
        //   sm_tickStatsForRemote TAIL 의 위치 비교 (= REMOTE.y <= groundY+0.005) 시 hold=false.
        if (sm.isHeadJumping || sm.isRopeSliding || sm.smRemoteHJVisualHold) {
            float targetYawRad = sm.stats.currentHorizontalAngle;
            // 🔴 fix #94/#95 (2026-05-13, 사용자 보고 "여우무빙 시 원래 몸 방향 → 바라보는 방향 보간"):
            //   fix #94: wasSelfSlideFire 가드 추가. fix #95: 발사 frame 만 snap.
            //   원인: fix #91 의 fade lerp 가 wasSelfSlideFire 가드 누락 → 여우무빙 발사 시
            //     prev=직전 vanilla bodyYaw → 5 tick lerp.
            //   fix #94 (= 매 frame prev=target): 발사 즉시 도달 ✓. 그러나 wasSelfSlideFire 잔존
            //     동안 매 frame instant → 벽 박을 때 currentHorizontalAngle 변화 시 fade 사라짐.
            //   fix #95 (= 발사 frame 만 snap + flag): 발사 frame 만 prev=target. 그 후 자연 fade
            //     (= fix #91 효과). 벽 박을 때 자연 보간 유지.
            //   X 회전 (fix #81 v2) 은 target=π/2 고정이라 매 frame snap 무관. Y 회전 target=
            //     currentHorizontalAngle 동적이라 snap 1회 한정 필요.
            if (sm.wasSelfSlideFire && !sm.smHeadJumpYawSnapDone) {
                sm.smHeadJumpYaw_prev = targetYawRad;
                sm.smHeadJumpYawFade_prevTime = animationProgress;
                sm.smHeadJumpYawSnapDone = true;
            }
            float laggedYawRad = lerpFadeAngle(sm.smHeadJumpYaw_prev, targetYawRad,
                                                sm.smHeadJumpYawFade_prevTime, animationProgress);
            sm.smHeadJumpYaw_prev = laggedYawRad;
            sm.smHeadJumpYawFade_prevTime = animationProgress;
            sm.smBodyYawActive = true;
            sm.smBodyYawOverride = (float) Math.toDegrees(laggedYawRad);
            return;
        }

        // 🔴 BUG-Slide-Anim-1 fix (2026-05-04): 원본 SmartMovingModel L447 1:1 복원.
        //   원본 isSlide 분기:
        //     L446 bipedOuter.fadeRotateAngleY = false   (fade 보간 비활성)
        //     L447 bipedOuter.rotateAngleY = currentHorizontalAngle   (이동 방향, 절대값)
        //   원본 rotatePlayer L147 의 renderYawOffset = forwardRotation 은 vanilla setupTransforms
        //   가 R_y(-forwardRotation) cancel 효과 → 그 후 bipedOuter.Y = currentHorizontalAngle 직접
        //   적용 → 최종 모델 yaw = currentHorizontalAngle (이동 방향). 마우스 회전 무관 → 모델은
        //   진입 시점 이동 방향 유지 = 슬라이딩 미끄러짐 자연스러움.
        //
        //   이전 1.21.1 매핑은 fallthrough (smBodyYawOverride = forwardRotation = 마우스 yaw lerp)
        //   라 마우스 따라 모델 같이 회전 → 사용자 보고 BUG.
        //   fix: smBodyYawOverride = currentHorizontalAngle (도) — 원본 1:1.
        //   fade 비활성 (원본 L446) → isHeadJumping/isRopeSliding 패턴 동일 (단순 적용).
        // 🔴 BUG-Slide-Anim-A fix (2026-05-04 사용자 보고 — "마우스 회전 시 머리 좌우 흔들림"):
        //   원본 1.7.10: SR 의 renderYawOffset = forwardRotation 강제 → entity.bodyYaw 자체
        //     force → vanilla netHeadYaw = headYaw - bodyYaw = 마우스 - 마우스 ≈ 0 → head.rotateAngleZ
        //     ≈ 0 → 흔들림 X.
        //   1.21.1 우리 매핑: sm_modifyBodyYaw (ModifyArg) 만 force. entity.bodyYaw field 자연 lerp
        //     (마우스 yaw 추적). vanilla netHeadYaw ≈ 0 (자연 lerp 결과).
        //     ★ 그러나 sm_modifyNetHeadYaw 가 추가 보정 = netHeadYaw + (natural - lagged) =
        //       0 + (마우스 yaw - 이동 방향) = 마우스 회전 시 큰 값 → head.roll = -netHeadYaw_rad
        //       변화 → 머리 좌우 기울어짐 BUG.
        //   엎드리기 isCrawling 분기 (위 L179) 와 동일 패턴: smCrawlMode=true → sm_modifyNetHeadYaw
        //     보정 skip → vanilla netHeadYaw (≈ 0) 그대로 → head.roll ≈ 0.
        if (sm.isSliding) {
            sm.smBodyYawActive = true;
            sm.smBodyYawOverride = (float) Math.toDegrees(sm.stats.currentHorizontalAngle);
            sm.smCrawlMode = true;   // ★ sm_modifyNetHeadYaw 보정 skip
            // 🔴 BUG-Slide-Anim-A2 fix (2026-05-04 사용자 보고 — "마우스 회전 시 머리 흔들림 안 사라짐"):
            //   원본 SmartMovingRender.rotatePlayer L145-L148 1:1: SM 활성 분기 (isSliding 포함)
            //   에서 `entity.renderYawOffset = forwardRotation (=lerpedYaw)` 매 frame 강제.
            //   메모리 feedback_entity_bodyyaw_force_branch_transition.md 패턴.
            //   비행 분기 (위 L332-L334) 적용 패턴 동일 차용.
            //
            //   원리:
            //     - vanilla setAngles `netHeadYaw = entity.headYaw_lerp - entity.bodyYaw_lerp`.
            //     - 이전 매핑: entity.bodyYaw 자연 lerp (이동 방향 따라 vanilla 처리) → 마우스
            //       yaw 와 차이 → netHeadYaw ≠ 0 → head.roll = -netHeadYaw_rad 변화 → 머리 좌우
            //       흔들림 BUG.
            //     - fix: entity.bodyYaw 를 마우스 yaw 로 force → headYaw_lerp = bodyYaw_lerp →
            //       netHeadYaw = 0 → head.roll = 0 → 흔들림 X.
            //   smBodyYawOverride (= currentHorizontalAngle, ModifyArg) 는 setupTransforms 의
            //   bodyYaw 인자만 수정 → 모델 회전은 이동 방향 (별개). entity.bodyYaw force 와 무관.
            float lerpedYaw = localPlayer.prevYaw
                    + (localPlayer.getYaw() - localPlayer.prevYaw) * tickDelta;
            localPlayer.setBodyYaw(lerpedYaw);
            localPlayer.prevBodyYaw = lerpedYaw;
            return;
        }

        // 나머지(isClimb/isClimbCrawling/isCeilingClimb/isAngleJumping) — 원본 rotatePlayer L269:
        //   forwardRotation = prevRotationYaw + (rotationYaw - prevRotationYaw) * f2 (player yaw 보간, 도)
        // isClimb/ClimbCrawling 은 원본 L308 forwardRotation/RadiantToAngle(라디안) 과 동등.
        // isCeilingClimb 의 `rotateY + horizontalAngle`(L476) 은 rotateY 공식 이식 필요 — 후속.
        sm.smBodyYawActive = true;
        sm.smBodyYawOverride = localPlayer.prevYaw + (localPlayer.getYaw() - localPlayer.prevYaw) * tickDelta;
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
        // 🔴 (Phase 2 fix-3-1) ModifyArg 인자에 entity 없음 → currentRenderTarget cursor 통해 sm 인스턴스 lookup.
        SmartMovingClientState sm = SmartMovingClientState.currentRenderTargetSm();
        if (sm == null) return bodyYaw;
        // vanilla 1 frame lerped bodyYaw 캐시 (sm_modifyNetHeadYaw 의 head 보정에 사용).
        sm.smCachedBodyYawNaturalDeg = bodyYaw;
        float result;
        if (sm.smBodyYawActive) {
            sm.smBodyYawActive_publicShared = true;
            // 🔴 (Phase 2 multi BUG-10 v2) FORCE + FADE 결합 분기:
            //   smBodyYawActive=true + smStandardFadeActive=true 동시 → force 값에 fade lerp 적용.
            //   사용처: isCrawling remote (force = currentHorizontalAngle, fade 로 보간 유지).
            if (sm.smStandardFadeActive) {
                float lagged = sm.applyFadeAngleDegrees(sm.smBodyYawOverride);
                sm.smCachedBodyYawLaggedDeg = lagged;
                result = lagged;
            } else {
                sm.smStandardBodyYawPrev = sm.smBodyYawOverride;
                sm.smStandardFadeTimePrev = sm.smCachedAnimationProgress;
                // 비행 외 분기에서 비행 fade prev 갱신용 — ModifyArg 의 실제 적용 결과 캐시.
                sm.smCachedBodyYawLaggedDeg = sm.smBodyYawOverride;
                result = sm.smBodyYawOverride;
            }
        } else if (sm.smStandardFadeActive) {
            // 🔴 (2026-04-27) 기본 상태 fade lerp — vanilla bodyYaw 위에 추가 lag (factor 0.2).
            float lagged = sm.applyFadeAngleDegrees(bodyYaw);
            // head 보정용 캐시 — sm_modifyNetHeadYaw 가 lagged - natural 차이만큼 보정.
            sm.smCachedBodyYawLaggedDeg = lagged;
            result = lagged;
        } else {
            // vanilla 통과 — 비행 fade prev 갱신용 캐시도 vanilla bodyYaw.
            sm.smCachedBodyYawLaggedDeg = bodyYaw;
            result = bodyYaw;
        }
        return result;
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
        // 🔴 (Phase 1-A) ClientPlayerEntity 가드 제거 — 다른 player render 도 SM 자세 적용.
        //   localPlayer 변수명 그대로 두지만 실제 = 모든 player (local + remote).
        AbstractClientPlayerEntity localPlayer = player;
        SmartMovingClientState sm = SmartMovingClientState.get(localPlayer);

        // BUG-12 (세션 36): SM disabled 시 모든 SM 분기 skip + smOuterTiltX = 0 reset →
        //   매 호출마다 0 으로 정리하므로 cape 클램프도 vanilla fallback (BUG-7 확장).
        sm.smOuterTiltX = 0f;
        // 🔴 (2026-05-05) self → Config.enabled / remote → 항상 true.
        if (!choco.ratel.smartmoving.client.SmartMovingClient.isSmRenderEnabled(player)) return;

        // B-17 capture (§16-25): outer.X 등가 tiltAngle 을 SmartMovingClientState.smOuterTiltX 에
        //   저장해 MixinCapeFeatureRenderer 에서 망토 X 회전 클램프 (70.523° - outerX_deg) 적용.
        //   진입 시 reset — 5 분기 중 한 분기만 활성 시 = 으로 할당, 비활성 시 0 (vanilla 망토 그대로).

        // SM 수영(isSwimming_sm): bipedOuter.rotateAngleX = Quarter - Sixteenth * standSneakFactor
        // 원본: fadeRotateAngleX = true + rotateAngleX = Quarter - Sixteenth * standSneakFactor
        // standSneakFactor: 정지/스니킹=1 → 67.5°, 보행=0 → 90°(완전 수평)
        // 🔵 (2026-05-20, BUG-Swim-Anim-1/3 fix #110) 슬라이딩 L554-565 패턴 1:1 적용:
        //   - pivotY = 1.5 - 3/16 = 1.3125 (= 엎드리기/슬라이딩 동일 머리 중심 회전).
        //   - translate(0, pivotY, 0) → rotate(-tiltAngle) → translate(0, -pivotY, 0)
        //     매트릭스 스택 — pivot 기준 회전. 사용자 보고 "회전 중심 머리 부분" fix.
        //   - POSITIVE_X.rotation(-tiltAngle) 부호 반전 — vanilla scale(-1,-1,1) 보정
        //     (= [[feedback_render_scale_negation]] + BUG-Slide-Anim-2 "하늘 봄" 정확 매치).
        //     사용자 보고 "180도 뒤집혀 하늘 보고 있음" fix.
        // 🔵 (2026-05-20, fix #115) isSwimming_sm + isDiving 통합 fade 분기.
        //   사용자 보고 BUG 1 (수면 도달 시 1자) + BUG 2 (wasd 시 팔다리 이상) 공통 cause =
        //     fadeRotateAngleX 매핑 누락.
        //   원본 SmartMovingModel L331 (isSwim) + L373 (isDive): `bipedOuter.fadeRotateAngleX = true`.
        //   원본 ModelRotationRenderer.fadeIntermediate L315-L345: 매 frame `current = prev +
        //     (target - prev) * deltaTime * 0.2F` 점진 lerp. 진입/전환 시 부드러운 자세 변화.
        //   기존 매핑 = 즉시 R_x(tilt) 적용 → vAngle / sSF 동적 변화 시 즉시 자세 변화 → 사용자
        //     "이상한 흔들림" + "1자 변환" 인지.
        //   해결: lerpFadeAngle 헬퍼 (= 비행/헤드점프 분기 검증 패턴) 차용. swim/dive 통합 분기 +
        //     별도 prev field (smSwimDiveTiltX_prev) 사용.
        //
        //   isJumping 매핑 정정:
        //   원본 isDive `isJump` 변수 = `moving.isJumping()` override 메서드 (SmartMovingSelf L3264)
        //     = vanilla player.jumping field (= raw space bar 누름).
        //   기존 매핑 `sm.isJumping` = SM 자체 1-tick flag (= tryJump 한정, 거의 항상 false) →
        //     dive 분기에서 사용자 jump 꾹누름 시 tilt=0° 도달 안 함.
        //   해결: vanilla LivingEntity.jumping field 직접 사용 (= local player raw key).
        if (sm.isSwimming_sm || sm.isDiving) {
            float targetTilt;
            if (sm.isDiving) {
                if (sm.isLevitating) {
                    targetTilt = (float) Math.PI / 2f - (float) Math.PI / 16f;
                } else if (sm_isPlayerJumping(player)) {  // ★ vanilla jumping = raw space bar
                    targetTilt = 0f;
                } else {
                    targetTilt = (float) Math.PI / 2f - sm.stats.currentVerticalAngle;
                }
            } else {  // isSwimming_sm
                targetTilt = (float) Math.PI / 2f - (float) Math.PI / 16f * sm.swimStandSneakFactor;
            }

            // 🔵 fade lerp (= ModelRotationRenderer.fadeIntermediate 1:1).
            //   prev 시작값 = 진입 직전 자세 (= 0, vanilla STANDING). [[feedback_fade_prev_pre_entry_pose]].
            //   진입 frame: prevTime < -100f → lerpFadeAngle 즉시 target return (= snap).
            //   정상 frame: deltaTime * 0.2 lerp → 5 frame 후 ~99% 도달.
            float laggedTilt = lerpFadeAngle(sm.smSwimDiveTiltX_prev, targetTilt,
                                              sm.smSwimDiveFade_prevTime, animationProgress);

            // 🔵 (2026-05-21, fix #118-B) swim/dive Y 회전 fade — 마우스 회전 lag.
            //   원본 bipedOuter.rotateAngleY = horizontalAngle (fadeRotateAngleY=true).
            //   isFlying 분기 (L779-L780, L786-L788) 의 fade Y 회전 패턴 1:1 차용.
            //   가만히 시 horizontalAngle = camera → 즉시 force 시 사용자 보고 "몸 회전 X" 매치 안 됨
            //   → fade 적용 시 매 frame 0.2 lerp → 천천히 추적 (= 사용자 보고 매치).
            //   wasd + mouse 시 horizontalAngle = 이동방향 (motion vector atan2) → mouse 변화 시
            //   motion 변경 → horizontalAngle 추적 + fade lag (= 사용자 보고 매치).
            float yawTarget = sm.smSwimDiveExtraYaw_target;
            float yawLerped = lerpFadeAngle(sm.smSwimDiveExtraYaw_prev, yawTarget,
                                             sm.smSwimDiveFade_prevTime, animationProgress);

            sm.smSwimDiveTiltX_prev = laggedTilt;
            sm.smSwimDiveExtraYaw_prev = yawLerped;
            sm.smSwimDiveFade_prevTime = animationProgress;

            // 매트릭스 stack 적용 (= fix #114 pivotY=1.5 동일).
            //   isFlying 패턴 (L782-L790): translate → R_y(-yaw, scale 부호 반전) → R_x(-tilt) → translate.
            float pivotY = 1.5f;
            matrices.translate(0f, pivotY, 0f);
            if (yawLerped != 0f) {
                // vanilla scale(-1,-1,1) 의 Y axis 부호 반전 보정 — isFlying L786-L788 패턴.
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-yawLerped));
            }
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(-laggedTilt));
            matrices.translate(0f, -pivotY, 0f);
            sm.smOuterTiltX = laggedTilt;
        } else {
            // swim/dive 비활성 시 fade prev reset (= 다음 진입 시 prev=0 시작).
            //   비행 fade prev reset 패턴 (L866-868) 동일.
            sm.smSwimDiveTiltX_prev = 0f;
            sm.smSwimDiveExtraYaw_prev = 0f;
            sm.smSwimDiveFade_prevTime = animationProgress;
        }

        // SM 슬라이딩(isSliding): bipedOuter.rotateAngleX = Quarter
        // 🔴 BUG-Slide-Anim-2 fix (2026-05-04 — "하늘 봄"): tilt 부호 반전 (scale -1,-1,1 보정).
        // 🔴 BUG-Slide-Anim-3/4 정정 (2026-05-04 사용자 보고 — "몸통이 다리쪽으로 너무 가있음"):
        //   이전 매핑은 엎드리기 isCrawling 분기 (`bipedTorso.rotationPointY=3F` 매핑) 를 슬라이딩에
        //   그대로 따라했음 — 원본 isCrawl 분기 매핑이지 isSlide 분기 매핑 아님.
        //   원본 isSlide L448: `bipedOuter.rotationPointY = 5F` (bipedTorso 가 아니라 bipedOuter).
        //   즉 슬라이딩 정확 매핑:
        //     pivotY = 1.5 - 5/16 = 1.1875 (= bipedOuter pivot world Y, vanilla biped pivot 보다 5/16 위).
        //     추가 translate = -5/16 (= bipedOuter.rotationPointY=+5F 자식 효과).
        //   엎드리기 (3/16) vs 슬라이딩 (5/16) 차이 = 2/16 = 12.5cm 회전 origin 위쪽 + body 위치
        //     12.5cm 회전 후 다리쪽 추가.
        //   ※ 모델 위치 -1m 보정 (박스 안) 은 sm_getPositionOffset 분기에서 처리 (BUG#5).
        if (sm.isSliding) {
            float tiltAngle = (float) Math.PI / 2f; // Quarter
            // 🔴 (2026-05-04 사용자 요청 — "슬라이딩 머리 위치 엎드리기와 통일"):
            //   원본 isSlide L448 = bipedOuter.rotationPointY=5F (= pivotY 1.1875).
            //   원본 isCrawl L405 = bipedTorso.rotationPointY=3F (= pivotY 1.3125).
            //   원본은 두 자세 회전 중심 12.5cm 차이 → 슬라이딩 머리가 엎드리기보다 앞.
            //   사용자 명시 일관성 우선 — 슬라이딩 pivotY 를 엎드리기와 동일 (1.3125) 로 통일.
            float pivotY = 1.5f - 3f / 16f;    // = 1.3125 (엎드리기와 동일 회전 중심)

            matrices.translate(0f, pivotY, 0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(-tiltAngle));
            matrices.translate(0f, -pivotY, 0f);
            sm.smOuterTiltX = tiltAngle;

            // 🔴 fix #86 v4 (2026-05-12, 사용자 보고 "엎드리기보다 슬라이딩 발쪽 치우침, 머리쪽으로 올려"):
            //   엎드리기 R_x(-78.75°) vs 슬라이딩 R_x(-90°) 회전 각도 차이 11.25° 로 슬라이딩
            //   발 vertex 가 더 정면으로 멀리 → 모델 중심이 발쪽으로 치우침.
            //   사용자 의도: 수평 누운 자세에서 *머리쪽* (= 모델 머리-발 축의 머리 방향) 으로 보정.
            //   매트릭스 분석: setupTransforms TAIL 의 translate Y → vertex 회전 전 적용 →
            //     R_x(-π/2) 후 world Z (= 수평 정면-뒤쪽) 영향. dy 양수 방향 (= 덜 음수) 으로
            //     변경 시 모델이 머리쪽 (정면) 으로 이동.
            //   값: -3/16 (발쪽 치우침) ↔ -1/16 (머리쪽 과보정) 중간 = -2/16.
            matrices.translate(0f, -2f / 16f, 0f);  // Y: 슬라이딩 머리쪽 보정 (회전 각도 차이 ⊃ 발쪽 치우침)

            // 🔴 여우무빙 (= 슬라이딩 ↔ 헤드점프 직접 cycle, standing 안 거침) 헤드점프 prev
            //   자연 시작점 매핑 (2026-05-11, 사용자 보고 "여우무빙 시 몸 거의 회전 안 함,
            //   바로 수평 세팅"):
            //   원본 SmartMovingModel L449 (1.7.10) / L486 (1.12.2) isSlide 분기 식
            //     = `bipedOuter.rotateAngleX = Quarter` (= π/2 = 90° 고정).
            //   원본 ModelRotationRenderer prev 매 frame 자동 갱신 → 슬라이딩 phase 동안
            //     prev = Quarter 유지 → 다음 헤드점프 phase 진입 시 lerp = prev + (target -
            //     prev) * 0.2 ≈ Quarter (target 도 Quarter 부근, 큰 hDist 라 angle 작음) →
            //     모델 90° 수평 유지.
            //   우리 매핑은 비행/헤드점프와 분리된 smHeadJumpTiltX_prev 사용 → 슬라이딩
            //     분기에서 매 frame 직접 갱신 필요. 갱신 안 하면 prev reset 가드로 0 → 헤드점프
            //     phase 매번 0° 시작 → 큰 회전 변화.
            //   일반 슬라이딩 → standing → 헤드점프 시나리오 = standing 분기에서 prev=0 reset
            //     으로 자연 시작 (= 영향 X).
            sm.smHeadJumpTiltX_prev = (float) (Math.PI / 2f);
            sm.smHeadJumpFade_prevTime = animationProgress;
            // 🔴 fix #85 (2026-05-12, 사용자 보고 "슬라이딩 후 절벽 떨어짐 자동 cycle 시 여우무빙 시각"):
            //   슬라이딩 phase 진입 시 wasSelfSlideFire 무조건 false reset. 그 후 자동 cycle 매치 시
            //   wasSelfSlideFire=false 잔존 → 일반 헤드점프 시각.
            //   자체 발사 frame: tickEssential 안 L2271 자동 cycle 매치 *후* isSliding=false →
            //   setupTransforms 호출 시 슬라이딩 분기 매치 X → reset 안 됨 → wasSelfSlideFire=true 유지.
            sm.wasSelfSlideFire = false;
        }

        // 🔴 SM 엎드리기(isCrawling) — Phase 3 자식 효과 매핑 (2026-05-03):
        //   원본 SmartMovingModel L404: bipedTorso.rotateAngleX = Quarter - Thirtytwoth = 78.75°.
        //   원본 SmartMovingModel L405: bipedTorso.rotationPointY = 3F.
        //   원본 1.7.10 SR 모델 = bipedTorso 가 head/body/arm/leg 부모 → 회전/위치가 자식 모두 적용.
        //   1.21.1 평탄 모델 → entity 회전 + translate 로 모든 노드 일괄 처리.
        //   진입 가드: `(isCrawling && !isClimbing)` = 원본 isCrawl 진입 조건 (SmartMovingRender L75).
        // 🔴 부호 반전 (사용자 보고 fix — "배가 하늘 보고 있음", 2026-05-03):
        //   메모리 `feedback_render_scale_negation.md`: vanilla LivingEntityRenderer.render 의
        //   matrices.scale(-1,-1,1) 가 setupTransforms TAIL 다음에 적용 → POSITIVE_X.rotation(+θ)
        //   시각 결과 = R_x(-θ). 즉 +tiltAngle 입력 = 시각상 머리 뒤로 회전 (= 등이 땅, 배가 위).
        //   원본 isCrawling 의도 = 배가 땅 (엎드림) → 시각 R_x(+θ) 필요 → 입력 -θ 적용.
        //   isFlying 분기도 같은 부호 반전 사용 (메모리 BUG-31).
        if (sm.isCrawling && !sm.isClimbing) {
            float tiltAngle = (float)(Math.PI / 2 - Math.PI / 16);  // Quarter - Thirtytwoth = 78.75°
            // 🔴 회전 중심 정정 (사용자 보고 fix 2026-05-03 — "엎드리면 모델 몸통이 좀 더 뒤로 옴"):
            //   원본 SmartMovingModel L405 `bipedTorso.rotationPointY = 3F` → 원본 회전 중심
            //     = bipedTorso pivot = vanilla biped head pivot - 3F/16 (modelpart Y down).
            //     scale(-1,-1,1) 후 world Y +1.407 - 0.176 = entity.y + 1.231 (= 어깨 부근).
            //   이전 매핑 (matrices.translate(0, 1.5, 0)) → 회전 중심 = entity.y + 1.406 (= 머리 부근).
            //     17.5 cm 위쪽. 회전 후 몸통 vertex 가 더 큰 호 그리며 회전 → 몸통이 더 뒤쪽 이동.
            //   fix: pivotY = 1.5 - 3/16 = 1.3125 → 회전 중심 = entity.y + 1.231 = 원본 일치.
            //   하단 추가 translate(0, -3/16, 0) 는 모델 전체 위치 보정 (= 별도 효과) 유지.
            float pivotY = 1.5f - 3f / 16f;  // = 1.3125 = 21/16

            // 🔴 사용자 보고 fix (2026-05-03 — "팔/다리/몸통/머리 디테일 미세 차이"):
            //   원본 SmartMovingModel L406: bipedTorso.rotateAngleZ = cos(distance + Quarter) * Sixtyfourth * walkFactor.
            //   bipedTorso 는 head/body/arm/leg 모두의 부모 → Z 회전이 모든 자식에 영향.
            //   이전 매핑 = body.roll 만 적용 (= bipedBody 자체 회전) → head/arm/leg 미적용 → 차이.
            //   fix: matrices stack 의 R_z 추가 → 모든 자식 일괄 적용 (= 원본 부모 효과 1:1).
            //   원본 YZX rotationOrder + Y=0 → vertex 적용 = R_z first → R_x. matrices stack 적용
            //     순서 = matrices.rotate(R_x) → matrices.rotate(R_z) → vertex 적용 = R_z → R_x. 일치.
            //   sm_animateCrawling 의 body.roll 인자는 0 으로 변경 (= 자식 효과 중복 방지).
            float partialTicks = SmartMovingClientState.globalCachedTickDelta;
            float distance = sm.stats.getTotalHorizontalDistance(partialTicks) * 1.3f;
            // 🔴 (2026-05-03 crawl 디테일 fix): partial ticks lerp getter — sm_animateCrawling 동일.
            float speed = sm.stats.getCurrentHorizontalSpeedFlattened(partialTicks);
            float walkFactor = (speed >= 0.12951545f) ? 1f : (speed <= 0f ? 0f : speed / 0.12951545f);
            float zAngle = (float)(Math.cos(distance + Math.PI / 2) * (Math.PI / 64)) * walkFactor;

            matrices.translate(0f, pivotY, 0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(-tiltAngle));   // 부호 반전 (scale -1,-1,1 보정)
            matrices.multiply(RotationAxis.POSITIVE_Z.rotation(zAngle));        // L406 매핑 — 그대로 (scale commute)
            matrices.translate(0f, -pivotY, 0f);
            sm.smOuterTiltX = tiltAngle;
            // 🔴 bipedTorso.rotationPointY = 3F 매핑 — 부호 반전 (사용자 보고 fix — "여전히 살짝 떠있음",
            //   2026-05-03):
            //   원본 SmartMovingModel L405 의 +3F (= +3 px = +3/16 블록) 자식 효과를 우리 매핑에서
            //   matrices.translate 로 직접 적용 시 vanilla scale(-1,-1,1) 의 Y 부호 반전 영향:
            //   - 원본 model 좌표계 +3/16 (자식 효과) = world 시점 +3/16 위
            //   - 그러나 1.21.1 vanilla scale(-1,-1,1) 가 setupTransforms TAIL 후 적용 →
            //     TAIL inject 의 translate(0, +3/16, 0) 가 시각상 -3/16 효과 (= world 위로 +3/16
            //     **반대로** 시각 결과 - 사용자 보고 시각상 위로 0.1m 떠있음).
            //   해결: 메모리 `feedback_render_scale_negation.md` 패턴 = -3/16 입력 → 시각 +3/16
            //     원본 자식 효과와 일치 (= 모델 -3/16 아래로 보정 = 사용자 보고 fix).
            matrices.translate(0f, -3f / 16f, 0f);
        }

        // 🔴 (2026-05-04) crawl-climbing 옵션 D 정확 매핑 (사용자 명시 "옵션 D 거부 해제"):
        //   원본 SmartMovingModel L265 `bipedTorso.rotateAngleX = bodyAngleX` (= 부모 회전).
        //   bipedTorso = head/body/arm/shoulder/pelvic/leg 모두의 부모 → 모든 자식이 R_x(B) 누적.
        //   ModelPart 단일 노드로는 R_z(Z) 가 두 R_x 사이에 있는 구조 표현 X.
        //   해결: setupTransforms 의 root R_x(-bodyAngleX) (= 부호 반전 by scale) → 부모 효과 정확.
        //   setAngles 의 leg.pitch/leg.roll/body/head/arm 매핑 변경 (= 부모 + cancel = 원본 1:1).
        //   회전 중심 = bipedTorso pivot = modelpart (0, 0, 0) ≈ matrices stack (0, 1.5, 0).
        else if (sm.isCrawlClimbing) {
            float height = sm.smallOverGroundHeight + 0.25f;
            float bodyLength = 0.7f;
            float bodyAngleX_target;
            if (height < bodyLength) {
                bodyAngleX_target = Math.max(0f, (float) Math.acos(height / bodyLength));
            } else {
                bodyAngleX_target = 0f;
            }
            // 🔴 fade 보간 (2026-05-04 — 사용자 명시 "이 로직에 맞는 페이드"):
            //   첫 호출 = 즉시 target (= 진입 시 jump 허용 — 원본과 동일).
            //   그 후 매 frame height 변화 → target 변화 → lerp 부드러움 (= jitter 차단).
            //   원본 ModelRotationRenderer.GetIntermediateAngle 식 0.2 * deltaT 1:1.
            float bodyAngleX = sm.applyCrawlClimbFade(bodyAngleX_target, animationProgress);

            // 부모 R_x 매핑 (원본 L265 bipedTorso.X = bodyAngleX 1:1).
            // 부호 반전 (메모리 feedback_render_scale_negation.md): input -bodyAngleX → 시각 +bodyAngleX.
            matrices.translate(0f, 1.5f, 0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(-bodyAngleX));
            matrices.translate(0f, -1.5f, 0f);
        }
        // 🔴 fade reset (2026-05-04): isCrawlClimbing 종료 시 모든 fade prev field reset →
        //   다음 진입 시 첫 호출 = CRAWL_TILT_ANGLE 시작 (= 부드러운 전환).
        if (!sm.isCrawlClimbing) {
            sm.smCrawlClimbBodyAngleXFaded = Float.NaN;
            sm.smCrawlClimbFadeTimePrev = Float.NaN;
            sm.smCrawlClimbLegAngleXFaded = Float.NaN;
            sm.smCrawlClimbLegAngleXFadeTimePrev = Float.NaN;
            sm.smCrawlClimbLegAngleZFaded = Float.NaN;
            sm.smCrawlClimbLegAngleZFadeTimePrev = Float.NaN;
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
            float yawTarget = sm.smFlyingExtraYaw;

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
            sm.smCachedBodyYawLaggedDeg = yawLerpedDeg;
            sm.smStandardBodyYawPrev = yawLerpedDeg;
            sm.smStandardFadeTimePrev = animationProgress;
        }

        // isHeadJumping body X 기울기: θ = Quarter - currentVerticalAngle (원본 SmartMovingModel L508).
        // 비행 분기와 동일 패턴: head pivot 기준 회전 (translate ±1.5) + scale(-1,-1,1) 부호 반전 (-theta).
        //   feedback_rotation_pivot_pattern.md / feedback_render_scale_negation.md.
        // 🔴 fade lerp (2026-05-11, 사용자 보고 "하강 시 몸 너무 수직, 원본 더 수평적"):
        //   원본 SmartMovingModel L507 `bipedOuter.fadeRotateAngleX = true` (= 5-tick lerp) 1:1.
        //   ModelRotationRenderer.GetIntermediateAngle = prev + (target - prev) * deltaT * 0.2F.
        //   누락 시 빠른 각도 변화 (10°/tick) → 모델 거의 거꾸로 (164°). fade 적용 시 130°.
        //   비행 prev 인프라와 분리 (= smHeadJumpTiltX_prev) — 비행 ↔ 헤드점프 전환 시 leak 차단.
        // 🔵 (2026-05-14, fix #98 위치 기반) smRemoteHJVisualHold OR 추가 — REMOTE 공중 자세 hold.
        if (sm.isHeadJumping || sm.smRemoteHJVisualHold) {
            // 🔴 fix #81 (2026-05-12, 사용자 보고 "여우무빙 진입 시 몸 수평 안 됨"):
            //   자체 슬라이딩 발사 → 같은 tick L2271 자동 cycle 매치 → isHeadJumping=true 진입 시
            //   thetaTarget = Quarter 고정 강제 → 원본 isSlide 분기 (= rotateAngleX = Quarter)
            //   시각 1:1. 일반 헤드점프 (wasSelfSlideFire=false) 는 기존 식 그대로.
            // 🔴 fix #81 v2 (2026-05-12): prev 도 π/2 강제 set — fade lerp 즉시 도달.
            //   prev=0 시작 시 점진 lerp 12 tick 후도 도달 못 함 (= 사용자 시각 절반 회전 잔존).
            //   prev=π/2 강제 set 시 lerp = π/2 즉시 (= 발사 frame 부터 완전 수평).
            float thetaTarget = sm.wasSelfSlideFire
                    ? (float) Math.PI / 2f
                    : (float) Math.PI / 2f - sm.stats.currentVerticalAngle;
            if (sm.wasSelfSlideFire) {
                sm.smHeadJumpTiltX_prev = (float) Math.PI / 2f;
                sm.smHeadJumpFade_prevTime = animationProgress;
            }
            float thetaLerped = lerpFadeAngle(sm.smHeadJumpTiltX_prev, thetaTarget,
                                               sm.smHeadJumpFade_prevTime, animationProgress);
            matrices.translate(0f, 1.5f, 0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(-thetaLerped));
            matrices.translate(0f, -1.5f, 0f);
            sm.smHeadJumpTiltX_prev = thetaLerped;
            sm.smHeadJumpFade_prevTime = animationProgress;
            sm.smOuterTiltX = thetaLerped;   // cape 클램프 (B-17) 도 보간된 값 사용
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
            sm.smOuterExtraYaw_prev = (float) Math.toRadians(sm.smCachedBodyYawLaggedDeg);
            sm.smOuterFade_prevTime = animationProgress;
        }

        // 🔴 헤드점프 fade prev 매 frame 갱신 (= 비행 prev 갱신 패턴과 동일).
        //   헤드점프/슬라이딩 외 분기에서 prev=0 reset → 다음 헤드점프 진입 시 자연 시작점
        //   (= standing).
        //   슬라이딩 분기는 위 isSliding 분기에서 prev=Quarter 직접 set (여우무빙 자연 cycle).
        // 🔴 fix #81 v3 (2026-05-12, 사용자 보고 "슬라이딩→비행 전환 시 애니메이션 뒤틀림"):
        //   기존 wasSelfSlideFire reset = SmartMovingClientState L2222 의 헤드점프 종료 분기.
        //   비행 전환 시 onGround=false 라 매치 X → 플래그 잔존 → 다음 헤드점프 발사 시
        //   thetaTarget=π/2 잘못 강제 또는 비행 phase 시각 영향.
        //   해결: 헤드점프/슬라이딩 외 분기 진입 시 (= 비행/standing/낙하 등) 플래그 reset.
        // 🔵 (2026-05-14, fix #98 위치 기반) smRemoteHJVisualHold !hold AND 추가 — hold 중 reset 차단.
        if (!sm.isHeadJumping && !sm.isSliding && !sm.smRemoteHJVisualHold) {
            sm.smHeadJumpTiltX_prev = 0f;
            sm.smHeadJumpFade_prevTime = animationProgress;
            sm.wasSelfSlideFire = false;
        }

        // 🔴 천장 등반 fade prev 매 frame 갱신 (비행 prev 갱신 패턴과 동일).
        //   천장 등반 외 분기에서 prev = 직전 vanilla 또는 SM bodyYaw (라디안).
        //   진입 첫 frame fade 자연 시작 (이전값 → target lerp).
        if (!sm.isCeilingClimbing) {
            sm.smCeilingYaw_prev = (float) Math.toRadians(sm.smCachedBodyYawLaggedDeg);
            sm.smCeilingFade_prevTime = animationProgress;
        }

        // 🔴 fix #91 (2026-05-13): 헤드점프 fade prev 매 frame 갱신 (= 비행 prev 갱신 패턴 동일).
        //   헤드점프/RopeSliding 외 분기에서 prev = 직전 vanilla 또는 SM bodyYaw (라디안).
        //   진입 첫 frame fade 자연 시작 (= 직전 자세 → currentHorizontalAngle target lerp).
        //   메모리 feedback_fade_prev_pre_entry_pose.md 참조.
        // 🔴 fix #95 (2026-05-13): smHeadJumpYawSnapDone reset — 외 분기 진입 시 false.
        //   다음 헤드점프 진입 시 (= wasSelfSlideFire 매트릭스 매트릭스) 첫 frame snap 활성.
        // 🔵 (2026-05-14, fix #98 위치 기반) smRemoteHJVisualHold !hold AND 추가 — hold 중 reset 차단.
        if (!sm.isHeadJumping && !sm.isRopeSliding && !sm.smRemoteHJVisualHold) {
            sm.smHeadJumpYaw_prev = (float) Math.toRadians(sm.smCachedBodyYawLaggedDeg);
            sm.smHeadJumpYawFade_prevTime = animationProgress;
            sm.smHeadJumpYawSnapDone = false;
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
        // 🔴 (2026-05-05) self → Config.enabled / remote → 항상 true.
        if (!choco.ratel.smartmoving.client.SmartMovingClient.isSmRenderEnabled(entity)) return;

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
    /**
     * 🔵 (fix #115) vanilla LivingEntity.jumping (= raw space bar) 검사 헬퍼.
     *   LivingEntity.jumping = protected — 직접 access 불가. ClientPlayerEntity 의 경우
     *   input.jumping 으로 등가 (= ClientPlayerEntity.tickMovement 안 `jumping = input.jumping`).
     *   remote player 의 경우 isJumping field 동기화 X → false return (= 향후 packet sync 시 갱신).
     */
    private static boolean sm_isPlayerJumping(net.minecraft.client.network.AbstractClientPlayerEntity player) {
        if (player instanceof net.minecraft.client.network.ClientPlayerEntity localPlayer) {
            return localPlayer.input.jumping;
        }
        return false;
    }

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
