package choco.ratel.smartmoving.mixin;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
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
     * Entity.dimensions — 현재 EntityDimensions (width/height/eyeHeight).
     * sm_offsetBoundingBoxForFlying 의 가드 신호.
     */
    @Shadow private EntityDimensions dimensions;

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

    /**
     * 비행 콜리전 — 사용자 매핑 (단계 4 복구):
     *   원본 SmartMovingSelf.setHeightOffset(-1F) (L1694-L1704):
     *     boundingBox.minY -= heightOffset = +1 (박스 1블록 위로)
     *     height += heightOffset = -1 (1.8 → 0.8)
     *
     *   1.21.1 매핑:
     *   - sm_getBaseDimensions inject: 비행 시 dimensions=(0.6, 0.8, 1.62) (height 0.8).
     *   - 본 mixin: 비행 시 calculateBoundingBox 결과 box.offset(0, 1, 0) — 박스 +1 위로.
     *   - 결과: 박스 = (entity.y+1, entity.y+1+0.8) = (entity.y+1, entity.y+1.8).
     *
     *   가드: dimensions.height < 1 && eyeHeight > 1 — 비행/Levitate 만 (다른 small SM 은
     *   eyeHeight 0.62 라 미통과). PlayerEntity 만.
     *   abilities=null 가드: PlayerEntity 생성자가 abilities 초기화 전 setPosition 호출 가능.
     */
    @Inject(method = "calculateBoundingBox", at = @At("RETURN"), cancellable = true)
    private void sm_offsetBoundingBoxForFlying(CallbackInfoReturnable<Box> cir) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!SmartMovingConfig.Config.enabled) return;
        if (player.getAbilities() == null) return;

        EntityDimensions dim = this.dimensions;
        if (dim.height() >= 1.0F) return;       // STANDING / 일반 height
        // 🔴 fix #56 (2026-05-10, 사용자 보고 "fix #53 후 박스 낮음 + 벽박힘 + 진입 어려움"):
        //   기존 가드 `eye <= 1.0F` 만 = isHeadJumping (eye=1.62) 시점만 mixin offset 활성.
        //   fix #53 (= 1.12.2 1:1) 진입 시 isHeadJumping=false + isSliding=true 강제 → dim eye=0.62
        //   → mixin offset 차단 → 박스 발 = entity.y = 원래 ground - 1m → 박힘.
        //
        //   1.7.10/1.12.2 의 `setHeightOffset(-1)` 식 = boundingBox.minY += 1m **직접 변경**
        //   (POSE 무관, isSliding/isHeadJumping 시 양쪽 적용). vanilla 1.21.1 매핑에서는 mixin
        //   offset 으로 등가 처리하는데 가드가 `eye>1` 만 검사 → isSliding 시 미적용 BUG.
        //
        //   해결: POSE.SLIDING 시도 mixin offset 활성. POSE.SLIDING 분기:
        //     - sm_updatePose 식: isHeadJumping || isSliding || isFlying || isLevitating → SLIDING.
        //   → 모든 SLIDING POSE 에서 박스 +1m up. 1.7.10/1.12.2 setHeightOffset 효과 1:1.
        //   부작용: 일반 슬라이딩 시도 박스 +1m up — 1.12.2 동등 동작 (= 부작용 X).
        EntityPose pose = ((Entity) (Object) this).getPose();
        if (dim.eyeHeight() <= 1.0F && pose != EntityPose.SLIDING) return;

        Box original = cir.getReturnValue();
        cir.setReturnValue(original.offset(0.0, 1.0, 0.0));
        // 🔴 v26.6 — remote 측 timer override 는 client-side mixin 에서 추가 적용 (분리 사유:
        //   main mixin set 은 server 빌드 시 client class 참조 불가).
        //   client-side `MixinEntityClient.sm_iccTimerOffsetBb` 가 같은 method 에 RETURN inject.
    }

}
