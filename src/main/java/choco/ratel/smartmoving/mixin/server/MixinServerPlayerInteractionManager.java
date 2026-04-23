package choco.ratel.smartmoving.mixin.server;

import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 서버측 블록 상호작용 — forceIsSneaking 오버라이드 훅.
 *
 * 원본: SmartMovingCoreEventHandler가 ItemInWorldManager.activateBlockOrUseItem 시작/종료 시
 *       SmartMovingServer.beforeActivateBlockOrUseItem() / afterActivateBlockOrUseItem() 호출.
 *
 * 원본 SmartMovingServer 본체:
 *   beforeActivateBlockOrUseItem() { forceIsSneaking = isSneakButtonPressed; }
 *   afterActivateBlockOrUseItem()  { forceIsSneaking = null; }
 *
 * 1.21.1 매핑: ItemInWorldManager.activateBlockOrUseItem → ServerPlayerInteractionManager.interactBlock.
 * 블록 상호작용 구간 동안 MixinEntity.sm_isSneaking 가 클라이언트 스니크 버튼 상태를 강제 반환.
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class MixinServerPlayerInteractionManager {

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void sm_beforeInteractBlock(ServerPlayerEntity player, World world, ItemStack stack,
                                         Hand hand, BlockHitResult hitResult,
                                         CallbackInfoReturnable<ActionResult> cir) {
        SmartMovingServer sm = SmartMovingServer.get(player);
        sm.forceIsSneaking = sm.isSneakButtonPressed;
    }

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void sm_afterInteractBlock(ServerPlayerEntity player, World world, ItemStack stack,
                                        Hand hand, BlockHitResult hitResult,
                                        CallbackInfoReturnable<ActionResult> cir) {
        SmartMovingServer.get(player).forceIsSneaking = null;
    }
}
