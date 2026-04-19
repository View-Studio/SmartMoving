package choco.ratel.smartmoving.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

public final class BlockUtil {

    private static final double PLAYER_HALF_WIDTH = 0.3;
    private static final double PLAYER_STAND_HEIGHT = 1.8;

    /**
     * 플레이어 발 위치에서 PLAYER_STAND_HEIGHT 높이의 박스를 검사.
     * 블록 충돌이 있으면 천장이 가로막혀 일어설 수 없는 상태 (mustCrawl).
     */
    public static boolean mustCrawl(PlayerEntity player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        Box standBox = new Box(
                x - PLAYER_HALF_WIDTH, y, z - PLAYER_HALF_WIDTH,
                x + PLAYER_HALF_WIDTH, y + PLAYER_STAND_HEIGHT, z + PLAYER_HALF_WIDTH
        );
        return player.getWorld().getBlockCollisions(player, standBox).iterator().hasNext();
    }

    private BlockUtil() {}
}
