package choco.ratel.smartmoving.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
     * 5-8 before: beforeMoveEntity() 이식.
     *
     * move() 실행 전:
     *   - STEP_HEIGHT 속성을 0으로 설정 (isSneaking/isCrawling 등 조건 시)
     *     → 원본 ySize=0 (계단 오르기 억제)에 해당
     *     → EntityAttributes.GENERIC_STEP_HEIGHT 속성으로 대응
     *
     * 플레이어 엔티티에만 적용. 비플레이어는 그대로 통과.
     *
     * TODO Phase 7: STEP_HEIGHT=0 설정 조건 구현
     *   (5-11-5 STEP_HEIGHT 항목과 연동)
     */
    @Inject(method = "move", at = @At("HEAD"))
    private void sm_beforeMove(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity)) return;
        // TODO Phase 7: SM 이동 상태(이건 스니킹/크롤링 등) 확인 후 STEP_HEIGHT=0 설정
        // EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
        // if (attr != null && smNeedsStepSuppression(player)) attr.setBaseValue(0.0);
    }

    /**
     * 5-8 after: afterMoveEntity() 이식.
     *
     * move() 실행 후:
     *   - STEP_HEIGHT 속성 복원 (원래 값으로 되돌림)
     *   - heightOffset > 0 시 player.setPos()로 Y 위치 수동 보정
     *   - 클라이밍 이동 거리 누적 (피로도 계산용: 지상 1.2×, 공중 0.9×)
     *   - 수영 소리 누적 (SwimSoundDistance > 1.0D 시 재생)
     *
     * TODO Phase 7: 각 항목 구현
     */
    @Inject(method = "move", at = @At("TAIL"))
    private void sm_afterMove(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity)) return;
        // TODO Phase 7: STEP_HEIGHT 복원
        // TODO Phase 7: heightOffset 위치 보정 (setPos)
        // TODO Phase 7: 클라이밍 이동 거리 누적
        // TODO Phase 7: 수영 소리 누적
    }
}
