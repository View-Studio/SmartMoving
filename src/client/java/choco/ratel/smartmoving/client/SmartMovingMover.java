package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.world.GameMode;

/**
 * 클라이언트 측 이동 속도 팩터 계산.
 * 원본: SmartMovingSelf.getConfigSpeedFactor / getPotionSpeedFactor /
 *       getNonSlowInputSpeedFactor / getSlowInputSpeedFactor 이식.
 *
 * 포함 항목:
 *   11-1: 속도 팩터 4단계 구조
 */
@Environment(EnvType.CLIENT)
public final class SmartMovingMover {

    private SmartMovingMover() {}

    // ── [11-1] 속도 팩터 4단계 ──────────────────────────────────────────────

    /**
     * 원본 `_speedUser = Creative("move.speed.user")` 팩토리 의미 재현.
     * `Creative` 팩토리는 Creative 게임 모드에서만 true 반환 → 1.21.1 에서는 런타임 gameMode
     * 감지로 대응. B-6 (세션 26) 신설.
     *
     * 원본 `isUserSpeedEnabled() = enabled && _speedUser.value` 에서 `_speedUser.value` 가
     * Creative 때만 true 이므로 이 조합이 Creative 체크 게이트가 됨.
     *
     * 1.21.1 대응:
     *   isUserSpeedEnabled = cfg.enabled && cfg.speedUser && (gameMode == CREATIVE)
     *   cfg.speedUser 는 사용자가 config 파일에서 허용 여부 설정 (기본 false, Easy 1:1).
     *   gameMode 체크는 매 호출 시 런타임 판정.
     */
    private static boolean isCreative(ClientPlayerEntity player) {
        if (player == null) return false;
        ClientPlayerInteractionManager im = MinecraftClient.getInstance().interactionManager;
        return im != null && im.getCurrentGameMode() == GameMode.CREATIVE;
    }

    /**
     * Config 기반 속도 배율.
     * 원본 `SmartMovingSelf.getConfigSpeedFactor()` (L154-157):
     *   `Config.enabled ? Config._speedFactor.value * Config.getUserSpeedFactor() : 1F`
     *
     * B-1 (세션 25): `cfg.enabled` 가드 추가.
     * B-6 (세션 26): Creative 전용 게이트 추가. `cfg.speedUser` 는 원본 `_speedUser.value`
     *   대응 — Creative 팩토리 의미 복원을 위해 런타임 gameMode 체크 병행.
     *   getUserSpeedFactor() 자체는 `!speedUser` 시 1F 반환하므로 Creative 아닐 때는
     *   사용자가 아무리 키 눌러도 factor=1F. 하지만 명시적 게이트가 원본 구조에 더 가까움.
     *
     * 공식 (B-6 이후):
     *   cfg.enabled && (gameMode == CREATIVE) ? speedFactor × (1+speedUserFactor)^exp : 1F
     *
     * Creative 외 게임 모드에서는 cfg.speedFactor (기본 1F) 반영도 안 됨 — 원본과 동일
     *   (`_speedFactor` 자체는 Creative 의존 아니지만, `getConfigSpeedFactor` 는 `Config.enabled`
     *    게이트만 있고, speedFactor × userSpeedFactor 이고 userSpeedFactor 가 Creative 때만
     *    1F 외값). speedFactor 는 전역 배율로 Creative 외에서도 적용되어야 하나 — 재검토.
     *
     * ⚠️ 엄밀 1:1: 원본 `getConfigSpeedFactor` 는 `Config.enabled` 만 체크하고
     *    `_speedFactor.value * getUserSpeedFactor()` 반환. `getUserSpeedFactor()` 가
     *    `!_speedUser.value` 시 1F 반환. 즉 Creative 외에서는 `_speedFactor.value` 만 반영됨.
     *    (`_speedFactor` 는 Creative 무관 전역 배율.)
     *    → 본 메서드에서 Creative 게이트는 userSpeedFactor 부분만 차단하는 게 정확.
     *    아래 구현은 speedFactor 는 전역 반영 + userSpeedFactor 는 Creative 게이트.
     */
    public static float getConfigSpeedFactor(ClientPlayerEntity player, SmartMovingConfig cfg) {
        if (!cfg.enabled) return 1F;
        float userFactor = isCreative(player) ? cfg.getUserSpeedFactor() : 1F;
        return cfg.speedFactor * userFactor;
    }

