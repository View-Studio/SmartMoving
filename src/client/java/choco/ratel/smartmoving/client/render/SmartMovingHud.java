package choco.ratel.smartmoving.client.render;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class SmartMovingHud {

    // 바 색상
    private static final int COLOR_CHARGE_BG  = 0x80000000; // 반투명 검정 배경
    private static final int COLOR_JUMP_CHARGE = 0xFF4FC3F7; // 하늘색 (점프 차지)
    private static final int COLOR_HEAD_CHARGE = 0xFFEF9A9A; // 연분홍 (헤드 점프 차지)
    private static final int COLOR_EXHAUSTION  = 0xFFFF7043; // 주황 (탈진)

    private static final int BAR_WIDTH  = 60;
    private static final int BAR_HEIGHT = 4;
    private static final int MARGIN     = 4;

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
        int y = screenH - 50;

        // 점프 차지 바 (화면 좌측 하단)
        if (state.jumpCharge > 0F) {
            float ratio = state.jumpCharge / 20F;
            drawBar(context, MARGIN, y, ratio, COLOR_JUMP_CHARGE, "Jump");
            y -= BAR_HEIGHT + MARGIN;
        }

        // 헤드 점프 차지 바
        if (state.headJumpCharge > 0F) {
            float ratio = state.headJumpCharge / 10F;
            drawBar(context, MARGIN, y, ratio, COLOR_HEAD_CHARGE, "Head");
            y -= BAR_HEIGHT + MARGIN;
        }

        // 탈진 바 (화면 우측 하단)
        if (state.exhaustion > 0F && state.maxExhaustionForAction > 0F) {
            float ratio = Math.min(state.exhaustion / state.maxExhaustionForAction, 1F);
            int x = screenW - BAR_WIDTH - MARGIN;
            drawBarNoLabel(context, x, screenH - 50, ratio, COLOR_EXHAUSTION);
        }
    }

    private static void drawBar(DrawContext ctx, int x, int y, float ratio, int color, String label) {
        // 배경
        ctx.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, COLOR_CHARGE_BG);
        // 채운 부분
        int filled = (int)(BAR_WIDTH * ratio);
        if (filled > 0) {
            ctx.fill(x, y, x + filled, y + BAR_HEIGHT, color);
        }
    }

    private static void drawBarNoLabel(DrawContext ctx, int x, int y, float ratio, int color) {
        ctx.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, COLOR_CHARGE_BG);
        int filled = (int)(BAR_WIDTH * ratio);
        if (filled > 0) {
            ctx.fill(x, y, x + filled, y + BAR_HEIGHT, color);
        }
    }

    private SmartMovingHud() {}
}
