package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 6-1 (클라이언트): getBaseDimensions() 커스텀 EntityDimensions 반환.
 * 6-4 (클라이언트): updatePose() Mixin — SM 상태별 포즈 강제 설정.
 *
 * getDimensions()는 final → getBaseDimensions()가 유일한 오버라이드 진입점.
 * Yarn: getBaseDimensions (intermediary: method_55694 확인 완료)
 * Yarn: updatePose (intermediary: method_7318 확인 완료)
 *
 * 서버 측 → MixinPlayerEntity.sm_getBaseDimensions_server(), sm_updatePose_server()
 */
@Mixin(PlayerEntity.class)
@Environment(EnvType.CLIENT)
public abstract class MixinPlayerEntityClient {

    /**
     * 6-1 (클라이언트): SM 포즈별 커스텀 EntityDimensions 반환.
     *
     * **1:1 번역 원칙** (세션 137): 원본 `setHeightOffset(-1F)` 시맨틱
     *   = `boundingBox.minY -= -1 = +1` + `height += -1` → bbox 키 1.8 → 0.8 축소
     *   = 원본 `getEyeHeight() = sp.height - 0.18F` → 0.62F
     *   → **모든 SM 활성 상태**: bbox 0.6 × 0.8 + eyeHeight 0.62F 로 통일.
     *
     * SM 활성 상태 (원본 setHeightOffset(-1F) 설정되는 경우):
     *   - isCrawling        (원본 L2829 crawl 진입 / L2798 climbCrawl 진입 등)
     *   - isClimbCrawling   (원본 L2798)
     *   - isHeadJumping     (원본 L2129 tryJump head)
     *   - isSliding         (원본 L2555 slide 직접 진입)
     *   - isSwimming_sm     (원본 L511 handleSwimming)
     *   - isDiving          (원본 L511)
     *   - isFlying (SM)     (원본 L2512 `isFlying && !wasFlying` 엣지)
     *   - isLevitating      (원본 L2519 `isLevitating && !wasLevitating` 엣지)
     *
     * 비활성 (heightOffset=0 유지, vanilla bbox):
     *   - isDipping: 수면 닿는 상태. 원본 setHeightOffset 없음 → STANDING 1.8 유지.
     *
     * POSE 자체는 `sm_updatePose_client` 에서 SWIMMING/SLIDING 중 적절한 것으로 가로챔.
     * dimensions 는 POSE 와 무관하게 SM 상태만 체크하여 반환 (원본 bbox 시맨틱 1:1).
     */
    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void sm_getBaseDimensions_client(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        // BUG-20 (세션 36): SM disabled 시 vanilla dimensions 그대로 사용.
        //   기존: sm.* 필드 (sm.isFlying/isLevitating 등) 조건만 검사 → sm.* 갱신 timing
        //   차이로 disabled 진입 첫 프레임에 잔존 true 시 비행 진입 0.6×0.8 bbox 강제 →
        //   vanilla 비행 motion 영향 (사용자 보고: 비행 진입 뚜둑).
        if (!SmartMovingConfig.Config.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);

        // 원본 setHeightOffset(-1F) 상태 전수 — 모두 0.6 × 0.8 + eyeHeight 0.62F.
        // isDipping 은 heightOffset=0 유지 (원본) → vanilla STANDING 통과.
        boolean smSmall = sm.isCrawling || sm.isClimbCrawling
                       || sm.isHeadJumping || sm.isSliding
                       || sm.isSwimming_sm || sm.isDiving
                       || sm.isFlying || sm.isLevitating;
        if (smSmall) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
            return;
        }