    /**
     * 포션 효과 포함 이동속도 역산 배율.
     * 원본 `SmartMovingSelf.getPotionSpeedFactor()` (L160-162) 1:1:
     *   `Config.enabled ? getLandMovementFactor() * 10F / (sp.isSprinting() ? 1.3F : 1F) : 1F`
     * B-1 (세션 25): `cfg.enabled` 가드 추가.
     *
     * GENERIC_MOVEMENT_SPEED 속성에 포션 효과가 이미 반영됨 → 이중 적용 주의.
     * 스프린트 modifier(+0.3)도 포함된 상태이므로 스프린팅 시 1.3F로 나눠 제거.
     */
    public static float getPotionSpeedFactor(ClientPlayerEntity player) {
        if (!SmartMovingConfig.Config.enabled) return 1F;
        float landMovementFactor = (float) player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        return landMovementFactor * 10F / (player.isSprinting() ? 1.3F : 1F);
    }

    /**
     * 원본 `SmartMovingSelf.getCombinedSpeedFactor()` (L149-152) 1:1:
     *   `return getConfigSpeedFactor() * getPotionSpeedFactor();`
     * B-1 (세션 25) 신설 — Climber/Jumper 등 6곳에서 인라인 곱셈 대체.
     * B-6 (세션 26): getConfigSpeedFactor 시그니처 변경에 따른 player 전달.
     */
    public static float getCombinedSpeedFactor(ClientPlayerEntity player, SmartMovingConfig cfg) {
        return getConfigSpeedFactor(player, cfg) * getPotionSpeedFactor(player);
    }

    /**
     * 비감속 입력 배율 (얼음/스프린트/달리기).
     * 팩터 적용 우선순위: 얼음 > 스프린트 > 달리기 (중첩 불가).
     *
     * isRunning = isSprinting() && !isFast (A-21 확인값).
     * iceSpeedFactor: 원본 config에 없음 — 1.5F 고정 (A-25 확인).
     */
    public static float getNonSlowInputSpeedFactor(ClientPlayerEntity player, SmartMovingClientState sm,
                                                    SmartMovingConfig cfg) {
        BlockState below = player.getWorld().getBlockState(player.getBlockPos().down());
        float slip = below.getBlock().getSlipperiness();
        if (slip > 0.6F) return 1.5F;
        if (player.isSprinting()) {
            return sm.isFast ? cfg.sprintFactor : cfg.runFactor;
        }
        return 1.0F;
    }

    /**
     * 감속 입력 배율 (크롤링/스니킹/아이템사용/천장클라이밍).
     * 팩터들은 순서대로 누적 곱셈 적용.
     */
    public static float getSlowInputSpeedFactor(ClientPlayerEntity player, SmartMovingClientState sm,
                                                 SmartMovingConfig cfg) {
        float f = 1.0F;
        if (player.isUsingItem()) f *= 0.2F;
        if (sm.isCrawling) f *= cfg.crawlFactor;
        else if (player.isSneaking()) f *= cfg.sneakFactor;
        if (sm.isCeilingClimbing) f *= cfg.ceilingClimbingSpeedFactor;
        return f;
    }

    /**
     * 최종 속도 팩터 = 4단계 곱셈.
     * 호출 위치: travel() SM 파이프라인 내 각 handle*() 메서드에서 speedFactor 계산 시.
     * B-6 (세션 26): getConfigSpeedFactor 시그니처 변경에 따른 player 전달.
     */
    public static float getSpeedFactor(ClientPlayerEntity player, SmartMovingClientState sm,
                                        SmartMovingConfig cfg) {
        return getConfigSpeedFactor(player, cfg)
             * getPotionSpeedFactor(player)
             * getNonSlowInputSpeedFactor(player, sm, cfg)
             * getSlowInputSpeedFactor(player, sm, cfg);
    }
}
