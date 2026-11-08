package dev.nivek.utility.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.nivek.utility.NivekUtility;
import dev.nivek.utility.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Waypoints salvos por mundo (singleplayer) ou por servidor, em config/nivekutility-waypoints.json. */
public final class WaypointManager {
    private static Map<String, List<Waypoint>> data = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private WaypointManager() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("nivekutility-waypoints.json");
    }

    public static void load() {
        try {
            Path p = file();
            if (Files.exists(p)) {
                Map<String, List<Waypoint>> m = GSON.fromJson(Files.readString(p),
                        new TypeToken<Map<String, List<Waypoint>>>() {
                        }.getType());
                if (m != null) data = m;
            }
        } catch (Exception e) {
            NivekUtility.LOGGER.warn("Não foi possível ler os waypoints", e);
        }
    }

    public static void save() {
        try {
            Path p = file();
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(data));
        } catch (Exception e) {
            NivekUtility.LOGGER.warn("Não foi possível salvar os waypoints", e);
        }
    }

    private static String worldKey() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.isInSingleplayer() && c.getServer() != null) {
            return "sp:" + c.getServer().getSaveProperties().getLevelName();
        }
        ServerInfo si = c.getCurrentServerEntry();
        return si != null ? "mp:" + si.address : "other";
    }

    public static String currentDimension() {
        MinecraftClient c = MinecraftClient.getInstance();
        return c.world == null ? "minecraft:overworld" : c.world.getRegistryKey().getValue().toString();
    }

    public static List<Waypoint> list() {
        return data.computeIfAbsent(worldKey(), k -> new ArrayList<>());
    }

    public static Waypoint get(String id) {
        if (id == null || id.isEmpty()) return null;
        for (Waypoint w : list()) if (id.equals(w.id)) return w;
        return null;
    }

    public static Waypoint selected() {
        return get(ConfigManager.get().selectedWaypoint);
    }

    public static Waypoint create(String name, int x, int y, int z, int type) {
        Waypoint w = new Waypoint(UUID.randomUUID().toString(), name, currentDimension(), x, y, z, type);
        list().add(w);
        save();
        return w;
    }

    public static void remove(Waypoint w) {
        list().remove(w);
        if (w.id.equals(ConfigManager.get().selectedWaypoint)) {
            ConfigManager.get().selectedWaypoint = "";
            ConfigManager.save();
        }
        save();
    }
}
