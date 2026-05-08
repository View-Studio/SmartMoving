package choco.ratel.smartmoving.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Camera 의 cameraY / lastCameraY 필드 setter 노출.
 *
 * 원본 1.7.10: 카메라 위치 = posY + (yOffset 또는 1.62F). 보간 없음.
 * 1.21.1 vanilla: Camera.updateEyeHeight() 가 매 frame
 *     lastCameraY = cameraY;
 *     cameraY     = cameraY + (entity.getStandingEyeHeight() - cameraY) * 0.5F;
 * → 0.5 lerp 로 standingEyeHeight 추격.
 *
 * SmartMoving ICC EXIT 매핑 시 entity.y +=1, eyeHeight 1.62→0.62 즉시 변경.
 * 이 때 Camera.cameraY 가 이전 standing 1.62 에서 0.62 로 lerp 추격 → 1 frame 카메라
 * 위로 +0.5 점프 후 점차 하강 → 사용자 시점 덜컹.
 *
 * 해결: ICC toCrawling 직후 cameraY/lastCameraY 를 standingEyeHeight (= 0.62) 로 강제
 * → 다음 frame update 부터 보간 baseline 일치 → 점프 차단.
 *
 * 🔴 interface 형태 유지 — Mixin class 는 직접 cast 불가 (IllegalClassLoadError).
 *   @Inject 는 별도 MixinCameraDebug abstract class 에서 처리.
 */
@Mixin(Camera.class)
@Environment(EnvType.CLIENT)
public interface MixinCamera {

    @Accessor("cameraY")
    void sm_setCameraY(float cameraY);

    @Accessor("lastCameraY")
    void sm_setLastCameraY(float lastCameraY);

    @Accessor("cameraY")
    float sm_getCameraY();

    @Accessor("lastCameraY")
    float sm_getLastCameraY();
}
