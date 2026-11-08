package dev.nivek.utility.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class Payloads {
    private Payloads() {
    }

    public static void registerAll() {
        PayloadTypeRegistry.playC2S().register(SettingsPayload.ID, SettingsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OrganizePayload.ID, OrganizePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TorchPayload.ID, TorchPayload.CODEC);
    }
}
