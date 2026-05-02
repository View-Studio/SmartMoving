package choco.ratel.smartmoving.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * 클라이언트 렌더 스레드에서 공유되는 일시적 컨텍스트 플래그.
 *
 * 1.21.1 의 1인칭 손은 PlayerEntityRenderer.renderArm 안에서
 *   model.setAngles(player, 0,0,0,0,0)
 * 를 호출 → SM 의 sm_setAngles HEAD/TAIL inject 가 발동해 손/팔 자세를 SM 분기로
 * 덮어씀 → 1인칭 손이 사라지거나 이상한 자세로 보이는 BUG.
 *
 * renderArm HEAD/RETURN 에서 firstPersonArmRender 플래그를 set/clear → sm_setAngles
 * HEAD 와 TAIL 가 진입 직후 플래그 검사 후 early return → 1인칭 손은 항상 vanilla
 * 기본 자세 유지 (3인칭 / 타인 시점 SM 애니메이션은 영향 없음).
 *
 * 클라이언트는 단일 렌더 스레드 → volatile static 으로 충분.
 */
@Environment(EnvType.CLIENT)
public final class SmartMovingRenderContext {
    /** PlayerEntityRenderer.renderArm 호출 중 true (1인칭 손 렌더링 컨텍스트). */
    public static volatile boolean firstPersonArmRender = false;

    private SmartMovingRenderContext() {}
}
