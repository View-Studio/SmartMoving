package choco.ratel.smartmoving.physics;

import choco.ratel.smartmoving.config.ConfigManager;
import choco.ratel.smartmoving.state.SmartMovingState;
import choco.ratel.smartmoving.util.BlockUtil;
import net.minecraft.entity.player.PlayerEntity;

public final class CrawlingHandler {

    public static float CRAWL_SPEED_FACTOR = 0.35F;

    static float getMaxFallDistance() {
        return ConfigManager.INSTANCE.fallingDistanceMinimum;
    }

    /**
     * 매 틱 기어가기 상태를 갱신한다.
     * SmartMovingState.tick() 에서 호출.
     */
    public static void update(SmartMovingState state, PlayerEntity player) {
        boolean mustCrawl = state.isCrawling && BlockUtil.mustCrawl(player);
        boolean canCrawl  = canCrawl(state, player);
        boolean wantCrawl = wantCrawl(state, player);

        state.wasCrawling = state.isCrawling;
        state.isCrawling  = canCrawl && (wantCrawl || mustCrawl);

        if (state.isCrawling && !state.wasCrawling) {
            onEnterCrawl(state);
        }

        // 크롤 중 + 그랩 + 전진 + 수평 충돌 → 크롤-클라이밍 의도 플래그
        state.wantCrawlNotClimb =
                (state.wantCrawlNotClimb || (state.grabButton.startPressed && !state.wasCrawling))
                && state.grabButton.pressed
                && state.forwardButton.pressed
                && state.isCrawling
                && player.horizontalCollision;

        if (state.isCrawling) {
            state.isSlow = true;
        }
    }

    private static boolean mustCrawl(SmartMovingState state, PlayerEntity player) {
        return state.isCrawling && BlockUtil.mustCrawl(player);
    }

    private static boolean canCrawl(SmartMovingState state, PlayerEntity player) {
        return !player.getAbilities().flying
                && !state.isSwimming
                && !state.isDiving
                && !state.isClimbing
                && player.fallDistance < getMaxFallDistance();
    }

    private static boolean wantCrawl(SmartMovingState state, PlayerEntity player) {
        boolean continueCrawl = state.isCrawling
                && (state.sneakButton.pressed || state.crawlToggled);
        boolean startCrawl = state.grabButton.startPressed
                && state.sneakButton.pressed
                && player.isOnGround();

        // 토글 진입: grab+sneak 시작 시 토글 상태 반전
        if (startCrawl) {
            state.crawlToggled = !state.isCrawling;
        }

        // 점프하면 토글 해제
        if (state.jumpButton.startPressed) {
            state.crawlToggled = false;
        }

        return continueCrawl || startCrawl;
    }

    private static void onEnterCrawl(SmartMovingState state) {
    }

    private CrawlingHandler() {}
}
