package io.fairyfox.papermc.despawneditems.client;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * The wire payload for the {@code papermc-despawned-items:targets} channel.
 *
 * <p>Deliberately a single UTF string rather than a structured packet. The server side is a
 * Bukkit plugin sending raw bytes on a plugin-messaging channel, and the protocol is
 * documented as plain text precisely so that <em>anything</em> can speak it — this mod, a
 * different mod, a Velocity plugin, a test harness — without linking against a shared
 * library. A binary codec would be marginally smaller and would cost that.
 *
 * <p>Note the asymmetry with Bukkit: Bukkit writes the raw channel bytes directly, while
 * Minecraft's own {@code PacketByteBuf#writeString} prefixes a VarInt length. Both sides
 * therefore use the length-prefixed form, which is what {@code PacketCodecs}-style string
 * handling produces — see {@code readString}/{@code writeString} below.
 */
public record TargetsPayload(String message) implements CustomPayload {

    public static final Identifier CHANNEL = Identifier.of("papermc-despawned-items", "targets");
    public static final CustomPayload.Id<TargetsPayload> ID = new CustomPayload.Id<>(CHANNEL);

    /** Max payload length. Generous for a line of text, small enough to bound a hostile server. */
    private static final int MAX_LENGTH = 32767;

    public static final PacketCodec<PacketByteBuf, TargetsPayload> CODEC =
            PacketCodec.of(TargetsPayload::write, TargetsPayload::read);

    private static void write(TargetsPayload payload, PacketByteBuf buf) {
        buf.writeString(payload.message(), MAX_LENGTH);
    }

    private static TargetsPayload read(PacketByteBuf buf) {
        return new TargetsPayload(buf.readString(MAX_LENGTH));
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
