package io.fairyfox.papermc.despawneditems.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/**
 * The button drawn on a container screen: <i>"Mark as despawn target"</i> / <i>"Despawn
 * target: ON"</i> / <i>"Despawn target: OFF"</i>.
 *
 * <p>Placement is deliberately outside the container's own panel — just above its top-left
 * corner — so it cannot cover slots, the recipe-book toggle, or the buttons other mods like
 * to add along the sides. If a mod does collide, the button is a normal widget in the
 * screen's list and moves with the rest of the layout.
 *
 * <p>Left-click cycles the useful states (unmarked → marked → off → marked …); shift-click
 * opens the full options screen. That keeps the common action one click deep and the rare
 * one discoverable, without a second widget competing for space.
 */
public final class ContainerButton {

    public static final int WIDTH = 110;
    public static final int HEIGHT = 20;

    /** Half the height of the tallest vanilla container panel (double chest, 222px). */
    private static final int PANEL_HALF_HEIGHT = 111;

    private final HandledScreen<?> screen;
    private final BlockPos pos;
    private ButtonWidget widget;

    public ContainerButton(HandledScreen<?> screen) {
        this.screen = screen;
        this.pos = lookedAtBlock();
    }

    /**
     * The block this screen belongs to.
     *
     * <p>A container screen does not carry its own world position, so we take the block the
     * player is looking at — which, at the moment a container screen opens, is the container.
     * The server re-validates the position against the player's reach anyway, so a wrong
     * guess here is refused rather than acted on.
     */
    private static BlockPos lookedAtBlock() {
        MinecraftClient client = MinecraftClient.getInstance();
        HitResult hit = client.crosshairTarget;
        if (hit instanceof BlockHitResult blockHit) {
            return blockHit.getBlockPos();
        }
        return null;
    }

    /** Centred horizontally on the container panel. */
    public int x() {
        return screen.width / 2 - WIDTH / 2;
    }

    /**
     * Sits above the container panel.
     *
     * <p>{@code HandledScreen}'s own {@code backgroundHeight} is protected, and reaching it
     * would mean an access widener or a mixin for a purely cosmetic detail. A fixed offset
     * from the screen centre clears every vanilla container (the tallest, the double chest,
     * is 222px) without that machinery, and the clamp keeps it on screen at large GUI scales.
     */
    public int y() {
        return Math.max(2, screen.height / 2 - PANEL_HALF_HEIGHT - HEIGHT - 4);
    }

    public void attach(ButtonWidget widget) {
        this.widget = widget;
        refresh();
    }

    public void detach() {
        this.widget = null;
    }

    /** Asks the server for this block's state so the label is right immediately. */
    public void requestState() {
        if (pos != null) {
            ServerLink.query(pos);
        }
    }

    /** Repaints the label from whatever the server last told us. */
    public void refresh() {
        if (widget == null) {
            return;
        }
        TargetState state = ServerLink.lastState();
        if (pos == null || state == null || !state.pos().equals(pos)) {
            widget.setMessage(Text.translatable("papermc-despawned-items.button.loading"));
            widget.active = false;
            return;
        }
        widget.active = true;
        if (!state.registered()) {
            widget.setMessage(Text.translatable("papermc-despawned-items.button.mark"));
        } else if (state.enabled()) {
            widget.setMessage(Text.translatable("papermc-despawned-items.button.on"));
        } else {
            widget.setMessage(Text.translatable("papermc-despawned-items.button.off"));
        }
    }

    /** Left-click cycles state; shift-click opens the options screen. */
    public void onPress() {
        if (pos == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        boolean shift = client.currentScreen != null && net.minecraft.client.gui.screen.Screen.hasShiftDown();

        TargetState state = ServerLink.lastState();
        if (shift && state != null && state.registered()) {
            client.setScreen(new OptionsScreen(client.currentScreen, pos));
            return;
        }

        if (state == null || !state.registered()) {
            ServerLink.mark(pos);
        } else {
            ServerLink.toggle(pos);
        }
        // The server answers with the new state; refresh when it lands.
        client.execute(this::refresh);
    }
}
