package io.fairyfox.papermc.despawneditems.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * The full options screen for one despawn target — the "real options and setup menu" that a
 * client mod can offer and a plugin cannot.
 *
 * <p>Every control here maps to a verb the server already validates. The screen shows what
 * the server last reported and sends a request on click; it does not predict the outcome,
 * because the server may refuse (limits, permissions, a protection plugin). Requests are
 * followed by a re-query, so the screen always converges on the truth rather than on what
 * the client hoped.
 *
 * <p>Controls the player is not permitted to use are disabled rather than hidden — a greyed
 * button that says why is more useful than a menu that silently lacks an option.
 */
public class OptionsScreen extends Screen {

    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 24;

    private final Screen parent;
    private final BlockPos pos;

    private ButtonWidget enabledButton;
    private ButtonWidget priorityButton;
    private ButtonWidget contrabandButton;
    private ButtonWidget removeButton;

    public OptionsScreen(Screen parent, BlockPos pos) {
        super(Text.translatable("papermc-despawned-items.options.title"));
        this.parent = parent;
        this.pos = pos;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - BUTTON_WIDTH / 2;
        int top = this.height / 4 + 8;

        enabledButton = addDrawableChild(ButtonWidget
                .builder(Text.empty(), b -> {
                    ServerLink.toggle(pos);
                    ServerLink.query(pos);
                })
                .dimensions(left, top, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        priorityButton = addDrawableChild(ButtonWidget
                .builder(Text.empty(), b -> {
                    TargetState state = ServerLink.lastState();
                    int next = state == null ? 1 : state.priority() + 1;
                    // Wrap rather than clamp: cycling with one button should never dead-end.
                    ServerLink.priority(pos, next > 10 ? 1 : next);
                    ServerLink.query(pos);
                })
                .dimensions(left, top + SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        contrabandButton = addDrawableChild(ButtonWidget
                .builder(Text.empty(), b -> {
                    TargetState state = ServerLink.lastState();
                    ServerLink.contraband(pos, state == null || !state.acceptsContraband());
                    ServerLink.query(pos);
                })
                .dimensions(left, top + SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        removeButton = addDrawableChild(ButtonWidget
                .builder(Text.translatable("papermc-despawned-items.options.remove"), b -> {
                    ServerLink.unmark(pos);
                    close();
                })
                .dimensions(left, top + SPACING * 3, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        addDrawableChild(ButtonWidget
                .builder(Text.translatable("gui.done"), b -> close())
                .dimensions(left, top + SPACING * 4 + 8, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        ServerLink.query(pos);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 4 - 16, 0xFFFFFF);

        TargetState state = ServerLink.lastState();
        boolean known = state != null && state.pos().equals(pos) && state.registered();

        // Labels are rebuilt every frame from server state — there is no local copy to drift.
        enabledButton.setMessage(Text.translatable(
                known && state.enabled()
                        ? "papermc-despawned-items.options.enabled"
                        : "papermc-despawned-items.options.disabled"));
        priorityButton.setMessage(Text.translatable(
                "papermc-despawned-items.options.priority", known ? state.priority() : 1));
        contrabandButton.setMessage(Text.translatable(
                known && state.acceptsContraband()
                        ? "papermc-despawned-items.options.contraband.accept"
                        : "papermc-despawned-items.options.contraband.refuse"));

        boolean editable = known && ServerLink.can("manage-own");
        enabledButton.active = editable;
        priorityButton.active = editable;
        contrabandButton.active = editable;
        removeButton.active = editable;

        if (!known) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("papermc-despawned-items.options.unregistered").formatted(Formatting.GRAY),
                    this.width / 2,
                    this.height - 44,
                    0xAAAAAA);
        }
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
