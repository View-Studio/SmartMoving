package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 클라이언트측 블록 상호작용 — forceIsSneaking 오버라이드 훅.
 *
 * 원본 SmartMovingSelf 본체(SmartMovingSelf.md L2063-2071):
 *   beforeActivateBlockOrUseItem() { forceIsSneaking = isp.localIsSneaking(); }
 *   afterActivateBlockOrUseItem()  { forceIsSneaking = null; }
 *
 * `localIsSneaking()` = vanilla `isSneaking()` 값. 블록 상호작용 구간 동안 스니크 상태 고정 →
 * MixinEntityClient.sm_isSneaking 이 이 값을 강제 반환하여 다른 로직에 의한 변경 차단.
 *
 * 1.21.1 매핑: ItemInWorldManager.activateBlockOrUseItem(client side)
 *            → ClientPlayerInteractionManager.interactBlock.
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager {

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void sm_beforeInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                         CallbackInfoReturnable<ActionResult> cir) {
        // BUG-10 (세션 36): SM disabled 시 forceIsSneaking 강제 안 함 →
        //   sm_isSneaking() override 가 vanilla isSneaking() 결과 그대로 반환 (BUG-7 확장).
        if (!SmartMovingConfig.Config.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        sm.forceIsSneaking = player.isSneaking();
    }

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void sm_afterInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                        CallbackInfoReturnable<ActionResult> cir) {
        // BUG-10 (세션 36): RETURN 도 동일 가드 — disabled 시 null 강제 안 함 (이미 null 일 가능성).
        if (!SmartMovingConfig.Config.enabled) return;
        SmartMovingClientState.get(player).forceIsSneaking = null;
    }
}
