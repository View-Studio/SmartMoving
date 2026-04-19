package choco.ratel.smartmoving.client.render;

import choco.ratel.smartmoving.config.ConfigManager;
import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

public final class SmartMovingHud {

    private static final Identifier ICONS = Identifier.of("smartmoving", "textures/gui/icons.png");
    private static final int ICON_SIZE = 9;

    public static void register() {
        HudRenderCallback.EVENT.register(SmartMovingHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        SmartMovingState state = client.player.getAttached(SmartMovingAttachments.STATE);
        if (state == null) return;

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        // 원본과 동일한 기준 Y (핫바 위)
        int baseY = screenH - 39 - 10;

        renderJumpChargeBar(context, client, state, screenW, baseY);
        renderExhaustionBar(context, client, state, screenW, baseY);
    }

    private static void renderJumpChargeBar(DrawContext ctx, MinecraftClient client,
                                             SmartMovingState state, int screenW, int baseY) {
        float maxStill = ConfigManager.INSTANCE.jumpChargeMaximum;
        float maxRun   = ConfigManager.INSTANCE.headJumpChargeMaximum;
        float still    = Math.min(state.jumpCharge, maxStill);
        float run      = Math.min(state.headJumpCharge, maxRun);
        if (still <= 0 && run <= 0) return;

        float maxCharge = still >= run ? maxStill : maxRun;
        float charge    = Math.max(still, run);
        boolean maxed   = charge >= maxCharge;

        int fulls = maxed ? 10 : (int) Math.ceil(((charge - 2) * 10D) / maxCharge);
        int half  = maxed ? 0  : (int) Math.ceil((charge * 10D) / maxCharge) - fulls;
        fulls = Math.max(0, fulls);
        half  = Math.max(0, Math.min(half, 1));

        int y = baseY - (client.player.getArmor() > 0 ? 10 : 0);
        for (int i = 0; i < fulls + half; i++) {
            int x = (screenW / 2 - 91) + i * 8;
            drawIcon(ctx, x, y, i < fulls ? 2 : 3, 0);
        }
    }

    private static void renderExhaustionBar(DrawContext ctx, MinecraftClient client,
                                             SmartMovingState state, int screenW, int baseY) {
        if (state.maxExhaustionForAction <= 0) return;

        float maxExhaustion = state.maxExhaustionForAction;
        float exhaustion    = Math.min(state.exhaustion, maxExhaustion);
        if (exhaustion <= 0) return;

        float fitness = maxExhaustion - exhaustion;
        // 원본: maxFitnessDrawn 기준으로 21 halfs 계산
        int halfs  = (int) Math.floor(fitness / maxExhaustion * 21F);
        int fulls  = halfs / 2;
        int half   = halfs % 2;

        boolean underwater = client.player.isSubmergedInWater();
        int y = baseY - (underwater ? 10 : 0);

        for (int i = 0; i < Math.min(fulls + half, 10); i++) {
            int x = (screenW / 2 + 90) - (i + 1) * 8;
            drawIcon(ctx, x, y, i < fulls ? 0 : 1, 0);
        }
    }

    private static void drawIcon(DrawContext ctx, int x, int y, int iconX, int iconY) {
        // icons.png는 256×256 기준. u/v는 픽셀 좌표.
        ctx.drawTexture(ICONS, x, y, iconX * ICON_SIZE, iconY * ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    private SmartMovingHud() {}
}