        // SLIDING POSE 가 vanilla 가 아닌 경로로 들어온 경우 (외부 모드 등) 보강 처리
        if (pose == EntityPose.SLIDING) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
        }
    }

    /**
     * 11-4 (클라이언트): SM 비행 비활성화 시 공중 이동 억제.
     *
     * vanilla: flying → flySpeed, !flying+sprinting → 0.026F, otherwise → 0.02F.
     * SM: flying && !cfg.fly → 0.05F (A-30 확인값).
     * 이 메서드를 0.05F로 제한하면 vanilla travel()의 공중 XZ 가속이 억제된다.
     */
    @Inject(method = "getOffGroundSpeed", at = @At("HEAD"), cancellable = true)
    private void sm_getOffGroundSpeed(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        // BUG-21 (세션 36): SM disabled 시 vanilla 공중 속도 그대로 사용.
        //   기존: cfg.fly 만 가드 → cfg.fly=false 사용자가 SM disabled 시 0.05F 강제 →
        //   vanilla 비행 속도 약화. 추가 cfg.enabled 가드로 SM disabled 시 vanilla 정상.
        if (!SmartMovingConfig.Config.enabled) return;
        if (player.getAbilities().flying && !SmartMovingConfig.Config.fly) {
            cir.setReturnValue(0.05F);
        }
    }

    /**
     * 6-4 (클라이언트): SM 이동 상태에서 vanilla updatePose() 취소 후 SM 포즈 강제 설정.
     *
     * **1:1 번역 원칙** (세션 137): 원본 SM 이 vanilla POSE 시스템 없던 시대 설계라
     * 상태별로 수동 bbox 조작. 1.21.1 는 POSE 기반 bbox 관리. SM 이 쓰는 상태별로 POSE 를
     * 독점 설정하여 vanilla 가 덮어쓰지 못하게 차단.
     *
     * 우선순위 매핑 (원본 heightOffset=-1F 활성 상태 전수):
     *   isCrawling / isClimbCrawling             → SWIMMING (엎드림 애니)
     *   isHeadJumping / isSliding                 → SLIDING  (납작 자세)
     *   isSwimming_sm / isDiving                  → SWIMMING (수영 애니)
     *   isFlying (SM) / isLevitating              → SLIDING  (비행 시 bbox 0.8 유지)
     *   isDipping                                 → STANDING (heightOffset=0, 수면 서있기)
     *   그 외                                     → vanilla updatePose() 통과 (elytra/trident 등 유지)
     *
     * dimensions 값 (0.6 × 0.8 + eyeHeight 0.62) 는 sm_getBaseDimensions_client 에서 전수 반영.
     */
    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void sm_updatePose_client(CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        // BUG-22 (세션 36): SM disabled 시 vanilla updatePose 그대로 — sm.* 잔존 timing
        //   영향 차단. 비행 진입 첫 프레임에 SLIDING POSE 강제 잔존 가능 → motion 영향.
        if (!SmartMovingConfig.Config.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);

        if (sm.isCrawling || sm.isClimbCrawling) {
            // 엎드림 자세 — SWIMMING POSE 재활용 (vanilla 수영 애니 공유)
            player.setPose(EntityPose.SWIMMING);
            ci.cancel();
        } else if (sm.isHeadJumping || sm.isSliding) {
            // 납작 자세 — SLIDING POSE
            player.setPose(EntityPose.SLIDING);
            ci.cancel();
        } else if (sm.isSwimming_sm || sm.isDiving) {
            // 수영/잠수 — SWIMMING POSE (vanilla 도 동일, 명시적 고정)
            player.setPose(EntityPose.SWIMMING);
            ci.cancel();
        } else if (sm.isFlying || sm.isLevitating) {
            // SM 비행 / levitate — 원본 setHeightOffset(-1F) 상태 (bbox 0.8)
            //   POSE 는 SLIDING 재활용 (0.6×0.8 동치). vanilla SWIMMING 대신 SLIDING 선택 이유:
            //   SWIMMING 은 수영 애니 트리거 → 비행 중 수영 애니는 부자연. SLIDING 이 시각
            //   정합성 더 나음.
            player.setPose(EntityPose.SLIDING);
            ci.cancel();
        }
        // isDipping / 그 외 → vanilla updatePose() 통과 (STANDING/CROUCHING/FALL_FLYING/SPIN_ATTACK)
    }

}
