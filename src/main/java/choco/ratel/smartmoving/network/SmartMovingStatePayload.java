package choco.ratel.smartmoving.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 64비트 압축 상태를 담는 C→S→All 패킷.
 * 비트 레이아웃은 research_networking.md 섹션 B 참조.
 */
public record SmartMovingStatePayload(int entityId, long state) implements CustomPayload {

    public static final CustomPayload.Id<SmartMovingStatePayload> ID =
            new CustomPayload.Id<>(Identifier.of("smartmoving", "state"));

    public static final PacketCodec<RegistryByteBuf, SmartMovingStatePayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER,  SmartMovingStatePayload::entityId,
                    PacketCodecs.VAR_LONG, SmartMovingStatePayload::state,
                    SmartMovingStatePayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
