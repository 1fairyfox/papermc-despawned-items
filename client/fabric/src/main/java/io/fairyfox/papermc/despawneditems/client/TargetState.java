package io.fairyfox.papermc.despawneditems.client;

import net.minecraft.util.math.BlockPos;

/**
 * What the server last told us about one block.
 *
 * <p>{@code registered == false} means the server explicitly said ABSENT — the block exists
 * but is not a despawn target. That is different from "we have not asked yet", which is
 * represented by the absence of a {@code TargetState} altogether. The button needs all three
 * states so it can show "mark", "marked", or nothing at all while it waits.
 */
public record TargetState(BlockPos pos, boolean registered, boolean enabled, int priority, boolean acceptsContraband) {

    public static TargetState absent(BlockPos pos) {
        return new TargetState(pos, false, false, 1, false);
    }

    /**
     * Parses a {@code TARGET} line:
     * {@code TARGET <world> <x> <y> <z> <owner> <enabled> <priority> <contraband>}.
     *
     * @return the parsed state, or null when the line is malformed — a client must never
     *         throw on something a server said, however odd.
     */
    public static TargetState parseTarget(String[] parts) {
        if (parts.length < 9) {
            return null;
        }
        try {
            BlockPos pos = new BlockPos(
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]));
            return new TargetState(
                    pos,
                    true,
                    Boolean.parseBoolean(parts[6]),
                    Integer.parseInt(parts[7]),
                    Boolean.parseBoolean(parts[8]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** Parses an {@code ABSENT <world> <x> <y> <z>} line. */
    public static TargetState parseAbsent(String[] parts) {
        if (parts.length < 5) {
            return null;
        }
        try {
            return absent(new BlockPos(
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4])));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
