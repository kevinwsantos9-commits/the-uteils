package dev.nivek.utility.network;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerSettingsStore {
    private static final Map<UUID, SettingsPayload> SETTINGS = new ConcurrentHashMap<>();

    private PlayerSettingsStore() {
    }

    public static SettingsPayload get(ServerPlayerEntity p) {
        return SETTINGS.getOrDefault(p.getUuid(), SettingsPayload.DISABLED);
    }

    public static void put(ServerPlayerEntity p, SettingsPayload s) {
        SETTINGS.put(p.getUuid(), s);
    }

    public static void remove(ServerPlayerEntity p) {
        SETTINGS.remove(p.getUuid());
    }
}
