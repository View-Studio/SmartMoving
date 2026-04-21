package choco.ratel.smartmoving.climbing;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;

/** 클라이밍 탐색 결과를 담는 데이터 클래스. 원본 Block(int)+Meta(int) 쌍을 BlockState 단일 객체로 대체. */
public class ClimbGap {
    /** null = 미설정 (원본 Block=-1/Meta=-1 대체) */
    public BlockState state;
    public boolean canStand;
    public boolean mustCrawl;
    public Direction direction;
    public boolean skipGaps;

    public void reset() {
        state = null;
        canStand = false;
        mustCrawl = false;
        direction = null;
        skipGaps = false;
    }

    public void copyFrom(ClimbGap other) {
        this.state = other.state;
        this.canStand = other.canStand;
        this.mustCrawl = other.mustCrawl;
        this.direction = other.direction;
        this.skipGaps = other.skipGaps;
    }
}
