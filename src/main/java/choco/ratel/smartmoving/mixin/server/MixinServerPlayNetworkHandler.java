package choco.ratel.smartmoving.mixin.server;

import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
    /** C-21: SM 클라이밍/크롤링 활성 여부 — onPlayerMove 진입 시 캐시. */
    @Unique
    private boolean sm_suppressPositionCheck;

    /**
     * C-21: onPlayerMove HEAD — SM 이동 상태를 캐시.
     * 원본: NetHandlerPlayServer.processPlayer() 위치 검증 skip 패치.
     */
    @Inject(method = "onPlayerMove", at = @At("HEAD"))
    private void sm_onPlayerMove_head(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (player == null) return;
        SmartMovingServer sm = SmartMovingServer.get(player);
        // [FLY-DBG-SERVER-MOVE] PlayerMoveC2S 수신 시점 server.y 변화 추적.
        //   SmartMovingServer 에 isFlying field 없음 → dY > 0.3m 만 출력 (= 큰 위치 변화 추적용).
        double packetY = packet.getY(player.getY());
        double oldY = player.getY();
        double dY = packetY - oldY;
        if (Math.abs(dY) > 0.3 || sm.isSliding || sm.isCrawling) {
            System.out.println(String.format(
                "[FLY-DBG-SERVER-MOVE] tick=%d sender=%s server.y=%.4f packet.y=%.4f dY=%.4f bb.minY=%.4f isSld=%b isCr=%b isCC=%b suppress=%b",
                player.age, player.getName().getString(),
                oldY, packetY, dY, player.getBoundingBox().minY,
                sm.isSliding, sm.isCrawling, sm.isClimbCrawling,
                (sm.isClimbing || sm.isCrawling || sm.isCrawlClimbing || sm.isCeilingClimbing || sm.isSliding)));
        }
        // 🔴 fix #89 (2026-05-12, BUG = "jump 차징 중 sneak 누름 → 1칸 띄어진 엎드리기"):
        //   isSliding 누락 시 자체 슬라이딩 발사 frame (= client entity.y -=1m) 의 delta y 가
        //   vanilla "moved too quickly" 검사 매치 → requestTeleport(=PlayerPositionLookS2CPacket
        //   송신) 발동 → client entity.y +1m 강제 보정 → 후속 SS-SlideStop + fix #62 v2 push 누적
        //   → 박스 ground+1m 부유 → 사용자 시각 "1칸 띄어진 엎드리기".
        //   다른 SM phase 가드 (isClimbing/isCrawling/isCrawlClimbing/isCeilingClimbing) 와 동일 패턴.
        sm_suppressPositionCheck = sm.isClimbing || sm.isCrawling || sm.isCrawlClimbing || sm.isCeilingClimbing
                || sm.isSliding;
    }

    @Inject(method = "onPlayerMove", at = @At("TAIL"))
    private void sm_onPlayerMove_tail(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (player == null) return;
        SmartMovingServer sm = SmartMovingServer.get(player);
        if (sm.isSliding || sm.isCrawling) {
            System.out.println(String.format(
                "[FLY-DBG-SERVER-MOVE-AFTER] tick=%d y=%.4f bb.minY=%.4f isSld=%b isCr=%b",
                player.age, player.getY(), player.getBoundingBox().minY,
                sm.isSliding, sm.isCrawling));
        }
    }

    /**
     * C-21: onPlayerMove 내 requestTeleport 호출 전부 가로채기.
     * SM 이동 중에는 "moved wrongly" / "moved too quickly" rubber-band를 차단한다.
     * require=0: 메서드 내 대상 invocation 개수가 변경되어도 크래시 없이 소프트 실패.
     */
    @Redirect(method = "onPlayerMove", require = 0,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;requestTeleport(DDDFF)V"))
    private void sm_cancelTeleportForSmMove(ServerPlayNetworkHandler handler,
            double x, double y, double z, float yaw, float pitch) {
        if (!sm_suppressPositionCheck) {
            requestTeleport(x, y, z, yaw, pitch);
        }
    }

    @Shadow
    public abstract void requestTeleport(double x, double y, double z, float yaw, float pitch);

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
