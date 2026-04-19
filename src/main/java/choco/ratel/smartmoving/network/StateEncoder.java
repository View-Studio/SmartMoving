package choco.ratel.smartmoving.network;

import choco.ratel.smartmoving.state.SmartMovingState;

/**
 * SmartMovingState ↔ 64비트 long 인코딩/디코딩.
 * 비트 레이아웃: research_networking.md 섹션 B 참조.
 */
public final class StateEncoder {

    // ── 인코딩 ────────────────────────────────────────────────────────────────

    public static long encode(SmartMovingState state) {
        long s = 0L;

        // 비트 0-3: feetClimbingType (4비트)
        s |= ((long)(state.feetClimbingType + 3) & 0xFL);
        // 비트 4-7: handsClimbingType (4비트)
        s |= ((long)(state.handsClimbingType + 3) & 0xFL) << 4;

        // 비트 8-33: boolean 순서 (원본 SmartMovingOther 디코딩 순서)
        s = setBit(s,  8, false);                      // isJumping (미사용)
        s = setBit(s,  9, state.isDiving);
        s = setBit(s, 10, state.isDipping);
        s = setBit(s, 11, state.isSwimming);
        s = setBit(s, 12, state.isCrawlClimbing);
        s = setBit(s, 13, state.isCrawling);
        s = setBit(s, 14, state.isClimbing);
        s = setBit(s, 15, state.isSmall());            // isSmall
        s = setBit(s, 16, false);                      // doFallingAnimation
        s = setBit(s, 17, state.isAerodynamic);
        s = setBit(s, 18, state.isCeilingClimbing);
        s = setBit(s, 19, state.isLevitating);
        s = setBit(s, 20, state.isHeadJumping);
        s = setBit(s, 21, state.isSliding);
        // 비트 22-24: angleJumpType (3비트)
        s |= ((long)(state.angleJumpType & 0x7)) << 22;
        s = setBit(s, 25, state.isFeetVineClimbing);
        s = setBit(s, 26, state.isHandsVineClimbing);
        s = setBit(s, 27, false);                      // isClimbJumping
        s = setBit(s, 28, false);                      // isClimbBackJumping
        s = setBit(s, 29, state.isSlow);
        s = setBit(s, 30, state.isFast);
        s = setBit(s, 31, state.isWallJumping);
        s = setBit(s, 32, state.isRopeSliding);
        s = setBit(s, 33, state.sneakButton.pressed);  // isSneakButtonPressed

        return s;
    }

    // ── 디코딩 ────────────────────────────────────────────────────────────────

    public static void decode(long s, SmartMovingState state) {
        state.feetClimbingType  = (int)((s & 0xFL)) - 3;
        state.handsClimbingType = (int)((s >>> 4) & 0xFL) - 3;

        state.isDiving          = getBit(s,  9);
        state.isDipping         = getBit(s, 10);
        state.isSwimming        = getBit(s, 11);
        state.isCrawlClimbing   = getBit(s, 12);
        state.isCrawling        = getBit(s, 13);
        state.isClimbing        = getBit(s, 14);
        // isSmall(15)은 서버 측 히트박스에서만 사용
        state.isAerodynamic     = getBit(s, 17);
        state.isCeilingClimbing = getBit(s, 18);
        state.isLevitating      = getBit(s, 19);
        state.isHeadJumping     = getBit(s, 20);
        state.isSliding         = getBit(s, 21);
        state.angleJumpType     = (int)((s >>> 22) & 0x7L);
        state.isFeetVineClimbing  = getBit(s, 25);
        state.isHandsVineClimbing = getBit(s, 26);
        state.isSlow            = getBit(s, 29);
        state.isFast            = getBit(s, 30);
        state.isWallJumping     = getBit(s, 31);
        state.isRopeSliding     = getBit(s, 32);
    }

    private static long setBit(long s, int bit, boolean value) {
        if (value) return s | (1L << bit);
        return s & ~(1L << bit);
    }

    private static boolean getBit(long s, int bit) {
        return ((s >>> bit) & 1L) != 0L;
    }

    private StateEncoder() {}
}
