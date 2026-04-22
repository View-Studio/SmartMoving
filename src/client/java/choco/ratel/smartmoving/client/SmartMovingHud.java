package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/**
 * SM 전용 HUD 렌더링.
 * 원본: SmartMovingRender.renderGuiIngame() — jumpCharge 바 + exhaustion 바
 *
 * 포함 항목:
 *   10-6: jumpCharge 바 — 왼쪽, 화면 중앙-91 기준, icons.png drawIcon(2,0)/drawIcon(3,0)
 *   C-33: exhaustion 바 — 오른쪽, 화면 중앙+90 기준, icons.png drawIcon(0,0)/drawIcon(1,0)
 *
 * 아이콘 레이아웃 (icons.png 256×256, 9×9 격자):
 *   (0,0) full fitness  (1,0) half fitness
 *   (2,0) full charge   (3,0) half charge
 */
@Environment(EnvType.CLIENT)
public final class SmartMovingHud {

    private static final Identifier ICONS = Identifier.of("smartmoving", "gui/icons.png");
    private static final int ICON_SIZE = 9;

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

        if (!cfg.enabled) return;

        // ── 점프 차지 바 ──────────────────────────────────────────────────
        // 원본: stillJumpCharge = sm.jumpCharge, runJumpCharge = sm.headJumpCharge
        // 큰 쪽을 표시하고 해당 maximum을 기준으로 삼음
        float maxStillCharge = cfg.jumpChargeMaximum;
        float maxRunCharge   = cfg.headJumpChargeMaximum;
        float stillCharge    = Math.min(sm.jumpCharge,     maxStillCharge);
        float runCharge      = Math.min(sm.headJumpCharge, maxRunCharge);
        boolean drawJump     = stillCharge > 0 || runCharge > 0;

        // ── 소진 바 ───────────────────────────────────────────────────────
        // 원본: maxExhaustion = climbExhaustionStop, 표시 조건 exhaustion > 0
        float maxExhaustion   = cfg.climbExhaustionStop;
        float exhaustion      = Math.min(sm.exhaustion, maxExhaustion);
        boolean drawExhaustion = exhaustion > 0 && exhaustion <= maxExhaustion;

        if (!drawJump && !drawExhaustion) return;

        int width  = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        if (drawJump) {
            float maxCharge = stillCharge > runCharge ? maxStillCharge : maxRunCharge;
            float charge    = Math.max(stillCharge, runCharge);

            // 원본: max 시 fulls=10 half=0, 그 외 ceil 공식
            boolean maxed = charge >= maxCharge;
            int fulls = maxed ? 10 : Math.max(0, (int) Math.ceil(((double)(charge - 2) * 10D) / maxCharge));
            int half  = maxed ? 0  : Math.max(0, (int) Math.ceil((double)charge * 10D / maxCharge) - fulls);

            // Y: height - 49. 갑옷 있으면 10 위로
            int jy = height - 49 - (player.getArmor() > 0 ? 10 : 0);

            for (int i = 0; i < fulls + half; i++) {
                drawIcon(context, (width / 2 - 91) + i * 8, jy, i < fulls ? 2 : 3, 0);
            }
        }

        if (drawExhaustion) {
            // fitness = 남은 여유분 (낮을수록 피로가 쌓인 것)
            float fitness = maxExhaustion - exhaustion;
            int halfs = (int) Math.floor(fitness / maxExhaustion * 21F);
            int fulls = halfs / 2;
            int half  = halfs % 2;

            // Y: height - 49. 물속이면 10 위로
            int jy = height - 49 - (player.isTouchingWater() ? 10 : 0);

            for (int i = 0; i < Math.min(fulls + half, 10); i++) {
                drawIcon(context, (width / 2 + 90) - (i + 1) * 8, jy, i < fulls ? 0 : 1, 0);
            }
        }
    }

    // icons.png에서 (iconX*9, iconY*9) 위치의 9×9 스프라이트를 (x, y)에 렌더.
    // DrawContext.drawTexture 7-인자 형태: textureWidth/Height = 256 자동 가정.
    private static void drawIcon(DrawContext context, int x, int y, int iconX, int iconY) {
        context.drawTexture(ICONS, x, y, iconX * ICON_SIZE, iconY * ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }
}
