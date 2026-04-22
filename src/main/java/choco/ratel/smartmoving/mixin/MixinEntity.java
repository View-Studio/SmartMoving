package choco.ratel.smartmoving.mixin;

import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 5-7: velocityDirty @Shadow — SM이 직접 속도 변경 시 서버 동기화 트리거.
 * 5-8: move() HEAD/TAIL 훅 — beforeMoveEntity / afterMoveEntity 이식.
 *
 * 참고: Entity.setVelocity()는 내부에서 velocityDirty=true를 자동 설정한다.
 *       SM이 setVelocity()를 통해 속도를 변경한다면 별도 설정 불필요.
 *       직접 velocity 벡터 필드를 조작하는 경우에만 수동 설정이 필요하다.
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

    /**
     * 5-7: Entity.velocityDirty (field_6007).
     * SM이 velocity를 직접 조작할 때 서버 동기화를 위해 true로 설정한다.
     * 사용처: SM 속도 계산 로직에서 setVelocity() 대신 직접 조작 시 참조.
     */
    @Shadow public boolean velocityDirty;

    /**
     * 5-8: beforeMove에서 STEP_HEIGHT=0으로 설정했을 때 원래 값을 저장한다.
     * -1.0 = 미설정 (beforeMove에서 STEP_HEIGHT를 변경하지 않았음).
     */
    @Unique
    private double sm_savedStepHeight = -1.0;

    /**
     * 5-8 before: beforeMoveEntity() 이식 (서버 측).
     *
     * SM 크롤링/천장 클라이밍 중 STEP_HEIGHT=0으로 설정.
     * 원본 ySize=0 (계단 오르기 억제)에 해당.
     *
     * 클라이언트 측 STEP_HEIGHT 억제: MixinEntityClient.sm_beforeMove_client()에서 구현됨.
     */
    /**
     * 3-4: isSneaking() 오버라이드 — 서버 플레이어 전용.
     * isSneaking()은 Entity에 선언되어 있어 ServerPlayerEntity에서 inject 불가.
     * Entity Mixin에서 instanceof 가드로 서버 플레이어에만 적용한다.
     *
     * 원본: SmartMovingServer.isSneaking(): forceIsSneaking != null ? forceIsSneaking : vanilla
     */
    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void sm_isSneaking(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);
        if (sm.forceIsSneaking != null) {
            cir.setReturnValue(sm.forceIsSneaking);
        }
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void sm_beforeMove(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);
        if (sm.isCrawling || sm.isCrawlClimbing || sm.isCeilingClimbing) {
            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
            if (attr != null) {
                sm_savedStepHeight = attr.getBaseValue();
                attr.setBaseValue(0.0);
            }
        }
    }

    /**
     * 5-8 after: afterMoveEntity() 이식 (서버 측).
     *
     * - STEP_HEIGHT 복원 (beforeMove에서 0으로 변경했던 경우)
     * - 클라이밍 이동 거리 누적 (피로도 계산용)
     *   isClimbing: 지면 접촉 중 → 1.2 배율
     *   isCrawlClimbing / isCeilingClimbing: 공중 → 0.9 배율
     *
     * heightOffset 위치 보정: MixinEntityClient.sm_afterMove_client()에서 구현됨.
     */
    @Inject(method = "move", at = @At("TAIL"))
    private void sm_afterMove(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        if (sm_savedStepHeight >= 0) {
            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
            if (attr != null) attr.setBaseValue(sm_savedStepHeight);
            sm_savedStepHeight = -1.0;
        }
        SmartMovingServer sm = SmartMovingServer.get(player);
        if (sm.isClimbing || sm.isCrawlClimbing || sm.isCeilingClimbing) {
            sm.distanceClimbedModified += movement.length() * (sm.isClimbing ? 1.2 : 0.9);
        }
    }
}
