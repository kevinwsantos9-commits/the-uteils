package dev.nivek.utility.inventory;

import dev.nivek.utility.config.ConfigManager;
import dev.nivek.utility.mixin.HandledScreenAccessor;
import dev.nivek.utility.network.OrganizePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Botões de organizar ao lado das telas de inventário, baú, barril e shulker. */
public final class InventoryButtons {
    private InventoryButtons() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!ConfigManager.get().showInvButtons) return;
            boolean container = screen instanceof GenericContainerScreen || screen instanceof ShulkerBoxScreen;
            if (!container && !(screen instanceof InventoryScreen)) return;

            HandledScreenAccessor a = (HandledScreenAccessor) screen;
            int x = a.nivek_getX() + a.nivek_getBackgroundWidth() + 4;
            int y = a.nivek_getY() + 4;
            if (container) {
                add(screen, x, y, "Organizar baú", 1);
                add(screen, x, y + 22, "Organizar inventário", 0);
                add(screen, x, y + 44, "Mover itens iguais", 2);
            } else {
                add(screen, x, y, "Organizar inventário", 0);
            }
        });
    }

    private static void add(Screen screen, int x, int y, String label, int mode) {
        Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal(label), b -> {
            if (ClientPlayNetworking.canSend(OrganizePayload.ID)) {
                ClientPlayNetworking.send(new OrganizePayload(mode));
            }
        }).dimensions(x, y, 110, 20).build());
    }
}
