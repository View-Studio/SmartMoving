package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributes;

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
     * Config 기반 속도 배율.
     * speedFactor × (1 + speedUserFactor)^speedUserExponent
     */
    public static float getConfigSpeedFactor(SmartMovingConfig cfg) {
        return cfg.speedFactor * cfg.getUserSpeedFactor();
    }

    /**
     * 포션 효과 포함 이동속도 역산 배율.
     * 원본: getLandMovementFactor() × 10F / (isSprinting ? 1.3F : 1F)
     *
     * GENERIC_MOVEMENT_SPEED 속성에 포션 효과가 이미 반영됨 → 이중 적용 주의.
     * 스프린트 modifier(+0.3)도 포함된 상태이므로 스프린팅 시 1.3F로 나눠 제거.
     */
    public static float getPotionSpeedFactor(ClientPlayerEntity player) {
        float landMovementFactor = (float) player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        return landMovementFactor * 10F / (player.isSprinting() ? 1.3F : 1F);
    }

    /**
     * 비감속 입력 배율 (얼음/스프린트/달리기).
     * 팩터 적용 우선순위: 얼음 > 스프린트 > 달리기 (중첩 불가).
     */
    public static float getNonSlowInputSpeedFactor(ClientPlayerEntity player, SmartMovingConfig cfg) {
        BlockState below = player.getWorld().getBlockState(player.getBlockPos().down());
        float slip = below.getBlock().getSlipperiness();
        if (slip > 0.6F) return 1.5F; // TODO Phase 13: cfg.iceSpeedFactor
        if (player.isSprinting()) return cfg.sprintFactor;
        // 달리기(run) 판정: 전진 + !sneaking + !sprinting — TODO Phase 12 정확한 조건 구현
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
     */
    public static float getSpeedFactor(ClientPlayerEntity player, SmartMovingClientState sm,
                                        SmartMovingConfig cfg) {
        return getConfigSpeedFactor(cfg)
             * getPotionSpeedFactor(player)
             * getNonSlowInputSpeedFactor(player, cfg)
             * getSlowInputSpeedFactor(player, sm, cfg);
    }
}
