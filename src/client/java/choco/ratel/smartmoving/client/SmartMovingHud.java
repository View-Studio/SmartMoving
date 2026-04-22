package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;

/**
 * SM 전용 HUD 렌더링.
 * 원본: SmartMovingRender.renderGuiIngame() — jumpCharge 바 + exhaustion 바
 *
 * 포함 항목:
 *   10-6: jumpCharge 바 (초록) + headJumpCharge 바 (파랑) 화면 하단 중앙 렌더
 */
@Environment(EnvType.CLIENT)
public final class SmartMovingHud {

    private SmartMovingHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(SmartMovingHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        SmartMovingClientState sm = SmartMovingClientState.get(player);
        SmartMovingConfig cfg = SmartMovingConfig.Config;

        boolean hasCharge     = sm.jumpCharge > 0;
        boolean hasHeadCharge = sm.headJumpCharge > 0;
        if (!hasCharge && !hasHeadCharge) return;

        int screenWidth  = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        int centerX = screenWidth / 2;
        int baseY   = screenHeight - 20;

        // jumpCharge 바 (초록색)
        if (hasCharge) {
            float ratio = cfg.jumpChargeMaximum > 0
                    ? sm.jumpCharge / cfg.jumpChargeMaximum : 0F;
            int barWidth = (int) (ratio * 100);
            context.fill(centerX - 50, baseY, centerX - 50 + barWidth, baseY + 4, 0xFF00FF00);
        }

        // headJumpCharge 바 (파란색)
        if (hasHeadCharge) {
            float ratio = cfg.headJumpChargeMaximum > 0
                    ? sm.headJumpCharge / cfg.headJumpChargeMaximum : 0F;
            int barWidth = (int) (ratio * 100);
            context.fill(centerX - 50, baseY - 6, centerX - 50 + barWidth, baseY - 2, 0xFF0000FF);
        }
    }
}
