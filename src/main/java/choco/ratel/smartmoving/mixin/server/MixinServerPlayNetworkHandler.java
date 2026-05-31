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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

    /** 🔴 fix #103 — SM phase 종료 history flag. */
    @Unique
    private boolean _smWasInPhase = false;
    @Unique
    private int _smPhaseExitTick = -9999;

    /**
     * C-21: onPlayerMove HEAD — SM 이동 상태를 캐시.
     * 원본: NetHandlerPlayServer.processPlayer() 위치 검증 skip 패치.
     */
    @Inject(method = "onPlayerMove", at = @At("HEAD"))
    private void sm_onPlayerMove_head(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (player == null) return;
        SmartMovingServer sm = SmartMovingServer.get(player);
        // 🔴 fix #89 (2026-05-12, BUG = "jump 차징 중 sneak 누름 → 1칸 띄어진 엎드리기"):
        //   isSliding 누락 시 자체 슬라이딩 발사 frame (= client entity.y -=1m) 의 delta y 가
        //   vanilla "moved too quickly" 검사 매치 → requestTeleport(=PlayerPositionLookS2CPacket
        //   송신) 발동 → client entity.y +1m 강제 보정 → 후속 SS-SlideStop + fix #62 v2 push 누적
        //   → 박스 ground+1m 부유 → 사용자 시각 "1칸 띄어진 엎드리기".
        //   다른 SM phase 가드 (isClimbing/isCrawling/isCrawlClimbing/isCeilingClimbing) 와 동일 패턴.
        // 🔴 fix #100 (2026-05-31, BUG-1 점프강화+여우무빙 "moved too quickly"):
        //   여우무빙 server side state = isHeadJumping=true (isSliding=false). 기존 식에
        //   isHeadJumping 누락 → suppress=false → vanilla requestTeleport 통과 → client
        //   rubber-band → "그자리에서 멈춤". 진단 로그로 직접 확인.
        boolean curInPhase = sm.isClimbing || sm.isCrawling || sm.isCrawlClimbing || sm.isCeilingClimbing
                || sm.isSliding || sm.isHeadJumping;

        // 🔴 fix #103 (2026-05-31, BUG = "HJ/슬라이딩 + 1칸 물 빠지면 뚝 끊기듯이 튀었다가 다시 돌아옴"):
        //   진단 로그 확정 사실 — client 가 isHJ=false StatePayload 송신 → server.sm.isHeadJumping
        //   즉시 갱신 → 같은 tick 의 다음 onPlayerMove 시 sm_suppressPositionCheck=false →
        //   fix #101/#102 의 ModifyVariable 가 원본 distSq/postDistSq 값 반환 → vanilla "moved
        //   wrongly" 매치 (postDistSq=0.10 > 0.0625) → requestTeleport EXECUTED → client
        //   PlayerPositionLookS2CPacket 수신 → setPos(serverY+1.0) → +1.5m up jump "튀어오름".
        //   = SM phase state 가 transition frame 에 빠르게 false 가 되어 fix #101/#102 못 잡음.
        //
        //   해결: history flag 패턴 (= fix #153 v3 / #154 v4 차용. 메모리 [feedback_multistep_state_transition_history_flag]).
        //   SM phase 종료 tick 기록 + N tick window 동안 suppress 유지 → transition frame 의
        //   rubber-band 차단.
        //
        //   window 5 tick — server broadcast lag 측정 안정값. 검증 후 조정.
        if (_smWasInPhase && !curInPhase) {
            _smPhaseExitTick = player.age;
        }
        boolean recentlyExited = (player.age - _smPhaseExitTick) >= 0
                && (player.age - _smPhaseExitTick) <= 5;
        sm_suppressPositionCheck = curInPhase || recentlyExited;
        _smWasInPhase = curInPhase;
    }

    /**
     * 🔴 fix #101 (2026-05-31, BUG "moved too quickly" — return 살아있는 문제):
     * vanilla bytecode `505: ifle 609` (threshold check) 가 SM phase 중에도 매치되면
     * warn + requestTeleport + return 가 통째로 실행. 기존 SM redirect 는 requestTeleport
     * 만 cancel 하므로 return 은 그대로 → player.move() 미실행 → server 위치 동결 → 다음
     * packet 도 동일 cascade.
     *
     * 해결: slot 25 의 첫 번째 dstore (= distanceSq, offset 313) 직후 값을 0 으로 만들면
     * `(0 - velocityLenSq) ≤ 0` → ifle 609 → 분기 진입 자체가 차단되어 warn + teleport +
     * return 전체 skip. 후속 player.move() 정상 실행. server 좌표 자연 갱신.
     *
     * 참고: 사이 sleeping check (offset 325, dload 25) 는 isSleeping=true 시점에 분기되며
     * 어차피 return 도달 경로라 distSq=0 영향 X.
     */
    @ModifyVariable(method = "onPlayerMove", require = 0,
        at = @At(value = "STORE", ordinal = 0),
        index = 25)
    private double sm_suppressDistanceSq(double original) {
        return sm_suppressPositionCheck ? 0.0 : original;
    }

    /**
     * 🔴 fix #102 (2026-05-31, BUG "moved wrongly" — server 동기화 안 됨):
     * vanilla bytecode `817: ifle 885` (postDistSq > 0.0625 check) 매치 시 wronglyMoved=1
     * + warn + 후속 rubber-band 분기 진입. 기존 SM redirect 는 requestTeleport 만 cancel
     * 하지만 return 은 살아있어 `player.updatePositionAndAngles(packetX, packetY, packetZ)`
     * 미호출 → server 가 packet pos 동기화 X. 진단 로그로 직접 확인: SM phase 중 client
     * SM physics vs server.move() 결과가 ~0.35m 차이 (sliding+sneak motion 식 미일치) →
     * postDistSq=0.14 > 0.0625 매번 매치.
     *
     * 해결: slot 25 의 두 번째 dstore (= postDistSq, offset 796) 직후 값을 0 으로 만들면
     * `0 ≤ 0.0625` → ifle 885 → wronglyMoved=1 set 안 됨 + warn skip. 후속 `iload 33;
     * ifeq 923` 에서 wronglyMoved=0 (default) 이므로 isPlayerNotCollidingWithBlocks 분기로.
     * 거기서도 SM phase 중 box 차이로 false positive 가능 → 기존 requestTeleport redirect
     * 가 안전망. updatePositionAndAngles 까지 진행되어 server 동기화 정상.
     */
    @ModifyVariable(method = "onPlayerMove", require = 0,
        at = @At(value = "STORE", ordinal = 1),
        index = 25)
    private double sm_suppressPostDistSq(double original) {
        return sm_suppressPositionCheck ? 0.0 : original;
    }

    /**
     * C-21: onPlayerMove 내 requestTeleport 호출 전부 가로채기.
     * SM 이동 중에는 "moved wrongly" / "moved too quickly" rubber-band를 차단한다.
     * require=0: 메서드 내 대상 invocation 개수가 변경되어도 크래시 없이 소프트 실패.
     *
     * 참고: fix #101/#102 가 두 IF 분기 자체를 차단하므로 SM phase 중 이 redirect 가
     * 호출될 일은 거의 없음. 단 isPlayerNotCollidingWithBlocks=true edge case 의 안전망.
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
