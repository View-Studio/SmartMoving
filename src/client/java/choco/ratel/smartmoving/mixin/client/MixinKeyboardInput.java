package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 🔴 사다리/덩굴 등반 + W + sneak 시 input.sneaking 차단.
 *
 * 사용자 의도: "사다리 + W + sneak → 아무 동작 없이 그냥 등반".
 *
 * 이전 시도 (실패):
 *   1. ClientPlayerEntity.shouldEnterCrouchingPose mixin — method 없음.
 *   2. LivingEntity.shouldEnterCrouchingPose mixin — yarn mapping 다름.
 *   3. Entity.setSneaking mixin — 사용자 보고 효과 없음 (vanilla 자세 결정이 다른 source 사용).
 *
 * 본질: vanilla 자세 결정이 정확히 어떤 method/source 를 사용하는지 mapping 모름.
 * → 가장 근본인 KeyboardInput.tick 에서 sneaking 갱신 직후 강제 false 로 덮어씀.
 *   vanilla 의 모든 후속 처리 (자세, isSneaking, setSneaking, applyClimbingSpeed 등)
 *   가 input.sneaking 을 source 로 사용하면 일괄 차단.
 *
 * 가드: sm.isClimbing && forward > 0F. W 안 누름 케이스는 정상 sneak hold 유지.
 */
@Mixin(KeyboardInput.class)
@Environment(EnvType.CLIENT)
public abstract class MixinKeyboardInput {

    @Inject(method = "tick(ZF)V", at = @At("TAIL"))
    private void sm_blockSneakInClimb(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        if (!SmartMovingConfig.Config.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        Input self = (Input) (Object) this;
        // 🔴 등반 중 sneak 누름 추적 + 등반 끝난 후에도 sneak 키 떼기 전까지 차단.
        //   사용자 의도: 사다리 등반 중 sneak 누르고 있다가 등반 끝나도 vanilla CROUCHING
        //   자세 발동 X. sneak 키 한 번 떼고 다시 누르면 정상 활성화.
        //   원본 isSneaking() override 는 등반 직후 sneak 자세 허용 (1.7.10 자세 시각 미미)
        //   하지만 1.21.1 CROUCHING 은 두드러져서 사용자 인식 → 추가 보정.
        if (sm.isClimbing && self.sneaking) {
            sm.sneakHeldDuringClimb = true;
        }
        if (!self.sneaking) {
            sm.sneakHeldDuringClimb = false;
        }
        if ((sm.isClimbing || sm.sneakHeldDuringClimb) && self.movementForward > 0F) {
            self.sneaking = false;
        }
    }
}
