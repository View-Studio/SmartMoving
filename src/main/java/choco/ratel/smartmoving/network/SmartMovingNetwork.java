package choco.ratel.smartmoving.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * SM 패킷 채널 등록 및 7종 Payload 타입 정의.
 * 원본 ObjectOutputStream/FML → Fabric PacketByteBuf/CustomPayload 변환.
 *
 * 채널 방향:
 *  C2S: state, config_info, config_change, speed_change, hunger_change, sound
 *  S2C: state, config_content, config_change, speed_change
 */
public final class SmartMovingNetwork {

    // ── Payload 타입 ──────────────────────────────────────────────

    /** State 패킷 (C2S + S2C): 클라이언트가 자신의 이동 상태를 전송, 서버가 추적 플레이어에게 릴레이 */
    public record StatePayload(int entityId, long state) implements CustomPayload {
        public static final Id<StatePayload> ID = new Id<>(Identifier.of("smartmoving", "state"));
        public static final PacketCodec<PacketByteBuf, StatePayload> CODEC = new PacketCodec<>() {
            @Override
            public StatePayload decode(PacketByteBuf buf) {
                return new StatePayload(buf.readInt(), buf.readLong());
            }
            @Override
            public void encode(PacketByteBuf buf, StatePayload value) {
                buf.writeInt(value.entityId());
                buf.writeLong(value.state());
            }
        };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** ConfigInfo 패킷 (C2S): 클라이언트가 설정 정보를 서버에 전송 */
    public record ConfigInfoPayload(String config) implements CustomPayload {
        public static final Id<ConfigInfoPayload> ID = new Id<>(Identifier.of("smartmoving", "config_info"));
        public static final PacketCodec<PacketByteBuf, ConfigInfoPayload> CODEC = new PacketCodec<>() {
            @Override
            public ConfigInfoPayload decode(PacketByteBuf buf) {
                return new ConfigInfoPayload(buf.readString());
            }
            @Override
            public void encode(PacketByteBuf buf, ConfigInfoPayload value) {
                buf.writeString(value.config());
            }
        };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /**
     * ConfigContent 패킷 (S2C): 서버가 설정 내용을 클라이언트에게 전송.
     * lines = null 시 설정 비활성화. username = null 시 해당 플레이어에게 권한 없음.
     */
    public record ConfigContentPayload(String[] lines, String username) implements CustomPayload {
        public static final Id<ConfigContentPayload> ID = new Id<>(Identifier.of("smartmoving", "config_content"));
        public static final PacketCodec<PacketByteBuf, ConfigContentPayload> CODEC = new PacketCodec<>() {
            @Override
            public ConfigContentPayload decode(PacketByteBuf buf) {
                boolean hasLines = buf.readBoolean();
                String[] lines = null;
                if (hasLines) {
                    int len = buf.readByte() & 0xFF;
                    lines = new String[len];
                    for (int i = 0; i < len; i++) lines[i] = buf.readString();
                }
                boolean hasUsername = buf.readBoolean();
                String username = hasUsername ? buf.readString() : null;
                return new ConfigContentPayload(lines, username);
            }
            @Override
            public void encode(PacketByteBuf buf, ConfigContentPayload value) {
                boolean hasLines = value.lines() != null;
                buf.writeBoolean(hasLines);
                if (hasLines) {
                    buf.writeByte(value.lines().length);
                    for (String line : value.lines()) buf.writeString(line);
                }
                buf.writeBoolean(value.username() != null);
                if (value.username() != null) buf.writeString(value.username());
            }
        };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** ConfigChange 패킷 (C2S + S2C): 설정 변경 알림, 페이로드 없음 */
    public record ConfigChangePayload() implements CustomPayload {
        public static final Id<ConfigChangePayload> ID = new Id<>(Identifier.of("smartmoving", "config_change"));
        public static final PacketCodec<PacketByteBuf, ConfigChangePayload> CODEC = new PacketCodec<>() {
            @Override
            public ConfigChangePayload decode(PacketByteBuf buf) {
                return new ConfigChangePayload();
            }
            @Override
            public void encode(PacketByteBuf buf, ConfigChangePayload value) {
                // 페이로드 없음
            }
        };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /**
     * SpeedChange 패킷 (C2S + S2C): 속도 변경 요청.
     * username = null 시 해당 플레이어는 속도 변경 권한 없음.
     */
    public record SpeedChangePayload(int difference, String username) implements CustomPayload {
        public static final Id<SpeedChangePayload> ID = new Id<>(Identifier.of("smartmoving", "speed_change"));
        public static final PacketCodec<PacketByteBuf, SpeedChangePayload> CODEC = new PacketCodec<>() {
            @Override
            public SpeedChangePayload decode(PacketByteBuf buf) {
                int diff = buf.readInt();
                boolean hasUsername = buf.readBoolean();
                String username = hasUsername ? buf.readString() : null;
                return new SpeedChangePayload(diff, username);
            }
            @Override
            public void encode(PacketByteBuf buf, SpeedChangePayload value) {
                buf.writeInt(value.difference());
                buf.writeBoolean(value.username() != null);
                if (value.username() != null) buf.writeString(value.username());
            }
        };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** HungerChange 패킷 (C2S): 클라이언트 소진값을 서버에 전달. -1 = 억제 해제, 0 = 소진 없음. */
    public record HungerChangePayload(float hunger) implements CustomPayload {
        public static final Id<HungerChangePayload> ID = new Id<>(Identifier.of("smartmoving", "hunger_change"));
        public static final PacketCodec<PacketByteBuf, HungerChangePayload> CODEC = new PacketCodec<>() {
            @Override
            public HungerChangePayload decode(PacketByteBuf buf) {
                return new HungerChangePayload(buf.readFloat());
            }
            @Override
            public void encode(PacketByteBuf buf, HungerChangePayload value) {
                buf.writeFloat(value.hunger());
            }
        };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /**
     * Sound 패킷 (C2S): SM 전용 사운드 재생 요청.
     * 원본 IPacketReceiver.processSoundPacket 파라미터명이 distance이지만 실제로는 volume 직렬화.
     */
    public record SoundPayload(String soundId, float volume, float pitch) implements CustomPayload {
        public static final Id<SoundPayload> ID = new Id<>(Identifier.of("smartmoving", "sound"));
        public static final PacketCodec<PacketByteBuf, SoundPayload> CODEC = new PacketCodec<>() {
            @Override
            public SoundPayload decode(PacketByteBuf buf) {
                return new SoundPayload(buf.readString(), buf.readFloat(), buf.readFloat());
            }
            @Override
            public void encode(PacketByteBuf buf, SoundPayload value) {
                buf.writeString(value.soundId());
                buf.writeFloat(value.volume());
                buf.writeFloat(value.pitch());
            }
        };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ── 등록 ──────────────────────────────────────────────────────

    /**
     * SmartMoving.onInitialize()에서 호출.
     * C2S/S2C 양방향에 필요한 모든 페이로드 타입을 PayloadTypeRegistry에 등록.
     */
    public static void register() {
        // C2S (클라이언트→서버)
        PayloadTypeRegistry.playC2S().register(StatePayload.ID,        StatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ConfigInfoPayload.ID,   ConfigInfoPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ConfigChangePayload.ID, ConfigChangePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SpeedChangePayload.ID,  SpeedChangePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HungerChangePayload.ID, HungerChangePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SoundPayload.ID,        SoundPayload.CODEC);

        // S2C (서버→클라이언트)
        PayloadTypeRegistry.playS2C().register(StatePayload.ID,         StatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ConfigContentPayload.ID, ConfigContentPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ConfigChangePayload.ID,  ConfigChangePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SpeedChangePayload.ID,   SpeedChangePayload.CODEC);
    }

    private SmartMovingNetwork() {}
}
