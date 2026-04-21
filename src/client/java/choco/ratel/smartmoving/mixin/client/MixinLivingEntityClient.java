package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 5-2: jump() 인터셉트 — jumpAvoided/jumpPending 세팅 + vanilla 취소
 * 5-3: jumpingCooldown 우회 — jump() 취소로 10틱 쿨다운 발동 자체 차단
 * 5-4: jumping 필드 @Shadow — SM 조건 필터 적용 시 참조 (tickEssential 내부)
 * 5-5 (클라이언트): isClimbing() 오버라이드 — SM 클라이밍 중 false 반환
 * 5-6: applyClimbingSpeed() 취소 — SM 클라이밍 중 x/z ±0.15F 클램프 이중 차단
 * 6-2 (클라이언트): isInSwimmingPose() 오버라이드 — SM 크롤링 중 false 반환
 */
@Mixin(LivingEntity.class)
@Environment(EnvType.CLIENT)
public abstract class MixinLivingEntityClient {

    /**
     * 5-4: LivingEntity.jumping (field_6282).
     * SM 조건 필터가 적용될 때 이 필드를 false로 강제 설정한다.
     *
     * 필터 조건 (tickEssential 내에서 적용):
     *   isCrawling || isSliding || isHeadJumpCharging || isJumpCharging
     *   || blockJumpTillButtonRelease
     *
     * TODO Phase 10: tickEssential()에서 조건 체크 후 this.jumping = false 호출
     */
    @Shadow protected boolean jumping;

    /**
     * 5-2 + 5-3: jump() 인터셉트.
     *
     * ClientPlayerEntity에 대해서만 동작한다.
     * SM이 vanilla jump()를 차단하고 jumpPending 플래그를 세팅한다.
     * 실제 점프 처리는 tickEssential() → handleJumping() → tryJump()에서 수행된다.
     *
     * vanilla jump() 차단 부작용:
     *   - addExhaustion(0.05F * 2): SM 자체 소진 시스템으로 대체 (TODO Phase 10)
     *   - Stats.JUMP 통계: SM tryJump()에서 별도 기록 (TODO Phase 10)
     *   - 스프린트 점프 +0.2F: SM tryJump()에서 자체 계산 (TODO Phase 10)
     *   - jumpingCooldown=10 (5-3): jump() 미실행으로 자동 차단
     *
     * ⚠️ Phase 10 (tryJump) 구현 전까지 점프가 비활성화됨.
     */
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void sm_jump(CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        sm.jumpAvoided = true;
        sm.jumpPending = true;
        ci.cancel();
    }

    /**
     * 5-5 (클라이언트): SM 커스텀 클라이밍 중 isClimbing() = false 강제.
     *
     * 목적 1: applyClimbingSpeed() x/z ±0.15F 클램프 차단
     * 목적 2: applyMovementInput()에서 isClimbing() 시 y=0.2 강제 적용 차단
     *
     * ClientPlayerEntity 인스턴스 확인 → SmartMovingClientState 상태 기반 판정.
     * 서버 측 동등 로직 → MixinLivingEntity.sm_isClimbing_server()
     */
    @Inject(method = "isClimbing", at = @At("HEAD"), cancellable = true)
    private void sm_isClimbing_client(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isClimbing || sm.isCrawlClimbing || sm.isCeilingClimbing) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 5-6: applyClimbingSpeed() 이중 차단 — SM 클라이밍 시 Vec3d 그대로 반환.
     * Yarn: applyClimbingSpeed (intermediary: method_18801 확인 완료)
     *
     * 5-5(isClimbing=false)로 이미 applyClimbingSpeed 내부 클램프가 차단되지만,
     * applyClimbingSpeed가 isClimbing()을 우회해 직접 호출되는 경로 대비 이중 방어.
     */
    @Inject(method = "applyClimbingSpeed", at = @At("HEAD"), cancellable = true)
    private void sm_applyClimbingSpeed(Vec3d velocity, CallbackInfoReturnable<Vec3d> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isClimbing || sm.isCrawlClimbing || sm.isCeilingClimbing) {
            cir.setReturnValue(velocity);
        }
    }

    /**
     * 6-2 (클라이언트): SM 크롤링 중 isInSwimmingPose() = false 강제.
     *
     * SM 크롤링은 SWIMMING 포즈를 사용하지만, vanilla isInSwimmingPose()=true가 되면:
     *   - setupTransforms Branch 2: X-90° 회전 + translate(0,-1,0.3) → 렌더 깨짐
     *   - updateLeaningPitch(): leaningPitch가 1.0까지 상승 → 자동 회전 발생
     *   - animateModel(): model.leaningPitch 반영 → setAngles 전 pitch 왜곡
     *
     * 클라이언트 측: ClientPlayerEntity 인스턴스 확인.
     * 서버 측 → MixinLivingEntity.sm_isInSwimmingPose_server()
     * Yarn: isInSwimmingPose (intermediary: method_20232 확인 완료)
     */
    @Inject(method = "isInSwimmingPose", at = @At("HEAD"), cancellable = true)
    private void sm_isInSwimmingPose_client(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isCrawling || sm.isCrawlClimbing) {
            cir.setReturnValue(false);
        }
    }
}
