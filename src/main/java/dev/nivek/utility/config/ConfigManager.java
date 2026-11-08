package dev.nivek.utility.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.nivek.utility.NivekUtility;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Carrega, salva (automaticamente a cada alteração) e restaura a config do cliente. */
public final class ConfigManager {
    private static NivekConfig cfg = new NivekConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** Chamado após cada save (usado para sincronizar as opções com o servidor). */
    public static Runnable onSave;

    private ConfigManager() {
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("nivekutility.json");
    }

    public static NivekConfig get() {
        return cfg;
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                NivekConfig c = GSON.fromJson(Files.readString(p), NivekConfig.class);
                cfg = c != null ? c : new NivekConfig();
                cfg.sanitize();
            } else {
                save();
            }
        } catch (Exception e) {
            NivekUtility.LOGGER.warn("Config inválida, usando padrão", e);
            cfg = new NivekConfig();
        }
    }

    public static void save() {
        try {
            cfg.sanitize();
            Path p = path();
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(cfg));
        } catch (Exception e) {
            NivekUtility.LOGGER.warn("Não foi possível salvar a config", e);
        }
        if (onSave != null) {
            try {
                onSave.run();
            } catch (Exception ignored) {
            }
        }
    }

    public static void reset() {
        String keepWaypoint = cfg.selectedWaypoint;
        cfg = new NivekConfig();
        cfg.selectedWaypoint = keepWaypoint;
        save();
    }
}
