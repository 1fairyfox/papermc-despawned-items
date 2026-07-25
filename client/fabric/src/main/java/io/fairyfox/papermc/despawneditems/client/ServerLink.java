package io.fairyfox.papermc.despawneditems.client;

import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.BlockPos;

/**
 * The client's view of the server: whether it supports this mod at all, what this player is
 * allowed to do, and the last known state of the block they are looking at.
 *
 * <p><b>The server is always right.</b> This class never assumes a capability; it starts in
 * {@link Status#UNKNOWN}, sends one {@code HELLO}, and does nothing visible until the server
 * answers. That is what makes installing the mod safe on any server: on a server without the
 * plugin, or with client-mod support switched off, no answer (or an {@code UNAVAILABLE})
 * arrives and the mod simply never shows its interface.
 */
public final class ServerLink {

    /** Protocol revision this mod speaks. Sent in HELLO so a server can refuse an old client. */
    public static final int PROTOCOL_VERSION = 1;

    public enum Status {
        /** No answer yet — either still waiting, or this server has no idea what we are. */
        UNKNOWN,
        /** The server answered WELCOME: the interface is available. */
        AVAILABLE,
        /** The server said no — client mods disabled, or this player lacks permission. */
        UNAVAILABLE
    }

    private static Status status = Status.UNKNOWN;
    private static String unavailableReason = "";
    private static final Set<String> CAPABILITIES = new HashSet<>();
    private static TargetState lastState;

    private ServerLink() {
    }

    /** Forgets everything. Called on disconnect so state never leaks between servers. */
    public static void reset() {
        status = Status.UNKNOWN;
        unavailableReason = "";
        CAPABILITIES.clear();
        lastState = null;
    }

    public static Status status() {
        return status;
    }

    public static String unavailableReason() {
        return unavailableReason;
    }

    public static boolean can(String capability) {
        return CAPABILITIES.contains(capability);
    }

    public static TargetState lastState() {
        return lastState;
    }

    /** True when the interface should be offered at all. */
    public static boolean available() {
        return status == Status.AVAILABLE;
    }

    // ── outgoing ────────────────────────────────────────────────────────────────────

    /** Introduces this client. Safe to call on any server: one packet, ignored if unknown. */
    public static void sayHello() {
        send("HELLO " + PROTOCOL_VERSION);
    }

    public static void query(BlockPos pos) {
        send(verb("QUERY", pos));
    }

    public static void mark(BlockPos pos) {
        send(verb("MARK", pos));
    }

    public static void unmark(BlockPos pos) {
        send(verb("UNMARK", pos));
    }

    public static void toggle(BlockPos pos) {
        send(verb("TOGGLE", pos));
    }

    public static void priority(BlockPos pos, int value) {
        send(verb("PRIORITY", pos) + " " + value);
    }

    public static void contraband(BlockPos pos, boolean accept) {
        send(verb("CONTRABAND", pos) + " " + accept);
    }

    /**
     * The world name is required by the protocol but the server re-derives the real world
     * from the player anyway, so sending a placeholder is correct and avoids the client
     * guessing at a name it cannot know reliably.
     */
    private static String verb(String verb, BlockPos pos) {
        return verb + " world " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static void send(String message) {
        if (ClientPlayNetworking.canSend(TargetsPayload.ID)) {
            ClientPlayNetworking.send(new TargetsPayload(message));
        }
    }

    // ── incoming ────────────────────────────────────────────────────────────────────

    /** Handles one line from the server. Never throws — a malformed line is simply dropped. */
    public static void receive(String message) {
        String[] parts = message.trim().split(" ");
        if (parts.length == 0) {
            return;
        }
        switch (parts[0]) {
            case "WELCOME" -> {
                status = Status.AVAILABLE;
                CAPABILITIES.clear();
                // parts[1] is the server's protocol version; capabilities follow.
                for (int i = 2; i < parts.length; i++) {
                    CAPABILITIES.add(parts[i]);
                }
            }
            case "UNAVAILABLE" -> {
                status = Status.UNAVAILABLE;
                unavailableReason = message.substring(Math.min("UNAVAILABLE ".length(), message.length()));
            }
            case "TARGET" -> {
                TargetState parsed = TargetState.parseTarget(parts);
                if (parsed != null) {
                    lastState = parsed;
                }
            }
            case "ABSENT" -> {
                TargetState parsed = TargetState.parseAbsent(parts);
                if (parsed != null) {
                    lastState = parsed;
                }
            }
            case "DENIED" -> {
                // The server refused something. Re-query so the button reflects reality
                // rather than the optimistic state a click may have implied.
                if (lastState != null) {
                    query(lastState.pos());
                }
            }
            default -> {
                // Unknown verb: ignore, so the server can add messages without breaking us.
            }
        }
    }
}
