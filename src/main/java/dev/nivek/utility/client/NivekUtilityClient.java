package dev.nivek.utility.client;

import dev.nivek.utility.config.ConfigManager;
import dev.nivek.utility.config.NivekConfig;
import dev.nivek.utility.fishing.FishingAlert;
import dev.nivek.utility.fishing.FoodHelper;
import dev.nivek.utility.gui.CraftingScreen;
import dev.nivek.utility.gui.NivekScreen;
import dev.nivek.utility.gui.WaypointScreen;
import dev.nivek.utility.hud.HudRenderer;
import dev.nivek.utility.inventory.InventoryButtons;
import dev.nivek.utility.keybind.Keybinds;
import dev.nivek.utility.network.TorchPayload;
import dev.nivek.utility.waypoint.WaypointManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class NivekUtilityClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        WaypointManager.load();
        ConfigManager.onSave = SettingsSync::send;
        Keybinds.register();
        HudRenderer.register();
        InventoryButtons.register();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> SettingsSync.send());
        ClientTickEvents.END_CLIENT_TICK.register(NivekUtilityClient::tick);
    }

    private static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        NivekConfig c = ConfigManager.get();

        while (Keybinds.OPEN_GUI.wasPressed()) mc.setScreen(new NivekScreen(null));
        while (Keybinds.CRAFTING.wasPressed()) mc.setScreen(new CraftingScreen(null));
        while (Keybinds.WAYPOINTS.wasPressed()) mc.setScreen(new WaypointScreen(null));
        while (Keybinds.TOGGLE_AREA.wasPressed()) {
            c.areaEnabled = !c.areaEnabled;
            ConfigManager.save();
            mc.player.sendMessage(Text.literal("Mineração 3x3: " + (c.areaEnabled ? "ligada" : "desligada")), true);
        }
        while (Keybinds.TOGGLE_TREE.wasPressed()) {
            c.treeEnabled = !c.treeEnabled;
            ConfigManager.save();
            mc.player.sendMessage(Text.literal("Tree Capitator: " + (c.treeEnabled ? "ligado" : "desligado")), true);
        }
        while (Keybinds.TORCH.wasPressed()) {
            if (c.torchEnabled && mc.currentScreen == null && ClientPlayNetworking.canSend(TorchPayload.ID)) {
                ClientPlayNetworking.send(new TorchPayload(c.torchMaxLight));
            }
        }
        while (Keybinds.EAT.wasPressed()) {
            if (mc.currentScreen == null) FoodHelper.start(mc);
        }

        SettingsSync.setVeinHeld(mc.currentScreen == null && Keybinds.VEIN_HOLD.isPressed());
        FishingAlert.tick(mc);
        FoodHelper.tick(mc);
    }
}
