package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmModelPartOverride;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 슬라이딩 팔 회전 매핑 fix (2026-05-04):
 *   원본 1.7.10 YZX 매트릭스 = R_x(pitch) * R_z(roll) * R_y(yaw).
 *   1.21.1 ModelPart.rotate 는 ZYX hardcoded = R_z * R_y * R_x.
 *   원본 yaw=-π/2 정확히 ZYX gimbal lock 영역 → JOML getEulerAnglesZYX 분해
 *   부정확 → qDiff angle = π (정반대 회전) → 자세 가운데 모임 + 위아래 흔들림 X.
 *
 *   가이드라인 (memory feedback_zxy_zyx_rotation_order.md):
 *     "큰 yaw (±π/2 이상) 경로에선 quaternion 분해 헬퍼 사용 금지"
 *
 *   해결: ModelPart.rotate HEAD inject + cancel + 의도 q 직접 적용.
 *         sm_overrideQuat 가 null 이면 vanilla 동작 유지 (영향 없음).
 */
@Environment(EnvType.CLIENT)
@Mixin(ModelPart.class)
public abstract class MixinModelPart implements SmModelPartOverride {
    @Shadow public float pivotX;
    @Shadow public float pivotY;
    @Shadow public float pivotZ;
    @Shadow public float xScale;
    @Shadow public float yScale;
    @Shadow public float zScale;

    @Unique
    private Quaternionf sm_overrideQuat = null;

    @Inject(method = "rotate", at = @At("HEAD"), cancellable = true)
    private void sm_rotate_override(MatrixStack matrices, CallbackInfo ci) {
        if (sm_overrideQuat != null) {
            matrices.translate(this.pivotX / 16f, this.pivotY / 16f, this.pivotZ / 16f);
            matrices.multiply(sm_overrideQuat);
            if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
                matrices.scale(this.xScale, this.yScale, this.zScale);
            }
            ci.cancel();
        }
    }

    /**
     * vanilla PlayerEntityModel.setAngles 의 끝에서 sleeve.copyTransform(arm) 호출.
     * arm 의 override quat 도 sleeve 에 함께 복사 → outer layer 자세 일치.
     * (jacket/hat/pants 도 동일 패턴으로 자동 처리.)
     */
    @Inject(method = "copyTransform", at = @At("TAIL"))
    private void sm_copyTransform_quat(ModelPart other, CallbackInfo ci) {
        SmModelPartOverride src = (SmModelPartOverride)(Object) other;
        Quaternionf srcQ = src.sm_getOverrideQuat();
        this.sm_overrideQuat = (srcQ != null) ? new Quaternionf(srcQ) : null;
    }

    @Override
    public void sm_setOverrideQuat(Quaternionf q) {
        this.sm_overrideQuat = q;
    }

    @Override
    public void sm_clearOverrideQuat() {
        this.sm_overrideQuat = null;
    }

    @Override
    public boolean sm_hasOverrideQuat() {
        return sm_overrideQuat != null;
    }

    @Override
    public Quaternionf sm_getOverrideQuat() {
        return sm_overrideQuat;
    }
}
