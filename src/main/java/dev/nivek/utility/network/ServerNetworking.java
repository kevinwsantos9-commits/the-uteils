package dev.nivek.utility.network;

import dev.nivek.utility.inventory.InventoryOrganizer;
import dev.nivek.utility.mining.BreakHandler;
import dev.nivek.utility.torch.TorchPlacer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ServerNetworking {
    private ServerNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SettingsPayload.ID,
                (payload, ctx) -> PlayerSettingsStore.put(ctx.player(), payload.sanitized()));
        ServerPlayNetworking.registerGlobalReceiver(OrganizePayload.ID,
                (payload, ctx) -> InventoryOrganizer.handle(ctx.player(), payload.mode()));
        ServerPlayNetworking.registerGlobalReceiver(TorchPayload.ID,
                (payload, ctx) -> TorchPlacer.place(ctx.player(), payload.maxLight()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PlayerSettingsStore.remove(handler.player);
            BreakHandler.forget(handler.player);
        });
    }
}
