package choco.ratel.smartmoving.climbing;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TrapdoorBlock;

/**
 * 1-6: 천장 클라이밍 가능 블록 판정.
 * 원본: SmartMovingBase.supportsCeilingClimbing() — Block ID/Meta → BlockState 기반으로 재설계.
 *
 * 기본 지원 블록 (원본 _ceilingClimbConfigurationString 기본값 이식):
 *   tile.fenceIron → minecraft:iron_bars
 *   tile.trapdoor/0/1/2/3 → 모든 목재 트랩도어 (OPEN=false, 닫힌 상태)
 *   tile.trapdoor_iron/0/1/2/3 → minecraft:iron_trapdoor (OPEN=false)
 *
 * 원본 meta 0/1/2/3 = 닫힌 상태(각도 방향), meta 4-7 = 열린 상태.
 * 1.21.1: TrapdoorBlock.OPEN = false 이면 닫힌 상태.
 *
 * 설정 파일 기반 커스텀 블록 추가는 미확인 (추가 리서치 필요).
 * TODO: config string 파싱 시스템 재구현 시 이 메서드에 설정 기반 블록 추가.
 */
public final class CeilingClimbBlocks {

    private CeilingClimbBlocks() {}

    /**
     * 해당 BlockState가 천장 클라이밍을 지원하는 블록인지 판정한다.
     *
     * @param state 판정할 BlockState (null이면 false)
     * @return 천장 클라이밍 가능 여부
     */
    public static boolean supports(BlockState state) {
        if (state == null) return false;

        // tile.fenceIron → minecraft:iron_bars: 모든 상태 허용
        if (state.isOf(Blocks.IRON_BARS)) return true;

        // 🔴 사용자 요구: fence ≡ iron_bars 동일 동작. 천장 클라이밍도 fence 추가.
        //   원본은 fence 천장 클라이밍 미지원 (_ceilingClimbConfigurationString 기본값에
        //   tile.fenceIron 만 포함) 이지만 사용자 의도 "두 블록 동일 작동" 우선.
        //   isFence(state) = FenceBlock || WallBlock || closed FenceGate.
        if (Orientation.isFence(state)) return true;

        // tile.trapdoor/trapdoor_iron → TrapdoorBlock 계열: 닫혀있는 상태만 허용
        // 원본 meta 0-3 = OPEN=false에 해당
        if (state.getBlock() instanceof TrapdoorBlock) {
            return !state.get(TrapdoorBlock.OPEN);
        }

        return false;
    }
}
