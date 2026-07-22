package io.fairyfox.papermc.despawneditems.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.RegistryByteBuf;
import net.minecraft.text.Text;

/**
 * Entry point for the companion client mod.
 *
 * <p>This is the piece a server plugin genuinely cannot do: put a real button inside the
 * chest / furnace / barrel screen. A container screen has only real slots, so a plugin can
 * only fake one by replacing the entire screen — risking the player's items and fighting
 * every storage mod on the server. Here it is a widget added to the existing screen, which
 * touches no slots at all.
 *
 * <p><b>Installing this mod is always safe.</b> It sends exactly one packet on join. On a
 * server without the plugin nothing answers, {@link ServerLink} stays
 * {@link ServerLink.Status#UNKNOWN}, and no button is ever drawn. On a server that has the
 * plugin but has switched client mods off, the answer is {@code UNAVAILABLE} and the result
 * is the same. The mod cannot make the game do anything the player could not already do by
 * typing {@code /despi}.
 */
public class DespawnedItemsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        registerNetworking();
        registerScreenButton();
    }

    private void registerNetworking() {
        // Register both directions. The codec is on RegistryByteBuf because that is what
        // play-phase custom payloads use in 1.20.5+.
        PacketCodec<RegistryByteBuf, TargetsPayload> codec =
                TargetsPayload.CODEC.cast();

        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C()
                .register(TargetsPayload.ID, codec);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S()
                .register(TargetsPayload.ID, codec);

        ClientPlayNetworking.registerGlobalReceiver(
                TargetsPayload.ID,
                (payload, context) -> context.client().execute(() -> ServerLink.receive(payload.message())));

        // Introduce ourselves once per join, and forget everything on disconnect so state
        // never leaks from one server to the next.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerLink.reset();
            ServerLink.sayHello();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ServerLink.reset());
    }

    private void registerScreenButton() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof HandledScreen<?> handled)) {
                return;
            }
            // Only offer the button once the server has said we may have it.
            if (!ServerLink.available()) {
                return;
            }

            ContainerButton button = new ContainerButton(handled);
            ButtonWidget widget = ButtonWidget
                    .builder(Text.translatable("papermc-despawned-items.button.loading"), b -> button.onPress())
                    .dimensions(button.x(), button.y(), ContainerButton.WIDTH, ContainerButton.HEIGHT)
                    .build();
            button.attach(widget);

            Screens.getButtons(screen).add(widget);
            ScreenEvents.remove(screen).register(ignored -> button.detach());

            button.requestState();
        });
    }
}
