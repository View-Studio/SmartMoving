package choco.ratel.smartmoving.network;

import choco.ratel.smartmoving.state.SmartMovingAttachments;
import choco.ratel.smartmoving.state.SmartMovingState;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 원격 플레이어 SmartMoving 상태 생명주기 관리.
 * 수신된 패킷으로 원격 플레이어의 SmartMovingState를 갱신.
 * AttachmentType으로 직접 플레이어에 붙이므로 별도 Map은 참조 추적용으로만 사용.
 */
public final class RemotePlayerManager {

    private static final ConcurrentHashMap<UUID, Long> lastUpdateTick = new ConcurrentHashMap<>();

    /**
     * 원격 플레이어 상태를 패킷 데이터로 갱신.
     * 클라이언트 수신 핸들러에서 호출.
     */
    public static void applyRemoteState(PlayerEntity player, long encodedState) {
        SmartMovingState state = player.getAttachedOrCreate(SmartMovingAttachments.STATE);
        StateEncoder.decode(encodedState, state);
        lastUpdateTick.put(player.getUuid(), System.currentTimeMillis());
    }

    /**
     * 접속 해제 플레이어 데이터 정리.
     */
    public static void onPlayerLeave(UUID uuid) {
        lastUpdateTick.remove(uuid);
    }

    private RemotePlayerManager() {}
}
