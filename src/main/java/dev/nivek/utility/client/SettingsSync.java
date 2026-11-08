package dev.nivek.utility.client;

import dev.nivek.utility.config.ConfigManager;
import dev.nivek.utility.config.NivekConfig;
import dev.nivek.utility.network.SettingsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Envia ao servidor as opções do jogador (o servidor aplica os próprios limites). */
public final class SettingsSync {
    private static boolean veinHeld = false;

    private SettingsSync() {
    }

    public static void setVeinHeld(boolean held) {
        if (held != veinHeld) {
            veinHeld = held;
            send();
        }
    }

    public static void send() {
        try {
            if (!ClientPlayNetworking.canSend(SettingsPayload.ID)) return;
            NivekConfig c = ConfigManager.get();
            ClientPlayNetworking.send(new SettingsPayload(c.treeEnabled, c.treeMax, c.treeBaseOnly,
                    c.veinEnabled, c.veinMax, c.veinMode, veinHeld,
                    c.areaEnabled, c.farmClick, c.farmAuto, c.farmRadius));
        } catch (Exception ignored) {
            // sem conexão ativa
        }
    }
}
