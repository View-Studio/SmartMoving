package choco.ratel.smartmoving.mixin.server;

import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 3-2: floatingTicks 리셋 Mixin.
 * SM 클라이밍(isClimbing / isCrawlClimbing / isCeilingClimbing) 중에는
 * ServerPlayNetworkHandler.floatingTicks를 매 틱 0으로 리셋하여
 * 80틱 초과 kick("multiplayer.disconnect.flying")을 방지한다.
 *
 * 리셋 조건: resetTicksForFloatKick == true (벽점프는 포함하지 않음)
 * 낙하 거리 리셋 조건: resetFallDistance == true (벽점프 포함)
 *
 * 원본: SmartMovingServerPlayerBase.resetTicksForFloatKick()
 *       → Reflect.SetField(NetHandlerPlayServer, handler, "ticksForFloatKick", 0)
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class MixinServerPlayNetworkHandler {

    /** Yarn: field_14138 — 플레이어가 공중에 떠있는 틱 카운터. 80 초과 시 kick. */
    @Shadow private int floatingTicks;

    /** 처리 대상 플레이어 */
    @Shadow public ServerPlayerEntity player;

    /**
     * tick() HEAD에서 SM 상태에 따라 floatingTicks를 리셋하고 낙하 거리를 초기화한다.
     * 클라이언트 State 패킷 수신 시 processStatePacket()이 resetTicksForFloatKick/
     * resetFallDistance 플래그를 갱신하므로, 이 Mixin은 단순히 플래그를 참조하기만 한다.
     */
    // ── [11-3] moved wrongly 완화 stub ───────────────────────────────────────
    // 1.21.1 Yarn명: ServerPlayNetworkHandler.onPlayerMove(PlayerMoveC2SPacket) (A-29 확인)
    // SM 클라이밍/크롤링 상태일 때 서버 위치 검증(positionSqDist 체크)을 skip해야
    // "moved wrongly" rubber-band 현상을 방지할 수 있다.
    // C-21: @Inject(method="onPlayerMove") 로 구현

    @Inject(method = "tick", at = @At("HEAD"))
    private void sm_tick(CallbackInfo ci) {
        if (player == null) return;
        SmartMovingServer sm = SmartMovingServer.get(player);

        // 3-9: 낙하 거리 리셋 (isClimbing 계열 + isWallJumping 중 fallDistance가 누적되지 않도록)
        if (sm.resetFallDistance) {
            sm.applyFallDistanceReset(player);
        }

        // 3-2: floatingTicks 리셋 (80틱 kick 방지)
        if (sm.resetTicksForFloatKick) {
            floatingTicks = 0;
        }
    }
}
